#!/usr/bin/env bash
# Tải mô hình ONNX KYC (YuNet, SFace) + PhoBERT đúng phiên bản vào server-python/models/
# Nguồn: bucket MinIO ml-models hoặc GitHub Release — *.onnx KHÔNG commit (TECH_STACK mục 2.2)
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
DEST=${MODELS_DIR:-$ROOT/server-python/models}
MODEL_VERSION=${MODEL_VERSION:-v1}
# Ví dụ: https://github.com/<org>/genie-platform/releases/download/models-v1
BASE_URL=${MODELS_BASE_URL:?Đặt MODELS_BASE_URL (GitHub Release hoặc URL MinIO ml-models)}

FILES=(
  "face_detection_yunet_2023mar.onnx"
  "face_recognition_sface_2021dec.onnx"
  "phobert-sentiment-$MODEL_VERSION-int8.onnx"
)

mkdir -p "$DEST"
for f in "${FILES[@]}"; do
  if [ -f "$DEST/$f" ]; then
    echo "Đã có $f"
  else
    echo "Tải $f ..."
    curl -fL --retry 3 -o "$DEST/$f" "$BASE_URL/$f"
  fi
done
# TODO: kiểm SHA-256 từng file với checksums.txt của bản phát hành
echo "Xong → $DEST"
