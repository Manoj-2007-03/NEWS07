package com.newsroom.interfaces;

/**
 * Observer interface for background news refresh cycles.
 */
public interface RefreshListener {
    /**
     * Invoked when a manual or scheduled network fetch begins.
     */
    void onRefreshStarted();

    /**
     * Invoked when network fetch and database synchronization finishes.
     *
     * @param success true if live network fetch succeeded; false if cached/offline
     * @param newArticlesCount count of newly ingested or updated articles
     * @param statusMessage descriptive human-readable outcome
     */
    void onRefreshCompleted(boolean success, int newArticlesCount, String statusMessage);
}
