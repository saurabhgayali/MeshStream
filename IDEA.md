# MeshStream — Architecture & Implementation Plan

> This document is the authoritative specification for MeshStream.  
> **Read this before proposing or implementing any new feature.**

---

## Table of Contents

1. [Vision](#1-vision)
2. [Constraints & Non-Goals](#2-constraints--non-goals)
3. [System Overview](#3-system-overview)
4. [Data Model](#4-data-model)
5. [Module Responsibilities](#5-module-responsibilities)
6. [Mesh Networking Strategy](#6-mesh-networking-strategy)
7. [Security Model](#7-security-model)
8. [Storage & Lifecycle Management](#8-storage--lifecycle-management)
9. [Server Component](#9-server-component)
10. [Implementation Phases](#10-implementation-phases)
11. [Design Decisions & Trade-offs](#11-design-decisions--trade-offs)
12. [Open Questions](#12-open-questions)

---

## 1. Vision

MeshStream enables a group of Android devices to form a self-healing relay network. A journalist records video on one device; that video is sliced into encrypted chunks and propagated hop-by-hop through the mesh until a node with internet access uploads the chunks to a secure server. No internet connection is required at the recording device.

**Core invariants:**
- A chunk that leaves the recording device is always encrypted.
- A chunk is only deleted from a device after a verified delivery receipt is received.
- The network functions even when every node is mobile and offline simultaneously ("store-and-forward").
- No single node is a required coordinator; any node may become an Initiator.

---

## 2. Constraints & Non-Goals

### Constraints
- Target API 26+ (Android 8.0) — minimum for Wi-Fi Aware (`android.net.wifi.aware`)
- Kotlin-first; no Java new code
- No third-party mesh SDK (rely on Android platform APIs)
- Battery budget: background recording + discovery must not drain > 15 % per hour on a mid-range device
- Storage: chunk files must not accumulate beyond a configurable cap (default 2 GB)

### Non-Goals (v1)
- Real-time streaming / live preview to server
- iOS client
- End-to-end re-encryption at relay nodes (relays forward opaque ciphertext)
- User authentication / login flows
- Video editing or transcoding

---

## 3. System Overview

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         MeshStream Device                                    │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │                         Android App Process                           │  │
│  │                                                                        │  │
│  │  ┌─────────────┐    raw video     ┌──────────────┐   plaintext chunk  │  │
│  │  │  Recording  │ ─────────────▶  │    Chunker   │ ─────────────────▶ │  │
│  │  │  Service    │                 │  (MediaMuxer)│                    │  │
│  │  └─────────────┘                 └──────────────┘                    │  │
│  │                                                                        │  │
│  │                              plaintext chunk                           │  │
│  │                                     │                                  │  │
│  │                                     ▼                                  │  │
│  │                          ┌──────────────────┐                          │  │
│  │                          │   Crypto Module  │  AES-256-GCM             │  │
│  │                          │ (Android Keystore│  per-chunk key           │  │
│  │                          └────────┬─────────┘                          │  │
│  │                                   │ encrypted chunk                    │  │
│  │                                   ▼                                    │  │
│  │                          ┌──────────────────┐                          │  │
│  │                          │  Storage Module  │  Room + encrypted files  │  │
│  │                          │  (ChunkStore)    │                          │  │
│  │                          └────────┬─────────┘                          │  │
│  │                                   │                                    │  │
│  │                         ┌─────────▼──────────┐                         │  │
│  │                         │   Relay Manager    │                         │  │
│  │                         │ (orchestration +   │                         │  │
│  │                         │  discovery loop)   │                         │  │
│  │                         └──┬──────────────┬──┘                         │  │
│  │                            │              │                            │  │
│  │                   ┌────────▼───┐  ┌───────▼──────┐                    │  │
│  │                   │ Wi-Fi      │  │  Bluetooth   │                    │  │
│  │                   │ Direct /   │  │  Transport   │                    │  │
│  │                   │ Wi-Fi Aware│  └──────────────┘                    │  │
│  │                   └────────────┘                                      │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
         mesh hop (encrypted chunks + receipts)
              │
              ▼
    ┌──────────────────────┐
    │   Peer Node(s)       │  ← stores, forwards, eventually delivers
    └──────────────────────┘
              │
              ▼ (when internet available)
    ┌──────────────────────┐    HTTPS POST    ┌─────────────────────┐
    │   Initiator Node     │ ───────────────▶ │  MeshStream Server  │
    │  (internet access)   │                  │  (Kotlin/JVM)       │
    └──────────────────────┘                  └─────────────────────┘
```

### Request / Response Lifecycle

```
Recorder ──[plaintext]──▶ Chunker ──[plaintext]──▶ Crypto ──[ciphertext]──▶ ChunkStore
                                                                               │
                                                                    Relay Manager polls
                                                                               │
                                                                 ┌─────────────▼──────────────┐
                                                                 │ Peer or Initiator discovered│
                                                                 └─────────────┬──────────────┘
                                                                               │
                                                        chunk transferred (encrypted over transport)
                                                                               │
                                                                               ▼
                                                                    Peer stores in its ChunkStore
                                                                               │
                                                               (receipt propagates back on ACK)
                                                                               │
                                                                               ▼
                                                               ChunkStore marks chunk DELIVERED
                                                                               │
                                                                    Secure deletion scheduled
```

---

## 4. Data Model

### `VideoChunk`

```
VideoChunk {
  id            : UUID           // globally unique (generated on originating device)
  sessionId     : UUID           // recording session this chunk belongs to
  sequenceNumber: Long           // 0-based monotonic index within session
  durationMs    : Long           // actual duration of this chunk
  createdAt     : Instant        // wall-clock time on recording device
  sizeBytes     : Long           // encrypted payload size
  sha256Digest  : ByteArray      // SHA-256 of the encrypted payload
  keyHandle     : String         // Keystore alias for this chunk's DEK
  status        : ChunkStatus    // PENDING | QUEUED | IN_FLIGHT | DELIVERED | DELETED
  hops          : Int            // number of relay hops traversed so far
  originDeviceId: String         // stable pseudonymous device identifier
}
```

### `ChunkStatus` (state machine)

```
PENDING ──▶ QUEUED ──▶ IN_FLIGHT ──▶ DELIVERED ──▶ DELETED
               │                         │
               └─────── FAILED ──────────┘
                       (retried)
```

### `NodeInfo`

```
NodeInfo {
  deviceId  : String     // stable pseudonymous identifier (HMAC of hardware serial)
  role      : NodeRole   // RECORDER | RELAY | INITIATOR
  transport : Transport  // WIFI_DIRECT | WIFI_AWARE | BLUETOOTH
  lastSeen  : Instant
  rssi      : Int?       // signal strength if available
}
```

### `DeliveryReceipt`

```
DeliveryReceipt {
  chunkId    : UUID
  receiverId : String    // device that confirmed receipt
  timestamp  : Instant
  hmac       : ByteArray // HMAC-SHA256(chunkId + receiverId + timestamp, sharedSecret)
}
```

### `RecordingSession`

```
RecordingSession {
  id         : UUID
  startedAt  : Instant
  endedAt    : Instant?
  totalChunks: Int
  resolution : VideoResolution
  frameRate  : Int
}
```

---

## 5. Module Responsibilities

### `core`

Pure Kotlin (no Android framework dependency). Contains:

- Domain models (`VideoChunk`, `NodeInfo`, `DeliveryReceipt`, `RecordingSession`)
- Repository interfaces (`ChunkRepository`, `NodeRepository`, `SessionRepository`)
- Use-case classes (`RecordAndChunkUseCase`, `PropagateChunksUseCase`, `VerifyAndDeleteUseCase`)
- Domain events (`ChunkProducedEvent`, `ChunkDeliveredEvent`, `PeerDiscoveredEvent`)
- Extension functions and pure utility code

**Rule:** No Android imports, no Hilt annotations, no I/O in this module.

### `feature/recorder`

- `ChunkRecorder` interface
- `MediaRecorderChunkRecorder` — wraps `MediaRecorder` + `MediaMuxer` to produce fixed-duration MP4 chunks
- `RecordingForegroundService` — manages lifecycle, wakelock, notifications
- Output: raw (unencrypted) chunk files on internal storage; emits `ChunkProducedEvent`

### `feature/crypto`

- `ChunkCipher` interface
- `AesGcmChunkCipher` — AES-256-GCM encryption/decryption backed by Android Keystore
- `KeyManager` — generates, stores, and retrieves per-chunk Data Encryption Keys (DEKs)
- Digest computation (SHA-256) and HMAC computation

### `feature/mesh`

- `MeshTransport` interface (send chunk, receive chunk, discover peers)
- `WifiDirectTransport` — uses `WifiP2pManager`
- `WifiAwareTransport` — uses `WifiAwareManager` (API 26+)
- `BluetoothTransport` — fallback for low-bandwidth delivery of small chunks
- `PeerDiscoveryManager` — aggregates all transports, emits `PeerDiscoveredEvent`

### `feature/storage`

- `ChunkStore` interface (CRUD for encrypted chunks + metadata)
- `RoomChunkStore` — Room database for metadata; encrypted files in `filesDir`
- `ChunkEntity` — Room entity mirroring `VideoChunk`
- `StorageQuotaManager` — enforces configurable storage cap, triggers emergency deletion

### `feature/relay`

- `RelayManager` — orchestrates the propagation loop:
  1. Discover peers via `PeerDiscoveryManager`
  2. Exchange chunk manifests (which chunks each peer needs)
  3. Select chunks to forward based on priority (sequence number, hop count)
  4. Encrypt-in-transit (TLS over transport channel)
  5. Await receipt and update `ChunkStatus`
- `ChunkPrioritizer` — determines forwarding order (FIFO by sequence number, prefer low-hop)
- `ReceiptVerifier` — validates `DeliveryReceipt` HMAC before marking chunk as delivered

### `app`

- Hilt application class
- Navigation graph (Compose Navigation)
- Main recording screen
- Peer status screen
- Settings screen
- Wires all feature modules together via Hilt dependency injection

### `server`

- Kotlin/JVM (no Android dependency)
- HTTP endpoint to receive encrypted chunks (`POST /chunks`)
- Signature verification and chunk integrity check
- Persistent storage (initially: local filesystem; later: object store)
- Session reassembly API (`GET /sessions/{id}/stream`)
- Issuance of delivery receipts

---

## 6. Mesh Networking Strategy

### Transport Priority

| Transport | Bandwidth | Range | Battery | Use when |
|-----------|-----------|-------|---------|----------|
| Wi-Fi Aware | ~100 Mbps | ~100 m | Medium | Peers nearby, Android 8+ |
| Wi-Fi Direct | ~50 Mbps | ~200 m | Medium-High | One-to-one large transfers |
| Bluetooth Classic | ~3 Mbps | ~10 m | Low | Last-resort small chunks |

### Discovery Loop

```
while (recording or hasUndeliveredChunks) {
    discover peers via all transports (parallel, 30 s window)
    for each discovered peer:
        exchange manifest (set of chunk IDs I have vs. peer needs)
        if peer needs chunks I have:
            open transport channel
            forward chunks in priority order
            await receipt
        if peer has chunks I need (relay role):
            accept incoming chunks
    sleep configurable interval (default 60 s)
}
```

### Manifest Protocol (v1 — simple)

```
ManifestRequest  { deviceId, myChunkIds: List<UUID> }
ManifestResponse { deviceId, wantedChunkIds: List<UUID> }
ChunkTransfer    { chunkId, encryptedPayload, sha256Digest }
ChunkAck         { chunkId, receiverId, hmac }
```

All messages are serialized with Protocol Buffers (future) or JSON (v1).

---

## 7. Security Model

### Key Hierarchy

```
Master Key (device-scoped, Android Keystore, never exported)
    │
    └── Session Key (per recording session, HKDF-derived, stored encrypted)
            │
            └── Chunk DEK (per chunk, AES-256, stored encrypted under Session Key)
```

### Encryption

- Algorithm: **AES-256-GCM**
- IV: 96-bit random per chunk (included in ciphertext header)
- AAD: `chunkId || sequenceNumber || sessionId` (bound to chunk identity)
- Key storage: Android Keystore with `StrongBoxBacked = true` when available

### Transport Security

- All transport channels use **TLS 1.3** where the platform supports it, or DTLS for datagram transports.
- Peer identity: each device generates an EC P-256 key pair on first launch; the public key is exchanged during discovery and pinned.

### Receipt Authentication

- `DeliveryReceipt.hmac = HMAC-SHA256(chunkId ‖ receiverId ‖ timestamp, sharedSecret)`
- `sharedSecret` = ECDH(originPrivateKey, receiverPublicKey)

### Secure Deletion

- Chunk files are overwritten with random bytes before `File.delete()`.
- Room records are hard-deleted (no soft-delete for delivered chunks).

### Privacy

- Device identifiers are pseudonymous: `HMAC-SHA256(hardwareSerial, rotationKey)`.
- The rotation key changes every 24 hours.
- No analytics, crash reporting, or telemetry.

---

## 8. Storage & Lifecycle Management

### Chunk Lifecycle

```
Created (encrypted file + Room record)
    │
    ├── status = PENDING (waiting for peer discovery)
    │
    ├── status = QUEUED (peer found, scheduled for transfer)
    │
    ├── status = IN_FLIGHT (transfer in progress)
    │
    ├── status = DELIVERED (receipt verified)
    │       └── schedule secure deletion in 10 min (grace period)
    │
    └── status = DELETED (file overwritten and deleted, Room record purged)
```

### Storage Quota

- Default cap: **2 GB** (configurable in settings)
- When storage > 80 % of cap: pause new recording
- When storage > 90 % of cap: drop oldest DELIVERED chunks immediately
- When storage > 95 % of cap: emergency alert to user

### Retention Policy

- Chunks that have been forwarded but receipt not yet received: retain for **72 hours**
- After 72 hours without receipt: re-queue for forwarding (reset to QUEUED)
- Chunks that have never left the device: retain indefinitely (bounded by quota)

---

## 9. Server Component

The server is a standalone Kotlin/JVM application (no Android dependency) that runs on a device with internet access or a cloud VM.

### API (v1)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/chunks` | Ingest an encrypted chunk |
| `GET` | `/api/v1/chunks/{id}` | Check ingestion status |
| `POST` | `/api/v1/receipts` | Issue a delivery receipt |
| `GET` | `/api/v1/sessions/{id}` | Get session metadata |
| `GET` | `/api/v1/sessions/{id}/chunks` | List chunks for a session |

### Chunk Ingestion Request

```json
{
  "chunkId": "uuid",
  "sessionId": "uuid",
  "sequenceNumber": 42,
  "sha256Digest": "base64",
  "encryptedPayload": "base64",
  "senderDeviceId": "pseudonymous-id"
}
```

### Tech Stack

- **Ktor** or **Javalin** (lightweight HTTP server)
- **Exposed** or **SQLite** for metadata
- **File system** or **S3-compatible** object store for chunk files
- Docker-packaged for easy deployment

---

## 10. Implementation Phases

### Phase 1 — Foundation (current)

- [x] Repository created
- [x] README and IDEA documentation
- [ ] Android project scaffold (Gradle, modules, version catalog)
- [ ] `core` domain models and interfaces
- [ ] `feature/crypto` — AES-GCM encryption with Android Keystore
- [ ] `feature/storage` — Room database and file storage
- [ ] Unit tests for core domain

### Phase 2 — Recording

- [ ] `feature/recorder` — `MediaRecorder` foreground service
- [ ] Fixed-duration chunk slicing with `MediaMuxer`
- [ ] Integration: recording → encryption → storage pipeline
- [ ] Instrumented tests for recording pipeline

### Phase 3 — Mesh Transport

- [ ] `feature/mesh` — Wi-Fi Aware transport (primary)
- [ ] `feature/mesh` — Wi-Fi Direct transport (secondary)
- [ ] Peer discovery and manifest exchange
- [ ] Chunk transfer and receipt flow
- [ ] Integration tests with emulated mesh environment

### Phase 4 — Relay Orchestration

- [ ] `feature/relay` — `RelayManager` discovery loop
- [ ] `ChunkPrioritizer` and `ReceiptVerifier`
- [ ] Storage quota management and lifecycle
- [ ] End-to-end test: recorder → relay → initiator upload

### Phase 5 — Server & Upload

- [ ] `server` — Ktor HTTP server scaffold
- [ ] Chunk ingestion endpoint
- [ ] Delivery receipt issuance
- [ ] Session reassembly
- [ ] Initiator upload flow in Android app

### Phase 6 — UI & UX

- [ ] Recording screen with status indicators
- [ ] Peer mesh visualisation
- [ ] Settings (chunk duration, storage cap, role selection)
- [ ] Onboarding flow

### Phase 7 — Hardening

- [ ] Bluetooth transport fallback
- [ ] StrongBox key backing
- [ ] Battery optimisation audit
- [ ] Security review and penetration testing
- [ ] F-Droid / Play Store release preparation

---

## 11. Design Decisions & Trade-offs

| Decision | Rationale | Trade-off |
|----------|-----------|-----------|
| Per-chunk encryption instead of stream encryption | Enables partial delivery and independent verification at each relay | Slightly larger overhead per chunk (IV + tag = 28 bytes) |
| Android Keystore for DEK storage | Hardware-backed key protection; keys never in userspace | Keys non-exportable; requires careful key handle serialization |
| Wi-Fi Aware as primary transport | Highest bandwidth; no infrastructure needed; supports NAN clustering | Requires API 26+; not all devices have hardware NAN support |
| Protobuf (future) / JSON (v1) for wire protocol | JSON is human-readable for debugging in v1; migrate to Protobuf for performance in v2 | JSON is larger; Protobuf adds build complexity |
| Room + encrypted files (separate) | Metadata is queryable; large binary payloads stay out of SQLite | Two sources of truth (must keep in sync) |
| Fixed chunk duration (default 30 s) | Simpler implementation; bounded chunk size | Not optimal for variable-bandwidth networks; make configurable |
| HMAC receipts (symmetric) | Simple; no PKI required for receipt exchange | Requires shared secret establishment first |
| Pseudonymous device IDs with 24 h rotation | Protects operator identity from passive observers | Rotation breaks persistent peer trust; requires re-handshake |

---

## 12. Open Questions

1. **Key escrow**: Should a server-side key escrow exist for disaster recovery? (Current answer: No — privacy over recoverability.)
2. **Bluetooth LE vs Classic**: BLE advertisement for discovery + Classic for bulk transfer, or BLE only? TBD based on bandwidth testing.
3. **Chunk size**: 30-second default with 720p30 video ≈ 15 MB per chunk over AVC. Is this acceptable for Bluetooth transfer? Likely need smaller chunks (5–10 s) when BT is the only transport.
4. **Server authentication**: Mutual TLS or bearer token for Initiator→Server? ****** simpler for v1.
5. **Multi-hop routing**: In v1 we use opportunistic flooding (forward to every peer). A future version may implement DTN routing (e.g., Epidemic or PROPHET) for efficiency.
6. **Offline map of peers**: Should the relay maintain a bloom filter of "chunks already seen" to avoid re-forwarding? Yes, implement in Phase 4.
