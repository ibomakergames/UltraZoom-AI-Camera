# Privacy AI for Android

A standalone Kotlin + Jetpack Compose prototype in `privacy-ai/`. It is kept separate from the existing UltraZoom AI Camera app.

## Build locally

Open the `privacy-ai/` folder in Android Studio and let Gradle sync, then build the app configuration. The GitHub Actions workflow also builds a debug APK on pushes and pull requests that touch this project.

The workflow artifact is named `privacy-ai-debug-apk`.

## Implemented in this version

- Arabic right-to-left dark interface with purple accents.
- Local persistence for the protection switch and privacy-level display preference.
- Android `FLAG_SECURE` is applied to the Privacy AI activity window while protection is enabled. This blocks screenshots and ordinary screen recording of this app's own window only.

## Not implemented yet

- Hiding notifications across Android.
- Selecting and protecting other installed apps.
- Automatic schedules.

These are shown as roadmap items, not working switches. Android requires additional system-level integrations and user-granted permissions for some of these behaviors.

## Hardware limitation

A normal Android app cannot reproduce Samsung's hardware Privacy Display viewing-angle behavior. Making the screen appear dark from side angles requires compatible display-panel hardware. The privacy-level slider in this prototype is only a saved visual preference and does not change the physical viewing angle.
