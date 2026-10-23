package com.nursetmobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nursetmobile.data.AppDatabase
import com.nursetmobile.data.repository.ClientRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ClientViewModel(database: AppDatabase) : ViewModel() {
    private val repository = ClientRepository(database.clientDao())
    val clients = repository.clients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val orders = database.orderDao().getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    fun save(id: Long?, name: String, phone: String, onSaved: (Long) -> Unit) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val result = if (id == null) repository.register(name, phone) else repository.update(id, name, phone)
                result.onSuccess { onSaved(it) }
                    .onFailure { _error.value = it.message ?: "Не удалось сохранить клиента." }
            } finally {
                _saving.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ClientViewModel(database) as T
    }
}
