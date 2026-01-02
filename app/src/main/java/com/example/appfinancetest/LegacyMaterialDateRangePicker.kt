package com.example.appfinancetest

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.datepicker.MaterialDatePicker
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LegacyMaterialDateRangePicker(
    onDismiss: () -> Unit,
    onDateSelected: (Float, Float) -> Unit // 🔧 Adding formatted display
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    val picker = remember {
        MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select a date range")
            .setTheme(R.style.DateRangePickerTheme)
            .build()
    }

    // Print the picker when the composable is launched
    LaunchedEffect(Unit) {
        picker.show(activity.supportFragmentManager, "DATE_PICKER")
    }

    DisposableEffect(Unit) {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val timeZone = TimeZone.getDefault()
        val zoneOffset = timeZone.getOffset(Date().time) * -1

        picker.addOnPositiveButtonClickListener { selection ->
            if (selection != null) {
                val startMillis = selection.first
                val endMillis = selection.second

                val startExcel = (startMillis / 86400000f) + 25569f
                val endExcel = (endMillis / 86400000f) + 25569f

                onDateSelected(startExcel, endExcel)
            }
            onDismiss()
        }

        picker.addOnDismissListener {
            onDismiss()
        }

        onDispose { }
    }
}
