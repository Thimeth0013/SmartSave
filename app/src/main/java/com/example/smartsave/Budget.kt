package com.example.smartsave

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartsave.data.TransactionDataManager
import com.example.smartsave.data.Utils
import com.example.smartsave.model.TransactionType
import java.util.Calendar

class Budget : AppCompatActivity() {
    private lateinit var dataManager: TransactionDataManager
    private lateinit var budgetInput: EditText
    private lateinit var setBudgetButton: Button
    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var currentExpensesText: TextView
    private lateinit var remainingBudgetText: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private var hasExceededBudgetNotified = false // Prevent multiple notifications

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "currencyType") {
            updateBudgetUI()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            updateBudgetUI() // Re-run UI update to send notification if needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dataManager = TransactionDataManager(this)
        sharedPreferences = getSharedPreferences("SmartSavePrefs", Context.MODE_PRIVATE)

        // Initialize views
        budgetInput = findViewById(R.id.budgetInput)
        setBudgetButton = findViewById(R.id.setBudgetButton)
        budgetProgressBar = findViewById(R.id.budgetProgressBar)
        currentExpensesText = findViewById(R.id.currentExpensesText)
        remainingBudgetText = findViewById(R.id.remainingBudgetText)

        // Create notification channel
        createNotificationChannel()

        // Request notification permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Load saved budget and update UI
        updateBudgetUI()

        // Set up the Set Budget button
        setBudgetButton.setOnClickListener {
            val budgetAmount = budgetInput.text.toString().toDoubleOrNull() ?: 0.0
            if (budgetAmount > 0) {
                saveBudget(budgetAmount)
                hasExceededBudgetNotified = false // Reset notification flag when budget changes
                updateBudgetUI()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        updateBudgetUI()
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Notifications"
            val descriptionText = "Notifications for budget exceeding limits"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("BUDGET_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendBudgetExceededNotification() {
        val builder = NotificationCompat.Builder(this, "BUDGET_CHANNEL_ID")
            .setSmallIcon(R.drawable.notifications_26dp_3b82f6_fill0_wght400_grad0_opsz24) // Replace with your notification icon
            .setContentTitle("Budget Exceeded")
            .setContentText("Your expenses have exceeded your monthly budget!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this@Budget,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(1, builder.build())
                }
            } else {
                notify(1, builder.build())
            }
        }
    }

    private fun saveBudget(budgetAmount: Double) {
        with(sharedPreferences.edit()) {
            putFloat("monthlyBudget", budgetAmount.toFloat())
            apply()
        }
    }

    private fun getSavedBudget(): Double {
        return sharedPreferences.getFloat("monthlyBudget", 0f).toDouble()
    }

    private fun updateBudgetUI() {
        val budget = getSavedBudget()
        budgetInput.setText(if (budget > 0) budget.toString() else "")

        // Calculate current month's expenses
        val transactions = dataManager.readAllTransactions()
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        var currentExpenses = 0.0

        transactions.forEach { transaction ->
            val transactionCalendar = Calendar.getInstance().apply { time = transaction.date }
            val transactionMonth = transactionCalendar.get(Calendar.MONTH)
            val transactionYear = transactionCalendar.get(Calendar.YEAR)

            if (transaction.type == TransactionType.EXPENSE &&
                transactionMonth == currentMonth &&
                transactionYear == currentYear
            ) {
                currentExpenses += transaction.amount
            }
        }

        // Update UI with formatted amounts
        currentExpensesText.text = "Current Expenses: ${Utils.formatAmount(this, currentExpenses)}"

        if (budget > 0) {
            val remainingBudget = budget - currentExpenses
            val progress = if (budget > 0) ((currentExpenses / budget) * 100).coerceAtMost(100.0).toInt() else 0
            budgetProgressBar.progress = progress
            remainingBudgetText.text = "Remaining Budget: ${Utils.formatAmount(this, remainingBudget)}"
            remainingBudgetText.setTextColor(
                if (remainingBudget >= 0) resources.getColor(R.color.green)
                else resources.getColor(R.color.red)
            )

            // Send notification if budget is exceeded and notifications are enabled
            if (remainingBudget < 0 && !hasExceededBudgetNotified) {
                val notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", false)
                if (notificationsEnabled) {
                    sendBudgetExceededNotification()
                    hasExceededBudgetNotified = true // Prevent multiple notifications
                }
            }
        } else {
            budgetProgressBar.progress = 0
            remainingBudgetText.text = "No budget set"
            remainingBudgetText.setTextColor(resources.getColor(R.color.white))
        }
    }
}