# Project State & Context
**Last Updated:** 2026-10-25 16:03 IST

---

## 1. Project Overview
**Qwenton-Studio-apex** (`com.qwenton.studio.apex`) is a production-ready Android-native local AI model runner and autonomous agent host modeled after LM Studio. Built with **Jetpack Compose (Material3 Obsidian Glassmorphism)** and an embedded **Linux PTY Terminal (`TerminalSession`)**, it provides complete 1-click execution for GGUF LLMs and Python agents on ARM64 devices without requiring manual terminal typing.

### Structural Architecture Highlights:
- **Persistent Header Bar:** `StudioTopAppBar` with APEX branding, live server status pill (`● SERVER ACTIVE` / `IDLE`), terminal toggle, and system specs modal trigger.
- **Taskbar / Bottom Navigation Bar:** Three core destinations (Studio Dashboard, Model Hub, Playground) with glassmorphic styling and active state glow.
- **Collapsible Live Terminal Drawer:** Multi-tiered bottom dock pill displaying real-time stdout logs and token speeds, expandable to interactive Termux console.
- **System Specs & Security Modal:** `AboutSystemDialog` detailing platform ABI, llama.cpp Vulkan acceleration, W^X security bypass, and Cloudflare tunnel status.
- **Android 10–15+ W^X Bypass:** Pre-compiled ARM64 `.so` libraries in `jniLibs/arm64-v8a/` extracted with executable permissions via `android:extractNativeLibs="true"`.
- **Pre-Flight Hardware Guardrails:** Proactive `ActivityManager.MemoryInfo` evaluation dynamically auto-tuning context lengths and CPU threads.
- **Hardened Security Posture:** Fully remediated against path traversal, shell injection, pip option injection, MITM certificate spoofing, and cloud keystore leakage.

---

## 2. Full Component & Layout Status

### Header Bar & Top Navigation
- [x] **`StudioTopAppBar.kt`**: Integrated in `Scaffold(topBar = { ... })`. Features branding badge, dynamic server status indicator, terminal drawer expander, and system info dialog button. Handles `.statusBarsPadding()`.

### Footer Bar & Taskbar Navigation
- [x] **`NavigationBar` & `StudioNavigation.kt`**: Rounded glassmorphic bottom bar with curved borders, active tab highlight, and destination routing across Dashboard, Hub, and Playground.

### Floating Terminal Drawer
- [x] **`CollapsibleTerminalDrawer.kt`**: Dual-state bottom drawer. In collapsed state: persistent dock pill with live stdout stream and token speed gauge. In expanded state: Termux `TerminalView` wrapped in Compose `AndroidView` with Clear, Copy, and Kill process controls.

### Modals & Dialogs
- [x] **`AboutSystemDialog.kt`**: Interactive modal presenting hardware specs, Android SDK targets (29-35), W^X native execution status, and security audit verification badge.

### Core Screens & Clearances
- [x] **`StudioDashboardScreen.kt`**: Hardware telemetry header (RAM progress bar, CPU cores), active OpenAI endpoint & API key card, model selector dropdown, context slider, GPU layer slider, and Start/Stop server button. Clears terminal dock with 70dp bottom spacer.
- [x] **`HuggingFaceHubScreen.kt`**: Searchable GGUF hub, curated mobile presets tab, custom agent manifest tab, and live download progress bars. Clears terminal dock with 80dp bottom spacer.
- [x] **`PlaygroundScreen.kt`**: Streaming chat client hitting `http://127.0.0.1:8080/v1/chat/completions`, syntax-highlighted code blocks with 1-tap copy, token generation metrics, and out-of-bounds stream safety.

---

## 3. Pentest & Security Verification
- **Audit Posture:** `PASS (HARDENED)`
- **All 12 Vulnerability Checks:** Marked `[FIXED]` in `.agent/security_report.md`.
- **Safety Guide:** Comprehensive review available in `.agent/security_review_guide.md`.

---

## 4. Current Progress
- **Project Completeness:** **100% Complete**.
- All layout bars (Header Bar, Footer/Taskbar, Dock Pill, Terminal Drawer, Dialogs) are fully implemented and integrated.
- Zero placeholder code or missing layout components remain.
