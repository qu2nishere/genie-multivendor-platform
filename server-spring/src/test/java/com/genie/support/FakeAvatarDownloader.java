package com.genie.support;

import com.genie.auth.oauth.application.AvatarDownloader;
import java.util.Optional;

/**
 * Ảnh Google giả (không gọi mạng): URL {@code https://lh3.googleusercontent.com/…} → ảnh PNG nhỏ; URL có chữ
 * {@code broken} → tải lỗi (giống mạng / Google trả lỗi).
 */
public class FakeAvatarDownloader implements AvatarDownloader {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

    @Override
    public Optional<DownloadedImage> download(String url, long maxBytes) {
        if (url == null || url.contains("broken") || !url.startsWith("https://lh3.googleusercontent.com/")) {
            return Optional.empty();
        }
        return Optional.of(new DownloadedImage(PNG.clone(), "image/png"));
    }
}
