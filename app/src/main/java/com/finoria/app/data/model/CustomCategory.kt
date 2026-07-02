package com.finoria.app.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.finoria.app.data.model.serializers.UUIDSerializer
import com.finoria.app.ui.components.StylableEnum
import kotlinx.serialization.Serializable
import java.text.Normalizer
import java.util.UUID

/**
 * Catégorie personnalisée de transaction — **propre à un compte** (chaque compte a
 * sa propre liste). Créée/modifiée/supprimée par l'utilisateur, ou créée
 * automatiquement à l'import CSV quand un libellé de catégorie est inconnu.
 *
 * Règle de suppression = **nullify** : les transactions/raccourcis/récurrences qui
 * la référençaient voient leur `customCategoryId` remis à null (elles retombent sur
 * la catégorie par défaut `Autre`). Appliquée au niveau base via FK `SET_NULL`.
 *
 * Implémente [StylableEnum] pour être affichée partout comme une catégorie par
 * défaut (cercle coloré + icône + libellé).
 */
@Serializable
data class CustomCategory(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val symbol: String = DEFAULT_SYMBOL,
    val colorHex: String = DEFAULT_COLOR_HEX,
) : StylableEnum {

    override val icon: ImageVector
        get() = CustomCategoryIcons.iconFor(symbol)

    override val color: Color
        get() = parseHexColor(colorHex)

    override val label: String
        get() = name

    companion object {
        /** Symbole par défaut — équivalent Android du `tag.fill` iOS. */
        const val DEFAULT_SYMBOL = "sell"

        /** Gris système iOS (`#8E8E93`) — couleur par défaut du portage. */
        const val DEFAULT_COLOR_HEX = "#8E8E93"

        /** Longueur max du nom dans la sheet de création/édition. */
        const val MAX_NAME_LENGTH = 15

        private val COMBINING_MARKS = Regex("\\p{Mn}+")

        /**
         * Normalisation utilisée pour **toute** comparaison de noms de catégorie
         * (validation des doublons, résolution à l'import CSV, rattachement
         * différé) : trim + pliage insensible à la casse ET aux accents.
         * « Épargne », « epargne » et « ÉPARGNE  » sont identiques.
         */
        fun normalizeName(name: String): String =
            Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replace(COMBINING_MARKS, "")
                .lowercase()

        /** Parse `#RRGGBB` (opacité pleine). Retombe sur le gris par défaut. */
        fun parseHexColor(hex: String): Color =
            runCatching { Color(0xFF000000L or hex.removePrefix("#").toLong(16)) }
                .getOrDefault(Color(0xFF8E8E93))
    }
}
