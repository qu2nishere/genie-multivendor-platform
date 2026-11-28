// Body REST của chat (API_SPEC 8.2, 8.4)
package com.genie.social.chat.api;

import com.genie.social.chat.application.SendCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

final class ChatRequests {

    private ChatRequests() {
    }

    record OpenRequest(@NotNull @Positive Long shopId) {
    }

    record SendRequest(@NotNull String type, String text, String imageObjectKey, Long productId, Long variantId,
                       Long orderId, String clientMessageId) {

        SendCommand toCommand() {
            return new SendCommand(type, text, imageObjectKey, productId, variantId, orderId, clientMessageId);
        }
    }
}
