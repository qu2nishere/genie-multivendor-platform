# PLAN — Công cụ seed sản phẩm từ file Excel (Excel → SQL seed + ảnh lên Silo)

| | |
|---|---|
| **Ngày lập** | 2026-11-21 |
| **Thể loại** | db (dữ liệu mẫu máy dev) + script |
| **Mục tiêu** | Thành viên nhập sản phẩm (tên, mô tả, giá, biến thể, thông số, **nhiều ảnh**) vào **một file Excel**, chạy **một lệnh** là có: ảnh đã chuẩn hóa nằm trong repo + đã lên Silo máy dev, file SQL seed nằm trong `db/seed` (vào git, cả nhóm dùng chung), dữ liệu đã nạp vào Postgres local |
| **Không làm** | API tạm không cần quyền; giao diện WinForms; đẩy lên AWS RDS / S3 production (ADR 0010) |
| **Kế thừa** | [plan seed 2026-11-16](../../2026-11-16/db/plan_seed_du_lieu_mau.md) — giữ nguyên N1–N10 của plan đó |
| **Tài liệu chuẩn** | ENTITY 13.2 · LUU_TRU 5.5 (khóa đối tượng) · TS 7.3 (migration) · [scripts/seed/README.md](../../../scripts/seed/README.md) |
| **Phụ trách** | _(điền tên)_ |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |

---

## Tiến độ

| Nhóm | Task | Xong |
|---|---|---|
| E00 Chốt quyết định | 1 | 0 |
| E01 Khung công cụ | 3 | 0 |
| E02 Đọc & kiểm tra Excel | 4 | 0 |
| E03 Ảnh | 5 | 0 |
| E04 Sinh SQL | 4 | 0 |
| E05 Nạp & đẩy ảnh | 4 | 0 |
| E06 Kiểm thử | 3 | 0 |
| E07 Tài liệu | 3 | 0 |
| **Tổng** | **27** | **0** |

---

## 0. Hiện trạng đã khảo sát

| Phần | Có gì | Ảnh hưởng tới công cụ |
|---|---|---|
| `products` (V2, V21) | Không có cột `slug` (backend sinh từ tên); `base_price NUMERIC(19,2)`; `status` CHECK 7 giá trị; `item_condition` NEW/USED/REFURBISHED; `shipping_payer` BUYER/SELLER; `weight` = gram; `search_vector` là cột GENERATED | Chống trùng sản phẩm theo `(vendor_id, name)` như seed 40; không cần điền search_vector |
| `product_variants` | `sku` UNIQUE, `attributes_json`, `price`, `stock_count`, `image_url` | SKU là khóa tự nhiên → `ON CONFLICT (sku)` |
| `product_images` | `image_url` (khóa đối tượng), `variant_id`, `width/height/file_size/mime_type`, `is_primary`, `display_order` | Công cụ đo ảnh bằng Pillow để điền đủ cột |
| `R__seed_demo_40_products.sql` | Mẫu chuẩn: bảng tạm `seed_product` / `seed_variant` từ `VALUES`, JOIN `users.email`, `vendor_profiles`, `categories.slug`, `brands.name`; cập nhật `active_product_count` | SQL sinh ra **đi đúng khuôn này** |
| `scripts/seed/seed.sh` · `seed.ps1` | Tự nhặt mọi `db/seed/R__seed_demo_*.sql`, mỗi file một transaction `ON_ERROR_STOP`, chỉ host local, `--only NN` | File mới tự được nạp, không phải sửa script nạp |
| `scripts/seed/upload-assets.sh` · `.ps1` | `mcli mirror` từ `web/public/img/products` → `product-media/seed/product/`, chặn `amazonaws.com` | Chỉ cần thêm một thư mục nguồn nữa — công cụ Python **không** tự nói chuyện với S3 |
| `SeedDataIT` | Chạy Flyway `db/migration` + `db/seed` trên PG thật, chạy lại lần 2 không đổi số dòng | File Excel sinh ra tự được kiểm; schema đổi làm hỏng → CI đỏ |
| Máy dev | Python 3.12 có sẵn | Công cụ viết Python, phụ thuộc `openpyxl` + `Pillow` |

---

## 1. Nguyên tắc riêng của công cụ

| # | Nguyên tắc |
|---|---|
| X1 | **Công cụ chỉ chuyển đổi, không ghi CSDL / S3 trực tiếp.** Đầu ra là (a) file SQL trong `db/seed`, (b) ảnh trong thư mục repo. Nạp CSDL dùng `seed.sh`, đẩy ảnh dùng `upload-assets.sh` — giữ nguyên lớp chặn host local / chặn AWS đã có |
| X2 | **Kiểm hết rồi mới ghi**: đọc toàn bộ file, gom mọi lỗi kèm `Sheet!Ô` (vd `SanPham!D7: giá phải là số nguyên VND`), có lỗi → in danh sách, thoát mã 1, không ghi file nào |
| X3 | **Đầu ra tất định**: cùng file Excel → cùng SQL, cùng tên ảnh (băm nội dung). Chạy lại không sinh diff giả trong git |
| X4 | **SQL sinh ra tuân thủ N1–N3, N6, N7 của plan seed**: chạy lại được, không ghi cứng id, SQL thuần, ảnh lưu khóa đối tượng |
| X5 | **Tham chiếu sai phải nổ to**: SQL kết thúc bằng khối `DO $$ … RAISE EXCEPTION` nếu có dòng không khớp chủ shop / danh mục / thương hiệu bắt buộc — không âm thầm bỏ qua như `JOIN` thường |
| X6 | Ảnh trong repo đã **chuẩn hóa** (JPEG, cạnh dài ≤ 1200px, chất lượng 85) để repo không phình |
| X7 | **Người dùng lại không cần Python**: chỉ `git pull` → `upload-assets` → chạy backend (hoặc `seed.sh`). Python chỉ cần cho người sửa Excel |

---

## 1b. Ảnh đi từ Excel lên giao diện thế nào (đọc trước khi làm)

**Mỗi bên giữ một vai trò:**

| Thành phần | Vai trò |
|---|---|
| Tool Python | Thu nhỏ ảnh gốc, đặt tên theo băm nội dung, ghi vào `assets/`, sinh SQL |
| Git | Chuyển ảnh (`assets/`) + SQL sang máy người khác — Silo mỗi máy là kho riêng, không thông nhau |
| Silo | Máy chủ phát file ảnh cho trình duyệt (chuẩn S3, giống AWS S3 production). **Không** chuyển đổi ảnh |
| CSDL | Chỉ nhớ **khóa ảnh** của sản phẩm (`seed/excel/a3f9c2e1.jpg`), không chứa ảnh |

**Hai folder ảnh:**

| Folder | Là gì | Commit | Máy khác cần để hiển thị? |
|---|---|---|---|
| `anh/` | Ảnh gốc người nhập bỏ vào, có thể nặng | **Không commit** (`.gitignore`) | Không |
| `assets/` | Bản thu nhỏ do tool sinh, tên `<băm8>.jpg` + `manifest.json` (tên tệp gốc → tên băm) | **Bắt buộc** | **Có** — `upload-assets` đẩy folder này lên Silo |

Vì `anh/` không lên git, người khác sửa Excel (đổi giá, mô tả…) mà không có ảnh gốc vẫn `build` được: tool tra `manifest.json`,
ảnh gốc vắng nhưng đã có bản thu nhỏ trong `assets/` thì dùng lại bản đó. Chỉ **ảnh mới** mới cần có tệp gốc trong `anh/` của người chạy.

**Vì sao đường dẫn ảnh khớp trên mọi máy:** URL ảnh = `địa chỉ Silo` + `bucket` + `khóa ảnh`. Chỉ **khóa ảnh** ghi trong SQL (cố định, nằm trong git).
Địa chỉ Silo (`MINIO_ENDPOINT`) và tiền tố bucket (`STORAGE_BUCKET_PREFIX`) **không** ghi vào SQL — backend và `upload-assets` cùng đọc từ `.env`
của máy đó, backend ghép lúc trả API (`catalog/product/application/MediaUrls`). Ô ảnh là link `https://…` thì `MediaUrls` giữ nguyên, không qua Silo.

```
Trình duyệt mở trang sản phẩm
  → Backend đọc CSDL: image_url = 'seed/excel/a3f9c2e1.jpg'
  → API trả: http://localhost:9000/<tiền tố>product-media/seed/excel/a3f9c2e1.jpg
  → Trình duyệt tải ảnh từ Silo → hiện ảnh
```

**Hai luồng làm việc:**

| Người tạo dữ liệu (cần Python) | Người dùng lại (không cần Python) |
|---|---|
| 1. Điền `products.xlsx`, bỏ ảnh vào `anh/` | 1. `git pull` |
| 2. `python excel_to_seed.py build products.xlsx --apply` | 2. `scripts/seed/upload-assets.sh` (Windows: `.ps1`) — đẩy `assets/` lên Silo máy mình |
| 3. Commit: file SQL 95, `assets/` (kèm `manifest.json`), `products.xlsx` — **không** commit `anh/` | 3. Chạy backend (profile local tự nạp vì `GENIE_SEED_DEMO=true`) **hoặc** `scripts/seed/seed.sh --docker` |
| | Quên bước 2 → sản phẩm vẫn hiện nhưng ảnh vỡ; chạy lại `upload-assets` là xong |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Đề xuất | Chốt |
|---|---|---|---|
| D1 | Ảnh nằm ở đâu để **cả nhóm** có (Silo mỗi máy là kho riêng; ảnh chỉ trên Silo máy người tạo thì máy khác vỡ ảnh) | Công cụ ghi bản thu nhỏ vào `scripts/seed/excel/assets/<băm8>.jpg` + `assets/manifest.json` — **bắt buộc commit**; ảnh gốc `anh/` **không commit** (đưa vào `.gitignore`), người khác sinh lại SQL nhờ `manifest.json`. `upload-assets` mirror `assets/` → `product-media/seed/excel/`. Khóa đối tượng trong SQL: `seed/excel/<băm8>.jpg` (xem mục 1b) | ☑ 2026-11-21 |
| D2 | Cách đưa ảnh vào Excel | Ô `anh` nhận 3 dạng, ưu tiên theo thứ tự: **(1) Chính — tên tệp** trong folder `anh/` cạnh file Excel, nhiều ảnh cách nhau `;` (giữ chất lượng, file Excel nhẹ). **(2) Link `https://…`** — ghi thẳng vào SQL, không tải về, không qua Silo (nhanh nhưng phụ thuộc web ngoài; cảnh báo khi dùng). **(3) Phụ — ảnh chèn thẳng vào Excel** kiểu *Insert → Pictures → Place over Cells* (`openpyxl` đọc ô neo). Ảnh kiểu *Place in Cell* (Excel 365) → **báo lỗi rõ** hướng dẫn chèn lại; hỗ trợ ở E03.05 (cắt được) | ☐ |
| D3 | Tên file SQL / thứ tự chạy | `R__seed_demo_95_excel_products.sql` — chạy sau 40 (sản phẩm tay) và 90 (bulk), dùng `seed.sh --only 95` để nạp riêng | ☐ |
| D4 | Sửa dòng trong Excel rồi chạy lại thì sao | Sản phẩm khớp theo `(email chủ shop, tên)`: **cập nhật** mô tả, giá, danh mục, thông số, trạng thái (`UPDATE … FROM`); biến thể `ON CONFLICT (sku) DO UPDATE` giá + tồn + tên; ảnh của sản phẩm Excel **xóa rồi chèn lại** (chỉ dòng có khóa `seed/excel/%`). Đổi **tên** sản phẩm = sản phẩm mới (ghi rõ trong README) | ☐ |
| D5 | Một hay nhiều file Excel | Một file nguồn duy nhất `scripts/seed/excel/products.xlsx` (commit) → một file SQL. Muốn tách thì nhiều sheet `SanPham` không cần — giữ đơn giản | ☐ |
| D6 | Mã SKU | `XL-<ma_sp>-<ma_bien_the>` (vd `XL-TN01-BLK`); sản phẩm không có biến thể → tự sinh một SKU `XL-<ma_sp>-STD` "Mặc định" với giá = `gia`, tồn = `ton_kho` | ☐ |
| D7 | Danh sách chọn shop / danh mục trong file mẫu | Lệnh `template` đọc **file seed hiện có** (`R__seed_demo_10/30`) bằng biểu thức chính quy để lấy slug danh mục + email chủ shop → sheet ẩn `_DanhMuc`, `_Shop` + Data Validation. Không cần kết nối CSDL. Nếu regex quá mong manh → chuyển sang `--from-db` (psycopg, cùng biến `DB_*`) | ☐ |
| D8 | Cache Redis sau khi nạp (trang sản phẩm cache 5 phút, trang shop 10 phút) | `--apply` in nhắc + tùy chọn `--flush-cache` xóa khóa catalog trong Redis Docker (`redis-cli --scan --pattern '<tiền tố catalog>*' \| xargs DEL`) — tra tiền tố trong `ProductDetailService` / `PublicShopService` khi làm E05.03 | ☐ |

---

## 3. Cấu trúc file Excel (bản đề xuất — duyệt ở E00)

**Sheet `SanPham`** — mỗi dòng một sản phẩm, dòng 1 là tiêu đề:

| Cột | Bắt buộc | Kiểu / giá trị | Vào cột DB |
|---|---|---|---|
| `ma_sp` | ✔ | chữ-số, duy nhất trong file (vd `TN01`) | chỉ dùng nối sheet + SKU |
| `email_chu_shop` | ✔ | email chủ shop đã có trong seed | `vendor_id` (tra `users.email`) |
| `slug_danh_muc` | ✔ | slug danh mục con | `category_id` |
| `ten` | ✔ | ≤ 255 ký tự | `name` |
| `mo_ta` | | văn bản dài | `description` |
| `thuong_hieu` | | tên; khớp `brands.name` thì gắn `brand_id` | `brand`, `brand_id` |
| `tinh_trang` | | `NEW` / `USED` / `REFURBISHED` (mặc định `NEW`) | `item_condition` |
| `xuat_xu` | | | `origin` |
| `gia` | ✔ | số nguyên VND > 0 | `base_price` |
| `ton_kho` | khi không có biến thể | số nguyên ≥ 0 | SKU `STD` |
| `can_nang_g` | ✔ | số nguyên gram > 0 | `weight` |
| `dai_cm` `rong_cm` `cao_cm` | | số | `length/width/height` |
| `mien_phi_ship` | | `x` / trống | `shipping_payer` SELLER / BUYER |
| `dat_truoc_ngay` | | số ngày | `is_pre_order`, `pre_order_days` |
| `trang_thai` | | `PUBLISHED` (mặc định) / `DRAFT` / `HIDDEN` | `status`, `is_published` |
| `da_ban` · `diem_sao` | | số — để test sắp xếp | `sold`, `rating` |
| `anh` | ✔ ≥ 1 ảnh | tên tệp trong `anh/`: `tai-nghe-1.jpg; tai-nghe-2.jpg` **hoặc** link `https://…` **hoặc** ảnh chèn nổi trong ô — ảnh đầu là ảnh chính | `product_images` |
| `anh_2` … `anh_8` | | chỉ dùng khi chèn ảnh thẳng vào Excel (mỗi ô một ảnh); dùng tên tệp thì ghi hết vào `anh` | `product_images.display_order` |

Ví dụ (cách chính — tên tệp):

| ma_sp | email_chu_shop | slug_danh_muc | ten | gia | can_nang_g | anh |
|---|---|---|---|---|---|---|
| TN01 | mai.tran@gmail.com | thiet-bi-am-thanh | Tai nghe Bluetooth ANC | 389000 | 350 | `tai-nghe-1.jpg; tai-nghe-2.jpg` |
| AT01 | owner.boutique@genie.example | thoi-trang-nam | Áo thun basic | 89000 | 250 | `ao-thun-den.png` |

**Sheet `BienThe`** (tùy chọn): `ma_sp` · `ma_bien_the` · `ten_bien_the` · `thuoc_tinh` (`color=Đen; size=M` → `attributes_json`) · `gia` · `ton_kho` · `anh` (ảnh riêng của biến thể → `product_variants.image_url` + `product_images.variant_id`).

**Sheet `ThongSo`** (tùy chọn): `ma_sp` · `khoa` · `gia_tri` → gộp thành `specs_json` (khóa nên trùng `category_attributes.code` để bộ lọc dùng được — cảnh báo nếu không trùng).

**Sheet `HuongDan`** + sheet ẩn `_DanhMuc`, `_Shop`: do lệnh `template` sinh.

---

## 4. Cây file sẽ tạo / sửa

```
scripts/seed/excel/
├── excel_to_seed.py          (mới) CLI: build | template | check
├── xlseed/                   (mới) gói nội bộ
│   ├── reader.py             đọc 3 sheet → dataclass, gom lỗi theo ô
│   ├── images.py             lấy ảnh nổi / ảnh theo tên tệp, chuẩn hóa, băm
│   ├── sql_writer.py         sinh SQL theo khuôn seed 40 + khối DO kiểm tham chiếu
│   └── template.py           sinh file mẫu có Data Validation
├── tests/test_*.py           (mới) pytest: đọc, kiểm lỗi, SQL tất định
├── requirements.txt          openpyxl, Pillow (pytest cho dev)
├── excel_to_seed.bat         (mới) kéo thả file .xlsx vào để chạy build --apply
├── products.xlsx             (mới) file nguồn của nhóm — vài sản phẩm ví dụ
├── .gitignore                (mới) bỏ qua anh/
├── anh/                      ảnh gốc người nhập bỏ vào — KHÔNG commit
├── assets/                   (mới, sinh ra) bản thu nhỏ <băm8>.jpg + manifest.json — BẮT BUỘC commit
└── README.md                 (mới — E07.01) cách chạy seed từ đầu đến cuối
server-spring/src/main/resources/db/seed/R__seed_demo_95_excel_products.sql   (sinh ra — commit)
scripts/seed/upload-assets.sh · upload-assets.ps1     (sửa) thêm mirror scripts/seed/excel/assets → product-media/seed/excel
scripts/seed/README.md                                (sửa) thêm mục "Seed từ Excel"
server-spring/src/test/java/com/genie/support/SeedDataIT.java   (sửa) khẳng định có SKU 'XL-%' khi file 95 tồn tại
```

Lệnh:

```bash
pip install -r scripts/seed/excel/requirements.txt
python scripts/seed/excel/excel_to_seed.py template  scripts/seed/excel/products.xlsx   # tạo file mẫu (không ghi đè nếu đã có)
python scripts/seed/excel/excel_to_seed.py check     scripts/seed/excel/products.xlsx   # chỉ kiểm, không ghi
python scripts/seed/excel/excel_to_seed.py build     scripts/seed/excel/products.xlsx   # ghi ảnh + SQL
python scripts/seed/excel/excel_to_seed.py build ... --apply [--flush-cache]           # + upload-assets + seed.sh --docker --only 95
```

---

## 5. Danh sách task

### E00 — Chốt quyết định
- [ ] **E00.01** Chốt D1–D8 và cấu trúc Excel mục 3 với nhóm; ghi kết quả vào cột *Chốt*.

### E01 — Khung công cụ
- [ ] **E01.01** Tạo `scripts/seed/excel/` + `requirements.txt` + CLI `argparse` 3 lệnh con; mã thoát 0 / 1 (lỗi dữ liệu) / 2 (lỗi dùng lệnh).
- [ ] **E01.02** Dò gốc repo từ vị trí script (như `seed.sh`), đường dẫn đầu ra cố định (D1, D3); thêm `scripts/seed/excel/.gitignore` bỏ qua `anh/`; chạy được trên Windows (PowerShell, Git Bash) và Linux / macOS.
- [ ] **E01.03** `excel_to_seed.bat`: nhận đường dẫn file kéo thả, gọi `build --apply`, `pause` cuối để đọc kết quả.

### E02 — Đọc & kiểm tra Excel (X2)
- [ ] **E02.01** `reader.py`: đọc sheet theo **tên tiêu đề** (không theo vị trí cột), bỏ dòng trống, chuẩn hóa khoảng trắng / Unicode NFC.
- [ ] **E02.02** Kiểm kiểu & miền giá trị: bắt buộc, số nguyên VND, enum đúng CHECK của V2, độ dài theo cột DB, `ma_sp` / `ma_bien_the` trùng, `BienThe.ma_sp` không có trong `SanPham`, `thuoc_tinh` sai cú pháp.
- [ ] **E02.03** Kiểm tham chiếu offline: `email_chu_shop` / `slug_danh_muc` có trong danh sách lấy từ file seed (D7); cảnh báo (không chặn) khi `thuong_hieu` / khóa `ThongSo` không khớp.
- [ ] **E02.04** Báo lỗi gom theo `Sheet!Ô`, tối đa in 50 lỗi + tổng số; lệnh `check` dừng ở đây.

### E03 — Ảnh (D1, D2, X6)
- [ ] **E03.01** *(cách chính)* Ảnh theo tên tệp: tìm trong `anh/` cạnh file Excel, không phân biệt hoa thường phần mở rộng; chỉ nhận jpg / jpeg / png / webp. Tệp gốc vắng → tra `assets/manifest.json`: có bản thu nhỏ thì dùng lại (người khác sửa Excel không cần ảnh gốc), không có → lỗi theo ô.
- [ ] **E03.02** Ảnh là link `https://…`: không tải, không chuẩn hóa, ghi nguyên URL vào `image_url` (`MediaUrls` giữ nguyên URL tuyệt đối); in cảnh báo "ảnh phụ thuộc web ngoài"; `http://` thường → lỗi.
- [ ] **E03.03** *(cách phụ)* Ảnh chèn nổi trong Excel: duyệt `ws._images`, ánh xạ ô neo `(hàng, cột)` → (sản phẩm / biến thể, thứ tự ảnh); nhiều ảnh neo cùng ô → xếp theo vị trí ngang; ảnh neo vào ô không thuộc cột ảnh → lỗi. Phát hiện ảnh *Place in Cell* → lỗi hướng dẫn chèn lại bằng *Place over Cells*.
- [ ] **E03.04** Chuẩn hóa bằng Pillow (xoay theo EXIF, bỏ alpha → nền trắng, cạnh dài ≤ 1200, JPEG q85), tên `<sha256[:8]>.jpg` theo **nội dung sau chuẩn hóa**; ghi vào `assets/`, trả `width/height/file_size`; cập nhật `assets/manifest.json` (tên tệp gốc → tên băm + kích thước, sắp xếp khóa để tất định); xóa ảnh mồ côi trong `assets/` và mục manifest không còn dòng Excel nào dùng.
- [ ] **E03.05** *(Giai đoạn 2, cắt được)* Đọc ảnh *Place in Cell*: `xl/metadata.xml` + `xl/richData/*` + thuộc tính `vm` của ô.

### E04 — Sinh SQL (X3, X4, X5, D4, D6)
- [ ] **E04.01** Khuôn SQL theo seed 40: đầu file ghi chú nguồn + "FILE SINH TỰ ĐỘNG — sửa Excel rồi chạy lại công cụ"; bảng tạm `xl_product`, `xl_variant`, `xl_image`; escape chuỗi `'` → `''`, JSON qua `json.dumps(ensure_ascii=False)`; thứ tự dòng theo `ma_sp`.
- [ ] **E04.02** Upsert: `UPDATE products … FROM xl_product` rồi `INSERT … WHERE NOT EXISTS (vendor_id, name)`; biến thể `ON CONFLICT (sku) DO UPDATE`; ảnh `DELETE … WHERE image_url LIKE 'seed/excel/%' AND product_id IN (…)` rồi `INSERT`; cập nhật `active_product_count` như seed 40.
- [ ] **E04.03** Khối `DO $$` cuối file: đếm dòng `xl_product` không khớp chủ shop / danh mục → `RAISE EXCEPTION` liệt kê `ma_sp`.
- [ ] **E04.04** Ghi file tất định (LF, UTF-8 không BOM); chạy `build` 2 lần trên cùng Excel → `git diff` rỗng.

### E05 — Nạp & đẩy ảnh (X1, D8)
- [ ] **E05.01** Sửa `upload-assets.sh` + `.ps1`: thêm mirror `scripts/seed/excel/assets` → `product-media/seed/excel` (bỏ qua nếu thư mục rỗng).
- [ ] **E05.02** Kiểm ảnh sau khi đẩy: cuối `upload-assets` đối chiếu mọi khóa `seed/excel/…` trong file SQL 95 với danh sách đối tượng trên Silo (`mcli ls`) → in ảnh thiếu + nhắc kiểm `STORAGE_BUCKET_PREFIX` / `MINIO_ENDPOINT` trong `.env` khớp backend; có ảnh thiếu thì thoát mã ≠ 0.
- [ ] **E05.03** `--apply`: gọi `upload-assets` rồi `seed.sh --docker --only 95` (Windows: `seed.ps1 -Docker -Only 95`), in bảng số dòng script trả về.
- [ ] **E05.04** `--flush-cache`: xóa khóa cache catalog trong Redis Docker (tra tiền tố khóa trong `ProductDetailService` / `PublicShopService`); không bật thì in nhắc "trang sản phẩm có thể còn bản cũ tới 5 phút".

### E06 — Kiểm thử
- [ ] **E06.01** pytest: file Excel nhỏ dựng bằng `openpyxl` trong test (có ảnh nổi) → đúng số sản phẩm / biến thể / ảnh; từng loại lỗi E02.02 báo đúng ô; SQL tất định.
- [ ] **E06.02** Sửa `SeedDataIT`: nếu có `R__seed_demo_95_*` thì khẳng định ≥ 1 SKU `XL-%`, mọi sản phẩm Excel có ≥ 1 ảnh chính; lần chạy thứ 2 không đổi số dòng (đã có sẵn). `./mvnw verify` xanh.
- [ ] **E06.03** Chạy thật máy dev: `build --apply` với `products.xlsx` mẫu → sản phẩm hiện ở tìm kiếm, trang sản phẩm có đủ ảnh + biến thể (web `PUBLIC_REAL_API=product,search,shop`); sửa giá một dòng, chạy lại → giá đổi, không sinh trùng.

### E07 — Tài liệu
- [ ] **E07.01** `scripts/seed/excel/README.md` — **hướng dẫn chạy seed từ đầu đến cuối**, viết cho người chưa đọc plan, đủ các mục:
  1. *Tool này làm gì* — 3 câu + sơ đồ mục 1b (Excel → tool → `assets/` + SQL → git → Silo + CSDL → giao diện) và bảng vai trò Tool / Git / Silo / CSDL.
  2. *Chuẩn bị máy* — Docker đang chạy `postgres`, `redis`, `silo` (`docker compose … up -d`), backend đã chạy một lần (migrate tới V21); với người tạo: Python 3.12 + `pip install -r requirements.txt`.
  3. *Luồng người dùng lại* (không cần Python) — `git pull` → `upload-assets` → chạy backend / `seed.sh --docker`; lệnh cho cả Git Bash và PowerShell.
  4. *Luồng người tạo dữ liệu* — `template` → điền Excel → bỏ ảnh vào `anh/` → `check` → `build --apply` → mở web kiểm → commit đúng các file (bảng: SQL 95 + `assets/` + `products.xlsx` bắt buộc; `anh/` không commit — tự giữ ảnh gốc nếu cần).
  5. *Mô tả các cột* — bảng mục 3 + ví dụ; 3 cách đưa ảnh (tên tệp / link / chèn vào Excel), kèm hình chụp chỗ chọn *Place over Cells* và cách tắt *Compress Pictures*.
  6. *Sửa và chạy lại* — sửa dòng thì cập nhật, **đổi tên = sản phẩm mới**, xóa dòng thì sản phẩm cũ vẫn còn trong CSDL (xóa bằng cách hạ CSDL `down -v`).
  7. *Lỗi thường gặp* — ảnh vỡ (quên `upload-assets` / lệch `STORAGE_BUCKET_PREFIX`), giá cũ vẫn hiện (cache Redis → `--flush-cache`), `RAISE EXCEPTION` không khớp shop / danh mục, `Place in Cell`, backend báo checksum Flyway.
  8. *Không bao giờ* — sửa tay file SQL 95, trỏ `.env` vào RDS / S3 production.
- [ ] **E07.02** Thêm mục vào `scripts/seed/README.md` (bảng file 95 + link sang `excel/README.md`) và một dòng vào CLAUDE.md mục 9 (bẫy: file 95 là file sinh tự động, không sửa tay; `assets/` phải commit cùng SQL).
- [ ] **E07.03** Một người **chưa tham gia** làm tool tự đọc README và chạy luồng "người dùng lại" trên máy mình; chỗ nào phải hỏi thì bổ sung vào README.

---

## 6. Rủi ro

| Rủi ro | Cách xử lý |
|---|---|
| Người nhập chèn ảnh kiểu *Place in Cell* (mặc định mới của Excel 365) | Cách chính là tên tệp nên ít gặp; phát hiện + báo lỗi hướng dẫn (D2); E03.05 nếu nhóm dùng nhiều |
| Ảnh gốc `anh/` không lên git → mất ảnh gốc khi đổi máy | Không ảnh hưởng hiển thị (`assets/` đủ); muốn đổi cách thu nhỏ (vd > 1200px) thì chỉ áp được cho ảnh còn bản gốc — ghi rõ trong README |
| Quên commit `assets/` → máy khác ảnh vỡ | Kiểm ảnh E05.02 báo thiếu ngay ở máy người dùng lại; README mục 4 có bảng file phải commit |
| Ảnh dạng link `https://` chết / bị chặn tải | Cảnh báo khi `build` (E03.02); README khuyên dùng tên tệp cho dữ liệu dùng lâu |
| Excel tự nén ảnh khi lưu (mặc định 220 ppi) → ảnh mờ | README hướng dẫn tắt *Compress Pictures*, hoặc dùng cách ghi tên tệp |
| Repo phình vì ảnh | X6 chuẩn hóa ≤ 1200px; E03.03 xóa ảnh mồ côi; nếu `assets/` > ~30 MB thì cân nhắc Git LFS |
| Sửa tay file 95 rồi bị công cụ ghi đè | Dòng đầu file ghi "SINH TỰ ĐỘNG"; E07.02 ghi bẫy vào CLAUDE.md |
| Schema `products` đổi (migration V22+) | `SeedDataIT` đỏ → sửa `sql_writer.py` + sinh lại file 95 cùng PR |
| Sau này có API `POST /seller/products` thật | Giữ `reader.py` + `images.py`; thêm đầu ra `api_writer.py` (đăng nhập chủ shop, upload presigned, gọi API) thay cho `sql_writer.py` |

---

## 7. Gợi ý chia PR (≤ ~400 dòng / PR, scope `db`)

1. `feat(db): add excel product seed reader and validation` — E01, E02, E06.01 (phần đọc).
2. `feat(db): extract and normalize product images from excel seed` — E03, test ảnh.
3. `feat(db): generate repeatable product seed sql from excel` — E04, E05, E06.02, file 95 + `products.xlsx` mẫu + `assets/`.
4. `docs(db): document excel product seeding` — E07 (README chạy seed).

---

## 8. Nhật ký

| Ngày | Việc | Commit / PR |
|---|---|---|
| 2026-11-21 | Lập plan | — |
| 2026-11-21 | Sửa plan: thêm mục 1b (vai trò Tool / Git / Silo / CSDL, 2 folder ảnh, 2 luồng làm việc); D1 `assets/` bắt buộc commit, `anh/` tùy chọn; D2 tên tệp là cách chính, thêm link `https://`; E05.02 kiểm ảnh trên Silo; E07.01 README chạy seed đủ 8 mục, E07.03 người ngoài thử README | — |
| 2026-11-21 | Chốt D1: không commit `anh/` (`.gitignore`), chỉ commit `assets/`; thêm `assets/manifest.json` để người khác sửa Excel không cần ảnh gốc (E03.01, E03.04) | — |
