# 🛡️ ShieldCore

A comprehensive Android security suite built with Kotlin and Jetpack Compose. ShieldCore combines antivirus scanning, real-time protection, network monitoring, app locking, junk cleaning, and privacy tooling into a single modern Material 3 app.

---

## ✨ Features

### 🦠 Antivirus & Malware Scanner
- **On-demand full-device scan** of installed applications (APK hash + binary signature analysis)
- **Native C++ scanning engine** (`scanner-native` module, CMake/NDK) with O3 optimizations
- **Real-time protection** — foreground service (`RealtimeShieldService`) continuously monitors the device
- **Install-time scanning** — `PackageInstallReceiver` inspects every new/replaced app immediately
- **Threat detection & quarantine** with risk-level classification (Safe / Suspicious / Malicious)
- Scan history persisted in Room (`ScanReportEntity`)

### 🌐 Network & Wi-Fi Security
- Scans the connected network for unknown/rogue devices
- Wi-Fi details and connectivity status monitoring
- Detects suspicious open networks

### 🔒 App Lock
- Lock any installed app with a PIN, pattern, or biometrics
- Overlay-based lock screen (`AppLockActivity`, `SYSTEM_ALERT_WINDOW`)
- Usage-stats driven auto-locking of protected apps
- Tamper-resistant `AppLockSecurityManager` (SHA-256 protected credential storage)

### 🧹 Junk Cleaner
- Scans caches, temp files, and residual data
- One-tap cleanup with per-category selection
- Space-freed summary after each clean

### 🔗 Phishing Protection
- **Local VPN service** (`PhishingVpnService`) performs non-intrusive link inspection
- **Accessibility-based address-bar URL checks** while browsing
- Blocklist/allowlist engine with URL reputation lookup (Retrofit)

### 🔓 Breach Monitoring
- Checks your email against known data-breach databases
- Flags compromised credentials and suggests remediation

### 🔋 Battery & Performance
- Battery health, temperature, and charge-cycle monitoring
- Power-hungry app detection and optimization suggestions

---

## 🏗️ Architecture

Clean, modular architecture with strict layer separation:

```
┌─────────────────────────────────────────────────────────┐
│                       :app (Compose UI)                 │
│     Screens · ViewModels (MVI) · Services · Receivers   │
├─────────────────────────────────────────────────────────┤
│   :core        Utilities · Base MVI components          │
│   :domain      Models · Repository interfaces · UseCases│
│   :data        Repositories · Room DB · DataStore       │
│   :scanner-native   C++ scanner engine (NDK/CMake)      │
└─────────────────────────────────────────────────────────┘
```

- **UI**: Jetpack Compose + Material 3, single-activity with `MainScreen` navigation
- **State**: MVI pattern (`UiState` / `UiEvent` / `UiEffect` in `:core`)
- **DI**: Hilt + KSP across all modules
- **Persistence**: Room 2.7, DataStore Preferences
- **Background work**: WorkManager (`ScannerWorker`), foreground services, accessibility service, local VPN service

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 17) |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| DI | Dagger Hilt 2.60 |
| Database | Room 2.7 + KSP |
| Networking | Retrofit, OkHttp, Gson |
| Native | C++17, CMake 3.22, NDK |
| Concurrency | Kotlin Coroutines + Flow |
| Build | Gradle 9.7, AGP 9.3.1 (built-in Kotlin 2.2.10) |

**Minimum SDK**: 26 (Android 8.0) · **Target/Compile SDK**: 35 · **ABIs**: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`

---

## 🚀 Getting Started

### Prerequisites
- JDK 17+ (bundled JBR with Android Studio works)
- Android Studio (or SDK + NDK + CMake 3.22.1)
- Android SDK 35

### Build

```bash
# Debug APK (signed with debug keystore)
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📱 Required Permissions

ShieldCore is a security tool and therefore requires sensitive permissions:

| Permission | Purpose |
|---|---|
| `QUERY_ALL_PACKAGES` | Enumerate installed apps for scanning |
| `MANAGE_EXTERNAL_STORAGE` | Scan APKs/files on storage (API ≤ 32) |
| `ACCESS_FINE/COARSE_LOCATION` | Wi-Fi network scanning |
| `PACKAGE_USAGE_STATS` | Detect app launches for App Lock |
| `SYSTEM_ALERT_WINDOW` | Overlay lock screen |
| `USE_BIOMETRIC` | Biometric unlock |
| `BIND_ACCESSIBILITY_SERVICE` | Address-bar URL inspection |
| `BIND_VPN_SERVICE` | Local phishing-inspection VPN |
| `FOREGROUND_SERVICE*` | Real-time protection service |
| `POST_NOTIFICATIONS` | Alert notifications |

> ⚠️ Accessible services, overlay, and usage-stats access must be granted manually in system settings on first launch.

---

## 📁 Project Structure

```
app/                 → Android application (UI, services, DI)
core/                → Shared utilities & MVI building blocks
domain/              → Pure business logic (models, interfaces, use cases)
data/                → Repositories, Room database, KSP-processed code
scanner-native/      → C++ malware-signature scanner (NDK)
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes
4. Push and open a pull request

---

## 📄 License

This project is provided for educational and research purposes. Use it responsibly — the security features require elevated system permissions and should be distributed in compliance with applicable laws and store policies.
