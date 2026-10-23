package com.nursetmobile.domain

import com.nursetmobile.data.entity.OrderEntity

enum class OrderFilter(val label: String) {
    ALL("Все"), ACTIVE("Активные"), NEW("Новые"), PROCESSING("В обработке"),
    COMPLETED("Выполненные"), CANCELLED("Отменённые");

    fun matches(order: OrderEntity): Boolean = when (this) {
        ALL -> true
        ACTIVE -> order.status == OrderStatus.NEW.value || order.status == OrderStatus.PROCESSING.value
        NEW -> order.status == OrderStatus.NEW.value
        PROCESSING -> order.status == OrderStatus.PROCESSING.value
        COMPLETED -> order.status == OrderStatus.COMPLETED.value
        CANCELLED -> order.status == OrderStatus.CANCELLED.value
    }
}

fun filterOrders(orders: List<OrderEntity>, query: String, filter: OrderFilter): List<OrderEntity> {
    val text = query.trim().removePrefix("#")
    return orders.filter { order ->
        filter.matches(order) && (text.isEmpty() || order.clientName.contains(text, ignoreCase = true) ||
            order.id.toString().padStart(3, '0').contains(text))
    }
}
