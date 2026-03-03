package com.esprit.controllers;

import com.esprit.entities.Adresse;
import com.esprit.entities.LieuTouristique;
import com.esprit.entities.categorie;
import com.esprit.services.AdresseServices;
import com.esprit.services.GeminiService;
import com.esprit.services.LieuTouristiqueServices;
import com.esprit.services.categorieServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;

/**
 * Controller for the AI Chatbot panel.
 * Uses Gemini to answer tourism questions based on real database data.
 */
public class ChatBotController {

    @FXML private VBox messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField tfMessage;
    @FXML private Button btnSend;
    @FXML private Button btnClear;
    @FXML private Label lblStatus;
    @FXML private FlowPane suggestionsPane;

    private GeminiService geminiService;
    private String dbContext = "";

    @FXML
    public void initialize() {
        geminiService = new GeminiService();

        // Load database context in background
        Thread dbThread = new Thread(() -> {
            String ctx = buildDatabaseContext();
            Platform.runLater(() -> {
                dbContext = ctx;
                System.out.println("✅ ChatBot database context loaded");
            });
        });
        dbThread.setDaemon(true);
        dbThread.start();

        // Add welcome message
        addBotMessage("Bonjour ! 👋 Je suis votre assistant touristique IA.\n\nJe connais tous les lieux touristiques de votre base de données. Posez-moi vos questions !");

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

        // Send to Gemini in background
        Thread aiThread = new Thread(() -> {
            try {
                String response = geminiService.chat(msg, dbContext);
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
        geminiService.clearHistory();
        addBotMessage("Conversation effacée ! 🔄 Posez-moi une nouvelle question.");
        suggestionsPane.setVisible(true);
        suggestionsPane.setManaged(true);

        // Refresh DB context
        Thread dbThread = new Thread(() -> {
            String ctx = buildDatabaseContext();
            Platform.runLater(() -> dbContext = ctx);
        });
        dbThread.setDaemon(true);
        dbThread.start();
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

    // ========== Database Context Builder ==========

    /**
     * Build a comprehensive text summary of the database for AI context.
     * This allows the AI to answer questions based on real data.
     */
    private String buildDatabaseContext() {
        StringBuilder sb = new StringBuilder();
        try {
            // Categories
            categorieServices catService = new categorieServices();
            List<categorie> categories = catService.afficher();
            sb.append("=== CATÉGORIES (").append(categories.size()).append(") ===\n");
            for (categorie c : categories) {
                sb.append("- ").append(c.getNomcategorie());
                if (c.getDescription() != null && !c.getDescription().isEmpty()) {
                    sb.append(": ").append(c.getDescription());
                }
                sb.append("\n");
            }

            // Addresses
            AdresseServices adrService = new AdresseServices();
            List<Adresse> adresses = adrService.afficher();
            sb.append("\n=== ADRESSES (").append(adresses.size()).append(") ===\n");
            for (Adresse a : adresses) {
                sb.append("- ID ").append(a.getId_adresse()).append(": ")
                        .append(a.getRue()).append(", ").append(a.getVille())
                        .append(" (lat: ").append(a.getLatitude())
                        .append(", lon: ").append(a.getLongitude()).append(")\n");
            }

            // Lieux Touristiques
            LieuTouristiqueServices lieuService = new LieuTouristiqueServices();
            List<LieuTouristique> lieux = lieuService.afficher();
            sb.append("\n=== LIEUX TOURISTIQUES (").append(lieux.size()).append(") ===\n");

            // Build category name map for display
            java.util.Map<Integer, String> catMap = new java.util.HashMap<>();
            for (categorie c : categories) catMap.put(c.getIdcategorie(), c.getNomcategorie());
            java.util.Map<Integer, String> adrMap = new java.util.HashMap<>();
            for (Adresse a : adresses) adrMap.put(a.getId_adresse(), a.getVille() + " - " + a.getRue());

            for (LieuTouristique l : lieux) {
                sb.append("- ").append(l.getNom())
                        .append(" | Ville: ").append(l.getVille())
                        .append(" | Prix: ").append(l.getPrix()).append(" TND")
                        .append(" | Catégorie: ").append(catMap.getOrDefault(l.getId_categorie(), "N/A"))
                        .append(" | Adresse: ").append(adrMap.getOrDefault(l.getId_adresse(), "N/A"))
                        .append(" | Statut: ").append(l.getStatut() == 1 ? "Actif" : "Inactif");
                if (l.getDescription() != null && !l.getDescription().isEmpty()) {
                    sb.append(" | Desc: ").append(l.getDescription());
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            sb.append("(Erreur chargement BD: ").append(e.getMessage()).append(")");
        }
        return sb.toString();
    }
}
