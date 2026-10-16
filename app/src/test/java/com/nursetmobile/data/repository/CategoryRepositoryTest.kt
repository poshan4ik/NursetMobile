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
 * Интеграционные тесты CategoryRepository с in-memory Room (Robolectric).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: CategoryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CategoryRepository.create(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `добавление категории успешно`() = runBlocking {
        val result = repository.addCategory("Тестовая категория")
        assertTrue(result.isSuccess)
        val id = result.getOrThrow()
        assertTrue(id > 0)
    }

    @Test
    fun `добавление категории с пустым именем — ошибка`() = runBlocking {
        val result = repository.addCategory("   ")
        assertFalse(result.isSuccess)
        assertEquals("Название не может быть пустым.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `добавление дубликата категории — ошибка без учёта регистра`() = runBlocking {
        repository.addCategory("Смартфоны")
        val result = repository.addCategory("смартфоны") // другой регистр
        assertFalse(result.isSuccess)
        assertEquals("Категория с таким названием уже существует.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `обновление категории успешно`() = runBlocking {
        val addResult = repository.addCategory("Старая категория")
        val id = addResult.getOrThrow()

        val updateResult = repository.updateCategory(id, "Новая категория")
        assertTrue(updateResult.isSuccess)

        val updated = database.categoryDao().getById(id)
        assertEquals("Новая категория", updated?.name)
    }

    @Test
    fun `удаление пустой категории успешно`() = runBlocking {
        val addResult = repository.addCategory("Для удаления")
        val id = addResult.getOrThrow()

        val deleteResult = repository.deleteCategory(id)
        assertTrue(deleteResult.isSuccess)

        val deleted = database.categoryDao().getById(id)
        assertEquals(null, deleted)
    }

    @Test
    fun `удаление категории с товарами — ошибка`() = runBlocking {
        // Создаём категорию
        val catResult = repository.addCategory("Категория с товаром")
        val catId = catResult.getOrThrow()

        // Добавляем товар в эту категорию
        val product = ProductEntity("Тестовый товар", catId, 1000, 5)
        database.productDao().insert(product)

        // Пытаемся удалить категорию
        val deleteResult = repository.deleteCategory(catId)
        assertFalse(deleteResult.isSuccess)
        assertEquals(
            "Нельзя удалить категорию, поскольку в ней находятся товары.",
            deleteResult.exceptionOrNull()?.message
        )
    }

    @Test
    fun `все категории загружаются из потока`() = runBlocking {
        repository.addCategory("Первая")
        repository.addCategory("Вторая")

        val categories = repository.allCategories.first()
        assertTrue(categories.size >= 2)
    }
}
