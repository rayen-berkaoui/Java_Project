package com.esprit.services;

import javafx.scene.Scene;

/**
 * Theme management service for switching between Dark (default) and Light themes.
 * Uses CSS stylesheet swapping for instant theme changes.
 */
public class ThemeService {

    public enum Theme {
        DARK, LIGHT
    }

    private static Theme currentTheme = Theme.DARK;

    /**
     * Get the current active theme.
     */
    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Toggle between dark and light themes.
     */
    public static Theme toggleTheme(Scene scene) {
        if (currentTheme == Theme.DARK) {
            currentTheme = Theme.LIGHT;
        } else {
            currentTheme = Theme.DARK;
        }
        applyTheme(scene);
        return currentTheme;
    }

    /**
     * Apply the current theme to the given scene.
     */
    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        scene.getStylesheets().clear();

        String baseCSS = ThemeService.class.getResource("/style.css").toExternalForm();
        scene.getStylesheets().add(baseCSS);

        if (currentTheme == Theme.LIGHT) {
            String lightCSS = ThemeService.class.getResource("/light-theme.css").toExternalForm();
            scene.getStylesheets().add(lightCSS);
        }

        System.out.println("🎨 Theme switched to: " + currentTheme);
    }

    /**
     * Check if the current theme is light.
     */
    public static boolean isLight() {
        return currentTheme == Theme.LIGHT;
    }
}
