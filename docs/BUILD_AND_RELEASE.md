# Build and Release

## Toolchain

| Component | Version |
| --- | --- |
| JDK | 17 (Temurin) |
| Gradle | 8.9 (wrapper) |
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.7.3 |
| KSP | 2.0.21-1.0.28 |
| Compose BOM | 2024.10.01 |
| compile/target SDK | 35 |
| min SDK | 26 |
| Application ID | `com.callora.app` (debug suffix: `.debug`) |
| Version | `0.1.0` (code 1) |

## Build commands

```bash
./gradlew assembleDebug                 # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest             # JVM + Robolectric tests
./gradlew lintDebug                     # SARIF + HTML reports under app/build/reports/
./gradlew assembleDebugAndroidTest      # compile UI tests
```

The development sandbox used for this project has no Android SDK and restricted
network access, so all builds execute in GitHub Actions
(`.github/workflows/android-ci.yml`). The wrapper JAR and scripts are committed;
the wrapper distribution is downloaded by Gradle on first run.

## Signing

No signing material is committed. Debug builds use the standard debug keystore
on the CI runner and are clearly labelled `-debug`. For a production build the
owner must configure, outside source control:

1. A dedicated upload keystore stored securely.
2. `keystore.properties` (git-ignored) or CI secrets: `CALLORA_KEYSTORE`,
   `CALLORA_KEY_ALIAS`, `CALLORA_STORE_PASSWORD`, `CALLORA_KEY_PASSWORD`.
3. A `signingConfigs.release` block reading those values.

Until that exists, every artifact is a debug APK and must not be represented as
a production-signed release.

## Release process

1. Update `versionName`/`versionCode` in `app/build.gradle.kts` and `CHANGELOG.md`.
2. Push the tag: `git tag v0.1.0 && git push origin v0.1.0`.
3. `release.yml` checks out the tag, builds and tests, then runs
   `gh release create` with the APK attached and `docs/release-notes.md` as notes.
4. Verify the release URL and that the asset is downloadable before announcing.

## Release acceptance checklist

- [x] Unit tests and lint green in CI
- [x] Debug APK produced by Gradle (not renamed or fabricated)
- [x] Manifest permissions reviewed; no secrets in the tree
- [x] Documentation matches implementation and verification status
- [ ] Production signing configured (owner action)
- [ ] Device/carrier verification (owner/hardware action)
