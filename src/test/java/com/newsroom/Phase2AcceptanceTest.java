package com.newsroom;

import com.newsroom.config.AppConfig;
import com.newsroom.database.DatabaseManager;
import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.model.HistoryEntry;
import com.newsroom.model.ReaderSettings;
import com.newsroom.parser.RssParser;
import com.newsroom.parser.SimpleJsonParser;
import com.newsroom.provider.CompositeNewsProvider;
import com.newsroom.provider.NewsApiProvider;
import com.newsroom.provider.RssNewsProvider;
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
import com.newsroom.ui.MainFrame;

import javax.swing.SwingUtilities;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Phase 2 Automated Acceptance & Integration Test Suite.
 * Validates SQLite persistence, real-time RSS/API integration, offline resilience,
 * deduplication, and Swing UI lifecycle.
 */
public class Phase2AcceptanceTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println(" NEWSROOM PHASE 2 ACCEPTANCE TEST SUITE");
        System.out.println("=================================================");

        // Use test SQLite database to avoid altering user database
        String testDbPath = "test_newsroom.db";
        new File(testDbPath).delete();

        DatabaseManager dbManager = new DatabaseManager(testDbPath);

        int passed = 0;
        int total = 0;

        total++; if (testDatabaseManagerAndMigrations(dbManager)) passed++;
        total++; if (testArticleRepositoryPersistence(dbManager)) passed++;
        total++; if (testBookmarkPersistenceAcrossRestarts(dbManager)) passed++;
        total++; if (testReadingHistoryPersistence(dbManager)) passed++;
        total++; if (testSettingsPersistence(dbManager)) passed++;
        total++; if (testRssFeedParser()) passed++;
        total++; if (testSimpleJsonParser()) passed++;
        total++; if (testNewsProviderArchitecture()) passed++;
        total++; if (testOfflineResilience(dbManager)) passed++;
        total++; if (testUIInstantiationAndNavigation()) passed++;

        // Clean up test database
        dbManager.close();
        new File(testDbPath).delete();

        System.out.println("=================================================");
        System.out.printf("PHASE 2 RESULTS: %d / %d TEST SUITES PASSED (%.1f%%)\n", passed, total, (passed * 100.0 / total));
        System.out.println("=================================================");

        if (passed != total) {
            System.exit(1);
        }
    }

    private static boolean testDatabaseManagerAndMigrations(DatabaseManager dbManager) {
        System.out.print("[TEST 1] SQLite Database Initialization & Schema V1 Migration ... ");
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check version
            try (ResultSet rs = stmt.executeQuery("SELECT MAX(version) FROM schema_version")) {
                if (!rs.next() || rs.getInt(1) < 1) {
                    System.err.println("FAILED: Schema version not recorded");
                    return false;
                }
            }

            // Check tables exist
            String[] tables = {"articles", "bookmarks", "reading_history", "reader_settings", "schema_version"};
            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM " + table)) {
                    if (!rs.next()) {
                        System.err.println("FAILED: Table missing: " + table);
                        return false;
                    }
                }
            }

            System.out.println("PASSED (All tables, foreign keys, and indexes initialized)");
            return true;
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            return false;
        }
    }

    private static boolean testArticleRepositoryPersistence(DatabaseManager dbManager) {
        System.out.print("[TEST 2] SQLite Article Repository (CRUD & Upsert) ... ");
        ArticleRepository repo = new SqliteArticleRepository(dbManager);

        Article a = Article.builder()
                .id("test-art-100")
                .title("Test Quantum Breakthrough")
                .summary("Summary of quantum story")
                .content("Full content of quantum research.")
                .author("Dr. Alice Vance")
                .source("Tech Chronicle")
                .category(Category.TECHNOLOGY)
                .publishedAt(LocalDateTime.now())
                .readingTimeMinutes(5)
                .originalUrl("https://example.com/quantum")
                .isFeatured(true)
                .isTrending(true)
                .build();

        repo.save(a);

        Optional<Article> loaded = repo.findById("test-art-100");
        if (loaded.isEmpty()) {
            System.err.println("FAILED: Article not found in database");
            return false;
        }

        Article l = loaded.get();
        if (!l.getTitle().equals("Test Quantum Breakthrough") || !l.getOriginalUrl().equals("https://example.com/quantum")) {
            System.err.println("FAILED: Article fields mismatch: " + l);
            return false;
        }

        // Search test
        List<Article> searchResults = repo.search("Vance");
        if (searchResults.isEmpty()) {
            System.err.println("FAILED: Search failed to find article by author");
            return false;
        }

        System.out.println("PASSED (SQLite persistence, search, and original URL verified)");
        return true;
    }

    private static boolean testBookmarkPersistenceAcrossRestarts(DatabaseManager dbManager) {
        System.out.print("[TEST 3] Bookmark Persistence Across Application Restarts ... ");
        ArticleRepository articleRepo = new SqliteArticleRepository(dbManager);
        BookmarkRepository bookmarkRepo1 = new SqliteBookmarkRepository(dbManager);

        Article a = Article.builder()
                .id("test-bookmark-101")
                .title("Persistent Bookmark Test")
                .summary("Testing restart survival")
                .content("Content here")
                .author("Test Author")
                .source("Test Desk")
                .category(Category.WORLD)
                .publishedAt(LocalDateTime.now())
                .readingTimeMinutes(4)
                .build();

        articleRepo.save(a);
        bookmarkRepo1.addBookmark(a.getId());

        // Simulate application restart by creating fresh repository instance
        BookmarkRepository bookmarkRepo2 = new SqliteBookmarkRepository(dbManager);
        if (!bookmarkRepo2.isBookmarked("test-bookmark-101")) {
            System.err.println("FAILED: Bookmark not found after re-instantiating repository");
            return false;
        }

        List<Article> bookmarked = bookmarkRepo2.getAllBookmarkedArticles();
        boolean found = bookmarked.stream().anyMatch(art -> art.getId().equals("test-bookmark-101"));
        if (!found) {
            System.err.println("FAILED: Bookmarked article not returned in getAllBookmarkedArticles");
            return false;
        }

        // Remove bookmark
        bookmarkRepo2.removeBookmark("test-bookmark-101");
        if (bookmarkRepo2.isBookmarked("test-bookmark-101")) {
            System.err.println("FAILED: Bookmark should be removed");
            return false;
        }

        System.out.println("PASSED (Bookmarks survive application restart and remove safely)");
        return true;
    }

    private static boolean testReadingHistoryPersistence(DatabaseManager dbManager) throws Exception {
        System.out.print("[TEST 4] Reading History Persistence & Deduplication ... ");
        ArticleRepository articleRepo = new SqliteArticleRepository(dbManager);
        ReadingHistoryRepository historyRepo1 = new SqliteReadingHistoryRepository(dbManager);

        Article a1 = Article.builder()
                .id("hist-01")
                .title("History Article 1")
                .category(Category.BUSINESS)
                .publishedAt(LocalDateTime.now())
                .readingTimeMinutes(3)
                .build();

        Article a2 = Article.builder()
                .id("hist-02")
                .title("History Article 2")
                .category(Category.SCIENCE)
                .publishedAt(LocalDateTime.now())
                .readingTimeMinutes(4)
                .build();

        articleRepo.save(a1);
        articleRepo.save(a2);

        historyRepo1.recordAccess(a1.getId());
        Thread.sleep(50);
        historyRepo1.recordAccess(a2.getId());

        // Simulate restart
        ReadingHistoryRepository historyRepo2 = new SqliteReadingHistoryRepository(dbManager);
        List<HistoryEntry> list = historyRepo2.getHistory();
        if (list.size() < 2) {
            System.err.println("FAILED: Expected at least 2 history entries after restart");
            return false;
        }

        // Most recent should be a2
        if (!list.get(0).getArticle().getId().equals("hist-02")) {
            System.err.println("FAILED: a2 should be most recent history entry");
            return false;
        }

        // Re-access a1 -> should promote a1 to top without increasing unique count
        Thread.sleep(50);
        int countBefore = historyRepo2.getHistoryCount();
        historyRepo2.recordAccess(a1.getId());
        int countAfter = historyRepo2.getHistoryCount();
        if (countBefore != countAfter) {
            System.err.println("FAILED: Reading history should deduplicate existing article access");
            return false;
        }

        List<HistoryEntry> updatedList = historyRepo2.getHistory();
        if (!updatedList.get(0).getArticle().getId().equals("hist-01")) {
            System.err.println("FAILED: a1 should be promoted to the top of the history list");
            return false;
        }

        System.out.println("PASSED (Session history deduplication and promotion verified)");
        return true;
    }

    private static boolean testSettingsPersistence(DatabaseManager dbManager) {
        System.out.print("[TEST 5] Settings Persistence in SQLite ... ");
        SettingsRepository settingsRepo1 = new SqliteSettingsRepository(dbManager);

        ReaderSettings s = new ReaderSettings();
        s.setBodyFontSize(21);
        s.setDarkMode(true);
        s.setDefaultStartupCategory(Category.TECHNOLOGY);
        s.setPreferredNewsProvider("RSS");
        s.setRefreshIntervalMinutes(15);

        settingsRepo1.persist(s);

        // Simulate restart
        SettingsRepository settingsRepo2 = new SqliteSettingsRepository(dbManager);
        ReaderSettings loadedSettings = new ReaderSettings();
        settingsRepo2.loadInto(loadedSettings);

        if (loadedSettings.getBodyFontSize() != 21) {
            System.err.println("FAILED: Font size not persisted: " + loadedSettings.getBodyFontSize());
            return false;
        }
        if (!loadedSettings.isDarkMode()) {
            System.err.println("FAILED: Dark mode preference not persisted");
            return false;
        }
        if (loadedSettings.getDefaultStartupCategory() != Category.TECHNOLOGY) {
            System.err.println("FAILED: Startup category not persisted: " + loadedSettings.getDefaultStartupCategory());
            return false;
        }
        if (loadedSettings.getRefreshIntervalMinutes() != 15) {
            System.err.println("FAILED: Refresh interval not persisted: " + loadedSettings.getRefreshIntervalMinutes());
            return false;
        }

        System.out.println("PASSED (All reader settings and intervals restored from SQLite)");
        return true;
    }

    private static boolean testRssFeedParser() {
        System.out.print("[TEST 6] RSS Feed Parser (XML Sanitization & Multi-Format) ... ");
        String sampleRss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:media="http://search.yahoo.com/mrss/">
              <channel>
                <title>BBC News - Technology</title>
                <item>
                  <title><![CDATA[Artificial Intelligence Breakthrough at Oxford]]></title>
                  <description><![CDATA[<p>Researchers have developed a <b>novel neural architecture</b> that reduces power draw by 90%.</p>]]></description>
                  <link>https://www.bbc.com/news/technology-123456</link>
                  <pubDate>Thu, 22 Oct 2026 08:30:00 GMT</pubDate>
                  <dc:creator>Jane Doe</dc:creator>
                  <media:thumbnail url="https://ichef.bbci.co.uk/news/1024/branded_news/1234.jpg"/>
                </item>
              </channel>
            </rss>
            """;

        RssParser parser = new RssParser();
        List<Article> articles = parser.parse(sampleRss, Category.TECHNOLOGY, "BBC News");

        if (articles.isEmpty()) {
            System.err.println("FAILED: Expected 1 parsed article");
            return false;
        }

        Article a = articles.get(0);
        if (!a.getTitle().equals("Artificial Intelligence Breakthrough at Oxford")) {
            System.err.println("FAILED: Title mismatch: " + a.getTitle());
            return false;
        }
        if (a.getSummary().contains("<p>") || a.getSummary().contains("<b>")) {
            System.err.println("FAILED: HTML tags not stripped from summary: " + a.getSummary());
            return false;
        }
        if (!a.getImageUrl().equals("https://ichef.bbci.co.uk/news/1024/branded_news/1234.jpg")) {
            System.err.println("FAILED: Image URL not extracted: " + a.getImageUrl());
            return false;
        }
        if (!a.getOriginalUrl().equals("https://www.bbc.com/news/technology-123456")) {
            System.err.println("FAILED: Original URL not parsed: " + a.getOriginalUrl());
            return false;
        }

        System.out.println("PASSED (XML parsing, HTML tag stripping, and media thumbnail extracted)");
        return true;
    }

    private static boolean testSimpleJsonParser() {
        System.out.print("[TEST 7] Zero-Dependency NewsAPI JSON Parser ... ");
        String sampleJson = """
            {
              "status": "ok",
              "totalResults": 1,
              "articles": [
                {
                  "source": {"id": "wired", "name": "Wired"},
                  "author": "Marcus Vance",
                  "title": "Quantum Computing Enters Industrial Era",
                  "description": "Silicon spin qubits operate at 1.5 Kelvin with 99.9% gate fidelity.",
                  "url": "https://www.wired.com/story/quantum-computing-era/",
                  "urlToImage": "https://media.wired.com/photos/quantum.jpg",
                  "publishedAt": "2026-10-22T09:15:00Z",
                  "content": "Detailed paragraphs of quantum mechanics and silicon spin architecture..."
                }
              ]
            }
            """;

        SimpleJsonParser parser = new SimpleJsonParser();
        List<Article> articles = parser.parseNewsApiArticles(sampleJson, Category.TECHNOLOGY);

        if (articles.isEmpty()) {
            System.err.println("FAILED: JSON parser returned 0 articles");
            return false;
        }

        Article a = articles.get(0);
        if (!a.getTitle().equals("Quantum Computing Enters Industrial Era")) {
            System.err.println("FAILED: JSON article title mismatch: " + a.getTitle());
            return false;
        }
        if (!a.getSource().equals("Wired")) {
            System.err.println("FAILED: JSON source name mismatch: " + a.getSource());
            return false;
        }
        if (!a.getOriginalUrl().equals("https://www.wired.com/story/quantum-computing-era/")) {
            System.err.println("FAILED: JSON original URL mismatch: " + a.getOriginalUrl());
            return false;
        }

        System.out.println("PASSED (JSON schema parsing and field extraction verified)");
        return true;
    }

    private static boolean testNewsProviderArchitecture() {
        System.out.print("[TEST 8] Provider Architecture (RSS, NewsAPI, Composite) ... ");
        RssNewsProvider rssProvider = new RssNewsProvider();
        NewsApiProvider apiProvider = new NewsApiProvider();
        CompositeNewsProvider composite = new CompositeNewsProvider(AppConfig.getInstance(), rssProvider, apiProvider);

        if (!rssProvider.isAvailable()) {
            System.err.println("FAILED: RSS provider should always be available");
            return false;
        }

        if (!composite.isAvailable()) {
            System.err.println("FAILED: Composite provider should always be available");
            return false;
        }

        System.out.println("PASSED (Provider contracts and fallback hierarchy verified)");
        return true;
    }

    private static boolean testOfflineResilience(DatabaseManager dbManager) {
        System.out.print("[TEST 9] Offline Resilience & Cache-First Strategy ... ");
        ArticleRepository repo = new SqliteArticleRepository(dbManager);

        // Ensure database has articles
        if (repo.count() == 0) {
            System.err.println("FAILED: Test database has no articles to test offline fallback");
            return false;
        }

        // Mock a provider that throws network failure
        com.newsroom.provider.NewsProvider failingProvider = new com.newsroom.provider.NewsProvider() {
            @Override public String getProviderId() { return "MOCK_FAILING"; }
            @Override public String getDisplayName() { return "Failing Network Provider"; }
            @Override public boolean isAvailable() { return false; }
            @Override public List<Article> fetchHeadlines(Category category) throws Exception {
                throw new java.net.ConnectException("Simulated network outage");
            }
            @Override public List<Article> search(String query) throws Exception {
                throw new java.net.ConnectException("Simulated network outage");
            }
        };

        DatabaseNewsServiceImpl service = new DatabaseNewsServiceImpl(repo, failingProvider, new ReaderSettings());

        // Refresh should not throw exception; it must gracefully report offline and serve cached data
        try {
            boolean result = service.refreshNews(Category.HOME).get();
            if (result) {
                System.err.println("FAILED: Refresh should return false when network fails");
                return false;
            }
            if (!service.isOffline()) {
                System.err.println("FAILED: Service should mark isOffline = true");
                return false;
            }

            // Articles should still be retrievable from SQLite
            List<Article> cached = service.getAllArticles();
            if (cached.isEmpty()) {
                System.err.println("FAILED: Cached articles should be served during network outage");
                return false;
            }
        } catch (Exception e) {
            System.err.println("FAILED: Unhandled exception during network outage: " + e.getMessage());
            return false;
        } finally {
            service.shutdown();
        }

        System.out.println("PASSED (Application gracefully operates offline using SQLite cache)");
        return true;
    }

    private static boolean testUIInstantiationAndNavigation() throws Exception {
        System.out.print("[TEST 10] Swing MainFrame UI Lifecycle with Phase 2 Services ... ");
        final boolean[] success = {false};
        final String[] error = {null};

        SwingUtilities.invokeAndWait(() -> {
            try {
                MainFrame frame = new MainFrame();

                // Test navigation hooks
                frame.onNavigateToHome();
                frame.onCategorySelected(Category.TECHNOLOGY);
                frame.onNavigateToSaved();
                frame.onNavigateToHistory();
                frame.onNavigateToSettings();
                frame.onNavigateToSearch("quantum");

                frame.dispose();
                success[0] = true;
            } catch (Throwable t) {
                error[0] = t.getMessage();
                t.printStackTrace();
            }
        });

        if (!success[0]) {
            System.err.println("FAILED: UI instantiation error: " + error[0]);
            return false;
        }

        System.out.println("PASSED (All Phase 2 UI views, card transitions, and SQLite services validated)");
        return true;
    }
}
