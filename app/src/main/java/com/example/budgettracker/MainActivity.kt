package com.example.budgettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgettracker.ui.theme.BudgetTrackerTheme

enum class Screen {
    Dashboard,
    Settings,
    ExpensesList,
    Calendar
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetTrackerTheme {
                var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
                var showAboutDialog by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Budget Tracker",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            },
                            actions = {
                                // Home/Dashboard navigation
                                IconButton(onClick = { currentScreen = Screen.Dashboard }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_home),
                                        contentDescription = "Home",
                                        tint = if (currentScreen == Screen.Dashboard) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Expenses navigation
                                IconButton(onClick = { currentScreen = Screen.ExpensesList }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_expenses),
                                        contentDescription = "ExpensesList",
                                        tint = if (currentScreen == Screen.ExpensesList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Calendar navigation
                                IconButton(onClick = { currentScreen = Screen.Calendar }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_calendar),
                                        contentDescription = "Calendar",
                                        tint = if (currentScreen == Screen.Calendar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Settings navigation (Gear icon)
                                IconButton(onClick = { currentScreen = Screen.Settings }) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Settings",
                                        tint = if (currentScreen == Screen.Settings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // About/Info trigger
                                IconButton(onClick = { showAboutDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "About",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val screenPadding = remember(innerPadding) {
                        androidx.compose.foundation.layout.PaddingValues(
                            start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            top = if (innerPadding.calculateTopPadding() > 12.dp) innerPadding.calculateTopPadding() - 12.dp else 0.dp,
                            end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            bottom = innerPadding.calculateBottomPadding()
                        )
                    }
                    when (currentScreen) {
                        Screen.Dashboard -> {
                            DashboardScreen(
                                onNavigateToSettings = { currentScreen = Screen.Settings },
                                onNavigateToExpenses = { currentScreen = Screen.ExpensesList },
                                modifier = Modifier.padding(screenPadding)
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                onNavigateBack = { currentScreen = Screen.Dashboard },
                                modifier = Modifier.padding(screenPadding)
                            )
                        }
                        Screen.ExpensesList -> {
                            ExpensesListScreen(
                                onNavigateBack = { currentScreen = Screen.Dashboard },
                                modifier = Modifier.padding(screenPadding)
                            )
                        }
                        Screen.Calendar -> {
                            CalendarScreen(
                                modifier = Modifier.padding(screenPadding)
                            )
                        }
                    }
                }

                if (showAboutDialog) {
                    AlertDialog(
                        onDismissRequest = { showAboutDialog = false },
                        title = { Text("About Budget Tracker", fontWeight = FontWeight.Bold) },
                        text = {
                            Text("Budget Tracker App\nVersion 1.0.0\nManage your finances smartly and track your daily expenses easily.")
                        },
                        confirmButton = {
                            TextButton(onClick = { showAboutDialog = false }) {
                                Text("OK", fontWeight = FontWeight.SemiBold)
                            }
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            }
        }
    }
}