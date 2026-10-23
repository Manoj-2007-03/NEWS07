package com.newsroom.interfaces;

/**
 * Interface representing any domain model that supports search matching.
 * Demonstrates Abstraction by isolating searchability behavior.
 */
public interface Searchable {
    /**
     * Checks if this object matches the given search query.
     * Implementations typically perform case-insensitive matching across multiple fields.
     *
     * @param query the search keyword or phrase
     * @return true if the item matches the query, false otherwise
     */
    boolean matches(String query);

    /**
     * Returns an aggregated textual representation of searchable fields.
     *
     * @return combined text content
     */
    String getSearchableContent();
}
