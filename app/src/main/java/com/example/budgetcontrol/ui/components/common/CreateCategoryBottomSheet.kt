package com.example.budgetcontrol.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import androidx.core.graphics.toColorInt
import com.example.budgetcontrol.core.domain.model.CategoryType

// ── Icon data ────────────────────────────────────────────────────────

data class IconEntry(val key: String, val icon: ImageVector)

data class IconGroup(val nameResId: Int, val icons: List<IconEntry>)

private val quickIcons = listOf(
    IconEntry("shopping_cart", Icons.Default.ShoppingCart),
    IconEntry("restaurant", Icons.Default.Restaurant),
    IconEntry("directions_car", Icons.Default.DirectionsCar),
    IconEntry("home", Icons.Default.Home),
    IconEntry("work", Icons.Default.Work),
    IconEntry("movie", Icons.Default.Movie),
    IconEntry("fitness_center", Icons.Default.FitnessCenter),
    IconEntry("card_giftcard", Icons.Default.CardGiftcard),
    IconEntry("flight", Icons.Default.Flight),
    IconEntry("devices", Icons.Default.Devices)
)

private val iconGroups = listOf(
    IconGroup(R.string.icon_group_finance, listOf(
        IconEntry("work", Icons.Default.Work),
        IconEntry("trending_up", Icons.Default.TrendingUp),
        IconEntry("account_balance", Icons.Default.AccountBalance),
        IconEntry("savings", Icons.Default.Savings),
        IconEntry("credit_card", Icons.Default.CreditCard),
        IconEntry("payments", Icons.Default.Payments),
        IconEntry("attach_money", Icons.Default.AttachMoney),
        IconEntry("percent", Icons.Default.Percent),
        IconEntry("sell", Icons.Default.Sell),
        IconEntry("replay", Icons.Default.Replay)
    )),
    IconGroup(R.string.icon_group_transport, listOf(
        IconEntry("directions_car", Icons.Default.DirectionsCar),
        IconEntry("flight", Icons.Default.Flight),
        IconEntry("train", Icons.Default.Train),
        IconEntry("directions_bus", Icons.Default.DirectionsBus),
        IconEntry("two_wheeler", Icons.Default.TwoWheeler),
        IconEntry("directions_bike", Icons.Default.DirectionsBike),
        IconEntry("local_shipping", Icons.Default.LocalShipping),
        IconEntry("sailing", Icons.Default.Sailing)
    )),
    IconGroup(R.string.icon_group_food, listOf(
        IconEntry("shopping_cart", Icons.Default.ShoppingCart),
        IconEntry("restaurant", Icons.Default.Restaurant),
        IconEntry("local_cafe", Icons.Default.LocalCafe),
        IconEntry("local_bar", Icons.Default.LocalBar),
        IconEntry("bakery_dining", Icons.Default.BakeryDining),
        IconEntry("fastfood", Icons.Default.Fastfood),
        IconEntry("liquor", Icons.Default.Liquor),
        IconEntry("cake", Icons.Default.Cake)
    )),
    IconGroup(R.string.icon_group_home, listOf(
        IconEntry("home", Icons.Default.Home),
        IconEntry("apartment", Icons.Default.Apartment),
        IconEntry("fitness_center", Icons.Default.FitnessCenter),
        IconEntry("spa", Icons.Default.Spa),
        IconEntry("local_hospital", Icons.Default.LocalHospital),
        IconEntry("school", Icons.Default.School),
        IconEntry("church", Icons.Default.Church),
        IconEntry("park", Icons.Default.Park)
    )),
    IconGroup(R.string.icon_group_entertainment, listOf(
        IconEntry("movie", Icons.Default.Movie),
        IconEntry("sports_esports", Icons.Default.SportsEsports),
        IconEntry("music_note", Icons.Default.MusicNote),
        IconEntry("sports_soccer", Icons.Default.SportsSoccer),
        IconEntry("casino", Icons.Default.Casino),
        IconEntry("theater_comedy", Icons.Default.TheaterComedy),
        IconEntry("headphones", Icons.Default.Headphones)
    )),
    IconGroup(R.string.icon_group_other, listOf(
        IconEntry("pets", Icons.Default.Pets),
        IconEntry("checkroom", Icons.Default.Checkroom),
        IconEntry("devices", Icons.Default.Devices),
        IconEntry("more_horiz", Icons.Default.MoreHoriz),
        IconEntry("card_giftcard", Icons.Default.CardGiftcard),
        IconEntry("star", Icons.Default.Star),
        IconEntry("favorite", Icons.Default.Favorite),
        IconEntry("category", Icons.Default.Category)
    ))
)

// ── Bottom sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCategoryBottomSheet(
    categoryType: CategoryType,
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String, color: String, type: CategoryType) -> Unit,
    initialName: String = "",
    colorPickerViewModel: ColorPickerViewModel = hiltViewModel()
) {
    val defaultIcon = "category"
    val defaultColor = colorPickerPresets[4] // #2196F3

    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(defaultIcon) }
    var selectedColor by remember { mutableStateOf(defaultColor) }
    var showMoreIcons by remember { mutableStateOf(false) }

    val recentColors by colorPickerViewModel.customColors.collectAsState()

    // Track unsaved changes
    val hasUnsavedChanges by remember {
        derivedStateOf {
            (name.isNotBlank() && name != initialName) || selectedIcon != defaultIcon || selectedColor != defaultColor
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden && hasUnsavedChanges) {
                showDiscardDialog = true
                false
            } else {
                true
            }
        }
    )

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes)) },
            text = { Text(stringResource(R.string.discard_changes_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDismiss()
                }) {
                    Text(stringResource(R.string.yes_upper))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.no_upper))
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (hasUnsavedChanges) showDiscardDialog = true else onDismiss()
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.create_category),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        if (hasUnsavedChanges) showDiscardDialog = true else onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }

            // ── SECTION 1: Name ──────────────────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.category_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            // ── SECTION 2: Icon ──────────────────────────────────────
            Text(
                text = stringResource(R.string.select_icon),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(quickIcons) { entry ->
                    IconCircle(
                        icon = entry.icon,
                        isSelected = selectedIcon == entry.key,
                        selectedColor = selectedColor,
                        onClick = { selectedIcon = entry.key }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clickable { showMoreIcons = !showMoreIcons }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (showMoreIcons) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.more_icons),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = showMoreIcons) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    iconGroups.forEach { group ->
                        Text(
                            text = stringResource(group.nameResId),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(8),
                            modifier = Modifier.heightIn(max = 200.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            userScrollEnabled = false
                        ) {
                            items(group.icons) { entry ->
                                IconCircle(
                                    icon = entry.icon,
                                    isSelected = selectedIcon == entry.key,
                                    selectedColor = selectedColor,
                                    onClick = { selectedIcon = entry.key },
                                    size = 40
                                )
                            }
                        }
                    }
                }
            }

            // ── SECTION 3: Color ─────────────────────────────────────
            Text(
                text = stringResource(R.string.select_color),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            CustomColorPicker(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it },
                recentColors = recentColors,
                previewContent = { color ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resolveIcon(selectedIcon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Save button ──────────────────────────────────────────
            Button(
                onClick = {
                    val color = selectedColor.uppercase()
                    if (color !in colorPickerPresets) {
                        colorPickerViewModel.addCustomColor(color)
                    }
                    onSave(name.trim(), selectedIcon, color, categoryType)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = stringResource(R.string.create_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

// ── Helper composables ───────────────────────────────────────────────

@Composable
private fun IconCircle(
    icon: ImageVector,
    isSelected: Boolean,
    selectedColor: String,
    onClick: () -> Unit,
    size: Int = 44
) {
    val bgColor = if (isSelected) {
        try { Color(selectedColor.toColorInt()) }
        catch (_: Exception) { MaterialTheme.colorScheme.primary }
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}

// ── Pure helpers ─────────────────────────────────────────────────────

private fun resolveIcon(key: String): ImageVector = when (key) {
    "shopping_cart" -> Icons.Default.ShoppingCart
    "directions_car" -> Icons.Default.DirectionsCar
    "movie" -> Icons.Default.Movie
    "local_hospital" -> Icons.Default.LocalHospital
    "home" -> Icons.Default.Home
    "subscriptions" -> Icons.Default.Subscriptions
    "restaurant" -> Icons.Default.Restaurant
    "checkroom" -> Icons.Default.Checkroom
    "school" -> Icons.Default.School
    "flight" -> Icons.Default.Flight
    "spa" -> Icons.Default.Spa
    "pets" -> Icons.Default.Pets
    "fitness_center" -> Icons.Default.FitnessCenter
    "devices" -> Icons.Default.Devices
    "more_horiz" -> Icons.Default.MoreHoriz
    "work" -> Icons.Default.Work
    "computer" -> Icons.Default.Computer
    "trending_up" -> Icons.Default.TrendingUp
    "card_giftcard" -> Icons.Default.CardGiftcard
    "sell" -> Icons.Default.Sell
    "apartment" -> Icons.Default.Apartment
    "replay" -> Icons.Default.Replay
    "account_balance" -> Icons.Default.AccountBalance
    "savings" -> Icons.Default.Savings
    "credit_card" -> Icons.Default.CreditCard
    "payments" -> Icons.Default.Payments
    "attach_money" -> Icons.Default.AttachMoney
    "percent" -> Icons.Default.Percent
    "train" -> Icons.Default.Train
    "directions_bus" -> Icons.Default.DirectionsBus
    "two_wheeler" -> Icons.Default.TwoWheeler
    "directions_bike" -> Icons.Default.DirectionsBike
    "local_shipping" -> Icons.Default.LocalShipping
    "sailing" -> Icons.Default.Sailing
    "local_cafe" -> Icons.Default.LocalCafe
    "local_bar" -> Icons.Default.LocalBar
    "bakery_dining" -> Icons.Default.BakeryDining
    "fastfood" -> Icons.Default.Fastfood
    "liquor" -> Icons.Default.Liquor
    "cake" -> Icons.Default.Cake
    "church" -> Icons.Default.Church
    "park" -> Icons.Default.Park
    "sports_esports" -> Icons.Default.SportsEsports
    "music_note" -> Icons.Default.MusicNote
    "sports_soccer" -> Icons.Default.SportsSoccer
    "casino" -> Icons.Default.Casino
    "theater_comedy" -> Icons.Default.TheaterComedy
    "headphones" -> Icons.Default.Headphones
    "star" -> Icons.Default.Star
    "favorite" -> Icons.Default.Favorite
    "category" -> Icons.Default.Category
    else -> Icons.Default.Category
}
