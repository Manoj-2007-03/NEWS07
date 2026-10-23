package com.newsroom.repository;

import com.newsroom.model.Article;
import com.newsroom.model.Category;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface defining CRUD and query contracts for Article entities.
 * Demonstrates Abstraction and Repository Pattern.
 */
public interface ArticleRepository {
    void save(Article article);

    void saveAll(List<Article> articles);

    Optional<Article> findById(String id);

    Optional<Article> findByOriginalUrl(String originalUrl);

    List<Article> findAll();

    List<Article> findByCategory(Category category, int limit);

    Optional<Article> findFeatured();

    List<Article> findTopStories(int limit);

    List<Article> findLatestHeadlines(int limit);

    List<Article> findTrending(int limit);

    List<Article> search(String query);

    Optional<Article> findNext(Article current);

    Optional<Article> findPrevious(Article current);

    int count();

    void clearAll();
}
