// Cấp / thu hồi mã quyền chi tiết của nhân viên theo chênh lệch (UC-ADM-02; API 11.18; plan B1 R03.03, D8, D9)
package com.genie.admin.rbac.application;

import com.genie.admin.rbac.domain.StaffPermission;
import com.genie.admin.rbac.domain.StaffPermissionEnum;
import com.genie.admin.rbac.domain.StaffPermissionRepository;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Server lưu <b>đúng</b> danh sách quyền gửi lên (bộ mặc định theo chức danh chỉ để giao diện nạp sẵn). Thiếu → thêm dòng
 * {@code staff_permissions} ({@code PERMISSION_GRANTED}); thừa → {@code revoke(now)} ({@code PERMISSION_REVOKED}); không
 * đổi → không ghi log. {@code MANAGE_RBAC} không cấp cho STAFF ({@code 403 GNE-SEC-4031}). Có cả {@code PROCESS_PAYOUT}
 * và {@code UPDATE_COMMISSION} → vẫn lưu, trả cảnh báo {@code SEGREGATION_OF_DUTIES}. Sau commit xóa cache quyền.
 */
@Service
public class PermissionService {

    public static final String SEGREGATION_OF_DUTIES = "SEGREGATION_OF_DUTIES";

    private final StaffPermissionRepository permissions;
    private final StaffPermissionAuthorityProvider authorities;
    private final AuditLogger audit;
    private final Clock clock;

    public PermissionService(StaffPermissionRepository permissions, StaffPermissionAuthorityProvider authorities,
                             AuditLogger audit, Clock clock) {
        this.permissions = permissions;
        this.authorities = authorities;
        this.audit = audit;
        this.clock = clock;
    }

    /** Kết quả: có thay đổi không + cảnh báo (rỗng hoặc {@code SEGREGATION_OF_DUTIES}). */
    public record Change(boolean changed, List<String> warnings) {
    }

    /** Ném {@code GNE-SEC-4031} khi danh sách có {@code MANAGE_RBAC}. */
    public static void requireGrantable(Set<StaffPermissionEnum> wanted) {
        if (wanted.contains(StaffPermissionEnum.MANAGE_RBAC)) {
            throw new BusinessException(ErrorCode.INVALID_PERMISSION_GRANT);
        }
    }

    public static List<String> warningsOf(Set<StaffPermissionEnum> perms) {
        return perms.contains(StaffPermissionEnum.PROCESS_PAYOUT) && perms.contains(StaffPermissionEnum.UPDATE_COMMISSION)
                ? List.of(SEGREGATION_OF_DUTIES) : List.of();
    }

    /** Quyền còn hiệu lực của một nhân viên. */
    @Transactional(readOnly = true)
    public Set<StaffPermissionEnum> activeOf(long staffUserId) {
        Set<StaffPermissionEnum> result = EnumSet.noneOf(StaffPermissionEnum.class);
        permissions.findByStaffUserIdAndRevokedAtIsNull(staffUserId).stream()
                .filter(StaffPermission::isActive).forEach(p -> result.add(p.getPermission()));
        return result;
    }

    /** Thay bộ quyền của nhân viên bằng {@code wanted} — gọi trong transaction của luồng (mời / sửa quyền / xóa). */
    @Transactional
    public Change replace(long staffUserId, Set<StaffPermissionEnum> wanted, long actorId, ClientInfo client) {
        requireGrantable(wanted);
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<String> warnings = warningsOf(wanted);
        Set<StaffPermissionEnum> kept = EnumSet.noneOf(StaffPermissionEnum.class);
        boolean changed = false;
        for (StaffPermission p : permissions.findByStaffUserIdAndRevokedAtIsNull(staffUserId)) {
            if (!p.isActive()) {
                continue;
            }
            if (wanted.contains(p.getPermission()) && kept.add(p.getPermission())) {
                continue;
            }
            p.revoke(now);   // không còn trong danh sách mới (hoặc dòng trùng)
            changed = true;
            audit.record(AuditAction.PERMISSION_REVOKED, actorId, String.valueOf(staffUserId),
                    Map.of("permission", p.getPermission().name()), client, true);
        }
        for (StaffPermissionEnum code : wanted) {
            if (kept.contains(code)) {
                continue;
            }
            permissions.save(StaffPermission.builder().staffUserId(staffUserId).permission(code)
                    .grantedByAdminId(actorId).grantedAt(now).build());
            changed = true;
            Map<String, Object> details = new HashMap<>();
            details.put("permission", code.name());
            if (!warnings.isEmpty()) {
                details.put("sod_warning", true);
            }
            audit.record(AuditAction.PERMISSION_GRANTED, actorId, String.valueOf(staffUserId), details, client, true);
        }
        if (changed) {
            evictAfterCommit(staffUserId);
        }
        return new Change(changed, warnings);
    }

    /** Xóa cache quyền sau commit (đổi quyền, khóa, xóa nhân viên). */
    public void evictAfterCommit(long staffUserId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            authorities.evict(staffUserId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                authorities.evict(staffUserId);
            }
        });
    }
}
