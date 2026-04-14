package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import javax.inject.Inject

class UpdateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase
) {
    sealed class Result {
        data class Success(val account: Account) : Result()
        // Currency change rejected because the account already has transactions.
        // UI must not offer the option in this case; returning it here is the
        // authoritative guard so tests and non-UI callers can't slip through.
        object CurrencyChangeBlocked : Result()
        data class ConversionFailed(val message: String) : Result()
    }

    suspend operator fun invoke(
        existing: Account,
        name: String,
        iconName: String,
        color: String,
        initialBalance: Double,
        newCurrency: String
    ): Result {
        val currencyChanged = existing.currency != newCurrency

        if (currencyChanged) {
            val txnCount = expenseRepository.getExpenseCountByAccount(existing.id) +
                incomeRepository.getIncomeCountByAccount(existing.id)
            if (txnCount > 0) return Result.CurrencyChangeBlocked
        }

        val finalBalance = if (currencyChanged) {
            when (val conv = convertCurrencyUseCase(initialBalance, existing.currency, newCurrency)) {
                is ConvertCurrencyResult.Success -> conv.conversion.convertedAmount
                is ConvertCurrencyResult.Error -> return Result.ConversionFailed(conv.message)
            }
        } else {
            initialBalance
        }

        val updated = existing.copy(
            name = name,
            iconName = iconName,
            color = color,
            initialBalance = finalBalance,
            currency = newCurrency
        )
        accountRepository.updateAccount(updated)
        return Result.Success(updated)
    }

    suspend fun previewConversion(
        fromCurrency: String,
        toCurrency: String,
        amount: Double
    ): ConvertCurrencyResult = convertCurrencyUseCase(amount, fromCurrency, toCurrency)
}
