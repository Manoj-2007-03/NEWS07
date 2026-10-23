package com.newsroom.service;

import com.newsroom.interfaces.ReadingHistoryService;
import com.newsroom.model.Article;
import com.newsroom.model.HistoryEntry;
import com.newsroom.repository.ArticleRepository;
import com.newsroom.repository.ReadingHistoryRepository;
import com.newsroom.repository.SqliteArticleRepository;
import com.newsroom.repository.SqliteReadingHistoryRepository;

import java.util.List;
import java.util.Objects;

/**
 * SQLite Persistent Implementation of ReadingHistoryService.
 * Records, deduplicates, and tracks session/historical article views.
 */
public class DatabaseHistoryServiceImpl implements ReadingHistoryService {
    private final ReadingHistoryRepository historyRepository;
    private final ArticleRepository articleRepository;

    public DatabaseHistoryServiceImpl() {
        this(new SqliteReadingHistoryRepository(), new SqliteArticleRepository());
    }

    public DatabaseHistoryServiceImpl(ReadingHistoryRepository historyRepository, ArticleRepository articleRepository) {
        this.historyRepository = Objects.requireNonNull(historyRepository, "ReadingHistoryRepository must not be null");
        this.articleRepository = Objects.requireNonNull(articleRepository, "ArticleRepository must not be null");
    }

    @Override
    public synchronized void recordAccess(Article article) {
        if (article == null) return;
        // Ensure article exists in SQLite
        articleRepository.save(article);
        historyRepository.recordAccess(article.getId());
    }

    @Override
    public synchronized List<HistoryEntry> getHistory() {
        return historyRepository.getHistory();
    }

    @Override
    public synchronized void clearHistory() {
        historyRepository.clearHistory();
    }

    @Override
    public synchronized int getHistoryCount() {
        return historyRepository.getHistoryCount();
    }
}
