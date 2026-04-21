package com.example.budgetcontrol.core.domain.model

enum class RateSource {
    BANK_AUTO,
    USER_CORRECTED,
    CASH_EXCHANGE,
    CACHED_RATE,
    HOME_CURRENCY;

    companion object {
        fun fromStringOrNull(value: String?): RateSource? =
            value?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}
