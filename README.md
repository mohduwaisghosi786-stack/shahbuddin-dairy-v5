<div align="center">

<img src="art/logo.png" width="160" height="160" alt="Shahbuddin Dairy Logo" style="border-radius: 28px;" />

# Shahbuddin Dairy V8.0

**Modern, Offline-First Android Dairy & Milk Distribution Management System**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Storage-Room%20SQLite%20(Offline--First)-4285F4?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Version](https://img.shields.io/badge/Version-8.0.0-blue)](https://github.com/mohduwaisghosi786-stack/shahbuddin-dairy-v5/releases/tag/v8.0.0)
[![License](https://img.shields.io/badge/License-Proprietary-red)](#)

</div>

---

## 📖 Overview

**Shahbuddin Dairy** is a business-grade Android application designed specifically for daily milk distribution businesses, route runners, and dairy khata management. Built with a 100% offline-first architecture, it ensures seamless operation in high-speed morning delivery environments with zero latency.

---

## 🚀 Key Modules & Capabilities

### ⚡ 1. Smart Runner 2.0 (Daily Delivery Flow)
- **Route-Optimized Delivery Sequence:** Real-time customer delivery list with sequential Auto-Next navigation.
- **Quick Milk Quantity Actions:** One-tap quick buttons (`-0.5L`, `+0.5L`, `+1.0L`) and custom quantity adjustments.
- **Status Toggles:** Instant toggle between **Served (Done)**, **Naaga (Skipped)**, and **Pending**.
- **Real-Time Delivery Counter:** Live visual breakdown of total served volume, pending stops, and naagas.

### 👥 2. Customer 360° & Khata Management
- **Full Profile Overview:** View current balance, lifetime delivery history, advance payments, and customer tags.
- **Smart Tags & Badges:** Categorize customers with operational tags (`VIP`, `Defaulter`, `Morning Only`, `Evening Only`).
- **Flexible Delivery Schedules:** Support for daily delivery, alternate-day supply, custom milk combinations (Buffalo & Cow).
- **Persistent Notes 2.0:** Save critical delivery instructions and customer-specific preferences.

### 📊 3. Reports & Analytics Dashboard
- **Multi-Period Analytics:** Daily, 7-Day Weekly, and 30-Day Monthly detailed business breakdowns.
- **Milk Type Segregation:** Automatic calculation of Buffalo vs. Cow milk consumption and revenue.
- **Instant Search & Filters:** Fast customer filtering by status (`All`, `Served`, `Naaga`, `Pending`) and text search.
- **Direct Export:** Generate professional Daily PDF statements and formatted WhatsApp sales reports.

### 💳 4. UPI QR & Digital Image Receipts
- **Dynamic ZXing QR Engine:** Instant offline UPI QR generation tailored with exact customer due amounts.
- **Digital Image Receipts:** Generate shareable visual receipt cards complete with payment QR and ledger breakdown.
- **Payment Reconciliation:** Record partial, full, and advance cash/online payments.

### 🛡️ 5. Privacy & Security (PIN App Lock)
- **Local Salted SHA-256 PIN:** Protect sensitive business finances without requiring internet or third-party servers.
- **Zero-Friction Default:** App Lock remains **OFF by default** and can be activated at any time from Settings.
- **Automatic Inactivity Lock:** Configurable auto-lock timeouts for field use safety.

### 🔄 6. Dual Backup System & Health Status
- **Local & Cloud Redundancy:** Export structured JSON snapshots and Room database backups.
- **3-Way Health Grid:** Live indicator validating Local Snapshot, Cloud Sync, and Database Integrity.
- **Zero Data Loss Guarantee:** Safe in-place restore mechanism preserving historical transactions.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 1.9+ |
| **UI Framework** | Jetpack Compose (Material Design 3) |
| **Architecture** | MVVM + Unidirectional Data Flow (StateFlow / Kotlin Coroutines) |
| **Local Database** | Android Room (SQLite) with Type Converters |
| **Dependency Injection** | Android ViewModel / ViewModelProvider Factory |
| **Document Generation** | Android PDF Document Engine & ZXing QR Code Library |
| **Minimum SDK** | Android 7.0 (API Level 24) |
| **Target SDK** | Android 14 / 15 (API Level 34+) |

---

## 🏗️ Project Structure

```
shahbuddin-dairy-v5/
├── app/
│   ├── src/main/
│   │   ├── java/com/shahbuddindairy/app/
│   │   │   ├── data/            # Room Entities, DAOs, Database & Repositories
│   │   │   ├── domain/          # Business logic, calculations & use cases
│   │   │   ├── security/        # Local salted SHA-256 PIN Security Manager
│   │   │   ├── ui/
│   │   │   │   ├── components/  # Reusable UI components & Header bars
│   │   │   │   ├── navigation/  # Compose Navigation Destinations
│   │   │   │   ├── screens/     # Screen Composables & ViewModels
│   │   │   │   └── theme/       # Color palettes, typography & shapes
│   │   │   └── util/            # PDF generation, UPI QR & Formatters
│   │   ├── res/                 # Vector drawables, mipmaps & strings
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── art/                         # Branding and application icon assets
├── build.gradle
├── settings.gradle
└── README.md
```

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug / Koala or newer
- JDK 17+
- Android SDK 34+

### Build Release APK
```bash
# Build the production release APK
./gradlew assembleRelease

# Output APK path:
# app/build/outputs/apk/release/app-release.apk
```

### Install onto Device via ADB
```bash
adb install -r -d app/build/outputs/apk/release/app-release.apk
```

---

## 📄 License & Ownership

Copyright © 2026 Shahbuddin Dairy. All Rights Reserved.  
Proprietary software developed for **Shahbuddin Dairy** milk distribution network.
