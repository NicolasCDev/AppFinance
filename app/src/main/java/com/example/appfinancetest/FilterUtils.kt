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
        (dateBeginQuery.isBlank() || DateFormattedText(it.dateBegin).contains(dateBeginQuery, ignoreCase = true)) &&
                (dateEndQuery.isBlank() || DateFormattedText(it.dateEnd).contains(dateEndQuery, ignoreCase = true)) &&
                (itemQuery.isBlank() || it.item?.contains(itemQuery, ignoreCase = true) == true) &&
                (labelQuery.isBlank() || it.label?.contains(labelQuery, ignoreCase = true) == true) &&
                (investedQuery.isBlank() || it.invested.toString().contains(investedQuery)) &&
                (earnedQuery.isBlank() || it.earned.toString().contains(earnedQuery))
    }
}
