package com.genie.promotion.coin.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.promotion.coin.domain.CoinTransactionType;
import com.genie.promotion.coin.domain.DailyCheckin;
import com.genie.promotion.coin.domain.DailyCheckinRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Điểm danh nhận Điểm — UC-PRO-08 (API_SPEC 3.13; plan Điểm Genie P03.02). Chống cộng 2 lần: khóa ngắn Redis
 * {@code lock:checkin:{userId}} (5 giây) + khóa dòng {@code daily_checkins} + {@code DailyCheckin.checkIn} kiểm ngày (giờ VN).
 * Cộng Điểm, ghi sổ, cập nhật chuỗi trong MỘT transaction (Điểm là tiền — N2).
 */
@Service
public class CheckInService {

    static final Duration LOCK_TTL = Duration.ofSeconds(5);

    private final DailyCheckinRepository checkins;
    private final CoinLedgerService ledger;
    private final StringRedisTemplate redis;
    private final TransactionTemplate tx;
    private final Clock clock;

    public CheckInService(DailyCheckinRepository checkins, CoinLedgerService ledger, StringRedisTemplate redis,
                          TransactionTemplate tx, Clock clock) {
        this.checkins = checkins;
        this.ledger = ledger;
        this.redis = redis;
        this.tx = tx;
        this.clock = clock;
    }

    /** Kết quả điểm danh (API 3.13): thưởng, số dư mới, ô chuỗi hiện tại. */
    public record CheckInResult(int reward, int balance, int streakDays) {
    }

    public CheckInResult checkIn(long userId) {
        String lockKey = "lock:checkin:" + userId;
        Boolean acquired;
        try {
            acquired = redis.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);   // không có khóa thì không ghi Điểm
        }
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);   // đang xử lý lần bấm trước
        }
        try {
            return tx.execute(s -> doCheckIn(userId));
        } catch (DataIntegrityViolationException e) {
            // hai lần điểm danh ĐẦU TIÊN cùng lúc (khóa Redis vừa hết hạn): uk_daily_checkin_customer chặn bản thứ hai
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);
        } finally {
            try {
                redis.delete(lockKey);
            } catch (DataAccessException ignored) {
                // khóa tự hết hạn sau 5 giây
            }
        }
    }

    private CheckInResult doCheckIn(long userId) {
        OffsetDateTime now = CoinTime.nowVn(clock);
        DailyCheckin d = checkins.findByCustomerIdForUpdate(userId).orElseGet(() -> DailyCheckin.firstFor(userId));
        int reward;
        try {
            reward = d.checkIn(now);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);
        }
        checkins.saveAndFlush(d);
        int day = CoinTime.streakDays(d, now);
        CoinLedgerService.Entry entry = ledger.credit(userId, reward, CoinTransactionType.CHECK_IN, null,
                "Điểm danh ngày " + day, now);
        return new CheckInResult(reward, entry.balance(), day);
    }
}
