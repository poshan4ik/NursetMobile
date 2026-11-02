package com.nursetmobile.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nursetmobile.NursetApplication
import com.nursetmobile.R
import com.nursetmobile.data.entity.CategoryEntity
import com.nursetmobile.data.entity.ProductEntity
import com.nursetmobile.ui.formatPrice
import com.nursetmobile.viewmodel.ProductViewModel
import com.nursetmobile.domain.ProductStockFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ProductViewModel = viewModel(
        factory = ProductViewModel.Factory(
            (LocalContext.current.applicationContext as NursetApplication).database
        )
    ),
    requestedStockFilter: String? = null,
    onStockFilterApplied: () -> Unit = {},
    onOpenCategories: () -> Unit = {}
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val stockFilter by viewModel.stockFilter.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isSaving by viewModel.isLoading.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var deletingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var restockingProduct by remember { mutableStateOf<ProductEntity?>(null) }

    LaunchedEffect(requestedStockFilter) {
        requestedStockFilter?.let { requested ->
            viewModel.showStockFilter(ProductStockFilter.entries.find { it.name == requested } ?: ProductStockFilter.ALL)
            onStockFilterApplied()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.takeIf { !showAddDialog && editingProduct == null && restockingProduct == null }?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(showAddDialog, editingProduct?.id, restockingProduct?.id) { viewModel.clearError() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.products),
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ProductSearchBar(
                searchQuery = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) }
            )

            CategoryFilterChips(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { viewModel.onCategoryFilterChange(it) }
            )

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ProductStockFilter.entries) { filter ->
                    FilterChip(selected = stockFilter == filter, onClick = { viewModel.onStockFilterChange(filter) },
                        label = { Text(filter.label) })
                }
            }

            val hasFilters = searchQuery.isNotBlank() || selectedCategoryId != null || stockFilter != ProductStockFilter.ALL
            if (hasFilters && products.isNotEmpty()) TextButton(onClick = { viewModel.resetFilters() }) { Text("Сбросить фильтры") }

            if (lowStockCount > 0 && stockFilter != ProductStockFilter.LOW) {
                LowStockWarning(lowStockCount) { viewModel.showStockFilter(ProductStockFilter.LOW) }
            }

            if (products.isEmpty()) {
                EmptyProductsMessage(
                    filtered = hasFilters,
                    needsCategory = categories.isEmpty(),
                    onReset = { viewModel.resetFilters() },
                    onAdd = { if (categories.isEmpty()) onOpenCategories() else showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            categoryName = categories.find { it.id == product.categoryId }?.name.orEmpty(),
                            onEdit = { editingProduct = product },
                            onDelete = { deletingProduct = product },
                            onRestock = { restockingProduct = product }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ProductDialog(
            title = stringResource(R.string.add),
            initial = null,
            categories = categories,
            isSaving = isSaving,
            error = error,
            onCreateCategory = { showAddDialog = false; onOpenCategories() },
            onConfirm = { name, categoryId, price, stock ->
                viewModel.addProduct(name, categoryId, price, stock) { showAddDialog = false }
            },
            onDismiss = { if (!isSaving) showAddDialog = false }
        )
    }

    editingProduct?.let { product ->
        ProductDialog(
            title = stringResource(R.string.edit),
            initial = product,
            categories = categories,
            isSaving = isSaving,
            error = error,
            onCreateCategory = { editingProduct = null; onOpenCategories() },
            onConfirm = { name, categoryId, price, stock ->
                viewModel.updateProduct(product.id, name, categoryId, price, stock, expectedStock = product.stock) { editingProduct = null }
            },
            onDismiss = { if (!isSaving) editingProduct = null }
        )
    }

    deletingProduct?.let { product ->
        DeleteProductDialog(
            productName = product.name,
            onConfirm = {
                viewModel.deleteProduct(product.id)
                deletingProduct = null
            },
            onDismiss = { deletingProduct = null }
        )
    }

    restockingProduct?.let { product ->
        RestockDialog(product, isSaving, error,
            onConfirm = { quantity -> viewModel.restockProduct(product.id, quantity) { restockingProduct = null } },
            onDismiss = { if (!isSaving) restockingProduct = null })
    }
}

@Composable
private fun ProductSearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterChips(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.all_categories)) }
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = {
                    onCategorySelected(if (selectedCategoryId == category.id) null else category.id)
                },
                label = { Text(category.name) }
            )
        }
    }
}

@Composable
private fun LowStockWarning(lowStockCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Наименований с низким остатком: $lowStockCount. Показать",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyProductsMessage(filtered: Boolean, needsCategory: Boolean, onReset: () -> Unit, onAdd: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = if (filtered) "Товары не найдены" else stringResource(R.string.empty_products),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(if (filtered) "Измените поиск, категорию или остаток."
                else if (needsCategory) "Сначала создайте категорию, затем добавьте товары."
                else "Добавьте первый товар, его цену и количество.")
            TextButton(onClick = if (filtered) onReset else onAdd) {
                Text(if (filtered) "Сбросить фильтры" else if (needsCategory) "Создать категорию" else "Добавить товар")
            }
        }
    }
}

@Composable
private fun DeleteProductDialog(
    productName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.confirm_delete_product, productName))
        },
        text = { Text(text = stringResource(R.string.irreversible_action)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.onError
                )
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ProductItemCard(
    product: ProductEntity,
    categoryName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestock: () -> Unit
) {
    val isLowStock = product.stock <= 3 && product.stock > 0
    val isOutOfStock = product.stock == 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else if (isLowStock) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(categoryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.price_with_value, formatPrice(product.price)),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.stock_with_value, product.stock),
                    fontSize = 14.sp,
                    color = if (isOutOfStock || isLowStock) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isLowStock || isOutOfStock) FontWeight.Bold else FontWeight.Normal
                )
            }

            TextButton(onClick = onRestock) { Text("Пополнить остаток") }

            if (isLowStock) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.low_stock_warning, product.stock),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (isOutOfStock) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.out_of_stock),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDialog(
    title: String,
    initial: ProductEntity?,
    categories: List<CategoryEntity>,
    isSaving: Boolean,
    error: String?,
    onCreateCategory: () -> Unit,
    onConfirm: (name: String, categoryId: Long, price: Long, stock: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var categoryId by remember { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id ?: 0L) }
    var priceText by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var stockText by remember { mutableStateOf(initial?.stock?.toString() ?: "") }

    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var stockError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = name,
                    enabled = !isSaving,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text(stringResource(R.string.name_label)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.error_empty_name)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (categories.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.filter_category),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { category ->
                            FilterChip(
                                enabled = !isSaving,
                                selected = categoryId == category.id,
                                onClick = { categoryId = category.id },
                                label = { Text(category.name) }
                            )
                        }
                    }
                } else {
                    Text("Для товара нужна категория.")
                    TextButton(onClick = onCreateCategory, enabled = !isSaving) { Text("Создать категорию") }
                }

                TextField(
                    value = priceText,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        priceText = it
                        priceError = false
                    },
                    label = { Text(stringResource(R.string.price)) },
                    isError = priceError,
                    supportingText = if (priceError) {
                        { Text(stringResource(R.string.error_price_positive)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                TextField(
                    value = stockText,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        stockText = it
                        stockError = false
                    },
                    label = { Text(stringResource(R.string.quantity)) },
                    isError = stockError,
                    supportingText = if (stockError) {
                        { Text(stringResource(R.string.error_stock_non_negative)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && categories.isNotEmpty(),
                onClick = {
                    val price = priceText.trim().toLongOrNull()
                    val stock = stockText.trim().toIntOrNull()
                    nameError = name.trim().isEmpty()
                    priceError = price == null || price <= 0
                    stockError = stock == null || stock < 0

                    if (!nameError && !priceError && !stockError) {
                        onConfirm(name.trim(), categoryId, price!!, stock!!)
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun RestockDialog(product: ProductEntity, isSaving: Boolean, error: String?, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var quantity by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Пополнить остаток") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(product.name)
                Text("Будет добавлено к текущему остатку. Цена и название не изменятся.")
                TextField(quantity, { quantity = it; invalid = false }, label = { Text("Поступило, шт.") },
                    enabled = !isSaving, singleLine = true, isError = invalid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                if (invalid) Text("Введите целое количество больше нуля.", color = MaterialTheme.colorScheme.error)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(enabled = !isSaving, onClick = {
            val amount = quantity.trim().toIntOrNull()
            invalid = amount == null || amount <= 0
            if (!invalid) onConfirm(amount!!)
        }) { Text(if (isSaving) "Сохранение…" else "Пополнить") } },
        dismissButton = { TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Отмена") } })
}
