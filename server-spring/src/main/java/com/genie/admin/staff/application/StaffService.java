// Quản lý nhân viên nội bộ & lời mời + khung Admin (UC-ADM-01/02/03; API 11.1, 11.18; plan B1 R04, mục 3.1–3.7)
package com.genie.admin.staff.application;

import com.genie.admin.rbac.application.PermissionService;
import com.genie.admin.rbac.domain.StaffPermission;
import com.genie.admin.rbac.domain.StaffPermissionEnum;
import com.genie.admin.rbac.domain.StaffPermissionRepository;
import com.genie.admin.staff.domain.AdminProfile;
import com.genie.admin.staff.domain.AdminProfileRepository;
import com.genie.admin.staff.domain.StaffTitle;
import com.genie.auth.profile.application.StaffAccountView;
import com.genie.auth.profile.application.UserFacade;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.audit.SystemLogRepository;
import com.genie.common.domain.NameNormalizer;
import com.genie.common.domain.UserRole;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.realtime.UserChannel;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.RateLimiter;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Module {@code admin} điều phối: tài khoản / phiên / mã lời mời qua {@link UserFacade} ({@code auth}), hồ sơ và quyền ở
 * {@code admin_profiles}, {@code staff_permissions}. Không có bảng lời mời (D1): trạng thái lời mời suy từ khóa Redis còn
 * hạn + dòng {@code STAFF_INVITED} mới nhất (D5).
 */
@Service
public class StaffService {

    public static final String CUSTOM = "CUSTOM";
    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 60;
    static final int RESEND_PER_HOUR = 3;

    private final UserFacade users;
    private final AdminProfileRepository profiles;
    private final StaffPermissionRepository permissionRows;
    private final PermissionService permissions;
    private final SystemLogRepository logs;
    private final AuditLogger audit;
    private final RateLimiter rateLimiter;
    private final UploadService uploads;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    /** Tên miền email được phép (chữ thường, không @); rỗng = mọi email hợp lệ (plan B1 D3 — chốt 2026-11-20). */
    private final Set<String> allowedDomains;
    private final UserChannel realtime;

    /** Tin realtime khi quyền / chức danh của nhân viên đổi (API_SPEC 13 — kênh {@code /user/queue/account}). */
    public static final String PERMISSIONS_CHANGED = "PERMISSIONS_CHANGED";

    public StaffService(UserFacade users, AdminProfileRepository profiles, StaffPermissionRepository permissionRows,
                        PermissionService permissions, SystemLogRepository logs, AuditLogger audit,
                        RateLimiter rateLimiter, UploadService uploads, ApplicationEventPublisher events, Clock clock,
                        @Value("${STAFF_EMAIL_DOMAINS:}") String allowedDomains, UserChannel realtime) {
        this.realtime = realtime;
        this.users = users;
        this.profiles = profiles;
        this.permissionRows = permissionRows;
        this.permissions = permissions;
        this.logs = logs;
        this.audit = audit;
        this.rateLimiter = rateLimiter;
        this.uploads = uploads;
        this.events = events;
        this.clock = clock;
        this.allowedDomains = parseDomains(allowedDomains);
    }

    /** {@code "genie.vn, Genie.com.vn"} → {@code {genie.vn, genie.com.vn}}; chuỗi rỗng → tập rỗng (không giới hạn). */
    static Set<String> parseDomains(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(d -> d.trim().toLowerCase(Locale.ROOT))
                .map(d -> d.startsWith("@") ? d.substring(1) : d)
                .filter(d -> !d.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Email nhân viên hợp lệ: đúng dạng {@code ten@mien.tld}; nếu có danh sách tên miền ({@code STAFF_EMAIL_DOMAINS},
     * dùng ở production) thì tên miền phải thuộc danh sách. Không còn bắt cứng {@code @genie.example} (D3).
     */
    static boolean isAllowedEmail(String normalizedEmail, Set<String> domains) {
        if (normalizedEmail == null || !normalizedEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return false;
        String domain = normalizedEmail.substring(normalizedEmail.indexOf('@') + 1);
        return domains.isEmpty() || domains.contains(domain);
    }

    // ===== Kiểm tra email trước khi mời (cảnh báo tức thời) =====

    /**
     * Kiểm tra nhanh email ở form mời: sai định dạng, ngoài tên miền cho phép, hay đã thuộc tài khoản khác (kèm vai trò
     * để giao diện nói rõ "đang là khách hàng / người bán / nhân viên"). Không ghi gì — {@link #invite} vẫn kiểm lại.
     */
    public StaffEmailCheckView checkEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!isAllowedEmail(normalized, Set.of())) {
            return new StaffEmailCheckView(normalized, false, "INVALID", null);
        }
        if (!isAllowedEmail(normalized, allowedDomains)) {
            return new StaffEmailCheckView(normalized, false, "DOMAIN", null);
        }
        return users.emailOwnerRole(normalized)
                .map(role -> new StaffEmailCheckView(normalized, false, "TAKEN", role))
                .orElseGet(() -> new StaffEmailCheckView(normalized, true, null, null));
    }

    // ===== Danh sách (D12, D5) =====

    @Transactional(readOnly = true)
    public Page<StaffView> list(String q, Integer page, Integer size, long meId) {
        int p = page == null ? 1 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        List<InvalidRequestException.Violation> errors = new ArrayList<>();
        if (p < 1) errors.add(new InvalidRequestException.Violation("page", "OUT_OF_RANGE", page, "page phải từ 1."));
        if (s < 1 || s > MAX_SIZE) {
            errors.add(new InvalidRequestException.Violation("size", "OUT_OF_RANGE", size, "size phải từ 1 đến 60."));
        }
        if (!errors.isEmpty()) throw new InvalidRequestException(errors);

        List<StaffView> all = views(users.listStaffAccounts(), meId);
        String needle = q == null ? "" : NameNormalizer.normalize(q);
        List<StaffView> filtered = needle.isEmpty() ? all : all.stream()
                .filter(v -> NameNormalizer.normalize(v.fullName() + " " + v.email()).contains(needle))
                .toList();
        int from = Math.min((p - 1) * s, filtered.size());
        int to = Math.min(from + s, filtered.size());
        return new PageImpl<>(filtered.subList(from, to), PageRequest.of(p - 1, s), filtered.size());
    }

    private List<StaffView> views(List<StaffAccountView> accounts, long meId) {
        List<Long> ids = accounts.stream().map(StaffAccountView::id).toList();
        if (ids.isEmpty()) return List.of();
        Map<Long, AdminProfile> byUser = profiles.findAllByUserIdIn(ids).stream()
                .collect(Collectors.toMap(AdminProfile::getUserId, Function.identity(), (a, b) -> a));
        Map<Long, Set<StaffPermissionEnum>> perms = new HashMap<>();
        for (StaffPermission row : permissionRows.findByStaffUserIdInAndRevokedAtIsNull(ids)) {
            if (row.isActive()) {
                perms.computeIfAbsent(row.getStaffUserId(), k -> EnumSet.noneOf(StaffPermissionEnum.class))
                        .add(row.getPermission());
            }
        }
        List<Long> pending = accounts.stream().filter(StaffAccountView::isPendingActivation).map(StaffAccountView::id)
                .toList();
        Map<Long, Duration> remaining = users.invitationsRemaining(pending);
        Map<Long, OffsetDateTime> invitedAt = lastInvitedAt(pending);
        OffsetDateTime now = OffsetDateTime.now(clock);
        return accounts.stream()
                .sorted(Comparator.comparing((StaffAccountView a) -> a.role() != UserRole.ADMIN)
                        .thenComparingLong(StaffAccountView::id))
                .map(a -> {
                    OffsetDateTime expiresAt = null;
                    if (a.isPendingActivation()) {
                        OffsetDateTime sent = invitedAt.get(a.id());
                        expiresAt = sent != null ? sent.plus(UserFacade.STAFF_INVITATION_TTL)
                                : remaining.containsKey(a.id()) ? now.plus(remaining.get(a.id())) : null;
                    }
                    return view(a, byUser.get(a.id()), perms.getOrDefault(a.id(), Set.of()),
                            statusOf(a, remaining.containsKey(a.id())), expiresAt, meId);
                })
                .toList();
    }

    private Map<Long, OffsetDateTime> lastInvitedAt(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<Long, OffsetDateTime> result = new HashMap<>();
        for (Object[] row : logs.findLatestByReference(AuditAction.STAFF_INVITED,
                ids.stream().map(String::valueOf).toList())) {
            try {
                result.put(Long.valueOf((String) row[0]), (OffsetDateTime) row[1]);
            } catch (RuntimeException ignored) {
                // reference_id không phải số — bỏ qua
            }
        }
        return result;
    }

    static String statusOf(StaffAccountView a, boolean invitationAlive) {
        if (a.isPendingActivation()) return invitationAlive ? "PENDING_ACTIVATION" : "INVITATION_EXPIRED";
        return a.isLocked() ? "LOCKED" : "ACTIVE";
    }

    private static StaffView view(StaffAccountView a, AdminProfile profile, Set<StaffPermissionEnum> perms,
                                  String status, OffsetDateTime expiresAt, long meId) {
        boolean admin = a.role() == UserRole.ADMIN;
        List<String> codes = admin ? Arrays.stream(StaffPermissionEnum.values()).map(Enum::name).toList()
                : Arrays.stream(StaffPermissionEnum.values()).filter(perms::contains).map(Enum::name).toList();
        String title = admin ? null : profile == null || profile.getStaffTitle() == null ? CUSTOM
                : profile.getStaffTitle().name();
        return new StaffView(a.id(), nameOf(profile, a.email()), a.email(), a.role().name(), title, codes, status,
                expiresAt, a.id() == meId, List.of());
    }

    private static String nameOf(AdminProfile profile, String email) {
        if (profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()) {
            return profile.getFullName();
        }
        return email == null ? "" : email.substring(0, Math.max(0, email.indexOf('@')));
    }

    private StaffView viewOf(long userId, long meId) {
        StaffAccountView a = users.findStaffAccount(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return views(List.of(a), meId).get(0);
    }

    // ===== Mời (3.1) & gửi lại (3.2) =====

    @Transactional
    public StaffView invite(long actorId, String fullName, String email, String staffTitle,
                            Collection<StaffPermissionEnum> requested, ClientInfo client) {
        String name = fullName == null ? "" : fullName.trim();
        if (name.length() < 2 || name.length() > 255) {
            throw new InvalidRequestException(List.of(new InvalidRequestException.Violation("full_name", "LENGTH",
                    fullName, "Họ tên 2–255 ký tự.")));
        }
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!isAllowedEmail(normalized, Set.of())) {
            throw new InvalidRequestException(List.of(new InvalidRequestException.Violation("email", "INVALID",
                    email, "Email không hợp lệ.")));
        }
        if (!isAllowedEmail(normalized, allowedDomains)) {
            throw new BusinessException(ErrorCode.STAFF_EMAIL_DOMAIN, Map.<String, Object>of("allowed_domains", allowedDomains));
        }
        StaffTitle title = parseTitle(staffTitle);
        Set<StaffPermissionEnum> wanted = toSet(requested);
        PermissionService.requireGrantable(wanted);

        long id = users.createStaffAccount(normalized);
        profiles.save(AdminProfile.builder().userId(id).fullName(name).staffTitle(title).build());
        PermissionService.Change change = permissions.replace(id, wanted, actorId, client);
        Map<String, Object> details = new HashMap<>();
        details.put("email", normalized);
        details.put("staff_title", title == null ? CUSTOM : title.name());
        details.put("permission_count", wanted.size());
        details.put("resend", false);
        audit.record(AuditAction.STAFF_INVITED, actorId, String.valueOf(id), details, client, true);
        events.publishEvent(new StaffInvitedEvent(id, actorId));

        List<String> codes = Arrays.stream(StaffPermissionEnum.values()).filter(wanted::contains).map(Enum::name)
                .toList();
        return new StaffView(id, name, normalized, UserRole.STAFF.name(), title == null ? CUSTOM : title.name(), codes,
                "PENDING_ACTIVATION", OffsetDateTime.now(clock).plus(UserFacade.STAFF_INVITATION_TTL), false,
                change.warnings());
    }

    @Transactional
    public void resend(long actorId, long staffId, ClientInfo client) {
        StaffAccountView a = staffOnly(staffId);
        if (!a.isPendingActivation()) throw new BusinessException(ErrorCode.STAFF_NOT_PENDING);
        rateLimiter.check("rl:staffinv:" + staffId, RESEND_PER_HOUR, Duration.ofHours(1));
        audit.record(AuditAction.STAFF_INVITED, actorId, String.valueOf(staffId),
                Map.of("email", a.email(), "resend", true), client, true);
        events.publishEvent(new StaffInvitedEvent(staffId, actorId));
    }

    // ===== Sửa quyền / chức danh (3.4) =====

    @Transactional
    public StaffView updatePermissions(long actorId, long staffId, String staffTitle,
                                       Collection<StaffPermissionEnum> requested, ClientInfo client) {
        StaffAccountView a = users.findStaffAccount(staffId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (a.role() == UserRole.ADMIN) throw new BusinessException(ErrorCode.INVALID_PERMISSION_GRANT);
        if (staffId == actorId) throw new BusinessException(ErrorCode.STAFF_SELF_OR_LAST_ADMIN);
        StaffTitle title = parseTitle(staffTitle);
        Set<StaffPermissionEnum> wanted = toSet(requested);
        PermissionService.requireGrantable(wanted);

        AdminProfile profile = profiles.findByUserIdForUpdate(staffId)
                .orElseGet(() -> profiles.save(AdminProfile.builder().userId(staffId)
                        .fullName(nameOf(null, a.email())).build()));
        boolean titleChanged = profile.getStaffTitle() != title;
        if (titleChanged) {
            String from = profile.getStaffTitle() == null ? CUSTOM : profile.getStaffTitle().name();
            profile.changeTitle(title);
            audit.record(AuditAction.RBAC_ROLE_CHANGED, actorId, String.valueOf(staffId),
                    Map.of("from", from, "to", title == null ? CUSTOM : title.name()), client, true);
        }
        PermissionService.Change change = permissions.replace(staffId, wanted, actorId, client);
        if (change.changed() || titleChanged) {
            // D19: KHÔNG thu hồi phiên — mã quyền đọc lại từ CSDL ở mỗi request (cache xóa sau commit) nên có hiệu lực
            // ngay ở request kế tiếp; chỉ báo client tải lại menu (/admin/me) qua kênh realtime /user/queue/account
            realtime.sendAfterCommit(staffId, UserChannel.ACCOUNT, Map.of("type", PERMISSIONS_CHANGED));
        }
        return viewOf(staffId, actorId).withWarnings(change.warnings());
    }

    // ===== Khóa / mở khóa / xóa (3.5) =====

    @Transactional
    public StaffView lock(long actorId, long staffId, String reason, ClientInfo client) {
        StaffAccountView a = staffOnly(staffId);
        if (staffId == actorId) throw new BusinessException(ErrorCode.STAFF_SELF_OR_LAST_ADMIN);
        if (a.isPendingActivation()) {
            // chưa kích hoạt thì không khóa (mở khóa sẽ thành ACTIVE mà chưa đặt mật khẩu) — xóa lời mời thay vì khóa
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Nhân viên chưa kích hoạt — hãy xóa lời mời thay vì khóa.");
        }
        String why = reason == null ? "" : reason.trim();
        if (why.isEmpty() || why.length() > 255) {
            throw new InvalidRequestException(List.of(new InvalidRequestException.Violation("reason", "REQUIRED",
                    reason, "Nhập lý do khóa (tối đa 255 ký tự).")));
        }
        if (!a.isLocked()) {
            users.lockStaff(staffId, why, actorId, client);
            permissions.evictAfterCommit(staffId);
        }
        return viewOf(staffId, actorId);
    }

    @Transactional
    public StaffView unlock(long actorId, long staffId, ClientInfo client) {
        StaffAccountView a = staffOnly(staffId);
        if (a.isLocked()) {
            users.reactivate(staffId, actorId, client, "ADMIN_UNLOCK");
            permissions.evictAfterCommit(staffId);
        }
        return viewOf(staffId, actorId);
    }

    @Transactional
    public void delete(long actorId, long staffId, ClientInfo client) {
        if (staffId == actorId) throw new BusinessException(ErrorCode.STAFF_SELF_OR_LAST_ADMIN);
        staffOnly(staffId);
        permissions.replace(staffId, Set.of(), actorId, client);
        users.deleteStaff(staffId, actorId, client);
        permissions.evictAfterCommit(staffId);
    }

    /** Chỉ thao tác trên STAFF — Admin được bảo vệ (không khóa / xóa qua màn này: luôn còn ≥ 1 Admin). */
    private StaffAccountView staffOnly(long staffId) {
        StaffAccountView a = users.findStaffAccount(staffId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (a.role() == UserRole.ADMIN) throw new BusinessException(ErrorCode.STAFF_SELF_OR_LAST_ADMIN);
        return a;
    }

    private static StaffTitle parseTitle(String raw) {
        if (raw == null || raw.isBlank() || CUSTOM.equals(raw)) return null;
        try {
            return StaffTitle.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(List.of(new InvalidRequestException.Violation("staff_title",
                    "INVALID", raw, "Chức danh không hợp lệ.")));
        }
    }

    private static Set<StaffPermissionEnum> toSet(Collection<StaffPermissionEnum> requested) {
        Set<StaffPermissionEnum> set = EnumSet.noneOf(StaffPermissionEnum.class);
        if (requested != null) requested.stream().filter(java.util.Objects::nonNull).forEach(set::add);
        return set;
    }

    // ===== Khung Admin — API 11.1 =====

    @Transactional(readOnly = true)
    public AdminMeView me(long userId) {
        StaffAccountView a = users.findStaffAccount(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        AdminProfile profile = profiles.findByUserId(userId).orElse(null);
        boolean admin = a.role() == UserRole.ADMIN;
        Set<StaffPermissionEnum> perms = admin ? EnumSet.allOf(StaffPermissionEnum.class) : permissions.activeOf(userId);
        String title = admin ? null : profile == null || profile.getStaffTitle() == null ? CUSTOM
                : profile.getStaffTitle().name();
        return new AdminMeView(userId, nameOf(profile, a.email()), a.email(),
                profile == null ? null : profile.getAvatarUrl(), a.role().name(), title,
                Arrays.stream(StaffPermissionEnum.values()).filter(perms::contains).map(Enum::name).toList(),
                statusOf(a, true));
    }

    @Transactional
    public AdminMeView updateMe(long userId, String fullName, String avatarObjectKey) {
        StaffAccountView a = users.findStaffAccount(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        AdminProfile profile = profiles.findByUserIdForUpdate(userId)
                .orElseGet(() -> profiles.save(AdminProfile.builder().userId(userId)
                        .fullName(nameOf(null, a.email())).build()));
        if (fullName != null) {
            String name = fullName.trim();
            if (name.length() < 2 || name.length() > 255) {
                throw new InvalidRequestException(List.of(new InvalidRequestException.Violation("full_name",
                        "LENGTH", fullName, "Họ tên 2–255 ký tự.")));
            }
            profile.setFullName(name);
        }
        if (avatarObjectKey != null && !avatarObjectKey.isBlank()) {
            String oldUrl = profile.getAvatarUrl();
            UploadService.StoredFile file = uploads.promote(userId, UploadPurpose.AVATAR, avatarObjectKey);
            profile.setAvatarUrl(file.publicUrl());
            uploads.keyOfPublicUrl(UploadPurpose.AVATAR, oldUrl).ifPresent(oldKey -> afterCommit(
                    () -> uploads.deleteQuietly(UploadPurpose.AVATAR, oldKey)));
        }
        return me(userId);
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
