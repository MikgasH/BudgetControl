package com.example.budgetcontrol.core.data.remote.cerps

import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionRequest
import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionResponse
import javax.inject.Inject
import javax.inject.Singleton

sealed class CerpsResult<out T> {
    data class Success<T>(val data: T) : CerpsResult<T>()
    data class Error(val message: String) : CerpsResult<Nothing>()
}

@Singleton
class CerpsRepository @Inject constructor(
    private val apiService: CerpsApiService
) {

    suspend fun getCurrencies(): CerpsResult<List<String>> {
        return try {
            val response = apiService.getCurrencies()
            if (response.isSuccessful && response.body() != null) {
                CerpsResult.Success(response.body()!!)
            } else {
                CerpsResult.Error("Ошибка загрузки валют: ${response.code()}")
            }
        } catch (e: Exception) {
            CerpsResult.Error("Сервис конвертации недоступен: ${e.message}")
        }
    }

    suspend fun convert(
        from: String,
        to: String,
        amount: Double
    ): CerpsResult<ConversionResponse> {
        return try {
            val request = ConversionRequest(amount = amount, from = from, to = to)
            val response = apiService.convert(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    CerpsResult.Success(body)
                } else {
                    CerpsResult.Error("Конвертация не удалась")
                }
            } else {
                CerpsResult.Error("Ошибка конвертации: ${response.code()}")
            }
        } catch (e: Exception) {
            CerpsResult.Error("Сервис конвертации недоступен: ${e.message}")
        }
    }

    suspend fun isServiceAvailable(): Boolean {
        return try {
            val response = apiService.getCurrencies()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}