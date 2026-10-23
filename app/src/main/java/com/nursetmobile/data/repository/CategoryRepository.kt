package com.nursetmobile.data.repository

import com.nursetmobile.data.AppDatabase
import com.nursetmobile.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CategoryRepository(private val database: AppDatabase) {
    private val dao = database.categoryDao()

    suspend fun addCategory(name: String): Result<Long> = database.mutate { addCategoryValidated(name) }
    suspend fun updateCategory(id: Long, name: String): Result<Unit> = database.mutate { updateCategoryValidated(id, name) }
    suspend fun deleteCategory(id: Long): Result<Unit> = database.mutate { deleteCategoryValidated(id) }

    val allCategories: Flow<List<CategoryEntity>> = dao.getAll()

    private suspend fun addCategoryValidated(name: String): Result<Long> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Название не может быть пустым."))
        }
        // Сравниваем в Kotlin: SQLite lower() не обрабатывает кириллицу
        val existing = dao.getAll().first().firstOrNull { it.name.equals(trimmedName, ignoreCase = true) }
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Категория с таким названием уже существует."))
        }
        val id = dao.insert(CategoryEntity(trimmedName))
        return Result.success(id)
    }

    private suspend fun updateCategoryValidated(id: Long, name: String): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Название не может быть пустым."))
        }
        val existing = dao.getById(id) ?: return Result.failure(IllegalArgumentException("Категория не найдена."))
        val duplicate = dao.getAll().first().firstOrNull {
            it.name.equals(trimmedName, ignoreCase = true) && it.id != id
        }
        if (duplicate != null) {
            return Result.failure(IllegalArgumentException("Категория с таким названием уже существует."))
        }
        dao.update(existing.copy(name = trimmedName))
        return Result.success(Unit)
    }

    private suspend fun deleteCategoryValidated(id: Long): Result<Unit> {
        val category = dao.getById(id) ?: return Result.failure(IllegalArgumentException("Категория не найдена."))
        val productsCount = dao.countProductsInCategory(id)
        if (productsCount > 0) {
            return Result.failure(IllegalArgumentException("Нельзя удалить категорию, поскольку в ней находятся товары."))
        }
        dao.deleteById(id)
        return Result.success(Unit)
    }

    companion object {
        fun create(database: AppDatabase): CategoryRepository = CategoryRepository(database)
    }
}
