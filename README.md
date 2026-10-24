# Callora — Smart Call Continuity

[![Android CI](https://github.com/shohanbusinessmail-cmd/callora-app/actions/workflows/android-ci.yml/badge.svg)](https://github.com/shohanbusinessmail-cmd/callora-app/actions/workflows/android-ci.yml)

Callora is an Android application for **Smart Call Continuity & Call Forwarding
Automation**, designed for the Bangladesh market. It helps users reduce missed
calls when the primary phone is powered off, out of coverage, busy, or
unanswered — using the operator's **network-side** call forwarding, the only
mechanism that works while the handset is physically off.

> **Honest engineering principle:** when an Android phone is powered off, no app
> code can run. Callora therefore provisions forwarding rules inside the carrier
> network *in advance*, and it never presents an unconfirmed dialer handoff as a
> network-verified result.

## Features

- Subscription discovery with real `SubscriptionManager` / `TelephonyManager`
  integration and privacy-safe SIM snapshots (single-SIM, dual-SIM, eSIM-aware).
- Four independently represented GSM forwarding conditions: **CFU (21), CFB
  (67), CFNRy (61), CFNRc (62)** — CFNRc is the primary power-off protection.
- Typed forwarding state machine with explicit provenance
  (verified / user-reported / submitted-unconfirmed / local policy).
- Evidence-backed [operator capability registry](docs/BANGLADESH_OPERATORS.md)
  for Grameenphone, Robi, Airtel Bangladesh, Banglalink and Teletalk, including
  NOT VERIFIED statuses instead of assumed support.
- Four backends selected from real device capability: official Android API
  (carrier-privileged only), user-confirmed standard GSM MMI handoff, manual
  guide, and honest unsupported fallback.
- Desired policy vs observed carrier state kept strictly separate; deterministic
  reconciliation engine that never blind-deactivates protection.
- Bounded exponential-backoff retry with conflict and duplicate-operation
  prevention; no automatic retries of rejected, potentially chargeable
  operations.
- Room persistence, DataStore preferences, WorkManager boot/network recovery
  and scheduling (inexact, Doze-aware — no false precision promises).
- Profiles (personal / office / custom / temporary), weekly schedules, activity
  history, and actionable troubleshooting.
- Premium Material 3 design in midnight navy + teal, light/dark themes, adaptive
  Compose layouts and full **English / বাংলা** localization.
- Minimal permissions: no SMS, contacts, call log, location, or microphone
  access; no internet permission and no remote backend.

## Supported environment

- Android **minSdk 26 (Android 8.0)** / targetSdk 35 / compileSdk 35.
- JDK 17, Gradle 8.9, Kotlin 2.0.21, Jetpack Compose (BOM 2024.10.01).
- A GSM subscription supporting standard supplementary-service MMI codes.

## Known carrier limitations (summary)

- Programmatic call-forwarding APIs are restricted to carrier-privileged
  applications; normal installs use the explicit, user-confirmed dialer workflow.
- Third-party apps cannot programmatically parse `*#62#` interrogation
  responses, so Callora records user-reported evidence rather than guessing.
- Only Grameenphone has an official-source capability document in this
  repository's research; other operators are documented from secondary/GSM
  standards evidence and marked accordingly. See
  [docs/BANGLADESH_OPERATORS.md](docs/BANGLADESH_OPERATORS.md).

## Build prerequisites

- JDK 17 and the Android SDK (platform 35, build-tools, platform-tools).
- Optional: a local Gradle 8.9 installation, otherwise use the wrapper.

## Build instructions

```bash
./gradlew assembleDebug          # debug APK -> app/build/outputs/apk/debug/
./gradlew testDebugUnitTest       # JVM + Robolectric unit tests
./gradlew lintDebug               # Android lint
./gradlew assembleDebugAndroidTest # compiles device UI tests
```

## Installation

The CI pipeline publishes a debug APK as a build artifact and, for version tags,
as a GitHub Release asset. Debug builds use application id `com.callora.app.debug`
and are signed with the Android debug key. Transfer the APK to a device and allow
installation from the hosting app when prompted. Production signing is
owner-managed — see [docs/BUILD_AND_RELEASE.md](docs/BUILD_AND_RELEASE.md).

## Configuration steps

1. Complete onboarding and grant READ_PHONE_STATE (required) and CALL_PHONE
   (optional; without it you press the call button yourself).
2. Open **SIMs**, choose a subscription and add a validated Bangladesh
   destination number.
3. Enable the desired conditions (CFNRc recommended for power-off protection).
4. Tap **Activate**, review the exact MMI command, charges warning and target
   SIM, then confirm the system-dialer handoff.
5. Record the carrier's response so the status reflects real evidence.

## Forwarding status meanings

| Status | Meaning |
| --- | --- |
| Active — verified | An authoritative query confirmed the rule. |
| Configured — not verified | A rule was submitted or user-confirmed but no network query confirmed it. |
| Activation / deactivation pending | An operation is in flight. |
| Inactive | Evidence indicates no rule is registered. |
| Manual action required | You must complete a step outside Callora. |
| Unsupported | No safe mechanism exists on this device/SIM. |
| Error / Unknown | The last operation failed, or no evidence has been collected. |

## Testing

See [docs/TESTING.md](docs/TESTING.md). Unit and Robolectric tests run in CI;
Compose UI tests are compiled in CI and require a physical device/emulator to
execute. No carrier has been tested live by the project yet — that status is
tracked explicitly in the operator matrix.

## Releases

Tags (`v*`) trigger [`.github/workflows/release.yml`](.github/workflows/release.yml),
which builds, tests, and publishes a GitHub Release with the debug APK attached.
The first release is
[`v0.1.0`](https://github.com/shohanbusinessmail-cmd/callora-app/releases/tag/v0.1.0)
(early functional build, debug-signed APK), not a 1.0.0 claim.

## Engineering report

The final Part 26 engineering report (in Bengali) is at
[docs/ENGINEERING_REPORT_BN.md](docs/ENGINEERING_REPORT_BN.md), with the full
evidence-label matrix, honest verification status and blocker list.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). All changes must build via the Gradle
wrapper, keep the localization key sets in sync, and preserve the no-fake-
functionality rules in [docs/KNOWN_LIMITATIONS.md](docs/KNOWN_LIMITATIONS.md).

## Contact

- Developer: **Shohan Khan**
- Email: helloiamshohan@gmail.com
- Repository: https://github.com/shohanbusinessmail-cmd/callora-app
