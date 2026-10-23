package com.nursetmobile.data.repository

import androidx.room.withTransaction
import com.nursetmobile.data.AppDatabase
import kotlinx.coroutines.CancellationException

internal suspend fun <T> AppDatabase.mutate(block: suspend () -> Result<T>): Result<T> = try {
    withTransaction { block() }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
