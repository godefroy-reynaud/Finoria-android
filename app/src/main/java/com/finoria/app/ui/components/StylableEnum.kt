package com.finoria.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Interface commune pour les enums stylisables (AccountStyle, TransactionCategory).
 * Permet d'utiliser StylePickerGrid et StyleIconView de manière générique.
 */
interface StylableEnum {
    val icon: ImageVector
    val color: Color
    val label: String
}
