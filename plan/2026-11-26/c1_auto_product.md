# PLAN C1 — P06: Kiểm duyệt sản phẩm tự động bằng Google Gemini (UC-CAT-12) + nút bật / tắt cho Admin

| | |
|---|---|
| **Ngày lập** | 2026-11-27 |
| **Thuộc plan** | [`c1.md`](./c1.md) — giai đoạn **P06** (task P06.01 → P06.05), quyết định **D11, D12, D13, D14** |
| **Use case** | UC-CAT-12 — Kiểm duyệt sản phẩm tự động qua AI (Google Gemini API) |
| **Yêu cầu mới của chủ dự án (2026-11-27)** | Tài khoản vai trò **ADMIN** có **nút bật / tắt** tính năng duyệt tự động. Tắt → sản phẩm gửi duyệt đi thẳng hàng chờ thủ công như hiện nay |
| **Nhánh** | `feature/c1` (hoặc nhánh con `feature/c1-ai-screening` tách từ `feature/c1`) |
| **Người commit** | Chủ dự án (`Khoa18062005`). Không `--author`, không `--no-verify`, **không** thêm `Co-Authored-By` / dấu vết AI vào commit hay PR |
| **Trạng thái trước khi làm** | P01–P05, P07, P08 xong, test xanh, đã merge vào `develop` (PR #113…). Rà lại 2026-11-28 trên `develop` @ `ecab948` (đã có B5 voucher + B6 thông báo / chat): migration mới nhất `V27__b5_b6_notification_chat_voucher.sql` — P06 **không cần migration** |

> **Đọc file này từ đầu tới cuối trước khi viết dòng code nào.** File viết để một người / một model khác (không có ngữ cảnh hội thoại) làm đúng luồng. Mọi đường dẫn, tên lớp, tên khóa đã kiểm trên code thật ở `feature/c1` ngày 2026-11-27. Chỗ nào code thật khác file này → **code thật đúng**, sửa file này và ghi chú.

---

## 0. Đọc trước (bắt buộc)

| Tài liệu | Đọc phần |
|---|---|
| `CLAUDE.md` (gốc repo) | Toàn bộ — nhất là mục 4 (git), 5 (frontend), 6 (backend), 9 (bẫy) |
| `plan/2026-11-26/c1.md` | Mục 1 (nguyên tắc **N1–N13**), dòng **D11–D14**, mục 9 (tiến độ), mục 10 (ranh giới với C2) |
| `docs/Document/document/API_SPEC.md` | Mục **11.12 Duyệt sản phẩm**, mục 15 (mã lỗi), 16 (enum) |
| `docs/Document/diagrams/ClassDiagram/ENTITY.md` | `## 2.10` `ProductModerationResult`; `## 2.2` `Product` (`status`, `submitted_at`, `moderator_id`, `moderation_reason`) |
| `docs/Document/diagrams/UseCaseDiagram/Document/Usecase.md` | UC-CAT-12 (luồng chính, 3a / 3b, ràng buộc không gửi dữ liệu cá nhân) |

### 0.1 Luật không được phá

1. **Không sửa `server-spring/src/main/resources/application*.yml`.** Cấu hình mới đọc thẳng biến môi trường bằng `@Value("${TÊN_BIẾN:mặc định}")` / `@ConditionalOnProperty(name = "TÊN_BIẾN")` — đúng mẫu `GHN_MODE` ở `shipping/fee/infra/HttpGhnFeeClient.java`. Khóa API đã có sẵn trong yml: `genie.gemini.api-key: ${GEMINI_API_KEY:}` → đọc bằng `@Value("${genie.gemini.api-key:}")`.
2. **Không thêm cột / bảng / migration.** Mọi thứ cần đã có: bảng `product_moderation_results` (V2), bảng khóa–giá trị `platform_settings` (V23), cột `products.submitted_at`, `moderator_id`, `moderation_reason` (V2 / V25). **Không thêm giá trị `AuditAction` mới** (sẽ phải sửa CHECK bằng migration).
3. **AI không bao giờ tự đăng sản phẩm lên sàn** (D13). `APPROVE` của AI chỉ giữ `PENDING_APPROVAL` cho người duyệt.
4. **Không gửi dữ liệu cá nhân sang Gemini** (N10): không tên / email / SĐT / địa chỉ chủ shop, không **nội dung** giấy tờ — chỉ tên tệp + loại giấy tờ.
5. **Không gọi Gemini trong transaction** và **không chặn người bán**: gửi duyệt trả về ngay; AI chạy ở `@Async @TransactionalEventListener(AFTER_COMMIT)` (N6).
6. Module: chỉ sửa trong `com.genie.catalog.*`, `com.genie.common.settings` (thêm 2 hàm), web `features/admin-moderation`. Không import lớp module khác ngoài `*Facade` / cổng đã có.
7. Comment code **tiếng Việt**, dòng đầu file ghi nguồn, ví dụ `// Sàng lọc AI sản phẩm — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.6)`.
8. Build backend bằng **JDK 21** (`export JAVA_HOME=$(/usr/libexec/java_home -v 21)`); JDK 24+ làm Lombok lỗi `TypeTag :: UNKNOWN`.

---

## 1. Hiện trạng code liên quan (đã có — dùng lại, KHÔNG viết lại)

| Thứ | Ở đâu | Ghi chú |
|---|---|---|
| Entity kết quả AI | `server-spring/src/main/java/com/genie/catalog/moderation/domain/ProductModerationResult.java` | Cột: `productId`, `modelName` (≤ 60), `decision` (`ModerationDecision`), `riskScore` (0–100), `violationsJson` (JSONB, chuỗi JSON mảng), `reasoning`, `rawResponse`, `fallbackToManual`, `createdAt`. Có sẵn `static fallback(productId, modelName, error, now)` và `isAutoReject(threshold)` |
| Enum quyết định | `catalog/moderation/domain/ModerationDecision.java` | `APPROVE, REJECT, NEED_REVIEW` |
| Repository kết quả | `catalog/moderation/domain/ProductModerationResultRepository.java` | `JpaRepository<ProductModerationResult, Long>` |
| Sự kiện gửi duyệt | `catalog/product/event/ProductSubmittedEvent.java` | `record ProductSubmittedEvent(long productId, long vendorUserId, OffsetDateTime submittedAt, boolean requeued)` — phát **trong** transaction lưu sản phẩm (`ProductWriteService.create/update`) |
| Phân công người duyệt | `catalog/moderation/application/ProductQueueAssigner.java` | `@EventListener` đồng bộ, gán `leastLoadedModerator()` lúc gửi. **Giữ nguyên** — AI chạy sau, chỉ đổi trạng thái |
| Sự kiện kết quả duyệt | `catalog/product/event/ProductModeratedEvent.java` | `(productId, vendorUserId, productName, decision, reasonCode, note)` |
| Email kết quả | `catalog/moderation/application/ProductModerationMailer.java` | Nghe `ProductModeratedEvent`; `REJECTED` → mẫu `PRODUCT_REJECTED`, nhãn lý do lấy `ModerationDecisionService.REJECT_REASONS.getOrDefault(reasonCode, reasonCode)` |
| Domain `Product` | `catalog/product/domain/Product.java` | Có `flag(String reason)` (→ `FLAGGED`), `getSubmittedAt()`, `isUnderReview()`; `findByIdForUpdate` ở `ProductRepository` |
| Hàng chờ / chi tiết duyệt | `catalog/moderation/application/ModerationQueueService.java` | Đã đọc kết quả AI mới nhất (`latestAiResult`) cho trang chi tiết (`ai_result`); `FLAGGED` đã xếp đầu tab Chờ duyệt; `rejected_by_ai = REJECTED && moderator_id IS NULL` |
| Cấu hình toàn sàn | `common/settings/PlatformSettings.java` + `PlatformSetting` + `PlatformSettingRepository` | Bảng `platform_settings(setting_key PK, value_json JSONB, updated_by_admin_id, updated_at)`. Mới có `decimal()` / `putDecimal()` — **thêm** `bool()` / `putBool()` (mục 4.1) |
| Ghi nhật ký | `common/audit/AuditLogger.record(AuditAction, Long userId, String referenceId, Map details, ClientInfo, boolean)` | Dùng `AuditAction.PRODUCT_MODERATED` |
| Đọc ảnh từ kho | `common/storage/ObjectStorage.get(bucket, key) → Optional<byte[]>` | Ảnh sản phẩm ở bucket logic **`product-media`**; `product_images.image_url` lưu **khóa** (vd `vendor/12/product/34/abc.jpg`, seed: `seed/product/x.jpg`) |
| Lọc HTML | jsoup đã có trong `pom.xml` (`catalog/product/application/HtmlSanitizer.java`) | Lấy chữ thuần: `Jsoup.parse(html).text()` |
| Bật `@Async` / lịch | `common/config/AsyncConfig.java` (`@EnableAsync`), `common/config/SchedulingConfig.java` (bật trừ khi `genie.scheduling.enabled=false`) | Job dùng `@Scheduled(cron = …, zone = "Asia/Ho_Chi_Minh")` như `promotion/coin/application/CoinExpiryJob.java` |
| Mẫu stub / http | `shipping/fee/infra/HttpGhnFeeClient.java` | `@ConditionalOnProperty(name = "GHN_MODE", havingValue = "http")` |
| Bean mặc định | `catalog/search/application/SearchPortsConfig.java` | `@Bean @ConditionalOnMissingBean(X.class)` |
| Web duyệt sản phẩm | `web/src/features/admin-moderation/` | Màn `ProductModerationPage.tsx` có 4 nút xem: Chờ duyệt / Đã duyệt / Không duyệt / **Cấu hình nhân viên** (`view === 'CONFIG'` → component `StaffConfig`). Trang chi tiết `ProductModerationDetail.tsx` đã hiện dòng Gemini từ `ai` (`decision`, `riskScore`, `violations`, `reasoning`) |
| Cờ mock web | `web/src/shared/config/env.ts` → `isMocked('product-moderation')` | `moderation.api.ts` dùng `const USE_MOCK = isMocked('product-moderation')` |
| Phiên web | `web/src/shared/auth/session.ts` → `$session` (nanostores), `SessionUser.roles: Role[]` (`'CUSTOMER' | 'VENDOR' | 'STAFF' | 'ADMIN'`) | Dùng để ẩn / khóa nút cho người không phải ADMIN |

---

## 2. Quyết định đã chốt cho P06 (thay thế phần chưa chốt của D11, D12)

| # | Nội dung |
|---|---|
| **A1 — Nút bật / tắt** | Lưu ở `platform_settings`, khóa **`moderation.ai_screening.enabled`**, giá trị JSON `true` / `false`. **Chưa có dòng = TẮT** (an toàn: không tốn tiền API khi chưa cấu hình). Chỉ người có **vai trò ADMIN** được đổi (`@PreAuthorize("hasRole('ADMIN')")`); nhân viên có `MANAGE_CATALOG` chỉ **xem** trạng thái |
| **A2 — Khi TẮT** | Listener và job quét **không gọi AI, không ghi kết quả**. Sản phẩm giữ `PENDING_APPROVAL`, vào hàng chờ thủ công như hiện nay. Bật lại không chấm lùi những sản phẩm gửi lúc đang tắt **quá 24 giờ** (giới hạn chi phí — mục 4.9) |
| **A3 — Chọn bản cài** | Biến môi trường **`GEMINI_MODE=stub|http`**, mặc định **`stub`** (không cần khóa, chạy được CI / máy dev). `http` = gọi Gemini thật, cần `GEMINI_API_KEY` |
| **A4 — Tham số** | `GEMINI_MODEL` (mặc định `gemini-3.8-flash` — đổi 2026-11-28: khóa mới tạo gọi `gemini-2.5-flash` bị Google trả 404 "no longer available to new users"), `GEMINI_REJECT_THRESHOLD` (mặc định `85`), `GEMINI_TIMEOUT_SECONDS` (mặc định `60` — đổi 2026-11-28, `gemini-3.8-flash` có ảnh thường quá 20 giây). Đọc bằng `@Value`, **không** thêm vào yml |
| **A5 — Thử lại** | Tối đa **3 lần gọi** cho một lần chấm; chờ **2 s** rồi **8 s** giữa các lần. Chỉ thử lại khi: HTTP 429, 5xx, timeout, lỗi mạng. HTTP 400 / 401 / 403 / 404 → **không** thử lại. Hết lượt hoặc JSON sai schema → kết quả **fallback** (A6) |
| **A6 — Fallback** | `ProductModerationResult.fallback(...)` (`NEED_REVIEW`, `risk 50`, `fallback_to_manual = true`, `raw_response` = thông điệp lỗi ≤ 2.000 ký tự). Sản phẩm **giữ `PENDING_APPROVAL`** (không `flag`) — lỗi kỹ thuật không phải dấu hiệu vi phạm; trang chi tiết vẫn thấy dòng "AI lỗi — duyệt thủ công" nhờ `fallback_to_manual` |
| **A7 — Áp kết quả (D13)** | `REJECT` **và** `risk ≥ ngưỡng` → **`REJECTED`** bởi AI (`moderator_id = NULL`, giao diện "AI Hệ thống"), gửi email cho shop. `REJECT` dưới ngưỡng **hoặc** `NEED_REVIEW` (không fallback) → **`FLAGGED`** (lên đầu hàng chờ, người duyệt đã gán giữ nguyên). `APPROVE` → **giữ `PENDING_APPROVAL`** |
| **A8 — Kết quả về muộn** | Chỉ áp khi sản phẩm **vẫn** `PENDING_APPROVAL` **và** `submitted_at` khớp lần gửi được chấm (so sánh đến **micro giây** — mục 4.7 bước 3). Không khớp → chỉ lưu dòng kết quả, không đổi trạng thái |
| **A9 — Ảnh** | Đã mở rộng 2026-11-28 — xem [`update_gemini.md`](./update_gemini.md) **B-D3, B-D4** (tối đa 8 ảnh gồm ảnh chung + ảnh phân loại + ảnh SKU + ảnh mô tả nội bộ, thu nhỏ cạnh dài ≤ 1024 px JPEG 0,85 qua `ScreeningImageResizer`, tổng thô ≤ 9 MB) |
| **A10 — Nhật ký** | Bật / tắt: `system_logs` `PRODUCT_MODERATED`, `reference_id = "ai_screening"`, `details = {action: "AI_SCREENING_ON" | "AI_SCREENING_OFF"}`. AI tự từ chối: `PRODUCT_MODERATED`, `userId = null`, `reference_id = <productId>`, `details = {action: "AI_REJECT", from_status, to_status, risk_score, model}` |
| **A11 — Thông báo** | Chỉ email cho shop khi **AI tự từ chối** (dùng lại `PRODUCT_REJECTED`). `FLAGGED` / `APPROVE` không báo shop (vẫn "Chờ duyệt"). Module `notification` (B6) **đã có** `NotificationFacade` + loại `PRODUCT_APPROVED / PRODUCT_REJECTED` nhưng **chưa ai** gửi in-app cho kết quả duyệt (cả duyệt tay) — P06 **không** tự nối; khi nối thì nối chung cho mọi `ProductModeratedEvent` (việc riêng, báo Thiện) |

---

## 3. Luồng tổng

```
Người bán bấm "Gửi duyệt"
  └─ ProductWriteService.create/update  (transaction)
       ├─ product.submit(now)  → PENDING_APPROVAL, submitted_at = now
       ├─ publish ProductSubmittedEvent(productId, vendorUserId, submittedAt, requeued)
       │     └─ ProductQueueAssigner (@EventListener, cùng transaction) → gán người duyệt ít việc nhất
       └─ COMMIT  → trả 200 cho người bán (không chờ AI)

AFTER_COMMIT, luồng @Async khác:
  ProductScreeningListener.onSubmitted(e)
    └─ ProductScreeningService.screen(productId, submittedAt)
         1. AiScreeningSettings.enabled() == false → return (A2)
         2. ScreeningRequestBuilder.build(productId)           [@Transactional(readOnly) — bean riêng]
               → null nếu sản phẩm không còn PENDING_APPROVAL → return
         3. client.screen(request)                              [KHÔNG transaction — gọi mạng, thử lại A5]
               → ScreeningVerdict  (hoặc ném ScreeningFailedException)
         4. ScreeningResultApplier.apply(productId, submittedAt, verdict | failure)   [@Transactional — bean riêng]
               ├─ SELECT … FOR UPDATE sản phẩm
               ├─ lưu ProductModerationResult (luôn luôn)
               ├─ còn PENDING_APPROVAL & submitted_at khớp?  không → dừng (A8)
               ├─ fallback → không đổi trạng thái (A6)
               ├─ REJECT & risk ≥ ngưỡng → product.rejectByAi(...) + audit + publish ProductModeratedEvent(REJECTED, "AI_AUTO", lý do)
               ├─ REJECT < ngưỡng | NEED_REVIEW → product.flag(tóm tắt)
               └─ APPROVE → không đổi
         (email gửi bởi ProductModerationMailer sau commit của bước 4)

Mỗi 5 phút: ProductScreeningSweepJob
  └─ bật? → lấy ≤ 20 sản phẩm PENDING_APPROVAL, submitted_at trong [now-24h, now-3m],
            chưa có kết quả AI tạo SAU submitted_at → screen(...) từng cái (mục 4.9)
```

---

## 4. Backend — từng bước (thứ tự làm đúng như dưới)

Gốc package: `server-spring/src/main/java/com/genie/`. Test: `server-spring/src/test/java/com/genie/`.

### 4.1 `common/settings/PlatformSettings.java` — thêm đọc / ghi boolean

Thêm 2 hàm cạnh `decimal()` / `putDecimal()` (cùng phong cách, khóa dòng khi ghi):

```java
/** Giá trị đúng / sai (JSON {@code true} / {@code false}); chưa có khóa → {@code fallback}. */
public boolean bool(String key, boolean fallback) {
    return settings.findById(key).map(s -> Boolean.parseBoolean(s.getValueJson().trim())).orElse(fallback);
}

/** Ghi đúng / sai (khóa dòng như {@link #putDecimal}); trả giá trị cũ ({@code null} nếu chưa có). */
@Transactional
public Boolean putBool(String key, boolean value, Long adminId) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    String json = Boolean.toString(value);
    return settings.findByKeyForUpdate(key).map(s -> {
        Boolean old = Boolean.parseBoolean(s.getValueJson().trim());
        s.change(json, adminId, now);
        return old;
    }).orElseGet(() -> {
        settings.save(PlatformSetting.builder().settingKey(key).valueJson(json).updatedByAdminId(adminId)
                .updatedAt(now).build());
        return null;
    });
}
```

Thêm hằng số: `public static final String AI_SCREENING_ENABLED = "moderation.ai_screening.enabled";`

Unit test `common/settings/PlatformSettingsTest` (nếu chưa có lớp test thì thêm vào IT ở 4.13 là đủ).

### 4.2 `catalog/moderation/application/AiScreeningSettings.java` (mới)

```java
// Bật / tắt kiểm duyệt tự động bằng Gemini — UC-CAT-12 (plan C1 P06; c1_auto_product.md A1, A10)
@Service
public class AiScreeningSettings {

    /** Trạng thái hiển thị cho màn Cấu hình (API 11.12 — GET /admin/product-moderation/ai-settings). */
    public record View(boolean enabled, String mode, String model, int rejectThreshold, boolean configured,
                       OffsetDateTime updatedAt, String updatedBy) { }

    private final PlatformSettings settings;
    private final PlatformSettingRepository repo;   // để đọc updated_at / updated_by_admin_id
    private final AuditLogger audit;
    private final String mode;          // @Value("${GEMINI_MODE:stub}")
    private final String model;         // @Value("${GEMINI_MODEL:gemini-2.5-flash}")
    private final int threshold;        // @Value("${GEMINI_REJECT_THRESHOLD:85}")
    private final String apiKey;        // @Value("${genie.gemini.api-key:}")

    public boolean enabled() { return settings.bool(PlatformSettings.AI_SCREENING_ENABLED, false); }
    public int rejectThreshold() { … }   // kẹp 1–100
    public String model() { … }          // "stub" khi mode = stub (để cột model_name ghi đúng)

    public View view() { … }             // configured = "stub".equals(mode) || !apiKey.isBlank()

    @Transactional
    public View setEnabled(long adminId, boolean on, ClientInfo client) {
        Boolean old = settings.putBool(PlatformSettings.AI_SCREENING_ENABLED, on, adminId);
        if (!Objects.equals(old, on)) {
            audit.record(AuditAction.PRODUCT_MODERATED, adminId, "ai_screening",
                    Map.of("action", on ? "AI_SCREENING_ON" : "AI_SCREENING_OFF"), client, true);
        }
        return view();
    }
}
```

- `updatedBy`: tên người đổi gần nhất — đọc `users.full_name` hoặc `admin_profiles.full_name` theo `updated_by_admin_id` bằng `JdbcTemplate` (read-model, giống các service catalog khác). Không có → `null`.
- Bật khi `configured = false` (mode `http` mà thiếu khóa) **vẫn cho bật** nhưng mọi lần chấm sẽ ra fallback; web hiện cảnh báo (mục 5.4).

### 4.3 Hợp đồng gọi AI — `catalog/moderation/application/`

Ba tệp mới:

```java
// ScreeningRequest.java — dữ liệu gửi AI (KHÔNG có dữ liệu cá nhân — N10)
public record ScreeningRequest(
        long productId,
        String name,
        String categoryPath,          // "Điện tử › Điện thoại"
        String brand,                 // có thể null
        String condition,             // NEW / USED / REFURBISHED
        long minPrice, long maxPrice, // VND, số nguyên
        String descriptionText,       // chữ thuần từ description_html, cắt 4.000 ký tự
        Map<String, String> specs,    // tên thông số → giá trị (bỏ khóa "__order")
        List<String> variantNames,    // tối đa 20, vd "512GB / Đỏ"
        List<String> documentNames,   // tên tệp giấy tờ, vd "giay-chung-nhan-hop-quy.pdf"
        List<Image> images) {         // tối đa 4
    public record Image(String mimeType, byte[] data) { }
}

// ScreeningVerdict.java — kết quả AI đã kiểm schema
public record ScreeningVerdict(ModerationDecision decision, int riskScore, List<String> violations,
                               String reasoning, String rawResponse, String modelName) { }

// ProductScreeningClient.java — cổng; 2 bản cài ở infra
public interface ProductScreeningClient {
    /** @throws ScreeningFailedException khi hết lượt thử / phản hồi sai schema / lỗi cấu hình */
    ScreeningVerdict screen(ScreeningRequest request);
}

// ScreeningFailedException.java — RuntimeException(String message, Throwable cause)
```

Bộ mã vi phạm hợp lệ (đúng chú thích ENTITY 2.10) — để ở `ScreeningVerdict`:

```java
public static final Set<String> VIOLATION_CODES = Set.of("COUNTERFEIT", "PROHIBITED_ITEM",
        "MISLEADING_DESCRIPTION", "BAD_IMAGE", "PRICE_ANOMALY", "WRONG_CATEGORY");
```

Mã lạ trong phản hồi → **bỏ đi** (không làm hỏng cả kết quả).

### 4.4 `catalog/moderation/infra/StubScreeningClient.java` (mới)

```java
@Component
@ConditionalOnProperty(name = "GEMINI_MODE", havingValue = "stub", matchIfMissing = true)
public class StubScreeningClient implements ProductScreeningClient { … }
```

Luật (so khớp **nguyên từ** trên `name + " " + descriptionText`, **giữ dấu** — chỉ chuẩn hóa NFC + chữ thường; mẫu `(?<![\p{L}\p{N}])<từ>(?![\p{L}\p{N}])`). **Không** bỏ dấu rồi tìm chuỗi con: sẽ bắt nhầm "thuộc" (→ thuoc), "bổ sung" (→ sung), "hàng giá" (→ hang gia) — lỗi đã gặp, sửa 2026-11-28:

| Chứa một trong | Kết quả |
|---|---|
| `hàng nhái`, `hang nhai`, `hàng giả`, `fake`, `replica`, `super fake` | `REJECT`, risk **90**, violations `["COUNTERFEIT"]`, reasoning "Mô tả có dấu hiệu hàng giả / hàng nhái." |
| `thuốc`, `vũ khí`, `súng`, `đạn dược` (chỉ bản có dấu) | `NEED_REVIEW`, risk **60**, `["PROHIBITED_ITEM"]`, "Có thể là hàng cấm / hạn chế kinh doanh — cần người duyệt kiểm tra." |
| `bán rẻ`, `giá sốc`, `ban re`, `gia soc` **và** `minPrice < 1000` | `REJECT`, risk **70**, `["PRICE_ANOMALY"]` (dưới ngưỡng 85 → `FLAGGED`) |
| còn lại | `APPROVE`, risk **10**, `[]`, "Không phát hiện vi phạm." |

`modelName = "stub"`, `rawResponse = null`. Thêm `catalog/moderation/infra/StubScreeningClientTest` (mỗi luật một ca, ca "Hàng Nhái" → REJECT, ca "thuộc / bổ sung / hàng giá tốt" → APPROVE, ca nguyên từ "fakeless" → APPROVE).

### 4.5 `catalog/moderation/infra/GeminiScreeningClient.java` (mới)

```java
@Component
@ConditionalOnProperty(name = "GEMINI_MODE", havingValue = "http")
public class GeminiScreeningClient implements ProductScreeningClient {
    // @Value("${genie.gemini.api-key:}") apiKey; @Value("${GEMINI_MODEL:gemini-2.5-flash}") model;
    // @Value("${GEMINI_TIMEOUT_SECONDS:20}") timeoutSeconds;
    // @Value("${GEMINI_BASE_URL:https://generativelanguage.googleapis.com}") baseUrl;  (để test trỏ máy chủ giả)
    // RestClient dựng trong constructor từ RestClient.Builder (Spring Boot tự cấp) — SimpleClientHttpRequestFactory
    //   setConnectTimeout(5 s), setReadTimeout(timeoutSeconds)
}
```

**Gọi:** `POST {baseUrl}/v1beta/models/{model}:generateContent`
Header: `x-goog-api-key: <apiKey>`, `Content-Type: application/json`. **Không** để khóa trong URL (tránh lộ ở log).
Thiếu khóa (`apiKey.isBlank()`) → ném `ScreeningFailedException("Thiếu GEMINI_API_KEY khi GEMINI_MODE=http")` ngay, không gọi.

**Thân request** (dựng bằng `ObjectMapper` / `Map`, KHÔNG nối chuỗi):

```json
{
  "systemInstruction": { "parts": [ { "text": "<SYSTEM_PROMPT — mục 4.5.1>" } ] },
  "contents": [ {
    "role": "user",
    "parts": [
      { "text": "<USER_PROMPT — mục 4.5.2>" },
      { "inline_data": { "mime_type": "image/jpeg", "data": "<base64>" } }
    ]
  } ],
  "generationConfig": {
    "temperature": 0,
    "responseMimeType": "application/json",
    "responseSchema": {
      "type": "OBJECT",
      "properties": {
        "decision":   { "type": "STRING", "enum": ["APPROVE", "REJECT", "NEED_REVIEW"] },
        "risk_score": { "type": "INTEGER" },
        "violations": { "type": "ARRAY", "items": { "type": "STRING",
                        "enum": ["COUNTERFEIT", "PROHIBITED_ITEM", "MISLEADING_DESCRIPTION",
                                 "BAD_IMAGE", "PRICE_ANOMALY", "WRONG_CATEGORY"] } },
        "reasoning":  { "type": "STRING" }
      },
      "required": ["decision", "risk_score", "violations", "reasoning"]
    }
  }
}
```

**Đọc phản hồi:** `candidates[0].content.parts[0].text` là **chuỗi JSON** → parse lần 2 bằng `ObjectMapper`.
Kiểm: `decision` thuộc enum; `risk_score` là số nguyên, **kẹp 0–100**; `violations` là mảng (lọc mã hợp lệ, bỏ trùng); `reasoning` chuỗi, cắt **1.000** ký tự. Thiếu `candidates` (bị chặn an toàn — `promptFeedback.blockReason`), parse lỗi, thiếu trường → `ScreeningFailedException("Phản hồi Gemini sai định dạng: …")`.
`rawResponse` = nguyên văn thân phản hồi HTTP, cắt **10.000** ký tự. `modelName` = `model`.

**Thử lại (A5):** vòng lặp tối đa 3 lần. Bắt `HttpClientErrorException.TooManyRequests`, `HttpServerErrorException`, `ResourceAccessException` (timeout / mạng) → chờ `2 s` rồi `8 s` (`Thread.sleep`, đặt thời gian chờ trong một trường `long[] backoffMillis = {2000, 8000}` để test truyền `{0, 0}` qua constructor phụ dành cho test). Các `HttpClientErrorException` khác → ném `ScreeningFailedException` ngay. Log `WARN` mỗi lần lỗi (không in khóa, không in base64 ảnh).

> **Cập nhật 2026-11-28 (`update_gemini.md` Phần B):** thân request bổ sung `safetySettings` (`BLOCK_NONE` 4 nhóm — B-D2), mỗi ảnh gắn nhãn chữ `"Ảnh i (…)"` đứng trước `inline_data` (B-D5), `SYSTEM_PROMPT` thêm mục "Kiểm tra ảnh (bắt buộc)", `parseVerdict` kiểm `SAFETY_BLOCKS` (`SAFETY`, `PROHIBITED_CONTENT`, `BLOCKLIST`, `IMAGE_SAFETY`) trước `candidates` rỗng → trả thẳng `REJECT 95 PROHIBITED_ITEM` không thử lại (B-D1).

#### 4.5.1 SYSTEM_PROMPT (hằng số `static final String`, giữ nguyên văn)

```
Bạn là bộ lọc kiểm duyệt tin đăng của sàn thương mại điện tử Genie (Việt Nam).
Nhiệm vụ: đánh giá MỘT tin đăng sản phẩm dựa trên tên, ngành hàng, thương hiệu, mô tả, thông số, phân loại,
khoảng giá, tên tệp giấy tờ và ảnh đính kèm. Chỉ đánh giá nội dung tin đăng, không suy đoán về người bán.

Các loại vi phạm (dùng đúng mã):
- COUNTERFEIT: hàng giả, hàng nhái, "replica", "fake", "super fake", logo / thương hiệu không khớp hàng chính hãng.
- PROHIBITED_ITEM: hàng cấm hoặc hạn chế kinh doanh tại Việt Nam (vũ khí, đạn dược, ma túy, thuốc kê đơn,
  động vật hoang dã, thuốc lá điện tử, tài liệu phản động, nội dung người lớn…).
- MISLEADING_DESCRIPTION: mô tả sai sự thật, cam kết chữa bệnh, thông tin mâu thuẫn với ảnh / thông số.
- BAD_IMAGE: ảnh mờ, không liên quan sản phẩm, chèn số điện thoại / mạng xã hội / chữ quảng cáo che sản phẩm.
- PRICE_ANOMALY: giá bất thường so với loại hàng (ví dụ điện thoại cao cấp giá vài chục nghìn đồng).
- WRONG_CATEGORY: sản phẩm không thuộc ngành hàng đã chọn.

Quyết định:
- APPROVE: không thấy vi phạm (risk_score 0–29).
- NEED_REVIEW: có dấu hiệu nhưng chưa chắc chắn, cần người kiểm tra (risk_score 30–84).
- REJECT: vi phạm rõ ràng (risk_score 60–100; chỉ đặt ≥ 85 khi bằng chứng rất rõ).
reasoning: 1–3 câu tiếng Việt, nêu bằng chứng cụ thể (trích từ/ảnh), không quá 600 ký tự.
Trả về đúng JSON theo schema, không thêm chữ nào khác.
```

#### 4.5.2 USER_PROMPT (dựng từ `ScreeningRequest`)

```
Tin đăng cần đánh giá:
- Tên: {name}
- Ngành hàng: {categoryPath}
- Thương hiệu: {brand | "không ghi"}
- Tình trạng: {Mới | Đã sử dụng | Tân trang}
- Khoảng giá: {minPrice} – {maxPrice} VND
- Phân loại: {variantNames nối "; " | "không có"}
- Thông số:
  • {tên}: {giá trị}          (mỗi dòng một thông số, tối đa 30 dòng, giá trị cắt 200 ký tự)
- Giấy tờ đính kèm (chỉ tên tệp): {documentNames nối ", " | "không có"}
- Mô tả:
"""
{descriptionText}
"""
Ảnh đính kèm: {số ảnh} ảnh (theo thứ tự ảnh bìa trước).
```

**Kiểm N10 trong test:** prompt không chứa email / số điện thoại của chủ shop (unit test dựng request có mô tả bình thường, khẳng định chuỗi prompt không chứa `@` của email chủ shop dựng trong test và không chứa tên shop).

`GeminiScreeningClientTest` (unit, `MockRestServiceServer.bindTo(RestClient.Builder)` — xem mẫu `shipping/fee/infra/HttpGhnFeeClientTest.java`):
1. Phản hồi đúng schema → `ScreeningVerdict` đúng (`decision`, `risk` kẹp, mã lạ bị bỏ).
2. `text` không phải JSON → `ScreeningFailedException`.
3. 429 hai lần rồi 200 → thành công sau 3 lần gọi (backoff `{0, 0}`).
4. 500 ba lần → `ScreeningFailedException`, đúng 3 lần gọi.
5. 400 → ném ngay, 1 lần gọi.
6. Request có header `x-goog-api-key`, URL không chứa khóa, thân có `responseSchema` và `inline_data` khi có ảnh.

### 4.6 `catalog/moderation/application/ScreeningRequestBuilder.java` (mới, `@Component`)

```java
@Transactional(readOnly = true)
public ScreeningRequest build(long productId) { … }   // null nếu không còn PENDING_APPROVAL
```

Đọc bằng `NamedParameterJdbcTemplate` (projection — **không** nạp entity `Product` vì `category` EAGER):

| Trường | Nguồn |
|---|---|
| `name`, `brand`, `item_condition`, `description_html`, `specs_json`, `category_id`, `status` | `products` |
| `categoryPath` | `CategoryCatalog.index().ancestors(categoryId)` + chính nó, nối `" › "` (xem cách `ModerationQueueService` dựng `categoryPath` cho trang chi tiết — dùng lại hàm nếu có) |
| `minPrice`, `maxPrice` | `SELECT min(price), max(price) FROM product_variants WHERE product_id = :p AND deleted_at IS NULL` → `longValue()` |
| `variantNames` | `variant_name` của biến thể còn hiệu lực, `ORDER BY id`, tối đa 20 |
| `specs` | `SpecCodec.parse(...)` (đã bỏ `__order` — xem `SpecCodec.ORDER_KEY`) hoặc tự parse JSON và bỏ khóa `__order` |
| `documentNames` | `product_documents.file_name` (nếu cột tên khác — đọc ENTITY 2.8 / V2) |
| `images` | `product_images` với `variant_id IS NULL`, `ORDER BY is_primary DESC NULLS LAST, display_order, id LIMIT 4` → `ObjectStorage.get("product-media", key)` (A9). Không đọc được → bỏ ảnh, ghi `DEBUG` |
| `descriptionText` | `Jsoup.parse(html).text()`, gộp khoảng trắng, cắt 4.000 ký tự |

**Tuyệt đối không** SELECT `users.*`, `vendor_profiles.*` vào request.

### 4.7 `catalog/moderation/application/ScreeningResultApplier.java` (mới, `@Component`)

```java
@Transactional
public void apply(long productId, OffsetDateTime submittedAt, ScreeningVerdict verdict) { … }
@Transactional
public void applyFailure(long productId, OffsetDateTime submittedAt, String modelName, String error) { … }
```

Các bước của `apply`:

1. `Product p = products.findByIdForUpdate(productId).orElse(null)`; `null` → return.
2. Lưu `ProductModerationResult` (builder): `productId`, `modelName`, `decision`, `riskScore`, `violationsJson = objectMapper.writeValueAsString(violations)`, `reasoning`, `rawResponse`, `fallbackToManual = false`. Lưu **trước** khi kiểm điều kiện (A8 — luôn giữ lịch sử).
3. Kiểm còn hiệu lực:
   ```java
   boolean current = p.getStatus() == ProductStatus.PENDING_APPROVAL
           && p.getSubmittedAt() != null
           && p.getSubmittedAt().toInstant().truncatedTo(ChronoUnit.MICROS)
                .equals(submittedAt.toInstant().truncatedTo(ChronoUnit.MICROS));
   if (!current) return;
   ```
   (PostgreSQL `TIMESTAMPTZ` chỉ giữ **micro giây**; `OffsetDateTime.now()` trong sự kiện có nano giây → so sánh thẳng sẽ luôn sai.)
4. Áp A7:
   - `verdict.decision() == REJECT && verdict.riskScore() >= settings.rejectThreshold()`:
     - `p.rejectByAi(reasonText)` (4.10). `reasonText = "Không đạt kiểm duyệt tự động: " + reasoning` (cắt tổng 500 ký tự).
     - `audit.record(AuditAction.PRODUCT_MODERATED, null, String.valueOf(productId), Map.of("action", "AI_REJECT", "from_status", "PENDING_APPROVAL", "to_status", "REJECTED", "risk_score", risk, "model", modelName), null, true)` — kiểm `AuditLogger.record` chấp nhận `userId = null` (hàm `resolve(null)`); nếu không → dùng `AuditActor` hệ thống có sẵn (đọc `common/audit/AuditActor`).
     - `events.publishEvent(new ProductModeratedEvent(p.getId(), p.getVendorId(), p.getName(), "REJECTED", "AI_AUTO", reasoning))`.
   - `REJECT` dưới ngưỡng hoặc `NEED_REVIEW` → `p.flag(tóm tắt)` với `tóm tắt = "AI nghi ngờ: " + nhãn vi phạm đầu tiên` (nhãn tiếng Việt: COUNTERFEIT "hàng giả / nhái", PROHIBITED_ITEM "hàng cấm / hạn chế", MISLEADING_DESCRIPTION "mô tả sai / gây hiểu lầm", BAD_IMAGE "ảnh không đạt", PRICE_ANOMALY "giá bất thường", WRONG_CATEGORY "sai ngành hàng"; rỗng → "cần kiểm tra"). **Không** đổi `moderator_id`.
   - `APPROVE` → không làm gì.
5. `products.save(p)`.

`applyFailure`: bước 1–3 giống trên nhưng lưu `ProductModerationResult.fallback(productId, modelName, error(cắt 2.000), now)`; **không** đổi trạng thái (A6).

Không cần xóa cache storefront (sản phẩm chờ duyệt không có trên sàn).

### 4.8 `catalog/moderation/application/ProductScreeningService.java` + `ProductScreeningListener.java` (mới)

```java
@Service
public class ProductScreeningService {      // KHÔNG @Transactional ở lớp này (gọi mạng ở giữa)
    public void screen(long productId, OffsetDateTime submittedAt) {
        if (!settings.enabled()) return;                                  // A2
        ScreeningRequest req = builder.build(productId);
        if (req == null) return;
        try {
            ScreeningVerdict v = client.screen(req);
            applier.apply(productId, submittedAt, v);
        } catch (ScreeningFailedException e) {
            log.warn("Sàng lọc AI sản phẩm #{} lỗi: {}", productId, e.getMessage());
            applier.applyFailure(productId, submittedAt, settings.model(), e.getMessage());
        } catch (RuntimeException e) {                                     // lỗi không lường → vẫn chuyển thủ công
            log.error("Sàng lọc AI sản phẩm #{} lỗi bất ngờ", productId, e);
            applier.applyFailure(productId, submittedAt, settings.model(), e.getClass().getSimpleName());
        }
    }
}

@Component
public class ProductScreeningListener {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(ProductSubmittedEvent e) {
        screening.screen(e.productId(), e.submittedAt());
    }
}
```

**Vì sao 3 bean riêng** (`ScreeningRequestBuilder`, `ScreeningResultApplier`, `ProductScreeningService`): gọi hàm `@Transactional` trong **cùng** lớp không qua proxy Spring → transaction không mở. Không gộp.

Rollback lưu sản phẩm → sự kiện `AFTER_COMMIT` không chạy → không gọi AI (có test).

### 4.9 `catalog/moderation/application/ProductScreeningSweepJob.java` (mới)

```java
@Component
public class ProductScreeningSweepJob {
    static final int BATCH = 20;

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void sweep() {
        if (!settings.enabled()) return;
        // KHÔNG gọi Gemini trên luồng lịch: Spring mặc định chỉ 1 luồng @Scheduled → 20 sản phẩm × ~70 s chặn mọi job khác.
        // Giao từng sản phẩm cho ProductScreeningListener.screenLater (@Async) — sửa 2026-11-28.
        for (Candidate c : claim()) listener.screenLater(c.productId(), c.submittedAt());
    }
}
```

`claim()` (`@Transactional` ở **bean khác** hoặc dùng `TransactionTemplate` — cùng lý do 4.8):

```sql
SELECT p.id, p.submitted_at FROM products p
 WHERE p.status = 'PENDING_APPROVAL'
   AND p.submitted_at BETWEEN now() - interval '24 hours' AND now() - interval '3 minutes'
   AND NOT EXISTS (SELECT 1 FROM product_moderation_results r
                    WHERE r.product_id = p.id AND r.created_at >= p.submitted_at)
 ORDER BY p.submitted_at
 LIMIT 20
 FOR UPDATE OF p SKIP LOCKED
```

(Khóa chỉ giữ trong transaction của `claim()` — đủ để hai instance không lấy cùng lô. Chấp nhận trường hợp hiếm listener và job cùng chấm một sản phẩm: kết quả thứ hai chỉ thêm một dòng lịch sử, trạng thái áp theo A8 vẫn đúng.)

Khối văn bản `"""` cắt khoảng trắng đầu dòng: nếu nối thêm điều kiện thì **thêm dấu cách** trước `AND` (bẫy đã gặp: `WHERE TRUEAND`).

### 4.10 `catalog/product/domain/Product.java` — thêm 1 phương thức

```java
/** AI tự từ chối (REJECT, rủi ro ≥ ngưỡng) — không có người duyệt; giao diện hiện "AI Hệ thống" (UC-CAT-12; D13) */
public void rejectByAi(String reason) {
    this.status = ProductStatus.REJECTED;
    this.isPublished = false;
    this.moderatorId = null;
    this.moderationReason = reason;
    this.isRequeued = false;
}
```

Không thêm cột. Sửa ENTITY.md `## 2.2` phần phương thức: thêm dòng `rejectByAi(reason)`.

### 4.11 `catalog/moderation/application/ProductModerationMailer.java` — lý do AI

Khi `decision = "REJECTED"` và `reasonCode = "AI_AUTO"`: nhãn = **"Không đạt kiểm duyệt tự động"**, ghi chú = `note` (lý do AI). Hiện code dùng `REJECT_REASONS.getOrDefault(reasonCode, reasonCode)` → sẽ in ra chữ `AI_AUTO`; sửa thành nhánh riêng. Không thêm mẫu email mới.

### 4.12 API cấu hình — `catalog/moderation/api/AdminProductModerationController.java`

Controller hiện có `@RequestMapping("/api/v1/admin")` + `@PreAuthorize("hasAuthority('MANAGE_CATALOG')")` ở lớp. Thêm:

```java
public record AiSettingsRequest(Boolean enabled) { }

@Operation(summary = "UC-CAT-12 — Trạng thái kiểm duyệt tự động bằng AI")
@GetMapping("/product-moderation/ai-settings")
public ResponseEntity<ApiResponse<AiScreeningSettings.View>> aiSettings() {
    return responses.ok(aiSettings.view());
}

@Operation(summary = "UC-CAT-12 — Bật / tắt kiểm duyệt tự động bằng AI (chỉ ADMIN)")
@PutMapping("/product-moderation/ai-settings")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<AiScreeningSettings.View>> setAiSettings(@CurrentUser AuthUser me,
        @RequestBody AiSettingsRequest body, ClientInfo client) {
    if (body == null || body.enabled() == null) {
        throw InvalidRequestException.of("enabled", "REQUIRED", null, "Thiếu trạng thái bật / tắt.");
    }
    return responses.ok(aiSettings.setEnabled(me.id(), body.enabled(), client));
}
```

- `@PreAuthorize` ở hàm **cộng dồn** với điều kiện ở lớp? → **Không**, Spring dùng chú thích gần nhất (hàm ghi đè lớp). ADMIN đã có đủ 13 quyền nên vẫn qua; STAFF có `MANAGE_CATALOG` nhưng không phải ADMIN → `403`. Có test (4.13).
- JSON trả (snake_case tự động):

```json
{ "enabled": true, "mode": "http", "model": "gemini-2.5-flash", "reject_threshold": 85,
  "configured": true, "updated_at": "2026-11-27T18:10:00+07:00", "updated_by": "Đặng Hải Yến" }
```

### 4.13 Test tích hợp — `catalog/moderation/ProductScreeningIT.java` (mới)

Kế thừa `IntegrationTestBase`. Thay client AI bằng bean giả **theo đúng mẫu KYC đã có** (không dùng `@MockBean` — mỗi `@MockBean` dựng lại Spring context + Testcontainers, rất chậm):

- Tạo `src/test/java/com/genie/support/FakeProductScreeningClient.java` (`implements ProductScreeningClient`): giữ danh sách `ScreeningRequest` đã nhận (`CopyOnWriteArrayList`), một `volatile ScreeningVerdict next` (mặc định `APPROVE 10`), cờ `failNext`, hàm `willReturn(verdict)`, `willFail()`, `requests()`, `clear()`.
- Đăng ký trong lớp `@TestConfiguration` ở cuối `IntegrationTestBase.java` (cạnh bean `fakeKycVerificationClient()` dòng ~276): `@Bean @Primary FakeProductScreeningClient fakeProductScreeningClient()`; thêm trường `@Autowired protected FakeProductScreeningClient aiScreening;` (cạnh `aiWorker` dòng ~145) và gọi `aiScreening.clear()` trong hàm dọn dẹp chung nếu có.
- "client không được gọi" = `assertThat(aiScreening.requests()).isEmpty()`.

Đồng bộ hóa: listener `@Async` → chờ bằng vòng lặp (xem hàm `await(BooleanSupplier)` trong `ProductModerationIT`), tối đa 10 s. Dựng sản phẩm gửi duyệt **qua API** `POST /api/v1/seller/products` với `submit = true` (xem `ProductModerationIT.submitViaApi()` / `SellerProductIT.fullBody`). Bật tính năng trong `@BeforeEach`: `jdbc.update("INSERT INTO platform_settings … ON CONFLICT (setting_key) DO UPDATE SET value_json = 'true'")`; `@AfterEach` đặt lại `false` (CSDL test dùng chung).

| # | Ca | Khẳng định |
|---|---|---|
| 1 | TẮT → gửi duyệt | `aiScreening.requests()` rỗng (AI không được gọi), không có dòng `product_moderation_results`, trạng thái `PENDING_APPROVAL` |
| 2 | `APPROVE 10` | có 1 dòng kết quả, trạng thái vẫn `PENDING_APPROVAL` |
| 3 | `NEED_REVIEW 60` | `FLAGGED`, `moderation_reason` bắt đầu "AI nghi ngờ", `moderator_id` không đổi; tab `PENDING` của `GET /admin/product-moderation` có sản phẩm ở **đầu** |
| 4 | `REJECT 90` (≥ 85) | `REJECTED`, `moderator_id IS NULL`, `rejected_by_ai = true` ở hàng chờ tab `REJECTED`; có `system_logs` `details.action = AI_REJECT`; email `product-rejected` gửi tới chủ shop (xem cách `ProductModerationIT` đọc `mails`) |
| 5 | `REJECT 70` (< 85) | `FLAGGED` |
| 6 | `aiScreening.willFail()` (ném `ScreeningFailedException`) | 1 dòng `fallback_to_manual = true`, trạng thái **giữ** `PENDING_APPROVAL` |
| 7 | Kết quả về muộn | Gọi thẳng `applier.apply(id, submittedAtCũ, REJECT 95)` sau khi đổi `submitted_at` trong CSDL → chỉ thêm dòng kết quả, trạng thái không đổi |
| 8 | Job quét | Đặt `submitted_at = now() - 10 phút`, không có kết quả → gọi `sweepJob.sweep()` → có kết quả. Sản phẩm `submitted_at = now() - 2 ngày` → **không** chấm |
| 9 | API cấu hình | `GET …/ai-settings` bằng STAFF có `MANAGE_CATALOG` → 200; `PUT` bằng STAFF đó → **403**; `PUT {enabled:false}` bằng ADMIN → 200 + `system_logs` `AI_SCREENING_OFF`; `PUT {}` → 422 |
| 10 | Rollback | Gửi duyệt bị lỗi (vd thiếu thông số bắt buộc → 422) → `aiScreening.requests()` rỗng |

Kết quả AI cho từng ca đặt bằng `aiScreening.willReturn(new ScreeningVerdict(...))` trước khi gửi duyệt.

Unit test: `StubScreeningClientTest` (4.4), `GeminiScreeningClientTest` (4.5), `ScreeningResultApplierTest` **không bắt buộc** (IT đã phủ).

Chạy:

```bash
cd server-spring
./mvnw clean verify -Dit.test='ProductScreeningIT,ProductModerationIT,SellerProductIT' -Dsurefire.failIfNoSpecifiedTests=false
```

`ModuleBoundaryTest` (ArchUnit) phải xanh — không import lớp của module khác.

---

## 5. Web — `web/src/features/admin-moderation/`

Thứ tự (CLAUDE.md mục 5): types → dto → mock → mapper → api → application → ui → i18n.

### 5.1 Domain — `domain/moderation.types.ts`

```ts
export interface AiScreeningSettings {
  enabled: boolean;
  mode: 'stub' | 'http';
  model: string;
  rejectThreshold: number;
  configured: boolean;
  updatedAt: string | null;
  updatedBy: string | null;
}
```

### 5.2 Infrastructure

- `moderation.dto.ts`: `AiSettingsDto` snake_case đúng JSON mục 4.12.
- `moderation.mock.ts`: `export const aiSettingsStore = mockState<AiSettingsDto>('admin.ai_settings', () => ({ enabled: false, mode: 'stub', model: 'stub', reject_threshold: 85, configured: true, updated_at: null, updated_by: null }));` — chữ ký thật `mockState<T>(name: string, init: () => T)` ở `shared/api/mock.ts` (tham số thứ hai là **hàm**).
- Feature này **chưa có** `moderation.mapper.ts` — các hàm ánh xạ (`toModerator`…) đang nằm ngay trong `moderation.api.ts`. Đặt `toAiSettings(d)` cạnh `toModerator` (không tạo tệp mapper riêng, giữ cách tổ chức hiện có).
- `moderation.api.ts` (cờ `USE_MOCK = isMocked('product-moderation')` đã có):

```ts
export async function fetchAiSettings(): Promise<AiScreeningSettings> {
  if (USE_MOCK) { await mockDelay(150); return toAiSettings(aiSettingsStore.get()); }
  return toAiSettings(await api.get<AiSettingsDto>('/admin/product-moderation/ai-settings'));
}

export async function saveAiSettings(enabled: boolean): Promise<AiScreeningSettings> {
  if (USE_MOCK) {
    await mockDelay(200);
    aiSettingsStore.set({ ...aiSettingsStore.get(), enabled, updated_at: new Date().toISOString(), updated_by: 'Admin' });
    return toAiSettings(aiSettingsStore.get());
  }
  return toAiSettings(await api.put<AiSettingsDto>('/admin/product-moderation/ai-settings', { enabled }));
}
```

### 5.3 Application — `application/useModeration.ts`

```ts
export const useAiSettings = () => useQuery({ queryKey: ['admin', 'ai-settings'], queryFn: fetchAiSettings });
export const useSaveAiSettings = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: saveAiSettings,
    onSuccess: (data) => qc.setQueryData(['admin', 'ai-settings'], data),
  });
};
```

### 5.4 UI — thẻ "Kiểm duyệt tự động bằng AI"

Tệp mới `ui/AiScreeningCard.tsx`, đặt **trên cùng** của view `CONFIG` (trong `ProductModerationPage.tsx`: `view === 'CONFIG' ? (<><AiScreeningCard /><StaffConfig /></>) : …`, cách nhau `gap-4`).

Nội dung thẻ (Tailwind theo token, không màu cứng; kiểm tương phản ở **9 theme**):

```
┌──────────────────────────────────────────────────────────────────────────┐
│ [icon sparkles]  Kiểm duyệt tự động bằng AI                [ công tắc ]  │
│ Sản phẩm gửi duyệt được Gemini chấm trước: vi phạm rõ (rủi ro ≥ 85) bị    │
│ từ chối tự động, nghi ngờ được đưa lên đầu hàng chờ. AI không tự đăng bán.│
│ Mô hình: gemini-2.5-flash · Chế độ: Thật / Giả lập · Ngưỡng từ chối: 85   │
│ Cập nhật lần cuối: 27/11/2026 18:10 bởi Đặng Hải Yến                      │
│ ⚠ (khi configured = false) Chưa cấu hình GEMINI_API_KEY — mọi lần chấm   │
│    sẽ chuyển duyệt thủ công.                                              │
│ ⓘ (khi không phải ADMIN) Chỉ Quản trị viên được bật / tắt.               │
└──────────────────────────────────────────────────────────────────────────┘
```

- Công tắc: dùng `Switch` có sẵn (`shared/ui/Choice.tsx`, xuất ở `shared/ui/index.ts`) + `ConfirmDialog` (`shared/ui/Modal`). (Bản rà 2026-11-28 ghi nhầm là không có — đã sửa.) Màu cảnh báo dùng `text-amber-600` (dự án **không có** token `text-warning`). **Không** thêm thư viện.
- Là ADMIN khi `$session.get()` là `authenticated` và `user.roles.includes('ADMIN')` (dùng `useStore($session)` từ `@nanostores/react`). Không phải ADMIN → công tắc `disabled` + dòng ⓘ.
- Bấm bật / tắt: mở `Modal` xác nhận (bật: "Bật kiểm duyệt tự động? Mỗi sản phẩm gửi duyệt sẽ được gửi sang Gemini (tốn phí API)."; tắt: "Tắt kiểm duyệt tự động? Sản phẩm gửi duyệt sẽ vào thẳng hàng chờ thủ công."), xác nhận → `save.mutate(!enabled)`; thành công → `toast.success`; lỗi → `toast.error(errorMessage(e, t))`.
- Ngày giờ qua `shared/lib/format.ts`. Icon qua `<Icon name="shield-check">` — bộ icon (`shared/ui/icon/icons.ts`) **không có** `sparkles` / `bot`; không thêm icon mới.

### 5.5 i18n — thêm **cả** `web/src/i18n/vi.json` và `en.json` (check-i18n chặn nếu lệch)

Khóa dưới `admin-moderation.product.ai_settings.*`:

| Khóa | vi | en |
|---|---|---|
| `title` | Kiểm duyệt tự động bằng AI | Automatic AI moderation |
| `desc` | Sản phẩm gửi duyệt được Gemini chấm trước: vi phạm rõ (rủi ro ≥ {threshold}) bị từ chối tự động, nghi ngờ được đưa lên đầu hàng chờ. AI không tự đăng bán. | Submitted products are screened by Gemini first: clear violations (risk ≥ {threshold}) are rejected automatically, suspicious ones go to the top of the queue. AI never publishes products. |
| `meta` | Mô hình: {model} · Chế độ: {mode} · Ngưỡng từ chối: {threshold} | Model: {model} · Mode: {mode} · Reject threshold: {threshold} |
| `mode.http` | Thật | Live |
| `mode.stub` | Giả lập | Simulated |
| `updated` | Cập nhật lần cuối: {time} bởi {name} | Last changed {time} by {name} |
| `not_configured` | Chưa cấu hình GEMINI_API_KEY — mọi lần chấm sẽ chuyển duyệt thủ công. | GEMINI_API_KEY is not set — every screening falls back to manual review. |
| `admin_only` | Chỉ Quản trị viên được bật / tắt. | Only administrators can turn this on or off. |
| `toggle` | Bật / tắt kiểm duyệt tự động | Turn automatic moderation on or off |
| `confirm_on` | Bật kiểm duyệt tự động? Mỗi sản phẩm gửi duyệt sẽ được gửi sang Gemini (tốn phí API). | Turn on automatic moderation? Every submitted product will be sent to Gemini (API costs apply). |
| `confirm_off` | Tắt kiểm duyệt tự động? Sản phẩm gửi duyệt sẽ vào thẳng hàng chờ thủ công. | Turn off automatic moderation? Submitted products will go straight to the manual queue. |
| `saved_on` | Đã bật kiểm duyệt tự động. | Automatic moderation is on. |
| `saved_off` | Đã tắt kiểm duyệt tự động. | Automatic moderation is off. |

Kiểm: `cd web && node ../scripts/check-i18n.mjs`.

### 5.6 Kiểm tra web

```bash
cd web && npm run verify     # lint → astro check + tsc → vitest → build → check-i18n (CỔNG BẮT BUỘC)
```

Thêm 1 test vitest cho mapper `toAiSettings` (snake_case → camelCase, `updated_at: null`).

---

## 6. Cấu hình & tài liệu

| Tệp | Sửa |
|---|---|
| `.env.example` (gốc repo) | Cạnh dòng `#GEMINI_API_KEY=` thêm (để comment): `#GEMINI_MODE=stub` (stub \| http), `#GEMINI_MODEL=gemini-2.5-flash`, `#GEMINI_REJECT_THRESHOLD=85`, `#GEMINI_TIMEOUT_SECONDS=20` kèm 1 dòng giải thích |
| `docs/cau-hinh-moi-truong.md` | Bảng biến: thêm 4 biến trên, ý nghĩa, mặc định; ghi "bật / tắt tính năng ở Admin › Duyệt sản phẩm › Cấu hình (khóa `moderation.ai_screening.enabled`)" |
| `docs/Document/document/API_SPEC.md` mục 11.12 | Thêm `GET/PUT /admin/product-moderation/ai-settings` (JSON mục 4.12; PUT chỉ ADMIN → 403); ghi luồng A7 / A8 và `reason_code = "AI_AUTO"` trong sự kiện; bảng mục 14 (màn → API) dòng "Duyệt sản phẩm" thêm endpoint mới |
| `docs/Document/diagrams/ClassDiagram/ENTITY.md` | `## 2.2` thêm phương thức `rejectByAi(reason)`; `## 2.10` ghi `model_name = "stub"` khi giả lập |
| `plan/2026-11-26/c1.md` | D11, D12 → `☑ 2026-11-27 (theo c1_auto_product.md A1–A11)`; P01 (phần cấu hình Gemini) + **P06** → ☑; tick P06.01–P06.05; một dòng **Nhật ký** (ngày, việc, mã commit) |
| `docs/Document/CHECKLIST_TIEN_DO_UC.md` | UC-CAT-12: BE ☑, Test ☑ (khi IT xanh), Chạy ☐ (chủ dự án tự chạy); Trạng thái 🟡; cập nhật mục 1 Tổng quan + bảng phân hệ 02 (đếm lại từ các dòng UC); một dòng mục 5 Nhật ký — theo *Quy trình cập nhật* ở mục 0 của file |
| `Usecase.md` UC-CAT-12 | Câu *Hiện trạng* khớp checklist |

---

## 7. Kịch bản chạy thật (chủ dự án tự làm — lớp **Chạy**)

1. `.env`: `GEMINI_MODE=stub` (hoặc `http` + `GEMINI_API_KEY=<khóa thật>`). Khởi động lại backend (JDK 21) và web.
2. Đăng nhập Admin (`/admin`) → Duyệt sản phẩm → **Cấu hình** → thẻ "Kiểm duyệt tự động bằng AI" đang **tắt** → bật → xác nhận → thấy toast + "Cập nhật lần cuối".
3. Đăng nhập nhân viên chỉ có `MANAGE_CATALOG` (không phải ADMIN) → thẻ hiện nhưng công tắc mờ + "Chỉ Quản trị viên…".
4. Đăng nhập người bán → gửi duyệt 3 sản phẩm:
   - tên "Tai nghe Bluetooth chính hãng" → sau vài giây Admin thấy vẫn "Chờ duyệt", trang chi tiết có dòng Gemini `APPROVE`.
   - mô tả chứa "hàng nhái" → shop thấy "Bị từ chối", lý do "Không đạt kiểm duyệt tự động: …", có email (Mailpit `http://localhost:8025`); Admin tab Không duyệt, người duyệt "AI Hệ thống".
   - mô tả chứa "thuốc" → Admin tab Chờ duyệt, nhãn "AI nghi ngờ", nằm **đầu** danh sách.
5. Tắt tính năng → gửi duyệt sản phẩm mới → không có dòng Gemini, vẫn "Chờ duyệt".
6. Admin duyệt tay sản phẩm "AI nghi ngờ" → lên sàn bình thường.

---

## 8. Commit (chỉ khi chủ dự án yêu cầu — CLAUDE.md mục 4)

Mỗi commit build được, test xanh; `git add` đúng từng tệp (không `git add .`); tiêu đề + thân **tiếng Anh**, chữ thường, ≤ 100 ký tự; **không** `Co-Authored-By` / dấu vết AI; backend và web **không chung** một commit.

| # | Commit | Tệp |
|---|---|---|
| 1 | `feat(common): add boolean values to platform settings` | `PlatformSettings.java` |
| 2 | `feat(catalog): add ai screening port with stub client` | `ScreeningRequest`, `ScreeningVerdict`, `ProductScreeningClient`, `ScreeningFailedException`, `StubScreeningClient` + test |
| 3 | `feat(catalog): call gemini to screen submitted products` | `GeminiScreeningClient` + test |
| 4 | `feat(catalog): apply ai screening results to the moderation queue` | `ScreeningRequestBuilder`, `ScreeningResultApplier`, `ProductScreeningService`, `ProductScreeningListener`, `ProductScreeningSweepJob`, `Product.rejectByAi`, `ProductModerationMailer` |
| 5 | `feat(catalog): let admins turn automatic ai moderation on or off` | `AiScreeningSettings`, `AdminProductModerationController` |
| 6 | `test(catalog): cover ai screening flow and settings` | `ProductScreeningIT` |
| 7 | `feat(admin-ui): add automatic ai moderation switch` | web types / dto / mock / mapper / api / hook / `AiScreeningCard.tsx` / `ProductModerationPage.tsx` / test |
| 8 | `feat(web): add translation keys for ai moderation settings` | `vi.json`, `en.json` — **đặt trước commit 7** để commit 7 build được |
| 9 | `docs(catalog): document ai screening and update c1 progress` | `.env.example`, `cau-hinh-moi-truong.md`, `API_SPEC.md`, `ENTITY.md`, `c1.md`, `CHECKLIST_TIEN_DO_UC.md`, `Usecase.md` |

PR vào **`develop`** theo `.github/pull_request_template.md`, tiêu đề đúng chuẩn commit, mục "Ảnh hưởng CSDL" = **Không có** (không migration).

---

## 9. Bẫy đã biết (đọc lại trước khi nộp)

1. `submitted_at` so sánh phải cắt về **micro giây** (mục 4.7 bước 3) — sai là mọi kết quả AI bị coi "về muộn".
2. Hàm `@Transactional` gọi từ **cùng** lớp không mở transaction → tách bean (4.8).
3. Khối `"""` Java cắt khoảng trắng đầu dòng → nối chuỗi SQL phải tự thêm dấu cách.
4. `Product.category` là `@ManyToOne` **EAGER** → đọc danh sách bằng projection / JDBC, không `findAll`.
5. `shop_id` trên API = `vendor_profiles.id`; `products.vendor_id` = `users.id` chủ shop — không trộn.
6. Test dùng chung CSDL: lọc theo id của test, đặt lại `platform_settings` sau test, đừng khẳng định đúng cả danh sách hàng chờ.
7. Biến `GEMINI_MODE` không có trong yml — `@ConditionalOnProperty(name = "GEMINI_MODE", …)` đọc thẳng biến môi trường; **phải** có `matchIfMissing = true` ở bản stub, nếu không thiếu biến sẽ không có bean nào → app không khởi động.
8. Không log `GEMINI_API_KEY`, không log base64 ảnh, không log `raw_response` ở mức `INFO`.
9. Web: không `any`; chuỗi qua `t()`; thêm khóa ở **cả** vi / en; component mới dùng utility Tailwind theo token; kiểm 9 theme.
10. VS Code Java biên dịch chồng vào `target/` → lỗi lạ → `./mvnw clean verify`.

---

## 10. Định nghĩa "xong"

- [x] `./mvnw clean verify` xanh (JDK 21), gồm `ProductScreeningIT`, `StubScreeningClientTest`, `GeminiScreeningClientTest`, `ModuleBoundaryTest`.
- [x] `npm run verify` xanh.
- [ ] Kịch bản mục 7 chạy đúng trên máy chủ dự án (chế độ `stub`; `http` nếu có khóa).
- [x] `c1.md`, `CHECKLIST_TIEN_DO_UC.md`, `Usecase.md`, `API_SPEC.md`, `ENTITY.md`, `cau-hinh-moi-truong.md` đã cập nhật như mục 6.
- [ ] Commit theo mục 8 khi chủ dự án yêu cầu; PR vào `develop`.

> **Ghi chú lệch nhỏ so với code thật (rà 2026-11-28 khi triển khai):**
> 1. Bảng `products` dùng cột `description` (không phải `description_html` như ghi ở bảng mục 4.6) → `ScreeningRequestBuilder` đọc `p.description`.
> 2. Bảng `users` không có cột `full_name` (tên hiển thị nằm ở `admin_profiles.full_name` / `customer_profiles.full_name`) → `AiScreeningSettings.adminName` đọc `COALESCE(ap.full_name, cp.full_name, u.email)`.
> 3. `web/src/shared/ui` đã có sẵn `Switch` và `ConfirmDialog` (đang dùng trong `StaffConfig` của `ProductModerationPage.tsx`) → `AiScreeningCard.tsx` tái sử dụng trực tiếp hai component này thay vì tự dựng lại.
> 4. `Usecase.md` nằm tại `docs/Document/diagrams/UseCaseDiagram/Usecase.md` (không có thư mục con `Document/`).

---

## 11. Bổ sung sau P06

- **Phần A** — Nhãn `ai_check` cho người kiểm duyệt (hàng chờ, chi tiết "AI lỗi" / "Chưa chấm", bộ lọc `?ai=`): xem [`update_gemini.md`](./update_gemini.md) mục 0–8.
- **Phần B** — Xử lý nội dung nhạy cảm (`safetySettings`, `SAFETY_BLOCKS`, thu nhỏ ảnh `ScreeningImageResizer`, gom ảnh phân loại & mô tả, nhãn ảnh): xem [`update_gemini.md`](./update_gemini.md) mục 9–16.

---

## 11. Ghi chú sau khi rà code (2026-11-28)

- Stub đổi sang so khớp nguyên từ, giữ dấu (mục 4.4) — bản đầu gắn cờ nhầm gần như mọi mô tả có "thuộc" / "bổ sung".
- Job quét giao việc cho `ProductScreeningListener.screenLater` (`@Async`) — mục 4.9.
- `.env` của dự án (`DotEnvEnvironmentPostProcessor`) **không cắt chú thích cuối dòng**: ghi `GEMINI_MODE=http`, `GEMINI_REJECT_THRESHOLD=85` **không** kèm `# …` trên cùng dòng, nếu không backend không khởi động (không bean nào khớp / `@Value int` lỗi).
- `GEMINI_MODE` chỉ nhận `stub` hoặc `http` — giá trị khác làm không có bean `ProductScreeningClient`.
- Mặc định `GEMINI_MODEL` đổi sang **`gemini-3.8-flash`** (2026-11-28): khóa API mới tạo gọi `gemini-2.5-flash` nhận `404 "This model … is no longer available to new users"`. Kiểm model khóa dùng được: `curl -H "x-goog-api-key: ${KEY}" "https://generativelanguage.googleapis.com/v1beta/models?pageSize=200"` (zsh: luôn viết `${M}:generateContent` có ngoặc nhọn). Gặp `503` (quá tải) là tạm thời — client tự thử lại 3 lần rồi mới chuyển "AI lỗi".
- **Quá hạn đọc phản hồi** (2026-11-28): `SocketTimeoutException` lúc đọc header bị Spring bọc thành `RestClientException` (không phải `ResourceAccessException`) → trước rơi vào "lỗi bất ngờ", không thử lại. `GeminiScreeningClient` thêm nhánh `catch (RestClientException)`: nguyên nhân là `IOException` → thử lại như 5xx; khác → `ScreeningFailedException`. Timeout mặc định 20 → **60 giây**.
- **Model dự phòng** (2026-11-28): `gemini-3.8-flash` hay quá hạn / `503` lúc đông → `GEMINI_FALLBACK_MODEL` (mặc định `gemini-3.8-flash-lite`) dùng cho **lần thử thứ 3**; `model_name` của kết quả ghi đúng model đã trả lời. Để trống biến = chỉ dùng model chính. Test `lastAttemptFallsBackToLighterModelWhenPrimaryIsOverloaded`.
