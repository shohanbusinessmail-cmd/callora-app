# Callora Architecture

## Overview

Callora is a single-Gradle-module Android application organized into strict
package-based layers. The architecture exists to keep carrier policy, carrier
commands, observed state and UI state independently testable, so that no
component can mistake a saved preference for a verified network rule.

```
feature/ (Compose screens + ViewModels, immutable UI state)
   ↓
core/domain (pure Kotlin: state machine, reconciliation, retry, schedules, validation)
   ↓
core/telephony (subscription scanning, capability provider, backends, coordinator)
   ↓
core/database (Room) + core/datastore (Preferences DataStore)
background/ (WorkManager workers, boot receiver, worker factory)
core/designsystem (Material 3 theme tokens + reusable components)
core/di (AppContainer — manual dependency graph)
```

## Key components

### Model layer (`core/model`)

Pure Kotlin data types with no Android imports: `ForwardingCondition`,
`ForwardingState`, `VerificationProvenance`, `ForwardingOperation`,
`ObservedForwardingStatus`, `ReconciliationDecision`, `OperatorCapability`,
`AutomationPolicy`, profiles, schedules and history events.

### Domain layer (`core/domain`)

- `PhoneNumberNormalizer` — libphonenumber wrapper, Bangladesh region only.
- `MmiCodeBuilder` (telephony package) — 3GPP TS 22.004 MMI strings.
- `ForwardingStateMachine` — pure reducer over `ForwardingState`.
- `ReconciliationEngine` — deterministic desired-vs-observed decision maker.
- `RetryPolicy` — bounded exponential backoff with jitter and explicit
  non-retryable failure classes.
- `ScheduleCalculator` — weekday/window math using `java.time`.

### Telephony layer (`core/telephony`)

- `SubscriptionScanner` — real `SubscriptionManager`/`TelephonyManager` access,
  permission-aware, no ICCID/IMSI persistence.
- `TelephonyCapabilityProvider` — device permission/privilege/connectivity snapshot.
- `OperatorCapabilityRegistry` — evidence records per Bangladesh operator.
- `CallForwardingBackend` implementations:
  `OfficialApiBackend` (privilege-gated), `MmiIntentBackend` (system dialer
  handoff), `ManualGuideBackend`, `UnsupportedBackend`.
- `ForwardingOperationCoordinator` — single mutation entry point; idempotency,
  conflict prevention, provenance rules, failure classification, history.

### Data layer

- Room (`CalloraDatabase`, version 1, schema export enabled) for destinations,
  profiles, schedules, per-SIM config, operations, history, observed status.
- Preferences DataStore for app settings (language, theme, notifications,
  onboarding, default profile, last reconciliation).

### Background layer

- `ReconciliationWorker` — periodic (12 h) and boot/network-triggered
  reconciliation; never starts activities, so it records decisions and asks the
  user to confirm material carrier changes via notification.
- `ScheduleTriggerWorker` — evaluates schedule windows and chains reconciliation.
- `BootCompletedReceiver` — enqueues network-constrained reconciliation.
- Custom `WorkerFactory` injects the same `AppContainer` graph.

### Presentation layer

- Jetpack Compose + Material 3, Navigation Compose, five bottom-bar destinations.
- ViewModels expose `StateFlow<UiState>`; events travel through snackbar channels.
- All user-facing strings are resources (`values/`, `values-bn/`); no
  hardcoded UI text in composables.

## Design decisions

- **No Hilt**: the graph is small; a manual container removes KAPT and keeps
  build times and version risk low.
- **KSP for Room** only.
- **Desired vs observed separation**: profiles/config express intent;
  `ObservedForwardingStatus` carries evidence. The UI never treats the former as
  proof of carrier state.
- **Foreground-only mutation**: dialer intents require an Activity, so all
  carrier changes are user-confirmed in a foreground screen.
- **No internet permission**: there is no remote service; all data stays local.

## Data flow for an activation

1. User enables CFNRc and taps Activate in `SimDetailScreen`.
2. `SimsViewModel` → `ForwardingOperationCoordinator.prepareOperation()`.
3. Conflict check → operation persisted as PENDING → backend selected from real
   capability → `MmiHandOff` returned with the exact MMI string.
4. Confirmation dialog displays command, SIM, destination, charge warning.
5. `MmiIntentBackend.launch()` opens the system dialer; operation becomes
   SUBMITTED with `COMMAND_SUBMITTED_UNCONFIRMED` provenance.
6. The user records the carrier response → `USER_REPORTED` provenance; state is
   never promoted to ACTIVE_VERIFIED without an authoritative query.
7. History and observed status are updated; reconciliation workers use the same
   typed state on subsequent boots/network changes.
