package com.finoria.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.CustomCategory
import com.finoria.app.data.model.Transaction
import com.finoria.app.ui.LocalCustomCategories
import com.finoria.app.ui.components.TransactionRow

/**
 * Écran de prévisualisation des transactions importées depuis un CSV (étape 1 de
 * l'import : rien n'est encore en base). Affiche la liste, annonce les catégories
 * personnalisées qui seront **créées automatiquement** au commit, et avertit que
 * réimporter le même fichier crée des doublons (pas de dé-duplication).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportPreviewScreen(
    transactions: List<Transaction>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Libellés inconnus → catégories qui seront créées (dé-dupliquées par nom
    // normalisé, en excluant celles qui existent déjà sur le compte).
    val existingCategories = LocalCustomCategories.current
    val categoriesToCreate = remember(transactions, existingCategories) {
        val existingKeys = existingCategories.values
            .map { CustomCategory.normalizeName(it.name) }
            .toSet()
        transactions
            .mapNotNull { it.importedCategoryName }
            .distinctBy { CustomCategory.normalizeName(it) }
            .filter { CustomCategory.normalizeName(it) !in existingKeys }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import CSV") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "${transactions.size} transaction${if (transactions.size > 1) "s" else ""} trouvée${if (transactions.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                if (categoriesToCreate.isNotEmpty()) {
                    val prefix = if (categoriesToCreate.size > 1) {
                        "${categoriesToCreate.size} catégories personnalisées seront créées"
                    } else {
                        "1 catégorie personnalisée sera créée"
                    }
                    Text(
                        text = "$prefix : ${categoriesToCreate.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                Text(
                    text = "⚠️ Réimporter un fichier déjà importé créera des doublons.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            items(transactions, key = { it.id }) { tx ->
                TransactionRow(transaction = tx)
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Importer ${transactions.size} transaction${if (transactions.size > 1) "s" else ""}")
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
