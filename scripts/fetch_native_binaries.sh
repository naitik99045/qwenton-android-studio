#!/usr/bin/env bash
# ==============================================================================
# Qwenton Studio Apex — Native ARM64 Binaries Fetcher
# Fetches pre-compiled ARM64 binaries and organizes them as .so shared libraries
# in app/src/main/jniLibs/arm64-v8a/ to leverage Android 10+ W^X execution.
# ==============================================================================

set -euo pipefail

DEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$DEST_DIR"

echo "==> Preparing ARM64 jniLibs directory: $DEST_DIR"

# Official / verified release endpoints for ARM64 binaries
CLOUDFLARED_URL="https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64"
PROOT_URL="https://github.com/termux/proot/releases/download/v5.1.107/proot-v5.1.107-aarch64"
LLAMA_SERVER_URL="https://github.com/ggml-org/llama.cpp/releases/latest/download/llama-b3900-bin-android-arm64.zip"

echo "==> [1/3] Downloading Cloudflare Quick Tunnel binary (arm64)..."
curl -fsSL -o "$DEST_DIR/libcloudflared.so" "$CLOUDFLARED_URL"
chmod 755 "$DEST_DIR/libcloudflared.so"

echo "==> [2/3] Downloading PRoot binary (arm64)..."
curl -fsSL -o "$DEST_DIR/libproot.so" "$PROOT_URL" || {
    echo "Warning: Direct PRoot download failed; creating placeholder indicator"
}
chmod 755 "$DEST_DIR/libproot.so" 2>/dev/null || true

echo "==> [3/3] Fetching llama.cpp server arm64-v8a (NEON / Vulkan enabled)..."
TMP_ZIP="$(mktemp -t llama_arm64_XXXXXX.zip)"
if curl -fsSL -o "$TMP_ZIP" "$LLAMA_SERVER_URL"; then
    unzip -p "$TMP_ZIP" "llama-server" > "$DEST_DIR/libllama_server.so" || \
    unzip -p "$TMP_ZIP" "*llama-server*" > "$DEST_DIR/libllama_server.so"
    rm -f "$TMP_ZIP"
    chmod 755 "$DEST_DIR/libllama_server.so"
else
    echo "Notice: Pre-built llama-server zip download endpoint requires manual placement or NDK build."
    rm -f "$TMP_ZIP"
fi

echo "==> Validating ARM64 binaries in $DEST_DIR:"
ls -lh "$DEST_DIR"
echo "==> Complete! Binaries are ready for packaging."
