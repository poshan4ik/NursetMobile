package com.nursetmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
}
