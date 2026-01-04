package com.example.appfinancetest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@Composable
fun ImportActionCard(
    modifier: Modifier = Modifier.fillMaxWidth(0.9f),
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel? = null,
    creditViewModel: CreditDBViewModel? = null,
    title: String = stringResource(id = R.string.add_transactions),
    description: String = stringResource(id = R.string.add_transactions_desc),
    icon: ImageVector = Icons.Default.Add,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    replacing: Boolean = false,
    onProcessingChange: (Boolean) -> Unit = {},
    onRefresh: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPreferences = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            onProcessingChange(true)
            lifecycleOwner.lifecycleScope.launch {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    sharedPreferences.edit { putString("selected_file_uri", uri.toString()) }

                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        // Reading transactions
                        val transactionsData = readExcelFile(inputStream)
                        
                        if (replacing) {
                            databaseViewModel.deleteAllTransactions()
                            investmentViewModel?.deleteAllInvestments()
                            creditViewModel?.deleteAllCredits()
                        }
                        
                        // Adding transactions in database
                        addTransaction(transactionsData, databaseViewModel)
                        
                        // Adding investments from transactions
                        if (investmentViewModel != null) {
                            addInvestments(databaseViewModel, investmentViewModel)
                        }

                        // Adding credits from transactions
                        if (creditViewModel != null) {
                            addCredits(databaseViewModel, creditViewModel)
                        }
                        
                        onRefresh()
                        databaseViewModel.refreshNetWorth()
                        onProcessingChange(false)
                        onDismiss?.invoke()
                    }
                } catch (e: Exception) {
                    Log.e("FilePicker", "Error : ${e.message}")
                    onProcessingChange(false)
                }
            }
        }
    }

    GenericActionCard(
        title = title,
        description = description,
        icon = icon,
        color = color,
        onClick = {
            filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        },
        modifier = modifier
    )
}

@Composable
fun GenericActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Start)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
