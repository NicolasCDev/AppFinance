package com.example.appfinancetest

import android.util.Log
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
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
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.io.InputStream
import java.io.OutputStream
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
    val viewModel: DataBase_ViewModel = viewModel() // <- the only instance is here
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
            0 -> DashboardScreen(modifier = Modifier.padding(innerPadding), viewModel = viewModel)
            1 -> InvestmentScreen(modifier = Modifier.padding(innerPadding), viewModel = viewModel)
            2 -> PatrimonialScreen(modifier = Modifier.padding(innerPadding), viewModel = viewModel)
            3 -> DataBaseScreen(modifier = Modifier.padding(innerPadding), viewModel = viewModel)
            else -> {
                ErrorScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
    if (showSettings) {
        SettingsScreen(onDismiss = { showSettings = false })
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

fun DateFormattedText(date: Double?): String {
    if (date == null) return "N/A"
    val excelDateMilliSec = (date - 25569) * 86400 * 1000
    val excelDate = Date(excelDateMilliSec.toLong())
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return dateFormat.format(excelDate)
}