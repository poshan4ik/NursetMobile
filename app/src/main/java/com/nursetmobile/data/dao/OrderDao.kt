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

@Dao
interface OrderDao {

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsByOrderId(orderId: Long): List<OrderItemEntity>

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
}