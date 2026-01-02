package com.example.appfinancetest

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@Composable
fun ImportExportInterface(
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDB_ViewModel,
    creditViewModel: CreditDB_ViewModel,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var showCreditScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            isProcessing = true
            (context as? ComponentActivity)?.lifecycleScope?.launch {
                try {
                    val allTransactionsRaw = databaseViewModel.getTransactionsSortedByDateASC()
                    val transactions = allTransactionsRaw.map { t ->
                        Transaction(
                            date = t.date ?: 0.0,
                            category = t.category ?: "",
                            item = t.item ?: "",
                            label = t.label ?: "",
                            amount = t.amount ?: 0.0,
                            variation = t.variation ?: 0.0,
                            balance = t.balance ?: 0.0,
                            idInvest = t.idInvest
                        )
                    }
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        writeExcelFile(outputStream, transactions)
                    }
                } catch (e: Exception) {
                    Log.e("Export", "Error: ${e.message}")
                }
                isProcessing = false
                onDismiss()
            }
        }
    }

    if (showCreditScreen) {
        CreditScreen(
            creditViewModel = creditViewModel,
            onDismiss = { showCreditScreen = false }
        )
    }

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(id = R.string.data_management_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isProcessing) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.close))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(id = R.string.processing), color = Color.Gray)
                } else {
                    ImportActionCard(
                        databaseViewModel = databaseViewModel,
                        investmentViewModel = investmentViewModel,
                        title = stringResource(id = R.string.add_transactions),
                        description = stringResource(id = R.string.add_transactions_desc),
                        icon = Icons.Default.Add,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onProcessingChange = { isProcessing = it },
                        onRefresh = onRefresh,
                        onDismiss = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ImportActionCard(
                        databaseViewModel = databaseViewModel,
                        investmentViewModel = investmentViewModel,
                        title = stringResource(id = R.string.replace_database),
                        description = stringResource(id = R.string.replace_database_desc),
                        icon = Icons.Default.Refresh,
                        color = MaterialTheme.colorScheme.errorContainer,
                        replacing = true,
                        onProcessingChange = { isProcessing = it },
                        onRefresh = onRefresh,
                        onDismiss = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GenericActionCard(
                        title = "Gérer les Crédits",
                        description = "Ajouter ou modifier vos crédits en cours.",
                        icon = Icons.Default.CreditCard,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = { showCreditScreen = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GenericActionCard(
                        title = stringResource(id = R.string.export_excel),
                        description = stringResource(id = R.string.export_excel_desc),
                        icon = Icons.Default.Share,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = {
                            exportFileLauncher.launch("finance_export.xlsx")
                        }
                    )
                }
            }
        }
    }
}
