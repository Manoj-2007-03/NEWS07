package com.newsroom.ui;

import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.interfaces.NewsService;
import com.newsroom.interfaces.ReadingHistoryService;
import com.newsroom.interfaces.ThemeChangeListener;
import com.newsroom.model.ReaderSettings;
import com.newsroom.theme.AppTheme;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.util.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Abstract base class for all primary view panels within the application.
 * 
 * Demonstrates:
 * - Inheritance: Subclasses inherit common services, theme lifecycle, and layout helpers.
 * - Abstraction: Exposes contract methods (refreshView, onThemeUpdated).
 * - Polymorphism: Each view panel overrides view lifecycle and theme behaviors uniquely.
 */
public abstract class BaseViewPanel extends JPanel implements ThemeChangeListener {
    protected final NewsService newsService;
    protected final BookmarkService bookmarkService;
    protected final ReadingHistoryService historyService;
    protected final ArticleNavigationListener navListener;
    protected final ReaderSettings settings;

    public BaseViewPanel(NewsService newsService,
                         BookmarkService bookmarkService,
                         ReadingHistoryService historyService,
                         ArticleNavigationListener navListener,
                         ReaderSettings settings) {
        this.newsService = newsService;
        this.bookmarkService = bookmarkService;
        this.historyService = historyService;
        this.navListener = navListener;
        this.settings = settings;

        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(ThemeManager.getInstance().getColors().background);

        ThemeManager.getInstance().addListener(this);
    }

    /**
     * Called whenever this view is displayed or when its underlying dataset requires refresh.
     */
    public abstract void refreshView();

    /**
     * Hook method invoked when the global theme changes.
     */
    protected abstract void onThemeUpdated(ThemeColors colors);

    @Override
    public void onThemeChanged(AppTheme theme) {
        ThemeColors colors = ThemeManager.getInstance().getColors();
        setBackground(colors.background);
        onThemeUpdated(colors);
        revalidate();
        repaint();
    }

    /**
     * Helper to construct a standard editorial page header.
     */
    protected JPanel createHeader(String title, String subtitle) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(20, 28, 16, 28));

        ThemeColors colors = ThemeManager.getInstance().getColors();

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIUtils.getEditorialFont(Font.BOLD, 26));
        titleLabel.setForeground(colors.textPrimary);
        header.add(titleLabel);

        if (subtitle != null && !subtitle.isEmpty()) {
            header.add(Box.createVerticalStrut(4));
            JLabel subLabel = new JLabel(subtitle);
            subLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 13));
            subLabel.setForeground(colors.textSecondary);
            header.add(subLabel);
        }

        return header;
    }

    /**
     * Helper to wrap main content in a smooth, styled JScrollPane.
     */
    protected JScrollPane wrapInScrollPane(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(18);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        return scrollPane;
    }
}
