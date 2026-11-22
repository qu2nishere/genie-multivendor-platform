// Cấu hình hoa hồng — API 11.8 (UC-CAT-14 bước 8–9; plan B2 mục 3.3, C05, D1, D2, N4, N6)
package com.genie.catalog.category.application;

import com.genie.catalog.category.domain.Category;
import com.genie.catalog.category.domain.CategoryRepository;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.security.ClientInfo;
import com.genie.common.settings.PlatformSettings;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mặc định toàn sàn ({@code platform_settings.commission.default_rate}) + hoa hồng riêng của danh mục gốc. Khung 3,5–8 %
 * (D1). Mỗi thay đổi thật ghi một dòng {@code COMMISSION_UPDATED} {@code {scope, category_id, from, to}} (phần trăm).
 * Tỷ lệ mới chỉ áp cho đơn mới — đơn cũ giữ bản chụp ở {@code order_items} (N6).
 */
@Service
public class CommissionService {

    /** Một dòng hoa hồng theo ngành của màn cấu hình. */
    public record CategoryRate(long categoryId, String name, BigDecimal rate, boolean isEnabled) {
    }

    /** API 11.8 — {@code { default_rate, categories[] }} (phần trăm). */
    public record CommissionView(BigDecimal defaultRate, List<CategoryRate> categories) {
    }

    /** Yêu cầu đổi một ngành: {@code isEnabled = false} → bỏ tỷ lệ riêng (dùng mặc định). */
    public record CategoryRateChange(Long categoryId, BigDecimal rate, Boolean isEnabled) {
    }

    private final CategoryRepository categories;
    private final CategoryCatalog catalog;
    private final PlatformSettings settings;
    private final AuditLogger audit;

    public CommissionService(CategoryRepository categories, CategoryCatalog catalog, PlatformSettings settings,
                             AuditLogger audit) {
        this.categories = categories;
        this.catalog = catalog;
        this.settings = settings;
        this.audit = audit;
    }

    /** Hoa hồng mặc định toàn sàn (phân số). */
    public BigDecimal defaultRate() {
        return settings.decimal(PlatformSettings.DEFAULT_COMMISSION_RATE, CommissionRates.FALLBACK_DEFAULT);
    }

    /** Tỷ lệ áp dụng thực tế cho một ngành: riêng của ngành → ngành cha gần nhất có cấu hình → mặc định (phân số). */
    public BigDecimal effectiveRate(CategoryIndex idx, long categoryId, BigDecimal defaultRate) {
        CategoryRow r = idx.find(categoryId).orElse(null);
        int guard = 0;
        while (r != null && guard++ < 50) {
            if (r.commissionRate() != null) return r.commissionRate();
            r = r.parentId() == null ? null : idx.find(r.parentId()).orElse(null);
        }
        return defaultRate;
    }

    @Transactional(readOnly = true)
    public CommissionView settings() {
        return view(catalog.freshIndex(), defaultRate());
    }

    /**
     * Đổi mặc định và / hoặc hoa hồng từng ngành gốc trong một transaction. Ngành không tồn tại → {@code 404} cả lô;
     * tỷ lệ ngoài 3,5–8 % → {@code 422}.
     */
    @Transactional
    public CommissionView update(BigDecimal defaultPercent, List<CategoryRateChange> changes, long actorId,
                                 ClientInfo client) {
        List<InvalidRequestException.Violation> errors = new ArrayList<>();
        BigDecimal newDefault = CommissionRates.toFraction(defaultPercent);
        if (defaultPercent != null && !CommissionRates.inRange(newDefault)) {
            errors.add(new InvalidRequestException.Violation("default_rate", "OUT_OF_RANGE", defaultPercent,
                    "Hoa hồng phải trong khung 3,5–8 %."));
        }
        List<CategoryRateChange> items = changes == null ? List.of() : changes;
        for (int i = 0; i < items.size(); i++) {
            CategoryRateChange c = items.get(i);
            if (c.categoryId() == null) {
                errors.add(new InvalidRequestException.Violation("categories[" + i + "].category_id", "REQUIRED", null,
                        "Thiếu category_id."));
            } else if (!Boolean.FALSE.equals(c.isEnabled())
                    && !CommissionRates.inRange(CommissionRates.toFraction(c.rate()))) {
                errors.add(new InvalidRequestException.Violation("categories[" + i + "].rate", "OUT_OF_RANGE",
                        c.rate(), "Hoa hồng phải trong khung 3,5–8 %."));
            }
        }
        if (defaultPercent == null && items.isEmpty()) {
            errors.add(new InvalidRequestException.Violation("default_rate", "REQUIRED", null,
                    "Cần default_rate hoặc categories."));
        }
        if (!errors.isEmpty()) throw new InvalidRequestException(errors);

        if (newDefault != null) {
            BigDecimal old = settings.putDecimal(PlatformSettings.DEFAULT_COMMISSION_RATE, newDefault, actorId);
            BigDecimal before = old == null ? CommissionRates.FALLBACK_DEFAULT : old;
            if (before.compareTo(newDefault) != 0) {
                record("DEFAULT", null, before, newDefault, actorId, client);
            }
        }
        if (!items.isEmpty()) {
            Set<Long> ids = items.stream().map(CategoryRateChange::categoryId).collect(Collectors.toSet());
            Map<Long, Category> locked = new HashMap<>();
            categories.findAllByIdForUpdate(ids).forEach(c -> locked.put(c.getId(), c));
            if (locked.size() != ids.size()) throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            for (CategoryRateChange c : items) {
                Category cat = locked.get(c.categoryId());
                BigDecimal before = cat.getCommissionRate();
                if (Boolean.FALSE.equals(c.isEnabled())) {
                    cat.clearCommissionRate();
                } else {
                    cat.changeCommissionRate(CommissionRates.toFraction(c.rate()));
                }
                BigDecimal after = cat.getCommissionRate();
                if (!sameRate(before, after)) record("CATEGORY", cat.getId(), before, after, actorId, client);
            }
            categories.flush();   // đọc lại bằng JDBC ngay sau đây
            catalog.evictAfterCommit();
        }
        return view(catalog.freshIndex(), newDefault != null ? newDefault : defaultRate());
    }

    /** Ghi {@code COMMISSION_UPDATED} — dùng chung với màn quản lý cây (đổi hoa hồng trong modal danh mục). */
    void record(String scope, Long categoryId, BigDecimal from, BigDecimal to, long actorId, ClientInfo client) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("scope", scope);
        details.put("category_id", categoryId);
        details.put("from", CommissionRates.toPercent(from));
        details.put("to", CommissionRates.toPercent(to));
        audit.record(AuditAction.COMMISSION_UPDATED, actorId, categoryId == null ? "DEFAULT" : String.valueOf(categoryId),
                details, client, true);
    }

    static boolean sameRate(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    private CommissionView view(CategoryIndex idx, BigDecimal defaultRate) {
        List<CategoryRate> rows = idx.roots().stream().map(r -> new CategoryRate(r.id(), r.name(),
                CommissionRates.toPercent(r.commissionRate() != null ? r.commissionRate() : defaultRate),
                r.commissionRate() != null)).toList();
        return new CommissionView(CommissionRates.toPercent(defaultRate), rows);
    }
}
