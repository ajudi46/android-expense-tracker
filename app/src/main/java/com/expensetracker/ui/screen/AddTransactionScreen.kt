package com.expensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    val expenseCategories = listOf("Food", "Transportation", "Shopping", "Entertainment", "Bills", "Healthcare", "Other")
    val incomeCategories = listOf("Salary", "Freelance", "Investment", "Gift", "Bonus", "Other")

    val categories = when (selectedType) {
        TransactionType.EXPENSE -> expenseCategories
        TransactionType.INCOME -> incomeCategories
        TransactionType.TRANSFER -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_transaction)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type Selection - M3 Expressive design
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp), // More rounded for M3 Expressive
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp) // Increased padding
                ) {
                    Text(
                        text = "Transaction Type",
                        style = MaterialTheme.typography.titleLarge, // Larger heading
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TransactionTypeChip(
                            type = TransactionType.EXPENSE,
                            isSelected = selectedType == TransactionType.EXPENSE,
                            onClick = { selectedType = it },
                            modifier = Modifier.weight(1f)
                        )
                        TransactionTypeChip(
                            type = TransactionType.INCOME,
                            isSelected = selectedType == TransactionType.INCOME,
                            onClick = { selectedType = it },
                            modifier = Modifier.weight(1f)
                        )
                        TransactionTypeChip(
                            type = TransactionType.TRANSFER,
                            isSelected = selectedType == TransactionType.TRANSFER,
                            onClick = { selectedType = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.amount)) },
                leadingIcon = {
                    Icon(Icons.Default.AttachMoney, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) },
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Category Selection (not for transfers)
            if (selectedType != TransactionType.TRANSFER) {
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
                        leadingIcon = {
                            Icon(Icons.Default.Category, contentDescription = null)
                        },
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
                    onValueChange = {},
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
                            text = { Text("${account.name} (${String.format("%.2f", account.balance)})") },
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
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.to_account)) },
                        leadingIcon = {
                            Icon(Icons.Default.AccountBalance, contentDescription = null)
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
                                text = { Text("${account.name} (${String.format("%.2f", account.balance)})") },
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
                            toAccountId = selectedToAccount?.id
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

    val icon = when (type) {
        TransactionType.EXPENSE -> Icons.Default.TrendingDown
        TransactionType.INCOME -> Icons.Default.TrendingUp
        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
    }

    val text = when (type) {
        TransactionType.EXPENSE -> stringResource(R.string.expense)
        TransactionType.INCOME -> stringResource(R.string.income)
        TransactionType.TRANSFER -> stringResource(R.string.transfer)
    }

    // M3 Expressive: Enhanced chip design with better visual feedback
    FilterChip(
        selected = isSelected,
        onClick = { onClick(type) },
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp) // Additional padding for better touch targets
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp) // Larger icon
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge, // Better typography
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        },
        modifier = modifier.height(56.dp), // Increased height for better accessibility
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
