package com.expensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.SwapHoriz
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.R
import com.expensetracker.data.model.Transaction
import com.expensetracker.data.model.TransactionType
import com.expensetracker.ui.theme.expenseColor
import com.expensetracker.ui.theme.expenseContainer
import com.expensetracker.ui.theme.incomeColor
import com.expensetracker.ui.theme.incomeContainer
import com.expensetracker.ui.theme.transferColor
import com.expensetracker.ui.theme.transferContainer
import com.expensetracker.ui.component.MiniPlayerStyleProgressBar
import com.expensetracker.ui.viewmodel.AccountViewModel
import com.expensetracker.ui.viewmodel.AuthViewModel
import com.expensetracker.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAccounts: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onScrollDirectionChanged: (Boolean) -> Unit = {},
    accountViewModel: AccountViewModel = hiltViewModel(),
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val totalBalance by accountViewModel.totalBalance.collectAsStateWithLifecycle(initialValue = 0.0)
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentTransactions by transactionViewModel.recentTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    
    // State for delete confirmation dialog
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Expense Tracker",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        if (!authState.userPhotoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = authState.userPhotoUrl,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                                error = painterResource(android.R.drawable.ic_menu_gallery)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp) // Space for floating nav
        ) {
        // Header
        item {
            Column {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // Total Balance Card - M3 Expressive prominent display
        item {
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(24.dp), // Larger radius for M3 Expressive
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp), // More generous padding
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.total_balance),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = currencyFormatter.format(totalBalance ?: 0.0),
                            style = MaterialTheme.typography.displayMedium, // Larger display for prominence
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        // Add subtle secondary info
                        Text(
                            text = "Across ${accounts.size} accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

        // Quick Actions
        item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = stringResource(R.string.accounts),
                        icon = Icons.Default.AccountBalanceWallet,
                        onClick = onNavigateToAccounts,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = stringResource(R.string.add_transaction),
                        icon = Icons.Default.Add,
                        onClick = onNavigateToAddTransaction,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

        // Recent Transactions Header
        item {
                Text(
                    text = stringResource(R.string.recent_transactions),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Recent Transactions List
            if (recentTransactions.isEmpty()) {
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
                            Text(
                                text = "No transactions yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Start by adding your first transaction",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentTransactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        currencyFormatter = currencyFormatter,
                        onEditTransaction = { 
                            // TODO: Implement edit transaction functionality
                        },
                        onDeleteTransaction = { txn ->
                            transactionToDelete = txn
                        }
                    )
                }
            }

        item {
            Spacer(modifier = Modifier.height(16.dp)) // Bottom padding
        }
        }
    }
    
    // Delete confirmation dialog
    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { 
                Text("Are you sure you want to delete this transaction?\n\n${transaction.category}: ${currencyFormatter.format(transaction.amount)}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionViewModel.deleteTransaction(transaction)
                        transactionToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(20.dp), // More rounded for M3 Expressive
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp, // Enhanced pressed state
            hoveredElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp), // Increased padding for better touch targets
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp) // Larger icon for better visibility
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge, // Larger text for better readability
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    currencyFormatter: NumberFormat,
    onEditTransaction: ((Transaction) -> Unit)? = null,
    onDeleteTransaction: ((Transaction) -> Unit)? = null
) {
    val icon = when (transaction.type) {
        TransactionType.EXPENSE -> Icons.Default.TrendingDown
        TransactionType.INCOME -> Icons.Default.TrendingUp
        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
    }

    val iconColor = when (transaction.type) {
        TransactionType.EXPENSE -> expenseColor
        TransactionType.INCOME -> incomeColor
        TransactionType.TRANSFER -> transferColor
    }
    
    val containerColor = when (transaction.type) {
        TransactionType.EXPENSE -> expenseContainer
        TransactionType.INCOME -> incomeContainer
        TransactionType.TRANSFER -> transferContainer
    }

    val amountText = when (transaction.type) {
        TransactionType.EXPENSE -> "-${currencyFormatter.format(transaction.amount)}"
        TransactionType.INCOME -> "+${currencyFormatter.format(transaction.amount)}"
        TransactionType.TRANSFER -> currencyFormatter.format(transaction.amount)
    }

    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp) // More rounded for M3 Expressive
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp), // Increased padding for better touch targets
            verticalAlignment = Alignment.CenterVertically
        ) {
            // M3 Expressive: Icon container with background color
            Card(
                modifier = Modifier.size(48.dp),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleMedium, // Category as main title
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Only show description if user actually added one (not empty and not generic transaction type text)
                if (transaction.description.isNotEmpty() && 
                    !transaction.description.contains("transaction", ignoreCase = true) &&
                    !transaction.description.equals("income", ignoreCase = true) &&
                    !transaction.description.equals("expense", ignoreCase = true) &&
                    !transaction.description.equals("transfer", ignoreCase = true)) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dateFormatter.format(Date(transaction.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleLarge, // More prominent amount
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                
                // Edit and Delete buttons (only show if callbacks are provided)
                if (onEditTransaction != null || onDeleteTransaction != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onEditTransaction != null) {
                            IconButton(
                                onClick = { onEditTransaction(transaction) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Transaction",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        if (onDeleteTransaction != null) {
                            IconButton(
                                onClick = { onDeleteTransaction(transaction) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Transaction",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
