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
        
        // Update account balances based on transaction type
        when (transaction.type) {
            TransactionType.EXPENSE -> {
                accountDao.updateBalance(transaction.fromAccountId, -transaction.amount)
                // Update budget for expense transactions
                updateBudgetSpending(transaction)
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
    
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)

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
}
