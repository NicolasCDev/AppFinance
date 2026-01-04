package com.example.appfinancetest

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import java.util.Calendar

@Composable
fun SettingsScreen(
    databaseViewModel: DataBaseViewModel? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Show/Hide goals
    var showGoals by remember {
        mutableStateOf(sharedPreferences.getBoolean("show_goals", true))
    }

    // Annual Interest Rate
    var annualInterestRate by remember {
        val savedRate = sharedPreferences.getFloat("goal_annual_interest_rate", 5.0f)
        mutableStateOf(savedRate.toString())
    }

    // Annual Invested Amount
    var annualInvestedAmount by remember {
        val savedAmount = sharedPreferences.getFloat("goal_annual_invested_amount", 1200.0f)
        mutableStateOf(savedAmount.toString())
    }

    // Birth Date
    var birthDate by remember {
        val savedBirthDate = sharedPreferences.getFloat("user_birth_date", 0.0f).toDouble()
        mutableDoubleStateOf(
            if (savedBirthDate == 0.0) {
                (System.currentTimeMillis() / 86400000.0) + 25569.0
            } else {
                savedBirthDate
            }
        )
    }

    // Goal Start Date
    var goalStartDate by remember {
        val savedDate = sharedPreferences.getFloat("goal_start_date", 0.0f).toDouble()
        mutableDoubleStateOf(savedDate)
    }

    // Calculated Target Age
    var calculatedTargetAge by remember { mutableIntStateOf(sharedPreferences.getInt("target_millionaire_age", 30)) }

    // Logic to calculate millionaire age
    LaunchedEffect(annualInterestRate, annualInvestedAmount, birthDate, goalStartDate, databaseViewModel) {
        val rate = annualInterestRate.toDoubleOrNull() ?: 0.0
        val investment = annualInvestedAmount.toDoubleOrNull() ?: 0.0
        
        // If goalStartDate is 0, we need to determine the default start date (first transaction or today)
        val startSimulationDate = if (goalStartDate == 0.0) {
            val firstTransDate = databaseViewModel?.getFirstTransactionDate() ?: ((System.currentTimeMillis() / 86400000.0) + 25569.0)
            goalStartDate = firstTransDate
            sharedPreferences.edit { putFloat("goal_start_date", firstTransDate.toFloat()) }
            firstTransDate
        } else {
            goalStartDate
        }

        // Get balance at the start date
        val startBalance = databaseViewModel?.getNetWorthAtDateStatic(startSimulationDate) ?: 0.0
        
        var currentWorth = startBalance
        var years = 0
        val maxYears = 100 // Safety break
        
        if (currentWorth < 1000000) {
            while (currentWorth < 1000000 && years < maxYears) {
                currentWorth = currentWorth * (1 + rate / 100.0) + investment
                years++
            }
        }
        
        val birthCalendar = Calendar.getInstance()
        val birthMillis = ((birthDate - 25569) * 86400 * 1000).toLong()
        birthCalendar.timeInMillis = birthMillis
        
        val simulationStartCalendar = Calendar.getInstance()
        val simMillis = ((startSimulationDate - 25569) * 86400 * 1000).toLong()
        simulationStartCalendar.timeInMillis = simMillis
        
        var ageAtStart = simulationStartCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        if (simulationStartCalendar.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            ageAtStart--
        }
        
        calculatedTargetAge = ageAtStart + years
        sharedPreferences.edit { putInt("target_millionaire_age", calculatedTargetAge) }
    }

    var showBirthDatePicker by remember { mutableStateOf(false) }
    var showGoalDatePicker by remember { mutableStateOf(false) }

    // Read the saved language from SharedPreferences
    val savedLanguage = remember {
        sharedPreferences.getString("app_language", 
            AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "fr"
        ) ?: "fr"
    }
    var tempLanguage by remember { mutableStateOf(savedLanguage) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Box {
                Column {
                    // Fixed Header with Title and Close Cross
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.parameters_title),
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

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        SettingsHeader(title = stringResource(id = R.string.language))
                        
                        SettingsLanguageItem(
                            label = "Français",
                            selected = tempLanguage == "fr",
                            onClick = { 
                                tempLanguage = "fr"
                                sharedPreferences.edit { putString("app_language", "fr") }
                                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("fr")
                                AppCompatDelegate.setApplicationLocales(appLocale)
                            }
                        )
                        
                        SettingsLanguageItem(
                            label = "English",
                            selected = tempLanguage == "en",
                            onClick = { 
                                tempLanguage = "en"
                                sharedPreferences.edit { putString("app_language", "en") }
                                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
                                AppCompatDelegate.setApplicationLocales(appLocale)
                            }
                        )

                        // Birth Date Picker moved out of Goals section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .clickable { showBirthDatePicker = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.birth_date),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = dateFormattedText(birthDate),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsHeader(title = stringResource(id = R.string.goals_title))

                        ListItem(
                            headlineContent = { Text(stringResource(id = R.string.show_goals), style = MaterialTheme.typography.bodyMedium) },
                            trailingContent = {
                                Switch(
                                    checked = showGoals,
                                    onCheckedChange = { 
                                        showGoals = it
                                        sharedPreferences.edit { putBoolean("show_goals", it) }
                                    }
                                )
                            }
                        )

                        if (showGoals) {
                            ListItem(
                                headlineContent = { Text(stringResource(id = R.string.target_millionaire_age),style = MaterialTheme.typography.bodyMedium) },
                                trailingContent = {
                                    Text(
                                        text = "$calculatedTargetAge ans",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            )

                            OutlinedTextField(
                                value = annualInterestRate,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                                        annualInterestRate = newValue
                                        if (newValue.isNotEmpty()) {
                                            sharedPreferences.edit { putFloat("goal_annual_interest_rate", newValue.toFloat()) }
                                        }
                                    }
                                },
                                label = { Text(stringResource(id = R.string.annual_interest_rate)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = annualInvestedAmount,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                                        annualInvestedAmount = newValue
                                        if (newValue.isNotEmpty()) {
                                            sharedPreferences.edit { putFloat("goal_annual_invested_amount", newValue.toFloat()) }
                                        }
                                    }
                                },
                                label = { Text(stringResource(id = R.string.annual_invested_amount), style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )

                            // Goal Start Date Picker
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                                    .clickable { showGoalDatePicker = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.goal_start_date),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = dateFormattedText(goalStartDate),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBirthDatePicker) {
        WheelDatePickerDialog(
            initialDate = birthDate,
            onDismiss = { showBirthDatePicker = false },
            onDateSelected = { date ->
                birthDate = date
                sharedPreferences.edit { putFloat("user_birth_date", date.toFloat()) }
                showBirthDatePicker = false
            }
        )
    }

    if (showGoalDatePicker) {
        WheelDatePickerDialog(
            initialDate = if (goalStartDate == 0.0) ((System.currentTimeMillis() / 86400000.0) + 25569.0) else goalStartDate,
            onDismiss = { showGoalDatePicker = false },
            onDateSelected = { date ->
                goalStartDate = date
                sharedPreferences.edit { putFloat("goal_start_date", date.toFloat()) }
                showGoalDatePicker = false
            }
        )
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsLanguageItem(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { 
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = null // Handled by ListItem clickable
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    )
}
