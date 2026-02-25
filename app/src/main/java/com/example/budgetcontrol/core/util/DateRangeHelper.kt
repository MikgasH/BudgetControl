package com.example.budgetcontrol.core.util

import android.content.Context
import com.example.budgetcontrol.R
import com.example.budgetcontrol.feature.main.PeriodType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Утилиты для работы с диапазонами дат
 */
object DateRangeHelper {

    /**
     * Получить диапазон дат для указанного периода
     */
    fun getDateRange(
        periodType: PeriodType,
        periodOffset: Int = 0,
        customStartDate: Long? = null,
        customEndDate: Long? = null
    ): Pair<Long, Long> {

        // Если указан кастомный период
        if (periodType == PeriodType.PERIOD && customStartDate != null && customEndDate != null) {
            return Pair(customStartDate, customEndDate)
        }

        val calendar = Calendar.getInstance()

        return when (periodType) {
            PeriodType.DAY -> {
                calendar.add(Calendar.DAY_OF_MONTH, periodOffset)
                val startOfDay = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfDay = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                Pair(startOfDay, endOfDay)
            }

            PeriodType.WEEK -> {
                calendar.add(Calendar.WEEK_OF_YEAR, periodOffset)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.timeInMillis

                calendar.add(Calendar.DAY_OF_MONTH, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val weekEnd = calendar.timeInMillis

                Pair(weekStart, weekEnd)
            }

            PeriodType.MONTH -> {
                calendar.add(Calendar.MONTH, periodOffset)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val monthEnd = calendar.timeInMillis

                Pair(monthStart, monthEnd)
            }

            PeriodType.YEAR -> {
                calendar.add(Calendar.YEAR, periodOffset)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val yearStart = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val yearEnd = calendar.timeInMillis

                Pair(yearStart, yearEnd)
            }

            else -> {
                // По умолчанию возвращаем текущий день
                val startOfDay = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfDay = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                Pair(startOfDay, endOfDay)
            }
        }
    }

    /**
     * Получить текстовое представление периода
     */
    fun getPeriodDisplayText(
        context: Context,
        periodType: PeriodType,
        periodOffset: Int = 0,
        customStartDate: Long? = null,
        customEndDate: Long? = null,
        isAllTimePeriod: Boolean = false
    ): String {

        if (isAllTimePeriod) {
            return context.getString(R.string.period_all_time)
        }

        if (periodType == PeriodType.PERIOD && customStartDate != null && customEndDate != null) {
            val formatter = SimpleDateFormat("d MMM", Locale.getDefault())
            val startFormatted = formatter.format(Date(customStartDate))
            val endFormatted = formatter.format(Date(customEndDate))
            return "$startFormatted - $endFormatted"
        }

        val calendar = Calendar.getInstance()

        return when (periodType) {
            PeriodType.DAY -> {
                calendar.add(Calendar.DAY_OF_MONTH, periodOffset)
                val dateStr = android.text.format.DateFormat.format("d MMMM", calendar).toString()
                when (periodOffset) {
                    0 -> context.getString(R.string.period_today, dateStr)
                    -1 -> context.getString(R.string.period_yesterday, dateStr)
                    1 -> context.getString(R.string.period_tomorrow, dateStr)
                    else -> android.text.format.DateFormat.format("d MMMM yyyy", calendar).toString()
                }
            }

            PeriodType.WEEK -> {
                calendar.add(Calendar.WEEK_OF_YEAR, periodOffset)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val weekStart = android.text.format.DateFormat.format("d MMM", calendar)

                calendar.add(Calendar.DAY_OF_MONTH, 6)
                val weekEnd = android.text.format.DateFormat.format("d MMM", calendar)

                "$weekStart - $weekEnd"
            }

            PeriodType.MONTH -> {
                calendar.add(Calendar.MONTH, periodOffset)
                android.text.format.DateFormat.format("MMMM yyyy", calendar).toString()
            }

            PeriodType.YEAR -> {
                calendar.add(Calendar.YEAR, periodOffset)
                calendar.get(Calendar.YEAR).toString()
            }

            else -> context.getString(R.string.period_label)
        }
    }

    /**
     * Проверка является ли период "За все время"
     */
    fun isAllTimePeriod(startDate: Long, endDate: Long): Boolean {
        val startCal = Calendar.getInstance().apply { timeInMillis = startDate }
        val endCal = Calendar.getInstance().apply { timeInMillis = endDate }

        return startCal.get(Calendar.YEAR) <= 2020 && endCal.get(Calendar.YEAR) >= 2030
    }

    /**
     * Проверка что две даты в один день
     */
    fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}