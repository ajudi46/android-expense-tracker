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
            
            // Check if budget exists for this category in this month
            val existingBudget = getBudgetForCategory(transaction.category, month, year)
            existingBudget?.let { budget ->
                val newSpentAmount = budget.currentSpent + transaction.amount
                updateBudgetSpent(transaction.category, month, year, newSpentAmount)
            }
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
}
