// Tạo / sửa / tắt / xóa voucher — shop (UC-PRO-01) và sàn (UC-PRO-02); plan 2026-11-26 voucher V03.02, V04.01, D3–D19
package com.genie.promotion.voucher.application;

import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.domain.VoucherType;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.InvalidRequestException.Violation;
import com.genie.common.security.ClientInfo;
import com.genie.promotion.coin.application.CoinProperties;
import com.genie.promotion.voucher.application.VoucherInputValidator.Kind;
import com.genie.promotion.voucher.domain.Voucher;
import com.genie.promotion.voucher.domain.VoucherRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Một luật cho hai màn: voucher shop gắn {@code vendor_id} = người gọi (lấy từ token — N4), voucher của shop khác → 404;
 * voucher sàn ghi nhật ký {@code VOUCHER_*} trong cùng transaction (N6). Mỗi thay đổi phát {@link VoucherChangedEvent}
 * (xóa cache sau commit — N5). Đụng độ {@code @Version} với luồng trừ lượt → thử lại một lần, vẫn lỗi → {@code 4094} (D19).
 */
@Service
public class VoucherManagementService {

    private final VoucherRepository vouchers;
    private final VoucherScopes scopes;
    private final VoucherReadModel readModel;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final CoinProperties coin;
    private final Clock clock;
    private final TransactionTemplate tx;

    public VoucherManagementService(VoucherRepository vouchers, VoucherScopes scopes, VoucherReadModel readModel,
                                    AuditLogger audit, ApplicationEventPublisher events, CoinProperties coin, Clock clock,
                                    PlatformTransactionManager transactionManager) {
        this.vouchers = vouchers;
        this.scopes = scopes;
        this.readModel = readModel;
        this.audit = audit;
        this.events = events;
        this.coin = coin;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // ===================== Voucher shop (UC-PRO-01) =====================

    public VoucherAdminViews.Detail createShop(long me, VoucherInput in) {
        return write(() -> {
            OffsetDateTime now = now();
            VoucherDraft d = validated(in, Kind.SHOP, now, true, me);
            Voucher v = save(Voucher.createShop(me, d.terms()), null);
            scopes.replace(v.getId(), VoucherScopes.Sets.of(d));
            events.publishEvent(new VoucherChangedEvent(v.getId(), true));
            return detail(v, now);
        });
    }

    public VoucherAdminViews.Detail updateShop(long me, long id, VoucherInput in) {
        return write(() -> {
            OffsetDateTime now = now();
            Voucher v = ownShopVoucher(me, id);
            VoucherDraft d = validated(in, Kind.SHOP, now, false, me);
            edit(v, Voucher.createShop(me, d.terms()), d, now, false);
            events.publishEvent(new VoucherChangedEvent(v.getId(), true));
            return detail(v, now);
        });
    }

    public VoucherAdminViews.Detail disableShop(long me, long id, Boolean isActive) {
        requireDisable(isActive);
        return write(() -> {
            Voucher v = ownShopVoucher(me, id);
            v.disable();
            vouchers.saveAndFlush(v);
            events.publishEvent(new VoucherChangedEvent(v.getId(), true));
            return detail(v, now());
        });
    }

    public VoucherAdminViews.Detail getShop(long me, long id) {
        return tx.execute(s -> detail(ownShopVoucher(me, id), now()));
    }

    // ===================== Voucher sàn (UC-PRO-02) =====================

    public VoucherAdminViews.Detail createPlatform(long staffId, VoucherInput in, ClientInfo client) {
        return write(() -> {
            OffsetDateTime now = now();
            VoucherDraft d = validated(in, Kind.PLATFORM, now, true, null);
            Voucher v = save(Voucher.createPlatform(d.terms(), d.sponsor(), d.platformSharePercent()), null);
            scopes.replace(v.getId(), VoucherScopes.Sets.of(d));
            Map<String, Object> details = target(v);
            details.put("type", v.getType().name());
            details.put("funded_by", v.getSponsorType().name());
            details.put("budget", VoucherAdminViews.money(v.getBudget()));
            audit.record(AuditAction.VOUCHER_CREATED, staffId, String.valueOf(v.getId()), details, client, true);
            events.publishEvent(new VoucherChangedEvent(v.getId(), false));
            return detail(v, now);
        });
    }

    public VoucherAdminViews.Detail updatePlatform(long staffId, long id, VoucherInput in, ClientInfo client) {
        return write(() -> {
            OffsetDateTime now = now();
            Voucher v = platformVoucher(id);
            VoucherDraft d = validated(in, Kind.PLATFORM, now, false, null);
            Voucher draft = Voucher.createPlatform(d.terms(), d.sponsor(), d.platformSharePercent());
            if (isCoinReward(v) && !v.getCode().equals(draft.getCode())) {
                throw locked(List.of("code"));   // cấu hình genie.coin.rewards trỏ theo mã (D12)
            }
            Map<String, Object> before = snapshot(v, scopes.of(v.getId()));
            edit(v, draft, d, now, true);
            Map<String, Object> after = snapshot(v, scopes.of(v.getId()));
            Map<String, Object> changes = new LinkedHashMap<>();
            after.forEach((k, val) -> {
                if (!Objects.equals(before.get(k), val)) {
                    changes.put(k, Map.of("from", String.valueOf(before.get(k)), "to", String.valueOf(val)));
                }
            });
            Map<String, Object> details = target(v);
            details.put("changes", changes);
            audit.record(AuditAction.VOUCHER_UPDATED, staffId, String.valueOf(v.getId()), details, client, true);
            events.publishEvent(new VoucherChangedEvent(v.getId(), false));
            return detail(v, now);
        });
    }

    public VoucherAdminViews.Detail disablePlatform(long staffId, long id, Boolean isActive, ClientInfo client) {
        requireDisable(isActive);
        return write(() -> {
            OffsetDateTime now = now();
            Voucher v = platformVoucher(id);
            String statusBefore = v.status(now).name();
            v.disable();
            vouchers.saveAndFlush(v);
            Map<String, Object> details = target(v);
            details.put("status_before", statusBefore);
            audit.record(AuditAction.VOUCHER_DISABLED, staffId, String.valueOf(v.getId()), details, client, true);
            events.publishEvent(new VoucherChangedEvent(v.getId(), false));
            return detail(v, now);
        });
    }

    public void deletePlatform(long staffId, long id, ClientInfo client) {
        write(() -> {
            OffsetDateTime now = now();
            Voucher v = platformVoucher(id);
            if (isCoinReward(v)) {
                throw locked(List.of("code"));
            }
            boolean used = readModel.hasUsages(v.getId()) || v.getUsedCount() > 0;
            if (!v.isDeletable(used, now)) {
                throw used ? new BusinessException(ErrorCode.VOUCHER_HAS_USAGES) : locked(List.of("status"));
            }
            Map<String, Object> details = target(v);
            scopes.deleteAll(v.getId());
            readModel.deleteSavers(v.getId());
            vouchers.delete(v);
            vouchers.flush();
            audit.record(AuditAction.VOUCHER_DELETED, staffId, String.valueOf(id), details, client, true);
            events.publishEvent(new VoucherChangedEvent(id, false));
            return null;
        });
    }

    public VoucherAdminViews.Detail getPlatform(long id) {
        return tx.execute(s -> detail(platformVoucher(id), now()));
    }

    // ===================== Dùng chung =====================

    /** Mã voucher quà đổi Điểm (D12). */
    public Set<String> coinRewardCodes() {
        return coin.rewards().stream().map(CoinProperties.Reward::voucherCode).filter(Objects::nonNull)
                .map(String::toUpperCase).collect(Collectors.toSet());
    }

    boolean isCoinReward(Voucher v) {
        return coinRewardCodes().contains(v.getCode().toUpperCase());
    }

    private VoucherAdminViews.Detail detail(Voucher v, OffsetDateTime now) {
        return VoucherAdminViews.detail(v, scopes.of(v.getId()), now, readModel.hasSavers(v.getId()), isCoinReward(v));
    }

    /** Kiểm input + các kiểm phụ thuộc CSDL (ngành đang bật, sản phẩm của chính shop, trùng mã) — gom lỗi một lần. */
    private VoucherDraft validated(VoucherInput in, Kind kind, OffsetDateTime now, boolean creating, Long vendorUserId) {
        VoucherInputValidator.Result r = VoucherInputValidator.validate(in, kind, now, creating);
        List<Violation> errors = new ArrayList<>(r.violations());
        VoucherDraft d = r.draft();
        if (d != null) {
            missing(errors, "category_ids", d.categoryIds(), readModel.activeCategories(d.categoryIds()), "Ngành hàng không tồn tại.");
            missing(errors, "excluded_category_ids", d.excludedCategoryIds(),
                    readModel.activeCategories(d.excludedCategoryIds()), "Ngành hàng không tồn tại.");
            if (vendorUserId != null) {
                missing(errors, "product_ids", d.productIds(), readModel.productsOwnedBy(vendorUserId, d.productIds()),
                        "Chỉ chọn được sản phẩm của shop.");
                missing(errors, "excluded_product_ids", d.excludedProductIds(),
                        readModel.productsOwnedBy(vendorUserId, d.excludedProductIds()), "Chỉ chọn được sản phẩm của shop.");
            } else {
                missing(errors, "excluded_product_ids", d.excludedProductIds(),
                        readModel.existingProducts(d.excludedProductIds()), "Sản phẩm không tồn tại.");
            }
        }
        VoucherInputValidator.throwIfAny(errors);
        return d;
    }

    private static void missing(List<Violation> errors, String field, Set<Long> wanted, Set<Long> found, String message) {
        Set<Long> bad = new LinkedHashSet<>(wanted);
        bad.removeAll(found);
        if (!bad.isEmpty()) {
            errors.add(VoucherInputValidator.err(field, "NOT_FOUND", List.copyOf(bad), message));
        }
    }

    /** Luật sửa D3: trường khóa (kể cả 4 tập phạm vi) → 409 4094 + {@code locked_fields}. */
    private void edit(Voucher v, Voucher draft, VoucherDraft d, OffsetDateTime now, boolean platform) {
        boolean hasSavers = readModel.hasSavers(v.getId());
        boolean full = v.fullyEditable(hasSavers, now);
        List<String> locked = new ArrayList<>(v.lockedChanges(draft, hasSavers, now));
        VoucherScopes.Sets sets = VoucherScopes.Sets.of(d);
        if (!full && locked.isEmpty() && !scopes.of(v.getId()).equals(sets)) {
            locked.add("scope");
        }
        if (!locked.isEmpty()) {
            throw locked(locked);
        }
        if (!v.getCode().equals(draft.getCode())) {
            ensureCodeFree(draft.getCode(), v.getId());
        }
        v.applyEdit(draft, full);
        save(v, v.getId());
        if (full) {
            scopes.replace(v.getId(), sets);
        }
    }

    private Voucher save(Voucher v, Long selfId) {
        if (selfId == null) {
            ensureCodeFree(v.getCode(), null);
        }
        try {
            return vouchers.saveAndFlush(v);
        } catch (DataIntegrityViolationException e) {
            throw codeTaken();   // hai request cùng mã đua nhau — UNIQUE chặn (D5)
        }
    }

    private void ensureCodeFree(String code, Long selfId) {
        vouchers.findByCode(code).filter(o -> !o.getId().equals(selfId)).ifPresent(o -> {
            throw codeTaken();
        });
    }

    private Voucher ownShopVoucher(long me, long id) {
        return vouchers.findById(id).filter(v -> v.getType() == VoucherType.SHOP && Objects.equals(v.getVendorId(), me))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private Voucher platformVoucher(long id) {
        return vouchers.findById(id).filter(v -> v.getType() != VoucherType.SHOP)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private static void requireDisable(Boolean isActive) {
        if (!Boolean.FALSE.equals(isActive)) {
            throw InvalidRequestException.of("is_active", "ONE_WAY", isActive,
                    "Chỉ tắt được voucher; muốn chạy lại hãy Nhân bản thành voucher mới.");
        }
    }

    /** Chạy trong transaction, đụng độ phiên bản → thử lại một lần (D19). */
    private <T> T write(Supplier<T> work) {
        try {
            return tx.execute(s -> work.get());
        } catch (ObjectOptimisticLockingFailureException first) {
            try {
                return tx.execute(s -> work.get());
            } catch (ObjectOptimisticLockingFailureException again) {
                throw new BusinessException(ErrorCode.VOUCHER_LOCKED, "Voucher vừa thay đổi, hãy tải lại.");
            }
        }
    }

    private static BusinessException locked(List<String> fields) {
        return new BusinessException(ErrorCode.VOUCHER_LOCKED, Map.of("locked_fields", List.copyOf(fields)));
    }

    private static BusinessException codeTaken() {
        return new BusinessException(ErrorCode.VOUCHER_CODE_TAKEN, Map.of("field", "code"));
    }

    private static Map<String, Object> target(Voucher v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("target_label", v.getCode() + " · " + v.getTitle());
        m.put("code", v.getCode());
        return m;
    }

    private static Map<String, Object> snapshot(Voucher v, VoucherScopes.Sets sets) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", v.getTitle());
        m.put("code", v.getCode());
        m.put("discount_type", v.getDiscountType().name());
        m.put("discount_value", VoucherAdminViews.discountValue(v));
        m.put("max_discount", VoucherAdminViews.money(v.getMaxDiscountCap()));
        m.put("min_order_value", v.getMinOrderValue().longValue());
        m.put("usage_limit", v.getUsageLimit());
        m.put("usage_per_user", v.getUsagePerUser());
        m.put("budget", VoucherAdminViews.money(v.getBudget()));
        m.put("valid_from", v.getValidFrom().toInstant());
        m.put("valid_to", v.getValidTo().toInstant());
        m.put("scope", v.getScope().name());
        m.put("category_ids", sets.categoryIds());
        m.put("excluded_category_ids", sets.excludedCategoryIds());
        m.put("excluded_product_ids", sets.excludedProductIds());
        m.put("funded_by", v.getSponsorType().name());
        m.put("platform_share_percent", VoucherAdminViews.plain(v.getPlatformSharePercent()));
        return m;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
