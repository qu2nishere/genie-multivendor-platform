package com.genie.auth.oauth.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Chỉ tải ảnh từ máy chủ ảnh của Google (chống SSRF) và lấy bản 256 px. */
class GoogleAvatarDownloaderTest {

    @Test
    void acceptsGoogleUserContentAndUpscales() {
        assertThat(GoogleAvatarDownloader.allowed("https://lh3.googleusercontent.com/a/ACg8oc=s96-c"))
                .hasToString("https://lh3.googleusercontent.com/a/ACg8oc=s256-c");
        assertThat(GoogleAvatarDownloader.allowed("https://lh3.googleusercontent.com/a/ACg8oc"))
                .hasToString("https://lh3.googleusercontent.com/a/ACg8oc");
    }

    @Test
    void rejectsOtherHostsAndPlainHttp() {
        assertThat(GoogleAvatarDownloader.allowed("http://lh3.googleusercontent.com/a/x")).isNull();
        assertThat(GoogleAvatarDownloader.allowed("https://evil.com/a.png")).isNull();
        assertThat(GoogleAvatarDownloader.allowed("https://googleusercontent.com.evil.com/a.png")).isNull();
        assertThat(GoogleAvatarDownloader.allowed("http://169.254.169.254/latest/meta-data")).isNull();
        assertThat(GoogleAvatarDownloader.allowed(null)).isNull();
    }
}
