package com.newsroom.theme;

import java.awt.Color;

/**
 * Immutable palette containing colors for both Light and Dark editorial themes.
 * Demonstrates Encapsulation and record/class modeling.
 */
public class ThemeColors {
    public final Color background;
    public final Color surface;
    public final Color surfaceElevated;
    public final Color sidebarBackground;
    public final Color sidebarItemHover;
    public final Color sidebarItemActive;
    public final Color sidebarText;
    public final Color sidebarTextMuted;
    public final Color headerBackground;
    public final Color textPrimary;
    public final Color textSecondary;
    public final Color textMuted;
    public final Color border;
    public final Color borderHover;
    public final Color accent;
    public final Color accentHover;
    public final Color accentSecondary;
    public final Color inputBackground;
    public final Color inputBorder;
    public final Color inputBorderFocused;
    public final Color tagBackground;
    public final Color tagText;
    public final Color bookmarkActive;

    public ThemeColors(Color background, Color surface, Color surfaceElevated,
                       Color sidebarBackground, Color sidebarItemHover, Color sidebarItemActive,
                       Color sidebarText, Color sidebarTextMuted,
                       Color headerBackground, Color textPrimary, Color textSecondary,
                       Color textMuted, Color border, Color borderHover,
                       Color accent, Color accentHover, Color accentSecondary,
                       Color inputBackground, Color inputBorder, Color inputBorderFocused,
                       Color tagBackground, Color tagText, Color bookmarkActive) {
        this.background = background;
        this.surface = surface;
        this.surfaceElevated = surfaceElevated;
        this.sidebarBackground = sidebarBackground;
        this.sidebarItemHover = sidebarItemHover;
        this.sidebarItemActive = sidebarItemActive;
        this.sidebarText = sidebarText;
        this.sidebarTextMuted = sidebarTextMuted;
        this.headerBackground = headerBackground;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.textMuted = textMuted;
        this.border = border;
        this.borderHover = borderHover;
        this.accent = accent;
        this.accentHover = accentHover;
        this.accentSecondary = accentSecondary;
        this.inputBackground = inputBackground;
        this.inputBorder = inputBorder;
        this.inputBorderFocused = inputBorderFocused;
        this.tagBackground = tagBackground;
        this.tagText = tagText;
        this.bookmarkActive = bookmarkActive;
    }

    public static ThemeColors getLight() {
        return new ThemeColors(
                new Color(248, 248, 246), // background (soft warm broadsheet paper)
                new Color(255, 255, 255), // surface (crisp white card)
                new Color(250, 250, 250), // surfaceElevated
                new Color(15, 23, 42),    // sidebarBackground (deep navy obsidian)
                new Color(30, 41, 59),    // sidebarItemHover
                new Color(185, 28, 28),   // sidebarItemActive (editorial crimson badge/accent)
                new Color(248, 250, 252), // sidebarText
                new Color(148, 163, 184), // sidebarTextMuted
                new Color(255, 255, 255), // headerBackground
                new Color(15, 23, 42),    // textPrimary
                new Color(71, 85, 105),   // textSecondary
                new Color(148, 163, 184), // textMuted
                new Color(226, 232, 240), // border
                new Color(203, 213, 225), // borderHover
                new Color(185, 28, 28),   // accent (editorial crimson)
                new Color(153, 27, 27),   // accentHover
                new Color(37, 99, 235),   // accentSecondary (electric royal blue)
                new Color(241, 245, 249), // inputBackground
                new Color(203, 213, 225), // inputBorder
                new Color(185, 28, 28),   // inputBorderFocused
                new Color(241, 245, 249), // tagBackground
                new Color(51, 65, 85),    // tagText
                new Color(217, 119, 6)    // bookmarkActive (warm amber/gold)
        );
    }

    public static ThemeColors getDark() {
        return new ThemeColors(
                new Color(11, 15, 25),    // background (midnight obsidian)
                new Color(19, 27, 46),    // surface (deep midnight card)
                new Color(26, 36, 60),    // surfaceElevated
                new Color(8, 11, 19),     // sidebarBackground
                new Color(23, 32, 54),    // sidebarItemHover
                new Color(220, 38, 38),   // sidebarItemActive
                new Color(248, 250, 252), // sidebarText
                new Color(148, 163, 184), // sidebarTextMuted
                new Color(19, 27, 46),    // headerBackground
                new Color(248, 250, 252), // textPrimary
                new Color(148, 163, 184), // textSecondary
                new Color(100, 116, 139), // textMuted
                new Color(30, 41, 59),    // border
                new Color(51, 65, 85),    // borderHover
                new Color(239, 68, 68),   // accent
                new Color(220, 38, 38),   // accentHover
                new Color(59, 130, 246),  // accentSecondary
                new Color(30, 41, 59),    // inputBackground
                new Color(51, 65, 85),    // inputBorder
                new Color(239, 68, 68),   // inputBorderFocused
                new Color(30, 41, 59),    // tagBackground
                new Color(226, 232, 240), // tagText
                new Color(245, 158, 11)   // bookmarkActive
        );
    }
}
