package com.genie.promotion.coin.application;

import com.genie.promotion.coin.domain.CoinTransactionRepository;
import com.genie.promotion.coin.domain.CoinTransactionType;
import com.genie.promotion.coin.domain.UserCoinRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Trừ Điểm của các lô đã quá hạn 12 tháng (UC-PRO-09 ràng buộc; LLD 10; plan Điểm Genie D5, P06.03) — 00:15 giờ Việt Nam
 * hằng ngày. Mỗi người một transaction: khóa ví {@code FOR UPDATE SKIP LOCKED} (đang bị khóa thì để lượt sau) → với từng lô
 * đã quá hạn còn Điểm (FEFO) ghi một dòng {@code EXPIRED} âm, {@code reference_id} = id lô. Chạy lại không trừ thêm.
 */
@Component
public class CoinExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(CoinExpiryJob.class);

    private final CoinTransactionRepository transactions;
    private final UserCoinRepository coins;
    private final CoinLedgerService ledger;
    private final TransactionTemplate tx;
    private final Clock clock;

    public CoinExpiryJob(CoinTransactionRepository transactions, UserCoinRepository coins, CoinLedgerService ledger,
                         TransactionTemplate tx, Clock clock) {
        this.transactions = transactions;
        this.coins = coins;
        this.ledger = ledger;
        this.tx = tx;
        this.clock = clock;
    }

    @Scheduled(cron = "0 15 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void scheduled() {
        int expired = run();
        if (expired > 0) {
            log.info("Đã trừ {} Điểm hết hạn", expired);
        }
    }

    /** @return tổng số Điểm đã trừ ở lượt này */
    public int run() {
        OffsetDateTime now = CoinTime.nowVn(clock);
        int total = 0;
        for (Long userId : transactions.findUsersWithExpiredLots(now)) {
            try {
                Integer n = tx.execute(s -> expireFor(userId, now));
                total += n == null ? 0 : n;
            } catch (RuntimeException e) {
                // một người lỗi (vd. băm lệch — D8) không chặn người khác; lượt sau thử lại
                log.warn("Không trừ được Điểm hết hạn của user {}: {}", userId, e.getMessage());
            }
        }
        return total;
    }

    private int expireFor(long userId, OffsetDateTime now) {
        if (coins.lockSkipLocked(userId).isEmpty()) {
            return 0;   // không có ví hoặc đang được giao dịch khác giữ — để lượt sau
        }
        int expired = 0;
        List<CoinLots.Remaining> lots = ledger.remainingLots(userId);
        for (CoinLots.Remaining lot : lots) {
            if (lot.expiresAt() == null || !lot.expiresAt().isBefore(now)) {
                continue;
            }
            String ref = String.valueOf(lot.lotId());
            if (transactions.existsByUserIdAndTypeAndReferenceId(userId, CoinTransactionType.EXPIRED, ref)) {
                continue;
            }
            ledger.debit(userId, lot.remaining(), CoinTransactionType.EXPIRED, ref, "Điểm hết hạn", now);
            expired += lot.remaining();
        }
        return expired;
    }
}
