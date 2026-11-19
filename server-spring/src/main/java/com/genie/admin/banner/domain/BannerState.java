package com.genie.admin.banner.domain;

/**
 * Trạng thái hiển thị của banner — tính từ {@code isActive} + khung giờ, KHÔNG lưu CSDL (API_SPEC 11.16;
 * plan banner D3). {@code OFF} = tắt tay · {@code SCHEDULED} = chưa tới giờ · {@code ENDED} = đã hết giờ · {@code LIVE}.
 */
public enum BannerState { LIVE, SCHEDULED, ENDED, OFF }
