package com.nursetmobile.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import com.nursetmobile.ui.clients.ClientsScreen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nursetmobile.domain.OrderFilter
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nursetmobile.R
import com.nursetmobile.ui.categories.CategoriesScreen
import com.nursetmobile.ui.home.HomeScreen
import com.nursetmobile.ui.orders.CreateOrderScreen
import com.nursetmobile.ui.orders.OrderDetailsScreen
import com.nursetmobile.ui.orders.OrdersScreen
import com.nursetmobile.ui.products.ProductsScreen

@Composable
fun NursetNavHost(
    navController: NavHostController = rememberNavController()
) {
    val tabs = listOf(
        TabItem(R.string.home, Icons.Default.Home, "home"),
        TabItem(R.string.products, Icons.Default.Inventory2, "products"),
        TabItem(R.string.categories, Icons.Default.Category, "categories"),
        TabItem(R.string.orders, Icons.Default.ReceiptLong, "orders"),
        TabItem(R.string.clients, Icons.Default.People, "clients")
    )

    // Какая вкладка выбрана: определяется по текущему маршруту
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = when {
        currentRoute == null -> 0
        currentRoute.startsWith("order_details") -> 3
        currentRoute.startsWith("create_order") -> 3
        else -> tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }

    fun navigateToTab(index: Int) {
        val tab = tabs.getOrNull(index) ?: return
        navController.navigate(tab.route) {
            popUpTo("home") { inclusive = false; saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute?.startsWith("create_order") != true) {
            NavigationBar {
                tabs.forEach { tab ->
                    val isSelected = tabs.indexOf(tab) == selectedTab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { navigateToTab(tabs.indexOf(tab)) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(tab.label),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.requiredWidth(64.dp)
                            )
                        }
                    )
                }
            }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onCreateOrder = { navController.navigate("create_order") },
                    onOpenProducts = { filter ->
                        navigateToTab(1)
                        navController.currentBackStackEntry?.savedStateHandle?.set("requestedStockFilter", filter.name)
                    },
                    onOpenActiveOrders = {
                        navigateToTab(3)
                        navController.currentBackStackEntry?.savedStateHandle?.set("requestedOrderFilter", OrderFilter.ACTIVE.name)
                    },
                    onOrderClick = { navController.navigate("order_details/$it") }
                )
            }
            composable("products") { entry ->
                val requested by entry.savedStateHandle.getStateFlow<String?>("requestedStockFilter", null).collectAsStateWithLifecycle()
                ProductsScreen(requestedStockFilter = requested,
                    onStockFilterApplied = { entry.savedStateHandle["requestedStockFilter"] = null },
                    onOpenCategories = { navigateToTab(2) })
            }
            composable("categories") {
                CategoriesScreen()
            }
            composable("clients") {
                ClientsScreen(onOrderClick = { navController.navigate("order_details/$it") },
                    onCreateOrder = { navController.navigate("create_order?clientId=$it") })
            }
            composable("orders") { entry ->
                val requested by entry.savedStateHandle.getStateFlow<String?>("requestedOrderFilter", null).collectAsStateWithLifecycle()
                OrdersScreen(
                    onCreateOrder = { navController.navigate("create_order") },
                    onOrderClick = { orderId -> navController.navigate("order_details/$orderId") },
                    requestedFilter = requested,
                    onFilterApplied = { entry.savedStateHandle["requestedOrderFilter"] = null }
                )
            }
            composable("create_order?clientId={clientId}", arguments = listOf(navArgument("clientId") {
                type = NavType.LongType
                defaultValue = 0L
            })) { entry ->
                CreateOrderScreen(onBack = { navController.popBackStack() },
                    initialClientId = entry.arguments?.getLong("clientId") ?: 0L,
                    onCreated = { id ->
                        navController.navigate("order_details/$id") {
                            popUpTo(entry.destination.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    })
            }
            composable(
                route = "order_details/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
                OrderDetailsScreen(
                    orderId = orderId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private data class TabItem(
    val label: Int,
    val icon: ImageVector,
    val route: String
)
