package com.resilience.app.data.mesh

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.aware.*
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

private const val AUDIO_PORT = 51314  // UDP port for mesh audio

/**
 * Opens a UDP socket over the Wi-Fi Aware network to stream raw audio frames
 * to/from a connected peer.
 *
 * Usage:
 *  - [openPublisherSocket]  — called by the PTT publisher side (TX)
 *  - [openSubscriberSocket] — called by the receiving side (RX)
 *  - [sendFrame]            — called by [WalkieTalkieManager] with Opus-encoded frames
 *  - [setReceiveCallback]   — ViewModel registers to receive incoming PCM/Opus frames
 */
@Singleton
class WifiAwareTransport @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var udpSendSocket: DatagramSocket?    = null
    private var udpReceiveSocket: DatagramSocket? = null
    private var peerAddress: InetSocketAddress?   = null
    private var receiveCallback: ((ByteArray) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    // ── Publisher (TX) side ───────────────────────────────────────────────────

    /**
     * Bind a plain UDP socket for simple loopback/test use.
     * Full Wi-Fi Aware transport is initiated via [requestWifiAwareNetwork].
     */
    fun openLocalSocket(onReady: (localPort: Int) -> Unit) {
        scope.launch {
            try {
                val socket = DatagramSocket(AUDIO_PORT)
                udpSendSocket = socket
                onReady(AUDIO_PORT)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Request a Wi-Fi Aware network specifier to get a proper bound socket.
     * Requires API 29+. Both sides (publisher and subscriber) must call this.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestWifiAwareNetwork(
        publishSession: PublishDiscoverySession?,
        subscribeSession: SubscribeDiscoverySession?,
        peerHandle: PeerHandle,
        isPublisher: Boolean,
        onNetworkBound: (Network, InetSocketAddress?) -> Unit
    ) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return

        // WifiAwareNetworkSpecifier.Builder takes a DiscoverySession (super-type)
        val discoverySession: DiscoverySession? = if (isPublisher) publishSession else subscribeSession
        if (discoverySession == null) return

        val specifier = WifiAwareNetworkSpecifier.Builder(discoverySession, peerHandle)
            .setPskPassphrase("safereach_mesh_audio")
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(specifier)
            .build()

        connectivityManager.requestNetwork(
            networkRequest,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val linkProperties = connectivityManager.getLinkProperties(network)
                    val localAddr = linkProperties?.linkAddresses?.firstOrNull()?.address
                    val localInet = if (localAddr != null)
                        InetSocketAddress(localAddr, AUDIO_PORT) else null
                    onNetworkBound(network, localInet)
                }
                override fun onUnavailable() { /* timeout */ }
            },
            android.os.Handler(android.os.Looper.getMainLooper()),
            10_000
        )
    }

    // ── UDP Audio Streaming ───────────────────────────────────────────────────

    /** Send an encoded audio frame to [peerAddress]. */
    fun sendFrame(encodedFrame: ByteArray) {
        val dest   = peerAddress ?: return
        val socket = udpSendSocket ?: return
        scope.launch {
            try {
                val packet = DatagramPacket(encodedFrame, encodedFrame.size, dest)
                socket.send(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Start a receive loop that delivers incoming frames to [callback]. */
    fun startReceiving(boundNetwork: Network? = null, callback: (ByteArray) -> Unit) {
        receiveCallback = callback
        scope.launch {
            try {
                val socket = DatagramSocket(null).also { s ->
                    s.reuseAddress = true
                    s.bind(InetSocketAddress(AUDIO_PORT))
                }
                udpReceiveSocket = socket

                // Bind socket to Wi-Fi Aware network if provided
                if (boundNetwork != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    boundNetwork.bindSocket(socket)
                }

                val buffer = ByteArray(4096)
                while (!socket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val frame = packet.data.copyOf(packet.length)
                    callback(frame)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setPeerAddress(host: String, port: Int = AUDIO_PORT) {
        peerAddress = InetSocketAddress(host, port)
    }

    fun release() {
        udpSendSocket?.close()
        udpReceiveSocket?.close()
        udpSendSocket    = null
        udpReceiveSocket = null
        peerAddress      = null
        receiveCallback  = null
    }
}
