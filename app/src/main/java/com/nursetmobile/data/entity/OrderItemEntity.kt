package com.nursetmobile.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Нет FK на ProductEntity — намеренно, чтобы товар можно было удалить из каталога,
// сохранив историю заказов (название и цена хранятся как снимок на момент заказа)
@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId"), Index("productId")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var orderId: Long,
    val productId: Long,
    val productName: String,
    val price: Long,
    val quantity: Int
) {
    constructor(orderId: Long, productId: Long, productName: String, price: Long, quantity: Int) :
        this(0, orderId, productId, productName, price, quantity)
}