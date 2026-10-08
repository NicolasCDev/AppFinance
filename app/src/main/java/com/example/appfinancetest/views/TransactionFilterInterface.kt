package com.example.appfinancetest.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.appfinancetest.R
import com.example.appfinancetest.ui.theme.RedAccent

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
    onDismiss: () -> Unit,
) {
    // Use of TextFieldValue in order to manage the cursor position when formatting
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
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(id = R.string.filter_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.close))
                    }
                }

                // Date Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = dateMinState,
                        onValueChange = { newValue ->
                            val formatted = formatDateInput(newValue.text, dateMinState.text)
                            var newSelection = newValue.selection
                            if (formatted.length > newValue.text.length) {
                                val diff = formatted.length - newValue.text.length
                                newSelection = TextRange(newSelection.end + diff)
                            }
                            dateMinState = newValue.copy(text = formatted, selection = newSelection)
                            onDateMinFilterChange(formatted)
                        },
                        label = { Text(stringResource(id = R.string.filter_after)) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(id = R.string.date_placeholder)) },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (dateMinState.text.isNotEmpty()) {
                                IconButton(onClick = {
                                    dateMinState = TextFieldValue("")
                                    onDateMinFilterChange("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
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
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (dateMaxState.text.isNotEmpty()) {
                                IconButton(onClick = {
                                    dateMaxState = TextFieldValue("")
                                    onDateMaxFilterChange("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Category Dropdown
                FilterDropdown(
                    label = stringResource(id = R.string.filter_category),
                    selectedOption = categoryFilter,
                    options = categories,
                    onOptionSelected = onCategoryFilterChange,
                    leadingIcon = Icons.Default.Category
                )

                // Item Dropdown
                FilterDropdown(
                    label = stringResource(id = R.string.filter_item),
                    selectedOption = itemFilter,
                    options = items,
                    onOptionSelected = onItemFilterChange,
                    leadingIcon = Icons.Default.Receipt
                )

                // Label Dropdown
                FilterDropdown(
                    label = stringResource(id = R.string.filter_label),
                    selectedOption = labelFilter,
                    options = labels,
                    onOptionSelected = onLabelFilterChange,
                    leadingIcon = Icons.Default.Bookmark
                )

                // Amount Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = amountMinFilter,
                        onValueChange = onAmountMinFilterChange,
                        label = { Text(stringResource(id = R.string.filter_amount_min)) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (amountMinFilter.isNotEmpty()) {
                                IconButton(onClick = { onAmountMinFilterChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amountMaxFilter,
                        onValueChange = onAmountMaxFilterChange,
                        label = { Text(stringResource(id = R.string.filter_amount_max)) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (amountMaxFilter.isNotEmpty()) {
                                IconButton(onClick = { onAmountMaxFilterChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onClearAll,
                        colors = ButtonDefaults.textButtonColors(contentColor = RedAccent)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(id = R.string.filter_delete_all),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
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
        if ((i == 1) || (i == 3)) {
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
    onOptionSelected: (String) -> Unit,
    leadingIcon: ImageVector
) {
    var expanded by remember { mutableStateOf(false) }
    val allLabel = stringResource(id = R.string.filter_all)
    
    Box(modifier = Modifier.fillMaxWidth()) {
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
                leadingIcon = {
                    Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedOption.isNotEmpty()) {
                            IconButton(onClick = { onOptionSelected("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                shape = RoundedCornerShape(12.dp),
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
