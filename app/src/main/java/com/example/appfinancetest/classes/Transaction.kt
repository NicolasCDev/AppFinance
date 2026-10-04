package com.example.appfinancetest.classes

class Transaction(
    val date: Double,
    val category: String,
    val item: String,
    val label: String,
    val amount: Double,
    val variation : Double,
    val balance : Double,
    val idInvest : String?
)