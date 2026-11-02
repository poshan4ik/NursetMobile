package com.nursetmobile.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductStockFilterTest {
    @Test fun stockBoundariesAreDistinct() {
        assertTrue(ProductStockFilter.OUT_OF_STOCK.matches(0))
        assertFalse(ProductStockFilter.LOW.matches(0))
        assertTrue(ProductStockFilter.LOW.matches(1))
        assertTrue(ProductStockFilter.LOW.matches(3))
        assertFalse(ProductStockFilter.LOW.matches(4))
        assertFalse(ProductStockFilter.AVAILABLE.matches(0))
        assertTrue(ProductStockFilter.AVAILABLE.matches(4))
        assertTrue(ProductStockFilter.ALL.matches(0))
        assertTrue(ProductStockFilter.ALL.matches(Int.MAX_VALUE))
    }
}
