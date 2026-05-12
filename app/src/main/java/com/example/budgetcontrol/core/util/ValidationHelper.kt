package com.example.budgetcontrol.core.util

import android.content.Context
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Category

fun String.toDoubleLocale(): Double? {
    return this.replace(",", ".").toDoubleOrNull()
}

object ValidationHelper {

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    fun validateAmount(context: Context, amount: String): ValidationResult {
        val normalized = amount.replace(',', '.')
        return when {
            normalized.isBlank() -> ValidationResult.Error(context.getString(R.string.validation_enter_amount))
            normalized.toDoubleOrNull() == null -> ValidationResult.Error(context.getString(R.string.validation_enter_valid_amount))
            normalized.toDouble() <= 0 -> ValidationResult.Error(context.getString(R.string.validation_amount_positive))
            else -> ValidationResult.Success
        }
    }

    fun validateCategory(context: Context, category: Category?): ValidationResult {
        return if (category == null) {
            ValidationResult.Error(context.getString(R.string.validation_select_category))
        } else {
            ValidationResult.Success
        }
    }

    /**
     * Filters input to allow only digits and a single decimal separator (comma → dot).
     */
    fun filterAmountInput(input: String): String {
        // Replace comma with dot first (European decimal separator)
        val normalized = input.replace(',', '.')
        val filtered = normalized.filter { it.isDigit() || it == '.' }
        return if (filtered.count { it == '.' } <= 1) {
            filtered
        } else {
            // Keep only the first decimal point
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