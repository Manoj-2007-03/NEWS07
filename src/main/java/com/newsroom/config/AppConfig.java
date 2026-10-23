package com.newsroom.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Central configuration manager for NEWSROOM Phase 2.
 * Securely loads settings from local properties file and environment variables.
 * Demonstrates Encapsulation and Security Best Practices (never logs raw secrets).
 */
public class AppConfig {
    private static final String CONFIG_FILE_NAME = "newsroom.properties";
    private static volatile AppConfig instance;

    private String apiKey;
    private String apiBaseUrl;
    private String providerStrategy;
    private String country;
    private int refreshIntervalMinutes;
    private String dbPath;
    private int networkTimeoutSeconds;

    private AppConfig() {
        loadConfiguration();
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    public synchronized void loadConfiguration() {
        Properties props = new Properties();
        File file = new File(CONFIG_FILE_NAME);

        if (file.exists() && file.isFile()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Notice: Could not read " + CONFIG_FILE_NAME + ", falling back to env/defaults.");
            }
        }

        // 1. API Key: properties file takes precedence, followed by environment variable
        String envKey = System.getenv("NEWS_API_KEY");
        this.apiKey = props.getProperty("news.api.key", envKey != null ? envKey : "").trim();

        // 2. Base URL
        this.apiBaseUrl = props.getProperty("news.api.base.url", "https://newsapi.org/v2").trim();

        // 3. Provider Strategy: RSS, NEWS_API, COMPOSITE
        String envProvider = System.getenv("NEWS_PROVIDER");
        this.providerStrategy = props.getProperty("news.provider", envProvider != null ? envProvider : "COMPOSITE").trim().toUpperCase();

        // 4. Country
        this.country = props.getProperty("news.country", "us").trim().toLowerCase();

        // 5. Refresh Interval (in minutes)
        try {
            this.refreshIntervalMinutes = Integer.parseInt(props.getProperty("news.refresh.interval", "30").trim());
        } catch (NumberFormatException e) {
            this.refreshIntervalMinutes = 30;
        }

        // 6. Database Path
        this.dbPath = props.getProperty("db.path", "newsroom.db").trim();

        // 7. Network Timeout
        try {
            this.networkTimeoutSeconds = Integer.parseInt(props.getProperty("network.timeout.seconds", "10").trim());
        } catch (NumberFormatException e) {
            this.networkTimeoutSeconds = 10;
        }
    }

    public synchronized void saveApiKey(String newKey) {
        this.apiKey = newKey != null ? newKey.trim() : "";
        File file = new File(CONFIG_FILE_NAME);
        Properties props = new Properties();

        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException ignored) {}
        }

        props.setProperty("news.api.key", this.apiKey);
        props.setProperty("news.provider", this.providerStrategy);
        props.setProperty("news.country", this.country);
        props.setProperty("news.refresh.interval", String.valueOf(this.refreshIntervalMinutes));
        props.setProperty("db.path", this.dbPath);

        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "NEWSROOM Configuration");
        } catch (IOException e) {
            System.err.println("Notice: Failed to persist configuration to file: " + e.getMessage());
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean hasValidApiKey() {
        return apiKey != null && !apiKey.isEmpty() && apiKey.length() >= 16;
    }

    public String getMaskedApiKey() {
        if (!hasValidApiKey()) {
            return "Not Configured (Real-Time RSS Active)";
        }
        if (apiKey.length() <= 8) {
            return "••••••••";
        }
        return apiKey.substring(0, 4) + "••••••••" + apiKey.substring(apiKey.length() - 4);
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getProviderStrategy() {
        return providerStrategy;
    }

    public void setProviderStrategy(String providerStrategy) {
        if (providerStrategy != null) {
            this.providerStrategy = providerStrategy.trim().toUpperCase();
        }
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        if (country != null) {
            this.country = country.trim().toLowerCase();
        }
    }

    public int getRefreshIntervalMinutes() {
        return refreshIntervalMinutes;
    }

    public void setRefreshIntervalMinutes(int refreshIntervalMinutes) {
        this.refreshIntervalMinutes = Math.max(0, refreshIntervalMinutes);
    }

    public String getDbPath() {
        return dbPath;
    }

    public int getNetworkTimeoutSeconds() {
        return networkTimeoutSeconds;
    }
}
