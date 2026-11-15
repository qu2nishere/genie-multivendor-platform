package com.genie.auth.profile.application;

import com.genie.auth.profile.domain.CustomerProfile;
import com.genie.auth.profile.domain.CustomerProfileRepository;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.domain.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dựng {@link UserSummary}: tên / ảnh lấy từ {@code customer_profiles} (khách, chủ shop) hoặc {@code admin_profiles}
 * (nhân viên, Admin — qua {@link StaffProfileLookup}); thiếu hồ sơ thì dùng phần trước @ của email.
 */
@Service
public class AccountSummaryService {

    private final CustomerProfileRepository profiles;
    private final UserRepository users;
    private final List<StaffProfileLookup> staffLookups;

    public AccountSummaryService(CustomerProfileRepository profiles, UserRepository users,
                                 List<StaffProfileLookup> staffLookups) {
        this.profiles = profiles;
        this.users = users;
        this.staffLookups = staffLookups;
    }

    /** Như {@link #summaryOf(User)} nhưng theo id (phản hồi làm mới phiên). */
    @Transactional(readOnly = true)
    public UserSummary summaryOf(long userId) {
        return users.findById(userId).map(this::summaryOf)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserSummary summaryOf(User user) {
        if (user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN) {
            for (StaffProfileLookup lookup : staffLookups) {
                Optional<StaffProfileLookup.StaffProfile> staff = lookup.find(user.getId());
                if (staff.isPresent()) {
                    return new UserSummary(user.getId(), user.getRole(), staff.get().fullName(), staff.get().avatarUrl());
                }
            }
        }
        Optional<CustomerProfile> profile = profiles.findByUserId(user.getId());
        if (profile.isPresent()) {
            return new UserSummary(user.getId(), user.getRole(), profile.get().getFullName(), profile.get().getAvatarUrl());
        }
        return new UserSummary(user.getId(), user.getRole(), fallbackName(user.getEmail()), null);
    }

    /** Ngôn ngữ email cho người dùng ({@code vi} mặc định). */
    @Transactional(readOnly = true)
    public String preferredLanguageOf(long userId) {
        return profiles.findByUserId(userId).map(CustomerProfile::getPreferredLanguage)
                .filter(l -> l != null && !l.isBlank()).orElse("vi");
    }

    private static String fallbackName(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
