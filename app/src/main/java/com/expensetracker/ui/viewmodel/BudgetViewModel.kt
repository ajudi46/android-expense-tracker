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

    fun addBudget(category: String, limitAmount: Double) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val budget = Budget(
                category = category,
                limitAmount = limitAmount,
                currentSpent = 0.0,
                month = calendar.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-based
                year = calendar.get(Calendar.YEAR)
            )
            repository.insertBudget(budget)
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
