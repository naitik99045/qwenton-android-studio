# Qwenton-Studio-apex — UI/UX & Interaction Architecture Specification

## 1. Aesthetic Philosophy: Obsidian Glassmorphism
The visual design language for **Qwenton-Studio-apex** merges an IDE-grade developer console with an elegant, modern mobile interface. It avoids generic flat surfaces and harsh light contrasts in favor of deep, battery-saving dark tones optimized for OLED screens, enhanced with translucent glass layers and glowing status indicators.

### 1.1 Color Palette & Design Tokens
| Token | Hex / Value | Usage & Meaning |
| :--- | :--- | :--- |
| **Obsidian Slate** | `#0B0E14` | Main application background and terminal background |
| **Translucent Dark Cyan** | `#0F293A` (Alpha 0.75) | Glassmorphic card surfaces, modal sheets, and toolbars |
| **Border Cyan Stroke** | `#00BCD4` (Alpha 0.35) | Subtle 1dp border lines defining card edges |
| **Border Active Glow** | `#00BCD4` (Alpha 0.85) | Highlighted active states, running server borders |
| **Soft Pink (Primary)** | `#F48FB1` | Hero action buttons ("Start Server", "Download", "Send") |
| **Metallic Gold** | `#FFD54F` | Status pills, telemetry alerts, hardware metrics |
| **Success Emerald** | `#4CAF50` | Healthy server state, completed downloads, tunnel ready |
| **Alert Coral** | `#EF5350` | OOM warnings, failed processes, error states |
| **Warm Cream (Text Primary)**| `#FFF8E7` | Main headers, body text, terminal prompt markers |
| **Muted Slate (Text Secondary)**| `#9EABB8` | Subtitles, parameter labels, file size notations |

---

## 2. Screen-by-Screen Interaction Blueprint

### 2.1 Studio Dashboard (Home)
- **Top Telemetry Header:**
  - Live RAM Bar: Displays `Available / Total RAM` with a color-coded segmented progress bar (Green: `<50%`, Amber: `50-70%`, Red: `>70%`).
  - CPU & Hardware Badge: Displays active cores and architecture (`arm64-v8a • NEON+Vulkan`).
  - Foreground Service Status Badge: Pill showing `ACTIVE` (with pulsing emerald dot) or `IDLE`.
- **Hero Endpoint & API Key Card:**
  - Local LAN Endpoint: `http://192.168.x.x:8080/v1` with 1-tap copy button.
  - Public Cloudflare Base URL: `https://[subdomain].trycloudflare.com/v1` with 1-tap copy button and QR code view option.
  - Generated API Key: Masked `sk-qwenton-apex-••••••••` with "Reveal" toggle, 1-tap copy button, and "Rotate Key" button.
- **Server Control Deck:**
  - Model Selector Dropdown: Displays list of installed `.gguf` models with file size and quantization badge.
  - Context Size Slider: Granular slider from 2048 to 16384 tokens with estimated RAM impact tooltip.
  - GPU Layers Slider (`-ngl`): Offloads layers to Vulkan GPU if supported.
  - Hero Action Button:
    - State A (Idle): "Start Server" (Soft Pink gradient, elevated).
    - State B (Booting): "Starting..." (Indeterminate progress indicator).
    - State C (Running): "Stop Server" (Coral / Crimson border, active pulse).

### 2.2 Hugging Face & Agent Hub
- **Search & Filter Bar:**
  - Real-time search querying Hugging Face API (`?filter=gguf`).
  - Filter chips: `All`, `Code`, `Chat`, `Small (1-3B)`, `Mid (7-8B)`, `Custom Agents`.
- **Curated 1-Tap Mobile Models:**
  - Card carousel highlighting battle-tested mobile models:
    - *Qwen 2.5 Coder 3B Instruct* (`Q4_K_M` ~1.9 GB)
    - *Llama 3.2 3B Instruct* (`Q4_K_M` ~2.0 GB)
    - *Gemma 3 4B IT* (`Q4_0` ~2.5 GB)
    - *DeepSeek R1 Distill Qwen 1.5B* (`Q4_K_M` ~1.1 GB)
- **Download & Installation Card State:**
  - Displays file size, quantization, author, and license.
  - 1-Click "Download & Setup" button.
  - During download: Resumable progress bar, active download speed (`X.X MB/s`), percentage (`XX%`), and cancel button.
  - Post-download: Smoothly transforms to "Ready to Launch" badge.
- **Agent Manifest Store (`agent.json`):**
  - "Import Custom Agent" button to load JSON bundle containing system prompt, model repo, and Python pip dependencies.

### 2.3 Local Playground (Chat & Tool Tester)
- **Header:**
  - Model indicator with latency and token speed counter (`⚡ 24.5 t/s`).
  - System Prompt drawer button to fine-tune instructions.
- **Chat Feed:**
  - User Bubbles: Aligned right, translucent dark cyan background with soft pink accent border.
  - Assistant Bubbles: Aligned left, obsidian background with markdown parsing, inline code blocks with syntax highlighting, and "Copy Code" button.
  - Streaming response indicator with smooth scrolling.
- **Input Deck:**
  - Multi-line expanding text field with Warm Cream text.
  - Clear history button, temperature preset toggle (`Precise 0.2`, `Balanced 0.7`, `Creative 1.0`), and Send button.

### 2.4 Collapsible Live Terminal Sheet (Bottom Drawer)
- **Persistent Dock Pill (Collapsed State):**
  - Always visible at the bottom of the screen.
  - Displays: Connection status dot (`●`), active process name (`llama-server` / `curl`), and the latest line from stdout.
  - Tap or swipe up to expand.
- **Full Terminal Console (Expanded State):**
  - Embedded Termux `TerminalView` backed by `TerminalSession`.
  - Full ANSI 256-color support, monospaced font, auto-scrolling to bottom.
  - Header Action Bar:
    - "Clear Terminal" (resets buffer).
    - "Kill Process" (emergency SIGKILL for background job).
    - "Copy Output" (copies session transcript to Android clipboard).
    - "Minimize" (collapses back to dock pill).

---

## 3. Error-Proofing & Guardrail UX Patterns

1. **Pre-Flight Memory Warning Dialog:**
   If a user selects a model whose estimated RAM usage exceeds 65% of `availMem`:
   - An alert dialog appears detailing: `Selected model requires ~3.2 GB. Device currently has 2.4 GB free.`
   - Offers 1-click auto-tuning: `Auto-tune context to 2048 & enable mmap` or `Cancel`.
2. **Terminal Health Polling & Fallback:**
   When "Start Server" is tapped, the UI transitions to a waiting state while monitoring `http://127.0.0.1:8080/health`. If the server fails to respond within 30 seconds, the terminal drawer auto-expands, highlights the error log in red, and presents a "Retry" button.
3. **Network & Tunnel Reconnection:**
   If the Cloudflare quick tunnel disconnects due to mobile network handover (e.g., WiFi to 5G), the status badge changes to Amber (`Reconnecting...`) and automatically re-spawns the tunnel binary without interrupting the local `llama-server`.
4. **Zero-Keybord First Launch Experience:**
   First launch checks if `filesDir/rootfs` and binary links exist. If not, a high-contrast welcome banner prompts: `Welcome to Qwenton Studio! Tap below to initialize local AI engine.` Tapping runs the bootstrap script and smoothly unlocks the dashboard upon receiving `[QWENTON_INIT_COMPLETE]`.
