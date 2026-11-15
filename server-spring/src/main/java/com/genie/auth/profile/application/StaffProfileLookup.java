package com.genie.auth.profile.application;

import java.util.Optional;

/**
 * Cổng tra họ tên / ảnh của nhân viên, Admin (bảng {@code admin_profiles} thuộc module {@code admin}).
 * Module {@code admin} cài đặt — {@code auth} không phụ thuộc {@code admin} (ArchUnit).
 */
public interface StaffProfileLookup {

    Optional<StaffProfile> find(long userId);

    record StaffProfile(String fullName, String avatarUrl) {
    }
}
