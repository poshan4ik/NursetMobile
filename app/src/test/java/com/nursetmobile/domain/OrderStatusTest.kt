package com.nursetmobile.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-тесты для правил перехода статусов заказа.
 * Чистые функции — тестируются без Android, без Room, без эмулятора.
 */
class OrderStatusTest {

    @Test
    fun `NEW можно перевести в PROCESSING`() {
        assertTrue(canTransitionTo(OrderStatus.NEW, OrderStatus.PROCESSING))
    }

    @Test
    fun `NEW можно перевести в CANCELLED`() {
        assertTrue(canTransitionTo(OrderStatus.NEW, OrderStatus.CANCELLED))
    }

    @Test
    fun `NEW НЕльзя перевести в COMPLETED`() {
        assertFalse(canTransitionTo(OrderStatus.NEW, OrderStatus.COMPLETED))
    }

    @Test
    fun `NEW НЕльзя перевести в NEW`() {
        assertFalse(canTransitionTo(OrderStatus.NEW, OrderStatus.NEW))
    }

    @Test
    fun `PROCESSING можно перевести в COMPLETED`() {
        assertTrue(canTransitionTo(OrderStatus.PROCESSING, OrderStatus.COMPLETED))
    }

    @Test
    fun `PROCESSING можно перевести в CANCELLED`() {
        assertTrue(canTransitionTo(OrderStatus.PROCESSING, OrderStatus.CANCELLED))
    }

    @Test
    fun `PROCESSING НЕльзя перевести в NEW`() {
        assertFalse(canTransitionTo(OrderStatus.PROCESSING, OrderStatus.NEW))
    }

    @Test
    fun `PROCESSING НЕльзя перевести в PROCESSING`() {
        assertFalse(canTransitionTo(OrderStatus.PROCESSING, OrderStatus.PROCESSING))
    }

    @Test
    fun `COMPLETED НЕльзя перевести никуда`() {
        assertFalse(canTransitionTo(OrderStatus.COMPLETED, OrderStatus.NEW))
        assertFalse(canTransitionTo(OrderStatus.COMPLETED, OrderStatus.PROCESSING))
        assertFalse(canTransitionTo(OrderStatus.COMPLETED, OrderStatus.CANCELLED))
        assertFalse(canTransitionTo(OrderStatus.COMPLETED, OrderStatus.COMPLETED))
    }

    @Test
    fun `CANCELLED НЕльзя перевести никуда`() {
        assertFalse(canTransitionTo(OrderStatus.CANCELLED, OrderStatus.NEW))
        assertFalse(canTransitionTo(OrderStatus.CANCELLED, OrderStatus.PROCESSING))
        assertFalse(canTransitionTo(OrderStatus.CANCELLED, OrderStatus.COMPLETED))
        assertFalse(canTransitionTo(OrderStatus.CANCELLED, OrderStatus.CANCELLED))
    }

    @Test
    fun `isActive возвращает true для NEW и PROCESSING`() {
        assertTrue(isActive(OrderStatus.NEW))
        assertTrue(isActive(OrderStatus.PROCESSING))
    }

    @Test
    fun `isActive возвращает false для COMPLETED и CANCELLED`() {
        assertFalse(isActive(OrderStatus.COMPLETED))
        assertFalse(isActive(OrderStatus.CANCELLED))
    }

    @Test
    fun `fromValue возвращает правильный enum`() {
        assertTrue(OrderStatus.fromValue(0) == OrderStatus.NEW)
        assertTrue(OrderStatus.fromValue(1) == OrderStatus.PROCESSING)
        assertTrue(OrderStatus.fromValue(2) == OrderStatus.COMPLETED)
        assertTrue(OrderStatus.fromValue(3) == OrderStatus.CANCELLED)
        // Неизвестное значение -> NEW по умолчанию
        assertTrue(OrderStatus.fromValue(99) == OrderStatus.NEW)
    }

    @Test
    fun `displayName на русском языке`() {
        assertTrue(OrderStatus.NEW.displayName == "Новый")
        assertTrue(OrderStatus.PROCESSING.displayName == "В обработке")
        assertTrue(OrderStatus.COMPLETED.displayName == "Выполнен")
        assertTrue(OrderStatus.CANCELLED.displayName == "Отменён")
    }
}