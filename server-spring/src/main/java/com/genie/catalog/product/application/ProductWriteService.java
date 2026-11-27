// Thêm mới / cập nhật sản phẩm & biến thể SKU của người bán — API_SPEC 10.5 POST / PUT (UC-CAT-09, UC-CAT-10; plan C1 P04)
package com.genie.catalog.product.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.attribute.application.AttributeView;
import com.genie.catalog.brand.application.BrandService;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.catalog.category.application.CategoryFacade;
import com.genie.catalog.category.application.CategoryIndex;
import com.genie.catalog.category.application.CategoryRow;
import com.genie.catalog.product.application.SellerProductViews.OptionGroupInput;
import com.genie.catalog.product.application.SellerProductViews.OptionValueInput;
import com.genie.catalog.product.application.SellerProductViews.ProductInput;
import com.genie.catalog.product.application.SellerProductViews.VariantInput;
import com.genie.catalog.product.domain.Product;
import com.genie.catalog.product.domain.ProductCondition;
import com.genie.catalog.product.domain.ProductEditPolicy;
import com.genie.catalog.product.domain.ProductRepository;
import com.genie.catalog.product.domain.ProductStatus;
import com.genie.catalog.product.domain.ShippingPayer;
import com.genie.catalog.product.domain.SkuGenerator;
import com.genie.catalog.product.event.ProductSubmittedEvent;
import com.genie.common.domain.NameNormalizer;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.InvalidRequestException.Violation;
import com.genie.common.storage.ObjectStorage;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Một transaction cho toàn bộ sản phẩm: dòng {@code products} (JPA, {@code @Version}) + ảnh, biến thể, giấy tờ (SQL chọn cột
 * — {@link ProductListingStore}). Thứ tự: kiểm trạng thái → kiểm từng trường ({@code 422} theo trường) → cổng gửi duyệt
 * (kho lấy hàng {@code 4224}, giấy tờ {@code 4222}, thông số bắt buộc {@code 4223}) → ghi. Tệp {@code tmp/} được chuyển sang
 * {@code vendor/{userId}/product/{productId}/…} ngay trong request (API 0.8).
 *
 * <p>Trạng thái sau khi lưu (plan C1 D3, D4): đang chờ duyệt → {@code 409 GNE-CAT-4096}; DRAFT / REJECTED + {@code submit}
 * → chờ duyệt; đang bán / đang ẩn mà đổi trường quan trọng ({@link ProductEditPolicy}) → chờ duyệt lại; đổi giá / tồn /
 * phân loại → giữ trạng thái, có hiệu lực ngay.
 */
@Service
public class ProductWriteService {

    static final int MAX_IMAGES = 8;
    static final int MAX_DOCUMENTS = 4;
    static final int MAX_SPECS = 30;
    /** Giá trị thông số được xuống dòng (vd "Tính năng màn hình") — web `SPEC_VALUE_MAX`. */
    static final int MAX_SPEC_VALUE = 1000;
    static final int MAX_GROUPS = 2;
    static final int MAX_GROUP_VALUES = 20;
    static final int MAX_VARIANTS = 50;
    static final long MIN_PRICE = 1_000;
    static final long MAX_PRICE = 1_000_000_000;
    static final int MAX_STOCK = 1_000_000;
    static final int MAX_WEIGHT_GRAMS = 50_000;
    static final int MAX_DIMENSION_CM = 200;

    private static final Pattern IMG_SRC = Pattern.compile("src=\"([^\"]+)\"");

    private final ProductRepository products;
    private final ProductListingStore store;
    private final SellerProductQueryService query;
    private final SellerShopPort shops;
    private final CategoryCatalog catalog;
    private final CategoryFacade categories;
    private final BrandService brands;
    private final HtmlSanitizer sanitizer;
    private final UploadService uploads;
    private final ObjectStorage storage;
    private final ProductCacheEvictor evictor;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;
    private final Clock clock;

    public ProductWriteService(ProductRepository products, ProductListingStore store, SellerProductQueryService query,
                               SellerShopPort shops, CategoryCatalog catalog, CategoryFacade categories,
                               BrandService brands, HtmlSanitizer sanitizer, UploadService uploads,
                               ObjectStorage storage, ProductCacheEvictor evictor, ApplicationEventPublisher events,
                               ObjectMapper json, Clock clock) {
        this.products = products;
        this.store = store;
        this.query = query;
        this.shops = shops;
        this.catalog = catalog;
        this.categories = categories;
        this.brands = brands;
        this.sanitizer = sanitizer;
        this.uploads = uploads;
        this.storage = storage;
        this.evictor = evictor;
        this.events = events;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public SellerProductViews.Saved create(long vendorUserId, ProductInput in) {
        SellerShopPort.ShopFacts shop = shopOf(vendorUserId);
        Plan plan = plan(vendorUserId, in, shop, null, in.submit());
        gates(plan, shop, in.submit());

        Product p = Product.builder().vendorId(vendorUserId).name(plan.name).status(ProductStatus.DRAFT)
                .basePrice(BigDecimal.ZERO).build();
        products.saveAndFlush(p);                         // cần id để đặt khóa tệp vendor/{uid}/product/{id}/
        write(p, plan, shop, vendorUserId, Set.of());
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (in.submit()) {
            p.submit(now);
            events.publishEvent(new ProductSubmittedEvent(p.getId(), vendorUserId, now, false));
        }
        products.save(p);
        return new SellerProductViews.Saved(p.getId(), SellerProductQueryService.shopStatus(p.getStatus().name()),
                p.getStatus() == ProductStatus.PENDING_APPROVAL);
    }

    @Transactional
    public SellerProductViews.Saved update(long vendorUserId, long productId, ProductInput in) {
        Product p = query.owned(vendorUserId, productId);
        if (p.isUnderReview()) {
            throw new BusinessException(ErrorCode.PRODUCT_UNDER_REVIEW);
        }
        if (in.version() != null && !Objects.equals(in.version(), p.getVersion())) {
            throw new BusinessException(ErrorCode.PRODUCT_STATE_CONFLICT,
                    "Sản phẩm vừa được thay đổi ở nơi khác — vui lòng tải lại trang.",
                    Map.of("reason", "STALE_VERSION", "current_version", p.getVersion() == null ? 0 : p.getVersion()));
        }
        SellerShopPort.ShopFacts shop = shopOf(vendorUserId);
        boolean live = p.isLive();
        ProductEditPolicy.ReviewFields before = reviewFields(p);
        Plan plan = plan(vendorUserId, in, shop, p, in.submit() || live);
        boolean needsReview = live && ProductEditPolicy.requiresReview(before, plan.reviewFields(p.getId()));
        boolean entersQueue = needsReview || (!live && in.submit());
        gates(plan, shop, entersQueue);

        write(p, plan, shop, vendorUserId, ownedKeys(p));
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (entersQueue) {
            p.submit(now);
            events.publishEvent(new ProductSubmittedEvent(p.getId(), vendorUserId, now, needsReview));
        }
        products.save(p);
        if (live) {
            evictor.afterCommit(p.getId(), shop.shopId(), entersQueue);     // vào hàng chờ = rời khỏi sàn
        }
        return new SellerProductViews.Saved(p.getId(), SellerProductQueryService.shopStatus(p.getStatus().name()),
                entersQueue);
    }

    // ===================================================================================================
    // Kiểm tra & chuẩn bị (chưa ghi gì)
    // ===================================================================================================

    /** Dữ liệu đã kiểm, sẵn sàng ghi. {@code variants}: theo thứ tự gửi lên. */
    private static final class Plan {
        String name;
        Long categoryId;
        BrandService.BrandView brand;
        ProductCondition condition;
        String origin;
        List<String> imageKeys = List.of();
        String videoKey;
        List<String> documentKeys = List.of();
        List<String> documentNames = List.of();
        List<SpecCodec.Entry> specs = List.of();
        String description;
        List<OptionGroupInput> groups = List.of();
        String imageGroup;
        List<VariantInput> variants = List.of();
        Integer weight;
        SellerProductViews.DimensionsInput dims;
        ShippingPayer payer;
        boolean preOrder;
        Integer preOrderDays;
        List<AttributeView> attributes = List.of();
        boolean requiresDocuments;

        ProductEditPolicy.ReviewFields reviewFields(Long productId) {
            return new ProductEditPolicy.ReviewFields(name, imageKeys, videoKey, description, categoryId,
                    brand == null ? null : brand.id(), new HashSet<>(documentKeys), SpecCodec.toMap(specs));
        }
    }

    private Plan plan(long vendorUserId, ProductInput in, SellerShopPort.ShopFacts shop, Product existing, boolean full) {
        List<Violation> v = new ArrayList<>();
        Plan plan = new Plan();

        // ----- Thông tin cơ bản -----
        String name = in.name() == null ? "" : in.name().trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) v.add(viol("name", "REQUIRED", in.name(), "Nhập tên sản phẩm."));
        else if (name.length() > 255) v.add(viol("name", "TOO_LONG", name.length(), "Tên sản phẩm tối đa 255 ký tự."));
        plan.name = name;

        CategoryIndex idx = catalog.index();
        if (in.categoryId() != null) {
            if (!idx.visible(in.categoryId())) {
                v.add(viol("category_id", "NOT_FOUND", in.categoryId(), "Danh mục không tồn tại hoặc đã ẩn."));
            } else {
                plan.categoryId = in.categoryId();
                plan.attributes = categories.attributesFor(in.categoryId());
                plan.requiresDocuments = SellerCatalogService.requiresDocuments(idx, in.categoryId());
            }
        } else if (full) {
            v.add(viol("category_id", "REQUIRED", null, "Chọn ngành hàng."));
        }

        if (in.brandId() != null) {
            plan.brand = brands.find(in.brandId()).orElse(null);
            if (plan.brand == null) v.add(viol("brand_id", "NOT_FOUND", in.brandId(), "Thương hiệu không tồn tại."));
        }

        plan.condition = ProductCondition.NEW;
        if (in.condition() != null && !in.condition().isBlank()) {
            try {
                plan.condition = ProductCondition.valueOf(in.condition().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                v.add(viol("condition", "INVALID_VALUE", in.condition(), "Tình trạng: NEW, USED hoặc REFURBISHED."));
            }
        }
        plan.origin = in.origin() == null || in.origin().isBlank() ? null : in.origin().trim();
        if (plan.origin != null && plan.origin.length() > 100) {
            v.add(viol("origin", "TOO_LONG", plan.origin.length(), "Xuất xứ tối đa 100 ký tự."));
        }

        // ----- Ảnh, video, giấy tờ -----
        List<String> images = cleanKeys(in.imageObjectKeys());
        if (images.size() != (in.imageObjectKeys() == null ? 0 : in.imageObjectKeys().size())) {
            v.add(viol("image_object_keys", "INVALID", null, "Danh sách ảnh có khóa trống hoặc trùng."));
        }
        if (images.size() > MAX_IMAGES) v.add(viol("image_object_keys", "TOO_MANY", images.size(), "Tối đa 8 ảnh."));
        else if (full && images.isEmpty()) v.add(viol("image_object_keys", "REQUIRED", 0, "Cần ít nhất 1 ảnh sản phẩm."));
        plan.imageKeys = images;
        plan.videoKey = in.videoObjectKey() == null || in.videoObjectKey().isBlank() ? null : in.videoObjectKey().trim();

        List<String> docs = cleanKeys(in.documentObjectKeys());
        if (docs.size() > MAX_DOCUMENTS) v.add(viol("document_object_keys", "TOO_MANY", docs.size(), "Tối đa 4 tệp giấy tờ."));
        plan.documentKeys = docs;
        plan.documentNames = in.documentFileNames() == null ? List.of() : in.documentFileNames();

        // ----- Thông số -----
        if (in.specs() != null && in.specs().size() > MAX_SPECS) {
            v.add(viol("specs", "TOO_MANY", in.specs().size(), "Tối đa 30 dòng thông số."));
        }
        if (in.specs() != null) {
            for (int i = 0; i < in.specs().size(); i++) {
                SellerProductViews.Spec s = in.specs().get(i);
                if (s == null) continue;
                if (s.name() != null && s.name().trim().length() > 60) {
                    v.add(viol("specs[" + i + "].name", "TOO_LONG", s.name().length(), "Tên thông số tối đa 60 ký tự."));
                }
                if (s.value() != null && s.value().trim().length() > MAX_SPEC_VALUE) {
                    v.add(viol("specs[" + i + "].value", "TOO_LONG", s.value().length(),
                            "Giá trị thông số tối đa " + MAX_SPEC_VALUE + " ký tự."));
                }
            }
        }
        plan.specs = SpecCodec.resolve(in.specs(), plan.attributes);
        for (SpecCodec.Entry e : plan.specs) {
            AttributeView a = e.attribute();
            if (a == null || a.values() == null || a.values().isEmpty()) continue;
            boolean ok = switch (a.dataType()) {
                case "ENUM" -> a.values().contains(e.value());
                case "MULTI_ENUM" -> a.values().containsAll(splitMulti(e.value()));
                default -> true;
            };
            if (!ok) {
                v.add(new Violation("specs", "INVALID_VALUE", e.value(),
                        "\"" + e.name() + "\" chỉ nhận: " + String.join(", ", a.values()) + "."));
            }
        }

        // ----- Mô tả -----
        HtmlSanitizer.Result desc = sanitizer.sanitize(in.descriptionHtml());
        if (desc.textLength() > HtmlSanitizer.MAX_TEXT_LENGTH || desc.html().length() > HtmlSanitizer.MAX_HTML_LENGTH) {
            v.add(viol("description_html", "TOO_LONG", desc.textLength(), "Mô tả tối đa 10.000 ký tự."));
        }
        plan.description = desc.html().isEmpty() ? null : desc.html();

        // ----- Phân loại & biến thể -----
        planVariants(in, plan, existing, full, v);

        // ----- Vận chuyển & đặt trước -----
        plan.weight = in.weightGrams();
        if (plan.weight == null) {
            if (full) v.add(viol("weight_grams", "REQUIRED", null, "Nhập cân nặng đóng gói (gram)."));
        } else if (plan.weight < 1 || plan.weight > MAX_WEIGHT_GRAMS) {
            v.add(viol("weight_grams", "OUT_OF_RANGE", plan.weight, "Cân nặng từ 1 đến 50.000 gram."));
        }
        plan.dims = in.dimensionsCm();
        if (plan.dims != null) {
            checkDimension("dimensions_cm.length", plan.dims.length(), v);
            checkDimension("dimensions_cm.width", plan.dims.width(), v);
            checkDimension("dimensions_cm.height", plan.dims.height(), v);
        }
        plan.payer = ShippingPayer.BUYER;
        if (in.shippingPayer() != null && !in.shippingPayer().isBlank()) {
            try {
                plan.payer = ShippingPayer.valueOf(in.shippingPayer().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                v.add(viol("shipping_payer", "INVALID_VALUE", in.shippingPayer(), "Người trả phí ship: BUYER hoặc SELLER."));
            }
        }
        plan.preOrder = Boolean.TRUE.equals(in.isPreOrder());
        if (plan.preOrder) {
            if (!shop.preOrderEnabled()) {
                v.add(viol("is_pre_order", "NOT_ALLOWED", true, "Shop chưa bật chế độ đặt trước (Cài đặt shop)."));
            }
            if (in.preOrderDays() == null || in.preOrderDays() < 7 || in.preOrderDays() > 30) {
                v.add(viol("pre_order_days", "OUT_OF_RANGE", in.preOrderDays(), "Số ngày chuẩn bị hàng từ 7 đến 30."));
            }
            plan.preOrderDays = in.preOrderDays();
        }

        if (!v.isEmpty()) {
            throw new InvalidRequestException(v);
        }
        return plan;
    }

    private void planVariants(ProductInput in, Plan plan, Product existing, boolean full, List<Violation> v) {
        List<OptionGroupInput> groups = in.optionGroups() == null ? List.of() : in.optionGroups();
        if (groups.size() > MAX_GROUPS) {
            v.add(viol("option_groups", "TOO_MANY", groups.size(), "Tối đa 2 nhóm phân loại."));
            return;
        }
        List<Set<String>> allowed = new ArrayList<>();
        Set<String> groupNames = new HashSet<>();
        for (int g = 0; g < groups.size(); g++) {
            OptionGroupInput group = groups.get(g);
            String gName = group == null || group.name() == null ? "" : group.name().trim();
            if (gName.isEmpty() || gName.length() > 30) {
                v.add(viol("option_groups[" + g + "].name", gName.isEmpty() ? "REQUIRED" : "TOO_LONG", gName,
                        "Tên nhóm phân loại từ 1 đến 30 ký tự."));
            } else if (!groupNames.add(NameNormalizer.normalize(gName))) {
                v.add(viol("option_groups[" + g + "].name", "DUPLICATE", gName, "Hai nhóm phân loại trùng tên."));
            }
            List<OptionValueInput> values = group == null || group.values() == null ? List.of() : group.values();
            if (values.isEmpty() || values.size() > MAX_GROUP_VALUES) {
                v.add(viol("option_groups[" + g + "].values", values.isEmpty() ? "REQUIRED" : "TOO_MANY", values.size(),
                        "Mỗi nhóm có từ 1 đến 20 giá trị."));
            }
            Set<String> seen = new LinkedHashSet<>();
            for (int k = 0; k < values.size(); k++) {
                String val = values.get(k) == null || values.get(k).value() == null ? "" : values.get(k).value().trim();
                if (val.isEmpty() || val.length() > 30) {
                    v.add(viol("option_groups[" + g + "].values[" + k + "].value", val.isEmpty() ? "REQUIRED" : "TOO_LONG",
                            val, "Giá trị phân loại từ 1 đến 30 ký tự."));
                } else if (!seen.add(val)) {
                    v.add(viol("option_groups[" + g + "].values[" + k + "].value", "DUPLICATE", val,
                            "Giá trị phân loại bị trùng."));
                }
            }
            allowed.add(seen);
        }
        plan.groups = groups;

        String imageGroup = in.variantImageGroup() == null || in.variantImageGroup().isBlank() ? null
                : in.variantImageGroup().trim();
        if (imageGroup != null && groups.stream().noneMatch(gr -> gr != null && gr.name() != null
                && gr.name().trim().equals(imageGroup))) {
            v.add(viol("variant_image_group", "NOT_FOUND", imageGroup, "Nhóm có ảnh phải là một nhóm phân loại."));
        }
        plan.imageGroup = imageGroup;

        List<VariantInput> variants = in.variants() == null ? List.of() : in.variants();
        if (variants.size() > MAX_VARIANTS) {
            v.add(viol("variants", "TOO_MANY", variants.size(), "Tối đa 50 biến thể."));
            return;
        }
        if (full && variants.isEmpty()) {
            v.add(viol("variants", "REQUIRED", 0, "Nhập giá và tồn kho cho sản phẩm."));
        }
        if (groups.isEmpty() && variants.size() > 1) {
            v.add(viol("variants", "TOO_MANY", variants.size(), "Không có nhóm phân loại thì chỉ có một biến thể."));
        }

        Map<Long, ProductListingStore.VariantRow> current = new HashMap<>();
        if (existing != null) {
            store.activeVariants(existing.getId()).forEach(r -> current.put(r.id(), r));
        }
        Set<List<String>> combos = new HashSet<>();
        Set<String> skus = new HashSet<>();
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < variants.size(); i++) {
            VariantInput vi = variants.get(i);
            String f = "variants[" + i + "]";
            if (vi == null) {
                v.add(viol(f, "REQUIRED", null, "Biến thể trống."));
                continue;
            }
            List<String> options = vi.options() == null ? List.of() : vi.options().stream()
                    .map(o -> o == null ? "" : o.trim()).toList();
            if (options.size() != groups.size()) {
                v.add(viol(f + ".options", "INVALID", options, "Mỗi biến thể chọn đúng một giá trị của mỗi nhóm."));
            } else {
                for (int g = 0; g < options.size(); g++) {
                    if (g < allowed.size() && !allowed.get(g).contains(options.get(g))) {
                        v.add(viol(f + ".options[" + g + "]", "NOT_FOUND", options.get(g),
                                "Giá trị không có trong nhóm phân loại."));
                    }
                }
                if (!combos.add(options)) {
                    v.add(viol(f + ".options", "DUPLICATE", options, "Hai biến thể trùng tổ hợp phân loại."));
                }
            }
            if (vi.id() != null) {
                if (!current.containsKey(vi.id()) || !ids.add(vi.id())) {
                    v.add(viol(f + ".id", "NOT_FOUND", vi.id(), "Biến thể không thuộc sản phẩm này."));
                }
            }
            if (vi.price() == null) {
                if (full) v.add(viol(f + ".price", "REQUIRED", null, "Nhập giá bán."));
            } else if (vi.price() < MIN_PRICE || vi.price() > MAX_PRICE) {
                v.add(viol(f + ".price", "OUT_OF_RANGE", vi.price(), "Giá bán từ 1.000₫ đến 1.000.000.000₫."));
            }
            if (vi.stock() == null) {
                if (full) v.add(viol(f + ".stock", "REQUIRED", null, "Nhập số lượng tồn kho."));
            } else if (vi.stock() < 0 || vi.stock() > MAX_STOCK) {
                v.add(viol(f + ".stock", "OUT_OF_RANGE", vi.stock(), "Tồn kho từ 0 đến 1.000.000."));
            } else if (vi.id() != null && current.containsKey(vi.id())
                    && vi.stock() < current.get(vi.id()).reserved()) {
                int reserved = current.get(vi.id()).reserved();
                v.add(new Violation(f + ".stock", "BELOW_RESERVED", Map.of("reserved", reserved),
                        "Đang có " + reserved + " sản phẩm được giữ cho đơn chưa thanh toán — tồn không thấp hơn số này."));
            }
            if (vi.sku() != null && !vi.sku().isBlank()) {
                String sku = vi.sku().trim();
                if (!SkuGenerator.isValid(sku)) {
                    v.add(viol(f + ".sku", "INVALID_FORMAT", sku, "SKU chỉ gồm chữ, số, dấu . _ - (tối đa 100 ký tự)."));
                } else if (!skus.add(sku) || store.skuTaken(sku, vi.id())) {
                    v.add(viol(f + ".sku", "DUPLICATE", sku, "SKU đã được dùng."));
                }
            }
        }
        plan.variants = variants;
    }

    /** Cổng gửi duyệt — chỉ khi sản phẩm sẽ vào hàng chờ (plan C1 D2, D7, D8). */
    private void gates(Plan plan, SellerShopPort.ShopFacts shop, boolean entersQueue) {
        if (!entersQueue) return;
        if (!shop.pickupConfigured()) {
            throw new BusinessException(ErrorCode.PICKUP_ADDRESS_REQUIRED, Map.of("field", "pickup_address"));
        }
        if (plan.requiresDocuments && plan.documentKeys.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_DOCUMENTS_REQUIRED, Map.of("field", "document_object_keys"));
        }
        Set<String> given = new HashSet<>();
        plan.specs.forEach(e -> given.add(e.key()));
        List<String> missing = plan.attributes.stream()
                .filter(AttributeView::isRequired).filter(a -> !given.contains(a.code()))
                .map(AttributeView::name).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_SPECS_MISSING, Map.of("missing_specs", missing));
        }
    }

    // ===================================================================================================
    // Ghi
    // ===================================================================================================

    private void write(Product p, Plan plan, SellerShopPort.ShopFacts shop, long vendorUserId, Set<String> ownedKeys) {
        long pid = p.getId();
        String prefix = "vendor/" + vendorUserId + "/product/" + pid + "/";

        // ảnh gallery + video
        List<String> imageKeys = new ArrayList<>();
        for (int i = 0; i < plan.imageKeys.size(); i++) {
            imageKeys.add(media(vendorUserId, UploadPurpose.PRODUCT_IMAGE, plan.imageKeys.get(i), prefix, ownedKeys,
                    "image_object_keys[" + i + "]"));
        }
        store.replaceGallery(pid, imageKeys, plan.name);
        p.setVideoUrl(plan.videoKey == null ? null
                : media(vendorUserId, UploadPurpose.PRODUCT_VIDEO, plan.videoKey, prefix, ownedKeys, "video_object_key"));

        // giấy tờ
        writeDocuments(pid, plan, vendorUserId, prefix);

        // mô tả: ảnh vừa tải lên (tmp) trong mô tả → khóa chính thức
        p.setDescription(promoteDescriptionImages(plan.description, vendorUserId, prefix));

        // biến thể
        BigDecimal minPrice = writeVariants(pid, plan, vendorUserId, prefix, ownedKeys);

        // trường của dòng products
        p.setName(plan.name);
        p.setCategoryId(plan.categoryId);
        p.setCategoryJson(categoryJson(plan.categoryId));
        p.setBrandId(plan.brand == null ? null : plan.brand.id());
        p.setBrand(plan.brand == null ? null : plan.brand.name());           // search_vector có thương hiệu (D10)
        p.setItemCondition(plan.condition);
        p.setCondition(plan.condition.name());
        p.setOrigin(plan.origin);
        Map<String, String> specs = SpecCodec.toMap(plan.specs);
        // kèm thứ tự dòng người bán sắp (JSONB tự sắp khóa — SpecCodec.ORDER_KEY)
        p.setSpecsJson(specs.isEmpty() ? null : store.toJson(SpecCodec.toStored(specs)));
        p.setWeight(plan.weight == null ? null : plan.weight.doubleValue());
        p.setLength(plan.dims == null || plan.dims.length() == null ? null : plan.dims.length().doubleValue());
        p.setWidth(plan.dims == null || plan.dims.width() == null ? null : plan.dims.width().doubleValue());
        p.setHeight(plan.dims == null || plan.dims.height() == null ? null : plan.dims.height().doubleValue());
        if (shop.pickupProvinceName() != null) p.setShipsFrom(shop.pickupProvinceName());
        p.setShippingPayer(plan.payer);
        p.setVariantImageAttribute(plan.imageGroup);
        p.setIsPreOrder(plan.preOrder);
        p.setPreOrderDays(plan.preOrder ? plan.preOrderDays : null);
        p.setBasePrice(minPrice);
    }

    /** Ghi biến thể theo hợp đồng mục 10.3: không ghi reserved_count; trả giá thấp nhất (base_price). */
    private BigDecimal writeVariants(long pid, Plan plan, long vendorUserId, String prefix, Set<String> ownedKeys) {
        Map<Long, ProductListingStore.VariantRow> current = new LinkedHashMap<>();
        store.activeVariants(pid).forEach(r -> current.put(r.id(), r));

        // biến thể bị bỏ: còn giữ chỗ thì chặn cả lần lưu
        Set<Long> keep = new HashSet<>();
        plan.variants.forEach(vi -> { if (vi.id() != null) keep.add(vi.id()); });
        List<Long> removed = current.keySet().stream().filter(id -> !keep.contains(id)).toList();
        List<Long> blocked = removed.stream().filter(id -> current.get(id).reserved() > 0).toList();
        if (!blocked.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_HAS_OPEN_ORDERS,
                    "Phân loại đang được giữ cho đơn chưa thanh toán — chưa bỏ được.", Map.of("variant_ids", blocked));
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (Long id : removed) {
            if (store.softRemoveVariant(pid, id, now) == 0) {
                throw new BusinessException(ErrorCode.PRODUCT_HAS_OPEN_ORDERS,
                        "Phân loại vừa được giữ cho một đơn — vui lòng thử lại.", Map.of("variant_ids", List.of(id)));
            }
        }

        // ảnh theo giá trị của nhóm có ảnh
        Map<String, String> valueImages = new HashMap<>();
        int imageGroupIndex = -1;
        for (int g = 0; g < plan.groups.size(); g++) {
            OptionGroupInput group = plan.groups.get(g);
            if (!group.name().trim().equals(plan.imageGroup)) continue;
            imageGroupIndex = g;
            for (int k = 0; k < group.values().size(); k++) {
                OptionValueInput val = group.values().get(k);
                if (val.imageObjectKey() != null && !val.imageObjectKey().isBlank()) {
                    valueImages.put(val.value().trim(), media(vendorUserId, UploadPurpose.VARIANT_IMAGE,
                            val.imageObjectKey().trim(), prefix, ownedKeys,
                            "option_groups[" + g + "].values[" + k + "].image_object_key"));
                }
            }
        }

        String generatedPrefix = SkuGenerator.generatedPrefix(pid);
        int seq = store.nextGeneratedSequence(pid, generatedPrefix);
        BigDecimal min = null;
        for (int i = 0; i < plan.variants.size(); i++) {
            VariantInput vi = plan.variants.get(i);
            List<String> options = vi.options() == null ? List.of()
                    : vi.options().stream().map(String::trim).toList();
            Map<String, String> attrs = new LinkedHashMap<>();
            for (int g = 0; g < options.size(); g++) {
                attrs.put(plan.groups.get(g).name().trim(), options.get(g));
            }
            String variantName = options.isEmpty() ? null : String.join(" / ", options);
            String image = imageGroupIndex >= 0 && imageGroupIndex < options.size()
                    ? valueImages.get(options.get(imageGroupIndex)) : null;
            BigDecimal price = BigDecimal.valueOf(vi.price() == null ? 0 : vi.price());
            int stock = vi.stock() == null ? 0 : vi.stock();
            String sku = vi.sku() == null || vi.sku().isBlank() ? null : vi.sku().trim();
            if (sku == null && vi.id() != null) {
                sku = current.get(vi.id()).sku();                       // giữ mã cũ khi người bán để trống
            }
            if (sku == null) {
                do {
                    sku = SkuGenerator.generated(pid, seq++);
                } while (store.skuTaken(sku, vi.id()));
            }
            if (vi.id() == null) {
                store.insertVariant(pid, sku, variantName, attrs, price, stock, image);
            } else if (store.updateVariant(pid, vi.id(), sku, variantName, attrs, price, stock, image) == 0) {
                // vừa có đơn giữ chỗ làm tồn mới < số giữ chỗ, hoặc biến thể vừa bị xóa
                throw new BusinessException(ErrorCode.PRODUCT_STATE_CONFLICT,
                        "Tồn kho vừa thay đổi do có đơn giữ chỗ — vui lòng tải lại và thử lại.",
                        Map.of("reason", "VARIANT_CHANGED", "variant_id", vi.id()));
            }
            if (vi.price() != null && (min == null || price.compareTo(min) < 0)) min = price;
        }
        return min == null ? BigDecimal.ZERO : min;
    }

    private void writeDocuments(long pid, Plan plan, long vendorUserId, String prefix) {
        List<ProductListingStore.DocumentRow> current = store.documents(pid);
        Set<String> wanted = new HashSet<>(plan.documentKeys);
        store.deleteDocuments(pid, current.stream().filter(d -> !wanted.contains(d.fileUrl()))
                .map(ProductListingStore.DocumentRow::id).toList());
        Set<String> kept = new HashSet<>();
        current.stream().map(ProductListingStore.DocumentRow::fileUrl).filter(wanted::contains).forEach(kept::add);
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (int i = 0; i < plan.documentKeys.size(); i++) {
            String key = plan.documentKeys.get(i);
            if (kept.contains(key)) continue;
            if (!isTmp(key)) {
                throw InvalidRequestException.of("document_object_keys[" + i + "]", "INVALID", key, "Tệp không hợp lệ.");
            }
            UploadService.StoredFile f = uploads.promoteTo(vendorUserId, UploadPurpose.PRODUCT_DOCUMENT, key,
                    prefix + "docs/");
            long size = storage.stat(UploadPurpose.PRODUCT_DOCUMENT.bucket(), f.objectKey())
                    .map(o -> o.size()).orElse(0L);
            String given = i < plan.documentNames.size() ? plan.documentNames.get(i) : null;
            String fileName = given == null || given.isBlank() ? SellerProductQueryService.fileName(f.objectKey())
                    : truncate(given.trim(), 255);
            String ext = f.objectKey().substring(f.objectKey().lastIndexOf('.') + 1);
            store.insertDocument(pid, f.objectKey(), fileName, truncate(ext, 20), size, now);
        }
    }

    /**
     * Khóa tệp người bán gửi: {@code tmp/…} của chính người đó → chuyển sang khóa chính thức; khóa khác phải là tệp sản
     * phẩm này đang dùng (sửa sản phẩm giữ ảnh cũ) — không cho trỏ tới tệp của sản phẩm / shop khác.
     */
    private String media(long vendorUserId, UploadPurpose purpose, String key, String prefix, Set<String> ownedKeys,
                         String field) {
        if (isTmp(key)) {
            return uploads.promoteTo(vendorUserId, purpose, key, prefix).objectKey();
        }
        if (ownedKeys.contains(key)) {
            return key;
        }
        throw InvalidRequestException.of(field, "INVALID", key, "Tệp không hợp lệ — vui lòng tải lại.");
    }

    private String promoteDescriptionImages(String html, long vendorUserId, String prefix) {
        if (html == null) return null;
        String tmpBase = sanitizer.imagePrefix() + "tmp/user/" + vendorUserId + "/";
        Matcher m = IMG_SRC.matcher(html);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String src = m.group(1);
            String replacement = src;
            if (src.startsWith(tmpBase)) {
                String key = src.substring(sanitizer.imagePrefix().length());
                replacement = uploads.promoteTo(vendorUserId, UploadPurpose.DESCRIPTION_IMAGE, key, prefix + "desc/")
                        .publicUrl();
            }
            m.appendReplacement(out, Matcher.quoteReplacement("src=\"" + replacement + "\""));
        }
        m.appendTail(out);
        return out.toString();
    }

    // ===================================================================================================
    // Tiện ích
    // ===================================================================================================

    private SellerShopPort.ShopFacts shopOf(long vendorUserId) {
        return shops.factsOf(vendorUserId).orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_VERIFIED));
    }

    /** "Trường quan trọng" hiện tại của sản phẩm (so trước / sau — D4). */
    private ProductEditPolicy.ReviewFields reviewFields(Product p) {
        Set<String> docs = new HashSet<>();
        store.documents(p.getId()).forEach(d -> docs.add(d.fileUrl()));
        // mô tả cũ lọc lại cùng whitelist — dữ liệu seed / cũ chưa lọc không bị coi là "đã sửa"
        String description = sanitizer.sanitize(p.getDescription()).html();
        return new ProductEditPolicy.ReviewFields(p.getName(), store.galleryKeys(p.getId()), p.getVideoUrl(),
                description.isEmpty() ? null : description, p.getCategoryId(), p.getBrandId(), docs,
                SpecCodec.parse(json, p.getSpecsJson()));
    }

    /** Khóa tệp sản phẩm này đang dùng (ảnh, ảnh phân loại, video). */
    private Set<String> ownedKeys(Product p) {
        Set<String> keys = new HashSet<>(store.galleryKeys(p.getId()));
        store.activeVariants(p.getId()).forEach(r -> { if (r.imageUrl() != null) keys.add(r.imageUrl()); });
        if (p.getVideoUrl() != null) keys.add(p.getVideoUrl());
        return keys;
    }

    private String categoryJson(Long categoryId) {
        if (categoryId == null) return null;
        CategoryIndex idx = catalog.index();
        List<Map<String, Object>> path = new ArrayList<>();
        List<CategoryRow> chain = new ArrayList<>(idx.ancestors(categoryId));
        idx.find(categoryId).ifPresent(chain::add);
        for (CategoryRow r : chain) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", r.id());
            node.put("name", r.name());
            node.put("slug", r.slug());
            path.add(node);
        }
        return store.toJson(path);
    }

    private static void checkDimension(String field, Integer value, List<Violation> v) {
        if (value != null && (value < 1 || value > MAX_DIMENSION_CM)) {
            v.add(viol(field, "OUT_OF_RANGE", value, "Mỗi chiều kích thước từ 1 đến 200 cm."));
        }
    }

    private static List<String> splitMulti(String value) {
        List<String> out = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) out.add(part.trim());
        }
        return out;
    }

    private static List<String> cleanKeys(Collection<String> keys) {
        if (keys == null) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String k : keys) {
            if (k != null && !k.isBlank()) out.add(k.trim());
        }
        return new ArrayList<>(out);
    }

    private static boolean isTmp(String key) {
        return key != null && key.startsWith("tmp/");
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static Violation viol(String field, String code, Object value, String message) {
        return new Violation(field, code, value, message);
    }
}
