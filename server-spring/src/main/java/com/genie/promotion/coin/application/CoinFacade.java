package com.genie.promotion.coin.application;

import com.genie.promotion.coin.domain.CoinTransactionType;
import com.genie.promotion.coin.domain.UserCoinRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cửa công khai của Điểm Genie cho module khác (TS 4.4; plan Điểm Genie P02.05) — chưa có nơi gọi. Sẽ dùng bởi:
 * {@code order} (hoàn Điểm theo đơn {@code ORDER_CASHBACK}, hoàn lại khi hủy {@code ORDER_REFUND}), checkout
 * (dùng Điểm khi thanh toán {@code USED_AT_CHECKOUT} — UC-PRO-10), {@code social} (thưởng đánh giá có ảnh
 * {@code REVIEW_BONUS}). Ghi Điểm chạy trong transaction chính của nơi gọi (Điểm là tiền — TS 4.5).
 */
@Component
public class CoinFacade {

    private final CoinLedgerService ledger;
    private final UserCoinRepository coins;
    private final Clock clock;

    public CoinFacade(CoinLedgerService ledger, UserCoinRepository coins, Clock clock) {
        this.ledger = ledger;
        this.coins = coins;
        this.clock = clock;
    }

    /** Cộng Điểm; trả số dư mới. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int earn(long userId, int amount, CoinTransactionType type, String referenceId, String label) {
        return ledger.credit(userId, amount, type, referenceId, label, OffsetDateTime.now(clock)).balance();
    }

    /** Trừ Điểm ({@code 422 GNE-PRO-4222} khi thiếu); trả số dư mới. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int spend(long userId, int amount, CoinTransactionType type, String referenceId, String label) {
        return ledger.debit(userId, amount, type, referenceId, label, OffsetDateTime.now(clock)).balance();
    }

    /** Số dư hiện tại (0 khi chưa có ví Điểm). */
    @Transactional(readOnly = true)
    public int balanceOf(long userId) {
        return coins.findById(userId).map(c -> c.getBalance()).orElse(0);
    }
}
