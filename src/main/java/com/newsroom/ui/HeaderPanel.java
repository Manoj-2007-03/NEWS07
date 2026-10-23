package com.newsroom.ui;

import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.RefreshListener;
import com.newsroom.interfaces.ThemeChangeListener;
import com.newsroom.model.Category;
import com.newsroom.model.ReaderSettings;
import com.newsroom.service.DatabaseNewsServiceImpl;
import com.newsroom.theme.AppTheme;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.ui.components.RoundedButton;
import com.newsroom.util.DateTimeFormatterUtil;
import com.newsroom.util.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;

/**
 * Editorial Top Header Panel.
 * Contains breadcrumbs, active search bar, formatted date, live refresh controls,
 * offline status indicator, and theme toggling.
 * Demonstrates Component Composition and Observer integration.
 */
public class HeaderPanel extends JPanel implements ThemeChangeListener, RefreshListener {
    private final ArticleNavigationListener navListener;
    private final ReaderSettings settings;
    private final DatabaseNewsServiceImpl newsService;

    private JLabel breadcrumbLabel;
    private JTextField searchField;
    private RoundedButton clearBtn;
    private JLabel statusBadge;
    private RoundedButton refreshBtn;
    private RoundedButton themeToggleBtn;

    public HeaderPanel(ArticleNavigationListener navListener, ReaderSettings settings, DatabaseNewsServiceImpl newsService) {
        this.navListener = navListener;
        this.settings = settings;
        this.newsService = newsService;

        setLayout(new BorderLayout(16, 0));
        setPreferredSize(new Dimension(1000, 58));
        setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));

        ThemeManager.getInstance().addListener(this);
        if (newsService != null) {
            newsService.addRefreshListener(this);
        }

        initUI();
    }

    private void initUI() {
        ThemeColors colors = ThemeManager.getInstance().getColors();
        setBackground(colors.headerBackground);

        // 1. Left: Breadcrumb / Page Title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        left.setOpaque(false);

        breadcrumbLabel = new JLabel("Home");
        breadcrumbLabel.setFont(UIUtils.getSansFont(Font.BOLD, 15));
        breadcrumbLabel.setForeground(colors.textPrimary);
        left.add(breadcrumbLabel);

        add(left, BorderLayout.WEST);

        // 2. Center: Search Bar
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        center.setOpaque(false);

        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setOpaque(false);
        searchBox.setPreferredSize(new Dimension(360, 34));

        JLabel searchIcon = new JLabel(" ⌕ ");
        searchIcon.setFont(UIUtils.getSansFont(Font.BOLD, 14));
        searchIcon.setForeground(colors.textMuted);
        searchBox.add(searchIcon, BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        searchField.setBackground(colors.inputBackground);
        searchField.setForeground(colors.textPrimary);
        searchField.setCaretColor(colors.textPrimary);
        searchField.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        searchField.setToolTipText("Search live and archived stories across desks (Press Enter)");

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    triggerSearch();
                }
            }
        });
        searchBox.add(searchField, BorderLayout.CENTER);

        clearBtn = new RoundedButton("✕", RoundedButton.Style.GHOST, 4);
        clearBtn.setPreferredSize(new Dimension(28, 28));
        clearBtn.setToolTipText("Clear search query");
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            if (navListener != null) {
                navListener.onNavigateToSearch("");
            }
        });
        searchBox.add(clearBtn, BorderLayout.EAST);

        // Box border
        searchBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colors.inputBorder, 1, true),
                BorderFactory.createEmptyBorder(0, 6, 0, 6)
        ));
        center.add(searchBox);

        add(center, BorderLayout.CENTER);

        // 3. Right: Refresh + Status Badge + Theme Toggle
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        right.setOpaque(false);

        // Status Badge (Live vs Offline / Last updated)
        statusBadge = new JLabel("● Live Feeds");
        statusBadge.setFont(UIUtils.getSansFont(Font.BOLD, 11));
        statusBadge.setForeground(new Color(22, 163, 74));
        right.add(statusBadge);

        // Manual Refresh Button
        refreshBtn = new RoundedButton("⟳ Refresh", RoundedButton.Style.SECONDARY, 6);
        refreshBtn.setPreferredSize(new Dimension(92, 30));
        refreshBtn.setToolTipText("Synchronize real-time news from configured providers");
        refreshBtn.addActionListener(e -> {
            if (newsService != null) {
                refreshBtn.setEnabled(false);
                refreshBtn.setText("⟳ Fetching...");
                newsService.refreshNews(Category.HOME);
            }
        });
        right.add(refreshBtn);

        // Theme toggle button
        boolean isDark = ThemeManager.getInstance().isDarkMode();
        themeToggleBtn = new RoundedButton(isDark ? "☀ Light" : "☾ Dark", RoundedButton.Style.SECONDARY, 6);
        themeToggleBtn.setPreferredSize(new Dimension(86, 30));
        themeToggleBtn.setToolTipText("Toggle between Light and Dark editorial theme");
        themeToggleBtn.addActionListener(e -> {
            ThemeManager.getInstance().toggleTheme();
            boolean darkNow = ThemeManager.getInstance().isDarkMode();
            settings.setDarkMode(darkNow);
            themeToggleBtn.setText(darkNow ? "☀ Light" : "☾ Dark");
        });
        right.add(themeToggleBtn);

        add(right, BorderLayout.EAST);
    }

    private void triggerSearch() {
        String q = searchField.getText().trim();
        if (navListener != null) {
            navListener.onNavigateToSearch(q);
        }
    }

    public void setBreadcrumb(String text) {
        if (breadcrumbLabel != null) {
            breadcrumbLabel.setText(text);
        }
    }

    public void setSearchFieldText(String text) {
        if (searchField != null) {
            searchField.setText(text != null ? text : "");
        }
    }

    @Override
    public void onRefreshStarted() {
        if (refreshBtn != null) {
            refreshBtn.setEnabled(false);
            refreshBtn.setText("⟳ Fetching...");
        }
        if (statusBadge != null) {
            statusBadge.setText("● Syncing Live...");
            statusBadge.setForeground(new Color(217, 119, 6)); // Amber
        }
    }

    @Override
    public void onRefreshCompleted(boolean success, int newArticlesCount, String statusMessage) {
        if (refreshBtn != null) {
            refreshBtn.setEnabled(true);
            refreshBtn.setText("⟳ Refresh");
        }
        if (statusBadge != null) {
            if (success) {
                String timeStr = DateTimeFormatter.ofPattern("h:mm a").format(java.time.LocalTime.now());
                statusBadge.setText("● Updated " + timeStr);
                statusBadge.setForeground(new Color(22, 163, 74)); // Green
            } else {
                statusBadge.setText("▲ Offline • Using Cache");
                statusBadge.setForeground(new Color(220, 38, 38)); // Red
            }
            statusBadge.setToolTipText(statusMessage);
        }
    }

    @Override
    public void onThemeChanged(AppTheme theme) {
        ThemeColors colors = ThemeManager.getInstance().getColors();
        setBackground(colors.headerBackground);

        if (breadcrumbLabel != null) breadcrumbLabel.setForeground(colors.textPrimary);
        if (searchField != null) {
            searchField.setBackground(colors.inputBackground);
            searchField.setForeground(colors.textPrimary);
            searchField.setCaretColor(colors.textPrimary);
        }
        if (themeToggleBtn != null) {
            themeToggleBtn.setText(ThemeManager.getInstance().isDarkMode() ? "☀ Light" : "☾ Dark");
        }

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, colors.border),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)
        ));

        revalidate();
        repaint();
    }
}
