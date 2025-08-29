package com.expensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    // Complete form state
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedFromAccount by remember { mutableStateOf<Account?>(null) }
    var selectedToAccount by remember { mutableStateOf<Account?>(null) }
    
    // ViewModel integration
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Enhanced categories with emojis
    val expenseCategories = listOf(
        "🍽️ Food & Dining", "🚗 Transportation", "🛍️ Shopping", "🎬 Entertainment", 
        "💡 Bills & Utilities", "🏥 Healthcare", "🛒 Groceries", "📚 Education", 
        "✈️ Travel", "📱 Subscriptions", "🛡️ Insurance", "⛽ Fuel", 
        "🏡 Home & Garden", "🏃 Sports & Fitness", "💄 Beauty & Personal Care", 
        "📱 Electronics", "👕 Clothing", "🐕 Pet Care", "🎁 Gifts & Donations", 
        "💼 Business", "Other"
    )
    val incomeCategories = listOf(
        "💰 Salary", "💼 Freelance", "📈 Investment Returns", "🏢 Business Income", 
        "🏠 Rental Income", "🎉 Bonus", "🎁 Gift Received", "↩️ Refund", 
        "💸 Interest Earned", "📊 Dividend", "💵 Side Hustle", "Other Income"
    )
    
    val categories = when (selectedType) {
        TransactionType.EXPENSE -> expenseCategories
        TransactionType.INCOME -> incomeCategories
        TransactionType.TRANSFER -> emptyList()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Add Transaction",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Check if accounts exist
        if (accounts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Accounts Found",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Please add at least one account before creating transactions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Account")
                    }
                }
            }
            return@Column
        }
        
        // STEP 1: Amount Input
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            leadingIcon = { Text("$", style = MaterialTheme.typography.titleMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        
        // STEP 2: Description Input (Optional)
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        // STEP 3: Transaction Type Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedType = TransactionType.EXPENSE },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.EXPENSE) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == TransactionType.EXPENSE) 
                                MaterialTheme.colorScheme.onPrimary 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Expense")
                    }
                    
                    Button(
                        onClick = { selectedType = TransactionType.INCOME },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.INCOME) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == TransactionType.INCOME) 
                                MaterialTheme.colorScheme.onPrimary 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Income")
                    }
                    
                    Button(
                        onClick = { selectedType = TransactionType.TRANSFER },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.TRANSFER) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == TransactionType.TRANSFER) 
                                MaterialTheme.colorScheme.onPrimary 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Transfer")
                    }
                }
            }
        }
        
        // Category Selection (for non-transfers)
        if (selectedType != TransactionType.TRANSFER && categories.isNotEmpty()) {
            var expandedCategory by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                expandedCategory = false
                            }
                        )
                    }
                }
            }
        }

        // From Account Selection
        var expandedFromAccount by remember { mutableStateOf(false) }
        
        ExposedDropdownMenuBox(
            expanded = expandedFromAccount,
            onExpandedChange = { expandedFromAccount = !expandedFromAccount }
        ) {
            OutlinedTextField(
                value = selectedFromAccount?.name ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text(if (selectedType == TransactionType.TRANSFER) "From Account" else "Account") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFromAccount)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expandedFromAccount,
                onDismissRequest = { expandedFromAccount = false }
            ) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.name) },
                        onClick = {
                            selectedFromAccount = account
                            expandedFromAccount = false
                        }
                    )
                }
            }
        }

        // To Account Selection (for transfers)
        if (selectedType == TransactionType.TRANSFER) {
            var expandedToAccount by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expandedToAccount,
                onExpandedChange = { expandedToAccount = !expandedToAccount }
            ) {
                OutlinedTextField(
                    value = selectedToAccount?.name ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("To Account") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedToAccount)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expandedToAccount,
                    onDismissRequest = { expandedToAccount = false }
                ) {
                    accounts.forEach { account ->
                        if (account.id != selectedFromAccount?.id) {
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedToAccount = account
                                    expandedToAccount = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // STEP 7: Save Transaction with full validation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0 && selectedFromAccount != null) {
                        // Strip emoji from category for database storage
                        val cleanCategory = if (selectedType == TransactionType.TRANSFER) {
                            "Transfer"
                        } else {
                            if (selectedCategory.contains(" ")) {
                                selectedCategory.substringAfter(" ")
                            } else {
                                selectedCategory
                            }
                        }
                        
                        transactionViewModel.addTransaction(
                            type = selectedType,
                            amount = amountValue,
                            description = description.ifBlank { "${selectedType.name} transaction" },
                            category = cleanCategory,
                            fromAccountId = selectedFromAccount!!.id,
                            toAccountId = selectedToAccount?.id,
                            date = System.currentTimeMillis()
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = amount.toDoubleOrNull() != null && 
                          amount.toDoubleOrNull() ?: 0.0 > 0 && 
                          selectedFromAccount != null &&
                          (selectedType == TransactionType.TRANSFER || selectedCategory.isNotBlank()) &&
                          (selectedType != TransactionType.TRANSFER || selectedToAccount != null)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }
        }
    }
}

/*
// Full implementation - temporarily disabled for debugging
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreenFull(
    onNavigateBack: () -> Unit,
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel()
) {
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

    // Enhanced categories with icons
    val expenseCategories = listOf(
        "🍽️ Food & Dining", "🚗 Transportation", "🛍️ Shopping", "🎬 Entertainment", 
        "💡 Bills & Utilities", "🏥 Healthcare", "🛒 Groceries", "📚 Education", 
        "✈️ Travel", "📱 Subscriptions", "🛡️ Insurance", "⛽ Fuel", 
        "🏡 Home & Garden", "🏃 Sports & Fitness", "💄 Beauty & Personal Care", 
        "📱 Electronics", "👕 Clothing", "🐕 Pet Care", "🎁 Gifts & Donations", 
        "💼 Business", "🏪 Office Supplies", "📋 Maintenance", "🚌 Public Transport",
        "🍕 Fast Food", "☕ Coffee & Tea", "🍺 Alcohol & Bars", "📚 Books & Media",
        "🎮 Gaming", "🎵 Music & Apps", "💇 Hair & Salon", "🔧 Repairs",
        "🏦 Bank Fees", "💳 Credit Card Fees", "📞 Phone Bills", "💻 Internet",
        "⚡ Electricity", "💧 Water", "🔥 Gas", "🗑️ Waste Management", "Other"
    )
    val incomeCategories = listOf(
        "💰 Salary", "💼 Freelance", "📈 Investment Returns", "🏢 Business Income", 
        "🏠 Rental Income", "🎉 Bonus", "🎁 Gift Received", "↩️ Refund", 
        "💸 Interest Earned", "📊 Dividend", "💵 Side Hustle", "🎯 Commission",
        "📝 Consulting", "🎨 Creative Work", "💡 Royalties", "🏆 Prize Money",
        "💱 Currency Exchange", "🔄 Cashback", "🎪 Event Income", "Other Income"
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
        
        // Check if accounts exist
        if (accounts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Accounts Found",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Please add at least one account before creating transactions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onNavigateBack() }, // Go back to let user add account
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Account")
                    }
                }
            }
            return@Column
        }

        // Transaction Type Segmented Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Expense Button
                    Button(
                        onClick = { selectedType = TransactionType.EXPENSE },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.EXPENSE) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == TransactionType.EXPENSE) 
                                MaterialTheme.colorScheme.onPrimary 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Expense")
                    }
                    
                    // Income Button
                    Button(
                        onClick = { selectedType = TransactionType.INCOME },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.INCOME) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == TransactionType.INCOME) 
                                MaterialTheme.colorScheme.onPrimary 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Income")
                    }
                    
                    // Transfer Button
                    Button(
                        onClick = { selectedType = TransactionType.TRANSFER },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.TRANSFER) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == TransactionType.TRANSFER) 
                                MaterialTheme.colorScheme.onPrimary 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transfer")
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

        // Description Input (Optional)
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Add a note about this transaction") }
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
                if (amountValue > 0 && selectedFromAccount != null) {
                    val categoryToUse = if (selectedType == TransactionType.TRANSFER) "Transfer" else selectedCategory
                    
                    // Strip emoji from category for database storage
                    val cleanCategory = if (categoryToUse.contains(" ")) {
                        categoryToUse.substringAfter(" ")
                    } else {
                        categoryToUse
                    }
                    
                    transactionViewModel.addTransaction(
                        type = selectedType,
                        amount = amountValue,
                        description = description.ifBlank { "${selectedType.name} transaction" },
                        category = cleanCategory,
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
                      selectedFromAccount != null &&
                      (selectedType == TransactionType.TRANSFER || selectedCategory.isNotBlank()) &&
                      (selectedType != TransactionType.TRANSFER || selectedToAccount != null)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.save))
        }
    }
    
    // Simple Date Selection Dialog (for now - DatePicker not available in current Material3 version)
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("Select Date") },
            text = {
                Column {
                    Text("Current date: ${dateFormatter.format(java.util.Date(selectedDate))}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Date picker will be enhanced in future updates.", 
                         style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    selectedDate = System.currentTimeMillis()
                    showDatePicker = false 
                }) {
                    Text("Use Today")
                }
            }
        )
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
*/