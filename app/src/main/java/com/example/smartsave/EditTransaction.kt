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

class EditTransaction : AppCompatActivity() {
    private lateinit var dataManager: TransactionDataManager
    private lateinit var titleEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var typeRadioGroup: RadioGroup
    private lateinit var incomeRadioButton: RadioButton
    private lateinit var expenseRadioButton: RadioButton
    private lateinit var categorySpinner: Spinner
    private lateinit var dateEditText: EditText
    private lateinit var updateButton: Button
    private lateinit var cancelButton: Button
    private var selectedDate: Date? = null
    private var transactionToEdit: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_transaction)

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
        updateButton = findViewById(R.id.updateButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Set up category spinner
        val categoryAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.category_list,
            android.R.layout.simple_spinner_item
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        // Retrieve the transaction to edit
        transactionToEdit = intent.getParcelableExtra("TRANSACTION")
        transactionToEdit?.let { transaction ->
            // Prefill the form
            titleEditText.setText(transaction.title)
            amountEditText.setText(transaction.amount.toString())
            if (transaction.type == TransactionType.INCOME) {
                incomeRadioButton.isChecked = true
            } else {
                expenseRadioButton.isChecked = true
            }
            val categories = resources.getStringArray(R.array.category_list)
            categorySpinner.setSelection(categories.indexOf(transaction.category))
            selectedDate = transaction.date
            dateEditText.setText(
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(transaction.date)
            )
        }

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

        // Set up update button
        updateButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val type = if (incomeRadioButton.isChecked) TransactionType.INCOME else TransactionType.EXPENSE
            val category = categorySpinner.selectedItem.toString()
            val date = selectedDate ?: transactionToEdit?.date ?: Date()

            if (title.isNotEmpty() && amount > 0) {
                val updatedTransaction = Transaction(
                    id = transactionToEdit!!.id,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    date = date
                )
                dataManager.updateTransaction(updatedTransaction)
                finish()
            }
        }

        // Set up cancel button
        cancelButton.setOnClickListener {
            finish()
        }
    }
}