# Study XP — Personal Gamified Learning App

Offline-first Android app implementing the Focus → Learn → XP → Level → Reward system.

## Features
- 1 XP per focused study minute
- Bonus XP actions
- Manual scrolling penalty
- Optional automatic penalty from selected social-app foreground usage (Instagram, Facebook, TikTok)
- 20+ net XP = Study Day / streak
- Levels and milestone rewards
- Local-only data; no login/server/ads
- Portrait Android app, min SDK 26

## Build
Open the `StudyXP` folder in Android Studio. Let Gradle sync, then use **Build → Build APK(s)**.

After building, the debug APK is normally under `app/build/outputs/apk/debug/`.

## Usage access
For automatic social-app usage penalty, open **Check App Usage / Setup**, grant Usage Access to Study XP, then return to the app and tap it again. The app syncs tracked foreground usage when opened; Android cannot reliably tell whether every minute was actually scrolling, so this is intentionally an approximate automation.
