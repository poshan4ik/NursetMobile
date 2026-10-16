package com.nursetmobile.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность товара.
 * - price: Long (тенге)
 * - stock: Int (количество на складе)
 * - categoryId: внешний ключ на категории (CASCADE удаление категории запрещено на уровне БД, проверяем в Repository)
 */
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.NO_ACTION // Удаление категории контролируется в Repository
        )
    ],
    indices = [Index("categoryId")]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val price: Long,    // в тенге
    val stock: Int      // остаток на складе
) {
    constructor(name: String, categoryId: Long, price: Long, stock: Int) : this(0, name, categoryId, price, stock)
}