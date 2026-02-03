package com.example.budgetcontrol.core.data.remote.cerps

import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionRequest
import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CerpsApiService {

    @GET("api/v1/currencies")
    suspend fun getCurrencies(): Response<List<String>>

    @POST("api/v1/currencies/convert")
    suspend fun convert(@Body request: ConversionRequest): Response<ConversionResponse>
}