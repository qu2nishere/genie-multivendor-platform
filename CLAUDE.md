# CLAUDE.md — Ngữ cảnh làm việc cho repo `genie-multivendor-platform`

> Đọc file này trước khi làm bất cứ việc gì. Mục tiêu: không phải đọc lại toàn bộ codebase / tài liệu.
> Cập nhật lần cuối: 2026-11-18 (cụm tìm kiếm & trang sản phẩm UC-CAT-02/04/05/06/07/16/18 — plan `plan/2026-11-17/`, nhánh `feature/web-catalog-real-api`).
> Khi thay đổi lớn (xong một plan, đổi quy ước, thêm ADR) → **cập nhật mục 2 và mục 9 của file này**.

## 0. Dự án là gì

**Genie** — sàn TMĐT đa người bán (multi-vendor), bản **TLCN** (tiểu luận chuyên ngành), nhóm 4 người,
8 sprint × 2 tuần. Phạm vi: **87 use case · 11 phân hệ · 16 tác nhân · 65 bảng · 55 enum**.
Bản mở rộng sau là DATN (206 UC) — chỉ được **thêm**, không đổi tên bảng / cột / enum / mã UC (NFR-12).

Ba bề mặt giao diện: **Storefront** (người mua) · **Seller Center** `/seller/*` · **Admin Portal** `/admin/*`.

## 1. Bản đồ thư mục máy dev

```
~/Documents/TLCN/
├── genie-multivendor-platform/   ← REPO CODE (monorepo, git: github.com/qu2nishere/genie-multivendor-platform)
├── "HOÀN CHỈNH "/                ← BỘ TÀI LIỆU TỔNG THỂ (lưu ý: tên thư mục có DẤU CÁCH Ở CUỐI)
└── Design/                       ← logo, icon, code prototype (không cần đọc trừ khi được yêu cầu)
```

Tài liệu tham chiếu trong code ghi kiểu `TECH_STACK mục 8`, `TS 5.2`, `Document_Vi 3.7.1`, `API_SPEC 6.1`,
`ENTITY.md quy ước 9`, `plan G10.08`, `D3` — tra ở bảng mục 8 bên dưới.

## 2. Hiện trạng (2026-11-14) — CÁI GÌ ĐÃ CÓ, CÁI GÌ CÒN LÀ KHUNG

| Phần | Trạng thái |
|---|---|
| `web/` | **Xong giao diện** (plan G00–G13): 34 feature, storefront + auth + buyer + Seller + Admin, chạy trên **lớp mock** (`PUBLIC_USE_MOCK=true`). 237 hàm `*.api.ts` có nhánh mock; **21 endpoint lệch `API_SPEC.md`** — danh sách ở `docs/web-backend-handoff.md`. Vitest + 6 spec Playwright (22 kịch bản × 2 khổ). |
| `server-spring/` | **Entity xong, nghiệp vụ chưa.** 65 entity + 55 enum + 65 repository (rỗng) khớp 1-1 ENTITY.md, migration `V1…V12` thật, `SchemaValidationIT` xanh (plan `plan/2026-11-14/server-spring/implement-entity.md`, PR #28–#51). Có thật ở `common`: `ApiResponse`, `ErrorResponse`, `GlobalExceptionHandler`, `ErrorCode` (6 mã), `BaseEntity`, `JacksonConfig`, `ClockConfig`, `NameNormalizer`, `SystemLog`/`AuditAction`. **Còn stub:** security (`SecurityConfig`, `JwtService`, `JwtAuthFilter`, `CurrentUser`, `RateLimiter`, `InternalTokenFilter`), `AuditLogger`, `ObjectStorage`/`UploadController`, `Money`, `Haversine`; mọi service / controller; `V13__bootstrap_admin.sql`, `R__seed_demo.sql`. Entity theo ENTITY.md đặt ở `<module>/<feature>/domain` (vd `User` ở `auth/profile/domain`), quan hệ JPA sang module khác đã bỏ, chỉ giữ `Long xxxId`. **Catalog công khai có thật (plan 2026-11-17):** `GET /public/products` (FTS + trigram, lọc, facet), `/public/search/suggestions|trending`, `/public/products/{id}` (+`/variants`, `/views`, `/similar`), `/public/recommendations`, `/public/shops/{id}`; migration `V21__catalog_search_tuning.sql`; `common/cache/JsonCache`, `common/api/PageResponse`, ETag cho `/public/**`; test dựng dữ liệu bằng `support/TestDataBuilder`. |
| `server-python/` | **Khung.** Có `main.py`, `health.py`, `core/config.py`, `core/security.py`; `kyc/*`, `sentiment/*` là `# TODO`. |
| `deploy/` | Máy dev: `docker-compose.local.yml` (postgres, redis, silo, mailpit — ADR 0009). Production (ADR 0010): CSDL **AWS RDS**, tệp **AWS S3** (`deploy/aws/`: README, `s3-init.sh`, `iam-policy.json`); `docker-compose.yml` chỉ còn nginx, frontend, backend, ai-worker, redis; nginx 2 conf, backup `pg_dump` từ RDS, deploy.sh (chưa chạy thử thật). |
| `.github/` | `ci.yml` (paths-filter backend / web / ai-worker + commitlint tiêu đề PR + SonarCloud), `release.yml`, `deploy.yml`, dependabot (đích `develop`), CODEOWNERS (tên team còn là placeholder `@genie-team/...`). |

**Việc kế tiếp hợp lý:** phần nền `common` (security, audit, mail) rồi nghiệp vụ từng phân hệ, bắt đầu từ AUTH
→ nối web bằng `docs/web-backend-handoff.md`. Đổi schema từ giờ chỉ bằng migration mới `V14+` (ENTITY.md sửa trước). Plan mới đặt ở `plan/<yyyy-mm-dd>/<thể loại>/`
(mẫu: `plan/2026-11-11/web/plan_chuyen_giao_dien.md` + `progress_chuyen_giao_dien.md` — plan có mã task `Gxx.yy`,
progress là bảng tick + nhật ký có mã commit).

**Plan 2026-11-16 (Nguyễn Ngọc Thiện):** `admin/audit` — Nhật ký hệ thống UC-ADM-07 (`GET /admin/audit-logs`, `…/export`) đã có; `admin/banner` — Banner UC-ADM-12 (`/admin/banners`, `/public/banners` cache Redis, `/public/banner-events` + `BannerStatsFlushJob`, migration `V18__banner_audit_action.sql`, `UploadPurpose.BANNER_IMAGE` có `requiredAuthority`, `UploadService.promoteTo`, `common/api/PagePagination` + `ApiResponseFactory.page()`); `promotion/coin` — Điểm Genie UC-PRO-08/09 (`/me/coins…`, `/public/coin-rewards`, `CoinLedgerService` cộng / trừ theo lô + băm `coin:` HMAC, `CoinFacade` cho order / social sau này, `CoinExpiryJob` 00:15, migration `V19__coin_expired_type.sql`; `promotion/voucher/application/VoucherView` = `$Voucher`); `catalog/wishlist` — Sản phẩm yêu thích UC-CAT-08 (`/me/wishlist…`, projection `catalog/product/domain/ProductSummaryRows` nạp theo lô, `common/domain/Slugs` cùng quy tắc slug với web, `support/TestDataBuilder` dựng danh mục / shop / sản phẩm cho IT); `social/follow` — Theo dõi gian hàng UC-SOC-06 (`/me/following…`; cổng `social.follow.application.ShopFollowDirectory` do `vendor/shop/application/ShopFollowDirectoryImpl` cài; `UserFacade.activeAmong` lọc chủ shop ACTIVE theo lô; `ShopFollowFacade` cho notification / `GET /me`); `vendor` — Mở shop & KYC UC-VND-01/02/03/04/07/21 (`/me/shop-registrations…`, `/seller/me|kyc|settings|payout-account`, `/admin/kyc-submissions…`, `/admin/shops`, `/internal/kyc/{id}/result`, `/public/banks`; `ShopFacade` là cửa công khai; `common/crypto/AesGcmCipher` mã hóa giấy tờ, `KycDossierStorage` bản xem tạm 5 phút; `KycAiDispatcher` gửi AI sau commit, `KycDecisionService` chấm điểm `genie.kyc.risk.*`; `AI_WORKER_MODE=stub|http`; `OtpFacade.issue/verify` cho OTP của module khác; migration `V20__vendor_kyc_additions.sql`; ADR 0011); dùng chung mới: `common/api/ApiListResponse` + `ApiResponseFactory.list()` (danh sách có `pagination` / `summary` ở cấp gốc), `common/error/InvalidRequestException` (422 có `errors[]` theo trường), web `requestBlob` (tải tệp kèm Bearer).

**Plan 2026-11-21 (seed từ Excel):** `scripts/seed/excel/` — `excel_to_seed.py template | check | build [--apply] [--flush-cache]` chuyển `products.xlsx` + ảnh (`anh/`, không commit) thành `db/seed/R__seed_demo_95_excel_products.sql` + ảnh thu nhỏ `scripts/seed/excel/assets/` (commit); `upload-assets` đẩy và kiểm ảnh `product-media/seed/excel/`. Hướng dẫn: `scripts/seed/excel/README.md`.

**Plan 2026-11-22 (Nguyễn Ngọc Thiện):** `server-spring/plan_nhat_ky_banner_chi_tiet.md` — banner ghi nhật ký bằng 5 mã `BANNER_CREATED / UPDATED / ACTIVATED / DEACTIVATED / DELETED` (migration `V24__banner_audit_granular_actions.sql`; `BANNER_CHANGED` chỉ còn cho bản ghi cũ); `GET /admin/audit-logs` trả thêm `reference_id`, `user_agent`, `changes` (`AuditChange`); web `AuditLogDetail` (hộp chi tiết) + khóa `admin-system.audit.actions.*`. Migration mới tiếp theo từ `V25`.

**Plan 2026-11-24 (Nguyễn Ngọc Thiện — rà soát làm bù B1 / B2 / B3):** `plan/2026-11-24/server-spring/plan_hoan_thien_b1_b2_b3.md` — `auth/profile/application/SuspensionExpiryJob` (cron 5 phút mở đình chỉ hết hạn, `SKIP LOCKED`) + `AccountReactivatedEvent` → email `account-reactivated`; `promotion/voucher/application/PromotionSearchPorts` cài `ShopVoucherAvailability` (cache 60 giây); web: bộ lọc thuộc tính từ `summary.facets.attribute`, ô "Shop có voucher" theo `has_shop_voucher_supported`; Admin danh mục sửa / ẩn / xóa / bộ thuộc tính được ngành mọi cấp (`flattenTree`, `findCategory`); `common/config/ContentEtagHeaderFilter` (ETag `/public/**` bỏ khối `meta` → `304`). Kịch bản: `docs/kiem-thu/kich-ban-2026-11-24-b1-b2-b3.md`.

**Plan 2026-11-25 C2 (giỏ → checkout, UC-ORD-01/04/06/07/08 — nhánh xếp chồng `feature/c2-cart` → `c2-shipping-fee` → `c2-checkout-preview` → `c2-checkout-session` → `c2-order-sweeper`):** `order/cart` (API 6.1–6.7, `GET /public/variants`, giới hạn 100 dòng / 999); `catalog/product/application/VariantFacade` (C2 sở hữu — đọc dòng giỏ thẳng CSDL, `reserve / release / commitSale` bằng UPDATE có điều kiện, tăng `version`); `ShopFacade.buyInfo`; `shipping/fee` (`ShippingFeeQuoter`, bảng 15k / 30k, `HttpGhnFeeClient` bật bằng biến môi trường `GHN_MODE=http`); `order/checkout` (`CheckoutPricer` dùng chung cho `POST /me/checkout/preview` và `POST /me/checkout-sessions`, cổng `CheckoutDiscountPolicy` / `PaymentInitiator` / `CheckoutCompensation` với bản mặc định ở `CheckoutPorts` — C6 / C3 cài thật); `common/idempotency/IdempotencyStore` (Redis `SET NX`, khóa theo người dùng); `OrderCodeGenerator` (`TR-yyyyMMdd-######`, ngày giờ VN, nối tiếp CSDL khi Redis mất khóa); `order/reservation/application/ExpiredSessionSweeper` (60 giây); `catalog/product/application/ProductOrderGuard` + `order/order/application/OrderProductGuard`, `OrderAccountDeletionBlocker`. Test dựng dữ liệu bằng `support/OrderTestData`. Web: cờ `PUBLIC_REAL_API=cart,checkout`.

**Plan đang chạy:** `plan/2026-11-14/auth.md` — phân hệ AUTH (9 UC, task A00–A16, quyết định D2–D22 phải chốt trước — D1 đã đóng; tick task + ghi commit vào mục 10 của file). Tiến độ theo use case toàn hệ thống (87 UC, 5 lớp GD / BE / Test / Nối / Chạy): `docs/document/CHECKLIST_TIEN_DO_UC.md`.

## 3. Lệnh hay dùng

```bash
# gốc repo
npm install                                   # husky + commitlint + lint-staged
cp .env.example .env                          # biến máy dev (profile local + CSDL) — docs/cau-hinh-moi-truong.md
docker compose --env-file .env -f deploy/docker-compose.local.yml up -d   # postgres, redis, silo, mailpit (genie / changeme); cổng đổi được qua .env

# web/ (Node 22)
npm run dev          # http://localhost:4321 ; /dev/ui = thư viện component (chỉ dev)
npm run verify       # CỔNG BẮT BUỘC trước commit: lint → astro check + tsc → vitest → build → check-i18n
npm run test         # vitest (src/**/*.test.ts)
npm run test:e2e     # playwright trên bản build cổng 4322 (E2E_DEV=1 chạy trên dev server)
node ../scripts/i18n-lookup.mjs   # tra bản dịch

# server-spring/ (JDK 21)
./mvnw spring-boot:run   # JDK 21 (JDK 24+ làm Lombok lỗi); profile local lấy từ .env
./mvnw verify        # unit + Testcontainers + ArchUnit

# dữ liệu mẫu (máy dev — scripts/seed/README.md): backend profile local tự nạp khi GENIE_SEED_DEMO=true
scripts/seed/seed.sh --docker      # nạp lại R__seed_demo + db/seed vào Postgres Docker (Windows: seed.ps1 -Docker)
scripts/seed/upload-assets.sh      # ảnh mẫu sản phẩm / banner lên Silo

# server-python/ (Python 3.12)
./scripts/fetch-models.sh && cd server-python && python -m venv .venv && .venv/bin/pip install -e ".[dev]"
.venv/bin/uvicorn app.main:app --port 8000
```

Mock: đăng nhập `mai.tran@gmail.com` / `Genie@123`, OTP luôn `123456` (vừa là người mua vừa chủ shop TechZone).
`/admin` = Admin đủ 13 quyền; xem dưới quyền nhân viên: `sessionStorage['genie.mock.staff_title']` =
`MODERATOR | FINANCE_OFFICER | CS_SPECIALIST`. Trạng thái mock giữ ở `sessionStorage['genie.mock.*']`.
Theme: `?theme=genie|classic|dongho|dongson|hoian|polymer|buudien|saigon|sleeve`.

## 4. Git & commit (commitlint CHẶN khi sai)

- Nhánh: `main` (phát hành) · `develop` (tích hợp, nhánh mặc định làm việc) · `feature/GNE-<số>-<mo-ta>` ·
  `bugfix/…` · `release/vX.Y.Z` · `hotfix/…` — chữ thường, gạch nối.
- Commit: `<type>(<scope>)!: <subject> [#GNE-<số>]`
  - `type`: feat · fix · perf · refactor · test · docs · build · ci · chore · revert
  - `scope` (cố định, `commitlint.config.cjs`): auth · catalog · order · payment · vendor · shipping · promotion ·
    social · support · notification · admin · common · web · seller-ui · admin-ui · ai-worker · db · deploy · deps · release · api
  - `subject`: **tiếng Anh**, mệnh lệnh, **chữ thường**, không dấu chấm cuối; header ≤ 100 ký tự. **Body cũng viết tiếng Anh** (toàn bộ nội dung commit bằng tiếng Anh).
  - Chưa có mã Jira thì bỏ `[#GNE-…]` (lịch sử hiện tại đều bỏ).
- PR vào `develop`, squash merge, CI xanh, ≥ 2 duyệt, PR ≤ ~400 dòng. Release: tag `vX.Y.Z` trên `main`.
- **Không** `--no-verify`, không `--author`. Một commit = một thay đổi logic, build được, test xanh.
- Ví dụ: `feat(seller-ui): add voucher and flash sale screens`, `fix(web): resolve hydration issue on cart`.

**Quy trình commit / PR khi làm cùng Claude (bắt buộc):**
1. **Chỉ đưa lệnh `git commit` khi người dùng yêu cầu** — làm xong việc thì báo kết quả, không tự đưa lệnh commit, không tự commit / push.
2. Khi được yêu cầu: chia commit theo từng thay đổi logic, `git add` đúng từng file (không `git add .`), mỗi lệnh có chú thích `#` tiếng Việt giải thích commit dùng để làm gì; nội dung commit (tiêu đề + body) bằng tiếng Anh.
3. Tác giả commit là người dùng (`Khoa18062005`). **Không** thêm dòng `Co-Authored-By: Claude`, `Claude-Session` hay bất kỳ dấu vết AI nào vào commit / PR.
4. **Đưa lệnh commit xong mới hỏi lại** người dùng có cần tạo nội dung PR không — chỉ viết nội dung PR khi người dùng đồng ý. PR luôn vào `develop` (không phải `main`), theo mẫu `.github/pull_request_template.md`, tiêu đề PR đúng chuẩn commit (CI kiểm bằng commitlint).

**Cập nhật tiến độ sau mỗi việc (bắt buộc):**
1. Xong một task trong plan đang chạy (vd `plan/2026-11-14/auth.md`) → tick task, cập nhật bảng **Tiến độ** đầu file và ghi một dòng **Nhật ký** (ngày, việc đã làm, mã commit / PR).
2. Nếu việc đó làm xong (hoặc xong một phần) một **lớp** của use case liên quan — GD / BE / Test / Nối / Chạy — thì cập nhật luôn `docs/document/CHECKLIST_TIEN_DO_UC.md` theo đúng *Quy trình cập nhật* ở mục 0 của file đó: đổi ô `☐` → `☑` (một phần `◐`, ghi phần thiếu ở dòng chữ nhỏ), cột **Trạng thái**, số liệu **mục 1. Tổng quan** + bảng *Tiến độ theo phân hệ*, mục 3 (việc còn lại), một dòng **mục 5. Nhật ký**.
3. Đồng bộ cùng trạng thái sang cột *Hiện trạng mã nguồn* trong `docs/document/diagrams/UseCaseDiagram/Quản Lý UseCase.xlsx` và câu *Hiện trạng* của UC trong `Usecase.md` để các nơi khớp nhau.
4. Chỉ tick **Chạy** khi người dùng đã tự thao tác trên giao diện với hệ thống thật và báo đúng; UC chỉ **✅ Đã triển khai** khi đủ 5 lớp.

## 5. Quy ước FRONTEND `web/` (Astro 5 SSR + React 19 islands + Tailwind 3, TS strict)

**Kiểu trang** (mỗi màn đúng 1 kiểu):
- Storefront công khai → **SSR + islands**; dữ liệu lấy trong frontmatter `.astro` qua `features/<x>/server.ts` hoặc `index.ts`.
- Trang người mua cần đăng nhập (giỏ, checkout, đơn, tài khoản, chat) → **SSR khung + 1 island lớn `client:load`**, `noindex`.
- `/seller/*`, `/admin/*` → **SPA** `client:only="react"` (`src/app/{seller,admin}/` : `SellerApp`, `routes.tsx` lazy, `nav.ts`, `KycGate`, `RequirePermission`).
- Hydrate: tĩnh = không JS · `client:load` (cần ngay) · `client:idle` (yêu thích, theo dõi) · `client:visible` (dưới màn đầu).

**Khuôn một feature** `src/features/<x>/` — 4 tầng, đặt tên file giống nhau:
```
domain/          <x>.types.ts · <x>.rules.ts (hàm thuần) · <x>.schema.ts (zod) · <x>.rules.test.ts
infrastructure/  <x>.dto.ts (snake_case đúng API) · <x>.mock.ts · <x>.mapper.ts (DTO⇄domain) · <x>.api.ts · <x>.storage.ts
application/     use<X>.ts (TanStack Query) · <x>.store.ts (nanostores) · <luồng>.ts
ui/              <X>Page.tsx (island/màn) · <Khối>.tsx · <Khối>.astro
index.ts         API công khai DUY NHẤT cho feature khác / pages / app
server.ts        (tùy) export component .astro cho trang SSR — .tsx không được import
```
Thứ tự làm: types → rules + test → dto → mock → mapper → api → application → ui → index.ts → gắn pages/routes → khóa i18n.

**Luật phụ thuộc** (ESLint `boundaries` + `no-restricted-imports` chặn):
`ui → application → domain ← infrastructure`; `ui` không gọi thẳng `infrastructure`; `domain` chỉ import `shared/lib` +
`shared/auth/permissions.ts`; feature khác chỉ qua `features/<x>/index.ts|server.ts` (import sâu bị chặn);
dùng chung ≥ 2 feature → `shared/`; `shared/` **không** import `features/`.

**Mẫu `*.api.ts`** (ADR 0003) — chữ ký hàm là bản cuối, thân rẽ nhánh mock:
```ts
export async function fetchCart(): Promise<CartLine[]> {
  if (USE_MOCK) { await mockDelay(); return toCartLines(toCartDto(db.get())); }
  return toCartLines(await api.get<CartDto>('/me/cart'));
}
```
`shared/api/mock.ts`: `USE_MOCK`, `mockDelay`, `mockOk`, `mockFail('GNE-…')`, `mockState('<tên>', seed)`, `paginateCursor`.
Dữ liệu mock dùng chung nhiều feature: `shared/api/mock/{seed,seller,admin,banners,activity,placed}.ts`.
HTTP: `shared/api/http.ts` (`api.get/post…`, Bearer, refresh 1 lần khi 401, `idempotencyKey`), SSR: `server-http.ts`.
Tải tệp: `shared/api/upload.ts` (presigned PUT → Silo → gửi `object_key` trong request nghiệp vụ).

**Trạng thái:** server data = TanStack Query · chia sẻ giữa island = nanostores · form = react-hook-form + zod
(`shared/lib/zodResolver.ts`) · phiên = `shared/auth/session.ts` (token trong bộ nhớ) · trình duyệt = `shared/lib/storage.ts`
(tiền tố `genie.`). Quyền: `shared/auth/permissions.ts` (13 mã + preset theo chức danh) — FE chỉ ẩn/hiện.

**i18n:** mọi chuỗi qua `t()` (`useT()` trong island, `createT(Astro.locals.lang)` trong .astro). Khóa
`<feature>.<màn>.<ý nghĩa>`; dùng chung `common.action.*`, `common.state.*`, `common.label.*`; lỗi `errors.GNE-*`;
khung `layout.*`. Thêm khóa ở `vi.json` **phải** thêm ở `en.json` (check-i18n chặn). Tiền / ngày qua `shared/lib/format.ts`.

**CSS (ADR 0002):** component mới viết **utility Tailwind** theo token (`styles/tokens.css` → `tailwind.config.ts`), không
màu / px cứng. Lớp `tr-*` (storefront, `styles/storefront/*.css`, `@layer components`) và `dash-*` (`styles/dash.css`) giữ CSS.
`themes.css` không bọc `@layer`. Mọi màn mới kiểm tương phản ở **cả 9 theme** (ADR 0001).
Icon: `shared/ui/icon` (`<Icon name=…>`, kiểu `IconName`) — **không** thêm thư viện icon (ADR 0004).
UI kit: `shared/ui` (Button, Input, Modal, DataTable, Price, ProductCard, Voucher, Tabs…), `shared/ui/dash` (StatTile, TimeChart…),
`shared/ui/brand` (GenieLogo, GenieCoin, ThemePicker).

**SEO / HTML (R14–R19):** link thật `<a href>` (không onClick điều hướng); nội dung cần index phải có trong HTML SSR;
mỗi trang 1 `<h1>`; ảnh có alt/width/height; 404 thật; slug sai → 301; trang sản phẩm có JSON-LD Product + BreadcrumbList
(`layouts/partials/SeoHead.astro`, `shared/lib/seo.ts`). Route `kebab-case`, sản phẩm `/p/{slug}-{id}`.

**Không làm (ADR 0005):** phần chỉ để trình diễn của prototype (dock đèn thần, tweaks panel, DemoBox, PREVIEW_ROLES,
công tắc kịch bản mock). Không `any`. Không biến toàn cục `window.LZ*`.

## 6. Quy ước BACKEND `server-spring/` (Java 21, Spring Boot 3.3.5, modular monolith)

- Package: `com.genie.<module>.<feature>.<layer>`; 11 module = `auth catalog order payment vendor shipping promotion social support notification admin` + `common` (shared kernel).
- Tầng: `api` (controller, DTO `record` `*Request/*Response`, mapper) → `application` (`*Service` mở `@Transactional`, `*Facade` cửa cho module khác, listener, **port** interface) → `domain` (`@Entity` = rich domain model, enum, Spring Data repo) ← `infra` (adapter: VNPay, GHN, Gemini, Silo (S3), AI Worker). `event/` = record sự kiện công khai (chỉ ID + giá trị). Feature chỉ tạo tầng cần.
- Giữa module: **chỉ** `*Facade` + `event/` + DTO facade trả; FK sang module khác lưu `Long xxxId`; không vòng phụ thuộc (ArchUnit trong `ModuleBoundaryTest`).
- Tiền / tồn / Điểm / suất Flash Sale xử lý **trong transaction chính**; `@Async @TransactionalEventListener(AFTER_COMMIT)` chỉ cho thông báo, email, cache, gọi AI, thống kê.
- Entity: Lombok `@Getter @Setter @Builder @NoArgsConstructor(PROTECTED) @AllArgsConstructor`, **không `@Data`**; PK `BIGINT GENERATED ALWAYS AS IDENTITY`; tiền `NUMERIC(19,2)`/`BigDecimal` (cấm float/double); thời gian `TIMESTAMPTZ`/`OffsetDateTime`; enum `@Enumerated(STRING)` + `VARCHAR CHECK`; JSON `JSONB` (`*Json`); `@Version` trên 10 bảng nóng; snapshot dữ liệu giao dịch; sổ cái chỉ ghi thêm.
- Migration Flyway viết tay từ `ENTITY.md`; `ddl-auto=validate`; **không sửa migration đã merge** → `V<n+1>__mo_ta.sql`. Tên: bảng snake_case số nhiều (ngoại lệ `financial_ledger`, `vendor_shipping_config`), `idx_<bảng>_<cột>`, `uk_<bảng>_<ý nghĩa>`, `fk_<bảng>_<bảng đích>`.
- API: `/api/v1/{public|me|seller|admin|internal}/…`, JSON **snake_case**, vỏ `ApiResponse{code:"SUCCESS",status,message,data,meta}`, lỗi `GNE-<MIỀN>-<HTTP><STT>` (danh mục Document_Vi 3.7.1 / API_SPEC 15), validation → `422 GNE-VAL-4220`, `Idempotency-Key` bắt buộc cho checkout / thanh toán / rút / hoàn (Redis SET NX 24h; Redis lỗi → 503), phân trang cursor, tiền = số nguyên VND, `@Operation(summary="UC-XXX-NN …")`.
- Bảo mật: JWT HS256 15 phút, refresh 7 ngày băm SHA-256 xoay vòng (cookie HttpOnly), Argon2id, `@PreAuthorize` 13 mã quyền, kiểm sở hữu (IDOR) ở service, rate limit Redis.
- Đồng thời: `UPDATE … WHERE còn >= ?` + `@Version`; ví shop `FOR UPDATE` + `balanceHash` HMAC; cron `FOR UPDATE SKIP LOCKED`; IPN/webhook idempotent; Return URL chỉ hiển thị.
- Test: `*Test` = unit, `*IT` = tích hợp (Testcontainers PG + Redis); 5 kịch bản tiền bắt buộc (TECH_STACK 14).

## 7. Quy ước AI Worker `server-python/` & từ ngữ

- FastAPI, chỉ nhận header token nội bộ; `POST /kyc/verify` → callback `PATCH /api/v1/internal/kyc/{id}/result`;
  `POST /sentiment` → `PATCH /api/v1/internal/reviews/{id}/sentiment`. Tiền xử lý PhoBERT giống lúc huấn luyện.
- **Thương hiệu:** "Genie"; điểm thưởng **"Điểm Genie / Điểm"** (không "Xu"); **"Chính hãng" / "Shop Chính Hãng"** (không "Mall");
  mã lỗi `GNE-*` (không `LZD-*`); mã đơn `TR-yyyyMMdd-NNNNNN`; chuyển khoản `TR{id}`, hoàn `TRRF{id}`, rút `TRWD{id}`.
  Tên kỹ thuật cũ (`UserCoin`, `isMall`) giữ nguyên.
- Tham số nghiệp vụ: hoa hồng 3,5–8% theo ngành (mặc định 5%); voucher 3 tầng Shop → Sàn → Freeship, Shop+Sàn ≤ 85%;
  Điểm 1 = 1₫, tối đa 50% sau voucher; escrow giữ 7 ngày sau DELIVERED; giữ hàng 15 phút; KYC risk < 30 tự duyệt,
  30–69 người duyệt, ≥ 70 từ chối; rút tối thiểu 100.000đ; khiếu nại 7 ngày, shop phản hồi 48h.
- Hoa hồng: API nhận / trả **phần trăm** (`5.5`), CSDL lưu **phân số** (`0.0550`, `CommissionRates.toPercent / toFraction`).
  Tỷ lệ áp dụng cho một ngành luôn lấy qua `CategoryFacade.effectiveCommissionRate(categoryId)` (ngành → cha gần nhất →
  mặc định ở bảng `platform_settings` khóa `commission.default_rate`) — module khác không tự đọc `categories.commission_rate`.
- Comment code viết **tiếng Việt**, dòng đầu file thường ghi nguồn (vd `// … (plan G10.08; API_SPEC 6.1)`).

## 8. Tra tài liệu — `~/Documents/TLCN/HOÀN CHỈNH /` (thứ tự ưu tiên khi mâu thuẫn: TECH_STACK mục 12 → Usecase/ENTITY → Document_Vi → implement)

| Cần gì | Mở file |
|---|---|
| **Mọi quy ước code / repo / git / CI / hạ tầng** | `kien-thuc/TECH_STACK_VA_QUY_UOC.md` (TS) — mục 4 backend, 5 frontend, 7 dữ liệu, 8 API, 9 bảo mật, 10 đồng thời, 11 đặt tên, 12 điểm đã chốt, 13 git, 17 checklist S1 |
| Dữ liệu lưu ở đâu (PG / Redis / Silo / trình duyệt), 9 bucket | `kien-thuc/LUU_TRU_DU_LIEU.md` |
| Bảng, cột, enum, phương thức entity (SSOT CSDL) | `diagrams/ClassDiagram/ENTITY.md` (rất lớn — grep theo `## <nhóm>.<số>`); giải nghĩa tiếng Việt: `GIAI_NGHIA_ENTITY.md`; Java mẫu: `ClassDiagram/java-code/Diagram01…10*.java` |
| Đặc tả 87 use case (UC-AUTH/CAT/ORD/PAY/VND/LOG/PRO/SOC/SUP/NOT/ADM/AI) | `diagrams/UseCaseDiagram/Document/Usecase.md`; theo dõi tiến độ UC: `Quản Lý UseCase.xlsx` |
| Endpoint, request/response, mã lỗi, enum, bản đồ màn → API | `document/API_SPEC.md` (mục 0 quy ước, 14 màn→API, 15 mã lỗi, 16 enum) |
| Tài liệu gộp SRS + HLD + LLD (FR, NFR-01…15, ADR-01…14, sequence, state machine) | `document/Document_Vi.md` |
| Tách riêng | `document/BRD.md` · `PRD.md` · `SRS.md` · `HLD.md` · `LLD.md` (máy trạng thái LLD 6, thuật toán LLD 9, cron LLD 10) |
| Lộ trình 16 tuần, chi tiết hiện thực từng phân hệ | `kien-thuc/implement.md` |
| Kiến thức + câu hỏi bảo vệ | `kien-thuc/Kien_Thuc_Can_Nam.md` |
| Kinh doanh / chi phí & AI / pháp lý | `kien-thuc/BAO_CAO_KIEN_THUC_KINH_DOANH_KINH_TE_v1.0.md` · `BAO_CAO_DU_TOAN_CHI_PHI_VA_GIAI_PHAP_AI_v1.0.md` · `BAO_CAO_HE_THONG_CHINH_SACH_HOP_DONG_PHAP_LY_v1.0.md` |
| Vì sao chọn / bỏ UC so với DATN; đối chiếu giao diện ↔ tài liệu | `BAO_CAO_PHAM_VI_MVP.md` · `BAO_CAO_DOI_CHIEU_GIAO_DIEN.md` |

Trong repo: `README.md` (chạy local), `docs/adr/0001…0006` (quyết định web), `docs/web-backend-handoff.md`
(nối mock → API), `plan/2026-11-11/web/` (plan + progress chuyển giao diện, nguyên tắc R1–R19).

## 9. Bẫy đã biết

- Thư mục tài liệu tên `"HOÀN CHỈNH "` có dấu cách cuối — luôn đặt trong ngoặc kép.
- `web/` đang chạy mock: sửa logic giá / voucher phải sửa cả `*.mock.ts`; checkout mock nhận `CheckoutQuote` do client tính (khi nối thật thì bỏ).
- Khi nối backend: sửa 21 endpoint lệch spec trước (handoff mục 2), thêm trường `product_slug`, `order_id` vào spec (mục 3).
- `ErrorCode.java` mới có 6 mã; khi thêm mã mới phải thêm cả `web/src/shared/api/error-codes.ts` + khóa `errors.GNE-*` ở vi/en.
- Dependabot mở PR nâng Astro / Tailwind 4 — không gộp lẫn với việc tính năng.
- Cấu hình: mọi giá trị theo môi trường đọc từ `.env` (gốc repo, backend tự nạp — `DotEnvEnvironmentPostProcessor`); không ghi mật khẩu vào `application*.yml`. Đổi CSDL máy dev (Docker / cài máy) = đổi khối trong `.env`; production RDS + S3 chỉ khai ở `deploy/.env` — `docs/cau-hinh-moi-truong.md`.
- Kho tệp: một cài đặt `S3ObjectStorage` cho cả Silo (dev) và AWS S3 (prod); mã nghiệp vụ dùng tên bucket **logic** (`UploadPurpose.bucket()`), tiền tố `STORAGE_BUCKET_PREFIX` chỉ ghép trong `S3ObjectStorage` — không tự ghép ở nơi khác. Không seed / không trỏ máy dev vào RDS production.
- Seed: thư mục `db/seed` (Flyway location thứ hai, `SeedDataFlywayCustomizer`) — SQL thuần, không placeholder, chạy lại được, tra quan hệ bằng khóa tự nhiên; đổi schema làm seed hỏng thì `SeedDataIT` đỏ → sửa seed cùng PR. `balance_hash` của seed dùng khóa máy dev `local-wallet-secret`. Bản cũ `scripts/seed-local.sh` đổ thẳng vào `psql` không thay placeholder nên không chèn được gì — đã sửa (gọi `scripts/seed/seed.sh`).
- Seed từ Excel (plan `plan/2026-11-21/db/plan_seed_tu_excel.md`, `scripts/seed/excel/README.md`): `R__seed_demo_95_excel_products.sql` do `scripts/seed/excel/excel_to_seed.py build` **sinh tự động — không sửa tay**, sửa `scripts/seed/excel/products.xlsx` rồi build lại. Commit cùng lúc file 95 + `scripts/seed/excel/assets/` (ảnh thu nhỏ + `manifest.json`); **không** commit `anh/` (ảnh gốc, `.gitignore`). SKU `XL-%` đánh dấu sản phẩm Excel: bỏ khỏi file → `HIDDEN`, biến thể bỏ → xóa mềm. Đổi schema `products` / `product_variants` / `product_images` → sửa `xlseed/sql_writer.py` + sinh lại file 95 cùng PR.
- `shop_id` trên API = `vendor_profiles.id`; các bảng nghiệp vụ (`shop_follows.vendor_id`, `products.vendor_id`…) lưu `users.id` của chủ shop — đổi qua cổng / `ShopFacade` ở module `vendor`, đừng trộn hai id.
- Cổng đảo ngược của `vendor` chờ module khác cài: `PayoutAccountGuard` (payment — còn lệnh rút chờ), `SellerBadgeContributor` (order / chat — badge Seller Center), `KycAdminNotifier` (notification — báo hồ sơ KYC mới), `ShopBootstrapContributor` (payment — tạo ví shop; shipping đã cài). `vendor` đã cài `WishlistShopAvailability` và `AccountShopProvider`. UC-VND-05 (kho GHN) đã có (2026-11-25): `PUT /seller/pickup-address` lưu kho 2 cấp (quận = `NULL`), cổng `PickupShopRegistrar` chọn bằng `GHN_PICKUP_MODE=stub|http` (mặc định giả lập; GHN `shop/register` còn đòi mã quận cũ nên bản `http` chưa kiểm thật). Chạy backend thử máy dev: ghi đè `SMTP_HOST=localhost SMTP_PORT=1025` nếu `.env` trỏ Gmail. `ProductVariant` không có `@SQLRestriction` thật → truy vấn tự lọc `deleted_at IS NULL`; `Product.category`, `ProductVariant.product` là `@ManyToOne` EAGER → màn danh sách dùng projection, đừng nạp entity hàng loạt.
- Chuỗi C2: thời gian giữ hàng đọc `genie.checkout.reservation-ttl` (mặc định `PT15M`, chạy thử máy dev có thể truyền `--genie.checkout.reservation-ttl=PT2M`). `checkout_sessions.created_at` là `@CreationTimestamp` (giờ hệ thống) còn `expires_at` theo `Clock` — test có `MutableClock` đừng so hai cột này. `MutableClock.withZone` bỏ qua múi giờ → đổi múi giờ bằng `clock.instant().atZone(…)`. Ghi `product_variants.reserved_count / stock_count` chỉ qua `VariantFacade` (plan C2 mục 8.3).
- Hook husky chạy `lint-staged` qua binary trong `web/node_modules` (cần `npm ci` trong `web/` trước khi commit).
- `astro check` báo `Cannot find module 'node:…'` / `Buffer` → `web/node_modules` cũ thiếu `@types/node` (đã có trong lock) → `npm ci`.
- Extension Java của VS Code biên dịch chồng vào `server-spring/target/` → lỗi lạ kiểu `ClassNotFoundException: TestDataBuilder`, "Unresolved compilation problems" khi chạy Maven → chạy `./mvnw clean verify`.
- Catalog công khai: nối web bằng `PUBLIC_REAL_API=product,search,shop` (+ `catalog`, `admin-catalog`, `commission` = cây danh mục / quản lý danh mục / hoa hồng của plan B2 — đã có backend). `catalog.search.application.ShopVoucherAvailability` do `promotion/voucher/application/PromotionSearchPorts` cài (plan 2026-11-24 D5–D6): cache 60 giây khóa `cache:promotion:shop_voucher_vendors` — cụm B5 tạo / sửa / tắt voucher shop phải `JsonCache.evict("promotion:shop_voucher_vendors")` sau commit. Bộ lọc thuộc tính ngành trên web lấy từ `summary.facets.attribute` (không có endpoint `attribute-filters`). Khi sửa / ẩn sản phẩm gọi `ProductDetailService.evict(id)`; khi sửa shop gọi `PublicShopService.evict(shopId)` (cache Redis 5 / 10 phút). SQL đọc của tìm kiếm / trang shop JOIN thẳng bảng `vendor_profiles`, `products` (read-model, plan 2026-11-17 D6) — không import class module khác.
- IT dùng chung CSDL test (không xóa giữa các test): test tìm kiếm phải lọc theo shop / từ lạ `TestDataBuilder.uniqueWord()`; tầng trigram có thể bắt nhầm dữ liệu test khác → khẳng định "có / không có", đừng khẳng định đúng cả danh sách.
