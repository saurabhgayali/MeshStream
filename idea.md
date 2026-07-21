
# IDEA.md — MeshStream Software Specification & Product Architecture

## 1. Product Vision & Concept

### The Problem
During political protests, civil unrest, and humanitarian crises, network operators or authorities often deploy localized Radio Frequency (RF) jammers, cell-site simulators, or internet shut-downs. This creates a total communications blackout inside the affected area (**Region J**). 

Field journalists and witnesses facing these blackouts encounter severe operational hurdles:
* Standard live-streaming and cloud uploads fail instantly due to lost cellular signals.
* Stopping long-form video capture to manually slice, compress, or move files disrupts continuous coverage and burns critical time.
* Recording high-bitrate video (1080p/4K) fills phone storage within tens of minutes if files cannot be offloaded.

### The Solution
**MeshStream** is a unified, Android-native mobile application designed to capture, process, and transport video out of radio-denied zones without relying on traditional internet or cellular infrastructure.

MeshStream decouples video recording from network transmission:
1. **Continuous Capture:** The journalist records continuously without stopping or losing video frames.
2. **Background Slicing:** The app automatically slices the video into lightweight, encrypted chunks without re-encoding, preserving master quality while using minimal battery and CPU.
3. **Multi-Modal Transport:** Chunks hop wirelessly across local peer-to-peer Wi-Fi/Bluetooth mesh networks or are carried physically by people walking outside the jammed zone ("data sweepers").
4. **Verified Purging:** Once a chunk reaches an ingestion hub in **Region O** (outside the jammed zone) and returns a cryptographic receipt, the app purges that chunk from local storage—reclaiming disk space while leaving the original continuous master file untouched.

---

## 2. High-Level System Architecture

The application operates across a three-zone physical model using a **single unified APK** capable of operating in three distinct runtime modes:


```

┌──────────────────────────────────────────────────────────────────────────────────┐
│                               MESHSTREAM UNIFIED APK                             │
└────────────────────────────────────────┬─────────────────────────────────────────┘
│
┌───────────────────────────────┼───────────────────────────────┐
▼                               ▼                               ▼
[ CLIENT MODE ]                 [ RELAY MODE ]                 [ INITIATOR MODE ]
• Active in Region J            • Active in Region E/J         • Active in Region O
• Custom Camera Engine          • Store-and-Forward Queue      • Embedded Ingestion Server
• Continuous Recording          • Ad-Hoc Mesh Listening        • Session QR / Key Generator
• Watch-Folder Chunker          • Physical Data Sweeper        • Master Reassembly Hub

```

### The Three Operational Zones

1. **Region J (Jammed Zone / Clients):** High RF interference, no cellular/internet access. Devices run **Client Mode** to record video, write to local master storage, slice chunks into the watch folder, and broadcast chunks to any reachable peers.
2. **Region E (Edge Relay Zone / Relays):** Perimeter boundary where jamming fades. Devices run **Relay Mode** to pick up chunks via short-range Wi-Fi/Bluetooth or store them on physical devices as users walk across the boundary.
3. **Region O (Outside Gateway Zone / Initiators):** Full cellular or fixed internet access. Devices run **Initiator Mode**, hosting an embedded HTTP ingestion server, receiving chunks from incoming relays/sweepers, reassembling the original master video, and streaming it to cloud platforms or newsrooms.

---

## 3. Core Functional Modules to Develop

To build MeshStream, the development team must build six core software modules:


```

┌──────────────────────────────────────────────────────────────────────────────────┐
│                             MODULE ARCHITECTURE                                  │
├───────────────────────┬─────────────────────────┬────────────────────────────────┤
│ 1. Camera & Storage   │ 2. Background Chunker   │ 3. Storage Calculator          │
│    Engine             │    Engine               │    & Purge Manager             │
├───────────────────────┼─────────────────────────┼────────────────────────────────┤
│ 4. P2P Mesh & Relay   │ 5. Initiator Ingestion  │ 6. End-to-End Security         │
│    Network Layer      │    Server               │    & Session Manager           │
└───────────────────────┴─────────────────────────┴────────────────────────────────┘

```

### Module 1: Custom Camera & Recording Engine
* **Purpose:** Capture continuous, high-reliability video without frame drops or system interrupts.
* **Requirements:**
  * Build on Android's native camera frameworks (`CameraX` / `MediaRecorder`).
  * Record directly to an app-isolated, non-deletable directory: `/Master_Recordings/`.
  * Support configurable resolutions (720p, 1080p, 4K) and frame rates (30/60 fps).
  * Run as a foreground service to prevent the OS from terminating recording when the screen turns off or the app is minimized.

### Module 2: Non-Re-encoding Background Chunker Engine
* **Purpose:** Slice active or finished master recordings into small, manageable video segments for network transit.
* **Requirements:**
  * Utilize native media demuxing/muxing tools (`MediaCodec` or `FFmpeg` in copy-codec mode `-c copy`).
  * Process files silently in the background without re-encoding video frames, taking only a few seconds per hour of video.
  * Output 10-to-15-second video chunks into a designated `/Watch_Folder/`.
  * Enforce deterministic, traceable naming conventions for every chunk:
    $$\texttt{NodeID\_TotalParts\_PartNumber\_Timestamp.mp4}$$

### Module 3: Storage Calculator & Purge Manager
* **Purpose:** Prevent device storage exhaustion and manage file lifecycles safely.
* **Requirements:**
  * **Pre-Recording Storage Footprint Calculator:** Query available device storage ($\text{Storage}_{\text{free}}$) and camera bitrate ($\text{Bitrate}_{\text{video}}$) to compute remaining time before recording starts:
    $$\text{Max Duration} = \frac{\text{Storage}_{\text{free}}}{2.2 \times \text{Bitrate}_{\text{video}}}$$
    *(Uses a $2.2\times$ safety multiplier to account for master file + active chunks + system buffer).*
  * **Real-Time Storage Safeguard:** Monitor free disk space during active recording. Automatically stop recording gracefully if free space falls below a critical threshold (e.g., $<500\text{ MB}$).
  * **Chunk Purge Engine:** Upon receiving an authenticated Server Receipt (ACK) from Region O, permanently delete **only the specific delivered chunk** from `/Watch_Folder/`. The continuous master file in `/Master_Recordings/` must remain completely untouched.

### Module 4: P2P Mesh & Physical Sweeper Network Layer
* **Purpose:** Transport chunks across devices when cellular internet is unavailable.
* **Requirements:**
  * Integrate low-level Android peer-to-peer frameworks (Wi-Fi Direct, Wi-Fi Aware / NAN, Bluetooth Low Energy).
  * **Digital Mesh Routing:** Automatically discover nearby MeshStream devices and negotiate chunk transfers based on signal quality and progress towards Region E.
  * **Physical Sweeper (Store-and-Forward) Support:** Store encrypted chunks on relay devices. Automatically trigger bulk uploads when a relay device enters Region E/O and detects an active Initiator server.
  * Implement resumable chunk transfers so interrupted connections don't waste battery re-sending partial files.

### Module 5: Initiator Ingestion Server & Reassembly Engine
* **Purpose:** Receive, verify, reassemble, and preview incoming video chunks outside the jammed area.
* **Requirements:**
  * Embed an in-app HTTP/REST ingestion server (e.g., `Ktor Embedded` or `NanoHTTPD`) running on the Initiator device.
  * Generate a session initialization payload (QR code containing IP, Port, Session Keys, and Network Credentials).
  * Parse arriving chunks, verify SHA-256 signatures, and immediately issue cryptographic ACK receipts to the sender.
  * Reassemble out-of-order chunks into a playable video stream using the file naming schema.
  * Provide a live dashboard UI showing received chunks, missing segments, and total transfer progress across active journalists.

### Module 6: End-to-End Security & Session Manager
* **Purpose:** Protect field reporters, prevent data tampering, and ensure privacy in hostile environments.
* **Requirements:**
  * **Public-Key Encryption:** Encrypt chunks in `/Watch_Folder/` using the Region O Initiator's public key. Intermediate relay nodes and physical sweepers cannot view the video content carried on their phones.
  * **Metadata Stripping:** Strip EXIF data, device serial numbers, and GPS tags from chunks prior to transit.
  * **Session-Based Anonymity:** Generate randomized, non-traceable `NodeID` strings per app session to prevent tracking specific journalists across events.
  * **Panic Action:** Provide a quick-action gesture or toggle to immediately wipe `/Watch_Folder/` and lock the application interface during physical stop-and-frisk situations.

---

## 4. User Experience & Application Workflows


```

┌──────────────────────────────────────────────────────────────────────────────────┐
│                            MAIN APP WORKFLOWS                                    │
└────────────────────────────────────────┬─────────────────────────────────────────┘
│
┌───────────────────────────────┼───────────────────────────────┐
▼                               ▼                               ▼
[ WORKFLOW A ]                  [ WORKFLOW B ]                  [ WORKFLOW C ]
Field Journalist                Data Sweeper / Relay            Initiator Hub
(Capture & Offload)             (Pass-Through Carrying)         (Receive & Reassemble)

```

### Workflow A: Field Journalist (Region J)
1. **Pairing:** Scans the Initiator's QR code before entering the jammed area to exchange encryption keys and session tokens.
2. **Pre-Check:** Opens the camera interface; views calculated max recording duration based on available storage.
3. **Capture:** Presses record. Continuous master file saves to `/Master_Recordings/`.
4. **Automated Background Offload:** Chunker slices video into `/Watch_Folder/`. The network layer silently pushes chunks to nearby relays or sweepers.
5. **Storage Cleanup:** As ACK receipts trickle back from Region O, chunks disappear from `/Watch_Folder/`, continuously freeing storage for more recording.

### Workflow B: Data Sweeper / Relay (Region E / J)
1. **Activation:** User selects *"Act as Mesh Relay"* on the home screen and sets a storage limit slider (e.g., *"Allocate 5 GB for relay queue"*).
2. **Ingestion:** Phone receives encrypted chunks silently from nearby journalists via Wi-Fi Direct.
3. **Transport:** User physically walks or drives towards the perimeter of the crowd (Region E).
4. **Auto-Dump:** Upon reaching Region O connection range, the app automatically dumps the queued chunks to the Initiator server, receives ACKs, and clears its relay storage.

### Workflow C: Initiator Hub (Region O)
1. **Initialization:** User selects *"Start Initiator Server"*, binding the app to an active cellular data connection or fixed Wi-Fi network.
2. **Broadcasting:** App displays the Session QR code and broadcasts a localized onboarding network.
3. **Ingestion & Visual Feedback:** Displays a dashboard showing incoming chunk streams, active sweepers, and reassembly progress bars per journalist.
4. **Export:** Streams or exports completed master videos to newsrooms, cloud drives, or broadcast services.

---

## 5. Milestone Roadmap


```

MILESTONE 1           MILESTONE 2           MILESTONE 3           MILESTONE 4
┌─────────────┐       ┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│ Core Engine │ ────> │ Local Mesh  │ ────> │  Initiator  │ ────> │ Security &  │
│ & Storage   │       │ & Sweeper   │       │ Ingestion   │       │ Field Hard- │
│ Validation  │       │ Transport   │       │ & UI Dashboard      │ ening       │
└─────────────┘       └─────────────┘       └─────────────┘       └─────────────┘

```

### Milestone 1: Core Engine & Storage Pipeline
* Develop custom camera interface with resolution and frame rate controls.
* Implement foreground service execution for background persistence.
* Build non-re-encoding video chunker (`-c copy`) writing to `/Watch_Folder/`.
* Implement the $2.2\times$ pre-recording storage calculator and low-space auto-stop triggers.
* Validate continuous recording + simultaneous background chunking on physical Android devices.

### Milestone 2: Local Mesh & Physical Sweeper Network Layer
* Implement Android Wi-Fi Direct and Wi-Fi Aware peer discovery protocols.
* Build the store-and-forward queue manager for handling chunk state transitions (`PENDING`, `IN_TRANSIT`, `DELIVERED`).
* Implement chunk chunking / resumable file transfer protocol over short-range sockets.
* Validate physical data muling (carrying a device from a disconnected state to a connected state to trigger auto-upload).

### Milestone 3: Initiator Ingestion Server & Dashboard
* Embed local HTTP ingestion server framework inside the Android application.
* Build session bootstrap mechanism (QR code generator and parser).
* Develop chunk verification, hash checking, and ACK receipt generation logic.
* Build server dashboard UI visualizing out-of-order chunk arrivals, reassembly progress, and video playback previews.
* Implement automated chunk purge trigger upon ACK confirmation.

### Milestone 4: Security, Anonymity & Field Hardening
* Implement public-key encryption for all chunks written to `/Watch_Folder/`.
* Integrate EXIF/metadata stripping engine for outgoing chunk payloads.
* Add dynamic session `NodeID` generation for reporter anonymity.
* Build panic wipe protocol (rapid deletion of `/Watch_Folder/` and temporary app lock).
* Perform field stress testing under simulated RF interference and high-density peer environments.

