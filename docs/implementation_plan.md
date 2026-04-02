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

