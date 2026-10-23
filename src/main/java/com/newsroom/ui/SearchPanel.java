package com.newsroom.ui;

import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.interfaces.NewsService;
import com.newsroom.interfaces.ReadingHistoryService;
import com.newsroom.interfaces.SearchService;
import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.model.ReaderSettings;
import com.newsroom.service.DatabaseSearchServiceImpl;
import com.newsroom.service.SearchServiceImpl;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.ui.components.CompactArticleCard;
import com.newsroom.ui.components.EmptyStatePanel;
import com.newsroom.ui.components.RoundedButton;
import com.newsroom.util.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Editorial Search & Filter Screen - Phase 2 Edition.
 * Features real-time hybrid search (SQLite database + live RSS/API endpoints),
 * multi-desk category filtering, sorting, and graceful empty states.
 */
public class SearchPanel extends BaseViewPanel {
    private final SearchService searchService;
    private JTextField searchInputField;
    private JComboBox<String> categoryFilterCombo;
    private JComboBox<String> sortCombo;
    private JLabel resultsCountLabel;
    private JPanel resultsList;
    private String currentQuery = "";

    public SearchPanel(NewsService newsService,
                       BookmarkService bookmarkService,
                       ReadingHistoryService historyService,
                       ArticleNavigationListener navListener,
                       ReaderSettings settings) {
        this(newsService, bookmarkService, historyService, navListener, settings, null);
    }

    public SearchPanel(NewsService newsService,
                       BookmarkService bookmarkService,
                       ReadingHistoryService historyService,
                       ArticleNavigationListener navListener,
                       ReaderSettings settings,
                       SearchService searchService) {
        super(newsService, bookmarkService, historyService, navListener, settings);
        this.searchService = (searchService != null) ? searchService : new SearchServiceImpl(newsService);
        initUI();
    }

    private void initUI() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(20, 28, 36, 28));

        // Header & Search Box
        container.add(createSearchHeader());
        container.add(Box.createVerticalStrut(20));

        // Results Container
        resultsList = new JPanel();
        resultsList.setLayout(new BoxLayout(resultsList, BoxLayout.Y_AXIS));
        resultsList.setOpaque(false);
        container.add(resultsList);

        add(wrapInScrollPane(container), BorderLayout.CENTER);
        refreshView();
    }

    private JPanel createSearchHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("Search Real-Time & Archived Reporting");
        title.setFont(UIUtils.getEditorialFont(Font.BOLD, 26));
        title.setForeground(ThemeManager.getInstance().getColors().textPrimary);
        header.add(title);
        header.add(Box.createVerticalStrut(4));

        JLabel desc = new JLabel("Search across local SQLite archives and live syndication newsrooms.");
        desc.setFont(UIUtils.getSansFont(Font.PLAIN, 13));
        desc.setForeground(ThemeManager.getInstance().getColors().textSecondary);
        header.add(desc);
        header.add(Box.createVerticalStrut(14));

        // Search Input Row
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setOpaque(false);
        searchBar.setMaximumSize(new Dimension(860, 42));

        ThemeColors colors = ThemeManager.getInstance().getColors();
        searchInputField = new JTextField();
        searchInputField.setFont(UIUtils.getSansFont(Font.PLAIN, 14));
        searchInputField.setBackground(colors.inputBackground);
        searchInputField.setForeground(colors.textPrimary);
        searchInputField.setCaretColor(colors.textPrimary);
        searchInputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colors.inputBorder, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // Real-time search listener
        searchInputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateSearch(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateSearch(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateSearch(); }
        });
        searchBar.add(searchInputField, BorderLayout.CENTER);

        // Action buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow.setOpaque(false);

        RoundedButton clearBtn = new RoundedButton("Clear", RoundedButton.Style.SECONDARY, 6);
        clearBtn.setPreferredSize(new Dimension(72, 38));
        clearBtn.addActionListener(e -> {
            searchInputField.setText("");
            currentQuery = "";
            executeSearch();
        });
        btnRow.add(clearBtn);

        searchBar.add(btnRow, BorderLayout.EAST);
        header.add(searchBar);
        header.add(Box.createVerticalStrut(12));

        // Filters Row (Category Filter & Sort Selector)
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);

        JLabel filterLbl = new JLabel("Desk Filter:");
        filterLbl.setFont(UIUtils.getSansFont(Font.BOLD, 12));
        filterLbl.setForeground(colors.textSecondary);
        filterRow.add(filterLbl);

        String[] catFilterOptions = {
                "All Desks", "World", "India", "Technology",
                "Business", "Science", "Sports", "Entertainment"
        };
        categoryFilterCombo = new JComboBox<>(catFilterOptions);
        categoryFilterCombo.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        categoryFilterCombo.setPreferredSize(new Dimension(140, 28));
        categoryFilterCombo.addActionListener(e -> executeSearch());
        filterRow.add(categoryFilterCombo);

        JLabel sortLbl = new JLabel("Sort:");
        sortLbl.setFont(UIUtils.getSansFont(Font.BOLD, 12));
        sortLbl.setForeground(colors.textSecondary);
        filterRow.add(sortLbl);

        String[] sortOptions = {"Newest First", "Reading Time (Shortest)", "Reading Time (Longest)"};
        sortCombo = new JComboBox<>(sortOptions);
        sortCombo.setFont(UIUtils.getSansFont(Font.PLAIN, 12));
        sortCombo.setPreferredSize(new Dimension(170, 28));
        sortCombo.addActionListener(e -> executeSearch());
        filterRow.add(sortCombo);

        header.add(filterRow);
        header.add(Box.createVerticalStrut(12));

        // Results count label
        resultsCountLabel = new JLabel("Type a keyword to begin searching.");
        resultsCountLabel.setFont(UIUtils.getSansFont(Font.BOLD, 12));
        resultsCountLabel.setForeground(colors.textMuted);
        header.add(resultsCountLabel);

        return header;
    }

    private void updateSearch() {
        this.currentQuery = searchInputField.getText().trim();
        executeSearch();
    }

    public void setSearchQuery(String query) {
        this.currentQuery = query != null ? query.trim() : "";
        if (searchInputField != null) {
            searchInputField.setText(this.currentQuery);
        }
        executeSearch();
    }

    private void executeSearch() {
        if (resultsList == null) return;
        resultsList.removeAll();
        ThemeColors colors = ThemeManager.getInstance().getColors();

        List<Article> matches;
        if (currentQuery.isEmpty()) {
            matches = new ArrayList<>(newsService.getAllArticles());
            resultsCountLabel.setText("Showing all " + matches.size() + " recent reporting entries. Type keywords to query live.");
        } else {
            matches = new ArrayList<>(searchService.search(currentQuery));
        }

        // Apply Category Desk Filter
        String selectedDesk = (categoryFilterCombo != null) ? (String) categoryFilterCombo.getSelectedItem() : "All Desks";
        if (selectedDesk != null && !"All Desks".equals(selectedDesk)) {
            matches.removeIf(a -> !a.getCategory().getDisplayName().equalsIgnoreCase(selectedDesk));
        }

        // Apply Sorting
        String sort = (sortCombo != null) ? (String) sortCombo.getSelectedItem() : "Newest First";
        if ("Reading Time (Shortest)".equals(sort)) {
            matches.sort(Comparator.comparingInt(Article::getReadingTimeMinutes));
        } else if ("Reading Time (Longest)".equals(sort)) {
            matches.sort(Comparator.comparingInt(Article::getReadingTimeMinutes).reversed());
        } else {
            matches.sort(Comparator.comparing(Article::getPublishedAt).reversed());
        }

        if (matches.isEmpty()) {
            resultsCountLabel.setText("No articles found matching '" + currentQuery + "'.");
            resultsList.add(new EmptyStatePanel(
                    "⌕",
                    "No Matching Reporting Found",
                    "We couldn't find any stories matching '" + currentQuery + "'. Try adjusting your desk filter or searching for broader terms.",
                    "Clear Search & Filters",
                    () -> {
                        if (categoryFilterCombo != null) categoryFilterCombo.setSelectedIndex(0);
                        setSearchQuery("");
                    }
            ));
        } else {
            if (!currentQuery.isEmpty()) {
                resultsCountLabel.setText("Found " + matches.size() + " matching stories for '" + currentQuery + "':");
            }
            for (Article art : matches) {
                resultsList.add(new CompactArticleCard(art, navListener, bookmarkService));
                resultsList.add(Box.createVerticalStrut(8));
            }
        }

        resultsList.revalidate();
        resultsList.repaint();
    }

    @Override
    public void refreshView() {
        executeSearch();
    }

    @Override
    protected void onThemeUpdated(ThemeColors colors) {
        if (searchInputField != null) {
            searchInputField.setBackground(colors.inputBackground);
            searchInputField.setForeground(colors.textPrimary);
            searchInputField.setCaretColor(colors.textPrimary);
            searchInputField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(colors.inputBorder, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
        }
        if (resultsCountLabel != null) {
            resultsCountLabel.setForeground(colors.textMuted);
        }
        executeSearch();
    }
}
