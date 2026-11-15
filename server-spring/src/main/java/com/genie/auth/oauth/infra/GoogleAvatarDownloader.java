package com.genie.auth.oauth.infra;

import com.genie.auth.oauth.application.AvatarDownloader;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Tải ảnh đại diện Google ({@code picture} trong {@code id_token}) để lưu vào Silo.
 * <ul>
 *   <li>Chỉ nhận {@code https://*.googleusercontent.com} — không cho backend bị lợi dụng tải địa chỉ tùy ý (SSRF).</li>
 *   <li>Đổi cỡ {@code =s96-c} thành {@code =s256-c} (ảnh vuông 256 px, nét hơn, vẫn nhỏ).</li>
 *   <li>Chỉ JPEG / PNG, ≤ {@code maxBytes}; timeout 3 s / 5 s.</li>
 * </ul>
 */
@Component
public class GoogleAvatarDownloader implements AvatarDownloader {

    private static final Logger log = LoggerFactory.getLogger(GoogleAvatarDownloader.class);
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png");
    static final String SIZE = "s256-c";

    private final RestClient http;

    public GoogleAvatarDownloader(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);
        this.http = builder.requestFactory(factory).build();
    }

    @Override
    public Optional<DownloadedImage> download(String url, long maxBytes) {
        URI uri = allowed(url);
        if (uri == null) {
            return Optional.empty();
        }
        try {
            ResponseEntity<byte[]> r = http.get().uri(uri)
                    .header(HttpHeaders.ACCEPT, "image/jpeg, image/png;q=0.9")
                    .retrieve().toEntity(byte[].class);
            byte[] body = r.getBody();
            String type = r.getHeaders().getContentType() == null ? null
                    : r.getHeaders().getContentType().getType() + "/" + r.getHeaders().getContentType().getSubtype();
            if (body == null || body.length == 0 || body.length > maxBytes || type == null
                    || !TYPES.contains(type.toLowerCase(Locale.ROOT))) {
                log.info("Bỏ qua ảnh Google: loại {} cỡ {}", type, body == null ? 0 : body.length);
                return Optional.empty();
            }
            return Optional.of(new DownloadedImage(body, type.toLowerCase(Locale.ROOT)));
        } catch (RestClientException e) {
            log.info("Không tải được ảnh Google: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** {@code https://lh3.googleusercontent.com/a/xyz=s96-c} → bản 256 px; địa chỉ khác → null. */
    static URI allowed(String url) {
        if (url == null || url.isBlank() || url.length() > 2048) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || !host.toLowerCase(Locale.ROOT).endsWith(".googleusercontent.com")) {
                return null;
            }
            return URI.create(url.trim().replaceFirst("=s\\d+(-c)?$", "=" + SIZE));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
