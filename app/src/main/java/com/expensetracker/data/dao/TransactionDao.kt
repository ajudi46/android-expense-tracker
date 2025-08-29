package com.expensetracker.data.dao

import androidx.room.*
import com.expensetracker.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE fromAccountId = :accountId OR toAccountId = :accountId ORDER BY createdAt DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE strftime('%m', datetime(createdAt / 1000, 'unixepoch')) = printf('%02d', :month)
        AND strftime('%Y', datetime(createdAt / 1000, 'unixepoch')) = :year
        ORDER BY createdAt DESC
    """)
    fun getTransactionsForMonth(month: Int, year: String): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE category = :category
        AND strftime('%m', datetime(createdAt / 1000, 'unixepoch')) = printf('%02d', :month)
        AND strftime('%Y', datetime(createdAt / 1000, 'unixepoch')) = :year
        ORDER BY createdAt DESC
    """)
    fun getTransactionsForCategoryAndMonth(category: String, month: Int, year: String): Flow<List<Transaction>>
}
