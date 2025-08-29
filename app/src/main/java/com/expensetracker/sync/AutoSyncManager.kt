package com.expensetracker.sync

import com.expensetracker.auth.AuthenticationRepository
import com.expensetracker.cloud.CloudSyncRepository
import com.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic synchronization of data when user is signed in
 */
@Singleton
class AutoSyncManager @Inject constructor(
    private val authRepository: AuthenticationRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val expenseRepository: ExpenseRepository
) {
    
    suspend fun performAutoSync() {
        try {
            val isSignedIn = authRepository.isSignedIn.firstOrNull() ?: false
            
            if (isSignedIn) {
                // Sync all data to cloud
                syncAllDataToCloud()
            }
        } catch (e: Exception) {
            // Handle sync error silently
        }
    }
    
    private suspend fun syncAllDataToCloud() {
        try {
            // Get all local data
            val accounts = expenseRepository.getAllAccounts().firstOrNull() ?: emptyList()
            val transactions = expenseRepository.getAllTransactions().firstOrNull() ?: emptyList()
            val categories = expenseRepository.getAllCategories().firstOrNull() ?: emptyList()
            val budgets = expenseRepository.getAllBudgets().firstOrNull() ?: emptyList()
            
            // Sync to cloud
            cloudSyncRepository.syncAccountsToCloud(accounts)
            cloudSyncRepository.syncTransactionsToCloud(transactions)
            cloudSyncRepository.syncCategoriesToCloud(categories)
            cloudSyncRepository.syncBudgetsToCloud(budgets)
            
            // Update last sync timestamp
            cloudSyncRepository.performFullSync()
            
        } catch (e: Exception) {
            // Handle individual sync failures
        }
    }
    
    suspend fun syncFromCloud() {
        try {
            val isSignedIn = authRepository.isSignedIn.firstOrNull() ?: false
            
            if (isSignedIn) {
                // Sync data from cloud
                val accountsResult = cloudSyncRepository.syncAccountsFromCloud()
                val transactionsResult = cloudSyncRepository.syncTransactionsFromCloud()
                val categoriesResult = cloudSyncRepository.syncCategoriesFromCloud()
                val budgetsResult = cloudSyncRepository.syncBudgetsFromCloud()
                
                // Update local database with cloud data
                accountsResult.getOrNull()?.forEach { account ->
                    try {
                        expenseRepository.insertAccount(account)
                    } catch (e: Exception) {
                        // Handle conflict - maybe update instead
                        expenseRepository.updateAccount(account)
                    }
                }
                
                transactionsResult.getOrNull()?.forEach { transaction ->
                    try {
                        expenseRepository.insertTransaction(transaction)
                    } catch (e: Exception) {
                        // Handle conflict
                    }
                }
                
                categoriesResult.getOrNull()?.forEach { category ->
                    try {
                        expenseRepository.insertCategory(category)
                    } catch (e: Exception) {
                        // Handle conflict
                        expenseRepository.updateCategory(category)
                    }
                }
                
                budgetsResult.getOrNull()?.forEach { budget ->
                    try {
                        expenseRepository.insertBudget(budget)
                    } catch (e: Exception) {
                        // Handle conflict
                        expenseRepository.updateBudget(budget)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle sync error
        }
    }
}
