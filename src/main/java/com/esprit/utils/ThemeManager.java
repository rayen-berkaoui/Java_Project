package com.esprit.utils;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

/**
 * Singleton that manages dark / light theme across the entire application.
 * Theme preference is persisted via java.util.prefs so it survives restarts.
 */
public class ThemeManager {

    public enum Theme { DARK, LIGHT }

    private static ThemeManager instance;
    private Theme currentTheme;
    private final Preferences prefs;
    private static final String PREF_KEY = "app_theme";

    private ThemeManager() {
        prefs = Preferences.userNodeForPackage(ThemeManager.class);
        String saved = prefs.get(PREF_KEY, "DARK");
        currentTheme = "LIGHT".equalsIgnoreCase(saved) ? Theme.LIGHT : Theme.DARK;
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /** Get the current theme. */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /** Returns true when dark mode is active. */
    public boolean isDark() {
        return currentTheme == Theme.DARK;
    }

    /** Toggle between dark and light and persist. */
    public void toggle() {
        setTheme(currentTheme == Theme.DARK ? Theme.LIGHT : Theme.DARK);
    }

    /** Set a specific theme and persist. */
    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        prefs.put(PREF_KEY, theme.name());
    }

    // ───────── CSS helpers ─────────

    private static final String DARK_CSS  = "/style.css";
    private static final String LIGHT_CSS = "/style-light.css";

    /** Apply the current theme's stylesheet to the given scene. */
    public void applyTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String css = (currentTheme == Theme.LIGHT) ? LIGHT_CSS : DARK_CSS;
        String resource = getClass().getResource(css) != null
                ? getClass().getResource(css).toExternalForm()
                : getClass().getResource(DARK_CSS).toExternalForm();   // fallback
        scene.getStylesheets().add(resource);
    }

    /** Convenience: resolve the right CSS path for a new Scene. */
    public String getCssPath() {
        String css = (currentTheme == Theme.LIGHT) ? LIGHT_CSS : DARK_CSS;
        return getClass().getResource(css) != null
                ? getClass().getResource(css).toExternalForm()
                : getClass().getResource(DARK_CSS).toExternalForm();
    }
}
