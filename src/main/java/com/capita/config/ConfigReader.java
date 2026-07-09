package com.capita.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input =
                     ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new RuntimeException("config.properties not found on classpath");
            }

            properties.load(input);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String getProperty(String key) {
        // Environment variables take priority (e.g. for Docker/CI overrides).
        // Converts camelCase key -> UPPER_SNAKE_CASE env var name,
        // e.g. "gridUrl" -> "GRID_URL", "baseUrl" -> "BASE_URL".
        String envKey = key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        String envValue = System.getenv(envKey);

        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Missing property in config.properties: " + key);
        }
        return value;
    }
}