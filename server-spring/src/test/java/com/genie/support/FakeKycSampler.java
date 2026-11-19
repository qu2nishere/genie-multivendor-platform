package com.genie.support;

import com.genie.vendor.kyc.application.KycSampler;

/** Lấy mẫu hậu kiểm cố định trong test — mặc định không bao giờ lấy mẫu. */
public class FakeKycSampler extends KycSampler {

    private volatile boolean sample;

    @Override
    public boolean sample(double rate) {
        return sample;
    }

    public void set(boolean value) {
        this.sample = value;
    }
}
