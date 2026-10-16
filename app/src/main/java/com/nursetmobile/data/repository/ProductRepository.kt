package com.nursetmobile.data.repository

import com.nursetmobile.data.AppDatabase
import com.nursetmobile.data.dao.CategoryDao
import com.nursetmobile.data.dao.ProductDao
import com.nursetmobile.data.entity.CategoryEntity
import com.nursetmobile.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao
) {

    val allProducts: Flow<List<ProductEntity>> = productDao.getAll()

    val lowStockCount: Flow<Int> = productDao.getLowStockProducts().map { it.size }

    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAll()

    fun searchAndFilter(query: String, categoryId: Long?): Flow<List<ProductEntity>> {
        return if (query.trim().isNotEmpty() && categoryId != null) {
            productDao.searchByNameAndCategory(query, categoryId)
        } else if (query.trim().isNotEmpty()) {
            productDao.searchByName(query)
        } else if (categoryId != null) {
            productDao.getByCategory(categoryId)
        } else {
            allProducts
        }
    }

    suspend fun addProduct(
        name: String,
        categoryId: Long,
        price: Long,
        stock: Int
    ): Result<Long> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Название не может быть пустым."))
        }
        if (price <= 0) {
            return Result.failure(IllegalArgumentException("Цена должна быть больше нуля."))
        }
        if (stock < 0) {
            return Result.failure(IllegalArgumentException("Количество не может быть отрицательным."))
        }
        val category = categoryDao.getById(categoryId)
        if (category == null) {
            return Result.failure(IllegalArgumentException("Категория не найдена."))
        }
        val productsInCategory = productDao.getByCategory(categoryId).first()
        val duplicate = productsInCategory.find { it.name.equals(trimmedName, ignoreCase = true) }
        if (duplicate != null) {
            return Result.failure(IllegalArgumentException("Товар с таким названием уже существует в этой категории."))
        }
        val product = ProductEntity(trimmedName, categoryId, price, stock)
        val id = productDao.insert(product)
        return Result.success(id)
    }

    suspend fun updateProduct(
        id: Long,
        name: String,
        categoryId: Long,
        price: Long,
        stock: Int
    ): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Название не может быть пустым."))
        }
        if (price <= 0) {
            return Result.failure(IllegalArgumentException("Цена должна быть больше нуля."))
        }
        if (stock < 0) {
            return Result.failure(IllegalArgumentException("Количество не может быть отрицательным."))
        }
        val product = productDao.getById(id) ?: return Result.failure(IllegalArgumentException("Товар не найден."))
        val category = categoryDao.getById(categoryId)
        if (category == null) {
            return Result.failure(IllegalArgumentException("Категория не найдена."))
        }
        val productsInCategory = productDao.getByCategory(categoryId).first()
        val duplicate = productsInCategory.find { it.name.equals(trimmedName, ignoreCase = true) && it.id != id }
        if (duplicate != null) {
            return Result.failure(IllegalArgumentException("Товар с таким названием уже существует в этой категории."))
        }
        productDao.update(
            product.copy(
                name = trimmedName,
                categoryId = categoryId,
                price = price,
                stock = stock
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteProduct(id: Long): Result<Unit> {
        val product = productDao.getById(id) ?: return Result.failure(IllegalArgumentException("Товар не найден."))
        val activeOrdersCount = productDao.countActiveOrderItems(id)
        if (activeOrdersCount > 0) {
            return Result.failure(IllegalArgumentException("Нельзя удалить товар, который используется в активном заказе."))
        }
        productDao.deleteById(id)
        return Result.success(Unit)
    }

    companion object {
        fun create(database: AppDatabase): ProductRepository =
            ProductRepository(database.productDao(), database.categoryDao())
    }
}
