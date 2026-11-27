# PLAN 2026-11-26 — Thông báo toàn sàn (UC-NOT-05) · cụm B6 Chat & realtime · cụm B5 Voucher

> **Nguồn chọn việc:** [`docs/Document/BAO_CAO_UC_DOC_LAP.md`](../../../docs/Document/BAO_CAO_UC_DOC_LAP.md) — nhóm A dòng *Admin phát thông báo toàn sàn* (UC-NOT-05, "đẩy realtime cần cụm B6"), mục 5 cụm **B5 Voucher — quản lý, chưa áp vào giá** và cụm **B6 Chat & realtime**.
> **Tài liệu chuẩn:** `docs/Document/` (TLCN v1.4). Viết tắt như [README 2026-11-16](../../2026-11-16/README.md): **TS** = `kien-thuc/TECH_STACK_VA_QUY_UOC.md` · **UC** = `diagrams/UseCaseDiagram/Usecase.md` · **API** = `document/API_SPEC.md` · **ENTITY** = `diagrams/ClassDiagram/ENTITY.md` · **LUU_TRU** = `kien-thuc/LUU_TRU_DU_LIEU.md` · **LLD** = `document/LLD.md`.
> Khi các nguồn mâu thuẫn: TS mục 12 → UC / ENTITY → Document_Vi → implement; **đường dẫn và hình dạng request / response lấy theo API**.

> **Phụ trách toàn bộ 3 plan:** **Nguyễn Ngọc Thiện** (nhận 2026-11-26 — 8 use case). Đã ghi tên ở cột *Phụ trách* của `CHECKLIST_TIEN_DO_UC.md`, `BAO_CAO_UC_DOC_LAP.md` và cột AA + sheet *Thành viên* của `Quản Lý UseCase.xlsx`.
> **Người commit:** danh tính git đã cấu hình sẵn trong repo (`git config user.name` = `Thien211105`) — không `--author`, không `--no-verify`, không dòng `Co-Authored-By` / dấu vết AI.

## 1. Danh sách plan (mỗi plan một progress cùng tên)

| # | Plan | Progress | Use case | Mã task | Nhánh (tách từ `develop`) |
|---|---|---|---|---|---|
| 1 | [plan_thong_bao_realtime.md](plan_thong_bao_realtime.md) | [progress](progress_thong_bao_realtime.md) | Thông báo trong ứng dụng thời gian thực — WebSocket (UC-NOT-01, thuộc cụm B6) · **Admin phát thông báo toàn sàn (UC-NOT-05)** | `Nxx.yy` | `feature/b56-shared-schema` → `feature/notification-inbox` → `feature/notification-platform-broadcast` |
| 2 | [plan_chat_khach_shop.md](plan_chat_khach_shop.md) | [progress](progress_chat_khach_shop.md) | Nhắn tin trò chuyện thời gian thực Khách – Shop (UC-SOC-01) · Gửi thẻ sản phẩm, hình ảnh và trạng thái đơn hàng trong chat (UC-SOC-02) · Cấu hình tin nhắn trả lời tự động (UC-VND-06) | `Mxx.yy` | `feature/chat-live` → `feature/chat-cards-auto-reply` |
| 3 | [plan_voucher_b5.md](plan_voucher_b5.md) | [progress](progress_voucher_b5.md) | Tạo & Quản lý Voucher Gian hàng (UC-PRO-01) · Tạo & Quản lý Voucher Toàn sàn (UC-PRO-02) · Khách hàng thu thập Voucher vào Kho Voucher cá nhân (UC-PRO-03) | `Vxx.yy` | `feature/voucher-management` → `feature/voucher-wallet` |

**Vì sao chia 3 plan mà không phải 2 cụm:** cụm B6 có 4 use case nhưng hai phần khác hẳn nhau — *Thông báo thời gian thực* (UC-NOT-01) là **nền** cho mọi module đẩy thông báo, dùng chung endpoint `GET /me/notifications` (API 3.12) với *Thông báo toàn sàn* (UC-NOT-05: dòng `pn_…` nằm trong cùng hộp thư) → gộp UC-NOT-01 + UC-NOT-05 thành plan [1]. Ba use case chat (UC-SOC-01, 02, UC-VND-06) cùng bảng `conversations` / `messages` → plan [2]. B5 tách hẳn → plan [3].

## 2. Thứ tự & phụ thuộc

```
[1] N01 — migration chung V26 (nhánh feature/b56-shared-schema, PR riêng, merge TRƯỚC) ──┬──► [1] N02… Thông báo (UC-NOT-01, UC-NOT-05)
                                                                                         ├──► [2] Chat (cần N01 + hub realtime web N05 + NotificationFacade N03)
                                                                                         └──► [3] Voucher (chỉ cần N01; làm song song được với [1] sau khi N01 merge)
[1] N05 (hub WebSocket dùng chung phía web, có gửi khung SEND) ─────────────────────────► [2] M06 (chat nối WebSocket)
[1] N03 (NotificationFacade) ───────────────────────────────────────────────────────────► [2] M04 (báo tin nhắn khi người nhận offline)
```

- **Thứ tự đề xuất (một người làm):** [1] → [2] → [3]. Báo cáo độc lập mục 5 / 8 ghi "làm B6 *sớm* thì các nhóm khác đỡ chờ" — plan [1] đi trước vì nó là nền và UC-NOT-05 được yêu cầu đầu tiên.
- [3] Voucher **không** phụ thuộc [1], [2] (chỉ cần migration chung N01) — nếu bị chặn ở [2] (vd chờ review) thì chuyển sang [3].
- Không plan nào chờ chuỗi C (đơn hàng / thanh toán): chỗ nào cần dữ liệu của C (thẻ đơn hàng trong chat, "Doanh thu từ khuyến mãi", "Đã dùng" của voucher) đều đọc bảng đã có từ C2 và trả `0` / rỗng khi chưa có dữ liệu.

## 3. Đồ thị phụ thuộc module — BẮT BUỘC đọc trước khi viết code

`ModuleBoundaryTest.modules_free_of_cycles` chặn mọi vòng phụ thuộc giữa module. Hiện trạng `develop` (đếm `import com.genie.<module>` 2026-11-26):

```
order ──► auth, catalog, shipping, vendor        vendor ──► admin, auth, catalog, social        promotion ──► catalog
admin ──► auth                                    auth, catalog, social, notification ──► (không module nào)
```

Ba plan này **chỉ được thêm các cạnh sau** (đã kiểm không tạo vòng):

| Cạnh mới | Vì sao | Cấm làm ngược lại |
|---|---|---|
| `notification ──► auth` | tra vai trò / email người nhận (`UserFacade`) | `auth` không import `notification` (auth chỉ phát event) |
| `vendor ──► notification` | cài `KycAdminNotifier` trong **vendor** (gọi `NotificationFacade`) | `notification` **không** cài cổng của vendor (sẽ thành vòng `vendor → social → notification → vendor`) |
| `promotion ──► notification` | cài `CoinAlertPort` trong **promotion** | — |
| `social ──► notification`, `social ──► auth` | chat báo tin khi người nhận offline; tên / ảnh người mua | — |
| `vendor ──► social` (đã có) | `ChatShopDirectoryImpl`, `ChatBadgeContributor` đặt ở **vendor** | `social` **không** import `vendor` |
| `order ──► social` | `ChatOrderLookupImpl` đặt ở **order** (thẻ đơn hàng) | `social` **không** import `order` (vòng `order → vendor → social → order`) |
| `catalog ──► social` | `ChatProductLookupImpl` đặt ở **catalog** (thẻ sản phẩm) | `social` **không** import `catalog` |
| `promotion ──► catalog` (đã có) | tên danh mục, sản phẩm thuộc shop | — |

Thông tin shop / đơn hàng mà `promotion` cần (tên shop, `shop_id`, mã đơn đã dùng voucher) đọc bằng **SQL read-model JOIN thẳng bảng** `vendor_profiles`, `orders` (tiền lệ plan 2026-11-17 D6) — **không** import class của `vendor` / `order` (tránh vòng `vendor → promotion → order → vendor` khi C6 cài cổng của order trong promotion).

## 4. Điểm dùng chung — tránh conflict

| Điểm chung | Quy ước |
|---|---|
| **Migration** | Một migration chung `V26__b5_b6_notification_chat_voucher.sql` (task **N01**, nhánh riêng, merge vào `develop` trước — giống tiền lệ `V25` chung của C1 / C2). Trước khi mở PR: `git fetch && git log origin/develop --oneline -- server-spring/src/main/resources/db/migration`; nếu C1 / C2 đã chiếm `V26` thì đổi thành số kế tiếp và **chép đủ** danh sách `ck_system_logs_action_type` của migration mới nhất. Sau khi merge không sửa; cần thêm → số kế tiếp |
| `common/error/ErrorCode.java` + `web/src/shared/api/error-codes.ts` + `errors.GNE-*` (vi / en) | [1]: `GNE-NOT-4091`, `GNE-NOT-4221` · [2]: `GNE-SOC-4031`, `GNE-SOC-4096` (mới) · [3]: `GNE-VCH-4041`, `4091`, `4092`, `4093`, `4094` (mới), `4221` (đã có trong API 15, chưa có trong code). Thêm đúng khối miền, cùng commit với API 15 |
| `common/storage/UploadPurpose.java` | [1]: `NOTIFICATION_IMAGE` (`content-assets`, quyền `MANAGE_PLATFORM_NOTIFICATION`) · [2]: `CHAT_IMAGE` (`chat-media` riêng tư) — LUU_TRU 5.4 đã đặt sẵn tên |
| `common/audit/AuditAction.java` + `category()` | Giá trị mới chỉ thêm trong `V26`: `PLATFORM_NOTIFICATION_PUBLISHED / UPDATED / DELETED` ([1]), `VOUCHER_CREATED / UPDATED / DISABLED / DELETED` ([3]) — cả hai nhóm `CONFIG` |
| `common/realtime/UserChannel` | [1] thêm hằng `NOTIFICATIONS = "/queue/notifications"`; [2] thêm `CHAT = "/queue/chat"` — chỉ thêm hằng, không đổi chữ ký |
| `web/src/shared/realtime/` | [1] N05 tách `accountChannel` thành hub dùng chung (`realtimeHub.ts`) + thêm `send()`; [2] chỉ **dùng** hub, không tạo `StompConnection` thứ hai |
| `promotion/voucher/application/PromotionSearchPorts` | [3] gọi `evictCache()` **sau commit** mỗi khi tạo / sửa / tắt / xóa voucher SHOP (báo cáo độc lập dòng UC-CAT-05) |
| `docs/web-backend-handoff.md` · `CHECKLIST_TIEN_DO_UC.md` · `BAO_CAO_UC_DOC_LAP.md` · `Quản Lý UseCase.xlsx` | Mỗi plan chỉ sửa dòng của use case mình ở giai đoạn cuối |
| `CLAUDE.md` | Của bạn cùng nhóm — **không** sửa |

## 5. Sổ đăng ký migration

| Số | File | Plan | Nội dung |
|---|---|---|---|
| `V25` | `V25__listing_and_checkout_schema.sql` | C1 + C2 (đã merge) | — |
| `V26` (đã chốt — PR #112) | `V26__b5_b6_notification_chat_voucher.sql` | [1] N01 (dùng chung cả 3 plan) | (a) `platform_notifications.dispatched_at` + 2 index; index `user_platform_notification_reads (platform_notification_id)`; (b) `NotificationType.CHAT_MESSAGE` (sửa `ck_notifications_type`) + index duy nhất một-thông-báo-chưa-đọc-mỗi-hội-thoại; index `admin_notifications (is_read, created_at)`; (c) `messages.is_auto`; 2 index `conversations` theo `last_message_at`; (d) 2 index `vouchers`; (e) 7 giá trị `AuditAction` (sửa `ck_system_logs_action_type`) |

## 6. Quy ước chung (trích TS 13, 14 — không lặp lại ở từng plan)

- **Commit:** `<type>(<scope>): <subject>` — subject + body **tiếng Anh**, mệnh lệnh, chữ thường, không dấu chấm cuối, header ≤ 100 ký tự; `scope` ∈ `commitlint.config.cjs` (`notification`, `social`, `promotion`, `vendor`, `common`, `db`, `web`, `seller-ui`, `admin-ui`, `api`…). Chưa có mã Jira → bỏ ` [#GNE-…]`. Một commit = một thay đổi logic, build được, test xanh. Tài liệu đi riêng commit `docs(<scope>): …` **đầu nhánh** (tài liệu sửa trước — TS 7.3).
- **Cổng kiểm tra trước mỗi task COMMIT:** `cd server-spring && ./mvnw verify` (JDK 21; máy chỉ có JDK 24 thì thêm `-Dnet.bytebuddy.experimental=true`) — unit + IT + ArchUnit + `SchemaValidationIT`; đụng `web/` thì `cd web && npm run verify`; `git add` đúng từng file (không `git add .`).
- **PR:** vào `develop`, squash merge, ≤ ~400 dòng (vượt thì tách theo nhóm task — mỗi nhóm đã có commit riêng), `git pull --rebase origin develop` trước khi xin duyệt.
- **Chạy thật (cột *Chạy*):** web bản build + backend `local` + Postgres + Redis + Silo + Mailpit, thao tác bằng Chromium điều khiển qua Playwright theo kịch bản `docs/kiem-thu/kich-ban-2026-11-26-*.md`; đạt hết ca thì tick `Chạy ☑` và ghi cách chạy + số ca vào progress (như B4 2026-11-25).
- **Xong một use case** (TS 13.7): code + test + migration + `@Operation(summary = "UC-…")` + i18n vi / en + chạy thật + cập nhật `CHECKLIST_TIEN_DO_UC.md`, `BAO_CAO_UC_DOC_LAP.md`, `Quản Lý UseCase.xlsx`, câu *Hiện trạng* trong Usecase.md.

## 7. Tiến độ các plan & tick checklist use case

**Luật:** xong use case nào thì tick ngay ở `CHECKLIST_TIEN_DO_UC.md` (5 ô GD / BE / Test / Nối / Chạy, Trạng thái, số mục 1 *Tổng quan* + bảng *Tiến độ theo phân hệ*, một dòng mục 5 *Nhật ký*), đổi *Hiện trạng mã nguồn* trong Excel, câu *Hiện trạng* trong Usecase.md, bảng cụm trong `BAO_CAO_UC_DOC_LAP.md` — **giữ cột Phụ trách = Nguyễn Ngọc Thiện** — rồi tick dòng dưới đây.

| Thứ tự | Plan | Use case phải tick khi xong | Phụ trách | Xong plan |
|:---:|---|---|---|:---:|
| 1 | [1] Thông báo & realtime | `UC-NOT-01`, `UC-NOT-05` — ✅ 2026-11-27 (chạy thật 13 / 13) | Nguyễn Ngọc Thiện | ☑ |
| 2 | [2] Chat Khách – Shop | `UC-SOC-01`, `UC-SOC-02`, `UC-VND-06` | Nguyễn Ngọc Thiện | ☐ |
| 3 | [3] Voucher B5 | `UC-PRO-01`, `UC-PRO-02`, `UC-PRO-03` | Nguyễn Ngọc Thiện | ☐ |

Khi cả 3 dòng `☑`: tổng quan checklist dự kiến **Đã triển khai +8** (24 → 32 theo số 2026-11-26; đếm lại từ các dòng UC lúc đóng plan), phân hệ 07: +3, 08: +2, 05: +1, 10: +2.
