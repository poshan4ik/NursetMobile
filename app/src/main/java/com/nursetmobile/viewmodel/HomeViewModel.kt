package com.nursetmobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nursetmobile.data.AppDatabase
import com.nursetmobile.data.entity.OrderEntity
import com.nursetmobile.data.repository.OrderRepository
import com.nursetmobile.data.repository.ProductRepository
import com.nursetmobile.domain.OrderStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeSummary(
    val productsCount: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val activeOrders: List<OrderEntity> = emptyList()
)

class HomeViewModel(productRepository: ProductRepository, orderRepository: OrderRepository) : ViewModel() {
    val summary = combine(productRepository.allProducts, orderRepository.allOrders) { products, orders ->
        HomeSummary(
            productsCount = products.size,
            lowStockCount = products.count { it.stock in 1..3 },
            outOfStockCount = products.count { it.stock == 0 },
            activeOrders = orders.filter {
                it.status == OrderStatus.NEW.value || it.status == OrderStatus.PROCESSING.value
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeSummary())

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(
            ProductRepository.create(database), OrderRepository.create(database)
        ) as T
    }
}
