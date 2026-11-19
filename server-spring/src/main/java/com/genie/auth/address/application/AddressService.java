package com.genie.auth.address.application;

import com.genie.auth.address.domain.Address;
import com.genie.auth.address.domain.AddressRepository;
import com.genie.auth.profile.domain.CustomerProfileRepository;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sổ địa chỉ giao hàng theo địa giới 2 cấp — UC-AUTH-09 (API_SPEC 3.8; plan AUTH A12.03, D25).
 * <ul>
 *   <li>Phường phải thuộc tỉnh theo {@link RegionDirectory}; tên lưu do server tra, không tin tên client gửi. Cột
 *       quận / huyện để {@code NULL} (địa chỉ cũ trước V16 vẫn giữ nguyên giá trị).
 *       Sai → {@code 422 GNE-VAL-4220} ({@code details.field = ward_code}).</li>
 *   <li>Địa chỉ đầu tiên tự thành mặc định; đặt mặc định thì bỏ cờ ở địa chỉ khác và ghi
 *       {@code customer_profiles.default_address_id}. Khóa hồ sơ ({@code SELECT … FOR UPDATE}) khi đổi mặc định để hai
 *       yêu cầu song song không tạo ra hai địa chỉ mặc định.</li>
 *   <li>Xóa là xóa mềm; địa chỉ mặc định không xóa được ({@code 409 GNE-USR-4092}) — đặt địa chỉ khác làm mặc định
 *       trước.</li>
 *   <li>Địa chỉ của người khác / đã xóa → {@code 404} (không lộ là có tồn tại).</li>
 *   <li>Không lưu trùng (plan AUTH A12.07, D26): cùng tỉnh + phường + SĐT + địa chỉ cụ thể (so bằng
 *       {@link AddressLines#normalize}) với một địa chỉ khác chưa xóa → {@code 409 GNE-USR-4093}
 *       ({@code details.existing_address_id}). Kiểm sau khi khóa hồ sơ nên hai yêu cầu song song không lọt.</li>
 * </ul>
 */
@Service
public class AddressService {

    private final AddressRepository addresses;
    private final CustomerProfileRepository profiles;
    private final RegionDirectory regions;

    public AddressService(AddressRepository addresses, CustomerProfileRepository profiles, RegionDirectory regions) {
        this.addresses = addresses;
        this.profiles = profiles;
        this.regions = regions;
    }

    @Transactional(readOnly = true)
    public List<AddressView> list(long userId) {
        return addresses.findActiveByUserId(userId).stream().map(AddressView::of).toList();
    }

    @Transactional
    public AddressView create(long userId, AddressInput input) {
        RegionDirectory.ResolvedRegion region = resolve(input);
        lockProfile(userId);
        rejectDuplicate(userId, null, region, input);
        boolean makeDefault = input.isDefault() || addresses.countActiveByUserId(userId) == 0;
        Address address = Address.builder()
                .userId(userId)
                .recipientName(input.recipientName().trim())
                .phoneNumber(input.phoneNumber().trim())
                .addressLine(input.addressLine().trim())
                .provinceId(region.province().id()).province(region.province().name())
                .wardCode(region.ward().code()).ward(region.ward().name())
                .label(input.label())
                .deliveryNote(input.cleanDeliveryNote())
                .isDefault(false)
                .build();
        address = addresses.saveAndFlush(address);
        if (makeDefault) {
            makeDefault(userId, address.getId());
        }
        return AddressView.of(reload(userId, address.getId()));
    }

    @Transactional
    public AddressView update(long userId, long addressId, AddressInput input) {
        RegionDirectory.ResolvedRegion region = resolve(input);
        find(userId, addressId);
        lockProfile(userId);
        rejectDuplicate(userId, addressId, region, input);
        Address address = find(userId, addressId);
        address.update(input.recipientName().trim(), input.phoneNumber().trim(), input.addressLine().trim(),
                region.province().id(), region.province().name(), region.ward().code(), region.ward().name(),
                input.label(), input.cleanDeliveryNote());
        addresses.saveAndFlush(address);
        // PUT không bỏ được cờ mặc định (muốn đổi thì đặt địa chỉ khác làm mặc định); chỉ bật thêm
        if (input.isDefault() && !address.isDefaultAddress()) {
            makeDefault(userId, addressId);
        }
        return AddressView.of(reload(userId, addressId));
    }

    /** {@code PATCH {is_default: true}} — chỉ nhận {@code true}. */
    @Transactional
    public AddressView setDefault(long userId, long addressId) {
        find(userId, addressId);
        lockProfile(userId);
        makeDefault(userId, addressId);
        return AddressView.of(reload(userId, addressId));
    }

    @Transactional
    public void delete(long userId, long addressId) {
        Address address = find(userId, addressId);
        if (address.isDefaultAddress()) {
            throw new BusinessException(ErrorCode.DEFAULT_ADDRESS_NOT_DELETABLE);
        }
        address.softDelete();
        addresses.save(address);
    }

    // ===== nội bộ =====

    private RegionDirectory.ResolvedRegion resolve(AddressInput input) {
        return regions.resolve(input.provinceId(), input.wardCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Tỉnh / phường không hợp lệ, vui lòng chọn lại.", Map.of("field", "ward_code")));
    }

    /**
     * Trùng với một địa chỉ khác (chưa xóa) của cùng người dùng? {@code exceptId} = địa chỉ đang sửa (sửa mà giữ
     * nguyên vẫn lưu được). Tên người nhận và loại địa chỉ không tính.
     */
    private void rejectDuplicate(long userId, Long exceptId, RegionDirectory.ResolvedRegion region,
                                 AddressInput input) {
        String phone = input.phoneNumber().trim();
        String line = AddressLines.normalize(input.addressLine());
        addresses.findActiveByUserId(userId).stream()
                .filter(a -> !a.getId().equals(exceptId))
                .filter(a -> a.getProvinceId() != null && a.getProvinceId() == region.province().id())
                .filter(a -> region.ward().code().equals(a.getWardCode()))
                .filter(a -> phone.equals(a.getPhoneNumber()))
                .filter(a -> line.equals(AddressLines.normalize(a.getAddressLine())))
                .findFirst()
                .ifPresent(a -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_ADDRESS,
                            Map.of("existing_address_id", a.getId()));
                });
    }

    private Address find(long userId, long addressId) {
        return addresses.findActiveByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy địa chỉ."));
    }

    private Address reload(long userId, long addressId) {
        return find(userId, addressId);
    }

    /** Khóa hồ sơ người mua (nếu có) — tuần tự hóa các thao tác đổi mặc định của cùng một người. */
    private void lockProfile(long userId) {
        profiles.findByUserIdForUpdate(userId);
    }

    /**
     * Bỏ cờ mặc định cũ rồi đặt cờ mới. {@code clearDefault} là câu UPDATE hàng loạt có xóa bộ nhớ đệm JPA, nên đọc lại
     * địa chỉ và hồ sơ SAU câu đó (khóa hồ sơ vẫn giữ vì cùng transaction).
     */
    private void makeDefault(long userId, long addressId) {
        addresses.clearDefault(userId);
        Address address = find(userId, addressId);
        address.markAsDefault();
        addresses.saveAndFlush(address);
        profiles.findByUserIdForUpdate(userId).ifPresent(p -> {
            p.setDefaultAddress(addressId);
            profiles.saveAndFlush(p);
        });
    }
}
