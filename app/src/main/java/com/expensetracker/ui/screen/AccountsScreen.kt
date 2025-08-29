package com.expensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.R
import com.expensetracker.data.model.Account
import com.expensetracker.ui.viewmodel.AccountViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    onScrollDirectionChanged: (Boolean) -> Unit = {},
    accountViewModel: AccountViewModel = hiltViewModel()
) {
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val uiState by accountViewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    
    // State for delete confirmation dialog
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    val listState = rememberLazyListState()
    
    // Track scroll direction
    LaunchedEffect(listState) {
        var previousFirstVisibleItemIndex = 0
        var previousFirstVisibleItemScrollOffset = 0
        
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (currentIndex, currentOffset) ->
            val isScrollingDown = when {
                currentIndex > previousFirstVisibleItemIndex -> true
                currentIndex < previousFirstVisibleItemIndex -> false
                else -> currentOffset > previousFirstVisibleItemScrollOffset
            }
            
            // Only call if there's actual scrolling happening
            if (currentIndex != previousFirstVisibleItemIndex || 
                kotlin.math.abs(currentOffset - previousFirstVisibleItemScrollOffset) > 10) {
                onScrollDirectionChanged(!isScrollingDown) // Show fab when scrolling up
            }
            
            previousFirstVisibleItemIndex = currentIndex
            previousFirstVisibleItemScrollOffset = currentOffset
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 56.dp, bottom = 100.dp) // Space for status bar + floating nav
    ) {
        // Header with Add Account button
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accounts",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    FilledTonalButton(
                        onClick = {
                            accountViewModel.updateUiState(uiState.copy(showAddAccountDialog = true))
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Account")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

            if (accounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No accounts yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Add your first account to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(accounts) { account ->
                    AccountItem(
                        account = account,
                        currencyFormatter = currencyFormatter,
                        onEditAccount = {
                            accountViewModel.updateUiState(
                                uiState.copy(
                                    selectedAccount = account,
                                    showAddAccountDialog = true
                                )
                            )
                        },
                        onDeleteAccount = {
                            accountToDelete = account
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp)) // Bottom padding
            }
        }

    if (uiState.showAddAccountDialog) {
        AddAccountDialog(
            account = uiState.selectedAccount,
            onDismiss = {
                accountViewModel.updateUiState(
                    uiState.copy(
                        showAddAccountDialog = false,
                        selectedAccount = null
                    )
                )
            },
            onConfirm = { name, iconName, balance ->
                if (uiState.selectedAccount != null) {
                    accountViewModel.updateAccount(
                        uiState.selectedAccount!!.copy(
                            name = name,
                            iconName = iconName,
                            balance = balance
                        )
                    )
                } else {
                    accountViewModel.addAccount(name, iconName, balance)
                }
                accountViewModel.updateUiState(
                    uiState.copy(
                        showAddAccountDialog = false,
                        selectedAccount = null
                    )
                )
            }
        )
    }
    
    // Delete confirmation dialog
    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account") },
            text = { 
                Text("Are you sure you want to delete the account '${account.name}'?\n\nThis action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountViewModel.deleteAccount(account)
                        accountToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AccountItem(
    account: Account,
    currencyFormatter: NumberFormat,
    onEditAccount: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val icon = getAccountIcon(account.iconName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = currencyFormatter.format(account.balance),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (account.balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            IconButton(onClick = onEditAccount) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            
            IconButton(onClick = onDeleteAccount) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    account: Account? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var balance by remember { mutableStateOf(account?.balance?.toString() ?: "") }
    var selectedIcon by remember { mutableStateOf(account?.iconName ?: "wallet") }

    val accountIcons = listOf(
        "wallet" to Icons.Default.AccountBalanceWallet,
        "bank" to Icons.Default.AccountBalance,
        "card" to Icons.Default.CreditCard,
        "savings" to Icons.Default.Savings,
        "cash" to Icons.Default.Money,
        "business" to Icons.Default.Business
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = if (account == null) stringResource(R.string.add_account) else "Edit Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.account_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text(stringResource(R.string.initial_balance)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.select_icon),
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(accountIcons) { index, (iconName, icon) ->
                        IconButton(
                            onClick = { selectedIcon = iconName },
                            modifier = Modifier
                                .size(48.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedIcon == iconName) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = iconName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    tint = if (selectedIcon == iconName) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val balanceValue = balance.toDoubleOrNull() ?: 0.0
                            onConfirm(name, selectedIcon, balanceValue)
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

fun getAccountIcon(iconName: String): ImageVector {
    return when (iconName) {
        "bank" -> Icons.Default.AccountBalance
        "card" -> Icons.Default.CreditCard
        "savings" -> Icons.Default.Savings
        "cash" -> Icons.Default.Money
        "business" -> Icons.Default.Business
        else -> Icons.Default.AccountBalanceWallet
    }
}
