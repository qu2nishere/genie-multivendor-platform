// Quản lý người dùng vi phạm — API 11.13 (UC-ADM-03, plan B1 mục 3.6, R05, D10, D11)
package com.genie.admin.users.application;

import com.genie.auth.profile.application.MemberDirectory;
import com.genie.auth.profile.application.MemberDirectory.Member;
import com.genie.auth.profile.application.MemberDirectory.MemberPage;
import com.genie.auth.profile.application.MemberDirectory.Tab;
import com.genie.auth.profile.application.UserFacade;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.security.ClientInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách 4 tab, khóa có thời hạn / vô thời hạn, mở khóa. Chỉ tác động tài khoản {@code CUSTOMER} / {@code VENDOR}:
 * đích là nhân viên / Admin / tài khoản đã xóa → {@code 404} (không lộ tài khoản nội bộ — khóa nhân viên ở 11.18).
 * Số đơn / cảnh báo / tên shop lấy qua cổng đảo ngược (D10) — chưa có module cài thì 0 / trống.
 */
@Service
public class UserAdminService {

    /** Mã lý do khóa — API 16.4 "Đình chỉ người dùng" (khớp {@code web/.../system.rules.ts SUSPEND_REASONS}). */
    public static final List<String> REASON_CODES = List.of("COUNTERFEIT_SELLER", "ABNORMAL_RETURNS", "REVIEW_SPAM",
            "FRAUD", "OTHER");
    static final String OTHER = "OTHER";
    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 60;
    /** Cột {@code users.suspension_reason} VARCHAR(500): {@code "<MÃ>: <ghi chú>"}. */
    static final int MAX_NOTE = 450;
    private static final Map<String, Duration> DURATIONS = Map.of("P7D", Duration.ofDays(7), "P30D", Duration.ofDays(30));

    private final MemberDirectory members;
    private final UserFacade users;
    private final ObjectProvider<UserOrderStats> orderStats;
    private final ObjectProvider<UserWarningStats> warningStats;
    private final ObjectProvider<ShopNameLookup> shopNames;
    private final Clock clock;

    public UserAdminService(MemberDirectory members, UserFacade users, ObjectProvider<UserOrderStats> orderStats,
                            ObjectProvider<UserWarningStats> warningStats, ObjectProvider<ShopNameLookup> shopNames,
                            Clock clock) {
        this.members = members;
        this.users = users;
        this.orderStats = orderStats;
        this.warningStats = warningStats;
        this.shopNames = shopNames;
        this.clock = clock;
    }

    /** Một trang người dùng + {@code summary.counts} theo 4 tab. */
    public record UserList(PageImpl<PlatformUserView> page, Map<String, Long> counts) {
    }

    // ===== Danh sách =====

    @Transactional(readOnly = true)
    public UserList list(String tab, String q, Integer page, Integer size) {
        int p = page == null ? 1 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        List<InvalidRequestException.Violation> errors = new ArrayList<>();
        Tab t = parseTab(tab, errors);
        if (p < 1) errors.add(new InvalidRequestException.Violation("page", "OUT_OF_RANGE", page, "page phải từ 1."));
        if (s < 1 || s > MAX_SIZE) {
            errors.add(new InvalidRequestException.Violation("size", "OUT_OF_RANGE", size, "size phải từ 1 đến 60."));
        }
        if (!errors.isEmpty()) throw new InvalidRequestException(errors);

        String needle = q == null ? "" : q.trim();
        ShopNameLookup shops = shopNames.getIfAvailable();
        Set<Long> byShop = needle.isEmpty() || shops == null ? Set.of() : shops.vendorUserIdsByShopName(needle);
        MemberPage result = members.search(t, needle, byShop, p, s);
        return new UserList(new PageImpl<>(views(result.items()), PageRequest.of(p - 1, s), result.total()),
                result.counts());
    }

    // ===== Khóa / mở khóa =====

    /**
     * Khóa người dùng: {@code duration} {@code P7D} / {@code P30D} / {@code null} (vô thời hạn). Lý do lưu dạng
     * {@code "<MÃ>"} hoặc {@code "<MÃ>: <ghi chú>"} (D11); {@code OTHER} bắt buộc có ghi chú. Thu hồi mọi phiên và ghi
     * {@code ACCOUNT_SUSPENDED} (qua {@link UserFacade#suspend}). Khóa lại người đang bị khóa = cập nhật lý do / hạn.
     */
    // Không bọc transaction: UserFacade tự commit, rồi mới đọc lại dòng (JDBC không thấy thay đổi JPA chưa flush)
    public PlatformUserView suspend(long actorId, long userId, String reasonCode, String note, String duration,
                                    ClientInfo client) {
        String code = reasonCode == null ? "" : reasonCode.trim().toUpperCase(Locale.ROOT);
        String cleanNote = note == null || note.isBlank() ? null : note.trim();
        List<InvalidRequestException.Violation> errors = new ArrayList<>();
        if (!REASON_CODES.contains(code)) {
            errors.add(new InvalidRequestException.Violation("reason_code", "INVALID", reasonCode,
                    "Lý do phải thuộc " + REASON_CODES + "."));
        }
        if (OTHER.equals(code) && cleanNote == null) {
            errors.add(new InvalidRequestException.Violation("note", "REQUIRED", note, "Lý do Khác cần ghi chú."));
        }
        if (cleanNote != null && cleanNote.length() > MAX_NOTE) {
            errors.add(new InvalidRequestException.Violation("note", "TOO_LONG", null,
                    "Ghi chú tối đa " + MAX_NOTE + " ký tự."));
        }
        if (duration != null && !DURATIONS.containsKey(duration)) {
            errors.add(new InvalidRequestException.Violation("duration", "INVALID", duration,
                    "Thời hạn phải là P7D, P30D hoặc null (vô thời hạn)."));
        }
        if (!errors.isEmpty()) throw new InvalidRequestException(errors);

        requireMember(userId);
        OffsetDateTime until = duration == null ? null : OffsetDateTime.now(clock).plus(DURATIONS.get(duration));
        users.suspend(userId, formatReason(code, cleanNote), until, actorId, client);
        return view(requireMember(userId));
    }

    /** Mở khóa (lý do bắt buộc — ghi vào {@code ACCOUNT_REACTIVATED}); người đang không bị khóa → giữ nguyên. */
    // Không bọc transaction: UserFacade tự commit, rồi mới đọc lại dòng (JDBC không thấy thay đổi JPA chưa flush)
    public PlatformUserView reactivate(long actorId, long userId, String reason, ClientInfo client) {
        String clean = reason == null ? "" : reason.trim();
        if (clean.isEmpty() || clean.length() > 500) {
            throw new InvalidRequestException(List.of(new InvalidRequestException.Violation("reason", "LENGTH", null,
                    "Lý do mở khóa 1–500 ký tự.")));
        }
        requireMember(userId);
        users.reactivate(userId, actorId, client, clean);
        return view(requireMember(userId));
    }

    // ===== Ánh xạ =====

    private Member requireMember(long userId) {
        return members.find(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private PlatformUserView view(Member m) {
        return views(List.of(m)).get(0);
    }

    private List<PlatformUserView> views(List<Member> rows) {
        if (rows.isEmpty()) return List.of();
        List<Long> ids = rows.stream().map(Member::id).toList();
        List<Long> vendorIds = rows.stream().filter(m -> "VENDOR".equals(m.role())).map(Member::id).toList();
        Map<Long, Integer> orders = counts(orderStats.getIfAvailable() == null ? null
                : orderStats.getIfAvailable().orderCounts(ids));
        Map<Long, Integer> warnings = counts(warningStats.getIfAvailable() == null ? null
                : warningStats.getIfAvailable().warningCounts(ids));
        ShopNameLookup shops = shopNames.getIfAvailable();
        Map<Long, String> names = shops == null || vendorIds.isEmpty() ? Map.of() : shops.shopNames(vendorIds);
        OffsetDateTime now = OffsetDateTime.now(clock);
        return rows.stream().map(m -> new PlatformUserView(m.id(), "U-" + m.id(),
                m.fullName() == null ? m.email() : m.fullName(), names.get(m.id()), m.email(), m.phoneNumber(),
                m.role(), orders.getOrDefault(m.id(), 0), warnings.getOrDefault(m.id(), 0), statusOf(m, now),
                suspensionOf(m))).toList();
    }

    private static Map<Long, Integer> counts(Map<Long, Integer> m) {
        return m == null ? Map.of() : m;
    }

    /** {@code SUSPENDED} có hạn → {@code SUSPENDED}; vô thời hạn, {@code LOCKED}, hoặc khóa tạm do sai mật khẩu → {@code LOCKED}. */
    static String statusOf(Member m, OffsetDateTime now) {
        if ("SUSPENDED".equals(m.status())) return m.suspendedUntil() == null ? "LOCKED" : "SUSPENDED";
        if ("LOCKED".equals(m.status())) return "LOCKED";
        if (m.lockedUntil() != null && m.lockedUntil().isAfter(now)) return "LOCKED";
        return "ACTIVE";
    }

    private static UserSuspensionView suspensionOf(Member m) {
        if (!"SUSPENDED".equals(m.status()) && !"LOCKED".equals(m.status())) return null;
        String[] parts = parseReason(m.suspensionReason());
        return new UserSuspensionView(parts[0], parts[1], m.suspendedUntil());
    }

    /** {@code "<MÃ>"} / {@code "<MÃ>: <ghi chú>"} (D11). */
    static String formatReason(String code, String note) {
        return note == null ? code : code + ": " + note;
    }

    /**
     * Tách lại {@code [mã, ghi chú]}; lý do cũ dạng chữ tự do (khóa trước khi có mã — vd khóa nhân viên) → {@code OTHER}
     * kèm nguyên văn.
     */
    static String[] parseReason(String raw) {
        if (raw == null || raw.isBlank()) return new String[] {OTHER, null};
        int colon = raw.indexOf(':');
        String head = colon < 0 ? raw.trim() : raw.substring(0, colon).trim();
        if (REASON_CODES.contains(head)) {
            String rest = colon < 0 ? null : raw.substring(colon + 1).trim();
            return new String[] {head, rest == null || rest.isEmpty() ? null : rest};
        }
        return new String[] {OTHER, raw.trim()};
    }

    private static Tab parseTab(String raw, List<InvalidRequestException.Violation> errors) {
        if (raw == null || raw.isBlank()) return Tab.ALL;
        try {
            return Tab.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            errors.add(new InvalidRequestException.Violation("tab", "INVALID", raw,
                    "tab phải là ALL, CUSTOMER, VENDOR hoặc LOCKED."));
            return Tab.ALL;
        }
    }
}
