package com.example.smartsave

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smartsave.data.TransactionDataManager
import com.example.smartsave.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Fragment_Settings : Fragment() {

    private lateinit var currencySpinner: Spinner
    private lateinit var notificationsSwitch: Switch
    private lateinit var expenseReminderSwitch: Switch
    private lateinit var expenseTimePickerLayout: LinearLayout
    private lateinit var btnSetExpenseReminderTime: Button
    private lateinit var tvExpenseReminderTime: TextView
    private lateinit var pinSwitch: Switch
    private lateinit var pinInputLayout: LinearLayout
    private lateinit var pinInput: EditText
    private lateinit var btnSavePin: Button
    private lateinit var importBtn: Button
    private lateinit var exportBtn: Button
    private lateinit var logoutBtn: ImageButton
    private lateinit var dataManager: TransactionDataManager
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    private val TAG = "Settings_Fragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d(TAG, "POST_NOTIFICATIONS permission granted")
                // Re-check budget thresholds or reschedule expense alarm if needed
                if (notificationsSwitch.isChecked) {
                    checkBudgetThresholds()
                }
                if (expenseReminderSwitch.isChecked && tvExpenseReminderTime.text != "Not set") {
                    val time = tvExpenseReminderTime.text.toString()
                    val parts = time.split(":").map { it.trim().replace("[^0-9]".toRegex(), "") }
                    if (parts.size >= 2) {
                        scheduleExpenseAlarm(parts[0].toInt(), parts[1].toInt())
                    }
                }
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission denied")
                Toast.makeText(requireContext(), "Notifications disabled due to missing permission", Toast.LENGTH_LONG).show()
                // Disable notification switches
                notificationsSwitch.isChecked = false
                expenseReminderSwitch.isChecked = false
                with(sharedPreferences.edit()) {
                    putBoolean("notificationsEnabled", false)
                    putBoolean("expenseReminderEnabled", false)
                    apply()
                }
                cancelExpenseAlarm()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment__settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("SmartSavePrefs", Context.MODE_PRIVATE)

        // Initialize views and data manager
        currencySpinner = view.findViewById(R.id.currencySpinner)
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch)
        expenseReminderSwitch = view.findViewById(R.id.expenseReminderSwitch)
        expenseTimePickerLayout = view.findViewById(R.id.expenseTimePickerLayout)
        btnSetExpenseReminderTime = view.findViewById(R.id.btnSetExpenseReminderTime)
        tvExpenseReminderTime = view.findViewById(R.id.tvExpenseReminderTime)
        pinSwitch = view.findViewById(R.id.pinSwitch)
        pinInputLayout = view.findViewById(R.id.pinInputLayout)
        pinInput = view.findViewById(R.id.pinInput)
        btnSavePin = view.findViewById(R.id.btnSavePin)
        importBtn = view.findViewById(R.id.importBtn)
        exportBtn = view.findViewById(R.id.exportBtn)
        dataManager = TransactionDataManager(requireContext())

        // Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Set up the Spinner with currency options
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.currencies,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter

        // Load and set currency selection
        val savedCurrency = sharedPreferences.getString("currencyType", "USD ($)")
        val savedPosition = adapter.getPosition(savedCurrency)
        currencySpinner.setSelection(savedPosition)

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCurrency = parent.getItemAtPosition(position) as String
                Log.d(TAG, "Selected currency: $selectedCurrency")
                with(sharedPreferences.edit()) {
                    putString("currencyType", selectedCurrency)
                    apply()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed
            }
        }

        // Load and set up budget notifications
        val notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", false)
        notificationsSwitch.isChecked = notificationsEnabled
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "notificationsEnabled: $isChecked")
            with(sharedPreferences.edit()) {
                putBoolean("notificationsEnabled", isChecked)
                apply()
            }
            if (isChecked && hasNotificationPermission()) {
                checkBudgetThresholds()
            }
        }

        // Load and set up expense reminders
        val expenseReminderEnabled = sharedPreferences.getBoolean("expenseReminderEnabled", false)
        expenseReminderSwitch.isChecked = expenseReminderEnabled
        expenseTimePickerLayout.visibility = if (expenseReminderEnabled) View.VISIBLE else View.GONE
        val savedExpenseReminderTime = sharedPreferences.getString("expenseReminderTime", "Not set")
        tvExpenseReminderTime.text = savedExpenseReminderTime

        expenseReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "expenseReminderEnabled: $isChecked")
            with(sharedPreferences.edit()) {
                putBoolean("expenseReminderEnabled", isChecked)
                apply()
            }
            expenseTimePickerLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked && tvExpenseReminderTime.text == "Not set") {
                showTimePickerDialog()
            } else if (!isChecked) {
                cancelExpenseAlarm()
            }
        }

        btnSetExpenseReminderTime.setOnClickListener {
            showTimePickerDialog()
        }

        // Load and set up PIN lock
        val pinEnabled = sharedPreferences.getBoolean("is_pin_set", false)
        pinSwitch.isChecked = pinEnabled
        pinInputLayout.visibility = if (pinEnabled) View.VISIBLE else View.GONE

        pinSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "pinEnabled: $isChecked")
            pinInputLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                with(sharedPreferences.edit()) {
                    remove("app_pin")
                    putBoolean("is_pin_set", false)
                    apply()
                }
                Toast.makeText(requireContext(), "PIN lock disabled", Toast.LENGTH_SHORT).show()
            }
        }

        btnSavePin.setOnClickListener {
            val pin = pinInput.text.toString().trim()
            if (pin.length != 4 || !pin.all { it.isDigit() }) {
                Toast.makeText(requireContext(), "Please enter a 4-digit PIN", Toast.LENGTH_SHORT).show()
                pinInput.text.clear()
                return@setOnClickListener
            }
            with(sharedPreferences.edit()) {
                putString("app_pin", pin)
                putBoolean("is_pin_set", true)
                apply()
            }
            pinInput.text.clear()
            Toast.makeText(requireContext(), "PIN saved successfully", Toast.LENGTH_SHORT).show()
        }

        // Set up export button
        exportBtn.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Confirm Export")
                .setMessage("Are you sure you want to export your transaction data? This will create a backup file in internal storage.")
                .setPositiveButton("Yes") { _, _ ->
                    exportData()
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Set up import button
        importBtn.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Confirm Import")
                .setMessage("Are you sure you want to import transaction data? This will overwrite your current data.")
                .setPositiveButton("Yes") { _, _ ->
                    importData()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed below API 33
        }
    }

    private fun showTimePickerDialog() {
        if (!hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val is24HourFormat = android.text.format.DateFormat.is24HourFormat(requireContext())

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
            }
            val timeFormat = if (is24HourFormat) "HH:mm" else "hh:mm a"
            val time = SimpleDateFormat(timeFormat, Locale.getDefault()).format(calendar.time)
            with(sharedPreferences.edit()) {
                putString("expenseReminderTime", time)
                apply()
            }
            tvExpenseReminderTime.text = time
            scheduleExpenseAlarm(selectedHour, selectedMinute)
            Toast.makeText(requireContext(), "Expense reminder set for $time", Toast.LENGTH_SHORT).show()
        }, hour, minute, is24HourFormat).show()
    }

    private fun scheduleExpenseAlarm(hour: Int, minute: Int) {
        if (!hasNotificationPermission()) {
            Log.d(TAG, "Cannot schedule alarm: POST_NOTIFICATIONS permission missing")
            return
        }
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
            putExtra("reminderType", "expense")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        Log.d(TAG, "Scheduled expense reminder at $hour:$minute")
    }

    private fun cancelExpenseAlarm() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
            putExtra("reminderType", "expense")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled expense reminder")
    }

    private fun checkBudgetThresholds() {
        if (!hasNotificationPermission()) {
            Log.d(TAG, "Cannot check budget thresholds: POST_NOTIFICATIONS permission missing")
            return
        }
        val notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", false)
        if (!notificationsEnabled) return

        // Placeholder: Replace with actual budget data from Home or TransactionDataManager
        val budgets = listOf(
            Budget("Food", 800.0, 1000.0, "2025-05"),
            Budget("Transport", 450.0, 500.0, "2025-05")
        )

        budgets.forEach { budget ->
            val usagePercent = (budget.amount / budget.limit) * 100
            val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
                putExtra("reminderType", "budget")
                putExtra("category", budget.category)
                putExtra("usagePercent", usagePercent)
            }
            if (usagePercent >= 80 && usagePercent < 100) {
                requireContext().sendBroadcast(intent)
                Log.d(TAG, "Budget near limit for ${budget.category}: $usagePercent%")
            } else if (usagePercent >= 100) {
                requireContext().sendBroadcast(intent)
                Log.d(TAG, "Budget exceeded for ${budget.category}: $usagePercent%")
            }
        }
    }

    private fun exportData() {
        try {
            Log.d(TAG, "Starting exportData")
            val transactions = dataManager.readAllTransactions()
            if (transactions.isEmpty()) {
                Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show()
                return
            }

            val gson = Gson()
            val json = gson.toJson(transactions)
            val directory = File(requireContext().filesDir, "SmartSave")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, "backup.json")
            FileWriter(file).use { writer ->
                writer.write(json)
            }

            Toast.makeText(requireContext(), "Data exported to internal storage", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importData() {
        try {
            Log.d(TAG, "Starting importData")
            val directory = File(requireContext().filesDir, "SmartSave")
            val file = File(directory, "backup.json")
            if (!file.exists()) {
                Toast.makeText(requireContext(), "Backup file not found", Toast.LENGTH_SHORT).show()
                return
            }

            val gson = Gson()
            val type = object : TypeToken<List<Transaction>>() {}.type
            val transactions: List<Transaction> = FileReader(file).use { reader ->
                gson.fromJson(reader, type)
            }

            dataManager.clearAllTransactions()
            transactions.forEach { transaction ->
                dataManager.addTransaction(
                    Transaction(
                        id = transaction.id,
                        title = transaction.title,
                        amount = transaction.amount,
                        category = transaction.category,
                        type = transaction.type,
                        date = transaction.date
                    )
                )
            }

            Toast.makeText(requireContext(), "Data imported successfully", Toast.LENGTH_SHORT).show()
            checkBudgetThresholds()
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            Toast.makeText(requireContext(), "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Placeholder Budget model
    data class Budget(
        val category: String,
        val amount: Double, // Current spending
        val limit: Double, // Budget limit
        val month: String // e.g., "2025-05"
    )
}