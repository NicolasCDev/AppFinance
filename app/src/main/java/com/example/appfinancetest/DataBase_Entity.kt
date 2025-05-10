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
    @ColumnInfo(name = "categorie") val categorie: String?,
    @ColumnInfo(name = "poste") val poste: String?,
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "amount") val amount: Double?,
    @ColumnInfo(name = "variation") val variation: Double?,
    @ColumnInfo(name = "solde") val solde: Double?,
    @ColumnInfo(name = "idInvest") val idInvest: String?
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM TransactionDB")
    fun getAll(): LiveData<List<TransactionDB>>

    @Insert
    suspend fun insertAll(vararg transactions: TransactionDB)

    @Query("DELETE FROM TransactionDB")
    fun deleteAll()

    @Query("SELECT * FROM TransactionDB ORDER BY date DESC")
    fun getTransactionsSortedByDate(): List<TransactionDB>

    @Query("SELECT * FROM TransactionDB ORDER BY date ASC")
    fun getTransactionsSortedByDateASC(): List<TransactionDB>

    @Query("SELECT * FROM TransactionDB ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsPaged(limit: Int, offset: Int): List<TransactionDB>

    @Query("UPDATE TransactionDB SET solde = :newSolde WHERE id = :transactionId")
    fun updateSolde(transactionId: Int, newSolde: Double)
}

@Database(entities = [TransactionDB::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}