# 🎯 BẢN ĐẶC TẢ KỸ THUẬT & LỘ TRÌNH TRIỂN KHAI — BẢN TLCN (IMPLEMENTATION BLUEPRINT)
## Dự án Sàn TMĐT Đa Người Bán Genie — Phạm vi Đồ án

> **Phạm vi:** 87 use case · 11 phân hệ · 16 tác nhân · 65 bảng · 1 ứng dụng Spring Boot (modular monolith) + 1 AI Worker (KYC + PhoBERT phân tích cảm xúc) + Gemini API (kiểm duyệt sản phẩm) + 1 frontend · 8 sprint × 2 tuần · triển khai Docker Compose trên 1 VPS. Phủ toàn bộ giao diện prototype Genie (`genie/ui_kits/`).
> **Nguồn đồng bộ:** [Usecase.md](diagrams/UseCaseDiagram/Usecase.md) · [ENTITY.md](diagrams/ClassDiagram/ENTITY.md) · [Document_Vi.md](document/Document_Vi.md). Bản hoàn chỉnh cho đồ án tốt nghiệp (206 use case, 100.000 CCU) ở `DATN/implement.md`.
> **Nguyên tắc:** làm **đúng và đủ** vòng nghiệp vụ trước, tối ưu sau. Không thêm hạ tầng (Kafka, Elasticsearch, Kubernetes…) khi chưa có số liệu chứng minh cần.

---

## 📑 MỤC LỤC

* [PHẦN 1: Phạm vi tính năng & Lộ trình 16 tuần](#phan-1)
* [PHẦN 2: Cấu trúc mã nguồn & Quy ước module](#phan-2)
* [PHẦN 3: Cơ sở dữ liệu, Migration & Dữ liệu mẫu](#phan-3)
* [PHẦN 4: Xác thực, Phân quyền & Bảo mật](#phan-4)
* [PHẦN 5: Danh mục, Sản phẩm, Tìm kiếm full-text & Kiểm duyệt Gemini](#phan-5)
* [PHẦN 6: Giỏ hàng, Checkout đa shop & Giữ chỗ tồn kho](#phan-6)
* [PHẦN 7: Thanh toán VNPay, VietQR qua SePay & Ký quỹ Escrow](#phan-7)
* [PHẦN 8: Gian hàng & KYC theo tầng (AI Worker)](#phan-8)
* [PHẦN 9: Vận chuyển đa hãng 3PL & Đối soát COD](#phan-9)
* [PHẦN 10: Voucher 3 tầng, Điểm, Flash Sale & Phân bổ chiết khấu](#phan-10)
* [PHẦN 11: Chat & Thông báo thời gian thực](#phan-11)
* [PHẦN 12: Trả hàng, Hoàn tiền & Phân xử](#phan-12)
* [PHẦN 13: Quản trị sàn, Nhân viên & RBAC, Rút tiền & Dashboard](#phan-13)
* [PHẦN 14: Frontend (Storefront, Kênh Người bán, Admin)](#phan-14)
* [PHẦN 15: Điểm xuất phát & Rủi ro khi xây mới](#phan-15)
* [PHẦN 16: Kiểm thử, Triển khai & Checklist demo](#phan-16)
* [PHẦN 17: Danh mục API](#phan-17)

---

<a id="phan-1"></a>
## 🚀 PHẦN 1: PHẠM VI TÍNH NĂNG & LỘ TRÌNH 16 TUẦN

### 1.1 Phạm vi tính năng (tóm tắt theo phân hệ)

| Phân hệ | Use case MUST | Use case SHOULD | Ghi chú triển khai (xây mới) |
|---|---|---|---|
| 01 AUTH | AUTH-01, AUTH-02, AUTH-04, AUTH-06, AUTH-08, AUTH-09, AUTH-11 | AUTH-03, AUTH-10 | OTP email + Redis, Argon2id, JWT 15 phút + refresh token xoay vòng; OAuth2 Google / Apple (PKCE); xóa tài khoản = ẩn danh hóa sau 30 ngày |
| 02 CAT | CAT-01, CAT-02, CAT-05, CAT-06, CAT-09, CAT-10, CAT-11, CAT-13, CAT-14, CAT-16 | CAT-04, CAT-07, CAT-08, CAT-12, CAT-15, CAT-17, CAT-18 | Full-text PostgreSQL + `search_keyword_stats`; Gemini chấm trước, Moderator duyệt cuối; deeplink + Open Graph; lượt xem đếm Redis |
| 03 ORD | ORD-01, ORD-04, ORD-06, ORD-07, ORD-08, ORD-09, ORD-10, ORD-11, ORD-12 | — | Tách giỏ theo shop, giữ chỗ có điều kiện, sweeper, tự hoàn tất |
| 04 PAY | PAY-01, PAY-03, PAY-05, PAY-06, PAY-07 | — | VNPay + VietQR qua SePay + COD kết hợp Escrow; IPN / webhook idempotent; sổ cái kép chỉ ghi thêm |
| 05 VND | VND-01, VND-02, VND-03, VND-04, VND-05, VND-07 | VND-06, VND-08, VND-19, VND-20, VND-21 | AI Worker KYC theo tầng (QR + MRZ + ONNX) — điểm nhấn kỹ thuật; ví & sổ giao dịch; cài đặt gian hàng |
| 06 LOG | LOG-01, LOG-02, LOG-03, LOG-16 | LOG-15 | Shop xác nhận / từ chối đơn; adapter đa hãng GHN / GHTK / ViettelPost / J&T; webhook idempotent; đối soát COD |
| 07 PRO | PRO-01, PRO-02, PRO-03 | PRO-04, PRO-05, PRO-06, PRO-08, PRO-09, PRO-10 | Voucher 3 tầng + phạm vi; Điểm (12 tháng, tối đa 50%); Flash Sale UPDATE có điều kiện |
| 08 SOC | SOC-03, SOC-04 | SOC-01, SOC-02, SOC-05, SOC-06, AI-07 | Chat STOMP (văn bản, ảnh, thẻ sản phẩm); Hữu ích; báo cáo đánh giá; theo dõi shop; PhoBERT chấm cảm xúc đánh giá (Phần 8.5) |
| 09 SUP | SUP-01, SUP-02, SUP-03, SUP-04 | — | Máy trạng thái hoàn tiền nhiều nhánh; hoàn Điểm theo phân bổ |
| 10 NOT | NOT-01, NOT-03 | NOT-05 | In-app WebSocket, email, thông báo toàn sàn (fan-out on read) |
| 11 ADM | ADM-01, ADM-02, ADM-03, ADM-04, ADM-05 | ADM-06, ADM-07, ADM-12 | Nhân viên 3 chức danh + 13 mã quyền; rút tiền + xác nhận SePay; dashboard SQL; nhật ký; banner |

### 1.2 Lộ trình 8 sprint × 2 tuần

| Sprint | Tuần | Mục tiêu (Definition of Done) | Số UC | Use case |
|:---:|:---:|---|:---:|---|
| **S1** | 1–2 | **Nền tảng tài khoản & phân quyền** — Repo, Docker Compose local, Flyway V1–V12 (65 bảng), Spring Security + JWT + RBAC 13 quyền, seed Admin, mời nhân viên; CI chạy test; storefront khung + đăng nhập | 7 | AUTH-01, AUTH-02, AUTH-04, AUTH-06, AUTH-11, ADM-01, ADM-02 |
| **S2** | 3–4 | **Danh mục, sản phẩm & KYC** — Danh mục, sản phẩm, tìm kiếm, trang shop; mở shop + KYC (nộp hồ sơ, AI Worker, duyệt tay); kho GHN; hàng chờ duyệt sản phẩm | 17 | AUTH-08, AUTH-09, CAT-01, CAT-02, CAT-05, CAT-06, CAT-09, CAT-10, CAT-11, CAT-13, CAT-14, CAT-16, VND-01, VND-02, VND-03, VND-04, VND-05 |
| **S3** | 5–6 | **Giỏ hàng, đặt hàng & thanh toán** — Giỏ hàng, checkout tách đơn, giữ chỗ, sweeper, phí ship; VNPay + VietQR (SePay) + COD, IPN / webhook, ký quỹ, sổ cái kép | 9 | ORD-01, ORD-04, ORD-06, ORD-07, ORD-08, PAY-01, PAY-03, PAY-05, PAY-06 |
| **S4** | 7–8 | **Vận chuyển, ví & rút tiền** — Shop xác nhận / từ chối đơn, vận đơn GHN, webhook, đối soát COD; xác nhận nhận hàng, tự hoàn tất, giải ngân; ví shop, rút tiền | 12 | ORD-09, ORD-10, ORD-11, ORD-12, PAY-07, VND-07, LOG-01, LOG-02, LOG-03, LOG-16, ADM-04, ADM-05 |
| **S5** | 9–10 | **Khuyến mãi, chat, đánh giá & trả hàng** — Voucher shop / sàn, kho voucher; trả hàng / hoàn tiền / phân xử; đánh giá + Hữu ích; thông báo in-app + email | 11 | PRO-01, PRO-02, PRO-03, SOC-03, SOC-04, SUP-01, SUP-02, SUP-03, SUP-04, NOT-01, NOT-03 |
| **S6** | 11–12 | **Thông báo, quản trị, phân tích cảm xúc & hoàn thiện** — Chat, yêu thích, phân tích bán hàng (kèm tỉ lệ cảm xúc đánh giá), PhoBERT trong AI Worker, dashboard, khóa tài khoản; kiểm thử E2E vòng lõi; triển khai VPS lần 1 | 6 | CAT-08, VND-08, SOC-01, ADM-03, ADM-06, AI-07 |
| **S7** | 13–14 | **Mở rộng tài khoản, sản phẩm, gian hàng & quản trị (đối chiếu giao diện)** — Đăng nhập Google / Apple, xóa tài khoản; từ khóa gợi ý, sản phẩm tương tự, Gemini, thuộc tính ngành, đề xuất danh mục, deeplink; tự trả lời, tạm nghỉ, ví & sổ giao dịch, cài đặt gian hàng; đa hãng 3PL; nhật ký hệ thống | 14 | AUTH-03, AUTH-10, CAT-04, CAT-07, CAT-12, CAT-15, CAT-17, CAT-18, VND-06, VND-19, VND-20, VND-21, LOG-15, ADM-07 |
| **S8** | 15–16 | **Khuyến mãi nâng cao, Điểm, tương tác & nội dung sàn** — Xếp chồng 3 tầng + Điểm, Flash Sale, điểm danh / lịch sử / đổi Điểm; thẻ sản phẩm trong chat, báo cáo đánh giá, theo dõi shop; thông báo sàn, banner; kiểm thử tải / bảo mật, seed demo, hoàn thiện báo cáo | 11 | PRO-04, PRO-05, PRO-06, PRO-08, PRO-09, PRO-10, SOC-02, SOC-05, SOC-06, NOT-05, ADM-12 |

> S1–S6 là **vòng nghiệp vụ lõi** (57 use case của bản v1.0 + phân quyền, sổ cái, xác nhận đơn); S7–S8 phủ phần còn lại của giao diện. Trễ tiến độ thì cắt từ cuối S8 ngược lên (Document_Vi Phụ lục C), không cắt luồng tiền và phân quyền.

### 1.3 Phân công gợi ý (4 thành viên)

| Vai trò | Phụ trách chính | Phụ trách phụ |
|---|---|---|
| Backend 1 | Đơn hàng, thanh toán, ký quỹ, sổ cái, hoàn tiền, voucher 3 tầng + Điểm, Flash Sale (luồng tiền) | Kiểm thử tích hợp luồng tiền |
| Backend 2 | Auth (+ OAuth, RBAC), catalog, tìm kiếm, Gemini, đa hãng 3PL, thông báo | CI/CD, triển khai |
| AI + Backend 3 | AI Worker KYC + PhoBERT cảm xúc đánh giá, gian hàng, chat, đánh giá, admin (nhân viên, nhật ký, banner) | Seed dữ liệu, k6 |
| Frontend | Storefront Astro, Kênh Người bán, Admin Portal (nav theo quyền), i18n vi / en | Playwright E2E |

---

<a id="phan-2"></a>
## 🧱 PHẦN 2: CẤU TRÚC MÃ NGUỒN & QUY ƯỚC MODULE

### 2.1 Cấu trúc repository

> Bản chi tiết và cập nhật (repo `genie-platform`, tổ chức theo tính năng `module/feature/layer`, luật ArchUnit): [TECH_STACK_VA_QUY_UOC.md](./TECH_STACK_VA_QUY_UOC.md) mục 2, 4, 5.

```
genie/
├── server-spring/                  # Spring Boot 3.3, Java 21, Maven
│   └── src/main/java/com/genie/
│       ├── common/                 # ApiResponse, ErrorCode (GNE-*), BaseEntity, security, utils
│       ├── auth/                   # UC-AUTH-* (+ oauth: GoogleIdTokenVerifier, Apple JWKS)
│       ├── catalog/                # UC-CAT-* (+ moderation/GeminiModerationClient, stats/ViewCounterFlusher)
│       ├── order/                  # UC-ORD-* (cart, checkout, order, reservation)
│       ├── payment/                # UC-PAY-* (vnpay, sepay, escrow, ledger, vendor wallet)
│       ├── vendor/                 # UC-VND-* (shop, kyc)
│       ├── shipping/               # UC-LOG-* (CarrierAdapter: ghn / ghtk / vtp / jt, webhook, cod reconciliation)
│       ├── promotion/              # UC-PRO-* (voucher stacking, flashsale, coin)
│       ├── social/                 # UC-SOC-* (chat, review)
│       ├── support/                # UC-SUP-* (refund, dispute)
│       ├── notification/           # UC-NOT-*
│       └── admin/                  # UC-ADM-* (staff, rbac, payout, dashboard, audit, banner)
│   └── src/main/resources/db/migration/   # Flyway V1__…V12__, R__seed_demo.sql
├── server-python/                  # AI Worker KYC — FastAPI
│   └── app/{main.py, kyc/{qr.py, mrz.py, face.py, scoring.py}, models/*.onnx}
├── web/                            # Astro + React + Tailwind
│   └── src/{pages/, components/, islands/, seller/, admin/, i18n/{vi,en}.json, lib/api.ts}
├── deploy/{docker-compose.yml, docker-compose.local.yml, nginx/, backup.sh}
└── .github/workflows/{ci.yml, release.yml}
```

### 2.2 Quy ước module (modular monolith)

Mỗi module có cấu trúc `api/` (controller + DTO), `application/` (service, event listener), `domain/` (entity, repository — **chỉ module sở hữu được dùng**), `infra/` (client ngoài). Module khác chỉ gọi qua interface công khai, ví dụ `order` gọi `PaymentFacade`, `InventoryFacade`; không inject `ProductVariantRepository` của `catalog`. Kiểm tra bằng **ArchUnit**:

```java
@ArchTest
static final ArchRule modules_only_talk_through_facades =
    noClasses().that().resideInAPackage("com.genie.(*)..")
        .should().dependOnClassesThat().resideInAPackage("com.genie.(*).domain..")
        .as("Module khác chỉ được gọi qua *.application.*Facade");   // ngoại lệ: chính module đó
```

### 2.3 Sự kiện nội bộ

```java
public record OrderPaidEvent(Long sessionId, List<Long> orderIds) {}

@Component @RequiredArgsConstructor
class NotifyVendorOnOrderPaid {
    private final NotificationService notifications;
    @Async @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(OrderPaidEvent e) { e.orderIds().forEach(notifications::notifyNewOrderForVendor); }
}
```

> Quy tắc: **việc liên quan tiền / tồn kho không đặt trong listener bất đồng bộ** — làm trong transaction chính. Listener chỉ dùng cho thông báo, email, cache.

---

<a id="phan-3"></a>
## 🗃️ PHẦN 3: CƠ SỞ DỮ LIỆU, MIGRATION & DỮ LIỆU MẪU

### 3.1 Danh sách migration (khớp 12 nhóm của ENTITY.md — 65 bảng)

| File | Bảng |
|---|---|
| `V1__identity.sql` | users, customer_profiles, addresses, vendor_profiles, admin_profiles, kyc_results, refresh_tokens, staff_permissions |
| `V2__catalog.sql` | categories, products, product_variants, product_images, user_wishlists, category_attributes, brands, product_documents, category_proposals, product_moderation_results, search_keyword_stats, product_view_stats (+ `search_vector`, trigger) |
| `V3__cart.sql` | carts, cart_items |
| `V4__promotion.sql` | vouchers, voucher_usages, user_vouchers, voucher_categories, voucher_products, voucher_excluded_categories, voucher_excluded_products, flash_sale_campaigns, flash_sale_items, user_coins, coin_transactions, daily_checkins |
| `V5__order.sql` | checkout_sessions, orders, order_items, order_discounts, stock_reservations |
| `V6__payment.sql` | payment_transactions, payment_refunds, escrow_ledgers, vendor_wallets, wallet_transactions, withdrawal_requests, financial_ledger, cod_remittances |
| `V7__shipping.sql` | shipment_trackings, shipment_events, vendor_shipping_config |
| `V8__refund.sql` | refund_requests, refund_request_items, refund_evidences |
| `V9__review.sql` | product_reviews, shop_follows, review_reports, review_helpful_votes |
| `V10__notification.sql` | notifications, platform_notifications, user_platform_notification_reads, home_banners, admin_notifications |
| `V11__chat.sql` | conversations, messages |
| `V12__audit.sql` | system_logs |
| `V13__bootstrap_admin.sql` | Admin đầu tiên từ placeholder Flyway `bootstrap_admin_email` / `bootstrap_admin_password_hash` (bỏ qua khi rỗng / đã có) |
| `V14__auth_additions.sql` | Bổ sung của AUTH (2026-11-14, ADR 0007): `refresh_tokens.family_id` (+ sequence) + `last_used_at`, `users.deletion_requested_at`, `uk_users_email_lower`, 5 index, `CHECK` `system_logs.action_type` +8 giá trị |

> Các cột / bảng của TLCN v1.3 (`cod_remittances`, `addresses.label`, `vendor_profiles.is_favorite`, `categories.description`, `flash_sale_items.status`, `product_reviews.is_anonymous`, `platform_notifications.send_at` + `recipient_user_ids_json`, `messages.order_id`) **đã nằm sẵn trong `V1…V12`** vì migration được viết tay 1-1 từ ENTITY.md v1.3 — kế hoạch `V14__api_sync_v13.sql` cũ không còn. Thay đổi schema từ nay chỉ bằng `V15+` (ENTITY.md sửa trước).
| `R__seed_demo.sql` | *(chỉ chạy khi placeholder `seed_demo = true` — profile `local` / `test`; hiện có: `mai.tran@gmail.com` / `Genie@123` + 2 địa chỉ, Admin, 3 nhân viên — phần còn lại bổ sung theo từng phân hệ)* 1 Admin, 3 nhân viên (MODERATOR, CS_SPECIALIST, FINANCE_OFFICER kèm `staff_permissions` mặc định), 5 shop VERIFIED (có `vendor_shipping_config`), 30 danh mục + thuộc tính, 20 thương hiệu, ~200 sản phẩm, 6 voucher (shop / sàn / freeship), 1 Flash Sale, 3 banner, 20 khách có Điểm |

### 3.2 Tìm kiếm full-text (trong `V2__catalog.sql`)

```sql
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE TEXT SEARCH CONFIGURATION vn_unaccent (COPY = simple);
ALTER TEXT SEARCH CONFIGURATION vn_unaccent ALTER MAPPING FOR hword, hword_part, word WITH unaccent, simple;

ALTER TABLE products ADD COLUMN search_vector tsvector;
CREATE FUNCTION products_search_trg() RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
      setweight(to_tsvector('vn_unaccent', coalesce(NEW.name, '')), 'A') ||
      setweight(to_tsvector('vn_unaccent', coalesce(NEW.brand, '')), 'B') ||
      setweight(to_tsvector('vn_unaccent', left(coalesce(NEW.description, ''), 2000)), 'C');
  RETURN NEW;
END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_products_search BEFORE INSERT OR UPDATE OF name, brand, description
  ON products FOR EACH ROW EXECUTE FUNCTION products_search_trg();
CREATE INDEX idx_products_search ON products USING GIN (search_vector);
CREATE INDEX idx_products_name_trgm ON products USING GIN (name gin_trgm_ops);
```

### 3.3 Quy ước bắt buộc
- Tiền `NUMERIC(19,2)`; thời gian `TIMESTAMPTZ`; enum `VARCHAR` + `CHECK`; khóa chính `BIGINT GENERATED ALWAYS AS IDENTITY`.
- `ddl-auto=validate` ở mọi môi trường (Hibernate chỉ kiểm tra, Flyway mới tạo bảng).
- Không sửa migration đã merge; thay đổi = migration mới.
- Tài khoản Admin đầu tiên tạo bằng migration seed riêng `V13__bootstrap_admin.sql` (email + mật khẩu băm đọc từ biến môi trường qua placeholder Flyway) — không có Super Admin, không có màn hình tự đăng ký Admin.

---

<a id="phan-4"></a>
## 🔐 PHẦN 4: XÁC THỰC, PHÂN QUYỀN & BẢO MẬT

### 4.1 Cấu hình Spring Security

```java
@Bean SecurityFilterChain api(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
    return http.csrf(c -> c.disable())                       // API stateless dùng Bearer; refresh cookie SameSite=Strict
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/v1/auth/**", "/api/v1/public/**", "/api/v1/payments/vnpay/ipn", "/api/v1/payments/sepay/webhook", "/api/v1/shipping/*/webhook/**", "/api/v1/payments/vnpay/return").permitAll()
            .requestMatchers("/api/v1/internal/**").access(internalToken())   // AI Worker callback
            .requestMatchers("/api/v1/seller/**").hasRole("VENDOR")
            .requestMatchers("/api/v1/admin/**").hasRole("STAFF")              // chi tiết từng API: @PreAuthorize theo mã quyền (4.5)
            .anyRequest().authenticated())
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
}
@Bean RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_STAFF\nROLE_VENDOR > ROLE_CUSTOMER");
}
```

> **Hiện thực (2026-11-14, ADR 0008):** `DELETE /auth/sessions/current` là ngoại lệ của `/auth/**` (cần đăng nhập hoặc cookie). Kế thừa vai trò do `JwtAuthFilter` cấp sẵn authority (VENDOR có cả `ROLE_CUSTOMER`, ADMIN có cả `ROLE_STAFF`) thay cho bean `RoleHierarchy`; quyền STAFF nạp qua cổng đảo ngược `common.security.AuthorityProvider` ← `admin.rbac.application.StaffPermissionAuthorityProvider` (cache 60 giây) để `common` không phụ thuộc `admin`. Filter bảo mật không đăng ký là bean (tránh Spring Boot thêm lần hai). Lỗi: `401 GNE-SEC-4010` (`RestAuthEntryPoint`), `403 GNE-SEC-4030` + `ACCESS_DENIED` (`RestAccessDeniedHandler`).

### 4.2 Refresh token xoay vòng + phát hiện dùng lại

Đã hiện thực ở `auth/token/application/SessionService` — mã mẫu đầy đủ ở LLD 11.2. Điểm chính:
- Mỗi phiên có `family_id` (cột thêm bằng `V14`) ổn định qua các lần xoay vòng; access token mang claim `sid = family_id` → biết "phiên này" ở màn Thiết bị đăng nhập.
- Đọc token bằng `SELECT … FOR UPDATE`; token đã `ROTATED` bị dùng lại trong **10 giây** (hai tab) chỉ trả `401`; quá 10 giây → `revokeAllOfUser(REUSE_DETECTED)` + `auth:revoked_before:{userId}` để chặn ngay mọi access token cũ.
- Làm mới trả kèm `user` (web tải lại trang dựng lại phiên); token mới cùng `family_id`, cập nhật `last_used_at`.
- Đăng xuất: thu hồi cả họ theo `sid`, `auth:denylist:{jti}` (TTL = thời gian còn lại của access token).

### 4.3 Rate limit đơn giản bằng Redis

| Endpoint | Giới hạn | Khóa Redis |
|---|---|---|
| `POST /auth/sessions` | 10 / phút / IP | `rl:login:{ip}` |
| `POST /auth/otp-challenges`, `…/resend`, `POST /me/otp-challenges` | 1 / 60 giây / email, 5 / giờ / email | `rl:otp:{email}`, `rl:otp:hour:{email}` |
| `POST /me/password/verify`, `DELETE /me/identities/{provider}` | 10 / phút / tài khoản | `rl:pwverify:{userId}` |
| `GET /public/products`, `GET /public/search/suggestions` | 60 / phút / IP | `rl:search:{ip}` |

```java
boolean allow(String key, int limit, Duration window) {
    Long n = redis.opsForValue().increment(key);
    if (n != null && n == 1) redis.expire(key, window);
    return n != null && n <= limit;          // vượt → 429 GNE-RAT-4290
}
```
Hiện thực (`common.security.RateLimiter`): vượt → `TooManyRequestsException` → `429` kèm header `Retry-After` (TTL còn lại); Redis lỗi → **cho qua** + log cảnh báo (khác idempotency của API tiền: Redis lỗi → `503`).

### 4.4 Chống IDOR
Mọi thao tác của shop kiểm tra quyền sở hữu ở service: `product.getVendorId().equals(currentUser.id())`; đơn hàng: khách xem `customerId`, shop xem `vendorId`. Không dựa vào ID trên URL.

### 4.5 RBAC 2 tầng: vai trò + 13 mã quyền (UC-ADM-01, UC-ADM-02)

```java
// Khi đăng nhập / làm mới token: authority = ROLE_<role> + mã quyền còn hiệu lực của STAFF
Collection<GrantedAuthority> authorities(User u) {
    var a = new ArrayList<GrantedAuthority>(List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())));
    if (u.getRole() == UserRole.ADMIN) EnumSet.allOf(StaffPermissionEnum.class).forEach(p -> a.add(new SimpleGrantedAuthority(p.name())));
    else if (u.getRole() == UserRole.STAFF) permissionRepo.findActiveByStaffId(u.getId()).forEach(p -> a.add(new SimpleGrantedAuthority(p.getPermission().name())));
    return a;
}

@PreAuthorize("hasAuthority('REVIEW_KYC')")    @PostMapping("/admin/kyc/{id}/approve")      ResponseEntity<?> approveKyc(...)
@PreAuthorize("hasAuthority('PROCESS_PAYOUT')") @PostMapping("/admin/withdrawals/{id}/approve") ResponseEntity<?> approvePayout(...)
@PreAuthorize("hasAuthority('MANAGE_RBAC')")    @PutMapping("/admin/staffs/{id}/permissions") ResponseEntity<?> updatePermissions(...)
```

- Bộ quyền mặc định theo chức danh: `StaffTitle.defaultPermissions()` (MODERATOR / CS_SPECIALIST / FINANCE_OFFICER) — bảng ở Document_Vi mục 3.6.
- Đổi quyền → thu hồi refresh token của nhân viên đó để token mới mang quyền mới; access token cũ hết hạn trong ≤ 15 phút.
- `GET /api/v1/admin/me/permissions` trả danh sách quyền để frontend dựng nav; truy cập trái quyền → 403 `GNE-SEC-4030` + `system_logs` `ACCESS_DENIED`.
- Không cấp `MANAGE_RBAC` cho STAFF (`GNE-SEC-4031`); cảnh báo khi một người giữ cả `PROCESS_PAYOUT` và `UPDATE_COMMISSION`.

### 4.6 Đăng nhập Google / Apple (UC-AUTH-03)
Authorization Code + PKCE; `state` lưu Redis 10 phút; xác thực chữ ký `id_token` bằng JWKS của nhà cung cấp, kiểm `aud`, `iss`, `nonce`; tìm theo (`oauthProvider`, `oauthProviderId`) → chưa có thì liên kết theo email đã xác minh hoặc tạo `User` + `CustomerProfile`; cấp token như UC-AUTH-02.

**Đã hiện thực (Google, 2026-11-15/16 — PR #55, #57, #59):** `GoogleIdentityProviderClient` (RestClient đổi `code`, `NimbusJwtDecoder` JWKS); callback cố định `<web>/auth/oauth-callback`, `next` lưu trong `state`; ảnh Google tải về lưu Silo (`UploadService.storeImported`), tên / ảnh chỉ điền khi hồ sơ trống; liên kết ở trang tài khoản: `POST /me/identities/google/authorize` → Google → `POST /me/identities/google` — **email Google phải trùng email tài khoản** (`GNE-AUTH-4098`); gỡ liên kết phải nhập lại mật khẩu; tài khoản chỉ Google đăng nhập mật khẩu → `GNE-AUTH-4012`. Apple để sau cờ `genie.oauth.apple.enabled = false`.

### 4.7 Hiện trạng phân hệ AUTH (2026-11-16)
Plan chi tiết trong repo: `plan/2026-11-14/auth.md` (task A00–A16, quyết định D2–D22, ADR 0007 / 0008). `./mvnw verify` 71 test xanh. Đã merge vào `develop`: PR #52–#60 — đăng ký, OTP dùng chung + step-up, đăng nhập (mật khẩu / OTP / mở khóa), phiên & thiết bị, quên / đổi / thiết lập mật khẩu, hồ sơ + ảnh đại diện (Silo), đăng nhập + liên kết Google, xóa tài khoản + cron ẩn danh. **Còn:** `GET /me`, đổi email, sổ địa chỉ + danh mục GHN (UC-AUTH-09), Apple, E2E với API thật. Trạng thái từng UC: [CHECKLIST_TIEN_DO_UC.md](../CHECKLIST_TIEN_DO_UC.md).

---

<a id="phan-5"></a>
## 🔍 PHẦN 5: DANH MỤC, SẢN PHẨM, TÌM KIẾM FULL-TEXT & KIỂM DUYỆT GEMINI

### 5.1 Truy vấn tìm kiếm (UC-CAT-02, UC-CAT-05)

```sql
-- :q đã chuẩn hóa; :cat, :minPrice, :maxPrice có thể NULL
SELECT p.id, p.name, p.sold, p.rating, min(v.price) AS min_price,
       ts_rank(p.search_vector, websearch_to_tsquery('vn_unaccent', :q)) AS rank
FROM products p JOIN product_variants v ON v.product_id = p.id AND v.deleted_at IS NULL
WHERE p.status = 'PUBLISHED'
  AND (:q IS NULL OR p.search_vector @@ websearch_to_tsquery('vn_unaccent', :q))
  AND (:cat IS NULL OR p.category_id IN (SELECT id FROM category_tree(:cat)))   -- hàm đệ quy lấy cây con
GROUP BY p.id
HAVING (:minPrice IS NULL OR min(v.price) >= :minPrice) AND (:maxPrice IS NULL OR min(v.price) <= :maxPrice)
ORDER BY rank DESC, p.created_at DESC
LIMIT 20 OFFSET :offset;
```
Nếu kết quả < 5: chạy thêm `WHERE similarity(unaccent(p.name), unaccent(:q)) > 0.3 ORDER BY similarity DESC` để bù gõ sai.

### 5.2 Tải ảnh bằng Presigned URL

```java
public PresignedUpload presignProductImage(Long vendorId, String contentType) {
    if (!Set.of("image/jpeg", "image/png", "image/webp").contains(contentType)) throw badRequest("GNE-VAL-4220");
    String key = "products/%d/%s".formatted(vendorId, UUID.randomUUID());
    String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
        .method(Method.PUT).bucket("product-media").object(key).expiry(5, TimeUnit.MINUTES).build());
    return new PresignedUpload(key, url);
}
```
Sau khi client PUT xong, backend gọi `statObject` kiểm tra kích thước ≤ 5 MB và MIME trước khi lưu `product_images`.

### 5.3 Kiểm duyệt: Gemini chấm trước, Moderator duyệt cuối (UC-CAT-12, UC-CAT-13)

```java
@Async @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
void on(ProductSubmittedEvent e) {
    Product p = products.findById(e.productId()).orElseThrow();
    ProductModerationResult r;
    try {
        String json = gemini.generateContent(GeminiRequest.builder()
            .model(props.model())                                   // ví dụ gemini-2.5-flash
            .systemInstruction(policyPrompt)                        // chính sách cấm bán của sàn
            .userParts(textOf(p), imagesOf(p, 4))                   // tên, mô tả bỏ HTML, danh mục, thương hiệu, giá, tối đa 4 ảnh
            .responseMimeType("application/json").responseSchema(MODERATION_SCHEMA)
            .timeout(Duration.ofSeconds(20)).build());              // Spring Retry: 3 lần, 2 s / 8 s / 30 s
        r = ProductModerationResult.from(p.getId(), props.model(), json);   // JSON sai schema → NEED_REVIEW
    } catch (GeminiException ex) {
        r = ProductModerationResult.fallback(p.getId(), props.model(), ex.getMessage(), now());
    }
    results.save(r);
    if (r.isAutoReject(props.rejectThreshold())) p.reject(r.getReasoning());   // REJECT và riskScore ≥ 85
    else if (r.getDecision() != ModerationDecision.APPROVE) p.flag();            // hàng chờ ưu tiên cao
    // APPROVE: giữ PENDING_APPROVAL ở hàng chờ thường — TLCN không cho AI tự đăng bán
}
```
`MODERATION_SCHEMA`: `{decision: enum[APPROVE, REJECT, NEED_REVIEW], riskScore: integer 0..100, violations: array<enum[COUNTERFEIT, PROHIBITED_ITEM, MISLEADING_DESCRIPTION, BAD_IMAGE, PRICE_ANOMALY, WRONG_CATEGORY]>, reasoning: string}`. Không gửi CCCD, số tài khoản, nội dung giấy tờ — chỉ tên tệp và loại giấy tờ.

Hàng chờ Moderator = `products WHERE status IN ('PENDING_APPROVAL','FLAGGED') ORDER BY (status = 'FLAGGED') DESC, created_at`; nhận việc bằng `SELECT … FOR UPDATE SKIP LOCKED LIMIT 1` để hai người không duyệt trùng; màn duyệt hiển thị ô "Phân tích từ AI" (`reasoning`, `violations`, `riskScore`).

### 5.4 Chia sẻ deeplink & Open Graph (UC-CAT-18)
Link `https://genie.vn/p/{slug}-{productId}?v={variantId}&src=share`; trang `/p/[slug]-[id]` của Astro render SSR với `og:title`, `og:image` (ảnh bìa 1200×630), `og:price:amount`, `og:url` chuẩn; `slug` đổi → 301 về link mới; `src=share` ghi nguồn `EXTERNAL_LINK` cho thống kê.

### 5.5 Bộ đếm lượt xem / từ khóa / banner (UC-CAT-04, UC-CAT-06, UC-VND-08, UC-ADM-12)
`INCR view:{productId}:{date}:{source}`, `PFADD uv:{productId}:{date} {visitorId}`, `ZINCRBY search:kw:{date} 1 {keyword}`, `INCR banner:{id}:click`; `@Scheduled(fixedDelay = 5 phút)` đọc-và-xóa (`GETDEL`) rồi cộng dồn vào `product_view_stats.accumulate()`, `search_keyword_stats.accumulate()`, `home_banners.recordStats()` và `products.view_count`.

### 5.6 Thuộc tính ngành, thương hiệu, giấy tờ, đề xuất danh mục (UC-CAT-15, UC-CAT-09, UC-CAT-17)
Khi lưu sản phẩm: kiểm tra mọi `category_attributes` `isRequired` có giá trị đúng kiểu; danh mục `requiresBusinessLicense` → bắt buộc ≥ 1 `product_documents` (bucket riêng tư `product-docs`). Đề xuất danh mục: tối đa 5 `PENDING` / shop; duyệt → tạo `Category` (hoa hồng mặc định của danh mục cha) + `CategoryProposal.approve()` trong 1 transaction.

---

<a id="phan-6"></a>
## 🛒 PHẦN 6: GIỎ HÀNG, CHECKOUT ĐA SHOP & GIỮ CHỖ TỒN KHO

### 6.1 Gộp giỏ khách vãng lai (UC-ORD-01)
Giỏ tạm lưu `localStorage` dạng `[{variantId, qty}]`. Khi đăng nhập, client gọi `POST /api/v1/me/cart/merge`; service lọc SKU ngừng bán / của chính shop người mua, cộng dồn số lượng và cắt theo tồn khả dụng, gọi `Cart.mergeGuestItems()`.

### 6.2 Thuật toán checkout (UC-ORD-04, UC-ORD-07)

```java
@Transactional
public CheckoutResult placeOrder(Long userId, CheckoutRequest req, String idemKey) {
    idempotency.ensureFirst("checkout:" + userId + ":" + idemKey);        // Redis SETNX 10 phút
    Address addr = addresses.requireOwned(userId, req.addressId());
    Map<Long, List<CartLine>> byShop = cart.selectedLines(userId, req.itemIds()).stream()
        .peek(l -> { if (l.vendorUserId().equals(userId)) throw conflict("GNE-ORD-4094"); })
        .collect(groupingBy(CartLine::vendorId, LinkedHashMap::new, toList()));
    CheckoutSession s = sessions.save(CheckoutSession.start(userId, now.plusMinutes(15)));
    List<Order> orders = new ArrayList<>();
    for (var e : byShop.entrySet()) {
        VendorProfile shop = vendors.require(e.getKey());
        BigDecimal fee = shipping.fee(shop, addr, e.getValue());           // GHN, có phí dự phòng
        Order o = Order.create(s, shop, addr.snapshotJson(), req.paymentMethod(), fee);
        e.getValue().forEach(l -> o.addItem(OrderItem.snapshot(l, catalog.commissionRateFor(l, shop))));
        orders.add(o);
    }
    promotion.applyAndAllocate(s, orders, req.shopVoucherIds(), req.platformVoucherId());   // PHẦN 10
    orders.forEach(o -> { o.recalculateGrandTotal(); orderRepo.save(o); });
    for (Order o : orders) for (OrderItem i : o.getItems())
        inventory.reserve(i.getProductVariantId(), i.getQuantity(), o.getId(), now.plusMinutes(15));  // 6.3
    s.markTotal(orders);
    if (req.paymentMethod() == PaymentMethod.VNPAY)
        return CheckoutResult.redirect(vnpay.createPaymentUrl(payments.start(s), clientIp));
    orders.forEach(o -> escrow.openPendingCod(o));                       // COD: ký quỹ PENDING_FUNDING
    return CheckoutResult.cod(s.getId());
}
```

### 6.3 Giữ chỗ có điều kiện

```java
@Modifying
@Query("""
  UPDATE ProductVariant v SET v.reservedCount = v.reservedCount + :q, v.version = v.version + 1
  WHERE v.id = :id AND v.deletedAt IS NULL AND v.stockCount - v.reservedCount >= :q""")
int tryReserve(Long id, int q);           // 0 dòng → ném GNE-INV-4090, cả transaction checkout rollback
```

### 6.4 Sweeper (UC-ORD-08)

```java
@Scheduled(fixedDelay = 60_000)
@Transactional
public void expireUnpaid() {
    for (CheckoutSession s : sessions.lockExpired(now(), 100)) {          // FOR UPDATE SKIP LOCKED
        for (StockReservation r : reservations.activeOf(s)) {
            if (reservations.releaseIfActive(r.getId(), now()) == 1)          // UPDATE … WHERE status='ACTIVE'
                variants.releaseReserved(r.getProductVariantId(), r.getReservedQuantity());
        }
        s.expire();
        orderRepo.findBySession(s).forEach(o -> o.transitionTo(OrderStatus.AUTO_CANCELLED));
        promotion.restoreUsages(s);
    }
}
```
> Điều kiện `WHERE status = 'ACTIVE'` là chốt chống đua với IPN đến muộn: nếu IPN đã `confirm()` trước thì sweeper cập nhật 0 dòng và bỏ qua.

---

<a id="phan-7"></a>
## 💳 PHẦN 7: THANH TOÁN VNPAY, VIETQR QUA SEPAY, COD & KÝ QUỸ ESCROW, SỔ CÁI

### 7.1 Tạo URL thanh toán (UC-PAY-01)

```java
public String createPaymentUrl(PaymentTransaction tx, String ip) {
    TreeMap<String, String> p = new TreeMap<>();
    p.put("vnp_Version", "2.1.0"); p.put("vnp_Command", "pay"); p.put("vnp_TmnCode", props.tmnCode());
    p.put("vnp_Amount", tx.getAmount().movePointRight(2).toBigInteger().toString());   // VNPay nhân 100
    p.put("vnp_CurrCode", "VND"); p.put("vnp_TxnRef", tx.getId().toString());
    p.put("vnp_OrderInfo", "Thanh toan phien " + tx.getCheckoutSessionId()); p.put("vnp_OrderType", "other");
    p.put("vnp_Locale", "vn"); p.put("vnp_ReturnUrl", props.returnUrl()); p.put("vnp_IpAddr", ip);
    p.put("vnp_CreateDate", VNP_TIME.format(now())); p.put("vnp_ExpireDate", VNP_TIME.format(now().plusMinutes(15)));
    String query = p.entrySet().stream().map(e -> e.getKey() + "=" + urlEncode(e.getValue())).collect(joining("&"));
    return props.payUrl() + "?" + query + "&vnp_SecureHash=" + hmacSha512(props.hashSecret(), query);
}
```

### 7.2 Xử lý IPN idempotent (UC-PAY-03)

```java
@Transactional
public VnpResponse handleIpn(Map<String, String> q) {
    if (!signatureValid(q)) return VnpResponse.of("97", "Invalid Checksum");
    PaymentTransaction tx = txRepo.lockById(Long.valueOf(q.get("vnp_TxnRef"))).orElse(null);   // SELECT … FOR UPDATE
    if (tx == null) return VnpResponse.of("01", "Order not found");
    if (tx.getStatus() != PaymentStatus.PENDING) return VnpResponse.of("02", "Order already confirmed");
    if (new BigDecimal(q.get("vnp_Amount")).movePointLeft(2).compareTo(tx.getAmount()) != 0)
        return VnpResponse.of("04", "Invalid Amount");
    if ("00".equals(q.get("vnp_ResponseCode")) && "00".equals(q.get("vnp_TransactionStatus"))) {
        tx.markSuccess(q.get("vnp_TransactionNo"), toJson(q));
        checkout.confirmPaid(tx.getCheckoutSessionId());   // confirm reservation, commitSale, markPaid, escrow.fund(ONLINE_GATEWAY)
        events.publishEvent(new OrderPaidEvent(tx.getCheckoutSessionId(), checkout.orderIds(tx)));
    } else {
        tx.markFailed(toJson(q));
        checkout.failAndRelease(tx.getCheckoutSessionId());
    }
    return VnpResponse.of("00", "Confirm Success");
}
```
> **Return URL** (`/payments/vnpay/return`) chỉ kiểm tra chữ ký rồi hiển thị "đang xác nhận…", trang kết quả hỏi trạng thái từ backend — **không** cập nhật đơn.

### 7.3 Ghi ký quỹ (UC-PAY-05)

```
OrderItem.grossAmount     = lineTotal − shopDiscount
OrderItem.commissionRate  = VendorProfile.effectiveCommissionRate(category.commissionRate)   // shop → ngành → 5%
OrderItem.commissionAmount= grossAmount × commissionRate  (làm tròn xuống đồng)
EscrowLedger.grossAmount  = Σ grossAmount ;  commissionFee = Σ commissionAmount
EscrowLedger.netAmount    = grossAmount − commissionFee − shopShippingSubsidy
```
Voucher **sàn** không làm giảm `grossAmount` (sàn chịu); voucher **shop** có (shop chịu).

### 7.4 Giải ngân tự động (UC-PAY-07)

```java
@Scheduled(cron = "0 5 * * * *")
public void releaseDue() {
    escrowRepo.lockReleasable(now(), 200).forEach(e -> tx.executeWithoutResult(s -> {   // HELD + COMPLETED + releaseEligibleAt<=now, SKIP LOCKED
        BigDecimal net = e.release(now());
        VendorWallet w = wallets.lockByVendor(e.getVendorId());
        if (!w.verifyHash(secret)) throw new IllegalStateException("ESCROW_DISCREPANCY");  // dừng, báo Admin
        BigDecimal before = w.getBalance();
        w.credit(net, secret);
        walletTx.save(WalletTransaction.credit(w, net, before, e.getOrderId(), WalletTransactionType.CREDIT_ESCROW_RELEASE));
        audit.log(AuditAction.ESCROW_RELEASED, e.getOrderId());
    }));
}
```

### 7.5 Hoàn tiền VNPay (dùng ở UC-SUP-04, UC-ORD-10)
`POST {vnp_ApiUrl}` JSON: `vnp_RequestId`, `vnp_Command=refund`, `vnp_TransactionType` (`02` toàn phần / `03` một phần), `vnp_TxnRef`, `vnp_Amount`, `vnp_TransactionNo`, `vnp_TransactionDate`, `vnp_CreateBy`, `vnp_SecureHash` (HMAC-SHA512 chuỗi nối `|`). Kết quả lưu `payment_refunds`.


### 7.6 Chuyển khoản VietQR & đối soát ngân hàng qua SePay

SePay theo dõi biến động tài khoản ngân hàng của sàn và gọi webhook cho mỗi giao dịch; **không tự chi tiền**. Một endpoint xử lý 4 loại giao dịch, phân biệt bằng tiền tố nội dung chuyển khoản:

| Tiền tố nội dung | Chiều | Ý nghĩa | Xử lý |
|---|:---:|---|---|
| `TR{paymentTransactionId}` | in | Khách thanh toán chuyển khoản VietQR | Như IPN thành công (UC-PAY-03) |
| *(không mã — chuyển từ GHN)* | in | GHN chuyển tiền COD | Tạo phiên đối soát chờ bảng kê (UC-LOG-03) |
| `TRWD{withdrawalId}` | out | Staff chuyển tiền rút cho shop | `WithdrawalRequest.complete()` (UC-ADM-05) |
| `TRRF{refundRequestId}` | out | Staff hoàn tiền chuyển khoản | `RefundRequest.markRefunded()` (UC-SUP-04) |

```java
private static final Pattern CODE = Pattern.compile("TR(WD|RF)?(\\d+)");

@PostMapping("/api/v1/payments/sepay/webhook")
public Map<String, Object> sepay(@RequestHeader("Authorization") String auth, @RequestBody SepayTx tx) {
    if (!("Apikey " + props.sepayApiKey()).equals(auth)) throw unauthorized("GNE-SEC-4010");
    if (!seen.markFirst("sepay:" + tx.id())) return Map.of("success", true);   // chống trùng: SePay thử lại tối đa 7 lần
    Matcher m = CODE.matcher(tx.content());
    boolean coded = m.find();
    if ("in".equals(tx.transferType()) && coded && m.group(1) == null)
        payments.confirmBankTransfer(Long.valueOf(m.group(2)), tx.transferAmount(), String.valueOf(tx.id()));  // khớp số tiền → luồng IPN
    else if ("out".equals(tx.transferType()) && coded && "WD".equals(m.group(1)))
        payouts.confirmTransferred(Long.valueOf(m.group(2)), tx.transferAmount(), tx.referenceCode());
    else if ("out".equals(tx.transferType()) && coded && "RF".equals(m.group(1)))
        refunds.confirmTransferred(Long.valueOf(m.group(2)), tx.transferAmount(), tx.referenceCode());
    else unmatched.enqueue(tx);                                                 // tiền GHN về / sai nội dung → Staff xử lý
    return Map.of("success", true);                                              // luôn HTTP 200 sau khi đã ghi nhận
}
```

- **QR động:** `https://qr.sepay.vn/img?acc={STK}&bank={mã NH}&amount={số tiền}&des=TR{id}`; trang thanh toán hỏi trạng thái mỗi 3 giây hoặc nghe `/user/queue/notifications`.
- **Sai số tiền / thiếu mã:** không tự xác nhận, đưa vào hàng đợi "giao dịch chưa khớp" ở Admin Portal.
- **Không có webhook sau 24 giờ** (lệnh rút / hoàn): Staff xác nhận tay theo sao kê.
- **Thử nghiệm:** chức năng "Gửi thử" và chế độ Test trên dashboard SePay; một tài khoản ngân hàng thật số dư nhỏ của nhóm cho buổi demo.

### 7.7 Sổ cái kép bất biến (UC-PAY-06)

```java
@Transactional(propagation = Propagation.MANDATORY)          // luôn chạy trong transaction nghiệp vụ gọi nó
public void post(FinancialEventType type, String refId, Long orderId, Long vendorId, List<Entry> entries) {
    BigDecimal d = entries.stream().map(Entry::debit).reduce(ZERO, BigDecimal::add);
    BigDecimal c = entries.stream().map(Entry::credit).reduce(ZERO, BigDecimal::add);
    if (d.compareTo(c) != 0) throw new IllegalStateException("Bút toán không cân: " + d + " ≠ " + c);
    entries.forEach(e -> ledger.save(FinancialLedger.of(nextSeq(), type, refId, orderId, vendorId, e)));   // chỉ INSERT
}
```

| Sự kiện | Nợ | Có |
|---|---|---|
| `ESCROW_IN` (tiền về: VNPay / VietQR / đối soát COD) | Tiền gửi ngân hàng của sàn | Phải trả ký quỹ |
| `COMMISSION` (khi giải ngân) | Phải trả ký quỹ | Doanh thu hoa hồng |
| `ESCROW_RELEASE` | Phải trả ký quỹ | Phải trả người bán (ví shop) |
| `PAYOUT_SETTLE` (SePay báo tiền ra) | Phải trả người bán | Tiền gửi ngân hàng của sàn |
| `PLATFORM_VOUCHER` / `FREESHIP_COST` / `COIN_COST` | Chi phí khuyến mãi | Phải trả ký quỹ |
| `REFUND` | Phải trả ký quỹ | Tiền gửi ngân hàng của sàn |

Không UPDATE / DELETE bảng `financial_ledger` (thu hồi quyền ở cấp CSDL cho user ứng dụng); điều chỉnh bằng bút toán đảo. Job đêm kiểm tra tổng Nợ = tổng Có và đối chiếu số dư tài khoản "Phải trả người bán" với Σ `vendor_wallets.balance`.

---

<a id="phan-8"></a>
## 🪪 PHẦN 8: GIAN HÀNG & KYC THEO TẦNG (AI WORKER)

### 8.1 Hợp đồng Backend ↔ AI Worker

> **Đã thay (2026-11-18, ADR 0011 mục 5):** hợp đồng **snake_case**, AI Worker **không** tính `riskScore` / kết luận — chỉ trả `checks{…}`, `signals[{code}]`, `extracted{…}`, `face_match_score`, `liveness_score`, `errors`, `model`; backend quy đổi điểm theo `genie.kyc.risk.points`, kiểm trùng CCCD, lấy mẫu 5% và chốt. Phần mô tả camelCase bên dưới chỉ còn giá trị lịch sử.

```http
POST http://ai-worker:8000/kyc/verify          X-Internal-Token: …
{ "vendorProfileId": 42, "front": "<presigned GET>", "back": "<presigned GET>", "selfie": "<presigned GET>",
  "declared": {"idNumber": "079200001234", "fullName": "NGUYEN VAN A", "dob": "2000-01-01"}, "biometricConsent": true }

PATCH http://backend:8080/api/v1/internal/kyc/42/result     X-Internal-Token: …
{ "qrCodeDetected": true, "mrzValid": true, "mrzQrMatch": true, "cccdNumberValid": true, "expiryRuleValid": true,
  "faceMatchScore": 0.91, "livenessScore": 0.88, "faceDuplicateFound": false, "duplicateIdFound": false,
  "riskScore": 12, "riskSignals": [], "extracted": {...}, "errors": null }
```

### 8.2 Pipeline (FastAPI, nạp mô hình một lần khi khởi động)

```python
@app.post("/kyc/verify")
async def verify(req: KycRequest, _=Depends(internal_token)):
    background.add_task(run_pipeline, req)            # trả 202 ngay; kết quả gọi callback
    return {"accepted": True}

def run_pipeline(req):
    front, back, selfie = (load_image(u) for u in (req.front, req.back, req.selfie))
    s = Signals()
    qr = decode_qr(front)                              # Tầng 0: < 100 ms, dữ liệu gần như tuyệt đối
    mrz = read_mrz(back)                               # 3 dòng MRZ, kiểm tra check digit
    s.check(qr is not None, "QR_NOT_FOUND", 25); s.check(mrz.valid, "MRZ_INVALID", 20)
    s.check(qr and mrz.valid and qr.id == mrz.id, "QR_MRZ_MISMATCH", 40)
    s.check(qr and qr.id == req.declared.idNumber, "DECLARED_ID_MISMATCH", 30)
    if req.biometricConsent:                           # Tầng 1: YuNet + SFace (ONNX, CPU ~1 s)
        score = cosine(embed(face(front)), embed(face(selfie)))
        s.check(score >= 0.85, "FACE_MISMATCH", 35); s.check(liveness(selfie) >= 0.7, "LIVENESS_LOW", 20)
        s.check(not duplicate_face(score_db, embed(face(selfie)), req.declared.idNumber), "FACE_DUPLICATE", 50)
    post_result(req.vendorProfileId, s.result(sample_rate=0.05))
```
Kết luận ở backend (`KycResult.applyRiskScore`): `< 30` PASS → `approveKyc(OCR_AUTO)` (trừ 5% bị lấy mẫu), `30–69` → `MANUAL_REVIEW`, `≥ 70` FAIL. Hết 30 giây không có callback → `MANUAL_REVIEW`.

### 8.3 Tài khoản nhận tiền chính chủ (UC-VND-07)
Tra cứu tên chủ tài khoản qua API giả lập (`/mock/bank/lookup`), so sánh sau khi bỏ dấu + viết hoa với `kyc_results.extracted_full_name`; khớp mới ghi `payoutAccountVerifiedAt`.

### 8.4 Ví & sổ giao dịch, cài đặt gian hàng, tự trả lời, tạm nghỉ (UC-VND-20, UC-VND-21, UC-VND-06, UC-VND-19)
- **Ví:** `GET /seller/wallet/overview` trả số dư khả dụng, đang giữ (Σ `escrow_ledgers.net_amount` `HELD` / `DISPUTED`), chờ đối soát COD (`PENDING_FUNDING`), đang rút; `GET /seller/wallet/transactions` keyset theo (`created_at`, `id`), xuất CSV.
- **Trang shop:** `rating_avg`, `rating_count` cập nhật bằng `VendorProfile.applyRating()` khi có / ẩn đánh giá; `follower_count` khi theo dõi; `active_product_count` khi sản phẩm đổi trạng thái; `chat_response_rate`, `avg_response_minutes` do job đêm tính từ `messages` 30 ngày (tin tự trả lời không tính).
- **Cài đặt:** logo / ảnh bìa qua Presigned URL; đổi tên shop tối đa 1 lần / 30 ngày; hãng vận chuyển & `freeship_threshold` ở `vendor_shipping_config`; `pre_order_enabled`.
- **Tạm nghỉ:** tối đa 30 ngày; chặn khi còn đơn chưa bàn giao; checkout chứa sản phẩm của shop → `GNE-ORD-4093`; cron `endVacation()`.

### 8.5 Phân tích cảm xúc đánh giá bằng PhoBERT (UC-AI-07)

Nhóm đã có mô hình **PhoBERT** fine-tune phân loại 3 nhãn (`POSITIVE` / `NEUTRAL` / `NEGATIVE`). TLCN đưa mô hình vào **cùng AI Worker** với KYC, không thêm dịch vụ mới.

| Việc | Cách làm |
|---|---|
| Đóng gói mô hình | Xuất ONNX (`optimum` / `torch.onnx.export`), lượng tử hóa động INT8 cho CPU; đặt tên `phobert-sentiment-v{n}`; kèm tokenizer và bảng nhãn |
| Tiền xử lý | Giống hệt lúc huấn luyện: NFC, viết thường, chuẩn hóa teencode, bỏ URL, **tách từ** (RDRSegmenter / VnCoreNLP — PhoBERT cần đầu vào đã tách từ), tối đa 256 token |
| API AI Worker | `POST /sentiment` `{reviewId, text}` → `{label, score, model}`; token nội bộ như `/kyc/verify`; batch tối đa 32 câu cho job chạy lại |
| Backend | `ReviewCreatedEvent` → `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` gọi AI Worker; nhận `PATCH /internal/reviews/{id}/sentiment` → `ProductReview.attachSentiment(json)` lưu `{"overall", "score", "model", "analyzedAt"}` vào `aspect_sentiment_json` |
| Lỗi / chạy lại | Thử lại 3 lần (2 / 8 / 30 s); Cron 15 phút quét đánh giá có chữ mà `aspect_sentiment_json IS NULL` trong 24 giờ; đổi mô hình → job chạy lại theo lô |
| Dùng kết quả | Phân tích bán hàng (UC-VND-08): tỉ lệ 3 nhãn theo kỳ, top sản phẩm bị tiêu cực (chỉ `score ≥ 0,6`); Kiểm duyệt đánh giá (UC-SOC-05): `NEGATIVE` với `score ≥ 0,85` lên đầu hàng chờ, nhãn "AI: Tiêu cực" |
| Truy vấn | Index biểu thức `((aspect_sentiment_json ->> 'overall'))` + `(vendor_id, created_at)`; cache Redis `sentiment:vendor:{id}` 10 phút |
| Kiểm thử | Tập kiểm thử cố định của nhóm: báo cáo accuracy / macro-F1 và ma trận nhầm lẫn; so sánh đầu ra ONNX với mô hình gốc trên cùng tập (lệch nhãn ≈ 0); đo thời gian suy luận / đánh giá trên VPS |

Kết quả chỉ tham khảo — không tự ẩn đánh giá, không đổi số sao; nội dung gửi sang AI Worker không kèm tên, email, số điện thoại khách.

---

<a id="phan-9"></a>
## 🚚 PHẦN 9: VẬN CHUYỂN ĐA HÃNG 3PL & ĐỐI SOÁT COD

**Shop xác nhận / từ chối đơn mới (UC-LOG-16):** `POST /seller/orders/{id}/confirm` → `CONFIRMED`; `POST /seller/orders/{id}/reject` (lý do) → `CANCELLED_BY_VENDOR`, nhả tồn, hoàn tiền tự động (VNPay) / tạo yêu cầu hoàn (VietQR), hoàn Điểm và lượt voucher. Cron mỗi giờ: đơn `PAID` / COD chờ quá 24 giờ → tự xác nhận và tăng bộ đếm "chậm xác nhận" của shop.

**Adapter đa hãng (UC-LOG-15):**

```java
public interface CarrierAdapter {
    CarrierType type();                                  // GHN, GHTK, VTP, JT
    FeeQuote quote(ShipmentRequest r);                   // timeout 3 s
    CreatedShipment create(ShipmentRequest r);           // client_order_code = orders.order_code
    ShipmentTrackingStatus mapStatus(String carrierStatus);
}
// chọn hãng: gọi quote() song song các hãng shop bật (vendor_shipping_config), bỏ hãng lỗi,
// lấy phí thấp nhất trong các hãng có ETA ≤ ETA nhanh nhất + 1 ngày; mặc định GHN
```
Webhook theo hãng `POST /api/v1/shipping/{carrier}/webhook/{secret}` → `mapStatus()` → cùng `ShipmentTrackingStatus` và cùng bảng ánh xạ đơn hàng bên dưới.

**GHN (hãng mặc định):**

| Việc | API GHN | Ghi chú |
|---|---|---|
| Địa giới hành chính | `master-data/province`, `district`, `ward` | Cache Redis 24 giờ |
| Đăng ký cửa hàng | `v2/shop/register` | Lưu `vendor_profiles.ghn_shop_id` (UC-VND-05) |
| Tính phí | `v2/shipping-order/fee` | Timeout 3 s, dự phòng phí cố định (UC-ORD-06) |
| Tạo vận đơn | `v2/shipping-order/create` | `client_order_code = orders.order_code`, `cod_amount = grandTotal` nếu COD (UC-LOG-02) |
| In phiếu | `v2/a5/gen-token` → URL in | A6 / 80 mm |
| Webhook | GHN → `POST /api/v1/shipping/ghn/webhook/{secret}` | Idempotent `(trackingCode, status, time)` (UC-LOG-03) |

**Ánh xạ trạng thái GHN → hệ thống:**

| GHN `status` | `ShipmentTrackingStatus` | `Order` |
|---|---|---|
| `ready_to_pick`, `picking` | `AWAITING_PICKUP` | `READY_FOR_PICKUP` |
| `picked`, `storing`, `transporting`, `sorting` | `PICKED_UP` / `IN_TRANSIT` | `DELIVERING` |
| `delivering` | `OUT_FOR_DELIVERY` | `DELIVERING` |
| `delivered` | `DELIVERED` | `DELIVERED` (`deliveredAt`) |
| `delivery_fail` | `FAILED` | `DELIVERY_FAILED` |
| `waiting_to_return`, `return*` | `RETURNING` | `RETURNING` |
| `returned` | `RETURNED` | `RETURNED_TO_VENDOR` |
| `lost`, `damage` | `LOST` | giữ nguyên, báo Staff xử lý bồi thường |

**Đối soát COD:** Staff tải CSV bảng kê của GHN (`order_code, cod_amount, transfer_date`) → service khớp từng dòng với `shipment_trackings.tracking_code` và `orders.grand_total` → khớp: `EscrowLedger.fund(amount, COD_CASH)` + `Order.markPaid()`; lệch: đưa vào danh sách ngoại lệ. Có job dự phòng mỗi 30 phút gọi API tra cứu trạng thái cho đơn `DELIVERING` quá 24 giờ không có webhook.

---

<a id="phan-10"></a>
## 🎟️ PHẦN 10: VOUCHER 3 TẦNG, ĐIỂM, FLASH SALE & PHÂN BỔ CHIẾT KHẤU

1. **Kiểm tra:** `Voucher.isApplicable(value, now)`, phạm vi (`scope` + `voucher_categories` / `voucher_products` / danh sách loại trừ), lượt / người (`voucher_usages`), khách đã lưu (`user_vouchers`), ngân sách còn.
2. **Tầng 1 — voucher Shop** (mỗi shop tối đa 1) áp trên tạm tính hàng hợp lệ của **đơn con** → `OrderDiscount(tier=SHOP, fundedBy=VENDOR)`, `order_items.shop_discount`.
3. **Tầng 2 — voucher Sàn** (tối đa 1) áp trên tổng **sau tầng 1** của cả phiên → chia cho các đơn con rồi từng dòng theo tỷ lệ giá trị → `OrderDiscount(tier=PLATFORM)`, `order_items.platform_discount`; nếu `platformSharePercent` < 100 thì shop đồng tài trợ phần còn lại.
4. **Tầng 3 — Freeship** (tối đa 1) trừ vào tổng phí ship, phân bổ theo phí ship từng shop, không vượt phí thực tế → `OrderDiscount(tier=FREESHIP)`.
5. **Điểm** (UC-PRO-10): `N = min(số dư, 50% tiền hàng sau 3 tầng)`, 1 Điểm = 1₫ → `orders.coin_used_count`, `coin_discount_amount`, `order_items.coin_discount`; sàn chịu (bút toán `COIN_COST`).
6. **Phân bổ xuống dòng hàng** (để hoàn tiền một phần đúng):

```java
static void allocate(List<OrderItem> items, BigDecimal discount) {      // phần dư dồn dòng cuối
    BigDecimal base = items.stream().map(OrderItem::getLineTotal).reduce(ZERO, BigDecimal::add), used = ZERO;
    for (int i = 0; i < items.size(); i++) {
        OrderItem it = items.get(i);
        BigDecimal part = i == items.size() - 1 ? discount.subtract(used)
            : discount.multiply(it.getLineTotal()).divide(base, 0, RoundingMode.DOWN);
        it.allocateDiscount(part); used = used.add(part);
    }
}
```
7. **Ghi lượt:** `Voucher.consume()` (`@Version`) + `VoucherUsage` (UNIQUE `voucher_id, checkout_session_id`); `UserCoin.spend()` (`@Version`) + `CoinTransaction(USED_AT_CHECKOUT)`; sweeper / hủy toàn phiên gọi `restore()` và hoàn Điểm (`ORDER_REFUND`, giữ hạn lô cũ).
8. **Gợi ý tự động:** với mỗi đơn con thử các voucher shop khách đã lưu, chọn mã giảm nhiều nhất; rồi thử voucher sàn và freeship trên kết quả đó (số mã nhỏ nên duyệt hết tổ hợp).

**Flash Sale (UC-PRO-05, UC-PRO-06):**
```sql
-- giữ suất khi tạo đơn; 0 dòng → GNE-INV-4091 (hết suất, mua giá thường)
UPDATE flash_sale_items SET reserved_count = reserved_count + :q, version = version + 1
WHERE id = :id AND flash_stock - sold_count - reserved_count >= :q;
```
Cron mỗi phút: `SCHEDULED` tới giờ → kiểm lại tồn → `activate()`; hết giờ / hết suất → `end()`. Thanh toán thành công `recordSale()`; phiên hết hạn `releaseReservation()`. Trang chủ đọc chiến dịch đang chạy từ cache Redis 30 giây.

**Điểm (UC-PRO-08, UC-PRO-09):** điểm danh khóa `SET lock:checkin:{userId} NX EX 5`; ngày 1–6: 100 Điểm, ngày 7 liên tiếp: 1.000 Điểm; mọi dòng cộng Điểm có `expires_at = now + 12 tháng`; cron đêm ghi dòng âm cho lô hết hạn; đổi Điểm lấy voucher trừ lô sắp hết hạn trước (FEFO); `UserCoin.verifyHash()` lệch → khóa tiêu Điểm, báo Admin.

---

<a id="phan-11"></a>
## 💬 PHẦN 11: CHAT & THÔNG BÁO THỜI GIAN THỰC

```java
@Configuration @EnableWebSocketMessageBroker
class WsConfig implements WebSocketMessageBrokerConfigurer {
    public void registerStompEndpoints(StompEndpointRegistry r) { r.addEndpoint("/ws").setAllowedOrigins(props.origins()); }
    public void configureMessageBroker(MessageBrokerRegistry r) {
        r.enableSimpleBroker("/queue"); r.setApplicationDestinationPrefixes("/app"); r.setUserDestinationPrefix("/user");
    }
    public void configureClientInboundChannel(ChannelRegistration c) { c.interceptors(jwtStompInterceptor); }  // xác thực ở CONNECT
}
```

| Kênh | Đích | Dữ liệu |
|---|---|---|
| Gửi tin chat | client → `/app/chat.send` | `{conversationId, type, content, attachmentKey}` |
| Nhận tin chat | `/user/queue/messages` | `Message` DTO |
| Thông báo | `/user/queue/notifications` | `Notification` DTO + số chưa đọc |

Thẻ sản phẩm / trạng thái đơn trong chat (UC-SOC-02): `type = PRODUCT_CARD | ORDER_CARD`, `product_variant_id` / `order_id` tham chiếu, nội dung thẻ dựng lúc hiển thị (không chép giá). Tự trả lời (UC-VND-06) gửi tin hệ thống đứng tên shop khi khách mở hội thoại. Thông báo toàn sàn (UC-NOT-05): 1 dòng `platform_notifications` (fan-out on read), đẩy `/topic/platform` cho người đang trực tuyến, đã đọc ghi `user_platform_notification_reads`.

Tin nhắn lưu `messages` trước rồi mới đẩy; người nhận offline sẽ thấy khi mở lại (tải theo cursor `created_at`). Simple broker chỉ chạy được **một instance** — đủ cho MVP; bản mở rộng chuyển sang broker relay.

---

<a id="phan-12"></a>
## ↩️ PHẦN 12: TRẢ HÀNG, HOÀN TIỀN & PHÂN XỬ

| Bước | Trạng thái `RefundRequest` | Hạn | Cron |
|---|---|---|---|
| Khách gửi (≤ 7 ngày từ `deliveredAt`) | `REQUESTED` | Shop phản hồi 48 giờ | `autoApproveIfShopSilent` |
| Shop yêu cầu thêm bằng chứng | `AWAITING_EVIDENCE` | Khách nộp 48 giờ | `closeIfEvidenceNotProvided` |
| Shop từ chối | `SHOP_REJECTED` | Khách khiếu nại 48 giờ | `closeIfCustomerSilent` |
| Khiếu nại lên sàn | `DISPUTED` | Staff xử lý 3 ngày làm việc | — |
| Đồng ý nhận lại hàng | `AWAITING_RETURN_SHIPMENT` | Khách gửi 7 ngày | `failReturnIfOverdue` |
| Chấp thuận | `APPROVED` → `REFUNDED` | — | — |

**Hoàn một phần (ví dụ kiểm thử bắt buộc):** đơn 1.000.000đ, hoa hồng 5% → gross 1.000.000 / phí 50.000 / net 950.000. Khách trả 1 món 300.000đ (hoa hồng dòng 15.000) → `applyPartialRefund(300.000, 15.000)` → gross 700.000 / phí 35.000 / net 665.000. Kiểm tra: khách 300.000 + shop 665.000 + sàn 35.000 = 1.000.000đ.

---

<a id="phan-13"></a>
## 🛠️ PHẦN 13: QUẢN TRỊ SÀN, NHÂN VIÊN & RBAC, RÚT TIỀN & DASHBOARD

- **Rút tiền (UC-ADM-04/05):** `WithdrawalRequest` `PENDING` → `VendorWallet.holdForWithdrawal(amount)` (số dư khả dụng = `balance − pendingWithdrawal`) → Staff `approve` → chuyển khoản tay với nội dung `TRWD{id}` → webhook SePay (tiền ra) gọi `complete(referenceCode)` → `settleWithdrawal` + `WalletTransaction(DEBIT_WITHDRAWAL)`; từ chối → nhả `pendingWithdrawal`.
- **Khóa tài khoản (UC-ADM-03):** `User.suspend(reason, until)` + thu hồi mọi refresh token + `system_logs`; cron mỗi giờ `reactivateIfSuspensionExpired`.
- **Dashboard (UC-ADM-06):** các truy vấn tổng hợp ở Usecase.md UC-ADM-06, cache Redis 5 phút; biểu đồ Recharts.
- **Nhân viên (UC-ADM-01):** mời → `users` (`STAFF`, `UNVERIFIED`) + `admin_profiles.staff_title` + `staff_permissions` mặc định trong 1 transaction; link kích hoạt dùng 1 lần, 48 giờ; nhân viên đặt mật khẩu sau khi xác thực OTP email. Phân quyền: mục 4.5.
- **Nhật ký hệ thống (UC-ADM-07):** `GET /admin/system-logs` lọc thời gian / loại / từ khóa, keyset (`created_at`, `id`), xuất CSV; bảng chỉ INSERT.
- **Banner (UC-ADM-12):** `home_banners` theo vị trí, tối đa 8 banner đang hiển thị / vị trí; trang chủ đọc cache Redis 60 giây; lượt hiển thị / bấm đếm Redis, job 5 phút ghi `recordStats()`.
- **Kiểm duyệt đánh giá (UC-SOC-05):** đánh giá ≥ 3 báo cáo lên đầu hàng chờ; ẩn → `is_visible = false`, tính lại `products.rating` và `VendorProfile.applyRating()`.

---

<a id="phan-14"></a>
## 🖥️ PHẦN 14: FRONTEND

| Khu vực | Route chính | Kỹ thuật |
|---|---|---|
| Storefront (Astro SSR) | `/`, `/c/[slug]`, `/p/[slug]-[id]`, `/shop/[id]`, `/search`, `/flash-sale`, `/cart`, `/checkout`, `/orders`, `/orders/[id]`, `/chat`, `/account` (`/profile`, `/addresses`, `/wishlist`, `/vouchers`, `/coins`, `/following`, `/notifications`, `/delete`) | Trang danh mục / sản phẩm / shop SSR + `Cache-Control: s-maxage=60, stale-while-revalidate`; giỏ, checkout, chat là React island |
| Kênh Người bán | `/seller/onboarding`, `/seller/kyc`, `/seller/dashboard`, `/seller/products`, `/seller/categories` (đề xuất), `/seller/orders`, `/seller/shipping`, `/seller/promotions` (voucher, Flash Sale), `/seller/reviews`, `/seller/returns`, `/seller/chat`, `/seller/analytics`, `/seller/wallet`, `/seller/settings` | React SPA (React Router), TanStack Query |
| Admin Portal | `/admin/dashboard`, `/admin/kyc`, `/admin/products`, `/admin/categories` (+ thuộc tính, đề xuất), `/admin/reviews`, `/admin/vouchers`, `/admin/flash-sales`, `/admin/disputes`, `/admin/cod`, `/admin/withdrawals`, `/admin/ledger`, `/admin/users`, `/admin/staffs`, `/admin/system-logs`, `/admin/banners`, `/admin/notifications` — nav lọc theo `GET /admin/me/permissions` | React SPA |

- **Gọi API:** `shared/api/http.ts` bọc `fetch` tự gắn access token, gặp `401 GNE-SEC-4010` thì gọi `/auth/sessions/refresh` một lần rồi thử lại. Giao diện đã dựng xong toàn bộ trên lớp mock (`PUBLIC_USE_MOCK=true`); nối thật từng nhóm bằng cờ `PUBLIC_REAL_API=auth,upload,profile,security` — danh sách hàm cần nối và 21 endpoint lệch spec: `docs/web-backend-handoff.md`.
- **SEO:** `<title>`, `meta description`, Open Graph, JSON-LD `Product` + `BreadcrumbList`, `sitemap.xml` sinh khi build + cập nhật mỗi ngày.
- **Ảnh:** nén WebP phía client (`browser-image-compression`) trước khi PUT lên Silo.
- **Đa ngôn ngữ:** `i18n/vi.json`, `i18n/en.json`; hàm `t(key)` dùng chung cho Astro và React; ngôn ngữ chọn ở header lưu cookie `lang`; CI chạy script so khóa giữa hai file.
- **Chia sẻ:** nút Chia sẻ dùng `navigator.share` khi có, ngược lại `navigator.clipboard.writeText`.

---

<a id="phan-15"></a>
## 🔍 PHẦN 15: ĐIỂM XUẤT PHÁT & RỦI RO KHI XÂY MỚI

> **Điểm xuất phát: chưa có mã nguồn — 0/87 use case.** Mọi phân hệ xây mới theo tài liệu này; `DATN/` là kế hoạch đồ án tốt nghiệp để mở rộng sau TLCN.

### 15.1 Việc phải xong trong tuần 1 (nền móng)
- [ ] Repo + nhánh `main` / `develop`, quy ước commit, template PR (Document_Vi 4.1).
- [ ] `docker-compose.local.yml`: PostgreSQL, Redis, Silo, Mailpit chạy được trên máy cả nhóm.
- [ ] Spring Boot khung 11 package module + ArchUnit + `ApiResponse` / `ErrorCode` chung.
- [ ] Flyway `V1`–`V14` sinh từ ENTITY.md (65 bảng + Admin đầu tiên), `ddl-auto=validate` chạy xanh.
- [ ] GitHub Actions CI: build + test (Testcontainers).
- [ ] Astro + React khung 3 khu vực (storefront, seller, admin) + `api.ts` có refresh token.
- [ ] Đăng ký tài khoản sandbox: VNPay, GHN dev, SePay; Cloudflare Tunnel cho IPN / webhook.
- [ ] Lấy API key Gemini (Google AI Studio), OAuth client Google (Apple khi có tài khoản developer); tài khoản dev GHTK / ViettelPost / J&T nếu cấp được (không có thì adapter giả lập).

### 15.2 Rủi ro & cách giảm

| Rủi ro | Mức | Cách giảm |
|---|:---:|---|
| 87 UC trong 16 tuần với 4 người là **nhiều** khi xây từ đầu | Cao | Làm theo đúng thứ tự sprint; S1–S6 là vòng lõi phải xong; UC SHOULD của S8 rồi S7 cắt trước nếu trễ; không thêm tính năng ngoài tài liệu |
| Gemini trả kết quả thất thường / hết hạn mức miễn phí | Trung bình | Moderator luôn duyệt cuối; `fallback()` khi lỗi; ghi phản hồi gốc để chỉnh prompt |
| Hãng 3PL phụ không cấp tài khoản dev | Thấp | Adapter giả lập trả phí / trạng thái cố định; demo bằng GHN |
| Tính tiền xếp chồng 3 tầng + Điểm sai lệch vài đồng | Cao | Phân bổ dồn phần dư dòng cuối; test bất biến "tổng phân bổ = tổng giảm" và sổ cái cân bằng |
| Sprint 2 dồn 17 UC | Trung bình | Phần lớn là UC đọc dữ liệu (danh mục, tìm kiếm, chi tiết); AI Worker KYC giao riêng cho thành viên AI làm song song từ tuần 1 |
| Tích hợp VNPay / SePay / GHN cần URL HTTPS công khai | Trung bình | Cloudflare Tunnel từ S1; có file giả lập webhook (`scripts/*.http`) để test offline |
| Luồng tiền sai (bán vượt, IPN lặp, hoàn một phần) | Cao | 5 kịch bản test bắt buộc (Document_Vi 4.3) viết **cùng lúc** với code ở S3–S5 |
| Mô hình KYC cho kết quả kém trên ảnh thật | Trung bình | Luôn có đường lui `MANUAL_REVIEW`; ngưỡng hiệu chỉnh trên ~30 ảnh thử của nhóm |
| Kiểm thử bị dồn cuối | Cao | Mỗi PR có test; tuần 12 kiểm thử vòng lõi, tuần 16 chỉ để kiểm thử tải / bảo mật và sửa lỗi |

### 15.3 Theo dõi tiến độ
Dùng [CHECKLIST_TIEN_DO_UC.md](../CHECKLIST_TIEN_DO_UC.md) (tick theo từng lớp: giao diện, backend, test, nối API, chạy thật) và `diagrams/UseCaseDiagram/Quản Lý UseCase.xlsx`: cột *Hiện trạng mã nguồn* chuyển "Chưa triển khai" → "Đang triển khai" → "Đã triển khai" (cả dòng chuyển nền xám) khi UC đạt Definition of Done (API + giao diện + test + đã demo); cột *Ghi chú* ghi sprint dự kiến. File cùng tên ở `DATN/` đã đánh "Đã triển khai" cho 87 use case thuộc TLCN để theo dõi phần còn lại của đồ án tốt nghiệp.

---

<a id="phan-16"></a>
## 🧪 PHẦN 16: KIỂM THỬ, TRIỂN KHAI & CHECKLIST DEMO

- Chiến lược kiểm thử, 5 kịch bản bắt buộc của luồng tiền: [Document_Vi.md](document/Document_Vi.md) mục 4.3.
- Docker Compose, CI/CD, giám sát: Document_Vi.md mục 4.4–4.6.

**Checklist trước buổi bảo vệ:**
- [ ] Seed dữ liệu demo; tài khoản demo: khách, shop, Admin và 3 nhân viên (Moderator, CS Specialist, Finance Officer) để trình diễn nav theo quyền (Guest dùng trình duyệt ẩn danh).
- [ ] Gemini: API key còn hạn mức; có 3 sản phẩm mẫu cho 3 nhánh (APPROVE / NEED_REVIEW / REJECT).
- [ ] VNPay sandbox: thẻ test NCB `9704198526191432198`, OTP `123456` hoạt động; IPN trỏ tới domain public (HTTPS).
- [ ] SePay: liên kết tài khoản ngân hàng demo, webhook trỏ tới domain public, thử "Gửi thử"; chuẩn bị 1 lần chuyển khoản thật số tiền nhỏ.
- [ ] GHN môi trường dev: tạo được vận đơn, webhook đã cấu hình; có kịch bản giả lập webhook (`scripts/ghn-webhook.http`) khi mạng hội trường chặn.
- [ ] 9 kịch bản E2E chạy xanh (Document_Vi 4.3); k6 50 VU đạt NFR-01; ZAP không có High.
- [ ] Thử khôi phục từ bản `pg_dump` gần nhất.
- [ ] Kịch bản demo 20 phút: Admin mời Moderator → mở shop + KYC tự duyệt → đăng sản phẩm → Gemini chấm, Moderator duyệt → khách vào qua link chia sẻ, theo dõi shop → mua 2 shop với voucher Shop + Sàn + Freeship + Điểm (VNPay + COD) → shop xác nhận, in phiếu → webhook giao thành công → xác nhận nhận hàng, đánh giá + Hữu ích → ví shop cộng tiền, sổ cái → rút tiền → khách trả hàng một phần → hoàn tiền.

---

<a id="phan-17"></a>
## 📚 PHẦN 17: DANH MỤC API (`/api/v1`)

> Đặc tả đầy đủ (request / response mẫu, màn hình dùng, lỗi từng endpoint): **`document/API_SPEC.md`** — bảng dưới chỉ là mục lục. Đồng bộ TLCN v1.3 (2026-11-08).

| Phân hệ | Method & Path | Vai trò | UC |
|---|---|---|---|
| AUTH | `POST /auth/registrations`, `POST /auth/otp-challenges`, `POST /auth/otp-challenges/{id}/resend`, `POST /auth/otp-challenges/{id}/verify` (purpose `LOGIN`, `PASSWORD_RESET`, `ACCOUNT_UNLOCK`) | Guest | AUTH-01, 02, 04 |
| AUTH | `POST /auth/sessions`, `POST /auth/sessions/refresh`, `DELETE /auth/sessions/current`, `POST /auth/password-resets` | Mọi vai trò | AUTH-02, 04, 11 |
| AUTH | `GET /auth/oauth/{provider}/authorize`, `POST /auth/oauth/{provider}/token`, `GET /me/login-methods`, `POST /me/identities/{provider}/authorize`, `POST/DELETE /me/identities/{provider}` | Guest · mọi vai trò | AUTH-03 |
| AUTH | `POST /auth/staff-activations` | Nhân viên được mời | ADM-01 |
| AUTH | `GET /me`, `GET/PATCH /me/profile`, `POST /me/password/verify`, `PUT /me/password` 🔐, `POST /me/otp-challenges`, `POST /me/otp-challenges/{id}/resend` | Mọi vai trò | AUTH-06, 08 |
| AUTH | `GET /me/sessions`, `DELETE /me/sessions/{id}`, `DELETE /me/sessions?scope=others` | Mọi vai trò | AUTH-11 |
| AUTH | `PUT /me/deletion-request` 🔐, `DELETE /me/deletion-request`, `POST /me/email-change-requests`, `POST /me/email-change-requests/{id}/confirm` | Customer | AUTH-08, 10 |
| AUTH | `GET/POST /me/addresses`, `PUT/PATCH/DELETE /me/addresses/{id}`, `GET /public/regions/{provinces,…/districts,…/wards}`, `GET /public/regions/reverse-geocode`, `GET /public/banks` | Customer · Vendor | AUTH-09, VND-05 |
| — | `POST /uploads/presigned-urls` | Mọi vai trò | Dùng chung (Silo) |
| CAT | `GET /public/categories`, `GET /public/categories/{slug}`, `GET /public/products`, `GET /public/products/{id}`, `GET /public/products/{id}/variants`, `POST /public/products/{id}/views` | Guest | CAT-01, 02, 05, 06 |
| CAT | `GET /public/search/suggestions`, `GET /public/search/trending`, `GET /public/recommendations`, `GET /public/products/{id}/similar` | Guest | CAT-04, 07 |
| CAT | `GET /public/shops/{id}`, `GET /public/shops?featured=true` | Guest | CAT-16 |
| CAT | `GET /me/wishlist`, `GET /me/wishlist/product-ids`, `PUT/DELETE /me/wishlist/{product_id}` | Customer | CAT-08 |
| CAT | `GET /seller/products`, `GET/PUT/PATCH/DELETE /seller/products/{id}`, `POST /seller/products`, `GET /seller/categories/{id}/listing-rules`, `GET/POST /seller/brands` | Vendor | CAT-09, 10, 11, 15 |
| CAT | `GET /seller/categories`, `GET/POST /seller/category-proposals`, `GET /admin/category-proposals`, `POST /admin/category-proposals/{id}/{approve,reject}` | Vendor · `MANAGE_CATALOG` | CAT-17 |
| CAT | `GET /admin/product-moderation`, `GET /admin/product-moderation/{id}`, `POST /admin/products/{id}/{approve,reject,requeue,lock,unlock}`, `PUT /admin/products/{id}/assignee`, `GET /admin/moderators`, `PUT /admin/moderators/availability`, `POST /admin/moderators/redistribute` | `MANAGE_CATALOG` | CAT-11, 12, 13 |
| CAT | `GET/POST /admin/categories`, `PATCH/DELETE /admin/categories/{id}`, `GET/POST /admin/categories/{id}/attributes`, `PUT/PATCH /admin/categories/{id}/attributes/{code}`, `GET /admin/products?category_id=`, `GET/PATCH /admin/commission-settings` | `MANAGE_CATALOG` · `UPDATE_COMMISSION` | CAT-14, 15 |
| ORD | `GET /me/cart`, `GET /me/cart/items`, `POST /me/cart/items`, `PATCH /me/cart/items/{id}`, `DELETE /me/cart/items?ids=`, `POST /me/cart/items/batch`, `POST /me/cart/merge` | Customer | ORD-01 |
| ORD | `POST /me/checkout/preview`, `POST /me/checkout/applicable-vouchers`, `POST /me/checkout-sessions` 🔁, `GET /me/checkout-sessions/{id}`, `POST /me/checkout-sessions/{id}/cancel` | Customer | ORD-04, 06, 07, 10, PRO-04, 10 |
| ORD | `GET /me/orders`, `GET /me/orders/{id}`, `POST /me/orders/{id}/cancel`, `POST /me/orders/{id}/confirm-receipt` | Customer | ORD-09, 10, 11 |
| PAY | `POST /me/checkout-sessions/{id}/payments` 🔁, `GET /payments/vnpay/return`, `GET /payments/vnpay/ipn`, `POST /payments/sepay/webhook` | Customer · VNPay · SePay | PAY-01, 03 |
| VND | `POST /me/shop-registrations`, `POST /me/shop-registrations/{id}/confirm`, `GET /seller/me` | Customer → Vendor | VND-01 |
| VND | `GET /seller/kyc`, `PUT /seller/kyc/draft`, `POST /seller/kyc/submissions`, `POST /seller/kyc/manual-review-requests`, `PATCH /internal/kyc/{id}/result` | Vendor · AI Worker | VND-02, 03 |
| VND | `GET /admin/kyc-submissions`, `GET /admin/kyc-submissions/{id}`, `POST /admin/kyc-submissions/{id}/{approve,reject,request-supplement,requeue}` | `REVIEW_KYC` | VND-04 |
| VND | `GET/PATCH /seller/settings`, `PUT /seller/pickup-address`, `POST /seller/payout-account/verification`, `PUT/DELETE /seller/payout-account` 🔐, `PUT/DELETE /seller/vacation` | Vendor | VND-05, 06, 07, 19, 21 |
| VND | `GET /seller/dashboard`, `GET /seller/analytics/{revenue,overview,traffic-sources,top-products,sentiment}` | Vendor | VND-08, AI-07 |
| VND | `GET /seller/wallet`, `GET /seller/wallet/transactions`, `GET /seller/wallet/transactions/export`, `POST /seller/withdrawals` 🔁🔐 | Vendor | VND-20, ADM-04 |
| LOG | `GET /seller/orders`, `GET /seller/orders/{id}`, `POST /seller/orders/{id}/{confirm,reject,prepare}` | Vendor | LOG-16, LOG-01 |
| LOG | `GET /seller/search?q=` (tìm nhanh đơn + sản phẩm của shop) | Vendor | LOG-16, CAT-10 |
| LOG | `GET /seller/orders/{id}/shipping-quotes`, `POST /seller/orders/{id}/shipments` 🔁, `GET /seller/orders/{id}/shipping-label` | Vendor | LOG-02, 15 |
| LOG | `POST /shipping/{carrier}/webhook/{secret}`, `GET /admin/cod-remittances`, `GET /admin/cod-remittances/{id}`, `POST /admin/cod-remittances/{id}/{reconciliation-preview,confirm}` | 3PL · `PROCESS_PAYOUT` | LOG-03 |
| PRO | `GET /seller/vouchers`, `GET/PUT/PATCH /seller/vouchers/{id}`, `POST /seller/vouchers` | Vendor | PRO-01 |
| PRO | `GET/POST /admin/vouchers`, `GET/PUT/PATCH/DELETE /admin/vouchers/{id}` | `MANAGE_VOUCHER` | PRO-02 |
| PRO | `GET /public/vouchers`, `GET /me/vouchers`, `POST /me/vouchers`, `DELETE /me/vouchers/{id}` | Guest · Customer | PRO-03 |
| PRO | `GET /public/flash-sales/current`, `GET /seller/flash-sales`, `GET /seller/flash-sales/{id}/{eligible-products,items}`, `POST /seller/flash-sales/{id}/items`, `GET/POST /admin/flash-sales`, `GET /admin/flash-sales/{id}`, `POST /admin/flash-sales/{id}/{schedule,end}`, `PATCH /admin/flash-sales/{id}/items/{item_id}` | Guest · Vendor · `MANAGE_VOUCHER` | PRO-05, 06 |
| PRO | `GET /me/coins`, `POST /me/coins/check-ins`, `GET /me/coins/transactions`, `GET /public/coin-rewards`, `POST /me/coins/redemptions` | Customer | PRO-08, 09 |
| SOC | `GET/POST /me/conversations`, `GET/POST /me/conversations/{id}/messages`, `POST /me/conversations/{id}/read`, `GET /seller/conversations`, `GET/POST /seller/conversations/{id}/messages`, `POST /seller/conversations/{id}/read`, WS `/app/chat.send` | Customer · Vendor | SOC-01, 02 |
| SOC | `GET /public/products/{id}/reviews`, `POST /me/reviews`, `PUT/DELETE /reviews/{id}/helpful-vote`, `GET /seller/reviews`, `PUT /seller/reviews/{id}/reply` | Customer · Vendor | SOC-03, 04 |
| SOC | `POST /reviews/{id}/reports`, `GET /admin/review-reports`, `PATCH /admin/reviews/{id}` | Customer · Vendor · `MODERATE_REVIEW` | SOC-05 |
| SOC | `GET /me/following`, `GET /me/following/shop-ids`, `PUT/DELETE /me/following/{shop_id}` | Customer | SOC-06 |
| AI | `PATCH /internal/reviews/{id}/sentiment`, `GET /seller/analytics/sentiment` | AI Worker · Vendor | AI-07 |
| SUP | `POST /me/refund-requests` 🔁, `POST /me/refund-requests/{id}/{evidence,escalate,accept-rejection}` | Customer | SUP-01, 03 |
| SUP | `GET /seller/refund-requests`, `POST /seller/refund-requests/{id}/{request-evidence,approve,reject,confirm-return-received,escalate}` | Vendor | SUP-02 |
| SUP | `GET /admin/disputes`, `POST /admin/disputes/{id}/{resolve,confirm-return-received,reopen}`, `POST /admin/refunds/{id}/{sync,retry,mark-transferred}`, `POST /admin/escrows/{id}/{freeze,unfreeze}` | `HANDLE_DISPUTE` | SUP-03, 04 |
| NOT | `GET /me/notifications`, `PATCH /me/notifications/{id}`, `POST /me/notifications/mark-all-read`, WS `/user/queue/notifications` | Mọi vai trò | NOT-01 |
| NOT | `GET/POST /admin/platform-notifications`, `GET/PUT/DELETE /admin/platform-notifications/{id}`, WS `/topic/platform` | `MANAGE_PLATFORM_NOTIFICATION` | NOT-05 |
| ADM | `GET /admin/withdrawals`, `GET /admin/withdrawals/{id}/transfer`, `POST /admin/withdrawals/{id}/{approve,reject,complete}` | `PROCESS_PAYOUT` | ADM-05 |
| ADM | `GET /admin/users`, `POST /admin/users/{id}/{suspend,reactivate}` | `MANAGE_USER` | ADM-03 |
| ADM | `GET /admin/queues`, `GET /admin/dashboard/{finance,gmv-chart,operations,growing-shops,export}`, `GET /admin/ledger-entries`, `GET /admin/ledger-entries/export` | `VIEW_DASHBOARD` · `VIEW_FINANCE` | ADM-06, PAY-06 |
| ADM | `GET/PATCH /admin/me`, `GET /admin/permission-catalog`, `GET /admin/staff`, `POST /admin/staff/invitations`, `POST /admin/staff/{id}/invitations/resend`, `PUT /admin/staff/{id}/permissions`, `POST /admin/staff/{id}/{lock,unlock}`, `DELETE /admin/staff/{id}` | `MANAGE_RBAC` | ADM-01, 02 |
| ADM | `GET /admin/audit-logs`, `GET /admin/audit-logs/export` | `VIEW_AUDIT_LOG` | ADM-07 |
| ADM | `GET/POST /admin/banners`, `PUT/PATCH/DELETE /admin/banners/{id}`, `GET /admin/shops`, `GET /public/banners`, `POST /public/banner-events` | `MANAGE_VOUCHER` · Guest | ADM-12 |

🔁 = bắt buộc `Idempotency-Key`; 🔐 = bắt buộc OTP bước 2 (`X-OTP-Challenge-Id` + `X-OTP-Code`).

Định dạng phản hồi, phân trang, idempotency: Document_Vi.md mục 3.5; mã lỗi: mục 3.7.1.

---

## LỊCH SỬ PHIÊN BẢN

- **TLCN v1.4 (2026-11-16):** đồng bộ với mã nguồn: kho tệp MinIO → Silo; Phần 3.1 tên migration thật (`V6__payment.sql` chứa `cod_remittances`, `V14__auth_additions.sql` thay kế hoạch `V14__api_sync_v13.sql`); Phần 4 theo hiện thực AUTH (cổng `AuthorityProvider`, `family_id`, ân hạn 10 giây, Retry-After, Google); thêm 4.7 hiện trạng AUTH; 15.3 dùng checklist tiến độ UC.

- **TLCN v1.3 (2026-11-08):** Phần 17 viết lại theo `document/API_SPEC.md` (API REST theo màn hình); thêm migration `V14__api_sync_v13.sql` (65 bảng).

- **TLCN v1.2 (2026-11-06):** thêm UC-AI-07 phân tích cảm xúc đánh giá bằng PhoBERT (Phần 8.5, Sprint 6 thành 6 UC, API `AI-07`) — 87 use case; Genie / Điểm.

- **MVP v1.0 (2026-10-28):** viết mới từ bản đầy đủ `fix-doc/implement.md` (35 phần) cho phạm vi 57 use case; thay Kafka / RabbitMQ / Elasticsearch / Qdrant / Kubernetes bằng sự kiện nội bộ Spring, full-text PostgreSQL, Docker Compose; lộ trình 6 sprint × 2 tuần.
- **TLCN v1.1 (2026-10-29):** phủ toàn bộ giao diện client + prototype — 86 use case / 64 bảng / 8 sprint × 2 tuần; thêm RBAC 13 mã quyền (4.5), OAuth (4.6), Gemini (5.3), deeplink (5.4), bộ đếm (5.5), sổ cái (7.7), ví & cài đặt gian hàng (8.4), adapter đa hãng + xác nhận đơn (Phần 9), voucher 3 tầng + Điểm + Flash Sale (Phần 10), nhân viên / nhật ký / banner (Phần 13), i18n (Phần 14), API mới (Phần 17).
