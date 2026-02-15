package com.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

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
        // Ensure TabPane is injected correctly
        if (mainTabPane == null) {
            System.err.println("❌ Error: mainTabPane not injected. Check FXML fx:id!");
        } else {
            System.out.println("✅ MainView loaded successfully!");
        }
    }

    /**
     * Navigation button: Select Catégories tab
     */
    @FXML
    public void selectCategoriesTab() {
        selectTab(0);
    }

    /**
     * Navigation button: Select Adresses tab
     */
    @FXML
    public void selectAddressesTab() {
        selectTab(1);
    }

    /**
     * Navigation button: Select Lieux Touristiques tab
     */
    @FXML
    public void selectLocationsTab() {
        selectTab(2);
    }

    /**
     * Toggle fullscreen mode
     */
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

    /**
     * Switches to a tab by its index.
     * @param index the index of the tab to select
     */
    public void selectTab(int index) {
        if (mainTabPane != null && index >= 0 && index < mainTabPane.getTabs().size()) {
            mainTabPane.getSelectionModel().select(index);
        }
    }

    /**
     * Switches to a tab by its text.
     * @param tabText the text of the tab to select
     */
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
