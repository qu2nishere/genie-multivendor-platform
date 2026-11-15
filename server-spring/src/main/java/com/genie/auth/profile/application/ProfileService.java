package com.genie.auth.profile.application;

import com.genie.auth.profile.domain.CustomerProfile;
import com.genie.auth.profile.domain.CustomerProfileRepository;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Hồ sơ người mua — UC-AUTH-08 (plan AUTH A11.03). Ảnh đại diện: nhận khóa tệp vừa tải lên MinIO → kiểm và chuyển khỏi
 * {@code tmp/} ({@link UploadService#promote}) → lưu URL công khai vào {@code customer_profiles.avatar_url} → SAU KHI
 * giao dịch thành công mới xóa ảnh cũ (rollback thì ảnh cũ vẫn còn dùng được).
 */
@Service
public class ProfileService {

    /** Hạn ẩn danh sau khi yêu cầu xóa tài khoản (UC-AUTH-10, plan D7). */
    public static final Duration DELETION_GRACE = Duration.ofDays(30);

    private final UserRepository users;
    private final CustomerProfileRepository profiles;
    private final UploadService uploads;

    public ProfileService(UserRepository users, CustomerProfileRepository profiles, UploadService uploads) {
        this.users = users;
        this.profiles = profiles;
        this.uploads = uploads;
    }

    @Transactional(readOnly = true)
    public ProfileView get(long userId) {
        User user = users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        CustomerProfile profile = profiles.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Tài khoản chưa có hồ sơ người mua."));
        return view(user, profile);
    }

    @Transactional
    public ProfileView update(long userId, ProfileChanges c) {
        User user = users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        CustomerProfile profile = profiles.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Tài khoản chưa có hồ sơ người mua."));
        profile.updateProfile(c.fullName(), c.phoneNumber(), c.gender(), c.dateOfBirth(), c.bio(),
                c.preferredLanguage());
        if (c.avatarObjectKey() != null) {
            String oldUrl = profile.getAvatarUrl();
            UploadService.StoredFile file = uploads.promote(userId, UploadPurpose.AVATAR, c.avatarObjectKey());
            profile.changeAvatar(file.publicUrl());
            uploads.keyOfPublicUrl(UploadPurpose.AVATAR, oldUrl)
                    .ifPresent(oldKey -> afterCommit(() -> uploads.deleteQuietly(UploadPurpose.AVATAR, oldKey)));
        }
        return view(user, profile);
    }

    private static void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private static ProfileView view(User user, CustomerProfile p) {
        ProfileView.DeletionRequest deletion = user.getDeletionRequestedAt() == null ? null
                : new ProfileView.DeletionRequest(user.getDeletionRequestedAt(),
                        user.getDeletionRequestedAt().plus(DELETION_GRACE));
        return new ProfileView(p.getFullName(), user.getEmail(), user.getEmailVerifiedAt() != null,
                p.getPhoneNumber(), p.getGender(), p.getDateOfBirth(), p.getBio(),
                p.getPreferredLanguage() == null ? "vi" : p.getPreferredLanguage(), p.getAvatarUrl(), deletion);
    }
}
