package com.nursetmobile.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nursetmobile.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
        listOf("", "123", "abcdefghijk", "79991234567abc", "1234567890123456").forEach {
            assertTrue(repository.register("Анна", it).isFailure)
        }
        assertTrue(repository.clients.first().isEmpty())
    }
}
