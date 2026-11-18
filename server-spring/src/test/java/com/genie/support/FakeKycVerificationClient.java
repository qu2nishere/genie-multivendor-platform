package com.genie.support;

import com.genie.vendor.kyc.application.KycVerificationClient;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thay AI Worker trong test (plan Mở shop & KYC K05.12–K05.13): ghi lại yêu cầu gửi đi, không tự trả kết quả — test gọi
 * callback / {@code KycDecisionService} trực tiếp. {@link #failNext(boolean)} giả lập AI Worker tắt.
 */
public class FakeKycVerificationClient implements KycVerificationClient {

    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private volatile boolean fail;

    @Override
    public void requestVerification(Request request) {
        if (fail) {
            throw new IllegalStateException("AI Worker giả lập đang tắt");
        }
        requests.add(request);
    }

    public void failNext(boolean value) {
        this.fail = value;
    }

    public List<Request> requests() {
        return requests;
    }

    public void clear() {
        requests.clear();
        fail = false;
    }
}
