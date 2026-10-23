package com.nursetmobile.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nursetmobile.NursetApplication
import com.nursetmobile.R
import com.nursetmobile.data.entity.OrderEntity
import com.nursetmobile.data.entity.OrderItemEntity
import com.nursetmobile.domain.OrderStatus
import com.nursetmobile.ui.StatusChip
import com.nursetmobile.ui.formatPrice
import com.nursetmobile.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    orderId: Long,
    onBack: () -> Unit,
    viewModel: OrderViewModel = viewModel(
        factory = OrderViewModel.Factory(
            (LocalContext.current.applicationContext as NursetApplication).database
        )
    )
) {
    val error by viewModel.error.collectAsStateWithLifecycle()
    val success by viewModel.success.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    // Данные заказа перезагружаем после каждого успешного изменения статуса
    var orderData by remember {
        mutableStateOf<Pair<OrderEntity?, List<OrderItemEntity>>?>(null)
    }
    LaunchedEffect(orderId, success) {
        orderData = viewModel.getOrderWithItems(orderId)
        if (success != null) {
            snackbarHostState.showSnackbar(success!!)
            viewModel.clearMessages()
        }
    }

    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.order_details),
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        val data = orderData
        if (data == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val order = data.first
            val orderItems = data.second
            if (order == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.order_not_found),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                OrderDetailsContent(
                    order = order,
                    orderItems = orderItems,
                    innerPadding = innerPadding,
                    isLoading = isLoading,
                    onStatusChange = { newStatus ->
                        viewModel.updateOrderStatus(orderId, newStatus)
                    },
                    onCancelRequest = { showCancelDialog = true }
                )
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.confirm_cancel_order, orderId)
                )
            },
            text = { Text(text = stringResource(R.string.cancel_order_warning)) },
            confirmButton = {
                Button(
                    enabled = !isLoading,
                    onClick = {
                        viewModel.cancelOrder(orderId)
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                Button(onClick = { showCancelDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun OrderDetailsContent(
    order: OrderEntity,
    orderItems: List<OrderItemEntity>,
    innerPadding: PaddingValues,
    isLoading: Boolean,
    onStatusChange: (OrderStatus) -> Unit,
    onCancelRequest: () -> Unit
) {
    val status = OrderStatus.fromValue(order.status)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { OrderInfoCard(order = order, status = status) }
        item { Text(stringResource(R.string.order_items), style = MaterialTheme.typography.titleMedium) }
        items(orderItems, key = { it.id }) { OrderDetailItemCard(it) }
        item { OrderTotalCard(totalPrice = order.totalPrice) }
        item { StatusActionButtons(
            status = status,
            enabled = !isLoading,
            onStatusChange = onStatusChange,
            onCancelRequest = onCancelRequest
        ) }
    }
}

@Composable
private fun OrderInfoCard(order: OrderEntity, status: OrderStatus) {
    val dateText =
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(order.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.order_number_format, order.id),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(status = status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (order.clientName.isNotBlank()) Text("Клиент: ${order.clientName}")
            Text(
                text = stringResource(R.string.date_with_value, dateText),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OrderTotalCard(totalPrice: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.total),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatPrice(totalPrice),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StatusActionButtons(
    status: OrderStatus,
    enabled: Boolean,
    onStatusChange: (OrderStatus) -> Unit,
    onCancelRequest: () -> Unit
) {
    when (status) {
        OrderStatus.NEW -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    enabled = enabled,
                    onClick = { onStatusChange(OrderStatus.PROCESSING) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        stringResource(R.string.action_process),
                        fontWeight = FontWeight.Medium
                    )
                }
                CancelButton(
                    enabled = enabled,
                    onClick = onCancelRequest,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OrderStatus.PROCESSING -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    enabled = enabled,
                    onClick = { onStatusChange(OrderStatus.COMPLETED) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(
                        stringResource(R.string.action_complete),
                        fontWeight = FontWeight.Medium
                    )
                }
                CancelButton(
                    enabled = enabled,
                    onClick = onCancelRequest,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OrderStatus.COMPLETED, OrderStatus.CANCELLED -> {
            Text(
                text = if (status == OrderStatus.COMPLETED) stringResource(R.string.message_order_completed)
                else stringResource(R.string.message_order_cancelled),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun CancelButton(onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            stringResource(R.string.action_cancel),
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun OrderDetailItemCard(item: OrderItemEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.productName,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatPrice(item.price * item.quantity),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatPrice(item.price)} × ${item.quantity}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
