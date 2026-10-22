package com.nursetmobile.data.repository

import com.nursetmobile.data.dao.ClientDao
import com.nursetmobile.data.entity.ClientEntity

class ClientRepository(private val dao: ClientDao) {
    val clients = dao.getAll()

    suspend fun register(name: String, phone: String): Result<Long> {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        if (trimmedName.isBlank()) return Result.failure(IllegalArgumentException("Введите имя клиента."))
        if (trimmedPhone.count { it.isDigit() } !in 7..15 ||
            trimmedPhone.any { !it.isDigit() && it !in "+()- " }) {
            return Result.failure(IllegalArgumentException("Введите телефон: от 7 до 15 цифр."))
        }
        return try {
            Result.success(dao.insert(ClientEntity(name = trimmedName, phone = trimmedPhone)))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
