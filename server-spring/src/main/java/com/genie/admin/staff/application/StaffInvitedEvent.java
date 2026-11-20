// Sự kiện nội bộ module admin: vừa mời / gửi lại lời mời → gửi email sau commit (plan B1 mục 3.1, 3.2)
package com.genie.admin.staff.application;

public record StaffInvitedEvent(long staffUserId, long inviterId) {
}
