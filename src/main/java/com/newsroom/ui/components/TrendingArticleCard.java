package com.newsroom.ui.components;

import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.model.Article;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
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
 * Trending article card displaying rank numbering (01, 02...) and velocity metrics.
 * Demonstrates Inheritance from BaseArticleCard.
 */
public class TrendingArticleCard extends BaseArticleCard {
    private final int rank;
    private JLabel rankLabel;
    private JLabel titleLabel;
    private JLabel metaLabel;
    private RoundedButton bookmarkBtn;

    public TrendingArticleCard(Article article, int rank, ArticleNavigationListener navListener, BookmarkService bookmarkService) {
        super(article, navListener, bookmarkService);
        this.rank = rank;
    }

    @Override
    protected void initCardLayout() {
        setLayout(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        this.cornerRadius = 8;
        setPreferredSize(new Dimension(300, 72));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        // Rank number (01, 02, etc.)
        rankLabel = new JLabel(String.format("%02d", rank));
        rankLabel.setFont(UIUtils.getEditorialFont(Font.BOLD, 22));
        rankLabel.setForeground(ThemeManager.getInstance().getColors().accent);
        rankLabel.setPreferredSize(new Dimension(36, 40));
        add(rankLabel, BorderLayout.WEST);

        // Center content
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        top.setOpaque(false);
        top.add(new CategoryBadge(article.getCategory(), true));

        metaLabel = new JLabel("• " + article.getReadingTimeMinutes() + "m read");
        metaLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 11));
        metaLabel.setForeground(ThemeManager.getInstance().getColors().textMuted);
        top.add(metaLabel);

        center.add(top);
        center.add(Box.createVerticalStrut(3));

        titleLabel = new JLabel("<html><body style='width: 200px;'><b>" + article.getTitle() + "</b></body></html>");
        titleLabel.setFont(UIUtils.getEditorialFont(Font.BOLD, 13));
        titleLabel.setForeground(ThemeManager.getInstance().getColors().textPrimary);
        center.add(titleLabel);

        add(center, BorderLayout.CENTER);

        // Bookmark button
        boolean bookmarked = bookmarkService != null && bookmarkService.isBookmarked(article.getId());
        bookmarkBtn = new RoundedButton(bookmarked ? "★" : "☆", RoundedButton.Style.GHOST, 6);
        bookmarkBtn.setPreferredSize(new Dimension(30, 26));
        bookmarkBtn.addActionListener(e -> {
            if (bookmarkService != null) {
                boolean state = bookmarkService.toggleBookmark(article);
                bookmarkBtn.setText(state ? "★" : "☆");
            }
        });
        add(bookmarkBtn, BorderLayout.EAST);
    }

    @Override
    protected void onThemeUpdated(ThemeColors colors) {
        if (rankLabel != null) rankLabel.setForeground(colors.accent);
        if (titleLabel != null) titleLabel.setForeground(colors.textPrimary);
        if (metaLabel != null) metaLabel.setForeground(colors.textMuted);
        if (bookmarkBtn != null && bookmarkService != null) {
            boolean bookmarked = bookmarkService.isBookmarked(article.getId());
            bookmarkBtn.setText(bookmarked ? "★" : "☆");
        }
    }
}
