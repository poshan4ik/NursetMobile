package com.nursetmobile.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность позиции заказа.
 * - productName и price сохраняются на момент создания заказа (snapshot).
 * - productId — просто число, БЕЗ жёсткого FOREIGN KEY на products,
 *   чтобы можно было удалить товар, который есть только в завершённых/отменённых заказах.
 * - Связь с orders — с каскадным удалением.
 */
@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
        // Нет ForeignKey на ProductEntity — намеренно по ТЗ
    ],
    indices = [Index("orderId"), Index("productId")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var orderId: Long,
    val productId: Long,
    val productName: String,  // snapshot названия на момент заказа
    val price: Long,          // snapshot цены на момент заказа (тенге)
    val quantity: Int         // количество в заказе
) {
    constructor(orderId: Long, productId: Long, productName: String, price: Long, quantity: Int) :
        this(0, orderId, productId, productName, price, quantity)
}