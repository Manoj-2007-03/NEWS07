package com.newsroom.ui.components;

import com.newsroom.model.Category;
import com.newsroom.util.UIUtils;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A custom-painted editorial category pill badge.
 * Demonstrates Custom Component Painting, Encapsulation, and Polymorphism in paintComponent.
 */
public class CategoryBadge extends JComponent {
    private final Category category;
    private final boolean compact;

    public CategoryBadge(Category category) {
        this(category, false);
    }

    public CategoryBadge(Category category, boolean compact) {
        this.category = category != null ? category : Category.LATEST;
        this.compact = compact;
        setOpaque(false);
        updatePreferredSize();
    }

    private void updatePreferredSize() {
        Font font = UIUtils.getSansFont(Font.BOLD, compact ? 10 : 11);
        FontMetrics fm = getFontMetrics(font);
        int textWidth = fm.stringWidth(category.getDisplayName().toUpperCase());
        int h = compact ? 20 : 24;
        int w = textWidth + (compact ? 14 : 18);
        setPreferredSize(new Dimension(w, h));
        setMinimumSize(new Dimension(w, h));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        UIUtils.enableAntialiasing(g2);

        int w = getWidth();
        int h = getHeight();
        Color catColor = category.getThemeColor();

        // Background pill
        Color bg = new Color(catColor.getRed(), catColor.getGreen(), catColor.getBlue(), 38);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, h, h);

        // Subtle border
        Color border = new Color(catColor.getRed(), catColor.getGreen(), catColor.getBlue(), 90);
        g2.setColor(border);
        g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

        // Text
        Font font = UIUtils.getSansFont(Font.BOLD, compact ? 10 : 11);
        g2.setFont(font);
        g2.setColor(catColor);

        FontMetrics fm = g2.getFontMetrics();
        String text = category.getDisplayName().toUpperCase();
        int textX = (w - fm.stringWidth(text)) / 2;
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();

        g2.drawString(text, textX, textY);
        g2.dispose();
    }
}
