package com.expensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val category: String,
    val fromAccountId: Long,
    val toAccountId: Long? = null, // Only used for transfers
    val createdAt: Long = System.currentTimeMillis()
)

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}
