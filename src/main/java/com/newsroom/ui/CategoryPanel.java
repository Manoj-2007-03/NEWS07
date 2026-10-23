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
import com.newsroom.ui.components.EmptyStatePanel;
import com.newsroom.ui.components.GridArticleCard;
import com.newsroom.ui.components.RoundedButton;
import com.newsroom.util.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Editorial Category View displaying articles filtered by desk.
 * Demonstrates Polymorphism and Collections filtering/sorting.
 */
public class CategoryPanel extends BaseViewPanel {
    private Category currentCategory = Category.WORLD;
    private String currentSort = "Newest First";
    private JPanel cardsGrid;
    private JLabel countLabel;
    private JLabel titleLabel;
    private JLabel descLabel;

    public CategoryPanel(NewsService newsService,
                         BookmarkService bookmarkService,
                         ReadingHistoryService historyService,
                         ArticleNavigationListener navListener,
                         ReaderSettings settings) {
        super(newsService, bookmarkService, historyService, navListener, settings);
        initUI();
    }

    private void initUI() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(20, 28, 36, 28));

        // Header section
        container.add(createCategoryHeader());
        container.add(Box.createVerticalStrut(20));

        // Cards grid container
        cardsGrid = new JPanel(new GridLayout(0, 3, 18, 18));
        cardsGrid.setOpaque(false);
        container.add(cardsGrid);

        add(wrapInScrollPane(container), BorderLayout.CENTER);
        refreshView();
    }

    private JPanel createCategoryHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        titleLabel = new JLabel(currentCategory.getDisplayName());
        titleLabel.setFont(UIUtils.getEditorialFont(Font.BOLD, 28));
        left.add(titleLabel);
        left.add(Box.createVerticalStrut(4));

        descLabel = new JLabel(currentCategory.getDescription());
        descLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 13));
        left.add(descLabel);

        header.add(left, BorderLayout.WEST);

        // Right side: Sort dropdown & count badge
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        right.setOpaque(false);

        countLabel = new JLabel("0 Articles");
        countLabel.setFont(UIUtils.getSansFont(Font.BOLD, 12));
        right.add(countLabel);

        String[] sortOptions = {"Newest First", "Reading Time (Shortest)", "Reading Time (Longest)"};
        JComboBox<String> sortCombo = new JComboBox<>(sortOptions);
        sortCombo.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        sortCombo.setPreferredSize(new Dimension(180, 30));
        sortCombo.addActionListener(e -> {
            currentSort = (String) sortCombo.getSelectedItem();
            populateArticles();
        });
        right.add(sortCombo);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    public void setCategory(Category category) {
        this.currentCategory = (category != null) ? category : Category.LATEST;
        if (titleLabel != null) titleLabel.setText(currentCategory.getDisplayName());
        if (descLabel != null) descLabel.setText(currentCategory.getDescription());
        refreshView();
    }

    @Override
    public void refreshView() {
        populateArticles();
    }

    private void populateArticles() {
        if (cardsGrid == null) return;
        cardsGrid.removeAll();

        List<Article> articles = new ArrayList<>(newsService.getArticlesByCategory(currentCategory));

        // Sort articles
        if ("Reading Time (Shortest)".equals(currentSort)) {
            articles.sort(Comparator.comparingInt(Article::getReadingTimeMinutes));
        } else if ("Reading Time (Longest)".equals(currentSort)) {
            articles.sort(Comparator.comparingInt(Article::getReadingTimeMinutes).reversed());
        } else {
            articles.sort(Comparator.comparing(Article::getPublishedAt).reversed());
        }

        if (countLabel != null) {
            countLabel.setText(articles.size() + " Articles");
        }

        if (articles.isEmpty()) {
            cardsGrid.setLayout(new BorderLayout());
            cardsGrid.add(new EmptyStatePanel(
                    "⌕",
                    "No Articles Available",
                    "No sample articles are currently assigned to the " + currentCategory.getDisplayName() + " desk.",
                    "Return to Home",
                    navListener::onNavigateToHome
            ), BorderLayout.CENTER);
        } else {
            cardsGrid.setLayout(new GridLayout(0, 3, 18, 18));
            for (Article art : articles) {
                cardsGrid.add(new GridArticleCard(art, navListener, bookmarkService));
            }
        }

        cardsGrid.revalidate();
        cardsGrid.repaint();
    }

    @Override
    protected void onThemeUpdated(ThemeColors colors) {
        if (titleLabel != null) titleLabel.setForeground(colors.textPrimary);
        if (descLabel != null) descLabel.setForeground(colors.textSecondary);
        if (countLabel != null) countLabel.setForeground(colors.accent);
        populateArticles();
    }
}
