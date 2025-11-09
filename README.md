# Intra
[![Build Status](https://travis-ci.org/Jigsaw-Code/Intra.svg?branch=master)](https://travis-ci.org/Jigsaw-Code/Intra)
[![Android Build](../../actions/workflows/android_build.yml/badge.svg)](../../actions/workflows/android_build.yml)

Intra is an experimental tool that allows you to test new DNS-over-HTTPS
services that encrypt domain name lookups and prevent manipulation by your
network. It currently supports services from Cloudflare and Google, and
additional options may be added over time.  You can get it from the
Google Play Store [here](https://play.google.com/store/apps/details?id=app.intra).

## Version 1.5.0 - Material 3 Update

This version includes a major UI overhaul with Material 3 design system:
* **Modern Material 3 UI** - Complete redesign using Material Design 3 components
* **Dedicated Query History Page** - View your DNS queries on a separate page with search and filtering
* **Enhanced Information Pages** - More detailed technical information about DNS protection
* **Automated Builds** - GitHub Actions workflow for continuous integration
* **Package Name**: `app.intra.wuyuan`

Features:
* Built-in support for public DNS services from Cloudflare and Google
* Visualization of server performance and application query behavior
* Geocoding of query results to compare against expected regional results
* Search and filter DNS query history
* Detailed technical information about DNS-over-HTTPS protection

## Android build instructions

### Using Android Studio
1. Clone this repo.
2. Open the `Android/` directory in Android Studio (Arctic Fox or later recommended).
3. Connect your phone
4. Click the green "play" triangle button.

### Using Command Line
```bash
cd Android
./gradlew assembleDebug
```

The APK will be generated at `Android/app/build/outputs/apk/debug/app-debug.apk`

### Automated Builds
GitHub Actions automatically builds the APK on every push to master/main branches. Check the Actions tab for build artifacts.
