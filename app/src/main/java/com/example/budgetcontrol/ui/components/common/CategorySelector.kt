package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.theme.AppBlue

/**
 * Общий компонент для выбора категории
 */
@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelect: (Category) -> Unit,
    transactionType: TransactionType,
    modifier: Modifier = Modifier
) {
    val title = when (transactionType) {
        TransactionType.EXPENSE -> "Категории расходов"
        TransactionType.INCOME -> "Категории доходов"
    }

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(categories) { category ->
                CategoryItem(
                    category = category,
                    isSelected = category.id == selectedCategory?.id,
                    onClick = { onCategorySelect(category) }
                )
            }

            // Кнопка "Еще" для добавления новых категорий (пока не реализована)
            item {
                CategoryItem(
                    category = null,
                    isSelected = false,
                    onClick = { /* TODO: Добавить новую категорию */ },
                    isAddButton = true
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAddButton: Boolean = false
) {
    val backgroundColor = when {
        isAddButton -> MaterialTheme.colorScheme.surfaceVariant
        isSelected -> category?.let {
            Color(android.graphics.Color.parseColor(it.color))
        } ?: AppBlue
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected && !isAddButton) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isAddButton) {
                    Icons.Default.Add
                } else {
                    getCategoryIcon(category?.iconName)
                },
                contentDescription = category?.name ?: "Добавить категорию",
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = category?.name ?: "Еще",
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected && !isAddButton) AppBlue else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected && !isAddButton) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp),
            maxLines = 2
        )
    }
}

/**
 * Получение иконки для категории
 */
@Composable
private fun getCategoryIcon(iconName: String?): ImageVector {
    return when (iconName) {
        // Иконки для расходов
        "shopping_cart" -> Icons.Default.ShoppingCart
        "directions_car" -> Icons.Default.DirectionsCar
        "movie" -> Icons.Default.Movie
        "local_hospital" -> Icons.Default.LocalHospital
        "home" -> Icons.Default.Home
        "subscriptions" -> Icons.Default.Subscriptions

        // Иконки для доходов
        "work" -> Icons.Default.Work
        "computer" -> Icons.Default.Computer
        "trending_up" -> Icons.Default.TrendingUp
        "card_giftcard" -> Icons.Default.CardGiftcard
        "sell" -> Icons.Default.Sell

        // По умолчанию
        else -> Icons.Default.Category
    }
}