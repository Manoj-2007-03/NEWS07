package com.newsroom.service;

import com.newsroom.data.SampleNewsDataProvider;
import com.newsroom.interfaces.NewsService;
import com.newsroom.interfaces.RefreshListener;
import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.model.ReaderSettings;
import com.newsroom.provider.CompositeNewsProvider;
import com.newsroom.provider.NewsProvider;
import com.newsroom.repository.ArticleRepository;
import com.newsroom.repository.SqliteArticleRepository;

import javax.swing.SwingUtilities;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Advanced Phase 2 Implementation of NewsService.
 * Coordinates SQLite persistent cache, real-time news retrieval (RSS/API),
 * offline resilience, and background refresh scheduling.
 * 
 * Demonstrates:
 * - Service / Repository Architecture.
 * - Concurrency & Background Thread Management.
 * - Offline Cache-First Strategy.
 * - Observer Pattern for UI refresh state.
 */
public class DatabaseNewsServiceImpl implements NewsService {
    private final ArticleRepository repository;
    private final NewsProvider newsProvider;
    private final ReaderSettings settings;

    private final ExecutorService networkExecutor;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> autoRefreshTask;

    private final List<RefreshListener> refreshListeners = new ArrayList<>();
    private boolean isRefreshing = false;
    private boolean isOffline = false;
    private LocalDateTime lastRefreshedAt;

    public DatabaseNewsServiceImpl() {
        this(new SqliteArticleRepository(), new CompositeNewsProvider(), new ReaderSettings());
    }

    public DatabaseNewsServiceImpl(ArticleRepository repository, NewsProvider newsProvider, ReaderSettings settings) {
        this.repository = Objects.requireNonNull(repository, "ArticleRepository must not be null");
        this.newsProvider = Objects.requireNonNull(newsProvider, "NewsProvider must not be null");
        this.settings = Objects.requireNonNull(settings, "ReaderSettings must not be null");

        this.networkExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "NewsService-NetworkWorker");
            t.setDaemon(true);
            return t;
        });

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NewsService-Scheduler");
            t.setDaemon(true);
            return t;
        });

        seedIfEmpty();
        configureAutoRefresh();
    }

    private void seedIfEmpty() {
        if (repository.count() == 0) {
            System.out.println("Seeding initial editorial archive into SQLite database...");
            List<Article> sampleArticles = SampleNewsDataProvider.generateSampleArticles();
            repository.saveAll(sampleArticles);
        } else {
            // Ensure sample articles are synchronized with latest images and metadata
            List<Article> sampleArticles = SampleNewsDataProvider.generateSampleArticles();
            repository.saveAll(sampleArticles);
        }
    }

    public void configureAutoRefresh() {
        if (autoRefreshTask != null && !autoRefreshTask.isDone()) {
            autoRefreshTask.cancel(false);
        }

        int intervalMins = settings.getRefreshIntervalMinutes();
        if (intervalMins > 0) {
            autoRefreshTask = scheduler.scheduleAtFixedRate(() -> {
                try {
                    refreshNews(Category.HOME);
                } catch (Exception e) {
                    System.err.println("Auto-refresh cycle error: " + e.getMessage());
                }
            }, intervalMins, intervalMins, TimeUnit.MINUTES);
        }
    }

    /**
     * Triggers asynchronous background refresh for a category desk.
     */
    public CompletableFuture<Boolean> refreshNews(Category category) {
        if (isRefreshing) {
            return CompletableFuture.completedFuture(false);
        }

        isRefreshing = true;
        notifyRefreshStarted();

        return CompletableFuture.supplyAsync(() -> {
            int newCount = 0;
            boolean success = false;
            String message;

            try {
                // Fetch from real provider (RSS or NewsAPI)
                List<Article> fetched = newsProvider.fetchHeadlines(category);
                if (fetched != null && !fetched.isEmpty()) {
                    repository.saveAll(fetched);
                    newCount = fetched.size();
                    success = true;
                    isOffline = false;
                    lastRefreshedAt = LocalDateTime.now();
                    settings.setLastRefreshedAt(lastRefreshedAt);
                    message = "Successfully synchronized " + newCount + " live stories from " + newsProvider.getDisplayName();
                } else {
                    message = "Provider returned 0 articles. Serving cached articles from SQLite database.";
                }
            } catch (Exception e) {
                isOffline = true;
                message = "Network unavailable (" + e.getMessage() + "). Serving cached articles from SQLite.";
                System.err.println("Notice: Network refresh failed. Operating in offline cache mode: " + e.getMessage());
            } finally {
                isRefreshing = false;
            }

            final boolean finalSuccess = success;
            final int finalCount = newCount;
            final String finalMsg = message;
            notifyRefreshCompleted(finalSuccess, finalCount, finalMsg);

            return finalSuccess;
        }, networkExecutor);
    }

    // --- NewsService Interface Implementations (SQLite-backed) ---

    @Override
    public List<Article> getAllArticles() {
        return repository.findAll();
    }

    @Override
    public Article getFeaturedArticle() {
        return repository.findFeatured().orElse(null);
    }

    @Override
    public List<Article> getTopStories(int limit) {
        return repository.findTopStories(limit);
    }

    @Override
    public List<Article> getLatestHeadlines(int limit) {
        return repository.findLatestHeadlines(limit);
    }

    @Override
    public List<Article> getTrendingArticles(int limit) {
        return repository.findTrending(limit);
    }

    @Override
    public List<Article> getArticlesByCategory(Category category) {
        return repository.findByCategory(category, 50);
    }

    @Override
    public Article getArticleById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Article getNextArticle(Article current) {
        return repository.findNext(current).orElse(null);
    }

    @Override
    public Article getPreviousArticle(Article current) {
        return repository.findPrevious(current).orElse(null);
    }

    // --- Observer and Status Methods ---

    public boolean isRefreshing() {
        return isRefreshing;
    }

    public boolean isOffline() {
        return isOffline;
    }

    public LocalDateTime getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public NewsProvider getNewsProvider() {
        return newsProvider;
    }

    public void addRefreshListener(RefreshListener listener) {
        if (listener != null && !refreshListeners.contains(listener)) {
            refreshListeners.add(listener);
        }
    }

    public void removeRefreshListener(RefreshListener listener) {
        refreshListeners.remove(listener);
    }

    private void notifyRefreshStarted() {
        Runnable r = () -> {
            for (RefreshListener l : new ArrayList<>(refreshListeners)) {
                l.onRefreshStarted();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    private void notifyRefreshCompleted(boolean success, int count, String msg) {
        Runnable r = () -> {
            for (RefreshListener l : new ArrayList<>(refreshListeners)) {
                l.onRefreshCompleted(success, count, msg);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    public void shutdown() {
        networkExecutor.shutdown();
        scheduler.shutdown();
    }
}
