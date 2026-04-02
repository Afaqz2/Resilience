# Resilience: Civilian Emergency Companion - Product Requirements Document (PRD)

## Overview
An offline-first, civilian-safety-focused Android application designed for resilience during conflict, blackouts, and telecom disruption.

---

## 1. Core Principles
1. **Privacy First**: Local-only by default. Sensitive data (medical, family locations) is encrypted on-device.
2. **Offline-as-Standard**: Every feature must provide value without an internet connection.
3. **Resilience**: Low battery consumption, high contrast UI, and robust against data corruption.
4. **Verified Content**: Survival guides must come from trusted, cited sources.

---

## 2. Updated User Stories
- **Blackout Scenario**: "As a civilian in a blackout, I need to access my water/food inventory and shelter guides without any internet."
- **Separation Scenario**: "As a parent, I need a pre-stored family reunification plan and offline map of meeting points if we are separated."
- **First Aid Scenario**: "As a first-responder (civilian), I need quick-access first-aid guides for bleeding and burns that work in a 5% battery situation."
- **Connectivity Loss**: "As a user during a communications outage, I need to compose and queue my 'I am safe' messages so they send automatically when signal returns."

---

## 3. Mandatory Feature Set (MVP v1)

### A. Quick-Action Dashboard
- **Emergency Buttons**: Large, high-contrast buttons for "Shelter", "Medical", "Fire", "SOS".
- **Status Indicators**: Battery %, Days of Water/Food remaining (dynamic calculation based on household size).
- **Crisis Mode**: Global toggle for monochrome, text-only, no-animation UI. (See [UI_BEHAVIOR.md](file:///Users/arslanhaider/Documents/Afaq%20n8n/Android%20Apps/SaveReach/docs/UI_BEHAVIOR.md)).

### B. Offline Playbooks (Survival Library)
- **Static Content**: Markdown/JSON bundles (First Aid, Shelter, Go-Bag, Water Purification).
- **Audio Guidance**: Text-to-Speech (TTS) for hands-free or low-visibility use.
- **Source Citations**: Clear labeling of "Last Verified" and "Source Organization" (e.g., WHO, Red Cross).

### C. Family Safety Vault (Encrypted)
- **Profiles**: Blood group, allergies, medications, emergency contacts.
- **Meeting Points**: GPS coordinates and plain-text directions for primary/secondary/tertiary meet-up spots.
- **Check-in Log**: Historical record of safety pings.

### D. Resource Tracker & Forecasting
- **Inventory**: Log water (liters), food (days), battery, fuel.
- **Forecasting**: Calculate "Days Remaining" based on household size and daily burn rate.
- **Expirations**: Tracking for food/medication expiry dates.

### E. Communication & Signaling
- **Visual/Audio SOS**: Flashlight strobe, loud whistle.
- **SMS Queue**: "I am Safe" templates (SMS) for automated retry when signal is acquired.

### F. Life-Line Maps (Offline)
- **MapLibre Integration**: Vector maps (.mbtiles) for city/regional levels.
- **POI Overlay**: Hospitals, water points, shelters.
- **Coordinate Sharing**: One-tap SMS of current Lat/Long.

---

## 4. Offline Behavior Matrix

| Feature | Fully Offline | Partially Offline | Online-Enhanced Only |
| :--- | :---: | :---: | :--- |
| **Playbooks** | Yes | - | Video assets (skipped/local fallback) |
| **Family Vault** | Yes | - | Sync with household (optional) |
| **Inventory** | Yes | - | Burn-rate data from local history |
| **SMS Templates** | Yes | Yes (Send) | Requires signal to transmit |
| **Maps** | - | Yes | Needs offline pack download first |
| **Alerts** | - | Yes | Syncs when signal exists; caches last data |
| **Crisis Mode** | Yes | - | Disables non-essential live-sync |

---

## 5. Permissions Policy

| Permission | Purpose | Optional? | Fallback if Denied |
| :--- | :--- | :---: | :--- |
| **Location** | POI distance, meeting points, SOS. | Yes | Manual coordinate entry / Map browsing. |
| **SMS** | Automated templates, 'I am Safe' queue. | Yes | Copy to clipboard manually. |
| **Notifications** | Emergency alerts and sync status. | Yes | App must be open to view alerts. |
| **Bluetooth** | Future local discovery (v2). | Yes | Feature remains hidden. |
| **Camera** | Flashlight strobe / QR scanning. | Yes | Screen-based visual signaling / Manual entry. |

---

## 6. Threat Model & Data Retention
*   **App Lock**: Biometric or PIN required for Family Vault (Optional/Opt-in).
*   **Data Minimization**: No location history stored beyond last known for "I am safe" pings.
*   **Retention**: Check-in logs expire after 30 days (default).
*   **Wipe Option**: "Delete All Data" button for extreme situations.
*   **Export**: Local encrypted file export for transfer between devices.
*   **Analytics**: Analytics and crash logs are disabled when in Crisis/Low-Battery mode.

---

## 7. Content Governance
*   **Sourcing**: Guides are sourced from WHO, Ready.gov, and Red Cross.
*   **Review**: Technical review of content markdown every 6 months.
*   **integrity**: Every playbook bundle is digitally signed by the maintainer.
*   **Localization**: Crowdsourced and vetted translations for Urdu, Arabic, etc.
*   **Citations**: Every guide includes a source metadata panel.

---

## 8. Technical Architecture
- **UI Architecture**: Jetpack Compose (Modern, state-driven).
- **Local Database**: Room with SQLCipher for encrypted storage.
- **Preferences**: DataStore for UI settings/Crisis Mode.
- **Background Tasks**: WorkManager for syncing alerts and background retries.
- **Encryption**: Android Keystore for key management.

---

## 9. Implementation Roadmap

### MVP (First Release)
- **Sprint 1**: Base Architecture + Room Schema + Dashboard UI.
- **Sprint 2**: Survival Playbooks (JSON loading) + Family Vault (Encryption).
- **Sprint 3**: Resource Tracker + SMS Templates/Queue UI.
- **Sprint 4**: MapLibre Integration (Offline Region Support).
- **Sprint 5**: Crisis/Battery-Extreme Mode + Localization.

### Future (v2/v3)
-   **Mesh Networking (D2D)**: Bluetooth/Wi-Fi Direct pings.
-   **Psychological First Aid**: Expanded trauma guidance.
-   **Safe Power Habit Guides**: Advanced battery preservation modules.
-   **"Dead Man's Switch"**: Opt-in inactivity alerts.
