package com.genie.promotion.coin.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.NotFoundException;
import com.genie.promotion.coin.domain.CoinTransactionType;
import com.genie.promotion.coin.domain.UserCoinRepository;
import com.genie.promotion.voucher.application.VoucherView;
import com.genie.promotion.voucher.domain.UserVoucher;
import com.genie.promotion.voucher.domain.UserVoucherRepository;
import com.genie.promotion.voucher.domain.Voucher;
import com.genie.promotion.voucher.domain.VoucherUsageRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đổi Điểm lấy voucher sàn — UC-PRO-09 bước 4 (API_SPEC 3.13; plan Điểm Genie P05.03, D4). Khóa ngắn Redis
 * {@code lock:coin:{userId}} chống bấm 2 lần; trong một transaction: khóa ví Điểm → kiểm Kho Voucher → trừ Điểm
 * ({@code VOUCHER_REDEEM}) → thêm voucher vào Kho.
 */
@Service
public class CoinRedemptionService {

    static final Duration LOCK_TTL = Duration.ofSeconds(5);

    private final CoinRewardCatalog catalog;
    private final CoinLedgerService ledger;
    private final UserCoinRepository coins;
    private final UserVoucherRepository userVouchers;
    private final VoucherUsageRepository usages;
    private final StringRedisTemplate redis;
    private final TransactionTemplate tx;
    private final Clock clock;

    public CoinRedemptionService(CoinRewardCatalog catalog, CoinLedgerService ledger, UserCoinRepository coins,
                                 UserVoucherRepository userVouchers, VoucherUsageRepository usages,
                                 StringRedisTemplate redis, TransactionTemplate tx, Clock clock) {
        this.catalog = catalog;
        this.ledger = ledger;
        this.coins = coins;
        this.userVouchers = userVouchers;
        this.usages = usages;
        this.redis = redis;
        this.tx = tx;
        this.clock = clock;
    }

    /** {@code {voucher: $Voucher, balance}}. */
    public record RedemptionResult(VoucherView voucher, int balance) {
    }

    public RedemptionResult redeem(long userId, long rewardId) {
        String lockKey = "lock:coin:" + userId;
        Boolean acquired;
        try {
            acquired = redis.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);   // đang xử lý lần đổi trước của chính người này
        }
        try {
            return tx.execute(s -> doRedeem(userId, rewardId));
        } catch (DataIntegrityViolationException e) {
            // hai lần đổi cùng quà lọt qua khóa: uk (user_id, voucher_id) chặn bản thứ hai → rollback cả phần trừ Điểm
            throw new BusinessException(ErrorCode.REWARD_ALREADY_HELD);
        } finally {
            try {
                redis.delete(lockKey);
            } catch (DataAccessException ignored) {
                // khóa tự hết hạn
            }
        }
    }

    private RedemptionResult doRedeem(long userId, long rewardId) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        CoinRewardCatalog.Reward reward = catalog.find(rewardId)
                .orElseThrow(() -> new NotFoundException("Không có quà đổi Điểm " + rewardId));
        Voucher voucher = reward.voucher();
        coins.findByIdForUpdate(userId);   // xếp hàng theo ví Điểm trước khi kiểm Kho Voucher
        var held = userVouchers.findByUserIdAndVoucherId(userId, voucher.getId());
        if (held.isPresent()) {
            boolean usedUp = voucher.getUsagePerUser() != null
                    && usages.countByUserIdAndVoucherId(userId, voucher.getId()) >= voucher.getUsagePerUser();
            throw usedUp
                    ? new BusinessException(ErrorCode.REWARD_ALREADY_HELD, "Bạn đã dùng hết lượt của voucher này.",
                    Map.of("reason", "USAGE_LIMIT_REACHED"))
                    : new BusinessException(ErrorCode.REWARD_ALREADY_HELD);
        }
        CoinLedgerService.Entry entry = ledger.debit(userId, reward.coinCost(), CoinTransactionType.VOUCHER_REDEEM,
                String.valueOf(voucher.getId()), "Đổi voucher " + voucher.getCode(), now);
        userVouchers.saveAndFlush(UserVoucher.builder().userId(userId).voucherId(voucher.getId()).voucher(voucher).build());
        return new RedemptionResult(VoucherView.of(voucher, now, false, true), entry.balance());
    }
}
