package com.resilience.app.data.ai

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.os.bundleOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

enum class VoiceAgentState {
    IDLE,
    LISTENING,     // STT active
    PROCESSING,    // RAG/LLM query in progress
    SPEAKING       // TTS playing response
}

/**
 * Offline AI voice assistant pipeline:
 *   PTT audio → Speech-to-Text → SurvivalKnowledgeBase query → Text-to-Speech
 *
 * STT uses Android's built-in [SpeechRecognizer] with PREFER_OFFLINE flag.
 * On devices with an offline language pack installed this works without internet.
 *
 * TODO: Replace SpeechRecognizer with Vosk (https://alphacephei.com/vosk/android)
 *       for guaranteed offline STT that doesn't depend on a Google language pack.
 *       The model files (~50 MB) should be bundled in assets/vosk-model/.
 *
 * TTS uses Android's built-in [TextToSpeech] engine — fully offline on all devices.
 */
@Singleton
class OfflineVoiceAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val knowledgeBase: SurvivalKnowledgeBase
) {
    private val _agentState = MutableStateFlow(VoiceAgentState.IDLE)
    val agentState: StateFlow<VoiceAgentState> = _agentState.asStateFlow()

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.9f)
                isTtsReady = true
            }
        }
    }

    /**
     * Begin listening for a voice query (called when PTT button released in AI mode).
     * The recognized text is automatically fed into the knowledge base and spoken back.
     */
    fun startListening() {
        if (_agentState.value != VoiceAgentState.IDLE) return
        _agentState.value = VoiceAgentState.LISTENING

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val transcript = matches?.firstOrNull() ?: ""
                _lastTranscript.value = transcript
                if (transcript.isNotBlank()) {
                    processQuery(transcript)
                } else {
                    _agentState.value = VoiceAgentState.IDLE
                }
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected."
                    SpeechRecognizer.ERROR_NETWORK   -> "Network error — ensure offline model is installed."
                    else -> "Speech recognition error ($error)."
                }
                _lastResponse.value = msg
                speak(msg)
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        recognizer.startListening(intent)
    }

    /**
     * Submit a text query directly (used by the AI Chat text UI).
     */
    fun processQuery(userText: String) {
        _agentState.value = VoiceAgentState.PROCESSING
        scope.launch(Dispatchers.IO) {
            val response = knowledgeBase.query(userText)
            _lastResponse.value = response
            scope.launch(Dispatchers.Main) {
                speak(response)
            }
        }
    }

    fun speak(text: String) {
        if (!isTtsReady) return
        _agentState.value = VoiceAgentState.SPEAKING
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                _agentState.value = VoiceAgentState.IDLE
            }
            @Deprecated("Deprecated in API 21")
            override fun onError(utteranceId: String?) {
                _agentState.value = VoiceAgentState.IDLE
            }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")
    }

    fun stopSpeaking() {
        tts?.stop()
        _agentState.value = VoiceAgentState.IDLE
    }

    fun release() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        speechRecognizer = null
        tts = null
        isTtsReady = false
    }
}
