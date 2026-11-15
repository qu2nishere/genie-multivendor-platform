package com.genie.admin.staff.application;

import com.genie.admin.staff.domain.AdminProfileRepository;
import com.genie.auth.profile.application.StaffProfileLookup;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Cài đặt cổng {@link StaffProfileLookup} của {@code auth}: họ tên / ảnh nhân viên, Admin từ {@code admin_profiles}.
 */
@Component
public class AdminProfileLookup implements StaffProfileLookup {

    private final AdminProfileRepository profiles;

    public AdminProfileLookup(AdminProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    public Optional<StaffProfile> find(long userId) {
        return profiles.findByUserId(userId).map(p -> new StaffProfile(p.getFullName(), p.getAvatarUrl()));
    }
}
