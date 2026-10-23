package com.newsroom.theme;

import com.newsroom.interfaces.ThemeChangeListener;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages the global theme lifecycle and notifies registered UI components.
 * Demonstrates Singleton pattern, Observer pattern, and Encapsulation.
 */
public class ThemeManager {
    private static volatile ThemeManager instance;

    private AppTheme currentTheme;
    private ThemeColors currentColors;
    private final List<ThemeChangeListener> listeners = new ArrayList<>();

    private ThemeManager() {
        this.currentTheme = AppTheme.LIGHT;
        this.currentColors = ThemeColors.getLight();
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            synchronized (ThemeManager.class) {
                if (instance == null) {
                    instance = new ThemeManager();
                }
            }
        }
        return instance;
    }

    public AppTheme getCurrentTheme() {
        return currentTheme;
    }

    public ThemeColors getColors() {
        return currentColors;
    }

    public boolean isDarkMode() {
        return currentTheme == AppTheme.DARK;
    }

    public void setTheme(AppTheme theme) {
        if (theme == null || theme == this.currentTheme) {
            return;
        }
        this.currentTheme = theme;
        this.currentColors = (theme == AppTheme.DARK) ? ThemeColors.getDark() : ThemeColors.getLight();
        notifyListeners();
    }

    public void toggleTheme() {
        setTheme(currentTheme == AppTheme.LIGHT ? AppTheme.DARK : AppTheme.LIGHT);
    }

    public void addListener(ThemeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        Runnable notification = () -> {
            for (ThemeChangeListener listener : new ArrayList<>(listeners)) {
                try {
                    listener.onThemeChanged(currentTheme);
                } catch (Exception e) {
                    System.err.println("Error notifying theme listener: " + e.getMessage());
                }
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            notification.run();
        } else {
            SwingUtilities.invokeLater(notification);
        }
    }
}
