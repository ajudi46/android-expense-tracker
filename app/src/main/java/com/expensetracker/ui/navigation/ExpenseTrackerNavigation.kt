package com.expensetracker.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.expensetracker.ui.screen.AccountsScreen
import com.expensetracker.ui.screen.AddTransactionScreen
import com.expensetracker.ui.screen.BudgetScreen
import com.expensetracker.ui.screen.DashboardScreen
import com.expensetracker.ui.screen.ProfileScreen
import com.expensetracker.ui.screen.RecentTransactionsScreen
import com.expensetracker.ui.screen.SignInScreen
import com.expensetracker.ui.viewmodel.AuthViewModel

@Composable
fun ExpenseTrackerNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var isBottomNavVisible by remember { mutableStateOf(true) }
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    
    // Show login screen for new users, Dashboard for existing users
    val startDestination = if (authState.shouldShowLogin) {
        Screen.SignIn.route
    } else {
        Screen.Dashboard.route
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            // Sign In Screen
            composable(Screen.SignIn.route) {
                SignInScreen(
                    onSignInSuccess = {
                        // Navigate to Dashboard after sign-in or skip
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToAccounts = {
                        navController.navigate(Screen.Accounts.route)
                    },
                    onNavigateToAddTransaction = {
                        navController.navigate(Screen.AddTransaction.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onScrollDirectionChanged = { visible ->
                        isBottomNavVisible = visible
                    },
                    authViewModel = authViewModel
                )
            }

            composable(Screen.Accounts.route) {
                AccountsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onScrollDirectionChanged = { visible ->
                        isBottomNavVisible = visible
                    }
                )
            }

            composable(Screen.AddTransaction.route) {
                AddTransactionScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onScrollDirectionChanged = { visible ->
                        isBottomNavVisible = visible
                    }
                )
            }

            composable(Screen.RecentTransactions.route) {
                RecentTransactionsScreen(
                    onScrollDirectionChanged = { visible ->
                        isBottomNavVisible = visible
                    }
                )
            }

            composable(Screen.Budget.route) {
                BudgetScreen(
                    onScrollDirectionChanged = { visible ->
                        isBottomNavVisible = visible
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onLogoutSuccess = {
                        // Navigate to login screen after logout
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }
        }
        
        // Floating bottom navigation (always show - works for both signed in and local users)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            ExpenseTrackerBottomNavigation(
                navController = navController,
                isVisible = isBottomNavVisible
            )
        }
    }
}

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object Dashboard : Screen("dashboard")
    object Accounts : Screen("accounts")
    object AddTransaction : Screen("add_transaction")
    object RecentTransactions : Screen("recent_transactions")
    object Budget : Screen("budget")
    object Profile : Screen("profile")
}
