package com.genie.promotion.coin.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.promotion.coin.domain.CoinTransaction;
import com.genie.promotion.coin.domain.CoinTransactionRepository;
import com.genie.promotion.coin.domain.CoinTransactionType;
import com.genie.promotion.coin.domain.UserCoin;
import com.genie.promotion.coin.domain.UserCoinRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sổ Điểm dùng chung trong module {@code promotion} (plan Điểm Genie P02; LLD 9.9). Mỗi lần ghi: khóa dòng
 * {@code user_coins} → kiểm băm (sai → từ chối, D8) → đổi số dư bằng phương thức entity → ghi dòng {@code coin_transactions}
 * → tính lại băm. Bắt buộc chạy trong transaction của nghiệp vụ gọi ({@code MANDATORY}) — Điểm là tiền (TS 4.5).
 */
@Service
public class CoinLedgerService {

    private static final Logger log = LoggerFactory.getLogger(CoinLedgerService.class);

    private final UserCoinRepository coins;
    private final CoinTransactionRepository transactions;
    private final CoinBalanceHasher hasher;
    private final CoinProperties properties;
    private final ObjectProvider<CoinAlertPort> alerts;

    public CoinLedgerService(UserCoinRepository coins, CoinTransactionRepository transactions, CoinBalanceHasher hasher,
                             CoinProperties properties, ObjectProvider<CoinAlertPort> alerts) {
        this.coins = coins;
        this.transactions = transactions;
        this.hasher = hasher;
        this.properties = properties;
        this.alerts = alerts;
    }

    /** Kết quả một lần ghi: dòng sổ vừa tạo + số dư mới. */
    public record Entry(CoinTransaction transaction, int balance) {
    }

    /** Cộng Điểm — tạo một LÔ hết hạn sau {@code genie.coin.expiry-months} tháng (N7). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Entry credit(long userId, int amount, CoinTransactionType type, String referenceId, String label,
                        OffsetDateTime now) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số Điểm cộng phải > 0");
        }
        UserCoin coin = lockOrCreate(userId, now);
        coin.earn(amount, now);
        hasher.seal(coin);
        CoinTransaction tx = transactions.save(CoinTransaction.builder().userId(userId).amount(amount).type(type)
                .referenceId(referenceId).label(label).expiresAt(now.plusMonths(properties.expiryMonths())).build());
        return new Entry(tx, coin.getBalance());
    }

    /** Trừ Điểm — một dòng âm; thiếu → {@code 422 GNE-PRO-4222} (số dư không đổi). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Entry debit(long userId, int amount, CoinTransactionType type, String referenceId, String label,
                       OffsetDateTime now) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số Điểm trừ phải > 0");
        }
        UserCoin coin = lockOrCreate(userId, now);
        if (coin.getBalance() < amount) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("balance", coin.getBalance());
            details.put("required", amount);
            details.put("missing", amount - coin.getBalance());
            throw new BusinessException(ErrorCode.INSUFFICIENT_COINS, details);
        }
        coin.spend(amount, now);
        hasher.seal(coin);
        CoinTransaction tx = transactions.save(CoinTransaction.builder().userId(userId).amount(-amount).type(type)
                .referenceId(referenceId).label(label).expiresAt(null).build());
        return new Entry(tx, coin.getBalance());
    }

    /** Các lô còn Điểm của người dùng (FEFO). */
    @Transactional(readOnly = true)
    public List<CoinLots.Remaining> remainingLots(long userId) {
        List<CoinLots.Lot> lots = transactions.findCreditLots(userId).stream()
                .map(t -> new CoinLots.Lot(t.getId(), t.getExpiresAt(), t.getAmount())).toList();
        return CoinLots.remainingLots(lots, transactions.sumDebits(userId));
    }

    /** Kiểm băm khi chỉ đọc: lệch thì báo động + log, KHÔNG chặn việc xem số dư (D8). */
    public boolean checkIntegrity(UserCoin coin) {
        if (hasher.matches(coin)) {
            return true;
        }
        report(coin);
        return false;
    }

    private UserCoin lockOrCreate(long userId, OffsetDateTime now) {
        UserCoin coin = coins.findByIdForUpdate(userId).orElse(null);
        if (coin == null) {
            coin = UserCoin.builder().userId(userId).balance(0).lastUpdated(now).build();
            hasher.seal(coin);
            coin = coins.saveAndFlush(coin);
            coin = coins.findByIdForUpdate(userId).orElseThrow();
        }
        if (!hasher.matches(coin)) {
            report(coin);
            // D8: không ghi Điểm cho tài khoản có số dư nghi bị sửa — không lộ chi tiết cho client
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return coin;
    }

    private void report(UserCoin coin) {
        long sum = transactions.sumAmount(coin.getUserId());
        log.error("Số dư Điểm của user {} không khớp băm (lưu {}, Σ sổ {}) — nghi bị sửa trực tiếp CSDL",
                coin.getUserId(), coin.getBalance(), sum);
        CoinAlertPort port = alerts.getIfAvailable();
        if (port != null) {
            port.balanceTampered(coin.getUserId(), coin.getBalance(), sum);
        }
    }
}
