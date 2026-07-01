package com.finoria.app.domain.service

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.TransactionCategory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Service d'export/import CSV.
 * Export via FileProvider, import via URI (ACTION_OPEN_DOCUMENT).
 */
object CsvService {

    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE)

    /**
     * Construit le contenu texte du CSV des transactions.
     * Retourne null si la liste est vide.
     */
    fun buildCsvContent(transactions: List<Transaction>): String? {
        val sorted = transactions.sortedByDescending { it.date }
        if (sorted.isEmpty()) return null

        val sb = StringBuilder("Date,Type,Montant,Commentaire,Statut,Catégorie\n")

        for (tx in sorted) {
            val dateStr = tx.date?.format(formatter) ?: "N/A"
            val type = if (tx.amount >= 0) "Revenu" else "Dépense"
            val amount = String.format(Locale.FRANCE, "%.2f", abs(tx.amount))
            val comment = tx.comment.replace(",", ";")
            val status = if (tx.potentiel) "Potentielle" else "Validée"
            val category = tx.category.labelText
            sb.appendLine("$dateStr,$type,$amount,$comment,$status,$category")
        }

        return sb.toString()
    }

    /**
     * Génère un fichier CSV et retourne l'URI via FileProvider pour le partage.
     */
    fun generateCsv(
        transactions: List<Transaction>,
        accountName: String,
        context: Context
    ): Uri? {
        val content = buildCsvContent(transactions) ?: return null

        val csvDir = File(context.cacheDir, "csv")
        csvDir.mkdirs()
        val file = File(csvDir, "${accountName}_transactions_${System.currentTimeMillis()}.csv")
        file.writeText(content)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    /**
     * Écrit le contenu CSV dans un URI choisi par l'utilisateur via le
     * sélecteur de fichiers Android (Storage Access Framework).
     * Permet d'enregistrer directement dans le gestionnaire de fichiers du téléphone.
     * Retourne true si l'écriture a réussi.
     */
    fun writeCsvToUri(
        uri: Uri,
        transactions: List<Transaction>,
        context: Context
    ): Boolean {
        val content = buildCsvContent(transactions) ?: return false
        return try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            } != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Importe les transactions depuis un fichier CSV (URI).
     */
    fun importCsv(uri: Uri, context: Context): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.readLine() // Skip header
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 4) {
                    try {
                        val date = try {
                            LocalDate.parse(parts[0].trim(), formatter)
                        } catch (_: Exception) {
                            null
                        }
                        val amount = parts[2].trim().replace(",", ".").toDoubleOrNull() ?: return@forEachLine
                        val signedAmount = if (parts[1].trim() == "Dépense") -abs(amount) else abs(amount)
                        val comment = parts[3].trim().replace(";", ",")
                        val isPotential = parts.getOrNull(4)?.trim() == "Potentielle"
                        val category = parts.getOrNull(5)?.trim()?.let { label ->
                            TransactionCategory.entries.find { it.labelText == label }
                        } ?: TransactionCategory.OTHER

                        transactions.add(
                            Transaction(
                                amount = signedAmount,
                                comment = comment,
                                potentiel = isPotential,
                                date = date,
                                category = category
                            )
                        )
                    } catch (_: Exception) {
                        // Ignore malformed lines
                    }
                }
            }
        }

        return transactions
    }
}
