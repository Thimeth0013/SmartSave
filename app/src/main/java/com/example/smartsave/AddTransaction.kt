package com.example.smartsave

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartsave.data.TransactionDataManager
import com.example.smartsave.model.Transaction
import com.example.smartsave.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddTransaction : AppCompatActivity() {
    private lateinit var dataManager: TransactionDataManager
    private lateinit var titleEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var typeRadioGroup: RadioGroup
    private lateinit var incomeRadioButton: RadioButton
    private lateinit var expenseRadioButton: RadioButton
    private lateinit var categorySpinner: Spinner
    private lateinit var dateEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private var selectedDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_transaction)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dataManager = TransactionDataManager(this)

        // Initialize views
        titleEditText = findViewById(R.id.inputTitle)
        amountEditText = findViewById(R.id.inputAmount)
        typeRadioGroup = findViewById(R.id.transactionTypeGroup)
        incomeRadioButton = findViewById(R.id.incomeR)
        expenseRadioButton = findViewById(R.id.expenseR)
        categorySpinner = findViewById(R.id.inputCategory)
        dateEditText = findViewById(R.id.inputDate)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Set up category spinner
        val categoryAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.category_list,
            android.R.layout.simple_spinner_item
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        // Set up date picker
        dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            selectedDate?.let { calendar.time = it }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                    selectedDate = selectedCalendar.time
                    dateEditText.setText(
                        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(selectedDate!!)
                    )
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        // Set up save button
        saveButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val type = if (incomeRadioButton.isChecked) TransactionType.INCOME else TransactionType.EXPENSE
            val category = categorySpinner.selectedItem.toString()
            val date = selectedDate ?: Date()

            if (title.isNotEmpty() && amount > 0) {
                val newTransaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    date = date
                )
                dataManager.createTransaction(newTransaction)
                finish()
            }
        }

        // Set up cancel button
        cancelButton.setOnClickListener {
            finish()
        }
    }
}