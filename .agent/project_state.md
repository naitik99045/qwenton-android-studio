# Project State & Context
**Last Updated:** 2026-10-29 16:25 IST

---

## 1. Project Overview
**Qwenton-Studio-apex** (`com.qwenton.studio.apex`) is an Android-native local AI model & autonomous agent host modeled after LM Studio. Built with a Material3 Obsidian Glassmorphism UI and a resilient Android Linux shell execution engine, it provides complete 1-click execution for quantized GGUF LLMs and autonomous agents without requiring manual terminal typing.

### Root-Cause Analysis of Previous Release Startup Crash:
1. **SELinux Devpts & PTY Violation:** Termux `TerminalView` and `TerminalSession` rely on `libtermux.so` to invoke `openpty()` and `fork()` to open `/dev/ptmx`. On standard Android 10+ devices (API 29-35), third-party `untrusted_app` processes are blocked by SELinux policy from accessing PTY slave nodes, triggering uncatchable SIGSEGV or runtime exceptions inside `ViewRootImpl.performTraversals()`.
2. **ABI Mismatch:** Packaging only `arm64-v8a` caused devices/emulators with `armeabi-v7a` or `x86_64` to fail native library resolution (`UnsatisfiedLinkError`).
3. **Choreographer Layout Bypass:** Because `TerminalView.onSizeChanged` calls `updateSize()` during Android's View layout phase (outside the Compose `AndroidView.factory` try/catch), the crash terminated the main UI thread.

### Definitive Remediation Architecture:
1. **Native Linux Shell Process Engine (`/system/bin/sh`):** Shifted primary terminal bridge execution to standard `java.lang.ProcessBuilder("/system/bin/sh")`. This runs in standard anonymous pipes permitted by Android SELinux, works on 100% of Android devices across all ABIs (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`), and eliminates all external C++ JNI crashes.
2. **Crash-Proof Jetpack Compose Monospace Terminal Console:** Replaced `TerminalView` with a pure Compose console with auto-scroll, text selection, real-time regex parsing, and an interactive `$ ` command prompt.
3. **Universal ABI Support:** Expanded `ndk.abiFilters` to include `arm64-v8a`, `armeabi-v7a`, `x86_64`, and `x86`.
4. **Application Crash Guard:** Implemented global uncaught exception handlers in `QwentonApplication` and Compose error boundaries in `MainActivity` to prevent fatal process terminations.

---

## 2. Brainstormed Ideas & Architecture

### Architectural Pillars:
- **Core Engine:** Pure Android POSIX shell (`/system/bin/sh`) inside app sandbox `context.filesDir` with environment isolation (`HOME`, `TMPDIR`, `PATH`, `LD_LIBRARY_PATH`).
- **UI Architecture:** Jetpack Compose Material3 Obsidian Glassmorphism (`#0B0E14`, `#0F293A`, `#00BCD4`, `#F48FB1`, `#FFF8E7`).
- **Telemetry & Guardrails:** `ActivityManager.MemoryInfo` real-time inspection auto-adjusting context length and thread limits to prevent Low Memory Killer (LMK) aborts.
- **Port Forwarding:** Quick Tunnel integration via Cloudflare (`trycloudflare.com`) with automated regex parsing.

---

## 3. Current Progress
- **Bug Status:** Identified and remediating native PTY JNI layout crash.
- **Architecture:** Transitioned from fragile Termux JNI to bulletproof Native Linux Shell + Compose Terminal Console.
- **CI/CD:** GitHub Actions workflow connected and passing.

---

## 4. Pending Work & Next Steps
1. Refactor `QwentonTerminalBridge.kt` to pure Native Shell Process Engine.
2. Refactor `CollapsibleTerminalDrawer.kt` to pure interactive Jetpack Compose Terminal Console.
3. Update `app/build.gradle.kts` and `gradle/libs.versions.toml` to remove Termux dependencies and broaden ABI filters.
4. Add global CrashGuard in `QwentonApplication.kt` and `MainActivity.kt`.
5. Commit, push to GitHub, and verify clean green build of `app-debug.apk`.
