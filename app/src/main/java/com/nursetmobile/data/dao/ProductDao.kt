package com.nursetmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nursetmobile.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity): Int

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    // lower() не обрабатывает кириллицу, поэтому дубликаты дополнительно проверяем в Kotlin
    @Query("SELECT * FROM products WHERE lower(name) LIKE '%' || lower(:query) || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchByName(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY name COLLATE NOCASE ASC")
    fun getByCategory(categoryId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND lower(name) LIKE '%' || lower(:query) || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchByNameAndCategory(query: String, categoryId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stock <= 3 AND stock > 0")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    // Проверка: используется ли товар в активных заказах (NEW, PROCESSING)
    @Query("""
        SELECT COUNT(*) FROM order_items oi
        JOIN orders o ON oi.orderId = o.id
        WHERE oi.productId = :productId AND o.status IN (0, 1)
    """)
    suspend fun countActiveOrderItems(productId: Long): Int
}