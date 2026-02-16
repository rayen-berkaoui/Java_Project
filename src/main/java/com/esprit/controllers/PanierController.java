package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;

import com.esprit.entities.Panier;
import com.esprit.entities.Reservation;
import com.esprit.entities.Etablissement;
import com.esprit.entities.utilisateur;
import com.esprit.services.PanierService;
import com.esprit.services.ReservationService;
import com.esprit.services.EtablissementService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class PanierController {

    @FXML private HBox titleBar;
    @FXML private Label userNameLabel;
    @FXML private Label totalItemsLabel;
    @FXML private Label enAttenteLabel;
    @FXML private Label totalPrixLabel;
    @FXML private Label messageLabel;
    @FXML private HBox panierCarousel;
    @FXML private Label pageLabel;
    @FXML private StackPane panierBadge;
    @FXML private Label panierCountLabel;

    private double xOffset = 0;
    private double yOffset = 0;
    private utilisateur currentUser;
    private final PanierService panierService = new PanierService();
    private final ReservationService reservationService = new ReservationService();
    private final EtablissementService etabService = new EtablissementService();

    private List<Panier> panierItems;
    private int currentPage = 0;
    private static final int CARDS_PER_PAGE = 3;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Promo code system
    private static final String[] PROMO_CODES = {"SMART10", "TRAVEL20", "VIP15", "GOLD25", "WELCOME5"};
    private static final double[] PROMO_DISCOUNTS = {10, 20, 15, 25, 5};
    private String appliedPromo = null;
    private double discountPercent = 0;

    @FXML
    public void initialize() {
        if (titleBar != null) {
            titleBar.setOnMousePressed(event -> { xOffset = event.getSceneX(); yOffset = event.getSceneY(); });
            titleBar.setOnMouseDragged(event -> {
                Stage stage = (Stage) titleBar.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }
    }

    public void setUser(utilisateur user) {
        this.currentUser = user;
        if (user != null) {
            if (userNameLabel != null) userNameLabel.setText(user.getNom() + " " + user.getPrenom());
            loadData();
        }
    }

    private void loadData() {
        if (currentUser == null) return;
        List<Panier> allItems = panierService.getPanierByClient(currentUser.getId());
        for (Panier p : allItems) {
            if (p.getNomEtablissement() == null || p.getNomEtablissement().isEmpty()) {
                Etablissement etab = etabService.getById(p.getIdEtablissement());
                if (etab != null) p.setNomEtablissement(etab.getNom());
            }
        }
        panierItems = allItems.stream()
                .filter(p -> "en_attente".equals(p.getStatutItem()))
                .collect(Collectors.toList());
        updateStats();
        updateBadge();
        renderCarousel();
    }

    private void updateStats() {
        int total = panierItems.size();
        long enAttente = panierItems.stream().filter(p -> "en_attente".equals(p.getStatutItem())).count();
        double totalPrix = panierItems.stream()
                .filter(p -> "en_attente".equals(p.getStatutItem()))
                .mapToDouble(Panier::getPrixEstime).sum();
        if (totalItemsLabel != null) totalItemsLabel.setText(String.valueOf(total));
        if (enAttenteLabel != null) enAttenteLabel.setText(String.valueOf(enAttente));
        if (totalPrixLabel != null) totalPrixLabel.setText(String.format("%.2f DT", totalPrix));
    }

    private void updateBadge() {
        if (panierBadge != null && panierCountLabel != null) {
            int count = panierItems.size();
            panierBadge.setVisible(count > 0);
            panierCountLabel.setText(String.valueOf(count));
        }
    }

    private void renderCarousel() {
        if (panierCarousel == null) return;
        panierCarousel.getChildren().clear();
        int start = currentPage * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, panierItems.size());
        for (int i = start; i < end; i++) {
            VBox card = buildPanierCard(panierItems.get(i));
            HBox.setHgrow(card, Priority.ALWAYS);
            panierCarousel.getChildren().add(card);
        }
        if (panierCarousel.getChildren().isEmpty()) {
            VBox emptyState = new VBox(16);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(40));
            emptyState.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 14;");
            Label emptyIcon = new Label("\uD83D\uDED2");
            emptyIcon.setStyle("-fx-font-size: 40;");
            Label emptyTitle = new Label("Votre panier est vide");
            emptyTitle.setStyle("-fx-text-fill: #888; -fx-font-size: 16; -fx-font-weight: bold;");
            Label emptyDesc = new Label("Explorez les destinations pour ajouter des articles !");
            emptyDesc.setStyle("-fx-text-fill: #555; -fx-font-size: 12;");
            emptyDesc.setWrapText(true);
            Button exploreBtn = new Button("\uD83C\uDF0D Explorer les Destinations");
            exploreBtn.getStyleClass().add("dashboard-button");
            exploreBtn.setStyle("-fx-padding: 10 24; -fx-font-size: 12;");
            exploreBtn.setOnAction(e -> handleBackToMain());
            emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc, exploreBtn);
            HBox.setHgrow(emptyState, Priority.ALWAYS);
            panierCarousel.getChildren().add(emptyState);
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) panierItems.size() / CARDS_PER_PAGE));
        if (pageLabel != null) pageLabel.setText((currentPage + 1) + " / " + totalPages);
    }

    // ================================================================
    // BUILD PANIER CARD  PROFESSIONAL DESIGN
    // ================================================================
    private VBox buildPanierCard(Panier p) {
        VBox card = new VBox(10);
        card.setMinWidth(310);
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color: rgba(15,15,15,0.95); -fx-padding: 18; -fx-background-radius: 14; " +
                "-fx-border-color: rgba(255,215,0,0.15); -fx-border-radius: 14; -fx-border-width: 1;");

        //  TOP: icon + name + price 
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(48, 48);
        iconPane.setStyle("-fx-background-color: rgba(255,215,0,0.1); -fx-background-radius: 12;");
        String icon = getServiceIcon(p.getTypeService());
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20;");
        iconPane.getChildren().add(iconLabel);

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        String displayName = p.getNomEtablissement() != null && !p.getNomEtablissement().isEmpty()
                ? p.getNomEtablissement() : p.getTypeService();
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        nameLabel.setMaxWidth(200);
        Label typeLabel = new Label(p.getTypeService());
        typeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 10;");
        nameBox.getChildren().addAll(nameLabel, typeLabel);

        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label prixLabel = new Label(String.format("%.2f DT", p.getPrixEstime()));
        prixLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");
        priceBox.getChildren().add(prixLabel);

        header.getChildren().addAll(iconPane, nameBox, priceBox);

        //  DETAIL GRID 
        VBox details = new VBox(6);
        details.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 12; -fx-background-radius: 8;");
        HBox row1 = new HBox(16);
        row1.getChildren().addAll(
            buildDetailChip("\uD83D\uDCC5", (p.getDateDebut() != null ? p.getDateDebut().format(DTF) : "N/A")
                + " \u2192 " + (p.getDateFin() != null ? p.getDateFin().format(DTF) : "N/A"))
        );
        HBox row2 = new HBox(16);
        row2.getChildren().addAll(
            buildDetailChip("\uD83D\uDC65", p.getNbPersonnes() + " personnes"),
            buildDetailChip("\u23F3", "En attente")
        );
        details.getChildren().addAll(row1, row2);

        // Discount info if promo applied
        if (appliedPromo != null && discountPercent > 0) {
            double discounted = p.getPrixEstime() * (1 - discountPercent / 100.0);
            HBox promoRow = new HBox(8);
            promoRow.setAlignment(Pos.CENTER);
            promoRow.setStyle("-fx-background-color: rgba(81,207,102,0.06); -fx-padding: 8; -fx-background-radius: 8;");
            Label oldPrice = new Label(String.format("%.2f DT", p.getPrixEstime()));
            oldPrice.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11; -fx-strikethrough: true;");
            Label arrow = new Label("\u2192");
            arrow.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
            Label newPrice = new Label(String.format("%.2f DT", discounted));
            newPrice.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 13; -fx-font-weight: bold;");
            Label discLabel = new Label("-" + (int)discountPercent + "%");
            discLabel.setStyle("-fx-background-color: rgba(81,207,102,0.2); -fx-text-fill: #51CF66; -fx-padding: 2 8; -fx-background-radius: 6; -fx-font-size: 9; -fx-font-weight: bold;");
            promoRow.getChildren().addAll(oldPrice, arrow, newPrice, discLabel);
            details.getChildren().add(promoRow);
        }

        //  STATUT BADGE 
        HBox statutRow = new HBox();
        statutRow.setAlignment(Pos.CENTER);
        Label statutBadge = new Label("\u23F3 En attente de confirmation");
        statutBadge.setStyle("-fx-background-color: rgba(255,215,0,0.08); -fx-text-fill: #FFD700; -fx-padding: 5 14; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold;");
        statutRow.getChildren().add(statutBadge);

        //  ACTION BUTTONS 
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER);

        Button confirmBtn = new Button("\u2705 Confirmer");
        confirmBtn.setStyle("-fx-background-color: rgba(81,207,102,0.12); -fx-text-fill: #51CF66; -fx-padding: 8 18; -fx-background-radius: 8; -fx-font-size: 11; -fx-font-weight: bold; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> showPaymentMethodDialog(p));

        Button deleteBtn = new Button("\uD83D\uDDD1 Supprimer");
        deleteBtn.setStyle("-fx-background-color: rgba(255,107,107,0.12); -fx-text-fill: #FF6B6B; -fx-padding: 8 18; -fx-background-radius: 8; -fx-font-size: 11; -fx-font-weight: bold; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deletePanierItem(p));

        buttons.getChildren().addAll(confirmBtn, deleteBtn);

        card.getChildren().addAll(header, details, statutRow, buttons);
        return card;
    }

    private HBox buildDetailChip(String icon, String text) {
        HBox chip = new HBox(4);
        chip.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(chip, Priority.ALWAYS);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 10;");
        Label textLbl = new Label(text);
        textLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        chip.getChildren().addAll(iconLbl, textLbl);
        return chip;
    }

    private String getServiceIcon(String type) {
        if (type == null) return "\uD83C\uDFAF";
        if (type.toLowerCase().contains("voyage")) return "\uD83C\uDFDD";
        if (type.toLowerCase().contains("caf")) return "\u2615";
        if (type.toLowerCase().contains("rest")) return "\uD83C\uDF55";
        return "\uD83C\uDFAF";
    }

    // ================================================================
    // PROMO CODE DIALOG
    // ================================================================
    @FXML public void handleApplyPromo() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Code Promo");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setAlignment(Pos.CENTER);

        Label titleLbl = new Label("\uD83C\uDF89 Code Promotionnel");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");

        Label descLbl = new Label("Entrez votre code promo pour beneficier d'une reduction");
        descLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");

        HBox inputRow = new HBox(10);
        inputRow.setAlignment(Pos.CENTER);
        TextField promoField = new TextField();
        promoField.setPromptText("Ex: SMART10, TRAVEL20...");
        promoField.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-prompt-text-fill: #555; -fx-padding: 12 16; -fx-background-radius: 10; -fx-font-size: 14; -fx-border-color: rgba(255,215,0,0.2); -fx-border-radius: 10; -fx-pref-width: 250;");
        inputRow.getChildren().add(promoField);

        Label resultLbl = new Label("");
        resultLbl.setStyle("-fx-font-size: 12;");

        VBox hintBox = new VBox(6);
        hintBox.setStyle("-fx-background-color: rgba(255,215,0,0.04); -fx-padding: 12; -fx-background-radius: 8;");
        Label hintTitle = new Label("\uD83D\uDCA1 Codes disponibles:");
        hintTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11; -fx-font-weight: bold;");
        HBox codesRow = new HBox(8);
        codesRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < PROMO_CODES.length; i++) {
            Label code = new Label(PROMO_CODES[i] + " (-" + (int)PROMO_DISCOUNTS[i] + "%)");
            code.setStyle("-fx-background-color: rgba(255,215,0,0.1); -fx-text-fill: #FFD700; -fx-padding: 4 8; -fx-background-radius: 6; -fx-font-size: 9;");
            codesRow.getChildren().add(code);
        }
        hintBox.getChildren().addAll(hintTitle, codesRow);

        content.getChildren().addAll(titleLbl, descLbl, inputRow, resultLbl, hintBox);
        dp.setContent(content);

        ButtonType applyType = new ButtonType("Appliquer", ButtonBar.ButtonData.OK_DONE);
        ButtonType removeType = new ButtonType("Retirer Promo", ButtonBar.ButtonData.LEFT);
        ButtonType cancelType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(applyType, removeType, cancelType);

        Button applyBtn = (Button) dp.lookupButton(applyType);
        applyBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 8;");

        Button removeBtn = (Button) dp.lookupButton(removeType);
        removeBtn.setStyle("-fx-background-color: rgba(255,107,107,0.15); -fx-text-fill: #FF6B6B; -fx-padding: 8 16; -fx-background-radius: 8;");
        removeBtn.setVisible(appliedPromo != null);

        applyBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String code = promoField.getText().trim().toUpperCase();
            boolean found = false;
            for (int i = 0; i < PROMO_CODES.length; i++) {
                if (PROMO_CODES[i].equals(code)) {
                    appliedPromo = code;
                    discountPercent = PROMO_DISCOUNTS[i];
                    found = true;
                    break;
                }
            }
            if (found) {
                showMessage("\uD83C\uDF89 Code promo " + appliedPromo + " applique ! -" + (int)discountPercent + "% de reduction", true);
                loadData();
            } else {
                resultLbl.setText("\u274C Code promo invalide");
                resultLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12;");
                event.consume();
            }
        });

        removeBtn.setOnAction(e -> {
            appliedPromo = null;
            discountPercent = 0;
            showMessage("Code promo retire.", true);
            loadData();
            dialog.close();
        });

        dialog.setResultConverter(bt -> null);
        dialog.showAndWait();
    }

    // ================================================================
    // PAYMENT METHOD DIALOG
    // ================================================================
    private void showPaymentMethodDialog(Panier p) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Mode de Paiement");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setAlignment(Pos.CENTER);

        Label titleLbl = new Label("\uD83D\uDCB3 Choisir le mode de paiement");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");

        String itemName = p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService();
        Label itemLbl = new Label(itemName);
        itemLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14;");

        double finalPrice = p.getPrixEstime();
        if (appliedPromo != null && discountPercent > 0) {
            finalPrice = p.getPrixEstime() * (1 - discountPercent / 100.0);
        }
        double fp = finalPrice;

        VBox priceSection = new VBox(4);
        priceSection.setAlignment(Pos.CENTER);
        if (appliedPromo != null && discountPercent > 0) {
            Label origPriceLbl = new Label(String.format("Prix original: %.2f DT", p.getPrixEstime()));
            origPriceLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12; -fx-strikethrough: true;");
            Label discountLbl = new Label(String.format("\uD83C\uDF89 -%d%% avec %s", (int)discountPercent, appliedPromo));
            discountLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 12; -fx-font-weight: bold;");
            Label montantLbl = new Label(String.format("Total: %.2f DT", finalPrice));
            montantLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");
            priceSection.getChildren().addAll(origPriceLbl, discountLbl, montantLbl);
        } else {
            Label montantLbl = new Label(String.format("Montant: %.2f DT", finalPrice));
            montantLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");
            priceSection.getChildren().add(montantLbl);
        }

        HBox paymentButtons = new HBox(20);
        paymentButtons.setAlignment(Pos.CENTER);

        VBox cashBox = new VBox(10);
        cashBox.setAlignment(Pos.CENTER);
        cashBox.setPadding(new Insets(20));
        cashBox.setStyle("-fx-background-color: rgba(81,207,102,0.08); -fx-background-radius: 14; -fx-border-color: rgba(81,207,102,0.2); -fx-border-radius: 14; -fx-border-width: 1; -fx-cursor: hand; -fx-pref-width: 180;");
        Label cashIcon = new Label("\uD83D\uDCB5");
        cashIcon.setStyle("-fx-font-size: 36;");
        Label cashLbl = new Label("Especes");
        cashLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 14; -fx-font-weight: bold;");
        Label cashDesc = new Label("Paiement immediat");
        cashDesc.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        cashBox.getChildren().addAll(cashIcon, cashLbl, cashDesc);

        VBox cardBox = new VBox(10);
        cardBox.setAlignment(Pos.CENTER);
        cardBox.setPadding(new Insets(20));
        cardBox.setStyle("-fx-background-color: rgba(100,181,246,0.08); -fx-background-radius: 14; -fx-border-color: rgba(100,181,246,0.2); -fx-border-radius: 14; -fx-border-width: 1; -fx-cursor: hand; -fx-pref-width: 180;");
        Label cardIcon = new Label("\uD83D\uDCB3");
        cardIcon.setStyle("-fx-font-size: 36;");
        Label cardLbl = new Label("Carte Bancaire");
        cardLbl.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 14; -fx-font-weight: bold;");
        Label cardDesc = new Label("Visa / Mastercard");
        cardDesc.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        cardBox.getChildren().addAll(cardIcon, cardLbl, cardDesc);

        paymentButtons.getChildren().addAll(cashBox, cardBox);
        content.getChildren().addAll(titleLbl, itemLbl, priceSection, paymentButtons);
        dp.setContent(content);

        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().add(cancelType);

        cashBox.setOnMouseClicked(e -> { dialog.setResult("cash"); dialog.close(); });
        cashBox.setOnMouseEntered(e -> cashBox.setStyle("-fx-background-color: rgba(81,207,102,0.15); -fx-background-radius: 14; -fx-border-color: rgba(81,207,102,0.4); -fx-border-radius: 14; -fx-border-width: 1; -fx-cursor: hand; -fx-pref-width: 180;"));
        cashBox.setOnMouseExited(e -> cashBox.setStyle("-fx-background-color: rgba(81,207,102,0.08); -fx-background-radius: 14; -fx-border-color: rgba(81,207,102,0.2); -fx-border-radius: 14; -fx-border-width: 1; -fx-cursor: hand; -fx-pref-width: 180;"));

        cardBox.setOnMouseClicked(e -> { dialog.setResult("card"); dialog.close(); });
        cardBox.setOnMouseEntered(e -> cardBox.setStyle("-fx-background-color: rgba(100,181,246,0.15); -fx-background-radius: 14; -fx-border-color: rgba(100,181,246,0.4); -fx-border-radius: 14; -fx-border-width: 1; -fx-cursor: hand; -fx-pref-width: 180;"));
        cardBox.setOnMouseExited(e -> cardBox.setStyle("-fx-background-color: rgba(100,181,246,0.08); -fx-background-radius: 14; -fx-border-color: rgba(100,181,246,0.2); -fx-border-radius: 14; -fx-border-width: 1; -fx-cursor: hand; -fx-pref-width: 180;"));

        dialog.setResultConverter(bt -> null);
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            if ("cash".equals(result.get())) {
                processPaymentCash(p, fp);
            } else if ("card".equals(result.get())) {
                showCardPaymentDialog(p, fp);
            }
        }
    }

    // ================================================================
    // CASH PAYMENT
    // ================================================================
    private void processPaymentCash(Panier p, double finalPrice) {
        Reservation r = new Reservation();
        r.setIdPanier(p.getIdPanier());
        r.setDatePaiement(LocalDateTime.now());
        r.setMontantTotal(finalPrice);
        r.setModePaiement("Especes");
        r.setStatutPaiement("Paye");

        if (reservationService.ajouter(r)) {
            panierService.modifier(updatePanierStatut(p, "confirme"));
            showPaymentSuccessOverlay(p, r, finalPrice);
            loadData();
        } else {
            showMessage("Echec du paiement.", false);
        }
    }

    // ================================================================
    // CARD PAYMENT DIALOG
    // ================================================================
    private void showCardPaymentDialog(Panier p, double finalPrice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Paiement par Carte");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a1a1a;");

        Label titleLbl = new Label("\uD83D\uDCB3 Paiement par Carte Bancaire");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");

        HBox amountBox = new HBox();
        amountBox.setAlignment(Pos.CENTER);
        amountBox.setStyle("-fx-background-color: rgba(255,215,0,0.08); -fx-padding: 14; -fx-background-radius: 10;");
        Label amountLbl = new Label(String.format("Montant: %.2f DT", finalPrice));
        amountLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");
        amountBox.getChildren().add(amountLbl);

        VBox cardVisual = new VBox(12);
        cardVisual.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a1a2e, #16213e); -fx-padding: 20; -fx-background-radius: 14; -fx-border-color: rgba(100,181,246,0.3); -fx-border-radius: 14; -fx-border-width: 1;");

        VBox numBox = new VBox(4);
        Label numLbl = new Label("Numero de carte");
        numLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        TextField cardNumField = new TextField();
        cardNumField.setPromptText("1234 5678 9012 3456");
        cardNumField.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-prompt-text-fill: #555; -fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 14; -fx-border-color: rgba(100,181,246,0.2); -fx-border-radius: 8;");
        numBox.getChildren().addAll(numLbl, cardNumField);

        VBox holderBox = new VBox(4);
        Label holderLbl = new Label("Titulaire de la carte");
        holderLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        TextField holderField = new TextField();
        holderField.setPromptText("NOM PRENOM");
        holderField.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-prompt-text-fill: #555; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: rgba(100,181,246,0.2); -fx-border-radius: 8;");
        holderBox.getChildren().addAll(holderLbl, holderField);

        HBox expiryRow = new HBox(12);
        VBox expiryBox = new VBox(4);
        HBox.setHgrow(expiryBox, Priority.ALWAYS);
        Label expiryLbl = new Label("Date d'expiration");
        expiryLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/AA");
        expiryField.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-prompt-text-fill: #555; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: rgba(100,181,246,0.2); -fx-border-radius: 8;");
        expiryBox.getChildren().addAll(expiryLbl, expiryField);

        VBox cvvBox = new VBox(4);
        HBox.setHgrow(cvvBox, Priority.ALWAYS);
        Label cvvLbl = new Label("CVV");
        cvvLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        PasswordField cvvField = new PasswordField();
        cvvField.setPromptText("123");
        cvvField.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-prompt-text-fill: #555; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: rgba(100,181,246,0.2); -fx-border-radius: 8;");
        cvvBox.getChildren().addAll(cvvLbl, cvvField);

        expiryRow.getChildren().addAll(expiryBox, cvvBox);
        cardVisual.getChildren().addAll(numBox, holderBox, expiryRow);

        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");

        content.getChildren().addAll(titleLbl, amountBox, cardVisual, errorLbl);
        dp.setContent(content);

        ButtonType payType = new ButtonType("\uD83D\uDD12 Payer " + String.format("%.2f DT", finalPrice), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(payType, cancelType);

        Button payButton = (Button) dp.lookupButton(payType);
        payButton.setStyle("-fx-background-color: linear-gradient(to right, #64B5F6, #42A5F5); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; -fx-font-size: 13;");

        payButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String cardNum = cardNumField.getText().trim().replaceAll("\\s", "");
            String holder = holderField.getText().trim();
            String expiry = expiryField.getText().trim();
            String cvv = cvvField.getText().trim();

            if (cardNum.length() < 13 || cardNum.length() > 19) {
                errorLbl.setText("Numero de carte invalide (13-19 chiffres)");
                event.consume(); return;
            }
            if (holder.isEmpty()) {
                errorLbl.setText("Veuillez saisir le nom du titulaire");
                event.consume(); return;
            }
            if (!expiry.matches("\\d{2}/\\d{2}")) {
                errorLbl.setText("Format de date invalide (MM/AA)");
                event.consume(); return;
            }
            if (cvv.length() < 3 || cvv.length() > 4) {
                errorLbl.setText("CVV invalide (3-4 chiffres)");
                event.consume(); return;
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == payType) {
            processPaymentCard(p, finalPrice);
        }
    }

    private void processPaymentCard(Panier p, double finalPrice) {
        Reservation r = new Reservation();
        r.setIdPanier(p.getIdPanier());
        r.setDatePaiement(LocalDateTime.now());
        r.setMontantTotal(finalPrice);
        r.setModePaiement("Carte Bancaire");
        r.setStatutPaiement("Paye");

        if (reservationService.ajouter(r)) {
            panierService.modifier(updatePanierStatut(p, "confirme"));
            showPaymentSuccessOverlay(p, r, finalPrice);
            loadData();
        } else {
            showMessage("Echec du paiement par carte.", false);
        }
    }

    // ================================================================
    // ANIMATED PAYMENT SUCCESS OVERLAY WITH CONFETTI
    // ================================================================
    private void showPaymentSuccessOverlay(Panier p, Reservation r, double finalPrice) {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        StackPane rootPane = findRootStackPane(stage);
        if (rootPane == null) {
            showMessage("\u2705 Paiement confirme ! Code: " + r.getCodeConfirmation(), true);
            return;
        }

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.85);");
        overlay.setOpacity(0);

        Pane confettiPane = new Pane();
        confettiPane.setMouseTransparent(true);
        confettiPane.setPickOnBounds(false);

        VBox successCard = new VBox(18);
        successCard.setMaxWidth(420);
        successCard.setAlignment(Pos.CENTER);
        successCard.setPadding(new Insets(40, 36, 36, 36));
        successCard.setStyle("-fx-background-color: rgba(15,15,15,0.98); -fx-background-radius: 20; " +
                "-fx-border-color: rgba(81,207,102,0.3); -fx-border-radius: 20; -fx-border-width: 2;");

        StackPane checkCircle = new StackPane();
        checkCircle.setPrefSize(80, 80);
        checkCircle.setMaxSize(80, 80);
        checkCircle.setStyle("-fx-background-color: rgba(81,207,102,0.15); -fx-background-radius: 40;");
        Label checkIcon = new Label("\u2705");
        checkIcon.setStyle("-fx-font-size: 40;");
        checkCircle.getChildren().add(checkIcon);
        checkCircle.setScaleX(0);
        checkCircle.setScaleY(0);

        Label successTitle = new Label("Paiement Reussi !");
        successTitle.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 24; -fx-font-weight: bold;");
        successTitle.setOpacity(0);

        Label successDesc = new Label("Votre reservation a ete confirmee avec succes");
        successDesc.setStyle("-fx-text-fill: #aaa; -fx-font-size: 13;");
        successDesc.setOpacity(0);

        VBox receipt = new VBox(8);
        receipt.setStyle("-fx-background-color: rgba(255,215,0,0.04); -fx-padding: 16; -fx-background-radius: 12; -fx-border-color: rgba(255,215,0,0.1); -fx-border-radius: 12; -fx-border-width: 1;");
        receipt.setOpacity(0);

        String itemName = p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService();
        addReceiptRow(receipt, "\uD83C\uDFAF Service", itemName);
        addReceiptRow(receipt, "\uD83D\uDC65 Personnes", String.valueOf(p.getNbPersonnes()));
        if (appliedPromo != null) {
            addReceiptRow(receipt, "\uD83C\uDF89 Promo", appliedPromo + " (-" + (int)discountPercent + "%)");
        }

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: rgba(255,215,0,0.15);");
        receipt.getChildren().add(divider);

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label totalLblLeft = new Label("Total Paye");
        totalLblLeft.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label totalLblRight = new Label(String.format("%.2f DT", finalPrice));
        totalLblRight.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");
        totalRow.getChildren().addAll(totalLblLeft, spacer, totalLblRight);
        receipt.getChildren().add(totalRow);

        HBox codeBadge = new HBox(8);
        codeBadge.setAlignment(Pos.CENTER);
        codeBadge.setStyle("-fx-background-color: rgba(255,215,0,0.08); -fx-padding: 12 20; -fx-background-radius: 10;");
        codeBadge.setOpacity(0);
        Label codeIcon = new Label("\uD83D\uDD11");
        codeIcon.setStyle("-fx-font-size: 14;");
        Label codeLbl = new Label(r.getCodeConfirmation());
        codeLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold; -fx-font-family: 'Consolas';");
        codeBadge.getChildren().addAll(codeIcon, codeLbl);

        Button closeBtn = new Button("\u2714 Fermer");
        closeBtn.setStyle("-fx-background-color: linear-gradient(to right, #51CF66, #40C057); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 40; -fx-background-radius: 10; -fx-font-size: 14; -fx-cursor: hand;");
        closeBtn.setOpacity(0);

        successCard.getChildren().addAll(checkCircle, successTitle, successDesc, receipt, codeBadge, closeBtn);
        overlay.getChildren().addAll(confettiPane, successCard);
        rootPane.getChildren().add(overlay);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
        fadeIn.setToValue(1);

        ScaleTransition scaleCheck = new ScaleTransition(Duration.millis(500), checkCircle);
        scaleCheck.setToX(1); scaleCheck.setToY(1);
        scaleCheck.setInterpolator(Interpolator.SPLINE(0.175, 0.885, 0.32, 1.0));

        FadeTransition fadeTitle = new FadeTransition(Duration.millis(300), successTitle);
        fadeTitle.setToValue(1); fadeTitle.setDelay(Duration.millis(200));
        FadeTransition fadeDesc = new FadeTransition(Duration.millis(300), successDesc);
        fadeDesc.setToValue(1); fadeDesc.setDelay(Duration.millis(300));
        FadeTransition fadeReceipt = new FadeTransition(Duration.millis(400), receipt);
        fadeReceipt.setToValue(1); fadeReceipt.setDelay(Duration.millis(400));
        FadeTransition fadeCode = new FadeTransition(Duration.millis(300), codeBadge);
        fadeCode.setToValue(1); fadeCode.setDelay(Duration.millis(600));
        FadeTransition fadeClose = new FadeTransition(Duration.millis(300), closeBtn);
        fadeClose.setToValue(1); fadeClose.setDelay(Duration.millis(700));

        ParallelTransition allAnims = new ParallelTransition(fadeIn, scaleCheck, fadeTitle, fadeDesc, fadeReceipt, fadeCode, fadeClose);
        allAnims.play();

        spawnConfetti(confettiPane, stage.getWidth(), stage.getHeight());

        Glow glow = new Glow(0);
        checkCircle.setEffect(glow);
        Timeline glowTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0)),
            new KeyFrame(Duration.millis(800), new KeyValue(glow.levelProperty(), 0.6)),
            new KeyFrame(Duration.millis(1600), new KeyValue(glow.levelProperty(), 0))
        );
        glowTimeline.setCycleCount(3);
        glowTimeline.play();

        closeBtn.setOnAction(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), overlay);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> rootPane.getChildren().remove(overlay));
            fadeOut.play();
        });
    }

    private void addReceiptRow(VBox receipt, String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label leftLbl = new Label(label);
        leftLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label rightLbl = new Label(value);
        rightLbl.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
        row.getChildren().addAll(leftLbl, sp, rightLbl);
        receipt.getChildren().add(row);
    }

    private void spawnConfetti(Pane container, double width, double height) {
        Random rand = new Random();
        String[] colors = {"#FFD700", "#51CF66", "#64B5F6", "#FF6B6B", "#B266FF", "#FF8C00", "#FF69B4", "#00CED1"};
        for (int i = 0; i < 60; i++) {
            Rectangle particle = new Rectangle(rand.nextInt(8) + 4, rand.nextInt(12) + 4);
            particle.setFill(Color.web(colors[rand.nextInt(colors.length)]));
            particle.setRotate(rand.nextInt(360));
            particle.setX(rand.nextDouble() * width);
            particle.setY(-20);
            particle.setOpacity(0.9);
            container.getChildren().add(particle);

            double targetY = height + 50;
            double drift = (rand.nextDouble() - 0.5) * 200;
            int duration = 2000 + rand.nextInt(2000);
            int delay = rand.nextInt(800);

            TranslateTransition fall = new TranslateTransition(Duration.millis(duration), particle);
            fall.setToY(targetY);
            fall.setToX(drift);
            fall.setDelay(Duration.millis(delay));
            fall.setInterpolator(Interpolator.EASE_IN);

            RotateTransition spin = new RotateTransition(Duration.millis(duration), particle);
            spin.setByAngle(360 + rand.nextInt(720));
            spin.setDelay(Duration.millis(delay));

            FadeTransition fade = new FadeTransition(Duration.millis(duration), particle);
            fade.setFromValue(0.9);
            fade.setToValue(0);
            fade.setDelay(Duration.millis(delay + duration / 2));

            new ParallelTransition(fall, spin, fade).play();
        }
    }

    private StackPane findRootStackPane(Stage stage) {
        Scene scene = stage.getScene();
        if (scene == null) return null;
        Parent root = scene.getRoot();
        if (root instanceof StackPane) return (StackPane) root;
        StackPane wrapper = new StackPane();
        wrapper.getChildren().add(root);
        scene.setRoot(wrapper);
        return wrapper;
    }

    private Panier updatePanierStatut(Panier p, String statut) {
        p.setStatutItem(statut);
        return p;
    }

    // ================================================================
    // DELETE PANIER ITEM
    // ================================================================
    private void deletePanierItem(Panier p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Supprimer l'article");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setAlignment(Pos.CENTER);

        StackPane warnIcon = new StackPane();
        warnIcon.setPrefSize(60, 60);
        warnIcon.setStyle("-fx-background-color: rgba(255,107,107,0.1); -fx-background-radius: 30;");
        Label wIcon = new Label("\u26A0");
        wIcon.setStyle("-fx-font-size: 28;");
        warnIcon.getChildren().add(wIcon);

        Label warnTitle = new Label("Supprimer cet article ?");
        warnTitle.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 16; -fx-font-weight: bold;");

        String itemName = p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService();
        Label warnDesc = new Label(itemName + " sera retire de votre panier.");
        warnDesc.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        warnDesc.setWrapText(true);

        VBox infoBox = new VBox(6);
        infoBox.setStyle("-fx-background-color: rgba(255,107,107,0.04); -fx-padding: 12; -fx-background-radius: 8;");
        HBox priceRow = new HBox();
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label pLeft = new Label("\uD83D\uDCB0 Prix");
        pLeft.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);
        Label pRight = new Label(String.format("%.2f DT", p.getPrixEstime()));
        pRight.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12; -fx-font-weight: bold;");
        priceRow.getChildren().addAll(pLeft, pSpacer, pRight);
        infoBox.getChildren().add(priceRow);

        content.getChildren().addAll(warnIcon, warnTitle, warnDesc, infoBox);
        dp.setContent(content);

        ButtonType deleteType = new ButtonType("\uD83D\uDDD1 Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(deleteType, cancelType);

        Button delBtn = (Button) dp.lookupButton(deleteType);
        delBtn.setStyle("-fx-background-color: #FF6B6B; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == deleteType) {
            if (panierService.supprimer(p.getIdPanier())) {
                showMessage("\u2705 Article supprime du panier.", true);
                loadData();
            } else {
                showMessage("\u274C Echec de la suppression.", false);
            }
        }
    }

    private void showMessage(String msg, boolean success) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + (success ? "#51CF66" : "#FF6B6B") + ";");
            messageLabel.setText(msg);
        }
    }

    @FXML public void handlePrev() {
        if (currentPage > 0) { currentPage--; renderCarousel(); }
    }
    @FXML public void handleNext() {
        if ((currentPage + 1) * CARDS_PER_PAGE < panierItems.size()) { currentPage++; renderCarousel(); }
    }

    @FXML public void handleBackToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/maininterface.fxml"));
            Parent root = loader.load();
            if (currentUser != null) { MainInterfaceController c = loader.getController(); c.setUser(currentUser); }
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Accueil");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleGoToReservation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reservation.fxml"));
            Parent root = loader.load();
            if (currentUser != null) { ReservationController c = loader.getController(); c.setUser(currentUser); }
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Mes Paiements");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleMinimize() { ((Stage) titleBar.getScene().getWindow()).setIconified(true); }
    @FXML public void handleClose() { ((Stage) titleBar.getScene().getWindow()).close(); }

    @FXML public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Connexion");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void fadeTransition(Stage stage, Parent newRoot, String title) {
        Scene oldScene = stage.getScene();
        Node oldRoot = oldScene.getRoot();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), oldRoot);
        fadeOut.setFromValue(1); fadeOut.setToValue(0); fadeOut.setInterpolator(Interpolator.EASE_IN);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(300), oldRoot);
        scaleOut.setToX(0.97); scaleOut.setToY(0.97); scaleOut.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition exitAnim = new ParallelTransition(fadeOut, scaleOut);
        exitAnim.setOnFinished(e -> {
            Scene newScene = new Scene(newRoot);
            newScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            newRoot.setOpacity(0); newRoot.setScaleX(1.03); newRoot.setScaleY(1.03); newRoot.setTranslateY(8);
            stage.setScene(newScene); stage.setTitle(title);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), newRoot);
            fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), newRoot);
            scaleIn.setToX(1); scaleIn.setToY(1);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), newRoot);
            slideIn.setToY(0);
            new ParallelTransition(fadeIn, scaleIn, slideIn).play();
        });
        exitAnim.play();
    }
}