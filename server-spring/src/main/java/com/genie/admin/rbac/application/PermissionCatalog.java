// Danh mục 13 mã quyền + bộ mặc định theo chức danh (UC-ADM-02 bước 1; API 11.18 GET /admin/permission-catalog; plan B1 R03.02)
package com.genie.admin.rbac.application;

import com.genie.admin.rbac.domain.StaffPermissionEnum;
import com.genie.admin.staff.domain.StaffTitle;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mô tả tiếng Việt theo UC-ADM-02 bước 1 (client hiển thị theo khóa i18n {@code admin.perm.*}). */
public final class PermissionCatalog {

    private static final Map<StaffPermissionEnum, String> DESCRIPTIONS = new EnumMap<>(StaffPermissionEnum.class);

    static {
        DESCRIPTIONS.put(StaffPermissionEnum.VIEW_DASHBOARD, "Xem trang Tổng quan");
        DESCRIPTIONS.put(StaffPermissionEnum.REVIEW_KYC, "Duyệt hồ sơ KYC người bán");
        DESCRIPTIONS.put(StaffPermissionEnum.MANAGE_CATALOG, "Quản lý danh mục, duyệt sản phẩm");
        DESCRIPTIONS.put(StaffPermissionEnum.MODERATE_REVIEW, "Kiểm duyệt đánh giá");
        DESCRIPTIONS.put(StaffPermissionEnum.HANDLE_DISPUTE, "Xử lý trả hàng & hoàn tiền");
        DESCRIPTIONS.put(StaffPermissionEnum.PROCESS_PAYOUT, "Xử lý rút tiền");
        DESCRIPTIONS.put(StaffPermissionEnum.UPDATE_COMMISSION, "Cấu hình hoa hồng");
        DESCRIPTIONS.put(StaffPermissionEnum.VIEW_FINANCE, "Xem sổ cái tài chính");
        DESCRIPTIONS.put(StaffPermissionEnum.MANAGE_VOUCHER, "Quản lý voucher sàn, Flash Sale, banner");
        DESCRIPTIONS.put(StaffPermissionEnum.MANAGE_PLATFORM_NOTIFICATION, "Phát thông báo sàn");
        DESCRIPTIONS.put(StaffPermissionEnum.VIEW_AUDIT_LOG, "Xem nhật ký hệ thống");
        DESCRIPTIONS.put(StaffPermissionEnum.MANAGE_USER, "Khóa / mở khóa người dùng");
        DESCRIPTIONS.put(StaffPermissionEnum.MANAGE_RBAC, "Quản lý nhân viên & phân quyền (chỉ Admin)");
    }

    public record Item(String code, String description) {
    }

    public record View(List<Item> permissions, Map<String, List<String>> titlePresets) {
    }

    private PermissionCatalog() {
    }

    public static View view() {
        List<Item> items = Arrays.stream(StaffPermissionEnum.values())
                .map(p -> new Item(p.name(), DESCRIPTIONS.get(p))).toList();
        Map<String, List<String>> presets = new LinkedHashMap<>();
        for (StaffTitle t : StaffTitle.values()) {
            presets.put(t.name(), Arrays.stream(StaffPermissionEnum.values())
                    .filter(t.defaultPermissions()::contains).map(Enum::name).toList());
        }
        return new View(items, presets);
    }
}
