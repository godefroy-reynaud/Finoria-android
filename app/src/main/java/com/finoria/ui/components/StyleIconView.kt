package com.finoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.finoria.ui.utils.StylableEnum

@Composable
fun <T : StylableEnum> StyleIconView(style: T, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(style.color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getIcon(style.icon),
            contentDescription = style.label,
            tint = Color.White
        )
    }
}

fun getIcon(name: String): ImageVector {
    return when (name) {
        "account_balance" -> Icons.Default.AccountBalance
        "savings" -> Icons.Default.Savings
        "trending_up" -> Icons.Default.TrendingUp
        "credit_card" -> Icons.Default.CreditCard
        "payments" -> Icons.Default.Payments
        "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
        "business_center" -> Icons.Default.BusinessCenter
        "add_chart" -> Icons.Default.AddChart
        "home" -> Icons.Default.Home
        "bolt" -> Icons.Default.Bolt
        "subscriptions" -> Icons.Default.Subscriptions
        "smartphone" -> Icons.Default.Smartphone
        "shield" -> Icons.Default.Shield
        "restaurant" -> Icons.Default.Restaurant
        "shopping_bag" -> Icons.Default.ShoppingBag
        "local_gas_station" -> Icons.Default.LocalGasStation
        "directions_bus" -> Icons.Default.DirectionsBus
        "family_restroom" -> Icons.Default.FamilyRestroom
        "medical_services" -> Icons.Default.MedicalServices
        "redeem" -> Icons.Default.Redeem
        "celebration" -> Icons.Default.Celebration
        "receipt_long" -> Icons.Default.ReceiptLong
        "more_horiz" -> Icons.Default.MoreHoriz
        "star" -> Icons.Default.Star
        "priority_high" -> Icons.Default.PriorityHigh
        else -> Icons.Default.MoreHoriz
    }
}
