package com.example.budgetcontrol.core.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class TrendBucket(
    val start: Long,
    val end: Long,
    val total: Double,
    val periodOffset: Int,
    val label: String
)

@Immutable
data class TrendSegment(
    val categoryId: String,
    val color: Color,
    val amount: Double
)

@Immutable
data class StackedTrendBucket(
    val start: Long,
    val end: Long,
    val periodOffset: Int,
    val label: String,
    val segments: List<TrendSegment>
) {
    val total: Double get() = segments.sumOf { it.amount }
}

@Immutable
data class PairedStackedTrendBucket(
    val start: Long,
    val end: Long,
    val periodOffset: Int,
    val label: String,
    val expenseSegments: List<TrendSegment>,
    val incomeSegments: List<TrendSegment>
) {
    val expenseTotal: Double get() = expenseSegments.sumOf { it.amount }
    val incomeTotal: Double get() = incomeSegments.sumOf { it.amount }
}
