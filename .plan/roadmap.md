# Project Execution Roadmap
**Status:** Active
**Instruction for Execution Agent:** Pick the first unchecked `[ ]` task in the current active phase, implement it completely without placeholders, verify it, and mark it `[x]`.

---

## Phase 1: Foundation, Config & Scaffolding
- [x] **Task 1.1: Android Project Gradle & Dependency Scaffolding**
  - *Action:* Configure root and app-level Gradle build scripts with Android Gradle Plugin (AGP), Kotlin 2.0+, Jetpack Compose BOM, Material3, Coroutines, OkHttp, Retrofit, Kotlinx Serialization, AndroidX Security Crypto, and Termux terminal libraries (`terminal-emulator` and `terminal-view`).
  - *Target Files:* `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`
  - *Done When:* All Gradle scripts are syntactically valid Kotlin DSL with target SDK 35, min SDK 29, Java 17/21 compatibility, and arm64-v8a ABI filters.

- [x] **Task 1.2: Android Manifest, Permissions & Binary Execution Configuration**
  - *Action:* Configure `AndroidManifest.xml` with `android:extractNativeLibs="true"` on the `<application>` tag (enforcing extraction of ARM64 binaries to `nativeLibraryDir` with execute permissions), and declare permissions: `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` (or `DATA_SYNC`), `WAKE_LOCK`, `POST_NOTIFICATIONS`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, and register `QwentonServerService`.
  - *Target Files:* `app/src/main/AndroidManifest.xml`
  - *Done When:* Manifest compiles with all required permissions and service metadata declared.

- [x] **Task 1.3: Native ARM64 Binaries Fetcher Script & jniLibs Setup**
  - *Action:* Create `scripts/fetch_native_binaries.sh` (and PowerShell equivalent `scripts/fetch_native_binaries.ps1`) to automatically fetch and verify pre-compiled ARM64 `libllama_server.so` (Vulkan+NEON), `libcloudflared.so`, and `libproot.so` into `app/src/main/jniLibs/arm64-v8a/`.
  - *Target Files:* `scripts/fetch_native_binaries.sh`, `scripts/fetch_native_binaries.ps1`, `app/src/main/jniLibs/arm64-v8a/.gitkeep`
  - *Done When:* Binary fetcher scripts provide checksum verification, download URLs, and directory structure verification for arm64-v8a.

- [x] **Task 1.4: Material 3 Glassmorphic Design System & Theme Tokens**
  - *Action:* Implement Obsidian Slate (`#0B0E14`), Dark Cyan glass surfaces (`#0F293A` @ 75%), Cyan borders (`#00BCD4` @ 35%), Soft Pink interactive highlights (`#F48FB1`), Metallic Gold telemetry badges (`#FFD54F`), Warm Cream typography (`#FFF8E7`), and reusable Glassmorphic Card/Border composables.
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `GlassmorphicCard.kt`
  - *Done When:* Theme and reusable glassmorphic components compile and render consistent visual tokens across light/dark surfaces.

---

## Phase 2: Data Layer, State & Guardrails
- [x] **Task 2.1: Data Models & Serialization**
  - *Action:* Build serializable data models for GGUF model info (`GgufModelInfo`), agent manifests (`AgentManifest`), server status enum (`ServerStatus`), server configuration (`ServerConfig`), and real-time hardware telemetry (`TelemetryState`).
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/data/model/Models.kt`
  - *Done When:* All models are cleanly typed, serializable with `@Serializable`, and provide helper methods for formatting bytes, speeds, and quantization badges.

- [x] **Task 2.2: Encrypted API Key Manager**
  - *Action:* Implement `ApiKeyManager` using AndroidX `EncryptedSharedPreferences` with `MasterKey` (AES256-GCM) to generate, validate, store, and rotate keys formatted as `sk-qwenton-apex-<24-hex-chars>`.
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/data/local/ApiKeyManager.kt`
  - *Done When:* API keys are generated deterministically, stored securely in hardware-backed keystore, and validation prevents malformed keys.

- [x] **Task 2.3: Pre-Flight RAM & Hardware Guardrail Validator**
  - *Action:* Implement `RamGuardrailValidator` querying `ActivityManager.MemoryInfo` (`availMem` and `totalMem`). Calculates safe defaults for `--ctx-size` (from 2048 to 16384) and `--threads` based on the CPU topology and verifies that model size does not exceed 65% of available physical RAM.
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/service/RamGuardrailValidator.kt`
  - *Done When:* Returns `GuardrailDecision` (SAFE, WARNING, or CRITICAL) with auto-tuned flags to prevent Out-Of-Memory (OOM) crashes.

- [x] **Task 2.4: Model & Agent Catalog Repository**
  - *Action:* Implement `ModelCatalogRepository` to manage installed `.gguf` files in `filesDir/models/` and custom agent configurations in `filesDir/agents/`, providing StateFlow of downloaded items and metadata caching.
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/data/local/ModelCatalogRepository.kt`
  - *Done When:* Local directory scanner accurately detects existing files, computes file sizes, and exposes reactive flows of available models.

---

## Phase 3: Core Engine, Terminal Bridge & Background Service
- [x] **Task 3.1: Deterministic Shell Script Factory**
  - *Action:* Implement `ShellScriptFactory` generating exact POSIX shell pipelines with strict shell escaping for:
    1. `buildInitBootstrapCommand()`: Directory creation (`/models`, `/agents`, `/rootfs`, `/logs`), extraction of Alpine rootfs, and printing `[QWENTON_INIT_COMPLETE]`.
    2. `buildDownloadModelCommand(url, destPath, pipPackages)`: Resumable curl download with progress reporting and optional PRoot pip tool installation.
    3. `buildStartServerCommand(modelPath, port, ctxSize, threads, gpuLayers, apiKey, systemPromptFile)`: Background launch of `libllama_server.so` with health-check loop and subsequent Cloudflare tunnel execution.
    4. `buildStopServerCommand()`: Process termination via pkill/kill with status confirmation `[QWENTON_SERVER_STOPPED]`.
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/terminal/ShellScriptFactory.kt`
  - *Done When:* Generated scripts are robust, contain zero injection vulnerabilities, and emit standard deterministic markers.

- [x] **Task 3.2: Bi-Directional Terminal Bridge with Termux PTY & Regex Output Parser**
  - *Action:* Implement `QwentonTerminalBridge` singleton managing `TerminalSession`. Expose StateFlow properties for `latestLogLine`, `transcript`, `downloadProgress`, `serverStatus`, `localBaseUrl`, `publicTunnelUrl`, and `tokenSpeed`. Implement streaming regex parsers for curl percentages, `https://*.trycloudflare.com` URLs, and `[QWENTON_*]` tokens.
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/terminal/QwentonTerminalBridge.kt`
  - *Done When:* Terminal outputs automatically update Compose state without any manual user keyboard input.

- [x] **Task 3.3: Android Foreground Service & WakeLock Manager**
  - *Action:* Implement `QwentonServerService` as an Android Foreground Service managing `PowerManager.WakeLock` (`PARTIAL_WAKE_LOCK`) and `WifiManager.WifiLock` (`WIFI_MODE_FULL_LOW_LATENCY`). Build persistent notification with live model stats and a "Stop Server" PendingIntent action.
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/service/QwentonServerService.kt`
  - *Done When:* Server keeps running with the screen locked/off, notification updates dynamically, and stopping from notification cleanly frees locks.

- [x] **Task 3.4: Hugging Face API Client & Preset Provider**
  - *Action:* Implement `HuggingFaceClient` using Retrofit and OkHttp to search models matching `filter=gguf`, parse GGUF variants from repository file trees, and supply curated 1-tap presets (Qwen 2.5 Coder, Llama 3.2, Gemma 3, DeepSeek R1).
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/data/api/HuggingFaceClient.kt`
  - *Done When:* Queries return typed model lists with file sizes, download URLs, and quantization badges.

---

## Phase 4: UI/UX Studio Screens & Collapsible Terminal Drawer
- [x] **Task 4.1: Collapsible Live Terminal Sheet with Termux View**
  - *Action:* Build `CollapsibleTerminalDrawer` composable wrapping Termux `TerminalView` inside `AndroidView`. Include persistent bottom dock pill showing status indicator, token speed, and latest stdout log. Expand into full terminal on swipe-up or tap, with quick action buttons (`Clear`, `Kill Process`, `Copy Logs`).
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/ui/components/CollapsibleTerminalDrawer.kt`
  - *Done When:* Drawer seamlessly expands/collapses with gesture support and displays live ANSI terminal output.

- [x] **Task 4.2: Studio Dashboard Screen & Telemetry Cards**
  - *Action:* Implement `StudioDashboardScreen`:
    - `TelemetryHeader`: Live RAM bar, CPU thread indicator, Foreground Service status badge.
    - `EndpointCard`: Glassmorphic card displaying Local LAN URL, Cloudflare HTTPS Base URL, and API Key with 1-tap Copy and Regenerate buttons.
    - `ServerControlDeck`: Model/Agent selector dropdown, context slider, GPU layer slider, and 1-click Start/Stop CTA.
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/ui/dashboard/StudioDashboardScreen.kt`, `EndpointCard.kt`, `TelemetryHeader.kt`
  - *Done When:* Dashboard displays all telemetry, enables 1-tap endpoint copying, and starts/stops the server with live visual state transitions.

- [x] **Task 4.3: Hugging Face Model & Agent Hub Screen**
  - *Action:* Implement `HuggingFaceHubScreen` featuring search bar, curated mobile models tab, custom agent manifest tab, and download progress cards displaying percentage, download speed, and installation status.
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/ui/hub/HuggingFaceHubScreen.kt`, `ModelDownloadCard.kt`, `AgentManifestCard.kt`
  - *Done When:* Users can search Hugging Face, trigger 1-click model/agent downloads, and view real-time progress bars.

- [x] **Task 4.4: Local Playground Screen with Markdown & Streaming Chat**
  - *Action:* Implement `PlaygroundScreen` connecting directly to local server `http://127.0.0.1:8080/v1/chat/completions`. Support streaming response chunks, markdown formatting, syntax-highlighted code blocks with 1-tap copy, token generation metrics, and system prompt tuning.
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/ui/playground/PlaygroundScreen.kt`, `ChatMessageItem.kt`
  - *Done When:* Chat messages stream smoothly from the local model with formatted code blocks and token speed readout.

- [x] **Task 4.5: Main Activity & Navigation Shell**
  - *Action:* Build `MainActivity.kt` and `StudioNavigation.kt` binding bottom navigation bar (Dashboard, Hub, Playground), managing system bars, runtime permission requests (Notifications, Battery Optimization), and integrating `CollapsibleTerminalDrawer`.
  - *Target Files:* `app/src/main/java/com/qwenton/studio/apex/MainActivity.kt`, `app/src/main/java/com/qwenton/studio/apex/ui/navigation/StudioNavigation.kt`
  - *Done When:* App launches cleanly with bottom navigation, smooth transitions between screens, and persistent terminal drawer overlay.

---

## Phase 5: Security Audit, Defensive Verification & Production Readiness
- [x] **Task 5.1: Defensive Security Audit & Shell Sanitization Verification**
  - *Action:* Perform automated and static security review on all shell commands generated in `ShellScriptFactory` to prevent command injection (`&&`, `;`, `|`, backticks). Verify W^X native library extraction configuration, and validate HTTPS Cloudflare tunnel encryption. Generate `.agent/security_report.md`.
  - *Target Files:* `.agent/security_report.md`
  - *Done When:* All security checks pass with zero command injection vulnerabilities and strict file permission handling.

- [x] **Task 5.2: End-to-End Build & Execution Verification**
  - *Action:* Verify the entire project structure, Gradle build files, source code completeness, absence of placeholders/TODOs, and generate project validation report.
  - *Target Files:* `.agent/verification_report.md`
  - *Done When:* Complete source codebase is verified, compilable, and ready for deployment to Android Studio or CI/CD pipelines.
