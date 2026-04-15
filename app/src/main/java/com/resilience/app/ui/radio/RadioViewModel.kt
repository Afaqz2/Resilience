package com.resilience.app.ui.radio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resilience.app.data.ai.OfflineVoiceAgent
import com.resilience.app.data.ai.VoiceAgentState
import com.resilience.app.data.audio.PttState
import com.resilience.app.data.audio.RadioSignalPreview
import com.resilience.app.data.audio.WalkieTalkieManager
import com.resilience.app.data.mesh.BleState
import com.resilience.app.data.mesh.MeshDiscoveryManager
import com.resilience.app.data.mesh.MeshState
import com.resilience.app.data.mesh.NearbyBleDevice
import com.resilience.app.data.mesh.NearbyPeer
import com.resilience.app.data.mesh.NearbyWifiNetwork
import com.resilience.app.data.mesh.PeerTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Data Classes ────────────────────────────────────────────────────────────

/** Tracks which hardware radios are available and permitted on this device. */
data class HardwareCapabilities(
    val hasMicPermission: Boolean        = false,
    val hasLocationPermission: Boolean   = false,
    val hasNearbyWifiPermission: Boolean = false,
    val hasBleScanPermission: Boolean    = false,
    val hasBleAdvertisePermission: Boolean = false,
    val hasWifiAwareHardware: Boolean    = false,
    val hasBleHardware: Boolean          = false,
    val isBleEnabled: Boolean            = false,
    val isWifiEnabled: Boolean           = false
)

data class RadioUiState(
    val currentFrequency: Int = 14,
    val isAiMode: Boolean = false,
    val isScannerSheetOpen: Boolean = false,
    val isGuideOpen: Boolean = false,
    val pttState: PttState = PttState.IDLE,
    val meshState: MeshState = MeshState.IDLE,
    val bleState: BleState = BleState.IDLE,
    val agentState: VoiceAgentState = VoiceAgentState.IDLE,
    val nearbyPeers: List<NearbyPeer> = emptyList(),
    val nearbyByFrequency: Map<Int, List<NearbyPeer>> = emptyMap(),
    val nearbyWifiNetworks: List<NearbyWifiNetwork> = emptyList(),
    val nearbyBleDevices: List<NearbyBleDevice> = emptyList(),
    val lastTranscript: String = "",
    val lastResponse: String = "",
    val hasAudioPermission: Boolean = false,
    val statusLine: String = "STANDBY — CH 14",
    val hardware: HardwareCapabilities = HardwareCapabilities(),
    val isAutoTuning: Boolean = false
)

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class RadioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meshDiscovery: MeshDiscoveryManager,
    private val walkieTalkie: WalkieTalkieManager,
    private val voiceAgent: OfflineVoiceAgent
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    // Tracks the peer ID we last attempted to connect to, preventing repeated
    // connection attempts for the same peer on every flow emission.
    private var lastConnectionAttemptPeerId: String? = null

    init {
        refreshHardwareCapabilities()
        collectFromManagers()
        // Start discovery immediately so radio is ready
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            meshDiscovery.startDiscovery(_uiState.value.currentFrequency)
        }
    }

    // ── Manager collection ────────────────────────────────────────────────────

    private fun collectFromManagers() {
        viewModelScope.launch {
            combine(
                meshDiscovery.meshState,
                meshDiscovery.bleState,
                meshDiscovery.nearbyPeers,
                meshDiscovery.nearbyWifi,
                meshDiscovery.nearbyBle,
                walkieTalkie.pttState,
                voiceAgent.agentState,
                voiceAgent.lastTranscript,
                voiceAgent.lastResponse
            ) { values ->
                val meshState    = values[0] as MeshState
                val bleState     = values[1] as BleState
                @Suppress("UNCHECKED_CAST")
                val peerList     = values[2] as List<NearbyPeer>
                @Suppress("UNCHECKED_CAST")
                val wifiNets     = values[3] as List<NearbyWifiNetwork>
                @Suppress("UNCHECKED_CAST")
                val bleDevices   = values[4] as List<NearbyBleDevice>
                val pttState     = values[5] as PttState
                val agentState   = values[6] as VoiceAgentState
                val transcript   = values[7] as String
                val response     = values[8] as String
                val activeFrequency = _uiState.value.currentFrequency
                val peersOnChannel = peerList.filter { it.frequency == activeFrequency }

                _uiState.update { s ->
                    s.copy(
                        meshState          = meshState,
                        bleState           = bleState,
                        nearbyPeers        = peerList,
                        nearbyByFrequency  = peerList.groupBy { it.frequency }.toSortedMap(),
                        nearbyWifiNetworks = wifiNets,
                        nearbyBleDevices   = bleDevices,
                        pttState           = pttState,
                        agentState         = agentState,
                        lastTranscript     = transcript,
                        lastResponse       = response,
                        statusLine         = buildStatusLine(
                            freq = s.currentFrequency,
                            ptt = pttState,
                            agent = agentState,
                            aiMode = s.isAiMode,
                            stationCount = peersOnChannel.size
                        )
                    )
                }

                walkieTalkie.updateSignalMonitor(
                    peersOnChannel.map { peer ->
                        RadioSignalPreview(
                            frequency = peer.frequency,
                            callSign = peer.callSign,
                            signalStrength = peer.signalStrength
                        )
                    }
                )

                // Automatically attempt to pair with the strongest Wi-Fi Aware peer on this channel.
                // Guard: only attempt once per peer ID to avoid repeated requestNetwork() calls
                // on every flow emission while a connection is already in progress.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val wifiAwarePeersOnChannel = peersOnChannel.filter {
                        it.frequency == activeFrequency && it.transport == PeerTransport.WIFI_AWARE && it.handle != null
                    }

                    if (wifiAwarePeersOnChannel.isNotEmpty() && !walkieTalkie.hasPeerTransport) {
                        val bestPeer = wifiAwarePeersOnChannel.maxByOrNull { it.signalStrength }
                        if (bestPeer != null && bestPeer.peerId != lastConnectionAttemptPeerId) {
                            lastConnectionAttemptPeerId = bestPeer.peerId
                            walkieTalkie.connectToPeer(bestPeer.handle!!)
                        }
                    }
                }
            }.collect()
        }
    }

    // Removing tryConnectToPeers here because it's moved to WalkieTalkieManager

    // ── Frequency / Channel ───────────────────────────────────────────────────

    fun onFrequencyChange(newFrequency: Int) {
        val clamped = newFrequency.coerceIn(1, 99)
        val signalsOnChannel = _uiState.value.nearbyPeers
            .filter { peer -> peer.frequency == clamped }
            .map { peer ->
                RadioSignalPreview(
                    frequency = peer.frequency,
                    callSign = peer.callSign,
                    signalStrength = peer.signalStrength
                )
            }
        _uiState.update { it.copy(
            currentFrequency = clamped,
            statusLine = buildStatusLine(
                freq = clamped,
                ptt = it.pttState,
                agent = it.agentState,
                aiMode = it.isAiMode,
                stationCount = signalsOnChannel.size
            )
        ) }
        walkieTalkie.updateSignalMonitor(signalsOnChannel)
        // Reset peer link and connection-attempt guard so the new channel gets a fresh start
        lastConnectionAttemptPeerId = null
        walkieTalkie.resetPeerTransport()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            meshDiscovery.stopDiscovery()
            meshDiscovery.startDiscovery(clamped)
        }
    }

    /** Auto-tunes to the frequency with the most active peers (the ▶▶ button). */
    fun onAutoTune() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAutoTuning = true) }
            delay(1500) // Simulate scanning time
            val best = meshDiscovery.findBestChannel(_uiState.value.currentFrequency)
            if (best != _uiState.value.currentFrequency) {
                onFrequencyChange(best)
            }
            _uiState.update { it.copy(isAutoTuning = false) }
        }
    }

    // ── PTT ───────────────────────────────────────────────────────────────────

    @Suppress("MissingPermission")
    fun onPttPress() {
        val state = _uiState.value
        if (!state.hasAudioPermission) return
        if (state.pttState != PttState.IDLE || state.agentState != VoiceAgentState.IDLE) return
        if (!state.isAiMode) {
            walkieTalkie.startTransmitting()
        }
        // AI mode: nothing on press, STT starts on release
    }

    fun onPttRelease() {
        val state = _uiState.value
        if (!state.hasAudioPermission) return
        if (state.isAiMode) {
            if (state.agentState == VoiceAgentState.IDLE) {
                voiceAgent.startListening()
            }
        } else if (state.pttState == PttState.TRANSMITTING) {
            walkieTalkie.stopTransmitting(loopbackEnabled = true)
        }
    }

    // ── UI toggles ────────────────────────────────────────────────────────────

    fun onToggleAiMode() {
        _uiState.update { it.copy(isAiMode = !it.isAiMode) }
    }

    fun onOpenScanner() {
        _uiState.update { it.copy(isScannerSheetOpen = true) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            meshDiscovery.startDiscovery(_uiState.value.currentFrequency)
        }
    }

    fun onCloseScanner() {
        _uiState.update { it.copy(isScannerSheetOpen = false) }
    }

    fun onTuneToPeer(frequency: Int) {
        onCloseScanner()
        onFrequencyChange(frequency)
    }

    fun onOpenGuide() {
        _uiState.update { it.copy(isGuideOpen = true) }
    }

    fun onCloseGuide() {
        _uiState.update { it.copy(isGuideOpen = false) }
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    /**
     * Called by the UI when a permission result comes back.
     * Re-evaluates all hardware capabilities after any grant/deny.
     */
    fun onPermissionResult() {
        refreshHardwareCapabilities()
        // Re-run discovery so newly-granted BLE/Wi-Fi permissions take effect immediately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            meshDiscovery.startDiscovery(_uiState.value.currentFrequency)
        }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasAudioPermission = true) }
        refreshHardwareCapabilities()
    }

    /** Refreshes all hardware availability + permission state. */
    fun refreshHardwareCapabilities() {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        fun hasPermission(perm: String) =
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

        val hasMic = hasPermission(Manifest.permission.RECORD_AUDIO)
        val hasLocation = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasNearbyWifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            hasPermission(Manifest.permission.NEARBY_WIFI_DEVICES) else true
        val hasBleScan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) else hasPermission(Manifest.permission.BLUETOOTH)
        val hasbleAdv  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE) else hasPermission(Manifest.permission.BLUETOOTH)

        val caps = HardwareCapabilities(
            hasMicPermission         = hasMic,
            hasLocationPermission    = hasLocation,
            hasNearbyWifiPermission  = hasNearbyWifi,
            hasBleScanPermission     = hasBleScan,
            hasBleAdvertisePermission = hasbleAdv,
            hasWifiAwareHardware     = meshDiscovery.isWifiAwareAvailable,
            hasBleHardware           = meshDiscovery.isBleAvailable,
            isBleEnabled             = meshDiscovery.isBleEnabled,
            isWifiEnabled            = wifiManager?.isWifiEnabled == true
        )

        _uiState.update { it.copy(
            hasAudioPermission = hasMic,
            hardware = caps
        ) }
    }

    // ── Status line builder ───────────────────────────────────────────────────

    private fun buildStatusLine(
        freq: Int,
        ptt: PttState,
        agent: VoiceAgentState,
        aiMode: Boolean,
        stationCount: Int
    ): String = when {
        agent == VoiceAgentState.LISTENING   -> "AI LISTENING..."
        agent == VoiceAgentState.PROCESSING  -> "AI PROCESSING..."
        agent == VoiceAgentState.SPEAKING    -> "AI RESPONDING..."
        ptt == PttState.TRANSMITTING         -> "TRANSMITTING — CH $freq"
        ptt == PttState.RECEIVING            -> "RECEIVING — CH $freq"
        aiMode                               -> "AI MODE — CH $freq"
        stationCount > 0                     -> "MONITORING $stationCount STATION${if (stationCount == 1) "" else "S"} — CH ${freq.toString().padStart(2, '0')}"
        else                                 -> "STANDBY — CH ${freq.toString().padStart(2, '0')}"
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        meshDiscovery.stopDiscovery()
        walkieTalkie.release()
    }
}
