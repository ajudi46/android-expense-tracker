package com.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.expensetracker.ui.screen.AccountsScreen
import com.expensetracker.ui.screen.AddTransactionScreen
import com.expensetracker.ui.screen.DashboardScreen

@Composable
fun ExpenseTrackerNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
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
    }
}

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Accounts : Screen("accounts")
    object AddTransaction : Screen("add_transaction")
}
