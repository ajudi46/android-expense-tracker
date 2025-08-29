package com.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.model.Account
import com.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    val accounts = repository.getAllAccounts()
    val totalBalance = repository.getTotalBalance()

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun addAccount(name: String, iconName: String, initialBalance: Double) {
        viewModelScope.launch {
            val account = Account(
                name = name,
                iconName = iconName,
                balance = initialBalance
            )
            repository.insertAccount(account)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun updateUiState(newState: AccountUiState) {
        _uiState.value = newState
    }
}

data class AccountUiState(
    val showAddAccountDialog: Boolean = false,
    val selectedAccount: Account? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
