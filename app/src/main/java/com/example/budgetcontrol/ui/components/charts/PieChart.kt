package com.example.budgetcontrol.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.CategoryStatistic

@Composable
fun PieChart(
    data: List<CategoryStatistic>,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (data.isNotEmpty()) {
            Canvas(
                modifier = Modifier.size(200.dp)
            ) {
                drawPieChart(data)
            }

            // Центральный текст с общей суммой
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${String.format("%.2f", totalAmount)} €",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.pie_chart_total),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun DrawScope.drawPieChart(data: List<CategoryStatistic>) {
    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2 * 0.8f
    val strokeWidth = 40.dp.toPx()

    var startAngle = -90f // Начинаем сверху

    data.forEach { stat ->
        val sweepAngle = (stat.percentage / 100f) * 360f
        val color = Color(android.graphics.Color.parseColor(stat.category.color))

        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            topLeft = androidx.compose.ui.geometry.Offset(
                center.x - radius,
                center.y - radius
            ),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )

        startAngle += sweepAngle
    }
}