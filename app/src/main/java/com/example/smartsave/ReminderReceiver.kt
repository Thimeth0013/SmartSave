package com.example.smartsave

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Check POST_NOTIFICATIONS permission
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed below API 33
        }

        if (!hasPermission) {
            Log.d("ReminderReceiver", "Cannot post notification: POST_NOTIFICATIONS permission missing")
            return
        }

        val reminderType = intent.getStringExtra("reminderType") ?: return

        // Create notification channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (reminderType) {
                "expense" -> createNotificationChannel(
                    context,
                    "expense_reminder_channel",
                    "Expense Reminder",
                    "Daily reminders to add expenses"
                )
                "budget" -> createNotificationChannel(
                    context,
                    "budget_reminder_channel",
                    "Budget Reminder",
                    "Notifications for budget thresholds"
                )
            }
        }

        try {
            when (reminderType) {
                "expense" -> {
                    val notification = NotificationCompat.Builder(context, "expense_reminder_channel")
                        .setSmallIcon(R.drawable.mainlogo) // Replace with your app's notification icon
                        .setContentTitle("SmartSave: Add Today's Expenses")
                        .setContentText("Don't forget to log your expenses for today!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()
                    with(NotificationManagerCompat.from(context)) {
                        notify(1, notification)
                    }
                }
                "budget" -> {
                    val category = intent.getStringExtra("category") ?: "Unknown"
                    val usagePercent = intent.getDoubleExtra("usagePercent", 0.0)
                    val message = if (usagePercent >= 100) {
                        "Budget for $category has been exceeded!"
                    } else {
                        "Budget for $category is nearing its limit (${String.format("%.0f%%", usagePercent)})"
                    }
                    val notification = NotificationCompat.Builder(context, "budget_reminder_channel")
                        .setSmallIcon(R.drawable.mainlogo) // Replace with your app's notification icon
                        .setContentTitle("SmartSave: Budget Alert")
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()
                    with(NotificationManagerCompat.from(context)) {
                        notify(category.hashCode(), notification)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("ReminderReceiver", "Failed to post notification: ${e.message}", e)
        }
    }

    private fun createNotificationChannel(context: Context, channelId: String, channelName: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                this.description = description
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}