package com.resilience.app.data.audio

import android.Manifest
import android.content.Context
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WalkieTalkie"

enum class PttState { IDLE, TRANSMITTING, RECEIVING }

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
    @ApplicationContext private val context: Context,
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
    private var staticJob: Job? = null

    // Local loopback buffer (used when no peers connected)
    private val capturedFrames = ArrayDeque<ByteArray>()

    // Whether a peer UDP socket is available for actual transmission
    var hasPeerTransport: Boolean = false

    init {
        // Start listening for incoming transport frames immediately
        transport.startReceiving { frame ->
            playIncomingFrame(frame)
        }
    }

    // ── PTT Press ────────────────────────────────────────────────────────────

    /**
     * Called when PTT button is pressed.
     * Starts mic capture and streams raw PCM via UDP to peers.
     * Falls back to local loopback if no peers are connected.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startTransmitting(onFrame: ((ByteArray) -> Unit)? = null) {
        if (_pttState.value == PttState.TRANSMITTING) return
        _pttState.value = PttState.TRANSMITTING
        capturedFrames.clear()

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
            .coerceAtLeast(FRAME_SIZE)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE, CHANNEL_IN, ENCODING, bufferSize
        ).also { recorder ->
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                _pttState.value = PttState.IDLE
                return
            }
            recorder.startRecording()
        }

        recordJob = scope.launch {
            val buffer = ByteArray(FRAME_SIZE)
            while (_pttState.value == PttState.TRANSMITTING) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (bytesRead > 0) {
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
            }
        }
    }

    // ── PTT Release ──────────────────────────────────────────────────────────

    /** Called when PTT button is released. */
    fun stopTransmitting(loopbackEnabled: Boolean = true) {
        _pttState.value = PttState.IDLE
        recordJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

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
        _pttState.value = PttState.RECEIVING

        scope.launch {
            // -- Opus decode here when concentus is available --
            // val pcm = opusDecoder.decode(frame)
            // getOrCreateTrack().write(pcm, 0, pcm.size)
            getOrCreateTrack().write(frame, 0, frame.size)
            _pttState.value = PttState.IDLE
        }
    }

    // ── Loopback (single device) ──────────────────────────────────────────────

    private fun playbackLoopback() {
        _pttState.value = PttState.RECEIVING
        scope.launch {
            val track = getOrCreateTrack()
            for (frame in capturedFrames) {
                track.write(frame, 0, frame.size)
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

    // ── Static Noise (Metro UX) ───────────────────────────────────────────────

    /**
     * Plays subtle white noise to simulate an open radio channel.
     * Call with true to start, false to stop.
     */
    fun setStaticEnabled(enabled: Boolean) {
        staticJob?.cancel()
        if (!enabled) return

        staticJob = scope.launch {
            val track = getOrCreateTrack()
            val staticBuffer = ByteArray(2048)
            val random = java.util.Random()
            
            while (_pttState.value == PttState.IDLE) {
                // Generate light static noise
                for (i in staticBuffer.indices) {
                    staticBuffer[i] = (random.nextInt(12) - 6).toByte()
                }
                track.write(staticBuffer, 0, staticBuffer.size)
                kotlinx.coroutines.delay(20)
            }
        }
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
        staticJob?.cancel()
        recordJob?.cancel()
        audioRecord?.release()
        audioTrack?.release()
        transport.release()
        audioRecord = null
        audioTrack  = null
        _pttState.value = PttState.IDLE
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
