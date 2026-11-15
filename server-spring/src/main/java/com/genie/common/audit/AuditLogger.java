package com.genie.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.security.ClientInfo;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi nhật ký kiểm toán {@code system_logs} (NFR-08, plan AUTH A01.07). Bảng chỉ ghi thêm.
 * <ul>
 *   <li>{@link #record} — chạy trong transaction của use case (REQUIRED): use case rollback thì nhật ký cũng không còn;</li>
 *   <li>{@link #recordIndependent} — transaction riêng (REQUIRES_NEW): dùng cho sự kiện thất bại phải giữ dù transaction
 *       chính rollback ({@code LOGIN_FAILED}, {@code ACCESS_DENIED}, {@code ACCOUNT_LOCKED}).</li>
 * </ul>
 * {@code details} serialize JSON; khóa nên viết snake_case.
 */
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    private final SystemLogRepository repository;
    private final List<ActorDirectory> directories;
    private final ObjectMapper objectMapper;

    public AuditLogger(SystemLogRepository repository, List<ActorDirectory> directories, ObjectMapper objectMapper) {
        this.repository = repository;
        this.directories = directories;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(AuditAction action, AuditActor actor, String referenceId, Map<String, ?> details,
                       ClientInfo client, boolean success) {
        save(action, actor, referenceId, details, client, success);
    }

    /** Như {@link #record} nhưng chỉ biết {@code userId} — tra email / vai trò qua {@link ActorDirectory}. */
    @Transactional
    public void record(AuditAction action, Long userId, String referenceId, Map<String, ?> details,
                       ClientInfo client, boolean success) {
        save(action, resolve(userId), referenceId, details, client, success);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependent(AuditAction action, AuditActor actor, String referenceId, Map<String, ?> details,
                                  ClientInfo client, boolean success) {
        save(action, actor, referenceId, details, client, success);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependent(AuditAction action, Long userId, String referenceId, Map<String, ?> details,
                                  ClientInfo client, boolean success) {
        save(action, resolve(userId), referenceId, details, client, success);
    }

    public AuditActor resolve(Long userId) {
        if (userId == null) {
            return AuditActor.SYSTEM;
        }
        for (ActorDirectory directory : directories) {
            var found = directory.find(userId);
            if (found.isPresent()) {
                return found.get();
            }
        }
        return new AuditActor(userId, "user:" + userId, "UNKNOWN");
    }

    private void save(AuditAction action, AuditActor actor, String referenceId, Map<String, ?> details,
                      ClientInfo client, boolean success) {
        AuditActor who = actor == null ? AuditActor.SYSTEM : actor;
        SystemLog entry = SystemLog.of(who.userId(), truncate(who.email(), 255), truncate(who.role(), 50), action,
                truncate(referenceId, 100), toJson(details), client == null ? null : truncate(client.ip(), 45), success);
        if (client != null) {
            entry.setUserAgent(truncate(client.userAgent(), 500));
        }
        repository.save(entry);
    }

    private String toJson(Map<String, ?> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Không serialize được details nhật ký: {}", e.getMessage());
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
