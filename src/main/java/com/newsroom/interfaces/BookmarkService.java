package com.newsroom.interfaces;

import com.newsroom.model.Article;

import java.util.List;

/**
 * Contract for managing saved / bookmarked articles.
 * Separates bookmark state management from news presentation.
 */
public interface BookmarkService {
    /**
     * Adds an article to bookmarks.
     */
    void addBookmark(Article article);

    /**
     * Removes an article from bookmarks.
     */
    void removeBookmark(String articleId);

    /**
     * Toggles the bookmark status of an article.
     *
     * @return true if now bookmarked, false if unbookmarked
     */
    boolean toggleBookmark(Article article);

    /**
     * Checks if an article is currently bookmarked.
     */
    boolean isBookmarked(String articleId);

    /**
     * Returns an unmodifiable list of currently bookmarked articles.
     */
    List<Article> getBookmarkedArticles();

    /**
     * Returns the total count of saved articles.
     */
    int getBookmarkCount();

    /**
     * Registers an observer for bookmark modifications.
     */
    void addBookmarkListener(BookmarkChangeListener listener);

    /**
     * Unregisters an observer.
     */
    void removeBookmarkListener(BookmarkChangeListener listener);
}
