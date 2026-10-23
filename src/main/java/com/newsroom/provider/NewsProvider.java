package com.newsroom.provider;

import com.newsroom.model.Article;
import com.newsroom.model.Category;

import java.util.List;

/**
 * Service contract for pluggable news retrieval providers.
 * Demonstrates Abstraction and Strategy Pattern.
 */
public interface NewsProvider {
    /**
     * Unique identifier for the provider (e.g. "RSS", "NEWS_API").
     */
    String getProviderId();

    /**
     * Human-readable provider brand name.
     */
    String getDisplayName();

    /**
     * Checks if this provider is configured and operational.
     */
    boolean isAvailable();

    /**
     * Fetches current headlines for the designated category desk.
     */
    List<Article> fetchHeadlines(Category category) throws Exception;

    /**
     * Executes a keyword query across remote news streams.
     */
    List<Article> search(String query) throws Exception;
}
