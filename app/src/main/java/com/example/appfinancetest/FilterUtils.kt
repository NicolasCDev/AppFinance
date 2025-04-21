package com.example.appfinancetest

fun filterTransactions(
    transactions: List<TransactionDB>,
    dateQuery: String,
    categoryQuery: String,
    posteQuery: String,
    labelQuery: String,
    montantQuery: String
): List<TransactionDB> {
    return transactions.filter {
        (dateQuery.isBlank() || DateFormattedText(it.date).contains(dateQuery, ignoreCase = true)) &&
                (categoryQuery.isBlank() || it.categorie?.contains(categoryQuery, ignoreCase = true) == true) &&
                (posteQuery.isBlank() || it.poste?.contains(posteQuery, ignoreCase = true) == true) &&
                (labelQuery.isBlank() || it.label?.contains(labelQuery, ignoreCase = true) == true) &&
                (montantQuery.isBlank() || it.montant.toString().contains(montantQuery))
    }
}
