package com.example.auth.config;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class LogbackConfigurationTest {

    @Test
    void logbackUsesCompressedArchiveNamingAndRetentionLimits() throws Exception {
        String xml = logbackXml();

        assertThat(xml)
                .contains("archive/${springAppName}.%d{yyyy-MM-dd}.%i.log.gz")
                .contains("<maxFileSize>20MB</maxFileSize>")
                .contains("<maxHistory>30</maxHistory>")
                .contains("<totalSizeCap>1GB</totalSizeCap>")
                .contains("<cleanHistoryOnStart>true</cleanHistoryOnStart>");
    }

    @Test
    void logbackUsesProfileSpecificAppenderStrategyAndPortableLogPath() throws Exception {
        String xml = logbackXml();

        assertThat(xml)
                .contains("default | local | test")
                .contains("dev | prod")
                .contains("<logger name=\"com.example\" level=\"DEBUG\"/>")
                .doesNotContain("../logs");
    }

    private String logbackXml() throws Exception {
        try (var inputStream = new ClassPathResource("logback-spring.xml").getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
