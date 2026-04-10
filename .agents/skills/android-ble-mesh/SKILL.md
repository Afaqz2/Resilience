---
name: android-ble-mesh
user-invocable: false
description: Use when implementing Android BLE advertising, scanning, and GATT server/client patterns for mesh networking. Covers BluetoothLeAdvertiser, BluetoothLeScanner, and Wi-Fi Direct P2P for offline peer communication.
allowed-tools:
  - Read
  - Write
  - Edit
  - Bash
  - Grep
  - Glob
---

# Android BLE Mesh & Wi-Fi Direct

Offline peer-to-peer communication patterns using Bluetooth LE (5.0) and Wi-Fi Direct.

## BLE Architecture

### Advertising a Presence Beacon

```kotlin
// Encode frequency + callsign into manufacturer data
// Manufacturer ID: 0xFACE (custom/test)
val data = BluetoothLeAdvertiseData(frequency, callSign) // your serialiser

val advertiseSettings = AdvertiseSettings.Builder()
    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
    .setConnectable(false) // beacon only
    .build()

val advertiseData = AdvertiseData.Builder()
    .setIncludeDeviceName(false)
    .addManufacturerData(0xFACE, payload)
    .build()

bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, callback)
```

### Scanning for BLE Beacons

```kotlin
val scanFilter = ScanFilter.Builder()
    .setManufacturerData(0xFACE, ByteArray(0))  // match manufacturer ID
    .build()

val scanSettings = ScanSettings.Builder()
    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
    .build()

bluetoothLeScanner.startScan(listOf(scanFilter), scanSettings, object : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        val payload = result.scanRecord?.getManufacturerSpecificData(0xFACE) ?: return
        val decoded = String(payload)        // "14:KFX22"
        val parts = decoded.split(":")
        val freq = parts[0].toIntOrNull() ?: return
        val callSign = parts.getOrElse(1) { "ANON" }
        val rssi = result.rssi
        // add to peer list
    }
})
```

## Required Permissions

```xml
<!-- API 31+ -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<!-- API <= 30 -->
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30"/>
<!-- BLE scan needs location on API <= 30 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

Runtime permissions to request:
- `BLUETOOTH_SCAN` (API 31+)
- `BLUETOOTH_ADVERTISE` (API 31+)
- `ACCESS_FINE_LOCATION` (API <= 30 for BLE scan)

## Wi-Fi Direct (P2P) for Audio Streaming

Wi-Fi Direct creates a local AP between two devices, allowing socket-based audio streaming at up to 250 Mbps — significantly longer range than BLE.

### Setup

```kotlin
val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
val channel = manager.initialize(context, mainLooper, null)

// Register broadcast receiver for WIFI_P2P_* intents
val intentFilter = IntentFilter().apply {
    addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
    addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
    addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
}
context.registerReceiver(p2pReceiver, intentFilter)
```

### Peer Discovery

```kotlin
manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
    override fun onSuccess() { /* discovery started */ }
    override fun onFailure(reason: Int) { /* handle */ }
})

// In broadcast receiver, when PEERS_CHANGED:
manager.requestPeers(channel) { peerList ->
    val devices = peerList.deviceList // List<WifiP2pDevice>
}
```

### Connecting & Audio Socket

```kotlin
// Connect to a WifiP2pDevice
val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
manager.connect(channel, config, actionListener)

// After connection, request group info to get GO (Group Owner) IP
manager.requestConnectionInfo(channel) { info ->
    val groupOwnerAddress = info.groupOwnerAddress
    val isGroupOwner = info.isGroupOwner

    if (isGroupOwner) {
        // Start ServerSocket and wait for audio client
        val serverSocket = ServerSocket(PORT)
        val socket = serverSocket.accept()
        streamAudioFrom(socket.inputStream)
    } else {
        // Connect to group owner and stream audio
        val socket = Socket(groupOwnerAddress, PORT)
        streamAudioTo(socket.outputStream)
    }
}
```

## Opus Codec Integration (concentus-java)

```kotlin
// build.gradle.kts
implementation("org.concentus:concentus:1.0.2")

// Encoding 20ms PCM → Opus frame
val encoder = OpusEncoder(16000, 1, OpusApplication.OPUS_APPLICATION_VOIP)
encoder.Bitrate = 16000  // 16 kbps — excellent voice quality
val outputBuffer = ByteArray(4000)
val encodedBytes = encoder.encode(pcmShortArray, 0, frameSize, outputBuffer, 0, outputBuffer.size)
val frame = outputBuffer.copyOf(encodedBytes)

// Decoding
val decoder = OpusDecoder(16000, 1)
val pcmOut = ShortArray(frameSize)
decoder.decode(frame, 0, frame.size, pcmOut, 0, frameSize, false)
```

## Anti-Patterns

- **Don't call `startScan()` continuously** — Android rate-limits scan calls (max 4 per 30s since API 28). Use a handler with 8-second intervals.
- **Don't advertise when screen is off** — use a foreground service with a notification to keep advertising alive.
- **Don't forget to unregister** the P2P broadcast receiver in `onDestroy()`.
- **Don't use BLE for audio streaming** — BLE bandwidth is too low (~27 Kbps max). Use it only for discovery beacons. Route actual audio through Wi-Fi Aware or Wi-Fi Direct.

## Related Patterns

- **Wi-Fi Aware (NAN)**: Higher range (~150m), more structured peer discovery. Prefer over BLE when API 26+ is available.
- **Multicast UDP**: Once peers are connected via Wi-Fi Direct, use UDP datagrams for low-latency audio instead of TCP streams.
