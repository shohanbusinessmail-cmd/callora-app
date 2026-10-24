# Callora release notes

## v0.1.0 — first functional build

- Single-module Kotlin + Jetpack Compose (Material 3) application, minSdk 26 / targetSdk 35.
- Real subscription discovery via SubscriptionManager/TelephonyManager with permission-aware fallbacks.
- Standard GSM MMI builder (CFU/CFB/CFNRy/CFNRc) and user-confirmed system-dialer handoff backend.
- Official-API backend honestly reports carrier-privilege restriction; manual guide and unsupported fallbacks included.
- Typed forwarding state machine, verification provenance, conflict-preventing operation coordinator and bounded retry policy.
- Evidence-backed Bangladesh operator registry (Grameenphone, Robi, Airtel, Banglalink, Teletalk) with NOT VERIFIED handling.
- Room + DataStore persistence, WorkManager reconciliation, boot recovery and inexact scheduling.
- Profiles, schedules, activity history, troubleshooting, notifications, English/Bengali localization, adaptive vector launcher icon.
- JVM/Robolectric unit tests and Compose UI tests; GitHub Actions CI builds, tests, lints and attaches the debug APK.

This is an early functional release. No live carrier verification has been
performed by the project; see docs/BANGLADESH_OPERATORS.md and
docs/KNOWN_LIMITATIONS.md for the exact evidence status.
