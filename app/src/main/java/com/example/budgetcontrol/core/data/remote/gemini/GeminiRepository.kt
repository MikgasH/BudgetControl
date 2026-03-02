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

            Log.d("GeminiRepository", "Requesting commission for bank: $bankName")
            Log.d("GeminiRepository", "API key present: ${BuildConfig.GEMINI_API_KEY.isNotBlank()}")

            val response = apiService.generateContent(
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = request
            )

            if (!response.isSuccessful) {
                val errorBody = try { response.errorBody()?.string() } catch (_: Exception) { "unable to read" }
                Log.e("GeminiRepository", "API error: HTTP ${response.code()}, body: $errorBody")
                return GeminiResult.Error
            }

            val body = response.body()
            if (body == null) {
                Log.e("GeminiRepository", "Response body is null")
                return GeminiResult.Error
            }

            Log.d("GeminiRepository", "Response candidates count: ${body.candidates?.size}")

            val text = body.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()

            if (text == null) {
                Log.e("GeminiRepository", "No text in response. Candidates: ${body.candidates}")
                return GeminiResult.Error
            }

            Log.d("GeminiRepository", "Gemini response text: $text")

            if (text.contains("NOT_FOUND", ignoreCase = true)) {
                return GeminiResult.NotFound
            }

            // Try to extract number from response (Gemini sometimes adds % or text around it)
            val numberRegex = Regex("""(\d+\.?\d*)""")
            val matchResult = numberRegex.find(text)
            val value = matchResult?.groupValues?.get(1)?.toDoubleOrNull()
                ?: text.toDoubleOrNull()

            if (value != null) {
                Log.d("GeminiRepository", "Parsed commission: $value%")
                GeminiResult.Success(value)
            } else {
                Log.w("GeminiRepository", "Could not parse number from: $text")
                GeminiResult.NotFound
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "getBankCommission failed: ${e.javaClass.simpleName}: ${e.message}", e)
            GeminiResult.Error
        }
    }
}
