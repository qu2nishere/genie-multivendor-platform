# PROGRESS — Chuỗi C8 Trả hàng & hoàn tiền (UC-SUP-01 · UC-SUP-02 · UC-SUP-03 · UC-SUP-04)

| | |
|---|---|
| **Plan** | [plan_tra_hang_hoan_tien_c8.md](./plan_tra_hang_hoan_tien_c8.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-28 |
| **Use case** | Tạo yêu cầu Trả hàng / Hoàn tiền (UC-SUP-01) · Người bán tiếp nhận và xử lý yêu cầu trả hàng (UC-SUP-02) · Nhân viên sàn phân xử tranh chấp khiếu nại (UC-SUP-03) · Thực hiện hoàn tiền — VNPay / VietQR / COD (UC-SUP-04) |
| **Tổng** | 45 task + 13 commit, 12 giai đoạn |
| **Nhánh** | `feature/c8-refund-requests` (U00–U04, U07–U08) → `feature/c8-refund-execution` (U05) → `feature/c8-disputes` (U06, U09–U11) |
| **Phụ trách** | _(điền kết quả `git config user.name` ở task U00.01 — tên này dùng khi tick cột Phụ trách ở checklist / báo cáo / Excel)_ |
| **Người commit** | danh tính git đã cấu hình trên máy người làm — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Cập nhật gần nhất** | 2026-11-28 — lập plan, chưa bắt đầu (0 / 58) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Điều kiện tạo yêu cầu** | ☐ | |
| D2 | **Đầu vào** (API 7.5 🔁) | ☐ | |
| D3 | **Số tiền từng món** | ☐ | |
| D4 | **Bằng chứng** | ☐ | |
| D5 | **Khi tạo** (UC-SUP-01 b7) | ☐ | |
| D6 | **Shop xử lý** (API 10.4) | ☐ | |
| D7 | **Vận đơn chiều về** | ☐ | |
| D8 | **Hạn chót** (cron 15 phút) | ☐ | |
| D9 | **Tài khoản nhận hoàn & mã chuyển khoản** | ☐ | |
| D10 | **Thực hiện hoàn — VNPay** (UC-SUP-04 b1) | ☐ | |
| D11 | **Thực hiện hoàn — VietQR / COD** | ☐ | |
| D12 | **Ghi nhận hoàn xong** (một transaction) | ☐ | |
| D13 | **Phong tỏa ký quỹ** (API 11.4) | ☐ | |
| D14 | **Phân xử** (API 11.4) | ☐ | |
| D15 | **Khách sau khi shop từ chối** (API 7.7, 7.8) | ☐ | |
| D16 | **Bổ sung bằng chứng** (API 7.6) | ☐ | |
| D17 | **Cài cổng `CancellationRefunds`** (C4) | ☐ | |
| D18 | **Hoàn chuyển khoản muộn** (C3 D10 `LATE_PAYMENT_REFUND`) | ☐ | |
| D19 | **Danh sách của khách** | ☐ | |
| D20 | **Thông báo** | ☐ | |
| D21 | **Cờ web** | ☐ | |
| D22 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| U00 | Chuẩn bị | 6 | 0 | 0 / 6 |
| U01 | Tài liệu trước | 3 | 1 | 0 / 4 |
| U02 | Migration & đồng bộ trạng thái | 4 | 2 | 0 / 6 |
| U03 | Khách tạo yêu cầu (UC-SUP-01) | 4 | 1 | 0 / 5 |
| U04 | Shop xử lý & hạn chót (UC-SUP-02) | 5 | 1 | 0 / 6 |
| U05 | Thực hiện hoàn tiền (UC-SUP-04) — nhánh `feature/c8-refund-execution` | 5 | 2 | 0 / 7 |
| U06 | Sàn phân xử (UC-SUP-03) — nhánh `feature/c8-disputes` | 3 | 1 | 0 / 4 |
| U07 | Nối web phía khách (cờ `refunds`) | 1 | 1 | 0 / 2 |
| U08 | Nối web Seller › Trả hàng (cờ `seller-refunds`) | 1 | 1 | 0 / 2 |
| U09 | Nối web Admin › Trả hàng & Hoàn tiền (cờ `admin-disputes`) | 2 | 1 | 0 / 3 |
| U10 | Kiểm thử & chạy thật | 3 | 1 | 0 / 4 |
| U11 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **45** | **13** | **0 / 58** |

## U00 — Chuẩn bị

- [ ] **U00.01** Chạy `git config user.name` / `user.email`
- [ ] **U00.02** Chốt D1–D22 vào progress.
- [ ] **U00.03** Tạo nhánh `feature/c8-refund-requests` từ `develop`.
- [ ] **U00.04** Đọc bảng đồng bộ ENTITY 8.1 + ví dụ số (chép thành bảng ca test), CHECK `refund_requests.status`, `resolution_mode`, `payment_refunds.status`; quyền `HANDLE_DISPUTE` (CS Specialist) có trong preset nào; `AesGcmCipher` (KYC) để mã hóa TK nhận hoàn.
- [ ] **U00.05** Tài liệu API refund VNPay sandbox (thứ tự trường băm, `vnp_RequestId`, mã phản hồi)
- [ ] **U00.06** Đọc màn mock: Chi tiết đơn → "Yêu cầu Trả hàng / Hoàn tiền" (chọn món, lý do, tải ảnh / video, TK nhận hoàn), Tài khoản › Trả hàng, Seller › Trả hàng (4 phương án + hạn), Admin › Trả hàng & Hoàn tiền (bằng chứng hai bên, phán quyết, trạng thái hoàn Gateway, phong tỏa).

## U01 — Tài liệu trước

- [ ] **U01.01** ENTITY 8.1 (bảng đồng bộ cập nhật D5, D8, D12, D14; cột D9), 8.2 (công thức D3), 8.3 (`uploaded_by_role` D4), 6.3 (`status_before_freeze` D13), 6.7 (`CARRIER_CLAIM_RECEIVABLE`), Phụ lục A (3 `AuditAction`, `RefundReasonCode`), Phụ lục E.
- [ ] **U01.02** API_SPEC
- [ ] **U01.03** Usecase.md
- [ ] **U01.C1** 🔖 COMMIT `docs(support): specify return requests, seller handling, disputes and refund execution` · hash: `…`

## U02 — Migration & đồng bộ trạng thái

- [ ] **U02.01** `V32__c8_refund_dispute.sql` (D9, D4 `uploaded_by_role`, D13, 3 `AuditAction`).
- [ ] **U02.02** `RefundAmounts` (D3, N2)
- [ ] **U02.03** `ErrorCode`, `AuditAction`, `UploadPurpose.REFUND_EVIDENCE`, `RefundReasonCode`.
- [ ] **U02.04** Test
- [ ] **U02.C1** 🔖 COMMIT `feat(db): add refund account snapshot, refund codes and escrow freeze memory` · hash: `…`
- [ ] **U02.C2** 🔖 COMMIT `feat(support): keep refund, order and escrow states in sync` · hash: `…`

## U03 — Khách tạo yêu cầu (UC-SUP-01)

- [ ] **U03.01** `RefundRequestService.create` (D1–D5, D9) + `MeRefundController` `POST /me/refund-requests` 🔁, `GET` danh sách / chi tiết (D19).
- [ ] **U03.02** `…/evidence` (D16), `…/escalate`, `…/accept-rejection` (D15).
- [ ] **U03.03** `RefundEvidenceViewer` (N6)
- [ ] **U03.04** Test
- [ ] **U03.C1** 🔖 COMMIT `feat(support): let buyers open return and refund requests with evidence` · hash: `…`

## U04 — Shop xử lý & hạn chót (UC-SUP-02)

- [ ] **U04.01** `SellerRefundService` + `SellerRefundController` (D6, D4 bằng chứng của shop).
- [ ] **U04.02** Vận đơn chiều về + `ReturnShipmentListener` (D7).
- [ ] **U04.03** `RefundDeadlineJob` (D8, N5).
- [ ] **U04.04** Thông báo (D20).
- [ ] **U04.05** Test
- [ ] **U04.C1** 🔖 COMMIT `feat(support): let sellers handle return requests with deadlines and return shipments` · hash: `…`

## U05 — Thực hiện hoàn tiền (UC-SUP-04) — nhánh `feature/c8-refund-execution`

- [ ] **U05.01** `VnpayRefundSignature` + `VnpayRefundClient` (D10; `stub` / `http`).
- [ ] **U05.02** `RefundExecutionService.execute` (N4, D10, D11) + D12 trong một transaction (tồn, Điểm, sổ cái, ví nếu đã giải ngân, audit).
- [ ] **U05.03** `RefundTransferMatcher` (D11) đăng ký vào `BankTransferRouter`; `AdminRefundController` (`sync`, `retry` 🔁, `mark-transferred`).
- [ ] **U05.04** `SupportCancellationRefunds` (D17) thay bản mặc định của C4.
- [ ] **U05.05** Test
- [ ] **U05.C1** 🔖 COMMIT `feat(payment): refund vnpay transactions through the refund api` · hash: `…`
- [ ] **U05.C2** 🔖 COMMIT `feat(support): execute approved refunds and confirm manual transfers through sepay` · hash: `…`

## U06 — Sàn phân xử (UC-SUP-03) — nhánh `feature/c8-disputes`

- [ ] **U06.01** `DisputeService` + `AdminDisputeController` (D14)
- [ ] **U06.02** `EscrowFreezeService` (D13)
- [ ] **U06.03** Test
- [ ] **U06.C1** 🔖 COMMIT `feat(support): let staff arbitrate disputes and freeze escrows` · hash: `…`

## U07 — Nối web phía khách (cờ `refunds`)

- [ ] **U07.01** `refund.api.ts` (5 hàm + danh sách / chi tiết)
- [ ] **U07.C1** 🔖 COMMIT `feat(web): connect buyer return and refund requests to the real api` · hash: `…`

## U08 — Nối web Seller › Trả hàng (cờ `seller-refunds`)

- [ ] **U08.01** `seller-orders/refund.api.ts` (`listSellerRefunds`, `actOnRefund` + bằng chứng của shop)
- [ ] **U08.C1** 🔖 COMMIT `feat(seller-ui): connect seller return handling to the real api` · hash: `…`

## U09 — Nối web Admin › Trả hàng & Hoàn tiền (cờ `admin-disputes`)

- [ ] **U09.01** `admin-disputes/dispute.api.ts` (9 hàm)
- [ ] **U09.02** vi / en + `error-codes.ts` cho U07–U09; test mapper; kiểm 9 theme, 360 px; `npm run verify`.
- [ ] **U09.C1** 🔖 COMMIT `feat(admin-ui): connect dispute arbitration and refund operations to the real api` · hash: `…`

## U10 — Kiểm thử & chạy thật

- [ ] **U10.01** `docs/api-tests/refunds.http`
- [ ] **U10.02** `docs/kiem-thu/kich-ban-2026-11-28-c8.md`
- [ ] **U10.03** Chạy thật
- [ ] **U10.C1** 🔖 COMMIT `docs(support): add refund api tests and real-run scenarios` · hash: `…`

## U11 — Hoàn thiện use case & tick tiến độ

- [ ] **U11.01** OpenAPI
- [ ] **U11.02** `docs/web-backend-handoff.md`
- [ ] **U11.03** Usecase.md
- [ ] **U11.04** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **U11.05** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **U11.06** `Quản Lý UseCase.xlsx`
- [ ] **U11.07** README ngày (mục 7)
- [ ] **U11.C1** 🔖 COMMIT `docs(support): record returns, disputes and refunds as implemented` · hash: `…`
- [ ] **U11.08** Mở 3 PR xếp chồng vào `develop` (`c8-refund-requests` → `c8-refund-execution` → `c8-disputes`).

## Ghi chú

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit / PR |
|---|---|---|
| 2026-11-28 | Lập plan + progress (45 task, 13 commit, 22 quyết định chờ chốt) | nhánh `feature/plan-c3-c11` |
