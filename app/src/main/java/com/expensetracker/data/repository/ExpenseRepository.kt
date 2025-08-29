package com.expensetracker.data.repository

import com.expensetracker.data.dao.AccountDao
import com.expensetracker.data.dao.BudgetDao
import com.expensetracker.data.dao.CategoryDao
import com.expensetracker.data.dao.TransactionDao
import com.expensetracker.data.model.Account
import com.expensetracker.data.model.Budget
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Transaction
import com.expensetracker.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao
) {
    // Account operations
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()
    suspend fun getAccountById(id: Long): Account? = accountDao.getAccountById(id)
    suspend fun insertAccount(account: Account): Long = accountDao.insertAccount(account)
    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: Account) = accountDao.deleteAccount(account)
    fun getTotalBalance(): Flow<Double?> = accountDao.getTotalBalance()

    // Transaction operations
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> = transactionDao.getRecentTransactions(limit)
    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> = transactionDao.getTransactionsByAccount(accountId)
    fun getTransactionsForMonth(month: Int, year: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsForMonth(month, year)
    
    suspend fun getTotalSpentForCategoryAndMonth(category: String, month: Int, year: String): Double =
        transactionDao.getTotalSpentForCategoryAndMonth(category, month, year) ?: 0.0
    
    suspend fun insertTransaction(transaction: Transaction): Long {
        val transactionId = transactionDao.insertTransaction(transaction)
        
        // Apply transaction balance changes
        applyTransactionBalance(transaction)
        
        // Update budget for expense transactions
        if (transaction.type == TransactionType.EXPENSE) {
            updateBudgetSpending(transaction)
        }
        
        android.util.Log.d("ExpenseRepository", "Transaction inserted with balance update: ${transaction.description}")
        return transactionId
    }
    
    private suspend fun updateBudgetSpending(transaction: Transaction) {
        if (transaction.type == TransactionType.EXPENSE) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = transaction.createdAt
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            
            // Debug: Print transaction details
            println("🔍 DEBUG: Transaction added - Category: '${transaction.category}', Amount: ${transaction.amount}, Month: $month, Year: $year")
            
            // Check if budget exists for this category in this month
            val existingBudget = getBudgetForCategory(transaction.category, month, year)
            if (existingBudget != null) {
                // Recalculate total from ALL transactions (not just add to existing)
                recalculateBudgetFromTransactions(transaction.category, month, year)
                println("🔍 DEBUG: Budget found and recalculated from all transactions")
            } else {
                println("🔍 DEBUG: NO BUDGET FOUND for category '${transaction.category}' in $month/$year")
            }
        }
    }
    
    suspend fun recalculateBudgetSpending(category: String, month: Int, year: Int) {
        // This method can be called to recalculate budget spending from all transactions
        val budget = getBudgetForCategory(category, month, year)
        if (budget != null) {
            // For now, we'll use the current approach
            // In a production app, you'd calculate from all expense transactions
            val currentSpent = budget.currentSpent
            updateBudgetSpent(category, month, year, currentSpent)
        }
    }
    
    suspend fun updateTransaction(transaction: Transaction) {
        // Get the original transaction to calculate balance difference
        val originalTransaction = transactionDao.getTransactionById(transaction.id)
        
        if (originalTransaction != null) {
            // Revert the original transaction's balance changes
            revertTransactionBalance(originalTransaction)
        }
        
        // Update the transaction
        transactionDao.updateTransaction(transaction)
        
        // Apply the new transaction's balance changes
        applyTransactionBalance(transaction)
        
        // Update budget if it's an expense transaction
        if (transaction.type == TransactionType.EXPENSE) {
            updateBudgetSpending(transaction)
        }
        
        android.util.Log.d("ExpenseRepository", "Transaction updated with balance recalculation: ${transaction.description}")
    }
    suspend fun deleteTransaction(transaction: Transaction) {
        // Revert the transaction's balance changes before deleting
        revertTransactionBalance(transaction)
        
        // Delete the transaction
        transactionDao.deleteTransaction(transaction)
        
        android.util.Log.d("ExpenseRepository", "Transaction deleted with balance revert: ${transaction.description}")
    }

    // Category operations
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> = categoryDao.getCategoriesByType(type)
    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)
    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    // Budget operations
    fun getAllBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets()
    
    fun getCurrentMonthBudgets(): Flow<List<Budget>> {
        val calendar = Calendar.getInstance()
        return budgetDao.getBudgetsForMonth(
            calendar.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-based
            calendar.get(Calendar.YEAR)
        )
    }
    
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> = 
        budgetDao.getBudgetsForMonth(month, year)
    
    suspend fun getBudgetForCategory(category: String, month: Int, year: Int): Budget? = 
        budgetDao.getBudgetForCategory(category, month, year)
    
    suspend fun insertBudget(budget: Budget): Long = budgetDao.insertBudget(budget)
    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)
    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)
    
    suspend fun updateBudgetSpent(category: String, month: Int, year: Int, newAmount: Double) {
        budgetDao.updateBudgetSpent(category, month, year, newAmount)
    }
    
    // Calculate total spending from all existing transactions for a specific category and month
    suspend fun calculateTotalSpentFromTransactions(category: String, month: Int, year: Int): Double {
        // Use direct database query instead of Flow collection for immediate result
        val totalSpent = transactionDao.getTotalSpentForCategoryAndMonth(category, month, year.toString()) ?: 0.0
        println("🔍 DEBUG: Direct DB query for $category ($month/$year): Total spent = $totalSpent")
        return totalSpent
    }
    
    // Recalculate and update budget spending from all existing transactions
    suspend fun recalculateBudgetFromTransactions(category: String, month: Int, year: Int) {
        val totalSpent = calculateTotalSpentFromTransactions(category, month, year)
        updateBudgetSpent(category, month, year, totalSpent)
        println("🔍 DEBUG: Recalculated budget for $category ($month/$year): Total spent = $totalSpent")
    }
    
    // Recalculate ALL budgets from existing transactions
    suspend fun recalculateAllBudgetsFromTransactions() {
        val allBudgets = budgetDao.getAllBudgets()
        allBudgets.collect { budgetList ->
            budgetList.forEach { budget ->
                recalculateBudgetFromTransactions(budget.category, budget.month, budget.year)
            }
        }
    }
    
    // Clear all local data (for logout)
    suspend fun clearAllLocalData() {
        android.util.Log.d("ExpenseRepository", "Clearing all local data...")
        
        // Clear all transactions first (due to foreign key constraints)
        transactionDao.deleteAllTransactions()
        
        // Clear all other data
        accountDao.deleteAllAccounts()
        categoryDao.deleteAllCategories()
        budgetDao.deleteAllBudgets()
        
        android.util.Log.d("ExpenseRepository", "All local data cleared successfully")
    }
    
    // Helper functions for balance management
    private suspend fun applyTransactionBalance(transaction: Transaction) {
        when (transaction.type) {
            TransactionType.EXPENSE -> {
                accountDao.updateBalance(transaction.fromAccountId, -transaction.amount)
            }
            TransactionType.INCOME -> {
                accountDao.updateBalance(transaction.fromAccountId, transaction.amount)
            }
            TransactionType.TRANSFER -> {
                transaction.toAccountId?.let { toAccountId ->
                    accountDao.updateBalance(transaction.fromAccountId, -transaction.amount)
                    accountDao.updateBalance(toAccountId, transaction.amount)
                }
            }
        }
    }
    
    private suspend fun revertTransactionBalance(transaction: Transaction) {
        when (transaction.type) {
            TransactionType.EXPENSE -> {
                // Revert expense: add back the amount
                accountDao.updateBalance(transaction.fromAccountId, transaction.amount)
            }
            TransactionType.INCOME -> {
                // Revert income: subtract the amount
                accountDao.updateBalance(transaction.fromAccountId, -transaction.amount)
            }
            TransactionType.TRANSFER -> {
                transaction.toAccountId?.let { toAccountId ->
                    // Revert transfer: reverse the movements
                    accountDao.updateBalance(transaction.fromAccountId, transaction.amount)
                    accountDao.updateBalance(toAccountId, -transaction.amount)
                }
            }
        }
    }
    
    // Function to recalculate all account balances from scratch
    suspend fun recalculateAllBalances() {
        android.util.Log.d("ExpenseRepository", "Starting complete balance recalculation...")
        
        try {
            // Get all accounts
            val accounts = mutableListOf<Account>()
            getAllAccounts().collect { accountList ->
                accounts.clear()
                accounts.addAll(accountList)
                return@collect // Exit after first emission
            }
            
            // Get all transactions
            val transactions = mutableListOf<Transaction>()
            getAllTransactions().collect { transactionList ->
                transactions.clear()
                transactions.addAll(transactionList)
                return@collect // Exit after first emission
            }
            
            android.util.Log.d("ExpenseRepository", "Processing ${accounts.size} accounts and ${transactions.size} transactions")
            
            // Recalculate account balances from initial account balance + all transactions
            for (account in accounts) {
                // We need to track the initial balance that was set when account was created
                // This is stored in the account but may have been corrupted during sync
                
                // Step 1: Calculate sum of all transaction effects on this account
                var transactionEffect = 0.0
                
                for (transaction in transactions) {
                    when (transaction.type) {
                        TransactionType.EXPENSE -> {
                            if (transaction.fromAccountId == account.id) {
                                transactionEffect -= transaction.amount
                            }
                        }
                        TransactionType.INCOME -> {
                            if (transaction.fromAccountId == account.id) {
                                transactionEffect += transaction.amount
                            }
                        }
                        TransactionType.TRANSFER -> {
                            if (transaction.fromAccountId == account.id) {
                                transactionEffect -= transaction.amount
                            }
                            if (transaction.toAccountId == account.id) {
                                transactionEffect += transaction.amount
                            }
                        }
                    }
                }
                
                // Step 2: Calculate correct balance = initial balance + transaction effects
                val correctBalance = account.initialBalance + transactionEffect
                
                android.util.Log.d("ExpenseRepository", "Account ${account.name}: initial=${account.initialBalance}, transaction_effect=${transactionEffect}, current=${account.balance}, correct=${correctBalance}")
                
                // Update account if balance is incorrect
                if (account.balance != correctBalance) {
                    android.util.Log.d("ExpenseRepository", "Correcting balance for ${account.name}: ${account.balance} → ${correctBalance}")
                    val correctedAccount = account.copy(balance = correctBalance)
                    accountDao.updateAccount(correctedAccount)
                } else {
                    android.util.Log.d("ExpenseRepository", "Account ${account.name} balance is correct")
                }
            }
            
            android.util.Log.d("ExpenseRepository", "Balance recalculation completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "Balance recalculation failed: ${e.message}", e)
            throw e
        }
    }
}
