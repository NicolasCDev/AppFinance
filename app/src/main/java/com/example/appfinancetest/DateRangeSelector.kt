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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class DateRangeOption(val resId: Int) {
    LAST_DAY(R.string.range_last_day),
    LAST_WEEK(R.string.range_last_week),
    LAST_MONTH(R.string.range_last_month),
    LAST_6_MONTHS(R.string.range_last_6_months),
    LAST_YEAR(R.string.range_last_year),
    LAST_5_YEARS(R.string.range_last_5_years),
    ALL_TIME(R.string.range_all_time)
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
                text = stringResource(id = selectedOption.resId),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
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
