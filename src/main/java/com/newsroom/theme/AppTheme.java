package com.newsroom.theme;

/**
 * Enumeration representing available visual themes.
 */
public enum AppTheme {
    LIGHT("Editorial Light", "Crisp paper aesthetic inspired by broadsheet journals"),
    DARK("Midnight Edition", "Deep slate surface designed for low-light reading");

    private final String title;
    private final String description;

    AppTheme(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
