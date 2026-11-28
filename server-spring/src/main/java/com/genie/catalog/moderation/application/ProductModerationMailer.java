// Email kết quả kiểm duyệt cho người bán sau commit — UC-CAT-13 bước 5 (plan C1 D14; thông báo in-app chờ cụm B6)
package com.genie.catalog.moderation.application;

import com.genie.auth.profile.application.UserFacade;
import com.genie.catalog.product.application.SellerShopPort;
import com.genie.catalog.product.event.ProductModeratedEvent;
import com.genie.common.mail.EmailSender;
import com.genie.common.mail.MailTemplate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gửi tới email tài khoản chủ shop: duyệt → {@code product-approved}, từ chối → {@code product-rejected} (lý do chuẩn + ghi
 * chú của Staff). Lỗi gửi chỉ ghi log — quyết định đã lưu. Thông báo in-app + realtime: cụm B6 cài sau (plan C1 D14).
 */
@Component
public class ProductModerationMailer {

    private static final Logger log = LoggerFactory.getLogger(ProductModerationMailer.class);

    private final EmailSender mail;
    private final UserFacade users;
    private final SellerShopPort shops;

    public ProductModerationMailer(EmailSender mail, UserFacade users, SellerShopPort shops) {
        this.mail = mail;
        this.users = users;
        this.shops = shops;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModerated(ProductModeratedEvent e) {
        String email = users.emailOf(e.vendorUserId()).orElse(null);
        if (email == null) return;
        String storeName = shops.factsOf(e.vendorUserId()).map(SellerShopPort.ShopFacts::shopName).orElse("");
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", storeName.isBlank() ? email : storeName);
        vars.put("storeName", storeName);
        vars.put("productName", e.productName());
        vars.put("productId", e.productId());
        MailTemplate template;
        if ("APPROVED".equals(e.decision())) {
            template = MailTemplate.PRODUCT_APPROVED;
        } else {
            template = MailTemplate.PRODUCT_REJECTED;
            String label = "AI_AUTO".equals(e.reasonCode())
                    ? "Không đạt kiểm duyệt tự động"
                    : ModerationDecisionService.REJECT_REASONS.getOrDefault(e.reasonCode(), e.reasonCode());
            vars.put("reason", e.note() == null || e.note().isBlank() ? label : label + " — " + e.note());
        }
        try {
            mail.send(email, template, vars, Locale.forLanguageTag("vi"));
        } catch (RuntimeException ex) {
            log.warn("Không gửi được email kiểm duyệt sản phẩm {} ({}): {}", e.productId(), e.decision(), ex.getMessage());
        }
    }
}
