# PROGRESS — Điểm danh nhận Điểm hàng ngày (UC-PRO-08) + Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm (UC-PRO-09)

| | |
|---|---|
| **Plan** | [plan_diem_danh_diem_genie.md](./plan_diem_danh_diem_genie.md) — chi tiết từng task tra theo mã `Pxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 45 task + 8 commit, 8 giai đoạn |
| **Nhánh** | `feature/promotion-coin-checkin` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-17 — đang làm (11 / 53) |

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
| P01 | Nền: mã lỗi, cấu hình, băm số dư | 6 | 1 | 6 / 7 | — | `feature/promotion-coin-checkin` |
| P02 | Sổ Điểm dùng chung (cộng / trừ theo lô) | 5 | 1 | 0 / 6 | — | `feature/promotion-coin-checkin` |
| P03 | UC-PRO-08 Điểm danh | 5 | 1 | 0 / 6 | — | `feature/promotion-coin-checkin` |
| P04 | UC-PRO-09 Tra cứu số dư & lịch sử | 5 | 1 | 0 / 6 | — | `feature/promotion-coin-checkin` |
| P05 | UC-PRO-09 Đổi quà (đổi Điểm lấy voucher) | 6 | 1 | 0 / 7 | — | `feature/promotion-coin-checkin` |
| P06 | (D5 — cắt được) Hết hạn lô Điểm | 5 | 1 | 0 / 6 | — | `feature/promotion-coin-checkin` |
| P07 | Nối giao diện `web/` & hoàn thiện | 8 | 2 | 0 / 10 | — | `feature/promotion-coin-checkin` |
| **Tổng** | | **45** | **8** | **11 / 53** | | |

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
- [ ] **P01.C1** 🔖 COMMIT `feat(promotion): add coin settings, balance hash and coin error codes` · hash: `…`

## P02 — Sổ Điểm dùng chung (cộng / trừ theo lô)

- [ ] **P02.01** `CoinLedgerService.credit(userId, amount, type, referenceId, label, now)`
- [ ] **P02.02** `CoinLedgerService.debit(userId, amount, type, referenceId, label, now)`
- [ ] **P02.03** Tính lô còn lại (FEFO)
- [ ] **P02.04** `CoinLedgerServiceTest` (unit, repository giả)
- [ ] **P02.05** `CoinFacade`
- [ ] **P02.C1** 🔖 COMMIT `feat(promotion): add coin ledger with expiring lots and balance integrity check` · hash: `…`

## P03 — UC-PRO-08 Điểm danh

- [ ] **P03.01** `DailyCheckinTest` (unit, bổ sung cho entity đã có)
- [ ] **P03.02** `CheckInService.checkIn(userId)`
- [ ] **P03.03** `MeCoinController`
- [ ] **P03.04** `CheckInIT` (PG + Redis, `MutableClock`)
- [ ] **P03.05** `CheckInIT` đồng thời
- [ ] **P03.C1** 🔖 COMMIT `feat(promotion): check in daily to earn coins with a seven day streak` · hash: `…`

## P04 — UC-PRO-09 Tra cứu số dư & lịch sử

- [ ] **P04.01** `CoinQueryService.summary(userId, now)`
- [ ] **P04.02** Kiểm toàn vẹn khi đọc
- [ ] **P04.03** `CoinQueryService.transactions(userId, cursor, limit)`
- [ ] **P04.04** `MeCoinController`
- [ ] **P04.05** `CoinQueryIT`
- [ ] **P04.C1** 🔖 COMMIT `feat(promotion): show coin balance, expiring coins and transaction history` · hash: `…`

## P05 — UC-PRO-09 Đổi quà (đổi Điểm lấy voucher)

- [ ] **P05.01** `CoinRewardCatalog`
- [ ] **P05.02** `PublicCoinRewardController`
- [ ] **P05.03** `CoinRedemptionService.redeem(userId, rewardId)`
- [ ] **P05.04** Ánh xạ `$Voucher` (API mục 1) từ entity `Voucher`
- [ ] **P05.05** `MeCoinController`
- [ ] **P05.06** `CoinRedemptionIT`
- [ ] **P05.C1** 🔖 COMMIT `feat(promotion): redeem coins for platform vouchers` · hash: `…`

## P06 — (D5 — cắt được) Hết hạn lô Điểm

- [ ] **P06.01** ENTITY trước
- [ ] **P06.02** `V17__coin_expired_type.sql`
- [ ] **P06.03** `CoinExpiryJob`
- [ ] **P06.04** `CoinExpiryJobIT` (`MutableClock`)
- [ ] **P06.05** `web/`
- [ ] **P06.C1** 🔖 COMMIT `feat(promotion): expire coin lots after twelve months` · hash: `…`

## P07 — Nối giao diện `web/` & hoàn thiện

- [ ] **P07.01** `web/src/features/coins/infrastructure/coins.api.ts`
- [ ] **P07.02** `error-codes.ts` + `vi.json` / `en.json`
- [ ] **P07.03** Chạy thật hai phía
- [ ] **P07.04** `docs/api-tests/coins.http`
- [ ] **P07.05** Tài liệu nguồn
- [ ] **P07.06** Tiến độ
- [ ] **P07.07** `cd web && npm run verify`.
- [ ] **P07.C1** 🔖 COMMIT `feat(web): connect coin screen to the real api` · hash: `…`
- [ ] **P07.C2** 🔖 COMMIT `docs(promotion): record coin check-in and coin history use cases as implemented` · hash: `…`
- [ ] **P07.08** Mở PR vào `develop`

---

## Chốt plan — tick tiến độ use case

Làm **ngay khi xong plan** (hoặc xong từng use case), trong file [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó.

- [ ] `UC-PRO-08` Điểm danh nhận Điểm hàng ngày — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [ ] `UC-PRO-09` Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [ ] Cập nhật số ở mục 1 *Tổng quan* của checklist (Đã triển khai / Đang triển khai / Chưa có backend)
- [ ] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · mã commit / PR)
- [ ] Đổi cột *Hiện trạng mã nguồn* của các use case trên trong `Quản Lý UseCase.xlsx`
- [ ] Tick dòng plan này ở bảng *Tiến độ các plan* trong [README](../README.md) mục 6

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

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (45 task, 8 commit, 8 giai đoạn) — chưa bắt đầu làm | — |
