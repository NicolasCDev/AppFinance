package com.example.appfinancetest

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.appfinancetest.ui.theme.AppFinanceTestTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import com.example.appfinancetest.classes.CreditDBViewModel
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.DataStorage
import com.example.appfinancetest.classes.InvestmentDBViewModel
import com.example.appfinancetest.views.BudgetScreen
import com.example.appfinancetest.views.HomeScreen
import com.example.appfinancetest.views.InvestmentScreen
import com.example.appfinancetest.views.TestScreen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable modern edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            val context = LocalContext.current
            val dataStorage = remember { DataStorage(context) }
            val isDarkThemeCustom by dataStorage.isDarkThemeFlow.collectAsState(initial = null)
            
            val darkTheme = isDarkThemeCustom ?: isSystemInDarkTheme()

            AppFinanceTestTheme(darkTheme = darkTheme) {
                MainScreen(dataStorage)
            }
        }
    }
}

@Composable
fun MainScreen(dataStorage: DataStorage) {
    // Force visibility to OFF on every app launch
    LaunchedEffect(Unit) {
        dataStorage.saveVisibilityState(true)
    }

    val databaseViewModel: DataBaseViewModel = viewModel()
    val investmentViewModel: InvestmentDBViewModel = viewModel()
    val creditViewModel: CreditDBViewModel = viewModel()
    var selectedItem by remember { mutableIntStateOf(0) }
    
    // Use translated strings for the navigation bar
    val items = listOf(
        stringResource(id = R.string.dashboard_title),
        stringResource(id = R.string.investments_title),
        stringResource(id = R.string.patrimonial_title),
        stringResource(id = R.string.test_title)
    )
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            when (index) {
                                0 -> Icon(painter = painterResource(id = R.drawable.ic_dashboard), contentDescription = item)
                                1 -> Icon(painter = painterResource(id = R.drawable.ic_investment), contentDescription = item)
                                2 -> Icon(painter = painterResource(id = R.drawable.ic_patrimoine), contentDescription = item)
                                3 -> Icon(imageVector = Icons.Default.Build, contentDescription = item)
                            }
                        },
                        label = { Text(item, style = MaterialTheme.typography.bodyMedium ) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        },
        // IMPORTANT: Set to 0 to avoid double inset padding at the top/bottom
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        // Only apply the bottom padding from the NavigationBar
        val screenModifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedItem) {
                0 -> HomeScreen(
                    modifier = screenModifier,
                    databaseViewModel = databaseViewModel,
                    investmentViewModel = investmentViewModel,
                    creditViewModel = creditViewModel
                )
                1 -> InvestmentScreen(
                    modifier = screenModifier,
                    databaseViewModel = databaseViewModel,
                    investmentViewModel = investmentViewModel,
                    creditViewModel = creditViewModel
                )
                2 -> BudgetScreen(
                    modifier = screenModifier,
                    databaseViewModel = databaseViewModel,
                    investmentViewModel = investmentViewModel,
                    creditViewModel = creditViewModel
                )
                3 -> TestScreen(
                    modifier = screenModifier,
                    databaseViewModel = databaseViewModel,
                    investmentViewModel = investmentViewModel,
                    creditViewModel = creditViewModel
                )
                else -> {
                    Text("Error")
                }
            }
        }
    }
}
