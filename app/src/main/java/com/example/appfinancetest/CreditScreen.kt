package com.example.appfinancetest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun CreditScreen(
    creditViewModel: CreditDBViewModel,
    onDismiss: () -> Unit
) {
    var credits by remember { mutableStateOf<List<CreditDB>>(emptyList()) }
    var showAddEditDialog by remember { mutableStateOf<CreditDB?>(null) }
    var isAddDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        credits = creditViewModel.getAllCredits()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.credits_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (credits.isEmpty()) {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(id = R.string.no_credits_found), style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        } else {
                            items(credits) { credit ->
                                CreditItemCard(
                                    credit = credit,
                                    onEdit = { 
                                        isAddDialog = false
                                        showAddEditDialog = credit 
                                    },
                                    onDelete = {
                                        scope.launch {
                                            creditViewModel.deleteCreditById(credit.id)
                                            refreshTrigger++
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { 
                        isAddDialog = true
                        showAddEditDialog = CreditDB(
                            label = "", 
                            dateBegin = 0.0, 
                            totalAmount = 0.0, 
                            reimbursedAmount = 0.0,
                            remainingAmount = 0.0,
                            monthlyPayment = 0.0, 
                            interestRate = 0.0, 
                            idInvest = ""
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.add_credit_button),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog != null) {
        CreditAddEditDialog(
            credit = showAddEditDialog!!,
            isAdd = isAddDialog,
            onDismiss = { showAddEditDialog = null },
            onSave = { updatedCredit ->
                scope.launch {
                    if (isAddDialog) {
                        creditViewModel.insertCredit(updatedCredit)
                    } else {
                        creditViewModel.updateCredit(updatedCredit)
                    }
                    refreshTrigger++
                    showAddEditDialog = null
                }
            }
        )
    }
}

@Composable
fun CreditItemCard(
    credit: CreditDB,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = credit.label ?: stringResource(id = R.string.no_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(id = R.string.credit_begin, dateFormattedText(credit.dateBegin ?: 0.0)),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!credit.idInvest.isNullOrBlank()) {
                        Text(
                            text = stringResource(id = R.string.credit_id_invest_label, credit.idInvest),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit_action), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_action), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    LabelValueSmall(label = stringResource(id = R.string.total_borrowed), value = "%.2f €".format(credit.totalAmount ?: 0.0))
                    LabelValueSmall(label = stringResource(id = R.string.reimbursed_amount), value = "%.2f €".format(credit.reimbursedAmount ?: 0.0))
                    LabelValueSmall(label = stringResource(id = R.string.remaining_to_pay), value = "%.2f €".format(credit.remainingAmount ?: 0.0))
                }
                Column(horizontalAlignment = Alignment.End) {
                    LabelValueSmall(label = stringResource(id = R.string.monthly_payment), value = "%.2f €".format(credit.monthlyPayment ?: 0.0))
                    LabelValueSmall(label = stringResource(id = R.string.interest_rate), value = "%.2f %%".format(credit.interestRate ?: 0.0))
                }
            }
            
            val progress = remember(credit.totalAmount, credit.reimbursedAmount) {
                val total = credit.totalAmount ?: 1.0
                val reimbursed = credit.reimbursedAmount ?: 0.0
                if (total > 0) (reimbursed / total).toFloat().coerceIn(0f, 1f) else 0f
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun LabelValueSmall(label: String, value: String) {
    Row {
        Text("$label : ", style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CreditAddEditDialog(
    credit: CreditDB,
    isAdd: Boolean,
    onDismiss: () -> Unit,
    onSave: (CreditDB) -> Unit
) {
    var label by remember { mutableStateOf(credit.label ?: "") }
    var totalAmount by remember { mutableStateOf(if (isAdd) "" else credit.totalAmount?.toString() ?: "") }
    var reimbursedAmount by remember { mutableStateOf(if (isAdd) "" else credit.reimbursedAmount?.toString() ?: "") }
    var remainingAmount by remember { mutableStateOf(if (isAdd) "" else credit.remainingAmount?.toString() ?: "") }
    var monthlyPayment by remember { mutableStateOf(if (isAdd) "" else credit.monthlyPayment?.toString() ?: "") }
    var interestRate by remember { mutableStateOf(if (isAdd) "" else credit.interestRate?.toString() ?: "") }
    var idInvest by remember { mutableStateOf(credit.idInvest ?: "") }
    
    // Default to current date if none provided
    var dateBegin by remember {
        mutableDoubleStateOf(
            if (credit.dateBegin == null || credit.dateBegin == 0.0) {
                (System.currentTimeMillis() / 86400000.0) + 25569.0
            } else {
                credit.dateBegin
            }
        ) 
    }
    
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()), 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isAdd) stringResource(id = R.string.add_credit_dialog_title) else stringResource(id = R.string.edit_credit_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(id = R.string.credit_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = idInvest,
                    onValueChange = { idInvest = it },
                    label = { Text(stringResource(id = R.string.id_invest_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { 
                        totalAmount = it
                        // Auto-calculate remaining if total changed
                        val t = it.toDoubleOrNull() ?: 0.0
                        val r = reimbursedAmount.toDoubleOrNull() ?: 0.0
                        remainingAmount = (t - r).toString()
                    },
                    label = { Text(stringResource(id = R.string.total_borrowed)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = reimbursedAmount,
                    onValueChange = { 
                        reimbursedAmount = it
                        // Auto-calculate remaining if reimbursed changed
                        val t = totalAmount.toDoubleOrNull() ?: 0.0
                        val r = it.toDoubleOrNull() ?: 0.0
                        remainingAmount = (t - r).toString()
                    },
                    label = { Text(stringResource(id = R.string.reimbursed_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = remainingAmount,
                    onValueChange = { remainingAmount = it },
                    label = { Text(stringResource(id = R.string.remaining_to_pay)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = monthlyPayment,
                    onValueChange = { monthlyPayment = it },
                    label = { Text(stringResource(id = R.string.monthly_payment)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = interestRate,
                    onValueChange = { interestRate = it },
                    label = { Text(stringResource(id = R.string.interest_rate)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.credit_begin, dateFormattedText(dateBegin)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCC0000))
                    ) { 
                        Text(stringResource(id = R.string.cancel), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onSave(credit.copy(
                                label = label,
                                totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                                reimbursedAmount = reimbursedAmount.toDoubleOrNull() ?: 0.0,
                                remainingAmount = remainingAmount.toDoubleOrNull() ?: 0.0,
                                monthlyPayment = monthlyPayment.toDoubleOrNull() ?: 0.0,
                                interestRate = interestRate.toDoubleOrNull() ?: 0.0,
                                dateBegin = dateBegin,
                                idInvest = idInvest
                            ))
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF669900))
                    ) {
                        Text(stringResource(id = R.string.save_button), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        WheelDatePickerDialog(
            initialDate = dateBegin,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                dateBegin = date
                showDatePicker = false
            }
        )
    }
}
