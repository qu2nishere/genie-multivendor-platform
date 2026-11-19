package com.genie.promotion.coin.application;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tính phần còn lại của từng lô Điểm theo FEFO (hết hạn trước — tiêu trước; LLD 9.9; plan Điểm Genie P02.03).
 * Bảng {@code coin_transactions} không có cột "còn lại": mỗi lô cộng là một dòng dương, mọi lần trừ (kể cả hết hạn) là
 * dòng âm; phần còn lại = trừ dần tổng đã tiêu vào các lô theo hạn tăng dần. Hàm thuần — không chạm CSDL.
 */
public final class CoinLots {

    private CoinLots() {
    }

    /** Một lô cộng: {@code expiresAt} {@code null} = không hết hạn (xếp cuối). */
    public record Lot(long id, OffsetDateTime expiresAt, int amount) {
    }

    /** Phần còn lại (> 0) của một lô. */
    public record Remaining(long lotId, OffsetDateTime expiresAt, int remaining) {
    }

    private static final Comparator<Lot> FEFO = Comparator
            .comparing((Lot l) -> l.expiresAt() == null ? 1 : 0)
            .thenComparing(Lot::expiresAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingLong(Lot::id);

    /** Các lô còn Điểm sau khi trừ {@code totalDebits} theo FEFO (bỏ lô đã dùng hết). */
    public static List<Remaining> remainingLots(List<Lot> creditLots, long totalDebits) {
        List<Lot> sorted = new ArrayList<>(creditLots);
        sorted.sort(FEFO);
        long toConsume = Math.max(0, totalDebits);
        List<Remaining> out = new ArrayList<>();
        for (Lot lot : sorted) {
            long used = Math.min(lot.amount(), toConsume);
            toConsume -= used;
            int left = (int) (lot.amount() - used);
            if (left > 0) {
                out.add(new Remaining(lot.id(), lot.expiresAt(), left));
            }
        }
        return out;
    }

    /** Tổng Điểm còn lại sẽ hết hạn trước {@code until} (bỏ qua lô đã quá {@code now} — job hết hạn sẽ xử lý). */
    public static int expiringBetween(List<Remaining> lots, OffsetDateTime now, OffsetDateTime until) {
        return lots.stream()
                .filter(r -> r.expiresAt() != null && !r.expiresAt().isBefore(now) && r.expiresAt().isBefore(until))
                .mapToInt(Remaining::remaining).sum();
    }
}
