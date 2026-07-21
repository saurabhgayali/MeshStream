# MeshStream

**Decentralized, offline-first video relay for resilient journalism in network-restricted environments.**

MeshStream is an open-source Android application that enables continuous video recording and propagation through peer-to-peer mesh networks when internet or cellular connectivity is unavailable or unreliable. Recorded video is automatically sliced into encrypted chunks and forwarded hop-by-hop until they reach an Initiator node with internet access.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Module Structure](#module-structure)
- [Getting Started](#getting-started)
- [Building the App](#building-the-app)
- [Running Tests](#running-tests)
- [Security Model](#security-model)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

MeshStream is designed for journalists, field reporters, and human-rights documentarians operating in environments where traditional communication infrastructure is absent, degraded, or actively blocked. The app turns a group of Android devices into a self-healing relay network: each device records, encrypts, forwards, and stores-and-carries video chunks until they reach a node with internet connectivity.

See [IDEA.md](IDEA.md) for full architecture details, data-flow diagrams, and the phased implementation plan.

---

## Key Features

| Feature | Description |
|---------|-------------|
| **Continuous recording** | Background `MediaRecorder` service that runs while the screen is off |
| **Automatic chunking** | Recordings are sliced into fixed-duration segments (default 30 s) |
| **Chunk encryption** | AES-256-GCM per-chunk encryption using Android Keystore-backed keys |
| **Mesh propagation** | Chunks are forwarded over Wi-Fi Direct, Wi-Fi Aware, or Bluetooth |
| **Store-and-forward** | Devices carry chunks on the move and relay when peers are discovered |
| **Verified delivery** | HMAC-signed delivery receipts trigger local deletion of forwarded chunks |
| **Storage reclamation** | Delivered chunks are securely deleted to free device storage |
| **Initiator upload** | Devices with internet upload verified chunks to a secure server |
| **Cross-platform server** | Kotlin/JVM server component for ingestion and reassembly |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        MeshStream Node                              │
│                                                                     │
│  ┌──────────┐   chunks   ┌──────────┐   encrypted   ┌───────────┐  │
│  │ Recorder │──────────▶│  Crypto  │──────────────▶│  Storage  │  │
│  │ Service  │            │  Module  │               │  (Room)   │  │
│  └──────────┘            └──────────┘               └─────┬─────┘  │
│                                                           │        │
│                                                    chunks │        │
│                                                           ▼        │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    Relay Manager                            │   │
│  │  ┌────────────┐  ┌─────────────┐  ┌──────────────────────┐ │   │
│  │  │ Wi-Fi      │  │  Wi-Fi      │  │     Bluetooth        │ │   │
│  │  │ Direct     │  │  Aware      │  │     (Classic/BLE)    │ │   │
│  │  └────────────┘  └─────────────┘  └──────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │ mesh hop
                              ▼
                    ┌─────────────────┐
                    │  Peer Node(s)   │
                    └────────┬────────┘
                             │ … n hops
                             ▼
                    ┌─────────────────┐      HTTPS      ┌──────────┐
                    │  Initiator Node │────────────────▶│  Server  │
                    │  (has internet) │                 │ (Kotlin) │
                    └─────────────────┘                 └──────────┘
```

### Node Roles

| Role | Description |
|------|-------------|
| **Recorder** | Records video and produces encrypted chunks |
| **Relay** | Receives chunks from peers, stores them, and forwards onward |
| **Initiator** | Has internet access; uploads received chunks to the server |

A single device may act in all three roles simultaneously.

---

## Module Structure

```
MeshStream/
├── app/                        # Android app shell (DI wiring, navigation, UI)
├── core/                       # Pure-Kotlin domain: models, repository interfaces, use cases
├── feature/
│   ├── recorder/               # Camera/microphone recording, chunk slicing
│   ├── crypto/                 # AES-GCM encryption, Android Keystore integration
│   ├── mesh/                   # Wi-Fi Direct / Wi-Fi Aware / Bluetooth transport
│   ├── storage/                # Room database + file storage for chunks
│   └── relay/                  # Store-and-forward relay orchestration
├── server/                     # Kotlin/JVM ingestion server (cross-platform)
└── gradle/
    └── libs.versions.toml      # Centralized version catalog
```

---

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2.x) or newer
- JDK 17+
- Android SDK 35 (API level 35)
- A device or emulator running Android 8.0+ (API 26+)

### Clone

```bash
git clone https://github.com/saurabhgayali/MeshStream.git
cd MeshStream
```

---

## Building the App

```bash
# Debug build
./gradlew :app:assembleDebug

# Release build (requires signing config)
./gradlew :app:assembleRelease

# Install on connected device
./gradlew :app:installDebug
```

---

## Running Tests

```bash
# Unit tests (all modules)
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Core module only
./gradlew :core:test
```

---

## Security Model

- **Per-chunk AES-256-GCM** encryption using keys generated in the Android Keystore.
- **Key hierarchy**: a master key per recording session; per-chunk derived keys via HKDF.
- **HMAC-SHA256 delivery receipts** prevent spoofed acknowledgements.
- **Chunk integrity**: each chunk carries a SHA-256 digest verified at every hop.
- **Secure deletion**: delivered chunks are overwritten before filesystem removal.
- Keys never leave the originating device's Keystore (public-key wrapping used for server delivery).

See [IDEA.md § Security](IDEA.md#security-model) for full details.

---

## Contributing

1. Read [IDEA.md](IDEA.md) to understand the full architecture before proposing changes.
2. Open an issue or comment on an existing one before starting work.
3. Keep pull requests small and focused — one logical change per PR.
4. Add or update tests for all changed code paths.
5. Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
6. Document non-obvious design decisions in code comments or PR descriptions.

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).
