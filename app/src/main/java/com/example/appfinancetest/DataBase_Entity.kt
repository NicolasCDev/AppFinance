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
data class Transaction_DB(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "date") val date: String?,
    @ColumnInfo(name = "categorie") val categorie: String?,
    @ColumnInfo(name = "poste") val poste: String?,
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "montant") val montant: Double?,
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM Transaction_DB")
    fun getAll(): LiveData<List<Transaction_DB>>

    @Insert
    fun insertAll(vararg transactions: Transaction_DB)

    @Query("DELETE FROM Transaction_DB")
    fun deleteAll()
}

@Database(entities = [Transaction_DB::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}