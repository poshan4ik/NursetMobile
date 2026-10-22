package com.nursetmobile.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
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
        TabItem(R.string.orders, Icons.Default.ReceiptLong, "orders")
    )

    // Какая вкладка выбрана: определяется по текущему маршруту
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = when {
        currentRoute == null -> 0
        currentRoute.startsWith("order_details") -> 3
        currentRoute == "create_order" -> 3
        else -> tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }

    fun navigateToTab(index: Int) {
        val tab = tabs.getOrNull(index) ?: return
        navController.navigate(tab.route) {
            popUpTo("home") { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        bottomBar = {
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
                                fontSize = 12.sp
                            )
                        }
                    )
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
                HomeScreen(onTabClick = { index -> navigateToTab(index) })
            }
            composable("products") {
                ProductsScreen()
            }
            composable("categories") {
                CategoriesScreen()
            }
            composable("orders") {
                OrdersScreen(
                    onCreateOrder = { navController.navigate("create_order") },
                    onOrderClick = { orderId -> navController.navigate("order_details/$orderId") }
                )
            }
            composable("create_order") {
                CreateOrderScreen(onBack = { navController.popBackStack() })
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
