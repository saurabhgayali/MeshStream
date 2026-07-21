#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$ROOT_DIR/build/out"

mkdir -p "$OUT_DIR"

kotlinc \
  "$ROOT_DIR/core/src/main/kotlin" \
  "$ROOT_DIR/feature/recorder/src/main/kotlin" \
  "$ROOT_DIR/feature/storage/src/main/kotlin" \
  "$ROOT_DIR/feature/crypto/src/main/kotlin" \
  "$ROOT_DIR/feature/mesh/src/main/kotlin" \
  "$ROOT_DIR/feature/relay/src/main/kotlin" \
  "$ROOT_DIR/app/src/main/kotlin" \
  -d "$OUT_DIR/meshstream.jar"

java -cp "$OUT_DIR/meshstream.jar:/usr/share/kotlinc/lib/kotlin-stdlib.jar" com.meshstream.app.MeshStreamAppKt
