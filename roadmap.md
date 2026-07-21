# MeshStream Roadmap

This roadmap translates the product vision in `idea.md` into an executable implementation plan for the initial build phase of MeshStream.

## Guiding Principles

- Follow the architecture constraints in `idea.md`: Kotlin-first, modular, offline-first, secure-by-default, and battery/storage conscious.
- Keep work split into small, reviewable tasks that can be implemented and validated incrementally.
- Update `status.md` after every completed task and before beginning the next one.

## Milestone 1 — Core Engine and Storage Pipeline

Goal: establish the fundamental recording and storage foundation for client devices.

### Tasks
- 1.1 Define the module boundaries and domain interfaces for recording, storage, and chunk lifecycle management.
- 1.2 Create the Android project skeleton with the planned modules: `app`, `core`, `feature/recorder`, `feature/storage`, and supporting feature modules.
- 1.3 Implement the recording service contract and storage directory strategy for master recordings and watch-folder chunks.
- 1.4 Implement the chunking pipeline with deterministic naming and basic lifecycle tracking.
- 1.5 Add storage guardrails, including the 2.2x footprint calculator and low-space stop conditions.
- 1.6 Validate continuous recording and background chunking on a physical device or emulator.

## Milestone 2 — Local Mesh and Physical Sweeper Transport

Goal: enable offline chunk movement between nearby devices and relay nodes.

### Tasks
- 2.1 Define transport contracts for chunk discovery, transfer, retry, and acknowledgements.
- 2.2 Implement local peer discovery and session bootstrap for nearby MeshStream devices.
- 2.3 Implement chunk queue state transitions: `PENDING`, `IN_TRANSIT`, `DELIVERED`, and `FAILED`.
- 2.4 Implement resumable transfer handling for interrupted chunk uploads.
- 2.5 Add relay-mode support for store-and-forward behavior when a device moves between disconnected and connected regions.
- 2.6 Validate mesh transfers in a controlled local test environment.

## Milestone 3 — Initiator Ingestion and Reassembly

Goal: provide a working ingestion endpoint and basic reassembly workflow for initiator devices.

### Tasks
- 3.1 Implement the embedded initiator server and session bootstrap payload generation.
- 3.2 Accept incoming chunks, verify them, and emit authenticated acknowledgements.
- 3.3 Reassemble arriving chunks into a playable output stream using the naming convention.
- 3.4 Add a basic dashboard for transfer progress, missing chunks, and completion status.
- 3.5 Validate end-to-end transfer from client to initiator.

## Milestone 4 — Security, Privacy, and Field Hardening

Goal: harden the app for adversarial and resource-constrained field conditions.

### Tasks
- 4.1 Implement chunk encryption using the initiator public key.
- 4.2 Strip metadata from outgoing chunks before transit.
- 4.3 Generate per-session anonymous node identifiers.
- 4.4 Add panic-mode cleanup and temporary app lock behavior.
- 4.5 Perform resilience and stress testing under simulated low-storage and poor-connectivity conditions.

## Execution Order

Work should proceed in order from Milestone 1 to Milestone 4. Each task should be completed and recorded in `status.md` before the next task begins.
