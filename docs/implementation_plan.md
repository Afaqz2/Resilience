# Resilience: Implementation Plan (Final Tech Stack)

## Goal Description
Initialize the Resilience Android project using the officially recommended tech stack to ensure offline-first reliability, security, and battery efficiency.

## Tech Stack (Confirmed)
-   **Language**: Kotlin
-   **UI Layer**: Jetpack Compose (MVVM + UDF)
-   **DI**: Hilt (Jetpack Integrations)
-   **Async**: Coroutines + Flow
-   **Data Storage**:
    -   **Room + SQLCipher**: Encrypted structured data. (SQLCipher for DB encryption).
    -   **DataStore**: Key-value settings (Crisis Mode, Onboarding).
    -   **EncryptedSharedPreferences**: Small secrets/keys. (Keystore backed).
-   **Background**: WorkManager (Sync, Retries, Cleanup).
-   **Networking**: Retrofit + OkHttp (Aggressive Caching).
-   **Maps**: MapLibre SDK (OfflineRegion Support).
-   **Security**: Android Keystore for key management.

## Project Structure
-   `ui/`: Compose screens, ViewModels, and State management.
-   `data/`: Room entities, DAOs, Repositories, and Retrofit services.
-   `domain/`: Use cases and domain models.
-   `di/`: Hilt Modules.
-   `workers/`: WorkManager implementation for sync/SMS.
-   `maps/`: MapLibre integration and OfflineManager logic.
-   `security/`: Keystore and encryption utilities.

---

## Proposed Changes

### Phase 1: Project Initialization (Sprint 1)
-   Initialize Android Gradle project with Version Catalog (`libs.versions.toml`).
-   Apply Hilt, Room, and Compose plugins.
-   Setup base package structure (`com.resilience.app`).
-   **Global State Controller**: Handling Normal/Crisis/Extreme mode transitions via DataStore.
-   **Dashboard Shell**: Compose-based UI for the 6 core action buttons.

---

## Sprint 1 Checklist ✅
- `[x]` Setup Gradle Version Catalog (Hilt, Room, Compose, MapLibre, SQLCipher).
- `[x]` Create Base Application class with `@HiltAndroidApp`.
- `[x]` Implement `MainActivity` + Navigation (NavGraph).
- `[x]` Implement `CrisisModeDataStore`.
- `[x]` Create Dashboard (Home) with emergency buttons.

---

## Sprint 2 Checklist ✅ — Survival Playbooks + Family Vault

### Survival Playbooks (Offline Library)
- `[x]` `PlaybookEntity` — Room entity with category, title, markdown body, source, accent color.
- `[x]` `PlaybookDao` — `getAllPlaybooks`, `getPlaybooksByCategory`, `getAllCategories`, `upsertAll`.
- `[x]` `playbooks.json` — 6 bundled guides: First Aid ×3, Shelter, Go-Bag, Water Purification.
- `[x]` `PlaybookRepository` — Seeds DB from JSON asset on first launch; exposes Flows.
- `[x]` `PlaybookViewModel` — Category filter, playbook selection, TTS state.
- `[x]` `PlaybookListScreen` — Filter chips, scrollable card list with accent colours.
- `[x]` `PlaybookDetailScreen` — Lightweight Markdown renderer + **Text-to-Speech** (hands-free mode).

### Family Safety Vault (Encrypted)
- `[x]` `FamilyMemberEntity` — Room entity: name, blood group, allergies, medications, emergency contact.
- `[x]` `MeetingPointEntity` — Room entity: GPS coordinates, plain-text directions, priority (Primary/Secondary/Tertiary).
- `[x]` `FamilyVaultDao` — Full CRUD for members and meeting points.
- `[x]` `FamilyVaultRepository` — Clean domain-level API over the DAO.
- `[x]` `FamilyVaultViewModel` — Add/edit/delete members & meeting points, sheet visibility state.
- `[x]` `FamilyVaultScreen` — Expandable member cards, meeting point cards, add/edit modal bottom sheets.

### Infrastructure
- `[x]` `ResilienceDatabase` — Room database with **SQLCipher** encryption (SupportFactory).
- `[x]` `DatabaseModule` — Hilt module providing DB singleton + DAOs.
- `[x]` `Routes` + `ResilienceNavGraph` — Full Compose Navigation graph (Dashboard → Playbooks → Detail, Family Vault).
- `[x]` Dashboard nav grid wired (Playbooks, Family Plan buttons now navigate).

---

## Sprint 3 Plan — Offline City Map Download

> User story: *"As a civilian, I want to download my city map so I can open it offline without any internet connection."*

### Goals
- Allow the user to select a region (city/area) and download it as a vector tile pack.
- Downloaded map opens instantly with no internet — fully offline.
- Show download progress, pause/resume, and delete stored packs.

### New Files to Create
| File | Purpose |
|------|---------|
| `maps/OfflineMapManager.kt` | Wraps MapLibre `OfflineManager` — download, list, delete regions |
| `data/db/entity/OfflineRegionEntity.kt` | Tracks downloaded regions in Room |
| `data/db/dao/OfflineRegionDao.kt` | CRUD for stored region metadata |
| `data/repository/OfflineMapRepository.kt` | Coordinates Room + MapLibre manager |
| `ui/maps/MapScreen.kt` | MapLibre composable view + POI overlay + coordinate share |
| `ui/maps/OfflineMapDownloadSheet.kt` | Download UI: region selector, progress bar, manage packs |
| `ui/maps/MapViewModel.kt` | Download state, progress %, manage list |
| `workers/MapTileCleanupWorker.kt` | WorkManager worker to prune stale tile packs |

### Checklist ✅
- `[x]` Add MapLibre `AndroidView` wrapper composable.
- `[x]` Implement `OfflineManager.downloadRegion()` with bounding-box from city selector.
- `[x]` Persist region metadata (name, bbox, size, status) in Room.
- `[x]` Show download progress in a `LinearProgressIndicator` bottom sheet.
- `[x]` Support pause / resume / delete downloaded packs.
- `[x]` POI overlay: hospitals, water sources, shelters (from bundled GeoJSON).
- `[x]` One-tap "Share my location" — copies current Lat/Long as SMS-ready text.
- `[x]` Wire "Maps" nav grid button to `MapScreen`.
- `[x]` Add `WorkManager` cleanup job to prune packs older than 90 days.

---

## Sprint 4 Plan — Offline RF Mesh Calling & AI Helper Integration

> User story: *"As a civilian, I want to use my phone like a walkie-talkie on specific 'frequencies' to reach others nearby without internet, and be able to ask the AI Helper for advice using my voice over a dedicated channel."*

### Goals
- Implement a Walkie-Talkie (Push-To-Talk) UI where users can tune into different analog-style "Frequencies" (Channels).
- **Deep Search & Discovery**: Scan for other active peers in the vicinity and display a list of users currently listening on different frequencies.
- **RF Audio Transmission**: Send offline voice messages to specific frequencies using Wi-Fi Aware (NAN) / Wi-Fi Direct and BLE mesh networking.
- **AI Helper Integration**: A dedicated AI Helper button or "AI Frequency" where the user can send PTT audio. The app will process the offline audio via a local Speech-to-Text model, consult the local RAG database, and respond via Text-to-Speech over the "radio".

### Technical Approach
1. **Mesh Discovery (`WifiAwareManager` & BLE)**: Use Wi-Fi Aware (and BLE as fallback) to broadcast the user's presence and the "frequency" they are tuned to. This allows the app to do a "Deep Search" that sniffs local broadcast beacons to populate a map/list of nearby users and their current channels without establishing a heavy socket connection.
2. **Audio Streaming (`AudioRecord` & `AudioTrack`)**: Capture raw microphone audio, compress it (e.g., using Opus codec for very low bandwidth), and send it as UDP datagrams over the Mesh network to peers on the same frequency.
3. **AI Speech Processing**: Use an offline STT library (like Vosk) to convert the PTT voice note into text, feed it into our existing offline RAG, and play the response back using `TextToSpeech`.

### New Files to Create

#### [NEW] `data/mesh/MeshDiscoveryManager.kt`
Handles Wi-Fi Aware / BLE advertising and discovery. Sniffs for nearby devices to extract their currently tuned "Frequency" to populate the user discovery list.

#### [NEW] `data/audio/WalkieTalkieManager.kt`
Handles `AudioRecord` (mic input), Opus encoding/decoding, and `AudioTrack` (speaker output) for PTT audio.

#### [NEW] `data/ai/OfflineVoiceAgent.kt`
Coordinates the transition: Voice Profile -> Speech-to-Text (STT) -> RAG / LLM query -> Text-to-Speech (TTS) for the AI Helper.

#### [NEW] `ui/radio/RadioFrequencyScreen.kt`
The main walkie-talkie UI. Includes:
- A "Tuner" dial to select frequencies.
- A prominent PTT (Push-To-Talk) button.
- An "AI Helper" toggle/button for sending audio directly to the assistant.
- A "Deep Search" scanner button that reveals the `FrequencyScannerSheet`.

#### [NEW] `ui/radio/FrequencyScannerSheet.kt`
A bottom sheet that populates a list of nearby users and which frequencies they are currently active on, leveraging the localized `MeshDiscoveryManager`.

### Verification Plan

#### Automated Tests
- Unit test the `MeshDiscoveryManager` by mocking Wi-Fi Aware service discovery callbacks and ensuring the user list maps correctly.
- Test `OfflineVoiceAgent` flow by feeding it a mocked WAV file and verifying it invokes the LLM query function and returns a valid text response for TTS.

#### Manual Verification
- **Device-to-Device Mesh**: Load the app onto two physical Android devices (emulators cannot test Wi-Fi Aware/Direct properly). Tune both to "Channel 14", hold PTT on Device A, and verify audio plays on Device B.
- **Deep Search**: Have Device B tune to "Channel 22". On Device A, hit "Deep Search" and verify Device B shows up under the "Channel 22" list.
- **AI Helper**: Tune to AI channel or press the AI Helper Button, hold PTT, and ask "How do I purify water?". Verify the app transcribes the audio, fetches the playbook answer, and speaks it back.---

## Sprint 4 Plan — Metro 2033 UI Overhaul

> User story: *"As a survivor, I need an interface that feels tactical, robust, and functional in low-light environments, using an industrial aesthetic."*

### Goals
- Redo the application-wide theme adopting a Metro 2033 survival color palette (#1A1A1A background, #2A2A2A surface, #B85C38 rust primary, #4A5A3A olive secondary, #D4A017 amber accent, #B33030 destructive).
- Apply functional aesthetic fonts: IBM Plex Mono for Headings and Share Tech Mono for stencils and labels.
- Reskin UI components across the Dashboard, Family Vault, and other screens with sharp edge radius (4dp or 0.25rem), noise backgrounds, indicator glow lines, and tactical scan-lines.

### Checklist
- `[ ]` Update project with correct icon asset (`safereach-icon.png`) and register in `AndroidManifest.xml`.
- `[ ]` Replace `Type.kt` and `Color.kt` to define the rust/amber tactical design tokens.
- `[ ]` Create universal Compose components: tactical buttons, glow cards, and scanner-line modifiers.
- `[ ]` Rewrite `DashboardScreen.kt`, turning navigation items into military/industrial button grids.
- `[ ]` Refactor `FamilyVaultScreen.kt` for Contact "PING/CONTACT" states and rally point visualizations.
- `[ ]` Add new screens: Inventory (Threat center ambient level tracking) with styled indicator badges.
