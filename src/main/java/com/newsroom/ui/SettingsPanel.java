package com.newsroom.ui;

import com.newsroom.config.AppConfig;
import com.newsroom.database.DatabaseManager;
import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.interfaces.NewsService;
import com.newsroom.interfaces.ReadingHistoryService;
import com.newsroom.model.Category;
import com.newsroom.model.ReaderSettings;
import com.newsroom.repository.SqliteArticleRepository;
import com.newsroom.repository.SqliteSettingsRepository;
import com.newsroom.service.DatabaseNewsServiceImpl;
import com.newsroom.theme.AppTheme;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.ui.components.RoundedButton;
import com.newsroom.util.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.Statement;

/**
 * Editorial Application Settings Screen - Phase 2 Edition.
 * Demonstrates Encapsulation (ReaderSettings, AppConfig), Event Handling,
 * SQLite persistence, and credential security.
 */
public class SettingsPanel extends BaseViewPanel {
    private JPanel mainContent;
    private RoundedButton lightThemeBtn;
    private RoundedButton darkThemeBtn;
    private JComboBox<String> fontSizeCombo;
    private JComboBox<Category> defaultCategoryCombo;
    private JComboBox<String> providerCombo;
    private JComboBox<String> refreshIntervalCombo;
    private JPasswordField apiKeyField;

    public SettingsPanel(NewsService newsService,
                         BookmarkService bookmarkService,
                         ReadingHistoryService historyService,
                         ArticleNavigationListener navListener,
                         ReaderSettings settings) {
        super(newsService, bookmarkService, historyService, navListener, settings);
        initUI();
    }

    private void initUI() {
        mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 28, 36, 28));

        refreshView();
        add(wrapInScrollPane(mainContent), BorderLayout.CENTER);
    }

    @Override
    public void refreshView() {
        mainContent.removeAll();
        ThemeColors colors = ThemeManager.getInstance().getColors();

        // Header
        mainContent.add(createHeader("Application Settings", "Configure news syndication, SQLite persistence, and editorial typography."));
        mainContent.add(Box.createVerticalStrut(20));

        // Setting Section 1: Appearance & Theme
        mainContent.add(createThemeSection(colors));
        mainContent.add(Box.createVerticalStrut(20));

        // Setting Section 2: Real News Providers & API Key
        mainContent.add(createNewsProviderSection(colors));
        mainContent.add(Box.createVerticalStrut(20));

        // Setting Section 3: Reader Typography Preferences
        mainContent.add(createTypographySection(colors));
        mainContent.add(Box.createVerticalStrut(20));

        // Setting Section 4: Navigation & Default Startup
        mainContent.add(createStartupSection(colors));
        mainContent.add(Box.createVerticalStrut(20));

        // Setting Section 5: SQLite Database & Cache Storage
        mainContent.add(createDatabaseSection(colors));
        mainContent.add(Box.createVerticalStrut(20));

        // Setting Section 6: About & Technical Specifications
        mainContent.add(createAboutSection(colors));
        mainContent.add(Box.createVerticalStrut(24));

        // Setting Section 7: Reset Actions
        mainContent.add(createResetSection(colors));

        mainContent.revalidate();
        mainContent.repaint();
    }

    private JPanel createThemeSection(ThemeColors colors) {
        JPanel section = createCardSection(colors, "VISUAL THEME", "Switch between broadsheet paper light mode and deep midnight slate.");

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        buttonRow.setOpaque(false);

        boolean isDark = ThemeManager.getInstance().isDarkMode();

        lightThemeBtn = new RoundedButton("☀ Editorial Light", isDark ? RoundedButton.Style.SECONDARY : RoundedButton.Style.PRIMARY, 8);
        lightThemeBtn.setPreferredSize(new Dimension(160, 38));
        lightThemeBtn.addActionListener(e -> {
            ThemeManager.getInstance().setTheme(AppTheme.LIGHT);
            settings.setDarkMode(false);
            new SqliteSettingsRepository().persist(settings);
            refreshView();
        });
        buttonRow.add(lightThemeBtn);

        darkThemeBtn = new RoundedButton("☾ Midnight Dark", isDark ? RoundedButton.Style.PRIMARY : RoundedButton.Style.SECONDARY, 8);
        darkThemeBtn.setPreferredSize(new Dimension(160, 38));
        darkThemeBtn.addActionListener(e -> {
            ThemeManager.getInstance().setTheme(AppTheme.DARK);
            settings.setDarkMode(true);
            new SqliteSettingsRepository().persist(settings);
            refreshView();
        });
        buttonRow.add(darkThemeBtn);

        section.add(buttonRow);
        return section;
    }

    private JPanel createNewsProviderSection(ThemeColors colors) {
        JPanel section = createCardSection(colors, "REAL-TIME NEWS RETRIEVAL", "Configure live syndication providers, API keys, and auto-refresh intervals.");

        // 1. Provider Mode
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        row1.setOpaque(false);

        JLabel provLbl = new JLabel("Provider Engine:");
        provLbl.setFont(UIUtils.getSansFont(Font.BOLD, 12));
        provLbl.setForeground(colors.textPrimary);
        row1.add(provLbl);

        String[] providers = {"COMPOSITE (Recommended - Hybrid)", "RSS (Zero-Key Live Syndication)", "NEWS_API (NewsAPI.org)"};
        providerCombo = new JComboBox<>(providers);
        providerCombo.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        providerCombo.setPreferredSize(new Dimension(280, 30));

        String curStrategy = AppConfig.getInstance().getProviderStrategy();
        if ("RSS".equalsIgnoreCase(curStrategy)) providerCombo.setSelectedIndex(1);
        else if ("NEWS_API".equalsIgnoreCase(curStrategy)) providerCombo.setSelectedIndex(2);
        else providerCombo.setSelectedIndex(0);

        providerCombo.addActionListener(e -> {
            int idx = providerCombo.getSelectedIndex();
            String chosen = (idx == 1) ? "RSS" : (idx == 2 ? "NEWS_API" : "COMPOSITE");
            AppConfig.getInstance().setProviderStrategy(chosen);
            settings.setPreferredNewsProvider(chosen);
            new SqliteSettingsRepository().persist(settings);
        });
        row1.add(providerCombo);
        section.add(row1);

        // 2. API Key input (masked)
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        row2.setOpaque(false);

        JLabel keyLbl = new JLabel("NewsAPI.org Key:");
        keyLbl.setFont(UIUtils.getSansFont(Font.BOLD, 12));
        keyLbl.setForeground(colors.textPrimary);
        row2.add(keyLbl);

        apiKeyField = new JPasswordField(AppConfig.getInstance().getApiKey());
        apiKeyField.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        apiKeyField.setBackground(colors.inputBackground);
        apiKeyField.setForeground(colors.textPrimary);
        apiKeyField.setCaretColor(colors.textPrimary);
        apiKeyField.setPreferredSize(new Dimension(240, 30));
        apiKeyField.setToolTipText("Enter NewsAPI.org developer key (optional - RSS works out of the box)");
        row2.add(apiKeyField);

        RoundedButton saveKeyBtn = new RoundedButton("Save Key", RoundedButton.Style.SECONDARY, 6);
        saveKeyBtn.setPreferredSize(new Dimension(90, 30));
        saveKeyBtn.addActionListener(e -> {
            String entered = new String(apiKeyField.getPassword()).trim();
            AppConfig.getInstance().saveApiKey(entered);
            JOptionPane.showMessageDialog(this, "API configuration updated successfully!\nCurrent Status: "
                    + AppConfig.getInstance().getMaskedApiKey(), "Configuration Saved", JOptionPane.INFORMATION_MESSAGE);
            refreshView();
        });
        row2.add(saveKeyBtn);

        JLabel statusKeyLbl = new JLabel("Status: " + AppConfig.getInstance().getMaskedApiKey());
        statusKeyLbl.setFont(UIUtils.getSansFont(Font.ITALIC, 11));
        statusKeyLbl.setForeground(colors.textMuted);
        row2.add(statusKeyLbl);

        section.add(row2);

        // 3. Auto-Refresh Interval
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        row3.setOpaque(false);

        JLabel refreshLbl = new JLabel("Auto-Refresh Interval:");
        refreshLbl.setFont(UIUtils.getSansFont(Font.BOLD, 12));
        refreshLbl.setForeground(colors.textPrimary);
        row3.add(refreshLbl);

        String[] refreshIntervals = {"Manual Only (0 min)", "Every 15 Minutes", "Every 30 Minutes (Default)", "Every 60 Minutes"};
        refreshIntervalCombo = new JComboBox<>(refreshIntervals);
        refreshIntervalCombo.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        refreshIntervalCombo.setPreferredSize(new Dimension(220, 30));

        int curInterval = settings.getRefreshIntervalMinutes();
        if (curInterval == 0) refreshIntervalCombo.setSelectedIndex(0);
        else if (curInterval == 15) refreshIntervalCombo.setSelectedIndex(1);
        else if (curInterval == 60) refreshIntervalCombo.setSelectedIndex(3);
        else refreshIntervalCombo.setSelectedIndex(2);

        refreshIntervalCombo.addActionListener(e -> {
            int idx = refreshIntervalCombo.getSelectedIndex();
            int mins = switch (idx) {
                case 0 -> 0;
                case 1 -> 15;
                case 3 -> 60;
                default -> 30;
            };
            settings.setRefreshIntervalMinutes(mins);
            AppConfig.getInstance().setRefreshIntervalMinutes(mins);
            new SqliteSettingsRepository().persist(settings);
            if (newsService instanceof DatabaseNewsServiceImpl dbSvc) {
                dbSvc.configureAutoRefresh();
            }
        });
        row3.add(refreshIntervalCombo);

        section.add(row3);
        return section;
    }

    private JPanel createTypographySection(ThemeColors colors) {
        JPanel section = createCardSection(colors, "READER TYPOGRAPHY", "Default font scale for the article reading view.");

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        row.setOpaque(false);

        JLabel label = new JLabel("Default Body Size:");
        label.setFont(UIUtils.getSansFont(Font.PLAIN, 13));
        label.setForeground(colors.textPrimary);
        row.add(label);

        String[] sizeOptions = {"Small (14pt)", "Medium (16pt - Default)", "Large (18pt)", "Extra Large (21pt)"};
        fontSizeCombo = new JComboBox<>(sizeOptions);
        fontSizeCombo.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        fontSizeCombo.setPreferredSize(new Dimension(200, 32));

        int cur = settings.getBodyFontSize();
        if (cur <= 14) fontSizeCombo.setSelectedIndex(0);
        else if (cur <= 16) fontSizeCombo.setSelectedIndex(1);
        else if (cur <= 18) fontSizeCombo.setSelectedIndex(2);
        else fontSizeCombo.setSelectedIndex(3);

        fontSizeCombo.addActionListener(e -> {
            int idx = fontSizeCombo.getSelectedIndex();
            switch (idx) {
                case 0 -> settings.setBodyFontSize(ReaderSettings.FONT_SIZE_SMALL);
                case 1 -> settings.setBodyFontSize(ReaderSettings.FONT_SIZE_MEDIUM);
                case 2 -> settings.setBodyFontSize(ReaderSettings.FONT_SIZE_LARGE);
                case 3 -> settings.setBodyFontSize(ReaderSettings.FONT_SIZE_XLARGE);
            }
            new SqliteSettingsRepository().persist(settings);
        });
        row.add(fontSizeCombo);

        section.add(row);
        return section;
    }

    private JPanel createStartupSection(ThemeColors colors) {
        JPanel section = createCardSection(colors, "STARTUP EXPERIENCE", "Select default desk to land on when launching the reader.");

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        row.setOpaque(false);

        JLabel label = new JLabel("Default Startup Section:");
        label.setFont(UIUtils.getSansFont(Font.PLAIN, 13));
        label.setForeground(colors.textPrimary);
        row.add(label);

        Category[] cats = Category.values();
        defaultCategoryCombo = new JComboBox<>(cats);
        defaultCategoryCombo.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        defaultCategoryCombo.setPreferredSize(new Dimension(200, 32));
        defaultCategoryCombo.setSelectedItem(settings.getDefaultStartupCategory());
        defaultCategoryCombo.addActionListener(e -> {
            Category chosen = (Category) defaultCategoryCombo.getSelectedItem();
            settings.setDefaultStartupCategory(chosen);
            new SqliteSettingsRepository().persist(settings);
        });
        row.add(defaultCategoryCombo);

        section.add(row);
        return section;
    }

    private JPanel createDatabaseSection(ThemeColors colors) {
        JPanel section = createCardSection(colors, "LOCAL SQLITE PERSISTENCE", "Local relational database storage for offline reading and persistent state.");

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));

        int count = new SqliteArticleRepository().count();
        long sizeBytes = DatabaseManager.getInstance().getDatabaseFileSizeBytes();
        String sizeFormatted = String.format("%.2f KB", sizeBytes / 1024.0);

        JLabel pathLbl = new JLabel("Database File: " + AppConfig.getInstance().getDbPath() + " (" + sizeFormatted + ")");
        pathLbl.setFont(UIUtils.getSansFont(Font.BOLD, 12));
        pathLbl.setForeground(colors.textPrimary);
        details.add(pathLbl);
        details.add(Box.createVerticalStrut(4));

        JLabel countLbl = new JLabel("Cached Editorial Articles in SQLite: " + count + " articles");
        countLbl.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        countLbl.setForeground(colors.textSecondary);
        details.add(countLbl);
        details.add(Box.createVerticalStrut(10));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);

        RoundedButton vacuumBtn = new RoundedButton("Optimize & Vacuum Database", RoundedButton.Style.SECONDARY, 6);
        vacuumBtn.setPreferredSize(new Dimension(210, 30));
        vacuumBtn.addActionListener(e -> {
            try (Statement s = DatabaseManager.getInstance().getConnection().createStatement()) {
                s.execute("VACUUM;");
                JOptionPane.showMessageDialog(this, "SQLite database storage successfully optimized and compacted.", "Database Maintenance", JOptionPane.INFORMATION_MESSAGE);
                refreshView();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Vacuum error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnRow.add(vacuumBtn);
        details.add(btnRow);

        section.add(details);
        return section;
    }

    private JPanel createAboutSection(ThemeColors colors) {
        JPanel section = createCardSection(colors, "ABOUT NEWSROOM", "System architecture & engineering specifications.");

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(6, 12, 12, 12));

        JLabel vLabel = new JLabel("NEWSROOM Desktop Edition • Version 2.0.0 (Phase 2)");
        vLabel.setFont(UIUtils.getSansFont(Font.BOLD, 13));
        vLabel.setForeground(colors.accent);
        info.add(vLabel);
        info.add(Box.createVerticalStrut(6));

        String archText = """
                <html><body style='width: 620px; color: " + getHex(colors.textSecondary) + ";'>
                <b>Architecture:</b> Pure Java 21 & Java Swing desktop client with bespoke Graphics2D rendering.<br>
                <b>Phase 2 Real-Time Integrations:</b><br>
                • <i>Live News Providers:</i> Pluggable multi-provider architecture (RSS Syndication + NewsAPI.org).<br>
                • <i>Persistent Storage:</i> Embedded SQLite via JDBC with versioned schema migrations, transactions, and foreign keys.<br>
                • <i>Offline Cache:</i> Seamless offline reading with full fallback to local SQLite article repository.<br>
                • <i>Asynchronous I/O:</i> Background HTTP client threads, bounded retries, and asynchronous image caching.<br>
                • <i>OOP Foundations:</i> Encapsulation, Abstraction, Inheritance, Polymorphism, and Composition preserved throughout.
                </body></html>
                """;
        JLabel specLabel = new JLabel(archText);
        specLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        info.add(specLabel);

        section.add(info);
        return section;
    }

    private JPanel createResetSection(ThemeColors colors) {
        JPanel section = createCardSection(colors, "RESET CONFIGURATION", "Restore initial defaults for all preferences.");

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        row.setOpaque(false);

        RoundedButton resetBtn = new RoundedButton("Reset to Defaults", RoundedButton.Style.DANGER, 8);
        resetBtn.setPreferredSize(new Dimension(160, 36));
        resetBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Reset all reader settings, theme, provider preferences, and font sizes to original defaults?",
                    "Confirm Reset",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                settings.resetToDefaults();
                new SqliteSettingsRepository().persist(settings);
                ThemeManager.getInstance().setTheme(AppTheme.LIGHT);
                refreshView();
                JOptionPane.showMessageDialog(this, "Settings restored to original factory defaults.", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        row.add(resetBtn);

        section.add(row);
        return section;
    }

    private JPanel createCardSection(ThemeColors colors, String title, String subtitle) {
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(colors.surface);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(840, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colors.border, 1, true),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UIUtils.getSansFont(Font.BOLD, 12));
        titleLbl.setForeground(colors.accent);
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(4));

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        subLbl.setForeground(colors.textSecondary);
        card.add(subLbl);
        card.add(Box.createVerticalStrut(10));

        return card;
    }

    private String getHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    protected void onThemeUpdated(ThemeColors colors) {
        refreshView();
    }
}
