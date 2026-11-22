# Redis trong Genie — xem và hiểu dữ liệu trên máy dev

> Redis là kho dữ liệu **trong RAM**, dạng *key → value*, có thể tự hết hạn (TTL). Genie chỉ dùng Redis cho dữ liệu **tạm thời**: OTP, giới hạn tần suất, khóa chống bấm trùng, cache. Dữ liệu chính (tài khoản, Điểm, đơn hàng…) luôn nằm ở PostgreSQL.
> Mất toàn bộ Redis = người dùng phải xin OTP lại, cache tự nạp lại — **không mất dữ liệu nghiệp vụ**.
>
> Chạy môi trường trước: [huong-dan-chay-local.md](huong-dan-chay-local.md).

## 1. Redis chạy ở đâu

| | Giá trị máy dev |
|---|---|
| Container | `genie-local-redis-1` (image `redis:7-alpine`), khai trong `deploy/docker-compose.local.yml` |
| Cổng | `localhost:6379` |
| Mật khẩu | `REDIS_PASSWORD` trong `.env`, mặc định **`changeme`** (`--requirepass`) |
| Lưu đĩa | `--appendonly yes` vào volume `genie-local_redisdata` → tắt / bật container vẫn còn; `down -v` thì mất |
| Backend nối | `spring.data.redis.*` (`application.yml` / `application-local.yml`), code dùng `StringRedisTemplate` |

## 2. Cách xem dữ liệu

### 2.1. Dòng lệnh `redis-cli` (có sẵn trong container)

Mở PowerShell ở thư mục gốc repo:

```powershell
docker compose --env-file .env -f deploy/docker-compose.local.yml exec redis redis-cli -a changeme
```

(Đổi `changeme` nếu `.env` đặt `REDIS_PASSWORD` khác. Dòng cảnh báo *"Using a password with '-a'…"* là bình thường.)

Các lệnh hay dùng trong `redis-cli`:

| Lệnh | Tác dụng |
|---|---|
| `SCAN 0 MATCH otp:* COUNT 100` | Liệt kê key theo mẫu (an toàn, không treo như `KEYS *`) |
| `TYPE <key>` | Kiểu của key: `string`, `hash`, `zset`… |
| `GET <key>` | Đọc key kiểu string |
| `HGETALL <key>` | Đọc toàn bộ trường của key kiểu hash |
| `ZRANGE <key> 0 -1 WITHSCORES` | Đọc key kiểu sorted set |
| `TTL <key>` | Số giây còn lại trước khi tự xóa (`-1` = không hết hạn, `-2` = không tồn tại) |
| `DEL <key>` | Xóa một key (vd gỡ chặn 429 khi đang test) |
| `MONITOR` | Xem **trực tiếp** mọi lệnh backend gửi tới Redis — thao tác trên web rồi nhìn log chạy. `Ctrl+C` để thoát |
| `exit` | Thoát |

> Tránh `FLUSHALL` trừ khi thật sự muốn xóa sạch (mọi phiên OTP, cache… mất hết — dữ liệu PostgreSQL không ảnh hưởng).

### 2.2. Giao diện đồ họa

Cài **RedisInsight** (miễn phí) → *Add database* → host `localhost`, port `6379`, password `changeme` → gõ mẫu key (vd `otp:*`) vào ô tìm kiếm. Xem hash, TTL, xóa key bằng chuột.

## 3. Bản đồ key Genie đang dùng

### 3.1. Xác thực (AUTH)

| Mẫu key | Kiểu | TTL | Dùng để | Code |
|---|---|---|---|---|
| `otp:challenge:<id>` | hash | 5 phút | Một lần gửi OTP: `purpose`, `email`, `user_id`, `code_hash`, `attempts`, `resend_count`, `expires_at` | `auth/otp/infra/RedisOtpStore` |
| `otp:active:<PURPOSE>:<email>` | string | 5 phút | id challenge đang hiệu lực của email cho mục đích đó (LOGIN, REGISTER, PAYOUT_ACCOUNT_UPDATE…) | `RedisOtpStore` |
| `register:temp:<…>` | string | 15 phút | Thông tin đăng ký tạm chờ xác nhận OTP | `auth/register/application/RegistrationService` |
| `auth:reset:<…>` / `auth:pwreset_lock:<…>` | string | ngắn | Token đặt lại mật khẩu / khóa chống gửi dồn | `auth/password/application/PasswordResetService` |
| `auth:locked_by:<userId>` | string | tới lúc mở khóa | Lý do / người khóa tài khoản tạm | `auth/login/application/LockStatusStore` |
| `auth:denylist:<jti>` | string | thời gian sống còn lại của access token | Access token đã đăng xuất → bị từ chối dù chưa hết hạn | `common/security/TokenRevocationStore` |
| `auth:revoked_before:<userId>` | string | thời hạn access token + 1 phút | Mọi token cấp trước mốc này bị vô hiệu (đổi mật khẩu, đăng xuất mọi thiết bị) | `TokenRevocationStore` |
| `oauth:state:<…>` / `oauth:signup:<…>` | string | ngắn | Chống giả mạo khi đăng nhập Google/Zalo; dữ liệu đăng ký OAuth chờ OTP | `auth/oauth/infra/Redis*Store` |

**Về OTP:** Redis **không chứa mã 6 số**, chỉ có `code_hash = SHA-256(id : mã : OTP_PEPPER)` (`OtpChallengeService`). Mã thật chỉ nằm trong email (Mailpit http://localhost:8025). Nhập đúng → key bị xóa ngay (mỗi mã dùng 1 lần). Email chưa đăng ký vẫn sinh challenge nhưng `code_hash` **rỗng** và không gửi mail — chống dò email (user enumeration).

Giới hạn OTP: tối đa **5 lần nhập sai** / challenge, **gửi lại sau 60 giây**, **5 lần gửi / giờ / email**.

### 3.2. Giới hạn tần suất (rate limit) — tiền tố `rl:`

Cơ chế (`common/security/RateLimiter`): mỗi request `INCR` một bộ đếm, lần đầu thì đặt `EXPIRE` bằng cửa sổ thời gian; vượt ngưỡng → **`429 GNE-RAT-4290`** kèm header `Retry-After` = TTL còn lại.

| Mẫu key | Cửa sổ | Chặn cái gì |
|---|---|---|
| `rl:login:<ip>` | 1 phút | Số lần đăng nhập mật khẩu từ một IP |
| `rl:otp:<email>` | 60 giây | Gửi / gửi lại OTP quá nhanh |
| `rl:otp:hour:<email>` | 1 giờ | Quá 5 OTP / giờ |
| `rl:pwverify:<userId>` | 1 phút | Thử mật khẩu khi liên kết tài khoản OAuth |
| `rl:geocode:<ip>` | 1 phút | Tra tọa độ địa chỉ (gọi Nominatim) |
| `rl:bannerevt:<ip>` | 1 phút | Gửi sự kiện xem / click banner |
| `rl:auditexport:<userId>` | 1 phút | Xuất CSV nhật ký hệ thống |

> Đang test mà bị 429: `DEL rl:otp:<email>` (hoặc key tương ứng) để gỡ ngay thay vì chờ.

### 3.3. Banner (UC-ADM-12)

| Mẫu key | Kiểu | TTL | Dùng để | Code |
|---|---|---|---|---|
| `banner:live:<vị trí>` (riêng `CATEGORY_TOP` là `banner:live:CATEGORY_TOP:<categoryId>`) | string (JSON) | 60 giây | Cache danh sách banner đang chạy cho `GET /public/banners` — trang chủ không truy vấn DB mỗi lần tải | `admin/banner/application/BannerCache` (sửa / xóa banner → xóa cache) |
| `banner:<id>:imp` / `banner:<id>:clk` | bộ đếm | – | Gom lượt **xem / click** trong RAM | `BannerEventService` |
| | | | `BannerStatsFlushJob` định kỳ đọc bộ đếm, cộng vào bảng `banners` trong PostgreSQL rồi xóa | `BannerStatsFlushJob` |

Đó là lý do số lượt click trên `/admin/banners` **trễ một chút** so với lúc bấm.

### 3.4. Điểm Genie (UC-PRO-08/09)

| Mẫu key | TTL | Dùng để |
|---|---|---|
| `lock:checkin:<userId>` | 5 giây | Khóa chống bấm **Điểm danh** 2 lần cùng lúc (2 tab) — `promotion/coin/application/CheckInService` |
| `lock:coin:<userId>` | vài giây | Khóa chống đổi voucher trùng — `CoinRedemptionService` |

Khóa chỉ chặn bấm trùng; **số dư Điểm và quy tắc "1 lần / ngày" vẫn do PostgreSQL bảo đảm** (ràng buộc + transaction).

### 3.5. KYC & địa chỉ

| Mẫu key | Dùng để |
|---|---|
| `kyc:views` (sorted set) | Danh sách bản xem tạm (đã giải mã) của hồ sơ KYC kèm thời điểm; `KycViewCleanupJob` xóa tệp sau ~10 phút — `vendor/kyc/application/KycDossierStorage` |
| `region:ghn:<…>` | Cache danh mục tỉnh / phường từ GHN cho sổ địa chỉ — `auth/address/infra/GhnRegionDirectory` |

## 4. Bài thực hành — nhìn OTP chạy trong Redis

1. Mở `redis-cli` (mục 2.1), gõ `MONITOR` ở một cửa sổ để xem lệnh trực tiếp (tùy chọn).
2. Web `/auth/login` → tab **Mã OTP** → nhập `mai.tran@gmail.com` → **Gửi mã OTP**.
3. Ở cửa sổ `redis-cli` khác:
   ```
   SCAN 0 MATCH otp:* COUNT 100
   GET otp:active:LOGIN:mai.tran@gmail.com
   HGETALL otp:challenge:<id vừa lấy>
   TTL otp:challenge:<id>
   ```
   → thấy `purpose LOGIN`, `attempts 0`, `code_hash` là chuỗi hex dài, TTL ≤ 300.
4. Nhập **sai** mã trên web → `HGETALL` lại → `attempts 1`.
5. Bấm **Gửi lại** ngay → web báo phải chờ; `TTL rl:otp:mai.tran@gmail.com` cho biết còn bao nhiêu giây.
6. Sau 60 giây bấm **Gửi lại** → `resend_count` tăng, `code_hash` đổi (mã mới trong Mailpit).
7. Nhập **đúng** mã → `SCAN` lại → `otp:challenge:<id>` biến mất.
8. Thử email chưa đăng ký (`hehe@gmail.com`) → có challenge nhưng `code_hash` rỗng, Mailpit không có thư.

Tương tự có thể thử:
- **Điểm danh**: bấm ở `/account/points` rồi `GET lock:checkin:<userId>` trong 5 giây đầu.
- **Banner**: tải trang chủ → `GET banner:live:HOME_TOP_CAROUSEL` (JSON banner); bấm banner → `GET banner:<id>:clk` tăng; sau khi job chạy thì key mất và số click trong admin tăng.
- **Đăng xuất**: đăng xuất trên web → `SCAN 0 MATCH auth:denylist:* COUNT 100` có thêm một key.

## 5. Lỗi thường gặp

| Triệu chứng | Nguyên nhân / cách xử lý |
|---|---|
| `NOAUTH Authentication required` | Quên `-a changeme` |
| `WRONGPASS` | Mật khẩu khác → xem `REDIS_PASSWORD` trong `.env` |
| `service "redis" is not running` | Chưa `docker compose ... up -d` |
| Backend log `Unable to connect to Redis` | Container Redis tắt hoặc cổng 6379 bị chiếm → `docker ps`, bật lại |
| Web báo **503 GNE-SYS-5030** khi gửi / nhập OTP | Redis không truy cập được — không có Redis thì không xác minh được OTP nên backend cố ý từ chối. (Riêng rate limit thì bỏ qua khi Redis lỗi, không chặn người dùng.) |
| Bị 429 liên tục khi test | `TTL rl:...` để xem còn chờ bao lâu, hoặc `DEL` key đó |
