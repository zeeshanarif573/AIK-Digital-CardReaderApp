# Card Reader App — NFC Transaction Initiator

Android app that acts as an **NFC transaction initiator (Reader)**: it builds a signed transaction token on-device and communicates with a receiver over **ISO-DEP**, with an in-app mock backend and FCM consent flow.

## Features

- **NFC reader mode (ISO-DEP)**: Detect tap, send token via APDU, parse response.
- **Transaction UI**: Amount input (PKR), states: initiated → sent → response received.
- **On-device token**: Nonce, timestamp, deviceId, amount, txnType → canonical payload → ECC sign (Android Keystore) → Base64 signature → JSON token.
- **Mock backend**: In-app verification, nonce reuse check, amount limit (e.g. ≤ 50,000 PKR auto-approved), returns APPROVED / DECLINED / PENDING_CONSENT.
- **FCM**: Consent push with Approve/Reject actions; decision sent to mock backend.

## Architecture

- **Clean Architecture + MVVM**: `presentation` (Compose + ViewModel), `domain` (models, repository interface), `data` (NFC, crypto, backend, repository impl, FCM).
- NFC and crypto are in separate modules and reusable.

## Docs

See **[docs/FLOW.md](docs/FLOW.md)** for the full transaction flow, APDU format, token structure, and configuration.

## Setup

1. Open in Android Studio and sync Gradle.
2. Replace `app/google-services.json` with your Firebase project file for real FCM.
3. Run on an NFC-capable device (or emulator with NFC); enable NFC in settings.

## Build

```bash
./gradlew assembleDebug
```
