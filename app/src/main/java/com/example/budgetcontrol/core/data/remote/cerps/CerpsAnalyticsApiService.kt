package com.example.budgetcontrol.core.data.remote.cerps

import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CerpsAnalyticsApiService {

    @GET("api/v1/analytics/trends")
    suspend fun getTrends(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("period") period: String
    ): Response<TrendsResponse>
}
