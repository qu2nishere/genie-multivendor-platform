# PLAN 2026-11-16 — Các use case độc lập + cụm B4 (Mở shop & KYC)

> **Nguồn chọn việc:** [`docs/Document/BAO_CAO_UC_DOC_LAP.md`](../../docs/Document/BAO_CAO_UC_DOC_LAP.md) (nhóm A — độc lập hoàn toàn, cụm B4 — Mở shop & KYC).
> **Tài liệu chuẩn:** bộ tài liệu trong repo `docs/Document/` (TLCN v1.4). Viết tắt dùng trong mọi plan: **TS** = `kien-thuc/TECH_STACK_VA_QUY_UOC.md` · **UC** = `diagrams/UseCaseDiagram/Usecase.md` · **API** = `document/API_SPEC.md` · **ENTITY** = `diagrams/ClassDiagram/ENTITY.md` · **LLD** = `document/LLD.md` · **LUU_TRU** = `kien-thuc/LUU_TRU_DU_LIEU.md` · **implement** = `kien-thuc/implement.md`.
> Khi các nguồn mâu thuẫn: TS mục 12 → UC / ENTITY → Document_Vi → implement; **đường dẫn và hình dạng request / response lấy theo API** (API mục 17 đã giải thích các chỗ khác UC).

## 1. Danh sách plan (mỗi plan một progress cùng tên)

| # | Thể loại | Plan | Progress | Use case | Mã task | Nhánh |
|---|---|---|---|---|---|---|
| 1 | `db/` | [plan_seed_du_lieu_mau.md](db/plan_seed_du_lieu_mau.md) | [progress](db/progress_seed_du_lieu_mau.md) | (nền dữ liệu cho mọi plan dưới) | `Sxx.yy` | `feature/db-seed-demo` |
| 2 | `server-spring/` | [plan_nhat_ky_kiem_toan.md](server-spring/plan_nhat_ky_kiem_toan.md) | [progress](server-spring/progress_nhat_ky_kiem_toan.md) | Tra cứu nhật ký kiểm toán hệ thống bất biến (UC-ADM-07) | `Lxx.yy` | `feature/admin-audit-log` |
| 3 | `server-spring/` | [plan_banner_trang_chu.md](server-spring/plan_banner_trang_chu.md) | [progress](server-spring/progress_banner_trang_chu.md) | Quản lý banner trang chủ & chiến dịch tĩnh (UC-ADM-12) | `Bxx.yy` | `feature/admin-home-banner` |
| 4 | `server-spring/` | [plan_diem_danh_diem_genie.md](server-spring/plan_diem_danh_diem_genie.md) | [progress](server-spring/progress_diem_danh_diem_genie.md) | Điểm danh nhận Điểm hàng ngày (UC-PRO-08) + Tra cứu số dư Điểm Genie & Lịch sử biến động Điểm (UC-PRO-09) | `Pxx.yy` | `feature/promotion-coin-checkin` |
| 5 | `server-spring/` | [plan_san_pham_yeu_thich.md](server-spring/plan_san_pham_yeu_thich.md) | [progress](server-spring/progress_san_pham_yeu_thich.md) | Quản lý danh sách sản phẩm yêu thích (UC-CAT-08) | `Wxx.yy` | `feature/catalog-wishlist` |
| 6 | `server-spring/` | [plan_theo_doi_gian_hang.md](server-spring/plan_theo_doi_gian_hang.md) | [progress](server-spring/progress_theo_doi_gian_hang.md) | Khách hàng theo dõi gian hàng (UC-SOC-06) | `Fxx.yy` | `feature/social-shop-follow` |
| 7 | `server-spring/` | [plan_mo_shop_kyc.md](server-spring/plan_mo_shop_kyc.md) | [progress](server-spring/progress_mo_shop_kyc.md) | Cụm B4: Đăng ký mở gian hàng (UC-VND-01) · Tải hồ sơ pháp lý & Giấy tờ định danh KYC (UC-VND-02) · phần backend của Thẩm định KYC tự động qua AI (UC-VND-03) · Kiểm duyệt thủ công hồ sơ KYC (UC-VND-04) · Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID (UC-VND-05) · Đăng ký tài khoản nhận giải ngân (UC-VND-07) · Cập nhật thông tin gian hàng & cài đặt vận hành (UC-VND-21) | `Kxx.yy` | `feature/vendor-shop-kyc` (+ nhánh con) |
| 8 | `server-python/` | [plan_kyc_ai_worker.md](server-python/plan_kyc_ai_worker.md) | [progress](server-python/progress_kyc_ai_worker.md) | Thẩm định KYC tự động qua AI — QR, MRZ & Face Match (UC-VND-03, phần AI Worker) | `Yxx.yy` | `feature/ai-worker-kyc` |

Mỗi plan `server-spring/` gồm cả giai đoạn cuối **nối giao diện `web/` với API thật** (scope commit `web` / `seller-ui` / `admin-ui`) — giao diện đã dựng xong trên lớp mock nên phần web chỉ là bỏ nhánh mock, sửa đường dẫn lệch spec và kiểm thử; không tách plan `web/` riêng để mỗi use case vẫn là một plan khép kín.

## 2. Thứ tự & phụ thuộc

```
[1] Seed dữ liệu mẫu ──┬──► [5] Sản phẩm yêu thích      (cần seed sản phẩm)
                       ├──► [6] Theo dõi gian hàng        (cần seed shop đã KYC)
                       ├──► [3] Banner trang chủ          (seed banner mẫu — không bắt buộc)
                       └──► [7] Mở shop & KYC             (seed hàng chờ KYC cho màn Admin)
[2] Nhật ký kiểm toán      — không phụ thuộc (đọc system_logs phân hệ AUTH đang ghi)
[4] Điểm danh & Điểm Genie — không phụ thuộc (phần "Đổi quà" cần voucher seed ở [1])
[7] Mở shop & KYC ◄──────── [8] AI Worker KYC  (hợp đồng HTTP chốt ở K00 / Y00; [7] có chế độ giả lập AI nên không phải chờ [8])
[7] giai đoạn K07 (kho lấy hàng GHN) cần danh mục hành chính GHN của plan AUTH (A12.01–A12.02) — chưa có thì dùng StaticRegionDirectory
```

- Plan **[2], [3], [4]** làm ngay được, song song.
- Plan **[1]** nên làm trước hoặc song song với [5], [6], [7]: giai đoạn S01–S03 (khung + tài khoản + danh mục) đủ để các plan khác bắt đầu.
- Plan **[7]** và **[8]** làm song song bởi hai người (backend / AI); hợp đồng chốt ở K00.02 ↔ Y00.02 (ADR 0010), ghép chạy thật ở K05 ↔ Y07.

## 3. Điểm dùng chung — đọc trước khi mở nhánh (tránh conflict)

| Điểm chung | Quy ước giữa các plan |
|---|---|
| **Số migration Flyway** | Đăng ký ở bảng mục 4. Hai nhánh không được dùng cùng một số. Nhánh nào merge sau phải rebase, **đổi số** nếu số đó đã bị chiếm, và (nếu sửa `ck_system_logs_action_type`) chép đủ giá trị của migration đã merge trước |
| `common/error/ErrorCode.java` + `web/src/shared/api/error-codes.ts` + khóa `errors.GNE-*` (`vi.json`, `en.json`) | Mỗi plan liệt kê sẵn các mã sẽ thêm (mục 4 của plan). Thêm theo đúng thứ tự nhóm miền trong file; rebase `develop` trước khi mở PR |
| `common/storage/UploadPurpose.java` | Plan [3] thêm `BANNER_IMAGE`; plan [7] thêm `KYC_DOCUMENT`, `SHOP_LOGO`, `SHOP_COVER`. Mỗi purpose một dòng — conflict chỉ là thêm dòng |
| `common/audit/AuditAction.java` | Chỉ thêm giá trị kèm migration đã đăng ký; không plan nào đổi tên giá trị cũ (NFR-12) |
| `vendor/shop/application/ShopFacade` | **Plan [7] sở hữu.** Plan [5], [6] cần tra shop thì khai **cổng đảo ngược** trong module của mình (mặc định an toàn khi chưa có cài đặt) — không tự tạo `ShopFacade` |
| Định danh shop trên API (`shop_id`) | `shop_id` = `vendor_profiles.id`; các bảng nghiệp vụ lưu `vendor_id` = `users.id` của chủ shop (ENTITY). Chuyển đổi nằm ở `ShopFacade` (plan [7]) / cổng đảo ngược — quyết định D1 của plan [6], [7] |
| `docs/Document/*` | Đổi schema / mã lỗi / đường dẫn phải sửa tài liệu **trước** (ENTITY → migration → entity — TS 7.3). Tài liệu nằm trong repo nên đi cùng PR, commit `docs(<scope>): …` |
| `docs/web-backend-handoff.md`, `docs/Document/CHECKLIST_TIEN_DO_UC.md`, `Quản Lý UseCase.xlsx` | Mỗi plan chỉ sửa dòng của use case mình ở giai đoạn cuối |

## 4. Sổ đăng ký migration

| Số | File | Plan | Nội dung |
|---|---|---|---|
| `V15` | `V15__vendor_kyc_additions.sql` | [7] Mở shop & KYC (K01) | 9 cột mới + 3 index của `vendor_profiles`, 4 giá trị `AuditAction` của vendor |
| `V16` | `V16__banner_audit_action.sql` | [3] Banner trang chủ (B01) | Giá trị `AuditAction.BANNER_CHANGED` |
| `V17` | `V17__coin_expired_type.sql` | [4] Điểm danh & Điểm Genie (P06 — chỉ khi làm phần Điểm hết hạn) | Giá trị `CoinTransactionType.EXPIRED` |
| — | `db/seed/R__seed_demo_*.sql` | [1] Seed | Migration lặp lại (repeatable), không chiếm số `V` |

> Nếu plan [3] merge trước plan [7] thì đổi thành `V15__banner_audit_action.sql` / `V16__vendor_kyc_additions.sql` — sửa bảng này trong cùng PR.

## 5. Quy ước chung của mọi plan (trích TS mục 13, 14 — không lặp lại ở từng plan)

- **Nhánh:** tách từ `develop` mới nhất, tên `feature/GNE-<số>-<mo-ta>` chữ thường gạch nối; **chưa có mã Jira thì bỏ `GNE-<số>-`** (tiền lệ plan 2026-11-11 D6, plan AUTH). PR vào `develop`, **squash merge**, CI xanh, ≥ 2 duyệt, PR ≤ ~400 dòng (lớn hơn thì tách theo nhóm task — mỗi nhóm đã có commit riêng). Trước khi xin duyệt: `git pull --rebase origin develop` (không merge `develop` ngược vào nhánh nhiều lần — TS 13.2).
- **Commit:** `<type>(<scope>)!: <subject> [#GNE-<số>]` — `subject` + body **tiếng Anh**, mệnh lệnh, chữ thường, không dấu chấm cuối, header ≤ 100 ký tự; `scope` chỉ trong danh sách `commitlint.config.cjs`. Một commit = một thay đổi logic, build được, test xanh. Migration đi cùng commit dùng nó; chỉ có migration → scope `db`.
- **Người commit:** danh tính git đã cấu hình sẵn trong repo (`git config user.name` / `user.email`) — **không** `--author`, **không** `--no-verify`, **không** dòng `Co-Authored-By` hay dấu vết AI trong commit / PR.
- **Mỗi nhóm task kết thúc bằng một task `COMMIT`**, chỉ làm sau khi qua *Cổng kiểm tra* của plan; xong thì tick ở file progress và điền mã commit 7 ký tự.
- **Định nghĩa "xong" một use case (TS 13.7):** code + test + migration + OpenAPI (`@Operation(summary = "UC-…")`) + chuỗi i18n vi / en + chạy được end-to-end + cập nhật `CHECKLIST_TIEN_DO_UC.md` và `Quản Lý UseCase.xlsx`.
- **Nhật ký:** cuối mỗi file progress có bảng *Nhật ký* (ngày · việc đã làm · mã commit) — ghi sau mỗi buổi làm, giống `plan/2026-11-11/web/progress_chuyen_giao_dien.md`.

## 6. Tiến độ các plan & tick checklist use case

**Luật:** xong plan nào (hoặc xong từng use case trong plan) thì **tick ngay** use case đó ở [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — đổi các ô BE / Test / Nối / Chạy, cột Trạng thái, số ở mục 1 *Tổng quan*, thêm dòng ở mục 5 *Nhật ký cập nhật* — rồi đổi cột *Hiện trạng mã nguồn* trong `Quản Lý UseCase.xlsx` và tick dòng tương ứng ở bảng dưới. Việc này đi cùng commit `docs(<scope>): …` cuối của plan. Mỗi file progress có sẵn mục **"Chốt plan — tick tiến độ use case"** liệt kê đúng các ô cần tick.

| Thứ tự gợi ý | Plan | Use case phải tick ở checklist khi xong | Xong plan |
|:---:|---|---|:---:|
| 1 | [1] Seed dữ liệu mẫu | — (không gắn use case; chỉ ghi Nhật ký) | ☐ |
| 1 (song song) | [2] Nhật ký kiểm toán | `UC-ADM-07` | ☐ |
| 1 (song song) | [3] Banner trang chủ | `UC-ADM-12` | ☐ |
| 1 (song song) | [4] Điểm danh & Điểm Genie | `UC-PRO-08`, `UC-PRO-09` | ☐ |
| 2 (sau S01–S04 của seed) | [5] Sản phẩm yêu thích | `UC-CAT-08` | ☐ |
| 2 (sau S01–S03 của seed) | [6] Theo dõi gian hàng | `UC-SOC-06` | ☐ |
| 2 (sau S01–S03; S06 cho màn Admin) | [7] Mở shop & KYC | `UC-VND-01`, `UC-VND-02`, `UC-VND-03` (`◐` cho tới khi [8] xong), `UC-VND-04`, `UC-VND-05`, `UC-VND-07`, `UC-VND-21` | ☐ |
| 2 (song song với [7]) | [8] AI Worker KYC | `UC-VND-03` (đủ `☑` khi cả [7] K05 và [8] Y07 xong) | ☐ |
