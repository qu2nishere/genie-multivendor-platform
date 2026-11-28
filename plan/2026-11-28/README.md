# PLAN 2026-11-28 — Nhóm C: chuỗi đơn hàng → tiền, từ C3 tới C11 (33 use case)

> **Nguồn chọn việc:** [`docs/Document/BAO_CAO_UC_DOC_LAP.md`](../../docs/Document/BAO_CAO_UC_DOC_LAP.md) mục 6 — nhóm C *Chuỗi đơn hàng → tiền (làm tuần tự)*. C1 Đăng bán (plan `2026-11-26/c1.md`) và C2 Giỏ → Checkout (plan `2026-11-25/server-spring/plan_gio_hang_checkout_c2.md`) đã có trên `develop`; bộ plan này phủ phần còn lại **C3 → C11**.
> **Tài liệu chuẩn:** `docs/Document/` (TLCN v1.4). Viết tắt như [README 2026-11-16](../2026-11-16/README.md): **TS** = `kien-thuc/TECH_STACK_VA_QUY_UOC.md` · **UC** = `diagrams/UseCaseDiagram/Usecase.md` · **API** = `document/API_SPEC.md` · **ENTITY** = `diagrams/ClassDiagram/ENTITY.md` · **LLD** = `document/LLD.md` · **LUU_TRU** = `kien-thuc/LUU_TRU_DU_LIEU.md`.
> Khi các nguồn mâu thuẫn: TS mục 12 → UC / ENTITY → Document_Vi → implement; **đường dẫn và hình dạng request / response lấy theo API**.

> **Phụ trách:** **không ghi tên cố định trong plan.** Người nhận một plan là người có **`git config user.name`** trên máy đang làm plan đó. Khi bắt đầu (task `xx00.01` của mỗi plan) chạy `git config user.name` / `git config user.email`, ghi kết quả vào dòng *Phụ trách* của progress, và dùng **đúng tên đó** khi tick cột *Phụ trách* ở `CHECKLIST_TIEN_DO_UC.md`, `BAO_CAO_UC_DOC_LAP.md`, cột AA + sheet *Thành viên* của `Quản Lý UseCase.xlsx` ở giai đoạn cuối.
> **Người commit:** chính danh tính git đã cấu hình trên máy người làm — không `--author`, không `--no-verify`, **không** dòng `Co-Authored-By` hay bất kỳ dấu vết AI nào trong commit / PR.

## 1. Danh sách plan (mỗi plan một progress cùng tên)

| # | Chuỗi | Plan | Progress | Use case | Mã task | Nhánh (tách từ `develop`, xếp chồng theo thứ tự) |
|---|---|---|---|---|---|---|
| 1 | **C3** Thanh toán & ký quỹ | [plan_thanh_toan_ky_quy_c3.md](server-spring/plan_thanh_toan_ky_quy_c3.md) | [progress](server-spring/progress_thanh_toan_ky_quy_c3.md) | Khởi tạo thanh toán trực tuyến — VNPay / VietQR qua SePay (UC-PAY-01) · Xử lý IPN VNPay & webhook SePay idempotent (UC-PAY-03) · Ghi nhận ký quỹ & Khấu trừ hoa hồng (UC-PAY-05) · Ghi sổ cái tài chính kép bất biến (UC-PAY-06) · Tự động giải phóng tiền ký quỹ về ví người bán (UC-PAY-07) | `Exx.yy` | `feature/c3-ledger-escrow` → `feature/c3-vnpay` → `feature/c3-sepay-vietqr` → `feature/c3-escrow-release` |
| 2 | **C4** Vận chuyển | [plan_van_chuyen_c4.md](server-spring/plan_van_chuyen_c4.md) | [progress](server-spring/progress_van_chuyen_c4.md) | Người bán xác nhận / từ chối đơn mới (UC-LOG-16) · Tiếp nhận & Đóng gói (UC-LOG-01) · Đẩy vận đơn sang GHN & In phiếu PDF (UC-LOG-02) · Điều phối đa hãng vận chuyển (UC-LOG-15) · Đồng bộ hành trình qua Webhook GHN (UC-LOG-03, gồm đối soát COD) | `Hxx.yy` | `feature/c4-seller-orders` → `feature/c4-ghn-shipment` → `feature/c4-carrier-webhook` → `feature/c4-cod-reconciliation` |
| 3 | **C5** Sau giao hàng | [plan_sau_giao_hang_c5.md](server-spring/plan_sau_giao_hang_c5.md) | [progress](server-spring/progress_sau_giao_hang_c5.md) | Theo dõi hành trình đơn (UC-ORD-09) · Khách yêu cầu hủy đơn (UC-ORD-10) · Xác nhận "Đã nhận hàng" (UC-ORD-11) · Tự hoàn tất đơn sau 7 ngày (UC-ORD-12) | `Dxx.yy` | `feature/c5-buyer-orders` → `feature/c5-cancel-complete` |
| 4 | **C6** Khuyến mãi khi mua | [plan_khuyen_mai_khi_mua_c6.md](server-spring/plan_khuyen_mai_khi_mua_c6.md) | [progress](server-spring/progress_khuyen_mai_khi_mua_c6.md) | Stacking Voucher 3 tầng & Điểm khi Checkout (UC-PRO-04) · Dùng Điểm Genie giảm trừ thanh toán (UC-PRO-10) · Tạo chiến dịch Flash Sale (UC-PRO-05) · Mua hàng Flash Sale chống bán vượt suất (UC-PRO-06) | `Xxx.yy` | `feature/c6-voucher-stacking` → `feature/c6-coins-at-checkout` → `feature/c6-flash-sale` |
| 5 | **C7** Đánh giá | [plan_danh_gia_c7.md](server-spring/plan_danh_gia_c7.md) | [progress](server-spring/progress_danh_gia_c7.md) | Đánh giá sản phẩm đã mua (UC-SOC-03) · Shop phản hồi đánh giá (UC-SOC-04) · Báo cáo & Kiểm duyệt đánh giá (UC-SOC-05) · Phân tích cảm xúc PhoBERT — phía backend (UC-AI-07) | `Rxx.yy` | `feature/c7-reviews` → `feature/c7-review-moderation` → `feature/c7-sentiment-backend` |
| 6 | **C7** (AI Worker) | [plan_phan_tich_cam_xuc.md](server-python/plan_phan_tich_cam_xuc.md) | [progress](server-python/progress_phan_tich_cam_xuc.md) | Phân tích cảm xúc đánh giá bằng PhoBERT — phía AI Worker (UC-AI-07) | `Zxx.yy` | `feature/ai-worker-sentiment` |
| 7 | **C8** Trả hàng & hoàn tiền | [plan_tra_hang_hoan_tien_c8.md](server-spring/plan_tra_hang_hoan_tien_c8.md) | [progress](server-spring/progress_tra_hang_hoan_tien_c8.md) | Tạo yêu cầu Trả hàng / Hoàn tiền (UC-SUP-01) · Shop xử lý yêu cầu (UC-SUP-02) · Sàn phân xử tranh chấp (UC-SUP-03) · Thực hiện hoàn tiền VNPay / VietQR / COD (UC-SUP-04) | `Uxx.yy` | `feature/c8-refund-requests` → `feature/c8-disputes` → `feature/c8-refund-execution` |
| 8 | **C9** Ví & rút tiền | [plan_vi_rut_tien_c9.md](server-spring/plan_vi_rut_tien_c9.md) | [progress](server-spring/progress_vi_rut_tien_c9.md) | Xem ví & sổ giao dịch người bán (UC-VND-20) · Shop gửi yêu cầu rút tiền (UC-ADM-04) · Sàn duyệt & chuyển khoản Payout (UC-ADM-05) | `Ixx.yy` | `feature/c9-seller-wallet` → `feature/c9-payouts` |
| 9 | **C10** Báo cáo | [plan_bao_cao_c10.md](server-spring/plan_bao_cao_c10.md) | [progress](server-spring/progress_bao_cao_c10.md) | Báo cáo doanh thu & Phân tích bán hàng (UC-VND-08) · Dashboard doanh thu & ký quỹ toàn sàn (UC-ADM-06) · Chế độ tạm nghỉ bán hàng (UC-VND-19) | `Qxx.yy` | `feature/c10-vacation` → `feature/c10-seller-analytics` → `feature/c10-admin-dashboard` |
| 10 | **C11** Email giao dịch | [plan_email_giao_dich_c11.md](server-spring/plan_email_giao_dich_c11.md) | [progress](server-spring/progress_email_giao_dich_c11.md) | Gửi email giao dịch tự động (UC-NOT-03) | `Jxx.yy` | `feature/c11-email-outbox` → `feature/c11-transaction-emails` |

**Vì sao 10 plan cho 9 chuỗi:** UC-AI-07 có hai nửa khác ngôn ngữ / khác người làm — backend (sự kiện, endpoint nội bộ, thống kê, hàng chờ ưu tiên) nằm trong plan C7, AI Worker Python (PhoBERT ONNX) tách thành plan riêng `server-python/` như tiền lệ KYC 2026-11-16 (plan [7] ↔ [8]); hai plan nối nhau bằng hợp đồng HTTP chốt ở R00 ↔ Z00.

## 2. Thứ tự & phụ thuộc

```
C2 (đã có: giỏ, preview, checkout-session, sweeper) ──► [1] C3 Thanh toán & ký quỹ ──► [2] C4 Vận chuyển ──► [3] C5 Sau giao hàng
                                                         │  (PAY-07 giải ngân)          │ (webhook DELIVERED,  │ (COMPLETED)
                                                         │                              │  đối soát COD)       ├──► [5] C7 Đánh giá ◄── [6] AI Worker PhoBERT
                                                         │                              │                      └──► [7] C8 Trả hàng & hoàn tiền
                                                         ├──► [8] C9 Ví & rút tiền (cần ví có tiền từ PAY-07 + tài khoản nhận tiền B4)
C2 + B5 (voucher đã có) ─────► [4] C6 Khuyến mãi khi mua (làm SONG SONG với [1]–[3]: chỉ cài 3 cổng checkout của C2)
[1]…[8] ─────────────────────► [9] C10 Báo cáo (đọc dữ liệu — viết code sớm được, kiểm số liệu sau cùng)
[1]…[8] ─────────────────────► [10] C11 Email (hạ tầng J01–J03 làm sớm; mỗi chuỗi tự gọi `TransactionalMailer` từ listener của mình)
```

- **Tuần tự bắt buộc:** C3 → C4 → C5. C5 cần vận đơn + ngày giao (C4) và ký quỹ `HELD` (C3); C4 cần đơn `PAID` (C3).
- **Song song được:** C6 (chỉ đụng checkout + promotion) ngay từ đầu; C11 giai đoạn hạ tầng (J01–J03) ngay từ đầu; plan AI Worker [6] ngay từ đầu (hợp đồng đã có ở API 12.1).
- **Sau C5:** C7 và C8 song song (C7 cần đơn `COMPLETED`; C8 cần đơn `DELIVERED` + ký quỹ + hoàn tiền cổng của C3). C9 cần C3 (PAY-07 ghi ví) — làm được ngay sau C3 nhưng chỉ kiểm đầy đủ khi có đơn hoàn tất (C5).
- **Sau cùng:** C10 (đọc mọi bảng), rồi C11 giai đoạn J04+ (email cho sự kiện của các chuỗi đã xong).
- **Một người làm hết:** C3 → C4 → C5 → C6 → C7 (+[6]) → C8 → C9 → C10 → C11. Nhiều người: chia theo cột *Song song được* ở trên.

## 3. Đồ thị phụ thuộc module — BẮT BUỘC đọc trước khi viết code

`ModuleBoundaryTest.modules_free_of_cycles` chặn mọi vòng phụ thuộc. Hiện trạng `develop` @ `1b66eae` (đếm `import com.genie.<module>` 2026-11-28):

```
order ──► auth, catalog, notification, shipping, social, vendor      vendor ──► admin, auth, catalog, notification, social
shipping ──► vendor        promotion ──► catalog, notification        catalog ──► auth, social        social ──► auth, notification
admin ──► auth, catalog     notification ──► auth                     auth, payment, support ──► (không module nào)
```

Các plan **chỉ được thêm các cạnh sau** (đã kiểm không tạo vòng):

| Cạnh mới | Plan | Vì sao | Cấm làm ngược lại / cách tránh vòng |
|---|---|---|---|
| `payment ──► order` | C3 | cài `PaymentInitiator` (cổng của C2), IPN gọi `OrderPaymentFacade` (đánh dấu đơn `PAID`, xác nhận giữ chỗ) | `order` **không** import `payment`: order cần gì của payment thì khai **cổng** trong order, payment cài |
| `payment ──► vendor`, `payment ──► catalog`, `payment ──► notification` | C3 | tỷ lệ hoa hồng (`CategoryFacade.effectiveCommissionRate`, hoa hồng riêng của shop), cài `ShopBootstrapContributor` (tạo ví), `PayoutAccountGuard` (C9) | `vendor` / `catalog` không import `payment` |
| `payment ──► shipping` | C4 | đối soát COD (`CodRemittance` ở `shipping/cod`) ghi ký quỹ `HELD` | `shipping` **không** import `payment` |
| `order ──► shipping` (đã có) | C4 | `SellerFulfillmentService` (order) gọi `ShipmentFacade` tạo vận đơn | **`shipping` không import `order`** (vòng `order → shipping → order`): webhook hãng cập nhật đơn bằng **sự kiện đồng bộ** `ShipmentStatusChangedEvent` (`shipping.event`) mà `order` lắng nghe trong **cùng transaction** (`@EventListener`, không `AFTER_COMMIT`) |
| `promotion ──► order` | C6 | cài `CheckoutDiscountPolicy`, `CheckoutCompensation` (cổng C2) và `FlashSaleReservations` (cổng mới của order) | `order` **không** import `promotion` (đã đúng hôm nay) |
| `support ──► order`, `support ──► payment`, `support ──► shipping`, `support ──► notification`, `support ──► promotion` | C8 | yêu cầu trả hàng đổi trạng thái đơn, phong tỏa ký quỹ, hoàn tiền cổng, vận đơn chiều về, hoàn Điểm theo phần phân bổ (`CoinFacade`) | không module nào import `support`; `order` cần tạo yêu cầu hoàn tự động (UC-ORD-10 b4) → **cổng** `CancellationRefunds` trong order, support cài |
| `order ──► social` (đã có) | C7 | cài `ReviewEligibility` (cổng của social: dòng hàng đã `COMPLETED` của người gọi) | **`social` không import `order` / `catalog` / `vendor` / `promotion`**: điểm sản phẩm → cổng `ReviewProductStats` (catalog cài, `catalog → social` đã có); điểm shop → `ReviewShopStats` (vendor cài); thưởng 50 Điểm → `ReviewCoinBonus` (promotion cài — cạnh mới `promotion ──► social`) |
| `promotion ──► social` | C6, C7 | C6: nhắc Flash Sale cho người theo dõi shop (`ShopFollowFacade`); C7: cài `ReviewCoinBonus` | `social` không import `promotion` |
| — (không cạnh mới) | C9 | API ví shop + API Admin rút tiền đặt ở **module `payment`** (`payment/wallet/api`) | **`admin` không import `payment`** (vòng `payment → vendor → admin → payment`) — controller Admin của rút tiền nằm trong `payment` như tiền lệ voucher sàn nằm trong `promotion` |
| — (không cạnh mới) | C10 | báo cáo shop (`vendor/analytics`) và dashboard sàn (`admin/dashboard`) đọc bằng **SQL read-model** JOIN thẳng bảng `orders`, `order_items`, `escrow_ledgers`, … | `vendor` / `admin` **không** import `order` / `payment` (vòng); chặn tạm nghỉ khi còn đơn → cổng `VacationOrderGuard` trong vendor, order cài (`order → vendor` đã có) |
| — (không cạnh mới) | C11 | `notification.email` chỉ cung cấp `TransactionalMailer` + outbox; **module nguồn** (order, payment, support, vendor) tự gọi từ listener `AFTER_COMMIT` của mình (tiền lệ `CheckoutNotificationListener`) | **`notification` không import `order` / `payment` / `support`** (vòng `order → notification → order`) |

## 4. Điểm dùng chung — tránh conflict

| Điểm chung | Quy ước |
|---|---|
| **Migration** | Mỗi chuỗi tối đa **một** migration (số ở mục 5 là **tạm** — đổi theo thứ tự merge). Trước khi mở PR: `git fetch && git log origin/develop --oneline -- server-spring/src/main/resources/db/migration`; số đã bị chiếm → đổi sang số kế tiếp, sửa mọi chỗ ghi số trong plan / ENTITY (tiền lệ đổi `V26 → V27` ngày 2026-11-28). Sửa `ck_system_logs_action_type` / `ck_notifications_type` thì **chép đủ** danh sách của migration mới nhất rồi thêm giá trị của mình. Sau khi merge không sửa |
| `common/error/ErrorCode.java` + `web/src/shared/api/error-codes.ts` + `errors.GNE-*` (vi / en) | Mỗi plan liệt kê mã ở mục 5 của plan. Đa số đã có ở API 15 (chỉ chưa có trong code). Thêm đúng khối miền (`// ===== Payment`, `// ===== Logistics`…) |
| `common/storage/UploadPurpose.java` | C7: `REVIEW_IMAGE`, `REVIEW_VIDEO` (bucket công khai `review-media`); C8: `REFUND_EVIDENCE` (bucket **riêng tư** `refund-evidence`) — thêm dòng, ghi LUU_TRU |
| `common/audit/AuditAction.java` + `category()` | Giá trị mới chỉ thêm kèm migration: C4 `COD_RECONCILED`; C6 `FLASH_SALE_CREATED / SCHEDULED / ENDED / ITEM_REVIEWED`; C7 `REVIEW_RESTORED`; C8 `ESCROW_FROZEN / ESCROW_UNFROZEN / DISPUTE_RESOLVED`. Đã có sẵn (dùng luôn): `ORDER_STATUS_CHANGED`, `ESCROW_RELEASED`, `PAYOUT_APPROVED / REJECTED`, `REFUND_PROCESSED`, `REVIEW_HIDDEN` |
| `notification/inbox/domain/NotificationType` | Đã đủ cho C3–C9 (`PAYMENT_SUCCESS / FAILED`, `ESCROW_RELEASED`, `WALLET_CREDITED`, `REFUND_PROCESSED`, `NEW_ORDER_FOR_VENDOR`, `ORDER_STATUS_CHANGED`, `FLASH_SALE_REMINDER`, `SYSTEM_ALERT`). Loại mới duy nhất: `REVIEW_REPLIED` (C7, `V31`). **C11 ghi dòng `channel = EMAIL` vào cùng bảng → mọi truy vấn hộp thư phải lọc `channel = IN_APP`** |
| **Sổ giao dịch ngân hàng SePay** (`bank_transactions`, C3) | **C3 sở hữu.** Một bảng ghi mọi webhook SePay (vào / ra, chống trùng theo `id` SePay) dùng chung: C3 (tiền vào `TR{id}`), C4 (tiền vào từ hãng vận chuyển — đối soát COD), C8 (tiền ra `TRRF{id}`), C9 (tiền ra `TRWD{id}`). Mỗi chuỗi sau **đăng ký một bộ khớp** (`BankTransferMatcher`) — không viết webhook thứ hai |
| `payment/ledger` — `LedgerPoster` (C3) | **C3 sở hữu.** Mọi bút toán sổ cái đi qua `LedgerPoster.post(group)` (kiểm Nợ = Có, cùng transaction). C4, C8, C9 chỉ **gọi**, không tự `INSERT financial_ledger` |
| `payment/escrow` — `EscrowFacade` (C3) | **C3 sở hữu** `fund / hold / scheduleRelease / release / freeze / dispute / refund`. C4 (COD), C5 (hoàn tất), C8 (khiếu nại, hoàn) chỉ gọi facade |
| `payment/wallet` — `WalletPoster` (C3) | **C3 sở hữu** ghi ví (`FOR UPDATE` + `balanceHash` + `wallet_transactions`). C8 (thu hồi), C9 (rút) gọi `WalletPoster`, không tự sửa `vendor_wallets` |
| Cổng của `order` | C2 đã có `PaymentInitiator`, `CheckoutDiscountPolicy`, `CheckoutCompensation`. Thêm: `OrderPaymentFacade` (C3, order cài dùng trong order), `FlashSaleReservations` (C6), `CancellationRefunds` (C4 khai cùng `OrderLifecycleService` — C8 cài bản thật; trước khi C8 xong bản mặc định chỉ tạo AdminNotification "đơn cần hoàn tiền" để Staff hoàn tay), `ReviewEligibility` (C7) |
| `order/order` — máy trạng thái đơn | `Order` entity giữ luật chuyển trạng thái (Document_Vi 3.4.2 / ENTITY 5.2). C4 (xác nhận, chuẩn bị, giao), C5 (hủy, nhận hàng, hoàn tất), C8 (trả hàng) chỉ gọi phương thức của entity qua `OrderLifecycleService` (C4 tạo) — **không** `UPDATE orders SET status` bằng SQL tay |
| `web/` | Mỗi plan bật **cờ riêng** trong `PUBLIC_REAL_API` (bảng mục 7 của plan); không bật cờ của plan khác. Màn dùng chung (chi tiết đơn của khách: C4 hiện vận đơn, C5 hủy / nhận hàng, C8 trả hàng, C7 đánh giá) — mỗi plan chỉ nối **hàm** của mình trong `orders/order.api.ts` |
| `docs/web-backend-handoff.md` · `CHECKLIST_TIEN_DO_UC.md` · `BAO_CAO_UC_DOC_LAP.md` · `Quản Lý UseCase.xlsx` · Usecase.md | Mỗi plan chỉ sửa dòng của use case mình ở giai đoạn cuối. Số tổng quan đếm lại từ các dòng UC lúc đóng plan (đừng cộng nhẩm — các chuỗi khác có thể đã đổi số) |
| `CLAUDE.md` | Của bạn cùng nhóm — **không** sửa |

## 5. Sổ đăng ký migration (tạm — đổi theo thứ tự merge)

| Số tạm | File dự kiến | Plan | Nội dung |
|---|---|---|---|
| `V25`, `V26`, `V27` | đã merge | C1 + C2, C1, B5 / B6 | — |
| `V28` | `V28__c3_payment_escrow.sql` | C3 | `payment_transactions`: `checkout_session_id` (FK, thay cho quan hệ theo `order_id` — một phiên nhiều đơn), `payment_code` (`TR{id}`), `expires_at`, UNIQUE `(payment_method, gateway_transaction_id)`; bảng mới **`bank_transactions`** (sổ webhook SePay); index `escrow_ledgers (status, release_eligible_at)` |
| `V29` | `V29__c4_fulfillment_shipping.sql` | C4 | `orders`: `confirmed_at`, `auto_confirmed`, `cancel_reason`, `cancelled_by`, `cancelled_at`, `ready_for_pickup_at`; `shipment_trackings`: `label_object_key`, `shipping_fee_actual`, UNIQUE `(carrier_type, tracking_code)`; `shipment_events`: `carrier_event_id` + UNIQUE `(shipment_id, carrier_event_id)`; `AuditAction.COD_RECONCILED` |
| — | (không migration) | C5 | dùng cột của V29 (`cancel_reason`, `cancelled_by`, `cancelled_at`, `confirmed_at`, `ready_for_pickup_at`) |
| `V30` | `V30__c6_flash_sale.sql` | C6 | `flash_sale_campaigns`: `registration_deadline`, `max_items_per_shop`; `flash_sale_items`: `product_variant_id`, `vendor_id`, `review_note`, UNIQUE `(campaign_id, product_variant_id)`; bảng `flash_sale_purchases` (chặn 1 suất / người / sản phẩm / chiến dịch); 4 giá trị `AuditAction.FLASH_SALE_*` |
| `V31` | `V31__c7_review_moderation.sql` | C7 | `review_reports`: `status`, `resolved_by_staff_id`, `resolved_at`, UNIQUE `(review_id, reporter_id)`; index `product_reviews (vendor_id, created_at)`, `(product_id, is_visible, helpful_count)`; `AuditAction.REVIEW_RESTORED`; `NotificationType.REVIEW_REPLIED` (sửa `ck_notifications_type`) |
| `V32` | `V32__c8_refund_dispute.sql` | C8 | `refund_requests`: `refund_bank_snapshot_json` (tài khoản nhận hoàn của khách — VietQR / COD, mã hóa), `refund_code` (`TRRF{id}`); `refund_evidences.uploaded_by_role`; `escrow_ledgers.status_before_freeze`; 3 giá trị `AuditAction` (`ESCROW_FROZEN`, `ESCROW_UNFROZEN`, `DISPUTE_RESOLVED`) |
| `V33` | `V33__c9_withdrawals.sql` | C9 | `withdrawal_requests`: `transfer_code` (`TRWD{id}`), `bank_reference`, `approved_at`, `approved_by_admin_id`; index `(status, requested_at)` |
| — | (không migration) | C10 | chỉ đọc; nếu đo thấy chậm mới thêm index (đăng ký số khi đó) |
| `V34` | `V34__c11_email_outbox.sql` | C11 | `notifications`: `next_attempt_at`, `last_error`, `template_key`, `recipient_email`; index `(channel, delivery_status, next_attempt_at)`; UNIQUE chống gửi trùng mẫu "một lần" |

## 6. Quy ước chung (trích TS 13, 14 — không lặp lại ở từng plan)

- **Commit:** `<type>(<scope>): <subject>` — subject + body **tiếng Anh**, mệnh lệnh, chữ thường, không dấu chấm cuối, header ≤ 100 ký tự; `scope` ∈ `commitlint.config.cjs` (`payment`, `order`, `shipping`, `promotion`, `social`, `support`, `vendor`, `admin`, `notification`, `common`, `db`, `web`, `seller-ui`, `admin-ui`, `ai-worker`, `api`…). Chưa có mã Jira → bỏ ` [#GNE-…]`. Một commit = một thay đổi logic, build được, test xanh. **Tài liệu đi trước** (TS 7.3): commit `docs(<scope>): …` đầu nhánh.
- **Cổng kiểm tra trước mỗi task COMMIT:** `cd server-spring && ./mvnw verify` (JDK 21) — unit + IT + ArchUnit (`ModuleBoundaryTest`, **đúng đồ thị mục 3**) + `SchemaValidationIT` + `SeedDataIT`; đụng `web/` thì `cd web && npm run verify`; đụng `server-python/` thì `pytest` + `ruff`; `git add` đúng từng file (không `git add .`).
- **Tiền:** CSDL `NUMERIC(19,2)` / `BigDecimal`, API số nguyên VND; mọi thay đổi tiền / tồn / Điểm / suất **trong transaction chính**; `@Async @TransactionalEventListener(AFTER_COMMIT)` chỉ cho thông báo, email, cache, gọi AI (TS 4). Năm kịch bản tiền bắt buộc của TS 14 phải có IT ở plan sở hữu: IPN trùng (C3), hai lần giải ngân song song (C3), rút tiền song song vượt số dư (C9), hoàn tiền một phần nhiều lần không vượt số đã trả (C8), mua Flash Sale song song không vượt suất (C6).
- **Bên thứ ba:** mỗi tích hợp có chế độ `stub` mặc định ở profile `local` / test và `http` thật bật bằng biến môi trường (`PAYMENT_MODE`, `GHN_MODE`, `GHN_PICKUP_MODE` đã có, `CARRIER_MODE`, `AI_WORKER_MODE` đã có). Biến mới khai ở `.env.example`, `deploy/.env.example`, bảng `docs/cau-hinh-moi-truong.md` cùng PR. Không ghi bí mật vào `application*.yml`.
- **PR:** vào `develop`, ≤ ~400 dòng (vượt thì tách theo nhánh xếp chồng của plan — mỗi nhóm task đã có commit riêng), `git fetch && git merge origin/develop` (hoặc rebase) trước khi xin duyệt; PR xếp chồng ghi nhánh nền ở dòng đầu mô tả.
- **Chạy thật (cột *Chạy*):** web bản build (`PUBLIC_REAL_API=<cờ của plan>,…`) + backend `local` + Postgres + Redis + Silo + Mailpit (+ AI Worker cho C7), thao tác bằng **Chromium điều khiển qua Playwright** theo kịch bản `docs/kiem-thu/kich-ban-2026-11-28-<chuỗi>.md`; đạt hết ca mới tick `Chạy ☑` và ghi cách chạy + số ca vào progress. Webhook (VNPay IPN, SePay, GHN) chạy thật bằng **sandbox của hãng** khi có khóa; chưa có khóa thì gọi webhook bằng `docs/api-tests/*.http` với chữ ký tính đúng thuật toán và ghi rõ "chưa kiểm với hãng thật" ở ô *Chạy* (`◐`).
- **Xong một use case** (TS 13.7): code + test + migration + `@Operation(summary = "UC-…")` + i18n vi / en + chạy thật + cập nhật `CHECKLIST_TIEN_DO_UC.md`, `BAO_CAO_UC_DOC_LAP.md`, `Quản Lý UseCase.xlsx`, câu *Hiện trạng* trong Usecase.md — **cột Phụ trách = `git config user.name` của người làm**.

## 7. Tiến độ các plan & tick checklist use case

**Luật:** xong use case nào thì tick ngay ở `CHECKLIST_TIEN_DO_UC.md` (5 ô GD / BE / Test / Nối / Chạy, Trạng thái, số mục 1 *Tổng quan* + bảng *Tiến độ theo phân hệ*, một dòng mục 5 *Nhật ký*), đổi *Hiện trạng mã nguồn* + cột AA trong Excel, câu *Hiện trạng* trong Usecase.md, bảng cụm trong `BAO_CAO_UC_DOC_LAP.md` — rồi tick dòng dưới đây.

| Thứ tự | Plan | Use case phải tick khi xong | Phân hệ checklist | Phụ trách (`git config user.name`) | Xong plan |
|:---:|---|---|---|---|:---:|
| 1 | C3 Thanh toán & ký quỹ | `UC-PAY-01`, `UC-PAY-03`, `UC-PAY-05`, `UC-PAY-06`, `UC-PAY-07` | 04 (+5) | | ☐ |
| 2 | C4 Vận chuyển | `UC-LOG-16`, `UC-LOG-01`, `UC-LOG-02`, `UC-LOG-15`, `UC-LOG-03` | 06 (+5) | | ☐ |
| 3 | C5 Sau giao hàng | `UC-ORD-09`, `UC-ORD-10`, `UC-ORD-11`, `UC-ORD-12` | 03 (+4) | | ☐ |
| 4 | C6 Khuyến mãi khi mua | `UC-PRO-04`, `UC-PRO-10`, `UC-PRO-05`, `UC-PRO-06` | 07 (+4) | | ☐ |
| 5 | C7 Đánh giá + [6] AI Worker | `UC-SOC-03`, `UC-SOC-04`, `UC-SOC-05`, `UC-AI-07` | 08 (+4) | | ☐ |
| 6 | C8 Trả hàng & hoàn tiền | `UC-SUP-01`, `UC-SUP-02`, `UC-SUP-03`, `UC-SUP-04` | 09 (+4) | | ☐ |
| 7 | C9 Ví & rút tiền | `UC-VND-20`, `UC-ADM-04`, `UC-ADM-05` | 05 (+1), 11 (+2) | | ☐ |
| 8 | C10 Báo cáo | `UC-VND-08`, `UC-ADM-06`, `UC-VND-19` | 05 (+2), 11 (+1) | | ☐ |
| 9 | C11 Email giao dịch | `UC-NOT-03` | 10 (+1) | | ☐ |

Khi cả 9 dòng `☑`: tổng quan checklist dự kiến **Đã triển khai +33** (37 → 70 theo số 2026-11-28 — đếm lại từ các dòng UC lúc đóng mỗi plan).
