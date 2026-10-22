package com.nursetmobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OrderViewModel(private val repository: OrderRepository) : ViewModel() {
    val clients = repository.clients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _selectedClientId = MutableStateFlow<Long?>(null)
    val selectedClientId = _selectedClientId.asStateFlow()
    fun selectClient(id: Long) { _selectedClientId.value = id }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    val orders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableProducts: StateFlow<List<ProductEntity>> = repository.availableProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedItems = MutableStateFlow<List<CartItem>>(emptyList())
    val selectedItems: StateFlow<List<CartItem>> = _selectedItems.asStateFlow()

    private val _cartTotal = MutableStateFlow(0L)
    val cartTotal: StateFlow<Long> = _cartTotal.asStateFlow()

    fun addToCart(product: ProductEntity, quantity: Int = 1) {
        if (quantity <= 0 || _isLoading.value) return
        val currentItems = _selectedItems.value.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.productId == product.id }

        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            if (quantity > product.stock - existing.quantity) {
                _error.value = "Недостаточно товара на складе. Доступно: ${product.stock} шт."
                return
            }
            currentItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
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
        setCartItems(currentItems)
    }

    fun updateCartItemQuantity(productId: Long, quantity: Int) {
        if (_isLoading.value) return
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
            setCartItems(currentItems)
        }
    }

    fun removeFromCart(productId: Long) {
        if (_isLoading.value) return
        val currentItems = _selectedItems.value.filterNot { it.productId == productId }
        setCartItems(currentItems)
    }

    fun clearCart() {
        setCartItems(emptyList())
    }

    private fun setCartItems(items: List<CartItem>) {
        val total = try {
            items.fold(0L) { sum, item -> Math.addExact(sum, Math.multiplyExact(item.price, item.quantity.toLong())) }
        } catch (_: ArithmeticException) {
            _error.value = "Слишком большая сумма заказа. Уменьшите количество или проверьте цену."
            return
        }
        _selectedItems.value = items
        _cartTotal.value = total
    }

    fun createOrder() {
        if (_isLoading.value || _success.value != null) return
        val clientId = _selectedClientId.value
        if (clientId == null) {
            _error.value = "Выберите клиента. Если его нет в списке, добавьте во вкладке «Клиенты»."
            return
        }
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
            val result = try {
                repository.createOrder(orderItems, clientId)
            } finally {
                _isLoading.value = false
            }

            result.onSuccess { orderId ->
                _success.value = "Заказ #%03d создан успешно.".format(orderId)
                clearCart()
            }.onFailure { _error.value = it.message }
        }
    }

    fun updateOrderStatus(orderId: Long, newStatus: OrderStatus) {
        runOrderAction("Статус заказа изменён на ${newStatus.displayName}.") {
            repository.updateOrderStatus(orderId, newStatus)
        }
    }

    fun cancelOrder(orderId: Long) {
        runOrderAction("Заказ отменён, товары возвращены на склад.") {
            repository.cancelOrder(orderId)
        }
    }

    private fun runOrderAction(message: String, action: suspend () -> Result<Unit>) {
        if (_isLoading.value) return
        _isLoading.value = true
        _error.value = null
        _success.value = null
        viewModelScope.launch {
            try {
                action().onSuccess { _success.value = message }
                    .onFailure { _error.value = it.message ?: "Не удалось изменить заказ." }
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getOrderWithItems(orderId: Long): Pair<OrderEntity?, List<OrderItemEntity>> =
        repository.getOrderWithItems(orderId)

    fun clearMessages() {
        _error.value = null
        _success.value = null
    }

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
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
