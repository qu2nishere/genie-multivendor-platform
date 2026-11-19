package com.genie.auth.address.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genie.auth.address.domain.Address;
import com.genie.auth.address.domain.AddressLabel;

/** {@code $Address} (API_SPEC 1, 3.8) — địa giới 2 cấp: tỉnh + phường (plan AUTH D25). */
public record AddressView(long id, String recipientName, String phoneNumber, RegionDirectory.Region province,
                          RegionDirectory.Ward ward, String addressLine, String deliveryNote, AddressLabel label,
                          @JsonProperty("is_default") boolean isDefault) {

    static AddressView of(Address a) {
        return new AddressView(a.getId(), a.getRecipientName(), a.getPhoneNumber(),
                new RegionDirectory.Region(a.getProvinceId(), a.getProvince()),
                new RegionDirectory.Ward(a.getWardCode(), a.getWard()), a.getAddressLine(), a.getDeliveryNote(),
                a.getLabel() == null ? AddressLabel.HOME : a.getLabel(), a.isDefaultAddress());
    }
}
