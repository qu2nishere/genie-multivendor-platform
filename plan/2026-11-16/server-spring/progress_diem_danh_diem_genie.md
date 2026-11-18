# PROGRESS — Điểm danh nhận Điểm hàng ngày (UC-PRO-08) + Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm (UC-PRO-09)

| | |
|---|---|
| **Plan** | [plan_diem_danh_diem_genie.md](./plan_diem_danh_diem_genie.md) — chi tiết từng task tra theo mã `Pxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 45 task + 8 commit, 8 giai đoạn |
| **Nhánh** | `feature/promotion-coin-checkin` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-17 — đang làm (53 / 53) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | Đường dẫn: UC-PRO-09 ghi `GET /api/v1/customer/coins`, `/coins/history`; API 3.13 ghi `/me/coins`, `/me/coins/transactions`, `/me/coins/check-ins`, `/me/coins/redemptions`, `/public/coin-rewards` | ☑ | Theo API: `/me/coins`, `/me/coins/transactions`, `/me/coins/check-ins`, `/me/coins/redemptions`, `/public/coin-rewards`; sửa Usecase.md ở P07.05 · 2026-11-17 |
| D2 | Khóa HMAC của `balance_hash` Điểm — TS 16.6 chỉ có `WALLET_HMAC_SECRET` | ☑ | Như đề xuất — `HMAC-SHA256(wallet-hmac-secret, "coin:" + userId + ":" + balance)` hex chữ thường (khớp seed `R__seed_demo_70`) · 2026-11-17 |
| D3 | Danh mục quà đổi Điểm ("danh sách do sàn cấu hình") không có bảng | ☑ | Như đề xuất — `genie.coin.rewards[]` `{id, coin-cost, voucher-code}` trỏ `COIN500/1000/2000` · 2026-11-17 |
| D4 | `user_vouchers` có `UNIQUE (user_id, voucher_id)` → một người chỉ giữ 1 bản mỗi voucher | ☑ | **Điều chỉnh:** bảng `user_vouchers` không lưu "đã dùng" qua entity (`isUsed` là `@Transient` = đã dùng HẾT lượt, ENTITY 4.3) → đếm `voucher_usages` so với `usage_per_user`. Đang giữ voucher (còn lượt) → `409 GNE-PRO-4092`; giữ mà đã hết lượt → cũng `409 GNE-PRO-4092` kèm `details.reason = "USAGE_LIMIT_REACHED"` (cấp lại sẽ vượt giới hạn lượt / người). Không xóa / sửa dòng cũ · 2026-11-17 |
| D5 | Lô Điểm hết hạn: cần dòng âm nhưng `CoinTransactionType` không có loại cho việc này | ☑ | Làm P06. Migration **`V16__coin_expired_type.sql`** (nhánh coin merge sau banner `V15`, trước KYC → KYC dời sang `V17`; sửa sổ đăng ký README mục 4) · 2026-11-17 |
| D6 | `streak_days` trả cho giao diện 7 ô | ☑ | Như đề xuất — `streak_days = ((streakCount − 1) mod 7) + 1` khi chuỗi còn hiệu lực, ngược lại 0 · 2026-11-17 |
| D7 | LLD 9.9 ghi `UNIQUE(user_id, checkin_date)` (mỗi ngày một dòng) nhưng entity / `V4` là mỗi khách một dòng | ☑ | Như đề xuất — theo ENTITY (mỗi khách một dòng), sửa câu LLD 9.9 · 2026-11-17 |
| D8 | Phát hiện lệch số dư (`verifyHash` sai) | ☑ | Như đề xuất — cổng `CoinAlertPort` (mặc định ghi log lỗi); ghi Điểm → `500 GNE-SYS-5000`, đọc vẫn trả số dư · 2026-11-17 |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| P00 | Chuẩn bị | 5 | 0 | 5 / 5 | — | `feature/promotion-coin-checkin` |
| P01 | Nền: mã lỗi, cấu hình, băm số dư | 6 | 1 | 7 / 7 | — | `feature/promotion-coin-checkin` |
| P02 | Sổ Điểm dùng chung (cộng / trừ theo lô) | 5 | 1 | 6 / 6 | — | `feature/promotion-coin-checkin` |
| P03 | UC-PRO-08 Điểm danh | 5 | 1 | 6 / 6 | — | `feature/promotion-coin-checkin` |
| P04 | UC-PRO-09 Tra cứu số dư & lịch sử | 5 | 1 | 6 / 6 | — | `feature/promotion-coin-checkin` |
| P05 | UC-PRO-09 Đổi quà (đổi Điểm lấy voucher) | 6 | 1 | 7 / 7 | — | `feature/promotion-coin-checkin` |
| P06 | (D5 — cắt được) Hết hạn lô Điểm | 5 | 1 | 6 / 6 | — | `feature/promotion-coin-checkin` |
| P07 | Nối giao diện `web/` & hoàn thiện | 8 | 2 | 10 / 10 | — | `feature/promotion-coin-checkin` |
| **Tổng** | | **45** | **8** | **53 / 53** | | |

## P00 — Chuẩn bị

- [x] **P00.01** Kiểm danh tính git
- [x] **P00.02** Chốt quyết định D1–D8
- [x] **P00.03** Tạo nhánh
- [x] **P00.04** Đọc màn mock
- [x] **P00.05** Đối chiếu `coins.api.ts`

## P01 — Nền: mã lỗi, cấu hình, băm số dư

- [x] **P01.01** `ErrorCode`
- [x] **P01.02** `CoinProperties` (`genie.coin`)
- [x] **P01.03** `CoinBalanceHasher`
- [x] **P01.04** `CoinBalanceHasherTest`
- [x] **P01.05** Repository
- [x] **P01.06** Tài liệu
- [x] **P01.C1** 🔖 COMMIT `feat(promotion): add coin settings, balance hash and coin error codes` · hash: `46be4dc`

## P02 — Sổ Điểm dùng chung (cộng / trừ theo lô)

- [x] **P02.01** `CoinLedgerService.credit(userId, amount, type, referenceId, label, now)`
- [x] **P02.02** `CoinLedgerService.debit(userId, amount, type, referenceId, label, now)`
- [x] **P02.03** Tính lô còn lại (FEFO)
- [x] **P02.04** `CoinLedgerServiceTest` (unit, repository giả)
- [x] **P02.05** `CoinFacade`
- [x] **P02.C1** 🔖 COMMIT `feat(promotion): add coin ledger with expiring lots and balance integrity check` · hash: `7699288`

## P03 — UC-PRO-08 Điểm danh

- [x] **P03.01** `DailyCheckinTest` (unit, bổ sung cho entity đã có)
- [x] **P03.02** `CheckInService.checkIn(userId)`
- [x] **P03.03** `MeCoinController`
- [x] **P03.04** `CheckInIT` (PG + Redis, `MutableClock`)
- [x] **P03.05** `CheckInIT` đồng thời
- [x] **P03.C1** 🔖 COMMIT `feat(promotion): check in daily to earn coins with a seven day streak` · hash: `b525f6e`

## P04 — UC-PRO-09 Tra cứu số dư & lịch sử

- [x] **P04.01** `CoinQueryService.summary(userId, now)`
- [x] **P04.02** Kiểm toàn vẹn khi đọc
- [x] **P04.03** `CoinQueryService.transactions(userId, cursor, limit)`
- [x] **P04.04** `MeCoinController`
- [x] **P04.05** `CoinQueryIT`
- [x] **P04.C1** 🔖 COMMIT `feat(promotion): show coin balance, expiring coins and transaction history` · hash: `8eeefdc`

## P05 — UC-PRO-09 Đổi quà (đổi Điểm lấy voucher)

- [x] **P05.01** `CoinRewardCatalog`
- [x] **P05.02** `PublicCoinRewardController`
- [x] **P05.03** `CoinRedemptionService.redeem(userId, rewardId)`
- [x] **P05.04** Ánh xạ `$Voucher` (API mục 1) từ entity `Voucher`
- [x] **P05.05** `MeCoinController`
- [x] **P05.06** `CoinRedemptionIT`
- [x] **P05.C1** 🔖 COMMIT `feat(promotion): redeem coins for platform vouchers` · hash: `7ad5cd9`

## P06 — (D5 — cắt được) Hết hạn lô Điểm

- [x] **P06.01** ENTITY trước
- [x] **P06.02** `V17__coin_expired_type.sql`
- [x] **P06.03** `CoinExpiryJob`
- [x] **P06.04** `CoinExpiryJobIT` (`MutableClock`)
- [x] **P06.05** `web/`
- [x] **P06.C1** 🔖 COMMIT `feat(promotion): expire coin lots after twelve months` · hash: `959d3f0`

## P07 — Nối giao diện `web/` & hoàn thiện

- [x] **P07.01** `web/src/features/coins/infrastructure/coins.api.ts`
- [x] **P07.02** `error-codes.ts` + `vi.json` / `en.json`
- [-] **P07.03** Chạy thật hai phía
- [x] **P07.04** `docs/api-tests/coins.http`
- [x] **P07.05** Tài liệu nguồn
- [x] **P07.06** Tiến độ
- [x] **P07.07** `cd web && npm run verify`.
- [x] **P07.C1** 🔖 COMMIT `feat(web): connect coin screen to the real api` · hash: `b1df774`
- [x] **P07.C2** 🔖 COMMIT `docs(promotion): record coin check-in and coin history use cases as implemented` · hash: `(commit tài liệu cuối nhánh)`
- [-] **P07.08** Mở PR vào `develop`

---

## Chốt plan — tick tiến độ use case

Làm **ngay khi xong plan** (hoặc xong từng use case), trong file [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó.

- [x] `UC-PRO-08` Điểm danh nhận Điểm hàng ngày — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [x] `UC-PRO-09` Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [x] Cập nhật số ở mục 1 *Tổng quan* của checklist (Đã triển khai / Đang triển khai / Chưa có backend)
- [x] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · mã commit / PR)
- [x] Đổi cột *Hiện trạng mã nguồn* của các use case trên trong `Quản Lý UseCase.xlsx`
- [ ] Tick dòng plan này ở bảng *Tiến độ các plan* trong [README](../README.md) mục 6 — **chờ**: UC-PRO-08, PRO-09 còn ô **Chạy** (người dùng chạy thật trên giao diện)

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|

## Nhật ký
| P00.03 | Nhánh tách từ `feature/admin-home-banner` (xếp nối tiếp seed → audit → banner → coin) để `ErrorCode`, i18n, số migration không xung đột |
| P00.05 | Web chờ: `GET /me/coins` đúng API; lịch sử đọc mảng `{id, type, description, amount, created_at}` (`limit` 50 — API cho tối đa 50); quà `{id, coin_cost, voucher_value, min_order_value}`; điểm danh đọc `{reward}`; đổi quà đọc `voucher.code`. Nhánh thật đang dùng `USE_MOCK` → đổi `isMocked("coins")` ở P07 |
| P03.01 | Lỗi tiềm ẩn đã sửa trong entity: `lastCheckinAt` đọc từ CSDL mang offset UTC (`hibernate.jdbc.time_zone: UTC`) nên `toLocalDate()` lệch ngày Việt Nam lúc 00:00–07:00 → `checkIn(now)` quy `lastCheckinAt` về cùng offset với `now` trước khi so ngày (ghi ENTITY Phụ lục E) |
| P01.03 | `CoinBalanceHasher` đọc `genie.security.wallet-hmac-secret` bằng `@Value` (không có trong `SecurityProperties`); thêm `seal(coin)` để ghi lại băm. Profile test thêm `wallet-hmac-secret: test-wallet-secret`. Vector cố định trong test khớp băm dữ liệu mẫu `R__seed_demo_70` (`coin:1:1250`, khóa `local-wallet-secret`) |
| P01.05 | `UserCoinRepository.findByIdForUpdate`, `DailyCheckinRepository.findByCustomerId(ForUpdate)`, `CoinTransactionRepository`: `sumAmount`, `findCreditLots` (hạn tăng dần, lô không hạn cuối), `sumDebits`, `findPage` / `findPageAfter` (keyset), `findUsersWithExpiredLots`, `existsByUserIdAndTypeAndReferenceId` (cho P06) |
| P02.01 | `credit` / `debit` mở `Propagation.MANDATORY` — bắt buộc nằm trong transaction của nghiệp vụ gọi (Điểm là tiền). Ví Điểm chưa có thì tạo `balance 0` + băm rồi khóa lại. Băm lệch → báo `CoinAlertPort` (nếu có bean) + log lỗi, ném `500 GNE-SYS-5000`; thiếu Điểm → `422 GNE-PRO-4222` kèm `details {balance, required, missing}` (web hiện "còn thiếu N Điểm") |
| P02.03 | `CoinLots.remainingLots` (FEFO, lô không hạn xếp cuối) + `expiringBetween(now, until)` — bỏ lô đã quá hạn (để job hết hạn xử lý) |
| P03.02 | Khóa `lock:checkin:{userId}` bằng `setIfAbsent(…, 5s)`, giải phóng trong `finally`; Redis lỗi → `503 GNE-SYS-5030`. Transaction mở bằng `TransactionTemplate` (khóa Redis bọc ngoài transaction). Lần điểm danh đầu tạo `DailyCheckin.firstFor` (entity mới — ENTITY 4.9, Phụ lục E); trùng `uk_daily_checkin_customer` cũng trả `409`. Nhãn sổ "Điểm danh ngày {ô}" theo D6. `CoinTime`: giờ VN, nửa đêm kế tiếp, `streak_days` (D6) |
| P03.04 | `created_at` của sổ do `@CreationTimestamp` lấy giờ thật JVM (không theo `MutableClock`) nên test so hạn lô với thời điểm điểm danh thay vì `created_at`. 10 luồng đồng thời: đúng 1 lần `201`, 9 lần `409`, 1 dòng `CHECK_IN` |
| P04.01 | `expiring_soon` = `null` khi không có Điểm nào sắp hết hạn (web đã nhận `null`); `next_check_in_at` luôn là 00:00 hôm sau giờ VN. Lịch sử trả vỏ danh sách (`pagination` cấp gốc — API 0.3/0.4), `limit` 1–50 |
| P04.05 | IDOR: gửi kèm `?user_id=<người khác>` vẫn chỉ thấy dòng của mình. Số dư bị sửa thẳng CSDL: `GET` vẫn trả 200 (+ log lỗi / cổng báo động), điểm danh → `500 GNE-SYS-5000`, sổ không đổi |
| P05.03 | Khóa `lock:coin:{userId}`; người đó bấm đổi chồng lên lần đang xử lý → `429 GNE-RAT-4290` (không phải lỗi nghiệp vụ). Trong transaction: khóa ví Điểm trước rồi mới kiểm Kho Voucher (xếp hàng theo người dùng) → `debit` → thêm `UserVoucher`; trùng `uk (user_id, voucher_id)` lọt qua khóa → rollback cả phần trừ Điểm, trả `409 4092`. "Đã dùng hết lượt" đếm `voucher_usages` so với `usage_per_user` (D4 điều chỉnh) |
| P05.04 | `promotion/voucher/application/VoucherView.of(voucher, now, usedUp, saved)` — `$Voucher` (API mục 1); `shop.name`, `included_categories` để trống / `null` tới khi có cổng vendor / catalog (voucher đổi Điểm là voucher sàn `ALL` nên không ảnh hưởng) |
| P05.06 | 5 luồng đổi cùng quà khi chỉ đủ Điểm cho 1 lần → đúng 1 `201`, các luồng còn lại `429` / `409` / `422`; `Σ amount = balance` |
| P06.02 | Migration **`V16__coin_expired_type.sql`** (không phải V17 như plan gốc — D5, sổ đăng ký README đã sửa). `SchemaValidationIT` xanh |
| P06.03 | Khóa ví bằng native `FOR UPDATE SKIP LOCKED` (`UserCoinRepository.lockSkipLocked`); mỗi người một transaction (`TransactionTemplate`), lỗi một người không chặn người khác. Idempotent nhờ FEFO (dòng `EXPIRED` ăn vào chính lô đã hết hạn) + kiểm `existsByUserIdAndTypeAndReferenceId` |
| P06.05 | Web chỉ hiện `description` do server trả ("Điểm hết hạn") nên không cần khóa i18n theo loại — chỉ thêm `EXPIRED` vào kiểu `CoinTxnType` |
| P07.01 | Cờ `isMocked('coins')` cho 5 hàm. Hook làm mới số dư cả khi lỗi (`onSettled`): `409 GNE-PRO-4091` (điểm danh ở tab khác) → nút chuyển "Đã điểm danh"; `422 GNE-PRO-4222` → toast "Bạn còn thiếu N Điểm" (đọc `details.missing` — khóa `coins.missing`); `409 GNE-PRO-4092` → thông điệp lỗi mới. DTO web khớp phản hồi thật (đã đối chiếu khi chạy API) |
| P07.03 | `[-]` Phần trình duyệt chờ người dùng (CLAUDE.md). Đã chạy thật phía API trên CSDL mới `genie_coin` (V16) với `mai.tran`: ví mới 0 → điểm danh 201 (+100, ô 1) → lần 2 `409 4091` → đổi `COIN500` khi có 100 → `422 4222` `missing 400` → nạp lô 1.000 hết hạn sau 20 ngày → `expiring_soon 1000` → đổi `COIN500` 201 (còn 600) → đổi lại `409 4092` → lịch sử 3 dòng đúng thứ tự; Σ sổ = số dư = 600. Cách chạy giao diện: `PUBLIC_REAL_API=auth,coins`, dữ liệu mẫu nhánh seed (1.250 Điểm + voucher `COIN*`) |
| P07.07 | Cổng web: lint 0, astro check 0 lỗi, tsc 0, vitest 318/318, build OK, i18n OK (3269 khóa) |
| P07.05 | Usecase.md UC-PRO-08 / 09: đường dẫn theo API (D1) + *Hiện trạng*; LLD 9.9 sửa câu `UNIQUE(user_id, checkin_date)` thành mỗi khách một dòng (D7), LLD 10 thêm cron "Điểm hết hạn" 00:15; API 3.13 ghi quy tắc `streak_days` (D6), `expiring_soon`, lỗi đổi quà + danh mục từ cấu hình (D3, D4). ENTITY 4.7 / 4.8 / 4.9 / Phụ lục A / E đã sửa ở P01, P03, P06 |
| P07.06 | Checklist UC-PRO-08, PRO-09: GD / BE / Test / Nối `☑`, **Chạy `☐`** (chờ người dùng) → 🟡; Excel "Đang triển khai". Cổng: `./mvnw verify` BUILD SUCCESS (107 unit + 130 IT) — lần chạy đầu JVM test sập vì máy còn ~1,6 GB RAM; tạm dừng container dev + `-DargLine="-Xmx768m"` thì xanh |
| P07.08 | `[-]` Chưa push / mở PR — chờ người dùng; PR mở sau PR banner (nhánh tách từ `feature/admin-home-banner`) |

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (45 task, 8 commit, 8 giai đoạn) — chưa bắt đầu làm | — |
| 2026-11-17 | P00–P07: mã lỗi + cấu hình + băm `coin:`, sổ Điểm FEFO, điểm danh 7 ngày (ngày giờ VN, chống đồng thời), số dư / sắp hết hạn / lịch sử, đổi quà, cron hết hạn `V16`, nối web, tài liệu; còn chạy thật trên giao diện (P07.03) và mở PR | `46be4dc` `7699288` `b525f6e` `8eeefdc` `7ad5cd9` `959d3f0` `b1df774` |
