package com.newsroom.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 * Common graphical and typographic utilities for the Swing editorial interface.
 */
public final class UIUtils {
    public static final String FONT_SERIF_NAME = "Georgia";
    public static final String FONT_SANS_NAME = "Segoe UI";

    private UIUtils() {
        // Prevent instantiation
    }

    /**
     * Enables high-quality antialiasing and subpixel text rendering on a Graphics2D context.
     */
    public static void enableAntialiasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /**
     * Creates an editorial serif font for headlines and article titles.
     */
    public static Font getEditorialFont(int style, int size) {
        return new Font(FONT_SERIF_NAME, style, size);
    }

    /**
     * Creates a clean modern sans-serif font for UI elements, labels, and badges.
     */
    public static Font getSansFont(int style, int size) {
        return new Font(FONT_SANS_NAME, style, size);
    }

    /**
     * Creates an article body font with reader-selected size.
     */
    public static Font getArticleBodyFont(int size) {
        return new Font(FONT_SERIF_NAME, Font.PLAIN, size);
    }

    /**
     * Creates a consistent rounded border.
     */
    public static Border createRoundedBorder(Color color, int radius) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        );
    }

    /**
     * Lightens or darkens a color by a factor.
     */
    public static Color adjustBrightness(Color c, float factor) {
        int r = Math.min(255, Math.max(0, (int) (c.getRed() * factor)));
        int g = Math.min(255, Math.max(0, (int) (c.getGreen() * factor)));
        int b = Math.min(255, Math.max(0, (int) (c.getBlue() * factor)));
        return new Color(r, g, b, c.getAlpha());
    }
}
