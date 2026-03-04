package com.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.esprit.utils.ThemeManager;

import java.net.URL;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        URL fxmlUrl = getClass().getResource("/home.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException(" FXML introuvable: /home.fxml\n" +
                    "➡️ Mets-le dans: src/main/resources/home.fxml");
        }

        Parent root = FXMLLoader.load(fxmlUrl);

        Scene scene = new Scene(root);

        URL cssUrl = getClass().getResource("/style.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        // Appliquer le thème mémorisé (clair/sombre)
        ThemeManager.applyTheme(scene);

        stage.setTitle("Dashboard");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
