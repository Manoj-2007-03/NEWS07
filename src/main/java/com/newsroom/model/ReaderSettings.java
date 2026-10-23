package com.newsroom.model;

import java.time.LocalDateTime;

/**
 * Encapsulates user preferences for the reading experience.
 * Demonstrates Encapsulation with defensive boundary checks and defaults.
 * Phase 2 Extensions: provider preference, refresh intervals, country filters.
 */
public class ReaderSettings {
    public static final int FONT_SIZE_SMALL = 14;
    public static final int FONT_SIZE_MEDIUM = 16;
    public static final int FONT_SIZE_LARGE = 18;
    public static final int FONT_SIZE_XLARGE = 21;

    private int bodyFontSize;
    private boolean darkMode;
    private Category defaultStartupCategory;
    private String preferredNewsProvider;
    private int refreshIntervalMinutes;
    private String preferredCountry;
    private LocalDateTime lastRefreshedAt;

    public ReaderSettings() {
        resetToDefaults();
    }

    public void resetToDefaults() {
        this.bodyFontSize = FONT_SIZE_MEDIUM;
        this.darkMode = false;
        this.defaultStartupCategory = Category.HOME;
        this.preferredNewsProvider = "COMPOSITE";
        this.refreshIntervalMinutes = 30;
        this.preferredCountry = "us";
        this.lastRefreshedAt = null;
    }

    public int getBodyFontSize() {
        return bodyFontSize;
    }

    public void setBodyFontSize(int bodyFontSize) {
        if (bodyFontSize < 12) {
            this.bodyFontSize = 12;
        } else if (bodyFontSize > 26) {
            this.bodyFontSize = 26;
        } else {
            this.bodyFontSize = bodyFontSize;
        }
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public Category getDefaultStartupCategory() {
        return defaultStartupCategory;
    }

    public void setDefaultStartupCategory(Category defaultStartupCategory) {
        this.defaultStartupCategory = (defaultStartupCategory != null) ? defaultStartupCategory : Category.HOME;
    }

    public String getPreferredNewsProvider() {
        return preferredNewsProvider;
    }

    public void setPreferredNewsProvider(String preferredNewsProvider) {
        this.preferredNewsProvider = preferredNewsProvider != null ? preferredNewsProvider : "COMPOSITE";
    }

    public int getRefreshIntervalMinutes() {
        return refreshIntervalMinutes;
    }

    public void setRefreshIntervalMinutes(int refreshIntervalMinutes) {
        this.refreshIntervalMinutes = Math.max(0, refreshIntervalMinutes);
    }

    public String getPreferredCountry() {
        return preferredCountry;
    }

    public void setPreferredCountry(String preferredCountry) {
        this.preferredCountry = preferredCountry != null ? preferredCountry : "us";
    }

    public LocalDateTime getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public void setLastRefreshedAt(LocalDateTime lastRefreshedAt) {
        this.lastRefreshedAt = lastRefreshedAt;
    }

    public void increaseFontSize() {
        setBodyFontSize(bodyFontSize + 2);
    }

    public void decreaseFontSize() {
        setBodyFontSize(bodyFontSize - 2);
    }
}
