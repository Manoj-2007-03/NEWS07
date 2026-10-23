package com.newsroom.service;

import com.newsroom.interfaces.BookmarkChangeListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.model.Article;
import com.newsroom.repository.ArticleRepository;
import com.newsroom.repository.BookmarkRepository;
import com.newsroom.repository.SqliteArticleRepository;
import com.newsroom.repository.SqliteBookmarkRepository;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SQLite Persistent Implementation of BookmarkService.
 * Keeps user bookmarks synchronized across sessions and notifies registered UI observers.
 */
public class DatabaseBookmarkServiceImpl implements BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final ArticleRepository articleRepository;
    private final List<BookmarkChangeListener> listeners = new ArrayList<>();

    public DatabaseBookmarkServiceImpl() {
        this(new SqliteBookmarkRepository(), new SqliteArticleRepository());
    }

    public DatabaseBookmarkServiceImpl(BookmarkRepository bookmarkRepository, ArticleRepository articleRepository) {
        this.bookmarkRepository = Objects.requireNonNull(bookmarkRepository, "BookmarkRepository must not be null");
        this.articleRepository = Objects.requireNonNull(articleRepository, "ArticleRepository must not be null");
    }

    @Override
    public synchronized void addBookmark(Article article) {
        if (article == null) return;
        article.setBookmarked(true);
        // Ensure article exists in database before foreign key reference
        articleRepository.save(article);
        bookmarkRepository.addBookmark(article.getId());
        notifyListeners(article, true);
    }

    @Override
    public synchronized void removeBookmark(String articleId) {
        if (articleId == null) return;
        bookmarkRepository.removeBookmark(articleId);
        articleRepository.findById(articleId).ifPresent(a -> {
            a.setBookmarked(false);
            notifyListeners(a, false);
        });
    }

    @Override
    public synchronized boolean toggleBookmark(Article article) {
        if (article == null) return false;
        boolean currentlyBookmarked = isBookmarked(article.getId());
        if (currentlyBookmarked) {
            removeBookmark(article.getId());
            return false;
        } else {
            addBookmark(article);
            return true;
        }
    }

    @Override
    public synchronized boolean isBookmarked(String articleId) {
        return bookmarkRepository.isBookmarked(articleId);
    }

    @Override
    public synchronized List<Article> getBookmarkedArticles() {
        return bookmarkRepository.getAllBookmarkedArticles();
    }

    @Override
    public synchronized int getBookmarkCount() {
        return bookmarkRepository.getBookmarkCount();
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

        Runnable task = () -> {
            for (BookmarkChangeListener l : copy) {
                try {
                    l.onBookmarkChanged(article, isBookmarked);
                } catch (Exception e) {
                    System.err.println("Error notifying bookmark listener: " + e.getMessage());
                }
            }
        };

        if (SwingUtilities.isEventDispatchThread()) task.run();
        else SwingUtilities.invokeLater(task);
    }
}
