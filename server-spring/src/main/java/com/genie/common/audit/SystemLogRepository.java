package com.genie.common.audit;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemLogRepository extends JpaRepository<SystemLog, String> {

    /**
     * Bản ghi gần nhất của một hành động của user theo kết quả ({@code SUCCESS} / {@code FAILED}) — vd. lần liên kết
     * Google thành công gần nhất (Tài khoản › Liên kết đăng nhập).
     */
    Optional<SystemLog> findFirstByUserIdAndActionTypeAndStatusOrderByCreatedAtDesc(Long userId, AuditAction actionType,
                                                                                    String status);
}
