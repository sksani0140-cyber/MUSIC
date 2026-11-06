# MUSIC Android starter v0.2 — Firebase integration scaffold

## Included
- Android app UI for Shawon (ADMIN) and ফাতেমা (MEMBER)
- Firebase anonymous authentication initialization
- Firestore live-message listener and send flow (after a valid pairId is available)
- Cloud Functions for one-use 6-digit pairing codes (10-minute expiry, atomic redemption)
- Firestore security rules limiting message access to the two pair members
- WebRTC dependency and Android camera/microphone/internet permissions scaffold

## Required setup before it works on two phones
1. Create a Firebase project at https://console.firebase.google.com/
2. Add an Android app with package name `com.music.privatechat`.
3. Download `google-services.json` and place it in `app/google-services.json`.
4. Enable **Authentication > Anonymous**.
5. Create a Cloud Firestore database.
6. From `backend/functions`, run `npm install`; deploy functions with Firebase CLI (`firebase deploy --only functions`).
7. Deploy `backend/firestore.rules` as the Firestore rules.
8. Connect the Android pairing UI to callable functions `createPairCode` and `redeemPairCode`, store the returned pairId for each authenticated device, and add role/name assignment.
9. Complete WebRTC offer/answer/ICE signaling, call UI, foreground service, notification handling, and TURN relay configuration for reliable calls across mobile networks/NAT.

## Important honest status
This is an integration scaffold, **not a finished production app or APK**. Pairing UI currently shows setup guidance rather than invoking the functions. The chat code is wired to Firestore but requires a pairId to be provisioned. WebRTC library is included, but actual audio/video call flow is not implemented. Do not install as a finished private messenger yet.

## Build
Open in Android Studio (JDK 17, Android SDK 35), add `google-services.json`, sync Gradle, then Run.
