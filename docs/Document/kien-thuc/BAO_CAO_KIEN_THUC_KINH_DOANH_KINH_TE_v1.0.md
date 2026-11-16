# BÁO CÁO KIẾN THỨC KINH DOANH & KINH TẾ — BẢN TLCN

**Dự án Genie — Sàn TMĐT đa người bán (phạm vi đồ án)** · TLCN v1.1 (2026-10-29)

> **Mục đích:** nắm đủ logic kinh doanh để hiện thực đúng 87 use case của TLCN và trả lời được hội đồng câu hỏi *"sàn kiếm tiền thế nào, tiền đi đâu, ai chịu rủi ro"*.
> **Nguồn:** tham số giữ nguyên từ DATN `DATN/kien-thuc/BAO_CAO_KIEN_THUC_KINH_DOANH_KINH_TE_v1.0.md`; các phần chỉ phục vụ tính năng của DATN (hạng thành viên, affiliate, gói VIP, PPC, tài xế, BNPL, B2B) được lược bỏ. Điểm, Flash Sale, voucher 3 tầng có trong TLCN (Phần 5B).
> **Lưu ý:** các con số dự báo là **giả định minh họa** cho lập luận khả thi, không phải số liệu thị trường đã kiểm chứng.

---

## 📑 MỤC LỤC
- [PHẦN 0 — Tham số chuẩn dùng chung](#p0)
- [PHẦN 1 — Mô hình kinh doanh marketplace](#p1)
- [PHẦN 2 — Dòng doanh thu của TLCN](#p2)
- [PHẦN 3 — Unit economics một đơn hàng](#p3)
- [PHẦN 4 — Logic nghiệp vụ & dòng tiền từng luồng](#p4)
- [PHẦN 5 — Logic khuyến mãi 3 tầng + Điểm](#p5)
- [PHẦN 5B — Khuyến mãi xếp chồng, Điểm & Flash Sale: ai chịu tiền](#p5b)
- [PHẦN 6 — Kinh tế người bán & người mua](#p6)
- [PHẦN 7 — Bài toán con gà – quả trứng & chiến lược khởi động](#p7)
- [PHẦN 8 — KPI cho TLCN](#p8)
- [PHẦN 9 — Dự báo tài chính chạy thử (minh họa)](#p9)
- [PHẦN 10 — Rủi ro kinh doanh & cách TLCN kiểm soát](#p10)
- [PHẦN 11 — Ánh xạ logic kinh doanh sang entity & use case](#p11)

---

<a id="p0"></a>
## PHẦN 0 — THAM SỐ CHUẨN DÙNG CHUNG

| Nhóm | Tham số | Entity / Use case |
|---|---|---|
| Hoa hồng ngành hàng | 3,5% – 8,0% (Điện tử 3,5%, Bách hóa 4,5%, Mẹ & Bé / Gia dụng 5,5%, Mỹ phẩm 7,0%, Thời trang 8,0%); mặc định 5% nếu ngành chưa cấu hình | `Category.commissionRate` — UC-CAT-14 |
| Hoa hồng riêng của shop | Ghi đè tỷ lệ ngành nếu Admin thỏa thuận (`VendorProfile.commissionRate`) | UC-PAY-05 |
| Voucher | **3 tầng**: Shop (shop chịu, mỗi shop 1) → Sàn (sàn chịu) → Freeship (sàn chịu); tổng giảm Shop + Sàn ≤ 85% giá trị hàng; giá trị tối thiểu sau giảm 1.000đ | `Voucher`, `OrderDiscount` — UC-PRO-01…04 |
| Điểm | 1 Điểm = 1₫; dùng tối đa 50% tiền hàng còn lại sau voucher; hết hạn 12 tháng; điểm danh 100 Điểm / ngày, ngày thứ 7 liên tiếp 1.000 Điểm | `UserCoin`, `CoinTransaction`, `DailyCheckin` — UC-PRO-08…10 |
| Ký quỹ (Escrow) | Giữ tiền đến 7 ngày sau `DELIVERED` hoặc khi khách xác nhận; khiếu nại → `DISPUTED` | `EscrowLedger` — UC-PAY-05, UC-PAY-07 |
| Khiếu nại | Khách gửi trong 7 ngày; shop phản hồi 48 giờ (quá hạn → tự chấp thuận); shop từ chối → khách có 48 giờ khiếu nại lên sàn | `RefundRequest` — UC-SUP-01…04 |
| Giữ chỗ tồn kho | 15 phút chờ thanh toán | `StockReservation` — UC-ORD-07, UC-ORD-08 |
| KYC người bán | Điểm rủi ro < 30 tự duyệt · 30–69 người duyệt · ≥ 70 từ chối; lấy mẫu 5% | `KycResult` — UC-VND-03 |
| Rút tiền | Tối thiểu 100.000đ; chỉ về tài khoản ngân hàng đúng tên trên CCCD; duyệt một bước | `WithdrawalRequest` — UC-ADM-04, UC-ADM-05 |

### 0.1 Giả định tài chính

| Giả định | Giá trị |
|---|---|
| AOV (giá trị đơn trung bình) | 350.000đ |
| Hoa hồng bình quân (trộn ngành) | ~5,5% |
| Chi phí biến đổi do sàn chịu trong MVP | ~1,8% GMV: phí cổng VNPay do sàn chịu ~0,5% (VNPay thu ~1–2% trên phần trả qua thẻ / QR VNPay; chuyển khoản VietQR qua SePay **không mất phí %** — càng nhiều khách chọn VietQR thì khoản này càng giảm), voucher sàn ~1,0%, hoàn tiền / sai sót ~0,3% |
| Biên đóng góp | ≈ 5,5% − 1,8% = **3,7% GMV** |

---

<a id="p1"></a>
## PHẦN 1 — MÔ HÌNH KINH DOANH MARKETPLACE

**Marketplace (sàn) ≠ cửa hàng online.** Sàn **không sở hữu hàng**, không chịu tồn kho; sàn bán **niềm tin và lưu lượng**: người mua tin rằng trả tiền sẽ nhận đúng hàng (nhờ ký quỹ + khiếu nại), người bán tin rằng giao hàng sẽ nhận được tiền (nhờ ký quỹ + giải ngân tự động).

| Khía cạnh | Cửa hàng 1 người bán | Sàn đa người bán (Genie) |
|---|---|---|
| Hàng hóa | Tự nhập, tự chịu tồn kho | Shop tự đăng, tự giữ hàng |
| Doanh thu | Chênh lệch giá | Hoa hồng trên GMV (take rate) |
| Rủi ro chính | Tồn kho, vốn | Gian lận, chất lượng người bán, tranh chấp |
| Tài sản cốt lõi | Hàng | Mạng lưới 2 phía + cơ chế niềm tin |

**Năm cơ chế niềm tin của TLCN:** KYC người bán (biết ai đang bán) → kiểm duyệt sản phẩm (Gemini sàng lọc + Moderator duyệt) → ký quỹ 7 ngày cho **mọi** phương thức, kể cả COD (tiền chưa tới tay shop khi người mua chưa hài lòng) → đánh giá của người đã mua có "Hữu ích" và báo cáo vi phạm → khiếu nại có phân xử (bên thứ ba trung lập). Trang shop công khai điểm đánh giá, người theo dõi, số sản phẩm, tỉ lệ phản hồi chat.

---

<a id="p2"></a>
## PHẦN 2 — DÒNG DOANH THU CỦA MVP

| Dòng doanh thu | TLCN | DATN |
|---|:---:|:---:|
| Hoa hồng theo ngành trên mỗi đơn hoàn tất | ✅ duy nhất | ✅ |
| Gói thành viên người bán (PRO / MALL_VIP) | — | ✅ |
| Quảng cáo tìm kiếm PPC | — | ✅ |
| Phí vận chuyển Genie Express (20% cước) | — | ✅ |
| Vật phẩm trang trí, phí BNPL… | — | ✅ |

**Take rate** = tổng doanh thu sàn / GMV. TLCN chỉ có hoa hồng nên take rate ≈ hoa hồng bình quân (~5,5%) **trừ phần hoa hồng của đơn bị hoàn** và **trừ chi phí khuyến mãi sàn chịu** (voucher Sàn, Freeship, Điểm — Phần 5B). Hoa hồng chỉ được ghi nhận khi ký quỹ **giải ngân** (`EscrowLedger.RELEASED`) — đơn bị hoàn toàn phần không mang lại doanh thu.

---

<a id="p3"></a>
## PHẦN 3 — UNIT ECONOMICS MỘT ĐƠN HÀNG

**Ví dụ đơn 350.000đ, ngành Thời trang (8%), khách dùng voucher shop 20.000đ và voucher sàn 15.000đ, phí ship 30.000đ khách trả, thanh toán VNPay:**

| Khoản | Khách trả | Shop nhận | Sàn |
|---|--:|--:|--:|
| Giá hàng | 350.000 | | |
| Voucher shop (shop chịu) | −20.000 | | |
| Voucher sàn (sàn chịu) | −15.000 | | −15.000 |
| Phí ship (trả GHN) | +30.000 | | (chuyển cho GHN) |
| **Khách thanh toán** | **345.000** | | |
| `grossAmount` = 350.000 − 20.000 | | 330.000 | |
| Hoa hồng 8% × 330.000 | | −26.400 | +26.400 |
| **`netAmount` shop nhận khi giải ngân** | | **303.600** | |
| Phí cổng VNPay (~1,1% × 345.000, sàn chịu) | | | −3.800 |
| **Lợi nhuận góp của sàn trên đơn** | | | **≈ 7.600** |

> Kiểm tra cân đối: khách trả 345.000 = GHN 30.000 + shop 303.600 + sàn giữ 26.400 − sàn bù voucher 15.000 → 30.000 + 303.600 + 11.400 = 345.000 ✓.
> Bài học: voucher sàn ăn trực tiếp vào hoa hồng — **ngân sách voucher sàn (`Voucher.budget`) phải được giới hạn**, nếu không mỗi đơn có thể lỗ.

---

<a id="p4"></a>
## PHẦN 4 — LOGIC NGHIỆP VỤ & DÒNG TIỀN TỪNG LUỒNG

### 4.1 Đặt hàng nhiều shop (UC-ORD-07)
Một lần thanh toán = 1 `CheckoutSession`, N `Order`. Mỗi đơn con có vận đơn, ký quỹ, khiếu nại **riêng** → shop A giao trễ không làm ảnh hưởng tiền của shop B.

### 4.2 Thanh toán VNPay / chuyển khoản VietQR (UC-PAY-03)
Tiền khách → tài khoản doanh nghiệp của sàn (qua VNPay T+1, hoặc chuyển khoản VietQR tức thời — SePay báo tiền vào) → hệ thống ghi `EscrowLedger HELD` cho từng đơn con. **Tiền thật nằm ở một tài khoản ngân hàng của sàn**; `escrow_ledgers` là sổ ghi "tiền này đang nợ shop nào, khi nào được trả".

### 4.3 COD qua GHN (UC-LOG-03)
Khách trả tiền mặt cho bưu tá → GHN giữ tiền → GHN chuyển về sàn theo chu kỳ (trừ phí thu hộ) → Staff đối soát → ký quỹ `HELD`. **Rủi ro COD:** khách bom hàng (từ chối nhận) → hàng hoàn, phí ship hai chiều; MVP chấp nhận rủi ro này, theo dõi tỷ lệ hoàn COD ở dashboard để quyết định có giới hạn COD hay không.

### 4.4 Giải ngân (UC-PAY-07)
Chỉ khi: đơn `COMPLETED` + ký quỹ `HELD` + không khiếu nại mở. Giải ngân cộng `netAmount` vào `VendorWallet` — **đây không phải ví điện tử**, chỉ là sổ công nợ sàn nợ shop; shop rút về ngân hàng bằng lệnh rút được Staff duyệt.

### 4.5 Hoàn tiền (UC-SUP-04)
- **Trước giải ngân** (thường gặp): tiền chưa rời sàn → hoàn VNPay hoặc chuyển khoản tay (COD); hoa hồng của phần bị hoàn không còn.
- **Hoàn một phần:** shop vẫn nhận phần còn lại; ví dụ chuẩn đơn 1.000.000đ, trả 300.000đ → khách 300.000 + shop 665.000 + sàn 35.000 = 1.000.000đ.
- **Sau giải ngân** (hiếm): thu hồi từ ví shop (`DEBIT_REFUND`); nếu ví không đủ → công nợ shop, trừ dần ở lần giải ngân sau.

---

<a id="p5"></a>
## PHẦN 5 — LOGIC KHUYẾN MÃI 3 TẦNG + ĐIỂM

| | Voucher shop (Tầng 1) | Voucher sàn (Tầng 2) | Freeship (Tầng 3) | Điểm (sau cùng) |
|---|---|---|---|---|
| Ai tạo | Shop | Finance Officer / Admin | Finance Officer / Admin | Khách tích lũy |
| Ai chịu | Shop (giảm `grossAmount`) | Sàn (không giảm `grossAmount`; đồng tài trợ theo `platformSharePercent`) | Sàn | Sàn |
| Phạm vi | Đơn con của shop (có thể giới hạn danh mục / sản phẩm) | Cả phiên thanh toán | Tổng phí ship | ≤ 50% tiền hàng còn lại |
| Mục tiêu | Tăng chuyển đổi của shop, xả hàng | Thu hút người mua mới, kích cầu toàn sàn | Giảm rào cản phí ship | Giữ chân, quay lại hằng ngày |
| Kiểm soát | `usageLimit`, `usagePerUser`, `minOrderValue`, `maxDiscountCap` | Thêm `budget` (trần tiền) — tự tắt khi hết | Không vượt phí ship thực tế | Hạn 12 tháng, `balanceHash` |

**Thứ tự áp:** Shop → Sàn → Freeship → Điểm (mỗi tầng tính trên giá trị đã trừ tầng trước) → sàn không trợ giá cho phần shop đã tự giảm. **Phân bổ xuống dòng hàng** bắt buộc để khi trả 1 món hoàn đúng phần giảm giá của món đó.

---

<a id="p5b"></a>
## PHẦN 5B — KHUYẾN MÃI XẾP CHỒNG, ĐIỂM & FLASH SALE: AI CHỊU TIỀN

| Công cụ | Người chịu | Ghi sổ | Kiểm soát |
|---|---|---|---|
| Voucher Shop (tầng 1, mỗi shop 1) | Shop — giảm `netAmount` của shop | `OrderDiscount(tier=SHOP, fundedBy=VENDOR)` | Shop tự đặt ngân sách, lượt / người |
| Voucher Sàn (tầng 2) | Sàn (hoặc đồng tài trợ theo `platformSharePercent`) | `PLATFORM_VOUCHER` ở sổ cái | `budget`, `usagePerUser`, KPI chi phí voucher / GMV |
| Freeship (tầng 3) | Sàn | `FREESHIP_COST` | Không vượt phí ship thực tế |
| Điểm (sau cùng, ≤ 50% tiền hàng còn lại) | Sàn — shop vẫn nhận đủ tiền hàng | `COIN_COST` khi đơn hoàn tất | Điểm hết hạn 12 tháng; `balanceHash`; khóa tiêu khi lệch |
| Flash Sale | Shop (giảm giá bán ≥ 15% so với giá 30 ngày) | Giá snapshot vào `order_items` | Suất giới hạn, UPDATE có điều kiện |

**Ví dụ:** đơn 500.000đ của 1 shop, hoa hồng 5%, voucher Shop 50.000đ, voucher Sàn 30.000đ, Freeship 20.000đ (phí ship 25.000đ), Điểm 100.000đ → khách trả 500.000 − 50.000 − 30.000 − 100.000 + (25.000 − 20.000) = 325.000đ; shop nhận (500.000 − 50.000) × 95% = 427.500đ; sàn: hoa hồng 22.500đ − (30.000 + 20.000 + 100.000) chi phí khuyến mãi = −127.500đ cho đơn này → **khuyến mãi sàn là chi phí thu hút có ngân sách**, phải theo dõi KPI ở Phần 8.

**Điểm như một khoản nợ:** Điểm đã phát chưa dùng là nghĩa vụ tương lai của sàn (tối đa 1 Điểm = 1₫); hạn 12 tháng giới hạn khoản nợ treo; thưởng điểm danh nhỏ (100 Điểm / ngày, 1.000 Điểm ngày thứ 7) để chi phí giữ chân dễ dự báo.

---

<a id="p6"></a>
## PHẦN 6 — KINH TẾ NGƯỜI BÁN & NGƯỜI MUA

**Người bán quan tâm:** phí (hoa hồng), tốc độ nhận tiền (7 ngày), rủi ro bị khiếu nại oan, công cụ đơn giản, lượng khách tìm tới. TLCN trả lời bằng: hoa hồng theo ngành công khai, ví & sổ giao dịch minh bạch từng đơn (`grossAmount`, `commissionFee`, `netAmount`), quy trình khiếu nại có bằng chứng 2 chiều, phân tích lượt xem / nguồn truy cập, người theo dõi shop, Flash Sale và voucher shop, tự trả lời chat, tạm nghỉ.

**Người mua quan tâm:** giá, tin tưởng, giao nhanh, đổi trả dễ. TLCN trả lời bằng: voucher xếp chồng 3 tầng + Điểm, Flash Sale, đánh giá của người đã mua (chỉ đơn `COMPLETED`) kèm "Hữu ích", chỉ số uy tín trên trang shop, chọn hãng vận chuyển rẻ / nhanh, theo dõi đơn, đổi trả 7 ngày, ký quỹ kể cả COD.

**Một tài khoản hai chế độ** giảm ma sát chuyển đổi khách → người bán (không đăng ký lại), giúp tăng nguồn cung ở giai đoạn đầu.

---

<a id="p7"></a>
## PHẦN 7 — BÀI TOÁN CON GÀ – QUẢ TRỨNG & CHIẾN LƯỢC KHỞI ĐỘNG

Sàn không có người bán thì không có người mua và ngược lại. Chiến lược khởi động phù hợp quy mô MVP:
1. **Cung trước:** mời 10–20 shop nhỏ (bạn bè, shop sinh viên, cửa hàng địa phương), hoa hồng ưu đãi 0–3% trong 3 tháng (`VendorProfile.commissionRate`).
2. **Một ngách hẹp:** ví dụ đồ dùng sinh viên / thời trang local brand trong một thành phố → GHN giao nội tỉnh nhanh, chi phí thấp.
3. **Cầu sau:** voucher sàn cho đơn đầu tiên có ngân sách cố định; đo CAC thực tế.
4. **Đo trước khi mở rộng:** Flash Sale, Điểm, voucher sàn đã có sẵn nhưng **bật theo ngân sách** — chỉ tăng ngân sách khi KPI ở Phần 8 cho thấy nút thắt nằm ở cầu; chia sẻ deeplink + Open Graph giúp người mua tự kéo người mua mới với chi phí 0.

---

<a id="p8"></a>
## PHẦN 8 — KPI CHO TLCN

| KPI | Định nghĩa | Nguồn dữ liệu | Mục tiêu chạy thử |
|---|---|---|---|
| GMV | Σ `orders.grand_total` đơn `COMPLETED` | `orders` | 150–200 triệu / tháng |
| Số đơn / AOV | Đếm / trung bình đơn hoàn tất | `orders` | ~500 / 350.000đ |
| Take rate thực | Σ `commission_fee` (RELEASED) / GMV | `escrow_ledgers` | ≥ 5% |
| Tỷ lệ chuyển đổi checkout | Phiên `COMPLETED` / phiên tạo | `checkout_sessions` | ≥ 60% |
| Tỷ lệ hủy do quá hạn thanh toán | `AUTO_CANCELLED` / đơn tạo | `orders` | ≤ 20% |
| Tỷ lệ hoàn hàng COD | Đơn COD `RETURNED_TO_VENDOR` / đơn COD | `orders` | ≤ 10% |
| Tỷ lệ khiếu nại | `refund_requests` / đơn giao | `refund_requests` | ≤ 5% |
| Thời gian duyệt KYC | `kycVerifiedAt − kycSubmittedAt` | `vendor_profiles` | Tự duyệt < 1 phút; tay < 24 giờ |
| Tỷ lệ KYC tự duyệt | `OCR_AUTO` / tổng duyệt | `vendor_profiles` | ≥ 60% |
| Chi phí voucher sàn / GMV | Σ `order_discounts` (PLATFORM) / GMV | `order_discounts` | ≤ 1,5% |
| Chi phí khuyến mãi sàn (Sàn + Freeship + Điểm) / GMV | Σ bút toán `PLATFORM_VOUCHER`, `FREESHIP_COST`, `COIN_COST` / GMV | `financial_ledger` | ≤ 3% |
| Tỷ lệ sản phẩm Gemini từ chối tự động / bị Moderator lật lại | `REJECT` tự động; số ca Moderator duyệt ngược kết quả AI | `product_moderation_results` | Lật lại ≤ 10% |
| Tỷ lệ phản hồi chat của shop | `chat_response_rate` bình quân | `vendor_profiles` | ≥ 80% |
| Lượt truy cập từ link chia sẻ | `view_count` nguồn `EXTERNAL_LINK` / tổng | `product_view_stats` | Theo dõi |
| Tỷ lệ bán hết suất Flash Sale | Σ `sold_count` / Σ `flash_stock` | `flash_sale_items` | ≥ 70% |

Tất cả tính được bằng SQL trên 64 bảng — dashboard UC-ADM-06 hiển thị các chỉ số chính.

---

<a id="p9"></a>
## PHẦN 9 — DỰ BÁO TÀI CHÍNH CHẠY THỬ (MINH HỌA, 6 THÁNG)

| Tháng | Shop hoạt động | Đơn | GMV (triệu) | Hoa hồng (5,5%) | Chi phí biến đổi (1,8%) | Hạ tầng + dịch vụ | Kết quả góp |
|:---:|:---:|--:|--:|--:|--:|--:|--:|
| 1 | 10 | 100 | 35 | 1,9 | 0,6 | 2,8 | −1,5 |
| 2 | 20 | 250 | 88 | 4,8 | 1,6 | 2,8 | +0,4 |
| 3 | 30 | 400 | 140 | 7,7 | 2,5 | 2,8 | +2,4 |
| 4–6 | 40–50 | 500–700 | 175–245 | 9,6–13,5 | 3,2–4,4 | 2,8 | +3,6 – 6,3 |

> Nếu áp hoa hồng ưu đãi 0–3% trong 3 tháng đầu (Phần 7) thì 3 tháng đầu lỗ góp ~2–4 triệu / tháng — đây là **chi phí thu hút nguồn cung** có chủ đích. Chưa tính lương nhân sự; mục tiêu chạy thử là **kiểm chứng hành vi** (chuyển đổi, hoàn hàng, khiếu nại), không phải có lãi.

---

<a id="p10"></a>
## PHẦN 10 — RỦI RO KINH DOANH & CÁCH TLCN KIỂM SOÁT

| Rủi ro | Biểu hiện | Kiểm soát trong TLCN |
|---|---|---|
| Người bán lừa đảo (hàng giả, không giao) | Khiếu nại tăng | KYC theo tầng; Gemini + Moderator kiểm duyệt, giấy tờ chứng thực cho ngành yêu cầu; ký quỹ 7 ngày; rút tiền chỉ về tài khoản chính chủ; Admin khóa shop (UC-ADM-03) |
| Người mua lạm dụng hoàn tiền | Khiếu nại "không nhận được hàng" khi đã nhận | Bằng chứng ảnh / video bắt buộc; GHN có trạng thái giao + chữ ký; phân xử 2 chiều |
| Lạm dụng voucher (nhiều tài khoản) | Chi phí voucher sàn vượt kế hoạch | `usagePerUser`, `budget`, OTP email khi đăng ký; theo dõi KPI chi phí voucher |
| Tự mua hàng của chính mình (thổi đánh giá, quay vòng voucher) | | Chặn `GNE-ORD-4094` |
| COD bom hàng | Hoàn hàng, mất phí ship | Ký quỹ COD chỉ nạp khi tiền về; theo dõi tỷ lệ; có thể tắt COD theo shop / khách ở DATN |
| Đánh giá giả / spam | Điểm shop bị thổi hoặc bị dìm | Chỉ đơn `COMPLETED` được đánh giá; báo cáo vi phạm, Moderator / CS ẩn (UC-SOC-05); tính lại điểm |
| Lạm dụng Điểm / điểm danh nhiều tài khoản | Chi phí Điểm vượt kế hoạch | Điểm giới hạn 50% đơn, hết hạn 12 tháng; `balanceHash`; theo dõi KPI chi phí khuyến mãi |
| Nhân viên lạm quyền | Duyệt chi sai, sửa hoa hồng | 13 mã quyền, tách biệt chi tiền / sửa hoa hồng, nhật ký hệ thống (UC-ADM-07) |
| Bán vượt tồn kho | Hủy đơn, mất uy tín | Giữ chỗ có điều kiện + `@Version` |
| Lệch tiền | Ví shop ≠ sổ giao dịch | `balanceHash`, sổ chỉ ghi thêm, job đối soát đêm |
| Pháp lý (sàn có chức năng thanh toán) | Nghĩa vụ khấu trừ thuế, đăng ký sàn | Xem báo cáo pháp lý TLCN — chỉ vận hành thương mại sau khi hoàn tất |

---

<a id="p11"></a>
## PHẦN 11 — ÁNH XẠ LOGIC KINH DOANH SANG ENTITY & USE CASE

| Logic kinh doanh | Entity / trường | Use case |
|---|---|---|
| Hoa hồng theo ngành, ưu đãi theo shop | `Category.commissionRate`, `VendorProfile.commissionRate`, `OrderItem.commissionRate/commissionAmount` | UC-CAT-14, UC-PAY-05 |
| Ai chịu voucher, Freeship, Điểm | `Voucher.sponsorType`, `Voucher.platformSharePercent`, `OrderDiscount.fundedBy`, `OrderItem.coinDiscount`, `FinancialLedger` | UC-PRO-01…04, UC-PRO-10, UC-PAY-06 |
| Flash Sale | `FlashSaleCampaign`, `FlashSaleItem.flashStock / soldCount` | UC-PRO-05, 06 |
| Giữ chân người mua | `UserCoin`, `CoinTransaction.expiresAt`, `DailyCheckin`, `ShopFollow` | UC-PRO-08, 09, UC-SOC-06 |
| Bảo vệ người mua 7 ngày | `EscrowLedger.status/releaseEligibleAt`, `Order.deliveredAt` | UC-ORD-11, 12, UC-PAY-07 |
| Hai mốc tiền COD | `Order.paymentConfirmedAt` ≠ `fundedAt` | UC-LOG-03 |
| Hoàn một phần không làm sàn lỗ | `EscrowLedger.applyPartialRefund()` | UC-SUP-04 |
| Công nợ sàn với shop | `VendorWallet`, `WalletTransaction`, `WithdrawalRequest` | UC-ADM-04, 05 |
| Niềm tin người bán | `KycResult.riskScore`, `VendorProfile.payoutAccountVerifiedAt` | UC-VND-02…07 |
| Uy tín sản phẩm & shop | `ProductReview` (chỉ đơn hoàn tất), `ReviewHelpfulVote`, `ReviewReport`, `VendorProfile.ratingAvg / followerCount / chatResponseRate` | UC-SOC-03, 05, UC-CAT-16 |
| Chất lượng hàng hóa | `ProductModerationResult`, `ProductDocument` | UC-CAT-09, 12, 13 |

---

## LỊCH SỬ PHIÊN BẢN
- **MVP v1.0 (2026-10-28):** viết lại từ bản đầy đủ v1.6 cho phạm vi MVP: một dòng doanh thu (hoa hồng), voucher 2 tầng, COD qua GHN, KPI và dự báo 6 tháng chạy thử; lược bỏ Điểm, hạng thành viên, affiliate, Flash Sale, gói VIP, PPC, tài xế, BNPL, B2B.
- **TLCN v1.1 (2026-10-29):** phạm vi 86 use case; thêm Phần 5B (voucher 3 tầng, Điểm, Flash Sale — ai chịu tiền, ví dụ tính tiền), 5 cơ chế niềm tin, KPI khuyến mãi / kiểm duyệt / phản hồi chat / chia sẻ / Flash Sale, rủi ro đánh giá giả, Điểm, nhân viên lạm quyền; COD kết hợp ký quỹ.
