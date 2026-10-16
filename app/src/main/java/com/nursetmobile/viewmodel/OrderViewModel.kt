package com.nursetmobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nursetmobile.data.AppDatabase
import com.nursetmobile.data.repository.OrderRepository
import com.nursetmobile.data.repository.OrderItemInput
import com.nursetmobile.data.entity.OrderEntity
import com.nursetmobile.data.entity.OrderItemEntity
import com.nursetmobile.data.entity.ProductEntity
import com.nursetmobile.domain.OrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OrderViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    val orders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ordersCount: StateFlow<Int> = repository.allOrders
        .map { list -> list.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val availableProducts: StateFlow<List<ProductEntity>> = repository.availableProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedItems = MutableStateFlow<List<CartItem>>(emptyList())
    val selectedItems: StateFlow<List<CartItem>> = _selectedItems.asStateFlow()

    private val _cartTotal = MutableStateFlow(0L)
    val cartTotal: StateFlow<Long> = _cartTotal.asStateFlow()

    fun addToCart(product: ProductEntity, quantity: Int = 1) {
        val currentItems = _selectedItems.value.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.productId == product.id }

        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            val newQuantity = existing.quantity + quantity
            if (newQuantity > product.stock) {
                _error.value = "Недостаточно товара на складе. Доступно: ${product.stock} шт."
                return
            }
            currentItems[existingIndex] = existing.copy(quantity = newQuantity)
        } else {
            if (quantity > product.stock) {
                _error.value = "Недостаточно товара на складе. Доступно: ${product.stock} шт."
                return
            }
            currentItems.add(
                CartItem(
                    productId = product.id,
                    productName = product.name,
                    price = product.price,
                    quantity = quantity,
                    maxStock = product.stock
                )
            )
        }
        _selectedItems.value = currentItems
        recalculateTotal()
    }

    fun updateCartItemQuantity(productId: Long, quantity: Int) {
        val currentItems = _selectedItems.value.toMutableList()
        val index = currentItems.indexOfFirst { it.productId == productId }
        if (index >= 0) {
            if (quantity <= 0) {
                currentItems.removeAt(index)
            } else {
                val item = currentItems[index]
                if (quantity > item.maxStock) {
                    _error.value = "Недостаточно товара на складе. Доступно: ${item.maxStock} шт."
                    return
                }
                currentItems[index] = item.copy(quantity = quantity)
            }
            _selectedItems.value = currentItems
            recalculateTotal()
        }
    }

    fun removeFromCart(productId: Long) {
        val currentItems = _selectedItems.value.filterNot { it.productId == productId }
        _selectedItems.value = currentItems
        recalculateTotal()
    }

    fun clearCart() {
        _selectedItems.value = emptyList()
        recalculateTotal()
    }

    private fun recalculateTotal() {
        _cartTotal.value = _selectedItems.value.sumOf { it.price * it.quantity }
    }

    fun createOrder() {
        val items = _selectedItems.value
        if (items.isEmpty()) {
            _error.value = "Корзина пуста. Добавьте товары."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _success.value = null

            val orderItems = items.map { OrderItemInput(it.productId, it.quantity, it.price) }
            val result = repository.createOrder(orderItems)

            _isLoading.value = false
            result.onSuccess { orderId ->
                _success.value = "Заказ #%03d создан успешно.".format(orderId)
                clearCart()
            }.onFailure { _error.value = it.message }
        }
    }

    fun updateOrderStatus(orderId: Long, newStatus: OrderStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _success.value = null

            val result = repository.updateOrderStatus(orderId, newStatus)

            _isLoading.value = false
            result.onSuccess { _success.value = "Статус заказа изменён на ${newStatus.displayName}." }
                .onFailure { _error.value = it.message }
        }
    }

    fun cancelOrder(orderId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _success.value = null

            val result = repository.cancelOrder(orderId)

            _isLoading.value = false
            result.onSuccess { _success.value = "Заказ отменён, товары возвращены на склад." }
                .onFailure { _error.value = it.message }
        }
    }

    suspend fun getOrderWithItems(orderId: Long): Pair<OrderEntity?, List<OrderItemEntity>> =
        repository.getOrderWithItems(orderId)

    fun clearMessages() {
        _error.value = null
        _success.value = null
    }

    class Factory(private val database: AppDatabase) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrderViewModel(OrderRepository.create(database)) as T
        }
    }
}

data class CartItem(
    val productId: Long,
    val productName: String,
    val price: Long,
    val quantity: Int,
    val maxStock: Int
)
