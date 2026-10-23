package com.newsroom.service;

import com.newsroom.interfaces.SearchService;
import com.newsroom.model.Article;
import com.newsroom.provider.NewsProvider;
import com.newsroom.repository.ArticleRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Hybrid Search Service Implementation.
 * Queries local SQLite database archive with multi-field search and supplements with
 * live network provider results when online.
 * Demonstrates Strategy Pattern and Deduplication.
 */
public class DatabaseSearchServiceImpl implements SearchService {
    private final ArticleRepository repository;
    private final NewsProvider newsProvider;

    public DatabaseSearchServiceImpl(ArticleRepository repository, NewsProvider newsProvider) {
        this.repository = Objects.requireNonNull(repository, "ArticleRepository must not be null");
        this.newsProvider = Objects.requireNonNull(newsProvider, "NewsProvider must not be null");
    }

    @Override
    public List<Article> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String q = query.trim();

        // 1. Local SQLite search (instant, indexed)
        List<Article> localResults = repository.search(q);

        // 2. Query live provider if available
        List<Article> liveResults = Collections.emptyList();
        if (newsProvider != null && newsProvider.isAvailable()) {
            try {
                liveResults = newsProvider.search(q);
                if (liveResults != null && !liveResults.isEmpty()) {
                    repository.saveAll(liveResults); // Cache live search results locally
                }
            } catch (Exception e) {
                System.err.println("Notice: Live search failed, using local database results: " + e.getMessage());
            }
        }

        // 3. Merge & Deduplicate
        List<Article> combined = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        if (liveResults != null) {
            for (Article a : liveResults) {
                if (seenIds.add(a.getId())) {
                    combined.add(a);
                }
            }
        }

        for (Article a : localResults) {
            if (seenIds.add(a.getId())) {
                combined.add(a);
            }
        }

        combined.sort(Comparator.comparing(Article::getPublishedAt).reversed());
        return Collections.unmodifiableList(combined);
    }
}
