package com.esprit.utils;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Centralized Theme Manager — handles dark/light theme switching.
 * Persists user preference using Java Preferences API.
 *
 * DARK  = black/dark grey + gold accents
 * LIGHT = white/cream + ocean blue accents — completely different look
 */
public class ThemeManager {

    public enum Theme { DARK, LIGHT }

    private static Theme currentTheme = Theme.DARK;
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_KEY = "app_theme";
    private static final List<Scene> trackedScenes = new ArrayList<>();
    private static final List<Runnable> themeChangeListeners = new ArrayList<>();

    static {
        String saved = prefs.get(PREF_KEY, "DARK");
        currentTheme = "LIGHT".equals(saved) ? Theme.LIGHT : Theme.DARK;
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDark() {
        return currentTheme == Theme.DARK;
    }

    public static void setTheme(Theme theme) {
        currentTheme = theme;
        prefs.put(PREF_KEY, theme.name());
        for (Scene scene : trackedScenes) {
            applyTheme(scene);
        }
        for (Runnable listener : new ArrayList<>(themeChangeListeners)) {
            try { listener.run(); } catch (Exception ignored) {}
        }
    }

    public static void toggleTheme() {
        setTheme(currentTheme == Theme.DARK ? Theme.LIGHT : Theme.DARK);
    }

    /** Register a callback that fires whenever the theme changes. */
    public static void addThemeChangeListener(Runnable listener) {
        if (!themeChangeListeners.contains(listener)) themeChangeListeners.add(listener);
    }

    public static void removeThemeChangeListener(Runnable listener) {
        themeChangeListeners.remove(listener);
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        scene.getStylesheets().add(
            ThemeManager.class.getResource("/style.css").toExternalForm()
        );
        if (currentTheme == Theme.LIGHT) {
            scene.getStylesheets().add(
                ThemeManager.class.getResource("/style-light.css").toExternalForm()
            );
        }
    }

    public static void trackScene(Scene scene) {
        if (!trackedScenes.contains(scene)) trackedScenes.add(scene);
        applyTheme(scene);
    }

    public static void untrackScene(Scene scene) {
        trackedScenes.remove(scene);
    }

    /* =========================================================
       INLINE STYLE HELPERS — return theme-appropriate colors
       Dark  → black/gold palette
       Light → white/blue palette (dramatically different)
       ========================================================= */

    /** Main page background */
    public static String bg() {
        return isDark() ? "#0a0a0a" : "#EEF2F7";
    }

    /** Card / panel background */
    public static String cardBg() {
        return isDark() ? "rgba(15,15,15,0.95)" : "#FFFFFF";
    }

    /** Row background (tables, lists) */
    public static String rowBg() {
        return isDark() ? "#0d0d0d" : "#F8FAFC";
    }

    /** Header / title-bar background */
    public static String headerBg() {
        return isDark() ? "#080808" : "#FFFFFF";
    }

    /** Sidebar background */
    public static String sidebarBg() {
        return isDark() ? "#080808" : "#F0F4F8";
    }

    /** Dialog / popup background */
    public static String dialogBg() {
        return isDark() ? "#1a1a1a" : "#FFFFFF";
    }

    /** Input field background */
    public static String inputBg() {
        return isDark() ? "rgba(18,18,18,0.9)" : "#F5F7FA";
    }

    /** Primary text */
    public static String textPrimary() {
        return isDark() ? "#ffffff" : "#1E293B";
    }

    /** Secondary / muted text */
    public static String textSecondary() {
        return isDark() ? "#888888" : "#64748B";
    }

    /** Input text color */
    public static String inputText() {
        return isDark() ? "#ffffff" : "#1E293B";
    }

    /** Accent color — GOLD for dark, OCEAN BLUE for light */
    public static String accent() {
        return isDark() ? "#FFD700" : "#2563EB";
    }

    /** Accent gradient */
    public static String accentGradient() {
        return isDark()
            ? "linear-gradient(to right, #FFD700, #FFAA00)"
            : "linear-gradient(to right, #2563EB, #3B82F6)";
    }

    /** Accent with opacity for borders */
    public static String accentBorder() {
        return isDark() ? "rgba(255,215,0,0.15)" : "rgba(37,99,235,0.2)";
    }

    /** Accent with light opacity for hover effects */
    public static String accentHover() {
        return isDark() ? "rgba(255,215,0,0.08)" : "rgba(37,99,235,0.08)";
    }

    /** Border color for cards/panels */
    public static String borderColor() {
        return isDark() ? "rgba(255,215,0,0.06)" : "rgba(37,99,235,0.1)";
    }

    /** Separator line color */
    public static String separatorColor() {
        return isDark() ? "#333333" : "#E2E8F0";
    }

    /** Prompt text fill */
    public static String promptText() {
        return isDark() ? "#666666" : "#94A3B8";
    }

    /** Input border */
    public static String inputBorder() {
        return isDark() ? "#333333" : "#CBD5E1";
    }

    /** Scroll pane background style */
    public static String scrollPaneBg() {
        return "-fx-background: " + bg() + "; -fx-background-color: " + bg() + ";";
    }

    // -------- Action button colors (stay the same in both themes) --------
    public static String successColor() { return "#16A34A"; }
    public static String dangerColor()  { return "#DC2626"; }
    public static String infoColor()    { return "#2563EB"; }

    /** Chatbot button gradient */
    public static String chatBtnGradient() {
        return isDark()
            ? "linear-gradient(to bottom right, #FFD700, #FF8C00)"
            : "linear-gradient(to bottom right, #2563EB, #1D4ED8)";
    }
    public static String chatBtnHoverGradient() {
        return isDark()
            ? "linear-gradient(to bottom right, #FFE44D, #FFA500)"
            : "linear-gradient(to bottom right, #3B82F6, #2563EB)";
    }

    /* =========================================================
       NODE STYLING — restyle FXML nodes programmatically
       Call this in initialize() to fix inline FXML styles
       ========================================================= */

    /**
     * Walk an FXML root and replace hardcoded dark inline styles
     * with theme-aware colors. Call from controller initialize().
     */
    public static void applyThemeToFXML(Parent root) {
        if (root == null) return;
        // Style the root itself
        applyBgToNode(root);
        // Walk all children recursively
        walkAndStyle(root);
    }

    private static void walkAndStyle(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            applyBgToNode(child);
            if (child instanceof Parent p) {
                walkAndStyle(p);
            }
        }
    }

    private static void applyBgToNode(Node node) {
        String style = node.getStyle();
        if (style == null || style.isEmpty()) return;

        // Replace dark backgrounds with theme bg
        style = style.replace("#0a0a0a", bg());
        style = style.replace("#080808", headerBg());
        style = style.replace("#0d0d0d", rowBg());
        style = style.replace("#1a1a1a", cardBg());
        style = style.replace("#111111", isDark() ? "#111111" : "#E2E8F0");

        // Replace accent colors: gold → blue in light mode
        if (!isDark()) {
            // Gold accent → ocean blue
            style = style.replace("#FFD700", "#2563EB");
            style = style.replace("#FF8C00", "#1D4ED8");
            style = style.replace("#FFAA00", "#3B82F6");
            style = style.replace("#FFE44D", "#3B82F6");
            style = style.replace("#FFA500", "#2563EB");
            style = style.replace("#B8860B", "#1E40AF");
            style = style.replace("#D4A800", "#1D4ED8");

            // Gold rgba → blue rgba
            style = style.replace("rgba(255,215,0,", "rgba(37,99,235,");
            style = style.replace("rgba(255,140,0,", "rgba(29,78,216,");

            // Fix text on accent buttons: black text → white on blue
            if (style.contains("#2563EB") || style.contains("#1D4ED8") || style.contains("linear-gradient")) {
                style = style.replace("-fx-text-fill: black", "-fx-text-fill: white");
                style = style.replace("-fx-text-fill: #000", "-fx-text-fill: white");
            }
        }

        // Replace dark borders
        style = style.replace("rgba(255,215,0,0.06)", borderColor());

        // Fix text fills for light mode
        if (!isDark()) {
            // Determine if this is an accent/gradient button (should keep white text)
            boolean isAccentElement = style.contains("linear-gradient") ||
                    style.contains("-fx-background-color: #2563EB") ||
                    style.contains("-fx-background-color: #1D4ED8");

            if (!isAccentElement && (node instanceof Label || node instanceof Region)) {
                style = style.replace("-fx-text-fill: white", "-fx-text-fill: " + textPrimary());
                style = style.replace("-fx-text-fill: #ffffff", "-fx-text-fill: " + textPrimary());
                style = style.replace("-fx-text-fill: #fff;", "-fx-text-fill: " + textPrimary() + ";");
            }
            // Keep white text on dark input/scroll backgrounds
            if (style.contains("-fx-background-color: rgba(255,255,255,") ||
                style.contains("-fx-background-color: rgba(18,18,18,")) {
                style = style.replace("rgba(255,255,255,0.08)", inputBg());
                style = style.replace("rgba(18,18,18,0.9)", inputBg());
                style = style.replace("-fx-text-fill: " + textPrimary(), "-fx-text-fill: " + inputText());
            }
        }

        node.setStyle(style);

        // Special handling for ScrollPane
        if (node instanceof ScrollPane sp) {
            sp.setStyle(scrollPaneBg() + " -fx-padding: 0;");
        }
    }

    /** Build theme-toggle icon text */
    public static String themeIcon() {
        return isDark() ? "☀" : "🌙";
    }

    /** Theme toggle button style */
    public static String themeToggleBtnStyle() {
        return isDark()
            ? "-fx-background-color: rgba(255,215,0,0.1); -fx-text-fill: #FFD700; -fx-font-size: 14; -fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand; -fx-border-color: rgba(255,215,0,0.2); -fx-border-radius: 20;"
            : "-fx-background-color: rgba(37,99,235,0.1); -fx-text-fill: #2563EB; -fx-font-size: 14; -fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand; -fx-border-color: rgba(37,99,235,0.2); -fx-border-radius: 20;";
    }
}
