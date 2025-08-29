package com.expensetracker.ui.viewmodel

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.expensetracker.auth.AuthenticationRepository
import com.expensetracker.cloud.CloudSyncRepository
import com.expensetracker.data.model.UserProfile
import com.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val user: UserProfile? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val errorMessage: String? = null,
    val isSyncing: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthenticationRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        observeAuthState()
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            combine(
                authRepository.isSignedIn,
                authRepository.currentUser
            ) { isSignedIn, user ->
                _uiState.value = _uiState.value.copy(
                    isSignedIn = isSignedIn,
                    user = user,
                    userEmail = user?.email,
                    userPhotoUrl = user?.photoUrl
                )
            }.collect()
        }
    }
    
    fun getGoogleSignInIntent(): Intent {
        return authRepository.getGoogleSignInClient().signInIntent
    }
    
    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                
                val signInResult = authRepository.signInWithGoogle(account)
                
                if (signInResult.isSuccess) {
                    // Start background sync after successful sign in
                    performInitialSync()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = signInResult.exceptionOrNull()?.message ?: "Sign in failed"
                    )
                }
            } catch (e: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = when (e.statusCode) {
                        10 -> "Configuration Error: Please set up Firebase properly. Check FIREBASE_SETUP.md for instructions."
                        12501 -> "Sign in was cancelled"
                        12502 -> "Sign in is in progress"
                        else -> "Sign in failed (Error ${e.statusCode}): ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = authRepository.signOut()
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (result.isFailure) {
                    result.exceptionOrNull()?.message ?: "Sign out failed"
                } else null
            )
        }
    }
    
    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = authRepository.deleteAccount()
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (result.isFailure) {
                    result.exceptionOrNull()?.message ?: "Account deletion failed"
                } else null
            )
        }
    }
    
    fun performInitialSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, isLoading = false)
            
            try {
                // Get all local data
                val localAccounts = expenseRepository.getAllAccounts().firstOrNull() ?: emptyList()
                val localTransactions = expenseRepository.getAllTransactions().firstOrNull() ?: emptyList()
                val localCategories = emptyList<com.expensetracker.data.model.Category>() // Categories not implemented yet
                val localBudgets = emptyList<com.expensetracker.data.model.Budget>() // Budgets not implemented yet
                
                // First, sync local data TO cloud (backup)
                val syncResult = cloudSyncRepository.performFullSync(
                    localAccounts = localAccounts,
                    localTransactions = localTransactions,
                    localCategories = localCategories,
                    localBudgets = localBudgets
                )
                
                // Then, try to restore any additional data from cloud
                cloudSyncRepository.syncAccountsFromCloud()
                cloudSyncRepository.syncTransactionsFromCloud()
                cloudSyncRepository.syncCategoriesFromCloud()
                cloudSyncRepository.syncBudgetsFromCloud()
                
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = if (syncResult.isFailure) {
                        "Sync completed with some issues: ${syncResult.exceptionOrNull()?.message}"
                    } else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = "Sync failed: ${e.message}"
                )
            }
        }
    }
    
    fun syncData() {
        performInitialSync()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
