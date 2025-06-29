package com.example.appfinancetest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.content.edit

@Composable
fun ImportExportInterface(
    databaseViewModel: DataBase_ViewModel,
    investmentViewModel: InvestmentDB_ViewModel,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    var replacing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var selectedFileUri by remember {
        mutableStateOf(sharedPreferences.getString("selected_file_uri", "") ?: "")
    }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                Log.d("Picker", "URI reçue : $uri")
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                sharedPreferences.edit { putString("selected_file_uri", uri.toString()) }
                selectedFileUri = uri.toString()

                context.contentResolver.openInputStream(it)?.let { inputStream ->
                    val file = readExcelFile(inputStream)
                    (context as? ComponentActivity)?.lifecycleScope?.launch {
                        if (replacing) {
                            databaseViewModel.deleteAllTransactions()
                            replacing = false
                        }
                        investmentViewModel.deleteAllInvestments()
                        addTransaction(file, databaseViewModel)
                        addInvestments(databaseViewModel, investmentViewModel)
                        onRefresh()
                        onDismiss()
                    }
                }
            } catch (e: SecurityException) {
                Log.e("FilePicker", "Error : ${e.message}")
            }
        }
    }
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { /* TODO: Write Excel file */ }
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 600.dp)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Import / Export Database",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                Button(onClick = {
                    (context as? ComponentActivity)?.lifecycleScope?.launch {
                        filePickerLauncher.launch(
                            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        )
                    }
                }) { Text("Add") }

                Spacer(modifier = Modifier.padding(10.dp))

                Button(onClick = {
                    (context as? ComponentActivity)?.lifecycleScope?.launch {
                        replacing = true
                        // We launch the filePicker
                        filePickerLauncher.launch(
                            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        )

                    }
                }) { Text("Replace") }

                Spacer(modifier = Modifier.padding(10.dp))

                Button(onClick = {
                    exportFileLauncher.launch("export.xlsx")
                }) { Text("Export") }
            }
        }

    }
}