package com.example.budgetcontrol.core.data.remote

import android.content.Context
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.CerpsAnalyticsApiService
import com.example.budgetcontrol.core.data.remote.cerps.CerpsApiService
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.data.remote.cerps.dto.RatesResponse
import com.example.budgetcontrol.core.data.remote.gemini.GeminiResult
import com.example.budgetcontrol.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CerpsRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mockk(relaxed = true)
    private val apiService: CerpsApiService = mockk()
    private val analyticsApiService: CerpsAnalyticsApiService = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private lateinit var applicationScope: CoroutineScope

    @Before
    fun setup() {
        // UnconfinedTestDispatcher so the init{} hydration launch completes eagerly
        applicationScope = TestScope(UnconfinedTestDispatcher())
        // Default: DataStore returns empty for all reads; tests override as needed
        coEvery { preferencesManager.getLastRates() } returns flowOf(emptyMap())
        coEvery { preferencesManager.getLastRatesTimestamp() } returns flowOf(0L)
        coEvery { preferencesManager.getAvailableCurrencies() } returns flowOf(emptyList())
        coEvery { preferencesManager.saveAvailableCurrencies(any()) } just Runs
        coEvery { preferencesManager.saveLastRates(any(), any()) } just Runs
    }

    private fun buildRepo(): CerpsRepository = CerpsRepository(
        context = context,
        apiService = apiService,
        analyticsApiService = analyticsApiService,
        preferencesManager = preferencesManager,
        applicationScope = applicationScope
    )

    @Test
    fun `getCurrencies falls back to DataStore on 500 and caches in-memory`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaTypeOrNull())
        coEvery { apiService.getCurrencies() } returns Response.error(500, errorBody)
        coEvery { preferencesManager.getAvailableCurrencies() } returns flowOf(listOf("EUR", "PLN"))

        val repo = buildRepo()

        val first = repo.getCurrencies()
        assertTrue(first is CerpsResult.Success)
        assertEquals(listOf("EUR", "PLN"), (first as CerpsResult.Success).data)

        val second = repo.getCurrencies()
        assertTrue(second is CerpsResult.Success)
        assertEquals(listOf("EUR", "PLN"), (second as CerpsResult.Success).data)
        // In-memory cache — apiService only invoked on the first call
        coVerify(exactly = 1) { apiService.getCurrencies() }
    }

    @Test
    fun `ensureRatesLoaded falls back to DataStore when API throws IOException`() = runTest {
        coEvery { apiService.getCurrentRates(any()) } throws IOException("offline")
        coEvery { preferencesManager.getLastRates() } returns flowOf(mapOf("PLN" to 4.5))
        coEvery { preferencesManager.getLastRatesTimestamp() } returns flowOf(1L)

        val repo = buildRepo()

        val result = repo.ensureRatesLoaded()
        assertTrue(result is CerpsResult.Success)
        assertEquals(mapOf("PLN" to 4.5), (result as CerpsResult.Success).data)
    }

    @Test
    fun `ensureRatesLoaded returns Error when both API and DataStore fail`() = runTest {
        coEvery { apiService.getCurrentRates(any()) } throws IOException("offline")
        coEvery { preferencesManager.getLastRates() } returns flowOf(emptyMap())
        coEvery { preferencesManager.getLastRatesTimestamp() } returns flowOf(0L)

        val repo = buildRepo()

        val result = repo.ensureRatesLoaded()
        assertTrue(result is CerpsResult.Error)
    }

    @Test
    fun `ensureRatesLoaded returns Success from API when available`() = runTest {
        val rates = mapOf("USD" to 1.08, "PLN" to 4.30)
        coEvery { apiService.getCurrentRates(any()) } returns Response.success(
            RatesResponse(base = "EUR", rates = rates, timestamp = null)
        )

        val repo = buildRepo()

        val result = repo.ensureRatesLoaded()
        assertTrue(result is CerpsResult.Success)
        assertEquals(rates, (result as CerpsResult.Success).data)
    }

    @Test
    fun `getBankCommission returns Success when found`() = runTest {
        coEvery {
            apiService.getBankCommission(any())
        } returns Response.success(
            com.example.budgetcontrol.core.data.remote.cerps.BankCommissionResponse(
                commission = 1.5,
                found = true
            )
        )

        val repo = buildRepo()
        val result = repo.getBankCommission("Revolut")

        assertTrue(result is GeminiResult.Success)
        assertEquals(1.5, (result as GeminiResult.Success).commission, 0.0)
    }

    @Test
    fun `getBankCommission returns NotFound when found is false`() = runTest {
        coEvery {
            apiService.getBankCommission(any())
        } returns Response.success(
            com.example.budgetcontrol.core.data.remote.cerps.BankCommissionResponse(
                commission = null,
                found = false
            )
        )

        val repo = buildRepo()
        val result = repo.getBankCommission("Unknown Bank")

        assertTrue(result is GeminiResult.NotFound)
    }

    @Test
    fun `getBankCommission returns Error when API throws`() = runTest {
        coEvery { apiService.getBankCommission(any()) } throws IOException("network down")

        val repo = buildRepo()
        val result = repo.getBankCommission("Revolut")

        assertTrue(result is GeminiResult.Error)
    }
}
