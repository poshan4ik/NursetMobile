package com.nursetmobile.ui.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nursetmobile.NursetApplication
import com.nursetmobile.ui.orders.OrderListItem
import com.nursetmobile.viewmodel.ClientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(onOrderClick: (Long) -> Unit, viewModel: ClientViewModel = viewModel(
    factory = ClientViewModel.Factory((LocalContext.current.applicationContext as NursetApplication).database)
)) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showRegistration by rememberSaveable { mutableStateOf(false) }
    val selected = clients.find { it.id == selectedId }
    val visibleClients = remember(clients, query) {
        val digits = query.filter { it.isDigit() }
        clients.filter { it.name.contains(query.trim(), ignoreCase = true) ||
            (digits.isNotEmpty() && it.phone.filter { c -> c.isDigit() }.contains(digits)) }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(selected?.name ?: "Клиенты") },
        navigationIcon = { if (selectedId != null) TextButton(onClick = { selectedId = null }) { Text("Назад") } },
        actions = { if (selectedId == null) TextButton(onClick = { showRegistration = true }) { Text("Добавить") } }
    ) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (selected == null) {
                item { OutlinedTextField(query, { query = it }, label = { Text("Имя или телефон") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()) }
                if (visibleClients.isEmpty()) item { Text(if (clients.isEmpty()) "Добавьте первого клиента." else "Клиенты не найдены.") }
                items(visibleClients, key = { it.id }) { client ->
                    Card(Modifier.fillMaxWidth().clickable { selectedId = client.id }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(client.name, style = MaterialTheme.typography.titleMedium)
                            Text(client.phone)
                        }
                    }
                }
            } else {
                item { Text(selected.phone); Text("История заказов", style = MaterialTheme.typography.titleLarge) }
                val history = orders.filter { it.clientId == selected.id }
                if (history.isEmpty()) item { Text("У клиента пока нет заказов.") }
                items(history, key = { it.id }) { order -> OrderListItem(order) { onOrderClick(order.id) } }
            }
        }
    }
    if (showRegistration) {
        var name by rememberSaveable { mutableStateOf("") }
        var phone by rememberSaveable { mutableStateOf("") }
        AlertDialog(onDismissRequest = { if (!saving) { showRegistration = false; viewModel.clearError() } },
            title = { Text("Новый клиент") }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true)
                    OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }, confirmButton = { TextButton(enabled = !saving, onClick = {
                viewModel.clearError()
                viewModel.register(name, phone) { showRegistration = false }
            }) { Text(if (saving) "Сохранение…" else "Сохранить") } },
            dismissButton = { TextButton(enabled = !saving, onClick = {
                showRegistration = false; viewModel.clearError()
            }) { Text("Отмена") } })
    }
}
