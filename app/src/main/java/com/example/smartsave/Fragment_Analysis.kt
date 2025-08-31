package com.example.smartsave

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.smartsave.data.TransactionDataManager
import com.example.smartsave.data.Utils
import com.example.smartsave.model.TransactionType
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat

class Fragment_Analysis : Fragment() {

    private lateinit var dataManager: TransactionDataManager
    private lateinit var pieChart: PieChart
    private lateinit var barChart: HorizontalBarChart
    private lateinit var sharedPreferences: SharedPreferences

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "currencyType") {
            setupPieChart()
            setupBarChart()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment__analysis, container, false)

        dataManager = TransactionDataManager(requireContext())
        sharedPreferences = requireContext().getSharedPreferences("SmartSavePrefs", Context.MODE_PRIVATE)
        pieChart = view.findViewById(R.id.pieChart)
        barChart = view.findViewById(R.id.barChart)

        setupPieChart()
        setupBarChart()

        return view
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        setupPieChart()
        setupBarChart()
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    private fun setupPieChart() {
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

        // Prepare pie chart entries
        val entries = mutableListOf<PieEntry>()
        if (income > 0) entries.add(PieEntry(income.toFloat(), "Income"))
        if (expenses > 0) entries.add(PieEntry(expenses.toFloat(), "Expenses"))
        if (savings > 0) entries.add(PieEntry(savings.toFloat(), "Savings"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#34C759"), // Green for Income
            Color.parseColor("#EF4444"), // Red for Expenses
            Color.parseColor("#3B82F6")  // Blue for Savings
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return Utils.formatAmount(requireContext(), value.toDouble())
            }
        })
        pieChart.data = data

        // Customize pie chart
        pieChart.setUsePercentValues(false) // Show raw values with currency
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = true
        pieChart.legend.textColor = Color.WHITE
        pieChart.legend.textSize = 12f
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(12f)

        // Enable center hole and set dark color
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.parseColor("#1F2A44")) // Dark color for the center
        pieChart.setHoleRadius(40f) // Adjust the size of the hole (40% of the chart radius)

        pieChart.invalidate()
    }

    private fun setupBarChart() {
        val transactions = dataManager.readAllTransactions()
        val categoryMap = mutableMapOf<String, Double>()

        // Calculate total expenses per category
        var totalExpenses = 0.0
        transactions.forEach { transaction ->
            if (transaction.type == TransactionType.EXPENSE) {
                totalExpenses += transaction.amount
                categoryMap[transaction.category] =
                    (categoryMap[transaction.category] ?: 0.0) + transaction.amount
            }
        }

        // Define all possible categories
        val allCategories = listOf("Food", "Health", "Utility", "Shopping", "Transport", "Entertainment")
        val categoryColors = mapOf(
            "Food" to Color.parseColor("#34C759"),        // Green
            "Health" to Color.parseColor("#3B82F6"),      // Blue
            "Utility" to Color.parseColor("#A855F7"),     // Purple
            "Shopping" to Color.parseColor("#FF6F61"),    // Coral
            "Transport" to Color.parseColor("#FACC15"),   // Yellow
            "Entertainment" to Color.parseColor("#EF4444") // Red
        )

        // Prepare bar chart entries for all categories (even those with zero expenses)
        val entries = mutableListOf<BarEntry>()
        val colors = mutableListOf<Int>()
        allCategories.forEachIndexed { index, category ->
            val amount = categoryMap[category] ?: 0.0
            val percentage = if (totalExpenses > 0) (amount / totalExpenses * 100).toFloat() else 0f
            entries.add(BarEntry(index.toFloat(), amount.toFloat(), percentage))
            colors.add(categoryColors[category] ?: Color.parseColor("#3B82F6")) // Default to blue
        }

        val dataSet = BarDataSet(entries, "Categories")
        dataSet.colors = colors
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f
        dataSet.setDrawValues(true)

        // Custom value formatter to show amount with currency and percentage (e.g., "$1,200.00 74%")
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getBarLabel(entry: BarEntry?): String {
                val amount = entry?.y ?: 0f
                val percentage = entry?.data as? Float ?: 0f
                val formattedAmount = Utils.formatAmount(requireContext(), amount.toDouble())
                return String.format("%s %.0f%%", formattedAmount, percentage)
            }
        }

        val data = BarData(dataSet)
        data.barWidth = 0.7f
        barChart.data = data

        // Customize Y-axis (amount) to show only the numeric value without currency
        barChart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val formatter = DecimalFormat("#,##0.00")
                return formatter.format(value.toDouble())
            }
        }

        // Customize bar chart
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false

        // X-axis (categories)
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.WHITE
        xAxis.textSize = 12f
        xAxis.labelCount = allCategories.size
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return allCategories.getOrNull(value.toInt()) ?: ""
            }
        }

        // Y-axis (amount)
        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisLeft.textSize = 12f
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false

        barChart.setFitBars(true)
        barChart.setExtraBottomOffset(10f)
        barChart.invalidate()
    }
}