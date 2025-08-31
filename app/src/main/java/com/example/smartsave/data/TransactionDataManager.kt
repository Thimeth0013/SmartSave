package com.example.smartsave.data

import android.content.Context
import com.example.smartsave.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TransactionDataManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("TransactionPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val transactionsKey = "TRANSACTIONS"

    fun createTransaction(transaction: Transaction) {
        val transactions = readAllTransactions().toMutableList()
        transactions.add(transaction)
        saveTransactions(transactions)
    }

    fun readAllTransactions(): List<Transaction> {
        val json = sharedPreferences.getString(transactionsKey, null) ?: return emptyList()
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun updateTransaction(updatedTransaction: Transaction) {
        val transactions = readAllTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == updatedTransaction.id }
        if (index != -1) {
            transactions[index] = updatedTransaction
            saveTransactions(transactions)
        }
    }

    fun deleteTransaction(transactionId: String) {
        val transactions = readAllTransactions().toMutableList()
        transactions.removeIf { it.id == transactionId }
        saveTransactions(transactions)
    }

    fun clearAllTransactions() {
        sharedPreferences.edit().remove(transactionsKey).apply()
    }

    fun addTransaction(transaction: Transaction) {
        createTransaction(transaction)
    }

    private fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString(transactionsKey, json).apply()
    }
}