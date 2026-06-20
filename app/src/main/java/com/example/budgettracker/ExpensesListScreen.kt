package com.example.budgettracker

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesListScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("BudgetTrackerPrefs", Context.MODE_PRIVATE)
    }

    var deletingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val currencies = listOf(
        CurrencyData("₱", "PHP"),
        CurrencyData("$", "USD"),
        CurrencyData("€", "EUR"),
        CurrencyData("¥", "JPY"),
        CurrencyData("₩", "KRW")
    )
    val selectedCurrencyIndex = sharedPreferences.getInt("KEY_CURRENCY_INDEX", 0)
    val selectedCurrency = currencies[selectedCurrencyIndex]

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
            }
        }
    }

    // Derived total
    val totalExpenses by remember {
        derivedStateOf {
            historyList.filter { it.isDeducted }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        }
    }

    // Helper functions for date formatting
    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun saveData() {
        val serialized = historyList.joinToString(";") { "${it.name}:${it.amount}:${it.timestamp}:${it.id}:${it.isDeducted}" }
        sharedPreferences.edit().apply {
            putString("KEY_HISTORY", serialized)
            apply()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header & Action ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expense History",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (historyList.isNotEmpty()) {
                        TextButton(onClick = { showClearAllDialog = true }) {
                            Text(
                                "Empty List",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── Summary Card ──
            item {
                val gradient = Brush.linearGradient(
                    listOf(
                        Color(0xFFE91E63),
                        Color(0xFFC2185B)
                    )
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(gradient, RoundedCornerShape(24.dp))
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Text(
                                "TOTAL EXPENSES",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${selectedCurrency.symbol}${formatCurrency(totalExpenses)}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp
                                ),
                                color = Color.White
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${historyList.size} items recorded",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // ── Expenses List Header ──
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "All Records",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── List items ──
            if (historyList.isEmpty()) {
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
                                "No expenses recorded",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Show latest first
                val sortedList = historyList.sortedByDescending { it.timestamp }
                items(sortedList, key = { it.id }) { expense ->
                    val category = getCategoryInfo(expense.name)
                    val amount = expense.amount.toDoubleOrNull() ?: 0.0

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                historyList.remove(expense)
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
                                    onClick = {},
                                    onLongClick = { deletingExpense = expense }
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                        ) {
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

                                // Name + Commit Time
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        expense.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            formatDateTime(expense.timestamp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!expense.isDeducted) {
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Pending",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Amount
                                Text(
                                    "-${selectedCurrency.symbol}${formatCurrency(amount)}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ── Delete Single Expense Dialog ──
    if (deletingExpense != null) {
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text("Delete Expense", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${deletingExpense?.name}\"?") },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        deletingExpense?.let {
                            historyList.remove(it)
                            saveData()
                        }
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

    // ── Clear All Expenses Dialog ──
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Expenses", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all recorded expenses? This action cannot be undone.") },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        historyList.clear()
                        saveData()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
