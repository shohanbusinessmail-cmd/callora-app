# Initial Repository and Environment Audit

Date: 2026-10-24

## Repository

- URL: https://github.com/shohanbusinessmail-cmd/callora-app
- Visibility: public
- Default branch: `main`
- Working branch: `arena/ce4df19c-callora-app`
- Access: ADMIN via the session GitHub token
- Starting state: one `README.md` stub, single "Initial commit", no tags, no workflows

## Local environment

- Terminal/git/gh available; authenticated as the repository owner
- **No JDK, no Android SDK, no Gradle, no Kotlin compiler, no emulator/device**
- Package installation impossible: apt mirrors outside the egress allowlist
- Egress limited to GitHub, npm and PyPI hosts; Google Maven, Gradle
  distributions and JDK distributors are unreachable from the sandbox
- Conclusion (per project mandate §3.2): local Android builds are BLOCKED;
  GitHub Actions is the authoritative build/test engine

## Mitigation

- Gradle wrapper JAR obtained through the GitHub API and verified as a valid
  archive containing `GradleWrapperMain`
- Full Android project committed and validated by the `Android CI` workflow
  (assemble, unit tests, lint, android-test compilation, APK artifact)
- CI logs are mirrored to the `ci-artifacts` branch for inspection because the
  sandbox cannot reach the Actions log storage host
