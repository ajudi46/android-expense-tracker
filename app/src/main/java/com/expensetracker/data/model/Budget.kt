package com.expensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val limitAmount: Double,
    val currentSpent: Double = 0.0,
    val month: Int, // Month (1-12)
    val year: Int,  // Year (e.g., 2024)
    val createdAt: Long = System.currentTimeMillis()
) {
    val remainingAmount: Double
        get() = limitAmount - currentSpent
    
    val percentageUsed: Float
        get() = if (limitAmount > 0) (currentSpent / limitAmount).toFloat() else 0f
    
    val isOverBudget: Boolean
        get() = currentSpent > limitAmount
}
