package com.example.appfinancetest

fun filterTransactions(
    transactions: List<TransactionDB>,
    dateQuery: String,
    categoryQuery: String,
    itemQuery: String,
    labelQuery: String,
    amountQuery: String
): List<TransactionDB> {
    return transactions.filter {
        (dateQuery.isBlank() || DateFormattedText(it.date).contains(dateQuery, ignoreCase = true)) &&
                (categoryQuery.isBlank() || it.category?.contains(categoryQuery, ignoreCase = true) == true) &&
                (itemQuery.isBlank() || it.item?.contains(itemQuery, ignoreCase = true) == true) &&
                (labelQuery.isBlank() || it.label?.contains(labelQuery, ignoreCase = true) == true) &&
                (amountQuery.isBlank() || it.amount.toString().contains(amountQuery))
    }
}
