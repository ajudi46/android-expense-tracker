package com.expensetracker.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.*

@Composable
fun PlayerStyleProgressBar(
    progress: Float, // 0.0 to 1.0
    spent: Double,
    limit: Double,
    category: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )
    
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    
    // Color based on progress
    val progressColor = when {
        progress < 0.7f -> MaterialTheme.colorScheme.primary
        progress < 1.0f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFFF5722) // Red
    }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Category and amounts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "${currencyFormatter.format(spent)} / ${currencyFormatter.format(limit)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Progress bar with music player style
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val progressWidth = size.width * animatedProgress
                
                // Background track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                )
                
                // Progress track with gradient
                if (progressWidth > 0) {
                    val gradient = Brush.horizontalGradient(
                        colors = listOf(
                            progressColor.copy(alpha = 0.8f),
                            progressColor
                        ),
                        startX = 0f,
                        endX = progressWidth
                    )
                    
                    drawRoundRect(
                        brush = gradient,
                        topLeft = Offset.Zero,
                        size = Size(progressWidth, size.height),
                        cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Progress percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = progressColor
            )
        }
    }
}

@Composable
fun MiniPlayerStyleProgressBar(
    progress: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "miniProgress"
    )
    
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val progressWidth = size.width * animatedProgress
            
            // Background track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(size.height / 2, size.height / 2)
            )
            
            // Progress track
            if (progressWidth > 0) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset.Zero,
                    size = Size(progressWidth, size.height),
                    cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                )
            }
        }
    }
}
