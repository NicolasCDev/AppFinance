package com.example.appfinancetest

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DateRangeOption(val title: String) {
    LAST_DAY("Dernier jour"),
    LAST_WEEK("Dernière semaine"),
    LAST_MONTH("Dernier mois"),
    LAST_6_MONTHS("6 derniers mois"),
    LAST_YEAR("Dernière année"),
    LAST_5_YEARS("5 dernières années"),
    ALL_TIME("Tout l'historique")
}

@Composable
fun DateRangeSelector(
    selectedOption: DateRangeOption,
    onOptionChange: (DateRangeOption) -> Unit,
    onCalendarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Calendar Button
        IconButton(
            onClick = onCalendarClick,
            modifier = Modifier
                .width(50.dp)
                .height(50.dp)
                .padding(end = 8.dp),
        ) {
            Icon(
                painterResource(id = R.drawable.ic_calendar),
                contentDescription = "Choose dates",
            )
        }

        // Navigation with arrows and single choice title
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                val options = DateRangeOption.entries
                val newIndex = (options.indexOf(selectedOption) - 1 + options.size) % options.size
                onOptionChange(options[newIndex])
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
            }

            Text(
                text = selectedOption.title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = {
                val options = DateRangeOption.entries
                val newIndex = (options.indexOf(selectedOption) + 1) % options.size
                onOptionChange(options[newIndex])
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
            }
        }
    }
}

fun calculateRangeFromOption(option: DateRangeOption, minDate: Double): ClosedFloatingPointRange<Float> {
    val today = (System.currentTimeMillis() / (1000 * 86400.0)) + 25569
    val start = when (option) {
        DateRangeOption.LAST_DAY -> today - 1
        DateRangeOption.LAST_WEEK -> today - 7
        DateRangeOption.LAST_MONTH -> today - 30
        DateRangeOption.LAST_6_MONTHS -> today - 182
        DateRangeOption.LAST_YEAR -> today - 365
        DateRangeOption.LAST_5_YEARS -> today - (365 * 5)
        DateRangeOption.ALL_TIME -> minDate
    }
    return start.toFloat()..today.toFloat()
}
