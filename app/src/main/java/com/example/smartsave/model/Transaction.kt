package com.example.smartsave.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.example.smartsave.data.Utils
import java.util.Date
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val type: TransactionType,
    val date: Date
) : Parcelable {
    companion object {
        const val FOOD = "Food"
        const val BILLS = "Bills"
        const val HEALTH = "Health"
        const val UTILITY = "Utility"
        const val SHOPPING = "Shopping"
        const val TRANSPORT = "Transport"
        const val ENTERTAINMENT = "Entertainment"
        const val SALARY = "Salary"
        const val BONUS = "Bonus"

        // List of expense categories for filtering
        val EXPENSE_CATEGORIES = listOf(
            FOOD,
            BILLS,
            HEALTH,
            UTILITY,
            SHOPPING,
            TRANSPORT,
            ENTERTAINMENT
        )

        @JvmField
        val CREATOR = object : Parcelable.Creator<Transaction> {
            override fun createFromParcel(parcel: Parcel): Transaction {
                return Transaction(
                    id = parcel.readString() ?: UUID.randomUUID().toString(),
                    title = parcel.readString() ?: "",
                    amount = parcel.readDouble(),
                    category = parcel.readString() ?: FOOD, // Default to FOOD if null
                    type = TransactionType.valueOf(parcel.readString() ?: TransactionType.EXPENSE.name),
                    date = Date(parcel.readLong())
                )
            }

            override fun newArray(size: Int): Array<Transaction?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeDouble(amount)
        parcel.writeString(category)
        parcel.writeString(type.name)
        parcel.writeLong(date.time)
    }

    override fun describeContents(): Int {
        return 0
    }

    // Function to get the formatted amount with the selected currency symbol
    fun getFormattedAmount(context: Context): String {
        return Utils.formatAmount(context, amount)
    }
}

enum class TransactionType {
    INCOME, EXPENSE
}