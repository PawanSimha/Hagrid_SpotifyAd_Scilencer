# Hagrid! Walkthrough Guide

This guide will help you navigate the architecture, installation, and testing workflows of the Hagrid! Universal Ad Silencer platform.

---

## 1. Getting Started

### Prerequisites
* Android Studio Ladybug (or newer)
* Android SDK 36 (Android 16 Preview)
* A physical Android device or Emulator with Google Play Services (for testing media background states)

### Local Project Synchronization
If you need to sync or reset your working directory to the clean baseline branch:
```bash
git fetch origin
git reset --hard origin/main
git clean -fd
```

---

## 2. Project Architecture

Hagrid! follows a lightweight, service-oriented architecture designed for minimum battery impact and maximum reliability.

### Core Components
*   **`MainActivity.kt`**: The UI layer built with **Jetpack Compose**. It handles the dashboard, real-time stats visualization (using custom Canvas charts), and the Developer Sandbox.
*   **`NotificationMuterService.kt`**: The brain of the app. It extends `NotificationListenerService` to monitor media metadata and uses the `AudioManager` to perform surgical muting of advertisements.
*   **`HagridTheme.kt`**: A custom Material3 theme supporting dynamic Light/Dark mode switching with persistent user preferences.

---

## 3. Security & Permissions

Hagrid! requires two critical permissions to function correctly in the background:

1.  **Notification Access**: Required for the `NotificationListenerService` to read media metadata (Title, Artist, Duration) from apps like Spotify and YouTube.
2.  **Battery Optimization Bypass**: Required to prevent Android's "Doze Mode" from killing the background service during long music sessions.

The app provides a mandatory setup screen upon first launch to help users enable these safely.

---

## 4. Testing & Developer Sandbox

To test the muting engine without waiting for a real advertisement, we've included a **Developer Sandbox**:

1.  Tap the **Settings Icon** in the top-right corner of the header.
2.  In the bottom sheet, locate the **Testing Tools** section.
3.  **Simulate Ad**: Triggers a broadcast that the engine treats as a detected advertisement, instantly muting the media stream.
4.  **Simulate Track**: Triggers a broadcast that restores the volume, simulating the start of a regular song.

---

## 5. Licensing

This project is licensed under the **GNU General Public License v3.0**. 
*   See the `LICENSE` file in the root directory for the full legal text.
*   All source files contain the standard GPLv3 copyright header.

---

*Build on May 2026 by Pawan Simha R*
