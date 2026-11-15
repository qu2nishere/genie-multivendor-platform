# CLAUDE.md — Ngữ cảnh làm việc cho repo `genie-multivendor-platform`

> Đọc file này trước khi làm bất cứ việc gì. Mục tiêu: không phải đọc lại toàn bộ codebase / tài liệu.
> Cập nhật lần cuối: 2026-11-14 (sau khi gộp loạt entity PR #28–#51 vào `develop`; đang ở nhánh `feature/auth`).
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
| `server-spring/` | **Entity xong, nghiệp vụ chưa.** 65 entity + 55 enum + 65 repository (rỗng) khớp 1-1 ENTITY.md, migration `V1…V12` thật, `SchemaValidationIT` xanh (plan `plan/2026-11-14/server-spring/implement-entity.md`, PR #28–#51). Có thật ở `common`: `ApiResponse`, `ErrorResponse`, `GlobalExceptionHandler`, `ErrorCode` (6 mã), `BaseEntity`, `JacksonConfig`, `ClockConfig`, `NameNormalizer`, `SystemLog`/`AuditAction`. **Còn stub:** security (`SecurityConfig`, `JwtService`, `JwtAuthFilter`, `CurrentUser`, `RateLimiter`, `InternalTokenFilter`), `AuditLogger`, `ObjectStorage`/`UploadController`, `Money`, `Haversine`; mọi service / controller; `V13__bootstrap_admin.sql`, `R__seed_demo.sql`. Entity theo ENTITY.md đặt ở `<module>/<feature>/domain` (vd `User` ở `auth/profile/domain`), quan hệ JPA sang module khác đã bỏ, chỉ giữ `Long xxxId`. |
| `server-python/` | **Khung.** Có `main.py`, `health.py`, `core/config.py`, `core/security.py`; `kyc/*`, `sentiment/*` là `# TODO`. |
| `deploy/` | Đã viết: compose local + VPS, nginx 3 conf, minio init, redis/postgres conf, backup/restore, deploy.sh (chưa chạy thử thật). |
| `.github/` | `ci.yml` (paths-filter backend / web / ai-worker + commitlint tiêu đề PR + SonarCloud), `release.yml`, `deploy.yml`, dependabot (đích `develop`), CODEOWNERS (tên team còn là placeholder `@genie-team/...`). |

**Việc kế tiếp hợp lý:** phần nền `common` (security, audit, mail) rồi nghiệp vụ từng phân hệ, bắt đầu từ AUTH
→ nối web bằng `docs/web-backend-handoff.md`. Đổi schema từ giờ chỉ bằng migration mới `V14+` (ENTITY.md sửa trước). Plan mới đặt ở `plan/<yyyy-mm-dd>/<thể loại>/`
(mẫu: `plan/2026-11-11/web/plan_chuyen_giao_dien.md` + `progress_chuyen_giao_dien.md` — plan có mã task `Gxx.yy`,
progress là bảng tick + nhật ký có mã commit).

**Plan đang chạy:** `plan/2026-11-14/auth.md` — phân hệ AUTH (9 UC, task A00–A16, quyết định D2–D22 phải chốt trước — D1 đã đóng; tick task + ghi commit vào mục 10 của file).

## 3. Lệnh hay dùng

```bash
# gốc repo
npm install                                   # husky + commitlint + lint-staged
docker compose -f deploy/docker-compose.local.yml --env-file deploy/.env up -d   # postgres, redis, minio, mailpit

# web/ (Node 22)
npm run dev          # http://localhost:4321 ; /dev/ui = thư viện component (chỉ dev)
npm run verify       # CỔNG BẮT BUỘC trước commit: lint → astro check + tsc → vitest → build → check-i18n
npm run test         # vitest (src/**/*.test.ts)
npm run test:e2e     # playwright trên bản build cổng 4322 (E2E_DEV=1 chạy trên dev server)
node ../scripts/i18n-lookup.mjs   # tra bản dịch

# server-spring/ (JDK 21)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
./mvnw verify        # unit + Testcontainers + ArchUnit

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
  - `subject`: **tiếng Anh**, mệnh lệnh, **chữ thường**, không dấu chấm cuối; header ≤ 100 ký tự. Body được viết tiếng Việt.
  - Chưa có mã Jira thì bỏ `[#GNE-…]` (lịch sử hiện tại đều bỏ).
- PR vào `develop`, squash merge, CI xanh, ≥ 2 duyệt, PR ≤ ~400 dòng. Release: tag `vX.Y.Z` trên `main`.
- **Không** `--no-verify`, không `--author`. Một commit = một thay đổi logic, build được, test xanh.
- Ví dụ: `feat(seller-ui): add voucher and flash sale screens`, `fix(web): resolve hydration issue on cart`.

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
Tải tệp: `shared/api/upload.ts` (presigned PUT → MinIO → gửi `object_key` trong request nghiệp vụ).

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
- Tầng: `api` (controller, DTO `record` `*Request/*Response`, mapper) → `application` (`*Service` mở `@Transactional`, `*Facade` cửa cho module khác, listener, **port** interface) → `domain` (`@Entity` = rich domain model, enum, Spring Data repo) ← `infra` (adapter: VNPay, GHN, Gemini, MinIO, AI Worker). `event/` = record sự kiện công khai (chỉ ID + giá trị). Feature chỉ tạo tầng cần.
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
- Comment code viết **tiếng Việt**, dòng đầu file thường ghi nguồn (vd `// … (plan G10.08; API_SPEC 6.1)`).

## 8. Tra tài liệu — `~/Documents/TLCN/HOÀN CHỈNH /` (thứ tự ưu tiên khi mâu thuẫn: TECH_STACK mục 12 → Usecase/ENTITY → Document_Vi → implement)

| Cần gì | Mở file |
|---|---|
| **Mọi quy ước code / repo / git / CI / hạ tầng** | `kien-thuc/TECH_STACK_VA_QUY_UOC.md` (TS) — mục 4 backend, 5 frontend, 7 dữ liệu, 8 API, 9 bảo mật, 10 đồng thời, 11 đặt tên, 12 điểm đã chốt, 13 git, 17 checklist S1 |
| Dữ liệu lưu ở đâu (PG / Redis / MinIO / trình duyệt), 9 bucket | `kien-thuc/LUU_TRU_DU_LIEU.md` |
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
- Hook husky chạy `lint-staged` qua binary trong `web/node_modules` (cần `npm ci` trong `web/` trước khi commit).
