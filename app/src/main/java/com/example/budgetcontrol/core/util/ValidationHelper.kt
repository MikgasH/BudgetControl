package com.example.budgetcontrol.core.util

import android.content.Context
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Category

/**
 * Утилиты для валидации данных транзакций
 */
object ValidationHelper {

    /**
     * Результат валидации
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    /**
     * Валидация суммы
     */
    fun validateAmount(context: Context, amount: String): ValidationResult {
        val normalized = amount.replace(',', '.')
        return when {
            normalized.isBlank() -> ValidationResult.Error(context.getString(R.string.validation_enter_amount))
            normalized.toDoubleOrNull() == null -> ValidationResult.Error(context.getString(R.string.validation_enter_valid_amount))
            normalized.toDouble() <= 0 -> ValidationResult.Error(context.getString(R.string.validation_amount_positive))
            else -> ValidationResult.Success
        }
    }

    /**
     * Валидация категории
     */
    fun validateCategory(context: Context, category: Category?): ValidationResult {
        return if (category == null) {
            ValidationResult.Error(context.getString(R.string.validation_select_category))
        } else {
            ValidationResult.Success
        }
    }

    /**
     * Фильтрация ввода суммы (только цифры и одна точка/запятая → точка)
     */
    fun filterAmountInput(input: String): String {
        // Replace comma with dot first (European decimal separator)
        val normalized = input.replace(',', '.')
        val filtered = normalized.filter { it.isDigit() || it == '.' }
        return if (filtered.count { it == '.' } <= 1) {
            filtered
        } else {
            // Оставляем только первую точку
            val firstDotIndex = filtered.indexOf('.')
            filtered.substring(0, firstDotIndex + 1) +
                    filtered.substring(firstDotIndex + 1).replace(".", "")
        }
    }

    /**
     * Safely parse amount string to Double, handling comma as decimal separator
     */
    fun parseAmount(input: String): Double? {
        return input.replace(',', '.').toDoubleOrNull()
    }

    /**
     * Валидация всех полей транзакции
     */
    fun validateTransaction(
        context: Context,
        amount: String,
        category: Category?
    ): ValidationResult {
        validateAmount(context, amount).let { result ->
            if (result is ValidationResult.Error) return result
        }

        validateCategory(context, category).let { result ->
            if (result is ValidationResult.Error) return result
        }

        return ValidationResult.Success
    }
}