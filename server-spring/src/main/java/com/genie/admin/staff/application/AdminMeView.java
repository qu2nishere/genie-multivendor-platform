// API 11.1 GET /admin/me — khung Admin: tên, ảnh, vai trò, chức danh, quyền (plan B1 — bổ sung để nối web)
package com.genie.admin.staff.application;

import java.util.List;

public record AdminMeView(long id, String fullName, String email, String avatarUrl, String role, String staffTitle,
                          List<String> permissions, String status) {
}
