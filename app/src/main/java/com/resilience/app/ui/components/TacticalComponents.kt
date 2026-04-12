package com.resilience.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resilience.app.ui.theme.TacticalBorder

@Composable
fun TacticalScannerOverlay(
    modifier: Modifier = Modifier,
    isProminent: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    
    // Subtle on all screens; slightly more visible on Home
    val defaultAlpha = if (isProminent) 0.06f else 0.03f
    val glowMulti = if (isProminent) 1.8f else 1.5f
    val lineMulti = if (isProminent) 2f else 1.5f

    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_y"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val lineY = size.height * scanY
                val scanBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = defaultAlpha),
                        primaryColor.copy(alpha = defaultAlpha * glowMulti),
                        primaryColor.copy(alpha = defaultAlpha),
                        Color.Transparent
                    ),
                    startY = lineY - 30.dp.toPx(),
                    endY = lineY + 30.dp.toPx()
                )
                drawRect(brush = scanBrush)
                // Draw the bright scan line
                drawLine(
                    color = primaryColor.copy(alpha = defaultAlpha * lineMulti),
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = 2.dp.toPx()
                )
            }
    )
}

@Composable
fun TacticalCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = TacticalBorder,  // spec --border: #3A3A3A dark gray
                shape = RoundedCornerShape(4.dp)
            ),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // Top glow accent line
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryColor.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )
            content()
        }
    }
}

@Composable
fun TacticalIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerSize: Dp = 36.dp,
    iconSize: Dp = 16.dp,
    tint: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = TacticalBorder,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(shape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.6f))
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.45f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun TacticalBottomStatusBar(
    statusText: String,
    infoText: String,
    modifier: Modifier = Modifier,
    statusColor: Color = MaterialTheme.colorScheme.secondary
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            HorizontalDivider(color = TacticalBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }
                Text(
                    text = infoText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
            }
        }
    }
}
