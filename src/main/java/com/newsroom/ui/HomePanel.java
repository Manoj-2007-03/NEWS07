package com.newsroom.ui;

import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.interfaces.NewsService;
import com.newsroom.interfaces.ReadingHistoryService;
import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.model.ReaderSettings;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.ui.components.CategoryBadge;
import com.newsroom.ui.components.CompactArticleCard;
import com.newsroom.ui.components.FeaturedArticleCard;
import com.newsroom.ui.components.GridArticleCard;
import com.newsroom.ui.components.RoundedButton;
import com.newsroom.ui.components.TrendingArticleCard;
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
import java.awt.GridLayout;
import java.util.List;

/**
 * Editorial Home Dashboard view.
 * Demonstrates Inheritance from BaseViewPanel, dynamic data binding, and varied card layouts.
 */
public class HomePanel extends BaseViewPanel {
    private JPanel mainContent;

    public HomePanel(NewsService newsService,
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
        mainContent.setBorder(BorderFactory.createEmptyBorder(16, 28, 36, 28));

        refreshView();
        add(wrapInScrollPane(mainContent), BorderLayout.CENTER);
    }

    @Override
    public void refreshView() {
        mainContent.removeAll();
        ThemeColors colors = ThemeManager.getInstance().getColors();

        // 1. Editorial Welcome Banner
        mainContent.add(createWelcomeBanner(colors));
        mainContent.add(Box.createVerticalStrut(20));

        // 2. Featured Lead Hero Story
        Article featured = newsService.getFeaturedArticle();
        if (featured != null) {
            mainContent.add(new FeaturedArticleCard(featured, navListener, bookmarkService));
            mainContent.add(Box.createVerticalStrut(24));
        }

        // 3. Category Shortcuts Pill Bar
        mainContent.add(createCategoryPillBar());
        mainContent.add(Box.createVerticalStrut(24));

        // 4. Two-Column Dashboard Layout (Top Stories on Left, Trending & Latest on Right)
        JPanel splitRow = new JPanel(new BorderLayout(24, 0));
        splitRow.setOpaque(false);

        // Left Column: Top Stories Grid
        JPanel leftColumn = new JPanel();
        leftColumn.setOpaque(false);
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));

        JLabel topStoriesTitle = new JLabel("Top Stories");
        topStoriesTitle.setFont(UIUtils.getEditorialFont(Font.BOLD, 20));
        topStoriesTitle.setForeground(colors.textPrimary);
        leftColumn.add(topStoriesTitle);
        leftColumn.add(Box.createVerticalStrut(12));

        List<Article> topStories = newsService.getTopStories(4);
        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 16, 16));
        gridPanel.setOpaque(false);
        for (Article art : topStories) {
            gridPanel.add(new GridArticleCard(art, navListener, bookmarkService));
        }
        leftColumn.add(gridPanel);

        splitRow.add(leftColumn, BorderLayout.CENTER);

        // Right Column: Trending Stories & Quick Bookmarks Widget
        JPanel rightColumn = new JPanel();
        rightColumn.setOpaque(false);
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setPreferredSize(new Dimension(340, 600));

        // Trending Header
        JLabel trendingTitle = new JLabel("Trending Velocity");
        trendingTitle.setFont(UIUtils.getEditorialFont(Font.BOLD, 18));
        trendingTitle.setForeground(colors.textPrimary);
        rightColumn.add(trendingTitle);
        rightColumn.add(Box.createVerticalStrut(12));

        List<Article> trending = newsService.getTrendingArticles(4);
        int rank = 1;
        for (Article art : trending) {
            rightColumn.add(new TrendingArticleCard(art, rank++, navListener, bookmarkService));
            rightColumn.add(Box.createVerticalStrut(8));
        }

        // Saved Articles Shortcut Widget
        rightColumn.add(Box.createVerticalStrut(16));
        rightColumn.add(createSavedWidget(colors));

        splitRow.add(rightColumn, BorderLayout.EAST);
        mainContent.add(splitRow);
        mainContent.add(Box.createVerticalStrut(28));

        // 5. Latest Headlines Section
        JLabel latestTitle = new JLabel("Latest Dispatches from the Desks");
        latestTitle.setFont(UIUtils.getEditorialFont(Font.BOLD, 20));
        latestTitle.setForeground(colors.textPrimary);
        mainContent.add(latestTitle);
        mainContent.add(Box.createVerticalStrut(12));

        List<Article> latest = newsService.getLatestHeadlines(6);
        for (Article art : latest) {
            mainContent.add(new CompactArticleCard(art, navListener, bookmarkService));
            mainContent.add(Box.createVerticalStrut(8));
        }

        mainContent.revalidate();
        mainContent.repaint();
    }

    private JPanel createWelcomeBanner(ThemeColors colors) {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel editionLabel = new JLabel("THE MORNING EDITION • " + DateTimeFormatterUtil.formatHeaderDate().toUpperCase());
        editionLabel.setFont(UIUtils.getSansFont(Font.BOLD, 11));
        editionLabel.setForeground(colors.accent);
        left.add(editionLabel);
        left.add(Box.createVerticalStrut(4));

        JLabel masthead = new JLabel("Curated Editorial Intelligence");
        masthead.setFont(UIUtils.getEditorialFont(Font.BOLD, 28));
        masthead.setForeground(colors.textPrimary);
        left.add(masthead);

        banner.add(left, BorderLayout.WEST);

        // Right summary tag
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        right.setOpaque(false);
        JLabel sampleNotice = new JLabel("Sample Editorial Edition • Phase 1");
        sampleNotice.setFont(UIUtils.getSansFont(Font.ITALIC, 12));
        sampleNotice.setForeground(colors.textMuted);
        right.add(sampleNotice);
        banner.add(right, BorderLayout.EAST);

        return banner;
    }

    private JPanel createCategoryPillBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);

        Category[] shortcuts = {
                Category.WORLD, Category.INDIA, Category.TECHNOLOGY,
                Category.BUSINESS, Category.SCIENCE, Category.SPORTS, Category.ENTERTAINMENT
        };

        for (Category cat : shortcuts) {
            RoundedButton btn = new RoundedButton(cat.getDisplayName(), RoundedButton.Style.SECONDARY, 14);
            btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, 32));
            btn.addActionListener(e -> {
                if (navListener != null) {
                    navListener.onCategorySelected(cat);
                }
            });
            bar.add(btn);
        }

        return bar;
    }

    private JPanel createSavedWidget(ThemeColors colors) {
        JPanel widget = new JPanel(new BorderLayout(12, 12));
        widget.setOpaque(true);
        widget.setBackground(colors.surface);
        widget.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colors.border, 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JPanel textSide = new JPanel();
        textSide.setOpaque(false);
        textSide.setLayout(new BoxLayout(textSide, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Saved Reading List");
        title.setFont(UIUtils.getSansFont(Font.BOLD, 13));
        title.setForeground(colors.textPrimary);
        textSide.add(title);
        textSide.add(Box.createVerticalStrut(3));

        int count = bookmarkService != null ? bookmarkService.getBookmarkCount() : 0;
        JLabel desc = new JLabel(count + " articles saved offline");
        desc.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        desc.setForeground(colors.textSecondary);
        textSide.add(desc);

        widget.add(textSide, BorderLayout.CENTER);

        RoundedButton viewBtn = new RoundedButton("Open →", RoundedButton.Style.PRIMARY, 6);
        viewBtn.setPreferredSize(new Dimension(80, 32));
        viewBtn.addActionListener(e -> {
            if (navListener != null) navListener.onNavigateToSaved();
        });
        widget.add(viewBtn, BorderLayout.EAST);

        return widget;
    }

    @Override
    protected void onThemeUpdated(ThemeColors colors) {
        refreshView();
    }
}
