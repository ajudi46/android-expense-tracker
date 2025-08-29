package com.expensetracker.ui.viewmodel

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.expensetracker.auth.AuthenticationRepository
import com.expensetracker.cloud.CloudSyncRepository
import com.expensetracker.data.model.UserProfile
import com.expensetracker.data.preference.UserPreferenceManager
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
    val isSyncing: Boolean = false,
    val hasSeenLogin: Boolean = false,
    val hasSkippedLogin: Boolean = false,
    val shouldShowLogin: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthenticationRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val expenseRepository: ExpenseRepository,
    private val userPreferenceManager: UserPreferenceManager
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
                authRepository.currentUser,
                userPreferenceManager.hasSeenLogin,
                userPreferenceManager.hasSkippedLogin
            ) { isSignedIn, user, hasSeenLogin, hasSkippedLogin ->
                val shouldShowLogin = !isSignedIn && !hasSeenLogin && !hasSkippedLogin
                _uiState.value = _uiState.value.copy(
                    isSignedIn = isSignedIn,
                    user = user,
                    userEmail = user?.email,
                    userPhotoUrl = user?.photoUrl,
                    hasSeenLogin = hasSeenLogin,
                    hasSkippedLogin = hasSkippedLogin,
                    shouldShowLogin = shouldShowLogin
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
            
            // Log device information for debugging
            logDeviceInfo()
            
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                
                val signInResult = authRepository.signInWithGoogle(account)
                
                if (signInResult.isSuccess) {
                    // Mark that user has seen login and signed in
                    userPreferenceManager.setHasSeenLogin(true)
                    userPreferenceManager.setHasSkippedLogin(false)
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
                        7 -> "Network Error: Please check your internet connection and try again. If using mobile data, try switching to WiFi."
                        10 -> "Configuration Error: SHA-1 fingerprint mismatch or Firebase not properly configured. Please check FIREBASE_SETUP.md for instructions."
                        12501 -> "Sign in was cancelled"
                        12502 -> "Sign in is in progress"
                        12500 -> "Google Play Services not available or outdated. Please update Google Play Services."
                        else -> "Sign in failed (Error ${e.statusCode}): ${e.message}. If this persists, try clearing app data or reinstalling."
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
    
    fun skipLogin() {
        userPreferenceManager.setHasSeenLogin(true)
        userPreferenceManager.setHasSkippedLogin(true)
    }
    
    fun forceShowLogin() {
        // Used when user logs out - they should see login again
        userPreferenceManager.setHasSeenLogin(false)
        userPreferenceManager.setHasSkippedLogin(false)
    }
    
    private fun logDeviceInfo() {
        Log.d("ExpenseTracker", "Device Info - Model: ${Build.MODEL}, SDK: ${Build.VERSION.SDK_INT}, Brand: ${Build.BRAND}")
        Log.d("ExpenseTracker", "Manufacturer: ${Build.MANUFACTURER}, Product: ${Build.PRODUCT}")
    }
}
