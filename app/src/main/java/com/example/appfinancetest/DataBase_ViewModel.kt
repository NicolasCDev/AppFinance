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
        "database-name"
    )
    .fallbackToDestructiveMigration(false)
    .build()

    private val dbDAO = transactionDB.transactionDao()
    suspend fun insertTransaction(transaction: TransactionDB) {
        viewModelScope.launch(Dispatchers.IO) {
            dbDAO.insertAll(transaction)
        }
    }
    fun deleteAllTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("DataBase_ViewModel", "Suppression de toutes les transactions")
            dbDAO.deleteAll()
        }
    }
    suspend fun updateSolde(transactionId: Int, newSolde: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            dbDAO.updateBalance(transactionId, newSolde)
        }
    }
    suspend fun getPagedTransactions(limit: Int, offset: Int): List<TransactionDB> {
        return dbDAO.getTransactionsPaged(limit, offset)
    }
    // Appel suspendu pour récupérer les transactions
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
