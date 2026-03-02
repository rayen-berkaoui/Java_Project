package com.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
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

        // ✅ Load CSS based on saved theme preference
        scene.getStylesheets().add(
                ThemeManager.getInstance().getCssPath()
        );

        // ✅ Remove white background
        scene.setFill(Color.BLACK);

        // ✅ Remove white title bar
        stage.initStyle(StageStyle.UNDECORATED);

        stage.setTitle("Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}