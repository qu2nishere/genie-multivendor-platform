package com.genie.vendor.kyc.event;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Hồ sơ KYC vừa có quyết định: tự duyệt / từ chối bởi AI (UC-VND-03) hoặc nhân viên duyệt / từ chối / yêu cầu bổ sung,
 * quá hạn bổ sung (UC-VND-04). Người nghe ({@code AFTER_COMMIT}): email kết quả cho chủ shop ({@code KycNotifier}),
 * thông báo in-app (module notification khi hiện thực).
 *
 * @param status             {@code VERIFIED | REJECTED | SUPPLEMENT_REQUESTED}
 * @param reason             lý do từ chối / ghi chú bổ sung (null khi duyệt)
 * @param supplementDocuments giấy tờ cần bổ sung (rỗng nếu không phải yêu cầu bổ sung)
 * @param deadlineAt         hạn bổ sung (null nếu không phải yêu cầu bổ sung)
 */
public record KycDecidedEvent(long vendorProfileId, long vendorUserId, String status, String reason,
                              List<String> supplementDocuments, OffsetDateTime deadlineAt) {
}
