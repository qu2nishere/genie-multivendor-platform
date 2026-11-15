package com.genie.common.mail;

/**
 * Nhận diện thương hiệu dùng chung cho mọi email (đặt trong code — không thêm vào {@code application*.yml}).
 *
 * <h2>Ảnh trong email</h2>
 * Ảnh <b>đính kèm trực tiếp</b> vào email (inline, {@code cid:}) từ {@code resources/templates/email/img/} — hiện được
 * trên Gmail / Outlook kể cả khi server chạy {@code localhost}, không cần URL công khai. {@code SmtpEmailSender} chỉ
 * đính kèm ảnh mà mẫu email thật sự dùng.
 * <ul>
 *   <li>Banner: hiển thị {@value #BANNER_WIDTH} × {@value #BANNER_HEIGHT} px; tệp {@code banner.jpg} 1200 × 400 px
 *       (bản tối ưu từ {@code web/src/assets/brand/BannerEmail.png} — JPG ~25 KB). Thiết kế mới: 1200 × 400 px, 3 : 1.</li>
 *   <li>Đèn thần + đồng xu góc phải cuối email: {@code genie-light.png} (từ {@code GenieLight.png}, nền trong suốt),
 *       hiển thị rộng {@value #LIGHT_WIDTH} px.</li>
 * </ul>
 */
public final class MailBranding {

    /** Content-ID ảnh đính kèm + đường dẫn tệp trong classpath. */
    public static final String BANNER_CID = "genie-banner";
    public static final String BANNER_FILE = "templates/email/img/banner.jpg";
    public static final String LIGHT_CID = "genie-light";
    public static final String LIGHT_FILE = "templates/email/img/genie-light.png";

    /** {@code src} của banner trong mẫu; đổi thành URL https công khai nếu muốn tải từ CDN thay vì đính kèm. */
    public static final String BANNER_URL = "cid:" + BANNER_CID;
    public static final String LIGHT_URL = "cid:" + LIGHT_CID;
    public static final int LIGHT_WIDTH = 148;

    public static final int BANNER_WIDTH = 600;
    public static final int BANNER_HEIGHT = 200;

    /** Tên hiển thị trong email. */
    public static final String SHOP_NAME = "Genie Shop";

    /** Xanh lá thương hiệu (theme web {@code --color-primary} #1f7356). */
    public static final String BRAND_COLOR = "#1F7356";

    /** Nền xanh nhạt để làm nổi bật (tên khách, ô mã OTP). */
    public static final String BRAND_TINT = "#E8F5EF";

    private MailBranding() {
    }
}
