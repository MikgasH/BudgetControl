package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.model.AccountGroup
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

data class AccountWithBalance(
    val account: Account,
    val currentBalance: Double,  // in account's own currency
    // Balance expressed in the user's base currency. Summed from per-transaction `amount`
    // (already in base currency at entry time) plus the initial balance converted via rates.
    // Approximate when the initial balance cannot be converted (rate missing) — in that
    // case we keep `initialBalance` as-is rather than poisoning the sum with null.
    val baseCurrencyBalance: Double = currentBalance,
    // False when this account is in a non-base currency and CERPS has no rate for it —
    // callers combining across currencies use this to distinguish "accurate cross-currency
    // sum" from "best-effort sum with missing rates".
    val baseConversionAvailable: Boolean = true
)

data class AccountGroupWithBalance(
    val group: AccountGroup,
    // Sum of member `baseCurrencyBalance` values — always well-defined. The underlying
    // per-account base balance is an approximation when rates are missing for the account's
    // initial balance, which should be surfaced to the user via an "~" prefix.
    val combinedBalance: Double,
    val memberCount: Int,
    // True when this is a mixed-currency group and at least one member lacks an exchange
    // rate to base currency. UI should render "—" rather than a silently-wrong sum.
    val ratesUnavailable: Boolean = false
)

class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) {
    operator fun invoke(): Flow<List<Account>> {
        return accountRepository.getAllAccounts()
    }

    fun getAccountsWithBalances(
        baseCurrencyFlow: Flow<String> = flowOf(DEFAULT_BASE_CURRENCY),
        ratesFlow: Flow<Map<String, Double>> = flowOf(emptyMap())
    ): Flow<List<AccountWithBalance>> {
        return combine(
            accountRepository.getAllAccounts(),
            expenseRepository.getAllExpenses(),
            incomeRepository.getAllIncomes(),
            baseCurrencyFlow,
            ratesFlow
        ) { accounts, expenses, incomes, baseCurrency, rates ->
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

                // Base-currency balance uses `tx.amount` directly — it's already in base
                // currency (converted at entry time), so no lookup in `rates` is needed for
                // the transactional part. This avoids the "—" hole we otherwise hit when
                // CERPS lacks a rate for an obscure currency (e.g. BYN): even if the account's
                // initial balance can't be re-converted today, the transactions carry their own
                // base amounts and still produce a meaningful group total.
                val totalExpensesBase = accountExpenses.sumOf { it.amount }
                val totalIncomesBase = accountIncomes.sumOf { it.amount }
                val needsConversion = account.currency != baseCurrency
                val convertedInitialOrNull = if (needsConversion) {
                    crossConvert(account.initialBalance, account.currency, baseCurrency, rates)
                } else {
                    account.initialBalance
                }
                val convertedInitial = convertedInitialOrNull ?: account.initialBalance
                val baseConversionAvailable = !needsConversion || convertedInitialOrNull != null

                AccountWithBalance(
                    account = account,
                    currentBalance = account.initialBalance + totalIncomes - totalExpenses,
                    baseCurrencyBalance = convertedInitial + totalIncomesBase - totalExpensesBase,
                    baseConversionAvailable = baseConversionAvailable
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
