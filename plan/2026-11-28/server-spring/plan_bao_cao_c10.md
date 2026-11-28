# PLAN — Chuỗi C10 Báo cáo (UC-VND-08 · UC-ADM-06 · UC-VND-19)

| | |
|---|---|
| **Ngày lập** | 2026-11-28 |
| **Thể loại** | server-spring (module `vendor` feature `analytics`, `settings` (tạm nghỉ); module `admin` feature `dashboard`; module `payment` feature `ledger` (API Sổ cái); cổng cài ở `order`, sửa nhỏ `promotion`) + nối `web/` (feature `seller-dashboard`, `seller-analytics`, `admin-dashboard`, `admin-finance` phần Sổ cái, `seller-settings` phần tạm nghỉ) |
| **Use case** | Xem báo cáo doanh thu & Phân tích bán hàng (UC-VND-08) · Dashboard doanh thu & ký quỹ toàn sàn (UC-ADM-06) · Bật chế độ tạm nghỉ bán hàng của shop (UC-VND-19) |
| **Tài liệu chuẩn** | UC phân hệ 05 (UC-VND-08 dòng 1645, 19 dòng 1674), 11 (UC-ADM-06 dòng 2688) · API 10.2 (tổng quan Seller), 10.9 (phân tích — trừ `sentiment` đã có ở C7), 10.11 (`PUT / DELETE /seller/vacation`), 11.2 (tổng quan Admin), 11.9 (Sổ cái), 12.2 (kết thúc tạm nghỉ hằng giờ), 15 (`GNE-VND-4092`, `GNE-ORD-4093`) · ENTITY 1.4 (`startVacation / endVacation / isOnVacation`), 2.12 `ProductViewStat`, 5.2, 5.3, 5.4, 6.3, 6.4, 6.7 — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_bao_cao_c10.md](progress_bao_cao_c10.md) — cùng mã task `Qxx.yy` |
| **Phụ trách** | Người làm plan — lấy theo **`git config user.name`** trên máy thực hiện (ghi vào progress ở Q00.01); không ghi tên cố định |
| **Người commit** | danh tính git đã cấu hình trên máy đó — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Phụ thuộc** | **Đọc** dữ liệu của C3–C9 (đơn, ký quỹ, ví, sổ cái, hoàn tiền, đánh giá). Viết code sớm được (UC tạm nghỉ không cần chuỗi nào; báo cáo trả `0` khi chưa có dữ liệu), **kiểm số liệu** sau khi C5 + C8 có đơn thật. Khối Cảm xúc (`/seller/analytics/sentiment`) đã thuộc C7 |
| **Mục tiêu** | 3 use case ⬜ → ✅. Shop xem tổng quan + phân tích đúng số với dữ liệu đơn thật; Admin xem tài chính / vận hành / shop tăng trưởng + Sổ cái + CSV; shop bật / tắt tạm nghỉ đúng luật |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `1b66eae`, 2026-11-28)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Tạm nghỉ** | `VendorProfile` có `vacation_starts_at / ends_at / message`, `isOnVacation(now)`; **đọc** đã nối: `ShopSettingsService` trả `vacation` (D14 của plan KYC — chỉ đọc), `PurchaseRules` (giỏ — chặn mua), trang sản phẩm / shop hiện băng "Shop đang tạm nghỉ" (`ProductDetailService`, `PublicShopService`), chat tự trả lời `vacation_message` (`AutoReplyPolicy` — B6); web `seller-settings/settings.api.ts` có `startVacation`, `endVacation` (cờ `vacation` — mock) | `PUT / DELETE /seller/vacation`, kiểm đơn chưa bàn giao (`409 GNE-VND-4092`), job kết thúc, ẩn voucher shop khi nghỉ (`/public/vouchers` B5), kiểm lại checkout (`GNE-ORD-4093`) |
| **Phân tích shop** | `vendor/analytics/{api,application}` chỉ `.gitkeep`; `product_view_stats` + job ghi gộp lượt xem 5 phút (plan 2026-11-17 — có nguồn truy cập `source`); `orders.auto_confirmed` (C4) | API 10.2, 10.9 (trừ sentiment) |
| **Dashboard sàn** | `admin/dashboard/{api,application}` chỉ `.gitkeep`; `admin-dashboard/queues.api.ts` (`getAdminQueues`) — kiểm đã nối thật chưa (Q00.04) | API 11.2 |
| **Sổ cái** | `financial_ledger` + `LedgerPoster` (C3) | API 11.9 (đọc, lọc, CSV) |
| **Phụ thuộc module** | `vendor → admin, auth, catalog, notification, social`; `admin → auth, catalog`; `order → vendor` | **`vendor` / `admin` không import `order` / `payment`** (vòng) → báo cáo bằng **SQL read-model**; kiểm đơn mở khi tạm nghỉ → cổng `VacationOrderGuard` (vendor khai, order cài) |
| **Quyền** | `VIEW_DASHBOARD`, `VIEW_FINANCE` | — |
| **Web** | `seller-dashboard/dashboard.api.ts` (`getDashboard`), `seller-analytics/analytics.api.ts` (`getRevenue`, `getAnalyticsOverview`, `getTrafficSources`, `getTopProducts`; `getSentiment` — C7), `admin-dashboard/dashboard.api.ts` (`getDashboardFinance`, `getGmvChart`, `getOperations`, `listGrowingShops`), `admin-finance/finance.api.ts` (`listLedger`, `listAllLedger`) — mock | Nối theo cờ (D15) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Báo cáo là **read-model SQL** (một lớp `*ReportQueries` mỗi màn, câu SQL có tên, tham số hóa) — không nạp entity hàng loạt, không import module khác | README mục 3; CLAUDE bẫy "màn danh sách dùng projection" |
| N2 | Mọi mốc thời gian theo **giờ Việt Nam** (`Asia/Ho_Chi_Minh`): "hôm nay", "tháng này", nhóm theo ngày / tuần / tháng | TS 8 |
| N3 | Định nghĩa chỉ số ghi **một lần** trong ENTITY / API (mục 2 dưới) và test bằng dữ liệu dựng tay có số biết trước | UC-VND-08, ADM-06 |
| N4 | Cache Redis 5 phút cho tổng hợp nặng (`JsonCache`), Redis lỗi → đọc CSDL; không cache sổ cái / CSV | UC-ADM-06 b2 |
| N5 | Tiền trả số nguyên VND; tỉ lệ trả số thập phân 0–100 (1 chữ số lẻ) | TS 8 |
| N6 | Tạm nghỉ: kiểm đơn mở + ghi trong **một** transaction; xóa cache trang shop / sản phẩm / voucher công khai **sau commit** | UC-VND-19 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Bật tạm nghỉ** (API 10.11) | UC-VND-19 b1–2 | `PUT /seller/vacation {ends_at, message ≤ 300}` — `ends_at` ∈ (now, now + 30 ngày] (giờ VN, làm tròn 00:00 ngày mở lại); bắt đầu **ngay**; còn đơn `CONFIRMED` / `PREPARING` chưa bàn giao **hoặc** đơn mới chưa xác nhận (`PAID`, COD `PENDING_PAYMENT` — UC chỉ ghi hai trạng thái đầu, thêm hai trạng thái này vì shop nghỉ thì đơn mới không ai xác nhận) → `409 GNE-VND-4092` + `details.orders[]` (mã đơn, trạng thái); đang nghỉ mà gọi lại → cập nhật `ends_at` / lời nhắn. Chỉ shop `VERIFIED` (shop chưa được bán thì không cần chế độ nghỉ) | ☐ |
| D2 | **Cổng kiểm đơn mở** | N1; README mục 3 | `VacationOrderGuard { List<OpenOrderRef> blockingOrders(vendorUserId) }` (vendor khai, order cài — `order → vendor` đã có; dùng lại `OpenOrders` của C2 với tập trạng thái D1). Bản mặc định (test vendor) trả rỗng | ☐ |
| D3 | **Khi đang nghỉ** (UC-VND-19 b3) | UC-VND-19 | Đã có: chặn thêm giỏ / mua (`PurchaseRules`), băng nghỉ, chat tự trả lời. Thêm: (a) checkout chứa sản phẩm của shop nghỉ → `409 GNE-ORD-4093` (kiểm `CheckoutPricer` / `ShopFacade.buyInfo` đã chặn chưa — Q00.04; chưa thì thêm một kiểm tra vào `PurchaseRules` dùng chung xem trước / đặt hàng); (b) voucher của shop **ẩn** khỏi `GET /public/vouchers?shop_id=` và bộ lọc "Shop có voucher" — read-model của promotion thêm điều kiện `vacation` (file của B5 — ghi ở PR); (c) shop vẫn **xem** được Seller Center, đơn cũ vẫn xử lý tiếp | ☐ |
| D4 | **Kết thúc** | UC-VND-19 b4; API 12.2 (hằng giờ) | `DELETE /seller/vacation` → `endVacation()` ngay (`204`); `VacationEndJob` mỗi giờ phút 0 (giờ VN): `vacation_ends_at <= now` → `endVacation()` (`SKIP LOCKED`); sau commit xóa cache shop (`PublicShopService.evict`), sản phẩm của shop (`ProductDetailService` theo vendor — kiểm có hàm evict theo shop chưa), voucher công khai của shop, bộ lọc "Shop có voucher" | ☐ |
| D5 | **Tổng quan Seller** (API 10.2) | UC-VND-08 b9 | `GET /seller/dashboard`: doanh thu tháng (Σ `gross_amount` ký quỹ của đơn `COMPLETED` theo `completed_at` tháng này), số dư khả dụng + escrow đang giữ (cùng công thức C9 D1 — gọi chung read-model), đếm đơn theo nhóm (chờ xác nhận / chờ đóng gói / đang giao / hoàn trả – hủy 30 ngày), 5 đơn gần đây, lối tắt. `GET /seller/analytics/revenue?range=7d|30d|1y` → chuỗi theo ngày (7d / 30d) hoặc tháng (1y): `gmv`, `orders` | ☐ |
| D6 | **Phân tích Seller** (API 10.9) | UC-VND-08 b2–8 | `range` ∈ `today`, `7d`, `30d`, `custom` (`from`, `to` ≤ 365 ngày). `GET /seller/analytics/overview`: **GMV** = Σ `orders.sub_total` của đơn đã thanh toán / COD đã xác nhận trong kỳ (không tính đơn hủy trước khi trả tiền); **doanh thu thuần** = GMV − hoa hồng − voucher shop (voucher sàn / Điểm do sàn chịu — không trừ); đơn mới / hoàn tất / bị hủy / trả hàng; **lượt truy cập** (Σ `product_view_stats.view_count` sản phẩm của shop), **tỉ lệ chuyển đổi** = số đơn đã thanh toán / số khách truy cập (`unique_visitors` nếu có, không thì lượt xem — ghi rõ); khối **Kết quả đơn 30 ngày** + **sức khỏe** (`health_30d`: tỉ lệ shop tự hủy / từ chối, số **khiếu nại shop thua** (`refund_requests` phán quyết có lợi khách — C8), số **đơn sàn tự xác nhận** (`orders.auto_confirmed` — C4)). `GET …/traffic-sources` (4 nguồn theo `product_view_stats.source`). `GET …/top-products?limit=5` (top SKU theo doanh thu + tỉ lệ đóng góp). Kèm `compare` với kỳ trước cùng độ dài | ☐ |
| D7 | **Tổng quan Admin** (API 11.2) | UC-ADM-06 b2–6 | `GET /admin/dashboard/finance?month=YYYY-MM` (`VIEW_FINANCE`): **GMV** (Σ `grand_total` đơn `COMPLETED` / `DELIVERED` trong kỳ — đúng UC), **doanh thu hoa hồng** (Σ `commission_fee` ký quỹ `RELEASED` trong kỳ), **chi phí sàn tài trợ** (Σ `order_discounts.discount_amount` `funded_by` ∈ `PLATFORM` (+ phần sàn của `CO_SPONSOR`) + freeship + Điểm), **lợi nhuận ròng** = hoa hồng − chi phí sàn − phí cổng (đọc sổ cái `GATEWAY_FEE`), **escrow đang giữ** (Σ `gross_amount` `HELD` / `DISPUTED` / `FROZEN`), **COD chờ đối soát** (Σ `cod_funded_amount` `PENDING_FUNDING`), **nợ vendor** (Σ số dư ví + escrow `HELD`), **tiền chờ hoàn** (Σ yêu cầu `APPROVED` chưa `REFUNDED`); `GET …/gmv-chart?range=6m|12m|week` (`VIEW_FINANCE`); `GET …/operations` (`VIEW_DASHBOARD`: đơn theo trạng thái, tỉ lệ hủy, top 10 shop theo GMV, top 10 sản phẩm, **việc tồn**: KYC chờ, sản phẩm chờ duyệt, lệnh rút chờ, tranh chấp mở); `GET …/growing-shops?category_id&q&page&size=5` (tăng trưởng GMV so kỳ trước, lọc ngành, tìm tên shop bỏ dấu); `GET …/export?month` (`VIEW_FINANCE`, CSV chi tiết theo đơn: mã đơn, shop, ngày, tổng, hoa hồng, giảm sàn, trạng thái ký quỹ) | ☐ |
| D8 | **Sổ cái** (API 11.9) | UC-ADM-06 b7; UC-PAY-06 b4 | `GET /admin/ledger-entries?event_type&q&from&to&cursor&limit=50` (`VIEW_FINANCE`) theo `seq` giảm dần, cursor theo `seq`; `q` tìm `reference_id` / mã đơn; mỗi dòng: thời gian, sự kiện, tham chiếu, tài khoản, Nợ, Có; `summary` Σ Nợ / Σ Có của bộ lọc (phải bằng nhau khi lọc theo nhóm tham chiếu). `GET …/export` → CSV `so-cai-tai-chinh.csv` (stream, không giới hạn 50). **Controller ở module `payment`** (chủ bảng) dù đường dẫn `/admin/**` | ☐ |
| D9 | **Định nghĩa "trong kỳ"** | N2 | Đơn tính theo mốc: GMV Seller → `payment_confirmed_at` (online) / `confirmed_at` (COD); GMV Admin → `completed_at` / `delivered_at`; hoa hồng → `released_at`; hủy → `cancelled_at`; trả hàng → `requested_at`. Ghi bảng định nghĩa vào API 10.9 / 11.2 (tránh hai màn hiểu khác nhau) | ☐ |
| D10 | **Hiệu năng** | N4 | Index cần thêm đo bằng `EXPLAIN ANALYZE` trên dữ liệu 10.000 đơn dựng tay (script `scripts/dev/seed-orders-bulk.sql` — **không** đưa vào `db/seed`); cần index → đăng ký số migration ở README mục 5 lúc đó; mục tiêu mỗi endpoint < 300 ms (không cache) | ☐ |
| D11 | **Cache** | N4 | `analytics:vendor:{id}:{endpoint}:{range}` 5 phút; `dashboard:admin:{endpoint}:{params}` 5 phút; không xóa chủ động (số liệu báo cáo chấp nhận trễ 5 phút — ghi API) | ☐ |
| D12 | **Quyền** | API 11.2 | `finance`, `gmv-chart`, `export`, `ledger-entries*` → `VIEW_FINANCE`; `operations`, `growing-shops` → `VIEW_DASHBOARD`; Seller → shop `VERIFIED` (cổng KYC sẵn có) | ☐ |
| D13 | **Khi chưa có dữ liệu** | UC-VND-20 tương tự | Mọi số trả `0` / mảng rỗng (không `null`), chuỗi thời gian đủ điểm (điền 0) để biểu đồ không gãy | ☐ |
| D14 | **Khối Cảm xúc** | UC-VND-08 b8 | Dùng `GET /seller/analytics/sentiment` của C7 — plan này chỉ đặt chung màn | ☐ |
| D15 | **Cờ web** | — | `vacation` (đã có — Seller › Cài đặt › Chế độ nghỉ), `seller-dashboard`, `seller-analytics`, `admin-dashboard`, `ledger` (Admin › Sổ cái tài chính) | ☐ |
| D16 | **Ngoài phạm vi** | — | Báo cáo theo khung giờ, xuất PDF, so sánh nhiều shop cho Seller, BI ngoài | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

```
Tạm nghỉ: PUT /seller/vacation (D1) → VacationOrderGuard.blockingOrders (order cài, D2) → rỗng → VendorProfile.startVacation
          → AFTER_COMMIT evict shop / sản phẩm / voucher (D4)        DELETE /seller/vacation · VacationEndJob mỗi giờ (D4)
Seller:   GET /seller/dashboard · /seller/analytics/revenue (D5) · /seller/analytics/overview | traffic-sources | top-products (D6)
          → SellerReportQueries (read-model, giờ VN, cache 5 phút)
Admin:    GET /admin/dashboard/finance | gmv-chart | operations | growing-shops | export (D7) → AdminDashboardQueries
          GET /admin/ledger-entries (+ export) (D8) → payment/ledger/api
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/java/com/genie/
├── vendor/settings/application/  VacationService · VacationEndJob · VacationOrderGuard (cổng) · VendorSettingsPorts (mặc định)
├── vendor/settings/api/          SellerVacationController (PUT / DELETE /seller/vacation)
├── vendor/analytics/application/ SellerReportQueries · SellerDashboardService · SellerAnalyticsService · ReportRange
├── vendor/analytics/api/         SellerDashboardController · SellerAnalyticsController
├── order/order/application/      VacationOrderGuardImpl (cài cổng)
├── order/cart/application/       PurchaseRules (sửa nếu checkout chưa chặn — D3a)
├── promotion/voucher/application/ VoucherReadModel / PromotionSearchPorts (sửa — ẩn voucher shop đang nghỉ, D3b)
├── admin/dashboard/application/  AdminDashboardQueries · AdminDashboardService · DashboardCsv
├── admin/dashboard/api/          AdminDashboardController
└── payment/ledger/api/           AdminLedgerController · LedgerEntryQuery · LedgerCsv
server-spring/src/test/java/com/genie/…
    ReportRangeTest · VacationIT · VacationEndJobIT · SellerDashboardIT · SellerAnalyticsIT · AdminDashboardIT · AdminLedgerIT
    ReportFixtures (dựng bộ đơn / ký quỹ / hoàn tiền có số biết trước — dùng chung các IT báo cáo)
scripts/dev/seed-orders-bulk.sql (đo hiệu năng — không phải seed demo)
web/src/features/seller-dashboard/… · seller-analytics/… · admin-dashboard/… · admin-finance/… (Sổ cái) · seller-settings/… (tạm nghỉ)
docs/Document/… API_SPEC (10.2, 10.9, 10.11, 11.2, 11.9, 12.2 + bảng định nghĩa chỉ số D9) · Usecase.md · ENTITY 1.4 · CHECKLIST · BAO_CAO · xlsx
docs/api-tests/reports.http · docs/kiem-thu/kich-ban-2026-11-28-c10.md
```

---

## 5. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint / tác vụ | Bảng (đọc) | Quyền |
|---|---|---|---|
| UC-VND-19 | `PUT / DELETE /seller/vacation`, `VacationEndJob` | `vendor_profiles` (ghi), `orders` (đọc qua cổng) | VENDOR `VERIFIED` |
| UC-VND-08 b1–8 | `GET /seller/analytics/overview | traffic-sources | top-products` | `orders`, `order_items`, `escrow_ledgers`, `order_discounts`, `product_view_stats`, `refund_requests` | VENDOR |
| UC-VND-08 b9 | `GET /seller/dashboard`, `GET /seller/analytics/revenue` | + `vendor_wallets`, `withdrawal_requests` | VENDOR |
| UC-ADM-06 b1–6 | `GET /admin/dashboard/finance | gmv-chart | operations | growing-shops | export` | `orders`, `escrow_ledgers`, `order_discounts`, `vendor_wallets`, `refund_requests`, `withdrawal_requests`, `vendor_profiles`, `products`, `financial_ledger` | `VIEW_FINANCE` / `VIEW_DASHBOARD` |
| UC-ADM-06 b7 | `GET /admin/ledger-entries` (+ `export`) | `financial_ledger` | `VIEW_FINANCE` |

**Mã lỗi:** `GNE-VND-4092` (409 còn đơn chưa bàn giao — `details.orders`), `GNE-ORD-4093` (409 shop tạm nghỉ khi đặt), `GNE-VAL-4220` (khoảng ngày sai / quá 365 ngày / `ends_at` quá 30 ngày).

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — `ModuleBoundaryTest` (**`vendor` / `admin` không import `order` / `payment`**).
2. Swagger: summary `UC-VND-08`, `UC-VND-19`, `UC-ADM-06`.
3. `docs/api-tests/reports.http`: số của bộ `ReportFixtures` khớp bảng kỳ vọng trong kịch bản.
4. Đụng `web/`: `npm run verify`.
5. Tick progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Qxx.yy` = task; `Qxx.Cn` = task commit.

### Q00 — Chuẩn bị

- **[Q00.01]** Chạy `git config user.name` / `user.email` — ghi vào dòng *Phụ trách* của progress; kiểm những chuỗi nào đã merge (C3–C9) để biết chỉ số nào kiểm được bằng dữ liệu thật.
- **[Q00.02]** Chốt D1–D16 vào progress.
- **[Q00.03]** Tạo nhánh `feature/c10-vacation` từ `develop`.
- **[Q00.04]** Đọc `PurchaseRules` / `CheckoutPricer` (đặt hàng đã chặn shop nghỉ chưa — D3a), read-model voucher công khai + `PromotionSearchPorts` (D3b), các hàm evict cache shop / sản phẩm, cột `product_view_stats` (`source`, `unique_visitors`?), `admin-dashboard/queues.api.ts` đã nối thật chưa.
- **[Q00.05]** Đọc màn mock Seller › Tổng quan, Seller › Phân tích (khoảng thời gian, KPI, biểu đồ, nguồn truy cập, top 5, kết quả đơn, sức khỏe), Admin › Tổng quan (2 tab), Admin › Sổ cái, Seller › Cài đặt › Chế độ nghỉ.

### Q01 — Tài liệu trước

- **[Q01.01]** API_SPEC — 10.11 (D1–D4), 10.2 (D5), 10.9 (D6 + bảng định nghĩa D9), 11.2 (D7, D12), 11.9 (D8), 12.2, lịch sử phiên bản.
- **[Q01.02]** Usecase.md — UC-VND-19 (chặn cả đơn chưa xác nhận — D1, voucher ẩn — D3), UC-VND-08 / UC-ADM-06 (định nghĩa chỉ số D9, trễ 5 phút D11); ENTITY 1.4 (`startVacation` kiểm đơn qua cổng).
- **[Q01.C1]** COMMIT — `docs(vendor): specify vacation mode rules and report metric definitions`

### Q02 — Chế độ tạm nghỉ (UC-VND-19)

- **[Q02.01]** Cổng `VacationOrderGuard` + bản mặc định + `VacationOrderGuardImpl` (order) (D2).
- **[Q02.02]** `VacationService` + `SellerVacationController` (D1, D4) + evict sau commit (N6).
- **[Q02.03]** Chặn đặt hàng (D3a) nếu chưa có; ẩn voucher shop nghỉ (D3b).
- **[Q02.04]** `VacationEndJob` (D4).
- **[Q02.05]** Test — `VacationIT` (còn đơn `PREPARING` → 4092 + danh sách; không còn → nghỉ, trang shop có `vacation`, thêm giỏ bị chặn, đặt hàng → 4093, voucher shop biến khỏi `/public/vouchers?shop_id=` và bộ lọc; `ends_at` 31 ngày → 422; mở lại sớm → 204), `VacationEndJobIT` (quá hạn → hết nghỉ + cache xóa).
- **[Q02.C1]** COMMIT — `feat(vendor): let sellers start and end vacation mode with open order checks`

### Q03 — Tổng quan & phân tích Seller (UC-VND-08) — nhánh `feature/c10-seller-analytics`

- **[Q03.01]** `ReportRange` (preset / custom, giờ VN, kỳ so sánh) + `ReportFixtures` (test).
- **[Q03.02]** `SellerReportQueries` — các câu SQL D5, D6 (có tên, tham số hóa, `EXPLAIN` đạt D10).
- **[Q03.03]** `SellerDashboardService` + `SellerAnalyticsService` + 2 controller (5 endpoint) + cache (D11).
- **[Q03.04]** Test — `ReportRangeTest` (biên 00:00 giờ VN, tháng 28/31 ngày), `SellerDashboardIT`, `SellerAnalyticsIT` (mỗi chỉ số đúng bảng kỳ vọng của `ReportFixtures`: GMV, thuần, hủy / trả, khiếu nại thua, tự xác nhận, nguồn truy cập, top 5 + tỉ lệ đóng góp; shop khác không lẫn số; không dữ liệu → toàn 0 + chuỗi đủ điểm).
- **[Q03.C1]** COMMIT — `feat(vendor): serve seller dashboard and sales analytics from order read models`

### Q04 — Dashboard & Sổ cái Admin (UC-ADM-06) — nhánh `feature/c10-admin-dashboard`

- **[Q04.01]** `AdminDashboardQueries` + `AdminDashboardService` + `AdminDashboardController` (5 endpoint D7, quyền D12, cache D11) + `DashboardCsv`.
- **[Q04.02]** `LedgerEntryQuery` + `AdminLedgerController` (payment) + `LedgerCsv` (stream) (D8).
- **[Q04.03]** Test — `AdminDashboardIT` (mỗi thẻ đúng `ReportFixtures`; lợi nhuận ròng; nợ vendor; việc tồn đếm đúng; tăng trưởng shop + lọc ngành + tìm bỏ dấu; `VIEW_DASHBOARD` không gọi được `finance` → 403), `AdminLedgerIT` (cursor không trùng / sót khi có dòng mới chèn giữa hai trang; lọc sự kiện; `summary` cân; CSV đủ dòng).
- **[Q04.C1]** COMMIT — `feat(admin): serve platform finance and operations dashboards`
- **[Q04.C2]** COMMIT — `feat(payment): list and export financial ledger entries for staff`

### Q05 — Nối web (cờ `vacation`, `seller-dashboard`, `seller-analytics`, `admin-dashboard`, `ledger`)

- **[Q05.01]** `seller-settings/settings.api.ts` (`startVacation`, `endVacation`) — hộp xác nhận, lỗi `4092` hiện danh sách đơn cần xử lý (link sang đơn).
- **[Q05.02]** `seller-dashboard` + `seller-analytics` (4 hàm; `getSentiment` đã nối ở C7) — bộ chọn khoảng thời gian, biểu đồ, so sánh kỳ trước.
- **[Q05.03]** `admin-dashboard` (4 hàm + CSV) + `admin-finance` Sổ cái (`listLedger`, `listAllLedger` → cursor, lọc, xuất).
- **[Q05.04]** vi / en + `error-codes.ts`; test mapper; kiểm 9 theme (biểu đồ đọc được ở theme tối), 360 px; `npm run verify`.
- **[Q05.C1]** COMMIT — `feat(web): connect vacation mode, seller analytics, admin dashboard and ledger to the real api`

### Q06 — Kiểm thử & chạy thật

- **[Q06.01]** `docs/api-tests/reports.http` — mọi endpoint mục 5.
- **[Q06.02]** `docs/kiem-thu/kich-ban-2026-11-28-c10.md` — TC-C10-01…: bảng số kỳ vọng tính tay từ đơn thật đã chạy ở C3–C9 (ghi mã đơn); bật nghỉ bị chặn rồi xử lý đơn xong bật được; khách thấy băng nghỉ, không mua được, không thấy voucher shop; hết hạn tự mở; Admin hai tab + CSV mở bằng Excel; Sổ cái lọc + xuất; 9 theme; 360 px.
- **[Q06.03]** Chạy thật — web build `PUBLIC_REAL_API=…,vacation,seller-dashboard,seller-analytics,sentiment,admin-dashboard,ledger` + backend `local` + Postgres + Redis + Silo + Mailpit; Chromium (Playwright) với chủ shop, Admin, Finance Officer; mỗi lỗi một commit `fix(…)`.
- **[Q06.C1]** COMMIT — `docs(vendor): add report api tests and real-run scenarios`

### Q07 — Hoàn thiện use case & tick tiến độ

- **[Q07.01]** OpenAPI — summary `UC-VND-08`, `UC-VND-19`, `UC-ADM-06`.
- **[Q07.02]** `docs/web-backend-handoff.md` — 5 cờ đã nối.
- **[Q07.03]** Usecase.md — *Hiện trạng* 3 UC.
- **[Q07.04]** `CHECKLIST_TIEN_DO_UC.md` — tick 5 lớp 3 UC, Trạng thái, **cột Phụ trách = tên ở Q00.01**, số mục 1 + phân hệ 05 / 11, dòng mục 5 *Nhật ký*.
- **[Q07.05]** `BAO_CAO_UC_DOC_LAP.md` — mục C10 + dòng *Phụ trách*.
- **[Q07.06]** `Quản Lý UseCase.xlsx` — 3 UC, cột AA.
- **[Q07.07]** README ngày (mục 7) — tick dòng C10.
- **[Q07.C1]** COMMIT — `docs(vendor): record vacation mode, seller analytics and platform dashboard as implemented`
- **[Q07.08]** Mở 3 PR xếp chồng vào `develop` (`c10-vacation` → `c10-seller-analytics` → `c10-admin-dashboard`).

---

## 8. Ngoài phạm vi

- Báo cáo theo khung giờ, PDF, BI ngoài; khối Cảm xúc (C7).

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Hai màn tính cùng chỉ số khác nhau | Bảng định nghĩa D9 ở API + `ReportFixtures` dùng chung |
| Câu tổng hợp chậm khi dữ liệu lớn | Đo `EXPLAIN` với 10.000 đơn (D10), cache 5 phút |
| Vòng phụ thuộc khi báo cáo cần đơn / tiền | Read-model SQL (N1); cổng `VacationOrderGuard` |
| Lệch múi giờ ở biên ngày / tháng | `ReportRange` + test biên 00:00 giờ VN |
| Shop nghỉ khi còn đơn mới chưa xác nhận | D1 chặn cả `PAID` / COD chờ xác nhận |
