# MeshStream Implementation Status

## Current Phase

Milestone 1 implementation has begun. The repository now contains a runnable Kotlin-based project skeleton for the core domain and feature modules.

## Status Summary

- [x] Review repository goals and architecture constraints from `README.md` and `idea.md`.
- [x] Create a milestone-based implementation roadmap in `roadmap.md`.
- [x] Start Milestone 1 tasks.
- [x] Complete the initial Milestone 1 implementation scaffold.
- [ ] Complete Milestone 2 and move to Milestone 3.
- [ ] Complete Milestone 3 and move to Milestone 4.

## Milestone Tracking

### Milestone 1 — Core Engine and Storage Pipeline
- [x] 1.1 Define module boundaries and domain interfaces.
- [x] 1.2 Create the initial project skeleton with core and feature modules.
- [x] 1.3 Implement the recording service contract and storage strategy scaffolding.
- [x] 1.4 Implement the initial chunk lifecycle models and repositories.
- [x] 1.5 Add storage guardrails and low-space stop conditions.
- [x] 1.6 Validate the initial project skeleton by compiling and running the entrypoint.

### Milestone 2 — Local Mesh and Physical Sweeper Transport
- [x] 2.1 Define transport contracts.
- [ ] 2.2 Implement peer discovery and session bootstrap.
- [ ] 2.3 Implement chunk queue state handling.
- [ ] 2.4 Implement resumable transfer handling.
- [ ] 2.5 Add relay-mode support.
- [ ] 2.6 Validate mesh transfers.

### Milestone 3 — Initiator Ingestion and Reassembly
- [ ] 3.1 Implement embedded initiator server.
- [ ] 3.2 Accept chunks and emit acknowledgements.
- [ ] 3.3 Reassemble incoming chunks.
- [ ] 3.4 Add transfer dashboard.
- [ ] 3.5 Validate end-to-end flow.

### Milestone 4 — Security, Privacy, and Field Hardening
- [ ] 4.1 Implement chunk encryption.
- [ ] 4.2 Strip outgoing metadata.
- [ ] 4.3 Generate anonymous node identifiers.
- [ ] 4.4 Add panic mode cleanup and app lock.
- [ ] 4.5 Perform resilience and stress testing.

## Notes

- The initial scaffold is implemented as a Kotlin/JVM application entrypoint because the provided environment does not have network access to fetch the Android Gradle plugin. The Android-specific app sources remain scaffolded for future conversion to Android once the plugin is available.
- The next implementation step is Milestone 2, Task 2.2.
