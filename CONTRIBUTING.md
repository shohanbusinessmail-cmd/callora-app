# Contributing to Callora

Thanks for your interest. Callora's priority is technical honesty: the app must
never present a preference as a verified carrier state.

## Ground rules

1. No fake functionality — every button must map to a real, observable behavior
   or an explicit explanation of the limitation.
2. Keep carrier policy inside `OperatorCapabilityRegistry` with dated evidence;
   do not invent USSD/MMI codes or operator claims.
3. Every user-facing string goes into `res/values/strings.xml` **and**
   `res/values-bn/strings.xml` with matching keys and placeholders.
4. Maintain the desired/observed state separation and the
   `VerificationProvenance` semantics. User-reported results are never verified.
5. Add unit tests for any change to pure domain logic (state machine,
   reconciliation, retry, schedule, MMI builder, number normalizer).
6. Do not add permissions beyond the documented matrix, and never commit
   keystores, credentials or personal data.

## Workflow

- Fork/branch from the working branch, keep commits focused and well described.
- Run locally (or rely on CI):
  `./gradlew assembleDebug testDebugUnitTest lintDebug`.
- Ensure the debug APK builds and all unit tests pass before requesting review.
- Update docs (`KNOWN_LIMITATIONS.md`, `BANGLADESH_OPERATORS.md`) when behaviour
  or evidence changes.

## Code style

- Kotlin idioms, immutable UI state, coroutines/Flow, no main-thread blocking.
- Material 3 components and the Callora design tokens only.
- Keep public APIs documented; suppress warnings only with justification.

## Reporting issues

Include device model, Android version, operator, the exact MMI string shown,
and the carrier's response. Never include other people's phone numbers.
