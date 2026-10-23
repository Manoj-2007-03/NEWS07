package com.newsroom.interfaces;

import com.newsroom.theme.AppTheme;

/**
 * Observer interface for components that respond to theme switches.
 * Demonstrates the Observer design pattern and Abstraction.
 */
public interface ThemeChangeListener {
    /**
     * Invoked when the global application theme is toggled between Light and Dark.
     *
     * @param theme the newly applied theme
     */
    void onThemeChanged(AppTheme theme);
}
