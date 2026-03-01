package com.example.budgetcontrol.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.budgetcontrol.core.data.remote.cerps.CerpsApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStatusRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cerpsApiService: CerpsApiService
) {

    fun isInternetAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun isCerpsAvailable(): Boolean {
        return try {
            val result = withTimeoutOrNull(3000L) {
                cerpsApiService.healthCheck()
            }
            result?.isSuccessful == true
        } catch (_: Exception) {
            false
        }
    }
}
