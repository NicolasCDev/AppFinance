package com.example.appfinancetest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.*

@Composable
fun WheelDatePickerDialog(
    initialDate: Double,
    onDismiss: () -> Unit,
    onDateSelected: (Double) -> Unit
) {
    // Use UTC to avoid timezone shifts during conversion to/from Excel dates
    val utcTimeZone = remember { TimeZone.getTimeZone("UTC") }
    val calendar = remember { Calendar.getInstance(utcTimeZone) }
    
    // States for selection
    var selectedDay by remember { mutableIntStateOf(1) }
    var selectedMonth by remember { mutableIntStateOf(1) }
    var selectedYear by remember { mutableIntStateOf(2025) }

    // Initialize states once or when initialDate changes
    LaunchedEffect(initialDate) {
        if (initialDate > 0) {
            val millis = ((initialDate - 25569.0) * 86400000.0).toLong()
            calendar.timeInMillis = millis
        } else {
            calendar.timeInMillis = System.currentTimeMillis()
        }
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
        selectedMonth = calendar.get(Calendar.MONTH) + 1
        selectedYear = calendar.get(Calendar.YEAR)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(id = R.string.choose_date),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val locale = LocalConfiguration.current.locales[0]
                    val isEnglish = locale.language == "en"

                    if (isEnglish) {
                        // Month
                        val months = (1..12).toList()
                        WheelPicker(
                            items = months,
                            initialIndex = months.indexOf(selectedMonth).coerceAtLeast(0),
                            onItemSelected = { selectedMonth = it },
                            modifier = Modifier.weight(1f),
                            label = { it.toString().padStart(2, '0') }
                        )

                        // Day
                        val days = (1..31).toList()
                        WheelPicker(
                            items = days,
                            initialIndex = days.indexOf(selectedDay).coerceAtLeast(0),
                            onItemSelected = { selectedDay = it },
                            modifier = Modifier.weight(1f),
                            label = { it.toString().padStart(2, '0') }
                        )
                    } else {
                        // Day
                        val days = (1..31).toList()
                        WheelPicker(
                            items = days,
                            initialIndex = (selectedDay - 1).coerceIn(0, 30),
                            onItemSelected = { selectedDay = it },
                            modifier = Modifier.weight(1f),
                            label = { it.toString().padStart(2, '0') }
                        )

                        // Month
                        val months = (1..12).toList()
                        WheelPicker(
                            items = months,
                            initialIndex = (selectedMonth - 1).coerceIn(0, 11),
                            onItemSelected = { selectedMonth = it },
                            modifier = Modifier.weight(1f),
                            label = { it.toString().padStart(2, '0') }
                        )
                    }

                    // Year
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val years = ((currentYear - 100)..(currentYear + 100)).toList()
                    WheelPicker(
                        items = years,
                        initialIndex = years.indexOf(selectedYear).coerceAtLeast(0),
                        onItemSelected = { selectedYear = it },
                        modifier = Modifier.weight(1.5f),
                        label = { it.toString() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val resultCalendar = Calendar.getInstance(utcTimeZone)
                        resultCalendar.clear()
                        resultCalendar.set(selectedYear, selectedMonth - 1, 1)
                        val maxDay = resultCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        resultCalendar.set(Calendar.DAY_OF_MONTH, selectedDay.coerceAtMost(maxDay))
                        
                        val excelDate = (resultCalendar.timeInMillis / 86400000.0) + 25569.0
                        onDateSelected(excelDate)
                    }) {
                        Text(stringResource(id = R.string.save_button))
                    }
                }
            }
        }
    }
}

@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialIndex: Int,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String
) {
    val itemHeight = 40.dp
    val containerHeight = 150.dp
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))) {
        items.size
    }

    LaunchedEffect(pagerState.currentPage) {
        if (items.isNotEmpty()) {
            onItemSelected(items[pagerState.currentPage])
        }
    }

    // Update pager state if external selection changes (e.g. initialization)
    LaunchedEffect(initialIndex) {
        if (initialIndex in items.indices && initialIndex != pagerState.currentPage) {
            pagerState.scrollToPage(initialIndex)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        ) {}

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = (containerHeight - itemHeight) / 2),
            pageSpacing = 0.dp,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { page ->
            val isSelected = page == pagerState.currentPage
            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(items[page]),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isSelected) 20.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                )
            }
        }
    }
}
