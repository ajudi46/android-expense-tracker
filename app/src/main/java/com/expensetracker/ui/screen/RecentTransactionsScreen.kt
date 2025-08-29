package com.expensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.data.model.Transaction
import com.expensetracker.data.model.TransactionType
import com.expensetracker.ui.theme.expenseColor
import com.expensetracker.ui.theme.expenseContainer
import com.expensetracker.ui.theme.incomeColor
import com.expensetracker.ui.theme.incomeContainer
import com.expensetracker.ui.theme.transferColor
import com.expensetracker.ui.theme.transferContainer
import com.expensetracker.ui.component.PlayerStyleMonthNavigator
import com.expensetracker.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentTransactionsScreen(
    onScrollDirectionChanged: (Boolean) -> Unit = {},
    transactionViewModel: TransactionViewModel = hiltViewModel()
) {
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
                onScrollDirectionChanged(!isScrollingDown) // Show nav when scrolling up
            }
            
            previousFirstVisibleItemIndex = currentIndex
            previousFirstVisibleItemScrollOffset = currentOffset
        }
    }
    
    // Month navigation state
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    
    // Get transactions for selected month
    val monthTransactions by transactionViewModel.getTransactionsForMonth(currentMonth, currentYear.toString()).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 100.dp) // Space for status bar + floating nav
    ) {
        // Header
        Text(
            text = "Transactions",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Player Style Month Navigation
        PlayerStyleMonthNavigator(
            currentMonth = currentMonth,
            currentYear = currentYear,
            monthNames = monthNames,
            onPreviousMonth = {
                if (currentMonth == 1) {
                    currentMonth = 12
                    currentYear -= 1
                } else {
                    currentMonth -= 1
                }
            },
            onNextMonth = {
                if (currentMonth == 12) {
                    currentMonth = 1
                    currentYear += 1
                } else {
                    currentMonth += 1
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Transactions List
        if (monthTransactions.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No transactions in ${monthNames[currentMonth - 1]}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add transactions to see them here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(monthTransactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        currencyFormatter = currencyFormatter
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    val (icon, iconColor, backgroundColor) = when (transaction.type) {
        TransactionType.INCOME -> Triple(
            Icons.Default.TrendingUp,
            incomeColor,
            incomeContainer
        )
        TransactionType.EXPENSE -> Triple(
            Icons.Default.TrendingDown,
            expenseColor,
            expenseContainer
        )
        TransactionType.TRANSFER -> Triple(
            Icons.Default.SwapHoriz,
            transferColor,
            transferContainer
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transaction Type Icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = backgroundColor.copy(alpha = 0.16f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = transaction.type.name,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Transaction Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.description.ifEmpty { "Transaction" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = dateFormatter.format(java.util.Date(transaction.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val amountColor = when (transaction.type) {
                    TransactionType.INCOME -> incomeColor
                    TransactionType.EXPENSE -> expenseColor
                    TransactionType.TRANSFER -> transferColor
                }
                
                val amountPrefix = when (transaction.type) {
                    TransactionType.INCOME -> "+"
                    TransactionType.EXPENSE -> "-"
                    TransactionType.TRANSFER -> ""
                }

                Text(
                    text = "$amountPrefix${currencyFormatter.format(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = amountColor
                )
                
                Text(
                    text = "Account ${transaction.fromAccountId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
