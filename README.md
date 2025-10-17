# Frigate Viewer

A simple Android application built with Kotlin and Jetpack Compose for viewing live camera streams from a Frigate NVR system.

## Features

- **Live RTSP Streaming**: H.264/H.265 playback via libVLC (TCP) with hardware decoding where available
- **Multiple Layouts**: Choose between Single View, 2x2 Grid, or 3x3 Grid layouts
- **Flexible Camera Selection**: Select cameras in a modal and apply changes on Done (prevents mid-selection stalls)
- **Auto-discovery**: Automatically fetches available cameras from your Frigate server
- **Material 3 Design**: Modern UI with Material Design 3

## Requirements

- Android 10 (API level 29) or higher
- Frigate NVR server with go2rtc configured
- Network access to your Frigate server

## Configuration

By default, the app connects to `http://192.168.1.15:5000`. To change this:

1. Open `RetrofitInstance.kt` (app/src/main/java/com/example/frigateviewer/data/api/RetrofitInstance.kt:11)
2. Update the `baseUrl` variable to your Frigate server address

```kotlin
private var baseUrl: String = "http://YOUR_FRIGATE_IP:PORT/"
```

## How It Works

### Stream Discovery

The app fetches the list of available cameras from your Frigate server using the `/api/config` endpoint.

### Stream Playback

- RTSP via go2rtc (forced TCP): `rtsp://<frigate-ip>:8554/<stream-name>`
- Backed by libVLC 4.x using `VLCVideoLayout` for rendering in Compose
- One shared `LibVLC` instance per screen; each tile creates its own `MediaPlayer`
- Only the first tile has audio enabled by default to reduce decoder contention

### Architecture

- **MVVM**: ViewModel manages UI state and business logic
- **Retrofit**: HTTP client for Frigate API calls
- **libVLC**: RTSP playback (H.264/H.265), TCP transport, configurable caching
- **Jetpack Compose**: Modern declarative UI with `VLCVideoLayout`
- **Kotlin Coroutines**: Asynchronous operations

## Project Structure

```
app/src/main/java/com/example/frigateviewer/
├── data/
│   ├── api/              # Retrofit API service and configuration
│   ├── model/            # Data models
│   └── repository/       # Data repository layer
├── ui/
│   ├── components/       # Reusable UI components
│   ├── screens/          # Screen composables
│   ├── theme/            # Material theme configuration
│   └── viewmodel/        # ViewModels
└── MainActivity.kt       # Main activity
```

## Building

Open the project in Android Studio and build:

```bash
./gradlew assembleDebug
```

## Usage

1. Launch the app
2. Tap the menu icon to open the camera selector
3. Select up to 1, 4, or 9 cameras (depending on your layout)
4. Tap the settings icon to change the grid layout
5. Streams will automatically start playing

## Dependencies

- AndroidX Core, Lifecycle, Activity Compose
- Jetpack Compose (Material 3, UI)
- Navigation Compose
- libVLC (`org.videolan.android:libvlc-all`)
- Retrofit with Gson converter
- OkHttp
- Kotlin Coroutines

## Notes

- Ensure your Android device can access your Frigate server on the local network
- The app uses `usesCleartextTraffic="true"` to allow HTTP connections (see `AndroidManifest.xml`)
- For production use, consider HTTPS
- Emulators often struggle with multiple concurrent RTSP decoders; test on a real device when possible
- Some older devices have limited H.265 hardware decoding support

## Multi‑view and Selection

- The camera selector batches changes and applies them when tapping Done to avoid tearing down/creating players repeatedly
- Grid items use stable keys so players are reused when possible
- Only one tile has audio enabled by default (the first)

## Troubleshooting

- Black video or “video output display creation failed”:
  - We use `VLCVideoLayout` and disable direct rendering (`--no-mediacodec-dr`) to improve compatibility
- Stalls when switching selection or ANR while selector is open:
  - Changes now apply on close; if issues persist, increase caching (`:network-caching=500`) or reduce active tiles
- “frame size exceeds the client's buffer size” logs:
  - We set `:rtsp-frame-buffer-size=2000000`; increase if needed for very high bitrate IDR frames
- No playback over Wi‑Fi/Emulator complaining about source address:
  - Transport is forced to RTSP over TCP; verify network reachability and try on a physical device

## License

This project is provided as-is for personal use.
