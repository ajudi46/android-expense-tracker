package com.expensetracker.ui.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.expensetracker.R

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : BottomNavItem(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    )
    
    object Accounts : BottomNavItem(
        route = "accounts",
        title = "Accounts",
        selectedIcon = Icons.Filled.AccountBalance,
        unselectedIcon = Icons.Outlined.AccountBalance
    )
    
    object AddTransaction : BottomNavItem(
        route = "add_transaction",
        title = "Add",
        selectedIcon = Icons.Filled.Add,
        unselectedIcon = Icons.Outlined.Add
    )
    
    object RecentTransactions : BottomNavItem(
        route = "recent_transactions",
        title = "Recent",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )
}

@Composable
fun ExpenseTrackerBottomNavigation(
    navController: NavController,
    isVisible: Boolean = true,
    items: List<BottomNavItem> = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Accounts,
        BottomNavItem.AddTransaction,
        BottomNavItem.RecentTransactions
    )
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    
    // Animation for hiding/showing with middle collapse/expand
    val animationSpec = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )
    
    val scaleX by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = animationSpec,
        label = "scaleX"
    )
    
    val scaleY by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.3f,
        animationSpec = animationSpec,
        label = "scaleY"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = animationSpec,
        label = "alpha"
    )
    
    // Floating bottom navigation container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scaleX,
                    scaleY = scaleY,
                    alpha = alpha,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                )
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(36.dp), // More rounded
                    clip = false
                )
                .clip(RoundedCornerShape(36.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp), // Shorter height
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    
                    FloatingNavItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .size(48.dp) // Slightly smaller for shorter height
            .clip(CircleShape)
            .background(
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else 
                    Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = true,
                    radius = 24.dp,
                    color = MaterialTheme.colorScheme.primary
                ),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.title,
            modifier = Modifier.size(24.dp), // Slightly smaller icon
            tint = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
