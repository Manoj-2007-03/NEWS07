package com.newsroom.ui;

import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkChangeListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.interfaces.ThemeChangeListener;
import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.theme.AppTheme;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.util.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Editorial Sidebar Navigation Panel.
 * Demonstrates Observer Pattern, Event Handling, and Custom UI Component design.
 */
public class SidebarPanel extends JPanel implements ThemeChangeListener, BookmarkChangeListener {
    public enum NavItem {
        HOME("Home", "⌂"),
        LATEST("Latest News", "⚡"),
        WORLD("World", "🌐"),
        INDIA("India", "🇮🇳"),
        TECHNOLOGY("Technology", "💻"),
        BUSINESS("Business", "📈"),
        SCIENCE("Science", "🔬"),
        SPORTS("Sports", "⚽"),
        ENTERTAINMENT("Entertainment", "🎭"),
        SAVED("Saved Articles", "★"),
        HISTORY("Reading History", "◷"),
        SETTINGS("Settings", "⚙");

        private final String label;
        private final String icon;

        NavItem(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String getLabel() { return label; }
        public String getIcon() { return icon; }
    }

    private final ArticleNavigationListener navListener;
    private final BookmarkService bookmarkService;
    private NavItem activeItem = NavItem.HOME;
    private final List<SidebarButton> buttons = new ArrayList<>();
    private JLabel savedBadgeLabel;

    public SidebarPanel(ArticleNavigationListener navListener, BookmarkService bookmarkService) {
        this.navListener = navListener;
        this.bookmarkService = bookmarkService;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(240, 800));
        setMinimumSize(new Dimension(200, 600));

        ThemeManager.getInstance().addListener(this);
        if (bookmarkService != null) {
            bookmarkService.addBookmarkListener(this);
        }

        initUI();
    }

    private void initUI() {
        ThemeColors colors = ThemeManager.getInstance().getColors();
        setBackground(colors.sidebarBackground);

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(20, 14, 20, 14));

        // 1. Logo & Masthead
        main.add(createLogoSection());
        main.add(Box.createVerticalStrut(24));

        // Section label: FEEDS
        main.add(createSectionHeader("EDITORIAL FEEDS"));
        main.add(Box.createVerticalStrut(6));

        // Main Feeds
        addNavButton(main, NavItem.HOME);
        addNavButton(main, NavItem.LATEST);
        main.add(Box.createVerticalStrut(14));

        // Section label: DESKS
        main.add(createSectionHeader("GLOBAL DESKS"));
        main.add(Box.createVerticalStrut(6));

        addNavButton(main, NavItem.WORLD);
        addNavButton(main, NavItem.INDIA);
        addNavButton(main, NavItem.TECHNOLOGY);
        addNavButton(main, NavItem.BUSINESS);
        addNavButton(main, NavItem.SCIENCE);
        addNavButton(main, NavItem.SPORTS);
        addNavButton(main, NavItem.ENTERTAINMENT);
        main.add(Box.createVerticalStrut(14));

        // Section label: LIBRARY
        main.add(createSectionHeader("PERSONAL LIBRARY"));
        main.add(Box.createVerticalStrut(6));

        addNavButton(main, NavItem.SAVED);
        addNavButton(main, NavItem.HISTORY);

        main.add(Box.createVerticalGlue());

        // Settings at bottom
        addNavButton(main, NavItem.SETTINGS);

        add(main, BorderLayout.CENTER);
    }

    private JPanel createLogoSection() {
        JPanel logo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logo.setOpaque(false);

        // Monogram square
        JPanel monogram = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                UIUtils.enableAntialiasing(g2);
                ThemeColors colors = ThemeManager.getInstance().getColors();
                g2.setColor(colors.accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(UIUtils.getEditorialFont(Font.BOLD, 18));
                g2.drawString("N", 9, 23);
                g2.dispose();
            }
        };
        monogram.setPreferredSize(new Dimension(32, 32));
        monogram.setOpaque(false);
        logo.add(monogram);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("NEWSROOM");
        title.setFont(UIUtils.getEditorialFont(Font.BOLD, 18));
        title.setForeground(ThemeManager.getInstance().getColors().sidebarText);
        text.add(title);

        JLabel edition = new JLabel("Desktop Reader v1.0");
        edition.setFont(UIUtils.getSansFont(Font.PLAIN, 10));
        edition.setForeground(ThemeManager.getInstance().getColors().sidebarTextMuted);
        text.add(edition);

        logo.add(text);
        return logo;
    }

    private JLabel createSectionHeader(String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(UIUtils.getSansFont(Font.BOLD, 10));
        lbl.setForeground(ThemeManager.getInstance().getColors().sidebarTextMuted);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        return lbl;
    }

    private void addNavButton(JPanel container, NavItem item) {
        SidebarButton btn = new SidebarButton(item);
        buttons.add(btn);
        container.add(btn);
        container.add(Box.createVerticalStrut(2));
    }

    public void setActiveItem(NavItem item) {
        this.activeItem = item;
        for (SidebarButton btn : buttons) {
            btn.repaint();
        }
    }

    public NavItem getActiveItem() {
        return activeItem;
    }

    @Override
    public void onBookmarkChanged(Article article, boolean isBookmarked) {
        if (savedBadgeLabel != null && bookmarkService != null) {
            savedBadgeLabel.setText(String.valueOf(bookmarkService.getBookmarkCount()));
            savedBadgeLabel.setVisible(bookmarkService.getBookmarkCount() > 0);
        }
        repaint();
    }

    @Override
    public void onThemeChanged(AppTheme theme) {
        setBackground(ThemeManager.getInstance().getColors().sidebarBackground);
        repaint();
    }

    /**
     * Inner custom styled button component for sidebar navigation items.
     */
    private class SidebarButton extends JPanel {
        private final NavItem item;
        private boolean isHovered = false;

        public SidebarButton(NavItem item) {
            this.item = item;
            setLayout(new BorderLayout(8, 0));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(212, 36));
            setMaximumSize(new Dimension(212, 36));
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

            // Left icon & label
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            left.setOpaque(false);

            JLabel icon = new JLabel(item.getIcon());
            icon.setFont(UIUtils.getSansFont(Font.PLAIN, 13));
            icon.setForeground(ThemeManager.getInstance().getColors().sidebarTextMuted);
            left.add(icon);

            JLabel label = new JLabel(item.getLabel());
            label.setFont(UIUtils.getSansFont(Font.PLAIN, 13));
            label.setForeground(ThemeManager.getInstance().getColors().sidebarText);
            left.add(label);

            add(left, BorderLayout.WEST);

            // Special badge for Saved Articles
            if (item == NavItem.SAVED) {
                int count = bookmarkService != null ? bookmarkService.getBookmarkCount() : 0;
                savedBadgeLabel = new JLabel(String.valueOf(count));
                savedBadgeLabel.setFont(UIUtils.getSansFont(Font.BOLD, 10));
                savedBadgeLabel.setForeground(Color.WHITE);
                savedBadgeLabel.setOpaque(true);
                savedBadgeLabel.setBackground(ThemeManager.getInstance().getColors().accent);
                savedBadgeLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                savedBadgeLabel.setVisible(count > 0);
                add(savedBadgeLabel, BorderLayout.EAST);
            }

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
                    setActiveItem(item);
                    handleNavigation(item);
                }
            });
        }

        private void handleNavigation(NavItem item) {
            if (navListener == null) return;
            switch (item) {
                case HOME -> navListener.onNavigateToHome();
                case LATEST -> navListener.onCategorySelected(Category.LATEST);
                case WORLD -> navListener.onCategorySelected(Category.WORLD);
                case INDIA -> navListener.onCategorySelected(Category.INDIA);
                case TECHNOLOGY -> navListener.onCategorySelected(Category.TECHNOLOGY);
                case BUSINESS -> navListener.onCategorySelected(Category.BUSINESS);
                case SCIENCE -> navListener.onCategorySelected(Category.SCIENCE);
                case SPORTS -> navListener.onCategorySelected(Category.SPORTS);
                case ENTERTAINMENT -> navListener.onCategorySelected(Category.ENTERTAINMENT);
                case SAVED -> navListener.onNavigateToSaved();
                case HISTORY -> navListener.onNavigateToHistory();
                case SETTINGS -> navListener.onNavigateToSettings();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UIUtils.enableAntialiasing(g2);

            ThemeColors colors = ThemeManager.getInstance().getColors();
            boolean isActive = (activeItem == item);

            int w = getWidth();
            int h = getHeight();

            if (isActive) {
                // Active pill background
                g2.setColor(colors.sidebarItemActive);
                g2.fillRoundRect(0, 0, w, h, 8, 8);
            } else if (isHovered) {
                // Hover pill
                g2.setColor(colors.sidebarItemHover);
                g2.fillRoundRect(0, 0, w, h, 8, 8);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
