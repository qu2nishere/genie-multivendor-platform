# PLAN — Chuyển toàn bộ giao diện prototype sang `web/`

| | |
|---|---|
| **Ngày lập** | 2026-11-11 |
| **Thể loại** | web (frontend Astro + React islands) |
| **Nguồn** | `MultivendorEcommercePrototype/genie/` (React 18 UMD + Babel, 4 UI kit: storefront, auth, vendor, admin) |
| **Đích** | `genie-multivendor-platform/web/` |
| **Tài liệu chuẩn** | `Document/kien-thuc/TECH_STACK_VA_QUY_UOC.md` — mục 2.4, 3.4, 5, 11, 13 (viết tắt bên dưới: **TS**) |
| **Theo dõi tiến độ** | [progress_chuyen_giao_dien.md](./progress_chuyen_giao_dien.md) — cùng mã task `Gxx.yy` |
| **Người commit** | tên + email đang config trong git của repo (`git config user.name` → `Thien211105`) — không dùng `--author`, không sửa config |

---

## 0. Hiện trạng đã khảo sát

**`web/` (đích)** — đã có đúng cây thư mục TS 2.4 nhưng gần như toàn bộ là stub:

- 27 feature có đủ 4 tầng `domain / application / infrastructure / ui` + `index.ts`, bên trong chỉ là `.gitkeep`; riêng `cart` có file mẫu nhưng nội dung `// TODO`.
- `shared/ui` có 9 component trả về `null`; `shared/api`, `shared/lib`, `shared/auth` là stub.
- `styles/tokens.css` rỗng, `tailwind.config.ts` chưa có theme, `global.css` đặt `@import` sau `@tailwind` (sai thứ tự).
- 22 file trong `pages/` chỉ có khung `<StorefrontLayout>`; `SellerApp` / `AdminApp` chưa có router.
- Chưa có `.prettierrc`, `vitest.config.ts`, `playwright.config.ts`, `web/.env.example`; chưa cài `node_modules` (cả gốc lẫn `web`).
- `.eslintrc.cjs` đã có `eslint-plugin-boundaries` theo TS 5.2.

**Prototype (nguồn)**

| Khối | File | Quy mô |
|---|---|---|
| Token | `tokens/{fonts,colors,typography,spacing,effects,base}.css` | 6 file |
| CSS component | `components/genie-components.css` (lớp `lz-*`) | 191 dòng |
| CSS storefront | `ui_kits/storefront/storefront.css` (lớp `tr-*`) | 624 dòng |
| Theme | `ui_kits/storefront/themes.css` + `themes.js` — 9 theme (`genie` mặc định, `hoian`, `dongho`, `dongson`, `polymer`, `buudien`, `saigon`, `sleeve`, `classic`) | 587 dòng |
| CSS bảng điều khiển | `dash.css`, `responsive.css` | 152 + 63 dòng |
| Design system | `_ds_bundle.js`: Icon, Button, IconButton, Input, Checkbox, Radio, Select, Switch, Tabs, Badge, StatusBadge, Tag, Avatar, Modal, Money/PriceVND, Rating, QuantityStepper, ProductCard, VendorChip, VoucherTag | ~20 component |
| Biểu đồ / thống kê | `dash.jsx`: Delta, StatTile, WorkQueue, Segmented, BarList, StackBar, Pipeline, TimeChart, SegBar | 245 dòng |
| Storefront | 20 file JSX + `data.jsx` + `pages-data.js` (trang chính sách) + router trong `index.html` (15 màn) | ~5.600 dòng |
| Auth | `ui_kits/auth/index.html` (Login mật khẩu / OTP, Register, Forgot, khóa 15 phút, Google / Apple mô phỏng) | 516 dòng |
| Seller Center | 10 file JSX, 11 mục nav | ~3.000 dòng |
| Admin Console | 15 file JSX, 18 mục nav, 13 mã quyền | ~3.300 dòng |
| i18n | `i18n.js` — từ điển EN **lấy chuỗi tiếng Việt làm khóa** | 61 KB |
| Tài sản | 24 font woff2, 22 ảnh sản phẩm, 15 banner, logo thanh toán, SVG thương hiệu | |

**Repo**: chỉ có nhánh `main` (chưa có `develop`), remote `origin`, Husky + commitlint đã cấu hình.

---

## 1. Nguyên tắc chuyển đổi (bắt buộc cho mọi task)

| # | Nguyên tắc | Nguồn |
|---|---|---|
| R1 | Giao diện **bám prototype**: bố cục, khoảng cách, màu, chữ, trạng thái (rỗng / lỗi / đang tải / hết hàng) phải khớp khi so ở 360, 768, 1440 px | TS 5.4 |
| R2 | Mỗi màn rơi vào đúng 1 trong 3 kiểu trang: **SSR + islands** (storefront công khai), **SSR khung + island lớn** (trang cần đăng nhập), **SPA** (`/seller/*`, `/admin/*`) | TS 5.1 |
| R3 | Chọn chỉ thị hydrate theo bảng TS 5.1: tĩnh → không JS; `client:load` / `client:idle` / `client:visible` / `client:only="react"` | TS 5.1 |
| R4 | Mỗi feature đủ 4 tầng, chiều phụ thuộc `ui → application → domain ← infrastructure`; `ui` không gọi thẳng `infrastructure` (trừ frontmatter `.astro` SSR) | TS 5.2 |
| R5 | Feature khác chỉ import qua `features/<x>/index.ts`; dùng chung từ 2 feature trở lên → đưa vào `shared/`; `shared/` không import `features/` | TS 5.2 |
| R6 | TypeScript strict, **không `any`**; JSX prototype (không kiểu) phải khai kiểu `Props` và kiểu domain đầy đủ | TS 3.4 |
| R7 | `window.LZ*`, biến toàn cục, sự kiện `emit()` của prototype **không mang sang**: thay bằng import ES module, nanostores (chia sẻ giữa island), TanStack Query (dữ liệu server) | TS 5.3 |
| R8 | Form dùng `react-hook-form + zod`; không tự quản `useState` cho từng trường như prototype | TS 5.3 |
| R9 | Mọi chuỗi giao diện qua `t()`; khóa `<feature>.<màn>.<ý nghĩa>`; đủ `vi.json` và `en.json`; tiền / ngày qua `Intl` trong `shared/lib/format.ts` | TS 5.4, 11 |
| R10 | Dữ liệu mock của prototype (`data.jsx`) đi qua đúng tầng: `infrastructure/*.mock.ts` (DTO `snake_case`) → `*.mapper.ts` → kiểu domain. UI không import mock | TS 5.2, 8 |
| R11 | Từ ngữ: **Genie**, **Điểm Genie / Điểm** (không "Xu"), **Chính hãng** (không "Mall"), mã lỗi `GNE-*` (không `LZD-*`), mã đơn `TR-yyyyMMdd-NNNNNN` | TS 11 |
| R12 | Route `kebab-case`, deeplink sản phẩm `/p/{slug}-{id}`; lưu trình duyệt qua `shared/lib/storage.ts` với tiền tố `genie.` | TS 11, 5.3 |
| R13 | `style={{…}}` inline của prototype → lớp Tailwind dùng token; không để mã màu / px cứng ngoài token | TS 3.4 |
| R14 | Storefront SSR phải có `<title>`, `meta description`, Open Graph, JSON-LD `Product` + `BreadcrumbList` ở trang sản phẩm | TS 5.4 |
| R15 | Feature có test: Vitest cho `domain` / `application`; luồng chính có Playwright ở `tests/e2e/` | TS 5.4 |
| R16 | Điều hướng bằng **liên kết thật `<a href>`** (prototype dùng `onClick` + `setScreen`): thẻ sản phẩm, danh mục, shop, phân trang, breadcrumb đều phải bấm được khi tắt JS và bot đọc được | TS 5.1, 5.4 |
| R17 | Nội dung cần SEO (tên, giá, mô tả, danh sách sản phẩm trang đầu, đánh giá trang đầu) phải có trong HTML do máy chủ trả về; island chỉ thêm tương tác. Không dùng `client:only` cho nội dung cần lập chỉ mục | TS 5.1 |
| R18 | HTML ngữ nghĩa: mỗi trang đúng một `<h1>`, thứ tự tiêu đề không nhảy cấp, có `<header> / <nav> / <main> / <footer>`; ảnh có `alt`, `width`, `height` | TS 5.4 |
| R19 | Mã trạng thái HTTP đúng: không tìm thấy → 404 thật (không trả 200 kèm màn "không tồn tại"); slug sai → 301 về URL chuẩn | TS 5.4 |

---

## 2. Quyết định cần chốt trước khi làm

Đã chốt với chủ dự án ngày 2026-11-11: **D1, D3, D5**. Các mục còn lại chạy theo khuyến nghị cho đến khi có ý kiến khác.

| # | Vấn đề | Lựa chọn | Ghi chú |
|---|---|---|---|
| D1 | Prototype có 9 theme. Mang hết hay chỉ theme mặc định? | **ĐÃ CHỐT: làm đủ 9 theme.** Theme áp cho cả 4 bề mặt (storefront, auth, seller, admin) như prototype; nút đổi theme nằm trên header đúng vị trí prototype | Làm theme `genie` trước, 8 theme còn lại ở G07 nhóm D |
| D2 | Chiến lược CSS: viết lại hết bằng utility Tailwind hay giữ file CSS? | **Lai**: `shared/ui` + màn Seller / Admin + inline style → utility Tailwind; lớp `tr-*` của storefront và `themes.css` (phụ thuộc bộ chọn `[data-theme]`) → giữ CSS trong `styles/`, bọc `@layer components`, giá trị lấy từ token | Viết lại 100% utility: thêm nhiều công, khó giữ 9 theme |
| D3 | Chưa có backend: dữ liệu lấy đâu? | **ĐÃ CHỐT: mock trước để có giao diện.** Mock nằm ở `infrastructure/*.mock.ts`, bật bằng `PUBLIC_USE_MOCK=true` | `*.api.ts` có chữ ký hàm cuối cùng, sau này chỉ đổi phần thân sang gọi `/api/v1` |
| D4 | Icon: bảng `ICONS` (SVG nội tuyến, tên kiểu Lucide) của prototype hay thêm `lucide-react`? | Port nguyên bảng `ICONS` sang `shared/ui/icon/icons.ts` có kiểu `IconName` — không thêm phụ thuộc, khớp 100% | Dùng `lucide-react`: thêm deps, phải rà tên icon |
| D5 | Phần chỉ để trình diễn của prototype | **ĐÃ CHỐT: không làm.** Bỏ hẳn: bảng nổi hình đèn thần ở mép màn hình để nhảy giữa các bề mặt / màn (`sh-dock` trong `shell.jsx`), `board.jsx`, `design-canvas.jsx`, `tweaks-panel.jsx`, bảng 7 hành trình PRD, `DemoBox`, `AcctDemo` / `SCENARIOS`, bảng mô phỏng `GoogleFlow`, nút xem trước vai trò `PREVIEW_ROLES` | Không chuyển thành tham số `?mock=`; mock chỉ có một bộ dữ liệu mặc định |
| D6 | Mã Jira cho commit (`[#GNE-<số>]`) | Tạo 1 Epic + mỗi giai đoạn G01…G13 một Story; thay `<số>` khi commit. Chưa có mã thì bỏ phần ngoặc (TS 13.3 có ví dụ không kèm mã) | — |
| D7 | Thư mục `plan/` có commit vào repo không | Có, commit bằng `docs:` ở G00 để cả nhóm theo dõi | Không commit: thêm `plan/` vào `.gitignore` |

---

## 3. Thư mục / file bổ sung so với cây TS 2.4

Cây TS 2.4 giữ nguyên; các mục dưới đây là phần thêm vì prototype có màn mà cây chưa có chỗ đặt.

```
web/
├── .env.example · .prettierrc.json · .prettierignore · vitest.config.ts · playwright.config.ts
├── public/
│   ├── fonts/                      # 24 woff2 Be Vietnam Pro + IBM Plex Mono + font của 9 theme
│   └── img/{products, banners, payments}/
└── src/
    ├── assets/brand/               # SVG thương hiệu import qua Astro
    ├── layouts/
    │   ├── AuthLayout.astro        # khung trang đăng nhập / đăng ký (prototype: Panel 2 cột)
    │   ├── partials/               # SeoHead.astro, ThemeBoot.astro — dùng chung cho cả 3 layout
    │   └── storefront/             # Header.tsx, Footer.astro, LangSelect.tsx, ToastHost.tsx
    ├── pages/
    │   ├── become-seller.astro     # màn BecomeSeller
    │   ├── flash-sale.astro        # đã có trong cây — prototype chưa có màn riêng, dựng từ FlashBar
    │   └── dev/ui.astro            # thư viện component nội bộ, 404 khi không phải DEV
    ├── features/
    │   ├── policies/               # 28 trang chính sách (pages-data.js)
    │   ├── seller-dashboard/       # Overview
    │   ├── seller-promotions/      # Promo, VoucherFormModal, đăng ký Flash Sale
    │   ├── seller-reviews/         # Reviews, ReviewCard
    │   ├── seller-settings/        # Settings, SecuritySection
    │   ├── admin-dashboard/        # Dashboard
    │   └── admin-catalog/          # Catalog, AttributePanel, CatModal
    ├── shared/
    │   ├── ui/icon/                # Icon.tsx + icons.ts
    │   ├── ui/brand/               # GenieMark, GenieLogo, GenieCoin, ThemePicker
    │   ├── ui/dash/                # StatTile, TimeChart, BarList… (dash.jsx)
    │   ├── hooks/                  # useCountdown, useClickOutside, useMediaQuery, useFocusTrap
    │   ├── api/mock.ts             # cờ USE_MOCK, mockDelay, phân trang cursor giả
    │   └── auth/permissions.ts     # 13 mã quyền + preset theo chức danh
    └── styles/
        ├── base.css · responsive.css · dash.css · themes.css
        └── storefront/*.css        # storefront.css tách theo khối
```

Feature mới phải có đủ 4 tầng + `index.ts` như 27 feature sẵn có và khai trong `boundaries/elements` (đã khớp mẫu `src/features/*/…`).

### 3.1 Khuôn một feature (áp cho mọi feature, đặt tên file giống nhau để dễ tìm)

```
features/<x>/
├── domain/
│   ├── <x>.types.ts          # kiểu dữ liệu, enum, hằng — TS thuần
│   ├── <x>.rules.ts          # hàm thuần: tính, kiểm hợp lệ, chuyển trạng thái
│   ├── <x>.schema.ts         # lược đồ zod của form (nếu có form)
│   └── <x>.rules.test.ts     # Vitest
├── infrastructure/
│   ├── <x>.dto.ts            # kiểu JSON snake_case đúng hợp đồng API
│   ├── <x>.mock.ts           # dữ liệu mẫu dạng DTO (chuyển từ data.jsx)
│   ├── <x>.mapper.ts         # DTO ⇄ domain
│   ├── <x>.api.ts            # hàm lấy dữ liệu; hiện trả mock, sau gọi /api/v1
│   └── <x>.storage.ts        # localStorage (nếu có)
├── application/
│   ├── use<X>.ts             # hook TanStack Query
│   ├── <x>.store.ts          # nanostores (khi cần chia sẻ giữa island)
│   └── <luồng>.ts            # luồng nhiều bước
├── ui/
│   ├── <X>Page.tsx           # island lớn của trang / màn SPA
│   ├── <Khối>.tsx            # island nhỏ
│   └── <Khối>.astro          # phần tĩnh render SSR
└── index.ts                  # API công khai duy nhất
```

Thứ tự làm trong một feature luôn là: `types` → `rules` + test → `dto` → `mock` → `mapper` → `api` → `application` → `ui` → `index.ts` → gắn vào `pages/` hoặc `routes.tsx` → khóa i18n.

Một feature coi là xong khi: đủ các file trên (bỏ file không cần), không còn `.gitkeep`, lint boundaries sạch, có khóa `vi` + `en`, có test cho `rules`.

---

## 4. Bảng ánh xạ nguồn → đích

### 4.1 Storefront

| Nguồn (`ui_kits/storefront/`) | Đích | Kiểu trang |
|---|---|---|
| `index.html` → `App` (router theo `?screen=`) | thay bằng `src/pages/**` + `middleware.ts` | — |
| `index.html` → `Footer`, `LoginPrompt`, toast | `layouts/storefront/Footer.astro`, `features/auth/ui/LoginPrompt.tsx`, `layouts/storefront/ToastHost.tsx` | — |
| `index.html` → `OrderDone`, `PaymentFailed`, `ReceiptConfirm` | `features/checkout/ui/` → `pages/checkout/result.astro` | khung + island |
| `index.html` → `readGuestCart`, `writeGuestCart`, `mergeCart`, `seedCart` | `features/cart/infrastructure/guestCart.storage.ts`, `application/mergeGuestCart.ts`, `infrastructure/cart.mock.ts` | — |
| `Header.jsx` → `Header` | `layouts/storefront/Header.tsx` (`client:load`) | — |
| `Header.jsx` → `MiniCart` / `UserMenu` / `NotifPanel` / `LangSelect` / `TRENDING` | `features/cart/ui/MiniCart.tsx` / `features/auth/ui/UserMenu.tsx` / `features/notifications/ui/NotifPanel.tsx` / `layouts/storefront/LangSelect.tsx` / `features/search/ui/SearchBox.tsx` | — |
| `Home.jsx` | `pages/index.astro` + `features/catalog/ui/CategoryTiles.astro`, `features/promotions/ui/{FlashBand,SaveVoucher}.tsx`, `features/product/ui/ProductFeed.tsx` | SSR + islands |
| `Shared.jsx` → `Banner*`, `BannerPopup` | `features/promotions/ui/banner/` | SSR + `client:visible` |
| `Shared.jsx` → `FlashBar`, `Seal`, `CardX` | `features/promotions/ui/FlashBar.tsx`, `shared/ui/Seal.tsx`, `shared/ui/Card.tsx` | — |
| `Shared.jsx` → `MoneyRail`, `MoneyChip`, `HoldNote`, `VietQrPanel`, `PayModal`, `PayGateway`, `FakeQr` | `features/payment/ui/` (`FakeQr` → `shared/ui/QrPlaceholder.tsx` vì Admin cũng dùng) | — |
| `Discover.jsx` | `pages/discover.astro` + `features/product/ui/DiscoverGrid.tsx` | SSR + islands |
| `Category.jsx` | `pages/c/[slug].astro` + `features/catalog/ui/` | SSR + islands |
| `Search.jsx` | `pages/search.astro` + `features/search/ui/` | SSR + islands |
| `ProductDetail.jsx` | `pages/p/[slug]-[id].astro` + `features/product/ui/` ; phần đánh giá → `features/reviews/ui/` | SSR + islands |
| `Shop.jsx` | `pages/shop/[id].astro` + `features/shop/ui/` | SSR + islands |
| `Campaign.jsx` | `pages/campaign/[slug].astro` + `features/promotions/ui/CampaignPage.astro` | SSR |
| `Pages.jsx` + `pages-data.js` | `pages/policies/[slug].astro` + `features/policies/` | SSR tĩnh |
| `Cart.jsx` (`Cart`, `VariantModal`) | `pages/cart.astro` + `features/cart/ui/` | khung + island |
| `VoucherPicker.jsx` | `features/promotions/ui/VoucherPicker.tsx` | — |
| `Checkout.jsx` | `pages/checkout.astro` + `features/checkout/ui/` | khung + island |
| `Account.jsx` → `Account`, `ProfilePanel`, `EmailChangeModal`, `PasswordPanel`, `LoginsPanel`, `DevicesPanel` | `pages/account/[...section].astro` + `features/account/ui/` | khung + island |
| `Account.jsx` → `AddressPanel` ; `AddressModal.jsx` | `features/account/ui/address/` | — |
| `Account.jsx` → `OrdersPanel`, `OrderGroup`, `OrderCard`, `Actions` | `pages/orders/index.astro` + `features/orders/ui/` | khung + island |
| `Account.jsx` → `VouchersPanel` / `CoinsPanel` / `NotificationsPanel` | `features/promotions/ui/` / `features/coins/ui/` / `features/notifications/ui/` | — |
| `Account.jsx` → `WishlistPanel` / `FollowsPanel` | `features/product/ui/WishlistPanel.tsx` / `features/shop/ui/FollowsPanel.tsx` | — |
| `OrderTracking.jsx` → `OrderTracking` / `ReturnModal` | `pages/orders/[id].astro` + `features/orders/ui/` / `features/refunds/ui/ReturnModal.tsx` | khung + island |
| `ReviewModal.jsx` | `features/reviews/ui/ReviewModal.tsx` | — |
| `ChatWidget.jsx` | `features/chat/ui/ChatWidget.tsx` (`client:only`) + `pages/chat.astro` | `client:only` |
| `BecomeSeller.jsx` | `pages/become-seller.astro` + `features/seller-onboarding/ui/BecomeSeller.tsx` | SSR + island |
| `data.jsx` (dữ liệu) | `features/*/infrastructure/*.mock.ts` theo từng miền | — |
| `data.jsx` (hàm tính: `priceCart`, `bestVouchers`, `voucherCheck`, `voucherDiscount`, `shipFee`, `lineTotal`, `eligibleBase`, `badgeStatus`) | `features/{cart,promotions,checkout}/domain/*.rules.ts` + test Vitest | — |

### 4.2 Auth

| Nguồn (`ui_kits/auth/index.html`) | Đích |
|---|---|
| `Panel` | `layouts/AuthLayout.astro` |
| `Auth` (đăng nhập mật khẩu / OTP, khóa 15 phút sau 5 lần sai) | `features/auth/ui/LoginForm.tsx` → `pages/auth/login.astro` |
| `Register` | `features/auth/ui/RegisterForm.tsx` → `pages/auth/register.astro` |
| `Forgot` | `features/auth/ui/ForgotForm.tsx` → `pages/auth/forgot.astro` |
| `OTPInput`, `PasswordInput` | `shared/ui/OtpInput.tsx`, `shared/ui/PasswordInput.tsx` (gộp với bản ở Account / Vendor / Admin) |
| `SocialButton`, `SsoDialog` | `features/auth/ui/SocialButton.tsx`, `SsoDialog.tsx` |
| `passwordIssue`, độ mạnh mật khẩu | `features/auth/domain/password.rules.ts` |
| `useCountdown` | `shared/hooks/useCountdown.ts` |
| (prototype không có) | `pages/auth/oauth-callback.astro`: trạng thái đang xử lý / lỗi |

### 4.3 Seller Center (SPA dưới `/seller/*`)

| Nguồn (`ui_kits/vendor/`) | Feature đích | Route |
|---|---|---|
| `VendorShell.jsx` (`VendorShell`, `NotifBell`, `AccountMenu`, `SellerSearch`), `KycGate` trong `index.html` | `app/seller/` (layout, routes, guard KYC) | — |
| `Overview.jsx` | `seller-dashboard` | `/seller` |
| `Fulfillment.jsx` → `Fulfillment` | `seller-orders` | `/seller/orders` |
| `Fulfillment.jsx` → `Returns` | `seller-orders` | `/seller/returns` |
| `Products.jsx` → `Products` | `seller-products` | `/seller/products` |
| `Products.jsx` → `Editor` và 12 khối con | `seller-products` | `/seller/products/new`, `/seller/products/:id/edit` |
| `VendorCatalog.jsx` | `seller-products` | `/seller/catalog` |
| `Wallet.jsx` | `seller-wallet` | `/seller/wallet` |
| `Onboarding.jsx` (`Onboarding`, `CameraModal`) | `seller-onboarding` | `/seller/kyc` |
| `VendorPages.jsx` → `Promo`, `VoucherFormModal` | `seller-promotions` | `/seller/promotions`, `/seller/flash-sales` |
| `Chat.jsx` | `chat` (`ui/SellerChatPage.tsx`) | `/seller/chat` |
| `VendorPages.jsx` → `Reviews`, `ReviewCard` | `seller-reviews` | `/seller/reviews` |
| `VendorPages.jsx` → `Stats` | `seller-analytics` | `/seller/analytics` |
| `VendorPages.jsx` → `Settings`, `SecuritySection` | `seller-settings` | `/seller/settings` |
| `data.jsx` | `*.mock.ts` của từng feature seller | — |

### 4.4 Admin Portal (SPA dưới `/admin/*`)

| Nguồn (`ui_kits/admin/`) | Feature đích | Route | Mã quyền |
|---|---|---|---|
| `AdminShell.jsx` (`AdminShell`, `AdminBell`) | `app/admin/` (layout, routes, nav lọc theo quyền) | — | — |
| `Dashboard.jsx` | `admin-dashboard` | `/admin` | `VIEW_DASHBOARD` |
| `KYCReview.jsx` (`KYCReview`, `DocViewer`) | `admin-kyc` | `/admin/kyc` | `REVIEW_KYC` |
| `Disputes.jsx` → `Refunds`, `GatewayCard` | `admin-disputes` | `/admin/refunds` | `HANDLE_DISPUTE` |
| `Disputes.jsx` → `Reviews` | `admin-moderation` | `/admin/reviews` | `MODERATE_REVIEW` |
| `ProductModeration.jsx` | `admin-moderation` | `/admin/product-moderation` | `MANAGE_CATALOG` |
| `Catalog.jsx` | `admin-catalog` | `/admin/catalog` | `MANAGE_CATALOG` |
| `Finance.jsx` → `Withdrawals` | `admin-finance` | `/admin/withdrawals` | `PROCESS_PAYOUT` |
| `CodReconciliation.jsx` | `admin-finance` | `/admin/cod-reconciliation` | `PROCESS_PAYOUT` |
| `Finance.jsx` → `Commission` | `admin-finance` | `/admin/commission` | `UPDATE_COMMISSION` |
| `Finance.jsx` → `Ledger` | `admin-finance` | `/admin/ledger` | `VIEW_FINANCE` |
| `Vouchers.jsx` | `admin-marketing` | `/admin/vouchers` | `MANAGE_VOUCHER` |
| `FlashSale.jsx` | `admin-marketing` | `/admin/flash-sales` | `MANAGE_VOUCHER` |
| `Banners.jsx` | `admin-marketing` | `/admin/banners` | `MANAGE_VOUCHER` |
| `PlatformNotifications.jsx` | `admin-marketing` | `/admin/notifications` | `MANAGE_PLATFORM_NOTIFICATION` |
| `AuditLog.jsx` | `admin-system` | `/admin/audit-logs` | `VIEW_AUDIT_LOG` |
| `Users.jsx` | `admin-system` | `/admin/users` | `MANAGE_USER` |
| `Finance.jsx` → `Rbac`, `InviteStaff`, `RbacEditor` | `admin-system` | `/admin/staff` | `MANAGE_RBAC` |
| `Finance.jsx` → `AccountAdmin` | `admin-system` | `/admin/account` | — |

### 4.5 Route storefront (thay cho `?screen=` của prototype)

| `?screen=` | Route | File |
|---|---|---|
| `home` | `/` | `pages/index.astro` |
| `discover` | `/discover` | `pages/discover.astro` |
| `category&cat=` | `/c/{slug}` | `pages/c/[slug].astro` |
| `product&product=` | `/p/{slug}-{id}` | `pages/p/[slug]-[id].astro` |
| `shop&shop=` | `/shop/{id}` | `pages/shop/[id].astro` |
| `search&q=&scope=&shop=` | `/search?q=&scope=&shop=` | `pages/search.astro` |
| `campaign` | `/campaign/{slug}` | `pages/campaign/[slug].astro` |
| `page&id=` | `/policies/{slug}` | `pages/policies/[slug].astro` |
| `cart` | `/cart` | `pages/cart.astro` |
| `checkout` | `/checkout` | `pages/checkout.astro` |
| `done`, `payfail` | `/checkout/result?status=` | `pages/checkout/result.astro` |
| `account&view=orders` | `/orders` | `pages/orders/index.astro` |
| `tracking&order=` | `/orders/{id}` | `pages/orders/[id].astro` |
| `account&view=<khác>` | `/account/{profile\|address\|password\|logins\|devices\|notifications\|vouchers\|points\|wishlist\|following}` | `pages/account/[...section].astro` |
| `seller` | `/become-seller` | `pages/become-seller.astro` |
| `../auth/index.html?screen=login` | `/auth/login?next=` | `pages/auth/login.astro` |

---

## 5. Quy ước nhánh & commit (TS mục 13)

**Nhánh** — tách từ `develop`, tên `feature/GNE-<số>-<mo-ta>`, chữ thường, gạch nối. Đề xuất mỗi giai đoạn lớn một nhánh + một PR vào `develop` (squash): `feature/GNE-<số>-web-foundation` (G01–G06), `-web-storefront-public` (G07–G08), `-web-auth` (G09), `-web-buyer` (G10), `-seller-ui` (G11), `-admin-ui` (G12), `-web-polish` (G13). TS 13.7 khuyên PR ≤ ~400 dòng: giai đoạn nào quá lớn thì tách PR theo từng nhóm nhỏ (mỗi nhóm đã có commit riêng).

**Định dạng commit**

```
<type>(<scope>): <subject> [#GNE-<số>]
```

- `type`: `feat` (màn / component mới), `build` (npm, cấu hình build), `test`, `docs`, `refactor`, `chore`, `fix`.
- `scope` dùng trong plan này: `web` (storefront, shared, layout, auth), `seller-ui`, `admin-ui`, `deps`. Không tự thêm scope ngoài danh sách `commitlint.config.cjs`.
- `subject`: tiếng Anh, thể mệnh lệnh, chữ thường, không dấu chấm cuối, ≤ 72 ký tự; cả dòng đầu ≤ 100 ký tự.
- Body (nếu cần) viết tiếng Việt: vì sao, cách làm.
- Một commit = một thay đổi logic, **build được và test xanh** — vì vậy mỗi nhóm task nhỏ kết thúc bằng một task `COMMIT` sau khi qua Cổng kiểm tra (mục 6).
- Thông điệp commit ghi sẵn ở từng task `COMMIT` chưa kèm mã Jira: thêm ` [#GNE-<số>]` vào cuối khi đã có mã.
- Người commit: danh tính git đang config. Kiểm bằng `git config user.name` trước commit đầu tiên; không truyền `--author`, không `--no-verify`.

**Lệnh mẫu**

```bash
git switch develop && git pull
git switch -c feature/GNE-<số>-web-foundation
git add web/src/styles web/tailwind.config.ts web/public/fonts
git commit -m "feat(web): port design tokens, fonts and tailwind theme from prototype [#GNE-<số>]"
git push -u origin feature/GNE-<số>-web-foundation
```

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

Chạy trong `web/` (gộp thành `npm run verify` ở G01.07):

1. `npm run lint` — 0 lỗi, kể cả luật `boundaries`.
2. `npm run check` — `astro check` + `tsc --noEmit` sạch, không `any`.
3. `npm run test` — Vitest xanh.
4. `npm run build` — build SSR thành công.
5. `node ../scripts/check-i18n.mjs` — `vi.json` và `en.json` khớp khóa.
6. So hình với prototype (`python -m http.server 8000` trong `MultivendorEcommercePrototype`) ở 360 / 768 / 1440 px cho các màn vừa làm; kiểm bàn phím (Tab, Esc, focus nhìn thấy).
7. Tick các task tương ứng trong `progress_chuyen_giao_dien.md`, ghi mã commit.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Gxx.yy` = task; `Gxx.Cn` = task commit (làm sau khi qua mục 6). Các task trong một nhóm làm theo thứ tự trên xuống.

### G00 — Chuẩn bị repo, nhánh, môi trường

- **[G00.01]** Kiểm danh tính git — chạy `git config user.name` và `git config user.email` trong repo, xác nhận đúng tài khoản đã config (`Thien211105`); không đổi config, không dùng `--author`.
- **[G00.02]** Chốt 7 quyết định D1–D7 — đọc mục 2, ghi lựa chọn vào đầu file progress.
- **[G00.03]** Tạo mã Jira — 1 Epic "Chuyển giao diện prototype sang web" + Story cho từng giai đoạn; ghi số vào progress.
- **[G00.04]** Tạo nhánh `develop` — repo chỉ có `main`: `git switch -c develop main && git push -u origin develop` (TS 13.1).
- **[G00.05]** Tạo nhánh làm việc — `git switch -c feature/GNE-<số>-web-foundation develop`.
- **[G00.06]** Cài công cụ gốc — `npm install` ở gốc repo để kích hoạt Husky; thử một commit sai định dạng để chắc commitlint chặn.
- **[G00.07]** Cài phụ thuộc web — `npm ci` trong `web/`.
- **[G00.08]** Đo baseline — chạy `npm run lint`, `npm run check`, `npm run test`, `npm run build`; ghi lỗi có sẵn của scaffold vào mục Ghi chú của progress để sửa ở G01.
- **[G00.09]** Dựng bản đối chiếu — chạy prototype ở cổng 8000; mở 4 surface `#storefront`, `#vendor`, `#admin`, `#auth`; ghi tài khoản demo (`Genie@123`, OTP `123456`, `?guest=1`).
- **[G00.10]** Chụp ảnh tham chiếu — mỗi màn ở 360 / 768 / 1440 px, lưu ngoài repo (hoặc dùng `MultivendorEcommercePrototype/.impeccable/review/` đã có) để so ở mục 6.
- **[G00.C1]** COMMIT — `docs: add web ui migration plan and progress tracker`

### G01 — Công cụ & cấu hình `web/`

- **[G01.01]** Prettier — tạo `.prettierrc.json` (plugin `prettier-plugin-astro`, `singleQuote`, `printWidth: 100` theo kiểu mã hiện có) và `.prettierignore` (`dist`, `.astro`, `public`).
- **[G01.02]** Vitest — tạo `vitest.config.ts` (alias `@` → `src`, include `src/**/*.test.ts`), thêm 1 test mẫu để `npm run test` không rỗng.
- **[G01.03]** Playwright — tạo `playwright.config.ts` (`baseURL` 4321, `webServer: npm run dev`, 2 project: mobile 360 px và desktop 1440 px).
- **[G01.04]** Biến môi trường — tạo `web/.env.example` (`PUBLIC_WEB_URL`, `PUBLIC_API_URL`, `INTERNAL_API_URL`, `PUBLIC_S3_URL`, `PUBLIC_USE_MOCK`).
- **[G01.05]** Kiểu môi trường — cập nhật `shared/config/env.ts` (zod, thêm cờ boolean `PUBLIC_USE_MOCK`) và `src/env.d.ts` (`ImportMetaEnv`, `App.Locals.lang`).
- **[G01.06]** ESLint boundaries — khai thêm element cho `src/i18n/**`, `src/styles/**`, `src/assets/**`, `src/middleware.ts`; cho phép `shared-lib → i18n`, `layouts → styles / assets`; kiểm hành vi với file chưa thuộc element nào.
- **[G01.07]** ESLint import sâu — thêm `no-restricted-imports` chặn `@/features/*/{domain,application,infrastructure,ui}/**` từ ngoài feature (dự phòng cho luật boundaries, TS 5.2).
- **[G01.08]** ESLint cho test & cấu hình — override cho `*.test.ts`, `tests/**`, `*.config.*` (nới luật boundaries, giữ `no-explicit-any`).
- **[G01.09]** Script `verify` — thêm `"verify": "npm run lint && npm run check && npm run test && npm run build && node ../scripts/check-i18n.mjs"` vào `web/package.json`.
- **[G01.10]** `check-i18n.mjs` — đọc script ở `scripts/`, bảo đảm so được khóa lồng nhiều cấp và báo khóa thiếu / thừa; sửa nếu chưa.
- **[G01.11]** Sửa lỗi baseline — xử lý các lỗi lint / check ghi ở G00.08 để `npm run verify` xanh trên scaffold.
- **[G01.C1]** COMMIT — `build(web): add prettier, vitest, playwright config and env example`

### G02 — Token thiết kế, font, theme Tailwind, CSS nền

**Nhóm A — Token**

- **[G02.01]** Font tệp — chép 24 file `assets/fonts/*.woff2` (Be Vietnam Pro 400–800, IBM Plex Mono 400–600; mỗi cỡ 3 tập `latin`, `latin-ext`, `vietnamese`) vào `web/public/fonts/`.
- **[G02.02]** Token font — chuyển `tokens/fonts.css` vào `styles/tokens.css`: `@font-face` trỏ `/fonts/…`, giữ `unicode-range`, `font-display: swap`.
- **[G02.03]** Token màu — chuyển `tokens/colors.css`: toàn bộ thang (`--brand-*`, `--sky-*`, `--flash-*`, `--gold-*`, `--mint-*`, đỏ, hổ phách, trung tính) và bí danh ngữ nghĩa; **giữ nguyên tên biến** để CSS storefront / theme dùng lại được.
- **[G02.04]** Token chữ — chuyển `tokens/typography.css` (`--type-*`, họ font, cỡ, độ đậm, giãn dòng).
- **[G02.05]** Token khoảng cách — chuyển `tokens/spacing.css`.
- **[G02.06]** Token hiệu ứng — chuyển `tokens/effects.css` (đổ bóng, bo góc `--radius-*`, vòng focus, thời lượng chuyển động).
- **[G02.07]** CSS nền — chuyển `tokens/base.css` sang `styles/base.css` trong `@layer base` (reset, `body`, `:focus-visible`, `prefers-reduced-motion`).
- **[G02.08]** Sửa `global.css` — đưa `@import` lên trước `@tailwind`; thứ tự: tokens → base → `@tailwind base/components/utilities` → CSS component → responsive.
- **[G02.C1]** COMMIT — `feat(web): port design tokens and base styles from prototype`

**Nhóm B — Tailwind**

- **[G02.09]** Tailwind màu — `tailwind.config.ts`: ánh xạ `colors` sang biến CSS (thang `brand`, `sky`, `flash`, `gold`, `mint`, `red`, `amber`, `neutral` + ngữ nghĩa `primary`, `surface`, `ink`, `muted`, `subtle`, `line`).
- **[G02.10]** Tailwind hình khối — `borderRadius` (`sm/md/lg/pill` từ `--radius-*`), `boxShadow` từ token đổ bóng, `ringColor` cho focus.
- **[G02.11]** Tailwind chữ — `fontFamily` (`sans`, `mono`, `display`), `fontSize` theo thang `--type-*` (kèm `lineHeight`, `fontWeight`).
- **[G02.12]** Tailwind bố cục — `spacing` theo token, `screens` `sm/md/lg` lấy đúng điểm gãy trong `responsive.css`, `container` tối đa 1440 px.
- **[G02.13]** Responsive — rà `responsive.css` (63 dòng): quy tắc nào thay được bằng tiền tố `sm:/md:/lg:` thì ghi chú cho component; phần còn lại đưa vào `styles/responsive.css`.
- **[G02.14]** Kiểm kê lớp `lz-*` — đọc `genie-components.css` (191 dòng), lập bảng lớp → component `shared/ui` sẽ nhận (ghi vào progress) để G06 viết lại bằng utility.
- **[G02.15]** Trang thư viện nội bộ — tạo `pages/dev/ui.astro` (trả 404 khi không phải `DEV`) hiển thị bảng màu, thang chữ, bo góc, đổ bóng để so với `DESIGN.md`.
- **[G02.C2]** COMMIT — `feat(web): map prototype tokens into tailwind theme`

### G03 — Tài sản tĩnh & thương hiệu

- **[G03.01]** Ảnh sản phẩm mẫu — chép 22 ảnh `assets/img/*.jpg` vào `public/img/products/`.
- **[G03.02]** Banner — chép 15 ảnh `assets/img/banners/*.jpg` vào `public/img/banners/`.
- **[G03.03]** Logo thanh toán — chép `assets/payments/*` (`vnpay.png`, `card.svg`, `momo-logo.png`, `zalopay-logo.png`) vào `public/img/payments/`; đánh dấu logo nào ngoài phạm vi TS 3.5 (chỉ VNPay / VietQR / COD) để không hiển thị.
- **[G03.04]** SVG thương hiệu — chép `logomark.svg`, `assets/brand/genie-lamp.svg`, `genie-coin.svg`, `coin-backdrop.png` vào `src/assets/brand/`.
- **[G03.05]** Favicon — thay `public/favicon.svg` bằng bản của prototype, thêm `favicon.ico`.
- **[G03.06]** `GenieMark` / `GenieLogo` — chuyển `brand.jsx` sang `shared/ui/brand/GenieMark.tsx`, `GenieLogo.tsx` (có kiểu, không phụ thuộc `window`).
- **[G03.07]** `GenieCoin` — đọc `assets/brand/genie-coin.js`, viết lại thành `shared/ui/brand/GenieCoin.tsx`.
- **[G03.08]** Font logo — kiểm `GenieLogo` có dùng `MJMilestoneScript.ttf` / `Baloo2` không; nếu có: kiểm giấy phép, chuyển sang woff2, khai `@font-face`.
- **[G03.09]** Ảnh nền theme — `dongson-drum.svg` (1,7 MB), `hoian-lanterns.svg`: tối ưu bằng SVGO, chỉ nạp khi theme tương ứng bật.
- **[G03.10]** Hằng đường dẫn ảnh — `shared/lib/assets.ts`: hàm `productImg(name)`, `bannerImg(name)` để mock không ghi cứng đường dẫn.
- **[G03.C1]** COMMIT — `feat(web): add brand assets and mock catalog images`

### G04 — `shared/lib`, hook dùng chung, nền i18n

- **[G04.01]** `cn()` — `shared/lib/cn.ts` ghép lớp có điều kiện (thay các đoạn `[..].filter(Boolean).join(' ')` của prototype).
- **[G04.02]** Định dạng tiền — `shared/lib/format.ts`: `formatVND`, `partsVND` (chuyển từ `Money` / `PriceVND` trong `_ds_bundle.js`) bằng `Intl.NumberFormat`.
- **[G04.03]** Định dạng ngày & số — `formatDate`, `formatDateTime`, `formatRelative`, `formatCompact` (số đã bán "1,2k" — thay `parseSold` lặp ở Category / Search / Shop).
- **[G04.04]** Che dữ liệu — `maskEmail`, `maskPhone`, `maskAccount` (chuyển `maskEmail` ở `Account.jsx`; dạng `0792******34` theo TS 9).
- **[G04.05]** Test định dạng — `format.test.ts` cho 4 nhóm hàm trên ở cả `vi` và `en`.
- **[G04.06]** Lưu trình duyệt — `shared/lib/storage.ts`: tiền tố `genie.`, đọc / ghi JSON an toàn, không lỗi khi SSR; bảng khóa: `genie.cart.guest`, `genie.theme`, `genie.popup.seen`, `genie.wishlist`.
- **[G04.07]** Hàm `t()` — `shared/lib/i18n.ts`: `t(key, params?, locale?)`, `createT(locale)` cho Astro, tra khóa lồng nhau, nội suy `{name}`, thiếu khóa thì rơi về `vi` và cảnh báo ở DEV.
- **[G04.08]** Hook `useT()` — `shared/hooks/useT.ts` đọc locale từ nanostore `$locale` (island) ; đồng bộ với cookie `lang` mà `middleware.ts` đọc.
- **[G04.09]** Test i18n — `i18n.test.ts`: khóa lồng, nội suy, rơi về `vi`, khóa không tồn tại.
- **[G04.10]** Quy ước không gian khóa — viết ở đầu `vi.json`: `common.action.*`, `common.state.*`, `errors.GNE-*`, `<feature>.<màn>.<ý nghĩa>`; nhập bộ `common` (Tất cả, Sửa, Xóa, Hủy, Lưu, Đóng, Xác nhận, Thêm, Chi tiết…).
- **[G04.11]** Công cụ tra bản dịch — script một lần `scripts/i18n-lookup.mjs`: đọc từ điển EN trong `MultivendorEcommercePrototype/genie/i18n.js`, nhập chuỗi tiếng Việt → trả chuỗi tiếng Anh, dùng khi điền `en.json`.
- **[G04.12]** SEO — `shared/lib/seo.ts`: `buildMeta()`, thẻ Open Graph, JSON-LD `Product` và `BreadcrumbList`.
- **[G04.13]** Hook dùng chung — `shared/hooks/`: `useCountdown` (từ auth), `useClickOutside` (từ `Select`), `useMediaQuery`, `useFocusTrap` (từ `Modal`), `useScrollLock`.
- **[G04.14]** Cập nhật boundaries — khai element `shared-hooks` nếu cần để `feature-domain` không import được hook.
- **[G04.C1]** COMMIT — `feat(web): add i18n, formatting, storage and shared hooks`

### G05 — `shared/api`, phiên đăng nhập, lớp mock

- **[G05.01]** Kiểu phong bì API — `shared/api/types.ts`: `ApiResponse<T>`, `ApiErrorBody`, `CursorPage<T>` đúng TS mục 8 (`snake_case`).
- **[G05.02]** Lỗi — `shared/api/errors.ts`: lớp `ApiError`, ánh xạ mã `GNE-*` → khóa i18n `errors.*`; nhập các mã prototype đang hiển thị (đổi `LZD-INV-4090` → `GNE-INV-4090`).
- **[G05.03]** `http.ts` — bọc `fetch`: gắn access token, bóc phong bì, làm mới 1 lần khi 401.
- **[G05.04]** `server-http.ts` — gọi nội bộ khi SSR qua `INTERNAL_API_URL`.
- **[G05.05]** `idempotency.ts` — sinh `Idempotency-Key` UUIDv4 cho checkout, thanh toán, rút tiền, hoàn tiền.
- **[G05.06]** Lớp mock — `shared/api/mock.ts`: cờ `USE_MOCK`, `mockDelay()`, `mockOk()`, `mockFail(code)`, phân trang cursor giả; một bộ dữ liệu mặc định, không có công tắc kịch bản (D5).
- **[G05.07]** Phiên — `shared/auth/session.ts`: access token trong bộ nhớ, nanostore `$session`, cờ cookie `genie_logged_in` cho `middleware.ts`, người dùng mock, chế độ khách.
- **[G05.08]** Quyền — `shared/auth/permissions.ts`: 13 mã quyền + preset `Admin` / `Moderator` / `CS Specialist` / `Finance Officer` (từ `admin/data.jsx` `PERMS`, `ROLE_PERMS`); `usePermissions.ts` trả `can(code)`.
- **[G05.09]** Providers — `app/providers.tsx`: `QueryClientProvider`, khởi tạo locale, vùng Toast.
- **[G05.10]** Test — `errors.test.ts`, `mock.test.ts` (phân trang), `permissions.test.ts`.
- **[G05.C1]** COMMIT — `feat(web): add api client, session, permissions and mock layer`

### G06 — `shared/ui` (design system)

Mỗi component: đọc bản trong `_ds_bundle.js` + lớp `lz-*` tương ứng → viết `.tsx` có kiểu `Props`, utility Tailwind, đủ trạng thái hover / focus / disabled / lỗi, có `aria-*`; thêm vào `pages/dev/ui.astro`.

**Nhóm A — Phần tử cơ bản**

- **[G06.01]** `Icon` — `shared/ui/icon/icons.ts` (bảng `ICONS` + kiểu `IconName`), `Icon.tsx` (`size`, `strokeWidth`, `title`).
- **[G06.02]** `Button` — biến thể, cỡ, `iconLeft` / `iconRight`, `loading`, dạng liên kết `href`.
- **[G06.03]** `IconButton` — vùng chạm 44×44, bắt buộc `label`.
- **[G06.04]** `Input` — nhãn, gợi ý, lỗi, tiền tố / hậu tố, `forwardRef` để dùng với react-hook-form.
- **[G06.05]** `Textarea` — cùng kiểu `Input`, đếm ký tự.
- **[G06.06]** `PasswordInput` — gộp `PasswordInput` (auth), `PwField` (Account), `VPwField` (vendor), `APwField` (admin): nút hiện / ẩn + thanh độ mạnh.
- **[G06.07]** `Checkbox`, `Radio`, `Switch` — 3 component, hỗ trợ trạng thái `indeterminate` cho Checkbox (chọn tất cả ở giỏ).
- **[G06.08]** `Select` — danh sách thả có bàn phím, đóng khi bấm ngoài.
- **[G06.09]** `Tabs`, `Segmented` — vai trò `tablist`, phím mũi tên.
- **[G06.10]** `Badge`, `StatusBadge`, `Tag` — `StatusBadge` nhận mã trạng thái và tra nhãn qua `t()` (thay `STATUS_WORDS`).
- **[G06.11]** `Avatar` — ảnh hoặc chữ cái đầu, bảng màu theo tên.
- **[G06.C1]** COMMIT — `feat(web): add base form and display components to shared ui`

**Nhóm B — Lớp phủ & phản hồi**

- **[G06.12]** `Modal` — bẫy focus, Esc, khóa cuộn, `aria-modal`, cỡ; kèm `ConfirmDialog`.
- **[G06.13]** `Popover` / `Dropdown` — nền cho menu tài khoản, chuông thông báo, mini-cart.
- **[G06.14]** `Toast` — store nanostores `$toasts` + `toast.info / error / success()`; thay các hàm `toast()` rải ở 4 file prototype.
- **[G06.15]** `EmptyState`, `Skeleton`, `Spinner` — trạng thái rỗng / đang tải.
- **[G06.16]** `OtpInput` — gộp `OTPInput` (auth), `OtpBoxes` (Account), `VOtpBoxes`, `AOtpBoxes`: 6 ô, tự chuyển focus, dán cả chuỗi, trạng thái khóa.
- **[G06.17]** `Countdown` — gộp bản ở Home / Checkout / `UntilMidnight`.
- **[G06.18]** `Pagination` — từ `pageBtn` / `pageList` của `Discover.jsx`.
- **[G06.19]** `Card`, `PanelHead`, `SectionHead` — khung thẻ và tiêu đề khối dùng lặp ở nhiều màn.
- **[G06.C2]** COMMIT — `feat(web): add overlay and feedback components to shared ui`

**Nhóm C — Thành phần thương mại**

- **[G06.20]** `Price` — giá hiện tại, giá gạch, phần trăm giảm (từ `Money`, `PriceVND`).
- **[G06.21]** `Rating` — sao nửa, chế độ nhập cho form đánh giá.
- **[G06.22]** `QuantityStepper` — min / max theo tồn kho, nhập tay.
- **[G06.23]** `ProductCard` — ảnh, nhãn (Chính hãng, Flash Sale, Đặt trước, hết hàng), giá, đã bán, tim yêu thích dạng slot.
- **[G06.24]** `VendorChip`, `Seal` — tên shop + dấu Chính hãng / đã xác minh.
- **[G06.25]** `VoucherTag`, `Ticket` — nhãn và phiếu voucher (dạng sàn / shop / freeship).
- **[G06.26]** `QrPlaceholder` — gộp `FakeQr` (storefront, admin).
- **[G06.27]** `DataTable` — thay bộ `hrow / brow / th / td` lặp ở 8 file admin / vendor: tiêu đề, hàng, ô, trạng thái rỗng, cuộn ngang ở màn hẹp.
- **[G06.28]** `Field`, `FormSection`, `DateInput` — nhãn + ô nhập cho form quản trị (thay `lab`, `dateInput`, `Section`).
- **[G06.C3]** COMMIT — `feat(web): add commerce and data components to shared ui`

**Nhóm D — Bộ thống kê (`dash.jsx` + `dash.css`)**

- **[G06.29]** `Delta`, `StatTile`, `Kpi` — `shared/ui/dash/`.
- **[G06.30]** `WorkQueue`, `Pipeline` — hàng việc cần làm, đường ống trạng thái đơn.
- **[G06.31]** `BarList`, `StackBar`, `SegBar` — thanh xếp hạng / tỉ lệ.
- **[G06.32]** `TimeChart` — biểu đồ thời gian SVG (kèm `niceStep`), có mô tả cho trình đọc màn hình.
- **[G06.33]** `dash.css` — chuyển phần chưa thành utility vào `styles/dash.css`.
- **[G06.34]** Test hàm thuần — `niceStep`, rút gọn số, tính phần trăm thay đổi.
- **[G06.C4]** COMMIT — `feat(web): add dashboard chart components to shared ui`

**Nhóm E — Rà soát**

- **[G06.35]** Xuất tập trung — `shared/ui/index.ts` xuất mọi component.
- **[G06.36]** Thư viện nội bộ — bổ sung đủ biến thể mọi component vào `pages/dev/ui.astro`; so từng cái với prototype.
- **[G06.37]** Xóa stub cũ — bỏ 9 file stub `return null` đã được thay.
- **[G06.C5]** COMMIT — `feat(web): add component gallery and shared ui barrel`

### G07 — Layout & khung trang

**Nhóm A — CSS storefront**

- **[G07.01]** Tách `storefront.css` — chia 624 dòng theo khối vào `styles/storefront/` (`shell`, `header`, `footer`, `home`, `product`, `cart`, `checkout`, `account`, `order`, `search`, `shop`, `chat`), bọc `@layer components`.
- **[G07.02]** Chuẩn hóa giá trị — thay màu / px cứng trong CSS vừa tách bằng biến token.
- **[G07.03]** Nhập vào `global.css` — đúng thứ tự, kiểm không lớp `tr-*` nào bị Tailwind purge (thêm `safelist` nếu lớp ghép động).
- **[G07.C1]** COMMIT — `feat(web): port storefront stylesheet into layered css modules`

**Nhóm B — Layout**

- **[G07.04]** `SeoHead.astro` + `<head>` của `StorefrontLayout.astro` — `layouts/partials/SeoHead.astro` nhận `title`, `description`, `canonical`, `image`, `noindex`, `jsonLd[]`; mẫu tiêu đề `%s | Genie`, Open Graph, `og:locale`, thẻ Twitter; layout nạp font, đặt `lang`.
- **[G07.05]** `ThemeBoot.astro` — đặt ở `layouts/partials/`, dùng cho cả 3 layout; script nội tuyến đặt `data-theme` trước khi vẽ (từ `themes.js`: `?theme=` → storage → mặc định `genie`), tránh nháy.
- **[G07.06]** Khung thân — liên kết "Bỏ qua tới nội dung chính", `<main id="main">`, biến thể lớp gốc (`tr-root--bar`, `tr-root--cart`, `tr-main--home`) qua `Props`.
- **[G07.07]** `ToastHost.tsx` — vùng hiện toast dùng chung cho mọi island (`client:idle`).
- **[G07.08]** `AuthLayout.astro` — khung 2 cột từ `Panel` (auth): cột thương hiệu + cột form, thu 1 cột ở màn hẹp.
- **[G07.09]** `AppShellLayout.astro` — khung trống cho SPA: `<head>`, font, `dash.css`, không header storefront.
- **[G07.10]** `middleware.ts` — đọc cookie `lang`, gắn `locals.lang`; chuyển hướng `/seller`, `/admin`, và các trang người mua cần đăng nhập về `/auth/login?next=`.
- **[G07.11]** `404.astro` — trang không tìm thấy theo ngôn ngữ thiết kế.
- **[G07.C2]** COMMIT — `feat(web): build storefront, auth and app shell layouts`

**Nhóm C — Header & Footer**

- **[G07.12]** Thanh tiện ích trên cùng — liên kết Kênh người bán, trợ giúp, `LangSelect` (đổi cookie `lang`, tải lại).
- **[G07.13]** Thanh chính — logo, nút danh mục, ô tìm kiếm, giỏ, chuông, tài khoản; `Header.tsx` `client:load`.
- **[G07.14]** `SearchBox` — `features/search/ui/`: gợi ý khi gõ, từ khóa thịnh hành (`TRENDING`), gửi sang `/search?q=`.
- **[G07.15]** `MiniCart` — `features/cart/ui/`: đọc số lượng từ `cart.store.ts` (nanostores), danh sách rút gọn, nút sang giỏ.
- **[G07.16]** `UserMenu` — `features/auth/ui/`: trạng thái khách (Đăng nhập / Đăng ký) và đã đăng nhập (menu tài khoản, đăng xuất).
- **[G07.17]** `NotifPanel` — `features/notifications/ui/`: danh sách rút gọn, số chưa đọc.
- **[G07.18]** Menu danh mục & điều hướng màn hẹp — ngăn kéo danh mục ở 360 px.
- **[G07.19]** `Footer.astro` — các nhóm liên kết (28 trang chính sách), dạng xếp gập ở màn hẹp bằng `<details>` (không cần JS), nhóm "Bán hàng".
- **[G07.20]** `LoginPrompt` — hộp nhắc đăng nhập khi khách bấm thao tác cần tài khoản.
- **[G07.21]** Khóa i18n khung trang — nhập `layout.header.*`, `layout.footer.*` cho `vi` và `en`.
- **[G07.C3]** COMMIT — `feat(web): add storefront header, footer and global overlays`

**Nhóm D — Đủ 9 theme (D1 đã chốt)**

- **[G07.22]** `themes.css` phần theme `genie` + `classic` — chuyển vào `styles/themes.css`.
- **[G07.23]** 7 theme còn lại — `hoian`, `dongho`, `dongson`, `polymer`, `buudien`, `saigon`, `sleeve`.
- **[G07.24]** Font theme — liệt kê họ font từng theme trong thẻ Google Fonts của prototype (Archivo, Alegreya, Baloo 2, Bai Jamjuree, Bricolage Grotesque…); tự lưu trữ dạng woff2 trong `public/fonts/`, khai `@font-face` để trình duyệt chỉ tải font của theme đang bật.
- **[G07.25]** `ThemePicker` — từ `brand.jsx` sang `shared/ui/brand/ThemePicker.tsx`; gắn đúng các chỗ prototype đang có: header storefront, góc trang auth, thanh trên Admin; lưu `genie.theme`, đồng bộ giữa các tab, phát lại hiệu ứng mở đầu (`tr-play`).
- **[G07.26]** Kiểm 9 theme — trang chủ, trang sản phẩm, trang đăng nhập, Seller, Admin và thư viện nội bộ ở 360 / 1440 px; kiểm tương phản chữ trên nền của từng theme.
- **[G07.C4]** COMMIT — `feat(web): port storefront themes and theme picker`

### G08 — Storefront công khai (SSR + islands)

**Nhóm A — Miền danh mục & sản phẩm**

- **[G08.01]** Kiểu domain sản phẩm — `features/product/domain/product.types.ts`: `Product`, `Variant`, `VariantGroup`, `ProductBadge`, `Brand` (từ `P`, `PRODUCTS`, `VARIANT_GROUPS`, `BADGE` trong `data.jsx`).
- **[G08.02]** Quy tắc sản phẩm — `product.rules.ts`: `badgeStatus`, phần trăm giảm, còn hàng / hết hàng / đặt trước, dựng slug `/p/{slug}-{id}`; kèm test.
- **[G08.03]** Hạ tầng sản phẩm — `product.dto.ts` (snake_case), `product.mock.ts` (20+ sản phẩm mẫu), `product.mapper.ts`, `product.api.ts` (`getProduct`, `listProducts`, `listRelated`).
- **[G08.04]** Kiểu & hạ tầng danh mục — `features/catalog/`: `Category`, `Subcategory`, `AttributeFilter` (từ `CATEGORIES`, `CATEGORY_META`, `SUBCATEGORIES`, `ATTR_FILTERS`, `BRANDS`, `VERIFIED_BRANDS`) + dto / mock / mapper / api.
- **[G08.05]** Kiểu & hạ tầng shop — `features/shop/`: `Shop` (từ `V`), trạng thái tạm nghỉ, dto / mock / mapper / api.
- **[G08.06]** API công khai feature — viết `index.ts` cho `product`, `catalog`, `shop`.
- **[G08.C1]** COMMIT — `feat(web): add catalog, product and shop domain with mock data`

**Nhóm B — Trang chủ**

- **[G08.07]** Miền banner — `features/promotions/`: kiểu `Banner`, `BannerSlot`; `banners()` lọc theo ngày (từ `BANNER_SEED`, `localDay`); dto / mock / mapper / api.
- **[G08.08]** `BannerCarousel`, `BannerSlot`, `BannerPair`, `BannerPicture` — `features/promotions/ui/banner/`; carousel `client:visible`, tạm dừng khi rê chuột / `prefers-reduced-motion`.
- **[G08.09]** `BannerPopup` — hiện 1 lần (`genie.popup.seen`), `client:idle`.
- **[G08.10]** `CategoryTiles.astro` — lưới danh mục tĩnh SSR.
- **[G08.11]** `FlashBand` — dải Flash Sale: đếm ngược đến nửa đêm, thanh tiến độ đã bán (`FLASH_SLOTS`, `FlashBar`).
- **[G08.12]** `SaveVoucher` — dải voucher lưu nhanh (`client:idle`).
- **[G08.13]** `ProductFeed` — "Gợi ý hôm nay" tải thêm khi cuộn (`client:visible`).
- **[G08.14]** Dải cam kết (Promise Strip trong `DESIGN.md`) — khối tĩnh SSR.
- **[G08.15]** `pages/index.astro` — lấy dữ liệu trong frontmatter qua `infrastructure`, ghép các khối, SEO trang chủ.
- **[G08.16]** Khóa i18n `home.*`.
- **[G08.C2]** COMMIT — `feat(web): build storefront home page`

**Nhóm C — Danh mục, tìm kiếm, khám phá**

- **[G08.17]** Quy tắc lọc & sắp xếp — `features/search/domain/`: `SORTS`, `PRICE_BANDS`, lọc theo thương hiệu / thuộc tính / khoảng giá / Chính hãng; trạng thái lọc ⇄ query string; kèm test.
- **[G08.18]** `FilterPanel` — nhóm lọc (`FilterGroup`, `Group`, `fRow`, `priceBox`), dạng ngăn kéo ở màn hẹp ("Bộ lọc" / "Đóng bộ lọc").
- **[G08.19]** `SortBar`, `ResultGrid` — thanh sắp xếp + lưới kết quả, trạng thái rỗng.
- **[G08.20]** `pages/c/[slug].astro` — đầu trang danh mục (banner danh mục, danh mục con, `BANDS`), SSR trang đầu, breadcrumb + JSON-LD.
- **[G08.21]** `pages/search.astro` — phạm vi tìm (`ScopeBtn`: toàn sàn / trong shop), khối shop khớp từ khóa, SSR theo `q`.
- **[G08.22]** `pages/discover.astro` — lưới phân trang (`Pagination`).
- **[G08.23]** Khóa i18n `catalog.*`, `search.*`, `discover.*`.
- **[G08.C3]** COMMIT — `feat(web): build category, search and discover pages`

**Nhóm D — Chi tiết sản phẩm**

- **[G08.24]** Thư viện ảnh — `ProductGallery.tsx` + `Lightbox` (phím mũi tên, Esc).
- **[G08.25]** Khối mua — tên, đánh giá, đã bán, giá, tiến độ Flash Sale, nhãn Đặt trước, chọn phân loại (`COLORS`, `SIZES` → `VariantPicker`), số lượng, tồn kho.
- **[G08.26]** `AddToCartButton` + Mua ngay — `features/cart/ui/`, ghi vào `cart.store.ts`; khách chưa đăng nhập ghi giỏ khách.
- **[G08.27]** Nút yêu thích & chia sẻ — `client:idle`; chia sẻ liên kết kèm phân loại.
- **[G08.28]** Khối shop — `VendorChip`, theo dõi, chat, trạng thái shop tạm nghỉ (`followBanner`).
- **[G08.29]** `SpecsTable.astro`, `ProductDescription.astro` — phần tĩnh SSR.
- **[G08.30]** Thanh mua dính đáy ở màn hẹp (`tr-root--bar`).
- **[G08.31]** `ProductGone` — trạng thái sản phẩm đã gỡ / không tồn tại (HTTP 404).
- **[G08.32]** Miền đánh giá — `features/reviews/`: kiểu `Review`, bộ lọc `R_FILTERS`, lý do báo cáo `REPORT_REASONS`; dto / mock / mapper / api.
- **[G08.33]** `ReviewList` — tổng điểm, lọc theo sao / có ảnh, đánh giá ẩn tên, phản hồi của shop, báo cáo đánh giá (`client:visible`).
- **[G08.34]** Sản phẩm tương tự — `client:visible`.
- **[G08.35]** `pages/p/[slug]-[id].astro` — SSR đầy đủ, `title` / `description` / OG ảnh sản phẩm, JSON-LD `Product` + `BreadcrumbList`, chuyển hướng khi slug sai.
- **[G08.36]** Khóa i18n `product.*`, `reviews.*`.
- **[G08.C4]** COMMIT — `feat(web): build product detail page with reviews`

**Nhóm E — Shop, Flash Sale, chiến dịch**

- **[G08.37]** `pages/shop/[id].astro` — đầu trang shop (`Stat`, theo dõi, chat), voucher shop, tìm trong shop, lưới sản phẩm, trạng thái tạm nghỉ.
- **[G08.38]** `pages/flash-sale.astro` — prototype chưa có màn riêng: dựng từ `FlashBar` + `FLASH_SLOTS` (các khung giờ, lưới sản phẩm theo khung), giữ đúng ngôn ngữ "Flash Band" trong `DESIGN.md`.
- **[G08.39]** `pages/campaign/[slug].astro` — từ `Campaign.jsx` + `CAMPAIGNS`.
- **[G08.40]** Khóa i18n `shop.*`, `promotions.flash.*`, `promotions.campaign.*`.
- **[G08.C5]** COMMIT — `feat(web): build shop, flash sale and campaign pages`

**Nhóm F — Trang chính sách & sitemap**

- **[G08.41]** Dữ liệu chính sách — chuyển `pages-data.js` (28 trang) sang `features/policies/infrastructure/policies.data.ts` có kiểu `PolicyPage` (nội dung giữ tiếng Việt như prototype).
- **[G08.42]** `PolicyPage.astro` — menu bên trái theo nhóm, nội dung, điều hướng trước / sau.
- **[G08.43]** `pages/policies/[slug].astro` — SSR tĩnh, 404 khi slug lạ.
- **[G08.44]** `pages/sitemap.xml.ts` — sinh từ danh mục, sản phẩm, shop, chính sách.
- **[G08.45]** `public/robots.txt` — trỏ sitemap, chặn `/seller`, `/admin`, `/account`, `/checkout`.
- **[G08.C6]** COMMIT — `feat(web): add policy pages and sitemap`

**Nhóm G — SEO kỹ thuật (rà toàn bộ trang công khai vừa dựng)**

- **[G08.46]** URL chuẩn — `shared/lib/seo.ts` thêm `canonicalUrl()`: bỏ tham số theo dõi, giữ `page`; trang lọc / sắp xếp trỏ canonical về trang danh mục gốc; trang `/search` đặt `noindex, follow`.
- **[G08.47]** Dữ liệu có cấu trúc trang chủ — JSON-LD `Organization` + `WebSite` kèm `SearchAction` trỏ `/search?q=`.
- **[G08.48]** Dữ liệu có cấu trúc trang danh sách — `ItemList` cho danh mục / khám phá / shop; `BreadcrumbList` cho danh mục, sản phẩm, shop, chính sách.
- **[G08.49]** Dữ liệu có cấu trúc sản phẩm — `Product` đủ `name`, `image`, `sku`, `brand`, `offers` (giá VND, tình trạng hàng), `aggregateRating`, vài `review` đầu.
- **[G08.50]** Phân trang SSR — danh mục, khám phá, shop, đánh giá có liên kết `?page=N` thật (R16); "tải thêm khi cuộn" chỉ là lớp nâng cao phía trên.
- **[G08.51]** Ảnh — mọi `<img>` có `alt` mô tả, `width` / `height` (tránh xô lệch bố cục), `loading="lazy"` cho ảnh dưới màn hình đầu, `fetchpriority="high"` cho ảnh chính của trang.
- **[G08.52]** Cấu trúc tiêu đề & vùng — rà R18 trên từng trang công khai: một `<h1>`, `<nav aria-label>` cho breadcrumb, danh sách sản phẩm là `<ul>`.
- **[G08.53]** Mã trạng thái — rà R19: sản phẩm / danh mục / shop / chính sách không tồn tại trả 404; slug sai trả 301.
- **[G08.54]** Kiểm tự động — Vitest cho `seo.ts`; `tests/e2e/seo.spec.ts` chạy **tắt JavaScript**: thấy tên + giá sản phẩm, liên kết danh mục bấm được, `title` / canonical đúng, JSON-LD phân tích được.
- **[G08.C7]** COMMIT — `feat(web): add structured data, canonical urls and seo checks`

### G09 — Xác thực

- **[G09.01]** Miền auth — `features/auth/domain/`: `password.rules.ts` (`passwordIssue`, độ mạnh 0–4: ≥ 8 ký tự, hoa, số, ký tự đặc biệt), hằng `OTP_LENGTH`, `OTP_TTL` 300 s, `RESEND_AFTER` 60 s, `MAX_ATTEMPTS` 5, `LOCK_SECONDS` 900; kèm test.
- **[G09.02]** Lược đồ form — zod cho đăng nhập, đăng ký, quên mật khẩu, đặt lại mật khẩu.
- **[G09.03]** Hạ tầng auth — `auth.dto.ts`, `auth.mock.ts` (mật khẩu `Genie@123`, OTP `123456`, email đã tồn tại), `auth.api.ts` (`login`, `requestOtp`, `verifyOtp`, `register`, `checkEmail`, `forgot`, `reset`, `logout`).
- **[G09.04]** Tầng ứng dụng — `useLogin`, `useRegister`, `useForgot`; cập nhật `$session`, gọi `mergeGuestCart` sau đăng nhập, chuyển về `next`.
- **[G09.05]** `LoginForm` — hai chế độ mật khẩu / OTP, thông báo lỗi, đếm số lần sai.
- **[G09.06]** Bước OTP — `OtpInput`, đếm ngược hết hạn 5 phút, gửi lại sau 60 giây.
- **[G09.07]** Trạng thái khóa — màn khóa 15 phút sau 5 lần sai, đếm ngược.
- **[G09.08]** `RegisterForm` — kiểm trùng email, thanh độ mạnh mật khẩu, đồng ý điều khoản, bước OTP xác minh.
- **[G09.09]** `ForgotForm` — nhập email → OTP → đặt mật khẩu mới → xong.
- **[G09.10]** `SocialButton`, `SsoDialog` — Google / Apple (mô phỏng).
- **[G09.11]** `pages/auth/{login,register,forgot}.astro` — dùng `AuthLayout`, island `client:load`, `noindex`.
- **[G09.12]** `pages/auth/oauth-callback.astro` — trạng thái đang xử lý / lỗi (kiểm `state`) / chuyển hướng.
- **[G09.13]** Hiển thị lỗi xác thực — mọi lỗi từ `auth.api.ts` đi qua `shared/api/errors.ts` (mã `GNE-AUTH-*`, `GNE-RAT-4290`) ra thông điệp i18n ngay tại trường / đầu form; không dựng bảng mô phỏng `GoogleFlow` (D5).
- **[G09.14]** Khóa i18n `auth.*`; `index.ts` xuất `LoginPrompt`, `UserMenu`, hook phiên.
- **[G09.C1]** COMMIT — `feat(web): build login, register and password recovery flows`

### G10 — Trang người mua (cần đăng nhập)

**Nhóm A — Giỏ hàng**

- **[G10.01]** Miền giỏ — `cart.types.ts` (`CartLine`, `GuestCart`), `cart.rules.ts` (`groupByShop`, `lineSubtotal` ← `lineTotal`, `MAX_GUEST_LINES`, khóa dòng theo sản phẩm + phân loại); kèm test.
- **[G10.02]** Hạ tầng giỏ — `cart.dto.ts`, `cart.mock.ts` (`seedCart`), `cart.mapper.ts`, `cart.api.ts`, `guestCart.storage.ts` (`genie.cart.guest`, có mốc thời gian).
- **[G10.03]** Ứng dụng giỏ — `cart.store.ts` (số lượng dùng chung giữa island), `useCart.ts`, `mergeGuestCart.ts` (← `mergeCart`); test gộp giỏ.
- **[G10.04]** `CartPage` — nhóm theo shop, chọn tất cả / theo shop / từng dòng, đổi số lượng, xóa nhiều dòng, trạng thái hết hàng.
- **[G10.05]** `VariantModal` — đổi phân loại ngay trong giỏ.
- **[G10.06]** Tóm tắt giỏ — tạm tính, voucher, Điểm, thanh tổng dính đáy (`tr-root--cart`), `CartSummary.astro`.
- **[G10.07]** Giỏ rỗng & giỏ khách — trạng thái rỗng, nhắc đăng nhập khi khách bấm thanh toán.
- **[G10.08]** `pages/cart.astro` + khóa i18n `cart.*`.
- **[G10.C1]** COMMIT — `feat(web): build multi-shop cart page`

**Nhóm B — Voucher & Điểm**

- **[G10.09]** Miền voucher — `features/promotions/domain/`: kiểu `Voucher` (sàn / shop / freeship), `voucherCheck` (lý do chưa đủ điều kiện), `eligibleBase`, `voucherLines`, `voucherDiscount`.
- **[G10.10]** Tính xếp chồng — `priceCart` (thứ tự Shop → Sàn → Freeship → Điểm, Điểm tối đa 50% tiền hàng còn lại, phân bổ giảm giá về từng dòng), `bestVouchers`; **test đủ các tổ hợp** (đây là logic tiền, TS 14).
- **[G10.11]** Phí vận chuyển hiển thị — `shipFee`, `shipOf` → `features/checkout/domain/shipping.rules.ts`; kèm test.
- **[G10.12]** Hạ tầng voucher — dto / mock (`VOUCHERS`) / mapper / api.
- **[G10.13]** `VoucherPicker` — nhập mã hoặc chọn từ danh sách, voucher chưa đủ điều kiện hiện lý do, gợi ý bộ tốt nhất.
- **[G10.14]** Voucher theo từng shop trong giỏ — nối `VoucherPicker` vào `CartPage`.
- **[G10.15]** Công tắc dùng Điểm Genie — đổi mọi chữ "Xu" thành "Điểm" (R11).
- **[G10.C2]** COMMIT — `feat(web): add stacked voucher and points pricing`

**Nhóm C — Thanh toán**

- **[G10.16]** Miền checkout — kiểu `CheckoutSession`, `PaymentMethod` (VNPay / VietQR / COD ← `PAYMENTS`), `Receipt`; thời hạn giữ chỗ 15 phút.
- **[G10.17]** Hạ tầng checkout — dto / mock / mapper / api `createCheckout` (kèm `Idempotency-Key`), kịch bản lỗi hết hàng `GNE-INV-4090`.
- **[G10.18]** `CheckoutPage` — địa chỉ nhận, các gói hàng theo shop + phí ship từng gói, voucher (`voucherRow`), Điểm, phương thức thanh toán, tóm tắt (`sumRow`, `disc`).
- **[G10.19]** Đếm ngược giữ chỗ 15 phút + xử lý dòng hết hàng (sửa số lượng / bỏ món).
- **[G10.20]** Chọn / đổi địa chỉ tại checkout — dùng `AddressModal` của `account` qua `index.ts`.
- **[G10.21]** Nháp checkout — lưu / khôi phục qua `storage.ts`.
- **[G10.22]** `pages/checkout.astro` + khóa i18n `checkout.*`.
- **[G10.C3]** COMMIT — `feat(web): build consolidated checkout page`

**Nhóm D — Thanh toán & kết quả**

- **[G10.23]** `MoneyRail`, `MoneyChip`, `HoldNote` — thành phần "Genie giữ tiền" (signature trong `DESIGN.md`).
- **[G10.24]** `PayModal` + `PayGateway` — cổng VNPay mô phỏng (thành công / thất bại / hủy).
- **[G10.25]** `VietQrPanel` — mã QR giữ chỗ, nội dung chuyển khoản `TR{paymentId}`, chờ xác nhận.
- **[G10.26]** COD — nhánh thanh toán khi nhận hàng (`CodLogo`).
- **[G10.27]** `OrderDone` — biên nhận theo phiên, nhóm đơn theo shop, nút theo dõi đơn.
- **[G10.28]** `PaymentFailed` — thử lại / về giỏ / về trang chủ.
- **[G10.29]** `pages/checkout/result.astro` — chỉ hiển thị theo `status`, không đổi trạng thái đơn (TS 10.3); khóa i18n `payment.*`.
- **[G10.C4]** COMMIT — `feat(web): add payment flows and checkout result page`

**Nhóm E — Đơn mua & theo dõi**

- **[G10.30]** Miền đơn — `features/orders/domain/`: `Order`, `OrderItem`, `OrderStatus` (đúng enum backend), `STEPS` / `STEP_OF`, nhóm đơn cùng phiên, thao tác hợp lệ theo trạng thái; kèm test.
- **[G10.31]** Hạ tầng đơn — dto / mock (`ORDERS`, sự kiện hành trình) / mapper / api.
- **[G10.32]** `OrdersPanel` — tab trạng thái (`TABS`), tìm đơn, `OrderGroup`, `OrderCard`, `Actions`.
- **[G10.33]** Hủy đơn có lý do — `CANCEL_REASONS`, hộp xác nhận.
- **[G10.34]** Xác nhận đã nhận hàng — hộp "Đã nhận, chuyển tiền cho shop".
- **[G10.35]** `pages/orders/index.astro`.
- **[G10.36]** `OrderTracking` — thanh bước trạng thái, hành trình vận chuyển (`EV_ICON`, `CARRIER_LABEL`), thông tin giao nhận, sản phẩm, tiền (`MoneyRail`).
- **[G10.37]** Mua lại, chat với shop từ đơn.
- **[G10.38]** `pages/orders/[id].astro` + khóa i18n `orders.*`.
- **[G10.C5]** COMMIT — `feat(web): build order list and order tracking pages`

**Nhóm F — Trả hàng & đánh giá**

- **[G10.39]** Miền hoàn tiền — `features/refunds/`: `RefundRequest`, `REFUND_STATE`, hạn 7 ngày; dto / mock / mapper / api.
- **[G10.40]** `ReturnModal` — lý do, chọn món, tải ảnh / video bằng chứng (nén WebP bằng `browser-image-compression`, giới hạn 5 MB / 30 MB), khiếu nại lên Sàn.
- **[G10.41]** Hiển thị trạng thái trả hàng trong trang đơn.
- **[G10.42]** `ReviewModal` — sao, nội dung, ảnh, ẩn tên; đánh dấu đã đánh giá.
- **[G10.43]** Khóa i18n `refunds.*`, `reviews.form.*`.
- **[G10.C6]** COMMIT — `feat(web): add return request and review submission`

**Nhóm G — Tài khoản**

- **[G10.44]** Khung tài khoản — `AccountShell`: menu trái 2 nhóm (`MENU`), thu thành tab ở màn hẹp; `pages/account/[...section].astro` định tuyến theo `section`, 404 khi lạ.
- **[G10.45]** Miền & hạ tầng tài khoản — `User`, `Address`, `Device`, `LinkedLogin`; dto / mock (`USER`, `ADDRESSES`, `DEVICES`) / mapper / api.
- **[G10.46]** `ProfilePanel` — sửa hồ sơ, ảnh đại diện (nén WebP).
- **[G10.47]** `EmailChangeModal` — đổi email có OTP.
- **[G10.48]** `AddressPanel` + `AddressModal` + `AddressForm` — danh sách, thêm / sửa / xóa, đặt mặc định.
- **[G10.49]** `RegionPicker` — tỉnh / quận / phường (`REGIONS`, `PROVINCES`, `DISTRICTS`).
- **[G10.50]** `PasswordPanel` — đổi mật khẩu hoặc thiết lập mật khẩu (tài khoản mạng xã hội chưa có mật khẩu).
- **[G10.51]** `LoginsPanel` — liên kết / hủy liên kết Google, Apple.
- **[G10.52]** `DevicesPanel` — thiết bị đăng nhập, đăng xuất từ xa.
- **[G10.53]** Xóa tài khoản sau 30 ngày — luồng yêu cầu + hủy yêu cầu.
- **[G10.54]** Khóa i18n `account.*`.
- **[G10.C7]** COMMIT — `feat(web): build account profile, address and security sections`

**Nhóm H — Voucher, Điểm, yêu thích, theo dõi, thông báo**

- **[G10.55]** `VouchersPanel` — Kho Voucher: tab, lịch sử, chi tiết voucher.
- **[G10.56]** Miền Điểm — `features/coins/`: số dư, lịch sử (`COIN_LOG`), quà đổi (`XU_REWARDS` → đặt lại tên `POINT_REWARDS`), điểm danh hằng ngày.
- **[G10.57]** `CoinsPanel` — điểm danh (đếm ngược đến nửa đêm), đổi Điểm lấy voucher, lịch sử; route `/account/points`.
- **[G10.58]** `WishlistPanel` — `features/product/`: đọc / ghi yêu thích (`readWish`, `wishlist`), store dùng chung với nút tim.
- **[G10.59]** `FollowsPanel` — `features/shop/`: shop đang theo dõi, bỏ theo dõi.
- **[G10.60]** `NotificationsPanel` — tab loại thông báo (`N_TABS`), đánh dấu đã đọc, mở đúng đích (đơn, voucher…).
- **[G10.61]** Khóa i18n `promotions.wallet.*`, `coins.*`, `notifications.*`.
- **[G10.C8]** COMMIT — `feat(web): add vouchers, points, wishlist and notification sections`

**Nhóm I — Chat & mở shop**

- **[G10.62]** Miền chat — `features/chat/`: `Thread`, `Message` (văn bản, ảnh, thẻ sản phẩm, thẻ đơn hàng), trạng thái gửi; dto / mock (`SEED`, `REPLIES`, `QUICK`) / mapper / api.
- **[G10.63]** `ChatWidget` — nút nổi, danh sách hội thoại, khung tin nhắn (`Bubble`, `ShopMark`), trả lời nhanh, đính kèm thẻ sản phẩm / đơn hàng; `client:only="react"`.
- **[G10.64]** `pages/chat.astro` — bản toàn trang dùng lại cùng component.
- **[G10.65]** `BecomeSeller` — lợi ích (`BENEFITS`), các bước (`STEPS`), form đăng ký shop (react-hook-form + zod), chuyển sang `/seller/kyc`.
- **[G10.66]** `pages/become-seller.astro` + khóa i18n `chat.*`, `seller-onboarding.landing.*`.
- **[G10.C9]** COMMIT — `feat(web): add buyer chat and become-seller landing`

### G11 — Seller Center (SPA `/seller/*`)

**Nhóm A — Khung SPA**

- **[G11.01]** Router — `app/seller/routes.tsx`: bảng route mục 4.3, lazy theo màn, trang 404 nội bộ.
- **[G11.02]** `SellerApp.tsx` — `Providers` + `BrowserRouter basename="/seller"` + layout.
- **[G11.03]** Layout Seller — từ `VendorShell`: thanh bên 11 mục (`NAV`, huy hiệu số đơn chờ / chat chưa đọc), thanh trên, thu gọn ở màn hẹp.
- **[G11.04]** `NotifBell`, `AccountMenu`, `SellerSearch`.
- **[G11.05]** `KycGate` — khóa màn khi shop chưa `VERIFIED` (trạng thái chưa nộp / bị từ chối), nút sang `/seller/kyc`.
- **[G11.06]** `pages/seller/[...path].astro` — `<SellerApp client:only="react" />` trong `AppShellLayout`.
- **[G11.07]** Hạ tầng shop người bán — `SHOP`, `BANK`, `KYC_NAME`, `NOTIFICATIONS` → mock + api dùng chung cho SPA.
- **[G11.08]** Khóa i18n `seller.shell.*`.
- **[G11.C1]** COMMIT — `feat(seller-ui): scaffold seller center shell and routing`

**Nhóm B — Tổng quan & phân tích**

- **[G11.09]** `seller-dashboard` miền + mock — `REVENUE`, `PERF`, `HEALTH`, `ORDER_COUNTS`, `DAILY`, `HOURS_TODAY`.
- **[G11.10]** `OverviewPage` — việc cần làm xếp đầu (`WorkQueue`), `KycBanner`, `Revenue` (chọn khoảng `RANGES`, `TimeChart`), `ShopMoney`.
- **[G11.11]** `seller-analytics` — `StatsPage`: top sản phẩm (`TOP`), nguồn truy cập (`SOURCES`), cảm xúc đánh giá (`SENTIMENT`), xuất CSV (`downloadCsv` → `shared/lib/csv.ts`).
- **[G11.12]** Khóa i18n `seller-dashboard.*`, `seller-analytics.*`.
- **[G11.C2]** COMMIT — `feat(seller-ui): add overview and analytics screens`

**Nhóm C — Đơn hàng & trả hàng**

- **[G11.13]** `seller-orders` miền — trạng thái đơn phía shop, `GHN_STATUS` / `ghnStatus`, `orderBadge`, lý do từ chối `REJECT_REASONS`; kèm test chuyển trạng thái.
- **[G11.14]** Hạ tầng — dto / mock (`ORDERS`, `QUOTES`, `RETURNS`) / mapper / api.
- **[G11.15]** `FulfillmentPage` — tab trạng thái (`TABS`), bảng đơn (`Row`), tìm / lọc.
- **[G11.16]** Xác nhận / từ chối đơn có lý do.
- **[G11.17]** Đóng gói & tạo vận đơn — so sánh hãng vận chuyển (`QUOTES`), in vận đơn.
- **[G11.18]** `ReturnsPage` — yêu cầu trả hàng, phản hồi trong 48 giờ (`R_STATE`), xem bằng chứng.
- **[G11.19]** Khóa i18n `seller-orders.*`.
- **[G11.C3]** COMMIT — `feat(seller-ui): add order fulfillment and returns screens`

**Nhóm D — Sản phẩm**

- **[G11.20]** `seller-products` miền — `SellerProduct`, `VariantMatrix`, thông số bắt buộc theo ngành (`REQUIRED_SPECS`, `SUGGESTED_SPECS`), `requiresDocs`; lược đồ zod cho form đăng bán; kèm test.
- **[G11.21]** Hạ tầng — dto / mock (`PRODUCTS`, `CATEGORY_TREE`, `BRANDS`) / mapper / api.
- **[G11.22]** `ProductsPage` — bảng sản phẩm, lọc trạng thái (đang bán / chờ duyệt / bị từ chối / ẩn), thao tác nhanh.
- **[G11.23]** `ProductEditor` khung — react-hook-form, bố cục các thẻ, lưu nháp / gửi duyệt.
- **[G11.24]** `ImageUpload` — kéo thả, sắp xếp, nén WebP.
- **[G11.25]** `BasicInfo` — tên, `CategoryPicker` (cây danh mục), `BrandCombo`.
- **[G11.26]** `SpecsTable` — thông số theo ngành, bắt buộc / gợi ý.
- **[G11.27]** `RichDescription` — trình soạn mô tả.
- **[G11.28]** `VariantSection` — nhóm phân loại (`GroupNameCombo`, `TagInput`), bảng giá / tồn / SKU theo tổ hợp, kiểm lỗi giá và tồn.
- **[G11.29]** `ShippingInfo` — cân nặng, kích thước.
- **[G11.30]** `LegalDocs` — giấy tờ chứng thực sản phẩm khi ngành yêu cầu.
- **[G11.31]** `VendorCatalogPage` — danh mục toàn sàn, đề xuất danh mục mới (`STATE`).
- **[G11.32]** Khóa i18n `seller-products.*`.
- **[G11.C4]** COMMIT — `feat(seller-ui): add product list, editor and catalog screens`

**Nhóm E — Ví, KYC**

- **[G11.33]** `seller-wallet` miền + hạ tầng — `WALLET`, `LEDGER`, loại giao dịch `TYPE_BADGE`, `COMMISSION_RATE`, `OUTCOMES`.
- **[G11.34]** `WalletPage` — số dư khả dụng / đang giữ (`bal`), luồng tiền Escrow (`Flow`, `Op`), bảng giao dịch có lọc (`T_FILTER`), xuất CSV.
- **[G11.35]** `WithdrawModal` — số tiền, tài khoản nhận, OTP.
- **[G11.36]** `seller-onboarding` miền + hạ tầng KYC — trạng thái `NOT_SUBMITTED` / `PENDING` / `SUPPLEMENT_REQUESTED` / `REJECTED` / `VERIFIED`.
- **[G11.37]** `OnboardingPage` — các bước hồ sơ: CCCD 2 mặt, giấy phép kinh doanh, thông tin thuế, tài khoản nhận tiền (xác minh tên + OTP); ảnh KYC **không nén** (TS 5.4).
- **[G11.38]** `CameraModal` — chụp selfie bằng camera (`getUserMedia`), xử lý từ chối quyền.
- **[G11.39]** Khóa i18n `seller-wallet.*`, `seller-onboarding.kyc.*`.
- **[G11.C5]** COMMIT — `feat(seller-ui): add wallet and kyc onboarding screens`

**Nhóm F — Khuyến mãi, chat, đánh giá, cài đặt**

- **[G11.40]** `seller-promotions` — `PromoPage`: bảng voucher shop (`V_STATE`), `VoucherFormModal` (zod, `genCode`), đăng ký Flash Sale (`/seller/flash-sales`).
- **[G11.41]** `SellerChatPage` — `features/chat/ui/`: danh sách hội thoại (`THREADS`), khung chat, thẻ đơn của khách (`ORDERS_OF`), tin nhắn tự động.
- **[G11.42]** `seller-reviews` — `ReviewsPage`: phân bố sao (`DIST`), `ReviewCard`, phản hồi, báo cáo đánh giá.
- **[G11.43]** `seller-settings` — `SettingsPage`: thông tin shop, địa chỉ lấy hàng, chế độ nghỉ, tin nhắn tự động (`Section`, `ToggleRow`).
- **[G11.44]** `SecuritySection` — đổi mật khẩu, OTP (`PasswordInput`, `OtpInput` dùng chung).
- **[G11.45]** Khóa i18n `seller-promotions.*`, `seller-reviews.*`, `seller-settings.*`.
- **[G11.C6]** COMMIT — `feat(seller-ui): add promotions, chat, reviews and settings screens`

### G12 — Admin Portal (SPA `/admin/*`)

**Nhóm A — Khung SPA & phân quyền**

- **[G12.01]** Router — `app/admin/routes.tsx`: bảng route mục 4.4, mỗi route gắn mã quyền.
- **[G12.02]** `AdminApp.tsx` — `Providers` + `BrowserRouter basename="/admin"` + layout.
- **[G12.03]** Layout Admin — từ `AdminShell`: thanh bên 18 mục **lọc theo quyền** (`usePermissions`), huy hiệu số việc chờ, `AdminBell`.
- **[G12.04]** Chặn route — component `RequirePermission`: không đủ quyền → màn 403 (chỉ ẩn / hiện; server mới là nơi kiểm, TS 5.4).
- **[G12.05]** Huy hiệu số việc chờ — số trên thanh bên lấy từ hook đếm của từng feature (prototype ghi cứng trong `NAV`); người dùng mock mặc định là Admin đủ 13 quyền, không dựng nút xem trước vai trò `PREVIEW_ROLES` (D5).
- **[G12.06]** `pages/admin/[...path].astro` — `<AdminApp client:only="react" />`.
- **[G12.07]** Khóa i18n `admin.shell.*`, tên 13 quyền `admin.perm.*`.
- **[G12.C1]** COMMIT — `feat(admin-ui): scaffold admin portal shell with permission gating`

**Nhóm B — Tổng quan, KYC**

- **[G12.08]** `admin-dashboard` — miền + mock (`DASHBOARD`, `GMV`, `MONTHS`, `WEEKS_JUNE`, `LAST_7_DAYS`); `DashboardPage`: KPI tài chính, biểu đồ theo khoảng (`RANGES`), hàng việc.
- **[G12.09]** `admin-kyc` miền + hạ tầng — hồ sơ `KYC`, mức rủi ro `RISK`, lý do từ chối `REASONS`, giấy tờ bổ sung `SUPP_DOCS`, kết quả OCR.
- **[G12.10]** `KycReviewPage` — hàng đợi, lọc trạng thái, chi tiết hồ sơ, kết quả xác thực tự động.
- **[G12.11]** `DocViewer` — xem tài liệu (phóng to, xoay), che số CCCD.
- **[G12.12]** Quyết định KYC — duyệt / yêu cầu bổ sung / từ chối kèm lý do.
- **[G12.13]** Khóa i18n `admin-dashboard.*`, `admin-kyc.*`.
- **[G12.C2]** COMMIT — `feat(admin-ui): add dashboard and kyc review screens`

**Nhóm C — Tranh chấp & kiểm duyệt**

- **[G12.14]** `admin-disputes` miền + hạ tầng — `REFUNDS`, trạng thái cổng `GW_STATUS`, kết quả phân xử `OUTCOMES`.
- **[G12.15]** `RefundsPage` — hàng đợi, bằng chứng hai bên, `GatewayCard` (hoàn qua VNPay / chuyển khoản tay nội dung `TRRF{id}`), biên bản phân xử.
- **[G12.16]** `admin-moderation` đánh giá — `ReviewModerationPage` (ẩn / giữ / báo cáo).
- **[G12.17]** `admin-moderation` sản phẩm — miền + hạ tầng (`MODERATION`, `MODERATORS`, `STATE`, `REJECT_REASONS`).
- **[G12.18]** `ProductModerationPage` — tab (`TABS`), lưới (`Grid`), chi tiết (`Detail`): phân tích AI, hồ sơ pháp lý, phân công nhân viên, duyệt / từ chối.
- **[G12.19]** Khóa i18n `admin-disputes.*`, `admin-moderation.*`.
- **[G12.C3]** COMMIT — `feat(admin-ui): add dispute and moderation screens`

**Nhóm D — Danh mục**

- **[G12.20]** `admin-catalog` miền + hạ tầng — cây danh mục `TREE`, `LEAVES`, đề xuất chờ `PENDING`, kiểu thuộc tính `ATTR_TYPES`, `CATEGORY_ATTRS`; `slugify` → `shared/lib/slug.ts`.
- **[G12.21]** `CatalogPage` — cây danh mục, KPI, `ProductList` theo danh mục.
- **[G12.22]** `CatModal` — thêm / sửa danh mục (biểu tượng `CAT_ICONS`, slug).
- **[G12.23]** `AttributePanel` — bộ thuộc tính theo ngành.
- **[G12.24]** Duyệt đề xuất danh mục của shop; khóa i18n `admin-catalog.*`.
- **[G12.C4]** COMMIT — `feat(admin-ui): add catalog management screens`

**Nhóm E — Tài chính**

- **[G12.25]** `admin-finance` miền + hạ tầng — `WITHDRAWALS`, `LEDGER`, `COD_SHIPMENTS`, `COD_SESSIONS`, loại bút toán `EVENT`.
- **[G12.26]** `WithdrawalsPage` — hàng đợi rút tiền, duyệt chi kèm QR chuyển khoản (nội dung `TRWD{id}`), từ chối có lý do (`W_REASONS`).
- **[G12.27]** `CommissionPage` — bảng hoa hồng theo danh mục, sửa tỉ lệ.
- **[G12.28]** `LedgerPage` — sổ cái chỉ đọc, lọc theo loại, xuất CSV.
- **[G12.29]** Đối soát COD miền — `parseCsv`, `matchLine`, `RESULT` → `domain/cod.rules.ts`; kèm test.
- **[G12.30]** `CodReconciliationPage` — tải CSV, bảng khớp / lệch, chốt phiên.
- **[G12.31]** Khóa i18n `admin-finance.*`.
- **[G12.C5]** COMMIT — `feat(admin-ui): add payout, commission, ledger and cod screens`

**Nhóm F — Marketing**

- **[G12.32]** `admin-marketing` voucher — miền + hạ tầng (`PLATFORM_VOUCHERS`, `STATUS`, `KIND`, `valueLabel`, `genCode`).
- **[G12.33]** `PlatformVouchersPage` + `PlatformVoucherModal` — tạo voucher theo danh mục, loại trừ, KPI.
- **[G12.34]** `FlashSalePage` — chiến dịch (`FLASH_CAMPAIGNS`), khung giờ, kiểm lỗi form.
- **[G12.35]** Banner miền — `BANNER_POSITIONS`, `LINK_TYPES`, `parseLink` / `linkTo` / `sitePath`, `sameSlot`, `nextOrder` → `domain/banner.rules.ts`; kèm test. Đường dẫn đích đổi sang route mới (mục 4.5).
- **[G12.36]** `BannersPage` — danh sách theo vị trí, form (`LinkPicker`, `ProductPicker`), xem trước.
- **[G12.37]** `PlatformNotificationsPage` — soạn (`RichBox`), đối tượng nhận (`TARGET`), thống kê tiếp cận (`REACH`, `Stat`), lọc.
- **[G12.38]** Khóa i18n `admin-marketing.*`.
- **[G12.C6]** COMMIT — `feat(admin-ui): add voucher, flash sale, banner and notification screens`

**Nhóm G — Hệ thống**

- **[G12.39]** `admin-system` miền + hạ tầng — `STAFF`, `ROLES`, `USERS`, `AUDIT`, `CURRENT_USER`.
- **[G12.40]** `StaffPage` (`Rbac`) — bảng nhân viên, 3 chức danh + Admin.
- **[G12.41]** `InviteStaff`, `RbacEditor` — mời nhân viên, bảng 13 mã quyền; `MANAGE_RBAC` không cấp cho Staff.
- **[G12.42]** `UsersPage` — khách hàng / shop, khóa / mở khóa có lý do và thời hạn (`TERMS`, `REASONS`).
- **[G12.43]** `AuditLogPage` — nhật ký chỉ đọc, lọc theo nhóm (`CATS`), xuất CSV.
- **[G12.44]** `AccountAdminPage` — hồ sơ, đổi mật khẩu, OTP.
- **[G12.45]** Khóa i18n `admin-system.*`.
- **[G12.C7]** COMMIT — `feat(admin-ui): add staff, users, audit log and account screens`

### G13 — Hoàn thiện, kiểm thử, bàn giao

**Nhóm A — Ngôn ngữ & thương hiệu**

- **[G13.01]** Quét chuỗi cứng — tìm chữ tiếng Việt còn nằm trong `.tsx` / `.astro` (ngoài mock và trang chính sách), chuyển hết sang `t()`.
- **[G13.02]** Hoàn tất `en.json` — điền bằng `scripts/i18n-lookup.mjs`, dịch tay phần từ điển prototype không có; `check-i18n` xanh.
- **[G13.03]** Rà từ ngữ R11 — không còn "Xu", "Mall", `LZD-`, `Lazadee`, `lz_` trong mã và khóa lưu trữ.
- **[G13.04]** Rà tiền / ngày — không còn `toLocaleString` hay nối chuỗi "đ" tự viết ngoài `format.ts`.
- **[G13.C1]** COMMIT — `refactor(web): complete i18n coverage and brand wording`

**Nhóm B — Chất lượng giao diện**

- **[G13.05]** So khớp hình storefront — mọi route mục 4.5 ở 360 / 768 / 1440 px; ghi lệch vào progress rồi sửa.
- **[G13.06]** So khớp hình Seller + Admin — mọi route mục 4.3, 4.4 ở 1024 / 1440 px và 360 px.
- **[G13.07]** Trợ năng — bàn phím toàn bộ luồng mua, focus nhìn thấy, tương phản theo `DESIGN.md`, `aria-label` cho nút chỉ có icon, `prefers-reduced-motion`.
- **[G13.08]** Rà chỉ thị hydrate — từng island đúng bảng TS 5.1; chuyển `client:load` thừa sang `idle` / `visible`.
- **[G13.09]** Rà SEO lần cuối — chạy lại `seo.spec.ts`; `noindex` ở mọi trang cần đăng nhập, `/auth/*`, `/seller/*`, `/admin/*`, `/dev/*`; đo Lighthouse (SEO, hiệu năng, trợ năng) cho trang chủ, danh mục, sản phẩm và ghi điểm vào progress.
- **[G13.10]** Rà kiến trúc — `npm run lint` không còn ngoại lệ `eslint-disable` cho luật boundaries; không `any`.
- **[G13.C2]** COMMIT — `fix(web): resolve visual parity, accessibility and hydration issues`

**Nhóm C — Kiểm thử**

- **[G13.11]** Bổ sung Vitest — mỗi feature có `domain` phải có test (rà theo danh sách feature).
- **[G13.12]** E2E khách vãng lai — xem trang chủ → tìm kiếm → sản phẩm → thêm giỏ → bị nhắc đăng nhập.
- **[G13.13]** E2E mua hàng — đăng nhập → giỏ nhiều shop → voucher 3 tầng + Điểm → checkout → VNPay mô phỏng → kết quả (`checkout-vnpay.spec.ts`).
- **[G13.14]** E2E đơn hàng — theo dõi đơn → yêu cầu trả hàng → đánh giá.
- **[G13.15]** E2E người bán — KYC (`kyc-onboarding.spec.ts`) → đăng sản phẩm → xử lý đơn.
- **[G13.16]** E2E quản trị — nhân viên đúng quyền chỉ thấy đúng mục; duyệt KYC; duyệt rút tiền.
- **[G13.C3]** COMMIT — `test(web): add unit and e2e coverage for migrated screens`

**Nhóm D — Dọn dẹp & bàn giao**

- **[G13.17]** Xóa `.gitkeep` ở thư mục đã có file; xóa stub không dùng.
- **[G13.18]** Tài liệu — cập nhật `README.md` (chạy `web` ở chế độ mock, biến môi trường, thư viện `/dev/ui`).
- **[G13.19]** ADR — ghi quyết định D1–D5 và danh sách thư mục bổ sung (mục 3) vào `docs/adr/` (TS mục 17).
- **[G13.20]** Danh sách việc nối backend — liệt kê mọi `*.api.ts` đang trả mock kèm endpoint `/api/v1` dự kiến, để giai đoạn sau thay thân hàm.
- **[G13.21]** CI — kiểm `ci.yml` chạy đủ bước web (lint, check, Vitest, build, check-i18n) trên PR.
- **[G13.22]** Mở PR vào `develop` — tiêu đề đúng định dạng commit, điền mẫu PR (ảnh trước / sau, checklist i18n, test).
- **[G13.C4]** COMMIT — `docs(web): document mock mode, ui gallery and backend handoff`

---

## 8. Ngoài phạm vi

- Toàn bộ phần chỉ để trình diễn của prototype (D5 đã chốt): `index.html` gốc, `shell.jsx` kèm **bảng nổi hình đèn thần ở mép màn hình dùng để chuyển qua lại giữa các bề mặt và các màn**, `board.jsx`, `design-canvas.jsx`, `tweaks-panel.jsx` (đổi màu nhấn / bo góc trực tiếp), bảng hướng dẫn 7 hành trình PRD, `DemoBox`, `AcctDemo`, `GoogleFlow`, `PREVIEW_ROLES`.
- Nối API thật, WebSocket chat thật, tải tệp lên MinIO thật, OAuth thật — plan này dựng giao diện trên lớp mock; chữ ký `*.api.ts` đã sẵn để nối sau (G13.20).
- Backend `server-spring`, `server-python`, `deploy/` — sẽ có plan riêng trong thư mục thể loại tương ứng.
- Nâng phiên bản lớn của phụ thuộc (các nhánh Dependabot đang mở cho Astro, Tailwind 4…): không gộp trong lúc chuyển giao diện để tránh đổi nền giữa chừng.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Prototype dùng inline style dày đặc, chuyển tay dễ lệch | So hình ở mục 6 sau mỗi nhóm; giữ nguyên tên biến token để tra ngược |
| Từ điển EN của prototype lấy chuỗi tiếng Việt làm khóa, không khớp quy ước khóa mới | Đặt khóa theo quy ước ngay khi viết màn (không để cuối), dùng `i18n-lookup` tra bản dịch |
| Các hàm tính tiền (`priceCart`, `bestVouchers`) chuyển sai | Viết test trước khi nối UI (G10.10); so kết quả với prototype trên cùng giỏ mẫu |
| `Account.jsx` (1.177 dòng), `VendorPages.jsx` (817), `Products.jsx` (705), `Finance.jsx` (514) trộn nhiều miền trong một file | Tách theo bảng mục 4 trước khi viết; mỗi panel một file, một feature |
| Lớp `tr-*` ghép động bị Tailwind loại | G07.03 kiểm, thêm `safelist` khi cần |
| PR quá lớn so với ngưỡng ~400 dòng | Mỗi nhóm có commit riêng → tách PR theo nhóm |
| Hook Husky chạy `lint-staged` chậm / lỗi trên Windows | G00.06 thử sớm; không dùng `--no-verify`, sửa cấu hình nếu lỗi |
