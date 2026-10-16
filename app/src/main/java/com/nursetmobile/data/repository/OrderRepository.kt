package com.nursetmobile.data.repository

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

class OrderRepository(
    private val orderDao: OrderDao,
    private val productDao: ProductDao
) {

    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()

    suspend fun getOrderWithItems(orderId: Long) = orderDao.getOrderWithItems(orderId)

    suspend fun createOrder(items: List<OrderItemInput>): Result<Long> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Корзина пуста. Добавьте товары."))
        }
        for (item in items) {
            if (item.quantity <= 0) {
                return Result.failure(IllegalArgumentException("Количество должно быть больше нуля."))
            }
        }
        return try {
            for (item in items) {
                val product = productDao.getById(item.productId)
                    ?: return Result.failure(IllegalArgumentException("Товар не найден (ID: ${item.productId})."))
                if (product.stock < item.quantity) {
                    return Result.failure(IllegalArgumentException(
                        "Недостаточно товара на складе. Доступно: ${product.stock} шт."
                    ))
                }
            }
            val totalPrice = items.sumOf { it.price * it.quantity }
            val order = OrderEntity(
                createdAt = System.currentTimeMillis(),
                totalPrice = totalPrice,
                status = OrderStatus.NEW.value
            )
            val orderItems = items.map { input ->
                val product = productDao.getById(input.productId)!!
                OrderItemEntity(
                    orderId = 0,
                    productId = input.productId,
                    productName = product.name,
                    price = product.price,
                    quantity = input.quantity
                )
            }
            val orderId = orderDao.createOrderWithItems(order, orderItems)
            for (item in items) {
                val product = productDao.getById(item.productId)!!
                productDao.update(product.copy(stock = product.stock - item.quantity))
            }
            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Ошибка при создании заказа: ${e.message}"))
        }
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: OrderStatus): Result<Unit> {
        val order = orderDao.getOrderById(orderId)
            ?: return Result.failure(IllegalArgumentException("Заказ не найден."))
        val currentStatus = OrderStatus.fromValue(order.status)
        if (!canTransitionTo(currentStatus, newStatus)) {
            return Result.failure(IllegalArgumentException(
                "Недопустимый переход статуса: ${currentStatus.displayName} → ${newStatus.displayName}"
            ))
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
            return Result.failure(IllegalArgumentException("Можно отменить только активные заказы (Новый или В обработке)."))
        }
        return try {
            val freshOrder = orderDao.getOrderById(orderId)
                ?: return Result.failure(IllegalArgumentException("Заказ не найден."))
            if (OrderStatus.fromValue(freshOrder.status) == OrderStatus.CANCELLED) {
                return Result.failure(IllegalArgumentException("Заказ уже отменён (проверка в транзакции)."))
            }
            val items = orderDao.getItemsByOrderId(orderId)
            for (item in items) {
                val product = productDao.getById(item.productId)
                if (product != null) {
                    productDao.update(product.copy(stock = product.stock + item.quantity))
                }
            }
            orderDao.updateOrder(freshOrder.copy(status = OrderStatus.CANCELLED.value))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Ошибка при отмене заказа: ${e.message}"))
        }
    }

    suspend fun countOrders(): Int = orderDao.countOrders()

    val availableProducts: Flow<List<ProductEntity>> = productDao.getAll().map { list -> list.filter { it.stock > 0 } }

    companion object {
        fun create(database: AppDatabase): OrderRepository =
            OrderRepository(database.orderDao(), database.productDao())
    }
}

data class OrderItemInput(
    val productId: Long,
    val quantity: Int,
    val price: Long
)
