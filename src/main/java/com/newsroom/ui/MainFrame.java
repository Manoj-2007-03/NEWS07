package com.newsroom.ui;

import com.newsroom.database.DatabaseManager;
import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.interfaces.NewsService;
import com.newsroom.interfaces.ReadingHistoryService;
import com.newsroom.interfaces.RefreshListener;
import com.newsroom.interfaces.SearchService;
import com.newsroom.interfaces.ThemeChangeListener;
import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.model.ReaderSettings;
import com.newsroom.provider.CompositeNewsProvider;
import com.newsroom.repository.ArticleRepository;
import com.newsroom.repository.BookmarkRepository;
import com.newsroom.repository.ReadingHistoryRepository;
import com.newsroom.repository.SettingsRepository;
import com.newsroom.repository.SqliteArticleRepository;
import com.newsroom.repository.SqliteBookmarkRepository;
import com.newsroom.repository.SqliteReadingHistoryRepository;
import com.newsroom.repository.SqliteSettingsRepository;
import com.newsroom.service.DatabaseBookmarkServiceImpl;
import com.newsroom.service.DatabaseHistoryServiceImpl;
import com.newsroom.service.DatabaseNewsServiceImpl;
import com.newsroom.service.DatabaseSearchServiceImpl;
import com.newsroom.theme.AppTheme;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * Main application window and navigation controller - Phase 2 Edition.
 * 
 * Demonstrates:
 * - Composition: Aggregates SidebarPanel, HeaderPanel, and primary View Panels.
 * - Abstraction: Implements ArticleNavigationListener, ThemeChangeListener, and RefreshListener.
 * - SQLite Persistence & Real-Time Ingestion Architecture.
 */
public class MainFrame extends JFrame implements ArticleNavigationListener, ThemeChangeListener, RefreshListener {
    public static final String VIEW_HOME = "VIEW_HOME";
    public static final String VIEW_CATEGORY = "VIEW_CATEGORY";
    public static final String VIEW_DETAIL = "VIEW_DETAIL";
    public static final String VIEW_SEARCH = "VIEW_SEARCH";
    public static final String VIEW_SAVED = "VIEW_SAVED";
    public static final String VIEW_HISTORY = "VIEW_HISTORY";
    public static final String VIEW_SETTINGS = "VIEW_SETTINGS";

    private final DatabaseNewsServiceImpl newsService;
    private final BookmarkService bookmarkService;
    private final ReadingHistoryService historyService;
    private final SearchService searchService;
    private final ReaderSettings settings;
    private final SettingsRepository settingsRepository;

    private SidebarPanel sidebarPanel;
    private HeaderPanel headerPanel;
    private CardLayout cardLayout;
    private JPanel cardContainer;

    private HomePanel homePanel;
    private CategoryPanel categoryPanel;
    private ArticleDetailPanel detailPanel;
    private SearchPanel searchPanel;
    private BookmarksPanel bookmarksPanel;
    private HistoryPanel historyPanel;
    private SettingsPanel settingsPanel;

    private String activeViewName = VIEW_HOME;

    public MainFrame() {
        super("NEWSROOM — Editorial Intelligence");

        // 1. Initialize SQLite Database & Repositories
        DatabaseManager dbManager = DatabaseManager.getInstance();
        ArticleRepository articleRepo = new SqliteArticleRepository(dbManager);
        BookmarkRepository bookmarkRepo = new SqliteBookmarkRepository(dbManager);
        ReadingHistoryRepository historyRepo = new SqliteReadingHistoryRepository(dbManager);
        this.settingsRepository = new SqliteSettingsRepository(dbManager);

        // 2. Load Persisted Settings
        this.settings = new ReaderSettings();
        settingsRepository.loadInto(settings);

        if (settings.isDarkMode()) {
            ThemeManager.getInstance().setTheme(AppTheme.DARK);
        }

        // 3. Initialize Domain Services (SQLite & Live Network Provider)
        CompositeNewsProvider provider = new CompositeNewsProvider();
        this.newsService = new DatabaseNewsServiceImpl(articleRepo, provider, settings);
        this.bookmarkService = new DatabaseBookmarkServiceImpl(bookmarkRepo, articleRepo);
        this.historyService = new DatabaseHistoryServiceImpl(historyRepo, articleRepo);
        this.searchService = new DatabaseSearchServiceImpl(articleRepo, provider);

        this.newsService.addRefreshListener(this);

        initWindow();
        initViews();

        ThemeManager.getInstance().addListener(this);

        // 4. Apply startup section preference
        Category startCat = settings.getDefaultStartupCategory();
        if (startCat == Category.HOME) {
            onNavigateToHome();
        } else {
            onCategorySelected(startCat);
        }

        // 5. Trigger initial asynchronous background refresh for live reporting
        this.newsService.refreshNews(Category.HOME);
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 840);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        ThemeColors colors = ThemeManager.getInstance().getColors();
        getContentPane().setBackground(colors.background);
    }

    private void initViews() {
        // 1. Sidebar Navigation
        sidebarPanel = new SidebarPanel(this, bookmarkService);
        add(sidebarPanel, BorderLayout.WEST);

        // 2. Central Workspace (Header on North, Views in Center)
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setOpaque(false);

        headerPanel = new HeaderPanel(this, settings, newsService);
        workspace.add(headerPanel, BorderLayout.NORTH);

        // 3. Card Container for Primary Panels
        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);

        homePanel = new HomePanel(newsService, bookmarkService, historyService, this, settings);
        categoryPanel = new CategoryPanel(newsService, bookmarkService, historyService, this, settings);
        detailPanel = new ArticleDetailPanel(newsService, bookmarkService, historyService, this, settings);
        searchPanel = new SearchPanel(newsService, bookmarkService, historyService, this, settings, searchService);
        bookmarksPanel = new BookmarksPanel(newsService, bookmarkService, historyService, this, settings);
        historyPanel = new HistoryPanel(newsService, bookmarkService, historyService, this, settings);
        settingsPanel = new SettingsPanel(newsService, bookmarkService, historyService, this, settings);

        cardContainer.add(homePanel, VIEW_HOME);
        cardContainer.add(categoryPanel, VIEW_CATEGORY);
        cardContainer.add(detailPanel, VIEW_DETAIL);
        cardContainer.add(searchPanel, VIEW_SEARCH);
        cardContainer.add(bookmarksPanel, VIEW_SAVED);
        cardContainer.add(historyPanel, VIEW_HISTORY);
        cardContainer.add(settingsPanel, VIEW_SETTINGS);

        workspace.add(cardContainer, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);
    }

    // --- ArticleNavigationListener implementation ---

    @Override
    public void onArticleSelected(Article article) {
        if (article == null) return;
        activeViewName = VIEW_DETAIL;
        detailPanel.setArticle(article);
        headerPanel.setBreadcrumb("Reading / " + article.getCategory().getDisplayName());
        cardLayout.show(cardContainer, VIEW_DETAIL);
    }

    @Override
    public void onCategorySelected(Category category) {
        if (category == null || category == Category.HOME) {
            onNavigateToHome();
            return;
        }
        activeViewName = VIEW_CATEGORY;
        categoryPanel.setCategory(category);
        sidebarPanel.setActiveItem(mapCategoryToNavItem(category));
        headerPanel.setBreadcrumb("Desks / " + category.getDisplayName());
        cardLayout.show(cardContainer, VIEW_CATEGORY);

        // Trigger background refresh for selected desk if online
        newsService.refreshNews(category);
    }

    @Override
    public void onNavigateToHome() {
        activeViewName = VIEW_HOME;
        sidebarPanel.setActiveItem(SidebarPanel.NavItem.HOME);
        headerPanel.setBreadcrumb("Home / Curated Editorial");
        homePanel.refreshView();
        cardLayout.show(cardContainer, VIEW_HOME);
    }

    @Override
    public void onNavigateToSaved() {
        activeViewName = VIEW_SAVED;
        sidebarPanel.setActiveItem(SidebarPanel.NavItem.SAVED);
        headerPanel.setBreadcrumb("Library / Saved Articles");
        bookmarksPanel.refreshView();
        cardLayout.show(cardContainer, VIEW_SAVED);
    }

    @Override
    public void onNavigateToHistory() {
        activeViewName = VIEW_HISTORY;
        sidebarPanel.setActiveItem(SidebarPanel.NavItem.HISTORY);
        headerPanel.setBreadcrumb("Library / Reading History");
        historyPanel.refreshView();
        cardLayout.show(cardContainer, VIEW_HISTORY);
    }

    @Override
    public void onNavigateToSettings() {
        activeViewName = VIEW_SETTINGS;
        sidebarPanel.setActiveItem(SidebarPanel.NavItem.SETTINGS);
        headerPanel.setBreadcrumb("Preferences / Settings");
        settingsPanel.refreshView();
        cardLayout.show(cardContainer, VIEW_SETTINGS);
    }

    @Override
    public void onNavigateToSearch(String query) {
        activeViewName = VIEW_SEARCH;
        headerPanel.setBreadcrumb("Archives / Search");
        headerPanel.setSearchFieldText(query);
        searchPanel.setSearchQuery(query);
        cardLayout.show(cardContainer, VIEW_SEARCH);
    }

    private SidebarPanel.NavItem mapCategoryToNavItem(Category category) {
        return switch (category) {
            case LATEST -> SidebarPanel.NavItem.LATEST;
            case WORLD -> SidebarPanel.NavItem.WORLD;
            case INDIA -> SidebarPanel.NavItem.INDIA;
            case TECHNOLOGY -> SidebarPanel.NavItem.TECHNOLOGY;
            case BUSINESS -> SidebarPanel.NavItem.BUSINESS;
            case SCIENCE -> SidebarPanel.NavItem.SCIENCE;
            case SPORTS -> SidebarPanel.NavItem.SPORTS;
            case ENTERTAINMENT -> SidebarPanel.NavItem.ENTERTAINMENT;
            default -> SidebarPanel.NavItem.HOME;
        };
    }

    // --- RefreshListener implementation ---

    @Override
    public void onRefreshStarted() {
        // Status handled by HeaderPanel
    }

    @Override
    public void onRefreshCompleted(boolean success, int newArticlesCount, String statusMessage) {
        SwingUtilities.invokeLater(() -> {
            if (VIEW_HOME.equals(activeViewName)) {
                homePanel.refreshView();
            } else if (VIEW_CATEGORY.equals(activeViewName)) {
                categoryPanel.refreshView();
            }
        });
    }

    @Override
    public void onThemeChanged(AppTheme theme) {
        ThemeColors colors = ThemeManager.getInstance().getColors();
        getContentPane().setBackground(colors.background);
        revalidate();
        repaint();
    }
}
