package com.genie.admin.banner.event;

/**
 * Banner vừa được tạo / sửa / bật / tắt / xóa (UC-ADM-12) — listener xóa cache {@code banner:live:*} sau khi commit
 * (plan banner N7, B04.04).
 *
 * @param bannerId id banner
 * @param op       {@code CREATE, UPDATE, ACTIVATE, DEACTIVATE, DELETE}
 */
public record BannerChangedEvent(long bannerId, String op) {
}
