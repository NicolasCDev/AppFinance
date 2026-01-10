package com.example.appfinancetest

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import java.io.File

enum class PendingCloudAction { NONE, BACKUP, RESTORE }

@Composable
fun ImportExportInterface(
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    creditViewModel: CreditDBViewModel,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var showCreditScreen by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf(PendingCloudAction.NONE) }
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val driveService = remember { GoogleDriveService(context) }

    fun executeBackup() {
        isProcessing = true
        lifecycleOwner.lifecycleScope.launch {
            val dbFile = context.getDatabasePath("FinanceDB")
            if (dbFile.exists()) {
                val result = driveService.uploadBackup(dbFile)
                if (result != null) {
                    Toast.makeText(context, "Backup successful !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Backup failure", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Database not found", Toast.LENGTH_LONG).show()
            }
            isProcessing = false
            pendingAction = PendingCloudAction.NONE
            onDismiss()
        }
    }

    fun executeRestore() {
        isProcessing = true
        lifecycleOwner.lifecycleScope.launch {
            val tempFile = File(context.cacheDir, "temp_db")
            if (driveService.downloadBackup(tempFile)) {
                val dbFile = context.getDatabasePath("FinanceDB")
                try {
                    tempFile.copyTo(dbFile, overwrite = true)
                    Toast.makeText(context, "Successful restoration !", Toast.LENGTH_SHORT).show()
                    onRefresh()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error during copying : ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "No backup found in the Cloud", Toast.LENGTH_LONG).show()
            }
            isProcessing = false
            pendingAction = PendingCloudAction.NONE
            onDismiss()
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("ImportExport", "Google Sign-In successful")
            when (pendingAction) {
                PendingCloudAction.BACKUP -> executeBackup()
                PendingCloudAction.RESTORE -> executeRestore()
                else -> {}
            }
        } else {
            // Analyze the actual error
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                task.getResult(ApiException::class.java)
            } catch (e: ApiException) {
                val errorMsg = "Google error (Code ${e.statusCode})"
                Log.e("ImportExport", errorMsg, e)
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("ImportExport", "Sign-in cancelled or failed", e)
                Toast.makeText(context, "Canceled connexion", Toast.LENGTH_SHORT).show()
            }
            pendingAction = PendingCloudAction.NONE
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            isProcessing = true
            lifecycleOwner.lifecycleScope.launch {
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
                    Toast.makeText(context, "Excel export successful", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("Export", "Error: ${e.message}")
                    Toast.makeText(context, "Export error : ${e.message}", Toast.LENGTH_LONG).show()
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
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(id = R.string.data_management_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (!isProcessing) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.close))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .graphicsLayer(alpha = if (isProcessing) 0.4f else 1f)
                            .then(if (isProcessing) Modifier.pointerInput(Unit) {} else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ImportActionCard(
                            databaseViewModel = databaseViewModel,
                            investmentViewModel = investmentViewModel,
                            creditViewModel = creditViewModel,
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
                            creditViewModel = creditViewModel,
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
                            title = stringResource(id = R.string.cloud_backup),
                            description = stringResource(id = R.string.cloud_backup_desc),
                            icon = Icons.Default.CloudUpload,
                            color = Color(0xFFE3F2FD),
                            onClick = {
                                val account = GoogleSignIn.getLastSignedInAccount(context)
                                if (account == null) {
                                    pendingAction = PendingCloudAction.BACKUP
                                    googleSignInLauncher.launch(driveService.getSignInIntent())
                                } else {
                                    executeBackup()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GenericActionCard(
                            title = stringResource(id = R.string.cloud_restore),
                            description = stringResource(id = R.string.cloud_restore_desc),
                            icon = Icons.Default.CloudDownload,
                            color = Color(0xFFF1F8E9),
                            onClick = {
                                val account = GoogleSignIn.getLastSignedInAccount(context)
                                if (account == null) {
                                    pendingAction = PendingCloudAction.RESTORE
                                    googleSignInLauncher.launch(driveService.getSignInIntent())
                                } else {
                                    executeRestore()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GenericActionCard(
                            title = stringResource(id = R.string.manage_credits),
                            description = stringResource(id = R.string.manage_credits_desc),
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

                    if (isProcessing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(id = R.string.processing), color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
