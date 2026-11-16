package com.genie.auth.deletion.application;

import com.genie.auth.deletion.event.AccountDeletionRequestedEvent;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.token.application.SessionService;
import com.genie.auth.token.domain.RevokeReason;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yêu cầu / hủy xóa tài khoản — UC-AUTH-10, API_SPEC 3.7.
 * <ul>
 *   <li>Yêu cầu (đã qua OTP {@code ACCOUNT_DELETION}): kiểm lại điều kiện chặn, ghi {@code deletion_requested_at},
 *       đăng xuất các thiết bị KHÁC (giữ phiên hiện tại để khách thấy băng đỏ và nút "Hủy yêu cầu xóa"), nhật ký,
 *       email báo ngày ẩn danh. Gọi lại khi đã có yêu cầu → trả yêu cầu cũ (không tính lại 30 ngày).</li>
 *   <li>Hủy (trong 30 ngày): xóa {@code deletion_requested_at}, nhật ký. Không có yêu cầu → vẫn {@code 204}.</li>
 *   <li>Sau 30 ngày {@link AnonymizeAccountsJob} ẩn danh dữ liệu cá nhân, giữ đơn hàng và sổ cái.</li>
 * </ul>
 */
@Service
public class AccountDeletionService {

    /** Thời gian chờ trước khi ẩn danh — đổi được ý trong khoảng này. */
    public static final Duration GRACE_PERIOD = Duration.ofDays(30);

    private final UserRepository users;
    private final AccountDeletionGuard guard;
    private final SessionService sessions;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AccountDeletionService(UserRepository users, AccountDeletionGuard guard, SessionService sessions,
                                  AuditLogger audit, ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.guard = guard;
        this.sessions = sessions;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public DeletionRequestView request(AuthUser me, ClientInfo client) {
        User user = users.findByIdForUpdate(me.id()).filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (user.isPendingDeletion()) {
            return view(user.getDeletionRequestedAt());
        }
        guard.ensureDeletable(user.getId());

        OffsetDateTime now = OffsetDateTime.now(clock);
        user.requestDeletion(now);
        if (me.sessionId() != null) {
            sessions.revokeOtherSessions(user.getId(), me.sessionId(), RevokeReason.ACCOUNT_DELETED, client);
        }
        audit.record(AuditAction.ACCOUNT_DELETION_REQUESTED, user.getId(), null,
                Map.of("anonymize_at", now.plus(GRACE_PERIOD).toString()), client, true);
        DeletionRequestView view = view(now);
        events.publishEvent(new AccountDeletionRequestedEvent(user.getId(), user.getEmail(), now, view.anonymizeAt()));
        return view;
    }

    @Transactional
    public void cancel(AuthUser me, ClientInfo client) {
        User user = users.findByIdForUpdate(me.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!user.isPendingDeletion()) {
            return;
        }
        user.cancelDeletion();
        audit.record(AuditAction.ACCOUNT_DELETION_CANCELLED, user.getId(), null, null, client, true);
    }

    private static DeletionRequestView view(OffsetDateTime requestedAt) {
        return new DeletionRequestView(requestedAt, requestedAt.plus(GRACE_PERIOD));
    }
}
