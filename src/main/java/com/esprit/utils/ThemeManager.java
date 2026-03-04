package com.esprit.utils;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import java.util.prefs.Preferences;

/**
 * Gestionnaire global de thème Clair / Sombre.
 * <p>
 * Le choix est mémorisé via {@link Preferences} et appliqué
 * immédiatement sur la scène courante.
 * </p>
 */
public class ThemeManager {

    private static final String PREF_KEY = "app_theme";
    private static final String DARK = "dark";
    private static final String LIGHT = "light";
    private static final String LIGHT_CSS = "/style-light.css";

    private static final Preferences prefs =
            Preferences.userNodeForPackage(ThemeManager.class);

    static {
        // Migration : passer au mode clair par défaut
        if (prefs.getInt("theme_v", 0) < 2) {
            prefs.remove(PREF_KEY);
            prefs.putInt("theme_v", 2);
        }
    }

    /* ────────────────────────── état ────────────────────────── */

    /** Renvoie {@code true} si le thème courant est sombre. */
    public static boolean isDarkTheme() {
        return !LIGHT.equals(prefs.get(PREF_KEY, LIGHT));
    }

    /** Mémorise le choix du thème. */
    private static void setTheme(boolean dark) {
        prefs.put(PREF_KEY, dark ? DARK : LIGHT);
    }

    /* ──────────────────── application CSS ───────────────────── */

    /**
     * Applique le thème mémorisé sur la scène donnée.
     * En mode clair, {@code style-light.css} est ajouté
     * au niveau du nœud racine (même priorité que le FXML stylesheets).
     */
    public static void applyTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        String lightCss = ThemeManager.class.getResource(LIGHT_CSS) != null
                ? ThemeManager.class.getResource(LIGHT_CSS).toExternalForm()
                : null;
        if (lightCss == null) return;           // CSS non trouvé

        Parent root = scene.getRoot();
        if (isDarkTheme()) {
            root.getStylesheets().remove(lightCss);
        } else {
            if (!root.getStylesheets().contains(lightCss)) {
                root.getStylesheets().add(lightCss);
            }
        }
        // Mettre à jour les boutons toggle présents dans la scène
        updateToggleButtons(scene);
    }

    /* ──────────────────── toggle interactif ─────────────────── */

    /**
     * Bascule le thème et met à jour la scène + le bouton.
     * Peut être appelé directement depuis un {@code onAction}.
     */
    public static void handleToggleTheme(ActionEvent event) {
        Node source = (Node) event.getSource();
        Scene scene = source.getScene();
        setTheme(!isDarkTheme());
        applyTheme(scene);
    }

    /* ──────────────── initialisation par contrôleur ─────────── */

    /**
     * Appelé dans {@code initialize()} d'un contrôleur.
     * Attend que la scène soit prête, puis applique le thème mémorisé.
     */
    public static void initializeTheme(Node anyNodeInScene) {
        if (anyNodeInScene == null) return;
        Platform.runLater(() -> {
            Scene scene = anyNodeInScene.getScene();
            if (scene != null) {
                applyTheme(scene);
            }
        });
    }

    /* ──────────────────── utilitaire interne ────────────────── */

    private static void updateToggleButtons(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        String icon = isDarkTheme() ? "☀" : "🌙";
        scene.getRoot().lookupAll(".theme-toggle-btn").forEach(node -> {
            if (node instanceof Button) {
                ((Button) node).setText(icon);
            }
        });
    }
}
