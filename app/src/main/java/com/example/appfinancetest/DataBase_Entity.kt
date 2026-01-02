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
import androidx.room.TypeConverters
import androidx.room.TypeConverter

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

    @Query("SELECT DISTINCT category FROM TransactionDB WHERE category IS NOT NULL AND category != ''")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT DISTINCT item FROM TransactionDB WHERE item IS NOT NULL AND item != ''")
    suspend fun getAllItems(): List<String>

    @Query("SELECT DISTINCT label FROM TransactionDB WHERE label IS NOT NULL AND label != ''")
    suspend fun getAllLabels(): List<String>

    @Query("""SELECT (
        -- 1. Somme des Revenus
        COALESCE((SELECT SUM(amount) FROM TransactionDB WHERE date <= :date AND category = 'Revenus'), 0)
        -
        -- 2. Soustraction des Charges
        COALESCE((SELECT SUM(amount) FROM TransactionDB WHERE date <= :date AND category = 'Charge'), 0)
        +
        -- 3. Gestion des Investissements (Gains - Investis)
        COALESCE((
            SELECT SUM(profit) FROM (
                SELECT 
                    t.idInvest,
                    -- Calcul de la somme (Gains - Investissements) pour cet idInvest jusqu'à :date
                    SUM(CASE WHEN t.category = 'Gain investissement' THEN t.amount ELSE 0 END) -
                    SUM(CASE WHEN t.category = 'Investissement' THEN t.amount ELSE 0 END) AS profit,
                    -- Vérification si l'investissement est clôturé (dateEnd non nulle)
                    MAX(CASE WHEN i.dateEnd IS NOT NULL THEN 1 ELSE 0 END) as isClosed
                FROM TransactionDB t
                LEFT JOIN InvestmentDB i ON t.idInvest = i.idInvest
                WHERE t.date <= :date AND t.idInvest IS NOT NULL AND t.idInvest != ''
                GROUP BY t.idInvest
            ) 
            -- Condition : Clôturé OU (Non-clôturé ET profit > 0)
            WHERE isClosed = 1 OR profit > 0
        ), 0)
        )
    """)
    fun getNetWorthAtDate(date: Double): LiveData<Double?>

    @Query("""SELECT (
        -- 1. Somme des Revenus
        COALESCE((SELECT SUM(amount) FROM TransactionDB WHERE date <= :date AND category = 'Revenus'), 0)
        -
        -- 2. Soustraction des Charges
        COALESCE((SELECT SUM(amount) FROM TransactionDB WHERE date <= :date AND category = 'Charge'), 0)
        +
        -- 3. Gestion des Investissements (Gains - Investis)
        COALESCE((
            SELECT SUM(profit) FROM (
                SELECT 
                    t.idInvest,
                    SUM(CASE WHEN t.category = 'Gain investissement' THEN t.amount ELSE 0 END) -
                    SUM(CASE WHEN t.category = 'Investissement' THEN t.amount ELSE 0 END) AS profit,
                    MAX(CASE WHEN i.dateEnd IS NOT NULL THEN 1 ELSE 0 END) as isClosed
                FROM TransactionDB t
                LEFT JOIN InvestmentDB i ON t.idInvest = i.idInvest
                WHERE t.date <= :date AND t.idInvest IS NOT NULL AND t.idInvest != ''
                GROUP BY t.idInvest
            ) 
            WHERE isClosed = 1 OR profit > 0
        ), 0)
        )
    """)
    suspend fun getNetWorthAtDateStatic(date: Double): Double
}

@Database(entities = [TransactionDB::class, InvestmentDB::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun investmentDao(): InvestmentDao
}

class Converters {
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return if (value.isBlank()) emptyList() else value.split(",").map { it.toInt() }
    }
}
