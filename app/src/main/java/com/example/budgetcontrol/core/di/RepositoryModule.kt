package com.example.budgetcontrol.core.di

import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.repository.AccountGroupRepositoryImpl
import com.example.budgetcontrol.core.data.repository.AccountRepositoryImpl
import com.example.budgetcontrol.core.data.repository.BankRepositoryImpl
import com.example.budgetcontrol.core.data.repository.CategoryLimitRepositoryImpl
import com.example.budgetcontrol.core.data.repository.CategoryRepositoryImpl
import com.example.budgetcontrol.core.data.repository.CurrencyExchangeRepositoryImpl
import com.example.budgetcontrol.core.data.repository.ExpenseRepositoryImpl
import com.example.budgetcontrol.core.data.repository.IncomeRepositoryImpl
import com.example.budgetcontrol.core.data.repository.TransactionRepositoryImpl
import com.example.budgetcontrol.core.domain.repository.AccountGroupRepository
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.domain.repository.BankRepository
import com.example.budgetcontrol.core.domain.repository.CategoryLimitRepository
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.CurrencyExchangeRepository
import com.example.budgetcontrol.core.domain.repository.CurrencyRateProvider
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        expenseRepositoryImpl: ExpenseRepositoryImpl
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindIncomeRepository(
        incomeRepositoryImpl: IncomeRepositoryImpl
    ): IncomeRepository

    @Binds
    @Singleton
    abstract fun bindCurrencyExchangeRepository(
        currencyExchangeRepositoryImpl: CurrencyExchangeRepositoryImpl
    ): CurrencyExchangeRepository

    @Binds
    @Singleton
    abstract fun bindBankRepository(
        bankRepositoryImpl: BankRepositoryImpl
    ): BankRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        accountRepositoryImpl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindAccountGroupRepository(
        accountGroupRepositoryImpl: AccountGroupRepositoryImpl
    ): AccountGroupRepository

    @Binds
    @Singleton
    abstract fun bindCurrencyRateProvider(
        cerpsRepository: CerpsRepository
    ): CurrencyRateProvider

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryLimitRepository(
        categoryLimitRepositoryImpl: CategoryLimitRepositoryImpl
    ): CategoryLimitRepository
}