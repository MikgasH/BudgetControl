package com.example.budgetcontrol.core.data.remote.gemini

import android.util.Log
import com.example.budgetcontrol.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

sealed class GeminiResult {
    data class Success(val commission: Double) : GeminiResult()
    object NotFound : GeminiResult()
    object Error : GeminiResult()
}

@Singleton
class GeminiRepository @Inject constructor(
    private val apiService: GeminiApiService
) {

    suspend fun getBankCommission(bankName: String): GeminiResult {
        return try {
            val prompt = "What is the foreign currency exchange commission percentage charged by $bankName " +
                    "when converting currencies? Reply with ONLY a single number like 1.5 or 0.5. " +
                    "If you don't know or are unsure, reply with exactly: NOT_FOUND"

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                )
            )

            val response = apiService.generateContent(
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = request
            )

            if (!response.isSuccessful) {
                Log.e("GeminiRepository", "API error: ${response.code()} ${response.errorBody()?.string()}")
                return GeminiResult.Error
            }

            val body = response.body() ?: return GeminiResult.Error
            val text = body.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?: return GeminiResult.Error

            if (text.contains("NOT_FOUND", ignoreCase = true)) {
                return GeminiResult.NotFound
            }

            val value = text.toDoubleOrNull()
            if (value != null) {
                GeminiResult.Success(value)
            } else {
                GeminiResult.NotFound
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "getBankCommission failed", e)
            GeminiResult.Error
        }
    }
}
