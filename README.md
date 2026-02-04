# Robot Controller

An Android app that controls a robot in real time over WebSocket. Connect your phone and robot to the same WiFi network, pick a control mode, and drive.

---

## Features

- **Two control modes** — switch between a classic D-Pad and an analog joystick with a single tap
- **Analog joystick** — touch-driven virtual stick with spring-back physics, circular clamping, and 20 Hz throttled output normalized to -1.0 .. 1.0
- **D-Pad controls** — directional buttons for forward, backward, left, right, and stop
- **Speed slider** — adjust motor speed from 0 % to 100 % in real time
- **Auto-reconnect** — exponential backoff (1 s, 2 s, 4 s ... up to 30 s) with configurable retry limit
- **Persistent settings** — server URL and reconnection preferences survive app restarts via DataStore
- **Material You** — dynamic color on Android 12+, full dark mode support

---

## Control Modes

### D-Pad

Sends discrete directional commands on each button press.

```
        [ Forward  ]
[ Left ] [  STOP  ] [ Right ]
        [ Backward ]
```

### Joystick

Sends continuous `x, y` values while the knob is held. On release the knob springs back to center and the robot receives `0, 0` immediately.

```
      ╭───────────╮
      │           │
      │     ●     │  ← draggable knob
      │           │
      ╰───────────╯
      x: 0.00  y: 0.00
```

The joystick outputs are throttled at ~20 Hz to avoid flooding the WebSocket.

---

## WebSocket Protocol

The app sends JSON messages over a WebSocket connection. The robot server should listen and parse these commands:

### Direction commands (D-Pad)

```json
{"command": "forward"}
{"command": "backward"}
{"command": "left"}
{"command": "right"}
{"command": "stop"}
```

### Speed

```json
{"command": "speed", "value": 50}
```

`value` is an integer from 0 to 100.

### Joystick

```json
{"command": "joystick", "x": 0.71, "y": -0.45}
```

| Field | Type  | Range          | Description                        |
|-------|-------|----------------|------------------------------------|
| `x`   | float | -1.00 to 1.00  | Horizontal axis. Negative = left.  |
| `y`   | float | -1.00 to 1.00  | Vertical axis. Negative = backward.|

Both values are `0.00` when the joystick is at rest.

---

## Architecture

The project follows Clean Architecture with three layers:

```
┌─────────────────────────────────────────────┐
│  Presentation                               │
│  ┌───────────────────┐  ┌────────────────┐  │
│  │ RobotControlScreen│  │ RobotControl   │  │
│  │ (Compose UI)      │──│ ViewModel      │  │
│  └───────────────────┘  └───────┬────────┘  │
├─────────────────────────────────┼───────────┤
│  Domain                         │           │
│  ┌──────────────┐ ┌─────────────┴────────┐  │
│  │ RobotCommand │ │ ConnectionState      │  │
│  │ RobotSettings│ │                      │  │
│  └──────────────┘ └─────────────┬────────┘  │
├─────────────────────────────────┼───────────┤
│  Data                           │           │
│  ┌───────────────┐  ┌──────────┴─────────┐  │
│  │ WebSocketClient│  │ RobotRepository   │  │
│  └───────────────┘  └────────────────────┘  │
│  ┌───────────────┐                          │
│  │SettingsDataStore│                        │
│  └───────────────┘                          │
└─────────────────────────────────────────────┘
```

### Source layout

```
app/src/main/java/com/gonzalo/robotcontroller/
├── MainActivity.kt
├── data/
│   ├── preferences/
│   │   └── SettingsDataStore.kt      # Persists server URL and reconnect prefs
│   ├── repository/
│   │   └── RobotRepository.kt       # Connection lifecycle, auto-reconnect
│   └── websocket/
│       └── WebSocketClient.kt       # OkHttp WebSocket wrapper
├── domain/model/
│   ├── ConnectionState.kt           # Connected | Connecting | Disconnected | Error
│   ├── RobotCommand.kt              # Sealed class with toJson()
│   └── RobotSettings.kt             # Server URL, reconnect config
├── presentation/
│   ├── RobotControlScreen.kt        # All Compose UI
│   └── RobotControlViewModel.kt     # State management
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 11+
- An Android device or emulator running API 24+ (Android 7.0)
- A robot with a WebSocket server listening for JSON commands

### Build

```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug
```

### Run

1. Start the WebSocket server on your robot (default expected at `ws://192.168.1.100:8765`)
2. Connect your phone to the same WiFi network as the robot
3. Open the app and tap **Connect**
4. Choose a control mode (D-Pad or Joystick) and drive

### Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

---

## Configuration

Settings are persisted automatically with DataStore Preferences:

| Setting              | Default                      | Description                          |
|----------------------|------------------------------|--------------------------------------|
| Server URL           | `ws://192.168.1.100:8765`    | WebSocket endpoint of the robot      |
| Auto-reconnect       | Enabled                      | Retry on connection loss             |
| Max retry attempts   | 5                            | Stop reconnecting after N failures   |

Reconnection uses exponential backoff capped at 30 seconds.

---

## Tech Stack

| Component        | Library                          |
|------------------|----------------------------------|
| UI               | Jetpack Compose + Material 3     |
| State management | ViewModel + StateFlow            |
| Networking       | OkHttp 4.12 (WebSocket)         |
| Persistence      | DataStore Preferences            |
| Concurrency      | Kotlin Coroutines 1.9            |
| Serialization    | kotlinx-serialization 1.7        |
| Min SDK          | 24 (Android 7.0)                 |
| Language          | Kotlin 2.0                      |

---

## Adding a New Command

1. Add a new case to the `RobotCommand` sealed class in `domain/model/RobotCommand.kt`
2. Add the JSON serialization in `toJson()`
3. Add UI controls in `RobotControlScreen.kt`
4. Call `onSendCommand(YourNewCommand)` from the UI

---

## License

This project is for personal/educational use.
