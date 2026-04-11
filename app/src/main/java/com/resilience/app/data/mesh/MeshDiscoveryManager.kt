package com.resilience.app.data.mesh

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.aware.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// ── Data Models ─────────────────────────────────────────────────────────────

data class NearbyPeer(
    val peerId: String,
    val callSign: String,
    val frequency: Int,
    val signalStrength: Int = -60,  // dBm estimate
    val transport: PeerTransport = PeerTransport.WIFI_AWARE,
    val handle: Any? = null        // Wi-Fi Aware PeerHandle (if applicable)
)

enum class PeerTransport { WIFI_AWARE, BLE }

/** A nearby Wi-Fi network found during scan (non-SaveReach networks shown in scanner) */
data class NearbyWifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val isSaveReachPeer: Boolean = false,
    val peerFrequency: Int? = null
)

/** A nearby BLE device found during scan (SaveReach peers decoded from manufacturer data) */
data class NearbyBleDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val isSaveReachPeer: Boolean = false,
    val peerFrequency: Int? = null,
    val peerCallSign: String? = null
)

enum class MeshState { IDLE, ADVERTISING, SCANNING, CONNECTED }
enum class BleState   { IDLE, ADVERTISING, SCANNING }

// ── Manager ──────────────────────────────────────────────────────────────────

/**
 * Multi-transport peer discovery manager.
 *
 * Transport priority:
 *   1. Wi-Fi Aware (NAN) — best range + audio-capable
 *   2. BLE advertising/scanning — fallback beacon on older devices
 *   3. Wi-Fi scan — ambient network list for scanner sheet UI
 *
 * Call [startDiscovery] when user opens the radio screen or scanner.
 * Call [stopDiscovery] on screen exit (called by ViewModel.onCleared).
 */
@Singleton
class MeshDiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG               = "MeshDiscovery"
        private const val SERVICE_NAME      = "safereach_mesh"
        private const val MAX_PEERS         = 50
        private const val BLE_MANUFACTURER  = 0xFACE  // custom manufacturer ID (test range)
        private const val WIFI_SCAN_INTERVAL = 10_000L // ms between Wi-Fi scans
        private const val PREFS_NAME        = "mesh_prefs"
        private const val PREF_CALL_SIGN    = "call_sign"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Stable call sign persisted across restarts so peers see a consistent identity. */
    private val callSign: String by lazy {
        prefs.getString(PREF_CALL_SIGN, null) ?: generateCallSign().also { cs ->
            prefs.edit().putString(PREF_CALL_SIGN, cs).apply()
        }
    }

    // ── Shared State ─────────────────────────────────────────────────────────
    private val _meshState   = MutableStateFlow(MeshState.IDLE)
    val meshState: StateFlow<MeshState> = _meshState.asStateFlow()

    private val _bleState    = MutableStateFlow(BleState.IDLE)
    val bleState: StateFlow<BleState> = _bleState.asStateFlow()

    private val _nearbyPeers = MutableStateFlow<List<NearbyPeer>>(emptyList())
    val nearbyPeers: StateFlow<List<NearbyPeer>> = _nearbyPeers.asStateFlow()

    private val _nearbyWifi  = MutableStateFlow<List<NearbyWifiNetwork>>(emptyList())
    val nearbyWifi: StateFlow<List<NearbyWifiNetwork>> = _nearbyWifi.asStateFlow()

    private val _nearbyBle   = MutableStateFlow<List<NearbyBleDevice>>(emptyList())
    val nearbyBle: StateFlow<List<NearbyBleDevice>> = _nearbyBle.asStateFlow()

    // ── Wi-Fi Aware ───────────────────────────────────────────────────────────
    private var wifiAwareSession: WifiAwareSession?      = null
    var publishSession: PublishDiscoverySession? = null
        private set
    var subscribeSession: SubscribeDiscoverySession? = null
        private set

    // ── BLE ────────────────────────────────────────────────────────────────────
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }
    private var bleScanner: BluetoothLeScanner?    = null
    private var bleAdvertiser: BluetoothLeAdvertiser? = null
    private var bleScanCallback: ScanCallback?     = null

    // ── Hardware availability ─────────────────────────────────────────────────
    val isWifiAwareAvailable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.packageManager.hasSystemFeature("android.hardware.wifi.aware")

    val isBleAvailable: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
                bluetoothAdapter != null

    val isBleEnabled: Boolean get() = bluetoothAdapter?.isEnabled == true

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Start all available discovery transports on [frequency].
     * Safe to call multiple times — stops previous sessions first.
     */
    fun startDiscovery(frequency: Int) {
        stopDiscovery()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isWifiAwareAvailable) {
            startWifiAwareDiscovery(frequency)
        }

        if (isBleAvailable && isBleEnabled) {
            startBleDiscovery(frequency)
        }

        startWifiNetworkScan()
    }

    fun stopDiscovery() {
        stopWifiAware()
        stopBle()
        _nearbyPeers.value = emptyList()
        _meshState.value   = MeshState.IDLE
    }

    /**
     * Returns the channel number with the most active peers.
     * If no peers are found, returns [currentFrequency] (no-op).
     * Used by the ▶▶ auto-tune button.
     */
    fun findBestChannel(currentFrequency: Int): Int {
        val peers = _nearbyPeers.value
        if (peers.isEmpty()) return currentFrequency

        // Prefer the frequency with the most peers, breaking ties by strongest signal
        return peers
            .groupBy { it.frequency }
            .maxByOrNull { (_, peers) ->
                peers.size * 1000 + peers.maxOf { it.signalStrength + 100 }
            }?.key ?: currentFrequency
    }

    /** Groups nearby peers by frequency for the scanner sheet. */
    fun peersGroupedByFrequency(): Map<Int, List<NearbyPeer>> =
        _nearbyPeers.value.groupBy { it.frequency }.toSortedMap()

    // ── Wi-Fi Aware ───────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startWifiAwareDiscovery(frequency: Int) {
        val wifiAwareManager = context.getSystemService(Context.WIFI_AWARE_SERVICE)
            as? WifiAwareManager ?: return

        wifiAwareManager.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession) {
                wifiAwareSession = session
                publishPresence(session, frequency)
                subscribeToPeers(session)
                _meshState.value = MeshState.ADVERTISING
            }

            override fun onAttachFailed() {
                Log.w(TAG, "Wi-Fi Aware attach failed — NAN unavailable on this device/session")
                _meshState.value = MeshState.IDLE
            }
        }, null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun publishPresence(session: WifiAwareSession, frequency: Int) {
        val payload = "$frequency:$callSign".toByteArray(Charsets.UTF_8)

        val config = PublishConfig.Builder()
            .setServiceName(SERVICE_NAME)
            .setServiceSpecificInfo(payload)
            .build()

        session.publish(config, object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                publishSession = session
            }
        }, null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun subscribeToPeers(session: WifiAwareSession) {
        val config = SubscribeConfig.Builder()
            .setServiceName(SERVICE_NAME)
            .build()

        session.subscribe(config, object : DiscoverySessionCallback() {
            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                subscribeSession = session
                _meshState.value = MeshState.SCANNING
            }

            override fun onServiceDiscovered(
                peerHandle: PeerHandle,
                serviceSpecificInfo: ByteArray,
                matchFilter: List<ByteArray>
            ) {
                if (serviceSpecificInfo.isEmpty()) return
                val payload = try {
                    serviceSpecificInfo.toString(Charsets.UTF_8)
                } catch (e: Exception) {
                    Log.w(TAG, "Wi-Fi Aware: malformed peer payload, skipping")
                    return
                }
                val parts = payload.split(":")
                if (parts.size < 2) return

                val peerFrequency = parts[0].trim().toIntOrNull() ?: return
                val peerCallSign  = parts[1].trim().takeIf { it.isNotEmpty() } ?: return
                val peerId        = peerHandle.hashCode().toString()

                addOrUpdatePeer(NearbyPeer(
                    peerId         = peerId,
                    callSign       = peerCallSign,
                    frequency      = peerFrequency,
                    signalStrength = -60,
                    transport      = PeerTransport.WIFI_AWARE,
                    handle         = peerHandle
                ))
            }

            override fun onSessionTerminated() { _meshState.value = MeshState.IDLE }
        }, null)
    }

    private fun stopWifiAware() {
        publishSession?.close();   publishSession    = null
        subscribeSession?.close(); subscribeSession  = null
        wifiAwareSession?.close(); wifiAwareSession  = null
    }

    // ── BLE Discovery (fallback) ──────────────────────────────────────────────

    private fun startBleDiscovery(frequency: Int) {
        val adapter = bluetoothAdapter ?: return

        val canAdvertise = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                PackageManager.PERMISSION_GRANTED

        val canScan = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED

        if (!canAdvertise) {
            Log.w(TAG, "BLUETOOTH_ADVERTISE not granted — skipping BLE advertising")
        } else {
            startBleAdvertising(adapter, frequency)
        }

        if (!canScan) {
            Log.w(TAG, "BLUETOOTH_SCAN not granted — skipping BLE scanning")
        } else {
            startBleScanning(adapter)
        }
    }

    @Suppress("MissingPermission")
    private fun startBleAdvertising(adapter: BluetoothAdapter, frequency: Int) {
        val advertiser = adapter.bluetoothLeAdvertiser ?: return
        bleAdvertiser  = advertiser

        val payload = "$frequency:$callSign".toByteArray(Charsets.UTF_8)

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(BLE_MANUFACTURER, payload)
            .build()

        advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                _bleState.value = BleState.ADVERTISING
            }
            override fun onStartFailure(errorCode: Int) {
                Log.w(TAG, "BLE advertising failed, errorCode=$errorCode — peers will not see this device via BLE")
                _bleState.value = BleState.IDLE
            }
        })
    }

    @Suppress("MissingPermission")
    private fun startBleScanning(adapter: BluetoothAdapter) {
        val scanner    = adapter.bluetoothLeScanner ?: return
        bleScanner     = scanner

        val filter = ScanFilter.Builder()
            .setManufacturerData(BLE_MANUFACTURER, ByteArray(0))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                processBleResult(result)
            }
            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { processBleResult(it) }
            }
        }
        bleScanCallback = callback

        // Only scan for 10 s at a time (Android rate-limits BLE scans)
        scanner.startScan(listOf(filter), settings, callback)
        _bleState.value = BleState.SCANNING

        scope.launch {
            delay(10_000)
            @Suppress("MissingPermission")
            scanner.stopScan(callback)
            _bleState.value = BleState.IDLE
        }
    }

    private fun processBleResult(result: ScanResult) {
        val deviceAddress = result.device.address
        val rssi          = result.rssi

        @Suppress("MissingPermission")
        val deviceName  = result.device.name ?: "UNKNOWN"

        // Try to decode SafeReach payload
        val payload = result.scanRecord?.getManufacturerSpecificData(BLE_MANUFACTURER)
        val decoded = payload?.let {
            try { it.toString(Charsets.UTF_8) } catch (e: Exception) { "" }
        } ?: ""
        val parts = decoded.split(":")
        val isSaveReach = parts.size >= 2

        // Update BLE device list (non-SaveReach devices shown in scanner too)
        updateBleDevices(NearbyBleDevice(
            address        = deviceAddress,
            name           = deviceName,
            rssi           = rssi,
            isSaveReachPeer = isSaveReach,
            peerFrequency  = if (isSaveReach) parts[0].toIntOrNull() else null,
            peerCallSign   = if (isSaveReach) parts.getOrNull(1) else null
        ))

        // Also add as NearbyPeer if it's a SaveReach device
        if (isSaveReach) {
            val frequency = parts[0].toIntOrNull() ?: return
            val callSign  = parts.getOrElse(1) { "ANON" }
            addOrUpdatePeer(NearbyPeer(
                peerId         = deviceAddress,
                callSign       = callSign,
                frequency      = frequency,
                signalStrength = rssi,
                transport      = PeerTransport.BLE
            ))
        }
    }

    @Suppress("MissingPermission")
    private fun stopBle() {
        bleScanCallback?.let { bleScanner?.stopScan(it) }
        bleAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        bleScanCallback = null
        bleScanner      = null
        bleAdvertiser   = null
        _bleState.value = BleState.IDLE
        _nearbyBle.value = emptyList()
    }

    // ── Wi-Fi Network Scan ────────────────────────────────────────────────────

    private fun startWifiNetworkScan() {
        scope.launch {
            while (true) {
                scanWifiNetworks()
                delay(WIFI_SCAN_INTERVAL)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun scanWifiNetworks() {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE)
            as? WifiManager ?: return

        if (!wifiManager.isWifiEnabled) {
            _nearbyWifi.value = emptyList()
            return
        }

        // startScan is deprecated API 28+ but still functional; use results immediately
        wifiManager.startScan()

        val scanResults = wifiManager.scanResults ?: return
        val networks = scanResults
            .filter { it.SSID.isNotBlank() }
            .sortedByDescending { it.level }
            .take(20)
            .map { result ->
                val ssid = result.SSID
                // Detect SaveReach peer hotspot by SSID prefix convention
                val isSaveReach = ssid.startsWith("SR-MESH-")
                val peerFreq = if (isSaveReach) {
                    ssid.removePrefix("SR-MESH-").toIntOrNull()
                } else null
                NearbyWifiNetwork(
                    ssid            = ssid,
                    bssid           = result.BSSID ?: "",
                    rssi            = result.level,
                    isSaveReachPeer = isSaveReach,
                    peerFrequency   = peerFreq
                )
            }
        _nearbyWifi.value = networks
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun addOrUpdatePeer(peer: NearbyPeer) {
        val current    = _nearbyPeers.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.peerId == peer.peerId }
        if (existingIdx >= 0) {
            current[existingIdx] = peer
        } else if (current.size < MAX_PEERS) {
            current.add(peer)
        }
        _nearbyPeers.value = current
    }

    private fun updateBleDevices(device: NearbyBleDevice) {
        val current    = _nearbyBle.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.address == device.address }
        if (existingIdx >= 0) {
            current[existingIdx] = device
        } else if (current.size < 30) {
            current.add(device)
        }
        _nearbyBle.value = current.sortedByDescending { it.rssi }
    }

    private fun generateCallSign(): String {
        val letters = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        return (1..3).map { letters.random() }.joinToString("") +
               (10..99).random().toString()
    }
}
