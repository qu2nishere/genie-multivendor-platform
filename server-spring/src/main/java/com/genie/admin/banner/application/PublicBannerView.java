package com.genie.admin.banner.application;

/** Một banner đang hiển thị trên storefront (API_SPEC 5.14) — cũng là dạng lưu trong cache Redis. */
public record PublicBannerView(long id, String title, String imageUrl, String mobileImageUrl, String targetUrl) {
}
