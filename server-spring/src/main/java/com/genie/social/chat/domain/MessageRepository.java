package com.genie.social.chat.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Tin nhắn (plan chat M03.03; API_SPEC 8.3). Mới nhất trước theo {@code (created_at, id)} — dùng index V11. */
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m FROM Message m WHERE m.conversationId = :cid
            ORDER BY m.createdAt DESC, m.id DESC""")
    List<Message> page(@Param("cid") long conversationId, Pageable page);

    @Query("""
            SELECT m FROM Message m WHERE m.conversationId = :cid
              AND (m.createdAt < :t OR (m.createdAt = :t AND m.id < :id))
            ORDER BY m.createdAt DESC, m.id DESC""")
    List<Message> pageBefore(@Param("cid") long conversationId, @Param("t") OffsetDateTime t, @Param("id") long id,
                             Pageable page);

    /** Tin mới nhất của mỗi hội thoại (khối {@code last_message} của 8.1) — một truy vấn cho cả trang. */
    @Query(value = """
            SELECT DISTINCT ON (conversation_id) * FROM messages WHERE conversation_id IN (:ids)
            ORDER BY conversation_id, created_at DESC, id DESC""", nativeQuery = true)
    List<Message> latestOf(@Param("ids") List<Long> conversationIds);

    /** Tin tự động gần nhất của hội thoại (luật lời chào / vắng mặt — D11). */
    @Query("SELECT MAX(m.createdAt) FROM Message m WHERE m.conversationId = :cid AND m.isAuto = TRUE")
    Optional<OffsetDateTime> lastAutoAt(@Param("cid") long conversationId);
}
