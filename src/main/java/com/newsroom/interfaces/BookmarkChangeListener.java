package com.newsroom.interfaces;

import com.newsroom.model.Article;

/**
 * Observer interface notified when an article's bookmark status changes.
 */
public interface BookmarkChangeListener {
    /**
     * Called whenever an article is added to or removed from bookmarks.
     *
     * @param article the target article
     * @param isBookmarked true if now bookmarked, false if removed
     */
    void onBookmarkChanged(Article article, boolean isBookmarked);
}
