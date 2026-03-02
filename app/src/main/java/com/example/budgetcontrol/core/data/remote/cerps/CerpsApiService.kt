package com.example.budgetcontrol.core.data.remote.cerps

import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionRequest
import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionResponse
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CerpsApiService {

    @GET("api/v1/currencies")
    suspend fun getCurrencies(): Response<List<String>>

    @POST("api/v1/currencies/convert")
    suspend fun convert(@Body request: ConversionRequest): Response<ConversionResponse>

    @GET("api/v1/analytics/trends")
    suspend fun getTrends(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("period") period: String
    ): Response<TrendsResponse>

    @GET("actuator/health")
    suspend fun healthCheck(): Response<Any>
}