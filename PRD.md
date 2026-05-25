# Product Requirements Document (PRD)

# Hagrid!: Universal Ad Silencer · v1.0.0 Stable

## 1. Executive Summary
**Hagrid!** is an ultra-lightweight, system-level background utility for Android designed to automatically detect and silence audio advertisements across popular media streaming platforms like Spotify, YouTube Music, and Chrome. Unlike traditional ad-blockers, Hagrid! focuses specifically on the audio experience, performing surgical muting without modifying hardware volume or requiring root access.

## 2. Problem Statement
Users of free-tier media streaming services are frequently interrupted by loud, repetitive, and intrusive audio advertisements. These ads often have a different dynamic range than the music, leading to a jarring user experience. Existing solutions either require premium subscriptions, root access, or complex VPN-based filtering which can be unreliable or compromise privacy.

## 3. Goals & Objectives
- **Seamless Silence**: Automatically mute audio ads with sub-second latency.
- **Privacy First**: Process all data locally on the device with zero external telemetry.
- **Low Footprint**: Maintain minimal CPU and battery usage to ensure the service can run 24/7.
- **Zero Configuration**: Provide an "out-of-the-box" experience with minimal setup.
- **Transparency**: Use a clean, Material 3 dashboard to show users exactly what the engine is doing.

## 4. Target Audience
- **Free-tier Streamers**: Users of Spotify, YouTube Music, and YouTube.
- **Commuters & Workers**: Users who listen to music/podcasts hands-free and cannot manually mute ads.
- **Privacy Enthusiasts**: Users looking for secure, open-source utilities with zero tracking.

## 5. Core Features

### 5.1 User Features
- **Real-time Dashboard**: View system health, engine status, and active platform monitoring.
- **Mute Statistics**: Track "Ads Muted Today" against a daily goal with a visual progress bar.
- **Activity Visualizations**: Interactive daily and hourly charts showing muting patterns over time.
- **Theme Persistence**: Support for persistent Light/Dark mode transitions.
- **Guided Setup**: Streamlined onboarding to grant necessary system permissions.

### 5.2 Developer & Sandbox Features
- **Simulation Suite**: Manual triggers to "Simulate Ad" and "Simulate Track" for engine verification.
- **Engine Logs**: Real-time log viewer for tracking background service events.
- **Status Monitoring**: Detailed breakdown of "Monitoring" (Active/Paused) and "Engine" (Scanning/Idle) states.

## 6. Technical Architecture

### 6.1 Tech Stack
- **UI Framework**: Jetpack Compose (Declarative UI).
- **Core Engine**: `NotificationListenerService` (Metadata monitoring).
- **Audio Control**: `AudioManager` (Stream-level muting).
- **Architecture**: MVI-inspired StateFlow-driven UI.
- **Language**: Kotlin 2.0+ with Coroutines.

### 6.2 Data Flow
1. **Metadata Interception**: The `NotificationListenerService` captures notification updates from target packages.
2. **Heuristic Analysis**: Title, artist, and duration metadata are compared against ad-signatures and duration thresholds.
3. **Volume Overriding**: If a match is found, the current volume is cached, and `STREAM_MUSIC` is set to 0.
4. **State Restoration**: Upon detecting a standard track (valid duration/no ad-keywords), the cached volume is restored.
5. **UI Synchronization**: All events are emitted via `StateFlow` to update the dashboard charts and counters in real-time.

## 7. User Flows

### 7.1 Initial Setup & Onboarding
1. User opens the app and views the centered splash screen.
2. Navigates to the dashboard and identifies missing permissions (Notification Access / Battery Optimization).
3. Follows the guided prompts to the Android System Settings to grant access.
4. Returns to Hagrid! and toggles "TURN ON ENGINE".

### 7.2 Passive Operation & Monitoring
1. User opens a supported streaming app (e.g., Spotify) and begins playback.
2. An advertisement starts; Hagrid! detects the metadata change and mutes the system audio.
3. The "Ads Muted Today" counter increments, and the activity chart updates.
4. User can return to the dashboard at any time to verify the "Scanning" status and review recent logs.

## 8. Future Roadmap
- [ ] **v1.1**: Support for regional ad keywords and internationalization.
- [ ] **v1.2**: Data export feature to share or backup mute history (CSV/JSON).
- [ ] **v2.0**: AI-based audio fingerprinting for apps that suppress metadata.
- [ ] **v2.1**: Home screen widgets for quick status toggling.

## 9. Success Metrics
- **Mute Latency**: Mute action must occur within < 200ms of ad detection.
- **Battery Efficiency**: Background service must consume < 1% total battery per 24 hours.
- **Accuracy Rate**: > 98% detection rate for supported streaming platforms.
- **System Stability**: 99.9% uptime for the `START_STICKY` service layer.

---
*Build on May 2026 by Pawan Simha R · v1.0.0 Stable*
