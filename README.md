
# MeshStream



> **Resilient, Off-Grid Video Streaming & Relay Infrastructure for Field Journalism in Radio-Denied Environments.**

MeshStream is an Android-first mobile architecture designed to capture, slice, and transport high-fidelity journalistic video out of heavily jammed, network-restricted, or protest environments. 

By combining continuous local recording, intelligent background chunking, physical store-and-forward data muling, and peer-to-peer Wi-Fi mesh routing, MeshStream ensures that truth reaches the outside world even under total cellular and internet blackouts.

<img width="1640" height="664" alt="banner" src="https://github.com/user-attachments/assets/e020bf9a-3499-4a05-b50b-14e8b5b9cf46" />


---

## 🎯 Purpose & Mission

During civil unrest, protests, and crisis events, official authorities often deploy localized Radio Frequency (RF) jammers, cell-site simulators (Stingrays), or internet kill-switches to enforce information blackouts. 

Traditional live-streaming and cloud-uploading tools fail completely under these conditions:
1. **Network Denial:** Mobile phones lose all cellular data and internet connectivity inside the jammed zone (**Region J**).
2. **Battery & Capture Loss:** Camera apps fail to offload files, and interrupting long-form video capture to slice files manually degrades journalistic context.
3. **Storage Exhaustion:** Recording 4K or 1080p footage rapidly fills local device storage when files cannot be offloaded.

**MeshStream solves this by decoupling raw video recording from network transmission.** 

It turns every nearby participant's phone into an encrypted relay node or physical "data mule." Journalists record continuously without interruption, while MeshStream silently slices the video in the background, pushes micro-chunks across peer-to-peer hops, and auto-purges verified uploads to preserve storage.

---

## 📐 System Architecture: The Three-Zone Model

MeshStream operates across three distinct geographical and operational zones:


```

[ Region J: Jammed Area ]        [ Region E: Boundary Edge ]        [ Region O: Open Gateways ]
• High RF/Cellular Jamming        • Intermittent / Weak Signal       • Full Cellular & Fiber Internet
• Journalists & Protesters       • Physical Sweepers & Relays       • Initiator Ingestion Server
• Continuous Video Capture        • Wi-Fi Mesh Hop Boundary          • Final Cloud / Broadcast Uplink

```

* **Region J (Jammed Zone):** Full cellular blackout. Phones capture footage, retain the master video file locally, and background-slice non-re-encoded video chunks into a monitored upload queue.
* **Region E (Edge Relay Zone):** Boundary perimeter where jammers lose effectiveness. Devices operate as high-speed peer-to-peer relays or physical sweepers moving between J and O.
* **Region O (Outside Gateway):** Fully connected area outside the jammer radius. The **MeshStream Initiator** runs an embedded ingestion server here, receiving chunks from sweepers or mesh hops and reassembling the master video stream for global broadcast.

---

## 📡 Server Creation & Joining Strategies

MeshStream relies on zero-configuration, ad-hoc network orchestration. No external internet or centralized domain servers are required to initialize the local network.

### 1. Initiator Server Creation (Region O)
Before journalists enter the jammed zone—or from a stationary point located outside the blackout perimeter—a designated user launches MeshStream in **Initiator Mode**:

1. **Local Server Initialization:** The app starts a lightweight, embedded local HTTP/VPN ingestion server directly on the Android device.
2. **Access Point / Hotspot Broadcast:** The Initiator phone generates a high-speed, local Wi-Fi Access Point or broadcasts a localized **MeshStream SSID** (with or without direct internet tethering).
3. **Session Secret & QR Bootstrapping:** The Initiator app displays a session QR code containing:
   * Local Gateway IP Address & Port.
   * Session Public Key (for end-to-end chunk encryption).
   * Network Handshake Token.

### 2. Peer Joining & Network Discovery (Regions E & J)
Users operating in Region E or Region J join the MeshStream infrastructure using one of three joining strategies based on the current RF environment:

* **Strategy A: QR / NFC Direct Pairing (Fastest)**
  Field reporters scan the Initiator's or a Relay's QR code before entering the crowd. The app exchanges cryptographic keys and automatically remembers the Region O Ingestion Server parameters.
* **Strategy B: Zero-Conf Peer-to-Peer Discovery (Wi-Fi Direct / Wi-Fi Aware)**
  If Wi-Fi frequencies are open inside Region J, phones running MeshStream discover nearby peers automatically using Android's low-level peer-to-peer discovery protocols. They form an ad-hoc **Mobile Ad-Hoc Network (MANET)**, automatically routing chunks toward any node detected closer to Region E.
* **Strategy C: Captive Portal Onboarding**
  Edge nodes in Region E can broadcast an open local Wi-Fi network. When field participants connect their standard Wi-Fi to it, a captive portal serves the MeshStream APK download and immediate pairing credentials, allowing bystanders to instantly become physical sweepers.

---

## 🎞️ Media Processing & Storage Pipeline


```

[ Camera Engine ] ──> Master Video (100% Safe) ──> 📂 /Master_Recordings/ (Preserved)
│
└──> Background Chunker ──> 📂 /Watch_Folder/
│
├──> Transmitted via Mesh / Sweeper
└──> ACK Received ──> Auto-Deleted 🗑️

```

### 1. Continuous Capture & Background Slicing
Journalists record uninterrupted continuous footage (e.g., 1-hour takes). The app writes the original high-resolution master file to an isolated, non-deletable directory (`/Master_Recordings/`). 

Simultaneously, a background engine slices the file into lightweight, 10-to-15-second standalone video chunks inside `/Watch_Folder/` without re-encoding, preserving exact original quality while minimizing CPU and battery consumption.

### 2. Standardized Traceable Naming Schema
Every chunk produced across the network follows a deterministic, collision-free naming convention:

$$\texttt{NodeID\_TotalParts\_PartNumber\_Timestamp.mp4}$$

This allows the Region O Initiator server to verify, index, and preview arriving video chunks out of order or from multiple intermediate sweepers without needing a central database.

### 3. $2.2\times$ Storage Safety Calculator
Because MeshStream temporarily holds both the master file and generated chunks, the camera interface dynamically calculates available recording duration using a **$2.2\times$ safety footprint multiplier**:

$$\text{Max Duration} = \frac{\text{Free Disk Space}}{2.2 \times \text{Camera Bitrate}}$$

This prevents phone storage from unexpectedly running out in critical field situations.

### 4. Server-Verified Chunk Purging
When a chunk is successfully delivered to the Region O Initiator server via a physical sweeper or mesh hop, the server returns an authenticated **Acknowledgement (ACK)** packet. Upon receiving this ACK, the app **purges only that specific chunk from `/Watch_Folder/`**, instantly reclaiming disk space while keeping the master recording intact.

---

## 🚚 Transport Modes: Digital Mesh vs. Physical Sweeping

MeshStream dynamically adapts to the severity of local jamming:

| Condition | Primary Transport | Operating Mechanism |
| :--- | :--- | :--- |
| **Cellular Jammed / Wi-Fi Active** | **Multi-Hop Wi-Fi Mesh** | Chunks hop wirelessly from device to device across Region J $\rightarrow$ Region E $\rightarrow$ Region O Server. |
| **Full Spectrum RF Denial** | **Physical Data Sweepers** | Chunks are stored in `/Watch_Folder/`. Participants moving between Region J and E physically carry the files across the jammer boundary, where auto-upload triggers upon detecting Region O. |

---

## 🔒 Security & Field Protections

* **Zero-Knowledge Chunk Encryption:** Chunks written to the `/Watch_Folder/` are encrypted using the Region O Initiator's public key. Intermediate physical sweepers and relay nodes cannot view or tamper with footage carried on their devices.
* **Foreground Service Persistence:** Transmission and chunking run inside an Android Foreground Service with a persistent status notification, preventing the OS from terminating background mesh transfers.
* **EXIF & Metadata Stripping:** Chunks stripped of location and device telemetry prior to uplink to protect journalist anonymity in high-risk zones.

---

## 📋 Project Status

MeshStream is currently in **architectural specification**. Implementation specs, protocol specifications, and Android app builds will be hosted in this repository. 

*Contributions, code review, and feedback from field journalists, mesh networking enthusiasts, and security researchers are welcome.*

