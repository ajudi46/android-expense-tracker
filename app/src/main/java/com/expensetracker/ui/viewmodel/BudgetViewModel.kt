package com.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.model.Budget
import com.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    val currentMonthBudgets = repository.getCurrentMonthBudgets()
    val allBudgets = repository.getAllBudgets()

    fun getBudgetsForMonth(month: Int, year: Int) = repository.getBudgetsForMonth(month, year)

    fun addBudget(category: String, limitAmount: Double) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
            val year = calendar.get(Calendar.YEAR)
            
            val budget = Budget(
                category = category,
                limitAmount = limitAmount,
                currentSpent = 0.0,
                month = month,
                year = year
            )
            repository.insertBudget(budget)
            
            // Automatically calculate spending from existing transactions
            repository.recalculateBudgetFromTransactions(category, month, year)
        }
    }

    fun addBudgetForMonth(category: String, limitAmount: Double, month: Int, year: Int, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val budget = Budget(
                    category = category,
                    limitAmount = limitAmount,
                    currentSpent = 0.0,
                    month = month,
                    year = year
                )
                repository.insertBudget(budget)
                
                // Automatically calculate spending from existing transactions
                repository.recalculateBudgetFromTransactions(category, month, year)
                
                // Notify completion
                onComplete?.invoke()
            } catch (e: Exception) {
                println("🚨 ERROR adding budget: ${e.message}")
                onComplete?.invoke()
            }
        }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            repository.updateBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }



    fun updateBudgetSpent(category: String, newAmount: Double) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            repository.updateBudgetSpent(
                category = category,
                month = calendar.get(Calendar.MONTH) + 1,
                year = calendar.get(Calendar.YEAR),
                newAmount = newAmount
            )
        }
    }
}
