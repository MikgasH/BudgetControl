package com.example.budgetcontrol.core.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Trend chart variants. The VM resolves segment colors here (not in the composable) so that
 * `@Immutable` skipping can hold across recompositions.
 */
sealed interface TrendChartData {
    data class Stacked(val buckets: List<StackedTrendBucket>) : TrendChartData
    data class PairedStacked(val buckets: List<PairedStackedTrendBucket>) : TrendChartData
    data class SingleCategory(
        val buckets: List<TrendBucket>,
        val color: Color,
        val limitAmount: Double?
    ) : TrendChartData
    object Empty : TrendChartData
}
