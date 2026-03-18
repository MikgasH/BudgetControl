package com.example.budgetcontrol.core.di

import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.domain.usecase.AddExpenseUseCase
import com.example.budgetcontrol.core.domain.usecase.AddIncomeUseCase
import com.example.budgetcontrol.core.domain.usecase.ConvertCurrencyUseCase
import com.example.budgetcontrol.core.domain.usecase.DeleteExpenseUseCase
import com.example.budgetcontrol.core.domain.usecase.DeleteIncomeUseCase
import com.example.budgetcontrol.core.domain.usecase.GetCategoriesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideConvertCurrencyUseCase(
        cerpsRepository: CerpsRepository,
        preferencesManager: PreferencesManager
    ): ConvertCurrencyUseCase {
        return ConvertCurrencyUseCase(cerpsRepository, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideAddExpenseUseCase(
        @ApplicationContext context: Context,
        repository: ExpenseRepository,
        convertCurrencyUseCase: ConvertCurrencyUseCase,
        categoryRepository: CategoryRepository
    ): AddExpenseUseCase {
        return AddExpenseUseCase(context, repository, convertCurrencyUseCase, categoryRepository)
    }

    @Provides
    @Singleton
    fun provideGetExpensesUseCase(
        repository: ExpenseRepository
    ): GetExpensesUseCase {
        return GetExpensesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetCategoriesUseCase(
        repository: CategoryRepository
    ): GetCategoriesUseCase {
        return GetCategoriesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteExpenseUseCase(
        repository: ExpenseRepository
    ): DeleteExpenseUseCase {
        return DeleteExpenseUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideAddIncomeUseCase(
        @ApplicationContext context: Context,
        repository: IncomeRepository,
        convertCurrencyUseCase: ConvertCurrencyUseCase,
        categoryRepository: CategoryRepository
    ): AddIncomeUseCase {
        return AddIncomeUseCase(context, repository, convertCurrencyUseCase, categoryRepository)
    }

    @Provides
    @Singleton
    fun provideGetIncomesUseCase(
        repository: IncomeRepository
    ): GetIncomesUseCase {
        return GetIncomesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteIncomeUseCase(
        repository: IncomeRepository
    ): DeleteIncomeUseCase {
        return DeleteIncomeUseCase(repository)
    }

}