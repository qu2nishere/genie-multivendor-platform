package com.genie.support;

import com.genie.vendor.settings.application.PickupShopRegistrar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** GHN giả cho kho lấy hàng (plan Mở shop & KYC K07.05) — ghi lại lời gọi, bật {@link #failNext} để thử GHN lỗi. */
public class FakePickupShopRegistrar implements PickupShopRegistrar {

    /** Một lời gọi {@code sync}: Shop ID đang lưu lúc gọi + thông tin kho gửi đi. */
    public record Call(Integer currentGhnShopId, PickupInfo info) {
    }

    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private volatile boolean failNext;

    @Override
    public int sync(Integer currentGhnShopId, PickupInfo info) {
        calls.add(new Call(currentGhnShopId, info));
        if (failNext) {
            failNext = false;
            throw new PickupSyncException("GHN giả lỗi", null);
        }
        return currentGhnShopId != null ? currentGhnShopId : Math.toIntExact(770_000 + info.vendorProfileId());
    }

    public void failNext() {
        this.failNext = true;
    }

    /** Các lời gọi cho một shop (IT dùng chung bean giữa các test). */
    public List<Call> callsFor(long vendorProfileId) {
        return calls.stream().filter(c -> c.info().vendorProfileId() == vendorProfileId).toList();
    }
}
