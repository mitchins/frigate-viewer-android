# Frigate Viewer

A simple Android application built with Kotlin and Jetpack Compose for viewing live camera streams from a Frigate NVR system.

## Features

- **Live RTSP Streaming**: View H.265 camera streams using ExoPlayer with hardware decoding
- **Multiple Layouts**: Choose between Single View, 2x2 Grid, or 3x3 Grid layouts
- **Flexible Camera Selection**: Select which cameras to display using a modal selector
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

By default, the app uses RTSP streams via go2rtc:
- RTSP URL format: `rtsp://<frigate-ip>:8554/<camera-name>`

The app also supports alternative stream types (configured in Camera.kt:18):
- HLS streams
- MJPEG fallback

### Architecture

- **MVVM Architecture**: ViewModel manages UI state and business logic
- **Retrofit**: HTTP client for Frigate API calls
- **ExoPlayer**: Media player with RTSP support
- **Jetpack Compose**: Modern declarative UI
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
- Media3 ExoPlayer with RTSP extension
- Retrofit with Gson converter
- OkHttp
- Kotlin Coroutines

## Notes

- Ensure your Android device can access your Frigate server on the local network
- The app uses `usesCleartextTraffic="true"` to allow HTTP connections (see AndroidManifest.xml:19)
- For production use, consider using HTTPS
- Some older devices may have limited H.265 hardware decoding support

## License

This project is provided as-is for personal use.
