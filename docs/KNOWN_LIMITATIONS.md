# Known Limitations and Risk Register

Status labels follow the project convention: IMPLEMENTED, BUILD VERIFIED,
UNIT TESTED, UI TESTED, DEVICE TESTED, CARRIER VERIFIED, PARTIALLY SUPPORTED,
NOT VERIFIED, BLOCKED.

## Risk A — Carrier API restrictions

Programmatic call-forwarding control is restricted to carrier-privileged apps.
Mitigation: real privilege check in `OfficialApiBackend`, user-confirmed MMI
handoff and manual guides. Status: IMPLEMENTED; privileged API path NOT
VERIFIED (unreachable for normal installs by design).

## Risk B — Unknown operator behaviour

Only Grameenphone has official-source evidence in this repository; Teletalk
rows are NOT VERIFIED. Mitigation: evidence-backed registry with dated sources,
explicit in-app warnings and a user verification procedure. Status:
IMPLEMENTED; CARRIER VERIFIED = not performed.

## Risk C — Incorrect SIM selection

Dual-SIM dialer routing is OEM-dependent. Mitigation:
`PhoneAccountHandle` targeting when resolvable, persistent SIM-selection
warning, subscription-id keying and suspension when the configured SIM
disappears. Status: IMPLEMENTED; DEVICE TESTED = NOT VERIFIED.

## Risk D — False active status

A saved preference or launched intent is not carrier proof. Mitigation:
separate desired/observed models, `VerificationProvenance` enumeration,
user-reported outcomes never promoted to verified, unknown states never
auto-deactivated. Status: IMPLEMENTED + UNIT TESTED.

## Risk E — Repeated carrier charges

Mitigation: in-flight conflict prevention, idempotent operation ids, bounded
retry policy that excludes rejections/ambiguous responses, explicit charge
warning before every handoff. Status: IMPLEMENTED + UNIT TESTED.

## Risk F — Android background restrictions

Doze/standby/OEM killers delay WorkManager; background activities cannot start.
Mitigation: network-side forwarding is the real protection; workers only
reconcile and notify; inexact scheduling is disclosed in the UI. Status:
IMPLEMENTED; DEVICE TESTED = NOT VERIFIED.

## Risk G — Unexpected reboot behaviour

Mitigation: boot receiver enqueues network-constrained reconciliation; the
reconciler compares desired/observed and never blindly erases rules. Status:
IMPLEMENTED + UNIT TESTED (engine); device path NOT VERIFIED.

## Risk H — Data exposure

Mitigation: no internet permission, minimal local data, no ICCID/IMSI,
masking helper for numbers, backup rules limited to app data, no third-party
analytics/SDKs beyond AndroidX and libphonenumber. Status: IMPLEMENTED.

## Risk I — UI fragmentation

Adaptive Compose layouts, dynamic type, TalkBack semantics, status text not
colour-only. Status: IMPLEMENTED; visual/device inspection NOT VERIFIED
(no emulator in the build environment).

## Risk J — Build environment limitations

The sandbox lacks a JDK/Android SDK and cannot download them. Mitigation:
reproducible Gradle wrapper + GitHub Actions build/test/lint pipeline and
artifact publication. Status: BUILD VERIFIED via CI (see Actions tab).

## Risk K — Incomplete release verification

The v0.1.0 artifact is a debug-signed APK; no production keystore, device test or
carrier test exists yet. Mitigation: release notes and this document disclose
the exact gap; no claim of production/carrier readiness is made.

## Feature-level honesty notes

- MMI result parsing is impossible for third-party apps; the "verified" state is
  reachable only via authoritative APIs on privileged builds. Current builds
  therefore produce verified states only through future privileged integration;
  user-confirmed states remain explicitly unverified.
- Scheduling uses inexact work; exact-minute execution is not promised.
- eSIM and multi-profile handling inherit Android subscription visibility;
  hidden/disabled profiles may not appear.
