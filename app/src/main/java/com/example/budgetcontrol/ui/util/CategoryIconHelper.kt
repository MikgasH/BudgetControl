package com.example.budgetcontrol.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getCategoryIcon(iconName: String?): ImageVector {
    return when (iconName) {
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
        "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
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
}
