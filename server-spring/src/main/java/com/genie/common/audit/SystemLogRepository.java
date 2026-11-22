package com.genie.common.audit;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Bảng {@code system_logs} chỉ ghi thêm (NFR-08, UC-ADM-07): KHÔNG khai báo phương thức sửa / xóa ở đây —
 * {@code SystemLogRepositoryReadOnlyTest} kiểm. Truy vấn đọc có lọc dùng {@link SystemLogSpecs} qua
 * {@link JpaSpecificationExecutor} (keyset {@code created_at DESC, id DESC}).
 */
public interface SystemLogRepository extends JpaRepository<SystemLog, String>, JpaSpecificationExecutor<SystemLog> {

    /**
     * Thời điểm gần nhất của một hành động theo từng đối tượng — {@code [referenceId, max(createdAt)]}. Dùng tính hạn lời
     * mời nhân viên từ dòng {@code STAFF_INVITED} mới nhất (plan B1 D5).
     */
    @Query("select l.referenceId, max(l.createdAt) from SystemLog l "
            + "where l.actionType = :action and l.referenceId in :refs group by l.referenceId")
    List<Object[]> findLatestByReference(@Param("action") AuditAction action, @Param("refs") Collection<String> refs);

    /**
     * Bản ghi gần nhất của một hành động của user theo kết quả ({@code SUCCESS} / {@code FAILED}) — vd. lần liên kết
     * Google thành công gần nhất (Tài khoản › Liên kết đăng nhập).
     */
    Optional<SystemLog> findFirstByUserIdAndActionTypeAndStatusOrderByCreatedAtDesc(Long userId, AuditAction actionType,
                                                                                    String status);
}
