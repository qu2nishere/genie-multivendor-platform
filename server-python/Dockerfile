# Python 3.12 slim (+ JRE nếu tách từ bằng VnCoreNLP / RDRSegmenter) — TECH_STACK mục 2.5
FROM python:3.12-slim

# JRE cho VnCoreNLP — bỏ dòng này nếu dùng bản tách từ Python thuần (phải GIỐNG lúc huấn luyện)
RUN apt-get update \
    && apt-get install -y --no-install-recommends default-jre-headless libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY pyproject.toml ./
COPY app/ app/
COPY resources/ resources/
RUN pip install --no-cache-dir .

# Mô hình *.onnx: mount vào /app/models hoặc tải bằng scripts/fetch-models.sh — không commit
RUN useradd --create-home worker && mkdir -p models && chown worker models
USER worker
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
