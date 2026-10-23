package com.newsroom.interfaces;

import com.newsroom.model.Article;
import com.newsroom.model.Category;

/**
 * Navigation listener interface that decouples view panels from the main navigation controller.
 * Demonstrates Abstraction and loose coupling.
 */
public interface ArticleNavigationListener {
    /**
     * Navigates to the full article reader view for a specific article.
     */
    void onArticleSelected(Article article);

    /**
     * Navigates to a specific category section.
     */
    void onCategorySelected(Category category);

    /**
     * Navigates to the Home dashboard view.
     */
    void onNavigateToHome();

    /**
     * Navigates to the Saved / Bookmarks view.
     */
    void onNavigateToSaved();

    /**
     * Navigates to the Reading History view.
     */
    void onNavigateToHistory();

    /**
     * Navigates to the Settings view.
     */
    void onNavigateToSettings();

    /**
     * Performs and navigates to a search query result view.
     */
    void onNavigateToSearch(String query);
}
