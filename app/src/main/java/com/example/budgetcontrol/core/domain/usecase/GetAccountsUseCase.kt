package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class AccountWithBalance(
    val account: Account,
    val currentBalance: Double
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
                .mapValues { (_, list) -> list.sumOf { it.amount } }
            val incomesByAccount = incomes.groupBy { it.accountId ?: Account.DEFAULT_ACCOUNT_ID }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            accounts.map { account ->
                val totalExpenses = expensesByAccount[account.id] ?: 0.0
                val totalIncomes = incomesByAccount[account.id] ?: 0.0
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
