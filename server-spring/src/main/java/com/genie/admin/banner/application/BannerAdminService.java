package com.genie.admin.banner.application;

import com.genie.admin.banner.domain.BannerPosition;
import com.genie.admin.banner.domain.HomeBanner;
import com.genie.admin.banner.domain.HomeBannerRepository;
import com.genie.admin.banner.event.BannerChangedEvent;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.InvalidRequestException.Violation;
import com.genie.common.error.NotFoundException;
import com.genie.common.security.ClientInfo;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Quản lý banner trang chủ — UC-ADM-12 (API_SPEC 11.16; plan banner B03). Mỗi thao tác một transaction: kiểm hợp lệ →
 * ghi → nhật ký {@code BANNER_CHANGED} → sự kiện xóa cache sau commit. Ảnh: khóa {@code tmp/…} được chuyển sang
 * {@code banner/{id}/{desktop|mobile}-…} (D4); ảnh cũ xóa sau commit (giữ khóa {@code seed/}); rollback thì xóa ảnh vừa chuyển.
 */
@Service
public class BannerAdminService {

    /** Tối đa banner bật có lịch chồng nhau ở một vị trí (UC-ADM-12 bước 3; D2). */
    public static final int MAX_PER_POSITION = 8;
    static final String SEED_PREFIX = "seed/";

    private final HomeBannerRepository banners;
    private final BannerQueryService query;
    private final UploadService uploads;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<BannerCategoryLookup> categories;
    private final Clock clock;

    public BannerAdminService(HomeBannerRepository banners, BannerQueryService query, UploadService uploads,
                              AuditLogger audit, ApplicationEventPublisher events,
                              ObjectProvider<BannerCategoryLookup> categories, Clock clock) {
        this.banners = banners;
        this.query = query;
        this.uploads = uploads;
        this.audit = audit;
        this.events = events;
        this.categories = categories;
        this.clock = clock;
    }

    @Transactional
    public BannerView create(long adminId, BannerCommand cmd, ClientInfo client) {
        validate(cmd, true);
        boolean active = cmd.isActive() == null || cmd.isActive();
        int sort = cmd.sortOrder() == null ? 0 : cmd.sortOrder();
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (active) {
            ensureSlot(cmd.position(), cmd.categoryId(), cmd.startsAt(), cmd.endsAt(), now, -1L);
        }
        // Lưu trước với khóa tạm để có id, rồi chuyển ảnh sang banner/{id}/… (bước cuối của transaction)
        HomeBanner b = banners.save(HomeBanner.create(cmd.title().trim(), cmd.imageObjectKey(), null, cmd.targetUrl(),
                cmd.position(), cmd.categoryId(), sort, cmd.startsAt(), cmd.endsAt(), active, adminId));
        String desktop = promote(adminId, b.getId(), "desktop", cmd.imageObjectKey(), "image_object_key");
        String mobile = blank(cmd.mobileImageObjectKey()) ? null
                : promote(adminId, b.getId(), "mobile", cmd.mobileImageObjectKey(), "mobile_image_object_key");
        b.changeImages(desktop, mobile);
        record(adminId, b, "CREATE", Map.of(), client);
        return query.view(b);
    }

    @Transactional
    public BannerView update(long adminId, long id, BannerCommand cmd, ClientInfo client) {
        validate(cmd, false);
        HomeBanner b = find(id);
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean active = cmd.isActive() == null ? Boolean.TRUE.equals(b.getIsActive()) : cmd.isActive();
        if (active) {
            ensureSlot(cmd.position(), cmd.categoryId(), cmd.startsAt(), cmd.endsAt(), now, id);
        }
        Map<String, Object> before = snapshot(b);

        String oldDesktop = b.getImageUrl();
        String oldMobile = b.getMobileImageUrl();
        String desktop = oldDesktop;
        if (!blank(cmd.imageObjectKey()) && !cmd.imageObjectKey().equals(oldDesktop)) {
            desktop = promote(adminId, id, "desktop", cmd.imageObjectKey(), "image_object_key");
        }
        String mobile = oldMobile;
        if (cmd.mobileImageObjectKey() != null) {
            if (cmd.mobileImageObjectKey().isBlank()) {
                mobile = null;
            } else if (!cmd.mobileImageObjectKey().equals(oldMobile)) {
                mobile = promote(adminId, id, "mobile", cmd.mobileImageObjectKey(), "mobile_image_object_key");
            }
        }
        b.changeContent(cmd.title().trim(), cmd.targetUrl(), cmd.position(), cmd.categoryId(),
                cmd.sortOrder() == null ? b.getSortOrder() : cmd.sortOrder());
        b.changeSchedule(cmd.startsAt(), cmd.endsAt());
        b.changeImages(desktop, mobile);
        if (active) {
            b.activate();
        } else {
            b.deactivate();
        }
        if (!Objects.equals(oldDesktop, desktop)) {
            deleteAfterCommit(oldDesktop);
        }
        if (oldMobile != null && !Objects.equals(oldMobile, mobile)) {
            deleteAfterCommit(oldMobile);
        }
        Map<String, Object> after = snapshot(b);
        Map<String, Object> changes = new LinkedHashMap<>();
        after.forEach((k, v) -> {
            if (!Objects.equals(before.get(k), v)) {
                changes.put(k, Map.of("from", String.valueOf(before.get(k)), "to", String.valueOf(v)));
            }
        });
        record(adminId, b, "UPDATE", Map.of("changes", changes), client);
        return query.view(b);
    }

    /** Tắt ngay / Bật lại (PATCH) — bật lại cũng kiểm giới hạn 8 banner. */
    @Transactional
    public BannerView setActive(long adminId, long id, boolean active, ClientInfo client) {
        HomeBanner b = find(id);
        if (active == Boolean.TRUE.equals(b.getIsActive())) {
            return query.view(b);
        }
        if (active) {
            ensureSlot(b.getPosition(), b.getCategoryId(), b.getStartsAt(), b.getEndsAt(), OffsetDateTime.now(clock), id);
            b.activate();
        } else {
            b.deactivate();
        }
        record(adminId, b, active ? "ACTIVATE" : "DEACTIVATE", Map.of(), client);
        return query.view(b);
    }

    /** Xóa — chỉ banner {@code OFF} / {@code ENDED} ({@code 409 GNE-BNR-4091}). */
    @Transactional
    public void delete(long adminId, long id, ClientInfo client) {
        HomeBanner b = find(id);
        if (!b.isDeletable(OffsetDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.BANNER_NOT_DELETABLE);
        }
        record(adminId, b, "DELETE", Map.of(), client);
        banners.delete(b);
        deleteAfterCommit(b.getImageUrl());
        deleteAfterCommit(b.getMobileImageUrl());
    }

    // ---------- kiểm hợp lệ ----------

    private void validate(BannerCommand cmd, boolean creating) {
        if (cmd.targetUrl() != null && !cmd.targetUrl().isBlank() && !BannerTargetValidator.isAllowed(cmd.targetUrl())) {
            throw new BusinessException(ErrorCode.BANNER_TARGET_FORBIDDEN);
        }
        List<Violation> errors = new ArrayList<>();
        if (blank(cmd.title())) {
            errors.add(new Violation("title", "REQUIRED", null, "Vui lòng nhập tiêu đề banner."));
        } else if (cmd.title().trim().length() > 200) {
            errors.add(new Violation("title", "TOO_LONG", null, "Tiêu đề tối đa 200 ký tự."));
        }
        if (creating && blank(cmd.imageObjectKey())) {
            errors.add(new Violation("image_object_key", "REQUIRED", null, "Vui lòng tải ảnh banner."));
        }
        if (blank(cmd.targetUrl())) {
            errors.add(new Violation("target_url", "REQUIRED", null, "Vui lòng chọn liên kết đích."));
        }
        if (cmd.position() == null) {
            errors.add(new Violation("position", "REQUIRED", null, "Vui lòng chọn vị trí hiển thị."));
        } else if (cmd.position() == BannerPosition.CATEGORY_TOP) {
            if (cmd.categoryId() == null) {
                errors.add(new Violation("category_id", "REQUIRED", null, "Banner đầu trang danh mục phải chọn danh mục."));
            } else {
                BannerCategoryLookup lookup = categories.getIfAvailable();
                if (lookup != null && !lookup.exists(cmd.categoryId())) {
                    errors.add(new Violation("category_id", "NOT_FOUND", cmd.categoryId(), "Danh mục không tồn tại."));
                }
            }
        }
        if (cmd.sortOrder() != null && cmd.sortOrder() < 0) {
            errors.add(new Violation("sort_order", "OUT_OF_RANGE", cmd.sortOrder(), "Thứ tự không được âm."));
        }
        if (cmd.startsAt() == null) {
            errors.add(new Violation("starts_at", "REQUIRED", null, "Vui lòng chọn thời điểm bắt đầu."));
        }
        if (cmd.endsAt() == null) {
            errors.add(new Violation("ends_at", "REQUIRED", null, "Vui lòng chọn thời điểm kết thúc."));
        } else if (cmd.startsAt() != null && !cmd.endsAt().isAfter(cmd.startsAt())) {
            errors.add(new Violation("ends_at", "INVALID_RANGE", null, "Thời điểm kết thúc phải sau thời điểm bắt đầu."));
        }
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }
    }

    /** D2: banner bật khác cùng vị trí (+ danh mục) có lịch giao nhau, chưa kết thúc, đã đủ 8 → {@code 4221}. */
    private void ensureSlot(BannerPosition position, Long categoryId, OffsetDateTime startsAt, OffsetDateTime endsAt,
                            OffsetDateTime now, long excludeId) {
        if (!endsAt.isAfter(now)) {
            return;   // banner đã hết giờ không chiếm chỗ
        }
        Long cat = position == BannerPosition.CATEGORY_TOP ? categoryId : null;
        if (banners.countOverlapping(position, cat, startsAt, endsAt, now, excludeId) >= MAX_PER_POSITION) {
            throw new BusinessException(ErrorCode.BANNER_SLOT_FULL);
        }
    }

    // ---------- ảnh, nhật ký, sự kiện ----------

    private String promote(long adminId, long bannerId, String kind, String tmpKey, String field) {
        if (!tmpKey.startsWith("tmp/")) {
            throw InvalidRequestException.of(field, "INVALID_FORMAT", tmpKey, "Ảnh không hợp lệ, vui lòng tải lại.");
        }
        String key = uploads.promoteTo(adminId, UploadPurpose.BANNER_IMAGE, tmpKey,
                "banner/" + bannerId + "/" + kind + "-").objectKey();
        // transaction rollback sau bước này → xóa ảnh vừa chuyển để không mồ côi
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    uploads.deleteQuietly(UploadPurpose.BANNER_IMAGE, key);
                }
            }
        });
        return key;
    }

    private void deleteAfterCommit(String key) {
        if (key == null || key.startsWith(SEED_PREFIX) || key.startsWith("tmp/")) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                uploads.deleteQuietly(UploadPurpose.BANNER_IMAGE, key);
            }
        });
    }

    private void record(long adminId, HomeBanner b, String op, Map<String, Object> extra, ClientInfo client) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("op", op);
        details.put("target_label", "BN-" + b.getId() + " · " + b.getTitle());
        details.put("position", b.getPosition().name());
        details.putAll(extra);
        audit.record(AuditAction.BANNER_CHANGED, adminId, String.valueOf(b.getId()), details, client, true);
        events.publishEvent(new BannerChangedEvent(b.getId(), op));
    }

    private static Map<String, Object> snapshot(HomeBanner b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", b.getTitle());
        m.put("image", b.getImageUrl());
        m.put("mobile_image", b.getMobileImageUrl());
        m.put("target_url", b.getTargetUrl());
        m.put("position", b.getPosition());
        m.put("category_id", b.getCategoryId());
        m.put("sort_order", b.getSortOrder());
        m.put("starts_at", b.getStartsAt() == null ? null : b.getStartsAt().toInstant());
        m.put("ends_at", b.getEndsAt() == null ? null : b.getEndsAt().toInstant());
        m.put("is_active", b.getIsActive());
        return m;
    }

    private HomeBanner find(long id) {
        return banners.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy banner " + id));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
