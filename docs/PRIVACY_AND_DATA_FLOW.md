# Privacy and Data Flow

## Data collected

Callora has **no internet permission and no backend service**. It cannot
transmit data off the device. All data is created by the user or by Android
telephony APIs the user grants access to.

| Data | Source | Stored where | Purpose |
| --- | --- | --- | --- |
| Subscription handle, display/carrier name, slot, SIM state, MCC-MNC | `SubscriptionManager`/`TelephonyManager` (READ_PHONE_STATE) | Room, local only | Per-SIM configuration and operator registry lookup |
| Destination numbers | User input, validated by libphonenumber | Room, local only | MMI command construction |
| Desired profiles, schedules, per-SIM conditions | User input | Room/DataStore, local only | Automation and reconciliation |
| Operations and observed statuses | App workflow | Room, local only | Audit trail, conflict prevention, provenance |
| Activity history | App workflow | Room, local only | User-visible diagnostics |
| Language, theme, notification, onboarding preferences | User input | DataStore, local only | Settings |

## Explicitly not collected

- ICCID, IMSI, line phone number (unavailable to third-party apps; not requested).
- Contacts, call logs, SMS, location, microphone, camera.
- Call content, caller identities, analytics identifiers, advertising IDs.
- Anything uploaded to a remote server — there is no network code path.

## Persistence and protection

- Room database (`callora.db`) and Preferences DataStore live in app-private
  storage, protected by Android sandboxing.
- No Keystore-encrypted column is currently required because no credentials or
  identifiers beyond user-entered numbers are stored; a masking helper avoids
  full numbers in casual UI surfaces.
- Android Auto Backup / cloud backup rules include only the app database and
  preferences on the user's own backup account; users can disable system backup.
- "Clear all stored configuration" in Settings drops Room tables and DataStore.

## Third-party libraries

- AndroidX/Google libraries (Compose, Room, WorkManager, AppCompat, DataStore).
- libphonenumber (on-device metadata, no network calls).
- No advertising, analytics or crash-reporting SDKs.

## Diagnostics

Diagnostic information stays in the in-app history screen. Full phone numbers
are not written to history messages; history records conditions, outcomes and
subscription handles.
