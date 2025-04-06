package com.example.appfinancetest

import android.app.Application
import android.util.Log
import android.os.Bundle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent// Import correcte pour l'extension
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.appfinancetest.ui.theme.AppFinanceTestTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.room.*
import android.content.Context
import androidx.activity.viewModels
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppFinanceTestTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Tableau de bord", "Investissement", "Patrimoine", "DataBase")
    val icons = listOf(R.drawable.ic_dashboard, R.drawable.ic_investment, R.drawable.ic_patrimoine, R.drawable.ic_database)
    Scaffold(
        modifier = Modifier.fillMaxSize(),

        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = icons[index]),
                                contentDescription = item
                            )
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        modifier = if(index == 0){
                             Modifier.weight(1f)
                        }else{
                            Modifier
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedItem) {
            0 -> DashboardScreen(modifier = Modifier.padding(innerPadding))
            1 -> InvestissementScreen(modifier = Modifier.padding(innerPadding))
            2 -> PatrimoineScreen(modifier = Modifier.padding(innerPadding))
            3 -> DataBaseScreen(modifier = Modifier.padding(innerPadding))
            else -> {
                ErrorScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Greeting(name = "Tableau de bord", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun InvestissementScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Greeting(name = "Investissement")
        }
    }
}

@Composable
fun PatrimoineScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Greeting(name = "Patrimoine")
        }
    }
}

data class Transaction(
    val date: String,
    val categorie: String,
    val poste: String,
    val label: String,
    val montant: Double
)

@Composable
fun DataBaseScreen(modifier: Modifier = Modifier) {
    val transactions = remember { mutableStateListOf(
        Transaction("26/10/23", "Alimentation", "Supermarché", "Courses", 50.50),
        Transaction("25/10/23", "Loisirs", "Restaurant", "Diner", 35.20),
        Transaction("25/10/23", "Loisirs", "Restaurant", "Diner", 35.20),
        Transaction("24/10/23", "Transport", "Bus", "Ticket", 2.00),
        Transaction("23/10/23", "Salaire", "Entreprise", "Salaire", 2500.00),
        Transaction("22/10/23", "Logement", "Loyer", "Loyer mensuel", 800.00),
        Transaction("21/10/23", "Alimentation", "Boulangerie", "Pain", 3.50),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),
        Transaction("20/10/23", "Loisirs", "Cinéma", "Film", 12.00),

    )}
    Box(modifier = modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Database",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text("Date", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Catégorie", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Poste", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Libellé", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Montant", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }

                LazyColumn {
                    items(transactions) { transaction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(transaction.date, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp)
                            Text(transaction.categorie, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp)
                            Text(transaction.poste, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp)
                            Text(transaction.label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp)
                            Text(
                                text = String.format("%.2f €", transaction.montant),
                                modifier = Modifier.weight(1f), fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )

                        }
                        Spacer(modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = {
                // Ajoute une nouvelle transaction à la liste
                val newTransaction = Transaction("01/11/23", "Nouveau", "Lieu", "Description", 100.00)
                transactions.add(newTransaction)
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(painterResource(id = R.drawable.ic_add_transac), contentDescription = "Ajouter une transaction")

        }
    }
}

@Composable
fun ErrorScreen(modifier: Modifier = Modifier){
    Greeting(name = "Error")
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Entity(tableName = "transactions")
data class TransactionDB(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val amount: Double,
    val description: String
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAll(): List<TransactionDB>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionDB)

    @Delete
    suspend fun delete(transaction: TransactionDB)

    @Update
    suspend fun update(transaction: TransactionDB)
}

@Database(entities = [TransactionDB::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "transactions_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TransactionRepository(private val dao: TransactionDao) {

    suspend fun addTransaction(t: TransactionDB) = dao.insert(t)
    suspend fun getAllTransactions() = dao.getAll()
    suspend fun deleteTransaction(t: TransactionDB) = dao.delete(t)
    suspend fun updateTransaction(t: TransactionDB) = dao.update(t)
}

// TransactionViewModel qui hérite de AndroidViewModel
class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()
    private val repository = TransactionRepository(transactionDao)

    // Utiliser SnapshotStateList pour une liste mutable observée
    val transactions: SnapshotStateList<TransactionDB> = mutableStateListOf()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            // Charger les transactions dans la liste observable
            transactions.clear()
            transactions.addAll(repository.getAllTransactions())
        }
    }

    // Cette fonction ajoutera des transactions de test
    fun addTestTransactions() {
        viewModelScope.launch {
            val testTransactions = listOf(
                TransactionDB(
                    date = "26/10/23",
                    amount = 50.50,
                    description = "Alimentation: Supermarché - Courses"
                ),
                TransactionDB(
                    date = "25/10/23",
                    amount = 35.20,
                    description = "Loisirs: Restaurant - Diner"
                ),
                TransactionDB(
                    date = "25/10/23",
                    amount = 35.20,
                    description = "Loisirs: Restaurant - Diner"
                ),
                TransactionDB(
                    date = "24/10/23",
                    amount = 2.00,
                    description = "Transport: Bus - Ticket"
                ),
                TransactionDB(
                    date = "23/10/23",
                    amount = 2500.00,
                    description = "Salaire: Entreprise - Salaire"
                ),
                TransactionDB(
                    date = "22/10/23",
                    amount = 800.00,
                    description = "Logement: Loyer - Loyer mensuel"
                ),
                TransactionDB(
                    date = "21/10/23",
                    amount = 3.50,
                    description = "Alimentation: Boulangerie - Pain"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                ),
                TransactionDB(
                    date = "20/10/23",
                    amount = 12.00,
                    description = "Loisirs: Cinéma - Film"
                )
            )

            // Insertion des données dans la base de données
            testTransactions.forEach { transaction ->
                repository.addTransaction(transaction)
            }
        }
    }

    // Ajouter une nouvelle transaction
    fun addTransaction(transaction: TransactionDB) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
            loadTransactions()  // Recharger les transactions après l'ajout
        }
    }

    // Supprimer une transaction
    fun deleteTransaction(transaction: TransactionDB) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            loadTransactions()  // Recharger les transactions après suppression
        }
    }

    // Mettre à jour une transaction
    fun updateTransaction(transaction: TransactionDB) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            loadTransactions()  // Recharger les transactions après mise à jour
        }
    }
}