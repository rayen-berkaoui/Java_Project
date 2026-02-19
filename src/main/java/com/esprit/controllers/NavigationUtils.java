package com.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.net.URL;

public class NavigationUtils {

    public static void goTo(String fxmlPath, ActionEvent event) {
        try {
            URL url = NavigationUtils.class.getResource(fxmlPath);
            if (url == null) {
                throw new RuntimeException("FXML introuvable: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(url);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // garder le CSS
            URL cssUrl = NavigationUtils.class.getResource("/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);

            // ✅ IMPORTANT : plein écran partout
            stage.setMaximized(true);

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
