package com.finoria.data

import android.content.Context
import com.finoria.model.Transaction
import com.finoria.model.TransactionCategory
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate

object CsvService {
    fun generateCsv(transactions: List<Transaction>, accountName: String): String {
        val builder = StringBuilder()
        builder.append("Date;Amount;Category;Comment\n")
        transactions.forEach {
            builder.append("${it.date};${it.amount};${it.category.name};${it.comment}\n")
        }
        return builder.toString()
    }

    fun importCsv(inputStream: InputStream): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.readLine() // Skip header
        
        reader.forEachLine { line ->
            val parts = line.split(";")
            if (parts.size >= 3) {
                try {
                    transactions.add(Transaction(
                        amount = parts[1].toDouble(),
                        date = LocalDate.parse(parts[0]),
                        category = TransactionCategory.valueOf(parts[2]),
                        comment = parts.getOrNull(3) ?: ""
                    ))
                } catch (e: Exception) { /* Ignorer les lignes mal formées */ }
            }
        }
        return transactions
    }

    fun saveCsvToFile(context: Context, csvContent: String): File? {
        return try {
            val csvDir = File(context.cacheDir, "csv")
            csvDir.mkdirs()
            val file = File(csvDir, "finoria_export_${System.currentTimeMillis()}.csv")
            FileOutputStream(file).use {
                it.write(csvContent.toByteArray())
            }
            file
        } catch (e: Exception) {
            null
        }
    }
}
