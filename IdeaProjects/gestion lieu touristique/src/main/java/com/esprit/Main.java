package com.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Main application entry point for Tourist Management System.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            // Load the MainView.fxml from resources
            URL fxmlLocation = getClass().getResource("/MainView.fxml");
            if (fxmlLocation == null) {
                System.err.println("❌ Error: Cannot find MainView.fxml! Check your resources folder.");
                showErrorAndExit("FXML Error", "Failed to load MainView.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);

            // Load the scene
            Scene scene = new Scene(loader.load());

            // Optional: apply a CSS style if you have one
            URL cssLocation = getClass().getResource("/style.css");
            if (cssLocation != null) {
                scene.getStylesheets().add(cssLocation.toExternalForm());
                System.out.println("✅ CSS loaded successfully!");
            } else {
                System.err.println("⚠️ Warning: CSS file not found");
            }

            stage.setTitle("Système de Gestion Touristique");
            stage.setScene(scene);
            
            // Get screen bounds for responsive sizing
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            
            // Set window to fullscreen by default (user can exit with Esc or fullscreen button)
            stage.setFullScreen(false); // Set to false, user can toggle with button
            
            // Maximize window on startup
            stage.setMaximized(true);
            
            // Set window dimensions to screen size
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            
            // Set position to top-left corner (0, 0)
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            
            // Make window resizable
            stage.setResizable(true);
            
            // Set minimum window size
            stage.setMinWidth(1024);
            stage.setMinHeight(768);
            
            stage.show();
            System.out.println("✅ Application started successfully!");
        } catch (Exception e) {
            System.err.println("❌ Error starting application: " + e.getMessage());
            e.printStackTrace();
            showErrorAndExit("Application Error", "Error starting: " + e.getMessage());
        }
    }

    private void showErrorAndExit(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
        System.exit(1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
