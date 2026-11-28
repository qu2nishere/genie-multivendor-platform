// Bean giả thay ProductScreeningClient trong test tích hợp — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.13)
package com.genie.support;

import com.genie.catalog.moderation.application.ProductScreeningClient;
import com.genie.catalog.moderation.application.ScreeningFailedException;
import com.genie.catalog.moderation.application.ScreeningRequest;
import com.genie.catalog.moderation.application.ScreeningVerdict;
import com.genie.catalog.moderation.domain.ModerationDecision;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Theo đúng mẫu {@link FakeKycVerificationClient}: ghi nhận các {@link ScreeningRequest} đã gửi và trả về kết quả định
 * sẵn (mặc định {@code APPROVE 10}), hoặc ném {@link ScreeningFailedException} khi bật {@link #willFail()}.
 */
public class FakeProductScreeningClient implements ProductScreeningClient {

    private static final ScreeningVerdict DEFAULT_VERDICT = new ScreeningVerdict(
            ModerationDecision.APPROVE, 10, List.of(), "Không phát hiện vi phạm.", null, "stub");

    private final List<ScreeningRequest> requests = new CopyOnWriteArrayList<>();
    private volatile ScreeningVerdict next = DEFAULT_VERDICT;
    private volatile boolean failNext;

    @Override
    public ScreeningVerdict screen(ScreeningRequest request) {
        requests.add(request);
        if (failNext) {
            throw new ScreeningFailedException("Lỗi giả lập từ FakeProductScreeningClient");
        }
        return next;
    }

    public void willReturn(ScreeningVerdict verdict) {
        this.failNext = false;
        this.next = verdict;
    }

    public void willFail() {
        this.failNext = true;
    }

    public List<ScreeningRequest> requests() {
        return requests;
    }

    public void clear() {
        requests.clear();
        next = DEFAULT_VERDICT;
        failNext = false;
    }
}
