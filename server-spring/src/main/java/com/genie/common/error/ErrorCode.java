package com.genie.common.error;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi {@code GNE-<MIỀN>-<HTTP><STT>} — danh mục đầy đủ ở Document_Vi 3.7.1 / API_SPEC 15; thêm mã mới thì thêm vào
 * danh mục, vào {@code web/src/shared/api/error-codes.ts} và khóa {@code errors.GNE-*} ở vi.json / en.json.
 * Thông điệp mặc định tiếng Việt — client hiển thị theo khóa i18n của mã.
 */
public enum ErrorCode {

    // ===== Chung =====
    VALIDATION_FAILED("GNE-VAL-4220", HttpStatus.UNPROCESSABLE_ENTITY, "Dữ liệu yêu cầu không hợp lệ."),
    RATE_LIMITED("GNE-RAT-4290", HttpStatus.TOO_MANY_REQUESTS, "Bạn thao tác quá nhanh, vui lòng thử lại sau."),
    NOT_FOUND("GNE-SYS-4040", HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên."),
    METHOD_NOT_ALLOWED("GNE-SYS-4050", HttpStatus.METHOD_NOT_ALLOWED, "Phương thức không được hỗ trợ."),
    INTERNAL_ERROR("GNE-SYS-5000", HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống gặp lỗi, vui lòng thử lại sau."),
    SERVICE_UNAVAILABLE("GNE-SYS-5030", HttpStatus.SERVICE_UNAVAILABLE, "Hệ thống đang bận, vui lòng thử lại."),
    DB_CONFLICT_RETRY("GNE-DB-5002", HttpStatus.INTERNAL_SERVER_ERROR, "Dữ liệu vừa được thay đổi, vui lòng thử lại."),
    UPLOAD_URL_EXPIRED("GNE-MINIO-5003", HttpStatus.INTERNAL_SERVER_ERROR, "Liên kết tải tệp đã hết hạn."),

    // ===== Bảo mật (LLD 12, API_SPEC 15) =====
    UNAUTHORIZED("GNE-SEC-4010", HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn hoặc không hợp lệ."),
    FORBIDDEN("GNE-SEC-4030", HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này."),
    INVALID_PERMISSION_GRANT("GNE-SEC-4031", HttpStatus.FORBIDDEN, "Thao tác phân quyền không hợp lệ."),

    // ===== AUTH (API_SPEC 2, 3, 15) =====
    OAUTH_INVALID("GNE-AUTH-4001", HttpStatus.BAD_REQUEST, "Phiên đăng nhập mạng xã hội không hợp lệ, vui lòng thử lại."),
    BAD_CREDENTIALS("GNE-AUTH-4011", HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác."),
    PASSWORD_NOT_SET("GNE-AUTH-4012", HttpStatus.UNAUTHORIZED,
            "Tài khoản đăng nhập bằng Google — dùng Google, đăng nhập bằng mã email hoặc thiết lập mật khẩu."),
    ACCOUNT_SUSPENDED("GNE-AUTH-4031", HttpStatus.FORBIDDEN, "Tài khoản đang bị đình chỉ."),
    OAUTH_EMAIL_UNVERIFIED("GNE-AUTH-4032", HttpStatus.FORBIDDEN, "Email của tài khoản mạng xã hội chưa được xác minh."),
    /**
     * Tài khoản nội bộ STAFF / ADMIN (kể cả nhân viên chờ kích hoạt) không đăng nhập / liên kết bằng Google, Zalo —
     * {@code details.provider} = tên nhà cung cấp. Câu chữ cố ý chung chung để không lộ email đó là tài khoản nội bộ.
     */
    OAUTH_ACCOUNT_UNAVAILABLE("GNE-AUTH-4033", HttpStatus.FORBIDDEN,
            "Tài khoản này hiện không thể tiếp tục với Google, "
                    + "vui lòng chọn tài khoản khác hoặc thử đăng nhập bằng mật khẩu."),
    EMAIL_ALREADY_REGISTERED("GNE-AUTH-4091", HttpStatus.CONFLICT, "Email đã được đăng ký."),
    IDENTITY_ALREADY_LINKED("GNE-AUTH-4092", HttpStatus.CONFLICT, "Tài khoản mạng xã hội này đã gắn với người dùng khác."),
    LAST_LOGIN_METHOD("GNE-AUTH-4093", HttpStatus.CONFLICT, "Không thể gỡ cách đăng nhập duy nhất."),
    NEW_EMAIL_TAKEN("GNE-AUTH-4094", HttpStatus.CONFLICT, "Email mới đã thuộc tài khoản khác."),
    PASSWORD_SETUP_REQUIRED("GNE-AUTH-4095", HttpStatus.CONFLICT, "Cần thiết lập mật khẩu trước khi đổi email."),
    ACCOUNT_NOT_LOCKED("GNE-AUTH-4096", HttpStatus.CONFLICT, "Tài khoản không ở trạng thái khóa tạm."),
    CURRENT_SESSION_NOT_REVOCABLE("GNE-AUTH-4097", HttpStatus.CONFLICT, "Dùng đăng xuất để thoát phiên hiện tại."),
    IDENTITY_EMAIL_MISMATCH("GNE-AUTH-4098", HttpStatus.CONFLICT,
            "Vui lòng chọn tài khoản Google có email trùng với email tài khoản Genie."),
    OTP_EXPIRED("GNE-AUTH-4101", HttpStatus.GONE, "Mã OTP đã hết hiệu lực."),
    RESET_TOKEN_EXPIRED("GNE-AUTH-4102", HttpStatus.GONE, "Phiên đặt lại mật khẩu đã hết hạn."),
    INVITATION_EXPIRED("GNE-AUTH-4103", HttpStatus.GONE, "Lời mời đã hết hạn."),
    OTP_TOO_MANY_ATTEMPTS("GNE-AUTH-4221", HttpStatus.UNPROCESSABLE_ENTITY, "Nhập sai mã quá 5 lần."),
    OTP_INVALID("GNE-AUTH-4222", HttpStatus.UNPROCESSABLE_ENTITY, "Mã OTP không đúng."),
    PASSWORD_REUSED("GNE-AUTH-4223", HttpStatus.UNPROCESSABLE_ENTITY, "Mật khẩu mới phải khác mật khẩu hiện tại."),
    EMAIL_UNCHANGED("GNE-AUTH-4224", HttpStatus.UNPROCESSABLE_ENTITY, "Email mới trùng email hiện tại."),
    ACCOUNT_LOCKED("GNE-AUTH-4230", HttpStatus.LOCKED, "Tài khoản đang bị khóa tạm thời."),

    // ===== Người dùng (API_SPEC 3.5, 3.8) =====
    ACTIVE_ORDERS_BLOCK_DELETION("GNE-USR-4091", HttpStatus.CONFLICT, "Còn đơn hàng đang xử lý, chưa thể xóa tài khoản."),
    DEFAULT_ADDRESS_NOT_DELETABLE("GNE-USR-4092", HttpStatus.CONFLICT, "Không thể xóa địa chỉ mặc định."),
    DUPLICATE_ADDRESS("GNE-USR-4093", HttpStatus.CONFLICT, "Địa chỉ này đã có trong sổ địa chỉ."),

    // ===== Quản trị — nhân viên & phân quyền (API_SPEC 11.18, 15 — UC-ADM-01/02/03, plan B1) =====
    STAFF_EMAIL_TAKEN("GNE-ADM-4091", HttpStatus.CONFLICT, "Email đã được sử dụng."),
    STAFF_SELF_OR_LAST_ADMIN("GNE-ADM-4092", HttpStatus.CONFLICT,
            "Không thể thao tác trên chính tài khoản của bạn hoặc Quản trị viên cuối cùng."),
    STAFF_NOT_PENDING("GNE-ADM-4093", HttpStatus.CONFLICT, "Nhân viên này không còn ở trạng thái chờ kích hoạt."),
    STAFF_EMAIL_DOMAIN("GNE-ADM-4221", HttpStatus.UNPROCESSABLE_ENTITY, "Email nhân viên không thuộc tên miền được phép."),

    // ===== Danh mục, thuộc tính & sản phẩm (API_SPEC 5.2, 11.11, 15 — UC-CAT-01, 14, 15: plan B2) =====
    CATEGORY_NOT_FOUND("GNE-CAT-4041", HttpStatus.NOT_FOUND, "Danh mục không tồn tại hoặc đã ẩn."),
    PRODUCT_UNAVAILABLE("GNE-CAT-4042", HttpStatus.NOT_FOUND, "Sản phẩm không còn bán."),
    CATEGORY_DUPLICATE("GNE-CAT-4091", HttpStatus.CONFLICT, "Tên hoặc đường dẫn danh mục đã tồn tại."),
    CATEGORY_NOT_DELETABLE("GNE-CAT-4092", HttpStatus.CONFLICT, "Danh mục còn sản phẩm hoặc danh mục con."),
    ATTRIBUTE_CODE_DUPLICATE("GNE-CAT-4095", HttpStatus.CONFLICT, "Mã thuộc tính đã có trong ngành này."),
    // ----- Đăng bán sản phẩm (API_SPEC 10.5, 10.6, 15 — UC-CAT-09…11, 17: plan C1 D16) -----
    PRODUCT_LOCKED("GNE-CAT-4093", HttpStatus.CONFLICT, "Sản phẩm đang bị sàn khóa."),
    PRODUCT_HAS_OPEN_ORDERS("GNE-CAT-4094", HttpStatus.CONFLICT, "Sản phẩm còn đơn đang xử lý."),
    PRODUCT_UNDER_REVIEW("GNE-CAT-4096", HttpStatus.CONFLICT,
            "Sản phẩm đang chờ duyệt. Bạn chỉ có thể xem thông tin cho tới khi có kết quả."),
    PRODUCT_STATE_CONFLICT("GNE-CAT-4097", HttpStatus.CONFLICT, "Trạng thái hiện tại không cho phép thao tác này."),
    CATEGORY_PROPOSAL_LIMIT("GNE-CAT-4221", HttpStatus.UNPROCESSABLE_ENTITY, "Bạn đã có 5 đề xuất danh mục đang chờ duyệt."),
    PRODUCT_DOCUMENTS_REQUIRED("GNE-CAT-4222", HttpStatus.UNPROCESSABLE_ENTITY, "Ngành hàng này bắt buộc giấy tờ chứng thực."),
    PRODUCT_SPECS_MISSING("GNE-CAT-4223", HttpStatus.UNPROCESSABLE_ENTITY, "Còn thiếu thông số bắt buộc của ngành hàng."),
    PICKUP_ADDRESS_REQUIRED("GNE-CAT-4224", HttpStatus.UNPROCESSABLE_ENTITY,
            "Shop chưa cấu hình kho lấy hàng — vào Cài đặt › Kho lấy hàng trước khi gửi duyệt sản phẩm."),

    // ===== Tương tác — theo dõi shop (API_SPEC 3.11, 15 — UC-SOC-06) =====
    CANNOT_FOLLOW_OWN_SHOP("GNE-SOC-4095", HttpStatus.CONFLICT, "Không thể theo dõi shop của chính mình."),

    // ===== Mở shop & KYC người bán (API_SPEC 0.9, 9, 10.11, 10.12, 15 — UC-VND-01/02/05/07/21) =====
    SHOP_NOT_VERIFIED("GNE-VND-4031", HttpStatus.FORBIDDEN, "Shop chưa được xác minh (KYC)."),
    SHOP_RENAME_TOO_SOON("GNE-VND-4091", HttpStatus.CONFLICT, "Chỉ được đổi tên shop 1 lần trong 30 ngày."),
    SHOP_ALREADY_EXISTS("GNE-VND-4093", HttpStatus.CONFLICT, "Tài khoản đã có shop."),
    SHOP_NAME_TAKEN("GNE-VND-4094", HttpStatus.CONFLICT, "Tên shop đã được dùng."),
    KYC_NOT_EDITABLE("GNE-VND-4095", HttpStatus.CONFLICT, "Hồ sơ KYC đang được xử lý hoặc đã được duyệt."),
    KYC_REVIEW_ALREADY_REQUESTED("GNE-VND-4096", HttpStatus.CONFLICT, "Bạn đã gửi yêu cầu xem xét hồ sơ KYC."),
    CARRIER_REQUIRED("GNE-VND-4221", HttpStatus.UNPROCESSABLE_ENTITY, "Phải bật ít nhất 1 hãng vận chuyển."),
    AUTO_REPLY_EMPTY("GNE-VND-4222", HttpStatus.UNPROCESSABLE_ENTITY, "Bật tự trả lời thì cần có nội dung."),
    KYC_DOCUMENTS_MISSING("GNE-VND-4223", HttpStatus.UNPROCESSABLE_ENTITY, "Hồ sơ KYC còn thiếu giấy tờ."),
    PAYOUT_PENDING_WITHDRAWAL("GNE-WAL-4091", HttpStatus.CONFLICT, "Còn lệnh rút đang chờ, chưa thể xóa tài khoản nhận tiền."),
    PAYOUT_HOLDER_MISMATCH("GNE-WAL-4222", HttpStatus.UNPROCESSABLE_ENTITY, "Tên chủ tài khoản không khớp với CCCD."),
    GHN_SYNC_FAILED("GNE-LOG-5022", HttpStatus.BAD_GATEWAY, "GHN đang lỗi khi đồng bộ kho lấy hàng. Vui lòng thử lại."),

    // ===== Banner trang chủ (API_SPEC 11.16, 15 — UC-ADM-12) =====
    BANNER_NOT_DELETABLE("GNE-BNR-4091", HttpStatus.CONFLICT, "Không thể xóa banner đang hiển thị hoặc đã lên lịch — hãy tắt trước."),
    BANNER_SLOT_FULL("GNE-BNR-4221", HttpStatus.UNPROCESSABLE_ENTITY, "Vị trí này đã đủ 8 banner đang hiển thị."),
    BANNER_TARGET_FORBIDDEN("GNE-BNR-4222", HttpStatus.UNPROCESSABLE_ENTITY, "Liên kết banner phải là đường dẫn trong sàn."),

    // ===== Điểm Genie (API_SPEC 3.13, 15 — UC-PRO-08, UC-PRO-09) =====
    ALREADY_CHECKED_IN("GNE-PRO-4091", HttpStatus.CONFLICT, "Hôm nay bạn đã điểm danh rồi."),
    REWARD_ALREADY_HELD("GNE-PRO-4092", HttpStatus.CONFLICT, "Bạn đang có voucher này trong Kho Voucher."),
    INSUFFICIENT_COINS("GNE-PRO-4222", HttpStatus.UNPROCESSABLE_ENTITY, "Bạn không đủ Điểm để thực hiện."),

    // ===== Gian hàng — trang shop công khai (API_SPEC 5.11, 15 — UC-CAT-16) =====
    SHOP_UNAVAILABLE("GNE-VND-4041", HttpStatus.NOT_FOUND, "Gian hàng không tồn tại hoặc đã ngừng hoạt động."),

    // ===== Đơn hàng / tồn kho (giữ từ khung ban đầu) =====
    OUT_OF_STOCK("GNE-INV-4090", HttpStatus.CONFLICT, "Sản phẩm đã hết hàng."),
    FLASH_SALE_SOLD_OUT("GNE-INV-4091", HttpStatus.CONFLICT, "Suất Flash Sale đã hết."),
    /** Thanh toán lại sau khi giữ chỗ 15 phút đã hết (API 6.12) — plan C2 O01.04. */
    RESERVATION_EXPIRED("GNE-INV-4093", HttpStatus.CONFLICT, "Thời gian giữ hàng 15 phút đã hết."),
    /** Số lượng trong giỏ vượt tồn khả dụng — {@code details.stock} (API 6.3, 6.4) — plan C2 O01.04. */
    CART_QUANTITY_EXCEEDS_STOCK("GNE-INV-4094", HttpStatus.CONFLICT, "Số lượng vượt quá tồn kho hiện có."),
    ORDER_CANNOT_CANCEL("GNE-ORD-4092", HttpStatus.CONFLICT, "Đơn hàng không thể hủy ở trạng thái hiện tại."),
    /** Shop đang tạm nghỉ — giỏ: không chi tiết; đặt hàng: {@code details.shop_ids} (API 6.3, 6.10) — plan C2 O01.04. */
    SHOP_ON_VACATION("GNE-ORD-4093", HttpStatus.CONFLICT, "Shop đang tạm nghỉ, chưa nhận đơn."),
    /** Người mua là chủ shop của sản phẩm (API 6.3) — plan C2 O01.04. */
    OWN_SHOP_PRODUCT("GNE-ORD-4094", HttpStatus.CONFLICT, "Bạn không thể mua sản phẩm của chính shop mình."),
    /** Tài khoản nội bộ (STAFF / ADMIN) gọi giỏ hàng / đặt hàng — plan B1 D16 (hướng B). */
    INTERNAL_ACCOUNT_CANNOT_BUY("GNE-ORD-4031", HttpStatus.FORBIDDEN,
            "Tài khoản nhân viên / quản trị không thể mua hàng — vui lòng dùng tài khoản khách hàng cá nhân.");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
