package com.example.appfinancetest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

@Composable
fun CreditScreen(
    creditViewModel: CreditDB_ViewModel,
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
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mes Crédits",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { 
                            isAddDialog = true
                            showAddEditDialog = CreditDB(label = "", dateBegin = 0.0, totalAmount = 0.0, monthlyPayment = 0.0, interestRate = 0.0, idInvest = "")
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Ajouter")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (credits.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Aucun crédit enregistré", color = Color.Gray)
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

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Fermer")
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
                        text = credit.label ?: "Sans nom",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Début : ${dateFormattedText(credit.dateBegin ?: 0.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    if (!credit.idInvest.isNullOrBlank()) {
                        Text(
                            text = "ID Invest : ${credit.idInvest}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editer", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    LabelValueSmall(label = "Total", value = "%.2f €".format(credit.totalAmount ?: 0.0))
                    LabelValueSmall(label = "Mensualité", value = "%.2f €".format(credit.monthlyPayment ?: 0.0))
                }
                Column(horizontalAlignment = Alignment.End) {
                    LabelValueSmall(label = "Taux", value = "%.2f %%".format(credit.interestRate ?: 0.0))
                }
            }
        }
    }
}

@Composable
fun LabelValueSmall(label: String, value: String) {
    Row {
        Text("$label : ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
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
    var totalAmount by remember { mutableStateOf(credit.totalAmount?.toString() ?: "") }
    var monthlyPayment by remember { mutableStateOf(credit.monthlyPayment?.toString() ?: "") }
    var interestRate by remember { mutableStateOf(credit.interestRate?.toString() ?: "") }
    var idInvest by remember { mutableStateOf(credit.idInvest ?: "") }
    var dateBegin by remember { mutableStateOf(credit.dateBegin ?: 0.0) }
    
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (isAdd) "Ajouter un crédit" else "Modifier le crédit",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nom du crédit") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = idInvest,
                    onValueChange = { idInvest = it },
                    label = { Text("ID Investissement") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { totalAmount = it },
                    label = { Text("Somme totale") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = monthlyPayment,
                    onValueChange = { monthlyPayment = it },
                    label = { Text("Mensualité") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = interestRate,
                    onValueChange = { interestRate = it },
                    label = { Text("Taux d'intérêt (%)") },
                    modifier = Modifier.fillMaxWidth()
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
                        text = "Début : ${dateFormattedText(dateBegin)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onSave(credit.copy(
                            label = label,
                            totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                            monthlyPayment = monthlyPayment.toDoubleOrNull() ?: 0.0,
                            interestRate = interestRate.toDoubleOrNull() ?: 0.0,
                            dateBegin = dateBegin,
                            idInvest = idInvest
                        ))
                    }) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        // Intégration du DatePicker existant dans votre projet
        LegacyMaterialDateRangePicker(
            onDismiss = { showDatePicker = false },
            onDateSelected = { start, _ ->
                dateBegin = start.toDouble()
                showDatePicker = false
            }
        )
    }
}
