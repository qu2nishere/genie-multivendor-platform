package com.genie.vendor.shop.application;

import com.genie.common.domain.NameNormalizer;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Luật tên shop (UC-VND-01, UC-VND-21; plan Mở shop & KYC K03.01):
 * <ul>
 *   <li>chuẩn hóa: bỏ khoảng trắng đầu / cuối, gộp khoảng trắng liên tiếp;</li>
 *   <li>dài 3–100 ký tự → khác thì {@code 422 GNE-VAL-4220};</li>
 *   <li>chứa cụm từ cấm ({@code resources/vendor/banned-words.txt}, so nguyên từ sau khi bỏ dấu) hoặc trùng tên shop khác
 *       (không phân biệt hoa thường — index {@code uk_vendor_profiles_shop_name_lower}) → {@code 409 GNE-VND-4094}.</li>
 * </ul>
 */
@Component
public class ShopNamePolicy {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 100;

    private final VendorProfileRepository profiles;
    private final List<String> banned;

    public ShopNamePolicy(VendorProfileRepository profiles) {
        this.profiles = profiles;
        this.banned = loadBannedWords();
    }

    /** {@code "  Tech   Zone "} → {@code "Tech Zone"}. */
    public static String normalize(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ");
    }

    /** Chuẩn hóa + kiểm độ dài, từ cấm, trùng tên (trừ shop {@code exceptShopId}); trả tên đã chuẩn hóa. */
    public String validate(String rawName, Long exceptShopId) {
        String name = normalize(rawName);
        if (name.length() < MIN_LENGTH || name.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Tên shop phải dài 3–100 ký tự.",
                    Map.of("field", "shop_name"));
        }
        if (containsBannedWord(name)) {
            throw new BusinessException(ErrorCode.SHOP_NAME_TAKEN, "Tên shop chứa từ không được phép.",
                    Map.of("reason", "BANNED_WORD"));
        }
        if (profiles.existsByShopNameNormalized(name, exceptShopId)) {
            throw new BusinessException(ErrorCode.SHOP_NAME_TAKEN, Map.of("reason", "DUPLICATE"));
        }
        return name;
    }

    boolean containsBannedWord(String name) {
        String padded = " " + NameNormalizer.normalize(name).replaceAll("[^A-Z0-9 ]", " ").replaceAll("\\s+", " ") + " ";
        for (String word : banned) {
            if (padded.contains(" " + word + " ")) {
                return true;
            }
        }
        return false;
    }

    private static List<String> loadBannedWords() {
        try (InputStream in = new ClassPathResource("vendor/banned-words.txt").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .map(NameNormalizer::normalize)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được vendor/banned-words.txt", e);
        }
    }
}
