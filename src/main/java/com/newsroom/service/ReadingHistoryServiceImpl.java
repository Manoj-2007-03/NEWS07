package com.newsroom.service;

import com.newsroom.interfaces.ReadingHistoryService;
import com.newsroom.model.Article;
import com.newsroom.model.HistoryEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory implementation of ReadingHistoryService.
 * Ensures deduplicated history tracking where revisiting an article promotes it to the top.
 * Demonstrates Collections API manipulation and encapsulation.
 */
public class ReadingHistoryServiceImpl implements ReadingHistoryService {
    private final LinkedList<HistoryEntry> history = new LinkedList<>();

    @Override
    public synchronized void recordAccess(Article article) {
        if (article == null) return;

        // Search for existing entry to update timestamp and access count
        Optional<HistoryEntry> existing = history.stream()
                .filter(e -> Objects.equals(e.getArticle().getId(), article.getId()))
                .findFirst();

        if (existing.isPresent()) {
            HistoryEntry entry = existing.get();
            entry.updateAccessTime();
            // Move to top of the history stack
            history.remove(entry);
            history.addFirst(entry);
        } else {
            history.addFirst(new HistoryEntry(article));
        }
    }

    @Override
    public synchronized List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    @Override
    public synchronized void clearHistory() {
        history.clear();
    }

    @Override
    public synchronized int getHistoryCount() {
        return history.size();
    }
}
