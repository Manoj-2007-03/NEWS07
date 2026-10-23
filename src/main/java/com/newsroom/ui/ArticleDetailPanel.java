package com.newsroom.ui;

import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.interfaces.NewsService;
import com.newsroom.interfaces.ReadingHistoryService;
import com.newsroom.model.Article;
import com.newsroom.model.ReaderSettings;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.ui.components.ArticlePlaceholderImage;
import com.newsroom.ui.components.CategoryBadge;
import com.newsroom.ui.components.RoundedButton;
import com.newsroom.util.DateTimeFormatterUtil;
import com.newsroom.util.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;

/**
 * Dedicated Article Reading Screen - Phase 2 Edition.
 * Features typographic scaling, session reading history tracking,
 * full-content transparency, link copying, and browser launch for original reporting.
 */
public class ArticleDetailPanel extends BaseViewPanel {
    private Article currentArticle;
    private JPanel contentContainer;
    private JLabel headlineLabel;
    private JLabel summaryLabel;
    private JLabel metaLabel;
    private JTextPane bodyTextPane;
    private RoundedButton bookmarkBtn;
    private RoundedButton prevBtn;
    private RoundedButton nextBtn;
    private JLabel fontSizeLabel;

    public ArticleDetailPanel(NewsService newsService,
                              BookmarkService bookmarkService,
                              ReadingHistoryService historyService,
                              ArticleNavigationListener navListener,
                              ReaderSettings settings) {
        super(newsService, bookmarkService, historyService, navListener, settings);
        initUI();
    }

    private void initUI() {
        contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setOpaque(false);
        contentContainer.setBorder(BorderFactory.createEmptyBorder(18, 48, 48, 48));

        add(wrapInScrollPane(contentContainer), BorderLayout.CENTER);
    }

    public void setArticle(Article article) {
        this.currentArticle = article;
        if (article != null && historyService != null) {
            historyService.recordAccess(article);
        }
        refreshView();
    }

    @Override
    public void refreshView() {
        contentContainer.removeAll();
        if (currentArticle == null) return;

        ThemeColors colors = ThemeManager.getInstance().getColors();

        // 1. Navigation Action Bar (Back, Prev, Next, Bookmark, Font Size controls)
        contentContainer.add(createReaderActionBar(colors));
        contentContainer.add(Box.createVerticalStrut(24));

        // 2. Reading Column (Centered editorial column, max width ~840px)
        JPanel readingColumn = new JPanel();
        readingColumn.setLayout(new BoxLayout(readingColumn, BoxLayout.Y_AXIS));
        readingColumn.setOpaque(false);
        readingColumn.setMaximumSize(new Dimension(840, Integer.MAX_VALUE));
        readingColumn.setAlignmentX(0.0f);

        // Category Badge & Transparency notice
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(new CategoryBadge(currentArticle.getCategory()));
        readingColumn.add(badgeRow);
        readingColumn.add(Box.createVerticalStrut(14));

        // Headline
        headlineLabel = new JLabel("<html><body style='width: 760px; line-height: 1.25;'>" + currentArticle.getTitle() + "</body></html>");
        headlineLabel.setFont(UIUtils.getEditorialFont(Font.BOLD, 30));
        headlineLabel.setForeground(colors.textPrimary);
        readingColumn.add(headlineLabel);
        readingColumn.add(Box.createVerticalStrut(14));

        // Subtitle / Summary (Lead paragraph)
        summaryLabel = new JLabel("<html><body style='width: 760px; font-style: italic; line-height: 1.4;'>" + currentArticle.getSummary() + "</body></html>");
        summaryLabel.setFont(UIUtils.getEditorialFont(Font.PLAIN, 17));
        summaryLabel.setForeground(colors.textSecondary);
        readingColumn.add(summaryLabel);
        readingColumn.add(Box.createVerticalStrut(16));

        // Metadata Bar
        String metaText = String.format("By %s  •  %s  •  %s  •  %d min read",
                currentArticle.getAuthor(),
                currentArticle.getSource(),
                DateTimeFormatterUtil.formatDetailDate(currentArticle.getPublishedAt()),
                currentArticle.getReadingTimeMinutes());
        metaLabel = new JLabel(metaText);
        metaLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        metaLabel.setForeground(colors.textMuted);
        readingColumn.add(metaLabel);
        readingColumn.add(Box.createVerticalStrut(20));

        // Editorial Image / Cover Header
        ArticlePlaceholderImage cover = new ArticlePlaceholderImage(currentArticle, 320, true);
        cover.setMaximumSize(new Dimension(840, 320));
        readingColumn.add(cover);
        readingColumn.add(Box.createVerticalStrut(24));

        // Transparency banner when reading syndicated summaries
        if (!currentArticle.hasFullContent()) {
            readingColumn.add(createSummaryTransparencyBanner(colors));
            readingColumn.add(Box.createVerticalStrut(18));
        }

        // Article Body Text (Formatted with comfortable reading typography)
        bodyTextPane = new JTextPane();
        bodyTextPane.setEditable(false);
        bodyTextPane.setOpaque(false);
        bodyTextPane.setMaximumSize(new Dimension(840, Integer.MAX_VALUE));

        updateBodyText(colors);
        readingColumn.add(bodyTextPane);
        readingColumn.add(Box.createVerticalStrut(24));

        // External Link Actions: Open Original & Share/Copy
        if (currentArticle.hasOriginalUrl()) {
            readingColumn.add(createExternalLinkRow(colors));
            readingColumn.add(Box.createVerticalStrut(32));
        }

        // Article Footer with Previous / Next Controls
        readingColumn.add(createStoryFooter(colors));

        contentContainer.add(readingColumn);
        contentContainer.revalidate();
        contentContainer.repaint();
    }

    private JPanel createSummaryTransparencyBanner(ThemeColors colors) {
        JPanel banner = new JPanel(new BorderLayout(10, 0));
        banner.setOpaque(true);
        banner.setBackground(colors.tagBackground);
        banner.setMaximumSize(new Dimension(840, 56));
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colors.border, 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel info = new JLabel("<html><b>EDITORIAL SYNOPSIS:</b> This summary is syndicated from "
                + currentArticle.getSource() + ". Use 'Open Original Article' to read complete interactive coverage.</html>");
        info.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        info.setForeground(colors.textSecondary);
        banner.add(info, BorderLayout.CENTER);

        return banner;
    }

    private JPanel createExternalLinkRow(ThemeColors colors) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(840, 42));

        RoundedButton openOriginalBtn = new RoundedButton("Open Original Article  ↗", RoundedButton.Style.PRIMARY, 6);
        openOriginalBtn.setPreferredSize(new Dimension(190, 36));
        openOriginalBtn.setToolTipText("Open full reporting in your system browser: " + currentArticle.getOriginalUrl());
        openOriginalBtn.addActionListener(e -> openUrlInBrowser(currentArticle.getOriginalUrl()));
        row.add(openOriginalBtn);

        RoundedButton copyLinkBtn = new RoundedButton("Copy Link", RoundedButton.Style.SECONDARY, 6);
        copyLinkBtn.setPreferredSize(new Dimension(100, 36));
        copyLinkBtn.setToolTipText("Copy canonical URL to clipboard");
        copyLinkBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(currentArticle.getOriginalUrl()), null);
            JOptionPane.showMessageDialog(this, "Article link copied to clipboard!\n" + currentArticle.getOriginalUrl(), "Link Copied", JOptionPane.INFORMATION_MESSAGE);
        });
        row.add(copyLinkBtn);

        return row;
    }

    private void openUrlInBrowser(String url) {
        if (url == null || url.trim().isEmpty() || !url.startsWith("http")) {
            JOptionPane.showMessageDialog(this, "Original URL is unavailable for this article.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url.trim()));
            } else {
                JOptionPane.showMessageDialog(this, "Cannot launch desktop browser. Link:\n" + url, "Notice", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not open browser: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createReaderActionBar(ThemeColors colors) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(840, 40));
        bar.setAlignmentX(0.0f);

        // Left: Back button
        RoundedButton backBtn = new RoundedButton("← Back to News", RoundedButton.Style.SECONDARY, 6);
        backBtn.setPreferredSize(new Dimension(130, 32));
        backBtn.addActionListener(e -> {
            if (navListener != null) navListener.onNavigateToHome();
        });
        bar.add(backBtn, BorderLayout.WEST);

        // Right: Font size controls + Bookmark button
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightControls.setOpaque(false);

        RoundedButton decreaseFontBtn = new RoundedButton("A-", RoundedButton.Style.SECONDARY, 6);
        decreaseFontBtn.setPreferredSize(new Dimension(42, 32));
        decreaseFontBtn.setToolTipText("Decrease reader font size");
        decreaseFontBtn.addActionListener(e -> {
            settings.decreaseFontSize();
            updateFontSizeDisplay();
            updateBodyText(ThemeManager.getInstance().getColors());
        });
        rightControls.add(decreaseFontBtn);

        fontSizeLabel = new JLabel(settings.getBodyFontSize() + "pt");
        fontSizeLabel.setFont(UIUtils.getSansFont(Font.BOLD, 12));
        fontSizeLabel.setForeground(colors.textSecondary);
        rightControls.add(fontSizeLabel);

        RoundedButton increaseFontBtn = new RoundedButton("A+", RoundedButton.Style.SECONDARY, 6);
        increaseFontBtn.setPreferredSize(new Dimension(42, 32));
        increaseFontBtn.setToolTipText("Increase reader font size");
        increaseFontBtn.addActionListener(e -> {
            settings.increaseFontSize();
            updateFontSizeDisplay();
            updateBodyText(ThemeManager.getInstance().getColors());
        });
        rightControls.add(increaseFontBtn);

        boolean bookmarked = bookmarkService != null && currentArticle != null && bookmarkService.isBookmarked(currentArticle.getId());
        bookmarkBtn = new RoundedButton(bookmarked ? "★ Saved" : "☆ Save", RoundedButton.Style.PRIMARY, 6);
        bookmarkBtn.setPreferredSize(new Dimension(100, 32));
        bookmarkBtn.addActionListener(e -> {
            if (bookmarkService != null && currentArticle != null) {
                boolean state = bookmarkService.toggleBookmark(currentArticle);
                bookmarkBtn.setText(state ? "★ Saved" : "☆ Save");
            }
        });
        rightControls.add(bookmarkBtn);

        bar.add(rightControls, BorderLayout.EAST);
        return bar;
    }

    private JPanel createStoryFooter(ThemeColors colors) {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, colors.border),
                BorderFactory.createEmptyBorder(20, 0, 20, 0)
        ));

        Article prev = newsService != null ? newsService.getPreviousArticle(currentArticle) : null;
        prevBtn = new RoundedButton("← Previous Story", RoundedButton.Style.SECONDARY, 6);
        prevBtn.setPreferredSize(new Dimension(150, 36));
        prevBtn.setEnabled(prev != null);
        prevBtn.addActionListener(e -> {
            if (prev != null) setArticle(prev);
        });
        footer.add(prevBtn, BorderLayout.WEST);

        Article next = newsService != null ? newsService.getNextArticle(currentArticle) : null;
        nextBtn = new RoundedButton("Next Story →", RoundedButton.Style.SECONDARY, 6);
        nextBtn.setPreferredSize(new Dimension(150, 36));
        nextBtn.setEnabled(next != null);
        nextBtn.addActionListener(e -> {
            if (next != null) setArticle(next);
        });
        footer.add(nextBtn, BorderLayout.EAST);

        return footer;
    }

    private void updateFontSizeDisplay() {
        if (fontSizeLabel != null) {
            fontSizeLabel.setText(settings.getBodyFontSize() + "pt");
        }
    }

    private void updateBodyText(ThemeColors colors) {
        if (bodyTextPane == null || currentArticle == null) return;

        bodyTextPane.setText(currentArticle.getContent());

        StyledDocument doc = bodyTextPane.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, UIUtils.FONT_SERIF_NAME);
        StyleConstants.setFontSize(attrs, settings.getBodyFontSize());
        StyleConstants.setForeground(attrs, colors.textPrimary);
        StyleConstants.setLineSpacing(attrs, 0.45f); // Comfortable editorial line height
        StyleConstants.setSpaceBelow(attrs, 16f); // Paragraph separation
        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
    }

    @Override
    protected void onThemeUpdated(ThemeColors colors) {
        if (headlineLabel != null) headlineLabel.setForeground(colors.textPrimary);
        if (summaryLabel != null) summaryLabel.setForeground(colors.textSecondary);
        if (metaLabel != null) metaLabel.setForeground(colors.textMuted);
        if (fontSizeLabel != null) fontSizeLabel.setForeground(colors.textSecondary);
        if (bookmarkBtn != null && bookmarkService != null && currentArticle != null) {
            boolean bookmarked = bookmarkService.isBookmarked(currentArticle.getId());
            bookmarkBtn.setText(bookmarked ? "★ Saved" : "☆ Save");
        }
        updateBodyText(colors);
    }
}
