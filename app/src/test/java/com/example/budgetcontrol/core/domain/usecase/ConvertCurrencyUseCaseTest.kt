package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.RateSource
import com.example.budgetcontrol.core.domain.repository.CurrencyRateProvider
import com.example.budgetcontrol.core.domain.repository.CurrencyRateResult
import com.example.budgetcontrol.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConvertCurrencyUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rateProvider: CurrencyRateProvider = mockk()
    private val useCase = ConvertCurrencyUseCase(rateProvider)

    @Test
    fun `cross-currency conversion PLN to EUR returns converted amount with null rateSource`() = runTest {
        coEvery { rateProvider.getRates() } returns CurrencyRateResult.Success(mapOf("PLN" to 4.50))
        every { rateProvider.areRatesStale() } returns false

        val result = useCase(amount = 100.0, fromCurrency = "PLN", toCurrency = "EUR")

        assertTrue(result is ConvertCurrencyResult.Success)
        val conversion = (result as ConvertCurrencyResult.Success).conversion
        // 100 * 1.0 / 4.50 ≈ 22.2222
        assertEquals(22.2222, conversion.convertedAmount, 0.0001)
        assertNull(conversion.rateSource)
    }

    @Test
    fun `same currency passthrough returns original amount without calling getRates`() = runTest {
        val result = useCase(amount = 100.0, fromCurrency = "EUR", toCurrency = "EUR")

        assertTrue(result is ConvertCurrencyResult.Success)
        val conversion = (result as ConvertCurrencyResult.Success).conversion
        assertEquals(100.0, conversion.convertedAmount, 0.0)
        assertNull(conversion.exchangeRate)
        assertNull(conversion.rateSource)
        coVerify(exactly = 0) { rateProvider.getRates() }
    }

    @Test
    fun `stale rates tag CACHED_RATE is set when areRatesStale returns true`() = runTest {
        coEvery { rateProvider.getRates() } returns CurrencyRateResult.Success(mapOf("PLN" to 4.50))
        every { rateProvider.areRatesStale() } returns true

        val result = useCase(amount = 100.0, fromCurrency = "PLN", toCurrency = "EUR")

        assertTrue(result is ConvertCurrencyResult.Success)
        val conversion = (result as ConvertCurrencyResult.Success).conversion
        assertEquals(RateSource.CACHED_RATE.name, conversion.rateSource)
    }

    @Test
    fun `missing rate for target currency returns Error`() = runTest {
        coEvery { rateProvider.getRates() } returns CurrencyRateResult.Success(mapOf("PLN" to 4.50))
        every { rateProvider.areRatesStale() } returns false

        val result = useCase(amount = 100.0, fromCurrency = "PLN", toCurrency = "USD")

        assertTrue(result is ConvertCurrencyResult.Error)
    }
}
