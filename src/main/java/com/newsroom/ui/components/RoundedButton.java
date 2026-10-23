package com.newsroom.ui.components;

import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.util.UIUtils;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom styled button with rounded corners, subtle hover feedback, and theme awareness.
 */
public class RoundedButton extends JButton {
    public enum Style {
        PRIMARY, SECONDARY, GHOST, DANGER
    }

    private final Style style;
    private final int radius;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public RoundedButton(String text, Style style) {
        this(text, style, 8);
    }

    public RoundedButton(String text, Style style, int radius) {
        super(text);
        this.style = style;
        this.radius = radius;
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(UIUtils.getSansFont(Font.BOLD, 12));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UIUtils.enableAntialiasing(g2);

        ThemeColors colors = ThemeManager.getInstance().getColors();
        int w = getWidth();
        int h = getHeight();

        Color bg;
        Color fg;
        Color border = null;

        switch (style) {
            case PRIMARY -> {
                bg = isPressed ? colors.accentHover : (isHovered ? colors.accentHover : colors.accent);
                fg = Color.WHITE;
            }
            case SECONDARY -> {
                bg = isPressed ? colors.borderHover : (isHovered ? colors.inputBackground : colors.surface);
                fg = colors.textPrimary;
                border = isHovered ? colors.accent : colors.border;
            }
            case GHOST -> {
                bg = isPressed ? colors.border : (isHovered ? colors.inputBackground : new Color(0, 0, 0, 0));
                fg = colors.textPrimary;
            }
            case DANGER -> {
                bg = isPressed ? new Color(153, 27, 27) : (isHovered ? new Color(185, 28, 28) : new Color(220, 38, 38));
                fg = Color.WHITE;
            }
            default -> {
                bg = colors.surface;
                fg = colors.textPrimary;
            }
        }

        // Draw background
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, radius, radius);

        // Draw border if applicable
        if (border != null) {
            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
        }

        // Draw text
        g2.setColor(fg);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textX = (w - fm.stringWidth(getText())) / 2;
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}
