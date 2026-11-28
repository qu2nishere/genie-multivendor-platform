// Quyết định kiểm duyệt của Staff: duyệt / từ chối / đưa lại hàng chờ / giao lại — API_SPEC 11.12 (UC-CAT-13 bước 4–6; plan C1 P07.03, D13, D18, D24)
package com.genie.catalog.moderation.application;

import com.genie.catalog.brand.application.BrandService;
import com.genie.catalog.product.application.ProductCacheEvictor;
import com.genie.catalog.product.application.SellerShopPort;
import com.genie.catalog.product.domain.Product;
import com.genie.catalog.product.domain.ProductRepository;
import com.genie.catalog.product.domain.ProductStatus;
import com.genie.catalog.product.event.ProductModeratedEvent;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.NotFoundException;
import com.genie.common.security.ClientInfo;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mỗi thao tác khóa dòng sản phẩm ({@code SELECT … FOR UPDATE}) rồi mới kiểm trạng thái: hai nhân viên bấm cùng lúc thì
 * người sau nhận {@code 409 GNE-CAT-4097}. Trạng thái được phép theo thanh quyết định của màn hình (web
 * {@code moderationActions}): duyệt khi chờ duyệt / bị từ chối; từ chối khi chờ duyệt / đang bán; đưa lại hàng chờ khi đã
 * duyệt / đã từ chối. Mọi thao tác ghi {@code system_logs PRODUCT_MODERATED} với {@code details.action}.
 */
@Service
public class ModerationDecisionService {

    /** Lý do từ chối chuẩn (UC-CAT-13 bước 6; API 16) → câu hiển thị cho người bán. */
    public static final Map<String, String> REJECT_REASONS = orderedReasons();

    /** Kết quả trả về sau mỗi thao tác. */
    public record Result(long productId, String status, ModerationQueueService.IdName assignee) {
    }

    private static final Set<ProductStatus> CAN_APPROVE =
            EnumSet.of(ProductStatus.PENDING_APPROVAL, ProductStatus.FLAGGED, ProductStatus.REJECTED);
    private static final Set<ProductStatus> CAN_REJECT =
            EnumSet.of(ProductStatus.PENDING_APPROVAL, ProductStatus.FLAGGED, ProductStatus.PUBLISHED);
    private static final Set<ProductStatus> CAN_REQUEUE = EnumSet.of(ProductStatus.PUBLISHED, ProductStatus.REJECTED);
    private static final Set<ProductStatus> CAN_LOCK =
            EnumSet.of(ProductStatus.PUBLISHED, ProductStatus.HIDDEN, ProductStatus.FLAGGED);

    private final ProductRepository products;
    private final ModerationQueueService queue;
    private final ModeratorPool pool;
    private final BrandService brands;
    private final SellerShopPort shops;
    private final ProductCacheEvictor evictor;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ModerationDecisionService(ProductRepository products, ModerationQueueService queue, ModeratorPool pool,
                                     BrandService brands, SellerShopPort shops, ProductCacheEvictor evictor,
                                     AuditLogger audit, ApplicationEventPublisher events, Clock clock) {
        this.products = products;
        this.queue = queue;
        this.pool = pool;
        this.brands = brands;
        this.shops = shops;
        this.evictor = evictor;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Result approve(long staffId, long productId, ClientInfo client) {
        Product p = lock(productId);
        ProductStatus before = require(p, CAN_APPROVE);
        p.approve(staffId);
        if (p.getBrandId() != null) {
            brands.verifyIfProposed(p.getBrandId());      // thương hiệu shop tự thêm được xác minh (UC-CAT-13 bước 6)
        }
        products.save(p);
        log(staffId, p, "APPROVE", before, null, null, client);
        evict(p, true);
        events.publishEvent(new ProductModeratedEvent(p.getId(), p.getVendorId(), p.getName(), "APPROVED", null, null));
        return result(p);
    }

    @Transactional
    public Result reject(long staffId, long productId, String reasonCode, String note, ClientInfo client) {
        String code = reasonCode(reasonCode);
        String cleanNote = note(code, note);
        Product p = lock(productId);
        ProductStatus before = require(p, CAN_REJECT);
        // Lưu nhãn tiếng Việt (shop đọc trực tiếp ở form sửa / email), không lưu mã lý do — mã ghi ở nhật ký
        String label = REJECT_REASONS.get(code);
        p.reject(staffId, cleanNote == null ? label : label + ": " + cleanNote);
        products.save(p);
        log(staffId, p, "REJECT", before, code, cleanNote, client);
        evict(p, before == ProductStatus.PUBLISHED);
        events.publishEvent(new ProductModeratedEvent(p.getId(), p.getVendorId(), p.getName(), "REJECTED", code,
                cleanNote));
        return result(p);
    }

    /**
     * Sàn khóa sản phẩm (đang bán / shop tự ẩn / AI nghi ngờ) → ẩn, shop không tự mở lại; lý do lưu bằng nhãn như từ chối
     * (UC-CAT-11; API 11.11; plan C1 D18). Báo người bán qua thông báo — chờ cụm B6 (D14).
     */
    @Transactional
    public Result lockProduct(long staffId, long productId, String reasonCode, String note, ClientInfo client) {
        String code = reasonCode(reasonCode);
        String cleanNote = note(code, note);
        Product p = lock(productId);
        ProductStatus before = require(p, CAN_LOCK);
        if (Boolean.TRUE.equals(p.getIsLocked())) {
            throw new BusinessException(ErrorCode.PRODUCT_STATE_CONFLICT, Map.of("current_status", "LOCKED"));
        }
        String label = REJECT_REASONS.get(code);
        p.lockByPlatform(staffId, cleanNote == null ? label : label + ": " + cleanNote);
        products.save(p);
        log(staffId, p, "LOCK", before, code, cleanNote, client);
        evict(p, before == ProductStatus.PUBLISHED);
        return result(p);
    }

    /** Mở khóa: chỉ sản phẩm đang bị sàn khóa → bán lại ngay (API 11.11; plan C1 D18). */
    @Transactional
    public Result unlockProduct(long staffId, long productId, ClientInfo client) {
        Product p = lock(productId);
        if (p.getStatus() != ProductStatus.HIDDEN || !Boolean.TRUE.equals(p.getIsLocked())) {
            throw new BusinessException(ErrorCode.PRODUCT_STATE_CONFLICT,
                    Map.of("current_status", p.getStatus().name()));
        }
        p.unlockByPlatform(staffId);
        products.save(p);
        log(staffId, p, "UNLOCK", ProductStatus.HIDDEN, null, null, client);
        evict(p, true);
        return result(p);
    }

    private static String reasonCode(String reasonCode) {
        String code = reasonCode == null ? null : reasonCode.trim().toUpperCase();
        if (code == null || !REJECT_REASONS.containsKey(code)) {
            throw InvalidRequestException.of("reason_code", "INVALID_VALUE", reasonCode,
                    "Lý do: " + String.join(", ", REJECT_REASONS.keySet()) + ".");
        }
        return code;
    }

    private static String note(String code, String note) {
        String cleanNote = note == null || note.isBlank() ? null : note.trim();
        if (cleanNote != null && cleanNote.length() > 500) {
            throw InvalidRequestException.of("note", "TOO_LONG", cleanNote.length(), "Ghi chú tối đa 500 ký tự.");
        }
        if ("OTHER".equals(code) && cleanNote == null) {
            throw InvalidRequestException.of("note", "REQUIRED", null, "Lý do \"Khác\" cần ghi chú cụ thể.");
        }
        return cleanNote;
    }

    /** "Chờ duyệt lại": đưa sản phẩm đã quyết về hàng chờ, giao lại theo người ít việc nhất (không báo người bán). */
    @Transactional
    public Result requeue(long staffId, long productId, ClientInfo client) {
        Product p = lock(productId);
        ProductStatus before = require(p, CAN_REQUEUE);
        p.requeue(OffsetDateTime.now(clock));
        p.assignTo(queue.leastLoadedModerator());
        products.save(p);
        log(staffId, p, "REQUEUE", before, null, null, client);
        evict(p, before == ProductStatus.PUBLISHED);
        return result(p);
    }

    /** Giao lại người duyệt — đích phải là nhân viên kiểm duyệt (có thể đang tắt nhận việc). */
    @Transactional
    public Result assign(long actorId, long productId, Long targetStaffId, ClientInfo client) {
        if (targetStaffId == null || pool.moderators().stream().noneMatch(m -> m.staffId() == targetStaffId)) {
            throw InvalidRequestException.of("staff_id", "NOT_FOUND", targetStaffId,
                    "Không phải nhân viên kiểm duyệt sản phẩm.");
        }
        Product p = lock(productId);
        ProductStatus before = require(p, EnumSet.of(ProductStatus.PENDING_APPROVAL, ProductStatus.FLAGGED));
        p.assignTo(targetStaffId);
        products.save(p);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("action", "ASSIGN");
        details.put("from_status", before.name());
        details.put("assignee_id", targetStaffId);
        audit.record(AuditAction.PRODUCT_MODERATED, actorId, String.valueOf(productId), details, client, true);
        return result(p);
    }

    // ===== Tiện ích =====

    private Product lock(long productId) {
        return products.findByIdForUpdate(productId).filter(p -> p.getStatus() != ProductStatus.DELETED)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm."));
    }

    private static ProductStatus require(Product p, Set<ProductStatus> allowed) {
        if (!allowed.contains(p.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_STATE_CONFLICT,
                    "Sản phẩm vừa được xử lý ở nơi khác — vui lòng tải lại.",
                    Map.of("status", p.getStatus().name()));
        }
        return p.getStatus();
    }

    private void log(long staffId, Product p, String action, ProductStatus before, String reasonCode, String note,
                     ClientInfo client) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("action", action);
        details.put("from_status", before.name());
        details.put("to_status", p.getStatus().name());
        details.put("product_name", p.getName());
        if (reasonCode != null) details.put("reason_code", reasonCode);
        if (note != null) details.put("note", note);
        audit.record(AuditAction.PRODUCT_MODERATED, staffId, String.valueOf(p.getId()), details, client, true);
    }

    /** Sản phẩm vào / ra khỏi sàn → xóa cache chi tiết, trang shop, số đếm danh mục sau commit. */
    private void evict(Product p, boolean storefrontChanged) {
        if (!storefrontChanged) return;
        shops.factsOf(p.getVendorId()).ifPresent(s -> evictor.afterCommit(p.getId(), s.shopId(), true));
    }

    private Result result(Product p) {
        ModerationQueueService.IdName assignee = null;
        if (p.getModeratorId() != null) {
            assignee = new ModerationQueueService.IdName(p.getModeratorId(),
                    pool.names(java.util.List.of(p.getModeratorId())).getOrDefault(p.getModeratorId(),
                            "#" + p.getModeratorId()));
        }
        return new Result(p.getId(), p.getStatus().name(), assignee);
    }

    private static Map<String, String> orderedReasons() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("MISSING_SAFETY_CERT", "Thiếu chứng nhận an toàn / hợp quy");
        m.put("POLICY_VIOLATION", "Vi phạm chính sách đăng bán");
        m.put("BAD_IMAGE", "Ảnh sản phẩm không đạt");
        m.put("MISLEADING_DESCRIPTION", "Mô tả sai / gây hiểu lầm");
        m.put("UNREASONABLE_PRICE", "Giá bất hợp lý");
        m.put("PROHIBITED_ITEM", "Hàng cấm / hàng hạn chế kinh doanh");
        m.put("OTHER", "Lý do khác");
        return java.util.Collections.unmodifiableMap(m);
    }
}
