# Project Execution Roadmap
**Status:** Active
**Instruction for Execution Agent:** Pick the first unchecked `[ ]` task in the current active phase, implement it completely without placeholders, verify it, and mark it `[x]`.

## Phase 1: Foundation, Config & Stability Hardening
- [x] **Task 1.1: Project Initialization & Build Automation**
  - *Action:* Configure AGP 8.7.1, Kotlin 2.0.21, and GitHub Actions CI workflow.
  - *Target Files:* `build.gradle.kts`, `.github/workflows/build-apk.yml`
  - *Done When:* GitHub workflow produces `app-debug.apk`.
- [ ] **Task 1.2: Terminal Engine & Crash-Proof Shell Modernization**
  - *Action:* Eliminate Termux JNI dependency. Implement 100% crash-proof Native Linux `/system/bin/sh` process engine in `QwentonTerminalBridge.kt` and pure Jetpack Compose interactive terminal in `CollapsibleTerminalDrawer.kt`.
  - *Target Files:* `QwentonTerminalBridge.kt`, `CollapsibleTerminalDrawer.kt`, `app/build.gradle.kts`, `gradle/libs.versions.toml`
  - *Done When:* Termux JNI removed, zero `UnsatisfiedLinkError` or SELinux PTY crashes can occur.
- [ ] **Task 1.3: Universal ABI & CrashGuard Setup**
  - *Action:* Add `armeabi-v7a`, `x86_64`, `x86` to `abiFilters` and add global UncaughtExceptionHandler in `QwentonApplication.kt`.
  - *Target Files:* `QwentonApplication.kt`, `MainActivity.kt`, `app/build.gradle.kts`
  - *Done When:* Unhandled exceptions are safely intercepted.

## Phase 2: Verification & Release Deployment
- [ ] **Task 2.1: GitHub Release & CI Verification**
  - *Action:* Commit and push all stability changes to `origin main`, track GitHub Actions run, and verify generated APK.
  - *Target Files:* `.github/workflows/build-apk.yml`
  - *Done When:* GitHub workflow is green and new `app-debug.apk` is released.
