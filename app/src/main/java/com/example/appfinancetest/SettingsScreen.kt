package com.example.appfinancetest

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.os.LocaleListCompat
import androidx.core.content.edit

@Composable
fun SettingsScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var selectedFileUri by remember {
        mutableStateOf(sharedPreferences.getString("selected_file_uri", "") ?: "")
    }

    //Read the saved language from SharedPreferences
    val savedLanguage = remember {
        sharedPreferences.getString("app_language", 
            AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "fr"
        ) ?: "fr"
    }
    var tempLanguage by remember { mutableStateOf(savedLanguage) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            sharedPreferences.edit { putString("selected_file_uri", uri.toString()) }
            selectedFileUri = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(id = R.string.parameters_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Language Section
                Text(
                    text = stringResource(id = R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = tempLanguage == "fr",
                        onClick = { 
                            tempLanguage = "fr"
                            // Save in SharedPreferences
                            sharedPreferences.edit { putString("app_language", "fr") }
                            // Appliquer le changement
                            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("fr")
                            AppCompatDelegate.setApplicationLocales(appLocale)
                        }
                    )
                    Text("Français")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = tempLanguage == "en",
                        onClick = { 
                            tempLanguage = "en"
                            // Save in SharedPreferences
                            sharedPreferences.edit { putString("app_language", "en") }
                            // Apply the modification
                            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
                            AppCompatDelegate.setApplicationLocales(appLocale)
                        }
                    )
                    Text("English")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)

                // File Section
                Text(
                    text = stringResource(id = R.string.database),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.choose_file))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (selectedFileUri.isNotEmpty()) 
                        stringResource(id = R.string.file_selected, selectedFileUri) 
                    else 
                        stringResource(id = R.string.no_file_selected),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(id = R.string.close))
                }
            }
        }
    }
}
