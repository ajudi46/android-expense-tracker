package com.expensetracker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.expensetracker.data.dao.AccountDao
import com.expensetracker.data.dao.BudgetDao
import com.expensetracker.data.dao.CategoryDao
import com.expensetracker.data.dao.TransactionDao
import com.expensetracker.data.dao.UserDao
import com.expensetracker.data.model.Account
import com.expensetracker.data.model.Budget
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Transaction
import com.expensetracker.data.model.User

@Database(
    entities = [Account::class, Transaction::class, Category::class, Budget::class, User::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        limitAmount REAL NOT NULL,
                        currentSpent REAL NOT NULL DEFAULT 0.0,
                        month INTEGER NOT NULL,
                        year INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        uid TEXT PRIMARY KEY NOT NULL,
                        email TEXT NOT NULL,
                        displayName TEXT,
                        photoUrl TEXT,
                        isSignedIn INTEGER NOT NULL DEFAULT 0,
                        lastSyncTimestamp INTEGER NOT NULL DEFAULT 0,
                        encryptionKey TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add initialBalance column to accounts table
                database.execSQL(
                    "ALTER TABLE accounts ADD COLUMN initialBalance REAL NOT NULL DEFAULT 0.0"
                )
                // Set initialBalance to current balance for existing accounts
                database.execSQL(
                    "UPDATE accounts SET initialBalance = balance"
                )
            }
        }

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
