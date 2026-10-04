package com.example.appfinancetest.calculations

import com.example.appfinancetest.classes.TransactionDB
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.text.isNullOrBlank

fun formatMovementSubtitle(
    item: TransactionDB,
    todayStr: String = "Aujourd'hui",
    yesterdayStr: String = "Hier",
    recentStr: String = "Récent"
): String {
    val dateText = if (item.date != null) {
        val excelMillis = ((item.date - 25569) * 86400 * 1000).toLong()
        val date = Date(excelMillis)
        val now = Calendar.getInstance()
        val transCal = Calendar.getInstance().apply { time = date }

        if (now.get(Calendar.YEAR) == transCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == transCal.get(Calendar.DAY_OF_YEAR)
        ) {
            todayStr
        } else if (now.get(Calendar.YEAR) == transCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - transCal.get(Calendar.DAY_OF_YEAR) == 1
        ) {
            yesterdayStr
        } else {
            SimpleDateFormat("d MMM", Locale.getDefault()).format(date)
        }
    } else {
        recentStr
    }

    val elements = mutableListOf<String>()
    elements.add(dateText)
    if (!item.category.isNullOrBlank()) {
        elements.add(item.category)
    }
    if (!item.item.isNullOrBlank() && item.item != item.category) {
        elements.add(item.item)
    }
    if (elements.size == 1) {
        elements.add("PEA")
    }

    return elements.joinToString(" • ")
}