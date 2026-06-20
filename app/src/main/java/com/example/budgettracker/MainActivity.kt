package com.example.budgettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import com.example.budgettracker.ui.theme.BudgetTrackerTheme

enum class Screen {
    Dashboard,
    Settings,
    ExpensesList
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetTrackerTheme {
                var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        Screen.Dashboard -> {
                            DashboardScreen(
                                onNavigateToSettings = { currentScreen = Screen.Settings },
                                onNavigateToExpenses = { currentScreen = Screen.ExpensesList },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                onNavigateBack = { currentScreen = Screen.Dashboard },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        Screen.ExpensesList -> {
                            ExpensesListScreen(
                                onNavigateBack = { currentScreen = Screen.Dashboard },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}