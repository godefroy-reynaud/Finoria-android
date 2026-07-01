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

/**
 * Service d'export/import CSV.
 *
 * Format — en-tête sur 4 colonnes, séparateur virgule, UTF-8, `\n` :
 * `Date,Montant,Commentaire,Catégorie`
 *
 * - `Date` : `jj/MM/aaaa` (FR) ou `N/A` si absente.
 * - `Montant` : montant **signé** (dépense < 0, revenu ≥ 0), 2 décimales,
 *   séparateur décimal **virgule** ; comme la virgule est aussi le séparateur de
 *   colonnes, le montant est **entouré de guillemets** pour rester dans une seule
 *   colonne (ex. `"-42,90"`).
 * - `Commentaire` / `Catégorie` : échappés RFC 4180 (voir [escapeCsv]).
 *
 * Export via FileProvider (partage) ou vers un URI choisi (SAF) ; import via URI.
 */
object CsvService {

    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE)
    private const val HEADER = "Date,Montant,Commentaire,Catégorie"

    /**
     * Construit le contenu texte du CSV des transactions du compte.
     *
     * On **exclut** les transactions potentielles et celles générées par une
     * récurrence. Tri par date décroissante (sans date en dernier).
     * Retourne null s'il n'y a rien à exporter.
     */
    fun buildCsvContent(transactions: List<Transaction>): String? {
        val exportable = transactions.filter { !it.potentiel && it.recurringTransactionId == null }
        if (exportable.isEmpty()) return null

        val sorted = exportable.sortedByDescending { it.date ?: LocalDate.MIN }

        val sb = StringBuilder()
        sb.append(HEADER).append('\n')
        for (tx in sorted) {
            val dateStr = tx.date?.format(formatter) ?: "N/A"
            // Montant signé, décimale = virgule → escapeCsv l'entoure de guillemets.
            val amount = String.format(Locale.FRANCE, "%.2f", tx.amount)
            sb.append(dateStr).append(',')
                .append(escapeCsv(amount)).append(',')
                .append(escapeCsv(tx.comment)).append(',')
                .append(escapeCsv(tx.category.labelText)).append('\n')
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
     *
     * Parsing RFC 4180 (respecte les guillemets/virgules échappés). Le montant est
     * déjà **signé** dans le fichier (dépense < 0, revenu ≥ 0) — il n'y a plus de
     * colonne `Type`. La catégorie est résolue par correspondance de libellé avec
     * les catégories par défaut ; un libellé inconnu retombe sur `Autre`. Les
     * transactions importées sont validées ; une date `N/A` est remplacée par la
     * date du jour.
     */
    fun importCsv(uri: Uri, context: Context): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.readLine() // Ignore l'en-tête
            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val parts = parseCsvLine(line)
                if (parts.size < 2) return@forEachLine
                try {
                    val date = try {
                        LocalDate.parse(parts[0].trim(), formatter)
                    } catch (_: Exception) {
                        null
                    }
                    // Décimale virgule → on repasse au point pour toDouble ; le
                    // signe est déjà porté par la valeur.
                    val amount = parts[1].trim().replace(",", ".").toDoubleOrNull()
                        ?: return@forEachLine
                    val comment = parts.getOrNull(2) ?: ""
                    val category = parts.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() }
                        ?.let { label -> TransactionCategory.entries.find { it.labelText == label } }
                        ?: TransactionCategory.OTHER

                    transactions.add(
                        Transaction(
                            amount = amount,
                            comment = comment,
                            potentiel = false,
                            date = date ?: LocalDate.now(),
                            category = category
                        )
                    )
                } catch (_: Exception) {
                    // Ligne malformée ignorée
                }
            }
        }

        return transactions
    }

    /**
     * Échappe un champ selon RFC 4180 : si le champ contient une virgule, un
     * guillemet ou un saut de ligne, il est entouré de guillemets et les
     * guillemets internes sont doublés.
     */
    private fun escapeCsv(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    /**
     * Découpe une ligne CSV en respectant les guillemets RFC 4180 (ne coupe pas
     * au milieu d'un champ échappé). À l'intérieur des guillemets, `""` = un
     * guillemet littéral ; une virgule hors guillemets sépare deux champs.
     */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    current.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        fields.add(current.toString())
                        current.setLength(0)
                    }
                    else -> current.append(c)
                }
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }
}
