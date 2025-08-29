package com.expensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.data.model.Budget
import com.expensetracker.ui.component.PlayerStyleMonthNavigator
import com.expensetracker.ui.component.PlayerStyleProgressBar
import com.expensetracker.ui.viewmodel.BudgetViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onScrollDirectionChanged: (Boolean) -> Unit = {},
    budgetViewModel: BudgetViewModel = hiltViewModel()
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    
    // Month navigation state
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    
    // Get budgets for selected month
    val budgets by budgetViewModel.getBudgetsForMonth(currentMonth, currentYear).collectAsStateWithLifecycle(initialValue = emptyList())
    
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp) // Space for floating nav
    ) {
        // Header
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Budget Management",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                FilledTonalButton(
                    onClick = { showAddBudgetDialog = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Budget")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        // Budget Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "This Month's Budget Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val totalBudget = budgets.sumOf { it.limitAmount }
                val totalSpent = budgets.sumOf { it.currentSpent }
                val overBudgetCount = budgets.count { it.isOverBudget }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Budget",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormatter.format(totalBudget),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormatter.format(totalSpent),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalSpent > totalBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                if (overBudgetCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$overBudgetCount ${if (overBudgetCount == 1) "category is" else "categories are"} over budget",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Budget List
        if (budgets.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No budgets set",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Create budgets to track your spending limits",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgets) { budget ->
                    BudgetItem(
                        budget = budget,
                        currencyFormatter = currencyFormatter,
                        onDeleteBudget = { budgetViewModel.deleteBudget(it) },
                        onTestSpending = { budgetViewModel.testUpdateBudgetSpending(it.category, it.month, it.year, it.currentSpent + 25.0) }
                    )
                }
            }
        }
    }

    // Add Budget Dialog
    if (showAddBudgetDialog) {
        AddBudgetDialog(
            onDismiss = { showAddBudgetDialog = false },
            onBudgetAdded = { category, amount ->
                budgetViewModel.addBudgetForMonth(category, amount, currentMonth, currentYear)
                showAddBudgetDialog = false
            }
        )
    }
}

@Composable
fun BudgetItem(
    budget: Budget,
    currencyFormatter: NumberFormat,
    onDeleteBudget: (Budget) -> Unit,
    onTestSpending: (Budget) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = budget.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (budget.isOverBudget) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Over budget",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    IconButton(onClick = { onDeleteBudget(budget) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete budget",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    // Test button for debugging progress bar
                    OutlinedButton(
                        onClick = { onTestSpending(budget) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Test +$25")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Player Style Progress Bar
            PlayerStyleProgressBar(
                progress = budget.percentageUsed,
                spent = budget.currentSpent,
                limit = budget.limitAmount,
                category = budget.category
            )
            
            if (budget.isOverBudget) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Over by ${currencyFormatter.format(budget.currentSpent - budget.limitAmount)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onBudgetAdded: (String, Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("") }
    var budgetAmount by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    
    val expenseCategories = listOf(
        "Food & Dining", "Transportation", "Shopping", "Entertainment", "Bills & Utilities",
        "Healthcare", "Groceries", "Education", "Travel", "Subscriptions", "Insurance",
        "Fuel", "Home & Garden", "Sports & Fitness", "Beauty & Personal Care", 
        "Electronics", "Clothing", "Pet Care", "Gifts & Donations", "Business", "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Budget",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                // Category Selection
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        expenseCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Budget Amount
                OutlinedTextField(
                    value = budgetAmount,
                    onValueChange = { budgetAmount = it },
                    label = { Text("Budget Limit") },
                    leadingIcon = { Text("$", style = MaterialTheme.typography.titleMedium) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = budgetAmount.toDoubleOrNull()
                    if (selectedCategory.isNotBlank() && amount != null && amount > 0) {
                        onBudgetAdded(selectedCategory, amount)
                    }
                },
                enabled = selectedCategory.isNotBlank() && budgetAmount.toDoubleOrNull() != null && (budgetAmount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
