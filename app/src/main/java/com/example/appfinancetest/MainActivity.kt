package com.example.appfinancetest

import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.example.appfinancetest.ui.theme.AppFinanceTestTheme
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Settings
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide system bars through immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.insetsController?.apply {
            hide(WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
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
    Log.d("DataBase_ViewModel", "DataBase_ViewModel created")
    val databaseViewModel: DataBase_ViewModel = viewModel()
    Log.d("InvestmentDB_ViewModel", "InvestmentDB_ViewModel created")
    val investmentViewModel: InvestmentDB_ViewModel = viewModel()
    var selectedItem by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val items = listOf("Dashboard", "Investment", "Patrimonial", "DataBase")
    val icons = listOf(R.drawable.ic_dashboard, R.drawable.ic_investment, R.drawable.ic_patrimoine, R.drawable.ic_database)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Parameters")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(65.dp)
            ) {
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
            0 -> DashboardScreen(modifier = Modifier.padding(innerPadding), databaseViewModel = databaseViewModel, investmentViewModel = investmentViewModel)
            1 -> InvestmentScreen()
            2 -> PatrimonialScreen()
            3 -> DataBaseScreen(modifier = Modifier.padding(innerPadding), databaseViewModel = databaseViewModel, investmentViewModel = investmentViewModel)
            else -> {
                ErrorScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
    if (showSettings) {
        SettingsScreen(onDismiss = { showSettings = false })
    }
}

fun dateFormattedText(date: Double?): String {
    if (date == null) return "N/A"
    val excelDateMilliSec = (date - 25569) * 86400 * 1000
    val excelDate = Date(excelDateMilliSec.toLong())
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return dateFormat.format(excelDate)
}