# DueDate 📅

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)

**DueDate** is a modern, privacy-focused Android application for calculating exact target dates from a start date and duration. Built with 100% Kotlin, Jetpack Compose, and Material 3 design guidelines.

---

## ✨ Features

- **Calendar-Aware Logic**: Add Days, Weeks, Months, or Years accurately using standard Java 8+ time APIs (`java.time.LocalDate`).
  - End-of-month clamping (e.g. *Jan 31 + 1 month = Feb 28/29*).
  - Handles leap years and leap days seamlessly.
- **Inclusive Start Date Mode**: Option to include the start date as Day 1 of the period.
- **Quick Duration Presets**: Instant one-tap selection for common periods (e.g., 7 days, 30 days, 90 days, 6 months, 1 year).
- **Calculation History**: Auto-saves recent calculations with tap-to-restore and confirmation dialogs for deletion.
- **Copy & Share**: Easily copy or share detailed calculation summaries with other apps.
- **Modern Material 3 Design**:
  - Light, Dark, and System theme modes.
  - Dynamic Color support (Android 12+ wallpaper integration).
  - Haptic feedback and subtle animations.
  - Edge-to-edge support with translucent system bars.
- **100% Offline & Private**: Zero network permissions required, no trackers, ads, or analytics.

---

## 🏗️ Architecture & Tech Stack

DueDate follows Modern Android Development (MAD) best practices with Clean Architecture and MVVM (Model-View-ViewModel) design pattern:

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3 components
- **Navigation**: Compose Navigation with top-level bottom bar navigation
- **State Management**: `StateFlow` and `collectAsStateWithLifecycle` for unidirectional data flow
- **Storage**: SharedPreferences with custom JSON codec
- **Asynchronous Execution**: Kotlin Coroutines
- **Testing**: JUnit 4 with unit test coverage for domain logic, validators, repositories, and ViewModels (108+ unit tests)

---

## 📂 Project Structure

```
app/src/main/java/com/example/duedate/
├── data/           # Data persistence interfaces & SharedPreferences store
├── domain/         # Core business logic (DueDateCalculator, DueDateFormatter, DueDateValidator)
├── model/          # Immutable domain models (DueDateResult, DueDateHistory, AppSettings)
├── repository/     # Data repository abstractions for History and Settings
├── ui/             # Jetpack Compose UI screens
│   ├── about/      # App information & privacy screen
│   ├── history/    # Recent calculations screen
│   ├── home/       # Main calculator interface & result cards
│   ├── settings/   # Theme and app preferences screen
│   ├── splash/     # Splash screen with customizable animations
│   ├── theme/      # Material 3 Color palette & Typography
│   ├── DueDateApp.kt # Navigation host & Scaffold structure
│   └── DueDateViewModel.kt # Central state holder
└── util/           # Utility helpers for Haptics, Sharing, and Date conversions
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer
- **JDK**: Version 17+
- **Android SDK**: Target SDK 35/37 (Minimum SDK 26 - Android 8.0)

### Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/abdullah-ateeq/DueDate.git
   cd DueDate
   ```

2. **Run Unit Tests**:
   ```bash
   ./gradlew test
   ```

3. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🧪 Testing

The codebase includes thorough unit test coverage across all domain calculation edge cases, input validators, repository persistence, and ViewModel state transitions:

```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

```
MIT License

Copyright (es) 2026 Abdullah Ateeq

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software.
```
