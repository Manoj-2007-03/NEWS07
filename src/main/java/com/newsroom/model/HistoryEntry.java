package com.newsroom.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Encapsulates an entry in the user's reading history.
 * Tracks access timestamps and reading occurrences.
 */
public class HistoryEntry {
    private final Article article;
    private LocalDateTime accessTime;
    private int accessCount;

    public HistoryEntry(Article article) {
        this.article = Objects.requireNonNull(article, "Article must not be null");
        this.accessTime = LocalDateTime.now();
        this.accessCount = 1;
    }

    public Article getArticle() {
        return article;
    }

    public LocalDateTime getAccessTime() {
        return accessTime;
    }

    public int getAccessCount() {
        return accessCount;
    }

    public void updateAccessTime() {
        this.accessTime = LocalDateTime.now();
        this.accessCount++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistoryEntry that)) return false;
        return Objects.equals(article.getId(), that.article.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(article.getId());
    }
}
