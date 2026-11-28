// Cửa công khai của chat cho module khác (plan 2026-11-26 chat M05.03, D12, D17)
package com.genie.social.chat.application;

import com.genie.social.chat.domain.ConversationRepository;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@code vendor} dùng cho badge Seller Center và job tỉ lệ phản hồi — chỉ trả số / record, không trả entity. */
@Service
public class ChatFacade {

    private final ConversationRepository conversations;
    private final ChatStats stats;

    public ChatFacade(ConversationRepository conversations, ChatStats stats) {
        this.conversations = conversations;
        this.stats = stats;
    }

    /** Tổng tin khách gửi mà shop chưa đọc ({@code badges.unread_chats} — API_SPEC 10.1). */
    @Transactional(readOnly = true)
    public long unreadForVendor(long vendorUserId) {
        return conversations.sumUnreadForVendor(vendorUserId);
    }

    /** Tỉ lệ / trung vị phút phản hồi của nhiều shop trong {@code [from, to)} (không tính tin tự động — N7). */
    @Transactional(readOnly = true)
    public Map<Long, ChatStats.ResponseStats> responseStats(Collection<Long> vendorUserIds, OffsetDateTime from,
                                                           OffsetDateTime to) {
        return stats.responseStats(vendorUserIds, from, to);
    }
}
