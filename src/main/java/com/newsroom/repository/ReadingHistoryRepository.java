package com.newsroom.repository;

import com.newsroom.model.HistoryEntry;

import java.util.List;

/**
 * Repository interface for managing persistent session and historical reading logs.
 */
public interface ReadingHistoryRepository {
    void recordAccess(String articleId);

    List<HistoryEntry> getHistory();

    void clearHistory();

    int getHistoryCount();
}
