# NFC Transaction Flow — Card Reader App

This document describes how the Reader app initiates NFC transactions, builds signed tokens, and integrates with the mock backend and FCM consent.

---

## 1. Architecture (Clean + MVVM)

- **Presentation**: `TransactionScreen` (Compose), `TransactionViewModel` — amount input, state, NFC trigger.
- **Domain**: `TransactionToken`, `TransactionState`, `BackendDecision`, `ApduResponse`, `TransactionRepository` interface.
- **Data**: `NfcManager`, `ApduBuilder`, `ApduParser`, `TokenGenerator`, `KeystoreSigner`, `MockBackendService`, `TransactionRepositoryImpl`, FCM service and consent receiver.

NFC and crypto are independent of the UI and can be reused elsewhere.

---

## 2. Transaction UI Flow

1. User enters **amount** (in PKR — Pakistani Rupee) and taps **Initiate & Send**.
2. App generates a signed **transaction token** (see §4) and **immediately** sends it to the **mock backend** (online flow).
3. Backend returns **APPROVED** / **DECLINED** / **PENDING_CONSENT**; the result is shown in the “Transaction state” card.
4. App enters **Waiting for tap**: user taps the **receiver device** (NFC).
5. App sends the **same token** over **ISO-DEP** via APDU; receiver responds with status bytes.
6. **Response received** and status/message are shown. User can **Reset** to start over.

---

## 3. NFC Reader Mode (ISO-DEP)

- **NfcManager**: Uses `IsoDep` from the discovered `Tag`; connects, `transceive(commandApdu)`, then closes.
- **Foreground dispatch**: Enabled in `MainActivity.onResume()` with `IsoDep` tech list so this app gets the tag when in front.
- **Activity**: `onNewIntent` receives the tag and calls `TransactionViewModel.onNfcTagDiscovered(tag)`.
- **APDU**: `ApduBuilder.buildSendTokenCommand(tokenBytes)` builds `CLA INS P1 P2 Lc [data]` (INS = 0x74). `ApduParser.parseResponse(response)` reads SW1-SW2 and optional data.

---

## 4. Mobile-Side Token Generation

- **Canonical payload** (signed string): `deviceId|amount|nonce|timestamp|txnType`.
- **Fields**:  
  - `deviceId`: from `DeviceIdProvider` (e.g. `reader_<androidId>`).  
  - `amount`: user input (PKR).  
  - `nonce`: 16 bytes cryptographically secure random, hex-encoded.  
  - `timestamp`: `System.currentTimeMillis()`.  
  - `txnType`: e.g. `"PAYMENT"`.
- **TokenGenerator**: builds payload, calls **KeystoreSigner** to sign payload bytes, then builds **TransactionToken** (deviceId, amount, nonce, timestamp, txnType, **signature**).
- **Token JSON** (for APDU and backend):  
  `{ "deviceId", "amount", "nonce", "timestamp", "txnType", "signature" }`.

---

## 5. Cryptography

- **KeystoreSigner**: Android Keystore, alias `CardReaderTxKey`, **EC (secp256r1)**, **SHA256withECDSA**.
- Private key is **non-exportable** (KeyGenParameterSpec in Android Keystore).
- **Sign**: input = canonical payload UTF-8 bytes; output = **Base64** signature (no newlines).

---

## 6. NFC Data Exchange (APDU)

- **Send**: APDU built with `ApduBuilder`; data = token JSON bytes (≤ 255 bytes).
- **Receive**: raw response bytes; `ApduParser` returns `ApduResponse(statusCode, statusMessage, data)`.
- Success when SW1=0x90, SW2=0x00.

---

## 7. Online Flow (Mock Backend)

- After generating the token, the app **immediately** calls `TransactionRepository.submitToBackend(token)`.
- **MockBackendService** (in-app):
  - **Verify signature**: simulated (checks non-empty).
  - **Nonce reuse**: in-memory set; duplicate nonce → **DECLINED**.
  - **Amount**: if amount ≤ configured limit (e.g. 50,000 PKR) → **APPROVED**; else → **PENDING_CONSENT**.
- Result is shown in the transaction state card.

---

## 8. Firebase Cloud Messaging (Consent)

- **TransactionConsentService** extends `FirebaseMessagingService`: in `onMessageReceived`, reads `transactionId` (or `nonce`) from data payload, creates a **notification** with **Approve** and **Reject** actions.
- **ConsentActionReceiver**: BroadcastReceiver for those actions; gets `TransactionRepository` from **CardReaderApplication** and calls `sendConsentDecision(transactionId, approved)`.
- **transactionId** in the FCM payload should be the **nonce** of the pending transaction so the mock backend can resolve it.
- To test: send a data message with `transactionId` = nonce of a transaction that returned **PENDING_CONSENT**; user taps Approve/Reject and the decision is sent to the mock backend.

---

## 9. Deliverables Summary

| Item | Location |
|------|----------|
| NFC manager (ISO-DEP lifecycle, transceive) | `data/nfc/NfcManager.kt` |
| Token generator (nonce, payload, sign, JSON) | `data/crypto/TokenGenerator.kt` |
| Keystore signer (ECC, non-exportable, Base64) | `data/crypto/KeystoreSigner.kt` |
| Mock backend (verify, nonce store, limit, consent) | `data/backend/MockBackendService.kt` |
| APDU command builder / response parser | `data/nfc/ApduBuilder.kt`, `ApduParser.kt` |
| Transaction repository | `domain/repository/TransactionRepository.kt`, `data/repository/TransactionRepositoryImpl.kt` |
| Transaction UI + ViewModel | `presentation/transaction/` |
| FCM consent service + receiver | `data/fcm/TransactionConsentService.kt`, `ConsentActionReceiver.kt` |
| Flow documentation | This file |

---

## 10. Configuration

- **Mock backend limit**: `MockBackendService(maxAutoApproveAmountPkr = 50_000L)` (50,000 PKR). Change in `CardReaderApplication` if needed.
- **Firebase**: Replace `app/google-services.json` with your project’s file for real FCM.
- **Notification channel**: `transaction_consent`; id in manifest matches `TransactionConsentService.CHANNEL_ID`.
