package com.example.appfinancetest

import com.github.mikephil.charting.data.Entry

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
