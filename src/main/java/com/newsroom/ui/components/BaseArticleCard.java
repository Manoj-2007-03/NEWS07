package com.newsroom.ui.components;

import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkChangeListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.interfaces.ThemeChangeListener;
import com.newsroom.model.Article;
import com.newsroom.theme.AppTheme;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.util.UIUtils;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Abstract base class for news article cards.
 * Demonstrates:
 * - Inheritance: Subclasses inherit card mechanics, hover states, and theme updates.
 * - Abstraction: Encapsulates boilerplate event handling and theme registration.
 * - Polymorphism: Concrete cards override layout initialization and custom rendering.
 * - Template Method: Defines standard lifecycle for card creation.
 */
public abstract class BaseArticleCard extends JPanel implements ThemeChangeListener, BookmarkChangeListener {
    protected final Article article;
    protected final ArticleNavigationListener navListener;
    protected final BookmarkService bookmarkService;

    protected boolean isHovered = false;
    protected int cornerRadius = 10;

    public BaseArticleCard(Article article, ArticleNavigationListener navListener, BookmarkService bookmarkService) {
        this.article = article;
        this.navListener = navListener;
        this.bookmarkService = bookmarkService;

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover & click listeners
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (navListener != null && article != null) {
                    navListener.onArticleSelected(article);
                }
            }
        });

        // Register for theme and bookmark observer updates
        ThemeManager.getInstance().addListener(this);
        if (bookmarkService != null) {
            bookmarkService.addBookmarkListener(this);
        }

        initCardLayout();
    }

    /**
     * Subclasses implement their custom component layout and visual hierarchy.
     */
    protected abstract void initCardLayout();

    /**
     * Hook method invoked when theme changes occur.
     */
    protected abstract void onThemeUpdated(ThemeColors colors);

    @Override
    public void onThemeChanged(AppTheme theme) {
        onThemeUpdated(ThemeManager.getInstance().getColors());
        repaint();
    }

    @Override
    public void onBookmarkChanged(Article changedArticle, boolean isBookmarked) {
        if (article != null && changedArticle != null && article.getId().equals(changedArticle.getId())) {
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UIUtils.enableAntialiasing(g2);

        ThemeColors colors = ThemeManager.getInstance().getColors();
        int w = getWidth();
        int h = getHeight();

        // Card surface fill
        g2.setColor(colors.surface);
        g2.fillRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

        // Card border (highlighted when hovered)
        Color border = isHovered ? colors.borderHover : colors.border;
        g2.setColor(border);
        g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);
    }

    public Article getArticle() {
        return article;
    }
}
