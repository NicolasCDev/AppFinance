package com.example.appfinancetest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun TransactionFilterInterface(
    dateMinFilter: String,
    onDateMinFilterChange: (String) -> Unit,
    dateMaxFilter: String,
    onDateMaxFilterChange: (String) -> Unit,
    categoryFilter: String,
    onCategoryFilterChange: (String) -> Unit,
    categories: List<String>,
    itemFilter: String,
    onItemFilterChange: (String) -> Unit,
    items: List<String>,
    labelFilter: String,
    onLabelFilterChange: (String) -> Unit,
    labels: List<String>,
    amountMinFilter: String,
    onAmountMinFilterChange: (String) -> Unit,
    amountMaxFilter: String,
    onAmountMaxFilterChange: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    // Use of TextFieldValue in order to manage the cursor position when formating
    var dateMinState by remember { mutableStateOf(TextFieldValue(dateMinFilter, TextRange(dateMinFilter.length))) }
    var dateMaxState by remember { mutableStateOf(TextFieldValue(dateMaxFilter, TextRange(dateMaxFilter.length))) }

    // Synchronisation si le state externe change (ex: reset)
    LaunchedEffect(dateMinFilter) {
        if (dateMinState.text != dateMinFilter) {
            dateMinState = TextFieldValue(dateMinFilter, TextRange(dateMinFilter.length))
        }
    }
    LaunchedEffect(dateMaxFilter) {
        if (dateMaxState.text != dateMaxFilter) {
            dateMaxState = TextFieldValue(dateMaxFilter, TextRange(dateMaxFilter.length))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(id = R.string.filter_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.close))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateMinState,
                        onValueChange = { newValue ->
                            val formatted = formatDateInput(newValue.text, dateMinState.text)
                            var newSelection = newValue.selection
                            // If we added a "/" --> we move right the cursor
                            if (formatted.length > newValue.text.length) {
                                val diff = formatted.length - newValue.text.length
                                newSelection = TextRange(newSelection.end + diff)
                            }
                            dateMinState = newValue.copy(text = formatted, selection = newSelection)
                            onDateMinFilterChange(formatted)
                        },
                        label = { Text(stringResource(id = R.string.filter_after))},
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(id = R.string.date_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = dateMaxState,
                        onValueChange = { newValue ->
                            val formatted = formatDateInput(newValue.text, dateMaxState.text)
                            var newSelection = newValue.selection
                            if (formatted.length > newValue.text.length) {
                                val diff = formatted.length - newValue.text.length
                                newSelection = TextRange(newSelection.end + diff)
                            }
                            dateMaxState = newValue.copy(text = formatted, selection = newSelection)
                            onDateMaxFilterChange(formatted)
                        },
                        label = { Text(stringResource(id = R.string.filter_before)) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(id = R.string.date_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                FilterDropdown(
                    label = stringResource(id = R.string.filter_category),
                    selectedOption = categoryFilter,
                    options = categories,
                    onOptionSelected = onCategoryFilterChange
                )

                FilterDropdown(
                    label = stringResource(id = R.string.filter_item),
                    selectedOption = itemFilter,
                    options = items,
                    onOptionSelected = onItemFilterChange
                )

                FilterDropdown(
                    label = stringResource(id = R.string.filter_label),
                    selectedOption = labelFilter,
                    options = labels,
                    onOptionSelected = onLabelFilterChange
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountMinFilter,
                        onValueChange = onAmountMinFilterChange,
                        label = { Text(stringResource(id = R.string.filter_amount_min)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = amountMaxFilter,
                        onValueChange = onAmountMaxFilterChange,
                        label = { Text(stringResource(id = R.string.filter_amount_max)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onClearAll,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(id = R.string.filter_delete_all),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(
                        onClick = onDismiss
                    ) {
                        Text(
                            stringResource(id = R.string.filter_apply),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun formatDateInput(input: String, previousValue: String): String {
    // If we delete, we don't reformat in order to allow correction
    if (input.length < previousValue.length) return input
    
    val clean = input.replace("/", "")
    val sb = StringBuilder()
    
    for (i in clean.indices) {
        sb.append(clean[i])
        // Adding "/" after 2nd and 4th character
        if (i == 1 || i == 3) {
            sb.append("/")
        }
    }
    
    return sb.toString().take(8)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val allLabel = stringResource(id = R.string.filter_all)
    
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedOption.ifEmpty { allLabel },
                onValueChange = { },
                readOnly = true,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            
            if (options.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(allLabel) },
                        onClick = {
                            onOptionSelected("")
                            expanded = false
                        }
                    )
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
