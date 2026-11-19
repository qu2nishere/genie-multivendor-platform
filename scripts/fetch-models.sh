#!/usr/bin/env bash
# Tải mô hình ONNX KYC (YuNet, SFace) + PhoBERT đúng phiên bản vào server-python/models/ và KIỂM SHA-256
# (scripts/models.sha256 — plan AI Worker Y08.01). *.onnx KHÔNG commit (TECH_STACK mục 2.2).
#
#   MODELS_BASE_URL   bản phát hành nội bộ (GitHub Release / Silo ml-models) chứa cả 3 tệp — ưu tiên nếu đặt
#   KYC_MODELS_BASE_URL nguồn mặc định của YuNet / SFace khi không đặt MODELS_BASE_URL: opencv_zoo (Apache-2.0)
#   SKIP_PHOBERT=1    bỏ qua mô hình PhoBERT (chưa có bản phát hành — plan cảm xúc đánh giá)
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
DEST=${MODELS_DIR:-$ROOT/server-python/models}
MODEL_VERSION=${MODEL_VERSION:-v1}
SUMS="$ROOT/scripts/models.sha256"
ZOO=${KYC_MODELS_BASE_URL:-https://media.githubusercontent.com/media/opencv/opencv_zoo/main/models}

declare -A ZOO_PATH=(
  ["face_detection_yunet_2023mar.onnx"]="face_detection_yunet"
  ["face_recognition_sface_2021dec.onnx"]="face_recognition_sface"
)
FILES=("face_detection_yunet_2023mar.onnx" "face_recognition_sface_2021dec.onnx")
if [ "${SKIP_PHOBERT:-0}" != "1" ]; then
  FILES+=("phobert-sentiment-$MODEL_VERSION-int8.onnx")
fi

sha256() { if command -v sha256sum >/dev/null; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi; }

verify() {
  local f=$1 expected
  expected=$(awk -v n="$f" '$2 == n { print $1 }' "$SUMS")
  if [ -z "$expected" ]; then
    echo "⚠ $f chưa có trong models.sha256 — bỏ qua kiểm" >&2
    return 0
  fi
  if [ "$(sha256 "$DEST/$f")" != "$expected" ]; then
    echo "✗ SHA-256 của $f không khớp — xóa tệp" >&2
    rm -f "$DEST/$f"
    exit 1
  fi
  echo "✓ $f"
}

mkdir -p "$DEST"
for f in "${FILES[@]}"; do
  if [ ! -f "$DEST/$f" ]; then
    if [ -n "${MODELS_BASE_URL:-}" ]; then
      url="$MODELS_BASE_URL/$f"
    elif [ -n "${ZOO_PATH[$f]:-}" ]; then
      url="$ZOO/${ZOO_PATH[$f]}/$f"
    else
      echo "Đặt MODELS_BASE_URL để tải $f (hoặc SKIP_PHOBERT=1)" >&2
      exit 1
    fi
    echo "Tải $f ..."
    curl -fL --retry 3 -o "$DEST/$f" "$url"
  fi
  verify "$f"
done
echo "Xong → $DEST"
