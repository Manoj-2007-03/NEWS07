package com.newsroom.model;

import java.awt.Color;

/**
 * Enumeration representing editorial news categories within the application.
 * Demonstrates encapsulation with private attributes, custom methods, and rich domain modeling.
 */
public enum Category {
    HOME("Home", "Top stories & curated editorial selection", new Color(30, 41, 59)),
    LATEST("Latest News", "Chronological updates across all global desks", new Color(15, 118, 110)),
    WORLD("World", "International affairs, diplomacy, and global developments", new Color(37, 99, 235)),
    INDIA("India", "National governance, industry, culture, and progress", new Color(217, 119, 6)),
    TECHNOLOGY("Technology", "Artificial intelligence, software, hardware, and computing", new Color(99, 102, 241)),
    BUSINESS("Business", "Global markets, finance, trade, and economic policy", new Color(16, 185, 129)),
    SCIENCE("Science", "Space exploration, genetics, climate science, and discoveries", new Color(14, 165, 233)),
    SPORTS("Sports", "Championships, athletics, tennis, cricket, and global games", new Color(234, 88, 12)),
    ENTERTAINMENT("Entertainment", "Cinema, arts, literature, culture, and music", new Color(168, 85, 247));

    private final String displayName;
    private final String description;
    private final Color themeColor;

    Category(String displayName, String description, Color themeColor) {
        this.displayName = displayName;
        this.description = description;
        this.themeColor = themeColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Color getThemeColor() {
        return themeColor;
    }

    /**
     * Case-insensitive lookup for matching category names.
     */
    public static Category fromDisplayName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return HOME;
        }
        for (Category cat : values()) {
            if (cat.displayName.equalsIgnoreCase(name.trim()) || cat.name().equalsIgnoreCase(name.trim())) {
                return cat;
            }
        }
        return HOME;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
