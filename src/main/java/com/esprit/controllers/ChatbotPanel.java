package com.esprit.controllers;

import com.esprit.services.ChatbotService;
import com.esprit.services.ChatbotService.ChatResponse;
import com.esprit.services.ChatbotService.Lang;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Floating chatbot panel that can be injected into any page.
 * Call {@code ChatbotPanel.install(rootStackPane)} from any controller
 * to add the chat FAB + panel.
 */
public class ChatbotPanel {

    private static final ChatbotService chatbot = new ChatbotService();
    private static boolean installed = false;

    // UI nodes
    private static StackPane overlayPane;
    private static VBox chatPanel;
    private static VBox messagesContainer;
    private static ScrollPane messagesScroll;
    private static TextField inputField;
    private static Button fabButton;
    private static Button aiToggleBtn;
    private static boolean isOpen = false;

    // ─── Quick suggestions ───
    private static final String[][] QUICK_SUGGESTIONS_FR = {
            {"🔍 Restaurants", "restaurant"},
            {"🏨 Hôtels", "hotel"},
            {"📍 À proximité", "restaurants proches"},
            {"⚽ Sport", "activités sport"},
            {"🌿 Nature", "activités nature"},
            {"📅 Planning", "planning"},
            {"📊 Stats", "statistiques"},
    };

    /**
     * Install the chatbot overlay on the given root StackPane.
     * Safe to call multiple times; only installs once per scene graph.
     */
    public static void install(StackPane rootPane) {
        // Check if already installed in this pane
        for (Node child : rootPane.getChildren()) {
            if ("chatbot-overlay".equals(child.getId())) return;
        }

        chatbot.refreshData();
        buildUI(rootPane);
    }

    // ═══════════════════════════════════════════
    //  UI CONSTRUCTION
    // ═══════════════════════════════════════════

    private static void buildUI(StackPane root) {
        // ── Overlay container (fills parent but is mouse-transparent when chat closed) ──
        overlayPane = new StackPane();
        overlayPane.setId("chatbot-overlay");
        overlayPane.setPickOnBounds(false);
        overlayPane.setAlignment(Pos.BOTTOM_RIGHT);
        overlayPane.setPadding(new Insets(0, 28, 28, 0));

        // ── FAB button ──
        fabButton = new Button("💬");
        fabButton.getStyleClass().add("chatbot-fab");
        fabButton.setCursor(Cursor.HAND);
        fabButton.setOnAction(e -> toggleChat());
        StackPane.setAlignment(fabButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fabButton, new Insets(0, 28, 28, 0));

        // ── Chat panel ──
        chatPanel = new VBox();
        chatPanel.getStyleClass().add("chatbot-panel");
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
        chatPanel.setPrefWidth(420);
        chatPanel.setMaxWidth(420);
        chatPanel.setPrefHeight(560);
        chatPanel.setMaxHeight(560);
        StackPane.setAlignment(chatPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatPanel, new Insets(0, 28, 90, 0));

        // ── Header ──
        HBox header = buildHeader();

        // ── Messages area ──
        messagesContainer = new VBox(10);
        messagesContainer.getStyleClass().add("chatbot-messages");
        messagesContainer.setPadding(new Insets(14));

        messagesScroll = new ScrollPane(messagesContainer);
        messagesScroll.getStyleClass().add("chatbot-scroll");
        messagesScroll.setFitToWidth(true);
        messagesScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        messagesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(messagesScroll, Priority.ALWAYS);

        // ── Quick suggestions ──
        FlowPane suggestions = buildSuggestions();

        // ── Input bar ──
        HBox inputBar = buildInputBar();

        chatPanel.getChildren().addAll(header, messagesScroll, suggestions, inputBar);

        overlayPane.getChildren().addAll(chatPanel, fabButton);
        root.getChildren().add(overlayPane);

        // ── Welcome message ──
        Platform.runLater(() -> addBotMessage(chatbot.process("bonjour").text));
    }

    private static HBox buildHeader() {
        Label title = new Label("🤖 Assistant Voyage");
        title.getStyleClass().add("chatbot-header-title");

        // Language selector
        ComboBox<String> langCombo = new ComboBox<>();
        langCombo.getItems().addAll("🇫🇷 FR", "🇬🇧 EN", "🇸🇦 AR");
        langCombo.setValue("🇫🇷 FR");
        langCombo.getStyleClass().add("chatbot-lang-combo");
        langCombo.setPrefWidth(85);
        langCombo.setOnAction(e -> {
            String val = langCombo.getValue();
            if (val.contains("EN")) chatbot.setLang(Lang.EN);
            else if (val.contains("AR")) chatbot.setLang(Lang.AR);
            else chatbot.setLang(Lang.FR);
        });

        // AI mode toggle button
        aiToggleBtn = new Button("🤖 AI");
        aiToggleBtn.getStyleClass().addAll("chatbot-ai-toggle", "chatbot-ai-off");
        aiToggleBtn.setCursor(Cursor.HAND);
        aiToggleBtn.setTooltip(new Tooltip("Activer/Désactiver le mode IA (OpenAI)"));
        aiToggleBtn.setOnAction(e -> toggleAiMode());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("chatbot-close-btn");
        closeBtn.setCursor(Cursor.HAND);
        closeBtn.setOnAction(e -> toggleChat());

        HBox header = new HBox(10, title, spacer, aiToggleBtn, langCombo, closeBtn);
        header.getStyleClass().add("chatbot-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        return header;
    }

    private static void toggleAiMode() {
        boolean nowActive = chatbot.toggleAiMode();

        // Update button style
        if (nowActive) {
            aiToggleBtn.getStyleClass().remove("chatbot-ai-off");
            aiToggleBtn.getStyleClass().add("chatbot-ai-on");
            aiToggleBtn.setText("✨ AI");

            if (!chatbot.isAiConfigured()) {
                addBotMessage("⚠️ **Clé API non configurée.** Veuillez définir votre clé OpenAI dans `OpenAIService.java`.");
                // revert
                chatbot.toggleAiMode();
                aiToggleBtn.getStyleClass().remove("chatbot-ai-on");
                aiToggleBtn.getStyleClass().add("chatbot-ai-off");
                aiToggleBtn.setText("🤖 AI");
                return;
            }

            addBotMessage("✨ **Mode IA activé !**\nJe suis maintenant alimenté par OpenAI. Posez-moi n'importe quelle question sur les voyages en Tunisie !");
        } else {
            aiToggleBtn.getStyleClass().remove("chatbot-ai-on");
            aiToggleBtn.getStyleClass().add("chatbot-ai-off");
            aiToggleBtn.setText("🤖 AI");
            addBotMessage("🔄 **Mode local activé.**\nJe fonctionne maintenant avec le moteur de réponses intégré.");
        }
    }

    private static FlowPane buildSuggestions() {
        FlowPane flow = new FlowPane(8, 8);
        flow.getStyleClass().add("chatbot-suggestions");
        flow.setPadding(new Insets(6, 14, 6, 14));
        flow.setAlignment(Pos.CENTER_LEFT);

        for (String[] sg : QUICK_SUGGESTIONS_FR) {
            Button btn = new Button(sg[0]);
            btn.getStyleClass().add("chatbot-suggestion-btn");
            btn.setCursor(Cursor.HAND);
            btn.setOnAction(e -> sendMessage(sg[1]));
            flow.getChildren().add(btn);
        }
        return flow;
    }

    private static HBox buildInputBar() {
        inputField = new TextField();
        inputField.setPromptText("Tapez votre message...");
        inputField.getStyleClass().add("chatbot-input");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !inputField.getText().isBlank()) {
                sendMessage(inputField.getText().trim());
            }
        });

        Button sendBtn = new Button("➤");
        sendBtn.getStyleClass().add("chatbot-send-btn");
        sendBtn.setCursor(Cursor.HAND);
        sendBtn.setOnAction(e -> {
            if (!inputField.getText().isBlank()) sendMessage(inputField.getText().trim());
        });

        HBox bar = new HBox(10, inputField, sendBtn);
        bar.getStyleClass().add("chatbot-input-bar");
        bar.setPadding(new Insets(10, 14, 12, 14));
        bar.setAlignment(Pos.CENTER);
        return bar;
    }

    // ═══════════════════════════════════════════
    //  CHAT LOGIC
    // ═══════════════════════════════════════════

    private static void sendMessage(String text) {
        addUserMessage(text);
        inputField.clear();

        // Show typing indicator
        HBox typing = createTypingIndicator();
        messagesContainer.getChildren().add(typing);
        scrollToBottom();

        // Process in background
        new Thread(() -> {
            ChatResponse response = chatbot.process(text);
            Platform.runLater(() -> {
                messagesContainer.getChildren().remove(typing);
                addBotMessage(response.text);

                // If there are navigable results, add action buttons
                if (!response.etablissements.isEmpty() || !response.activites.isEmpty()) {
                    addResultActions(response);
                }
            });
        }).start();
    }

    private static void addUserMessage(String text) {
        Label msg = new Label(text);
        msg.getStyleClass().add("chatbot-msg-user");
        msg.setWrapText(true);
        msg.setMaxWidth(300);

        HBox row = new HBox(msg);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(2, 0, 2, 40));
        messagesContainer.getChildren().add(row);
        scrollToBottom();
    }

    private static void addBotMessage(String text) {
        // Parse simple markdown-like formatting
        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("chatbot-msg-bot");
        bubble.setMaxWidth(340);

        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.isBlank()) {
                bubble.getChildren().add(new Region() {{ setPrefHeight(4); }});
                continue;
            }

            TextFlow textflow = new TextFlow();
            textflow.getStyleClass().add("chatbot-text-flow");

            // Parse bold (**text**)
            parseBoldLine(line, textflow);
            bubble.getChildren().add(textflow);
        }

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 40, 2, 0));

        // Fade-in animation
        row.setOpacity(0);
        messagesContainer.getChildren().add(row);
        FadeTransition ft = new FadeTransition(Duration.millis(300), row);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();

        scrollToBottom();
    }

    private static void parseBoldLine(String line, TextFlow flow) {
        // Split by ** for bold
        String[] parts = line.split("\\*\\*");
        boolean bold = false;
        for (String part : parts) {
            if (part.isEmpty()) { bold = !bold; continue; }
            Text t = new Text(part);
            t.getStyleClass().add(bold ? "chatbot-text-bold" : "chatbot-text-normal");
            flow.getChildren().add(t);
            bold = !bold;
        }
    }

    private static void addResultActions(ChatResponse response) {
        FlowPane actions = new FlowPane(6, 6);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 4, 0));

        if (!response.etablissements.isEmpty()) {
            Button btn = new Button("📋 Voir le tableau Étab.");
            btn.getStyleClass().add("chatbot-action-btn");
            btn.setCursor(Cursor.HAND);
            btn.setOnAction(e -> {
                // Navigate via scene root swap
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                            ChatbotPanel.class.getResource("/etablissement_tableau.fxml"));
                    javafx.scene.Parent root = loader.load();
                    btn.getScene().setRoot(root);
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            actions.getChildren().add(btn);
        }

        if (!response.activites.isEmpty()) {
            Button btn = new Button("📋 Voir le tableau Activ.");
            btn.getStyleClass().add("chatbot-action-btn");
            btn.setCursor(Cursor.HAND);
            btn.setOnAction(e -> {
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                            ChatbotPanel.class.getResource("/activite_tableau.fxml"));
                    javafx.scene.Parent root = loader.load();
                    btn.getScene().setRoot(root);
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            actions.getChildren().add(btn);
        }

        HBox row = new HBox(actions);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 40, 4, 8));
        messagesContainer.getChildren().add(row);
        scrollToBottom();
    }

    private static HBox createTypingIndicator() {
        HBox dots = new HBox(5);
        dots.setAlignment(Pos.CENTER_LEFT);
        dots.getStyleClass().add("chatbot-typing");
        dots.setPadding(new Insets(8, 14, 8, 14));

        for (int i = 0; i < 3; i++) {
            Label dot = new Label("●");
            dot.getStyleClass().add("chatbot-typing-dot");
            dots.getChildren().add(dot);

            // Animate each dot with offset
            ScaleTransition st = new ScaleTransition(Duration.millis(500), dot);
            st.setFromX(1); st.setFromY(1);
            st.setToX(1.4); st.setToY(1.4);
            st.setCycleCount(Animation.INDEFINITE);
            st.setAutoReverse(true);
            st.setDelay(Duration.millis(i * 150));
            st.play();
        }

        HBox row = new HBox(dots);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ═══════════════════════════════════════════
    //  TOGGLE / SCROLL
    // ═══════════════════════════════════════════

    private static void toggleChat() {
        isOpen = !isOpen;
        chatPanel.setVisible(isOpen);
        chatPanel.setManaged(isOpen);

        if (isOpen) {
            // Slide up animation
            chatPanel.setTranslateY(30);
            chatPanel.setOpacity(0);
            TranslateTransition tt = new TranslateTransition(Duration.millis(250), chatPanel);
            tt.setFromY(30); tt.setToY(0);
            FadeTransition ft = new FadeTransition(Duration.millis(250), chatPanel);
            ft.setFromValue(0); ft.setToValue(1);
            new ParallelTransition(tt, ft).play();
            fabButton.setText("✕");
            Platform.runLater(() -> inputField.requestFocus());
        } else {
            fabButton.setText("💬");
        }
    }

    private static void scrollToBottom() {
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }
}
