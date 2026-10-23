package com.newsroom.repository;

import com.newsroom.model.ReaderSettings;

/**
 * Repository interface for persistent user settings in SQLite.
 */
public interface SettingsRepository {
    void saveSetting(String key, String value);

    String getSetting(String key, String defaultValue);

    void loadInto(ReaderSettings settings);

    void persist(ReaderSettings settings);
}
