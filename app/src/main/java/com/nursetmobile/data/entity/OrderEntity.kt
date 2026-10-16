package com.nursetmobile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность заказа.
 * - createdAt: Long (millis since epoch)
 * - totalPrice: Long (сумма в тенге)
 * - status: Int (enum OrderStatus: NEW=0, PROCESSING=1, COMPLETED=2, CANCELLED=3)
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val totalPrice: Long = 0,
    val status: Int = 0 // OrderStatus.NEW
) {
    constructor(createdAt: Long, totalPrice: Long, status: Int) : this(0, createdAt, totalPrice, status)
}