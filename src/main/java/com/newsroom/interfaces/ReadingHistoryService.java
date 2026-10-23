package com.newsroom.interfaces;

import com.newsroom.model.Article;
import com.newsroom.model.HistoryEntry;

import java.util.List;

/**
 * Contract for tracking read articles during the current session.
 * Deduplicates entries and prioritizes most recent access.
 */
public interface ReadingHistoryService {
    /**
     * Records or updates article access.
     */
    void recordAccess(Article article);

    /**
     * Returns the ordered reading history (most recent first).
     */
    List<HistoryEntry> getHistory();

    /**
     * Clears all reading history for the current session.
     */
    void clearHistory();

    /**
     * Returns total count of unique articles viewed in this session.
     */
    int getHistoryCount();
}
