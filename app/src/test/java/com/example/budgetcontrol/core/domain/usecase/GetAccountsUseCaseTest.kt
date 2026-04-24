package com.example.budgetcontrol.core.domain.usecase

import app.cash.turbine.test
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.util.MainDispatcherRule
import com.example.budgetcontrol.util.TestFixtures
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetAccountsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountRepository: AccountRepository = mockk()
    private val expenseRepository: ExpenseRepository = mockk()
    private val incomeRepository: IncomeRepository = mockk()
    private lateinit var useCase: GetAccountsUseCase

    @Before
    fun setup() {
        useCase = GetAccountsUseCase(accountRepository, expenseRepository, incomeRepository)
    }

    @Test
    fun `single base-currency EUR account - balance equals initial plus income minus expense`() = runTest {
        val account = TestFixtures.account(
            id = "acc-eur",
            currency = "EUR",
            initialBalance = 100.0
        )
        val income = TestFixtures.income(
            id = "inc-1",
            amount = 50.0,
            originalAmount = 50.0,
            originalCurrency = "EUR",
            accountId = "acc-eur"
        )
        val expense = TestFixtures.expense(
            id = "exp-1",
            amount = 20.0,
            originalAmount = 20.0,
            originalCurrency = "EUR",
            accountId = "acc-eur"
        )

        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account))
        every { expenseRepository.getAllExpenses() } returns flowOf(listOf(expense))
        every { incomeRepository.getAllIncomes() } returns flowOf(listOf(income))

        useCase.getAccountsWithBalances(
            baseCurrencyFlow = flowOf("EUR"),
            ratesFlow = flowOf(emptyMap())
        ).test {
            val balances = awaitItem()
            assertEquals(1, balances.size)
            val row = balances[0]
            assertEquals(130.0, row.currentBalance, 0.0)
            assertEquals(130.0, row.baseCurrencyBalance, 0.0)
            assertTrue(row.baseConversionAvailable)
            awaitComplete()
        }
    }

    @Test
    fun `foreign PLN account - current balance in PLN, base conversion available`() = runTest {
        val account = TestFixtures.account(
            id = "acc-pln",
            currency = "PLN",
            initialBalance = 1000.0
        )
        // Expense recorded natively in PLN — account's own currency
        val expense = TestFixtures.expense(
            id = "exp-1",
            amount = 22.22, // base EUR equivalent at entry time
            originalAmount = 100.0,
            originalCurrency = "PLN",
            accountId = "acc-pln"
        )

        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account))
        every { expenseRepository.getAllExpenses() } returns flowOf(listOf(expense))
        every { incomeRepository.getAllIncomes() } returns flowOf(emptyList())

        useCase.getAccountsWithBalances(
            baseCurrencyFlow = flowOf("EUR"),
            ratesFlow = flowOf(mapOf("PLN" to 4.50))
        ).test {
            val balances = awaitItem()
            assertEquals(1, balances.size)
            val row = balances[0]
            // Native PLN balance: 1000 - 100 = 900
            assertEquals(900.0, row.currentBalance, 0.0)
            assertTrue(row.baseConversionAvailable)
            awaitComplete()
        }
    }

    @Test
    fun `missing rate - BYN account with empty rates reports baseConversionAvailable false without crashing`() = runTest {
        val account = TestFixtures.account(
            id = "acc-byn",
            currency = "BYN",
            initialBalance = 500.0
        )

        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account))
        every { expenseRepository.getAllExpenses() } returns flowOf(emptyList())
        every { incomeRepository.getAllIncomes() } returns flowOf(emptyList())

        useCase.getAccountsWithBalances(
            baseCurrencyFlow = flowOf("EUR"),
            ratesFlow = flowOf(emptyMap())
        ).test {
            val balances = awaitItem()
            val row = balances.single()
            assertFalse(row.baseConversionAvailable)
            // No NaN — base balance falls back to raw initial
            assertFalse(row.baseCurrencyBalance.isNaN())
            assertEquals(500.0, row.baseCurrencyBalance, 0.0)
            awaitComplete()
        }
    }
}
