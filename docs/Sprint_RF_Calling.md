# Sprint Plan — Offline RF Mesh Calling & AI Helper Integration

> User story: *"As a civilian, I want to use my phone like a walkie-talkie on specific 'frequencies' to reach others nearby without internet, and be able to ask the AI Helper for advice using my voice over a dedicated channel."*

## Goals
- **Walkie-Talkie UI**: Implement a PTT (Push-To-Talk) UI where users can tune into different analog-style "Frequencies" (Channels).
- **Deep Search & Discovery**: Scan for other active peers in the vicinity and display a list of users currently listening on different frequencies.
- **RF Audio Transmission**: Send offline voice messages to specific frequencies using Wi-Fi Aware (NAN) / Wi-Fi Direct and BLE mesh networking.
- **AI Helper Integration**: A dedicated AI Helper button or "AI Frequency" where the user can send PTT audio. The app will process the offline audio via a local Speech-to-Text model, consult the local RAG database, and respond via Text-to-Speech over the "radio".

## Technical Approach
1. **Mesh Discovery (`WifiAwareManager` & BLE)**: Use Wi-Fi Aware (and BLE as fallback) to broadcast the user's presence and the "frequency" they are tuned to. This allows the app to do a "Deep Search" that sniffs local broadcast beacons to populate a map/list of nearby users and their current channels without establishing a heavy socket connection.
2. **Audio Streaming (`AudioRecord` & `AudioTrack`)**: Capture raw microphone audio, compress it (e.g., using Opus codec for very low bandwidth), and send it as UDP datagrams over the Mesh network to peers on the same frequency.
3. **AI Speech Processing**: Use an offline STT library (like Vosk) to convert the PTT voice note into text, feed it into our existing offline RAG, and play the response back using `TextToSpeech`.

## New Files to Create

| File | Purpose |
|------|---------|
| `data/mesh/MeshDiscoveryManager.kt` | Wi-Fi Aware/BLE advertising and discovery. Populates user frequency lists. |
| `data/audio/WalkieTalkieManager.kt` | Handles `AudioRecord`, Opus encoding/decoding, and PTT transmission. |
| `data/ai/OfflineVoiceAgent.kt` | Speech-to-Text -> LLM/RAG -> Text-to-Speech logic for AI. |
| `ui/radio/RadioFrequencyScreen.kt` | Main Walkie-Talkie UI with Tuner, PTT, AI button, and Search button. |
| `ui/radio/FrequencyScannerSheet.kt` | Bottom sheet displaying nearby users by frequency. |

## Verification Plan

### Automated Tests
- Unit test `MeshDiscoveryManager` by mocking discovery callbacks and verifying user map data.
- Unit test `OfflineVoiceAgent` with mocked audio input to ensure LLM interactions and TTS output behave expectedly.

### Manual Verification
- **Device-to-Device PTT**: Deploy to two physical devices tuned to the same frequency. Hold PTT on A and verify audio output on B.
- **Deep Search**: Tune Device B to a different frequency. Trigger "Deep Search" on Device A and verify Device B appears in the list.
- **AI Channel**: Switch to AI Channel, press PTT, ask a question offline, and ensure the voice synthesized answer plays correctly.
