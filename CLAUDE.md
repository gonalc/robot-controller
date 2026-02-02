# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Robot Controller is an Android application built with Kotlin and Jetpack Compose. The project uses modern Android development practices with Compose for UI, Material3 design components, and Kotlin as the primary language.

**Package name:** `com.gonzalo.robotcontroller`

## Build Configuration

- **Compile SDK:** 36
- **Min SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 35
- **Java Version:** 11
- **Kotlin Version:** 2.0.21
- **AGP Version:** 8.9.1

## Common Commands

### Building
```bash
./gradlew build                  # Build the project (both debug and release)
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew clean                  # Clean build artifacts
```

### Testing
```bash
./gradlew test                   # Run all unit tests
./gradlew testDebugUnitTest      # Run debug unit tests only
./gradlew connectedAndroidTest   # Run instrumented tests on connected device/emulator
./gradlew connectedDebugAndroidTest  # Run debug instrumented tests
```

### Installing
```bash
./gradlew installDebug           # Install debug build on connected device
./gradlew uninstallAll           # Uninstall all builds from device
```

### Code Quality
```bash
./gradlew lint                   # Run lint checks
./gradlew lintDebug              # Run lint on debug build with text output
./gradlew lintFix                # Run lint and apply safe suggestions
./gradlew check                  # Run all checks (lint + tests)
```

## Architecture

### UI Layer
- **UI Framework:** Jetpack Compose (Material3)
- **Main Activity:** `MainActivity.kt` - Single activity serving as the app's entry point
- **Theme:** Custom theme defined in `ui.theme` package with Color, Type, and Theme files

### Project Structure
```
app/src/
├── main/
│   ├── java/com/gonzalo/robotcontroller/
│   │   ├── MainActivity.kt          # App entry point
│   │   └── ui/theme/                # Theming components (Color, Type, Theme)
│   ├── res/                         # Resources (layouts, strings, etc.)
│   └── AndroidManifest.xml
├── test/                            # Unit tests
└── androidTest/                     # Instrumented tests
```

### Key Dependencies
- **Compose BOM:** 2024.09.00 (manages Compose library versions)
- **Core KTX:** 1.17.0
- **Lifecycle Runtime KTX:** 2.9.4
- **Activity Compose:** 1.11.0
- **Testing:** JUnit 4.13.2, AndroidX JUnit 1.3.0, Espresso 3.7.0

### Dependency Management
Dependencies are managed using version catalogs in `gradle/libs.versions.toml`. Reference dependencies in build files using `libs.` prefix (e.g., `libs.androidx.core.ktx`).

## Development Notes

### Compose UI
The app uses Jetpack Compose for all UI. When creating new screens:
- Use `@Composable` functions
- Follow Material3 design guidelines
- Use `@Preview` annotations for preview support in Android Studio
- Theme components are available through `RobotControllerTheme`

### Testing Strategy
- Unit tests go in `app/src/test/`
- Instrumented tests (require device/emulator) go in `app/src/androidTest/`
- Test runner: AndroidJUnitRunner

### Build Variants
- **Debug:** Default development build
- **Release:** Production build (requires signing configuration for distribution)
