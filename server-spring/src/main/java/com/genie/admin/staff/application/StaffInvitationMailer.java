// Gửi email mời sau commit, chạy nền (plan B1 R04.03; TS 4.5). Lỗi chỉ ghi log — Admin bấm "Gửi lại lời mời".
package com.genie.admin.staff.application;

import com.genie.admin.staff.domain.AdminProfile;
import com.genie.admin.staff.domain.AdminProfileRepository;
import com.genie.admin.staff.domain.StaffTitle;
import com.genie.auth.profile.application.StaffInvitationMail;
import com.genie.auth.profile.application.UserFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StaffInvitationMailer {

    private static final Logger log = LoggerFactory.getLogger(StaffInvitationMailer.class);

    private final UserFacade users;
    private final AdminProfileRepository profiles;

    public StaffInvitationMailer(UserFacade users, AdminProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvited(StaffInvitedEvent event) {
        try {
            AdminProfile staff = profiles.findByUserId(event.staffUserId()).orElse(null);
            String inviter = profiles.findByUserId(event.inviterId()).map(AdminProfile::getFullName)
                    .orElse("Quản trị viên");
            users.sendStaffInvitation(event.staffUserId(), new StaffInvitationMail(
                    staff == null ? null : staff.getFullName(), inviter,
                    titleLabel(staff == null ? null : staff.getStaffTitle())));
        } catch (RuntimeException e) {
            log.warn("Không gửi được email mời nhân viên {}: {}", event.staffUserId(), e.getMessage());
        }
    }

    static String titleLabel(StaffTitle title) {
        if (title == null) {
            return "Nhân viên vận hành";
        }
        return switch (title) {
            case MODERATOR -> "Nhân viên kiểm duyệt";
            case CS_SPECIALIST -> "Nhân viên chăm sóc khách hàng";
            case FINANCE_OFFICER -> "Nhân viên tài chính";
        };
    }
}
