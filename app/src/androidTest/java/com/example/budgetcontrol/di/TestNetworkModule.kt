package com.example.budgetcontrol.di

import android.content.Context
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.BankCommissionResponse
import com.example.budgetcontrol.core.data.remote.cerps.CerpsAnalyticsApiService
import com.example.budgetcontrol.core.data.remote.cerps.CerpsApiService
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionRequest
import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionResponse
import com.example.budgetcontrol.core.data.remote.cerps.dto.RatePoint
import com.example.budgetcontrol.core.data.remote.cerps.dto.RatesResponse
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import com.example.budgetcontrol.core.data.repository.NetworkStatusRepository
import com.example.budgetcontrol.core.di.ApplicationScope
import com.example.budgetcontrol.core.di.NetworkModule
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import retrofit2.Response
import java.math.BigDecimal
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object TestNetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideCerpsApiService(): CerpsApiService = FakeCerpsApiService()

    @Provides
    @Singleton
    fun provideCerpsAnalyticsApiService(): CerpsAnalyticsApiService = FakeCerpsAnalyticsApiService()

    @Provides
    @Singleton
    fun provideCerpsRepository(
        @ApplicationContext context: Context,
        apiService: CerpsApiService,
        analyticsApiService: CerpsAnalyticsApiService,
        preferencesManager: PreferencesManager,
        @ApplicationScope applicationScope: CoroutineScope
    ): CerpsRepository = CerpsRepository(
        context,
        apiService,
        analyticsApiService,
        preferencesManager,
        applicationScope
    )

    @Provides
    @Singleton
    fun provideNetworkStatusRepository(
        @ApplicationContext context: Context,
        cerpsApiService: CerpsApiService
    ): NetworkStatusRepository = NetworkStatusRepository(context, cerpsApiService)
}

private class FakeCerpsApiService : CerpsApiService {
    override suspend fun getCurrencies(): Response<List<String>> =
        Response.success(listOf("EUR", "USD", "PLN", "BYN", "GBP"))

    override suspend fun convert(request: ConversionRequest): Response<ConversionResponse> =
        Response.success(
            ConversionResponse(
                originalAmount = BigDecimal(request.amount.toString()),
                fromCurrency = request.from,
                toCurrency = request.to,
                convertedAmount = BigDecimal(request.amount.toString()),
                exchangeRate = BigDecimal.ONE,
                timestamp = "2026-01-01T00:00:00Z"
            )
        )

    override suspend fun getCurrentRates(base: String): Response<RatesResponse> =
        Response.success(
            RatesResponse(
                base = base,
                rates = mapOf(
                    "EUR" to 1.0,
                    "USD" to 1.08,
                    "PLN" to 4.50,
                    "BYN" to 3.50,
                    "GBP" to 0.85
                ),
                timestamp = "2026-01-01T00:00:00Z"
            )
        )

    override suspend fun getBankCommission(bankName: String): Response<BankCommissionResponse> =
        Response.success(BankCommissionResponse(commission = null, found = false))

    override suspend fun healthCheck(): Response<Any> = Response.success(Unit)
}

private class FakeCerpsAnalyticsApiService : CerpsAnalyticsApiService {
    override suspend fun getTrends(
        from: String,
        to: String,
        period: String
    ): Response<TrendsResponse> = Response.success(
        TrendsResponse(
            from = from,
            to = to,
            period = period,
            oldRate = 1.0,
            newRate = 1.0,
            changePercentage = 0.0,
            startDate = "2026-01-01T00:00:00Z",
            endDate = "2026-01-01T00:00:00Z",
            dataPoints = 0,
            points = emptyList()
        )
    )
}
