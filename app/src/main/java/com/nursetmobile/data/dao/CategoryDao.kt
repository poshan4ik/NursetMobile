package com.nursetmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nursetmobile.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для категорий.
 */
@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity): Int

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE lower(name) = lower(:name)")
    suspend fun findByName(name: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM products WHERE categoryId = :categoryId")
    suspend fun countProductsInCategory(categoryId: Long): Int

    @Query("SELECT * FROM categories WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<CategoryEntity>
}