package com.example.appfinancetest

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var selectedFileUri by remember {
        mutableStateOf(sharedPreferences.getString("selected_file_uri", "") ?: "")
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            sharedPreferences.edit().putString("selected_file_uri", uri.toString()).apply()
            selectedFileUri = uri.toString()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Paramètres",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.padding(12.dp))

            Button(onClick = {
                filePickerLauncher.launch(arrayOf("*/*"))
            }) {
                Text("Choisir un fichier")
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Text(
                text = if (selectedFileUri.isNotEmpty()) "Fichier sélectionné :\n$selectedFileUri" else "Aucun fichier sélectionné",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.padding(16.dp))

            Button(onClick = { onDismiss() }) {
                Text("Fermer les paramètres")
            }
        }
    }
}