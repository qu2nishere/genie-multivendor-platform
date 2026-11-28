// Bản cài giả lập sàng lọc sản phẩm bằng luật từ khóa — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.4)
package com.genie.catalog.moderation.infra;

import com.genie.catalog.moderation.application.ProductScreeningClient;
import com.genie.catalog.moderation.application.ScreeningRequest;
import com.genie.catalog.moderation.application.ScreeningVerdict;
import com.genie.catalog.moderation.domain.ModerationDecision;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Bản mặc định khi {@code GEMINI_MODE=stub} (hoặc không khai biến — chạy local / CI không cần khóa API).
 * So khớp <b>nguyên từ</b> trên {@code name + " " + descriptionText} <b>giữ dấu</b> (chỉ chuẩn hóa NFC + chữ thường):
 * bỏ dấu rồi tìm chuỗi con sẽ bắt nhầm "thuộc" (→ thuoc), "bổ sung" (→ sung), "hàng giá" (→ hang gia) — sửa 2026-11-28.
 */
@Component
@ConditionalOnProperty(name = "GEMINI_MODE", havingValue = "stub", matchIfMissing = true)
public class StubScreeningClient implements ProductScreeningClient {

    // Từ có dấu chỉ khớp đúng chữ có dấu ("thuốc" ≠ "thuộc"); bản không dấu chỉ giữ cho từ không gây nhầm
    private static final List<Pattern> COUNTERFEIT_KEYWORDS = words(
            "hàng nhái", "hang nhai", "hàng giả", "fake", "replica", "super fake");
    private static final List<Pattern> PROHIBITED_KEYWORDS = words(
            "thuốc", "vũ khí", "súng", "đạn dược");
    private static final List<Pattern> PRICE_ANOMALY_KEYWORDS = words(
            "bán rẻ", "giá sốc", "ban re", "gia soc");

    @Override
    public ScreeningVerdict screen(ScreeningRequest request) {
        String combined = (request.name() == null ? "" : request.name()) + " "
                + (request.descriptionText() == null ? "" : request.descriptionText());
        String normalized = Normalizer.normalize(combined, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);

        if (containsAny(normalized, COUNTERFEIT_KEYWORDS)) {
            return new ScreeningVerdict(
                    ModerationDecision.REJECT,
                    90,
                    List.of("COUNTERFEIT"),
                    "Mô tả có dấu hiệu hàng giả / hàng nhái.",
                    null,
                    "stub");
        }
        if (containsAny(normalized, PROHIBITED_KEYWORDS)) {
            return new ScreeningVerdict(
                    ModerationDecision.NEED_REVIEW,
                    60,
                    List.of("PROHIBITED_ITEM"),
                    "Có thể là hàng cấm / hạn chế kinh doanh — cần người duyệt kiểm tra.",
                    null,
                    "stub");
        }
        if (containsAny(normalized, PRICE_ANOMALY_KEYWORDS) && request.minPrice() < 1_000L) {
            return new ScreeningVerdict(
                    ModerationDecision.REJECT,
                    70,
                    List.of("PRICE_ANOMALY"),
                    "Giá bán bất thường so với loại hàng.",
                    null,
                    "stub");
        }
        return new ScreeningVerdict(
                ModerationDecision.APPROVE,
                10,
                List.of(),
                "Không phát hiện vi phạm.",
                null,
                "stub");
    }

    private static boolean containsAny(String haystack, List<Pattern> needles) {
        for (Pattern n : needles) {
            if (n.matcher(haystack).find()) return true;
        }
        return false;
    }

    /** Mỗi từ khóa thành mẫu "nguyên từ": trước / sau không phải chữ hay số (Unicode — chữ có dấu tính là chữ). */
    private static List<Pattern> words(String... keywords) {
        return java.util.Arrays.stream(keywords)
                .map(k -> Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(Normalizer.normalize(k, Normalizer.Form.NFC))
                        + "(?![\\p{L}\\p{N}])"))
                .toList();
    }
}
