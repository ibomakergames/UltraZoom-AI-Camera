# UltraZoom AI Camera

An Android camera app prototype built with Kotlin, Jetpack Compose, and CameraX.

## Current prototype
- Dark camera interface.
- CameraX live preview and photo capture.
- Rear/front camera switching.
- Flashlight toggle.
- Zoom presets: 1x, 3x, 10x, 30x, 60x, 100x, and 180x.
- Zoom requests are clamped to the maximum zoom ratio reported by the active camera. The interface does not pretend that unsupported magnification is available.
- Photos are saved to **Pictures/UltraZoom**.

## Important limitations
The current version is an initial camera prototype, not a finished AI camera. AI photo enhancement and advanced stabilization are not implemented yet. High digital zoom cannot restore detail that the sensor did not capture. Actual zoom limits depend on the camera hardware and the camera provider on the device.

## Build
Open this repository in Android Studio with JDK 17 and Android SDK 35, then run the `app` configuration. A GitHub Actions workflow also builds a debug APK on pushes to `main` and manual workflow runs.

## Package
`com.ibomaker.ultrazoom`
