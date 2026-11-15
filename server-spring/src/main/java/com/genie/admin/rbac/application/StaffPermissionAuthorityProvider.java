package com.genie.admin.rbac.application;

import com.genie.admin.rbac.domain.StaffPermission;
import com.genie.admin.rbac.domain.StaffPermissionEnum;
import com.genie.admin.rbac.domain.StaffPermissionRepository;
import com.genie.common.domain.UserRole;
import com.genie.common.security.AuthorityProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Nạp 13 mã quyền cho JwtAuthFilter (implement.md 4.5, plan AUTH D14 / A04.06):
 * ADMIN = mọi quyền; STAFF = quyền còn hiệu lực trong {@code staff_permissions}; vai trò khác = rỗng.
 * Cache trong bộ nhớ 60 giây theo userId (đổi quyền → gọi {@link #evict(long)}; access token cũ hết hạn ≤ 15 phút).
 */
@Component
public class StaffPermissionAuthorityProvider implements AuthorityProvider {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final Set<String> ALL = Arrays.stream(StaffPermissionEnum.values())
            .map(Enum::name).collect(Collectors.toUnmodifiableSet());

    private final StaffPermissionRepository repository;
    private final Clock clock;
    private final ConcurrentHashMap<Long, Cached> cache = new ConcurrentHashMap<>();

    public StaffPermissionAuthorityProvider(StaffPermissionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Set<String> permissionsOf(long userId, UserRole role) {
        if (role == UserRole.ADMIN) {
            return ALL;
        }
        if (role != UserRole.STAFF) {
            return Set.of();
        }
        Instant now = clock.instant();
        Cached hit = cache.get(userId);
        if (hit != null && hit.expiresAt().isAfter(now)) {
            return hit.permissions();
        }
        Set<String> perms = repository.findByStaffUserIdAndRevokedAtIsNull(userId).stream()
                .filter(StaffPermission::isActive)
                .map(p -> p.getPermission().name())
                .collect(Collectors.toUnmodifiableSet());
        cache.put(userId, new Cached(perms, now.plus(CACHE_TTL)));
        return perms;
    }

    /** Xóa cache sau khi đổi quyền nhân viên (UC-ADM-02). */
    public void evict(long userId) {
        cache.remove(userId);
    }

    private record Cached(Set<String> permissions, Instant expiresAt) {
    }
}
