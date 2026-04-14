package com.example.budgetcontrol.core.domain.usecase

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
    // null when any member account's currency cannot be converted to base (missing rates)
    val combinedBalance: Double?,
    val memberCount: Int
)

class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) {
    operator fun invoke(): Flow<List<Account>> {
        return accountRepository.getAllAccounts()
    }

    fun getAccountsWithBalances(): Flow<List<AccountWithBalance>> {
        return combine(
            accountRepository.getAllAccounts(),
            expenseRepository.getAllExpenses(),
            incomeRepository.getAllIncomes()
        ) { accounts, expenses, incomes ->
            val expensesByAccount = expenses.groupBy { it.accountId ?: Account.DEFAULT_ACCOUNT_ID }
            val incomesByAccount = incomes.groupBy { it.accountId ?: Account.DEFAULT_ACCOUNT_ID }

            accounts.map { account ->
                val accountExpenses = expensesByAccount[account.id] ?: emptyList()
                val accountIncomes = incomesByAccount[account.id] ?: emptyList()

                // Sum each transaction in its own originalCurrency when it matches the account;
                // otherwise fall back to the base-currency `amount`. Keying off the account's
                // current currency would misread transactions recorded under a different
                // currency (e.g. after a prior currency change, or mixed-currency entries).
                val totalExpenses = accountExpenses.sumOf { tx ->
                    if (tx.originalCurrency == account.currency) tx.originalAmount else tx.amount
                }
                val totalIncomes = accountIncomes.sumOf { tx ->
                    if (tx.originalCurrency == account.currency) tx.originalAmount else tx.amount
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
