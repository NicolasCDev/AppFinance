package com.example.appfinancetest

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataBase_ViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDB = Room.databaseBuilder(
        application.applicationContext,
        AppDatabase::class.java,
        "TransactionDB"
    )
    .fallbackToDestructiveMigration(false)
    .build()

    private val dbDAO = transactionDB.transactionDao()
    suspend fun insertTransaction(transaction: TransactionDB) {
        withContext(Dispatchers.IO) {
            dbDAO.insertAll(transaction)
        }
    }
    suspend fun deleteAllTransactions() {
        withContext(Dispatchers.IO) {
            Log.d("DataBase_ViewModel", "Deleting every transactions")
            dbDAO.deleteAll()
        }
    }
    suspend fun updateBalance(transactionId: Int, newBalance: Double) {
        withContext(Dispatchers.IO) {
            dbDAO.updateBalance(transactionId, newBalance)
        }
    }
    suspend fun getPagedTransactions(limit: Int, offset: Int): List<TransactionDB> {
        return dbDAO.getTransactionsPaged(limit, offset)
    }
    // Suspend call for gathering every transaction
    suspend fun getTransactionsSortedByDateASC(): List<TransactionDB> {
        return withContext(Dispatchers.IO) {
            dbDAO.getTransactionsSortedByDateASC()
        }
    }
    suspend fun getInvestmentTransactions(): List<TransactionDB> {
        return withContext(Dispatchers.IO) {
            dbDAO.getInvestmentTransactions()
        }
    }
    suspend fun getInvestmentTransactionsByID(idInvest: String): List<TransactionDB> {
        return withContext(Dispatchers.IO) {
            dbDAO.getInvestmentTransactionsByID(idInvest)
        }
    }
}
