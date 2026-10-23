package com.newsroom.ui.components;

import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.network.AsyncImageLoader;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.util.UIUtils;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * Editorial Cover Component supporting asynchronous remote image rendering with
 * procedural gradient fallback.
 * 
 * Ensures the application never displays broken image icons or missing asset glitches.
 * Demonstrates Custom Painting, Asynchronous Image Rendering, and Graceful Degradation.
 */
public class ArticlePlaceholderImage extends JComponent {
    private final Article article;
    private final int preferredHeight;
    private final boolean showTitle;
    private Image loadedRemoteImage;
    private boolean imageRequested = false;

    public ArticlePlaceholderImage(Article article, int preferredHeight) {
        this(article, preferredHeight, false);
    }

    public ArticlePlaceholderImage(Article article, int preferredHeight, boolean showTitle) {
        this.article = article;
        this.preferredHeight = preferredHeight;
        this.showTitle = showTitle;
        setPreferredSize(new Dimension(300, preferredHeight));
        setMinimumSize(new Dimension(100, preferredHeight));

        requestRemoteImage();
    }

    private void requestRemoteImage() {
        if (article != null && article.getImageUrl() != null && !article.getImageUrl().trim().isEmpty()) {
            imageRequested = true;
            AsyncImageLoader.getInstance().loadImage(article.getImageUrl(), icon -> {
                if (icon != null) {
                    this.loadedRemoteImage = icon.getImage();
                    repaint();
                }
            });
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        UIUtils.enableAntialiasing(g2);

        int w = getWidth();
        int h = getHeight();
        ThemeColors colors = ThemeManager.getInstance().getColors();

        if (!imageRequested && article != null && article.getImageUrl() != null && !article.getImageUrl().trim().isEmpty()) {
            requestRemoteImage();
        }

        Shape originalClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));

        // 1. If remote image is loaded, render it smoothly with center-cover crop
        if (loadedRemoteImage != null) {
            int imgW = loadedRemoteImage.getWidth(this);
            int imgH = loadedRemoteImage.getHeight(this);
            if (imgW > 0 && imgH > 0) {
                double scale = Math.max((double) w / imgW, (double) h / imgH);
                int drawW = (int) Math.round(imgW * scale);
                int drawH = (int) Math.round(imgH * scale);
                int x = (w - drawW) / 2;
                int y = (h - drawH) / 2;
                g2.drawImage(loadedRemoteImage, x, y, drawW, drawH, this);
            } else {
                g2.drawImage(loadedRemoteImage, 0, 0, w, h, this);
            }

            // Subtle dark overlay gradient for readability if showTitle is requested
            if (showTitle && article != null) {
                GradientPaint bottomFade = new GradientPaint(0, h - 70, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 210));
                g2.setPaint(bottomFade);
                g2.fillRect(0, h - 70, w, 70);

                g2.setColor(Color.WHITE);
                g2.setFont(UIUtils.getSansFont(Font.BOLD, 12));
                g2.drawString(article.getSource() + " \u2022 " + article.getReadingTimeMinutes() + " min read", 16, h - 16);
            }

            g2.setClip(originalClip);
            g2.setColor(colors.border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
            g2.dispose();
            return;
        }

        // 2. Procedural Editorial Artwork Fallback
        Category cat = article != null ? article.getCategory() : Category.LATEST;
        Color base = cat.getThemeColor();

        // Elegant editorial gradient
        Color c1 = UIUtils.adjustBrightness(base, 0.45f);
        Color c2 = UIUtils.adjustBrightness(base, 0.15f);
        GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        // Subtle geometric abstract grid & circles
        g2.setColor(new Color(255, 255, 255, 18));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(w - (int)(h * 0.8), -h / 4, (int)(h * 1.2), (int)(h * 1.2));
        g2.drawOval(-h / 3, h / 3, (int)(h * 0.9), (int)(h * 0.9));

        for (int x = 20; x < w; x += 40) {
            g2.drawLine(x, 0, x, h);
        }

        // Editorial category watermark
        g2.setColor(new Color(255, 255, 255, 140));
        g2.setFont(UIUtils.getSansFont(Font.BOLD, 11));
        g2.drawString("NEWSROOM JOURNAL \u2022 " + cat.getDisplayName().toUpperCase(), 16, 24);

        // Centered monogram
        String monogram = cat.getDisplayName().toUpperCase();
        if (monogram.length() > 4) {
            monogram = monogram.substring(0, 4);
        }
        g2.setColor(new Color(255, 255, 255, 45));
        g2.setFont(UIUtils.getEditorialFont(Font.BOLD, Math.min(h / 2, 64)));
        FontMetrics fm = g2.getFontMetrics();
        int monoX = (w - fm.stringWidth(monogram)) / 2;
        int monoY = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(monogram, monoX, monoY);

        if (showTitle && article != null) {
            GradientPaint bottomFade = new GradientPaint(0, h - 60, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 180));
            g2.setPaint(bottomFade);
            g2.fillRect(0, h - 60, w, 60);

            g2.setColor(Color.WHITE);
            g2.setFont(UIUtils.getSansFont(Font.BOLD, 13));
            g2.drawString(article.getSource() + " \u2022 " + article.getReadingTimeMinutes() + " min read", 16, h - 16);
        }

        g2.setClip(originalClip);
        g2.setColor(colors.border);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

        g2.dispose();
    }
}
