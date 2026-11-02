package com.nursetmobile.domain

enum class ProductStockFilter(val label: String) {
    ALL("Все остатки"),
    AVAILABLE("В наличии"),
    LOW("Заканчиваются"),
    OUT_OF_STOCK("Нет в наличии");

    fun matches(stock: Int): Boolean = when (this) {
        ALL -> true
        AVAILABLE -> stock > 0
        LOW -> stock in 1..3
        OUT_OF_STOCK -> stock == 0
    }
}
