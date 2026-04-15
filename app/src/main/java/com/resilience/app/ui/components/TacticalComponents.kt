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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_progress"
    )

    val rustColor = Color(0xFFB46B3C) // hsl(18 50% 47%)
    val grainStep = if (isProminent) 5.dp else 6.dp
    val grainSize = if (isProminent) 1.4.dp else 1.1.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val lineHeightPx = 2.dp.toPx()
                val grainStepPx = grainStep.toPx()
                val grainSizePx = grainSize.toPx()
                val canvasWidth = size.width
                val canvasHeight = size.height
                val grainOffsets = buildList {
                    var column = 0
                    var x = 0f
                    while (x < canvasWidth) {
                        var row = 0
                        var y = 0f
                        while (y < canvasHeight) {
                            if (grainValue(column, row) > 0.72f) {
                                add(Offset(x, y))
                            }
                            row += 1
                            y += grainStepPx
                        }
                        column += 1
                        x += grainStepPx
                    }
                }
                val scanBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        rustColor.copy(alpha = 0.3f),
                        rustColor.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )

                onDrawWithContent {
                    drawContent()

                    grainOffsets.forEach { offset ->
                        drawRect(
                            color = Color.White.copy(alpha = 0.04f),
                            topLeft = offset,
                            size = Size(grainSizePx, grainSizePx)
                        )
                    }

                    val lineY = -lineHeightPx + ((size.height + lineHeightPx) * scanProgress)
                    drawRect(
                        brush = scanBrush,
                        topLeft = Offset(0f, lineY),
                        size = Size(size.width, lineHeightPx)
                    )
                }
            }
    )
}

private fun grainValue(column: Int, row: Int): Float {
    val hash = (column * 73_856_093) xor (row * 19_349_663)
    return ((hash and Int.MAX_VALUE) % 1_000) / 1_000f
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
