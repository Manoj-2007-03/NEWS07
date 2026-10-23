package com.newsroom.interfaces;

import com.newsroom.model.Article;

import java.util.List;

/**
 * Service contract for searching news articles.
 * Demonstrates Abstraction and Strategy / Service pattern.
 */
public interface SearchService {
    /**
     * Executes a keyword query across headlines, summaries, authors, and body text.
     *
     * @param query search query
     * @return list of matching articles, sorted by relevance or recency
     */
    List<Article> search(String query);
}
