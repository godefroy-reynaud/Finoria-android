package com.finoria.model

import androidx.compose.ui.graphics.Color
import com.finoria.ui.utils.StylableEnum
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WidgetShortcut(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val amount: Double,
    val category: TransactionCategory,
    val style: ShortcutStyle
)

@Serializable
enum class ShortcutStyle(
    override val icon: String,
    @Serializable(with = ColorSerializer::class)
    override val color: Color,
    override val label: String
) : StylableEnum {
    STANDARD("bolt", Color(0xFFFFC107), "Standard"),
    ESSENTIAL("star", Color(0xFF4CAF50), "Essentiel"),
    URGENT("priority_high", Color(0xFFF44336), "Urgent"),
    FUN("celebration", Color(0xFF9C27B0), "Loisir")
}
