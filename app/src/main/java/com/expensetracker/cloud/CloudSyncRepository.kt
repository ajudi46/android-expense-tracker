package com.expensetracker.cloud

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.expensetracker.data.model.Account
import com.expensetracker.data.model.Transaction
import com.expensetracker.data.model.Budget
import com.expensetracker.data.model.Category
import com.expensetracker.auth.AuthenticationRepository
import com.expensetracker.security.EncryptionManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthenticationRepository,
    private val encryptionManager: EncryptionManager
) {
    
    private fun getUserCollection(collection: String): String? {
        val currentUser = authRepository.getCurrentFirebaseUser()
        return currentUser?.let { "users/${it.uid}/$collection" }
    }
    
    // Accounts
    suspend fun syncAccountsToCloud(accounts: List<Account>): Result<Unit> {
        return try {
            val userCollection = getUserCollection("accounts") ?: throw Exception("User not signed in")
            
            if (accounts.isEmpty()) {
                return Result.success(Unit)
            }
            
            android.util.Log.d("CloudSync", "Starting batch upload of ${accounts.size} accounts")
            
            // Use batch for better performance
            val batch = firestore.batch()
            accounts.forEach { account ->
                val encryptedAccount = encryptionManager.encryptAccount(account)
                val docRef = firestore.collection(userCollection).document(account.id.toString())
                batch.set(docRef, encryptedAccount)
            }
            
            batch.commit().await()
            android.util.Log.d("CloudSync", "Successfully uploaded ${accounts.size} accounts")
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "Failed to sync accounts to cloud: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun syncAccountsFromCloud(): Result<List<Account>> {
        return try {
            val userCollection = getUserCollection("accounts") ?: throw Exception("User not signed in")
            
            val snapshot = firestore.collection(userCollection)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val accounts = snapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null
                    encryptionManager.decryptAccount(data)
                } catch (e: Exception) {
                    android.util.Log.e("CloudSync", "Failed to decrypt account: ${e.message}")
                    null // Skip corrupted data
                }
            }
            
            Result.success(accounts)
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "Failed to sync accounts from cloud: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Transactions
    suspend fun syncTransactionsToCloud(transactions: List<Transaction>): Result<Unit> {
        return try {
            val userCollection = getUserCollection("transactions") ?: throw Exception("User not signed in")
            
            if (transactions.isEmpty()) {
                return Result.success(Unit)
            }
            
            android.util.Log.d("CloudSync", "Starting batch upload of ${transactions.size} transactions")
            
            // For large datasets, split into batches (Firestore batch limit is 500 operations)
            val batchSize = 450 // Leave some margin
            val totalBatches = (transactions.size + batchSize - 1) / batchSize
            
            for (i in 0 until totalBatches) {
                val startIndex = i * batchSize
                val endIndex = minOf(startIndex + batchSize, transactions.size)
                val batch = firestore.batch()
                
                for (j in startIndex until endIndex) {
                    val transaction = transactions[j]
                    val encryptedTransaction = encryptionManager.encryptTransaction(transaction)
                    val docRef = firestore.collection(userCollection).document(transaction.id.toString())
                    batch.set(docRef, encryptedTransaction)
                }
                
                batch.commit().await()
                android.util.Log.d("CloudSync", "Uploaded batch ${i + 1}/$totalBatches (${endIndex - startIndex} transactions)")
            }
            
            android.util.Log.d("CloudSync", "Successfully uploaded all ${transactions.size} transactions")
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "Failed to sync transactions to cloud: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun syncTransactionsFromCloud(lastSyncTimestamp: Long = 0): Result<List<Transaction>> {
        return try {
            val userCollection = getUserCollection("transactions") ?: throw Exception("User not signed in")
            
            val query = if (lastSyncTimestamp > 0) {
                firestore.collection(userCollection)
                    .whereGreaterThan("createdAt", lastSyncTimestamp)
            } else {
                firestore.collection(userCollection)
            }
            
            val snapshot = query
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val transactions = snapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null
                    encryptionManager.decryptTransaction(data)
                } catch (e: Exception) {
                    android.util.Log.e("CloudSync", "Failed to decrypt transaction: ${e.message}")
                    null // Skip corrupted data
                }
            }
            
            Result.success(transactions)
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "Failed to sync transactions from cloud: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Categories
    suspend fun syncCategoriesToCloud(categories: List<Category>): Result<Unit> {
        return try {
            val userCollection = getUserCollection("categories") ?: throw Exception("User not signed in")
            
            categories.forEach { category ->
                val encryptedCategory = encryptionManager.encryptCategory(category)
                firestore.collection(userCollection)
                    .document(category.id.toString())
                    .set(encryptedCategory)
                    .await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncCategoriesFromCloud(): Result<List<Category>> {
        return try {
            val userCollection = getUserCollection("categories") ?: throw Exception("User not signed in")
            
            val snapshot = firestore.collection(userCollection)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val categories = snapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null
                    encryptionManager.decryptCategory(data)
                } catch (e: Exception) {
                    null // Skip corrupted data
                }
            }
            
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Budgets
    suspend fun syncBudgetsToCloud(budgets: List<Budget>): Result<Unit> {
        return try {
            val userCollection = getUserCollection("budgets") ?: throw Exception("User not signed in")
            
            budgets.forEach { budget ->
                val encryptedBudget = encryptionManager.encryptBudget(budget)
                firestore.collection(userCollection)
                    .document(budget.id.toString())
                    .set(encryptedBudget)
                    .await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncBudgetsFromCloud(): Result<List<Budget>> {
        return try {
            val userCollection = getUserCollection("budgets") ?: throw Exception("User not signed in")
            
            val snapshot = firestore.collection(userCollection)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val budgets = snapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null
                    encryptionManager.decryptBudget(data)
                } catch (e: Exception) {
                    null // Skip corrupted data
                }
            }
            
            Result.success(budgets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Full sync method - syncs ALL local data to cloud
    suspend fun performFullSync(
        localAccounts: List<Account> = emptyList(),
        localTransactions: List<Transaction> = emptyList(), 
        localCategories: List<Category> = emptyList(),
        localBudgets: List<Budget> = emptyList()
    ): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentFirebaseUser() ?: throw Exception("User not signed in")
            
            android.util.Log.d("CloudSync", "Starting full sync: ${localAccounts.size} accounts, ${localTransactions.size} transactions")
            
            // Sync all local data TO cloud with error checking
            if (localAccounts.isNotEmpty()) {
                val accountResult = syncAccountsToCloud(localAccounts)
                if (accountResult.isFailure) {
                    throw Exception("Account sync failed: ${accountResult.exceptionOrNull()?.message}")
                }
            }
            
            if (localTransactions.isNotEmpty()) {
                val transactionResult = syncTransactionsToCloud(localTransactions)
                if (transactionResult.isFailure) {
                    throw Exception("Transaction sync failed: ${transactionResult.exceptionOrNull()?.message}")
                }
            }
            
            if (localCategories.isNotEmpty()) {
                val categoryResult = syncCategoriesToCloud(localCategories)
                if (categoryResult.isFailure) {
                    throw Exception("Category sync failed: ${categoryResult.exceptionOrNull()?.message}")
                }
            }
            
            if (localBudgets.isNotEmpty()) {
                val budgetResult = syncBudgetsToCloud(localBudgets)
                if (budgetResult.isFailure) {
                    throw Exception("Budget sync failed: ${budgetResult.exceptionOrNull()?.message}")
                }
            }
            
            // Update last sync timestamp
            authRepository.updateLastSyncTimestamp(currentUser.uid, System.currentTimeMillis())
            
            android.util.Log.d("CloudSync", "Full sync completed successfully")
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "Full sync failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Clear all cloud data for testing purposes
    suspend fun clearAllCloudData(): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentFirebaseUser() ?: throw Exception("User not signed in")
            
            android.util.Log.d("CloudSync", "Starting to clear all cloud data for user: ${currentUser.uid}")
            
            // Clear accounts
            val accountsCollection = getUserCollection("accounts")
            if (accountsCollection != null) {
                val accountsSnapshot = firestore.collection(accountsCollection).get().await()
                val accountsBatch = firestore.batch()
                accountsSnapshot.documents.forEach { doc ->
                    accountsBatch.delete(doc.reference)
                }
                if (accountsSnapshot.documents.isNotEmpty()) {
                    accountsBatch.commit().await()
                    android.util.Log.d("CloudSync", "Deleted ${accountsSnapshot.documents.size} accounts")
                }
            }
            
            // Clear transactions
            val transactionsCollection = getUserCollection("transactions")
            if (transactionsCollection != null) {
                val transactionsSnapshot = firestore.collection(transactionsCollection).get().await()
                val transactionsBatch = firestore.batch()
                transactionsSnapshot.documents.forEach { doc ->
                    transactionsBatch.delete(doc.reference)
                }
                if (transactionsSnapshot.documents.isNotEmpty()) {
                    transactionsBatch.commit().await()
                    android.util.Log.d("CloudSync", "Deleted ${transactionsSnapshot.documents.size} transactions")
                }
            }
            
            // Clear categories
            val categoriesCollection = getUserCollection("categories")
            if (categoriesCollection != null) {
                val categoriesSnapshot = firestore.collection(categoriesCollection).get().await()
                val categoriesBatch = firestore.batch()
                categoriesSnapshot.documents.forEach { doc ->
                    categoriesBatch.delete(doc.reference)
                }
                if (categoriesSnapshot.documents.isNotEmpty()) {
                    categoriesBatch.commit().await()
                    android.util.Log.d("CloudSync", "Deleted ${categoriesSnapshot.documents.size} categories")
                }
            }
            
            // Clear budgets
            val budgetsCollection = getUserCollection("budgets")
            if (budgetsCollection != null) {
                val budgetsSnapshot = firestore.collection(budgetsCollection).get().await()
                val budgetsBatch = firestore.batch()
                budgetsSnapshot.documents.forEach { doc ->
                    budgetsBatch.delete(doc.reference)
                }
                if (budgetsSnapshot.documents.isNotEmpty()) {
                    budgetsBatch.commit().await()
                    android.util.Log.d("CloudSync", "Deleted ${budgetsSnapshot.documents.size} budgets")
                }
            }
            
            android.util.Log.d("CloudSync", "Successfully cleared all cloud data")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "Failed to clear cloud data: ${e.message}")
            Result.failure(e)
        }
    }
}
