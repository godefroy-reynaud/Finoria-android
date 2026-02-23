package com.finoria.app.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * Champ de saisie de montant avec suffix €.
 * Filtre les entrées pour n'accepter que les nombres décimaux.
 */
@Composable
fun CurrencyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Montant"
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            // Accept only digits, dots, and commas (max 2 decimal places)
            val sanitized = text.replace(",", ".")
            if (sanitized.isEmpty() || sanitized.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                onValueChange(sanitized)
            }
        },
        label = { Text(label) },
        suffix = { Text("€") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}
