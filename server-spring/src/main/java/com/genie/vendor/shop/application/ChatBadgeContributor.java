// Badge "Chat" của Seller Center (API_SPEC 10.1 badges.unread_chats; plan 2026-11-26 chat M05.03, D17)
package com.genie.vendor.shop.application;

import com.genie.social.chat.application.ChatFacade;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Cài cổng {@link SellerBadgeContributor} ở {@code vendor} (vendor → social) để {@code social} không phải import {@code vendor}. */
@Component
public class ChatBadgeContributor implements SellerBadgeContributor {

    private final ChatFacade chat;

    public ChatBadgeContributor(ChatFacade chat) {
        this.chat = chat;
    }

    @Override
    public Map<String, Long> badgesOf(long vendorUserId) {
        return Map.of("unread_chats", chat.unreadForVendor(vendorUserId));
    }
}
