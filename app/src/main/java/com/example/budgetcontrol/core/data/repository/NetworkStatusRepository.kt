package com.example.budgetcontrol.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.budgetcontrol.core.data.remote.cerps.CerpsApiService
import com.example.budgetcontrol.core.util.CERPS_TIMEOUT_MS
import com.example.budgetcontrol.core.util.HEALTH_CHECK_CACHE_MS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStatusRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cerpsApiService: CerpsApiService
) {

    private var lastHealthCheckResult: Boolean = false
    private var lastHealthCheckTimestamp: Long = 0L

    fun isInternetAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun isCerpsAvailable(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastHealthCheckTimestamp < HEALTH_CHECK_CACHE_MS) {
            return lastHealthCheckResult
        }

        val result = try {
            // Timeout matches the OkHttp connect timeout — fail fast if CERPS is unreachable
            val response = withTimeoutOrNull(CERPS_TIMEOUT_MS) {
                cerpsApiService.healthCheck()
            }
            response?.isSuccessful == true
        } catch (_: Exception) {
            false
        }

        lastHealthCheckResult = result
        lastHealthCheckTimestamp = now
        return result
    }
}
