package com.meshstream.core.model

import java.time.Instant

/**
 * Describes a node participating in the MeshStream relay network.
 *
 * @property deviceId Stable pseudonymous identifier for this device.
 *   Computed as HMAC-SHA256(hardwareSerial, rotationKey), rotated every 24 hours.
 * @property role The current operational role of this node.
 * @property transport The transport mechanism through which this node was discovered.
 * @property lastSeen Wall-clock time of the most recent contact with this node.
 * @property rssi Received signal strength in dBm, when available from the transport layer.
 */
data class NodeInfo(
    val deviceId: String,
    val role: NodeRole,
    val transport: Transport,
    val lastSeen: Instant,
    val rssi: Int? = null,
)

/**
 * The operational role a MeshStream node can take on.
 * A single device may act in multiple roles simultaneously (e.g., RECORDER + RELAY).
 */
enum class NodeRole {
    /** Records video and produces encrypted chunks. */
    RECORDER,

    /** Receives chunks from peers, stores them, and forwards onward. */
    RELAY,

    /**
     * Has internet access and uploads received chunks to the MeshStream server.
     * Any RELAY node that gains connectivity automatically promotes itself to INITIATOR.
     */
    INITIATOR,
}

/**
 * The physical transport layer used to communicate with a peer node.
 */
enum class Transport {
    /** IEEE 802.11 Wi-Fi Direct (P2P group owner / client model). */
    WIFI_DIRECT,

    /**
     * Wi-Fi Aware (Neighbour Awareness Networking — NAN).
     * Requires Android 8.0+ (API 26) and hardware NAN support.
     */
    WIFI_AWARE,

    /** Bluetooth Classic (RFCOMM) or Bluetooth Low Energy (GATT). */
    BLUETOOTH,
}
