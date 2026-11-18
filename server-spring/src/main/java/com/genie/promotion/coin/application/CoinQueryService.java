package com.genie.promotion.coin.application;

import com.genie.common.api.CursorPage;
import com.genie.common.error.InvalidRequestException;
import com.genie.promotion.coin.domain.CoinTransaction;
import com.genie.promotion.coin.domain.CoinTransactionRepository;
import com.genie.promotion.coin.domain.CoinTransactionType;
import com.genie.promotion.coin.domain.DailyCheckin;
import com.genie.promotion.coin.domain.DailyCheckinRepository;
import com.genie.promotion.coin.domain.UserCoin;
import com.genie.promotion.coin.domain.UserCoinRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tra cứu Điểm Genie của chính người dùng — UC-PRO-09 (API_SPEC 3.13; plan Điểm Genie P04). Chỉ đọc.
 */
@Service
public class CoinQueryService {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final UserCoinRepository coins;
    private final CoinTransactionRepository transactions;
    private final DailyCheckinRepository checkins;
    private final CoinLedgerService ledger;
    private final CoinProperties properties;
    private final Clock clock;

    public CoinQueryService(UserCoinRepository coins, CoinTransactionRepository transactions,
                            DailyCheckinRepository checkins, CoinLedgerService ledger, CoinProperties properties,
                            Clock clock) {
        this.coins = coins;
        this.transactions = transactions;
        this.checkins = checkins;
        this.ledger = ledger;
        this.properties = properties;
        this.clock = clock;
    }

    /** {@code GET /me/coins}. {@code expiringSoon} = {@code null} khi không có Điểm nào sắp hết hạn. */
    public record Summary(int balance, ExpiringSoon expiringSoon, CheckInStatus checkIn) {
    }

    public record ExpiringSoon(int amount, int withinDays) {
    }

    public record CheckInStatus(int streakDays, boolean checkedInToday, OffsetDateTime nextCheckInAt,
                                List<Integer> rewards) {
    }

    /** Một dòng lịch sử ({@code amount} âm khi tiêu). */
    public record TransactionView(long id, CoinTransactionType type, String description, int amount,
                                  OffsetDateTime createdAt) {
    }

    @Transactional(readOnly = true)
    public Summary summary(long userId) {
        OffsetDateTime now = CoinTime.nowVn(clock);
        UserCoin coin = coins.findById(userId).orElse(null);
        int balance = 0;
        ExpiringSoon expiring = null;
        if (coin != null) {
            ledger.checkIntegrity(coin);   // lệch băm: báo động + log, vẫn cho xem số dư (D8)
            balance = coin.getBalance();
            int days = properties.expiringSoonDays();
            int amount = CoinLots.expiringBetween(ledger.remainingLots(userId), now, now.plusDays(days));
            expiring = amount > 0 ? new ExpiringSoon(amount, days) : null;
        }
        DailyCheckin d = checkins.findByCustomerId(userId).orElse(null);
        CheckInStatus status = new CheckInStatus(CoinTime.streakDays(d, now), CoinTime.checkedInOn(d, now),
                CoinTime.nextMidnight(now), properties.checkIn().rewards());
        return new Summary(balance, expiring, status);
    }

    /** {@code GET /me/coins/transactions?cursor&limit} — mới nhất trước, keyset ({@code created_at}, {@code id}). */
    @Transactional(readOnly = true)
    public CursorPage<TransactionView> transactions(long userId, String cursor, Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1 || size > MAX_LIMIT) {
            throw InvalidRequestException.of("limit", "OUT_OF_RANGE", limit, "limit phải từ 1 đến 50.");
        }
        PageRequest page = PageRequest.of(0, size + 1);
        List<CoinTransaction> rows;
        if (cursor == null || cursor.isBlank()) {
            rows = transactions.findPage(userId, page);
        } else {
            Cursor c = Cursor.decode(cursor.trim());
            rows = transactions.findPageAfter(userId, c.createdAt(), c.id(), page);
        }
        boolean hasMore = rows.size() > size;
        List<CoinTransaction> pageRows = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            CoinTransaction last = pageRows.get(pageRows.size() - 1);
            next = new Cursor(last.getCreatedAt(), last.getId()).encode();
        }
        List<TransactionView> items = pageRows.stream().map(t -> new TransactionView(t.getId(), t.getType(),
                describe(t), t.getAmount(), t.getCreatedAt().atZoneSameInstant(CoinTime.VN).toOffsetDateTime())).toList();
        return new CursorPage<>(items, new CursorPage.Pagination(size, hasMore, next));
    }

    /** Mô tả hiển thị: nhãn đã ghi, không có thì sinh theo loại. */
    static String describe(CoinTransaction t) {
        if (t.getLabel() != null && !t.getLabel().isBlank()) {
            return t.getLabel();
        }
        return switch (t.getType()) {
            case CHECK_IN -> "Điểm danh hằng ngày";
            case ORDER_CASHBACK -> "Hoàn Điểm đơn hàng";
            case REVIEW_BONUS -> "Thưởng đánh giá có ảnh";
            case USED_AT_CHECKOUT -> "Dùng Điểm khi thanh toán";
            case ORDER_REFUND -> "Hoàn lại Điểm đơn bị hủy";
            case VOUCHER_REDEEM -> "Đổi Điểm lấy voucher";
        };
    }

    /** Con trỏ = Base64URL({@code <created_at ISO>|<id>}). Hỏng → 422 trường {@code cursor}. */
    record Cursor(OffsetDateTime createdAt, long id) {

        String encode() {
            String raw = createdAt.toInstant() + "|" + id;
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        static Cursor decode(String value) {
            try {
                String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
                int sep = raw.indexOf('|');
                return new Cursor(Instant.parse(raw.substring(0, sep)).atOffset(ZoneOffset.UTC),
                        Long.parseLong(raw.substring(sep + 1)));
            } catch (RuntimeException e) {
                throw InvalidRequestException.of("cursor", "INVALID_FORMAT", value, "Con trỏ phân trang không hợp lệ.");
            }
        }
    }
}
