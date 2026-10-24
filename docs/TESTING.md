# Testing Strategy and Verification Matrix

## What runs where

| Layer | Tooling | Where it runs | Status in CI |
| --- | --- | --- | --- |
| Pure domain logic | JUnit 4 + Truth, plain JVM | `testDebugUnitTest` | Executed |
| Android-dependent persistence/integration | Robolectric (pinned SDK 33) | `testDebugUnitTest` | Executed |
| Compose UI | `androidx.compose.ui:ui-test-junit4` | Emulator/device | Compiled (`assembleDebugAndroidTest`), not executed in CI (no KVM device) |
| Lint | Android lint | `lintDebug` | Executed |
| Real device | Physical Android phone | Manual | NOT VERIFIED — see below |
| Live carrier | Controlled SIM test | Manual | NOT VERIFIED — no SIM available to the project |

## Unit test coverage

- `MmiCodeBuilderTest` — GSM 21/67/61/62 syntax, timer variant, erasure/query, input validation.
- `PhoneNumberNormalizerTest` — BD local/E.164 normalization, region rejection, empty/garbage input.
- `ForwardingStateMachineTest` — provenance-aware transitions; user reports never become verified.
- `RetryPolicyTest` — retryable failure classes, bounded jittered backoff, max-attempt and rejection rules.
- `ScheduleCalculatorTest` — weekday windows, overnight wrap, all-day and disabled schedules.
- `ReconciliationEngineTest` — missing SIM, permission/backend gaps, unknown-state safety,
  destination mismatch, desired-off rules, manual policy, in-flight de-duplication.
- `OperatorCapabilityRegistryTest` — all five operators present, GP official evidence,
  Teletalk NOT VERIFIED, MCC-MNC resolution, evidence invariants.
- `RoomDatabaseTest` (Robolectric) — destination CRUD, in-flight conflict counts, history, config retention.
- `SettingsRepositoryTest` (Robolectric) — DataStore defaults and persisted updates.
- `ForwardingOperationCoordinatorTest` (Robolectric) — handoff preparation, conflict rejection,
  unconfirmed-submission provenance, user-reported success/rejection, deactivation flow.

## UI tests (device-scoped)

- First-launch onboarding display and page navigation.
- About screen developer credit (`Shohan Khan`) and contact email.
- Troubleshooting content and localized status chip rendering.

## Local execution

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebugAndroidTest
```

## Device test protocol (when hardware becomes available)

1. Single-SIM then dual-SIM devices, at least one modern Android version.
2. SIM removal/replacement, reboot, airplane-mode toggles.
3. For CFNRc: register `**62*<destination>#`, power off, call from a second
   phone, confirm the destination rings; reboot and reconcile.
4. Verify deactivation with `##62#` and interrogation with `*#62#`.
5. Never run tests against emergency numbers.

Carrier tests must be performed with the subscriber's consent and awareness of
possible charges.
