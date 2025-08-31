package com.example.smartsave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartsave.model.Transaction
import com.example.smartsave.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private var transactions: MutableList<Transaction>,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.transactionTitle)
        val amountTextView: TextView = itemView.findViewById(R.id.transactionAmount)
        val dateTextView: TextView = itemView.findViewById(R.id.transactionDate)
        val categoryTextView: TextView = itemView.findViewById(R.id.transactionCategory)
        val typeTextView: TextView = itemView.findViewById(R.id.transactionType)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.titleTextView.text = transaction.title
        // Use getFormattedAmount() to include the selected currency symbol
        holder.amountTextView.text = transaction.getFormattedAmount(holder.itemView.context)
        holder.dateTextView.text = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(transaction.date)
        holder.categoryTextView.text = transaction.category
        holder.typeTextView.text = transaction.type.toString()

        // Set amount color based on transaction type
        val amountColor = if (transaction.type == TransactionType.INCOME) {
            ContextCompat.getColor(holder.itemView.context, R.color.green)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.red)
        }
        holder.amountTextView.setTextColor(amountColor)

        holder.editButton.setOnClickListener { onEditClick(transaction) }
        holder.deleteButton.setOnClickListener {
            // Show confirmation dialog before deleting
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this transaction: ${transaction.title}?")
                .setPositiveButton("Yes") { _, _ ->
                    onDeleteClick(transaction)
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        notifyDataSetChanged()
    }
}