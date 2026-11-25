package com.genie.vendor.settings.application;

import com.genie.auth.address.application.RegionDirectory;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.vendor.settings.application.PickupShopRegistrar.PickupInfo;
import com.genie.vendor.settings.application.PickupShopRegistrar.PickupSyncException;
import com.genie.vendor.shop.application.PublicShopService;
import com.genie.vendor.shop.application.SellerContext;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID — UC-VND-05 (API_SPEC 10.11 {@code PUT /seller/pickup-address}; plan Mở shop
 * & KYC K07.04, D12, D16). Chỉ shop {@code VERIFIED} (endpoint nằm trong danh sách mở của cổng KYC nên kiểm ở đây). Tên
 * tỉnh / phường do server tra từ danh mục hành chính 2 cấp — không tin client. GHN được gọi <b>ngoài transaction</b> (không
 * giữ khóa dòng khi chờ mạng); thành công mới lưu, lỗi → {@code 502 GNE-LOG-5022} và dữ liệu cũ giữ nguyên.
 */
@Service
public class PickupAddressService {

    private final VendorProfileRepository profiles;
    private final SellerContext sellers;
    private final RegionDirectory regions;
    private final PickupShopRegistrar registrar;
    private final PublicShopService publicShops;
    private final TransactionTemplate tx;

    public PickupAddressService(VendorProfileRepository profiles, SellerContext sellers, RegionDirectory regions,
                                PickupShopRegistrar registrar, PublicShopService publicShops, TransactionTemplate tx) {
        this.profiles = profiles;
        this.sellers = sellers;
        this.regions = regions;
        this.registrar = registrar;
        this.publicShops = publicShops;
        this.tx = tx;
    }

    /** Body API 10.11 — đã qua kiểm định dạng ở controller. */
    public record Input(String phoneNumber, int provinceId, String wardCode, String addressLine) {
    }

    public ShopSettingsService.PickupAddress save(long userId, Input in) {
        long shopId = sellers.requireVerified(userId).vendorProfileId();
        RegionDirectory.ResolvedRegion region = regions.resolve(in.provinceId(), in.wardCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Phường / xã không thuộc tỉnh / thành đã chọn.", Map.of("field", "ward_code")));
        VendorProfile current = profiles.findById(shopId).orElseThrow();
        String phone = in.phoneNumber().replaceAll("\\s", "");
        String line = in.addressLine().trim();
        PickupInfo info = new PickupInfo(shopId, current.getShopName(), phone, region.province().id(),
                region.province().name(), region.ward().code(), region.ward().name(), line);
        int ghnShopId;
        try {
            ghnShopId = registrar.sync(current.getGhnShopId(), info);
        } catch (PickupSyncException e) {
            throw new BusinessException(ErrorCode.GHN_SYNC_FAILED);
        }
        return tx.execute(status -> {
            VendorProfile p = profiles.findByIdForUpdate(shopId).orElseThrow();
            p.updatePickup(phone, region.province().id(), region.province().name(), region.ward().code(),
                    region.ward().name(), line, ghnShopId);
            // trang shop công khai hiển thị tỉnh của kho — xóa cache sau commit
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publicShops.evict(shopId);
                }
            });
            return ShopSettingsService.pickupOf(p);
        });
    }
}
