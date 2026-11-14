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
| **Cập nhật gần nhất** | 2026-11-14 — xong tới P07 (18 / 23 lượt); lượt kế: P08 |

---

## 0. CÁCH DÙNG FILE NÀY (đọc trước mỗi lượt)

1. **Một task = một lượt** trong bảng mục 7 (Phase nhỏ = 1 lượt; Phase lớn chia sẵn `a`, `b`, `c`). Không gộp nhiều lượt vào một lần làm.
   - **Bắt đầu lượt:** làm đúng mục 7.2 bước 1–3 (kiểm tra cây làm việc sạch → điền mã commit lượt trước vào mục 8 → **tạo nhánh mới** của lượt).
   - **Kết thúc lượt:** làm đúng mục 7.2 bước 4–7 (kiểm tra → **ghi nhật ký mục 8** → commit trên nhánh của lượt).
2. Trước khi viết một entity: đọc **toàn bộ mục** của nó trong ENTITY.md — khối code **và** phần `💡 Kiến thức chuyên sâu` (thường chứa `CHECK`, `UNIQUE`, index, `ON DELETE` chỉ có trong lời văn).
3. Chép **đủ 100%**: mọi trường, mọi annotation, mọi `@Builder.Default`, mọi phương thức nghiệp vụ (kể cả `private`, `static`, hằng `static final`), giữ nguyên chú thích tiếng Việt. Không tự thêm trường, không đổi tên, không rút gọn thân phương thức.
4. Chỉ được sửa so với ENTITY.md ở đúng các điểm trong **mục 2 — Điều chỉnh bắt buộc** (D1–D8).
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
| D8 | Trường `Long xxxId` nào trùng cột với một `@JoinColumn(name = "xxx_id", insertable = false, updatable = false)` (kể cả `@OneToMany` của entity cha trỏ sang bảng con) → thêm `name = "xxx_id"` vào `@Column` của trường đó (thiếu `@Column` thì thêm `@Column(name = "xxx_id")`). Cột vật lý không đổi. | Hibernate 6 ném `DuplicateMappingException` khi một cột có 2 tên logic (`userId` vs `user_id`) — phát hiện ở P01c bằng `SchemaValidationIT` |
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
| [x] | `User` (L124) | `auth.profile.domain` | 20 | 7 | ✔ | Bỏ `vendorProfile`, `adminProfile`. Giữ `customerProfile`, `addresses`, `refreshTokens`. `@UniqueConstraint uk_users_oauth` |
| [x] | `CustomerProfile` (L290) | `auth.profile.domain` | 14 | 2 | | Giữ `user` |
| [x] | `Address` (L375) | `auth.address.domain` | 16 | 2 | | Xóa mềm `isDeleted` |
| [x] | `RefreshToken` (L1029) | `auth.token.domain` | 9 | 2 | | |

- [x] Enum: `AccountStatus`, `Gender` (auth.profile) · `AddressLabel` (auth.address). (`UserRole` đã ở Phase 0.)

**Lượt 1b — Entity còn lại**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [x] | `VendorProfile` (L468) | `vendor.shop.domain` | 51 | 15 | | Bỏ `user`. Entity lớn nhất — làm riêng, đếm kỹ 51 trường. Dùng `NameNormalizer` |
| [x] | `KycResult` (L852) | `vendor.kyc.domain` | 29 | 3 | | Giữ `vendorProfile` (cùng module `vendor`) |
| [x] | `AdminProfile` (L786) | `admin.staff.domain` | 9 | 2 | | Bỏ `user` |
| [x] | `StaffPermission` (L1107) | `admin.rbac.domain` | 6 | 2 | | |

- [x] Enum: `BusinessType`, `KycStatus`, `KycVerificationMethod` (vendor.shop) · `KycOverallResult` (vendor.kyc) · `StaffPermissionEnum` (admin.rbac) · `StaffTitle` (admin.staff — enum có constructor & `defaultPermissions()`, import `StaffPermissionEnum`).
- [x] 8 repository (D6).

**Lượt 1c — Migration `V1__identity.sql`** (giữ 2 dòng `CREATE EXTENSION` đã có)
- [x] Bảng theo thứ tự: `users` → `customer_profiles` → `addresses` → `refresh_tokens` → `vendor_profiles` → `kyc_results` → `admin_profiles` → `staff_permissions`.
- [x] `customer_profiles.default_address_id`: **FK mềm — không tạo constraint** (ENTITY.md L319, L364; mục chi tiết thắng ví dụ ở Quy ước 9).
- [x] FK nợ: `vendor_profiles.category_id` → `categories` (ghi mục 5, thêm ở V2).
- [x] Unique 1-1: `customer_profiles.user_id`, `vendor_profiles.user_id`, `admin_profiles.user_id`, `kyc_results.vendor_profile_id`.
- [x] Kiểm tra cuối Phase: compile + `./mvnw -q test -Dtest=ModuleBoundaryTest`.

---

### Phase 2 — Nhóm 2: Danh mục & sản phẩm → `V2__catalog.sql`

**Lượt 2a — Lõi sản phẩm**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [x] | `Category` (L1189) | `catalog.category.domain` | 11 | 2 | | Giữ `children` (tự tham chiếu `parent_id`) |
| [x] | `Brand` (L1854) | `catalog.brand.domain` | 7 | 2 | | Có factory `static proposedBy` |
| [x] | `Product` (L1276) | `catalog.product.domain` | 37 | 6 | ✔ | Bỏ `vendorProfile` (→ VendorProfile, L1396). Giữ `category`, `variants`, `images` |
| [x] | `ProductVariant` (L1509) | `catalog.product.domain` | 12 | 5 | ✔ | Giữ `product`. Xóa mềm bằng timestamp |
| [x] | `ProductImage` (L1635) | `catalog.product.domain` | 12 | 1 | | |
| [x] | `ProductDocument` (L1911) | `catalog.product.domain` | 10 | 1 | | Giữ `product`; `@Table indexes` |

- [x] Enum: `ProductStatus`, `ShippingPayer`, `ProductCondition`, `ProductDocumentType` (catalog.product).

**Lượt 2b — Phụ trợ catalog**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [x] | `CategoryAttribute` (L1749) | `catalog.attribute.domain` | 15 | 2 | | Giữ `category` (cùng module) |
| [x] | `UserWishlist` (L1716) | `catalog.wishlist.domain` | 4 | 0 | | Chỉ unique constraint |
| [x] | `CategoryProposal` (L1972) | `catalog.proposal.domain` | 11 | 2 | | |
| [x] | `ProductModerationResult` (L2041) | `catalog.moderation.domain` | 11 | 2 | | Giữ `product`; factory `static fallback` |
| [x] | `SearchKeywordStat` (L2116) | `catalog.search.domain` | 6 | 1 | | |
| [x] | `ProductViewStat` (L2162) | `catalog.stats.domain` | 8 | 1 | | |

- [x] Enum: `AttributeDataType` (catalog.attribute) · `CategoryProposalStatus` (catalog.proposal) · `ModerationDecision` (catalog.moderation) · `TrafficSource` (catalog.stats).
- [x] 12 repository.

**Lượt 2c — Migration `V2__catalog.sql`**
- [x] Thứ tự: `categories` → `category_attributes` → `brands` → `products` → `product_variants` → `product_images` → `product_documents` → `category_proposals` → `product_moderation_results` → `search_keyword_stats` → `product_view_stats` → `user_wishlists`.
- [x] `products.search_vector`: cột `tsvector` sinh tự động (`GENERATED ALWAYS AS … STORED`) từ `name`, `brand`, `description` qua `unaccent` — `unaccent` không IMMUTABLE nên tạo hàm bọc IMMUTABLE trước; **không** thêm trường này vào entity (validate bỏ qua cột thừa). Ghi mục 6 nếu chọn cách khác.
- [x] Index mục 13.1: GIN `search_vector`, GIN `name gin_trgm_ops`, `(category_id, status, created_at DESC)`, `(vendor_id, status)`, `UNIQUE product_variants(sku)`; GIN `jsonb_path_ops` cho `products.specs_json` (mục 2.6).
- [x] Trả FK nợ từ V1: `vendor_profiles.category_id` → `categories`.
- [x] Kiểm tra cuối Phase: compile + ModuleBoundaryTest.

---

### Phase 3 — Nhóm 3: Giỏ hàng → `V3__cart.sql`

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [x] | `Cart` (L2218) | `order.cart.domain` (stub) | 4 | 4 | | Giữ `items` |
| [x] | `CartItem` (L2269) | `order.cart.domain` (stub) | 7 | 2 | | Bỏ `variant` (→ catalog) |

- [x] Thay nội dung stub `Cart`, `CartItem`, `CartRepository`; tạo `CartItemRepository`.
- [x] Migration `carts` → `cart_items` (`cart_id` CASCADE; `product_variant_id` → `product_variants`).
- [x] Kiểm tra cuối Phase.

---

### Phase 4 — Nhóm 4: Voucher, Flash Sale & Điểm → `V4__promotion.sql`

**Lượt 4a — Voucher**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [x] | `Voucher` (L2345) | `promotion.voucher.domain` | 27 | 5 | ✔ | Giữ 4 `@OneToMany` tới bảng nối |
| [x] | `VoucherUsage` (L2539) | `promotion.voucher.domain` | 7 | 1 | | Giữ `voucher`; factory `static of` |
| [x] | `UserVoucher` (L2604) | `promotion.voucher.domain` | 7 | 1 | | Bỏ `user` (→ auth). Giữ `voucher` |
| [x] | `VoucherCategory` (L2676) | `promotion.voucher.domain` | 4 | 1 | | `@IdClass`; bỏ `category` (→ catalog) |
| [x] | `VoucherProduct` (L2705) | `promotion.voucher.domain` | 4 | 1 | | `@IdClass`; bỏ `product` |
| [x] | `VoucherExcludedCategory` (L2734) | `promotion.voucher.domain` | 4 | 1 | | `@IdClass`; bỏ `category` |
| [x] | `VoucherExcludedProduct` (L2763) | `promotion.voucher.domain` | 4 | 1 | | `@IdClass`; bỏ `product` |

- [x] 4 class khóa gộp (D4).
- [x] Enum: `VoucherScope`, `DiscountType` (promotion.voucher). (`VoucherType`, `FundingParty` ở common.)

**Lượt 4b — Flash Sale & Điểm**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [x] | `FlashSaleCampaign` (L2813) | `promotion.flashsale.domain` | 9 | 4 | | Giữ `items` |
| [x] | `FlashSaleItem` (L2881) | `promotion.flashsale.domain` | 9 | 5 | ✔ | |
| [x] | `UserCoin` (L2993) | `promotion.coin.domain` | 5 | 3 | ✔ | |
| [x] | `CoinTransaction` (L3040) | `promotion.coin.domain` | 8 | 2 | | Chỉ ghi thêm |
| [x] | `DailyCheckin` (L3096) | `promotion.coin.domain` | 4 | 1 | | |

- [x] Enum: `FlashSaleStatus`, `FlashSaleItemStatus` (promotion.flashsale) · `CoinTransactionType` (promotion.coin).
- [x] 12 repository (bảng nối dùng kiểu id = class khóa gộp).

**Lượt 4c — Migration `V4__promotion.sql`**
- [x] Thứ tự: `vouchers` → `voucher_usages` → `user_vouchers` → 4 bảng nối (PRIMARY KEY gộp 2 cột, CASCADE theo voucher) → `flash_sale_campaigns` → `flash_sale_items` → `user_coins` → `coin_transactions` → `daily_checkins`.
- [x] FK nợ: `voucher_usages.checkout_session_id` → `checkout_sessions`, `voucher_usages.order_id` → `orders` (để lại cho V5 — mục 5).
- [x] Kiểm tra cuối Phase.

---

### Phase 5 — Nhóm 5: Đơn hàng → `V5__order.sql`

**Lượt 5a — `Order` (làm riêng)**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [x] | `Order` (L3287) | `order.order.domain` (stub) | 34 | 22 | ✔ | Bỏ `vendorProfile` (L3303). Giữ `items`, `discounts`. Chép đủ hằng `ALLOWED` (L3409, mọi `Map.entry`) + `transitionTo`, `isCod` private |

- [x] Enum: `OrderStatus` (stub, 19 giá trị), `FundingStatus` (order.order).

**Lượt 5b — Còn lại**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [x] | `CheckoutSession` (L3183) | `order.checkout.domain` (stub) | 9 | 4 | | Giữ `orders` |
| [x] | `OrderItem` (L3647) | `order.order.domain` (stub) | 17 | 2 | | Bỏ `productVariant` (→ catalog). Snapshot |
| [x] | `OrderDiscount` (L3771) | `order.order.domain` (stub) | 7 | 1 | | `voucher_id` `ON DELETE SET NULL` |
| [x] | `StockReservation` (L3826) | `order.reservation.domain` (stub) | 9 | 3 | ✔ | |

- [x] Enum: `SessionStatus` (stub) · `StockReservationStatus` (order.reservation).
- [x] Thay nội dung stub repository + tạo `OrderItemRepository`, `OrderDiscountRepository`.

**Lượt 5c — Migration `V5__order.sql`**
- [x] Thứ tự: `checkout_sessions` → `orders` → `order_items` → `order_discounts` → `stock_reservations`.
- [x] Index 13.1: `orders(customer_id, created_at DESC)`, `orders(vendor_id, status, created_at DESC)`, partial `orders(status, delivered_at) WHERE status = 'DELIVERED'`, partial `stock_reservations(status, expires_at) WHERE status = 'ACTIVE'`.
- [x] Trả FK nợ từ V4.
- [x] Kiểm tra cuối Phase.

---

### Phase 6 — Nhóm 6: Thanh toán & ký quỹ → `V6__payment.sql`

**Lượt 6a**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [x] | `PaymentTransaction` (L3927) | `payment.vnpay.domain` | 12 | 4 | ✔ | CHECK XOR `order_id`/`checkout_session_id` (mục 💡, L4027) |
| [x] | `PaymentRefund` (L4060) | `payment.vnpay.domain` | 6 | 2 | | |
| [x] | `EscrowLedger` (L4121) | `payment.escrow.domain` | 18 | 11 | | Có `private totalFunded()` |
| [x] | `FinancialLedger` (L4655) | `payment.ledger.domain` | 10 | 1 | | Chỉ ghi thêm; factory `static entry` |

- [x] Enum: `PaymentRefundStatus` (payment.vnpay) · `FundingSource`, `EscrowStatus` (payment.escrow) · `FinancialEventType` (payment.ledger).

**Lượt 6b**

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [x] | `VendorWallet` (L4333) | `payment.wallet.domain` | 8 | 6 | ✔ | |
| [x] | `WalletTransaction` (L4447) | `payment.wallet.domain` | 10 | 3 | | 2 factory `static debit/credit(VendorWallet …)` |
| [x] | `WithdrawalRequest` (L4544) | `payment.wallet.domain` | 12 | 4 | | Đặt ở `payment` (sở hữu tiền); `admin.payout` gọi qua facade. Ghi mục 6 nếu muốn đổi |
| [x] | `CodRemittance` (L4772) | `shipping.cod.domain` | 12 | 1 | | Dùng `CarrierType` của `shipping.shipment.domain` (cùng module) — nếu Phase 7 chưa làm, tạo enum đó ngay ở đây |

- [x] Enum: `WalletTransactionType`, `WalletReferenceType`, `WithdrawalStatus` (payment.wallet) · `CodRemittanceStatus` (shipping.cod).
- [x] 8 repository.

**Lượt 6c — Migration `V6__payment.sql`**
- [x] Thứ tự: `payment_transactions` → `payment_refunds` → `escrow_ledgers` → `vendor_wallets` → `wallet_transactions` → `withdrawal_requests` → `financial_ledger` → `cod_remittances`.
- [x] Index 13.1: `UNIQUE payment_transactions(idempotency_key)`, `UNIQUE (gateway_transaction_id)`, partial `escrow_ledgers(status, release_eligible_at) WHERE status = 'HELD'`.
- [x] FK nợ: `payment_refunds.refund_request_id` → `refund_requests` (để lại cho V8 — mục 5).
- [x] Kiểm tra cuối Phase.

---

### Phase 7 — Nhóm 7: Vận chuyển → `V7__shipping.sql`

| ✓ | Entity (dòng) | Package | Trường | PT | @V | Quan hệ bỏ (D2) / ghi chú |
|---|---|---|---|---|---|---|
| [x] | `ShipmentTracking` (L4847) | `shipping.shipment.domain` | 11 | 3 | | Giữ `events`; factory `static forReturn` |
| [x] | `ShipmentEvent` (L4918) | `shipping.shipment.domain` | 7 | 1 | | |
| [x] | `VendorShippingConfig` (L4984) | `shipping.shipment.domain` | 8 | 2 | | Bảng `vendor_shipping_config` (số ít) |

- [x] Enum: `CarrierType` (nếu chưa tạo ở 6b), `ShipmentDirection`, `ShipmentTrackingStatus` (shipping.shipment).
- [x] 3 repository.
- [x] Migration: 2 partial unique index ghi ở L4844–4845 (`uq_shipment_outbound`, `uq_shipment_return`).
- [x] Kiểm tra cuối Phase.

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
| [x] | `vendor_profiles.category_id` (V1) | `categories` | cuối V2 |
| [x] | `voucher_usages.checkout_session_id` (V4) | `checkout_sessions` | cuối V5 |
| [x] | `voucher_usages.order_id` (V4) | `orders` | cuối V5 |
| [ ] | `payment_refunds.refund_request_id` (V6) | `refund_requests` | cuối V8 |
| [ ] | `shipment_trackings.refund_request_id` (V7) | `refund_requests` | cuối V8 |
| [ ] | *(bổ sung khi phát hiện thêm trong lúc làm)* | | |

---

## 6. NHẬT KÝ SAI LỆCH / CÂU HỎI

> Ghi mọi chỗ phải lệch khỏi ENTITY.md ngoài D1–D8, mọi mâu thuẫn tìm thấy, mọi quyết định cần người dùng chốt. Định dạng: `Phase · Entity · vấn đề · đề xuất · trạng thái`.

- P01b · `VendorProfile` · `common.domain.NameNormalizer` mới là stub rỗng, `verifyPayoutAccount` cần `normalize()` · hiện thực tối thiểu theo mô tả ENTITY.md 1.4 (bỏ dấu kể cả đ/Đ, viết hoa, gộp khoảng trắng) · đã làm
- P01c · `CustomerProfile`, `Address`, `RefreshToken`, `KycResult` · Hibernate `DuplicateMappingException` (cột FK có 2 tên logic) · thêm điều chỉnh D8, sửa 4 entity đã commit ở P01a/P01b · đã làm
- P01c · checklist · tên trường D2 ghi sai: thật là `Product.vendorProfile`, `CartItem.variant`, `Order.vendorProfile` · đã sửa bảng Phase 2/3/5 · đã làm
- P01c · `SchemaValidationIT` · viết sớm (chưa commit) để chạy ở mỗi lượt migration; commit ở P12 · JVM Windows mặc định `Asia/Saigon` PostgreSQL 16 không nhận → test đặt UTC · đã làm
- P07 · `VendorShippingConfig.supports()` · `switch` không có nhánh `JNT_EXPRESS` (enum `CarrierType` có) → Java 21 báo lỗi biên dịch · thêm `case JNT_EXPRESS -> false` (cấu hình shop chưa có cờ bật J&T); nên bổ sung vào ENTITY.md · đã làm

---

## 7. NHÁNH & COMMIT CHO TỪNG LƯỢT (TECH_STACK mục 13)

### 7.1 Quy ước

- **Mỗi lượt một nhánh mới, một commit.** Tên nhánh: `feature/entity-p<NN><lượt>-<mo-ta>` — chữ thường, gạch nối. Chưa có mã Jira nên bỏ phần `GNE-<số>` khỏi tên nhánh và commit (giống tiền lệ `plan/2026-11-11/web`, quyết định D6); có mã thì thêm ` [#GNE-<số>]` cuối commit.
- **Nhánh xếp chồng:** lượt sau phụ thuộc code lượt trước (vd. 1c cần entity của 1a, 1b) ⇒ nhánh lượt đầu tiên (P00) tách từ `develop`; **mỗi lượt sau tách từ nhánh của lượt ngay trước**. Nếu nhánh lượt trước đã được merge vào `develop` thì tách từ `develop` mới nhất (`git switch develop && git pull`).
- **Commit:** đúng `commitlint.config.cjs` — `<type>(<scope>): <subject>`, subject tiếng Anh, chữ thường, không dấu chấm cuối, dòng đầu ≤ 100 ký tự; body tiếng Việt nếu cần. Dùng thông điệp ghi sẵn ở bảng 7.3. Không `--no-verify`, không `--author`; hook commitlint / lint-staged lỗi thì sửa nguyên nhân.
- **Không có collaborator:** thông điệp commit **không** có dòng `Co-Authored-By:` (cũng không có dòng "Generated with Claude Code"). Commit chỉ mang danh tính git của chủ repo (`git config user.name` / `user.email`). Quy tắc này thắng mọi hướng dẫn tự động thêm dòng attribution. Chỉ viết `-m "<subject>"` (+ body tiếng Việt nếu cần). Trước khi commit, kiểm `git log -1 --format=%B` không chứa `Co-Authored-By`.
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
| [x] | P01a | `feature/entity-p01a-auth-entities` | `feat(auth): add user, profile, address and refresh token entities` |
| [x] | P01b | `feature/entity-p01b-vendor-admin-entities` | `feat(vendor): add vendor profile, kyc and staff permission entities` |
| [x] | P01c | `feature/entity-p01c-identity-migration` | `feat(db): add identity schema migration` |
| [x] | P02a | `feature/entity-p02a-catalog-core` | `feat(catalog): add category, brand and product entities` |
| [x] | P02b | `feature/entity-p02b-catalog-support` | `feat(catalog): add attribute, proposal, moderation and stats entities` |
| [x] | P02c | `feature/entity-p02c-catalog-migration` | `feat(db): add catalog schema migration` |
| [x] | P03 | `feature/entity-p03-cart` | `feat(order): add cart entities and migration` |
| [x] | P04a | `feature/entity-p04a-voucher` | `feat(promotion): add voucher entities and composite keys` |
| [x] | P04b | `feature/entity-p04b-flashsale-coin` | `feat(promotion): add flash sale and coin entities` |
| [x] | P04c | `feature/entity-p04c-promotion-migration` | `feat(db): add promotion schema migration` |
| [x] | P05a | `feature/entity-p05a-order-aggregate` | `feat(order): add order entity with status transition table` |
| [x] | P05b | `feature/entity-p05b-checkout-reservation` | `feat(order): add checkout, order item and stock reservation entities` |
| [x] | P05c | `feature/entity-p05c-order-migration` | `feat(db): add order schema migration` |
| [x] | P06a | `feature/entity-p06a-payment-escrow` | `feat(payment): add payment, escrow and ledger entities` |
| [x] | P06b | `feature/entity-p06b-wallet-cod` | `feat(payment): add wallet, withdrawal and cod remittance entities` |
| [x] | P06c | `feature/entity-p06c-payment-migration` | `feat(db): add payment schema migration` |
| [x] | P07 | `feature/entity-p07-shipping` | `feat(shipping): add shipment entities and migration` |
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
| 1 | 2026-11-14 | P00 | `feature/entity-p00-shared-enums` (từ `develop`) | `6397563` | 5 enum ở `common.domain`: `UserRole`, `PaymentMethod`, `PaymentStatus`, `VoucherType`, `FundingParty`; commit kèm file plan | `compile` ✔, `ModuleBoundaryTest` ✔ | Xong. Không có sai lệch so với Phụ lục A |
| 2 | 2026-11-14 | P01a | `feature/entity-p01a-auth-entities` (từ `feature/entity-p00-shared-enums`) | `2ec68f7` | Entity `User`, `CustomerProfile`, `Address`, `RefreshToken`; enum `AccountStatus`, `Gender`, `AddressLabel`; 4 repository (`auth.profile` / `auth.address` / `auth.token`) | `compile` ✔, `ModuleBoundaryTest` ✔; đếm trường 18/14/16/9, phương thức 7/2/2/2 khớp | Xong. D2: `User` bỏ `vendorProfile`, `adminProfile` (18 trường thay vì 20). `User.addresses` / `refreshTokens` trỏ sang feature khác của cùng module `auth` nên giữ |
| 3 | 2026-11-14 | P01b | `feature/entity-p01b-vendor-admin-entities` (từ `feature/entity-p01a-auth-entities`) | `b7f3611` | Entity `VendorProfile`, `KycResult`, `AdminProfile`, `StaffPermission`; enum `BusinessType`, `KycStatus`, `KycVerificationMethod`, `KycOverallResult`, `StaffPermissionEnum`, `StaffTitle`; 4 repository; hiện thực `common.domain.NameNormalizer` (đang là stub) | `compile` ✔, `ModuleBoundaryTest` ✔; trường 50/29/8/6, PT 15/3/2/2 khớp | Xong. D2: `VendorProfile`, `AdminProfile` bỏ `user` (50, 8 trường). `NameNormalizer.normalize` viết theo mô tả ENTITY.md 1.4 (bỏ dấu, viết hoa, gộp khoảng trắng) — ghi mục 6. Từ lượt này code chép từ ENTITY.md bằng script trích xuất (giữ nguyên văn khối code) để tránh sót |
| 4 | 2026-11-14 | P01c | `feature/entity-p01c-identity-migration` (từ `feature/entity-p01b-vendor-admin-entities`) | `42e10b1` | `V1__identity.sql`: 8 bảng, CHECK enum, unique 1-1, 8 FK (`ON DELETE` theo B.1); D8 cho `CustomerProfile`, `Address`, `RefreshToken`, `KycResult` | `compile` ✔, `ModuleBoundaryTest` ✔, V1 chạy trên `postgres:16` ✔, `SchemaValidationIT` (Flyway + Hibernate validate) ✔ | Xong. Phát hiện & thêm D8 (xem mục 2, 6). FK nợ `vendor_profiles.category_id` để V2. `staff_permissions.granted_by_admin_id` FK không cascade |
| 5 | 2026-11-14 | P02a | `feature/entity-p02a-catalog-core` (từ `feature/entity-p01c-identity-migration`) | `a69b974` | Entity `Category`, `Brand`, `Product`, `ProductVariant`, `ProductImage`, `ProductDocument`; enum `ProductStatus`, `ShippingPayer`, `ProductCondition`, `ProductDocumentType`; 6 repository | `compile` ✔, `ModuleBoundaryTest` ✔; trường 11/7/36/12/12/10, PT 2/2/6/5/1/1 khớp | Xong. D2: `Product` bỏ `vendorProfile`. D8 áp cho `Product.categoryId`, `ProductVariant`/`ProductImage`/`ProductDocument.productId`, `Category.parentId` |
| 6 | 2026-11-14 | P02b | `feature/entity-p02b-catalog-support` (từ `feature/entity-p02a-catalog-core`) | `a20cb56` | Entity `CategoryAttribute`, `UserWishlist`, `CategoryProposal`, `ProductModerationResult`, `SearchKeywordStat`, `ProductViewStat`; enum `AttributeDataType`, `CategoryProposalStatus`, `ModerationDecision`, `TrafficSource`; 6 repository (đủ 12 repository của Phase 2) | `compile` ✔, `ModuleBoundaryTest` ✔; trường 15/4/11/11/6/8, PT 2/0/2/2/1/1 khớp | Xong. Không có quan hệ chéo module trong lượt này |
| 7 | 2026-11-14 | P02c | `feature/entity-p02c-catalog-migration` (từ `feature/entity-p02b-catalog-support`) | `feaa6dc` | `V2__catalog.sql`: 12 bảng, CHECK enum, unique/index từ `@Table`, 15 FK + FK tự tham chiếu `categories.parent_id`; trả FK nợ `vendor_profiles.category_id` (SET NULL); hàm `immutable_unaccent`, cột sinh `products.search_vector` + 5 index mục 13.1 | V1+V2 chạy trên `postgres:16` ✔, `SchemaValidationIT` ✔ (24 entity), `ModuleBoundaryTest` ✔ | Xong. `@UniqueConstraint(columnNames = {"userId", "productId"})` của `UserWishlist` dùng tên Java → SQL dùng `user_id, product_id`. Full-text dùng cấu hình `simple` + unaccent (PostgreSQL không có cấu hình tiếng Việt) |
| 8 | 2026-11-14 | P03 | `feature/entity-p03-cart` (từ `feature/entity-p02c-catalog-migration`) | `c0c1f13` | Thay stub `Cart`, `CartItem`, `CartRepository`; thêm `CartItemRepository`; `V3__cart.sql` (2 bảng, 3 FK: `carts.user_id`, `cart_items.cart_id` CASCADE, `cart_items.product_variant_id`) | `compile` ✔, `ModuleBoundaryTest` ✔, V1–V3 trên `postgres:16` ✔ (22 bảng), `SchemaValidationIT` ✔; trường 4/6, PT 4/2 khớp | Xong. D2: `CartItem` bỏ `variant` (6 trường thay vì 7) |
| 9 | 2026-11-14 | P04a | `feature/entity-p04a-voucher` (từ `feature/entity-p03-cart`) | `9734a0d` | Entity `Voucher`, `VoucherUsage`, `UserVoucher`, 4 bảng nối phạm vi; 4 class khóa gộp `*Id`; enum `VoucherScope`, `DiscountType`; 7 repository (bảng nối dùng kiểu id gộp) | `compile` ✔, `ModuleBoundaryTest` ✔; trường 27/7/6/3/3/3/3, PT 5/1/1/1/1/1/1 khớp | Xong. D2: `UserVoucher` bỏ `user`; 4 bảng nối bỏ `category` / `product` |
| 10 | 2026-11-14 | P04b | `feature/entity-p04b-flashsale-coin` (từ `feature/entity-p04a-voucher`) | `1949682` | Entity `FlashSaleCampaign`, `FlashSaleItem`, `UserCoin`, `CoinTransaction`, `DailyCheckin`; enum `FlashSaleStatus`, `FlashSaleItemStatus`, `CoinTransactionType`; 5 repository (đủ 12 repository của Phase 4) | `compile` ✔, `ModuleBoundaryTest` ✔; trường 9/9/5/8/4, PT 4/5/3/2/1 khớp | Xong. `UserCoin.verifyHash` cần import `MessageDigest`, `StandardCharsets` (ENTITY.md lược import) |
| 11 | 2026-11-14 | P04c | `feature/entity-p04c-promotion-migration` (từ `feature/entity-p04b-flashsale-coin`) | `8c924bd` | `V4__promotion.sql`: 12 bảng (4 bảng nối PK gộp, `user_coins` PK = `user_id`), CHECK enum, UNIQUE `(voucher_id, checkout_session_id)`, `(user_id, voucher_id)`, 19 FK (bảng nối CASCADE theo voucher) | V1–V4 trên `postgres:16` ✔ (34 bảng), `SchemaValidationIT` ✔, `ModuleBoundaryTest` ✔ | Xong. 2 FK nợ `voucher_usages.checkout_session_id`, `voucher_usages.order_id` để V5 |
| 12 | 2026-11-14 | P05a | `feature/entity-p05a-order-aggregate` (từ `feature/entity-p04c-promotion-migration`) | `563b3b9` | Thay stub `Order`, `OrderStatus`, `OrderRepository`; thêm enum `FundingStatus` | `compile` ✔, `ModuleBoundaryTest` ✔; 33 trường, 22 PT (kể cả `transitionTo`, `isCod` private), hằng `ALLOWED` 14/14 entry khớp ENTITY.md | Xong. D2: bỏ `vendorProfile` (→ `vendor`). `paymentMethod` / `paymentStatus` dùng enum ở `common.domain` (D3) |
| 13 | 2026-11-14 | P05b | `feature/entity-p05b-checkout-reservation` (từ `feature/entity-p05a-order-aggregate`) | `edb5958` | Thay stub `CheckoutSession`, `SessionStatus`, `CheckoutSessionRepository`, `OrderItem`, `OrderDiscount`, `StockReservation`, `StockReservationRepository`; thêm enum `StockReservationStatus`, `OrderItemRepository`, `OrderDiscountRepository` | `compile` ✔, `ModuleBoundaryTest` ✔; trường 9/16/7/9, PT 4/2/1/3 khớp | Xong. D2: `OrderItem` bỏ `productVariant`. `CheckoutSession.orders` giữ (cùng module `order`) |
| 14 | 2026-11-14 | P05c | `feature/entity-p05c-order-migration` (từ `feature/entity-p05b-checkout-reservation`) | `4b21972` | `V5__order.sql`: 5 bảng, CHECK enum, 10 FK (`order_items`/`order_discounts` CASCADE theo đơn, `order_discounts.voucher_id` SET NULL), 4 index mục 13.1 (2 partial); trả 2 FK nợ của `voucher_usages` | V1–V5 trên `postgres:16` ✔ (39 bảng), `SchemaValidationIT` ✔, `ModuleBoundaryTest` ✔ | Xong |
| 15 | 2026-11-14 | P06a | `feature/entity-p06a-payment-escrow` (từ `feature/entity-p05c-order-migration`) | `a489e42` | Entity `PaymentTransaction`, `PaymentRefund`, `EscrowLedger`, `FinancialLedger`; enum `PaymentRefundStatus`, `FundingSource`, `EscrowStatus`, `FinancialEventType`; 4 repository | `compile` ✔, `ModuleBoundaryTest` ✔; trường 12/6/18/10, PT 4/2/11/1 khớp (kể cả `totalFunded()` private) | Xong. `EscrowLedger` dùng `@ColumnDefault` của Hibernate (ENTITY.md lược import) |
| 16 | 2026-11-14 | P06b | `feature/entity-p06b-wallet-cod` (từ `feature/entity-p06a-payment-escrow`) | `ac3ef65` | Entity `VendorWallet`, `WalletTransaction`, `WithdrawalRequest` (payment.wallet), `CodRemittance` (shipping.cod); enum `WalletTransactionType`, `WalletReferenceType`, `WithdrawalStatus`, `CodRemittanceStatus`, `CarrierType` (tạo sớm ở shipping.shipment); 4 repository (đủ 8 của Phase 6) | `compile` ✔, `ModuleBoundaryTest` ✔; trường 8/10/12/12, PT 6/3/4/1 khớp | Xong. `WithdrawalRequest` đặt ở `payment.wallet` theo checklist; `CarrierType` tạo ở lượt này vì `CodRemittance` dùng |
| 17 | 2026-11-14 | P06c | `feature/entity-p06c-payment-migration` (từ `feature/entity-p06b-wallet-cod`) | `608dd32` | `V6__payment.sql`: 8 bảng, CHECK enum, CHECK XOR `order_id`/`checkout_session_id`, UNIQUE `gateway_transaction_id`, partial index escrow `HELD`, 10 FK; UNIQUE từ entity (`escrow_ledgers.order_id`, `vendor_wallets.vendor_id`, `cod_remittances.bank_reference`…) | V1–V6 trên `postgres:16` ✔ (47 bảng), `SchemaValidationIT` ✔, `ModuleBoundaryTest` ✔ | Xong. FK mềm không tạo constraint: `withdrawal_requests.wallet_id`, `processed_by_admin_id`, `cod_remittances.reconciled_by_staff_id`. FK nợ `payment_refunds.refund_request_id` để V8 |
| 18 | 2026-11-14 | P07 | `feature/entity-p07-shipping` (từ `feature/entity-p06c-payment-migration`) | (lượt sau điền) | Entity `ShipmentTracking`, `ShipmentEvent`, `VendorShippingConfig`; enum `ShipmentDirection`, `ShipmentTrackingStatus`; 3 repository; `V7__shipping.sql` (3 bảng, 3 FK, 2 partial unique index `uq_shipment_outbound` / `uq_shipment_return`) | `compile` ✔, `ModuleBoundaryTest` ✔, V1–V7 trên `postgres:16` ✔ (50 bảng), `SchemaValidationIT` ✔; trường 11/7/8, PT 3/1/2 khớp | Xong. Sửa lệch ENTITY.md: `supports()` thiếu nhánh `JNT_EXPRESS` (mục 6). FK nợ `shipment_trackings.refund_request_id` để V8 |
