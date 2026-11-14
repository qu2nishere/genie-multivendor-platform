# PROGRESS — Chuyển toàn bộ giao diện prototype sang `web/`

| | |
|---|---|
| **Plan** | [plan_chuyen_giao_dien.md](./plan_chuyen_giao_dien.md) — chi tiết từng task tra theo mã `Gxx.yy` |
| **Ngày lập** | 2026-11-11 |
| **Tổng** | 379 task + 50 commit, 14 giai đoạn |
| **Cập nhật gần nhất** | 2026-11-14 — xong G13.01–G13.21 trên `feature/web-polish` (xếp chồng từ `feature/admin-ui`, chưa push); G13.22 (mở PR) chờ chủ dự án xác nhận |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Khuyến nghị | Đã chốt |
|---|---|---|---|
| D1 | Số theme storefront | **Đã chốt 2026-11-11:** làm đủ 9 theme, `genie` trước | ☑ |
| D2 | Chiến lược CSS | Lai: utility Tailwind + giữ CSS `tr-*` / theme — chạy theo khuyến nghị (G00.02) | ☑ |
| D3 | Nguồn dữ liệu | **Đã chốt 2026-11-11:** mock trước để có giao diện | ☑ |
| D4 | Icon | Port bảng `ICONS` của prototype — chạy theo khuyến nghị (G00.02) | ☑ |
| D5 | Phần chỉ để trình diễn | **Đã chốt 2026-11-11:** không làm (kể cả bảng nổi đèn thần chuyển màn) | ☑ |
| D6 | Mã Jira | Chưa có quyền tạo Jira → commit **không kèm** `[#GNE-…]` (TS 13.3 cho phép), nhánh bỏ phần số | ☑ |
| D7 | Commit thư mục `plan/` | Có — commit `docs:` ở G00.C1 | ☑ |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| G00 | Chuẩn bị repo, nhánh, môi trường | 10 | 1 | 11 / 11 | — | `feature/web-foundation` |
| G01 | Công cụ & cấu hình `web/` | 11 | 1 | 12 / 12 | — | `feature/web-foundation` |
| G02 | Token thiết kế, font, theme Tailwind, CSS nền | 15 | 2 | 17 / 17 | — | `feature/web-foundation` |
| G03 | Tài sản tĩnh & thương hiệu | 10 | 1 | 11 / 11 | — | `feature/web-foundation` |
| G04 | `shared/lib`, hook dùng chung, nền i18n | 14 | 1 | 15 / 15 | — | `feature/web-foundation` |
| G05 | `shared/api`, phiên đăng nhập, lớp mock | 10 | 1 | 11 / 11 | — | `feature/web-foundation` |
| G06 | `shared/ui` (design system) | 37 | 5 | 42 / 42 | — | `feature/web-foundation` |
| G07 | Layout & khung trang | 26 | 4 | 30 / 30 | — | `feature/web-storefront-public` |
| G08 | Storefront công khai (SSR + islands) | 54 | 7 | 61 / 61 | — | `feature/web-storefront-public` |
| G09 | Xác thực | 14 | 1 | 15 / 15 | — | `feature/web-auth` |
| G10 | Trang người mua (cần đăng nhập) | 66 | 9 | 75 / 75 | — | `feature/web-buyer` |
| G11 | Seller Center (SPA `/seller/*`) | 45 | 6 | 51 / 51 | — | `feature/seller-ui` |
| G12 | Admin Portal (SPA `/admin/*`) | 45 | 7 | 52 / 52 | — | `feature/admin-ui` |
| G13 | Hoàn thiện, kiểm thử, bàn giao | 22 | 4 | 25 / 26 | — | `feature/web-polish` |
| **Tổng** | | **379** | **50** | **428 / 429** | | |

---

## G00 — Chuẩn bị repo, nhánh, môi trường

- [x] **G00.01** Kiểm danh tính git
- [x] **G00.02** Chốt 7 quyết định D1–D7
- [-] **G00.03** Tạo mã Jira
- [x] **G00.04** Tạo nhánh `develop`
- [x] **G00.05** Tạo nhánh làm việc
- [x] **G00.06** Cài công cụ gốc
- [x] **G00.07** Cài phụ thuộc web
- [x] **G00.08** Đo baseline
- [x] **G00.09** Dựng bản đối chiếu
- [x] **G00.10** Chụp ảnh tham chiếu
- [x] **G00.C1** 🔖 COMMIT `docs: add web ui migration plan and progress tracker` · hash: `46557a9`

## G01 — Công cụ & cấu hình `web/`

- [x] **G01.01** Prettier
- [x] **G01.02** Vitest
- [x] **G01.03** Playwright
- [x] **G01.04** Biến môi trường
- [x] **G01.05** Kiểu môi trường
- [x] **G01.06** ESLint boundaries
- [x] **G01.07** ESLint import sâu
- [x] **G01.08** ESLint cho test & cấu hình
- [x] **G01.09** Script `verify`
- [x] **G01.10** `check-i18n.mjs`
- [x] **G01.11** Sửa lỗi baseline
- [x] **G01.C1** 🔖 COMMIT `build(web): add prettier, vitest, playwright config and env example` · hash: `d787910`

## G02 — Token thiết kế, font, theme Tailwind, CSS nền


**Nhóm A — Token**

- [x] **G02.01** Font tệp
- [x] **G02.02** Token font
- [x] **G02.03** Token màu
- [x] **G02.04** Token chữ
- [x] **G02.05** Token khoảng cách
- [x] **G02.06** Token hiệu ứng
- [x] **G02.07** CSS nền
- [x] **G02.08** Sửa `global.css`
- [x] **G02.C1** 🔖 COMMIT `feat(web): port design tokens and base styles from prototype` · hash: `674cb5c`

**Nhóm B — Tailwind**

- [x] **G02.09** Tailwind màu
- [x] **G02.10** Tailwind hình khối
- [x] **G02.11** Tailwind chữ
- [x] **G02.12** Tailwind bố cục
- [x] **G02.13** Responsive
- [x] **G02.14** Kiểm kê lớp `lz-*`
- [x] **G02.15** Trang thư viện nội bộ
- [x] **G02.C2** 🔖 COMMIT `feat(web): map prototype tokens into tailwind theme` · hash: `fb6c735`

## G03 — Tài sản tĩnh & thương hiệu

- [x] **G03.01** Ảnh sản phẩm mẫu
- [x] **G03.02** Banner
- [x] **G03.03** Logo thanh toán
- [x] **G03.04** SVG thương hiệu
- [x] **G03.05** Favicon
- [x] **G03.06** `GenieMark` / `GenieLogo`
- [x] **G03.07** `GenieCoin`
- [x] **G03.08** Font logo
- [x] **G03.09** Ảnh nền theme
- [x] **G03.10** Hằng đường dẫn ảnh
- [x] **G03.C1** 🔖 COMMIT `feat(web): add brand assets and mock catalog images` · hash: `29e40e3`

## G04 — `shared/lib`, hook dùng chung, nền i18n

- [x] **G04.01** `cn()`
- [x] **G04.02** Định dạng tiền
- [x] **G04.03** Định dạng ngày & số
- [x] **G04.04** Che dữ liệu
- [x] **G04.05** Test định dạng
- [x] **G04.06** Lưu trình duyệt
- [x] **G04.07** Hàm `t()`
- [x] **G04.08** Hook `useT()`
- [x] **G04.09** Test i18n
- [x] **G04.10** Quy ước không gian khóa
- [x] **G04.11** Công cụ tra bản dịch
- [x] **G04.12** SEO
- [x] **G04.13** Hook dùng chung
- [x] **G04.14** Cập nhật boundaries
- [x] **G04.C1** 🔖 COMMIT `feat(web): add i18n, formatting, storage and shared hooks` · hash: `6510c2b`

## G05 — `shared/api`, phiên đăng nhập, lớp mock

- [x] **G05.01** Kiểu phong bì API
- [x] **G05.02** Lỗi
- [x] **G05.03** `http.ts`
- [x] **G05.04** `server-http.ts`
- [x] **G05.05** `idempotency.ts`
- [x] **G05.06** Lớp mock
- [x] **G05.07** Phiên
- [x] **G05.08** Quyền
- [x] **G05.09** Providers
- [x] **G05.10** Test
- [x] **G05.C1** 🔖 COMMIT `feat(web): add api client, session, permissions and mock layer` · hash: `b44e30c`

## G06 — `shared/ui` (design system)


**Nhóm A — Phần tử cơ bản**

- [x] **G06.01** `Icon`
- [x] **G06.02** `Button`
- [x] **G06.03** `IconButton`
- [x] **G06.04** `Input`
- [x] **G06.05** `Textarea`
- [x] **G06.06** `PasswordInput`
- [x] **G06.07** `Checkbox`, `Radio`, `Switch`
- [x] **G06.08** `Select`
- [x] **G06.09** `Tabs`, `Segmented`
- [x] **G06.10** `Badge`, `StatusBadge`, `Tag`
- [x] **G06.11** `Avatar`
- [x] **G06.C1** 🔖 COMMIT `feat(web): add base form and display components to shared ui` · hash: `cbc0ee1`

**Nhóm B — Lớp phủ & phản hồi**

- [x] **G06.12** `Modal`
- [x] **G06.13** `Popover` / `Dropdown`
- [x] **G06.14** `Toast`
- [x] **G06.15** `EmptyState`, `Skeleton`, `Spinner`
- [x] **G06.16** `OtpInput`
- [x] **G06.17** `Countdown`
- [x] **G06.18** `Pagination`
- [x] **G06.19** `Card`, `PanelHead`, `SectionHead`
- [x] **G06.C2** 🔖 COMMIT `feat(web): add overlay and feedback components to shared ui` · hash: `225e430`

**Nhóm C — Thành phần thương mại**

- [x] **G06.20** `Price`
- [x] **G06.21** `Rating`
- [x] **G06.22** `QuantityStepper`
- [x] **G06.23** `ProductCard`
- [x] **G06.24** `VendorChip`, `Seal`
- [x] **G06.25** `VoucherTag`, `Ticket`
- [x] **G06.26** `QrPlaceholder`
- [x] **G06.27** `DataTable`
- [x] **G06.28** `Field`, `FormSection`, `DateInput`
- [x] **G06.C3** 🔖 COMMIT `feat(web): add commerce and data components to shared ui` · hash: `a703360`

**Nhóm D — Bộ thống kê (`dash.jsx` + `dash.css`)**

- [x] **G06.29** `Delta`, `StatTile`, `Kpi`
- [x] **G06.30** `WorkQueue`, `Pipeline`
- [x] **G06.31** `BarList`, `StackBar`, `SegBar`
- [x] **G06.32** `TimeChart`
- [x] **G06.33** `dash.css`
- [x] **G06.34** Test hàm thuần
- [x] **G06.C4** 🔖 COMMIT `feat(web): add dashboard chart components to shared ui` · hash: `4b76a89`

**Nhóm E — Rà soát**

- [x] **G06.35** Xuất tập trung
- [x] **G06.36** Thư viện nội bộ
- [x] **G06.37** Xóa stub cũ
- [x] **G06.C5** 🔖 COMMIT `feat(web): add component gallery and shared ui barrel` · hash: `f6095a3`

## G07 — Layout & khung trang


**Nhóm A — CSS storefront**

- [x] **G07.01** Tách `storefront.css`
- [x] **G07.02** Chuẩn hóa giá trị
- [x] **G07.03** Nhập vào `global.css`
- [x] **G07.C1** 🔖 COMMIT `feat(web): port storefront stylesheet into layered css modules` · hash: `4bcae11`

**Nhóm B — Layout**

- [x] **G07.04** `SeoHead.astro` + `<head>` của `StorefrontLayout.astro`
- [x] **G07.05** `ThemeBoot.astro`
- [x] **G07.06** Khung thân
- [x] **G07.07** `ToastHost.tsx`
- [x] **G07.08** `AuthLayout.astro`
- [x] **G07.09** `AppShellLayout.astro`
- [x] **G07.10** `middleware.ts`
- [x] **G07.11** `404.astro`
- [x] **G07.C2** 🔖 COMMIT `feat(web): build storefront, auth and app shell layouts` · hash: `7cfb914`

**Nhóm C — Header & Footer**

- [x] **G07.12** Thanh tiện ích trên cùng
- [x] **G07.13** Thanh chính
- [x] **G07.14** `SearchBox`
- [x] **G07.15** `MiniCart`
- [x] **G07.16** `UserMenu`
- [x] **G07.17** `NotifPanel`
- [x] **G07.18** Menu danh mục & điều hướng màn hẹp
- [x] **G07.19** `Footer.astro`
- [x] **G07.20** `LoginPrompt`
- [x] **G07.21** Khóa i18n khung trang
- [x] **G07.C3** 🔖 COMMIT `feat(web): add storefront header, footer and global overlays` · hash: `3b73c77`

**Nhóm D — Đủ 9 theme (D1 đã chốt)**

- [x] **G07.22** `themes.css` phần theme `genie` + `classic`
- [x] **G07.23** 7 theme còn lại
- [x] **G07.24** Font theme
- [x] **G07.25** `ThemePicker`
- [x] **G07.26** Kiểm 9 theme
- [x] **G07.C4** 🔖 COMMIT `feat(web): port storefront themes and theme picker` · hash: `3dda4a9`

## G08 — Storefront công khai (SSR + islands)


**Nhóm A — Miền danh mục & sản phẩm**

- [x] **G08.01** Kiểu domain sản phẩm
- [x] **G08.02** Quy tắc sản phẩm
- [x] **G08.03** Hạ tầng sản phẩm
- [x] **G08.04** Kiểu & hạ tầng danh mục
- [x] **G08.05** Kiểu & hạ tầng shop
- [x] **G08.06** API công khai feature
- [x] **G08.C1** 🔖 COMMIT `feat(web): add catalog, product and shop domain with mock data` · hash: `f630a30`

**Nhóm B — Trang chủ**

- [x] **G08.07** Miền banner
- [x] **G08.08** `BannerCarousel`, `BannerSlot`, `BannerPair`, `BannerPicture`
- [x] **G08.09** `BannerPopup`
- [-] **G08.10** `CategoryTiles.astro`
- [x] **G08.11** `FlashBand`
- [x] **G08.12** `SaveVoucher`
- [x] **G08.13** `ProductFeed`
- [x] **G08.14** Dải cam kết (Promise Strip trong `DESIGN.md`)
- [x] **G08.15** `pages/index.astro`
- [x] **G08.16** Khóa i18n `home.*`
- [x] **G08.C2** 🔖 COMMIT `feat(web): build storefront home page` · hash: `7a87a36`

**Nhóm C — Danh mục, tìm kiếm, khám phá**

- [x] **G08.17** Quy tắc lọc & sắp xếp
- [x] **G08.18** `FilterPanel`
- [x] **G08.19** `SortBar`, `ResultGrid`
- [x] **G08.20** `pages/c/[slug].astro`
- [x] **G08.21** `pages/search.astro`
- [x] **G08.22** `pages/discover.astro`
- [x] **G08.23** Khóa i18n `catalog.*`, `search.*`, `discover.*`
- [x] **G08.C3** 🔖 COMMIT `feat(web): build category, search and discover pages` · hash: `5058a10`

**Nhóm D — Chi tiết sản phẩm**

- [x] **G08.24** Thư viện ảnh
- [x] **G08.25** Khối mua
- [x] **G08.26** `AddToCartButton` + Mua ngay
- [x] **G08.27** Nút yêu thích & chia sẻ
- [x] **G08.28** Khối shop
- [x] **G08.29** `SpecsTable.astro`, `ProductDescription.astro`
- [x] **G08.30** Thanh mua dính đáy ở màn hẹp (`tr-root--bar`)
- [x] **G08.31** `ProductGone`
- [x] **G08.32** Miền đánh giá
- [x] **G08.33** `ReviewList`
- [x] **G08.34** Sản phẩm tương tự
- [x] **G08.35** `pages/p/[slug]-[id].astro`
- [x] **G08.36** Khóa i18n `product.*`, `reviews.*`
- [x] **G08.C4** 🔖 COMMIT `feat(web): build product detail page with reviews` · hash: `3f136be`

**Nhóm E — Shop, Flash Sale, chiến dịch**

- [x] **G08.37** `pages/shop/[id].astro`
- [x] **G08.38** `pages/flash-sale.astro`
- [x] **G08.39** `pages/campaign/[slug].astro`
- [x] **G08.40** Khóa i18n `shop.*`, `promotions.flash.*`, `promotions.campaign.*`
- [x] **G08.C5** 🔖 COMMIT `feat(web): build shop, flash sale and campaign pages` · hash: `65aa679`

**Nhóm F — Trang chính sách & sitemap**

- [x] **G08.41** Dữ liệu chính sách
- [x] **G08.42** `PolicyPage.astro`
- [x] **G08.43** `pages/policies/[slug].astro`
- [x] **G08.44** `pages/sitemap.xml.ts`
- [x] **G08.45** `public/robots.txt`
- [x] **G08.C6** 🔖 COMMIT `feat(web): add policy pages and sitemap` · hash: `254fcc6`

**Nhóm G — SEO kỹ thuật (rà toàn bộ trang công khai vừa dựng)**

- [x] **G08.46** URL chuẩn
- [x] **G08.47** Dữ liệu có cấu trúc trang chủ
- [x] **G08.48** Dữ liệu có cấu trúc trang danh sách
- [x] **G08.49** Dữ liệu có cấu trúc sản phẩm
- [x] **G08.50** Phân trang SSR
- [x] **G08.51** Ảnh
- [x] **G08.52** Cấu trúc tiêu đề & vùng
- [x] **G08.53** Mã trạng thái
- [x] **G08.54** Kiểm tự động
- [x] **G08.C7** 🔖 COMMIT `feat(web): add structured data, canonical urls and seo checks` · hash: `aac0d8a`

## G09 — Xác thực

- [x] **G09.01** Miền auth
- [x] **G09.02** Lược đồ form
- [x] **G09.03** Hạ tầng auth
- [x] **G09.04** Tầng ứng dụng
- [x] **G09.05** `LoginForm`
- [x] **G09.06** Bước OTP
- [x] **G09.07** Trạng thái khóa
- [x] **G09.08** `RegisterForm`
- [x] **G09.09** `ForgotForm`
- [x] **G09.10** `SocialButton`, `SsoDialog`
- [x] **G09.11** `pages/auth/{login,register,forgot}.astro`
- [x] **G09.12** `pages/auth/oauth-callback.astro`
- [x] **G09.13** Hiển thị lỗi xác thực
- [x] **G09.14** Khóa i18n `auth.*`; `index.ts` xuất `LoginPrompt`, `UserMenu`, hook phiên
- [x] **G09.C1** 🔖 COMMIT `feat(web): build login, register and password recovery flows` · hash: `49bbf6f`

## G10 — Trang người mua (cần đăng nhập)


**Nhóm A — Giỏ hàng**

- [x] **G10.01** Miền giỏ
- [x] **G10.02** Hạ tầng giỏ
- [x] **G10.03** Ứng dụng giỏ
- [x] **G10.04** `CartPage`
- [x] **G10.05** `VariantModal`
- [x] **G10.06** Tóm tắt giỏ
- [x] **G10.07** Giỏ rỗng & giỏ khách
- [x] **G10.08** `pages/cart.astro` + khóa i18n `cart.*`
- [x] **G10.C1** 🔖 COMMIT `feat(web): build multi-shop cart page` · hash: `8a9c33e`

**Nhóm B — Voucher & Điểm**

- [x] **G10.09** Miền voucher
- [x] **G10.10** Tính xếp chồng
- [x] **G10.11** Phí vận chuyển hiển thị
- [x] **G10.12** Hạ tầng voucher
- [x] **G10.13** `VoucherPicker`
- [x] **G10.14** Voucher theo từng shop trong giỏ
- [x] **G10.15** Công tắc dùng Điểm Genie
- [x] **G10.C2** 🔖 COMMIT `feat(web): add stacked voucher and points pricing` · hash: `37f2d0b`

**Nhóm C — Thanh toán**

- [x] **G10.16** Miền checkout
- [x] **G10.17** Hạ tầng checkout
- [x] **G10.18** `CheckoutPage`
- [x] **G10.19** Đếm ngược giữ chỗ 15 phút + xử lý dòng hết hàng (sửa số lượng / bỏ món)
- [x] **G10.20** Chọn / đổi địa chỉ tại checkout
- [x] **G10.21** Nháp checkout
- [x] **G10.22** `pages/checkout.astro` + khóa i18n `checkout.*`
- [x] **G10.C3** 🔖 COMMIT `feat(web): build consolidated checkout page` · hash: `3bd8edb`

**Nhóm D — Thanh toán & kết quả**

- [x] **G10.23** `MoneyRail`, `MoneyChip`, `HoldNote`
- [x] **G10.24** `PayModal` + `PayGateway`
- [x] **G10.25** `VietQrPanel`
- [x] **G10.26** COD
- [x] **G10.27** `OrderDone`
- [x] **G10.28** `PaymentFailed`
- [x] **G10.29** `pages/checkout/result.astro`
- [x] **G10.C4** 🔖 COMMIT `feat(web): add payment flows and checkout result page` · hash: `ad830ca`

**Nhóm E — Đơn mua & theo dõi**

- [x] **G10.30** Miền đơn
- [x] **G10.31** Hạ tầng đơn
- [x] **G10.32** `OrdersPanel`
- [x] **G10.33** Hủy đơn có lý do
- [x] **G10.34** Xác nhận đã nhận hàng
- [x] **G10.35** `pages/orders/index.astro`
- [x] **G10.36** `OrderTracking`
- [x] **G10.37** Mua lại, chat với shop từ đơn
- [x] **G10.38** `pages/orders/[id].astro` + khóa i18n `orders.*`
- [x] **G10.C5** 🔖 COMMIT `feat(web): build order list and order tracking pages` · hash: `913f4d8`

**Nhóm F — Trả hàng & đánh giá**

- [x] **G10.39** Miền hoàn tiền
- [x] **G10.40** `ReturnModal`
- [x] **G10.41** Hiển thị trạng thái trả hàng trong trang đơn
- [x] **G10.42** `ReviewModal`
- [x] **G10.43** Khóa i18n `refunds.*`, `reviews.form.*`
- [x] **G10.C6** 🔖 COMMIT `feat(web): add return request and review submission` · hash: `7c90b73`

**Nhóm G — Tài khoản**

- [x] **G10.44** Khung tài khoản
- [x] **G10.45** Miền & hạ tầng tài khoản
- [x] **G10.46** `ProfilePanel`
- [x] **G10.47** `EmailChangeModal`
- [x] **G10.48** `AddressPanel` + `AddressModal` + `AddressForm`
- [x] **G10.49** `RegionPicker`
- [x] **G10.50** `PasswordPanel`
- [x] **G10.51** `LoginsPanel`
- [x] **G10.52** `DevicesPanel`
- [x] **G10.53** Xóa tài khoản sau 30 ngày
- [x] **G10.54** Khóa i18n `account.*`
- [x] **G10.C7** 🔖 COMMIT `feat(web): build account profile, address and security sections` · hash: `a6334cd`

**Nhóm H — Voucher, Điểm, yêu thích, theo dõi, thông báo**

- [x] **G10.55** `VouchersPanel`
- [x] **G10.56** Miền Điểm
- [x] **G10.57** `CoinsPanel`
- [x] **G10.58** `WishlistPanel`
- [x] **G10.59** `FollowsPanel`
- [x] **G10.60** `NotificationsPanel`
- [x] **G10.61** Khóa i18n `promotions.wallet.*`, `coins.*`, `notifications.*`
- [x] **G10.C8** 🔖 COMMIT `feat(web): add vouchers, points, wishlist and notification sections` · hash: `488581b`

**Nhóm I — Chat & mở shop**

- [x] **G10.62** Miền chat
- [x] **G10.63** `ChatWidget`
- [x] **G10.64** `pages/chat.astro`
- [x] **G10.65** `BecomeSeller`
- [x] **G10.66** `pages/become-seller.astro` + khóa i18n `chat.*`, `seller-onboarding.landing.*`
- [x] **G10.C9** 🔖 COMMIT `feat(web): add buyer chat and become-seller landing` · hash: `95fc810`

## G11 — Seller Center (SPA `/seller/*`)


**Nhóm A — Khung SPA**

- [x] **G11.01** Router
- [x] **G11.02** `SellerApp.tsx`
- [x] **G11.03** Layout Seller
- [x] **G11.04** `NotifBell`, `AccountMenu`, `SellerSearch`
- [x] **G11.05** `KycGate`
- [x] **G11.06** `pages/seller/[...path].astro`
- [x] **G11.07** Hạ tầng shop người bán
- [x] **G11.08** Khóa i18n `seller.shell.*`
- [x] **G11.C1** 🔖 COMMIT `feat(seller-ui): scaffold seller center shell and routing` · hash: `e1de77b`

**Nhóm B — Tổng quan & phân tích**

- [x] **G11.09** `seller-dashboard` miền + mock
- [x] **G11.10** `OverviewPage`
- [x] **G11.11** `seller-analytics`
- [x] **G11.12** Khóa i18n `seller-dashboard.*`, `seller-analytics.*`
- [x] **G11.C2** 🔖 COMMIT `feat(seller-ui): add overview and analytics screens` · hash: `428eaa8`

**Nhóm C — Đơn hàng & trả hàng**

- [x] **G11.13** `seller-orders` miền
- [x] **G11.14** Hạ tầng
- [x] **G11.15** `FulfillmentPage`
- [x] **G11.16** Xác nhận / từ chối đơn có lý do
- [x] **G11.17** Đóng gói & tạo vận đơn
- [x] **G11.18** `ReturnsPage`
- [x] **G11.19** Khóa i18n `seller-orders.*`
- [x] **G11.C3** 🔖 COMMIT `feat(seller-ui): add order fulfillment and returns screens` · hash: `93021af`

**Nhóm D — Sản phẩm**

- [x] **G11.20** `seller-products` miền
- [x] **G11.21** Hạ tầng
- [x] **G11.22** `ProductsPage`
- [x] **G11.23** `ProductEditor` khung
- [x] **G11.24** `ImageUpload`
- [x] **G11.25** `BasicInfo`
- [x] **G11.26** `SpecsTable`
- [x] **G11.27** `RichDescription`
- [x] **G11.28** `VariantSection`
- [x] **G11.29** `ShippingInfo`
- [x] **G11.30** `LegalDocs`
- [x] **G11.31** `VendorCatalogPage`
- [x] **G11.32** Khóa i18n `seller-products.*`
- [x] **G11.C4** 🔖 COMMIT `feat(seller-ui): add product list, editor and catalog screens` · hash: `2a33c6b`

**Nhóm E — Ví, KYC**

- [x] **G11.33** `seller-wallet` miền + hạ tầng
- [x] **G11.34** `WalletPage`
- [x] **G11.35** `WithdrawModal`
- [x] **G11.36** `seller-onboarding` miền + hạ tầng KYC
- [x] **G11.37** `OnboardingPage`
- [x] **G11.38** `CameraModal`
- [x] **G11.39** Khóa i18n `seller-wallet.*`, `seller-onboarding.kyc.*`
- [x] **G11.C5** 🔖 COMMIT `feat(seller-ui): add wallet and kyc onboarding screens` · hash: `44c8537`

**Nhóm F — Khuyến mãi, chat, đánh giá, cài đặt**

- [x] **G11.40** `seller-promotions`
- [x] **G11.41** `SellerChatPage`
- [x] **G11.42** `seller-reviews`
- [x] **G11.43** `seller-settings`
- [x] **G11.44** `SecuritySection`
- [x] **G11.45** Khóa i18n `seller-promotions.*`, `seller-reviews.*`, `seller-settings.*`
- [x] **G11.C6** 🔖 COMMIT `feat(seller-ui): add promotions, chat, reviews and settings screens` · hash: `543719a`

## G12 — Admin Portal (SPA `/admin/*`)


**Nhóm A — Khung SPA & phân quyền**

- [x] **G12.01** Router
- [x] **G12.02** `AdminApp.tsx`
- [x] **G12.03** Layout Admin
- [x] **G12.04** Chặn route
- [x] **G12.05** Huy hiệu số việc chờ
- [x] **G12.06** `pages/admin/[...path].astro`
- [x] **G12.07** Khóa i18n `admin.shell.*`, tên 13 quyền `admin.perm.*`
- [x] **G12.C1** 🔖 COMMIT `feat(admin-ui): scaffold admin portal shell with permission gating` · hash: `68cc825`

**Nhóm B — Tổng quan, KYC**

- [x] **G12.08** `admin-dashboard`
- [x] **G12.09** `admin-kyc` miền + hạ tầng
- [x] **G12.10** `KycReviewPage`
- [x] **G12.11** `DocViewer`
- [x] **G12.12** Quyết định KYC
- [x] **G12.13** Khóa i18n `admin-dashboard.*`, `admin-kyc.*`
- [x] **G12.C2** 🔖 COMMIT `feat(admin-ui): add dashboard and kyc review screens` · hash: `b8bcfb5`

**Nhóm C — Tranh chấp & kiểm duyệt**

- [x] **G12.14** `admin-disputes` miền + hạ tầng
- [x] **G12.15** `RefundsPage`
- [x] **G12.16** `admin-moderation` đánh giá
- [x] **G12.17** `admin-moderation` sản phẩm
- [x] **G12.18** `ProductModerationPage`
- [x] **G12.19** Khóa i18n `admin-disputes.*`, `admin-moderation.*`
- [x] **G12.C3** 🔖 COMMIT `feat(admin-ui): add dispute and moderation screens` · hash: `7045474`

**Nhóm D — Danh mục**

- [x] **G12.20** `admin-catalog` miền + hạ tầng
- [x] **G12.21** `CatalogPage`
- [x] **G12.22** `CatModal`
- [x] **G12.23** `AttributePanel`
- [x] **G12.24** Duyệt đề xuất danh mục của shop; khóa i18n `admin-catalog.*`
- [x] **G12.C4** 🔖 COMMIT `feat(admin-ui): add catalog management screens` · hash: `93b53d5`

**Nhóm E — Tài chính**

- [x] **G12.25** `admin-finance` miền + hạ tầng
- [x] **G12.26** `WithdrawalsPage`
- [x] **G12.27** `CommissionPage`
- [x] **G12.28** `LedgerPage`
- [x] **G12.29** Đối soát COD miền
- [x] **G12.30** `CodReconciliationPage`
- [x] **G12.31** Khóa i18n `admin-finance.*`
- [x] **G12.C5** 🔖 COMMIT `feat(admin-ui): add payout, commission, ledger and cod screens` · hash: `e35446a`

**Nhóm F — Marketing**

- [x] **G12.32** `admin-marketing` voucher
- [x] **G12.33** `PlatformVouchersPage` + `PlatformVoucherModal`
- [x] **G12.34** `FlashSalePage`
- [x] **G12.35** Banner miền
- [x] **G12.36** `BannersPage`
- [x] **G12.37** `PlatformNotificationsPage`
- [x] **G12.38** Khóa i18n `admin-marketing.*`
- [x] **G12.C6** 🔖 COMMIT `feat(admin-ui): add voucher, flash sale, banner and notification screens` · hash: `c070b95`

**Nhóm G — Hệ thống**

- [x] **G12.39** `admin-system` miền + hạ tầng
- [x] **G12.40** `StaffPage` (`Rbac`)
- [x] **G12.41** `InviteStaff`, `RbacEditor`
- [x] **G12.42** `UsersPage`
- [x] **G12.43** `AuditLogPage`
- [x] **G12.44** `AccountAdminPage`
- [x] **G12.45** Khóa i18n `admin-system.*`
- [x] **G12.C7** 🔖 COMMIT `feat(admin-ui): add staff, users, audit log and account screens` · hash: `0b68d13`

## G13 — Hoàn thiện, kiểm thử, bàn giao


**Nhóm A — Ngôn ngữ & thương hiệu**

- [x] **G13.01** Quét chuỗi cứng
- [x] **G13.02** Hoàn tất `en.json`
- [x] **G13.03** Rà từ ngữ R11
- [x] **G13.04** Rà tiền / ngày
- [x] **G13.C1** 🔖 COMMIT `refactor(web): complete i18n coverage and brand wording` · hash: `fc589e8`

**Nhóm B — Chất lượng giao diện**

- [x] **G13.05** So khớp hình storefront
- [x] **G13.06** So khớp hình Seller + Admin
- [x] **G13.07** Trợ năng
- [x] **G13.08** Rà chỉ thị hydrate
- [x] **G13.09** Rà SEO lần cuối
- [x] **G13.10** Rà kiến trúc
- [x] **G13.C2** 🔖 COMMIT `fix(web): resolve visual parity, accessibility and hydration issues` · hash: `0914230`

**Nhóm C — Kiểm thử**

- [x] **G13.11** Bổ sung Vitest
- [x] **G13.12** E2E khách vãng lai
- [x] **G13.13** E2E mua hàng
- [x] **G13.14** E2E đơn hàng
- [x] **G13.15** E2E người bán
- [x] **G13.16** E2E quản trị
- [x] **G13.C3** 🔖 COMMIT `test(web): add unit and e2e coverage for migrated screens` · hash: `7254e9f`

**Nhóm D — Dọn dẹp & bàn giao**

- [x] **G13.17** Xóa `.gitkeep` ở thư mục đã có file; xóa stub không dùng
- [x] **G13.18** Tài liệu
- [x] **G13.19** ADR
- [x] **G13.20** Danh sách việc nối backend
- [x] **G13.21** CI
- [ ] **G13.22** Mở PR vào `develop`
- [x] **G13.C4** 🔖 COMMIT `docs(web): document mock mode, ui gallery and backend handoff` · hash: `9228a94`

---

## Ghi chú

### Môi trường đối chiếu (G00.09, G00.10)

- Prototype: `python -m http.server 8000` trong `MultivendorEcommercePrototype`, mở `genie/ui_kits/{storefront,auth,vendor,admin}/index.html`.
- Tài khoản demo: mật khẩu `Genie@123`, OTP `123456`, khách vãng lai `?guest=1`.
- Ảnh tham chiếu (16 màn × 360 / 768 / 1440 px, 47 ảnh) chụp bằng Playwright: `MultivendorEcommercePrototype/.impeccable/review/migration-ref/` (ngoài repo).

### Lỗi baseline của scaffold (G00.08)

| Lệnh | Lỗi | Đã sửa ở |
|---|---|---|
| `npm run lint` | `src/env.d.ts` 1:1 `@typescript-eslint/triple-slash-reference` | G01.11 (override cho `env.d.ts`) |
| `npm run test` | Vitest quét nhầm `tests/e2e/*.spec.ts` của Playwright → 2 file lỗi, 0 test | G01.02 (`vitest.config.ts` chỉ lấy `src/**/*.test.ts`) |
| `npm run check` | sạch (0 lỗi) | — |
| `npm run build` | thành công | — |
| `check-i18n` | OK — 3 khóa | — |

### Bảng lớp `lz-*` → component `shared/ui` (G02.14)

| Lớp CSS prototype | Component đích | Xong |
|---|---|---|
| `.lz-btn` (+ `--secondary/outline/ghost/subtle/danger/sm/lg/block`) | `Button` | ☑ |
| `.lz-iconbtn` (+ `--solid/outline/sm`) | `IconButton` | ☑ |
| `.lz-field`, `__label`, `.req`, `__hint`, `__error` | `Field` | ☑ |
| `.lz-inputwrap` (`__icon`, `--icon`, `--icon-right`), `.lz-input`, `--invalid` | `Input` | ☑ |
| `.lz-textarea` | `Textarea` | ☑ |
| `.lz-select` | `Select` | ☑ |
| `.lz-check`, `__box` | `Checkbox` | ☑ |
| `.lz-radio`, `__box` | `Radio` | ☑ |
| `.lz-switch`, `__track`, `__thumb` | `Switch` | ☑ |
| `.lz-badge` (+ `--neutral/primary/solid/flash/success/warning/danger/mint/gold/outline`) | `Badge` | ☑ |
| `.lz-tag` (+ `--brand`, `__x`) | `Tag` | ☑ |
| `.lz-status`, `__dot` | `StatusBadge` | ☑ |
| `.lz-price` (`__now`, `.sym`, `__was`, `--sm/--lg`) | `Price` | ☑ |
| `.lz-rating` (`__stars`, `__num`, `__count`) | `Rating` | ☑ |
| `.lz-qty` | `QuantityStepper` | ☑ |
| `.lz-vendor` (`__logo`, `__name`, `__meta`) | `VendorChip` | ☑ |
| `.lz-voucher` (+ `--platform/shop/freeship`, `__amt`, `__body`, `__kind`) | `VoucherTag` | ☑ |
| `.lz-card` | `Card` | ☐ |
| `.lz-pcard` (`__media`, `__flags`, `__disc`, `__body`, `__title`, `__foot`, `__loc`) | `ProductCard` | ☑ |
| `.lz-spin`, `.lz-typing-dot` (nhắc trong `base.css`) | `Spinner`, chấm đang gõ của `ChatWidget` | ☐ |

### Lệch giao diện so với prototype (G13.05, G13.06)

| Route | Khổ màn | Mô tả lệch | Đã sửa |
|---|---|---|---|
| Mọi trang storefront | 360 / 768 / 1440 | Tiêu đề nhóm footer màu trắng; prototype (theme genie, hoian, dongson, dongho) tô vàng — `<h2>` trong `summary` đặt `color: var(--text-inverse)` đè màu theme của `.tr-foot__head` | ☑ `h2` kế thừa màu |
| `/c/*`, `/search` | 360 | Nút sắp xếp "Liên quan / Bán chạy / Mới nhất" gãy 2 dòng; prototype 1 dòng cuộn ngang — liên kết nằm trong `<nav class="contents">` nên quy tắc `.tr-sortbar > *` không áp | ☑ thêm `.tr-sortbar > nav > *` + `nowrap` |
| `/seller/products` | 360, 1024 | Trang tràn ngang (799 / 1047 px): nhãn `sr-only` trong `th` thoát khung cuộn bảng (cùng lỗi G12.40) | ☑ khung bảng `relative` (kèm `DataTable` dùng chung, bảng biến thể) |
| `/policies/*` | 360 | Prototype hiện cả danh sách mục lục (dài) trước nội dung; web gập vào nút "Thông tin hỗ trợ" | Giữ — cải tiến có chủ đích từ G08 (nội dung lên trước) |
| `/account/profile` | 360 | Ảnh đại diện lên đầu form (prototype để cuối) | Giữ — khối ảnh đọc trước khi sửa thông tin |
| `/become-seller` | 768 | Form đăng ký 2 cột (prototype 1 cột ở 768) | Giữ — đủ rộng, ít cuộn hơn |

### Task bỏ qua / thay đổi so với plan

| Mã task | Lý do |
|---|---|
| G00.03 | Không có quyền truy cập Jira từ máy làm việc → bỏ mã `[#GNE-…]` trong commit / tên nhánh (D6). Khi có Epic thì bổ sung ở tiêu đề PR. |
| G00.04 / G00.05 | Đã tạo nhánh `develop` và `feature/web-foundation` **ở local**; chưa `git push` lên `origin` — chờ chủ dự án xác nhận đẩy nhánh. |
| G00.06 / G01.C1 | Hook `lint-staged` lỗi trên Windows (prettier chạy từ gốc repo không tìm thấy `prettier-plugin-astro`, `eslint` không có trên PATH) → chuyển cấu hình sang `web/.lintstagedrc.mjs` (gọi binary trong `web/node_modules`) và `web/.prettierrc.mjs` (import plugin). Không dùng `--no-verify`. Kèm `package-lock.json` gốc (CI chạy `npm ci` ở gốc cho commitlint). |
| G01.07 | Trong cùng feature dùng import tương đối; `@/features/<x>/<tầng>/**` bị chặn ở mọi nơi. `pages/` lấy dữ liệu SSR qua hàm `*.api.ts` được xuất ở `index.ts` (bỏ quyền import sâu `feature-infrastructure` của pages). |
| G01.06 | Luật boundaries dùng capture `feature`: tầng trong feature chỉ import tầng của **chính** feature đó; bật `no-unknown-files` + `no-unknown` cho `src/**`. Đã thử 5 kiểu vi phạm (ui → infrastructure feature khác, import sâu bằng alias, file lạ, shared → features, domain → infrastructure): đều báo lỗi. |
| G02.08 / G02.13 | CSS chuẩn bắt buộc `@import` đứng đầu nên `responsive.css` (và sau này CSS component) được import ngay sau `base.css`, trước `@tailwind`; quy tắc của chúng là bộ chọn phần tử / `@layer components` nên không đè utility. Bộ chọn `[style*=…]` của prototype không mang sang — bảng đối chiếu sang tiền tố `sm:/md:/lg:` ghi ở đầu `styles/responsive.css`. Điểm gãy Tailwind: sm 481, md 769, lg 1101, xl 1200, 2xl 1440. |
| G02.15 | `/dev/ui` dùng `AppShellLayout` (thêm prop `noindex`, mặc định bật) để pages không import thẳng `styles/`; build production trả 404. |
| G03.03 | Đã chép đủ 4 logo; chỉ `vnpay.png` và `card.svg` (mô tả thẻ qua VNPay) được khai trong `shared/lib/assets.ts`. `momo-logo.png`, `zalopay-logo.png` **ngoài phạm vi TS 3.5** (chỉ VNPay / VietQR / COD) → không hiển thị ở đâu. |
| G03.05 | `favicon.svg` ở gốc prototype thực chất là favicon mặc định của Astro (chữ A); prototype dùng `genie-coin.svg` (97 KB vì nhúng PNG). Đã dựng `favicon.svg` 6,7 KB từ đồng xu (bỏ lớp PNG), xuất `favicon.ico` 16/32/48 và `apple-touch-icon.png` 180. |
| G03.08 | `GenieLogo` dùng font `MJ Milestone Script` (Fortune Foundry, "All rights reserved", không kèm giấy phép web) → **không** phát hành tệp font: chuyển chữ "Genie" thành path SVG bằng fontTools (`shared/ui/brand/genieLettering.ts`), giống cách prototype đã làm với chữ trên đồng xu. `Baloo 2` là font theme `sleeve` (OFL) → xử lý ở G07.24. |
| G03.09 | SVGO `--multipass`: `dongson-drum.svg` 1.713 KB → 440 KB, `hoian-lanterns.svg` 4,9 → 3,8 KB; đặt ở `public/img/themes/` (kèm `genie-lamp.svg`), chỉ tải qua `background` của bộ chọn `[data-theme=…]` nên theme khác không tải. |
| G04.06 | Bảng khóa theo LUU_TRU_DU_LIEU 6.2: `genie.popup.seen` là **sessionStorage** (plan ghi chung là storage); thêm các khóa `checkout.draft/idem`, `search.recent`, `recent.viewed`, `ui.seller/admin`. Phong bì `{version, expires_at}` + `clearUserData()` đúng mẫu 6.5. |
| G04.07 / G04.08 | Ngôn ngữ khi SSR lấy theo request qua `registerServerLocale()` (middleware sẽ dùng AsyncLocalStorage ở G07.10) thay vì biến toàn cục, tránh lẫn ngôn ngữ giữa request song song; trên trình duyệt dùng nanostore `$locale` khởi tạo từ `<html lang>`. Ghi chú quy ước khóa đặt ở khóa `_meta.convention` của cả 2 file JSON (JSON không có chú thích). |
| G04.04 | Che dữ liệu đặt riêng `shared/lib/mask.ts` (không gộp vào `format.ts`). |
| G04.14 | Element `shared-hooks` đã khai ở G01.06; đã thử: `feature-domain` import `@/shared/hooks` → lỗi boundaries. |
| G05.01 | API_SPEC 0.4 có **hai** kiểu phân trang: số trang `{page,size,total_items,total_pages}` và con trỏ `{limit,has_more,next_cursor}` → khai cả `Page<T>` và `CursorPage<T>`; mock có `paginatePage` + `paginateCursor`. |
| G05.02 | Nhập đủ **92 mã** của danh mục Document_Vi 3.7.1 vào `errors.*` (vi + en) và `shared/api/error-codes.ts`; prototype không còn mã `LZD-*` nào trong JSX, `normalizeCode()` vẫn đổi `LZD-` → `GNE-` phòng dữ liệu cũ. Thêm `errors.network/timeout/not_found/unknown` cho lỗi phía client. |
| G05.07 | Mock có 2 tài khoản: người mua + chủ shop `MOCK_USER` (Trần Thị Mai) cho storefront / `/seller`, Admin `MOCK_STAFF` (Đặng Hải Yến, đủ 13 quyền) cho `/admin` — chọn theo đường dẫn, không có nút đổi vai (D5). |
| G05.09 | `Providers` gồm QueryClient + khởi tạo locale + `bootstrapSession()`; vùng hiện toast là `ToastHost` tách riêng (G06.14 / G07.07) vì mỗi island là một cây React riêng — toast đi qua nanostore dùng chung. |
| G06.06 | Luật mật khẩu (passwordIssue, độ mạnh 0–4) đặt ở `shared/lib/password.ts` để `PasswordInput` (shared/ui) dùng được; `features/auth/domain/password.rules.ts` (G09.01) sẽ tái xuất từ đây. |
| G06.07 / G06.09 | `Checkbox`, `Radio`, `Switch` nằm chung `shared/ui/Choice.tsx`; `Tabs`, `Segmented` chung `Tabs.tsx`. `Segmented` giữ đúng prototype (`.dash-seg`: role=group + aria-pressed), `Tabs` là tablist có phím mũi tên và hỗ trợ `href` (R16). |
| G06.12–G06.19 | Đã kiểm bằng Playwright trên `/dev/ui`: ConfirmDialog focus vào nút đầu (bỏ nút Đóng như prototype), Esc đóng; Dropdown ↓ đi đúng mục; OtpInput dán "123456" điền đủ 6 ô. Sửa xung đột lớp Tailwind (`border-transparent` đè viền biến thể secondary/outline; nền trang hiện tại của Pagination). |
| G06.23 | Storefront prototype thực tế dùng thẻ `CardX` (lớp `.tr-card*` trong storefront.css), không dùng `ProductCard` của DS (`.lz-pcard`). `shared/ui/ProductCard` dựng theo `CardX` ở mức "Marketplace density" (thân 13px, giá 15px, tim 32px — tim có vùng chạm 44px bằng pseudo-element), giữ nguyên tên lớp `tr-card*` để `themes.css` đè được; ở G07.01 các quy tắc `.tr-card*`, `.tr-bar`, `.tr-ticket*` của storefront.css không chép lại (đã nằm trong component). Liên kết chính là `<a href>` (R16), ảnh có `alt`/`width`/`height` (R18). |
| G06.21 / G06.25 / G06.28 | Thêm `RatingInput` (radiogroup, dùng cho ReviewModal), `Ticket` + `TicketSaveButton` (phiếu .tr-ticket), `FormSection` + `FormGrid` + `DateInput` ở `FormSection.tsx` (`Field` đã có từ G06.04). |
| G06.29–G06.33 | Bộ thống kê dùng lại lớp `dash-*` chuyển nguyên từ `dash.css` sang `styles/dash.css` (bọc `@layer components`, nạp trong `AppShellLayout`) vì lưới `--n`, bộ chọn `[data-tone]`, mũi tên `::after` của pipeline không gọn bằng utility; bỏ `.dash-seg` (Segmented là utility) và `.tr-kycdemo` (D5). Bảng ICONS prototype **thiếu `arrow-down-right`** mà `Delta` dùng (prototype không hiện mũi tên giảm) → bổ sung icon Lucide. `short()` → `formatMoneyShort()` trong `format.ts` ("45,8 tr" / "45.8M"). |
| G06.37 | 9 file stub `return null` (Badge, Button, EmptyState, Input, Modal, Price, Rating, Select, Toast) được **viết đè tại chỗ** bằng bản thật trong G06.01–G06.25 nên không còn file stub nào để xóa; `grep TODO src/shared/ui` rỗng. |
| G06.36 | `/dev/ui` có 4 island (`GalleryBasics`, `GalleryOverlays`, `GalleryCommerce`, `GalleryDash`) đủ biến thể; đã chụp so ở 1280 / 1440 / 360 px với ảnh tham chiếu (thẻ sản phẩm trang chủ, bảng điều khiển vendor). |
| G07.01 | Tách bằng script theo **từng bộ chọn** (mỗi quy tắc nhiều bộ chọn được chia về đúng khối, giữ thứ tự gốc và vị trí trong @media) → `styles/storefront/{shell,header,home,footer,product,search,cart,checkout,account,chat}.css` + `index.css`, bọc `@layer components`. Không có quy tắc nào thuộc `shop`/`order` trong storefront.css gốc nên không tạo 2 file đó. Bỏ 56 bộ chọn: `.tr-card*` (29), `.tr-ticket*` (17), `.tr-bar` (3) — đã nằm trong ProductCard/FlashProgress/Ticket; `.tr-demo`, `.tr-acct__demo` (D5); 3 bộ chọn `[style*=…]` khớp style inline của prototype. |
| G07.02 / G07.03 | Màu cứng → token: `#fff` nền → `--surface-card`, chữ → `--text-inverse`, viền / vòng focus trắng → `--surface-card`; thêm token `--foot-ink`, `--foot-legal`, `--camp-ink`, `--camp-sub`. Giữ nguyên các lớp phủ `rgba(11,31,75,α)` / `rgba(255,255,255,α)` của lớp kính (không có token alpha) và kích thước px của bố cục. Tailwind `safelist: /^(tr|dash|gn|lz)-/` để lớp ghép động không bị loại; `Button` thêm móc `lz-btn` để `.tr .lz-btn` (nút bo viên thuốc) áp dụng. |
| G07.10 | Middleware dùng `AsyncLocalStorage` + `registerServerLocale()` để mọi `t()` / định dạng khi SSR đúng ngôn ngữ của request (đã thử: cookie `lang=en` → `<html lang="en">`, `og:locale=en_US`). Trang cần đăng nhập: `/seller`, `/admin`, `/checkout` (trừ `/checkout/result`), `/orders`, `/account`, `/chat`; `/cart` để khách vãng lai dùng (giỏ khách). Đã thử `/orders` → 302 `/auth/login?next=%2Forders`. |
| G07.08 | Lời hứa ở cột thương hiệu trang đăng nhập dùng `<p class="gn-auth__title">` thay `<h1>` của prototype để trang chỉ có một `<h1>` là tiêu đề form (R18); kiểu chữ tương ứng thêm khi chuyển `themes.css` (G07.22). Khóa theme lưu `genie.theme` (prototype: `genie_theme`). |
| G07.C2 / G07.C3 | Hai commit 7cfb914, 3b73c77 build hỏng (`dash.css` import riêng trong AppShellLayout → Tailwind báo `@layer components` thiếu `@tailwind components`); sửa bằng commit riêng nhập `dash.css` qua `global.css`. Từ đây mọi commit đi qua script cổng: chỉ commit khi `npm run verify` xanh và có "Complete!". |
| G07.13–G07.19 | Header cần dữ liệu thật nên làm sớm một phần các feature: `catalog` (đủ 4 tầng — phần lớn G08.04), `notifications` (đủ 4 tầng + NotifPanel), `cart` (types, mock seedCart, store, MiniCart — G10.01–03 mở rộng), `search` (gợi ý — G08.17 mở rộng), `auth` (UserMenu, LoginPrompt, store `requireLogin`), `policies` (dữ liệu 25 trang sinh từ pages-data.js — tức G08.41). Bộ seed dùng chung `shared/api/mock/seed.ts` **sinh tự động** từ data.jsx (sản phẩm p1→id 1, shop techzone→id 1) để các `*.mock.ts` không chép trùng dữ liệu. |
| G07.18 / G07.19 | Màn hẹp: giữ hàng danh mục cuộn ngang như prototype + thêm chip "Tất cả danh mục" mở ngăn kéo (Modal) có cả danh mục con. Footer dùng `<details>/<summary>` (gập ở ≤768px không cần JS; màn rộng luôn mở). Logo header/footer: chữ "Genie" dạng SVG (`GenieWordmark`) thay font Genie Script. Prototype có **25** trang chính sách (plan ghi 28). |
| G07.16 | Layout được phép import `app/` (Header là island lớn cần `Providers`) — bổ sung vào luật boundaries. `SessionUser` thêm `shop_name` cho mục chuyển sang Kênh người bán. |
| G07.24 | Tự lưu trữ font 8 theme (Archivo, Alegreya, Baloo 2, Bai Jamjuree, Bricolage Grotesque, Lexend, Phudu, Saira Condensed — OFL 1.1) tải đúng URL Google Fonts của prototype, chỉ giữ tập latin / latin-ext / vietnamese: 45 tệp woff2 (1,2 MB) ở `public/fonts/themes/`, khai trong `styles/theme-fonts.css`; trình duyệt chỉ tải khi theme tương ứng bật. |
| G07.26 | Đã kiểm 9 theme trên khung trang hiện có (header, footer, nút, trang 404) ở 1440 px bằng `?theme=`; lựa chọn lưu `genie.theme` và giữ khi mở lại. Tương phản đo tự động: nút chính 5,76–18,72; h1 ≥ 15,2; chữ phụ ≥ 6,45; link footer ≥ 5,34 (đạt WCAG AA). Trang chủ / sản phẩm / đăng nhập / Seller / Admin chưa dựng → kiểm lại 9 theme trên các trang đó ở G13.05, G13.06. |
| G07.22 / G07.23 | themes.css giữ nguyên (không bọc @layer, độ ưu tiên [data-theme] đè utility); bỏ @font-face "Genie Script" và quy tắc font của `.tr-logo span`/`.tr-foot__brand` (logo là SVG); ảnh nền theme đọc từ `/img/themes/`. ThemePicker còn gắn ở góc trang đăng nhập (AuthTools); thanh trên Admin gắn ở G12.03. |
| G08.02 | `badgeStatus` (ánh xạ trạng thái đơn → màu nhãn) thuộc miền đơn hàng, chuyển sang `features/orders/domain` ở G10.30. `product.rules` có: đường dẫn, % giảm, nhãn, giá / tồn theo phân loại, trạng thái kho, mã phân loại trong liên kết chia sẻ, kiểm slug chuẩn (301). |
| G08.03 | Mock chi tiết dùng đúng dữ liệu prototype cho mọi sản phẩm: phân loại COLORS/SIZES, 4 ảnh (ảnh chính + banner-tech, keyboard, speaker), bảng thông số và mô tả HTML mẫu của ProductDetail.jsx. Lọc / sắp xếp / phân trang chạy trong `product.mock.ts` (mô phỏng backend), có test `product.api.test.ts`. |
| G08.10 | Prototype Home.jsx không có lưới ô danh mục; danh mục đã có ở hàng danh mục header (SSR) nên không dựng CategoryTiles để giữ đúng bố cục gốc. |
| G08.14 | Theo DESIGN.md, Promise Strip nằm ở trang sản phẩm dưới hàng shop: đã dựng product/ui/PromiseStrip.astro (SSR), sẽ ghép vào trang sản phẩm ở G08.35; trang chủ giữ hero + Money Rail như prototype. |
| G08.15 | Thêm features/<x>/server.ts làm API công khai cho component .astro (FlashBand, VerifiedShops, PromiseStrip), ESLint chặn .tsx import server.ts để island không kéo .astro vào bundle; env.d.ts khai báo module *.astro cho tsc thuần. |
| G08.17 | Trạng thái lọc sống trong query string (parseFilters/serializeFilters/filtersHref); bộ lọc là form GET thật, sắp xếp / thẻ lọc / phân trang là liên kết — tắt JS vẫn dùng được. ProductQuery đổi brand/attribute đơn sang brands/attributes nhiều giá trị, thêm categoryIds, locations, hasVoucher; thêm getFacets(). |
| G08.18 | Danh sách Thương hiệu / Nơi bán lấy theo phạm vi từ khóa (getFacets) thay vì toàn bộ sản phẩm như prototype, để không hiện lựa chọn cho ra 0 kết quả. Trang danh mục thêm nhóm thuộc tính ngành (UC-CAT-05) mà prototype chưa có. Khoảng giá ghi bằng formatVND ("5.000.000₫" thay cho "5 triệu"). |
| G08.20 | /c/<danh-mục-con> chuyển 301 về /c/<gốc>?sub=<slug>; danh mục lạ trả 404. Bộ lọc Chính hãng gồm cả thương hiệu đã xác minh (giống Search.jsx) trên cả hai trang. |
| G08.21 | Thêm khối "shop khớp từ khóa" theo plan (prototype Search.jsx chưa có); phân trang 20 sản phẩm / trang thay vì hiện hết. |
| G08.22 | /discover mặc định trang 1 (prototype mở trang 2 vì trang chủ đã hiện trang 1) để URL gốc và canonical khớp nội dung; trang chủ "Xem thêm" dẫn sang /discover?page=N. |
| G08.26 | Thêm SKU (product_variant_id) cho mỗi tổ hợp phân loại vào Product (skus, skuOf). Khách: giỏ khách chỉ lưu SKU + số lượng (LUU_TRU 6.3), header hiện số dòng giỏ khách; hiển thị dòng giỏ khách và gộp sau đăng nhập làm ở G10. "Mua ngay" chuyển /cart?buy=<khóa dòng> (trang giỏ xử lý ở G10). |
| G08.29 | Bỏ ô xám "Ảnh minh hoạ sản phẩm" trong mô tả prototype (phần trình diễn, D5); mô tả là HTML người bán, thu gọn 480px bằng script nhỏ, tắt JS hiện đầy đủ. |
| G08.33 | Mock: mọi sản phẩm dùng 4 đánh giá của prototype; phân trang đánh giá có liên kết ?review_page=N#reviews thật. Huy hiệu sản phẩm "Chính hãng" lấy theo sản phẩm hoặc shop chính hãng (prototype dùng cờ của riêng sản phẩm). |
| Commit | Theo yêu cầu người dùng 2026-11-13: commit bằng git config của người dùng, không thêm dòng Co-Authored-By; đã gỡ dòng này khỏi 3 commit local chưa push (hash mới 7a87a36, 5058a10, 3b3016d). |
| G08.37 | Tab danh mục / Mới nhất / Bán chạy là liên kết ?tab= (có phân trang ?page=); tìm trong shop là form GET sang /search (scope=shop / scope=all). Nút Theo dõi tách thành FollowButton dùng chung với dải shop trang sản phẩm; thêm listShopVouchers(). |
| G08.38 | Khung giờ Flash Sale 00/09/12/15/20 giờ (giờ Việt Nam, flash.rules + test), chọn khung bằng ?slot=HH; mock dùng cùng danh sách sản phẩm Flash Sale cho mọi khung. Countdown thêm prop label/prefix; thêm clockVN() vào format.ts. |
| G08.39 | Chiến dịch lạ trả HTTP 404 kèm thông báo của prototype; thêm mã lỗi GNE-CMP-4041 (vi/en) và ProductQuery.minDiscount để lấy sản phẩm tham gia. |
| G08.41 | Dữ liệu chính sách đã sinh sớm ở G07 (25 trang trong 5 nhóm như pages-data.js của prototype, không phải 28); mã cũ của prototype (legacy_id) chuyển 301 về slug chuẩn. |
| G08.45 | robots.txt sinh động (pages/robots.txt.ts) thay cho public/robots.txt để dòng Sitemap là URL tuyệt đối theo tên miền; chặn thêm /cart, /orders, /chat, /auth, /dev ngoài /seller, /admin, /account, /checkout. |
| G08.47–G08.53 | JSON-LD trang chủ / danh sách / sản phẩm, phân trang SSR, mã trạng thái đã làm cùng từng trang ở nhóm B–F; nhóm G thêm canonicalUrl() (bỏ tham số theo dõi, trang lọc / sắp xếp trỏ về gốc, giữ page), bổ sung width/height cho ảnh lightbox và ảnh bìa shop, kiểm tra một h1 mỗi trang bằng e2e. |
| G08.54 | tests/e2e/seo.spec.ts chạy tắt JavaScript, 8 kịch bản, xanh trên cả mobile-360 và desktop-1440 (chạy tay bằng npx playwright test — npm run verify chỉ chạy Vitest). |
| G09.03 | Theo API_SPEC §2: login (POST /auth/sessions), OTP challenge / resend / verify, registrations, password-resets, logout, OAuth token. Không có endpoint kiểm trùng email riêng — trùng email báo qua 409 GNE-AUTH-4091 khi đăng ký (form hiện liên kết Đăng nhập / Quên mật khẩu). Mock giữ trạng thái khóa / OTP trong bộ nhớ trình duyệt. |
| G09.04 | Gộp giỏ khách sau đăng nhập ghép ở tầng UI (ui/completeLogin.ts truyền mergeGuestCart của cart) vì ESLint boundaries không cho application import feature khác; thêm mergeGuestCart + POST /me/cart/merge (mock). AuthSession / VerifyResult trong domain là kiểu tổng quát để domain không phụ thuộc shared/auth. |
| G09.10 | Mock: hộp "Tiếp tục với Google / Apple" rồi đăng nhập luôn; thật: chuyển sang /auth/oauth/{provider}/authorize, next đi kèm địa chỉ callback. Bỏ bảng mô phỏng GoogleFlow (D5). |
| G09 (sửa kèm) | Header chỉ đọc phiên / số giỏ thật sau khi mount (hết lỗi hydration khi đã đăng nhập); giỏ khách hiện số dòng trên icon giỏ; quy tắc min-height 44px cho label ở màn hẹp không áp vào nhãn ô nhập (.lz-field). |
| Nhánh | feature/web-auth tạo từ feature/web-storefront-public (các nhánh xếp chồng vì chưa mở PR / merge vào develop; vẫn local, chưa push). |
| Nhánh (G10) | feature/web-buyer tạo từ feature/web-auth (xếp chồng, local, chưa push). |
| G10.02 | DTO giỏ theo đúng API_SPEC 6.1 (nhóm theo shop, `id` dòng giỏ, `variant_id`, `is_available`, `unavailable_reason`); thêm 2 trường client cần mà spec chưa có: `product_slug` (liên kết /p/{slug}-{id}, R16) và `category_name` (voucher theo ngành khi tính ở mock) — ghi vào danh sách nối backend G13.20. Giỏ khách dựng dòng hiển thị qua `GET /public/variants?ids=` (spec chưa có endpoint tra SKU công khai — bổ sung G13.20). Đổi phân loại dùng API 5.10 `GET /public/products/{id}/variants` (`useVariantChoice`). |
| G10.02 (mock) | Trang storefront là MPA nên mock trong bộ nhớ mất khi chuyển trang → thêm `mockState(name, init)` ở `shared/api/mock.ts`: giữ trạng thái mock có thể sửa (giỏ, sau này đơn, địa chỉ…) trong sessionStorage `genie.mock.<name>`, chỉ khi USE_MOCK; SSR / test giữ trong bộ nhớ. Xóa cùng lớp mock khi nối backend. |
| G10.03 | `useCart` đọc store nanostores dùng chung (header ↔ trang giỏ) thay vì cache TanStack riêng; TanStack Query chỉ dùng tra dòng giỏ khách và phân loại. Thêm `setLineQty`, `setLineVariant`, `removeLines`, `buyAgain` (6.6, dùng ở G10.37), thao tác giỏ khách; test gộp giỏ khách `mergeGuestCart.test.ts`. |
| G10.04–G10.07 | Tên sản phẩm / ảnh / tên shop là `<a href>` thật; nút Chat dẫn `/chat?shop=` (G10.64). Dòng không mua được (hết hàng, shop nghỉ…) hiện nhãn lý do, không chọn được, khóa ô số lượng. Ô "chọn tất cả" của shop / thanh tổng có trạng thái một phần (indeterminate). Khách: xem giỏ tạm + thanh nhắc đăng nhập; bấm "Mua hàng" mở LoginPrompt (next=/cart, đăng nhập xong tự gộp giỏ). |
| G10.06 | `CartSummary.astro` của khung cũ bỏ đi: thanh tổng phụ thuộc lựa chọn của người dùng nên là `CartBar.tsx` trong island (prototype .tr-cartbar, màn hẹp tách công cụ + thanh trả tiền dính đáy). Phí ship / voucher / Điểm nối ở nhóm B. "Mua hàng" ghi `genie.checkout.draft` (selected_item_ids…) rồi sang /checkout — phần nháp tạo sớm trong `features/checkout` (G10.21 hoàn thiện phần khôi phục). |
| G10.08 | So ảnh /cart với sf-cart-1440 / sf-cart-360: khớp bố cục, nhóm shop, dòng đổi giá, thanh tổng dính đáy; phần voucher shop, Voucher Genie, Điểm, phí ship đối chiếu lại sau nhóm B. HTML SSR có đúng một `<h1>`. |
| G10.09–G10.10 | Luật voucher + tính giá xếp chồng đặt ở `promotions/domain/pricing.rules.ts` (một bộ cho Giỏ và Thanh toán): voucherLines, eligibleBase, voucherCheck (lý do `no_items` / `min_order` / `not_active`), voucherDiscount, priceCart, bestVouchers, pruneVoucherSelection, extraSaving, đổi lựa chọn ⇄ mã để lưu nháp. Phí ship truyền vào bằng hàm `ShipFn` (domain không import feature khác). Thêm chặn tổng giảm Shop + Sàn ≤ 85% tiền hàng theo API_SPEC 6.8 / LLD 9.3 (prototype chưa có). Test 13 ca, gồm "bộ số vàng" của giỏ mẫu prototype: tổng 3.485.200₫, giảm 363.800₫, phân bổ voucher sàn 37.635 / 44.408 / 66.757₫. Đây là bản tính phía client cho mock; nối backend thì lấy từ POST /me/checkout/preview. |
| G10.11 | `checkout/domain/shipping.rules.ts` (shipFee, shipOf, makeShipFn, isFreeByShop) + test. Chính sách phí ship lấy qua `useShipPolicies` — API_SPEC chỉ trả phí ship trong preview 6.8, tạm dùng `GET /public/shops/shipping-policies?ids=` (ghi G13.20). |
| G10.12 | Kho voucher dùng lại `listMyVouchers` (G08); thêm `findVoucherByCode` cho ô nhập mã (6.9 có `code`, trả cả voucher chưa lưu; mã hết hạn / đã dùng → GNE-VCH-4041). Tạo sớm `features/coins` tối thiểu (GET /me/coins → số dư) cho công tắc Điểm; G10.56 mở rộng. |
| G10.13–G10.15 | `VoucherPicker` ở promotions/ui (bám VoucherPicker.jsx; voucher không đủ điều kiện mờ + lý do; nhập mã). Ghép giá ở `cart/ui/useCartPricing.ts` (kho voucher + phí ship + số dư Điểm + nháp checkout) — sẽ dùng lại ở trang Thanh toán. Lựa chọn voucher / Điểm lưu `genie.checkout.draft` (voucher_codes, use_coins), tải lại trang vẫn giữ; chưa chọn lần nào → bộ mã giảm nhiều nhất như prototype. Khách vãng lai không hiện voucher / Điểm (cần tài khoản). Đã thử thật: bỏ voucher sàn → 3.634.000₫, bật Điểm → 3.632.750₫, tải lại giữ nguyên. |
| G10.16–G10.17 | Miền `checkout.types` (PaymentMethod VNPAY / VIETQR / COD, CheckoutSession, PaymentInstruction, PlaceOrderInput, HOLD_MINUTES, NOTE_MAX) + `checkout.rules` (đếm ngược giữ chỗ, nhãn nút theo phương thức, đọc `out_of_stock_cart_item_ids` của GNE-INV-4090, `resultView`, mã đơn TR-yyyyMMdd-NNNNNN theo giờ VN) + test. Hạ tầng 6.10–6.12 gửi Idempotency-Key (dùng lại `genie.checkout.idem` tới khi đặt thành công). Mock không có engine giá phía server → `createCheckout` nhận thêm `CheckoutQuote` (bảng giá client đã hiển thị) chỉ để dựng đơn mock; API thật bỏ qua. Kho mock dùng chung `shared/api/mock/placed.ts` ("đơn vừa đặt"): giỏ mock bỏ dòng đã đặt, ví Điểm mock trừ Điểm đã dùng, Đơn mua (G10.31) sẽ đọc đơn mới — thay cho transaction phía server. VNPay mock trả `payment_url = mock:vnpay` để client mở cổng giả lập (nhóm D). |
| G10.18 | `CheckoutPage` dùng lại `useCartPricing` của giỏ (cùng lựa chọn voucher / Điểm qua nháp) → số liệu khớp prototype: tổng 3.485.200₫, tổng đơn từng shop 1.339.200 / 812.765 / 783.000 / 408.294 / 141.941₫. Thêm "Có bộ mã tiết kiệm thêm … / Áp bộ mã tốt nhất" (extraSaving). Bỏ hộp "Mô phỏng lỗi thanh toán và hết hàng" của prototype (D5). |
| G10.19 | Đồng hồ giữ hàng 15 phút tính từ lúc vào trang (`HoldCountdown`), hết giờ cho "Giữ lại". Lỗi GNE-INV-4090 → dòng hết hàng bị gạch + nhãn "Hết hàng", nút "Bỏ món" (bỏ khỏi nháp) và "Sửa số lượng ở giỏ"; tổng tiền tính lại không gồm dòng đó. Dữ liệu mock mặc định không sinh lỗi này (không có công tắc kịch bản — D5); đường xử lý có test ở rules. |
| G10.20 | Làm sớm phần sổ địa chỉ của nhóm G để checkout dùng: miền / lược đồ zod / hạ tầng 3.8–3.9 / hook TanStack, `AddressModal` (chọn, đặt mặc định, sửa, thêm), `AddressForm` (react-hook-form + zod qua `shared/lib/zodResolver.ts` — không thêm @hookform/resolvers), `RegionPicker` (3 tab + tìm nhanh, cây GHN rút gọn của prototype), GPS → reverse-geocode (từ chối định vị thì dùng tọa độ TP. HCM). G10.48 / G10.49 sẽ tick khi có `AddressPanel` ở trang Tài khoản. SĐT hiển thị nhóm "0901 234 567" (`formatPhone`). |
| G10.21 | Nháp `genie.checkout.draft` đọc sau khi mount (tránh lệch hydrate); lời nhắn từng shop lưu nháp ngay khi gõ; đặt thành công thì xóa nháp + Idempotency-Key. Địa chỉ không lưu vào nháp (LUU_TRU 6.7). |
| G10.22 | /checkout khớp sf-checkout-1440 / 360 (bố cục 2 cột → 1 cột, thanh trả tiền dính đáy màn hẹp, đơn con gập "Xem thêm"). Đã thử đặt COD thật: chuyển `/checkout/result?session=8801`; trang kết quả làm ở nhóm D. |
| G10.23–G10.26 | `MoneyRail`, `HoldNote` có từ G08; thêm `MoneyChip` (hàm `moneyChip` + test), `PayModal` (chọn phương thức / bước QR), `VietQrPanel` (QR giữ chỗ, đếm ngược tới `expires_at`, sao chép số TK / nội dung `TR…`), `PayGateway`, `PaymentLogo` / `CodLogo` ở `payment/ui` — chỉ vẽ; `checkout/ui/PaymentFlow.tsx` ghép với hook checkout. VNPAY: thật → chuyển `payment_url`; mock (`mock:vnpay`) → cổng mô phỏng, "Thanh toán" → phiên COMPLETED, "Quay lại đơn hàng" = VNPAY trả về "khách hủy" → trang thất bại. VietQR: hỏi lại phiên mỗi 3 giây; mock tự coi SePay đã nhận tiền sau ~12 giây (thay nút "Mô phỏng: SePay báo đã nhận tiền" của prototype — D5). COD → kết quả ngay. |
| G10.27–G10.29 | `/checkout/result?session=<id>` (island `CheckoutResult`): chỉ đọc GET 6.11 rồi hiện OrderDone (mã giao dịch, từng đơn con + Theo dõi → /orders/{id}, tổng; COD "Thu khi nhận hàng", không có đường tiền), PaymentFailed (giữ chỗ còn mm:ss đếm từng giây, Thử lại / Đổi phương thức → PayModal → 6.12, Về giỏ; hết giữ chỗ thì ẩn nút thử lại), hoặc "Đang chờ xác nhận" (tự hỏi lại). Thiếu / sai mã phiên → "Không tìm thấy giao dịch" (prototype mở thẳng thì hiện biên nhận mẫu — không làm). Đã thử thật 4 luồng: COD, VNPAY thành công, VNPAY hủy, VietQR (360px) — đều đúng trang. |
| G10.30 | Miền đơn theo API_SPEC 7.1–7.4 + 16.3: `OrderStatus` đủ enum backend (gồm các trạng thái trả hàng), `statusGroupOf` (PAID của đơn COD đã giao thuộc "Chờ giao hàng"), `stepOf` / `STEPS`, `statusTone` (về đúng bộ tông của StatusBadge), `allowedActions` (server trả `allowed_actions`; mock tính bằng hàm này — hủy được tới PREPARING), `groupOrders` (đơn online cùng phiên chờ thanh toán / đã hủy → "Nhóm đơn hàng"), lý do hủy theo mã 16.4 + test. `badgeStatus` của G08.02 thành `statusTone` ở đây. |
| G10.31 | Mock = 11 đơn của prototype (mã "TR-20260608-000044" → id 44, thời điểm đổi sang ISO giờ VN) + đơn vừa đặt ở checkout (kho `placed`), thay đổi giữ trong phiên tab. Nhóm chờ thanh toán mẫu (CS-8F21A) có hạn thanh toán tính từ lúc mở và một phiên mock tương ứng để "Thanh toán ngay" đi đúng 6.12. `product_slug` thêm vào $OrderItem (ghi G13.20). |
| G10.32–G10.35 | `/orders` dùng khung tài khoản `AccountShell.astro` + island `AccountNav` (làm sớm phần khung của G10.44: menu liên kết thật, huy hiệu Thông báo / Kho Voucher, Đăng xuất có xác nhận, màn hẹp cuộn tới mục đang mở; đọc phiên sau khi mount tránh lệch hydrate). Tab là liên kết `?tab=<status_group>` (R16). Nút trên thẻ đơn vẽ theo `allowed_actions`; hộp hủy chọn lý do (OTHER bắt buộc ghi chú), hủy nhóm đơn gọi lần lượt từng đơn; "Đã nhận hàng" có hộp xác nhận chuyển tiền cho shop. Thêm lớp `lz-vendor__logo` cho VendorChip để `.tr-chip` ẩn ô chữ cái cạnh con dấu như prototype. Bỏ hộp "Mô phỏng cho buổi demo: kịch bản tài khoản" (D5). |
| G10.36–G10.38 | `/orders/[id]` (id không phải số → 404 thật): thanh 7 bước, thẻ ĐVVC + sao chép mã vận đơn, ghi chú giữ tiền / ngày giải ngân, hành trình mới nhất trước, địa chỉ, sản phẩm + tiền, nút theo trạng thái. Mua lại = POST /me/cart/items/batch (6.6) rồi sang giỏ (báo khi có món bị bỏ qua); Chat dẫn `/chat?shop=&order=`. Nút Trả hàng / Đánh giá / khối yêu cầu trả hàng gắn ở nhóm F. Đã thử thật: Đã nhận hàng (đơn 41 → Hoàn thành), Hủy đơn 47 (toast hoàn tiền), Mua lại đơn đã hủy (cộng dồn vào giỏ). |
| G10.39 | `features/refunds` đủ 4 tầng: kiểu theo 7.5–7.8, lý do trả hàng theo mã 16.4, `refundEstimate` (theo `paid_unit_price`, thiếu thì chia giảm giá theo tỉ lệ như prototype), `evidenceMissing` (≥ 1 ảnh + 1 video — GNE-SUP-4221), `needsBank` (VietQR / COD hoàn bằng chuyển khoản), lược đồ zod tài khoản hoàn tiền + test. Danh sách ngân hàng GET /public/banks. Mock ghi vào kho dùng chung `shared/api/mock/activity.ts`; mock đơn đọc lại để đổi trạng thái (RETURN_REQUESTED / COMPLETED khi chấp nhận từ chối / REFUNDED). |
| G10.40 | Tải tệp qua `shared/api/upload.ts` (4.1: xin URL ký sẵn → PUT MinIO → object_key) + `shared/ui/MediaPicker.tsx` (kéo thả, xem trước, xóa). Ảnh nén bằng browser-image-compression nhưng ra **JPEG** thay vì WebP như plan ghi, vì REVIEW_IMAGE / REFUND_EVIDENCE / AVATAR chỉ nhận jpeg, png (bảng 4.1); giới hạn theo 4.1: bằng chứng 10 MB/tệp (plan ghi 5 MB), ảnh đánh giá 5 MB, video đánh giá 30 MB. Form tài khoản hoàn tiền dùng react-hook-form + zod. |
| G10.41 | `RefundBlock` (id `#return`, thẻ đơn "Xem yêu cầu trả hàng" nhảy tới): phương án, số tiền, lý do, lời nhắn shop, hạn của bước, tài khoản nhận (đã che); hành động thật theo trạng thái (bổ sung bằng chứng 7.6, khiếu nại 7.7, chấp nhận từ chối 7.8). Bỏ các nút "Mô phỏng: Shop yêu cầu bổ sung / từ chối / đồng ý" của prototype (D5) — trạng thái đó do shop / sàn đổi ở G11 / G12. |
| G10.42 | `ReviewModal` ở `features/reviews` + POST /me/reviews (7.9): lần lượt từng sản phẩm của đơn, RatingInput, nhận xét ≤ 1000 ký tự, ≤ 5 ảnh, 1 video, ẩn tên; có ảnh +50 Điểm (mock cộng vào ví Điểm, đơn đánh dấu đã đánh giá). Nút "Trả hàng" / "Đánh giá" trên thẻ đơn dẫn `/orders/{id}?return=1` / `?review=1` (liên kết thật), trang theo dõi mở hộp tương ứng khi đơn cho phép. Đã thử thật: gửi trả hàng đơn 41 (VietQR, có tài khoản hoàn) → RFN-0301 "Chờ shop phản hồi", tiền hoàn dự kiến 383.000₫; đánh giá đơn 33 có ảnh → "+50 Điểm". |
| G10.44 | `/account/[...section]`: /account → /account/profile (302), /account/orders → /orders (301), mục lạ → 404 thật. Khung `AccountShell.astro` + `AccountNav` (làm ở nhóm E) — dòng phụ dưới tên ghi loại tài khoản (Email / Google / cả hai) theo GET /me/login-methods như prototype; nhãn "Thiết lập / Đổi mật khẩu" cũng theo đó. Mục mua sắm (thông báo, voucher, Điểm, yêu thích, theo dõi) gắn ở nhóm H. Bỏ hộp "Mô phỏng cho buổi demo: kịch bản tài khoản" (AcctDemo / SCENARIOS — D5). |
| G10.45 | Miền `account.types` (Profile, LoginMethods, Device, OTP bước 2) + lược đồ zod hồ sơ / đổi email / mật khẩu (tài khoản chỉ Google không cần mật khẩu cũ) + test. Hạ tầng 3.2–3.7: thao tác 🔐 gửi header `X-OTP-Challenge-Id` + `X-OTP-Code` (0.7). Mock: OTP 123456, mật khẩu hiện tại Genie@123, tài khoản mẫu tạo bằng Google chưa có mật khẩu / SĐT / ngày sinh như prototype; đổi mật khẩu thu hồi các phiên khác như API. |
| G10.46–G10.47 | ProfilePanel: react-hook-form + zod; đổi ngôn ngữ lưu vào hồ sơ rồi `switchLocale` (cookie lang); ảnh đại diện tải qua `uploadFile('AVATAR')` (nén JPEG, ≤ 1 MB). EmailChangeModal: email mới → mã gửi tới email MỚI → xác nhận; tài khoản chưa có mật khẩu hiện gợi ý "thiết lập mật khẩu" thay vì mở hộp (GNE-AUTH-4095). Ô OTP bước 2 dùng chung `StepUpOtp` (email đã che, gửi lại sau 60 giây). |
| G10.48–G10.49 | `AddressPanel` (thêm / sửa trong hộp với `AddressForm`, xóa có xác nhận — địa chỉ mặc định không xóa được, đặt mặc định). `AddressModal`, `AddressForm`, `RegionPicker` đã làm ở nhóm C. |
| G10.50–G10.52 | Mật khẩu: kiểm mật khẩu hiện tại khi đang gõ (POST /me/password/verify, chờ 500 ms), thanh độ mạnh, OTP bước 2; lần thiết lập đầu tải lại trang sau thông báo để menu (island khác) đổi nhãn như prototype. Liên kết Google: thật chuyển sang OAuth `intent=link`, mock liên kết ngay; không gỡ được khi là cách đăng nhập duy nhất (GNE-AUTH-4093). Thiết bị: icon theo loại, đăng xuất từng thiết bị / tất cả thiết bị khác, phiên hiện tại không có nút. |
| G10.53 | Xóa tài khoản: POST /me/otp-challenges `ACCOUNT_DELETION` — còn đơn đang xử lý → hộp liệt kê mã đơn (GNE-USR-4091, mock đọc trạng thái đơn mock trong phiên); hết đơn → OTP → PUT /me/deletion-request → băng đỏ "ẩn danh vào …" + "Hủy yêu cầu xóa". Bỏ nút "Mô phỏng: đã hoàn tất các đơn" (D5). Đã thử thật: thiết lập mật khẩu (OTP) → thành công, xóa tài khoản bị chặn bởi 7 đơn đang xử lý. |
| G10.55 | `VouchersPanel` (promotions/ui, /account/vouchers): lưu bằng mã, tab theo loại có số đếm, thanh lượt đã dùng đổi màu theo %, xóa khỏi kho (DELETE /me/vouchers/{id}, mới thêm), chi tiết, lịch sử Hết hiệu lực / Đã dùng; "Dùng ngay" dẫn /cart. Danh sách chỉ gồm voucher đã lưu theo GET /me/vouchers (prototype hiện cả voucher chưa lưu với nút "Lưu"). Mock: mã đã lưu giữ trong phiên tab (trước chỉ trong bộ nhớ). |
| G10.56–G10.57 | `features/coins` đủ 4 tầng: lịch sử (3.13 transactions), quà đổi (XU_REWARDS → `CoinReward` / /public/coin-rewards), điểm danh 7 ngày, đổi quà; `coins.rules` + test. `CoinsPanel` ở /account/points: đếm ngược tới nửa đêm giờ VN sau khi điểm danh. Mock: số dư = mẫu − Điểm dùng khi đặt + Điểm đánh giá có ảnh + giao dịch trong phiên; voucher đổi từ Điểm hiện ngay trong Kho Voucher. Đã thử: điểm danh +100, đổi quà → DIEM5K1 đứng đầu kho. |
| G10.58 | `WishlistPanel` (product/ui) đọc store yêu thích dùng chung với nút tim; "Thêm vào giỏ" chọn tổ hợp phân loại đầu tiên còn hàng (lấy chi tiết sản phẩm), shop tạm nghỉ thì khóa. |
| G10.59 | `FollowsPanel` (shop/ui): con dấu shop, Chính hãng, đánh giá, số theo dõi, Vào shop, bỏ theo dõi có xác nhận; huy hiệu số shop ở menu tài khoản. |
| G10.60 | `NotificationsPanel` (notifications/ui): tab Tất cả / Đơn hàng / Khuyến mãi / Hệ thống / Chat, đánh dấu tất cả đã đọc, mở = đánh dấu đã đọc rồi đi tới đích; thông báo toàn sàn → hộp chi tiết; "Xem thêm" 5 mục / lần. Sửa liên kết thông báo đơn: route theo dõi đơn dùng id nên lấy số thứ tự từ mã TR (API nên trả kèm order_id — ghi G13.20). |
| G10.61 | Khóa `promotions.wallet.*`, `coins.*`, `notifications.page.*`, `product.wishlist.*`, `shop.follows.*` (vi + en). Khu tài khoản thêm `<h1>` (ẩn khỏi mắt, theo tên mục) trong `AccountShell` — trước đó /orders và /account/* chưa có h1 (R18). |
| G10.62 | `features/chat`: kiểu, quy tắc (+test), dto/mock (4 hội thoại theo prototype) / api 8.1–8.5. Mock: shop tự trả lời sau ~1,1 giây kèm trạng thái "đang nhập". Chưa có WebSocket (mục 13) → luồng tin hỏi lại 2 giây khi đang mở, hộp thư 15 giây; nối WS sau ở G13. Gửi tin có bong bóng "Đang gửi…" (client_message_id = Idempotency-Key), lỗi → "Gửi lỗi". |
| G10.63 | `ChatWidget` (client:only) gắn trong `StorefrontLayout` (tắt bằng `noChat`); khách bấm → LoginPrompt. Ảnh đại diện shop dùng `Seal` (con dấu shop chung toàn site) thay ô chữ cái của prototype. Ảnh gửi qua upload CHAT_IMAGE (nén trước); thẻ đơn chọn từ đơn với shop đó. |
| G10.64 | `/chat` cần đăng nhập, noindex; `?shop=&product=&order=` mở hội thoại và tự gửi thẻ sản phẩm / thẻ đơn rồi bỏ tham số khỏi URL. Màn hẹp: danh sách → luồng tin có nút quay lại. |
| G10.65 | `BecomeSeller` (seller-onboarding): form rhf + zod, điền sẵn họ tên / email / SĐT từ phiên, ngành hàng lấy danh mục gốc (SSR), loại hình INDIVIDUAL/HOUSEHOLD/BUSINESS → 9.1 → modal OTP → 9.2 → làm mới token → `/seller/kyc`. Tên shop trùng (GNE-VND-4094) báo ngay dưới ô. Mock bỏ kiểm GNE-VND-4093 (tài khoản mẫu vốn là chủ shop) để trình diễn được luồng. Điều khoản dẫn `/policies/seller-terms`, `/policies/commission-rates` (R16). |
| G10.66 | `/become-seller` công khai, lập chỉ mục; phần giới thiệu / lợi ích / 3 bước render SSR, chỉ thẻ đăng ký là island. Ở 360px thẻ lợi ích xếp 1 cột (prototype 2 cột chữ bị bó hẹp). Khóa `chat.*`, `seller-onboarding.landing.*` (vi + en). |
| G11.01–G11.02 | Nhánh `feature/seller-ui` tách từ `feature/web-buyer` (xếp chồng, chưa push). `app/seller/routes.tsx`: bảng route mục 4.3, mỗi màn `lazy`; màn chưa chuyển dùng tạm `Pending` (thay dần ở nhóm B–F); đường dẫn lạ → 404 nội bộ. `SellerApp`: `BrowserRouter basename="/seller"` (bật cờ v7), tải `GET /seller/me` trước — lỗi 403 (chưa có shop) → mời "Đăng ký mở Shop". |
| G11.03–G11.04 | Khung theo `VendorShell`: mục nav là `NavLink` thật, mục bị khóa là nút `aria-disabled` + toast. Màn hẹp: thanh bên thành dải trên, nav cuộn ngang (như responsive.css prototype). Bỏ bảng "Mô phỏng KYC" (D5). Chuông dùng lại `NotifPanel` (thêm `onSelect`, `seeAllHref`) + modal chi tiết có nút "Mở trang liên quan"; mock chọn bộ thông báo của shop khi đang ở `/seller`. Tìm nhanh debounce 250 ms, từ 2 ký tự (10.1b): bấm đơn → `/seller/orders?q=<mã>`, sản phẩm → form sửa. Ngôn ngữ dùng `<select>` như prototype. |
| G11.05 | `KycGate`: thông điệp riêng cho chưa nộp / đang chờ (gom PENDING · AI_PROCESSING · MANUAL_REVIEW) / bị từ chối / cần bổ sung; chỉ mở Hồ sơ KYC + Cài đặt (API 0.9). |
| G11.07 | Ngữ cảnh shop (10.1, 10.1b, tài khoản nhận tiền) đặt ở `features/seller-settings` (feature sở hữu thông tin shop) thay vì một feature mới. Kho mock dùng chung `shared/api/mock/seller.ts` giữ tên shop + trạng thái KYC: mở shop ở `/become-seller` đặt NOT_SUBMITTED → vào Seller Center thấy cổng KYC. |
| G11.09 | `seller-dashboard`: GET /seller/dashboard (10.2). Mock: hàng "Việc cần làm" + số sản phẩm đếm từ kho dùng chung `shared/api/mock/seller.ts` (thêm `sellerOrdersStore`, `sellerProductsStore` theo prototype) nên xác nhận đơn ở màn Đơn hàng sẽ đổi số ở Tổng quan và huy hiệu nav; tìm nhanh cũng đọc kho này. Đồng hồ mock đứng ở ngày chốt số liệu của bản mẫu (08/06/2026) để "hôm nay / hôm qua" và "còn 12 giờ" khớp prototype. |
| G11.10 | `OverviewPage` khớp ảnh `vendor-1440/360`. Doanh thu dùng `seller-analytics` (endpoint 10.2 revenue nằm dưới /seller/analytics). "Đơn hàng gần đây" dùng `useSellerOrders({ size: 5 })` của `seller-orders` (đã dựng sớm phần miền + danh sách: kiểu, huy hiệu, tab, mốc thời gian — nhóm C dùng tiếp). Ô việc / nút dẫn route SPA thật (`/orders?tab=NEW`, `/returns`, `/chat`, `/wallet?withdraw=1`). `KycBanner` có làm nhưng thực tế không hiện vì cổng KYC khóa Tổng quan khi chưa VERIFIED (API 0.9). |
| G11.11 | `StatsPage`: chọn Hôm nay / 7 ngày / 30 ngày / Tùy chọn ngày (chưa đủ ngày → lời nhắc, không giữ số cũ). Prototype không có nút xuất ở màn này; thêm "Xuất CSV" (chỉ số kỳ + kết quả đơn + top sản phẩm) theo plan, dùng `shared/lib/csv.ts` (+test). Tiêu đề khối dùng `<h2>` vì `<h1>` đã ở thanh trên (R18). Mock "Tùy chọn ngày" trả số 30 ngày như prototype. |
| G11.13–G11.14 | `seller-orders`: miền + danh sách đã dựng sớm ở nhóm B, nhóm này thêm `fulfillment.rules` (thao tác theo trạng thái = `allowed_actions`, lý do từ chối, chọn hãng, thu hộ COD), `fulfillment.schema` (zod), miền trả hàng `refund.*`; test chuyển trạng thái NEW → CONFIRMED → PREPARING → giao hãng và các nút trả hàng. Trạng thái vận đơn dùng bộ chuẩn hóa của API (`IN_TRANSIT`, `DELIVERY_FAILED`…) thay mã GHN thô của prototype (`ghnStatus`). Kho mock thêm `sellerRefundsStore`; mọi thao tác ghi kho chung rồi làm mới mọi truy vấn `['seller', …]` (Tổng quan, huy hiệu nav, tìm nhanh đổi theo — đã thử). |
| G11.15 | `FulfillmentPage`: tab là `Tabs` điều hướng route SPA (`/orders?tab=`; "Trả hàng" → `/returns`), mặc định "Tất cả" (prototype mở tab "Mới"; nút "Xem tất cả" ở Tổng quan cần tab Tất cả). `?q=` từ tìm nhanh / Tổng quan điền sẵn ô tìm. Có phân trang 10 đơn / trang. Màn hẹp: tab đang chọn tự cuộn vào khung nhìn. |
| G11.16–G11.17 | Từ chối: react-hook-form + zod, "Lý do khác" bắt buộc ghi chú (nút khóa tới khi nhập). Bàn giao ĐVVC: báo giá từ API (10.3), hãng được chọn đánh dấu, gửi kèm Idempotency-Key, xong mở ngay Phiếu gửi hàng; "In" dùng `window.print()` như prototype (PDF A6 của hãng để G13). |
| G11.18 | `ReturnsPage`: lý do trả hàng dùng lại khóa `refunds.reason.*` của phía khách; ảnh bằng chứng là liên kết mở ảnh gốc; "Yêu cầu bổ sung" chỉ 1 lần (ẩn sau khi dùng, mock trả GNE-SUP-4091 nếu gọi lại). Thêm hộp xác nhận cho "Đã nhận hàng hoàn" / "Nhờ Sàn phân xử" (prototype bấm là đổi ngay). |
| G11.20 | `seller-products` miền: kiểu theo 10.5 (`$ProductInput`), quy tắc ma trận phân loại (`combos` / `syncVariants` giữ giá-tồn-SKU khi sửa nhóm), giới hạn (8 ảnh, 2 nhóm, 50 biến thể, 4 giấy tờ, mô tả 10.000 ký tự, đặt trước 7–30 ngày), `missingSpecs`, `publishIssues`, `requiresReview` (chỉ đổi giá / tồn thì không duyệt lại); zod cho các ô nhập tay + form đề xuất danh mục; 9 test. Thông số bắt buộc / gợi ý và "bắt buộc giấy tờ" lấy từ luật ngành (10.5 listing-rules) thay vì hằng số trong UI như prototype. |
| G11.21 | Hạ tầng đủ 10.5–10.6 (danh sách có `summary.counts`, chi tiết, lưu nháp / gửi duyệt, ẩn / hiện, xóa, luật ngành, thương hiệu tìm + thêm mới, cây danh mục, đề xuất). Mock: hàng danh sách ở kho dùng chung `sellerProductsStore`, chi tiết form ở kho riêng; gửi duyệt khi shop chưa VERIFIED → GNE-VND-4031, thiếu giấy tờ / thông số → GNE-CAT-4222 / 4223; xóa sản phẩm còn đơn đang xử lý → GNE-CAT-4094 (đã thử với "Tai nghe ANC"). Lớp tải tệp thêm mục đích sản phẩm / shop / KYC theo API 4.1: ảnh sản phẩm nén WebP, giấy tờ sản phẩm + KYC giữ nguyên tệp; `UploadedFile.mediaType` thêm `DOCUMENT`. |
| G11.22 | `ProductsPage`: tab trạng thái là route `?tab=` có số đếm, thêm ô tìm theo tên (prototype không có), tên sản phẩm là liên kết tới form, Xóa có hộp xác nhận. |
| G11.23–G11.30 | `ProductEditor` (react-hook-form giữ cả 7 khối): `/products/new`, `/products/:id/edit`; chờ duyệt → chỉ xem; bị từ chối → hiện lý do; rời trang khi đang sửa → hỏi lại (cả `beforeunload`); lỗi gửi duyệt viền đỏ khối + cuộn tới khối đầu tiên. Ảnh: kéo thả + phím ← → / Delete. Mô tả: `contentEditable` như prototype; chèn liên kết dùng hộp nhập URL (thay `window.prompt`), chèn ảnh qua upload DESCRIPTION_IMAGE. Phân loại: ảnh theo giá trị qua VARIANT_IMAGE. Vận chuyển: "Gửi hàng từ" chỉ hiển thị (API lấy từ địa chỉ lấy hàng, không nhận từ client) — giá trị tạm cố định tới khi có Cài đặt (G11.43). |
| G11.31 | `VendorCatalogPage`: cây 2 cấp + đề xuất; form đề xuất rhf + zod, trùng tên báo ngay dưới ô (GNE-CAT-4091), đủ 5 đề xuất chờ → GNE-CAT-4221. Kiểm thử e2e trên server dev riêng cổng 4330 vì server 4321 đang chạy giữ bản tối ưu dependency cũ (Vite 504 khi tải `browser-image-compression`). |
| G11.33 | `seller-wallet`: miền (kiểm rút tiền tối thiểu / vượt số dư / chưa có tài khoản, huy hiệu loại + trạng thái theo API 10.10 — thêm `PAYOUT_DONE` thay cho đoán từ chữ "chờ duyệt" như prototype, lọc sổ) + hạ tầng (ví, sổ giao dịch có `available_months`, rút tiền 🔁 🔐). Số dư khả dụng / đang rút đưa vào kho mock dùng chung `sellerMoneyStore` để Tổng quan đổi theo sau khi rút (đã thử). |
| G11.34 | `WalletPage` khớp bố cục prototype; ngày giờ theo `formatDateTime`; Xuất CSV tạo phía client từ dòng đang xem (cột đúng API export, tên `so-giao-dich-vi-06-2026.csv`); `?withdraw=1` từ nút "Rút tiền" ở Tổng quan mở ngay hộp rút. |
| G11.35 | `WithdrawModal`: OTP dùng lại `StepUpOtp` + `useRequestStepUp` của feature account (xuất qua `account/index.ts`; `StepUpPurpose` thêm `WITHDRAWAL`, `PAYOUT_ACCOUNT_UPDATE`, `PAYOUT_ACCOUNT_DELETE`) thay ô nhập 6 số tự làm của prototype; có gửi lại mã. Chưa có tài khoản nhận → liên kết `/settings#payout`. |
| G11.36–G11.37 | KYC trong `seller-onboarding`: miền (giấy tờ bắt buộc theo loại hình, gom PENDING/AI_PROCESSING/MANUAL_REVIEW, chỉ sửa khi chưa nộp / từ chối / cần bổ sung, kiểm tệp PDF/JPG/PNG ≤ 10 MB, ảnh ≥ 20 KB; zod CCCD 12 số + MST 10 số / 10-3 số); hạ tầng 10.12 (nháp, nộp chỉ gửi giấy tờ mới — giữ bản cũ, yêu cầu nhân viên xem xét). Tệp KYC tải bằng mục đích `KYC_DOCUMENT` — không nén. Nộp xong kho shop chuyển PENDING → khung đổi chip "KYC đang chờ", khóa các mục (đã thử từ shop mới mở). Bỏ bảng "Mô phỏng trạng thái KYC" (D5). Tài khoản nhận tiền (xác minh tên + OTP) theo API thuộc Cài đặt → làm ở G11.43 thay vì trong trang KYC như mô tả plan. |
| G11.38 | `CameraModal`: `getUserMedia` (camera trước, xem trước lật gương), 3 bước hướng dẫn người thật rồi Chụp → JPEG → tải như ảnh selfie; bị chặn quyền / không có camera → báo lỗi cụ thể, gợi ý tải ảnh lên (đã thử nhánh bị chặn). Nút "Mô phỏng: đã làm xong" của prototype đổi thành "Đã làm xong" (người dùng tự xác nhận; liveness thật do AI Worker chấm). |
| G11.40 | `seller-promotions`: voucher shop (danh sách + summary 3 chỉ số, tạo / sửa / nhân bản / tắt có xác nhận) theo 10.7; `VoucherFormModal` rhf + zod (mã 6–12 A-Z0-9, % ≤ 100, số lượng, ngày, phạm vi danh mục / sản phẩm + ngoại trừ) — xuất qua index để G12 (voucher sàn) dùng lại. Trùng mã báo dưới ô (GNE-VCH-4092). Đăng ký Flash Sale: sản phẩm đủ điều kiện, giá ≤ 85% giá 30 ngày, suất 1..tồn, danh sách chờ duyệt; `/seller/flash-sales` mở sẵn hộp đăng ký. 3 ô chỉ số dùng `StatTile` như `MiniStat` của prototype. |
| G11.41 | `SellerChatPage` trong `features/chat`: API `/seller/conversations/**` (bên kia là khách), mock 3 hội thoại prototype; `Bubble` tách ra dùng chung với phía khách (truyền ảnh đại diện + đường dẫn thẻ đơn). Gửi thẻ đơn lấy từ đơn của chính khách đó (`useSellerOrders`). Số tin chưa đọc vào kho dùng chung `sellerChatUnreadStore` → huy hiệu nav + Tổng quan giảm khi đọc (đã thử 5 → 3). Hỏi lại 2 giây thay WebSocket như phía khách. |
| G11.42 | `seller-reviews`: điểm TB + phân bố, 3 tab có số đếm, phản hồi ≤ 500 ký tự chặn số điện thoại (GNE-SOC-4221), báo cáo dùng lại `reportReview` + lý do của feature reviews (hàm gửi truyền từ UI vào hook để tầng application không phụ thuộc feature khác). |
| G11.43 | `SettingsPage` (seller-settings): ảnh bìa / logo (SHOP_COVER ≤ 2 MB, SHOP_LOGO ≤ 1 MB), tên đổi 1 lần / 30 ngày (GNE-VND-4091, đã thử), 5 hãng — chặn tắt hãng cuối, freeship; địa chỉ lấy hàng dùng `RegionPicker` của khu tài khoản → "Lưu và Đồng bộ với GHN"; tài khoản nhận tiền: xác minh tên khớp CCCD (GNE-WAL-4222) → OTP `PAYOUT_ACCOUNT_UPDATE` (dùng `StepUpOtp`), xóa có OTP `PAYOUT_ACCOUNT_DELETE`; tự trả lời + "Lưu tin nhắn"; chế độ nghỉ ≤ 30 ngày, còn đơn chưa bàn giao → liệt kê mã đơn (từ kho đơn chung). Bỏ nút "Mô phỏng: đã bàn giao hết" (D5). Sửa lỗi `Switch` dùng chung: khi không có chữ đi kèm vỏ là `<span>` nên bấm không bật / tắt được → luôn là `<label>`. |
| G11.44 | `SecuritySection`: chip cách đăng nhập (email/mật khẩu, Google) + liên kết quản lý; form mật khẩu dùng lại `PasswordForm` của khu tài khoản (xuất qua index) vì shop chạy trên tài khoản mua hàng của chủ shop. |
| G12.08 | `admin-dashboard`: miền + mock theo API 11.2 (`/admin/dashboard/finance`, `gmv-chart` 7d / weekly / 6m, `operations`, `growing-shops`), test lợi nhuận ròng 37.432.500₫ + nhãn trục. `DashboardPage` khớp ảnh `admin-1440/360`. "Tiền đang chờ xử lý" tính từ kho mock dùng chung (lệnh rút, phiên COD, tranh chấp) nên đổi theo các màn xử lý. Ngành hàng của "Shop tăng trưởng nhanh" lấy danh mục gốc của `catalog` (lọc theo `category_id`). Xuất CSV tạo phía client từ số đang xem (như Seller G11.11); API có `/admin/dashboard/export` — dùng khi nối backend (G13.20). Thêm `getPage()` ở `shared/api/http.ts` (bóc phong bì phân trang số + `summary`) và `TablePager` ở `shared/ui` (chân bảng "Hiển thị 1 - 5 trong tổng số 7 …") dùng cho các bảng Admin. |
| G12.09 | `admin-kyc`: kiểu theo 11.3, quy tắc (tab Chờ duyệt gom AI_PROCESSING / MANUAL_REVIEW, rủi ro theo điểm, thao tác theo trạng thái, hoa hồng riêng 0–50%), zod từ chối (Lý do khác bắt buộc ghi chú) / bổ sung (≥ 1 giấy tờ) + test. Mock: trạng thái ở `adminKycStore` (huy hiệu nav, Tổng quan đổi theo — đã thử duyệt 3 → 2), chi tiết ở kho riêng. Ảnh giấy tờ mock là hình minh hoạ SVG `public/img/mock/kyc-*.svg` (không phải giấy tờ thật). |
| G12.10 | `KycReviewPage`: tab (`?tab=`) + hồ sơ đang xem (`?id=`) trên URL; lý do từ chối / yêu cầu bổ sung đã gửi hiện ngay trong chi tiết. Màn hẹp: 2 cột → 1 cột, ảnh giấy tờ 2 cột. |
| G12.11 | `DocViewer` (portal, giữ focus, Esc đóng, khóa cuộn): phóng to / thu nhỏ / xoay / lật, chặn kéo thả + chuột phải (URL ký sẵn 5 phút). Số CCCD chỉ hiện dạng server đã che ở "Dữ liệu kê khai" — không che trên ảnh vì vị trí số trên ảnh thật do AI Worker xác định. |
| G12.12 | Phê duyệt (gửi hoa hồng riêng nếu nhập), Từ chối (mã lý do 16.4 + ghi chú), Yêu cầu bổ sung (giấy tờ theo `KycDocumentType`, hạn 7 ngày), Đưa lại vào hàng chờ — mỗi thao tác làm mới mọi truy vấn `['admin', …]`. |
| G12.14–G12.15 | `admin-disputes`: kiểu theo 11.4, quy tắc (trạng thái sau phán quyết, thao tác theo trạng thái + phong tỏa / gỡ, khối hoàn tiền VNPay đồng bộ / thử lại hay chuyển khoản tay, nội dung `TRRF…`), zod biên bản + lý do phong tỏa + test. Mock ghi kho dùng chung `adminDisputesStore`: phán quyết hoàn tiền tạo lệnh hoàn VNPay (đồng bộ → SUCCESS) hoặc chuyển khoản tay tới tài khoản khách cho đơn VietQR / COD; huy hiệu nav + "Tiền chờ hoàn" ở Tổng quan đổi theo (đã thử RFN-0231: 2 → 1, hiện thẻ "Chờ chuyển khoản thủ công" TRRF0231). "Thử lại hoàn tiền" gửi Idempotency-Key. "Mở lại" chỉ cho tranh chấp đã bác (prototype cho cả đã hoàn — tiền đã rời escrow nên không mở lại). Video bằng chứng mock là liên kết tên tệp (chưa có tệp thật). |
| G12.16 | `ReviewModerationPage`: thêm tab Chờ xử lý / Đã ẩn / Đã xử lý theo `state` của API 11.5 (prototype gộp một danh sách); tiêu cực mạnh (PhoBERT ≥ 0,85) lên đầu rồi theo số báo cáo; "Ẩn" gửi lý do = cờ báo cáo chính. |
| G12.17–G12.18 | `admin-moderation` sản phẩm: kiểu / quy tắc theo 11.12 (tab, nhãn "Chờ duyệt lại", FLAGGED lên đầu, thanh quyết định theo trạng thái, màu điểm rủi ro, chia đều hàng chờ cho người đang nhận việc) + test. Danh sách `/admin/product-moderation` (?tab=, tìm, giao lại người duyệt, Cấu hình nhân viên: bật / tắt nhận việc, Lưu, Phân phối lại có xác nhận); chi tiết thành route riêng `/admin/product-moderation/:id` (prototype là màn con). Mô tả người bán chỉ hiện chữ (bỏ thẻ HTML) để không chạy HTML lạ trong trang quản trị; tài liệu pháp lý mock là SVG minh hoạ `public/img/mock/product-doc.svg`. Khung Admin: màn hẹp tự cuộn mục nav đang mở vào khung nhìn. |
| G12.20 | `admin-catalog`: kiểu / quy tắc / zod theo 11.11 (tách giá trị thuộc tính, chỉ xóa danh mục rỗng, danh mục cha gốc + cấp 2 — cây 3 cấp, tab + thao tác sản phẩm, trùng mã thuộc tính, slug, hoa hồng 0–50%) + test. `slugify` đã có sẵn ở `shared/lib/slug.ts` từ G07 nên không tạo lại. Mock theo cây TREE / LEAVES của prototype, mã danh mục gốc trùng danh mục storefront (Điện tử = 1…); lỗi như server: trùng slug GNE-CAT-4091, còn sản phẩm GNE-CAT-4092, trùng mã thuộc tính GNE-CAT-4095 (đã thử). |
| G12.21 | `CatalogPage`: danh mục gốc đang xem trên URL (`?id=`); công tắc "Hiển thị" gọi PATCH `is_active` ngay. "Bộ thuộc tính" và "Xem SP" là route thật `/admin/catalog/:id/attributes`, `/admin/catalog/sub/:id/products` (prototype là màn con trong state). Sản phẩm theo danh mục con: duyệt / từ chối dùng endpoint 11.12, khóa / mở khóa 11.11; mock dùng cùng 4 sản phẩm cho mọi danh mục con như prototype; lý do khóa / từ chối dùng bộ mã 16.4 (nhãn lấy chung khóa `admin-moderation.product.reasons.*`). |
| G12.22 | `CatModal` rhf + zod: slug tự sinh khi thêm, nút Auto, danh mục cha chỉ chọn khi thêm (sửa chỉ đổi thông tin danh mục gốc như prototype), Xóa khi rỗng có hộp xác nhận. |
| G12.23 | `AttributePanel`: thêm thuộc tính bắt buộc → toast "Shop có 14 ngày bổ sung" theo `grace_period_days` của API (mock trả 14 khi thuộc tính mới / vừa chuyển sang bắt buộc); mã không sửa được sau khi tạo (khóa trong URL PUT). |
| G12.24 | Đề xuất danh mục: Duyệt (tạo danh mục dưới danh mục cha, hoa hồng theo cha) / Từ chối bắt buộc lý do (zod). Đã thử duyệt "Thú cưng (Pet)" → 7 danh mục gốc, chờ duyệt 2 → 1. Kho đề xuất của Seller (G11.31) là mock riêng, chưa nối với kho này — khi có backend cùng đọc `category_proposals`. |
| G12.25 | `admin-finance`: kiểu theo 11.6–11.9, quy tắc (lý do từ chối rút 16.4, nội dung `TRWD…`, trạng thái hiển thị lệnh rút, 3 chỉ số 8.150.000₫ / 1.200.000₫ khớp prototype, tỷ lệ 0–50%, màu sự kiện sổ cái) + test. Lệnh rút / phiên COD ghi kho dùng chung (huy hiệu nav, "Tiền đang chờ xử lý" ở Tổng quan); sổ cái + hoa hồng ở mock riêng; duyệt chi xong ghi bút toán `PAYOUT_SETTLE`, đối soát xong ghi `ESCROW_IN` cho từng đơn khớp (đã thử: duyệt PAYOUT-0012 → huy hiệu 4 → 3, sổ cái có dòng #100483). |
| G12.26 | `WithdrawalsPage`: Duyệt chi có hộp xác nhận → hộp chuyển khoản (QR mô phỏng dùng `QrPlaceholder`, tài khoản đầy đủ chỉ hiện sau duyệt theo API 11.6); "Xác nhận tay" bằng mã giao dịch ngân hàng. Nút "Mô phỏng: SePay báo tiền đã ra" (D5) đổi thành "Đã chuyển khoản" — thật thì webhook SePay tự hoàn tất. Lý do "Lý do khác" bắt buộc ghi chú. |
| G12.27 | `CommissionPage`: tỷ lệ nhận số thập phân, ô sai (ngoài 0–50%) viền đỏ + khóa nút Lưu; ngành tắt công tắc hiện "Đang dùng mặc định x%". |
| G12.28 | `LedgerPage`: tìm (debounce 300 ms) + lọc sự kiện theo enum 11.9, phân trang con trỏ 50 dòng với "Tải thêm" (`useInfiniteQuery`); Xuất sổ cái tạo CSV phía client từ mọi dòng khớp bộ lọc (API `/ledger-entries/export` dùng khi nối backend — G13.20). |
| G12.29 | `domain/cod.rules.ts`: `parseCodCsv` (bỏ BOM, tiêu đề, dòng trống; trả số dòng sai định dạng), `matchLine`, `reconcile` (cân = tổng tiền − phí = số SePay đã nhận), `canConfirm` + test 2 phiên mẫu (GHN cân 2 dòng khớp; GHTK cân nhưng có dòng lệch / không tìm thấy). |
| G12.30 | `CodReconciliationPage`: phiên đang xem trên URL (`?id=`); tải CSV (≤ 1 MB, chỉ .csv) đọc ở client rồi gọi preview 11.7, xác nhận gửi Idempotency-Key; phiên đã xác nhận hiện lại kết quả đã lưu. |
| G12.32 | `admin-marketing`: kiểu theo 11.14–11.17, quy tắc (tab + thao tác voucher theo trạng thái, % lượt dùng; % giảm Flash + luật ≥ 15%, Lên lịch cần ≥ 1 SP duyệt; vị trí banner, đọc liên kết đích theo route mới, `sitePath` chặn liên kết ngoài miền / `//` / `/\` / `javascript:`, thứ tự kế tiếp theo ô, CTR, trạng thái; tách email, chữ thuần, tỷ lệ đọc) + zod Flash Sale / banner / thông báo + 10 test. `genCode` dùng chung của seller-promotions. |
| G12.33 | `VoucherFormModal` (G11.40) tách thành `VoucherEditor` dùng chung: nơi gọi cấp dữ liệu + hàm lưu; chế độ `platform` thêm tên chương trình (bắt buộc), nhà tài trợ Genie / Đồng tài trợ + tỷ lệ sàn tài trợ, phạm vi chỉ Toàn sàn / Danh mục, ngày bắt đầu không ở quá khứ khi tạo (`voucherSchemaFor`, test cả 2 chế độ). Seller vẫn dùng `VoucherFormModal` như cũ. Trùng mã → báo dưới ô (đã thử GENIE50); Xoá vĩnh viễn chỉ khi kết thúc và chưa dùng (GNE-VCH-4093). Tổng giảm giá / ngân sách còn lại lấy từ `summary` (mock giữ số prototype). |
| G12.34 | `FlashSalePage`: chiến dịch đang xem trên URL (`?id=`), tạo chiến dịch rhf + zod (hạn đăng ký phải trước ngày mở), bỏ "Mô phỏng: đến giờ mở bán" (D5 — cron server mở / đóng). |
| G12.35 | Miền banner nằm ở `domain/marketing.rules.ts` (cùng các quy tắc marketing khác) thay vì file `banner.rules.ts` riêng; đích chuyển sang route mới (/c/…, /p/…-id, /shop/id, /#flash-sale, /discover, /campaign/…). Bộ banner mẫu chuyển sang kho chung `shared/api/mock/banners.ts` — storefront (promotions) và Admin cùng đọc; Admin sửa giữ trong phiên tab, storefront SSR vẫn đọc bộ gốc (khi có backend cùng một bảng). |
| G12.36 | `BannersPage` + `BannerModal`: ảnh tải qua mục đích mới `BANNER_IMAGE` (thêm vào `upload.rules` cùng `NOTIFICATION_IMAGE` theo API 4.1, nén WebP ≤ 5 MB); ô chọn đích dùng danh mục (`catalog`), sản phẩm (`product.listProducts` + lọc shop), cửa hàng (`shop.listShops` — API thật là `/admin/shops`, ghi G13.20); đủ 8 banner đang hiển thị → lỗi dưới ô Vị trí (GNE-BNR-4221). Cột thao tác dính phải để không bị đẩy khỏi khung khi bảng cuộn ngang. |
| G12.37 | `PlatformNotificationsPage`: `RichBox` (In đậm / In nghiêng / Liên kết qua hộp nhập URL thay `window.prompt`), `sanitizeHtml` chỉ giữ thẻ ô soạn tạo ra và href http/https — dùng khi lưu và khi hiển thị chi tiết; email người nhận cụ thể kiểm định dạng + email không tồn tại báo dưới ô (GNE-NOT-4221); chỉ sửa được khi còn hẹn giờ (GNE-NOT-4091). |
| G12.39 | `admin-system`: kiểu / quy tắc theo 11.10 + 11.13 (chức danh nạp bộ quyền mặc định, chỉnh khác → Tùy chỉnh, không cấp MANAGE_RBAC cho nhân viên, thao tác theo trạng thái, tab + tìm người dùng theo SĐT có khoảng trắng, hạn đình chỉ 7 / 30 ngày / vô thời hạn, khoảng thời gian nhật ký theo giờ VN, màu hành động), zod mời (email `@genie.example`) / khóa / đình chỉ (Lý do khác bắt buộc mô tả) / mở khóa / tên + test. Mock nhân viên, người dùng, nhật ký theo prototype; nhật ký mẫu kết thúc 22/06/2026 nên "Hôm nay / 7 / 30 ngày" tính từ ngày đó. |
| G12.40–G12.41 | `StaffPage`: mã quyền di chuột xem mô tả; Gửi lại lời mời / Sửa quyền / Khóa (lý do) / Mở khóa / Xóa (hộp xác nhận) theo trạng thái; tài khoản đang đăng nhập không tự thao tác. `RbacEditor` cảnh báo tách biệt nhiệm vụ (PROCESS_PAYOUT + UPDATE_COMMISSION) theo `warnings` của API — vẫn lưu được. Sửa lỗi tràn ngang ở 360: nhãn `sr-only` trong `th` thoát khung cuộn (thêm `relative` cho khung bảng). |
| G12.42 | `UsersPage`: tab có số đếm, tìm theo email / SĐT / tên shop; khóa tài khoản = đình chỉ theo mã lý do + thời hạn (phiên đăng nhập bị thu hồi), mở khóa bắt buộc lý do. Khóa nhân viên chỉ ở Phân quyền nhân viên (có liên kết). |
| G12.43 | `AuditLogPage`: lọc khoảng thời gian (có Tuỳ chọn từ / đến), loại hành động, tìm; phân trang con trỏ "Tải thêm"; ô "n lần truy cập bị từ chối" đếm theo bộ lọc (`denied_count` của API); Xuất nhật ký tạo CSV phía client từ toàn bộ kết quả lọc (API có export — dùng khi nối backend G13). Chỉ đọc, không có thao tác sửa / xóa. |
| G12.44 | `AccountAdminPage`: ảnh đại diện (upload AVATAR) + sửa tên qua PATCH `/admin/me` (khung Admin đổi theo); đổi mật khẩu và thiết bị đăng nhập dùng lại `PasswordForm`, `DevicesPanel` của khu tài khoản (thêm prop `passwordHref`). Ghi chú "Bắt buộc OTP" đặt trên form (form đã có khối Mẹo bảo mật riêng). `PanelHead` (shared/ui) + dòng thiết bị tự xuống hàng khi hẹp — trang tài khoản khách ở 1440 không đổi. |
| Nhánh (G13) | `feature/web-polish` tạo từ `feature/admin-ui` (xếp chồng, local, chưa push). |
| G13.01 | Quét bằng script (bỏ chú thích, `*mock*`, `*.test.*`, `policies.data.ts`): chuyển sang `t()` placeholder tên người (`common.placeholder.person_name` — 3 form), placeholder địa chỉ, 4 nhãn phiếu voucher trang chủ (`home.vouchers.*`, số tiền qua `formatVND`). Tên ngôn ngữ "Tiếng Việt" / "English" giữ nguyên dạng tự xưng (không dịch) nhưng gom về `LOCALE_LABELS` ở `shared/lib/locale.ts` (4 ô chọn ngôn ngữ). Khóa thông số "Thương hiệu" là dữ liệu người bán → hằng `BRAND_SPEC_KEY`. **Giữ lại có chủ đích:** thư viện `/dev/ui` (146 chuỗi mẫu, chỉ chạy dev, build production 404) và dữ liệu mock nằm trong `*.api.ts` (chat, ví, đánh giá… — sẽ do backend trả, G13.20); tên nhóm phân loại gợi ý của form sản phẩm (`seller-products/domain/product.rules.ts`) là dữ liệu người bán lưu thẳng vào sản phẩm. |
| G13.02 | `en.json` đủ 3.246 khóa, `check-i18n` xanh; quét thêm mọi `t('…')` dạng chuỗi trong mã → 0 khóa thiếu. 8 giá trị en còn dấu tiếng Việt đều là tên riêng (Hội An, Đông Hồ, Đông Sơn, GHN — Giao Hàng Nhanh…). |
| G13.03 | Không còn "Xu", "Mall", `LZD-`, `Lazadee`, `lz_` trong mã chạy và khóa lưu trữ; chỉ còn trong chú thích ghi nguồn prototype và `normalizeCode()` (đổi `LZD-` → `GNE-` phòng dữ liệu cũ, có test). "Xu hướng" (`search.trending`) là từ thường, không phải đơn vị Xu. |
| G13.04 | Thêm `isoDateVN()` (ngày giờ VN "YYYY-MM-DD") và `formatMonthShort()` ("T6" / "Jun") vào `format.ts` + test; thay 3 bản `Intl.DateTimeFormat('en-CA')` tự viết (marketing, admin-system, VoucherFormModal), 2 bản nhãn tháng tự viết (admin-dashboard, seller-analytics), 2 `toLocaleString` trong mock voucher đổi từ Điểm. Không còn nối "₫"/"đ" ngoài `format.ts` (hậu tố `suffix="₫"` của ô nhập là ký hiệu đơn vị, không phải định dạng số). |
| G13.05 | Chụp toàn trang bản build (mock) ở 360 / 768 / 1440 cho 16 màn có ảnh tham chiếu bằng Playwright rồi ghép cạnh ảnh prototype để so. Bố cục, thứ tự khối, số liệu khớp ở trang chủ, danh mục, tìm kiếm, khám phá, sản phẩm, shop, chính sách, trở thành người bán, giỏ, thanh toán (vào từ giỏ → "Mua hàng"), đơn mua, tài khoản, đăng nhập / đăng ký. Kiểm tự động mọi trang: không tràn ngang, đúng 1 `<h1>`, không lỗi JS. Ảnh danh mục prototype chụp "Điện thoại" rỗng nên chiều cao khác; ảnh gốc chụp khi đã đăng nhập + popup mở. Lệch tìm được ghi ở bảng "Lệch giao diện". |
| G13.06 | Tổng quan Seller / Admin khớp ảnh `vendor-*`, `admin-*` (360 / 768 / 1440). Quét tự động **36 route** mục 4.3 + 4.4 ở 360 / 1024 / 1440 (108 lượt): chỉ `/seller/products` tràn ngang (đã sửa); xem lưới ảnh 1024 của 9 màn đại diện (đơn hàng, thêm sản phẩm, ví, cài đặt, duyệt KYC, rút tiền, danh mục, nhân viên, banner) — bảng rộng cuộn trong khung, không vỡ bố cục. |
| G13.07 | Quét toàn bộ trang storefront + 36 route SPA ở 1440 và 360 (nút / liên kết không tên, ô nhập không nhãn, ảnh thiếu `alt`, id trùng, Tab 12 lần đo vòng focus): sửa liên kết ảnh bằng chứng ở `/admin/refunds` thiếu tên (`admin-disputes.evidence_image`); ô tìm kiếm header có vòng focus ở khung cha (`:focus-within`). Lighthouse báo thêm và đã sửa: `aria-label` trên `div` không role (logo footer → `role="img"`), chấm chuyển popup banner 10 px → nút 24 px (WCAG 2.5.8, chấm vẽ bằng `::after`), nhãn nút ngôn ngữ lệch chữ hiển thị (2.5.3 → chữ ẩn "Ngôn ngữ:" trong nút), ô số lượng `aria-valuemin/max` trên textbox → `role="spinbutton"` + `aria-valuenow`. Tương phản 9 theme × 6 trang (chủ, sản phẩm, giỏ, đăng nhập, Seller, Admin): nhãn % trắng trên thanh tỷ lệ Seller / Admin 4,42:1 → đoạn `ok` / `bad` / `ship` dùng `--tone-ink`; phiếu freeship theme polymer `#0E8C8C` 4,08:1 → `#0C7F7F` (≈ 4,8:1); các báo ~1,0 ở theme saigon / sleeve / dongho là báo nhầm (nền vẽ bằng lớp khác — đã chụp kiểm). `prefers-reduced-motion`: CSS đã có từ G02; thêm cho `scrollIntoView` của form sản phẩm. Luồng mua bằng bàn phím kiểm ở e2e G13.12–G13.13. |
| G13.08 | Đối chiếu 63 chỉ thị với bảng TS 5.1: đúng ở hầu hết (Header / BuyBox / trang giỏ, checkout, đơn, tài khoản, form auth `load`; nút yêu thích / theo dõi / chia sẻ / bộ lọc `idle`; đánh giá, lưới sản phẩm, banner dưới màn `visible`; Seller / Admin / chat `only`). Chuyển 2 island sang `client:idle`: `AuthTools` (theme + ngôn ngữ góc trang đăng nhập) và `Countdown` trang Flash Sale (SSR đã in giờ, trang chủ cũng dùng `idle`). |
| G13.09 | `seo.spec.ts` thêm kịch bản `noindex, nofollow` cho `/auth/*`, `/cart`, `/checkout/result`, `/dev/ui` và (có cookie đăng nhập) `/seller`, `/admin`, `/orders`, `/account/*`, `/chat`, `/checkout`: **18/18 xanh** (360 + 1440). Lighthouse 12.8 (Chromium của Playwright) trên bản build mock **sau proxy gzip cục bộ** (server Node của Astro không nén — production nén ở reverse proxy; đo thẳng không nén: hiệu năng mobile 57–58, LCP 11–13 s). Điểm (hiệu năng / trợ năng / thực hành tốt / SEO): **trang chủ** mobile 65 / 100 / 100 / 100 (LCP 8,0 s — là ảnh popup banner lần đầu, render sau hydrate), desktop 91 / 100 / 100 / 100; **danh mục** mobile 66 / 100 / 100 / 100, desktop 94 / 100 / 100 / 100; **sản phẩm** mobile 67 / 100 / 100 / 100, desktop 95 / 100 / 100 / 100. Đã sửa: banner phụ đầu trang + ảnh popup không còn `lazy`. Việc còn lại (ghi cho giai đoạn sau): (1) chunk chung 267 KB (88 KB gzip) chứa **cả hai** từ điển vi + en — tách theo ngôn ngữ cần `t()` bất đồng bộ; (2) CSS chặn render 186 KB (34 KB gzip, gồm themes.css của 9 theme) — tách CSS theme theo `data-theme`; (3) banner 1600 px JPEG cho mọi khổ — cần srcset / WebP từ pipeline ảnh MinIO. |
| G13.10 | Không còn `/* eslint-disable */` toàn tệp ở 3 tệp sinh tự động (`seed.ts`, `voucher.mock.ts`, `policies.data.ts`) — bỏ đi vẫn sạch lint nên luật boundaries áp cho cả 3. Còn 17 dòng `eslint-disable-next-line react-hooks/exhaustive-deps` có chủ đích (chạy hiệu ứng 1 lần khi mở hộp / đổi khóa), không có cho boundaries. Không có `any` (luật `no-explicit-any: error` cả mã lẫn test). Override nới boundaries chỉ cho test / tệp cấu hình (G01.08). |
| G13.11 | Rà theo danh sách: cả 34 feature có `domain/` đều đã có test; rà sâu từng tệp `domain/*` (trừ `*.types.ts`) chưa được test nào import → thêm 3 tệp: `payment/domain/escrow.test.ts` (Money Rail), `account/domain/account.menu.test.ts` (mục /account, 404 mục lạ), `admin-moderation/domain/moderation.schema.test.ts` (lý do từ chối, ghi chú ≤ 500). Vitest: 64 tệp / 313 test. |
| G13.12–G13.16 (hạ tầng) | E2E mặc định chạy trên **bản build production** (`astro build` → `node dist/server/entry.mjs`, cổng 4322) thay vì `astro dev`: dev server phục vụ hàng trăm module ESM rời làm Chromium báo `ERR_INSUFFICIENT_RESOURCES` khi 2 trình duyệt chạy song song (island giỏ / thanh toán không hydrate được — không phải lỗi sản phẩm). `E2E_DEV=1` dùng lại dev server, `E2E_PORT` đổi cổng (cổng 4321 đang có phiên dev khác). Giới hạn 2 worker (nhiều hơn làm Chromium crash 0xC0000409 trên máy Windows). Bộ chọn tiêu đề dùng `#main h1` (thanh dev toolbar của Astro có `<h1>` riêng). **Toàn bộ 44 kịch bản xanh** (22 × mobile-360 / desktop-1440). |
| G13.12 | `guest.spec.ts`: thao tác bằng **bàn phím** (focus + Enter / Space): ô tìm kiếm header → kết quả → mở sản phẩm → chọn phân loại (kiểm `aria-pressed`) → Thêm vào giỏ → số trên icon giỏ → /cart bỏ chọn / chọn lại bằng Space → "Mua hàng (1)" → hộp "Vui lòng đăng nhập" (liên kết `next=/cart`), Esc đóng; trang cần đăng nhập chuyển `/auth/login?next=`. |
| G13.13 | `checkout-vnpay.spec.ts` (thay stub `test.skip`): đăng nhập bằng form → giỏ ≥ 3 shop, voucher shop ở từng nhóm + "Voucher Genie (−…) + Freeship" (3 tầng), tổng **3.485.200₫** đúng bộ số vàng G10.18 → bật công tắc Điểm (bàn phím) → tổng giảm → Mua hàng → /checkout (Đơn 1/n) → chọn VNPAY → cổng mô phỏng → "Thanh toán" → "Thanh toán thành công!", mã giao dịch, ≥ 3 liên kết Theo dõi. Thêm ca "Quay lại đơn hàng" (khách hủy) → "Thanh toán thất bại" + Thử thanh toán lại / Đổi phương thức. |
| G13.14 | `orders.spec.ts`: danh sách → Theo dõi đơn 44 (mã đơn, sao chép mã vận đơn, chưa có Trả hàng); trả hàng đơn 41 (VietQR): tải 1 ảnh + 1 video, tài khoản hoàn tiền → toast + khối `#return`; đánh giá đơn 33 bằng phím mũi tên. **Sửa kèm (trợ năng):** `RatingInput` đổi sao bằng ← → nhưng focus đứng yên ở sao cũ → nay focus đi theo sao được chọn (mẫu radiogroup WAI-ARIA). |
| G13.15 | `kyc-onboarding.spec.ts` (thay stub): /become-seller → OTP 123456 → `/seller/kyc` → Số CCCD + Người đại diện + CCCD 2 mặt + selfie (PNG ≥ 20 KB) → "Gửi duyệt KYC" → "Hồ sơ đang chờ duyệt", mục Sản phẩm trên nav bị khóa. Shop đã xác minh: tạo sản phẩm (Lưu nháp — chỉ cần tên) → có ở tab Nháp; đơn tab Mới → Xác nhận → tab Chờ chuẩn bị → Chuẩn bị hàng. |
| G13.16 | `admin.spec.ts`: thêm cơ chế mock `genie.mock.staff_title` (sessionStorage, `shared/auth/session.ts`) đăng nhập /admin bằng nhân viên theo chức danh MODERATOR / FINANCE_OFFICER / CS_SPECIALIST với bộ quyền `TITLE_PRESETS` — chỉ cho e2e, không có nút đổi vai (D5). Kiểm: Kiểm duyệt viên thấy đúng 4 mục, không thấy Rút tiền / Sổ cái / Phân quyền / Voucher, mở `/admin/withdrawals` → "Bạn không có quyền truy cập"; Tài chính thấy Rút tiền, không thấy Duyệt KYC; Admin phê duyệt KYC; Admin Duyệt chi → hộp chuyển khoản có nội dung `TRWD…` → Đã chuyển khoản → hoàn tất. |
| G13.17 | Xóa 28 `.gitkeep` ở thư mục `web/` đã có tệp; giữ 4 ở tầng còn trống (`catalog/application`, `catalog/ui`, `payment/application`, `payment/infrastructure`). `.gitkeep` của `server-spring` / `server-python` ngoài phạm vi (plan mục 8). Stub không dùng: barrel `shared/hooks/index.ts` (mọi nơi import thẳng `@/shared/hooks/useT`…) và khóa i18n `admin.shell.pending` ("Màn này đang được chuyển…") còn sót từ màn tạm G12 — đã xóa; không còn TODO / FIXME trong `web/src`. Commit C4 làm sau G13.21 vì G13.22 chờ xác nhận. |
| G13.18 | README gốc thêm mục "Frontend `web/` — chạy riêng ở chế độ mock": lệnh chạy, bảng biến môi trường (mặc định lấy từ `shared/config/env.ts`), tài khoản / OTP mẫu, nhân viên mock theo chức danh, `?theme=`, thư viện `/dev/ui` (chỉ ở dev), bảng lệnh `verify` / `test` / `test:e2e` (`E2E_DEV`, `E2E_PORT`), liên kết tài liệu bàn giao + ADR. |
| G13.19 | `docs/adr/`: README (mục lục, mẫu Trạng thái · Bối cảnh · Quyết định · Hệ quả) + 6 ADR: 0001 chín theme, 0002 CSS lai, 0003 mock trước, 0004 bảng icon, 0005 bỏ phần trình diễn (kèm ngoại lệ `genie.mock.staff_title` cho e2e), 0006 thư mục bổ sung — viết theo cây **thực tế** (34 feature, `features/*/server.ts`, `app/{seller,admin}`, `shared/api/mock/`, `tests/e2e/`…), không chép nguyên plan mục 3. |
| G13.20 | `docs/web-backend-handoff.md`: bảng 237 hàm có nhánh mock / 33 nhóm, sinh tự động từ mã (endpoint ở nhánh gọi thật), rồi đối chiếu tự động với `API_SPEC.md` → **21 endpoint lệch**: 11 đặt khác tên spec (sửa mã khi nối — vd. `/me/wishlist/ids` → `/me/wishlist/product-ids`, `/public/feed` → `/public/recommendations`, `/me/shop-follows` → `/me/following`), 5 spec chưa có (chiến dịch, bộ lọc thuộc tính, thương hiệu xác minh, tra SKU công khai, chính sách phí ship). Kèm trường DTO thiếu (`product_slug`, `order_id` trong thông báo), chức năng thay tạm (WebSocket chat, xuất CSV server, PDF A6, `/admin/shops`…), 3 hàm chỉ có ở mock, các bước nối. |
| G13.21 | `ci.yml` job `web` đã có lint, Vitest, build, check-i18n, lọc đường dẫn `web/**` + `scripts/check-i18n.mjs`, chạy trên PR vào `develop` / `main`. Sửa: `npx astro check` → `npm run check` (thêm `tsc --noEmit`, cùng cổng với `npm run verify` ở máy dev). E2E Playwright chưa đưa vào CI (cần cài trình duyệt + build, ~1 phút) — đề xuất thêm job riêng khi nhóm cần. |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-11 | Lập plan và progress | |
| 2026-11-11 | G00–G06: nhánh develop + feature/web-foundation, công cụ (Prettier, Vitest, Playwright, ESLint boundaries), token + Tailwind, tài sản thương hiệu, shared/lib (i18n, format, storage, seo), shared/api (http, mock, lỗi GNE-*), shared/ui (40+ component + bộ thống kê), thư viện /dev/ui | 46557a9 … f6095a3 |
| 2026-11-11 | G07: CSS storefront tách 10 module, layout Storefront/Auth/AppShell, SeoHead, ThemeBoot, middleware (ngôn ngữ theo request), 404, Header/Footer + catalog/notifications/cart/search/auth/policies tối thiểu, 9 theme + font tự lưu trữ + ThemePicker | 4bcae11 … 3dda4a9 |
| 2026-11-12 | Trang chủ storefront (G08 nhóm B) | 7a87a36 |
| 2026-11-13 | Danh mục, tìm kiếm, khám phá (G08 nhóm C) | 5058a10 |
| 2026-11-13 | Trang chi tiết sản phẩm + đánh giá (G08 nhóm D) | 3f136be |
| 2026-11-13 | Trang shop, Flash Sale, chiến dịch (G08 nhóm E) | 65aa679 |
| 2026-11-13 | Trang chính sách, sitemap, robots (G08 nhóm F) | 254fcc6 |
| 2026-11-13 | SEO: canonical, dữ liệu có cấu trúc, kiểm tra không JS (G08 nhóm G) | aac0d8a |
| 2026-11-13 | Đăng nhập, đăng ký, quên mật khẩu, OAuth callback (G09) | 49bbf6f |
| 2026-11-13 | Giỏ hàng nhiều shop: miền / hạ tầng / store giỏ, đổi phân loại, xóa nhiều, giỏ khách (G10 nhóm A) | 8a9c33e |
| 2026-11-13 | Voucher xếp chồng, Voucher Picker, phí ship, công tắc Điểm ở giỏ (G10 nhóm B) | 37f2d0b |
| 2026-11-13 | Trang Thanh toán gộp nhiều shop, sổ địa chỉ + RegionPicker, giữ hàng 15 phút, nháp checkout (G10 nhóm C) | 3bd8edb |
| 2026-11-13 | Luồng thanh toán VNPAY / VietQR / COD, cổng mô phỏng, trang kết quả (G10 nhóm D) | ad830ca |
| 2026-11-13 | Đơn mua (tab, nhóm đơn, hủy, đã nhận, mua lại, thanh toán lại) + theo dõi đơn + khung tài khoản (G10 nhóm E) | 913f4d8 |
| 2026-11-13 | Trả hàng / hoàn tiền (modal, bằng chứng, khối trạng thái) + đánh giá sản phẩm, lớp tải tệp dùng chung (G10 nhóm F) | 7c90b73 |
| 2026-11-13 | Tài khoản: hồ sơ, đổi email, sổ địa chỉ, mật khẩu (OTP bước 2), liên kết Google, thiết bị, xóa tài khoản (G10 nhóm G) | a6334cd |
| 2026-11-13 | Kho Voucher, Điểm Genie (điểm danh, đổi quà), yêu thích, shop theo dõi, trung tâm thông báo (G10 nhóm H) | 488581b |
| 2026-11-13 | Chat với shop (khung chat nổi + trang /chat, hỏi lại định kỳ thay WebSocket) + trang Trở thành Người bán (form, OTP → /seller/kyc) (G10 nhóm I) | 95fc810 |
| 2026-11-13 | Seller Center: khung SPA (thanh bên, thanh trên, chuông, tìm nhanh, menu tài khoản), router lazy, cổng KYC, 404 nội bộ (G11 nhóm A) | e1de77b |
| 2026-11-13 | Seller Center: Tổng quan (việc cần làm, doanh thu, tiền của shop, đơn gần đây, đơn 30 ngày, sản phẩm) + Phân tích (4 ô số, tiền thực về shop, kết quả đơn, nguồn truy cập, cảm xúc đánh giá, top sản phẩm, xuất CSV) (G11 nhóm B) | 428eaa8 |
| 2026-11-13 | Seller Center: Đơn hàng (tab, tìm, sắp xếp, xác nhận / từ chối, chuẩn bị, bàn giao 3PL, phiếu gửi hàng, chi tiết) + Trả hàng (48 giờ, bằng chứng, 6 thao tác) (G11 nhóm C) | 93021af |
| 2026-11-13 | Seller Center: Sản phẩm (danh sách, form đăng bán 7 khối, ẩn / xóa) + Danh mục toàn sàn (cây, đề xuất); lớp tải tệp thêm mục đích sản phẩm / KYC (G11 nhóm D) | 2a33c6b |
| 2026-11-13 | Seller Center: Ví & Escrow (số dư, cách tính tiền, sổ giao dịch, xuất CSV, rút tiền + OTP) + Hồ sơ KYC (giấy tờ, selfie / camera, nộp, yêu cầu xem xét) (G11 nhóm E) | 44c8537 |
| 2026-11-14 | Seller Center: Khuyến mãi (voucher + Flash Sale), Chat của shop, Đánh giá (phản hồi, báo cáo), Cài đặt (gian hàng, vận chuyển, lấy hàng GHN, tài khoản nhận tiền + OTP, chế độ nghỉ, bảo mật) — xong G11 (G11 nhóm F) | 543719a |
| 2026-11-14 | Admin Portal: khung SPA (thanh bên lọc theo quyền, huy hiệu hàng chờ, chuông, 403, 404 nội bộ), `/admin/me`, `/admin/queues`, kho mock admin dùng chung (G12 nhóm A) | 68cc825 |
| 2026-11-14 | Admin Portal: Tổng quan (việc cần xử lý, kỳ báo cáo, 4 ô tiền, hoa hồng đi về đâu, GMV, tiền giữ / chờ xử lý, đơn đang chạy, xếp hạng, shop tăng trưởng) + Duyệt KYC (hàng chờ, chi tiết, trình xem tài liệu, phê duyệt / từ chối / bổ sung) (G12 nhóm B) | b8bcfb5 |
| 2026-11-14 | Admin Portal: Trả hàng & Hoàn tiền (phân xử 4 phán quyết, phong tỏa ký quỹ, hoàn tiền Gateway / chuyển khoản tay), Kiểm duyệt đánh giá, Duyệt sản phẩm (hàng chờ, giao người duyệt, cấu hình nhân viên, trang chi tiết + thanh quyết định) (G12 nhóm C) | 7045474 |
| 2026-11-14 | Admin Portal: Danh mục toàn sàn (4 chỉ số, cây danh mục, chi tiết + hiển thị, thêm / sửa / xóa danh mục, bộ thuộc tính, sản phẩm theo danh mục con: duyệt / từ chối / khóa, đề xuất từ người bán) (G12 nhóm D) | 93b53d5 |
| 2026-11-14 | Admin Portal: Xử lý rút tiền (duyệt chi + chuyển khoản VietQR TRWD…, xác nhận tay, từ chối hoàn ví), Cấu hình hoa hồng, Sổ cái (lọc, tải thêm, xuất CSV), Đối soát COD (bảng kê CSV, đối chiếu, xác nhận) (G12 nhóm E) | e35446a |
| 2026-11-14 | Admin Portal: Voucher sàn (dùng chung VoucherEditor với Seller), Flash Sale (chiến dịch, duyệt SP đăng ký, lên lịch / kết thúc sớm), Banner trang chủ (kho banner chung với storefront, chọn đích từ dữ liệu sàn), Thông báo sàn (soạn rich-text, hẹn giờ, người nhận cụ thể) (G12 nhóm F) | c070b95 |
| 2026-11-14 | Admin Portal: Phân quyền nhân viên (mời, sửa quyền theo chức danh + cảnh báo tách biệt nhiệm vụ, khóa / mở khóa / xóa), Người dùng (tab, tìm, đình chỉ có thời hạn, mở khóa), Nhật ký hệ thống (lọc, tải thêm, xuất CSV), Tài khoản quản trị (ảnh, tên, mật khẩu OTP, thiết bị) (G12 nhóm G) — xong G12 | 0b68d13 |
| 2026-11-14 | Hoàn thiện i18n + từ ngữ thương hiệu: chuỗi cứng còn lại sang `t()`, tên ngôn ngữ dùng chung, `isoDateVN` / `formatMonthShort` thay Intl tự viết (G13 nhóm A) | fc589e8 |
| 2026-11-14 | So khớp hình storefront / Seller / Admin (16 màn × 3 khổ + quét 36 route SPA), sửa màu footer theo theme, thanh sắp xếp màn hẹp, tràn ngang bảng; trợ năng (Lighthouse a11y 100), tương phản 9 theme, 2 island sang `idle`, `noindex` có e2e, bỏ `eslint-disable` toàn tệp (G13 nhóm B) | 0914230 |
| 2026-11-14 | Vitest bổ sung 3 tệp miền; E2E 6 spec / 22 kịch bản × 2 khổ màn (khách vãng lai bằng bàn phím, mua hàng VNPay + voucher 3 tầng + Điểm, đơn hàng / trả hàng / đánh giá, KYC + sản phẩm + xử lý đơn, phân quyền Admin + duyệt KYC + rút tiền), chạy trên bản build production; sửa focus của ô chọn sao (G13 nhóm C) | 7254e9f |
| 2026-11-14 | Dọn `.gitkeep` / stub thừa, README chế độ mock, 6 ADR, tài liệu bàn giao backend (237 hàm, 21 endpoint lệch API_SPEC), CI dùng `npm run check` (G13 nhóm D, trừ G13.22 chờ xác nhận) | 9228a94 |
