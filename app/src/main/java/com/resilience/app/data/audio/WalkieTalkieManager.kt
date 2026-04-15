package com.resilience.app.data.audio

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.resilience.app.data.mesh.WifiAwareTransport
import com.resilience.app.data.mesh.MeshDiscoveryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

private const val TAG = "WalkieTalkie"

enum class PttState { IDLE, TRANSMITTING, RECEIVING }

data class RadioSignalPreview(
    val frequency: Int,
    val callSign: String,
    val signalStrength: Int
)

/**
 * Push-To-Talk audio engine with Wi-Fi Aware UDP transport.
 *
 * Audio pipeline (TX):
 *   Mic → PCM 16-bit → [optional Opus encode via concentus] → UDP frame → WifiAwareTransport
 *
 * Audio pipeline (RX):
 *   WifiAwareTransport → [optional Opus decode] → PCM 16-bit → AudioTrack
 *
 * Opus integration: add `implementation("io.github.jaredmdobson:concentus:1.0.1")` to build.gradle,
 * then uncomment the encoder/decoder blocks and wire in OpusEncoder/OpusDecoder from
 * `io.github.jaredmdobson.concentus` package. For now, raw PCM is used for compatibility.
 *
 * Loopback fallback (single device / no peers):
 *   Mic → PCM buffer → AudioTrack (local playback immediately after PTT release)
 */
@Singleton
class WalkieTalkieManager @Inject constructor(
    private val transport: WifiAwareTransport,
    private val meshDiscovery: MeshDiscoveryManager
) {
    companion object {
        private const val SAMPLE_RATE  = 16_000       // 16 kHz voice
        private const val CHANNEL_IN   = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT  = AudioFormat.CHANNEL_OUT_MONO
        private const val ENCODING     = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_SIZE   = 3200         // ~200ms of 16kHz PCM
    }

    private val _pttState = MutableStateFlow(PttState.IDLE)
    val pttState: StateFlow<PttState> = _pttState.asStateFlow()

    private var recordJob: Job?          = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack?  = null

    private val scope = CoroutineScope(Dispatchers.IO)
    private var monitorJob: Job? = null
    private val recordLock = Any()
    private val playbackLock = Any()
    @Volatile
    private var monitoredSignals: List<RadioSignalPreview> = emptyList()

    // Local loopback buffer (used when no peers connected)
    private val capturedFrames = ArrayDeque<ByteArray>()

    // Whether a peer UDP socket is available for actual transmission
    var hasPeerTransport: Boolean = false

    init {
        // Start listening for incoming transport frames immediately
        transport.startReceiving { frame ->
            playIncomingFrame(frame)
        }
        startSignalMonitor()
    }

    // ── PTT Press ────────────────────────────────────────────────────────────

    /**
     * Called when PTT button is pressed.
     * Starts mic capture and streams raw PCM via UDP to peers.
     * Falls back to local loopback if no peers are connected.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startTransmitting(onFrame: ((ByteArray) -> Unit)? = null) {
        val recorder = synchronized(recordLock) {
            if (_pttState.value != PttState.IDLE || recordJob?.isActive == true) {
                return
            }
            capturedFrames.clear()

            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
                .coerceAtLeast(FRAME_SIZE)

            val freshRecorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL_IN, ENCODING, bufferSize
            )

            if (freshRecorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "AudioRecord failed to initialize")
                freshRecorder.release()
                return
            }

            val started = runCatching { freshRecorder.startRecording() }.isSuccess
            if (!started) {
                Log.w(TAG, "AudioRecord failed to start recording")
                freshRecorder.release()
                return
            }

            audioRecord = freshRecorder
            _pttState.value = PttState.TRANSMITTING
            freshRecorder
        }

        recordJob = scope.launch {
            val buffer = ByteArray(FRAME_SIZE)
            while (isActive && _pttState.value == PttState.TRANSMITTING) {
                val bytesRead = runCatching {
                    recorder.read(buffer, 0, buffer.size)
                }.getOrElse { error ->
                    Log.w(TAG, "AudioRecord read failed", error)
                    AudioRecord.ERROR_INVALID_OPERATION
                }

                if (bytesRead <= 0) {
                    if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION ||
                        bytesRead == AudioRecord.ERROR_DEAD_OBJECT
                    ) {
                        break
                    }
                    delay(20)
                    continue
                }

                val frame = buffer.copyOf(bytesRead)
                if (hasPeerTransport) {
                    // -- Opus encode here when concentus is available --
                    // val encoded = opusEncoder.encode(frame)
                    // transport.sendFrame(encoded)
                    transport.sendFrame(frame)  // raw PCM for now
                } else {
                    capturedFrames.addLast(frame)
                }
                onFrame?.invoke(frame)
            }

            synchronized(recordLock) {
                if (audioRecord === recorder) {
                    audioRecord = null
                }
                if (recordJob?.isActive != true) {
                    recordJob = null
                }
            }
        }
    }

    // ── PTT Release ──────────────────────────────────────────────────────────

    /** Called when PTT button is released. */
    fun stopTransmitting(loopbackEnabled: Boolean = true) {
        val recorderToRelease = synchronized(recordLock) {
            if (_pttState.value != PttState.TRANSMITTING && audioRecord == null) {
                return
            }
            _pttState.value = PttState.IDLE
            recordJob?.cancel()
            recordJob = null
            audioRecord.also { audioRecord = null }
        }

        recorderToRelease?.let { recorder ->
            runCatching {
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop()
                }
            }.onFailure { error ->
                Log.w(TAG, "Ignoring AudioRecord stop failure during PTT release", error)
            }
            runCatching { recorder.release() }
        }

        if (loopbackEnabled && capturedFrames.isNotEmpty() && !hasPeerTransport) {
            playbackLoopback()
        }
    }

    // ── Incoming frames from mesh transport ──────────────────────────────────

    /**
     * Called by transport when an audio frame arrives from a peer.
     * Plays through speaker immediately (raw PCM or Opus-decoded).
     */
    fun playIncomingFrame(frame: ByteArray) {
        if (_pttState.value == PttState.TRANSMITTING) return // half-duplex
        if (_pttState.value != PttState.RECEIVING) {
            _pttState.value = PttState.RECEIVING
        }

        scope.launch {
            writeToTrack(frame)
            _pttState.value = PttState.IDLE
        }
    }

    // ── Loopback (single device) ──────────────────────────────────────────────

    private fun playbackLoopback() {
        _pttState.value = PttState.RECEIVING
        scope.launch {
            for (frame in capturedFrames) {
                writeToTrack(frame)
            }
            capturedFrames.clear()
            _pttState.value = PttState.IDLE
        }
    }

    /**
     * Attempts to establish a Wi-Fi Aware data network with a nearby peer.
     * Once connected, [hasPeerTransport] becomes true and audio flows to/from this peer.
     */
    fun connectToPeer(handle: Any) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val peerHandle = handle as? android.net.wifi.aware.PeerHandle ?: return

        // In a mesh, we attempt to bind to the publisher if we are subscribing
        // or vice versa.
        scope.launch {
            val pubSession = meshDiscovery.publishSession
            val subSession = meshDiscovery.subscribeSession
            
            transport.requestWifiAwareNetwork(
                publishSession = pubSession,
                subscribeSession = subSession,
                peerHandle = peerHandle,
                isPublisher = pubSession != null,
                onNetworkUnavailable = {
                    Log.w(TAG, "Peer transport unavailable — reverting to loopback mode")
                    hasPeerTransport = false
                }
            ) { _, inetAddress ->
                hasPeerTransport = true
                val host = inetAddress?.address?.hostAddress
                if (host != null) {
                    transport.setPeerAddress(host)
                }
            }
        }
    }

    // ── Station Monitor ───────────────────────────────────────────────────────

    /**
     * Updates the station monitor with the strongest discovered signals on the
     * currently tuned channel. The monitor stays silent when no signals exist.
     */
    fun updateSignalMonitor(signals: List<RadioSignalPreview>) {
        monitoredSignals = signals
            .sortedByDescending { it.signalStrength }
            .take(4)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Drops the current peer UDP transport without releasing the audio track.
     * Call when the user changes frequency so the next peer connection starts fresh.
     */
    fun resetPeerTransport() {
        hasPeerTransport = false
        transport.release()
        // Re-arm the receive loop so incoming frames on the new channel are handled
        transport.startReceiving { frame -> playIncomingFrame(frame) }
    }

    fun release() {
        monitorJob?.cancel()
        recordJob?.cancel()
        audioRecord?.release()
        audioTrack?.release()
        transport.release()
        audioRecord = null
        audioTrack  = null
        _pttState.value = PttState.IDLE
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun startSignalMonitor() {
        if (monitorJob?.isActive == true) return

        monitorJob = scope.launch {
            while (isActive) {
                val activeSignals = monitoredSignals
                if (_pttState.value != PttState.IDLE || activeSignals.isEmpty()) {
                    delay(180)
                    continue
                }

                for (signal in activeSignals) {
                    if (!isActive || _pttState.value != PttState.IDLE) break
                    writeToTrack(synthesizeSignalBurst(signal))
                    delay(90)
                }

                delay(500)
            }
        }
    }

    private fun synthesizeSignalBurst(signal: RadioSignalPreview): ByteArray {
        val sampleCount = SAMPLE_RATE / 8 // 125 ms
        val pcm = ByteArray(sampleCount * 2)
        val strength = ((signal.signalStrength + 100).coerceIn(8, 55)) / 55f
        val baseFrequency = 260 + (signal.frequency * 9)
        val callSignBias = signal.callSign.sumOf { it.code } % 110
        val toneHz = baseFrequency + callSignBias
        val amplitude = (Short.MAX_VALUE * (0.08f + 0.18f * strength)).toInt()

        for (sampleIndex in 0 until sampleCount) {
            val envelope = when {
                sampleIndex < sampleCount / 6 -> sampleIndex / (sampleCount / 6f)
                sampleIndex > sampleCount * 5 / 6 -> (sampleCount - sampleIndex) / (sampleCount / 6f)
                else -> 1f
            }
            val sweep = 1f + (sampleIndex / sampleCount.toFloat()) * 0.035f
            val sampleValue = (
                sin(2 * PI * toneHz * sweep * sampleIndex / SAMPLE_RATE) *
                    amplitude * envelope
                ).toInt().toShort()
            pcm[sampleIndex * 2] = (sampleValue.toInt() and 0xFF).toByte()
            pcm[sampleIndex * 2 + 1] = ((sampleValue.toInt() shr 8) and 0xFF).toByte()
        }

        return pcm
    }

    private fun writeToTrack(frame: ByteArray) {
        synchronized(playbackLock) {
            runCatching {
                getOrCreateTrack().write(frame, 0, frame.size)
            }.onFailure { error ->
                Log.w(TAG, "AudioTrack write failed", error)
            }
        }
    }

    private fun getOrCreateTrack(): AudioTrack {
        audioTrack?.let { if (it.state == AudioTrack.STATE_INITIALIZED) return it }

        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, ENCODING)
            .coerceAtLeast(FRAME_SIZE)

        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(ENCODING)
                    .setChannelMask(CHANNEL_OUT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also {
                it.play()
                audioTrack = it
            }
    }
}
