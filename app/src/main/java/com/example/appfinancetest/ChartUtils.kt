package com.example.appfinancetest

import com.github.mikephil.charting.data.Entry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun makeCumulative(entries: List<Entry>, allDates: List<Float>): List<Entry> {
    val sorted = entries.sortedBy { it.x }
    val result = mutableListOf<Entry>()
    var index = 0
    var total = 0f
    for (date in allDates) {
        while (index < sorted.size && sorted[index].x == date) {
            total += sorted[index].y
            index++
        }
        result.add(Entry(date, total))
    }
    return result
}

fun dateFormattedText(date: Double?): String {
    if (date == null) return "N/A"
    val excelDateMilliSec = (date - 25569) * 86400 * 1000
    val excelDate = Date(excelDateMilliSec.toLong())
    val dateFormat = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
    return dateFormat.format(excelDate)
}