package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.chat.ChatWebSocketServer;

public class MainFX extends Application {

    private static ChatWebSocketServer chatServer;

    @Override
    public void start(Stage stage) throws Exception {
        startChatServer();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Fxml/main.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 1200, 750);

        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            System.out.println("CSS loaded successfully");
        } catch (Exception e) {
            System.out.println("CSS not found - continuing without it");
        }

        stage.setTitle("Tabaani Connect");
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private static void startChatServer() {
        Thread serverThread = new Thread(() -> {
            try {
                ChatWebSocketServer s = new ChatWebSocketServer();
                s.start();
                chatServer = s;
                System.out.println("[Chat] Ce client héberge le serveur (port " + org.example.chat.ChatWebSocketServer.PORT + ").");
            } catch (Exception e) {
                chatServer = null;
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("Address already in use") || msg.contains("Cannot assign requested address")) {
                    System.out.println("[Chat] Salon déjà ouvert ailleurs — cette fenêtre est en mode client uniquement.");
                } else {
                    System.err.println("[Chat] Serveur non démarré: " + msg);
                }
            }
        });
        serverThread.setDaemon(true);
        serverThread.setName("ChatWebSocketServer");
        serverThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
