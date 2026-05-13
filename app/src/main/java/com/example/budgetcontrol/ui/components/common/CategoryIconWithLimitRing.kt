package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.budgetcontrol.ui.util.getCategoryIcon

private val RING_GREEN = Color(0xFF4CAF50)
private val RING_AMBER = Color(0xFFFFC107)
private val RING_RED = Color(0xFFF44336)

@Composable
fun CategoryIconWithLimitRing(
    iconName: String,
    iconColor: Color,
    sizeDp: Dp,
    progress: Float?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentColor: Color = Color.White
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 3.dp.toPx() }
    // The ring sits a hair outside the icon disc so it doesn't overlap it.
    val ringSize = sizeDp + 6.dp

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        if (progress != null) {
            val (ringColor, sweep) = remember(progress) {
                when {
                    progress < 0.5f -> RING_GREEN to (progress * 360f)
                    progress < 0.8f -> RING_AMBER to (progress * 360f)
                    progress < 1.0f -> RING_RED to (progress * 360f)
                    else -> RING_RED to 360f
                }
            }
            Canvas(modifier = Modifier.size(ringSize)) {
                val inset = strokeWidthPx / 2f
                val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(sizeDp)
                .clip(CircleShape)
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(iconName),
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(sizeDp * 0.5f)
            )
        }
    }
}
