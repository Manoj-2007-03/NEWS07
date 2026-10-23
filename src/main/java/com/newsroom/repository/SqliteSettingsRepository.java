package com.newsroom.repository;

import com.newsroom.database.DatabaseManager;
import com.newsroom.model.Category;
import com.newsroom.model.ReaderSettings;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * SQLite JDBC implementation of SettingsRepository.
 * Ensures user reading preferences, theme choices, and intervals survive desktop restarts.
 */
public class SqliteSettingsRepository implements SettingsRepository {
    private final DatabaseManager dbManager;

    public SqliteSettingsRepository() {
        this(DatabaseManager.getInstance());
    }

    public SqliteSettingsRepository(DatabaseManager dbManager) {
        this.dbManager = Objects.requireNonNull(dbManager, "DatabaseManager must not be null");
    }

    @Override
    public void saveSetting(String key, String value) {
        if (key == null) return;
        String sql = "INSERT INTO reader_settings (setting_key, setting_value) VALUES (?, ?) " +
                     "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value;";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value != null ? value : "");
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving setting '" + key + "' in SQLite: " + e.getMessage());
        }
    }

    @Override
    public String getSetting(String key, String defaultValue) {
        if (key == null) return defaultValue;
        String sql = "SELECT setting_value FROM reader_settings WHERE setting_key = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("setting_value");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading setting '" + key + "': " + e.getMessage());
        }
        return defaultValue;
    }

    @Override
    public void loadInto(ReaderSettings settings) {
        if (settings == null) return;

        try {
            String fontSizeStr = getSetting("body_font_size", null);
            if (fontSizeStr != null) {
                settings.setBodyFontSize(Integer.parseInt(fontSizeStr));
            }

            String darkModeStr = getSetting("dark_mode", null);
            if (darkModeStr != null) {
                settings.setDarkMode(Boolean.parseBoolean(darkModeStr));
            }

            String defaultCatStr = getSetting("default_category", null);
            if (defaultCatStr != null) {
                settings.setDefaultStartupCategory(Category.fromDisplayName(defaultCatStr));
            }

            String providerStr = getSetting("preferred_news_provider", null);
            if (providerStr != null) {
                settings.setPreferredNewsProvider(providerStr);
            }

            String refreshStr = getSetting("refresh_interval_minutes", null);
            if (refreshStr != null) {
                settings.setRefreshIntervalMinutes(Integer.parseInt(refreshStr));
            }

            String countryStr = getSetting("preferred_country", null);
            if (countryStr != null) {
                settings.setPreferredCountry(countryStr);
            }

            String lastRefreshedStr = getSetting("last_refreshed_at", null);
            if (lastRefreshedStr != null) {
                settings.setLastRefreshedAt(LocalDateTime.parse(lastRefreshedStr));
            }
        } catch (Exception e) {
            System.err.println("Notice: Could not parse some persistent settings: " + e.getMessage());
        }
    }

    @Override
    public void persist(ReaderSettings settings) {
        if (settings == null) return;

        saveSetting("body_font_size", String.valueOf(settings.getBodyFontSize()));
        saveSetting("dark_mode", String.valueOf(settings.isDarkMode()));
        saveSetting("default_category", settings.getDefaultStartupCategory().name());
        saveSetting("preferred_news_provider", settings.getPreferredNewsProvider());
        saveSetting("refresh_interval_minutes", String.valueOf(settings.getRefreshIntervalMinutes()));
        saveSetting("preferred_country", settings.getPreferredCountry());
        if (settings.getLastRefreshedAt() != null) {
            saveSetting("last_refreshed_at", settings.getLastRefreshedAt().toString());
        }
    }
}
