package com.genie.auth.oauth.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.SystemLog;
import com.genie.common.audit.SystemLogRepository;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cách đăng nhập của tài khoản — Tài khoản › Liên kết đăng nhập, màn Mật khẩu ("Đổi" hay "Thiết lập"), Seller › Bảo mật.
 * Bảng {@code users} chỉ có {@code oauth_provider} / {@code oauth_provider_id} (không thêm trường): thời điểm liên kết và
 * email Google lấy từ nhật ký {@code OAUTH_LINKED} gần nhất; không có thì dùng email / ngày tạo tài khoản.
 */
@Service
public class LoginMethodsService {

    private final UserRepository users;
    private final SystemLogRepository logs;
    private final ObjectMapper objectMapper;

    public LoginMethodsService(UserRepository users, SystemLogRepository logs, ObjectMapper objectMapper) {
        this.users = users;
        this.logs = logs;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public LoginMethodsView of(long userId) {
        User user = users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        List<LoginMethodsView.Identity> identities = providerOf(user).map(p -> {
            Optional<SystemLog> linked = logs.findFirstByUserIdAndActionTypeAndStatusOrderByCreatedAtDesc(userId,
                    AuditAction.OAUTH_LINKED, "SUCCESS");
            String email = linked.map(l -> emailIn(l.getDetails())).orElse(null);
            OffsetDateTime at = linked.map(SystemLog::getCreatedAt).orElse(user.getCreatedAt());
            return List.of(new LoginMethodsView.Identity(p.name(), email == null ? user.getEmail() : email, at));
        }).orElse(List.of());
        return new LoginMethodsView(user.getEmail(), new LoginMethodsView.Password(user.hasPassword()), identities);
    }

    private static Optional<OAuthProvider> providerOf(User user) {
        String id = user.getOauthProvider();
        return id == null ? Optional.empty()
                : Arrays.stream(OAuthProvider.values()).filter(p -> p.id().equals(id)).findFirst();
    }

    private String emailIn(String details) {
        if (details == null || details.isBlank()) {
            return null;
        }
        try {
            JsonNode email = objectMapper.readTree(details).get("email");
            return email == null || email.isNull() ? null : email.asText();
        } catch (Exception e) {
            return null;
        }
    }
}
