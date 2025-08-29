package com.expensetracker.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.expensetracker.ui.screen.AccountsScreen
import com.expensetracker.ui.screen.AddTransactionScreen
import com.expensetracker.ui.screen.DashboardScreen
import com.expensetracker.ui.screen.RecentTransactionsScreen

@Composable
fun ExpenseTrackerNavigation(
    navController: NavHostController = rememberNavController()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToAccounts = {
                        navController.navigate(Screen.Accounts.route)
                    },
                    onNavigateToAddTransaction = {
                        navController.navigate(Screen.AddTransaction.route)
                    }
                )
            }

            composable(Screen.Accounts.route) {
                AccountsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.AddTransaction.route) {
                AddTransactionScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.RecentTransactions.route) {
                RecentTransactionsScreen()
            }
        }
        
        // Floating bottom navigation
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            ExpenseTrackerBottomNavigation(navController = navController)
        }
    }
}

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Accounts : Screen("accounts")
    object AddTransaction : Screen("add_transaction")
    object RecentTransactions : Screen("recent_transactions")
}
