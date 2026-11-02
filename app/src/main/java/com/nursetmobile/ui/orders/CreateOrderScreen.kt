package com.nursetmobile.ui.orders

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nursetmobile.NursetApplication
import com.nursetmobile.R
import com.nursetmobile.data.entity.ProductEntity
import com.nursetmobile.data.entity.CategoryEntity
import com.nursetmobile.ui.formatPrice
import com.nursetmobile.viewmodel.CartItem
import com.nursetmobile.viewmodel.OrderViewModel
import com.nursetmobile.viewmodel.ClientViewModel
import com.nursetmobile.ui.clients.ClientFormDialog
import com.nursetmobile.domain.matchesSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onBack: () -> Unit,
    initialClientId: Long = 0L,
    onCreated: (Long) -> Unit = { onBack() },
    viewModel: OrderViewModel = viewModel(
        factory = OrderViewModel.Factory(
            (LocalContext.current.applicationContext as NursetApplication).database
        )
    ),
    clientViewModel: ClientViewModel = viewModel(
        factory = ClientViewModel.Factory((LocalContext.current.applicationContext as NursetApplication).database)
    )
) {
    val cartItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val createdOrderId by viewModel.createdOrderId.collectAsStateWithLifecycle()
    val availableProducts by viewModel.availableProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val selectedClientId by viewModel.selectedClientId.collectAsStateWithLifecycle()
    var showClientPicker by rememberSaveable { mutableStateOf(false) }
    var showClientForm by rememberSaveable { mutableStateOf(false) }
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialClientId) { if (initialClientId > 0 && selectedClientId == null) viewModel.selectClient(initialClientId) }
    fun requestBack() {
        if (isLoading) return
        if (cartItems.isNotEmpty()) showLeaveDialog = true else onBack()
    }
    BackHandler { requestBack() }

    val snackbarHostState = remember { SnackbarHostState() }
    var showProductPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(createdOrderId) {
        createdOrderId?.let { id ->
            viewModel.consumeCreatedOrder()
            onCreated(id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.new_order),
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                navigationIcon = {
                    IconButton(onClick = { requestBack() }, enabled = !isLoading) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showProductPicker = true }, enabled = !isLoading) { Text("Добавить товары") }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (cartItems.isNotEmpty()) Surface(shadowElevation = 4.dp) {
                Column(Modifier.fillMaxWidth().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Позиций: ${cartItems.size}", style = MaterialTheme.typography.bodySmall)
                    CartTotalCard(cartTotal)
                    if (cartItems.any { it.quantity > it.maxStock }) {
                        Text("Проверьте количество недоступных товаров.", color = MaterialTheme.colorScheme.error)
                    }
                    CreateOrderButton(isLoading, !isLoading && cartItems.all { it.quantity in 1..it.maxStock }) {
                        viewModel.createOrder()
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(12.dp)) {
                    val client = clients.find { it.id == selectedClientId }
                    Text(client?.name ?: "Без указания клиента", style = MaterialTheme.typography.titleMedium)
                    if (client == null) Text("Клиент необязателен для оформления заказа.", style = MaterialTheme.typography.bodySmall)
                    client?.let { Text(it.phone, style = MaterialTheme.typography.bodySmall) }
                    Row {
                        TextButton(onClick = { showClientPicker = true }, enabled = !isLoading) { Text(if (client == null) "Выбрать клиента" else "Изменить") }
                        TextButton(onClick = { showClientForm = true }, enabled = !isLoading) { Text("Новый клиент") }
                        if (client != null) TextButton(onClick = { viewModel.clearClient() }, enabled = !isLoading) { Text("Убрать") }
                    }
                }
            }
        Box(Modifier.weight(1f)) {
        if (cartItems.isEmpty()) {
            EmptyCartView(
                innerPadding = PaddingValues(0.dp),
                onSelectProducts = { showProductPicker = true }
            )
        } else {
            CartContent(
                cartItems = cartItems,
                isLoading = isLoading,
                innerPadding = PaddingValues(0.dp),
                onQuantityChange = { productId, quantity ->
                    viewModel.updateCartItemQuantity(productId, quantity)
                },
                onRemove = { productId -> viewModel.removeFromCart(productId) }
            )
        }
        }
        }
    }

    if (showClientPicker) {
        var query by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showClientPicker = false }, title = { Text("Клиент заказа") },
            text = {
                Column(Modifier.heightIn(max = 360.dp)) {
                    TextField(query, { query = it }, label = { Text("Имя или телефон") }, singleLine = true)
                    val matches = clients.filter { it.matchesSearch(query) }
                    if (matches.isEmpty()) Text("Клиенты не найдены.")
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(matches, key = { it.id }) { client ->
                            TextButton(onClick = {
                                viewModel.selectClient(client.id)
                                showClientPicker = false
                            }, modifier = Modifier.fillMaxWidth()) { Text("${client.name} · ${client.phone}") }
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = {
                showClientPicker = false; showClientForm = true
            }) { Text("Новый клиент") } },
            dismissButton = { TextButton(onClick = { showClientPicker = false }) { Text("Закрыть") } })
    }

    if (showProductPicker) {
        ProductPickerDialog(
            products = availableProducts,
            categories = categories,
            cartItems = cartItems,
            onProductClick = { product ->
                viewModel.addToCart(product, 1)
            },
            onDismiss = { showProductPicker = false }
        )
    }
    if (showClientForm) ClientFormDialog(viewModel = clientViewModel, onSaved = {
        viewModel.selectClient(it)
        showClientForm = false
    }, onDismiss = { showClientForm = false })
    if (showLeaveDialog) AlertDialog(onDismissRequest = { showLeaveDialog = false },
        title = { Text("Выйти из оформления?") },
        text = { Text("Товары в корзине не будут сохранены. Заказ ещё не создан.") },
        confirmButton = { TextButton(onClick = { showLeaveDialog = false; onBack() }) { Text("Выйти") } },
        dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Продолжить заказ") } })
}

@Composable
private fun EmptyCartView(
    innerPadding: PaddingValues,
    onSelectProducts: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = stringResource(R.string.cart_empty),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.cart_empty_hint),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSelectProducts) {
                Text(stringResource(R.string.select_products))
            }
        }
    }
}

@Composable
private fun CartContent(
    cartItems: List<CartItem>,
    isLoading: Boolean,
    innerPadding: PaddingValues,
    onQuantityChange: (productId: Long, quantity: Int) -> Unit,
    onRemove: (productId: Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cartItems, key = { it.productId }) { item ->
            OrderCartItemCard(
                item = item,
                enabled = !isLoading,
                onQuantityChange = { newQuantity -> onQuantityChange(item.productId, newQuantity) },
                onRemove = { onRemove(item.productId) }
            )
        }

    }
}

@Composable
private fun CartTotalCard(cartTotal: Long) {
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
                text = formatPrice(cartTotal),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun CreateOrderButton(
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.create_order),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OrderCartItemCard(
    item: CartItem,
    enabled: Boolean,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.productName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove, enabled = enabled) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${formatPrice(item.price)} × ${item.quantity} = ${formatPrice(item.price * item.quantity)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(if (item.maxStock == 0) "Товар недоступен. Удалите его из заказа."
                    else "В наличии: ${item.maxStock} шт.",
                    color = if (item.quantity > item.maxStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                QuantityCounter(
                    quantity = item.quantity,
                    maxQuantity = item.maxStock,
                    enabled = enabled,
                    onSetQuantity = onQuantityChange,
                    onDecrease = { onQuantityChange(item.quantity - 1) },
                    onIncrease = { onQuantityChange(item.quantity + 1) }
                )
            }
        }
    }
}

@Composable
private fun QuantityCounter(
    quantity: Int,
    maxQuantity: Int,
    enabled: Boolean,
    onSetQuantity: (Int) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    var showInput by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onDecrease, enabled = enabled && quantity > 1) {
            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.decrease))
        }
        TextButton(onClick = { input = quantity.toString(); showInput = true }, enabled = enabled && maxQuantity > 0,
            modifier = Modifier.widthIn(min = 64.dp)) {
            Text(quantity.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onIncrease, enabled = enabled && quantity < maxQuantity) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.increase))
        }
    }
    if (showInput) {
        val parsed = input.trim().toIntOrNull()
        val valid = parsed != null && parsed in 1..maxQuantity
        AlertDialog(onDismissRequest = { showInput = false }, title = { Text("Количество товара") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Доступно: $maxQuantity шт.")
                    TextField(input, { input = it }, label = { Text("Количество") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !valid, supportingText = { Text("Введите от 1 до $maxQuantity") })
                }
            }, confirmButton = { TextButton(enabled = enabled && valid, onClick = {
                onSetQuantity(parsed!!); showInput = false
            }) { Text("Применить") } },
            dismissButton = { TextButton(onClick = { showInput = false }) { Text("Отмена") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductPickerDialog(
    products: List<ProductEntity>,
    categories: List<CategoryEntity>,
    cartItems: List<CartItem>,
    onProductClick: (ProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }

    val filteredProducts = products.filter { product ->
        val matchesSearch =
            searchQuery.isBlank() || product.name.contains(searchQuery.trim(), ignoreCase = true)
        matchesSearch && product.stock > 0 && (selectedCategoryId == null || product.categoryId == selectedCategoryId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.select_product)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = selectedCategoryId == null, onClick = { selectedCategoryId = null }, label = { Text("Все категории") }) }
                    items(categories, key = { it.id }) { category ->
                        FilterChip(selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id }, label = { Text(category.name) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (filteredProducts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_products_found),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            val inCart = cartItems.find { it.productId == product.id }?.quantity ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = inCart < product.stock) { onProductClick(product) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(categories.find { it.id == product.categoryId }?.name.orEmpty(), style = MaterialTheme.typography.bodySmall)
                                    if (inCart > 0) Text("В корзине: $inCart", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = stringResource(
                                            R.string.product_price_and_stock,
                                            formatPrice(product.price), product.stock
                                        ),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Готово")
            }
        }
    )
}
