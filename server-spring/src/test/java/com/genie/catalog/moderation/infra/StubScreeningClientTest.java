// Unit test bản giả lập sàng lọc sản phẩm — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.4)
package com.genie.catalog.moderation.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.moderation.application.ScreeningRequest;
import com.genie.catalog.moderation.application.ScreeningVerdict;
import com.genie.catalog.moderation.domain.ModerationDecision;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StubScreeningClientTest {

    private final StubScreeningClient client = new StubScreeningClient();

    private static ScreeningRequest req(String name, String desc, long minPrice) {
        return new ScreeningRequest(1L, name, "Điện tử › Tai nghe", "Sony", "NEW",
                minPrice, minPrice, desc, Map.of(), List.of("Đen"), List.of(), List.of(), 0);
    }

    @Test
    void rejectsCounterfeitKeywordsIncludingAccentedVietnamese() {
        ScreeningVerdict v = client.screen(req("Túi xách Hàng Nhái cao cấp", "Đẹp như thật", 500_000));
        assertThat(v.decision()).isEqualTo(ModerationDecision.REJECT);
        assertThat(v.riskScore()).isEqualTo(90);
        assertThat(v.violations()).containsExactly("COUNTERFEIT");
        assertThat(v.modelName()).isEqualTo("stub");
        assertThat(v.rawResponse()).isNull();
    }

    @Test
    void flagsProhibitedKeywordsForManualReview() {
        ScreeningVerdict v = client.screen(req("Thực phẩm bổ sung", "Có chứa thuốc đặc trị", 200_000));
        assertThat(v.decision()).isEqualTo(ModerationDecision.NEED_REVIEW);
        assertThat(v.riskScore()).isEqualTo(60);
        assertThat(v.violations()).containsExactly("PROHIBITED_ITEM");
    }

    @Test
    void rejectsPriceAnomalyBelowThresholdWhenPriceTooLow() {
        ScreeningVerdict v = client.screen(req("Điện thoại giá sốc hôm nay", "Xả kho", 500));
        assertThat(v.decision()).isEqualTo(ModerationDecision.REJECT);
        assertThat(v.riskScore()).isEqualTo(70);
        assertThat(v.violations()).containsExactly("PRICE_ANOMALY");
    }

    @Test
    void approvesCleanListing() {
        ScreeningVerdict v = client.screen(req("Tai nghe Bluetooth chính hãng", "Bảo hành 12 tháng", 1_200_000));
        assertThat(v.decision()).isEqualTo(ModerationDecision.APPROVE);
        assertThat(v.riskScore()).isEqualTo(10);
        assertThat(v.violations()).isEmpty();
    }

    @Test
    void doesNotFlagCommonWordsThatOnlyLookAlikeWithoutAccents() {
        // "thuộc", "bổ sung", "hàng giá tốt" từng bị bắt nhầm khi bỏ dấu rồi tìm chuỗi con
        ScreeningVerdict v = client.screen(req("Tai nghe thuộc dòng cao cấp",
                "Bổ sung thêm đệm tai, hàng giá tốt, súng bắn keo không kèm theo", 1_200_000));
        assertThat(v.decision()).isEqualTo(ModerationDecision.NEED_REVIEW);   // "súng" là nguyên từ → vẫn bắt
        ScreeningVerdict clean = client.screen(req("Tai nghe thuộc dòng cao cấp",
                "Bổ sung thêm đệm tai, hàng giá tốt", 1_200_000));
        assertThat(clean.decision()).isEqualTo(ModerationDecision.APPROVE);
    }

    @Test
    void matchesWholeWordsOnly() {
        // "fakeless" / "replicant" không phải từ khóa
        assertThat(client.screen(req("Áo fakeless replicant", "Vải cotton", 300_000)).decision())
                .isEqualTo(ModerationDecision.APPROVE);
        assertThat(client.screen(req("Áo FAKE loại 1", "Vải cotton", 300_000)).decision())
                .isEqualTo(ModerationDecision.REJECT);
    }
}
