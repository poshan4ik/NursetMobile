package com.nursetmobile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// name проверяется на уникальность без учёта регистра в Repository
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
) {
    constructor(name: String) : this(0, name)
}