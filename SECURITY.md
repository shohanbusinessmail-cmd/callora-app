# Security Policy

## Supported versions

| Version | Supported |
| --- | --- |
| 0.1.x | Yes — current development line |

## Design posture

- No internet permission and no remote endpoint: the attack surface for data
  exfiltration through the app is intentionally absent.
- Minimal runtime permissions (see `docs/PERMISSIONS.md`); no SMS, contacts,
  call-log, location or microphone access.
- No signing material, tokens or credentials are stored in the repository.
- Dependencies are pinned in the Gradle version catalog and resolved from
  Google Maven / Maven Central over HTTPS.

## Reporting a vulnerability

Email helloiamshohan@gmail.com with a description, reproduction steps and
impact. Please give a reasonable disclosure window before any public post.

## Handling carrier commands

Callora only opens the system dialer with standard GSM codes after explicit
user confirmation. It never intercepts calls or messages, and it cannot read
USSD responses. If a vulnerability could cause an unconfirmed carrier change,
treat it as critical severity.

## Supply chain

CI uses pinned major-version GitHub Actions with `contents: read` (the release
workflow requires `contents: write` only to publish releases). Dependabot/action
updates should be reviewed before merging.
