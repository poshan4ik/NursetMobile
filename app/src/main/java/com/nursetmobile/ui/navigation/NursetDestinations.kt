package com.nursetmobile.ui.navigation

/**
 * Sealed-класс для навигационных маршрутов (type-safe navigation).
 * Каждый маршрут соответствует экрану приложения.
 */
sealed interface NursetDestination {

    /** Главный экран со статистикой */
    data object Home : NursetDestination

    /** Список товаров с поиском и фильтром (добавление/редактирование через диалоги) */
    data object Products : NursetDestination

    /** Список категорий */
    data object Categories : NursetDestination

    /** Список заказов */
    data object Orders : NursetDestination

    /** Создание нового заказа */
    data object CreateOrder : NursetDestination

    /** Просмотр деталей заказа */
    data class OrderDetails(val orderId: Long) : NursetDestination

    /**
     * Преобразует маршрут в строковый путь для Navigation Compose.
     */
    fun route(): String = when (this) {
        is Home -> "home"
        is Products -> "products"
        is Categories -> "categories"
        is Orders -> "orders"
        is CreateOrder -> "create_order"
        is OrderDetails -> "order_details/$orderId"
    }

    companion object {
        /**
         * Парсит строку пути обратно в маршрут (для deep links, если понадобится).
         */
        fun parse(route: String): NursetDestination {
            val parts = route.split("/")
            return when (parts[0]) {
                "home" -> Home
                "products" -> Products
                "categories" -> Categories
                "orders" -> Orders
                "create_order" -> CreateOrder
                "order_details" -> OrderDetails(parts[1].toLong())
                else -> throw IllegalArgumentException("Unknown route: $route")
            }
        }
    }
}