package com.example.budgetcontrol.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.TrendChartData
import com.example.budgetcontrol.core.domain.model.TrendSegment
import com.example.budgetcontrol.ui.util.displayName

/**
 * Legend for stacked / paired-stacked trend charts. Aggregates every category that appears
 * in any bucket, sums its amount across the whole window, and sorts largest-first. This way
 * the legend reflects the full series, not just whichever bucket happens to be tallest.
 *
 * Single-category and Empty modes return without rendering — the chart already conveys the
 * single colour visually.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrendChartLegend(
    data: TrendChartData,
    categories: List<Category>,
    modifier: Modifier = Modifier
) {
    val segments: List<TrendSegment> = remember(data) {
        val all: List<TrendSegment> = when (data) {
            is TrendChartData.Stacked -> data.buckets.flatMap { it.segments }
            is TrendChartData.PairedStacked ->
                data.buckets.flatMap { it.expenseSegments + it.incomeSegments }
            is TrendChartData.SingleCategory, TrendChartData.Empty -> emptyList()
        }
        if (all.isEmpty()) return@remember emptyList()
        // Sum amounts per categoryId across all buckets; keep the first-seen color so a
        // category renders with the same swatch wherever it appears.
        val byCategory = LinkedHashMap<String, TrendSegment>()
        for (seg in all) {
            val existing = byCategory[seg.categoryId]
            byCategory[seg.categoryId] = existing?.let { it.copy(amount = it.amount + seg.amount) } ?: seg
        }
        byCategory.values.sortedByDescending { it.amount }
    }
    if (segments.isEmpty()) return

    val categoryById = remember(categories) { categories.associateBy { it.id } }

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.forEach { segment ->
            val name = categoryById[segment.categoryId]?.displayName()
                ?: segment.categoryId
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(segment.color)
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

