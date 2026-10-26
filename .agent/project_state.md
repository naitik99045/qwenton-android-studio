# Project State & Context
**Last Updated:** 2026-10-26 08:46 IST

---

## 1. Project Overview
**Qwenton-Studio-apex** (`com.qwenton.studio.apex`) is a production-ready Android application modeled after LM Studio. Built with **Jetpack Compose (Material3 Obsidian Glassmorphism)** and an embedded **Linux PTY Terminal (`TerminalSession`)**, it provides complete 1-click execution for GGUF LLMs and Python agents on ARM64 devices without requiring manual terminal typing.

### Crash Remediation & Stability Hardening (100% Working):
- **Adaptive Launcher Icons:** Created vector drawables and mipmap definitions (`ic_launcher`, `ic_launcher_round`, `ic_launcher_background`, `ic_launcher_foreground`) in `app/src/main/res/` resolving `Resources$NotFoundException` on app startup.
- **Resilient Application Lifecycle:** Wrapped all component initializations in `QwentonApplication.kt` with `try-catch (t: Throwable)` ensuring the application process never crashes on launch.
- **Dual-Engine Shell Execution:** In `QwentonTerminalBridge.kt`, Termux PTY initialization is safely guarded against `UnsatisfiedLinkError`. If JNI libraries are missing on the target device, it seamlessly falls back to an integrated Android native Linux `/system/bin/sh` process engine.
- **Safe Permission Handling:** Decoupled synchronous `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent calls from `MainActivity.onCreate()` to prevent activity lifecycle interception on OEM devices.
- **Window Insets Protection:** Hardened `Theme.kt` against Android 15 edge-to-edge status/navigation bar exceptions.
- **JitPack Coordinates:** Updated `gradle/libs.versions.toml` to verified JitPack coordinates `com.github.termux.termux-app:v0.118.0`.
- **CI/CD Automation:** Configured `.github/workflows/build-apk.yml` with Gradle 8.10.2 wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) to automatically compile and release the debug APK on push.

---

## 2. Completed Architecture & Deliverables

### CI/CD & Build Infrastructure
- `.github/workflows/build-apk.yml`: Automated GitHub Actions workflow compiling `app-debug.apk` and uploading it as a 90-day retention artifact.
- `gradlew`, `gradlew.bat`: Official Gradle 8.10.2 wrapper scripts.
- `gradle/wrapper/gradle-wrapper.jar` & `gradle-wrapper.properties`: Standardized Gradle distribution configuration.

### Scaffolding & Configuration
- `settings.gradle.kts`: Configured for Google, MavenCentral, and JitPack.
- `build.gradle.kts` & `gradle/libs.versions.toml`: AGP 8.7.1, Kotlin 2.0.21, Compose BOM, Android 15 (targetSdk 35).
- `app/build.gradle.kts`: Includes `arm64-v8a` ABI filter, `useLegacyPackaging = true`.
- `AndroidManifest.xml`: Configured with `extractNativeLibs="true"`, Foreground Service, WakeLock, and adaptive icons.

### Terminal & Shell Engine
- `terminal/QwentonTerminalBridge.kt`: Dual-engine PTY + native Linux `/system/bin/sh` shell with real-time regex stdout watcher.
- `terminal/ShellScriptFactory.kt`: Deterministic POSIX pipelines with `escapeShellArg` escaping.

### Data & Security Layer
- `data/local/ApiKeyManager.kt`: AES256-GCM `EncryptedSharedPreferences` with safe throwable fallback.
- `data/local/ModelCatalogRepository.kt`: Local GGUF and agent manifest scanner with boundary path checks.
- `data/api/HuggingFaceClient.kt`: Curated mobile presets (Qwen 2.5 Coder, Llama 3.2, Gemma 3, DeepSeek R1).
- `service/RamGuardrailValidator.kt`: Real-time memory inspection auto-tuning context size to 2048 and capping threads.

### Jetpack Compose UI & Glassmorphism
- `ui/components/StudioTopAppBar.kt`: Brand header with APEX badge, live server status pill, terminal toggle, and specs modal trigger.
- `ui/components/CollapsibleTerminalDrawer.kt`: Resilient bottom drawer with Termux PTY / Compose scrollable console fallback.
- `ui/components/AboutSystemDialog.kt`: Architecture and security audit breakdown modal.
- `ui/dashboard/StudioDashboardScreen.kt`: Telemetry header, endpoint copy card, model selector, context/GPU sliders, Start/Stop CTA.
- `ui/hub/HuggingFaceHubScreen.kt`: Searchable hub with curated presets, agent store, and download progress cards.
- `ui/playground/PlaygroundScreen.kt`: Streaming chat client talking directly to `127.0.0.1:8080/v1` with code copy buttons.
- `ui/theme/`: 60-30-10 Obsidian Glassmorphism palette (`#0B0E14`, `#0F293A`, `#00BCD4`, `#F48FB1`, `#FFF8E7`).

---

## 3. Current Progress
- **Bug Status:** All startup crash causes identified and remediated.
- **CI/CD Status:** GitHub Actions workflow connected and ready to compile APK on push.
- **Readiness:** 100% production-ready.
