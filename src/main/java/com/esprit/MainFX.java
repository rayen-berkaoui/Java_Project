package com.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        URL fxmlUrl = getClass().getResource("/etablissement_affichage.fxml");

        if (fxmlUrl == null) {
            throw new RuntimeException("❌ FXML introuvable: /activite_view.fxml\n" +
                    "➡️ Mets-le dans: src/main/resources/activite_view.fxml");
        }

        Parent root = FXMLLoader.load(fxmlUrl);

        Scene scene = new Scene(root, 1200, 750);

        URL cssUrl = getClass().getResource("/style.css"); // ou /style.css selon ton projet
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.out.println("⚠️ CSS introuvable (optionnel).");
        }

        stage.setTitle("Gestion des Activités");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
