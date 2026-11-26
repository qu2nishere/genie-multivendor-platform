package com.genie.shipping.fee.application;

import com.genie.common.cache.JsonCache;
import com.genie.shipping.shipment.domain.VendorShippingConfig;
import com.genie.shipping.shipment.domain.VendorShippingConfigRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính phí vận chuyển một kiện — UC-ORD-06 (plan C2 3.2, D4–D7). Cửa của module {@code shipping} cho {@code order}.
 * <ol>
 *   <li>Freeship (D7): tạm tính ≥ ngưỡng của shop, hoặc mọi món người bán trả ship → khách trả 0, vẫn tính phí thật.</li>
 *   <li>Shop chưa có kho lấy hàng (D5) → bảng dự phòng, {@code estimated = true}.</li>
 *   <li>{@code GHN_MODE=stub} (không có {@link GhnFeeClient}) → bảng phí giả lập, {@code estimated = false}.</li>
 *   <li>{@code GHN_MODE=http}: shop chưa có mã shop GHN hoặc địa chỉ thiếu mã phường → bảng dự phòng; còn lại hỏi GHN
 *       theo địa giới 2 cấp ({@code /v2/shipping-order/preview} — spike D4, O11.01; cache 10 phút), lỗi / timeout →
 *       bảng dự phòng, không phục vụ tuyến → {@code unavailable}.</li>
 * </ol>
 */
@Component
public class ShippingFeeQuoter {

    private static final Logger log = LoggerFactory.getLogger(ShippingFeeQuoter.class);
    static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final VendorShippingConfigRepository configs;
    private final ObjectProvider<GhnFeeClient> ghn;
    private final JsonCache cache;

    public ShippingFeeQuoter(VendorShippingConfigRepository configs, ObjectProvider<GhnFeeClient> ghn, JsonCache cache) {
        this.configs = configs;
        this.ghn = ghn;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    public ShippingQuote quote(Parcel p) {
        Long threshold = p.allSellerPays() ? Long.valueOf(0L) : threshold(p.vendorUserId());
        boolean freeByShop = threshold != null && p.subtotal() >= threshold;
        Fee fee = carrierFee(p);
        if (fee.unavailable()) {
            return new ShippingQuote(0, 0, false, false, true, threshold);
        }
        return new ShippingQuote(fee.amount(), freeByShop ? 0 : fee.amount(), freeByShop, fee.estimated(), false,
                threshold);
    }

    private record Fee(long amount, boolean estimated, boolean unavailable) {
    }

    private Fee carrierFee(Parcel p) {
        long flat = FlatFeeTable.fee(p.fromProvinceId(), p.toProvinceId());
        if (p.fromProvinceId() == null) {
            return new Fee(flat, true, false);
        }
        GhnFeeClient client = ghn.getIfAvailable();
        if (client == null) {
            return new Fee(flat, false, false);
        }
        if (p.ghnShopId() == null || p.toWardCode() == null || p.toWardCode().isBlank()) {
            return new Fee(flat, true, false);
        }
        try {
            Long amount = cache.getOrLoad("shipping:fee:" + hash(p), cache.type(Long.class), CACHE_TTL,
                    () -> client.fee(p));
            return new Fee(amount, false, false);
        } catch (GhnFeeClient.RouteNotSupportedException e) {
            return new Fee(0, false, true);
        } catch (RuntimeException e) {
            log.warn("GHN tính phí lỗi cho shop {} → dùng bảng phí dự phòng: {}", p.vendorUserId(), e.getMessage());
            return new Fee(flat, true, false);
        }
    }

    /** Ngưỡng freeship của shop (VND, làm tròn lên); không bật / ≤ 0 → NULL. */
    private Long threshold(long vendorUserId) {
        Optional<BigDecimal> t = configs.findByVendorId(vendorUserId).map(VendorShippingConfig::getFreeshipThreshold);
        return t.filter(v -> v.signum() > 0).map(v -> v.setScale(0, java.math.RoundingMode.CEILING).longValueExact())
                .orElse(null);
    }

    /** Khóa cache = SHA-256 các tham số ảnh hưởng tới phí (không đưa tên / SĐT / địa chỉ người nhận vào khóa). */
    static String hash(Parcel p) {
        String raw = String.join("|", String.valueOf(p.ghnShopId()), p.toWardCode(), String.valueOf(p.weightGrams()),
                String.valueOf(p.lengthCm()), String.valueOf(p.widthCm()), String.valueOf(p.heightCm()),
                String.valueOf(p.subtotal()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
