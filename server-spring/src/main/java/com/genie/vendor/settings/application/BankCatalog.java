package com.genie.vendor.settings.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Danh mục ngân hàng Việt Nam ({@code resources/banks/banks.json}) — API 10.11 {@code GET /public/banks}; plan K08.01. */
@Component
public class BankCatalog {

    public record Bank(String code, String name) {
    }

    private final List<Bank> banks;

    public BankCatalog(ObjectMapper json) {
        try (InputStream in = new ClassPathResource("banks/banks.json").getInputStream()) {
            this.banks = List.copyOf(json.readValue(in, new TypeReference<List<Bank>>() { }));
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được banks/banks.json", e);
        }
    }

    public List<Bank> all() {
        return banks;
    }

    public Optional<Bank> find(String code) {
        return banks.stream().filter(b -> b.code().equalsIgnoreCase(code == null ? "" : code.trim())).findFirst();
    }
}
