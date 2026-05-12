package com.example.budgetcontrol.core.util

import com.example.budgetcontrol.core.domain.model.PeriodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DateRangeHelperTest {

    @Test
    fun `DAY offset 0 - start at 00-00-00-000 and end at 23-59-59-999, offset -1 is 24h earlier`() {
        val todayRange = DateRangeHelper.getDateRange(PeriodType.DAY, periodOffset = 0)

        val startCal = Calendar.getInstance().apply { timeInMillis = todayRange.first }
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))
        assertEquals(0, startCal.get(Calendar.MILLISECOND))

        val endCal = Calendar.getInstance().apply { timeInMillis = todayRange.second }
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))

        val yesterdayRange = DateRangeHelper.getDateRange(PeriodType.DAY, periodOffset = -1)
        val dayMs = TimeUnit.DAYS.toMillis(1)
        assertEquals(dayMs, todayRange.first - yesterdayRange.first)
        assertEquals(dayMs, todayRange.second - yesterdayRange.second)
    }

    @Test
    fun `MONTH offset 0 - start on day 1 at 00-00-00-000 and end on last day at 23-59-59-999`() {
        val range = DateRangeHelper.getDateRange(PeriodType.MONTH, periodOffset = 0)

        val startCal = Calendar.getInstance().apply { timeInMillis = range.first }
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))
        assertEquals(0, startCal.get(Calendar.MILLISECOND))

        val endCal = Calendar.getInstance().apply { timeInMillis = range.second }
        val lastDayOfMonth = endCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        assertEquals(lastDayOfMonth, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))

        // Start and end are in the same month/year
        assertEquals(startCal.get(Calendar.MONTH), endCal.get(Calendar.MONTH))
        assertEquals(startCal.get(Calendar.YEAR), endCal.get(Calendar.YEAR))
        assertTrue(range.second > range.first)
    }
}
