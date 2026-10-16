package com.nursetmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nursetmobile.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для товаров.
 */
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

    @Query("SELECT * FROM products WHERE lower(name) LIKE '%' || lower(:query) || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchByName(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY name COLLATE NOCASE ASC")
    fun getByCategory(categoryId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND lower(name) LIKE '%' || lower(:query) || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchByNameAndCategory(query: String, categoryId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stock <= 3 AND stock > 0")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products WHERE stock <= 3 AND stock > 0")
    suspend fun countLowStockProducts(): Int

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id AND stock >= :quantity")
    suspend fun checkStock(id: Long, quantity: Int): ProductEntity?

    // Для проверки: используется ли товар в активных заказах
    @Query("""
        SELECT COUNT(*) FROM order_items oi
        JOIN orders o ON oi.orderId = o.id
        WHERE oi.productId = :productId AND o.status IN (0, 1) -- NEW, PROCESSING
    """)
    suspend fun countActiveOrderItems(productId: Long): Int
}