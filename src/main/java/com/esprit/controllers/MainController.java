package com.esprit.controllers;

import com.esprit.utils.LanguageManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Main controller for navigation, fullscreen toggle, and language switching.
 */
public class MainController {

    @FXML
    private TabPane mainTabPane;

    @FXML private Button btnLangFr, btnLangEn, btnLangAr;
    
    private Stage stage;
    private boolean isFullScreen = false;

    @FXML
    public void initialize() {
        if (mainTabPane == null) {
            System.err.println("❌ Error: mainTabPane not injected. Check FXML fx:id!");
        } else {
            System.out.println("✅ MainView loaded successfully!");

            // Animated fade transition when switching tabs
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null && newTab.getContent() != null) {
                    Node content = newTab.getContent();
                    content.setOpacity(0);
                    content.setTranslateY(12);

                    FadeTransition fade = new FadeTransition(Duration.millis(350), content);
                    fade.setFromValue(0);
                    fade.setToValue(1);
                    fade.setInterpolator(Interpolator.EASE_OUT);

                    TranslateTransition slide = new TranslateTransition(Duration.millis(350), content);
                    slide.setFromY(12);
                    slide.setToY(0);
                    slide.setInterpolator(Interpolator.EASE_OUT);

                    new ParallelTransition(fade, slide).play();
                }
            });

            // Entrance animation for the whole scene
            Platform.runLater(() -> {
                if (mainTabPane.getScene() != null) {
                    Node root = mainTabPane.getScene().getRoot();
                    root.setOpacity(0);
                    FadeTransition entrance = new FadeTransition(Duration.millis(700), root);
                    entrance.setFromValue(0);
                    entrance.setToValue(1);
                    entrance.setInterpolator(Interpolator.EASE_OUT);
                    entrance.play();
                }
            });

            // Apply initial language
            updateLanguageUI();
        }
    }

    // ========== LANGUAGE SWITCHING ==========

    @FXML
    public void switchToFrench() {
        switchLanguage(Locale.FRENCH);
    }

    @FXML
    public void switchToEnglish() {
        switchLanguage(Locale.ENGLISH);
    }

    @FXML
    public void switchToArabic() {
        switchLanguage(new Locale("ar"));
    }

    private void switchLanguage(Locale locale) {
        LanguageManager.getInstance().setLocale(locale);
        updateLanguageUI();
        System.out.println("🌐 Language switched to: " + locale.getLanguage());
    }

    private void updateLanguageUI() {
        ResourceBundle bundle = LanguageManager.getInstance().getBundle();
        String lang = LanguageManager.getInstance().getLocale().getLanguage();

        // Update tab names
        if (mainTabPane != null && mainTabPane.getTabs().size() >= 5) {
            mainTabPane.getTabs().get(0).setText(bundle.getString("tab.dashboard"));
            mainTabPane.getTabs().get(1).setText(bundle.getString("tab.categories"));
            mainTabPane.getTabs().get(2).setText(bundle.getString("tab.addresses"));
            mainTabPane.getTabs().get(3).setText(bundle.getString("tab.locations"));
            mainTabPane.getTabs().get(4).setText(bundle.getString("tab.favorites"));
        }

        // Update language toggle button styles
        if (btnLangFr != null) {
            btnLangFr.getStyleClass().remove("lang-toggle-active");
            btnLangEn.getStyleClass().remove("lang-toggle-active");
            btnLangAr.getStyleClass().remove("lang-toggle-active");
            switch (lang) {
                case "fr": btnLangFr.getStyleClass().add("lang-toggle-active"); break;
                case "en": btnLangEn.getStyleClass().add("lang-toggle-active"); break;
                case "ar": btnLangAr.getStyleClass().add("lang-toggle-active"); break;
            }
        }

        // Set RTL for Arabic
        if (mainTabPane != null && mainTabPane.getScene() != null) {
            Node root = mainTabPane.getScene().getRoot();
            if ("ar".equals(lang)) {
                root.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
            } else {
                root.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
            }
        }
    }

    // ========== TAB NAVIGATION ==========

    @FXML
    public void selectDashboardTab() {
        selectTab(0);
    }

    @FXML
    public void selectCategoriesTab() {
        selectTab(1);
    }

    @FXML
    public void selectAddressesTab() {
        selectTab(2);
    }

    @FXML
    public void selectLocationsTab() {
        selectTab(3);
    }

    @FXML
    public void selectFavorisTab() {
        selectTab(4);
    }

    @FXML
    public void toggleFullScreen() {
        if (stage == null && mainTabPane != null && mainTabPane.getScene() != null) {
            stage = (Stage) mainTabPane.getScene().getWindow();
        }
        
        if (stage != null) {
            isFullScreen = !isFullScreen;
            stage.setFullScreen(isFullScreen);
            System.out.println(isFullScreen ? "✅ Fullscreen ON" : "✅ Fullscreen OFF");
        }
    }

    public void selectTab(int index) {
        if (mainTabPane != null && index >= 0 && index < mainTabPane.getTabs().size()) {
            mainTabPane.getSelectionModel().select(index);
        }
    }

    public void selectTab(String tabText) {
        if (mainTabPane != null) {
            for (Tab tab : mainTabPane.getTabs()) {
                if (tab.getText().equalsIgnoreCase(tabText)) {
                    mainTabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }
    }
}
