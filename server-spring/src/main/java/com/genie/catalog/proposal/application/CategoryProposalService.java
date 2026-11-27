// Người bán đề xuất danh mục mới & sàn duyệt đề xuất — UC-CAT-17 (API 10.6, 11.11; plan C1 P08, D20)
package com.genie.catalog.proposal.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.genie.catalog.category.application.AdminCategoryView;
import com.genie.catalog.category.application.CategoryAdminService;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.catalog.category.application.CategoryIndex;
import com.genie.catalog.category.application.CategoryRow;
import com.genie.catalog.proposal.domain.CategoryProposal;
import com.genie.catalog.proposal.domain.CategoryProposalRepository;
import com.genie.catalog.proposal.domain.CategoryProposalStatus;
import com.genie.common.domain.NameNormalizer;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.InvalidRequestException.Violation;
import com.genie.common.error.NotFoundException;
import com.genie.common.security.ClientInfo;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tên 2–100 ký tự, lý do ≤ 500. Trùng tên (bỏ dấu, không phân biệt hoa) với danh mục cùng cha → {@code 409 GNE-CAT-4091}
 * kèm {@code details.existing_category}; trùng đề xuất đang chờ của chính shop → 4091 {@code details.field = proposal};
 * tối đa 5 đề xuất chờ duyệt mỗi shop → {@code 422 GNE-CAT-4221}. Duyệt = tạo danh mục qua {@link CategoryAdminService#create}
 * (slug tự sinh, hoa hồng kế thừa nếu không gửi). Thông báo shop chờ cụm B6 (D14) — shop xem trạng thái ở màn Danh mục.
 */
@Service
public class CategoryProposalService {

    static final int MAX_PENDING = 5;

    public record IdName(long id, String name) {
    }

    /** Dòng ở màn Danh mục của Seller Center — API 10.6. */
    public record SellerView(long id, String code, String name, IdName parent, String status, String rejectReason,
                             OffsetDateTime createdAt) {
    }

    /** Dòng hàng chờ đề xuất của Admin — API 11.11. */
    public record AdminView(long id, String code, String name, IdName shop, IdName parent, String reason,
                            String status, OffsetDateTime createdAt) {
    }

    public record CreateRequest(String name, Long parentId, String reason) {
    }

    public record Approved(IdName category) {
    }

    private static final String SELECT = """
            SELECT cp.id, cp.proposed_name, cp.parent_category_id, c.name AS parent_name, cp.status,
                   cp.rejection_reason, cp.reason, cp.created_at, vp.id AS shop_id, vp.shop_name
              FROM category_proposals cp
              LEFT JOIN categories c ON c.id = cp.parent_category_id
              LEFT JOIN vendor_profiles vp ON vp.user_id = cp.vendor_id
            """;

    private final CategoryProposalRepository proposals;
    private final CategoryCatalog catalog;
    private final CategoryAdminService categoryAdmin;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Clock clock;

    public CategoryProposalService(CategoryProposalRepository proposals, CategoryCatalog catalog,
                                   CategoryAdminService categoryAdmin, JdbcTemplate jdbc, ObjectMapper json,
                                   Clock clock) {
        this.proposals = proposals;
        this.catalog = catalog;
        this.categoryAdmin = categoryAdmin;
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    // ===== Người bán =====

    @Transactional(readOnly = true)
    public List<SellerView> mine(long vendorUserId) {
        return jdbc.query(SELECT + " WHERE cp.vendor_id = ? ORDER BY cp.created_at DESC, cp.id DESC",
                (rs, i) -> sellerView(rs), vendorUserId);
    }

    @Transactional
    public SellerView create(long vendorUserId, CreateRequest in) {
        String name = in == null || in.name() == null ? "" : in.name().trim().replaceAll("\\s+", " ");
        String reason = in == null || in.reason() == null || in.reason().isBlank() ? null : in.reason().trim();
        Long parentId = in == null ? null : in.parentId();
        List<Violation> errors = new ArrayList<>();
        if (name.length() < 2 || name.length() > 100) {
            errors.add(new Violation("name", "LENGTH", name, "Tên danh mục 2–100 ký tự."));
        }
        if (reason != null && reason.length() > 500) {
            errors.add(new Violation("reason", "TOO_LONG", reason.length(), "Lý do tối đa 500 ký tự."));
        }
        CategoryIndex idx = catalog.freshIndex();
        if (parentId != null) {
            CategoryRow parent = idx.find(parentId).orElse(null);
            if (parent == null || !parent.active()) {
                errors.add(new Violation("parent_id", "NOT_FOUND", parentId, "Danh mục cha không tồn tại."));
            } else if (idx.level(parentId) >= CategoryIndex.MAX_LEVEL) {
                errors.add(new Violation("parent_id", "TOO_DEEP", parentId, "Cây danh mục tối đa 3 cấp."));
            }
        }
        if (!errors.isEmpty()) throw new InvalidRequestException(errors);

        String key = NameNormalizer.normalize(name);
        for (CategoryRow r : idx.all()) {
            if (Objects.equals(parentId, r.parentId()) && key.equals(NameNormalizer.normalize(r.name()))) {
                throw new BusinessException(ErrorCode.CATEGORY_DUPLICATE, Map.of("field", "name",
                        "existing_category", Map.of("id", r.id(), "name", r.name())));
            }
        }
        // khóa theo shop để đếm "đang chờ" không bị hai request song song vượt giới hạn
        jdbc.queryForList("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, vendorUserId);
        List<Map<String, Object>> pending = jdbc.queryForList("""
                SELECT proposed_name, parent_category_id FROM category_proposals
                 WHERE vendor_id = ? AND status = 'PENDING'""", vendorUserId);
        for (Map<String, Object> p : pending) {
            Long pParent = p.get("parent_category_id") == null ? null : ((Number) p.get("parent_category_id")).longValue();
            if (Objects.equals(parentId, pParent) && key.equals(NameNormalizer.normalize((String) p.get("proposed_name")))) {
                throw new BusinessException(ErrorCode.CATEGORY_DUPLICATE,
                        "Bạn đã gửi đề xuất này và đang chờ duyệt.", Map.of("field", "proposal"));
            }
        }
        if (pending.size() >= MAX_PENDING) throw new BusinessException(ErrorCode.CATEGORY_PROPOSAL_LIMIT);

        CategoryProposal saved = proposals.saveAndFlush(CategoryProposal.builder().vendorId(vendorUserId)
                .parentCategoryId(parentId).proposedName(name).reason(reason).build());
        return jdbc.query(SELECT + " WHERE cp.id = ?", (rs, i) -> sellerView(rs), saved.getId()).get(0);
    }

    // ===== Sàn =====

    @Transactional(readOnly = true)
    public List<AdminView> forAdmin(String status) {
        String s = status == null || status.isBlank() ? "PENDING" : status.trim().toUpperCase();
        if (!List.of("PENDING", "APPROVED", "REJECTED").contains(s)) {
            throw InvalidRequestException.of("status", "INVALID_VALUE", status, "Trạng thái: PENDING, APPROVED, REJECTED.");
        }
        // chờ duyệt: cũ trước (xử lý theo thứ tự đến); đã xử lý: mới trước
        String order = "PENDING".equals(s) ? " ORDER BY cp.created_at ASC, cp.id ASC" : " ORDER BY cp.created_at DESC, cp.id DESC";
        return jdbc.query(SELECT + " WHERE cp.status = ?" + order, (rs, i) -> adminView(rs), s);
    }

    /** Duyệt: tạo danh mục dưới đúng cha; {@code commissionRate = null} → kế thừa. Gửi tỷ lệ cần {@code UPDATE_COMMISSION}. */
    @Transactional
    public Approved approve(long staffId, long proposalId, BigDecimal commissionRate, boolean canChangeCommission,
                            ClientInfo client) {
        CategoryProposal p = pendingForUpdate(proposalId);
        ObjectNode body = json.createObjectNode();
        body.put("name", p.getProposedName());
        if (p.getParentCategoryId() == null) body.putNull("parent_id");
        else body.put("parent_id", p.getParentCategoryId());
        if (commissionRate != null) body.put("commission_rate", commissionRate);
        // trùng tên phát sinh sau khi gửi → CategoryAdminService ném 409 GNE-CAT-4091
        AdminCategoryView created = categoryAdmin.create(body, canChangeCommission, staffId, client);
        p.approve(created.id(), staffId, OffsetDateTime.now(clock));
        proposals.save(p);
        return new Approved(new IdName(created.id(), created.name()));
    }

    @Transactional
    public void reject(long staffId, long proposalId, String reason) {
        String clean = reason == null ? null : reason.trim();
        if (clean == null || clean.isEmpty()) {
            throw InvalidRequestException.of("reason", "REQUIRED", null, "Nhập lý do từ chối.");
        }
        if (clean.length() > 500) {
            throw InvalidRequestException.of("reason", "TOO_LONG", clean.length(), "Lý do tối đa 500 ký tự.");
        }
        CategoryProposal p = pendingForUpdate(proposalId);
        p.reject(clean, staffId, OffsetDateTime.now(clock));
        proposals.save(p);
    }

    private CategoryProposal pendingForUpdate(long id) {
        CategoryProposal p = proposals.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề xuất."));
        if (p.getStatus() != CategoryProposalStatus.PENDING) {
            throw new BusinessException(ErrorCode.PRODUCT_STATE_CONFLICT, "Đề xuất đã được xử lý.",
                    Map.of("current_status", p.getStatus().name()));
        }
        return p;
    }

    // ===== Ánh xạ =====

    static String code(long id) {
        return String.format("CP-%03d", id);
    }

    private static IdName parent(ResultSet rs) throws SQLException {
        long pid = rs.getLong("parent_category_id");
        return rs.wasNull() ? null : new IdName(pid, rs.getString("parent_name"));
    }

    private static OffsetDateTime created(ResultSet rs) throws SQLException {
        var ts = rs.getTimestamp("created_at");
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.ofHours(7));
    }

    private static SellerView sellerView(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        return new SellerView(id, code(id), rs.getString("proposed_name"), parent(rs), rs.getString("status"),
                rs.getString("rejection_reason"), created(rs));
    }

    private static AdminView adminView(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long shopId = rs.getLong("shop_id");
        IdName shop = rs.wasNull() ? null : new IdName(shopId, rs.getString("shop_name"));
        return new AdminView(id, code(id), rs.getString("proposed_name"), shop, parent(rs), rs.getString("reason"),
                rs.getString("status"), created(rs));
    }
}
