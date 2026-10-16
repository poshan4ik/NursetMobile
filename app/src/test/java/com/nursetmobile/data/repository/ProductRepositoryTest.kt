package com.nursetmobile.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nursetmobile.data.AppDatabase
import com.nursetmobile.data.entity.CategoryEntity
import com.nursetmobile.data.entity.ProductEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Интеграционные тесты ProductRepository с in-memory Room (Robolectric).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: ProductRepository
    private var categoryId: Long = 0

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ProductRepository.create(database)

        runBlocking {
            // Создаём категорию для тестов
            categoryId = database.categoryDao().insert(CategoryEntity("Тестовая категория"))
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `добавление товара успешно`() = runBlocking {
        val result = repository.addProduct("Тестовый товар", categoryId, 1000, 5)
        assertTrue(result.isSuccess)
        val id = result.getOrThrow()
        assertTrue(id > 0)
    }

    @Test
    fun `добавление товара с пустым именем — ошибка`() = runBlocking {
        val result = repository.addProduct("   ", categoryId, 1000, 5)
        assertFalse(result.isSuccess)
        assertEquals("Название не может быть пустым.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `добавление товара с нулевой ценой — ошибка`() = runBlocking {
        val result = repository.addProduct("Товар", categoryId, 0, 5)
        assertFalse(result.isSuccess)
        assertEquals("Цена должна быть больше нуля.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `добавление товара с отрицательным остатком — ошибка`() = runBlocking {
        val result = repository.addProduct("Товар", categoryId, 1000, -1)
        assertFalse(result.isSuccess)
        assertEquals("Количество не может быть отрицательным.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `добавление товара в несуществующую категорию — ошибка`() = runBlocking {
        val result = repository.addProduct("Товар", 999999, 1000, 5)
        assertFalse(result.isSuccess)
        assertEquals("Категория не найдена.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `добавление дубликата названия в той же категории — ошибка без учёта регистра`() =
        runBlocking {
            repository.addProduct("Смартфон", categoryId, 1000, 5)
            val result = repository.addProduct("смартфон", categoryId, 2000, 3)
            assertFalse(result.isSuccess)
            assertEquals(
                "Товар с таким названием уже существует в этой категории.",
                result.exceptionOrNull()?.message
            )
        }

    @Test
    fun `обновление товара успешно`() = runBlocking {
        val addResult = repository.addProduct("Старый товар", categoryId, 1000, 5)
        val id = addResult.getOrThrow()

        val updateResult = repository.updateProduct(id, "Новый товар", categoryId, 2000, 10)
        assertTrue(updateResult.isSuccess)

        val updated = database.productDao().getById(id)
        assertEquals("Новый товар", updated?.name)
        assertEquals(2000L, updated?.price)
        assertEquals(10, updated?.stock)
    }

    @Test
    fun `удаление товара без активных заказов — успешно`() = runBlocking {
        val addResult = repository.addProduct("Для удаления", categoryId, 1000, 5)
        val id = addResult.getOrThrow()

        val deleteResult = repository.deleteProduct(id)
        assertTrue(deleteResult.isSuccess)

        val deleted = database.productDao().getById(id)
        assertEquals(null, deleted)
    }

    @Test
    fun `поиск товаров по названию работает`() = runBlocking {
        repository.addProduct("iPhone 15", categoryId, 1000, 5)
        repository.addProduct("Samsung Galaxy", categoryId, 2000, 3)
        repository.addProduct("iPhone 14", categoryId, 1500, 2)

        val results = repository.searchByName("iPhone").first()
        assertEquals(2, results.size)
        assertTrue(results.any { it.name.contains("iPhone") })
    }
}
