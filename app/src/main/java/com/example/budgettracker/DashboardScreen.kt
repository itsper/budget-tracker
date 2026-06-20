package com.example.budgettracker

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.ui.theme.BudgetTrackerTheme
import java.text.DecimalFormat

// ─── Data Models ───────────────────────────────────────────────────────────────

data class CurrencyData(val symbol: String, val code: String)
data class ExpenseItem(
    val name: String,
    val amount: String,
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val isDeducted: Boolean = false
)
data class CategoryInfo(val icon: ImageVector, val color: Color)

// ─── Helpers ───────────────────────────────────────────────────────────────────

private val decimalFormat = DecimalFormat("#,##0.00")

fun formatCurrency(value: Double): String = decimalFormat.format(value)

fun getCategoryInfo(name: String): CategoryInfo = when (name.lowercase()) {
    "food", "groceries", "dining", "restaurant" ->
        CategoryInfo(Icons.Filled.Restaurant, Color(0xFFFF6B35))
    "electricity", "electric", "utility", "utilities" ->
        CategoryInfo(Icons.Filled.Bolt, Color(0xFFFFB300))
    "water" ->
        CategoryInfo(Icons.Filled.WaterDrop, Color(0xFF29B6F6))
    "internet", "wifi" ->
        CategoryInfo(Icons.Filled.Wifi, Color(0xFF7E57C2))
    "transport", "transportation", "gas", "fuel" ->
        CategoryInfo(Icons.Filled.DirectionsCar, Color(0xFF26A69A))
    "shopping", "clothes" ->
        CategoryInfo(Icons.Filled.ShoppingBag, Color(0xFFEC407A))
    "entertainment", "movies" ->
        CategoryInfo(Icons.Filled.Movie, Color(0xFF5C6BC0))
    "health", "medical" ->
        CategoryInfo(Icons.Filled.HealthAndSafety, Color(0xFFEF5350))
    "education", "school", "tuition" ->
        CategoryInfo(Icons.Filled.School, Color(0xFF66BB6A))
    "rent", "housing" ->
        CategoryInfo(Icons.Filled.Home, Color(0xFF8D6E63))
    "phone", "mobile" ->
        CategoryInfo(Icons.Filled.PhoneAndroid, Color(0xFF78909C))
    "savings" ->
        CategoryInfo(Icons.Filled.Savings, Color(0xFF26A69A))
    else ->
        CategoryInfo(Icons.Filled.Receipt, Color(0xFF78909C))
}

// ─── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("BudgetTrackerPrefs", Context.MODE_PRIVATE)
    }

    val currencies = listOf(
        CurrencyData("₱", "PHP"),
        CurrencyData("$", "USD"),
        CurrencyData("€", "EUR"),
        CurrencyData("¥", "JPY"),
        CurrencyData("₩", "KRW")
    )

    var selectedCurrencyIndex by remember {
        mutableStateOf(sharedPreferences.getInt("KEY_CURRENCY_INDEX", 0))
    }
    val selectedCurrency = currencies[selectedCurrencyIndex]

    var balanceAmount by remember {
        mutableStateOf(sharedPreferences.getString("KEY_BALANCE", "12500.00") ?: "12500.00")
    }

    val expenseList = remember {
        mutableStateListOf<ExpenseItem>().apply {
            if (sharedPreferences.contains("KEY_EXPENSES")) {
                val savedData = sharedPreferences.getString("KEY_EXPENSES", "") ?: ""
                if (savedData.isNotEmpty()) {
                    savedData.split(";").forEach { itemStr ->
                        val parts = itemStr.split(":")
                        if (parts.size >= 2) {
                            val name = parts[0]
                            val amount = parts[1]
                            val timestamp = if (parts.size >= 3) parts[2].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
                            val id = if (parts.size >= 4) parts[3] else java.util.UUID.randomUUID().toString()
                            val isDeducted = if (parts.size >= 5) parts[4].toBoolean() else false
                            add(ExpenseItem(name = name, amount = amount, id = id, timestamp = timestamp, isDeducted = isDeducted))
                        }
                    }
                }
            } else {
                addAll(
                    listOf(
                        ExpenseItem("Food", "150.00", isDeducted = true),
                        ExpenseItem("Electricity", "2500.00", isDeducted = true),
                        ExpenseItem("Water", "350.00", isDeducted = true),
                        ExpenseItem("Internet", "1499.00", isDeducted = true)
                    )
                )
            }
        }
    }

    val historyList = remember {
        mutableStateListOf<ExpenseItem>().apply {
            if (sharedPreferences.contains("KEY_HISTORY")) {
                val savedData = sharedPreferences.getString("KEY_HISTORY", "") ?: ""
                if (savedData.isNotEmpty()) {
                    savedData.split(";").forEach { itemStr ->
                        val parts = itemStr.split(":")
                        if (parts.size >= 2) {
                            val name = parts[0]
                            val amount = parts[1]
                            val timestamp = if (parts.size >= 3) parts[2].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
                            val id = if (parts.size >= 4) parts[3] else java.util.UUID.randomUUID().toString()
                            val isDeducted = if (parts.size >= 5) parts[4].toBoolean() else false
                            add(ExpenseItem(name = name, amount = amount, id = id, timestamp = timestamp, isDeducted = isDeducted))
                        }
                    }
                }
            } else {
                addAll(
                    listOf(
                        ExpenseItem("Food", "150.00", isDeducted = true),
                        ExpenseItem("Electricity", "2500.00", isDeducted = true),
                        ExpenseItem("Water", "350.00", isDeducted = true),
                        ExpenseItem("Internet", "1499.00", isDeducted = true)
                    )
                )
            }
        }
    }

    fun saveData() {
        val serializedExpenses = expenseList.joinToString(";") { "${it.name}:${it.amount}:${it.timestamp}:${it.id}:${it.isDeducted}" }
        val serializedHistory = historyList.joinToString(";") { "${it.name}:${it.amount}:${it.timestamp}:${it.id}:${it.isDeducted}" }
        sharedPreferences.edit().apply {
            putString("KEY_BALANCE", balanceAmount)
            putInt("KEY_CURRENCY_INDEX", selectedCurrencyIndex)
            putString("KEY_EXPENSES", serializedExpenses)
            putString("KEY_HISTORY", serializedHistory)
            apply()
        }
    }

    // Derived calculations
    val balanceValue by remember(balanceAmount) {
        derivedStateOf { balanceAmount.toDoubleOrNull() ?: 0.0 }
    }
    val totalExpenses by remember {
        derivedStateOf {
            historyList.filter { it.isDeducted }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        }
    }
    val deductedExpensesCount by remember {
        derivedStateOf { historyList.count { it.isDeducted } }
    }
    val remaining by remember {
        derivedStateOf { balanceValue - totalExpenses }
    }
    val progress by remember {
        derivedStateOf {
            if (balanceValue > 0)
                (totalExpenses / balanceValue).coerceIn(0.0, 1.0).toFloat() else 0f
        }
    }

    // Sheet / dialog state
    var showAddSheet by remember { mutableStateOf(false) }
    var showEditBalance by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var deletingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var newExpenseName by remember { mutableStateOf("") }
    var newExpenseAmount by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Top spacer ──
            item { Spacer(Modifier.height(8.dp)) }

            // ── Header ──
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Budget Tracker",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            "Manage your finances smartly",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }

            // ── Gradient Balance Card ──
            item {
                val gradient = Brush.linearGradient(
                    listOf(
                        Color(0xFF1E88E5),
                        Color(0xFF1565C0)
                    )
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditBalance = true },
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(gradient, RoundedCornerShape(24.dp))
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            // Top row: label + currency picker
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "CURRENT BALANCE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.85f)
                                )

                                var dropdownExpanded by remember { mutableStateOf(false) }
                                Box {
                                    Surface(
                                        onClick = { dropdownExpanded = true },
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.18f),
                                        contentColor = Color.White
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                selectedCurrency.code,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                            Icon(
                                                Icons.Filled.ArrowDropDown, null,
                                                Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false }
                                    ) {
                                        currencies.forEachIndexed { idx, cur ->
                                            DropdownMenuItem(
                                                text = { Text("${cur.symbol}  ${cur.code}") },
                                                onClick = {
                                                    selectedCurrencyIndex = idx
                                                    dropdownExpanded = false
                                                    saveData()
                                                },
                                                trailingIcon = {
                                                    if (idx == selectedCurrencyIndex)
                                                        Icon(
                                                            Icons.Filled.Check, null,
                                                            Modifier.size(18.dp),
                                                            MaterialTheme.colorScheme.primary
                                                        )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Big balance number
                            Text(
                                "${selectedCurrency.symbol}${formatCurrency(balanceValue)}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 36.sp
                                ),
                                color = Color.White
                            )

                            Spacer(Modifier.height(20.dp))

                            // Progress bar
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color.White.copy(alpha = 0.9f),
                                trackColor = Color.White.copy(alpha = 0.25f),
                            )
                        }
                    }
                }
            }

            // ── Quick Stats Row ──
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Receipt,
                        value = "${selectedCurrency.symbol}${formatCurrency(totalExpenses)}",
                        label = "Total Expenses",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = onNavigateToExpenses
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Analytics,
                        value = "${selectedCurrency.symbol}${
                            formatCurrency(
                                if (deductedExpensesCount > 0) totalExpenses / deductedExpensesCount else 0.0
                            )
                        }",
                        label = "Average",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // ── Section Header ──
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Expenses",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    TextButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add New", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Expense Items or Empty State ──
            if (expenseList.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.ReceiptLong, null,
                                Modifier.size(52.dp),
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No expenses yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Tap + to add your first expense",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    expenseList,
                    key = { _, expense -> expense.id }
                ) { index, expense ->
                    val category = remember(expense.name) { getCategoryInfo(expense.name) }
                    val amount = remember(expense.amount) { expense.amount.toDoubleOrNull() ?: 0.0 }
                    val pct = remember(amount, totalExpenses) {
                        if (totalExpenses > 0) (amount / totalExpenses * 100).toInt() else 0
                    }

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                expenseList.remove(expense)
                                saveData()
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { editingExpense = expense },
                                    onLongClick = { deletingExpense = expense }
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                        ) {
                            Column {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Category icon
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(category.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            category.icon, null,
                                            tint = category.color,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    // Name + percentage
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            expense.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "$pct% of total",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Amount
                                    Text(
                                        "${selectedCurrency.symbol}${formatCurrency(amount)}",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.error
                                    )

                                    // Deduct from Balance
                                    IconButton(
                                        onClick = {
                                            val expenseAmount = expense.amount.toDoubleOrNull() ?: 0.0
                                            val currentBalance = balanceAmount.toDoubleOrNull() ?: 0.0
                                            if (currentBalance < expenseAmount) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "You don't have enough balance",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                balanceAmount = String.format(
                                                    java.util.Locale.US,
                                                    "%.2f",
                                                    currentBalance - expenseAmount
                                                )
                                                val targetIndex = expenseList.indexOfFirst { it.id == expense.id }
                                                if (targetIndex != -1) {
                                                    if (!expense.isDeducted) {
                                                        expenseList[targetIndex] = expense.copy(isDeducted = true)
                                                    }
                                                }
                                                val newHistoryItem = ExpenseItem(
                                                    name = expense.name,
                                                    amount = expense.amount,
                                                    isDeducted = true,
                                                    timestamp = System.currentTimeMillis()
                                                )
                                                historyList.add(newHistoryItem)
                                                saveData()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_sent),
                                            contentDescription = "Deduct from Balance",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Mini progress bar
                                if (totalExpenses > 0) {
                                    LinearProgressIndicator(
                                        progress = {
                                            (amount / totalExpenses).coerceIn(0.0, 1.0).toFloat()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(2.dp)
                                            .clip(RoundedCornerShape(1.dp)),
                                        color = category.color.copy(alpha = 0.5f),
                                        trackColor = Color.Transparent,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacing for FAB
            item { Spacer(Modifier.height(96.dp)) }
        }
    }

    // ── Add Expense Bottom Sheet ──
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddSheet = false
                newExpenseName = ""
                newExpenseAmount = ""
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Add New Expense",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Fill in the details below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = newExpenseName,
                    onValueChange = { newExpenseName = it },
                    label = { Text("Expense Name") },
                    leadingIcon = { Icon(Icons.Filled.Receipt, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = newExpenseAmount,
                    onValueChange = { newExpenseAmount = it },
                    label = { Text("Amount") },
                    leadingIcon = {
                        Text(
                            selectedCurrency.symbol,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (newExpenseName.isNotBlank() && newExpenseAmount.isNotBlank()) {
                            expenseList.add(ExpenseItem(newExpenseName, newExpenseAmount))
                            newExpenseName = ""
                            newExpenseAmount = ""
                            saveData()
                            showAddSheet = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = newExpenseName.isNotBlank() && newExpenseAmount.isNotBlank()
                ) {
                    Icon(Icons.Filled.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Expense", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // ── Edit Balance Dialog ──
    if (showEditBalance) {
        var editValue by remember { mutableStateOf(balanceAmount) }
        AlertDialog(
            onDismissRequest = { showEditBalance = false },
            title = { Text("Edit Balance", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text("Balance Amount") },
                    leadingIcon = {
                        Text(selectedCurrency.symbol, style = MaterialTheme.typography.bodyLarge)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                FilledTonalButton(onClick = {
                    balanceAmount = editValue
                    saveData()
                    showEditBalance = false
                }) { Text("Save", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditBalance = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── Edit Expense Dialog ──
    if (editingExpense != null) {
        val expense = editingExpense!!
        var editName by remember(expense) { mutableStateOf(expense.name) }
        var editAmount by remember(expense) { mutableStateOf(expense.amount) }

        AlertDialog(
            onDismissRequest = { editingExpense = null },
            title = { Text("Edit Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        leadingIcon = { Icon(Icons.Filled.Receipt, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Amount") },
                        leadingIcon = {
                            Text(selectedCurrency.symbol, style = MaterialTheme.typography.bodyLarge)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(onClick = {
                    if (editName.isNotBlank() && editAmount.isNotBlank()) {
                        val targetIndex = expenseList.indexOfFirst { it.id == expense.id }
                        if (targetIndex != -1) {
                            expenseList[targetIndex] = ExpenseItem(
                                name = editName,
                                amount = editAmount,
                                id = expense.id,
                                timestamp = expense.timestamp,
                                isDeducted = expense.isDeducted
                            )
                            saveData()
                        }
                    }
                    editingExpense = null
                }) { Text("Save", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { editingExpense = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── Delete Expense Dialog ──
    if (deletingExpense != null) {
        val expense = deletingExpense!!
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text("Delete Expense", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${expense.name}\"?") },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        expenseList.remove(expense)
                        saveData()
                        deletingExpense = null
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ─── Extracted Composables ─────────────────────────────────────────────────────

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

// ─── Preview ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    BudgetTrackerTheme { DashboardScreen() }
}