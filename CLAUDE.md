# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform (KMP) project using Compose Multiplatform for UI, targeting Android and iOS platforms. The project uses Gradle with Kotlin DSL and the version catalog system.

## Build Commands

### Android

```bash
# Build debug APK
./gradlew :composeApp:assembleDebug

# Build release APK
./gradlew :composeApp:assembleRelease

# Build and run all Android variants
./gradlew :composeApp:assemble
```

### iOS

```bash
# Build iOS framework for simulator (arm64)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Build iOS framework for device
./gradlew :composeApp:linkDebugFrameworkIosArm64

# Build release frameworks
./gradlew :composeApp:linkReleaseFrameworkIosArm64
./gradlew :composeApp:linkReleaseFrameworkIosSimulatorArm64
```

The iOS app itself is built via Xcode from the `iosApp/` directory.

### Testing

```bash
# Run all tests
./gradlew :composeApp:allTests

# Run common tests only
./gradlew :composeApp:testDebugUnitTest

# Run iOS simulator tests
./gradlew :composeApp:iosSimulatorArm64Test
```

### General

```bash
# Clean build
./gradlew clean

# Full build and test
./gradlew :composeApp:build
```

## Architecture

### Source Set Structure

The project follows KMP conventions with platform-specific source sets under `composeApp/src/`:

- **commonMain**: Shared Kotlin code for all platforms. Contains the main `App.kt` composable, shared business logic, and `expect` declarations
- **commonTest**: Shared test code
- **androidMain**: Android-specific implementations (`actual` declarations) and Android entry point
- **iosMain**: iOS-specific implementations and `MainViewController` that bridges to SwiftUI

### Platform Abstraction Pattern

The codebase uses the `expect`/`actual` pattern for platform-specific code:
- `Platform.kt` in commonMain declares `expect fun getPlatform(): Platform`
- `Platform.android.kt` and `Platform.ios.kt` provide `actual` implementations

### iOS Integration

The iOS app (`iosApp/`) uses SwiftUI with a `ComposeView` wrapper that embeds the Compose Multiplatform UI via `MainViewController`. The framework is named `ComposeApp` and is statically linked.

### Key Dependencies

- Compose Multiplatform 1.9.3
- Kotlin 2.3.0
- Material 3 for theming
- AndroidX Lifecycle for ViewModel and runtime compose integration