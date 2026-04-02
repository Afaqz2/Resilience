package com.resilience.app.ui.playbooks

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.resilience.app.data.db.entity.PlaybookEntity
import com.resilience.app.ui.theme.SafeReachDarkGray
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybookDetailScreen(
    playbook: PlaybookEntity,
    onBack: () -> Unit,
    viewModel: PlaybookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val accentColor = remember(playbook.accentColor) {
        try { Color(android.graphics.Color.parseColor(playbook.accentColor)) }
        catch (e: Exception) { Color(0xFFE53935) }
    }

    // ---- Text-to-Speech setup ----
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        viewModel.setSpeaking(false)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        viewModel.setSpeaking(false)
                    }
                })
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
            viewModel.setSpeaking(false)
        }
    }

    fun toggleTts() {
        if (uiState.isSpeaking) {
            tts?.stop()
            viewModel.setSpeaking(false)
        } else {
            // Strip Markdown syntax for clean speech
            val textToSpeak = playbook.contentMarkdown
                .replace(Regex("#{1,6}\\s?"), "")
                .replace(Regex("\\*{1,2}(.*?)\\*{1,2}"), "$1")
                .replace(Regex("\\[.*?\\]\\(.*?\\)"), "")
                .replace("|", "")
                .trim()

            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "PLAYBOOK_TTS")
            viewModel.setSpeaking(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            playbook.category.uppercase(),
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            playbook.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // TTS toggle button
                    IconButton(onClick = { toggleTts() }) {
                        AnimatedContent(
                            targetState = uiState.isSpeaking,
                            label = "tts_icon"
                        ) { speaking ->
                            Icon(
                                imageVector = if (speaking) Icons.Default.StopCircle
                                              else Icons.Default.RecordVoiceOver,
                                contentDescription = if (speaking) "Stop reading" else "Read aloud",
                                tint = if (speaking) accentColor else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ---- Source & verification badge ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = SafeReachDarkGray,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            playbook.sourceOrganisation,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Surface(
                    color = SafeReachDarkGray,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Verified ${playbook.lastVerifiedDate}",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- TTS Banner (shown when speaking) ----
            AnimatedVisibility(visible = uiState.isSpeaking) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Reading aloud… tap the stop button to cancel.",
                            color = accentColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ---- Rendered Markdown content ----
            SimpleMarkdownRenderer(
                markdown = playbook.contentMarkdown,
                accentColor = accentColor
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Minimal Markdown renderer — handles H1–H3, bold, bullet lists, blockquotes,
 * tables (stripped), and horizontal rules. No external library needed.
 */
@Composable
private fun SimpleMarkdownRenderer(markdown: String, accentColor: Color) {
    val lines = markdown.lines()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { raw ->
            val line = raw.trimStart()
            when {
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("> ") -> {
                    Surface(
                        color = accentColor.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                width = 3.dp,
                                color = accentColor.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                            )
                    ) {
                        Text(
                            text = line.removePrefix("> "),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                line.startsWith("- [ ] ") || line.startsWith("- [x] ") -> {
                    val checked = line.startsWith("- [x] ")
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (checked) Icons.Default.CheckBox
                                          else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (checked) accentColor
                                   else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp).padding(top = 1.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = inlineMarkdown(
                                line.removePrefix("- [ ] ").removePrefix("- [x] ")
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 10.dp)
                                .size(5.dp)
                                .background(
                                    accentColor.copy(alpha = 0.7f),
                                    CircleShape
                                )
                        )
                        Text(
                            text = inlineMarkdown(line.drop(2)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                    }
                }
                line.startsWith("---") -> {
                    Divider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                line.startsWith("|") -> {
                    // Simple table row — just strip pipes and show as text
                    val cells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    if (cells.all { it.replace("-","").isBlank() }) return@forEach // separator row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cells.forEach { cell ->
                            Text(
                                text = inlineMarkdown(cell),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                line.isBlank() -> Spacer(Modifier.height(6.dp))
                else -> {
                    Text(
                        text = inlineMarkdown(line),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/** Strips inline bold/italic markers; returns a plain String for now */
private fun inlineMarkdown(text: String): String =
    text
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace(Regex("\\*(.*?)\\*"), "$1")
        .replace(Regex("`(.*?)`"), "$1")
