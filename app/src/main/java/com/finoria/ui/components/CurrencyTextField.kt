package com.finoria.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun CurrencyTextField(
    value: Double,
    onValueChange: (Double) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { mutableStateOf(if (value == 0.0) "" else value.toString()) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            if (newValue.isEmpty()) {
                textValue = ""
                onValueChange(0.0)
            } else if (newValue.toDoubleOrNull() != null) {
                textValue = newValue
                onValueChange(newValue.toDouble())
            }
        },
        label = { Text(label) },
        suffix = { Text("€") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}
