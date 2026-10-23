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
 * Compact horizontal article card for lists, search results, and bookmarks.
 * Demonstrates Inheritance from BaseArticleCard and streamlined UI density.
 */
public class CompactArticleCard extends BaseArticleCard {
    private JLabel titleLabel;
    private JLabel metaLabel;
    private RoundedButton bookmarkBtn;

    public CompactArticleCard(Article article, ArticleNavigationListener navListener, BookmarkService bookmarkService) {
        super(article, navListener, bookmarkService);
    }

    @Override
    protected void initCardLayout() {
        setLayout(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        this.cornerRadius = 8;
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        setPreferredSize(new Dimension(500, 88));

        // Mini visual cover on left
        ArticlePlaceholderImage cover = new ArticlePlaceholderImage(article, 68);
        cover.setPreferredSize(new Dimension(90, 68));
        add(cover, BorderLayout.WEST);

        // Center text container
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Category & Relative time
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        topRow.setOpaque(false);
        topRow.add(new CategoryBadge(article.getCategory(), true));

        JLabel time = new JLabel("•  " + DateTimeFormatterUtil.formatRelativeTime(article.getPublishedAt()));
        time.setFont(UIUtils.getSansFont(Font.PLAIN, 11));
        time.setForeground(ThemeManager.getInstance().getColors().textMuted);
        topRow.add(time);

        centerPanel.add(topRow);
        centerPanel.add(Box.createVerticalStrut(4));

        // Headline
        titleLabel = new JLabel("<html><body style='width: 480px;'><b>" + article.getTitle() + "</b></body></html>");
        titleLabel.setFont(UIUtils.getEditorialFont(Font.BOLD, 14));
        titleLabel.setForeground(ThemeManager.getInstance().getColors().textPrimary);
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(4));

        // Meta
        metaLabel = new JLabel(article.getSource() + " • " + article.getAuthor() + " • " + article.getReadingTimeMinutes() + " min");
        metaLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 11));
        metaLabel.setForeground(ThemeManager.getInstance().getColors().textMuted);
        centerPanel.add(metaLabel);

        add(centerPanel, BorderLayout.CENTER);

        // Right Action: Bookmark
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 18));
        rightPanel.setOpaque(false);

        boolean bookmarked = bookmarkService != null && bookmarkService.isBookmarked(article.getId());
        bookmarkBtn = new RoundedButton(bookmarked ? "★" : "☆", RoundedButton.Style.SECONDARY, 6);
        bookmarkBtn.setPreferredSize(new Dimension(32, 28));
        bookmarkBtn.setToolTipText(bookmarked ? "Remove bookmark" : "Save article");
        bookmarkBtn.addActionListener(e -> {
            if (bookmarkService != null) {
                boolean state = bookmarkService.toggleBookmark(article);
                bookmarkBtn.setText(state ? "★" : "☆");
            }
        });
        rightPanel.add(bookmarkBtn);

        add(rightPanel, BorderLayout.EAST);
    }

    @Override
    protected void onThemeUpdated(ThemeColors colors) {
        if (titleLabel != null) titleLabel.setForeground(colors.textPrimary);
        if (metaLabel != null) metaLabel.setForeground(colors.textMuted);
        if (bookmarkBtn != null && bookmarkService != null) {
            boolean bookmarked = bookmarkService.isBookmarked(article.getId());
            bookmarkBtn.setText(bookmarked ? "★" : "☆");
        }
    }
}
