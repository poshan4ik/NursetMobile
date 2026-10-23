package com.nursetmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nursetmobile.domain.phoneDigits
import com.nursetmobile.data.entity.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name, id")
    fun getAll(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getById(id: Long): ClientEntity?

    @Insert
    suspend fun insert(client: ClientEntity): Long

    @Update
    suspend fun update(client: ClientEntity): Int

    @Query("SELECT * FROM clients")
    suspend fun getSnapshot(): List<ClientEntity>

    @Transaction
    suspend fun saveUnique(client: ClientEntity): Long {
        require(getSnapshot().none { it.id != client.id && phoneDigits(it.phone) == phoneDigits(client.phone) }) {
            "Клиент с таким телефоном уже есть. Найдите его в списке."
        }
        if (client.id == 0L) return insert(client)
        require(update(client) == 1) { "Клиент не найден." }
        return client.id
    }
}
