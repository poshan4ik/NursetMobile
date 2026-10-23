package com.nursetmobile.ui.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nursetmobile.data.entity.ClientEntity
import com.nursetmobile.viewmodel.ClientViewModel

@Composable
fun ClientFormDialog(
    client: ClientEntity? = null,
    viewModel: ClientViewModel,
    onSaved: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var name by rememberSaveable(client?.id) { mutableStateOf(client?.name.orEmpty()) }
    var phone by rememberSaveable(client?.id) { mutableStateOf(client?.phone.orEmpty()) }
    LaunchedEffect(client?.id) { viewModel.clearError() }
    fun dismiss() { if (!saving) { viewModel.clearError(); onDismiss() } }
    AlertDialog(onDismissRequest = { dismiss() },
        title = { Text(if (client == null) "Новый клиент" else "Редактирование клиента") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it; viewModel.clearError() }, enabled = !saving,
                    label = { Text("Имя") }, singleLine = true)
                OutlinedTextField(phone, { phone = it; viewModel.clearError() }, enabled = !saving,
                    label = { Text("Телефон") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(enabled = !saving, onClick = {
            viewModel.clearError()
            viewModel.save(client?.id, name, phone, onSaved)
        }) { Text(if (saving) "Сохранение…" else "Сохранить") } },
        dismissButton = { TextButton(enabled = !saving, onClick = { dismiss() }) { Text("Отмена") } })
}
