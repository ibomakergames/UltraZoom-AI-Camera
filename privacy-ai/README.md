# Privacy AI for Android

A standalone Kotlin + Jetpack Compose prototype, kept in this repository under `privacy-ai/` so it does not modify the existing UltraZoom AI Camera app.

## Build
Open this folder as an Android Studio project, or use the repository workflow to build the debug APK.

## Current prototype
- Arabic, right-to-left dark interface with purple accents.
- Persisted local preferences for protection, notification setting, schedule setting, and privacy-level slider.
- Uses Android `FLAG_SECURE` to block screenshots and screen recording **inside the Privacy AI app window only**.
- Clearly labels notification, app-selection, and scheduling controls as setup placeholders until their required system integrations are implemented.

## Important limitation
A normal app cannot reproduce Samsung's hardware Privacy Display viewing-angle behavior. That requires display-panel hardware. This prototype does not claim otherwise.
