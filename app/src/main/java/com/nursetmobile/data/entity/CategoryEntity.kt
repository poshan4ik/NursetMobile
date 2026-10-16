package com.nursetmobile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность категории товаров.
 * name — уникальное без учёта регистра (проверка на уровне DAO/Repository).
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
) {
    // Вспомогательный конструктор для создания новой категории без id
    constructor(name: String) : this(0, name)
}