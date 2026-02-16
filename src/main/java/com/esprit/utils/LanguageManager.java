package com.esprit.utils;

import java.util.*;

/**
 * Singleton manager for application-wide language/locale setting.
 * All controllers use this to get the current ResourceBundle.
 */
public class LanguageManager {

    private static LanguageManager instance;
    private Locale currentLocale;
    private ResourceBundle bundle;
    private final List<Runnable> listeners = new ArrayList<>();

    private LanguageManager() {
        currentLocale = Locale.FRENCH;
        loadBundle();
    }

    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    public void setLocale(Locale locale) {
        this.currentLocale = locale;
        loadBundle();
        notifyListeners();
    }

    public Locale getLocale() {
        return currentLocale;
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    /**
     * Convenience: get a string by key with fallback.
     */
    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Register a listener that is called when the language changes.
     * Controllers can use this to refresh their UI.
     */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void loadBundle() {
        try {
            bundle = ResourceBundle.getBundle("messages", currentLocale);
        } catch (MissingResourceException e) {
            System.err.println("⚠️ Could not load resource bundle for locale: " + currentLocale);
            bundle = ResourceBundle.getBundle("messages", Locale.FRENCH);
        }
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception e) {
                System.err.println("⚠️ Error notifying language listener: " + e.getMessage());
            }
        }
    }
}
