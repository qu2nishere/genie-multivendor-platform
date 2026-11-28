// Ẩn / mở bán lại / xóa mềm sản phẩm của shop — UC-CAT-11 (plan C1 P05, D17; API 10.5 PATCH / DELETE /seller/products/{id})
package com.genie.catalog.product.application;

import com.genie.catalog.product.domain.Product;
import com.genie.catalog.product.domain.ProductRepository;
import com.genie.catalog.product.domain.ProductStatus;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.NotFoundException;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductLifecycleService {

    private final ProductRepository products;
    private final ProductOrderGuard orders;
    private final SellerShopPort shops;
    private final ProductCacheEvictor evictor;
    private final JdbcTemplate jdbc;

    public ProductLifecycleService(ProductRepository products, ProductOrderGuard orders, SellerShopPort shops,
                                   ProductCacheEvictor evictor, JdbcTemplate jdbc) {
        this.products = products;
        this.orders = orders;
        this.shops = shops;
        this.evictor = evictor;
        this.jdbc = jdbc;
    }

    /** {@code status = HIDDEN}: chỉ từ PUBLISHED; {@code PUBLISHED}: chỉ từ HIDDEN do shop tự ẩn (sàn khóa → 4093). */
    @Transactional
    public SellerProductViews.Saved setStatus(long vendorUserId, long productId, String target) {
        if (!"HIDDEN".equals(target) && !"PUBLISHED".equals(target)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, Map.of("field", "status"));
        }
        Product p = owned(vendorUserId, productId);
        if ("HIDDEN".equals(target)) {
            if (p.getStatus() != ProductStatus.PUBLISHED) throw conflict(p);
            p.hide();
        } else {
            if (p.getStatus() != ProductStatus.HIDDEN) throw conflict(p);
            if (Boolean.TRUE.equals(p.getIsLocked())) throw new BusinessException(ErrorCode.PRODUCT_LOCKED);
            p.show();
        }
        products.save(p);
        evict(p, true);
        return new SellerProductViews.Saved(p.getId(), p.getStatus().name(), false);
    }

    /** Xóa mềm ở mọi trạng thái; còn giữ chỗ / đơn chưa kết thúc chứa sản phẩm → 409 GNE-CAT-4094. */
    @Transactional
    public void delete(long vendorUserId, long productId) {
        Product p = owned(vendorUserId, productId);
        Boolean reserved = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM product_variants
                                WHERE product_id = ? AND deleted_at IS NULL AND reserved_count > 0)""",
                Boolean.class, productId);
        if (Boolean.TRUE.equals(reserved) || orders.hasOpenOrders(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_HAS_OPEN_ORDERS);
        }
        boolean wasLive = p.getStatus() == ProductStatus.PUBLISHED;
        p.markDeleted();             // moderator_id giữ nguyên để còn lịch sử; rời hàng chờ vì status không còn chờ duyệt
        products.save(p);
        evict(p, wasLive);
    }

    /** Khóa dòng để không chạy song song với lần sửa / duyệt; sai chủ hoặc đã xóa → 404 (không lộ tồn tại). */
    private Product owned(long vendorUserId, long productId) {
        return products.findByIdForUpdate(productId)
                .filter(p -> p.getVendorId() != null && p.getVendorId() == vendorUserId)
                .filter(p -> p.getStatus() != ProductStatus.DELETED)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm."));
    }

    private static BusinessException conflict(Product p) {
        return new BusinessException(ErrorCode.PRODUCT_STATE_CONFLICT,
                Map.of("current_status", SellerProductQueryService.shopStatus(p.getStatus().name())));
    }

    private void evict(Product p, boolean storefrontChanged) {
        shops.factsOf(p.getVendorId()).ifPresent(s -> evictor.afterCommit(p.getId(), s.shopId(), storefrontChanged));
    }
}
