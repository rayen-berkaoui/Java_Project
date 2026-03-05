package com.esprit.controllers;

import com.esprit.services.ChatbotService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Controller for the AI Chatbot panel.
 * Uses the unified ChatbotService (multi-model Gemini + DB context + offline fallback).
 */
public class ChatBotController {

    @FXML private VBox messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField tfMessage;
    @FXML private Button btnSend;
    @FXML private Button btnClear;
    @FXML private Label lblStatus;
    @FXML private FlowPane suggestionsPane;

    private ChatbotService chatbotService;

    @FXML
    public void initialize() {
        chatbotService = new ChatbotService();

        // Add welcome message
        addBotMessage("Bonjour ! 👋 Je suis votre assistant IA Tabaani.\n\nJe peux repondre a toutes vos questions — tourisme, culture, science et plus encore ! 🌍");

        // Add suggestion chips
        addSuggestionChips();

        // Auto-scroll to bottom when new messages are added
        messagesContainer.heightProperty().addListener((obs, o, n) -> {
            scrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void sendMessage() {
        String msg = tfMessage.getText().trim();
        if (msg.isEmpty()) return;

        // Add user message
        addUserMessage(msg);
        tfMessage.clear();
        tfMessage.setDisable(true);
        btnSend.setDisable(true);

        // Hide suggestions after first message
        if (suggestionsPane.isVisible()) {
            suggestionsPane.setVisible(false);
            suggestionsPane.setManaged(false);
        }

        // Show typing indicator
        HBox typingBox = createTypingIndicator();
        messagesContainer.getChildren().add(typingBox);

        // Send to AI in background
        Thread aiThread = new Thread(() -> {
            try {
                String response = chatbotService.chat(msg);
                Platform.runLater(() -> {
                    messagesContainer.getChildren().remove(typingBox);
                    addBotMessage(response);
                    tfMessage.setDisable(false);
                    btnSend.setDisable(false);
                    tfMessage.requestFocus();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    messagesContainer.getChildren().remove(typingBox);
                    addBotMessage("❌ Erreur: " + e.getMessage());
                    tfMessage.setDisable(false);
                    btnSend.setDisable(false);
                });
            }
        });
        aiThread.setDaemon(true);
        aiThread.setName("gemini-chat");
        aiThread.start();
    }

    @FXML
    private void clearChat() {
        messagesContainer.getChildren().clear();
        chatbotService.clearHistory();
        addBotMessage("Conversation effacée ! 🔄 Posez-moi une nouvelle question.");
        suggestionsPane.setVisible(true);
        suggestionsPane.setManaged(true);
    }

    // ========== UI Message Builders ==========

    private void addUserMessage(String text) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER_RIGHT);
        wrapper.setPadding(new Insets(2, 0, 2, 50));

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("chat-bubble-user");
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setMaxWidth(280);

        Text msgText = new Text(text);
        msgText.getStyleClass().add("chat-text-user");
        msgText.setWrappingWidth(252);
        TextFlow textFlow = new TextFlow(msgText);
        bubble.getChildren().add(textFlow);

        wrapper.getChildren().add(bubble);
        messagesContainer.getChildren().add(wrapper);
    }

    private void addBotMessage(String text) {
        HBox wrapper = new HBox(8);
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.setPadding(new Insets(2, 50, 2, 0));

        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 18px; -fx-padding: 2 0 0 0;");

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("chat-bubble-bot");
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setMaxWidth(280);

        Text msgText = new Text(text);
        msgText.getStyleClass().add("chat-text-bot");
        msgText.setWrappingWidth(240);
        TextFlow textFlow = new TextFlow(msgText);
        bubble.getChildren().add(textFlow);

        wrapper.getChildren().addAll(avatar, bubble);
        messagesContainer.getChildren().add(wrapper);
    }

    private HBox createTypingIndicator() {
        HBox wrapper = new HBox(8);
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.setPadding(new Insets(2, 50, 2, 0));

        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 18px; -fx-padding: 2 0 0 0;");

        Label dots = new Label("●  ●  ●");
        dots.getStyleClass().add("chat-typing");
        dots.setPadding(new Insets(10, 14, 10, 14));

        wrapper.getChildren().addAll(avatar, dots);
        return wrapper;
    }

    private void addSuggestionChips() {
        String[] suggestions = {
                "Quels lieux visiter ?",
                "Lieu le moins cher ?",
                "Que voir à Tunis ?",
                "Suggère un itinéraire"
        };

        for (String s : suggestions) {
            Button chip = new Button(s);
            chip.getStyleClass().add("chatbot-chip");
            chip.setOnAction(e -> {
                tfMessage.setText(s);
                sendMessage();
            });
            suggestionsPane.getChildren().add(chip);
        }
    }
}
