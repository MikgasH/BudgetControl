package com.example.budgetcontrol.di

import android.content.Context
import androidx.room.Room
import com.example.budgetcontrol.core.data.local.database.AppDatabase
import com.example.budgetcontrol.core.data.local.database.dao.AccountDao
import com.example.budgetcontrol.core.data.local.database.dao.AccountGroupDao
import com.example.budgetcontrol.core.data.local.database.dao.BankDao
import com.example.budgetcontrol.core.data.local.database.dao.CategoryDao
import com.example.budgetcontrol.core.data.local.database.dao.CategoryLimitDao
import com.example.budgetcontrol.core.data.local.database.dao.CurrencyExchangeDao
import com.example.budgetcontrol.core.data.local.database.dao.ExpenseDao
import com.example.budgetcontrol.core.data.local.database.dao.IncomeDao
import com.example.budgetcontrol.core.di.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.PREPOPULATE_CALLBACK)
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideIncomeDao(database: AppDatabase): IncomeDao = database.incomeDao()

    @Provides
    fun provideBankDao(database: AppDatabase): BankDao = database.bankDao()

    @Provides
    fun provideCurrencyExchangeDao(database: AppDatabase): CurrencyExchangeDao =
        database.currencyExchangeDao()

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideAccountGroupDao(database: AppDatabase): AccountGroupDao = database.accountGroupDao()

    @Provides
    fun provideCategoryLimitDao(database: AppDatabase): CategoryLimitDao =
        database.categoryLimitDao()
}
