# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Robot Controller is an Android application built with Kotlin and Jetpack Compose that controls a robot over WebSocket communication. The app and robot connect to the same WiFi network, allowing real-time command transmission. The architecture is designed to scale from simple one-way command sending to bidirectional communication with video streaming capabilities.

**Package name:** `com.gonzalo.robotcontroller`
**Default WebSocket URL:** `ws://192.168.1.100:8765`

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

The app follows Clean Architecture principles with clear separation of concerns across three layers:

### Layer Structure

**Presentation Layer** (`presentation/`)
- `RobotControlViewModel` - Manages UI state and coordinates operations
- `RobotControlScreen.kt` - Main control UI with directional controls and speed slider

**Domain Layer** (`domain/model/`)
- `RobotCommand` - Sealed class representing robot commands (Forward, Backward, Left, Right, Stop, Speed)
- `ConnectionState` - WebSocket connection states (Connected, Connecting, Disconnected, Error)
- `RobotSettings` - Configuration for connection and reconnection behavior

**Data Layer** (`data/`)
- `WebSocketClient` - Low-level WebSocket handling using OkHttp
- `RobotRepository` - Connection lifecycle management and automatic reconnection with exponential backoff
- `SettingsDataStore` - Persists connection settings using DataStore Preferences

### Robot Command Protocol

Commands are sent as JSON over WebSocket:
```json
{"command": "forward"}
{"command": "backward"}
{"command": "left"}
{"command": "right"}
{"command": "stop"}
{"command": "speed", "value": 50}
```

### Project Structure
```
app/src/main/java/com/gonzalo/robotcontroller/
├── MainActivity.kt
├── data/
│   ├── preferences/
│   │   └── SettingsDataStore.kt
│   ├── repository/
│   │   └── RobotRepository.kt
│   └── websocket/
│       └── WebSocketClient.kt
├── domain/
│   └── model/
│       ├── ConnectionState.kt
│       ├── RobotCommand.kt
│       └── RobotSettings.kt
├── presentation/
│   ├── RobotControlScreen.kt
│   └── RobotControlViewModel.kt
└── ui/theme/
```

### Key Dependencies
- **Compose BOM:** 2024.09.00
- **ViewModel Compose:** 2.9.4
- **Coroutines:** 1.9.0
- **OkHttp:** 4.12.0 (WebSocket client)
- **Kotlinx Serialization:** 1.7.3
- **DataStore Preferences:** 1.1.1
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

### WebSocket Communication
- Connection is managed by `RobotRepository` with automatic reconnection
- Reconnection uses exponential backoff (1s, 2s, 4s, 8s, 16s, max 30s)
- Default max reconnection attempts: 5
- Commands are only sent when connection state is `Connected`

### Adding New Commands
To add a new command type:
1. Add new sealed class case to `RobotCommand` in `domain/model/RobotCommand.kt`
2. Update the `toJson()` method to serialize the new command
3. Add UI controls in `RobotControlScreen.kt`

### Settings Persistence
Settings are persisted using DataStore Preferences:
- Server URL (default: `ws://192.168.1.100:8765`)
- Reconnect enabled flag
- Max reconnection attempts
Access via `SettingsDataStore` in data layer

### Scalability Notes
The architecture is designed to easily add:
- Bidirectional communication (incoming message handling already structured in `WebSocketClient`)
- Video streaming (add separate WebSocket channel or HTTP stream)
- Multiple robot support (extend repository to manage multiple connections)
- Additional message types (extend sealed classes)

### Testing Strategy
- Unit tests go in `app/src/test/`
- Instrumented tests (require device/emulator) go in `app/src/androidTest/`
- Test runner: AndroidJUnitRunner
- Repository and ViewModel can be tested independently by mocking dependencies

### Build Variants
- **Debug:** Default development build
- **Release:** Production build (requires signing configuration for distribution)
