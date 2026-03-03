package com.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.esprit.utils.ThemeManager;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/login.fxml")
        );

        Scene scene = new Scene(loader.load());

        // Apply theme (loads dark or light based on saved preference)
        ThemeManager.applyTheme(scene);
        ThemeManager.trackScene(scene);

        // Remove white title bar
        stage.initStyle(StageStyle.UNDECORATED);

        stage.setTitle("Login");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}