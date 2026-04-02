# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BoanCurator Android app — 보안 뉴스 큐레이션 플랫폼. Kotlin + Jetpack Compose + Material 3.
Backend (FastAPI) is deployed at `bc-api.danyeon.cloud`.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Run lint checks
./gradlew lint

# Clean + rebuild
./gradlew clean assembleDebug
```

Open in Android Studio for emulator/device testing. Min SDK 26, Target SDK 35.

## Architecture

**MVVM**: Screen → ViewModel → Repository → ApiService (Retrofit)

```
ui/screens/{feature}/
  ├── {Feature}Screen.kt      (Composable UI)
  └── {Feature}ViewModel.kt   (State + business logic)

data/
  ├── api/ApiService.kt        (Retrofit endpoints)
  ├── api/AuthInterceptor.kt   (Auto-attach JWT header)
  ├── model/Models.kt          (All data classes)
  └── repository/              (ArticleRepo, AuthRepo, BookmarkRepo)

di/AppModule.kt                (Hilt: Retrofit, OkHttp, TokenManager)
navigation/NavGraph.kt         (5 screens, bottom nav for 4 main tabs)
util/TokenManager.kt           (DataStore-based JWT persistence)
```

## Key Design Decisions

- **Dark cyber theme**: Background `#0D1117`, accent `#00D4FF` (cyan), secondary `#7B61FF` (neon blue). All colors defined in `ui/theme/Color.kt`.
- **Level badges**: 초급=cyan, 중급=purple, 고급=red
- **Bottom nav**: Home (피드) / Search (검색) / Bookmarks (북마크) / Profile (프로필)
- **Detail screen** hides bottom nav, opens article URL in external browser

## Backend API

- Base URL: `https://bc-api.danyeon.cloud` (configured in `app/build.gradle.kts` as `API_BASE_URL`)
- Swagger: `https://bc-api.danyeon.cloud/docs`
- Auth: Google ID Token → `POST /v1/auth/google` → JWT → Bearer header (auto-injected by `AuthInterceptor`)

## Google OAuth Setup

Set `GOOGLE_CLIENT_ID` in `app/build.gradle.kts` `buildConfigField`. Must match backend's `.env`.
Google Cloud Console must have the app's SHA-1 fingerprint registered for Credential Manager.

## Dependencies (Version Catalog)

Managed in `gradle/libs.versions.toml`. Key libs: Compose BOM 2024.12, Hilt 2.53.1, Retrofit 2.11, Coil 3.0.4, Navigation Compose 2.8.5.
