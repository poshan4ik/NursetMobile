package com.nursetmobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nursetmobile.data.AppDatabase
import com.nursetmobile.data.entity.CategoryEntity
import com.nursetmobile.data.entity.ProductEntity
import com.nursetmobile.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    val products: StateFlow<List<ProductEntity>> = combine(
        _searchQuery,
        _selectedCategoryId
    ) { query, categoryId -> query to categoryId }
        .flatMapLatest { (query, categoryId) -> repository.searchAndFilter(query, categoryId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        repository.allCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockCount: StateFlow<Int> = repository.lowStockCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryFilterChange(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun addProduct(name: String, categoryId: Long, price: Long, stock: Int, onSaved: () -> Unit = {}) =
        runMutation(onSaved) { repository.addProduct(name, categoryId, price, stock) }

    fun updateProduct(id: Long, name: String, categoryId: Long, price: Long, stock: Int,
        expectedStock: Int? = null, onSaved: () -> Unit = {}) =
        runMutation(onSaved) { repository.updateProduct(id, name, categoryId, price, stock, expectedStock) }

    fun deleteProduct(id: Long) = runMutation({}) { repository.deleteProduct(id) }

    private fun <T> runMutation(onSaved: () -> Unit, action: suspend () -> Result<T>) {
        if (_isLoading.value) return
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                action().onSuccess { onSaved() }.onFailure { _error.value = it.message ?: "Не удалось сохранить товар." }
            } finally { _isLoading.value = false }
        }
    }

    fun clearError() {
        _error.value = null
    }

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProductViewModel(ProductRepository.create(database)) as T
        }
    }
}
