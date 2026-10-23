package com.newsroom.repository;

import com.newsroom.model.Article;

import java.util.List;

/**
 * Repository interface for managing persistent saved bookmarks.
 */
public interface BookmarkRepository {
    void addBookmark(String articleId);

    void addBookmark(String articleId, String notes);

    void removeBookmark(String articleId);

    boolean isBookmarked(String articleId);

    List<Article> getAllBookmarkedArticles();

    int getBookmarkCount();

    void clearAll();
}
