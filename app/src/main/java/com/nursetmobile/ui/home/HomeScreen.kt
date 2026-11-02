package com.nursetmobile.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nursetmobile.NursetApplication
import com.nursetmobile.domain.ProductStockFilter
import com.nursetmobile.ui.orders.OrderListItem
import com.nursetmobile.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onCreateOrder: () -> Unit,
    onOpenProducts: (ProductStockFilter) -> Unit,
    onOpenActiveOrders: () -> Unit,
    onOrderClick: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory((LocalContext.current.applicationContext as NursetApplication).database)
    )
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Товары и заказы", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("NursetMobile · учёт небольшого магазина", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onCreateOrder, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Новый заказ")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Товаров в каталоге", summary.productsCount, Modifier.weight(1f)) {
                onOpenProducts(ProductStockFilter.ALL)
            }
            SummaryCard("Активных заказов", summary.activeOrders.size, Modifier.weight(1f), onClick = onOpenActiveOrders)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Заканчиваются", summary.lowStockCount, Modifier.weight(1f)) {
                onOpenProducts(ProductStockFilter.LOW)
            }
            SummaryCard("Нет в наличии", summary.outOfStockCount, Modifier.weight(1f)) {
                onOpenProducts(ProductStockFilter.OUT_OF_STOCK)
            }
        }
        Text("Активные заказы", style = MaterialTheme.typography.titleLarge)
        if (summary.activeOrders.isEmpty()) {
            Text("Активных заказов нет. Можно создать новый заказ.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            summary.activeOrders.take(3).forEach { order -> OrderListItem(order) { onOrderClick(order.id) } }
            TextButton(onClick = onOpenActiveOrders, modifier = Modifier.fillMaxWidth()) { Text("Все активные заказы") }
        }
    }
}

@Composable
private fun SummaryCard(label: String, count: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick).heightIn(min = 112.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
