// Cài cổng KycAdminNotifier: báo đội vận hành có hồ sơ KYC mới (UC-VND-02, UC-NOT-01 bước 9; plan 2026-11-26 thông báo N03.07)
package com.genie.vendor.kyc.application;

import com.genie.notification.admin.application.AdminInboxFacade;
import com.genie.notification.admin.application.StaffAlertKind;
import org.springframework.stereotype.Component;

/**
 * Đặt ở module {@code vendor} (cạnh {@code vendor → notification}) — không để {@code notification} cài cổng của vendor
 * (sẽ thành vòng {@code vendor → social → notification → vendor}, README mục 3). Được gọi từ
 * {@link KycSubmittedNotificationListener} (sau commit) nên ghi ngay.
 */
@Component
public class KycAdminNotifierImpl implements KycAdminNotifier {

    private final AdminInboxFacade adminInbox;

    public KycAdminNotifierImpl(AdminInboxFacade adminInbox) {
        this.adminInbox = adminInbox;
    }

    @Override
    public void kycSubmitted(long vendorProfileId, String shopName) {
        String name = shopName == null || shopName.isBlank() ? "#" + vendorProfileId : shopName;
        adminInbox.raiseNow(StaffAlertKind.USER_APPROVAL, "Hồ sơ KYC mới: " + name,
                "Gian hàng " + name + " vừa nộp hồ sơ KYC, cần thẩm định.", "/admin/kyc");
    }
}
