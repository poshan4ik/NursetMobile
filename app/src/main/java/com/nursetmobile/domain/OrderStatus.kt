package com.nursetmobile.domain

enum class OrderStatus(val value: Int, val displayName: String) {
    NEW(0, "Новый"),
    PROCESSING(1, "В обработке"),
    COMPLETED(2, "Выполнен"),
    CANCELLED(3, "Отменён");

    companion object {
        fun fromValue(value: Int): OrderStatus = values().firstOrNull { it.value == value } ?: NEW
    }
}

// Допустимые переходы статусов заказа (честная функция — тестируется обычным JUnit)
fun canTransitionTo(current: OrderStatus, next: OrderStatus): Boolean {
    return when (current) {
        OrderStatus.NEW -> next in setOf(OrderStatus.PROCESSING, OrderStatus.CANCELLED)
        OrderStatus.PROCESSING -> next in setOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED)
        OrderStatus.COMPLETED, OrderStatus.CANCELLED -> false
    }
}

// Активным считается заказ, который ещё можно отменить
fun isActive(status: OrderStatus): Boolean = status in setOf(OrderStatus.NEW, OrderStatus.PROCESSING)