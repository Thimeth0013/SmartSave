package com.example.smartsave

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartsave.data.TransactionDataManager
import com.example.smartsave.model.Transaction
import com.example.smartsave.model.TransactionType
import com.example.smartsave.model.Transaction.Companion.EXPENSE_CATEGORIES

class History_Fragment : Fragment() {

    private lateinit var dataManager: TransactionDataManager
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var filterSpinner: Spinner
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var rootView: View
    private var allTransactions: MutableList<Transaction> = mutableListOf()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "currencyType") {
            refreshTransactionList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history_, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view
        dataManager = TransactionDataManager(requireContext())
        sharedPreferences = requireContext().getSharedPreferences("SmartSavePrefs", Context.MODE_PRIVATE)

        // Set up RecyclerView for all transactions
        recyclerView = view.findViewById(R.id.transactionRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        allTransactions = dataManager.readAllTransactions().toMutableList()
        transactionAdapter = TransactionAdapter(
            allTransactions,
            onEditClick = { transaction ->
                val intent = Intent(activity, EditTransaction::class.java).apply {
                    putExtra("TRANSACTION", transaction)
                }
                startActivity(intent)
            },
            onDeleteClick = { transaction ->
                dataManager.deleteTransaction(transaction.id)
                refreshTransactionList()
            }
        )
        recyclerView.adapter = transactionAdapter

        // Set up filter spinner
        filterSpinner = view.findViewById(R.id.filterSpinner)
        setupFilterSpinner()
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        refreshTransactionList()
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    private fun setupFilterSpinner() {
        // Base filter options from strings.xml
        val baseOptions = resources.getStringArray(R.array.filter_options).toMutableList()
        // Add category-specific expense filters (e.g., "Expenses: Food")
        val categoryOptions = EXPENSE_CATEGORIES.map { "Expenses: $it" }
        val filterOptions = baseOptions + categoryOptions

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = adapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedFilter = parent.getItemAtPosition(position) as String
                applyFilter(selectedFilter)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                applyFilter("All Transactions")
            }
        }
    }

    private fun applyFilter(filter: String) {
        val filteredTransactions = when {
            filter == "All Transactions" -> allTransactions.toMutableList()
            filter == "Income Only" -> allTransactions.filter { it.type == TransactionType.INCOME }.toMutableList()
            filter == "Expenses Only" -> allTransactions.filter { it.type == TransactionType.EXPENSE }.toMutableList()
            filter.startsWith("Expenses: ") -> {
                val category = filter.substringAfter("Expenses: ")
                allTransactions.filter { it.type == TransactionType.EXPENSE && it.category == category }.toMutableList()
            }
            else -> allTransactions.toMutableList()
        }
        transactionAdapter.updateTransactions(filteredTransactions)
    }

    private fun refreshTransactionList() {
        allTransactions = dataManager.readAllTransactions().toMutableList()
        val selectedFilter = filterSpinner.selectedItem?.toString() ?: "All Transactions"
        applyFilter(selectedFilter)
    }
}