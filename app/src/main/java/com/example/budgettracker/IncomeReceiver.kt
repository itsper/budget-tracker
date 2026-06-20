package com.example.budgettracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class IncomeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val incomeId = intent.getStringExtra("KEY_INCOME_ID") ?: return
        val sharedPreferences = context.getSharedPreferences("BudgetTrackerPrefs", Context.MODE_PRIVATE)

        // 1. Load sources of money
        val sourcesSerialized = sharedPreferences.getString("KEY_SOURCES", "") ?: ""
        val sources = mutableListOf<SourceOfMoney>()
        if (sourcesSerialized.isNotEmpty()) {
            sourcesSerialized.split(";").forEach { itemStr ->
                val parts = itemStr.split(":")
                if (parts.size >= 6) {
                    sources.add(
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

        val source = sources.find { it.id == incomeId } ?: return

        // 2. Update balance
        val amountValue = source.amount.toDoubleOrNull() ?: 0.0
        val currentBalance = sharedPreferences.getString("KEY_BALANCE", "12500.00")?.toDoubleOrNull() ?: 12500.00
        val newBalance = currentBalance + amountValue
        val newBalanceStr = String.format(java.util.Locale.US, "%.2f", newBalance)

        // 3. Add to history
        val historySerialized = sharedPreferences.getString("KEY_SOURCE_HISTORY", "") ?: ""
        val newHistoryItem = "${java.util.UUID.randomUUID()}:${source.name}:${source.amount}:${System.currentTimeMillis()}"
        val updatedHistory = if (historySerialized.isEmpty()) newHistoryItem else "$historySerialized;$newHistoryItem"

        // 4. If recurring, schedule the next alarm!
        if (source.intervalType != "NONE") {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = source.dateTime
            }
            if (source.intervalType == "DAYS") {
                calendar.add(Calendar.DAY_OF_YEAR, source.intervalValue)
            } else if (source.intervalType == "MONTHS") {
                calendar.add(Calendar.MONTH, source.intervalValue)
            }
            val nextTime = calendar.timeInMillis

            // Update source time in list
            val targetIdx = sources.indexOfFirst { it.id == source.id }
            if (targetIdx != -1) {
                sources[targetIdx] = source.copy(dateTime = nextTime)
            }

            // Reschedule in AlarmManager
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val nextIntent = Intent(context, IncomeReceiver::class.java).apply {
                putExtra("KEY_INCOME_ID", source.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                source.id.hashCode(),
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        nextTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        nextTime,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // Fallback for Android 12+ if SCHEDULE_EXACT_ALARM permission is not granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        nextTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        android.app.AlarmManager.RTC_WAKEUP,
                        nextTime,
                        pendingIntent
                    )
                }
            }
        }

        // Save updated sources, history and balance
        val updatedSourcesSerialized = sources.joinToString(";") {
            "${it.id}:${it.name}:${it.amount}:${it.dateTime}:${it.intervalType}:${it.intervalValue}"
        }
        sharedPreferences.edit().apply {
            putString("KEY_BALANCE", newBalanceStr)
            putString("KEY_SOURCE_HISTORY", updatedHistory)
            putString("KEY_SOURCES", updatedSourcesSerialized)
            apply()
        }

        // 5. Send notification to the user
        sendNotification(context, source.name, source.amount)
    }

    private fun sendNotification(context: Context, name: String, amount: String) {
        val channelId = "income_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Income Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a source of money is added to balance"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open MainActivity when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Read selected currency symbol
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

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Money Added!")
            .setContentText("$symbol$amount is added to your balance from $name.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
