package com.esprit.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.esprit.services.ThemeService;
import com.esprit.entities.utilisateur;
import com.esprit.utils.ThemeManager;

/**
 * Main controller for navigation, fullscreen toggle, and chatbot panel.
 * Supports returning to either admin dashboard or user main interface.
 */
public class MainController {

    @FXML
    private TabPane mainTabPane;

    @FXML
    private VBox chatBotPanel;

    @FXML
    private Button btnChatBot;

    @FXML
    private Button btnThemeToggle;

    @FXML
    private javafx.scene.layout.HBox navButtonsBox;

    @FXML
    private Button btnBack;

    @FXML
    private Tooltip backTooltip;

    private Stage stage;
    private boolean isFullScreen = false;
    private boolean chatBotVisible = false;

    /** Tracks where to return: "admin" or "user" */
    private String returnTarget = "admin";
    /** Holds the current user (when coming from user interface) */
    private utilisateur currentUser;

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
                    // Ensure content is visible immediately — no animation on initial load
                    content.setOpacity(1.0);
                    content.setTranslateY(0);
                }
            });

            // No entrance animation on the scene root — handled by parent's fadeTransition
        }
    }

    // ========== RETURN TARGET ==========

    /**
     * Sets the return target and user when navigating from user interface.
     * @param target "admin" to return to admin dashboard, "user" to return to user interface
     * @param user the logged-in user (only needed when target is "user")
     */
    public void setReturnTarget(String target, utilisateur user) {
        this.returnTarget = target;
        this.currentUser = user;
        Platform.runLater(() -> {
            if (backTooltip != null) {
                backTooltip.setText("user".equals(target) ? "Retour a l'accueil" : "Retour au Dashboard Admin");
            }
        });
    }

    /**
     * Show only the Dashboard tab, hiding all other tabs and their nav buttons.
     */
    public void setDashboardOnly(boolean dashOnly) {
        Platform.runLater(() -> {
            if (mainTabPane == null) return;
            if (dashOnly) {
                // Remove all tabs except Dashboard (index 0)
                while (mainTabPane.getTabs().size() > 1) {
                    mainTabPane.getTabs().remove(mainTabPane.getTabs().size() - 1);
                }
                mainTabPane.getSelectionModel().select(0);
                // Hide the nav buttons bar (Dashboard/Categories/Adresses/Lieux)
                if (navButtonsBox != null) {
                    navButtonsBox.setVisible(false);
                    navButtonsBox.setManaged(false);
                }
            }
        });
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
    public void handleBack() {
        try {
            if ("user".equals(returnTarget)) {
                // Return to user main interface
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/maininterface.fxml"));
                Parent root = loader.load();
                MainInterfaceController controller = loader.getController();
                if (currentUser != null) {
                    controller.setUser(currentUser);
                }
                Stage currentStage = (Stage) mainTabPane.getScene().getWindow();
                Scene newScene = new Scene(root);
                ThemeManager.applyTheme(newScene);
                ThemeManager.trackScene(newScene);
                currentStage.setScene(newScene);
                currentStage.setTitle("SmartTravel - Accueil");
                currentStage.setMaximized(true);
            } else {
                // Return to admin dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
                Parent root = loader.load();
                Stage currentStage = (Stage) mainTabPane.getScene().getWindow();
                Scene newScene = new Scene(root);
                newScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
                currentStage.setScene(newScene);
                currentStage.setTitle("Tabaani - Dashboard");
                currentStage.setMaximized(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    // ========== THEME TOGGLE ==========

    @FXML
    public void toggleTheme() {
        if (mainTabPane != null && mainTabPane.getScene() != null) {
            ThemeService.Theme theme = ThemeService.toggleTheme(mainTabPane.getScene());
            if (btnThemeToggle != null) {
                btnThemeToggle.setText(theme == ThemeService.Theme.LIGHT ? "☾" : "☀");
                btnThemeToggle.setTooltip(new Tooltip(
                        theme == ThemeService.Theme.LIGHT ? "Thème sombre" : "Thème clair"));
            }
        }
    }

    // ========== CHATBOT TOGGLE ==========

    @FXML
    public void toggleChatBot() {
        if (chatBotPanel == null) return;

        chatBotVisible = !chatBotVisible;

        if (chatBotVisible) {
            // Show panel with slide-in animation
            chatBotPanel.setVisible(true);
            chatBotPanel.setManaged(true);
            chatBotPanel.setTranslateX(400);
            chatBotPanel.setOpacity(0);

            TranslateTransition slide = new TranslateTransition(Duration.millis(300), chatBotPanel);
            slide.setFromX(400);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition fade = new FadeTransition(Duration.millis(300), chatBotPanel);
            fade.setFromValue(0);
            fade.setToValue(1);

            new ParallelTransition(slide, fade).play();

            // Change button to X
            btnChatBot.setText("✕");
        } else {
            // Hide panel with slide-out animation
            TranslateTransition slide = new TranslateTransition(Duration.millis(250), chatBotPanel);
            slide.setFromX(0);
            slide.setToX(400);
            slide.setInterpolator(Interpolator.EASE_IN);

            FadeTransition fade = new FadeTransition(Duration.millis(250), chatBotPanel);
            fade.setFromValue(1);
            fade.setToValue(0);

            ParallelTransition anim = new ParallelTransition(slide, fade);
            anim.setOnFinished(e -> {
                chatBotPanel.setVisible(false);
                chatBotPanel.setManaged(false);
            });
            anim.play();

            // Change button back to robot
            btnChatBot.setText("🤖");
        }
    }
}
