package com.example.appfinancetest

import android.content.Context
import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // État local pour afficher l'URI
    var selectedFileUri by remember {
        mutableStateOf(sharedPreferences.getString("selected_file_uri", "") ?: "")
    }

    // Launcher pour ouvrir le sélecteur de fichiers
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("DashboardScreen", "Fichier sélectionné : $uri")

            // Sauvegarder l'URI dans SharedPreferences
            sharedPreferences.edit().putString("selected_file_uri", uri.toString()).apply()

            // Mettre à jour l'état local
            selectedFileUri = uri.toString()
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Greeting(name = "Tableau de bord")

            Spacer(modifier = Modifier.padding(8.dp))

            Button(onClick = {
                filePickerLauncher.launch(arrayOf("*/*"))
            }) {
                Text("Choisir un fichier")
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Text(
                text = if (selectedFileUri.isNotEmpty()) "Fichier sélectionné : $selectedFileUri" else "Aucun fichier sélectionné",
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp
            )
        }
    }
}
