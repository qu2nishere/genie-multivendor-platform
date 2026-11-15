package com.genie.auth.profile.application;

import com.genie.auth.profile.domain.Gender;
import java.time.LocalDate;

/**
 * Thay đổi hồ sơ ({@code PATCH /me/profile}): {@code null} = giữ nguyên; chuỗi rỗng ở {@code phoneNumber} / {@code bio}
 * = xóa giá trị. {@code avatarObjectKey} là khóa {@code tmp/…} vừa tải lên qua {@code POST /uploads/presigned-urls}.
 */
public record ProfileChanges(String fullName, String phoneNumber, Gender gender, LocalDate dateOfBirth, String bio,
                             String preferredLanguage, String avatarObjectKey) {
}
