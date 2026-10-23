package com.newsroom.provider;

import com.newsroom.config.AppConfig;
import com.newsroom.model.Article;
import com.newsroom.model.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Composite Provider orchestrating NewsAPI and RSS providers with automatic fallback.
 * Demonstrates Composite Pattern and Fault-Tolerant Redundancy.
 */
public class CompositeNewsProvider implements NewsProvider {
    private final AppConfig config;
    private final RssNewsProvider rssProvider;
    private final NewsApiProvider newsApiProvider;

    public CompositeNewsProvider() {
        this.config = AppConfig.getInstance();
        this.rssProvider = new RssNewsProvider();
        this.newsApiProvider = new NewsApiProvider();
    }

    public CompositeNewsProvider(AppConfig config, RssNewsProvider rssProvider, NewsApiProvider newsApiProvider) {
        this.config = config;
        this.rssProvider = rssProvider;
        this.newsApiProvider = newsApiProvider;
    }

    @Override
    public String getProviderId() {
        return "COMPOSITE";
    }

    @Override
    public String getDisplayName() {
        if (newsApiProvider.isAvailable()) {
            return "Hybrid Network (NewsAPI.org + Real-Time RSS)";
        }
        return "Editorial RSS Syndication Network";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<Article> fetchHeadlines(Category category) throws Exception {
        String strategy = config.getProviderStrategy();

        // 1. If explicit NewsAPI requested
        if ("NEWS_API".equalsIgnoreCase(strategy) && newsApiProvider.isAvailable()) {
            try {
                return newsApiProvider.fetchHeadlines(category);
            } catch (Exception e) {
                System.err.println("NewsAPI failed, falling back to RSS: " + e.getMessage());
            }
        }

        // 2. Composite strategy: try NewsAPI first if key available, else RSS
        if ("COMPOSITE".equalsIgnoreCase(strategy) && newsApiProvider.isAvailable()) {
            try {
                List<Article> apiResults = newsApiProvider.fetchHeadlines(category);
                if (apiResults != null && !apiResults.isEmpty()) {
                    return apiResults;
                }
            } catch (Exception e) {
                System.err.println("NewsAPI request failed: " + e.getMessage() + ". Engaging RSS fallback...");
            }
        }

        // 3. Fallback to RSS provider (always zero-key & operational)
        List<Article> rssResults = rssProvider.fetchHeadlines(category);
        return deduplicate(rssResults);
    }

    @Override
    public List<Article> search(String query) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        if (newsApiProvider.isAvailable()) {
            try {
                List<Article> results = newsApiProvider.search(query);
                if (results != null && !results.isEmpty()) {
                    return deduplicate(results);
                }
            } catch (Exception ignored) {}
        }

        return deduplicate(rssProvider.search(query));
    }

    private List<Article> deduplicate(List<Article> input) {
        if (input == null || input.isEmpty()) return Collections.emptyList();
        List<Article> unique = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (Article a : input) {
            String key = (a.getOriginalUrl() != null && !a.getOriginalUrl().isEmpty())
                    ? a.getOriginalUrl().toLowerCase()
                    : a.getTitle().toLowerCase();

            if (seenKeys.add(key)) {
                unique.add(a);
            }
        }
        return unique;
    }
}
