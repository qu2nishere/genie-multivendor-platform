# Hợp đồng mẫu Backend ↔ AI Worker (KYC)

Dùng chung cho test hai phía (ADR 0011 mục 5; plan 2026-11-16 AI Worker Y00.06, Mở shop & KYC D6). Dữ liệu **giả** — không phải giấy tờ thật.

| Tệp | Chiều | Kết quả backend mong đợi (bảng điểm mặc định) |
|---|---|---|
| `verify-request.json` | backend → worker `POST /kyc/verify` | worker trả `202 {kyc_id, accepted: true}` |
| `result-pass.json` | worker → backend `PATCH /api/v1/internal/kyc/{id}/result` | 0 điểm → `VERIFIED` (trừ khi bị lấy mẫu 5%) |
| `result-partial.json` | như trên | 25 + 30 = 55 → `MANUAL_REVIEW` |
| `result-fail.json` | như trên | 60 + 60 = 120 → `REJECTED` |

Đổi hợp đồng: sửa các tệp này + ADR 0011 + API_SPEC 12 trong cùng PR; test `server-python/tests/kyc/test_schemas.py` và `server-spring/.../KycContractIT` đọc trực tiếp các tệp này.
