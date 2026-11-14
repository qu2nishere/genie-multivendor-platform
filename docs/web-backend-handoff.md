# Bàn giao web → backend: danh sách hàm mock cần nối `/api/v1`

> Plan chuyển giao diện G13.20 · cập nhật 2026-11-14 · nguồn: mọi `web/src/features/*/infrastructure/*.api.ts`
> và `web/src/shared/api/upload.ts` (bảng ở mục 6 sinh tự động từ mã, rồi đối chiếu `API_SPEC.md`).

Giao diện chạy trên lớp mock (ADR 0003). Mỗi hàm `*.api.ts` đã có **chữ ký cuối cùng** và **nhánh gọi
thật**; khi có backend chỉ cần bỏ nhánh `if (USE_MOCK) { … }`.

## 1. Các bước nối

1. Đặt `PUBLIC_USE_MOCK=false`, `PUBLIC_API_URL` / `INTERNAL_API_URL` trỏ backend (`web/.env`).
2. Với từng hàm ở mục 6: xóa nhánh `if (USE_MOCK)`; giữ DTO (`*.dto.ts`) và mapper (`*.mapper.ts`).
   Sửa đường dẫn theo mục 2 trước khi xóa nhánh mock.
3. Xóa dữ liệu mock: `features/*/infrastructure/*.mock.ts`, `shared/api/mock/` (`seed`, `seller`,
   `admin`, `banners`, `activity`, `placed`), `mockState` / `mockDelay` / `mockFail` trong
   `shared/api/mock.ts`, ảnh `public/img/mock/`, nhân vật nhân viên mock trong `shared/auth/session.ts`
   (`MOCK_USER`, `MOCK_STAFF`, `genie.mock.staff_title`).
4. Xóa 3 hàm chỉ phục vụ mock (mục 5) và chỗ gọi ở tầng `application`.
5. E2E (`web/tests/e2e`) đang dựa trên dữ liệu mock (mã đơn, tài khoản mẫu) — chuyển sang dữ liệu seed
   `R__seed_demo.sql` hoặc chạy với mock bật ở CI.

**Tổng:** 237 hàm có nhánh mock trong 33 nhóm; 238 endpoint khác nhau ở nhánh thật; **21 endpoint
chưa khớp `API_SPEC.md`** (mục 2).

## 2. Endpoint mã đang gọi nhưng API_SPEC chưa có / đặt khác tên

| Mã đang gọi | Theo API_SPEC | Việc cần làm |
|---|---|---|
| `GET /me/wishlist/ids` | `GET /me/wishlist/product-ids` | Sửa mã |
| `GET /me/shop-follows` · `POST /me/shop-follows` · `DELETE /me/shop-follows/{id}` | `GET /me/following` · `PUT/DELETE /me/following/{shop_id}` · `GET /me/following/shop-ids` | Sửa mã (đổi cả phương thức theo dõi sang `PUT`) |
| `GET /public/search/suggest` | `GET /public/search/suggestions` | Sửa mã |
| `GET /public/feed` | `GET /public/recommendations?page&size` | Sửa mã |
| `GET /public/products/{id}/related` | `GET /public/products/{id}/similar?limit=6` | Sửa mã |
| `GET /public/products/facets` | `GET /public/products?…&include_facets=true` (trả cùng danh sách) | Sửa mã — gộp vào `listProducts` |
| `POST /me/notifications/{id}/read` · `POST /me/notifications/read-all` | `PATCH /me/notifications/{id}` · `POST /me/notifications/mark-all-read` | Sửa mã |
| `POST /reviews/{reviewId}/helpful` | `PUT` / `DELETE /reviews/{id}/helpful-vote` | Sửa mã (bỏ hữu ích = `DELETE`) |
| `POST /public/banners/{id}/events` | `POST /public/banner-events` (id trong body) | Sửa mã |
| `GET /public/shops/{shopId}/vouchers` | `GET /public/vouchers?shop_id=` | Sửa mã |
| `POST` / `DELETE /me/identities/GOOGLE` | `POST /me/identities/{provider}` (`google` chữ thường) | Sửa mã |
| `GET /public/campaigns/{slug}` · `GET /public/campaigns/slugs` | Chưa có — spec dựng trang chiến dịch bằng `GET /public/products?discount_min=` + `GET /public/vouchers?code=` | **Bổ sung spec** (trang `/campaign/[slug]` cần tiêu đề, banner, mốc thời gian, mã voucher; sitemap cần danh sách slug) |
| `GET /public/categories/{categoryId}/attribute-filters` | Chưa có (spec chỉ có thuộc tính ở `/admin/categories/{id}/attributes`) | **Bổ sung spec** hoặc trả trong `include_facets` (bộ lọc thuộc tính ngành UC-CAT-05) |
| `GET /public/brands/verified` | Chưa có | **Bổ sung spec** hoặc trả cờ `verified` trong facet thương hiệu (dấu tích ở bộ lọc Thương hiệu) |
| `GET /public/variants?ids=` | Chưa có | **Bổ sung spec** — giỏ khách lưu SKU, cần tra dòng hiển thị (G10.02) |
| `GET /public/shops/shipping-policies?ids=` | Chưa có — phí ship chỉ có trong preview 6.8 | **Bổ sung spec** hoặc tính phí ship ở `POST /me/checkout/preview` cho cả trang giỏ (G10.11) |

## 3. Trường dữ liệu client cần mà API_SPEC chưa có

| Trường | Ở đâu | Lý do |
|---|---|---|
| `product_slug` | dòng giỏ (6.1), `$OrderItem` (7.x) | Liên kết sản phẩm chuẩn `/p/{slug}-{id}` (R16) — không có thì phải gọi thêm chi tiết sản phẩm |
| `order_id` | thông báo loại đơn hàng (`target`) | Route theo dõi đơn dùng id; hiện tách số từ mã `TR-…` (G10.60) |
| `category_name` | dòng giỏ | Chỉ mock dùng để áp voucher theo ngành phía client — **bỏ** khi server tính giá |

`createCheckout` ở mock nhận thêm bảng giá client tính (`CheckoutQuote`) vì mock không có engine giá —
khi nối, server tính lại và client **không** gửi kèm.

## 4. Chức năng đang thay tạm, chờ hạ tầng thật

| Chức năng | Hiện tại (mock) | Khi có backend |
|---|---|---|
| Chat thời gian thực (8.x) | Hỏi lại 2 giây khi đang mở hội thoại, 15 giây hộp thư | WebSocket theo API_SPEC; giữ `client_message_id` làm Idempotency-Key |
| Xuất CSV (sổ cái 11.9, nhật ký 11.13, báo cáo Tổng quan Admin / Seller) | Tạo CSV ở trình duyệt từ dòng đang lọc | Gọi endpoint `…/export` của spec (tệp lớn, đúng toàn bộ dữ liệu) |
| Phiếu gửi hàng | `window.print()` trang HTML | PDF A6 của hãng vận chuyển |
| Chọn shop đích cho banner (Admin) | `shop.listShops` công khai | `GET /admin/shops` |
| Đề xuất danh mục Seller ↔ Admin | Hai kho mock riêng | Cùng bảng `category_proposals` |
| Banner trang chủ | Admin sửa trong phiên tab; storefront SSR đọc bộ gốc | Cùng bảng banner |
| Tải tệp (ảnh, video, giấy tờ KYC) | Bỏ qua bước PUT MinIO, trả `object_key` giả | `POST /uploads/presigned-urls` → PUT MinIO (đã có ở nhánh thật `shared/api/upload.ts`) |
| Đăng nhập Google / Apple | Hộp "Tiếp tục với Google" đăng nhập ngay | `GET /auth/oauth/{provider}/authorize` (đã có ở nhánh thật) |
| Thanh toán VNPAY | Cổng mô phỏng trong trang | Chuyển tới `payment_url` (đã có ở nhánh thật) |

## 5. Hàm chỉ có ở mock — xóa khi nối

| Hàm | Tệp | Dùng ở |
|---|---|---|
| `mockTyping` | `chat/infrastructure/chat.api.ts` | trạng thái "shop đang nhập" giả (`useChat`) — WebSocket sẽ gửi sự kiện typing |
| `settleMockPayment` | `checkout/infrastructure/checkout.api.ts` | cổng VNPAY mô phỏng (`useCheckout`) |
| `markReported` | `seller-reviews/infrastructure/review.api.ts` | đánh dấu đã báo cáo trong kho mock (`useShopReviews`) — server trả `is_reported_by_me` |

## 6. Toàn bộ hàm có nhánh mock (sinh từ mã)

Cột "Endpoint" là đường dẫn ở nhánh gọi thật hiện tại (tiền tố `/api/v1`); `{x}` là tham số đường dẫn.

### `shared/api`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `uploadFile` | upload.ts | `POST /uploads/presigned-urls` |

### `account`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getProfile` | account.api.ts | `GET /me/profile` |
| `updateProfile` | account.api.ts | `PATCH /me/profile` |
| `requestEmailChange` | account.api.ts | `POST /me/email-change-requests` |
| `getLoginMethods` | account.api.ts | `GET /me/login-methods` |
| `linkGoogle` | account.api.ts | `POST /me/identities/GOOGLE` |
| `unlinkGoogle` | account.api.ts | `DELETE /me/identities/GOOGLE` |
| `verifyPassword` | account.api.ts | `POST /me/password/verify` |
| `requestStepUp` | account.api.ts | `POST /me/otp-challenges` |
| `resendStepUp` | account.api.ts | `POST /me/otp-challenges/{challengeId}/resend` |
| `changePassword` | account.api.ts | `PUT /me/password` |
| `listDevices` | account.api.ts | `GET /me/sessions` |
| `logoutDevices` | account.api.ts | `DELETE /me/sessions`<br>`DELETE /me/sessions/{id}` |
| `requestDeletion` | account.api.ts | `PUT /me/deletion-request` |
| `cancelDeletion` | account.api.ts | `DELETE /me/deletion-request` |
| `listAddresses` | address.api.ts | `GET /me/addresses` |
| `createAddress` | address.api.ts | `POST /me/addresses` |
| `updateAddress` | address.api.ts | `PUT /me/addresses/{id}` |
| `setDefaultAddress` | address.api.ts | `PATCH /me/addresses/{id}` |
| `deleteAddress` | address.api.ts | `DELETE /me/addresses/{id}` |
| `listProvinces` | address.api.ts | `GET /public/regions/provinces` |
| `listDistricts` | address.api.ts | `GET /public/regions/provinces/{provinceId}/districts` |
| `listWards` | address.api.ts | `GET /public/regions/districts/{districtId}/wards` |
| `reverseGeocode` | address.api.ts | `GET /public/regions/reverse-geocode` |

### `admin-catalog`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getCategoryTree` | catalog.api.ts | `GET /admin/categories` |
| `createCategory` | catalog.api.ts | `POST /admin/categories` |
| `updateCategory` | catalog.api.ts | `PATCH /admin/categories/{id}` |
| `deleteCategory` | catalog.api.ts | `DELETE /admin/categories/{id}` |
| `listAttributes` | catalog.api.ts | `GET /admin/categories/{categoryId}/attributes` |
| `setAttributeActive` | catalog.api.ts | `PATCH /admin/categories/{categoryId}/attributes/{code}` |
| `listProposals` | catalog.api.ts | `GET /admin/category-proposals` |
| `approveProposal` | catalog.api.ts | `POST /admin/category-proposals/{id}/approve` |
| `rejectProposal` | catalog.api.ts | `POST /admin/category-proposals/{id}/reject` |
| `listCatalogProducts` | catalog.api.ts | `GET /admin/products` |
| `runProductCommand` | catalog.api.ts | `POST /admin/products/{id}/approve`<br>`POST /admin/products/{id}/reject`<br>`POST /admin/products/{id}/lock`<br>`POST /admin/products/{id}/unlock` |

### `admin-dashboard`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getDashboardFinance` | dashboard.api.ts | `GET /admin/dashboard/finance` |
| `getGmvChart` | dashboard.api.ts | `GET /admin/dashboard/gmv-chart` |
| `getOperations` | dashboard.api.ts | `GET /admin/dashboard/operations` |
| `listGrowingShops` | dashboard.api.ts | `GET /admin/dashboard/growing-shops` |
| `getAdminQueues` | queues.api.ts | `GET /admin/queues` |

### `admin-disputes`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listDisputes` | dispute.api.ts | `GET /admin/disputes` |
| `resolveDispute` | dispute.api.ts | `POST /admin/disputes/{id}/resolve` |
| `confirmReturnReceived` | dispute.api.ts | `POST /admin/disputes/{id}/confirm-return-received` |
| `reopenDispute` | dispute.api.ts | `POST /admin/disputes/{id}/reopen` |
| `freezeEscrow` | dispute.api.ts | `POST /admin/escrows/{escrowId}/freeze` |
| `unfreezeEscrow` | dispute.api.ts | `POST /admin/escrows/{escrowId}/unfreeze` |
| `syncRefund` | dispute.api.ts | `POST /admin/refunds/{refundId}/sync` |
| `retryRefund` | dispute.api.ts | `POST /admin/refunds/{refundId}/retry` |
| `markRefundTransferred` | dispute.api.ts | `POST /admin/refunds/{refundId}/mark-transferred` |

### `admin-finance`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listWithdrawals` | finance.api.ts | `GET /admin/withdrawals` |
| `approveWithdrawal` | finance.api.ts | `POST /admin/withdrawals/{id}/approve` |
| `getWithdrawalTransfer` | finance.api.ts | `GET /admin/withdrawals/{id}/transfer` |
| `rejectWithdrawal` | finance.api.ts | `POST /admin/withdrawals/{id}/reject` |
| `completeWithdrawal` | finance.api.ts | `POST /admin/withdrawals/{id}/complete` |
| `getCommission` | finance.api.ts | `GET /admin/commission-settings` |
| `saveDefaultRate` | finance.api.ts | `PATCH /admin/commission-settings` |
| `saveCategoryRates` | finance.api.ts | `PATCH /admin/commission-settings` |
| `listLedger` | finance.api.ts | `GET /admin/ledger-entries` |
| `listCodRemittances` | finance.api.ts | `GET /admin/cod-remittances` |
| `getCodRemittance` | finance.api.ts | `GET /admin/cod-remittances/{id}` |
| `previewReconciliation` | finance.api.ts | `POST /admin/cod-remittances/{id}/reconciliation-preview` |
| `confirmReconciliation` | finance.api.ts | `POST /admin/cod-remittances/{id}/confirm` |

### `admin-kyc`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listKycSubmissions` | kyc.api.ts | `GET /admin/kyc-submissions` |
| `getKycSubmission` | kyc.api.ts | `GET /admin/kyc-submissions/{id}` |
| `approveKyc` | kyc.api.ts | `POST /admin/kyc-submissions/{id}/approve` |
| `rejectKyc` | kyc.api.ts | `POST /admin/kyc-submissions/{id}/reject` |
| `requestKycSupplement` | kyc.api.ts | `POST /admin/kyc-submissions/{id}/request-supplement` |
| `requeueKyc` | kyc.api.ts | `POST /admin/kyc-submissions/{id}/requeue` |

### `admin-marketing`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listPlatformVouchers` | marketing.api.ts | `GET /admin/vouchers` |
| `getPlatformVoucher` | marketing.api.ts | `GET /admin/vouchers/{id}` |
| `savePlatformVoucher` | marketing.api.ts | `PUT /admin/vouchers/{id}`<br>`POST /admin/vouchers` |
| `disablePlatformVoucher` | marketing.api.ts | `PATCH /admin/vouchers/{id}` |
| `deletePlatformVoucher` | marketing.api.ts | `DELETE /admin/vouchers/{id}` |
| `listCampaigns` | marketing.api.ts | `GET /admin/flash-sales` |
| `getCampaign` | marketing.api.ts | `GET /admin/flash-sales/{id}` |
| `createCampaign` | marketing.api.ts | `POST /admin/flash-sales` |
| `scheduleCampaign` | marketing.api.ts | `POST /admin/flash-sales/{id}/schedule` |
| `endCampaign` | marketing.api.ts | `POST /admin/flash-sales/{id}/end` |
| `reviewFlashItem` | marketing.api.ts | `PATCH /admin/flash-sales/{campaignId}/items/{itemId}` |
| `listBanners` | marketing.api.ts | `GET /admin/banners` |
| `saveBanner` | marketing.api.ts | `PUT /admin/banners/{id}`<br>`POST /admin/banners` |
| `setBannerActive` | marketing.api.ts | `PATCH /admin/banners/{id}` |
| `deleteBanner` | marketing.api.ts | `DELETE /admin/banners/{id}` |
| `listPlatformNotifications` | marketing.api.ts | `GET /admin/platform-notifications` |
| `getPlatformNotification` | marketing.api.ts | `GET /admin/platform-notifications/{id}` |
| `savePlatformNotification` | marketing.api.ts | `PUT /admin/platform-notifications/{id}`<br>`POST /admin/platform-notifications` |
| `deletePlatformNotification` | marketing.api.ts | `DELETE /admin/platform-notifications/{id}` |

### `admin-moderation`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listModeration` | moderation.api.ts | `GET /admin/product-moderation` |
| `getModeration` | moderation.api.ts | `GET /admin/product-moderation/{id}` |
| `approveProduct` | moderation.api.ts | `POST /admin/products/{id}/approve` |
| `rejectProduct` | moderation.api.ts | `POST /admin/products/{id}/reject` |
| `requeueProduct` | moderation.api.ts | `POST /admin/products/{id}/requeue` |
| `assignModerator` | moderation.api.ts | `PUT /admin/products/{id}/assignee` |
| `listModerators` | moderation.api.ts | `GET /admin/moderators` |
| `saveModeratorAvailability` | moderation.api.ts | `PUT /admin/moderators/availability` |
| `redistributeQueue` | moderation.api.ts | `POST /admin/moderators/redistribute` |
| `listReviewReports` | review.api.ts | `GET /admin/review-reports` |
| `setReviewVisibility` | review.api.ts | `PATCH /admin/reviews/{reviewId}` |

### `admin-system`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getAdminMe` | me.api.ts | `GET /admin/me` |
| `listStaff` | system.api.ts | `GET /admin/staff` |
| `inviteStaff` | system.api.ts | `POST /admin/staff/invitations` |
| `resendInvitation` | system.api.ts | `POST /admin/staff/{id}/invitations/resend` |
| `updateStaffPermissions` | system.api.ts | `PUT /admin/staff/{id}/permissions` |
| `lockStaff` | system.api.ts | `POST /admin/staff/{id}/lock` |
| `unlockStaff` | system.api.ts | `POST /admin/staff/{id}/unlock` |
| `deleteStaff` | system.api.ts | `DELETE /admin/staff/{id}` |
| `listUsers` | system.api.ts | `GET /admin/users` |
| `suspendUser` | system.api.ts | `POST /admin/users/{id}/suspend` |
| `reactivateUser` | system.api.ts | `POST /admin/users/{id}/reactivate` |
| `auditNow` | system.api.ts | `GET /admin/audit-logs` |
| `listAuditLogs` | system.api.ts | `GET /admin/audit-logs` |
| `updateAdminMe` | system.api.ts | `PATCH /admin/me` |

### `auth`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `login` | auth.api.ts | `POST /auth/sessions` |
| `requestOtp` | auth.api.ts | `POST /auth/otp-challenges` |
| `resendOtp` | auth.api.ts | `POST /auth/otp-challenges/{challengeId}/resend` |
| `verifyOtp` | auth.api.ts | `POST /auth/otp-challenges/{challengeId}/verify` |
| `register` | auth.api.ts | `POST /auth/registrations` |
| `resetPassword` | auth.api.ts | `POST /auth/password-resets` |
| `logout` | auth.api.ts | `DELETE /auth/sessions/current` |

### `cart`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `fetchCart` | cart.api.ts | `GET /me/cart` |
| `addCartItem` | cart.api.ts | `POST /me/cart/items` |
| `updateCartItem` | cart.api.ts | `PATCH /me/cart/items/{id}` |
| `removeCartItems` | cart.api.ts | `DELETE /me/cart/items` |
| `resolveGuestItems` | cart.api.ts | `GET /public/variants` |
| `getVariantChoice` | cart.api.ts | `GET /public/products/{productId}/variants` |

### `catalog`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listCategories` | catalog.api.ts | `GET /public/categories` |
| `getCategoryBySlug` | catalog.api.ts | `GET /public/categories/{slug}` |
| `listAttributeFilters` | catalog.api.ts | `GET /public/categories/{categoryId}/attribute-filters` |
| `listVerifiedBrands` | catalog.api.ts | `GET /public/brands/verified` |

### `chat`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listConversations` | chat.api.ts | `GET /me/conversations` |
| `openConversation` | chat.api.ts | `POST /me/conversations` |
| `listMessages` | chat.api.ts | `GET /me/conversations/{conversationId}/messages` |
| `mockTyping` | chat.api.ts | — |
| `sendMessage` | chat.api.ts | `POST /me/conversations/{conversationId}/messages` |
| `markRead` | chat.api.ts | `POST /me/conversations/{conversationId}/read` |
| `listSellerConversations` | seller-chat.api.ts | `GET /seller/conversations` |
| `listSellerMessages` | seller-chat.api.ts | `GET /seller/conversations/{id}/messages` |
| `sendSellerMessage` | seller-chat.api.ts | `POST /seller/conversations/{id}/messages` |
| `markSellerRead` | seller-chat.api.ts | `POST /seller/conversations/{id}/read` |

### `checkout`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `createCheckout` | checkout.api.ts | `POST /me/checkout-sessions` |
| `getCheckoutSession` | checkout.api.ts | `GET /me/checkout-sessions/{id}` |
| `retryPayment` | checkout.api.ts | `POST /me/checkout-sessions/{id}/payments` |
| `settleMockPayment` | checkout.api.ts | — |
| `getShipPolicies` | shipping.api.ts | `GET /public/shops/shipping-policies` |

### `coins`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getCoins` | coins.api.ts | `GET /me/coins` |
| `listCoinTxns` | coins.api.ts | `GET /me/coins/transactions` |
| `listCoinRewards` | coins.api.ts | `GET /public/coin-rewards` |

### `notifications`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listNotifications` | notifications.api.ts | `GET /me/notifications` |
| `markAllNotificationsRead` | notifications.api.ts | `POST /me/notifications/read-all` |
| `markNotificationRead` | notifications.api.ts | `POST /me/notifications/{id}/read` |

### `orders`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getOrder` | order.api.ts | `GET /me/orders/{id}` |
| `confirmReceipt` | order.api.ts | `POST /me/orders/{id}/confirm-receipt` |

### `product`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listProducts` | product.api.ts | `GET /public/products` |
| `getFacets` | product.api.ts | `GET /public/products/facets` |
| `getProduct` | product.api.ts | `GET /public/products/{id}` |
| `listRelated` | product.api.ts | `GET /public/products/{id}/related` |
| `listProductsByIds` | product.api.ts | `GET /public/products` |
| `listFeed` | product.api.ts | `GET /public/feed` |
| `fetchWishlistIds` | wishlist.api.ts | `GET /me/wishlist/ids` |
| `setWishlisted` | wishlist.api.ts | `POST /me/wishlist`<br>`DELETE /me/wishlist/{productId}` |

### `promotions`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listLiveBanners` | banner.api.ts | `GET /public/banners` |
| `trackBanner` | banner.api.ts | `POST /public/banners/{id}/events` |
| `getCampaign` | campaign.api.ts | `GET /public/campaigns/{slug}` |
| `listCampaignSlugs` | campaign.api.ts | `GET /public/campaigns/slugs` |
| `listMyVouchers` | voucher.api.ts | `GET /me/vouchers` |
| `getVouchersByCodes` | voucher.api.ts | `GET /public/vouchers` |
| `saveVoucher` | voucher.api.ts | `POST /me/vouchers` |
| `listShopVouchers` | voucher.api.ts | `GET /public/shops/{shopId}/vouchers` |
| `findVoucherByCode` | voucher.api.ts | `POST /me/checkout/applicable-vouchers` |
| `removeVoucher` | voucher.api.ts | `DELETE /me/vouchers/{id}` |

### `refunds`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listBanks` | refund.api.ts | `GET /public/banks` |
| `createRefund` | refund.api.ts | `POST /me/refund-requests` |
| `addEvidence` | refund.api.ts | `POST /me/refund-requests/{id}/evidence` |
| `escalateRefund` | refund.api.ts | `POST /me/refund-requests/{id}/escalate` |
| `acceptRejection` | refund.api.ts | `POST /me/refund-requests/{id}/accept-rejection` |

### `reviews`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listReviews` | review.api.ts | `GET /public/products/{productId}/reviews` |
| `toggleHelpful` | review.api.ts | `POST /reviews/{reviewId}/helpful` |
| `reportReview` | review.api.ts | `POST /reviews/{reviewId}/reports` |

### `search`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `fetchTrending` | suggest.api.ts | `GET /public/search/trending` |
| `fetchSuggestions` | suggest.api.ts | `GET /public/search/suggest` |

### `seller-analytics`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getRevenue` | analytics.api.ts | `GET /seller/analytics/revenue` |
| `getAnalyticsOverview` | analytics.api.ts | `GET /seller/analytics/overview` |
| `getTrafficSources` | analytics.api.ts | `GET /seller/analytics/traffic-sources` |
| `getSentiment` | analytics.api.ts | `GET /seller/analytics/sentiment` |

### `seller-dashboard`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getDashboard` | dashboard.api.ts | `GET /seller/dashboard` |

### `seller-onboarding`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getKyc` | kyc.api.ts | `GET /seller/kyc` |
| `saveKycDraft` | kyc.api.ts | `PUT /seller/kyc/draft` |
| `submitKyc` | kyc.api.ts | `POST /seller/kyc/submissions` |
| `requestManualReview` | kyc.api.ts | `POST /seller/kyc/manual-review-requests` |
| `registerShop` | registration.api.ts | `POST /me/shop-registrations` |

### `seller-orders`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getSellerOrder` | fulfillment.api.ts | `GET /seller/orders/{id}` |
| `confirmOrder` | fulfillment.api.ts | `POST /seller/orders/{id}/confirm` |
| `rejectOrder` | fulfillment.api.ts | `POST /seller/orders/{id}/reject` |
| `prepareOrder` | fulfillment.api.ts | `POST /seller/orders/{id}/prepare` |
| `getShippingQuotes` | fulfillment.api.ts | `GET /seller/orders/{id}/shipping-quotes` |
| `getShippingLabel` | fulfillment.api.ts | `GET /seller/orders/{id}/shipping-label` |
| `listSellerOrders` | order.api.ts | `GET /seller/orders` |
| `listSellerRefunds` | refund.api.ts | `GET /seller/refund-requests` |
| `actOnRefund` | refund.api.ts | `POST /seller/refund-requests/{id}/{action}` |

### `seller-products`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `listSellerProducts` | product.api.ts | `GET /seller/products` |
| `getSellerProduct` | product.api.ts | `GET /seller/products/{id}` |
| `saveProduct` | product.api.ts | `PUT /seller/products/{id}`<br>`POST /seller/products` |
| `setProductHidden` | product.api.ts | `PATCH /seller/products/{id}` |
| `deleteProduct` | product.api.ts | `DELETE /seller/products/{id}` |
| `getCategoryTree` | product.api.ts | `GET /public/categories` |
| `getListingRules` | product.api.ts | `GET /seller/categories/{categoryId}/listing-rules` |
| `searchBrands` | product.api.ts | `GET /seller/brands` |
| `createBrand` | product.api.ts | `POST /seller/brands` |
| `listCatalog` | product.api.ts | `GET /seller/categories` |
| `listProposals` | product.api.ts | `GET /seller/category-proposals` |
| `createProposal` | product.api.ts | `POST /seller/category-proposals` |

### `seller-promotions`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getVoucher` | promo.api.ts | `GET /seller/vouchers/{id}` |
| `saveVoucher` | promo.api.ts | `PUT /seller/vouchers/{id}`<br>`POST /seller/vouchers` |
| `disableVoucher` | promo.api.ts | `PATCH /seller/vouchers/{id}` |
| `listFlashCampaigns` | promo.api.ts | `GET /seller/flash-sales` |
| `listEligible` | promo.api.ts | `GET /seller/flash-sales/{campaignId}/eligible-products` |
| `listFlashItems` | promo.api.ts | `GET /seller/flash-sales/{campaignId}/items` |
| `registerFlashItem` | promo.api.ts | `POST /seller/flash-sales/{campaignId}/items` |

### `seller-reviews`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `replyReview` | review.api.ts | `PUT /seller/reviews/{id}/reply` |
| `markReported` | review.api.ts | — |

### `seller-settings`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getSettings` | settings.api.ts | `GET /seller/settings` |
| `patchSettings` | settings.api.ts | `PATCH /seller/settings` |
| `savePickupAddress` | settings.api.ts | `PUT /seller/pickup-address` |
| `listBanks` | settings.api.ts | `GET /public/banks` |
| `savePayoutAccount` | settings.api.ts | `PUT /seller/payout-account` |
| `deletePayoutAccount` | settings.api.ts | `DELETE /seller/payout-account` |
| `startVacation` | settings.api.ts | `PUT /seller/vacation` |
| `endVacation` | settings.api.ts | `DELETE /seller/vacation` |
| `getSellerMe` | shop.api.ts | `GET /seller/me` |
| `searchSeller` | shop.api.ts | `GET /seller/search` |
| `getPayoutAccount` | shop.api.ts | `GET /seller/payout-account` |

### `seller-wallet`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getWallet` | wallet.api.ts | `GET /seller/wallet` |
| `createWithdrawal` | wallet.api.ts | `POST /seller/withdrawals` |

### `shop`

| Hàm | Tệp | Endpoint (nhánh thật) |
|---|---|---|
| `getShop` | shop.api.ts | `GET /public/shops/{id}` |
| `listShops` | shop.api.ts | `GET /public/shops` |
| `listFollowedShops` | shop.api.ts | `GET /me/shop-follows` |
| `setFollow` | shop.api.ts | `POST /me/shop-follows`<br>`DELETE /me/shop-follows/{id}` |

