package com.newsroom.ui.components;

import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.model.Article;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.util.DateTimeFormatterUtil;
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
import java.awt.GridLayout;

/**
 * Featured Hero Article Card for prominent homepage editorial placement.
 * Demonstrates Inheritance from BaseArticleCard and Custom UI Composition.
 */
public class FeaturedArticleCard extends BaseArticleCard {
    private JLabel titleLabel;
    private JLabel summaryLabel;
    private JLabel metaLabel;
    private RoundedButton bookmarkBtn;

    public FeaturedArticleCard(Article article, ArticleNavigationListener navListener, BookmarkService bookmarkService) {
        super(article, navListener, bookmarkService);
    }

    @Override
    protected void initCardLayout() {
        setLayout(new BorderLayout(24, 0));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        this.cornerRadius = 14;

        // Visual header / cover (Left or Top, responsive)
        ArticlePlaceholderImage cover = new ArticlePlaceholderImage(article, 240, true);
        cover.setPreferredSize(new Dimension(380, 240));
        add(cover, BorderLayout.WEST);

        // Content Column
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Category & Badge Row
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(new CategoryBadge(article.getCategory()));

        JLabel featureTag = new JLabel("★ EDITORIAL LEAD STORY");
        featureTag.setFont(UIUtils.getSansFont(Font.BOLD, 10));
        featureTag.setForeground(ThemeManager.getInstance().getColors().accent);
        badgeRow.add(featureTag);

        contentPanel.add(badgeRow);
        contentPanel.add(Box.createVerticalStrut(10));

        // Headline
        titleLabel = new JLabel("<html><body style='width: 380px;'>" + article.getTitle() + "</body></html>");
        titleLabel.setFont(UIUtils.getEditorialFont(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getInstance().getColors().textPrimary);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Summary
        summaryLabel = new JLabel("<html><body style='width: 380px;'>" + article.getSummary() + "</body></html>");
        summaryLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 13));
        summaryLabel.setForeground(ThemeManager.getInstance().getColors().textSecondary);
        contentPanel.add(summaryLabel);
        contentPanel.add(Box.createVerticalStrut(14));

        // Meta row
        String metaText = String.format("By %s • %s • %d min read",
                article.getAuthor(),
                DateTimeFormatterUtil.formatCardDate(article.getPublishedAt()),
                article.getReadingTimeMinutes());
        metaLabel = new JLabel(metaText);
        metaLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 11));
        metaLabel.setForeground(ThemeManager.getInstance().getColors().textMuted);
        contentPanel.add(metaLabel);
        contentPanel.add(Box.createVerticalGlue());

        // Action row
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);

        RoundedButton readBtn = new RoundedButton("Read Full Story  →", RoundedButton.Style.PRIMARY);
        readBtn.setPreferredSize(new Dimension(150, 36));
        readBtn.addActionListener(e -> {
            if (navListener != null) navListener.onArticleSelected(article);
        });
        actionRow.add(readBtn);

        boolean bookmarked = bookmarkService != null && bookmarkService.isBookmarked(article.getId());
        bookmarkBtn = new RoundedButton(bookmarked ? "★ Saved" : "☆ Save", RoundedButton.Style.SECONDARY);
        bookmarkBtn.setPreferredSize(new Dimension(100, 36));
        bookmarkBtn.addActionListener(e -> {
            if (bookmarkService != null) {
                boolean newState = bookmarkService.toggleBookmark(article);
                bookmarkBtn.setText(newState ? "★ Saved" : "☆ Save");
            }
        });
        actionRow.add(bookmarkBtn);

        contentPanel.add(actionRow);

        add(contentPanel, BorderLayout.CENTER);
    }

    @Override
    protected void onThemeUpdated(ThemeColors colors) {
        if (titleLabel != null) titleLabel.setForeground(colors.textPrimary);
        if (summaryLabel != null) summaryLabel.setForeground(colors.textSecondary);
        if (metaLabel != null) metaLabel.setForeground(colors.textMuted);
        if (bookmarkBtn != null && bookmarkService != null) {
            boolean bookmarked = bookmarkService.isBookmarked(article.getId());
            bookmarkBtn.setText(bookmarked ? "★ Saved" : "☆ Save");
        }
    }
}
