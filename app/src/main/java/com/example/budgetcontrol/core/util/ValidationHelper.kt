package com.example.budgetcontrol.core.util

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
    fun validateAmount(amount: String): ValidationResult {
        return when {
            amount.isBlank() -> ValidationResult.Error("Введите сумму")
            amount.toDoubleOrNull() == null -> ValidationResult.Error("Введите корректную сумму")
            amount.toDouble() <= 0 -> ValidationResult.Error("Сумма должна быть больше нуля")
            else -> ValidationResult.Success
        }
    }

    /**
     * Валидация категории
     */
    fun validateCategory(category: Category?): ValidationResult {
        return if (category == null) {
            ValidationResult.Error("Выберите категорию")
        } else {
            ValidationResult.Success
        }
    }

    /**
     * Фильтрация ввода суммы (только цифры и одна точка)
     */
    fun filterAmountInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
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
     * Валидация всех полей транзакции
     */
    fun validateTransaction(
        amount: String,
        category: Category?
    ): ValidationResult {
        validateAmount(amount).let { result ->
            if (result is ValidationResult.Error) return result
        }

        validateCategory(category).let { result ->
            if (result is ValidationResult.Error) return result
        }

        return ValidationResult.Success
    }
}