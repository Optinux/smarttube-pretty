# SmartTube Pretty

A UI-focused fork of [SmartTube](https://github.com/yuliskov/SmartTube) that updates the Android TV interface with a **Material 3 Expressive** visual style.

## What's Changed

- **Material 3 Expressive TV theming**: New surface/container color tokens, typography overrides, and updated dark/accent color schemes.
- **Redesigned cards & navigation**: Rounded video, channel, tag, and settings cards with focus outlines, updated browse headers, and refreshed search bar/orb styling.
- **Updated player & dialogs**: Restyled playback controls, seek/thumbs bars, tooltips, preference lists, sign-in/error dialogs, and live chat items.

All core streaming, SponsorBlock, playback, and device features come from upstream SmartTube.

## Download

[![Build Debug APK](https://github.com/Optinux/smarttube-pretty/actions/workflows/CI.yml/badge.svg)](https://github.com/Optinux/smarttube-pretty/actions/workflows/CI.yml)

Pre-built APKs are compiled automatically on every commit via GitHub Actions:

1. Open the [**latest successful CI builds**](https://github.com/Optinux/smarttube-pretty/actions/workflows/CI.yml?query=is%3Asuccess).
2. Click the newest run at the top.
3. Scroll down to **Artifacts** and download the APK for your device (most Android TV devices use `arm64`, older 32-bit devices use `armeabi-v7a`, or pick `universal`).

> **Note:** GitHub requires you to be signed in to a GitHub account to download workflow artifacts.

## Building

Requires OpenJDK 17 and the Android SDK:

```bash
git clone https://github.com/Optinux/smarttube-pretty.git
cd smarttube-pretty
git submodule update --init
./gradlew clean assembleStstableDebug
```

To build and install directly over ADB:

```bash
adb connect <device_ip_address>
./gradlew clean installStstableDebug
```

## Upstream & License

- **Original project**: [yuliskov/SmartTube](https://github.com/yuliskov/SmartTube) — refer there for full documentation, compatibility notes, and FAQ.
- **License**: [MIT](LICENSE)
