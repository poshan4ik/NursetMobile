package com.nursetmobile.domain

import com.nursetmobile.data.entity.ClientEntity
import com.nursetmobile.data.entity.OrderEntity
import org.junit.Assert.*
import org.junit.Test

class SearchTest {
    @Test fun `клиент находится по кириллице и форматированному номеру`() {
        val client = ClientEntity(name = "Анна Иванова", phone = "+7 (999) 123-45-67")
        assertTrue(client.matchesSearch("  ИВАНОВА "))
        assertTrue(client.matchesSearch("999123"))
        assertTrue(client.matchesSearch("(999) 123-45"))
        assertTrue(client.matchesSearch(""))
        assertFalse(client.matchesSearch("Борис 999"))
    }

    @Test fun `поиск заказа совмещается с фильтром и понимает номер с решёткой`() {
        val orders = listOf(
            OrderEntity(id = 1, clientName = "Анна", status = OrderStatus.NEW.value),
            OrderEntity(id = 2, clientName = "Анна", status = OrderStatus.COMPLETED.value),
            OrderEntity(id = 3, clientName = "Борис", status = OrderStatus.PROCESSING.value),
            OrderEntity(id = 4, clientName = "Борис", status = OrderStatus.CANCELLED.value))
        assertEquals(listOf(1L, 3L), filterOrders(orders, "", OrderFilter.ACTIVE).map { it.id })
        assertEquals(listOf(1L), filterOrders(orders, "  АННА  ", OrderFilter.ACTIVE).map { it.id })
        assertEquals(listOf(2L), filterOrders(orders, "#002", OrderFilter.ALL).map { it.id })
        assertTrue(filterOrders(orders, "#002", OrderFilter.NEW).isEmpty())
        assertEquals(listOf(4L), filterOrders(orders, "", OrderFilter.CANCELLED).map { it.id })
    }
}
