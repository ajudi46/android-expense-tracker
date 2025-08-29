package com.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.model.Transaction
import com.expensetracker.data.model.TransactionType
import com.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    val allTransactions = repository.getAllTransactions()
    val recentTransactions = repository.getRecentTransactions()

    fun getTransactionsForMonth(month: Int, year: String) = 
        repository.getTransactionsForMonth(month, year)

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        description: String,
        category: String,
        fromAccountId: Long,
        toAccountId: Long? = null,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                type = type,
                amount = amount,
                description = description,
                category = category,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                createdAt = date
            )
            repository.insertTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateUiState(newState: TransactionUiState) {
        _uiState.value = newState
    }
}

data class TransactionUiState(
    val selectedTransactionType: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val description: String = "",
    val selectedCategory: String = "",
    val selectedFromAccount: Long? = null,
    val selectedToAccount: Long? = null,
    val showAddTransactionDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
