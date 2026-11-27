// Dựng $Voucher theo lô cho người xem (plan 2026-11-26 voucher V05.01, D13, D16) — chống N+1
package com.genie.promotion.voucher.application;

import com.genie.common.domain.VoucherType;
import com.genie.promotion.voucher.domain.Voucher;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Một lời gọi mỗi loại cho cả trang: shop ({@code vendor_profiles}), tên ngành (4 bảng phạm vi), bản lưu trong Kho và lượt
 * đã dùng của người xem. {@code viewerId = null} (khách vãng lai) → không có {@code is_saved}.
 */
@Component
public class VoucherViews {

    private final VoucherReadModel readModel;
    private final VoucherScopes scopes;

    public VoucherViews(VoucherReadModel readModel, VoucherScopes scopes) {
        this.readModel = readModel;
        this.scopes = scopes;
    }

    public List<VoucherView> of(List<Voucher> vouchers, OffsetDateTime now, Long viewerId) {
        if (vouchers.isEmpty()) return List.of();
        List<Long> ids = vouchers.stream().map(Voucher::getId).toList();
        Map<Long, VoucherReadModel.ShopRef> shops = readModel.shopRefs(vouchers.stream()
                .filter(v -> v.getType() == VoucherType.SHOP).map(Voucher::getVendorId).filter(Objects::nonNull)
                .distinct().toList());
        Map<Long, VoucherScopes.Sets> sets = scopes.ofMany(ids);
        Set<Long> catIds = new LinkedHashSet<>();
        sets.values().forEach(s -> {
            catIds.addAll(s.categoryIds());
            catIds.addAll(s.excludedCategoryIds());
        });
        Map<Long, String> catNames = readModel.categoryNameMap(catIds);
        Set<Long> saved = viewerId == null ? Set.of() : readModel.savedAmong(viewerId, ids);
        Map<Long, VoucherReadModel.Usage> usages = viewerId == null ? Map.of() : readModel.usagesOf(viewerId, ids);
        List<VoucherView> out = new ArrayList<>(vouchers.size());
        for (Voucher v : vouchers) {
            VoucherReadModel.ShopRef s = v.getType() == VoucherType.SHOP ? shops.get(v.getVendorId()) : null;
            VoucherScopes.Sets set = sets.get(v.getId());
            VoucherReadModel.Usage u = usages.get(v.getId());
            boolean usedUp = u != null && v.getUsagePerUser() != null && u.count() >= v.getUsagePerUser();
            out.add(VoucherView.of(v, now, usedUp, viewerId == null ? null : saved.contains(v.getId()),
                    s == null ? null : new VoucherView.ShopRef(s.id(), s.name()),
                    names(set.categoryIds(), catNames), names(set.excludedCategoryIds(), catNames),
                    u == null ? null : u.lastOrderCode()));
        }
        return out;
    }

    public VoucherView of(Voucher v, OffsetDateTime now, Long viewerId) {
        return of(List.of(v), now, viewerId).get(0);
    }

    private static List<String> names(Set<Long> ids, Map<Long, String> names) {
        return ids.stream().map(names::get).filter(Objects::nonNull).toList();
    }
}
