# Permission Matrix

Callora requests only permissions tied to implemented features. No SMS,
contacts, call-log, location, microphone or camera permission is used, and the
app holds no `INTERNET` permission.

| Permission | Type / API level | Feature | When requested | If denied | Alternative |
| --- | --- | --- | --- | --- | --- |
| `READ_PHONE_STATE` | Runtime, API 26+ | Subscription discovery, operator/MCC-MNC detection, SIM state, carrier-privilege check | Onboarding permission page and when opening SIMs | SIM list stays empty; app explains the limitation and offers retry | None — subscription management cannot work without it |
| `CALL_PHONE` | Runtime, API 26+ | Handing a confirmed MMI command to the system dialer via `ACTION_CALL` | Only when the user activates/deactivates a rule and confirms the dialog | `ACTION_DIAL` pre-fills the exact MMI string and the user presses call manually | Fully supported manual path |
| `RECEIVE_BOOT_COMPLETED` | Normal, API 26+ | Enqueue post-reboot reconciliation of network-side rules | Install time (declared) | No automatic reconciliation after reboot; manual refresh still works | User opens Callora and refreshes |
| `ACCESS_NETWORK_STATE` | Normal, API 26+ | Dashboard readiness and network-constrained workers | Install time (declared) | Readiness shows unknown; workers still run without network constraints | Manual retry |
| `POST_NOTIFICATIONS` | Runtime, API 33+ | Status/manual-action notifications | Onboarding and first notification need (API 33+) | All functionality remains; changes appear only inside the app | In-app history and dashboard |

## Design rules enforced in code

- Permissions are checked at the point of use (`ContextCompat.checkSelfPermission`);
  no feature assumes a granted permission.
- `CALL_PHONE` is optional by construction: `MmiIntentBackend` falls back to
  `ACTION_DIAL`, which needs no permission.
- No privileged or signature permissions are requested. The official
  call-forwarding API is unreachable for third-party apps and is reported as
  such rather than bypassed.
- No identifier beyond the app-local subscription handle and user-entered
  destination numbers is persisted; ICCID/IMSI are never stored.
