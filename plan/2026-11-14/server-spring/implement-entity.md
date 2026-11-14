# IMPLEMENT ENTITY — Checklist đưa 65 entity của ENTITY.md vào `genie/server-spring`

> **Mục đích:** chia việc hiện thực toàn bộ tầng dữ liệu (enum, entity, khóa gộp, repository, migration Flyway) thành các lượt nhỏ, mỗi lượt một model (vd. Sonnet 5.5) làm trọn vẹn không sót.
> **Nguồn sự thật:** [ENTITY.md](../../../../implementation-docs/HOÀN%20CHỈNH/diagrams/ClassDiagram/ENTITY.md) — đường dẫn tuyệt đối `TLCN/implementation-docs/HOÀN CHỈNH/diagrams/ClassDiagram/ENTITY.md` (nằm **ngoài** repo `genie`; TLCN v1.3 — 65 entity, 5 `@IdClass`, 55 enum, 202 phương thức nghiệp vụ). File này **không chứa code**, chỉ chỉ ra *làm gì, ở đâu, kiểm thế nào*.
> **Quy ước kiến trúc:** [TECH_STACK_VA_QUY_UOC.md](../../../../implementation-docs/HOÀN%20CHỈNH/kien-thuc/TECH_STACK_VA_QUY_UOC.md) mục 4 (package, ArchUnit), mục 7 (kiểu dữ liệu, migration), mục 13 (nhánh, commit).
> Số dòng `Lxxxx` bên dưới là vị trí khối code của class trong ENTITY.md lúc lập checklist — nếu lệch, tìm theo tiêu đề mục (`## 5.2. \`Order\``).
> Mọi đường dẫn code trong file tính từ gốc repo `genie/` (vd. `server-spring/src/...`). File này nằm trong repo: `plan/2026-11-14/server-spring/implement-entity.md`.

| | |
|---|---|
| **Ngày lập** | 2026-11-14 |
| **Tổng** | 23 lượt (= 23 nhánh, 23 commit) — bảng ở mục 7 |
| **Cập nhật gần nhất** | 2026-11-14 — xong P00 (1 / 23 lượt); lượt kế: P01a |

---

## 0. CÁCH DÙNG FILE NÀY (đọc trước mỗi lượt)

1. **Một task = một lượt** trong bảng mục 7 (Phase nhỏ = 1 lượt; Phase lớn chia sẵn `a`, `b`, `c`). Không gộp nhiều lượt vào một lần làm.
   - **Bắt đầu lượt:** làm đúng mục 7.2 bước 1–3 (kiểm tra cây làm việc sạch → điền mã commit lượt trước vào mục 8 → **tạo nhánh mới** của lượt).
   - **Kết thúc lượt:** làm đúng mục 7.2 bước 4–7 (kiểm tra → **ghi nhật ký mục 8** → commit trên nhánh của lượt).
2. Trước khi viết một entity: đọc **toàn bộ mục** của nó trong ENTITY.md — khối code **và** phần `💡 Kiến thức chuyên sâu` (thường chứa `CHECK`, `UNIQUE`, index, `ON DELETE` chỉ có trong lời văn).
3. Chép **đủ 100%**: mọi trường, mọi annotation, mọi `@Builder.Default`, mọi phương thức nghiệp vụ (kể cả `private`, `static`, hằng `static final`), giữ nguyên chú thích tiếng Việt. Không tự thêm trường, không đổi tên, không rút gọn thân phương thức.
4. Chỉ được sửa so với ENTITY.md ở đúng các điểm trong **mục 2 — Điều chỉnh bắt buộc**.
5. Sau mỗi entity: tự đối chiếu với cột **Trường / PT** trong bảng của Phase (số khai báo `private` cấp class, tính cả trường quan hệ / số phương thức nghiệp vụ theo Phụ lục E). Lệch → đọc lại mục đó.
6. Làm xong mục nào thì đổi `- [ ]` thành `- [x]` ngay trong file này. Gặp chỗ ENTITY.md mâu thuẫn hoặc không rõ → **không tự đoán**, ghi vào **mục 6 — Nhật ký sai lệch** và báo người dùng.
7. Mỗi lượt (kể cả lượt `a`, `b` giữa Phase) đều phải `compile` + `ModuleBoundaryTest` xanh trước khi commit; lượt cuối Phase chạy thêm bước **Kiểm tra cuối Phase**. Chỉ tick lượt / Phase khi build xanh.

---

## 1. VỊ TRÍ CODE & HIỆN TRẠNG

- Gốc mã: `server-spring/src/main/java/com/genie/` — package `com.genie.<module>.<feature>.domain`.
- Migration: `server-spring/src/main/resources/db/migration/V1__identity.sql … V12__audit.sql` — hiện **chỉ có header + `-- TODO`, chưa chạy ở đâu** ⇒ được viết thẳng vào các file này (không tạo V14+).
- `application.yml`: `ddl-auto: validate` ⇒ tên bảng / cột / kiểu trong migration phải khớp entity tuyệt đối, nếu không app không khởi động.
- Đã có file **stub rỗng** (thay nội dung, giữ đường dẫn): `order/cart/domain/{Cart, CartItem, CartRepository}`, `order/checkout/domain/{CheckoutSession, SessionStatus, CheckoutSessionRepository}`, `order/order/domain/{Order, OrderItem, OrderDiscount, OrderStatus, OrderRepository}`, `order/reservation/domain/{StockReservation, StockReservationRepository}`, `common/audit/{SystemLog, SystemLogRepository}`.
- Tiện ích có sẵn ở `com.genie.common.domain`: `BaseEntity`, `Money`, `NameNormalizer`, `Haversine`.
- Ngoài phạm vi checklist này: service, controller, DTO, event, query tùy biến, seed dữ liệu (`R__seed_demo.sql`, `V13__bootstrap_admin.sql`).

---

## 2. ĐIỀU CHỈNH BẮT BUỘC SO VỚI ENTITY.md (chỉ những điểm này)

| # | Điều chỉnh | Lý do |
|---|---|---|
| D1 | `package` theo bảng mục 3; tự thêm `import` (ENTITY.md lược bỏ import). `NameNormalizer` import từ `com.genie.common.domain` (không phải `com.genie.platform…` như trong `java-code/`). | Cấu trúc repo thực tế |
| D2 | **Bỏ trường điều hướng JPA trỏ sang entity của module khác** (danh sách đầy đủ ở cột *Quan hệ bỏ* của từng Phase). Giữ nguyên trường `Long xxxId` tương ứng. Phương thức nghiệp vụ nào dùng trường bị bỏ → ghi mục 6 và hỏi. | ArchUnit `modules_only_talk_through_facades` + TECH_STACK 4.4 luật 4 |
| D3 | 5 enum dùng chung giữa nhiều module đặt ở `com.genie.common.domain`: `UserRole`, `PaymentMethod`, `PaymentStatus`, `VoucherType`, `FundingParty`. Mọi enum khác đặt cùng package với entity sở hữu (theo bảng B.4 / Phụ lục A). | `domain` của module A không được phụ thuộc `domain` của module B |
| D4 | Tạo 5 class khóa gộp (ENTITY.md chỉ nhắc tên): `VoucherCategoryId`, `VoucherProductId`, `VoucherExcludedCategoryId`, `VoucherExcludedProductId`, `UserPlatformNotificationReadId` — cùng package với entity; `implements Serializable`; constructor không tham số public; đúng tên & kiểu các trường `@Id` của entity; `equals`/`hashCode` trên tất cả trường khóa. | `@IdClass` bắt buộc |
| D5 | **Không** `extends BaseEntity`; khai báo `createdAt`/`updatedAt` đúng như từng class trong ENTITY.md (có bảng chỉ ghi thêm, không có `updatedAt`). | Giữ khớp 1-1 với ENTITY.md |
| D6 | Mỗi entity một repository `XxxRepository extends JpaRepository<Xxx, KiểuId>` đặt cùng package `domain` (IdClass → kiểu id là class khóa gộp; `SystemLog` id là `String`). Chưa thêm query tùy biến. | Con được ghi qua `Long xxxId` riêng nên cũng cần repository |
| D7 | Phương thức trong ENTITY.md tham chiếu class thuộc module khác (tham số, kiểu trả về, thân hàm) → không tự sửa chữ ký, ghi mục 6 và hỏi. | Tránh lệch thiết kế |

---

## 3. LUẬT VIẾT MIGRATION (áp dụng mọi Phase)

- **Tên cột:** camelCase → snake_case (Spring `CamelCaseToUnderscoresNamingStrategy`): `isDeleted` → `is_deleted`, `oauthProviderId` → `oauth_provider_id`; `@Column(name=…)` / `@JoinColumn(name=…)` thắng.
- **Kiểu:** `Long id` → `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` · `Long` → `BIGINT` · `Integer` → `INT` · `Boolean` → `BOOLEAN` · `String` → `VARCHAR(length)` (không ghi length → `VARCHAR(255)`) · `columnDefinition` → dùng y nguyên · `BigDecimal` → `NUMERIC(precision, scale)` (không ghi → `NUMERIC(19,2)`) · `OffsetDateTime`/`Instant` → `TIMESTAMPTZ` · `LocalDate` → `DATE` · trường `*Json` → `JSONB` · enum → `VARCHAR(50)` + `CHECK (cột IN (…))` liệt kê **đúng các giá trị Phụ lục A** · `@Version` → `BIGINT NOT NULL DEFAULT 0`.
- **NOT NULL:** cột có `nullable = false` hoặc `@Id`; cột ghi `// NULLABLE` thì cho NULL. `@Builder.Default` → thêm `DEFAULT` cùng giá trị.
- **Trường quan hệ** (`@ManyToOne`/`@OneToMany`/`@OneToOne` có `insertable = false`) **không sinh cột** — cột thật là trường `Long xxxId`.
- **Ràng buộc từ annotation:** `unique = true`, `@UniqueConstraint`, `@Index` trong `@Table` → tạo đúng tên đã cho; không có tên → `uk_<bảng>_<ý nghĩa>` / `idx_<bảng>_<cột>`.
- **Ràng buộc từ lời văn:** quét mục entity tìm `CHECK`, `UNIQUE`, `INDEX`, `partial`, `DDL:` và áp dụng; cộng với index mục **13.1** của ENTITY.md cho các bảng trong Phase.
- **Khóa ngoại** `fk_<bảng>_<bảng đích>`. `ON DELETE`: theo chú thích của trường nếu có → nếu không, tra Phụ lục B.1: ◆ Composition → `CASCADE`; FK nullable → `SET NULL`; còn lại để mặc định (RESTRICT). Trường ghi *Not a DB Foreign Key* / *FK mềm* / *không ràng buộc* → **không** tạo FK. FK sang module khác vẫn tạo ở DB (chỉ Java mới bỏ điều hướng).
- **FK trỏ tới bảng của file V sau** (vd. `vendor_profiles.category_id` ở V1 → `categories` ở V2; `voucher_usages.order_id` ở V4 → `orders` ở V5): tạo cột ở file hiện tại, thêm `ALTER TABLE … ADD CONSTRAINT …` ở **cuối file V của bảng đích**. Ghi danh sách các FK “nợ” vào mục 5 để không quên.
- Tạo bảng theo thứ tự cha trước con trong cùng file. Mỗi bảng có comment 1 dòng tên entity.

---

## 4. CÁC PHASE

> Cột: **Trường** = số khai báo `private` cấp class trong khối code (gồm trường quan hệ, đếm tự động) · **PT** = số phương thức nghiệp vụ (Phụ lục E) · **@V** = có `@Version`.

### Phase 0 — Nền chung

- [x] Tạo 5 enum dùng chung ở `com.genie.common.domain` (D3): `UserRole`, `PaymentMethod`, `PaymentStatus`, `VoucherType`, `FundingParty` — giá trị đúng Phụ lục A, giữ chú thích `// mở rộng:`.
- [x] Đọc mục **Quy ước chung 1–12** của ENTITY.md (L55–L110) và mục 2–3 của file này.
- [x] Kiểm tra cuối Phase: `cd server-spring && ./mvnw -q -DskipTests compile`.

---

### Phase 1 — Nhóm 1: Người dùng & định danh → `V1__identity.sql`

**Lượt 1a — Enum + entity**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `User` (L124) | `auth.profile.domain` | 20 | 7 | ✔ | Bỏ `vendorProfile`, `adminProfile`. Giữ `customerProfile`, `addresses`, `refreshTokens`. `@UniqueConstraint uk_users_oauth` |
| [ ] | `CustomerProfile` (L290) | `auth.profile.domain` | 14 | 2 | | Giữ `user` |
| [ ] | `Address` (L375) | `auth.address.domain` | 16 | 2 | | Xóa mềm `isDeleted` |
| [ ] | `RefreshToken` (L1029) | `auth.token.domain` | 9 | 2 | | |

- [ ] Enum: `AccountStatus`, `Gender` (auth.profile) · `AddressLabel` (auth.address). (`UserRole` đã ở Phase 0.)

**Lượt 1b — Entity còn lại**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `VendorProfile` (L468) | `vendor.shop.domain` | 51 | 15 | | Bỏ `user`. Entity lớn nhất — làm riêng, đếm kỹ 51 trường. Dùng `NameNormalizer` |
| [ ] | `KycResult` (L852) | `vendor.kyc.domain` | 29 | 3 | | Giữ `vendorProfile` (cùng module `vendor`) |
| [ ] | `AdminProfile` (L786) | `admin.staff.domain` | 9 | 2 | | Bỏ `user` |
| [ ] | `StaffPermission` (L1107) | `admin.rbac.domain` | 6 | 2 | | |

- [ ] Enum: `BusinessType`, `KycStatus`, `KycVerificationMethod` (vendor.shop) · `KycOverallResult` (vendor.kyc) · `StaffPermissionEnum` (admin.rbac) · `StaffTitle` (admin.staff — enum có constructor & `defaultPermissions()`, import `StaffPermissionEnum`).
- [ ] 8 repository (D6).

**Lượt 1c — Migration `V1__identity.sql`** (giữ 2 dòng `CREATE EXTENSION` đã có)
- [ ] Bảng theo thứ tự: `users` → `customer_profiles` → `addresses` → `refresh_tokens` → `vendor_profiles` → `kyc_results` → `admin_profiles` → `staff_permissions`.
- [ ] `customer_profiles.default_address_id`: **FK mềm — không tạo constraint** (ENTITY.md L319, L364; mục chi tiết thắng ví dụ ở Quy ước 9).
- [ ] FK nợ: `vendor_profiles.category_id` → `categories` (ghi mục 5, thêm ở V2).
- [ ] Unique 1-1: `customer_profiles.user_id`, `vendor_profiles.user_id`, `admin_profiles.user_id`, `kyc_results.vendor_profile_id`.
- [ ] Kiểm tra cuối Phase: compile + `./mvnw -q test -Dtest=ModuleBoundaryTest`.

---

### Phase 2 — Nhóm 2: Danh mục & sản phẩm → `V2__catalog.sql`

**Lượt 2a — Lõi sản phẩm**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `Category` (L1189) | `catalog.category.domain` | 11 | 2 | | Giữ `children` (tự tham chiếu `parent_id`) |
| [ ] | `Brand` (L1854) | `catalog.brand.domain` | 7 | 2 | | Có factory `static proposedBy` |
| [ ] | `Product` (L1276) | `catalog.product.domain` | 37 | 6 | ✔ | Bỏ `vendor` (→ VendorProfile, L1396). Giữ `category`, `variants`, `images` |
| [ ] | `ProductVariant` (L1509) | `catalog.product.domain` | 12 | 5 | ✔ | Giữ `product`. Xóa mềm bằng timestamp |
| [ ] | `ProductImage` (L1635) | `catalog.product.domain` | 12 | 1 | | |
| [ ] | `ProductDocument` (L1911) | `catalog.product.domain` | 10 | 1 | | Giữ `product`; `@Table indexes` |

- [ ] Enum: `ProductStatus`, `ShippingPayer`, `ProductCondition`, `ProductDocumentType` (catalog.product).

**Lượt 2b — Phụ trợ catalog**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `CategoryAttribute` (L1749) | `catalog.attribute.domain` | 15 | 2 | | Giữ `category` (cùng module) |
| [ ] | `UserWishlist` (L1716) | `catalog.wishlist.domain` | 4 | 0 | | Chỉ unique constraint |
| [ ] | `CategoryProposal` (L1972) | `catalog.proposal.domain` | 11 | 2 | | |
| [ ] | `ProductModerationResult` (L2041) | `catalog.moderation.domain` | 11 | 2 | | Giữ `product`; factory `static fallback` |
| [ ] | `SearchKeywordStat` (L2116) | `catalog.search.domain` | 6 | 1 | | |
| [ ] | `ProductViewStat` (L2162) | `catalog.stats.domain` | 8 | 1 | | |

- [ ] Enum: `AttributeDataType` (catalog.attribute) · `CategoryProposalStatus` (catalog.proposal) · `ModerationDecision` (catalog.moderation) · `TrafficSource` (catalog.stats).
- [ ] 12 repository.

**Lượt 2c — Migration `V2__catalog.sql`**
- [ ] Thứ tự: `categories` → `category_attributes` → `brands` → `products` → `product_variants` → `product_images` → `product_documents` → `category_proposals` → `product_moderation_results` → `search_keyword_stats` → `product_view_stats` → `user_wishlists`.
- [ ] `products.search_vector`: cột `tsvector` sinh tự động (`GENERATED ALWAYS AS … STORED`) từ `name`, `brand`, `description` qua `unaccent` — `unaccent` không IMMUTABLE nên tạo hàm bọc IMMUTABLE trước; **không** thêm trường này vào entity (validate bỏ qua cột thừa). Ghi mục 6 nếu chọn cách khác.
- [ ] Index mục 13.1: GIN `search_vector`, GIN `name gin_trgm_ops`, `(category_id, status, created_at DESC)`, `(vendor_id, status)`, `UNIQUE product_variants(sku)`; GIN `jsonb_path_ops` cho `products.specs_json` (mục 2.6).
- [ ] Trả FK nợ từ V1: `vendor_profiles.category_id` → `categories`.
- [ ] Kiểm tra cuối Phase: compile + ModuleBoundaryTest.

---

### Phase 3 — Nhóm 3: Giỏ hàng → `V3__cart.sql`

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `Cart` (L2218) | `order.cart.domain` (stub) | 4 | 4 | | Giữ `items` |
| [ ] | `CartItem` (L2269) | `order.cart.domain` (stub) | 7 | 2 | | Bỏ `productVariant` (→ catalog) |

- [ ] Thay nội dung stub `Cart`, `CartItem`, `CartRepository`; tạo `CartItemRepository`.
- [ ] Migration `carts` → `cart_items` (`cart_id` CASCADE; `product_variant_id` → `product_variants`).
- [ ] Kiểm tra cuối Phase.

---

### Phase 4 — Nhóm 4: Voucher, Flash Sale & Điểm → `V4__promotion.sql`

**Lượt 4a — Voucher**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `Voucher` (L2345) | `promotion.voucher.domain` | 27 | 5 | ✔ | Giữ 4 `@OneToMany` tới bảng nối |
| [ ] | `VoucherUsage` (L2539) | `promotion.voucher.domain` | 7 | 1 | | Giữ `voucher`; factory `static of` |
| [ ] | `UserVoucher` (L2604) | `promotion.voucher.domain` | 7 | 1 | | Bỏ `user` (→ auth). Giữ `voucher` |
| [ ] | `VoucherCategory` (L2676) | `promotion.voucher.domain` | 4 | 1 | | `@IdClass`; bỏ `category` (→ catalog) |
| [ ] | `VoucherProduct` (L2705) | `promotion.voucher.domain` | 4 | 1 | | `@IdClass`; bỏ `product` |
| [ ] | `VoucherExcludedCategory` (L2734) | `promotion.voucher.domain` | 4 | 1 | | `@IdClass`; bỏ `category` |
| [ ] | `VoucherExcludedProduct` (L2763) | `promotion.voucher.domain` | 4 | 1 | | `@IdClass`; bỏ `product` |

- [ ] 4 class khóa gộp (D4).
- [ ] Enum: `VoucherScope`, `DiscountType` (promotion.voucher). (`VoucherType`, `FundingParty` ở common.)

**Lượt 4b — Flash Sale & Điểm**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `FlashSaleCampaign` (L2813) | `promotion.flashsale.domain` | 9 | 4 | | Giữ `items` |
| [ ] | `FlashSaleItem` (L2881) | `promotion.flashsale.domain` | 9 | 5 | ✔ | |
| [ ] | `UserCoin` (L2993) | `promotion.coin.domain` | 5 | 3 | ✔ | |
| [ ] | `CoinTransaction` (L3040) | `promotion.coin.domain` | 8 | 2 | | Chỉ ghi thêm |
| [ ] | `DailyCheckin` (L3096) | `promotion.coin.domain` | 4 | 1 | | |

- [ ] Enum: `FlashSaleStatus`, `FlashSaleItemStatus` (promotion.flashsale) · `CoinTransactionType` (promotion.coin).
- [ ] 12 repository (bảng nối dùng kiểu id = class khóa gộp).

**Lượt 4c — Migration `V4__promotion.sql`**
- [ ] Thứ tự: `vouchers` → `voucher_usages` → `user_vouchers` → 4 bảng nối (PRIMARY KEY gộp 2 cột, CASCADE theo voucher) → `flash_sale_campaigns` → `flash_sale_items` → `user_coins` → `coin_transactions` → `daily_checkins`.
- [ ] FK nợ: `voucher_usages.checkout_session_id` → `checkout_sessions`, `voucher_usages.order_id` → `orders` (để lại cho V5 — mục 5).
- [ ] Kiểm tra cuối Phase.

---

### Phase 5 — Nhóm 5: Đơn hàng → `V5__order.sql`

**Lượt 5a — `Order` (làm riêng)**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `Order` (L3287) | `order.order.domain` (stub) | 34 | 22 | ✔ | Bỏ `vendor` (L3303). Giữ `items`, `discounts`. Chép đủ hằng `ALLOWED` (L3409, mọi `Map.entry`) + `transitionTo`, `isCod` private |

- [ ] Enum: `OrderStatus` (stub, 19 giá trị), `FundingStatus` (order.order).

**Lượt 5b — Còn lại**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `CheckoutSession` (L3183) | `order.checkout.domain` (stub) | 9 | 4 | | Giữ `orders` |
| [ ] | `OrderItem` (L3647) | `order.order.domain` (stub) | 17 | 2 | | Bỏ `productVariant` (→ catalog). Snapshot |
| [ ] | `OrderDiscount` (L3771) | `order.order.domain` (stub) | 7 | 1 | | `voucher_id` `ON DELETE SET NULL` |
| [ ] | `StockReservation` (L3826) | `order.reservation.domain` (stub) | 9 | 3 | ✔ | |

- [ ] Enum: `SessionStatus` (stub) · `StockReservationStatus` (order.reservation).
- [ ] Thay nội dung stub repository + tạo `OrderItemRepository`, `OrderDiscountRepository`.

**Lượt 5c — Migration `V5__order.sql`**
- [ ] Thứ tự: `checkout_sessions` → `orders` → `order_items` → `order_discounts` → `stock_reservations`.
- [ ] Index 13.1: `orders(customer_id, created_at DESC)`, `orders(vendor_id, status, created_at DESC)`, partial `orders(status, delivered_at) WHERE status = 'DELIVERED'`, partial `stock_reservations(status, expires_at) WHERE status = 'ACTIVE'`.
- [ ] Trả FK nợ từ V4.
- [ ] Kiểm tra cuối Phase.

---

### Phase 6 — Nhóm 6: Thanh toán & ký quỹ → `V6__payment.sql`

**Lượt 6a**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `PaymentTransaction` (L3927) | `payment.vnpay.domain` | 12 | 4 | ✔ | CHECK XOR `order_id`/`checkout_session_id` (mục 💡, L4027) |
| [ ] | `PaymentRefund` (L4060) | `payment.vnpay.domain` | 6 | 2 | | |
| [ ] | `EscrowLedger` (L4121) | `payment.escrow.domain` | 18 | 11 | | Có `private totalFunded()` |
| [ ] | `FinancialLedger` (L4655) | `payment.ledger.domain` | 10 | 1 | | Chỉ ghi thêm; factory `static entry` |

- [ ] Enum: `PaymentRefundStatus` (payment.vnpay) · `FundingSource`, `EscrowStatus` (payment.escrow) · `FinancialEventType` (payment.ledger).

**Lượt 6b**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `VendorWallet` (L4333) | `payment.wallet.domain` | 8 | 6 | ✔ | |
| [ ] | `WalletTransaction` (L4447) | `payment.wallet.domain` | 10 | 3 | | 2 factory `static debit/credit(VendorWallet …)` |
| [ ] | `WithdrawalRequest` (L4544) | `payment.wallet.domain` | 12 | 4 | | Đặt ở `payment` (sở hữu tiền); `admin.payout` gọi qua facade. Ghi mục 6 nếu muốn đổi |
| [ ] | `CodRemittance` (L4772) | `shipping.cod.domain` | 12 | 1 | | Dùng `CarrierType` của `shipping.shipment.domain` (cùng module) — nếu Phase 7 chưa làm, tạo enum đó ngay ở đây |

- [ ] Enum: `WalletTransactionType`, `WalletReferenceType`, `WithdrawalStatus` (payment.wallet) · `CodRemittanceStatus` (shipping.cod).
- [ ] 8 repository.

**Lượt 6c — Migration `V6__payment.sql`**
- [ ] Thứ tự: `payment_transactions` → `payment_refunds` → `escrow_ledgers` → `vendor_wallets` → `wallet_transactions` → `withdrawal_requests` → `financial_ledger` → `cod_remittances`.
- [ ] Index 13.1: `UNIQUE payment_transactions(idempotency_key)`, `UNIQUE (gateway_transaction_id)`, partial `escrow_ledgers(status, release_eligible_at) WHERE status = 'HELD'`.
- [ ] FK nợ: `payment_refunds.refund_request_id` → `refund_requests` (để lại cho V8 — mục 5).
- [ ] Kiểm tra cuối Phase.

---

### Phase 7 — Nhóm 7: Vận chuyển → `V7__shipping.sql`

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `ShipmentTracking` (L4847) | `shipping.shipment.domain` | 11 | 3 | | Giữ `events`; factory `static forReturn` |
| [ ] | `ShipmentEvent` (L4918) | `shipping.shipment.domain` | 7 | 1 | | |
| [ ] | `VendorShippingConfig` (L4984) | `shipping.shipment.domain` | 8 | 2 | | Bảng `vendor_shipping_config` (số ít) |

- [ ] Enum: `CarrierType` (nếu chưa tạo ở 6b), `ShipmentDirection`, `ShipmentTrackingStatus` (shipping.shipment).
- [ ] 3 repository.
- [ ] Migration: 2 partial unique index ghi ở L4844–4845 (`uq_shipment_outbound`, `uq_shipment_return`).
- [ ] Kiểm tra cuối Phase.

---

### Phase 8 — Nhóm 8: Hoàn tiền / khiếu nại → `V8__refund.sql`

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `RefundRequest` (L5054) | `support.refund.domain` | 22 | 16 | | Giữ `items`, `evidences`. 2 PT private (`applyApproval`, `requireStatus`). Đọc thêm bảng đồng bộ L5250 |
| [ ] | `RefundRequestItem` (L5284) | `support.refund.domain` | 5 | 1 | | |
| [ ] | `RefundEvidence` (L5319) | `support.refund.domain` | 7 | 2 | | |

- [ ] Enum: `RefundRequestType`, `RefundResolution`, `RefundStatus`, `MediaType` (support.refund).
- [ ] 3 repository · migration · trả FK nợ ở cuối V8: `payment_refunds.refund_request_id` (V6) và `shipment_trackings.refund_request_id` (V7) → `refund_requests`.
- [ ] Kiểm tra cuối Phase.

---

### Phase 9 — Nhóm 9: Đánh giá & tương tác shop → `V9__review.sql`

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `ProductReview` (L5414) | `social.review.domain` | 16 | 4 | | Index 13.1 `(vendor_id, created_at DESC)` + biểu thức `((aspect_sentiment_json ->> 'overall'))` |
| [ ] | `ReviewReport` (L5577) | `social.review.domain` | 6 | 1 | | |
| [ ] | `ReviewHelpfulVote` (L5632) | `social.review.domain` | 4 | 1 | | `review_id` CASCADE |
| [ ] | `ShopFollow` (L5536) | `social.follow.domain` | 4 | 1 | | |

- [ ] Enum: `ReviewReportReason` (social.review).
- [ ] 4 repository · migration · Kiểm tra cuối Phase.

---

### Phase 10 — Nhóm 10: Thông báo & nội dung sàn → `V10__notification.sql`

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `Notification` (L5670) | `notification.inbox.domain` | 16 | 3 | | Index 13.1 `(user_id, is_read, created_at DESC)` |
| [ ] | `PlatformNotification` (L5774) | `notification.platform.domain` | 12 | 1 | | |
| [ ] | `UserPlatformNotificationRead` (L5833) | `notification.platform.domain` | 3 | 1 | | `@IdClass` → tạo `UserPlatformNotificationReadId` (D4) |
| [ ] | `AdminNotification` (L5994) | `notification.admin.domain` | 9 | 1 | | |
| [ ] | `HomeBanner` (L5893) | `admin.banner.domain` | 17 | 4 | | `@Table indexes` |

- [ ] Enum: `NotificationType`, `NotificationChannel`, `NotificationDeliveryStatus` (notification.inbox) · `PlatformNotificationTarget` (notification.platform) · `AdminNotificationType` (notification.admin) · `BannerPosition` (admin.banner).
- [ ] 5 repository · migration · Kiểm tra cuối Phase.

---

### Phase 11 — Nhóm 11 & 12: Chat + Nhật ký → `V11__chat.sql`, `V12__audit.sql`

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [ ] | `Conversation` (L6058) | `social.chat.domain` | 10 | 2 | | Giữ `messages`; `@Table` có unique constraint |
| [ ] | `Message` (L6123) | `social.chat.domain` | 10 | 1 | | Index 13.1 `(conversation_id, created_at DESC)` |
| [ ] | `SystemLog` (L6207) | `common.audit` (stub) | 12 | 2 (+`prePersist`) | | Id `String` UUID sinh ở `@PrePersist`; chỉ ghi thêm; index `(created_at, action_type)` |

- [ ] Enum: `MessageType` (social.chat) · `AuditAction` (common.audit).
- [ ] `ConversationRepository`, `MessageRepository`; thay stub `SystemLogRepository` (`JpaRepository<SystemLog, String>`).
- [ ] Migration V11, V12 · Kiểm tra cuối Phase.

---

### Phase 12 — Nghiệm thu toàn bộ

- [ ] **Đếm tổng:** 65 class `@Entity`, 5 class khóa gộp, 55 enum (5 ở `common.domain`, `AuditAction` ở `common.audit`, 49 trong các module), 65 repository — liệt kê bằng `grep -rl "@Entity"` và so với Mục lục ENTITY.md (L25–L39).
- [ ] **Phương thức:** với mỗi entity, số phương thức nghiệp vụ khớp Phụ lục E (L7193–L7458; tổng 202).
- [ ] **Enum:** mỗi enum khớp từng giá trị Phụ lục A (L6357–L6497) và mỗi `CHECK` trong SQL khớp enum Java.
- [ ] **Quan hệ:** mục 5 không còn FK nợ chưa trả; mọi FK trong Phụ lục B.1 (101 quan hệ) có constraint ở DB (trừ *Not a DB Foreign Key*).
- [ ] **Test kiểm schema:** tạo `src/test/java/com/genie/support/SchemaValidationIT.java` — `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + Testcontainers `postgres:16`, Flyway chạy V1–V12, Hibernate `validate`; test rỗng chỉ cần context khởi động.
- [ ] Chạy `./mvnw -q verify` (cần Docker): compile, `ModuleBoundaryTest`, `SchemaValidationIT` đều xanh.
- [ ] Không còn file domain nào chứa chú thích `TODO: hiện thực` trong 11 module + `common/audit`.

---

## 5. SỔ FK “NỢ” (khóa ngoại trỏ tới bảng của file V sau)

| ✓ | Cột (file tạo cột) | Bảng đích | Thêm `ALTER TABLE` ở |
|---|---|---|---|
| [ ] | `vendor_profiles.category_id` (V1) | `categories` | cuối V2 |
| [ ] | `voucher_usages.checkout_session_id` (V4) | `checkout_sessions` | cuối V5 |
| [ ] | `voucher_usages.order_id` (V4) | `orders` | cuối V5 |
| [ ] | `payment_refunds.refund_request_id` (V6) | `refund_requests` | cuối V8 |
| [ ] | `shipment_trackings.refund_request_id` (V7) | `refund_requests` | cuối V8 |
| [ ] | *(bổ sung khi phát hiện thêm trong lúc làm)* | | |

---

## 6. NHẬT KÝ SAI LỆCH / CÂU HỎI

> Ghi mọi chỗ phải lệch khỏi ENTITY.md ngoài D1–D7, mọi mâu thuẫn tìm thấy, mọi quyết định cần người dùng chốt. Định dạng: `Phase · Entity · vấn đề · đề xuất · trạng thái`.

- *(trống)*

---

## 7. NHÁNH & COMMIT CHO TỪNG LƯỢT (TECH_STACK mục 13)

### 7.1 Quy ước

- **Mỗi lượt một nhánh mới, một commit.** Tên nhánh: `feature/entity-p<NN><lượt>-<mo-ta>` — chữ thường, gạch nối. Chưa có mã Jira nên bỏ phần `GNE-<số>` khỏi tên nhánh và commit (giống tiền lệ `plan/2026-11-11/web`, quyết định D6); có mã thì thêm ` [#GNE-<số>]` cuối commit.
- **Nhánh xếp chồng:** lượt sau phụ thuộc code lượt trước (vd. 1c cần entity của 1a, 1b) ⇒ nhánh lượt đầu tiên (P00) tách từ `develop`; **mỗi lượt sau tách từ nhánh của lượt ngay trước**. Nếu nhánh lượt trước đã được merge vào `develop` thì tách từ `develop` mới nhất (`git switch develop && git pull`).
- **Commit:** đúng `commitlint.config.cjs` — `<type>(<scope>): <subject>`, subject tiếng Anh, chữ thường, không dấu chấm cuối, dòng đầu ≤ 100 ký tự; body tiếng Việt nếu cần. Dùng thông điệp ghi sẵn ở bảng 7.3. Không `--no-verify`, không `--author`; hook commitlint / lint-staged lỗi thì sửa nguyên nhân.
- Commit gồm: code của lượt **+** file này (đã tick checklist và ghi nhật ký mục 8). Lượt P00 commit thêm chính file plan này lần đầu.
- **Không tự `git push`, không mở PR** — chỉ làm khi người dùng yêu cầu (khi đó mở PR xếp chồng: PR của lượt N có base là nhánh lượt N−1, PR P00 base `develop`).

### 7.2 Quy trình một lượt

1. `git status` phải sạch (trừ file plan này). Có thay đổi lạ → dừng, báo người dùng.
2. Nếu lượt trước đã commit: điền mã commit 7 ký tự (`git log -1 --format=%h` trên nhánh lượt trước) vào cột **Commit** của dòng lượt trước ở mục 8.
3. Tạo nhánh: `git switch <nhánh lượt trước | develop>` rồi `git switch -c <nhánh lượt này>` (tên ở bảng 7.3).
4. Làm việc theo checklist Phase ở mục 4; tick `[x]` từng mục.
5. Kiểm tra: `cd server-spring && ./mvnw -q -DskipTests compile` và `./mvnw -q test -Dtest=ModuleBoundaryTest` (lượt cuối Phase / P12 chạy thêm kiểm tra tương ứng). Đỏ → sửa, không commit.
6. **Ghi nhật ký:** thêm 1 dòng vào bảng mục 8 (cột Commit để `(lượt sau điền)`), tick cột ✓ của lượt ở bảng 7.3, cập nhật dòng **Cập nhật gần nhất** ở đầu file; sai lệch / câu hỏi phát sinh ghi thêm vào mục 6.
7. `git add` đúng các file của lượt + file plan này → `git commit -m "<thông điệp bảng 7.3>"`. Lượt cuối cùng (P12): sau commit, điền luôn mã commit của chính nó bằng một commit `docs(db): record entity implementation log`.

### 7.3 Bảng lượt → nhánh → commit

| ✓ | Lượt | Nhánh | Thông điệp commit |
|---|---|---|---|
| [x] | P00 | `feature/entity-p00-shared-enums` | `feat(common): add shared enums for entity layer` |
| [ ] | P01a | `feature/entity-p01a-auth-entities` | `feat(auth): add user, profile, address and refresh token entities` |
| [ ] | P01b | `feature/entity-p01b-vendor-admin-entities` | `feat(vendor): add vendor profile, kyc and staff permission entities` |
| [ ] | P01c | `feature/entity-p01c-identity-migration` | `feat(db): add identity schema migration` |
| [ ] | P02a | `feature/entity-p02a-catalog-core` | `feat(catalog): add category, brand and product entities` |
| [ ] | P02b | `feature/entity-p02b-catalog-support` | `feat(catalog): add attribute, proposal, moderation and stats entities` |
| [ ] | P02c | `feature/entity-p02c-catalog-migration` | `feat(db): add catalog schema migration` |
| [ ] | P03 | `feature/entity-p03-cart` | `feat(order): add cart entities and migration` |
| [ ] | P04a | `feature/entity-p04a-voucher` | `feat(promotion): add voucher entities and composite keys` |
| [ ] | P04b | `feature/entity-p04b-flashsale-coin` | `feat(promotion): add flash sale and coin entities` |
| [ ] | P04c | `feature/entity-p04c-promotion-migration` | `feat(db): add promotion schema migration` |
| [ ] | P05a | `feature/entity-p05a-order-aggregate` | `feat(order): add order entity with status transition table` |
| [ ] | P05b | `feature/entity-p05b-checkout-reservation` | `feat(order): add checkout, order item and stock reservation entities` |
| [ ] | P05c | `feature/entity-p05c-order-migration` | `feat(db): add order schema migration` |
| [ ] | P06a | `feature/entity-p06a-payment-escrow` | `feat(payment): add payment, escrow and ledger entities` |
| [ ] | P06b | `feature/entity-p06b-wallet-cod` | `feat(payment): add wallet, withdrawal and cod remittance entities` |
| [ ] | P06c | `feature/entity-p06c-payment-migration` | `feat(db): add payment schema migration` |
| [ ] | P07 | `feature/entity-p07-shipping` | `feat(shipping): add shipment entities and migration` |
| [ ] | P08 | `feature/entity-p08-refund` | `feat(support): add refund entities and migration` |
| [ ] | P09 | `feature/entity-p09-review` | `feat(social): add review and shop follow entities and migration` |
| [ ] | P10 | `feature/entity-p10-notification` | `feat(notification): add notification and banner entities and migration` |
| [ ] | P11 | `feature/entity-p11-chat-audit` | `feat(social): add chat and audit log entities and migrations` |
| [ ] | P12 | `feature/entity-p12-schema-verification` | `test(db): add schema validation integration test` |

---

## 8. NHẬT KÝ THỰC HIỆN

> Mỗi lượt thêm **đúng 1 dòng** ngay trước khi commit (mục 7.2 bước 6). Không sửa dòng cũ, trừ cột Commit (điền ở lượt sau). Lượt làm dở / bị dừng vẫn ghi, cột Kết quả ghi `DỪNG` + lý do.

| # | Ngày | Lượt | Nhánh (tách từ) | Commit | Đã làm (entity / enum / repo / migration) | Kiểm tra | Kết quả & ghi chú |
|---|---|---|---|---|---|---|---|
| 1 | 2026-11-14 | P00 | `feature/entity-p00-shared-enums` (từ `develop`) | (lượt sau điền) | 5 enum ở `common.domain`: `UserRole`, `PaymentMethod`, `PaymentStatus`, `VoucherType`, `FundingParty`; commit kèm file plan | `compile` ✔, `ModuleBoundaryTest` ✔ | Xong. Không có sai lệch so với Phụ lục A |
