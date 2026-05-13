package com.example.budgetcontrol.core.domain.model

import androidx.annotation.StringRes
import com.example.budgetcontrol.R

enum class PeriodType(@param:StringRes val displayNameRes: Int) {
    DAY(R.string.period_day),
    WEEK(R.string.period_week),
    MONTH(R.string.period_month),
    YEAR(R.string.period_year),
    PERIOD(R.string.period_custom)
}
