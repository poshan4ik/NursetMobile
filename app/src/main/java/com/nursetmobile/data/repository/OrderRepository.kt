package com.nursetmobile.data.repository

import androidx.room.withTransaction
import com.nursetmobile.data.AppDatabase
import com.nursetmobile.data.dao.OrderDao
import com.nursetmobile.data.dao.ProductDao
import com.nursetmobile.data.entity.OrderEntity
import com.nursetmobile.data.entity.OrderItemEntity
import com.nursetmobile.data.entity.ProductEntity
import com.nursetmobile.domain.OrderStatus
import com.nursetmobile.domain.canTransitionTo
import com.nursetmobile.domain.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OrderRepository(private val database: AppDatabase) {

    private val orderDao: OrderDao = database.orderDao()
    private val productDao: ProductDao = database.productDao()

    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()

    val availableProducts: Flow<List<ProductEntity>> = productDao.getAll()
        .map { list -> list.filter { it.stock > 0 } }

    suspend fun getOrderWithItems(orderId: Long) = orderDao.getOrderWithItems(orderId)

    suspend fun createOrder(items: List<OrderItemInput>): Result<Long> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Корзина пуста. Добавьте товары."))
        }
        if (items.any { it.quantity <= 0 }) {
            return Result.failure(IllegalArgumentException("Количество должно быть больше нуля."))
        }
        return try {
            val orderId = database.withTransaction { createOrderInTransaction(items) }
            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: OrderStatus): Result<Unit> {
        // Отмена через отдельный метод: он возвращает товары на склад
        if (newStatus == OrderStatus.CANCELLED) {
            return cancelOrder(orderId)
        }
        val order = orderDao.getOrderById(orderId)
            ?: return Result.failure(IllegalArgumentException("Заказ не найден."))
        val currentStatus = OrderStatus.fromValue(order.status)
        if (!canTransitionTo(currentStatus, newStatus)) {
            return Result.failure(
                IllegalArgumentException(
                    "Недопустимый переход статуса: ${currentStatus.displayName} → ${newStatus.displayName}"
                )
            )
        }
        orderDao.updateOrder(order.copy(status = newStatus.value))
        return Result.success(Unit)
    }

    suspend fun cancelOrder(orderId: Long): Result<Unit> {
        val order = orderDao.getOrderById(orderId)
            ?: return Result.failure(IllegalArgumentException("Заказ не найден."))
        val currentStatus = OrderStatus.fromValue(order.status)
        if (currentStatus == OrderStatus.CANCELLED) {
            return Result.failure(IllegalArgumentException("Заказ уже отменён."))
        }
        if (!isActive(currentStatus)) {
            return Result.failure(
                IllegalArgumentException("Можно отменить только активные заказы (Новый или В обработке).")
            )
        }
        return try {
            database.withTransaction {
                cancelOrderInTransaction(orderId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Возвращает товары на склад и ставит статус CANCELLED — все изменения в одной транзакции
    private suspend fun cancelOrderInTransaction(orderId: Long) {
        val order = orderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Заказ не найден.")
        if (OrderStatus.fromValue(order.status) == OrderStatus.CANCELLED) {
            throw IllegalArgumentException("Заказ уже отменён.")
        }
        val items = orderDao.getItemsByOrderId(orderId)
        for (item in items) {
            val product = productDao.getById(item.productId)
            if (product != null) {
                productDao.update(product.copy(stock = product.stock + item.quantity))
            }
        }
        orderDao.updateOrder(order.copy(status = OrderStatus.CANCELLED.value))
    }

    // Создание заказа и списание остатков — все изменения в одной транзакции
    private suspend fun createOrderInTransaction(items: List<OrderItemInput>): Long {
        // Повторяющиеся товары объединяем: суммарное количество по каждому товару
        val totalQuantityByProductId = items
            .groupBy { it.productId }
            .mapValues { (_, productItems) -> productItems.sumOf { item -> item.quantity } }

        // Проверяем остатки по объединённому количеству и берём цены из базы
        val products = totalQuantityByProductId.mapValues { (productId, totalQuantity) ->
            val product = productDao.getById(productId)
                ?: throw IllegalArgumentException("Товар не найден (ID: $productId).")
            if (product.stock < totalQuantity) {
                throw IllegalArgumentException("Недостаточно товара на складе. Доступно: ${product.stock} шт.")
            }
            product
        }

        val totalPrice = products.values.sumOf { product ->
            product.price * totalQuantityByProductId.getValue(product.id)
        }

        val order = OrderEntity(
            createdAt = System.currentTimeMillis(),
            totalPrice = totalPrice,
            status = OrderStatus.NEW.value
        )
        val orderItems = products.values.map { product ->
            OrderItemEntity(
                orderId = 0,
                productId = product.id,
                productName = product.name,
                price = product.price,
                quantity = totalQuantityByProductId.getValue(product.id)
            )
        }
        val orderId = orderDao.createOrderWithItems(order, orderItems)

        products.values.forEach { product ->
            productDao.update(
                product.copy(stock = product.stock - totalQuantityByProductId.getValue(product.id))
            )
        }
        return orderId
    }

    companion object {
        fun create(database: AppDatabase): OrderRepository = OrderRepository(database)
    }
}

data class OrderItemInput(
    val productId: Long,
    val quantity: Int,
    val price: Long
)
