# Resilience: UI Behavior Specification

## 1. Global Behavior

### Design Principles
*   **Fast to understand** (Low cognitive load in crisis).
*   **Works under stress** (Large targets).
*   **Works offline** (No loading spinners for local data).
*   **Low battery friendly** (Minimal brightness/animations).
*   **One-hand usable** (Bottom-focused controls).
*   **Readable** in sunlight and darkness.

### Core UI Rules
*   Large tap targets.
*   Minimal text on critical screens.
*   Strong visual hierarchy.
*   No hidden critical actions.
*   **Status Labeling**: Every screen must label content as "Offline Available," "Cached," or "Last Updated [Time]."
*   Avoid complex animations.
*   **Two-Tap Rule**: All essential actions reachable within **2 taps max**.

---

## 2. App States

The UI must dynamically adapt to different device/context states.

### A. Normal Mode
Used when conditions are stable.
*   Full icons + text.
*   Interactive Maps enabled.
*   Moderate color usage.
*   Recent alerts visible.

### B. Crisis Mode
User-triggered for extreme situations.
*   Monochrome or high-contrast near-monochrome UI.
*   Text-first layout (suppress nonessential graphics).
*   No decorative transitions/animations.
*   Larger buttons for visibility.
*   **Home Screen**: Only critical modules shown.
*   Suppress background sync unless essential.
*   Maps hidden behind an explicit tap-to-unlock.

### C. Extreme Battery Mode
Auto-triggered or suggested when battery is <10-15%.
*   Full-screen prompt to switch.
*   Disables animations, auto-refresh, and nonessential background tasks.
*   Rich maps disabled by default.
*   **Enabled Core**: Text-only playbooks, Family Vault, SMS templates, Flashlight/Whistle.
*   Default to dark theme (OLED efficiency).

### D. Offline Mode
Triggered when connectivity is lost.
*   Small, persistent banner: **"Offline — Using Saved Data."**
*   Hide live-sync features.
*   Keep local content 100% usable.
*   Map displays downloaded regions only.
*   **Action Queuing**: Network actions (like sending status pings) are queued locally instead of failing silently.

### E. Limited-Permission Mode
Triggered if user denies Location, SMS, or other permissions.
*   App remains 100% usable with manual alternatives.
*   Gentle inline prompts (no blocking popups).
*   **Substitutes**: Manual coordinates, manual message copying, local-only logs.

---

## 3. Screen-Specific Behaviors

### Dashboard (Home)
The primary entry point must require **no scrolling** for the top 6 emergency actions.
*   **Top Bar**: Battery %, Offline/Online status, "Days of Water Left," and a Mode Badge (Normal/Crisis/Battery).
*   **Middle (Emergency Grid)**: Shelter, Medical, Fire, Evacuate, Family, SOS.
*   **Bottom**: Recent Alerts with prominent timestamps.
*   **Interaction**: Single tap opens immediate action; long press for quick shortcuts.

### Shelter & Medical (Checklists)
*   Top line shows "What to do now."
*   3-7 immediate, actionable steps as cards.
*   Checkboxes for progress tracking.
*   Large "Read Aloud" button for hands-free guidance.
*   Danger warnings in distinct, bold styling.

### Family Vault
*   **List View**: Household member cards with "Critical Medical Marks."
*   **Profile View**: Sensitive details masked by default; tap to reveal.
*   **Meeting Points**: Text directions prominently displayed above coordinates (in case of map failure).

### Resource Tracker
*   Grouped categories (Water, Food, Fuel, Meds, Batteries).
*   Simple States: **Good, Watch, Low, Critical**. (Do not rely on color alone).
*   One-tap "Consume/Add" with large Plus/Minus buttons.

### SMS & Communication
*   One-tap templates: "I am safe," "I need help," etc.
*   **Local Queue**: Visual status of messages: Queued, Sent, or Failed.
*   Failure handling: Never silently discard; always offer "Copy Text" and "Retry."

### SOS Tools
*   One-tap from Home.
*   Tools: Flashlight Strobe, Loud Whistle, Medical Card, Location Share.
*   Confirmatory tap for loud/strobe actions to prevent accidental activation.
*   Screen stays awake while SOS is active.

---

## 4. Accessibility & Tone

### Accessibility
*   Dynamic Text Scaling (no clipping).
*   Full Right-to-Left (RTL) support (Urdu/Arabic).
*   Screen-reader labels on all icons.
*   Haptic confirmation for critical actions.
*   **Low Literacy Mode**: Optional mode using simpler language and more heavy icon usage.

### UI Tone
*   **Calm, direct, and trustworthy.**
*   Avoid alarmist or militarized design.
*   **Exact Labels to use**: "Available Offline," "Needs Internet," "Queued," "Last Updated," "Verified Source," "Unverified," "Low Supply," "Critical," "Battery Saver Active," "Crisis Mode Active."
