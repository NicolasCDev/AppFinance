package com.example.appfinancetest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@Composable
fun ImportActionCard(
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDB_ViewModel,
    title: String = stringResource(id = R.string.add_transactions),
    description: String = stringResource(id = R.string.add_transactions_desc),
    icon: ImageVector = Icons.Default.Add,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    replacing: Boolean = false,
    onProcessingChange: (Boolean) -> Unit = {},
    onRefresh: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth(0.9f)
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            onProcessingChange(true)
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                sharedPreferences.edit { putString("selected_file_uri", uri.toString()) }

                context.contentResolver.openInputStream(it)?.let { inputStream ->
                    val transactionsData = readExcelFile(inputStream)
                    (context as? ComponentActivity)?.lifecycleScope?.launch {
                        if (replacing) {
                            databaseViewModel.deleteAllTransactions()
                        }
                        investmentViewModel.deleteAllInvestments()
                        addTransaction(transactionsData, databaseViewModel)
                        addInvestments(databaseViewModel, investmentViewModel)
                        onRefresh()
                        databaseViewModel.refreshNetWorth()
                        onProcessingChange(false)
                        onDismiss?.invoke()
                    }
                }
            } catch (e: Exception) {
                Log.e("FilePicker", "Error : ${e.message}")
                onProcessingChange(false)
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
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
