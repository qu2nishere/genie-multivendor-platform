package com.genie.order.checkout.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.auth.address.application.AddressSnapshot;
import com.genie.catalog.category.application.CategoryFacade;
import com.genie.common.domain.PaymentMethod;
import com.genie.common.domain.PaymentStatus;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.idempotency.IdempotencyStore;
import com.genie.order.cart.domain.CartItemRepository;
import com.genie.order.checkout.application.CheckoutDiscountPolicy.Selection;
import com.genie.order.checkout.application.CheckoutPricer.Priced;
import com.genie.order.checkout.application.CheckoutPricer.PricedItem;
import com.genie.order.checkout.application.CheckoutPricer.PricedShop;
import com.genie.order.checkout.domain.CheckoutSession;
import com.genie.order.checkout.domain.CheckoutSessionRepository;
import com.genie.order.checkout.domain.SessionStatus;
import com.genie.order.checkout.event.CheckoutCreatedEvent;
import com.genie.order.order.domain.FundingStatus;
import com.genie.order.order.domain.Order;
import com.genie.order.order.domain.OrderItem;
import com.genie.order.order.domain.OrderItemRepository;
import com.genie.order.order.domain.OrderRepository;
import com.genie.order.order.domain.OrderStatus;
import com.genie.order.reservation.application.ReservationService;
import com.genie.order.reservation.application.ReservationService.Hold;
import com.genie.order.reservation.domain.StockReservation;
import com.genie.vendor.shop.application.ShopFacade;
import com.genie.vendor.shop.application.ShopFacade.ShopBuyInfo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đặt hàng — UC-ORD-07 (API_SPEC 6.10, 6.11; plan C2 3.4, D3, D10–D12). Một transaction: tạo phiên {@code INITIATED},
 * mỗi shop một đơn con ({@code PENDING_PAYMENT}) + dòng snapshot + hoa hồng, giữ chỗ tồn kho 15 phút, xóa dòng giỏ đã đặt.
 * COD → phiên {@code COMPLETED}, chốt tồn ngay, đơn chờ shop xác nhận. VNPAY / VIETQR → phiên {@code PAYMENT_PENDING},
 * khối {@code payment} lấy từ cổng {@link PaymentInitiator} (bản thật — chuỗi C3). Chống gửi trùng bằng
 * {@code Idempotency-Key} ({@link IdempotencyStore}).
 */
@Service
public class CheckoutService {

    static final String IDEM_SCOPE = "checkout";
    public static final int NOTE_MAX = 500;

    private final CheckoutPricer pricer;
    private final CheckoutSessionRepository sessions;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final CartItemRepository cartItems;
    private final ReservationService reservations;
    private final OrderCodeGenerator codes;
    private final ShopFacade shops;
    private final CategoryFacade categories;
    private final PaymentInitiator payments;
    private final IdempotencyStore idempotency;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final Duration ttl;

    public CheckoutService(CheckoutPricer pricer, CheckoutSessionRepository sessions, OrderRepository orders,
                           OrderItemRepository orderItems, CartItemRepository cartItems, ReservationService reservations,
                           OrderCodeGenerator codes, ShopFacade shops, CategoryFacade categories,
                           PaymentInitiator payments, IdempotencyStore idempotency, ApplicationEventPublisher events,
                           ObjectMapper json, Clock clock, PlatformTransactionManager txManager,
                           @Value("${genie.checkout.reservation-ttl:PT15M}") Duration ttl) {
        this.pricer = pricer;
        this.sessions = sessions;
        this.orders = orders;
        this.orderItems = orderItems;
        this.cartItems = cartItems;
        this.reservations = reservations;
        this.codes = codes;
        this.shops = shops;
        this.categories = categories;
        this.payments = payments;
        this.idempotency = idempotency;
        this.events = events;
        this.json = json;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
        this.ttl = ttl;
    }

    /** Yêu cầu đặt hàng (6.10). {@code notes}: {@code shop_id} (= vendor_profiles.id) → lời nhắn. */
    public record PlaceCommand(List<Long> cartItemIds, Long addressId, Selection selection, Map<Long, String> notes,
                               PaymentMethod paymentMethod, String returnUrl) {
    }

    /**
     * {@code POST /me/checkout-sessions}. Gửi lại cùng {@code Idempotency-Key} → trả phiên đã tạo (không tạo đơn trùng).
     */
    public CheckoutView place(long userId, PlaceCommand cmd, String idempotencyKey) {
        Optional<String> done = idempotency.begin(IDEM_SCOPE, userId, idempotencyKey);
        if (done.isPresent()) {
            CheckoutView v = get(userId, Long.parseLong(done.get()));
            return v.withPayment(paymentFor(v, userId, cmd.returnUrl()));
        }
        try {
            CheckoutView v = Objects.requireNonNull(tx.execute(status -> doPlace(userId, cmd)));
            idempotency.complete(IDEM_SCOPE, userId, idempotencyKey, String.valueOf(v.id()));
            return v.withPayment(paymentFor(v, userId, cmd.returnUrl()));
        } catch (RuntimeException e) {
            idempotency.abort(IDEM_SCOPE, userId, idempotencyKey);
            throw e;
        }
    }

    /** {@code GET /me/checkout-sessions/{id}} — chỉ chủ phiên; người khác / không có → {@code 404}. */
    @Transactional(readOnly = true)
    public CheckoutView get(long userId, long sessionId) {
        CheckoutSession s = sessions.findById(sessionId).filter(x -> x.getUserId() == userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy phiên đặt hàng."));
        return view(s, orders.findByCheckoutSessionIdOrderByIdAsc(s.getId()));
    }

    // ===================== đặt hàng =====================

    private CheckoutView doPlace(long userId, PlaceCommand cmd) {
        if (cmd.paymentMethod() == null) {
            throw InvalidRequestException.of("payment_method", "REQUIRED", null, "Chọn phương thức thanh toán.");
        }
        validateNotes(cmd.notes());
        Priced priced = pricer.price(userId, cmd.cartItemIds(), cmd.addressId(), cmd.selection());
        if (priced.address() == null) {
            throw InvalidRequestException.of("address_id", "REQUIRED", null, "Chọn địa chỉ giao hàng.");
        }
        Set<Long> found = new LinkedHashSet<>();
        priced.shops().forEach(s -> s.items().forEach(i -> found.add(i.cartItemId())));
        List<Long> missing = cmd.cartItemIds().stream().filter(id -> !found.contains(id)).distinct().toList();
        if (!missing.isEmpty()) {
            throw InvalidRequestException.of("cart_item_ids", "NOT_FOUND", missing,
                    "Một số sản phẩm không còn trong giỏ hàng — vui lòng tải lại giỏ.");
        }
        rejectUnavailable(userId, priced);

        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime expiresAt = now.plus(ttl);
        CheckoutSession session = sessions.save(CheckoutSession.builder().userId(userId)
                .totalAmount(BigDecimal.valueOf(priced.summary().grandTotal())).status(SessionStatus.INITIATED)
                .cartSnapshotJson(snapshot(priced)).expiresAt(expiresAt).build());

        String address = addressJson(priced.address());
        Map<Long, BigDecimal> categoryRates = new HashMap<>();
        List<Order> created = new ArrayList<>();
        List<Hold> holds = new ArrayList<>();
        Map<Long, List<Long>> cartItemsByVariant = new HashMap<>();
        for (PricedShop s : priced.shops()) {
            Order o = orders.save(Order.builder().orderCode(codes.nextOrderCode()).customerId(userId)
                    .vendorId(s.vendorUserId()).checkoutSessionId(session.getId()).shippingAddressSnapshotJson(address)
                    .subTotal(BigDecimal.valueOf(s.subtotal()))
                    .shippingFee(BigDecimal.valueOf(s.shippingFee() == null ? 0 : s.shippingFee()))
                    .discountAmount(BigDecimal.valueOf(s.shopDiscount()))
                    .platformDiscountAmount(BigDecimal.valueOf(s.platformDiscount()))
                    .shippingDiscountAmount(BigDecimal.valueOf(s.freeshipDiscount()))
                    .grandTotal(BigDecimal.valueOf(s.total())).paymentMethod(cmd.paymentMethod())
                    .paymentStatus(PaymentStatus.PENDING).fundingStatus(FundingStatus.UNFUNDED)
                    .status(OrderStatus.PENDING_PAYMENT).buyerNote(note(cmd.notes(), s)).build());
            created.add(o);
            List<OrderItem> items = new ArrayList<>();
            for (PricedItem i : s.items()) {
                BigDecimal rate = commissionRate(s.vendorUserId(), i.line().categoryId(), categoryRates);
                BigDecimal gross = BigDecimal.valueOf(i.lineTotal());
                BigDecimal commission = gross.multiply(rate).setScale(0, RoundingMode.HALF_UP);
                items.add(OrderItem.builder().orderId(o.getId()).productVariantId(i.variantId())
                        .productNameSnapshot(i.productName()).skuSnapshot(i.line().sku()).quantity(i.quantity())
                        .unitPrice(BigDecimal.valueOf(i.unitPrice())).lineTotal(gross).grossAmount(gross)
                        .commissionRate(rate).commissionAmount(commission).vendorReceivable(gross.subtract(commission))
                        .build());
                holds.add(new Hold(o.getId(), i.variantId(), i.quantity()));
                cartItemsByVariant.computeIfAbsent(i.variantId(), k -> new ArrayList<>()).add(i.cartItemId());
            }
            orderItems.saveAll(items);
        }

        List<StockReservation> held;
        try {
            held = reservations.reserveAll(holds, now, expiresAt);
        } catch (ReservationService.OutOfStockException e) {
            // Vừa hết hàng giữa xem trước và đặt (người khác mua trước) — rollback toàn bộ phiên
            throw new BusinessException(ErrorCode.OUT_OF_STOCK, Map.of("out_of_stock_cart_item_ids",
                    cartItemsByVariant.getOrDefault(e.variantId(), List.of())));
        }
        cartItems.deleteAllByIdInBatch(found);

        if (cmd.paymentMethod() == PaymentMethod.COD) {
            reservations.confirmAll(held);
            session.complete(now);
        } else {
            session.markPaymentPending();
        }
        sessions.save(session);
        events.publishEvent(new CheckoutCreatedEvent(session.getId(), userId,
                created.stream().map(Order::getId).toList(), cmd.paymentMethod().name(),
                priced.summary().grandTotal()));
        return view(session, created);
    }

    /** Lỗi theo đúng loại (API 6.10): shop nghỉ / không giao được → 4093, hàng của chính mình → 4094, còn lại → 4090. */
    private static void rejectUnavailable(long userId, Priced priced) {
        List<Long> restingShops = new ArrayList<>();
        List<Long> outOfStock = new ArrayList<>();
        boolean own = false;
        for (PricedShop s : priced.shops()) {
            for (PricedItem i : s.items()) {
                if (i.isAvailable()) {
                    continue;
                }
                if (i.line().vendorUserId() == userId) {
                    own = true;
                } else if ("SHOP_ON_VACATION".equals(i.unavailableReason())
                        || CheckoutPricer.SHIPPING_UNAVAILABLE.equals(i.unavailableReason())) {
                    if (s.shop() != null && !restingShops.contains(s.shop().shopId())) {
                        restingShops.add(s.shop().shopId());
                    }
                } else {
                    outOfStock.add(i.cartItemId());
                }
            }
        }
        if (own) {
            throw new BusinessException(ErrorCode.OWN_SHOP_PRODUCT);
        }
        if (!restingShops.isEmpty()) {
            throw new BusinessException(ErrorCode.SHOP_ON_VACATION, Map.of("shop_ids", restingShops));
        }
        if (!outOfStock.isEmpty()) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK, Map.of("out_of_stock_cart_item_ids", outOfStock));
        }
    }

    /** Hoa hồng chụp vào dòng đơn (D12): thỏa thuận riêng của shop → ngành → 5%. */
    private BigDecimal commissionRate(long vendorUserId, Long categoryId, Map<Long, BigDecimal> cache) {
        BigDecimal categoryRate = categoryId == null ? null
                : cache.computeIfAbsent(categoryId, categories::effectiveCommissionRate);
        return shops.effectiveCommissionRate(vendorUserId, categoryRate);
    }

    private static void validateNotes(Map<Long, String> notes) {
        if (notes == null) {
            return;
        }
        notes.forEach((shopId, n) -> {
            if (n != null && n.length() > NOTE_MAX) {
                throw InvalidRequestException.of("notes", "TOO_LONG", shopId, "Lời nhắn tối đa 500 ký tự.");
            }
        });
    }

    private static String note(Map<Long, String> notes, PricedShop s) {
        if (notes == null || s.shop() == null) {
            return null;
        }
        String n = notes.get(s.shop().shopId());
        return n == null || n.isBlank() ? null : n.trim();
    }

    private String addressJson(AddressSnapshot a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("address_id", a.addressId());
        m.put("recipient_name", a.recipientName());
        m.put("phone_number", a.phoneNumber());
        m.put("address_line", a.addressLine());
        m.put("ward_code", a.wardCode());
        m.put("ward", a.ward());
        m.put("province_id", a.provinceId());
        m.put("province", a.province());
        m.put("delivery_note", a.deliveryNote());
        m.put("full_address", a.fullAddress());
        return write(m);
    }

    private String snapshot(Priced p) {
        List<Map<String, Object>> lines = new ArrayList<>();
        p.shops().forEach(s -> s.items().forEach(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("cart_item_id", i.cartItemId());
            m.put("variant_id", i.variantId());
            m.put("vendor_id", s.vendorUserId());
            m.put("quantity", i.quantity());
            m.put("unit_price", i.unitPrice());
            lines.add(m);
        }));
        return write(Map.of("lines", lines, "grand_total", p.summary().grandTotal()));
    }

    private String write(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    // ===================== đọc =====================

    private CheckoutView view(CheckoutSession s, List<Order> list) {
        Map<Long, ShopBuyInfo> info = shops.buyInfo(list.stream().map(Order::getVendorId).toList(),
                OffsetDateTime.now(clock));
        long sub = 0, ship = 0, disc = 0, coin = 0;
        List<CheckoutView.OrderLine> lines = new ArrayList<>();
        for (Order o : list) {
            ShopBuyInfo si = info.get(o.getVendorId());
            lines.add(new CheckoutView.OrderLine(o.getId(), o.getOrderCode(),
                    new CheckoutView.Shop(si == null ? 0 : si.shopId(), si == null ? "" : si.mini().name()),
                    money(o.getGrandTotal())));
            sub += money(o.getSubTotal());
            ship += money(o.getShippingFee());
            disc += money(o.getDiscountAmount()) + money(o.getPlatformDiscountAmount())
                    + money(o.getShippingDiscountAmount());
            coin += money(o.getCoinDiscountAmount());
        }
        String method = list.isEmpty() || list.get(0).getPaymentMethod() == null ? null
                : list.get(0).getPaymentMethod().name();
        return new CheckoutView(s.getId(), OrderCodeGenerator.sessionCode(s.getId()), s.getStatus().name(), method,
                s.getCreatedAt(), s.getExpiresAt(), lines,
                new CheckoutView.Summary(sub, ship, disc, coin, money(s.getTotalAmount())), null);
    }

    /** Khối payment: COD → chỉ method; online còn chờ → hỏi cổng thanh toán; đã kết thúc → null. */
    private PaymentInitiator.Payment paymentFor(CheckoutView v, long userId, String returnUrl) {
        if (PaymentMethod.COD.name().equals(v.paymentMethod())) {
            return PaymentInitiator.Payment.cod();
        }
        if (!SessionStatus.PAYMENT_PENDING.name().equals(v.status()) || v.paymentMethod() == null) {
            return null;
        }
        return payments.initiate(new PaymentInitiator.PaymentRequest(v.id(), v.code(), userId,
                v.summary().grandTotal(), PaymentMethod.valueOf(v.paymentMethod()), v.reservationExpiresAt(),
                returnUrl));
    }

    private static long money(BigDecimal v) {
        return v == null ? 0 : v.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
