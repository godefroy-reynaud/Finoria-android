package com.finoria.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.CustomCategory
import com.finoria.app.data.model.Transaction
import com.finoria.app.ui.LocalCustomCategories
import com.finoria.app.ui.theme.ExpenseRed
import com.finoria.app.ui.theme.IncomeGreen
import com.finoria.app.util.formattedCurrency
import com.finoria.app.util.shortFormatted

/**
 * Ligne d'affichage d'une transaction.
 * Icône catégorie + commentaire + date + montant coloré.
 *
 * Règle d'affichage du portage : si la transaction porte une **catégorie
 * personnalisée**, son nom/icône/couleur remplacent ceux de la catégorie par
 * défaut. Un libellé importé pas encore résolu (`importedCategoryName`) est
 * affiché avec le style par défaut des catégories perso (étiquette grise) —
 * utile dans l'aperçu d'import CSV.
 */
@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayStyle = transaction.customCategoryId
        ?.let { LocalCustomCategories.current[it] }
        ?: transaction.importedCategoryName?.let { CustomCategory(name = it) }
        ?: transaction.category

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StyleIconView(style = displayStyle, size = 36.dp)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.comment.ifEmpty { displayStyle.label },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            transaction.date?.let { date ->
                Text(
                    text = date.shortFormatted(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (transaction.potentiel) {
                Text(
                    text = "Potentielle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Text(
            text = transaction.amount.formattedCurrency(),
            color = if (transaction.amount >= 0) IncomeGreen else ExpenseRed,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
