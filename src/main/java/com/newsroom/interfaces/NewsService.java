package com.newsroom.interfaces;

import com.newsroom.model.Article;
import com.newsroom.model.Category;

import java.util.List;

/**
 * Service contract for news data retrieval and curation.
 * Demonstrates Abstraction: UI relies solely on this interface, allowing Phase 2
 * to swap in a live REST API implementation without changing UI components.
 */
public interface NewsService {
    /**
     * Retrieves all available articles in the current repository.
     */
    List<Article> getAllArticles();

    /**
     * Retrieves the primary hero / featured article for editorial highlight.
     */
    Article getFeaturedArticle();

    /**
     * Retrieves top stories for homepage editorial grid.
     */
    List<Article> getTopStories(int limit);

    /**
     * Retrieves chronological latest headlines.
     */
    List<Article> getLatestHeadlines(int limit);

    /**
     * Retrieves trending articles ranked by readership velocity.
     */
    List<Article> getTrendingArticles(int limit);

    /**
     * Filters articles by editorial category.
     */
    List<Article> getArticlesByCategory(Category category);

    /**
     * Finds an article by its unique identifier.
     */
    Article getArticleById(String id);

    /**
     * Returns the chronologically or sequentially next article relative to the current one.
     */
    Article getNextArticle(Article current);

    /**
     * Returns the chronologically or sequentially previous article relative to the current one.
     */
    Article getPreviousArticle(Article current);
}
