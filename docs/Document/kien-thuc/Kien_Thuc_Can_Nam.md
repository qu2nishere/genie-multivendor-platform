# BẢN ĐỒ KIẾN THỨC CẦN NẮM — GENIE BẢN TLCN
## Kiến thức kỹ thuật tối thiểu để xây, kiểm thử, triển khai và bảo vệ đồ án

> **Phạm vi:** đúng ngăn xếp công nghệ của TLCN (Spring Boot modular monolith, PostgreSQL, Redis, Silo, FastAPI KYC, PhoBERT phân tích cảm xúc, Google Gemini API, OAuth2 Google / Apple, Astro + React + i18n, Docker Compose) cho 87 use case / 65 bảng. Các chủ đề chỉ cần cho DATN (Kafka, RabbitMQ, Elasticsearch, Qdrant, Kubernetes, Saga, A/B testing, ML gợi ý vector, Flash Sale Redis Lua…) nằm ở `DATN/kien-thuc/Kien_Thuc_Can_Nam.md` — học khi mở rộng.
> **Cách dùng:** mỗi phần có **Cần biết** (bắt buộc để code), **Áp dụng trong dự án** (chỗ dùng cụ thể), **Câu hỏi bảo vệ** (hội đồng hay hỏi).

---

## MỤC LỤC
| # | Phần | Ai cần |
|---|---|---|
| 01 | [Năng lực theo vai trò](#p01) | Cả nhóm |
| 02 | [Git, quy trình nhóm & Jira](#p02) | Cả nhóm |
| 03 | [Java 21 & Spring Boot 3 (modular monolith)](#p03) | Backend |
| 04 | [JPA / Hibernate & PostgreSQL 16](#p04) | Backend |
| 05 | [Giao dịch, khóa & chống bán vượt tồn kho](#p05) | Backend |
| 06 | [Redis 7 ở mức MVP](#p06) | Backend |
| 07 | [Silo (kho tệp S3) & Presigned URL](#p07) | Backend, Frontend |
| 08 | [Bảo mật: JWT, Spring Security, OWASP](#p08) | Backend |
| 09 | [Tích hợp VNPay & SePay](#p09) | Backend |
| 10 | [Tích hợp GHN](#p10) | Backend |
| 11 | [WebSocket / STOMP](#p11) | Backend, Frontend |
| 12 | [Python FastAPI & thị giác máy tính cho KYC](#p12) | AI |
| 13 | [Astro, React & SEO](#p13) | Frontend |
| 14 | [Kiểm thử](#p14) | Cả nhóm |
| 15 | [Docker Compose, Nginx, CI/CD](#p15) | DevOps |
| 16 | [Nghiệp vụ marketplace & pháp lý cốt lõi](#p16) | Cả nhóm |
| 17 | [Bảng tra lỗi thường gặp](#p17) | Cả nhóm |
| 18 | [Câu hỏi bảo vệ đồ án & cách trả lời](#p18) | Cả nhóm |
| 19 | [Google Gemini API & structured output](#p19) | Backend |
| 20 | [OAuth2 / OpenID Connect (Google, Apple)](#p20) | Backend, Frontend |
| 21 | [RBAC 2 tầng & sổ cái kép](#p21) | Backend |
| 22 | [Deeplink, Open Graph & đa ngôn ngữ (i18n)](#p22) | Frontend |
| 23 | [PhoBERT — phân tích cảm xúc đánh giá](#p23) | AI, Backend |
| 24 | [Dựng frontend từ prototype: kiến trúc feature, token, theme, mock, SEO kỹ thuật](#p24) | Frontend |

---

<a id="p01"></a>
## PHẦN 01: NĂNG LỰC THEO VAI TRÒ

| Vai trò | Bắt buộc | Nên có |
|---|---|---|
| Backend | Java 21 (record, switch pattern), Spring Boot (Web, Security, Data JPA, Validation, Scheduling, WebSocket), SQL, giao dịch & khóa, REST | Testcontainers, ArchUnit, Flyway |
| AI | Python, FastAPI, OpenCV, ONNX Runtime, xử lý ảnh cơ bản | Đo độ chính xác (FAR / FRR) |
| Frontend | TypeScript, React (hooks, state), Astro SSR, Tailwind, gọi API có xác thực | TanStack Query, Playwright |
| DevOps | Docker, Compose, Nginx, Linux cơ bản, GitHub Actions | Let's Encrypt, backup |

---

<a id="p02"></a>
## PHẦN 02: GIT, QUY TRÌNH NHÓM & JIRA

- **Nhánh:** `main` (bản phát hành) · `develop` (tích hợp) · `feature/GNE-<số>-<mo-ta>` · `hotfix/…`.
- **Commit theo Conventional Commits:** `feat(order): conditional stock reservation`, `fix(payment): idempotent vnpay ipn`.
- **Pull request:** ≥ 1 người duyệt, CI xanh, có test cho luồng tiền.
- **Phiên bản:** SemVer `v1.0.0` cho bản bảo vệ; tag có chữ ký `git tag -s`.
- **Jira:** Epic = phân hệ (AUTH, CAT, ORD…), Story = use case (UC-ORD-07), Sub-task = việc kỹ thuật.
- Chi tiết: [Document_Vi.md](../document/Document_Vi.md) mục 4.1–4.2.

---

<a id="p03"></a>
## PHẦN 03: JAVA 21 & SPRING BOOT 3 (MODULAR MONOLITH)

**Cần biết**
- Vòng đời bean, `@Configuration`, `@ConfigurationProperties` (đọc `VNPAY_*`, `GHN_*` từ biến môi trường).
- `@RestController`, DTO + Bean Validation (`@NotBlank`, `@Positive`), `@ControllerAdvice` trả `ApiResponse` + mã lỗi `GNE-*`.
- `@Transactional` (propagation mặc định REQUIRED, chỉ có tác dụng khi gọi qua proxy — gọi nội bộ cùng class không mở transaction mới).
- `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` cho việc phụ (thông báo, email).
- `@Scheduled` (`fixedDelay`, `cron`) cho sweeper, giải ngân, quá hạn khiếu nại.
- Virtual Threads (Java 21): `spring.threads.virtual.enabled=true` — phù hợp gọi HTTP ra ngoài (GHN, VNPay).

**Áp dụng:** 11 module = 11 package; module khác chỉ gọi qua `*Facade` (kiểm bằng ArchUnit — implement.md Phần 2).

**Câu hỏi bảo vệ:** *Vì sao không dùng microservices?* → Nhóm nhỏ, cần ACID cho luồng tiền, vẫn chia module để tách sau (ADR-01).

---

<a id="p04"></a>
## PHẦN 04: JPA / HIBERNATE & POSTGRESQL 16

**Cần biết**
- Ánh xạ `@Entity`, `@Enumerated(STRING)`, `@Version`, `@CreationTimestamp`, JSONB (`@JdbcTypeCode(SqlTypes.JSON)`).
- Lazy loading & N+1: dùng `@EntityGraph` / `JOIN FETCH` cho danh sách đơn kèm item.
- **Flyway:** `V<n>__mo_ta.sql`, không sửa migration đã chạy; `ddl-auto=validate`.
- **Full-text search:** `tsvector`, `to_tsvector`, `websearch_to_tsquery`, `ts_rank`, extension `unaccent` (bỏ dấu), `pg_trgm` (`similarity`) — thay Elasticsearch ở quy mô MVP.
- Index B-Tree ghép theo thứ tự truy vấn, partial index (`WHERE status = 'HELD'`), GIN cho tsvector / JSONB.
- `EXPLAIN ANALYZE` để kiểm tra truy vấn chậm.

**Áp dụng:** 65 bảng ở [ENTITY.md](../diagrams/ClassDiagram/ENTITY.md); index nóng ở mục 13.1; SQL tìm kiếm ở implement.md Phần 3.2, 5.1; gợi ý từ khóa và sản phẩm tương tự bằng `pg_trgm` (UC-CAT-04, UC-CAT-07).

**Câu hỏi bảo vệ:** *Tại sao tiền dùng `DECIMAL(19,2)`?* → `double` sai số nhị phân, cộng dồn lệch sổ. *Snapshot là gì?* → chép tên / giá / địa chỉ vào đơn để đơn cũ không đổi khi shop sửa sản phẩm.

---

<a id="p05"></a>
## PHẦN 05: GIAO DỊCH, KHÓA & CHỐNG BÁN VƯỢT TỒN KHO

| Kỹ thuật | Dùng ở đâu | Ý chính |
|---|---|---|
| **UPDATE có điều kiện** (atomic) | Giữ chỗ tồn kho, giữ suất Flash Sale | `UPDATE … SET reserved = reserved + q WHERE stock - reserved >= q` — 0 dòng = hết hàng; Flash Sale: `WHERE flash_stock - sold_count - reserved_count >= q` |
| **Khóa lạc quan `@Version`** | `ProductVariant`, `Order`, `Voucher`, `VendorWallet`, `FlashSaleItem`, `UserCoin` | Hai luồng cùng sửa → luồng sau lỗi `OptimisticLockException` → thử lại (chống tiêu Điểm 2 lần) |
| **Khóa bi quan `SELECT … FOR UPDATE`** | IPN VNPay, ví shop khi giải ngân | Giữ khóa dòng tới hết transaction |
| **`FOR UPDATE SKIP LOCKED`** | Cron sweeper / giải ngân / hàng đợi duyệt | Nhiều instance hoặc nhiều Staff không xử lý trùng |
| **Idempotency** | IPN, webhook GHN, POST checkout / rút tiền | Lần thứ 2 trả kết quả cũ, không làm lại |

**Câu hỏi bảo vệ:** *100 người cùng mua SKU còn 10 thì sao?* → Đúng 10 lệnh UPDATE thành công vì điều kiện được PostgreSQL kiểm tra trong cùng câu lệnh có khóa dòng; 90 lệnh còn lại cập nhật 0 dòng → `GNE-INV-4090` (có test tích hợp chứng minh — Document_Vi 4.3).

---

<a id="p06"></a>
## PHẦN 06: REDIS 7 Ở MỨC MVP

| Dùng cho | Kiểu | TTL |
|---|---|---|
| OTP (`otp:challenge:{id}` — bản băm + số lần sai) | Hash | 5 phút |
| Dữ liệu đăng ký tạm (`register:temp:{email}`, mật khẩu đã băm) | String JSON | 15 phút |
| Phiên đặt lại mật khẩu, khóa khôi phục, lý do khóa đăng nhập, chặn access token (`auth:reset:*`, `auth:pwreset_lock:*`, `auth:locked_by:*`, `auth:denylist:{jti}`, `auth:revoked_before:{userId}`) | String | 10 – 30 phút (≤ thời gian sống access token) |
| Rate limit | `INCR` + `EXPIRE` | 1 phút / 1 giờ |
| Idempotency key | `SET NX` | 24 giờ (Document_Vi 3.5.2) |
| Cache danh mục, phí ship, số liệu sản phẩm / shop, banner, Flash Sale đang chạy | String / Hash | 30 giây – 1 giờ |
| Bộ đếm lượt xem / từ khóa / bấm banner | `INCR`, `ZINCRBY`, `PFADD` (HyperLogLog đếm khách khác nhau) | Tới khi job 5 phút ghi gộp vào PostgreSQL |
| `state` OAuth, khóa điểm danh | `SET NX EX` | 5 giây – 10 phút |

Danh mục khóa đầy đủ: [LUU_TRU_DU_LIEU.md](./LUU_TRU_DU_LIEU.md) mục 4.2. Khi Redis lỗi: rate limit / denylist cho qua (ghi cảnh báo), OTP / đăng ký / API tiền trả `503 GNE-SYS-5030` (lệnh Redis timeout 2 giây).

Nguyên tắc: Redis **không** là nguồn sự thật của tiền, tồn kho, suất Flash Sale hay Điểm trong TLCN — mất Redis chỉ chậm hơn và mất vài phút bộ đếm thống kê, không sai dữ liệu. (Flash Sale dùng Redis Lua là chủ đề của DATN.)

---

<a id="p07"></a>
## PHẦN 07: SILO (KHO TỆP S3) & PRESIGNED URL

- Bucket (danh mục đầy đủ ở [LUU_TRU_DU_LIEU.md](./LUU_TRU_DU_LIEU.md) mục 5): công khai `product-media`, `avatar-assets`, `review-media`, `content-assets`; riêng tư `chat-media`, `product-docs`, `refund-evidence`, `kyc-dossiers` (mã hóa AES-256-GCM), `ml-models` (nội bộ).
- Luồng tải lên: client xin **Presigned PUT URL** (15 phút; KYC 10 phút) vào `tmp/` → PUT thẳng lên Silo → báo backend khóa đối tượng → backend `statObject` kiểm tra kích thước / MIME rồi chuyển sang khóa chính thức.
- Xem tệp riêng tư: backend cấp **Presigned GET URL** 5 phút sau khi kiểm tra quyền.
- **Vì sao:** không để tệp lớn đi qua Spring Boot (tiết kiệm RAM, băng thông).
- **Silo là gì, vì sao không còn MinIO (2026-11-15, ADR 0009 trong repo):** MinIO Inc. ngừng phát hành bản cộng đồng — repo `minio/minio` bị lưu trữ (04/2026), image `minio/minio` bị gỡ khỏi Docker Hub (09/2026), không còn bản vá bảo mật → máy thành viên mới, runner CI, VPS mới không kéo được image, test tích hợp Testcontainers đỏ. **Silo** (`pgsty/silo`, do Pigsty duy trì) là fork cộng đồng của MinIO, giữ nguyên API S3, API admin, đường dẫn `/minio/*`, định dạng dữ liệu `.minio.sys` → ứng dụng **không đổi logic**: vẫn gọi qua SDK S3 `io.minio` (`MinioClient`), biến môi trường giữ tên `MINIO_*`, mã lỗi `GNE-MINIO-5003` giữ nguyên; chỉ đổi tên lớp `MinioObjectStorage` → `SiloObjectStorage`, dịch vụ Docker `silo` + `silo-init`, client dòng lệnh `mcli` (vẫn có tên `mc`).
- **Ý nghĩa thiết kế (hay được hỏi):** nhờ cổng `ObjectStorage` (hexagonal port) nói chuyện bằng giao thức S3, đổi nhà cung cấp kho tệp chỉ là đổi image + script khởi tạo; nếu Silo ngừng thì chuyển RustFS / Garage, hoặc lên cloud S3 / Cloudflare R2 chỉ bằng cấu hình `genie.storage.endpoint`.
- **Giấy phép:** Silo dùng **AGPL-3.0** như MinIO — dùng làm dịch vụ riêng, không sửa mã nguồn thì không phát sinh nghĩa vụ công bố mã của Genie (Phần 16).
- **Hiện thực (UC-AUTH-08):** ảnh đại diện `purpose = AVATAR` (JPG / PNG ≤ 1 MB): `POST /uploads/presigned-urls` → PUT vào `tmp/user/{id}/{uuid}.png` → `PATCH /me/profile` kèm `avatar_object_key` → backend `statObject`, chuyển sang `user/{id}/{uuid}.png`, xóa ảnh cũ sau commit. Ảnh Google khi đăng nhập lần đầu do backend tự tải về rồi lưu vào Silo (không lưu URL của Google).

---

<a id="p08"></a>
## PHẦN 08: BẢO MẬT — JWT, SPRING SECURITY, OWASP

- **JWT:** header.payload.signature; HS256 với khóa bí mật ≥ 256 bit; access 15 phút; **không** lưu dữ liệu nhạy cảm trong payload.
- **Refresh token xoay vòng:** mỗi lần dùng cấp token mới, thu hồi token cũ; token đã thu hồi bị dùng lại → thu hồi cả họ token (phát hiện đánh cắp). Hiện thực: mỗi phiên có `family_id` ổn định (= mã thiết bị ở màn Thiết bị đăng nhập, = claim `sid`); **ân hạn 10 giây** cho trường hợp hai tab cùng làm mới; khi nghi đánh cắp còn đặt `auth:revoked_before:{userId}` để access token cũ hết hiệu lực ngay (không phải chờ 15 phút).
- **Vì sao cookie `HttpOnly; SameSite=Strict` cho refresh, còn access token để trong bộ nhớ JS:** XSS không đọc được refresh token; CSRF không gửi được cookie từ trang khác; tải lại trang thì gọi `/auth/sessions/refresh` để lấy lại access token (kèm `user`).
- **Mật khẩu:** Argon2id (`Argon2PasswordEncoder` của Spring Security, tham số OWASP 2024 `m = 19456 KiB, t = 2, p = 1`, muối 16 B) — băm chậm có muối, **tốn bộ nhớ** nên chống dò bằng GPU / ASIC tốt hơn bcrypt ("cost factor" là khái niệm của bcrypt, không phải Argon2); mật khẩu chỉ truyền qua HTTPS, không mã hóa thêm phía client. Đăng nhập sai và sai email trả **cùng** mã `GNE-AUTH-4011`, có băm giả để thời gian xử lý đồng đều (chống dò email / timing attack).
- **OTP:** chỉ lưu SHA-256(mã + pepper) trong Redis, so sánh bằng `MessageDigest.isEqual` (thời gian hằng), đếm sai bằng `HINCRBY` (an toàn khi 2 request song song), dùng một lần; thao tác nhạy cảm khi đã đăng nhập dùng **OTP bước 2** (step-up) qua header `X-OTP-Challenge-Id` / `X-OTP-Code`.
- **Phân quyền:** `ROLE_*` + RoleHierarchy cho vai trò, authority theo **13 mã quyền** cho nhân viên (`@PreAuthorize("hasAuthority('REVIEW_KYC')")` — Phần 21); kiểm tra **quyền sở hữu** tài nguyên (chống IDOR).
- **OWASP Top 10 mức MVP:** SQL injection (JPA tham số hóa), XSS (escape đầu ra, không render HTML người dùng nhập), CSRF (cookie refresh `SameSite=Strict`), cấu hình sai (không mở cổng DB), lộ dữ liệu (che CCCD / STK), SSRF (chỉ tải ảnh từ Silo nội bộ).
- **Mã hóa ảnh CCCD:** AES-256-GCM (có xác thực toàn vẹn), khóa trong biến môi trường.

---

<a id="p09"></a>
## PHẦN 09: TÍCH HỢP VNPAY & SEPAY

- Tham số `vnp_*`, sắp xếp theo khóa, ký **HMAC-SHA512** bằng `vnp_HashSecret`; `vnp_Amount` = số tiền × 100.
- **Return URL** (trình duyệt quay về) ≠ **IPN** (server VNPay gọi server sàn): **chỉ IPN được cập nhật trạng thái**; Return URL có thể bị người dùng sửa / không bao giờ tới.
- Mã phản hồi IPN: `00` thành công, `01` không tìm thấy đơn, `02` đã xác nhận, `04` sai số tiền, `97` sai chữ ký.
- Hoàn tiền: API `refund` (`02` toàn phần / `03` một phần).
- Sandbox: thẻ NCB `9704198526191432198`, tên `NGUYEN VAN A`, ngày phát hành `07/15`, OTP `123456`.
- IPN cần URL HTTPS công khai → khi phát triển dùng Cloudflare Tunnel / ngrok.

**SePay (chuyển khoản ngân hàng VietQR)**
- SePay **theo dõi biến động** tài khoản ngân hàng đã liên kết và gửi webhook JSON: `id`, `gateway`, `transactionDate`, `accountNumber`, `content`, `transferType` (`in` / `out`), `transferAmount`, `accumulated`, `referenceCode`, `description`. SePay **không** chi tiền thay sàn.
- Xác thực webhook bằng header `Authorization: Apikey …` (SePay còn hỗ trợ HMAC-SHA256, OAuth 2.0); lọc webhook theo tiền tố nội dung (`TR`).
- Chống trùng theo `id`; trả HTTP 200 — endpoint lỗi thì SePay thử lại tối đa 7 lần (giãn cách Fibonacci, ~33 phút).
- **VietQR:** QR động chuẩn NAPAS / EMVCo chứa số tài khoản, số tiền, nội dung; khách quét bằng app ngân hàng bất kỳ.
- Dùng trong MVP cho: thanh toán chuyển khoản (UC-PAY-01/03), ghi nhận tiền COD GHN chuyển về (UC-LOG-03), tự hoàn tất lệnh rút tiền / hoàn tiền Staff chuyển tay (UC-ADM-05, UC-SUP-04).
- **Nội dung chuyển khoản là "khóa đối soát":** `TR{paymentId}` (thanh toán), `TRWD{id}` (rút tiền — tiền ra), `TRRF{id}` (hoàn tiền — tiền ra); tiền vào từ hãng vận chuyển → tạo phiên đối soát COD. Khách ghi sai nội dung → không tự khớp → Staff "Xác nhận tay" bằng mã giao dịch ngân hàng.
- **Trong repo:** endpoint `POST /api/v1/payments/sepay/webhook` (đã mở trong `SecurityConfig`, xác thực `Authorization: Apikey`), biến `SEPAY_API_KEY`, `SEPAY_ACCOUNT_NUMBER`; ảnh QR lấy từ `qr.sepay.vn` (đã khai trong CSP của Nginx); giả lập bằng `scripts/webhooks/sepay-webhook.http`. Giao diện VietQR hỏi lại phiên mỗi 3 giây; ở chế độ mock coi như SePay đã nhận tiền sau ~12 giây.
- **Chi phí:** SePay có gói miễn phí / gói thấp tính theo số giao dịch (cần lấy báo giá khi vận hành thật); chuyển khoản liên ngân hàng NAPAS 247 không mất % như cổng thẻ (xem BAO_CAO_DU_TOAN_CHI_PHI mục 3).

---

<a id="p10"></a>
## PHẦN 10: TÍCH HỢP GHN (VÀ CÁC HÃNG 3PL KHÁC)

- **Adapter pattern:** một interface `CarrierAdapter` (`quote`, `create`, `mapStatus`) và một lớp cho mỗi hãng (GHN, GHTK, ViettelPost, J&T) — thêm hãng không sửa luồng đơn hàng; điều phối (UC-LOG-15) gọi `quote` song song, timeout 3 giây, chọn hãng rẻ nhất trong nhóm giao nhanh.

- Header `Token` (theo tài khoản), `ShopId` (theo cửa hàng con của từng shop).
- Địa giới hành chính GHN (`ProvinceID`, `DistrictID`, `WardCode`) phải lưu trong `addresses` và `vendor_profiles.pickup*` — dùng mã GHN, không dùng tên.
- Tạo vận đơn với `client_order_code` = mã đơn sàn (idempotent khi gọi lại), `cod_amount` cho COD.
- Webhook: nhận POST trạng thái → idempotent → ánh xạ sang `ShipmentTrackingStatus` / `OrderStatus` (bảng ở implement.md Phần 9).
- **Đối soát COD:** GHN chuyển tiền thu hộ theo chu kỳ kèm bảng kê → khớp `trackingCode` + số tiền.

---

<a id="p11"></a>
## PHẦN 11: WEBSOCKET / STOMP

- WebSocket = kết nối 2 chiều lâu dài; **STOMP** = giao thức tin nhắn trên WebSocket (`SUBSCRIBE`, `SEND`, destination).
- Spring: `@EnableWebSocketMessageBroker`, simple broker `/queue`, đích riêng từng người `/user/queue/...`, xác thực JWT ở frame `CONNECT`.
- Frontend: `@stomp/stompjs`, tự kết nối lại, khi mất kết nối tải lại lịch sử bằng REST.
- **Giới hạn MVP:** simple broker chạy trong bộ nhớ một instance — muốn chạy nhiều instance phải dùng broker relay (bản mở rộng).

---

<a id="p12"></a>
## PHẦN 12: PYTHON FASTAPI & THỊ GIÁC MÁY TÍNH CHO KYC

| Chủ đề | Cần biết |
|---|---|
| FastAPI | Pydantic model, `Depends` (kiểm token nội bộ), `BackgroundTasks`, chạy Uvicorn 1 worker |
| Ảnh | OpenCV đọc / xoay / cắt, phát hiện mờ (phương sai Laplacian), lóa (tỷ lệ điểm ảnh bão hòa) |
| QR CCCD | Chuỗi phân tách bằng `|`: số CCCD, số CMND cũ, họ tên, ngày sinh, giới tính, địa chỉ, ngày cấp |
| MRZ (TD1 – 3 dòng × 30 ký tự) | Cấu trúc, check digit (trọng số 7-3-1), đối chiếu với QR |
| Nhận diện khuôn mặt | **YuNet** phát hiện mặt + 5 điểm mốc; **SFace** sinh embedding 128 chiều; so **cosine similarity** (ngưỡng ~0,85 — hiệu chỉnh trên tập thử) |
| Liveness | Thử thách chủ động (nháy mắt / quay đầu) phía client + kiểm tra ảnh in / màn hình đơn giản |
| ONNX Runtime | Nạp mô hình một lần, chạy CPU; đo thời gian suy luận |
| Đánh giá | FAR (chấp nhận nhầm) vs FRR (từ chối nhầm); chọn ngưỡng theo mục tiêu an toàn |
| Dữ liệu sinh trắc | Chỉ xử lý khi có đồng ý riêng; không lưu ảnh ở AI Worker; xóa embedding khi xóa hồ sơ |

**Câu hỏi bảo vệ:** *Có chặn được CCCD giả không?* → Không tuyệt đối (không đọc chip NFC / C06); rủi ro khống chế bằng tầng 2: tiền chỉ rút về tài khoản ngân hàng đúng tên — ngân hàng đã xác thực sinh trắc chủ tài khoản.

---

<a id="p13"></a>
## PHẦN 13: ASTRO, REACT & SEO

- **Astro SSR:** trang render ở server → bot tìm kiếm đọc được nội dung; **islands** chỉ nạp JavaScript cho phần tương tác (giỏ hàng, chat).
- **React:** hooks, quản lý trạng thái form, TanStack Query (cache API, tự refetch).
- **Xác thực phía client:** access token trong bộ nhớ, refresh token cookie HttpOnly; interceptor tự refresh khi `401`.
- **SEO:** `title` / `meta description` / Open Graph, JSON-LD `Product`, `sitemap.xml`, URL thân thiện `/p/ao-thun-nam-123`.
- **Core Web Vitals:** LCP (ảnh chính có `fetchpriority="high"`, kích thước cố định), CLS (đặt sẵn kích thước ảnh), INP (tránh JS nặng).
- **Tailwind:** responsive `sm/md/lg`, dark mode không bắt buộc.
- **Đào sâu khi bắt tay dựng giao diện:** 3 kiểu trang, chỉ thị hydrate, 4 tầng trong feature, token → theme, lớp mock, SEO kỹ thuật — xem [Phần 24](#p24).

---

<a id="p14"></a>
## PHẦN 14: KIỂM THỬ

| Loại | Công cụ | Ghi nhớ |
|---|---|---|
| Unit | JUnit 5, AssertJ, Mockito | Test phương thức nghiệp vụ entity (máy trạng thái, tính tiền) |
| Tích hợp | Spring Boot Test + **Testcontainers** (PostgreSQL thật, Redis thật) | Test đồng thời bằng `ExecutorService` + `CountDownLatch` |
| API | Apidog / Postman | Mỗi UC ≥ 1 kịch bản đúng + 1 kịch bản lỗi |
| E2E | Playwright | 9 luồng chính (Document_Vi 4.3), gồm nhân viên đúng quyền |
| Tải | k6 | `vus: 50, duration: '10m'`, ngưỡng `http_req_duration{p(95)}<500` |
| Bảo mật | OWASP ZAP baseline | Không có cảnh báo High |

5 kịch bản bắt buộc của luồng tiền: Document_Vi.md mục 4.3.

---

<a id="p15"></a>
## PHẦN 15: DOCKER COMPOSE, NGINX, CI/CD

- **Dockerfile nhiều tầng:** tầng build Maven / npm → tầng chạy JRE / Node nhỏ gọn, chạy user không phải root.
- **Compose:** `depends_on` + `healthcheck`, `restart: unless-stopped`, volume cho dữ liệu, `.env` cho bí mật (không commit).
- **Nginx:** reverse proxy `/api` → backend, `/ws` (header `Upgrade`, `Connection`) → backend, `/` → frontend, ảnh công khai → Silo; HTTPS bằng Let's Encrypt (`certbot`).
- **GitHub Actions:** CI cho PR (build + test), release theo tag (build image → GHCR → SSH deploy).
- **Sao lưu & khôi phục:** `pg_dump` / `pg_restore`; **thử khôi phục ít nhất một lần**.
- Tệp mẫu: Document_Vi.md mục 4.4.

---

<a id="p16"></a>
## PHẦN 16: NGHIỆP VỤ MARKETPLACE & PHÁP LÝ CỐT LÕI

- **Tách đơn:** 1 phiên thanh toán → N đơn con theo shop; mỗi đơn con có vận đơn, ký quỹ, khiếu nại riêng.
- **Ký quỹ 7 ngày & hoa hồng theo dòng hàng:** `netAmount = grossAmount − commissionFee − shopShippingSubsidy`.
- **Ai chịu voucher:** voucher shop giảm tiền shop nhận; voucher sàn do sàn chịu.
- **COD kết hợp ký quỹ:** hai mốc tiền — khách trả (lúc giao) ≠ tiền về sàn (đối soát); ký quỹ COD ở `PENDING_FUNDING` tới khi tiền về, sau đó dùng chung quy tắc 7 ngày với VNPay / VietQR.
- **Voucher xếp chồng 3 tầng + Điểm:** Shop (mỗi shop 1) → Sàn (1) → Freeship (1) → Điểm tối đa 50% tiền hàng còn lại (1 Điểm = 1₫, hết hạn 12 tháng); chiết khấu phân bổ xuống từng dòng để hoàn một phần đúng.
- **Flash Sale:** suất giới hạn, giá snapshot vào đơn, shop đăng ký giảm ≥ 15% so với giá 30 ngày.
- **Pháp lý tối thiểu:** quy chế sàn, chính sách quyền riêng tư & đồng ý sinh trắc, đổi trả 7 ngày, không giữ ví người mua, nghĩa vụ thuế khi vận hành thật.
- **Quyền xóa dữ liệu (Luật Bảo vệ dữ liệu cá nhân 2025, hiệu lực 01/01/2026; trước đó Nghị định 13/2023/NĐ-CP):** người dùng tự yêu cầu xóa tài khoản (UC-AUTH-10) → 30 ngày để đổi ý → hệ thống **ẩn danh hóa** (không xóa cứng) vì chứng từ kế toán (đơn, thanh toán, sổ cái) phải lưu tối thiểu 10 năm theo Luật Kế toán; dữ liệu nhận dạng (email, họ tên, SĐT, ảnh, địa chỉ, liên kết Google) bị xóa / ghi đè, đánh giá hiển thị "Người dùng đã xóa". Mọi lần yêu cầu / hủy / ẩn danh ghi nhật ký kiểm toán.
- **Giấy phép phần mềm:** Silo (kho tệp) và MinIO SDK: AGPL-3.0 / Apache-2.0 — Genie chỉ chạy Silo như một dịch vụ độc lập, không sửa mã nên không phải công bố mã nguồn của sàn; nếu sửa mã Silo và cung cấp qua mạng thì phải công bố phần sửa.
- Chi tiết: [BAO_CAO_KIEN_THUC_KINH_DOANH_KINH_TE_v1.0.md](./BAO_CAO_KIEN_THUC_KINH_DOANH_KINH_TE_v1.0.md), [BAO_CAO_HE_THONG_CHINH_SACH_HOP_DONG_PHAP_LY_v1.0.md](./BAO_CAO_HE_THONG_CHINH_SACH_HOP_DONG_PHAP_LY_v1.0.md).

---

<a id="p17"></a>
## PHẦN 17: BẢNG TRA LỖI THƯỜNG GẶP

| Triệu chứng | Nguyên nhân hay gặp | Cách xử lý |
|---|---|---|
| IPN VNPay không về | URL localhost / HTTP | Dùng tunnel HTTPS, cấu hình IPN URL trên cổng merchant sandbox |
| `97 Invalid Checksum` | Sai thứ tự tham số, encode khác nhau giữa lúc ký và lúc gửi | Ký trên chuỗi đã URL-encode giống hệt chuỗi gửi đi |
| `LazyInitializationException` | Truy cập quan hệ lazy ngoài transaction | `JOIN FETCH` / DTO projection |
| `OptimisticLockException` dồn dập | Nhiều luồng sửa cùng dòng | Thử lại có giới hạn; với tồn kho dùng UPDATE có điều kiện |
| Cron giải ngân chạy trùng | 2 instance cùng chạy | `FOR UPDATE SKIP LOCKED` + kiểm trạng thái trong transaction |
| WebSocket mất kết nối sau Nginx | Thiếu header Upgrade / timeout | `proxy_set_header Upgrade $http_upgrade; proxy_read_timeout 3600s;` |
| AI Worker hết RAM | Nhiều worker nạp lại mô hình | `WORKERS=1`, nạp mô hình khi khởi động |
| GHN báo sai địa chỉ | Lưu tên thay vì mã GHN | Dùng `DistrictID` / `WardCode` từ API master-data |
| Tìm kiếm tiếng Việt không ra | Chưa `unaccent` cả khi lưu và khi tìm | Dùng cùng cấu hình `vn_unaccent` ở trigger và truy vấn |
| Flyway báo checksum mismatch | Sửa migration đã chạy | Hoàn tác thay đổi, tạo migration mới |
| Gemini trả lỗi 429 / hết hạn mức | Gói miễn phí giới hạn lượt / phút | Thử lại giãn cách; quá 3 lần → `fallback()` duyệt tay; demo trải lượt gọi |
| Gemini trả JSON thiếu trường | Không đặt `responseSchema` / prompt mơ hồ | Luôn đặt `responseMimeType` + `responseSchema`; validate lại phía Java, sai → `NEED_REVIEW` |
| Link chia sẻ không hiện ảnh trên Facebook / Zalo | Trang render phía client, thiếu `og:image` tuyệt đối | Render SSR, `og:image` là URL HTTPS tuyệt đối 1200×630; dùng Facebook Sharing Debugger làm mới cache |
| Nhân viên đổi quyền mà vẫn vào được mục cũ | Access token cũ còn hạn | Thu hồi refresh token khi đổi quyền; access token 15 phút |
| Đăng nhập Google lỗi `redirect_uri_mismatch` | URI trên Google Console khác URI gửi đi | Callback cố định `<web>/auth/oauth-callback` — khai đúng URI này cho từng môi trường (local, tunnel, VPS) |
| `docker compose up` báo không kéo được `minio/minio` / test `*IT` đỏ vì Testcontainers | Image MinIO đã bị gỡ khỏi Docker Hub (2026) | Dùng `pgsty/silo` (ADR 0009); máy có dữ liệu cũ thì chép volume `miniodata` → `silodata` |
| Tài khoản tạo bằng Google đăng nhập bằng mật khẩu bị báo lỗi | Tài khoản chưa có mật khẩu (`password = NULL`) | Backend trả `GNE-AUTH-4012` (không tính lần sai); dùng Google, mã OTP email hoặc Quên mật khẩu để thiết lập |
| Liên kết Google ở trang tài khoản báo `GNE-AUTH-4098` | Chọn Gmail khác email tài khoản Genie | Chọn đúng tài khoản Google có cùng email |
| Hai tab cùng mở, một tab bị đăng xuất | Hai tab làm mới token cùng lúc | Web gom làm mới một lần / tab; backend có ân hạn 10 giây cho token vừa xoay |
| Biến màu / font trong CSS không có tác dụng, build cảnh báo `@import` | `@import` đặt sau `@tailwind` — CSS bắt buộc `@import` đứng đầu file | Đưa mọi `@import` lên trước `@tailwind base` *(plan_chuyen_giao_dien.md — G02.08)* |
| Chạy dev thấy đúng, build xong mất kiểu dáng một số khối | Tên lớp ghép động (`'tr-root--' + x`) nên Tailwind không thấy và loại bỏ | Viết đủ tên lớp trong mã hoặc khai `safelist` *(plan_chuyen_giao_dien.md — G07.03)* |
| Tải trang nháy từ theme mặc định sang theme đã chọn | Đặt `data-theme` sau khi React chạy | Script nội tuyến trong `<head>` đọc lựa chọn và đặt `data-theme` trước khi trình duyệt vẽ *(plan_chuyen_giao_dien.md — G07.05)* |
| Bấm "Thêm vào giỏ" mà số trên header không đổi | Hai island là hai cây React riêng, không chung Context | Dùng nanostores làm kho chung *(plan_chuyen_giao_dien.md — G07.15, G10.03)* |
| `window is not defined` / `localStorage is not defined` khi chạy SSR | Mã trình duyệt chạy ở máy chủ lúc render | Kiểm `typeof window` trong hàm lưu trữ; phần chỉ có nghĩa ở trình duyệt dùng `client:only="react"` *(plan_chuyen_giao_dien.md — G04.06)* |
| Cảnh báo "hydration mismatch" ở đồng hồ đếm ngược, ngày giờ | HTML máy chủ và lần vẽ đầu ở trình duyệt khác nhau (thời gian, múi giờ) | Tính giá trị phụ thuộc thời gian sau khi component đã gắn (`useEffect`) *(plan_chuyen_giao_dien.md — G06.17)* |
| ESLint báo lỗi `boundaries/element-types` | Import sâu vào tầng trong của feature khác, hoặc `ui` gọi thẳng `infrastructure` | Import qua `index.ts` của feature; đi qua hook ở `application` *(plan_chuyen_giao_dien.md — mục 1, R4–R5)* |
| `git commit` bị chặn | commitlint: `scope` ngoài danh sách, `subject` viết hoa, dòng đầu > 100 ký tự | Sửa thông điệp cho đúng định dạng; không dùng `--no-verify` *(plan_chuyen_giao_dien.md — mục 5)* |
| Trang "không tìm thấy sản phẩm" vẫn bị Google lập chỉ mục | Trả mã 200 kèm giao diện báo lỗi (soft 404) | Trả mã 404 thật từ frontmatter Astro *(plan_chuyen_giao_dien.md — G08.31, G08.53)* |

---

<a id="p18"></a>
## PHẦN 18: CÂU HỎI BẢO VỆ ĐỒ ÁN & CÁCH TRẢ LỜI

| Câu hỏi | Ý trả lời |
|---|---|
| Phạm vi đồ án có quá lớn không? | TLCN 87 use case / 65 bảng phủ đúng giao diện đã dựng, chia 8 sprint; 6 sprint đầu là vòng nghiệp vụ lõi; DATN 206 UC là đồ án tốt nghiệp, tên / mã giữ nguyên để mở rộng |
| Vì sao dùng Gemini mà không tự huấn luyện mô hình kiểm duyệt? | Không có dữ liệu gán nhãn; Gemini hiểu chữ + ảnh, trả JSON theo schema; chỉ sàng lọc — Moderator luôn duyệt cuối; ~40đ / lượt |
| Gửi dữ liệu sang Gemini có lộ thông tin không? | Chỉ gửi nội dung sản phẩm công khai; không gửi CCCD, số tài khoản, nội dung giấy tờ |
| Nhân viên sàn phân quyền thế nào? | Vai trò + 13 mã quyền; 3 chức danh chỉ là mẫu quyền mặc định; Admin chỉnh từng quyền; API kiểm `@PreAuthorize`, giao diện chỉ ẩn mục; không có Super Admin |
| Xếp chồng voucher có làm âm tiền shop không? | Voucher Sàn / Freeship / Điểm do sàn chịu (ghi sổ cái `PLATFORM_VOUCHER`, `FREESHIP_COST`, `COIN_COST`); shop chỉ chịu voucher Shop của mình |
| Vì sao các "use case" Kafka, Saga… không còn? | Chúng không có tác nhân chủ động → là yêu cầu phi chức năng, không phải use case UML; MVP không cần hạ tầng đó |
| Đảm bảo không bán vượt tồn thế nào? | UPDATE có điều kiện + `@Version` + giữ chỗ 15 phút + sweeper; có test 100 luồng |
| Nếu IPN tới 2 lần? | Khóa dòng giao dịch, đã `SUCCESS` thì trả `02`, không ghi lại; webhook SePay chống trùng theo `id` |
| Vì sao OTP chỉ qua email? | SMS / ZNS tốn 300–700đ/tin và cần đăng ký brandname; email gần như miễn phí, đủ an toàn cho khách hàng thường |
| SePay có chuyển tiền cho shop được không? | Không — SePay chỉ báo biến động số dư; Staff chuyển khoản tay, SePay xác nhận để hệ thống tự hoàn tất lệnh |
| Tiền khách nằm ở đâu? | Tài khoản doanh nghiệp của sàn; `escrow_ledgers` ghi phần nợ từng shop; giải ngân sau 7 ngày hoặc khi khách xác nhận |
| Vì sao không làm ví khách hàng? | Cần giấy phép trung gian thanh toán; MVP hoàn tiền về VNPay / chuyển khoản |
| KYC có an toàn không? | 3 tầng: tài liệu (QR + MRZ), sinh trắc (face match + liveness), tài chính (rút tiền chính chủ); điểm rủi ro + lấy mẫu hậu kiểm; nêu rõ giới hạn không đọc chip |
| Hệ thống chịu được bao nhiêu người? | Mục tiêu TLCN 50 CCU, đo bằng k6; hướng mở rộng: tách module, thêm instance, broker, search engine (DATN) |
| Hoàn một phần tính thế nào? | Ví dụ đơn 1.000.000đ trả 300.000đ → khách 300.000 + shop 665.000 + sàn 35.000 |
| Vì sao đổi MinIO sang Silo giữa chừng? Có rủi ro không? | MinIO ngừng phát hành bản cộng đồng, image bị gỡ → không dựng được môi trường mới, không có bản vá. Silo là fork giữ nguyên API S3 nên code chỉ đổi tên lớp; nhờ cổng `ObjectStorage` đổi nhà cung cấp chỉ là đổi image. Rủi ro: phụ thuộc Pigsty → ghim tag, có phương án RustFS / Garage / cloud S3 |
| Vì sao chọn SePay thay vì cổng chuyển khoản khác? | SePay đọc biến động số dư tài khoản ngân hàng thật và đẩy webhook — phù hợp VietQR (không mất % như thẻ), đối soát COD, xác nhận lệnh chi tay; idempotent theo `id`; xác thực API key |
| Tài khoản tạo bằng Google muốn dùng mật khẩu thì sao? | "Thiết lập mật khẩu" qua OTP email (đang đăng nhập: UC-AUTH-06; chưa đăng nhập: Quên mật khẩu). Đăng nhập mật khẩu khi chưa có → `GNE-AUTH-4012`, không bị tính là sai |
| Sao liên kết Google phải trùng email? | Một người = một tài khoản = một email; tránh gắn Gmail của người khác hoặc Gmail đang là tài khoản Genie khác; đồng nhất với tự liên kết theo email khi đăng nhập Google |
| Xóa tài khoản có xóa đơn hàng không? | Không — chờ 30 ngày rồi ẩn danh dữ liệu cá nhân; đơn / thanh toán / sổ cái giữ để đúng Luật Kế toán; cron 02:30 xử lý theo lô `FOR UPDATE SKIP LOCKED` |
| Phát hiện refresh token bị đánh cắp thế nào? | Mỗi lần làm mới token cũ bị thu hồi; ai dùng lại token cũ (quá 10 giây ân hạn) → thu hồi mọi phiên + chặn access token cũ bằng `revoked_before` |

---

<a id="p19"></a>
## PHẦN 19: GOOGLE GEMINI API & STRUCTURED OUTPUT

**Cần biết**
- REST `POST …/models/{model}:generateContent` với API key (header `x-goog-api-key`); `contents` gồm phần chữ và ảnh (`inline_data` base64 hoặc URL tệp); `systemInstruction` cho chính sách cấm bán.
- **Structured output:** `generationConfig.responseMimeType = "application/json"` + `responseSchema` (kiểu OpenAPI: `enum`, `integer` có `minimum` / `maximum`, `array`) → mô hình bắt buộc trả JSON đúng khuôn; phía Java vẫn validate lại.
- Token & chi phí: ảnh nhỏ ≈ 258 token; đặt giới hạn `maxOutputTokens`; xử lý `429` (hạn mức) và `5xx` bằng thử lại giãn cách.
- Hạn chế của LLM: có thể sai / không ổn định giữa các lần gọi → dùng làm **bộ lọc**, người duyệt cuối; lưu phản hồi gốc để giải thích quyết định.

**Áp dụng:** UC-CAT-12 / UC-CAT-13; mã ở implement.md 5.3; chi phí ở [BAO_CAO_DU_TOAN_CHI_PHI_VA_GIAI_PHAP_AI_v1.0.md](./BAO_CAO_DU_TOAN_CHI_PHI_VA_GIAI_PHAP_AI_v1.0.md) Phần 6B.

---

<a id="p20"></a>
## PHẦN 20: OAUTH2 / OPENID CONNECT (GOOGLE, APPLE)

- **Authorization Code + PKCE:** trình duyệt → trang đồng ý của nhà cung cấp → quay về `redirect_uri` kèm `code` → backend đổi `code` lấy `id_token`.
- **Kiểm `id_token`:** chữ ký theo JWKS của nhà cung cấp, `iss`, `aud` = client id, `exp`, `nonce`; `state` chống CSRF (Redis 10 phút).
- **Nguyên tắc:** một người = một tài khoản = tối đa một mật khẩu; Google / Apple chỉ là **thêm một cách đăng nhập** vào cùng tài khoản (cùng đơn hàng, ví, Điểm), không tạo tài khoản thứ hai. Liên kết = gắn `oauthProvider` + `oauthProviderId` (= claim `sub`) vào `users` đã có.
- **Luồng khi bấm "Đăng nhập Google" (UC-AUTH-03):**
  1. Tìm `users` theo (`oauthProvider`, `oauthProviderId = sub`) → có thì kiểm `status` rồi đăng nhập luôn.
  2. Không có → `email_verified` phải là `true`, nếu không thì **từ chối liên kết** (chống chiếm tài khoản: kẻ gian tạo tài khoản IdP mang email nạn nhân chưa xác minh).
  3. Tìm theo email → có: kiểm trạng thái (đang khóa tạm `lockedUntil` → `423`, `SUSPENDED` → `403`; không liên kết, không cấp phiên; đã gắn nhà cung cấp khác → `409 GNE-AUTH-4092`), hợp lệ thì **liên kết** rồi đăng nhập; không có: tạo user mới `password = NULL`, `role = CUSTOMER`, `status = ACTIVE`; ảnh Google tải về lưu Silo.
  4. Tài khoản đã có: tên / ảnh Google **chỉ điền khi hồ sơ trống** — không ghi đè thông tin người dùng tự nhập.
- **Nhận diện bằng `sub`, không bằng email:** email bên Google đổi được, `sub` thì không; sau khi đã liên kết luôn tìm theo `sub` trước.
- **Ràng buộc CSDL:** `users.email` UNIQUE; `users.password` cho phép NULL (tài khoản chỉ có Google / Apple); UNIQUE (`oauth_provider`, `oauth_provider_id`) — `uk_users_oauth`, một tài khoản Google chỉ gắn với một user. TLCN lưu thẳng trên `users` (mỗi user tối đa 1 nhà cung cấp); khi cần nhiều nhà cung cấp cho cùng user (DATN) tách bảng `user_identities` (`user_id`, `provider`, `provider_user_id`, UNIQUE (`provider`, `provider_user_id`)).
- **Mật khẩu (UC-AUTH-06):** `password = NULL` → nút "Thiết lập mật khẩu", không hỏi mật khẩu cũ, xác thực OTP email; đã có mật khẩu → "Đổi mật khẩu", bắt buộc mật khẩu cũ. Sau khi thiết lập, đăng nhập được bằng cả email + mật khẩu lẫn Google. Đăng nhập email + mật khẩu với `password = NULL` → `401 GNE-AUTH-4012` ("Tài khoản đăng nhập bằng Google — dùng Google, đăng nhập bằng mã email hoặc thiết lập mật khẩu"), **không** tính là một lần sai.
- **Liên kết / gỡ liên kết thủ công** (Tài khoản → "Liên kết đăng nhập"): người đang đăng nhập bấm "Liên kết Google" → web gọi `POST /me/identities/google/authorize` (XHR có token, vì trình duyệt chuyển hướng không mang access token) → Google → callback chung → `POST /me/identities/google` gắn `sub` vào tài khoản hiện tại. Luật đã hiện thực: **email Google phải trùng email tài khoản** (`GNE-AUTH-4098`), `sub` đã gắn user khác → `4092`, state đăng nhập và state liên kết không dùng lẫn. "Gỡ liên kết" chỉ cho khi tài khoản đã có mật khẩu (`4093`) **và phải nhập lại mật khẩu hiện tại** (người mượn máy đang đăng nhập không gỡ được).
- **Tình huống đặc biệt:** quên mật khẩu với tài khoản chỉ có Google → cho "Thiết lập mật khẩu" qua OTP email hoặc nhắc dùng Google; người dùng đổi email bên Google → vẫn vào đúng tài khoản nhờ `sub`, email trong CSDL giữ nguyên; hai email khác nhau (một đăng ký thường, một Google) → hai tài khoản khác nhau, đúng hành vi mong đợi.
- **Kiểm thử nên có:** đã liên kết → đăng nhập bằng `sub`; email trùng + `email_verified = false` → từ chối; email trùng tài khoản bị khóa → từ chối; email mới → tạo user `password = NULL`; gỡ liên kết khi chưa có mật khẩu → từ chối; liên kết `sub` đang thuộc user khác → vi phạm `uk_users_oauth`. Tài liệu gốc: `account-linking-google-login.md` ở thư mục gốc repo; giao diện mô phỏng ở `genie/ui_kits/auth/` (bảng tình huống Google) và Tài khoản → "Liên kết đăng nhập".
- Apple: email có thể là địa chỉ chuyển tiếp ẩn danh; chỉ trả họ tên ở lần đầu đồng ý — phải lưu ngay. **Hiện trạng:** Apple tắt bằng cờ `genie.oauth.apple.enabled = false` (cần Apple Developer Program trả phí, tên miền HTTPS, `client_secret` ký ES256) — Google đã chạy thật.
- **Callback cố định:** backend không nhận `redirect_uri` từ client (chống open redirect); trang cần quay về (`next`) lưu trong `state` ở Redis 10 phút, `state` đọc xong xóa ngay (dùng một lần).

---

<a id="p21"></a>
## PHẦN 21: RBAC 2 TẦNG & SỔ CÁI KÉP

**RBAC**
- Tầng 1 vai trò (`CUSTOMER`, `VENDOR`, `STAFF`, `ADMIN`) — quyết định khu vực (storefront / seller / admin).
- Tầng 2 mã quyền (13 mã: `VIEW_DASHBOARD`, `REVIEW_KYC`, `MANAGE_CATALOG`, `MODERATE_REVIEW`, `HANDLE_DISPUTE`, `PROCESS_PAYOUT`, `UPDATE_COMMISSION`, `VIEW_FINANCE`, `MANAGE_VOUCHER`, `MANAGE_PLATFORM_NOTIFICATION`, `VIEW_AUDIT_LOG`, `MANAGE_USER`, `MANAGE_RBAC`) — quyết định từng chức năng trong Admin.
- Nguyên tắc **tách biệt nhiệm vụ**: người duyệt chi tiền không đồng thời sửa hoa hồng; mọi thay đổi quyền ghi nhật ký.

**Sổ cái kép (double-entry)**
- Mỗi nghiệp vụ tiền sinh ≥ 2 bút toán, tổng Nợ = tổng Có; sổ chỉ ghi thêm, sai thì ghi bút toán đảo.
- Tài khoản dùng ở TLCN: tiền gửi ngân hàng của sàn, phải trả ký quỹ, phải trả người bán, doanh thu hoa hồng, chi phí khuyến mãi — bảng bút toán ở implement.md 7.7.

---

<a id="p22"></a>
## PHẦN 22: DEEPLINK, OPEN GRAPH & ĐA NGÔN NGỮ (I18N)

- **Deeplink web:** URL ổn định `/p/{slug}-{id}?v={variantId}&src=share` — `id` là khóa thật, `slug` chỉ để đẹp; đổi `slug` → 301.
- **Open Graph:** `og:title`, `og:description`, `og:image` (HTTPS tuyệt đối, 1200×630), `og:url`, `og:type=product`, `og:price:amount`, `og:price:currency`; bot của Facebook / Zalo không chạy JavaScript → bắt buộc SSR.
- **Web Share API:** `navigator.share({title, url})` trên điện thoại; máy tính dùng `navigator.clipboard.writeText`.
- **i18n:** tách chuỗi theo khóa (`vi.json`, `en.json`), không ghép chuỗi bằng tay; định dạng tiền / ngày bằng `Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'})`; script CI phát hiện khóa thiếu.

---

<a id="p23"></a>
## PHẦN 23: PHOBERT — PHÂN TÍCH CẢM XÚC ĐÁNH GIÁ (UC-AI-07)

| Chủ đề | Cần biết |
|---|---|
| PhoBERT là gì | Mô hình ngôn ngữ RoBERTa huấn luyện trước trên ~20 GB văn bản tiếng Việt (VinAI); bản `base` ~135 triệu tham số. Fine-tune thêm một lớp phân loại để ra 3 nhãn cảm xúc |
| Tách từ trước khi đưa vào | PhoBERT học trên văn bản **đã tách từ** ("sản_phẩm", "giao_hàng"); suy luận phải dùng đúng bộ tách từ lúc huấn luyện (RDRSegmenter / VnCoreNLP), nếu không độ chính xác giảm mạnh |
| Tiền xử lý tiếng Việt | Chuẩn hóa Unicode NFC (dấu tổ hợp vs dựng sẵn), teencode ("ko", "dc", "sp"), emoji, lặp ký tự ("đẹppppp"); giữ nguyên quy trình giữa huấn luyện và chạy thật |
| Đầu ra | Logits → softmax → nhãn có xác suất cao nhất + độ tin cậy; ngưỡng 0,6 để tính thống kê, 0,85 để ưu tiên kiểm duyệt (hiệu chỉnh lại trên tập kiểm thử) |
| Đánh giá mô hình | Accuracy, precision / recall / **macro-F1** (dữ liệu đánh giá thường lệch về tích cực nên không chỉ nhìn accuracy), ma trận nhầm lẫn — nhầm Trung tính ↔ Tiêu cực là lỗi hay gặp |
| Chạy trên CPU | Xuất ONNX + lượng tử hóa INT8, ONNX Runtime; nạp một lần khi khởi động; giới hạn 256 token; batch khi chạy lại theo lô |
| Tích hợp | Bất đồng bộ sau khi lưu đánh giá (không làm chậm người dùng); lưu `aspect_sentiment_json` khóa `overall` để DATN thêm phân tích theo khía cạnh mà không đổi cấu trúc |
| Giới hạn | Mỉa mai, câu khen chê lẫn lộn, câu tiếng Anh → độ tin cậy thấp; kết quả chỉ tham khảo, không tự ẩn đánh giá |

**Câu hỏi bảo vệ:** *Sao không gọi Gemini cho cảm xúc như kiểm duyệt sản phẩm?* → Nhóm đã có mô hình PhoBERT chuyên tiếng Việt; tự host nên không tốn phí theo lượt, nội dung đánh giá không rời hệ thống, và chạy được trong AI Worker sẵn có. *Mô hình sai thì sao?* → Chỉ dùng để thống kê và xếp thứ tự hàng chờ; ẩn đánh giá luôn do nhân viên quyết định; kết quả ghi kèm phiên bản mô hình để chạy lại khi có bản tốt hơn.

---

<a id="p24"></a>
## PHẦN 24: DỰNG FRONTEND TỪ PROTOTYPE — KIẾN TRÚC FEATURE, TOKEN, THEME, MOCK, SEO KỸ THUẬT

> Phần này gom kiến thức phát sinh khi lập kế hoạch chuyển toàn bộ giao diện prototype sang ứng dụng `web`. Mỗi mục có dòng **📌 Nằm trong plan** ghi tên file plan và mã mục / task để tra ngược. Quy tắc gốc nằm ở `TECH_STACK_VA_QUY_UOC.md` mục 2.4, 3.4, 5, 11, 13.

### 24.1 Ba kiểu trang và chỉ thị hydrate của Astro

**Cần biết:**
- Một trang Astro mặc định **không gửi JavaScript**. Component React chỉ "sống" ở trình duyệt khi gắn chỉ thị `client:*`; không gắn thì nó chỉ là HTML tĩnh do máy chủ dựng.
- `client:load` (chạy ngay), `client:idle` (khi trình duyệt rảnh), `client:visible` (khi cuộn tới) **vẫn được máy chủ dựng HTML trước** → bot đọc được. Riêng `client:only="react"` **không có HTML từ máy chủ** → không dùng cho nội dung cần SEO.
- Ba kiểu trang: **SSR + islands** (trang công khai), **SSR khung + một island lớn** (trang cần đăng nhập), **SPA** (một island `client:only` chứa cả React Router).

**Áp dụng trong dự án:** header, giỏ, checkout dùng `client:load`; nút yêu thích, chia sẻ dùng `client:idle`; đánh giá, sản phẩm tương tự, "Gợi ý hôm nay" dùng `client:visible`; Seller Center, Admin Portal, chat dùng `client:only="react"`. SPA đặt `basename` cho React Router (`/seller`, `/admin`) và trang Astro bắt mọi đường con bằng `[...path].astro`.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — mục 1 (R2, R3, R17), bảng 4.1 cột "Kiểu trang", task G11.01–G11.06, G12.01–G12.06, G13.08.

**Câu hỏi bảo vệ:** *Sao không làm cả sàn thành một SPA React?* → Storefront cần SEO và ảnh xem trước khi chia sẻ; bot Facebook / Zalo không chạy JavaScript. Seller / Admin không cần SEO nên dùng SPA cho nhanh và đơn giản.

### 24.2 Bốn tầng trong một feature phía frontend

**Cần biết:**
- Chiều phụ thuộc: `ui → application → domain ← infrastructure`.
  - `domain`: kiểu dữ liệu và hàm thuần TypeScript — không React, không `fetch`, không `localStorage`.
  - `infrastructure`: gọi API, kiểu DTO, mapper, lưu trình duyệt.
  - `application`: hook TanStack Query, store, luồng nhiều bước — không có JSX.
  - `ui`: island `.tsx` và phần tĩnh `.astro`.
- **DTO khác kiểu domain:** DTO giữ nguyên `snake_case` như JSON của API; **mapper** đổi sang kiểu domain `camelCase`. API đổi tên trường thì chỉ sửa mapper, giao diện không đổi.
- Mỗi feature có một file `index.ts` là **cửa công khai duy nhất**; feature khác không được import sâu vào bên trong. Thứ gì từ 2 feature trở lên dùng thì chuyển vào `shared/`; `shared/` không bao giờ import `features/`.
- `eslint-plugin-boundaries` biến các luật trên thành lỗi lint: khai từng loại thư mục là một "element" rồi liệt kê element nào được import element nào, mặc định là cấm.

**Áp dụng trong dự án:** mọi feature dùng chung một khuôn tên file (`<x>.types.ts`, `<x>.rules.ts`, `<x>.dto.ts`, `<x>.mock.ts`, `<x>.mapper.ts`, `<x>.api.ts`, `use<X>.ts`, `<x>.store.ts`) và một thứ tự làm cố định: types → rules + test → dto → mock → mapper → api → application → ui → index → gắn vào trang → khóa i18n.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — mục 1 (R4, R5, R10), mục 3.1 "Khuôn một feature", task G01.06–G01.08, G13.10.

**Câu hỏi bảo vệ:** *Frontend mà cũng chia tầng thì có thừa không?* → Hàm tính tiền (voucher xếp chồng, Điểm) nằm ở `domain` nên test được bằng Vitest mà không cần trình duyệt; và khi backend xong chỉ thay thân hàm ở `infrastructure`, không đụng giao diện.

### 24.3 Trạng thái: chọn đúng công cụ

**Cần biết:**
- Mỗi island là **một cây React riêng** → `Context` không truyền được từ island này sang island khác trên cùng trang. Muốn chia sẻ (số lượng giỏ trên header ↔ nút "Thêm vào giỏ") phải dùng kho nằm ngoài React: **nanostores** (`atom`, `map`, hook `useStore`).
- Dữ liệu lấy từ máy chủ → **TanStack Query** (cache, tải lại, thử lại). Không sao chép dữ liệu máy chủ vào `useState`.
- Form → **react-hook-form + zod**: zod khai lược đồ một lần, vừa kiểm dữ liệu vừa suy ra kiểu TypeScript; component nhập liệu cần `forwardRef` để `register` gắn được.
- Prototype dùng biến toàn cục `window.*` và tự phát sự kiện để các màn nói chuyện với nhau — cách này không mang sang sản phẩm; thay bằng import ES module + ba công cụ trên.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — mục 1 (R7, R8), task G05.07, G05.09, G06.04, G06.14, G07.15, G10.03.

### 24.4 Token thiết kế → biến CSS → theme Tailwind

**Cần biết:**
- **Token** là giá trị thiết kế có tên (màu, cỡ chữ, khoảng cách, bo góc, đổ bóng). Khai thành biến CSS ở `:root`, rồi cho Tailwind trỏ vào biến (`colors: { primary: 'var(--color-primary)' }`) → đổi một chỗ, cả giao diện đổi theo.
- Hai lớp token: **thang cơ sở** (`--brand-500`) và **bí danh ngữ nghĩa** (`--color-primary`). Mã sản phẩm dùng bí danh.
- Thứ tự trong file CSS gốc: `@import` phải đứng **trước** `@tailwind`. Dùng `@layer base` / `@layer components` để CSS tự viết xếp đúng tầng ưu tiên với utility.
- Tailwind chỉ giữ lại lớp nó **nhìn thấy nguyên văn** trong mã nguồn; tên lớp ghép bằng chuỗi lúc chạy sẽ bị loại → viết đủ tên hoặc khai `safelist`.
- **Font tự lưu trữ:** `@font-face` với `font-display: swap` (hiện chữ dự phòng trước, không để trắng) và `unicode-range` (tách tập `latin` / `latin-ext` / `vietnamese`, trình duyệt chỉ tải tập có dùng).

**Áp dụng trong dự án:** giữ nguyên tên biến của prototype để CSS storefront và các theme dùng lại được; `style` nội tuyến của prototype đổi thành utility dùng token; thành phần dùng chung viết bằng utility, còn CSS riêng của storefront giữ dạng file đặt trong `@layer components`.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — mục 1 (R13), mục 2 (D2), task G02.01–G02.15, G07.01–G07.03.

### 24.5 Nhiều theme bằng `data-theme` và chống nháy

**Cần biết:**
- Mỗi theme là một khối `[data-theme="tên"] { … }` ghi đè biến CSS (và vài quy tắc riêng). Đổi theme = đổi thuộc tính `data-theme` trên thẻ `<html>`, không cần tải lại trang.
- **Chống nháy (FOUC):** nếu chờ React chạy mới đặt theme, người dùng thấy theme mặc định chớp một cái. Phải có một đoạn script **nội tuyến trong `<head>`**, chạy trước khi trình duyệt vẽ: đọc lựa chọn đã lưu rồi đặt `data-theme`.
- Sự kiện `storage` của trình duyệt cho phép các tab đang mở cùng đổi theme khi một tab thay đổi.
- Mỗi theme có thể kéo theo font riêng → chỉ khai `@font-face`, trình duyệt tự tải font nào thực sự được dùng.
- `prefers-reduced-motion`: hiệu ứng mở đầu của theme phải tắt được cho người chọn giảm chuyển động.

**Áp dụng trong dự án:** 9 theme, áp cho cả storefront, trang đăng nhập, Seller Center, Admin Portal; lựa chọn lưu ở trình duyệt với khóa có tiền tố `genie.`.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — mục 2 (D1), task G07.05, G07.22–G07.26, G03.09.

### 24.6 Lớp mock: dựng giao diện khi chưa có backend

**Cần biết:**
- Mock đặt ở tầng `infrastructure`, **dạng DTO** giống hệt JSON sau này của API, rồi vẫn đi qua mapper. Giao diện không bao giờ import mock trực tiếp.
- Hàm trong `*.api.ts` có **chữ ký cuối cùng** ngay từ đầu (tham số, kiểu trả về, phân trang cursor); một cờ môi trường quyết định trả mock hay gọi thật. Nối backend = thay thân hàm.
- Biến môi trường của Astro: tiền tố `PUBLIC_` thì trình duyệt đọc được, không có tiền tố thì chỉ máy chủ đọc; kiểm bằng zod ngay lúc khởi động để thiếu biến là báo lỗi sớm.
- Mock nên có độ trễ giả và trường hợp lỗi (ví dụ hết hàng) để giao diện có đủ trạng thái đang tải / lỗi / rỗng.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — mục 1 (R10), mục 2 (D3), task G01.04, G01.05, G05.01–G05.06, G13.20.

**Câu hỏi bảo vệ:** *Giao diện chạy mock thì lúc nối API có phải viết lại không?* → Không. Mock nằm sau cùng một hàm `*.api.ts` và cùng kiểu DTO với hợp đồng API; chỉ thay phần thân hàm, các tầng trên giữ nguyên.

### 24.7 SEO kỹ thuật (bổ sung Phần 13 và Phần 22)

**Cần biết:**
- **Liên kết thật:** điều hướng phải là `<a href>`; `onClick` đổi màn thì bot không đi theo được và người dùng không mở được tab mới.
- **URL chuẩn (canonical):** nhiều URL cùng một nội dung (lọc, sắp xếp, tham số theo dõi) phải chỉ về một URL chuẩn bằng `<link rel="canonical">`. Trang kết quả tìm kiếm nội bộ đặt `noindex, follow`.
- **Dữ liệu có cấu trúc (JSON-LD) theo loại trang:** trang chủ `Organization` + `WebSite` kèm `SearchAction`; trang danh sách `ItemList`; đường dẫn `BreadcrumbList`; trang sản phẩm `Product` có `offers` (giá, tiền tệ, tình trạng hàng) và `aggregateRating`.
- **Phân trang:** có liên kết `?page=N` thật; "cuộn để tải thêm" chỉ là lớp nâng cao bên trên.
- **HTML ngữ nghĩa:** mỗi trang một `<h1>`, tiêu đề không nhảy cấp, có `<header> / <nav> / <main> / <footer>`.
- **Mã trạng thái:** không tồn tại → 404 thật (tránh "soft 404": trả 200 kèm màn báo lỗi); đổi slug → 301.
- **`robots.txt`** chặn khu vực riêng tư (`/account`, `/checkout`, `/seller`, `/admin`) và trỏ tới sitemap; trang cần đăng nhập đặt thêm `noindex`.
- **Cách kiểm:** mở trang khi **tắt JavaScript** — vẫn phải thấy tên, giá, mô tả, liên kết; đo thêm bằng Lighthouse.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — mục 1 (R14, R16–R19), task G04.12, G07.04, G08.35, G08.44–G08.54, G13.09.

**Câu hỏi bảo vệ:** *Làm sao chứng minh trang đã thân thiện SEO?* → Có bài kiểm tự động chạy trình duyệt tắt JavaScript xác nhận nội dung và liên kết có trong HTML máy chủ trả về, JSON-LD phân tích được, tiêu đề và URL chuẩn đúng; kèm điểm Lighthouse của trang chủ, danh mục, sản phẩm.

### 24.8 Đa ngôn ngữ ở mức triển khai (bổ sung Phần 22)

**Cần biết:**
- Khóa đặt theo `<feature>.<màn>.<ý nghĩa>` (`checkout.summary.total`); nhóm dùng chung `common.*`; thông điệp lỗi tra theo mã `GNE-*`.
- Một hàm `t(key, params)` dùng cho cả Astro (máy chủ) lẫn React (trình duyệt); có **nội suy** `{name}` thay vì nối chuỗi; thiếu khóa thì rơi về tiếng Việt.
- Ngôn ngữ lưu trong cookie → `middleware` đọc được ngay lúc render SSR, HTML trả về đã đúng ngôn ngữ.
- Lấy chính chuỗi tiếng Việt làm khóa (cách prototype làm) thì nhanh nhưng sửa một chữ là mất bản dịch và không biết chuỗi thuộc màn nào → sản phẩm dùng khóa có cấu trúc.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — mục 1 (R9), task G04.07–G04.11, G07.10, G13.01, G13.02.

### 24.9 Trợ năng (accessibility) cho thành phần dùng chung

**Cần biết:**
- **Hộp thoại:** giữ focus bên trong (focus trap), đóng bằng Esc, khóa cuộn nền, trả focus về nút đã mở, có `role="dialog"` và `aria-modal`.
- Nút chỉ có biểu tượng phải có `aria-label`; vùng chạm tối thiểu 44×44 px.
- `:focus-visible` luôn nhìn thấy; có liên kết "Bỏ qua tới nội dung chính" ở đầu trang.
- Thẻ tab, danh sách chọn, ô OTP phải dùng được bằng bàn phím (mũi tên, Tab, dán cả chuỗi).
- Tương phản chữ / nền đạt chuẩn trên **từng theme**, không chỉ theme mặc định.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — task G06.03, G06.08, G06.09, G06.12, G06.16, G07.06, G07.26, G13.07.

### 24.10 Phân quyền ở giao diện chỉ là trải nghiệm

**Cần biết:** ẩn mục menu, chặn route, chuyển hướng trong `middleware` chỉ giúp người dùng không đi lạc; ai cũng sửa được mã ở trình duyệt. Quyền thật luôn do máy chủ kiểm. Vì access token chỉ nằm trong bộ nhớ, `middleware` phía web không biết người dùng đã đăng nhập hay chưa → dùng một cookie cờ không nhạy cảm chỉ để chuyển hướng cho tiện.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — task G05.07, G05.08, G07.10, G12.03, G12.04.

### 24.11 Kiểm thử phía frontend (bổ sung Phần 14)

**Cần biết:**
- **Vitest** cho tầng `domain` / `application`: hàm thuần nên không cần trình duyệt, chạy nhanh. Ưu tiên logic tiền: tính voucher xếp chồng, trừ Điểm, gộp giỏ khách.
- **Playwright**: khai nhiều `project` để cùng một kịch bản chạy ở màn 360 px và 1440 px; `webServer` tự bật ứng dụng trước khi test; có thể tắt JavaScript để kiểm SSR.
- **So hình với prototype** ở 3 khổ (360 / 768 / 1440 px) là bước kiểm bắt buộc trước mỗi commit giao diện.
- Một lệnh gộp (lint → kiểm kiểu → test → build → so khóa i18n) làm "cổng" trước khi commit, để mỗi commit đều build được.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — mục 6 "Cổng kiểm tra", task G01.02, G01.03, G01.09, G10.10, G13.11–G13.16.

### 24.12 Commit được máy kiểm (bổ sung Phần 02)

**Cần biết:**
- **Husky** chạy script trước khi commit; **commitlint** đọc dòng đầu thông điệp và chặn nếu sai: `type` ngoài danh sách, `scope` ngoài danh sách cố định, `subject` viết hoa hoặc có dấu chấm cuối, dòng đầu quá 100 ký tự. **lint-staged** chỉ lint những file sắp commit.
- Phần giao diện dùng ba `scope`: `web` (storefront, phần dùng chung, đăng nhập), `seller-ui`, `admin-ui`.
- Một commit = một thay đổi logic, build được, test xanh → chia việc lớn thành nhóm nhỏ, mỗi nhóm kết thúc bằng một commit; nhờ vậy cũng tách được PR khi vượt ngưỡng ~400 dòng.
- Người commit lấy từ cấu hình git của máy; không ghi đè bằng `--author`, không bỏ qua hook bằng `--no-verify`.

**📌 Nằm trong plan:** `plan_chuyen_giao_dien.md` — mục 5 "Quy ước nhánh & commit", task G00.01, G00.04–G00.06, mọi task có mã `Gxx.Cn`.

---

## LỊCH SỬ PHIÊN BẢN
- **TLCN v1.4 (2026-11-16):** đồng bộ với mã nguồn: Phần 07 viết lại cho **Silo** (vì sao rời MinIO, giữ API S3, giấy phép AGPL, luồng ảnh đại diện thật); Phần 06 khóa Redis của AUTH; Phần 08 tham số Argon2id, OTP bước 2, `family_id` + ân hạn 10 giây, `revoked_before`; Phần 09 nội dung chuyển khoản `TR` / `TRWD` / `TRRF`, endpoint webhook SePay trong repo; Phần 16 quyền xóa dữ liệu (Luật BVDLCN 2025) + giấy phép phần mềm; Phần 17 thêm 5 lỗi thường gặp; Phần 18 thêm 6 câu hỏi bảo vệ; Phần 20 theo hiện thực (`GNE-AUTH-4012`, liên kết phải trùng email `GNE-AUTH-4098`, gỡ liên kết nhập lại mật khẩu, callback cố định, Apple sau cờ).
- **TLCN v1.3 (2026-11-11):** thêm Phần 24 (dựng frontend từ prototype: 3 kiểu trang & chỉ thị hydrate, 4 tầng trong feature, nanostores / TanStack Query / react-hook-form + zod, token → theme Tailwind, nhiều theme chống nháy, lớp mock, SEO kỹ thuật, i18n, trợ năng, kiểm thử frontend, commit có máy kiểm) — mỗi mục ghi rõ nằm trong `plan_chuyen_giao_dien.md` ở mục / task nào; Phần 17 thêm 9 lỗi frontend thường gặp; Phần 13 thêm dòng dẫn sang Phần 24.
- **MVP v1.0 (2026-10-28):** viết lại từ bản đầy đủ (30 phần, 1.800 dòng) theo ngăn xếp MVP; bỏ Kafka, RabbitMQ, Elasticsearch, Qdrant, Kubernetes, Saga, A/B testing, ML gợi ý, Flash Sale Lua, Genie Express, PWA offline; thêm phần tích hợp VNPay / GHN, KYC ONNX, full-text PostgreSQL, câu hỏi bảo vệ.
- **TLCN v1.2 (2026-11-06):** thêm Phần 23 (PhoBERT phân tích cảm xúc đánh giá, UC-AI-07 — 87 use case). Phần 20 bổ sung đầy đủ liên kết tài khoản Google (luồng 3 bước, nhận diện bằng `sub`, UNIQUE `uk_users_oauth`, thiết lập / đổi mật khẩu, gỡ liên kết, tình huống đặc biệt, ca kiểm thử) theo `account-linking-google-login.md`; đổi thương hiệu Genie / Điểm.
- **TLCN v1.1 (2026-10-29):** phạm vi 86 use case / 64 bảng; thêm Phần 19 (Gemini structured output), 20 (OAuth2 / OIDC), 21 (RBAC 2 tầng & sổ cái kép), 22 (deeplink, Open Graph, i18n); cập nhật Redis (bộ đếm, HyperLogLog), khóa cho Flash Sale / Điểm, adapter đa hãng 3PL, nghiệp vụ voucher 3 tầng + Điểm + COD kết hợp ký quỹ, lỗi thường gặp và câu hỏi bảo vệ.
