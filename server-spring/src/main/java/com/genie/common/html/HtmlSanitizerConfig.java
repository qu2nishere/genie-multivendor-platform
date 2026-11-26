// Bean HtmlSanitizer — ảnh chỉ được trỏ vào bucket công khai content-assets của sàn (plan 2026-11-26 thông báo D13)
package com.genie.common.html;

import com.genie.common.storage.ObjectStorage;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HtmlSanitizerConfig {

    /** Tiền tố URL công khai của {@code content-assets} (cùng cách ghép với {@code S3ObjectStorage.publicUrl}). */
    @Bean
    HtmlSanitizer htmlSanitizer(ObjectStorage storage) {
        return new HtmlSanitizer(List.of(storage.publicUrl("content-assets", "")));
    }
}
