package com.newsroom.ui;

import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkChangeListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.interfaces.NewsService;
import com.newsroom.interfaces.ReadingHistoryService;
import com.newsroom.model.Article;
import com.newsroom.model.ReaderSettings;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.ui.components.CompactArticleCard;
import com.newsroom.ui.components.EmptyStatePanel;
import com.newsroom.util.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

/**
 * Saved Articles / Bookmarks view.
 * Demonstrates Observer pattern by listening to BookmarkService events.
 */
public class BookmarksPanel extends BaseViewPanel implements BookmarkChangeListener {
    private JPanel listContainer;
    private JLabel countBadge;

    public BookmarksPanel(NewsService newsService,
                          BookmarkService bookmarkService,
                          ReadingHistoryService historyService,
                          ArticleNavigationListener navListener,
                          ReaderSettings settings) {
        super(newsService, bookmarkService, historyService, navListener, settings);
        if (bookmarkService != null) {
            bookmarkService.addBookmarkListener(this);
        }
        initUI();
    }

    private void initUI() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(20, 28, 36, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Saved Articles");
        title.setFont(UIUtils.getEditorialFont(Font.BOLD, 28));
        title.setForeground(ThemeManager.getInstance().getColors().textPrimary);
        left.add(title);
        left.add(Box.createVerticalStrut(4));

        JLabel desc = new JLabel("Offline reading list curated by you during this session.");
        desc.setFont(UIUtils.getSansFont(Font.PLAIN, 13));
        desc.setForeground(ThemeManager.getInstance().getColors().textSecondary);
        left.add(desc);

        header.add(left, BorderLayout.WEST);

        countBadge = new JLabel("0 Saved");
        countBadge.setFont(UIUtils.getSansFont(Font.BOLD, 13));
        countBadge.setForeground(ThemeManager.getInstance().getColors().accent);
        header.add(countBadge, BorderLayout.EAST);

        container.add(header);
        container.add(Box.createVerticalStrut(24));

        // List Container
        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);
        container.add(listContainer);

        add(wrapInScrollPane(container), BorderLayout.CENTER);
        refreshView();
    }

    @Override
    public void refreshView() {
        if (listContainer == null) return;
        listContainer.removeAll();

        List<Article> bookmarked = bookmarkService.getBookmarkedArticles();
        countBadge.setText(bookmarked.size() + " Saved");

        if (bookmarked.isEmpty()) {
            listContainer.add(new EmptyStatePanel(
                    "★",
                    "No Saved Articles Yet",
                    "Bookmark stories by clicking the star or save icon on any article card to revisit them here offline.",
                    "Explore Top Stories",
                    navListener::onNavigateToHome
            ));
        } else {
            for (Article art : bookmarked) {
                listContainer.add(new CompactArticleCard(art, navListener, bookmarkService));
                listContainer.add(Box.createVerticalStrut(8));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    @Override
    public void onBookmarkChanged(Article article, boolean isBookmarked) {
        refreshView();
    }

    @Override
    protected void onThemeUpdated(ThemeColors colors) {
        if (countBadge != null) {
            countBadge.setForeground(colors.accent);
        }
        refreshView();
    }
}
