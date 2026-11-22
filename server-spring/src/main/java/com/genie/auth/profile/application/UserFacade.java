package com.genie.auth.profile.application;

import com.genie.auth.profile.domain.AccountStatus;
import com.genie.auth.staff.application.StaffInvitationStore;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.token.application.SessionService;
import com.genie.auth.token.domain.RevokeReason;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.domain.UserRole;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.mail.EmailSender;
import com.genie.common.mail.MailTemplate;
import com.genie.common.security.ClientInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cửa công khai của module {@code auth} cho module khác (TS 4.4, LLD 3; plan AUTH A10.01). Chỉ trả DTO, không trả entity.
 * <ul>
 *   <li>{@link #findSummary} — tên / ảnh / vai trò (chat, đánh giá, đơn hàng…);</li>
 *   <li>{@link #promoteToVendor} — UC-VND-01 mở shop: CUSTOMER → VENDOR, GIỮ phiên; token làm mới mang vai trò mới;</li>
 *   <li>{@link #suspend} / {@link #reactivate} — UC-ADM-03: đình chỉ thì thu hồi mọi phiên và chặn ngay access token.</li>
 *   <li>Nhân viên (UC-ADM-01/02/03, plan B1): {@link #createStaffAccount}, {@link #sendStaffInvitation},
 *       {@link #invitationsRemaining}, {@link #listStaffAccounts}, {@link #lockStaff}, {@link #deleteStaff} — module
 *       {@code admin} điều phối, {@code auth} không gọi ngược {@code admin}. Đổi quyền KHÔNG thu hồi phiên (plan B1 D19).</li>
 * </ul>
 */
@Component
public class UserFacade {

    private final UserRepository users;
    private final AccountSummaryService summaries;
    private final SessionService sessions;
    private final AuditLogger audit;
    private final StaffInvitationStore invitations;
    private final EmailSender emailSender;
    private final Clock clock;
    private final String webUrl;

    public UserFacade(UserRepository users, AccountSummaryService summaries, SessionService sessions, AuditLogger audit,
                      StaffInvitationStore invitations, EmailSender emailSender, Clock clock,
                      @Value("${genie.web-url:http://localhost:4321}") String webUrl) {
        this.users = users;
        this.summaries = summaries;
        this.sessions = sessions;
        this.audit = audit;
        this.invitations = invitations;
        this.emailSender = emailSender;
        this.clock = clock;
        this.webUrl = webUrl;
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

    /**
     * Mở shop thành công (UC-VND-01) — chỉ đổi vai trò, KHÔNG thu hồi phiên (plan Mở shop & KYC D11): người dùng vẫn đang
     * ở giữa luồng mở shop; {@code POST /auth/sessions/refresh} cấp access token mới đọc vai trò từ CSDL → có quyền
     * {@code /seller} ngay (API_SPEC 9.2). Access token cũ (vai trò CUSTOMER) vẫn mua hàng bình thường tới khi hết hạn.
     */
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
        reactivate(userId, actorId, client, "ADMIN");
    }

    /** Mở khóa thủ công kèm lý do Admin nhập (UC-ADM-03 — API 11.13 {@code reactivate.reason}). */
    @Transactional
    public void reactivate(long userId, Long actorId, ClientInfo client, String reason) {
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (user.getStatus() == AccountStatus.ACTIVE && user.getLockedUntil() == null) {
            return;
        }
        if (user.getStatus() == AccountStatus.ACTIVE) {
            user.clearLock();
        } else {
            user.reactivate();
        }
        audit.record(AuditAction.ACCOUNT_REACTIVATED, actorId, String.valueOf(userId),
                Map.of("reason", reason == null ? "ADMIN" : reason), client, true);
    }

    // ===== Nhân viên & lời mời (UC-ADM-01/02/03 — plan B1) =====

    /** Hạn của liên kết mời (48 giờ) — module {@code admin} tính {@code invitation_expires_at} (D5). */
    public static final Duration STAFF_INVITATION_TTL = StaffInvitationStore.TTL;

    /** Tạo tài khoản nhân viên chờ kích hoạt (bản ghi lời mời — D1). Email đã có (kể cả đang chờ xóa) → {@code 409 GNE-ADM-4091}. */
    @Transactional
    public long createStaffAccount(String email) {
        String normalized = User.normalizeEmail(email);
        Optional<String> owner = emailOwnerRole(normalized);
        if (owner.isPresent()) {
            throw new BusinessException(ErrorCode.STAFF_EMAIL_TAKEN, Map.<String, Object>of("taken_by", owner.get()));
        }
        return users.save(User.newStaff(normalized)).getId();
    }

    /**
     * Vai trò của tài khoản đang giữ email (CUSTOMER / VENDOR / STAFF / ADMIN); {@code OTHER} khi email bị giữ bởi tài
     * khoản không đọc được vai trò (ví dụ đang chờ xóa). Rỗng = email còn trống. Dùng cho cảnh báo tức thời ở form mời.
     */
    @Transactional(readOnly = true)
    public Optional<String> emailOwnerRole(String email) {
        String normalized = User.normalizeEmail(email);
        Optional<String> role = users.findByEmail(normalized).map(u -> u.getRole().name());
        if (role.isPresent()) return role;
        return users.existsByEmail(normalized) ? Optional.of("OTHER") : Optional.empty();
    }

    /** Nhân viên còn ở trạng thái chờ kích hoạt (gửi lại lời mời chỉ hợp lệ khi đúng). */
    @Transactional(readOnly = true)
    public boolean isPendingStaff(long userId) {
        return users.findById(userId).map(User::isPendingStaffActivation).orElse(false);
    }

    /**
     * Sinh mã lời mời mới (vô hiệu mã cũ, hạn 48 giờ) rồi gửi email {@code STAFF_INVITATION}. Module {@code admin} gọi
     * sau commit. Không còn chờ kích hoạt → {@code 409 GNE-ADM-4093}.
     */
    public void sendStaffInvitation(long userId, StaffInvitationMail mail) {
        User user = users.findById(userId).filter(User::isPendingStaffActivation)
                .orElseThrow(() -> new BusinessException(ErrorCode.STAFF_NOT_PENDING));
        String token = invitations.issue(userId);
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", mail.fullName());
        vars.put("inviterName", mail.inviterName());
        vars.put("staffTitle", mail.staffTitleLabel());
        vars.put("activationUrl", webUrl + "/auth/staff-activation?token=" + token);
        vars.put("expiresHours", StaffInvitationStore.TTL.toHours());
        emailSender.send(user.getEmail(), MailTemplate.STAFF_INVITATION, vars, Locale.forLanguageTag("vi"));
    }

    /** Thời gian còn lại của lời mời theo nhân viên — không có trong kết quả = lời mời đã hết hạn (D5). */
    public Map<Long, Duration> invitationsRemaining(Collection<Long> userIds) {
        return userIds.isEmpty() ? Map.of() : invitations.remaining(userIds);
    }

    /** Mọi tài khoản STAFF / ADMIN chưa xóa (danh sách nhân viên — D12). */
    @Transactional(readOnly = true)
    public List<StaffAccountView> listStaffAccounts() {
        return users.findInternalAccounts().stream()
                .map(u -> new StaffAccountView(u.getId(), u.getEmail(), u.getRole(), u.getStatus().name()))
                .toList();
    }

    /** Một tài khoản nội bộ STAFF / ADMIN chưa xóa; khác (khách, shop, không tồn tại) → rỗng. */
    @Transactional(readOnly = true)
    public Optional<StaffAccountView> findStaffAccount(long userId) {
        return users.findById(userId)
                .filter(u -> u.getRole() == UserRole.STAFF || u.getRole() == UserRole.ADMIN)
                .filter(u -> u.getStatus() != AccountStatus.ANONYMIZED && u.getStatus() != AccountStatus.DELETED)
                .map(u -> new StaffAccountView(u.getId(), u.getEmail(), u.getRole(), u.getStatus().name()));
    }

    /** Khóa nhân viên vô thời hạn (D6): thu hồi phiên, ghi {@code ACCOUNT_LOCKED}. */
    @Transactional
    public void lockStaff(long userId, String reason, Long actorId, ClientInfo client) {
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        user.suspend(reason, null);
        sessions.revokeAllSessions(userId, RevokeReason.SUSPENDED);
        audit.record(AuditAction.ACCOUNT_LOCKED, actorId, String.valueOf(userId), Map.of("reason", reason), client, true);
    }

    /**
     * Xóa mềm nhân viên (D7): hủy lời mời, thu hồi phiên, ẩn danh hóa (giải phóng email để mời lại; {@code id} giữ cho
     * {@code system_logs}). Ghi {@code PII_ANONYMIZED} với {@code details.reason = STAFF_DELETED}.
     */
    @Transactional
    public void deleteStaff(long userId, Long actorId, ClientInfo client) {
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        // Ẩn danh hóa TRƯỚC khi thu hồi phiên: truy vấn thu hồi dùng clearAutomatically → xóa persistence context, sửa
        // entity sau đó sẽ không được lưu (cùng thứ tự với suspend / lockStaff — flushAutomatically ghi thay đổi trước)
        user.anonymize();
        sessions.revokeAllSessions(userId, RevokeReason.ACCOUNT_DELETED);
        invitations.cancel(userId);
        audit.record(AuditAction.PII_ANONYMIZED, actorId, String.valueOf(userId),
                Map.of("reason", "STAFF_DELETED", "at", OffsetDateTime.now(clock)), client, true);
    }
}
