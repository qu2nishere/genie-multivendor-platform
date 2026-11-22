# Seed sản phẩm từ file Excel

> Plan: [`plan/2026-11-21/db/plan_seed_tu_excel.md`](../../../plan/2026-11-21/db/plan_seed_tu_excel.md) · seed chung: [`scripts/seed/README.md`](../README.md)
> **Chỉ dùng cho máy dev** (PostgreSQL + Silo chạy Docker). Production (AWS RDS + S3) không bao giờ seed (ADR 0010).

Bạn **chỉ cần dữ liệu để test** (không sửa sản phẩm)? Đọc [mục 3](#3-luồng-người-dùng-lại-không-cần-python) là đủ, không cần cài Python.

---

## 1. Tool này làm gì

Bạn điền sản phẩm (tên, giá, biến thể, thông số, nhiều ảnh) vào **một file Excel**. Tool đọc file đó rồi sinh ra:
- **ảnh đã thu nhỏ** trong `scripts/seed/excel/assets/`,
- **file SQL** `server-spring/src/main/resources/db/seed/R__seed_demo_95_excel_products.sql`.

Hai thứ này được commit lên git, nên cả nhóm pull về là có cùng dữ liệu.

```
products.xlsx + anh/  ──(tool Python)──►  assets/*.jpg  +  R__seed_demo_95_….sql
                                              │                   │
                                         (git push / pull)   (git push / pull)
                                              ▼                   ▼
                                    upload-assets → Silo     seed.sh / backend → PostgreSQL
                                              └────────► giao diện hiển thị sản phẩm + ảnh ◄┘
```

| Thành phần | Vai trò |
|---|---|
| Tool Python | Kiểm file Excel, thu nhỏ ảnh, đặt tên ảnh theo nội dung, sinh SQL |
| Git | Chuyển ảnh (`assets/`) và SQL sang máy người khác. Silo của mỗi máy là kho riêng, không thông nhau |
| Silo | Máy chủ phát file ảnh cho trình duyệt (chuẩn S3, giống AWS S3 khi chạy thật). Silo **không** chuyển đổi ảnh |
| CSDL | Chỉ lưu **khóa ảnh** của sản phẩm, ví dụ `seed/excel/56769b09.jpg`, không chứa ảnh |

Khi mở trang sản phẩm, backend ghép `địa chỉ Silo` + `bucket` + `khóa ảnh` thành URL, ví dụ
`http://localhost:9000/product-media/seed/excel/56769b09.jpg`. Địa chỉ Silo (`MINIO_ENDPOINT`) và tiền tố bucket
(`STORAGE_BUCKET_PREFIX`) đọc từ `.env` của từng máy, không ghi trong SQL. Vì vậy máy nào cũng khớp, kể cả khi mỗi máy dùng cổng khác nhau.

**Hai folder ảnh:**

| Folder | Là gì | Commit? |
|---|---|---|
| `anh/` | Ảnh gốc bạn bỏ vào để tool đọc | **Không** (đã có trong `.gitignore`) |
| `assets/` | Bản thu nhỏ do tool sinh (`<băm8>.jpg`) + `manifest.json` | **Bắt buộc** |

`manifest.json` ghi ảnh gốc nào ứng với ảnh thu nhỏ nào. Nhờ file này, người không có ảnh gốc vẫn sửa được Excel (đổi giá, mô tả…) và build lại bình thường.

---

## 2. Chuẩn bị máy

1. Docker đang chạy các dịch vụ của máy dev (từ gốc repo):
   ```bash
   docker compose --env-file .env -f deploy/docker-compose.local.yml up -d
   ```
2. Backend đã chạy ít nhất một lần, để Flyway tạo schema tới V21 và nạp dữ liệu mẫu gốc (shop, danh mục):
   `cd server-spring && ./mvnw spring-boot:run`.
3. **Chỉ người tạo / sửa dữ liệu** cần thêm Python 3.12:
   ```bash
   pip install -r scripts/seed/excel/requirements.txt
   ```

---

## 3. Luồng người dùng lại (không cần Python)

Dùng khi bạn chỉ cần có sản phẩm để test, không sửa gì.

```bash
git pull
# 1) đẩy ảnh từ scripts/seed/excel/assets/ lên Silo máy mình (kèm kiểm đủ ảnh)
scripts/seed/upload-assets.sh                                         # Git Bash / Linux / macOS
powershell -ExecutionPolicy Bypass -File scripts\seed\upload-assets.ps1   # PowerShell
# 2) nạp dữ liệu — chọn MỘT trong hai cách
cd server-spring && ./mvnw spring-boot:run      # profile local tự nạp vì GENIE_SEED_DEMO=true
scripts/seed/seed.sh --docker --only 95         # hoặc: seed.ps1 -Docker -Only 95
```

Quên bước 1 thì sản phẩm vẫn hiện nhưng **ảnh bị vỡ**. Chỉ cần chạy lại `upload-assets` là có ảnh.

---

## 4. Luồng người tạo dữ liệu

```bash
# (lần đầu, nếu chưa có file) tạo file mẫu có danh sách chọn shop / danh mục
python scripts/seed/excel/excel_to_seed.py template scripts/seed/excel/products.xlsx
```

1. Mở `scripts/seed/excel/products.xlsx`, điền sheet `SanPham` (và `BienThe`, `ThongSo` nếu cần, xem [mục 5](#5-các-cột-trong-file-excel)).
2. Bỏ ảnh vào folder `scripts/seed/excel/anh/` rồi ghi tên tệp vào cột `anh`.
3. Kiểm (không ghi gì):
   ```bash
   python scripts/seed/excel/excel_to_seed.py check scripts/seed/excel/products.xlsx
   ```
   Có lỗi thì tool in ra kèm ô, ví dụ `SanPham!E7: phải là số nguyên VND`.
4. Sinh ảnh + SQL rồi nạp luôn vào máy mình:
   ```bash
   python scripts/seed/excel/excel_to_seed.py build scripts/seed/excel/products.xlsx --apply --flush-cache
   ```
   Trên Windows có thể **kéo thả file `.xlsx` vào `excel_to_seed.bat`**, hoặc nhấp đúp để dùng `products.xlsx`.
5. Mở web kiểm tra: tìm tên sản phẩm, mở trang sản phẩm xem ảnh và biến thể.
   Web cần nối API thật: `PUBLIC_REAL_API=product,search,shop`.
6. Commit:

   | File | Commit? |
   |---|---|
   | `server-spring/src/main/resources/db/seed/R__seed_demo_95_excel_products.sql` | **Bắt buộc** |
   | `scripts/seed/excel/assets/` (mọi `.jpg` + `manifest.json`) | **Bắt buộc**, kể cả ảnh bị xóa |
   | `scripts/seed/excel/products.xlsx` | **Bắt buộc**, để người khác sửa tiếp |
   | `scripts/seed/excel/anh/` | **Không**. Tự giữ ảnh gốc nếu sau này cần |

| Lệnh | Làm gì |
|---|---|
| `template <file> [--force]` | Tạo file Excel mẫu, không ghi đè file đã có trừ khi `--force` |
| `check <file>` | Chỉ kiểm, không ghi gì |
| `build <file>` | Kiểm rồi ghi `assets/` + file SQL 95. Có lỗi thì **không ghi gì** |
| `build <file> --apply` | Như trên, rồi chạy `upload-assets` + `seed --docker --only 95` |
| `build <file> --flush-cache` | Xóa cache sản phẩm / shop trong Redis Docker, để trang hiện dữ liệu mới ngay |

Mã thoát: `0` ổn · `1` lỗi dữ liệu · `2` gõ lệnh sai · `3` lỗi khi đẩy ảnh / nạp CSDL.

---

## 5. Các cột trong file Excel

Tiêu đề cột ở dòng 1. Tool đọc theo **tên cột**, không theo vị trí. Cột tô vàng là bắt buộc; rê chuột vào tiêu đề để xem giải thích.

**Sheet `SanPham`**: mỗi dòng một sản phẩm.

| Cột | Bắt buộc | Ghi gì |
|---|---|---|
| `ma_sp` | ✔ | Mã bạn tự đặt, duy nhất trong file (chữ không dấu + số, ≤ 20 ký tự), ví dụ `TN01` |
| `email_chu_shop` | ✔ | Email chủ shop đã có trong seed (chọn trong danh sách), ví dụ `mai.tran@gmail.com` |
| `slug_danh_muc` | ✔ | Slug danh mục, nên chọn danh mục con (chọn trong danh sách) |
| `ten` | ✔ | Tên sản phẩm, ≤ 255 ký tự |
| `mo_ta` | | Mô tả, xuống dòng bằng Alt+Enter |
| `thuong_hieu` | | Tên thương hiệu. Trùng bảng brands (Sony, Samsung…) thì được gắn để lọc |
| `tinh_trang` | | `NEW` (mặc định) / `USED` / `REFURBISHED` |
| `xuat_xu` | | Ví dụ `Việt Nam` |
| `gia` | ✔ | Giá bán, số nguyên VND. Nhận cả `389000`, `389.000`, `389,000` |
| `ton_kho` | khi không có biến thể | Số nguyên ≥ 0 |
| `can_nang_g` | ✔ | Gram, số nguyên |
| `dai_cm` `rong_cm` `cao_cm` | | Kích thước đóng gói |
| `mien_phi_ship` | | `x` = shop trả phí ship; để trống = người mua trả |
| `dat_truoc_ngay` | | Số ngày chuẩn bị nếu là hàng đặt trước |
| `trang_thai` | | `PUBLISHED` (mặc định) / `DRAFT` / `HIDDEN` |
| `da_ban` · `diem_sao` | | Số đã bán, điểm 0–5 (để thử sắp xếp) |
| `anh` | ✔ | Ảnh. **Ảnh đầu tiên là ảnh chính.** Tối đa 8 ảnh / sản phẩm |
| `anh_2` … `anh_8` | | Chỉ dùng khi chèn ảnh thẳng vào Excel (mỗi ô một ảnh) |

**Sheet `BienThe`** (tùy chọn): mỗi dòng một biến thể.

| Cột | Ghi gì |
|---|---|
| `ma_sp` ✔ | Mã sản phẩm ở sheet `SanPham` |
| `ma_bien_the` ✔ | Ví dụ `BLK`, `M`, `512`. SKU sinh ra là `XL-<ma_sp>-<ma_bien_the>` |
| `ten_bien_the` | Ví dụ `Đen`, `Size M`. Bỏ trống thì lấy từ `thuoc_tinh` |
| `thuoc_tinh` | `khoa=gia tri; khoa=gia tri`, ví dụ `color=Đen; size=M` |
| `gia` | Bỏ trống thì bằng giá sản phẩm |
| `ton_kho` ✔ | Số nguyên ≥ 0 |
| `anh` | Một ảnh riêng của biến thể (tùy chọn) |

Sản phẩm không có dòng nào ở `BienThe` sẽ tự có một biến thể `Mặc định` (SKU `XL-<ma_sp>-STD`) với tồn kho = `ton_kho`.

**Sheet `ThongSo`** (tùy chọn): `ma_sp` · `khoa` · `gia_tri`, ví dụ `TN01 · warranty · 12 tháng`. Khóa nên trùng thuộc tính lọc của sàn
(`warranty`, `color`, `material`, `size`, `skin_type`) để bộ lọc dùng được. Khóa khác vẫn lưu, tool chỉ cảnh báo.

### Ba cách đưa ảnh vào

| Cách | Ghi vào ô `anh` | Khi nào dùng |
|---|---|---|
| **1. Tên tệp** (khuyên dùng) | `tai-nghe-1.jpg; tai-nghe-2.jpg` — tệp nằm trong `anh/` | Mặc định. Giữ chất lượng ảnh, file Excel nhẹ |
| **2. Link** | `https://…/anh.jpg` | Thử nhanh. Ảnh vỡ nếu web đó chết hoặc chặn; tool sẽ cảnh báo |
| **3. Chèn vào Excel** | **Insert → Pictures → Place over Cells**, kéo ảnh đặt lên ô `anh` / `anh_2`… của đúng dòng | Muốn nhìn thấy ảnh ngay trong bảng |

Lưu ý khi dùng cách 3:
- Tool đọc được cả ảnh kiểu **Place in Cell** của Excel 365. Nếu file của bạn không đọc được, chèn lại bằng **Place over Cells**.
- Tắt nén ảnh: **File → Options → Advanced → Image Size and Quality → tick "Do not compress images in file"**. Nếu không, Excel tự làm mờ ảnh khi lưu.
- Ảnh phải nằm (góc trên bên trái) trong ô thuộc cột ảnh của một dòng có dữ liệu. Ảnh đặt lệch chỗ thì tool báo lỗi kèm ô.

Ảnh được nhận: `.jpg`, `.jpeg`, `.png`, `.webp`, tối đa 25 MB. Tool tự xoay theo EXIF, thay nền trong suốt bằng nền trắng, thu cạnh dài về tối đa 1200px và lưu JPEG.

---

## 6. Sửa và chạy lại

| Bạn làm gì trong Excel | Kết quả sau `build` + nạp |
|---|---|
| Sửa giá, mô tả, danh mục, thông số, trạng thái… | Sản phẩm được **cập nhật** |
| Sửa biến thể (giá, tồn, tên, ảnh) | Biến thể được cập nhật theo SKU |
| Xóa một dòng biến thể | Biến thể bị **xóa mềm** (không hiện nữa) |
| **Đổi tên** sản phẩm | Coi là **sản phẩm mới**; bản tên cũ bị chuyển `HIDDEN` |
| Xóa một dòng sản phẩm | Sản phẩm đó bị chuyển `HIDDEN` |
| Đổi `ma_sp` | SKU đổi theo: biến thể cũ bị xóa mềm, biến thể mới được tạo |
| Thay ảnh (cùng tên tệp, nội dung khác) | Ảnh thu nhỏ mới được tạo, ảnh cũ trong `assets/` bị xóa |

Tên sản phẩm + email chủ shop là khóa nhận diện, vì bảng `products` không có mã riêng. Muốn xóa hẳn dữ liệu cũ thì hạ CSDL
(`docker compose … down -v`) rồi chạy lại backend.

---

## 7. Lỗi thường gặp

| Hiện tượng | Nguyên nhân / cách sửa |
|---|---|
| Ảnh vỡ trên web | Chưa chạy `upload-assets`, hoặc chưa pull đủ `assets/`. Nếu `upload-assets` báo `THIẾU ảnh trên Silo`, kiểm `STORAGE_BUCKET_PREFIX` / `MINIO_ENDPOINT` trong `.env` có giống cấu hình backend đang chạy không |
| Sửa giá rồi mà web vẫn hiện giá cũ | Cache Redis (5–10 phút). Thêm `--flush-cache` hoặc đợi |
| `Seed Excel: chưa có shop VERIFIED của chủ shop` / `không có danh mục` khi nạp | CSDL máy bạn chưa có dữ liệu mẫu gốc. Chạy đủ `scripts/seed/seed.sh --docker` (không `--only`) hoặc khởi động backend |
| `không thấy tệp 'x.jpg' trong scripts/seed/excel/anh/` | Ảnh mới nhưng chưa bỏ vào `anh/`, hoặc gõ sai tên. Tên tệp không phân biệt hoa thường |
| `không đọc được ảnh kiểu 'Place in Cell'` | Chèn lại bằng Place over Cells, hoặc dùng tên tệp |
| Backend báo lỗi Flyway ở `seed demo 95 excel products` khi khởi động | File SQL tham chiếu shop / danh mục không có (xem dòng trên), hoặc có người sửa tay file 95. Sinh lại bằng `build` |
| `UnicodeEncodeError` khi in | Chạy bằng `python` 3.12 mới; tool tự chuyển console sang UTF-8 |

---

## 8. Không bao giờ

- **Sửa tay** file `R__seed_demo_95_excel_products.sql`. Lần `build` sau sẽ ghi đè. Hãy sửa file Excel.
- Commit folder `anh/` (đã có trong `.gitignore`), hoặc quên commit `assets/`.
- Trỏ `.env` vào AWS RDS / S3 production rồi chạy seed. Các script sẽ từ chối, đừng tìm cách vượt qua.

---

## Cho người sửa tool

```bash
python -m pytest scripts/seed/excel/tests            # unit test Python
cd server-spring && ./mvnw verify                    # SeedDataIT chạy cả file 95 trên PostgreSQL thật
```

Cấu trúc: `excel_to_seed.py` (CLI) · `xlseed/reader.py` (đọc + kiểm) · `xlseed/images.py` (ảnh, manifest) ·
`xlseed/sql_writer.py` (sinh SQL) · `xlseed/runner.py` (`--apply`, `--flush-cache`) · `xlseed/template.py` (file mẫu) ·
`xlseed/refdata.py` (đọc danh sách shop / danh mục từ file seed 10, 20, 30).
Đổi schema `products` / `product_variants` / `product_images` → sửa `sql_writer.py` và sinh lại file 95 trong cùng PR.
