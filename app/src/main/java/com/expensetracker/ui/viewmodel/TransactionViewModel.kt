package com.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.auth.AuthenticationRepository
import com.expensetracker.cloud.CloudSyncRepository
import com.expensetracker.data.model.Transaction
import com.expensetracker.data.model.TransactionType
import com.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val authRepository: AuthenticationRepository,
    private val cloudSyncRepository: CloudSyncRepository
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
            
            // Insert transaction locally
            val transactionId = repository.insertTransaction(transaction)
            
            // Auto-backup to cloud if user is signed in
            backupTransactionToCloud(transaction.copy(id = transactionId))
        }
    }
    
    private fun backupTransactionToCloud(transaction: Transaction) {
        viewModelScope.launch {
            try {
                val isSignedIn = authRepository.isSignedIn.firstOrNull() ?: false
                if (isSignedIn) {
                    cloudSyncRepository.syncTransactionsToCloud(listOf(transaction))
                }
            } catch (e: Exception) {
                // Handle backup error silently - transaction is still saved locally
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            // Auto-backup updated transaction to cloud
            backupTransactionToCloud(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            // First reverse the balance changes before deleting
            when (transaction.type) {
                TransactionType.EXPENSE -> {
                    // Add back the expense amount to the account (reverse the deduction)
                    repository.updateAccountBalance(transaction.fromAccountId, transaction.amount)
                }
                TransactionType.INCOME -> {
                    // Subtract the income amount from the account (reverse the addition)
                    repository.updateAccountBalance(transaction.fromAccountId, -transaction.amount)
                }
                TransactionType.TRANSFER -> {
                    transaction.toAccountId?.let { toAccountId ->
                        // Reverse transfer: add back to from account, subtract from to account
                        repository.updateAccountBalance(transaction.fromAccountId, transaction.amount)
                        repository.updateAccountBalance(toAccountId, -transaction.amount)
                    }
                }
            }
            
            // Then delete the transaction
            repository.deleteTransaction(transaction)
            
            // Note: For cloud sync, we could implement a "deleted" flag instead of actual deletion
            // to sync deletions across devices, but for simplicity, we'll just delete locally
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
