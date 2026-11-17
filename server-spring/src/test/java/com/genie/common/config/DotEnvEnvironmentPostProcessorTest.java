package com.genie.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.junit.jupiter.api.io.TempDir;

class DotEnvEnvironmentPostProcessorTest {

    @Test
    void parsesKeyValueLinesAndSkipsCommentsBlanksAndEmptyValues() {
        Map<String, Object> v = DotEnvEnvironmentPostProcessor.parse(List.of(
                "# comment", "", "SMTP_HOST=smtp.gmail.com", "export SMTP_PORT=587",
                "SMTP_PASSWORD=\"abcd efgh ijkl mnop\"", "MAIL_FROM='Genie Shop <shop@gmail.com>'",
                "GOOGLE_CLIENT_SECRET=", "not a pair", "URL=jdbc:postgresql://localhost:5432/genie?a=b"));

        assertThat(v).containsEntry("SMTP_HOST", "smtp.gmail.com").containsEntry("SMTP_PORT", "587")
                .containsEntry("SMTP_PASSWORD", "abcd efgh ijkl mnop")
                .containsEntry("MAIL_FROM", "Genie Shop <shop@gmail.com>")
                .containsEntry("URL", "jdbc:postgresql://localhost:5432/genie?a=b")
                .doesNotContainKey("GOOGLE_CLIENT_SECRET").hasSize(5);
    }

    @Test
    void findsEnvFileInParentDirectory(@TempDir Path root) throws Exception {
        Path module = Files.createDirectories(root.resolve("server-spring"));
        Files.writeString(root.resolve(".env"), "A=1");

        assertThat(DotEnvEnvironmentPostProcessor.find(module)).isEqualTo(root.resolve(".env"));
        assertThat(DotEnvEnvironmentPostProcessor.find(Files.createDirectories(root.resolve("x/y/z/w")))).isNull();
    }

    @Test
    void mapsSpringProfilesActiveSoDotEnvCanChooseTheProfile() {
        Map<String, Object> v = DotEnvEnvironmentPostProcessor.withSpringAliases(
                DotEnvEnvironmentPostProcessor.parse(List.of("SPRING_PROFILES_ACTIVE=local", "DB_USER=genie")));

        assertThat(v).containsEntry("spring.profiles.active", "local").containsEntry("SPRING_PROFILES_ACTIVE", "local")
                .containsEntry("DB_USER", "genie");
    }

    @Test
    void runsBeforeSpringReadsApplicationYamlAndProfiles() {
        assertThat(new DotEnvEnvironmentPostProcessor().getOrder())
                .isLessThan(ConfigDataEnvironmentPostProcessor.ORDER);
    }
}
