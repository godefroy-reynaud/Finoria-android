package com.finoria.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Icône dans un cercle coloré — utilisé pour les catégories et les styles de compte.
 */
@Composable
fun StyleIconView(
    style: StylableEnum,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(style.color.copy(alpha = 0.15f))
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = style.label,
            tint = style.color,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}
