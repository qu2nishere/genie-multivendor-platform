package com.genie.auth.profile.application;

import com.genie.auth.profile.domain.UserRepository;
import com.genie.common.audit.ActorDirectory;
import com.genie.common.audit.AuditActor;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Cài đặt cổng {@link ActorDirectory} của {@code common.audit}: tra email / vai trò để chụp vào {@code system_logs}.
 */
@Component
public class UserActorDirectory implements ActorDirectory {

    private final UserRepository users;

    public UserActorDirectory(UserRepository users) {
        this.users = users;
    }

    @Override
    public Optional<AuditActor> find(long userId) {
        return users.findById(userId).map(u -> new AuditActor(u.getId(), u.getEmail(), u.getRole().name()));
    }
}
