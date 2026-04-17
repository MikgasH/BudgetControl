package com.example.budgetcontrol.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.budgetcontrol.R

// Shared preset palette used by both category and account colour pickers.
// A colour not in this list is considered "custom" and eligible for the Recent row.
internal val colorPickerPresets = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#3F51B5",
    "#2196F3", "#00BCD4", "#4CAF50", "#8BC34A",
    "#FF9800", "#FF5722", "#795548", "#607D8B"
)

/**
 * Shared colour picker used by both CreateCategoryBottomSheet and
 * CreateEditAccountBottomSheet.
 *
 * @param selectedColor  Current hex colour (e.g. "#2196F3").
 * @param onColorSelected Called whenever the selection changes.
 * @param recentColors   Most-recent-first list of custom colours to show above presets.
 * @param previewContent Optional composable rendered inside the custom-colour panel,
 *                       receiving the live [Color] so callers can show an icon preview.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    recentColors: List<String> = emptyList(),
    previewContent: (@Composable (color: Color) -> Unit)? = null
) {
    var showCustom by remember { mutableStateOf(false) }

    val (initR, initG, initB) = remember(selectedColor) { parsePickerHex(selectedColor) }
    var red by remember { mutableIntStateOf(initR) }
    var green by remember { mutableIntStateOf(initG) }
    var blue by remember { mutableIntStateOf(initB) }
    var hexDraft by remember { mutableStateOf(selectedColor.removePrefix("#")) }

    // ── Unified colour grid (recent first, then presets) ─────────────
    val allColors = remember(recentColors) {
        (recentColors + colorPickerPresets).distinct()
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        allColors.forEach { hex ->
            PickerColorCircle(
                hex = hex,
                isSelected = selectedColor == hex && !showCustom,
                onClick = {
                    onColorSelected(hex)
                    showCustom = false
                    val (r, g, b) = parsePickerHex(hex)
                    red = r; green = g; blue = b
                    hexDraft = hex.removePrefix("#")
                }
            )
        }
    }

    // ── Custom colour toggle ─────────────────────────────────────────
    Row(
        modifier = Modifier
            .clickable { showCustom = !showCustom }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (showCustom) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.custom_color),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    AnimatedVisibility(visible = showCustom) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Optional icon/preview content supplied by the caller
            previewContent?.invoke(Color(red, green, blue))

            // HEX input
            OutlinedTextField(
                value = hexDraft,
                onValueChange = { input ->
                    val filtered = input.filter { it.isLetterOrDigit() }.take(6)
                    hexDraft = filtered
                    if (filtered.length == 6) {
                        val (r, g, b) = parsePickerHex("#$filtered")
                        red = r; green = g; blue = b
                        onColorSelected("#${filtered.uppercase()}")
                    }
                },
                label = { Text(stringResource(R.string.hex_color_label)) },
                prefix = { Text("#") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            PickerColorSliderRow(
                label = "R",
                value = red,
                color = Color.Red,
                onValueChange = {
                    red = it
                    val hex = buildPickerHex(red, green, blue)
                    onColorSelected(hex)
                    hexDraft = hex.removePrefix("#")
                }
            )
            PickerColorSliderRow(
                label = "G",
                value = green,
                color = Color(0xFF4CAF50),
                onValueChange = {
                    green = it
                    val hex = buildPickerHex(red, green, blue)
                    onColorSelected(hex)
                    hexDraft = hex.removePrefix("#")
                }
            )
            PickerColorSliderRow(
                label = "B",
                value = blue,
                color = Color.Blue,
                onValueChange = {
                    blue = it
                    val hex = buildPickerHex(red, green, blue)
                    onColorSelected(hex)
                    hexDraft = hex.removePrefix("#")
                }
            )
        }
    }
}

// ── Internal helpers ─────────────────────────────────────────────────

@Composable
internal fun PickerColorCircle(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = try { Color(hex.toColorInt()) } catch (_: Exception) { Color.Gray }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier
            )
            .clickable { onClick() }
    )
}

@Composable
private fun PickerColorSliderRow(
    label: String,
    value: Int,
    color: Color,
    onValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(30.dp),
            fontSize = 12.sp
        )
    }
}

internal fun parsePickerHex(hex: String): Triple<Int, Int, Int> {
    return try {
        val c = hex.toColorInt()
        Triple(
            android.graphics.Color.red(c),
            android.graphics.Color.green(c),
            android.graphics.Color.blue(c)
        )
    } catch (_: Exception) {
        Triple(33, 150, 243) // fallback #2196F3
    }
}

internal fun buildPickerHex(r: Int, g: Int, b: Int): String =
    "#%02X%02X%02X".format(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
