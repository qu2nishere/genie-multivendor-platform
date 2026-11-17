# PLAN — Điểm danh nhận Điểm hàng ngày (UC-PRO-08) + Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm (UC-PRO-09)

| | |
|---|---|
| **Ngày lập** | 2026-11-16 |
| **Thể loại** | server-spring (module `promotion`, feature `coin`) + nối `web/` (feature `coins`) |
| **Use case** | Điểm danh nhận Điểm hàng ngày & Duy trì chuỗi điểm danh (UC-PRO-08) · Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm (UC-PRO-09) — Sprint 8 theo implement 1.2, làm sớm vì độc lập |
| **Tài liệu chuẩn** | UC phân hệ 07 (UC-PRO-08, UC-PRO-09) · API 3.13, 0.3, 0.4, 15, 16 · ENTITY 4.7 (`UserCoin`), 4.8 (`CoinTransaction`), 4.9 (`DailyCheckin`), 4.3 (`UserVoucher`) · LLD 9.9, 10 · LUU_TRU 4.6 (khóa ngắn) · TS 4, 7, 8, 10.3, 14 — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_diem_danh_diem_genie.md](./progress_diem_danh_diem_genie.md) — cùng mã task `Pxx.yy` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |

---

## 0. Hiện trạng đã khảo sát

| Phần | Có gì | Thiếu |
|---|---|---|
| `promotion/coin/domain` | `UserCoin` (`userId` là khóa chính, `balance`, `@Version`, `balanceHash`, `lastUpdated`; `earn`, `spend`, `verifyHash`), `CoinTransaction` (`amount` dương cộng / âm trừ, `type`, `referenceId`, `label`, `expiresAt`; `isExpired`, `isCredit`), `DailyCheckin` (mỗi khách **một** dòng: `lastCheckinAt`, `streakCount`; `checkIn(now)` trả 100 hoặc 1.000 khi `streakCount % 7 == 0`), `CoinTransactionType` (6 giá trị), 3 repository rỗng | Toàn bộ `api`, `application`; truy vấn repository |
| `promotion/voucher/domain` | `Voucher`, `UserVoucher` (`UNIQUE (user_id, voucher_id)`, `is_used`), repository rỗng | Dùng cho "Đổi quà" |
| CSDL | `V4__promotion.sql`: `user_coins`, `coin_transactions` (`CHECK` 6 loại), `daily_checkins` (`UNIQUE customer_id`) | Loại giao dịch cho **lô Điểm hết hạn** (UC-PRO-09 ràng buộc: "Cron ghi dòng âm để Σ amount = balance") chưa có trong enum |
| Cấu hình | `genie.security.wallet-hmac-secret` (`WALLET_HMAC_SECRET`) | Khóa HMAC cho số dư Điểm; danh mục quà đổi Điểm |
| `common/error/ErrorCode` | Chưa có `GNE-PRO-4091`, `GNE-PRO-4222` | Thêm (đã có ở API 15 và `web/src/shared/api/error-codes.ts`) |
| `web/features/coins` | Màn Tài khoản › Điểm Genie xong trên mock: `getCoins` (`GET /me/coins`), `listCoinTxns` (`GET /me/coins/transactions`), `listCoinRewards` (`GET /public/coin-rewards`), điểm danh (`POST /me/coins/check-ins`), đổi quà (`POST /me/coins/redemptions`) | Nối API thật |
| Seed | Plan [seed](../db/plan_seed_du_lieu_mau.md) S05.02 (3 voucher `COIN*`), S06.01–S06.03 (1.250 Điểm, chuỗi 3 ngày của `mai.tran`) | Chỉ cần cho phần Đổi quà (P05) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.promotion.coin.{api,application,domain}`; một thao tác = một method `@Transactional` ở `application`; controller không mở transaction | TS 4.1–4.2 |
| N2 | **Điểm là tiền**: cộng / trừ Điểm, ghi `coin_transactions`, cập nhật `daily_checkins` nằm **trong cùng một transaction chính**; không `@Async` cho các bước này | TS 4.5, 10.3 |
| N3 | Luật nằm trong entity (`DailyCheckin.checkIn`, `UserCoin.earn / spend / verifyHash`); service chỉ điều phối, khóa và tính `balanceHash` | TS 7.2 |
| N4 | Đồng thời: khóa ngắn Redis `lock:checkin:{userId}` (`SET NX EX 5`) + khóa dòng `user_coins` (`SELECT … FOR UPDATE`) + `@Version`; hai request song song chỉ một lần cộng Điểm | UC-PRO-08 bước 2; TS 10.3 |
| N5 | Toàn vẹn số dư: kiểm `verifyHash()` **trước** mỗi lần ghi và tính lại `balanceHash` **sau** mỗi lần ghi; `Σ coin_transactions.amount = user_coins.balance` luôn đúng | LLD 9.9; UC-PRO-09 bước 3 |
| N6 | "Ngày" tính theo **giờ Việt Nam** (`Asia/Ho_Chi_Minh`): service đổi `now` sang múi giờ +07:00 trước khi gọi `checkIn(now)`; thời gian lấy từ `Clock` bean | UC-PRO-08 tiền điều kiện |
| N7 | Mỗi lần cộng tạo một **lô** `coin_transactions` có `expires_at = now + 12 tháng`; tiêu / hết hạn trừ lô sắp hết hạn trước (FEFO) | LLD 9.9 |
| N8 | Điểm không quy đổi thành tiền mặt, không rút về ngân hàng — không có endpoint nào làm việc đó | UC-PRO-09 ràng buộc |
| N9 | JSON snake_case, vỏ `ApiResponse`, lịch sử phân trang cursor; `@Operation(summary = "UC-PRO-08 …" / "UC-PRO-09 …")`; chỉ đọc / ghi Điểm của chính người gọi (lấy `userId` từ `@CurrentUser`, không nhận từ request) | TS 8, 9 |
| N10 | Test: unit entity + service (đồng hồ giả `MutableClock`), `*IT` Testcontainers (PG + Redis) gồm ca đồng thời | TS 14 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Nguồn lệch | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | Đường dẫn: UC-PRO-09 ghi `GET /api/v1/customer/coins`, `/coins/history`; API 3.13 ghi `/me/coins`, `/me/coins/transactions`, `/me/coins/check-ins`, `/me/coins/redemptions`, `/public/coin-rewards` | UC vs API | Theo **API**; sửa câu trong Usecase.md (P07.03) | ☐ |
| D2 | Khóa HMAC của `balance_hash` Điểm — TS 16.6 chỉ có `WALLET_HMAC_SECRET` | ENTITY 4.7 | **Dùng lại** `genie.security.wallet-hmac-secret`, chuỗi băm có tiền tố miền để không trùng ví shop: `HMAC-SHA256("coin:" + userId + ":" + balance)` (hex). Không thêm biến môi trường. Ghi công thức vào ENTITY 4.7 | ☐ |
| D3 | Danh mục quà đổi Điểm ("danh sách do sàn cấu hình") không có bảng | API 3.13; UC-PRO-09 bước 4 | Cấu hình trong `application.yml`: `genie.coin.rewards[]` = `{id, coin-cost, voucher-code}`; mặc định 3 quà trỏ voucher seed `COIN500` / `COIN1000` / `COIN2000`. `voucher_value`, `min_order_value` đọc từ bảng `vouchers`. Màn quản trị quà đổi Điểm để DATN | ☐ |
| D4 | `user_vouchers` có `UNIQUE (user_id, voucher_id)` → một người chỉ giữ 1 bản mỗi voucher | V4 vs UC-PRO-09 | Đang giữ bản **chưa dùng** → không cho đổi tiếp: `409` mã mới **`GNE-PRO-4092`** "Bạn đang có voucher này chưa dùng"; bản đã dùng (`is_used = true`) → đổi lại được bằng cách đặt lại `is_used = false`, `created_at = now`. Thêm mã vào API 15, `ErrorCode`, web | ☐ |
| D5 | Lô Điểm hết hạn: cần dòng âm nhưng `CoinTransactionType` không có loại cho việc này | ENTITY Phụ lục A vs UC-PRO-09 | Thêm giá trị **`EXPIRED`** (chỉ thêm — NFR-12) bằng migration `V17__coin_expired_type.sql` (đăng ký ở [README mục 4](../README.md)) + cron hằng ngày (LLD 10). Giai đoạn P06 — **cắt được** nếu thiếu thời gian (lô đầu tiên chỉ hết hạn sau 12 tháng; riêng lô seed "sắp hết hạn" sẽ không tự trừ) | ☐ |
| D6 | `streak_days` trả cho giao diện 7 ô | API 3.13 vs entity (`streakCount` tăng mãi, thưởng khi `% 7 == 0`) | `streak_days = ((streakCount − 1) mod 7) + 1` khi chuỗi còn hiệu lực (điểm danh hôm nay hoặc hôm qua), ngược lại `0`; sau ngày thứ 7 ô quay về 1 ("bắt đầu chuỗi mới" — UC-PRO-08 bước 4). Không đổi entity | ☐ |
| D7 | LLD 9.9 ghi `UNIQUE(user_id, checkin_date)` (mỗi ngày một dòng) nhưng entity / `V4` là mỗi khách một dòng | LLD vs ENTITY | Theo **ENTITY** (SSOT); chống điểm danh 2 lần / ngày bằng `checkIn()` + khóa dòng; sửa câu ở LLD 9.9 | ☐ |
| D8 | Phát hiện lệch số dư (`verifyHash` sai) | UC-PRO-09 bước 3 | Từ chối mọi thao tác **ghi** Điểm của tài khoản đó (`500 GNE-SYS-5000`, không lộ chi tiết), ghi log lỗi + tạo `AdminNotification` loại `SYSTEM_ALERT` qua cổng đảo ngược `CoinAlertPort` (module `notification` cài sau; mặc định chỉ ghi log). Đọc số dư vẫn cho xem | ☐ |

---

## 3. Cây file sẽ tạo / sửa

```
server-spring/src/main/java/com/genie/
├── common/error/ErrorCode.java                 (sửa) + ALREADY_CHECKED_IN 4091 · REWARD_ALREADY_HELD 4092 · INSUFFICIENT_COINS 4222
└── promotion/coin/
    ├── domain/       (đã có) UserCoin · CoinTransaction · DailyCheckin · CoinTransactionType (+EXPIRED — D5) · 3 repository (thêm truy vấn)
    ├── application/  CoinBalanceHasher · CoinLedgerService (cộng / trừ FEFO, dùng chung nội bộ module)
    │                 CheckInService · CoinQueryService · CoinRedemptionService · CoinRewardCatalog (đọc cấu hình D3)
    │                 CoinExpiryJob (D5) · CoinAlertPort (D8) · CoinFacade (cửa cho module khác cộng / trừ Điểm sau này)
    │                 CoinProperties (@ConfigurationProperties genie.coin)
    └── api/          MeCoinController · PublicCoinRewardController · các record *Response / *Request
server-spring/src/main/resources/
├── application.yml                             (sửa) genie.coin.{rewards, check-in.rewards, expiry-months, expiring-soon-days}
└── db/migration/V17__coin_expired_type.sql     (mới — D5)
server-spring/src/test/java/com/genie/promotion/
    DailyCheckinTest · CoinBalanceHasherTest · CoinLedgerServiceTest (FEFO) · CheckInIT · CoinQueryIT · CoinRedemptionIT · CoinExpiryJobIT
web/src/features/coins/infrastructure/coins.api.ts   (sửa)
web/src/shared/api/error-codes.ts · web/src/i18n/{vi,en}.json   (sửa — GNE-PRO-4092)
docs/Document/…  ENTITY (4.7, 4.8, Phụ lục A, E) · API_SPEC (3.13, 15, 16) · LLD 9.9, 10 · Usecase.md · CHECKLIST_TIEN_DO_UC.md · Quản Lý UseCase.xlsx
docs/web-backend-handoff.md · docs/api-tests/coins.http
```

---

## 4. Bảng ánh xạ Use case → API → Dữ liệu

| UC / bước | Endpoint (API 3.13) | Bảng | Redis |
|---|---|---|---|
| UC-PRO-09 — số dư, sắp hết hạn, trạng thái điểm danh | `GET /me/coins` | `user_coins`, `coin_transactions`, `daily_checkins` (đọc) | — |
| UC-PRO-08 — điểm danh | `POST /me/coins/check-ins` → `201 {reward, balance, streak_days}` | `daily_checkins`, `user_coins`, `coin_transactions` (`CHECK_IN`) | `lock:checkin:{userId}` 5 giây |
| UC-PRO-09 — lịch sử | `GET /me/coins/transactions?cursor&limit=20` | `coin_transactions` | — |
| UC-PRO-09 — danh mục quà | `GET /public/coin-rewards` | `vouchers` (đọc) + cấu hình | — |
| UC-PRO-09 — đổi quà | `POST /me/coins/redemptions {reward_id}` → `201 {voucher, balance}` | `user_coins`, `coin_transactions` (`VOUCHER_REDEEM`), `user_vouchers` | `lock:coin:{userId}` 5 giây |
| UC-PRO-09 ràng buộc — hết hạn lô | (cron hằng ngày — LLD 10) | `coin_transactions` (`EXPIRED`), `user_coins` | — |

**Mã lỗi thêm vào `ErrorCode`:** `GNE-PRO-4091` (409 — đã điểm danh hôm nay), `GNE-PRO-4222` (422 — không đủ Điểm), **`GNE-PRO-4092`** (409 — mới, D4). Dùng lại: `GNE-VAL-4220`, `GNE-SYS-4040` (quà không tồn tại), `GNE-SYS-5030` (không lấy được khóa Redis), `GNE-RAT-4290`.
**Migration:** `V17__coin_expired_type.sql` (chỉ khi làm P06). **Không** thêm `UploadPurpose` / `AuditAction`.

---

## 5. Nhánh, commit, PR

- Nhánh: `feature/promotion-coin-checkin` tách từ `develop` (có mã Jira: `feature/GNE-<số>-promotion-coin-checkin`). Tách PR: (1) P01–P03 nền + điểm danh, (2) P04 tra cứu, (3) P05 đổi quà, (4) P06 hết hạn (nếu làm), (5) P07 nối web + tài liệu.
- Scope commit: `common`, `db`, `promotion`, `web`, `docs`.
- Quy ước chung: [README mục 5](../README.md).

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — unit + IT (PG + Redis) + ArchUnit (+ `SchemaValidationIT` sau `V17`).
2. Swagger UI hiện endpoint mới, summary có `UC-PRO-08` / `UC-PRO-09`.
3. Thử tay `docs/api-tests/coins.http` với `mai.tran@gmail.com`.
4. Sau mỗi kịch bản ghi Điểm: `SELECT balance FROM user_coins` = `SELECT sum(amount) FROM coin_transactions` của cùng user.
5. Nếu đụng `web/`: `cd web && npm run verify`.
6. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Pxx.yy` = task; `Pxx.Cn` = task commit.

### P00 — Chuẩn bị

- **[P00.01]** Kiểm danh tính git — `git config user.name` / `user.email`.
- **[P00.02]** Chốt quyết định D1–D8 — ghi vào đầu file progress (đặc biệt D5: có làm P06 không).
- **[P00.03]** Tạo nhánh — `git switch develop && git pull && git switch -c feature/promotion-coin-checkin`.
- **[P00.04]** Đọc màn mock — mở `/account/points` (mock): 7 ô chuỗi điểm danh, hiệu ứng nhận Điểm, nút "Đã điểm danh" + đếm ngược tới 00:00, dòng "150 Điểm sắp hết hạn trong 30 ngày", lịch sử, lưới "Đổi quà" (khóa khi thiếu Điểm) — ghi các trường giao diện cần.
- **[P00.05]** Đối chiếu `coins.api.ts` — ghi lại DTO web đang chờ ở nhánh gọi thật (tên trường, kiểu) để phản hồi backend khớp; chỗ nào web lệch API 3.13 thì ghi để sửa ở P07.

### P01 — Nền: mã lỗi, cấu hình, băm số dư

- **[P01.01]** `ErrorCode` — thêm `GNE-PRO-4091`, `GNE-PRO-4222`, `GNE-PRO-4092` (D4) với thông điệp tiếng Việt; cập nhật test đếm mã (nếu có).
- **[P01.02]** `CoinProperties` (`genie.coin`) — `check-in.rewards` (mặc định `[100,100,100,100,100,100,1000]` — chỉ để trả về giao diện; số thưởng thật do `DailyCheckin.checkIn` quyết định, test khẳng định hai nơi khớp), `expiry-months = 12`, `expiring-soon-days = 30`, `rewards[]` (D3). Khai trong `application.yml` kèm chú thích nguồn.
- **[P01.03]** `CoinBalanceHasher` — `hash(userId, balance)` theo D2 (đọc `genie.security.wallet-hmac-secret`); `matches(UserCoin)` gọi `verifyHash`.
- **[P01.04]** `CoinBalanceHasherTest` — cùng đầu vào cho cùng băm; đổi `balance` / `userId` cho băm khác; khớp vector cố định (ghi vào test) để seed tính được băm.
- **[P01.05]** Repository — `UserCoinRepository.findByIdForUpdate(userId)` (`@Lock(PESSIMISTIC_WRITE)`); `DailyCheckinRepository.findByCustomerIdForUpdate`; `CoinTransactionRepository`: `sumAmountByUserId`, `findCreditLots(userId)` (amount > 0, sắp `expires_at ASC NULLS LAST, id`), `sumDebits(userId)`, trang keyset `(created_at DESC, id DESC)`.
- **[P01.06]** Tài liệu — ENTITY 4.7 ghi công thức băm (D2); API 15 thêm `GNE-PRO-4092`.
- **[P01.C1]** COMMIT — `feat(promotion): add coin settings, balance hash and coin error codes`

### P02 — Sổ Điểm dùng chung (cộng / trừ theo lô)

- **[P02.01]** `CoinLedgerService.credit(userId, amount, type, referenceId, label, now)` — khóa dòng `user_coins` (chưa có ví Điểm thì tạo với `balance = 0` + băm), `verifyHash` (sai → D8), `earn`, lưu `CoinTransaction` có `expiresAt = now + 12 tháng` (N7), tính lại `balanceHash`; `amount` phải > 0.
- **[P02.02]** `CoinLedgerService.debit(userId, amount, type, referenceId, label, now)` — khóa dòng, `verifyHash`, không đủ → ném lỗi `GNE-PRO-4222`; `spend`; lưu **một** dòng âm; tính lại băm. (FEFO thể hiện ở cách tính lô còn lại — P02.03 — vì bảng không có cột "còn lại".)
- **[P02.03]** Tính lô còn lại (FEFO) — hàm thuần `remainingLots(creditLots, totalDebits)`: duyệt các lô cộng theo hạn tăng dần, trừ dần tổng đã tiêu; trả danh sách `(expiresAt, remaining)`. Dùng cho "sắp hết hạn" (P04) và cron hết hạn (P06).
- **[P02.04]** `CoinLedgerServiceTest` (unit, repository giả) — cộng tạo lô có hạn 12 tháng; trừ quá số dư → lỗi, số dư không đổi; `remainingLots`: tiêu ăn vào lô sắp hết hạn trước, lô đã hết hạn mà chưa tiêu vẫn hiện để cron xử lý; băm sai → từ chối ghi.
- **[P02.05]** `CoinFacade` — cửa công khai cho module khác sau này (`order` hoàn Điểm, `social` thưởng đánh giá, checkout dùng Điểm): `earn(…)`, `spend(…)`, `balanceOf(userId)`; chỉ nhận / trả giá trị nguyên thủy + record (TS 4.4). Chưa có nơi gọi — có Javadoc ghi rõ ai sẽ dùng.
- **[P02.C1]** COMMIT — `feat(promotion): add coin ledger with expiring lots and balance integrity check`

### P03 — UC-PRO-08 Điểm danh

- **[P03.01]** `DailyCheckinTest` (unit, bổ sung cho entity đã có) — lần đầu → chuỗi 1, 100 Điểm; hôm qua đã điểm danh → +1; ngày thứ 7 → 1.000; ngày thứ 8 → 100 (chuỗi mới theo D6); bỏ 1 ngày → về 1; cùng ngày → `IllegalStateException`; ranh giới 23:59 / 00:01 giờ Việt Nam.
- **[P03.02]** `CheckInService.checkIn(userId)` — (1) `SET lock:checkin:{userId} NX EX 5`, không lấy được → `409 GNE-PRO-4091` (coi như đang xử lý lần bấm trước); Redis lỗi → `503 GNE-SYS-5030`; (2) transaction: khóa dòng `daily_checkins` (chưa có thì tạo), đổi `now` sang +07:00 (N6), `reward = checkIn(now)` — bắt `IllegalStateException` → `409 GNE-PRO-4091`; (3) `CoinLedgerService.credit(…, CHECK_IN, label "Điểm danh ngày {n}")`; (4) trả `{reward, balance, streak_days}` (D6).
- **[P03.03]** `MeCoinController` — `POST /api/v1/me/coins/check-ins` → `201`; `@Operation(summary = "UC-PRO-08 — Điểm danh nhận Điểm")`; yêu cầu đăng nhập (mọi vai trò có thể mua hàng: CUSTOMER, VENDOR).
- **[P03.04]** `CheckInIT` (PG + Redis, `MutableClock`) — chuỗi 7 ngày liên tiếp: thưởng 100×6 + 1.000, số dư 1.600, 7 dòng `CHECK_IN` đều có hạn 12 tháng; điểm danh lần 2 cùng ngày → `409`, số dư không đổi; bỏ một ngày → chuỗi về 1; 23:30 và 00:30 (giờ VN) là hai ngày khác nhau dù cùng ngày UTC.
- **[P03.05]** `CheckInIT` đồng thời — 10 luồng cùng gọi điểm danh cho một user → đúng 1 lần `201`, 9 lần `409`, đúng 1 dòng `CHECK_IN`, `Σ amount = balance`.
- **[P03.C1]** COMMIT — `feat(promotion): check in daily to earn coins with a seven day streak`

### P04 — UC-PRO-09 Tra cứu số dư & lịch sử

- **[P04.01]** `CoinQueryService.summary(userId, now)` — `balance` (0 khi chưa có ví Điểm); `expiring_soon = {amount, within_days: 30}` = tổng phần còn lại của các lô có hạn trong 30 ngày tới (P02.03); `check_in = {streak_days (D6), checked_in_today, next_check_in_at (00:00 hôm sau giờ VN), rewards}`.
- **[P04.02]** Kiểm toàn vẹn khi đọc — `verifyHash` sai: vẫn trả số dư nhưng gọi `CoinAlertPort` + log lỗi (D8); thêm trường nội bộ không đưa ra API.
- **[P04.03]** `CoinQueryService.transactions(userId, cursor, limit)` — keyset `(created_at, id)` giảm dần; mỗi dòng `id`, `type`, `description` (= `label`, không có thì sinh theo `type`), `amount` (âm khi tiêu), `created_at`.
- **[P04.04]** `MeCoinController` — `GET /api/v1/me/coins`, `GET /api/v1/me/coins/transactions?cursor&limit=20` (`limit` 1–50); `Cache-Control: no-store`; `@Operation(summary = "UC-PRO-09 …")`.
- **[P04.05]** `CoinQueryIT` — tài khoản chưa có Điểm trả `balance 0`, chuỗi 0, mảng rỗng; sau điểm danh `checked_in_today = true`, `next_check_in_at` đúng nửa đêm VN; lô hạn 20 ngày nữa được tính vào `expiring_soon`, lô 40 ngày thì không; đã tiêu ăn hết lô sắp hết hạn thì `expiring_soon.amount = 0`; phân trang 2 trang không trùng; **IDOR**: không có cách nào xem Điểm của user khác (không nhận `userId` từ request).
- **[P04.C1]** COMMIT — `feat(promotion): show coin balance, expiring coins and transaction history`

### P05 — UC-PRO-09 Đổi quà (đổi Điểm lấy voucher)

- **[P05.01]** `CoinRewardCatalog` — đọc `genie.coin.rewards`, tra `vouchers` theo `voucher-code` (cùng module `promotion`); bỏ qua quà có voucher không tồn tại / đã hết hạn (log cảnh báo lúc khởi động); `list()` trả `{id, coin_cost, voucher_value, min_order_value}`.
- **[P05.02]** `PublicCoinRewardController` — `GET /api/v1/public/coin-rewards`, `Cache-Control: public, max-age=300`; `@Operation(summary = "UC-PRO-09 — Danh mục quà đổi Điểm")`.
- **[P05.03]** `CoinRedemptionService.redeem(userId, rewardId)` — khóa ngắn `lock:coin:{userId}`; transaction: quà không có → `404 GNE-SYS-4040`; kiểm `user_vouchers` (D4): đang giữ bản chưa dùng → `409 GNE-PRO-4092`; `CoinLedgerService.debit(…, VOUCHER_REDEEM, referenceId = voucherId, label "Đổi voucher {code}")` (thiếu → `422 GNE-PRO-4222`); tạo `UserVoucher` (hoặc kích hoạt lại bản đã dùng); trả `{voucher: $Voucher, balance}`.
- **[P05.04]** Ánh xạ `$Voucher` (API mục 1) từ entity `Voucher` — đặt mapper ở feature `voucher` (`promotion/voucher/api` hoặc `application`) để plan voucher sau dùng lại; chỉ điền các trường `$Voucher` yêu cầu.
- **[P05.05]** `MeCoinController` — `POST /api/v1/me/coins/redemptions` (`{reward_id}` bắt buộc) → `201`.
- **[P05.06]** `CoinRedemptionIT` — đổi thành công: số dư giảm đúng `coin_cost`, có 1 dòng `VOUCHER_REDEEM` âm, có `user_vouchers`; thiếu Điểm → `422`, không có dòng nào được ghi; đổi lần 2 khi chưa dùng → `409 4092`; đã dùng thì đổi lại được; quà lạ → `404`; 5 luồng đổi cùng lúc khi chỉ đủ Điểm cho 1 lần → đúng 1 thành công; `Σ amount = balance`.
- **[P05.C1]** COMMIT — `feat(promotion): redeem coins for platform vouchers`

### P06 — (D5 — cắt được) Hết hạn lô Điểm

- **[P06.01]** ENTITY trước — Phụ lục A thêm `CoinTransactionType.EXPIRED`; 4.8 ghi dòng hết hạn (`amount` âm, `reference_id` = id lô gốc); API 16 + 3.13 thêm `EXPIRED` vào `type`; `GIAI_NGHIA_ENTITY.md`, `java-code/Diagram09_*.java`.
- **[P06.02]** `V17__coin_expired_type.sql` — thay `ck_coin_transactions_type` thêm `'EXPIRED'`; enum Java thêm `EXPIRED`; `SchemaValidationIT` xanh.
- **[P06.03]** `CoinExpiryJob` — `@Scheduled(cron = "0 15 0 * * *", zone = "Asia/Ho_Chi_Minh")`: lấy user có lô cộng `expires_at < now` (theo lô, `FOR UPDATE SKIP LOCKED` trên `user_coins`), mỗi user một transaction: tính phần còn lại của các lô đã hết hạn (P02.03) → `debit(…, EXPIRED, referenceId = id lô, label "Điểm hết hạn")`; idempotent — chạy lại không trừ thêm (lô đã có dòng `EXPIRED` tham chiếu thì bỏ qua).
- **[P06.04]** `CoinExpiryJobIT` (`MutableClock`) — lô 100 Điểm, tiêu 30, tua 12 tháng + 1 ngày → job trừ 70, số dư đúng, `Σ amount = balance`; chạy lại không trừ thêm; lô chưa hết hạn không bị đụng; hai user xử lý độc lập.
- **[P06.05]** `web/` — thêm nhãn loại `EXPIRED` ("Điểm hết hạn") ở `vi.json` / `en.json` và kiểu `CoinTxnType`.
- **[P06.C1]** COMMIT — `feat(promotion): expire coin lots after twelve months`

### P07 — Nối giao diện `web/` & hoàn thiện

- **[P07.01]** `web/src/features/coins/infrastructure/coins.api.ts` — thay `USE_MOCK` bằng `isMocked('coins')`; đối chiếu DTO / mapper với phản hồi thật của 5 endpoint; lỗi `GNE-PRO-4091` → nút chuyển "Đã điểm danh" + tải lại; `GNE-PRO-4222` → khóa nút đổi + số Điểm còn thiếu; `GNE-PRO-4092` → thông báo đang có voucher chưa dùng.
- **[P07.02]** `error-codes.ts` + `vi.json` / `en.json` — thêm `GNE-PRO-4092`; `check-i18n` xanh.
- **[P07.03]** Chạy thật hai phía — `PUBLIC_REAL_API=auth,coins`, đăng nhập `mai.tran@gmail.com` (đã seed 1.250 Điểm, chuỗi 3 ngày): thấy "150 Điểm sắp hết hạn", điểm danh ngày thứ 4 (+100), nút đổi "Đã điểm danh" + đếm ngược đúng; đổi quà `COIN500` → Kho Voucher có voucher (khi phân hệ voucher nối thật thì kiểm lại); lịch sử có dòng mới.
- **[P07.04]** `docs/api-tests/coins.http` — kịch bản đúng (xem → điểm danh → lịch sử → đổi quà) + lỗi (`4091`, `4222`, `4092`, `401`).
- **[P07.05]** Tài liệu nguồn — Usecase.md UC-PRO-08 / UC-PRO-09: đường dẫn theo API (D1), dòng *Hiện trạng*; LLD 9.9 sửa câu `UNIQUE(user_id, checkin_date)` (D7), LLD 10 ghi giờ chạy cron hết hạn; API 3.13 ghi quy tắc `streak_days` (D6), quà đổi lấy từ cấu hình (D3), `GNE-PRO-4092` (D4); ghi lịch sử phiên bản.
- **[P07.06]** Tiến độ — `CHECKLIST_TIEN_DO_UC.md` (2 UC, tick 5 lớp, Nhật ký), `Quản Lý UseCase.xlsx` → "Đã triển khai"; `docs/web-backend-handoff.md` đánh dấu nhóm `coins` đã nối; `CLAUDE.md` mục 2.
- **[P07.07]** `cd web && npm run verify`.
- **[P07.C1]** COMMIT — `feat(web): connect coin screen to the real api`
- **[P07.C2]** COMMIT — `docs(promotion): record coin check-in and coin history use cases as implemented`
- **[P07.08]** Mở PR vào `develop` — mẫu PR ghi migration `V17` (nếu có P06: chỉ nới `CHECK`, tương thích ngược).

---

## 8. Ngoài phạm vi

- Nguồn Điểm khác: hoàn Điểm theo đơn (`ORDER_CASHBACK`), thưởng đánh giá có ảnh (`REVIEW_BONUS`), dùng Điểm khi thanh toán (`USED_AT_CHECKOUT` — UC-PRO-10), hoàn Điểm khi hủy đơn (`ORDER_REFUND`) — thuộc nhóm C, sẽ gọi `CoinFacade` (P02.05).
- Màn quản trị danh mục quà đổi Điểm; thông báo in-app khi Điểm sắp hết hạn — DATN / cụm thông báo.
- Hiển thị voucher vừa đổi trong Kho Voucher — thuộc cụm B5 (voucher); plan này chỉ ghi `user_vouchers`.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Bấm điểm danh liên tiếp / nhiều tab → cộng Điểm 2 lần | Khóa Redis + khóa dòng + `checkIn()` kiểm ngày; test 10 luồng (P03.05) |
| Lệch múi giờ: server chạy UTC, "ngày" phải theo giờ Việt Nam | Đổi múi giờ ở service (N6); test ranh giới nửa đêm (P03.01, P03.04) |
| `balance_hash` trong seed không khớp khóa local | Công thức + vector cố định trong `CoinBalanceHasherTest` (P01.04); seed ghi chú cách tính |
| Số thưởng trong cấu hình lệch luật trong entity | Test khẳng định `genie.coin.check-in.rewards` khớp `DailyCheckin.checkIn` 7 ngày (P01.02) |
| `UNIQUE (user_id, voucher_id)` làm đổi quà lần 2 lỗi ràng buộc | Xử lý theo D4 trước khi chèn; test (P05.06) |
| Thêm `EXPIRED` làm lệch enum giữa backend / web / Swagger | Làm ENTITY + API trước (P06.01); cập nhật web (P06.05) và hợp đồng `genie-api-spec` ở đợt đồng bộ sau |
