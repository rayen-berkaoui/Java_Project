package com.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.esprit.utils.ThemeManager;

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
            Scene scene = stage.getScene();

            // ✅ on garde la même scene -> FULL SCREEN reste OK
            if (scene != null) {
                scene.setRoot(root);
            } else {
                stage.setScene(new Scene(root));
            }

            // ✅ force maximized après changement de root
            stage.setMaximized(true);

            // ✅ Appliquer le thème mémorisé
            javafx.application.Platform.runLater(() -> ThemeManager.applyTheme(stage.getScene()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
