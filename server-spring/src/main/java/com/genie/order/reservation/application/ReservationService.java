package com.genie.order.reservation.application;

import com.genie.catalog.product.application.VariantFacade;
import com.genie.order.reservation.domain.StockReservation;
import com.genie.order.reservation.domain.StockReservationRepository;
import com.genie.order.reservation.domain.StockReservationStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giữ chỗ / chốt / nhả tồn kho của đơn (LLD 9.1; plan C2 O07.03, mục 8.3). Câu SQL ghi {@code product_variants} nằm ở
 * {@link VariantFacade} (UPDATE có điều kiện, tăng {@code version}). Luôn chạy TRONG transaction của người gọi.
 */
@Service
public class ReservationService {

    private final VariantFacade variants;
    private final StockReservationRepository reservations;

    public ReservationService(VariantFacade variants, StockReservationRepository reservations) {
        this.variants = variants;
        this.reservations = reservations;
    }

    /** Một món cần giữ: thuộc đơn nào, biến thể nào, bao nhiêu. */
    public record Hold(long orderId, long variantId, int quantity) {
    }

    /** Biến thể hết hàng / không còn bán lúc giữ chỗ → người gọi rollback và trả {@code 409 GNE-INV-4090}. */
    public static class OutOfStockException extends RuntimeException {
        private final long variantId;

        public OutOfStockException(long variantId) {
            super("Biến thể " + variantId + " không đủ tồn");
            this.variantId = variantId;
        }

        public long variantId() {
            return variantId;
        }
    }

    /**
     * Giữ chỗ theo {@code variant_id} TĂNG DẦN (tránh deadlock khi hai phiên cùng giữ nhiều SKU — R4), mỗi món một câu
     * {@code UPDATE … WHERE còn đủ}. Một món không đủ → {@link OutOfStockException} (transaction của người gọi rollback
     * toàn bộ, kể cả các món đã giữ trước đó).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<StockReservation> reserveAll(List<Hold> holds, OffsetDateTime now, OffsetDateTime expiresAt) {
        List<Hold> sorted = new ArrayList<>(holds);
        sorted.sort(Comparator.comparingLong(Hold::variantId).thenComparingLong(Hold::orderId));
        List<StockReservation> out = new ArrayList<>();
        for (Hold h : sorted) {
            if (!variants.reserve(h.variantId(), h.quantity())) {
                throw new OutOfStockException(h.variantId());
            }
            out.add(StockReservation.builder().orderId(h.orderId()).productVariantId(h.variantId())
                    .reservedQuantity(h.quantity()).status(StockReservationStatus.ACTIVE).expiresAt(expiresAt)
                    .createdAt(now).build());
        }
        return reservations.saveAll(out);
    }

    /** Chốt bán (COD đặt xong / thanh toán thành công): trừ tồn thật, giữ chỗ → {@code CONFIRMED}. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmAll(List<StockReservation> list) {
        for (StockReservation r : list) {
            if (r.getStatus() != StockReservationStatus.ACTIVE) {
                continue;
            }
            if (!variants.commitSale(r.getProductVariantId(), r.getReservedQuantity())) {
                throw new IllegalStateException("Không chốt được tồn của biến thể " + r.getProductVariantId());
            }
            r.setStatus(StockReservationStatus.CONFIRMED);
        }
        reservations.saveAll(list);
    }

    /**
     * Nhả một lượt giữ chỗ còn {@code ACTIVE} → {@code newStatus} (EXPIRED / RELEASED). {@code false} = lượt đã được
     * chốt / nhả ở nơi khác (vd IPN đến trước) — không đụng tồn.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean releaseIfActive(StockReservation r, StockReservationStatus newStatus, OffsetDateTime now) {
        if (reservations.closeIfActive(r.getId(), newStatus.name(), now) == 0) {
            return false;
        }
        if (!variants.release(r.getProductVariantId(), r.getReservedQuantity())) {
            throw new IllegalStateException("reserved_count của biến thể " + r.getProductVariantId() + " nhỏ hơn lượt giữ");
        }
        return true;
    }
}
