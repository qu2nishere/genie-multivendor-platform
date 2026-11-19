package com.genie.auth.address.application;

import com.genie.auth.address.domain.AddressRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Cửa công khai của sổ địa chỉ cho module khác (TS 4.4) — plan AUTH A12.04. */
@Component
public class AddressFacade {

    private final AddressRepository addresses;

    public AddressFacade(AddressRepository addresses) {
        this.addresses = addresses;
    }

    /**
     * Chụp địa chỉ để gắn vào phiên checkout / đơn hàng. Không phải của người này, hoặc đã xóa → rỗng (order trả
     * lỗi địa chỉ không hợp lệ).
     */
    @Transactional(readOnly = true)
    public Optional<AddressSnapshot> snapshotForCheckout(long userId, long addressId) {
        return addresses.findActiveByIdAndUserId(addressId, userId).map(a -> new AddressSnapshot(a.getId(),
                a.getRecipientName(), a.getPhoneNumber(), a.getAddressLine(), a.getWardCode(), a.getWard(),
                a.getProvinceId(), a.getProvince(), a.getDeliveryNote()));
    }

    /** Địa chỉ mặc định của người mua (checkout chọn sẵn); chưa có → rỗng. */
    @Transactional(readOnly = true)
    public Optional<AddressSnapshot> defaultFor(long userId) {
        return addresses.findActiveByUserId(userId).stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                .findFirst().flatMap(a -> snapshotForCheckout(userId, a.getId()));
    }
}
