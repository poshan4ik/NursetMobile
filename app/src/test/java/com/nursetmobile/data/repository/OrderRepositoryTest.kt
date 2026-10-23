package com.nursetmobile.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nursetmobile.data.AppDatabase
import com.nursetmobile.data.entity.CategoryEntity
import com.nursetmobile.data.entity.ProductEntity
import com.nursetmobile.domain.OrderStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import com.nursetmobile.data.entity.ClientEntity

/**
 * Интеграционные тесты OrderRepository с in-memory Room (Robolectric).
 * Проверяют: создание заказа, списание остатков, отмену с возвратом, защиту от повторной отмены.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrderRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: OrderRepository
    private var categoryId: Long = 0
    private var product1Id: Long = 0
    private var product2Id: Long = 0

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OrderRepository.create(database)

        runBlocking {
            // Создаём категорию и товары для тестов
            categoryId = database.categoryDao().insert(CategoryEntity("Тест"))
            product1Id = database.productDao().insert(ProductEntity("Товар 1", categoryId, 1000, 10))
            product2Id = database.productDao().insert(ProductEntity("Товар 2", categoryId, 500, 5))
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `заказ связан с клиентом и хранит его имя`() = runBlocking {
        val clientId = database.clientDao().insert(ClientEntity(name = "Анна", phone = "+79991234567"))
        val id = repository.createOrder(listOf(OrderItemInput(product1Id, 2, 1)), clientId).getOrThrow()
        val order = database.orderDao().getOrderById(id)!!
        assertEquals(clientId, order.clientId)
        assertEquals("Анна", order.clientName)
        assertEquals(2000L, order.totalPrice)
    }

    @Test
    fun `несуществующий клиент не списывает товары`() = runBlocking {
        assertTrue(repository.createOrder(listOf(OrderItemInput(product1Id, 1, 1000)), -1).isFailure)
        assertEquals(10, database.productDao().getById(product1Id)?.stock)
        assertEquals(0, database.orderDao().countOrders())
    }

    @Test
    fun `изменение цены требует повторного подтверждения и не списывает товар`() = runBlocking {
        val product = database.productDao().getById(product1Id)!!
        database.productDao().update(product.copy(price = 1500))
        val result = repository.createOrder(listOf(OrderItemInput(product1Id, 2, 1000)), expectedTotal = 2000)
        assertTrue(result.isFailure)
        assertEquals(10, database.productDao().getById(product1Id)?.stock)
        assertEquals(0, database.orderDao().countOrders())
        val id = repository.createOrder(listOf(OrderItemInput(product1Id, 2, 1500)), expectedTotal = 3000).getOrThrow()
        assertEquals(3000L, database.orderDao().getOrderById(id)?.totalPrice)
        assertEquals(8, database.productDao().getById(product1Id)?.stock)
    }

    @Test
    fun `изменение цен позиций проверяется даже при прежней итоговой сумме`() = runBlocking {
        database.productDao().update(database.productDao().getById(product1Id)!!.copy(price = 900))
        database.productDao().update(database.productDao().getById(product2Id)!!.copy(price = 600))
        assertTrue(repository.createOrder(listOf(OrderItemInput(product1Id, 1, 1000), OrderItemInput(product2Id, 1, 500)), expectedTotal = 1500).isFailure)
        assertEquals(0, database.orderDao().countOrders())
    }

    @Test
    fun `редактирование клиента сохраняет историю и снимок имени в заказе`() = runBlocking {
        val clients = ClientRepository(database.clientDao())
        val clientId = clients.register("Анна", "79991234567").getOrThrow()
        val orderId = repository.createOrder(listOf(OrderItemInput(product1Id, 1, 1000)), clientId).getOrThrow()
        clients.update(clientId, "Анна Иванова", "79991234567").getOrThrow()
        val order = database.orderDao().getOrderById(orderId)!!
        assertEquals(clientId, order.clientId)
        assertEquals("Анна", order.clientName)
    }

    @Test
    fun `переполнение количества не создаёт заказ`() = runBlocking {
        val result = repository.createOrder(listOf(
            OrderItemInput(product1Id, Int.MAX_VALUE, 1000), OrderItemInput(product1Id, 1, 1000)))
        assertTrue(result.isFailure)
        assertEquals(10, database.productDao().getById(product1Id)?.stock)
        assertEquals(0, database.orderDao().countOrders())
    }

    @Test
    fun `переполнение суммы откатывает заказ`() = runBlocking {
        val product = database.productDao().getById(product1Id)!!
        database.productDao().update(product.copy(price = Long.MAX_VALUE))
        assertTrue(repository.createOrder(listOf(OrderItemInput(product1Id, 2, 1))).isFailure)
        assertEquals(10, database.productDao().getById(product1Id)?.stock)
        assertEquals(0, database.orderDao().countOrders())
    }

    @Test
    fun `одновременные завершение и отмена сохраняют правильный остаток`() = runBlocking {
        repeat(10) {
            val id = repository.createOrder(listOf(OrderItemInput(product1Id, 1, 1000))).getOrThrow()
            repository.updateOrderStatus(id, OrderStatus.PROCESSING).getOrThrow()
            val stockBefore = database.productDao().getById(product1Id)!!.stock
            val complete = async(Dispatchers.Default) { repository.updateOrderStatus(id, OrderStatus.COMPLETED) }
            val cancel = async(Dispatchers.Default) { repository.cancelOrder(id) }
            val results = listOf(complete.await(), cancel.await())
            assertEquals(1, results.count { it.isSuccess })
            val cancelled = database.orderDao().getOrderById(id)!!.status == OrderStatus.CANCELLED.value
            assertEquals(stockBefore + if (cancelled) 1 else 0, database.productDao().getById(product1Id)!!.stock)
        }
    }

    @Test
    fun `создание заказа — успешное списание остатков`() = runBlocking {
        val items = listOf(
            OrderItemInput(product1Id, 3, 1000),
            OrderItemInput(product2Id, 2, 500)
        )

        val result = repository.createOrder(items)
        assertTrue(result.isSuccess)

        val orderId = result.getOrThrow()
        val order = database.orderDao().getOrderById(orderId)
        assertNotNull(order)
        assertEquals(OrderStatus.NEW.value, order?.status)
        assertEquals(4000L, order?.totalPrice) // 3*1000 + 2*500

        // Проверяем остатки: 10-3=7, 5-2=3
        val p1 = database.productDao().getById(product1Id)
        val p2 = database.productDao().getById(product2Id)
        assertEquals(7, p1?.stock)
        assertEquals(3, p2?.stock)

        // Проверяем позиции заказа (snapshot цен)
        val itemsDb = database.orderDao().getItemsByOrderId(orderId)
        assertEquals(2, itemsDb.size)
        assertEquals("Товар 1", itemsDb[0].productName)
        assertEquals(1000L, itemsDb[0].price) // snapshot цены
        assertEquals(3, itemsDb[0].quantity)
    }

    @Test
    fun `создание заказа при недостатке товара — ошибка, остатки не меняются`() = runBlocking {
        val items = listOf(
            OrderItemInput(product1Id, 15, 1000) // больше чем в наличии (10)
        )

        val result = repository.createOrder(items)
        assertFalse(result.isSuccess)
        assertEquals(
            "Недостаточно товара на складе. Доступно: 10 шт.",
            result.exceptionOrNull()?.message
        )

        // Остаток не должен измениться
        val p1 = database.productDao().getById(product1Id)
        assertEquals(10, p1?.stock)
    }

    @Test
    fun `создание заказа с пустой корзиной — ошибка`() = runBlocking {
        val result = repository.createOrder(emptyList())
        assertFalse(result.isSuccess)
        assertEquals("Корзина пуста. Добавьте товары.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `создание заказа с количеством ноль — ошибка`() = runBlocking {
        val items = listOf(
            OrderItemInput(product1Id, 0, 1000)
        )
        val result = repository.createOrder(items)
        assertFalse(result.isSuccess)
        assertEquals("Количество должно быть больше нуля.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `отмена заказа NEW — возврат товаров на склад`() = runBlocking {
        // Создаём заказ
        val items = listOf(
            OrderItemInput(product1Id, 3, 1000),
            OrderItemInput(product2Id, 2, 500)
        )
        val createResult = repository.createOrder(items)
        val orderId = createResult.getOrThrow()

        // Проверяем остатки после создания: 7 и 3
        var p1 = database.productDao().getById(product1Id)
        var p2 = database.productDao().getById(product2Id)
        assertEquals(7, p1?.stock)
        assertEquals(3, p2?.stock)

        // Отменяем заказ
        val cancelResult = repository.cancelOrder(orderId)
        assertTrue(cancelResult.isSuccess)

        // Проверяем статус заказа
        val order = database.orderDao().getOrderById(orderId)
        assertEquals(OrderStatus.CANCELLED.value, order?.status)

        // Проверяем возврат остатков: 7+3=10, 3+2=5
        p1 = database.productDao().getById(product1Id)
        p2 = database.productDao().getById(product2Id)
        assertEquals(10, p1?.stock)
        assertEquals(5, p2?.stock)
    }

    @Test
    fun `отмена заказа PROCESSING — возврат товаров на склад`() = runBlocking {
        // Создаём заказ
        val items = listOf(OrderItemInput(product1Id, 2, 1000))
        val createResult = repository.createOrder(items)
        val orderId = createResult.getOrThrow()

        // Меняем статус на PROCESSING
        repository.updateOrderStatus(orderId, OrderStatus.PROCESSING)

        // Отменяем
        val cancelResult = repository.cancelOrder(orderId)
        assertTrue(cancelResult.isSuccess)

        // Проверяем возврат: было 10-2=8, стало 10
        val p1 = database.productDao().getById(product1Id)
        assertEquals(10, p1?.stock)
    }

    @Test
    fun `отмена COMPLETED заказа — ошибка`() = runBlocking {
        val items = listOf(OrderItemInput(product1Id, 2, 1000))
        val createResult = repository.createOrder(items)
        val orderId = createResult.getOrThrow()

        // COMPLETED
        repository.updateOrderStatus(orderId, OrderStatus.PROCESSING)
        repository.updateOrderStatus(orderId, OrderStatus.COMPLETED)

        val cancelResult = repository.cancelOrder(orderId)
        assertFalse(cancelResult.isSuccess)
        assertEquals(
            "Можно отменить только активные заказы (Новый или В обработке).",
            cancelResult.exceptionOrNull()?.message
        )
    }

    @Test
    fun `отмена CANCELLED заказа — ошибка`() = runBlocking {
        val items = listOf(OrderItemInput(product1Id, 2, 1000))
        val createResult = repository.createOrder(items)
        val orderId = createResult.getOrThrow()

        // Отменяем первый раз
        repository.cancelOrder(orderId)

        // Пытаемся отменить повторно
        val cancelResult = repository.cancelOrder(orderId)
        assertFalse(cancelResult.isSuccess)
        assertEquals("Заказ уже отменён.", cancelResult.exceptionOrNull()?.message)

        // Остаток не должен увеличиться повторно (остался 10)
        val p1 = database.productDao().getById(product1Id)
        assertEquals(10, p1?.stock)
    }

    @Test
    fun `защита от повторной отмены в транзакции`() = runBlocking {
        val items = listOf(OrderItemInput(product1Id, 2, 1000))
        val createResult = repository.createOrder(items)
        val orderId = createResult.getOrThrow()

        // Первая отмена
        repository.cancelOrder(orderId)

        // Вторая отмена — должна не пройти проверку статуса
        val cancelResult = repository.cancelOrder(orderId)
        assertFalse(cancelResult.isSuccess)
        assertTrue(cancelResult.exceptionOrNull()?.message?.contains("уже отменён") == true)

        // Остаток не изменился
        val p1 = database.productDao().getById(product1Id)
        assertEquals(10, p1?.stock)
    }

    @Test
    fun `отмена несуществующего заказа — ошибка`() = runBlocking {
        val result = repository.cancelOrder(999999)
        assertFalse(result.isSuccess)
        assertEquals("Заказ не найден.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `смена статуса несуществующего заказа — ошибка`() = runBlocking {
        val result = repository.updateOrderStatus(999999, OrderStatus.PROCESSING)
        assertFalse(result.isSuccess)
        assertEquals("Заказ не найден.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `недопустимый переход статуса — ошибка`() = runBlocking {
        val items = listOf(OrderItemInput(product1Id, 1, 1000))
        val createResult = repository.createOrder(items)
        val orderId = createResult.getOrThrow()

        // Пытаемся перевести NEW в COMPLETED (недопустимо)
        val result = repository.updateOrderStatus(orderId, OrderStatus.COMPLETED)
        assertFalse(result.isSuccess)
        assertEquals(
            "Недопустимый переход статуса: Новый → Выполнен",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `допустимый переход NEW в PROCESSING`() = runBlocking {
        val items = listOf(OrderItemInput(product1Id, 1, 1000))
        val createResult = repository.createOrder(items)
        val orderId = createResult.getOrThrow()

        val result = repository.updateOrderStatus(orderId, OrderStatus.PROCESSING)
        assertTrue(result.isSuccess)

        val order = database.orderDao().getOrderById(orderId)
        assertEquals(OrderStatus.PROCESSING.value, order?.status)
    }

    @Test
    fun `допустимый переход PROCESSING в COMPLETED`() = runBlocking {
        val items = listOf(OrderItemInput(product1Id, 1, 1000))
        val createResult = repository.createOrder(items)
        val orderId = createResult.getOrThrow()

        repository.updateOrderStatus(orderId, OrderStatus.PROCESSING)
        val result = repository.updateOrderStatus(orderId, OrderStatus.COMPLETED)
        assertTrue(result.isSuccess)

        val order = database.orderDao().getOrderById(orderId)
        assertEquals(OrderStatus.COMPLETED.value, order?.status)
    }

    @Test
    fun `createOrder при ошибке — ничего не меняется (одна транзакция)`() = runBlocking {
        // Второй товар не существует: заказ не должен создаться, остатки — не измениться
        val items = listOf(
            OrderItemInput(product1Id, 3, 1000),
            OrderItemInput(999L, 1, 500)
        )

        val result = repository.createOrder(items)
        assertFalse(result.isSuccess)
        assertEquals("Товар не найден (ID: 999).", result.exceptionOrNull()?.message)

        val p1 = database.productDao().getById(product1Id)
        assertEquals(10, p1?.stock)
        assertEquals(0, database.orderDao().countOrders())
    }

    @Test
    fun `cancelOrder — при отмене товары возвращаются на склад`() = runBlocking {
        val items = listOf(OrderItemInput(product1Id, 3, 1000))
        val orderId = repository.createOrder(items).getOrThrow()
        assertEquals(7, database.productDao().getById(product1Id)?.stock)

        repository.updateOrderStatus(orderId, OrderStatus.PROCESSING)
        val result = repository.updateOrderStatus(orderId, OrderStatus.CANCELLED)
        assertTrue(result.isSuccess)

        val order = database.orderDao().getOrderById(orderId)
        assertEquals(OrderStatus.CANCELLED.value, order?.status)
        assertEquals(10, database.productDao().getById(product1Id)?.stock)
    }

    @Test
    fun `сумма заказа считается по ценам из базы, а не из корзины`() = runBlocking {
        // Цена из корзины намеренно занижена — в заказе должна быть цена из базы (1000)
        val items = listOf(OrderItemInput(product1Id, 2, 1))

        val orderId = repository.createOrder(items).getOrThrow()

        val order = database.orderDao().getOrderById(orderId)
        assertEquals(2000L, order?.totalPrice)
        val orderItems = database.orderDao().getItemsByOrderId(orderId)
        assertEquals(1000L, orderItems[0].price)
    }

    @Test
    fun `один и тот же товар в корзине дважды — количество складывается перед проверкой`() = runBlocking {
        // 6+6=12 > 10 в наличии — ошибка, остатки не меняются
        val tooMany = listOf(
            OrderItemInput(product1Id, 6, 1000),
            OrderItemInput(product1Id, 6, 1000)
        )
        val failResult = repository.createOrder(tooMany)
        assertFalse(failResult.isSuccess)
        assertEquals(
            "Недостаточно товара на складе. Доступно: 10 шт.",
            failResult.exceptionOrNull()?.message
        )
        assertEquals(10, database.productDao().getById(product1Id)?.stock)

        // 3+4=7 <= 10 — успех, одна позиция заказа с количеством 7, остаток 3
        val okItems = listOf(
            OrderItemInput(product1Id, 3, 1000),
            OrderItemInput(product1Id, 4, 1000)
        )
        val orderId = repository.createOrder(okItems).getOrThrow()
        val orderItems = database.orderDao().getItemsByOrderId(orderId)
        assertEquals(1, orderItems.size)
        assertEquals(7, orderItems[0].quantity)
        assertEquals(3, database.productDao().getById(product1Id)?.stock)
        assertEquals(7000L, database.orderDao().getOrderById(orderId)?.totalPrice)
    }
}
