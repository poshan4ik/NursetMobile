package com.nursetmobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nursetmobile.data.AppDatabase
import com.nursetmobile.data.entity.CategoryEntity
import com.nursetmobile.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(private val repository: CategoryRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> =
        repository.allCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCategory(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.addCategory(name)
            _isLoading.value = false
            result.onFailure { _error.value = it.message }
        }
    }

    fun updateCategory(id: Long, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.updateCategory(id, name)
            _isLoading.value = false
            result.onFailure { _error.value = it.message }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.deleteCategory(id)
            _isLoading.value = false
            result.onFailure { _error.value = it.message }
        }
    }

    fun clearError() {
        _error.value = null
    }

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoryViewModel(CategoryRepository.create(database)) as T
        }
    }
}
