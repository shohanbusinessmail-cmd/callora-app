# Android Telephony Capabilities (researched constraints)

Last reviewed: 2026-10-24. This document records what the public Android SDK
allows a third-party application to do, and how Callora adapts.

## Subscription discovery

- `SubscriptionManager.getActiveSubscriptionInfoList()` is available with
  `READ_PHONE_STATE` and returns display name, carrier name, slot index and the
  app-local subscription handle.
- Since Android 10 (API 29), ICCID, IMSI and the line's phone number are
  restricted to privileged/system apps. Callora therefore records
  `phoneNumberAvailable` instead of guessing a number, and never persists ICCID/IMSI.
- `TelephonyManager.createForSubscriptionId(subId)` (API 29+) gives per-SIM
  `simOperator` (MCC+MNC) under `READ_PHONE_STATE`. Callora guards the call by
  SDK level and falls back to `UNKNOWN` operator rather than crashing or guessing.
- Slot indices are not stable identities: Callora keys config by subscription id
  and suspends automation when a configured SIM disappears, instead of applying
  rules to whatever SIM now occupies the slot.

## Call-forwarding APIs

- `TelephonyManager.setCallForwarding(CallForwardingInfo, …)` and related
  interrogation APIs are guarded by carrier privileges / signature-level
  permissions and are not part of the third-party SDK surface. A Play-installed
  app cannot call them.
- `TelephonyManager.hasCarrierPrivileges()` is a public check; Callora's
  `OfficialApiBackend` performs this check honestly and reports UNAVAILABLE for
  normal installs instead of faking an API path.
- There is no public API to read USSD/MMI responses. `*#62#` interrogation
  results cannot be parsed by a third-party app, so network-verified status is
  only reachable on carrier-privileged builds; user-reported outcomes are stored
  with distinct provenance and never labelled as verified.

## What Callora actually does

1. Builds standard GSM/3GPP TS 22.004 MMI strings (`**21*`, `**67*`, `**61*`,
   `**62*`, `##`, `*#`) in `MmiCodeBuilder` — standards syntax, not invented
   operator codes.
2. Hands them to the system phone app via `ACTION_CALL` (with `CALL_PHONE`) or
   `ACTION_DIAL` (user presses call), targeting the selected subscription
   through `TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE` when resolvable.
3. Records `COMMAND_SUBMITTED_UNCONFIRMED`, then promotes only to
   `USER_REPORTED` based on an explicit in-app question. Verified states remain
   reserved for authoritative queries available to privileged builds.
4. Offers evidence-backed manual guides and an explicit unsupported fallback
   that preserves configuration without claiming success.

## The four conditions

| Condition | Code | Purpose | Powered-off relevance |
| --- | --- | --- | --- |
| CFU | 21 | Forward every eligible call | Works regardless of handset state |
| CFB | 67 | Forward while busy | Requires handset/network busy detection |
| CFNRy | 61 | Forward after no reply (timer 5–30 s) | Handset must be ringing |
| CFNRc | 62 | Forward when unreachable | **Primary protection while off/out of coverage** |

CFNRc is the only condition designed for the powered-off scenario because the
network itself decides the subscriber is unreachable. No Android background code
can run after shutdown; protection must therefore be provisioned beforehand,
which is exactly what Callora's onboarding and reconciliation explain.

## Background execution reality

- Doze, app standby, battery optimization and OEM restrictions delay background
  work; WorkManager jobs are inexact and Callora never promises minute precision.
- Background activities cannot be launched freely, so workers cannot open the
  dialer. They reconcile, persist, and notify; the user confirms changes in the
  foreground.
- `RECEIVE_BOOT_COMPLETED` allows post-reboot reconciliation of network-side
  rules, which survive independently of the device because they live in the
  carrier network.

## Permission mapping

See [PERMISSIONS.md](PERMISSIONS.md). Callora requests only READ_PHONE_STATE,
CALL_PHONE, RECEIVE_BOOT_COMPLETED, ACCESS_NETWORK_STATE and POST_NOTIFICATIONS.
No SMS, contacts, call-log, location or microphone permissions are used.
