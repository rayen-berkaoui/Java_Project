package com.esprit.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Main controller for navigation and fullscreen toggle.
 */
public class MainController {

    @FXML
    private TabPane mainTabPane;
    
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
