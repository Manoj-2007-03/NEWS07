package com.newsroom.interfaces;

/**
 * Interface representing any item that can be saved / bookmarked by the reader.
 * Demonstrates Abstraction and interface segregation.
 */
public interface Bookmarkable {
    /**
     * Unique identifier for bookmark tracking.
     *
     * @return unique string identifier
     */
    String getId();

    /**
     * Determines whether this item is currently marked as saved.
     *
     * @return true if bookmarked
     */
    boolean isBookmarked();

    /**
     * Updates the bookmark state of this item.
     *
     * @param bookmarked new bookmark state
     */
    void setBookmarked(boolean bookmarked);
}
