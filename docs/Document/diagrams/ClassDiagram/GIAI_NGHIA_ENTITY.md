# GIẢI NGHĨA ENTITY — BẢN TLCN
## Đọc hiểu từng bảng, từng trường của `ENTITY.md` bằng tiếng Việt

> **Dành cho:** thành viên nhóm mới đọc mô hình dữ liệu Genie bản TLCN — không cần biết trước Java / JPA.
> **Nguồn:** sinh tự động từ [ENTITY.md](./ENTITY.md) (65 entity, 744 trường, 55 enum — TLCN v1.3). Khi ENTITY.md đổi, chạy lại script để file này khớp theo.
> **Cách đọc mỗi bảng:** cột *Tên trường* là tên trong code (tiếng Anh, viết kiểu camelCase); *Đọc là* là nghĩa tiếng Việt của tên đó; *Kiểu* là loại dữ liệu; *Bắt buộc* cho biết có được bỏ trống không; *Ý nghĩa & cách dùng* giải thích trường dùng để làm gì.

---

## 0. Kiến thức nền cần biết trước

### 0.1. Entity, bảng, trường là gì?

- **Entity** (thực thể) = một loại đối tượng nghiệp vụ, ví dụ *Người dùng*, *Sản phẩm*, *Đơn hàng*. Mỗi entity là **một bảng** trong cơ sở dữ liệu PostgreSQL và **một class** trong code Java.
- **Trường** (field / cột) = một ô thông tin của entity, ví dụ `email` của người dùng. Trong code viết `camelCase` (`createdAt`), trong CSDL tự đổi thành `snake_case` (`created_at`).
- **Dòng** (bản ghi) = một đối tượng cụ thể, ví dụ người dùng có email `an@gmail.com`.

### 0.2. Quy ước đặt tên — nhìn đuôi là đoán được nghĩa

| Mẫu tên | Nghĩa | Ví dụ |
|---|---|---|
| `id` | Mã định danh (khóa chính) — số duy nhất của mỗi dòng, CSDL tự tăng | `User.id = 15` |
| `…Id` | Mã của đối tượng **khác** mà dòng này trỏ tới (khóa ngoại) | `Order.customerId` = mã người mua |
| `…At` | **Thời điểm** xảy ra việc gì đó (ngày + giờ) | `createdAt` = lúc tạo, `deliveredAt` = lúc giao xong |
| `is…` | Câu hỏi có / không (đúng / sai) | `isActive` = có đang bật không |
| `…Count` | Bộ đếm số lượng | `followerCount` = số người theo dõi |
| `…Amount`, `…Fee`, `…Price`, `…Total` | Số tiền (VND) | `grandTotal` = tổng khách phải trả |
| `…Rate` | Tỷ lệ, lưu dạng thập phân | `commissionRate = 0.0500` = 5% |
| `…Url` | Đường dẫn tới ảnh / tệp (thường nằm trên Silo) | `avatarUrl` |
| `…Json` | Một cột chứa dữ liệu dạng JSON (nhiều giá trị có cấu trúc) | `specsJson` = bảng thông số sản phẩm |
| `…Snapshot` | **Ảnh chụp dữ liệu** tại thời điểm giao dịch — sau này dữ liệu gốc đổi thì giá trị này vẫn giữ nguyên | `productNameSnapshot` |
| `…Hash` | Mã băm (chuỗi mã hóa một chiều) để bảo mật hoặc chống sửa trái phép | `tokenHash`, `balanceHash` |
| `version` | Số phiên bản dùng **khóa lạc quan**: chống hai người sửa cùng lúc | — |
| `status`, `type` | Trạng thái / loại, chỉ nhận giá trị trong một **danh sách cố định (enum)** | `Order.status = DELIVERED` |

### 0.3. Các kiểu dữ liệu

| Kiểu ghi trong bảng | Nghĩa |
|---|---|
| số nguyên / số nguyên lớn | Số không có phần lẻ (`Integer` / `Long`). Mã ID luôn là số nguyên lớn. |
| số tiền (DECIMAL) | `BigDecimal` — số thập phân chính xác tuyệt đối, dùng cho tiền. **Không** dùng `double` vì cộng dồn sẽ lệch vài đồng. |
| tỷ lệ (vd 0.0500 = 5%) | Phần trăm lưu dạng thập phân 4 chữ số lẻ. |
| chuỗi ≤ N ký tự / văn bản dài | Chữ; *văn bản dài* là không giới hạn độ dài (`TEXT`). |
| đúng / sai | `Boolean` — chỉ có `true` (đúng) hoặc `false` (sai). |
| ngày + giờ (có múi giờ) | `OffsetDateTime` — thời điểm chính xác tới micro-giây, lưu chuẩn UTC. |
| ngày (không có giờ) | `LocalDate` — chỉ ngày, ví dụ ngày sinh. |
| JSON (JSONB) | Một cột chứa dữ liệu có cấu trúc, ví dụ `{"Màu":"Đen","Size":"L"}`. |
| danh sách cố định `TênEnum` | Chỉ nhận một trong các giá trị liệt kê sẵn — xem Phụ lục cuối file. |
| danh sách / đối tượng (quan hệ) | Không phải cột: là đường liên kết để code lấy dữ liệu bảng khác. |

### 0.4. Cột *Bắt buộc*

- **Có** — phải có giá trị khi lưu (CSDL chặn nếu bỏ trống).
- **Có (có mặc định)** — bắt buộc nhưng hệ thống tự điền giá trị ban đầu nếu không nhập.
- **Không** — được để trống (`NULL`), thường vì chưa xảy ra (ví dụ `deliveredAt` khi đơn chưa giao) hoặc không phải ai cũng có.
- **—** — không phải cột (đường liên kết quan hệ).

---

## Mục lục

- **Nhóm 1 — Người dùng & định danh:** [User](#user), [CustomerProfile](#customerprofile), [Address](#address), [VendorProfile](#vendorprofile), [AdminProfile](#adminprofile), [KycResult](#kycresult), [RefreshToken](#refreshtoken), [StaffPermission](#staffpermission)
- **Nhóm 2 — Danh mục & sản phẩm:** [Category](#category), [Product](#product), [ProductVariant](#productvariant), [ProductImage](#productimage), [UserWishlist](#userwishlist), [CategoryAttribute](#categoryattribute), [Brand](#brand), [ProductDocument](#productdocument), [CategoryProposal](#categoryproposal), [ProductModerationResult](#productmoderationresult), [SearchKeywordStat](#searchkeywordstat), [ProductViewStat](#productviewstat)
- **Nhóm 3 — Giỏ hàng:** [Cart](#cart), [CartItem](#cartitem)
- **Nhóm 4 — Khuyến mãi, voucher, flash sale & điểm:** [Voucher](#voucher), [VoucherUsage](#voucherusage), [UserVoucher](#uservoucher), [VoucherCategory](#vouchercategory), [VoucherProduct](#voucherproduct), [VoucherExcludedCategory](#voucherexcludedcategory), [VoucherExcludedProduct](#voucherexcludedproduct), [FlashSaleCampaign](#flashsalecampaign), [FlashSaleItem](#flashsaleitem), [UserCoin](#usercoin), [CoinTransaction](#cointransaction), [DailyCheckin](#dailycheckin)
- **Nhóm 5 — Đơn hàng:** [CheckoutSession](#checkoutsession), [Order](#order), [OrderItem](#orderitem), [OrderDiscount](#orderdiscount), [StockReservation](#stockreservation)
- **Nhóm 6 — Thanh toán & ký quỹ:** [PaymentTransaction](#paymenttransaction), [PaymentRefund](#paymentrefund), [EscrowLedger](#escrowledger), [VendorWallet](#vendorwallet), [WalletTransaction](#wallettransaction), [WithdrawalRequest](#withdrawalrequest), [FinancialLedger](#financialledger)
- **Nhóm 7 — Vận chuyển:** [ShipmentTracking](#shipmenttracking), [ShipmentEvent](#shipmentevent), [VendorShippingConfig](#vendorshippingconfig)
- **Nhóm 8 — Hoàn tiền / khiếu nại:** [RefundRequest](#refundrequest), [RefundRequestItem](#refundrequestitem), [RefundEvidence](#refundevidence)
- **Nhóm 9 — Đánh giá & tương tác shop:** [ProductReview](#productreview), [ShopFollow](#shopfollow), [ReviewReport](#reviewreport), [ReviewHelpfulVote](#reviewhelpfulvote)
- **Nhóm 10 — Thông báo & nội dung sàn:** [Notification](#notification), [PlatformNotification](#platformnotification), [UserPlatformNotificationRead](#userplatformnotificationread), [HomeBanner](#homebanner), [AdminNotification](#adminnotification)
- **Nhóm 11 — Nhắn tin (chat):** [Conversation](#conversation), [Message](#message)
- **Nhóm 12 — Nhật ký hệ thống:** [SystemLog](#systemlog)
- [Phụ lục — Giải nghĩa các danh sách cố định (enum)](#phu-luc-enum)

---

# Nhóm 1 — Người dùng & định danh

<a id="user"></a>
## 1.1 `User` — Tài khoản đăng nhập

- **Đọc là:** người dùng
- **Bảng trong CSDL:** `users`
- **Nằm ở sơ đồ lớp:** Sơ đồ 1 — Định danh, Hồ sơ người dùng, KYC & Phân quyền nhân viên (Nhóm 1)
- **Liên kết với bảng khác:**
  - Mỗi `User` **sở hữu** 0 hoặc 1 `CustomerProfile`; `CustomerProfile` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `CustomerProfile.userId`).
  - Mỗi `User` **sở hữu** nhiều (0..n) `Address`; `Address` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `Address.userId`).
  - Mỗi `User` **sở hữu** 0 hoặc 1 `VendorProfile`; `VendorProfile` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `VendorProfile.userId`).
  - Mỗi `User` **sở hữu** 0 hoặc 1 `AdminProfile`; `AdminProfile` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `AdminProfile.userId`).
  - Mỗi `User` **sở hữu** nhiều (0..n) `RefreshToken`; `RefreshToken` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `RefreshToken.userId`).
  - Mỗi `StaffPermission` **trỏ tới** đúng 1 `User` (qua cột `StaffPermission.grantedByAdminId`).
  - Mỗi `User` **sở hữu** nhiều (0..n) `StaffPermission`; `StaffPermission` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `StaffPermission.staffUserId`).
  - Mỗi `User` **sở hữu** nhiều (0..n) `UserWishlist`; `UserWishlist` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `UserWishlist.userId`).
  - Mỗi `CategoryProposal` **trỏ tới** đúng 1 `User` (qua cột `CategoryProposal.vendorId`) — vai trò *vendor*.
  - Mỗi `ProductViewStat` **trỏ tới** đúng 1 `User` (qua cột `ProductViewStat.vendorId`) — vai trò *vendor*.
  - Mỗi `User` **sở hữu** 0 hoặc 1 `Cart`; `Cart` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `Cart.userId`).
  - Mỗi `VoucherUsage` **trỏ tới** đúng 1 `User` (qua cột `VoucherUsage.userId`).
  - Mỗi `UserVoucher` **trỏ tới** đúng 1 `User` (qua cột `UserVoucher.userId`).
  - Mỗi `FlashSaleCampaign` **trỏ tới** 0 hoặc 1 `User` (qua cột `FlashSaleCampaign.vendorId`) — vai trò *vendor*.
  - Mỗi `User` **sở hữu** 0 hoặc 1 `UserCoin`; `UserCoin` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `UserCoin.userId`).
  - Mỗi `User` **sở hữu** nhiều (0..n) `CoinTransaction`; `CoinTransaction` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `CoinTransaction.userId`).
  - Mỗi `User` **sở hữu** 0 hoặc 1 `DailyCheckin`; `DailyCheckin` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `DailyCheckin.customerId`).
  - Mỗi `CheckoutSession` **trỏ tới** đúng 1 `User` (qua cột `CheckoutSession.userId`).
  - Mỗi `Order` **trỏ tới** đúng 1 `User` (qua cột `Order.customerId`).
  - Mỗi `RefundRequest` **trỏ tới** đúng 1 `User` (qua cột `RefundRequest.requestedByUserId`).
  - Mỗi `ProductReview` **trỏ tới** đúng 1 `User` (qua cột `ProductReview.customerId`).
  - Mỗi `ProductReview` **trỏ tới** đúng 1 `User` (qua cột `ProductReview.vendorId`) — vai trò *vendor*.
  - Mỗi `ShopFollow` **trỏ tới** đúng 1 `User` (qua cột `ShopFollow.customerId`) — vai trò *customer*.
  - Mỗi `ShopFollow` **trỏ tới** đúng 1 `User` (qua cột `ShopFollow.vendorId`) — vai trò *vendor*.
  - Mỗi `ReviewReport` **trỏ tới** đúng 1 `User` (qua cột `ReviewReport.reporterId`).
  - Mỗi `ReviewHelpfulVote` **trỏ tới** đúng 1 `User` (qua cột `ReviewHelpfulVote.userId`).
  - Mỗi `User` **sở hữu** nhiều (0..n) `Notification`; `Notification` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `Notification.userId`).
  - Mỗi `UserPlatformNotificationRead` **trỏ tới** đúng 1 `User` (qua cột `UserPlatformNotificationRead.userId`).
  - Mỗi `Conversation` **trỏ tới** đúng 1 `User` (qua cột `Conversation.customerId`) — vai trò *customer*.
  - Mỗi `Message` **trỏ tới** đúng 1 `User` (qua cột `Message.senderId`) — vai trò *sender*.

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính, ID định danh duy nhất của tài khoản. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `email` | email | chuỗi ≤ 255 ký tự | Có | Email đăng nhập — DUY NHẤT toàn hệ thống, dùng làm username. Giá trị **không được trùng** giữa các dòng, **không phân biệt hoa thường** (luôn lưu chữ thường; index `uk_users_email_lower` — V14). |
| `password` | mật khẩu | chuỗi ≤ 255 ký tự | Không | Mật khẩu đã băm (Argon2id chuẩn OWASP 2024, `m = 19456 KiB, t = 2, p = 1`). NULL khi tài khoản tạo bằng Google / Apple và chưa "Thiết lập mật khẩu" — đăng nhập bằng mật khẩu khi đó trả `GNE-AUTH-4012`. |
| `role` | vai trò | danh sách cố định `UserRole` | Có | 4 vai trò MVP: CUSTOMER, VENDOR, STAFF, ADMIN (Phụ lục A — nhóm 1). Spring Security cấp authority dạng ROLE_<giá trị>; RoleHierarchy: ROLE_VENDOR > ROLE_CUSTOMER, ROLE_ADMIN > ROLE_STAFF — chủ shop dùng cùng tài khoản để mua hàng ở shop khác. Giá trị: `CUSTOMER` (khách hàng), `VENDOR` (người bán (shop)), `STAFF` (nhân viên), `ADMIN` (quản trị viên). |
| `status` | trạng thái | danh sách cố định `AccountStatus` | Có | Trạng thái tài khoản: UNVERIFIED, ACTIVE, LOCKED, SUSPENDED, ANONYMIZED, DELETED. Thực tế TLCN: đăng ký thường tạo thẳng `ACTIVE`; `UNVERIFIED` chỉ cho nhân viên được mời chưa kích hoạt; khóa tạm do sai mật khẩu / OTP **không** đổi trạng thái (dùng `lockedUntil`) nên `LOCKED` để dành. Giá trị: `UNVERIFIED` (chưa xác thực email), `ACTIVE` (đang hoạt động), `LOCKED` (bị khóa), `SUSPENDED` (bị đình chỉ), `ANONYMIZED` (đã ẩn danh hóa), `DELETED` (đã xóa). |
| `emailVerifiedAt` | thời điểm xác thực email | ngày + giờ (có múi giờ) | Không | Thời điểm user bấm link xác thực email. NULL = chưa xác thực. |
| `failedLoginAttempts` | số lần đăng nhập sai liên tiếp | số nguyên | Có (có mặc định) | Đếm số lần nhập sai mật khẩu liên tiếp. Vượt ngưỡng → tự khoá. Mặc định: `0`. |
| `lockedUntil` | bị khóa đến | ngày + giờ (có múi giờ) | Không | Khoá tạm thời (sai mật khẩu hoặc sai OTP đăng nhập 5 lần → 15 phút) đến thời điểm này; trạng thái vẫn `ACTIVE`. Lý do khóa lưu Redis `auth:locked_by:{userId}`. |
| `suspendedUntil` | bị đình chỉ đến | ngày + giờ (có múi giờ) | Không | Bị admin đình chỉ đến thời điểm này (vi phạm chính sách). |
| `suspensionReason` | lý do đình chỉ | chuỗi ≤ 500 ký tự | Không | Lý do bị đình chỉ, hiển thị cho user biết. |
| `version` | số phiên bản (khóa lạc quan) | số nguyên lớn | Có | Optimistic Lock — chống 2 request cùng sửa 1 tài khoản. Mỗi lần sửa dòng, số này tăng 1; nếu hai người sửa cùng lúc, người sau bị chặn và phải tải lại (chống ghi đè, chống bán vượt / rút tiền 2 lần). |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo tài khoản. Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Thời điểm cập nhật gần nhất. Hệ thống tự ghi mỗi lần sửa. |
| `oauthProvider` | nhà cung cấp đăng nhập (Google / Apple) | chuỗi ≤ 30 ký tự | Không | Nhà cung cấp OAuth: "google", "apple". |
| `oauthProviderId` | mã người dùng bên Google / Apple | chuỗi ≤ 255 ký tự | Không | ID của user bên phía nhà cung cấp OAuth (claim `sub`, không đổi kể cả khi người dùng đổi email bên Google). Cặp (`oauthProvider`, `oauthProviderId`) **không được trùng** giữa các dòng (`uk_users_oauth`) — một tài khoản Google / Apple chỉ gắn với một user. |
| `deletionRequestedAt` | thời điểm yêu cầu xóa tài khoản | ngày + giờ (có múi giờ) | Không | *(Bổ sung V14)* Lúc khách yêu cầu xóa tài khoản (UC-AUTH-10). NULL = không có yêu cầu. Quá 30 ngày thì cron 02:30 ẩn danh tài khoản; trong 30 ngày khách hủy được. |
| `customerProfile` | hồ sơ khách hàng | đối tượng `CustomerProfile` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `CustomerProfile` liên quan (JPA nạp khi cần). Hồ sơ khách hàng (chỉ có khi role = CUSTOMER). |
| `vendorProfile` | hồ sơ người bán (shop) | đối tượng `VendorProfile` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `VendorProfile` liên quan (JPA nạp khi cần). Hồ sơ người bán (chỉ có khi role = VENDOR). |
| `adminProfile` | hồ sơ quản trị viên | đối tượng `AdminProfile` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `AdminProfile` liên quan (JPA nạp khi cần). Hồ sơ admin/staff (chỉ có khi role = ADMIN hoặc STAFF). |
| `addresses` | các địa chỉ | danh sách `Address` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Address` liên quan (JPA nạp khi cần). Danh sách địa chỉ giao hàng của user. |
| `refreshTokens` | các token làm mới | danh sách `RefreshToken` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `RefreshToken` liên quan (JPA nạp khi cần). Danh sách phiên đăng nhập đang hoạt động. |

<a id="customerprofile"></a>
## 1.2 `CustomerProfile` — Hồ sơ khách hàng

- **Đọc là:** hồ sơ khách hàng
- **Bảng trong CSDL:** `customer_profiles`
- **Nằm ở sơ đồ lớp:** Sơ đồ 1 — Định danh, Hồ sơ người dùng, KYC & Phân quyền nhân viên (Nhóm 1)
- **Liên kết với bảng khác:**
  - Mỗi `User` **sở hữu** 0 hoặc 1 `CustomerProfile`; `CustomerProfile` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `CustomerProfile.userId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id — UNIQUE nên quan hệ là 1-1. **Khóa ngoại** → bảng của `User`. Giá trị **không được trùng** giữa các dòng. |
| `fullName` | họ tên | chuỗi ≤ 255 ký tự | Có | Họ tên đầy đủ của khách hàng. |
| `phoneNumber` | số điện thoại | chuỗi ≤ 20 ký tự | Không | Số điện thoại liên hệ. |
| `avatarUrl` | link ảnh đại diện | chuỗi ≤ 500 ký tự | Không | Link ảnh đại diện. |
| `gender` | giới tính | danh sách cố định `Gender` | Không | MALE / FEMALE / OTHER / PREFER_NOT_TO_SAY. Giá trị: `MALE` (nam), `FEMALE` (nữ), `OTHER` (khác), `PREFER_NOT_TO_SAY` (không muốn tiết lộ). |
| `dateOfBirth` | ngày sinh | ngày (không có giờ) | Không | Ngày sinh (chỉ ngày, không giờ). |
| `bio` | giới thiệu bản thân | văn bản dài | Không | Giới thiệu bản thân. |
| `preferredLanguage` | ngôn ngữ ưa thích | chuỗi ≤ 5 ký tự | Không | Ngôn ngữ ưa thích: "vi", "en" (chuẩn ISO 639-1). |
| `defaultAddressId` | mã địa chỉ mặc định | số nguyên lớn | Không | FK mềm → addresses.id, địa chỉ giao hàng mặc định. Lưu mã của đối tượng khác nhưng không ràng buộc cứng ở CSDL (khóa ngoại mềm). |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo hồ sơ. Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Thời điểm cập nhật gần nhất. Hệ thống tự ghi mỗi lần sửa. |
| `anonymizedAt` | thời điểm ẩn danh hóa | ngày + giờ (có múi giờ) | Không | Thời điểm hồ sơ bị ẩn danh hoá (xoá tài khoản — UC-AUTH-10). |
| `user` | tài khoản người dùng | đối tượng `User` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `User` liên quan (JPA nạp khi cần). Tài khoản đăng nhập tương ứng. |

<a id="address"></a>
## 1.3 `Address` — Địa chỉ giao hàng

- **Đọc là:** địa chỉ
- **Bảng trong CSDL:** `addresses`
- **Nằm ở sơ đồ lớp:** Sơ đồ 1 — Định danh, Hồ sơ người dùng, KYC & Phân quyền nhân viên (Nhóm 1)
- **Liên kết với bảng khác:**
  - Mỗi `User` **sở hữu** nhiều (0..n) `Address`; `Address` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `Address.userId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id — địa chỉ này thuộc về ai. **Khóa ngoại** → bảng của `User`. |
| `recipientName` | tên người nhận | chuỗi ≤ 255 ký tự | Có | Tên người NHẬN hàng (có thể khác tên chủ tài khoản). |
| `phoneNumber` | số điện thoại | chuỗi ≤ 20 ký tự | Có | SĐT người nhận — shipper gọi số này. |
| `addressLine` | địa chỉ chi tiết | chuỗi ≤ 500 ký tự | Có | Địa chỉ chi tiết: số nhà, tên đường, toà nhà. |
| `ward` | tên phường / xã | chuỗi ≤ 100 ký tự | Có | TÊN phường/xã — dùng để HIỂN THỊ cho người đọc. |
| `wardCode` | mã phường / xã (GHN) | chuỗi ≤ 50 ký tự | Có | MÃ phường/xã theo chuẩn GHN — dùng để GỌI API tính phí ship. |
| `district` | tên quận / huyện | chuỗi ≤ 100 ký tự | Có | TÊN quận/huyện — để hiển thị. |
| `districtId` | mã quận / huyện (GHN) | số nguyên | Có | MÃ quận/huyện theo GHN — để gọi API. |
| `province` | tên tỉnh / thành | chuỗi ≤ 100 ký tự | Có | TÊN tỉnh/thành phố — để hiển thị. |
| `provinceId` | mã tỉnh / thành (GHN) | số nguyên | Có | MÃ tỉnh/thành theo GHN — để gọi API. |
| `isDefault` | là mặc định | đúng / sai | Không | True = địa chỉ mặc định khi thanh toán. |
| `label` | loại địa chỉ | danh sách cố định `AddressLabel` | Có | HOME (Nhà riêng) / OFFICE (Văn phòng) — nhãn hiện cạnh địa chỉ (UC-AUTH-09, TLCN v1.3). Mặc định: `HOME`. |
| `isDeleted` | đã xóa mềm chưa | đúng / sai | Có | Xoá mềm — true = user đã xoá, nhưng bản ghi vẫn giữ lại. Mặc định: `false`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo địa chỉ. Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Thời điểm cập nhật gần nhất. Hệ thống tự ghi mỗi lần sửa. |

<a id="vendorprofile"></a>
## 1.4 `VendorProfile` — Hồ sơ người bán (Shop)

- **Đọc là:** hồ sơ người bán (shop)
- **Bảng trong CSDL:** `vendor_profiles`
- **Nằm ở sơ đồ lớp:** Sơ đồ 1 — Định danh, Hồ sơ người dùng, KYC & Phân quyền nhân viên (Nhóm 1)
- **Liên kết với bảng khác:**
  - Mỗi `User` **sở hữu** 0 hoặc 1 `VendorProfile`; `VendorProfile` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `VendorProfile.userId`).
  - Mỗi `VendorProfile` **trỏ tới** 0 hoặc 1 `Category` (qua cột `VendorProfile.categoryId`).
  - Mỗi `VendorProfile` **sở hữu** 0 hoặc 1 `KycResult`; `KycResult` không tồn tại nếu thiếu `VendorProfile` (xóa cha thì xóa con) (qua cột `KycResult.vendorProfileId`).
  - Mỗi `Product` **trỏ tới** đúng 1 `VendorProfile` (qua cột `Product.vendorId`).
  - Mỗi `Voucher` **trỏ tới** 0 hoặc 1 `VendorProfile` (qua cột `Voucher.vendorId`).
  - Mỗi `Order` **trỏ tới** đúng 1 `VendorProfile` (qua cột `Order.vendorId`).
  - Mỗi `EscrowLedger` **trỏ tới** đúng 1 `VendorProfile` (qua cột `EscrowLedger.vendorId`).
  - Mỗi `VendorProfile` **sở hữu** 0 hoặc 1 `VendorWallet`; `VendorWallet` không tồn tại nếu thiếu `VendorProfile` (xóa cha thì xóa con) (qua cột `VendorWallet.vendorId`).
  - Mỗi `FinancialLedger` **trỏ tới** 0 hoặc 1 `VendorProfile` (qua cột `FinancialLedger.vendorId`).
  - Mỗi `VendorProfile` **sở hữu** 0 hoặc 1 `VendorShippingConfig`; `VendorShippingConfig` không tồn tại nếu thiếu `VendorProfile` (xóa cha thì xóa con) (qua cột `VendorShippingConfig.vendorId`).
  - Mỗi `Conversation` **trỏ tới** đúng 1 `VendorProfile` (qua cột `Conversation.vendorId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id — UNIQUE, 1 tài khoản chỉ mở được 1 shop. **Khóa ngoại** → bảng của `User`. Giá trị **không được trùng** giữa các dòng. |
| `shopName` | tên shop | chuỗi ≤ 255 ký tự | Có | Tên cửa hàng hiển thị cho khách. |
| `categoryId` | mã danh mục | số nguyên lớn | Không | FK → categories.id, ngành hàng chính của shop. **Khóa ngoại** → bảng của `Category`. |
| `businessType` | loại hình kinh doanh | danh sách cố định `BusinessType` | Không | INDIVIDUAL (cá nhân) / HOUSEHOLD (hộ KD) / BUSINESS (doanh nghiệp). Giá trị: `INDIVIDUAL` (cá nhân), `HOUSEHOLD` (hộ kinh doanh), `BUSINESS` (doanh nghiệp). |
| `identityCode` | số CCCD / CMND | chuỗi ≤ 255 ký tự | Không | Số CCCD/CMND của chủ shop (dùng đối chiếu KYC). |
| `taxCode` | mã số thuế | chuỗi ≤ 255 ký tự | Không | Mã số thuế (bắt buộc với hộ KD và doanh nghiệp). |
| `representative` | người đại diện pháp luật | chuỗi ≤ 255 ký tự | Không | Họ tên người đại diện pháp luật. |
| `coverUrl` | ảnh bìa shop | chuỗi ≤ 255 ký tự | Không | Ảnh bìa trang shop. |
| `shopDescription` | mô tả shop | văn bản dài | Không | Mô tả giới thiệu về shop. |
| `logoUrl` | logo shop | chuỗi ≤ 500 ký tự | Không | Logo/avatar của shop. |
| `idFrontUrl` | ảnh mặt trước CCCD | chuỗi ≤ 500 ký tự | Không | Ảnh MẶT TRƯỚC CCCD. |
| `idBackUrl` | ảnh mặt sau CCCD | chuỗi ≤ 500 ký tự | Không | Ảnh MẶT SAU CCCD. |
| `licenseUrl` | ảnh giấy phép kinh doanh | chuỗi ≤ 500 ký tự | Không | Ảnh giấy phép kinh doanh. |
| `selfieUrl` | ảnh chân dung tự chụp | chuỗi ≤ 500 ký tự | Không | Ảnh chân dung tự chụp, dùng so khớp khuôn mặt với CCCD. |
| `kycStatus` | trạng thái KYC | danh sách cố định `KycStatus` | Có | NOT_SUBMITTED /| Có | UNFUNDED | PENDING | FUNDED | FAILED — gán UNFUNDED khi tạo đơn./ AI_PROCESSING / MANUAL_REVIEW / VERIFIED / REJECTED / SUPPLEMENT_REQUESTED (7 trạng thái). Giá trị: `NOT_SUBMITTED` (chưa nộp), `PENDING` (đang chờ), `AI_PROCESSING` (AI đang xử lý), `MANUAL_REVIEW` (chờ duyệt tay), `VERIFIED` (đã xác minh), `REJECTED` (bị từ chối), `SUPPLEMENT_REQUESTED` (yêu cầu bổ sung). |
| `kycRejectionReason` | lý do từ chối KYC | văn bản dài | Không | Lý do bị từ chối, để vendor biết đường sửa. |
| `kycVerificationMethod` | cách duyệt KYC | danh sách cố định `KycVerificationMethod` | Không | OCR_AUTO (AI duyệt) / MANUAL_STAFF (người duyệt) / BIOMETRIC_FACIAL (sinh trắc khuôn mặt). Giá trị: `OCR_AUTO` (AI tự duyệt), `MANUAL_STAFF` (nhân viên duyệt tay), `BIOMETRIC_FACIAL` (sinh trắc khuôn mặt). |
| `kycVerifiedBy` | người duyệt KYC | số nguyên lớn | Không | ID staff đã duyệt (KHÔNG đặt FK, xem giải thích bên dưới). |
| `commissionRate` | tỷ lệ hoa hồng | tỷ lệ (vd 0.0500 = 5%) | Không | Tỷ lệ hoa hồng THƯƠNG LƯỢNG RIÊNG của shop (VD: 0.0300 = 3%). NULL = không có thỏa thuận riêng → dùng Category.commissionRate → mặc định toàn sàn 5% (xem effectiveCommissionRate()). |
| `kycSubmittedAt` | thời điểm nộp KYC | ngày + giờ (có múi giờ) | Không | Thời điểm vendor nộp hồ sơ. |
| `kycVerifiedAt` | thời điểm duyệt KYC | ngày + giờ (có múi giờ) | Không | Thời điểm hồ sơ được duyệt. |
| `pickupProvinceId` | mã tỉnh / thành của kho lấy hàng (GHN) | số nguyên | Không | Mã tỉnh kho hàng (GHN) — kho lấy hàng duy nhất của shop trong MVP (UC-VND-05), dùng tính phí ship GHN. |
| `pickupProvince` | tên tỉnh / thành của kho lấy hàng | chuỗi ≤ 100 ký tự | Không | Tên tỉnh/thành kho hàng (cache hiển thị, như Address.province). |
| `pickupDistrictId` | mã quận / huyện của kho lấy hàng (GHN) | số nguyên | Không | Mã quận/huyện kho hàng (GHN). |
| `pickupDistrict` | tên quận / huyện của kho lấy hàng | chuỗi ≤ 100 ký tự | Không | Tên quận/huyện kho hàng (cache hiển thị). |
| `pickupWardCode` | mã phường / xã của kho lấy hàng (GHN) | chuỗi ≤ 50 ký tự | Không | Mã phường/xã kho hàng (GHN). |
| `pickupWard` | tên phường / xã của kho lấy hàng | chuỗi ≤ 100 ký tự | Không | Tên phường/xã kho hàng (cache hiển thị). |
| `pickupAddressLine` | địa chỉ chi tiết của kho lấy hàng | chuỗi ≤ 500 ký tự | Không | Địa chỉ chi tiết kho hàng để shipper tới lấy. |
| `ghnShopId` | mã cửa hàng bên GHN | số nguyên | Không | ID shop do GHN cấp sau khi gọi API đăng ký (UC-VND-05); NULL khi mới mở shop (UC-VND-01). |
| `payoutBankName` | ngân hàng nhận tiền | chuỗi ≤ 100 ký tự | Không | Tên ngân hàng nhận tiền rút. |
| `payoutBankAccountNumber` | số tài khoản nhận tiền | chuỗi ≤ 50 ký tự | Không | Số tài khoản ngân hàng. |
| `payoutBankAccountName` | tên chủ tài khoản nhận tiền | chuỗi ≤ 255 ký tự | Không | Tên chủ tài khoản ngân hàng. |
| `payoutAccountVerifiedAt` | thời điểm xác minh tài khoản chính chủ | ngày + giờ (có múi giờ) | Không | Thời điểm xác minh tài khoản chính chủ (tên ngân hàng trả về khớp họ tên KYC). NULL = chưa được rút tiền; đổi tài khoản thì đặt lại NULL. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm đăng ký shop. Hệ thống tự ghi khi tạo dòng. |
| `isMall` | là shop chính hãng | đúng / sai | Có | Huy hiệu "Chính hãng" / bộ lọc "Shop Chính Hãng": Staff gắn khi duyệt KYC doanh nghiệp chính hãng (UC-VND-04). Mặc định: `false`. |
| `ratingAvg` | điểm đánh giá trung bình | số thập phân (1 chữ số lẻ) | Có | Điểm đánh giá shop = trung bình sao các đánh giá đang hiển thị của mọi sản phẩm shop (UC-CAT-16). Mặc định: `BigDecimal.ZERO`. |
| `ratingCount` | số lượt đánh giá | số nguyên | Có | Số đánh giá dùng để tính ratingAvg. Mặc định: `0`. |
| `followerCount` | số người theo dõi | số nguyên | Có | Số người theo dõi (đếm sẵn, đồng bộ với shop_follows — UC-SOC-06). Mặc định: `0`. |
| `activeProductCount` | số sản phẩm đang bán | số nguyên | Có | Số sản phẩm PUBLISHED (cập nhật khi đăng / ẩn / xóa sản phẩm). Mặc định: `0`. |
| `chatResponseRate` | tỉ lệ phản hồi chat | số tiền (DECIMAL, không làm tròn sai) | Không | % hội thoại do khách mở được shop trả lời trong 24h, tính trên 30 ngày (job hằng ngày); NULL = chưa đủ dữ liệu. |
| `avgResponseMinutes` | thời gian phản hồi trung bình (phút) | số nguyên | Không | Thời gian phản hồi chat trung bình (phút) — hiển thị "phản hồi trong vài phút / vài giờ". |
| `preOrderEnabled` | cho phép đặt trước | đúng / sai | Có | Cho phép đăng sản phẩm đặt trước (Pre-order) — UC-VND-21. Mặc định: `false`. |
| `isFavorite` | là Shop Yêu Thích | đúng / sai | Có | Huy hiệu "Shop Yêu Thích" do sàn gắn khi đạt chuẩn đánh giá & phản hồi chat (UC-CAT-16, TLCN v1.3). Mặc định: `false`. |
| `autoReplyEnabled` | bật tự trả lời chat | đúng / sai | Có | Bật/tắt tính năng trả lời tin nhắn tự động. Mặc định: `false`. |
| `autoReplyGreeting` | lời chào tự động | văn bản dài | Không | Câu chào tự động khi khách mở chat. |
| `autoReplyOffline` | tin trả lời khi vắng mặt | văn bản dài | Không | Câu trả lời khi shop vắng mặt / ngoài giờ làm việc. |
| `vacationStartsAt` | bắt đầu tạm nghỉ | ngày + giờ (có múi giờ) | Không | Bắt đầu tạm nghỉ: sản phẩm vẫn hiển thị nhưng KHÔNG cho đặt mua; NULL = đang bán bình thường. |
| `vacationEndsAt` | kết thúc tạm nghỉ | ngày + giờ (có múi giờ) | Không | Tự động mở bán lại (tối đa 30 ngày / lần). |
| `vacationMessage` | thông báo tạm nghỉ | chuỗi ≤ 500 ký tự | Không | Thông báo hiển thị trên trang shop và trả lời chat tự động khi tạm nghỉ. |
| `user` | tài khoản người dùng | đối tượng `User` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `User` liên quan (JPA nạp khi cần). Tài khoản đăng nhập của chủ shop. |

<a id="adminprofile"></a>
## 1.5 `AdminProfile` — Hồ sơ Quản trị viên / Nhân viên

- **Đọc là:** hồ sơ quản trị viên
- **Bảng trong CSDL:** `admin_profiles`
- **Nằm ở sơ đồ lớp:** Sơ đồ 1 — Định danh, Hồ sơ người dùng, KYC & Phân quyền nhân viên (Nhóm 1)
- **Liên kết với bảng khác:**
  - Mỗi `User` **sở hữu** 0 hoặc 1 `AdminProfile`; `AdminProfile` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `AdminProfile.userId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id, UNIQUE (quan hệ 1-1). **Khóa ngoại** → bảng của `User`. Giá trị **không được trùng** giữa các dòng. |
| `fullName` | họ tên | chuỗi ≤ 255 ký tự | Có | Họ tên nhân viên/quản trị viên. |
| `avatarUrl` | link ảnh đại diện | chuỗi ≤ 500 ký tự | Không | Ảnh đại diện. |
| `isActiveModerator` | đang trực kiểm duyệt | đúng / sai | Có | True = đang trong danh sách nhận phân công kiểm duyệt. Mặc định: `false`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo hồ sơ. Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Thời điểm cập nhật gần nhất. Hệ thống tự ghi mỗi lần sửa. |
| `staffTitle` | chức danh nhân viên | danh sách cố định `StaffTitle` | Không | Chức danh của STAFF (3 chức danh); NULL với ADMIN. Chỉ dùng nạp bộ quyền mặc định — quyền thật ở staff_permissions. Giá trị: `MODERATOR` (kiểm duyệt viên), `CS_SPECIALIST` (chuyên viên CSKH), `FINANCE_OFFICER` (nhân viên tài chính). |
| `user` | tài khoản người dùng | đối tượng `User` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `User` liên quan (JPA nạp khi cần). Tài khoản đăng nhập. |

<a id="kycresult"></a>
## 1.6 `KycResult` — Kết quả OCR/AI thẩm định giấy tờ

- **Đọc là:** kết quả KYC
- **Bảng trong CSDL:** `kyc_results`
- **Nằm ở sơ đồ lớp:** Sơ đồ 1 — Định danh, Hồ sơ người dùng, KYC & Phân quyền nhân viên (Nhóm 1)
- **Liên kết với bảng khác:**
  - Mỗi `VendorProfile` **sở hữu** 0 hoặc 1 `KycResult`; `KycResult` không tồn tại nếu thiếu `VendorProfile` (xóa cha thì xóa con) (qua cột `KycResult.vendorProfileId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `vendorProfileId` | mã hồ sơ shop | số nguyên lớn | Có | FK → vendor_profiles.id, UNIQUE (mỗi shop 1 bản kết quả; TLCN chỉ KYC người bán). **Khóa ngoại** → bảng của `VendorProfile`. Giá trị **không được trùng** giữa các dòng. |
| `extractedIdNumber` | số CCCD đọc được từ thẻ | chuỗi ≤ 20 ký tự | Không | Số CCCD 12 số (từ QR; MRZ chứa số này ở trường tùy chọn dòng 1). |
| `extractedFullName` | họ tên đọc được từ thẻ | chuỗi ≤ 255 ký tự | Không | Họ tên có dấu (từ QR; MRZ dòng 3 là bản không dấu để đối chiếu). |
| `extractedDob` | ngày sinh đọc được từ thẻ | chuỗi ≤ 20 ký tự | Không | Ngày sinh (từ QR, dạng ddMMyyyy; lưu String để giữ dữ liệu thô). |
| `extractedGender` | giới tính đọc được từ thẻ | chuỗi ≤ 10 ký tự | Không | Giới tính "Nam"/"Nữ" (từ QR; MRZ ghi M/F). |
| `extractedNationality` | quốc tịch đọc được từ thẻ | chuỗi ≤ 100 ký tự | Không | Quốc tịch (từ MRZ, mã VNM). |
| `extractedAddress` | địa chỉ thường trú đọc được từ thẻ | văn bản dài | Không | Nơi thường trú (từ QR). |
| `extractedIssueDate` | ngày cấp đọc được từ thẻ | chuỗi ≤ 20 ký tự | Không | Ngày cấp (từ QR). |
| `extractedExpiryDate` | ngày hết hạn đọc được từ thẻ | chuỗi ≤ 20 ký tự | Không | Ngày hết hạn (từ MRZ dòng 2, YYMMDD). |
| `extractedIssuer` | nơi cấp | chuỗi ≤ 255 ký tự | Không | Nơi cấp; QR/MRZ không chứa, chỉ có khi người duyệt nhập tay. |
| `qrCodeDetected` | đọc được QR | đúng / sai | Không | Giải mã được QR mặt trước (thẻ gắn chip / thẻ căn cước mới đều có). |
| `mrzValid` | MRZ hợp lệ | đúng / sai | Không | Đọc được 3 dòng MRZ mặt sau và đúng toàn bộ check digit ICAO 9303 (trọng số 7-3-1). |
| `mrzQrMatch` | QR khớp MRZ | đúng / sai | Không | QR khớp MRZ: số thẻ, ngày sinh, giới tính, họ tên (bỏ dấu, viết hoa). |
| `cccdNumberValid` | số CCCD hợp lệ | đúng / sai | Không | Số CCCD đúng cấu trúc (mã tỉnh 3 số + thế kỷ/giới tính + 2 số năm sinh) và khớp ngày sinh, giới tính. |
| `expiryRuleValid` | hạn thẻ đúng quy tắc | đúng / sai | Không | Ngày hết hạn khớp quy tắc theo tuổi đổi thẻ (14 / 25 / 40 / 60 tuổi hoặc không thời hạn — Luật Căn cước 2023). |
| `duplicateIdFound` | phát hiện trùng CCCD | đúng / sai | Không | Số CCCD đã gắn với một shop khác đang hoạt động. |
| `faceMatchResult` | kết quả khớp khuôn mặt | đúng / sai | Không | Khuôn mặt selfie khớp ảnh chân dung cắt từ mặt trước thẻ. |
| `faceMatchScore` | điểm khớp khuôn mặt | tỷ lệ (vd 0.0500 = 5%) | Không | Điểm tương đồng cosine 0.0000 → 1.0000 (YuNet + SFace). |
| `livenessScore` | điểm người thật | tỷ lệ (vd 0.0500 = 5%) | Không | Điểm người thật 0.0000 → 1.0000 (thử thách nháy mắt/quay đầu phía client + anti-spoof thụ động). |
| `faceDuplicateFound` | phát hiện trùng khuôn mặt | đúng / sai | Không | Khuôn mặt trùng hồ sơ KYC của CCCD khác (MVP: so cosine với embedding đã lưu trong AI Worker — không dùng Vector DB). |
| `riskScore` | điểm rủi ro | số nguyên | Không | Tổng điểm rủi ro 0..∞; mỗi tín hiệu cộng điểm thay vì chặn cứng. |
| `riskSignalsJson` | danh sách tín hiệu rủi ro (JSON) | JSON (JSONB) | Không | Mảng JSON các tín hiệu đã cộng điểm, vd [{"code":"FACE_SCORE_BORDERLINE","points":25}]. |
| `sampledForReview` | bị lấy mẫu để hậu kiểm | đúng / sai | Có | True = hồ sơ điểm thấp bị lấy mẫu ngẫu nhiên (~5%) để người duyệt hậu kiểm. Mặc định: `false`. |
| `errors` | danh sách lỗi | văn bản dài | Không | Mảng JSON chứa thông báo lỗi của từng bước xử lý. |
| `overallStatus` | kết luận tổng | danh sách cố định `KycOverallResult` | Có | Kết luận tổng: PASS / PARTIAL / FAIL (VARCHAR + CHECK constraint trên PostgreSQL 16). Giá trị: `PASS` (đạt), `PARTIAL` (đạt một phần (chuyển người duyệt)), `FAIL` (thất bại). |
| `processedAt` | thời điểm xử lý xong | ngày + giờ (có múi giờ) | Không | Thời điểm AI xử lý xong. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo bản ghi. Hệ thống tự ghi khi tạo dòng. |
| `vendorProfile` | hồ sơ người bán (shop) | đối tượng `VendorProfile` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `VendorProfile` liên quan (JPA nạp khi cần). Hồ sơ shop tương ứng (NULL khi là KYC tài xế). |

<a id="refreshtoken"></a>
## 1.7 `RefreshToken` — Phiên đăng nhập

- **Đọc là:** token làm mới
- **Bảng trong CSDL:** `refresh_tokens`
- **Nằm ở sơ đồ lớp:** Sơ đồ 1 — Định danh, Hồ sơ người dùng, KYC & Phân quyền nhân viên (Nhóm 1)
- **Liên kết với bảng khác:**
  - Mỗi `User` **sở hữu** nhiều (0..n) `RefreshToken`; `RefreshToken` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `RefreshToken.userId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id — token này của ai. **Khóa ngoại** → bảng của `User`. |
| `tokenHash` | mã băm của token | chuỗi ≤ 255 ký tự | Có | BĂM của refresh token (KHÔNG lưu token gốc!). Giá trị **không được trùng** giữa các dòng. |
| `deviceInfo` | thông tin thiết bị | chuỗi ≤ 255 ký tự | Có | Thông tin thiết bị (User-Agent) — để user nhận ra phiên nào là của mình. |
| `ipAddress` | địa chỉ IP | chuỗi ≤ 45 ký tự | Có | IP lúc đăng nhập — độ dài 45 để chứa được cả IPv6. |
| `expiresAt` | thời điểm hết hạn | ngày + giờ (có múi giờ) | Có | Thời điểm token hết hạn (chuẩn dự án: 7 ngày, xoay vòng mỗi lần làm mới). |
| `revokedAt` | thời điểm thu hồi | ngày + giờ (có múi giờ) | Không | Thời điểm bị thu hồi. NULL = còn hiệu lực. |
| `revokedReason` | lý do thu hồi | chuỗi ≤ 100 ký tự | Không | Lý do thu hồi: "USER_LOGOUT", "PASSWORD_CHANGED", "ROTATED", "REUSE_DETECTED", "SUSPENDED", "ACCOUNT_DELETED", "ADMIN_REVOKED". |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm đăng nhập tạo ra phiên này. Hệ thống tự ghi khi tạo dòng. |
| `familyId` | mã phiên (họ token) | số nguyên lớn | Có | *(Bổ sung V14)* Mã phiên ổn định qua các lần xoay vòng refresh token — bằng `id` của token đầu tiên của phiên. Là `id` thiết bị ở màn "Thiết bị đăng nhập" và claim `sid` trong JWT. |
| `lastUsedAt` | lần hoạt động cuối | ngày + giờ (có múi giờ) | Không | *(Bổ sung V14)* Lần làm mới token gần nhất — hiển thị "Hoạt động lần cuối". NULL = chưa làm mới (dùng `createdAt`). |

<a id="staffpermission"></a>
## 1.8 `StaffPermission` — Phân quyền chi tiết cho nhân viên

- **Đọc là:** quyền nhân viên
- **Bảng trong CSDL:** `staff_permissions`
- **Nằm ở sơ đồ lớp:** Sơ đồ 1 — Định danh, Hồ sơ người dùng, KYC & Phân quyền nhân viên (Nhóm 1)
- **Liên kết với bảng khác:**
  - Mỗi `StaffPermission` **trỏ tới** đúng 1 `User` (qua cột `StaffPermission.grantedByAdminId`).
  - Mỗi `User` **sở hữu** nhiều (0..n) `StaffPermission`; `StaffPermission` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `StaffPermission.staffUserId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `staffUserId` | mã nhân viên | số nguyên lớn | Có | FK → users.id — nhân viên được cấp quyền. **Khóa ngoại** → bảng của `User`. |
| `permission` | mã quyền | danh sách cố định `StaffPermissionEnum` | Có | 13 quyền TLCN: VIEW_DASHBOARD, REVIEW_KYC, MANAGE_CATALOG, MODERATE_REVIEW, HANDLE_DISPUTE, PROCESS_PAYOUT, UPDATE_COMMISSION, VIEW_FINANCE, MANAGE_VOUCHER, MANAGE_PLATFORM_NOTIFICATION, VIEW_AUDIT_LOG, MANAGE_USER, MANAGE_RBAC. Giá trị: `VIEW_DASHBOARD` (xem tổng quan), `REVIEW_KYC` (duyệt KYC), `MANAGE_CATALOG` (quản lý danh mục & duyệt sản phẩm), `MODERATE_REVIEW` (kiểm duyệt đánh giá), `HANDLE_DISPUTE` (xử lý trả hàng / khiếu nại), `PROCESS_PAYOUT` (xử lý rút tiền), `UPDATE_COMMISSION` (sửa hoa hồng), `VIEW_FINANCE` (xem sổ cái tài chính), `MANAGE_VOUCHER` (quản lý voucher sàn & Flash Sale), `MANAGE_PLATFORM_NOTIFICATION` (phát thông báo sàn), `VIEW_AUDIT_LOG` (xem nhật ký hệ thống), `MANAGE_USER` (khóa / mở người dùng), …. |
| `grantedByAdminId` | mã Admin đã cấp quyền | số nguyên lớn | Có | FK → users.id — Admin nào đã cấp quyền này. **Khóa ngoại** → bảng của `User`. |
| `grantedAt` | thời điểm cấp | ngày + giờ (có múi giờ) | Có | Thời điểm cấp quyền. |
| `revokedAt` | thời điểm thu hồi | ngày + giờ (có múi giờ) | Không | Thời điểm thu hồi. NULL = quyền vẫn còn hiệu lực. |

# Nhóm 2 — Danh mục & sản phẩm

<a id="category"></a>
## 2.1 `Category` — Danh mục ngành hàng

- **Đọc là:** danh mục
- **Bảng trong CSDL:** `categories`
- **Nằm ở sơ đồ lớp:** Sơ đồ 2 — Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3)
- **Liên kết với bảng khác:**
  - Mỗi `VendorProfile` **trỏ tới** 0 hoặc 1 `Category` (qua cột `VendorProfile.categoryId`).
  - Mỗi `Category` **gom** nhiều (0..n) `Category`; `Category` vẫn tồn tại độc lập (qua cột `Category.parentId`).
  - Mỗi `Category` **gom** nhiều (0..n) `Product`; `Product` vẫn tồn tại độc lập (qua cột `Product.categoryId`).
  - Mỗi `Category` **sở hữu** nhiều (0..n) `CategoryAttribute`; `CategoryAttribute` không tồn tại nếu thiếu `Category` (xóa cha thì xóa con) (qua cột `CategoryAttribute.categoryId`).
  - Mỗi `CategoryProposal` **trỏ tới** 0 hoặc 1 `Category` (qua cột `CategoryProposal.createdCategoryId`) — vai trò *created*.
  - Mỗi `CategoryProposal` **trỏ tới** 0 hoặc 1 `Category` (qua cột `CategoryProposal.parentCategoryId`) — vai trò *parent*.
  - Mỗi `VoucherCategory` **trỏ tới** đúng 1 `Category` (qua cột `VoucherCategory.categoryId`).
  - Mỗi `VoucherExcludedCategory` **trỏ tới** đúng 1 `Category` (qua cột `VoucherExcludedCategory.categoryId`).
  - Mỗi `HomeBanner` **trỏ tới** 0 hoặc 1 `Category` (qua cột `HomeBanner.categoryId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `name` | tên | chuỗi ≤ 255 ký tự | Có | Tên danh mục hiển thị: "Điện thoại", "Thời trang nam". |
| `slug` | đường dẫn thân thiện (slug) | chuỗi ≤ 255 ký tự | Không | Chuỗi thân thiện URL: "dien-thoai" → /danh-muc/dien-thoai. |
| `parentId` | mã danh mục cha | số nguyên lớn | Không | FK → chính bảng này. NULL = danh mục gốc (cấp 1). **Khóa ngoại** → bảng của `Category`. |
| `displayOrder` | thứ tự hiển thị | số nguyên | Không | Thứ tự sắp xếp khi hiển thị (số nhỏ lên trước). |
| `icon` | biểu tượng | chuỗi ≤ 50 ký tự | Không | Tên/mã icon hiển thị trên giao diện. |
| `description` | mô tả danh mục | chuỗi ≤ 500 ký tự | Không | Mô tả ngắn hiện ở đầu trang danh mục (UC-CAT-01, TLCN v1.3). |
| `commissionRate` | tỷ lệ hoa hồng | tỷ lệ (vd 0.0500 = 5%) | Không | Hoa hồng mặc định theo ngành hàng, cùng kiểu với VendorProfile/OrderItem (vd 0.0350 = 3.5%, khung 0.0350–0.0800). |
| `isActive` | đang bật / đang hoạt động | đúng / sai | Có | True = đang hiển thị; false = ẩn khỏi menu nhưng giữ dữ liệu. Mặc định: `true`. |
| `requiresBusinessLicense` | ngành hàng bắt buộc giấy phép | đúng / sai | Có | True = ngành hàng có điều kiện (thực phẩm, mỹ phẩm, thiết bị điện…) — sản phẩm phải kèm giấy tờ chứng thực (product_documents) mới được gửi duyệt. Mặc định: `false`. |
| `subCategories` | các danh mục con | danh sách `Category` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Category` liên quan (JPA nạp khi cần). Các danh mục con trực tiếp (đệ quy). |

<a id="product"></a>
## 2.2 `Product` — Sản phẩm (SPU)

- **Đọc là:** sản phẩm
- **Bảng trong CSDL:** `products`
- **Nằm ở sơ đồ lớp:** Sơ đồ 2 — Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3)
- **Liên kết với bảng khác:**
  - Mỗi `Category` **gom** nhiều (0..n) `Product`; `Product` vẫn tồn tại độc lập (qua cột `Product.categoryId`).
  - Mỗi `Product` **trỏ tới** 0 hoặc 1 `Brand` (qua cột `Product.brandId`).
  - Mỗi `Product` **trỏ tới** đúng 1 `VendorProfile` (qua cột `Product.vendorId`).
  - Mỗi `Product` **sở hữu** nhiều (0..n) `ProductVariant`; `ProductVariant` không tồn tại nếu thiếu `Product` (xóa cha thì xóa con) (qua cột `ProductVariant.productId`).
  - Mỗi `Product` **sở hữu** nhiều (0..n) `ProductImage`; `ProductImage` không tồn tại nếu thiếu `Product` (xóa cha thì xóa con) (qua cột `ProductImage.productId`).
  - Mỗi `UserWishlist` **trỏ tới** đúng 1 `Product` (qua cột `UserWishlist.productId`).
  - Mỗi `Product` **sở hữu** nhiều (0..n) `ProductDocument`; `ProductDocument` không tồn tại nếu thiếu `Product` (xóa cha thì xóa con) (qua cột `ProductDocument.productId`).
  - Mỗi `Product` **sở hữu** nhiều (0..n) `ProductModerationResult`; `ProductModerationResult` không tồn tại nếu thiếu `Product` (xóa cha thì xóa con) (qua cột `ProductModerationResult.productId`).
  - Mỗi `Product` **sở hữu** nhiều (0..n) `ProductViewStat`; `ProductViewStat` không tồn tại nếu thiếu `Product` (xóa cha thì xóa con) (qua cột `ProductViewStat.productId`).
  - Mỗi `VoucherProduct` **trỏ tới** đúng 1 `Product` (qua cột `VoucherProduct.productId`).
  - Mỗi `VoucherExcludedProduct` **trỏ tới** đúng 1 `Product` (qua cột `VoucherExcludedProduct.productId`).
  - Mỗi `FlashSaleItem` **trỏ tới** đúng 1 `Product` (qua cột `FlashSaleItem.productId`).
  - Mỗi `ProductReview` **trỏ tới** đúng 1 `Product` (qua cột `ProductReview.productId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Có | FK → users.id (của vendor) — sản phẩm này thuộc shop nào. **Khóa ngoại** → bảng của `VendorProfile`. |
| `categoryId` | mã danh mục | số nguyên lớn | Không | FK → categories.id, danh mục của sản phẩm. **Khóa ngoại** → bảng của `Category`. |
| `name` | tên | chuỗi ≤ 255 ký tự | Có | Tên sản phẩm hiển thị. |
| `description` | mô tả | văn bản dài | Không | Mô tả chi tiết, có thể chứa HTML. |
| `brand` | thương hiệu (chữ) | chuỗi ≤ 255 ký tự | Không | Thương hiệu: "Samsung", "Nike". |
| `condition` | tình trạng (chữ, cũ) | chuỗi ≤ 50 ký tự | Không | Tình trạng: "NEW" (mới) / "USED" (đã qua sử dụng). |
| `origin` | xuất xứ | chuỗi ≤ 100 ký tự | Không | Xuất xứ: "Việt Nam", "Trung Quốc". |
| `specsJson` | thông số kỹ thuật (JSON) | JSON (JSONB) | Không | Mảng JSON chứa thông số kỹ thuật (RAM, màn hình, chất liệu...). |
| `categoryJson` | đường dẫn danh mục (JSON) | JSON (JSONB) | Không | Mảng JSON lưu ĐƯỜNG DẪN danh mục đầy đủ (breadcrumb). |
| `weight` | khối lượng | số thực | Không | Khối lượng (gram) — bắt buộc để GHN tính cước. |
| `length` | chiều dài | số thực | Không | Chiều dài kiện hàng (cm). |
| `width` | chiều rộng | số thực | Không | Chiều rộng kiện hàng (cm). |
| `height` | chiều cao | số thực | Không | Chiều cao kiện hàng (cm). |
| `shipsFrom` | nơi gửi hàng | chuỗi ký tự | Không | Nơi gửi hàng hiển thị cho khách. |
| `shippingPayer` | bên trả phí ship | danh sách cố định `ShippingPayer` | Không | Ai trả phí ship: BUYER (khách) / SELLER (shop). Giá trị: `BUYER` (người mua), `SELLER` (người bán). |
| `shippingFeeSupport` | tiền shop hỗ trợ phí ship | số tiền (DECIMAL, không làm tròn sai) | Không | Số tiền shop hỗ trợ phí ship (chuẩn hóa DECIMAL thay cho Double theo database-schema-designer). |
| `basePrice` | giá gốc | số tiền (DECIMAL, không làm tròn sai) | Có | Giá gốc/giá hiển thị của sản phẩm (giá thật nằm ở từng variant). |
| `status` | trạng thái | danh sách cố định `ProductStatus` | Có | DRAFT / PENDING_APPROVAL / PUBLISHED / HIDDEN / REJECTED / FLAGGED / DELETED. Giá trị: `DRAFT` (bản nháp), `PENDING_APPROVAL` (chờ duyệt), `PUBLISHED` (đang bán), `HIDDEN` (đã ẩn), `REJECTED` (bị từ chối), `FLAGGED` (bị gắn cờ), `DELETED` (đã xóa). |
| `moderationReason` | lý do kiểm duyệt | văn bản dài | Không | Lý do bị từ chối/gắn cờ, để vendor biết đường sửa. |
| `moderatorId` | mã kiểm duyệt viên | số nguyên lớn | Không | FK mềm → users.id, staff được phân công duyệt sản phẩm này. Lưu mã của đối tượng khác nhưng không ràng buộc cứng ở CSDL (khóa ngoại mềm). |
| `isPublished` | đã từng đăng bán | đúng / sai | Không | Cờ phụ đánh dấu đã từng được đăng bán. |
| `version` | số phiên bản (khóa lạc quan) | số nguyên lớn | Có | Optimistic Lock — chống ghi đè khi vendor và staff cùng sửa. Mỗi lần sửa dòng, số này tăng 1; nếu hai người sửa cùng lúc, người sau bị chặn và phải tải lại (chống ghi đè, chống bán vượt / rút tiền 2 lần). |
| `variantImageAttribute` | thuộc tính quyết định ảnh biến thể | chuỗi ≤ 255 ký tự | Không | Tên thuộc tính quyết định ảnh biến thể, VD "Màu sắc". |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo sản phẩm. Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Thời điểm cập nhật gần nhất. Hệ thống tự ghi mỗi lần sửa. |
| `sold` | số đã bán | số nguyên | Không | Số lượng đã bán — tính từ bảng order_items. **Không lưu thành cột**: tính ra mỗi lần đọc. |
| `rating` | số sao đánh giá | số thực | Không | Điểm đánh giá trung bình — tính từ bảng product_reviews. **Không lưu thành cột**: tính ra mỗi lần đọc. |
| `brandId` | mã thương hiệu | số nguyên lớn | Không | FK → brands.id; NULL = "No Brand" (UC-CAT-09). **Khóa ngoại** → bảng của `Brand`. |
| `itemCondition` | tình trạng hàng | danh sách cố định `ProductCondition` | Có | Tình trạng: NEW / USED / REFURBISHED. Giá trị: `NEW` (mới), `USED` (đã qua sử dụng), `REFURBISHED` (tân trang). Mặc định: `ProductCondition.NEW`. |
| `isPreOrder` | là hàng đặt trước | đúng / sai | Có | Sản phẩm đặt trước (chỉ khi VendorProfile.preOrderEnabled). Mặc định: `false`. |
| `preOrderDays` | số ngày chuẩn bị hàng đặt trước | số nguyên | Không | Số ngày chuẩn bị hàng của sản phẩm đặt trước (7–30); NULL khi không đặt trước. |
| `viewCount` | lượt xem | số nguyên lớn | Có | Lượt xem tích lũy (flush từ Redis product:{id}:views; chi tiết theo ngày ở product_view_stats). Mặc định: `0L`. |
| `category` | danh mục | đối tượng `Category` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Category` liên quan (JPA nạp khi cần). Danh mục chứa sản phẩm. |
| `variants` | các biến thể | danh sách `ProductVariant` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `ProductVariant` liên quan (JPA nạp khi cần). Các biến thể (SKU) — nơi chứa giá và tồn kho thật. |
| `images` | các ảnh | danh sách `ProductImage` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `ProductImage` liên quan (JPA nạp khi cần). Bộ ảnh của sản phẩm. |
| `vendorProfile` | hồ sơ người bán (shop) | đối tượng `VendorProfile` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `VendorProfile` liên quan (JPA nạp khi cần). Hồ sơ shop bán sản phẩm này. |

<a id="productvariant"></a>
## 2.3 `ProductVariant` — Biến thể sản phẩm (SKU)

- **Đọc là:** biến thể sản phẩm
- **Bảng trong CSDL:** `product_variants`
- **Nằm ở sơ đồ lớp:** Sơ đồ 2 — Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3)
- **Liên kết với bảng khác:**
  - Mỗi `Product` **sở hữu** nhiều (0..n) `ProductVariant`; `ProductVariant` không tồn tại nếu thiếu `Product` (xóa cha thì xóa con) (qua cột `ProductVariant.productId`).
  - Mỗi `ProductImage` **trỏ tới** 0 hoặc 1 `ProductVariant` (qua cột `ProductImage.variantId`).
  - Mỗi `CartItem` **trỏ tới** đúng 1 `ProductVariant` (qua cột `CartItem.productVariantId`).
  - Mỗi `OrderItem` **trỏ tới** đúng 1 `ProductVariant` (qua cột `OrderItem.productVariantId`).
  - Mỗi `StockReservation` **trỏ tới** đúng 1 `ProductVariant` (qua cột `StockReservation.productVariantId`).
  - Mỗi `Message` **trỏ tới** 0 hoặc 1 `ProductVariant` (qua cột `Message.productVariantId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `productId` | mã sản phẩm | số nguyên lớn | Có | FK → products.id — biến thể này thuộc sản phẩm nào. **Khóa ngoại** → bảng của `Product`. |
| `sku` | mã SKU | chuỗi ≤ 100 ký tự | Có | Mã SKU duy nhất toàn hệ thống — mã kho của đơn vị bán. Giá trị **không được trùng** giữa các dòng. |
| `variantName` | tên biến thể | chuỗi ≤ 255 ký tự | Không | Tên hiển thị của biến thể: "Đen - Size L". |
| `attributesJson` | thuộc tính biến thể (JSON) | JSON (JSONB) | Không | JSON thuộc tính: {"Màu":"Đen","Size":"L"}. |
| `price` | giá bán | số tiền (DECIMAL, không làm tròn sai) | Có | GIÁ BÁN THẬT của biến thể này. |
| `stockCount` | tồn kho thực | số nguyên | Có | Tổng tồn kho vật lý đang có. |
| `reservedCount` | số đang giữ chỗ | số nguyên | Có | Số lượng đang bị GIỮ CHỖ cho các đơn chưa thanh toán xong. Mặc định: `0`. |
| `version` | số phiên bản (khóa lạc quan) | số nguyên lớn | Có | Optimistic Lock — lá chắn chống bán vượt tồn kho (overselling). Mỗi lần sửa dòng, số này tăng 1; nếu hai người sửa cùng lúc, người sau bị chặn và phải tải lại (chống ghi đè, chống bán vượt / rút tiền 2 lần). |
| `imageUrl` | link ảnh | chuỗi ≤ 500 ký tự | Không | Ảnh riêng của biến thể này. |
| `deletedAt` | thời điểm xóa mềm | ngày + giờ (có múi giờ) | Không | Xoá mềm (@SQLDelete + @SQLRestriction). NULL = chưa xoá. |
| `product` | sản phẩm | đối tượng `Product` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Product` liên quan (JPA nạp khi cần). Sản phẩm cha. |

<a id="productimage"></a>
## 2.4 `ProductImage` — Ảnh sản phẩm

- **Đọc là:** ảnh sản phẩm
- **Bảng trong CSDL:** `product_images`
- **Nằm ở sơ đồ lớp:** Sơ đồ 2 — Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3)
- **Liên kết với bảng khác:**
  - Mỗi `ProductImage` **trỏ tới** 0 hoặc 1 `ProductVariant` (qua cột `ProductImage.variantId`).
  - Mỗi `Product` **sở hữu** nhiều (0..n) `ProductImage`; `ProductImage` không tồn tại nếu thiếu `Product` (xóa cha thì xóa con) (qua cột `ProductImage.productId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `productId` | mã sản phẩm | số nguyên lớn | Có | FK → products.id — ảnh thuộc sản phẩm nào. **Khóa ngoại** → bảng của `Product`. |
| `variantId` | mã biến thể | số nguyên lớn | Không | FK → product_variants.id. NULL = ảnh trong gallery chung của sản phẩm; khác NULL = ảnh RIÊNG của biến thể (chỉ hiện khi khách chọn biến thể đó, KHÔNG xuất hiện trong gallery chung). **Khóa ngoại** → bảng của `ProductVariant`. |
| `imageUrl` | link ảnh | chuỗi ≤ 500 ký tự | Có | Link ảnh gốc trên storage (S3/Silo). |
| `cdnUrl` | link ảnh qua CDN | chuỗi ≤ 500 ký tự | Không | Link ảnh qua CDN, tải nhanh hơn cho người dùng cuối. |
| `width` | chiều rộng | số nguyên | Không | Chiều rộng ảnh (pixel). |
| `height` | chiều cao | số nguyên | Không | Chiều cao ảnh (pixel). |
| `fileSize` | dung lượng tệp | số nguyên lớn | Không | Dung lượng file (byte). |
| `altText` | chữ thay thế cho ảnh | chuỗi ≤ 500 ký tự | Không | Văn bản thay thế cho SEO & người khiếm thị. |
| `isPrimary` | là ảnh chính | đúng / sai | Không | True = ảnh đại diện hiển thị ở danh sách sản phẩm. |
| `mimeType` | loại tệp (MIME) | chuỗi ≤ 50 ký tự | Không | Loại file: "image/jpeg", "image/webp". |
| `displayOrder` | thứ tự hiển thị | số nguyên | Có | Thứ tự hiển thị trong thư viện ảnh (số nhỏ lên trước). |

<a id="userwishlist"></a>
## 2.5 `UserWishlist` — Danh sách sản phẩm yêu thích

- **Đọc là:** danh sách yêu thích người dùng
- **Bảng trong CSDL:** `user_wishlists`
- **Nằm ở sơ đồ lớp:** Sơ đồ 2 — Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3)
- **Liên kết với bảng khác:**
  - Mỗi `UserWishlist` **trỏ tới** đúng 1 `Product` (qua cột `UserWishlist.productId`).
  - Mỗi `User` **sở hữu** nhiều (0..n) `UserWishlist`; `UserWishlist` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `UserWishlist.userId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `userId` | mã người dùng | số nguyên lớn | Có | ID người dùng thả tim yêu thích. **Khóa ngoại** → bảng của `User`. |
| `productId` | mã sản phẩm | số nguyên lớn | Có | ID sản phẩm được lưu lại. **Khóa ngoại** → bảng của `Product`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm thêm vào danh sách yêu thích. Hệ thống tự ghi khi tạo dòng. |

<a id="categoryattribute"></a>
## 2.6 `CategoryAttribute` — Bộ thuộc tính chuẩn theo ngành hàng

- **Đọc là:** thuộc tính danh mục
- **Bảng trong CSDL:** `category_attributes`
- **Nằm ở sơ đồ lớp:** Sơ đồ 2 — Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3)
- **Dùng để làm gì:** Phục vụ **UC-CAT-15**: mỗi ngành hàng khai báo danh sách thuộc tính chuẩn (Xuất xứ, Thương hiệu, Hạn sử dụng, Chất liệu, Dung lượng…), đánh dấu bắt buộc / dùng để lọc / là trục biến thể. Người bán nhập giá trị vào `Product.specsJson` theo đúng `code`; sản phẩm thiếu thuộc tính bắt buộc không được gửi duyệt.
- **Liên kết với bảng khác:**
  - Mỗi `Category` **sở hữu** nhiều (0..n) `CategoryAttribute`; `CategoryAttribute` không tồn tại nếu thiếu `Category` (xóa cha thì xóa con) (qua cột `CategoryAttribute.categoryId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `categoryId` | mã danh mục | số nguyên lớn | Có | FK → categories.id — thuộc tính khai ở ngành cha được ngành con kế thừa (Service duyệt cây danh mục). **Khóa ngoại** → bảng của `Category`. |
| `code` | mã | chuỗi ≤ 50 ký tự | Có | Khóa máy đọc, dùng làm key trong Product.specsJson (vd: "origin", "expiry_date", "storage_gb"). |
| `name` | tên | chuỗi ≤ 100 ký tự | Có | Tên hiển thị: "Xuất xứ", "Hạn sử dụng". |
| `dataType` | kiểu dữ liệu | danh sách cố định `AttributeDataType` | Có | TEXT / NUMBER / BOOLEAN / ENUM / MULTI_ENUM / DATE. Giá trị: `TEXT` (văn bản), `NUMBER` (số), `BOOLEAN` (đúng / sai), `ENUM` (chọn một giá trị trong danh sách), `MULTI_ENUM` (chọn nhiều giá trị), `DATE` (ngày). |
| `allowedValuesJson` | danh sách giá trị hợp lệ (JSON) | JSON (JSONB) | Không | Danh sách giá trị hợp lệ khi ENUM / MULTI_ENUM: ["Việt Nam","Nhật Bản",...]. |
| `unit` | đơn vị đo | chuỗi ≤ 20 ký tự | Không | Đơn vị cho NUMBER: "GB", "ml", "kg". |
| `isRequired` | bắt buộc nhập | đúng / sai | Có | Bắt buộc nhập trước khi gửi duyệt (thông tin pháp lý: xuất xứ, hạn dùng, thành phần…). Mặc định: `false`. |
| `isFilterable` | dùng làm bộ lọc | đúng / sai | Có | Dùng làm bộ lọc trên trang tìm kiếm (truy vấn JSONB Product.specsJson, index GIN). Mặc định: `false`. |
| `isVariantAxis` | là trục tạo biến thể | đúng / sai | Có | Là trục tạo biến thể SKU (Màu, Size) — giá trị nằm ở ProductVariant.attributesJson. Mặc định: `false`. |
| `displayOrder` | thứ tự hiển thị | số nguyên | Có | Thứ tự hiển thị trên form đăng bán & trang chi tiết. Mặc định: `0`. |
| `isActive` | đang bật / đang hoạt động | đúng / sai | Có | Ngừng dùng thuộc tính mà không làm hỏng dữ liệu cũ. Mặc định: `true`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Hệ thống tự ghi mỗi lần sửa. |
| `category` | danh mục | đối tượng `Category` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Category` liên quan (JPA nạp khi cần). Ngành hàng sở hữu thuộc tính. |

<a id="brand"></a>
## 2.7 `Brand` — Thương hiệu

- **Đọc là:** thương hiệu
- **Bảng trong CSDL:** `brands`
- **Nằm ở sơ đồ lớp:** Sơ đồ 2 — Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3)
- **Dùng để làm gì:** Phục vụ **UC-CAT-09 / UC-CAT-10** (ô "Thương hiệu" có gợi ý và nút "Thêm … làm thương hiệu mới") và bộ lọc thương hiệu ở **UC-CAT-05**.
- **Liên kết với bảng khác:**
  - Mỗi `Product` **trỏ tới** 0 hoặc 1 `Brand` (qua cột `Product.brandId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `name` | tên | chuỗi ≤ 100 ký tự | Có | Tên hiển thị: "Samsung", "Logitech". |
| `slug` | đường dẫn thân thiện (slug) | chuỗi ≤ 120 ký tự | Có | Khóa chuẩn hóa (bỏ dấu, chữ thường) — chống trùng "samsung" / "SAMSUNG". |
| `logoUrl` | logo shop | chuỗi ≤ 500 ký tự | Không | Logo thương hiệu. |
| `isVerified` | đã xác minh | đúng / sai | Có | True = Staff đã xác nhận (thương hiệu do shop tự thêm mặc định chưa xác minh). Mặc định: `false`. |
| `createdByUserId` | người tạo (shop) | số nguyên lớn | Không | FK mềm → users.id — shop đã thêm thương hiệu (NULL = do sàn tạo). Lưu mã của đối tượng khác nhưng không ràng buộc cứng ở CSDL (khóa ngoại mềm). |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Hệ thống tự ghi khi tạo dòng. |

<a id="productdocument"></a>
## 2.8 `ProductDocument` — Giấy tờ chứng thực sản phẩm

- **Đọc là:** giấy tờ sản phẩm
- **Bảng trong CSDL:** `product_documents`
- **Nằm ở sơ đồ lớp:** Sơ đồ 8 — Kiểm duyệt sản phẩm, Giấy tờ chứng thực, Đề xuất danh mục & Thống kê truy cập (Nhóm 2)
- **Dùng để làm gì:** Phục vụ **UC-CAT-09 / UC-CAT-10** (mục "Giấy tờ chứng thực — bắt buộc, tối đa 4 tệp PDF / DOC / XLS") và **UC-CAT-12 / UC-CAT-13** (Staff xem "Hồ sơ pháp lý sản phẩm" khi duyệt).
- **Liên kết với bảng khác:**
  - Mỗi `Product` **sở hữu** nhiều (0..n) `ProductDocument`; `ProductDocument` không tồn tại nếu thiếu `Product` (xóa cha thì xóa con) (qua cột `ProductDocument.productId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `productId` | mã sản phẩm | số nguyên lớn | Có | FK → products.id (ON DELETE CASCADE). **Khóa ngoại** → bảng của `Product`. |
| `documentType` | loại giấy tờ | danh sách cố định `ProductDocumentType` | Có | BUSINESS_LICENSE / QUALITY_CERTIFICATE / IMPORT_DOCUMENT / BRAND_AUTHORIZATION / OTHER. Giá trị: `BUSINESS_LICENSE` (giấy phép kinh doanh), `QUALITY_CERTIFICATE` (giấy chứng nhận chất lượng), `IMPORT_DOCUMENT` (giấy tờ nhập khẩu), `BRAND_AUTHORIZATION` (giấy ủy quyền thương hiệu), `OTHER` (khác). |
| `fileUrl` | link tệp | chuỗi ≤ 500 ký tự | Có | Đường dẫn Silo (bucket riêng tư, đọc qua Presigned URL). |
| `fileName` | tên tệp | chuỗi ≤ 255 ký tự | Có | Tên tệp gốc hiển thị cho Staff. |
| `fileExtension` | đuôi tệp | chuỗi ≤ 20 ký tự | Có | Pdf / doc / docx / xls / xlsx — KHÔNG nhận ảnh (giao diện chặn). |
| `fileSizeBytes` | dung lượng tệp (byte) | số nguyên lớn | Có | ≤ 10 MB / tệp. |
| `expiresAt` | thời điểm hết hạn | ngày (không có giờ) | Không | Ngày hết hạn của giấy tờ (giấy phép có thời hạn); NULL = không thời hạn. |
| `uploadedAt` | thời điểm tải lên | ngày + giờ (có múi giờ) | Có | Hệ thống tự ghi khi tạo dòng. |
| `product` | sản phẩm | đối tượng `Product` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Product` liên quan (JPA nạp khi cần). Sản phẩm sở hữu giấy tờ. |

<a id="categoryproposal"></a>
## 2.9 `CategoryProposal` — Đề xuất danh mục mới của người bán

- **Đọc là:** đề xuất danh mục
- **Bảng trong CSDL:** `category_proposals`
- **Nằm ở sơ đồ lớp:** Sơ đồ 8 — Kiểm duyệt sản phẩm, Giấy tờ chứng thực, Đề xuất danh mục & Thống kê truy cập (Nhóm 2)
- **Dùng để làm gì:** Phục vụ **UC-CAT-17**: người bán không tìm thấy ngành hàng phù hợp → đề xuất danh mục mới (kèm danh mục cha); Staff / Admin duyệt (tạo `Category`) hoặc từ chối kèm lý do.
- **Liên kết với bảng khác:**
  - Mỗi `CategoryProposal` **trỏ tới** 0 hoặc 1 `Category` (qua cột `CategoryProposal.createdCategoryId`) — vai trò *created*.
  - Mỗi `CategoryProposal` **trỏ tới** 0 hoặc 1 `Category` (qua cột `CategoryProposal.parentCategoryId`) — vai trò *parent*.
  - Mỗi `CategoryProposal` **trỏ tới** đúng 1 `User` (qua cột `CategoryProposal.vendorId`) — vai trò *vendor*.

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Có | FK → users.id — shop đề xuất. **Khóa ngoại** → bảng của `User`. |
| `parentCategoryId` | mã danh mục cha | số nguyên lớn | Không | FK → categories.id — danh mục cha đề xuất; NULL = đề xuất danh mục gốc. **Khóa ngoại** → bảng của `Category`. |
| `proposedName` | tên danh mục đề xuất | chuỗi ≤ 100 ký tự | Có | Tên danh mục đề xuất: "Thú cưng (Pet)". |
| `reason` | lý do | văn bản dài | Không | Shop giải thích vì sao cần danh mục này. |
| `status` | trạng thái | danh sách cố định `CategoryProposalStatus` | Có || Có | UNFUNDED | PENDING | FUNDED | FAILED — gán UNFUNDED khi tạo đơn./ APPROVED / REJECTED. Giá trị: `PENDING` (đang chờ), `APPROVED` (đã duyệt), `REJECTED` (bị từ chối). Mặc định: `CategoryProposalStatus.PENDING`. |
| `createdCategoryId` | mã danh mục đã tạo | số nguyên lớn | Không | FK → categories.id — danh mục đã tạo khi duyệt. **Khóa ngoại** → bảng của `Category`. |
| `reviewedByStaffId` | mã nhân viên xem xét | số nguyên lớn | Không | FK mềm → users.id — Staff / Admin xử lý. Lưu mã của đối tượng khác nhưng không ràng buộc cứng ở CSDL (khóa ngoại mềm). |
| `rejectionReason` | lý do từ chối | chuỗi ≤ 500 ký tự | Không | Lý do từ chối. |
| `reviewedAt` | thời điểm xem xét | ngày + giờ (có múi giờ) | Không | NULLABLE. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Hệ thống tự ghi khi tạo dòng. |

<a id="productmoderationresult"></a>
## 2.10 `ProductModerationResult` — Kết quả kiểm duyệt sản phẩm tự động

- **Đọc là:** kết quả kiểm duyệt sản phẩm
- **Bảng trong CSDL:** `product_moderation_results`
- **Nằm ở sơ đồ lớp:** Sơ đồ 8 — Kiểm duyệt sản phẩm, Giấy tờ chứng thực, Đề xuất danh mục & Thống kê truy cập (Nhóm 2)
- **Dùng để làm gì:** Phục vụ **UC-CAT-12** (kiểm duyệt tự động) và **UC-CAT-13** (màn duyệt hiển thị "Phân tích từ AI", "Lý do AI từ chối"). Mỗi lần sản phẩm được gửi duyệt (tạo mới hoặc sửa trường quan trọng) sinh **một dòng mới** — giữ lịch sử, không ghi đè.
- **Liên kết với bảng khác:**
  - Mỗi `Product` **sở hữu** nhiều (0..n) `ProductModerationResult`; `ProductModerationResult` không tồn tại nếu thiếu `Product` (xóa cha thì xóa con) (qua cột `ProductModerationResult.productId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `productId` | mã sản phẩm | số nguyên lớn | Có | FK → products.id. **Khóa ngoại** → bảng của `Product`. |
| `modelName` | tên mô hình AI | chuỗi ≤ 60 ký tự | Có | Mô hình đã dùng: "gemini-2.5-flash" (TLCN) / tên model AI Worker (DATN). |
| `decision` | quyết định | danh sách cố định `ModerationDecision` | Có | APPROVE / REJECT / NEED_REVIEW. Giá trị: `APPROVE` (duyệt), `REJECT` (từ chối), `NEED_REVIEW` (cần người duyệt). |
| `riskScore` | điểm rủi ro | số nguyên | Có | 0–100 — điểm rủi ro do mô hình trả về. |
| `violationsJson` | danh sách vi phạm (JSON) | JSON (JSONB) | Không | Mã vi phạm: ["COUNTERFEIT","PROHIBITED_ITEM","MISLEADING_DESCRIPTION","BAD_IMAGE","PRICE_ANOMALY","WRONG_CATEGORY"]. |
| `reasoning` | giải thích của AI | văn bản dài | Không | Giải thích tiếng Việt hiển thị ở ô "Phân tích từ AI". |
| `rawResponse` | phản hồi gốc của mô hình | văn bản dài | Không | JSON gốc mô hình trả về (phục vụ điều tra; không hiển thị). |
| `fallbackToManual` | chuyển duyệt thủ công | đúng / sai | Có | True = gọi mô hình lỗi / quá hạn / JSON sai → chuyển thẳng hàng chờ duyệt thủ công. Mặc định: `false`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Hệ thống tự ghi khi tạo dòng. |
| `product` | sản phẩm | đối tượng `Product` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Product` liên quan (JPA nạp khi cần). Sản phẩm được kiểm duyệt. |

<a id="searchkeywordstat"></a>
## 2.11 `SearchKeywordStat` — Thống kê từ khóa tìm kiếm theo ngày

- **Đọc là:** thống kê từ khóa tìm kiếm
- **Bảng trong CSDL:** `search_keyword_stats`
- **Nằm ở sơ đồ lớp:** Sơ đồ 8 — Kiểm duyệt sản phẩm, Giấy tờ chứng thực, Đề xuất danh mục & Thống kê truy cập (Nhóm 2)
- **Dùng để làm gì:** Phục vụ **UC-CAT-04** (từ khóa phổ biến dưới ô tìm kiếm: "Smartphone", "Áo thun", "Serum"… và gợi ý theo tiền tố). Không lưu theo từng người dùng — chỉ đếm gộp theo ngày.

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `normalizedKeyword` | từ khóa đã chuẩn hóa | chuỗi ≤ 100 ký tự | Có | Từ khóa đã chuẩn hóa: chữ thường, bỏ dấu cách thừa (giữ dấu tiếng Việt để hiển thị). |
| `statDate` | ngày thống kê | ngày (không có giờ) | Có | Ngày thống kê. |
| `searchCount` | số lượt tìm | số nguyên | Có | Số lượt tìm trong ngày (flush từ Redis search:kw:{date}). Mặc định: `0`. |
| `resultClickCount` | số lượt bấm vào kết quả | số nguyên | Có | Số lượt bấm vào kết quả — loại từ khóa "tìm mà không mua". Mặc định: `0`. |
| `isBlocked` | bị chặn | đúng / sai | Có | Staff chặn từ khóa nhạy cảm khỏi danh sách phổ biến. Mặc định: `false`. |

<a id="productviewstat"></a>
## 2.12 `ProductViewStat` — Lượt xem sản phẩm theo ngày & nguồn truy cập

- **Đọc là:** thống kê lượt xem sản phẩm
- **Bảng trong CSDL:** `product_view_stats`
- **Nằm ở sơ đồ lớp:** Sơ đồ 8 — Kiểm duyệt sản phẩm, Giấy tờ chứng thực, Đề xuất danh mục & Thống kê truy cập (Nhóm 2)
- **Dùng để làm gì:** Phục vụ **UC-VND-08** (thẻ "Lượt truy cập", "Tỉ lệ chuyển đổi", biểu đồ "Nguồn truy cập") và **UC-CAT-06** (mỗi lần mở trang chi tiết tăng bộ đếm trên Redis).
- **Liên kết với bảng khác:**
  - Mỗi `ProductViewStat` **trỏ tới** đúng 1 `User` (qua cột `ProductViewStat.vendorId`) — vai trò *vendor*.
  - Mỗi `Product` **sở hữu** nhiều (0..n) `ProductViewStat`; `ProductViewStat` không tồn tại nếu thiếu `Product` (xóa cha thì xóa con) (qua cột `ProductViewStat.productId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `productId` | mã sản phẩm | số nguyên lớn | Có | FK → products.id. **Khóa ngoại** → bảng của `Product`. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Có | FK → users.id — sao chép từ sản phẩm để tổng hợp theo shop không cần JOIN. **Khóa ngoại** → bảng của `User`. |
| `statDate` | ngày thống kê | ngày (không có giờ) | Có | Ngày thống kê. |
| `source` | nguồn truy cập | danh sách cố định `TrafficSource` | Có | SEARCH / HOME_FEED / PROMOTION / SHOP_PAGE / EXTERNAL_LINK. Giá trị: `SEARCH` (tìm kiếm), `HOME_FEED` (gợi ý trang chủ), `PROMOTION` (khuyến mãi), `SHOP_PAGE` (trang shop), `EXTERNAL_LINK` (link bên ngoài (chia sẻ)). |
| `viewCount` | lượt xem | số nguyên | Có | Lượt xem trang chi tiết. Mặc định: `0`. |
| `uniqueVisitorCount` | số khách khác nhau | số nguyên | Có | Khách khác nhau (HyperLogLog trên Redis, ghi số ước lượng). Mặc định: `0`. |
| `addToCartCount` | số lượt thêm vào giỏ | số nguyên | Có | Lượt thêm vào giỏ từ trang này. Mặc định: `0`. |

# Nhóm 3 — Giỏ hàng

<a id="cart"></a>
## 3.1 `Cart` — Giỏ hàng

- **Đọc là:** giỏ hàng
- **Bảng trong CSDL:** `carts`
- **Nằm ở sơ đồ lớp:** Sơ đồ 2 — Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3)
- **Liên kết với bảng khác:**
  - Mỗi `User` **sở hữu** 0 hoặc 1 `Cart`; `Cart` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `Cart.userId`).
  - Mỗi `Cart` **sở hữu** nhiều (0..n) `CartItem`; `CartItem` không tồn tại nếu thiếu `Cart` (xóa cha thì xóa con) (qua cột `CartItem.cartId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id, UNIQUE — mỗi user có đúng 1 giỏ hàng. **Khóa ngoại** → bảng của `User`. Giá trị **không được trùng** giữa các dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Lần cuối giỏ hàng thay đổi — dùng cho email nhắc "bỏ quên giỏ hàng". Hệ thống tự ghi mỗi lần sửa. |
| `items` | các dòng hàng | danh sách `CartItem` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `CartItem` liên quan (JPA nạp khi cần). Các dòng sản phẩm trong giỏ. |

<a id="cartitem"></a>
## 3.2 `CartItem` — Dòng sản phẩm trong giỏ

- **Đọc là:** dòng hàng giỏ hàng
- **Bảng trong CSDL:** `cart_items`
- **Nằm ở sơ đồ lớp:** Sơ đồ 2 — Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3)
- **Liên kết với bảng khác:**
  - Mỗi `CartItem` **trỏ tới** đúng 1 `ProductVariant` (qua cột `CartItem.productVariantId`).
  - Mỗi `Cart` **sở hữu** nhiều (0..n) `CartItem`; `CartItem` không tồn tại nếu thiếu `Cart` (xóa cha thì xóa con) (qua cột `CartItem.cartId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `cartId` | mã giỏ hàng | số nguyên lớn | Có | FK → carts.id — thuộc giỏ hàng nào. **Khóa ngoại** → bảng của `Cart`. |
| `productVariantId` | mã biến thể sản phẩm (SKU) | số nguyên lớn | Có | FK → product_variants.id — mua BIẾN THỂ nào (không phải sản phẩm chung). **Khóa ngoại** → bảng của `ProductVariant`. |
| `quantity` | số lượng | số nguyên | Có | Số lượng khách muốn mua. |
| `priceSnapshot` | giá chụp lại lúc thêm vào giỏ | số tiền (DECIMAL, không làm tròn sai) | Có | Giá tại thời điểm THÊM VÀO GIỎ — để phát hiện biến động giá. |
| `addedAt` | thời điểm thêm vào giỏ | ngày + giờ (có múi giờ) | Có | Thời điểm thêm vào giỏ. |
| `variant` | biến thể | đối tượng `ProductVariant` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `ProductVariant` liên quan (JPA nạp khi cần). Biến thể sản phẩm tương ứng. |

# Nhóm 4 — Khuyến mãi, voucher, flash sale & điểm

<a id="voucher"></a>
## 4.1 `Voucher` — Mã giảm giá

- **Đọc là:** voucher
- **Bảng trong CSDL:** `vouchers`
- **Nằm ở sơ đồ lớp:** Sơ đồ 3 — Voucher 3 tầng Shop / Sàn / Freeship & Phạm vi áp dụng (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `Voucher` **trỏ tới** 0 hoặc 1 `VendorProfile` (qua cột `Voucher.vendorId`).
  - Mỗi `VoucherUsage` **trỏ tới** đúng 1 `Voucher` (qua cột `VoucherUsage.voucherId`).
  - Mỗi `Voucher` **sở hữu** nhiều (0..n) `UserVoucher`; `UserVoucher` không tồn tại nếu thiếu `Voucher` (xóa cha thì xóa con) (qua cột `UserVoucher.voucherId`).
  - Mỗi `Voucher` **sở hữu** nhiều (0..n) `VoucherCategory`; `VoucherCategory` không tồn tại nếu thiếu `Voucher` (xóa cha thì xóa con) (qua cột `VoucherCategory.voucherId`).
  - Mỗi `Voucher` **sở hữu** nhiều (0..n) `VoucherProduct`; `VoucherProduct` không tồn tại nếu thiếu `Voucher` (xóa cha thì xóa con) (qua cột `VoucherProduct.voucherId`).
  - Mỗi `Voucher` **sở hữu** nhiều (0..n) `VoucherExcludedCategory`; `VoucherExcludedCategory` không tồn tại nếu thiếu `Voucher` (xóa cha thì xóa con) (qua cột `VoucherExcludedCategory.voucherId`).
  - Mỗi `Voucher` **sở hữu** nhiều (0..n) `VoucherExcludedProduct`; `VoucherExcludedProduct` không tồn tại nếu thiếu `Voucher` (xóa cha thì xóa con) (qua cột `VoucherExcludedProduct.voucherId`).
  - Mỗi `OrderDiscount` **trỏ tới** 0 hoặc 1 `Voucher` (qua cột `OrderDiscount.voucherId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `code` | mã | chuỗi ≤ 50 ký tự | Có | Mã khách nhập: "SALE50K" — UNIQUE toàn hệ thống. Giá trị **không được trùng** giữa các dòng. |
| `title` | tiêu đề | chuỗi ≤ 255 ký tự | Có | Tên chương trình hiển thị: "Giảm 50K cho đơn từ 300K". |
| `scope` | phạm vi áp dụng | danh sách cố định `VoucherScope` | Có | Phạm vi MVP: ALL (mọi sản phẩm của shop / của đơn). Giữ cột để mở rộng CATEGORY / PRODUCT ở bản đầy đủ. Giá trị: `ALL` (tất cả), `CATEGORY` (danh mục), `PRODUCT` (sản phẩm). |
| `type` | loại | danh sách cố định `VoucherType` | Có | MVP 2 tầng: SHOP (shop tài trợ, Tầng 1) / PLATFORM (sàn tài trợ, Tầng 2). Giá trị: `SHOP` (shop), `PLATFORM` (sàn), `FREESHIP` (miễn phí vận chuyển). |
| `discountType` | kiểu giảm giá | danh sách cố định `DiscountType` | Có | PERCENTAGE (theo %) / FIXED_AMOUNT (số tiền cố định). Giá trị: `PERCENTAGE` (phần trăm), `FIXED_AMOUNT` (số tiền cố định), `FREESHIP` (miễn phí vận chuyển). |
| `discountValue` | mức giảm | số tiền (DECIMAL, không làm tròn sai) | Có | Giá trị giảm: 10 (nếu là %) hoặc 50000 (nếu là số tiền). |
| `maxDiscountCap` | mức giảm tối đa (trần) | số tiền (DECIMAL, không làm tròn sai) | Không | Trần giảm tối đa. VD giảm 50% nhưng không quá 100.000đ. |
| `minOrderValue` | giá trị đơn tối thiểu | số tiền (DECIMAL, không làm tròn sai) | Có | Giá trị đơn tối thiểu mới được dùng mã. |
| `usageLimit` | tổng số lượt dùng tối đa | số nguyên | Có | Tổng số lượt được dùng của TOÀN BỘ mã này. |
| `usagePerUser` | số lượt dùng tối đa mỗi người | số nguyên | Có | Số lượt tối đa MỖI NGƯỜI được dùng. |
| `usedCount` | số lượt đã dùng | số nguyên | Có | Số lượt đã dùng thực tế (bộ đếm). Mặc định: `0`. |
| `priority` | độ ưu tiên | số nguyên | Có | Độ ưu tiên khi khách dùng nhiều mã cùng lúc (số lớn áp trước). |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Không | FK → users.id. Có giá trị khi type = SHOP; NULL = mã của sàn. **Khóa ngoại** → bảng của `VendorProfile`. |
| `validFrom` | hiệu lực từ | ngày + giờ (có múi giờ) | Có | Thời điểm bắt đầu hiệu lực. |
| `validTo` | hiệu lực đến | ngày + giờ (có múi giờ) | Có | Thời điểm hết hiệu lực. |
| `isActive` | đang bật / đang hoạt động | đúng / sai | Có | Công tắc bật/tắt thủ công (vòng đời logic: SCHEDULED nếu now < validFrom; ACTIVE nếu isActive=true && validFrom <= now <= validTo; EXPIRED nếu now > validTo). Mặc định: `true`. |
| `version` | số phiên bản (khóa lạc quan) | số nguyên lớn | Có | Optimistic Lock — chống phát vượt số lượt khi nhiều người cùng dùng. Mỗi lần sửa dòng, số này tăng 1; nếu hai người sửa cùng lúc, người sau bị chặn và phải tải lại (chống ghi đè, chống bán vượt / rút tiền 2 lần). |
| `budget` | ngân sách | số tiền (DECIMAL, không làm tròn sai) | Không | Trần NGÂN SÁCH tính bằng tiền cho cả chiến dịch. |
| `totalDiscountUsed` | tổng tiền đã giảm | số tiền (DECIMAL, không làm tròn sai) | Có | Tổng số tiền đã giảm tích luỹ — đối chiếu với budget. Mặc định: `BigDecimal.ZERO`. |
| `notificationBody` | nội dung thông báo kèm voucher | văn bản dài | Không | Nội dung thông báo đẩy khi tung mã mới. |
| `sponsorType` | bên tài trợ | danh sách cố định `FundingParty` | Có | Ai chi trả khoản giảm: PLATFORM (sàn) hay VENDOR (shop). Giá trị: `PLATFORM` (sàn), `VENDOR` (người bán (shop)), `CO_SPONSOR` (sàn và shop cùng chịu). |
| `platformSharePercent` | phần trăm sàn chịu | số tiền (DECIMAL, không làm tròn sai) | Không | Chỉ khi sponsorType = CO_SPONSOR: % sàn chịu, phần còn lại shop chịu. |
| `categories` | các danh mục | danh sách `VoucherCategory` (quan hệ) | Không | Danh mục ĐƯỢC áp dụng. |
| `products` | các sản phẩm | danh sách `VoucherProduct` (quan hệ) | Không | Sản phẩm ĐƯỢC áp dụng. |
| `excludedCategories` | các danh mục bị loại trừ | danh sách `VoucherExcludedCategory` (quan hệ) | Không | Danh mục BỊ LOẠI TRỪ. |
| `excludedProducts` | các sản phẩm bị loại trừ | danh sách `VoucherExcludedProduct` (quan hệ) | Không | Sản phẩm BỊ LOẠI TRỪ. |

<a id="voucherusage"></a>
## 4.2 `VoucherUsage` — Lịch sử sử dụng voucher

- **Đọc là:** lượt dùng voucher
- **Bảng trong CSDL:** `voucher_usages`
- **Nằm ở sơ đồ lớp:** Sơ đồ 3 — Voucher 3 tầng Shop / Sàn / Freeship & Phạm vi áp dụng (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `VoucherUsage` **trỏ tới** 0 hoặc 1 `Order` (qua cột `VoucherUsage.orderId`).
  - Mỗi `VoucherUsage` **trỏ tới** đúng 1 `CheckoutSession` (qua cột `VoucherUsage.checkoutSessionId`).
  - Mỗi `VoucherUsage` **trỏ tới** đúng 1 `User` (qua cột `VoucherUsage.userId`).
  - Mỗi `VoucherUsage` **trỏ tới** đúng 1 `Voucher` (qua cột `VoucherUsage.voucherId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `voucherId` | mã voucher | số nguyên lớn | Có | FK → vouchers.id — mã nào đã được dùng. **Khóa ngoại** → bảng của `Voucher`. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id — ai dùng. **Khóa ngoại** → bảng của `User`. |
| `checkoutSessionId` | mã phiên thanh toán | số nguyên lớn | Có | FK → checkout_sessions.id — phiên thanh toán đã dùng mã (đơn vị tính lượt dùng). **Khóa ngoại** → bảng của `CheckoutSession`. |
| `orderId` | mã đơn hàng | số nguyên lớn | Không | FK → orders.id. CÓ giá trị với voucher SHOP (đúng 1 đơn con của shop đó); NULL với voucher PLATFORM / FREESHIP / PAYMENT áp toàn phiên (nhiều đơn con). **Khóa ngoại** → bảng của `Order`. |
| `usedAt` | thời điểm dùng | ngày + giờ (có múi giờ) | Có | Thời điểm sử dụng. |
| `voucher` | voucher | đối tượng `Voucher` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Voucher` liên quan (JPA nạp khi cần). Voucher được dùng. |

<a id="uservoucher"></a>
## 4.3 `UserVoucher` — Ví voucher của người dùng

- **Đọc là:** voucher người dùng
- **Bảng trong CSDL:** `user_vouchers`
- **Nằm ở sơ đồ lớp:** Sơ đồ 3 — Voucher 3 tầng Shop / Sàn / Freeship & Phạm vi áp dụng (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `UserVoucher` **trỏ tới** đúng 1 `User` (qua cột `UserVoucher.userId`).
  - Mỗi `Voucher` **sở hữu** nhiều (0..n) `UserVoucher`; `UserVoucher` không tồn tại nếu thiếu `Voucher` (xóa cha thì xóa con) (qua cột `UserVoucher.voucherId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id — chủ ví. **Khóa ngoại** → bảng của `User`. |
| `voucherId` | mã voucher | số nguyên lớn | Có | FK → vouchers.id — mã đã lưu vào ví. **Khóa ngoại** → bảng của `Voucher`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm user bấm "Lưu mã". Hệ thống tự ghi khi tạo dòng. |
| `voucher` | voucher | đối tượng `Voucher` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Voucher` liên quan (JPA nạp khi cần). Chi tiết voucher. |
| `user` | tài khoản người dùng | đối tượng `User` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `User` liên quan (JPA nạp khi cần). Chủ sở hữu. |
| `isUsed` | đã dùng hết lượt | đúng / sai | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Boolean` liên quan (JPA nạp khi cần). true = user đã dùng hết lượt cho phép của mã này. |

<a id="vouchercategory"></a>
## 4.4 `VoucherCategory` — 

- **Đọc là:** danh mục voucher
- **Bảng trong CSDL:** `voucher_categories`
- **Nằm ở sơ đồ lớp:** Sơ đồ 3 — Voucher 3 tầng Shop / Sàn / Freeship & Phạm vi áp dụng (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `VoucherCategory` **trỏ tới** đúng 1 `Category` (qua cột `VoucherCategory.categoryId`).
  - Mỗi `Voucher` **sở hữu** nhiều (0..n) `VoucherCategory`; `VoucherCategory` không tồn tại nếu thiếu `Voucher` (xóa cha thì xóa con) (qua cột `VoucherCategory.voucherId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `voucherId` | mã voucher | số nguyên lớn | Có | FK → vouchers.id — phần 1 của khoá chính. Một phần của khóa chính. **Khóa ngoại** → bảng của `Voucher`. |
| `categoryId` | mã danh mục | số nguyên lớn | Có | FK → categories.id — phần 2 của khoá chính. Một phần của khóa chính. **Khóa ngoại** → bảng của `Category`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm gắn danh mục ĐƯỢC áp dụng voucher (@CreationTimestamp). Hệ thống tự ghi khi tạo dòng. |
| `category` | danh mục | đối tượng `Category` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Category` liên quan (JPA nạp khi cần). Danh mục ĐƯỢC áp dụng voucher. |

<a id="voucherproduct"></a>
## 4.4 `VoucherProduct` — 

- **Đọc là:** sản phẩm voucher
- **Bảng trong CSDL:** `voucher_products`
- **Nằm ở sơ đồ lớp:** Sơ đồ 3 — Voucher 3 tầng Shop / Sàn / Freeship & Phạm vi áp dụng (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `VoucherProduct` **trỏ tới** đúng 1 `Product` (qua cột `VoucherProduct.productId`).
  - Mỗi `Voucher` **sở hữu** nhiều (0..n) `VoucherProduct`; `VoucherProduct` không tồn tại nếu thiếu `Voucher` (xóa cha thì xóa con) (qua cột `VoucherProduct.voucherId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `voucherId` | mã voucher | số nguyên lớn | Có | FK → vouchers.id — phần 1 của khoá chính. Một phần của khóa chính. **Khóa ngoại** → bảng của `Voucher`. |
| `productId` | mã sản phẩm | số nguyên lớn | Có | FK → products.id — phần 2 của khoá chính. Một phần của khóa chính. **Khóa ngoại** → bảng của `Product`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm gắn sản phẩm ĐƯỢC áp dụng voucher (@CreationTimestamp). Hệ thống tự ghi khi tạo dòng. |
| `product` | sản phẩm | đối tượng `Product` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Product` liên quan (JPA nạp khi cần). Sản phẩm ĐƯỢC áp dụng voucher. |

<a id="voucherexcludedcategory"></a>
## 4.4 `VoucherExcludedCategory` — 

- **Đọc là:** danh mục bị loại trừ voucher
- **Bảng trong CSDL:** `voucher_excluded_categories`
- **Nằm ở sơ đồ lớp:** Sơ đồ 3 — Voucher 3 tầng Shop / Sàn / Freeship & Phạm vi áp dụng (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `VoucherExcludedCategory` **trỏ tới** đúng 1 `Category` (qua cột `VoucherExcludedCategory.categoryId`).
  - Mỗi `Voucher` **sở hữu** nhiều (0..n) `VoucherExcludedCategory`; `VoucherExcludedCategory` không tồn tại nếu thiếu `Voucher` (xóa cha thì xóa con) (qua cột `VoucherExcludedCategory.voucherId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `voucherId` | mã voucher | số nguyên lớn | Có | FK → vouchers.id — phần 1 của khoá chính. Một phần của khóa chính. **Khóa ngoại** → bảng của `Voucher`. |
| `categoryId` | mã danh mục | số nguyên lớn | Có | FK → categories.id — phần 2 của khoá chính. Một phần của khóa chính. **Khóa ngoại** → bảng của `Category`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm gắn danh mục BỊ LOẠI TRỪ khỏi voucher (@CreationTimestamp). Hệ thống tự ghi khi tạo dòng. |
| `category` | danh mục | đối tượng `Category` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Category` liên quan (JPA nạp khi cần). Danh mục BỊ LOẠI TRỪ khỏi voucher. |

<a id="voucherexcludedproduct"></a>
## 4.4 `VoucherExcludedProduct` — 

- **Đọc là:** sản phẩm bị loại trừ voucher
- **Bảng trong CSDL:** `voucher_excluded_products`
- **Nằm ở sơ đồ lớp:** Sơ đồ 3 — Voucher 3 tầng Shop / Sàn / Freeship & Phạm vi áp dụng (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `VoucherExcludedProduct` **trỏ tới** đúng 1 `Product` (qua cột `VoucherExcludedProduct.productId`).
  - Mỗi `Voucher` **sở hữu** nhiều (0..n) `VoucherExcludedProduct`; `VoucherExcludedProduct` không tồn tại nếu thiếu `Voucher` (xóa cha thì xóa con) (qua cột `VoucherExcludedProduct.voucherId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `voucherId` | mã voucher | số nguyên lớn | Có | FK → vouchers.id — phần 1 của khoá chính. Một phần của khóa chính. **Khóa ngoại** → bảng của `Voucher`. |
| `productId` | mã sản phẩm | số nguyên lớn | Có | FK → products.id — phần 2 của khoá chính. Một phần của khóa chính. **Khóa ngoại** → bảng của `Product`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm gắn sản phẩm BỊ LOẠI TRỪ khỏi voucher (@CreationTimestamp). Hệ thống tự ghi khi tạo dòng. |
| `product` | sản phẩm | đối tượng `Product` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Product` liên quan (JPA nạp khi cần). Sản phẩm BỊ LOẠI TRỪ khỏi voucher. |

<a id="flashsalecampaign"></a>
## 4.5 `FlashSaleCampaign` — Chiến dịch Flash Sale

- **Đọc là:** chiến dịch bán hàng Flash
- **Bảng trong CSDL:** `flash_sale_campaigns`
- **Nằm ở sơ đồ lớp:** Sơ đồ 9 — Flash Sale, Điểm Genie & Điểm danh (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `FlashSaleCampaign` **trỏ tới** 0 hoặc 1 `User` (qua cột `FlashSaleCampaign.vendorId`) — vai trò *vendor*.
  - Mỗi `FlashSaleCampaign` **sở hữu** nhiều (0..n) `FlashSaleItem`; `FlashSaleItem` không tồn tại nếu thiếu `FlashSaleCampaign` (xóa cha thì xóa con) (qua cột `FlashSaleItem.campaignId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Không | FK → users.id. NULL = chiến dịch TOÀN SÀN. **Khóa ngoại** → bảng của `User`. |
| `title` | tiêu đề | chuỗi ≤ 200 ký tự | Có | Tên chiến dịch: "Flash Sale 12.12". |
| `startsAt` | thời điểm bắt đầu | ngày + giờ (có múi giờ) | Có | Thời điểm bắt đầu. |
| `endsAt` | thời điểm kết thúc | ngày + giờ (có múi giờ) | Có | Thời điểm kết thúc. |
| `maxProducts` | số sản phẩm tối đa | số nguyên | Có | Số sản phẩm tối đa được tham gia (mặc định 20). Mặc định: `20`. |
| `status` | trạng thái | danh sách cố định `FlashSaleStatus` | Có | DRAFT / SCHEDULED / ACTIVE / ENDED. Giá trị: `DRAFT` (bản nháp), `SCHEDULED` (đã lên lịch), `ACTIVE` (đang hoạt động), `ENDED` (đã kết thúc). |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo chiến dịch. Hệ thống tự ghi khi tạo dòng. |
| `items` | các dòng hàng | danh sách `FlashSaleItem` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `FlashSaleItem` liên quan (JPA nạp khi cần). Danh sách sản phẩm trong chiến dịch. |

<a id="flashsaleitem"></a>
## 4.6 `FlashSaleItem` — Sản phẩm trong Flash Sale

- **Đọc là:** dòng hàng bán hàng Flash
- **Bảng trong CSDL:** `flash_sale_items`
- **Nằm ở sơ đồ lớp:** Sơ đồ 9 — Flash Sale, Điểm Genie & Điểm danh (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `FlashSaleCampaign` **sở hữu** nhiều (0..n) `FlashSaleItem`; `FlashSaleItem` không tồn tại nếu thiếu `FlashSaleCampaign` (xóa cha thì xóa con) (qua cột `FlashSaleItem.campaignId`).
  - Mỗi `FlashSaleItem` **trỏ tới** đúng 1 `Product` (qua cột `FlashSaleItem.productId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `campaignId` | mã chiến dịch | số nguyên lớn | Có | FK → flash_sale_campaigns.id. **Khóa ngoại** → bảng của `FlashSaleCampaign`. |
| `productId` | mã sản phẩm | số nguyên lớn | Có | FK → products.id — sản phẩm được giảm giá. **Khóa ngoại** → bảng của `Product`. |
| `flashPrice` | giá Flash Sale | số tiền (DECIMAL, không làm tròn sai) | Có | GIÁ SỐC trong thời gian diễn ra chiến dịch. |
| `flashStock` | số suất Flash Sale | số nguyên | Có | Số lượng GIỚI HẠN dành riêng cho flash sale. |
| `reservedCount` | số đang giữ chỗ | số nguyên | Có | Số suất đã có ĐƠN đang chờ thanh toán (tăng khi tạo Order + StockReservation; giảm khi PAID hoặc giữ chỗ hết hạn). Mặc định: `0`. |
| `soldCount` | số đã bán | số nguyên | Có | Số đã bán trong flash sale — dùng vẽ thanh tiến trình. Mặc định: `0`. |
| `status` | trạng thái duyệt | danh sách cố định `FlashSaleItemStatus` | Có | PENDING / APPROVED / REJECTED — sàn duyệt từng sản phẩm shop đăng ký (UC-PRO-05, TLCN v1.3). Mặc định: `PENDING`. |
| `version` | số phiên bản (khóa lạc quan) | số nguyên lớn | Có | Optimistic Lock — chống race condition khi trừ tồn kho Flash Sale đồng thời. Mỗi lần sửa dòng, số này tăng 1; nếu hai người sửa cùng lúc, người sau bị chặn và phải tải lại (chống ghi đè, chống bán vượt / rút tiền 2 lần). |

<a id="usercoin"></a>
## 4.7 `UserCoin` — Số dư Điểm

- **Đọc là:** Điểm người dùng
- **Bảng trong CSDL:** `user_coins`
- **Nằm ở sơ đồ lớp:** Sơ đồ 9 — Flash Sale, Điểm Genie & Điểm danh (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `User` **sở hữu** 0 hoặc 1 `UserCoin`; `UserCoin` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `UserCoin.userId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `userId` | mã người dùng | số nguyên lớn | Có | KHOÁ CHÍNH đồng thời là FK → users.id (KHÔNG tự tăng) — ví Điểm của khách hàng (1 Điểm = 1₫ khi thanh toán). Một phần của khóa chính. **Khóa ngoại** → bảng của `User`. |
| `balance` | số dư | số nguyên | Có | Số Điểm hiện có. |
| `version` | số phiên bản (khóa lạc quan) | số nguyên lớn | Có | Optimistic Lock — chống race condition khi trừ Điểm đồng thời. Mỗi lần sửa dòng, số này tăng 1; nếu hai người sửa cùng lúc, người sau bị chặn và phải tải lại (chống ghi đè, chống bán vượt / rút tiền 2 lần). |
| `balanceHash` | mã băm chống sửa số dư | chuỗi ≤ 255 ký tự | Có | HMAC-SHA256(userId + balance + secret) — chống can thiệp trực tiếp CSDL. |
| `lastUpdated` | lần biến động gần nhất | ngày + giờ (có múi giờ) | Có | Lần biến động gần nhất. |

<a id="cointransaction"></a>
## 4.8 `CoinTransaction` — Lịch sử biến động Điểm

- **Đọc là:** giao dịch Điểm
- **Bảng trong CSDL:** `coin_transactions`
- **Nằm ở sơ đồ lớp:** Sơ đồ 9 — Flash Sale, Điểm Genie & Điểm danh (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `User` **sở hữu** nhiều (0..n) `CoinTransaction`; `CoinTransaction` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `CoinTransaction.userId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id — Điểm của khách hàng nào. **Khóa ngoại** → bảng của `User`. |
| `amount` | số tiền / số lượng | số nguyên | Có | Số Điểm biến động — DƯƠNG là cộng, ÂM là trừ (cùng quy ước dấu với các sổ ví tiền). |
| `type` | loại | danh sách cố định `CoinTransactionType` | Có | CHECK_IN / ORDER_CASHBACK / REVIEW_BONUS / USED_AT_CHECKOUT / ORDER_REFUND / VOUCHER_REDEEM. Giá trị: `CHECK_IN` (điểm danh), `ORDER_CASHBACK` (hoàn Điểm theo đơn), `REVIEW_BONUS` (thưởng đánh giá), `USED_AT_CHECKOUT` (dùng khi thanh toán), `ORDER_REFUND` (hoàn Điểm khi hủy / hoàn đơn), `VOUCHER_REDEEM` (đổi Điểm lấy voucher). |
| `referenceId` | mã đối tượng liên quan | chuỗi ≤ 50 ký tự | Không | ID đối tượng liên quan (thường là order_id). |
| `label` | nhãn hiển thị | chuỗi ≤ 200 ký tự | Không | Mô tả hiển thị: "Hoàn Điểm đơn hàng #1234". |
| `expiresAt` | thời điểm hết hạn | ngày + giờ (có múi giờ) | Không | Điểm HẾT HẠN sau 12 tháng kể từ ngày nhận (giao diện: "Điểm hết hạn sau 12 tháng"). |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm phát sinh. Hệ thống tự ghi khi tạo dòng. |

<a id="dailycheckin"></a>
## 4.9 `DailyCheckin` — Điểm danh hằng ngày

- **Đọc là:** điểm danh hằng ngày
- **Bảng trong CSDL:** `daily_checkins`
- **Nằm ở sơ đồ lớp:** Sơ đồ 9 — Flash Sale, Điểm Genie & Điểm danh (Nhóm 4)
- **Liên kết với bảng khác:**
  - Mỗi `User` **sở hữu** 0 hoặc 1 `DailyCheckin`; `DailyCheckin` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `DailyCheckin.customerId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `customerId` | mã khách hàng | số nguyên lớn | Có | FK → users.id, UNIQUE — mỗi khách 1 dòng theo dõi. **Khóa ngoại** → bảng của `User`. Giá trị **không được trùng** giữa các dòng. |
| `lastCheckinAt` | lần điểm danh gần nhất | ngày + giờ (có múi giờ) | Có | Thời điểm điểm danh gần nhất. |
| `streakCount` | số ngày điểm danh liên tiếp | số nguyên | Có | Số ngày điểm danh LIÊN TIẾP — đứt 1 ngày là reset về 1. |

# Nhóm 5 — Đơn hàng

<a id="checkoutsession"></a>
## 5.1 `CheckoutSession` — Phiên thanh toán

- **Đọc là:** phiên thanh toán
- **Bảng trong CSDL:** `checkout_sessions`
- **Nằm ở sơ đồ lớp:** Sơ đồ 4 — Phiên thanh toán, Đơn hàng con theo shop & Giữ chỗ tồn kho (Nhóm 5)
- **Liên kết với bảng khác:**
  - Mỗi `VoucherUsage` **trỏ tới** đúng 1 `CheckoutSession` (qua cột `VoucherUsage.checkoutSessionId`).
  - Mỗi `CheckoutSession` **trỏ tới** đúng 1 `User` (qua cột `CheckoutSession.userId`).
  - Mỗi `CheckoutSession` **gom** ít nhất 1 `Order`; `Order` vẫn tồn tại độc lập (qua cột `Order.checkoutSessionId`).
  - Mỗi `PaymentTransaction` **trỏ tới** 0 hoặc 1 `CheckoutSession` (qua cột `PaymentTransaction.checkoutSessionId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id — khách đang thanh toán. **Khóa ngoại** → bảng của `User`. |
| `totalAmount` | tổng tiền cả phiên | số tiền (DECIMAL, không làm tròn sai) | Có | TỔNG tiền của cả phiên (gộp nhiều shop). |
| `status` | trạng thái | danh sách cố định `SessionStatus` | Có | INITIATED / PAYMENT_PENDING / COMPLETED / FAILED / EXPIRED. Giá trị: `INITIATED` (vừa khởi tạo), `PAYMENT_PENDING` (chờ thanh toán), `COMPLETED` (đã hoàn tất), `FAILED` (thất bại), `EXPIRED` (đã hết hạn). |
| `cartSnapshotJson` | ảnh chụp giỏ hàng (JSON) | JSON (JSONB) | Không | Ảnh chụp JSON toàn bộ giỏ hàng lúc bắt đầu thanh toán. |
| `expiresAt` | thời điểm hết hạn | ngày + giờ (có múi giờ) | Có | Hạn chót thanh toán (thường 15 phút) — hết hạn sẽ nhả tồn kho. |
| `completedAt` | thời điểm hoàn tất | ngày + giờ (có múi giờ) | Không | Thời điểm thanh toán thành công. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm bắt đầu phiên. Hệ thống tự ghi khi tạo dòng. |
| `orders` | các đơn hàng | danh sách `Order` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Order` liên quan (JPA nạp khi cần). Các đơn hàng được tách ra theo từng shop. |

<a id="order"></a>
## 5.2 `Order` — Đơn hàng (theo từng shop)

- **Đọc là:** đơn hàng
- **Bảng trong CSDL:** `orders`
- **Nằm ở sơ đồ lớp:** Sơ đồ 4 — Phiên thanh toán, Đơn hàng con theo shop & Giữ chỗ tồn kho (Nhóm 5)
- **Liên kết với bảng khác:**
  - Mỗi `VoucherUsage` **trỏ tới** 0 hoặc 1 `Order` (qua cột `VoucherUsage.orderId`).
  - Mỗi `CheckoutSession` **gom** ít nhất 1 `Order`; `Order` vẫn tồn tại độc lập (qua cột `Order.checkoutSessionId`).
  - Mỗi `Order` **trỏ tới** đúng 1 `User` (qua cột `Order.customerId`).
  - Mỗi `Order` **trỏ tới** đúng 1 `VendorProfile` (qua cột `Order.vendorId`).
  - Mỗi `Order` **sở hữu** ít nhất 1 `OrderItem`; `OrderItem` không tồn tại nếu thiếu `Order` (xóa cha thì xóa con) (qua cột `OrderItem.orderId`).
  - Mỗi `Order` **sở hữu** nhiều (0..n) `OrderDiscount`; `OrderDiscount` không tồn tại nếu thiếu `Order` (xóa cha thì xóa con) (qua cột `OrderDiscount.orderId`).
  - Mỗi `StockReservation` **trỏ tới** 0 hoặc 1 `Order` (qua cột `StockReservation.orderId`).
  - Mỗi `PaymentTransaction` **trỏ tới** 0 hoặc 1 `Order` (qua cột `PaymentTransaction.orderId`).
  - Mỗi `EscrowLedger` **trỏ tới** đúng 1 `Order` (qua cột `EscrowLedger.orderId`).
  - Mỗi `FinancialLedger` **trỏ tới** 0 hoặc 1 `Order` (qua cột `FinancialLedger.orderId`).
  - Mỗi `ShipmentTracking` **trỏ tới** đúng 1 `Order` (qua cột `ShipmentTracking.orderId`).
  - Mỗi `RefundRequest` **trỏ tới** đúng 1 `Order` (qua cột `RefundRequest.orderId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `orderCode` | mã đơn hàng hiển thị | chuỗi ≤ 32 ký tự | Có | Mã đơn công khai cho khách/CSKH tra cứu (vd: TR-20261015-000123) — Hash Index. Giá trị **không được trùng** giữa các dòng. |
| `customerId` | mã khách hàng | số nguyên lớn | Có | FK → users.id — người mua. **Khóa ngoại** → bảng của `User`. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Có | FK → users.id — shop bán (mỗi đơn CHỈ thuộc 1 shop). **Khóa ngoại** → bảng của `VendorProfile`. |
| `vendorProfile` | hồ sơ người bán (shop) | đối tượng `VendorProfile` (quan hệ) | Có | Profile shop bán hàng. |
| `checkoutSessionId` | mã phiên thanh toán | số nguyên lớn | Không | FK → checkout_sessions.id, phiên thanh toán gốc. **Khóa ngoại** → bảng của `CheckoutSession`. |
| `shippingAddressSnapshotJson` | ảnh chụp địa chỉ giao hàng (JSON) | JSON (JSONB) | Có | Ảnh chụp JSON địa chỉ giao hàng tại thời điểm đặt. |
| `subTotal` | tạm tính (tiền hàng) | số tiền (DECIMAL, không làm tròn sai) | Có | Tổng tiền hàng trước mọi khuyến mãi và phí ship. |
| `shippingFee` | phí vận chuyển | số tiền (DECIMAL, không làm tròn sai) | Có | Phí vận chuyển gốc. |
| `discountAmount` | số tiền giảm | số tiền (DECIMAL, không làm tròn sai) | Có | Tổng giảm giá từ voucher của SHOP. Mặc định: `BigDecimal.ZERO`. |
| `platformDiscountAmount` | số tiền giảm do sàn tài trợ | số tiền (DECIMAL, không làm tròn sai) | Có | Tổng giảm giá từ voucher của SÀN. Mặc định: `BigDecimal.ZERO`. |
| `shippingDiscountAmount` | số tiền giảm phí ship | số tiền (DECIMAL, không làm tròn sai) | Có | Tổng giảm từ mã miễn phí vận chuyển. Mặc định: `BigDecimal.ZERO`. |
| `grandTotal` | tổng tiền khách phải trả | số tiền (DECIMAL, không làm tròn sai) | Có | SỐ TIỀN KHÁCH THỰC TRẢ lúc thanh toán (sau tất cả giảm giá) — BẤT BIẾN sau khi thanh toán; phần đã hoàn nằm ở refundedAmount, số còn giữ = netPaidAmount(). |
| `refundedAmount` | số tiền đã hoàn | số tiền (DECIMAL, không làm tròn sai) | Có | Tổng đã hoàn cho khách (mặc định 0) — UI hiển thị "Đã hoàn một phần" khi paymentStatus = PARTIALLY_REFUNDED. Mặc định: `BigDecimal.ZERO`. |
| `paymentMethod` | phương thức thanh toán | danh sách cố định `PaymentMethod` | Không | VNPAY / MOMO / ZALOPAY / VIETQR / INTERNAL_WALLET / COD. Giá trị: `VNPAY` (VNPay), `VIETQR` (VietQR), `COD` (COD). |
| `paymentStatus` | trạng thái thanh toán | danh sách cố định `PaymentStatus` | Không | Bản sao tóm tắt PaymentTransaction.status (denormalize để đọc nhanh). Giá trị: `PENDING` (đang chờ), `REQUIRES_ACTION` (chờ khách xác thực (OTP / 3DS)), `SUCCESS` (thành công), `FAILED` (thất bại), `EXPIRED` (đã hết hạn), `REFUNDED` (đã hoàn tiền), `PARTIALLY_REFUNDED` (đã hoàn một phần), `CANCELLED` (đã hủy). |
| `fundingStatus` | trạng thái tiền vào ký quỹ | danh sách cố định `FundingStatus` | Có | UNFUNDED | PENDING | FUNDED | FAILED — gán UNFUNDED khi tạo đơn.\| PENDING \| FUNDED \| FAILED. Giá trị: `UNFUNDED` (chưa có tiền), `PENDING` (đang chờ), `FUNDED` (đã nạp tiền), `FAILED` (thất bại). |
| `paymentConfirmedAt` | thời điểm khách đã trả tiền | ngày + giờ (có múi giờ) | Không | Thời điểm KHÁCH đã trả tiền: online = IPN thành công; COD = lúc giao hàng, khách đưa tiền mặt cho tài xế. |
| `fundedAt` | thời điểm tiền vào ký quỹ | ngày + giờ (có múi giờ) | Không | Thời điểm tiền VÀO QUỸ ESCROW của sàn: online = paymentConfirmedAt; COD = Staff xác nhận GHN đã chuyển tiền thu hộ (UC-LOG-03) — hai mốc khác nhau với COD. |
| `deliveredAt` | thời điểm giao thành công | ngày + giờ (có múi giờ) | Không | Thời điểm giao hàng thành công (DELIVERED) để tính mốc 7 ngày. |
| `customerConfirmedAt` | thời điểm khách xác nhận đã nhận | ngày + giờ (có múi giờ) | Không | Thời điểm khách hàng bấm 'Đã nhận được hàng'. |
| `completedAt` | thời điểm hoàn tất | ngày + giờ (có múi giờ) | Không | Thời điểm đơn chuyển COMPLETED (giải ngân Escrow). |
| `status` | trạng thái | danh sách cố định `OrderStatus` | Có | Trạng thái đơn con (Phụ lục A — OrderStatus, máy trạng thái ALLOWED bên dưới). Giá trị: `PENDING_PAYMENT` (chờ thanh toán), `PAID` (đã thanh toán), `CONFIRMED` (đã xác nhận), `PREPARING` (đang chuẩn bị), `READY_FOR_PICKUP` (chờ hãng lấy hàng), `DELIVERING` (đang giao), `DELIVERED` (đã giao), `DELIVERY_FAILED` (giao thất bại), `RETURNING` (đang hoàn về), `RETURNED_TO_VENDOR` (đã hoàn về shop), `COMPLETED` (đã hoàn tất), `RETURN_REQUESTED` (khách yêu cầu trả hàng), …. |
| `ghnOrderCode` | mã vận đơn bên GHN | chuỗi ≤ 100 ký tự | Không | Mã vận đơn bên GHN, để tra cứu hành trình. |
| `shippingExpectedAt` | ngày giao dự kiến | ngày + giờ (có múi giờ) | Không | Ngày dự kiến giao hàng. |
| `version` | số phiên bản (khóa lạc quan) | số nguyên lớn | Có | Optimistic Lock — chống 2 luồng cùng đổi trạng thái đơn. Mỗi lần sửa dòng, số này tăng 1; nếu hai người sửa cùng lúc, người sau bị chặn và phải tải lại (chống ghi đè, chống bán vượt / rút tiền 2 lần). |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo đơn. Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Thời điểm cập nhật gần nhất. Hệ thống tự ghi mỗi lần sửa. |
| `isReviewed` | đã đánh giá chưa | đúng / sai | Không | Trường tính toán — khách đã đánh giá đơn này chưa. **Không lưu thành cột**: tính ra mỗi lần đọc. |
| `buyerNote` | lời nhắn cho shop | chuỗi ≤ 500 ký tự | Không | "Lời nhắn cho Shop" khách nhập ở checkout (≤ 500 ký tự) — hiển thị ở UC-LOG-01. |
| `coinUsedCount` | số Điểm đã dùng | số nguyên | Có | Số Điểm khách đã dùng cho đơn này (UC-PRO-10). Mặc định: `0`. |
| `coinDiscountAmount` | số tiền giảm từ Điểm | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền quy đổi tương ứng từ Điểm (1 Điểm = 1₫). Mặc định: `BigDecimal.ZERO`. |
| `items` | các dòng hàng | danh sách `OrderItem` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `OrderItem` liên quan (JPA nạp khi cần). Các dòng sản phẩm trong đơn. |
| `discounts` | các khoản giảm giá | danh sách `OrderDiscount` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `OrderDiscount` liên quan (JPA nạp khi cần). Các khoản giảm giá đã áp. |

<a id="orderitem"></a>
## 5.3 `OrderItem` — Dòng sản phẩm trong đơn

- **Đọc là:** dòng hàng đơn hàng
- **Bảng trong CSDL:** `order_items`
- **Nằm ở sơ đồ lớp:** Sơ đồ 4 — Phiên thanh toán, Đơn hàng con theo shop & Giữ chỗ tồn kho (Nhóm 5)
- **Liên kết với bảng khác:**
  - Mỗi `OrderItem` **trỏ tới** đúng 1 `ProductVariant` (qua cột `OrderItem.productVariantId`).
  - Mỗi `Order` **sở hữu** ít nhất 1 `OrderItem`; `OrderItem` không tồn tại nếu thiếu `Order` (xóa cha thì xóa con) (qua cột `OrderItem.orderId`).
  - Mỗi `RefundRequestItem` **trỏ tới** đúng 1 `OrderItem` (qua cột `RefundRequestItem.orderItemId`).
  - Mỗi `ProductReview` **trỏ tới** đúng 1 `OrderItem` (qua cột `ProductReview.orderItemId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `orderId` | mã đơn hàng | số nguyên lớn | Có | FK → orders.id — thuộc đơn hàng nào. **Khóa ngoại** → bảng của `Order`. |
| `productVariantId` | mã biến thể sản phẩm (SKU) | số nguyên lớn | Có | FK → product_variants.id — biến thể đã mua. **Khóa ngoại** → bảng của `ProductVariant`. |
| `productNameSnapshot` | tên sản phẩm (chụp lại lúc mua) | chuỗi ≤ 255 ký tự | Không | Tên sản phẩm CHỤP LẠI lúc mua (bất biến). |
| `skuSnapshot` | mã SKU (chụp lại lúc mua) | chuỗi ≤ 100 ký tự | Không | Mã SKU chụp lại lúc mua. |
| `quantity` | số lượng | số nguyên | Có | Số lượng mua. |
| `unitPrice` | đơn giá | số tiền (DECIMAL, không làm tròn sai) | Có | Đơn giá CHỐT tại thời điểm mua. |
| `lineTotal` | thành tiền dòng | số tiền (DECIMAL, không làm tròn sai) | Có | Thành tiền dòng = unitPrice × quantity. |
| `proratedDiscount` | phần giảm chung phân bổ cho dòng | số tiền (DECIMAL, không làm tròn sai) | Có | Phần giảm giá chung được chia tỷ lệ cho dòng này. Mặc định: `BigDecimal.ZERO`. |
| `platformDiscount` | phần giảm do sàn tài trợ của dòng | số tiền (DECIMAL, không làm tròn sai) | Có | Phần giảm do SÀN tài trợ, phân bổ cho dòng này. Mặc định: `BigDecimal.ZERO`. |
| `shopDiscount` | phần giảm do shop tài trợ của dòng | số tiền (DECIMAL, không làm tròn sai) | Có | Phần giảm do SHOP tài trợ, phân bổ cho dòng này. Mặc định: `BigDecimal.ZERO`. |
| `grossAmount` | doanh thu gộp | số tiền (DECIMAL, không làm tròn sai) | Có | Doanh thu gộp của dòng, dùng làm căn cứ tính hoa hồng. |
| `commissionRate` | tỷ lệ hoa hồng | tỷ lệ (vd 0.0500 = 5%) | Có | Tỷ lệ hoa hồng CHỤP LẠI lúc bán. VD 0.0550 = 5.5%. |
| `commissionAmount` | tiền hoa hồng của dòng | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền hoa hồng sàn thu = grossAmount × commissionRate. |
| `vendorReceivable` | số tiền shop nhận cho dòng | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền shop thực nhận cho dòng này. |
| `coinDiscount` | phần giảm từ Điểm của dòng | số tiền (DECIMAL, không làm tròn sai) | Có | Phần giảm từ Điểm, phân bổ cho dòng này (sàn chịu — shop vẫn nhận đủ). Mặc định: `BigDecimal.ZERO`. |
| `productVariant` | biến thể sản phẩm | đối tượng `ProductVariant` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `ProductVariant` liên quan (JPA nạp khi cần). Biến thể sản phẩm. |

<a id="orderdiscount"></a>
## 5.4 `OrderDiscount` — Chi tiết các mã đã áp cho đơn

- **Đọc là:** giảm giá đơn hàng
- **Bảng trong CSDL:** `order_discounts`
- **Nằm ở sơ đồ lớp:** Sơ đồ 4 — Phiên thanh toán, Đơn hàng con theo shop & Giữ chỗ tồn kho (Nhóm 5)
- **Liên kết với bảng khác:**
  - Mỗi `OrderDiscount` **trỏ tới** 0 hoặc 1 `Voucher` (qua cột `OrderDiscount.voucherId`).
  - Mỗi `Order` **sở hữu** nhiều (0..n) `OrderDiscount`; `OrderDiscount` không tồn tại nếu thiếu `Order` (xóa cha thì xóa con) (qua cột `OrderDiscount.orderId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `orderId` | mã đơn hàng | số nguyên lớn | Có | FK → orders.id. **Khóa ngoại** → bảng của `Order`. |
| `voucherId` | mã voucher | số nguyên lớn | Không | FK → vouchers.id. NULL khi là giảm giá từ Điểm. **Khóa ngoại** → bảng của `Voucher`. |
| `voucherCodeSnapshot` | mã voucher chụp lại | chuỗi ≤ 50 ký tự | Không | Mã voucher CHỤP LẠI, phòng khi voucher bị xoá. |
| `discountAmount` | số tiền giảm | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền mã này đã giảm. |
| `voucherType` | loại voucher | danh sách cố định `VoucherType` | Không | PLATFORM / SHOP / FREESHIP / PAYMENT. NULL = dòng giảm giá từ Điểm (xem Order.coinDiscountAmount). Giá trị: `SHOP` (shop), `PLATFORM` (sàn), `FREESHIP` (miễn phí vận chuyển). |
| `fundedBy` | bên chịu tiền giảm | danh sách cố định `FundingParty` | Không | AI TRẢ TIỀN: PLATFORM hay VENDOR (enum, cùng enum với Voucher.sponsorType). Giá trị: `PLATFORM` (sàn), `VENDOR` (người bán (shop)), `CO_SPONSOR` (sàn và shop cùng chịu). |

<a id="stockreservation"></a>
## 5.5 `StockReservation` — Giữ chỗ tồn kho

- **Đọc là:** giữ chỗ tồn kho
- **Bảng trong CSDL:** `stock_reservations`
- **Nằm ở sơ đồ lớp:** Sơ đồ 4 — Phiên thanh toán, Đơn hàng con theo shop & Giữ chỗ tồn kho (Nhóm 5)
- **Liên kết với bảng khác:**
  - Mỗi `StockReservation` **trỏ tới** 0 hoặc 1 `Order` (qua cột `StockReservation.orderId`).
  - Mỗi `StockReservation` **trỏ tới** đúng 1 `ProductVariant` (qua cột `StockReservation.productVariantId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `productVariantId` | mã biến thể sản phẩm (SKU) | số nguyên lớn | Có | FK → product_variants.id — giữ chỗ cho biến thể nào. **Khóa ngoại** → bảng của `ProductVariant`. |
| `orderId` | mã đơn hàng | số nguyên lớn | Có | FK → orders.id — giữ cho đơn hàng nào. **Khóa ngoại** → bảng của `Order`. |
| `reservedQuantity` | số lượng đang giữ chỗ | số nguyên | Có | Số lượng đang giữ. |
| `status` | trạng thái | danh sách cố định `StockReservationStatus` | Có | ACTIVE (đang giữ) / CONFIRMED (đã chốt) / EXPIRED / RELEASED. Giá trị: `ACTIVE` (đang hoạt động), `CONFIRMED` (đã xác nhận), `EXPIRED` (đã hết hạn), `RELEASED` (đã giải ngân). |
| `version` | số phiên bản (khóa lạc quan) | số nguyên lớn | Có | Optimistic Lock — chống Sweeper và Webhook cùng đổi trạng thái 1 lượt giữ chỗ. Mỗi lần sửa dòng, số này tăng 1; nếu hai người sửa cùng lúc, người sau bị chặn và phải tải lại (chống ghi đè, chống bán vượt / rút tiền 2 lần). |
| `expiresAt` | thời điểm hết hạn | ngày + giờ (có múi giờ) | Có | Hạn giữ chỗ — quá hạn sẽ tự nhả. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm bắt đầu giữ. Hệ thống tự ghi khi tạo dòng. |
| `releasedAt` | thời điểm giải ngân | ngày + giờ (có múi giờ) | Không | Thời điểm thực sự nhả chỗ. |

# Nhóm 6 — Thanh toán & ký quỹ

<a id="paymenttransaction"></a>
## 6.1 `PaymentTransaction` — Giao dịch thanh toán

- **Đọc là:** giao dịch thanh toán
- **Bảng trong CSDL:** `payment_transactions`
- **Nằm ở sơ đồ lớp:** Sơ đồ 5 — Thanh toán VNPay / VietQR, Ký quỹ Escrow, Ví shop, Rút tiền & Sổ cái (Nhóm 6)
- **Liên kết với bảng khác:**
  - Mỗi `PaymentTransaction` **trỏ tới** 0 hoặc 1 `CheckoutSession` (qua cột `PaymentTransaction.checkoutSessionId`).
  - Mỗi `PaymentTransaction` **trỏ tới** 0 hoặc 1 `Order` (qua cột `PaymentTransaction.orderId`).
  - Mỗi `PaymentRefund` **trỏ tới** đúng 1 `PaymentTransaction` (qua cột `PaymentRefund.paymentTransactionId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `orderId` | mã đơn hàng | số nguyên lớn | Không | FK → orders.id (khi thanh toán cho 1 đơn lẻ). **Khóa ngoại** → bảng của `Order`. |
| `checkoutSessionId` | mã phiên thanh toán | số nguyên lớn | Không | FK → checkout_sessions.id (khi trả gộp nhiều shop). **Khóa ngoại** → bảng của `CheckoutSession`. |
| `idempotencyKey` | khóa chống trùng giao dịch | chuỗi ≤ 255 ký tự | Có | KHOÁ CHỐNG TRÙNG — chìa khoá ngăn trừ tiền 2 lần. Giá trị **không được trùng** giữa các dòng. |
| `amount` | số tiền / số lượng | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền giao dịch. |
| `refundedAmount` | số tiền đã hoàn | số tiền (DECIMAL, không làm tròn sai) | Có | Tổng đã hoàn (cho phép hoàn nhiều lần từng phần). Mặc định: `BigDecimal.ZERO`. |
| `gatewayTransactionId` | mã giao dịch bên cổng thanh toán | chuỗi ≤ 255 ký tự | Không | VNPAY = vnp_TransactionNo; VIETQR = `id` giao dịch SePay (UNIQUE — chống trùng webhook); COD = NULL. |
| `paymentMethod` | phương thức thanh toán | danh sách cố định `PaymentMethod` | Không | VNPAY / MOMO / ZALOPAY / VIETQR / INTERNAL_WALLET / COD. Giá trị: `VNPAY` (VNPay), `VIETQR` (VietQR), `COD` (COD). |
| `status` | trạng thái | danh sách cố định `PaymentStatus` | Có || Có | UNFUNDED | PENDING | FUNDED | FAILED — gán UNFUNDED khi tạo đơn./ REQUIRES_ACTION / SUCCESS / FAILED / EXPIRED / REFUNDED / PARTIALLY_REFUNDED / CANCELLED. Giá trị: `PENDING` (đang chờ), `REQUIRES_ACTION` (chờ khách xác thực (OTP / 3DS)), `SUCCESS` (thành công), `FAILED` (thất bại), `EXPIRED` (đã hết hạn), `REFUNDED` (đã hoàn tiền), `PARTIALLY_REFUNDED` (đã hoàn một phần), `CANCELLED` (đã hủy). |
| `webhookPayload` | dữ liệu gốc webhook gửi về | văn bản dài | Không | JSON THÔ cổng thanh toán gửi về, giữ nguyên để đối soát. |
| `version` | số phiên bản (khóa lạc quan) | số nguyên lớn | Có | Optimistic Lock — chống xử lý song song 1 giao dịch. Mỗi lần sửa dòng, số này tăng 1; nếu hai người sửa cùng lúc, người sau bị chặn và phải tải lại (chống ghi đè, chống bán vượt / rút tiền 2 lần). |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm khởi tạo giao dịch. Hệ thống tự ghi khi tạo dòng. |

<a id="paymentrefund"></a>
## 6.2 `PaymentRefund` — Giao dịch hoàn tiền qua cổng

- **Đọc là:** hoàn tiền thanh toán
- **Bảng trong CSDL:** `payment_refunds`
- **Nằm ở sơ đồ lớp:** Sơ đồ 5 — Thanh toán VNPay / VietQR, Ký quỹ Escrow, Ví shop, Rút tiền & Sổ cái (Nhóm 6)
- **Liên kết với bảng khác:**
  - Mỗi `PaymentRefund` **trỏ tới** đúng 1 `PaymentTransaction` (qua cột `PaymentRefund.paymentTransactionId`).
  - Mỗi `PaymentRefund` **trỏ tới** đúng 1 `RefundRequest` (qua cột `PaymentRefund.refundRequestId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `paymentTransactionId` | mã giao dịch thanh toán | số nguyên lớn | Có | FK → payment_transactions.id — hoàn cho giao dịch gốc nào. **Khóa ngoại** → bảng của `PaymentTransaction`. |
| `refundRequestId` | mã yêu cầu hoàn tiền | số nguyên lớn | Có | FK → refund_requests.id — xuất phát từ yêu cầu hoàn nào. **Khóa ngoại** → bảng của `RefundRequest`. |
| `gatewayRefundId` | mã hoàn tiền bên cổng | chuỗi ≤ 255 ký tự | Có | Mã hoàn tiền bên cổng thanh toán cấp. |
| `amount` | số tiền / số lượng | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền hoàn của lần này. |
| `status` | trạng thái | danh sách cố định `PaymentRefundStatus` | Có || Có | UNFUNDED | PENDING | FUNDED | FAILED — gán UNFUNDED khi tạo đơn./ SUCCESS / FAILED. Giá trị: `PENDING` (đang chờ), `SUCCESS` (thành công), `FAILED` (thất bại). |

<a id="escrowledger"></a>
## 6.3 `EscrowLedger` — Sổ ký quỹ (tiền tạm giữ)

- **Đọc là:** sổ ký quỹ
- **Bảng trong CSDL:** `escrow_ledgers`
- **Nằm ở sơ đồ lớp:** Sơ đồ 5 — Thanh toán VNPay / VietQR, Ký quỹ Escrow, Ví shop, Rút tiền & Sổ cái (Nhóm 6)
- **Liên kết với bảng khác:**
  - Mỗi `EscrowLedger` **trỏ tới** đúng 1 `VendorProfile` (qua cột `EscrowLedger.vendorId`).
  - Mỗi `EscrowLedger` **trỏ tới** đúng 1 `Order` (qua cột `EscrowLedger.orderId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `orderId` | mã đơn hàng | số nguyên lớn | Có | FK → orders.id, UNIQUE — mỗi đơn đúng 1 bản ghi ký quỹ. **Khóa ngoại** → bảng của `Order`. Giá trị **không được trùng** giữa các dòng. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Có | FK → users.id — tiền này sẽ về shop nào. **Khóa ngoại** → bảng của `VendorProfile`. |
| `grossAmount` | doanh thu gộp | số tiền (DECIMAL, không làm tròn sai) | Có | GrossAmount = Order.subTotal - Order.discountAmount (KHÔNG gồm shippingFee). |
| `commissionFee` | tiền hoa hồng | số tiền (DECIMAL, không làm tròn sai) | Có | Tiền hoa hồng sàn thu = Σ OrderItem.commissionAmount của đơn (khớp tuyệt đối từng dòng). |
| `platformShippingFee` | phí ship sàn trả hộ | số tiền (DECIMAL, không làm tròn sai) | Có | Phần phí ship sàn trả hộ. |
| `shopShippingSubsidy` | phí ship shop hỗ trợ | số tiền (DECIMAL, không làm tròn sai) | Có | Phần phí ship shop tự hỗ trợ khách. |
| `netAmount` | số tiền thực nhận | số tiền (DECIMAL, không làm tròn sai) | Có | SỐ TIỀN THỰC shop được nhận sau tất cả khoản trừ. |
| `refundedAmount` | số tiền đã hoàn | số tiền (DECIMAL, không làm tròn sai) | Có | Tổng tiền hàng đã hoàn một phần cho khách (mặc định 0) — grossAmount gốc = grossAmount + refundedAmount. Mặc định: `BigDecimal.ZERO`. |
| `fundingSource` | nguồn tiền nạp ký quỹ | danh sách cố định `FundingSource` | Không | NULLABLE (NULL tới lần nạp đầu) — WALLET \| ONLINE_GATEWAY \| COD_CASH \| SPLIT (SPLIT = có từ 2 nguồn khác nhau trở lên). Giá trị: `ONLINE_GATEWAY` (cổng thanh toán online), `COD_CASH` (tiền COD đã đối soát). |
| `gatewayFundedAmount` | số tiền nạp qua cổng online | số tiền (DECIMAL, không làm tròn sai) | Có | Phần trả online: VNPay hoặc chuyển khoản VietQR (xác nhận qua SePay). Mặc định: `BigDecimal.ZERO`. |
| `codFundedAmount` | số tiền nạp từ COD | số tiền (DECIMAL, không làm tròn sai) | Có | Tiền COD đã thu nộp (khi kế toán confirm SETTLED). Mặc định: `BigDecimal.ZERO`. |
| `status` | trạng thái | danh sách cố định `EscrowStatus` | Có | PENDING_FUNDING \| HELD \| FROZEN \| RELEASED \| REFUNDED \| DISPUTED. Giá trị: `PENDING_FUNDING` (chờ tiền vào ký quỹ), `HELD` (đang giữ), `FROZEN` (bị phong tỏa), `RELEASED` (đã giải ngân), `REFUNDED` (đã hoàn tiền), `DISPUTED` (đang tranh chấp). |
| `fundedAt` | thời điểm tiền vào ký quỹ | ngày + giờ (có múi giờ) | Không | ← THÊM: Thời điểm fund đủ (chuyển PENDING_FUNDING → HELD). |
| `codRemittedAt` | thời điểm nhận tiền COD từ hãng | ngày + giờ (có múi giờ) | Không | ← THÊM: Thời điểm kế toán xác nhận đã nhận COD. |
| `releaseEligibleAt` | thời điểm đủ điều kiện giải ngân | ngày + giờ (có múi giờ) | Không | Thời điểm ĐỦ ĐIỀU KIỆN giải ngân (hết hạn khiếu nại). |
| `releasedAt` | thời điểm giải ngân | ngày + giờ (có múi giờ) | Không | Thời điểm THỰC SỰ giải ngân. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo bút toán ký quỹ. Hệ thống tự ghi khi tạo dòng. |

<a id="vendorwallet"></a>
## 6.4 `VendorWallet` — Ví tiền của người bán

- **Đọc là:** ví người bán (shop)
- **Bảng trong CSDL:** `vendor_wallets`
- **Nằm ở sơ đồ lớp:** Sơ đồ 5 — Thanh toán VNPay / VietQR, Ký quỹ Escrow, Ví shop, Rút tiền & Sổ cái (Nhóm 6)
- **Liên kết với bảng khác:**
  - Mỗi `VendorProfile` **sở hữu** 0 hoặc 1 `VendorWallet`; `VendorWallet` không tồn tại nếu thiếu `VendorProfile` (xóa cha thì xóa con) (qua cột `VendorWallet.vendorId`).
  - Mỗi `VendorWallet` **sở hữu** nhiều (0..n) `WalletTransaction`; `WalletTransaction` không tồn tại nếu thiếu `VendorWallet` (xóa cha thì xóa con) (qua cột `WalletTransaction.walletId`).
  - Mỗi `WithdrawalRequest` **trỏ tới** đúng 1 `VendorWallet` (qua cột `WithdrawalRequest.walletId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Có | FK → users.id, UNIQUE — mỗi shop đúng 1 ví. **Khóa ngoại** → bảng của `VendorProfile`. Giá trị **không được trùng** giữa các dòng. |
| `balance` | số dư | số tiền (DECIMAL, không làm tròn sai) | Có | Số dư KHẢ DỤNG — tiền shop có thể rút ngay. Mặc định: `BigDecimal.ZERO`. |
| `pendingWithdrawal` | tiền đang chờ rút | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền đang bị GIỮ do có lệnh rút chờ xử lý. Mặc định: `BigDecimal.ZERO`. |
| `balanceHash` | mã băm chống sửa số dư | chuỗi ≤ 255 ký tự | Có | HMAC-SHA256(vendorId + balance + pendingWithdrawal + secret) — service verify trước mỗi UPDATE và ghi lại sau UPDATE, chống sửa trực tiếp CSDL. |
| `version` | số phiên bản (khóa lạc quan) | số nguyên lớn | Có | Optimistic Lock — LÁ CHẮN CHỐNG RÚT TIỀN 2 LẦN. Mỗi lần sửa dòng, số này tăng 1; nếu hai người sửa cùng lúc, người sau bị chặn và phải tải lại (chống ghi đè, chống bán vượt / rút tiền 2 lần). |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo ví. Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Thời điểm biến động số dư gần nhất. Hệ thống tự ghi mỗi lần sửa. |

<a id="wallettransaction"></a>
## 6.5 `WalletTransaction` — Lịch sử biến động ví

- **Đọc là:** giao dịch ví
- **Bảng trong CSDL:** `wallet_transactions`
- **Nằm ở sơ đồ lớp:** Sơ đồ 5 — Thanh toán VNPay / VietQR, Ký quỹ Escrow, Ví shop, Rút tiền & Sổ cái (Nhóm 6)
- **Liên kết với bảng khác:**
  - Mỗi `VendorWallet` **sở hữu** nhiều (0..n) `WalletTransaction`; `WalletTransaction` không tồn tại nếu thiếu `VendorWallet` (xóa cha thì xóa con) (qua cột `WalletTransaction.walletId`).
  - Mỗi `WithdrawalRequest` **trỏ tới** 0 hoặc 1 `WalletTransaction` (qua cột `WithdrawalRequest.walletTransactionId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `walletId` | mã ví | số nguyên lớn | Có | FK → vendor_wallets.id — biến động của ví nào. **Khóa ngoại** → bảng của `VendorWallet`. |
| `type` | loại | danh sách cố định `WalletTransactionType` | Có | CREDIT_ESCROW_RELEASE / DEBIT_REFUND / DEBIT_WITHDRAWAL / CREDIT_ADJUSTMENT / DEBIT_ADJUSTMENT / CREDIT_TOPUP / DEBIT_PURCHASE (7 giá trị). Giá trị: `CREDIT_ESCROW_RELEASE` (cộng tiền giải ngân ký quỹ), `DEBIT_REFUND` (trừ tiền hoàn cho khách), `DEBIT_WITHDRAWAL` (trừ tiền rút), `CREDIT_ADJUSTMENT` (cộng tiền điều chỉnh tay), `DEBIT_ADJUSTMENT` (trừ tiền điều chỉnh tay). |
| `amount` | số tiền / số lượng | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền biến động CÓ DẤU: DƯƠNG = CREDIT (cộng vào ví), ÂM = DEBIT (trừ khỏi ví) — quy ước chung của mọi sổ ví. |
| `balanceBefore` | số dư trước giao dịch | số tiền (DECIMAL, không làm tròn sai) | Có | SỐ DƯ TRƯỚC giao dịch — bất biến kiểm toán: balanceBefore + amount = balanceAfter. |
| `balanceAfter` | số dư sau giao dịch | số tiền (DECIMAL, không làm tròn sai) | Có | SỐ DƯ SAU giao dịch — dấu vết kiểm toán. |
| `referenceId` | mã đối tượng liên quan | chuỗi ≤ 255 ký tự | Không | ID của đối tượng liên quan (order_id, withdrawal_id...). |
| `referenceType` | loại đối tượng liên quan | danh sách cố định `WalletReferenceType` | Không | Loại đối tượng: ORDER / WITHDRAWAL / REFUND / ADJUSTMENT. Giá trị: `ORDER` (đơn hàng), `WITHDRAWAL` (rút tiền), `REFUND` (hoàn tiền), `ADJUSTMENT` (điều chỉnh). |
| `note` | ghi chú | chuỗi ≤ 500 ký tự | Không | Ghi chú, đặc biệt quan trọng với các khoản điều chỉnh tay. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm phát sinh. Hệ thống tự ghi khi tạo dòng. |

<a id="withdrawalrequest"></a>
## 6.6 `WithdrawalRequest` — Yêu cầu rút tiền

- **Đọc là:** yêu cầu rút tiền
- **Bảng trong CSDL:** `withdrawal_requests`
- **Nằm ở sơ đồ lớp:** Sơ đồ 5 — Thanh toán VNPay / VietQR, Ký quỹ Escrow, Ví shop, Rút tiền & Sổ cái (Nhóm 6)
- **Liên kết với bảng khác:**
  - Mỗi `WithdrawalRequest` **trỏ tới** 0 hoặc 1 `WalletTransaction` (qua cột `WithdrawalRequest.walletTransactionId`).
  - Mỗi `WithdrawalRequest` **trỏ tới** đúng 1 `VendorWallet` (qua cột `WithdrawalRequest.walletId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `walletId` | mã ví | số nguyên lớn | Có | FK mềm theo ownerType → vendor_wallets.id / customer_wallets.id / shippers.id. **Khóa ngoại** → bảng của `VendorWallet`. |
| `amount` | số tiền / số lượng | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền yêu cầu rút. |
| `bankAccountName` | tên chủ tài khoản | chuỗi ≤ 255 ký tự | Có | Tên chủ TK ngân hàng — CHỤP LẠI lúc gửi yêu cầu. |
| `bankAccountNumber` | số tài khoản | chuỗi ≤ 50 ký tự | Có | Số TK ngân hàng — chụp lại. |
| `bankName` | tên ngân hàng | chuỗi ≤ 100 ký tự | Có | Tên ngân hàng — chụp lại. |
| `status` | trạng thái | danh sách cố định `WithdrawalStatus` | Có || Có | UNFUNDED | PENDING | FUNDED | FAILED — gán UNFUNDED khi tạo đơn./ APPROVED / PROCESSING / COMPLETED / REJECTED. Giá trị: `PENDING` (đang chờ), `APPROVED` (đã duyệt), `PROCESSING` (đang xử lý), `COMPLETED` (đã hoàn tất), `REJECTED` (bị từ chối). |
| `rejectionReason` | lý do từ chối | chuỗi ≤ 500 ký tự | Không | Lý do từ chối, để shop biết đường xử lý. |
| `processedByAdminId` | mã nhân viên xử lý | số nguyên lớn | Không | FK mềm → users.id, admin nào đã xử lý. Lưu mã của đối tượng khác nhưng không ràng buộc cứng ở CSDL (khóa ngoại mềm). |
| `walletTransactionId` | mã giao dịch ví tương ứng | số nguyên lớn | Không | FK → wallet_transactions.id, bút toán tương ứng. **Khóa ngoại** → bảng của `WalletTransaction`. |
| `requestedAt` | thời điểm gửi yêu cầu | ngày + giờ (có múi giờ) | Có | Thời điểm shop gửi yêu cầu. |
| `processedAt` | thời điểm xử lý xong | ngày + giờ (có múi giờ) | Không | Thời điểm admin xử lý xong. |

<a id="financialledger"></a>
## 6.7 `FinancialLedger` — Sổ cái tài chính (chỉ ghi thêm)

- **Đọc là:** sổ tài chính
- **Bảng trong CSDL:** `financial_ledger`
- **Nằm ở sơ đồ lớp:** Sơ đồ 5 — Thanh toán VNPay / VietQR, Ký quỹ Escrow, Ví shop, Rút tiền & Sổ cái (Nhóm 6)
- **Liên kết với bảng khác:**
  - Mỗi `FinancialLedger` **trỏ tới** 0 hoặc 1 `Order` (qua cột `FinancialLedger.orderId`).
  - Mỗi `FinancialLedger` **trỏ tới** 0 hoặc 1 `VendorProfile` (qua cột `FinancialLedger.vendorId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `seq` | số thứ tự bút toán (khóa chính) | số nguyên lớn | Có | Số THỨ TỰ bút toán — tăng dần tuyệt đối, không bao giờ tái sử dụng. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `eventType` | loại sự kiện tài chính | danh sách cố định `FinancialEventType` | Có | ESCROW_IN, COMMISSION, PLATFORM_VOUCHER, FREESHIP_COST, COIN_COST, ESCROW_RELEASE, PAYOUT_ACCRUE, PAYOUT_SETTLE, REFUND, GATEWAY_FEE. Giá trị: `ESCROW_IN` (tiền vào ký quỹ), `COMMISSION` (hoa hồng), `PLATFORM_VOUCHER` (chi phí voucher sàn), `FREESHIP_COST` (chi phí miễn phí ship), `COIN_COST` (chi phí Điểm), `ESCROW_RELEASE` (giải ngân ký quỹ), `PAYOUT_ACCRUE` (ghi nhận nợ phải trả shop), `PAYOUT_SETTLE` (đã chi trả cho shop), `REFUND` (hoàn tiền), `GATEWAY_FEE` (phí cổng thanh toán). |
| `referenceId` | mã đối tượng liên quan | chuỗi ≤ 255 ký tự | Có | ID đối tượng gốc: order_id / payout_id / refund_id. |
| `orderId` | mã đơn hàng | số nguyên lớn | Không | FK → orders.id khi bút toán gắn với một đơn (ESCROW_IN, COMMISSION, REFUND…) — lọc sổ cái theo đơn. **Khóa ngoại** → bảng của `Order`. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Không | FK → users.id của shop khi bút toán gắn với một shop (giải ngân, phí) — đối soát theo shop. **Khóa ngoại** → bảng của `VendorProfile`. |
| `account` | tài khoản kế toán | chuỗi ≤ 100 ký tự | Có | TÊN TÀI KHOẢN kế toán bị tác động. |
| `debit` | số tiền ghi Nợ | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền ghi NỢ (bên trái). |
| `credit` | số tiền ghi Có | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền ghi CÓ (bên phải). |
| `balanceAfter` | số dư sau giao dịch | số tiền (DECIMAL, không làm tròn sai) | Có | Số dư của tài khoản đó sau bút toán này. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm ghi sổ. Hệ thống tự ghi khi tạo dòng. |

## 6.8 `CodRemittance` — Phiên đối soát tiền thu hộ COD

- **Đọc là:** phiên đối soát COD
- **Bảng trong CSDL:** `cod_remittances`
- **Nằm ở sơ đồ lớp:** Sơ đồ 5 — Thanh toán VNPay / VietQR, Ký quỹ Escrow, Ví shop, Rút tiền & Sổ cái (Nhóm 6)
- **Dùng để làm gì:** mỗi lần hãng vận chuyển chuyển tiền thu hộ về tài khoản sàn (SePay báo) tạo một phiên; nhân viên tài chính tải bảng kê để đối chiếu từng đơn COD rồi xác nhận (màn Admin › Đối soát COD, UC-LOG-03). Thêm ở TLCN v1.3.
- **Liên kết với bảng khác:** không có khóa ngoại cứng; đơn khớp lưu trong `linesJson`.

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã phiên (khóa chính) | số nguyên lớn | Có | Khóa chính, CSDL tự tăng. |
| `remittanceCode` | mã phiên hiển thị | chuỗi ≤ 30 ký tự | Có | "COD-20260612". Không trùng. |
| `carrierType` | hãng vận chuyển | danh sách cố định `CarrierType` | Có | Hãng chuyển tiền về: GHN / GHTK / VIETTEL_POST / JNT_EXPRESS. |
| `receivedAmount` | số tiền đã nhận | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền thực vào tài khoản sàn. |
| `bankReference` | mã giao dịch ngân hàng | chuỗi ≤ 100 ký tự | Có | Mã do SePay gửi; không trùng để webhook gửi lại không tạo phiên mới. |
| `receivedAt` | thời điểm nhận tiền | ngày + giờ (có múi giờ) | Có | Thời điểm tiền vào. |
| `status` | trạng thái | danh sách cố định `CodRemittanceStatus` | Có | WAITING (chờ bảng kê) / DONE (đã đối soát). Mặc định: `WAITING`. |
| `statementFileName` | tên tệp bảng kê | chuỗi ≤ 255 ký tự | Không | Tệp CSV đã dùng để đối chiếu. |
| `linesJson` | kết quả đối chiếu | JSON | Không | Từng dòng: mã vận đơn, đơn hàng, số tiền, phí, kết quả (Khớp / Lệch / Không tìm thấy). |
| `reconciledByStaffId` | nhân viên xác nhận | số nguyên lớn | Không | FK mềm → users.id. |
| `reconciledAt` | thời điểm xác nhận | ngày + giờ (có múi giờ) | Không | Lúc bấm "Xác nhận đối soát". |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Hệ thống tự ghi khi tạo dòng. |

# Nhóm 7 — Vận chuyển

<a id="shipmenttracking"></a>
## 7.1 `ShipmentTracking` — Vận đơn

- **Đọc là:** theo dõi vận đơn
- **Bảng trong CSDL:** `shipment_trackings`
- **Nằm ở sơ đồ lớp:** Sơ đồ 6 — Vận đơn 3PL, Cấu hình vận chuyển của shop, Trả hàng & Hoàn tiền (Nhóm 7–8)
- **Liên kết với bảng khác:**
  - Mỗi `ShipmentTracking` **trỏ tới** đúng 1 `Order` (qua cột `ShipmentTracking.orderId`).
  - Mỗi `ShipmentTracking` **trỏ tới** 0 hoặc 1 `RefundRequest` (qua cột `ShipmentTracking.refundRequestId`).
  - Mỗi `ShipmentTracking` **sở hữu** nhiều (0..n) `ShipmentEvent`; `ShipmentEvent` không tồn tại nếu thiếu `ShipmentTracking` (xóa cha thì xóa con) (qua cột `ShipmentEvent.shipmentId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `orderId` | mã đơn hàng | số nguyên lớn | Có | FK → orders.id — 1 vận đơn chiều đi (OUTBOUND) / đơn + 0..N vận đơn hoàn (RETURN, mỗi yêu cầu trả hàng 1 vận đơn). **Khóa ngoại** → bảng của `Order`. |
| `direction` | chiều vận đơn | danh sách cố định `ShipmentDirection` | Có | OUTBOUND (shop → khách) / RETURN (khách → shop, Reverse Logistics UC-SUP-02). Giá trị: `OUTBOUND` (chiều đi (shop → khách)), `RETURN` (chiều về (khách → shop)). |
| `refundRequestId` | mã yêu cầu hoàn tiền | số nguyên lớn | Không | FK → refund_requests.id; BẮT BUỘC khi direction = RETURN (CHECK), NULL khi OUTBOUND. **Khóa ngoại** → bảng của `RefundRequest`. |
| `trackingCode` | mã vận đơn | chuỗi ≤ 100 ký tự | Không | Mã vận đơn khách dùng để tra cứu. |
| `carrierType` | hãng vận chuyển | danh sách cố định `CarrierType` | Không | Hãng vận chuyển được chọn khi điều phối (UC-LOG-15): GHN / GHTK / VIETTEL_POST / JNT_EXPRESS / GENIE_EXPRESS. Giá trị: `GHN` (GHN), `GHTK` (GHTK), `VIETTEL_POST` (ViettelPost), `JNT_EXPRESS` (J&T Express), `GENIE_EXPRESS` (Genie Express). |
| `carrierOrderId` | mã đơn bên hãng vận chuyển | chuỗi ≤ 100 ký tự | Không | Mã đơn bên hệ thống hãng vận chuyển. |
| `status` | trạng thái | danh sách cố định `ShipmentTrackingStatus` | Có | AWAITING_PICKUP → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED (hoặc FAILED/RETURNING/RETURNED/LOST). Giá trị: `AWAITING_PICKUP` (chờ lấy hàng), `PICKED_UP` (đã lấy hàng), `IN_TRANSIT` (đang trung chuyển), `OUT_FOR_DELIVERY` (đang đi giao), `DELIVERED` (đã giao), `FAILED` (thất bại), `RETURNING` (đang hoàn về), `RETURNED` (đã trả về), `LOST` (thất lạc). |
| `estimatedDeliveryAt` | ngày giao dự kiến | ngày + giờ (có múi giờ) | Không | Ngày giao DỰ KIẾN hãng vận chuyển cam kết. |
| `actualDeliveredAt` | ngày giao thực tế | ngày + giờ (có múi giờ) | Không | Ngày giao THỰC TẾ. |
| `events` | các sự kiện | danh sách `ShipmentEvent` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `ShipmentEvent` liên quan (JPA nạp khi cần). Nhật ký hành trình chi tiết. |

<a id="shipmentevent"></a>
## 7.2 `ShipmentEvent` — Sự kiện hành trình

- **Đọc là:** sự kiện vận đơn
- **Bảng trong CSDL:** `shipment_events`
- **Nằm ở sơ đồ lớp:** Sơ đồ 6 — Vận đơn 3PL, Cấu hình vận chuyển của shop, Trả hàng & Hoàn tiền (Nhóm 7–8)
- **Liên kết với bảng khác:**
  - Mỗi `ShipmentTracking` **sở hữu** nhiều (0..n) `ShipmentEvent`; `ShipmentEvent` không tồn tại nếu thiếu `ShipmentTracking` (xóa cha thì xóa con) (qua cột `ShipmentEvent.shipmentId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `shipmentId` | mã vận đơn | số nguyên lớn | Có | FK → shipment_trackings.id — thuộc vận đơn nào. **Khóa ngoại** → bảng của `ShipmentTracking`. |
| `eventStatus` | trạng thái thô hãng gửi | chuỗi ≤ 100 ký tự | Có | Mã trạng thái THÔ do hãng vận chuyển gửi về. |
| `location` | địa điểm | chuỗi ≤ 255 ký tự | Không | Địa điểm xảy ra sự kiện: "Kho Sóc Sơn - Hà Nội". |
| `occurredAt` | thời điểm sự kiện xảy ra | ngày + giờ (có múi giờ) | Có | Thời điểm sự kiện XẢY RA (theo hãng vận chuyển). |
| `description` | mô tả | chuỗi ≤ 500 ký tự | Không | Mô tả dễ đọc: "Đơn hàng đã rời kho phân loại". |
| `receivedAt` | thời điểm hệ thống nhận | ngày + giờ (có múi giờ) | Có | Thời điểm hệ thống NHẬN ĐƯỢC webhook này. |

<a id="vendorshippingconfig"></a>
## 7.3 `VendorShippingConfig` — Cấu hình vận chuyển của shop

- **Đọc là:** cấu hình vận chuyển người bán (shop)
- **Bảng trong CSDL:** `vendor_shipping_config`
- **Nằm ở sơ đồ lớp:** Sơ đồ 6 — Vận đơn 3PL, Cấu hình vận chuyển của shop, Trả hàng & Hoàn tiền (Nhóm 7–8)
- **Liên kết với bảng khác:**
  - Mỗi `VendorProfile` **sở hữu** 0 hoặc 1 `VendorShippingConfig`; `VendorShippingConfig` không tồn tại nếu thiếu `VendorProfile` (xóa cha thì xóa con) (qua cột `VendorShippingConfig.vendorId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Có | FK → users.id, UNIQUE — mỗi shop 1 cấu hình. **Khóa ngoại** → bảng của `VendorProfile`. Giá trị **không được trùng** giữa các dòng. |
| `ghnEnabled` | bật GHN | đúng / sai | Có | Bật/tắt Giao Hàng Nhanh (mặc định bật). |
| `ghtkEnabled` | bật GHTK | đúng / sai | Có | Bật/tắt Giao Hàng Tiết Kiệm (mặc định bật). |
| `vtpEnabled` | bật ViettelPost | đúng / sai | Có | Bật/tắt Viettel Post (mặc định tắt). |
| `jtEnabled` | bật J&T Express | đúng / sai | Có | Bật/tắt J&T Express (mặc định tắt). |
| `genieExpressEnabled` | bật Genie Express | đúng / sai | Có | Bật/tắt giao nội thành Genie Express (FBL) — TLCN chỉ là lựa chọn, không có app tài xế (mặc định tắt). |
| `freeshipThreshold` | ngưỡng miễn phí ship của shop | số tiền (DECIMAL, không làm tròn sai) | Không | Ngưỡng đơn tối thiểu để shop MIỄN PHÍ ship. |

# Nhóm 8 — Hoàn tiền / khiếu nại

<a id="refundrequest"></a>
## 8.1 `RefundRequest` — Yêu cầu hoàn tiền

- **Đọc là:** yêu cầu hoàn tiền
- **Bảng trong CSDL:** `refund_requests`
- **Nằm ở sơ đồ lớp:** Sơ đồ 6 — Vận đơn 3PL, Cấu hình vận chuyển của shop, Trả hàng & Hoàn tiền (Nhóm 7–8)
- **Liên kết với bảng khác:**
  - Mỗi `PaymentRefund` **trỏ tới** đúng 1 `RefundRequest` (qua cột `PaymentRefund.refundRequestId`).
  - Mỗi `ShipmentTracking` **trỏ tới** 0 hoặc 1 `RefundRequest` (qua cột `ShipmentTracking.refundRequestId`).
  - Mỗi `RefundRequest` **trỏ tới** đúng 1 `Order` (qua cột `RefundRequest.orderId`).
  - Mỗi `RefundRequest` **trỏ tới** đúng 1 `User` (qua cột `RefundRequest.requestedByUserId`).
  - Mỗi `RefundRequest` **sở hữu** nhiều (0..n) `RefundRequestItem`; `RefundRequestItem` không tồn tại nếu thiếu `RefundRequest` (xóa cha thì xóa con) (qua cột `RefundRequestItem.refundRequestId`).
  - Mỗi `RefundRequest` **sở hữu** nhiều (0..n) `RefundEvidence`; `RefundEvidence` không tồn tại nếu thiếu `RefundRequest` (xóa cha thì xóa con) (qua cột `RefundEvidence.refundRequestId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `orderId` | mã đơn hàng | số nguyên lớn | Có | FK → orders.id — hoàn tiền cho đơn nào. **Khóa ngoại** → bảng của `Order`. |
| `requestedByUserId` | mã người gửi yêu cầu | số nguyên lớn | Có | FK → users.id — ai gửi yêu cầu (thường là khách). **Khóa ngoại** → bảng của `User`. |
| `refundAmount` | số tiền yêu cầu hoàn | số tiền (DECIMAL, không làm tròn sai) | Có | Tổng số tiền yêu cầu hoàn. |
| `reason` | lý do | văn bản dài | Không | Lý do khách đưa ra: "Hàng lỗi", "Sai mẫu"... |
| `requestType` | phương án khách chọn | danh sách cố định `RefundRequestType` | Có | Phương án KHÁCH chọn: RETURN_AND_REFUND (gửi trả hàng rồi hoàn tiền) / REFUND_ONLY (hoàn tiền không cần trả hàng — thiếu hàng, hư hỏng một phần) — UC-SUP-01. Giá trị: `RETURN_AND_REFUND` (trả hàng và hoàn tiền), `REFUND_ONLY` (chỉ hoàn tiền). |
| `resolutionMode` | phương án được duyệt | danh sách cố định `RefundResolution` | Không | Phương án được DUYỆT (shop / sàn / tự động): APPROVE_WITH_RETURN (sinh vận đơn chiều về) / APPROVE_REFUND_ONLY (hoàn ngay) — UC-SUP-02. Giá trị: `APPROVE_WITH_RETURN` (duyệt, có thu hồi hàng), `APPROVE_REFUND_ONLY` (duyệt, chỉ hoàn tiền). |
| `status` | trạng thái | danh sách cố định `RefundStatus` | Có | REQUESTED / AWAITING_EVIDENCE / SHOP_REJECTED / DISPUTED / AWAITING_RETURN_SHIPMENT / RETURN_FAILED / APPROVED / REJECTED / REFUNDED (9 trạng thái). Giá trị: `REQUESTED` (khách vừa gửi yêu cầu), `AWAITING_EVIDENCE` (chờ khách bổ sung bằng chứng), `SHOP_REJECTED` (shop từ chối), `DISPUTED` (đang tranh chấp), `AWAITING_RETURN_SHIPMENT` (chờ khách gửi hàng hoàn), `RETURN_FAILED` (khách không gửi hàng hoàn), `APPROVED` (đã duyệt), `REJECTED` (bị từ chối), `REFUNDED` (đã hoàn tiền). |
| `resolvedByStaffId` | mã nhân viên xử lý | số nguyên lớn | Không | FK mềm → users.id, staff nào đã xử lý. Lưu mã của đối tượng khác nhưng không ràng buộc cứng ở CSDL (khóa ngoại mềm). |
| `resolutionNote` | ghi chú quyết định | văn bản dài | Không | Ghi chú của staff về quyết định. |
| `requestedAt` | thời điểm gửi yêu cầu | ngày + giờ (có múi giờ) | Có | Thời điểm khách gửi yêu cầu. |
| `shopResponseDueAt` | hạn shop phải phản hồi | ngày + giờ (có múi giờ) | Có | Hạn shop phải phản hồi = requestedAt + 48h; đặt lại now + 48h khi khách nộp bằng chứng bổ sung. |
| `evidenceRequestedAt` | thời điểm shop yêu cầu bổ sung bằng chứng | ngày + giờ (có múi giờ) | Không | Thời điểm shop yêu cầu bổ sung bằng chứng (tối đa 1 lần); khách có 48 giờ. |
| `evidenceRequestNote` | nội dung cần bổ sung | chuỗi ≤ 500 ký tự | Không | Shop ghi rõ cần bổ sung gì (vd: "Video mở hộp thấy rõ mã vận đơn"). |
| `resolvedAt` | thời điểm ra quyết định | ngày + giờ (có múi giờ) | Không | Thời điểm staff ra quyết định. |
| `shopRejectedAt` | thời điểm shop từ chối | ngày + giờ (có múi giờ) | Không | Thời điểm shop từ chối; mốc 48 giờ để khách khiếu nại lên sàn (Cron đóng yêu cầu nếu quá hạn). |
| `returnDueAt` | hạn gửi hàng hoàn | ngày + giờ (có múi giờ) | Không | Hạn khách bàn giao hàng hoàn cho ĐVVC = thời điểm duyệt + 7 ngày (chỉ khi APPROVE_WITH_RETURN). |
| `returnShippedAt` | thời điểm hãng lấy hàng hoàn | ngày + giờ (có múi giờ) | Không | ĐVVC đã lấy hàng hoàn (vận đơn RETURN → PICKED_UP); từ mốc này Cron không còn đóng yêu cầu vì quá hạn. |
| `returnReceivedAt` | thời điểm shop nhận lại hàng | ngày + giờ (có múi giờ) | Không | Hàng hoàn đã về kho shop (vận đơn RETURN → DELIVERED) → chuyển APPROVED để hoàn tiền. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Lần thay đổi gần nhất (@UpdateTimestamp) — truy vấn "yêu cầu không cập nhật quá 24 giờ" cho BI/SLA. Hệ thống tự ghi mỗi lần sửa. |
| `items` | các dòng hàng | danh sách `RefundRequestItem` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `RefundRequestItem` liên quan (JPA nạp khi cần). Các sản phẩm cụ thể muốn trả. |
| `evidences` | các bằng chứng | danh sách `RefundEvidence` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `RefundEvidence` liên quan (JPA nạp khi cần). Ảnh/video bằng chứng. |

<a id="refundrequestitem"></a>
## 8.2 `RefundRequestItem` — Dòng sản phẩm muốn hoàn

- **Đọc là:** dòng hàng yêu cầu hoàn tiền
- **Bảng trong CSDL:** `refund_request_items`
- **Nằm ở sơ đồ lớp:** Sơ đồ 6 — Vận đơn 3PL, Cấu hình vận chuyển của shop, Trả hàng & Hoàn tiền (Nhóm 7–8)
- **Liên kết với bảng khác:**
  - Mỗi `RefundRequestItem` **trỏ tới** đúng 1 `OrderItem` (qua cột `RefundRequestItem.orderItemId`).
  - Mỗi `RefundRequest` **sở hữu** nhiều (0..n) `RefundRequestItem`; `RefundRequestItem` không tồn tại nếu thiếu `RefundRequest` (xóa cha thì xóa con) (qua cột `RefundRequestItem.refundRequestId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `refundRequestId` | mã yêu cầu hoàn tiền | số nguyên lớn | Có | FK → refund_requests.id. **Khóa ngoại** → bảng của `RefundRequest`. |
| `orderItemId` | mã dòng hàng | số nguyên lớn | Có | FK → order_items.id — hoàn dòng hàng nào của đơn. **Khóa ngoại** → bảng của `OrderItem`. |
| `quantity` | số lượng | số nguyên | Có | Số lượng muốn trả (có thể ÍT HƠN số đã mua). |
| `itemRefundAmount` | số tiền hoàn của dòng | số tiền (DECIMAL, không làm tròn sai) | Có | Số tiền hoàn cho dòng này (đã tính cả phân bổ giảm giá). |

<a id="refundevidence"></a>
## 8.3 `RefundEvidence` — Bằng chứng khiếu nại

- **Đọc là:** bằng chứng hoàn tiền
- **Bảng trong CSDL:** `refund_evidences`
- **Nằm ở sơ đồ lớp:** Sơ đồ 6 — Vận đơn 3PL, Cấu hình vận chuyển của shop, Trả hàng & Hoàn tiền (Nhóm 7–8)
- **Liên kết với bảng khác:**
  - Mỗi `RefundRequest` **sở hữu** nhiều (0..n) `RefundEvidence`; `RefundEvidence` không tồn tại nếu thiếu `RefundRequest` (xóa cha thì xóa con) (qua cột `RefundEvidence.refundRequestId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `refundRequestId` | mã yêu cầu hoàn tiền | số nguyên lớn | Có | FK → refund_requests.id. **Khóa ngoại** → bảng của `RefundRequest`. |
| `mediaUrl` | link ảnh / video | chuỗi ≤ 500 ký tự | Có | Link ảnh/video bằng chứng khách upload. |
| `mediaType` | loại tệp (ảnh / video) | danh sách cố định `MediaType` | Có | IMAGE hoặc VIDEO. Giá trị: `IMAGE` (ảnh), `VIDEO` (video). |
| `displayOrder` | thứ tự hiển thị | số nguyên | Có | Thứ tự hiển thị. |
| `submissionRound` | lần nộp bằng chứng | số nguyên | Có | 1 = nộp khi tạo yêu cầu; 2 = nộp bổ sung theo yêu cầu của shop. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm upload. Hệ thống tự ghi khi tạo dòng. |

# Nhóm 9 — Đánh giá & tương tác shop

<a id="productreview"></a>
## 9.1 `ProductReview` — Đánh giá sản phẩm

- **Đọc là:** đánh giá sản phẩm
- **Bảng trong CSDL:** `product_reviews`
- **Nằm ở sơ đồ lớp:** Sơ đồ 7 — Đánh giá, Hữu ích, Báo cáo vi phạm, Theo dõi shop & Chat (Nhóm 9, 11)
- **Liên kết với bảng khác:**
  - Mỗi `ProductReview` **trỏ tới** đúng 1 `Product` (qua cột `ProductReview.productId`).
  - Mỗi `ProductReview` **trỏ tới** đúng 1 `User` (qua cột `ProductReview.customerId`).
  - Mỗi `ProductReview` **trỏ tới** đúng 1 `User` (qua cột `ProductReview.vendorId`) — vai trò *vendor*.
  - Mỗi `ProductReview` **trỏ tới** đúng 1 `OrderItem` (qua cột `ProductReview.orderItemId`).
  - Mỗi `ReviewReport` **trỏ tới** đúng 1 `ProductReview` (qua cột `ReviewReport.reviewId`).
  - Mỗi `ProductReview` **sở hữu** nhiều (0..n) `ReviewHelpfulVote`; `ReviewHelpfulVote` không tồn tại nếu thiếu `ProductReview` (xóa cha thì xóa con) (qua cột `ReviewHelpfulVote.reviewId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `orderItemId` | mã dòng hàng | số nguyên lớn | Có | FK → order_items.id, UNIQUE — ĐẢM BẢO ĐÁNH GIÁ THẬT. **Khóa ngoại** → bảng của `OrderItem`. Giá trị **không được trùng** giữa các dòng. |
| `productId` | mã sản phẩm | số nguyên lớn | Có | FK → products.id — đánh giá sản phẩm nào (denormalize để query nhanh). **Khóa ngoại** → bảng của `Product`. |
| `customerId` | mã khách hàng | số nguyên lớn | Có | FK → users.id — ai đánh giá. **Khóa ngoại** → bảng của `User`. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Có | FK → users.id — shop nào bị/được đánh giá. **Khóa ngoại** → bảng của `User`. |
| `rating` | số sao đánh giá | số nguyên | Có | Số sao 1–5. |
| `comment` | nội dung nhận xét | văn bản dài | Không | Nội dung nhận xét. |
| `imageUrlsJson` | danh sách link ảnh (JSON) | JSON (JSONB) | Không | Mảng JSON các link ảnh khách đính kèm. |
| `videoUrl` | link video đánh giá | chuỗi ≤ 500 ký tự | Không | Video review ngắn ≤ 15 giây (Review Shorts) trên Silo. |
| `vendorReply` | phản hồi của shop | văn bản dài | Không | Phản hồi của shop. |
| `vendorRepliedAt` | thời điểm shop phản hồi | ngày + giờ (có múi giờ) | Không | Thời điểm shop phản hồi. |
| `isVisible` | đang hiển thị | đúng / sai | Có | False = bị staff ẩn do vi phạm, nhưng vẫn giữ dữ liệu. Mặc định: `true`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm đánh giá. Hệ thống tự ghi khi tạo dòng. |
| `helpfulCount` | số lượt Hữu ích | số nguyên | Có | Số lượt bấm "Hữu ích" (đếm sẵn, đồng bộ review_helpful_votes) — sắp xếp đánh giá hữu ích lên đầu (UC-SOC-03). Mặc định: `0`. |
| `isAnonymous` | là đánh giá ẩn tên | đúng / sai | Có | True = hiện tên người đánh giá dạng "L***g" (UC-SOC-03, TLCN v1.3). Mặc định: `false`. |
| `aspectSentimentJson` | kết quả phân tích cảm xúc (PhoBERT) | chuỗi JSON (jsonb) | Không | Kết quả mô hình PhoBERT do AI Worker trả về (UC-AI-07). TLCN chỉ lưu cảm xúc tổng thể: `{"overall": "POSITIVE" / "NEUTRAL" / "NEGATIVE", "score": độ tin cậy 0–1, "model": tên + phiên bản mô hình, "analyzedAt": thời điểm}`; DATN thêm khóa theo khía cạnh. Rỗng khi chưa phân tích hoặc đánh giá chỉ có số sao. Chỉ để tham khảo — không tự ẩn đánh giá. |

<a id="shopfollow"></a>
## 9.2 `ShopFollow` — Theo dõi shop

- **Đọc là:** theo dõi shop
- **Bảng trong CSDL:** `shop_follows`
- **Nằm ở sơ đồ lớp:** Sơ đồ 7 — Đánh giá, Hữu ích, Báo cáo vi phạm, Theo dõi shop & Chat (Nhóm 9, 11)
- **Liên kết với bảng khác:**
  - Mỗi `ShopFollow` **trỏ tới** đúng 1 `User` (qua cột `ShopFollow.customerId`) — vai trò *customer*.
  - Mỗi `ShopFollow` **trỏ tới** đúng 1 `User` (qua cột `ShopFollow.vendorId`) — vai trò *vendor*.

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `customerId` | mã khách hàng | số nguyên lớn | Có | FK → users.id — người theo dõi (UNIQUE ghép với vendorId). **Khóa ngoại** → bảng của `User`. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Có | FK → users.id — shop được theo dõi. **Khóa ngoại** → bảng của `User`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm bấm theo dõi. Hệ thống tự ghi khi tạo dòng. |

<a id="reviewreport"></a>
## 9.3 `ReviewReport` — Báo cáo đánh giá vi phạm

- **Đọc là:** báo cáo đánh giá
- **Bảng trong CSDL:** `review_reports`
- **Nằm ở sơ đồ lớp:** Sơ đồ 7 — Đánh giá, Hữu ích, Báo cáo vi phạm, Theo dõi shop & Chat (Nhóm 9, 11)
- **Liên kết với bảng khác:**
  - Mỗi `ReviewReport` **trỏ tới** đúng 1 `ProductReview` (qua cột `ReviewReport.reviewId`).
  - Mỗi `ReviewReport` **trỏ tới** đúng 1 `User` (qua cột `ReviewReport.reporterId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `reviewId` | mã đánh giá | số nguyên lớn | Có | FK → product_reviews.id — báo cáo review nào. **Khóa ngoại** → bảng của `ProductReview`. |
| `reporterId` | mã người báo cáo | số nguyên lớn | Có | FK → users.id — ai báo cáo (thường là shop bị đánh giá). **Khóa ngoại** → bảng của `User`. |
| `reason` | lý do | danh sách cố định `ReviewReportReason` | Có | SPAM / OFFENSIVE (xúc phạm) / FAKE_PURCHASE (mua giả). Giá trị: `SPAM` (spam), `OFFENSIVE` (xúc phạm), `FAKE_PURCHASE` (mua giả). |
| `note` | ghi chú | văn bản dài | Không | Giải thích thêm của người báo cáo. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm báo cáo. Hệ thống tự ghi khi tạo dòng. |

<a id="reviewhelpfulvote"></a>
## 9.4 `ReviewHelpfulVote` — Lượt bấm "Hữu ích" cho đánh giá

- **Đọc là:** lượt bình chọn hữu ích đánh giá
- **Bảng trong CSDL:** `review_helpful_votes`
- **Nằm ở sơ đồ lớp:** Sơ đồ 7 — Đánh giá, Hữu ích, Báo cáo vi phạm, Theo dõi shop & Chat (Nhóm 9, 11)
- **Dùng để làm gì:** Phục vụ **UC-SOC-03** (nút "Hữu ích (24)" dưới mỗi đánh giá). Bảng này chặn một người bấm nhiều lần; `ProductReview.helpfulCount` là bộ đếm sẵn để sắp xếp.
- **Liên kết với bảng khác:**
  - Mỗi `ProductReview` **sở hữu** nhiều (0..n) `ReviewHelpfulVote`; `ReviewHelpfulVote` không tồn tại nếu thiếu `ProductReview` (xóa cha thì xóa con) (qua cột `ReviewHelpfulVote.reviewId`).
  - Mỗi `ReviewHelpfulVote` **trỏ tới** đúng 1 `User` (qua cột `ReviewHelpfulVote.userId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `reviewId` | mã đánh giá | số nguyên lớn | Có | FK → product_reviews.id (ON DELETE CASCADE). **Khóa ngoại** → bảng của `ProductReview`. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id — người bấm (không được là tác giả đánh giá). **Khóa ngoại** → bảng của `User`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Hệ thống tự ghi khi tạo dòng. |

# Nhóm 10 — Thông báo & nội dung sàn

<a id="notification"></a>
## 10.1 `Notification` — Thông báo cá nhân

- **Đọc là:** thông báo
- **Bảng trong CSDL:** `notifications`
- **Nằm ở sơ đồ lớp:** Sơ đồ 10 — Thông báo cá nhân, Thông báo sàn, Thông báo quản trị, Banner & Nhật ký hệ thống (Nhóm 10, 12)
- **Liên kết với bảng khác:**
  - Mỗi `User` **sở hữu** nhiều (0..n) `Notification`; `Notification` không tồn tại nếu thiếu `User` (xóa cha thì xóa con) (qua cột `Notification.userId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id — gửi cho ai. **Khóa ngoại** → bảng của `User`. |
| `type` | loại | danh sách cố định `NotificationType` | Có | 15 loại (Phụ lục A): ORDER_STATUS_CHANGED, KYC_APPROVED, PAYMENT_SUCCESS (người mua), NEW_ORDER_FOR_VENDOR (shop)... — sự kiện con ghi trong metadataJson.event. Giá trị: `ORDER_STATUS_CHANGED` (đơn đổi trạng thái), `KYC_APPROVED` (KYC được duyệt), `KYC_REJECTED` (KYC bị từ chối), `PAYMENT_SUCCESS` (thanh toán thành công), `PAYMENT_FAILED` (thanh toán thất bại), `ESCROW_RELEASED` (đã giải ngân ký quỹ), `WALLET_CREDITED` (ví được cộng tiền), `SYSTEM_ALERT` (cảnh báo hệ thống), `REFUND_PROCESSED` (đã xử lý hoàn tiền), `NEW_PLATFORM_VOUCHER` (voucher sàn mới), `NEW_VENDOR_VOUCHER` (voucher shop mới), `NEW_ORDER_FOR_VENDOR` (shop có đơn mới), …. |
| `title` | tiêu đề | chuỗi ≤ 255 ký tự | Có | Tiêu đề thông báo. |
| `body` | nội dung | văn bản dài | Không | Nội dung chi tiết. |
| `imageUrl` | link ảnh | chuỗi ≤ 500 ký tự | Không | Ảnh minh hoạ (ảnh sản phẩm, banner). |
| `referenceId` | mã đối tượng liên quan | chuỗi ≤ 255 ký tự | Không | ID đối tượng liên quan, để bấm vào là điều hướng tới. |
| `referenceType` | loại đối tượng liên quan | chuỗi ≤ 50 ký tự | Không | Loại đối tượng: "ORDER", "PRODUCT", "VOUCHER". |
| `metadataJson` | dữ liệu bổ sung (JSON) | JSON (JSONB) | Không | Dữ liệu bổ sung dạng JSON tuỳ loại thông báo. |
| `channel` | kênh gửi | danh sách cố định `NotificationChannel` | Có | Kênh phát thông báo MVP: IN_APP (lưu bảng + đẩy WebSocket) / EMAIL. Giá trị: `IN_APP` (trong ứng dụng), `EMAIL` (email). |
| `deliveryStatus` | trạng thái gửi | danh sách cố định `NotificationDeliveryStatus` | Có | PENDING, SENT, DELIVERED, FAILED (A.14). Giá trị: `PENDING` (đang chờ), `SENT` (đã gửi), `DELIVERED` (đã giao), `FAILED` (thất bại). |
| `retryCount` | số lần thử gửi lại | số nguyên | Không | Số lần đã thử gửi lại khi thất bại. |
| `isRead` | đã đọc chưa | đúng / sai | Có | Đã đọc hay chưa — dùng cho badge số đỏ. Mặc định: `false`. |
| `sentAt` | thời điểm gửi | ngày + giờ (có múi giờ) | Không | Thời điểm gửi đi thành công. |
| `readAt` | thời điểm đọc | ngày + giờ (có múi giờ) | Không | Thời điểm user mở đọc. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo thông báo. Hệ thống tự ghi khi tạo dòng. |

<a id="platformnotification"></a>
## 10.2 `PlatformNotification` — Thông báo phát toàn sàn

- **Đọc là:** thông báo sàn
- **Bảng trong CSDL:** `platform_notifications`
- **Nằm ở sơ đồ lớp:** Sơ đồ 10 — Thông báo cá nhân, Thông báo sàn, Thông báo quản trị, Banner & Nhật ký hệ thống (Nhóm 10, 12)
- **Liên kết với bảng khác:**
  - Mỗi `PlatformNotification` **sở hữu** nhiều (0..n) `UserPlatformNotificationRead`; `UserPlatformNotificationRead` không tồn tại nếu thiếu `PlatformNotification` (xóa cha thì xóa con) (qua cột `UserPlatformNotificationRead.platformNotificationId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `title` | tiêu đề | chuỗi ≤ 255 ký tự | Có | Tiêu đề thông báo. |
| `content` | nội dung | văn bản dài | Có | Nội dung. |
| `imageUrl` | link ảnh | chuỗi ≤ 500 ký tự | Không | Ảnh banner kèm theo. |
| `target` | đối tượng nhận | danh sách cố định `PlatformNotificationTarget` | Có | Đối tượng nhận: ALL, CUSTOMER, VENDOR, SPECIFIC_USERS. Giá trị: `ALL` (tất cả), `CUSTOMER` (khách hàng), `VENDOR` (người bán (shop)), `SPECIFIC_USERS` (người dùng cụ thể). |
| `createdBy` | người tạo | số nguyên lớn | Có | FK mềm → users.id — admin nào đã tạo. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm tạo. Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Thời điểm sửa gần nhất. Hệ thống tự ghi mỗi lần sửa. |
| `readCount` | số người đã đọc | số nguyên lớn | Không | CHỈ ĐỌC, tính động — số người đã đọc. **Không lưu thành cột**: tính ra mỗi lần đọc. |
| `reachCount` | số người nhận | số nguyên lớn | Có | Số người ĐÃ NHẬN (tính lúc phát) — mẫu số để tính tỷ lệ đọc. Mặc định: `0L`. |
| `sendAt` | thời điểm hẹn phát | ngày + giờ (có múi giờ) | Không | Hẹn giờ phát thông báo; trống = phát ngay (UC-NOT-05, TLCN v1.3). |
| `recipientUserIdsJson` | danh sách người nhận | JSON | Không | Mảng users.id khi target = SPECIFIC_USERS (UC-NOT-05, TLCN v1.3). |

<a id="userplatformnotificationread"></a>
## 10.3 `UserPlatformNotificationRead` — Đánh dấu đã đọc thông báo sàn

- **Đọc là:** đã đọc thông báo sàn người dùng
- **Bảng trong CSDL:** `user_platform_notification_reads`
- **Nằm ở sơ đồ lớp:** Sơ đồ 10 — Thông báo cá nhân, Thông báo sàn, Thông báo quản trị, Banner & Nhật ký hệ thống (Nhóm 10, 12)
- **Liên kết với bảng khác:**
  - Mỗi `PlatformNotification` **sở hữu** nhiều (0..n) `UserPlatformNotificationRead`; `UserPlatformNotificationRead` không tồn tại nếu thiếu `PlatformNotification` (xóa cha thì xóa con) (qua cột `UserPlatformNotificationRead.platformNotificationId`).
  - Mỗi `UserPlatformNotificationRead` **trỏ tới** đúng 1 `User` (qua cột `UserPlatformNotificationRead.userId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `userId` | mã người dùng | số nguyên lớn | Có | FK → users.id — phần 1 khoá chính gộp. Một phần của khóa chính. **Khóa ngoại** → bảng của `User`. |
| `platformNotificationId` | mã thông báo sàn | số nguyên lớn | Có | FK → platform_notifications.id — phần 2 khoá chính gộp. Một phần của khóa chính. **Khóa ngoại** → bảng của `PlatformNotification`. |
| `readAt` | thời điểm đọc | ngày + giờ (có múi giờ) | Có | Thời điểm user đọc. |

<a id="homebanner"></a>
## 10.4 `HomeBanner` — Banner trang chủ & chiến dịch tĩnh (CMS)

- **Đọc là:** banner trang chủ
- **Bảng trong CSDL:** `home_banners`
- **Nằm ở sơ đồ lớp:** Sơ đồ 10 — Thông báo cá nhân, Thông báo sàn, Thông báo quản trị, Banner & Nhật ký hệ thống (Nhóm 10, 12)
- **Dùng để làm gì:** Phục vụ **UC-ADM-12**: banner tĩnh do Admin quản lý (slider trang chủ "SIÊU SALE 12.12", banner ngành hàng, banner khu Flash Sale). Khác với `PlatformNotification` (thông báo in-app) — banner chỉ hiển thị, không gửi tới từng người.
- **Liên kết với bảng khác:**
  - Mỗi `HomeBanner` **trỏ tới** 0 hoặc 1 `Category` (qua cột `HomeBanner.categoryId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `title` | tiêu đề | chuỗi ≤ 200 ký tự | Có | Tên nội bộ / alt text: "Siêu Sale 11.11 — Slider 1". |
| `imageUrl` | link ảnh | chuỗi ≤ 500 ký tự | Có | Ảnh banner bản desktop (Silo/CDN). |
| `mobileImageUrl` | ảnh bản điện thoại | chuỗi ≤ 500 ký tự | Không | Ảnh bản mobile; NULL = dùng imageUrl. |
| `targetUrl` | link đích khi bấm | chuỗi ≤ 500 ký tự | Có | Đích khi bấm (đường dẫn trong miền sàn): danh mục, sản phẩm, cửa hàng, khu Flash Sale, Gợi ý hôm nay, trang chiến dịch. |
| `position` | vị trí hiển thị | danh sách cố định `BannerPosition` | Có | HOME_TOP_CAROUSEL / HOME_TOP_SIDE / HOME_MIDDLE / CATEGORY_TOP / FLASH_SALE_TOP / POPUP. Giá trị: `HOME_TOP_CAROUSEL` (băng trượt đầu trang chủ), `HOME_TOP_SIDE` (hai ô cạnh băng chuyền), `HOME_MIDDLE` (giữa trang chủ), `CATEGORY_TOP` (đầu trang danh mục), `FLASH_SALE_TOP` (đầu khu Flash Sale), `POPUP` (cửa sổ bật lên). |
| `categoryId` | mã danh mục | số nguyên lớn | Không | FK → categories.id; bắt buộc khi position = CATEGORY_TOP. **Khóa ngoại** → bảng của `Category`. |
| `sortOrder` | thứ tự sắp xếp | số nguyên | Có | Thứ tự trong cùng vị trí (số nhỏ hiển thị trước). Mặc định: `0`. |
| `startsAt` | thời điểm bắt đầu | ngày + giờ (có múi giờ) | Có | Bắt đầu hiển thị. |
| `endsAt` | thời điểm kết thúc | ngày + giờ (có múi giờ) | Có | Kết thúc hiển thị. |
| `isActive` | đang bật / đang hoạt động | đúng / sai | Có | Công tắc tắt khẩn cấp (sai giá, sai nội dung) mà không sửa lịch. Mặc định: `true`. |
| `targetAudienceJson` | đối tượng nhắm tới (JSON) | JSON (JSONB) | Không | Nhắm đối tượng: {"roles":["CUSTOMER"],"newUserOnly":true}; NULL = mọi người. |
| `createdByAdminId` | Admin tạo | số nguyên lớn | Có | ID admin tạo banner — dữ liệu kiểm toán, KHÔNG đặt FK (như VendorProfile.kycVerifiedBy). Lưu mã của đối tượng khác nhưng không ràng buộc cứng ở CSDL (khóa ngoại mềm). |
| `impressionCount` | lượt hiển thị | số nguyên lớn | Có | Lượt hiển thị (flush từ Redis banner:{id}:imp mỗi 5 phút). Mặc định: `0L`. |
| `clickCount` | lượt bấm | số nguyên lớn | Có | Lượt bấm (flush từ Redis banner:{id}:clk). Mặc định: `0L`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Hệ thống tự ghi mỗi lần sửa. |

<a id="adminnotification"></a>
## 10.5 `AdminNotification` — Thông báo nội bộ cho quản trị viên

- **Đọc là:** thông báo quản trị viên
- **Bảng trong CSDL:** `admin_notifications`
- **Nằm ở sơ đồ lớp:** Sơ đồ 10 — Thông báo cá nhân, Thông báo sàn, Thông báo quản trị, Banner & Nhật ký hệ thống (Nhóm 10, 12)
- **Dùng để làm gì:** Chuông thông báo của Admin Console (hộp thư chung của đội vận hành): đơn mới, hồ sơ KYC / sản phẩm chờ duyệt, yêu cầu rút tiền, cảnh báo hệ thống, lệch số dư ví / sổ cái. Bảng này **đang được dùng trong mã nguồn cũ** (`server/internal/app/domain/admin_notification.go`, API `GET /admin/notifications`, `/admin/notifications/unread-count`) — đưa vào TLCN v1.1 để mô hình khớp hiện trạng. Mở rộng ở DATN: `FRAUD_ALERT`, `KYC_BACKLOG`, `SLA_OVERDUE`, `COD_OVERDUE`.

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `type` | loại | danh sách cố định `AdminNotificationType` | Có | SYSTEM_ALERT / NEW_ORDER / USER_APPROVAL / WITHDRAW_REQUEST / ESCROW_DISCREPANCY (5 loại TLCN). Giá trị: `SYSTEM_ALERT` (cảnh báo hệ thống), `NEW_ORDER` (đơn hàng mới), `USER_APPROVAL` (hồ sơ chờ duyệt), `WITHDRAW_REQUEST` (yêu cầu rút tiền), `ESCROW_DISCREPANCY` (lệch số dư ví / sổ cái). |
| `title` | tiêu đề | chuỗi ≤ 255 ký tự | Có | Tiêu đề. |
| `content` | nội dung | văn bản dài | Có | Nội dung chi tiết. |
| `referenceUrl` | link tới trang xử lý | chuỗi ≤ 500 ký tự | Không | Link tới trang xử lý trong admin panel. |
| `imageUrl` | link ảnh | chuỗi ≤ 500 ký tự | Không | Ảnh minh hoạ. |
| `isRead` | đã đọc chưa | đúng / sai | Có | Đã xử lý/đã xem chưa. Mặc định: `false`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm phát sinh. Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Thời điểm cập nhật. Hệ thống tự ghi mỗi lần sửa. |

# Nhóm 11 — Nhắn tin (chat)

<a id="conversation"></a>
## 11.1 `Conversation` — Cuộc hội thoại

- **Đọc là:** hội thoại
- **Bảng trong CSDL:** `conversations`
- **Nằm ở sơ đồ lớp:** Sơ đồ 7 — Đánh giá, Hữu ích, Báo cáo vi phạm, Theo dõi shop & Chat (Nhóm 9, 11)
- **Liên kết với bảng khác:**
  - Mỗi `Conversation` **trỏ tới** đúng 1 `User` (qua cột `Conversation.customerId`) — vai trò *customer*.
  - Mỗi `Conversation` **trỏ tới** đúng 1 `VendorProfile` (qua cột `Conversation.vendorId`).
  - Mỗi `Conversation` **sở hữu** nhiều (0..n) `Message`; `Message` không tồn tại nếu thiếu `Conversation` (xóa cha thì xóa con) (qua cột `Message.conversationId`).

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `customerId` | mã khách hàng | số nguyên lớn | Có | FK → users.id — khách hàng (UNIQUE ghép với vendorId). **Khóa ngoại** → bảng của `User`. |
| `vendorId` | mã người bán (shop) | số nguyên lớn | Có | FK → users.id — shop. **Khóa ngoại** → bảng của `VendorProfile`. |
| `lastMessageSnippet` | trích đoạn tin nhắn cuối | chuỗi ≤ 255 ký tự | Không | Trích đoạn tin nhắn cuối, hiển thị ở danh sách chat. |
| `lastMessageAt` | thời điểm tin nhắn cuối | ngày + giờ (có múi giờ) | Không | Thời điểm tin cuối — dùng để SẮP XẾP danh sách. |
| `customerUnread` | số tin khách chưa đọc | số nguyên | Có | Số tin khách chưa đọc — badge đỏ phía khách. |
| `vendorUnread` | số tin shop chưa đọc | số nguyên | Có | Số tin shop chưa đọc — badge đỏ phía shop. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm mở hội thoại. Hệ thống tự ghi khi tạo dòng. |
| `updatedAt` | thời điểm cập nhật gần nhất | ngày + giờ (có múi giờ) | Có | Thời điểm cập nhật gần nhất. Hệ thống tự ghi mỗi lần sửa. |
| `messages` | các tin nhắn | danh sách `Message` (quan hệ) | — | Không phải cột trong bảng: là **đường liên kết** để code Java lấy nhanh `Message` liên quan (JPA nạp khi cần). Các tin nhắn trong hội thoại. |

<a id="message"></a>
## 11.2 `Message` — Tin nhắn

- **Đọc là:** tin nhắn
- **Bảng trong CSDL:** `messages`
- **Nằm ở sơ đồ lớp:** Sơ đồ 7 — Đánh giá, Hữu ích, Báo cáo vi phạm, Theo dõi shop & Chat (Nhóm 9, 11)
- **Liên kết với bảng khác:**
  - Mỗi `Conversation` **sở hữu** nhiều (0..n) `Message`; `Message` không tồn tại nếu thiếu `Conversation` (xóa cha thì xóa con) (qua cột `Message.conversationId`).
  - Mỗi `Message` **trỏ tới** 0 hoặc 1 `ProductVariant` (qua cột `Message.productVariantId`).
  - Mỗi `Message` **trỏ tới** đúng 1 `User` (qua cột `Message.senderId`) — vai trò *sender*.

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | số nguyên lớn | Có | Khoá chính. Khóa chính, CSDL tự tăng khi thêm dòng mới — không tự nhập. |
| `conversationId` | mã hội thoại | số nguyên lớn | Có | FK → conversations.id — thuộc hội thoại nào. **Khóa ngoại** → bảng của `Conversation`. |
| `senderId` | mã người gửi | số nguyên lớn | Có | FK → users.id — ai gửi (khách hoặc shop). **Khóa ngoại** → bảng của `User`. |
| `type` | loại | danh sách cố định `MessageType` | Có | TEXT (chữ) / IMAGE (ảnh). Giá trị: `TEXT` (văn bản), `IMAGE` (ảnh), `PRODUCT_CARD` (thẻ sản phẩm), `ORDER_CARD` (thẻ đơn hàng). |
| `content` | nội dung | văn bản dài | Không | Nội dung chữ (khi type = TEXT). |
| `attachmentUrl` | link tệp đính kèm | chuỗi ≤ 500 ký tự | Không | Link ảnh đính kèm (khi type = IMAGE). |
| `isRead` | đã đọc chưa | đúng / sai | Có | Người nhận đã đọc tin này chưa. Mặc định: `false`. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm gửi. Hệ thống tự ghi khi tạo dòng. |
| `productVariantId` | mã biến thể sản phẩm (SKU) | số nguyên lớn | Không | FK → product_variants.id (khi type = PRODUCT_CARD — UC-SOC-02). **Khóa ngoại** → bảng của `ProductVariant`. |
| `orderId` | mã đơn hàng | số nguyên lớn | Không | FK → orders.id (khi type = ORDER_CARD — thẻ đơn hàng trong chat, UC-SOC-02, TLCN v1.3). **Khóa ngoại** → bảng của `Order`. |

# Nhóm 12 — Nhật ký hệ thống

<a id="systemlog"></a>
## 12.1 `SystemLog` — Nhật ký kiểm toán

- **Đọc là:** nhật ký hệ thống
- **Bảng trong CSDL:** `system_logs`
- **Nằm ở sơ đồ lớp:** Sơ đồ 10 — Thông báo cá nhân, Thông báo sàn, Thông báo quản trị, Banner & Nhật ký hệ thống (Nhóm 10, 12)

| Tên trường | Đọc là | Kiểu | Bắt buộc | Ý nghĩa & cách dùng |
|---|---|---|:---:|---|
| `id` | mã định danh (khóa chính) | chuỗi ≤ 36 ký tự | Có | Khoá chính kiểu UUID (36 ký tự), KHÔNG tự tăng. Một phần của khóa chính. |
| `createdAt` | thời điểm tạo | ngày + giờ (có múi giờ) | Có | Thời điểm xảy ra hành động. Hệ thống tự ghi khi tạo dòng. |
| `userId` | mã người dùng | số nguyên lớn | Không | Ai thực hiện. NULL = hành động của hệ thống. Lưu mã của đối tượng khác nhưng không ràng buộc cứng ở CSDL (khóa ngoại mềm). |
| `userEmail` | email người thực hiện (chụp lại) | chuỗi ≤ 255 ký tự | Có | Email người thực hiện — CHỤP LẠI, không JOIN. |
| `userRole` | vai trò lúc thực hiện (chụp lại) | chuỗi ≤ 50 ký tự | Có | Vai trò lúc thực hiện — chụp lại. |
| `actionType` | loại hành động | danh sách cố định `AuditAction` | Có | Loại hành động: LOGIN_SUCCESS, KYC_APPROVED, PAYOUT_APPROVED... Giá trị: `LOGIN_SUCCESS` (đăng nhập thành công), `LOGIN_FAILED` (đăng nhập thất bại), `ACCOUNT_LOCKED` (tài khoản bị khóa), `ACCOUNT_SUSPENDED` (tài khoản bị đình chỉ), `ACCOUNT_REACTIVATED` (tài khoản được mở lại), `KYC_SUBMITTED` (đã nộp KYC), `KYC_APPROVED` (KYC được duyệt), `KYC_REJECTED` (KYC bị từ chối), `ORDER_STATUS_CHANGED` (đơn đổi trạng thái), `ESCROW_RELEASED` (đã giải ngân ký quỹ), `PAYOUT_APPROVED` (duyệt lệnh rút tiền), `PAYOUT_REJECTED` (từ chối lệnh rút tiền), … (30 giá trị — gồm 8 hành động AUTH thêm bằng V14). |
| `referenceId` | mã đối tượng liên quan | chuỗi ≤ 100 ký tự | Không | ID đối tượng bị tác động. |
| `details` | chi tiết | văn bản dài | Không | Chi tiết dạng JSON: giá trị trước/sau khi sửa. |
| `ipAddress` | địa chỉ IP | chuỗi ≤ 45 ký tự | Không | Địa chỉ IP thực hiện hành động. |
| `status` | trạng thái | chuỗi ≤ 20 ký tự | Có | Kết quả: "SUCCESS" / "FAILED". |
| `userAgent` | trình duyệt / thiết bị (User-Agent) | chuỗi ≤ 500 ký tự | Không | Trình duyệt/thiết bị thực hiện. |

<a id="phu-luc-enum"></a>
# Phụ lục — Giải nghĩa các danh sách cố định (enum)

> Enum là cột chỉ nhận một trong các giá trị liệt kê sẵn. Giá trị lưu trong CSDL đúng như cột *Giá trị* (chữ in hoa); cột *Nghĩa* là dịch tiếng Việt.

### `UserRole` — vai trò người dùng

Dùng ở: User.role (mở rộng: VENDOR_STAFF, SHIPPER, B2B_BUYER)

| Giá trị | Nghĩa |
|---|---|
| `CUSTOMER` | khách hàng |
| `VENDOR` | người bán (shop) |
| `STAFF` | nhân viên |
| `ADMIN` | quản trị viên |

### `AccountStatus` — trạng thái tài khoản

Dùng ở: User.status (ANONYMIZED: xóa tài khoản — UC-AUTH-10)

| Giá trị | Nghĩa |
|---|---|
| `UNVERIFIED` | chưa xác thực email |
| `ACTIVE` | đang hoạt động |
| `LOCKED` | bị khóa |
| `SUSPENDED` | bị đình chỉ |
| `ANONYMIZED` | đã ẩn danh hóa |
| `DELETED` | đã xóa |

### `Gender` — giới tính

Dùng ở: CustomerProfile.gender

| Giá trị | Nghĩa |
|---|---|
| `MALE` | nam |
| `FEMALE` | nữ |
| `OTHER` | khác |
| `PREFER_NOT_TO_SAY` | không muốn tiết lộ |

### `BusinessType` — loại kinh doanh

Dùng ở: VendorProfile.businessType

| Giá trị | Nghĩa |
|---|---|
| `INDIVIDUAL` | cá nhân |
| `HOUSEHOLD` | hộ kinh doanh |
| `BUSINESS` | doanh nghiệp |

### `KycStatus` — trạng thái KYC

Dùng ở: VendorProfile.kycStatus

| Giá trị | Nghĩa |
|---|---|
| `NOT_SUBMITTED` | chưa nộp |
| `PENDING` | đang chờ |
| `AI_PROCESSING` | AI đang xử lý |
| `MANUAL_REVIEW` | chờ duyệt tay |
| `VERIFIED` | đã xác minh |
| `REJECTED` | bị từ chối |
| `SUPPLEMENT_REQUESTED` | yêu cầu bổ sung |

### `KycVerificationMethod` — phương thức xác minh KYC

Dùng ở: VendorProfile.kycVerificationMethod

| Giá trị | Nghĩa |
|---|---|
| `OCR_AUTO` | AI tự duyệt |
| `MANUAL_STAFF` | nhân viên duyệt tay |
| `BIOMETRIC_FACIAL` | sinh trắc khuôn mặt |

### `KycOverallResult` — kết quả tổng thể KYC

Dùng ở: KycResult.overallStatus

| Giá trị | Nghĩa |
|---|---|
| `PASS` | đạt |
| `PARTIAL` | đạt một phần (chuyển người duyệt) |
| `FAIL` | thất bại |

### `StaffPermissionEnum` — danh sách quyền nhân viên

Dùng ở: StaffPermission.permission (13 quyền TLCN; mở rộng: SUPPORT_CUSTOMER, VIEW_SYSTEM_LOG, MANAGE_FEATURE_FLAGS, RUN_EXPERIMENTS, MANAGE_B2B_CREDIT, MANAGE_DISPATCH, MANAGE_SHIPPERS, REVIEW_FRAUD, HANDLE_DSAR, HANDLE_TAKEDOWN)

| Giá trị | Nghĩa |
|---|---|
| `VIEW_DASHBOARD` | xem tổng quan |
| `REVIEW_KYC` | duyệt KYC |
| `MANAGE_CATALOG` | quản lý danh mục & duyệt sản phẩm |
| `MODERATE_REVIEW` | kiểm duyệt đánh giá |
| `HANDLE_DISPUTE` | xử lý trả hàng / khiếu nại |
| `PROCESS_PAYOUT` | xử lý rút tiền |
| `UPDATE_COMMISSION` | sửa hoa hồng |
| `VIEW_FINANCE` | xem sổ cái tài chính |
| `MANAGE_VOUCHER` | quản lý voucher sàn & Flash Sale |
| `MANAGE_PLATFORM_NOTIFICATION` | phát thông báo sàn |
| `VIEW_AUDIT_LOG` | xem nhật ký hệ thống |
| `MANAGE_USER` | khóa / mở người dùng |
| `MANAGE_RBAC` | quản lý nhân viên & phân quyền |

### `StaffTitle` — tiêu đề nhân viên

Dùng ở: AdminProfile.staffTitle (3 chức danh, mỗi chức danh mang bộ quyền mặc định; mở rộng: KYC_OFFICER, MARKETING_OFFICER, DISPATCHER, RISK_ANALYST, COMPLIANCE_OFFICER, B2B_CREDIT_OFFICER, TECH_OPS)

| Giá trị | Nghĩa |
|---|---|
| `MODERATOR` | kiểm duyệt viên |
| `CS_SPECIALIST` | chuyên viên CSKH |
| `FINANCE_OFFICER` | nhân viên tài chính |

### `ProductStatus` — trạng thái sản phẩm

Dùng ở: Product.status (FLAGGED: kiểm duyệt tự động đánh dấu nghi vấn — UC-CAT-12)

| Giá trị | Nghĩa |
|---|---|
| `DRAFT` | bản nháp |
| `PENDING_APPROVAL` | chờ duyệt |
| `PUBLISHED` | đang bán |
| `HIDDEN` | đã ẩn |
| `REJECTED` | bị từ chối |
| `FLAGGED` | bị gắn cờ |
| `DELETED` | đã xóa |

### `ShippingPayer` — người trả vận chuyển

Dùng ở: Product.shippingPayer

| Giá trị | Nghĩa |
|---|---|
| `BUYER` | người mua |
| `SELLER` | người bán |

### `AttributeDataType` — loại dữ liệu thuộc tính

Dùng ở: CategoryAttribute.dataType

| Giá trị | Nghĩa |
|---|---|
| `TEXT` | văn bản |
| `NUMBER` | số |
| `BOOLEAN` | đúng / sai |
| `ENUM` | chọn một giá trị trong danh sách |
| `MULTI_ENUM` | chọn nhiều giá trị |
| `DATE` | ngày |

### `ProductCondition` — tình trạng sản phẩm

Dùng ở: Product.itemCondition (tình trạng hàng)

| Giá trị | Nghĩa |
|---|---|
| `NEW` | mới |
| `USED` | đã qua sử dụng |
| `REFURBISHED` | tân trang |

### `ProductDocumentType` — loại giấy tờ sản phẩm

Dùng ở: ProductDocument.documentType

| Giá trị | Nghĩa |
|---|---|
| `BUSINESS_LICENSE` | giấy phép kinh doanh |
| `QUALITY_CERTIFICATE` | giấy chứng nhận chất lượng |
| `IMPORT_DOCUMENT` | giấy tờ nhập khẩu |
| `BRAND_AUTHORIZATION` | giấy ủy quyền thương hiệu |
| `OTHER` | khác |

### `CategoryProposalStatus` — trạng thái đề xuất danh mục

Dùng ở: CategoryProposal.status

| Giá trị | Nghĩa |
|---|---|
| `PENDING` | đang chờ |
| `APPROVED` | đã duyệt |
| `REJECTED` | bị từ chối |

### `ModerationDecision` — quyết định kiểm duyệt

Dùng ở: ProductModerationResult.decision

| Giá trị | Nghĩa |
|---|---|
| `APPROVE` | duyệt |
| `REJECT` | từ chối |
| `NEED_REVIEW` | cần người duyệt |

### `TrafficSource` — nguồn truy cập

Dùng ở: ProductViewStat.source

| Giá trị | Nghĩa |
|---|---|
| `SEARCH` | tìm kiếm |
| `HOME_FEED` | gợi ý trang chủ |
| `PROMOTION` | khuyến mãi |
| `SHOP_PAGE` | trang shop |
| `EXTERNAL_LINK` | link bên ngoài (chia sẻ) |

### `VoucherScope` — phạm vi voucher

Dùng ở: Voucher.scope (CATEGORY / PRODUCT dùng 4 bảng nối mục 4.4)

| Giá trị | Nghĩa |
|---|---|
| `ALL` | tất cả |
| `CATEGORY` | danh mục |
| `PRODUCT` | sản phẩm |

### `VoucherType` — loại voucher

Dùng ở: Voucher.type, OrderDiscount.voucherType (3 tầng; mở rộng: PAYMENT — tầng 4)

| Giá trị | Nghĩa |
|---|---|
| `SHOP` | shop |
| `PLATFORM` | sàn |
| `FREESHIP` | miễn phí vận chuyển |

### `DiscountType` — loại giảm giá

Dùng ở: Voucher.discountType

| Giá trị | Nghĩa |
|---|---|
| `PERCENTAGE` | phần trăm |
| `FIXED_AMOUNT` | số tiền cố định |
| `FREESHIP` | miễn phí vận chuyển |

### `FundingParty` — bên nguồn tiền

Dùng ở: Voucher.sponsorType, OrderDiscount.fundedBy (CO_SPONSOR: sàn + shop cùng chịu theo Voucher.platformSharePercent)

| Giá trị | Nghĩa |
|---|---|
| `PLATFORM` | sàn |
| `VENDOR` | người bán (shop) |
| `CO_SPONSOR` | sàn và shop cùng chịu |

### `FlashSaleStatus` — trạng thái bán hàng Flash

Dùng ở: FlashSaleCampaign.status

| Giá trị | Nghĩa |
|---|---|
| `DRAFT` | bản nháp |
| `SCHEDULED` | đã lên lịch |
| `ACTIVE` | đang hoạt động |
| `ENDED` | đã kết thúc |

### `CoinTransactionType` — loại giao dịch Điểm

Dùng ở: CoinTransaction.type (mở rộng: COSMETIC_REDEEM, VENDOR_REWARD)

| Giá trị | Nghĩa |
|---|---|
| `CHECK_IN` | điểm danh |
| `ORDER_CASHBACK` | hoàn Điểm theo đơn |
| `REVIEW_BONUS` | thưởng đánh giá |
| `USED_AT_CHECKOUT` | dùng khi thanh toán |
| `ORDER_REFUND` | hoàn Điểm khi hủy / hoàn đơn |
| `VOUCHER_REDEEM` | đổi Điểm lấy voucher |

### `SessionStatus` — trạng thái phiên

Dùng ở: CheckoutSession.status

| Giá trị | Nghĩa |
|---|---|
| `INITIATED` | vừa khởi tạo |
| `PAYMENT_PENDING` | chờ thanh toán |
| `COMPLETED` | đã hoàn tất |
| `FAILED` | thất bại |
| `EXPIRED` | đã hết hạn |

### `OrderStatus` — trạng thái đơn hàng

Dùng ở: Order.status (19 trạng thái — giữ đủ vì GHN cũng có giao thất bại / hoàn hàng)

| Giá trị | Nghĩa |
|---|---|
| `PENDING_PAYMENT` | chờ thanh toán |
| `PAID` | đã thanh toán |
| `CONFIRMED` | đã xác nhận |
| `PREPARING` | đang chuẩn bị |
| `READY_FOR_PICKUP` | chờ hãng lấy hàng |
| `DELIVERING` | đang giao |
| `DELIVERED` | đã giao |
| `DELIVERY_FAILED` | giao thất bại |
| `RETURNING` | đang hoàn về |
| `RETURNED_TO_VENDOR` | đã hoàn về shop |
| `COMPLETED` | đã hoàn tất |
| `RETURN_REQUESTED` | khách yêu cầu trả hàng |
| `RETURN_APPROVED` | đã duyệt trả hàng |
| `RETURN_REJECTED` | từ chối trả hàng |
| `PARTIALLY_REFUNDED` | đã hoàn một phần |
| `REFUNDED` | đã hoàn tiền |
| `CANCELLED` | đã hủy |
| `CANCELLED_BY_VENDOR` | shop hủy |
| `AUTO_CANCELLED` | hệ thống tự hủy |

### `FundingStatus` — trạng thái nguồn tiền

Dùng ở: Order.fundingStatus

| Giá trị | Nghĩa |
|---|---|
| `UNFUNDED` | chưa có tiền |
| `PENDING` | đang chờ |
| `FUNDED` | đã nạp tiền |
| `FAILED` | thất bại |

### `StockReservationStatus` — trạng thái giữ chỗ tồn kho

Dùng ở: StockReservation.status

| Giá trị | Nghĩa |
|---|---|
| `ACTIVE` | đang hoạt động |
| `CONFIRMED` | đã xác nhận |
| `EXPIRED` | đã hết hạn |
| `RELEASED` | đã giải ngân |

### `PaymentMethod` — phương thức thanh toán

Dùng ở: PaymentTransaction.paymentMethod, Order.paymentMethod (VIETQR = chuyển khoản, xác nhận bằng webhook SePay; mở rộng: MOMO, ZALOPAY, INTERNAL_WALLET, BNPL)

| Giá trị | Nghĩa |
|---|---|
| `VNPAY` | VNPay |
| `VIETQR` | VietQR |
| `COD` | COD |

### `PaymentStatus` — trạng thái thanh toán

Dùng ở: PaymentTransaction.status, Order.paymentStatus

| Giá trị | Nghĩa |
|---|---|
| `PENDING` | đang chờ |
| `REQUIRES_ACTION` | chờ khách xác thực (OTP / 3DS) |
| `SUCCESS` | thành công |
| `FAILED` | thất bại |
| `EXPIRED` | đã hết hạn |
| `REFUNDED` | đã hoàn tiền |
| `PARTIALLY_REFUNDED` | đã hoàn một phần |
| `CANCELLED` | đã hủy |

### `PaymentRefundStatus` — trạng thái hoàn tiền thanh toán

Dùng ở: PaymentRefund.status

| Giá trị | Nghĩa |
|---|---|
| `PENDING` | đang chờ |
| `SUCCESS` | thành công |
| `FAILED` | thất bại |

### `FundingSource` — nguồn nguồn tiền

Dùng ở: EscrowLedger.fundingSource (ONLINE_GATEWAY gồm VNPay và VietQR qua SePay; mở rộng: WALLET, SPLIT)

| Giá trị | Nghĩa |
|---|---|
| `ONLINE_GATEWAY` | cổng thanh toán online |
| `COD_CASH` | tiền COD đã đối soát |

### `EscrowStatus` — trạng thái ký quỹ

Dùng ở: EscrowLedger.status

| Giá trị | Nghĩa |
|---|---|
| `PENDING_FUNDING` | chờ tiền vào ký quỹ |
| `HELD` | đang giữ |
| `FROZEN` | bị phong tỏa |
| `RELEASED` | đã giải ngân |
| `REFUNDED` | đã hoàn tiền |
| `DISPUTED` | đang tranh chấp |

### `WalletTransactionType` — loại giao dịch ví

Dùng ở: WalletTransaction.type (mở rộng: CREDIT_TOPUP, DEBIT_PURCHASE, DEBIT_AD_SPEND, DEBIT_SUBSCRIPTION_FEE, DEBIT_TAX_WITHHOLDING, DEBIT_COSMETIC_PURCHASE)

| Giá trị | Nghĩa |
|---|---|
| `CREDIT_ESCROW_RELEASE` | cộng tiền giải ngân ký quỹ |
| `DEBIT_REFUND` | trừ tiền hoàn cho khách |
| `DEBIT_WITHDRAWAL` | trừ tiền rút |
| `CREDIT_ADJUSTMENT` | cộng tiền điều chỉnh tay |
| `DEBIT_ADJUSTMENT` | trừ tiền điều chỉnh tay |

### `WalletReferenceType` — loại tham chiếu ví

Dùng ở: WalletTransaction.referenceType (mở rộng: SUBSCRIPTION, AD_CAMPAIGN, COSMETIC, TAX)

| Giá trị | Nghĩa |
|---|---|
| `ORDER` | đơn hàng |
| `WITHDRAWAL` | rút tiền |
| `REFUND` | hoàn tiền |
| `ADJUSTMENT` | điều chỉnh |

### `WithdrawalStatus` — trạng thái rút tiền

Dùng ở: WithdrawalRequest.status

| Giá trị | Nghĩa |
|---|---|
| `PENDING` | đang chờ |
| `APPROVED` | đã duyệt |
| `PROCESSING` | đang xử lý |
| `COMPLETED` | đã hoàn tất |
| `REJECTED` | bị từ chối |

### `FinancialEventType` — loại sự kiện tài chính

Dùng ở: FinancialLedger.eventType (10 giá trị TLCN; mở rộng: B2B_WHOLESALE_DISBURSEMENT, TAX_WITHHOLDING, LOGISTICS_FEE, SUBSCRIPTION_FEE, AD_REVENUE, COSMETIC_REVENUE, BNPL_MERCHANT_FEE, COSMETIC_DEFERRED, SHIPPER_COD_REMIT, SHIPPER_DEPOSIT_DEDUCTION, AFFILIATE_COMMISSION)

| Giá trị | Nghĩa |
|---|---|
| `ESCROW_IN` | tiền vào ký quỹ |
| `COMMISSION` | hoa hồng |
| `PLATFORM_VOUCHER` | chi phí voucher sàn |
| `FREESHIP_COST` | chi phí miễn phí ship |
| `COIN_COST` | chi phí Điểm |
| `ESCROW_RELEASE` | giải ngân ký quỹ |
| `PAYOUT_ACCRUE` | ghi nhận nợ phải trả shop |
| `PAYOUT_SETTLE` | đã chi trả cho shop |
| `REFUND` | hoàn tiền |
| `GATEWAY_FEE` | phí cổng thanh toán |

### `CarrierType` — loại hãng vận chuyển

Dùng ở: ShipmentTracking.carrierType (GENIE_EXPRESS chỉ là lựa chọn giao nội thành — không có app tài xế ở TLCN)

| Giá trị | Nghĩa |
|---|---|
| `GHN` | GHN |
| `GHTK` | GHTK |
| `VIETTEL_POST` | ViettelPost |
| `JNT_EXPRESS` | J&T Express |
| `GENIE_EXPRESS` | Genie Express |

### `ShipmentDirection` — chiều vận đơn

Dùng ở: ShipmentTracking.direction

| Giá trị | Nghĩa |
|---|---|
| `OUTBOUND` | chiều đi (shop → khách) |
| `RETURN` | chiều về (khách → shop) |

### `ShipmentTrackingStatus` — trạng thái theo dõi vận đơn

Dùng ở: ShipmentTracking.status (9 trạng thái)

| Giá trị | Nghĩa |
|---|---|
| `AWAITING_PICKUP` | chờ lấy hàng |
| `PICKED_UP` | đã lấy hàng |
| `IN_TRANSIT` | đang trung chuyển |
| `OUT_FOR_DELIVERY` | đang đi giao |
| `DELIVERED` | đã giao |
| `FAILED` | thất bại |
| `RETURNING` | đang hoàn về |
| `RETURNED` | đã trả về |
| `LOST` | thất lạc |

### `RefundRequestType` — loại yêu cầu hoàn tiền

Dùng ở: RefundRequest.requestType

| Giá trị | Nghĩa |
|---|---|
| `RETURN_AND_REFUND` | trả hàng và hoàn tiền |
| `REFUND_ONLY` | chỉ hoàn tiền |

### `RefundResolution` — phương án xử lý hoàn tiền

Dùng ở: RefundRequest.resolutionMode

| Giá trị | Nghĩa |
|---|---|
| `APPROVE_WITH_RETURN` | duyệt, có thu hồi hàng |
| `APPROVE_REFUND_ONLY` | duyệt, chỉ hoàn tiền |

### `RefundStatus` — trạng thái hoàn tiền

Dùng ở: RefundRequest.status (9 trạng thái)

| Giá trị | Nghĩa |
|---|---|
| `REQUESTED` | khách vừa gửi yêu cầu |
| `AWAITING_EVIDENCE` | chờ khách bổ sung bằng chứng |
| `SHOP_REJECTED` | shop từ chối |
| `DISPUTED` | đang tranh chấp |
| `AWAITING_RETURN_SHIPMENT` | chờ khách gửi hàng hoàn |
| `RETURN_FAILED` | khách không gửi hàng hoàn |
| `APPROVED` | đã duyệt |
| `REJECTED` | bị từ chối |
| `REFUNDED` | đã hoàn tiền |

### `MediaType` — loại tệp media

Dùng ở: RefundEvidence.mediaType

| Giá trị | Nghĩa |
|---|---|
| `IMAGE` | ảnh |
| `VIDEO` | video |

### `NotificationType` — loại thông báo

Dùng ở: Notification.type (mở rộng: PRICE_DROP_ALERT, B2B_RFQ_QUOTED)

| Giá trị | Nghĩa |
|---|---|
| `ORDER_STATUS_CHANGED` | đơn đổi trạng thái |
| `KYC_APPROVED` | KYC được duyệt |
| `KYC_REJECTED` | KYC bị từ chối |
| `PAYMENT_SUCCESS` | thanh toán thành công |
| `PAYMENT_FAILED` | thanh toán thất bại |
| `ESCROW_RELEASED` | đã giải ngân ký quỹ |
| `WALLET_CREDITED` | ví được cộng tiền |
| `SYSTEM_ALERT` | cảnh báo hệ thống |
| `REFUND_PROCESSED` | đã xử lý hoàn tiền |
| `NEW_PLATFORM_VOUCHER` | voucher sàn mới |
| `NEW_VENDOR_VOUCHER` | voucher shop mới |
| `NEW_ORDER_FOR_VENDOR` | shop có đơn mới |
| `FLASH_SALE_REMINDER` | nhắc Flash Sale |
| `NEW_DEVICE_LOGIN` | đăng nhập thiết bị lạ |
| `PRODUCT_APPROVED` | sản phẩm đã duyệt |
| `PRODUCT_REJECTED` | sản phẩm bị từ chối |

### `NotificationChannel` — kênh thông báo

Dùng ở: Notification.channel (mở rộng: PUSH, SMS, ZNS, WEBSOCKET)

| Giá trị | Nghĩa |
|---|---|
| `IN_APP` | trong ứng dụng |
| `EMAIL` | email |

### `NotificationDeliveryStatus` — trạng thái giao hàng thông báo

Dùng ở: Notification.deliveryStatus

| Giá trị | Nghĩa |
|---|---|
| `PENDING` | đang chờ |
| `SENT` | đã gửi |
| `DELIVERED` | đã giao |
| `FAILED` | thất bại |

### `ReviewReportReason` — lý do báo cáo đánh giá

Dùng ở: ReviewReport.reason

| Giá trị | Nghĩa |
|---|---|
| `SPAM` | spam |
| `OFFENSIVE` | xúc phạm |
| `FAKE_PURCHASE` | mua giả |

### `PlatformNotificationTarget` — đích thông báo sàn

Dùng ở: PlatformNotification.target (mở rộng: SHIPPER)

| Giá trị | Nghĩa |
|---|---|
| `ALL` | tất cả |
| `CUSTOMER` | khách hàng |
| `VENDOR` | người bán (shop) |
| `SPECIFIC_USERS` | người dùng cụ thể |

### `BannerPosition` — vị trí banner

Dùng ở: HomeBanner.position

| Giá trị | Nghĩa |
|---|---|
| `HOME_TOP_CAROUSEL` | băng trượt đầu trang chủ |
| `HOME_TOP_SIDE` | hai ô cạnh băng chuyền đầu trang chủ |
| `HOME_MIDDLE` | giữa trang chủ |
| `CATEGORY_TOP` | đầu trang danh mục |
| `FLASH_SALE_TOP` | đầu khu Flash Sale |
| `POPUP` | cửa sổ bật lên |

### `AdminNotificationType` — loại thông báo quản trị viên

Dùng ở: AdminNotification.type (mở rộng: FRAUD_ALERT, KYC_BACKLOG, SLA_OVERDUE, COD_OVERDUE)

| Giá trị | Nghĩa |
|---|---|
| `SYSTEM_ALERT` | cảnh báo hệ thống |
| `NEW_ORDER` | đơn hàng mới |
| `USER_APPROVAL` | hồ sơ chờ duyệt |
| `WITHDRAW_REQUEST` | yêu cầu rút tiền |
| `ESCROW_DISCREPANCY` | lệch số dư ví / sổ cái |

### `MessageType` — loại tin nhắn

Dùng ở: Message.type (thẻ sản phẩm / thẻ đơn hàng — UC-SOC-02)

| Giá trị | Nghĩa |
|---|---|
| `TEXT` | văn bản |
| `IMAGE` | ảnh |
| `PRODUCT_CARD` | thẻ sản phẩm |
| `ORDER_CARD` | thẻ đơn hàng |

### `AuditAction` — hành động kiểm toán

Dùng ở: SystemLog.actionType (mở rộng: MFA_ENABLED, MFA_DISABLED, MFA_CHALLENGE_FAILED, PAYOUT_REVIEWED)

| Giá trị | Nghĩa |
|---|---|
| `LOGIN_SUCCESS` | đăng nhập thành công |
| `LOGIN_FAILED` | đăng nhập thất bại |
| `ACCOUNT_LOCKED` | tài khoản bị khóa |
| `ACCOUNT_SUSPENDED` | tài khoản bị đình chỉ |
| `ACCOUNT_REACTIVATED` | tài khoản được mở lại |
| `KYC_SUBMITTED` | đã nộp KYC |
| `KYC_APPROVED` | KYC được duyệt |
| `KYC_REJECTED` | KYC bị từ chối |
| `ORDER_STATUS_CHANGED` | đơn đổi trạng thái |
| `ESCROW_RELEASED` | đã giải ngân ký quỹ |
| `PAYOUT_APPROVED` | duyệt lệnh rút tiền |
| `PAYOUT_REJECTED` | từ chối lệnh rút tiền |
| `REFUND_PROCESSED` | đã xử lý hoàn tiền |
| `REVIEW_HIDDEN` | đánh giá đã ẩn |
| `COMMISSION_UPDATED` | đổi tỷ lệ hoa hồng |
| `PERMISSION_GRANTED` | quyền được cấp |
| `PERMISSION_REVOKED` | quyền đã thu hồi |
| `RBAC_ROLE_CHANGED` | đổi vai trò / chức danh |
| `STAFF_INVITED` | đã mời nhân viên |
| `PII_ANONYMIZED` | đã ẩn danh dữ liệu cá nhân |
| `ACCESS_DENIED` | bị chặn quyền truy cập |
| `PRODUCT_MODERATED` | đã kiểm duyệt sản phẩm |
| `PASSWORD_CHANGED` | đổi / thiết lập mật khẩu khi đang đăng nhập *(V14)* |
| `PASSWORD_RESET` | đặt lại mật khẩu qua quên mật khẩu *(V14)* |
| `EMAIL_CHANGED` | đổi email đăng nhập *(V14)* |
| `OAUTH_LINKED` | liên kết Google / Apple *(V14)* |
| `OAUTH_UNLINKED` | gỡ liên kết Google / Apple *(V14)* |
| `SESSION_REVOKED` | đăng xuất một thiết bị từ xa *(V14)* |
| `ACCOUNT_DELETION_REQUESTED` | khách yêu cầu xóa tài khoản *(V14)* |
| `ACCOUNT_DELETION_CANCELLED` | khách hủy yêu cầu xóa *(V14)* |

---

*Sinh lại:* `python tk/gen_giai_nghia.py` (công cụ ở `fix-doc-backup-2026-10-29/tools/`) sau khi cập nhật `ENTITY.md` và chạy `cd/parse_entity.py`.
