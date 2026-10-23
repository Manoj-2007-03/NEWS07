package com.newsroom.service;

import com.newsroom.data.SampleNewsDataProvider;
import com.newsroom.interfaces.NewsService;
import com.newsroom.model.Article;
import com.newsroom.model.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the NewsService interface.
 * Demonstrates Abstraction, Polymorphism, and Collections API usage.
 * Ready for Phase 2 data source delegation (e.g., REST API or SQLite).
 */
public class NewsServiceImpl implements NewsService {
    private final List<Article> articles;

    public NewsServiceImpl() {
        this(SampleNewsDataProvider.generateSampleArticles());
    }

    public NewsServiceImpl(List<Article> initialArticles) {
        this.articles = new ArrayList<>(Objects.requireNonNull(initialArticles, "Initial articles must not be null"));
    }

    @Override
    public List<Article> getAllArticles() {
        return Collections.unmodifiableList(new ArrayList<>(articles));
    }

    @Override
    public Article getFeaturedArticle() {
        return articles.stream()
                .filter(Article::isFeatured)
                .findFirst()
                .orElse(articles.isEmpty() ? null : articles.get(0));
    }

    @Override
    public List<Article> getTopStories(int limit) {
        return articles.stream()
                .filter(a -> !a.isFeatured())
                .sorted(Comparator.comparing(Article::getPublishedAt).reversed())
                .limit(Math.max(1, limit))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public List<Article> getLatestHeadlines(int limit) {
        return articles.stream()
                .sorted(Comparator.comparing(Article::getPublishedAt).reversed())
                .limit(Math.max(1, limit))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public List<Article> getTrendingArticles(int limit) {
        return articles.stream()
                .filter(Article::isTrending)
                .sorted(Comparator.comparing(Article::getPublishedAt).reversed())
                .limit(Math.max(1, limit))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public List<Article> getArticlesByCategory(Category category) {
        if (category == null || category == Category.HOME || category == Category.LATEST) {
            return getLatestHeadlines(articles.size());
        }
        return articles.stream()
                .filter(a -> a.getCategory() == category)
                .sorted(Comparator.comparing(Article::getPublishedAt).reversed())
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public Article getArticleById(String id) {
        if (id == null) return null;
        return articles.stream()
                .filter(a -> a.getId().equalsIgnoreCase(id.trim()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Article getNextArticle(Article current) {
        if (current == null || articles.isEmpty()) return null;
        int idx = articles.indexOf(current);
        if (idx >= 0 && idx < articles.size() - 1) {
            return articles.get(idx + 1);
        }
        return articles.get(0); // Wrap around to start
    }

    @Override
    public Article getPreviousArticle(Article current) {
        if (current == null || articles.isEmpty()) return null;
        int idx = articles.indexOf(current);
        if (idx > 0) {
            return articles.get(idx - 1);
        }
        return articles.get(articles.size() - 1); // Wrap around to end
    }
}
