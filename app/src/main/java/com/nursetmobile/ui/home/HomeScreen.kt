package com.nursetmobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.People
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nursetmobile.NursetApplication
import com.nursetmobile.R
import com.nursetmobile.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onTabClick: (Int) -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            (LocalContext.current.applicationContext as NursetApplication).database
        )
    )
) {
    val productsCount by viewModel.productsCount.collectAsStateWithLifecycle()
    val categoriesCount by viewModel.categoriesCount.collectAsStateWithLifecycle()
    val ordersCount by viewModel.ordersCount.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = stringResource(R.string.stat_products, productsCount),
                icon = Icons.Default.Inventory2,
                color = MaterialTheme.colorScheme.primaryContainer
            )
            StatCard(
                title = stringResource(R.string.stat_categories, categoriesCount),
                icon = Icons.Default.Category,
                color = MaterialTheme.colorScheme.secondaryContainer
            )
            StatCard(
                title = stringResource(R.string.stat_orders, ordersCount),
                icon = Icons.Default.ReceiptLong,
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
            StatCard(
                title = stringResource(R.string.stat_low_stock, lowStockCount),
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.errorContainer
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickNavButton(stringResource(R.string.go_to_products), Icons.Default.Inventory2) { onTabClick(1) }
            QuickNavButton(stringResource(R.string.go_to_categories), Icons.Default.Category) { onTabClick(2) }
            QuickNavButton(stringResource(R.string.go_to_orders), Icons.Default.ReceiptLong) { onTabClick(3) }
            QuickNavButton(stringResource(R.string.clients), Icons.Default.People) { onTabClick(4) }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun QuickNavButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, fontSize = 16.sp)
        }
    }
}
