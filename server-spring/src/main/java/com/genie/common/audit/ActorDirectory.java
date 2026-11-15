package com.genie.common.audit;

import java.util.Optional;

/**
 * Cổng tra email / vai trò của người dùng để chụp vào {@code system_logs} khi chỉ biết {@code userId}
 * (ví dụ filter bảo mật ghi {@code ACCESS_DENIED}). Module {@code auth} cài đặt — {@code common} không được phụ thuộc
 * {@code auth} (plan AUTH, mục 3).
 */
public interface ActorDirectory {

    Optional<AuditActor> find(long userId);
}
