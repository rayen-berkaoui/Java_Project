package com.esprit.utils;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class ThemeManager {

    public enum Theme { DARK, LIGHT }

    public enum ThemeMode { SYSTEM, DARK, LIGHT }

    private static Theme currentTheme = Theme.DARK;
    private static ThemeMode currentMode = ThemeMode.SYSTEM;
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_KEY = "app_theme";
    private static final String MODE_KEY = "app_theme_mode";
    private static final List<Scene> trackedScenes = new ArrayList<>();
    private static final List<Runnable> themeChangeListeners = new ArrayList<>();

    static {
        String savedMode = prefs.get(MODE_KEY, "SYSTEM");
        try {
            currentMode = ThemeMode.valueOf(savedMode);
        } catch (Exception e) {
            currentMode = ThemeMode.SYSTEM;
        }
        if (currentMode == ThemeMode.SYSTEM) {
            currentTheme = detectSystemTheme();
        } else {
            currentTheme = (currentMode == ThemeMode.LIGHT) ? Theme.LIGHT : Theme.DARK;
        }
    }

    private static Theme detectSystemTheme() {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query",
                    "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            if (output.contains("0x0")) return Theme.DARK;
            if (output.contains("0x1")) return Theme.LIGHT;
        } catch (Exception ignored) {}
        return Theme.DARK;
    }

    public static Theme getCurrentTheme() { return currentTheme; }
    public static ThemeMode getCurrentMode() { return currentMode; }
    public static boolean isDark() { return currentTheme == Theme.DARK; }

    public static void setMode(ThemeMode mode) {
        currentMode = mode;
        prefs.put(MODE_KEY, mode.name());
        if (mode == ThemeMode.SYSTEM) {
            currentTheme = detectSystemTheme();
        } else {
            currentTheme = (mode == ThemeMode.LIGHT) ? Theme.LIGHT : Theme.DARK;
        }
        prefs.put(PREF_KEY, currentTheme.name());
        for (Scene scene : trackedScenes) applyTheme(scene);
        for (Runnable listener : new ArrayList<>(themeChangeListeners)) {
            try { listener.run(); } catch (Exception ignored) {}
        }
    }

    public static void setModeFromString(String modeStr) {
        if (modeStr == null) modeStr = "SYSTEM";
        try { setMode(ThemeMode.valueOf(modeStr.toUpperCase())); }
        catch (Exception e) { setMode(ThemeMode.SYSTEM); }
    }

    public static void toggleTheme() { setMode(nextMode()); }

    public static void setTheme(Theme theme) {
        setMode(theme == Theme.LIGHT ? ThemeMode.LIGHT : ThemeMode.DARK);
    }

    public static void handleToggleTheme(ActionEvent event) {
        Node source = (Node) event.getSource();
        Scene scene = source.getScene();
        toggleTheme();
        applyTheme(scene);
    }

    public static void addThemeChangeListener(Runnable listener) {
        if (!themeChangeListeners.contains(listener)) themeChangeListeners.add(listener);
    }

    public static void removeThemeChangeListener(Runnable listener) {
        themeChangeListeners.remove(listener);
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        scene.getStylesheets().add(ThemeManager.class.getResource("/style.css").toExternalForm());
        if (currentTheme == Theme.LIGHT) {
            scene.getStylesheets().add(ThemeManager.class.getResource("/style-light.css").toExternalForm());
        }
    }

    public static void trackScene(Scene scene) {
        if (!trackedScenes.contains(scene)) trackedScenes.add(scene);
        applyTheme(scene);
    }

    public static void untrackScene(Scene scene) { trackedScenes.remove(scene); }

    public static String bg() { return isDark() ? "#0a0a0a" : "#EEF2F7"; }
    public static String cardBg() { return isDark() ? "rgba(15,15,15,0.95)" : "#FFFFFF"; }
    public static String rowBg() { return isDark() ? "#0d0d0d" : "#F8FAFC"; }
    public static String headerBg() { return isDark() ? "#080808" : "#FFFFFF"; }
    public static String sidebarBg() { return isDark() ? "#080808" : "#F0F4F8"; }
    public static String dialogBg() { return isDark() ? "#1a1a1a" : "#FFFFFF"; }
    public static String inputBg() { return isDark() ? "rgba(18,18,18,0.9)" : "#F8FAFC"; }
    public static String textPrimary() { return isDark() ? "#ffffff" : "#1E293B"; }
    public static String textSecondary() { return isDark() ? "#888888" : "#64748B"; }
    public static String inputText() { return isDark() ? "#ffffff" : "#1E293B"; }
    public static String accent() { return isDark() ? "#FFD700" : "#2563EB"; }
    public static String accentGradient() { return isDark() ? "linear-gradient(to right, #FFD700, #FFAA00)" : "linear-gradient(to right, #2563EB, #3B82F6)"; }
    public static String accentBorder() { return isDark() ? "rgba(255,215,0,0.15)" : "rgba(37,99,235,0.2)"; }
    public static String accentHover() { return isDark() ? "rgba(255,215,0,0.08)" : "rgba(37,99,235,0.08)"; }
    public static String borderColor() { return isDark() ? "rgba(255,215,0,0.06)" : "rgba(37,99,235,0.1)"; }
    public static String separatorColor() { return isDark() ? "#333333" : "#E2E8F0"; }
    public static String promptText() { return isDark() ? "#666666" : "#94A3B8"; }
    public static String inputBorder() { return isDark() ? "#333333" : "#CBD5E1"; }
    public static String scrollPaneBg() { return "-fx-background: " + bg() + "; -fx-background-color: " + bg() + ";"; }
    public static String successColor() { return "#16A34A"; }
    public static String dangerColor() { return "#DC2626"; }
    public static String infoColor() { return "#2563EB"; }

    public static String chatBtnGradient() {
        return isDark() ? "linear-gradient(to bottom right, #FFD700, #FF8C00)" : "linear-gradient(to bottom right, #2563EB, #1D4ED8)";
    }

    public static String chatBtnHoverGradient() {
        return isDark() ? "linear-gradient(to bottom right, #FFE44D, #FFA500)" : "linear-gradient(to bottom right, #3B82F6, #2563EB)";
    }

    public static void applyThemeToFXML(Parent root) {
        if (root == null) return;
        applyBgToNode(root);
        walkAndStyle(root);
    }

    private static void walkAndStyle(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            applyBgToNode(child);
            if (child instanceof Parent p) walkAndStyle(p);
        }
    }

    private static void applyBgToNode(Node node) {
        String style = node.getStyle();
        if (style == null || style.isEmpty()) return;

        style = style.replace("#0a0a0a", bg());
        style = style.replace("#080808", headerBg());
        style = style.replace("#0d0d0d", rowBg());
        style = style.replace("#111111", isDark() ? "#111111" : "#F1F5F9");
        style = style.replace("rgba(15,15,15,0.95)", cardBg());
        style = style.replace("rgba(15,15,15,0.98)", cardBg());
        style = style.replace("#1a1a1a", isDark() ? "#1a1a1a" : "#FFFFFF");

        if (!isDark()) {
            style = style.replace("#FFD700", "#2563EB");
            style = style.replace("#FF8C00", "#1D4ED8");
            style = style.replace("#FFAA00", "#3B82F6");
            style = style.replace("#FFE44D", "#3B82F6");
            style = style.replace("#FFA500", "#2563EB");
            style = style.replace("#B8860B", "#1E40AF");
            style = style.replace("#D4A800", "#1D4ED8");
            style = style.replace("rgba(255,215,0,", "rgba(37,99,235,");
            style = style.replace("rgba(255,140,0,", "rgba(29,78,216,");

            if (style.contains("linear-gradient") || style.contains("#2563EB") || style.contains("#1D4ED8")) {
                style = style.replace("-fx-text-fill: black", "-fx-text-fill: white");
                style = style.replace("-fx-text-fill: #000", "-fx-text-fill: white");
            }

            style = style.replace("rgba(100,181,246,0.08)", "rgba(37,99,235,0.06)");
            style = style.replace("rgba(100,181,246,0.2)", "rgba(37,99,235,0.12)");
            style = style.replace("rgba(100,181,246,0.3)", "rgba(37,99,235,0.18)");
            style = style.replace("rgba(100,181,246,0.4)", "rgba(37,99,235,0.22)");
            style = style.replace("rgba(100,181,246,0.15)", "rgba(37,99,235,0.1)");
            style = style.replace("#64B5F6", "#2563EB");
            style = style.replace("#42A5F5", "#3B82F6");

            style = style.replace("rgba(178,102,255,0.06)", "rgba(37,99,235,0.05)");
            style = style.replace("rgba(178,102,255,0.08)", "rgba(37,99,235,0.05)");
            style = style.replace("#B266FF", "#2563EB");

            style = style.replace("linear-gradient(to bottom right, #1a1a2e, #16213e)", "linear-gradient(to bottom right, #F1F5F9, #E2E8F0)");

            boolean isAccentEl = style.contains("linear-gradient") || style.contains("-fx-background-color: #2563EB") || style.contains("-fx-background-color: #1D4ED8");
            boolean isColoredLabel = style.contains("#51CF66") || style.contains("#FF6B6B") || style.contains("#DC2626") || style.contains("#16A34A");

            if (!isAccentEl && !isColoredLabel && (node instanceof Label || node instanceof Region)) {
                style = style.replace("-fx-text-fill: white", "-fx-text-fill: " + textPrimary());
                style = style.replace("-fx-text-fill: #ffffff", "-fx-text-fill: " + textPrimary());
                style = style.replace("-fx-text-fill: #fff;", "-fx-text-fill: " + textPrimary() + ";");
            }

            style = style.replace("rgba(255,255,255,0.08)", inputBg());
            style = style.replace("rgba(255,255,255,0.02)", "rgba(37,99,235,0.02)");
            style = style.replace("rgba(18,18,18,0.9)", inputBg());

            style = style.replace("-fx-text-fill: #888;", "-fx-text-fill: #64748B;");
            style = style.replace("-fx-text-fill: #888888", "-fx-text-fill: #64748B");
            style = style.replace("-fx-text-fill: #aaa;", "-fx-text-fill: #64748B;");
            style = style.replace("-fx-text-fill: #666;", "-fx-text-fill: #94A3B8;");
            style = style.replace("-fx-text-fill: #555;", "-fx-text-fill: #94A3B8;");
            style = style.replace("-fx-prompt-text-fill: #555;", "-fx-prompt-text-fill: #94A3B8;");
            style = style.replace("-fx-prompt-text-fill: #555555", "-fx-prompt-text-fill: #94A3B8");
        }

        node.setStyle(style);

        if (node instanceof ScrollPane sp) {
            sp.setStyle(scrollPaneBg() + " -fx-padding: 0; -fx-border-width: 0;");
        }
    }

    public static String themeIcon() {
        return switch (currentMode) {
            case SYSTEM -> "\uD83D\uDCBB";
            case DARK -> "\u2600";
            case LIGHT -> "\uD83C\uDF19";
        };
    }

    public static ThemeMode nextMode() {
        return switch (currentMode) {
            case DARK -> ThemeMode.LIGHT;
            case LIGHT -> ThemeMode.SYSTEM;
            case SYSTEM -> ThemeMode.DARK;
        };
    }

    public static String modeLabel() {
        return switch (currentMode) {
            case SYSTEM -> "Systeme";
            case DARK -> "Sombre";
            case LIGHT -> "Clair";
        };
    }

    public static String themeToggleBtnStyle() {
        return isDark()
            ? "-fx-background-color: rgba(255,215,0,0.1); -fx-text-fill: #FFD700; -fx-font-size: 13; -fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand; -fx-border-color: rgba(255,215,0,0.2); -fx-border-radius: 20;"
            : "-fx-background-color: rgba(37,99,235,0.08); -fx-text-fill: #2563EB; -fx-font-size: 13; -fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand; -fx-border-color: rgba(37,99,235,0.15); -fx-border-radius: 20;";
    }
}