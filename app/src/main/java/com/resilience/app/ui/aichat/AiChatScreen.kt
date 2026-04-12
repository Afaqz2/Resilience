package com.resilience.app.ui.aichat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.resilience.app.ui.components.TacticalScannerOverlay
import com.resilience.app.ui.theme.*

@Composable
fun AiChatScreen(
    onBack: () -> Unit = {},
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TacticalBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
            TacticalScannerOverlay(isProminent = false)

            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header ────────────────────────────────────────────────
                AiChatHeader(onBack = onBack)

                HorizontalDivider(color = TacticalPrimaryRust.copy(alpha = 0.2f))

                // ── Message List ──────────────────────────────────────────
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        ChatBubble(message = message)
                    }
                }

                // ── Input Bar ─────────────────────────────────────────────
                ChatInputBar(
                    inputText    = uiState.inputText,
                    isProcessing = uiState.isProcessing,
                    onTextChange = { viewModel.onInputChange(it) },
                    onSend       = { viewModel.onSendMessage() }
                )
            }
        }
    }
}

@Composable
private fun AiChatHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TacticalSurface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TacticalText)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(1.dp, TacticalAccentAmber.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .background(TacticalAccentAmber.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Psychology, contentDescription = null,
                tint = TacticalAccentAmber, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "SURVIVAL AI",
                style = MaterialTheme.typography.titleSmall,
                letterSpacing = 2.sp,
                color = TacticalText
            )
            Text(
                text = "OFFLINE  •  KNOWLEDGE BASE",
                style = MaterialTheme.typography.labelSmall,
                color = TacticalMuted
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        // Online status dot
        Surface(
            modifier = Modifier.size(8.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = TacticalSecondaryOlive
        ) {}
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI icon
            Box(
                modifier = Modifier
                    .padding(end = 8.dp, top = 4.dp)
                    .size(28.dp)
                    .border(1.dp, TacticalAccentAmber.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .background(TacticalAccentAmber.copy(alpha = 0.08f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null,
                    tint = TacticalAccentAmber, modifier = Modifier.size(16.dp))
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .border(
                    1.dp,
                    if (isUser) TacticalPrimaryRust.copy(alpha = 0.5f)
                    else TacticalAccentAmber.copy(alpha = 0.25f),
                    RoundedCornerShape(
                        topStart = if (isUser) 8.dp else 2.dp,
                        topEnd = if (isUser) 2.dp else 8.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 8.dp
                    )
                )
                .background(
                    if (isUser) TacticalPrimaryRust.copy(alpha = 0.15f)
                    else TacticalSurface,
                    RoundedCornerShape(
                        topStart = if (isUser) 8.dp else 2.dp,
                        topEnd = if (isUser) 2.dp else 8.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 8.dp
                    )
                )
                .padding(10.dp)
        ) {
            if (message.isLoading) {
                LoadingDots()
            } else {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUser) TacticalText else TacticalText.copy(alpha = 0.9f)
                )
            }
        }

        if (isUser) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp)
                    .size(28.dp)
                    .border(1.dp, TacticalPrimaryRust.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .background(TacticalPrimaryRust.copy(alpha = 0.08f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null,
                    tint = TacticalPrimaryRust, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dots_alpha"
    )
    Text(
        text = "• • •",
        style = MaterialTheme.typography.bodySmall,
        color = TacticalAccentAmber.copy(alpha = alpha)
    )
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isProcessing: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TacticalSurface)
            .border(
                width = 1.dp,
                color = TacticalPrimaryRust.copy(alpha = 0.2f),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "Ask about survival, first aid, navigation...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TacticalMuted
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor        = TacticalText,
                unfocusedTextColor      = TacticalText,
                focusedBorderColor      = TacticalAccentAmber.copy(alpha = 0.6f),
                unfocusedBorderColor    = TacticalPrimaryRust.copy(alpha = 0.3f),
                cursorColor             = TacticalAccentAmber,
                focusedContainerColor   = TacticalBackground,
                unfocusedContainerColor = TacticalBackground
            ),
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(4.dp),
            enabled = !isProcessing
        )

        // Send button
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(4.dp),
            color = if (inputText.isNotBlank() && !isProcessing)
                TacticalPrimaryRust.copy(alpha = 0.2f) else TacticalSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (inputText.isNotBlank() && !isProcessing) TacticalPrimaryRust
                else TacticalMuted.copy(alpha = 0.3f)
            ),
            onClick = onSend
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TacticalAccentAmber,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) TacticalPrimaryRust
                               else TacticalMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
