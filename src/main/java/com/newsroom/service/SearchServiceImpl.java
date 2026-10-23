package com.newsroom.service;

import com.newsroom.interfaces.NewsService;
import com.newsroom.interfaces.SearchService;
import com.newsroom.model.Article;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of SearchService leveraging the Searchable interface.
 * Demonstrates Polymorphism and Dependency Injection through NewsService.
 */
public class SearchServiceImpl implements SearchService {
    private final NewsService newsService;

    public SearchServiceImpl(NewsService newsService) {
        this.newsService = Objects.requireNonNull(newsService, "NewsService must not be null");
    }

    @Override
    public List<Article> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String trimmed = query.trim();
        return newsService.getAllArticles().stream()
                .filter(article -> article.matches(trimmed))
                .sorted(Comparator.comparing(Article::getPublishedAt).reversed())
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }
}
