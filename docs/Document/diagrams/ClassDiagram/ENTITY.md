# ENTITY DOCUMENTATION — Multi-Vendor E-commerce Platform (Genie) — BẢN TLCN
## PHẠM VI TIỂU LUẬN CHUYÊN NGÀNH (TLCN) — PHỦ TOÀN BỘ GIAO DIỆN CLIENT + PROTOTYPE, MỞ RỘNG ĐƯỢC LÊN DATN

> Tài liệu này là **Nguồn Sự Thật Duy Nhất (SSOT)** cho Cơ sở dữ liệu và Mô hình Thực thể của **bản MVP** (thư mục `TLCN/`).
> Bản hoàn chỉnh cho đồ án tốt nghiệp (DATN — 162 + 7 entity) ở `DATN/diagrams/ClassDiagram/ENTITY.md`; mọi tên entity / bảng / cột / enum của TLCN có mặt ở DATN.
> - **Khung Ứng dụng & ORM:** Java 21 LTS & Spring Boot 3.3.x, Jakarta Persistence (Hibernate 6), Spring Data JPA.
> - **CSDL Quan hệ:** PostgreSQL 16 (**65 bảng vật lý ↔ 65 class `@Entity`**, 5 bảng nối dùng khóa gộp `@IdClass`), migration bằng Flyway.
> - **Lưu trữ phụ trợ (chỉ 2 thành phần):**
>   - **Redis 7:** OTP, giới hạn tần suất đơn giản, cache số liệu sản phẩm (`sold`, `rating`), khóa ngắn hạn khi giữ chỗ tồn kho.
>   - **Silo (S3-compatible):** ảnh sản phẩm, ảnh đại diện, hồ sơ KYC (bucket riêng tư), ảnh/video bằng chứng trả hàng — qua Presigned URL.
> - **Không dùng trong MVP:** Kafka, RabbitMQ, Elasticsearch, Qdrant, Kubernetes. Sự kiện nội bộ dùng Spring `ApplicationEvent` + `@Async`; tìm kiếm dùng full-text của PostgreSQL.
> - **Quy mô mục tiêu:** demo đồ án và vận hành thử nghiệm (vài trăm người dùng, ~50 CCU) trên 1 máy chủ Docker Compose.

> 🔁 **Quy tắc tương thích để mở rộng:** tên entity, tên bảng, tên cột và tên enum của bản MVP **trùng 100%** với bản đầy đủ. Các cột / bảng / giá trị enum bị cắt
> được liệt kê trong `TLCN/BAO_CAO_PHAM_VI_MVP.md`. Khi mở rộng chỉ cần **thêm** bảng / cột / giá trị enum bằng migration Flyway mới — không phải đổi tên hay chuyển dữ liệu.

> 📝 **TLCN v1.4 (2026-11-16) — đồng bộ với mã nguồn `genie-multivendor-platform`:** 65 entity + `V1…V12` đã sinh 1-1 từ tài liệu này; AUTH bổ sung bằng `V14__auth_additions.sql` (không đổi tên bảng / cột / enum cũ): `User.deletionRequestedAt`, `RefreshToken.familyId` + `lastUsedAt`, 8 giá trị `AuditAction`, 5 index (mục 13.1), `User.anonymize()` xóa cả `oauthProvider`, thêm phương thức nghiệp vụ AUTH (mục 1.1, 1.7). Kho tệp **MinIO → Silo** (fork cộng đồng của MinIO, cùng API S3 — ADR 0009 trong repo).

> 📝 **TLCN v1.3 (2026-11-08) — đồng bộ với `document/API_SPEC.md`:** thêm `Address.label`, `VendorProfile.isFavorite`, `Category.description`, `FlashSaleItem.status`, `ProductReview.isAnonymous`, `PlatformNotification.sendAt` + `recipientUserIdsJson`, `Message.orderId`; thêm entity `CodRemittance` (bảng `cod_remittances`, mục 6.8) và 3 enum `AddressLabel`, `FlashSaleItemStatus`, `CodRemittanceStatus`. Tổng: 65 entity, 55 enum, 101 quan hệ, 202 phương thức, 744 thuộc tính.

---

## MỤC LỤC TỔNG THỂ (12 NHÓM THỰC THỂ)

> 📌 **12 nhóm ≠ 10 sơ đồ:** 12 nhóm dưới đây là cách chia **chương** của tài liệu; khi vẽ Class Diagram, các nhóm được gom thành **10 sơ đồ** — bảng ánh xạ ở Phụ lục C.0.1.

### 📌 PHẦN I: CÁC THỰC THỂ NGHIỆP VỤ TLCN (NHÓM 01 ĐẾN 12 — 65 ENTITY)
| # | Nhóm | Các Entity |
|---|------|-----------|
| 1 | [Người dùng & định danh](#1-nhóm-người-dùng--định-danh) | User, CustomerProfile, Address, VendorProfile, AdminProfile, KycResult, RefreshToken, StaffPermission |
| 2 | [Danh mục & sản phẩm](#2-nhóm-danh-mục--sản-phẩm) | Category, Product, ProductVariant, ProductImage, UserWishlist, CategoryAttribute, Brand, ProductDocument, CategoryProposal, ProductModerationResult, SearchKeywordStat, ProductViewStat |
| 3 | [Giỏ hàng](#3-nhóm-giỏ-hàng) | Cart, CartItem |
| 4 | [Khuyến mãi, voucher, flash sale & điểm](#4-nhóm-khuyến-mãi-voucher-flash-sale--điểm) | Voucher, VoucherUsage, UserVoucher, VoucherCategory, VoucherProduct, VoucherExcludedCategory, VoucherExcludedProduct, FlashSaleCampaign, FlashSaleItem, UserCoin, CoinTransaction, DailyCheckin |
| 5 | [Đơn hàng](#5-nhóm-đơn-hàng) | CheckoutSession, Order, OrderItem, OrderDiscount, StockReservation |
| 6 | [Thanh toán & ký quỹ](#6-nhóm-thanh-toán--ký-quỹ) | PaymentTransaction, PaymentRefund, EscrowLedger, VendorWallet, WalletTransaction, WithdrawalRequest, FinancialLedger, CodRemittance |
| 7 | [Vận chuyển](#7-nhóm-vận-chuyển) | ShipmentTracking, ShipmentEvent, VendorShippingConfig |
| 8 | [Hoàn tiền / khiếu nại](#8-nhóm-hoàn-tiền--khiếu-nại) | RefundRequest, RefundRequestItem, RefundEvidence |
| 9 | [Đánh giá & tương tác shop](#9-nhóm-đánh-giá--tương-tác-shop) | ProductReview, ShopFollow, ReviewReport, ReviewHelpfulVote |
| 10 | [Thông báo & nội dung sàn](#10-nhóm-thông-báo--nội-dung-sàn) | Notification, PlatformNotification, UserPlatformNotificationRead, HomeBanner, AdminNotification |
| 11 | [Nhắn tin (chat)](#11-nhóm-nhắn-tin-chat) | Conversation, Message |
| 12 | [Nhật ký hệ thống](#12-nhóm-nhật-ký-hệ-thống) | SystemLog |

### 🛠️ PHẦN II: TIÊU CHUẨN KỸ THUẬT & PHỤ LỤC
| # | Mục | Nội dung |
|---|-----|----------|
| 13 | [Tiêu chuẩn Vật lý PostgreSQL 16 (mức MVP)](#13-tiêu-chuẩn-vật-lý-postgresql-16-mức-mvp) | Index cho đường dẫn nóng, quy ước migration, cấu hình tối thiểu |
| — | [Phụ lục A: Danh mục Enum](#phụ-lục-a--tổng-hợp-enum-bản-tlcn) | 55 enum dùng trong TLCN |
| — | [Phụ lục B: Bảng quan hệ Khóa ngoại](#phụ-lục-b--bảng-quan-hệ-để-vẽ-class-diagram) | 101 quan hệ, phân loại Composition / Aggregation / Association |
| — | [Phụ lục C: Đặc tả 10 Sơ đồ Class Diagram](#phụ-lục-c--đặc-tả-10-sơ-đồ-class-diagram-bản-tlcn) | Entity, enum, phương thức, quan hệ của từng sơ đồ |
| — | [Phụ lục D: Đối chiếu TLCN ↔ DATN](#phụ-lục-d--đối-chiếu-tlcn--datn-bản-hoàn-chỉnh--lộ-trình-mở-rộng-mô-hình) | Ánh xạ mục TLCN ↔ DATN và thứ tự mở rộng |
| — | [Phụ lục E: Phương thức nghiệp vụ (Operations)](#phụ-lục-e--danh-mục-phương-thức-nghiệp-vụ-ngăn-operations-của-class-diagram) | 202 phương thức nghiệp vụ của 64 class |

> **TỔNG CỘNG:** **65 Entity / Bảng vật lý** ↔ **65 Class Java `@Entity`** (+ 5 class khóa gộp `@IdClass`), **55 Enum**, **101 quan hệ**, **10 sơ đồ lớp**.

---

## QUY ƯỚC CHUNG TOÀN DỰ ÁN (POSTGRESQL 16 & JAVA 21)

Trước khi đọc từng class, cần nắm 12 quy ước chuẩn Enterprise áp dụng xuyên suốt toàn bộ hệ thống:

### 1. `Long id` — Khóa chính tự tăng (PostgreSQL `BIGSERIAL`)
Trong Java sử dụng `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`. Trong PostgreSQL 16, cột khóa chính được khai báo kiểu `BIGINT GENERATED ALWAYS AS IDENTITY` (hoặc `BIGSERIAL`), lưu trữ số nguyên 64-bit từ 1 đến 9,223,372,036,854,775,807, bảo đảm không bao giờ cạn kiệt ID.

### 2. `Long version` — Optimistic Locking (Khóa lạc quan)
Các bảng có tần suất cập nhật cao (`User`, `Product`, `ProductVariant`, `Voucher`, `Order`, `StockReservation`, `PaymentTransaction`, `VendorWallet`) đều có cột `version`.
> **Cơ chế:** Khi 2 luồng cùng đọc và cập nhật 1 bản ghi, câu lệnh `UPDATE` kèm điều kiện `WHERE id = ? AND version = ?`. Luồng hoàn tất trước sẽ tăng `version` lên 1; luồng sau cập nhật 0 dòng sẽ phát sinh `OptimisticLockException` và kích hoạt Retry.
> **Ý nghĩa sống còn:** Ngăn chặn tuyệt đối **overselling** (bán âm hàng khi nhiều người cùng đặt) và **double-spend** (rút tiền ví shop 2 lần).

### 3. `BigDecimal` cho Tiền tệ — **TUYỆT ĐỐI KHÔNG** dùng `float` / `double`
Java dùng `java.math.BigDecimal`, PostgreSQL dùng `DECIMAL(19,2)`.
> **Lý do:** Số dấu chấm động nhị phân gây sai số làm tròn tích lũy khi có hàng triệu giao dịch, dẫn đến lệch sổ cái kế toán. `DECIMAL(19,2)` hỗ trợ giá trị tiền tệ lên tới hàng nghìn tỷ VNĐ với 2 chữ số thập phân chính xác tuyệt đối. Tỷ lệ phần trăm hoa hồng và chiết khấu dùng `DECIMAL(5,4)` (ví dụ: `0.0500` = 5.0%).

### 4. `Snapshot Pattern` — Ảnh chụp dữ liệu bất biến
Các thực thể giao dịch (`Order`, `OrderItem`, `OrderDiscount`) sao chép toàn bộ thông tin tại thời điểm đặt hàng: `productNameSnapshot`, `skuSnapshot`, `shippingAddressSnapshotJson`, `priceSnapshot`, `voucherCodeSnapshot`.
> **Nguyên tắc:** Dữ liệu hóa đơn và chứng từ kế toán phải bất biến theo thời gian. Khi Nhà bán hàng đổi tên sản phẩm hoặc tăng giá niêm yết trong tương lai, toàn bộ đơn hàng trong quá khứ không bị thay đổi, bảo đảm giá trị pháp lý khi thanh tra thuế và đối soát.

### 5. Kiểu Nullable & Wrapper Classes
Trong Java 21, toàn bộ các trường không bắt buộc (Nullable) sử dụng Wrapper Classes (`String`, `Integer`, `Long`, `Boolean`, `BigDecimal`, `OffsetDateTime`). Tuyệt đối không dùng kiểu nguyên thủy (`int`, `boolean`) cho các cột có thể mang giá trị `NULL`. Các trường này được đánh dấu `// NULLABLE`.

### 6. Enum lưu dạng Chuỗi String (`EnumType.STRING`)
Trong Java sử dụng `@Enumerated(EnumType.STRING)`. Tuyệt đối không dùng `EnumType.ORDINAL` (vì ORDINAL lưu vị trí chỉ số 0, 1, 2; khi chèn thêm giá trị enum mới vào giữa thì toàn bộ dữ liệu cũ trong database sẽ bị sai lệch toàn bộ).

### 7. Dùng `TIMESTAMPTZ` cho thời gian — Triệt tiêu lỗi múi giờ và Year 2038
Toàn bộ các cột mốc thời gian (`createdAt`, `updatedAt`, `expiresAt`, `startsAt`, `endsAt`) đều sử dụng kiểu **`TIMESTAMPTZ` (Timestamp with time zone)** trong PostgreSQL 16 và map với **`OffsetDateTime`** (hoặc `Instant`) trong Java.
> **Ưu điểm vượt trội:** PostgreSQL `TIMESTAMPTZ` lưu trữ mốc thời gian UTC với độ chính xác micro-giây, hỗ trợ mốc thời gian tới năm 294,276 sau Công nguyên, triệt tiêu hoàn toàn rủi ro **Year 2038 Problem** của MySQL 32-bit `TIMESTAMP` và không bị ảnh hưởng bởi múi giờ máy chủ ứng dụng.

### 8. Kiểu dữ liệu `JSONB` cho Cấu hình Động và Dữ liệu Bán cấu trúc
Toàn bộ các trường hậu tố `*Json` (như `specsJson`, `attributesJson`, `shippingAddressSnapshotJson`, `cartSnapshotJson`, `metadataJson`) sử dụng kiểu **`JSONB`** của PostgreSQL 16 kết hợp `@JdbcTypeCode(SqlTypes.JSON)` + `@Column(columnDefinition = "jsonb")`.
> **Lợi thế:** JSONB lưu trữ dưới dạng nhị phân đã phân tích cú pháp, cho phép đánh chỉ mục **GIN Index** (`CREATE INDEX ... USING GIN (payload_json)`), giúp truy vấn các khóa lồng nhau với tốc độ O(log N) tương đương bảng quan hệ.

### 9. Chiến lược Khóa ngoại & Ràng buộc `ON DELETE`
- **`ON DELETE CASCADE` (Composition / Sở hữu chặt):** Áp dụng cho các thực thể phụ thuộc hoàn toàn vào cha (`OrderItem` theo `Order`, `ProductVariant` theo `Product`, `CartItem` theo `Cart`).
- **`ON DELETE RESTRICT` (Kế toán & Pháp lý):** Cấm xóa vật lý đối với các thực thể liên quan đến tiền bạc và danh tính (`users`, `vendor_profiles`, `products` đã phát sinh đơn hàng). Áp dụng **Soft Delete** (`isDeleted`, `status = DELETED`) để bảo vệ toàn vẹn lịch sử đối soát thuế và kiện tụng.
- **`ON DELETE SET NULL` (Tham chiếu lỏng):** Áp dụng khi xóa dữ liệu phụ trợ không bắt buộc (ví dụ xóa một địa chỉ giao hàng thì `CustomerProfile.defaultAddressId` chuyển thành `NULL`).

### 10. Phân định Ranh giới Lưu trữ (MVP — chỉ 3 thành phần)
- **PostgreSQL 16 (ACID & Storage of Truth):** 100% dữ liệu nghiệp vụ bền vững: người dùng, sản phẩm, tồn kho, giỏ hàng, đơn hàng, ký quỹ Escrow, ví shop, khiếu nại, chat, thông báo. Tìm kiếm sản phẩm dùng **full-text search** (`tsvector` + extension `unaccent` cho tiếng Việt không dấu) và `pg_trgm` cho gõ sai nhẹ.
- **Redis 7:** dữ liệu tạm thời có TTL: OTP đăng ký / quên mật khẩu, dữ liệu đăng ký tạm, rate limit đơn giản (đếm theo key), cache số liệu `product:stats:{id}`, danh sách token bị thu hồi.
- **Silo (S3-compatible):** ảnh sản phẩm, ảnh đại diện, hồ sơ KYC (bucket riêng tư `kyc-dossiers`), bằng chứng trả hàng — client tải trực tiếp qua Presigned URL.
- **Ngoài MVP (bản đầy đủ):** Kafka, RabbitMQ, Elasticsearch, Qdrant được thêm khi lưu lượng tăng — mô hình dữ liệu PostgreSQL không đổi.

### 11. Lombok, Getter/Setter & Phương thức nghiệp vụ (Rich Domain Model)
- Mọi class `@Entity` dùng `@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor` (Lombok). **Không** dùng `@Data`/`@EqualsAndHashCode` mặc định trên entity (gây vòng lặp lazy-loading và sai `hashCode` khi `id` chưa sinh) — so sánh bằng `id`.
- Getter/Setter là phương thức kỹ thuật, **không vẽ** trên Class Diagram. Ngăn *Operations* chỉ vẽ **phương thức nghiệp vụ** (chuyển trạng thái, tính toán, kiểm tra bất biến, factory) được khai báo cuối mỗi class, liệt kê đầy đủ ở **Phụ lục F**.
- Nguyên tắc: phương thức nghiệp vụ chỉ thay đổi **chính entity đó** và ném `IllegalStateException` khi vi phạm máy trạng thái; việc đọc/ghi entity khác, khóa bi quan (`SELECT … FOR UPDATE`), sổ cái, cache, sự kiện Outbox do **Service** điều phối trong cùng transaction (ví dụ `implement.md` Phần 2.8, 5.18, 10.11).
- Tiện ích dùng chung trong thân phương thức: `Haversine.km(...)`, `MurmurHash3.hash32(...)` (gói `com.genie.common`), `AntPathMatcher` (Spring); mã lỗi theo danh mục `GNE-*` (Document_Vi.md mục 3.7.1).

### 12. Giá trị mặc định (`@Builder.Default`) & Mốc thời gian kiểm toán (`@CreationTimestamp` / `@UpdateTimestamp`)
- **Vấn đề:** `@Builder` của Lombok **bỏ qua** giá trị khởi tạo của field nếu không có `@Builder.Default` → `X.builder()...build()` để `Boolean`/`Integer`/`BigDecimal` là `null` → vi phạm `NOT NULL` khi INSERT hoặc `NullPointerException` trong phương thức nghiệp vụ (vd: `FlashSaleItem.recordSale()`, `FlashSaleCampaign.schedule()`).
- **Quy tắc:** mọi field `NOT NULL` có giá trị khởi tạo tự nhiên (cờ `Boolean`, bộ đếm `Integer`/`Long` = 0, số tiền tích lũy `BigDecimal.ZERO`) khai báo `@Builder.Default` kèm giá trị, ví dụ `@Builder.Default private Integer soldCount = 0;`. Lombok 1.18.34 (đi kèm Spring Boot 3.3.4) áp dụng cùng giá trị này cho constructor không tham số, nên factory `new X()` trong thân phương thức cũng nhận đúng mặc định. Các cột có `DEFAULT` ở DDL giữ `@Builder.Default` cùng giá trị để Java và PostgreSQL không lệch nhau.
- **Mốc thời gian:** mọi `createdAt` khai báo `@CreationTimestamp @Column(nullable = false, updatable = false)`; mọi `updatedAt` khai báo `@UpdateTimestamp` — Hibernate tự gán khi INSERT và mỗi lần UPDATE, nên phương thức chuyển trạng thái **không** phải tự gán `updatedAt` và không thể quên. Việc gán `createdAt = now` trong factory chỉ phục vụ unit test; khi lưu, Hibernate ghi đè bằng giờ máy chủ.

---

---

# 1. NHÓM NGƯỜI DÙNG & ĐỊNH DANH

## 1.1. `User` — Tài khoản đăng nhập
Bảng: `users`

```java
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_oauth", columnNames = {"oauth_provider", "oauth_provider_id"}))
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính, ID định danh duy nhất của tài khoản

    @Column(unique = true, nullable = false, length = 255)
    private String email;                     // Email đăng nhập — DUY NHẤT toàn hệ thống, dùng làm username

    @Column(length = 255)
    private String password;                  // NULLABLE — Mật khẩu đã băm (Argon2id chuẩn OWASP 2024); NULL khi tài khoản tạo bằng Google / Apple và chưa "Thiết lập mật khẩu" (UC-AUTH-03, UC-AUTH-06)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;                    // 4 vai trò MVP: CUSTOMER, VENDOR, STAFF, ADMIN (Phụ lục A — nhóm 1). Spring Security cấp authority dạng ROLE_<giá trị>; RoleHierarchy: ROLE_VENDOR > ROLE_CUSTOMER, ROLE_ADMIN > ROLE_STAFF — chủ shop dùng cùng tài khoản để mua hàng ở shop khác

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;             // Trạng thái tài khoản: UNVERIFIED (chỉ dùng cho nhân viên được mời chưa kích hoạt), ACTIVE, LOCKED (để dành — TLCN không dùng: khóa tạm chỉ đặt lockedUntil, status vẫn ACTIVE), SUSPENDED, DELETED, ANONYMIZED

    private OffsetDateTime emailVerifiedAt;    // NULLABLE — Thời điểm user bấm link xác thực email. NULL = chưa xác thực

    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer failedLoginAttempts = 0;  // Đếm số lần nhập sai mật khẩu liên tiếp. Vượt ngưỡng → tự khoá

    private OffsetDateTime lockedUntil;        // NULLABLE — Khoá tạm thời (do sai pass) đến thời điểm này

    private OffsetDateTime suspendedUntil;     // NULLABLE — Bị admin đình chỉ đến thời điểm này (vi phạm chính sách)

    @Column(length = 500)
    private String suspensionReason;          // NULLABLE — Lý do bị đình chỉ, hiển thị cho user biết

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống 2 request cùng sửa 1 tài khoản

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo tài khoản

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    // ---- Đăng nhập mạng xã hội (UC-AUTH-03) ----
    @Column(length = 30)
    private String oauthProvider;             // NULLABLE — Nhà cung cấp OAuth: "google", "apple"

    @Column(length = 255)
    private String oauthProviderId;           // NULLABLE — ID của user bên phía nhà cung cấp OAuth (claim `sub`, cố định kể cả khi đổi email bên Google); UNIQUE cùng oauthProvider — một tài khoản Google / Apple chỉ gắn với một user

    // ---- Xóa tài khoản (UC-AUTH-10) — bổ sung bằng V14__auth_additions.sql (plan AUTH D7) ----
    private OffsetDateTime deletionRequestedAt; // NULLABLE — Thời điểm khách yêu cầu xóa tài khoản; NULL = không có yêu cầu. Cron ẩn danh khi quá 30 ngày; index một phần idx_users_deletion_requested_at

    // ===== QUAN HỆ =====
    @OneToOne(mappedBy = "user")
    private CustomerProfile customerProfile;  // NULLABLE (phía nghịch của OneToOne) — Hồ sơ khách hàng (chỉ có khi role = CUSTOMER)

    @OneToOne(mappedBy = "user")
    private VendorProfile vendorProfile;      // NULLABLE (phía nghịch của OneToOne) — Hồ sơ người bán (chỉ có khi role = VENDOR)

    @OneToOne(mappedBy = "user")
    private AdminProfile adminProfile;        // NULLABLE (phía nghịch của OneToOne) — Hồ sơ admin/staff (chỉ có khi role = ADMIN hoặc STAFF)

    @OneToMany
    @JoinColumn(name = "user_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua Address.userId
    private List<Address> addresses;          // Danh sách địa chỉ giao hàng của user

    @OneToMany
    @JoinColumn(name = "user_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua RefreshToken.userId
    private List<RefreshToken> refreshTokens; // Danh sách phiên đăng nhập đang hoạt động
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kiểm tra tài khoản đang bị khóa tạm do sai mật khẩu — UC-AUTH-02 */
    public boolean isLocked(OffsetDateTime now) {
        return this.lockedUntil != null && this.lockedUntil.isAfter(now);
    }

    /** Tăng đếm sai mật khẩu, đủ 5 lần thì khóa 15 phút — UC-AUTH-02 */
    public void recordFailedLogin(OffsetDateTime now) {
        this.failedLoginAttempts = this.failedLoginAttempts + 1;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = now.plusMinutes(15);   // khóa 15 phút
            this.failedLoginAttempts = 0;
        }
    }

    /** Reset bộ đếm khi đăng nhập đúng — UC-AUTH-02 */
    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /** Đình chỉ tài khoản vi phạm (chỉ Admin) — UC-ADM-03 */
    public void suspend(String reason, OffsetDateTime until) {
        if (this.status == AccountStatus.DELETED) throw new IllegalStateException("Tài khoản đã xóa");
        this.status = AccountStatus.SUSPENDED;
        this.suspensionReason = reason;
        this.suspendedUntil = until;   // NULL = đình chỉ vô thời hạn
    }

    /** Cron mở lại tài khoản khi hết thời hạn đình chỉ — UC-ADM-03 */
    public boolean reactivateIfSuspensionExpired(OffsetDateTime now) {
        if (this.status != AccountStatus.SUSPENDED || this.suspendedUntil == null || this.suspendedUntil.isAfter(now)) return false;
        this.status = AccountStatus.ACTIVE;
        this.suspensionReason = null;
        this.suspendedUntil = null;
        return true;
    }

    /** Admin mở khóa thủ công — UC-ADM-03 */
    public void reactivate() {
        if (this.status != AccountStatus.SUSPENDED && this.status != AccountStatus.LOCKED) throw new IllegalStateException("Tài khoản không bị khóa");
        this.status = AccountStatus.ACTIVE;
        this.suspensionReason = null;
        this.suspendedUntil = null;
        this.lockedUntil = null;
    }

    /** Ẩn danh hóa theo yêu cầu xóa tài khoản (sau thời gian chờ 30 ngày) — UC-AUTH-10. Xóa cả oauthProvider (plan AUTH D19) */
    public void anonymize() {
        this.status = AccountStatus.ANONYMIZED;
        this.email = "deleted_user_" + this.id + "@anonymized.local";
        this.password = null;
        this.oauthProvider = null;
        this.oauthProviderId = null;
        this.deletionRequestedAt = null;
    }

    // ---- Phương thức bổ sung khi hiện thực AUTH (plan A03.01, 2026-11-14) ----

    /** Tài khoản đã có mật khẩu chưa (false = chỉ đăng nhập Google / Apple) — UC-AUTH-02/03/06 */
    public boolean hasPassword() { return this.password != null; }

    /** Đặt / đổi mật khẩu (bản băm Argon2id) — UC-AUTH-04/06 */
    public void changePassword(String passwordHash) { this.password = passwordHash; }

    /** Đánh dấu email đã xác minh — UC-AUTH-01/03/08 */
    public void verifyEmail(OffsetDateTime now) { if (this.emailVerifiedAt == null) this.emailVerifiedAt = now; }

    /** Đổi email đăng nhập (luôn chuẩn hóa chữ thường) — UC-AUTH-08 */
    public void changeEmail(String newEmail, OffsetDateTime now) { this.email = newEmail.trim().toLowerCase(Locale.ROOT); this.emailVerifiedAt = now; }

    /** Gắn Google / Apple (mỗi user tối đa 1 nhà cung cấp) — UC-AUTH-03 */
    public void linkIdentity(String provider, String sub) {
        if (this.oauthProvider != null) throw new IllegalStateException("Tài khoản đã gắn nhà cung cấp khác");
        this.oauthProvider = provider; this.oauthProviderId = sub;
    }

    /** Gỡ liên kết — chỉ khi đã có mật khẩu (không thì mất đường đăng nhập → GNE-AUTH-4093) — UC-AUTH-03 */
    public void unlinkIdentity() {
        if (!hasPassword()) throw new IllegalStateException("Chưa có mật khẩu");
        this.oauthProvider = null; this.oauthProviderId = null;
    }

    /** Khóa tạm do sai OTP đăng nhập 5 lần (status vẫn ACTIVE) — UC-AUTH-02 */
    public void lockFor(Duration duration, OffsetDateTime now) { this.lockedUntil = now.plus(duration); }

    /** Gỡ khóa tạm (mở khóa qua email, đặt lại mật khẩu) — UC-AUTH-02/04 */
    public void clearLock() { this.lockedUntil = null; this.failedLoginAttempts = 0; }

    /** Ghi yêu cầu xóa / hủy yêu cầu / đang chờ xóa — UC-AUTH-10 */
    public void requestDeletion(OffsetDateTime now) { if (this.deletionRequestedAt == null) this.deletionRequestedAt = now; }
    public void cancelDeletion() { this.deletionRequestedAt = null; }
    public boolean isPendingDeletion() { return this.deletionRequestedAt != null; }

    /** Factory người mua mới (đăng ký hoặc Google) — email chuẩn hóa chữ thường (plan AUTH D21) — UC-AUTH-01/03 */
    public static User newCustomer(String email, String passwordHash, OffsetDateTime now) { /* role = CUSTOMER, status = ACTIVE, emailVerifiedAt = now */ }

}
```

> **Bổ sung sau Phase 12 (2026-11-14, `V14__auth_additions.sql`, ADR 0007):** cột `deletion_requested_at`; index `uk_users_email_lower` (UNIQUE `lower(email)` — email không phân biệt hoa thường; giữ cả UNIQUE cũ để `ddl-auto=validate` khớp), `idx_users_deletion_requested_at` (một phần, `WHERE deletion_requested_at IS NOT NULL`). Email luôn được service chuẩn hóa `trim` + chữ thường trước khi ghi / tìm.

### 💡 Kiến thức chuyên sâu

**a) Mô hình "Single Table Account + Profile tách riêng"**
Dự án **không** tạo 3 bảng riêng cho Customer/Vendor/Admin. Thay vào đó:
- `users` giữ phần **chung nhất**: email, mật khẩu, quyền, trạng thái.
- 3 bảng profile giữ phần **riêng** của từng vai trò.

Lợi ích: login chỉ cần query 1 bảng; 1 email không thể tồn tại 2 lần dưới 2 vai trò khác nhau; khi user "nâng cấp" từ khách hàng lên người bán thì chỉ cần thêm 1 `VendorProfile`, **không phải tạo tài khoản mới**. Đây là biến thể của **Table-per-Type inheritance**.

**a1) Một tài khoản, hai chế độ Mua / Bán**
- Một email = một `users` = một `role`. Khi khách hàng mở shop (UC-VND-01), `role` đổi `CUSTOMER` → `VENDOR` nhưng `customer_profiles`, giỏ hàng, danh sách yêu thích và đơn đã mua vẫn gắn cùng `userId`.
- Spring Security `RoleHierarchy` khai báo `ROLE_VENDOR > ROLE_CUSTOMER` và `ROLE_ADMIN > ROLE_STAFF`: chủ shop tự có mọi quyền mua hàng, không cần tài khoản thứ hai; Admin có mọi quyền của Staff. `STAFF`, `ADMIN` **không** kế thừa Customer — đó là tài khoản làm việc.
- Giao diện có nút chuyển chế độ Mua / Bán: cùng phiên đăng nhập, chỉ đổi khu vực hiển thị (storefront `/` ↔ Kênh Người bán `/seller`), không cấp token mới.
- Tiền hai bên tách biệt: mua hàng trả qua VNPay / chuyển khoản VietQR (SePay xác nhận) / COD, doanh thu shop nằm ở `VendorWallet`.
- Không được mua hàng của chính shop mình (`GNE-ORD-4094`): kiểm tra `Product.vendorId ≠ VendorProfile.id` của người mua ở giỏ hàng và checkout — chống thổi phồng đơn, đánh giá và quay vòng voucher sàn.

**c) Phân biệt khóa tạm (`lockedUntil`) vs `SUSPENDED`**

| | Khóa tạm (`lockedUntil > now`, `status` vẫn `ACTIVE`) | SUSPENDED |
|---|---|---|
| Nguyên nhân | Hệ thống tự động (sai mật khẩu / sai OTP đăng nhập 5 lần) | Người (Admin) ra quyết định |
| Thời hạn | `lockedUntil`, 15 phút | `suspendedUntil`, dài ngày (NULL = vô thời hạn) |
| Mở khoá | Tự động hết hạn / mở khóa qua OTP email (`ACCOUNT_UNLOCK`) / đặt lại mật khẩu | Admin gỡ thủ công (UC-ADM-03) hoặc tự mở khi hết hạn |
| Mã lỗi khi đăng nhập | `423 GNE-AUTH-4230` (`locked_until`, `locked_by`) | `403 GNE-AUTH-4031` |

Giá trị enum `LOCKED` vẫn giữ trong `AccountStatus` (NFR-12 — không xóa enum) nhưng TLCN **không** gán (plan AUTH D9).

**e) `failedLoginAttempts` + `lockedUntil` = chống Brute-force**
Thay vì chỉ giới hạn theo IP (kẻ tấn công đổi IP là né được), khoá theo tài khoản đảm bảo dù tấn công từ 1000 IP cũng chỉ được N lần thử cho 1 email.

---

## 1.2. `CustomerProfile` — Hồ sơ khách hàng
Bảng: `customer_profiles`

```java
@Entity
@Table(name = "customer_profiles")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CustomerProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long userId;                      // FK → users.id — UNIQUE nên quan hệ là 1-1

    @Column(nullable = false, length = 255)
    private String fullName;                  // Họ tên đầy đủ của khách hàng

    @Column(length = 20)
    private String phoneNumber;               // NULLABLE — Số điện thoại liên hệ

    @Column(length = 500)
    private String avatarUrl;                 // NULLABLE — Link ảnh đại diện

    @Enumerated(EnumType.STRING)
    private Gender gender;                    // NULLABLE — MALE / FEMALE / OTHER / PREFER_NOT_TO_SAY

    @Column(columnDefinition = "DATE")
    private LocalDate dateOfBirth;            // NULLABLE — Ngày sinh (chỉ ngày, không giờ)

    @Column(columnDefinition = "TEXT")
    private String bio;                       // NULLABLE — Giới thiệu bản thân

    @Column(length = 5)
    private String preferredLanguage;         // NULLABLE — Ngôn ngữ ưa thích: "vi", "en" (chuẩn ISO 639-1)

    private Long defaultAddressId;            // NULLABLE — FK mềm → addresses.id, địa chỉ giao hàng mặc định


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo hồ sơ

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    private OffsetDateTime anonymizedAt;       // NULLABLE — Thời điểm hồ sơ bị ẩn danh hoá (xoá tài khoản — UC-AUTH-10)


    // ===== QUAN HỆ =====
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;                        // Tài khoản đăng nhập tương ứng
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đặt địa chỉ giao hàng mặc định — UC-AUTH-09 */
    public void setDefaultAddress(Long addressId) {
        this.defaultAddressId = addressId;
    }

    /** Xóa PII hồ sơ, giữ ID cho khóa ngoại — UC-AUTH-10 */
    public void anonymize(OffsetDateTime now) {
        this.fullName = "Người dùng đã xóa";
        this.phoneNumber = null;
        this.avatarUrl = null;
        this.bio = null;
        this.dateOfBirth = null;
        this.anonymizedAt = now;
    }

}
```

### 💡 Kiến thức chuyên sâu

**`PREFER_NOT_TO_SAY` trong enum Gender:** Đây là thiết kế có chủ đích, khác hoàn toàn với `NULL`. `NULL` = "hệ thống chưa hỏi / user chưa điền". `PREFER_NOT_TO_SAY` = "user **đã chủ động chọn** không tiết lộ". Phân biệt được 2 trạng thái này giúp hệ thống không spam nhắc user điền lại thông tin.

**`dateOfBirth` dùng `LocalDate` chứ không phải `OffsetDateTime`:** Ngày sinh không có khái niệm giờ. Nếu lưu kèm giờ, khi đổi múi giờ (UTC+7 sang UTC) ngày sinh có thể **lùi 1 ngày**. Trong JPA, sử dụng `@Column(columnDefinition = "date")` kết hợp `LocalDate`.

**`defaultAddressId` là FK mềm (không ràng buộc):** Vì `addresses` có cơ chế xoá mềm (`isDeleted`), nếu đặt FK cứng thì việc xử lý sẽ phức tạp. Ứng dụng tự đảm bảo tính đúng đắn.

---

## 1.3. `Address` — Địa chỉ giao hàng
Bảng: `addresses`

```java
@Entity
@Table(name = "addresses")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Address {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — địa chỉ này thuộc về ai

    @Column(nullable = false, length = 255)
    private String recipientName;             // Tên người NHẬN hàng (có thể khác tên chủ tài khoản)

    @Column(nullable = false, length = 20)
    private String phoneNumber;               // SĐT người nhận — shipper gọi số này

    @Column(nullable = false, length = 500)
    private String addressLine;               // Địa chỉ chi tiết: số nhà, tên đường, toà nhà

    @Column(nullable = false, length = 100)
    private String ward;                      // TÊN phường/xã — dùng để HIỂN THỊ cho người đọc

    @Column(nullable = false, length = 50)
    private String wardCode;                  // MÃ phường/xã/đặc khu (địa giới 2 cấp từ 01/07/2025) — dùng để GỌI API vận chuyển

    @Column(length = 100)
    private String district;                  // NULLABLE — TÊN quận/huyện, chỉ còn ở địa chỉ cũ trước sáp nhập (V16; plan AUTH D25)

    private Integer districtId;               // NULLABLE — MÃ quận/huyện, chỉ còn ở địa chỉ cũ trước sáp nhập (V16; plan AUTH D25)

    @Column(nullable = false, length = 100)
    private String province;                  // TÊN tỉnh/thành phố (34 đơn vị sau sáp nhập) — để hiển thị

    @Column(nullable = false)
    private Integer provinceId;               // MÃ tỉnh/thành theo GHN — để gọi API

    @Column(length = 200)
    private String deliveryNote;              // NULLABLE — ghi chú cho người giao hàng ("Gần cổng ĐH NEU, gửi phòng bảo vệ") — UC-AUTH-09, plan AUTH D27 (V17)

    private Boolean isDefault;                // NULLABLE — true = địa chỉ mặc định khi thanh toán

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddressLabel label = AddressLabel.HOME; // Loại địa chỉ: HOME (Nhà riêng) / OFFICE (Văn phòng) / SCHOOL (Trường học) / APARTMENT (Chung cư) / OTHER (Khác) — UC-AUTH-09 (TLCN v1.3; 2026-11-18 thêm 3 loại, migration V15)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;        // Xoá mềm — true = user đã xoá, nhưng bản ghi vẫn giữ lại

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo địa chỉ

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đánh dấu địa chỉ mặc định (service bỏ cờ ở địa chỉ khác cùng user) — UC-AUTH-09 */
    public void markAsDefault() {
        this.isDefault = true;
    }

    /** Xóa mềm địa chỉ — UC-AUTH-09 */
    public void softDelete() {
        this.isDeleted = true;
        this.isDefault = false;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Vì sao lưu CẢ tên lẫn mã (`ward` + `wardCode`, `district` + `districtId`)?**
Đây là mẫu thiết kế **"Denormalize for Display, Normalize for Integration"**:
- **Mã (`districtId`, `wardCode`)** là thứ API của Giao Hàng Nhanh (GHN) hiểu. Không có mã thì không tính được phí ship, không tạo được vận đơn.
- **Tên (`district`, `ward`)** là thứ con người đọc. Nếu chỉ lưu mã, mỗi lần hiển thị địa chỉ lại phải gọi API GHN để tra tên → chậm và phụ thuộc mạng.
- Quan trọng hơn: khi **địa giới hành chính Việt Nam thay đổi** (sáp nhập phường/xã), tên cũ trong đơn hàng cũ vẫn phải giữ nguyên để đối chiếu.
- **Địa giới 2 cấp từ 01/07/2025** (Nghị quyết 202/2025/QH15 — 34 tỉnh, 3.321 xã, bỏ cấp huyện): địa chỉ mới chỉ lưu tỉnh + phường; `district` / `districtId` cho phép `NULL` (migration `V19`), không xóa cột để địa chỉ cũ vẫn đọc được (plan AUTH D25).

**b) Xoá mềm (Soft Delete) qua `isDeleted`**
Không dùng `DELETE FROM addresses`. Lý do: các đơn hàng cũ có thể vẫn tham chiếu tới địa chỉ này. Xoá cứng sẽ làm gãy dữ liệu lịch sử. Xoá mềm chỉ ẩn khỏi danh sách hiển thị.
> **Đánh đổi:** Mọi câu query đều phải nhớ thêm `WHERE is_deleted = false`. Quên là lộ dữ liệu đã xoá. Trong JPA có thể dùng `@Where(clause = "is_deleted = false")` ở cấp entity để tự động hoá.

**c) Nghịch lý `isDefault` kiểu `Boolean` (nullable)**
Về lý thuyết chỉ cần `true/false`. Nhưng để `NULL` cho phép phân biệt "chưa từng set" với "đã set là false". Ngoài ra, DB không thể tự ràng buộc "mỗi user chỉ có đúng 1 địa chỉ default" bằng UNIQUE thông thường — logic này phải nằm ở tầng service (khi set 1 địa chỉ làm default thì UPDATE tất cả địa chỉ khác của user đó về false, trong cùng 1 transaction).

---

## 1.4. `VendorProfile` — Hồ sơ người bán (Shop)
Bảng: `vendor_profiles`

```java
@Entity
@Table(name = "vendor_profiles")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VendorProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long userId;                      // FK → users.id — UNIQUE, 1 tài khoản chỉ mở được 1 shop

    @Column(nullable = false, length = 255)
    private String shopName;                  // Tên cửa hàng hiển thị cho khách

    private OffsetDateTime shopNameChangedAt;  // NULLABLE — Lần đổi tên shop gần nhất; đổi tối đa 1 lần / 30 ngày (UC-VND-21, V20)

    private Long categoryId;                  // NULLABLE — FK → categories.id, ngành hàng chính của shop

    @Enumerated(EnumType.STRING)
    private BusinessType businessType;        // NULLABLE — INDIVIDUAL (cá nhân) / HOUSEHOLD (hộ KD) / BUSINESS (doanh nghiệp)

    @Column(length = 255)
    private String identityCode;              // NULLABLE — Số CCCD/CMND của chủ shop (dùng đối chiếu KYC)

    @Column(length = 255)
    private String taxCode;                   // NULLABLE — Mã số thuế (bắt buộc với hộ KD và doanh nghiệp)

    @Column(length = 255)
    private String representative;            // NULLABLE — Họ tên người đại diện pháp luật

    @Column(length = 20)
    private String contactPhone;              // NULLABLE — SĐT liên hệ của shop (form Mở shop — API 9.1, V20)

    @Column(length = 255)
    private String contactEmail;              // NULLABLE — Email liên hệ của shop; OTP mở shop gửi tới email này (V20)

    @Column(length = 255)
    private String coverUrl;                  // NULLABLE — Ảnh bìa trang shop

    @Column(columnDefinition = "TEXT")
    private String shopDescription;           // NULLABLE — Mô tả giới thiệu về shop

    @Column(length = 500)
    private String logoUrl;                   // NULLABLE — Logo/avatar của shop

    // ---- Hồ sơ KYC: ảnh giấy tờ đã upload ----
    @Column(length = 500)
    private String idFrontUrl;                // NULLABLE — Ảnh MẶT TRƯỚC CCCD

    @Column(length = 500)
    private String idBackUrl;                 // NULLABLE — Ảnh MẶT SAU CCCD

    @Column(length = 500)
    private String licenseUrl;                // NULLABLE — Ảnh giấy phép kinh doanh

    @Column(length = 500)
    private String selfieUrl;                 // NULLABLE — Ảnh chân dung tự chụp, dùng so khớp khuôn mặt với CCCD

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String kycDocumentsMetaJson;      // NULLABLE — Tên tệp + dung lượng gốc từng giấy tờ, vd {"ID_FRONT":{"file_name":"cccd.jpg","size_bytes":2202009}}; màn KYC chỉ hiện thông tin này, không trả URL ảnh (V20)

    @Builder.Default
    @Column(nullable = false)
    private Boolean kycBiometricConsent = false; // Đồng ý xử lý dữ liệu sinh trắc (so khớp khuôn mặt); false → luôn chuyển nhân viên đối chiếu (UC-VND-02, V20)

    // ---- Kết quả thẩm định KYC ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus;              // NOT_SUBMITTED / PENDING / AI_PROCESSING / MANUAL_REVIEW / VERIFIED / REJECTED / SUPPLEMENT_REQUESTED (7 trạng thái)

    @Column(columnDefinition = "TEXT")
    private String kycRejectionReason;        // NULLABLE — Lý do bị từ chối, để vendor biết đường sửa

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String kycSupplementDocumentsJson; // NULLABLE — Giấy tờ nhân viên yêu cầu bổ sung, vd ["ID_FRONT","BUSINESS_LICENSE"] (UC-VND-04, V20)

    private OffsetDateTime kycSupplementDeadlineAt; // NULLABLE — Hạn bổ sung (yêu cầu + 7 ngày); quá hạn → job chuyển REJECTED (V20)

    private OffsetDateTime kycManualReviewRequestedAt; // NULLABLE — Lúc người bán bấm "Yêu cầu nhân viên xem xét" (1 lần / hồ sơ; hồ sơ nộp lại mà AI kết luận FAIL thì vào MANUAL_REVIEW) (V20)

    @Enumerated(EnumType.STRING)
    private KycVerificationMethod kycVerificationMethod; // NULLABLE — OCR_AUTO (AI duyệt) / MANUAL_STAFF (người duyệt) / BIOMETRIC_FACIAL (sinh trắc khuôn mặt)

    private Long kycVerifiedBy;               // NULLABLE — ID staff đã duyệt (KHÔNG đặt FK, xem giải thích bên dưới)

    @Column(precision = 5, scale = 4)
    private BigDecimal commissionRate;        // NULLABLE — Tỷ lệ hoa hồng THƯƠNG LƯỢNG RIÊNG của shop (VD: 0.0300 = 3%). NULL = không có thỏa thuận riêng → dùng Category.commissionRate → mặc định toàn sàn 5% (xem effectiveCommissionRate())

    private OffsetDateTime kycSubmittedAt;     // NULLABLE — Thời điểm vendor nộp hồ sơ

    private OffsetDateTime kycVerifiedAt;      // NULLABLE — Thời điểm hồ sơ được duyệt

    // ---- Địa chỉ lấy hàng (kho của shop) — NULLABLE cho tới khi hoàn tất UC-VND-05 (đăng ký GHN) ----
    private Integer pickupProvinceId;         // NULLABLE — Mã tỉnh kho hàng (GHN) — kho lấy hàng duy nhất của shop trong MVP (UC-VND-05), dùng tính phí ship GHN

    @Column(length = 100)
    private String pickupProvince;            // NULLABLE — Tên tỉnh/thành kho hàng (cache hiển thị, như Address.province)

    private Integer pickupDistrictId;         // NULLABLE — Mã quận/huyện kho hàng (GHN)

    @Column(length = 100)
    private String pickupDistrict;            // NULLABLE — Tên quận/huyện kho hàng (cache hiển thị)

    @Column(length = 50)
    private String pickupWardCode;            // NULLABLE — Mã phường/xã kho hàng (GHN)

    @Column(length = 100)
    private String pickupWard;                // NULLABLE — Tên phường/xã kho hàng (cache hiển thị)

    @Column(length = 500)
    private String pickupAddressLine;         // NULLABLE — Địa chỉ chi tiết kho hàng để shipper tới lấy

    @Column(length = 20)
    private String pickupPhone;               // NULLABLE — SĐT người liên hệ tại kho lấy hàng (gửi GHN — UC-VND-05, V20)

    private Integer ghnShopId;                // NULLABLE — ID shop do GHN cấp sau khi gọi API đăng ký (UC-VND-05); NULL khi mới mở shop (UC-VND-01)

    // ---- Tài khoản nhận tiền ----
    @Column(length = 100)
    private String payoutBankName;            // NULLABLE — Tên ngân hàng nhận tiền rút

    @Column(length = 50)
    private String payoutBankAccountNumber;   // NULLABLE — Số tài khoản ngân hàng

    @Column(length = 255)
    private String payoutBankAccountName;     // NULLABLE — Tên chủ tài khoản ngân hàng

    private OffsetDateTime payoutAccountVerifiedAt; // NULLABLE — Thời điểm xác minh tài khoản chính chủ (tên ngân hàng trả về khớp họ tên KYC). NULL = chưa được rút tiền; đổi tài khoản thì đặt lại NULL

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm đăng ký shop

    // ---- Chỉ số hiển thị trang Shop & cấu hình vận hành (đối chiếu giao diện 2026-10-29) ----
    @Builder.Default
    @Column(nullable = false)
    private Boolean isMall = false;           // Huy hiệu "Chính hãng" / bộ lọc "Shop Chính Hãng": Staff gắn khi duyệt KYC doanh nghiệp chính hãng (UC-VND-04)

    @Builder.Default
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal ratingAvg = BigDecimal.ZERO; // Điểm đánh giá shop = trung bình sao các đánh giá đang hiển thị của mọi sản phẩm shop (UC-CAT-16)

    @Builder.Default
    @Column(nullable = false)
    private Integer ratingCount = 0;          // Số đánh giá dùng để tính ratingAvg

    @Builder.Default
    @Column(nullable = false)
    private Integer followerCount = 0;        // Số người theo dõi (đếm sẵn, đồng bộ với shop_follows — UC-SOC-06)

    @Builder.Default
    @Column(nullable = false)
    private Integer activeProductCount = 0;   // Số sản phẩm PUBLISHED (cập nhật khi đăng / ẩn / xóa sản phẩm)

    @Column(precision = 5, scale = 2)
    private BigDecimal chatResponseRate;      // NULLABLE — % hội thoại do khách mở được shop trả lời trong 24h, tính trên 30 ngày (job hằng ngày); NULL = chưa đủ dữ liệu

    private Integer avgResponseMinutes;       // NULLABLE — Thời gian phản hồi chat trung bình (phút) — hiển thị "phản hồi trong vài phút / vài giờ"

    @Builder.Default
    @Column(nullable = false)
    private Boolean preOrderEnabled = false;  // Cho phép đăng sản phẩm đặt trước (Pre-order) — UC-VND-21

    @Builder.Default
    @Column(nullable = false)
    private Boolean isFavorite = false;       // Huy hiệu "Shop Yêu Thích" do sàn gắn (đánh giá ≥ 4.8, phản hồi chat ≥ 90%) — UC-CAT-16 (TLCN v1.3)

    // ---- Chat tự động (UC-VND-06) ----
    @Builder.Default
    @Column(nullable = false)
    private Boolean autoReplyEnabled = false; // Bật/tắt tính năng trả lời tin nhắn tự động

    @Column(columnDefinition = "TEXT")
    private String autoReplyGreeting;         // NULLABLE — Câu chào tự động khi khách mở chat

    @Column(columnDefinition = "TEXT")
    private String autoReplyOffline;          // NULLABLE — Câu trả lời khi shop vắng mặt / ngoài giờ làm việc

    // ---- Chế độ tạm nghỉ (Vacation Mode — UC-VND-19) ----
    private OffsetDateTime vacationStartsAt;   // NULLABLE — Bắt đầu tạm nghỉ: sản phẩm vẫn hiển thị nhưng KHÔNG cho đặt mua; NULL = đang bán bình thường

    private OffsetDateTime vacationEndsAt;     // NULLABLE — Tự động mở bán lại (tối đa 30 ngày / lần)

    @Column(length = 500)
    private String vacationMessage;           // NULLABLE — Thông báo hiển thị trên trang shop và trả lời chat tự động khi tạm nghỉ

    // ===== QUAN HỆ =====
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;                        // Tài khoản đăng nhập của chủ shop
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Factory mở shop: KYC chưa nộp, chưa có hoa hồng riêng / kho / GHN — UC-VND-01 bước 4 */
    public static VendorProfile register(Long userId, String shopName, Long categoryId, BusinessType businessType,
                                         String representative, String contactPhone, String contactEmail) {
        return VendorProfile.builder()
                .userId(userId).shopName(shopName).categoryId(categoryId).businessType(businessType)
                .representative(representative).contactPhone(contactPhone).contactEmail(contactEmail)
                .kycStatus(KycStatus.NOT_SUBMITTED)
                .build();
    }

    /** Hồ sơ còn sửa / nộp được: chưa nộp, bị từ chối hoặc được yêu cầu bổ sung — UC-VND-02 */
    public boolean isKycEditable() {
        return this.kycStatus == KycStatus.NOT_SUBMITTED || this.kycStatus == KycStatus.REJECTED
                || this.kycStatus == KycStatus.SUPPLEMENT_REQUESTED;
    }

    /** Lưu nháp dữ liệu kê khai KYC (trường null = giữ nguyên), không đổi trạng thái — UC-VND-02 */
    public void saveKycDraft(String identityCode, String taxCode, String representative, String shopName,
                             BusinessType businessType, Boolean biometricConsent) {
        if (!isKycEditable()) throw new IllegalStateException("Hồ sơ KYC đang được xử lý hoặc đã duyệt");
        if (identityCode != null) this.identityCode = identityCode;
        if (taxCode != null) this.taxCode = taxCode.isBlank() ? null : taxCode;
        if (representative != null) this.representative = representative;
        if (shopName != null) this.shopName = shopName;
        if (businessType != null) this.businessType = businessType;
        if (biometricConsent != null) this.kycBiometricConsent = biometricConsent;
    }

    /** Gắn giấy tờ đã mã hóa (khóa đối tượng .enc; null = giữ bản cũ) + metadata hiển thị — UC-VND-02 */
    public void attachKycDocuments(String idFrontKey, String idBackKey, String licenseKey, String selfieKey,
                                   String documentsMetaJson) {
        if (!isKycEditable()) throw new IllegalStateException("Hồ sơ KYC đang được xử lý hoặc đã duyệt");
        if (idFrontKey != null) this.idFrontUrl = idFrontKey;
        if (idBackKey != null) this.idBackUrl = idBackKey;
        if (licenseKey != null) this.licenseUrl = licenseKey;
        if (selfieKey != null) this.selfieUrl = selfieKey;
        this.kycDocumentsMetaJson = documentsMetaJson;
    }

    /** Nộp / nộp lại hồ sơ KYC (chỉ khi chưa nộp, bị từ chối, cần bổ sung) — UC-VND-02 */
    public void submitKyc(OffsetDateTime now) {
        if (!isKycEditable()) throw new IllegalStateException("Hồ sơ KYC đang được xử lý hoặc đã duyệt");
        this.kycStatus = KycStatus.PENDING;
        this.kycSubmittedAt = now;
        this.kycRejectionReason = null;
        this.kycSupplementDocumentsJson = null;
        this.kycSupplementDeadlineAt = null;
    }

    /** Đẩy hồ sơ vào hàng đợi AI Worker OCR/FaceMatch — UC-VND-03 */
    public void startAiProcessing() {
        if (this.kycStatus != KycStatus.PENDING) throw new IllegalStateException("Chỉ xử lý AI khi hồ sơ PENDING");
        this.kycStatus = KycStatus.AI_PROCESSING;
    }

    /** AI nghi ngờ / điểm tin cậy thấp / AI lỗi hoặc quá hạn → chuyển nhân viên duyệt tay — UC-VND-03 */
    public void routeToManualReview() {
        if (this.kycStatus != KycStatus.AI_PROCESSING) throw new IllegalStateException("Chỉ chuyển duyệt tay từ AI_PROCESSING");
        this.kycStatus = KycStatus.MANUAL_REVIEW;
    }

    /** Hồ sơ bị từ chối → người bán yêu cầu nhân viên xem xét lại (1 lần / hồ sơ) — UC-VND-03 ràng buộc */
    public void requestManualReview(OffsetDateTime now) {
        if (this.kycStatus != KycStatus.REJECTED) throw new IllegalStateException("Chỉ yêu cầu xem xét khi hồ sơ bị từ chối");
        if (isManualReviewRequested()) throw new IllegalStateException("Đã yêu cầu nhân viên xem xét");
        this.kycManualReviewRequestedAt = now;
        this.kycStatus = KycStatus.MANUAL_REVIEW;
    }

    /** Đã từng yêu cầu nhân viên xem xét — UC-VND-03 */
    public boolean isManualReviewRequested() {
        return this.kycManualReviewRequestedAt != null;
    }

    /** Nhân viên yêu cầu bổ sung giấy tờ (không từ chối thẳng), hạn 7 ngày — UC-VND-04 */
    public void requestSupplement(String documentsJson, String note, OffsetDateTime deadline) {
        if (this.kycStatus != KycStatus.MANUAL_REVIEW) throw new IllegalStateException("Chỉ yêu cầu bổ sung khi đang duyệt tay");
        this.kycStatus = KycStatus.SUPPLEMENT_REQUESTED;
        this.kycRejectionReason = note;
        this.kycSupplementDocumentsJson = documentsJson;
        this.kycSupplementDeadlineAt = deadline;
    }

    /** Quá hạn bổ sung hồ sơ → từ chối (job hằng ngày) — UC-VND-04 */
    public void expireSupplement(String reason) {
        if (this.kycStatus != KycStatus.SUPPLEMENT_REQUESTED) throw new IllegalStateException("Hồ sơ không ở trạng thái chờ bổ sung");
        this.kycStatus = KycStatus.REJECTED;
        this.kycRejectionReason = reason;
        this.kycSupplementDeadlineAt = null;
    }

    /** Đưa hồ sơ đã quyết định trở lại hàng chờ duyệt tay (không chạy lại AI) — UC-VND-04 */
    public void requeueForReview() {
        if (this.kycStatus != KycStatus.VERIFIED && this.kycStatus != KycStatus.REJECTED
                && this.kycStatus != KycStatus.SUPPLEMENT_REQUESTED) {
            throw new IllegalStateException("Hồ sơ đang ở hàng chờ hoặc chưa nộp");
        }
        this.kycStatus = KycStatus.MANUAL_REVIEW;
        this.kycRejectionReason = null;
        this.kycSupplementDocumentsJson = null;
        this.kycSupplementDeadlineAt = null;
        this.kycVerificationMethod = null;
        this.kycVerifiedBy = null;
        this.kycVerifiedAt = null;
    }

    /** Duyệt KYC: AI tự duyệt (từ AI_PROCESSING) hoặc nhân viên duyệt (từ MANUAL_REVIEW) — UC-VND-03, UC-VND-04 */
    public void approveKyc(Long staffId, KycVerificationMethod method, OffsetDateTime now) {
        if (this.kycStatus != KycStatus.AI_PROCESSING && this.kycStatus != KycStatus.MANUAL_REVIEW) {
            throw new IllegalStateException("Hồ sơ không ở trạng thái chờ duyệt");
        }
        this.kycStatus = KycStatus.VERIFIED;
        this.kycVerificationMethod = method;
        this.kycVerifiedBy = staffId;
        this.kycVerifiedAt = now;
        this.kycRejectionReason = null;
        this.kycSupplementDocumentsJson = null;
        this.kycSupplementDeadlineAt = null;
    }

    /** Từ chối KYC kèm lý do (AI kết luận FAIL hoặc nhân viên từ chối) — UC-VND-03, UC-VND-04 */
    public void rejectKyc(String reason) {
        if (this.kycStatus != KycStatus.AI_PROCESSING && this.kycStatus != KycStatus.MANUAL_REVIEW) {
            throw new IllegalStateException("Hồ sơ không ở trạng thái chờ duyệt");
        }
        this.kycStatus = KycStatus.REJECTED;
        this.kycRejectionReason = reason;
    }

    /** Gắn / gỡ huy hiệu "Chính hãng" khi duyệt KYC doanh nghiệp — UC-VND-04 bước 8 */
    public void markAsMall(boolean mall) {
        this.isMall = mall;
    }

    /** Hoa hồng thương lượng riêng (tỷ lệ 0..1, vd 0.0300 = 3%); null = theo ngành hàng — UC-VND-04 */
    public void setNegotiatedCommission(BigDecimal rate) {
        if (rate != null && (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("Tỷ lệ hoa hồng phải trong khoảng 0–100%");
        }
        this.commissionRate = rate;
    }

    /** Thời điểm được đổi tên lần tiếp theo; null = đổi được ngay — UC-VND-21 */
    public OffsetDateTime nextRenameAllowedAt(OffsetDateTime now) {
        if (this.shopNameChangedAt == null) return null;
        OffsetDateTime next = this.shopNameChangedAt.plusDays(30);
        return next.isAfter(now) ? next : null;
    }

    /** Đổi tên shop, tối đa 1 lần / 30 ngày (Service kiểm trùng tên / từ cấm trước) — UC-VND-21 */
    public void renameShop(String newName, OffsetDateTime now) {
        if (newName.equals(this.shopName)) return;
        if (nextRenameAllowedAt(now) != null) throw new IllegalStateException("Chỉ đổi tên shop 1 lần mỗi 30 ngày");
        this.shopName = newName;
        this.shopNameChangedAt = now;
    }

    /** Cập nhật mô tả, logo, ảnh bìa (khóa đối tượng trong avatar-assets) — UC-VND-21 */
    public void updateBranding(String description, String logoKey, String coverKey) {
        this.shopDescription = description;
        this.logoUrl = logoKey;
        this.coverUrl = coverKey;
    }

    /** Cấu hình tin nhắn trả lời tự động: bật mà cả hai nội dung đều rỗng → lỗi (GNE-VND-4222) — UC-VND-06 */
    public void configureAutoReply(boolean enabled, String greeting, String awayMessage) {
        boolean empty = (greeting == null || greeting.isBlank()) && (awayMessage == null || awayMessage.isBlank());
        if (enabled && empty) throw new IllegalArgumentException("Bật tự trả lời cần ít nhất một nội dung");
        this.autoReplyEnabled = enabled;
        this.autoReplyGreeting = greeting;
        this.autoReplyOffline = awayMessage;
    }

    /** Lưu kho lấy hàng sau khi GHN đã cấp / cập nhật Shop ID — UC-VND-05 */
    public void updatePickup(String phone, Integer provinceId, String provinceName, Integer districtId, String districtName,
                             String wardCode, String wardName, String addressLine, Integer ghnShopId) {
        this.pickupPhone = phone;
        this.pickupProvinceId = provinceId;
        this.pickupProvince = provinceName;
        this.pickupDistrictId = districtId;
        this.pickupDistrict = districtName;
        this.pickupWardCode = wardCode;
        this.pickupWard = wardName;
        this.pickupAddressLine = addressLine;
        this.ghnShopId = ghnShopId;
    }

    /** Đã cấu hình kho lấy hàng & đăng ký GHN (điều kiện đăng bán / nhận đơn) — UC-VND-05 */
    public boolean isPickupConfigured() {
        return this.ghnShopId != null && this.pickupDistrictId != null && this.pickupWardCode != null && this.pickupAddressLine != null;
    }

    /** Tỷ lệ hoa hồng áp dụng: thỏa thuận riêng của shop → ngành hàng → mặc định 5% — UC-PAY-05 */
    public BigDecimal effectiveCommissionRate(BigDecimal categoryRate) {
        if (this.commissionRate != null) return this.commissionRate;          // thương lượng riêng ghi đè ngành hàng
        if (categoryRate != null) return categoryRate;
        return new BigDecimal("0.0500");                                      // mặc định toàn sàn
    }

    /** Xác minh tài khoản nhận tiền chính chủ: tên chủ TK do ngân hàng trả về (tra cứu tên qua đối tác chi hộ / NAPAS) phải khớp họ tên KYC sau khi bỏ dấu, viết hoa, gộp khoảng trắng — UC-VND-07 */
    public boolean verifyPayoutAccount(String bankReturnedHolderName, String kycFullName, OffsetDateTime now) {
        boolean matched = NameNormalizer.normalize(bankReturnedHolderName).equals(NameNormalizer.normalize(kycFullName));
        this.payoutAccountVerifiedAt = matched ? now : null;
        return matched;
    }

    /** Đã có tài khoản nhận tiền chính chủ — điều kiện tạo lệnh rút (GNE-WAL-4032) — UC-ADM-04 */
    public boolean isPayoutAccountVerified() {
        return this.kycStatus == KycStatus.VERIFIED && this.payoutAccountVerifiedAt != null;
    }

    /** Lưu tài khoản nhận tiền chỉ khi tên ngân hàng trả về khớp họ tên KYC; không khớp → giữ nguyên tài khoản cũ, trả false — UC-VND-07 */
    public boolean setPayoutAccount(String bankName, String accountNumber, String holderName, String bankReturnedHolderName,
                                    String kycFullName, OffsetDateTime now) {
        if (!NameNormalizer.normalize(bankReturnedHolderName).equals(NameNormalizer.normalize(kycFullName))) return false;
        this.payoutBankName = bankName;
        this.payoutBankAccountNumber = accountNumber;
        this.payoutBankAccountName = holderName;
        this.payoutAccountVerifiedAt = now;
        return true;
    }

    /** Xóa tài khoản nhận tiền (Service kiểm không còn lệnh rút đang chờ) — UC-VND-07 */
    public void clearPayoutAccount() {
        this.payoutBankName = null;
        this.payoutBankAccountNumber = null;
        this.payoutBankAccountName = null;
        this.payoutAccountVerifiedAt = null;
    }

    /** Cập nhật điểm đánh giá shop khi có đánh giá mới / bị ẩn — UC-CAT-16 */
    public void applyRating(BigDecimal newAvg, int newCount) {
        this.ratingAvg = newAvg;
        this.ratingCount = newCount;
    }

    /** Huy hiệu "Shop Yêu Thích": điểm ≥ 4.5, đủ 20 đánh giá và phản hồi chat ≥ 80% — UC-CAT-16 */
    public boolean isPreferredShop() {
        return this.ratingCount >= 20 && this.ratingAvg.compareTo(new BigDecimal("4.5")) >= 0
                && this.chatResponseRate != null && this.chatResponseRate.compareTo(new BigDecimal("80")) >= 0;
    }


    /** Bật chế độ tạm nghỉ (Service kiểm tra không còn đơn CONFIRMED/PREPARING chưa bàn giao) — UC-VND-19 */
    public void startVacation(OffsetDateTime from, OffsetDateTime until, String message, OffsetDateTime now) {
        if (from.isBefore(now.minusMinutes(1)) || !until.isAfter(from)) throw new IllegalArgumentException("Khoảng thời gian không hợp lệ");
        if (until.isAfter(from.plusDays(30))) throw new IllegalArgumentException("Tạm nghỉ tối đa 30 ngày mỗi lần");
        this.vacationStartsAt = from;
        this.vacationEndsAt = until;
        this.vacationMessage = message;
    }

    /** Kết thúc tạm nghỉ (shop chủ động hoặc Cron khi tới vacationEndsAt) — UC-VND-19 */
    public void endVacation() {
        this.vacationStartsAt = null;
        this.vacationEndsAt = null;
        this.vacationMessage = null;
    }

    /** Đang tạm nghỉ: chặn đặt mua sản phẩm của shop, chat tự động trả vacationMessage — UC-VND-19 */
    public boolean isOnVacation(OffsetDateTime now) {
        return this.vacationStartsAt != null && !now.isBefore(this.vacationStartsAt) && (this.vacationEndsAt == null || now.isBefore(this.vacationEndsAt));
    }

}
```

### 💡 Kiến thức chuyên sâu

**a) KYC (Know Your Customer) — Vì sao sàn TMĐT bắt buộc phải có?**
KYC là quy trình xác minh danh tính người bán trước khi cho phép kinh doanh. Không có KYC, sàn sẽ đầy shop ma bán hàng giả rồi biến mất. Về pháp lý, Luật Thương mại điện tử 2025 (thay cho Nghị định 52/2013 và 85/2021) **bắt buộc** sàn TMĐT phải lưu và xác thực thông tin người bán.

Luồng KYC trong dự án này:

```
NOT_SUBMITTED  →  (vendor upload giấy tờ)  →  PENDING
PENDING  →  (Đẩy vào AI Worker)  →  AI_PROCESSING
AI_PROCESSING  →  (PASS: riskScore < 30, không bị lấy mẫu)  →  VERIFIED   (OCR_AUTO)
AI_PROCESSING  →  (PARTIAL: 30 ≤ riskScore < 70, hoặc lấy mẫu 5%)  →  MANUAL_REVIEW
AI_PROCESSING  →  (FAIL: riskScore ≥ 70)  →  REJECTED
MANUAL_REVIEW  →  (Staff duyệt)  →  VERIFIED / REJECTED / SUPPLEMENT_REQUESTED
SUPPLEMENT_REQUESTED  →  (vendor bổ sung)  →  PENDING  (vòng lặp)
REJECTED  →  (vendor nộp lại, có thể kèm "Yêu cầu nhân viên xem xét")  →  PENDING
```

Hồ sơ nộp lại kèm yêu cầu xem xét vẫn được AI chấm điểm nhưng luôn đi `routeToManualReview()` thay vì tự từ chối (không để quyết định bất lợi hoàn toàn tự động).

`SUPPLEMENT_REQUESTED` là trạng thái "nửa vời" rất thực tế: hồ sơ không sai nhưng ảnh mờ / thiếu 1 giấy tờ → yêu cầu bổ sung thay vì từ chối thẳng, giữ chân người bán.

**b) `commissionRate` nằm ở đây thay vì hằng số cứng**
Mỗi shop có thể được thương lượng mức hoa hồng khác nhau (shop lớn thường được ưu đãi). Lưu ở cấp vendor cho phép sàn linh hoạt về kinh doanh. Cột **cho phép NULL**: NULL nghĩa là shop không có thỏa thuận riêng.
> **Thứ tự áp dụng (thống nhất UC-PAY-05 và Document_Vi 2.4):** `VendorProfile.commissionRate` (thương lượng riêng, **ghi đè**) → `Category.commissionRate` (mặc định ngành) → **5%** mặc định toàn sàn — cài đặt ở `effectiveCommissionRate(categoryRate)`. Shop thương lượng được 3% trong ngành 5% thì bị thu 3%.
> ⚠️ **Snapshot:** khi phát sinh đơn hàng, tỷ lệ áp dụng được **COPY** vào từng `OrderItem.commissionRate` (mỗi dòng theo ngành hàng của sản phẩm đó). `EscrowLedger.commissionFee` = tổng `OrderItem.commissionAmount` của đơn. Nếu mai sau sàn đổi hoa hồng, các đơn cũ vẫn giữ tỷ lệ tại thời điểm bán. Đây lại là **Snapshot Pattern**.

**b2) Vòng đời các trường lấy hàng (`pickup*`, `ghnShopId`)**
UC-VND-01 (đăng ký mở shop) chỉ tạo `vendor_profiles` với `shopName`, `businessType`, `kycStatus = NOT_SUBMITTED`; mã kho và `ghnShopId` chỉ có sau UC-VND-05 (gọi API GHN). Vì vậy các cột này **NULLABLE**; `isPickupConfigured()` là điều kiện bắt buộc trước khi shop đăng bán (UC-CAT-09) và nhận đơn. Tên tỉnh/huyện/xã (`pickupProvince`, `pickupDistrict`, `pickupWard`) là bản sao hiển thị như `Address`, tránh gọi API GHN mỗi lần hiển thị.

**b5) Tài khoản nhận tiền chính chủ — "mượn KYC của ngân hàng"**
Ngân hàng đã phải xác thực sinh trắc học chủ tài khoản với dữ liệu dân cư. Nếu sàn **chỉ giải ngân vào tài khoản mà tên ngân hàng trả về khớp họ tên trên CCCD**, kẻ dùng thẻ giả có bán được hàng cũng không rút được tiền. Vì vậy KYC tự động có thể duyệt nhanh (Tầng 0–1), còn `isPayoutAccountVerified()` là chốt chặn cứng trước lần rút tiền đầu tiên. Đổi tài khoản ngân hàng (UC-VND-07) đặt lại `payoutAccountVerifiedAt = NULL` và phải xác minh lại.

**c) Vì sao `kycVerifiedBy` KHÔNG đặt Foreign Key?**
Trong thiết kế JPA/CSDL không đặt `@ManyToOne` hay Foreign Key ràng buộc cứng (`// Not a DB Foreign Key`). Lý do:
1. Staff duyệt hồ sơ có thể nghỉ việc và tài khoản bị khóa/xoá mềm — FK cứng sẽ gây khó cho thao tác đó.
2. Giá trị này đôi khi là ID của **hệ thống AI** (không phải user thật).
3. Đây là dữ liệu **audit** (ghi nhận lịch sử), không phải dữ liệu quan hệ nghiệp vụ. Audit data ưu tiên "giữ được mãi mãi" hơn là "toàn vẹn tham chiếu".

**d) Vì sao tách địa chỉ lấy hàng (`pickup*`) ra khỏi bảng `addresses`?**
Bảng `addresses` là địa chỉ **nhận hàng của khách** (nhiều địa chỉ / 1 user). Địa chỉ kho là **điểm xuất phát duy nhất** của shop, có ngữ nghĩa và vòng đời khác hẳn, lại cần join cực nhanh trong mọi lần tính phí ship. Nhúng thẳng vào `vendor_profiles` giúp tránh 1 lần JOIN trên đường đi nóng (hot path).

---

## 1.5. `AdminProfile` — Hồ sơ Quản trị viên / Nhân viên
Bảng: `admin_profiles`

```java
@Entity
@Table(name = "admin_profiles")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class AdminProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long userId;                      // FK → users.id, UNIQUE (quan hệ 1-1)

    @Column(nullable = false, length = 255)
    private String fullName;                  // Họ tên nhân viên/quản trị viên

    @Column(length = 500)
    private String avatarUrl;                 // NULLABLE — Ảnh đại diện

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActiveModerator = false; // true = đang trong danh sách nhận phân công kiểm duyệt

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo hồ sơ

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StaffTitle staffTitle;            // NULLABLE — Chức danh của STAFF (3 chức danh); NULL với ADMIN. Chỉ dùng nạp bộ quyền mặc định — quyền thật ở staff_permissions


    // ===== QUAN HỆ =====
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;                        // Tài khoản đăng nhập
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bật/tắt trạng thái đang trực kiểm duyệt — UC-ADM-03 */
    public void toggleModerator(boolean active) {
        this.isActiveModerator = active;
    }

    /** Đổi chức danh — chỉ trả về bộ quyền mặc định để Admin xác nhận, không tự cấp/thu hồi quyền — UC-ADM-01, UC-ADM-02 */
    public Set<StaffPermissionEnum> changeTitle(StaffTitle newTitle) {
        this.staffTitle = newTitle;
        return newTitle == null ? Set.of() : newTitle.defaultPermissions();
    }

}
```

### 💡 Kiến thức chuyên sâu

**`isActiveModerator` phục vụ cơ chế phân phối tải kiểm duyệt (Load Balancing).**
Sản phẩm mới đăng cần được duyệt (xem `Product.moderatorId`). Hệ thống sẽ lấy danh sách staff có `isActiveModerator = true` rồi chia việc theo vòng tròn (round-robin) hoặc theo số việc đang tồn. Khi 1 staff nghỉ phép, chỉ cần tắt cờ này — họ sẽ không nhận việc mới nhưng vẫn xử lý nốt việc đang có.

---

## 1.6. `KycResult` — Kết quả OCR/AI thẩm định giấy tờ
Bảng: `kyc_results`

```java
@Entity
@Table(name = "kyc_results")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class KycResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long vendorProfileId;             // FK → vendor_profiles.id, UNIQUE (mỗi shop 1 bản kết quả; TLCN chỉ KYC người bán)

    // ===== Dữ liệu bóc tách: QR mặt trước (nguồn chính) + MRZ mặt sau (đối chiếu) =====
    @Column(length = 20)
    private String extractedIdNumber;         // NULLABLE — Số CCCD 12 số (từ QR; MRZ chứa số này ở trường tùy chọn dòng 1)

    @Column(length = 255)
    private String extractedFullName;         // NULLABLE — Họ tên có dấu (từ QR; MRZ dòng 3 là bản không dấu để đối chiếu)

    @Column(length = 20)
    private String extractedDob;              // NULLABLE — Ngày sinh (từ QR, dạng ddMMyyyy; lưu String để giữ dữ liệu thô)

    @Column(length = 10)
    private String extractedGender;           // NULLABLE — Giới tính "Nam"/"Nữ" (từ QR; MRZ ghi M/F)

    @Column(length = 100)
    private String extractedNationality;      // NULLABLE — Quốc tịch (từ MRZ, mã VNM)

    @Column(columnDefinition = "TEXT")
    private String extractedAddress;          // NULLABLE — Nơi thường trú (từ QR)

    @Column(length = 20)
    private String extractedIssueDate;        // NULLABLE — Ngày cấp (từ QR)

    @Column(length = 20)
    private String extractedExpiryDate;       // NULLABLE — Ngày hết hạn (từ MRZ dòng 2, YYMMDD)

    @Column(length = 255)
    private String extractedIssuer;           // NULLABLE — Nơi cấp; QR/MRZ không chứa, chỉ có khi người duyệt nhập tay

    // ===== Tầng 0 — kiểm tra dữ liệu (vài chục ms, không cần GPU) =====
    private Boolean qrCodeDetected;           // NULLABLE — Giải mã được QR mặt trước (thẻ gắn chip / thẻ căn cước mới đều có)

    private Boolean mrzValid;                 // NULLABLE — Đọc được 3 dòng MRZ mặt sau và đúng toàn bộ check digit ICAO 9303 (trọng số 7-3-1)

    private Boolean mrzQrMatch;               // NULLABLE — QR khớp MRZ: số thẻ, ngày sinh, giới tính, họ tên (bỏ dấu, viết hoa)

    private Boolean cccdNumberValid;          // NULLABLE — Số CCCD đúng cấu trúc (mã tỉnh 3 số + thế kỷ/giới tính + 2 số năm sinh) và khớp ngày sinh, giới tính

    private Boolean expiryRuleValid;          // NULLABLE — Ngày hết hạn khớp quy tắc theo tuổi đổi thẻ (14 / 25 / 40 / 60 tuổi hoặc không thời hạn — Luật Căn cước 2023)

    private Boolean duplicateIdFound;         // NULLABLE — Số CCCD đã gắn với một shop khác đang hoạt động

    // ===== Tầng 1 — sinh trắc (~1 giây, CPU ONNX) =====
    private Boolean faceMatchResult;          // NULLABLE — Khuôn mặt selfie khớp ảnh chân dung cắt từ mặt trước thẻ

    @Column(precision = 5, scale = 4)
    private BigDecimal faceMatchScore;        // NULLABLE — Điểm tương đồng cosine 0.0000 → 1.0000 (YuNet + SFace)

    @Column(precision = 5, scale = 4)
    private BigDecimal livenessScore;         // NULLABLE — Điểm người thật 0.0000 → 1.0000 (thử thách nháy mắt/quay đầu phía client + anti-spoof thụ động)

    private Boolean faceDuplicateFound;       // NULLABLE — Khuôn mặt trùng hồ sơ KYC của CCCD khác (MVP: so cosine với embedding đã lưu trong AI Worker — không dùng Vector DB)

    // ===== Điểm rủi ro tổng hợp =====
    private Integer riskScore;                // NULLABLE — Tổng điểm rủi ro 0..∞; mỗi tín hiệu cộng điểm thay vì chặn cứng

    @Column(columnDefinition = "TEXT")
    private String riskSignalsJson;           // NULLABLE — Mảng JSON các tín hiệu đã cộng điểm, vd [{"code":"FACE_SCORE_BORDERLINE","points":25}]

    @Builder.Default
    @Column(nullable = false)
    private Boolean sampledForReview = false; // true = hồ sơ điểm thấp bị lấy mẫu ngẫu nhiên (~5%) để người duyệt hậu kiểm

    // ===== Metadata =====
    @Column(columnDefinition = "TEXT")
    private String errors;                    // NULLABLE — Mảng JSON chứa thông báo lỗi của từng bước xử lý

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycOverallResult overallStatus;   // Kết luận tổng: PASS / PARTIAL / FAIL (VARCHAR + CHECK constraint trên PostgreSQL 16)

    private OffsetDateTime processedAt;        // NULLABLE — Thời điểm AI xử lý xong

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo bản ghi

    // ===== QUAN HỆ =====
    @OneToOne(optional = false)
    @JoinColumn(name = "vendor_profile_id", insertable = false, updatable = false)
    private VendorProfile vendorProfile;      // Hồ sơ shop tương ứng

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kết quả cần nhân viên duyệt thủ công — UC-VND-03 */
    public boolean requiresManualReview() {
        return this.overallStatus == KycOverallResult.PARTIAL;
    }

    /** AI kết luận đạt — UC-VND-03 */
    public boolean isPassed() {
        return this.overallStatus == KycOverallResult.PASS;
    }

    /** Chốt điểm rủi ro và kết luận: ≥ rejectFrom → FAIL; ≥ autoApproveBelow hoặc bị lấy mẫu → PARTIAL; còn lại PASS (ngưỡng mặc định 30 / 70, cấu hình genie.kyc.risk.*) — UC-VND-03 */
    public KycOverallResult applyRiskScore(int score, String signalsJson, boolean sampled, int autoApproveBelow, int rejectFrom, OffsetDateTime now) {
        this.riskScore = score;
        this.riskSignalsJson = signalsJson;
        this.sampledForReview = sampled && score < autoApproveBelow;
        if (score >= rejectFrom) this.overallStatus = KycOverallResult.FAIL;
        else if (score >= autoApproveBelow || this.sampledForReview) this.overallStatus = KycOverallResult.PARTIAL;
        else this.overallStatus = KycOverallResult.PASS;
        this.processedAt = now;
        return this.overallStatus;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Kiến trúc AI Worker bất đồng bộ**
Bảng này được ghi bởi **một service Python riêng** (`server-python/`), không phải bởi backend Spring Boot. Luồng:

```
Vendor upload ảnh → Spring Boot API lưu URL vào vendor_profiles, set kycStatus = PENDING
                  → Spring Boot gọi bất đồng bộ (@Async + HTTP) sang AI Worker (kycStatus = AI_PROCESSING)
                  → Python Worker tải ảnh qua Presigned URL, giải mã QR, đọc MRZ, so khớp khuôn mặt
                  → Python gọi callback về Spring Boot → ghi kyc_results, cập nhật kycStatus (VERIFIED / MANUAL_REVIEW / REJECTED)
```

> **Vì sao phải tách?** Pipeline cũ (OCR toàn ảnh + DeepFace) mất 5–30 giây và ngốn CPU/GPU; pipeline theo tầng chỉ còn ~1–2 giây nhưng vẫn để bất đồng bộ vì có thể phải chờ người duyệt. Nếu xử lý đồng bộ trong request HTTP, người dùng sẽ phải chờ, kết nối dễ timeout, và một vài request nặng có thể làm sập cả API server. Đây là mô hình **Message Queue + Worker**.

**b) Vì sao các trường `extracted*` lưu `String` chứ không phải kiểu đúng (LocalDate, Integer)?**
Dù lấy dữ liệu chính từ QR (không có lỗi OCR), MRZ vẫn là OCR và thẻ đời cũ phải nhập tay — dữ liệu vẫn **có xác suất sai**. Nó có thể đọc ra `"01/13/2000"` (tháng 13 — vô nghĩa) hoặc `"O1-O1-2OOO"` (nhầm số 0 thành chữ O). Nếu ép kiểu `LocalDate` ngay khi ghi, việc ghi sẽ **ném exception và mất luôn dữ liệu thô** — trong khi staff nhìn vào dữ liệu thô đó vẫn có thể tự sửa tay. Nguyên tắc: **lưu nguyên dữ liệu thô, ép kiểu ở tầng nghiệp vụ**.

**c) Pipeline theo tầng — tầng rẻ chạy trước, tầng nặng chỉ chạy khi cần**

| Tầng | Kiểm tra | Chi phí |
|:---:|---|---|
| 0 | Giải mã QR mặt trước; đọc MRZ mặt sau + check digit; QR khớp MRZ; số CCCD khớp ngày sinh & giới tính; hạn thẻ khớp quy tắc tuổi; trùng CCCD | vài chục ms |
| 1 | Khớp khuôn mặt (selfie ↔ ảnh chân dung trên thẻ) + liveness + trùng khuôn mặt | ~1 giây |
| 2 | Tên chủ tài khoản ngân hàng khớp họ tên KYC (`VendorProfile.verifyPayoutAccount()`); MST với hộ KD/doanh nghiệp | gọi API ngoài, **trước lần rút tiền đầu tiên** |
| 3 | Người duyệt: chỉ hồ sơ điểm trung bình hoặc bị lấy mẫu ngẫu nhiên | tốn người |

Mỗi tín hiệu **cộng điểm** vào `riskScore` thay vì chặn cứng (bảng điểm mặc định, cấu hình được):

| Tín hiệu (`code`) | Điểm |
|---|--:|
| `QR_MRZ_MISMATCH` — QR và MRZ lệch số thẻ / ngày sinh | +60 |
| `ID_STRUCTURE_MISMATCH` — số CCCD không khớp ngày sinh / giới tính | +40 |
| `EXPIRY_RULE_MISMATCH` — hạn thẻ sai quy tắc tuổi | +30 |
| `DUPLICATE_ID` — CCCD đã gắn shop khác đang hoạt động | +50 |
| `FACE_DUPLICATE` — khuôn mặt trùng hồ sơ CCCD khác | +70 |
| `FACE_SCORE_BORDERLINE` — điểm khớp mặt 0,60–0,85 | +25 |
| `FACE_SCORE_LOW` — điểm khớp mặt < 0,60 | +60 |
| `LIVENESS_SUSPECT` — nghi ảnh in / chụp màn hình | +40 |
| `LEGACY_CARD` — thẻ không có QR/MRZ (CMND, CCCD mã vạch) | +30 |
| `DECLARED_DATA_MISMATCH` — thông tin shop tự khai lệch QR | +20 |
| `UNREADABLE_IMAGE` — không đọc được QR/MRZ sau khi đã chụp lại (chuyển người duyệt) | +30 |

**Lỗi đọc không phải bằng chứng gian lận:** QR không giải mã được hoặc MRZ sai check digit thường do ảnh mờ, lóa → client kiểm tra và cho **chụp lại ngay** trước khi nộp; nếu lên tới server vẫn không đọc được thì chỉ gắn `UNREADABLE_IMAGE` (+30) để chuyển người duyệt yêu cầu bổ sung (`requestSupplement()`). Chỉ khi đọc rõ mà dữ liệu **mâu thuẫn** mới là tín hiệu gian lận.

**d) Ba vùng kết luận (`applyRiskScore()`)**
- `riskScore < 30` → `PASS`, tự duyệt (`approveKyc(…, OCR_AUTO)`); ~5% bị lấy mẫu ngẫu nhiên thành `PARTIAL` để hậu kiểm.
- `30 ≤ riskScore < 70` → `PARTIAL`, sang hàng đợi người duyệt (UC-VND-04).
- `riskScore ≥ 70` → `FAIL`, từ chối kèm lý do, cho nộp lại.

**e) Giới hạn phải nói rõ khi bảo vệ đồ án**
Thuật toán tạo QR và MRZ là công khai: một thẻ giả in dữ liệu nhất quán với **chính khuôn mặt kẻ gian** vẫn qua Tầng 0–1. Chỉ đọc chữ ký số trong chip NFC hoặc xác thực qua C06 / VNeID mới chặn được triệt để (cần app native hoặc đơn vị được cấp phép — ngoài phạm vi đồ án). Rủi ro còn lại được khống chế bằng **Tầng 2**: tiền chỉ được rút về tài khoản ngân hàng đứng đúng tên trên CCCD, mà ngân hàng đã xác thực sinh trắc chủ tài khoản. Embedding khuôn mặt (`kyc_faces`) là dữ liệu sinh trắc học: chỉ xử lý khi người bán tick đồng ý ở bước nộp hồ sơ (UC-VND-02), lưu trong AI Worker, xoá khi hồ sơ bị từ chối quá 30 ngày.

---

## 1.7. `RefreshToken` — Phiên đăng nhập
Bảng: `refresh_tokens`

```java
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — token này của ai

    @Column(unique = true, nullable = false, length = 255)
    private String tokenHash;                 // BĂM của refresh token (KHÔNG lưu token gốc!)

    @Column(nullable = false, length = 255)
    private String deviceInfo;                // Thông tin thiết bị (User-Agent) — để user nhận ra phiên nào là của mình

    @Column(nullable = false, length = 45)
    private String ipAddress;                 // IP lúc đăng nhập — độ dài 45 để chứa được cả IPv6

    @Column(nullable = false)
    private OffsetDateTime expiresAt;          // Thời điểm token hết hạn (chuẩn dự án: 7 ngày, xoay vòng mỗi lần làm mới)

    private OffsetDateTime revokedAt;          // NULLABLE — Thời điểm bị thu hồi. NULL = còn hiệu lực

    @Column(length = 100)
    private String revokedReason;             // NULLABLE — Lý do thu hồi (hằng RevokeReason trong code): "USER_LOGOUT", "PASSWORD_CHANGED", "ROTATED", "REUSE_DETECTED", "SUSPENDED", "ACCOUNT_DELETED", "ADMIN_REVOKED"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm đăng nhập tạo ra phiên này

    // ---- Bổ sung bằng V14__auth_additions.sql (plan AUTH D6) — màn "Thiết bị đăng nhập" ----
    @Column(nullable = false)
    private Long familyId;                    // Mã phiên ổn định qua các lần xoay vòng (= id của token gốc; sequence refresh_token_family_seq); = `id` trả ở GET /me/sessions, = claim `sid` của JWT

    private OffsetDateTime lastUsedAt;         // NULLABLE — Lần làm mới gần nhất (hiển thị "Hoạt động lần cuối"); NULL = chưa làm mới lần nào (dùng createdAt)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Token còn dùng được (chưa thu hồi, chưa hết hạn 7 ngày) — UC-AUTH-02 */
    public boolean isUsable(OffsetDateTime now) {
        return this.revokedAt == null && this.expiresAt.isAfter(now);
    }

    /** Thu hồi khi xoay vòng / đăng xuất / phát hiện dùng lại — UC-AUTH-11 */
    public void revoke(String reason, OffsetDateTime now) {
        this.revokedAt = now;
        this.revokedReason = reason;
    }

    // ---- Bổ sung khi hiện thực AUTH (plan A03.05) ----

    /** Ghi lần làm mới gần nhất — UC-AUTH-02/11 */
    public void touch(OffsetDateTime now) { this.lastUsedAt = now; }

    /** Factory: cắt deviceInfo còn 255 ký tự, ip thiếu → "0.0.0.0"; token gốc của phiên lưu xong thì gán familyId = id — UC-AUTH-02 */
    public static RefreshToken issue(Long userId, Long familyId, String tokenHash, String deviceInfo, String ip, OffsetDateTime expiresAt) { /* … */ }
}
```

> **Bổ sung sau Phase 12 (`V14`):** cột `family_id BIGINT NOT NULL` (thêm NULL → điền `family_id = id` → `SET NOT NULL`), `last_used_at TIMESTAMPTZ`; index `idx_refresh_tokens_user_active (user_id) WHERE revoked_at IS NULL`, `idx_refresh_tokens_family (family_id)`, `idx_refresh_tokens_user_created (user_id, created_at DESC)`. Tên / loại thiết bị suy từ `deviceInfo` lúc đọc; `location` trả `null` (chưa có GeoIP — DATN). Job dọn token hết hạn / đã thu hồi chạy 03:00 hằng ngày, giữ 90 ngày.

### 💡 Kiến thức chuyên sâu

**a) Cặp Access Token / Refresh Token — vì sao cần cả hai?**

| | Access Token (JWT) | Refresh Token |
|---|---|---|
| Sống bao lâu | Ngắn: ~15 phút | Dài: 7–30 ngày |
| Lưu ở đâu | Chỉ ở client, **KHÔNG lưu DB** | Băm rồi lưu DB (bảng này) |
| Dùng làm gì | Gửi kèm mọi request API | Chỉ dùng để xin Access Token mới |

> **Nghịch lý cần giải:** JWT không thể thu hồi (server không lưu nên không biết token nào đã phát ra). Nếu cho JWT sống 30 ngày, user bị hack thì kẻ gian dùng được 30 ngày. Nếu cho sống 15 phút, user phải đăng nhập lại 4 lần/giờ — không chấp nhận được.
> **Lời giải:** Access Token sống 15 phút (thiệt hại tối đa nếu lộ = 15 phút). Refresh Token sống dài nhưng **có trong DB nên thu hồi được ngay lập tức** bằng cách set `revokedAt`.

**b) Vì sao lưu `tokenHash` chứ không lưu token gốc?**
Áp dụng đúng nguyên tắc như lưu mật khẩu. Nếu hacker dump được database, với `tokenHash` họ **không thể** khôi phục token gốc để mạo danh user. Khi client gửi token lên, server băm lại rồi so sánh hash.

**c) `deviceInfo` + `ipAddress` = tính năng "Quản lý thiết bị đăng nhập"**
Cho phép dựng màn hình kiểu: *"iPhone 13 – Hà Nội – 2 giờ trước"* kèm nút **Đăng xuất**. Đây là tính năng bảo mật tiêu chuẩn của mọi nền tảng lớn.

**d) `revokedReason` phục vụ điều tra sự cố**
Khi `PASSWORD_CHANGED`, hệ thống nên thu hồi **toàn bộ** refresh token của user đó — vì đổi mật khẩu thường xảy ra sau khi nghi ngờ bị hack, và phải đá kẻ gian ra khỏi mọi thiết bị.

**e) Vì sao `ipAddress` dài 45 ký tự?**
IPv4 tối đa 15 ký tự (`255.255.255.255`), nhưng IPv6 dạng đầy đủ kèm phần IPv4-mapped cần đúng 45 ký tự. Đây là con số chuẩn công nghiệp.

---

## 1.8. `StaffPermission` — Phân quyền chi tiết cho nhân viên
Bảng: `staff_permissions`

```java
@Entity
@Table(name = "staff_permissions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class StaffPermission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long staffUserId;                 // FK → users.id — nhân viên được cấp quyền

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffPermissionEnum permission;   // 13 quyền TLCN: VIEW_DASHBOARD, REVIEW_KYC, MANAGE_CATALOG, MODERATE_REVIEW, HANDLE_DISPUTE, PROCESS_PAYOUT, UPDATE_COMMISSION, VIEW_FINANCE, MANAGE_VOUCHER, MANAGE_PLATFORM_NOTIFICATION, VIEW_AUDIT_LOG, MANAGE_USER, MANAGE_RBAC

    @Column(nullable = false)
    private Long grantedByAdminId;            // FK → users.id — Admin nào đã cấp quyền này

    @Column(nullable = false)
    private OffsetDateTime grantedAt;          // Thời điểm cấp quyền

    private OffsetDateTime revokedAt;          // NULLABLE — Thời điểm thu hồi. NULL = quyền vẫn còn hiệu lực
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Quyền còn hiệu lực — UC-ADM-02 */
    public boolean isActive() {
        return this.revokedAt == null;
    }

    /** Thu hồi quyền — UC-ADM-02 */
    public void revoke(OffsetDateTime now) {
        this.revokedAt = now;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) RBAC 2 tầng: Role thô + Permission mịn**
Dự án dùng mô hình lai:
- **Tầng 1 — Role** (`User.role`): phân loại lớn CUSTOMER / VENDOR / STAFF / ADMIN. Dùng để chặn ở tầng router.
- **Tầng 2 — Permission** (bảng này): chỉ áp dụng cho STAFF, phân quyền chi tiết theo từng hành động.

> Vì sao không gộp làm một? Nếu chỉ có Role, mỗi khi nghiệp vụ cần một tổ hợp quyền mới lại phải đẻ ra 1 role mới (`STAFF_KYC`, `STAFF_KYC_AND_PAYOUT`...) → bùng nổ tổ hợp. Tách Permission ra cho phép **ghép quyền tự do**: 1 staff có thể vừa `REVIEW_KYC` vừa `PROCESS_PAYOUT` mà không cần định nghĩa role mới.

**b) `revokedAt` thay cho việc DELETE dòng — mẫu "Temporal Table"**
Khi thu hồi quyền, hệ thống **không xoá dòng** mà chỉ set `revokedAt = now()`. Nhờ vậy 6 tháng sau, khi điều tra "ai đã duyệt hồ sơ KYC gian lận ngày X", ta vẫn trả lời được: staff đó **từng có** quyền đó trong khoảng thời gian nào.
Câu truy vấn kiểm tra quyền vì thế luôn là:

```sql
SELECT 1 FROM staff_permissions
WHERE staff_user_id = ? AND permission = ? AND revoked_at IS NULL
```

**c) Quyền `MANAGE_RBAC` — quyền nguy hiểm nhất**
Đây là quyền cho phép cấp quyền cho người khác. Nếu để lọt, một staff có thể tự nâng mình lên toàn quyền. Trong thực tế, quyền này nên **chỉ ADMIN có**, và mọi thao tác dùng nó bắt buộc phải ghi vào `SystemLog` (`PERMISSION_GRANTED` / `PERMISSION_REVOKED`).

**d) Tách biệt nhiệm vụ (Segregation of Duties — v2.9)**
Service cấp quyền (UC-ADM-02) từ chối 3 trường hợp, trả mã `GNE-SEC-4031`:
1. **Cặp xung đột trên cùng một người:** `PROCESS_PAYOUT` + `UPDATE_COMMISSION` (người chi tiền không được tự sửa tỷ lệ ăn chia). Lưu ý: bộ quyền mặc định của `FINANCE_OFFICER` có cả hai quyền — Admin phải bỏ một trong hai khi giao cho cùng một người nếu áp quy tắc này nghiêm ngặt; TLCN mặc định chỉ **cảnh báo** (ghi `SystemLog`), KLTN chặn hẳn.
2. **Tự cấp quyền cho chính mình** (`staffUserId == grantedByAdminId`).
3. **Cấp `MANAGE_RBAC`, `UPDATE_COMMISSION` bởi người không phải ADMIN.**

**e) Ba chức danh nhân viên của TLCN (không có Super Admin)**
Vai trò cao nhất là `ADMIN`. Nhân viên (`STAFF`) mang một trong 3 chức danh `AdminProfile.staffTitle` — chức danh chỉ là **mẫu** để nạp quyền mặc định khi mời (UC-ADM-01), quyền thật nằm ở bảng này và Admin chỉnh từng quyền được (UC-ADM-02):

| `StaffTitle` | Quyền mặc định |
|---|---|
| `MODERATOR` (kiểm duyệt) | `VIEW_DASHBOARD`, `REVIEW_KYC`, `MANAGE_CATALOG`, `MODERATE_REVIEW` |
| `CS_SPECIALIST` (CSKH) | `VIEW_DASHBOARD`, `HANDLE_DISPUTE`, `MODERATE_REVIEW` |
| `FINANCE_OFFICER` (tài chính) | `VIEW_DASHBOARD`, `PROCESS_PAYOUT`, `UPDATE_COMMISSION`, `VIEW_FINANCE`, `MANAGE_VOUCHER` |
| `ADMIN` (vai trò, không phải chức danh) | Toàn bộ 13 quyền; riêng `MANAGE_RBAC` không cấp được cho STAFF |

---

# 2. NHÓM DANH MỤC & SẢN PHẨM

## 2.1. `Category` — Danh mục ngành hàng
Bảng: `categories`

```java
@Entity
@Table(name = "categories")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Category {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, length = 255)
    private String name;                      // Tên danh mục hiển thị: "Điện thoại", "Thời trang nam"

    @Column(length = 255)
    private String slug;                      // NULLABLE — Chuỗi thân thiện URL: "dien-thoai" → /danh-muc/dien-thoai

    private Long parentId;                    // NULLABLE — FK → chính bảng này. NULL = danh mục gốc (cấp 1)

    private Integer displayOrder;             // NULLABLE — Thứ tự sắp xếp khi hiển thị (số nhỏ lên trước)

    @Column(length = 50)
    private String icon;                      // NULLABLE — Tên/mã icon hiển thị trên giao diện

    @Column(length = 500)
    private String description;               // NULLABLE — Mô tả ngắn hiển thị ở đầu trang danh mục — UC-CAT-01 (TLCN v1.3)

    @Column(precision = 5, scale = 4)
    private BigDecimal commissionRate;        // NULLABLE — Hoa hồng mặc định theo ngành hàng, cùng kiểu với VendorProfile/OrderItem (vd 0.0350 = 3.5%, khung 0.0350–0.0800)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;          // true = đang hiển thị; false = ẩn khỏi menu nhưng giữ dữ liệu

    @Builder.Default
    @Column(nullable = false)
    private Boolean requiresBusinessLicense = false; // true = ngành hàng có điều kiện (thực phẩm, mỹ phẩm, thiết bị điện…) — sản phẩm phải kèm giấy tờ chứng thực (product_documents) mới được gửi duyệt

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua Category.parentId
    private List<Category> subCategories;     // Các danh mục con trực tiếp (đệ quy)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Danh mục gốc — UC-CAT-01 */
    public boolean isRoot() {
        return this.parentId == null;
    }

    /** Đổi tỷ lệ hoa hồng ngành (chỉ áp dụng đơn mới) — UC-CAT-14 */
    public void changeCommissionRate(BigDecimal rate) {
        if (rate.compareTo(new BigDecimal("0.0350")) < 0 || rate.compareTo(new BigDecimal("0.0800")) > 0)
            throw new IllegalArgumentException("Hoa hồng phải trong khung 3.5% – 8%");
        this.commissionRate = rate;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Adjacency List — mô hình cây bằng `parentId`**
Đây là cách biểu diễn cây đơn giản nhất: mỗi node chỉ nhớ cha của nó. Quan hệ `Category → Category` là **self-referencing** (tự tham chiếu) — trong class diagram vẽ là mũi tên trỏ ngược vào chính class đó.

| Ưu điểm | Nhược điểm |
|---|---|
| Thêm/sửa/xoá 1 node cực nhanh | Lấy **toàn bộ** cây con phải đệ quy nhiều query (N+1) |
| Cấu trúc dễ hiểu, dễ bảo trì | Với cây sâu, query chậm |

> **Cách khắc phục trong thực tế:** PostgreSQL (từ 8.4, đầy đủ trên bản 16) hỗ trợ `WITH RECURSIVE` (CTE đệ quy) để lấy cả cây trong 1 query. Hoặc cache toàn bộ cây danh mục vào Redis — vì danh mục rất hiếm khi thay đổi mà lại được đọc ở mọi trang.

**b) `slug` — SEO cho thương mại điện tử**
`/category/15` thì máy tìm kiếm không hiểu gì. `/category/dien-thoai-smartphone` chứa từ khoá → Google xếp hạng cao hơn. Với sàn TMĐT, traffic từ Google là nguồn khách miễn phí lớn nhất, nên `slug` không phải chi tiết nhỏ mà là yếu tố kinh doanh.

**c) `commissionRate` ở Category — cơ chế 2 tầng với VendorProfile**
Sàn có 2 nơi định nghĩa hoa hồng:
- `Category.commissionRate` — mức **mặc định theo ngành hàng** (đồ điện tử biên lợi nhuận thấp → hoa hồng thấp; mỹ phẩm biên cao → hoa hồng cao).
- `VendorProfile.commissionRate` — mức **riêng cho từng shop**, ghi đè mức ngành hàng.

Thứ tự áp dụng: mức riêng của shop → nếu không có thì mức của ngành hàng → nếu không có nữa thì mức mặc định toàn sàn.

**d) `isActive` thay vì xoá**
Xoá 1 danh mục sẽ làm mồ côi hàng nghìn sản phẩm đang trỏ vào nó. `isActive = false` chỉ ẩn khỏi menu, dữ liệu cũ vẫn nguyên vẹn.

---

## 2.2. `Product` — Sản phẩm (SPU)
Bảng: `products`

```java
@Entity
@Table(name = "products")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id (của vendor) — sản phẩm này thuộc shop nào

    private Long categoryId;                  // NULLABLE — FK → categories.id, danh mục của sản phẩm

    @Column(nullable = false, length = 255)
    private String name;                      // Tên sản phẩm hiển thị

    @Column(columnDefinition = "TEXT")
    private String description;               // NULLABLE — Mô tả chi tiết, có thể chứa HTML

    @Column(length = 255)
    private String brand;                     // NULLABLE — Thương hiệu: "Samsung", "Nike"

    @Column(length = 50)
    private String condition;                 // NULLABLE — Tình trạng: "NEW" (mới) / "USED" (đã qua sử dụng)

    @Column(length = 100)
    private String origin;                    // NULLABLE — Xuất xứ: "Việt Nam", "Trung Quốc"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String specsJson;                 // NULLABLE — Mảng JSON chứa thông số kỹ thuật (RAM, màn hình, chất liệu...)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String categoryJson;              // NULLABLE — Mảng JSON lưu ĐƯỜNG DẪN danh mục đầy đủ (breadcrumb)

    // ---- Thông tin phục vụ vận chuyển ----
    private Double weight;                    // NULLABLE — Khối lượng (gram) — bắt buộc để GHN tính cước

    private Double length;                    // NULLABLE — Chiều dài kiện hàng (cm)

    private Double width;                     // NULLABLE — Chiều rộng kiện hàng (cm)

    private Double height;                    // NULLABLE — Chiều cao kiện hàng (cm)

    private String shipsFrom;                 // NULLABLE — Nơi gửi hàng hiển thị cho khách

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ShippingPayer shippingPayer;      // NULLABLE — Ai trả phí ship: BUYER (khách) / SELLER (shop)

    @Column(precision = 15, scale = 2)
    private BigDecimal shippingFeeSupport;    // NULLABLE — Số tiền shop hỗ trợ phí ship (chuẩn hóa DECIMAL thay cho Double theo database-schema-designer)

    // ---- Giá & trạng thái ----
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice;             // Giá gốc/giá hiển thị của sản phẩm (giá thật nằm ở từng variant)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;             // DRAFT / PENDING_APPROVAL / PUBLISHED / HIDDEN / REJECTED / FLAGGED / DELETED

    @Column(columnDefinition = "TEXT")
    private String moderationReason;          // NULLABLE — Lý do bị từ chối/gắn cờ, để vendor biết đường sửa

    private Long moderatorId;                 // NULLABLE — FK mềm → users.id, staff được phân công duyệt sản phẩm này

    private Boolean isPublished;              // NULLABLE — Cờ phụ đánh dấu đã từng được đăng bán

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống ghi đè khi vendor và staff cùng sửa

    @Column(length = 255)
    private String variantImageAttribute;     // NULLABLE — Tên thuộc tính quyết định ảnh biến thể, VD "Màu sắc"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo sản phẩm

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    // ---- Trường TÍNH TOÁN, KHÔNG lưu trong DB ----
    @Transient
    private Integer sold;                     // Số lượng đã bán — tính từ bảng order_items

    @Transient
    private Double rating;                    // Điểm đánh giá trung bình — tính từ bảng product_reviews

    // ---- Thông tin bổ sung theo form đăng bán (đối chiếu giao diện 2026-10-29) ----
    private Long brandId;                     // NULLABLE — FK → brands.id; NULL = "No Brand" (UC-CAT-09)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private ProductCondition itemCondition = ProductCondition.NEW; // Tình trạng: NEW / USED / REFURBISHED

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPreOrder = false;       // Sản phẩm đặt trước (chỉ khi VendorProfile.preOrderEnabled)

    private Integer preOrderDays;             // NULLABLE — Số ngày chuẩn bị hàng của sản phẩm đặt trước (7–30); NULL khi không đặt trước

    @Builder.Default
    @Column(nullable = false)
    private Long viewCount = 0L;              // Lượt xem tích lũy (flush từ Redis product:{id}:views; chi tiết theo ngày ở product_view_stats)


    // ===== QUAN HỆ =====
    @ManyToOne
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;                // NULLABLE — Danh mục chứa sản phẩm (theo categoryId)

    @OneToMany(mappedBy = "product")
    private List<ProductVariant> variants;    // Các biến thể (SKU) — nơi chứa giá và tồn kho thật

    @OneToMany
    @JoinColumn(name = "product_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua ProductImage.productId
    private List<ProductImage> images;        // Bộ ảnh của sản phẩm

    @ManyToOne(optional = false)
    @JoinColumn(name = "vendor_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private VendorProfile vendorProfile;      // Hồ sơ shop bán sản phẩm này
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Gửi duyệt sản phẩm (shop đã cấu hình kho lấy hàng — Service kiểm tra isPickupConfigured()) — UC-CAT-09 */
    public void submitForReview() {
        if (this.status != ProductStatus.DRAFT && this.status != ProductStatus.REJECTED)
            throw new IllegalStateException("Chỉ gửi duyệt sản phẩm ở trạng thái DRAFT / REJECTED");
        this.status = ProductStatus.PENDING_APPROVAL;
    }

    /** Duyệt và đăng bán — UC-CAT-13 */
    public void approve(Long moderatorId) {
        this.status = ProductStatus.PUBLISHED;
        this.isPublished = true;
        this.moderatorId = moderatorId;
        this.moderationReason = null;
    }

    /** Từ chối kèm lý do — UC-CAT-13 */
    public void reject(Long moderatorId, String reason) {
        this.status = ProductStatus.REJECTED;
        this.isPublished = false;
        this.moderatorId = moderatorId;
        this.moderationReason = reason;
    }

    /** Shop tự ẩn sản phẩm — UC-CAT-11 */
    public void hide() {
        this.status = ProductStatus.HIDDEN;
        this.isPublished = false;
    }

    /** Bật đặt trước: shop phải cho phép và số ngày chuẩn bị 7–30 — UC-CAT-09 */
    public void markPreOrder(int days, boolean shopAllows) {
        if (!shopAllows) throw new IllegalStateException("Shop chưa bật chế độ đặt trước");
        if (days < 7 || days > 30) throw new IllegalArgumentException("Số ngày chuẩn bị phải từ 7 đến 30");
        this.isPreOrder = true;
        this.preOrderDays = days;
    }


    /** Kiểm duyệt tự động đánh dấu nghi vấn → chờ Staff duyệt thủ công — UC-CAT-12 */
    public void flag(String reason) {
        this.status = ProductStatus.FLAGGED;
        this.isPublished = false;
        this.moderationReason = reason;
    }

}
```

### 💡 Kiến thức chuyên sâu

**a) SPU vs SKU — tách `Product` và `ProductVariant`**
- **SPU (Standard Product Unit)** = `Product`: khái niệm sản phẩm chung. VD *"Áo thun Uniqlo AIRism"*.
- **SKU (Stock Keeping Unit)** = `ProductVariant`: đơn vị thật sự có kho và có giá. VD *"Áo thun Uniqlo AIRism – Đen – Size L"*.

> Vì sao bắt buộc phải tách? Vì tồn kho và giá **luôn thuộc về biến thể**, không thuộc về sản phẩm. Size S còn 10 cái nhưng size XL đã hết. Nếu nhập chung 1 bảng, bạn không thể trả lời câu hỏi "size XL còn hàng không?". Mọi sàn TMĐT lớn đều dùng mô hình này.

**b) `@Transient` — trường ảo không có cột trong DB**
`sold` và `rating` được đánh dấu `@Transient` trong JPA (không lưu thành cột vật lý trong DB), được service tính toán động rồi gán vào object trước khi trả về API.

> **Vì sao không lưu thẳng vào cột?** Vì đó là **dữ liệu dẫn xuất** (derived data) — có thể tính lại bất cứ lúc nào từ `order_items` và `product_reviews`. Lưu cứng sẽ sinh rủi ro **dữ liệu lệch nhau** (denormalization drift): khi 1 review bị ẩn, cột `rating` không tự cập nhật → hiển thị sai vĩnh viễn.
> **Đánh đổi:** tính lại mỗi lần đọc thì chậm. Giải pháp thực tế là cache vào Redis với TTL ngắn, hoặc dùng bảng tổng hợp cập nhật định kỳ.

> **Đặc tả triển khai:**
> - **Redis Hash** `product:stats:{productId}` gồm `sold`, `ratingSum`, `ratingCount` (rating = ratingSum / ratingCount, làm tròn 1 chữ số). Không đặt TTL (dữ liệu nóng, cập nhật theo sự kiện).
> - **Cập nhật theo sự kiện nội bộ** (Spring `@TransactionalEventListener(AFTER_COMMIT)`, cùng tiến trình — MVP không dùng message broker): `OrderCompleted` → `HINCRBY sold +qty`; `OrderRefunded` (trả hàng) → `HINCRBY sold −qty`; `ReviewCreated` / `ReviewHidden` → `HINCRBY ratingSum ±stars`, `ratingCount ±1`.
> - **Đối soát hằng đêm** (`@Scheduled` 02:00): tính lại từ `order_items` (đơn `COMPLETED`) và `product_reviews` (đang hiển thị), ghi đè Hash và cập nhật cột tổng hợp `products.sold`, `products.rating` (cột đọc dùng cho sắp xếp / lọc trên trang danh sách).
> - **Trang danh sách:** sắp xếp / lọc đọc cột tổng hợp trong PostgreSQL; trang chi tiết gọi Redis `HMGET` để có số liệu mới nhất. Cache miss → tính từ DB rồi ghi lại Hash.

**c) `specsJson` / `categoryJson` — mẫu "JSON Column" cho dữ liệu không đồng nhất (EAV thay thế)**
Điện thoại có thông số RAM/CPU/pin. Áo thun có chất liệu/size. Không thể thiết kế bảng cột cứng cho mọi loại. 3 hướng giải quyết:

| Cách | Mô tả | Nhược điểm |
|---|---|---|
| Nhiều bảng con | Mỗi ngành hàng 1 bảng | Bùng nổ số bảng, khó mở rộng |
| EAV | Bảng key-value (attribute, value) | Query cực phức tạp, chậm |
| **JSON column (dự án dùng)** | Lưu JSON vào cột JSONB | Lọc được bằng toán tử JSONB + GIN index, nhưng không mạnh bằng search engine |

> Dự án chọn JSONB vì đơn giản và linh hoạt. Nhược điểm "khó lọc" được giảm bằng **GIN index** trên cột JSONB (`specs_json @> '{"ram":"8GB"}'`) — đủ cho quy mô MVP; bản đầy đủ chuyển sang Elasticsearch khi cần lọc theo thuộc tính ở quy mô lớn.

**d) `categoryJson` lưu breadcrumb — đánh đổi chuẩn hoá lấy tốc độ**
Thay vì đệ quy ngược lên `categories` để dựng chuỗi *"Điện tử > Điện thoại > Smartphone"*, hệ thống lưu sẵn đường dẫn dạng JSON ngay trong sản phẩm. Đọc nhanh hơn rất nhiều, đổi lại khi danh mục đổi tên phải chạy job cập nhật lại.

**e) Vòng đời `ProductStatus` — 7 trạng thái**

```
DRAFT ──(vendor bấm đăng bán)──► PENDING_APPROVAL
PENDING_APPROVAL ──(staff duyệt)──► PUBLISHED
PENDING_APPROVAL ──(staff từ chối)──► REJECTED ──(vendor sửa)──► PENDING_APPROVAL
PUBLISHED ──(vendor tạm ẩn)──► HIDDEN ──(bật lại)──► PUBLISHED
PUBLISHED ──(khách báo cáo vi phạm)──► FLAGGED ──(staff xử lý)──► PUBLISHED | DELETED
bất kỳ ──► DELETED (xoá mềm)
```

Điểm tinh tế: `HIDDEN` do **vendor** chủ động, `FLAGGED` do **sàn** cưỡng chế. Tách 2 trạng thái để vendor không thể tự gỡ cờ vi phạm bằng cách bật/tắt hiển thị.

**f) `variantImageAttribute` — giải bài toán ảnh theo biến thể**
Sản phẩm có thể có 2 chiều biến thể: Màu (Đen/Trắng) × Size (S/M/L) = 6 SKU. Nhưng ảnh chỉ khác nhau theo **Màu**, không theo Size. Nếu gắn ảnh cho từng SKU thì phải upload trùng lặp 6 lần.
Trường này lưu tên thuộc tính quyết định ảnh (VD `"Màu sắc"`). Khi khách chọn "Đen", giao diện đổi ảnh; khi chọn size, ảnh giữ nguyên. Đây chính xác là cách Shopee/Lazada hoạt động.

---

## 2.3. `ProductVariant` — Biến thể sản phẩm (SKU)
Bảng: `product_variants`

```java
@Entity
@Table(name = "product_variants")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductVariant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productId;                   // FK → products.id — biến thể này thuộc sản phẩm nào

    @Column(unique = true, nullable = false, length = 100)
    private String sku;                       // Mã SKU duy nhất toàn hệ thống — mã kho của đơn vị bán

    @Column(length = 255)
    private String variantName;               // NULLABLE — Tên hiển thị của biến thể: "Đen - Size L"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String attributesJson;            // NULLABLE — JSON thuộc tính: {"Màu":"Đen","Size":"L"}

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;                 // GIÁ BÁN THẬT của biến thể này

    @Column(nullable = false)
    private Integer stockCount;               // Tổng tồn kho vật lý đang có

    @Builder.Default
    @Column(nullable = false)
    private Integer reservedCount = 0;        // Số lượng đang bị GIỮ CHỖ cho các đơn chưa thanh toán xong

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — lá chắn chống bán vượt tồn kho (overselling)

    @Column(length = 500)
    private String imageUrl;                  // NULLABLE — Ảnh riêng của biến thể này

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;          // NULLABLE — Xoá mềm (@SQLDelete + @SQLRestriction). NULL = chưa xoá

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm cha
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tồn khả dụng = tồn vật lý − đang giữ chỗ — UC-CAT-06 */
    public int availableStock() {
        return Math.max(0, this.stockCount - this.reservedCount);
    }

    /** Giữ chỗ mềm (service dùng UPDATE có điều kiện) — UC-ORD-07 */
    public void reserve(int qty) {
        if (availableStock() < qty) throw new IllegalStateException("GNE-INV-4090");   // hết tồn kho
        this.reservedCount += qty;
    }

    /** Nhả giữ chỗ (chỉ gọi sau khi StockReservation.release() thành công) — UC-ORD-08 */
    public void releaseReservation(int qty) {
        if (this.reservedCount < qty) throw new IllegalStateException("reservedCount không đủ để nhả — giữ chỗ đã được xử lý");
        this.reservedCount -= qty;
    }

    /** Trừ tồn cứng khi PAID — chỉ gọi sau StockReservation.confirm() (ACTIVE) để chống đua với Sweeper — UC-PAY-03 */
    public void commitSale(int qty) {
        if (this.reservedCount < qty || this.stockCount < qty) throw new IllegalStateException("GNE-INV-4093");
        this.reservedCount -= qty;
        this.stockCount -= qty;
    }

    /** Xóa mềm biến thể: chặn khi còn giữ chỗ của đơn đang xử lý; còn tồn thì chỉ Admin được ép xóa (force) — UC-CAT-11 */
    public void softDelete(OffsetDateTime now, boolean force) {
        if (this.reservedCount > 0) throw new IllegalStateException("Biến thể đang được giữ chỗ cho " + this.reservedCount + " sản phẩm trong đơn chưa thanh toán");
        if (this.stockCount > 0 && !force) throw new IllegalStateException("Còn " + this.stockCount + " tồn kho — đưa tồn về 0 hoặc Admin xóa cưỡng bức");
        this.deletedAt = now;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) `stockCount` vs `reservedCount` — trái tim chống overselling**
Công thức cốt lõi của toàn sàn:

```
Số lượng KHÁCH CÓ THỂ MUA = stockCount - reservedCount
```

Vì sao cần 2 cột thay vì trừ thẳng `stockCount`? Hãy xét luồng thực tế:

```
1. Khách bấm "Đặt hàng"  → reservedCount += 1   (kho vật lý CHƯA đổi, hàng vẫn nằm ở shop)
2a. Khách trả tiền thành công → stockCount -= 1; reservedCount -= 1  (xuất kho thật)
2b. Khách bỏ ngang / hết 15 phút → reservedCount -= 1  (trả chỗ lại, KHÔNG ai mất hàng)
```

> Nếu trừ thẳng `stockCount` ngay bước 1, khi khách bỏ giỏ hàng bạn phải "cộng lại" — và nếu tiến trình chết giữa chừng, kho sẽ **vĩnh viễn thiếu hụt** mà không ai biết. Tách 2 cột giúp phân biệt rõ "hàng thật trong kho" và "hàng đang được giữ tạm", có thể đối soát lại bất cứ lúc nào bằng bảng `stock_reservations`.

**b) Kịch bản overselling và cách `@Version` chặn nó**
Sản phẩm còn đúng 1 cái. Hai khách bấm mua **cùng lúc**:

```
        Khách A                         Khách B
t1: đọc stock=1, version=5       t1: đọc stock=1, version=5
t2: UPDATE ... WHERE id=9 AND version=5   →  thành công, version thành 6
t3:                              t3: UPDATE ... WHERE id=9 AND version=5  →  0 rows!
                                     → OptimisticLockException → báo "Sản phẩm vừa hết hàng"
```

Không có `version`, cả 2 đều UPDATE thành công → bán 2 cái trong khi chỉ có 1 → sàn phải đền khách.

**c) `sku` UNIQUE toàn hệ thống**
SKU là mã định danh vật lý của hàng hoá, dùng khi in tem, quét mã trong kho, đối soát với đơn vị vận chuyển. Nếu 2 shop khác nhau có cùng SKU, kho tổng sẽ không phân biệt được hàng của ai.

**d) `deletedAt` — Soft Delete kiểu timestamp**
Khác với `Address.isDeleted` (kiểu boolean), ở đây dùng timestamp: vừa biết **đã xoá hay chưa** (NULL/không NULL), vừa biết **xoá lúc nào**. Trong JPA/Hibernate (Spring Boot 3.3.4), entity áp dụng `@SQLDelete(sql = "UPDATE ... SET deleted_at = NOW() WHERE id = ?")` kết hợp `@SQLRestriction("deleted_at IS NULL")`.
> Bắt buộc phải xoá mềm ở đây, vì `order_items` trỏ tới `productVariantId`. Xoá cứng sẽ làm gãy toàn bộ lịch sử đơn hàng.

---

## 2.4. `ProductImage` — Ảnh sản phẩm
Bảng: `product_images`

```java
@Entity
@Table(name = "product_images")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productId;                   // FK → products.id — ảnh thuộc sản phẩm nào

    private Long variantId;                   // NULLABLE — FK → product_variants.id. NULL = ảnh trong gallery chung của sản phẩm; khác NULL = ảnh RIÊNG của biến thể (chỉ hiện khi khách chọn biến thể đó, KHÔNG xuất hiện trong gallery chung)

    @Column(nullable = false, length = 500)
    private String imageUrl;                  // Link ảnh gốc trên storage (S3/Silo)

    @Column(length = 500)
    private String cdnUrl;                    // NULLABLE — Link ảnh qua CDN, tải nhanh hơn cho người dùng cuối

    private Integer width;                    // NULLABLE — Chiều rộng ảnh (pixel)

    private Integer height;                   // NULLABLE — Chiều cao ảnh (pixel)

    private Long fileSize;                    // NULLABLE — Dung lượng file (byte)

    @Column(length = 500)
    private String altText;                   // NULLABLE — Văn bản thay thế cho SEO & người khiếm thị

    private Boolean isPrimary;                // NULLABLE — true = ảnh đại diện hiển thị ở danh sách sản phẩm

    @Column(length = 50)
    private String mimeType;                  // NULLABLE — Loại file: "image/jpeg", "image/webp"

    @Column(nullable = false)
    private Integer displayOrder;             // Thứ tự hiển thị trong thư viện ảnh (số nhỏ lên trước)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đặt làm ảnh đại diện — UC-CAT-10 */
    public void markPrimary() {
        this.isPrimary = true;
        this.displayOrder = 0;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) `variantId` nullable — một bảng phục vụ 2 mục đích**
- `variantId = NULL` → ảnh mô tả chung của sản phẩm, hiện trong gallery.
- `variantId` có giá trị → ảnh riêng của biến thể, hiện khi khách chọn "Màu Đen".

Thiết kế này tránh phải tạo 2 bảng `product_images` và `variant_images` gần như giống hệt nhau.

**b) `imageUrl` vs `cdnUrl` — hai tầng lưu trữ**

| | imageUrl (Origin) | cdnUrl (CDN) |
|---|---|---|
| Nơi lưu | S3 / Silo — 1 vị trí duy nhất | Nhân bản ra hàng trăm máy chủ biên |
| Ai dùng | Backend, xử lý ảnh, sao lưu | Trình duyệt của khách |
| Tốc độ | Chậm hơn với khách ở xa | Nhanh, lấy từ máy chủ gần nhất |

Giữ cả 2 để: ảnh vẫn hiển thị được nếu CDN chưa kịp cache hoặc đang sự cố (fallback), và backend vẫn thao tác được trên file gốc.

**c) `width` / `height` / `fileSize` lưu sẵn để chống "giật layout" (CLS)**
Nếu trình duyệt không biết kích thước ảnh trước khi tải xong, nó sẽ dành 0 pixel rồi đột ngột đẩy nội dung xuống khi ảnh hiện ra. Hiện tượng này gọi là **Cumulative Layout Shift** — một chỉ số Core Web Vitals mà Google dùng để xếp hạng SEO. Biết trước `width`/`height` cho phép đặt sẵn khung đúng tỷ lệ.

**d) `altText` — vừa SEO vừa Accessibility**
Trình đọc màn hình cho người khiếm thị đọc `altText` thay cho ảnh. Google cũng dùng nó để hiểu nội dung ảnh (Google Image Search). Ở nhiều nước, thiếu `alt` là **vi phạm luật tiếp cận** (ADA, WCAG).

**e) `isPrimary` + `displayOrder` — ràng buộc phải giữ ở tầng ứng dụng**
DB không thể ép "mỗi sản phẩm chỉ có đúng 1 ảnh `isPrimary = true`" bằng UNIQUE thông thường. Service phải xử lý trong 1 transaction: set ảnh mới làm primary thì đồng thời hạ tất cả ảnh khác của sản phẩm đó xuống false.

---

## 2.5. `UserWishlist` — Danh sách sản phẩm yêu thích
Bảng: `user_wishlists`

```java
@Entity
@Table(name = "user_wishlists", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "productId"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class UserWishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // ID người dùng thả tim yêu thích

    @Column(nullable = false)
    private Long productId;                   // ID sản phẩm được lưu lại

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm thêm vào danh sách yêu thích
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.
}
```


- **Phương thức `of(userId, productId, now)`** (plan 2026-11-16 wishlist W01.02) — factory tạo một lượt thích. Ghi thật dùng `INSERT … ON CONFLICT (user_id, product_id) DO NOTHING` để `PUT /me/wishlist/{id}` idempotent dưới tải đồng thời; chỉ thích được sản phẩm `PUBLISHED`.
---

## 2.6. `CategoryAttribute` — Bộ thuộc tính chuẩn theo ngành hàng
Bảng: `category_attributes`

> Phục vụ **UC-CAT-15**: mỗi ngành hàng khai báo danh sách thuộc tính chuẩn (Xuất xứ, Thương hiệu, Hạn sử dụng, Chất liệu, Dung lượng…), đánh dấu bắt buộc / dùng để lọc / là trục biến thể. Người bán nhập giá trị vào `Product.specsJson` theo đúng `code`; sản phẩm thiếu thuộc tính bắt buộc không được gửi duyệt.

```java
@Entity
@Table(name = "category_attributes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_category_attribute_code", columnNames = {"category_id", "code"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CategoryAttribute {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long categoryId;                  // FK → categories.id — thuộc tính khai ở ngành cha được ngành con kế thừa (Service duyệt cây danh mục)

    @Column(nullable = false, length = 50)
    private String code;                      // Khóa máy đọc, dùng làm key trong Product.specsJson (vd: "origin", "expiry_date", "storage_gb")

    @Column(nullable = false, length = 100)
    private String name;                      // Tên hiển thị: "Xuất xứ", "Hạn sử dụng"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttributeDataType dataType;       // TEXT / NUMBER / BOOLEAN / ENUM / MULTI_ENUM / DATE

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String allowedValuesJson;         // NULLABLE — Danh sách giá trị hợp lệ khi ENUM / MULTI_ENUM: ["Việt Nam","Nhật Bản",...]

    @Column(length = 20)
    private String unit;                      // NULLABLE — Đơn vị cho NUMBER: "GB", "ml", "kg"

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRequired = false;       // Bắt buộc nhập trước khi gửi duyệt (thông tin pháp lý: xuất xứ, hạn dùng, thành phần…)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isFilterable = false;     // Dùng làm bộ lọc trên trang tìm kiếm (truy vấn JSONB Product.specsJson, index GIN)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVariantAxis = false;    // Là trục tạo biến thể SKU (Màu, Size) — giá trị nằm ở ProductVariant.attributesJson

    @Builder.Default
    @Column(nullable = false)
    private Integer displayOrder = 0;         // Thứ tự hiển thị trên form đăng bán & trang chi tiết

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;          // Ngừng dùng thuộc tính mà không làm hỏng dữ liệu cũ

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    // ===== QUAN HỆ =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;                // Ngành hàng sở hữu thuộc tính
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kiểm tra giá trị người bán nhập (null hợp lệ khi không bắt buộc; ENUM phải thuộc danh sách — Service parse allowedValuesJson) — UC-CAT-09 */
    public boolean accepts(String rawValue, java.util.Set<String> allowedValues) {
        if (rawValue == null || rawValue.isBlank()) return !Boolean.TRUE.equals(this.isRequired);
        return switch (this.dataType) {
            case NUMBER -> rawValue.matches("-?\\d+(\\.\\d+)?");
            case BOOLEAN -> rawValue.equals("true") || rawValue.equals("false");
            case DATE -> rawValue.matches("\\d{4}-\\d{2}-\\d{2}");
            case ENUM -> allowedValues.contains(rawValue);
            case MULTI_ENUM -> java.util.Arrays.stream(rawValue.split(",")).map(String::trim).allMatch(allowedValues::contains);
            case TEXT -> rawValue.length() <= 500;
        };
    }

    /** Ngừng dùng thuộc tính (dữ liệu cũ trong specsJson giữ nguyên) — UC-CAT-15 */
    public void deactivate() {
        this.isActive = false;
        this.isRequired = false;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Vì sao không để `specsJson` hoàn toàn tự do?**
JSON tự do linh hoạt nhưng sàn không kiểm soát được: shop A ghi "Xuất xứ", shop B ghi "Nơi sản xuất", shop C bỏ trống. Hậu quả: (1) không lọc được theo thuộc tính, (2) thiếu thông tin bắt buộc theo Luật BVQLNTD (xuất xứ, hạn sử dụng, thành phần) mà sàn có nghĩa vụ kiểm soát. Bộ thuộc tính chuẩn theo ngành là "lược đồ" cho phần JSON đó — vẫn giữ JSONB (không EAV hàng triệu dòng), nhưng **khóa và kiểu dữ liệu do sàn quy định**.

**b) Kế thừa theo cây danh mục**
Thuộc tính khai ở "Thực phẩm" (Hạn sử dụng, Xuất xứ) tự áp dụng cho "Thực phẩm > Bánh kẹo". Service lấy thuộc tính bằng CTE đệ quy từ `Category.parentId` lên gốc, cache Redis `cat:attrs:{categoryId}` (xóa khi Admin sửa).

**c) Liên kết với tìm kiếm**
`isFilterable = true` → trang tìm kiếm (UC-CAT-05) hiện bộ lọc theo thuộc tính, truy vấn `specs_json ->> 'code'` có index GIN `jsonb_path_ops`; đổi `dataType` của thuộc tính đang dùng phải tạo thuộc tính mới (không đổi kiểu tại chỗ — tránh làm hỏng dữ liệu cũ).

---

## 2.7. `Brand` — Thương hiệu
Bảng: `brands`

> Phục vụ **UC-CAT-09 / UC-CAT-10** (ô "Thương hiệu" có gợi ý và nút "Thêm … làm thương hiệu mới") và bộ lọc thương hiệu ở **UC-CAT-05**.

```java
@Entity
@Table(name = "brands", uniqueConstraints = {
    @UniqueConstraint(name = "uq_brand_slug", columnNames = "slug")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Brand {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, length = 100)
    private String name;                      // Tên hiển thị: "Samsung", "Logitech"

    @Column(nullable = false, length = 120)
    private String slug;                      // Khóa chuẩn hóa (bỏ dấu, chữ thường) — chống trùng "samsung" / "SAMSUNG"

    @Column(length = 500)
    private String logoUrl;                   // NULLABLE — Logo thương hiệu

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVerified = false;       // true = Staff đã xác nhận (thương hiệu do shop tự thêm mặc định chưa xác minh)

    private Long createdByUserId;             // NULLABLE — FK mềm → users.id — shop đã thêm thương hiệu (NULL = do sàn tạo)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Shop thêm thương hiệu mới ngay trong form đăng bán (chưa xác minh) — UC-CAT-09 */
    public static Brand proposedBy(String name, String slug, Long vendorUserId, OffsetDateTime now) {
        Brand b = new Brand();
        b.name = name; b.slug = slug; b.createdByUserId = vendorUserId; b.isVerified = false; b.createdAt = now;
        return b;
    }

    /** Staff xác nhận thương hiệu (khi duyệt sản phẩm đầu tiên dùng thương hiệu này) — UC-CAT-13 */
    public void verify() {
        this.isVerified = true;
    }
}
```

### 💡 Kiến thức chuyên sâu
- **Vì sao tách bảng thay vì để chuỗi tự do trên `Product`?** Chuỗi tự do sinh ra "Samsung", "SAMSUNG", "Sam sung" — không lọc được theo thương hiệu và không gắn được giấy ủy quyền phân phối. `slug` UNIQUE gom các cách viết về một dòng.
- **Thương hiệu do shop tự thêm** vẫn dùng được ngay (`isVerified = false`), nhưng sản phẩm dùng thương hiệu chưa xác minh luôn vào hàng chờ duyệt thủ công (UC-CAT-13) để chống hàng giả mạo thương hiệu.

---

## 2.8. `ProductDocument` — Giấy tờ chứng thực sản phẩm
Bảng: `product_documents`

> Phục vụ **UC-CAT-09 / UC-CAT-10** (mục "Giấy tờ chứng thực — bắt buộc, tối đa 4 tệp PDF / DOC / XLS") và **UC-CAT-12 / UC-CAT-13** (Staff xem "Hồ sơ pháp lý sản phẩm" khi duyệt).

```java
@Entity
@Table(name = "product_documents", indexes = {
    @Index(name = "idx_product_document_product", columnList = "product_id")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductDocument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productId;                   // FK → products.id (ON DELETE CASCADE)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductDocumentType documentType; // BUSINESS_LICENSE / QUALITY_CERTIFICATE / IMPORT_DOCUMENT / BRAND_AUTHORIZATION / OTHER

    @Column(nullable = false, length = 500)
    private String fileUrl;                   // Đường dẫn Silo (bucket riêng tư, đọc qua Presigned URL)

    @Column(nullable = false, length = 255)
    private String fileName;                  // Tên tệp gốc hiển thị cho Staff

    @Column(nullable = false, length = 20)
    private String fileExtension;             // pdf / doc / docx / xls / xlsx — KHÔNG nhận ảnh (giao diện chặn)

    @Column(nullable = false)
    private Long fileSizeBytes;               // ≤ 10 MB / tệp

    private LocalDate expiresAt;              // NULLABLE — Ngày hết hạn của giấy tờ (giấy phép có thời hạn); NULL = không thời hạn

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime uploadedAt;

    // ===== QUAN HỆ =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm sở hữu giấy tờ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Giấy tờ còn hiệu lực tại thời điểm duyệt — UC-CAT-13 */
    public boolean isValidOn(LocalDate day) {
        return this.expiresAt == null || !this.expiresAt.isBefore(day);
    }
}
```

### 💡 Kiến thức chuyên sâu
- **Quy tắc ở Service:** tối đa 4 tệp / sản phẩm; danh mục có `Category.requiresBusinessLicense = true` bắt buộc tối thiểu 1 tệp trước khi `Product.submitForReview()`. Sửa giấy tờ của sản phẩm đang bán đưa sản phẩm về `PENDING_APPROVAL` (duyệt lại).
- **Lưu trữ:** bucket riêng `product-docs` không công khai; Gemini (UC-CAT-12) chỉ nhận **tên tệp và loại giấy tờ**, không nhận nội dung tệp — tránh lộ dữ liệu pháp lý của người bán ra dịch vụ bên ngoài.

---

## 2.9. `CategoryProposal` — Đề xuất danh mục mới của người bán
Bảng: `category_proposals`

> Phục vụ **UC-CAT-17**: người bán không tìm thấy ngành hàng phù hợp → đề xuất danh mục mới (kèm danh mục cha); Staff / Admin duyệt (tạo `Category`) hoặc từ chối kèm lý do.

```java
@Entity
@Table(name = "category_proposals", indexes = {
    @Index(name = "idx_category_proposal_status", columnList = "status, created_at")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CategoryProposal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop đề xuất

    private Long parentCategoryId;            // NULLABLE — FK → categories.id — danh mục cha đề xuất; NULL = đề xuất danh mục gốc

    @Column(nullable = false, length = 100)
    private String proposedName;              // Tên danh mục đề xuất: "Thú cưng (Pet)"

    @Column(columnDefinition = "TEXT")
    private String reason;                    // NULLABLE — Shop giải thích vì sao cần danh mục này

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private CategoryProposalStatus status = CategoryProposalStatus.PENDING; // PENDING / APPROVED / REJECTED

    private Long createdCategoryId;           // NULLABLE — FK → categories.id — danh mục đã tạo khi duyệt

    private Long reviewedByStaffId;           // NULLABLE — FK mềm → users.id — Staff / Admin xử lý

    @Column(length = 500)
    private String rejectionReason;           // NULLABLE — Lý do từ chối

    private OffsetDateTime reviewedAt;         // NULLABLE

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Duyệt đề xuất — Service tạo Category mới rồi gọi hàm này — UC-CAT-17 */
    public void approve(Long categoryId, Long staffId, OffsetDateTime now) {
        if (this.status != CategoryProposalStatus.PENDING) throw new IllegalStateException("Đề xuất đã được xử lý");
        this.status = CategoryProposalStatus.APPROVED;
        this.createdCategoryId = categoryId;
        this.reviewedByStaffId = staffId;
        this.reviewedAt = now;
    }

    /** Từ chối đề xuất kèm lý do — UC-CAT-17 */
    public void reject(String reason, Long staffId, OffsetDateTime now) {
        if (this.status != CategoryProposalStatus.PENDING) throw new IllegalStateException("Đề xuất đã được xử lý");
        this.status = CategoryProposalStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedByStaffId = staffId;
        this.reviewedAt = now;
    }
}
```

---

## 2.10. `ProductModerationResult` — Kết quả kiểm duyệt sản phẩm tự động
Bảng: `product_moderation_results`

> Phục vụ **UC-CAT-12** (kiểm duyệt tự động) và **UC-CAT-13** (màn duyệt hiển thị "Phân tích từ AI", "Lý do AI từ chối"). Mỗi lần sản phẩm được gửi duyệt (tạo mới hoặc sửa trường quan trọng) sinh **một dòng mới** — giữ lịch sử, không ghi đè.

```java
@Entity
@Table(name = "product_moderation_results", indexes = {
    @Index(name = "idx_moderation_product_time", columnList = "product_id, created_at")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductModerationResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productId;                   // FK → products.id

    @Column(nullable = false, length = 60)
    private String modelName;                 // Mô hình đã dùng: "gemini-2.5-flash" (TLCN) / tên model AI Worker (DATN)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModerationDecision decision;      // APPROVE / REJECT / NEED_REVIEW

    @Column(nullable = false)
    private Integer riskScore;                // 0–100 — điểm rủi ro do mô hình trả về

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String violationsJson;            // NULLABLE — Mã vi phạm: ["COUNTERFEIT","PROHIBITED_ITEM","MISLEADING_DESCRIPTION","BAD_IMAGE","PRICE_ANOMALY","WRONG_CATEGORY"]

    @Column(columnDefinition = "TEXT")
    private String reasoning;                 // NULLABLE — Giải thích tiếng Việt hiển thị ở ô "Phân tích từ AI"

    @Column(columnDefinition = "TEXT")
    private String rawResponse;               // NULLABLE — JSON gốc mô hình trả về (phục vụ điều tra; không hiển thị)

    @Builder.Default
    @Column(nullable = false)
    private Boolean fallbackToManual = false; // true = gọi mô hình lỗi / quá hạn / JSON sai → chuyển thẳng hàng chờ duyệt thủ công

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ===== QUAN HỆ =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm được kiểm duyệt
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kết quả khi gọi mô hình thất bại — luôn chuyển duyệt thủ công — UC-CAT-12 */
    public static ProductModerationResult fallback(Long productId, String modelName, String error, OffsetDateTime now) {
        ProductModerationResult r = new ProductModerationResult();
        r.productId = productId; r.modelName = modelName; r.decision = ModerationDecision.NEED_REVIEW;
        r.riskScore = 50; r.fallbackToManual = true; r.rawResponse = error; r.createdAt = now;
        return r;
    }

    /** Có tự từ chối được không (REJECT và điểm rủi ro ≥ ngưỡng cấu hình) — UC-CAT-12 */
    public boolean isAutoReject(int rejectThreshold) {
        return this.decision == ModerationDecision.REJECT && this.riskScore >= rejectThreshold;
    }
}
```

### 💡 Kiến thức chuyên sâu
- **Mô hình chỉ là tín hiệu:** mặc định không cho mô hình tự đăng bán — `APPROVE` vẫn vào hàng chờ CAT-13 (ưu tiên thấp), `NEED_REVIEW` ưu tiên cao; chỉ `REJECT` với `riskScore` vượt ngưỡng mới tự từ chối (người bán sửa và gửi lại).
- **Đầu ra có cấu trúc:** yêu cầu mô hình trả JSON theo schema cố định (`decision`, `riskScore`, `violations[]`, `reasoning`). JSON sai định dạng → `fallback()` — không bao giờ để lỗi của dịch vụ bên ngoài chặn người bán.

---

## 2.11. `SearchKeywordStat` — Thống kê từ khóa tìm kiếm theo ngày
Bảng: `search_keyword_stats`

> Phục vụ **UC-CAT-04** (từ khóa phổ biến dưới ô tìm kiếm: "Smartphone", "Áo thun", "Serum"… và gợi ý theo tiền tố). Không lưu theo từng người dùng — chỉ đếm gộp theo ngày.

```java
@Entity
@Table(name = "search_keyword_stats", uniqueConstraints = {
    @UniqueConstraint(name = "uq_keyword_day", columnNames = {"normalized_keyword", "stat_date"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class SearchKeywordStat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, length = 100)
    private String normalizedKeyword;         // Từ khóa đã chuẩn hóa: chữ thường, bỏ dấu cách thừa (giữ dấu tiếng Việt để hiển thị)

    @Column(nullable = false)
    private LocalDate statDate;               // Ngày thống kê

    @Builder.Default
    @Column(nullable = false)
    private Integer searchCount = 0;          // Số lượt tìm trong ngày (flush từ Redis search:kw:{date})

    @Builder.Default
    @Column(nullable = false)
    private Integer resultClickCount = 0;     // Số lượt bấm vào kết quả — loại từ khóa "tìm mà không mua"

    @Builder.Default
    @Column(nullable = false)
    private Boolean isBlocked = false;        // Staff chặn từ khóa nhạy cảm khỏi danh sách phổ biến
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cộng dồn bộ đếm từ Redis — UC-CAT-04 */
    public void accumulate(int searches, int clicks) {
        this.searchCount += searches;
        this.resultClickCount += clicks;
    }
}
```

---

## 2.12. `ProductViewStat` — Lượt xem sản phẩm theo ngày & nguồn truy cập
Bảng: `product_view_stats`

> Phục vụ **UC-VND-08** (thẻ "Lượt truy cập", "Tỉ lệ chuyển đổi", biểu đồ "Nguồn truy cập") và **UC-CAT-06** (mỗi lần mở trang chi tiết tăng bộ đếm trên Redis).

```java
@Entity
@Table(name = "product_view_stats", uniqueConstraints = {
    @UniqueConstraint(name = "uq_product_view_day_source", columnNames = {"product_id", "stat_date", "source"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductViewStat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productId;                   // FK → products.id

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — sao chép từ sản phẩm để tổng hợp theo shop không cần JOIN

    @Column(nullable = false)
    private LocalDate statDate;               // Ngày thống kê

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrafficSource source;             // SEARCH / HOME_FEED / PROMOTION / SHOP_PAGE / EXTERNAL_LINK

    @Builder.Default
    @Column(nullable = false)
    private Integer viewCount = 0;            // Lượt xem trang chi tiết

    @Builder.Default
    @Column(nullable = false)
    private Integer uniqueVisitorCount = 0;   // Khách khác nhau (HyperLogLog trên Redis, ghi số ước lượng)

    @Builder.Default
    @Column(nullable = false)
    private Integer addToCartCount = 0;       // Lượt thêm vào giỏ từ trang này
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cộng dồn bộ đếm từ Redis (job 5 phút) — UC-CAT-06 */
    public void accumulate(int views, int visitors, int carts) {
        this.viewCount += views;
        this.uniqueVisitorCount = Math.max(this.uniqueVisitorCount, visitors);
        this.addToCartCount += carts;
    }
}
```

### 💡 Kiến thức chuyên sâu
- **Tỉ lệ chuyển đổi của shop** = số đơn đã thanh toán / Σ `uniqueVisitorCount` trong kỳ (UC-VND-08). Nguồn truy cập lấy từ tham số `?src=` mà giao diện gắn vào link (tìm kiếm, trang chủ, khuyến mãi, trang shop, link chia sẻ UC-CAT-18).
- **Không ghi mỗi lượt xem vào PostgreSQL:** `INCR` trên Redis rồi job gộp theo ngày — tránh hàng nghìn UPDATE/giây vào cùng một dòng.

---

# 3. NHÓM GIỎ HÀNG

## 3.1. `Cart` — Giỏ hàng
Bảng: `carts`

```java
@Entity
@Table(name = "carts")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Cart {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long userId;                      // FK → users.id, UNIQUE — mỗi user có đúng 1 giỏ hàng

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Lần cuối giỏ hàng thay đổi — dùng cho email nhắc "bỏ quên giỏ hàng"

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "cart_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua CartItem.cartId
    private List<CartItem> items;             // Các dòng sản phẩm trong giỏ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Thêm sản phẩm (gộp nếu trùng SKU) — UC-ORD-01 */
    public void addItem(CartItem item) {
        this.items.stream().filter(i -> i.getProductVariantId().equals(item.getProductVariantId())).findFirst()
            .ifPresentOrElse(i -> i.increase(item.getQuantity()), () -> this.items.add(item));
    }

    /** Xóa dòng hàng — UC-ORD-01 */
    public void removeItem(Long productVariantId) {
        this.items.removeIf(i -> i.getProductVariantId().equals(productVariantId));
    }

    /** Tạm tính theo giá snapshot — UC-ORD-04 */
    public BigDecimal subtotal() {
        return this.items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Gộp giỏ tạm của khách vãng lai (lưu trên trình duyệt) vào giỏ tài khoản khi đăng nhập / đăng ký; service đã lọc SKU ngừng bán và giới hạn số lượng theo tồn kho trước khi gọi — UC-ORD-01 */
    public void mergeGuestItems(List<CartItem> guestItems) {
        guestItems.forEach(this::addItem);
    }
}
```

---

## 3.2. `CartItem` — Dòng sản phẩm trong giỏ
Bảng: `cart_items`

```java
@Entity
@Table(name = "cart_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CartItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long cartId;                      // FK → carts.id — thuộc giỏ hàng nào

    @Column(nullable = false)
    private Long productVariantId;            // FK → product_variants.id — mua BIẾN THỂ nào (không phải sản phẩm chung)

    @Column(nullable = false)
    private Integer quantity;                 // Số lượng khách muốn mua

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal priceSnapshot;         // Giá tại thời điểm THÊM VÀO GIỎ — để phát hiện biến động giá

    @Column(nullable = false)
    private OffsetDateTime addedAt;            // Thời điểm thêm vào giỏ

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_variant_id", insertable = false, updatable = false)
    private ProductVariant variant;           // Biến thể sản phẩm tương ứng
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tăng số lượng — UC-ORD-01 */
    public void increase(int qty) {
        this.quantity += qty;
    }

    /** Thành tiền dòng — UC-ORD-04 */
    public BigDecimal lineTotal() {
        return this.priceSnapshot.multiply(BigDecimal.valueOf(this.quantity));
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) `priceSnapshot` trong giỏ hàng — KHÔNG phải để tính tiền!**
Đây là điểm dễ hiểu sai nhất. Giá trong giỏ hàng **không có giá trị ràng buộc**. Vai trò thật của nó là **phát hiện thay đổi giá**:

```
Khi khách vào trang thanh toán:
  giá hiện tại = productVariant.price
  if (giá hiện tại != cartItem.priceSnapshot):
      → hiện cảnh báo "Giá sản phẩm đã thay đổi từ 200.000đ thành 250.000đ"
      → BẮT BUỘC khách xác nhận lại trước khi đặt
```

> **Vì sao không dùng giá trong giỏ để thanh toán?** Vì đó là lỗ hổng bảo mật kinh điển: khách thêm hàng vào giỏ, đợi 3 tháng rồi mua với giá cũ. Tệ hơn, nếu client gửi giá lên, kẻ gian có thể sửa request thành 1.000đ. **Nguyên tắc vàng: giá thanh toán LUÔN đọc lại từ server tại thời điểm tạo đơn.**
> Khác biệt với `OrderItem.unitPrice`: giá trong **đơn hàng** mới là giá chốt bất biến; giá trong **giỏ** chỉ là tham khảo.

**b) Giỏ hàng trỏ tới Variant chứ không phải Product**
Khách không mua "Áo thun Uniqlo" chung chung — họ mua "Áo thun Uniqlo, màu Đen, size L". Chỉ variant mới có giá và tồn kho cụ thể.

**c) Giỏ hàng KHÔNG giữ chỗ tồn kho**
Thêm vào giỏ không hề trừ `reservedCount`. Việc giữ chỗ chỉ xảy ra khi khách bấm **đặt hàng** (tạo `StockReservation`).
> **Vì sao?** Nếu giỏ hàng giữ chỗ, kẻ phá hoại chỉ cần thêm toàn bộ hàng của shop vào giỏ rồi bỏ đó → shop không bán được cho ai. Đây gọi là **Denial of Inventory attack**. Vì thế mọi sàn lớn đều chỉ giữ chỗ ở bước checkout với thời hạn ngắn (10–15 phút).

**d) `Cart.updatedAt` — nền tảng cho email "Abandoned Cart"**
Giỏ hàng bị bỏ quên là nguồn doanh thu lớn trong TMĐT (tỷ lệ bỏ giỏ trung bình ~70%). Bản mở rộng (UC-NOT-08) sẽ có job quét các giỏ có `updatedAt` cách đây 24 giờ mà chưa thành đơn → gửi email nhắc; MVP chỉ giữ sẵn cột này.

---

# 4. NHÓM KHUYẾN MÃI, VOUCHER, FLASH SALE & ĐIỂM

## 4.1. `Voucher` — Mã giảm giá
Bảng: `vouchers`

```java
@Entity
@Table(name = "vouchers")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Voucher {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false, length = 50)
    private String code;                      // Mã khách nhập: "SALE50K" — UNIQUE toàn hệ thống

    @Column(nullable = false, length = 255)
    private String title;                     // Tên chương trình hiển thị: "Giảm 50K cho đơn từ 300K"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoucherScope scope;               // Phạm vi MVP: ALL (mọi sản phẩm của shop / của đơn). Giữ cột để mở rộng CATEGORY / PRODUCT ở bản đầy đủ

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherType type;                 // MVP 2 tầng: SHOP (shop tài trợ, Tầng 1) / PLATFORM (sàn tài trợ, Tầng 2)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;        // PERCENTAGE (theo %) / FIXED_AMOUNT (số tiền cố định)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;         // Giá trị giảm: 10 (nếu là %) hoặc 50000 (nếu là số tiền)

    @Column(precision = 19, scale = 2)
    private BigDecimal maxDiscountCap;        // NULLABLE — Trần giảm tối đa. VD giảm 50% nhưng không quá 100.000đ

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal minOrderValue;         // Giá trị đơn tối thiểu mới được dùng mã

    @Column(nullable = false)
    private Integer usageLimit;               // Tổng số lượt được dùng của TOÀN BỘ mã này

    @Column(nullable = false)
    private Integer usagePerUser;             // Số lượt tối đa MỖI NGƯỜI được dùng

    @Builder.Default
    @Column(nullable = false)
    private Integer usedCount = 0;            // Số lượt đã dùng thực tế (bộ đếm)

    @Column(nullable = false)
    private Integer priority;                 // Độ ưu tiên khi khách dùng nhiều mã cùng lúc (số lớn áp trước)

    private Long vendorId;                    // NULLABLE — FK → users.id. Có giá trị khi type = SHOP; NULL = mã của sàn

    @Column(nullable = false)
    private OffsetDateTime validFrom;          // Thời điểm bắt đầu hiệu lực

    @Column(nullable = false)
    private OffsetDateTime validTo;            // Thời điểm hết hiệu lực

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;          // Công tắc bật/tắt thủ công (vòng đời logic: SCHEDULED nếu now < validFrom; ACTIVE nếu isActive=true && validFrom <= now <= validTo; EXPIRED nếu now > validTo)

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống phát vượt số lượt khi nhiều người cùng dùng

    @Column(precision = 19, scale = 2)
    private BigDecimal budget;                // NULLABLE — Trần NGÂN SÁCH tính bằng tiền cho cả chiến dịch

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDiscountUsed = BigDecimal.ZERO; // Tổng số tiền đã giảm tích luỹ — đối chiếu với budget

    @Column(columnDefinition = "TEXT")
    private String notificationBody;          // NULLABLE — Nội dung thông báo đẩy khi tung mã mới

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FundingParty sponsorType;         // Ai chi trả khoản giảm: PLATFORM (sàn) hay VENDOR (shop)

    @Column(precision = 5, scale = 2)
    private BigDecimal platformSharePercent;  // NULLABLE — Chỉ khi sponsorType = CO_SPONSOR: % sàn chịu, phần còn lại shop chịu

    // ===== Phạm vi áp dụng (scope = CATEGORY / PRODUCT) và danh sách loại trừ =====
    @OneToMany
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private List<VoucherCategory> categories;          // Danh mục ĐƯỢC áp dụng

    @OneToMany
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private List<VoucherProduct> products;             // Sản phẩm ĐƯỢC áp dụng

    @OneToMany
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private List<VoucherExcludedCategory> excludedCategories; // Danh mục BỊ LOẠI TRỪ

    @OneToMany
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private List<VoucherExcludedProduct> excludedProducts;    // Sản phẩm BỊ LOẠI TRỪ

    // ===== QUAN HỆ =====

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kiểm tra điều kiện áp dụng (thời gian, lượt, đơn tối thiểu) — UC-PRO-03 */
    public boolean isApplicable(BigDecimal orderValue, OffsetDateTime now) {
        return Boolean.TRUE.equals(this.isActive) && !now.isBefore(this.validFrom) && now.isBefore(this.validTo)
            && (this.usageLimit == null || this.usedCount < this.usageLimit)
            && orderValue.compareTo(this.minOrderValue) >= 0;
    }

    /** Tính số tiền giảm (có trần, không vượt giá trị) — UC-PRO-03 */
    public BigDecimal calculateDiscount(BigDecimal base) {
        BigDecimal d = this.discountType == DiscountType.PERCENTAGE
            ? base.multiply(this.discountValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
            : this.discountValue;
        return this.maxDiscountCap == null ? d.min(base) : d.min(this.maxDiscountCap).min(base);
    }

    /** Ghi nhận một lượt dùng (service dùng @Version), hết ngân sách tự tắt — UC-PRO-03 */
    public void consume(BigDecimal discount) {
        if (this.usageLimit != null && this.usedCount >= this.usageLimit) throw new IllegalStateException("GNE-VCH-4221");
        this.usedCount += 1;
        this.totalDiscountUsed = this.totalDiscountUsed.add(discount);
        if (this.budget != null && this.totalDiscountUsed.compareTo(this.budget) >= 0) this.isActive = false;
    }

    /** Hoàn lượt khi đơn bị hủy / hết hạn thanh toán — UC-ORD-08 */
    public void restore(BigDecimal discount) {
        this.usedCount = Math.max(0, this.usedCount - 1);
        this.totalDiscountUsed = this.totalDiscountUsed.subtract(discount).max(BigDecimal.ZERO);
    }

    /** Kiểm tra cấu hình khi tạo/sửa: SHOP bắt buộc vendorId & shop tài trợ; PLATFORM không gắn shop & sàn tài trợ — UC-PRO-01, UC-PRO-02 */
    public void validateConfig() {
        if (this.type == VoucherType.SHOP && (this.vendorId == null || this.sponsorType != FundingParty.VENDOR))
            throw new IllegalArgumentException("Voucher SHOP phải có vendorId và sponsorType = VENDOR");
        if (this.type == VoucherType.PLATFORM && (this.vendorId != null || this.sponsorType != FundingParty.PLATFORM))
            throw new IllegalArgumentException("Voucher PLATFORM không gắn shop và sponsorType = PLATFORM");
        if (this.validTo.isBefore(this.validFrom)) throw new IllegalArgumentException("validTo phải sau validFrom");
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Ba lớp giới hạn độc lập — vì sao cần cả ba?**

| Trường | Giới hạn theo | Ngăn chặn điều gì |
|---|---|---|
| `usageLimit` | Tổng số **lượt** | Mã bị dùng vô hạn |
| `usagePerUser` | Số lượt **mỗi người** | 1 người ôm hết mã |
| `budget` + `totalDiscountUsed` | Tổng **số tiền** | Vỡ ngân sách marketing |

> **Vì sao `usageLimit` không đủ, phải có thêm `budget`?** Xét mã "giảm 10% không giới hạn trần", `usageLimit = 1000`. Nếu 1000 người mua đơn 100.000đ → tốn 10 triệu. Nhưng nếu 1000 người mua đơn 50 triệu → tốn **5 tỷ**. Cùng số lượt, chi phí chênh 500 lần! `budget` là chốt chặn cuối cùng theo tiền thật.

**b) `sponsorType` — ai thật sự chịu chi phí giảm giá?**
Đây là trường mang ý nghĩa **kế toán**, quyết định dòng tiền (enum `FundingParty`, dùng chung với `OrderDiscount.fundedBy`):
- `PLATFORM` (sàn tài trợ) → sàn chịu lỗ, vendor vẫn nhận **đủ tiền hàng** (`EscrowLedger.grossAmount` không bị trừ khoản này). Báo cáo chi phí marketing của sàn tổng hợp từ `OrderDiscount` có `fundedBy = PLATFORM`.
- `VENDOR` (shop tài trợ) → shop chịu lỗ, số tiền giảm bị trừ vào khoản vendor được nhận.

Điều này được phản ánh trực tiếp ở `OrderDiscount.fundedBy` và ở `OrderItem` (tách riêng `platformDiscount` và `shopDiscount`).

**c) `priority` — thứ tự cộng dồn khuyến mãi (stacking)**
Khách có thể dùng đồng thời (MVP 2 tầng): 1 mã của shop cho mỗi đơn con + 1 mã của sàn cho cả phiên thanh toán. Thứ tự áp dụng ảnh hưởng trực tiếp tới số tiền cuối:

```
Đơn 1.000.000đ, có mã A "giảm 10%" và mã B "giảm 100.000đ"
Áp A trước: 1.000.000 → 900.000 → 800.000   (khách trả 800k)
Áp B trước: 1.000.000 → 900.000 → 810.000   (khách trả 810k)
```

Chênh 10.000đ chỉ vì đổi thứ tự. `priority` giúp quy tắc này **tường minh và nhất quán**, tránh tranh chấp.

**d) `maxDiscountCap` — chống "mã giảm giá thảm hoạ"**
Mã "giảm 50%" mà không có trần: khách mua iPhone 30 triệu được giảm 15 triệu. Trần giảm biến rủi ro vô hạn thành rủi ro có kiểm soát.
Công thức đầy đủ:

```
discount = (discountType == PERCENTAGE)
             ? min(orderValue * discountValue / 100, maxDiscountCap)
             : discountValue
```

**e) `isActive` — công tắc ngắt khẩn cấp**
Khi phát hiện mã bị lộ hoặc cấu hình sai đang gây lỗ, admin chỉ cần set `isActive = false` là chặn ngay lập tức, không phải sửa `validTo` (thao tác này còn ảnh hưởng tới báo cáo lịch sử).

---

## 4.2. `VoucherUsage` — Lịch sử sử dụng voucher
Bảng: `voucher_usages`

```java
@Entity
@Table(name = "voucher_usages", uniqueConstraints = {
    @UniqueConstraint(name = "uq_voucher_usage_session", columnNames = {"voucher_id", "checkout_session_id"})   // 1 voucher chỉ trừ 1 lượt / phiên thanh toán
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherUsage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long voucherId;                   // FK → vouchers.id — mã nào đã được dùng

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — ai dùng

    @Column(nullable = false)
    private Long checkoutSessionId;           // FK → checkout_sessions.id — phiên thanh toán đã dùng mã (đơn vị tính lượt dùng)

    private Long orderId;                     // NULLABLE — FK → orders.id. CÓ giá trị với voucher SHOP (đúng 1 đơn con của shop đó); NULL với voucher PLATFORM / FREESHIP / PAYMENT áp toàn phiên (nhiều đơn con)

    @Column(nullable = false)
    private OffsetDateTime usedAt;             // Thời điểm sử dụng

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private Voucher voucher;                  // Voucher được dùng
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo bản ghi lượt dùng — 1 bản ghi / voucher / phiên (UNIQUE voucher+session); shopOrderId chỉ truyền với voucher SHOP — UC-PRO-03 */
    public static VoucherUsage of(Long voucherId, Long userId, Long checkoutSessionId, Long shopOrderId, OffsetDateTime now) {
        VoucherUsage u = new VoucherUsage();
        u.voucherId = voucherId; u.userId = userId; u.checkoutSessionId = checkoutSessionId; u.orderId = shopOrderId; u.usedAt = now;
        return u;
    }
}
```

### 💡 Kiến thức chuyên sâu

**Nguồn sự thật (Source of Truth) cho việc kiểm tra `usagePerUser`**
`Voucher.usedCount` chỉ là **bộ đếm nhanh** (counter cache) cho tổng số lượt. Nhưng để trả lời câu hỏi *"User số 42 đã dùng mã này mấy lần rồi?"*, bắt buộc phải đếm ở bảng này:

```sql
SELECT COUNT(*) FROM voucher_usages WHERE voucher_id = ? AND user_id = ?
```

> **Vì sao không chỉ giữ counter mà bỏ bảng chi tiết?** Vì counter có thể **lệch** (do lỗi ứng dụng, do đơn bị huỷ). Bảng chi tiết là dữ liệu gốc dùng để **tính lại** counter khi cần đối soát. Nguyên tắc: counter để đọc nhanh, bảng chi tiết để đảm bảo đúng.

**Ràng buộc ở tầng DB:** UNIQUE `(voucher_id, checkout_session_id)` — một phiên thanh toán không thể áp cùng 1 mã hai lần do lỗi double-submit.

**Đơn vị ghi lượt dùng:** `orders` là đơn con theo shop, còn voucher sàn (`PLATFORM`, Tầng 2) áp cho **cả phiên** gồm nhiều đơn con. Vì vậy lượt dùng ghi theo **phiên thanh toán**:
| Loại voucher | Số bản ghi `voucher_usages` | `orderId` | Trừ `usageLimit` / `usagePerUser` |
|---|---|---|---|
| `SHOP` (Tầng 1) | 1 / phiên (một phiên chỉ có 1 đơn con của mỗi shop) | Đơn con của shop đó | 1 lượt |
| `PLATFORM` (Tầng 2) | 1 / phiên, dù phiên có 3 đơn con | `NULL` | 1 lượt |
Số tiền giảm phân bổ xuống từng đơn con vẫn nằm ở `OrderDiscount` (mỗi đơn con 1 dòng), nên đối soát theo đơn không bị mất.

---

## 4.3. `UserVoucher` — Ví voucher của người dùng
Bảng: `user_vouchers`

```java
@Entity
@Table(name = "user_vouchers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "voucher_id"}))
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class UserVoucher {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — chủ ví

    @Column(nullable = false)
    private Long voucherId;                   // FK → vouchers.id — mã đã lưu vào ví

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm user bấm "Lưu mã"

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false) @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private Voucher voucher;                  // Chi tiết voucher

    @ManyToOne(optional = false) @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;                        // Chủ sở hữu

    // ---- Trường tính toán, KHÔNG lưu DB ----
    @Transient
    private Boolean isUsed;                   // true = user đã dùng hết lượt cho phép của mã này
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đánh dấu voucher trong ví đã dùng — UC-PRO-03 */
    public void markUsed() {
        if (Boolean.TRUE.equals(this.isUsed)) throw new IllegalStateException("Voucher đã dùng");
        this.isUsed = true;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Phân biệt rõ 3 bảng dễ nhầm lẫn**

| Bảng | Trả lời câu hỏi | Bản chất |
|---|---|---|
| `Voucher` | Mã này có nội dung gì? | Định nghĩa |
| `UserVoucher` | User đã **lưu** những mã nào? | Sở hữu (ví) |
| `VoucherUsage` | User đã **dùng** mã lúc nào, cho đơn nào? | Lịch sử |

Có `UserVoucher` nhưng chưa có `VoucherUsage` = đã lưu vào ví nhưng chưa dùng.

**b) UNIQUE `(userId, voucherId)` — chống lưu trùng**
Nếu user bấm "Lưu mã" 5 lần liên tiếp (hoặc do lỗi mạng gửi lặp request), DB sẽ chặn ở lần thứ 2. Đây là cách đảm bảo **idempotency ở tầng dữ liệu** — an toàn hơn kiểm tra ở code vì không bị race condition.

**c) `isUsed` là `@Transient` — tính theo ngữ cảnh**
Trạng thái "đã dùng hết" không phải thuộc tính cố định của ví, mà phụ thuộc `usagePerUser` của voucher và số dòng trong `VoucherUsage`. Lưu cứng vào DB sẽ sai ngay khi admin tăng `usagePerUser`, hoặc khi đơn hàng bị huỷ và trả lại lượt dùng.

---

## 4.4. `VoucherCategory` / `VoucherProduct` / `VoucherExcludedCategory` / `VoucherExcludedProduct`
Bảng: `voucher_categories`, `voucher_products`, `voucher_excluded_categories`, `voucher_excluded_products`

> 💡 **Mục này đặc tả 4 entity bảng nối dưới một tiêu đề** (khóa chính gộp `@IdClass`) phục vụ phạm vi voucher theo danh mục / sản phẩm và danh sách loại trừ (UC-PRO-01, UC-PRO-02, UC-PRO-04):
> 1. `VoucherCategory` (`voucher_categories` — danh mục áp dụng)
> 2. `VoucherProduct` (`voucher_products` — sản phẩm áp dụng)
> 3. `VoucherExcludedCategory` (`voucher_excluded_categories` — danh mục loại trừ)
> 4. `VoucherExcludedProduct` (`voucher_excluded_products` — sản phẩm loại trừ)

Bốn bảng có cấu trúc giống hệt nhau (khoá chính gộp 2 cột), chỉ khác ngữ nghĩa:

```java
@Entity
@Table(name = "voucher_categories")
@IdClass(VoucherCategoryId.class)             // Khoá chính GỘP (composite key) 2 cột
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherCategory {

    @Id
    private Long voucherId;                   // FK → vouchers.id — phần 1 của khoá chính

    @Id
    private Long categoryId;                  // FK → categories.id — phần 2 của khoá chính

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gắn danh mục ĐƯỢC áp dụng voucher (@CreationTimestamp)

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;                // Danh mục ĐƯỢC áp dụng voucher
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Phạm vi áp dụng của voucher — UC-PRO-01 */
    public boolean matches(Long categoryId) {
        return this.categoryId.equals(categoryId);
    }
}

@Entity
@Table(name = "voucher_products")
@IdClass(VoucherProductId.class)              // Khoá chính GỘP (composite key) 2 cột
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherProduct {

    @Id
    private Long voucherId;                   // FK → vouchers.id — phần 1 của khoá chính

    @Id
    private Long productId;                   // FK → products.id — phần 2 của khoá chính

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gắn sản phẩm ĐƯỢC áp dụng voucher (@CreationTimestamp)

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm ĐƯỢC áp dụng voucher
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Phạm vi áp dụng của voucher — UC-PRO-01 */
    public boolean matches(Long productId) {
        return this.productId.equals(productId);
    }
}

@Entity
@Table(name = "voucher_excluded_categories")
@IdClass(VoucherExcludedCategoryId.class)     // Khoá chính GỘP (composite key) 2 cột
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherExcludedCategory {

    @Id
    private Long voucherId;                   // FK → vouchers.id — phần 1 của khoá chính

    @Id
    private Long categoryId;                  // FK → categories.id — phần 2 của khoá chính

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gắn danh mục BỊ LOẠI TRỪ khỏi voucher (@CreationTimestamp)

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;                // Danh mục BỊ LOẠI TRỪ khỏi voucher
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Phạm vi loại trừ của voucher — UC-PRO-01 */
    public boolean matches(Long categoryId) {
        return this.categoryId.equals(categoryId);
    }
}

@Entity
@Table(name = "voucher_excluded_products")
@IdClass(VoucherExcludedProductId.class)      // Khoá chính GỘP (composite key) 2 cột
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherExcludedProduct {

    @Id
    private Long voucherId;                   // FK → vouchers.id — phần 1 của khoá chính

    @Id
    private Long productId;                   // FK → products.id — phần 2 của khoá chính

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gắn sản phẩm BỊ LOẠI TRỪ khỏi voucher (@CreationTimestamp)

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm BỊ LOẠI TRỪ khỏi voucher
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Phạm vi loại trừ của voucher — UC-PRO-01 */
    public boolean matches(Long productId) {
        return this.productId.equals(productId);
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Bảng trung gian (Junction Table) cho quan hệ N-N**
Một voucher áp dụng cho nhiều danh mục; một danh mục cũng nằm trong nhiều voucher → quan hệ **nhiều-nhiều**. CSDL quan hệ không biểu diễn trực tiếp được N-N, buộc phải tách thành 2 quan hệ 1-N qua một bảng trung gian.
Trong class diagram, đây là các class nằm **giữa** `Voucher` và `Category`/`Product`, với bội số `1..*` ở cả hai đầu.

**b) Khoá chính gộp `(voucherId, categoryId)` — ràng buộc nghiệp vụ miễn phí**
Thay vì thêm cột `id` tự tăng rồi thêm UNIQUE riêng, dùng luôn cặp khoá làm primary key. Lợi ích: DB **tự động** chặn việc gắn trùng 1 danh mục 2 lần vào cùng 1 voucher, không cần code kiểm tra.
Trong JPA: Sử dụng `@IdClass` hoặc `@EmbeddedId` cho khoá chính phức hợp (Composite Key).

**c) Vì sao 4 bảng riêng mà không gộp 1 bảng có cột `type`?**
Cách gộp: 1 bảng `voucher_rules(voucher_id, target_type, target_id, is_excluded)`. Ưu điểm: ít bảng hơn.
Nhược điểm nghiêm trọng: **không đặt được Foreign Key**, vì `target_id` khi thì trỏ `categories`, khi thì trỏ `products`. Đây là anti-pattern **Polymorphic Association** — mất toàn vẹn tham chiếu ở tầng DB.
Dự án chọn 4 bảng tường minh: mỗi bảng có FK đúng đắn, DB tự bảo vệ dữ liệu.

---

## 4.5. `FlashSaleCampaign` — Chiến dịch Flash Sale
Bảng: `flash_sale_campaigns`

```java
@Entity
@Table(name = "flash_sale_campaigns")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class FlashSaleCampaign {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    private Long vendorId;                    // NULLABLE — FK → users.id. NULL = chiến dịch TOÀN SÀN

    @Column(nullable = false, length = 200)
    private String title;                     // Tên chiến dịch: "Flash Sale 12.12"

    @Column(nullable = false)
    private OffsetDateTime startsAt;           // Thời điểm bắt đầu

    @Column(nullable = false)
    private OffsetDateTime endsAt;             // Thời điểm kết thúc

    @Builder.Default
    @Column(nullable = false)
    private Integer maxProducts = 20;         // Số sản phẩm tối đa được tham gia (mặc định 20)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlashSaleStatus status;           // DRAFT / SCHEDULED / ACTIVE / ENDED

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo chiến dịch

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "campaign_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua FlashSaleItem.campaignId
    private List<FlashSaleItem> items;        // Danh sách sản phẩm trong chiến dịch
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Lên lịch (sau khi duyệt) — UC-PRO-05 */
    public void schedule() {
        if (this.status != FlashSaleStatus.DRAFT) throw new IllegalStateException("Chỉ lên lịch bản nháp");
        if (this.items.size() > this.maxProducts) throw new IllegalStateException("Vượt số sản phẩm tối đa");
        this.status = FlashSaleStatus.SCHEDULED;
    }

    /** Cron mở bán khi now ≥ startsAt — UC-PRO-05 */
    public void activate(OffsetDateTime now) {
        if (this.status != FlashSaleStatus.SCHEDULED || now.isBefore(this.startsAt)) throw new IllegalStateException("Chưa tới giờ mở bán");
        this.status = FlashSaleStatus.ACTIVE;
    }

    /** Đang trong khung giờ vàng — UC-PRO-06 */
    public boolean isRunning(OffsetDateTime now) {
        return this.status == FlashSaleStatus.ACTIVE && !now.isBefore(this.startsAt) && now.isBefore(this.endsAt);
    }

    /** Kết thúc chiến dịch (Cron khi now ≥ endsAt, hoặc Admin dừng) — UC-PRO-05 */
    public void end() {
        if (this.status != FlashSaleStatus.ACTIVE && this.status != FlashSaleStatus.SCHEDULED) throw new IllegalStateException("Chiến dịch không chạy");
        this.status = FlashSaleStatus.ENDED;
    }
}
```

## 4.6. `FlashSaleItem` — Sản phẩm trong Flash Sale
Bảng: `flash_sale_items`

```java
@Entity
@Table(name = "flash_sale_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class FlashSaleItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long campaignId;                  // FK → flash_sale_campaigns.id

    @Column(nullable = false)
    private Long productId;                   // FK → products.id — sản phẩm được giảm giá

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal flashPrice;            // GIÁ SỐC trong thời gian diễn ra chiến dịch

    @Column(nullable = false)
    private Integer flashStock;               // Số lượng GIỚI HẠN dành riêng cho flash sale

    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer reservedCount = 0;        // Số suất đã có ĐƠN đang chờ thanh toán (tăng khi tạo Order + StockReservation; giảm khi PAID hoặc giữ chỗ hết hạn)

    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer soldCount = 0;            // Số đã bán trong flash sale — dùng vẽ thanh tiến trình

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlashSaleItemStatus status = FlashSaleItemStatus.PENDING; // Kết quả sàn duyệt đăng ký: PENDING / APPROVED / REJECTED — UC-PRO-05 (TLCN v1.3)

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống race condition khi trừ tồn kho Flash Sale đồng thời
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đăng ký: 0 < flashStock ≤ tồn khả dụng của sản phẩm (Σ stockCount − reservedCount các SKU); kiểm tra lại khi chiến dịch mở bán — UC-PRO-05 */
    public void validateAgainst(int productAvailableStock) {
        if (this.flashStock <= 0 || this.flashStock > productAvailableStock)
            throw new IllegalArgumentException("Số suất Flash Sale (" + this.flashStock + ") phải > 0 và ≤ tồn khả dụng (" + productAvailableStock + ")");
    }

    /** Suất còn lại — UC-PRO-06 */
    public int remaining() {
        return this.flashStock - this.soldCount - this.reservedCount;
    }

    /** Giữ suất khi tạo Order + StockReservation trong cùng transaction — UC-PRO-06 */
    public void reserve(int qty) {
        if (remaining() < qty) throw new IllegalStateException("GNE-INV-4091");   // hết suất Flash Sale
        this.reservedCount += qty;
    }

    /** Giữ chỗ hết hạn / đơn bị hủy trước khi trả tiền → trả suất — UC-ORD-08 */
    public void releaseReservation(int qty) {
        if (this.reservedCount < qty) throw new IllegalStateException("reservedCount không đủ để nhả");
        this.reservedCount -= qty;
    }

    /** Ghi bền khi đơn Flash Sale thanh toán thành công — UC-PRO-06 */
    public void recordSale(int qty) {
        if (this.reservedCount < qty) throw new IllegalStateException("reservedCount không đủ");
        this.reservedCount -= qty;
        this.soldCount += qty;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) `flashStock` tách biệt khỏi tồn kho thật — kho ảo cho chiến dịch**
Shop có 1000 sản phẩm trong kho nhưng chỉ muốn bán **50 cái** với giá sốc. `flashStock = 50` là hạn mức riêng. Hết 50 cái, sản phẩm quay về giá gốc dù kho vẫn còn.
> Điều này tạo hiệu ứng **khan hiếm giả có kiểm soát** — động lực tâm lý mạnh nhất trong bán hàng, đồng thời giới hạn chính xác chi phí khuyến mãi của shop.

**b) `soldCount` — thanh tiến trình "Đã bán 45/50"**
Đây không chỉ là số liệu mà là **công cụ chuyển đổi**. Thanh tiến trình gần đầy tạo áp lực FOMO (sợ bỏ lỡ), thúc đẩy khách quyết định ngay. Mọi sàn đều hiển thị nó rất nổi bật.

**c) `vendorId` nullable — một bảng cho hai loại chiến dịch**
- `NULL` → chiến dịch của **sàn** (VD "Sale 12.12 toàn sàn"), nhiều shop cùng tham gia.
- Có giá trị → chiến dịch riêng của **một shop**.

**d) Chống bán vượt suất ở quy mô TLCN**
Khung giờ vàng có nhiều người bấm mua cùng lúc. Ở quy mô demo (~50 người dùng đồng thời) TLCN giữ suất **ngay trong PostgreSQL bằng một câu UPDATE có điều kiện** — nguyên tử, không cần khóa bi quan hay Redis:

```sql
UPDATE flash_sale_items
   SET reserved_count = reserved_count + :qty
 WHERE id = :id AND flash_stock - sold_count - reserved_count >= :qty;   -- 0 dòng bị ảnh hưởng → hết suất (GNE-INV-4091)
```

Bản KLTN (lưu lượng thật) chuyển cửa chặn tải lên Redis (`DECRBY` nguyên tử + hàng đợi ghi DB) — các trường `reservedCount` / `soldCount` và phương thức giữ nguyên nên không phải đổi mô hình.

**e) Vòng đời trạng thái và job chuyển tự động**

```
DRAFT      → Đang soạn, chưa công bố
SCHEDULED  → Đã lên lịch, chờ tới giờ
ACTIVE     → Đang diễn ra (job tự chuyển khi tới startsAt)
ENDED      → Đã kết thúc (job tự chuyển khi qua endsAt)
```

Việc chuyển trạng thái do job nền thực hiện, không dựa vào việc so sánh thời gian lúc đọc. Lý do: cần một mốc rõ ràng để kích hoạt các tác dụng phụ (gửi thông báo, làm mới cache trang chủ).

---

## 4.7. `UserCoin` — Số dư Điểm
Bảng: `user_coins`

```java
@Entity
@Table(name = "user_coins")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class UserCoin {

    @Id
    private Long userId;                      // KHOÁ CHÍNH đồng thời là FK → users.id (KHÔNG tự tăng) — ví Điểm của khách hàng (1 Điểm = 1₫ khi thanh toán)

    @Column(nullable = false)
    private Integer balance;                  // Số Điểm hiện có

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống race condition khi trừ Điểm đồng thời

    @Column(name = "balance_hash", nullable = false, length = 255)
    private String balanceHash;               // HMAC-SHA256(WALLET_HMAC_SECRET, "coin:" + userId + ":" + balance), hex chữ thường — chống can thiệp trực tiếp CSDL (tiền tố "coin:" để không trùng băm ví shop)

    @Column(nullable = false)
    private OffsetDateTime lastUpdated;        // Lần biến động gần nhất
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cộng Điểm (service cập nhật lại balanceHash) — UC-PRO-08 */
    public void earn(int amount, OffsetDateTime now) {
        this.balance += amount;
        this.lastUpdated = now;
    }

    /** Trừ Điểm (thanh toán đơn / đổi voucher), FEFO theo lô ở service — UC-PRO-10 */
    public void spend(int amount, OffsetDateTime now) {
        if (amount > this.balance) throw new IllegalStateException("Không đủ Điểm");
        this.balance -= amount;
        this.lastUpdated = now;
    }

    /** Phát hiện sửa số dư trực tiếp CSDL — UC-PRO-09 */
    public boolean verifyHash(String expectedHmac) {
        return MessageDigest.isEqual(this.balanceHash.getBytes(StandardCharsets.UTF_8), expectedHmac.getBytes(StandardCharsets.UTF_8));
    }
}
```

## 4.8. `CoinTransaction` — Lịch sử biến động Điểm
Bảng: `coin_transactions`

```java
@Entity
@Table(name = "coin_transactions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CoinTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — Điểm của khách hàng nào

    @Column(nullable = false)
    private Integer amount;                   // Số Điểm biến động — DƯƠNG là cộng, ÂM là trừ (cùng quy ước dấu với các sổ ví tiền)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoinTransactionType type;         // CHECK_IN / ORDER_CASHBACK / REVIEW_BONUS / USED_AT_CHECKOUT / ORDER_REFUND / VOUCHER_REDEEM

    @Column(length = 50)
    private String referenceId;               // NULLABLE — ID đối tượng liên quan (thường là order_id)

    @Column(length = 200)
    private String label;                     // NULLABLE — Mô tả hiển thị: "Hoàn Điểm đơn hàng #1234"

    private OffsetDateTime expiresAt;          // NULLABLE — Điểm HẾT HẠN sau 12 tháng kể từ ngày nhận (giao diện: "Điểm hết hạn sau 12 tháng")

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm phát sinh
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Lô Điểm đã hết hạn (12 tháng) — UC-PRO-09 */
    public boolean isExpired(OffsetDateTime now) {
        return this.expiresAt != null && this.expiresAt.isBefore(now);
    }

    /** Giao dịch cộng Điểm — UC-PRO-09 */
    public boolean isCredit() {
        return this.amount > 0;
    }
}
```

### 💡 Kiến thức chuyên sâu
- **Nguồn Điểm của TLCN** (khớp giao diện "Lịch sử Điểm"): điểm danh hằng ngày (`CHECK_IN`, UC-PRO-08), hoàn Điểm theo đơn / voucher hoàn Điểm (`ORDER_CASHBACK`), đánh giá có ảnh (`REVIEW_BONUS`, UC-SOC-03), hoàn lại Điểm khi đơn bị hủy / hoàn tiền (`ORDER_REFUND`). Tiêu Điểm: thanh toán (`USED_AT_CHECKOUT`, tối đa 50% tiền hàng — UC-PRO-10) và đổi Điểm lấy voucher (`VOUCHER_REDEEM`, UC-PRO-09).
- **Lô hết hạn (`EXPIRED`, V19):** cron 00:15 giờ Việt Nam tính phần còn lại của từng lô cộng đã quá `expiresAt` theo FEFO (lô sắp hết hạn bị tiêu trước) và ghi **một dòng âm** `type = EXPIRED`, `referenceId` = id lô gốc — sổ vẫn chỉ ghi thêm, `Σ amount = UserCoin.balance` luôn đúng; chạy lại không trừ thêm (lô đã có dòng `EXPIRED` tham chiếu thì bỏ qua).
- **Sổ lô + hết hạn:** mỗi dòng cộng Điểm là một "lô" có `expiresAt`; tiêu Điểm trừ lô sắp hết hạn trước (FEFO). Job hằng ngày ghi dòng âm cho lô hết hạn để `Σ amount = UserCoin.balance` luôn đúng.

---

## 4.9. `DailyCheckin` — Điểm danh hằng ngày
Bảng: `daily_checkins`

```java
@Entity
@Table(name = "daily_checkins", uniqueConstraints = {
    @UniqueConstraint(name = "uk_daily_checkin_customer", columnNames = {"customerId"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class DailyCheckin {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long customerId;                  // FK → users.id, UNIQUE — mỗi khách 1 dòng theo dõi

    @Column(nullable = false)
    private OffsetDateTime lastCheckinAt;      // Thời điểm điểm danh gần nhất

    @Column(nullable = false)
    private Integer streakCount;              // Số ngày điểm danh LIÊN TIẾP — đứt 1 ngày là reset về 1
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bản ghi của khách chưa từng điểm danh (chưa lưu — gọi checkIn rồi mới lưu) — UC-PRO-08 */
    public static DailyCheckin firstFor(Long customerId) {
        DailyCheckin d = new DailyCheckin();
        d.customerId = customerId;
        d.streakCount = 0;
        return d;
    }

    /** Điểm danh, tính chuỗi & số Điểm thưởng — UC-PRO-08 */
    public int checkIn(OffsetDateTime now) {
        LocalDate today = now.toLocalDate();
        // quy mốc cũ về cùng offset với now (CSDL trả TIMESTAMPTZ theo UTC) — "ngày" là ngày Việt Nam (now đã ở +07:00)
        LocalDate last = this.lastCheckinAt == null ? null
                : this.lastCheckinAt.withOffsetSameInstant(now.getOffset()).toLocalDate();
        if (today.equals(last)) throw new IllegalStateException("Hôm nay đã điểm danh");
        this.streakCount = (last != null && last.plusDays(1).equals(today)) ? this.streakCount + 1 : 1;
        this.lastCheckinAt = now;
        return this.streakCount % 7 == 0 ? 1000 : 100;   // ngày thứ 7 liên tiếp: 1.000 Điểm
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Vì sao `UserCoin` dùng `userId` làm khoá chính, không có cột `id` riêng?**
Đây là mẫu **Natural Key** (khoá tự nhiên): quan hệ với `users` là 1-1 tuyệt đối, nên `userId` đã đủ định danh duy nhất. Lợi ích:
- Tiết kiệm 1 cột và 1 index.
- **Tự động** đảm bảo mỗi tài khoản chỉ có 1 ví Điểm, không cần UNIQUE riêng.
- Query nhanh hơn: `WHERE user_id = ?` chính là truy cập khoá chính (clustered index).

> **Tên bảng:** `user_coins` khóa `userId` (đồng bộ DATN). Ở TLCN chỉ khách hàng có Điểm; DATN mở rộng cho chủ shop (`VENDOR_REWARD`).

Trong JPA: Không dùng `@GeneratedValue` vì ID được chia sẻ trực tiếp từ bảng gốc (@MapsId).

**b) Điểm là NỢ PHẢI TRẢ của sàn, không phải "điểm thưởng vô hại"**
Đây là điểm nhiều người làm hệ thống bỏ qua. Mỗi Điểm phát ra là một cam kết giảm giá trong tương lai. Nếu sàn phát 1 tỷ Điểm (tương đương 1 tỷ VNĐ), thì trên sổ sách đó là **khoản nợ 1 tỷ**. Điều này được phản ánh ở `FinancialLedger.COIN_COST`.
Hệ quả thiết kế: `amount` dùng `Integer` (Điểm là số nguyên, không có Điểm lẻ), nhưng khi quy đổi thành tiền ở `Order.coinDiscountAmount` phải dùng `BigDecimal`.

**c) `expiresAt` — Điểm hết hạn sau 12 tháng, và vì sao điều đó là bắt buộc**
Ba lý do, theo thứ tự quan trọng:
1. **Kế toán:** nợ phải trả không thể tồn tại vô thời hạn trên bảng cân đối. Điểm hết hạn được ghi nhận là **thu nhập khác** (breakage income).
2. **Tâm lý học hành vi:** thời hạn tạo cảm giác khẩn cấp, thúc đẩy khách quay lại mua hàng.
3. **Kiểm soát rủi ro:** giới hạn quy mô nghĩa vụ tài chính tích luỹ.

> **Thuật toán tiêu Điểm bắt buộc phải là FIFO** (First In First Out — Điểm nào sắp hết hạn tiêu trước — FEFO). Nếu tiêu Điểm mới trước, Điểm cũ sẽ hết hạn và khách sẽ khiếu nại rằng sàn cố tình để họ mất Điểm.

**d) `amount` dùng dấu âm/dương thay vì 2 cột riêng**
`+100` là nhận Điểm, `-50` là tiêu Điểm. Nhờ đó số dư luôn kiểm chứng được bằng:

```sql
SELECT SUM(amount) FROM coin_transactions
WHERE user_id = ? AND (expires_at IS NULL OR expires_at > NOW())
-- Phải KHỚP với user_coins.balance
```

**e) `streakCount` — Gamification và tại sao nó hiệu quả**
Chuỗi ngày liên tiếp khai thác hiệu ứng tâm lý **loss aversion** (sợ mất mát): khách đã điểm danh 29 ngày liên tiếp sẽ rất ngại bỏ lỡ ngày 30. Hệ thống thường tăng phần thưởng theo streak (ngày 7 nhiều Điểm hơn ngày 1) để khuếch đại hiệu ứng.
Cách kiểm tra streak khi điểm danh:

```
nếu lastCheckinAt là NGÀY HÔM QUA      → streakCount += 1
nếu lastCheckinAt là HÔM NAY           → từ chối, đã điểm danh rồi
nếu lastCheckinAt cũ hơn hôm qua       → streakCount = 1  (đứt chuỗi, làm lại)
```

> Lưu ý kỹ thuật: so sánh phải theo **ngày lịch tại múi giờ của người dùng**, không phải theo số giờ chênh lệch. Nếu so theo 24 giờ, khách điểm danh 23h hôm nay và 8h sáng mai sẽ bị tính là đứt chuỗi — một lỗi rất hay gặp.

**f) `ORDER_REFUND` — hoàn Điểm khi huỷ đơn**
Khách tiêu 500 Điểm cho đơn hàng, sau đó đơn bị huỷ → phải **trả lại 500 Điểm**. Nếu không có loại giao dịch này, khách mất Điểm oan và sẽ khiếu nại. Đây là ví dụ điển hình cho thấy mọi luồng "tiêu" đều phải có luồng "hoàn" tương ứng.

---

# 5. NHÓM ĐƠN HÀNG

## 5.1. `CheckoutSession` — Phiên thanh toán
Bảng: `checkout_sessions`

```java
@Entity
@Table(name = "checkout_sessions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CheckoutSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — khách đang thanh toán

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;           // TỔNG tiền của cả phiên (gộp nhiều shop)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;             // INITIATED / PAYMENT_PENDING / COMPLETED / FAILED / EXPIRED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String cartSnapshotJson;          // NULLABLE — Ảnh chụp JSON toàn bộ giỏ hàng lúc bắt đầu thanh toán

    @Column(nullable = false)
    private OffsetDateTime expiresAt;          // Hạn chót thanh toán (thường 15 phút) — hết hạn sẽ nhả tồn kho

    private OffsetDateTime completedAt;        // NULLABLE — Thời điểm thanh toán thành công

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm bắt đầu phiên

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "checkout_session_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua Order.checkoutSessionId
    private List<Order> orders;               // Các đơn hàng được tách ra theo từng shop
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Chuyển sang chờ thanh toán — UC-ORD-07 */
    public void markPaymentPending() {
        if (this.status != SessionStatus.INITIATED) throw new IllegalStateException("Sai trạng thái phiên");
        this.status = SessionStatus.PAYMENT_PENDING;
    }

    /** Thanh toán thành công toàn phiên — UC-PAY-03 */
    public void complete(OffsetDateTime now) {
        if (this.status != SessionStatus.INITIATED && this.status != SessionStatus.PAYMENT_PENDING) throw new IllegalStateException("Phiên không còn chờ thanh toán");
        this.status = SessionStatus.COMPLETED;
        this.completedAt = now;
    }

    /** Cổng báo thất bại — UC-PAY-03 */
    public void fail() {
        if (this.status == SessionStatus.PAYMENT_PENDING) this.status = SessionStatus.FAILED;
    }

    /** Hết hạn 15 phút — Sweeper nhả tồn kho — UC-ORD-08 */
    public void expire() {
        if (this.status == SessionStatus.INITIATED || this.status == SessionStatus.PAYMENT_PENDING) this.status = SessionStatus.EXPIRED;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Đây là khái niệm CỐT LÕI nhất của mô hình sàn đa người bán (multi-vendor)**
Khách đặt 1 lần, nhưng mua hàng từ 3 shop khác nhau. Hệ thống phải xử lý:
- **1 lần thanh toán** (khách chỉ quẹt thẻ 1 lần, tổng 1 số tiền) → `CheckoutSession`
- **3 đơn hàng riêng** (mỗi shop tự đóng gói, tự giao, tự nhận tiền) → 3 `Order`

```
                CheckoutSession (1.500.000đ — khách trả 1 lần)
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
   Order (Shop A)  Order (Shop B)  Order (Shop C)
     500.000đ        700.000đ        300.000đ
   (giao riêng)     (giao riêng)    (giao riêng)
```

> Đây gọi là **Order Splitting**. Vì sao không gộp 1 đơn duy nhất? Vì mỗi shop có kho riêng, đơn vị vận chuyển riêng, tiến độ giao khác nhau, và **tiền phải vào ví riêng của từng shop**. Shop A giao xong không có lý do gì phải chờ Shop C mới được nhận tiền.

**b) `expiresAt` — cơ chế hết hạn bảo vệ tồn kho**
Khi phiên bắt đầu, tồn kho bị giữ chỗ (`StockReservation`). Nếu khách bỏ ngang, tồn kho sẽ bị treo mãi mãi nếu không có cơ chế hết hạn. Một job chạy nền quét các phiên quá `expiresAt` → chuyển `status = EXPIRED` → nhả `reservedCount`.
Con số 15 phút là cân bằng giữa "đủ để khách thao tác ngân hàng" và "không giữ hàng quá lâu".

**c) `cartSnapshotJson` — chốt nội dung đơn hàng**
Khi phiên đã bắt đầu, nếu khách mở tab khác và sửa giỏ hàng, phiên này **không được thay đổi theo**. Snapshot đóng băng nội dung để đảm bảo "cái khách nhìn thấy khi bấm thanh toán chính là cái được tạo thành đơn".

**d) Vòng đời trạng thái**

```
INITIATED ──(chuyển sang cổng thanh toán)──► PAYMENT_PENDING
PAYMENT_PENDING ──(webhook báo thành công)──► COMPLETED  → tạo các Order
PAYMENT_PENDING ──(webhook báo thất bại)──► FAILED       → nhả tồn kho
INITIATED/PAYMENT_PENDING ──(quá 15 phút)──► EXPIRED     → nhả tồn kho
```

---

## 5.2. `Order` — Đơn hàng (theo từng shop)
Bảng: `orders`

```java
@Entity
@Table(name = "orders")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false, length = 32)
    private String orderCode;                 // Mã đơn công khai cho khách/CSKH tra cứu (vd: TR-20261015-000123) — Hash Index

    @Column(nullable = false)
    private Long customerId;                  // FK → users.id — người mua

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop bán (mỗi đơn CHỈ thuộc 1 shop)

    // Quan hệ ánh xạ tới VendorProfile qua users.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private VendorProfile vendorProfile;      // Profile shop bán hàng

    private Long checkoutSessionId;           // NULLABLE — FK → checkout_sessions.id, phiên thanh toán gốc

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String shippingAddressSnapshotJson; // Ảnh chụp JSON địa chỉ giao hàng tại thời điểm đặt

    // ---- Các thành phần tiền ----
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subTotal;              // Tổng tiền hàng trước mọi khuyến mãi và phí ship

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingFee;           // Phí vận chuyển gốc

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO; // Tổng giảm giá từ voucher của SHOP

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal platformDiscountAmount = BigDecimal.ZERO; // Tổng giảm giá từ voucher của SÀN

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingDiscountAmount = BigDecimal.ZERO; // Tổng giảm từ mã miễn phí vận chuyển

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal;            // SỐ TIỀN KHÁCH THỰC TRẢ lúc thanh toán (sau tất cả giảm giá) — BẤT BIẾN sau khi thanh toán; phần đã hoàn nằm ở refundedAmount, số còn giữ = netPaidAmount()

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO; // Tổng đã hoàn cho khách (mặc định 0) — UI hiển thị "Đã hoàn một phần" khi paymentStatus = PARTIALLY_REFUNDED

    // ---- Thanh toán, Nguồn vốn (Funding) & Trạng thái ----
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;     // NULLABLE — VNPAY / MOMO / ZALOPAY / VIETQR / INTERNAL_WALLET / COD

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentStatus paymentStatus;      // NULLABLE — Bản sao tóm tắt PaymentTransaction.status (denormalize để đọc nhanh)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FundingStatus fundingStatus;      // UNFUNDED | PENDING | FUNDED | FAILED — gán UNFUNDED khi tạo đơn

    private OffsetDateTime paymentConfirmedAt; // NULLABLE — Thời điểm KHÁCH đã trả tiền: online = IPN thành công; COD = lúc giao hàng, khách đưa tiền mặt cho tài xế

    private OffsetDateTime fundedAt;           // NULLABLE — Thời điểm tiền VÀO QUỸ ESCROW của sàn: online = paymentConfirmedAt; COD = Staff xác nhận GHN đã chuyển tiền thu hộ (UC-LOG-03) — hai mốc khác nhau với COD

    private OffsetDateTime deliveredAt;        // NULLABLE — Thời điểm giao hàng thành công (DELIVERED) để tính mốc 7 ngày

    private OffsetDateTime customerConfirmedAt;// NULLABLE — Thời điểm khách hàng bấm 'Đã nhận được hàng'

    private OffsetDateTime completedAt;        // NULLABLE — Thời điểm đơn chuyển COMPLETED (giải ngân Escrow)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;               // Trạng thái đơn con (Phụ lục A — OrderStatus, máy trạng thái ALLOWED bên dưới)
                                              // - Online Flow: PENDING_PAYMENT → PAID → CONFIRMED → PREPARING → READY_FOR_PICKUP → DELIVERING → DELIVERED → COMPLETED
                                              // - COD Flow:    PENDING_PAYMENT → CONFIRMED → PREPARING → READY_FOR_PICKUP → DELIVERING → DELIVERED → PAID → COMPLETED
                                              // - Ngoại lệ:     DELIVERY_FAILED, RETURNING, RETURNED_TO_VENDOR, CANCELLED, CANCELLED_BY_VENDOR, AUTO_CANCELLED, RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED, PARTIALLY_REFUNDED, REFUNDED

    @Column(length = 100)
    private String ghnOrderCode;              // NULLABLE — Mã vận đơn bên GHN, để tra cứu hành trình

    private OffsetDateTime shippingExpectedAt; // NULLABLE — Ngày dự kiến giao hàng

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống 2 luồng cùng đổi trạng thái đơn

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo đơn

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    @Transient
    private Boolean isReviewed;               // Trường tính toán — khách đã đánh giá đơn này chưa

    @Column(length = 500)
    private String buyerNote;                 // NULLABLE — "Lời nhắn cho Shop" khách nhập ở checkout (≤ 500 ký tự) — hiển thị ở UC-LOG-01

    @Builder.Default
    @Column(nullable = false)
    private Integer coinUsedCount = 0;          // Số Điểm khách đã dùng cho đơn này (UC-PRO-10)

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal coinDiscountAmount = BigDecimal.ZERO; // Số tiền quy đổi tương ứng từ Điểm (1 Điểm = 1₫)

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "order_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua OrderItem.orderId
    private List<OrderItem> items;            // Các dòng sản phẩm trong đơn

    @OneToMany
    @JoinColumn(name = "order_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua OrderDiscount.orderId
    private List<OrderDiscount> discounts;    // Các khoản giảm giá đã áp

    // Bảng chuyển trạng thái hợp lệ — đồng bộ Document_Vi.md mục 3.4.2 (online & COD)
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.ofEntries(   // = Document_Vi.md 3.4.2
        Map.entry(OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PAID, OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.CANCELLED_BY_VENDOR, OrderStatus.AUTO_CANCELLED)),   // CANCELLED_BY_VENDOR: shop từ chối đơn COD chờ xác nhận / đơn online chưa trả tiền
        Map.entry(OrderStatus.PAID, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.CANCELLED_BY_VENDOR, OrderStatus.COMPLETED, OrderStatus.RETURN_REQUESTED)),   // COMPLETED/RETURN_REQUESTED: chỉ COD đã giao
        Map.entry(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED, OrderStatus.CANCELLED_BY_VENDOR)),
        Map.entry(OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED, OrderStatus.CANCELLED_BY_VENDOR)),
        Map.entry(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.DELIVERING)),
        Map.entry(OrderStatus.DELIVERING, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.DELIVERY_FAILED)),
        Map.entry(OrderStatus.DELIVERY_FAILED, EnumSet.of(OrderStatus.DELIVERING, OrderStatus.RETURNING)),
        Map.entry(OrderStatus.RETURNING, EnumSet.of(OrderStatus.RETURNED_TO_VENDOR)),
        Map.entry(OrderStatus.RETURNED_TO_VENDOR, EnumSet.of(OrderStatus.REFUNDED)),   // đơn đã trả online (VNPay / VietQR) bị GHN hoàn về shop → tự tạo RefundRequest REFUND_ONLY, hoàn theo UC-SUP-04
        Map.entry(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.PAID, OrderStatus.COMPLETED, OrderStatus.RETURN_REQUESTED)),
        Map.entry(OrderStatus.RETURN_REQUESTED, EnumSet.of(OrderStatus.RETURN_APPROVED, OrderStatus.RETURN_REJECTED)),
        Map.entry(OrderStatus.RETURN_APPROVED, EnumSet.of(OrderStatus.REFUNDED, OrderStatus.PARTIALLY_REFUNDED, OrderStatus.COMPLETED)),   // COMPLETED: khách không gửi hàng hoàn đúng hạn (RefundRequest.RETURN_FAILED)
        Map.entry(OrderStatus.PARTIALLY_REFUNDED, EnumSet.of(OrderStatus.RETURN_REQUESTED, OrderStatus.COMPLETED)),   // trả tiếp món khác trong hạn 7 ngày / hết hạn → giải ngân phần còn lại
        Map.entry(OrderStatus.RETURN_REJECTED, EnumSet.of(OrderStatus.COMPLETED)));
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bảo vệ máy trạng thái theo bảng ALLOWED (Document_Vi.md 3.4.2) */
    private void transitionTo(OrderStatus next) {
        // ALLOWED = hằng Map<OrderStatus, Set<OrderStatus>> khai báo ngay trên khối phương thức
        if (!ALLOWED.getOrDefault(this.status, Set.of()).contains(next))
            throw new IllegalStateException("Đơn " + this.orderCode + ": không thể chuyển " + this.status + " → " + next);
        this.status = next;
    }

    /** Đơn thanh toán khi nhận hàng */
    private boolean isCod() {
        return this.paymentMethod == PaymentMethod.COD;
    }

    /** Xác nhận đã thanh toán (online qua IPN / COD sau khi GHN đối soát tiền thu hộ) — đồng bộ paymentStatus & fundingStatus — UC-PAY-03 */
    public void markPaid(OffsetDateTime now) {
        if (isCod() && this.status != OrderStatus.DELIVERED) throw new IllegalStateException("COD chỉ PAID sau khi giao & kế toán đối soát");
        transitionTo(OrderStatus.PAID);   // online: PENDING_PAYMENT → PAID; COD: DELIVERED → PAID
        this.paymentStatus = PaymentStatus.SUCCESS;
        if (this.paymentConfirmedAt == null) this.paymentConfirmedAt = now;   // COD đã ghi lúc giao hàng (markDelivered)
        this.fundingStatus = FundingStatus.FUNDED;
        this.fundedAt = now;                                                  // tiền vào quỹ Escrow
    }

    /** Đồng bộ khi PaymentTransaction thất bại — UC-PAY-03 */
    public void markPaymentFailed() {
        if (this.status != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("Đơn không chờ thanh toán");
        this.paymentStatus = PaymentStatus.FAILED;   // trạng thái đơn giữ PENDING_PAYMENT, Sweeper hủy khi hết hạn
    }

    /** Shop xác nhận đơn (online đã PAID / COD chưa thu tiền) — UC-LOG-01 */
    public void confirmByVendor() {
        if (!isCod() && this.status != OrderStatus.PAID) throw new IllegalStateException("Đơn online phải PAID trước khi xác nhận");
        transitionTo(OrderStatus.CONFIRMED);   // online: PAID → CONFIRMED; COD: PENDING_PAYMENT → CONFIRMED
        if (isCod()) this.fundingStatus = FundingStatus.UNFUNDED;
    }

    /** Bắt đầu đóng gói — UC-LOG-01 */
    public void startPreparing() {
        transitionTo(OrderStatus.PREPARING);
    }

    /** In nhãn, chờ lấy hàng — UC-LOG-02 */
    public void readyForPickup() {
        transitionTo(OrderStatus.READY_FOR_PICKUP);
    }

    /** GHN đã lấy hàng (webhook picked) — UC-LOG-03 */
    public void startDelivering() {
        transitionTo(OrderStatus.DELIVERING);
    }

    /** Giao thành công (webhook delivered) — bắt đầu đếm 7 ngày (COD: chờ GHN đối soát) — UC-LOG-03 */
    public void markDelivered(OffsetDateTime now) {
        transitionTo(OrderStatus.DELIVERED);
        this.deliveredAt = now;
        if (isCod()) {
            this.paymentConfirmedAt = now;                  // khách đã trả tiền mặt cho tài xế
            this.fundingStatus = FundingStatus.PENDING;     // nhưng tiền chưa về quỹ Escrow (fundedAt đặt khi đối soát)
        }
    }

    /** Khách bấm "Đã nhận được hàng" (hoặc Cron tự xác nhận) — trước khi complete() — UC-ORD-11 */
    public void markCustomerConfirmed(OffsetDateTime now) {
        if (this.deliveredAt == null || (this.status != OrderStatus.DELIVERED && this.status != OrderStatus.PAID))
            throw new IllegalStateException("Chỉ xác nhận khi đơn đã giao");
        if (this.customerConfirmedAt == null) this.customerConfirmedAt = now;   // idempotent
    }

    /** GHN báo một lần giao thất bại — UC-LOG-03 */
    public void markDeliveryFailed() {
        transitionTo(OrderStatus.DELIVERY_FAILED);
    }

    /** GHN giao lại (tổng tối đa 3 lần giao theo chính sách GHN) — UC-LOG-03 */
    public void retryDelivery() {
        transitionTo(OrderStatus.DELIVERING);
    }

    /** GHN hoàn hàng về shop sau khi giao thất bại — UC-LOG-03 */
    public void startReturning() {
        transitionTo(OrderStatus.RETURNING);
    }

    /** Shop đã nhận lại hàng hoàn — UC-LOG-03 */
    public void markReturnedToVendor() {
        transitionTo(OrderStatus.RETURNED_TO_VENDOR);
    }

    /** Hủy đơn — chỉ 3 trạng thái hủy, chỉ trước khi bàn giao vận chuyển — UC-ORD-10 */
    public void cancel(OrderStatus cancelType) {
        if (cancelType != OrderStatus.CANCELLED && cancelType != OrderStatus.CANCELLED_BY_VENDOR && cancelType != OrderStatus.AUTO_CANCELLED)
            throw new IllegalArgumentException("Loại hủy không hợp lệ: " + cancelType);
        if (this.status == OrderStatus.PAID && this.deliveredAt != null) throw new IllegalStateException("GNE-ORD-4092");   // COD đã giao
        transitionTo(cancelType);
    }

    /** Khách mở yêu cầu trả hàng (đồng bộ RefundRequest.REQUESTED) — UC-SUP-01 */
    public void requestReturn() {
        if (this.deliveredAt == null) throw new IllegalStateException("Chỉ trả hàng sau khi đã giao");
        transitionTo(OrderStatus.RETURN_REQUESTED);   // online: DELIVERED; COD: DELIVERED hoặc PAID (đã đối soát); trả tiếp đợt sau: PARTIALLY_REFUNDED
    }

    /** Đồng bộ khi RefundRequest → APPROVED — UC-SUP-02 */
    public void approveReturn() {
        transitionTo(OrderStatus.RETURN_APPROVED);
    }

    /** Đồng bộ khi RefundRequest → REJECTED — UC-SUP-03 */
    public void rejectReturn() {
        transitionTo(OrderStatus.RETURN_REJECTED);
    }

    /** Đồng bộ khi RefundRequest → REFUNDED; cộng dồn refundedAmount: đủ grandTotal → REFUNDED, chưa đủ → PARTIALLY_REFUNDED (hoàn nhiều đợt được) — UC-SUP-04 */
    public void markRefunded(BigDecimal amount) {
        if (amount.signum() <= 0 || this.refundedAmount.add(amount).compareTo(this.grandTotal) > 0)
            throw new IllegalArgumentException("Số hoàn phải > 0 và tổng hoàn không vượt grandTotal");
        this.refundedAmount = this.refundedAmount.add(amount);
        boolean full = this.refundedAmount.compareTo(this.grandTotal) >= 0;
        transitionTo(full ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED);   // từ RETURN_APPROVED, hoặc RETURNED_TO_VENDOR (hoàn toàn bộ)
        this.paymentStatus = full ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;   // UI: "Đã hoàn một phần {refundedAmount}"
    }

    /** Số tiền khách đã trả còn nằm lại ở đơn (sau các lần hoàn) — UC-SUP-04 */
    public BigDecimal netPaidAmount() {
        return this.grandTotal.subtract(this.refundedAmount);
    }

    /** Hoàn tất (khách xác nhận hoặc tự động sau 7 ngày) — UC-ORD-12 */
    public void complete(OffsetDateTime now) {
        if (this.status == OrderStatus.DELIVERED && isCod()) throw new IllegalStateException("COD phải đối soát (PAID) trước khi hoàn tất");
        if (this.status == OrderStatus.PAID && this.deliveredAt == null) throw new IllegalStateException("Đơn chưa giao");
        transitionTo(OrderStatus.COMPLETED);   // online: DELIVERED; COD: PAID (sau giao); hoặc RETURN_REJECTED / PARTIALLY_REFUNDED / RETURN_APPROVED (trả hàng thất bại)
        this.completedAt = now;
    }

    /** Tính lại tổng thanh toán (không âm) — CHỈ trước khi thanh toán (giỏ/checkout); sau thanh toán grandTotal bất biến — UC-PRO-03 */
    public BigDecimal recalculateGrandTotal() {
        if (this.status != OrderStatus.PENDING_PAYMENT || this.paymentConfirmedAt != null)
            throw new IllegalStateException("grandTotal đã chốt khi thanh toán — hoàn tiền ghi vào refundedAmount");
        this.grandTotal = this.subTotal.add(this.shippingFee)
            .subtract(this.discountAmount).subtract(this.platformDiscountAmount)
            .subtract(this.shippingDiscountAmount).max(BigDecimal.ZERO);
        return this.grandTotal;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Công thức tính tiền đầy đủ**

```
grandTotal = subTotal
           + shippingFee
           - discountAmount            (mã của shop)
           - platformDiscountAmount    (mã của sàn)
           - shippingDiscountAmount    (mã freeship)
           - coinDiscountAmount          (Điểm tích luỹ)
```

**Vì sao KHÔNG gộp tất cả thành 1 cột `totalDiscount`?** Vì mỗi loại giảm giá có **người chi trả khác nhau**:
- `discountAmount` (shop tài trợ) → trừ vào tiền shop nhận được.
- `platformDiscountAmount`, `shippingDiscountAmount`, `coinDiscountAmount` (sàn tài trợ) → sàn chịu, **shop vẫn nhận đủ**.

Gộp lại là mất khả năng đối soát tài chính, và tháng sau sẽ không ai giải thích nổi tiền đi đâu.

**b) `shippingAddressSnapshotJson` — bảo vệ đơn hàng khỏi thay đổi tương lai**
Nếu chỉ lưu `addressId`, khi khách đổi địa chỉ nhà 6 tháng sau, **đơn hàng cũ sẽ hiển thị địa chỉ mới** → sai lệch với vận đơn đã giao, không thể giải quyết khiếu nại. Snapshot đóng băng dữ liệu tại thời điểm giao dịch.

**c) `paymentStatus` là dữ liệu phi chuẩn hoá có chủ đích**
Trạng thái thanh toán "thật" nằm ở `PaymentTransaction.status`. Nhưng màn hình "Danh sách đơn hàng" cần hiển thị 50 đơn cùng lúc — nếu mỗi đơn phải JOIN sang bảng giao dịch thì rất chậm. Sao chép một bản tóm tắt sang `orders` giúp đọc bằng 1 query duy nhất.
> **Cái giá phải trả:** hai nơi có thể lệch nhau. Bắt buộc phải cập nhật cả hai trong **cùng một transaction**.

**d) Vòng đời `OrderStatus` (19 trạng thái Canonical) và mối liên hệ với dòng tiền**

```
[Online Payment Flow]:
PENDING_PAYMENT ──(thanh toán OK)──► PAID ──(shop xác nhận)──► CONFIRMED
  ──(chuẩn bị hàng)──► PREPARING ──(bàn giao ĐVVC)──► READY_FOR_PICKUP
  ──(shipper lấy hàng đi giao)──► DELIVERING ──(giao thành công)──► DELIVERED
  ──(qua 7 ngày bảo hộ)──► COMPLETED (Giải ngân Escrow vào ví shop)

[COD Flow]:
PENDING_PAYMENT ──(shop xác nhận)──► CONFIRMED ──► PREPARING ──► READY_FOR_PICKUP ──► DELIVERING
  ──(khách nhận hàng & trả tiền)──► DELIVERED ──(đối soát COD)──► PAID ──► COMPLETED

[Ngoại lệ & Trả hàng]:
DELIVERY_FAILED ──► RETURNING ──► RETURNED_TO_VENDOR ──(đơn đã trả tiền online/ví: Cron AutoRefundReturnedOrdersJob)──► REFUNDED
DELIVERED ──► RETURN_REQUESTED ──► RETURN_APPROVED ──► REFUNDED (hoàn đủ) / PARTIALLY_REFUNDED (hoàn một phần) (hoặc RETURN_REJECTED)
PARTIALLY_REFUNDED ──(trả tiếp món khác trong 7 ngày)──► RETURN_REQUESTED … / ──(hết hạn)──► COMPLETED (giải ngân phần còn lại)
RETURN_APPROVED ──(khách không gửi hàng hoàn đúng hạn: RefundRequest RETURN_FAILED)──► COMPLETED
PENDING_PAYMENT / PAID / CONFIRMED / PREPARING ──► CANCELLED (khách) / CANCELLED_BY_VENDOR (shop)
PENDING_PAYMENT ──(quá hạn thanh toán)──► AUTO_CANCELLED
```

> **Điểm quan trọng nhất:** `DELIVERED` ≠ `COMPLETED`. Giao tới tay khách **không** đồng nghĩa shop được nhận tiền ngay. Phải qua **thời gian bảo vệ người mua** (7 ngày) mới chuyển `COMPLETED` và tự động giải ngân từ Escrow sang ví vendor. Đây là cơ chế bảo vệ người mua — trái tim niềm tin của sàn TMĐT.

**d2) Hai mốc tiền của đơn: `paymentConfirmedAt` ≠ `fundedAt`**
| Mốc | Online (VNPay) | COD (GHN thu hộ) |
|---|---|---|
| `paymentConfirmedAt` — khách đã trả | IPN thành công (`markPaid`) | Lúc giao, khách đưa tiền mặt cho bưu tá GHN (`markDelivered`) |
| `fundedAt` — tiền vào quỹ Escrow | = `paymentConfirmedAt` | Staff xác nhận GHN đã chuyển tiền thu hộ theo bảng đối soát COD (`markPaid`, UC-LOG-03) |

Khoảng cách giữa hai mốc với COD chính là thời gian tiền nằm ở GHN (thường 2–3 ngày làm việc theo chu kỳ đối soát của GHN).

**d3) `grandTotal` bất biến sau thanh toán**
`grandTotal` là số khách đã trả lúc thanh toán và dùng làm trần cho tổng hoàn (`refundedAmount ≤ grandTotal`). Hoàn tiền **không** sửa `grandTotal`; số còn lại là `netPaidAmount() = grandTotal − refundedAmount`. `recalculateGrandTotal()` chỉ chạy khi đơn còn `PENDING_PAYMENT`. (`grandTotal` của đơn và `EscrowLedger.grossAmount` vốn khác nhau: `grossAmount` không gồm phí ship và các khoản sàn tài trợ.)

**e) Vì sao `Order` trỏ tới `users.id` của vendor mà không phải `vendor_profiles.id`?**
Trong JPA: `@JoinColumn(name = "vendor_id", referencedColumnName = "id")`. Chọn `users.id` làm khoá ngoại liên kết vì đó là **danh tính gốc** (User identity) của người bán, tồn tại độc lập với hồ sơ shop. Nếu sau này vendor tạo lại hồ sơ shop (profile id đổi), các đơn hàng cũ vẫn trỏ chính xác về tài khoản người bán.

---

## 5.3. `OrderItem` — Dòng sản phẩm trong đơn
Bảng: `order_items`

```java
@Entity
@Table(name = "order_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long orderId;                     // FK → orders.id — thuộc đơn hàng nào

    @Column(nullable = false)
    private Long productVariantId;            // FK → product_variants.id — biến thể đã mua

    @Column(length = 255)
    private String productNameSnapshot;       // NULLABLE — Tên sản phẩm CHỤP LẠI lúc mua (bất biến)

    @Column(length = 100)
    private String skuSnapshot;               // NULLABLE — Mã SKU chụp lại lúc mua

    @Column(nullable = false)
    private Integer quantity;                 // Số lượng mua

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;             // Đơn giá CHỐT tại thời điểm mua

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;             // Thành tiền dòng = unitPrice × quantity

    // ---- Phân bổ giảm giá xuống từng dòng ----
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal proratedDiscount = BigDecimal.ZERO; // Phần giảm giá chung được chia tỷ lệ cho dòng này

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal platformDiscount = BigDecimal.ZERO; // Phần giảm do SÀN tài trợ, phân bổ cho dòng này

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal shopDiscount = BigDecimal.ZERO; // Phần giảm do SHOP tài trợ, phân bổ cho dòng này

    // ---- Kết toán tài chính cho dòng này ----
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;           // Doanh thu gộp của dòng, dùng làm căn cứ tính hoa hồng

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;        // Tỷ lệ hoa hồng CHỤP LẠI lúc bán. VD 0.0550 = 5.5%

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal commissionAmount;      // Số tiền hoa hồng sàn thu = grossAmount × commissionRate

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal vendorReceivable;      // Số tiền shop thực nhận cho dòng này

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal coinDiscount = BigDecimal.ZERO; // Phần giảm từ Điểm, phân bổ cho dòng này (sàn chịu — shop vẫn nhận đủ)

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_variant_id", insertable = false, updatable = false)
    private ProductVariant productVariant;    // Biến thể sản phẩm
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Chụp tỷ lệ & tính hoa hồng dòng hàng — UC-PAY-05 */
    public void applyCommission(BigDecimal rate) {
        this.commissionRate = rate;
        this.commissionAmount = this.grossAmount.multiply(rate).setScale(0, RoundingMode.HALF_UP);
        this.vendorReceivable = this.grossAmount.subtract(this.commissionAmount);
    }

    /** Gán phần chiết khấu phân bổ (phần dư dồn dòng cuối) — UC-PRO-03 */
    public void allocateDiscount(BigDecimal prorated) {
        this.proratedDiscount = prorated;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Phân bổ giảm giá theo tỷ lệ (Prorated Discount) — vì sao bắt buộc phải làm?**
Voucher giảm 100.000đ áp cho **cả đơn**, nhưng đơn có 3 sản phẩm. Khi khách chỉ trả lại 1 sản phẩm, hoàn bao nhiêu tiền?

```
Đơn:  SP A = 600.000đ,  SP B = 300.000đ,  SP C = 100.000đ   (tổng 1.000.000đ)
Voucher giảm 100.000đ  →  phân bổ theo tỷ trọng:
   A: 100.000 × (600/1000) = 60.000đ  →  A thực trả 540.000đ
   B: 100.000 × (300/1000) = 30.000đ  →  B thực trả 270.000đ
   C: 100.000 × (100/1000) = 10.000đ  →  C thực trả  90.000đ
```

Khách trả lại SP B → hoàn đúng **270.000đ**, không phải 300.000đ (hoàn dư 30.000đ) cũng không phải 200.000đ (hoàn thiếu).
> **Bẫy làm tròn:** chia theo tỷ lệ luôn sinh số lẻ. Kỹ thuật chuẩn là tính từng dòng rồi **dồn phần dư vào dòng cuối cùng**, đảm bảo tổng phân bổ **khớp tuyệt đối** với số giảm ban đầu. Sai 1 đồng ở đây, nhân với hàng triệu đơn, là lệch sổ.

**b) Vì sao tách 4 loại giảm giá riêng cho từng dòng?**
Vì khi hoàn tiền phải biết **trả lại cho ai**:
- Phần `platformDiscount`, `coinDiscount` → sàn đã chịu → khi hoàn, sàn thu lại phần đó, shop không bị ảnh hưởng.
- Phần `shopDiscount` → shop đã chịu → khi hoàn, ghi ngược lại vào tài khoản shop.

Nếu chỉ có 1 cột tổng, việc hoàn tiền sẽ **luôn sai với một trong hai bên**.

**c) `commissionRate` được CHỤP LẠI ở cấp dòng**
Tỷ lệ này copy từ `VendorProfile.commissionRate` (hoặc `Category.commissionRate`) tại thời điểm đặt hàng. Mai sau sàn tăng hoa hồng lên 8%, các đơn cũ vẫn tính theo 5.5%.
> **Đây là yêu cầu pháp lý**, không chỉ là kỹ thuật: hợp đồng giữa sàn và shop áp dụng mức phí tại thời điểm giao dịch. Tính lại theo mức hiện tại là vi phạm thoả thuận.

**d) Chuỗi công thức tài chính đầy đủ của 1 dòng**

```
lineTotal        = unitPrice × quantity
grossAmount      = lineTotal - shopDiscount            (sàn chỉ ăn hoa hồng trên phần shop thật sự thu)
commissionAmount = grossAmount × commissionRate
vendorReceivable = grossAmount - commissionAmount
```

Lưu ý tinh tế: hoa hồng **không** tính trên `platformDiscount`. Nghĩa là nếu sàn giảm giá cho khách, sàn tự chịu và **không ăn hoa hồng trên phần mình đã bù**. Đây là chính sách công bằng với người bán.

---

## 5.4. `OrderDiscount` — Chi tiết các mã đã áp cho đơn
Bảng: `order_discounts`

```java
@Entity
@Table(name = "order_discounts")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class OrderDiscount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long orderId;                     // FK → orders.id

    private Long voucherId;                   // NULLABLE — FK → vouchers.id. NULL khi là giảm giá từ Điểm

    @Column(length = 50)
    private String voucherCodeSnapshot;       // NULLABLE — Mã voucher CHỤP LẠI, phòng khi voucher bị xoá

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;        // Số tiền mã này đã giảm

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VoucherType voucherType;          // NULLABLE — PLATFORM / SHOP / FREESHIP / PAYMENT. NULL = dòng giảm giá từ Điểm (xem Order.coinDiscountAmount)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FundingParty fundedBy;            // NULLABLE — AI TRẢ TIỀN: PLATFORM hay VENDOR (enum, cùng enum với Voucher.sponsorType)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Voucher do sàn tài trợ (không trừ tiền shop) — UC-PRO-03 */
    public boolean isPlatformFunded() {
        return this.fundedBy == FundingParty.PLATFORM;
    }
}
```

### 💡 Kiến thức chuyên sâu

**`fundedBy` — trường quan trọng nhất trong bảng này**
Đây là cầu nối giữa nghiệp vụ khuyến mãi và báo cáo tài chính:
- `fundedBy = PLATFORM` → ghi nhận **chi phí marketing của sàn** (voucher sàn); dashboard UC-ADM-06 cộng các dòng này.
- `fundedBy = VENDOR` → trừ vào khoản phải trả cho shop, sàn không mất gì.

**`voucherId` nullable, nhưng `voucherCodeSnapshot` luôn có**
Kể cả khi voucher bị admin xoá khỏi hệ thống (`voucherId` thành `NULL` do `ON DELETE SET NULL`), `voucherCodeSnapshot` vẫn giúp hoá đơn cũ hiển thị đầy đủ *"Đã áp mã SALE50K"*.

**Vì sao cần bảng này khi `Order` đã có các cột tổng?**
`Order.platformDiscountAmount = 150.000đ` chỉ cho biết tổng. Bảng này cho biết **150.000đ đó đến từ những mã nào** — cần thiết để báo cáo hiệu quả từng chiến dịch marketing, và để hoàn đúng khi huỷ đơn.

---

## 5.5. `StockReservation` — Giữ chỗ tồn kho
Bảng: `stock_reservations`

```java
@Entity
@Table(name = "stock_reservations")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class StockReservation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productVariantId;            // FK → product_variants.id — giữ chỗ cho biến thể nào

    @Column(nullable = false)
    private Long orderId;                     // FK → orders.id — giữ cho đơn hàng nào

    @Column(nullable = false)
    private Integer reservedQuantity;         // Số lượng đang giữ

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockReservationStatus status;    // ACTIVE (đang giữ) / CONFIRMED (đã chốt) / EXPIRED / RELEASED

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống Sweeper và Webhook cùng đổi trạng thái 1 lượt giữ chỗ

    @Column(nullable = false)
    private OffsetDateTime expiresAt;          // Hạn giữ chỗ — quá hạn sẽ tự nhả

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm bắt đầu giữ

    private OffsetDateTime releasedAt;         // NULLABLE — Thời điểm thực sự nhả chỗ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đã quá hạn giữ chỗ — UC-ORD-08 */
    public boolean isExpired(OffsetDateTime now) {
        return this.status == StockReservationStatus.ACTIVE && this.expiresAt.isBefore(now);
    }

    /** Chốt khi thanh toán thành công — UC-PAY-03 */
    public void confirm() {
        if (this.status != StockReservationStatus.ACTIVE) throw new IllegalStateException("GNE-INV-4093: giữ chỗ đã hết hạn");
        this.status = StockReservationStatus.CONFIRMED;   // @Version: IPN và Sweeper chỉ một bên thắng
    }

    /** Nhả giữ chỗ (idempotent) — UC-ORD-08 */
    public boolean release(OffsetDateTime now, boolean expired) {
        if (this.status != StockReservationStatus.ACTIVE) return false;   // đã CONFIRMED/RELEASED → không nhả lần 2
        this.status = expired ? StockReservationStatus.EXPIRED : StockReservationStatus.RELEASED;
        this.releasedAt = now;
        return true;   // true → service mới được giảm reservedCount
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Vai trò: sổ chi tiết cho cột tổng `ProductVariant.reservedCount`**
`reservedCount` chỉ là con số tổng. Bảng này ghi **từng lượt giữ chỗ là của đơn nào, hết hạn khi nào**. Nhờ đó luôn kiểm chứng được:

```sql
SELECT SUM(reserved_quantity) FROM stock_reservations
WHERE product_variant_id = ? AND status = 'ACTIVE'
-- Kết quả phải KHỚP với product_variants.reserved_count
```

Nếu lệch, hệ thống có lỗi và có thể tự chữa (self-healing) bằng cách tính lại từ bảng chi tiết.

**b) Vòng đời giữ chỗ**

```
Tạo đơn         → ACTIVE      (reservedCount += n)
Thanh toán OK   → CONFIRMED   (stockCount -= n; reservedCount -= n)   ← xuất kho thật
Khách huỷ đơn   → RELEASED    (reservedCount -= n)
Quá expiresAt   → EXPIRED     (reservedCount -= n)  ← do job nền xử lý
```

**c) Vì sao phải có bảng này thay vì chỉ 1 cột đếm?**

| Tình huống | Chỉ có `reservedCount` | Có `stock_reservations` |
|---|---|---|
| Server chết giữa chừng | Số đếm treo vĩnh viễn, không ai biết của đơn nào | Job quét `expiresAt` và tự dọn |
| Cần biết ai đang giữ hàng | Không thể trả lời | Query ra ngay |
| Nghi ngờ số liệu sai | Không có cách kiểm chứng | Tính lại từ dữ liệu gốc |

**d) Chỉ mục kép `(status, expiresAt)`**
Job dọn dẹp chạy mỗi phút với câu query `WHERE status = 'ACTIVE' AND expires_at < NOW()`. Với hàng triệu dòng, không có index này thì mỗi lần chạy sẽ quét toàn bảng. Đây là **composite index** đặt đúng theo thứ tự cột trong mệnh đề WHERE.

**e) `releasedAt` tách riêng khỏi `expiresAt`**
`expiresAt` = thời điểm **dự kiến** hết hạn. `releasedAt` = thời điểm **thực tế** được nhả. Hai giá trị này thường lệch nhau vài giây tới vài phút (do job chạy theo chu kỳ). Ghi nhận cả hai giúp đo hiệu năng job dọn dẹp và điều tra khi có tranh chấp tồn kho.

---

# 6. NHÓM THANH TOÁN & KÝ QUỸ

## 6.1. `PaymentTransaction` — Giao dịch thanh toán
Bảng: `payment_transactions`

```java
@Entity
@Table(name = "payment_transactions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class PaymentTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    private Long orderId;                     // NULLABLE — FK → orders.id (khi thanh toán cho 1 đơn lẻ)

    private Long checkoutSessionId;           // NULLABLE — FK → checkout_sessions.id (khi trả gộp nhiều shop)

    @Column(unique = true, nullable = false, length = 255)
    private String idempotencyKey;            // KHOÁ CHỐNG TRÙNG — chìa khoá ngăn trừ tiền 2 lần

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền giao dịch

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO; // Tổng đã hoàn (cho phép hoàn nhiều lần từng phần)

    @Column(length = 255)
    private String gatewayTransactionId;      // NULLABLE — VNPAY = vnp_TransactionNo; VIETQR = `id` giao dịch SePay (UNIQUE — chống trùng webhook); COD = NULL

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;    // NULLABLE — VNPAY / MOMO / ZALOPAY / VIETQR / INTERNAL_WALLET / COD

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;             // PENDING / REQUIRES_ACTION / SUCCESS / FAILED / EXPIRED / REFUNDED / PARTIALLY_REFUNDED / CANCELLED

    @Column(columnDefinition = "TEXT")
    private String webhookPayload;            // NULLABLE — JSON THÔ cổng thanh toán gửi về, giữ nguyên để đối soát

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống xử lý song song 1 giao dịch

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm khởi tạo giao dịch
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Ghi nhận IPN thành công (idempotent) — UC-PAY-03 */
    public void markSuccess(String gatewayTxId, String payload) {
        if (this.status == PaymentStatus.SUCCESS) return;   // IPN lặp → idempotent
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.REQUIRES_ACTION) throw new IllegalStateException("Giao dịch đã kết thúc: " + this.status);
        this.status = PaymentStatus.SUCCESS;
        this.gatewayTransactionId = gatewayTxId;
        this.webhookPayload = payload;
    }

    /** Ghi nhận thất bại (service gọi Order.markPaymentFailed) — UC-PAY-03 */
    public void markFailed(String payload) {
        if (this.status == PaymentStatus.SUCCESS) throw new IllegalStateException("Không ghi đè giao dịch đã thành công");
        this.status = PaymentStatus.FAILED;
        this.webhookPayload = payload;
    }

    /** Số tiền còn có thể hoàn — UC-SUP-04 */
    public BigDecimal refundableAmount() {
        return this.amount.subtract(this.refundedAmount);
    }

    /** Cập nhật sau khi hoàn qua cổng — UC-SUP-04 */
    public void recordRefund(BigDecimal amt) {
        if (this.status != PaymentStatus.SUCCESS && this.status != PaymentStatus.PARTIALLY_REFUNDED) throw new IllegalStateException("Chỉ hoàn giao dịch đã thành công");
        if (amt.compareTo(refundableAmount()) > 0) throw new IllegalArgumentException("Vượt số tiền có thể hoàn");
        this.refundedAmount = this.refundedAmount.add(amt);
        this.status = refundableAmount().signum() == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }
}
```


### 💡 Kiến thức chuyên sâu

**a) `idempotencyKey` — trường sống còn của mọi hệ thống thanh toán**
**Idempotency** (tính bất biến khi lặp) nghĩa là: thực hiện 1 lần hay 100 lần đều cho cùng kết quả.

Vấn đề thực tế:

```
Client gửi "thanh toán 500.000đ"
   → Server xử lý xong, trừ tiền thành công
   → Mạng rớt khi trả response
   → Client KHÔNG biết kết quả, tự động gửi lại
   → Nếu không có idempotencyKey: TRỪ TIỀN LẦN 2 ❌
```

Cách hoạt động: client sinh 1 UUID duy nhất cho **ý định thanh toán**, gửi kèm mọi lần thử. Server đặt UNIQUE trên cột này. Lần thứ 2 gửi lên, `INSERT` sẽ vi phạm UNIQUE → server bắt lỗi đó và **trả về kết quả của lần 1** thay vì tạo giao dịch mới.
> Đây không phải "tuỳ chọn hay ho" mà là **bắt buộc**. Stripe, PayPal, VNPay đều yêu cầu. Việc dùng UNIQUE ở tầng DB (thay vì kiểm tra bằng `SELECT` rồi `INSERT`) là quan trọng, vì `SELECT`-rồi-`INSERT` vẫn có khe hở race condition giữa 2 lệnh.

**b) `orderId` và `checkoutSessionId` cùng nullable — quan hệ đa hình có kiểm soát**
Một giao dịch gắn với **một trong hai**, không bao giờ cả hai, không bao giờ không có cái nào:
- Trả gộp nhiều shop → gắn `checkoutSessionId`.
- Trả riêng 1 đơn (VD thanh toán lại đơn lỗi) → gắn `orderId`.

> Ràng buộc này nên được ép ở tầng DB bằng CHECK constraint:
> ```sql
> CHECK ((order_id IS NULL) <> (checkout_session_id IS NULL))
> ```
> (phép XOR — đúng đúng một trong hai khác NULL).

**c) `webhookPayload` lưu JSON thô — nguyên tắc "giữ bằng chứng gốc"**
Khi cổng thanh toán gọi webhook báo kết quả, hệ thống lưu **nguyên văn** payload. Vì sao?
1. **Đối soát**: cuối tháng so với sao kê của cổng, bằng chứng gốc là thứ duy nhất có giá trị.
2. **Debug**: khi logic phân tích sai, dữ liệu thô cho phép chạy lại (replay) mà không cần cổng gửi lại.
3. **Pháp lý**: tranh chấp thanh toán cần chứng cứ nguyên bản, không phải bản đã qua diễn giải.

**d) `refundedAmount` cho phép hoàn từng phần nhiều lần**
Đơn 1.000.000đ có thể hoàn 300.000đ (trả 1 món), rồi sau đó hoàn tiếp 200.000đ. Vì vậy phải cộng dồn, và quy tắc bất biến luôn phải giữ:

```
refundedAmount <= amount        (không bao giờ hoàn nhiều hơn đã nhận)

refundedAmount == 0          → status giữ SUCCESS
0 < refundedAmount < amount  → status = PARTIALLY_REFUNDED
refundedAmount == amount     → status = REFUNDED
```

**e) `REQUIRES_ACTION` — trạng thái của xác thực 3D Secure / OTP**
Khi ngân hàng yêu cầu khách nhập OTP, giao dịch chưa thành công cũng chưa thất bại — nó **đang chờ hành động của người dùng**. Không có trạng thái này, hệ thống buộc phải coi là FAILED và huỷ đơn oan.

---

## 6.2. `PaymentRefund` — Giao dịch hoàn tiền qua cổng
Bảng: `payment_refunds`

```java
@Entity
@Table(name = "payment_refunds")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class PaymentRefund {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long paymentTransactionId;        // FK → payment_transactions.id — hoàn cho giao dịch gốc nào

    @Column(nullable = false)
    private Long refundRequestId;             // FK → refund_requests.id — xuất phát từ yêu cầu hoàn nào

    @Column(nullable = false, length = 255)
    private String gatewayRefundId;           // Mã hoàn tiền bên cổng thanh toán cấp

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền hoàn của lần này

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentRefundStatus status;       // PENDING / SUCCESS / FAILED
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cổng xác nhận hoàn tiền — UC-SUP-04 */
    public void markSuccess(String gatewayRefundId) {
        this.status = PaymentRefundStatus.SUCCESS;
        this.gatewayRefundId = gatewayRefundId;
    }

    /** Hoàn tiền thất bại — chuyển xử lý tay — UC-SUP-04 */
    public void markFailed() {
        this.status = PaymentRefundStatus.FAILED;
    }
}
```

### 💡 Kiến thức chuyên sâu

**Phân biệt `RefundRequest` và `PaymentRefund` — hai tầng khác nhau**

| | RefundRequest | PaymentRefund |
|---|---|---|
| Tầng | **Nghiệp vụ** | **Kỹ thuật/tài chính** |
| Nội dung | Khách yêu cầu trả hàng, lý do, ảnh bằng chứng | Lệnh gọi API hoàn tiền tới VNPay/Momo |
| Ai tạo | Khách hàng | Hệ thống, sau khi staff duyệt |
| Số lượng | 1 yêu cầu | Có thể nhiều lần thử (mỗi lần gọi API 1 dòng) |

Quan hệ **1 - N**: một yêu cầu hoàn tiền có thể sinh nhiều bản ghi `PaymentRefund` nếu lần gọi đầu thất bại và hệ thống thử lại. Giữ lại cả các lần `FAILED` là cần thiết để điều tra sự cố với cổng thanh toán.

**`gatewayRefundId` — chìa khoá đối soát**
Khi khách khiếu nại *"Tôi chưa nhận được tiền hoàn"*, đây là mã duy nhất để tra cứu với ngân hàng/cổng thanh toán. Không có nó, sàn không thể chứng minh đã hoàn.

---

## 6.3. `EscrowLedger` — Sổ ký quỹ (tiền tạm giữ)
Bảng: `escrow_ledgers`

```java
@Entity
@Table(name = "escrow_ledgers")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class EscrowLedger {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long orderId;                     // FK → orders.id, UNIQUE — mỗi đơn đúng 1 bản ghi ký quỹ

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — tiền này sẽ về shop nào

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;           // grossAmount = Order.subTotal - Order.discountAmount (KHÔNG gồm shippingFee)

    // Không có cột commissionRate — một đơn có thể gồm nhiều ngành hàng với tỷ lệ khác nhau; tỷ lệ chụp ở từng OrderItem.commissionRate

    @Column(name = "commission_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal commissionFee;         // Tiền hoa hồng sàn thu = Σ OrderItem.commissionAmount của đơn (khớp tuyệt đối từng dòng)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal platformShippingFee;   // Phần phí ship sàn trả hộ

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal shopShippingSubsidy;   // Phần phí ship shop tự hỗ trợ khách

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;             // SỐ TIỀN THỰC shop được nhận sau tất cả khoản trừ

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO; // Tổng tiền hàng đã hoàn một phần cho khách (mặc định 0) — grossAmount gốc = grossAmount + refundedAmount

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FundingSource fundingSource;      // NULLABLE (NULL tới lần nạp đầu) — WALLET | ONLINE_GATEWAY | COD_CASH | SPLIT (SPLIT = có từ 2 nguồn khác nhau trở lên)

    @Builder.Default
    @Column(precision = 19, scale = 2, nullable = false)
    @ColumnDefault("0")
    private BigDecimal gatewayFundedAmount = BigDecimal.ZERO; // Phần trả online: VNPay hoặc chuyển khoản VietQR (xác nhận qua SePay)

    @Builder.Default
    @Column(precision = 19, scale = 2, nullable = false)
    @ColumnDefault("0")
    private BigDecimal codFundedAmount = BigDecimal.ZERO; // Tiền COD đã thu nộp (khi kế toán confirm SETTLED)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EscrowStatus status;              // PENDING_FUNDING | HELD | FROZEN | RELEASED | REFUNDED | DISPUTED

    private OffsetDateTime fundedAt;          // NULLABLE — Thời điểm fund đủ (chuyển PENDING_FUNDING → HELD)

    private OffsetDateTime codRemittedAt;     // NULLABLE — Thời điểm kế toán xác nhận đã nhận COD

    private OffsetDateTime releaseEligibleAt;  // NULLABLE — Thời điểm ĐỦ ĐIỀU KIỆN giải ngân (hết hạn khiếu nại)

    private OffsetDateTime releasedAt;         // NULLABLE — Thời điểm THỰC SỰ giải ngân

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo bút toán ký quỹ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Nạp tiền vào ký quỹ — nguồn ONLINE_GATEWAY (VNPay / VietQR qua SePay) hoặc COD_CASH (GHN đã đối soát); chỉ HELD khi tổng đã nạp ≥ grossAmount — UC-PAY-05 */
    public void fund(BigDecimal amount, FundingSource source, OffsetDateTime now) {
        switch (source) {
            case ONLINE_GATEWAY -> this.gatewayFundedAmount = this.gatewayFundedAmount.add(amount);
            case COD_CASH -> this.codFundedAmount = this.codFundedAmount.add(amount);
        }
        this.fundingSource = source;   // MVP: mỗi đơn đúng 1 nguồn tiền (không chia nguồn)
        if (isFullyFunded() && this.status == EscrowStatus.PENDING_FUNDING) {
            this.status = EscrowStatus.HELD;   // chỉ HELD khi đủ grossAmount
            this.fundedAt = now;
        }
    }

    /** Tổng đã nạp từ mọi nguồn */
    private BigDecimal totalFunded() {
        return this.gatewayFundedAmount.add(this.codFundedAmount);
    }

    /** Đã nhận đủ tiền đơn — UC-PAY-05 */
    public boolean isFullyFunded() {
        return totalFunded().compareTo(this.grossAmount) >= 0;
    }

    /** Đặt mốc giải ngân 7 ngày sau DELIVERED — UC-ORD-12 */
    public void scheduleRelease(OffsetDateTime deliveredAt) {
        this.releaseEligibleAt = deliveredAt.plusDays(7);
    }

    /** Đủ điều kiện giải ngân (HELD, đủ tiền, qua 7 ngày) — UC-PAY-07 */
    public boolean isReleasable(OffsetDateTime now) {
        return this.status == EscrowStatus.HELD && isFullyFunded() && this.releaseEligibleAt != null && !this.releaseEligibleAt.isAfter(now);
    }

    /** Giải ngân cho shop — trả về số tiền netAmount hiện hành để Service cộng ví shop (VendorWallet + WalletTransaction) — UC-PAY-07 */
    public BigDecimal release(OffsetDateTime now) {
        if (!isReleasable(now)) throw new IllegalStateException("Chưa đủ điều kiện giải ngân");
        this.status = EscrowStatus.RELEASED;
        this.releasedAt = now;
        return this.netAmount;   // service ghi CREDIT_ESCROW_RELEASE đúng số này (đã trừ phần hoàn một phần nếu có)
    }

    /** Khóa tranh chấp khi khách gửi yêu cầu hoàn tiền (kể cả COD chưa đối soát) — UC-SUP-01 */
    public void dispute() {
        if (this.status != EscrowStatus.HELD && this.status != EscrowStatus.PENDING_FUNDING) throw new IllegalStateException("Không thể tranh chấp ở " + this.status);
        this.status = EscrowStatus.DISPUTED;
    }

    /** Sàn bác khiếu nại → quay về HELD (hoặc PENDING_FUNDING nếu COD chưa nộp) — UC-SUP-03 */
    public void releaseDisputeHold() {
        if (this.status != EscrowStatus.DISPUTED) return;
        this.status = isFullyFunded() ? EscrowStatus.HELD : EscrowStatus.PENDING_FUNDING;
    }

    /** Hoàn một phần: giảm grossAmount và commissionFee đúng bằng phần của các dòng bị trả (refundCommission = Σ OrderItem.commissionAmount × tỷ lệ số lượng trả), tính lại netAmount (khách + shop + sàn = tiền gốc); sau đó releaseDisputeHold() — UC-SUP-04 */
    public void applyPartialRefund(BigDecimal refundGross, BigDecimal refundCommission) {
        if (this.status != EscrowStatus.DISPUTED && this.status != EscrowStatus.HELD) throw new IllegalStateException("Không thể hoàn ở " + this.status);
        if (refundGross.signum() <= 0 || refundGross.compareTo(this.grossAmount) >= 0)
            throw new IllegalArgumentException("Hoàn toàn bộ dùng markRefunded(); hoàn một phần phải 0 < số tiền < grossAmount");
        if (refundCommission.signum() < 0 || refundCommission.compareTo(this.commissionFee) > 0)
            throw new IllegalArgumentException("Hoa hồng hoàn lại không hợp lệ");
        this.refundedAmount = this.refundedAmount.add(refundGross);
        this.grossAmount = this.grossAmount.subtract(refundGross);                 // doanh thu còn lại của shop
        this.commissionFee = this.commissionFee.subtract(refundCommission);        // bỏ đúng hoa hồng của các dòng bị trả (mỗi dòng giữ tỷ lệ ngành hàng riêng)
        this.netAmount = this.grossAmount.subtract(this.commissionFee).subtract(this.shopShippingSubsidy).max(BigDecimal.ZERO);
    }

    /** Hoàn tiền cho khách từ ký quỹ — UC-SUP-04 */
    public void markRefunded() {
        if (this.status != EscrowStatus.DISPUTED && this.status != EscrowStatus.HELD) throw new IllegalStateException("Không thể hoàn ở " + this.status);
        this.status = EscrowStatus.REFUNDED;
    }

    /** Admin phong tỏa tạm khi nghi ngờ gian lận (xử lý thủ công) — UC-SUP-03 */
    public void freeze() {
        if (this.status == EscrowStatus.RELEASED || this.status == EscrowStatus.REFUNDED) throw new IllegalStateException("Ký quỹ đã đóng");
        this.status = EscrowStatus.FROZEN;
    }
}

// Enum trạng thái ký quỹ
enum EscrowStatus {
    PENDING_FUNDING,  // Đã tạo record nhưng chưa nhận đủ tiền (COD: GHN chưa đối soát)
    HELD,             // Đã fund đầy đủ (VNPay / VietQR qua SePay / COD đã đối soát), đang giữ an toàn
    FROZEN,           // Admin chủ động đóng băng (nghi vấn gian lận, vi phạm sàn)
    RELEASED,         // Đã giải ngân thành công cho ví vendor
    REFUNDED,         // Đã hoàn tiền cho khách hàng
    DISPUTED          // Hệ thống tự động khóa do phát sinh yêu cầu đổi trả / khiếu nại (RefundRequest)
}

// Enum nguồn nạp vốn ký quỹ
enum FundingSource {
    ONLINE_GATEWAY,   // VNPay / chuyển khoản VietQR (SePay)
    COD_CASH          // Tiền mặt GHN thu hộ, đã đối soát về sàn
}
```

### 💡 Kiến thức chuyên sâu

**a) Escrow là gì và vì sao là nền tảng niềm tin của sàn TMĐT?**
**Escrow** (ký quỹ / tài khoản trung gian) là cơ chế bên thứ ba **giữ tiền** cho tới khi giao dịch hoàn tất.

```
Khách trả tiền  →  TIỀN VÀO TÚI SÀN (không vào shop!)   [status = HELD]
Shop giao hàng  →  Khách nhận, có 7 ngày khiếu nại
Hết 7 ngày yên  →  Sàn chuyển tiền vào ví shop           [status = RELEASED]
Khách khiếu nại →  Tiền bị đóng băng chờ xử lý           [status = DISPUTED]
```

> **Vì sao không trả thẳng cho shop?** Vì nếu shop nhận tiền ngay rồi không giao hàng (hoặc giao hàng giả), sàn **không còn đòn bẩy** để bảo vệ khách. Escrow biến sàn thành trọng tài có thực quyền: tiền nằm trong tay sàn nên sàn có thể hoàn cho khách bất cứ lúc nào. Đây là lý do khách dám mua hàng của shop lạ.

**b) Phân biệt `releaseEligibleAt` và `releasedAt`**
- `releaseEligibleAt` = thời điểm **đủ điều kiện** (VD: ngày giao hàng + 7 ngày). Đây là dữ liệu do nghiệp vụ tính.
- `releasedAt` = thời điểm **thực sự** chuyển tiền, do job nền thực hiện (VD: chạy 2h sáng mỗi ngày).

Hai mốc này luôn lệch nhau. Tách riêng cho phép trả lời: *"Đơn này lẽ ra được giải ngân ngày 10 nhưng mãi 12 mới chuyển — vì sao?"*

**c) Tất cả các khoản phí đều lưu ĐẦY ĐỦ, không chỉ lưu kết quả**

```
netAmount = grossAmount - commissionFee - shopShippingSubsidy
```

> `commissionFee` = Σ `OrderItem.commissionAmount` — mỗi dòng giữ tỷ lệ ngành hàng riêng (điện tử 3.5%, thời trang 8%…), nên ký quỹ không lưu một `commissionRate` chung (để tổng hoa hồng luôn khớp từng dòng).
> Khi hoàn **một phần**, `applyPartialRefund(refundGross, refundCommission)` giảm `grossAmount` và trừ đúng hoa hồng của các dòng bị trả (Service tính `refundCommission` từ `OrderItem`), tính lại `netAmount` theo công thức trên, cộng dồn `refundedAmount`; `release()` trả về `netAmount` hiện hành để Service ghi ví shop — không bao giờ giải ngân `netAmount` cũ (xem ví dụ số ở mục 8.1).

Về lý thuyết chỉ cần lưu `netAmount`. Nhưng lưu đủ mọi thành phần cho phép:
1. Shop xem được **bảng kê chi tiết** *"Vì sao tôi chỉ nhận 450.000đ từ đơn 500.000đ?"* → tăng minh bạch, giảm khiếu nại.
2. Sàn **đối soát ngược** khi nghi ngờ sai sót.
3. Báo cáo doanh thu hoa hồng theo thời gian mà không phải tính lại.

**d) Phân biệt `platformShippingFee` và `shopShippingSubsidy`**
- `platformShippingFee`: sàn bù phí ship (từ mã freeship của sàn) → **sàn chịu**, không trừ shop.
- `shopShippingSubsidy`: shop tự nguyện hỗ trợ ship để hút khách → **trừ vào tiền shop nhận**.

Cùng là "giảm phí ship" với khách, nhưng dòng tiền hoàn toàn ngược nhau. Đây là lý do 2 cột phải tách.

**e) `orderId` UNIQUE — bảo đảm không giải ngân 2 lần**
Ràng buộc UNIQUE ở tầng DB tuyệt đối chặn việc 1 đơn hàng sinh ra 2 bút toán ký quỹ, dù code có lỗi gọi trùng. Đây là lớp phòng vệ cuối cùng cho tiền thật.

---

## 6.4. `VendorWallet` — Ví tiền của người bán
Bảng: `vendor_wallets`

```java
@Entity
@Table(name = "vendor_wallets")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VendorWallet {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long vendorId;                    // FK → users.id, UNIQUE — mỗi shop đúng 1 ví

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO; // Số dư KHẢ DỤNG — tiền shop có thể rút ngay

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal pendingWithdrawal = BigDecimal.ZERO; // Số tiền đang bị GIỮ do có lệnh rút chờ xử lý

    @Column(nullable = false, length = 255)
    private String balanceHash;               // HMAC-SHA256(vendorId + balance + pendingWithdrawal + secret) — service verify trước mỗi UPDATE và ghi lại sau UPDATE, chống sửa trực tiếp CSDL

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — LÁ CHẮN CHỐNG RÚT TIỀN 2 LẦN

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo ví

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm biến động số dư gần nhất
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Số dư khả dụng = số dư − đang chờ rút — UC-ADM-04 */
    public BigDecimal availableBalance() {
        return this.balance.subtract(this.pendingWithdrawal);
    }

    /** Cộng tiền (giải ngân Escrow, nạp ví) — UC-PAY-07 */
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    /** Trừ tiền (rút tiền, thu hồi khi hoàn tiền sau giải ngân, điều chỉnh giảm) — UC-ADM-04 */
    public void debit(BigDecimal amount) {
        if (availableBalance().compareTo(amount) < 0) throw new IllegalStateException("GNE-WAL-4001");
        this.balance = this.balance.subtract(amount);
    }

    /** Giữ tiền khi tạo lệnh rút — UC-ADM-04 */
    public void holdForWithdrawal(BigDecimal amount) {
        if (availableBalance().compareTo(amount) < 0) throw new IllegalStateException("GNE-WAL-4001");
        this.pendingWithdrawal = this.pendingWithdrawal.add(amount);
    }

    /** Chốt lệnh rút (chi tiền hoặc trả lại số dư) — UC-ADM-05 */
    public void settleWithdrawal(BigDecimal amount, boolean approved) {
        this.pendingWithdrawal = this.pendingWithdrawal.subtract(amount);
        if (approved) this.balance = this.balance.subtract(amount);
    }

    /** Phát hiện sửa số dư trực tiếp CSDL (so HMAC hằng thời gian) — UC-PAY-07 */
    public boolean verifyHash(String expectedHmac) {
        return MessageDigest.isEqual(this.balanceHash.getBytes(StandardCharsets.UTF_8), expectedHmac.getBytes(StandardCharsets.UTF_8));
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) `balance` vs `pendingWithdrawal` — cùng nguyên lý với `stockCount` vs `reservedCount`**

```
Tiền shop THỰC SỰ rút được = balance
Tiền đang bị giữ chờ duyệt  = pendingWithdrawal
Tổng tài sản của shop       = balance + pendingWithdrawal
```

Luồng rút tiền:

```
1. Shop gửi lệnh rút 5 triệu   → balance -= 5tr; pendingWithdrawal += 5tr
2a. Admin duyệt & chuyển khoản → pendingWithdrawal -= 5tr  (tiền rời hệ thống)
2b. Admin từ chối              → pendingWithdrawal -= 5tr; balance += 5tr  (trả lại ví)
```

> Nếu không tách 2 cột, shop có thể gửi 10 lệnh rút 5 triệu liên tiếp trong khi chỉ có 5 triệu — và admin duyệt lần lượt sẽ chi ra 50 triệu.

**b) `@Version` ở đây là lớp bảo vệ quan trọng nhất toàn hệ thống**
Đây là tấn công kinh điển **Double Spending** (tiêu 2 lần):

```
Ví có 1.000.000đ. Kẻ gian gửi 2 request rút 1.000.000đ CÙNG LÚC:
  Request A: đọc balance = 1.000.000, version = 7
  Request B: đọc balance = 1.000.000, version = 7    ← cả hai đều thấy đủ tiền!
  A: UPDATE ... SET balance = 0 WHERE version = 7  → OK, version thành 8
  B: UPDATE ... SET balance = 0 WHERE version = 7  → 0 rows → NÉM LỖI ✅
```

Không có `@Version`, cả hai đều thành công → sàn mất 1 triệu. Với hàng nghìn ví, đây là lỗ hổng chí mạng.
> Ngoài optimistic lock, hệ thống tiền thật thường dùng thêm `SELECT ... FOR UPDATE` (pessimistic lock) cho các thao tác ví, vì số lần tranh chấp thấp nhưng hậu quả sai cực nặng.

**c) Vì sao `balance` là cột tổng mà không tính từ `wallet_transactions`?**
Về lý thuyết `balance = SUM(các giao dịch)`. Nhưng với ví có 100.000 giao dịch, cộng lại mỗi lần hiển thị là không khả thi. Đây là **snapshot balance** — đọc nhanh, và `WalletTransaction.balanceBefore/balanceAfter` cho phép kiểm chứng lại bất cứ lúc nào.

---

## 6.5. `WalletTransaction` — Lịch sử biến động ví
Bảng: `wallet_transactions`

```java
@Entity
@Table(name = "wallet_transactions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class WalletTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long walletId;                    // FK → vendor_wallets.id — biến động của ví nào

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTransactionType type;       // CREDIT_ESCROW_RELEASE / DEBIT_REFUND / DEBIT_WITHDRAWAL / CREDIT_ADJUSTMENT / DEBIT_ADJUSTMENT / CREDIT_TOPUP / DEBIT_PURCHASE (7 giá trị)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền biến động CÓ DẤU: DƯƠNG = CREDIT (cộng vào ví), ÂM = DEBIT (trừ khỏi ví) — quy ước chung của mọi sổ ví

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;         // SỐ DƯ TRƯỚC giao dịch — bất biến kiểm toán: balanceBefore + amount = balanceAfter

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;          // SỐ DƯ SAU giao dịch — dấu vết kiểm toán

    @Column(length = 255)
    private String referenceId;               // NULLABLE — ID của đối tượng liên quan (order_id, withdrawal_id...)

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WalletReferenceType referenceType; // NULLABLE — Loại đối tượng: ORDER / WITHDRAWAL / REFUND / ADJUSTMENT

    @Column(length = 500)
    private String note;                      // NULLABLE — Ghi chú, đặc biệt quan trọng với các khoản điều chỉnh tay

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm phát sinh
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo giao dịch trừ ví (append-only, gọi sau khi ví đã trừ; balanceBefore = số dư sau + số trừ) — UC-ADM-04 */
    public static WalletTransaction debit(VendorWallet wallet, WalletTransactionType type, BigDecimal amount, WalletReferenceType refType, String reference) {
        WalletTransaction t = new WalletTransaction();
        t.walletId = wallet.getId(); t.type = type; t.amount = amount.negate();
        t.balanceAfter = wallet.getBalance(); t.balanceBefore = t.balanceAfter.add(amount);
        t.referenceType = refType; t.referenceId = reference; t.createdAt = OffsetDateTime.now();
        return t;
    }

    /** Tạo giao dịch cộng ví (append-only, gọi sau khi ví đã cộng) — UC-PAY-07 */
    public static WalletTransaction credit(VendorWallet wallet, WalletTransactionType type, BigDecimal amount, WalletReferenceType refType, String reference) {
        WalletTransaction t = new WalletTransaction();
        t.walletId = wallet.getId(); t.type = type; t.amount = amount;
        t.balanceAfter = wallet.getBalance(); t.balanceBefore = t.balanceAfter.subtract(amount);
        t.referenceType = refType; t.referenceId = reference; t.createdAt = OffsetDateTime.now();
        return t;
    }

    /** Kiểm tra bất biến sổ ví (job đối soát đêm) — UC-PAY-07 */
    public boolean isConsistent() {
        return this.balanceBefore.add(this.amount).compareTo(this.balanceAfter) == 0;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) `balanceBefore` / `balanceAfter` — kỹ thuật "Running Balance" trong kế toán**
Mỗi dòng ghi lại số dư **ngay trước** và **ngay sau** khi nó phát sinh, với bất biến `balanceBefore + amount = balanceAfter` (`isConsistent()`) và `balanceBefore` của dòng N = `balanceAfter` của dòng N−1. Tác dụng:
1. **Phát hiện giả mạo**: nếu ai đó sửa trộm 1 dòng trong DB, chuỗi số dư sẽ đứt gãy — dòng sau không khớp dòng trước.
2. **Sao kê tức thì**: in sao kê không cần cộng dồn lại từ đầu.
3. **Đối soát**: `balanceAfter` của dòng mới nhất phải **bằng đúng** `VendorWallet.balance`. Lệch = có lỗi.

```
Dòng 1: +500.000  → balanceAfter = 500.000
Dòng 2: +300.000  → balanceAfter = 800.000
Dòng 3: -200.000  → balanceAfter = 600.000   ← phải khớp vendor_wallets.balance
```

**b) Quy ước CREDIT / DEBIT theo góc nhìn của shop**
- `CREDIT_*` = **tiền vào** ví shop (giải ngân escrow, điều chỉnh tăng) → `amount` **dương**.
- `DEBIT_*` = **tiền ra** khỏi ví shop (rút tiền, hoàn tiền cho khách, điều chỉnh giảm) → `amount` **âm**.

**c) `CREDIT_ADJUSTMENT` / `DEBIT_ADJUSTMENT` — cửa hậu hợp pháp cho con người**
Mọi hệ thống tài chính đều cần cơ chế **điều chỉnh tay**: đền bù cho shop khi sàn có lỗi, thu hồi tiền phát nhầm, xử lý phán quyết tranh chấp.
> Nguyên tắc vàng: **không bao giờ UPDATE thẳng `balance`**. Mọi thay đổi phải đi qua một `WalletTransaction` có `note` giải thích. Nếu cho phép sửa số dư trực tiếp, sổ sách mất khả năng truy vết và không ai chứng minh được tiền đi đâu.

**d) `referenceId` + `referenceType` — quan hệ đa hình**
Một biến động ví có thể bắt nguồn từ nhiều loại đối tượng khác nhau (đơn hàng, lệnh rút, yêu cầu hoàn tiền). Cặp 2 cột này cho biết "trỏ tới bảng nào, dòng nào".
> **Đánh đổi:** không đặt được FK (cùng cột trỏ nhiều bảng). Bù lại rất linh hoạt. Ứng dụng phải tự đảm bảo tính đúng đắn.

---

## 6.6. `WithdrawalRequest` — Yêu cầu rút tiền
Bảng: `withdrawal_requests`

```java
@Entity
@Table(name = "withdrawal_requests")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class WithdrawalRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long walletId;                    // FK mềm theo ownerType → vendor_wallets.id / customer_wallets.id / shippers.id

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền yêu cầu rút

    @Column(nullable = false, length = 255)
    private String bankAccountName;           // Tên chủ TK ngân hàng — CHỤP LẠI lúc gửi yêu cầu

    @Column(nullable = false, length = 50)
    private String bankAccountNumber;         // Số TK ngân hàng — chụp lại

    @Column(nullable = false, length = 100)
    private String bankName;                  // Tên ngân hàng — chụp lại

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status;          // PENDING / APPROVED / PROCESSING / COMPLETED / REJECTED

    @Column(length = 500)
    private String rejectionReason;           // NULLABLE — Lý do từ chối, để shop biết đường xử lý

    private Long processedByAdminId;          // NULLABLE — FK mềm → users.id, admin nào đã xử lý

    private Long walletTransactionId;         // NULLABLE — FK → wallet_transactions.id, bút toán tương ứng

    @Column(nullable = false)
    private OffsetDateTime requestedAt;        // Thời điểm shop gửi yêu cầu

    private OffsetDateTime processedAt;        // NULLABLE — Thời điểm admin xử lý xong

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Nhân viên sàn phê duyệt lệnh rút (MVP duyệt một bước) — UC-ADM-05 */
    public void approve(Long adminId, OffsetDateTime now) {
        if (this.status != WithdrawalStatus.PENDING) throw new IllegalStateException("Lệnh rút không ở PENDING");
        this.status = WithdrawalStatus.APPROVED;
        this.processedByAdminId = adminId;
        this.processedAt = now;
    }

    /** Đã gửi lệnh chuyển khoản sang ngân hàng — UC-ADM-05 */
    public void startProcessing() {
        if (this.status != WithdrawalStatus.APPROVED) throw new IllegalStateException("Chưa phê duyệt");
        this.status = WithdrawalStatus.PROCESSING;
    }

    /** Ngân hàng xác nhận đã chuyển — UC-ADM-05 */
    public void complete(Long walletTransactionId) {
        if (this.status != WithdrawalStatus.APPROVED && this.status != WithdrawalStatus.PROCESSING) throw new IllegalStateException("Lệnh rút chưa duyệt");
        this.status = WithdrawalStatus.COMPLETED;
        this.walletTransactionId = walletTransactionId;
    }

    /** Từ chối, trả tiền về số dư — UC-ADM-05 */
    public void reject(String reason, Long adminId, OffsetDateTime now) {
        if (this.status == WithdrawalStatus.COMPLETED || this.status == WithdrawalStatus.REJECTED) throw new IllegalStateException("Lệnh rút đã kết thúc");
        this.status = WithdrawalStatus.REJECTED;
        this.rejectionReason = reason;
        this.processedByAdminId = adminId;
        this.processedAt = now;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Vì sao CHỤP LẠI thông tin ngân hàng thay vì JOIN sang `VendorProfile`?**
Thông tin ngân hàng ở `VendorProfile` có thể bị **thay đổi bất cứ lúc nào**. Nếu lệnh rút chỉ tham chiếu tới profile:

```
Ngày 1: Shop gửi lệnh rút 50 triệu về TK Vietcombank ...1234
Ngày 2: Tài khoản shop bị hack, hacker đổi TK thành ...9999
Ngày 3: Admin duyệt → hệ thống đọc TK HIỆN TẠI → chuyển 50 triệu cho hacker ❌
```

Chụp lại thông tin tại thời điểm gửi yêu cầu giúp lệnh rút luôn chuyển về đúng tài khoản đã đăng ký khi tạo lệnh. Đây vừa là **snapshot pattern**, vừa là **biện pháp chống gian lận**.

**b) Vòng đời 5 trạng thái — vì sao cần `APPROVED` tách khỏi `PROCESSING`?**

```
PENDING     → Shop vừa gửi, chờ admin xem
APPROVED    → Admin đã DUYỆT (quyết định của con người)
PROCESSING  → Hệ thống ĐANG gọi API ngân hàng (máy đang làm việc)
COMPLETED   → Ngân hàng xác nhận đã chuyển thành công
REJECTED    → Admin từ chối → hoàn tiền về balance
```

Tách `APPROVED` và `PROCESSING` cho phép phân biệt lỗi: nếu treo ở `APPROVED` là **con người chậm**; treo ở `PROCESSING` là **hệ thống/ngân hàng có sự cố**. Hai loại sự cố này cần hai cách xử lý hoàn toàn khác nhau.

**c) `processedByAdminId` — trách nhiệm giải trình (accountability)**
Rút tiền là thao tác rủi ro cao nhất trong hệ thống. Ghi rõ ai duyệt là yêu cầu bắt buộc của kiểm toán. Kết hợp với `SystemLog` (`PAYOUT_APPROVED`), mọi đồng tiền ra khỏi hệ thống đều truy được về một con người cụ thể.

**d) `walletTransactionId` — liên kết 2 chiều nghiệp vụ ↔ kế toán**
Yêu cầu rút là **nghiệp vụ**; bút toán ví là **kế toán**. Nối 2 bảng cho phép đi cả hai chiều: từ lệnh rút tra ra bút toán, và từ dòng sao kê tra ngược lại lệnh rút gốc.

---

## 6.7. `FinancialLedger` — Sổ cái tài chính (chỉ ghi thêm)
Bảng: `financial_ledger`

```java
@Entity
@Table(name = "financial_ledger")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class FinancialLedger {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;                         // Số THỨ TỰ bút toán — tăng dần tuyệt đối, không bao giờ tái sử dụng

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialEventType eventType;     // ESCROW_IN, COMMISSION, PLATFORM_VOUCHER, FREESHIP_COST, COIN_COST, ESCROW_RELEASE, PAYOUT_ACCRUE, PAYOUT_SETTLE, REFUND, GATEWAY_FEE

    @Column(nullable = false, length = 255)
    private String referenceId;               // ID đối tượng gốc: order_id / payout_id / refund_id

    private Long orderId;                     // NULLABLE — FK → orders.id khi bút toán gắn với một đơn (ESCROW_IN, COMMISSION, REFUND…) — lọc sổ cái theo đơn

    private Long vendorId;                    // NULLABLE — FK → users.id của shop khi bút toán gắn với một shop (giải ngân, phí) — đối soát theo shop

    @Column(nullable = false, length = 100)
    private String account;                   // TÊN TÀI KHOẢN kế toán bị tác động

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal debit;                 // Số tiền ghi NỢ (bên trái)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal credit;                // Số tiền ghi CÓ (bên phải)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;          // Số dư của tài khoản đó sau bút toán này

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm ghi sổ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo bút toán (bảng chỉ INSERT, cấm UPDATE/DELETE) — UC-PAY-06 */
    public static FinancialLedger entry(FinancialEventType type, String account, BigDecimal debit, BigDecimal credit, String referenceId) {
        FinancialLedger l = new FinancialLedger();
        l.eventType = type; l.account = account; l.debit = debit; l.credit = credit;
        l.referenceId = referenceId; l.createdAt = OffsetDateTime.now();
        return l;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Bút toán kép (Double-Entry Bookkeeping) — phát minh 500 năm tuổi vẫn là chuẩn vàng**
Nguyên lý: **mọi giao dịch đều tác động tối thiểu 2 tài khoản**, và tổng Nợ luôn **bằng** tổng Có.

Ví dụ: khách trả 1.000.000đ cho đơn hàng:

| account | debit (Nợ) | credit (Có) |
|---|---|---|
| `CASH` (tiền mặt sàn giữ) | 1.000.000 | 0 |
| `ESCROW_LIABILITY` (nợ phải trả shop) | 0 | 1.000.000 |

Tổng Nợ = Tổng Có = 1.000.000 ✅

Sau đó sàn thu hoa hồng 5%:

| account | debit | credit |
|---|---|---|
| `ESCROW_LIABILITY` | 50.000 | 0 |
| `COMMISSION_REVENUE` (doanh thu sàn) | 0 | 50.000 |

> **Sức mạnh của mô hình:** bất kỳ lúc nào chạy `SELECT SUM(debit), SUM(credit) FROM financial_ledger` — nếu hai số **không bằng nhau**, chắc chắn có lỗi ở đâu đó. Đây là cơ chế **tự kiểm tra** mà không mô hình nào khác có được.

**b) Append-only — sổ cái CHỈ được ghi thêm, KHÔNG được sửa/xoá**
Đây là ràng buộc bất di bất dịch của kế toán. Ghi sai thì phải ghi **bút toán đảo** (reversal entry) để triệt tiêu, chứ không được xoá dòng cũ.

```
Ghi nhầm:   account=X, debit=100, credit=0     (seq 501)
Sửa sai:    account=X, debit=0,   credit=100   (seq 502)  ← bút toán đảo
```

> **Vì sao?** Vì lịch sử tài chính phải **bất biến**. Nếu sửa được, mọi báo cáo quá khứ đều mất giá trị pháp lý, và gian lận nội bộ không thể phát hiện. Trong thực tế nên thu hồi luôn quyền `UPDATE`/`DELETE` trên bảng này ở cấp database user.

**c) Vì sao khoá chính tên là `seq` chứ không phải `id`?**
`seq` (sequence) nhấn mạnh **thứ tự thời gian tuyệt đối** của bút toán. Trong sổ cái, thứ tự ghi sổ là dữ liệu có ý nghĩa pháp lý, không chỉ là mã định danh. Số này không bao giờ được nhảy cách bất thường hay tái sử dụng.

**d) Phân biệt `FinancialLedger` với `WalletTransaction` và `EscrowLedger`**

| Bảng | Góc nhìn | Ai đọc |
|---|---|---|
| `WalletTransaction` | Số dư của **từng shop** | Shop xem sao kê của mình |
| `EscrowLedger` | Tiền tạm giữ của **từng đơn** | Vận hành theo dõi giải ngân |
| `FinancialLedger` | **Toàn bộ dòng tiền của sàn** | Kế toán, kiểm toán, CFO |

Sổ cái là nơi duy nhất trả lời được: *"Tháng này sàn thu bao nhiêu hoa hồng? Chi bao nhiêu cho khuyến mãi? Còn nợ shop bao nhiêu?"*

**e) Các `eventType` ánh xạ toàn bộ dòng tiền của sàn**

| Event | Ý nghĩa | Ảnh hưởng tới sàn |
|---|---|---|
| `ESCROW_IN` | Tiền khách vào ký quỹ | Tăng tiền mặt, tăng nợ phải trả |
| `COMMISSION` | Sàn thu hoa hồng | **Doanh thu** |
| `PLATFORM_VOUCHER` | Sàn tài trợ voucher | **Chi phí** marketing |
| `FREESHIP_COST` | Sàn bù phí ship | **Chi phí** |
| `COIN_COST` | Khách tiêu Điểm | **Chi phí** (Điểm là nợ phải trả của sàn) |
| `ESCROW_RELEASE` | Giải ngân khỏi ký quỹ | Giảm nợ phải trả |
| `PAYOUT_ACCRUE` | Ghi nhận nợ phải trả shop | Tăng nợ |
| `PAYOUT_SETTLE` | Thực chi trả cho shop | Giảm tiền mặt, giảm nợ |
| `REFUND` | Hoàn tiền khách | Giảm tiền mặt |
| `GATEWAY_FEE` | Phí cổng VNPay sàn chịu | **Chi phí** |

> **Đơn COD:** tiền GHN chuyển về (webhook SePay) ghi `ESCROW_IN` với `referenceId` là mã đối soát COD của GHN — cùng một sổ cái cho cả online và COD (màn "Sổ cái tài chính" của Admin, UC-PAY-06).

---

## 6.8. `CodRemittance` — Phiên đối soát tiền thu hộ COD
Bảng: `cod_remittances` · Thêm ở TLCN v1.3 (màn Admin › Đối soát COD, UC-LOG-03 / UC-PAY-05)

```java
@Entity
@Table(name = "cod_remittances")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CodRemittance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, unique = true, length = 30)
    private String remittanceCode;            // Mã phiên hiển thị: "COD-20260612"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarrierType carrierType;          // Hãng chuyển tiền thu hộ về sàn: GHN / GHTK / VIETTEL_POST / JNT_EXPRESS

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal receivedAmount;        // Số tiền thực nhận vào tài khoản sàn (SePay báo)

    @Column(nullable = false, unique = true, length = 100)
    private String bankReference;             // Mã giao dịch ngân hàng do SePay gửi — UNIQUE để webhook idempotent

    @Column(nullable = false)
    private OffsetDateTime receivedAt;         // Thời điểm tiền vào tài khoản sàn

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CodRemittanceStatus status = CodRemittanceStatus.WAITING; // WAITING (chờ bảng kê) / DONE (đã đối soát)

    @Column(length = 255)
    private String statementFileName;         // NULLABLE — Tên tệp bảng kê CSV đã đối chiếu

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String linesJson;                 // NULLABLE — Kết quả đối chiếu từng dòng [{trackingCode, orderId, amount, fee, result}]

    private Long reconciledByStaffId;         // NULLABLE — FK mềm → users.id, nhân viên xác nhận (không đặt FK cứng, như kycVerifiedBy)

    private OffsetDateTime reconciledAt;       // NULLABLE — Thời điểm xác nhận đối soát

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm SePay báo tiền vào, tạo phiên
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Xác nhận đối soát khi bảng kê cân với số tiền nhận; service fund các EscrowLedger khớp — UC-LOG-03 */
    public void confirm(String fileName, String lines, Long staffId, OffsetDateTime now) {
        if (this.status != CodRemittanceStatus.WAITING) throw new IllegalStateException("Phiên đã đối soát");
        this.statementFileName = fileName;
        this.linesJson = lines;
        this.reconciledByStaffId = staffId;
        this.reconciledAt = now;
        this.status = CodRemittanceStatus.DONE;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Một phiên = một lần hãng chuyển tiền về.** Hãng vận chuyển gom tiền thu hộ của nhiều đơn rồi chuyển một khoản vào tài khoản sàn. Webhook SePay tạo phiên `WAITING`; nhân viên tài chính tải bảng kê CSV (mã vận đơn, số tiền, phí) để đối chiếu với từng đơn COD.

**b) Vì sao kết quả dòng lưu `jsonb`?** Bảng kê chỉ cần xem lại và xuất CSV, không truy vấn theo dòng; tiền thật vẫn đi qua `EscrowLedger.fundCod()` và `FinancialLedger` (`ESCROW_IN`) cho từng đơn khớp.

**c) Idempotent theo `bankReference`.** SePay gửi lại cùng giao dịch → `UNIQUE` chặn tạo phiên trùng.

---

# 7. NHÓM VẬN CHUYỂN

## 7.1. `ShipmentTracking` — Vận đơn
Bảng: `shipment_trackings`

```java
@Entity
@Table(name = "shipment_trackings")   // DDL: CREATE UNIQUE INDEX uq_shipment_outbound ON shipment_trackings(order_id) WHERE direction = 'OUTBOUND';
                                      //      CREATE UNIQUE INDEX uq_shipment_return   ON shipment_trackings(refund_request_id) WHERE direction = 'RETURN';
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ShipmentTracking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long orderId;                     // FK → orders.id — 1 vận đơn chiều đi (OUTBOUND) / đơn + 0..N vận đơn hoàn (RETURN, mỗi yêu cầu trả hàng 1 vận đơn)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ShipmentDirection direction;      // OUTBOUND (shop → khách) / RETURN (khách → shop, Reverse Logistics UC-SUP-02)

    private Long refundRequestId;             // NULLABLE — FK → refund_requests.id; BẮT BUỘC khi direction = RETURN (CHECK), NULL khi OUTBOUND

    @Column(length = 100)
    private String trackingCode;              // NULLABLE — Mã vận đơn khách dùng để tra cứu

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CarrierType carrierType;          // NULLABLE — Hãng vận chuyển được chọn khi điều phối (UC-LOG-15): GHN / GHTK / VIETTEL_POST / JNT_EXPRESS / GENIE_EXPRESS

    @Column(length = 100)
    private String carrierOrderId;            // NULLABLE — Mã đơn bên hệ thống hãng vận chuyển

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentTrackingStatus status;    // AWAITING_PICKUP → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED (hoặc FAILED/RETURNING/RETURNED/LOST)

    private OffsetDateTime estimatedDeliveryAt; // NULLABLE — Ngày giao DỰ KIẾN hãng vận chuyển cam kết

    private OffsetDateTime actualDeliveredAt;  // NULLABLE — Ngày giao THỰC TẾ

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "shipment_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua ShipmentEvent.shipmentId
    private List<ShipmentEvent> events;       // Nhật ký hành trình chi tiết
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cập nhật trạng thái từ webhook 3PL đã chuẩn hóa — UC-LOG-03 */
    public void applyCarrierStatus(ShipmentTrackingStatus newStatus, OffsetDateTime at) {
        this.status = newStatus;
        if (newStatus == ShipmentTrackingStatus.DELIVERED) this.actualDeliveredAt = at;
    }

    /** Trạng thái kết thúc — UC-ORD-09 */
    public boolean isFinal() {
        return this.status == ShipmentTrackingStatus.DELIVERED || this.status == ShipmentTrackingStatus.RETURNED || this.status == ShipmentTrackingStatus.LOST;
    }

    /** Tạo vận đơn chiều về cho yêu cầu trả hàng đã duyệt — UC-SUP-02 */
    public static ShipmentTracking forReturn(Long orderId, Long refundRequestId, CarrierType carrier) {
        ShipmentTracking t = new ShipmentTracking();
        t.orderId = orderId; t.refundRequestId = refundRequestId; t.carrierType = carrier;
        t.direction = ShipmentDirection.RETURN; t.status = ShipmentTrackingStatus.AWAITING_PICKUP;
        return t;
    }
}
```

> **Reverse Logistics:** trước đây `orderId` UNIQUE nên không thể tạo vận đơn chiều về cho cùng đơn. Nay mỗi đơn có **đúng 1** vận đơn `OUTBOUND` (partial unique trên `order_id`) và **0..N** vận đơn `RETURN` — mỗi `RefundRequest` được duyệt "trả hàng" có 1 vận đơn riêng (partial unique trên `refund_request_id`), hỗ trợ khách trả hàng nhiều đợt. Khi vận đơn `RETURN` chuyển `PICKED_UP`, Service gọi `RefundRequest.markReturnShipped()`; khi `DELIVERED` về kho shop, gọi `RefundRequest.confirmReturnReceived()`.

---

## 7.2. `ShipmentEvent` — Sự kiện hành trình
Bảng: `shipment_events`

```java
@Entity
@Table(name = "shipment_events")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ShipmentEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long shipmentId;                  // FK → shipment_trackings.id — thuộc vận đơn nào

    @Column(nullable = false, length = 100)
    private String eventStatus;               // Mã trạng thái THÔ do hãng vận chuyển gửi về

    @Column(length = 255)
    private String location;                  // NULLABLE — Địa điểm xảy ra sự kiện: "Kho Sóc Sơn - Hà Nội"

    @Column(nullable = false)
    private OffsetDateTime occurredAt;         // Thời điểm sự kiện XẢY RA (theo hãng vận chuyển)

    @Column(length = 500)
    private String description;               // NULLABLE — Mô tả dễ đọc: "Đơn hàng đã rời kho phân loại"

    @Column(nullable = false)
    private OffsetDateTime receivedAt;         // Thời điểm hệ thống NHẬN ĐƯỢC webhook này
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Chống ghi trùng webhook — UC-LOG-03 */
    public boolean isDuplicateOf(ShipmentEvent other) {
        return this.shipmentId.equals(other.getShipmentId()) && this.eventStatus.equals(other.getEventStatus()) && this.occurredAt.equals(other.getOccurredAt());
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) `occurredAt` vs `receivedAt` — hai trục thời gian khác nhau**

```
14:30  Bưu tá GHN quét mã "đã lấy hàng"   → occurredAt = 14:30
14:45  GHN xử lý xong, bắn webhook sang sàn → receivedAt = 14:45
```

Chênh lệch có thể lên tới hàng giờ khi hệ thống hãng vận chuyển bị dồn hàng đợi. Tách 2 mốc cho phép:
1. Hiển thị cho khách **mốc thật** (`occurredAt`), không phải mốc hệ thống biết tin.
2. Đo **độ trễ tích hợp** để khiếu nại với hãng khi webhook chậm.
3. Xử lý **webhook đến sai thứ tự** — sắp xếp lại theo `occurredAt` thay vì theo thứ tự nhận.

**b) Event Sourcing cho hành trình đơn hàng**
`ShipmentTracking.status` là **trạng thái hiện tại** (đọc nhanh). `ShipmentEvent` là **toàn bộ lịch sử** dẫn tới trạng thái đó.
> Vì sao cần cả hai? Nếu chỉ lưu trạng thái hiện tại, khách không thấy được hành trình *"Đã rời Hà Nội → Đến Đà Nẵng → Đang giao"*. Nếu chỉ lưu sự kiện, mỗi lần hiển thị danh sách đơn phải quét toàn bộ lịch sử → chậm. Kết hợp cả hai là mẫu **CQRS nhẹ**: ghi theo sự kiện, đọc theo trạng thái đã tổng hợp.

**c) `eventStatus` lưu mã THÔ của hãng vận chuyển**
Mỗi hãng vận chuyển (MVP chỉ GHN; sau này thêm GHTK, VTP) có bộ mã riêng (`ready_to_pick`, `picking`, `storing`...). Hệ thống lưu nguyên mã thô rồi mới ánh xạ sang enum chuẩn hoá của sàn.
> **Lợi ích:** khi ánh xạ sai, dữ liệu thô vẫn còn để chạy lại. Khi tích hợp hãng mới, chỉ cần viết thêm bảng ánh xạ chứ không phải đổi cấu trúc DB. Đây là nguyên tắc **Anti-Corruption Layer** trong DDD: dữ liệu ngoại lai không được làm ô nhiễm mô hình nội bộ.

**d) Các trạng thái "xấu" đều được mô hình hoá tường minh**
`FAILED` (giao thất bại), `RETURNING` (đang hoàn về), `RETURNED` (đã hoàn về shop), `LOST` (thất lạc). Đây là những tình huống **chắc chắn sẽ xảy ra** trong vận hành thực tế, và mỗi trạng thái kéo theo một luồng nghiệp vụ riêng (hoàn tiền, bồi thường, nhập lại kho). Thiết kế thiếu các trạng thái này là dấu hiệu hệ thống chưa sẵn sàng cho sản xuất.

---

## 7.3. `VendorShippingConfig` — Cấu hình vận chuyển của shop
Bảng: `vendor_shipping_config`

```java
@Entity
@Table(name = "vendor_shipping_config")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VendorShippingConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long vendorId;                    // FK → users.id, UNIQUE — mỗi shop 1 cấu hình

    @Column(name = "ghn_enabled", nullable = false)
    private Boolean ghnEnabled;               // Bật/tắt Giao Hàng Nhanh (mặc định bật)

    @Column(name = "ghtk_enabled", nullable = false)
    private Boolean ghtkEnabled;              // Bật/tắt Giao Hàng Tiết Kiệm (mặc định bật)

    @Column(name = "vtp_enabled", nullable = false)
    private Boolean vtpEnabled;               // Bật/tắt Viettel Post (mặc định tắt)

    @Column(name = "jt_enabled", nullable = false)
    private Boolean jtEnabled;                // Bật/tắt J&T Express (mặc định tắt)

    @Column(name = "genie_express_enabled", nullable = false)
    private Boolean genieExpressEnabled;    // Bật/tắt giao nội thành Genie Express (FBL) — TLCN chỉ là lựa chọn, không có app tài xế (mặc định tắt)

    @Column(precision = 15, scale = 2)
    private BigDecimal freeshipThreshold;     // NULLABLE — Ngưỡng đơn tối thiểu để shop MIỄN PHÍ ship
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đơn đạt ngưỡng shop miễn phí ship — UC-ORD-06 */
    public boolean isFreeshipFor(BigDecimal orderValue) {
        return this.freeshipThreshold != null && orderValue.compareTo(this.freeshipThreshold) >= 0;
    }

    /** Shop có bật hãng vận chuyển này không — UC-ORD-06 */
    public boolean supports(CarrierType carrier) {
        return switch (carrier) {
            case GHN -> Boolean.TRUE.equals(this.ghnEnabled);
            case GHTK -> Boolean.TRUE.equals(this.ghtkEnabled);
            case VIETTEL_POST -> Boolean.TRUE.equals(this.vtpEnabled);
            case GENIE_EXPRESS -> Boolean.TRUE.equals(this.genieExpressEnabled);
        };
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Thiết kế cột-cho-mỗi-hãng: đơn giản nhưng khó mở rộng**
Mỗi hãng vận chuyển là 1 cột boolean riêng. Ưu điểm: query cực nhanh, code dễ đọc. Nhược điểm: **thêm hãng mới phải ALTER TABLE**, và với hệ thống lớn đó là thao tác phải lên lịch bảo trì.
> Phương án linh hoạt hơn: bảng `vendor_carriers(vendor_id, carrier_code, enabled)`. Thêm hãng chỉ cần INSERT.
> Dự án chọn cột cứng vì số hãng ở Việt Nam ít và ổn định (~5 hãng). Đây là đánh đổi hợp lý, không phải lỗi thiết kế — nhưng cần biết giới hạn của nó.

**b) `freeshipThreshold` — đòn bẩy tăng giá trị đơn hàng trung bình (AOV)**
Đặt ngưỡng *"Miễn phí vận chuyển cho đơn từ 300.000đ"* khiến khách đang có giỏ 250.000đ có xu hướng mua thêm để đạt mốc. Đây là kỹ thuật kinh doanh chuẩn của TMĐT, và trường này chính là nơi cấu hình nó.
> Lưu ý phân biệt với mã `FREESHIP` của sàn: ngưỡng này do **shop tự chịu chi phí** (dẫn tới `EscrowLedger.shopShippingSubsidy`), còn mã freeship của sàn do **sàn chịu** (dẫn tới `platformShippingFee`).

**c) Vì sao tách khỏi `VendorProfile`?**
`VendorProfile` đã rất rộng (hơn 30 cột). Cấu hình vận chuyển là **mối quan tâm riêng biệt** (separation of concerns), chỉ dùng trong luồng checkout. Tách bảng giúp `vendor_profiles` nhẹ hơn khi đọc ở các luồng khác, và cho phép mở rộng cấu hình vận chuyển mà không đụng vào bảng hồ sơ.

---

# 8. NHÓM HOÀN TIỀN / KHIẾU NẠI

## 8.1. `RefundRequest` — Yêu cầu hoàn tiền
Bảng: `refund_requests`

```java
@Entity
@Table(name = "refund_requests")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class RefundRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long orderId;                     // FK → orders.id — hoàn tiền cho đơn nào

    @Column(nullable = false)
    private Long requestedByUserId;           // FK → users.id — ai gửi yêu cầu (thường là khách)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;          // Tổng số tiền yêu cầu hoàn

    @Column(columnDefinition = "TEXT")
    private String reason;                    // NULLABLE — Lý do khách đưa ra: "Hàng lỗi", "Sai mẫu"...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundRequestType requestType;    // Phương án KHÁCH chọn: RETURN_AND_REFUND (gửi trả hàng rồi hoàn tiền) / REFUND_ONLY (hoàn tiền không cần trả hàng — thiếu hàng, hư hỏng một phần) — UC-SUP-01

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RefundResolution resolutionMode;  // NULLABLE — Phương án được DUYỆT (shop / sàn / tự động): APPROVE_WITH_RETURN (sinh vận đơn chiều về) / APPROVE_REFUND_ONLY (hoàn ngay) — UC-SUP-02

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundStatus status;              // REQUESTED / AWAITING_EVIDENCE / SHOP_REJECTED / DISPUTED / AWAITING_RETURN_SHIPMENT / RETURN_FAILED / APPROVED / REJECTED / REFUNDED (9 trạng thái)

    private Long resolvedByStaffId;           // NULLABLE — FK mềm → users.id, staff nào đã xử lý

    @Column(columnDefinition = "TEXT")
    private String resolutionNote;            // NULLABLE — Ghi chú của staff về quyết định

    @Column(nullable = false)
    private OffsetDateTime requestedAt;        // Thời điểm khách gửi yêu cầu

    @Column(nullable = false)
    private OffsetDateTime shopResponseDueAt;  // Hạn shop phải phản hồi = requestedAt + 48h; đặt lại now + 48h khi khách nộp bằng chứng bổ sung

    private OffsetDateTime evidenceRequestedAt; // NULLABLE — Thời điểm shop yêu cầu bổ sung bằng chứng (tối đa 1 lần); khách có 48 giờ

    @Column(length = 500)
    private String evidenceRequestNote;       // NULLABLE — Shop ghi rõ cần bổ sung gì (vd: "Video mở hộp thấy rõ mã vận đơn")

    private OffsetDateTime resolvedAt;         // NULLABLE — Thời điểm staff ra quyết định

    private OffsetDateTime shopRejectedAt;     // NULLABLE — Thời điểm shop từ chối; mốc 48 giờ để khách khiếu nại lên sàn (Cron đóng yêu cầu nếu quá hạn)

    private OffsetDateTime returnDueAt;        // NULLABLE — Hạn khách bàn giao hàng hoàn cho ĐVVC = thời điểm duyệt + 7 ngày (chỉ khi APPROVE_WITH_RETURN)

    private OffsetDateTime returnShippedAt;    // NULLABLE — ĐVVC đã lấy hàng hoàn (vận đơn RETURN → PICKED_UP); từ mốc này Cron không còn đóng yêu cầu vì quá hạn

    private OffsetDateTime returnReceivedAt;   // NULLABLE — Hàng hoàn đã về kho shop (vận đơn RETURN → DELIVERED) → chuyển APPROVED để hoàn tiền

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Lần thay đổi gần nhất (@UpdateTimestamp) — truy vấn "yêu cầu không cập nhật quá 24 giờ" cho BI/SLA

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "refund_request_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua RefundRequestItem.refundRequestId
    private List<RefundRequestItem> items;    // Các sản phẩm cụ thể muốn trả

    @OneToMany
    @JoinColumn(name = "refund_request_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua RefundEvidence.refundRequestId
    private List<RefundEvidence> evidences;   // Ảnh/video bằng chứng
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Shop đồng ý: hoàn tiền ngay (APPROVE_REFUND_ONLY) hoặc cho trả hàng (APPROVE_WITH_RETURN → chờ hàng hoàn) — UC-SUP-02 */
    public void approveByShop(RefundResolution mode, String note, OffsetDateTime now) {
        requireStatus(RefundStatus.REQUESTED);
        applyApproval(mode, now);
        this.resolutionNote = note;
    }

    /** Áp phương án duyệt: REFUND_ONLY chỉ được duyệt hoàn ngay (shop muốn lấy lại hàng thì phải từ chối / nhờ sàn); RETURN_AND_REFUND được chọn cả hai */
    private void applyApproval(RefundResolution mode, OffsetDateTime now) {
        if (this.requestType == RefundRequestType.REFUND_ONLY && mode == RefundResolution.APPROVE_WITH_RETURN)
            throw new IllegalArgumentException("Khách chọn hoàn tiền không trả hàng — không thể buộc trả hàng khi chấp thuận");
        this.resolutionMode = mode;
        this.resolvedAt = now;
        if (mode == RefundResolution.APPROVE_WITH_RETURN) {
            this.status = RefundStatus.AWAITING_RETURN_SHIPMENT;   // Service sinh vận đơn ShipmentTracking.forReturn(...)
            this.returnDueAt = now.plusDays(7);
        } else {
            this.status = RefundStatus.APPROVED;                   // hoàn ngay (UC-SUP-04)
        }
    }

    /** Shop từ chối — UC-SUP-02 */
    public void shopReject(String note, OffsetDateTime now) {
        requireStatus(RefundStatus.REQUESTED);
        this.status = RefundStatus.SHOP_REJECTED;
        this.resolutionNote = note;
        this.shopRejectedAt = now;   // khách có 48 giờ để khiếu nại lên sàn
    }

    /** Shop yêu cầu khách bổ sung bằng chứng (tạm dừng đồng hồ 48 giờ của shop) — UC-SUP-02 */
    public void requestMoreEvidence(String note, OffsetDateTime now) {
        requireStatus(RefundStatus.REQUESTED);
        if (this.evidenceRequestedAt != null) throw new IllegalStateException("Shop chỉ được yêu cầu bổ sung bằng chứng 1 lần");
        this.status = RefundStatus.AWAITING_EVIDENCE;
        this.evidenceRequestNote = note;
        this.evidenceRequestedAt = now;   // khách có 48 giờ để bổ sung
    }

    /** Khách đã tải bằng chứng bổ sung → quay về chờ shop — UC-SUP-01 */
    public void submitAdditionalEvidence(OffsetDateTime now) {
        requireStatus(RefundStatus.AWAITING_EVIDENCE);
        this.status = RefundStatus.REQUESTED;
        this.shopResponseDueAt = now.plusHours(48);   // shop có lại đủ 48 giờ
    }

    /** Cron: quá 48 giờ khách không bổ sung → đóng yêu cầu — UC-SUP-02 */
    public boolean closeIfEvidenceNotProvided(OffsetDateTime now) {
        if (this.status != RefundStatus.AWAITING_EVIDENCE || this.evidenceRequestedAt.plusHours(48).isAfter(now)) return false;
        this.status = RefundStatus.REJECTED;
        this.resolutionNote = "Đóng: khách không bổ sung bằng chứng trong 48 giờ";
        this.resolvedAt = now;
        return true;
    }

    /** Cron: quá shopResponseDueAt (48 giờ) shop không phản hồi → tự động chấp thuận theo đúng phương án khách đã chọn — UC-SUP-02 */
    public boolean autoApproveIfShopSilent(OffsetDateTime now) {
        if (this.status != RefundStatus.REQUESTED || this.shopResponseDueAt.isAfter(now)) return false;
        applyApproval(this.requestType == RefundRequestType.RETURN_AND_REFUND ? RefundResolution.APPROVE_WITH_RETURN : RefundResolution.APPROVE_REFUND_ONLY, now);
        this.resolutionNote = "Tự động chấp thuận: shop không phản hồi trong 48 giờ";
        return true;
    }

    /** ĐVVC đã lấy hàng hoàn từ khách (vận đơn RETURN → PICKED_UP) — UC-SUP-02 */
    public void markReturnShipped(OffsetDateTime now) {
        requireStatus(RefundStatus.AWAITING_RETURN_SHIPMENT);
        if (this.returnShippedAt == null) this.returnShippedAt = now;
    }

    /** Hàng hoàn đã về kho shop (vận đơn RETURN → DELIVERED) → sẵn sàng hoàn tiền — UC-SUP-02 */
    public void confirmReturnReceived(OffsetDateTime now) {
        requireStatus(RefundStatus.AWAITING_RETURN_SHIPMENT);
        if (this.returnShippedAt == null) throw new IllegalStateException("Chưa có ghi nhận ĐVVC lấy hàng hoàn");
        this.returnReceivedAt = now;
        this.status = RefundStatus.APPROVED;
    }

    /** Cron: quá returnDueAt (7 ngày) khách chưa giao hàng hoàn cho ĐVVC → RETURN_FAILED, đóng tranh chấp, giải ngân bình thường — UC-SUP-02 */
    public boolean failReturnIfOverdue(OffsetDateTime now) {
        if (this.status != RefundStatus.AWAITING_RETURN_SHIPMENT || this.returnShippedAt != null || this.returnDueAt.isAfter(now)) return false;
        this.status = RefundStatus.RETURN_FAILED;
        this.resolutionNote = "Đóng: khách không gửi hàng hoàn trong 7 ngày";
        return true;
    }

    /** Khách khiếu nại lên sàn (trong 48 giờ sau khi shop từ chối / không đồng ý yêu cầu bổ sung) hoặc shop chủ động nhờ sàn — UC-SUP-03 */
    public void escalateToDispute() {
        if (this.status != RefundStatus.REQUESTED && this.status != RefundStatus.SHOP_REJECTED && this.status != RefundStatus.AWAITING_EVIDENCE) throw new IllegalStateException("Không thể khiếu nại");
        this.status = RefundStatus.DISPUTED;
    }

    /** Cron: quá 48 giờ khách không khiếu nại → đóng yêu cầu (không hoàn tiền) — UC-SUP-02 */
    public boolean closeIfCustomerSilent(OffsetDateTime now) {
        if (this.status != RefundStatus.SHOP_REJECTED || this.shopRejectedAt.plusHours(48).isAfter(now)) return false;
        this.status = RefundStatus.REJECTED;
        this.resolutionNote = "Đóng: khách không khiếu nại trong 48 giờ sau khi shop từ chối";
        this.resolvedAt = now;
        return true;
    }

    /** Sàn phân xử chấp thuận hoàn tiền (chọn có / không thu hồi hàng) — UC-SUP-03 */
    public void approve(Long staffId, RefundResolution mode, String note, OffsetDateTime now) {
        requireStatus(RefundStatus.DISPUTED);
        applyApproval(mode, now);
        this.resolvedByStaffId = staffId; this.resolutionNote = note;
    }

    /** Sàn bác khiếu nại — UC-SUP-03 */
    public void reject(Long staffId, String note, OffsetDateTime now) {
        requireStatus(RefundStatus.DISPUTED);
        this.status = RefundStatus.REJECTED;
        this.resolvedByStaffId = staffId; this.resolutionNote = note; this.resolvedAt = now;
    }

    /** Đã hoàn đúng nguồn (chỉ một lần) — UC-SUP-04 */
    public void markRefunded() {
        requireStatus(RefundStatus.APPROVED);
        this.status = RefundStatus.REFUNDED;
    }

    /** Bảo vệ máy trạng thái */
    private void requireStatus(RefundStatus s) {
        if (this.status != s) throw new IllegalStateException("Yêu cầu hoàn không ở " + s);
    }
}
```

### Đồng bộ trạng thái `RefundRequest` ↔ `Order` ↔ `EscrowLedger`

Mỗi chuyển trạng thái của yêu cầu hoàn tiền được thực hiện trong **cùng một transaction** với đơn hàng và ký quỹ (Service điều phối, entity tự bảo vệ bất biến):

| `RefundRequest` | `Order` | `EscrowLedger` | Ghi chú |
|---|---|---|---|
| Tạo `REQUESTED` | `requestReturn()`: `DELIVERED` (hoặc `PAID` với COD đã đối soát) → `RETURN_REQUESTED` | `dispute()`: `HELD` / `PENDING_FUNDING` → `DISPUTED` (chặn Cron giải ngân) | Trong 7 ngày kể từ `deliveredAt` (UC-SUP-01) |
| `REQUESTED` → `APPROVED` (shop / Cron duyệt `APPROVE_REFUND_ONLY`) | `approveReturn()` → `RETURN_APPROVED` | giữ `DISPUTED` | UC-SUP-02 — hoàn tiền ngay, không có vận đơn chiều về |
| `REQUESTED` → `AWAITING_RETURN_SHIPMENT` (shop / Cron `autoApproveIfShopSilent` duyệt `APPROVE_WITH_RETURN`) | `approveReturn()` → `RETURN_APPROVED` | giữ `DISPUTED` | Sinh `ShipmentTracking` (`direction = RETURN`); `returnDueAt = now + 7 ngày` |
| `AWAITING_RETURN_SHIPMENT` (vận đơn RETURN `PICKED_UP`) | giữ `RETURN_APPROVED` | giữ `DISPUTED` | `markReturnShipped()` — từ đây không còn bị đóng vì quá hạn |
| `AWAITING_RETURN_SHIPMENT` → `APPROVED` (vận đơn RETURN `DELIVERED` về kho shop) | giữ `RETURN_APPROVED` | giữ `DISPUTED` | `confirmReturnReceived()` → chạy UC-SUP-04 |
| `AWAITING_RETURN_SHIPMENT` → `RETURN_FAILED` (Cron `failReturnIfOverdue`, quá 7 ngày chưa gửi) | `complete()` → `COMPLETED` | `releaseDisputeHold()` → `HELD` → giải ngân bình thường | Không hoàn tiền; tiền shop không bị khóa vô thời hạn |
| `REQUESTED` → `AWAITING_EVIDENCE` (shop yêu cầu bổ sung, tối đa 1 lần) | giữ `RETURN_REQUESTED` | giữ `DISPUTED` | Ghi `evidenceRequestedAt`; khách có 48 giờ để nộp |
| `AWAITING_EVIDENCE` → `REQUESTED` (khách nộp bổ sung) | giữ `RETURN_REQUESTED` | giữ `DISPUTED` | `shopResponseDueAt = now + 48h` (shop có lại đủ 48 giờ) |
| `AWAITING_EVIDENCE` → `REJECTED` (Cron `closeIfEvidenceNotProvided` sau 48 giờ) | `rejectReturn()` → `complete()` | `releaseDisputeHold()` | Không hoàn tiền |
| `REQUESTED` → `SHOP_REJECTED` | giữ `RETURN_REQUESTED` | giữ `DISPUTED` | Ghi `shopRejectedAt`; khách có 48 giờ để khiếu nại |
| `SHOP_REJECTED` → `DISPUTED` | giữ `RETURN_REQUESTED` | giữ `DISPUTED` | Khách bấm "Khiếu nại lên Sàn" (UC-SUP-03) |
| `SHOP_REJECTED` → `REJECTED` (Cron `closeIfCustomerSilent` sau 48 giờ) / `DISPUTED` → `REJECTED` (sàn bác) | `rejectReturn()` → `RETURN_REJECTED` → `complete()` → `COMPLETED` | `releaseDisputeHold()` → `HELD` (hoặc `PENDING_FUNDING` nếu COD chưa nộp) → giải ngân bình thường | Không hoàn tiền |
| `DISPUTED` → `APPROVED` / `AWAITING_RETURN_SHIPMENT` (sàn chấp thuận, chọn `RefundResolution`) | `approveReturn()` → `RETURN_APPROVED` | giữ `DISPUTED` | UC-SUP-03 |
| `APPROVED` → `REFUNDED` | `markRefunded(amount)` → `REFUNDED` (hoàn đủ) hoặc `PARTIALLY_REFUNDED` (hoàn một phần, khách còn trả tiếp được trong hạn 7 ngày); cộng dồn `refundedAmount` | Toàn phần: `markRefunded()` → `REFUNDED`. **Một phần:** `applyPartialRefund(refundGross, refundCommission)` giảm `grossAmount` và hoa hồng của đúng các dòng bị trả, tính lại `netAmount`, rồi `releaseDisputeHold()` → giải ngân **netAmount mới** | Hoàn đúng nguồn: VNPay refund API / chuyển khoản tay với COD (UC-SUP-04) |

> **Ví dụ hoàn một phần (chống lỗ vốn sàn):** đơn 1.000.000đ, hoa hồng 5%, không hỗ trợ ship → `grossAmount` 1.000.000 / `commissionFee` 50.000 / `netAmount` 950.000. Khách trả 1 món 300.000đ (hoa hồng dòng đó 15.000) → `applyPartialRefund(300.000, 15.000)` → `grossAmount` 700.000 / `commissionFee` 35.000 / `netAmount` 665.000. Dòng tiền: khách 300.000 + shop 665.000 + sàn 35.000 = **1.000.000đ** (nếu vẫn giải ngân 950.000 như cũ sàn sẽ lỗ 250.000đ).

> Cron `RefundDeadlineJob` (15 phút/lần) quét ba mốc 48 giờ và một mốc 7 ngày: `status = REQUESTED AND shop_response_due_at < now()` → `autoApproveIfShopSilent()`; `status = AWAITING_EVIDENCE AND evidence_requested_at < now() − 48h` → `closeIfEvidenceNotProvided()`; `status = SHOP_REJECTED AND shop_rejected_at < now() − 48h` → `closeIfCustomerSilent()`; `status = AWAITING_RETURN_SHIPMENT AND return_shipped_at IS NULL AND return_due_at < now()` → `failReturnIfOverdue()` (+ `Order.complete()`, `EscrowLedger.releaseDisputeHold()`). Mỗi bản ghi xử lý trong transaction riêng, khóa `SELECT … FOR UPDATE SKIP LOCKED` để nhiều Pod chạy song song không xử lý trùng.

---

## 8.2. `RefundRequestItem` — Dòng sản phẩm muốn hoàn
Bảng: `refund_request_items`

```java
@Entity
@Table(name = "refund_request_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class RefundRequestItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long refundRequestId;             // FK → refund_requests.id

    @Column(nullable = false)
    private Long orderItemId;                 // FK → order_items.id — hoàn dòng hàng nào của đơn

    @Column(nullable = false)
    private Integer quantity;                 // Số lượng muốn trả (có thể ÍT HƠN số đã mua)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal itemRefundAmount;      // Số tiền hoàn cho dòng này (đã tính cả phân bổ giảm giá)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Số tiền hoàn của dòng (đã trừ chiết khấu phân bổ) — UC-SUP-01 */
    public BigDecimal amount() {
        return this.itemRefundAmount;
    }
}
```

---

## 8.3. `RefundEvidence` — Bằng chứng khiếu nại
Bảng: `refund_evidences`

```java
@Entity
@Table(name = "refund_evidences")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class RefundEvidence {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long refundRequestId;             // FK → refund_requests.id

    @Column(nullable = false, length = 500)
    private String mediaUrl;                  // Link ảnh/video bằng chứng khách upload

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;              // IMAGE hoặc VIDEO

    @Column(nullable = false)
    private Integer displayOrder;             // Thứ tự hiển thị

    @Column(nullable = false)
    private Integer submissionRound;          // 1 = nộp khi tạo yêu cầu; 2 = nộp bổ sung theo yêu cầu của shop

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm upload
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bằng chứng là video — UC-SUP-01 */
    public boolean isVideo() {
        return this.mediaType == MediaType.VIDEO;
    }

    /** Bằng chứng nộp bổ sung theo yêu cầu của shop — UC-SUP-01 */
    public boolean isAdditional() {
        return this.submissionRound != null && this.submissionRound > 1;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Hoàn tiền TỪNG PHẦN (Partial Refund) — vì sao cần bảng `RefundRequestItem`?**
Nếu chỉ lưu `refundAmount` ở cấp đơn, hệ thống sẽ không biết **trả lại món nào, mấy cái**. Nhưng đó là thông tin bắt buộc để:
1. **Nhập lại kho** đúng biến thể, đúng số lượng.
2. Tính lại **hoa hồng** — sàn phải trả lại phần hoa hồng của món bị hoàn.
3. Cập nhật lại **số lượng đã bán** của sản phẩm.

Ví dụ: đơn có 3 áo + 1 quần, khách chỉ trả 1 áo bị lỗi → chỉ nhập lại kho 1 áo, chỉ hoàn tiền tương ứng 1 áo.

**b) `itemRefundAmount` KHÔNG bằng `unitPrice × quantity`**
Đây là điểm dễ sai nhất. Phải dùng giá **sau khi đã phân bổ giảm giá** (xem `OrderItem.proratedDiscount`):

```
Khách mua áo 300.000đ, đơn được giảm 100.000đ, phân bổ cho áo là 30.000đ
→ Khách thực trả cho áo: 270.000đ
→ Hoàn đúng 270.000đ, KHÔNG PHẢI 300.000đ
```

Hoàn 300.000đ nghĩa là khách **lời 30.000đ** từ việc trả hàng — lỗ hổng này bị khai thác rất nhanh trong thực tế.

**c) `RefundEvidence` — bằng chứng là trung tâm của xử lý tranh chấp**
Khiếu nại luôn là lời của khách chống lại lời của shop. Ảnh/video là dữ liệu khách quan duy nhất. Thiết kế tách bảng riêng (thay vì nhét mảng URL vào 1 cột JSON) cho phép:
- Sắp xếp thứ tự hiển thị (`displayOrder`).
- Phân biệt ảnh và video để render khác nhau.
- Mở rộng sau này: thêm cột ai upload (khách hay shop phản biện), thời điểm, kích thước.

**d) Vòng đời và điểm nối với escrow**

```
REQUESTED         → Khách vừa gửi        → EscrowLedger.status chuyển sang DISPUTED (ĐÓNG BĂNG tiền!)
AWAITING_EVIDENCE → Shop cần thêm bằng chứng → khách có 48 giờ để nộp bổ sung
SHOP_REJECTED     → Shop từ chối          → khách có 48 giờ để khiếu nại lên sàn (DISPUTED)
DISPUTED          → Sàn phân xử           → Staff CS xem bằng chứng hai bên
APPROVED          → Shop/sàn đồng ý        → sinh PaymentRefund, gọi API refund VNPay (COD: chuyển khoản tay)
REFUNDED          → Cổng xác nhận         → tiền đã về tài khoản khách, ghi SystemLog (REFUND_PROCESSED)
REJECTED          → Bị bác / quá hạn      → EscrowLedger trở lại HELD, tiếp tục đếm ngược giải ngân
```
(Bảng chuyển trạng thái đầy đủ: mục "Đồng bộ trạng thái `RefundRequest` ↔ `Order` ↔ `EscrowLedger`" ở trên.)

> **Điểm then chốt:** ngay khi có khiếu nại, tiền ký quỹ phải bị **đóng băng** (`DISPUTED`). Nếu không, job giải ngân tự động có thể chuyển tiền cho shop trước khi tranh chấp được giải quyết — lúc đó sàn phải tự bỏ tiền túi đền khách.

**e) `resolvedByStaffId` + `resolutionNote` — chống lạm quyền nội bộ**
Duyệt hoàn tiền là quyền có thể bị lạm dụng (staff thông đồng với khách). Ghi rõ ai quyết định và lý do, kết hợp với `SystemLog` (`REFUND_PROCESSED`), tạo ra dấu vết kiểm toán đầy đủ.

---

# 9. NHÓM ĐÁNH GIÁ & TƯƠNG TÁC SHOP

## 9.1. `ProductReview` — Đánh giá sản phẩm
Bảng: `product_reviews`

```java
@Entity
@Table(name = "product_reviews")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductReview {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long orderItemId;                 // FK → order_items.id, UNIQUE — ĐẢM BẢO ĐÁNH GIÁ THẬT

    @Column(nullable = false)
    private Long productId;                   // FK → products.id — đánh giá sản phẩm nào (denormalize để query nhanh)

    @Column(nullable = false)
    private Long customerId;                  // FK → users.id — ai đánh giá

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop nào bị/được đánh giá

    @Column(nullable = false)
    private Integer rating;                   // Số sao 1–5

    @Column(columnDefinition = "TEXT")
    private String comment;                   // NULLABLE — Nội dung nhận xét

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String imageUrlsJson;             // NULLABLE — Mảng JSON các link ảnh khách đính kèm

    @Column(length = 500)
    private String videoUrl;                  // NULLABLE — Video review ngắn ≤ 15 giây (Review Shorts) trên Silo

    @Column(columnDefinition = "TEXT")
    private String vendorReply;               // NULLABLE — Phản hồi của shop

    private OffsetDateTime vendorRepliedAt;    // NULLABLE — Thời điểm shop phản hồi

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVisible = true;         // false = bị staff ẩn do vi phạm, nhưng vẫn giữ dữ liệu

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm đánh giá
    @Builder.Default
    @Column(nullable = false)
    private Integer helpfulCount = 0;         // Số lượt bấm "Hữu ích" (đếm sẵn, đồng bộ review_helpful_votes) — sắp xếp đánh giá hữu ích lên đầu (UC-SOC-03)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAnonymous = false;      // Đánh giá ẩn tên — hiển thị dạng "L***g" — UC-SOC-03 (TLCN v1.3)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String aspectSentimentJson;       // NULLABLE — Kết quả PhoBERT (UC-AI-07). TLCN lưu cảm xúc tổng thể: {"overall":"NEGATIVE","score":0.93,"model":"phobert-sentiment-v1","analyzedAt":"..."}; DATN thêm khóa theo khía cạnh (quality, delivery, packaging, service). NULL = chưa phân tích / đánh giá không có chữ

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Shop phản hồi đánh giá — UC-SOC-04 */
    public void reply(String text, OffsetDateTime now) {
        if (this.vendorReply != null) throw new IllegalStateException("Shop chỉ phản hồi 1 lần");
        this.vendorReply = text;
        this.vendorRepliedAt = now;
    }

    /** Ẩn đánh giá vi phạm (do con người quyết định) — UC-SOC-03 */
    public void hide() {
        this.isVisible = false;
    }

    /** Cộng / trừ bộ đếm "Hữu ích" khi thêm / xóa ReviewHelpfulVote — UC-SOC-03 */
    public void adjustHelpful(int delta) {
        this.helpfulCount = Math.max(0, this.helpfulCount + delta);
    }

    /** Gắn kết quả phân tích cảm xúc PhoBERT do AI Worker gọi lại — UC-AI-07 */
    public void attachSentiment(String aspectJson) {
        this.aspectSentimentJson = aspectJson;
    }

}
```

### 💡 Kiến thức chuyên sâu

**a) `orderItemId` UNIQUE — thiết kế quan trọng nhất của bảng này**
Ràng buộc này đồng thời giải quyết **hai vấn đề lớn**:

1. **Chống đánh giá ảo (Verified Purchase):** Muốn đánh giá thì phải có `orderItemId` — nghĩa là **phải thật sự mua và nhận hàng**. Không mua thì không có dòng nào trong `order_items` để trỏ tới. Đây là lý do review trên Shopee/Amazon có nhãn "Đã mua hàng" và đáng tin hơn review tự do.
2. **Chống spam:** Mỗi dòng hàng chỉ đánh giá được đúng 1 lần. Không thể vào rate 5 sao 100 lần cho shop nhà mình.

> Lưu ý: khách mua **2 lần** cùng 1 sản phẩm sẽ có 2 `orderItem` khác nhau → đánh giá được 2 lần. Điều này **hợp lý**, vì họ đã trả tiền 2 lần và có thể có trải nghiệm khác nhau.

**b) `productId` và `vendorId` là dữ liệu phi chuẩn hoá có chủ đích**
Về lý thuyết có thể suy ra: `orderItem → productVariant → product → vendor`. Nhưng trang chi tiết sản phẩm phải load review liên tục, và chuỗi 3 lần JOIN là quá tốn kém. Sao chép sẵn 2 khoá này cho phép:

```sql
SELECT * FROM product_reviews WHERE product_id = ? AND is_visible = true
```

Chỉ 1 bảng, 1 index, không JOIN.

**c) `isVisible` thay vì xoá review xấu**
Khi review vi phạm (chửi bậy, spam), staff ẩn chứ không xoá. Vì sao?
- **Minh bạch:** nếu cho xoá cứng, shop có thể gây sức ép để xoá mọi review 1 sao → điểm đánh giá mất ý nghĩa.
- **Kiểm toán:** cần chứng minh được vì sao review bị ẩn, và ai ẩn.
- **Khôi phục:** nếu ẩn nhầm, chỉ cần bật lại.

**d) `vendorReply` nhúng thẳng thay vì bảng riêng**
Shop chỉ được phản hồi **1 lần cho 1 review** (không phải cuộc hội thoại nhiều lượt). Quan hệ 1-1 nên nhúng 2 cột vào là hợp lý, tránh thêm bảng và thêm JOIN.
> Nếu nghiệp vụ mai sau cho phép trao đổi qua lại nhiều lượt, đây sẽ là lúc phải tách thành bảng `review_replies` riêng.

---

## 9.2. `ShopFollow` — Theo dõi shop
Bảng: `shop_follows`

```java
@Entity
@Table(name = "shop_follows", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"customerId", "vendorId"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ShopFollow {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long customerId;                  // FK → users.id — người theo dõi (UNIQUE ghép với vendorId)

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop được theo dõi

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm bấm theo dõi
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Theo dõi shop (UNIQUE customer+vendor) — UC-SOC-06 */
    public static ShopFollow of(Long customerId, Long vendorId, OffsetDateTime now) {
        ShopFollow f = new ShopFollow();
        f.customerId = customerId; f.vendorId = vendorId; f.createdAt = now;
        return f;
    }
}
```

### 💡 Kiến thức chuyên sâu

**Bảng nối N-N cho quan hệ xã hội, và giá trị marketing của nó**
Một khách theo dõi nhiều shop; một shop có nhiều người theo dõi. UNIQUE `(customerId, vendorId)` chống theo dõi trùng.
Giá trị thật của bảng này là **kênh marketing miễn phí cho vendor**: khi shop tung mã giảm giá mới, hệ thống gửi thông báo `NEW_VENDOR_VOUCHER` tới toàn bộ người theo dõi — chi phí gần bằng 0 so với quảng cáo trả tiền. Đây là lý do mọi sàn đều có nút "Theo dõi shop".

---

## 9.3. `ReviewReport` — Báo cáo đánh giá vi phạm
Bảng: `review_reports`

```java
@Entity
@Table(name = "review_reports")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ReviewReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long reviewId;                    // FK → product_reviews.id — báo cáo review nào

    @Column(nullable = false)
    private Long reporterId;                  // FK → users.id — ai báo cáo (thường là shop bị đánh giá)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewReportReason reason;        // SPAM / OFFENSIVE (xúc phạm) / FAKE_PURCHASE (mua giả)

    @Column(columnDefinition = "TEXT")
    private String note;                      // NULLABLE — Giải thích thêm của người báo cáo

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm báo cáo
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Báo cáo đánh giá vi phạm — UC-SOC-05 */
    public static ReviewReport of(Long reviewId, Long reporterId, ReviewReportReason reason, String note, OffsetDateTime now) {
        ReviewReport r = new ReviewReport();
        r.reviewId = reviewId; r.reporterId = reporterId; r.reason = reason; r.note = note; r.createdAt = now;
        return r;
    }
}
```

### 💡 Kiến thức chuyên sâu

**Kiểm duyệt dựa vào cộng đồng (Community Moderation)**
Sàn không thể thuê người đọc hết hàng triệu review. Giải pháp: để người dùng và shop báo cáo, hệ thống ưu tiên xử lý theo số lượt báo cáo. Review bị báo cáo nhiều lần sẽ được đẩy lên đầu hàng đợi kiểm duyệt.
> **Rủi ro cần lường trước:** shop có thể lạm dụng để vùi dập review tiêu cực hợp lệ. Vì thế báo cáo **chỉ là tín hiệu ưu tiên**, quyết định ẩn (`ProductReview.isVisible = false`) phải do con người đưa ra, và mọi thao tác ẩn đều ghi vào `SystemLog` (`REVIEW_HIDDEN`).

**`FAKE_PURCHASE` — nghịch lý thú vị**
Dù `orderItemId` UNIQUE đã đảm bảo người đánh giá có mua hàng thật, vẫn tồn tại gian lận tinh vi: **shop đối thủ đặt hàng thật rồi đánh giá 1 sao**. Về mặt kỹ thuật đây là "đã mua hàng" hợp lệ, chỉ con người mới phát hiện được động cơ. Lý do báo cáo này chính là để xử lý tình huống đó.

---

## 9.4. `ReviewHelpfulVote` — Lượt bấm "Hữu ích" cho đánh giá
Bảng: `review_helpful_votes`

> Phục vụ **UC-SOC-03** (nút "Hữu ích (24)" dưới mỗi đánh giá). Bảng này chặn một người bấm nhiều lần; `ProductReview.helpfulCount` là bộ đếm sẵn để sắp xếp.

```java
@Entity
@Table(name = "review_helpful_votes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_review_helpful_vote", columnNames = {"review_id", "user_id"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ReviewHelpfulVote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long reviewId;                    // FK → product_reviews.id (ON DELETE CASCADE)

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — người bấm (không được là tác giả đánh giá)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bấm "Hữu ích" (bấm lần 2 = xóa dòng và trừ helpfulCount) — UC-SOC-03 */
    public static ReviewHelpfulVote of(Long reviewId, Long userId, Long reviewAuthorId, OffsetDateTime now) {
        if (userId.equals(reviewAuthorId)) throw new IllegalArgumentException("Không tự bấm Hữu ích cho đánh giá của mình");
        ReviewHelpfulVote v = new ReviewHelpfulVote();
        v.reviewId = reviewId; v.userId = userId; v.createdAt = now;
        return v;
    }
}
```

---

# 10. NHÓM THÔNG BÁO & NỘI DUNG SÀN

## 10.1. `Notification` — Thông báo cá nhân
Bảng: `notifications`

```java
@Entity
@Table(name = "notifications")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — gửi cho ai

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;            // 15 loại (Phụ lục A): ORDER_STATUS_CHANGED, KYC_APPROVED, PAYMENT_SUCCESS (người mua), NEW_ORDER_FOR_VENDOR (shop)... — sự kiện con ghi trong metadataJson.event

    @Column(nullable = false, length = 255)
    private String title;                     // Tiêu đề thông báo

    @Column(columnDefinition = "TEXT")
    private String body;                      // NULLABLE — Nội dung chi tiết

    @Column(length = 500)
    private String imageUrl;                  // NULLABLE — Ảnh minh hoạ (ảnh sản phẩm, banner)

    @Column(length = 255)
    private String referenceId;               // NULLABLE — ID đối tượng liên quan, để bấm vào là điều hướng tới

    @Column(length = 50)
    private String referenceType;             // NULLABLE — Loại đối tượng: "ORDER", "PRODUCT", "VOUCHER"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadataJson;              // NULLABLE — Dữ liệu bổ sung dạng JSON tuỳ loại thông báo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;      // Kênh phát thông báo MVP: IN_APP (lưu bảng + đẩy WebSocket) / EMAIL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationDeliveryStatus deliveryStatus; // PENDING, SENT, DELIVERED, FAILED (A.14)

    private Integer retryCount;               // NULLABLE — Số lần đã thử gửi lại khi thất bại

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;           // Đã đọc hay chưa — dùng cho badge số đỏ

    private OffsetDateTime sentAt;             // NULLABLE — Thời điểm gửi đi thành công

    private OffsetDateTime readAt;             // NULLABLE — Thời điểm user mở đọc

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo thông báo
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đã gửi qua kênh — UC-NOT-01 */
    public void markSent(OffsetDateTime now) {
        this.deliveryStatus = NotificationDeliveryStatus.SENT;
        this.sentAt = now;
    }

    /** Gửi lỗi — tăng retryCount, @Scheduled thử lại tối đa 3 lần — UC-NOT-03 */
    public void markFailed() {
        this.retryCount += 1;
        this.deliveryStatus = NotificationDeliveryStatus.FAILED;
    }

    /** Người dùng đã đọc — UC-NOT-01 */
    public void markRead(OffsetDateTime now) {
        this.isRead = true;
        this.readAt = now;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Vì sao tách `deliveryStatus` và `isRead` — hai khái niệm hoàn toàn khác nhau?**
- `deliveryStatus` = **hệ thống đã gửi được chưa** (góc nhìn kỹ thuật). `FAILED` nghĩa là email bật lại, push token hết hạn.
- `isRead` = **người dùng đã xem chưa** (góc nhìn người dùng).

Một thông báo có thể `SENT` thành công nhưng `isRead = false` mãi mãi. Hai trục này độc lập và cần cho hai loại báo cáo khác nhau: tỷ lệ gửi thành công (đo chất lượng hạ tầng) và tỷ lệ mở (đo chất lượng nội dung).

**b) `retryCount` — cơ chế thử lại với backoff**
Gửi thông báo qua mạng luôn có thể lỗi tạm thời. Hệ thống thử lại nhưng phải có giới hạn (thường 3–5 lần) với khoảng cách tăng dần (**exponential backoff**: 1 phút → 5 phút → 25 phút). Không có `retryCount`, một thông báo lỗi vĩnh viễn sẽ bị thử lại vô hạn, làm nghẽn hàng đợi.

**c) `referenceId` + `referenceType` — deep link**
Khi khách bấm vào thông báo *"Đơn hàng #1234 đã được giao"*, ứng dụng cần biết mở màn hình nào. Cặp 2 trường này cho phép điều hướng: `referenceType = "ORDER"`, `referenceId = "1234"` → mở `/orders/1234`.

**d) `channel` — cùng nội dung, nhiều đường gửi**
Một sự kiện có thể sinh ra nhiều bản ghi với `channel` khác nhau: vừa hiện trong app (`IN_APP`), vừa gửi email (`EMAIL`), vừa đẩy realtime qua WebSocket. Mỗi kênh có tỷ lệ thành công và độ trễ riêng nên phải theo dõi tách biệt.

**e) `metadataJson` — mở rộng không cần đổi cấu trúc bảng**
Thông báo đơn hàng cần lưu trạng thái mới; thông báo KYC cần lưu lý do từ chối. Thay vì thêm hàng chục cột chỉ dùng cho một loại, dữ liệu riêng được nhét vào JSON. Đánh đổi quen thuộc: linh hoạt nhưng không query được bằng SQL thường.

---

## 10.2. `PlatformNotification` — Thông báo phát toàn sàn
Bảng: `platform_notifications`

```java
@Entity
@Table(name = "platform_notifications")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class PlatformNotification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, length = 255)
    private String title;                     // Tiêu đề thông báo

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;                   // Nội dung

    @Column(length = 500)
    private String imageUrl;                  // NULLABLE — Ảnh banner kèm theo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlatformNotificationTarget target; // Đối tượng nhận: ALL, CUSTOMER, VENDOR, SPECIFIC_USERS

    @Column(nullable = false)
    private Long createdBy;                   // FK mềm → users.id — admin nào đã tạo

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm sửa gần nhất

    @Formula("(SELECT COUNT(*) FROM user_platform_notification_reads r WHERE r.platform_notification_id = id)")
    private Long readCount;                   // CHỈ ĐỌC, tính động (@Formula, thuộc tính dẫn xuất) — số người đã đọc; luôn ≥ 0

    @Builder.Default
    @Column(nullable = false)
    private Long reachCount = 0L;             // Số người ĐÃ NHẬN (tính lúc phát) — mẫu số để tính tỷ lệ đọc

    private OffsetDateTime sendAt;             // NULLABLE — Hẹn giờ phát; NULL = phát ngay (cron "Phát thông báo hẹn giờ") — UC-NOT-05 (TLCN v1.3)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String recipientUserIdsJson;      // NULLABLE — Mảng users.id khi target = SPECIFIC_USERS — UC-NOT-05 (TLCN v1.3)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đếm lượt đọc broadcast — UC-NOT-05 */
    public void incrementRead() {
        this.readCount += 1;
    }
}
```

## 10.3. `UserPlatformNotificationRead` — Đánh dấu đã đọc thông báo sàn
Bảng: `user_platform_notification_reads`

```java
@Entity
@Table(name = "user_platform_notification_reads")
@IdClass(UserPlatformNotificationReadId.class)
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class UserPlatformNotificationRead {

    @Id
    private Long userId;                      // FK → users.id — phần 1 khoá chính gộp

    @Id
    private Long platformNotificationId;      // FK → platform_notifications.id — phần 2 khoá chính gộp

    @Column(nullable = false)
    private OffsetDateTime readAt;             // Thời điểm user đọc
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đánh dấu đã đọc thông báo toàn sàn — UC-NOT-05 */
    public static UserPlatformNotificationRead of(Long userId, Long notificationId, OffsetDateTime now) {
        UserPlatformNotificationRead r = new UserPlatformNotificationRead();
        r.userId = userId; r.platformNotificationId = notificationId; r.readAt = now;
        return r;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Fan-out on Read vs Fan-out on Write — bài toán kinh điển của hệ thống thông báo**
Sàn có 1 triệu người dùng và admin muốn gửi 1 thông báo cho tất cả. Có 2 cách:

| | Fan-out on **Write** | Fan-out on **Read** (dự án dùng) |
|---|---|---|
| Cách làm | Tạo ngay 1 triệu dòng trong `notifications` | Tạo **1 dòng** duy nhất; user đọc thì tự lấy |
| Dung lượng | 1 triệu dòng / thông báo | 1 dòng + số dòng "đã đọc" |
| Tốc độ gửi | Rất chậm, có thể khoá DB | Tức thì |
| Tốc độ đọc | Nhanh (1 query theo user) | Chậm hơn (phải lọc theo target + trạng thái đọc) |

> Dự án dùng **Fan-out on Read** cho thông báo toàn sàn (`PlatformNotification`) và **Fan-out on Write** cho thông báo cá nhân (`Notification`). Đây là lựa chọn đúng: thông báo cá nhân vốn chỉ có 1 người nhận, còn thông báo toàn sàn nếu ghi 1 triệu dòng mỗi lần phát sẽ làm sập database.

**b) `UserPlatformNotificationRead` — lưu trạng thái đọc một cách tiết kiệm**
Thay vì tạo trước 1 triệu dòng "chưa đọc", hệ thống chỉ ghi dòng **khi user thực sự đọc**. Tỷ lệ đọc thực tế thường chỉ 10–30%, nên tiết kiệm được 70–90% dung lượng.
Khoá chính gộp `(userId, platformNotificationId)` đảm bảo mỗi người chỉ đánh dấu đọc 1 lần.

**c) `readCount` dùng `@Formula` — cột tính động, không lưu**
Trong JPA: Sử dụng `@Formula` hoặc `@Transient` để tính toán giá trị qua subquery mỗi khi đọc.
> **Vì sao không lưu thành cột thật?** Vì mỗi lượt đọc mới sẽ phải UPDATE dòng `platform_notifications` → với thông báo phổ biến, hàng nghìn UPDATE đồng thời vào **cùng một dòng** tạo điểm nghẽn nghiêm trọng (hot row contention).

**d) `reachCount` được chốt tại thời điểm phát**
Số người nhận phải chụp lại ngay lúc gửi, vì số lượng user thay đổi liên tục. Nếu tính động, tỷ lệ đọc sẽ giảm dần một cách vô lý khi có user mới đăng ký (họ chưa từng nhận thông báo cũ nhưng lại bị đưa vào mẫu số).

---

## 10.4. `HomeBanner` — Banner trang chủ & chiến dịch tĩnh (CMS)
Bảng: `home_banners`

> Phục vụ **UC-ADM-12**: banner tĩnh do Admin quản lý (slider trang chủ "SIÊU SALE 12.12", banner ngành hàng, banner khu Flash Sale). Khác với `PlatformNotification` (thông báo in-app) — banner chỉ hiển thị, không gửi tới từng người.

```java
@Entity
@Table(name = "home_banners", indexes = {
    @Index(name = "idx_banner_position_time", columnList = "position, is_active, starts_at, ends_at")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class HomeBanner {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, length = 200)
    private String title;                     // Tên nội bộ / alt text: "Siêu Sale 11.11 — Slider 1"

    @Column(nullable = false, length = 500)
    private String imageUrl;                  // Ảnh banner bản desktop (Silo/CDN)

    @Column(length = 500)
    private String mobileImageUrl;            // NULLABLE — Ảnh bản mobile; NULL = dùng imageUrl

    @Column(nullable = false, length = 500)
    private String targetUrl;                 // Đích khi bấm (đường dẫn trong miền sàn): danh mục, sản phẩm, cửa hàng, khu Flash Sale, Gợi ý hôm nay, trang chiến dịch

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BannerPosition position;          // HOME_TOP_CAROUSEL / HOME_TOP_SIDE / HOME_MIDDLE / CATEGORY_TOP / FLASH_SALE_TOP / POPUP

    private Long categoryId;                  // NULLABLE — FK → categories.id; bắt buộc khi position = CATEGORY_TOP

    @Builder.Default
    @Column(nullable = false)
    private Integer sortOrder = 0;            // Thứ tự trong cùng vị trí (số nhỏ hiển thị trước)

    @Column(nullable = false)
    private OffsetDateTime startsAt;           // Bắt đầu hiển thị

    @Column(nullable = false)
    private OffsetDateTime endsAt;             // Kết thúc hiển thị

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;          // Công tắc tắt khẩn cấp (sai giá, sai nội dung) mà không sửa lịch

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String targetAudienceJson;        // NULLABLE — Nhắm đối tượng: {"roles":["CUSTOMER"],"newUserOnly":true}; NULL = mọi người

    @Column(nullable = false)
    private Long createdByAdminId;            // ID admin tạo banner — dữ liệu kiểm toán, KHÔNG đặt FK (như VendorProfile.kycVerifiedBy)

    @Builder.Default
    @Column(nullable = false)
    private Long impressionCount = 0L;        // Lượt hiển thị (flush từ Redis banner:{id}:imp mỗi 5 phút)

    @Builder.Default
    @Column(nullable = false)
    private Long clickCount = 0L;             // Lượt bấm (flush từ Redis banner:{id}:clk)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đang hiển thị (bật và trong khung giờ) — UC-ADM-12 */
    public boolean isLive(OffsetDateTime now) {
        return Boolean.TRUE.equals(this.isActive) && !now.isBefore(this.startsAt) && now.isBefore(this.endsAt);
    }

    /** Tắt khẩn cấp — UC-ADM-12 */
    public void deactivate() {
        this.isActive = false;
    }

    /** Ghi bền bộ đếm từ Redis (GETDEL) — UC-ADM-12 */
    public void recordStats(long impressions, long clicks) {
        this.impressionCount += impressions;
        this.clickCount += clicks;
    }

    /** Tỷ lệ nhấp CTR — UC-ADM-12 */
    public double ctr() {
        return this.impressionCount == 0 ? 0.0 : (double) this.clickCount / this.impressionCount;
    }

    // ===== Bổ sung plan 2026-11-16 banner (B02.02; ENTITY 10.4, Phụ lục E) =====

    /** Tạo banner mới — UC-ADM-12. {@code imageKey} / {@code mobileImageKey} là khóa đối tượng trong {@code content-assets}. */
    public static HomeBanner create(String title, String imageKey, String mobileImageKey, String targetUrl,
                                    BannerPosition position, Long categoryId, int sortOrder, OffsetDateTime startsAt,
                                    OffsetDateTime endsAt, boolean active, long adminId) {
        HomeBanner b = new HomeBanner();
        b.impressionCount = 0L;
        b.clickCount = 0L;
        b.createdByAdminId = adminId;
        b.isActive = active;
        b.changeContent(title, targetUrl, position, categoryId, sortOrder);
        b.changeSchedule(startsAt, endsAt);
        b.changeImages(imageKey, mobileImageKey);
        return b;
    }

    /** Trạng thái hiển thị (D3): tắt → OFF; chưa tới giờ → SCHEDULED; hết giờ → ENDED; còn lại → LIVE — UC-ADM-12 */
    public BannerState state(OffsetDateTime now) {
        if (!Boolean.TRUE.equals(this.isActive)) return BannerState.OFF;
        if (now.isBefore(this.startsAt)) return BannerState.SCHEDULED;
        if (!now.isBefore(this.endsAt)) return BannerState.ENDED;
        return BannerState.LIVE;
    }

    /** Bật lại sau khi tắt khẩn cấp (service kiểm giới hạn 8 banner / vị trí trước) — UC-ADM-12 */
    public void activate() {
        this.isActive = true;
    }

    /** Đổi khung giờ hiển thị; {@code endsAt} phải sau {@code startsAt} — UC-ADM-12 */
    public void changeSchedule(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt))
            throw new IllegalArgumentException("Thời điểm kết thúc phải sau thời điểm bắt đầu");
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /** Đổi nội dung / vị trí; CATEGORY_TOP bắt buộc {@code categoryId}, vị trí khác bỏ {@code categoryId} — UC-ADM-12 */
    public void changeContent(String title, String targetUrl, BannerPosition position, Long categoryId, int sortOrder) {
        if (position == null) throw new IllegalArgumentException("Thiếu vị trí hiển thị");
        if (position == BannerPosition.CATEGORY_TOP && categoryId == null)
            throw new IllegalArgumentException("Banner đầu trang ngành hàng phải chọn danh mục");
        if (sortOrder < 0) throw new IllegalArgumentException("Thứ tự không được âm");
        this.title = title;
        this.targetUrl = targetUrl;
        this.position = position;
        this.categoryId = position == BannerPosition.CATEGORY_TOP ? categoryId : null;
        this.sortOrder = sortOrder;
    }

    /** Đổi ảnh (khóa đối tượng); ảnh desktop bắt buộc, mobile {@code null} = dùng ảnh desktop — UC-ADM-12 */
    public void changeImages(String imageKey, String mobileImageKey) {
        if (imageKey == null || imageKey.isBlank()) throw new IllegalArgumentException("Thiếu ảnh banner");
        this.imageUrl = imageKey;
        this.mobileImageUrl = mobileImageKey == null || mobileImageKey.isBlank() ? null : mobileImageKey;
    }

    /** Chỉ xóa được banner đã tắt hoặc đã hết giờ — UC-ADM-12 */
    public boolean isDeletable(OffsetDateTime now) {
        BannerState s = state(now);
        return s == BannerState.OFF || s == BannerState.ENDED;
    }
}
```

### 💡 Kiến thức chuyên sâu
- **Đọc nhanh:** trang chủ chịu tải lớn nhất, nên danh sách banner đang hiển thị theo `position` được cache Redis `banner:live:{position}` (TTL 60 giây, xóa khi Admin sửa). Truy vấn gốc dùng index `(position, is_active, starts_at, ends_at)`.
- **Bộ đếm không ghi thẳng DB:** mỗi lượt xem trang chủ là một impression — ghi trực tiếp sẽ tạo hàng chục nghìn UPDATE/giây. `INCR` trên Redis rồi flush định kỳ qua `recordStats()`.
- **Kiểm tra cấu hình:** `endsAt > startsAt`, `position = CATEGORY_TOP` thì phải có `categoryId` — nằm trong entity (`changeSchedule`, `changeContent`); `imageUrl` / `mobileImageUrl` lưu **khóa đối tượng** (`banner/{id}/desktop-{uuid}.jpg`), API ghép URL. Service kiểm thêm: tối đa 8 banner bật có lịch giao nhau mỗi vị trí (`GNE-BNR-4221`), `targetUrl` chỉ đường dẫn trong sàn (`GNE-BNR-4222`), chỉ xóa banner `OFF` / `ENDED` (`isDeletable`, `GNE-BNR-4091`). Mọi thay đổi ghi `system_logs` `BANNER_CHANGED`.

---

## 10.5. `AdminNotification` — Thông báo nội bộ cho quản trị viên
Bảng: `admin_notifications`

> Chuông thông báo của Admin Console (hộp thư chung của đội vận hành): đơn mới, hồ sơ KYC / sản phẩm chờ duyệt, yêu cầu rút tiền, cảnh báo hệ thống, lệch số dư ví / sổ cái. Bảng này **đang được dùng trong mã nguồn cũ** (`server/internal/app/domain/admin_notification.go`, API `GET /admin/notifications`, `/admin/notifications/unread-count`) — đưa vào TLCN v1.1 để mô hình khớp hiện trạng. Mở rộng ở DATN: `FRAUD_ALERT`, `KYC_BACKLOG`, `SLA_OVERDUE`, `COD_OVERDUE`.

```java
@Entity
@Table(name = "admin_notifications")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class AdminNotification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminNotificationType type;       // SYSTEM_ALERT / NEW_ORDER / USER_APPROVAL / WITHDRAW_REQUEST / ESCROW_DISCREPANCY (5 loại TLCN)

    @Column(nullable = false, length = 255)
    private String title;                     // Tiêu đề

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;                   // Nội dung chi tiết

    @Column(length = 500)
    private String referenceUrl;              // NULLABLE — Link tới trang xử lý trong admin panel

    @Column(length = 500)
    private String imageUrl;                  // NULLABLE — Ảnh minh hoạ

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;           // Đã xử lý/đã xem chưa

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm phát sinh

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Nhân viên / Admin đã xem cảnh báo — UC-NOT-01 */
    public void markRead() {
        this.isRead = true;
    }
}
```

### 💡 Kiến thức chuyên sâu

**Vì sao không dùng chung bảng `Notification`?**
Vì đây thực chất là **hàng đợi công việc (work queue)** chứ không phải thông báo thông thường:
- **Không có `userId`:** thông báo thuộc về *"đội ngũ admin"* nói chung, ai xử lý trước cũng được. Đây là mô hình **shared inbox**.
- **`referenceUrl` thay cho cặp `referenceId`/`referenceType`:** admin panel là web nội bộ, dùng URL trực tiếp đơn giản hơn nhiều so với cơ chế deep-link của ứng dụng khách hàng.
- **Vòng đời khác:** thông báo khách hàng chỉ cần "đọc", thông báo admin cần "được xử lý xong".

Việc tách bảng thể hiện đúng nguyên tắc: **hai thứ trông giống nhau nhưng có vòng đời và người dùng khác nhau thì nên là hai mô hình khác nhau.**

---

# 11. NHÓM NHẮN TIN (CHAT)

## 11.1. `Conversation` — Cuộc hội thoại
Bảng: `conversations`

```java
@Entity
@Table(name = "conversations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "vendor_id"}))
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Conversation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long customerId;                  // FK → users.id — khách hàng (UNIQUE ghép với vendorId)

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop

    @Column(length = 255)
    private String lastMessageSnippet;        // NULLABLE — Trích đoạn tin nhắn cuối, hiển thị ở danh sách chat

    private OffsetDateTime lastMessageAt;      // NULLABLE — Thời điểm tin cuối — dùng để SẮP XẾP danh sách

    @Column(name = "customer_unread_count", nullable = false)
    private Integer customerUnread;           // Số tin khách chưa đọc — badge đỏ phía khách

    @Column(name = "vendor_unread_count", nullable = false)
    private Integer vendorUnread;             // Số tin shop chưa đọc — badge đỏ phía shop

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm mở hội thoại

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "conversation_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua Message.conversationId
    private List<Message> messages;           // Các tin nhắn trong hội thoại
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cập nhật tóm tắt & bộ đếm chưa đọc — UC-SOC-01 */
    public void onNewMessage(Message m, boolean fromCustomer) {
        this.lastMessageSnippet = m.getContent() == null ? "[" + m.getType() + "]" : m.getContent().substring(0, Math.min(100, m.getContent().length()));
        this.lastMessageAt = m.getCreatedAt();
        if (fromCustomer) this.vendorUnread += 1; else this.customerUnread += 1;
    }

    /** Đặt lại badge chưa đọc — UC-SOC-01 */
    public void markReadBy(boolean customer) {
        if (customer) this.customerUnread = 0; else this.vendorUnread = 0;
    }
}
```

---

## 11.2. `Message` — Tin nhắn
Bảng: `messages`

```java
public enum MessageType {
    TEXT,           // Tin nhắn văn bản
    IMAGE           // Tin nhắn đính kèm hình ảnh (MVP; thẻ sản phẩm / thẻ đơn hàng để bản mở rộng)
}

@Entity
@Table(name = "messages")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Message {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long conversationId;              // FK → conversations.id — thuộc hội thoại nào

    @Column(nullable = false)
    private Long senderId;                    // FK → users.id — ai gửi (khách hoặc shop)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;                 // TEXT (chữ) / IMAGE (ảnh)

    @Column(columnDefinition = "TEXT")
    private String content;                   // NULLABLE — Nội dung chữ (khi type = TEXT)

    @Column(length = 500)
    private String attachmentUrl;             // NULLABLE — Link ảnh đính kèm (khi type = IMAGE)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;           // Người nhận đã đọc tin này chưa

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gửi
    private Long productVariantId;            // NULLABLE — FK → product_variants.id (khi type = PRODUCT_CARD — UC-SOC-02)
    private Long orderId;                     // NULLABLE — FK → orders.id (khi type = ORDER_CARD — UC-SOC-02, TLCN v1.3)

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tin đã đọc — UC-SOC-01 */
    public void markRead() {
        this.isRead = true;
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) UNIQUE `(customerId, vendorId)` — mô hình hội thoại 1-1 vĩnh viễn**
Giữa 1 khách và 1 shop chỉ tồn tại **đúng một** cuộc hội thoại, kéo dài mãi mãi. Khác hẳn mô hình "ticket hỗ trợ" (mỗi vấn đề mở 1 ticket mới rồi đóng).
> Ưu điểm: shop thấy được toàn bộ lịch sử trao đổi với khách — biết khách này từng mua gì, từng khiếu nại gì. Đây là lựa chọn đúng cho TMĐT vì quan hệ mua bán là lâu dài.
> Hệ quả kỹ thuật: bảng `messages` của một hội thoại lâu năm có thể rất lớn, nên phải phân trang (thường dùng **cursor pagination** theo `id` hoặc `createdAt`, không dùng `OFFSET` vì OFFSET lớn rất chậm).

**b) Hai bộ đếm chưa đọc riêng biệt — vì sao không dùng 1 cột?**
Trong hội thoại 2 chiều, mỗi bên có số tin chưa đọc **khác nhau**:

```
Khách gửi 3 tin  → vendorUnread = 3,  customerUnread = 0
Shop đọc         → vendorUnread = 0
Shop trả lời 1 tin → customerUnread = 1
```

Nếu chỉ 1 cột, badge đỏ sẽ hiện sai cho ít nhất một bên. Đây là **counter cache** — tránh phải chạy `SELECT COUNT(*) FROM messages WHERE is_read = false` mỗi lần mở danh sách chat (với 100 hội thoại là 100 lần đếm).

**c) `lastMessageSnippet` + `lastMessageAt` — phi chuẩn hoá cho màn hình danh sách**
Màn hình danh sách chat cần hiển thị 50 hội thoại, mỗi cái kèm tin nhắn cuối. Nếu không lưu sẵn, phải chạy 50 subquery kiểu `ORDER BY created_at DESC LIMIT 1` — cực kỳ tốn kém.
Sao chép sẵn 2 trường này cho phép hiển thị toàn bộ danh sách bằng **1 query duy nhất**, sắp xếp trực tiếp theo `lastMessageAt`. Đây là kỹ thuật bắt buộc trong mọi ứng dụng chat.

**d) `Message` đa hình qua `type` — một bảng chứa nhiều loại nội dung**
Tuỳ `type` mà trường nào được dùng:

| type | content | attachmentUrl |
|---|---|---|
| `TEXT` | ✅ có | — |
| `IMAGE` | (caption, tuỳ chọn) | ✅ có |

> Đây là **Single Table Inheritance**. Đánh đổi: nhiều cột NULL. Lợi ích: mọi tin nhắn nằm trong 1 bảng nên sắp xếp theo thời gian cực đơn giản — nếu tách 3 bảng, việc trộn 3 dòng thời gian lại theo đúng thứ tự sẽ rất phức tạp và chậm.

---

# 12. NHÓM NHẬT KÝ HỆ THỐNG

## 12.1. `SystemLog` — Nhật ký kiểm toán
Bảng: `system_logs`

```java
@Entity
@Table(name = "system_logs")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class SystemLog {

    @Id
    @Column(length = 36)
    private String id;                        // Khoá chính kiểu UUID (36 ký tự), KHÔNG tự tăng

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm xảy ra hành động

    private Long userId;                      // NULLABLE — Ai thực hiện. NULL = hành động của hệ thống

    @Column(nullable = false, length = 255)
    private String userEmail;                 // Email người thực hiện — CHỤP LẠI, không JOIN

    @Column(nullable = false, length = 50)
    private String userRole;                  // Vai trò lúc thực hiện — chụp lại

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AuditAction actionType;           // Loại hành động: LOGIN_SUCCESS, KYC_APPROVED, PAYOUT_APPROVED...

    @Column(length = 100)
    private String referenceId;               // NULLABLE — ID đối tượng bị tác động

    @Column(columnDefinition = "TEXT")
    private String details;                   // NULLABLE — Chi tiết dạng JSON: giá trị trước/sau khi sửa

    @Column(length = 45)
    private String ipAddress;                 // NULLABLE — Địa chỉ IP thực hiện hành động (NULL với tác vụ hệ thống / Cron)

    @Column(nullable = false, length = 20)
    private String status;                    // Kết quả: "SUCCESS" / "FAILED"

    @Column(length = 500)
    private String userAgent;                 // NULLABLE — Trình duyệt/thiết bị thực hiện

    // Sinh UUID và mốc thời gian trước khi lưu thông qua hook @PrePersist của JPA (createdAt còn có @CreationTimestamp — Quy ước 12)
    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.createdAt == null) this.createdAt = OffsetDateTime.now();
    }
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo bản ghi kiểm toán bất biến — UC-ADM-03 */
    public static SystemLog of(Long userId, String userEmail, String userRole, AuditAction action, String referenceId, String details, String ip, boolean success) {
        SystemLog l = new SystemLog();
        l.userId = userId; l.userEmail = userEmail; l.userRole = userRole;   // NOT NULL — chụp lại lúc thực hiện ("system@genie.vn" / "SYSTEM" với job nền)
        l.actionType = action; l.referenceId = referenceId; l.details = details; l.ipAddress = ip;
        l.status = success ? "SUCCESS" : "FAILED";
        return l;   // id & createdAt gán ở prePersist() — không vi phạm NOT NULL dù factory không truyền thời gian
    }
}
```

### 💡 Kiến thức chuyên sâu

**a) Vì sao khoá chính là UUID chứ không phải số tự tăng?**
Đây là bảng có khối lượng ghi lớn nhất hệ thống. Ba lý do dùng UUID:
1. **Ghi phân tán:** nhiều server (hoặc nhiều shard) có thể sinh ID đồng thời mà không cần hỏi database, không có điểm nghẽn ở bộ đếm trung tâm.
2. **Không lộ thông tin kinh doanh:** ID tự tăng cho phép suy ra khối lượng giao dịch (`id = 1.000.000` nghĩa là đã có 1 triệu bản ghi). Log là dữ liệu nhạy cảm.
3. **Gộp log dễ dàng:** khi cần trộn log từ nhiều nguồn, UUID không bao giờ đụng nhau.

> Đánh đổi: UUID chiếm 36 byte (so với 8 byte của BIGINT) và ngẫu nhiên nên làm phân mảnh index. Với bảng chỉ ghi-và-đọc-theo-thời-gian thì đánh đổi này chấp nhận được. Nếu cần tối ưu, có thể dùng **UUID v7** (có thứ tự theo thời gian) để khắc phục phân mảnh.

**b) `userEmail` và `userRole` được CHỤP LẠI, không JOIN — nguyên tắc cốt lõi của kiểm toán**
Đây là điểm quan trọng nhất của bảng này. Vì sao không JOIN sang `users` để lấy email?

```
Ngày 1:  staff@company.com (role = STAFF) duyệt một hồ sơ KYC
Ngày 30: Người này nghỉ việc, tài khoản bị xoá mềm (DELETED), email đổi thành deleted_99@genie.local
Ngày 60: Kiểm toán hỏi "Ai duyệt hồ sơ ngày 1?"
   → Nếu JOIN: trả về "deleted_99@genie.local" — VÔ NGHĨA ❌
   → Nếu chụp lại: trả về "staff@company.com, role STAFF" — CHÍNH XÁC ✅
```

Log phải ghi lại **sự thật tại thời điểm xảy ra**, không phải sự thật hiện tại. Tương tự, `userRole` cũng phải chụp lại: người đó có thể được thăng từ STAFF lên ADMIN sau này, nhưng lúc thực hiện hành động họ chỉ là STAFF.

**c) `userId` nullable — hành động của hệ thống**
Job tự động giải ngân escrow, job hết hạn giữ chỗ tồn kho... không có con người nào thực hiện. `userId = NULL` với `userEmail = "system@internal"` thể hiện điều này một cách tường minh.

**d) `details` lưu giá trị trước/sau — Change Data Capture ở cấp nghiệp vụ**

```json
{
  "field": "commission_rate",
  "old_value": "0.0500",
  "new_value": "0.0800",
  "vendor_id": 42
}
```

Không có thông tin này, log chỉ nói "ai đó đã sửa hoa hồng" mà không biết sửa từ bao nhiêu thành bao nhiêu — vô dụng khi cần khôi phục hoặc điều tra.

**e) `status` ghi cả hành động THẤT BẠI**
Ghi cả `FAILED` mới là điểm mấu chốt về bảo mật. Chuỗi 50 dòng `LOGIN_FAILED` từ cùng 1 IP trong 1 phút là dấu hiệu tấn công brute-force. Nếu chỉ ghi thành công, hệ thống **mù hoàn toàn** trước các cuộc tấn công đang diễn ra.

**f) Chỉ mục kép `(createdAt, actionType)`**
Truy vấn kiểm toán điển hình: *"Tất cả hành động PAYOUT_APPROVED trong tháng 9"*. Index ghép theo đúng thứ tự này phục vụ trực tiếp mẫu truy vấn đó.

**g) Danh sách `AuditAction` phản ánh đúng các điểm rủi ro cao**
Nhìn vào danh sách enum có thể thấy ngay hệ thống coi cái gì là nhạy cảm: đăng nhập, khoá tài khoản, duyệt KYC, duyệt sản phẩm, giải ngân, duyệt rút tiền, hoàn tiền, đổi hoa hồng. Đó chính xác là những hành động mà kiểm toán viên sẽ hỏi tới. Khi hiện thực AUTH (V14) bổ sung 8 hành động: đổi / đặt lại mật khẩu, đổi email, liên kết / gỡ Google, thu hồi phiên, yêu cầu / hủy xóa tài khoản — tổng **30** giá trị. Lần đăng nhập sai và truy cập bị chặn được ghi bằng transaction riêng (`REQUIRES_NEW`) để không mất khi transaction chính rollback.

---

# 13. TIÊU CHUẨN VẬT LÝ POSTGRESQL 16 (MỨC MVP)

## 13.1. Index cho các đường dẫn nóng (Hot Paths)

| Bảng | Index | Phục vụ |
|---|---|---|
| `products` | `GIN (search_vector)` — cột `tsvector` sinh từ `name`, `brand`, `description` (qua `unaccent`) | UC-CAT-02 tìm kiếm theo từ khóa |
| `products` | `GIN (name gin_trgm_ops)` | Gợi ý khi gõ sai nhẹ (`similarity()`) |
| `products` | `(category_id, status, created_at DESC)` | UC-CAT-01 / UC-CAT-05 duyệt & lọc |
| `products` | `(vendor_id, status)` | UC-CAT-16 trang shop, kênh người bán |
| `product_variants` | `UNIQUE (sku)` | Chống trùng SKU |
| `orders` | `(customer_id, created_at DESC)`, `(vendor_id, status, created_at DESC)` | UC-ORD-09 lịch sử đơn, kênh người bán |
| `orders` | `(status, delivered_at)` partial `WHERE status = 'DELIVERED'` | UC-ORD-12 cron tự hoàn tất |
| `stock_reservations` | `(status, expires_at)` partial `WHERE status = 'ACTIVE'` | UC-ORD-08 sweeper hủy đơn quá hạn |
| `escrow_ledgers` | `(status, release_eligible_at)` partial `WHERE status = 'HELD'` | UC-PAY-07 cron giải ngân |
| `payment_transactions` | `UNIQUE (idempotency_key)`, `UNIQUE (gateway_transaction_id)` | UC-PAY-03 IPN VNPay / webhook SePay idempotent |
| `notifications` | `(user_id, is_read, created_at DESC)` | UC-NOT-01 hộp thông báo |
| `messages` | `(conversation_id, created_at DESC)` | UC-SOC-01 tải lịch sử chat |
| `product_reviews` | `(vendor_id, created_at DESC)` + biểu thức `((aspect_sentiment_json ->> 'overall'))` | UC-AI-07 / UC-VND-08 tỉ lệ cảm xúc theo shop, UC-SOC-05 ưu tiên đánh giá tiêu cực |
| `system_logs` | `(created_at, action_type)` | UC-ADM-03 tra cứu kiểm toán |
| `users` | `uk_users_email_lower` UNIQUE `(lower(email))` *(V14)* | UC-AUTH-01/02 email không phân biệt hoa thường |
| `users` | `idx_users_deletion_requested_at (deletion_requested_at)` partial `WHERE deletion_requested_at IS NOT NULL` *(V14)* | UC-AUTH-10 cron ẩn danh |
| `refresh_tokens` | `idx_refresh_tokens_user_active (user_id)` partial `WHERE revoked_at IS NULL`, `idx_refresh_tokens_family (family_id)`, `idx_refresh_tokens_user_created (user_id, created_at DESC)` *(V14)* | UC-AUTH-02 làm mới / phát hiện thiết bị lạ, UC-AUTH-11 danh sách thiết bị |
| `addresses` | `idx_addresses_user (user_id)` partial `WHERE is_deleted = FALSE` *(V14)* | UC-AUTH-09 sổ địa chỉ |
| `vendor_profiles` | `uk_vendor_profiles_shop_name_lower` UNIQUE `(lower(shop_name))` *(V20)* | UC-VND-01 / UC-VND-21 tên shop không trùng (không phân biệt hoa thường) |
| `vendor_profiles` | `idx_vendor_profiles_kyc_status (kyc_status, kyc_submitted_at)` *(V20)* | UC-VND-04 hàng chờ KYC theo trạng thái |
| `vendor_profiles` | `idx_vendor_profiles_supplement_deadline (kyc_supplement_deadline_at)` partial `WHERE kyc_status = 'SUPPLEMENT_REQUESTED'` *(V20)* | UC-VND-04 job hết hạn bổ sung hồ sơ |

> PostgreSQL **không** tự tạo index cho cột khóa ngoại — 5 index *(V14)* ở trên được thêm khi hiện thực AUTH (plan AUTH D21, D22; ADR 0007 trong repo).

## 13.2. Quy ước migration & dữ liệu

- **Flyway** quản lý schema (đã có thật trong repo): `V1__identity.sql`, `V2__catalog.sql`, `V3__cart.sql`, `V4__promotion.sql`, `V5__order.sql`, `V6__payment.sql`, `V7__shipping.sql`, `V8__refund.sql`, `V9__review.sql`, `V10__notification.sql`, `V11__chat.sql`, `V12__audit.sql` (mỗi nhóm thực thể một file), `V13__bootstrap_admin.sql` (Admin đầu tiên từ placeholder `bootstrap_admin_email` / `bootstrap_admin_password_hash`, bỏ qua khi rỗng), `V14__auth_additions.sql` (bổ sung của AUTH), `V15__banner_audit_action.sql`, `V16__coin_expired_type.sql`, `V20__vendor_kyc_additions.sql` (Mở shop & KYC: 9 cột `vendor_profiles`, 3 index, 4 `AuditAction`). **Không sửa migration đã merge** — thay đổi bằng `V15+`; `ddl-auto=validate` + `SchemaValidationIT` chặn lệch giữa entity và schema.
- **Không phân vùng bảng** (partitioning) trong MVP — dữ liệu dưới vài triệu dòng; bản đầy đủ phân vùng `system_logs`, `notifications` theo tháng khi cần.
- **Seed dữ liệu demo** — một công tắc `GENIE_SEED_DEMO` (`genie.seed.demo-data`; `true` ở profile `local`, `false` ở `prod` / `test`): `R__seed_demo.sql` (tài khoản: `mai.tran@gmail.com` / `Genie@123`, 1 Admin, 3 nhân viên với quyền mặc định theo chức danh) + thư mục **`db/seed/`** (Flyway location thứ hai do `SeedDataFlywayCustomizer` nối vào) gồm `R__seed_demo_10_categories` (23 danh mục, hoa hồng theo ngành), `20_brands_attributes`, `30_shops` (6 shop `VERIFIED` + ví + `vendor_shipping_config` + kho lấy hàng), `40_products` (20 sản phẩm + SKU + ảnh), `50_vouchers` (sàn / shop / freeship + quà đổi Điểm `COIN500/1000/2000`), `60_banners`, `70_customer_activity` (Điểm, điểm danh, yêu thích, theo dõi shop), `80_kyc_queue` (5 hồ sơ KYC), `90_bulk_products` (~180 sản phẩm sinh tự động). Quy tắc: SQL thuần (không placeholder), chạy lại được (`ON CONFLICT` / `NOT EXISTS`), không ghi cứng `id`, ảnh lưu **khóa đối tượng** `seed/…`; `balance_hash` tính bằng HMAC-SHA256 khóa máy dev (`coin:<user_id>:<balance>`, `wallet:<vendor_id>:<balance>:<pending>`). Nạp tay vào CSDL local: `scripts/seed/seed.sh` / `seed.ps1` (từ chối host không phải local — production RDS không bao giờ seed); ảnh mẫu: `scripts/seed/upload-assets.*`. `SeedDataIT` kiểm đủ và chạy lại được. Băm Argon2id cho seed sinh bằng `scripts/hash-password.py`.
- **Sao lưu:** `pg_dump` hằng ngày bằng cron trên máy chủ, giữ 7 bản.

## 13.3. Cấu hình PostgreSQL tối thiểu (VPS 4 vCPU / 8 GB RAM chạy chung toàn bộ Docker Compose — Postgres được cấp ~2 GB)

```ini
shared_buffers = 512MB            # ~25% phần RAM cấp cho Postgres
effective_cache_size = 1536MB
work_mem = 8MB
maintenance_work_mem = 128MB
max_connections = 100             # HikariCP mỗi instance Spring Boot: maximum-pool-size = 20
random_page_cost = 1.1            # SSD
```

> Không cần PgBouncer, read-replica hay Patroni ở quy mô MVP; các thành phần này nằm trong lộ trình mở rộng (`DATN/implement.md` Phần 17).

---

# PHỤ LỤC A — TỔNG HỢP ENUM (BẢN TLCN)

> Tổng hợp **55 enum** dùng trong TLCN — danh mục gốc duy nhất: mọi giá trị enum nhắc tới ở thân tài liệu, Usecase.md và Document_Vi.md phải có ở đây.
> Lưu dạng `VARCHAR` (`@Enumerated(EnumType.STRING)`) kèm ràng buộc `CHECK`. **Giá trị bị cắt so với bản đầy đủ** được ghi ở comment `// mở rộng:` — khi mở rộng chỉ **thêm** giá trị (không đổi tên, không đổi thứ tự lưu), dữ liệu cũ vẫn hợp lệ.

```java
// ==========================================
// NHÓM 01. NGƯỜI DÙNG & ĐỊNH DANH (9 enum)
// ==========================================
enum UserRole { CUSTOMER, VENDOR, STAFF, ADMIN }                                      // ← User.role (mở rộng: VENDOR_STAFF, SHIPPER, B2B_BUYER)
enum AccountStatus { UNVERIFIED, ACTIVE, LOCKED, SUSPENDED, ANONYMIZED, DELETED }      // ← User.status (ANONYMIZED: xóa tài khoản — UC-AUTH-10; LOCKED để dành — khóa tạm dùng lockedUntil; UNVERIFIED chỉ cho nhân viên được mời)
enum Gender { MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY }                                  // ← CustomerProfile.gender
enum AddressLabel { HOME, OFFICE, SCHOOL, APARTMENT, OTHER }                            // ← Address.label (TLCN v1.3; + SCHOOL, APARTMENT, OTHER — 2026-11-18, V15)
enum BusinessType { INDIVIDUAL, HOUSEHOLD, BUSINESS }                                   // ← VendorProfile.businessType
enum KycStatus { NOT_SUBMITTED, PENDING, AI_PROCESSING, MANUAL_REVIEW, VERIFIED, REJECTED, SUPPLEMENT_REQUESTED } // ← VendorProfile.kycStatus
enum KycVerificationMethod { OCR_AUTO, MANUAL_STAFF, BIOMETRIC_FACIAL }                 // ← VendorProfile.kycVerificationMethod
enum KycOverallResult { PASS, PARTIAL, FAIL }                                           // ← KycResult.overallStatus
enum StaffPermissionEnum {                                                              // ← StaffPermission.permission (13 quyền TLCN; mở rộng: SUPPORT_CUSTOMER, VIEW_SYSTEM_LOG, MANAGE_FEATURE_FLAGS, RUN_EXPERIMENTS, MANAGE_B2B_CREDIT, MANAGE_DISPATCH, MANAGE_SHIPPERS, REVIEW_FRAUD, HANDLE_DSAR, HANDLE_TAKEDOWN)
    VIEW_DASHBOARD, REVIEW_KYC, MANAGE_CATALOG, MODERATE_REVIEW, HANDLE_DISPUTE, PROCESS_PAYOUT,
    UPDATE_COMMISSION, VIEW_FINANCE, MANAGE_VOUCHER, MANAGE_PLATFORM_NOTIFICATION, VIEW_AUDIT_LOG, MANAGE_USER, MANAGE_RBAC
}
enum StaffTitle {                                                                       // ← AdminProfile.staffTitle (3 chức danh, mỗi chức danh mang bộ quyền mặc định; mở rộng: KYC_OFFICER, MARKETING_OFFICER, DISPATCHER, RISK_ANALYST, COMPLIANCE_OFFICER, B2B_CREDIT_OFFICER, TECH_OPS)
    MODERATOR(VIEW_DASHBOARD, REVIEW_KYC, MANAGE_CATALOG, MODERATE_REVIEW),
    CS_SPECIALIST(VIEW_DASHBOARD, HANDLE_DISPUTE, MODERATE_REVIEW),
    FINANCE_OFFICER(VIEW_DASHBOARD, PROCESS_PAYOUT, UPDATE_COMMISSION, VIEW_FINANCE, MANAGE_VOUCHER);
    private final Set<StaffPermissionEnum> defaults;
    StaffTitle(StaffPermissionEnum... p) { this.defaults = Set.of(p); }
    public Set<StaffPermissionEnum> defaultPermissions() { return defaults; }   // MANAGE_RBAC không thuộc mẫu nào
}

// ==========================================
// NHÓM 02–03. DANH MỤC, SẢN PHẨM & GIỎ HÀNG (8 enum)
// ==========================================
enum ProductStatus {                                                                    // ← Product.status (FLAGGED: kiểm duyệt tự động đánh dấu nghi vấn — UC-CAT-12)
    DRAFT, PENDING_APPROVAL, PUBLISHED, HIDDEN, REJECTED, FLAGGED, DELETED
}
enum ShippingPayer { BUYER, SELLER }                                                    // ← Product.shippingPayer
enum AttributeDataType { TEXT, NUMBER, BOOLEAN, ENUM, MULTI_ENUM, DATE }                // ← CategoryAttribute.dataType
enum ProductCondition { NEW, USED, REFURBISHED }                                   // ← Product.itemCondition (tình trạng hàng)
enum ProductDocumentType { BUSINESS_LICENSE, QUALITY_CERTIFICATE, IMPORT_DOCUMENT, BRAND_AUTHORIZATION, OTHER } // ← ProductDocument.documentType
enum CategoryProposalStatus { PENDING, APPROVED, REJECTED }                         // ← CategoryProposal.status
enum ModerationDecision { APPROVE, REJECT, NEED_REVIEW }                            // ← ProductModerationResult.decision
enum TrafficSource { SEARCH, HOME_FEED, PROMOTION, SHOP_PAGE, EXTERNAL_LINK }       // ← ProductViewStat.source

// ==========================================
// NHÓM 04. VOUCHER 3 TẦNG, FLASH SALE & ĐIỂM (6 enum)
// ==========================================
enum VoucherScope { ALL, CATEGORY, PRODUCT }                                            // ← Voucher.scope (CATEGORY / PRODUCT dùng 4 bảng nối mục 4.4)
enum VoucherType { SHOP, PLATFORM, FREESHIP }                                           // ← Voucher.type, OrderDiscount.voucherType (3 tầng; mở rộng: PAYMENT — tầng 4)
enum DiscountType { PERCENTAGE, FIXED_AMOUNT, FREESHIP }                                // ← Voucher.discountType
enum FundingParty { PLATFORM, VENDOR, CO_SPONSOR }                                      // ← Voucher.sponsorType, OrderDiscount.fundedBy (CO_SPONSOR: sàn + shop cùng chịu theo Voucher.platformSharePercent)
enum FlashSaleStatus { DRAFT, SCHEDULED, ACTIVE, ENDED }                                // ← FlashSaleCampaign.status
enum FlashSaleItemStatus { PENDING, APPROVED, REJECTED }                                 // ← FlashSaleItem.status (TLCN v1.3)
enum CoinTransactionType { CHECK_IN, ORDER_CASHBACK, REVIEW_BONUS, USED_AT_CHECKOUT, ORDER_REFUND, VOUCHER_REDEEM, EXPIRED } // ← CoinTransaction.type (EXPIRED: dòng âm trừ phần còn lại của lô hết hạn sau 12 tháng, reference_id = id lô — V19, plan 2026-11-16 Điểm Genie D5; mở rộng: COSMETIC_REDEEM, VENDOR_REWARD)

// ==========================================
// NHÓM 05. ĐƠN HÀNG, PHIÊN & GIỮ CHỖ KHO (4 enum)
// ==========================================
enum SessionStatus { INITIATED, PAYMENT_PENDING, COMPLETED, FAILED, EXPIRED }           // ← CheckoutSession.status
enum OrderStatus {                                                                      // ← Order.status (19 trạng thái — giữ đủ vì GHN cũng có giao thất bại / hoàn hàng)
    PENDING_PAYMENT,   // Online: chờ thanh toán | COD: chờ shop xác nhận (đã có đơn)
    PAID,              // Online: đã TT | COD: sàn đã nhận tiền thu hộ từ GHN (fund escrow HELD)
    CONFIRMED, PREPARING, READY_FOR_PICKUP,
    DELIVERING, DELIVERED, DELIVERY_FAILED, RETURNING, RETURNED_TO_VENDOR,
    COMPLETED, RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED,
    PARTIALLY_REFUNDED, // đã hoàn một phần, phần còn lại vẫn giao dịch bình thường
    REFUNDED, CANCELLED, CANCELLED_BY_VENDOR, AUTO_CANCELLED
}
enum FundingStatus { UNFUNDED, PENDING, FUNDED, FAILED }                                // ← Order.fundingStatus
enum StockReservationStatus { ACTIVE, CONFIRMED, EXPIRED, RELEASED }                    // ← StockReservation.status

// ==========================================
// NHÓM 06. THANH TOÁN, KÝ QUỸ ESCROW, VÍ SHOP & SỔ CÁI (9 enum)
// ==========================================
enum PaymentMethod { VNPAY, VIETQR, COD }                                               // ← PaymentTransaction.paymentMethod, Order.paymentMethod (VIETQR = chuyển khoản, xác nhận bằng webhook SePay; mở rộng: MOMO, ZALOPAY, INTERNAL_WALLET, BNPL)
enum PaymentStatus {                                                                    // ← PaymentTransaction.status, Order.paymentStatus
    PENDING, REQUIRES_ACTION, SUCCESS, FAILED, EXPIRED, REFUNDED, PARTIALLY_REFUNDED, CANCELLED
}
enum PaymentRefundStatus { PENDING, SUCCESS, FAILED }                                   // ← PaymentRefund.status
enum FundingSource { ONLINE_GATEWAY, COD_CASH }                                         // ← EscrowLedger.fundingSource (ONLINE_GATEWAY gồm VNPay và VietQR qua SePay; mở rộng: WALLET, SPLIT)
enum EscrowStatus {                                                                     // ← EscrowLedger.status
    PENDING_FUNDING,  // Đã tạo record nhưng chưa nhận đủ tiền (COD: GHN chưa đối soát)
    HELD,             // Đã fund đầy đủ, đang giữ an toàn
    FROZEN,           // Admin chủ động đóng băng (nghi vấn gian lận, vi phạm sàn)
    RELEASED,         // Đã giải ngân cho vendor
    REFUNDED,         // Đã hoàn tiền cho khách
    DISPUTED          // Tự động khóa do phát sinh yêu cầu đổi trả / khiếu nại (RefundRequest)
}
enum WalletTransactionType {                                                            // ← WalletTransaction.type (mở rộng: CREDIT_TOPUP, DEBIT_PURCHASE, DEBIT_AD_SPEND, DEBIT_SUBSCRIPTION_FEE, DEBIT_TAX_WITHHOLDING, DEBIT_COSMETIC_PURCHASE)
    CREDIT_ESCROW_RELEASE, DEBIT_REFUND, DEBIT_WITHDRAWAL, CREDIT_ADJUSTMENT, DEBIT_ADJUSTMENT
}
enum WalletReferenceType { ORDER, WITHDRAWAL, REFUND, ADJUSTMENT }                      // ← WalletTransaction.referenceType (mở rộng: SUBSCRIPTION, AD_CAMPAIGN, COSMETIC, TAX)
enum WithdrawalStatus { PENDING, APPROVED, PROCESSING, COMPLETED, REJECTED }            // ← WithdrawalRequest.status
enum CodRemittanceStatus { WAITING, DONE }                                               // ← CodRemittance.status (TLCN v1.3)
enum FinancialEventType {                                                               // ← FinancialLedger.eventType (10 giá trị TLCN; mở rộng: B2B_WHOLESALE_DISBURSEMENT, TAX_WITHHOLDING, LOGISTICS_FEE, SUBSCRIPTION_FEE, AD_REVENUE, COSMETIC_REVENUE, BNPL_MERCHANT_FEE, COSMETIC_DEFERRED, SHIPPER_COD_REMIT, SHIPPER_DEPOSIT_DEDUCTION, AFFILIATE_COMMISSION)
    ESCROW_IN, COMMISSION, PLATFORM_VOUCHER, FREESHIP_COST, COIN_COST, ESCROW_RELEASE,
    PAYOUT_ACCRUE, PAYOUT_SETTLE, REFUND, GATEWAY_FEE
}

// ==========================================
// NHÓM 07–08. VẬN CHUYỂN GHN & HOÀN TIỀN / KHIẾU NẠI (7 enum)
// ==========================================
enum CarrierType { GHN, GHTK, VIETTEL_POST, JNT_EXPRESS, GENIE_EXPRESS }              // ← ShipmentTracking.carrierType (GENIE_EXPRESS chỉ là lựa chọn giao nội thành — không có app tài xế ở TLCN)
enum ShipmentDirection { OUTBOUND, RETURN }                                             // ← ShipmentTracking.direction
enum ShipmentTrackingStatus {                                                           // ← ShipmentTracking.status (9 trạng thái)
    AWAITING_PICKUP, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED, RETURNING, RETURNED, LOST
}
enum RefundRequestType { RETURN_AND_REFUND, REFUND_ONLY }                               // ← RefundRequest.requestType
enum RefundResolution { APPROVE_WITH_RETURN, APPROVE_REFUND_ONLY }                      // ← RefundRequest.resolutionMode
enum RefundStatus {                                                                     // ← RefundRequest.status (9 trạng thái)
    REQUESTED, AWAITING_EVIDENCE, SHOP_REJECTED, DISPUTED, AWAITING_RETURN_SHIPMENT, RETURN_FAILED, APPROVED, REJECTED, REFUNDED
}
enum MediaType { IMAGE, VIDEO }                                                         // ← RefundEvidence.mediaType

// ==========================================
// NHÓM 09–10. ĐÁNH GIÁ, THÔNG BÁO & NỘI DUNG SÀN (7 enum)
// ==========================================
enum NotificationType {                                                                 // ← Notification.type (mở rộng: PRICE_DROP_ALERT, B2B_RFQ_QUOTED)
    ORDER_STATUS_CHANGED, KYC_APPROVED, KYC_REJECTED, PAYMENT_SUCCESS, PAYMENT_FAILED, ESCROW_RELEASED,
    WALLET_CREDITED, SYSTEM_ALERT, REFUND_PROCESSED, NEW_PLATFORM_VOUCHER, NEW_VENDOR_VOUCHER, NEW_ORDER_FOR_VENDOR,
    FLASH_SALE_REMINDER, NEW_DEVICE_LOGIN, PRODUCT_APPROVED, PRODUCT_REJECTED   // cảnh báo đăng nhập thiết bị lạ (UC-AUTH-02), kết quả duyệt SP (UC-CAT-12/13)
}
enum NotificationChannel { IN_APP, EMAIL }                                              // ← Notification.channel (mở rộng: PUSH, SMS, ZNS, WEBSOCKET)
enum NotificationDeliveryStatus { PENDING, SENT, DELIVERED, FAILED }                    // ← Notification.deliveryStatus
enum ReviewReportReason { SPAM, OFFENSIVE, FAKE_PURCHASE }                              // ← ReviewReport.reason
enum PlatformNotificationTarget { ALL, CUSTOMER, VENDOR, SPECIFIC_USERS }               // ← PlatformNotification.target (mở rộng: SHIPPER)
enum BannerPosition { HOME_TOP_CAROUSEL, HOME_TOP_SIDE, HOME_MIDDLE, CATEGORY_TOP, FLASH_SALE_TOP, POPUP } // ← HomeBanner.position
// BannerState { LIVE, SCHEDULED, ENDED, OFF } — KHÔNG lưu CSDL, do HomeBanner.state(now) tính (API 11.16 — plan banner D3)
enum AdminNotificationType { SYSTEM_ALERT, NEW_ORDER, USER_APPROVAL, WITHDRAW_REQUEST, ESCROW_DISCREPANCY } // ← AdminNotification.type (mở rộng: FRAUD_ALERT, KYC_BACKLOG, SLA_OVERDUE, COD_OVERDUE)

// ==========================================
// NHÓM 11–12. CHAT & NHẬT KÝ HỆ THỐNG (2 enum)
// ==========================================
enum MessageType { TEXT, IMAGE, PRODUCT_CARD, ORDER_CARD }                              // ← Message.type (thẻ sản phẩm / thẻ đơn hàng — UC-SOC-02)
enum AuditAction {                                                                      // ← SystemLog.actionType (mở rộng: MFA_ENABLED, MFA_DISABLED, MFA_CHALLENGE_FAILED, PAYOUT_REVIEWED)
    LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED, ACCOUNT_SUSPENDED, ACCOUNT_REACTIVATED,
    KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED, ORDER_STATUS_CHANGED, ESCROW_RELEASED,
    PAYOUT_APPROVED, PAYOUT_REJECTED, REFUND_PROCESSED, REVIEW_HIDDEN, COMMISSION_UPDATED,
    PERMISSION_GRANTED, PERMISSION_REVOKED, RBAC_ROLE_CHANGED, STAFF_INVITED, PII_ANONYMIZED,
    ACCESS_DENIED, PRODUCT_MODERATED,                 // lần truy cập bị chặn quyền (màn Nhật ký), kết quả duyệt sản phẩm
    PASSWORD_CHANGED, PASSWORD_RESET, EMAIL_CHANGED, OAUTH_LINKED, OAUTH_UNLINKED, SESSION_REVOKED,
    ACCOUNT_DELETION_REQUESTED, ACCOUNT_DELETION_CANCELLED,  // +8 giá trị của AUTH (V14 thay ck_system_logs_action_type — plan AUTH D20)
    BANNER_CHANGED,                                          // CHỈ CÒN cho bản ghi cũ (V18: tạo / sửa / bật / tắt / xóa gộp một mã, details.op) — code mới ghi 5 mã BANNER_* bên dưới
    KYC_SUPPLEMENT_REQUESTED, KYC_REQUEUED, KYC_DOCUMENT_VIEWED, PAYOUT_ACCOUNT_CHANGED, // Mở shop & KYC (V20, plan 2026-11-16 D3): yêu cầu bổ sung, đưa lại hàng chờ, nhân viên mở giấy tờ (NFR-07), đổi / xóa tài khoản nhận tiền
    BANNER_CREATED, BANNER_UPDATED, BANNER_ACTIVATED, BANNER_DEACTIVATED, BANNER_DELETED // Banner theo động từ cụ thể (V24, plan 2026-11-22 nhật ký banner N1) — details.changes {trường: {from, to}} khi sửa
}
```

> **Nhóm hiển thị của `AuditAction`** (`AuditAction.category()` → enum chỉ có ở API `AuditCategory`, màn Nhật ký hệ thống UC-ADM-07 — API_SPEC 16.2): **AUTH** = `LOGIN_SUCCESS, LOGIN_FAILED, PASSWORD_CHANGED, PASSWORD_RESET, EMAIL_CHANGED, OAUTH_LINKED, OAUTH_UNLINKED, SESSION_REVOKED, ACCOUNT_DELETION_REQUESTED, ACCOUNT_DELETION_CANCELLED` · **KYC** = `KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED, KYC_SUPPLEMENT_REQUESTED, KYC_REQUEUED, KYC_DOCUMENT_VIEWED` · **FINANCE** = `ESCROW_RELEASED, PAYOUT_APPROVED, PAYOUT_REJECTED, REFUND_PROCESSED, ORDER_STATUS_CHANGED, PAYOUT_ACCOUNT_CHANGED` · **MODERATION** = `REVIEW_HIDDEN, PRODUCT_MODERATED` · **SECURITY** = `ACCOUNT_LOCKED, ACCOUNT_SUSPENDED, ACCOUNT_REACTIVATED, ACCESS_DENIED, PII_ANONYMIZED` · **CONFIG** = `COMMISSION_UPDATED, PERMISSION_GRANTED, PERMISSION_REVOKED, RBAC_ROLE_CHANGED, STAFF_INVITED, BANNER_CHANGED, BANNER_CREATED, BANNER_UPDATED, BANNER_ACTIVATED, BANNER_DEACTIVATED, BANNER_DELETED`. Thêm giá trị `AuditAction` mới phải gán nhóm (switch không `default` — thiếu là lỗi biên dịch).

---

# PHỤ LỤC B — BẢNG QUAN HỆ ĐỂ VẼ CLASS DIAGRAM

> Phụ lục này là **kim chỉ nam để vẽ Class Diagram / ERD** của bản TLCN: **65 class `@Entity`**, **55 enum**, **101 quan hệ** phân loại Composition (◆), Aggregation (◇), Association (→).

---

## B.0. Quy tắc phân loại quan hệ (Được xác lập chuẩn xác theo Domain & Lifecycle)

Mỗi quan hệ giữa 2 entity trong hệ thống được phân định rõ ràng theo 3 mức độ ràng buộc chặt chẽ của UML:

| Loại quan hệ | Ký hiệu UML | Tiêu chí phân định trong hệ thống | Số lượng |
|---|:---:|---|:---:|
| **Composition** | ◆ hình thoi **đặc** ở phía "chủ" | **Sở hữu trọn vẹn (Strong Ownership / Life-bound)**: Bộ phận **không thể tồn tại độc lập** nếu không có chủ. Khi xóa bản ghi chủ, toàn bộ bản ghi con bị xóa theo (`ON DELETE CASCADE` ở DB). | **—** |
| **Aggregation** | ◇ hình thoi **rỗng** ở phía "chủ" | **Gom nhóm độc lập (Shared / Weak Ownership)**: Chủ gom nhóm các bộ phận nhưng bộ phận **vẫn có vòng đời và giá trị riêng biệt**. Khi xóa chủ, bộ phận chuyển sang trạng thái tự do hoặc hệ thống **chặn xóa chủ** khi còn bộ phận con. | **—** |
| **Association** | → mũi tên thường | **Tham chiếu nghiệp vụ thông thường**: Mũi tên đi **từ bảng giữ FK tới bảng được trỏ**. Dùng cho các chứng từ có vòng đời riêng (Order, PaymentTransaction, EscrowLedger, RefundRequest) và các bảng quan hệ N-N. | **—** |
| **Dependency** | ⇢ nét đứt | **Phụ thuộc lỏng / Không ràng buộc cứng ở DB**: FK mềm (`defaultAddressId`, `moderatorId`), sự kiện nội bộ Spring (`ApplicationEvent`), và các trường có kiểu Enum. | **—** |

### Quy tắc đọc Bội số (Multiplicity)
- **Phía bảng được trỏ:** `1` nếu cột FK là `NOT NULL`; `0..1` nếu cột FK cho phép `NULL` (nullable).
- **Phía bảng giữ FK:** `0..1` nếu cột FK có ràng buộc `UNIQUE` 1 cột; còn lại là `0..*`. Riêng `1..*` chỉ xuất hiện khi nghiệp vụ code bắt buộc tối thiểu 1 bản ghi con (ví dụ: tạo `Order` bắt buộc phải có ít nhất 1 `OrderItem`).

---

---

## B.1. Bảng tra cứu quan hệ đầy đủ (101 quan hệ — sinh từ mô hình TLCN)

| # | Quan hệ UML | Loại | Tên trường Khóa ngoại | Nằm ở |
|:---:|---|:---:|---|:---:|
| **I** | **NHÓM 1: NGƯỜI DÙNG & ĐỊNH DANH** | | | |
| 1 | `User` ◆ 1 — 0..1 `CustomerProfile` | ◆ Composition | `CustomerProfile.userId` · UNIQUE | Sơ đồ 1 |
| 2 | `User` ◆ 1 — 0..* `Address` | ◆ Composition | `Address.userId` | Sơ đồ 1 |
| 3 | `User` ◆ 1 — 0..1 `VendorProfile` | ◆ Composition | `VendorProfile.userId` · UNIQUE | Sơ đồ 1 |
| 4 | `VendorProfile` 0..* → 0..1 `Category` | → Association | `VendorProfile.categoryId` · nullable | Sơ đồ 1 |
| 5 | `User` ◆ 1 — 0..1 `AdminProfile` | ◆ Composition | `AdminProfile.userId` · UNIQUE | Sơ đồ 1 |
| 6 | `VendorProfile` ◆ 1 — 0..1 `KycResult` | ◆ Composition | `KycResult.vendorProfileId` · nullable, UNIQUE | Sơ đồ 1 |
| 7 | `User` ◆ 1 — 0..* `RefreshToken` | ◆ Composition | `RefreshToken.userId` | Sơ đồ 1 |
| 8 | `StaffPermission` 0..* → 1 `User` | → Association | `StaffPermission.grantedByAdminId` | Sơ đồ 1 |
| 9 | `User` ◆ 1 — 0..* `StaffPermission` | ◆ Composition | `StaffPermission.staffUserId` | Sơ đồ 1 |
| **II** | **NHÓM 2: DANH MỤC & SẢN PHẨM** | | | |
| 10 | `Category` ◇ 0..1 — 0..* `Category` | ◇ Aggregation | `Category.parentId` · nullable (cây phân cấp) | Sơ đồ 2 |
| 11 | `Category` ◇ 0..1 — 0..* `Product` | ◇ Aggregation | `Product.categoryId` · nullable | Sơ đồ 2 |
| 12 | `Product` 0..* → 0..1 `Brand` | → Association | `Product.brandId` · nullable | Sơ đồ 2 |
| 13 | `Product` 0..* → 1 `VendorProfile` | → Association | `Product.vendorId` | Sơ đồ 2 |
| 14 | `Product` ◆ 1 — 0..* `ProductVariant` | ◆ Composition | `ProductVariant.productId` | Sơ đồ 2 |
| 15 | `ProductImage` 0..* → 0..1 `ProductVariant` | → Association | `ProductImage.variantId` · nullable | Sơ đồ 2 |
| 16 | `Product` ◆ 1 — 0..* `ProductImage` | ◆ Composition | `ProductImage.productId` | Sơ đồ 2 |
| 17 | `UserWishlist` 0..* → 1 `Product` | → Association | `UserWishlist.productId` | Sơ đồ 2 |
| 18 | `User` ◆ 1 — 0..* `UserWishlist` | ◆ Composition | `UserWishlist.userId` · UNIQUE(user,prod) | Sơ đồ 2 |
| 19 | `Category` ◆ 1 — 0..* `CategoryAttribute` | ◆ Composition | `CategoryAttribute.categoryId` · UNIQUE(category, code) | Sơ đồ 2 |
| 20 | `Product` ◆ 1 — 0..* `ProductDocument` | ◆ Composition | `ProductDocument.productId` | Sơ đồ 8 |
| 21 | `CategoryProposal` 0..* → 0..1 `Category` (created) | → Association | `CategoryProposal.createdCategoryId` · nullable | Sơ đồ 8 |
| 22 | `CategoryProposal` 0..* → 0..1 `Category` (parent) | → Association | `CategoryProposal.parentCategoryId` · nullable | Sơ đồ 8 |
| 23 | `CategoryProposal` 0..* → 1 `User` (vendor) | → Association | `CategoryProposal.vendorId` | Sơ đồ 8 |
| 24 | `Product` ◆ 1 — 0..* `ProductModerationResult` | ◆ Composition | `ProductModerationResult.productId` | Sơ đồ 8 |
| 25 | `ProductViewStat` 0..* → 1 `User` (vendor) | → Association | `ProductViewStat.vendorId` | Sơ đồ 8 |
| 26 | `Product` ◆ 1 — 0..* `ProductViewStat` | ◆ Composition | `ProductViewStat.productId` · UNIQUE(product,date,source) | Sơ đồ 8 |
| **III** | **NHÓM 3: GIỎ HÀNG** | | | |
| 27 | `User` ◆ 1 — 0..1 `Cart` | ◆ Composition | `Cart.userId` · UNIQUE | Sơ đồ 2 |
| 28 | `CartItem` 0..* → 1 `ProductVariant` | → Association | `CartItem.productVariantId` | Sơ đồ 2 |
| 29 | `Cart` ◆ 1 — 0..* `CartItem` | ◆ Composition | `CartItem.cartId` | Sơ đồ 2 |
| **IV** | **NHÓM 4: KHUYẾN MÃI, VOUCHER, FLASH SALE & ĐIỂM** | | | |
| 30 | `Voucher` 0..* → 0..1 `VendorProfile` | → Association | `Voucher.vendorId` · nullable (NULL = Sàn tài trợ) | Sơ đồ 3 |
| 31 | `VoucherUsage` 0..* → 0..1 `Order` | → Association | `VoucherUsage.orderId` · nullable — chỉ voucher SHOP | Sơ đồ 3 |
| 32 | `VoucherUsage` 0..* → 1 `CheckoutSession` | → Association | `VoucherUsage.checkoutSessionId` · UNIQUE(voucher, session) | Sơ đồ 3 |
| 33 | `VoucherUsage` 0..* → 1 `User` | → Association | `VoucherUsage.userId` | Sơ đồ 3 |
| 34 | `VoucherUsage` 0..* → 1 `Voucher` | → Association | `VoucherUsage.voucherId` | Sơ đồ 3 |
| 35 | `UserVoucher` 0..* → 1 `User` | → Association | `UserVoucher.userId` | Sơ đồ 3 |
| 36 | `Voucher` ◆ 1 — 0..* `UserVoucher` | ◆ Composition | `UserVoucher.voucherId` | Sơ đồ 3 |
| 37 | `VoucherCategory` 0..* → 1 `Category` | → Association | `VoucherCategory.categoryId` | Sơ đồ 3 |
| 38 | `Voucher` ◆ 1 — 0..* `VoucherCategory` | ◆ Composition | `VoucherCategory.voucherId` | Sơ đồ 3 |
| 39 | `VoucherProduct` 0..* → 1 `Product` | → Association | `VoucherProduct.productId` | Sơ đồ 3 |
| 40 | `Voucher` ◆ 1 — 0..* `VoucherProduct` | ◆ Composition | `VoucherProduct.voucherId` | Sơ đồ 3 |
| 41 | `VoucherExcludedCategory` 0..* → 1 `Category` | → Association | `VoucherExcludedCategory.categoryId` | Sơ đồ 3 |
| 42 | `Voucher` ◆ 1 — 0..* `VoucherExcludedCategory` | ◆ Composition | `VoucherExcludedCategory.voucherId` | Sơ đồ 3 |
| 43 | `VoucherExcludedProduct` 0..* → 1 `Product` | → Association | `VoucherExcludedProduct.productId` | Sơ đồ 3 |
| 44 | `Voucher` ◆ 1 — 0..* `VoucherExcludedProduct` | ◆ Composition | `VoucherExcludedProduct.voucherId` | Sơ đồ 3 |
| 45 | `FlashSaleCampaign` 0..* → 0..1 `User` (vendor) | → Association | `FlashSaleCampaign.vendorId` · nullable — NULL = chiến dịch toàn sàn | Sơ đồ 9 |
| 46 | `FlashSaleCampaign` ◆ 1 — 0..* `FlashSaleItem` | ◆ Composition | `FlashSaleItem.campaignId` | Sơ đồ 9 |
| 47 | `FlashSaleItem` 0..* → 1 `Product` | → Association | `FlashSaleItem.productId` | Sơ đồ 9 |
| 48 | `User` ◆ 1 — 0..1 `UserCoin` | ◆ Composition | `UserCoin.userId` · UNIQUE (PK) | Sơ đồ 9 |
| 49 | `User` ◆ 1 — 0..* `CoinTransaction` | ◆ Composition | `CoinTransaction.userId` | Sơ đồ 9 |
| 50 | `User` ◆ 1 — 0..1 `DailyCheckin` | ◆ Composition | `DailyCheckin.customerId` · UNIQUE (1 dòng/khách, lưu chuỗi `streakCount`) | Sơ đồ 9 |
| **V** | **NHÓM 5: ĐƠN HÀNG** | | | |
| 51 | `CheckoutSession` 0..* → 1 `User` | → Association | `CheckoutSession.userId` | Sơ đồ 4 |
| 52 | `CheckoutSession` ◇ 0..1 — 1..* `Order` | ◇ Aggregation | `Order.checkoutSessionId` · nullable | Sơ đồ 4 |
| 53 | `Order` 0..* → 1 `User` | → Association | `Order.customerId` | Sơ đồ 4 |
| 54 | `Order` 0..* → 1 `VendorProfile` | → Association | `Order.vendorId` | Sơ đồ 4 |
| 55 | `OrderItem` 0..* → 1 `ProductVariant` | → Association | `OrderItem.productVariantId` | Sơ đồ 4 |
| 56 | `Order` ◆ 1 — 1..* `OrderItem` | ◆ Composition | `OrderItem.orderId` | Sơ đồ 4 |
| 57 | `OrderDiscount` 0..* → 0..1 `Voucher` | → Association | `OrderDiscount.voucherId` · nullable | Sơ đồ 4 |
| 58 | `Order` ◆ 1 — 0..* `OrderDiscount` | ◆ Composition | `OrderDiscount.orderId` | Sơ đồ 4 |
| 59 | `StockReservation` 0..* → 0..1 `Order` | → Association | `StockReservation.orderId` · nullable | Sơ đồ 4 |
| 60 | `StockReservation` 0..* → 1 `ProductVariant` | → Association | `StockReservation.productVariantId` | Sơ đồ 4 |
| **VI** | **NHÓM 6: THANH TOÁN & KÝ QUỸ** | | | |
| 61 | `PaymentTransaction` 0..* → 0..1 `CheckoutSession` | → Association | `PaymentTransaction.checkoutSessionId` · nullable | Sơ đồ 5 |
| 62 | `PaymentTransaction` 0..* → 0..1 `Order` | → Association | `PaymentTransaction.orderId` · nullable | Sơ đồ 5 |
| 63 | `PaymentRefund` 0..* → 1 `PaymentTransaction` | → Association | `PaymentRefund.paymentTransactionId` | Sơ đồ 5 |
| 64 | `PaymentRefund` 0..* → 1 `RefundRequest` | → Association | `PaymentRefund.refundRequestId` | Sơ đồ 5 |
| 65 | `EscrowLedger` 0..* → 1 `VendorProfile` | → Association | `EscrowLedger.vendorId` | Sơ đồ 5 |
| 66 | `EscrowLedger` 0..1 → 1 `Order` | → Association | `EscrowLedger.orderId` · UNIQUE | Sơ đồ 5 |
| 67 | `VendorProfile` ◆ 1 — 0..1 `VendorWallet` | ◆ Composition | `VendorWallet.vendorId` · UNIQUE | Sơ đồ 5 |
| 68 | `VendorWallet` ◆ 1 — 0..* `WalletTransaction` | ◆ Composition | `WalletTransaction.walletId` | Sơ đồ 5 |
| 69 | `WithdrawalRequest` 0..* → 0..1 `WalletTransaction` | → Association | `WithdrawalRequest.walletTransactionId` · nullable | Sơ đồ 5 |
| 70 | `WithdrawalRequest` 0..* → 1 `VendorWallet` | → Association | `WithdrawalRequest.walletId` | Sơ đồ 5 |
| 71 | `FinancialLedger` 0..* → 0..1 `Order` | → Association | `FinancialLedger.orderId` · nullable | Sơ đồ 5 |
| 72 | `FinancialLedger` 0..* → 0..1 `VendorProfile` | → Association | `FinancialLedger.vendorId` · nullable | Sơ đồ 5 |
| **VII** | **NHÓM 7: VẬN CHUYỂN** | | | |
| 73 | `ShipmentTracking` 0..* → 1 `Order` | → Association | `ShipmentTracking.orderId` · partial UNIQUE (`direction = OUTBOUND`) — 1 vận đơn đi + 0..N vận đơn hoàn | Sơ đồ 6 |
| 74 | `ShipmentTracking` 0..1 → 0..1 `RefundRequest` | → Association | `ShipmentTracking.refundRequestId` · nullable, partial UNIQUE (`direction = RETURN`) — vận đơn chiều về | Sơ đồ 6 |
| 75 | `ShipmentTracking` ◆ 1 — 0..* `ShipmentEvent` | ◆ Composition | `ShipmentEvent.shipmentId` | Sơ đồ 6 |
| 76 | `VendorProfile` ◆ 1 — 0..1 `VendorShippingConfig` | ◆ Composition | `VendorShippingConfig.vendorId` · UNIQUE | Sơ đồ 6 |
| **VIII** | **NHÓM 8: HOÀN TIỀN / KHIẾU NẠI** | | | |
| 77 | `RefundRequest` 0..* → 1 `Order` | → Association | `RefundRequest.orderId` | Sơ đồ 6 |
| 78 | `RefundRequest` 0..* → 1 `User` | → Association | `RefundRequest.requestedByUserId` | Sơ đồ 6 |
| 79 | `RefundRequestItem` 0..* → 1 `OrderItem` | → Association | `RefundRequestItem.orderItemId` | Sơ đồ 6 |
| 80 | `RefundRequest` ◆ 1 — 0..* `RefundRequestItem` | ◆ Composition | `RefundRequestItem.refundRequestId` | Sơ đồ 6 |
| 81 | `RefundRequest` ◆ 1 — 0..* `RefundEvidence` | ◆ Composition | `RefundEvidence.refundRequestId` | Sơ đồ 6 |
| **IX** | **NHÓM 9: ĐÁNH GIÁ & TƯƠNG TÁC SHOP** | | | |
| 82 | `ProductReview` 0..* → 1 `Product` | → Association | `ProductReview.productId` | Sơ đồ 7 |
| 83 | `ProductReview` 0..* → 1 `User` | → Association | `ProductReview.customerId` | Sơ đồ 7 |
| 84 | `ProductReview` 0..* → 1 `User` (vendor) | → Association | `ProductReview.vendorId` | Sơ đồ 7 |
| 85 | `ProductReview` 0..1 → 1 `OrderItem` | → Association | `ProductReview.orderItemId` · UNIQUE | Sơ đồ 7 |
| 86 | `ShopFollow` 0..* → 1 `User` (customer) | → Association | `ShopFollow.customerId` | Sơ đồ 7 |
| 87 | `ShopFollow` 0..* → 1 `User` (vendor) | → Association | `ShopFollow.vendorId` · UNIQUE(customer,vendor) | Sơ đồ 7 |
| 88 | `ReviewReport` 0..* → 1 `ProductReview` | → Association | `ReviewReport.reviewId` | Sơ đồ 7 |
| 89 | `ReviewReport` 0..* → 1 `User` | → Association | `ReviewReport.reporterId` | Sơ đồ 7 |
| 90 | `ProductReview` ◆ 1 — 0..* `ReviewHelpfulVote` | ◆ Composition | `ReviewHelpfulVote.reviewId` · UNIQUE(review,user) | Sơ đồ 7 |
| 91 | `ReviewHelpfulVote` 0..* → 1 `User` | → Association | `ReviewHelpfulVote.userId` | Sơ đồ 7 |
| **X** | **NHÓM 10: THÔNG BÁO & NỘI DUNG SÀN** | | | |
| 92 | `User` ◆ 1 — 0..* `Notification` | ◆ Composition | `Notification.userId` | Sơ đồ 10 |
| 93 | `PlatformNotification` ◆ 1 — 0..* `UserPlatformNotificationRead` | ◆ Composition | `UserPlatformNotificationRead.platformNotificationId` | Sơ đồ 10 |
| 94 | `UserPlatformNotificationRead` 0..* → 1 `User` | → Association | `UserPlatformNotificationRead.userId` | Sơ đồ 10 |
| 95 | `HomeBanner` 0..* → 0..1 `Category` | → Association | `HomeBanner.categoryId` · nullable — banner đầu trang ngành hàng | Sơ đồ 10 |
| **XI** | **NHÓM 11: NHẮN TIN (CHAT)** | | | |
| 96 | `Conversation` 0..* → 1 `User` (customer) | → Association | `Conversation.customerId` | Sơ đồ 7 |
| 97 | `Conversation` 0..* → 1 `VendorProfile` | → Association | `Conversation.vendorId` · UNIQUE(cust,vend) | Sơ đồ 7 |
| 98 | `Conversation` ◆ 1 — 0..* `Message` | ◆ Composition | `Message.conversationId` | Sơ đồ 7 |
| 99 | `Message` 0..* → 0..1 `ProductVariant` | → Association | `Message.productVariantId` · nullable | Sơ đồ 7 |
| 100 | `Message` 0..* → 1 `User` (sender) | → Association | `Message.senderId` | Sơ đồ 7 |
| 101 | `Message` 0..* → 0..1 `Order` | → Association | `Message.orderId` · nullable (thẻ đơn hàng — TLCN v1.3) | Sơ đồ 7 |

---

## B.2. Sơ đồ Cây phân rã Domain (TLCN)

```
                                  ┌──────────────────────────┐
                                  │           User           │ ◄── TRUNG TÂM MỌI THỰC THỂ (đăng nhập email / Google / Apple)
                                  └────────────┬─────────────┘
          ┌─────────────────┬──────────────────┼─────────────────┬─────────────────┐
          ▼                 ▼                  ▼                 ▼                 ▼
   CustomerProfile    VendorProfile       AdminProfile        Address         RefreshToken
   (Hồ sơ mua, Điểm)    (Shop, KYC, kho,    (3 chức danh) ──► StaffPermission
                       chỉ số, tạm nghỉ)
                            ├──► KycResult (AI Worker: QR + MRZ + quốc huy + vân tay + Face Match)
                            ├──► Product ──► ProductVariant · ProductImage · ProductDocument · ProductModerationResult (Gemini)
                            │       └──► Brand · Category ◄── CategoryAttribute · CategoryProposal
                            └──► VendorWallet ──► WalletTransaction · WithdrawalRequest

   User ──► Cart ──► CartItem ──► ProductVariant          User ──► UserWishlist ◄── Product      User ──► ShopFollow ──► Shop
   User ──► UserVoucher ◄── Voucher (3 tầng) ──► VoucherUsage · Voucher{Category,Product,ExcludedCategory,ExcludedProduct}
   User ──► UserCoin ──► CoinTransaction (lô Điểm 12 tháng)      FlashSaleCampaign ──► FlashSaleItem ──► Product

   [CHECKOUT] CheckoutSession ──► Order (1 đơn con / shop) ──► OrderItem ──► ProductReview ──► ReviewHelpfulVote · ReviewReport
                    │                  ├──► OrderDiscount ◄── Voucher
                    │                  ├──► StockReservation ◄── ProductVariant
                    │                  ├──► ShipmentTracking (GHN / GHTK / VTP / J&T) ──► ShipmentEvent
                    │                  ├──► EscrowLedger ──(7 ngày)──► VendorWallet          FinancialLedger (sổ cái kép)
                    │                  └──► RefundRequest ──► RefundRequestItem · RefundEvidence · PaymentRefund
                    └──► PaymentTransaction (VNPay IPN / webhook SePay VietQR)

   Conversation ──► Message (thẻ SP)    Notification · PlatformNotification ──► UserPlatformNotificationRead    HomeBanner    SystemLog
   SearchKeywordStat · ProductViewStat (thống kê, flush từ Redis)
```

---

## B.3. Tám nguyên tắc cốt lõi khi vẽ Class Diagram / ERD

1. **`vendorId` luôn trỏ tới `users.id`**, KHÔNG phải `vendor_profiles.id`. Quan hệ `Product → VendorProfile` và `Order → VendorProfile` thực chất liên kết qua `users.id` (`@JoinColumn(name = "vendor_id", referencedColumnName = "user_id")`).
2. **Bảng nối N-N thuần dùng khóa gộp `@IdClass`** (5 bảng: 4 bảng phạm vi voucher và `UserPlatformNotificationRead`); các quan hệ nhiều – nhiều mang thuộc tính được vẽ thành class riêng (`UserVoucher`, `UserWishlist`, `CartItem`, `OrderItem`).
3. **Quan hệ tự tham chiếu** chỉ có ở `Category` (`parentId`) — vẽ mũi tên vòng lại chính nó.
4. **Mỗi sơ đồ một domain** (10 sơ đồ, Phụ lục C); class ở sơ đồ khác vẽ ô xám `«→ Sơ đồ N»`, không kéo dây xuyên sơ đồ.
5. **FK mềm (không ràng buộc cứng ở DB)** vẽ **nét đứt**: `VendorProfile.kycVerifiedBy`, `Product.moderatorId`, `RefundRequest.resolvedByStaffId`, `WithdrawalRequest.processedByAdminId`, `WalletTransaction.referenceId`, `Notification.referenceId`.
6. **Trường `@Transient` / cột tổng hợp** (`Product.sold`, `Product.rating`, `Order.isReviewed`, `UserVoucher.isUsed`) đánh dấu thuộc tính dẫn xuất `/derived`.
7. **Enum** vẽ ô riêng `«enum»`, nối **từ entity tới enum** bằng nét đứt ghi tên trường (VD: `User ..> UserRole : role`) — danh sách đầy đủ ở B.4.
8. **Khóa chính** mọi bảng là `id BIGINT GENERATED ALWAYS AS IDENTITY`; mã nghiệp vụ hiển thị cho người dùng (`Order.orderCode`, `Voucher.code`) là cột UNIQUE riêng.

---

## B.4. Quan hệ Entity → Enum (62 cặp — sinh từ Phụ lục A)

| # | Entity sở hữu | Tên trường | Tên Enum | Số giá trị | Nằm ở |
|:---:|---|---|---|:---:|:---:|
| 1 | `User` | `role` | `UserRole` | 4 | Sơ đồ 1 |
| 2 | `User` | `status` | `AccountStatus` | 6 | Sơ đồ 1 |
| 3 | `CustomerProfile` | `gender` | `Gender` | 4 | Sơ đồ 1 |
| 4 | `VendorProfile` | `businessType` | `BusinessType` | 3 | Sơ đồ 1 |
| 5 | `VendorProfile` | `kycStatus` | `KycStatus` | 7 | Sơ đồ 1 |
| 6 | `VendorProfile` | `kycVerificationMethod` | `KycVerificationMethod` | 3 | Sơ đồ 1 |
| 7 | `AdminProfile` | `staffTitle` | `StaffTitle` | 3 | Sơ đồ 1 |
| 8 | `KycResult` | `overallStatus` | `KycOverallResult` | 3 | Sơ đồ 1 |
| 9 | `StaffPermission` | `permission` | `StaffPermissionEnum` | 13 | Sơ đồ 1 |
| 10 | `Product` | `shippingPayer` | `ShippingPayer` | 2 | Sơ đồ 2 |
| 11 | `Product` | `status` | `ProductStatus` | 7 | Sơ đồ 2 |
| 12 | `Product` | `itemCondition` | `ProductCondition` | 3 | Sơ đồ 2 |
| 13 | `CategoryAttribute` | `dataType` | `AttributeDataType` | 6 | Sơ đồ 2 |
| 14 | `ProductDocument` | `documentType` | `ProductDocumentType` | 5 | Sơ đồ 8 |
| 15 | `CategoryProposal` | `status` | `CategoryProposalStatus` | 3 | Sơ đồ 8 |
| 16 | `ProductModerationResult` | `decision` | `ModerationDecision` | 3 | Sơ đồ 8 |
| 17 | `ProductViewStat` | `source` | `TrafficSource` | 5 | Sơ đồ 8 |
| 18 | `Voucher` | `scope` | `VoucherScope` | 3 | Sơ đồ 3 |
| 19 | `Voucher` | `type` | `VoucherType` | 3 | Sơ đồ 3 |
| 20 | `Voucher` | `discountType` | `DiscountType` | 3 | Sơ đồ 3 |
| 21 | `Voucher` | `sponsorType` | `FundingParty` | 3 | Sơ đồ 3 |
| 22 | `FlashSaleCampaign` | `status` | `FlashSaleStatus` | 4 | Sơ đồ 9 |
| 23 | `CoinTransaction` | `type` | `CoinTransactionType` | 6 | Sơ đồ 9 |
| 24 | `CheckoutSession` | `status` | `SessionStatus` | 5 | Sơ đồ 4 |
| 25 | `Order` | `paymentMethod` | `PaymentMethod` | 3 | Sơ đồ 4 |
| 26 | `Order` | `paymentStatus` | `PaymentStatus` | 8 | Sơ đồ 4 |
| 27 | `Order` | `fundingStatus` | `FundingStatus` | 4 | Sơ đồ 4 |
| 28 | `Order` | `status` | `OrderStatus` | 19 | Sơ đồ 4 |
| 29 | `Order` | `ALLOWED` | `OrderStatus` | 19 | Sơ đồ 4 |
| 30 | `Order` | `ALLOWED` | `OrderStatus` | 19 | Sơ đồ 4 |
| 31 | `OrderDiscount` | `voucherType` | `VoucherType` | 3 | Sơ đồ 4 |
| 32 | `OrderDiscount` | `fundedBy` | `FundingParty` | 3 | Sơ đồ 4 |
| 33 | `StockReservation` | `status` | `StockReservationStatus` | 4 | Sơ đồ 4 |
| 34 | `PaymentTransaction` | `paymentMethod` | `PaymentMethod` | 3 | Sơ đồ 5 |
| 35 | `PaymentTransaction` | `status` | `PaymentStatus` | 8 | Sơ đồ 5 |
| 36 | `PaymentRefund` | `status` | `PaymentRefundStatus` | 3 | Sơ đồ 5 |
| 37 | `EscrowLedger` | `fundingSource` | `FundingSource` | 2 | Sơ đồ 5 |
| 38 | `EscrowLedger` | `status` | `EscrowStatus` | 6 | Sơ đồ 5 |
| 39 | `WalletTransaction` | `type` | `WalletTransactionType` | 5 | Sơ đồ 5 |
| 40 | `WalletTransaction` | `referenceType` | `WalletReferenceType` | 4 | Sơ đồ 5 |
| 41 | `WithdrawalRequest` | `status` | `WithdrawalStatus` | 5 | Sơ đồ 5 |
| 42 | `FinancialLedger` | `eventType` | `FinancialEventType` | 10 | Sơ đồ 5 |
| 43 | `ShipmentTracking` | `direction` | `ShipmentDirection` | 2 | Sơ đồ 6 |
| 44 | `ShipmentTracking` | `carrierType` | `CarrierType` | 5 | Sơ đồ 6 |
| 45 | `ShipmentTracking` | `status` | `ShipmentTrackingStatus` | 9 | Sơ đồ 6 |
| 46 | `RefundRequest` | `requestType` | `RefundRequestType` | 2 | Sơ đồ 6 |
| 47 | `RefundRequest` | `resolutionMode` | `RefundResolution` | 2 | Sơ đồ 6 |
| 48 | `RefundRequest` | `status` | `RefundStatus` | 9 | Sơ đồ 6 |
| 49 | `RefundEvidence` | `mediaType` | `MediaType` | 2 | Sơ đồ 6 |
| 50 | `ReviewReport` | `reason` | `ReviewReportReason` | 3 | Sơ đồ 7 |
| 51 | `Notification` | `type` | `NotificationType` | 16 | Sơ đồ 10 |
| 52 | `Notification` | `channel` | `NotificationChannel` | 2 | Sơ đồ 10 |
| 53 | `Notification` | `deliveryStatus` | `NotificationDeliveryStatus` | 4 | Sơ đồ 10 |
| 54 | `PlatformNotification` | `target` | `PlatformNotificationTarget` | 4 | Sơ đồ 10 |
| 55 | `HomeBanner` | `position` | `BannerPosition` | 6 | Sơ đồ 10 |
| 56 | `AdminNotification` | `type` | `AdminNotificationType` | 5 | Sơ đồ 10 |
| 57 | `Message` | `type` | `MessageType` | 4 | Sơ đồ 7 |
| 58 | `SystemLog` | `actionType` | `AuditAction` | 30 (22 + 8 của AUTH, V14) | Sơ đồ 10 |
| 59 | `Address` | `label` | `AddressLabel` | 2 | Sơ đồ 1 |
| 60 | `FlashSaleItem` | `status` | `FlashSaleItemStatus` | 3 | Sơ đồ 9 |
| 61 | `CodRemittance` | `carrierType` | `CarrierType` | 5 | Sơ đồ 5 |
| 62 | `CodRemittance` | `status` | `CodRemittanceStatus` | 2 | Sơ đồ 5 |

---

# PHỤ LỤC C — ĐẶC TẢ 10 SƠ ĐỒ CLASS DIAGRAM (BẢN TLCN)

> Mỗi sơ đồ gồm: phạm vi nghiệp vụ, entity tham gia (PK / FK / UQ), enum, ngăn Operations và ma trận quan hệ — đủ để vẽ lại trên Enterprise Architect, Draw.io, PlantUML hoặc StarUML.
> Bộ mã `java-code/DiagramNN_*.java` và `Genie_ClassDiagram_UML25.xmi` cùng thư mục được **sinh từ chính tài liệu này** (package «Sơ đồ NN — …»).

---

## C.0. TẤM TỔNG — 10 sơ đồ domain

### C.0.1. Bảng ánh xạ 12 nhóm thực thể → 10 sơ đồ

| Sơ đồ | Tên sơ đồ | Nhóm thực thể | Entity | Enum | File Java (package `com.genie.platform.…`) |
|:---:|---|---|:---:|:---:|---|
| 1 | Định danh, Hồ sơ người dùng, KYC & Phân quyền nhân viên (Nhóm 1) | 1. Người dùng & định danh | 8 | 9 | `Diagram01_IdentityAndRbac.java` (`d01_identity`) |
| 2 | Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3) | 2. Danh mục & sản phẩm, 3. Giỏ hàng | 9 | 4 | `Diagram02_CatalogAndCart.java` (`d02_catalog_cart`) |
| 3 | Voucher 3 tầng Shop / Sàn / Freeship & Phạm vi áp dụng (Nhóm 4) | 4. Khuyến mãi, voucher, flash sale & điểm | 7 | 4 | `Diagram03_VoucherPromotion.java` (`d03_voucher`) |
| 4 | Phiên thanh toán, Đơn hàng con theo shop & Giữ chỗ tồn kho (Nhóm 5) | 5. Đơn hàng | 5 | 6 | `Diagram04_OrderAndInventory.java` (`d04_order_inventory`) |
| 5 | Thanh toán VNPay / VietQR, Ký quỹ Escrow, Ví shop, Rút tiền & Sổ cái (Nhóm 6) | 6. Thanh toán & ký quỹ | 7 | 7 | `Diagram05_PaymentAndEscrow.java` (`d05_payment_escrow`) |
| 6 | Vận đơn 3PL, Cấu hình vận chuyển của shop, Trả hàng & Hoàn tiền (Nhóm 7–8) | 7. Vận chuyển, 8. Hoàn tiền / khiếu nại | 6 | 7 | `Diagram06_ShippingAndRefund.java` (`d06_shipping_refund`) |
| 7 | Đánh giá, Hữu ích, Báo cáo vi phạm, Theo dõi shop & Chat (Nhóm 9, 11) | 9. Đánh giá & tương tác shop, 11. Nhắn tin (chat) | 6 | 2 | `Diagram07_ReviewFollowAndChat.java` (`d07_review_follow_chat`) |
| 8 | Kiểm duyệt sản phẩm, Giấy tờ chứng thực, Đề xuất danh mục & Thống kê truy cập (Nhóm 2) | 2. Danh mục & sản phẩm | 5 | 4 | `Diagram08_ProductModerationAndInsights.java` (`d08_moderation_insights`) |
| 9 | Flash Sale, Điểm Genie & Điểm danh (Nhóm 4) | 4. Khuyến mãi, voucher, flash sale & điểm | 5 | 2 | `Diagram09_FlashSaleAndCoin.java` (`d09_flashsale_coin`) |
| 10 | Thông báo cá nhân, Thông báo sàn, Thông báo quản trị, Banner & Nhật ký hệ thống (Nhóm 10, 12) | 10. Thông báo & nội dung sàn, 12. Nhật ký hệ thống | 6 | 7 | `Diagram10_NotificationBannerAndAudit.java` (`d10_notification_banner_audit`) |
| **Σ** | | **12 nhóm** | **64** | **52** | 10 file |

---

## C.2. CHI TIẾT 10 SƠ ĐỒ CLASS DIAGRAM THEO TỪNG DOMAIN

### C.2.1. Sơ đồ 1 — Định danh, Hồ sơ người dùng, KYC & Phân quyền nhân viên (Nhóm 1)
* **Phạm vi nghiệp vụ:** Tài khoản 4 vai trò (CUSTOMER, VENDOR, STAFF, ADMIN — không có Super Admin) kèm đăng nhập Google / Apple, hồ sơ khách hàng (ẩn danh hóa khi xóa tài khoản), hồ sơ shop (kho GHN, tài khoản nhận tiền, chỉ số trang shop, chat tự động, tạm nghỉ), hồ sơ nhân viên 3 chức danh, quyền chi tiết, kết quả KYC và phiên đăng nhập.
* **Các Entity tham gia (8 entity):**
  1. `User` («PK» id, «UQ» email, «version», «FK» oauthProviderId)
  2. `CustomerProfile` («PK» id, «UQ» userId, «FK» defaultAddressId)
  3. `Address` («PK» id, «FK» userId, «FK» districtId, «FK» provinceId)
  4. `VendorProfile` («PK» id, «UQ» userId, «FK» categoryId, «FK» pickupProvinceId, «FK» pickupDistrictId, «FK» ghnShopId)
  5. `AdminProfile` («PK» id, «UQ» userId)
  6. `KycResult` («PK» id, «UQ» vendorProfileId)
  7. `RefreshToken` («PK» id, «FK» userId, «UQ» tokenHash)
  8. `StaffPermission` («PK» id, «FK» staffUserId, «FK» grantedByAdminId)
* **Các Enum liên kết (10 enum):** `AccountStatus`, `AddressLabel`, `BusinessType`, `Gender`, `KycOverallResult`, `KycStatus`, `KycVerificationMethod`, `StaffPermissionEnum`, `StaffTitle`, `UserRole`.
* **Ngăn Operations (phương thức nghiệp vụ — chi tiết Phụ lục E):** `User`: isLocked(), recordFailedLogin(), recordSuccessfulLogin(), suspend(), reactivateIfSuspensionExpired(), reactivate(), anonymize(), hasPassword(), changePassword(), verifyEmail(), changeEmail(), linkIdentity(), unlinkIdentity(), lockFor(), clearLock(), requestDeletion(), cancelDeletion(), isPendingDeletion(), newCustomer() · `CustomerProfile`: setDefaultAddress(), anonymize(), updateProfile(), changeAvatar(), create() · `Address`: markAsDefault(), softDelete(), update(), unmarkDefault() · `VendorProfile`: submitKyc(), startAiProcessing(), routeToManualReview(), requestSupplement(), approveKyc(), rejectKyc(), isPickupConfigured(), effectiveCommissionRate(), verifyPayoutAccount(), isPayoutAccountVerified(), applyRating(), isPreferredShop(), startVacation(), endVacation(), isOnVacation() · `AdminProfile`: toggleModerator(), changeTitle() · `KycResult`: requiresManualReview(), isPassed(), applyRiskScore() · `RefreshToken`: isUsable(), revoke(), touch(), issue() · `StaffPermission`: isActive(), revoke()
* **Ma trận quan hệ:**
  - `User` ◆ 1 — 0..1 `CustomerProfile`
  - `User` ◆ 1 — 0..* `Address`
  - `User` ◆ 1 — 0..1 `VendorProfile`
  - `VendorProfile` 0..* → 0..1 `Category`
  - `User` ◆ 1 — 0..1 `AdminProfile`
  - `VendorProfile` ◆ 1 — 0..1 `KycResult`
  - `User` ◆ 1 — 0..* `RefreshToken`
  - `StaffPermission` 0..* → 1 `User`
  - `User` ◆ 1 — 0..* `StaffPermission`
* **💡 Ghi chú kỹ thuật khi vẽ diagram:**
  - Đặt `User` ở giữa phía trên; 3 profile (`CustomerProfile`, `VendorProfile`, `AdminProfile`) ở hàng ngang bên dưới.
  - `KycResult` treo dưới `VendorProfile`; `StaffPermission` treo dưới `AdminProfile` (nối `User` qua `staffUserId`).

---

### C.2.2. Sơ đồ 2 — Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3)
* **Phạm vi nghiệp vụ:** Cây danh mục kèm tỷ lệ hoa hồng và bộ thuộc tính chuẩn, thương hiệu, sản phẩm SPU (tình trạng, đặt trước, lượt xem) – biến thể SKU – ảnh, danh sách yêu thích và giỏ hàng.
* **Các Entity tham gia (9 entity):**
  1. `Category` («PK» id, «FK» parentId)
  2. `CategoryAttribute` («PK» id, «FK» categoryId)
  3. `Brand` («PK» id, «FK» createdByUserId)
  4. `Product` («PK» id, «FK» vendorId, «FK» categoryId, «FK» moderatorId, «version», «FK» brandId)
  5. `ProductVariant` («PK» id, «FK» productId, «UQ» sku, «version»)
  6. `ProductImage` («PK» id, «FK» productId, «FK» variantId)
  7. `UserWishlist` («PK» id, «FK» userId, «FK» productId)
  8. `Cart` («PK» id, «UQ» userId)
  9. `CartItem` («PK» id, «FK» cartId, «FK» productVariantId)
* **Các Enum liên kết (4 enum):** `AttributeDataType`, `ProductCondition`, `ProductStatus`, `ShippingPayer`.
* **Ngăn Operations (phương thức nghiệp vụ — chi tiết Phụ lục E):** `Category`: isRoot(), changeCommissionRate() · `CategoryAttribute`: accepts(), deactivate() · `Brand`: proposedBy(), verify() · `Product`: submitForReview(), approve(), reject(), hide(), markPreOrder(), flag() · `ProductVariant`: availableStock(), reserve(), releaseReservation(), commitSale(), softDelete() · `ProductImage`: markPrimary() · `Cart`: addItem(), removeItem(), subtotal(), mergeGuestItems() · `CartItem`: increase(), lineTotal() · `UserWishlist`: of()
* **Ma trận quan hệ:**
  - `Category` ◇ 0..1 — 0..* `Category`
  - `Category` ◇ 0..1 — 0..* `Product`
  - `Product` 0..* → 0..1 `Brand`
  - `Product` 0..* → 1 `VendorProfile`
  - `Product` ◆ 1 — 0..* `ProductVariant`
  - `ProductImage` 0..* → 0..1 `ProductVariant`
  - `Product` ◆ 1 — 0..* `ProductImage`
  - `UserWishlist` 0..* → 1 `Product`
  - `User` ◆ 1 — 0..* `UserWishlist`
  - `Category` ◆ 1 — 0..* `CategoryAttribute`
  - `User` ◆ 1 — 0..1 `Cart`
  - `CartItem` 0..* → 1 `ProductVariant`
  - `Cart` ◆ 1 — 0..* `CartItem`
* **💡 Ghi chú kỹ thuật khi vẽ diagram:**
  - Cột trái: `Category` (vòng tự tham chiếu) → `CategoryAttribute`; `Brand` ở trên `Product`.
  - `Product` → `ProductVariant` / `ProductImage`; cột phải: `Cart` → `CartItem` trỏ `ProductVariant`; `UserWishlist` trỏ `Product`.

---

### C.2.3. Sơ đồ 3 — Voucher 3 tầng Shop / Sàn / Freeship & Phạm vi áp dụng (Nhóm 4)
* **Phạm vi nghiệp vụ:** Voucher 3 tầng (Shop / Sàn / Freeship) có phạm vi danh mục – sản phẩm, danh sách loại trừ, nhà tài trợ đồng tài trợ, ngân sách tự tắt; kho voucher của khách và lịch sử dùng.
* **Các Entity tham gia (7 entity):**
  1. `Voucher` («PK» id, «UQ» code, «FK» vendorId, «version»)
  2. `VoucherCategory` («PK» voucherId+categoryId)
  3. `VoucherProduct` («PK» voucherId+productId)
  4. `VoucherExcludedCategory` («PK» voucherId+categoryId)
  5. `VoucherExcludedProduct` («PK» voucherId+productId)
  6. `VoucherUsage` («PK» id, «FK» voucherId, «FK» userId, «FK» checkoutSessionId, «FK» orderId)
  7. `UserVoucher` («PK» id, «FK» userId, «FK» voucherId)
* **Các Enum liên kết (4 enum):** `DiscountType`, `FundingParty`, `VoucherScope`, `VoucherType`.
* **Ngăn Operations (phương thức nghiệp vụ — chi tiết Phụ lục E):** `Voucher`: isApplicable(), calculateDiscount(), consume(), restore(), validateConfig() · `VoucherCategory`: matches() · `VoucherProduct`: matches() · `VoucherExcludedCategory`: matches() · `VoucherExcludedProduct`: matches() · `VoucherUsage`: of() · `UserVoucher`: markUsed()
* **Ma trận quan hệ:**
  - `Voucher` 0..* → 0..1 `VendorProfile`
  - `VoucherUsage` 0..* → 0..1 `Order`
  - `VoucherUsage` 0..* → 1 `CheckoutSession`
  - `VoucherUsage` 0..* → 1 `User`
  - `VoucherUsage` 0..* → 1 `Voucher`
  - `UserVoucher` 0..* → 1 `User`
  - `Voucher` ◆ 1 — 0..* `UserVoucher`
  - `VoucherCategory` 0..* → 1 `Category`
  - `Voucher` ◆ 1 — 0..* `VoucherCategory`
  - `VoucherProduct` 0..* → 1 `Product`
  - `Voucher` ◆ 1 — 0..* `VoucherProduct`
  - `VoucherExcludedCategory` 0..* → 1 `Category`
  - `Voucher` ◆ 1 — 0..* `VoucherExcludedCategory`
  - `VoucherExcludedProduct` 0..* → 1 `Product`
  - `Voucher` ◆ 1 — 0..* `VoucherExcludedProduct`
* **💡 Ghi chú kỹ thuật khi vẽ diagram:**
  - `Voucher` ở giữa; 4 bảng phạm vi (`VoucherCategory`, `VoucherProduct`, `VoucherExcludedCategory`, `VoucherExcludedProduct`) xếp dọc bên phải.
  - `UserVoucher` (kho voucher) bên trái, `VoucherUsage` (lượt dùng theo phiên) phía dưới; ô tham chiếu `Category`, `Product` «→ Sơ đồ 2».

---

### C.2.4. Sơ đồ 4 — Phiên thanh toán, Đơn hàng con theo shop & Giữ chỗ tồn kho (Nhóm 5)
* **Phạm vi nghiệp vụ:** Phiên thanh toán gom nhiều đơn con theo shop, lời nhắn cho shop, dòng hàng chụp giá (snapshot) và phân bổ giảm giá voucher / Điểm, giữ chỗ tồn kho 15 phút.
* **Các Entity tham gia (5 entity):**
  1. `CheckoutSession` («PK» id, «FK» userId)
  2. `Order` («PK» id, «UQ» orderCode, «FK» customerId, «FK» vendorId, «FK» checkoutSessionId, «version»)
  3. `OrderItem` («PK» id, «FK» orderId, «FK» productVariantId)
  4. `OrderDiscount` («PK» id, «FK» orderId, «FK» voucherId)
  5. `StockReservation` («PK» id, «FK» productVariantId, «FK» orderId, «version»)
* **Các Enum liên kết (6 enum):** `FundingStatus`, `OrderStatus`, `PaymentMethod`, `PaymentStatus`, `SessionStatus`, `StockReservationStatus`.
* **Ngăn Operations (phương thức nghiệp vụ — chi tiết Phụ lục E):** `CheckoutSession`: markPaymentPending(), complete(), fail(), expire() · `Order`: transitionTo(), isCod(), markPaid(), markPaymentFailed(), confirmByVendor(), startPreparing(), readyForPickup(), startDelivering(), markDelivered(), markCustomerConfirmed(), markDeliveryFailed(), retryDelivery(), startReturning(), markReturnedToVendor(), cancel(), requestReturn(), approveReturn(), rejectReturn(), markRefunded(), netPaidAmount(), complete(), recalculateGrandTotal() · `OrderItem`: applyCommission(), allocateDiscount() · `OrderDiscount`: isPlatformFunded() · `StockReservation`: isExpired(), confirm(), release()
* **Ma trận quan hệ:**
  - `CheckoutSession` 0..* → 1 `User`
  - `CheckoutSession` ◇ 0..1 — 1..* `Order`
  - `Order` 0..* → 1 `User`
  - `Order` 0..* → 1 `VendorProfile`
  - `OrderItem` 0..* → 1 `ProductVariant`
  - `Order` ◆ 1 — 1..* `OrderItem`
  - `OrderDiscount` 0..* → 0..1 `Voucher`
  - `Order` ◆ 1 — 0..* `OrderDiscount`
  - `StockReservation` 0..* → 0..1 `Order`
  - `StockReservation` 0..* → 1 `ProductVariant`
* **💡 Ghi chú kỹ thuật khi vẽ diagram:**
  - `CheckoutSession` trên cùng → nhiều `Order` (mỗi shop 1 đơn con).
  - `Order` sở hữu `OrderItem`, `OrderDiscount`; `StockReservation` nối `Order` và `ProductVariant` «→ Sơ đồ 2».

---

### C.2.5. Sơ đồ 5 — Thanh toán VNPay / VietQR, Ký quỹ Escrow, Ví shop, Rút tiền & Sổ cái (Nhóm 6)
* **Phạm vi nghiệp vụ:** Giao dịch VNPay / VietQR (SePay) / COD, hoàn tiền qua cổng, sổ ký quỹ Escrow 7 ngày (COD fund sau đối soát GHN), ví shop, lệnh rút tiền và sổ cái tài chính kép.
* **Các Entity tham gia (8 entity):**
  1. `PaymentTransaction` («PK» id, «FK» orderId, «FK» checkoutSessionId, «UQ» idempotencyKey, «FK» gatewayTransactionId, «version»)
  2. `PaymentRefund` («PK» id, «FK» paymentTransactionId, «FK» refundRequestId, «FK» gatewayRefundId)
  3. `EscrowLedger` («PK» id, «UQ» orderId, «FK» vendorId)
  4. `VendorWallet` («PK» id, «UQ» vendorId, «version»)
  5. `WalletTransaction` («PK» id, «FK» walletId, «FK» referenceId)
  6. `WithdrawalRequest` («PK» id, «FK» walletId, «FK» processedByAdminId, «FK» walletTransactionId)
  7. `FinancialLedger` («PK» seq, «FK» referenceId, «FK» orderId, «FK» vendorId)
  8. `CodRemittance` («PK» id, «UQ» remittanceCode, «UQ» bankReference, «FK» reconciledByStaffId) — TLCN v1.3
* **Các Enum liên kết (8 enum):** `CodRemittanceStatus`, `EscrowStatus`, `FinancialEventType`, `FundingSource`, `PaymentRefundStatus`, `WalletReferenceType`, `WalletTransactionType`, `WithdrawalStatus`.
* **Ngăn Operations (phương thức nghiệp vụ — chi tiết Phụ lục E):** `PaymentTransaction`: markSuccess(), markFailed(), refundableAmount(), recordRefund() · `PaymentRefund`: markSuccess(), markFailed() · `EscrowLedger`: fund(), totalFunded(), isFullyFunded(), scheduleRelease(), isReleasable(), release(), dispute(), releaseDisputeHold(), applyPartialRefund(), markRefunded(), freeze() · `VendorWallet`: availableBalance(), credit(), debit(), holdForWithdrawal(), settleWithdrawal(), verifyHash() · `WalletTransaction`: debit(), credit(), isConsistent() · `WithdrawalRequest`: approve(), startProcessing(), complete(), reject() · `FinancialLedger`: entry()
* **Ma trận quan hệ:**
  - `PaymentTransaction` 0..* → 0..1 `CheckoutSession`
  - `PaymentTransaction` 0..* → 0..1 `Order`
  - `PaymentRefund` 0..* → 1 `PaymentTransaction`
  - `PaymentRefund` 0..* → 1 `RefundRequest`
  - `EscrowLedger` 0..* → 1 `VendorProfile`
  - `EscrowLedger` 0..1 → 1 `Order`
  - `VendorProfile` ◆ 1 — 0..1 `VendorWallet`
  - `VendorWallet` ◆ 1 — 0..* `WalletTransaction`
  - `WithdrawalRequest` 0..* → 0..1 `WalletTransaction`
  - `WithdrawalRequest` 0..* → 1 `VendorWallet`
  - `FinancialLedger` 0..* → 0..1 `Order`
  - `FinancialLedger` 0..* → 0..1 `VendorProfile`
* **💡 Ghi chú kỹ thuật khi vẽ diagram:**
  - Trục dòng tiền từ trái sang phải: `PaymentTransaction` → `EscrowLedger` → `VendorWallet` → `WalletTransaction` / `WithdrawalRequest`.
  - `FinancialLedger` đặt riêng phía dưới (chỉ ghi thêm, không FK cứng tới ví); `PaymentRefund` treo dưới `PaymentTransaction`.
  - `CodRemittance` đứng cạnh `EscrowLedger` (không FK cứng — liên kết đơn qua `linesJson`; dùng `CarrierType` của Sơ đồ 6).

---

### C.2.6. Sơ đồ 6 — Vận đơn 3PL, Cấu hình vận chuyển của shop, Trả hàng & Hoàn tiền (Nhóm 7–8)
* **Phạm vi nghiệp vụ:** Vận đơn 3PL (GHN mặc định, GHTK / ViettelPost / J&T khi shop bật) chiều đi / chiều về và sự kiện hành trình; yêu cầu trả hàng – hoàn tiền với dòng hàng và bằng chứng.
* **Các Entity tham gia (6 entity):**
  1. `ShipmentTracking` («PK» id, «FK» orderId, «FK» refundRequestId, «FK» carrierOrderId)
  2. `ShipmentEvent` («PK» id, «FK» shipmentId)
  3. `VendorShippingConfig` («PK» id, «UQ» vendorId)
  4. `RefundRequest` («PK» id, «FK» orderId, «FK» requestedByUserId, «FK» resolvedByStaffId)
  5. `RefundRequestItem` («PK» id, «FK» refundRequestId, «FK» orderItemId)
  6. `RefundEvidence` («PK» id, «FK» refundRequestId)
* **Các Enum liên kết (7 enum):** `CarrierType`, `MediaType`, `RefundRequestType`, `RefundResolution`, `RefundStatus`, `ShipmentDirection`, `ShipmentTrackingStatus`.
* **Ngăn Operations (phương thức nghiệp vụ — chi tiết Phụ lục E):** `ShipmentTracking`: applyCarrierStatus(), isFinal(), forReturn() · `ShipmentEvent`: isDuplicateOf() · `VendorShippingConfig`: isFreeshipFor(), supports() · `RefundRequest`: approveByShop(), applyApproval(), shopReject(), requestMoreEvidence(), submitAdditionalEvidence(), closeIfEvidenceNotProvided(), autoApproveIfShopSilent(), markReturnShipped(), confirmReturnReceived(), failReturnIfOverdue(), escalateToDispute(), closeIfCustomerSilent(), approve(), reject(), markRefunded(), requireStatus() · `RefundRequestItem`: amount() · `RefundEvidence`: isVideo(), isAdditional()
* **Ma trận quan hệ:**
  - `ShipmentTracking` 0..* → 1 `Order`
  - `ShipmentTracking` 0..1 → 0..1 `RefundRequest`
  - `ShipmentTracking` ◆ 1 — 0..* `ShipmentEvent`
  - `VendorProfile` ◆ 1 — 0..1 `VendorShippingConfig`
  - `RefundRequest` 0..* → 1 `Order`
  - `RefundRequest` 0..* → 1 `User`
  - `RefundRequestItem` 0..* → 1 `OrderItem`
  - `RefundRequest` ◆ 1 — 0..* `RefundRequestItem`
  - `RefundRequest` ◆ 1 — 0..* `RefundEvidence`
* **💡 Ghi chú kỹ thuật khi vẽ diagram:**
  - Bên trái: `ShipmentTracking` → `ShipmentEvent`; `VendorShippingConfig` (1 shop 1 cấu hình: hãng bật / ngưỡng freeship) đặt trên cùng. Bên phải: `RefundRequest` → `RefundRequestItem`, `RefundEvidence`.
  - Vận đơn chiều về (`direction = RETURN`) nối `RefundRequest` bằng `refundRequestId`.

---

### C.2.7. Sơ đồ 7 — Đánh giá, Hữu ích, Báo cáo vi phạm, Theo dõi shop & Chat (Nhóm 9, 11)
* **Phạm vi nghiệp vụ:** Đánh giá sản phẩm của người mua đã nhận hàng, lượt bấm Hữu ích, báo cáo đánh giá vi phạm, theo dõi shop và hội thoại chat khách – shop (thẻ sản phẩm, ảnh, trạng thái đã xem).
* **Các Entity tham gia (6 entity):**
  1. `ProductReview` («PK» id, «UQ» orderItemId, «FK» productId, «FK» customerId, «FK» vendorId)
  2. `ReviewHelpfulVote` («PK» id, «FK» reviewId, «FK» userId)
  3. `ReviewReport` («PK» id, «FK» reviewId, «FK» reporterId)
  4. `ShopFollow` («PK» id, «FK» customerId, «FK» vendorId)
  5. `Conversation` («PK» id, «FK» customerId, «FK» vendorId)
  6. `Message` («PK» id, «FK» conversationId, «FK» senderId, «FK» productVariantId, «FK» orderId)
* **Các Enum liên kết (2 enum):** `MessageType`, `ReviewReportReason`.
* **Ngăn Operations (phương thức nghiệp vụ — chi tiết Phụ lục E):** `ProductReview`: reply(), hide(), adjustHelpful(), attachSentiment() · `ReviewHelpfulVote`: of() · `ReviewReport`: of() · `ShopFollow`: of() · `Conversation`: onNewMessage(), markReadBy() · `Message`: markRead()
* **Ma trận quan hệ:**
  - `ProductReview` 0..* → 1 `Product`
  - `ProductReview` 0..* → 1 `User`
  - `ProductReview` 0..* → 1 `User` (vendor)
  - `ProductReview` 0..1 → 1 `OrderItem`
  - `ShopFollow` 0..* → 1 `User` (customer)
  - `ShopFollow` 0..* → 1 `User` (vendor)
  - `ReviewReport` 0..* → 1 `ProductReview`
  - `ReviewReport` 0..* → 1 `User`
  - `ProductReview` ◆ 1 — 0..* `ReviewHelpfulVote`
  - `ReviewHelpfulVote` 0..* → 1 `User`
  - `Conversation` 0..* → 1 `User` (customer)
  - `Conversation` 0..* → 1 `VendorProfile`
  - `Conversation` ◆ 1 — 0..* `Message`
  - `Message` 0..* → 0..1 `ProductVariant`
  - `Message` 0..* → 0..1 `Order`
  - `Message` 0..* → 1 `User` (sender)
* **💡 Ghi chú kỹ thuật khi vẽ diagram:**
  - Cụm đánh giá: `ProductReview` (nối `OrderItem` «→ Sơ đồ 4») → `ReviewHelpfulVote`, `ReviewReport`.
  - Cụm xã hội: `ShopFollow` (khách ↔ shop) và `Conversation` → `Message` (thẻ sản phẩm trỏ `ProductVariant` «→ Sơ đồ 2»).

---

### C.2.8. Sơ đồ 8 — Kiểm duyệt sản phẩm, Giấy tờ chứng thực, Đề xuất danh mục & Thống kê truy cập (Nhóm 2)
* **Phạm vi nghiệp vụ:** Giấy tờ chứng thực sản phẩm, kết quả kiểm duyệt tự động bằng Gemini, đề xuất danh mục của người bán, thống kê từ khóa tìm kiếm và lượt xem / nguồn truy cập sản phẩm.
* **Các Entity tham gia (5 entity):**
  1. `ProductDocument` («PK» id, «FK» productId)
  2. `ProductModerationResult` («PK» id, «FK» productId)
  3. `CategoryProposal` («PK» id, «FK» vendorId, «FK» parentCategoryId, «FK» createdCategoryId, «FK» reviewedByStaffId)
  4. `SearchKeywordStat` («PK» id)
  5. `ProductViewStat` («PK» id, «FK» productId, «FK» vendorId)
* **Các Enum liên kết (4 enum):** `CategoryProposalStatus`, `ModerationDecision`, `ProductDocumentType`, `TrafficSource`.
* **Ngăn Operations (phương thức nghiệp vụ — chi tiết Phụ lục E):** `ProductDocument`: isValidOn() · `ProductModerationResult`: fallback(), isAutoReject() · `CategoryProposal`: approve(), reject() · `SearchKeywordStat`: accumulate() · `ProductViewStat`: accumulate()
* **Ma trận quan hệ:**
  - `Product` ◆ 1 — 0..* `ProductDocument`
  - `CategoryProposal` 0..* → 0..1 `Category` (created)
  - `CategoryProposal` 0..* → 0..1 `Category` (parent)
  - `CategoryProposal` 0..* → 1 `User` (vendor)
  - `Product` ◆ 1 — 0..* `ProductModerationResult`
  - `ProductViewStat` 0..* → 1 `User` (vendor)
  - `Product` ◆ 1 — 0..* `ProductViewStat`
* **💡 Ghi chú kỹ thuật khi vẽ diagram:**
  - `Product` «→ Sơ đồ 2» ở giữa; `ProductDocument`, `ProductModerationResult`, `ProductViewStat` treo quanh.
  - `CategoryProposal` nối `Category` «→ Sơ đồ 2»; `SearchKeywordStat` đứng riêng (không FK).

---

### C.2.9. Sơ đồ 9 — Flash Sale, Điểm Genie & Điểm danh (Nhóm 4)
* **Phạm vi nghiệp vụ:** Chiến dịch Flash Sale (sàn hoặc shop) với suất giá sốc riêng; ví Điểm Genie và sổ biến động Điểm theo lô có hạn 12 tháng.
* **Các Entity tham gia (5 entity):**
  1. `FlashSaleCampaign` («PK» id, «FK» vendorId)
  2. `FlashSaleItem` («PK» id, «FK» campaignId, «FK» productId, «version»)
  3. `UserCoin` («PK» userId, «version»)
  4. `CoinTransaction` («PK» id, «FK» userId, «FK» referenceId)
  5. `DailyCheckin` («PK» id, «UQ» customerId)
* **Các Enum liên kết (3 enum):** `CoinTransactionType`, `FlashSaleItemStatus`, `FlashSaleStatus`.
* **Ngăn Operations (phương thức nghiệp vụ — chi tiết Phụ lục E):** `FlashSaleCampaign`: schedule(), activate(), isRunning(), end() · `FlashSaleItem`: validateAgainst(), remaining(), reserve(), releaseReservation(), recordSale() · `UserCoin`: earn(), spend(), verifyHash() · `CoinTransaction`: isExpired(), isCredit() · `DailyCheckin`: checkIn(), firstFor()
* **Ma trận quan hệ:**
  - `FlashSaleCampaign` 0..* → 0..1 `User` (vendor)
  - `FlashSaleCampaign` ◆ 1 — 0..* `FlashSaleItem`
  - `FlashSaleItem` 0..* → 1 `Product`
  - `User` ◆ 1 — 0..1 `UserCoin`
  - `User` ◆ 1 — 0..* `CoinTransaction`
  - `User` ◆ 1 — 0..1 `DailyCheckin`
* **💡 Ghi chú kỹ thuật khi vẽ diagram:**
  - `FlashSaleCampaign` → `FlashSaleItem` (trỏ `Product` «→ Sơ đồ 2»).
  - `UserCoin` (khóa chính = userId) → `CoinTransaction` theo lô; `DailyCheckin` (1 khách 1 dòng, giữ chuỗi điểm danh).

---

### C.2.10. Sơ đồ 10 — Thông báo cá nhân, Thông báo sàn, Thông báo quản trị, Banner & Nhật ký hệ thống (Nhóm 10, 12)
* **Phạm vi nghiệp vụ:** Thông báo cá nhân (in-app / email), thông báo phát toàn sàn và trạng thái đã đọc, banner trang chủ, nhật ký kiểm toán (kể cả lần truy cập bị chặn).
* **Các Entity tham gia (6 entity):**
  1. `Notification` («PK» id, «FK» userId, «FK» referenceId)
  2. `PlatformNotification` («PK» id)
  3. `UserPlatformNotificationRead` («PK» userId+platformNotificationId)
  4. `HomeBanner` («PK» id, «FK» categoryId, «FK» createdByAdminId)
  5. `AdminNotification` («PK» id)
  6. `SystemLog` («PK» id, «FK» userId, «FK» referenceId)
* **Các Enum liên kết (7 enum):** `AdminNotificationType`, `AuditAction`, `BannerPosition`, `NotificationChannel`, `NotificationDeliveryStatus`, `NotificationType`, `PlatformNotificationTarget`.
* **Ngăn Operations (phương thức nghiệp vụ — chi tiết Phụ lục E):** `Notification`: markSent(), markFailed(), markRead() · `PlatformNotification`: incrementRead() · `UserPlatformNotificationRead`: of() · `HomeBanner`: isLive(), deactivate(), recordStats(), ctr(), create(), state(), activate(), changeSchedule(), changeContent(), changeImages(), isDeletable() · `AdminNotification`: markRead() · `SystemLog`: prePersist(), of()
* **Ma trận quan hệ:**
  - `User` ◆ 1 — 0..* `Notification`
  - `PlatformNotification` ◆ 1 — 0..* `UserPlatformNotificationRead`
  - `UserPlatformNotificationRead` 0..* → 1 `User`
  - `HomeBanner` 0..* → 0..1 `Category`
* **💡 Ghi chú kỹ thuật khi vẽ diagram:**
  - `Notification` (cá nhân, fan-out on write) và `PlatformNotification` → `UserPlatformNotificationRead` (fan-out on read) đặt cạnh nhau để so sánh.
  - `HomeBanner`, `SystemLog` đứng riêng (không FK cứng).

---

## C.3. Luồng nghiệp vụ xuyên sơ đồ (TLCN)

| Luồng | Đi qua sơ đồ | Entity chính |
|---|---|---|
| Đặt hàng nhiều shop | 2 → 3 → 4 → 5 | `Cart` → `CheckoutSession` → `Order`×N → `StockReservation` → `PaymentTransaction` |
| Xếp chồng voucher + Điểm | 3 → 9 → 4 | `UserVoucher` → `Voucher` (3 tầng) → `UserCoin.spend()` → `OrderDiscount` / `OrderItem.coinDiscount` |
| Thanh toán VNPay / VietQR | 4 → 5 | `PaymentTransaction` (IPN / webhook SePay) → `Order.markPaid()` → `EscrowLedger.fund()` → `FinancialLedger` |
| COD + escrow | 4 → 6 → 5 | `Order` (COD) → `ShipmentTracking` (GHN) → đối soát → `EscrowLedger` `PENDING_FUNDING` → `HELD` |
| Giải ngân 7 ngày | 4 → 5 | `Order.complete()` → `EscrowLedger.release()` → `VendorWallet.credit()` → `WalletTransaction` |
| Kiểm duyệt sản phẩm | 2 → 8 | `Product.submitForReview()` → `ProductModerationResult` (Gemini) → Staff duyệt → `Product` `PUBLISHED` / `REJECTED` |
| Trả hàng / hoàn tiền | 4 → 6 → 5 | `RefundRequest` → `EscrowLedger.dispute()` → `PaymentRefund` (VNPay) / chuyển khoản tay (VietQR, COD) |
| Rút tiền shop | 5 | `WithdrawalRequest` → `VendorWallet.holdForWithdrawal()` → `settleWithdrawal()` |

## C.4. Hướng dẫn vẽ lại

- **Enterprise Architect:** *Import Package from XMI* → chọn `Genie_ClassDiagram_UML25.xmi` (đã có sẵn 10 sơ đồ và vị trí phần tử); hoặc *Code Import* thư mục `java-code/`.
- **Draw.io / StarUML:** vẽ theo C.2; class ở sơ đồ khác vẽ ô xám `«→ Sơ đồ N»`, không kéo dây xuyên sơ đồ.
- **Enum:** ô vàng nhạt `«enum»`, nối nét đứt từ entity, ghi tên trường.

---

# PHỤ LỤC D — ĐỐI CHIẾU TLCN ↔ DATN (BẢN HOÀN CHỈNH) & LỘ TRÌNH MỞ RỘNG MÔ HÌNH

## D.1. Con số

| | DATN (`DATN/`, đồ án tốt nghiệp) | TLCN (`TLCN/`) |
|---|:---:|:---:|
| Nhóm thực thể | 32 | 12 |
| Entity / bảng vật lý | 162 (+7 mới v1.1 = 169) | 64 |
| Class `@IdClass` (khóa gộp) | 11 | 5 |
| Enum | 124 (+5 mới v1.1) | 52 |
| Quan hệ khóa ngoại | 195 (+10 mới v1.1) | 100 |
| Sơ đồ lớp | 19 | 10 |

## D.2. Ánh xạ mục TLCN → mục DATN

| Entity | Mục TLCN | Mục DATN |
|---|:---:|:---:|
| `User` | 1.1 | 1.1 |
| `CustomerProfile` | 1.2 | 1.2 |
| `Address` | 1.3 | 1.3 |
| `VendorProfile` | 1.4 | 1.4 |
| `AdminProfile` | 1.5 | 1.5 |
| `KycResult` | 1.6 | 1.6 |
| `RefreshToken` | 1.7 | 1.8 |
| `StaffPermission` | 1.8 | 1.7 |
| `Category` | 2.1 | 2.1 |
| `Product` | 2.2 | 2.2 |
| `ProductVariant` | 2.3 | 2.3 |
| `ProductImage` | 2.4 | 2.4 |
| `UserWishlist` | 2.5 | 18.7 |
| `CategoryAttribute` | 2.6 | 2.5 |
| `Brand` | 2.7 | mới v1.1 — thêm cả DATN |
| `ProductDocument` | 2.8 | mới v1.1 — thêm cả DATN |
| `CategoryProposal` | 2.9 | mới v1.1 — thêm cả DATN |
| `ProductModerationResult` | 2.10 | mới v1.1 — thêm cả DATN |
| `SearchKeywordStat` | 2.11 | mới v1.1 — thêm cả DATN |
| `ProductViewStat` | 2.12 | mới v1.1 — thêm cả DATN |
| `Cart` | 3.1 | 3.1 |
| `CartItem` | 3.2 | 3.2 |
| `Voucher` | 4.1 | 4.1 |
| `VoucherUsage` | 4.2 | 4.3 |
| `UserVoucher` | 4.3 | 4.4 |
| `VoucherCategory` | 4.4 | 4.2 |
| `VoucherProduct` | 4.4 | 4.2 |
| `VoucherExcludedCategory` | 4.4 | 4.2 |
| `VoucherExcludedProduct` | 4.4 | 4.2 |
| `FlashSaleCampaign` | 4.5 | 13.1 |
| `FlashSaleItem` | 4.6 | 13.2 |
| `UserCoin` | 4.7 | 10.1 |
| `CoinTransaction` | 4.8 | 10.2 |
| `DailyCheckin` | 4.9 | 10.3 |
| `CheckoutSession` | 5.1 | 5.1 |
| `Order` | 5.2 | 5.2 |
| `OrderItem` | 5.3 | 5.3 |
| `OrderDiscount` | 5.4 | 5.4 |
| `StockReservation` | 5.5 | 5.5 |
| `PaymentTransaction` | 6.1 | 6.1 |
| `PaymentRefund` | 6.2 | 6.2 |
| `EscrowLedger` | 6.3 | 6.3 |
| `VendorWallet` | 6.4 | 6.4 |
| `WalletTransaction` | 6.5 | 6.5 |
| `WithdrawalRequest` | 6.6 | 6.6 |
| `FinancialLedger` | 6.7 | 6.7 |
| `CodRemittance` | 6.8 | — (mới ở TLCN v1.3, cần thêm vào DATN) |
| `ShipmentTracking` | 7.1 | 7.1 |
| `ShipmentEvent` | 7.2 | 7.2 |
| `VendorShippingConfig` | 7.3 | 7.3 |
| `RefundRequest` | 8.1 | 8.1 |
| `RefundRequestItem` | 8.2 | 8.2 |
| `RefundEvidence` | 8.3 | 8.3 |
| `ProductReview` | 9.1 | 9.1 |
| `ShopFollow` | 9.2 | 9.2 |
| `ReviewReport` | 9.3 | 9.3 |
| `ReviewHelpfulVote` | 9.4 | mới v1.1 — thêm cả DATN |
| `Notification` | 10.1 | 11.1 |
| `PlatformNotification` | 10.2 | 11.2 |
| `UserPlatformNotificationRead` | 10.3 | 11.3 |
| `HomeBanner` | 10.4 | 18.14 |
| `AdminNotification` | 10.5 | 11.4 |
| `Conversation` | 11.1 | 12.1 |
| `Message` | 11.2 | 12.2 |
| `SystemLog` | 12.1 | 14.1 |

## D.3. Thứ tự mở rộng đề xuất (lên DATN)

1. **Ví khách hàng & đa cổng** (DATN nhóm 15) — thêm `PaymentMethod.INTERNAL_WALLET, MOMO, ZALOPAY, BNPL`, `FundingSource.WALLET, SPLIT`.
2. **Voucher tầng 4 (ưu đãi thanh toán)** — thêm `VoucherType.PAYMENT`, cột `Voucher.applicablePaymentMethod`.
3. **Genie Express & app tài xế** (DATN nhóm 16) — thêm `UserRole.SHIPPER`, `KycResult.shipperId`, `ShipperCodLedger`.
4. **Flash Sale tải cao** — chuyển cửa chặn tải sang Redis + hàng đợi (giữ nguyên `FlashSaleItem`).
5. **Gamification, đa kho, tài khoản phụ shop, MFA, PDPA đầy đủ**, rồi các nhóm hạ tầng 21–30 khi lưu lượng thật đòi hỏi.

> Mọi bước trên chỉ **thêm** bảng / cột / giá trị enum — tên entity, bảng, cột, enum của TLCN trùng 100% DATN nên dữ liệu chuyển thẳng.

---

# PHỤ LỤC E — DANH MỤC PHƯƠNG THỨC NGHIỆP VỤ (NGĂN OPERATIONS CỦA CLASS DIAGRAM)

> Tổng hợp **202 phương thức nghiệp vụ** của **65 class `@Entity`** + **19 phương thức bổ sung khi hiện thực AUTH** (đánh dấu *(AUTH)*, 2026-11-14 — tổng 221) (sinh từ thân các class). Ký hiệu UML: `+` public, `-` private, `{static}` phương thức tĩnh (factory). Getter/Setter Lombok không liệt kê.

## E.1. Nhóm 1 — Người dùng & định danh

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `Address` | `+ markAsDefault(): void` | Đánh dấu địa chỉ mặc định (service bỏ cờ ở địa chỉ khác cùng user) | UC-AUTH-09 |
|  | `+ softDelete(): void` | Xóa mềm địa chỉ | UC-AUTH-09 |
|  | `+ update(...): void` *(AUTH)* | Sửa toàn bộ địa chỉ (tên tỉnh / quận / phường do server điền từ mã GHN) | UC-AUTH-09 |
|  | `+ unmarkDefault(): void` *(AUTH)* | Bỏ cờ mặc định | UC-AUTH-09 |
| `AdminProfile` | `+ toggleModerator(active: boolean): void` | Bật/tắt trạng thái đang trực kiểm duyệt | UC-ADM-03 |
|  | `+ changeTitle(newTitle: StaffTitle): Set<StaffPermissionEnum>` | Đổi chức danh, trả bộ quyền mặc định để Admin xác nhận | UC-ADM-01, UC-ADM-02 |
| `CustomerProfile` | `+ setDefaultAddress(addressId: Long): void` | Đặt địa chỉ giao hàng mặc định | UC-AUTH-09 |
|  | `+ anonymize(now: OffsetDateTime): void` | Xóa PII hồ sơ, giữ ID cho khóa ngoại | UC-AUTH-10 |
|  | `+ updateProfile(fullName, phone, gender, dob, bio, language): void` *(AUTH)* | Sửa hồ sơ (`null` = giữ nguyên) | UC-AUTH-08 |
|  | `+ changeAvatar(url: String): void` *(AUTH)* | Đổi ảnh đại diện (URL công khai Silo, ≤ 500 ký tự) | UC-AUTH-03, UC-AUTH-08 |
|  | `+ {static} create(user: User, fullName, phone, language): CustomerProfile` *(AUTH)* | Tạo hồ sơ cùng transaction với `User` mới | UC-AUTH-01, UC-AUTH-03 |
| `KycResult` | `+ requiresManualReview(): boolean` | Kết quả cần nhân viên duyệt thủ công | UC-VND-03 |
|  | `+ isPassed(): boolean` | AI kết luận đạt | UC-VND-03 |
|  | `+ applyRiskScore(score: int, signalsJson: String, sampled: boolean, autoApproveBelow: int, rejectFrom: int, now: OffsetDateTime): KycOverallResult` | Chốt điểm rủi ro và kết luận: ≥ rejectFrom → FAIL; ≥ autoApproveBelow hoặc bị lấy mẫu → PARTIAL; còn lại PASS (ngưỡng mặc định 30 / 70, cấu hình genie.kyc.risk.*) | UC-VND-03 |
| `RefreshToken` | `+ isUsable(now: OffsetDateTime): boolean` | Token còn dùng được (chưa thu hồi, chưa hết hạn 7 ngày) | UC-AUTH-02 |
|  | `+ revoke(reason: String, now: OffsetDateTime): void` | Thu hồi khi xoay vòng / đăng xuất / phát hiện dùng lại | UC-AUTH-11 |
|  | `+ touch(now: OffsetDateTime): void` *(AUTH)* | Ghi `lastUsedAt` khi làm mới | UC-AUTH-02, UC-AUTH-11 |
|  | `+ {static} issue(userId, familyId, tokenHash, deviceInfo, ip, expiresAt): RefreshToken` *(AUTH)* | Tạo token (cắt `deviceInfo` 255 ký tự) | UC-AUTH-02 |
| `StaffPermission` | `+ isActive(): boolean` | Quyền còn hiệu lực | UC-ADM-02 |
|  | `+ revoke(now: OffsetDateTime): void` | Thu hồi quyền | UC-ADM-02 |
| `User` | `+ isLocked(now: OffsetDateTime): boolean` | Kiểm tra tài khoản đang bị khóa tạm do sai mật khẩu | UC-AUTH-02 |
|  | `+ recordFailedLogin(now: OffsetDateTime): void` | Tăng đếm sai mật khẩu, đủ 5 lần thì khóa 15 phút | UC-AUTH-02 |
|  | `+ recordSuccessfulLogin(): void` | Reset bộ đếm khi đăng nhập đúng | UC-AUTH-02 |
|  | `+ suspend(reason: String, until: OffsetDateTime): void` | Đình chỉ tài khoản vi phạm (chỉ Admin) | UC-ADM-03 |
|  | `+ reactivateIfSuspensionExpired(now: OffsetDateTime): boolean` | Cron mở lại tài khoản khi hết thời hạn đình chỉ | UC-ADM-03 |
|  | `+ reactivate(): void` | Admin mở khóa thủ công | UC-ADM-03 |
|  | `+ anonymize(): void` | Ẩn danh hóa theo yêu cầu xóa dữ liệu (sau Grace Period 30 ngày); xóa cả `oauthProvider` | UC-AUTH-10 |
|  | `+ hasPassword(): boolean` *(AUTH)* | Đã có mật khẩu chưa (false = chỉ Google / Apple) | UC-AUTH-02, 03, 06 |
|  | `+ changePassword(hash: String): void` *(AUTH)* | Đặt / đổi mật khẩu | UC-AUTH-04, 06 |
|  | `+ verifyEmail(now: OffsetDateTime): void` *(AUTH)* | Đánh dấu email đã xác minh | UC-AUTH-01, 03, 08 |
|  | `+ changeEmail(newEmail: String, now: OffsetDateTime): void` *(AUTH)* | Đổi email đăng nhập (chuẩn hóa chữ thường) | UC-AUTH-08 |
|  | `+ linkIdentity(provider: String, sub: String): void` *(AUTH)* | Gắn Google / Apple (tối đa 1 nhà cung cấp) | UC-AUTH-03 |
|  | `+ unlinkIdentity(): void` *(AUTH)* | Gỡ liên kết (chỉ khi đã có mật khẩu) | UC-AUTH-03 |
|  | `+ lockFor(duration: Duration, now: OffsetDateTime): void` *(AUTH)* | Khóa tạm do sai OTP đăng nhập | UC-AUTH-02 |
|  | `+ clearLock(): void` *(AUTH)* | Gỡ khóa tạm (mở khóa qua email / đặt lại mật khẩu) | UC-AUTH-02, 04 |
|  | `+ requestDeletion(now: OffsetDateTime): void` *(AUTH)* | Ghi yêu cầu xóa tài khoản | UC-AUTH-10 |
|  | `+ cancelDeletion(): void` *(AUTH)* | Hủy yêu cầu xóa trong 30 ngày | UC-AUTH-10 |
|  | `+ isPendingDeletion(): boolean` *(AUTH)* | Đang chờ ẩn danh | UC-AUTH-08, 10 |
|  | `+ {static} newCustomer(email: String, passwordHash: String, now: OffsetDateTime): User` *(AUTH)* | Tạo người mua mới | UC-AUTH-01, 03 |
| `VendorProfile` | `+ {static} register(userId: Long, shopName: String, categoryId: Long, businessType: BusinessType, representative: String, contactPhone: String, contactEmail: String): VendorProfile` *(V20)* | Mở shop: KYC `NOT_SUBMITTED`, chưa có hoa hồng riêng / kho / GHN | UC-VND-01 |
|  | `+ isKycEditable(): boolean` *(V20)* | Hồ sơ còn sửa / nộp được (`NOT_SUBMITTED / REJECTED / SUPPLEMENT_REQUESTED`) | UC-VND-02 |
|  | `+ saveKycDraft(identityCode: String, taxCode: String, representative: String, shopName: String, businessType: BusinessType, biometricConsent: Boolean): void` *(V20)* | Lưu nháp kê khai KYC, không đổi trạng thái | UC-VND-02 |
|  | `+ attachKycDocuments(idFrontKey: String, idBackKey: String, licenseKey: String, selfieKey: String, documentsMetaJson: String): void` *(V20)* | Gắn giấy tờ đã mã hóa AES-256-GCM (`.enc`) + tên tệp / dung lượng | UC-VND-02 |
|  | `+ submitKyc(now: OffsetDateTime): void` | Nộp / nộp lại hồ sơ KYC — chỉ khi `isKycEditable()`; xóa lý do từ chối + yêu cầu bổ sung cũ | UC-VND-02 |
|  | `+ startAiProcessing(): void` | Đẩy hồ sơ vào hàng đợi AI Worker OCR/FaceMatch | UC-VND-03 |
|  | `+ routeToManualReview(): void` | AI nghi ngờ / điểm tin cậy thấp / AI lỗi hoặc quá hạn → chuyển nhân viên duyệt tay | UC-VND-03 |
|  | `+ requestManualReview(now: OffsetDateTime): void` *(V20)* | Hồ sơ bị từ chối → người bán yêu cầu nhân viên xem xét (`REJECTED → MANUAL_REVIEW`, 1 lần / hồ sơ) | UC-VND-03 |
|  | `+ isManualReviewRequested(): boolean` *(V20)* | Đã từng yêu cầu xem xét — AI kết luận `FAIL` thì vào `MANUAL_REVIEW` thay vì `REJECTED` | UC-VND-03 |
|  | `+ requestSupplement(documentsJson: String, note: String, deadline: OffsetDateTime): void` | Nhân viên yêu cầu bổ sung giấy tờ (không từ chối thẳng), hạn 7 ngày | UC-VND-04 |
|  | `+ expireSupplement(reason: String): void` *(V20)* | Quá hạn bổ sung → `REJECTED` (job hằng ngày) | UC-VND-04 |
|  | `+ requeueForReview(): void` *(V20)* | Đưa hồ sơ đã quyết định về hàng chờ duyệt tay (`VERIFIED / REJECTED / SUPPLEMENT_REQUESTED → MANUAL_REVIEW`), không chạy lại AI | UC-VND-04 |
|  | `+ approveKyc(staffId: Long, method: KycVerificationMethod, now: OffsetDateTime): void` | Duyệt KYC — chỉ từ `AI_PROCESSING` (AI tự duyệt) hoặc `MANUAL_REVIEW` | UC-VND-03, UC-VND-04 |
|  | `+ rejectKyc(reason: String): void` | Từ chối KYC kèm lý do — chỉ từ `AI_PROCESSING` hoặc `MANUAL_REVIEW` | UC-VND-03, UC-VND-04 |
|  | `+ markAsMall(mall: boolean): void` *(V20)* | Gắn / gỡ huy hiệu "Chính hãng" khi duyệt | UC-VND-04 |
|  | `+ setNegotiatedCommission(rate: BigDecimal): void` *(V20)* | Hoa hồng thương lượng riêng 0..1 (cần quyền `UPDATE_COMMISSION`) | UC-VND-04 |
|  | `+ nextRenameAllowedAt(now: OffsetDateTime): OffsetDateTime` *(V20)* | Lúc được đổi tên tiếp theo (null = đổi được ngay) | UC-VND-21 |
|  | `+ renameShop(newName: String, now: OffsetDateTime): void` *(V20)* | Đổi tên shop tối đa 1 lần / 30 ngày | UC-VND-21 |
|  | `+ updateBranding(description: String, logoKey: String, coverKey: String): void` *(V20)* | Mô tả, logo, ảnh bìa | UC-VND-21 |
|  | `+ configureAutoReply(enabled: boolean, greeting: String, awayMessage: String): void` *(V20)* | Tin nhắn tự động; bật mà cả hai nội dung rỗng → lỗi | UC-VND-06 |
|  | `+ updatePickup(phone: String, provinceId: Integer, provinceName: String, districtId: Integer, districtName: String, wardCode: String, wardName: String, addressLine: String, ghnShopId: Integer): void` *(V20)* | Lưu kho lấy hàng sau khi GHN cấp / cập nhật Shop ID | UC-VND-05 |
|  | `+ isPickupConfigured(): boolean` | Đã cấu hình kho lấy hàng & đăng ký GHN (điều kiện đăng bán / nhận đơn) | UC-VND-05 |
|  | `+ effectiveCommissionRate(categoryRate: BigDecimal): BigDecimal` | Tỷ lệ hoa hồng áp dụng: thỏa thuận riêng của shop → ngành hàng → mặc định 5% | UC-PAY-05 |
|  | `+ verifyPayoutAccount(bankReturnedHolderName: String, kycFullName: String, now: OffsetDateTime): boolean` | Xác minh tài khoản nhận tiền chính chủ: tên chủ TK do ngân hàng trả về (tra cứu tên qua đối tác chi hộ / NAPAS) phải khớp họ tên KYC sau khi bỏ dấu, viết hoa, gộp khoảng trắng | UC-VND-07 |
|  | `+ isPayoutAccountVerified(): boolean` | Đã có tài khoản nhận tiền chính chủ | UC-ADM-04 |
|  | `+ setPayoutAccount(bankName: String, accountNumber: String, holderName: String, bankReturnedHolderName: String, kycFullName: String, now: OffsetDateTime): boolean` *(V20)* | Lưu tài khoản nhận tiền chỉ khi tên ngân hàng trả về khớp họ tên KYC; không khớp → giữ tài khoản cũ | UC-VND-07 |
|  | `+ clearPayoutAccount(): void` *(V20)* | Xóa tài khoản nhận tiền | UC-VND-07 |
|  | `+ applyRating(newAvg: BigDecimal, newCount: int): void` | Cập nhật điểm đánh giá shop khi có đánh giá mới / bị ẩn | UC-CAT-16 |
|  | `+ isPreferredShop(): boolean` | Huy hiệu "Shop Yêu Thích": điểm ≥ 4.5, đủ 20 đánh giá và phản hồi chat ≥ 80% | UC-CAT-16 |
|  | `+ startVacation(from: OffsetDateTime, until: OffsetDateTime, message: String, now: OffsetDateTime): void` | Bật chế độ tạm nghỉ (Service kiểm tra không còn đơn CONFIRMED/PREPARING chưa bàn giao) | UC-VND-19 |
|  | `+ endVacation(): void` | Kết thúc tạm nghỉ (shop chủ động hoặc Cron khi tới vacationEndsAt) | UC-VND-19 |
|  | `+ isOnVacation(now: OffsetDateTime): boolean` | Đang tạm nghỉ: chặn đặt mua sản phẩm của shop, chat tự động trả vacationMessage | UC-VND-19 |

## E.2. Nhóm 2 — Danh mục & sản phẩm

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `Brand` | `+ {static} proposedBy(name: String, slug: String, vendorUserId: Long, now: OffsetDateTime): Brand` | Shop thêm thương hiệu mới ngay trong form đăng bán (chưa xác minh) | UC-CAT-09 |
|  | `+ verify(): void` | Staff xác nhận thương hiệu (khi duyệt sản phẩm đầu tiên dùng thương hiệu này) | UC-CAT-13 |
| `Category` | `+ isRoot(): boolean` | Danh mục gốc | UC-CAT-01 |
|  | `+ changeCommissionRate(rate: BigDecimal): void` | Đổi tỷ lệ hoa hồng ngành (chỉ áp dụng đơn mới) | UC-CAT-14 |
| `CategoryAttribute` | `+ accepts(rawValue: String, allowedValues: java.util.Set<String>): boolean` | Kiểm tra giá trị người bán nhập (null hợp lệ khi không bắt buộc; ENUM phải thuộc danh sách — Service parse allowedValuesJson) | UC-CAT-09 |
|  | `+ deactivate(): void` | Ngừng dùng thuộc tính (dữ liệu cũ trong specsJson giữ nguyên) | UC-CAT-15 |
| `CategoryProposal` | `+ approve(categoryId: Long, staffId: Long, now: OffsetDateTime): void` | Duyệt đề xuất — Service tạo Category mới rồi gọi hàm này | UC-CAT-17 |
|  | `+ reject(reason: String, staffId: Long, now: OffsetDateTime): void` | Từ chối đề xuất kèm lý do | UC-CAT-17 |
| `Product` | `+ submitForReview(): void` | Gửi duyệt sản phẩm (shop đã cấu hình kho lấy hàng | UC-CAT-09 |
|  | `+ approve(moderatorId: Long): void` | Duyệt và đăng bán | UC-CAT-13 |
|  | `+ reject(moderatorId: Long, reason: String): void` | Từ chối kèm lý do | UC-CAT-13 |
|  | `+ hide(): void` | Shop tự ẩn sản phẩm | UC-CAT-11 |
|  | `+ markPreOrder(days: int, shopAllows: boolean): void` | Bật đặt trước: shop phải cho phép và số ngày chuẩn bị 7–30 | UC-CAT-09 |
|  | `+ flag(reason: String): void` | Gắn cờ vi phạm / gỡ hàng giả | UC-CAT-12 |
| `ProductDocument` | `+ isValidOn(day: LocalDate): boolean` | Giấy tờ còn hiệu lực tại thời điểm duyệt | UC-CAT-13 |
| `ProductImage` | `+ markPrimary(): void` | Đặt làm ảnh đại diện | UC-CAT-10 |
| `ProductModerationResult` | `+ {static} fallback(productId: Long, modelName: String, error: String, now: OffsetDateTime): ProductModerationResult` | Kết quả khi gọi mô hình thất bại — luôn chuyển duyệt thủ công | UC-CAT-12 |
|  | `+ isAutoReject(rejectThreshold: int): boolean` | Có tự từ chối được không (REJECT và điểm rủi ro ≥ ngưỡng cấu hình) | UC-CAT-12 |
| `ProductVariant` | `+ availableStock(): int` | Tồn khả dụng = tồn vật lý − đang giữ chỗ | UC-CAT-06 |
|  | `+ reserve(qty: int): void` | Giữ chỗ mềm (service dùng UPDATE có điều kiện) | UC-ORD-07 |
|  | `+ releaseReservation(qty: int): void` | Nhả giữ chỗ (chỉ gọi sau khi StockReservation.release() thành công) | UC-ORD-08 |
|  | `+ commitSale(qty: int): void` | Trừ tồn cứng khi PAID | UC-PAY-03 |
|  | `+ softDelete(now: OffsetDateTime, force: boolean): void` | Xóa mềm biến thể: chặn khi còn giữ chỗ của đơn đang xử lý; còn tồn thì chỉ Admin được ép xóa (force) | UC-CAT-11 |
| `ProductViewStat` | `+ accumulate(views: int, visitors: int, carts: int): void` | Cộng dồn bộ đếm từ Redis (job 5 phút) | UC-CAT-06 |
| `SearchKeywordStat` | `+ accumulate(searches: int, clicks: int): void` | Cộng dồn bộ đếm từ Redis | UC-CAT-04 |

## E.3. Nhóm 3 — Giỏ hàng

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `Cart` | `+ addItem(item: CartItem): void` | Thêm sản phẩm (gộp nếu trùng SKU) | UC-ORD-01 |
|  | `+ removeItem(productVariantId: Long): void` | Xóa dòng hàng | UC-ORD-01 |
|  | `+ subtotal(): BigDecimal` | Tạm tính theo giá snapshot | UC-ORD-04 |
|  | `+ mergeGuestItems(guestItems: List<CartItem>): void` | Gộp giỏ tạm của khách vãng lai (lưu trên trình duyệt) vào giỏ tài khoản khi đăng nhập / đăng ký; service đã lọc SKU ngừng bán và giới hạn số lượng theo tồn kho trước khi gọi | UC-ORD-01 |
| `CartItem` | `+ increase(qty: int): void` | Tăng số lượng | UC-ORD-01 |
|  | `+ lineTotal(): BigDecimal` | Thành tiền dòng | UC-ORD-04 |

## E.4. Nhóm 4 — Khuyến mãi, voucher, flash sale & điểm

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `CoinTransaction` | `+ isExpired(now: OffsetDateTime): boolean` | Lô Điểm đã hết hạn (12 tháng) | UC-PRO-09 |
|  | `+ isCredit(): boolean` | Giao dịch cộng Điểm | UC-PRO-09 |
| `DailyCheckin` | `+ checkIn(now: OffsetDateTime): int` | Điểm danh, tính chuỗi & số Điểm thưởng | UC-PRO-08 |
| `FlashSaleCampaign` | `+ schedule(): void` | Lên lịch (sau khi duyệt) | UC-PRO-05 |
|  | `+ activate(now: OffsetDateTime): void` | Cron mở bán khi now ≥ startsAt (sau khi nạp tồn lên Redis lúc T-15 phút) | UC-PRO-05 |
|  | `+ isRunning(now: OffsetDateTime): boolean` | Đang trong khung giờ vàng | UC-PRO-06 |
|  | `+ end(): void` | Kết thúc chiến dịch (Cron khi now ≥ endsAt, hoặc Admin dừng) | UC-PRO-05 |
| `FlashSaleItem` | `+ validateAgainst(productAvailableStock: int): void` | Đăng ký: 0 < flashStock ≤ tồn khả dụng của sản phẩm (Σ stockCount − reservedCount các SKU); kiểm tra lại lúc nạp Redis T-15 phút | UC-PRO-05 |
|  | `+ remaining(): int` | Suất còn lại theo DB (đối soát với Redis: Redis stock = remaining() − số lượt đang trong hàng đợi) | UC-PRO-06 |
|  | `+ reserve(qty: int): void` | Worker lấy 1 lượt từ hàng đợi, tạo Order + StockReservation trong cùng transaction → giữ suất ở DB | UC-PRO-06 |
|  | `+ releaseReservation(qty: int): void` | Giữ chỗ hết hạn / đơn bị hủy trước khi trả tiền → trả suất (Service INCR lại flash_sale:{id}:stock) | UC-ORD-08 |
|  | `+ recordSale(qty: int): void` | Ghi bền khi đơn Flash Sale thanh toán thành công | UC-PRO-06 |
| `UserCoin` | `+ earn(amount: int, now: OffsetDateTime): void` | Cộng Điểm (service cập nhật lại balanceHash) | UC-PRO-08 |
|  | `+ spend(amount: int, now: OffsetDateTime): void` | Trừ Điểm (thanh toán đơn / đổi vật phẩm), FEFO theo lô ở service | UC-PRO-10 |
|  | `+ verifyHash(expectedHmac: String): boolean` | Phát hiện sửa số dư trực tiếp CSDL | UC-PRO-09 |
| `UserVoucher` | `+ markUsed(): void` | Đánh dấu voucher trong ví đã dùng | UC-PRO-03 |
| `Voucher` | `+ isApplicable(orderValue: BigDecimal, now: OffsetDateTime): boolean` | Kiểm tra điều kiện áp dụng (thời gian, lượt, đơn tối thiểu) | UC-PRO-03 |
|  | `+ calculateDiscount(base: BigDecimal): BigDecimal` | Tính số tiền giảm (có trần, không vượt giá trị) | UC-PRO-03 |
|  | `+ consume(discount: BigDecimal): void` | Ghi nhận một lượt dùng (service dùng @Version), hết ngân sách tự tắt | UC-PRO-03 |
|  | `+ restore(discount: BigDecimal): void` | Hoàn lượt khi đơn bị hủy / hết hạn thanh toán | UC-ORD-08 |
|  | `+ validateConfig(): void` | Kiểm tra cấu hình khi tạo/sửa: SHOP bắt buộc vendorId & shop tài trợ; PLATFORM không gắn shop & sàn tài trợ | UC-PRO-01, UC-PRO-02 |
| `VoucherCategory` | `+ matches(categoryId: Long): boolean` | Phạm vi áp dụng của voucher | UC-PRO-01 |
| `VoucherExcludedCategory` | `+ matches(categoryId: Long): boolean` | Phạm vi loại trừ của voucher | UC-PRO-01 |
| `VoucherExcludedProduct` | `+ matches(productId: Long): boolean` | Phạm vi loại trừ của voucher | UC-PRO-01 |
| `VoucherProduct` | `+ matches(productId: Long): boolean` | Phạm vi áp dụng của voucher | UC-PRO-01 |
| `VoucherUsage` | `+ {static} of(voucherId: Long, userId: Long, checkoutSessionId: Long, shopOrderId: Long, now: OffsetDateTime): VoucherUsage` | Tạo bản ghi lượt dùng | UC-PRO-03 |

## E.5. Nhóm 5 — Đơn hàng

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `CheckoutSession` | `+ markPaymentPending(): void` | Chuyển sang chờ thanh toán | UC-ORD-07 |
|  | `+ complete(now: OffsetDateTime): void` | Thanh toán thành công toàn phiên | UC-PAY-03 |
|  | `+ fail(): void` | Cổng báo thất bại | UC-PAY-03 |
|  | `+ expire(): void` | Hết hạn 15 phút | UC-ORD-08 |
| `Order` | `- transitionTo(next: OrderStatus): void` | Bảo vệ máy trạng thái theo bảng ALLOWED (Document_Vi.md 3.4.2) | — |
|  | `- isCod(): boolean` | Đơn thanh toán khi nhận hàng | — |
|  | `+ markPaid(now: OffsetDateTime): void` | Xác nhận đã thanh toán (online qua IPN / COD sau khi GHN đối soát tiền thu hộ) | UC-PAY-03 |
|  | `+ markPaymentFailed(): void` | Đồng bộ khi PaymentTransaction thất bại | UC-PAY-03 |
|  | `+ confirmByVendor(): void` | Shop xác nhận đơn (online đã PAID / COD chưa thu tiền) | UC-LOG-01 |
|  | `+ startPreparing(): void` | Bắt đầu đóng gói | UC-LOG-01 |
|  | `+ readyForPickup(): void` | In nhãn, chờ lấy hàng | UC-LOG-02 |
|  | `+ startDelivering(): void` | GHN đã lấy hàng (webhook picked) | UC-LOG-03 |
|  | `+ markDelivered(now: OffsetDateTime): void` | Giao thành công (webhook delivered) | UC-LOG-03 |
|  | `+ markCustomerConfirmed(now: OffsetDateTime): void` | Khách bấm "Đã nhận được hàng" (hoặc Cron tự xác nhận) | UC-ORD-11 |
|  | `+ markDeliveryFailed(): void` | GHN báo một lần giao thất bại | UC-LOG-03 |
|  | `+ retryDelivery(): void` | GHN giao lại (tổng tối đa 3 lần giao theo chính sách GHN) | UC-LOG-03 |
|  | `+ startReturning(): void` | GHN hoàn hàng về shop sau khi giao thất bại | UC-LOG-03 |
|  | `+ markReturnedToVendor(): void` | Shop đã nhận lại hàng hoàn | UC-LOG-03 |
|  | `+ cancel(cancelType: OrderStatus): void` | Hủy đơn | UC-ORD-10 |
|  | `+ requestReturn(): void` | Khách mở yêu cầu trả hàng (đồng bộ RefundRequest.REQUESTED) | UC-SUP-01 |
|  | `+ approveReturn(): void` | Đồng bộ khi RefundRequest → APPROVED | UC-SUP-02 |
|  | `+ rejectReturn(): void` | Đồng bộ khi RefundRequest → REJECTED | UC-SUP-03 |
|  | `+ markRefunded(amount: BigDecimal): void` | Đồng bộ khi RefundRequest → REFUNDED; cộng dồn refundedAmount: đủ grandTotal → REFUNDED, chưa đủ → PARTIALLY_REFUNDED (hoàn nhiều đợt được) | UC-SUP-04 |
|  | `+ netPaidAmount(): BigDecimal` | Số tiền khách đã trả còn nằm lại ở đơn (sau các lần hoàn) | UC-SUP-04 |
|  | `+ complete(now: OffsetDateTime): void` | Hoàn tất (khách xác nhận hoặc tự động sau 7 ngày) | UC-ORD-12 |
|  | `+ recalculateGrandTotal(): BigDecimal` | Tính lại tổng thanh toán (không âm) | UC-PRO-03 |
| `OrderDiscount` | `+ isPlatformFunded(): boolean` | Voucher do sàn tài trợ (không trừ tiền shop) | UC-PRO-03 |
| `OrderItem` | `+ applyCommission(rate: BigDecimal): void` | Chụp tỷ lệ & tính hoa hồng dòng hàng | UC-PAY-05 |
|  | `+ allocateDiscount(prorated: BigDecimal): void` | Gán phần chiết khấu phân bổ (phần dư dồn dòng cuối) | UC-PRO-03 |
| `StockReservation` | `+ isExpired(now: OffsetDateTime): boolean` | Đã quá hạn giữ chỗ | UC-ORD-08 |
|  | `+ confirm(): void` | Chốt khi thanh toán thành công | UC-PAY-03 |
|  | `+ release(now: OffsetDateTime, expired: boolean): boolean` | Nhả giữ chỗ (idempotent) | UC-ORD-08 |

## E.6. Nhóm 6 — Thanh toán & ký quỹ

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `EscrowLedger` | `+ fund(amount: BigDecimal, source: FundingSource, now: OffsetDateTime): void` | Nạp tiền vào ký quỹ | UC-PAY-05 |
|  | `- totalFunded(): BigDecimal` | Tổng đã nạp từ mọi nguồn | — |
|  | `+ isFullyFunded(): boolean` | Đã nhận đủ tiền đơn | UC-PAY-05, UC-LOG-03 |
|  | `+ scheduleRelease(deliveredAt: OffsetDateTime): void` | Đặt mốc giải ngân 7 ngày sau DELIVERED | UC-ORD-12 |
|  | `+ isReleasable(now: OffsetDateTime): boolean` | Đủ điều kiện giải ngân (HELD, đủ tiền, qua 7 ngày) | UC-PAY-07 |
|  | `+ release(now: OffsetDateTime): BigDecimal` | Giải ngân cho shop | UC-PAY-07 |
|  | `+ dispute(): void` | Khóa tranh chấp khi khách gửi yêu cầu hoàn tiền (kể cả COD chưa đối soát) | UC-SUP-01, UC-SUP-03 |
|  | `+ releaseDisputeHold(): void` | Sàn bác khiếu nại → quay về HELD (hoặc PENDING_FUNDING nếu COD chưa nộp) | UC-SUP-03 |
|  | `+ applyPartialRefund(refundGross: BigDecimal, refundCommission: BigDecimal): void` | Hoàn một phần: giảm grossAmount và commissionFee đúng bằng phần của các dòng bị trả (refundCommission = Σ OrderItem.commissionAmount × tỷ lệ số lượng trả), tính lại netAmount (khách + shop + sàn = tiền gốc); sau đó releaseDisputeHold() | UC-SUP-04 |
|  | `+ markRefunded(): void` | Hoàn tiền cho khách từ ký quỹ | UC-SUP-04 |
|  | `+ freeze(): void` | Admin phong tỏa tạm khi nghi ngờ gian lận (xử lý thủ công) | UC-SUP-03 |
| `FinancialLedger` | `+ {static} entry(type: FinancialEventType, account: String, debit: BigDecimal, credit: BigDecimal, referenceId: String): FinancialLedger` | Tạo bút toán (bảng chỉ INSERT, cấm UPDATE/DELETE) | UC-PAY-06 |
| `CodRemittance` | `+ confirm(fileName: String, lines: String, staffId: Long, now: OffsetDateTime): void` | Xác nhận đối soát COD khi bảng kê cân (TLCN v1.3) | UC-LOG-03 |
| `PaymentRefund` | `+ markSuccess(gatewayRefundId: String): void` | Cổng xác nhận hoàn tiền | UC-SUP-04 |
|  | `+ markFailed(): void` | Hoàn tiền thất bại | UC-SUP-04 |
| `PaymentTransaction` | `+ markSuccess(gatewayTxId: String, payload: String): void` | Ghi nhận IPN thành công (idempotent) | UC-PAY-03 |
|  | `+ markFailed(payload: String): void` | Ghi nhận thất bại (service gọi Order.markPaymentFailed) | UC-PAY-03 |
|  | `+ refundableAmount(): BigDecimal` | Số tiền còn có thể hoàn | UC-SUP-04 |
|  | `+ recordRefund(amt: BigDecimal): void` | Cập nhật sau khi hoàn qua cổng | UC-SUP-04 |
| `VendorWallet` | `+ availableBalance(): BigDecimal` | Số dư khả dụng = số dư − đang chờ rút | UC-ADM-04 |
|  | `+ credit(amount: BigDecimal): void` | Cộng tiền (giải ngân Escrow, nạp ví) | UC-PAY-07 |
|  | `+ debit(amount: BigDecimal): void` | Trừ tiền (rút tiền, thu hồi khi hoàn tiền sau giải ngân, điều chỉnh giảm) | UC-ADM-04 |
|  | `+ holdForWithdrawal(amount: BigDecimal): void` | Giữ tiền khi tạo lệnh rút | UC-ADM-04 |
|  | `+ settleWithdrawal(amount: BigDecimal, approved: boolean): void` | Chốt lệnh rút (chi tiền hoặc trả lại số dư) | UC-ADM-05 |
|  | `+ verifyHash(expectedHmac: String): boolean` | Phát hiện sửa số dư trực tiếp CSDL (so HMAC hằng thời gian) | UC-PAY-07 |
| `WalletTransaction` | `+ {static} debit(wallet: VendorWallet, type: WalletTransactionType, amount: BigDecimal, refType: WalletReferenceType, reference: String): WalletTransaction` | Tạo giao dịch trừ ví (append-only, gọi sau khi ví đã trừ; balanceBefore = số dư sau + số trừ) | UC-ADM-04 |
|  | `+ {static} credit(wallet: VendorWallet, type: WalletTransactionType, amount: BigDecimal, refType: WalletReferenceType, reference: String): WalletTransaction` | Tạo giao dịch cộng ví (append-only, gọi sau khi ví đã cộng) | UC-PAY-07 |
|  | `+ isConsistent(): boolean` | Kiểm tra bất biến sổ ví (job đối soát đêm) | UC-PAY-07 |
| `WithdrawalRequest` | `+ approve(adminId: Long, now: OffsetDateTime): void` | Nhân viên sàn phê duyệt lệnh rút (MVP duyệt một bước) | UC-ADM-05 |
|  | `+ startProcessing(): void` | Đã gửi lệnh chuyển khoản sang ngân hàng | UC-ADM-05 |
|  | `+ complete(walletTransactionId: Long): void` | Ngân hàng xác nhận đã chuyển | UC-ADM-05 |
|  | `+ reject(reason: String, adminId: Long, now: OffsetDateTime): void` | Từ chối, trả tiền về số dư | UC-ADM-05 |

## E.7. Nhóm 7 — Vận chuyển

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `ShipmentEvent` | `+ isDuplicateOf(other: ShipmentEvent): boolean` | Chống ghi trùng webhook | UC-LOG-03 |
| `ShipmentTracking` | `+ applyCarrierStatus(newStatus: ShipmentTrackingStatus, at: OffsetDateTime): void` | Cập nhật trạng thái từ webhook 3PL đã chuẩn hóa | UC-LOG-03 |
|  | `+ isFinal(): boolean` | Trạng thái kết thúc | UC-ORD-09 |
|  | `+ {static} forReturn(orderId: Long, refundRequestId: Long, carrier: CarrierType): ShipmentTracking` | Tạo vận đơn chiều về cho yêu cầu trả hàng đã duyệt | UC-SUP-02 |
| `VendorShippingConfig` | `+ isFreeshipFor(orderValue: BigDecimal): boolean` | Đơn đạt ngưỡng shop miễn phí ship | UC-ORD-06 |
|  | `+ supports(carrier: CarrierType): boolean` | Shop có bật hãng vận chuyển này không | UC-ORD-06 |

## E.8. Nhóm 8 — Hoàn tiền / khiếu nại

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `RefundEvidence` | `+ isVideo(): boolean` | Bằng chứng là video | UC-SUP-01 |
|  | `+ isAdditional(): boolean` | Bằng chứng nộp bổ sung theo yêu cầu của shop | UC-SUP-01 |
| `RefundRequest` | `+ approveByShop(mode: RefundResolution, note: String, now: OffsetDateTime): void` | Shop đồng ý: hoàn tiền ngay (APPROVE_REFUND_ONLY) hoặc cho trả hàng (APPROVE_WITH_RETURN → chờ hàng hoàn) | UC-SUP-02 |
|  | `- applyApproval(mode: RefundResolution, now: OffsetDateTime): void` | Áp phương án duyệt: REFUND_ONLY chỉ được duyệt hoàn ngay (shop muốn lấy lại hàng thì phải từ chối / nhờ sàn); RETURN_AND_REFUND được chọn cả hai | — |
|  | `+ shopReject(note: String, now: OffsetDateTime): void` | Shop từ chối | UC-SUP-02 |
|  | `+ requestMoreEvidence(note: String, now: OffsetDateTime): void` | Shop yêu cầu khách bổ sung bằng chứng (tạm dừng đồng hồ 48 giờ của shop) | UC-SUP-02 |
|  | `+ submitAdditionalEvidence(now: OffsetDateTime): void` | Khách đã tải bằng chứng bổ sung → quay về chờ shop | UC-SUP-01 |
|  | `+ closeIfEvidenceNotProvided(now: OffsetDateTime): boolean` | Cron: quá 48 giờ khách không bổ sung → đóng yêu cầu | UC-SUP-02 |
|  | `+ autoApproveIfShopSilent(now: OffsetDateTime): boolean` | Cron: quá shopResponseDueAt (48 giờ) shop không phản hồi → tự động chấp thuận theo đúng phương án khách đã chọn | UC-SUP-02 |
|  | `+ markReturnShipped(now: OffsetDateTime): void` | ĐVVC đã lấy hàng hoàn từ khách (vận đơn RETURN → PICKED_UP) | UC-SUP-02 |
|  | `+ confirmReturnReceived(now: OffsetDateTime): void` | Hàng hoàn đã về kho shop (vận đơn RETURN → DELIVERED) → sẵn sàng hoàn tiền | UC-SUP-02 |
|  | `+ failReturnIfOverdue(now: OffsetDateTime): boolean` | Cron: quá returnDueAt (7 ngày) khách chưa giao hàng hoàn cho ĐVVC → RETURN_FAILED, đóng tranh chấp, giải ngân bình thường | UC-SUP-02 |
|  | `+ escalateToDispute(): void` | Khách khiếu nại lên sàn (trong 48 giờ sau khi shop từ chối / không đồng ý yêu cầu bổ sung) hoặc shop chủ động nhờ sàn | UC-SUP-03 |
|  | `+ closeIfCustomerSilent(now: OffsetDateTime): boolean` | Cron: quá 48 giờ khách không khiếu nại → đóng yêu cầu (không hoàn tiền) | UC-SUP-02 |
|  | `+ approve(staffId: Long, mode: RefundResolution, note: String, now: OffsetDateTime): void` | Sàn phân xử chấp thuận hoàn tiền (chọn có / không thu hồi hàng) | UC-SUP-03 |
|  | `+ reject(staffId: Long, note: String, now: OffsetDateTime): void` | Sàn bác khiếu nại | UC-SUP-03 |
|  | `+ markRefunded(): void` | Đã hoàn đúng nguồn (chỉ một lần) | UC-SUP-04 |
|  | `- requireStatus(s: RefundStatus): void` | Bảo vệ máy trạng thái | — |
| `RefundRequestItem` | `+ amount(): BigDecimal` | Số tiền hoàn của dòng (đã trừ chiết khấu phân bổ) | UC-SUP-01 |

## E.9. Nhóm 9 — Đánh giá & tương tác shop

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `ProductReview` | `+ reply(text: String, now: OffsetDateTime): void` | Shop phản hồi đánh giá | UC-SOC-04 |
|  | `+ hide(): void` | Ẩn đánh giá vi phạm (do con người quyết định) | UC-SOC-03 |
|  | `+ adjustHelpful(delta: int): void` | Cộng / trừ bộ đếm "Hữu ích" khi thêm / xóa ReviewHelpfulVote | UC-SOC-03 |
|  | `+ attachSentiment(aspectJson: String): void` | Lưu kết quả phân tích cảm xúc PhoBERT (TLCN: khóa `overall`) | UC-AI-07 |
| `ReviewHelpfulVote` | `+ {static} of(reviewId: Long, userId: Long, reviewAuthorId: Long, now: OffsetDateTime): ReviewHelpfulVote` | Bấm "Hữu ích" (bấm lần 2 = xóa dòng và trừ helpfulCount) | UC-SOC-03 |
| `ReviewReport` | `+ {static} of(reviewId: Long, reporterId: Long, reason: ReviewReportReason, note: String, now: OffsetDateTime): ReviewReport` | Báo cáo đánh giá vi phạm | UC-SOC-05 |
| `ShopFollow` | `+ {static} of(customerId: Long, vendorId: Long, now: OffsetDateTime): ShopFollow` | Theo dõi shop (UNIQUE customer+vendor) | UC-SOC-06 |

## E.10. Nhóm 10 — Thông báo & nội dung sàn

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `AdminNotification` | `+ markRead(): void` | Admin đã xem cảnh báo | UC-NOT-01 |
| `HomeBanner` | `+ isLive(now: OffsetDateTime): boolean` | Đang hiển thị (bật và trong khung giờ) | UC-ADM-12 |
|  | `+ deactivate(): void` | Tắt khẩn cấp | UC-ADM-12 |
|  | `+ recordStats(impressions: long, clicks: long): void` | Ghi bền bộ đếm từ Redis (GETDEL) | UC-ADM-12 |
|  | `+ ctr(): double` | Tỷ lệ nhấp CTR | UC-ADM-12 |
| `Notification` | `+ markSent(now: OffsetDateTime): void` | Đã gửi qua kênh | UC-NOT-01 |
|  | `+ markFailed(): void` | Gửi lỗi | UC-NOT-03 |
|  | `+ markRead(now: OffsetDateTime): void` | Người dùng đã đọc | UC-NOT-01 |
| `PlatformNotification` | `+ incrementRead(): void` | Đếm lượt đọc broadcast | UC-NOT-05 |
| `UserPlatformNotificationRead` | `+ {static} of(userId: Long, notificationId: Long, now: OffsetDateTime): UserPlatformNotificationRead` | Đánh dấu đã đọc thông báo toàn sàn | UC-NOT-05 |

## E.11. Nhóm 11 — Nhắn tin (chat)

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `Conversation` | `+ onNewMessage(m: Message, fromCustomer: boolean): void` | Cập nhật tóm tắt & bộ đếm chưa đọc | UC-SOC-01 |
|  | `+ markReadBy(customer: boolean): void` | Đặt lại badge chưa đọc | UC-SOC-01 |
| `Message` | `+ markRead(): void` | Tin đã đọc | UC-SOC-01 |

## E.12. Nhóm 12 — Nhật ký hệ thống

| Entity | Phương thức (UML) | Ý nghĩa nghiệp vụ | Use Case |
|---|---|---|---|
| `SystemLog` | `+ prePersist(): void` |  | — |
|  | `+ {static} of(userId: Long, userEmail: String, userRole: String, action: AuditAction, referenceId: String, details: String, ip: String, success: boolean): SystemLog` | Tạo bản ghi kiểm toán bất biến | UC-ADM-03 |

---

## CHANGELOG

### TLCN v1.5 — 2026-11-18 (Mở shop & KYC — `V20`)
- Nhóm 1: `VendorProfile` thêm 9 cột (`shopNameChangedAt`, `contactPhone`, `contactEmail`, `kycDocumentsMetaJson`, `kycBiometricConsent`, `kycSupplementDocumentsJson`, `kycSupplementDeadlineAt`, `kycManualReviewRequestedAt`, `pickupPhone`) và 17 phương thức (`register`, `saveKycDraft`, `attachKycDocuments`, `requestManualReview`, `requeueForReview`, `expireSupplement`, `renameShop`, `updatePickup`, `setPayoutAccount`, `clearPayoutAccount`…); `submitKyc` / `approveKyc` / `rejectKyc` siết trạng thái nguồn; `requestSupplement` nhận danh sách giấy tờ + hạn 7 ngày. Ảnh giấy tờ (`idFrontUrl`…) lưu **khóa đối tượng đã mã hóa AES-256-GCM** (`kyc-dossiers/vendor/{id}/…enc`), không phải URL.
- Phụ lục A: `AuditAction` + `KYC_SUPPLEMENT_REQUESTED`, `KYC_REQUEUED`, `KYC_DOCUMENT_VIEWED`, `PAYOUT_ACCOUNT_CHANGED`. Mục 13.1: 3 index `vendor_profiles`. Plan `2026-11-16/server-spring/plan_mo_shop_kyc.md` D2, D3; ADR 0011 trong repo.

### TLCN v1.4 — 2026-11-16 (đồng bộ với mã nguồn)
- Nhóm 1: `User` thêm `deletionRequestedAt` + 12 phương thức AUTH (`hasPassword`, `changePassword`, `verifyEmail`, `changeEmail`, `linkIdentity`, `unlinkIdentity`, `lockFor`, `clearLock`, `requestDeletion`, `cancelDeletion`, `isPendingDeletion`, `newCustomer`); `anonymize()` xóa cả `oauthProvider`; `RefreshToken` thêm `familyId`, `lastUsedAt`, `touch()`, `issue()`; `revokedReason` theo hằng `RevokeReason`. `AccountStatus.LOCKED` để dành (khóa tạm = `lockedUntil`).
- Phụ lục A: `AuditAction` 22 → 30 giá trị. Mục 13.1: 5 index của `V14`. Mục 13.2: tên migration thật `V1…V14`, seed theo `R__seed_demo.sql`.
- Kho tệp MinIO → Silo (đổi tên trong chú thích; tên cột / bucket không đổi).

### TLCN v1.2 — 2026-11-06 (đối chiếu giao diện Genie)

- Đổi thương hiệu Lazadee → Genie: package `com.genie.platform`, `CarrierType.GENIE_EXPRESS`, `VendorShippingConfig.genieExpressEnabled` (`genie_express_enabled`), `FinancialEventType.COIN_COST`, cột `coinDiscount` / `coinDiscountAmount` / `coinUsedCount` (`coin_discount`, `coin_discount_amount`, `coin_used_count`); Xu → Điểm Genie.
- `ProductReview` thêm `aspectSentimentJson` (jsonb, NULLABLE) và `attachSentiment()` — lấy đúng tên của DATN, TLCN chỉ dùng khóa `overall` (cảm xúc tổng thể PhoBERT, UC-AI-07); `product_reviews` 15 cột.
- `BannerPosition` thêm `HOME_TOP_SIDE` (hai ô cạnh băng chuyền); `HomeBanner.targetUrl` ghi rõ 6 loại đích.
- Rà bội số `[0..1]` với chú thích NULLABLE: cột `version` (10 entity) và các cờ / FK có mặc định thêm `nullable = false`; tham chiếu `@ManyToOne` / `@OneToOne` theo FK bắt buộc thêm `optional = false`; bổ sung chú thích NULLABLE cho thuộc tính được phép rỗng (`User.password` — tài khoản Google / Apple, `EscrowLedger.fundedAt`, `SystemLog.ipAddress`…); `PlatformNotification.readCount` đánh dấu dẫn xuất. Java (`java-code/`) và `Genie_ClassDiagram_UML25.xmi` cập nhật theo.

### TLCN v1.1 — 2026-10-29 (đối chiếu giao diện client + prototype)
- Thêm **26 entity** (38 → 64): 16 entity đưa lại từ DATN (`StaffPermission`, `CategoryAttribute`, 4 bảng phạm vi voucher, `FlashSaleCampaign`, `FlashSaleItem`, `UserCoin`, `CoinTransaction`, `FinancialLedger`, `ShopFollow`, `ReviewReport`, `PlatformNotification`, `UserPlatformNotificationRead`, `HomeBanner`), 3 entity lấy từ DATN theo nhu cầu nghiệp vụ / mã nguồn cũ (`VendorShippingConfig` — hãng vận chuyển & ngưỡng freeship của shop, `DailyCheckin` — chuỗi điểm danh, `AdminNotification` — chuông thông báo Admin đang dùng ở `server/`) và 7 entity mới (`Brand`, `ProductDocument`, `CategoryProposal`, `ProductModerationResult`, `SearchKeywordStat`, `ProductViewStat`, `ReviewHelpfulVote` — thêm cả DATN).
- Thêm trường: đăng nhập Google / Apple, ẩn danh hóa, chức danh nhân viên, chỉ số trang shop, chat tự động, tạm nghỉ, đặt trước, Hữu ích của đánh giá, thương hiệu, tình trạng hàng, lượt xem, lời nhắn cho shop, Điểm, đồng tài trợ voucher. Dấu hiệu quốc huy / vân tay của KYC ghi trong `KycResult.riskSignalsJson` (không thêm trường riêng — giống DATN v2.10).
- Enum: 52 (thêm 3 tầng voucher, 13 quyền nhân viên, 3 chức danh, sự kiện sổ cái, loại Điểm, vị trí banner…); quan hệ: 100; sơ đồ lớp: 7 → 10.
- Sinh lại `java-code/` và `Genie_ClassDiagram_UML25.xmi` từ tài liệu này.

### MVP v1.0 — 2026-10-28
- Tạo bản MVP từ bản đầy đủ v2.15: giữ 38/162 entity trong 12 nhóm, rút enum (chỉ cắt giá trị, không đổi tên).
