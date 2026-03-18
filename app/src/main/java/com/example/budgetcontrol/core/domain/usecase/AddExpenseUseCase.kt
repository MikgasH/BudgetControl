package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import java.util.UUID
import javax.inject.Inject

sealed class AddExpenseResult {
    object Success : AddExpenseResult()

    data class Error(val error: AddTransactionError) : AddExpenseResult()
}

class AddExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        amount: Double,
        currency: String = "EUR",
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis(),
        bankName: String? = null,
        bankCommission: Double? = null
    ): AddExpenseResult {

        return try {
            val conversionResult = convertCurrencyUseCase(amount, currency)
            if (conversionResult is ConvertCurrencyResult.Error) {
                return AddExpenseResult.Error(AddTransactionError.ConversionFailed(conversionResult.message))
            }
            val conversion = (conversionResult as ConvertCurrencyResult.Success).conversion

            val expense = Expense(
                id = UUID.randomUUID().toString(),
                amount = conversion.convertedAmount,
                categoryId = categoryId,
                description = description,
                date = date,
                createdAt = System.currentTimeMillis(),
                originalAmount = amount,
                originalCurrency = currency,
                exchangeRate = conversion.exchangeRate,
                bankName = bankName,
                bankCommission = bankCommission,
                rateSource = conversion.rateSource
            )

            repository.insertExpense(expense)
            categoryRepository.incrementUsageCount(categoryId)
            AddExpenseResult.Success

        } catch (e: Exception) {
            AddExpenseResult.Error(AddTransactionError.SavingFailed(e.message))
        }
    }

    suspend fun addInEur(
        amount: Double,
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis()
    ): AddExpenseResult {
        return invoke(amount, "EUR", categoryId, description, date)
    }

    /**
     * Save an expense where the user manually specified the exact EUR amount
     * (e.g. taken directly from their banking app). Skips CERPS conversion entirely.
     */
    suspend fun addWithExactEurAmount(
        originalAmount: Double,
        originalCurrency: String,
        exactEurAmount: Double,
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis(),
        bankName: String? = null,
        bankCommission: Double? = null,
        rateSource: String = "USER_CORRECTED"
    ): AddExpenseResult {
        return try {
            val expense = Expense(
                id = UUID.randomUUID().toString(),
                amount = exactEurAmount,
                categoryId = categoryId,
                description = description,
                date = date,
                createdAt = System.currentTimeMillis(),
                originalAmount = originalAmount,
                originalCurrency = originalCurrency,
                exchangeRate = if (originalAmount > 0) originalAmount / exactEurAmount else null,
                bankName = bankName,
                bankCommission = bankCommission,
                rateSource = rateSource
            )
            repository.insertExpense(expense)
            categoryRepository.incrementUsageCount(categoryId)
            AddExpenseResult.Success
        } catch (e: Exception) {
            AddExpenseResult.Error(AddTransactionError.SavingFailed(e.message))
        }
    }
}
