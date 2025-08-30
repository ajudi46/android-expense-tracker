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
import com.expensetracker.data.model.Transaction
import com.expensetracker.data.preference.UserPreferenceManager
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.security.EncryptionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val user: UserProfile? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val errorMessage: String? = null,
    val isSyncing: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val hasSeenLogin: Boolean = false,
    val hasSkippedLogin: Boolean = false,
    val shouldShowLogin: Boolean = false,
    val lastBackupTime: Long? = null,
    val lastRestoreTime: Long? = null,
    val toastMessage: String? = null,
    val toastType: ToastType = ToastType.INFO
)

enum class ToastType {
    SUCCESS, ERROR, INFO, WARNING
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthenticationRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val expenseRepository: ExpenseRepository,
    private val userPreferenceManager: UserPreferenceManager,
    private val encryptionManager: EncryptionManager
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
                    
                    // Refresh encryption key to ensure consistency across devices
                    encryptionManager.refreshEncryptionKey()
                    Log.d("ExpenseTracker", "Encryption key refreshed for new sign-in")
                    
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
            showToast("Signing out and clearing local data...", ToastType.INFO)
            
            try {
                Log.d("ExpenseTracker", "Starting logout process...")
                
                // Clear all local data first
                expenseRepository.clearAllLocalData()
                Log.d("ExpenseTracker", "Local data cleared successfully")
                
                // Clear encryption key
                encryptionManager.clearEncryptionKey()
                Log.d("ExpenseTracker", "Encryption key cleared")
                
                // Clear user preferences
                userPreferenceManager.setHasSeenLogin(false)
                userPreferenceManager.setHasSkippedLogin(false)
                Log.d("ExpenseTracker", "User preferences cleared")
                
                // Sign out from Firebase
                val result = authRepository.signOut()
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    showToast("✅ Signed out successfully. All local data cleared.", ToastType.SUCCESS)
                    Log.d("ExpenseTracker", "Logout completed successfully")
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    showToast("❌ Sign out failed: ${result.exceptionOrNull()?.message}", ToastType.ERROR)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                showToast("❌ Logout failed: ${e.message}", ToastType.ERROR)
                Log.e("ExpenseTracker", "Logout failed: ${e.message}", e)
            }
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
                
                // Then, restore data from cloud and merge with local
                restoreAndMergeCloudData()
                
                // Recalculate balances after initial sync to ensure consistency
                Log.d("ExpenseTracker", "Recalculating balances after initial sync...")
                expenseRepository.recalculateAllBalances()
                Log.d("ExpenseTracker", "Balance recalculation after initial sync completed")
                
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
    
    fun forceFullSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            
            try {
                // Force upload all local data to cloud
                val localAccounts = expenseRepository.getAllAccounts().firstOrNull() ?: emptyList()
                val localTransactions = expenseRepository.getAllTransactions().firstOrNull() ?: emptyList()
                
                cloudSyncRepository.performFullSync(
                    localAccounts = localAccounts,
                    localTransactions = localTransactions
                )
                
                // Then download and merge cloud data
                restoreAndMergeCloudData()
                
                // Recalculate balances after force sync to ensure consistency
                Log.d("ExpenseTracker", "Recalculating balances after force sync...")
                expenseRepository.recalculateAllBalances()
                Log.d("ExpenseTracker", "Balance recalculation after force sync completed")
                
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = null
                )
                
                Log.d("ExpenseTracker", "Force sync completed successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = "Sync failed: ${e.message}"
                )
                Log.e("ExpenseTracker", "Force sync failed: ${e.message}")
            }
        }
    }
    
    fun backupDataToCloud() {
        if (!_uiState.value.isSignedIn) {
            showToast("Please sign in to backup data to cloud", ToastType.WARNING)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUp = true, errorMessage = null)
            showToast("Starting backup...", ToastType.INFO)
            
            try {
                Log.d("ExpenseTracker", "Starting backup operation...")
                val startTime = System.currentTimeMillis()
                
                // Get all local data
                Log.d("ExpenseTracker", "Fetching local data...")
                val localAccounts = expenseRepository.getAllAccounts().firstOrNull() ?: emptyList()
                val localTransactions = expenseRepository.getAllTransactions().firstOrNull() ?: emptyList()
                
                Log.d("ExpenseTracker", "Found ${localAccounts.size} accounts, ${localTransactions.size} transactions to backup")
                
                // Debug: Log account balances before backup
                localAccounts.forEach { account ->
                    Log.d("ExpenseTracker", "BACKUP - Account: ${account.name}, Balance: ${account.balance}")
                }
                
                if (localAccounts.isEmpty() && localTransactions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isBackingUp = false)
                    showToast("No data to backup. Add some accounts and transactions first.", ToastType.WARNING)
                    return@launch
                }
                
                // Upload to cloud with timeout (2 minutes)
                Log.d("ExpenseTracker", "Starting cloud upload...")
                val result = withTimeout(120_000) {
                    cloudSyncRepository.performFullSync(
                        localAccounts = localAccounts,
                        localTransactions = localTransactions
                    )
                }
                
                val endTime = System.currentTimeMillis()
                val duration = (endTime - startTime) / 1000.0
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isBackingUp = false,
                        lastBackupTime = System.currentTimeMillis(),
                        errorMessage = null
                    )
                    showToast("✅ Backup completed in ${String.format("%.1f", duration)}s! ${localAccounts.size} accounts, ${localTransactions.size} transactions", ToastType.SUCCESS)
                    Log.d("ExpenseTracker", "Backup completed successfully in ${duration}s - ${localAccounts.size} accounts, ${localTransactions.size} transactions")
                } else {
                    _uiState.value = _uiState.value.copy(isBackingUp = false)
                    showToast("❌ Backup failed: ${result.exceptionOrNull()?.message}", ToastType.ERROR)
                    Log.e("ExpenseTracker", "Backup failed after ${duration}s: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.value = _uiState.value.copy(isBackingUp = false)
                showToast("⏰ Backup timed out. Check your internet connection and try again.", ToastType.ERROR)
                Log.e("ExpenseTracker", "Backup timed out after 2 minutes")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isBackingUp = false)
                showToast("❌ Backup failed: ${e.message}", ToastType.ERROR)
                Log.e("ExpenseTracker", "Backup failed: ${e.message}", e)
            }
        }
    }
    
    fun restoreDataFromCloud() {
        if (!_uiState.value.isSignedIn) {
            showToast("Please sign in to restore data from cloud", ToastType.WARNING)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true, errorMessage = null)
            showToast("Checking cloud data...", ToastType.INFO)
            
            try {
                Log.d("ExpenseTracker", "Starting cloud restore operation")
                
                // First check if there's any data in the cloud
                val cloudAccounts = cloudSyncRepository.syncAccountsFromCloud()
                val cloudTransactions = cloudSyncRepository.syncTransactionsFromCloud()
                
                if (cloudAccounts.isFailure && cloudTransactions.isFailure) {
                    val accountError = cloudAccounts.exceptionOrNull()?.message ?: "Unknown error"
                    val transactionError = cloudTransactions.exceptionOrNull()?.message ?: "Unknown error"
                    throw Exception("Failed to connect to cloud: Account Error: $accountError, Transaction Error: $transactionError")
                }
                
                val accountCount = cloudAccounts.getOrNull()?.size ?: 0
                val transactionCount = cloudTransactions.getOrNull()?.size ?: 0
                
                if (accountCount == 0 && transactionCount == 0) {
                    _uiState.value = _uiState.value.copy(isRestoring = false)
                    showToast("📭 No data found in cloud. Please backup your data first.", ToastType.WARNING)
                    return@launch
                }
                
                Log.d("ExpenseTracker", "Found cloud data: $accountCount accounts, $transactionCount transactions")
                showToast("Restoring $accountCount accounts, $transactionCount transactions...", ToastType.INFO)
                
                // Download and merge cloud data
                restoreAndMergeCloudData()
                
                // Automatically recalculate all account balances after restore to ensure consistency
                Log.d("ExpenseTracker", "Recalculating balances after restore...")
                showToast("Synchronizing account balances...", ToastType.INFO)
                expenseRepository.recalculateAllBalances()
                Log.d("ExpenseTracker", "Balance recalculation after restore completed")
                
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    lastRestoreTime = System.currentTimeMillis(),
                    errorMessage = null
                )
                
                showToast("✅ Restore completed! $accountCount accounts, $transactionCount transactions, balances synchronized", ToastType.SUCCESS)
                Log.d("ExpenseTracker", "Restore operation completed successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRestoring = false)
                showToast("❌ Restore failed: ${e.message}", ToastType.ERROR)
                Log.e("ExpenseTracker", "Restore operation failed: ${e.message}", e)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun showToast(message: String, type: ToastType = ToastType.INFO) {
        _uiState.value = _uiState.value.copy(
            toastMessage = message,
            toastType = type
        )
    }
    
    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
    
    fun testFirestoreConnection() {
        if (!_uiState.value.isSignedIn) {
            showToast("Please sign in to test Firestore connection", ToastType.WARNING)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            showToast("Testing Firestore connection...", ToastType.INFO)
            
            try {
                Log.d("ExpenseTracker", "Testing Firestore connection...")
                
                val result = cloudSyncRepository.testFirestoreConnection()
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    showToast("✅ Firestore connection successful!", ToastType.SUCCESS)
                    Log.d("ExpenseTracker", "Firestore connection test successful: ${result.getOrNull()}")
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    showToast("❌ Firestore test failed: ${result.exceptionOrNull()?.message}", ToastType.ERROR)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                showToast("❌ Firestore test failed: ${e.message}", ToastType.ERROR)
                Log.e("ExpenseTracker", "Firestore connection test failed: ${e.message}", e)
            }
        }
    }
    
    fun testEncryption() {
        if (!_uiState.value.isSignedIn) {
            showToast("Please sign in to test encryption", ToastType.WARNING)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            showToast("Testing encryption consistency...", ToastType.INFO)
            
            try {
                Log.d("ExpenseTracker", "Testing encryption consistency...")
                
                // Test account encryption specifically with balance values
                val testAccount = com.expensetracker.data.model.Account(
                    id = 999999,
                    name = "Test Account", 
                    iconName = "test_icon",
                    balance = 1234.56, // Test with decimal balance
                    initialBalance = 1000.00, // Test with different initial balance
                    createdAt = System.currentTimeMillis()
                )
                
                Log.d("ExpenseTracker", "Original account: ${testAccount.name}, balance: ${testAccount.balance}")
                
                // Test encryption/decryption locally first
                val encryptedData = encryptionManager.encryptAccount(testAccount)
                val decryptedAccount = encryptionManager.decryptAccount(encryptedData)
                
                Log.d("ExpenseTracker", "Decrypted account: ${decryptedAccount.name}, balance: ${decryptedAccount.balance}")
                
                if (testAccount.balance == decryptedAccount.balance && testAccount.initialBalance == decryptedAccount.initialBalance) {
                    Log.d("ExpenseTracker", "Local encryption/decryption test PASSED")
                    
                    // Now test cloud round-trip
                    val result = cloudSyncRepository.testEncryptionConsistency()
                    
                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        showToast("✅ Encryption test passed! Balance: ${testAccount.balance} → ${decryptedAccount.balance}, Initial: ${testAccount.initialBalance} → ${decryptedAccount.initialBalance}", ToastType.SUCCESS)
                        Log.d("ExpenseTracker", "Encryption test successful: ${result.getOrNull()}")
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        showToast("❌ Cloud encryption test failed: ${result.exceptionOrNull()?.message}", ToastType.ERROR)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    showToast("❌ Local encryption test failed: Balance ${testAccount.balance} ≠ ${decryptedAccount.balance} or Initial ${testAccount.initialBalance} ≠ ${decryptedAccount.initialBalance}", ToastType.ERROR)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                showToast("❌ Encryption test failed: ${e.message}", ToastType.ERROR)
                Log.e("ExpenseTracker", "Encryption test failed: ${e.message}", e)
            }
        }
    }
    

    fun clearCloudData() {
        if (!_uiState.value.isSignedIn) {
            showToast("Please sign in to clear cloud data", ToastType.WARNING)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            showToast("Clearing cloud data...", ToastType.INFO)
            
            try {
                Log.d("ExpenseTracker", "Starting to clear cloud data...")
                
                val result = cloudSyncRepository.clearAllCloudData()
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    showToast("🗑️ Cloud data cleared successfully!", ToastType.SUCCESS)
                    Log.d("ExpenseTracker", "Cloud data cleared successfully")
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    showToast("❌ Failed to clear cloud data: ${result.exceptionOrNull()?.message}", ToastType.ERROR)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                showToast("❌ Failed to clear cloud data: ${e.message}", ToastType.ERROR)
                Log.e("ExpenseTracker", "Failed to clear cloud data: ${e.message}", e)
            }
        }
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
    
    private suspend fun restoreAndMergeCloudData() {
        try {
            Log.d("ExpenseTracker", "Starting restore and merge from cloud")
            
            // Get cloud data
            val cloudAccounts = cloudSyncRepository.syncAccountsFromCloud()
            val cloudTransactions = cloudSyncRepository.syncTransactionsFromCloud()
            
            Log.d("ExpenseTracker", "Cloud accounts result: ${cloudAccounts.isSuccess}, data: ${cloudAccounts.getOrNull()?.size} accounts")
            Log.d("ExpenseTracker", "Cloud transactions result: ${cloudTransactions.isSuccess}, data: ${cloudTransactions.getOrNull()?.size} transactions")
            
            if (cloudAccounts.isFailure) {
                Log.e("ExpenseTracker", "Failed to get cloud accounts: ${cloudAccounts.exceptionOrNull()?.message}")
            }
            
            if (cloudTransactions.isFailure) {
                Log.e("ExpenseTracker", "Failed to get cloud transactions: ${cloudTransactions.exceptionOrNull()?.message}")
            }
            
            // Get current local data
            val localAccounts = expenseRepository.getAllAccounts().firstOrNull() ?: emptyList()
            val localTransactions = expenseRepository.getAllTransactions().firstOrNull() ?: emptyList()
            
            Log.d("ExpenseTracker", "Local data: ${localAccounts.size} accounts, ${localTransactions.size} transactions")
            
            // Merge accounts
            var accountsRestored = 0
            if (cloudAccounts.isSuccess) {
                val accountsToAdd = cloudAccounts.getOrNull() ?: emptyList()
                Log.d("ExpenseTracker", "Processing ${accountsToAdd.size} cloud accounts for restore")
                
                // Debug: Log account balances from cloud before merging
                accountsToAdd.forEach { account ->
                    Log.d("ExpenseTracker", "RESTORE - Cloud Account: ${account.name}, Balance: ${account.balance}")
                }
                
                accountsToAdd.forEach { cloudAccount ->
                    // Check if account already exists locally (by name, since IDs might differ)
                    val existsLocally = localAccounts.any { it.name == cloudAccount.name }
                    if (!existsLocally) {
                        // Add cloud account to local database
                        val newAccountId = expenseRepository.insertAccount(cloudAccount.copy(id = 0)) // Let DB assign new ID
                        accountsRestored++
                        Log.d("ExpenseTracker", "Restored account from cloud: ${cloudAccount.name} (new ID: $newAccountId)")
                    } else {
                        Log.d("ExpenseTracker", "Account already exists locally: ${cloudAccount.name}")
                    }
                }
            }
            
            // Merge transactions
            var transactionsRestored = 0
            if (cloudTransactions.isSuccess) {
                val transactionsToAdd = cloudTransactions.getOrNull() ?: emptyList()
                Log.d("ExpenseTracker", "Processing ${transactionsToAdd.size} cloud transactions for restore")
                
                transactionsToAdd.forEach { cloudTransaction ->
                    // Check if transaction already exists locally (by timestamp + amount + description)
                    val existsLocally = localTransactions.any { 
                        it.createdAt == cloudTransaction.createdAt && 
                        it.amount == cloudTransaction.amount && 
                        it.description == cloudTransaction.description
                    }
                    
                    if (!existsLocally) {
                        // Try to get transaction with account mapping first
                        val transactionWithMapping = cloudSyncRepository.getTransactionWithAccountMapping(cloudTransaction.id.toString())
                        
                        val mappedTransaction = if (transactionWithMapping != null) {
                            // Use account mapping information for precise matching
                            mapCloudTransactionWithMapping(transactionWithMapping)
                        } else {
                            // Fallback to legacy mapping
                            Log.d("ExpenseTracker", "No account mapping found for ${cloudTransaction.description}, using legacy mapping")
                            mapCloudTransactionToLocal(cloudTransaction)
                        }
                        
                        if (mappedTransaction != null) {
                            // Insert transaction but don't apply balance changes yet (will be done in recalculateAllBalances)
                            val newTransactionId = expenseRepository.insertTransactionWithoutBalanceUpdate(mappedTransaction.copy(id = 0))
                            transactionsRestored++
                            Log.d("ExpenseTracker", "Restored transaction from cloud: ${cloudTransaction.description} (new ID: $newTransactionId)")
                        } else {
                            Log.w("ExpenseTracker", "Could not map cloud transaction to local accounts: ${cloudTransaction.description}")
                        }
                    } else {
                        Log.d("ExpenseTracker", "Transaction already exists locally: ${cloudTransaction.description}")
                    }
                }
            }
            
            Log.d("ExpenseTracker", "Restore completed: $accountsRestored accounts, $transactionsRestored transactions restored")
            
        } catch (e: Exception) {
            Log.e("ExpenseTracker", "Error restoring cloud data: ${e.message}", e)
        }
    }
    
    private suspend fun mapCloudTransactionToLocal(cloudTransaction: Transaction): Transaction? {
        try {
            // Get current local accounts to map IDs
            val localAccounts = expenseRepository.getAllAccounts().firstOrNull() ?: emptyList()
            
            if (localAccounts.isEmpty()) {
                Log.w("ExpenseTracker", "No local accounts available to map cloud transaction")
                return null
            }
            
            // Get cloud accounts to help with name-based mapping
            val cloudAccountsResult = cloudSyncRepository.syncAccountsFromCloud()
            val cloudAccounts = cloudAccountsResult.getOrNull() ?: emptyList()
            
            // Find the cloud account that this transaction belongs to
            val cloudFromAccount = cloudAccounts.find { it.id == cloudTransaction.fromAccountId }
            val cloudToAccount = if (cloudTransaction.toAccountId != null) {
                cloudAccounts.find { it.id == cloudTransaction.toAccountId }
            } else null
            
            // Try to find matching local account by ID first, then by name
            var fromAccount = localAccounts.find { it.id == cloudTransaction.fromAccountId }
            if (fromAccount == null && cloudFromAccount != null) {
                // Try to match by account name and initial balance
                fromAccount = localAccounts.find { 
                    it.name == cloudFromAccount.name && it.initialBalance == cloudFromAccount.initialBalance
                }
                if (fromAccount != null) {
                    Log.d("ExpenseTracker", "Mapped cloud transaction by name: ${cloudFromAccount.name} → ${fromAccount.name}")
                }
            }
            
            // If still no match, something is wrong - don't map to random account
            if (fromAccount == null) {
                Log.w("ExpenseTracker", "Could not find matching account for transaction ${cloudTransaction.description}. Cloud account: ${cloudFromAccount?.name}, Available local accounts: ${localAccounts.map { it.name }}")
                return null
            }
            
            var toAccount: com.expensetracker.data.model.Account? = null
            if (cloudTransaction.toAccountId != null) {
                // Try to match toAccount by ID first, then by name
                toAccount = localAccounts.find { it.id == cloudTransaction.toAccountId }
                if (toAccount == null && cloudToAccount != null) {
                    toAccount = localAccounts.find { 
                        it.name == cloudToAccount.name && it.initialBalance == cloudToAccount.initialBalance
                    }
                    if (toAccount != null) {
                        Log.d("ExpenseTracker", "Mapped cloud transaction TO account by name: ${cloudToAccount.name} → ${toAccount.name}")
                    }
                }
            }
            
            Log.d("ExpenseTracker", "Transaction mapping: ${cloudTransaction.description} from ${cloudFromAccount?.name} (${cloudTransaction.fromAccountId}) → ${fromAccount.name} (${fromAccount.id})")
            
            return cloudTransaction.copy(
                id = 0, // Let database assign new ID
                fromAccountId = fromAccount.id,
                toAccountId = toAccount?.id
            )
        } catch (e: Exception) {
            Log.e("ExpenseTracker", "Error mapping cloud transaction: ${e.message}")
            return null
        }
    }
    
    private suspend fun mapCloudTransactionWithMapping(transactionWithMapping: com.expensetracker.security.TransactionWithAccountMapping): Transaction? {
        try {
            val localAccounts = expenseRepository.getAllAccounts().firstOrNull() ?: emptyList()
            
            if (localAccounts.isEmpty()) {
                Log.w("ExpenseTracker", "No local accounts available for mapping")
                return null
            }
            
            val transaction = transactionWithMapping.transaction
            val fromMapping = transactionWithMapping.fromAccountMapping
            val toMapping = transactionWithMapping.toAccountMapping
            
            // Find local account that matches the mapping
            val fromAccount = if (fromMapping != null) {
                localAccounts.find { 
                    it.name == fromMapping.name && 
                    it.initialBalance == fromMapping.initialBalance &&
                    it.iconName == fromMapping.iconName
                }
            } else {
                // Fallback to ID matching
                localAccounts.find { it.id == transaction.fromAccountId }
            }
            
            val toAccount = if (toMapping != null && transaction.toAccountId != null) {
                localAccounts.find { 
                    it.name == toMapping.name && 
                    it.initialBalance == toMapping.initialBalance &&
                    it.iconName == toMapping.iconName
                }
            } else if (transaction.toAccountId != null) {
                // Fallback to ID matching
                localAccounts.find { it.id == transaction.toAccountId }
            } else {
                null
            }
            
            if (fromAccount == null) {
                Log.w("ExpenseTracker", "Could not find matching local account for transaction ${transaction.description}. Looking for: ${fromMapping?.name}")
                return null
            }
            
            Log.d("ExpenseTracker", "Mapped transaction with account mapping: ${transaction.description} from ${fromMapping?.name} → ${fromAccount.name}")
            
            return transaction.copy(
                id = 0, // Let database assign new ID
                fromAccountId = fromAccount.id,
                toAccountId = toAccount?.id
            )
        } catch (e: Exception) {
            Log.e("ExpenseTracker", "Error mapping transaction with account mapping: ${e.message}")
            return null
        }
    }
    
    private fun logDeviceInfo() {
        Log.d("ExpenseTracker", "Device Info - Model: ${Build.MODEL}, SDK: ${Build.VERSION.SDK_INT}, Brand: ${Build.BRAND}")
        Log.d("ExpenseTracker", "Manufacturer: ${Build.MANUFACTURER}, Product: ${Build.PRODUCT}")
    }
}
