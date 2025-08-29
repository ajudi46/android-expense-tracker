package com.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.expensetracker.data.dao.AccountDao
import com.expensetracker.data.dao.CategoryDao
import com.expensetracker.data.dao.TransactionDao
import com.expensetracker.data.database.ExpenseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExpenseDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ExpenseDatabase::class.java,
            "expense_database"
        ).build()
    }

    @Provides
    fun provideAccountDao(database: ExpenseDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    fun provideTransactionDao(database: ExpenseDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: ExpenseDatabase): CategoryDao {
        return database.categoryDao()
    }
}
