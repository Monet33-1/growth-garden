package com.grow.lyy.desktop;

import com.grow.lyy.common.config.AIProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Writes desktop-only settings into the per-user configuration directory. */
@Service
@Profile("desktop")
@RequiredArgsConstructor
public class DesktopConfigService {

    private static final String API_KEY = "ai.deepseek.api-key";

    private final AIProperties aiProperties;

    public void saveDeepSeekKey(String apiKey) {
        String value = apiKey == null ? "" : apiKey.trim();
        Path path = configPath();
        Properties properties = new Properties();

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new IllegalStateException("无法读取桌面配置: " + path, e);
            }
        }

        if (value.isEmpty()) {
            properties.remove(API_KEY);
        } else {
            properties.setProperty(API_KEY, value);
        }

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                properties.store(writer, "Growth Garden desktop settings");
            }
        } catch (IOException e) {
            throw new IllegalStateException("无法写入桌面配置: " + path, e);
        }

        aiProperties.setApiKey(value.isEmpty() ? null : value);
    }

    private Path configPath() {
        return Path.of(
                System.getProperty("user.home"),
                ".growth-garden",
                "config",
                "application-local.properties"
        );
    }
}
