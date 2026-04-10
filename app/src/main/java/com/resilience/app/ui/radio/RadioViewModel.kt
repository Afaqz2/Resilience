package com.resilience.app.ui.radio

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
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

    init {
        refreshHardwareCapabilities()
        collectFromManagers()
        // Start discovery immediately so radio is ready
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            meshDiscovery.startDiscovery(_uiState.value.currentFrequency)
        }
        // Enable atmospheric radio static
        walkieTalkie.setStaticEnabled(true)
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
                        statusLine         = buildStatusLine(s.currentFrequency, pttState, agentState, s.isAiMode)
                    )
                }

                // Automatically attempt to pair with Wi-Fi Aware peers on the same frequency
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val activeFrequency = _uiState.value.currentFrequency
                    val peersOnChannel = peerList.filter { 
                        it.frequency == activeFrequency && it.transport == PeerTransport.WIFI_AWARE && it.handle != null
                    }
                    
                    if (peersOnChannel.isNotEmpty() && !walkieTalkie.hasPeerTransport) {
                        val bestPeer = peersOnChannel.maxByOrNull { it.signalStrength }
                        bestPeer?.handle?.let { walkieTalkie.connectToPeer(it) }
                    }
                }
            }.collect()
        }
    }

    // Removing tryConnectToPeers here because it's moved to WalkieTalkieManager

    // ── Frequency / Channel ───────────────────────────────────────────────────

    fun onFrequencyChange(newFrequency: Int) {
        val clamped = newFrequency.coerceIn(1, 99)
        _uiState.update { it.copy(
            currentFrequency = clamped,
            statusLine = buildStatusLine(clamped, it.pttState, it.agentState, it.isAiMode)
        ) }
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
        if (!_uiState.value.hasAudioPermission) return
        if (!_uiState.value.isAiMode) {
            walkieTalkie.startTransmitting()
        }
        // AI mode: nothing on press, STT starts on release
    }

    fun onPttRelease() {
        if (!_uiState.value.hasAudioPermission) return
        if (_uiState.value.isAiMode) {
            voiceAgent.startListening()
        } else {
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
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasAudioPermission = true) }
        refreshHardwareCapabilities()
    }

    /** Refreshes all hardware availability + permission state. */
    fun refreshHardwareCapabilities() {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val btAdapter   = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

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
        aiMode: Boolean
    ): String = when {
        agent == VoiceAgentState.LISTENING   -> "AI LISTENING..."
        agent == VoiceAgentState.PROCESSING  -> "AI PROCESSING..."
        agent == VoiceAgentState.SPEAKING    -> "AI RESPONDING..."
        ptt == PttState.TRANSMITTING         -> "TRANSMITTING — CH $freq"
        ptt == PttState.RECEIVING            -> "RECEIVING — CH $freq"
        aiMode                               -> "AI MODE — CH $freq"
        else                                 -> "STANDBY — CH ${freq.toString().padStart(2, '0')}"
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        meshDiscovery.stopDiscovery()
        walkieTalkie.release()
    }
}
