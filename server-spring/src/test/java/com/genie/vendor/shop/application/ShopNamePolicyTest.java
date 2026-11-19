package com.genie.vendor.shop.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import org.junit.jupiter.api.Test;

/** Luật tên shop (plan Mở shop & KYC K03.01). */
class ShopNamePolicyTest {

    private final VendorProfileRepository repo = mock(VendorProfileRepository.class);
    private final ShopNamePolicy policy = new ShopNamePolicy(repo);

    @Test
    void normalizesWhitespaceAndChecksLength() {
        assertThat(policy.validate("  Tech   Zone  ", null)).isEqualTo("Tech Zone");
        assertThatThrownBy(() -> policy.validate(" ab ", null)).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
        assertThatThrownBy(() -> policy.validate("x".repeat(101), null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void bannedPhrasesMatchWholeWordsAfterRemovingDiacritics() {
        assertThat(policy.containsBannedWord("Hàng Giả Giá Rẻ")).isTrue();
        assertThat(policy.containsBannedWord("Genie   Official Store")).isTrue();
        assertThat(policy.containsBannedWord("Shop Lừa-Đảo")).isTrue();
        assertThat(policy.containsBannedWord("Đồ Lớn Giá Tốt")).isFalse();         // "lớn" không phải từ cấm
        assertThat(policy.containsBannedWord("Hàng Giảm Giá")).isFalse();          // "giam" ≠ "gia"
        assertThatThrownBy(() -> policy.validate("Hàng giả 24h", null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHOP_NAME_TAKEN);
    }

    @Test
    void duplicateNameIsTaken() {
        when(repo.existsByShopNameNormalized(eq("TechZone"), any())).thenReturn(true);
        assertThatThrownBy(() -> policy.validate("TechZone", null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHOP_NAME_TAKEN);
    }
}
