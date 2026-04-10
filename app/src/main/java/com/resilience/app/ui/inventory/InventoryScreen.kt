package com.resilience.app.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.resilience.app.data.model.InventoryItem
import com.resilience.app.data.model.ItemCategory
import com.resilience.app.data.model.StockStatus
import com.resilience.app.ui.components.TacticalCard
import com.resilience.app.ui.components.TacticalIconButton
import com.resilience.app.ui.components.TacticalScannerOverlay
import com.resilience.app.ui.theme.TacticalPrimaryRust

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var adjustQuantityItem by remember { mutableStateOf<InventoryItem?>(null) }

    val totalItems = items.size
    val totalQuantity = items.sumOf { it.quantity }
    val lowCount = items.count { it.status == StockStatus.LOW }
    val critCount = items.count { it.status == StockStatus.CRITICAL }

    val overallStatusWord = when {
        totalQuantity <= 10 -> "STATUS: BARREN"
        totalQuantity <= 25 -> "STATUS: SCAVENGED"
        else -> "STATUS: STOCKED"
    }

    if (!hasSeenOnboarding) {
        AlertDialog(
            onDismissRequest = { viewModel.markOnboardingSeen() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(6.dp),
            title = {
                Text(
                    "// INVENTORY TRACKER",
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 2.sp,
                    color = TacticalPrimaryRust
                )
            },
            text = {
                Text(
                    "Welcome to your supply cache. Keeping track of rations, hydration, and gear could mean the difference between life and death.\n\nCurrent status is BARREN. Start by adding Water and Food quantities.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.markOnboardingSeen() }) {
                    Text("ACKNOWLEDGE", style = MaterialTheme.typography.labelLarge, color = TacticalPrimaryRust)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = TacticalPrimaryRust,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "INVENTORY",
                            style = MaterialTheme.typography.titleLarge,
                            letterSpacing = 3.sp
                        )
                    }
                },
                navigationIcon = {
                    TacticalIconButton(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        iconSize = 16.dp
                    )
                },
                actions = {
                    TacticalIconButton(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item",
                        onClick = { showAddDialog = true },
                        containerSize = 36.dp,
                        iconSize = 18.dp,
                        tint = TacticalPrimaryRust,
                        containerColor = TacticalPrimaryRust.copy(alpha = 0.14f),
                        borderColor = TacticalPrimaryRust.copy(alpha = 0.35f)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(6.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.secondary
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SUPPLY MANIFEST",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = "$totalItems ITEMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            TacticalScannerOverlay(isProminent = false)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(label = "TOTAL ITEMS", value = "$totalItems",
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f))
                        StatCard(label = "LOW STOCK", value = "$lowCount",
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f))
                        StatCard(label = "CRITICAL", value = "$critCount",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "// $overallStatusWord",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                    )
                    Spacer(Modifier.height(4.dp))
                }

                items(items, key = { it.id }) { item ->
                    InventoryItemRow(item, onClick = { adjustQuantityItem = item })
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, category, qty ->
                viewModel.addItem(name, category, qty)
                showAddDialog = false
            }
        )
    }

    adjustQuantityItem?.let { itemToAdjust ->
        AdjustQuantityDialog(
            item = itemToAdjust,
            onDismiss = { adjustQuantityItem = null },
            onConfirm = { newQuantity ->
                viewModel.updateQuantity(itemToAdjust.id, newQuantity)
                adjustQuantityItem = null
            }
        )
    }
}

// ── Stat card ─────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    TacticalCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = if (color == MaterialTheme.colorScheme.onBackground) 0.6f else 1f)
            )
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}

// ── Inventory row ─────────────────────────────────────────────────────────────

@Composable
private fun InventoryItemRow(item: InventoryItem, onClick: () -> Unit) {
    val (statusBg, statusBorder, statusText, statusLabel) = when (item.status) {
        StockStatus.STOCKED -> StatusColors(
            bg     = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
            border = MaterialTheme.colorScheme.secondary,
            text   = MaterialTheme.colorScheme.secondary,
            label  = "STOCKED"
        )
        StockStatus.LOW -> StatusColors(
            bg     = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
            border = MaterialTheme.colorScheme.tertiary,
            text   = MaterialTheme.colorScheme.tertiary,
            label  = "LOW"
        )
        StockStatus.CRITICAL -> StatusColors(
            bg     = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
            border = MaterialTheme.colorScheme.error,
            text   = MaterialTheme.colorScheme.error,
            label  = "CRITICAL"
        )
    }

    TacticalCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        if (item.status == StockStatus.CRITICAL) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(10.dp)
                                    .padding(end = 2.dp)
                            )
                        }
                        Text(
                            item.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(item.name, style = MaterialTheme.typography.titleMedium)
            }

            Text(
                "×${item.quantity}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.width(14.dp))

            Surface(
                color = statusBg,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, statusBorder)
            ) {
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusText,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

private data class StatusColors(
    val bg: Color,
    val border: Color,
    val text: Color,
    val label: String
)

// ── Adjust item dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustQuantityDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantity by remember { mutableIntStateOf(item.quantity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        title = {
            Text(
                "// ADJUST ${item.name.uppercase()}",
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 2.sp,
                color = TacticalPrimaryRust
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TacticalIconButton(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    onClick = { if (quantity > 0) quantity-- },
                    containerSize = 48.dp,
                    iconSize = 24.dp
                )
                
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                TacticalIconButton(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    onClick = { quantity++ },
                    containerSize = 48.dp,
                    iconSize = 24.dp,
                    tint = TacticalPrimaryRust,
                    containerColor = TacticalPrimaryRust.copy(alpha = 0.14f),
                    borderColor = TacticalPrimaryRust.copy(alpha = 0.35f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(quantity) }) {
                Text("UPDATE", style = MaterialTheme.typography.labelLarge, color = TacticalPrimaryRust)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }
    )
}

// ── Add item dialog ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: ItemCategory, qty: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ItemCategory.GEAR) }
    var quantityText by remember { mutableStateOf("1") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val quantity = quantityText.toIntOrNull() ?: 0
    val previewStatus = when {
        quantity <= 3  -> StockStatus.CRITICAL
        quantity <= 8  -> StockStatus.LOW
        else           -> StockStatus.STOCKED
    }
    val isValid = name.isNotBlank() && quantity > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        title = {
            Text(
                "// ADD SUPPLY ITEM",
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 2.sp,
                color = TacticalPrimaryRust
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ITEM NAME", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalPrimaryRust,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                        focusedLabelColor = TacticalPrimaryRust
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("CATEGORY", style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalPrimaryRust,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                            focusedLabelColor = TacticalPrimaryRust
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ItemCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(cat.label, style = MaterialTheme.typography.bodyMedium)
                                },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { v -> if (v.length <= 4) quantityText = v.filter { it.isDigit() } },
                    label = { Text("QUANTITY", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalPrimaryRust,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                        focusedLabelColor = TacticalPrimaryRust
                    ),
                    supportingText = {
                        if (quantity > 0) {
                            val (label, color) = when (previewStatus) {
                                StockStatus.STOCKED  -> "STOCKED"  to MaterialTheme.colorScheme.secondary
                                StockStatus.LOW      -> "LOW"      to MaterialTheme.colorScheme.tertiary
                                StockStatus.CRITICAL -> "CRITICAL" to MaterialTheme.colorScheme.error
                            }
                            Text("Status: $label", style = MaterialTheme.typography.labelSmall, color = color)
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), selectedCategory, quantity) },
                enabled = isValid
            ) {
                Text(
                    "ADD ITEM",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isValid) TacticalPrimaryRust else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "CANCEL",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    )
}
