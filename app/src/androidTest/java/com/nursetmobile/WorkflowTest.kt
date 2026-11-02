package com.nursetmobile

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nursetmobile.data.AppDatabase
import com.nursetmobile.data.entity.*
import com.nursetmobile.data.repository.OrderRepository
import com.nursetmobile.domain.OrderStatus
import com.nursetmobile.ui.clients.ClientsScreen
import com.nursetmobile.ui.orders.CreateOrderScreen
import com.nursetmobile.ui.orders.OrdersScreen
import com.nursetmobile.viewmodel.ClientViewModel
import com.nursetmobile.viewmodel.OrderViewModel
import com.nursetmobile.viewmodel.ProductViewModel
import com.nursetmobile.viewmodel.HomeViewModel
import com.nursetmobile.ui.products.ProductsScreen
import com.nursetmobile.ui.home.HomeScreen
import com.nursetmobile.domain.ProductStockFilter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@RunWith(AndroidJUnit4::class)
class WorkflowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: AppDatabase
    private val store = ViewModelStore()
    private lateinit var clients: ClientViewModel
    private lateinit var orders: OrderViewModel

    @Before fun setup() {
        dismissSystemUiDialog()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        compose.runOnUiThread {
            clients = ViewModelProvider(store, ClientViewModel.Factory(database))[ClientViewModel::class.java]
            orders = ViewModelProvider(store, OrderViewModel.Factory(database))[OrderViewModel::class.java]
        }
    }

    @After fun close() {
        compose.runOnUiThread { store.clear() }
        database.close()
    }

    @Test fun orderSearchAndStatusWorkTogether() = runBlocking {
        database.orderDao().insertOrder(OrderEntity(clientName = "Анна", status = OrderStatus.NEW.value))
        database.orderDao().insertOrder(OrderEntity(clientName = "Борис", status = OrderStatus.COMPLETED.value))
        compose.setContent { MaterialTheme { OrdersScreen({}, {}, orders) } }
        waitForText("Борис")
        compose.onNodeWithText("Активные").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithText("Активные").assertIsSelected()
        compose.onNodeWithText("Анна").assertExists()
        compose.onNodeWithText("Номер заказа или имя клиента").performTextInput("#002")
        waitForText("Сбросить фильтры")
        compose.onNodeWithText("Сбросить фильтры").assertExists().performClick()
        waitForText("Борис")
        compose.onNodeWithText("Борис").assertExists()
        Unit
    }

    @Test fun editClientAndCreateOrderFromCard() = runBlocking {
        val id = database.clientDao().insert(ClientEntity(name = "Анна", phone = "79991234567"))
        val createdFor = AtomicLong(0)
        compose.setContent { MaterialTheme { ClientsScreen({}, { createdFor.set(it) }, clients) } }
        waitForText("Анна")
        compose.onNodeWithText("Анна").performClick()
        waitForText("Изменить")
        compose.onNodeWithText("Изменить").performClick()
        waitForText("Имя")
        compose.onNodeWithText("Имя").performTextReplacement("Анна Иванова")
        compose.onNodeWithText("Сохранить").performClick()
        waitWithoutText("Имя")
        waitForText("Анна Иванова")
        compose.onNodeWithText("Создать заказ").performClick()
        compose.waitUntil(20_000) { createdFor.get() == id }
        assertEquals(id, createdFor.get())
        assertEquals("Анна Иванова", database.clientDao().getById(id)?.name)
        compose.onNodeWithText("Назад").performClick()
        waitForText("Имя или телефон")
        compose.onNodeWithText("Имя или телефон").assertExists()
        Unit
    }

    @Test fun registerClientInsideOrderAndConfirmCart() = runBlocking {
        val categoryId = database.categoryDao().insert(CategoryEntity("Категория"))
        database.productDao().insert(ProductEntity("Тестовый товар", categoryId, 1000, 5))
        val returned = AtomicBoolean(false)
        compose.setContent { MaterialTheme {
            CreateOrderScreen(onBack = { returned.set(true) }, viewModel = orders, clientViewModel = clients)
        } }
        compose.onNodeWithText("Новый клиент").performClick()
        waitForText("Имя")
        compose.onNodeWithText("Имя").performTextInput("Анна")
        compose.onNodeWithText("Телефон").performTextInput("79991234567")
        compose.onNodeWithText("Сохранить").performClick()
        waitWithoutText("Имя")
        waitForText("Анна")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Выбрать товары").performClick()
        waitForText("Тестовый товар")
        compose.onNode(hasText("Тестовый товар") and hasClickAction()).performClick()
        waitForText("В корзине: 1")
        compose.onNode(hasText("Тестовый товар") and hasClickAction()).performClick()
        waitForText("В корзине: 2")
        compose.onNodeWithText("В корзине: 2").assertExists()
        compose.onNodeWithText("Готово").performClick()
        waitWithoutText("Готово")
        compose.onNodeWithContentDescription("Отмена").performClick()
        waitForText("Выйти из оформления?")
        compose.onNodeWithText("Выйти из оформления?").assertExists()
        compose.onNodeWithText("Продолжить заказ").performClick()
        waitWithoutText("Выйти из оформления?")
        assertFalse(returned.get())
        compose.onNodeWithText("Создать заказ").assertIsDisplayed().performClick()
        compose.waitUntil(20_000) { runBlocking { database.orderDao().countOrders() == 1 } }
        val order = OrderRepository.create(database).allOrders
        val saved = order.first().single()
        assertEquals("Анна", saved.clientName)
        assertEquals(2000L, saved.totalPrice)
        assertEquals(2, database.orderDao().getItemsByOrderId(saved.id).single().quantity)
    }

    private fun waitForText(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun guestOrderUsesManualQuantityAndOpensCreatedOrder() = runBlocking {
        val category = database.categoryDao().insert(CategoryEntity("Аксессуары"))
        val product = database.productDao().insert(ProductEntity("Кабель", category, 500, 50))
        val created = AtomicLong(0)
        compose.setContent { MaterialTheme {
            CreateOrderScreen(onBack = {}, onCreated = { created.set(it) }, viewModel = orders, clientViewModel = clients)
        } }
        compose.onNodeWithText("Без указания клиента").assertExists()
        compose.onNodeWithText("Выбрать товары").performClick()
        waitForText("Кабель")
        compose.onNode(hasText("Кабель") and hasClickAction()).performClick()
        compose.onNodeWithText("Готово").performClick()
        waitWithoutText("Готово")
        compose.onNode(hasText("1") and hasClickAction()).performScrollTo().performClick()
        waitForText("Количество товара")
        compose.onNodeWithText("Количество").performTextReplacement("0")
        compose.onNodeWithText("Применить").assertIsNotEnabled()
        compose.onNodeWithText("Количество").performTextReplacement("51")
        compose.onNodeWithText("Применить").assertIsNotEnabled()
        compose.onNodeWithText("Количество").performTextReplacement("20")
        compose.onNodeWithText("Применить").performClick()
        waitWithoutText("Применить")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("10 000 ₸", substring = false).assertIsDisplayed()
        saveScreenshot("qa-checkout.png")
        compose.onNodeWithText("Создать заказ").assertIsDisplayed().performClick()
        compose.waitUntil(20_000) { created.get() > 0 }
        val saved = database.orderDao().getOrderById(created.get())!!
        assertNull(saved.clientId)
        assertEquals(10_000L, saved.totalPrice)
        assertEquals(20, database.orderDao().getItemsByOrderId(saved.id).single().quantity)
        assertEquals(30, database.productDao().getById(product)?.stock)
    }

    @Test fun productCategoryPickerDistinguishesIdenticalNames() = runBlocking {
        val first = database.categoryDao().insert(CategoryEntity("Категория А"))
        val second = database.categoryDao().insert(CategoryEntity("Категория Б"))
        database.productDao().insert(ProductEntity("Кабель", first, 500, 10))
        val chosen = database.productDao().insert(ProductEntity("Кабель", second, 1000, 10))
        compose.setContent { MaterialTheme {
            CreateOrderScreen(onBack = {}, viewModel = orders, clientViewModel = clients)
        } }
        compose.onNodeWithText("Выбрать товары").performClick()
        waitForText("Все категории")
        compose.onNode(hasScrollToIndexAction() and hasAnyDescendant(hasText("Все категории")))
            .performScrollToNode(hasText("Категория Б"))
        val categoryChip = hasText("Категория Б") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)
        compose.onNode(categoryChip).performClick().assertIsSelected()
        saveScreenshot("qa-picker.png")
        val chosenRow = hasText("Кабель") and hasText("Категория Б") and hasClickAction()
        val otherRow = hasText("Кабель") and hasText("Категория А") and hasClickAction()
        compose.onNode(otherRow).assertIsNotDisplayed()
        compose.onNode(chosenRow).assertIsDisplayed().performClick()
        compose.onNodeWithText("Готово").performClick()
        waitWithoutText("Готово")
        compose.onNodeWithText("Создать заказ").performClick()
        compose.waitUntil(20_000) { runBlocking { database.orderDao().countOrders() == 1 } }
        val saved = database.orderDao().getAllOrders().first().single()
        assertEquals(chosen, database.orderDao().getItemsByOrderId(saved.id).single().productId)
        assertEquals(1000L, saved.totalPrice)
    }

    @Test fun restockRemovesProductFromOutOfStockFilter() = runBlocking {
        val category = database.categoryDao().insert(CategoryEntity("Категория"))
        val product = database.productDao().insert(ProductEntity("Закончился", category, 500, 0))
        database.productDao().insert(ProductEntity("Есть на складе", category, 1000, 10))
        lateinit var products: ProductViewModel
        compose.runOnUiThread { products = ViewModelProvider(store, ProductViewModel.Factory(database))[ProductViewModel::class.java] }
        compose.setContent { MaterialTheme {
            ProductsScreen(products, requestedStockFilter = ProductStockFilter.OUT_OF_STOCK.name)
        } }
        waitForText("Закончился")
        saveScreenshot("qa-products.png")
        compose.onNodeWithText("Есть на складе").assertDoesNotExist()
        compose.onNodeWithText("Пополнить остаток").performClick()
        waitForText("Поступило, шт.")
        compose.onNodeWithText("Поступило, шт.").performTextInput("5")
        compose.onNodeWithText("Пополнить", substring = false).performClick()
        waitForText("Товары не найдены")
        assertEquals(5, database.productDao().getById(product)?.stock)
        compose.onNodeWithText("Сбросить фильтры").assertExists()
        Unit
    }

    @Test fun homeCardsOpenTheRelevantStockList() = runBlocking {
        val category = database.categoryDao().insert(CategoryEntity("Категория"))
        database.productDao().insert(ProductEntity("Закончился", category, 500, 0))
        database.productDao().insert(ProductEntity("Мало", category, 1000, 2))
        lateinit var home: HomeViewModel
        var selected: ProductStockFilter? = null
        compose.runOnUiThread { home = ViewModelProvider(store, HomeViewModel.Factory(database))[HomeViewModel::class.java] }
        compose.setContent { MaterialTheme { HomeScreen({}, { selected = it }, {}, {}, home) } }
        compose.waitUntil(20_000) { home.summary.value.productsCount == 2 }
        waitForText("Нет в наличии")
        saveScreenshot("qa-home.png")
        compose.onNode(hasText("Нет в наличии") and hasClickAction()).performClick()
        compose.runOnIdle { assertEquals(ProductStockFilter.OUT_OF_STOCK, selected) }
        compose.onNode(hasText("Заканчиваются") and hasClickAction()).performClick()
        compose.runOnIdle { assertEquals(ProductStockFilter.LOW, selected) }
    }

    private fun waitWithoutText(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }
    }

    private fun saveScreenshot(name: String) {
        dismissSystemUiDialog()
        compose.waitForIdle()
        Thread.sleep(500) // Let the system keyboard finish its dismissal animation.
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), name)
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun dismissSystemUiDialog() {
        val automation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(20) {
            val root = automation.rootInActiveWindow ?: return
            if (root.findAccessibilityNodeInfosByText("System UI isn't responding").isEmpty()) return
            root.findAccessibilityNodeInfosByText("Wait").firstOrNull()
                ?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
            Thread.sleep(100)
        }
    }

}
