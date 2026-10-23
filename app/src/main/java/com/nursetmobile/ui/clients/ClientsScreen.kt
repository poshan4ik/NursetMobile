package com.nursetmobile.ui.clients

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nursetmobile.NursetApplication
import com.nursetmobile.domain.OrderStatus
import com.nursetmobile.domain.matchesSearch
import com.nursetmobile.ui.orders.OrderListItem
import com.nursetmobile.viewmodel.ClientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    onOrderClick: (Long) -> Unit,
    onCreateOrder: (Long) -> Unit,
    viewModel: ClientViewModel = viewModel(
        factory = ClientViewModel.Factory((LocalContext.current.applicationContext as NursetApplication).database)
    )
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = clients.find { it.id == selectedId }
    val visibleClients = remember(clients, query) {
        clients.filter { it.matchesSearch(query) }.sortedBy { it.name.lowercase() }
    }
    val orderCounts = remember(orders) { orders.groupingBy { it.clientId }.eachCount() }
    BackHandler(enabled = selectedId != null && !showForm) { selectedId = null }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (selectedId == null) "Клиенты" else "Карточка клиента") },
            navigationIcon = {
                if (selectedId != null) TextButton(onClick = { selectedId = null }) { Text("Назад") }
            },
            actions = {
                TextButton(onClick = { editingId = selected?.id; showForm = true }) {
                    Text(if (selectedId == null) "Добавить" else "Изменить")
                }
            })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (selected == null) {
                item {
                    OutlinedTextField(query, { query = it }, label = { Text("Имя или телефон") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text("Сбросить") }
                        })
                }
                if (visibleClients.isEmpty()) item {
                    Text(if (clients.isEmpty()) "Добавьте первого клиента, чтобы оформить заказ." else "Клиенты не найдены. Попробуйте другое имя или телефон.")
                    TextButton(onClick = { editingId = null; showForm = true }) { Text("Добавить клиента") }
                }
                items(visibleClients, key = { it.id }) { client ->
                    Card(Modifier.fillMaxWidth().clickable { selectedId = client.id }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(client.name, style = MaterialTheme.typography.titleMedium)
                            Text(client.phone)
                            Text("Заказов: ${orderCounts[client.id] ?: 0}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                val history = orders.filter { it.clientId == selected.id }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(selected.name, style = MaterialTheme.typography.titleLarge)
                            Text(selected.phone)
                            Text("Всего заказов: ${history.size}")
                            val activeCount = history.count { it.status == OrderStatus.NEW.value || it.status == OrderStatus.PROCESSING.value }
                            val completedCount = history.count { it.status == OrderStatus.COMPLETED.value }
                            Text("Активных: $activeCount · Выполнено: $completedCount")
                            Button(onClick = { onCreateOrder(selected.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Создать заказ")
                            }
                        }
                    }
                }
                item { Text("История заказов", style = MaterialTheme.typography.titleLarge) }
                if (history.isEmpty()) item { Text("У клиента пока нет заказов.") }
                items(history, key = { it.id }) { order -> OrderListItem(order) { onOrderClick(order.id) } }
            }
        }
    }
    if (showForm) {
        ClientFormDialog(client = clients.find { it.id == editingId }, viewModel = viewModel,
            onSaved = { showForm = false }, onDismiss = { showForm = false })
    }
}
