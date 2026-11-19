package com.genie.vendor.shop.application;

import com.genie.vendor.shop.event.ShopRegisteredEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/** Sau khi mở shop commit: chạy các {@link ShopBootstrapContributor} (plan Mở shop & KYC K03.06). */
@Component
public class ShopBootstrapListener {

    private static final Logger log = LoggerFactory.getLogger(ShopBootstrapListener.class);

    private final List<ShopBootstrapContributor> contributors;
    private final TransactionTemplate requiresNew;

    public ShopBootstrapListener(List<ShopBootstrapContributor> contributors, PlatformTransactionManager tx) {
        this.contributors = contributors;
        this.requiresNew = new TransactionTemplate(tx);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistered(ShopRegisteredEvent event) {
        for (ShopBootstrapContributor c : contributors) {
            try {
                requiresNew.executeWithoutResult(s -> c.onShopRegistered(event.shopId(), event.vendorUserId()));
            } catch (RuntimeException e) {
                log.error("Khởi tạo sau mở shop {} thất bại ở {}: {}", event.shopId(), c.getClass().getSimpleName(),
                        e.getMessage());
            }
        }
    }
}
