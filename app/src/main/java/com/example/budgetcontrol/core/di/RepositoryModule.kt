package com.example.budgetcontrol.core.di

import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.repository.BankRepositoryImpl
import com.example.budgetcontrol.core.data.repository.CategoryRepositoryImpl
import com.example.budgetcontrol.core.data.repository.CurrencyExchangeRepositoryImpl
import com.example.budgetcontrol.core.data.repository.ExpenseRepositoryImpl
import com.example.budgetcontrol.core.data.repository.IncomeRepositoryImpl
import com.example.budgetcontrol.core.domain.repository.BankRepository
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.CurrencyExchangeRepository
import com.example.budgetcontrol.core.domain.repository.CurrencyRateProvider
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
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
    abstract fun bindCurrencyRateProvider(
        cerpsRepository: CerpsRepository
    ): CurrencyRateProvider
}