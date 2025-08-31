package com.example.smartsave

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartsave.data.TransactionDataManager
import com.example.smartsave.model.Transaction
import com.example.smartsave.model.TransactionType
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import java.util.Date

class Home_Fragment : Fragment() {
    private lateinit var dataManager: TransactionDataManager
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var currentExpensesText: TextView
    private lateinit var remainingBudgetText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = TransactionDataManager(requireContext())

        // Initialize Budget Status views
        budgetProgressBar = view.findViewById(R.id.budgetProgressBar)
        currentExpensesText = view.findViewById(R.id.currentExpensesText)
        remainingBudgetText = view.findViewById(R.id.remainingBudgetText)

        // Set up summary (income, expenses, savings)
        updateSummary(view)

        // Set up RecyclerView for recent transactions (last 5)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recentTransactionsRv)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val transactions = dataManager.readAllTransactions().toMutableList()
        val recentTransactions = transactions.takeLast(5).toMutableList() // Last 5 transactions
        transactionAdapter = TransactionAdapter(
            recentTransactions,
            onEditClick = { transaction ->
                val intent = Intent(activity, EditTransaction::class.java).apply {
                    putExtra("TRANSACTION", transaction)
                }
                startActivity(intent)
            },
            onDeleteClick = { transaction ->
                dataManager.deleteTransaction(transaction.id)
                refreshTransactionList(view)
            }
        )
        recyclerView.adapter = transactionAdapter

        // Set up the Add Transaction button click listener
        val addTransaction = view.findViewById<Button>(R.id.addBtn)
        addTransaction.setOnClickListener {
            val intent = Intent(requireContext(), AddTransaction::class.java)
            startActivity(intent)
        }

        // Set up View Reports button (navigates to Analysis_Fragment via BottomNavigationView)
        val viewReports = view.findViewById<Button>(R.id.toAnalysisBtn)
        viewReports.setOnClickListener {
            val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            // Programmatically select the Analysis_Fragment menu item
            bottomNavigationView?.selectedItemId = R.id.analysis
        }

        // Set up Budget button to navigate to Budget activity
        val budgetButton = view.findViewById<Button>(R.id.budgetBtn)
        budgetButton.setOnClickListener {
            val intent = Intent(requireContext(), Budget::class.java)
            startActivity(intent)
        }

        // Set up See All button to navigate to History_Fragment
        val seeAllButton = view.findViewById<Button>(R.id.toSeeAllBtn)
        seeAllButton.setOnClickListener {
            // Find the BottomNavigationView in the parent activity (Home)
            val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            // Programmatically select the History_Fragment menu item
            bottomNavigationView?.selectedItemId = R.id.history
        }

        // Update Budget Status
        updateBudgetStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshTransactionList(requireView())
    }

    private fun updateSummary(view: View) {
        val transactions = dataManager.readAllTransactions()
        var income = 0.0
        var expenses = 0.0
        transactions.forEach { transaction ->
            if (transaction.type == TransactionType.INCOME) {
                income += transaction.amount
            } else {
                expenses += transaction.amount
            }
        }
        val savings = income - expenses

        // Use the Transaction's getFormattedAmount() to format amounts with the selected currency
        view.findViewById<TextView>(R.id.textView10).text = transactionWithAmount(income).getFormattedAmount(requireContext())
        view.findViewById<TextView>(R.id.textView11).text = transactionWithAmount(expenses).getFormattedAmount(requireContext())
        view.findViewById<TextView>(R.id.textView12).text = transactionWithAmount(savings).getFormattedAmount(requireContext())
    }

    private fun refreshTransactionList(view: View) {
        val transactions = dataManager.readAllTransactions()
        val recentTransactions = transactions.takeLast(5).toMutableList() // Last 5 transactions
        transactionAdapter.updateTransactions(recentTransactions)
        updateSummary(view)
        updateBudgetStatus()
    }

    private fun updateBudgetStatus() {
        val budget = getSavedBudget()
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

        // Update UI with formatted amounts using getFormattedAmount()
        currentExpensesText.text = "Current Expenses: ${transactionWithAmount(currentExpenses).getFormattedAmount(requireContext())}"

        if (budget > 0) {
            val remainingBudget = budget - currentExpenses
            val progress = if (budget > 0) ((currentExpenses / budget) * 100).coerceAtMost(100.0).toInt() else 0
            budgetProgressBar.progress = progress
            remainingBudgetText.text = "Remaining Budget: ${transactionWithAmount(remainingBudget).getFormattedAmount(requireContext())}"
            remainingBudgetText.setTextColor(
                if (remainingBudget >= 0) resources.getColor(R.color.green)
                else resources.getColor(R.color.red)
            )
        } else {
            budgetProgressBar.progress = 0
            remainingBudgetText.text = "No budget set"
            remainingBudgetText.setTextColor(resources.getColor(R.color.white))
        }
    }

    private fun getSavedBudget(): Double {
        val sharedPreferences = requireContext().getSharedPreferences("SmartSavePrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getFloat("monthlyBudget", 0f).toDouble()
    }

    // Helper function to create a dummy Transaction for formatting purposes
    private fun transactionWithAmount(amount: Double): Transaction {
        return Transaction(
            id = "",
            title = "",
            amount = amount,
            category = "",
            type = TransactionType.EXPENSE,
            date = Date()
        )
    }
}