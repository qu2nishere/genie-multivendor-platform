# BÁO CÁO HỆ THỐNG CHÍNH SÁCH & HỢP ĐỒNG PHÁP LÝ — BẢN TLCN

**Dự án Genie — Sàn TMĐT đa người bán (phạm vi đồ án)** · TLCN v1.4 (2026-11-16 — đồng bộ với mã nguồn)

> **Mục đích:** xác định văn bản chính sách / hợp đồng **tối thiểu** mà MVP cần có trên giao diện và trong quy trình, cùng các nghĩa vụ pháp lý phải hoàn tất **trước khi** vận hành thương mại.
> **Nguồn:** rút gọn từ bản đầy đủ `DATN/kien-thuc/BAO_CAO_HE_THONG_CHINH_SACH_HOP_DONG_PHAP_LY_v1.0.md` (14 nhóm). Căn cứ pháp lý giữ nguyên tên văn bản như bản đầy đủ.
> ⚖️ **Tuyên bố:** tài liệu phục vụ học tập / đồ án, **không phải tư vấn pháp lý**. Trước khi mở cho người dùng thật phải có pháp chế / luật sư rà soát, và kiểm tra văn bản pháp luật hiện hành tại thời điểm triển khai.

---

## PHẦN 0 — CHECKLIST TỔNG THỂ CHO TLCN

| Nhóm | Văn bản / việc cần có | Demo đồ án | Chạy thử thương mại | Căn cứ chính |
|---|---|:---:|:---:|---|
| 1. TMĐT & sàn | Quy chế hoạt động sàn; thông tin người sở hữu sàn ở chân trang; đăng ký sàn với Bộ Công Thương | Bản nháp công khai trên web | ✅ Bắt buộc | Luật TMĐT 2025; NĐ 248/2026 |
| 2. Dữ liệu cá nhân | Chính sách quyền riêng tư; đồng ý khi đăng ký; **đồng ý riêng** cho dữ liệu sinh trắc học khi KYC; quy trình xử lý yêu cầu của chủ thể dữ liệu (xóa tài khoản tự phục vụ — UC-AUTH-10; yêu cầu khác qua email) | ✅ | ✅ | Luật BVDLCN 2025 (kế thừa NĐ 13/2023) |
| 3. Thuế | Quy trình khấu trừ & nộp thuế thay hộ / cá nhân kinh doanh khi sàn có chức năng thanh toán | Không áp dụng (không có giao dịch thật) | ⚠️ **Phải bổ sung** (chưa có trong MVP) | NĐ 117/2025/NĐ-CP |
| 4. Thanh toán | Hợp đồng với VNPay; **thỏa thuận dịch vụ SePay** (theo dõi biến động tài khoản ngân hàng của sàn cho VietQR, COD, lệnh chi); mở tài khoản doanh nghiệp nhận tiền; chính sách thanh toán & hoàn tiền; **không giữ ví tiền người mua** | Sandbox VNPay; SePay với tài khoản demo / webhook giả lập | ✅ | NĐ 52/2024/NĐ-CP |
| 15. Giấy phép phần mềm | Rà giấy phép mã nguồn mở của các thành phần (Silo AGPL-3.0, MinIO Java SDK Apache-2.0, PostgreSQL, Redis…); không sửa mã Silo, chạy như dịch vụ riêng | ✅ (ghi trong báo cáo) | ✅ | Điều khoản giấy phép AGPL-3.0 |
| 5. Vận chuyển | Chính sách vận chuyển (GHN mặc định, GHTK / ViettelPost / J&T khi shop bật); hợp đồng với từng hãng | ✅ (dev) | ✅ | — (không tự vận hành đội xe) |
| 6. Người bán | Hợp đồng điện tử cung ứng dịch vụ sàn / điều khoản người bán (tick đồng ý khi mở shop); biểu phí hoa hồng | ✅ | ✅ | Luật TMĐT 2025 |
| 8. Người tiêu dùng | Chính sách đổi trả & hoàn tiền 7 ngày; công bố thông tin sản phẩm (giá, xuất xứ, người bán) | ✅ | ✅ | Luật BVQLNTD 2023 |
| 9. SHTT & hàng cấm | Danh mục hàng cấm / hạn chế; giấy tờ chứng thực cho ngành yêu cầu; kiểm duyệt Gemini + Moderator; quy trình gỡ sản phẩm vi phạm (UC-CAT-11) | ✅ | ✅ | Luật TMĐT 2025 |
| 10. Tranh chấp | Quy chế giải quyết khiếu nại (theo luồng UC-SUP-01…04) | ✅ | ✅ | Luật TMĐT 2025 |
| 12. Khuyến mại | Thể lệ voucher 3 tầng, Flash Sale (khung giờ, số suất), Điểm Genie (cách nhận, hạn 12 tháng, giới hạn 50%) hiển thị công khai | ✅ | ✅ | Luật Thương mại 2005 |

**Nhóm của DATN không cần cho TLCN:** 7 (hợp đồng tài xế — không có đội xe), 11 (AML / kiểm toán nội bộ ở quy mô lớn — chỉ giữ nhật ký `system_logs`), 13 (quảng cáo PPC / KOL / livestream), 14 (ngoại hối — không đa tiền tệ), giấy phép ví điện tử (không làm ví), quy chế livestream.

---

## PHẦN 1 — TMĐT & QUY CHẾ SÀN

- **Quy chế hoạt động** (trang `/quy-che`): quyền & nghĩa vụ của sàn, người bán, người mua; quy trình đăng bán và kiểm duyệt; cơ chế ký quỹ 7 ngày; giải quyết khiếu nại; biểu phí; xử lý vi phạm (khóa tài khoản — UC-ADM-03).
- **Chân trang:** tên đơn vị sở hữu, địa chỉ, email, số điện thoại liên hệ, liên kết quy chế và chính sách.
- **Trang sản phẩm phải hiển thị:** tên shop, giá, tình trạng tồn, phí vận chuyển, chính sách đổi trả — đã có trong UC-CAT-06 / UC-CAT-16.
- **Khi demo đồ án:** ghi rõ "Website thử nghiệm phục vụ học tập — không giao dịch thật"; dùng VNPay sandbox.

## PHẦN 2 — BẢO VỆ DỮ LIỆU CÁ NHÂN

| Dữ liệu | Mục đích | Biện pháp trong TLCN | Use case |
|---|---|---|---|
| Email, SĐT, địa chỉ | Tài khoản, giao hàng (OTP chỉ gửi qua email) | Đồng ý khi đăng ký; chỉ shop của đơn thấy địa chỉ giao; che SĐT ở danh sách | UC-AUTH-01, 09 |
| CCCD, ảnh chân dung (sinh trắc) | KYC người bán | **Đồng ý riêng**; ảnh mã hóa AES-256-GCM; bucket riêng tư; Presigned URL 5 phút; ghi nhật ký khi Staff xem; xóa hồ sơ bị từ chối sau 30 ngày | UC-VND-02…04 |
| Số tài khoản ngân hàng | Giải ngân | Che khi hiển thị; chỉ Staff duyệt rút tiền thấy đầy đủ | UC-VND-07, UC-ADM-05 |
| Nội dung chat | Hỗ trợ giao dịch | Chỉ 2 bên + CS Specialist khi phân xử khiếu nại | UC-SOC-01, UC-SUP-03 |
| Email, tên, ảnh từ Google / Apple | Đăng nhập mạng xã hội | Chỉ xin `openid email profile`; ảnh đại diện Google được **sao về kho tệp của sàn** (Silo) thay vì nhúng URL của Google, tên / ảnh chỉ điền khi hồ sơ trống; liên kết thủ công chỉ khi email Google trùng email tài khoản; ghi nhật ký khi liên kết / gỡ; gỡ liên kết khi xóa tài khoản | UC-AUTH-03 |
| Lịch sử giao dịch ngân hàng nhận qua SePay (số tiền, nội dung, mã giao dịch, tên ngân hàng) | Đối soát thanh toán VietQR, tiền COD, lệnh chi | Chỉ nhận webhook có tiền tố nội dung của sàn (`TR…`); lưu mã giao dịch để chống trùng; không lưu số tài khoản người chuyển ngoài nội dung SePay gửi; SePay là **bên xử lý dữ liệu** — cần điều khoản bảo mật trong thỏa thuận | UC-PAY-03, UC-LOG-03, UC-ADM-05 |
| Thiết bị đăng nhập (User-Agent, IP) | Bảo mật tài khoản, cảnh báo đăng nhập lạ | Hiển thị cho chính chủ ở "Thiết bị đăng nhập"; lưu tối đa 90 ngày rồi job dọn xóa; chưa định vị GeoIP | UC-AUTH-02, UC-AUTH-11 |
| Nội dung sản phẩm gửi Gemini (tên, mô tả, ảnh, tên giấy tờ) | Kiểm duyệt tự động | **Không** chứa dữ liệu cá nhân (không gửi CCCD, STK, nội dung giấy tờ); ghi rõ trong điều khoản người bán rằng nội dung đăng bán được xử lý bởi dịch vụ AI bên thứ ba | UC-CAT-12 |
| Lượt xem, từ khóa tìm kiếm | Thống kê cho shop / sàn | Chỉ lưu số gộp theo ngày, không gắn danh tính | UC-CAT-04, UC-VND-08 |

**Yêu cầu của chủ thể dữ liệu** (xem / sửa / xóa): xem / sửa trong Hồ sơ; **xóa tài khoản tự phục vụ** (UC-AUTH-10 — **đã hiện thực**): chặn khi còn đơn đang xử lý / số dư ví shop (báo lý do trước khi gửi mã), xác nhận OTP, email thông báo ngày ẩn danh kèm nút giữ lại tài khoản, 30 ngày để hủy yêu cầu (vẫn đăng nhập được, các thiết bị khác bị đăng xuất), sau đó cron 02:30 ẩn danh hóa dữ liệu cá nhân (email, họ tên, SĐT, ảnh, địa chỉ, liên kết Google; ảnh xóa khỏi kho tệp), giữ đơn hàng & sổ cái theo thời hạn lưu chứng từ (Luật Kế toán — tối thiểu 10 năm); mọi bước ghi nhật ký kiểm toán; yêu cầu khác xử lý qua email trong 72 giờ.

## PHẦN 3 — THUẾ (RỦI RO CẦN NÊU RÕ)

Theo NĐ 117/2025/NĐ-CP, sàn TMĐT **có chức năng thanh toán** phải khấu trừ, khai và nộp thuế thay cho hộ / cá nhân kinh doanh bán hàng trên sàn (hàng hóa: 1% GTGT + 0,5% TNCN — tham số bản đầy đủ). MVP thu tiền khách qua tài khoản sàn (ký quỹ) nên **thuộc diện này khi vận hành thật**.
- **Trong đồ án:** không phát sinh doanh thu thật → không áp dụng; nêu rõ là giới hạn phạm vi.
- **Trước khi chạy thử thương mại:** bổ sung bảng `TaxWithholdingRecord` và luồng UC-PAY-10 từ bản đầy đủ (chỉ thêm, không đổi mô hình hiện có), hoặc thu hẹp thành mô hình chỉ cho doanh nghiệp tự xuất hóa đơn bán hàng.

## PHẦN 4 — THANH TOÁN

- Sàn **không** cung cấp ví điện tử cho người mua (tránh yêu cầu giấy phép trung gian thanh toán; vốn tối thiểu ví điện tử 50 tỷ đồng theo NĐ 52/2024 — cần pháp chế xác nhận). Tiền khách đi qua VNPay (đơn vị được cấp phép), chuyển khoản thẳng vào tài khoản doanh nghiệp của sàn (SePay chỉ đọc biến động số dư để đối soát, không giữ tiền — cần thỏa thuận xử lý dữ liệu giao dịch với SePay) hoặc GHN (thu hộ COD).
- `VendorWallet` là **sổ công nợ** sàn phải trả shop, không cho nạp tiền / chuyển tiền giữa người dùng.
- **Chính sách thanh toán & hoàn tiền:** thời hạn hoàn VNPay theo ngân hàng phát hành (thường 3–15 ngày làm việc); VietQR / COD hoàn bằng chuyển khoản trong 3 ngày làm việc sau khi phán quyết.

## PHẦN 5 — VẬN CHUYỂN

Chính sách vận chuyển nêu: các đơn vị vận chuyển (GHN mặc định; GHTK / ViettelPost / J&T khi shop bật — hệ thống chọn hãng theo phí và thời gian); phí theo bảng giá hãng hiển thị khi đặt; ngưỡng miễn phí vận chuyển do shop tự chịu; thời gian giao dự kiến; xử lý giao thất bại (GHN giao lại tối đa 3 lần rồi hoàn); hàng hư hỏng do vận chuyển → sàn khiếu nại GHN theo chính sách bồi thường của GHN, người mua vẫn được hoàn tiền theo quy trình khiếu nại.

## PHẦN 6 — HỢP ĐỒNG / ĐIỀU KHOẢN NGƯỜI BÁN

Người bán tick đồng ý **Điều khoản dịch vụ người bán** khi mở shop (UC-VND-01), lưu thời điểm và phiên bản điều khoản trong `system_logs`. Nội dung tối thiểu: phí hoa hồng theo ngành (công khai), cơ chế ký quỹ & giải ngân 7 ngày, nghĩa vụ giao hàng trong 2 ngày làm việc, nghĩa vụ xử lý khiếu nại 48 giờ, cấm hàng giả / hàng cấm, quyền của sàn khóa gian hàng & giữ tiền khi có tranh chấp, chỉ rút tiền về tài khoản chính chủ. *(Bản đầy đủ có entity `PartnerAgreement` lưu phiên bản hợp đồng — thêm khi mở rộng.)*

## PHẦN 8 — BẢO VỆ NGƯỜI TIÊU DÙNG

- **Đổi trả 7 ngày** kể từ khi giao thành công: hàng lỗi, sai mô tả, thiếu, hư hỏng → hoàn tiền toàn phần / một phần, có thể yêu cầu gửi trả hàng (UC-SUP-01…04).
- Thông tin sản phẩm trung thực; đánh giá chỉ từ người đã mua (UC-SOC-03) — tránh đánh giá giả.
- Không mua hàng của chính shop mình (chống thao túng đánh giá).

## PHẦN 9 — HÀNG CẤM & SỞ HỮU TRÍ TUỆ

Danh mục hàng cấm hiển thị ở trang đăng sản phẩm; ngành hàng có điều kiện yêu cầu giấy tờ chứng thực (`ProductDocument`); Gemini sàng lọc (UC-CAT-12) rồi Moderator duyệt trước khi hiển thị (UC-CAT-13) và khóa sản phẩm vi phạm (UC-CAT-11); đánh giá vi phạm bị báo cáo và ẩn (UC-SOC-05). Biểu mẫu báo cáo vi phạm SHTT công khai và xử lý trong 24 giờ (UC-ADM-10 bản đầy đủ) để bản mở rộng; MVP nhận báo cáo qua email liên hệ.

## PHẦN 10 — GIẢI QUYẾT TRANH CHẤP

Quy trình 3 bước trùng luồng hệ thống: (1) khách – shop tự thỏa thuận trong 48 giờ; (2) khiếu nại lên sàn, Staff phân xử dựa trên bằng chứng hai bên, vận đơn GHN, chat; (3) ngoài hệ thống: hòa giải / cơ quan bảo vệ người tiêu dùng / tòa án. Tiền luôn được **đóng băng** trong suốt tranh chấp (`EscrowLedger.DISPUTED`).

## PHẦN 12 — KHUYẾN MẠI

Mỗi voucher hiển thị thể lệ: tầng (Shop / Sàn / Freeship), mức giảm, trần, đơn tối thiểu, thời hạn, số lượt, phạm vi (shop / toàn sàn / danh mục / sản phẩm), đơn vị tài trợ. Flash Sale công bố khung giờ, giá, số suất; Điểm Genie công bố cách nhận, giá trị quy đổi (1 Điểm = 1₫), giới hạn dùng 50% và hạn 12 tháng. Tổng mức giảm ≤ 85% giá trị hàng (tham số chuẩn). Khi vận hành thật, chương trình khuyến mại vượt ngưỡng phải thực hiện thủ tục thông báo theo quy định hiện hành.

---

## PHẦN 13 — ÁNH XẠ YÊU CẦU PHÁP LÝ → THIẾT KẾ HỆ THỐNG TLCN

| Yêu cầu | Thiết kế | Use case / Entity |
|---|---|---|
| Biết người bán là ai | KYC theo tầng, rút tiền chính chủ | UC-VND-02…07 · `KycResult`, `VendorProfile` |
| Đồng ý xử lý dữ liệu | Ô đồng ý khi đăng ký, đồng ý riêng sinh trắc khi KYC, ghi nhật ký | UC-AUTH-01, UC-VND-02 · `SystemLog` |
| Bảo mật dữ liệu nhạy cảm | Mã hóa ảnh CCCD, bucket riêng tư, che dữ liệu | NFR-07 |
| Đổi trả 7 ngày & giải quyết khiếu nại | Luồng trả hàng 3 bên, đóng băng ký quỹ | UC-SUP-01…04 · `RefundRequest`, `EscrowLedger` |
| Không giữ tiền người mua dưới dạng ví | VNPay / VietQR / COD vào tài khoản sàn rồi ký quỹ theo đơn; ví shop là sổ công nợ; Điểm không quy đổi ra tiền mặt | ADR-04 · `VendorWallet`, `UserCoin` |
| Gỡ hàng vi phạm | Kiểm duyệt trước (Gemini + Moderator), khóa sau | UC-CAT-11, UC-CAT-12, UC-CAT-13 · `ProductModerationResult` |
| Quyền được xóa dữ liệu | Xóa tài khoản tự phục vụ, ẩn danh hóa sau 30 ngày (đã hiện thực) | UC-AUTH-10 · `users.deletion_requested_at`, `User.anonymize()` (xóa cả `oauthProvider`), `CustomerProfile.anonymizedAt`, `AnonymizeAccountsJob`, nhật ký `ACCOUNT_DELETION_REQUESTED` / `PII_ANONYMIZED` |
| An toàn tài khoản | Mật khẩu băm Argon2id, OTP chỉ lưu bản băm, email cảnh báo khi đổi mật khẩu / đăng nhập thiết bị lạ, đăng xuất từ xa | UC-AUTH-02/04/06/11 · `system_logs` |
| Giấy phép AGPL của kho tệp | Silo chạy như dịch vụ riêng, không sửa mã → không phát sinh nghĩa vụ công bố mã nguồn của sàn; nếu sửa mã Silo và cung cấp qua mạng phải công bố phần sửa | ADR-15 / ADR 0009 |
| Kiểm soát nội bộ nhân viên | 13 mã quyền, nhật ký hệ thống | UC-ADM-01, 02, 07 · `StaffPermission`, `SystemLog` |
| Minh bạch khuyến mại | Thể lệ voucher / Flash Sale / Điểm, sổ cái chi phí khuyến mãi | UC-PRO-01…10 · `Voucher`, `FlashSaleCampaign`, `CoinTransaction`, `FinancialLedger` |
| Lưu vết thao tác nhạy cảm | `system_logs` | NFR-08 |
| Khấu trừ thuế thay | **Chưa có** — bắt buộc trước khi thương mại | UC-PAY-10 (DATN) |

---

## LỊCH SỬ PHIÊN BẢN
- **TLCN v1.4 (2026-11-16):** đồng bộ với mã nguồn: thêm thỏa thuận SePay + dữ liệu giao dịch ngân hàng (SePay là bên xử lý dữ liệu), dữ liệu thiết bị đăng nhập (90 ngày), ảnh Google sao về kho tệp; luồng xóa tài khoản theo hiện thực (chặn trước khi gửi mã, email, giữ phiên hiện tại, cron 02:30); nhóm 15 — giấy phép phần mềm (Silo AGPL-3.0 thay MinIO).
- **MVP v1.0 (2026-10-28):** rút gọn từ bản đầy đủ (14 nhóm) còn 10 nhóm áp dụng cho MVP; tách rõ yêu cầu cho demo đồ án và cho chạy thử thương mại; nêu rủi ro khấu trừ thuế thay (NĐ 117/2025) do MVP chưa hiện thực.
- **TLCN v1.1 (2026-10-29):** cập nhật theo 86 use case: xóa tài khoản tự phục vụ (UC-AUTH-10), dữ liệu OAuth và dữ liệu gửi Gemini, đa hãng vận chuyển, kiểm duyệt Gemini + Moderator, báo cáo đánh giá, thể lệ voucher 3 tầng / Flash Sale / Điểm, kiểm soát nhân viên bằng mã quyền.
