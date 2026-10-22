package com.nursetmobile.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * Unit-тесты чистой функции formatPrice.
 * Явно задаём локаль, чтобы тест не зависел от настроек машины.
 */
class UtilsTest {

    @Test
    fun `цена с пробелами-разделителями групп`() {
        val locales = listOf(Locale.US, Locale.forLanguageTag("ru-RU"), Locale.forLanguageTag("kk-KZ"))
        for (locale in locales) {
            Locale.setDefault(locale)
            assertEquals("1 000 ₸", formatPrice(1000))
            assertEquals("1 234 567 ₸", formatPrice(1234567))
        }
    }

    @Test
    fun `цена без разделителей`() {
        assertEquals("0 ₸", formatPrice(0))
        assertEquals("999 ₸", formatPrice(999))
    }

    @Test
    fun `большая цена`() {
        assertEquals("12 345 678 ₸", formatPrice(12345678))
    }
}
