package com.example.appfinancetest

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity
data class TransactionDB(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "date") val date: Double?,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "item") val item: String?,
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "amount") val amount: Double?,
    @ColumnInfo(name = "variation") val variation: Double?,
    @ColumnInfo(name = "balance") val balance: Double?,
    @ColumnInfo(name = "idInvest") val idInvest: String?
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM TransactionDB")
    fun getAll(): LiveData<List<TransactionDB>>

    @Insert
    suspend fun insertAll(vararg transactions: TransactionDB)

    @Query("DELETE FROM TransactionDB")
    suspend fun deleteAll()

    @Query("SELECT * FROM TransactionDB ORDER BY date DESC")
    suspend fun getTransactionsSortedByDate(): List<TransactionDB>

    @Query("SELECT * FROM TransactionDB ORDER BY date ASC")
    suspend fun getTransactionsSortedByDateASC(): List<TransactionDB>

    @Query("SELECT * FROM TransactionDB ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsPaged(limit: Int, offset: Int): List<TransactionDB>

    @Query("UPDATE TransactionDB SET balance = :newBalance WHERE id = :transactionId")
    suspend fun updateBalance(transactionId: Int, newBalance: Double)

    @Query("SELECT * FROM TransactionDB WHERE idInvest IS NOT NULL AND idInvest != ''")
    suspend fun getInvestmentTransactions(): List<TransactionDB>

    @Query("SELECT * FROM TransactionDB WHERE idInvest = :idInvest")
    suspend fun getInvestmentTransactionsByID(idInvest: String): List<TransactionDB>

}

@Database(entities = [TransactionDB::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}