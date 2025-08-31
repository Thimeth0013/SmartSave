package com.example.smartsave.data

import android.content.Context
import java.text.DecimalFormat

object Utils {
    fun formatAmount(context: Context, amount: Double): String {
        val sharedPreferences = context.getSharedPreferences("SmartSavePrefs", Context.MODE_PRIVATE)
        val currencyType = sharedPreferences.getString("currencyType", "USD ($)") ?: "USD ($)"
        // Extract the symbol from the currency type (e.g., "USD ($)" → "$")
        val currencySymbol = currencyType.substringAfter("(").substringBefore(")")

        val formatter = DecimalFormat("#,##0.00")
        return "$currencySymbol${formatter.format(amount)}"
    }
}