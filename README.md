# SmartTube Pretty

A UI-focused fork of [SmartTube](https://github.com/yuliskov/SmartTube) that updates the Android TV interface with a **Material 3 Expressive** visual style.

## What's Changed

- **Material 3 Expressive TV theming**: New surface/container color tokens, typography overrides, and updated dark/accent color schemes.
- **Redesigned cards & navigation**: Rounded video, channel, tag, and settings cards with focus outlines, updated browse headers, and refreshed search bar/orb styling.
- **Updated player & dialogs**: Restyled playback controls, seek/thumbs bars, tooltips, preference lists, sign-in/error dialogs, and live chat items.

All core streaming, SponsorBlock, playback, and device features come from upstream SmartTube.

## Download

[![Build and Release APK](https://github.com/Optinux/smarttube-pretty/actions/workflows/CI.yml/badge.svg)](https://github.com/Optinux/smarttube-pretty/actions/workflows/CI.yml)

Pre-built signed release APKs and the in-app update manifest are published automatically to [**GitHub Releases**](https://github.com/Optinux/smarttube-pretty/releases) on every `master` push and release tag:

1. Open the [**latest release**](https://github.com/Optinux/smarttube-pretty/releases/latest).
2. Download the APK for your device (most Android TV devices use `arm64-v8a`, older 32-bit devices use `armeabi-v7a`, or pick `universal`).
3. Once installed, future updates can be installed directly in-app (About -> **Check for updates** or automatic launch check).

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
