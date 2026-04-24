package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.RateSource
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.CurrencyRateProvider
import com.example.budgetcontrol.core.domain.repository.CurrencyRateResult
import com.example.budgetcontrol.core.domain.repository.TransactionRepository
import com.example.budgetcontrol.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transactionRepository: TransactionRepository = mockk()
    private val rateProvider: CurrencyRateProvider = mockk()
    private val categoryRepository: CategoryRepository = mockk()
    private lateinit var convertCurrencyUseCase: ConvertCurrencyUseCase
    private lateinit var useCase: AddTransactionUseCase

    @Before
    fun setup() {
        convertCurrencyUseCase = ConvertCurrencyUseCase(rateProvider)
        useCase = AddTransactionUseCase(
            transactionRepository,
            convertCurrencyUseCase,
            categoryRepository
        )
        coEvery { transactionRepository.insert(any()) } just Runs
        coEvery { categoryRepository.incrementUsageCount(any()) } just Runs
    }

    @Test
    fun `HOME_CURRENCY path - bank info is stripped and rateSource is HOME_CURRENCY`() = runTest {
        // When from == to the use case short-circuits without touching rates
        val inserted = slot<Transaction>()
        coEvery { transactionRepository.insert(capture(inserted)) } just Runs

        val result = useCase(
            type = TransactionType.EXPENSE,
            amount = 100.0,
            currency = "PLN",
            categoryId = "cat-1",
            description = null,
            bankName = "Revolut",
            bankCommission = 0.5,
            baseCurrency = "PLN",
            accountCurrency = "PLN"
        )

        assertTrue(result is AddTransactionResult.Success)
        val tx = inserted.captured as Transaction.ExpenseTransaction
        assertNull(tx.bankName)
        assertNull(tx.bankCommission)
        assertEquals(RateSource.HOME_CURRENCY.name, tx.rateSource)
        coVerify(exactly = 1) { categoryRepository.incrementUsageCount("cat-1") }
    }

    @Test
    fun `foreign currency path - preserves bank info and rateSource from conversion`() = runTest {
        coEvery { rateProvider.getRates() } returns CurrencyRateResult.Success(mapOf("PLN" to 4.50))
        every { rateProvider.areRatesStale() } returns true // forces rateSource = CACHED_RATE

        val inserted = slot<Transaction>()
        coEvery { transactionRepository.insert(capture(inserted)) } just Runs

        val result = useCase(
            type = TransactionType.EXPENSE,
            amount = 100.0,
            currency = "PLN",
            categoryId = "cat-1",
            description = null,
            bankName = "Revolut",
            bankCommission = 0.5,
            baseCurrency = "EUR",
            accountCurrency = "EUR"
        )

        assertTrue(result is AddTransactionResult.Success)
        val tx = inserted.captured as Transaction.ExpenseTransaction
        assertEquals("Revolut", tx.bankName)
        assertEquals(0.5, tx.bankCommission!!, 0.0)
        assertEquals(RateSource.CACHED_RATE.name, tx.rateSource)
    }

    @Test
    fun `conversion failure - transactionRepository insert is never called`() = runTest {
        coEvery { rateProvider.getRates() } returns CurrencyRateResult.Error("boom")

        val result = useCase(
            type = TransactionType.EXPENSE,
            amount = 100.0,
            currency = "PLN",
            categoryId = "cat-1",
            description = null,
            baseCurrency = "EUR",
            accountCurrency = "EUR"
        )

        assertTrue(result is AddTransactionResult.Error)
        assertTrue((result as AddTransactionResult.Error).error is AddTransactionError.ConversionFailed)
        coVerify(exactly = 0) { transactionRepository.insert(any()) }
        coVerify(exactly = 0) { categoryRepository.incrementUsageCount(any()) }
    }

    @Test
    fun `addWithExactBaseAmount - zero exactBaseAmount returns InvalidAmount Error`() = runTest {
        val result = useCase.addWithExactBaseAmount(
            type = TransactionType.EXPENSE,
            originalAmount = 100.0,
            originalCurrency = "PLN",
            exactBaseAmount = 0.0,
            categoryId = "cat-1",
            description = null
        )

        assertTrue(result is AddTransactionResult.Error)
        assertTrue((result as AddTransactionResult.Error).error is AddTransactionError.InvalidAmount)
        coVerify(exactly = 0) { transactionRepository.insert(any()) }
    }

    @Test
    fun `addWithExactBaseAmount - valid inputs produce USER_CORRECTED with derived exchangeRate`() = runTest {
        val inserted = slot<Transaction>()
        coEvery { transactionRepository.insert(capture(inserted)) } just Runs

        val result = useCase.addWithExactBaseAmount(
            type = TransactionType.EXPENSE,
            originalAmount = 100.0,
            originalCurrency = "PLN",
            exactBaseAmount = 22.50,
            categoryId = "cat-1",
            description = null
        )

        assertTrue(result is AddTransactionResult.Success)
        val tx = inserted.captured as Transaction.ExpenseTransaction
        assertEquals(RateSource.USER_CORRECTED.name, tx.rateSource)
        // 100 / 22.50 ≈ 4.4444
        assertEquals(4.4444, tx.exchangeRate!!, 0.0001)
        assertEquals(22.50, tx.amount, 0.0)
        assertEquals(100.0, tx.originalAmount, 0.0)
    }
}
