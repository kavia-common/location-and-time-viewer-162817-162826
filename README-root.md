# Workspace Root

This repository contains an Android app under android_frontend/.

To build from the workspace root (for CI tools that run `./gradlew` here), use the provided proxy wrapper:

- Build: `./gradlew build`
- Install debug (connected device): `./gradlew :android_frontend:app:installDebug`

Internally this delegates to `android_frontend/gradlew`.
