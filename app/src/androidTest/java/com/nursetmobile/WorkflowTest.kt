package com.nursetmobile

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
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
        compose.onNodeWithText("Создать заказ").performScrollTo().performClick()
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

    private fun waitWithoutText(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }
    }

}
