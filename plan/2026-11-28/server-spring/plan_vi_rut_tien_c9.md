# PLAN — Chuỗi C9 Ví & rút tiền (UC-VND-20 · UC-ADM-04 · UC-ADM-05)

| | |
|---|---|
| **Ngày lập** | 2026-11-28 |
| **Thể loại** | server-spring (module `payment` feature `wallet` — cả API của Seller **và** Admin; cài cổng `PayoutAccountGuard` của vendor) + nối `web/` (feature `seller-wallet`, `admin-finance` phần rút tiền) |
| **Use case** | Xem ví & sổ giao dịch người bán (UC-VND-20) · Người bán gửi yêu cầu rút tiền từ Ví về Ngân hàng (UC-ADM-04) · Nhân viên sàn phê duyệt & Thực hiện lệnh chuyển khoản — Payout (UC-ADM-05) |
| **Tài liệu chuẩn** | UC phân hệ 05 (UC-VND-20 dòng 1696), 11 (UC-ADM-04 dòng 2628, 05 dòng 2659) · API 10.10 (ví, sổ giao dịch, CSV, `POST /seller/withdrawals` 🔁 🔐), 11.6 (xử lý rút tiền), 12.1 (SePay tiền ra), 15 (`GNE-WAL-*`) · ENTITY 6.3, 6.4 `VendorWallet` (`availableBalance = balance − pendingWithdrawal`, `holdForWithdrawal`, `settleWithdrawal`), 6.5, 6.6 `WithdrawalRequest`, 6.7, 1.4 (`isPayoutAccountVerified`) · TS 14 (rút song song vượt số dư) · tham số: rút tối thiểu 100.000₫ — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_vi_rut_tien_c9.md](progress_vi_rut_tien_c9.md) — cùng mã task `Ixx.yy` |
| **Phụ trách** | Người làm plan — lấy theo **`git config user.name`** trên máy thực hiện (ghi vào progress ở I00.01); không ghi tên cố định |
| **Người commit** | danh tính git đã cấu hình trên máy đó — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Phụ thuộc** | **C3** (`WalletPoster`, `WalletHasher`, `LedgerPoster`, ví có tiền từ `EscrowReleaseJob`, `bank_transactions` + `BankTransferRouter`). B4 (tài khoản nhận tiền đã xác minh — UC-VND-07, `PayoutAccountGuard` chờ cài). Xác thực bước cao 🔐 (`purpose = WITHDRAWAL`) của AUTH. Kiểm số liệu đầy đủ cần đơn hoàn tất (C5) — trước đó dựng ví bằng giải ngân thật từ đơn dựng tay |
| **Mục tiêu** | 3 use case ⬜ → ✅. Shop xem ví minh bạch + CSV; tạo lệnh rút (OTP) giữ tiền; Finance Officer duyệt, chuyển khoản theo `TRWD{id}`, SePay xác nhận tự hoàn tất (hoặc xác nhận tay); sổ cái + hash ví luôn đúng |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `1b66eae`, 2026-11-28)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Entity** | `VendorWallet.availableBalance()` = `balance − pending_withdrawal`; `holdForWithdrawal(amount)` (chỉ tăng `pending`, chặn khi không đủ — `GNE-WAL-4001`); `settleWithdrawal(amount, approved)` (giảm `pending`, `approved` → giảm `balance`); `debit` / `credit`; `verifyHash`. `WithdrawalRequest.approve / startProcessing / complete / reject`; bảng `withdrawal_requests` (`wallet_id`, `amount`, `bank_*`, `status`, `rejection_reason`, `processed_by_admin_id`, `wallet_transaction_id`, `requested_at`, `processed_at`) | ⚠ Không có mã chuyển khoản `TRWD{id}`, mã giao dịch ngân hàng, thời điểm / người duyệt → migration (D8). ⚠ UC-ADM-04 b5 ghi "trừ `balance` + cộng `pending`" khi tạo lệnh — **lệch** entity (D2) |
| **Vendor** | `PayoutAccountGuard` (cổng: chặn xóa tài khoản nhận tiền khi còn lệnh rút — `GNE-WAL-4091`) **chưa ai cài**; `VendorProfile.isPayoutAccountVerified()`; `KycGateInterceptor` | Cài cổng (D10) |
| **Phụ thuộc module** | `payment → vendor` (C3) | **`admin` không import `payment`** (vòng `payment → vendor → admin`) → controller Admin rút tiền đặt trong **`payment/wallet/api`** |
| **Quyền** | `PROCESS_PAYOUT`, `VIEW_FINANCE` (`StaffPermissionEnum`) | — |
| **Mã lỗi** | — | `GNE-WAL-4001`, `4221`, `4032`, `4092`, `4091` (API 15); **mới** `GNE-WAL-4093` (đã có lệnh rút đang chờ — D4), `GNE-WAL-4094` (lệnh không ở bước cho phép) |
| **Web** | `seller-wallet/wallet.api.ts` (`getWallet`, `listTransactions`, `createWithdrawal`), `admin-finance/finance.api.ts` (`listWithdrawals`, `approveWithdrawal`, `getWithdrawalTransfer`, `rejectWithdrawal`, `completeWithdrawal`) — mock | Nối theo cờ (D14) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Mọi thay đổi ví qua `WalletPoster` (C3): `SELECT … FOR UPDATE` + `@Version` + `balance_hash` HMAC + một dòng `wallet_transactions` mỗi lần **số dư** đổi | README mục 4; TS 10 |
| N2 | Chỉ chuyển tới tài khoản **đã xác minh chính chủ**; thông tin TK chụp vào lệnh rút lúc tạo (đổi TK sau không ảnh hưởng lệnh cũ) | UC-ADM-05 ràng buộc |
| N3 | SePay **không** chi tiền — chỉ xác nhận tiền đã ra; khớp `TRWD{id}` + số tiền mới hoàn tất; lệch → không tự hoàn tất | UC-ADM-05 b4 |
| N4 | `admin` không import `payment` — API Admin rút tiền nằm trong module `payment` | README mục 3 |
| N5 | Tạo lệnh rút cần xác thực bước cao 🔐 `purpose = WITHDRAWAL` + Idempotency-Key 🔁 | API 10.10 |
| N6 | Sổ giao dịch ví **chỉ đọc**; số liệu gộp từ `escrow_ledgers` + `wallet_transactions` bằng read-model | UC-VND-20 ràng buộc |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Khối tổng quan ví** (API 10.10 `GET /seller/wallet`) | UC-VND-20 b2–3 | `available_balance` (`availableBalance()`), `escrow_held` (Σ `net_amount` ký quỹ `HELD` / `DISPUTED` / `FROZEN` của shop), `cod_pending` (Σ `net_amount` `PENDING_FUNDING`), `pending_withdrawal`, `revenue_this_month` + `revenue_change_percent` (Σ `gross_amount` ký quỹ của đơn `COMPLETED` theo `completed_at` giờ VN, so tháng trước), `commission_example` (đơn hoàn tất gần nhất: gross, tỷ lệ theo ngành, hoa hồng, net), `payout_account` (ngân hàng, số TK che giữa, đã xác minh?), `can_withdraw` + lý do. `?month=` cho doanh thu kỳ khác | ☐ |
| D2 | **Khi nào trừ số dư** — UC-ADM-04 b5 ("trừ `balance`, cộng `pending`") lệch entity (`pending` chỉ giữ chỗ; `availableBalance = balance − pending`) | ENTITY 6.4 | Theo **entity**: tạo lệnh → `holdForWithdrawal` (`pending += amount`, `balance` giữ nguyên — khả dụng giảm), **không** ghi `wallet_transactions` (số dư chưa đổi) nhưng sổ giao dịch hiện lệnh "Đang rút"; hoàn tất → `settleWithdrawal(amount, true)` (`balance −= amount`, `pending −= amount`) + `wallet_transactions` `DEBIT_WITHDRAWAL` (gắn `withdrawal_requests.wallet_transaction_id`); từ chối → `settleWithdrawal(amount, false)` (`pending −= amount`). Sửa câu UC-ADM-04 b5, UC-ADM-05 b5 | ☐ |
| D3 | **Điều kiện tạo lệnh** | UC-ADM-04 tiền điều kiện; API 15 | Shop `VERIFIED`; tài khoản nhận tiền đã xác minh (`403 GNE-WAL-4032` + `details.next = /seller/settings/payout`); `amount` nguyên VND ≥ 100.000 (`422 GNE-WAL-4221`, cấu hình `genie.wallet.min-withdrawal=100000`); ≤ khả dụng (`400 GNE-WAL-4001`); hash ví hợp lệ (lệch → `409 GNE-WAL-4094` "ví đang bị khóa kiểm tra" + AdminNotification) | ☐ |
| D4 | **Một lệnh chờ tại một thời điểm** | UC không ghi; chống spam | Tối đa **một** lệnh `PENDING` / `APPROVED` / `PROCESSING` mỗi ví → `409 GNE-WAL-4093` (mới). Ghi UC | ☐ |
| D5 | **Xác thực bước cao** | API 10.10 🔐 | Dùng cơ chế step-up có sẵn của AUTH (`purpose = WITHDRAWAL` — kiểm ở I00.04 tên header / token); thiếu / hết hạn → `401` theo AUTH. Không tự viết OTP mới | ☐ |
| D6 | **Sổ giao dịch** (API 10.10) | UC-VND-20 b4–5 | `GET /seller/wallet/transactions?month=YYYY-MM&type&q&page&size` — gộp (read-model `UNION ALL`): (a) ký quỹ của shop → loại `SALE` (Bán hàng → vào escrow; trạng thái `ESCROW` / `COD_PENDING` / `DISPUTED`), (b) `wallet_transactions` → `RELEASE` (Giải ngân — `CREDIT_ESCROW_RELEASE`), `WITHDRAWAL` (`DEBIT_WITHDRAWAL`), `REFUND` (`DEBIT_REFUND`), `ADJUSTMENT`; (c) lệnh rút chưa xong → `WITHDRAWAL` trạng thái `PENDING`. Cột: thời gian, mã tham chiếu (mã đơn `TR-…` / `TRWD…` / `TRRF…`), loại, gross, hoa hồng, net, trạng thái. `q` tìm mã; `summary` tổng vào / ra trong kỳ. Rỗng → `items = []` (web hiện "Chưa có giao dịch nào trong tháng này") | ☐ |
| D7 | **Xuất CSV** | API 10.10 `export` | `GET /seller/wallet/transactions/export?month&type` → `text/csv; charset=UTF-8` có BOM (Excel đọc đúng tiếng Việt), tên `sao-ke-vi-YYYY-MM.csv`, phần ghi CSV dùng chung tách lên `common/csv/CsvWriter` (nếu `AuditLogCsvWriter` của admin tách gọn được thì cả hai dùng chung; không thì viết `CsvWriter` mới, admin giữ nguyên) | ☐ |
| D8 | **Migration** | UC-ADM-05 b2–5 | `V33`: `withdrawal_requests.transfer_code` (`TRWD{id}`, UNIQUE), `bank_reference`, `approved_at`, `approved_by_admin_id`; index `(status, requested_at)`. `processed_by_admin_id` / `processed_at` giữ cho bước hoàn tất / từ chối | ☐ |
| D9 | **Duyệt & chuyển khoản** (API 11.6) | UC-ADM-05 b1–6 | `GET /admin/withdrawals?status=PENDING|APPROVED|UNCONFIRMED|COMPLETED|REJECTED&page&size` (`UNCONFIRMED` = `APPROVED` quá 24 giờ chưa khớp) + chi tiết (số dư, khiếu nại đang mở của shop — read-model `refund_requests`, TK nhận); `POST …/{id}/approve` — shop phải `VERIFIED` (`409 GNE-WAL-4092`), lệnh `PENDING` → `APPROVED` (+ `approved_*`). Nhật ký: bước duyệt **không** ghi audit (chưa có giá trị riêng — `PAYOUT_REVIEWED` là giá trị mở rộng DATN), **hoàn tất** ghi `PAYOUT_APPROVED` (đúng UC b5), **từ chối** ghi `PAYOUT_REJECTED`; `GET …/{id}/transfer` → khối chuyển khoản (ngân hàng, số TK đầy đủ — chỉ `PROCESS_PAYOUT`, tên, số tiền, nội dung `TRWD{id}`, ảnh VietQR `qr.sepay.vn` tới TK shop); `POST …/{id}/reject {reason}` (bắt buộc) từ `PENDING` / `APPROVED`; `POST …/{id}/complete {bank_reference}` (xác nhận tay) từ `APPROVED` / `PROCESSING`. `startProcessing()` (`PROCESSING`) — **bỏ** trong TLCN (API không có bước "đã chuyển"), ghi UC | ☐ |
| D10 | **Khớp SePay tiền ra** | UC-ADM-05 b4–5; C3 D8–D9 | `WithdrawalTransferMatcher` (đăng ký vào `BankTransferRouter`): `OUT` + `TRWD{id}` + `amount` khớp + lệnh `APPROVED` → hoàn tất như D11 với `bank_reference = referenceCode`; số tiền lệch → `UNMATCHED` + AdminNotification (không tự hoàn tất — UC 4b); lệnh đã `COMPLETED` → `IGNORED` | ☐ |
| D11 | **Hoàn tất** (một transaction) | UC-ADM-05 b5–6 | `WithdrawalRequest.complete(referenceCode)` → `COMPLETED`; `WalletPoster.settleWithdrawal(…, true)` + `wallet_transactions` `DEBIT_WITHDRAWAL`; sổ cái `PAYOUT_SETTLE` (Nợ `VENDOR_PAYABLE` / Có `BANK`); audit `PAYOUT_APPROVED`; AFTER_COMMIT: thông báo shop + email (C11) kèm `bank_reference`, số TK che | ☐ |
| D12 | **Cài `PayoutAccountGuard`** | B4 cổng | `PaymentPayoutAccountGuard` (payment): còn lệnh `PENDING` / `APPROVED` / `PROCESSING` → chặn xóa / đổi TK nhận tiền (`409 GNE-WAL-4091`) | ☐ |
| D13 | **Thông báo** | UC-ADM-04 b6; UC-ADM-05 b6 | Tạo lệnh: shop "Lệnh rút … đang chờ duyệt (24 giờ làm việc)", Admin `AdminNotification` "Lệnh rút mới"; duyệt / từ chối / hoàn tất: shop nhận loại `SYSTEM_ALERT` với `metadata.event = WITHDRAWAL_APPROVED | WITHDRAWAL_REJECTED | WITHDRAWAL_COMPLETED` (không thêm `NotificationType` mới — README mục 4) | ☐ |
| D14 | **Cờ web** | — | `seller-wallet` (Seller › Ví & Escrow: tổng quan, sổ giao dịch, CSV, hộp Rút tiền có OTP), `admin-withdrawals` (Admin › Xử lý rút tiền) | ☐ |
| D15 | **Ngoài phạm vi** | — | Chi hộ tự động qua API ngân hàng (SePay không chi — UC), rút nhiều TK, điều chỉnh ví tay (`CREDIT/DEBIT_ADJUSTMENT` — DATN), màn Sổ cái Admin (C10) | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

```
Seller › Ví & Escrow: GET /seller/wallet (D1) · GET /seller/wallet/transactions (D6) · GET …/export (D7)
Rút tiền: bước cao 🔐 WITHDRAWAL (D5) → POST /seller/withdrawals 🔁 {amount}
   └─ WithdrawalService.request (một transaction): khóa ví FOR UPDATE → kiểm D3, D4 → holdForWithdrawal (pending += amount)
      → withdrawal_requests PENDING (chụp TK nhận, transfer_code = TRWD{id}) → AFTER_COMMIT báo shop + AdminNotification
Admin › Xử lý rút tiền (PROCESS_PAYOUT):
   approve (shop VERIFIED) → APPROVED → GET …/transfer (QR + TRWD{id}) → Staff chuyển khoản qua internet banking
   SePay OUT TRWD{id} ─► WithdrawalTransferMatcher (D10) ─► complete (D11)
   quá 24 giờ chưa khớp → UNCONFIRMED → POST …/complete {bank_reference} (D11)
   reject {reason} → settleWithdrawal(false) → PAYOUT_REJECTED → báo shop
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/resources/db/migration/V33__c9_withdrawals.sql                  (mới — số tạm)
server-spring/src/main/java/com/genie/
├── common/error/ErrorCode.java                  (sửa) WAL-4001, 4221, 4032, 4091, 4092, 4093, 4094
├── common/csv/CsvWriter.java                    (mới hoặc tách từ AuditLogCsvWriter — D7)
└── payment/wallet/
    ├── domain/       WithdrawalRequest (sửa: transferCode, bankReference, approvedAt/By) · repo (sửa)
    ├── application/  SellerWalletQuery (D1, D6 read-model) · WalletStatementCsv · WithdrawalService (request, approve, reject, complete)
    │                 WithdrawalTransferMatcher · PaymentPayoutAccountGuard · WithdrawalNotifications
    └── api/          SellerWalletController (10.10) · AdminWithdrawalController (11.6 — trong payment, N4)
server-spring/src/test/java/com/genie/payment/wallet/…
    SellerWalletIT · WalletStatementIT · WithdrawalRequestIT · WithdrawalConcurrencyIT · AdminWithdrawalIT
    WithdrawalTransferMatcherIT · PayoutAccountGuardIT
web/src/features/seller-wallet/infrastructure/wallet.api.ts · admin-finance/infrastructure/finance.api.ts (rút tiền) · error-codes.ts · i18n
docs/Document/… ENTITY (6.4, 6.6, Phụ lục E) · API_SPEC (10.10, 11.6, 15) · Usecase.md · CHECKLIST · BAO_CAO · xlsx
docs/api-tests/wallet.http · docs/kiem-thu/kich-ban-2026-11-28-c9.md
```

---

## 5. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint / tác vụ | Bảng | Quyền |
|---|---|---|---|
| UC-VND-20 | `GET /seller/wallet`, `GET /seller/wallet/transactions`, `GET …/export` | `vendor_wallets`, `wallet_transactions`, `escrow_ledgers`, `withdrawal_requests`, `orders`, `order_items` (đọc) | VENDOR `VERIFIED` |
| UC-ADM-04 | `POST /seller/withdrawals` 🔁 🔐 | `vendor_wallets`, `withdrawal_requests`, `admin_notifications` | VENDOR `VERIFIED` |
| UC-ADM-05 | `GET /admin/withdrawals`, `POST …/approve | reject | complete`, `GET …/transfer`, SePay tiền ra `TRWD` | `withdrawal_requests`, `vendor_wallets`, `wallet_transactions`, `financial_ledger`, `system_logs`, `bank_transactions` | `PROCESS_PAYOUT` |

**Mã lỗi:** `GNE-WAL-4001` (400 không đủ số dư), `4221` (422 dưới 100.000₫), `4032` (403 TK chưa xác minh), `4092` (409 duyệt shop chưa `VERIFIED`), `4091` (409 xóa TK khi còn lệnh); **mới:** `GNE-WAL-4093` (409 đã có lệnh đang chờ), `GNE-WAL-4094` (409 sai bước / ví đang khóa kiểm tra).

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — `ModuleBoundaryTest` (**`admin` không import `payment`**).
2. Swagger: summary `UC-VND-20`, `UC-ADM-04`, `UC-ADM-05`.
3. `docs/api-tests/wallet.http`: rút 99.000₫ → 4221; rút quá khả dụng → 4001; lệnh thứ 2 khi đang chờ → 4093; Moderator gọi Admin rút tiền → 403.
4. Đụng `web/`: `npm run verify`.
5. Tick progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Ixx.yy` = task; `Ixx.Cn` = task commit.

### I00 — Chuẩn bị

- **[I00.01]** Chạy `git config user.name` / `user.email` — ghi vào dòng *Phụ trách* của progress; kiểm C3 đã merge (`WalletPoster`, `BankTransferRouter`).
- **[I00.02]** Chốt D1–D15 vào progress.
- **[I00.03]** Tạo nhánh `feature/c9-seller-wallet` từ `develop`.
- **[I00.04]** Đọc cơ chế bước cao 🔐 của AUTH (`purpose`, header, thời hạn), `PayoutAccountGuard` + chỗ vendor gọi, `AuditLogCsvWriter` (có tách lên `common` được không — D7), CHECK `withdrawal_requests.status`, `wallet_transactions.type`.
- **[I00.05]** Đọc màn mock Seller › Ví & Escrow (4 thẻ số, khối "Cách tính tiền về ví", sổ giao dịch lọc tháng / loại, CSV, hộp Rút tiền + OTP) và Admin › Xử lý rút tiền (tab, chi tiết, modal "Chờ chuyển khoản" có QR, từ chối có lý do, xác nhận tay).

### I01 — Tài liệu trước

- **[I01.01]** ENTITY 6.4 (luồng giữ / chốt theo D2 — sửa câu mâu thuẫn), 6.6 (cột D8, bỏ `PROCESSING` khỏi luồng TLCN — D9), Phụ lục E.
- **[I01.02]** API_SPEC — 10.10 (khối D1, sổ D6, CSV D7, điều kiện D3–D5), 11.6 (D9–D11, `UNCONFIRMED`), 15 (`GNE-WAL-4093`, `4094`), lịch sử phiên bản.
- **[I01.03]** Usecase.md — UC-ADM-04 b5 (D2, D4), UC-ADM-05 b2–5 (không có bước `PROCESSING`, audit theo D9).
- **[I01.C1]** COMMIT — `docs(payment): specify seller wallet statements, withdrawal holds and payout confirmation`

### I02 — Ví & sổ giao dịch (UC-VND-20)

- **[I02.01]** `SellerWalletQuery` — tổng quan (D1) + sổ giao dịch gộp (D6) bằng read-model, giờ VN.
- **[I02.02]** `WalletStatementCsv` + `CsvWriter` (D7).
- **[I02.03]** `SellerWalletController` — `GET /seller/wallet`, `/transactions`, `/transactions/export`; `@Operation(summary = "UC-VND-20 …")`.
- **[I02.04]** Test — `SellerWalletIT` (dựng 2 đơn giải ngân + 1 ký quỹ `HELD` + 1 COD chờ + 1 lệnh rút chờ → 4 số đúng; doanh thu tháng + % so tháng trước; ví shop khác không thấy), `WalletStatementIT` (gộp đúng loại / trạng thái; lọc tháng theo giờ VN biên 00:00; `q` theo mã đơn; CSV có BOM, tiếng Việt, đúng số dòng).
- **[I02.C1]** COMMIT — `feat(payment): show seller wallet balances and a merged transaction statement`

### I03 — Tạo lệnh rút (UC-ADM-04)

- **[I03.01]** `V33__c9_withdrawals.sql` (D8) + entity / repo + `ErrorCode`.
- **[I03.02]** `WithdrawalService.request` (D2–D5) + `POST /seller/withdrawals` 🔁 🔐.
- **[I03.03]** `PaymentPayoutAccountGuard` (D12) + thông báo (D13).
- **[I03.04]** Test — `WithdrawalRequestIT` (đủ điều kiện → `PENDING`, `pending_withdrawal` tăng, `balance` giữ, khả dụng giảm, hash hợp lệ; thiếu 🔐 → 401; TK chưa xác minh → 4032; 99.000 → 4221; quá khả dụng → 4001; lệnh thứ 2 → 4093), **`WithdrawalConcurrencyIT`** (khả dụng 1.000.000; 10 request 300.000 song song → tổng giữ ≤ 1.000.000, chỉ 1 lệnh do D4 — TS 14), `PayoutAccountGuardIT` (còn lệnh chờ → xóa TK `409 GNE-WAL-4091`).
- **[I03.C1]** COMMIT — `feat(db): add withdrawal transfer codes and approval columns`
- **[I03.C2]** COMMIT — `feat(payment): let sellers request withdrawals that hold available balance`

### I04 — Duyệt & chuyển khoản (UC-ADM-05) — nhánh `feature/c9-payouts`

- **[I04.01]** `WithdrawalService.approve / reject / complete` (D9, D11) + `AdminWithdrawalController` (5 endpoint 11.6, `PROCESS_PAYOUT`).
- **[I04.02]** `WithdrawalTransferMatcher` (D10) đăng ký vào `BankTransferRouter`.
- **[I04.03]** Hàng đợi `UNCONFIRMED` (truy vấn `APPROVED` + `approved_at < now − 24h`), AdminNotification nhắc mỗi ngày (job 09:00 giờ VN).
- **[I04.04]** Test — `AdminWithdrawalIT` (duyệt shop chưa `VERIFIED` → 4092; duyệt → `APPROVED`; `transfer` có `TRWD`, QR, số TK đầy đủ chỉ với `PROCESS_PAYOUT`; từ chối thiếu lý do → 422; từ chối → `pending` trả lại, `PAYOUT_REJECTED`; xác nhận tay → `COMPLETED`, `balance` giảm, `DEBIT_WITHDRAWAL`, sổ cái `PAYOUT_SETTLE` cân, `PAYOUT_APPROVED`; sai bước → 4094), `WithdrawalTransferMatcherIT` (SePay OUT `TRWD` đúng số → hoàn tất 1 lần dù webhook gửi lại; lệch số → `UNMATCHED`, lệnh giữ `APPROVED`).
- **[I04.C1]** COMMIT — `feat(payment): let finance staff approve payouts and confirm transfers through sepay`

### I05 — Nối web (cờ `seller-wallet`, `admin-withdrawals`)

- **[I05.01]** `seller-wallet/wallet.api.ts` (3 hàm + CSV) — 4 thẻ số, khối cách tính, sổ giao dịch (lọc tháng / loại / tìm), "Xuất CSV" (`requestBlob`), hộp Rút tiền: bước cao OTP (dùng component sẵn của AUTH), Idempotency-Key, lỗi `4032` dẫn tới Cài đặt › Tài khoản nhận tiền.
- **[I05.02]** `admin-finance/finance.api.ts` (5 hàm rút tiền) — tab trạng thái + `UNCONFIRMED`, chi tiết, modal chuyển khoản (QR, nút chép nội dung `TRWD…`), từ chối (lý do bắt buộc), xác nhận tay (mã giao dịch ngân hàng).
- **[I05.03]** vi / en + `error-codes.ts`; test mapper; kiểm 9 theme, 360 px; `npm run verify`.
- **[I05.C1]** COMMIT — `feat(web): connect the seller wallet and payout processing to the real api`

### I06 — Kiểm thử & chạy thật

- **[I06.01]** `docs/api-tests/wallet.http` — ví, sổ, CSV, rút (đủ / lỗi), duyệt, chuyển (giả lập SePay OUT), từ chối, xác nhận tay.
- **[I06.02]** `docs/kiem-thu/kich-ban-2026-11-28-c9.md` — TC-C9-01…: đơn hoàn tất (C5) → ví tăng sau giải ngân; sổ giao dịch + CSV mở được bằng Excel; rút tiền có OTP → Finance Officer duyệt → giả lập SePay tiền ra → hoàn tất, email (Mailpit, khi C11 có); từ chối; xác nhận tay sau 24 giờ (chỉnh giờ); xóa TK nhận tiền khi còn lệnh bị chặn; 9 theme; 360 px.
- **[I06.03]** Chạy thật — web build `PUBLIC_REAL_API=…,seller-wallet,admin-withdrawals` + backend `local` + Postgres + Redis + Silo + Mailpit; Chromium (Playwright) với chủ shop + Finance Officer; mỗi lỗi một commit `fix(…)`.
- **[I06.C1]** COMMIT — `docs(payment): add wallet and payout api tests and real-run scenarios`

### I07 — Hoàn thiện use case & tick tiến độ

- **[I07.01]** OpenAPI — summary `UC-VND-20`, `UC-ADM-04/05`.
- **[I07.02]** `docs/web-backend-handoff.md` — 2 cờ đã nối.
- **[I07.03]** Usecase.md — *Hiện trạng* 3 UC; cổng `PayoutAccountGuard` đã có (sửa ghi chú UC-VND-07 nếu có).
- **[I07.04]** `CHECKLIST_TIEN_DO_UC.md` — tick 5 lớp 3 UC, Trạng thái, **cột Phụ trách = tên ở I00.01**, số mục 1 + phân hệ 05 / 11, dòng mục 5 *Nhật ký*.
- **[I07.05]** `BAO_CAO_UC_DOC_LAP.md` — mục C9 + dòng *Phụ trách*.
- **[I07.06]** `Quản Lý UseCase.xlsx` — 3 UC, cột AA.
- **[I07.07]** README ngày (mục 7) — tick dòng C9.
- **[I07.C1]** COMMIT — `docs(payment): record seller wallet, withdrawals and payouts as implemented`
- **[I07.08]** Mở 2 PR xếp chồng vào `develop` (`c9-seller-wallet` → `c9-payouts`).

---

## 8. Ngoài phạm vi

- Chi hộ tự động qua API ngân hàng, rút nhiều TK, điều chỉnh ví tay (DATN); màn Sổ cái / Dashboard (C10).

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Rút song song vượt số dư | Khóa ví `FOR UPDATE` + `holdForWithdrawal` kiểm khả dụng + D4; `WithdrawalConcurrencyIT` |
| SePay báo tiền ra hai lần | UNIQUE `sepay_id` (C3) + lệnh đã `COMPLETED` → `IGNORED` |
| Chuyển nhầm TK | Chỉ TK đã xác minh, chụp vào lệnh (N2); modal hiện tên chủ TK để Staff so |
| Câu UC lệch entity (trừ số dư lúc tạo lệnh) | Theo entity (D2), sửa UC ở I01.03 |
| Vòng `payment → vendor → admin → payment` | Controller Admin trong `payment` (N4); `ModuleBoundaryTest` |
