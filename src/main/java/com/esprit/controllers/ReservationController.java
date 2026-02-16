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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import com.esprit.entities.Reservation;
import com.esprit.entities.Etablissement;
import com.esprit.entities.utilisateur;
import com.esprit.services.ReservationService;
import com.esprit.services.EtablissementService;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class ReservationController {

    @FXML private HBox titleBar;
    @FXML private Label userNameLabel;
    @FXML private Label totalReservationsLabel;
    @FXML private Label payeLabel;
    @FXML private Label enCoursLabel;
    @FXML private Label totalDepenseLabel;
    @FXML private Label messageLabel;
    @FXML private HBox reservationCarousel;
    @FXML private Label pageLabel;

    private double xOffset = 0;
    private double yOffset = 0;
    private utilisateur currentUser;
    private final ReservationService reservationService = new ReservationService();
    private final EtablissementService etabService = new EtablissementService();

    private List<Reservation> reservations;
    private int currentPage = 0;
    private static final int CARDS_PER_PAGE = 3;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] CARD_COLORS = {
        "rgba(255,215,0,0.08)", "rgba(81,207,102,0.08)", "rgba(100,181,246,0.08)",
        "rgba(255,107,107,0.08)", "rgba(178,102,255,0.08)", "rgba(255,167,38,0.08)"
    };

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
        reservations = reservationService.getReservationsByClient(currentUser.getId());
        for (Reservation r : reservations) {
            if (r.getNomEtablissement() == null || r.getNomEtablissement().isEmpty()) {
                r.setNomEtablissement(r.getTypeService() != null ? r.getTypeService() : "Service");
            }
        }
        updateStats();
        renderCarousel();
    }

    private void updateStats() {
        int total = reservations.size();
        long paye = reservations.stream().filter(r -> "Paye".equalsIgnoreCase(r.getStatutPaiement()) || "Pay\u00e9".equalsIgnoreCase(r.getStatutPaiement())).count();
        long enCours = reservations.stream().filter(r -> "En cours de paiement".equalsIgnoreCase(r.getStatutPaiement())).count();
        double totalDepense = reservations.stream().mapToDouble(Reservation::getMontantTotal).sum();
        if (totalReservationsLabel != null) totalReservationsLabel.setText(String.valueOf(total));
        if (payeLabel != null) payeLabel.setText(String.valueOf(paye));
        if (enCoursLabel != null) enCoursLabel.setText(String.valueOf(enCours));
        if (totalDepenseLabel != null) totalDepenseLabel.setText(String.format("%.2f DT", totalDepense));
    }

    private void renderCarousel() {
        if (reservationCarousel == null) return;
        reservationCarousel.getChildren().clear();
        int start = currentPage * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, reservations.size());
        for (int i = start; i < end; i++) {
            VBox card = buildReservationCard(reservations.get(i), i);
            HBox.setHgrow(card, Priority.ALWAYS);
            reservationCarousel.getChildren().add(card);
        }
        if (reservationCarousel.getChildren().isEmpty()) {
            VBox emptyState = new VBox(16);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(40));
            emptyState.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 14;");
            Label emptyIcon = new Label("\uD83D\uDCB3");
            emptyIcon.setStyle("-fx-font-size: 40;");
            Label emptyTitle = new Label("Aucun paiement");
            emptyTitle.setStyle("-fx-text-fill: #888; -fx-font-size: 16; -fx-font-weight: bold;");
            Label emptyDesc = new Label("Confirmez des articles de votre panier pour voir vos reservations ici.");
            emptyDesc.setStyle("-fx-text-fill: #555; -fx-font-size: 12;");
            emptyDesc.setWrapText(true);
            Button goToPanierBtn = new Button("\uD83D\uDED2 Aller au Panier");
            goToPanierBtn.getStyleClass().add("dashboard-button");
            goToPanierBtn.setStyle("-fx-padding: 10 24; -fx-font-size: 12;");
            goToPanierBtn.setOnAction(e -> handleGoToPanier());
            emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc, goToPanierBtn);
            HBox.setHgrow(emptyState, Priority.ALWAYS);
            reservationCarousel.getChildren().add(emptyState);
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) reservations.size() / CARDS_PER_PAGE));
        if (pageLabel != null) pageLabel.setText((currentPage + 1) + " / " + totalPages);
    }

    // ================================================================
    // BUILD RESERVATION CARD  PROFESSIONAL DESIGN
    // ================================================================
    private VBox buildReservationCard(Reservation r, int index) {
        VBox card = new VBox(10);
        card.setMinWidth(310);
        card.setMaxWidth(420);

        String bgColor = CARD_COLORS[index % CARD_COLORS.length];
        String statutColor = getStatutColor(r.getStatutPaiement());
        String statutIcon = getStatutIcon(r.getStatutPaiement());

        card.setStyle("-fx-background-color: rgba(15,15,15,0.95); -fx-padding: 18; -fx-background-radius: 14; " +
                "-fx-border-color: " + bgColor.replace("0.08", "0.15") + "; -fx-border-radius: 14; -fx-border-width: 1;");

        //  TOP: icon + name + price 
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(48, 48);
        iconPane.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12;");
        String svcIcon = getServiceIcon(r.getTypeService());
        Label iconLabel = new Label(svcIcon);
        iconLabel.setStyle("-fx-font-size: 20;");
        iconPane.getChildren().add(iconLabel);

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        String displayName = r.getNomEtablissement() != null && !r.getNomEtablissement().isEmpty()
                ? r.getNomEtablissement() : (r.getTypeService() != null ? r.getTypeService() : "Reservation");
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        nameLabel.setMaxWidth(200);
        Label typeLabel = new Label(r.getTypeService() != null ? r.getTypeService() : "");
        typeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 10;");
        nameBox.getChildren().addAll(nameLabel, typeLabel);

        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label prixLabel = new Label(String.format("%.2f DT", r.getMontantTotal()));
        prixLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");
        priceBox.getChildren().add(prixLabel);

        header.getChildren().addAll(iconPane, nameBox, priceBox);

        //  CONFIRMATION CODE BADGE 
        HBox codeBadge = new HBox(8);
        codeBadge.setAlignment(Pos.CENTER);
        codeBadge.setStyle("-fx-background-color: rgba(255,215,0,0.06); -fx-padding: 8 14; -fx-background-radius: 8;");
        Label codeIcon = new Label("\uD83D\uDD11");
        codeIcon.setStyle("-fx-font-size: 11;");
        Label codeLabel = new Label(r.getCodeConfirmation() != null ? r.getCodeConfirmation() : "N/A");
        codeLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'Consolas';");
        Region codeSpacer = new Region();
        HBox.setHgrow(codeSpacer, Priority.ALWAYS);
        // Statut badge inline
        Label statutBadge = new Label(statutIcon + " " + (r.getStatutPaiement() != null ? r.getStatutPaiement() : "Inconnu"));
        String sbBg = statutColor.replace("rgb(", "rgba(").replace(")", ",0.12)");
        statutBadge.setStyle("-fx-background-color: " + sbBg + "; -fx-text-fill: " + statutColor +
                "; -fx-padding: 4 12; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold;");
        codeBadge.getChildren().addAll(codeIcon, codeLabel, codeSpacer, statutBadge);

        //  PROGRESS TRACKER 
        HBox progressTracker = buildProgressTracker(r);

        //  DETAIL GRID 
        VBox details = new VBox(6);
        details.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 12; -fx-background-radius: 8;");

        HBox row1 = new HBox(16);
        row1.getChildren().addAll(
            buildDetailChip("\uD83D\uDCC5", r.getDatePaiement() != null ? r.getDatePaiement().format(DTF) : "N/A"),
            buildDetailChip("\uD83D\uDCB3", r.getModePaiement() != null ? r.getModePaiement() : "N/A")
        );
        HBox row2 = new HBox(16);
        row2.getChildren().addAll(
            buildDetailChip("\uD83D\uDC65", r.getNbPersonnes() + " personnes"),
            buildDetailChip("\u23F0", getTimeAgo(r))
        );
        details.getChildren().addAll(row1, row2);

        //  ACTION BUTTONS 
        HBox buttons = new HBox(6);
        buttons.setAlignment(Pos.CENTER);

        boolean isEnCours = "En cours de paiement".equalsIgnoreCase(r.getStatutPaiement());
        if (isEnCours) {
            Button payerBtn = new Button("\u2705 Payer");
            payerBtn.setStyle("-fx-background-color: rgba(81,207,102,0.12); -fx-text-fill: #51CF66; -fx-padding: 7 14; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: hand;");
            payerBtn.setOnAction(e -> markAsPaid(r));
            buttons.getChildren().add(payerBtn);
        }

        Button modifyBtn = new Button("\u270F Modifier");
        modifyBtn.setStyle("-fx-background-color: rgba(255,167,38,0.12); -fx-text-fill: #FFA726; -fx-padding: 7 14; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        modifyBtn.setOnAction(e -> showModifyReservationDialog(r));

        Button detailBtn = new Button("\uD83D\uDCCB Detail");
        detailBtn.setStyle("-fx-background-color: rgba(100,181,246,0.12); -fx-text-fill: #64B5F6; -fx-padding: 7 14; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        detailBtn.setOnAction(e -> showReservationDetail(r));

        Button deleteBtn = new Button("\uD83D\uDDD1 Supprimer");
        deleteBtn.setStyle("-fx-background-color: rgba(255,107,107,0.12); -fx-text-fill: #FF6B6B; -fx-padding: 7 14; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteReservation(r));

        buttons.getChildren().addAll(modifyBtn, detailBtn, deleteBtn);

        card.getChildren().addAll(header, codeBadge, progressTracker, details, buttons);
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
        if (type == null) return "\uD83D\uDCB3";
        if (type.toLowerCase().contains("voyage")) return "\uD83C\uDFDD";
        if (type.toLowerCase().contains("caf")) return "\u2615";
        if (type.toLowerCase().contains("rest")) return "\uD83C\uDF55";
        return "\uD83C\uDFAF";
    }

    // ================================================================
    // VISUAL STEP-BY-STEP PROGRESS TRACKER
    // ================================================================
    private HBox buildProgressTracker(Reservation r) {
        HBox tracker = new HBox(0);
        tracker.setAlignment(Pos.CENTER);
        tracker.setPadding(new Insets(6, 4, 6, 4));
        tracker.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 10;");

        String statut = r.getStatutPaiement() != null ? r.getStatutPaiement().toLowerCase() : "";
        int step = 0;
        if (statut.contains("cours")) step = 1;
        else if (statut.contains("pay")) step = 2;
        else if (statut.contains("rembours")) step = -1;

        String[] labels = {"Ajout\u00e9", "En cours", "Pay\u00e9", "Termin\u00e9"};
        String[] icons = {"\uD83D\uDED2", "\u23F3", "\u2705", "\uD83C\uDF1F"};

        for (int i = 0; i < 4; i++) {
            VBox stepBox = new VBox(2);
            stepBox.setAlignment(Pos.CENTER);
            stepBox.setMinWidth(55);
            HBox.setHgrow(stepBox, Priority.ALWAYS);

            boolean isCompleted;
            boolean isCurrent;
            if (step == -1) {
                isCompleted = i == 0;
                isCurrent = false;
            } else {
                isCompleted = i <= step;
                isCurrent = i == step;
            }

            StackPane circle = new StackPane();
            circle.setPrefSize(26, 26);
            circle.setMaxSize(26, 26);

            if (step == -1 && i > 0) {
                circle.setStyle("-fx-background-color: rgba(255,107,107,0.15); -fx-background-radius: 13;");
                Label stepIcon = new Label("\u274C");
                stepIcon.setStyle("-fx-font-size: 9;");
                circle.getChildren().add(stepIcon);
            } else if (isCompleted) {
                circle.setStyle("-fx-background-color: rgba(81,207,102,0.2); -fx-background-radius: 13; " +
                    (isCurrent ? "-fx-border-color: #51CF66; -fx-border-radius: 13; -fx-border-width: 2;" : ""));
                Label stepIcon = new Label(icons[i]);
                stepIcon.setStyle("-fx-font-size: 10;");
                circle.getChildren().add(stepIcon);
            } else {
                circle.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 13;");
                Label stepNum = new Label(String.valueOf(i + 1));
                stepNum.setStyle("-fx-text-fill: #444; -fx-font-size: 9; -fx-font-weight: bold;");
                circle.getChildren().add(stepNum);
            }

            Label stepLabel = new Label(labels[i]);
            String labelColor = (step == -1 && i > 0) ? "#FF6B6B" : (isCompleted ? "#51CF66" : "#444");
            stepLabel.setStyle("-fx-text-fill: " + labelColor + "; -fx-font-size: 8; -fx-font-weight: bold;");
            stepBox.getChildren().addAll(circle, stepLabel);
            tracker.getChildren().add(stepBox);

            if (i < 3) {
                Region line = new Region();
                line.setPrefHeight(2);
                line.setMinWidth(16);
                line.setMaxHeight(2);
                HBox.setHgrow(line, Priority.ALWAYS);
                String lineColor;
                if (step == -1) lineColor = "rgba(255,107,107,0.3)";
                else if (i < step) lineColor = "rgba(81,207,102,0.4)";
                else lineColor = "rgba(255,255,255,0.06)";
                line.setStyle("-fx-background-color: " + lineColor + "; -fx-background-radius: 1;");
                VBox lineWrapper = new VBox(line);
                lineWrapper.setAlignment(Pos.CENTER);
                lineWrapper.setPadding(new Insets(0, 0, 12, 0));
                HBox.setHgrow(lineWrapper, Priority.ALWAYS);
                tracker.getChildren().add(lineWrapper);
            }
        }
        return tracker;
    }

    // ================================================================
    // TIME AGO HELPER
    // ================================================================
    private String getTimeAgo(Reservation r) {
        if (r.getDatePaiement() == null) return "Date inconnue";
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(r.getDatePaiement(), now);
        if (minutes < 1) return "A l'instant";
        if (minutes < 60) return "Il y a " + minutes + " min";
        long hours = ChronoUnit.HOURS.between(r.getDatePaiement(), now);
        if (hours < 24) return "Il y a " + hours + "h";
        long days = ChronoUnit.DAYS.between(r.getDatePaiement(), now);
        if (days < 30) return "Il y a " + days + " jours";
        long months = days / 30;
        return "Il y a " + months + " mois";
    }

    // ================================================================
    // MODIFY RESERVATION DIALOG
    // ================================================================
    private void showModifyReservationDialog(Reservation r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier la Reservation");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(18);
        content.setPadding(new Insets(28));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(480);

        // Header
        HBox headerRow = new HBox(14);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        StackPane headerIcon = new StackPane();
        headerIcon.setPrefSize(50, 50);
        headerIcon.setStyle("-fx-background-color: rgba(255,167,38,0.12); -fx-background-radius: 14;");
        Label hIcon = new Label("\u270F");
        hIcon.setStyle("-fx-font-size: 22;");
        headerIcon.getChildren().add(hIcon);
        VBox headerText = new VBox(2);
        Label titleLbl = new Label("Modifier la Reservation");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");
        String dispName = r.getNomEtablissement() != null && !r.getNomEtablissement().isEmpty()
                ? r.getNomEtablissement() : "Reservation";
        Label subLbl = new Label(dispName + " | " + (r.getCodeConfirmation() != null ? r.getCodeConfirmation() : ""));
        subLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        headerText.getChildren().addAll(titleLbl, subLbl);
        headerRow.getChildren().addAll(headerIcon, headerText);

        // Current info
        VBox currentInfo = new VBox(8);
        currentInfo.setStyle("-fx-background-color: rgba(255,215,0,0.04); -fx-padding: 14; -fx-background-radius: 10; -fx-border-color: rgba(255,215,0,0.1); -fx-border-radius: 10; -fx-border-width: 1;");
        Label curTitle = new Label("\uD83D\uDCCB Informations actuelles");
        curTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");
        HBox curRow1 = buildInfoChip("\uD83D\uDCB0 Montant", String.format("%.2f DT", r.getMontantTotal()));
        HBox curRow2 = buildInfoChip("\uD83D\uDCB3 Mode", r.getModePaiement() != null ? r.getModePaiement() : "N/A");
        HBox curRow3 = buildInfoChip("\uD83D\uDCCA Statut", r.getStatutPaiement() != null ? r.getStatutPaiement() : "N/A");
        currentInfo.getChildren().addAll(curTitle, curRow1, curRow2, curRow3);

        //  Editable fields 
        VBox editSection = new VBox(14);
        editSection.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 16; -fx-background-radius: 10;");
        Label editTitle = new Label("\u2728 Nouvelles valeurs");
        editTitle.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 12; -fx-font-weight: bold;");

        // Montant field
        VBox montantBox = new VBox(4);
        Label montantLbl = new Label("Montant Total (DT)");
        montantLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        TextField montantField = new TextField(String.format("%.2f", r.getMontantTotal()));
        montantField.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-padding: 10 14; -fx-background-radius: 8; -fx-font-size: 13; -fx-border-color: rgba(255,215,0,0.15); -fx-border-radius: 8;");
        montantBox.getChildren().addAll(montantLbl, montantField);

        // Mode de paiement
        VBox modeBox = new VBox(4);
        Label modeLbl = new Label("Mode de Paiement");
        modeLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        ComboBox<String> modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("Especes", "Carte Bancaire");
        modeCombo.setValue(r.getModePaiement() != null ? r.getModePaiement() : "Especes");
        modeCombo.setMaxWidth(Double.MAX_VALUE);
        modeCombo.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
        modeBox.getChildren().addAll(modeLbl, modeCombo);

        // Statut paiement
        VBox statutBox = new VBox(4);
        Label statutLbl = new Label("Statut du Paiement");
        statutLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        ComboBox<String> statutCombo = new ComboBox<>();
        statutCombo.getItems().addAll("Pay\u00e9", "En cours de paiement", "Rembours\u00e9");
        statutCombo.setValue(r.getStatutPaiement() != null ? r.getStatutPaiement() : "Pay\u00e9");
        statutCombo.setMaxWidth(Double.MAX_VALUE);
        statutCombo.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
        statutBox.getChildren().addAll(statutLbl, statutCombo);

        editSection.getChildren().addAll(editTitle, montantBox, modeBox, statutBox);

        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");

        content.getChildren().addAll(headerRow, currentInfo, editSection, errorLbl);
        dp.setContent(content);

        ButtonType saveType = new ButtonType("\uD83D\uDCBE Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(saveType, cancelType);

        Button saveBtn = (Button) dp.lookupButton(saveType);
        saveBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 28; -fx-background-radius: 8; -fx-font-size: 13;");

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String montantStr = montantField.getText().trim().replace(",", ".");
            double newMontant;
            try {
                newMontant = Double.parseDouble(montantStr);
                if (newMontant <= 0) {
                    errorLbl.setText("\u274C Le montant doit etre positif");
                    event.consume();
                    return;
                }
            } catch (NumberFormatException ex) {
                errorLbl.setText("\u274C Montant invalide. Entrez un nombre valide.");
                event.consume();
                return;
            }

            r.setMontantTotal(newMontant);
            r.setModePaiement(modeCombo.getValue());
            r.setStatutPaiement(statutCombo.getValue());

            if (reservationService.modifier(r)) {
                showMessage("\u2705 Reservation modifiee avec succes !", true);
                loadData();
            } else {
                errorLbl.setText("\u274C Echec de la modification.");
                event.consume();
            }
        });

        dialog.setResultConverter(bt -> null);
        dialog.showAndWait();
    }

    private HBox buildInfoChip(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label leftLbl = new Label(label);
        leftLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label rightLbl = new Label(value);
        rightLbl.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
        row.getChildren().addAll(leftLbl, spacer, rightLbl);
        return row;
    }

    // ================================================================
    // RESERVATION DETAIL DIALOG
    // ================================================================
    private void showReservationDetail(Reservation r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Detail de la Reservation");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(16);
        content.setPadding(new Insets(28));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setAlignment(Pos.CENTER);
        content.setMinWidth(460);

        // Header
        StackPane headerIcon = new StackPane();
        headerIcon.setPrefSize(60, 60);
        headerIcon.setStyle("-fx-background-color: rgba(255,215,0,0.1); -fx-background-radius: 30;");
        Label hIcon = new Label("\uD83D\uDCCB");
        hIcon.setStyle("-fx-font-size: 28;");
        headerIcon.getChildren().add(hIcon);

        Label titleLbl = new Label("Detail de la Reservation");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 20; -fx-font-weight: bold;");

        String displayName = r.getNomEtablissement() != null && !r.getNomEtablissement().isEmpty()
                ? r.getNomEtablissement() : (r.getTypeService() != null ? r.getTypeService() : "Reservation");
        Label nameLbl = new Label(displayName);
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        HBox progressInDetail = buildProgressTracker(r);
        progressInDetail.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12; -fx-padding: 12;");

        VBox detailRows = new VBox(10);
        detailRows.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 16; -fx-background-radius: 12;");

        addDetailRow(detailRows, "\uD83D\uDD11 Code", r.getCodeConfirmation() != null ? r.getCodeConfirmation() : "N/A", "#FFD700");
        addDetailRow(detailRows, "\uD83D\uDCB0 Montant", String.format("%.2f DT", r.getMontantTotal()), "#FFD700");
        addDetailRow(detailRows, "\uD83D\uDCB3 Mode", r.getModePaiement() != null ? r.getModePaiement() : "N/A", "#64B5F6");
        addDetailRow(detailRows, "\uD83C\uDFAF Service", r.getTypeService() != null ? r.getTypeService() : "N/A", "white");
        addDetailRow(detailRows, "\uD83D\uDC65 Personnes", String.valueOf(r.getNbPersonnes()), "white");
        addDetailRow(detailRows, "\uD83D\uDCC5 Date", r.getDatePaiement() != null ? r.getDatePaiement().format(DTF) : "N/A", "#999");
        addDetailRow(detailRows, "\u23F0 Temps", getTimeAgo(r), "#888");

        String statutColor = getStatutColor(r.getStatutPaiement());
        String statutIcon = getStatutIcon(r.getStatutPaiement());
        HBox statutRow = new HBox(8);
        statutRow.setAlignment(Pos.CENTER);
        statutRow.setStyle("-fx-background-color: " + statutColor.replace(")", ",0.08)").replace("rgb(", "rgba(") + "; -fx-padding: 10; -fx-background-radius: 10;");
        Label statutLabel = new Label(statutIcon + " Statut: " + (r.getStatutPaiement() != null ? r.getStatutPaiement() : "Inconnu"));
        statutLabel.setStyle("-fx-text-fill: " + statutColor + "; -fx-font-size: 14; -fx-font-weight: bold;");
        statutRow.getChildren().add(statutLabel);

        content.getChildren().addAll(headerIcon, titleLbl, nameLbl, progressInDetail, detailRows, statutRow);
        dp.setContent(content);

        ButtonType closeType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().add(closeType);

        dialog.showAndWait();
    }

    private void addDetailRow(VBox container, String label, String value, String valueColor) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label leftLbl = new Label(label);
        leftLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label rightLbl = new Label(value);
        rightLbl.setStyle("-fx-text-fill: " + valueColor + "; -fx-font-size: 12; -fx-font-weight: bold;");
        row.getChildren().addAll(leftLbl, spacer, rightLbl);
        container.getChildren().add(row);
    }

    // ================================================================
    // STATUT HELPERS
    // ================================================================
    private String getStatutColor(String statut) {
        if (statut == null) return "rgb(136,136,136)";
        String lower = statut.toLowerCase();
        if (lower.contains("pay")) return "rgb(81,207,102)";
        if (lower.contains("cours")) return "rgb(100,181,246)";
        if (lower.contains("rembours")) return "rgb(255,107,107)";
        return "rgb(255,215,0)";
    }

    private String getStatutIcon(String statut) {
        if (statut == null) return "\uD83D\uDCB3";
        String lower = statut.toLowerCase();
        if (lower.contains("pay")) return "\u2705";
        if (lower.contains("cours")) return "\u23F3";
        if (lower.contains("rembours")) return "\u274C";
        return "\uD83D\uDCB3";
    }

    // ================================================================
    // ACTIONS
    // ================================================================
    private void markAsPaid(Reservation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer le paiement");
        confirm.setHeaderText("Marquer comme paye ?");
        confirm.setContentText("Code: " + r.getCodeConfirmation() + "\nMontant: " + String.format("%.2f DT", r.getMontantTotal()));
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                if (reservationService.updateStatut(r.getIdReservation(), "Pay\u00e9")) {
                    showMessage("\u2705 Paiement confirme ! Code: " + r.getCodeConfirmation(), true);
                    loadData();
                } else {
                    showMessage("Echec de la mise a jour.", false);
                }
            }
        });
    }

    private void deleteReservation(Reservation r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Supprimer la Reservation");
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

        Label warnTitle = new Label("Supprimer cette reservation ?");
        warnTitle.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 16; -fx-font-weight: bold;");

        Label warnDesc = new Label("Cette action est irreversible. La reservation sera definitivement supprimee.");
        warnDesc.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        warnDesc.setWrapText(true);

        VBox infoBox = new VBox(6);
        infoBox.setStyle("-fx-background-color: rgba(255,107,107,0.04); -fx-padding: 12; -fx-background-radius: 8;");
        addDetailRow(infoBox, "\uD83D\uDD11 Code", r.getCodeConfirmation() != null ? r.getCodeConfirmation() : "N/A", "#FFD700");
        addDetailRow(infoBox, "\uD83D\uDCB0 Montant", String.format("%.2f DT", r.getMontantTotal()), "#FF6B6B");

        content.getChildren().addAll(warnIcon, warnTitle, warnDesc, infoBox);
        dp.setContent(content);

        ButtonType deleteType = new ButtonType("\uD83D\uDDD1 Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(deleteType, cancelType);

        Button delBtn = (Button) dp.lookupButton(deleteType);
        delBtn.setStyle("-fx-background-color: #FF6B6B; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == deleteType) {
            if (reservationService.supprimer(r.getIdReservation())) {
                showMessage("\u2705 Reservation supprimee.", true);
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

    // ================================================================
    // CAROUSEL NAVIGATION
    // ================================================================
    @FXML public void handlePrev() {
        if (currentPage > 0) { currentPage--; renderCarousel(); }
    }
    @FXML public void handleNext() {
        if ((currentPage + 1) * CARDS_PER_PAGE < reservations.size()) { currentPage++; renderCarousel(); }
    }

    // ================================================================
    // NAVIGATION
    // ================================================================
    @FXML public void handleBackToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/maininterface.fxml"));
            Parent root = loader.load();
            if (currentUser != null) { MainInterfaceController c = loader.getController(); c.setUser(currentUser); }
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Accueil");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleGoToPanier() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/panier.fxml"));
            Parent root = loader.load();
            if (currentUser != null) { PanierController c = loader.getController(); c.setUser(currentUser); }
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Mon Panier");
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