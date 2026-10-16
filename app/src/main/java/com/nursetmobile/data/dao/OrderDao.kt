package com.nursetmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nursetmobile.data.entity.OrderEntity
import com.nursetmobile.data.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для заказов и позиций заказов.
 */
@Dao
interface OrderDao {

    // Orders
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity): Int

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderById(id: Long): OrderEntity?

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun countOrders(): Int

    // Order Items
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrderItem(item: OrderItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsByOrderId(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE id = :id")
    suspend fun getItemById(id: Long): OrderItemEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId AND productId = :productId")
    suspend fun findItemByOrderAndProduct(orderId: Long, productId: Long): OrderItemEntity?

    @Query("DELETE FROM order_items WHERE id = :id")
    suspend fun deleteItemById(id: Long): Int

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteAllItemsByOrderId(orderId: Long): Int

    @Query("UPDATE order_items SET quantity = :quantity WHERE id = :id")
    suspend fun updateItemQuantity(id: Long, quantity: Int): Int

    // Транзакционные операции
    @Transaction
    suspend fun createOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>): Long {
        val orderId = insertOrder(order)
        items.forEach { it.orderId = orderId }
        insertOrderItems(items)
        return orderId
    }

    @Transaction
    suspend fun getOrderWithItems(orderId: Long): Pair<OrderEntity?, List<OrderItemEntity>> {
        val order = getOrderById(orderId)
        val items = if (order != null) getItemsByOrderId(orderId) else emptyList()
        return order to items
    }

    // Для проверки активных заказов при удалении товара
    @Query("""
        SELECT DISTINCT oi.productId FROM order_items oi
        JOIN orders o ON oi.orderId = o.id
        WHERE o.status IN (0, 1) -- NEW, PROCESSING
    """)
    suspend fun getProductIdsInActiveOrders(): List<Long>
}