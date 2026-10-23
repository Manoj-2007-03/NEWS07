package com.newsroom.service;

import com.newsroom.interfaces.BookmarkChangeListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.model.Article;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory implementation of BookmarkService.
 * Maintains saved articles with observer notification for UI state synchronization.
 * Demonstrates Observer pattern and Encapsulation.
 */
public class BookmarkServiceImpl implements BookmarkService {
    private final Map<String, Article> bookmarkedArticles = new LinkedHashMap<>();
    private final List<BookmarkChangeListener> listeners = new ArrayList<>();

    @Override
    public synchronized void addBookmark(Article article) {
        if (article == null) return;
        article.setBookmarked(true);
        bookmarkedArticles.put(article.getId(), article);
        notifyListeners(article, true);
    }

    @Override
    public synchronized void removeBookmark(String articleId) {
        if (articleId == null) return;
        Article removed = bookmarkedArticles.remove(articleId);
        if (removed != null) {
            removed.setBookmarked(false);
            notifyListeners(removed, false);
        }
    }

    @Override
    public synchronized boolean toggleBookmark(Article article) {
        if (article == null) return false;
        if (isBookmarked(article.getId())) {
            removeBookmark(article.getId());
            return false;
        } else {
            addBookmark(article);
            return true;
        }
    }

    @Override
    public synchronized boolean isBookmarked(String articleId) {
        return articleId != null && bookmarkedArticles.containsKey(articleId);
    }

    @Override
    public synchronized List<Article> getBookmarkedArticles() {
        return Collections.unmodifiableList(new ArrayList<>(bookmarkedArticles.values()));
    }

    @Override
    public synchronized int getBookmarkCount() {
        return bookmarkedArticles.size();
    }

    @Override
    public synchronized void addBookmarkListener(BookmarkChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public synchronized void removeBookmarkListener(BookmarkChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Article article, boolean isBookmarked) {
        List<BookmarkChangeListener> copy;
        synchronized (this) {
            copy = new ArrayList<>(listeners);
        }

        Runnable notifyTask = () -> {
            for (BookmarkChangeListener listener : copy) {
                try {
                    listener.onBookmarkChanged(article, isBookmarked);
                } catch (Exception e) {
                    System.err.println("Error notifying bookmark listener: " + e.getMessage());
                }
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            notifyTask.run();
        } else {
            SwingUtilities.invokeLater(notifyTask);
        }
    }
}
