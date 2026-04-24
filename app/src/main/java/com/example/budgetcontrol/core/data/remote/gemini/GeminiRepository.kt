package com.example.budgetcontrol.core.data.remote.gemini

import android.util.Log
import com.example.budgetcontrol.BuildConfig
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import javax.inject.Inject
import javax.inject.Singleton

sealed class GeminiResult {
    data class Success(val commission: Double) : GeminiResult()
    object NotFound : GeminiResult()
    data class Error(val message: String? = null) : GeminiResult()
}

@Singleton
class GeminiRepository @Inject constructor(
    private val cerpsRepository: CerpsRepository
) {

    suspend fun getBankCommission(bankName: String): GeminiResult {
        if (BuildConfig.DEBUG) {
            Log.d("GeminiRepository", "Requesting commission for bank: $bankName")
        }
        return cerpsRepository.getBankCommission(bankName)
    }
}
