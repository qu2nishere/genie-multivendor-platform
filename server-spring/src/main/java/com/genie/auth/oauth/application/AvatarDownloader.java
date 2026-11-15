package com.genie.auth.oauth.application;

import java.util.Optional;

/**
 * Tải ảnh đại diện từ nhà cung cấp đăng nhập (cài đặt: {@code infra.GoogleAvatarDownloader}). Không được làm hỏng đăng
 * nhập: lỗi mạng / ảnh quá lớn / sai định dạng / địa chỉ lạ → {@link Optional#empty()}.
 */
public interface AvatarDownloader {

    Optional<DownloadedImage> download(String url, long maxBytes);

    record DownloadedImage(byte[] data, String contentType) {
    }
}
