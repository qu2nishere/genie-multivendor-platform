# PROGRESS — Chuyển toàn bộ giao diện prototype sang `web/`

| | |
|---|---|
| **Plan** | [plan_chuyen_giao_dien.md](./plan_chuyen_giao_dien.md) — chi tiết từng task tra theo mã `Gxx.yy` |
| **Ngày lập** | 2026-11-11 |
| **Tổng** | 379 task + 50 commit, 14 giai đoạn |
| **Cập nhật gần nhất** | 2026-11-11 — mới lập, chưa bắt đầu |

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
| G00 | Chuẩn bị repo, nhánh, môi trường | 10 | 1 | 0 / 11 | 10 / 11 | |
| G01 | Công cụ & cấu hình `web/` | 11 | 1 | 0 / 12 | 0 / 12 | |
| G02 | Token thiết kế, font, theme Tailwind, CSS nền | 15 | 2 | 0 / 17 | 0 / 17 | |
| G03 | Tài sản tĩnh & thương hiệu | 10 | 1 | 0 / 11 | 0 / 11 | |
| G04 | `shared/lib`, hook dùng chung, nền i18n | 14 | 1 | 0 / 15 | 0 / 15 | |
| G05 | `shared/api`, phiên đăng nhập, lớp mock | 10 | 1 | 0 / 11 | 0 / 11 | |
| G06 | `shared/ui` (design system) | 37 | 5 | 0 / 42 | 0 / 42 | |
| G07 | Layout & khung trang | 26 | 4 | 0 / 30 | 0 / 30 | |
| G08 | Storefront công khai (SSR + islands) | 54 | 7 | 0 / 61 | 0 / 61 | |
| G09 | Xác thực | 14 | 1 | 0 / 15 | 0 / 15 | |
| G10 | Trang người mua (cần đăng nhập) | 66 | 9 | 0 / 75 | 0 / 75 | |
| G11 | Seller Center (SPA `/seller/*`) | 45 | 6 | 0 / 51 | 0 / 51 | |
| G12 | Admin Portal (SPA `/admin/*`) | 45 | 7 | 0 / 52 | 0 / 52 | |
| G13 | Hoàn thiện, kiểm thử, bàn giao | 22 | 4 | 0 / 26 | 0 / 26 | |
| **Tổng** | | **379** | **50** | **10 / 429** | | |

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
- [ ] **G00.C1** 🔖 COMMIT `docs: add web ui migration plan and progress tracker` · hash: `_______`

## G01 — Công cụ & cấu hình `web/`

- [ ] **G01.01** Prettier
- [ ] **G01.02** Vitest
- [ ] **G01.03** Playwright
- [ ] **G01.04** Biến môi trường
- [ ] **G01.05** Kiểu môi trường
- [ ] **G01.06** ESLint boundaries
- [ ] **G01.07** ESLint import sâu
- [ ] **G01.08** ESLint cho test & cấu hình
- [ ] **G01.09** Script `verify`
- [ ] **G01.10** `check-i18n.mjs`
- [ ] **G01.11** Sửa lỗi baseline
- [ ] **G01.C1** 🔖 COMMIT `build(web): add prettier, vitest, playwright config and env example` · hash: `_______`

## G02 — Token thiết kế, font, theme Tailwind, CSS nền


**Nhóm A — Token**

- [ ] **G02.01** Font tệp
- [ ] **G02.02** Token font
- [ ] **G02.03** Token màu
- [ ] **G02.04** Token chữ
- [ ] **G02.05** Token khoảng cách
- [ ] **G02.06** Token hiệu ứng
- [ ] **G02.07** CSS nền
- [ ] **G02.08** Sửa `global.css`
- [ ] **G02.C1** 🔖 COMMIT `feat(web): port design tokens and base styles from prototype` · hash: `_______`

**Nhóm B — Tailwind**

- [ ] **G02.09** Tailwind màu
- [ ] **G02.10** Tailwind hình khối
- [ ] **G02.11** Tailwind chữ
- [ ] **G02.12** Tailwind bố cục
- [ ] **G02.13** Responsive
- [ ] **G02.14** Kiểm kê lớp `lz-*`
- [ ] **G02.15** Trang thư viện nội bộ
- [ ] **G02.C2** 🔖 COMMIT `feat(web): map prototype tokens into tailwind theme` · hash: `_______`

## G03 — Tài sản tĩnh & thương hiệu

- [ ] **G03.01** Ảnh sản phẩm mẫu
- [ ] **G03.02** Banner
- [ ] **G03.03** Logo thanh toán
- [ ] **G03.04** SVG thương hiệu
- [ ] **G03.05** Favicon
- [ ] **G03.06** `GenieMark` / `GenieLogo`
- [ ] **G03.07** `GenieCoin`
- [ ] **G03.08** Font logo
- [ ] **G03.09** Ảnh nền theme
- [ ] **G03.10** Hằng đường dẫn ảnh
- [ ] **G03.C1** 🔖 COMMIT `feat(web): add brand assets and mock catalog images` · hash: `_______`

## G04 — `shared/lib`, hook dùng chung, nền i18n

- [ ] **G04.01** `cn()`
- [ ] **G04.02** Định dạng tiền
- [ ] **G04.03** Định dạng ngày & số
- [ ] **G04.04** Che dữ liệu
- [ ] **G04.05** Test định dạng
- [ ] **G04.06** Lưu trình duyệt
- [ ] **G04.07** Hàm `t()`
- [ ] **G04.08** Hook `useT()`
- [ ] **G04.09** Test i18n
- [ ] **G04.10** Quy ước không gian khóa
- [ ] **G04.11** Công cụ tra bản dịch
- [ ] **G04.12** SEO
- [ ] **G04.13** Hook dùng chung
- [ ] **G04.14** Cập nhật boundaries
- [ ] **G04.C1** 🔖 COMMIT `feat(web): add i18n, formatting, storage and shared hooks` · hash: `_______`

## G05 — `shared/api`, phiên đăng nhập, lớp mock

- [ ] **G05.01** Kiểu phong bì API
- [ ] **G05.02** Lỗi
- [ ] **G05.03** `http.ts`
- [ ] **G05.04** `server-http.ts`
- [ ] **G05.05** `idempotency.ts`
- [ ] **G05.06** Lớp mock
- [ ] **G05.07** Phiên
- [ ] **G05.08** Quyền
- [ ] **G05.09** Providers
- [ ] **G05.10** Test
- [ ] **G05.C1** 🔖 COMMIT `feat(web): add api client, session, permissions and mock layer` · hash: `_______`

## G06 — `shared/ui` (design system)


**Nhóm A — Phần tử cơ bản**

- [ ] **G06.01** `Icon`
- [ ] **G06.02** `Button`
- [ ] **G06.03** `IconButton`
- [ ] **G06.04** `Input`
- [ ] **G06.05** `Textarea`
- [ ] **G06.06** `PasswordInput`
- [ ] **G06.07** `Checkbox`, `Radio`, `Switch`
- [ ] **G06.08** `Select`
- [ ] **G06.09** `Tabs`, `Segmented`
- [ ] **G06.10** `Badge`, `StatusBadge`, `Tag`
- [ ] **G06.11** `Avatar`
- [ ] **G06.C1** 🔖 COMMIT `feat(web): add base form and display components to shared ui` · hash: `_______`

**Nhóm B — Lớp phủ & phản hồi**

- [ ] **G06.12** `Modal`
- [ ] **G06.13** `Popover` / `Dropdown`
- [ ] **G06.14** `Toast`
- [ ] **G06.15** `EmptyState`, `Skeleton`, `Spinner`
- [ ] **G06.16** `OtpInput`
- [ ] **G06.17** `Countdown`
- [ ] **G06.18** `Pagination`
- [ ] **G06.19** `Card`, `PanelHead`, `SectionHead`
- [ ] **G06.C2** 🔖 COMMIT `feat(web): add overlay and feedback components to shared ui` · hash: `_______`

**Nhóm C — Thành phần thương mại**

- [ ] **G06.20** `Price`
- [ ] **G06.21** `Rating`
- [ ] **G06.22** `QuantityStepper`
- [ ] **G06.23** `ProductCard`
- [ ] **G06.24** `VendorChip`, `Seal`
- [ ] **G06.25** `VoucherTag`, `Ticket`
- [ ] **G06.26** `QrPlaceholder`
- [ ] **G06.27** `DataTable`
- [ ] **G06.28** `Field`, `FormSection`, `DateInput`
- [ ] **G06.C3** 🔖 COMMIT `feat(web): add commerce and data components to shared ui` · hash: `_______`

**Nhóm D — Bộ thống kê (`dash.jsx` + `dash.css`)**

- [ ] **G06.29** `Delta`, `StatTile`, `Kpi`
- [ ] **G06.30** `WorkQueue`, `Pipeline`
- [ ] **G06.31** `BarList`, `StackBar`, `SegBar`
- [ ] **G06.32** `TimeChart`
- [ ] **G06.33** `dash.css`
- [ ] **G06.34** Test hàm thuần
- [ ] **G06.C4** 🔖 COMMIT `feat(web): add dashboard chart components to shared ui` · hash: `_______`

**Nhóm E — Rà soát**

- [ ] **G06.35** Xuất tập trung
- [ ] **G06.36** Thư viện nội bộ
- [ ] **G06.37** Xóa stub cũ
- [ ] **G06.C5** 🔖 COMMIT `feat(web): add component gallery and shared ui barrel` · hash: `_______`

## G07 — Layout & khung trang


**Nhóm A — CSS storefront**

- [ ] **G07.01** Tách `storefront.css`
- [ ] **G07.02** Chuẩn hóa giá trị
- [ ] **G07.03** Nhập vào `global.css`
- [ ] **G07.C1** 🔖 COMMIT `feat(web): port storefront stylesheet into layered css modules` · hash: `_______`

**Nhóm B — Layout**

- [ ] **G07.04** `SeoHead.astro` + `<head>` của `StorefrontLayout.astro`
- [ ] **G07.05** `ThemeBoot.astro`
- [ ] **G07.06** Khung thân
- [ ] **G07.07** `ToastHost.tsx`
- [ ] **G07.08** `AuthLayout.astro`
- [ ] **G07.09** `AppShellLayout.astro`
- [ ] **G07.10** `middleware.ts`
- [ ] **G07.11** `404.astro`
- [ ] **G07.C2** 🔖 COMMIT `feat(web): build storefront, auth and app shell layouts` · hash: `_______`

**Nhóm C — Header & Footer**

- [ ] **G07.12** Thanh tiện ích trên cùng
- [ ] **G07.13** Thanh chính
- [ ] **G07.14** `SearchBox`
- [ ] **G07.15** `MiniCart`
- [ ] **G07.16** `UserMenu`
- [ ] **G07.17** `NotifPanel`
- [ ] **G07.18** Menu danh mục & điều hướng màn hẹp
- [ ] **G07.19** `Footer.astro`
- [ ] **G07.20** `LoginPrompt`
- [ ] **G07.21** Khóa i18n khung trang
- [ ] **G07.C3** 🔖 COMMIT `feat(web): add storefront header, footer and global overlays` · hash: `_______`

**Nhóm D — Đủ 9 theme (D1 đã chốt)**

- [ ] **G07.22** `themes.css` phần theme `genie` + `classic`
- [ ] **G07.23** 7 theme còn lại
- [ ] **G07.24** Font theme
- [ ] **G07.25** `ThemePicker`
- [ ] **G07.26** Kiểm 9 theme
- [ ] **G07.C4** 🔖 COMMIT `feat(web): port storefront themes and theme picker` · hash: `_______`

## G08 — Storefront công khai (SSR + islands)


**Nhóm A — Miền danh mục & sản phẩm**

- [ ] **G08.01** Kiểu domain sản phẩm
- [ ] **G08.02** Quy tắc sản phẩm
- [ ] **G08.03** Hạ tầng sản phẩm
- [ ] **G08.04** Kiểu & hạ tầng danh mục
- [ ] **G08.05** Kiểu & hạ tầng shop
- [ ] **G08.06** API công khai feature
- [ ] **G08.C1** 🔖 COMMIT `feat(web): add catalog, product and shop domain with mock data` · hash: `_______`

**Nhóm B — Trang chủ**

- [ ] **G08.07** Miền banner
- [ ] **G08.08** `BannerCarousel`, `BannerSlot`, `BannerPair`, `BannerPicture`
- [ ] **G08.09** `BannerPopup`
- [ ] **G08.10** `CategoryTiles.astro`
- [ ] **G08.11** `FlashBand`
- [ ] **G08.12** `SaveVoucher`
- [ ] **G08.13** `ProductFeed`
- [ ] **G08.14** Dải cam kết (Promise Strip trong `DESIGN.md`)
- [ ] **G08.15** `pages/index.astro`
- [ ] **G08.16** Khóa i18n `home.*`
- [ ] **G08.C2** 🔖 COMMIT `feat(web): build storefront home page` · hash: `_______`

**Nhóm C — Danh mục, tìm kiếm, khám phá**

- [ ] **G08.17** Quy tắc lọc & sắp xếp
- [ ] **G08.18** `FilterPanel`
- [ ] **G08.19** `SortBar`, `ResultGrid`
- [ ] **G08.20** `pages/c/[slug].astro`
- [ ] **G08.21** `pages/search.astro`
- [ ] **G08.22** `pages/discover.astro`
- [ ] **G08.23** Khóa i18n `catalog.*`, `search.*`, `discover.*`
- [ ] **G08.C3** 🔖 COMMIT `feat(web): build category, search and discover pages` · hash: `_______`

**Nhóm D — Chi tiết sản phẩm**

- [ ] **G08.24** Thư viện ảnh
- [ ] **G08.25** Khối mua
- [ ] **G08.26** `AddToCartButton` + Mua ngay
- [ ] **G08.27** Nút yêu thích & chia sẻ
- [ ] **G08.28** Khối shop
- [ ] **G08.29** `SpecsTable.astro`, `ProductDescription.astro`
- [ ] **G08.30** Thanh mua dính đáy ở màn hẹp (`tr-root--bar`)
- [ ] **G08.31** `ProductGone`
- [ ] **G08.32** Miền đánh giá
- [ ] **G08.33** `ReviewList`
- [ ] **G08.34** Sản phẩm tương tự
- [ ] **G08.35** `pages/p/[slug]-[id].astro`
- [ ] **G08.36** Khóa i18n `product.*`, `reviews.*`
- [ ] **G08.C4** 🔖 COMMIT `feat(web): build product detail page with reviews` · hash: `_______`

**Nhóm E — Shop, Flash Sale, chiến dịch**

- [ ] **G08.37** `pages/shop/[id].astro`
- [ ] **G08.38** `pages/flash-sale.astro`
- [ ] **G08.39** `pages/campaign/[slug].astro`
- [ ] **G08.40** Khóa i18n `shop.*`, `promotions.flash.*`, `promotions.campaign.*`
- [ ] **G08.C5** 🔖 COMMIT `feat(web): build shop, flash sale and campaign pages` · hash: `_______`

**Nhóm F — Trang chính sách & sitemap**

- [ ] **G08.41** Dữ liệu chính sách
- [ ] **G08.42** `PolicyPage.astro`
- [ ] **G08.43** `pages/policies/[slug].astro`
- [ ] **G08.44** `pages/sitemap.xml.ts`
- [ ] **G08.45** `public/robots.txt`
- [ ] **G08.C6** 🔖 COMMIT `feat(web): add policy pages and sitemap` · hash: `_______`

**Nhóm G — SEO kỹ thuật (rà toàn bộ trang công khai vừa dựng)**

- [ ] **G08.46** URL chuẩn
- [ ] **G08.47** Dữ liệu có cấu trúc trang chủ
- [ ] **G08.48** Dữ liệu có cấu trúc trang danh sách
- [ ] **G08.49** Dữ liệu có cấu trúc sản phẩm
- [ ] **G08.50** Phân trang SSR
- [ ] **G08.51** Ảnh
- [ ] **G08.52** Cấu trúc tiêu đề & vùng
- [ ] **G08.53** Mã trạng thái
- [ ] **G08.54** Kiểm tự động
- [ ] **G08.C7** 🔖 COMMIT `feat(web): add structured data, canonical urls and seo checks` · hash: `_______`

## G09 — Xác thực

- [ ] **G09.01** Miền auth
- [ ] **G09.02** Lược đồ form
- [ ] **G09.03** Hạ tầng auth
- [ ] **G09.04** Tầng ứng dụng
- [ ] **G09.05** `LoginForm`
- [ ] **G09.06** Bước OTP
- [ ] **G09.07** Trạng thái khóa
- [ ] **G09.08** `RegisterForm`
- [ ] **G09.09** `ForgotForm`
- [ ] **G09.10** `SocialButton`, `SsoDialog`
- [ ] **G09.11** `pages/auth/{login,register,forgot}.astro`
- [ ] **G09.12** `pages/auth/oauth-callback.astro`
- [ ] **G09.13** Hiển thị lỗi xác thực
- [ ] **G09.14** Khóa i18n `auth.*`; `index.ts` xuất `LoginPrompt`, `UserMenu`, hook phiên
- [ ] **G09.C1** 🔖 COMMIT `feat(web): build login, register and password recovery flows` · hash: `_______`

## G10 — Trang người mua (cần đăng nhập)


**Nhóm A — Giỏ hàng**

- [ ] **G10.01** Miền giỏ
- [ ] **G10.02** Hạ tầng giỏ
- [ ] **G10.03** Ứng dụng giỏ
- [ ] **G10.04** `CartPage`
- [ ] **G10.05** `VariantModal`
- [ ] **G10.06** Tóm tắt giỏ
- [ ] **G10.07** Giỏ rỗng & giỏ khách
- [ ] **G10.08** `pages/cart.astro` + khóa i18n `cart.*`
- [ ] **G10.C1** 🔖 COMMIT `feat(web): build multi-shop cart page` · hash: `_______`

**Nhóm B — Voucher & Điểm**

- [ ] **G10.09** Miền voucher
- [ ] **G10.10** Tính xếp chồng
- [ ] **G10.11** Phí vận chuyển hiển thị
- [ ] **G10.12** Hạ tầng voucher
- [ ] **G10.13** `VoucherPicker`
- [ ] **G10.14** Voucher theo từng shop trong giỏ
- [ ] **G10.15** Công tắc dùng Điểm Genie
- [ ] **G10.C2** 🔖 COMMIT `feat(web): add stacked voucher and points pricing` · hash: `_______`

**Nhóm C — Thanh toán**

- [ ] **G10.16** Miền checkout
- [ ] **G10.17** Hạ tầng checkout
- [ ] **G10.18** `CheckoutPage`
- [ ] **G10.19** Đếm ngược giữ chỗ 15 phút + xử lý dòng hết hàng (sửa số lượng / bỏ món)
- [ ] **G10.20** Chọn / đổi địa chỉ tại checkout
- [ ] **G10.21** Nháp checkout
- [ ] **G10.22** `pages/checkout.astro` + khóa i18n `checkout.*`
- [ ] **G10.C3** 🔖 COMMIT `feat(web): build consolidated checkout page` · hash: `_______`

**Nhóm D — Thanh toán & kết quả**

- [ ] **G10.23** `MoneyRail`, `MoneyChip`, `HoldNote`
- [ ] **G10.24** `PayModal` + `PayGateway`
- [ ] **G10.25** `VietQrPanel`
- [ ] **G10.26** COD
- [ ] **G10.27** `OrderDone`
- [ ] **G10.28** `PaymentFailed`
- [ ] **G10.29** `pages/checkout/result.astro`
- [ ] **G10.C4** 🔖 COMMIT `feat(web): add payment flows and checkout result page` · hash: `_______`

**Nhóm E — Đơn mua & theo dõi**

- [ ] **G10.30** Miền đơn
- [ ] **G10.31** Hạ tầng đơn
- [ ] **G10.32** `OrdersPanel`
- [ ] **G10.33** Hủy đơn có lý do
- [ ] **G10.34** Xác nhận đã nhận hàng
- [ ] **G10.35** `pages/orders/index.astro`
- [ ] **G10.36** `OrderTracking`
- [ ] **G10.37** Mua lại, chat với shop từ đơn
- [ ] **G10.38** `pages/orders/[id].astro` + khóa i18n `orders.*`
- [ ] **G10.C5** 🔖 COMMIT `feat(web): build order list and order tracking pages` · hash: `_______`

**Nhóm F — Trả hàng & đánh giá**

- [ ] **G10.39** Miền hoàn tiền
- [ ] **G10.40** `ReturnModal`
- [ ] **G10.41** Hiển thị trạng thái trả hàng trong trang đơn
- [ ] **G10.42** `ReviewModal`
- [ ] **G10.43** Khóa i18n `refunds.*`, `reviews.form.*`
- [ ] **G10.C6** 🔖 COMMIT `feat(web): add return request and review submission` · hash: `_______`

**Nhóm G — Tài khoản**

- [ ] **G10.44** Khung tài khoản
- [ ] **G10.45** Miền & hạ tầng tài khoản
- [ ] **G10.46** `ProfilePanel`
- [ ] **G10.47** `EmailChangeModal`
- [ ] **G10.48** `AddressPanel` + `AddressModal` + `AddressForm`
- [ ] **G10.49** `RegionPicker`
- [ ] **G10.50** `PasswordPanel`
- [ ] **G10.51** `LoginsPanel`
- [ ] **G10.52** `DevicesPanel`
- [ ] **G10.53** Xóa tài khoản sau 30 ngày
- [ ] **G10.54** Khóa i18n `account.*`
- [ ] **G10.C7** 🔖 COMMIT `feat(web): build account profile, address and security sections` · hash: `_______`

**Nhóm H — Voucher, Điểm, yêu thích, theo dõi, thông báo**

- [ ] **G10.55** `VouchersPanel`
- [ ] **G10.56** Miền Điểm
- [ ] **G10.57** `CoinsPanel`
- [ ] **G10.58** `WishlistPanel`
- [ ] **G10.59** `FollowsPanel`
- [ ] **G10.60** `NotificationsPanel`
- [ ] **G10.61** Khóa i18n `promotions.wallet.*`, `coins.*`, `notifications.*`
- [ ] **G10.C8** 🔖 COMMIT `feat(web): add vouchers, points, wishlist and notification sections` · hash: `_______`

**Nhóm I — Chat & mở shop**

- [ ] **G10.62** Miền chat
- [ ] **G10.63** `ChatWidget`
- [ ] **G10.64** `pages/chat.astro`
- [ ] **G10.65** `BecomeSeller`
- [ ] **G10.66** `pages/become-seller.astro` + khóa i18n `chat.*`, `seller-onboarding.landing.*`
- [ ] **G10.C9** 🔖 COMMIT `feat(web): add buyer chat and become-seller landing` · hash: `_______`

## G11 — Seller Center (SPA `/seller/*`)


**Nhóm A — Khung SPA**

- [ ] **G11.01** Router
- [ ] **G11.02** `SellerApp.tsx`
- [ ] **G11.03** Layout Seller
- [ ] **G11.04** `NotifBell`, `AccountMenu`, `SellerSearch`
- [ ] **G11.05** `KycGate`
- [ ] **G11.06** `pages/seller/[...path].astro`
- [ ] **G11.07** Hạ tầng shop người bán
- [ ] **G11.08** Khóa i18n `seller.shell.*`
- [ ] **G11.C1** 🔖 COMMIT `feat(seller-ui): scaffold seller center shell and routing` · hash: `_______`

**Nhóm B — Tổng quan & phân tích**

- [ ] **G11.09** `seller-dashboard` miền + mock
- [ ] **G11.10** `OverviewPage`
- [ ] **G11.11** `seller-analytics`
- [ ] **G11.12** Khóa i18n `seller-dashboard.*`, `seller-analytics.*`
- [ ] **G11.C2** 🔖 COMMIT `feat(seller-ui): add overview and analytics screens` · hash: `_______`

**Nhóm C — Đơn hàng & trả hàng**

- [ ] **G11.13** `seller-orders` miền
- [ ] **G11.14** Hạ tầng
- [ ] **G11.15** `FulfillmentPage`
- [ ] **G11.16** Xác nhận / từ chối đơn có lý do
- [ ] **G11.17** Đóng gói & tạo vận đơn
- [ ] **G11.18** `ReturnsPage`
- [ ] **G11.19** Khóa i18n `seller-orders.*`
- [ ] **G11.C3** 🔖 COMMIT `feat(seller-ui): add order fulfillment and returns screens` · hash: `_______`

**Nhóm D — Sản phẩm**

- [ ] **G11.20** `seller-products` miền
- [ ] **G11.21** Hạ tầng
- [ ] **G11.22** `ProductsPage`
- [ ] **G11.23** `ProductEditor` khung
- [ ] **G11.24** `ImageUpload`
- [ ] **G11.25** `BasicInfo`
- [ ] **G11.26** `SpecsTable`
- [ ] **G11.27** `RichDescription`
- [ ] **G11.28** `VariantSection`
- [ ] **G11.29** `ShippingInfo`
- [ ] **G11.30** `LegalDocs`
- [ ] **G11.31** `VendorCatalogPage`
- [ ] **G11.32** Khóa i18n `seller-products.*`
- [ ] **G11.C4** 🔖 COMMIT `feat(seller-ui): add product list, editor and catalog screens` · hash: `_______`

**Nhóm E — Ví, KYC**

- [ ] **G11.33** `seller-wallet` miền + hạ tầng
- [ ] **G11.34** `WalletPage`
- [ ] **G11.35** `WithdrawModal`
- [ ] **G11.36** `seller-onboarding` miền + hạ tầng KYC
- [ ] **G11.37** `OnboardingPage`
- [ ] **G11.38** `CameraModal`
- [ ] **G11.39** Khóa i18n `seller-wallet.*`, `seller-onboarding.kyc.*`
- [ ] **G11.C5** 🔖 COMMIT `feat(seller-ui): add wallet and kyc onboarding screens` · hash: `_______`

**Nhóm F — Khuyến mãi, chat, đánh giá, cài đặt**

- [ ] **G11.40** `seller-promotions`
- [ ] **G11.41** `SellerChatPage`
- [ ] **G11.42** `seller-reviews`
- [ ] **G11.43** `seller-settings`
- [ ] **G11.44** `SecuritySection`
- [ ] **G11.45** Khóa i18n `seller-promotions.*`, `seller-reviews.*`, `seller-settings.*`
- [ ] **G11.C6** 🔖 COMMIT `feat(seller-ui): add promotions, chat, reviews and settings screens` · hash: `_______`

## G12 — Admin Portal (SPA `/admin/*`)


**Nhóm A — Khung SPA & phân quyền**

- [ ] **G12.01** Router
- [ ] **G12.02** `AdminApp.tsx`
- [ ] **G12.03** Layout Admin
- [ ] **G12.04** Chặn route
- [ ] **G12.05** Huy hiệu số việc chờ
- [ ] **G12.06** `pages/admin/[...path].astro`
- [ ] **G12.07** Khóa i18n `admin.shell.*`, tên 13 quyền `admin.perm.*`
- [ ] **G12.C1** 🔖 COMMIT `feat(admin-ui): scaffold admin portal shell with permission gating` · hash: `_______`

**Nhóm B — Tổng quan, KYC**

- [ ] **G12.08** `admin-dashboard`
- [ ] **G12.09** `admin-kyc` miền + hạ tầng
- [ ] **G12.10** `KycReviewPage`
- [ ] **G12.11** `DocViewer`
- [ ] **G12.12** Quyết định KYC
- [ ] **G12.13** Khóa i18n `admin-dashboard.*`, `admin-kyc.*`
- [ ] **G12.C2** 🔖 COMMIT `feat(admin-ui): add dashboard and kyc review screens` · hash: `_______`

**Nhóm C — Tranh chấp & kiểm duyệt**

- [ ] **G12.14** `admin-disputes` miền + hạ tầng
- [ ] **G12.15** `RefundsPage`
- [ ] **G12.16** `admin-moderation` đánh giá
- [ ] **G12.17** `admin-moderation` sản phẩm
- [ ] **G12.18** `ProductModerationPage`
- [ ] **G12.19** Khóa i18n `admin-disputes.*`, `admin-moderation.*`
- [ ] **G12.C3** 🔖 COMMIT `feat(admin-ui): add dispute and moderation screens` · hash: `_______`

**Nhóm D — Danh mục**

- [ ] **G12.20** `admin-catalog` miền + hạ tầng
- [ ] **G12.21** `CatalogPage`
- [ ] **G12.22** `CatModal`
- [ ] **G12.23** `AttributePanel`
- [ ] **G12.24** Duyệt đề xuất danh mục của shop; khóa i18n `admin-catalog.*`
- [ ] **G12.C4** 🔖 COMMIT `feat(admin-ui): add catalog management screens` · hash: `_______`

**Nhóm E — Tài chính**

- [ ] **G12.25** `admin-finance` miền + hạ tầng
- [ ] **G12.26** `WithdrawalsPage`
- [ ] **G12.27** `CommissionPage`
- [ ] **G12.28** `LedgerPage`
- [ ] **G12.29** Đối soát COD miền
- [ ] **G12.30** `CodReconciliationPage`
- [ ] **G12.31** Khóa i18n `admin-finance.*`
- [ ] **G12.C5** 🔖 COMMIT `feat(admin-ui): add payout, commission, ledger and cod screens` · hash: `_______`

**Nhóm F — Marketing**

- [ ] **G12.32** `admin-marketing` voucher
- [ ] **G12.33** `PlatformVouchersPage` + `PlatformVoucherModal`
- [ ] **G12.34** `FlashSalePage`
- [ ] **G12.35** Banner miền
- [ ] **G12.36** `BannersPage`
- [ ] **G12.37** `PlatformNotificationsPage`
- [ ] **G12.38** Khóa i18n `admin-marketing.*`
- [ ] **G12.C6** 🔖 COMMIT `feat(admin-ui): add voucher, flash sale, banner and notification screens` · hash: `_______`

**Nhóm G — Hệ thống**

- [ ] **G12.39** `admin-system` miền + hạ tầng
- [ ] **G12.40** `StaffPage` (`Rbac`)
- [ ] **G12.41** `InviteStaff`, `RbacEditor`
- [ ] **G12.42** `UsersPage`
- [ ] **G12.43** `AuditLogPage`
- [ ] **G12.44** `AccountAdminPage`
- [ ] **G12.45** Khóa i18n `admin-system.*`
- [ ] **G12.C7** 🔖 COMMIT `feat(admin-ui): add staff, users, audit log and account screens` · hash: `_______`

## G13 — Hoàn thiện, kiểm thử, bàn giao


**Nhóm A — Ngôn ngữ & thương hiệu**

- [ ] **G13.01** Quét chuỗi cứng
- [ ] **G13.02** Hoàn tất `en.json`
- [ ] **G13.03** Rà từ ngữ R11
- [ ] **G13.04** Rà tiền / ngày
- [ ] **G13.C1** 🔖 COMMIT `refactor(web): complete i18n coverage and brand wording` · hash: `_______`

**Nhóm B — Chất lượng giao diện**

- [ ] **G13.05** So khớp hình storefront
- [ ] **G13.06** So khớp hình Seller + Admin
- [ ] **G13.07** Trợ năng
- [ ] **G13.08** Rà chỉ thị hydrate
- [ ] **G13.09** Rà SEO lần cuối
- [ ] **G13.10** Rà kiến trúc
- [ ] **G13.C2** 🔖 COMMIT `fix(web): resolve visual parity, accessibility and hydration issues` · hash: `_______`

**Nhóm C — Kiểm thử**

- [ ] **G13.11** Bổ sung Vitest
- [ ] **G13.12** E2E khách vãng lai
- [ ] **G13.13** E2E mua hàng
- [ ] **G13.14** E2E đơn hàng
- [ ] **G13.15** E2E người bán
- [ ] **G13.16** E2E quản trị
- [ ] **G13.C3** 🔖 COMMIT `test(web): add unit and e2e coverage for migrated screens` · hash: `_______`

**Nhóm D — Dọn dẹp & bàn giao**

- [ ] **G13.17** Xóa `.gitkeep` ở thư mục đã có file; xóa stub không dùng
- [ ] **G13.18** Tài liệu
- [ ] **G13.19** ADR
- [ ] **G13.20** Danh sách việc nối backend
- [ ] **G13.21** CI
- [ ] **G13.22** Mở PR vào `develop`
- [ ] **G13.C4** 🔖 COMMIT `docs(web): document mock mode, ui gallery and backend handoff` · hash: `_______`

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
| | | |

### Lệch giao diện so với prototype (G13.05, G13.06)

| Route | Khổ màn | Mô tả lệch | Đã sửa |
|---|---|---|---|
| | | | |

### Task bỏ qua / thay đổi so với plan

| Mã task | Lý do |
|---|---|
| G00.03 | Không có quyền truy cập Jira từ máy làm việc → bỏ mã `[#GNE-…]` trong commit / tên nhánh (D6). Khi có Epic thì bổ sung ở tiêu đề PR. |
| G00.04 / G00.05 | Đã tạo nhánh `develop` và `feature/web-foundation` **ở local**; chưa `git push` lên `origin` — chờ chủ dự án xác nhận đẩy nhánh. |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-11 | Lập plan và progress | |
