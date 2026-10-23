package com.nursetmobile.data.repository

import com.nursetmobile.data.dao.ClientDao
import com.nursetmobile.data.entity.ClientEntity

class ClientRepository(private val dao: ClientDao) {
    val clients = dao.getAll()

    suspend fun register(name: String, phone: String): Result<Long> = save(0, name, phone)

    suspend fun update(id: Long, name: String, phone: String): Result<Long> {
        if (id <= 0) return Result.failure(IllegalArgumentException("Клиент не найден."))
        return save(id, name, phone)
    }

    private suspend fun save(id: Long, name: String, phone: String): Result<Long> {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        if (trimmedName.isBlank()) return Result.failure(IllegalArgumentException("Введите имя клиента."))
        if (trimmedPhone.count { it in '0'..'9' } !in 7..15 ||
            !trimmedPhone.matches(Regex("\\+?[0-9 ()-]+"))) {
            return Result.failure(IllegalArgumentException("Введите телефон: от 7 до 15 цифр."))
        }
        return try {
            Result.success(dao.saveUnique(ClientEntity(id = id, name = trimmedName, phone = trimmedPhone)))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
