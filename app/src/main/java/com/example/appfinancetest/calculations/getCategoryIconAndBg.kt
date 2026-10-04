package com.example.appfinancetest.calculations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.ui.theme.*

fun getCategoryIconAndBg(item: TransactionDB): Pair<ImageVector, Color> {
    val cat = (item.category ?: "") + " " + (item.item ?: "") + " " + (item.label ?: "")
    val catLower = cat.lowercase()

    return when {
        catLower.contains("btc") || catLower.contains("crypto") -> Pair(Icons.Default.CurrencyBitcoin, CryptoAmber)
        catLower.contains(" bourse") || catLower.contains("etf") || catLower.contains("pea") || catLower.contains("action") -> Pair(Icons.AutoMirrored.Filled.ShowChart, BourseBlue)
        catLower.contains("salaire") || catLower.contains("virement") || catLower.contains("revenus") -> Pair(Icons.Default.ArrowDownward, TealDark)
        catLower.contains("immobilier") || catLower.contains("loyer") -> Pair(Icons.Default.HomeWork, GreenAccent)
        else -> Pair(Icons.Default.AccountBalanceWallet, IncomeBlue)
    }
}