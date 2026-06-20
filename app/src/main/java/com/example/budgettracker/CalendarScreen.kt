package com.example.budgettracker

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

// ── Data Models ───────────────────────────────────────────────────────────────

data class SourceOfMoney(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val amount: String,
    val dateTime: Long, // timestamp
    val intervalType: String, // "NONE", "DAYS", "MONTHS"
    val intervalValue: Int
)

data class SourceHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val amount: String,
    val timestamp: Long
)

// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
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
    val selectedCurrencyIndex = sharedPreferences.getInt("KEY_CURRENCY_INDEX", 0)
    val selectedCurrency = currencies[selectedCurrencyIndex]

    // 1. Scheduled Sources of Money
    val sourceList = remember {
        mutableStateListOf<SourceOfMoney>().apply {
            val savedData = sharedPreferences.getString("KEY_SOURCES", "") ?: ""
            if (savedData.isNotEmpty()) {
                savedData.split(";").forEach { itemStr ->
                    val parts = itemStr.split(":")
                    if (parts.size >= 6) {
                        add(
                            SourceOfMoney(
                                id = parts[0],
                                name = parts[1],
                                amount = parts[2],
                                dateTime = parts[3].toLongOrNull() ?: 0L,
                                intervalType = parts[4],
                                intervalValue = parts[5].toIntOrNull() ?: 0
                            )
                        )
                    }
                }
            }
        }
    }

    // 2. History of Money Additions
    val historyList = remember {
        mutableStateListOf<SourceHistoryItem>().apply {
            val savedData = sharedPreferences.getString("KEY_SOURCE_HISTORY", "") ?: ""
            if (savedData.isNotEmpty()) {
                savedData.split(";").forEach { itemStr ->
                    val parts = itemStr.split(":")
                    if (parts.size >= 4) {
                        add(
                            SourceHistoryItem(
                                id = parts[0],
                                name = parts[1],
                                amount = parts[2],
                                timestamp = parts[3].toLongOrNull() ?: 0L
                            )
                        )
                    }
                }
            }
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var deletingSource by remember { mutableStateOf<SourceOfMoney?>(null) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    // Helpers
    fun saveData() {
        val serializedSources = sourceList.joinToString(";") {
            "${it.id}:${it.name}:${it.amount}:${it.dateTime}:${it.intervalType}:${it.intervalValue}"
        }
        val serializedHistory = historyList.joinToString(";") {
            "${it.id}:${it.name}:${it.amount}:${it.timestamp}"
        }
        sharedPreferences.edit().apply {
            putString("KEY_SOURCES", serializedSources)
            putString("KEY_SOURCE_HISTORY", serializedHistory)
            apply()
        }
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd  •  hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun scheduleAlarm(source: SourceOfMoney) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, IncomeReceiver::class.java).apply {
            putExtra("KEY_INCOME_ID", source.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            source.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    source.dateTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    source.dateTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback if SCHEDULE_EXACT_ALARM permission is missing on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    source.dateTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    source.dateTime,
                    pendingIntent
                )
            }
        }
    }

    fun cancelAlarm(sourceId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, IncomeReceiver::class.java).apply {
            putExtra("KEY_INCOME_ID", sourceId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sourceId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    // Manual Execution
    fun executeManually(source: SourceOfMoney) {
        val amountValue = source.amount.toDoubleOrNull() ?: 0.0
        val currentBalance = sharedPreferences.getString("KEY_BALANCE", "12500.00")?.toDoubleOrNull() ?: 12500.00
        val newBalance = currentBalance + amountValue
        val newBalanceStr = String.format(Locale.US, "%.2f", newBalance)

        sharedPreferences.edit().putString("KEY_BALANCE", newBalanceStr).apply()

        // Log to history
        val newHistory = SourceHistoryItem(
            name = source.name,
            amount = source.amount,
            timestamp = System.currentTimeMillis()
        )
        historyList.add(newHistory)
        saveData()
        sendLocalNotification(context, source.name, source.amount)

        Toast.makeText(context, "${selectedCurrency.symbol}${source.amount} added to balance!", Toast.LENGTH_SHORT).show()
    }

    // Permission Launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Toast.makeText(context, "Notification permission is required for auto-updates", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Source of Money")
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
            // ── Top Header ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sources of Money",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Manage scheduled salaries, allowance and income",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Scheduled Sources List ──
            if (sourceList.isEmpty()) {
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
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Payments, null,
                                Modifier.size(44.dp),
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No active income sources",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Tap + to add your salary or allowance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(sourceList, key = { it.id }) { source ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { },
                                onLongClick = { deletingSource = source }
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon Box
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AccountBalanceWallet, null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            // Name + Next trigger time details
                            Column(Modifier.weight(1f)) {
                                Text(
                                    source.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when (source.intervalType) {
                                        "NONE" -> "One-time: ${formatShortDate(source.dateTime)}"
                                        "DAYS" -> "Every ${source.intervalValue} days: ${formatShortDate(source.dateTime)}"
                                        "MONTHS" -> "Every ${source.intervalValue} months: ${formatShortDate(source.dateTime)}"
                                        else -> formatShortDate(source.dateTime)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Amount
                            Text(
                                "+${selectedCurrency.symbol}${source.amount}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            )

                            Spacer(Modifier.width(8.dp))

                            // Execute manually now (Sent Icon)
                            IconButton(
                                onClick = { executeManually(source) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sent),
                                    contentDescription = "Add to Balance Now",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // ── Section Divider / Header for History ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Income History",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    if (historyList.isNotEmpty()) {
                        TextButton(onClick = { showClearHistoryDialog = true }) {
                            Text(
                                "Empty History",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── History List ──
            if (historyList.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No history recorded yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Show latest first
                val sortedHistory = historyList.sortedByDescending { it.timestamp }
                items(sortedHistory, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle, null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    formatDateTime(item.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                "+${selectedCurrency.symbol}${item.amount}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    // ── Add Source Dialog ──
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var dateTimeStamp by remember { mutableStateOf(0L) }
        var dateTimeText by remember { mutableStateOf("Select Date & Time") }

        // Interval Type dropdown/options
        var expandedIntervalDropdown by remember { mutableStateOf(false) }
        val intervalTypes = listOf("One-time", "Every X Days", "Every X Months")
        var selectedIntervalTypeIndex by remember { mutableStateOf(0) }
        var intervalValueText by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Source of Money", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Name field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Source Name (e.g. Salary, Allowance)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Amount field
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        leadingIcon = { Text(selectedCurrency.symbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Date & Time Picker trigger
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val targetCalendar = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                set(Calendar.MINUTE, minute)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            dateTimeStamp = targetCalendar.timeInMillis
                                            dateTimeText = SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault()).format(targetCalendar.time)
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(dateTimeText, fontWeight = FontWeight.SemiBold)
                    }

                    // Interval dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { expandedIntervalDropdown = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = intervalTypes[selectedIntervalTypeIndex],
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Icon(Icons.Filled.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedIntervalDropdown,
                            onDismissRequest = { expandedIntervalDropdown = false }
                        ) {
                            intervalTypes.forEachIndexed { idx, type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedIntervalTypeIndex = idx
                                        expandedIntervalDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Dynamic interval value input (only if not One-time)
                    if (selectedIntervalTypeIndex > 0) {
                        OutlinedTextField(
                            value = intervalValueText,
                            onValueChange = { intervalValueText = it },
                            label = {
                                Text(
                                    if (selectedIntervalTypeIndex == 1) "Number of Days (e.g. 24)"
                                    else "Number of Months (e.g. 1)"
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val nameStr = name.trim()
                        val amtStr = amount.trim()
                        if (nameStr.isNotBlank() && amtStr.isNotBlank() && dateTimeStamp > 0L) {
                            val type = when (selectedIntervalTypeIndex) {
                                1 -> "DAYS"
                                2 -> "MONTHS"
                                else -> "NONE"
                            }
                            val valInt = intervalValueText.toIntOrNull() ?: 1

                            val newSource = SourceOfMoney(
                                name = nameStr,
                                amount = amtStr,
                                dateTime = dateTimeStamp,
                                intervalType = type,
                                intervalValue = valInt
                            )

                            val now = System.currentTimeMillis()
                            if (dateTimeStamp <= now) {
                                // 1. Run the manual addition immediately
                                val amountValue = amtStr.toDoubleOrNull() ?: 0.0
                                val currentBalance = sharedPreferences.getString("KEY_BALANCE", "12500.00")?.toDoubleOrNull() ?: 12500.00
                                val newBalance = currentBalance + amountValue
                                val newBalanceStr = String.format(Locale.US, "%.2f", newBalance)
                                sharedPreferences.edit().putString("KEY_BALANCE", newBalanceStr).apply()

                                // Log to history
                                val newHistory = SourceHistoryItem(
                                    name = nameStr,
                                    amount = amtStr,
                                    timestamp = System.currentTimeMillis()
                                )
                                historyList.add(newHistory)
                                
                                // Send notification immediately
                                sendLocalNotification(context, nameStr, amtStr)
                                Toast.makeText(context, "${selectedCurrency.symbol}$amtStr added to balance!", Toast.LENGTH_SHORT).show()

                                // 2. If recurring, calculate the next future alarm trigger time and schedule it!
                                if (type != "NONE") {
                                    val calendar = Calendar.getInstance().apply {
                                        timeInMillis = dateTimeStamp
                                    }
                                    while (calendar.timeInMillis <= now) {
                                        if (type == "DAYS") {
                                            calendar.add(Calendar.DAY_OF_YEAR, valInt)
                                        } else if (type == "MONTHS") {
                                            calendar.add(Calendar.MONTH, valInt)
                                        }
                                    }
                                    val nextRecurringSource = newSource.copy(dateTime = calendar.timeInMillis)
                                    sourceList.add(nextRecurringSource)
                                    saveData()
                                    scheduleAlarm(nextRecurringSource)
                                } else {
                                    saveData()
                                }
                            } else {
                                // Future alarm, schedule normally
                                sourceList.add(newSource)
                                saveData()
                                scheduleAlarm(newSource)
                            }
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "Please complete all inputs", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Schedule", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── Delete Confirmation Dialog ──
    if (deletingSource != null) {
        val source = deletingSource!!
        AlertDialog(
            onDismissRequest = { deletingSource = null },
            title = { Text("Delete Income Source", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to stop scheduling \"${source.name}\"?") },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        cancelAlarm(source.id)
                        sourceList.remove(source)
                        saveData()
                        deletingSource = null
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
                TextButton(onClick = { deletingSource = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── Clear All History Dialog ──
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Income History", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all recorded income history? This action cannot be undone.") },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        historyList.clear()
                        saveData()
                        showClearHistoryDialog = false
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
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

private fun sendLocalNotification(context: Context, name: String, amount: String) {
    val channelId = "income_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            channelId,
            "Income Notifications",
            android.app.NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when a source of money is added to balance"
        }
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val sharedPreferences = context.getSharedPreferences("BudgetTrackerPrefs", Context.MODE_PRIVATE)
    val selectedCurrencyIndex = sharedPreferences.getInt("KEY_CURRENCY_INDEX", 0)
    val symbol = when (selectedCurrencyIndex) {
        0 -> "₱"
        1 -> "$"
        2 -> "€"
        3 -> "¥"
        4 -> "₩"
        else -> "₱"
    }

    val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Money Added!")
        .setContentText("$symbol$amount is added to your balance from $name.")
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}
