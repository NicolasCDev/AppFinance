package com.example.appfinancetest

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataBase_ViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDB = Room.databaseBuilder(
        application.applicationContext,
        AppDatabase::class.java,
        "database-name"
    ).build()
    private val dbDAO = transactionDB.transactionDao()
    fun insertTransaction(transaction: Transaction_DB) {
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
    val transactionsSortedByDate: LiveData<List<Transaction_DB>> = dbDAO.getTransactionsSortedByDate()
    val transactions: LiveData<List<Transaction_DB>> = dbDAO.getAll()
}
