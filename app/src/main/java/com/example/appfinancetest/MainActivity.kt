package com.example.appfinancetest

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.appfinancetest.ui.theme.AppFinanceTestTheme
import androidx.lifecycle.viewmodel.compose.viewModel


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable modern edge-to-edge display
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
    val databaseViewModel: DataBaseViewModel = viewModel()
    val investmentViewModel: InvestmentDBViewModel = viewModel()
    val creditViewModel: CreditDBViewModel = viewModel()
    var selectedItem by remember { mutableIntStateOf(0) }
    
    // Use translated strings for the navigation bar
    val items = listOf(
        stringResource(id = R.string.dashboard_title),
        stringResource(id = R.string.investments_title),
        stringResource(id = R.string.patrimonial_title)
    )
    val icons = listOf(R.drawable.ic_dashboard, R.drawable.ic_investment, R.drawable.ic_patrimoine)
    
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
                0 -> DashboardScreen(
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
                2 -> PatrimonialScreen(
                    modifier = screenModifier, 
                    databaseViewModel = databaseViewModel, 
                    investmentViewModel = investmentViewModel, 
                    creditViewModel = creditViewModel
                )
                else -> {
                    // ErrorScreen() // Assumed to exist or replaced with a simple text
                    Text("Error")
                }
            }
        }
    }
}
