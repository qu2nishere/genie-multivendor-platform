package com.genie.promotion.coin.application;

import com.genie.promotion.voucher.domain.Voucher;
import com.genie.promotion.voucher.domain.VoucherRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục quà đổi Điểm (UC-PRO-09 bước 4; plan Điểm Genie D3, P05.01): cấu hình {@code genie.coin.rewards} trỏ tới voucher sàn
 * theo mã. Quà có voucher không tồn tại / đã tắt / hết hạn bị bỏ qua (cảnh báo lúc khởi động).
 */
@Component
public class CoinRewardCatalog {

    private static final Logger log = LoggerFactory.getLogger(CoinRewardCatalog.class);

    private final CoinProperties properties;
    private final VoucherRepository vouchers;
    private final Clock clock;

    public CoinRewardCatalog(CoinProperties properties, VoucherRepository vouchers, Clock clock) {
        this.properties = properties;
        this.vouchers = vouchers;
        this.clock = clock;
    }

    /** Một quà còn đổi được: cấu hình + voucher thật. */
    public record Reward(long id, int coinCost, Voucher voucher) {
    }

    /** Dạng trả cho client (API 3.13). */
    public record RewardView(long id, int coinCost, long voucherValue, long minOrderValue) {
    }

    @Transactional(readOnly = true)
    public List<Reward> available() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Map<String, Voucher> byCode = vouchers.findByCodeIn(properties.rewards().stream()
                        .map(CoinProperties.Reward::voucherCode).toList())
                .stream().collect(Collectors.toMap(Voucher::getCode, Function.identity()));
        List<Reward> out = new ArrayList<>();
        for (CoinProperties.Reward r : properties.rewards()) {
            Voucher v = byCode.get(r.voucherCode());
            if (v != null && Boolean.TRUE.equals(v.getIsActive()) && now.isBefore(v.getValidTo())) {
                out.add(new Reward(r.id(), r.coinCost(), v));
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Optional<Reward> find(long rewardId) {
        return available().stream().filter(r -> r.id() == rewardId).findFirst();
    }

    @Transactional(readOnly = true)
    public List<RewardView> list() {
        return available().stream().map(r -> new RewardView(r.id(), r.coinCost(),
                r.voucher().getDiscountValue().longValue(), r.voucher().getMinOrderValue().longValue())).toList();
    }

    /** Cảnh báo cấu hình quà trỏ tới voucher không dùng được (vd. chưa nạp dữ liệu mẫu). */
    @EventListener(ApplicationReadyEvent.class)
    public void warnMissingVouchers() {
        try {
            List<Long> ok = available().stream().map(Reward::id).toList();
            properties.rewards().stream().filter(r -> !ok.contains(r.id())).forEach(r -> log.warn(
                    "Quà đổi Điểm {} trỏ tới voucher '{}' không tồn tại / đã tắt / hết hạn — bị ẩn khỏi danh mục",
                    r.id(), r.voucherCode()));
        } catch (RuntimeException e) {
            log.warn("Không kiểm được danh mục quà đổi Điểm: {}", e.getMessage());
        }
    }
}
