package com.nursetmobile.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nursetmobile.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClientRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ClientRepository

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = ClientRepository(database.clientDao())
    }
    @After fun close() { database.close() }

    @Test fun `регистрация сохраняет имя и телефон без пробелов по краям`() = runBlocking {
        val id = repository.register("  Анна Иванова  ", " +7 (999) 123-45-67 ").getOrThrow()
        val client = database.clientDao().getById(id)!!
        assertEquals("Анна Иванова", client.name)
        assertEquals("+7 (999) 123-45-67", client.phone)
        assertEquals(1, repository.clients.first().size)
    }

    @Test fun `пустое имя и неверный телефон не сохраняются`() = runBlocking {
        assertTrue(repository.register(" ", "79991234567").isFailure)
        listOf("", "123", "abcdefghijk", "79991234567abc", "1234567890123456", "7+9991234567", "++79991234567").forEach {
            assertTrue(repository.register("Анна", it).isFailure)
        }
        assertTrue(repository.clients.first().isEmpty())
    }

    @Test fun `одинаковые телефоны с разным форматированием не дублируются`() = runBlocking {
        repository.register("Анна", "+7 (999) 123-45-67").getOrThrow()
        assertTrue(repository.register("Аня", "79991234567").isFailure)
        assertEquals(1, repository.clients.first().size)
    }

    @Test fun `одновременная регистрация одного телефона создаёт одного клиента`() = runBlocking {
        val first = async(Dispatchers.Default) { repository.register("Анна", "+7 (999) 123-45-67") }
        val second = async(Dispatchers.Default) { repository.register("Аня", "79991234567") }
        assertEquals(1, listOf(first.await(), second.await()).count { it.isSuccess })
        assertEquals(1, repository.clients.first().size)
    }

    @Test fun `редактирование сохраняет id и разрешает свой телефон`() = runBlocking {
        val id = repository.register("Анна", "79991234567").getOrThrow()
        repository.update(id, "Анна Иванова", "+7 (999) 123-45-67").getOrThrow()
        assertEquals("Анна Иванова", database.clientDao().getById(id)?.name)
        assertEquals(1, repository.clients.first().size)
        val other = repository.register("Борис", "78881234567").getOrThrow()
        assertTrue(repository.update(other, "Борис", "79991234567").isFailure)
        assertEquals("78881234567", database.clientDao().getById(other)?.phone)
    }

    @Test fun `редактирование отсутствующего клиента не создаёт нового`() = runBlocking {
        assertTrue(repository.update(999, "Анна", "79991234567").isFailure)
        assertTrue(repository.update(0, "Анна", "79991234567").isFailure)
        assertTrue(repository.clients.first().isEmpty())
    }
}
