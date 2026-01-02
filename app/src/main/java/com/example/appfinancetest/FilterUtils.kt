package com.example.appfinancetest

import java.text.SimpleDateFormat
import java.util.Locale

fun filterTransactions(
    transactions: List<TransactionDB>,
    dateMinQuery: String,
    dateMaxQuery: String,
    categoryQuery: String,
    itemQuery: String,
    labelQuery: String,
    amountMinQuery: String,
    amountMaxQuery: String
): List<TransactionDB> {
    val minAmount = amountMinQuery.toDoubleOrNull()
    val maxAmount = amountMaxQuery.toDoubleOrNull()

    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    
    // Conversion de la date min (Après le...)
    val dateMin = if (dateMinQuery.length == 8) {
        try {
            val date = sdf.parse(dateMinQuery)
            if (date != null) (date.time / (1000.0 * 86400.0)) + 25569 else null
        } catch (e: Exception) { null }
    } else null

    // Conversion de la date max (Avant le...)
    val dateMax = if (dateMaxQuery.length == 8) {
        try {
            val date = sdf.parse(dateMaxQuery)
            if (date != null) (date.time / (1000.0 * 86400.0)) + 25569 else null
        } catch (e: Exception) { null }
    } else null

    return transactions.filter {
        // Chronological Filter (double comparison)
        (dateMin == null || (it.date ?: 0.0) >= dateMin) &&
        (dateMax == null || (it.date ?: 0.0) <= dateMax) &&
        
        // Text filters
        (categoryQuery.isBlank() || it.category?.contains(categoryQuery, ignoreCase = true) == true) &&
        (itemQuery.isBlank() || it.item?.contains(itemQuery, ignoreCase = true) == true) &&
        (labelQuery.isBlank() || it.label?.contains(labelQuery, ignoreCase = true) == true) &&
        
        // Amount filters
        (minAmount == null || (it.amount ?: 0.0) >= minAmount) &&
        (maxAmount == null || (it.amount ?: 0.0) <= maxAmount)
    }
}

fun filterInvestments(
    investments: List<InvestmentDB>,
    dateBeginQuery: String,
    dateEndQuery: String,
    itemQuery: String,
    labelQuery: String,
    investedQuery: String,
    earnedQuery: String
): List<InvestmentDB> {
    return investments.filter {
        (dateBeginQuery.isBlank() || dateFormattedText(it.dateBegin).contains(dateBeginQuery, ignoreCase = true)) &&
        (dateEndQuery.isBlank() || dateFormattedText(it.dateEnd).contains(dateEndQuery, ignoreCase = true)) &&
        (itemQuery.isBlank() || it.item?.contains(itemQuery, ignoreCase = true) == true) &&
        (labelQuery.isBlank() || it.label?.contains(labelQuery, ignoreCase = true) == true) &&
        (investedQuery.isBlank() || it.invested.toString().contains(investedQuery)) &&
        (earnedQuery.isBlank() || it.earned.toString().contains(earnedQuery))
    }
}
