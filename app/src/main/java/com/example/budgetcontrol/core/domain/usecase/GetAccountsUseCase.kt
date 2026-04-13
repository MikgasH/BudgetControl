package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.model.AccountGroup
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class AccountWithBalance(
    val account: Account,
    val currentBalance: Double  // in account's own currency
)

data class AccountGroupWithBalance(
    val group: AccountGroup,
    val combinedBalance: Double,
    val memberCount: Int
)

class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val preferencesManager: PreferencesManager
) {
    operator fun invoke(): Flow<List<Account>> {
        return accountRepository.getAllAccounts()
    }

    fun getAccountsWithBalances(): Flow<List<AccountWithBalance>> {
        return combine(
            accountRepository.getAllAccounts(),
            expenseRepository.getAllExpenses(),
            incomeRepository.getAllIncomes(),
            preferencesManager.baseCurrencyFlow
        ) { accounts, expenses, incomes, baseCurrency ->
            val expensesByAccount = expenses.groupBy { it.accountId ?: Account.DEFAULT_ACCOUNT_ID }
            val incomesByAccount = incomes.groupBy { it.accountId ?: Account.DEFAULT_ACCOUNT_ID }

            accounts.map { account ->
                val accountExpenses = expensesByAccount[account.id] ?: emptyList()
                val accountIncomes = incomesByAccount[account.id] ?: emptyList()

                // For accounts in their own currency, sum originalAmount
                // (the amount in the currency the transaction was recorded in).
                // For base-currency accounts, sum amount (already in base currency).
                val totalExpenses = if (account.currency != baseCurrency) {
                    accountExpenses.sumOf { it.originalAmount }
                } else {
                    accountExpenses.sumOf { it.amount }
                }
                val totalIncomes = if (account.currency != baseCurrency) {
                    accountIncomes.sumOf { it.originalAmount }
                } else {
                    accountIncomes.sumOf { it.amount }
                }

                AccountWithBalance(
                    account = account,
                    currentBalance = account.initialBalance + totalIncomes - totalExpenses
                )
            }
        }
    }

    fun searchAccounts(query: String): Flow<List<Account>> {
        return accountRepository.searchAccounts(query)
    }

    suspend fun getAccountById(id: String): Account? {
        return accountRepository.getAccountById(id)
    }
}
