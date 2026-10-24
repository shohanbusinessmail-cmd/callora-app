# Callora Implementation Plan

## Product goals

Reduce missed calls for Bangladesh Android users when the primary device is
powered off, out of coverage, busy or unanswered, using the strongest genuinely
supported mechanism: operator network-side call forwarding, managed through
safe, transparent, user-confirmed workflows.

## Technical constraints (verified in the build environment)

- The development sandbox has no JDK, Android SDK or Gradle and cannot download
  them (egress is restricted to a small host allowlist). Local Gradle builds are
  therefore **BLOCKED**; all compilation, tests, lint and APK production run in
  **GitHub Actions**, which has full SDK/network access.
- Android restricts programmatic call-forwarding control
  (`TelephonyManager.setCallForwarding`/`CallForwardingInfo`) to carrier-
  privileged/system apps. Third-party builds must use standard GSM MMI handoff
  via the system dialer, and cannot parse MMI interrogation responses.
- Background activity starts are restricted: workers reconcile state and notify,
  but carrier changes always require foreground user confirmation.
- Bangladesh MCC is 470; MNCs: 01 Grameenphone, 02 Robi, 03 Banglalink,
  04 Teletalk, 07 Airtel Bangladesh.

## Architecture decisions

- Single Gradle module with package-based layering (model / domain / telephony /
  database / datastore / designsystem / background / feature) — simpler than
  multi-module at this size, same separation of concerns.
- Manual DI through an `AppContainer`; no Hilt/KAPT, KSP only (Room).
- Desired policy and observed carrier state are separate data models; a pure,
  deterministic reconciliation engine produces decisions that background workers
  and UI execute safely.
- MMI strings come from a standards-based builder (3GPP TS 22.004 codes
  21/67/61/62); per-operator provisioning/charging evidence lives in the registry.
- Compose + Material 3, Navigation Compose, ViewModel/StateFlow, coroutines/Flow.
- Room for structured data, DataStore Preferences for settings, WorkManager for
  recovery/scheduling, libphonenumber for BD number validation.

## Feature priorities

1. Honest telephony foundations: scanner, capability provider, backends, state
   model, coordinator with conflict prevention and provenance.
2. Persistence and reconciliation (Room, DataStore, WorkManager, boot receiver).
3. Premium UX: onboarding, dashboard, SIM detail, confirmation/result dialogs.
4. Profiles, scheduling, history, settings, troubleshooting, about.
5. Localization (en/bn), accessibility, adaptive icon.
6. Tests, CI, release engineering.

## Telecom research tasks

- Track official Android API restrictions and per-operator evidence in
  `TELEPHONY_CAPABILITIES.md` and `BANGLADESH_OPERATORS.md`.
- Re-verify operator pages before each release; never promote NOT VERIFIED claims
  without new evidence.
- Define a real-device CFNRc test protocol (configure before power-off, call from
  a controlled second phone, confirm destination rings).

## Testing strategy

- JVM unit tests for pure logic: MMI builder, number normalization, state
  machine, retry policy, schedule math, reconciliation, operator registry.
- Robolectric tests for Room, DataStore and the full coordinator workflow.
- Compose UI tests for onboarding/about/troubleshooting/status chips — compiled
  in CI, executed on emulator/device when available.
- GitHub Actions runs assembleDebug, testDebugUnitTest, lintDebug and
  assembleDebugAndroidTest; device/carrier tests remain explicitly documented as
  not executable in standard CI.

## Milestones

- M1 scaffold + design system + models; M2 telephony layer + registry;
  M3 persistence + coordinator; M4 UI features; M5 background automation;
  M6 localization + icon; M7 tests + CI; M8 docs + v0.1.0 release.

## Known risks

See KNOWN_LIMITATIONS.md (carrier API restriction, unknown operator behaviour,
SIM mis-selection, false-active status, repeated charges, background limits,
reboot behaviour, data exposure, UI fragmentation, build environment, release
verification).

## Release criteria

Green CI (build + unit tests + lint), honest documentation of verification
status, no fake core functionality, debug APK artifact produced by Gradle, and a
tag-triggered GitHub Release that references real artifacts only.
