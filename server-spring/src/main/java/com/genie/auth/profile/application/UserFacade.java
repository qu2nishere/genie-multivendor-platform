package com.genie.auth.profile.application;

import com.genie.auth.profile.domain.AccountStatus;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.token.application.SessionService;
import com.genie.auth.token.domain.RevokeReason;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.domain.UserRole;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cửa công khai của module {@code auth} cho module khác (TS 4.4, LLD 3; plan AUTH A10.01). Chỉ trả DTO, không trả entity.
 * <ul>
 *   <li>{@link #findSummary} — tên / ảnh / vai trò (chat, đánh giá, đơn hàng…);</li>
 *   <li>{@link #promoteToVendor} — UC-VND-01 mở shop: CUSTOMER → VENDOR, thu hồi phiên để token mới mang vai trò mới;</li>
 *   <li>{@link #suspend} / {@link #reactivate} — UC-ADM-03: đình chỉ thì thu hồi mọi phiên và chặn ngay access token.</li>
 * </ul>
 */
@Component
public class UserFacade {

    private final UserRepository users;
    private final AccountSummaryService summaries;
    private final SessionService sessions;
    private final AuditLogger audit;

    public UserFacade(UserRepository users, AccountSummaryService summaries, SessionService sessions, AuditLogger audit) {
        this.users = users;
        this.summaries = summaries;
        this.sessions = sessions;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Optional<UserSummary> findSummary(long userId) {
        return users.findById(userId).map(summaries::summaryOf);
    }

    @Transactional(readOnly = true)
    public boolean isActive(long userId) {
        return users.findById(userId).map(User::isActive).orElse(false);
    }

    /** Các id đang ACTIVE trong tập đưa vào — một truy vấn cho cả trang (vd. lọc shop có chủ còn hoạt động). */
    @Transactional(readOnly = true)
    public java.util.Set<Long> activeAmong(java.util.Collection<Long> userIds) {
        return userIds.isEmpty() ? java.util.Set.of() : new java.util.HashSet<>(users.findActiveIds(userIds));
    }

    @Transactional(readOnly = true)
    public Optional<String> emailOf(long userId) {
        return users.findById(userId).map(User::getEmail);
    }

    /** Mở shop thành công (UC-VND-01) — vai trò mới có hiệu lực từ lần đăng nhập / làm mới tiếp theo. */
    @Transactional
    public void promoteToVendor(long userId) {
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (user.getRole() == UserRole.VENDOR) {
            return;
        }
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Chỉ tài khoản người mua mới mở shop được");
        }
        user.setRole(UserRole.VENDOR);
        sessions.revokeAllSessions(userId, RevokeReason.ROLE_CHANGED);
    }

    /** Đình chỉ tài khoản (UC-ADM-03). {@code until} null = vô thời hạn. */
    @Transactional
    public void suspend(long userId, String reason, OffsetDateTime until, Long actorId, ClientInfo client) {
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        user.suspend(reason, until);
        sessions.revokeAllSessions(userId, RevokeReason.SUSPENDED);
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("until", until);
        audit.record(AuditAction.ACCOUNT_SUSPENDED, actorId, String.valueOf(userId), details, client, true);
    }

    /** Mở khóa thủ công (UC-ADM-03). */
    @Transactional
    public void reactivate(long userId, Long actorId, ClientInfo client) {
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (user.getStatus() == AccountStatus.ACTIVE && user.getLockedUntil() == null) {
            return;
        }
        if (user.getStatus() == AccountStatus.ACTIVE) {
            user.clearLock();
        } else {
            user.reactivate();
        }
        audit.record(AuditAction.ACCOUNT_REACTIVATED, actorId, String.valueOf(userId), Map.of("reason", "ADMIN"), client, true);
    }
}
