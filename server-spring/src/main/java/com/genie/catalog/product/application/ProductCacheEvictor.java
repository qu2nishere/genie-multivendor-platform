// Xóa cache phía đọc công khai sau khi sản phẩm của shop đổi — plan C1 N9 (CLAUDE.md mục 9: evict chi tiết / trang shop)
package com.genie.catalog.product.application;

import com.genie.catalog.category.application.CategoryCatalog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Chạy <b>sau commit</b> (rollback thì không xóa): chi tiết sản phẩm ({@code catalog:product:{id}}, 5 phút), trang shop
 * (10 phút) và — khi tập sản phẩm đang bán đổi — số sản phẩm theo danh mục (10 phút). Không có transaction → xóa ngay.
 */
@Component
public class ProductCacheEvictor {

    private final ProductDetailService details;
    private final SellerShopPort shops;
    private final CategoryCatalog catalog;

    public ProductCacheEvictor(ProductDetailService details, SellerShopPort shops, CategoryCatalog catalog) {
        this.details = details;
        this.shops = shops;
        this.catalog = catalog;
    }

    /** @param storefrontChanged sản phẩm vừa vào / ra khỏi trạng thái đang bán (đổi số đếm danh mục) */
    public void afterCommit(long productId, long shopId, boolean storefrontChanged) {
        if (storefrontChanged) {
            catalog.evictAfterCommit();
        }
        Runnable evict = () -> {
            details.evict(productId);
            shops.evictShopPage(shopId);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict.run();
                }
            });
        } else {
            evict.run();
        }
    }
}
