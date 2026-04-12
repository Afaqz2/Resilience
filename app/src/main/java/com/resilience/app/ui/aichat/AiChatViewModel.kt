package com.resilience.app.ui.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resilience.app.data.ai.SurvivalKnowledgeBase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

data class AiChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = 0L,
            text = "SURVIVAL AI ONLINE\n\nAsk me anything about first aid, shelter, water purification, navigation, or emergency signalling.\n\n[Survival data module loading — stub responses active until corpus is ready]",
            isUser = false
        )
    ),
    val inputText: String = "",
    val isProcessing: Boolean = false
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val knowledgeBase: SurvivalKnowledgeBase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isProcessing) return

        val userMsg = ChatMessage(text = text, isUser = true)
        val loadingMsg = ChatMessage(id = -1L, text = "...", isUser = false, isLoading = true)

        _uiState.update {
            it.copy(
                messages    = it.messages + userMsg + loadingMsg,
                inputText   = "",
                isProcessing = true
            )
        }

        viewModelScope.launch {
            val response = knowledgeBase.query(text)
            val aiMsg = ChatMessage(text = response, isUser = false)

            _uiState.update {
                it.copy(
                    messages     = it.messages.filter { m -> !m.isLoading } + aiMsg,
                    isProcessing = false
                )
            }
        }
    }
}
