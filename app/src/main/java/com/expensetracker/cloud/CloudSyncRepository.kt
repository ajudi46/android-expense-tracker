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
            
            accounts.forEach { account ->
                val encryptedAccount = encryptionManager.encryptAccount(account)
                firestore.collection(userCollection)
                    .document(account.id.toString())
                    .set(encryptedAccount)
                    .await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncAccountsFromCloud(): Result<List<Account>> {
        return try {
            val userCollection = getUserCollection("accounts") ?: throw Exception("User not signed in")
            
            val snapshot = firestore.collection(userCollection)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val accounts = snapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null
                    encryptionManager.decryptAccount(data)
                } catch (e: Exception) {
                    null // Skip corrupted data
                }
            }
            
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Transactions
    suspend fun syncTransactionsToCloud(transactions: List<Transaction>): Result<Unit> {
        return try {
            val userCollection = getUserCollection("transactions") ?: throw Exception("User not signed in")
            
            transactions.forEach { transaction ->
                val encryptedTransaction = encryptionManager.encryptTransaction(transaction)
                firestore.collection(userCollection)
                    .document(transaction.id.toString())
                    .set(encryptedTransaction)
                    .await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
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
                    null // Skip corrupted data
                }
            }
            
            Result.success(transactions)
        } catch (e: Exception) {
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
    
    // Full sync method
    suspend fun performFullSync(): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentFirebaseUser() ?: throw Exception("User not signed in")
            
            // Update last sync timestamp
            authRepository.updateLastSyncTimestamp(currentUser.uid, System.currentTimeMillis())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
