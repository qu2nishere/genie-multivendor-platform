package com.genie.common.audit;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Bảng {@code system_logs} chỉ ghi thêm (NFR-08, UC-ADM-07): KHÔNG khai báo phương thức sửa / xóa ở đây —
 * {@code SystemLogRepositoryReadOnlyTest} kiểm. Truy vấn đọc có lọc dùng {@link SystemLogSpecs} qua
 * {@link JpaSpecificationExecutor} (keyset {@code created_at DESC, id DESC}).
 */
public interface SystemLogRepository extends JpaRepository<SystemLog, String>, JpaSpecificationExecutor<SystemLog> {

    /**
     * Bản ghi gần nhất của một hành động của user theo kết quả ({@code SUCCESS} / {@code FAILED}) — vd. lần liên kết
     * Google thành công gần nhất (Tài khoản › Liên kết đăng nhập).
     */
    Optional<SystemLog> findFirstByUserIdAndActionTypeAndStatusOrderByCreatedAtDesc(Long userId, AuditAction actionType,
                                                                                    String status);
}
