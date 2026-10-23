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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Standard grid article card for editorial grids and category views.
 * Demonstrates Inheritance from BaseArticleCard and responsive card styling.
 */
public class GridArticleCard extends BaseArticleCard {
    private JLabel titleLabel;
    private JLabel summaryLabel;
    private JLabel metaLabel;
    private RoundedButton bookmarkBtn;

    public GridArticleCard(Article article, ArticleNavigationListener navListener, BookmarkService bookmarkService) {
        super(article, navListener, bookmarkService);
    }

    @Override
    protected void initCardLayout() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 14, 12));
        this.cornerRadius = 10;

        // Image header
        ArticlePlaceholderImage cover = new ArticlePlaceholderImage(article, 130);
        add(cover, BorderLayout.NORTH);

        // Content Area
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Category & relative date
        JPanel topMeta = new JPanel(new BorderLayout());
        topMeta.setOpaque(false);
        topMeta.add(new CategoryBadge(article.getCategory(), true), BorderLayout.WEST);

        JLabel timeLabel = new JLabel(DateTimeFormatterUtil.formatRelativeTime(article.getPublishedAt()));
        timeLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 11));
        timeLabel.setForeground(ThemeManager.getInstance().getColors().textMuted);
        topMeta.add(timeLabel, BorderLayout.EAST);

        contentPanel.add(topMeta);
        contentPanel.add(Box.createVerticalStrut(8));

        // Headline
        titleLabel = new JLabel("<html><body style='width: 240px;'><b>" + article.getTitle() + "</b></body></html>");
        titleLabel.setFont(UIUtils.getEditorialFont(Font.BOLD, 15));
        titleLabel.setForeground(ThemeManager.getInstance().getColors().textPrimary);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(6));

        // Short summary
        String shortSummary = article.getSummary();
        if (shortSummary.length() > 100) {
            shortSummary = shortSummary.substring(0, 97) + "...";
        }
        summaryLabel = new JLabel("<html><body style='width: 240px;'>" + shortSummary + "</body></html>");
        summaryLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        summaryLabel.setForeground(ThemeManager.getInstance().getColors().textSecondary);
        contentPanel.add(summaryLabel);
        contentPanel.add(Box.createVerticalGlue());

        add(contentPanel, BorderLayout.CENTER);

        // Footer Row
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        metaLabel = new JLabel(article.getReadingTimeMinutes() + " min read • " + article.getAuthor());
        metaLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 11));
        metaLabel.setForeground(ThemeManager.getInstance().getColors().textMuted);
        footer.add(metaLabel, BorderLayout.WEST);

        boolean bookmarked = bookmarkService != null && bookmarkService.isBookmarked(article.getId());
        bookmarkBtn = new RoundedButton(bookmarked ? "★" : "☆", RoundedButton.Style.SECONDARY, 6);
        bookmarkBtn.setPreferredSize(new Dimension(32, 26));
        bookmarkBtn.setToolTipText(bookmarked ? "Remove bookmark" : "Save article");
        bookmarkBtn.addActionListener(e -> {
            if (bookmarkService != null) {
                boolean state = bookmarkService.toggleBookmark(article);
                bookmarkBtn.setText(state ? "★" : "☆");
            }
        });
        footer.add(bookmarkBtn, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);
    }

    @Override
    protected void onThemeUpdated(ThemeColors colors) {
        if (titleLabel != null) titleLabel.setForeground(colors.textPrimary);
        if (summaryLabel != null) summaryLabel.setForeground(colors.textSecondary);
        if (metaLabel != null) metaLabel.setForeground(colors.textMuted);
        if (bookmarkBtn != null && bookmarkService != null) {
            boolean bookmarked = bookmarkService.isBookmarked(article.getId());
            bookmarkBtn.setText(bookmarked ? "★" : "☆");
        }
    }
}
