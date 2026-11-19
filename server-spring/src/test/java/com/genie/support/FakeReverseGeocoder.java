package com.genie.support;

import com.genie.auth.address.application.ReverseGeocoder;
import java.util.Optional;

/** Thay Nominatim trong test (không gọi mạng): {@link #answer} đặt kết quả cho lần định vị tiếp theo. */
public class FakeReverseGeocoder implements ReverseGeocoder {

    private volatile GeoPlace next;

    public void answer(GeoPlace place) {
        this.next = place;
    }

    public void clear() {
        this.next = null;
    }

    @Override
    public Optional<GeoPlace> lookup(double lat, double lng) {
        return Optional.ofNullable(next);
    }
}
