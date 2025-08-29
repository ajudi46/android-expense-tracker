package com.expensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.R
import com.expensetracker.data.model.Account
import com.expensetracker.data.model.TransactionType
import com.expensetracker.ui.theme.expenseColor
import com.expensetracker.ui.theme.incomeColor
import com.expensetracker.ui.theme.transferColor
import com.expensetracker.ui.viewmodel.AccountViewModel
import com.expensetracker.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle(initialValue = emptyList())

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedFromAccount by remember { mutableStateOf<Account?>(null) }
    var selectedToAccount by remember { mutableStateOf<Account?>(null) }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showFromAccountDropdown by remember { mutableStateOf(false) }
    var showToAccountDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Transaction date (default to current date/time)
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
    val dateFormatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())

    val expenseCategories = listOf(
        "Food & Dining", "Transportation", "Shopping", "Entertainment", "Bills & Utilities",
        "Healthcare", "Groceries", "Education", "Travel", "Subscriptions", "Insurance",
        "Fuel", "Home & Garden", "Sports & Fitness", "Beauty & Personal Care", 
        "Electronics", "Clothing", "Pet Care", "Gifts & Donations", "Business", "Other"
    )
    val incomeCategories = listOf(
        "Salary", "Freelance", "Investment", "Business", "Gift", "Bonus", 
        "Rental Income", "Side Hustle", "Dividend", "Interest", "Other"
    )

    val categories = when (selectedType) {
        TransactionType.EXPENSE -> expenseCategories
        TransactionType.INCOME -> incomeCategories
        TransactionType.TRANSFER -> emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 100.dp), // Space for status bar + floating nav
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Add Transaction",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Transaction Type Selection - M3 Expressive design
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(20.dp) // More rounded for M3 Expressive
        ) {
            Column(
                modifier = Modifier.padding(24.dp) // Increased padding for M3 Expressive
            ) {
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.titleLarge, // Larger typography for M3 Expressive
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TransactionType.values().forEach { type ->
                        TransactionTypeChip(
                            type = type,
                            isSelected = selectedType == type,
                            onClick = { selectedType = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Amount Input
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            leadingIcon = { Text("$", style = MaterialTheme.typography.titleMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        // Description Input
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        // Date Selection
        OutlinedTextField(
            value = dateFormatter.format(java.util.Date(selectedDate)),
            onValueChange = { },
            readOnly = true,
            label = { Text("Date & Time") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Category Selection (not shown for transfers)
        if (selectedType != TransactionType.TRANSFER) {
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
                    categories.forEach { category ->
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
        }

        // From Account Selection
        ExposedDropdownMenuBox(
            expanded = showFromAccountDropdown,
            onExpandedChange = { showFromAccountDropdown = !showFromAccountDropdown }
        ) {
            OutlinedTextField(
                value = selectedFromAccount?.name ?: "",
                onValueChange = { },
                readOnly = true,
                label = { 
                    Text(
                        if (selectedType == TransactionType.TRANSFER) 
                            stringResource(R.string.from_account) 
                        else stringResource(R.string.account)
                    ) 
                },
                leadingIcon = {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFromAccountDropdown)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = showFromAccountDropdown,
                onDismissRequest = { showFromAccountDropdown = false }
            ) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.name) },
                        onClick = {
                            selectedFromAccount = account
                            showFromAccountDropdown = false
                        }
                    )
                }
            }
        }

        // To Account Selection (only for transfers)
        if (selectedType == TransactionType.TRANSFER) {
            ExposedDropdownMenuBox(
                expanded = showToAccountDropdown,
                onExpandedChange = { showToAccountDropdown = !showToAccountDropdown }
            ) {
                OutlinedTextField(
                    value = selectedToAccount?.name ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.to_account)) },
                    leadingIcon = {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showToAccountDropdown)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = showToAccountDropdown,
                    onDismissRequest = { showToAccountDropdown = false }
                ) {
                    accounts.filter { it.id != selectedFromAccount?.id }.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                selectedToAccount = account
                                showToAccountDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save Button
        Button(
            onClick = {
                val amountValue = amount.toDoubleOrNull() ?: 0.0
                if (amountValue > 0 && description.isNotBlank() && selectedFromAccount != null) {
                    val categoryToUse = if (selectedType == TransactionType.TRANSFER) "Transfer" else selectedCategory
                    
                    transactionViewModel.addTransaction(
                        type = selectedType,
                        amount = amountValue,
                        description = description,
                        category = categoryToUse,
                        fromAccountId = selectedFromAccount!!.id,
                        toAccountId = selectedToAccount?.id,
                        date = selectedDate
                    )
                    onNavigateBack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = amount.toDoubleOrNull() != null && 
                      amount.toDoubleOrNull() ?: 0.0 > 0 && 
                      description.isNotBlank() && 
                      selectedFromAccount != null &&
                      (selectedType == TransactionType.TRANSFER || selectedCategory.isNotBlank()) &&
                      (selectedType != TransactionType.TRANSFER || selectedToAccount != null)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
fun TransactionTypeChip(
    type: TransactionType,
    isSelected: Boolean,
    onClick: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when (type) {
        TransactionType.EXPENSE -> expenseColor
        TransactionType.INCOME -> incomeColor
        TransactionType.TRANSFER -> transferColor
    }

    val label = when (type) {
        TransactionType.EXPENSE -> "Expense"
        TransactionType.INCOME -> "Income"
        TransactionType.TRANSFER -> "Transfer"
    }

    FilterChip(
        selected = isSelected,
        onClick = { onClick(type) },
        label = { 
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge // M3 Expressive larger label
            )
        },
        modifier = modifier.height(56.dp), // M3 Expressive increased height
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = when (type) {
                        TransactionType.EXPENSE -> Icons.Default.AccountBalanceWallet
                        TransactionType.INCOME -> Icons.Default.AccountBalanceWallet
                        TransactionType.TRANSFER -> Icons.Default.AccountBalanceWallet
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp) // M3 Expressive larger icon
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.16f), // M3 Expressive alpha
            selectedLabelColor = color,
            selectedLeadingIconColor = color,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = if (isSelected) 
            FilterChipDefaults.filterChipBorder(
                borderColor = color,
                selectedBorderColor = color,
                borderWidth = 2.dp
            ) else FilterChipDefaults.filterChipBorder()
    )
}