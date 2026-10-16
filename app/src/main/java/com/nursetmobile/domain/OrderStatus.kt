package com.nursetmobile.domain

/**
 * Статусы заказа.
 * NEW=0, PROCESSING=1, COMPLETED=2, CANCELLED=3
 */
enum class OrderStatus(val value: Int, val displayName: String) {
    NEW(0, "Новый"),
    PROCESSING(1, "В обработке"),
    COMPLETED(2, "Выполнен"),
    CANCELLED(3, "Отменён");

    companion object {
        fun fromValue(value: Int): OrderStatus = values().firstOrNull { it.value == value } ?: NEW
    }
}

/**
 * Проверка допустимости перехода статуса.
 * Чистая функция для unit-тестирования.
 */
fun canTransitionTo(current: OrderStatus, next: OrderStatus): Boolean {
    return when (current) {
        OrderStatus.NEW -> next in setOf(OrderStatus.PROCESSING, OrderStatus.CANCELLED)
        OrderStatus.PROCESSING -> next in setOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED)
        OrderStatus.COMPLETED, OrderStatus.CANCELLED -> false
    }
}

/**
 * Проверка, является ли заказ "активным" (можно отменить).
 */
fun isActive(status: OrderStatus): Boolean = status in setOf(OrderStatus.NEW, OrderStatus.PROCESSING)