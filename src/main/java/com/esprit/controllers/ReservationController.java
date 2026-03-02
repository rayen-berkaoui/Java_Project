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
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.concurrent.Worker;

import com.esprit.entities.Reservation;
import com.esprit.entities.Etablissement;
import com.esprit.entities.utilisateur;
import com.esprit.services.ReservationService;
import com.esprit.services.EtablissementService;
import com.esprit.services.EmailService;
import com.esprit.services.utilisateurServices;
import com.esprit.services.ChatbotService;
import com.esprit.utils.ThemeManager;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import javafx.stage.FileChooser;

public class ReservationController {

    @FXML private HBox titleBar;
    @FXML private Button fullscreenBtn;
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
    private final EmailService emailService = new EmailService();
    private final utilisateurServices userService = new utilisateurServices();
    private final ChatbotService chatbotService = new ChatbotService();

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
            addThemeToggleButton();
            addChatbotButton();
            addFeatureButtons();

            // Apply theme to FXML nodes
            javafx.application.Platform.runLater(() -> {
                if (titleBar.getScene() != null && titleBar.getScene().getRoot() != null) {
                    ThemeManager.applyThemeToFXML((Parent) titleBar.getScene().getRoot());
                }
            });
            ThemeManager.addThemeChangeListener(() -> javafx.application.Platform.runLater(() -> {
                if (titleBar.getScene() != null && titleBar.getScene().getRoot() != null) {
                    ThemeManager.applyThemeToFXML((Parent) titleBar.getScene().getRoot());
                }
            }));
        }
    }

    public void setUser(utilisateur user) {
        this.currentUser = user;
        if (user != null) {
            if (userNameLabel != null) userNameLabel.setText(user.getNom() + " " + user.getPrenom());
            user.setLoyaltyPoints(userService.getLoyaltyPoints(user.getId()));
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
        // Exclude restaurant/cafe reservations from total spending (they have 0 montant)
        double totalDepense = reservations.stream()
                .filter(r -> !isRestoOrCafeType(r.getTypeService()))
                .mapToDouble(Reservation::getMontantTotal).sum();
        if (totalReservationsLabel != null) totalReservationsLabel.setText(String.valueOf(total));
        if (payeLabel != null) payeLabel.setText(String.valueOf(paye));
        if (enCoursLabel != null) enCoursLabel.setText(String.valueOf(enCours));
        if (totalDepenseLabel != null) totalDepenseLabel.setText(String.format("%.2f DT", totalDepense));
    }

    private boolean isRestoOrCafeType(String type) {
        if (type == null) return false;
        String lower = type.toLowerCase();
        return lower.contains("restaurant") || lower.contains("caf") || lower.contains("resto");
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
    // BUILD RESERVATION CARD - ENHANCED CAROUSEL DESIGN
    // ================================================================
    private VBox buildReservationCard(Reservation r, int index) {
        VBox card = new VBox(0);
        card.setMinWidth(340);
        card.setMaxWidth(440);

        String bgColor = CARD_COLORS[index % CARD_COLORS.length];
        String statutColor = getStatutColor(r.getStatutPaiement());
        String statutIcon = getStatutIcon(r.getStatutPaiement());

        card.setStyle("-fx-background-color: rgba(15,15,15,0.95); -fx-background-radius: 16; " +
                "-fx-border-color: " + bgColor.replace("0.08", "0.18") + "; -fx-border-radius: 16; -fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, " + bgColor.replace("0.08", "0.08") + ", 12, 0, 0, 4);");

        // ── GRADIENT HEADER BAR ──
        HBox headerBar = new HBox(12);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(16, 18, 12, 18));
        headerBar.setStyle("-fx-background-color: linear-gradient(to right, " + bgColor + ", rgba(0,0,0,0)); -fx-background-radius: 16 16 0 0;");

        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(52, 52);
        iconPane.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 14;");
        String svcIcon = getServiceIcon(r.getTypeService());
        Label iconLabel = new Label(svcIcon);
        iconLabel.setStyle("-fx-font-size: 24;");
        iconPane.getChildren().add(iconLabel);

        VBox nameBox = new VBox(3);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        String displayName = r.getNomEtablissement() != null && !r.getNomEtablissement().isEmpty()
                ? r.getNomEtablissement() : (r.getTypeService() != null ? r.getTypeService() : "Reservation");
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold;");
        nameLabel.setMaxWidth(220);
        nameLabel.setWrapText(true);
        Label typeChip = new Label(r.getTypeService() != null ? r.getTypeService() : "");
        typeChip.setStyle("-fx-text-fill: #999; -fx-font-size: 9; -fx-background-color: rgba(255,255,255,0.05); -fx-padding: 2 8; -fx-background-radius: 6;");
        nameBox.getChildren().addAll(nameLabel, typeChip);

        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        boolean isRestoRes = isRestoOrCafeType(r.getTypeService());
        if (isRestoRes) {
            Label gratuitLabel = new Label("Sur place");
            gratuitLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 14; -fx-font-weight: bold;");
            priceBox.getChildren().add(gratuitLabel);
        } else {
            Label prixLabel = new Label(String.format("%.2f DT", r.getMontantTotal()));
            prixLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 20; -fx-font-weight: bold;");
            // Per-person price
            if (r.getNbPersonnes() > 1) {
                Label perPerson = new Label(String.format("%.2f DT/pers", r.getMontantTotal() / r.getNbPersonnes()));
                perPerson.setStyle("-fx-text-fill: #888; -fx-font-size: 9;");
                priceBox.getChildren().addAll(prixLabel, perPerson);
            } else {
                priceBox.getChildren().add(prixLabel);
            }
        }
        headerBar.getChildren().addAll(iconPane, nameBox, priceBox);

        // ── CONFIRMATION CODE & STATUS STRIP ──
        HBox codeBadge = new HBox(8);
        codeBadge.setAlignment(Pos.CENTER);
        codeBadge.setPadding(new Insets(0, 14, 0, 14));
        codeBadge.setStyle("-fx-background-color: rgba(255,215,0,0.04); -fx-padding: 8 18;");
        Label codeIcon = new Label("\uD83D\uDD11");
        codeIcon.setStyle("-fx-font-size: 12;");
        Label codeLabel = new Label(r.getCodeConfirmation() != null ? r.getCodeConfirmation() : "N/A");
        codeLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'Consolas';");
        Region codeSpacer = new Region();
        HBox.setHgrow(codeSpacer, Priority.ALWAYS);
        Label statutBadge = new Label(statutIcon + " " + (r.getStatutPaiement() != null ? r.getStatutPaiement() : "Inconnu"));
        String sbBg = statutColor.replace("rgb(", "rgba(").replace(")", ",0.12)");
        statutBadge.setStyle("-fx-background-color: " + sbBg + "; -fx-text-fill: " + statutColor +
                "; -fx-padding: 4 14; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold;");
        codeBadge.getChildren().addAll(codeIcon, codeLabel, codeSpacer, statutBadge);

        // ── PROGRESS TRACKER ──
        HBox progressTracker = buildProgressTracker(r);
        progressTracker.setPadding(new Insets(4, 14, 4, 14));

        // ── DETAIL TILES (carousel sections) ──
        VBox detailSections = new VBox(8);
        detailSections.setPadding(new Insets(6, 14, 6, 14));

        // Info tiles row
        HBox infoTiles = new HBox(6);
        infoTiles.setAlignment(Pos.CENTER);
        infoTiles.getChildren().addAll(
            buildInfoTile("\uD83D\uDCC5", r.getDatePaiement() != null ? r.getDatePaiement().format(DTF) : "N/A", "Date"),
            buildInfoTile("\uD83D\uDCB3", r.getModePaiement() != null ? r.getModePaiement() : "N/A", "Paiement"),
            buildInfoTile("\uD83D\uDC65", r.getNbPersonnes() + "", "Personnes")
        );

        // Time ago
        HBox timeRow = new HBox(8);
        timeRow.setAlignment(Pos.CENTER);
        timeRow.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 6 12; -fx-background-radius: 8;");
        Label timeIcon = new Label("\u23F0");
        timeIcon.setStyle("-fx-font-size: 11;");
        Label timeLabel = new Label(getTimeAgo(r));
        timeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        timeRow.getChildren().addAll(timeIcon, timeLabel);

        detailSections.getChildren().addAll(infoTiles, timeRow);

        // Show rating stars if already rated
        if (r.getRating() > 0) {
            HBox ratingDisplay = new HBox(6);
            ratingDisplay.setAlignment(Pos.CENTER);
            ratingDisplay.setStyle("-fx-background-color: rgba(255,215,0,0.04); -fx-padding: 8; -fx-background-radius: 8;");
            StringBuilder stars = new StringBuilder();
            for (int s = 0; s < 5; s++) stars.append(s < r.getRating() ? "\u2605" : "\u2606");
            Label ratingStars = new Label(stars.toString());
            ratingStars.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16;");
            ratingDisplay.getChildren().add(ratingStars);
            if (r.getReviewComment() != null && !r.getReviewComment().isEmpty()) {
                Label reviewSnippet = new Label("\"" + (r.getReviewComment().length() > 35 ? r.getReviewComment().substring(0, 35) + "..." : r.getReviewComment()) + "\"");
                reviewSnippet.setStyle("-fx-text-fill: #888; -fx-font-size: 9; -fx-font-style: italic;");
                ratingDisplay.getChildren().add(reviewSnippet);
            }
            detailSections.getChildren().add(ratingDisplay);
        }

        // ── ACTION BUTTONS ──
        VBox actionsArea = new VBox(6);
        actionsArea.setPadding(new Insets(6, 14, 14, 14));

        HBox mainButtons = new HBox(6);
        mainButtons.setAlignment(Pos.CENTER);

        // Upgrade button: Cash -> Konnect
        boolean isCash = "Especes".equalsIgnoreCase(r.getModePaiement());
        boolean isPaid = r.getStatutPaiement() != null && (r.getStatutPaiement().toLowerCase().contains("pay"));
        if (isCash && isPaid && !isRestoRes) {
            Button upgradeBtn = new Button("\uD83D\uDCB3 Passer en Konnect");
            upgradeBtn.setStyle("-fx-background-color: rgba(100,181,246,0.12); -fx-text-fill: #64B5F6; -fx-padding: 7 12; -fx-background-radius: 8; -fx-font-size: 9; -fx-font-weight: bold; -fx-cursor: hand;");
            upgradeBtn.setOnAction(e -> showUpgradeToCardDialog(r));
            mainButtons.getChildren().add(upgradeBtn);
        }

        Button modifyBtn = new Button("\u270F Modifier");
        modifyBtn.setStyle("-fx-background-color: rgba(255,167,38,0.12); -fx-text-fill: #FFA726; -fx-padding: 7 12; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        modifyBtn.setOnAction(e -> showModifyReservationDialog(r));

        Button detailBtn = new Button("\uD83D\uDCCB Detail");
        detailBtn.setStyle("-fx-background-color: rgba(100,181,246,0.12); -fx-text-fill: #64B5F6; -fx-padding: 7 12; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        detailBtn.setOnAction(e -> showReservationDetail(r));

        Button factureBtn = new Button("\uD83E\uDDFE Facture");
        factureBtn.setStyle("-fx-background-color: rgba(178,102,255,0.12); -fx-text-fill: #B266FF; -fx-padding: 7 12; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        factureBtn.setOnAction(e -> generateInvoice(r));

        Button deleteBtn = new Button("\uD83D\uDDD1");
        deleteBtn.setStyle("-fx-background-color: rgba(255,107,107,0.12); -fx-text-fill: #FF6B6B; -fx-padding: 7 12; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteReservation(r));

        mainButtons.getChildren().addAll(modifyBtn, detailBtn, factureBtn, deleteBtn);

        // Feature buttons row 1
        HBox featureRow1 = new HBox(6);
        featureRow1.setAlignment(Pos.CENTER);

        Button shareBtn = new Button("\uD83D\uDCE4 Partager");
        shareBtn.setStyle("-fx-background-color: rgba(41,182,246,0.12); -fx-text-fill: #29B6F6; -fx-padding: 6 10; -fx-background-radius: 8; -fx-font-size: 9; -fx-font-weight: bold; -fx-cursor: hand;");
        shareBtn.setOnAction(e -> shareReservation(r));

        Button countdownBtn = new Button("\u23F0 Countdown");
        countdownBtn.setStyle("-fx-background-color: rgba(255,167,38,0.12); -fx-text-fill: #FFA726; -fx-padding: 6 10; -fx-background-radius: 8; -fx-font-size: 9; -fx-font-weight: bold; -fx-cursor: hand;");
        countdownBtn.setOnAction(e -> showCountdownDialog(r));

        Button ratingBtn = new Button("\u2B50 Evaluer");
        ratingBtn.setStyle("-fx-background-color: rgba(255,215,0,0.12); -fx-text-fill: #FFD700; -fx-padding: 6 10; -fx-background-radius: 8; -fx-font-size: 9; -fx-font-weight: bold; -fx-cursor: hand;");
        ratingBtn.setOnAction(e -> showRatingDialog(r));

        featureRow1.getChildren().addAll(shareBtn, countdownBtn, ratingBtn);

        // Feature buttons row 2
        HBox featureRow2 = new HBox(6);
        featureRow2.setAlignment(Pos.CENTER);

        Button calendarBtn = new Button("\uD83D\uDCC5 Calendrier");
        calendarBtn.setStyle("-fx-background-color: rgba(81,207,102,0.12); -fx-text-fill: #51CF66; -fx-padding: 6 10; -fx-background-radius: 8; -fx-font-size: 9; -fx-font-weight: bold; -fx-cursor: hand;");
        calendarBtn.setOnAction(e -> exportToCalendar(r));

        Button aiReviewBtn = new Button("\uD83E\uDD16 AI Avis");
        aiReviewBtn.setStyle("-fx-background-color: rgba(178,102,255,0.12); -fx-text-fill: #B266FF; -fx-padding: 6 10; -fx-background-radius: 8; -fx-font-size: 9; -fx-font-weight: bold; -fx-cursor: hand;");
        aiReviewBtn.setOnAction(e -> showAIReviewGenerator(r));

        Button rebookBtn = new Button("\uD83D\uDD01 Re-book");
        rebookBtn.setStyle("-fx-background-color: rgba(255,215,0,0.12); -fx-text-fill: #FFD700; -fx-padding: 6 10; -fx-background-radius: 8; -fx-font-size: 9; -fx-font-weight: bold; -fx-cursor: hand;");
        rebookBtn.setOnAction(e -> rebookReservation(r));

        featureRow2.getChildren().addAll(calendarBtn, aiReviewBtn, rebookBtn);

        actionsArea.getChildren().addAll(mainButtons, featureRow1, featureRow2);

        // ── ASSEMBLE CARD ──
        card.getChildren().addAll(headerBar, codeBadge, progressTracker, detailSections, actionsArea);
        return card;
    }

    /** Builds a small info tile for the carousel detail view */
    private VBox buildInfoTile(String icon, String value, String label) {
        VBox tile = new VBox(2);
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(8, 12, 8, 12));
        tile.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 10;");
        HBox.setHgrow(tile, Priority.ALWAYS);
        Label iLbl = new Label(icon);
        iLbl.setStyle("-fx-font-size: 14;");
        Label vLbl = new Label(value);
        vLbl.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
        Label lLbl = new Label(label);
        lLbl.setStyle("-fx-text-fill: #666; -fx-font-size: 9;");
        tile.getChildren().addAll(iLbl, vLbl, lLbl);
        return tile;
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
        if (type.toLowerCase().contains("hotel")) return "\uD83C\uDFE8";
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
    // UPGRADE CASH -> KONNECT (IN-APP WEBVIEW)
    // ================================================================
    private void showUpgradeToCardDialog(Reservation r) {
        Stage paymentStage = new Stage();
        paymentStage.setTitle("Passer en Paiement Konnect");
        paymentStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        int pointsToEarn = (int)(r.getMontantTotal() * 10);
        final String[] paymentIdHolder = {null};
        com.esprit.services.KonnectPaymentService konnectSvc = new com.esprit.services.KonnectPaymentService();

        // Root
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #333; -fx-border-width: 1;");

        // Title bar
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle("-fx-background-color: #111; -fx-background-radius: 16 16 0 0;");

        Label flouciIcon = new Label("\uD83D\uDCB3");
        flouciIcon.setStyle("-fx-font-size: 18;");
        Label topTitle = new Label("Especes \u2192 Konnect");
        topTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14; -fx-font-weight: bold;");
        Label amountTag = new Label(String.format("%.2f DT", r.getMontantTotal()));
        amountTag.setStyle("-fx-background-color: rgba(255,215,0,0.15); -fx-text-fill: #FFD700; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 12; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("\u2715");
        closeBtn.setStyle("-fx-background-color: rgba(255,107,107,0.15); -fx-text-fill: #FF6B6B; -fx-font-size: 14; -fx-background-radius: 20; -fx-padding: 4 10; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> paymentStage.close());
        topBar.getChildren().addAll(flouciIcon, topTitle, amountTag, spacer, closeBtn);

        // Draggable
        final double[] dragOffset = {0, 0};
        topBar.setOnMousePressed(e -> { dragOffset[0] = e.getSceneX(); dragOffset[1] = e.getSceneY(); });
        topBar.setOnMouseDragged(e -> {
            paymentStage.setX(e.getScreenX() - dragOffset[0]);
            paymentStage.setY(e.getScreenY() - dragOffset[1]);
        });
        root.setTop(topBar);

        // Initial view
        VBox initialView = new VBox(14);
        initialView.setPadding(new Insets(20, 24, 20, 24));
        initialView.setAlignment(Pos.CENTER);
        initialView.setStyle("-fx-background-color: #1a1a1a;");

        Label mainIcon = new Label("\uD83D\uDCB3");
        mainIcon.setStyle("-fx-font-size: 48;");
        Label payTitle = new Label("Passer de Especes a Konnect");
        payTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");
        Label paySubtitle = new Label(r.getCodeConfirmation() + " — Le paiement se fait directement dans l'application.");
        paySubtitle.setStyle("-fx-text-fill: #888; -fx-font-size: 11; -fx-text-alignment: center;");
        paySubtitle.setWrapText(true);

        // Info box
        VBox infoBox = new VBox(6);
        infoBox.setStyle("-fx-background-color: rgba(255,215,0,0.04); -fx-padding: 12; -fx-background-radius: 10;");
        Label infoPts = new Label("\u2B50 Gagnez " + pointsToEarn + " points de fidelite");
        infoPts.setStyle("-fx-text-fill: #B266FF; -fx-font-size: 11;");
        Label infoSec = new Label("\uD83D\uDD12 Paiement securise via Konnect");
        infoSec.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        Label infoWarn = new Label("\u26A0 Cette action est irreversible");
        infoWarn.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 10;");
        infoBox.getChildren().addAll(infoPts, infoSec, infoWarn);

        Button payBtn = new Button("\uD83D\uDCB3 Payer via Konnect - " + String.format("%.2f DT", r.getMontantTotal()));
        payBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 14 40; -fx-background-radius: 12; -fx-font-size: 14; -fx-cursor: hand;");
        payBtn.setMaxWidth(Double.MAX_VALUE);

        Label statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.setMaxWidth(400);

        initialView.getChildren().addAll(mainIcon, payTitle, paySubtitle, infoBox, payBtn, statusLbl);

        // WebView
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webView.setPrefSize(700, 580);

        HBox webStatusBar = new HBox(10);
        webStatusBar.setPadding(new Insets(8, 16, 8, 16));
        webStatusBar.setAlignment(Pos.CENTER_LEFT);
        webStatusBar.setStyle("-fx-background-color: #111; -fx-background-radius: 0 0 16 16;");
        Label webStatusLbl = new Label("Chargement...");
        webStatusLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        Region wSpacer = new Region();
        HBox.setHgrow(wSpacer, Priority.ALWAYS);
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: rgba(255,107,107,0.15); -fx-text-fill: #FF6B6B; -fx-padding: 5 16; -fx-background-radius: 6; -fx-font-size: 10; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> paymentStage.close());
        webStatusBar.getChildren().addAll(new Label("\uD83D\uDD12"), webStatusLbl, wSpacer, cancelBtn);

        VBox webViewContainer = new VBox();
        webViewContainer.getChildren().addAll(webView, webStatusBar);
        VBox.setVgrow(webView, Priority.ALWAYS);

        // URL change listener — detect success/failure
        webEngine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl != null) {
                System.out.println("[KONNECT UPGRADE WEBVIEW] " + newUrl);
                String lower = newUrl.toLowerCase();
                if (lower.contains(com.esprit.services.KonnectPaymentService.SUCCESS_URL.toLowerCase()) || lower.contains("/payment/success")) {
                    webStatusLbl.setText("\u2705 Paiement detecte ! Verification...");
                    webStatusLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 10; -fx-font-weight: bold;");
                    if (paymentIdHolder[0] != null) {
                        new Thread(() -> {
                            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                            com.esprit.services.KonnectPaymentService.PaymentResult verifyResult = konnectSvc.verifyPayment(paymentIdHolder[0]);
                            javafx.application.Platform.runLater(() -> {
                                if (verifyResult.isSuccess()) {
                                    processUpgradeSuccess(r, pointsToEarn);
                                    paymentStage.close();
                                } else {
                                    showUpgradeVerifyScreen(paymentStage, root, r, pointsToEarn, paymentIdHolder[0], konnectSvc);
                                }
                            });
                        }).start();
                    }
                } else if (lower.contains(com.esprit.services.KonnectPaymentService.FAIL_URL.toLowerCase()) || lower.contains("/payment/fail")) {
                    javafx.application.Platform.runLater(() -> {
                        root.setCenter(initialView);
                        statusLbl.setText("\u274C Paiement refuse. Reessayez.");
                        statusLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");
                        payBtn.setDisable(false);
                        payBtn.setText("\uD83D\uDCF1 Reessayer");
                    });
                }
            }
        });

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldS, newS) -> {
            if (newS == Worker.State.RUNNING) webStatusLbl.setText("\u23F3 Chargement...");
            else if (newS == Worker.State.SUCCEEDED) { webStatusLbl.setText("\uD83D\uDD12 Page securisee"); webStatusLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 10;"); }
            else if (newS == Worker.State.FAILED) webStatusLbl.setText("\u26A0 Erreur de chargement");
        });

        // Pay button
        payBtn.setOnAction(e -> {
            payBtn.setDisable(true);
            payBtn.setText("\u23F3 Connexion a Konnect...");
            new Thread(() -> {
                com.esprit.services.KonnectPaymentService.PaymentResult result = konnectSvc.initPayment(
                    r.getMontantTotal(), "TABAANI_UPGRADE_" + r.getCodeConfirmation());
                javafx.application.Platform.runLater(() -> {
                    if (result.isSuccess() && result.getPaymentLink() != null) {
                        paymentIdHolder[0] = result.getPaymentId();
                        webEngine.load(result.getPaymentLink());
                        root.setCenter(webViewContainer);
                        paymentStage.setWidth(750);
                        paymentStage.setHeight(680);
                        paymentStage.centerOnScreen();
                    } else {
                        statusLbl.setText("\u274C " + result.getMessage());
                        statusLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");
                        payBtn.setDisable(false);
                        payBtn.setText("\uD83D\uDCF1 Reessayer");
                    }
                });
            }).start();
        });

        root.setCenter(initialView);
        Scene scene = new Scene(root, 520, 460);
        scene.setFill(Color.TRANSPARENT);
        paymentStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        paymentStage.setScene(scene);
        paymentStage.centerOnScreen();
        paymentStage.showAndWait();
    }

    /**
     * Process successful upgrade from Cash to Konnect.
     */
    private void processUpgradeSuccess(Reservation r, int pointsToEarn) {
        r.setModePaiement("Konnect");
        if (reservationService.modifier(r)) {
            if (currentUser != null) {
                userService.addLoyaltyPoints(currentUser.getId(), pointsToEarn);
                currentUser.setLoyaltyPoints(currentUser.getLoyaltyPoints() + pointsToEarn);
            }
            if (currentUser != null && currentUser.getEmail() != null) {
                new Thread(() -> {
                    String name = currentUser.getPrenom() + " " + currentUser.getNom();
                    int totalPts = userService.getLoyaltyPoints(currentUser.getId());
                    emailService.sendPaymentConfirmationEmail(currentUser.getEmail(), name,
                        r.getNomEtablissement(), r.getCodeConfirmation(), r.getMontantTotal(),
                        "Konnect (upgrade)", pointsToEarn, totalPts);
                }).start();
            }
            showMessage("\u2705 Paiement Konnect confirme ! +" + pointsToEarn + " points gagnes \u2B50", true);
            loadData();
        }
    }

    /**
     * Show manual verify screen if auto-verify didn't confirm yet.
     */
    private void showUpgradeVerifyScreen(Stage paymentStage, BorderPane root, Reservation r,
                                          int pointsToEarn, String konnectPaymentId,
                                          com.esprit.services.KonnectPaymentService konnectSvc) {
        VBox verifyView = new VBox(16);
        verifyView.setPadding(new Insets(30, 24, 30, 24));
        verifyView.setAlignment(Pos.CENTER);
        verifyView.setStyle("-fx-background-color: #1a1a1a;");

        Label checkIcon = new Label("\u23F3");
        checkIcon.setStyle("-fx-font-size: 48;");
        Label verifyTitle = new Label("Verification du paiement");
        verifyTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");
        Label verifyMsg = new Label("Cliquez pour verifier votre paiement.");
        verifyMsg.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        Label verifyStatus = new Label("");
        verifyStatus.setWrapText(true);

        Button verifyBtn = new Button("\u2705 Verifier le paiement");
        verifyBtn.setStyle("-fx-background-color: rgba(81,207,102,0.2); -fx-text-fill: #51CF66; -fx-font-weight: bold; -fx-padding: 12 32; -fx-background-radius: 10; -fx-font-size: 13; -fx-cursor: hand;");
        verifyBtn.setMaxWidth(Double.MAX_VALUE);

        verifyBtn.setOnAction(e -> {
            verifyBtn.setDisable(true);
            verifyBtn.setText("\u23F3 Verification...");
            new Thread(() -> {
                com.esprit.services.KonnectPaymentService.PaymentResult result = konnectSvc.verifyPayment(konnectPaymentId);
                javafx.application.Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        processUpgradeSuccess(r, pointsToEarn);
                        new Thread(() -> {
                            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                            javafx.application.Platform.runLater(paymentStage::close);
                        }).start();
                    } else {
                        verifyStatus.setText("\u23F3 " + result.getMessage());
                        verifyStatus.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 11;");
                        verifyBtn.setDisable(false);
                        verifyBtn.setText("\u2705 Verifier le paiement");
                    }
                });
            }).start();
        });

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: rgba(255,107,107,0.1); -fx-text-fill: #FF6B6B; -fx-padding: 8 24; -fx-background-radius: 8; -fx-font-size: 11; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> paymentStage.close());

        verifyView.getChildren().addAll(checkIcon, verifyTitle, verifyMsg, verifyBtn, verifyStatus, cancelBtn);
        root.setCenter(verifyView);
        paymentStage.setWidth(520);
        paymentStage.setHeight(420);
        paymentStage.centerOnScreen();
    }

    // ================================================================
    // MODIFY RESERVATION DIALOG (only mode de paiement: Especes -> Carte)
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
        Label titleLbl = new Label("Modifier le Mode de Paiement");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");
        String dispName = r.getNomEtablissement() != null && !r.getNomEtablissement().isEmpty()
                ? r.getNomEtablissement() : "Reservation";
        Label subLbl = new Label(dispName + " | " + (r.getCodeConfirmation() != null ? r.getCodeConfirmation() : ""));
        subLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        headerText.getChildren().addAll(titleLbl, subLbl);
        headerRow.getChildren().addAll(headerIcon, headerText);

        boolean isRestoReservation = isRestoOrCafeType(r.getTypeService());

        // Current info
        VBox currentInfo = new VBox(8);
        currentInfo.setStyle("-fx-background-color: rgba(255,215,0,0.04); -fx-padding: 14; -fx-background-radius: 10; -fx-border-color: rgba(255,215,0,0.1); -fx-border-radius: 10; -fx-border-width: 1;");
        Label curTitle = new Label("\uD83D\uDCCB Informations actuelles");
        curTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");
        HBox curRow1 = buildInfoChip("\uD83D\uDCB0 Montant", isRestoReservation ? "Paiement sur place" : String.format("%.2f DT", r.getMontantTotal()));
        HBox curRow2 = buildInfoChip("\uD83D\uDCB3 Mode", r.getModePaiement() != null ? r.getModePaiement() : "N/A");
        HBox curRow3 = buildInfoChip("\uD83D\uDCCA Statut", r.getStatutPaiement() != null ? r.getStatutPaiement() : "N/A");
        currentInfo.getChildren().addAll(curTitle, curRow1, curRow2, curRow3);

        // Mode de paiement section - only thing that can be changed
        VBox editSection = new VBox(14);
        editSection.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 16; -fx-background-radius: 10;");
        Label editTitle = new Label("\u2728 Modifier le mode de paiement");
        editTitle.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 12; -fx-font-weight: bold;");

        boolean isCurrentlyCard = "Konnect".equalsIgnoreCase(r.getModePaiement());
        boolean isCash = "Especes".equalsIgnoreCase(r.getModePaiement());
        VBox modeBox = new VBox(4);
        Label modeLbl = new Label("Mode de Paiement");
        modeLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        ComboBox<String> modeCombo = new ComboBox<>();

        if (isRestoReservation) {
            modeCombo.getItems().add("Especes");
            modeCombo.setValue("Especes");
            modeCombo.setDisable(true);
            Label lockLbl = new Label("\uD83C\uDF7D Paiement sur place uniquement");
            lockLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 9;");
            modeBox.getChildren().addAll(modeLbl, modeCombo, lockLbl);
        } else if (isCurrentlyCard) {
            modeCombo.getItems().add("Konnect");
            modeCombo.setValue("Konnect");
            modeCombo.setDisable(true);
            Label lockLbl = new Label("\uD83D\uDD12 Le mode Konnect ne peut pas etre change en Especes");
            lockLbl.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 9;");
            modeBox.getChildren().addAll(modeLbl, modeCombo, lockLbl);
        } else if (isCash) {
            modeCombo.getItems().addAll("Especes", "Konnect");
            modeCombo.setValue("Especes");
            modeCombo.setMaxWidth(Double.MAX_VALUE);
            int pointsToEarn = (int)(r.getMontantTotal() * 10);
            Label upgradeTip = new Label("\u2B50 Passez en Konnect pour gagner " + pointsToEarn + " points de fidelite !");
            upgradeTip.setStyle("-fx-text-fill: #B266FF; -fx-font-size: 9;");
            modeBox.getChildren().addAll(modeLbl, modeCombo, upgradeTip);
        } else {
            modeCombo.getItems().addAll("Especes", "Konnect");
            modeCombo.setValue(r.getModePaiement() != null ? r.getModePaiement() : "Especes");
            modeCombo.setMaxWidth(Double.MAX_VALUE);
            modeBox.getChildren().addAll(modeLbl, modeCombo);
        }
        modeCombo.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");

        // Info note about what can't be changed
        VBox infoNote = new VBox(6);
        infoNote.setStyle("-fx-background-color: rgba(100,181,246,0.04); -fx-padding: 12; -fx-background-radius: 8;");
        Label infoIcon = new Label("\u2139 Le montant et le statut ne peuvent pas etre modifies ici");
        infoIcon.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 10;");
        Label infoDesc = new Label("Le montant est fixe lors de la reservation. Le statut est gere par l'administrateur pour les paiements en especes.");
        infoDesc.setStyle("-fx-text-fill: #666; -fx-font-size: 9;");
        infoDesc.setWrapText(true);
        infoNote.getChildren().addAll(infoIcon, infoDesc);

        editSection.getChildren().addAll(editTitle, modeBox, infoNote);

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
            String oldMode = r.getModePaiement();
            String newMode = modeCombo.getValue();

            // Nothing changed
            if (oldMode != null && oldMode.equalsIgnoreCase(newMode)) {
                showMessage("\u2139 Aucune modification effectuee.", true);
                return;
            }

            // Award points if upgrading from Especes to Carte
            boolean upgrading = "Especes".equalsIgnoreCase(oldMode) && "Konnect".equalsIgnoreCase(newMode);
            if (upgrading && currentUser != null) {
                int pts = (int)(r.getMontantTotal() * 10);
                userService.addLoyaltyPoints(currentUser.getId(), pts);
                currentUser.setLoyaltyPoints(currentUser.getLoyaltyPoints() + pts);
                showMessage("\u2B50 +" + pts + " points de fidelite pour le passage en carte !", true);
            }

            r.setModePaiement(newMode);

            if (reservationService.modifier(r)) {
                showMessage("\u2705 Mode de paiement modifie avec succes !", true);
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
    // RESERVATION DETAIL DIALOG (with loyalty points)
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
        if (isRestoOrCafeType(r.getTypeService())) {
            addDetailRow(detailRows, "\uD83D\uDCB0 Montant", "Paiement sur place", "#51CF66");
        } else {
            addDetailRow(detailRows, "\uD83D\uDCB0 Montant", String.format("%.2f DT", r.getMontantTotal()), "#FFD700");
        }
        addDetailRow(detailRows, "\uD83D\uDCB3 Mode", r.getModePaiement() != null ? r.getModePaiement() : "N/A", "#64B5F6");
        addDetailRow(detailRows, "\uD83C\uDFAF Service", r.getTypeService() != null ? r.getTypeService() : "N/A", "white");
        addDetailRow(detailRows, "\uD83D\uDC65 Personnes", String.valueOf(r.getNbPersonnes()), "white");
        addDetailRow(detailRows, "\uD83D\uDCC5 Date", r.getDatePaiement() != null ? r.getDatePaiement().format(DTF) : "N/A", "#999");
        addDetailRow(detailRows, "\u23F0 Temps", getTimeAgo(r), "#888");

        // Payment mode badge with lock info
        boolean isCard = "Konnect".equalsIgnoreCase(r.getModePaiement());
        if (isCard) {
            HBox cardBadge = new HBox(8);
            cardBadge.setAlignment(Pos.CENTER);
            cardBadge.setStyle("-fx-background-color: rgba(100,181,246,0.06); -fx-padding: 8; -fx-background-radius: 8;");
            Label lockIcon = new Label("\uD83D\uDD12");
            lockIcon.setStyle("-fx-font-size: 11;");
            Label lockLbl = new Label("Paiement par carte verrouille - impossible de revenir en especes");
            lockLbl.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 10;");
            cardBadge.getChildren().addAll(lockIcon, lockLbl);
            detailRows.getChildren().add(cardBadge);
        }

        String statutColor = getStatutColor(r.getStatutPaiement());
        String statutIcon = getStatutIcon(r.getStatutPaiement());
        HBox statutRow = new HBox(8);
        statutRow.setAlignment(Pos.CENTER);
        statutRow.setStyle("-fx-background-color: " + statutColor.replace(")", ",0.08)").replace("rgb(", "rgba(") + "; -fx-padding: 10; -fx-background-radius: 10;");
        Label statutLabel = new Label(statutIcon + " Statut: " + (r.getStatutPaiement() != null ? r.getStatutPaiement() : "Inconnu"));
        statutLabel.setStyle("-fx-text-fill: " + statutColor + "; -fx-font-size: 14; -fx-font-weight: bold;");
        statutRow.getChildren().add(statutLabel);

        // Loyalty info
        VBox loyaltyInfo = new VBox(6);
        loyaltyInfo.setAlignment(Pos.CENTER);
        loyaltyInfo.setStyle("-fx-background-color: rgba(178,102,255,0.06); -fx-padding: 12; -fx-background-radius: 10;");
        int userPoints = currentUser != null ? userService.getLoyaltyPoints(currentUser.getId()) : 0;
        Label loyaltyLbl = new Label("\u2B50 Votre solde fidelite: " + userPoints + " points");
        loyaltyLbl.setStyle("-fx-text-fill: #B266FF; -fx-font-size: 12; -fx-font-weight: bold;");
        loyaltyInfo.getChildren().add(loyaltyLbl);

        content.getChildren().addAll(headerIcon, titleLbl, nameLbl, progressInDetail, detailRows, statutRow, loyaltyInfo);
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
    // INVOICE GENERATION
    // ================================================================
    private void generateInvoice(Reservation r) {
        boolean isResto = isRestoOrCafeType(r.getTypeService());

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer la Facture");
        fileChooser.setInitialFileName("Facture_" + (r.getCodeConfirmation() != null ? r.getCodeConfirmation() : "RES") + ".html");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier HTML", "*.html"));
        Stage stage = (Stage) titleBar.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            String clientName = currentUser != null ? (currentUser.getNom() + " " + currentUser.getPrenom()) : "Client";
            String datePaiement = r.getDatePaiement() != null ? r.getDatePaiement().format(DTF) : "N/A";
            String serviceIcon = getServiceIcon(r.getTypeService());
            String montantDisplay = isResto ? "Paiement sur place" : String.format("%.2f DT", r.getMontantTotal());
            String statutDisplay = r.getStatutPaiement() != null ? r.getStatutPaiement() : "N/A";
            String modeDisplay = r.getModePaiement() != null ? r.getModePaiement() : "N/A";

            pw.println("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>");
            pw.println("<title>Facture - " + (r.getCodeConfirmation() != null ? r.getCodeConfirmation() : "") + "</title>");
            pw.println("<style>");
            pw.println("* { margin: 0; padding: 0; box-sizing: border-box; }");
            pw.println("body { font-family: 'Segoe UI', Tahoma, sans-serif; background: #0a0a0a; color: #ddd; padding: 40px; }");
            pw.println(".invoice { max-width: 700px; margin: 0 auto; background: #111; border: 1px solid rgba(255,215,0,0.2); border-radius: 16px; overflow: hidden; }");
            pw.println(".header { background: linear-gradient(135deg, #1a1a1a, #0f0f0f); padding: 32px; text-align: center; border-bottom: 2px solid #FFD700; }");
            pw.println(".header h1 { color: #FFD700; font-size: 28px; margin-bottom: 4px; }");
            pw.println(".header p { color: #888; font-size: 12px; }");
            pw.println(".body { padding: 32px; }");
            pw.println(".info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px; }");
            pw.println(".info-box { background: rgba(255,255,255,0.03); padding: 16px; border-radius: 10px; border: 1px solid rgba(255,215,0,0.08); }");
            pw.println(".info-box .label { color: #888; font-size: 11px; text-transform: uppercase; margin-bottom: 6px; }");
            pw.println(".info-box .value { color: white; font-size: 15px; font-weight: 600; }");
            pw.println(".total-section { background: linear-gradient(135deg, rgba(255,215,0,0.08), rgba(255,140,0,0.06)); padding: 24px; border-radius: 12px; text-align: center; margin: 24px 0; border: 1px solid rgba(255,215,0,0.15); }");
            pw.println(".total-section .amount { color: #FFD700; font-size: 32px; font-weight: bold; }");
            pw.println(".total-section .label { color: #aaa; font-size: 12px; margin-bottom: 8px; }");
            pw.println(".footer { background: #0a0a0a; padding: 20px 32px; text-align: center; border-top: 1px solid rgba(255,215,0,0.1); }");
            pw.println(".footer p { color: #555; font-size: 11px; }");
            pw.println(".badge { display: inline-block; padding: 4px 14px; border-radius: 20px; font-size: 12px; font-weight: bold; }");
            pw.println(".badge-gold { background: rgba(255,215,0,0.12); color: #FFD700; }");
            pw.println(".badge-green { background: rgba(81,207,102,0.12); color: #51CF66; }");
            pw.println("</style></head><body>");
            pw.println("<div class='invoice'>");
            pw.println("<div class='header'>");
            pw.println("<h1>\u2728 SmartTravel</h1>");
            pw.println("<p>FACTURE DE RESERVATION</p>");
            pw.println("<p style='margin-top:8px;color:#FFD700;font-size:14px;'>" + serviceIcon + " " + (r.getCodeConfirmation() != null ? r.getCodeConfirmation() : "N/A") + "</p>");
            pw.println("</div>");
            pw.println("<div class='body'>");
            pw.println("<div class='info-grid'>");
            pw.println("<div class='info-box'><div class='label'>\uD83D\uDC64 Client</div><div class='value'>" + clientName + "</div></div>");
            pw.println("<div class='info-box'><div class='label'>\uD83D\uDCC5 Date</div><div class='value'>" + datePaiement + "</div></div>");
            pw.println("<div class='info-box'><div class='label'>\uD83C\uDFE8 Etablissement</div><div class='value'>" + (r.getNomEtablissement() != null ? r.getNomEtablissement() : "N/A") + "</div></div>");
            pw.println("<div class='info-box'><div class='label'>\uD83C\uDFAF Type de Service</div><div class='value'>" + (r.getTypeService() != null ? r.getTypeService() : "N/A") + "</div></div>");
            pw.println("<div class='info-box'><div class='label'>\uD83D\uDC65 Personnes</div><div class='value'>" + r.getNbPersonnes() + " personne(s)</div></div>");
            pw.println("<div class='info-box'><div class='label'>\uD83D\uDCB3 Mode de Paiement</div><div class='value'>" + modeDisplay + "</div></div>");
            pw.println("</div>");
            pw.println("<div class='total-section'>");
            pw.println("<div class='label'>MONTANT TOTAL</div>");
            pw.println("<div class='amount'>" + montantDisplay + "</div>");
            pw.println("<div style='margin-top:10px;'><span class='badge " + (statutDisplay.toLowerCase().contains("pay") ? "badge-green" : "badge-gold") + "'>" + getStatutIcon(statutDisplay) + " " + statutDisplay + "</span></div>");
            pw.println("</div>");
            if (isResto) {
                pw.println("<div style='background:rgba(81,207,102,0.06);padding:14px;border-radius:10px;text-align:center;border:1px solid rgba(81,207,102,0.15);'>");
                pw.println("<p style='color:#51CF66;font-size:13px;font-weight:bold;'>\uD83C\uDF7D Le paiement s'effectue directement a l'etablissement</p>");
                pw.println("</div>");
            }
            pw.println("</div>");
            pw.println("<div class='footer'>");
            pw.println("<p>Merci pour votre confiance ! \u2764 SmartTravel &copy; 2025</p>");
            pw.println("<p style='margin-top:4px;'>Ce document fait office de facture pour votre reservation.</p>");
            pw.println("</div></div></body></html>");

            // Show success and open the file
            showMessage("\u2705 Facture generee: " + file.getName(), true);
            try {
                java.awt.Desktop.getDesktop().browse(file.toURI());
            } catch (Exception ex) {
                // If can't open browser, just show success message
            }
        } catch (Exception e) {
            showMessage("\u274C Erreur lors de la generation de la facture.", false);
            e.printStackTrace();
        }
    }

    // ================================================================
    // SHARE RESERVATION (Copy to Clipboard)
    // ================================================================
    private void shareReservation(Reservation r) {
        boolean isResto = isRestoOrCafeType(r.getTypeService());
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════\n");
        sb.append("   \u2728 SmartTravel - Reservation\n");
        sb.append("═══════════════════════════════\n\n");
        sb.append("\uD83D\uDD11 Code: ").append(r.getCodeConfirmation() != null ? r.getCodeConfirmation() : "N/A").append("\n");
        sb.append("\uD83C\uDFE8 ").append(r.getNomEtablissement() != null ? r.getNomEtablissement() : "N/A").append("\n");
        sb.append("\uD83C\uDFAF Type: ").append(r.getTypeService() != null ? r.getTypeService() : "N/A").append("\n");
        sb.append("\uD83D\uDCC5 Date: ").append(r.getDatePaiement() != null ? r.getDatePaiement().format(DTF) : "N/A").append("\n");
        sb.append("\uD83D\uDC65 Personnes: ").append(r.getNbPersonnes()).append("\n");
        if (isResto) {
            sb.append("\uD83D\uDCB0 Paiement sur place\n");
        } else {
            sb.append("\uD83D\uDCB0 Montant: ").append(String.format("%.2f DT", r.getMontantTotal())).append("\n");
        }
        sb.append("\uD83D\uDCB3 Mode: ").append(r.getModePaiement() != null ? r.getModePaiement() : "N/A").append("\n");
        sb.append(getStatutIcon(r.getStatutPaiement())).append(" Statut: ").append(r.getStatutPaiement() != null ? r.getStatutPaiement() : "N/A").append("\n");
        sb.append("\n═══════════════════════════════\n");
        sb.append("Partage via SmartTravel \u2764\n");

        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(sb.toString());
        clipboard.setContent(cc);

        showMessage("\uD83D\uDCCB Reservation copiee dans le presse-papiers !", true);
    }

    // ================================================================
    // COUNTDOWN TIMER DIALOG
    // ================================================================
    private void showCountdownDialog(Reservation r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Compte a Rebours");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setAlignment(Pos.CENTER);
        content.setMinWidth(420);

        StackPane timerIcon = new StackPane();
        timerIcon.setPrefSize(60, 60);
        timerIcon.setStyle("-fx-background-color: rgba(255,215,0,0.1); -fx-background-radius: 30;");
        Label tIcon = new Label("\u23F0");
        tIcon.setStyle("-fx-font-size: 28;");
        timerIcon.getChildren().add(tIcon);

        Label titleLbl = new Label("Compte a Rebours");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");

        String etabName = r.getNomEtablissement() != null ? r.getNomEtablissement() : "Reservation";
        Label subtitleLbl = new Label(getServiceIcon(r.getTypeService()) + " " + etabName);
        subtitleLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 13;");

        // Calculate countdown
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime target = r.getDatePaiement();

        VBox countdownBox = new VBox(8);
        countdownBox.setAlignment(Pos.CENTER);
        countdownBox.setStyle("-fx-background-color: rgba(255,215,0,0.06); -fx-padding: 20; -fx-background-radius: 12; -fx-border-color: rgba(255,215,0,0.12); -fx-border-radius: 12; -fx-border-width: 1;");

        if (target != null && target.isAfter(now)) {
            long totalMinutes = ChronoUnit.MINUTES.between(now, target);
            long days = totalMinutes / (24 * 60);
            long hours = (totalMinutes % (24 * 60)) / 60;
            long minutes = totalMinutes % 60;

            HBox timerRow = new HBox(16);
            timerRow.setAlignment(Pos.CENTER);

            VBox daysBox = buildTimerUnit(String.valueOf(days), "JOURS");
            VBox hoursBox = buildTimerUnit(String.valueOf(hours), "HEURES");
            VBox minsBox = buildTimerUnit(String.valueOf(minutes), "MIN");

            Label sep1 = new Label(":");
            sep1.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 28; -fx-font-weight: bold;");
            Label sep2 = new Label(":");
            sep2.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 28; -fx-font-weight: bold;");

            timerRow.getChildren().addAll(daysBox, sep1, hoursBox, sep2, minsBox);
            countdownBox.getChildren().add(timerRow);

            Label statusLbl = new Label("\u2705 Votre reservation approche !");
            statusLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 12; -fx-font-weight: bold;");
            countdownBox.getChildren().add(statusLbl);
        } else {
            Label passedLbl = new Label("\uD83C\uDFC1 Reservation deja passee !");
            passedLbl.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 16; -fx-font-weight: bold;");
            if (target != null) {
                long daysAgo = ChronoUnit.DAYS.between(target, now);
                Label agoLbl = new Label("Il y a " + daysAgo + " jour(s)");
                agoLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
                countdownBox.getChildren().addAll(passedLbl, agoLbl);
            } else {
                countdownBox.getChildren().add(passedLbl);
            }
        }

        content.getChildren().addAll(timerIcon, titleLbl, subtitleLbl, countdownBox);
        dp.setContent(content);
        dp.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.showAndWait();
    }

    private VBox buildTimerUnit(String value, String label) {
        VBox unit = new VBox(2);
        unit.setAlignment(Pos.CENTER);
        unit.setStyle("-fx-background-color: rgba(255,215,0,0.1); -fx-padding: 10 18; -fx-background-radius: 10;");
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 28; -fx-font-weight: bold; -fx-font-family: 'Consolas';");
        Label lblLbl = new Label(label);
        lblLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 9; -fx-font-weight: bold;");
        unit.getChildren().addAll(valLbl, lblLbl);
        return unit;
    }

    // ================================================================
    // RESERVATION RATING DIALOG
    // ================================================================
    private void showRatingDialog(Reservation r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Evaluer la Reservation");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setAlignment(Pos.CENTER);
        content.setMinWidth(420);

        StackPane ratingIcon = new StackPane();
        ratingIcon.setPrefSize(60, 60);
        ratingIcon.setStyle("-fx-background-color: rgba(255,215,0,0.1); -fx-background-radius: 30;");
        Label rIcon = new Label("\u2B50");
        rIcon.setStyle("-fx-font-size: 28;");
        ratingIcon.getChildren().add(rIcon);

        Label titleLbl = new Label("Evaluer Votre Experience");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");

        String etabName = r.getNomEtablissement() != null ? r.getNomEtablissement() : "Reservation";
        Label subtitleLbl = new Label(getServiceIcon(r.getTypeService()) + " " + etabName);
        subtitleLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 13;");

        // Star rating
        HBox starsRow = new HBox(8);
        starsRow.setAlignment(Pos.CENTER);
        final int[] selectedRating = {0};
        Label[] starLabels = new Label[5];

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            Label star = new Label("\u2606");
            star.setStyle("-fx-font-size: 32; -fx-text-fill: #444; -fx-cursor: hand;");
            starLabels[i] = star;
            star.setOnMouseEntered(e -> {
                for (int j = 0; j < 5; j++) {
                    starLabels[j].setText(j < starIndex ? "\u2605" : "\u2606");
                    starLabels[j].setStyle("-fx-font-size: 32; -fx-cursor: hand; -fx-text-fill: " + (j < starIndex ? "#FFD700" : "#444") + ";");
                }
            });
            star.setOnMouseExited(e -> {
                for (int j = 0; j < 5; j++) {
                    starLabels[j].setText(j < selectedRating[0] ? "\u2605" : "\u2606");
                    starLabels[j].setStyle("-fx-font-size: 32; -fx-cursor: hand; -fx-text-fill: " + (j < selectedRating[0] ? "#FFD700" : "#444") + ";");
                }
            });
            star.setOnMouseClicked(e -> {
                selectedRating[0] = starIndex;
                for (int j = 0; j < 5; j++) {
                    starLabels[j].setText(j < starIndex ? "\u2605" : "\u2606");
                    starLabels[j].setStyle("-fx-font-size: 32; -fx-cursor: hand; -fx-text-fill: " + (j < starIndex ? "#FFD700" : "#444") + ";");
                }
            });
            starsRow.getChildren().add(star);
        }

        Label ratingText = new Label("Cliquez sur les etoiles pour evaluer");
        ratingText.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");

        // Comment
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Partagez votre experience... (optionnel)");
        commentArea.setPrefRowCount(3);
        commentArea.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-prompt-text-fill: #555; -fx-background-radius: 10; -fx-border-color: rgba(255,215,0,0.15); -fx-border-radius: 10; -fx-font-size: 12;");

        // Emoji quick reactions
        HBox emojiRow = new HBox(10);
        emojiRow.setAlignment(Pos.CENTER);
        String[] emojis = {"\uD83D\uDE0D", "\uD83D\uDE0A", "\uD83D\uDE10", "\uD83D\uDE1E", "\uD83D\uDE21"};
        String[] emojiLabels = {"Excellent", "Bien", "Moyen", "Mauvais", "Terrible"};
        for (int i = 0; i < emojis.length; i++) {
            VBox emojiBox = new VBox(4);
            emojiBox.setAlignment(Pos.CENTER);
            Label emojiLbl = new Label(emojis[i]);
            emojiLbl.setStyle("-fx-font-size: 24; -fx-cursor: hand;");
            Label emojiName = new Label(emojiLabels[i]);
            emojiName.setStyle("-fx-text-fill: #555; -fx-font-size: 8;");
            final int rating = 5 - i;
            emojiLbl.setOnMouseClicked(e -> {
                selectedRating[0] = rating;
                for (int j = 0; j < 5; j++) {
                    starLabels[j].setText(j < rating ? "\u2605" : "\u2606");
                    starLabels[j].setStyle("-fx-font-size: 32; -fx-cursor: hand; -fx-text-fill: " + (j < rating ? "#FFD700" : "#444") + ";");
                }
            });
            emojiBox.getChildren().addAll(emojiLbl, emojiName);
            emojiRow.getChildren().add(emojiBox);
        }

        content.getChildren().addAll(ratingIcon, titleLbl, subtitleLbl, starsRow, ratingText, emojiRow, commentArea);
        dp.setContent(content);

        ButtonType submitType = new ButtonType("\u2B50 Envoyer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(submitType, cancelType);

        Button submitBtn = (Button) dp.lookupButton(submitType);
        submitBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == submitType) {
            if (selectedRating[0] > 0) {
                // Save rating to database
                String comment = commentArea.getText() != null ? commentArea.getText().trim() : "";
                reservationService.saveRating(r.getIdReservation(), selectedRating[0], comment);
                r.setRating(selectedRating[0]);
                r.setReviewComment(comment);

                // Award loyalty points for leaving a review
                if (currentUser != null) {
                    int bonusPoints = selectedRating[0] * 5;
                    userService.addLoyaltyPoints(currentUser.getId(), bonusPoints);
                    currentUser.setLoyaltyPoints(currentUser.getLoyaltyPoints() + bonusPoints);
                    showMessage("\u2B50 Merci ! " + selectedRating[0] + "/5 etoiles - +" + bonusPoints + " pts fidelite !", true);
                } else {
                    showMessage("\u2B50 Merci pour votre evaluation: " + selectedRating[0] + "/5 etoiles !", true);
                }
                loadData(); // Refresh to show rating on card
            } else {
                showMessage("\u274C Veuillez selectionner au moins une etoile.", false);
            }
        }
    }

    // ================================================================
    // ACTIONS
    // ================================================================
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
    @FXML public void handleToggleFullscreen() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
        if (fullscreenBtn != null) fullscreenBtn.setText(stage.isFullScreen() ? "\u29C9" : "\u26F6");
    }

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
            ThemeManager.applyTheme(newScene);
            ThemeManager.trackScene(newScene);
            newRoot.setOpacity(0); newRoot.setScaleX(1.03); newRoot.setScaleY(1.03); newRoot.setTranslateY(8);
            stage.setScene(newScene); stage.setTitle(title);
            stage.setMaximized(true);
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

    // ================================================================
    // THEME TOGGLE BUTTON
    // ================================================================
    private void addThemeToggleButton() {
        Button themeBtn = new Button(ThemeManager.themeIcon() + " " + ThemeManager.modeLabel());
        themeBtn.setStyle(ThemeManager.themeToggleBtnStyle());
        themeBtn.setOnMouseEntered(e -> themeBtn.setStyle(ThemeManager.themeToggleBtnStyle().replace("0.1;", "0.2;").replace("0.08;", "0.15;")));
        themeBtn.setOnMouseExited(e -> themeBtn.setStyle(ThemeManager.themeToggleBtnStyle()));
        themeBtn.setOnAction(e -> {
            ThemeManager.toggleTheme();
            themeBtn.setText(ThemeManager.themeIcon() + " " + ThemeManager.modeLabel());
            themeBtn.setStyle(ThemeManager.themeToggleBtnStyle());
            if (currentUser != null) {
                new Thread(() -> userService.saveThemePreference(
                    currentUser.getId(), ThemeManager.getCurrentMode().name()
                )).start();
            }
        });
        Tooltip tooltip = new Tooltip("Theme: Sombre / Clair / Systeme");
        tooltip.setStyle("-fx-font-size: 11;");
        themeBtn.setTooltip(tooltip);

        int insertIdx = titleBar.getChildren().size() - 1;
        Region spacer = new Region();
        spacer.setPrefWidth(6);
        titleBar.getChildren().add(insertIdx, spacer);
        titleBar.getChildren().add(insertIdx + 1, themeBtn);
    }

    // ================================================================
    // AI CHATBOT
    // ================================================================
    private VBox chatPanel;
    private VBox chatMessages;
    private boolean chatOpen = false;

    private void addChatbotButton() {
        Button chatBtn = new Button("\uD83E\uDD16");
        chatBtn.setStyle("-fx-background-color: " + ThemeManager.chatBtnGradient() + "; -fx-text-fill: white; -fx-font-size: 16; " +
                "-fx-padding: 6 10; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);");
        chatBtn.setOnMouseEntered(e -> chatBtn.setStyle("-fx-background-color: " + ThemeManager.chatBtnHoverGradient() + "; -fx-text-fill: white; -fx-font-size: 16; " +
                "-fx-padding: 6 10; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 3);"));
        chatBtn.setOnMouseExited(e -> chatBtn.setStyle("-fx-background-color: " + ThemeManager.chatBtnGradient() + "; -fx-text-fill: white; -fx-font-size: 16; " +
                "-fx-padding: 6 10; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);"));
        chatBtn.setOnAction(e -> toggleChatPanel());
        Tooltip chatTip = new Tooltip("Assistant IA Tabaani");
        chatTip.setStyle("-fx-font-size: 11;");
        chatBtn.setTooltip(chatTip);

        int insertIdx = titleBar.getChildren().size() - 1;
        Region spacer = new Region();
        spacer.setPrefWidth(6);
        titleBar.getChildren().add(insertIdx, spacer);
        titleBar.getChildren().add(insertIdx + 1, chatBtn);
    }

    private void toggleChatPanel() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        StackPane rootPane = findRootStackPane(stage);
        if (rootPane == null) return;

        if (chatOpen && chatPanel != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), chatPanel);
            fadeOut.setToValue(0);
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), chatPanel);
            slideOut.setToY(20);
            ParallelTransition close = new ParallelTransition(fadeOut, slideOut);
            close.setOnFinished(e -> { rootPane.getChildren().remove(chatPanel); chatPanel = null; });
            close.play();
            chatOpen = false;
            return;
        }

        chatOpen = true;
        chatPanel = new VBox(0);
        chatPanel.setMaxWidth(380);
        chatPanel.setMaxHeight(520);
        chatPanel.setStyle("-fx-background-color: " + ThemeManager.cardBg() + "; -fx-background-radius: 16; " +
                "-fx-border-color: " + ThemeManager.accentBorder() + "; -fx-border-radius: 16; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 5);");
        StackPane.setAlignment(chatPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatPanel, new Insets(0, 30, 30, 0));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-background-color: " + (ThemeManager.isDark() ? "linear-gradient(to right, rgba(255,215,0,0.15), rgba(255,140,0,0.08))" : "linear-gradient(to right, rgba(37,99,235,0.1), rgba(59,130,246,0.05))") + "; -fx-background-radius: 16 16 0 0;");

        StackPane botAvatar = new StackPane();
        botAvatar.setPrefSize(32, 32);
        botAvatar.setStyle("-fx-background-color: " + ThemeManager.chatBtnGradient() + "; -fx-background-radius: 16;");
        Label botIcon = new Label("\uD83E\uDD16");
        botIcon.setStyle("-fx-font-size: 14;");
        botAvatar.getChildren().add(botIcon);

        VBox headerText = new VBox(1);
        Label chatTitle = new Label("Tabaani AI Assistant");
        chatTitle.setStyle("-fx-text-fill: " + ThemeManager.accent() + "; -fx-font-size: 13; -fx-font-weight: bold;");
        Label chatSub = new Label("En ligne - Pret a vous aider");
        chatSub.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 9;");
        headerText.getChildren().addAll(chatTitle, chatSub);
        HBox.setHgrow(headerText, Priority.ALWAYS);

        Button closeChat = new Button("\u2715");
        closeChat.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.textSecondary() + "; -fx-font-size: 12; -fx-cursor: hand;");
        closeChat.setOnAction(e -> toggleChatPanel());
        header.getChildren().addAll(botAvatar, headerText, closeChat);

        chatMessages = new VBox(8);
        chatMessages.setPadding(new Insets(12));
        ScrollPane msgScroll = new ScrollPane(chatMessages);
        msgScroll.setFitToWidth(true);
        msgScroll.setPrefHeight(340);
        msgScroll.setStyle("-fx-background: " + ThemeManager.bg() + "; -fx-background-color: " + ThemeManager.bg() + "; -fx-border-width: 0;");
        msgScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(msgScroll, Priority.ALWAYS);

        addBotMessage("Bonjour! \uD83D\uDC4B Je suis votre assistant Tabaani AI.\n\n" +
                "\u2022 Infos sur vos reservations\n" +
                "\u2022 Suivi de paiements\n" +
                "\u2022 Suggestions de destinations\n\n" +
                "Comment puis-je vous aider ?");

        HBox quickActions = new HBox(6);
        quickActions.setPadding(new Insets(8, 12, 4, 12));
        quickActions.setAlignment(Pos.CENTER);
        String[][] actions = {
                {"\uD83D\uDCB3 Paiements", "Resume mes paiements et reservations recentes"},
                {"\uD83C\uDF0D Destinations", "Quelles destinations me recommandes-tu ?"},
                {"\uD83D\uDCA1 Conseils", "Donne-moi des conseils pour economiser sur mes voyages"}
        };
        for (String[] action : actions) {
            Button qBtn = new Button(action[0]);
            qBtn.setStyle("-fx-background-color: " + ThemeManager.accentHover() + "; -fx-text-fill: " + ThemeManager.accent() + "; " +
                    "-fx-font-size: 9; -fx-padding: 5 10; -fx-background-radius: 12; -fx-cursor: hand; " +
                    "-fx-border-color: " + ThemeManager.accentBorder() + "; -fx-border-radius: 12;");
            qBtn.setOnAction(e -> sendChatMessage(action[1]));
            quickActions.getChildren().add(qBtn);
        }

        HBox inputArea = new HBox(8);
        inputArea.setPadding(new Insets(10, 12, 14, 12));
        inputArea.setAlignment(Pos.CENTER);
        inputArea.setStyle("-fx-border-color: " + ThemeManager.borderColor() + "; -fx-border-width: 1 0 0 0;");

        TextField chatInput = new TextField();
        chatInput.setPromptText("Ecrivez votre message...");
        chatInput.setStyle("-fx-background-color: " + ThemeManager.inputBg() + "; -fx-text-fill: " + ThemeManager.textPrimary() + "; " +
                "-fx-prompt-text-fill: " + ThemeManager.textSecondary() + "; -fx-padding: 10 14; -fx-background-radius: 20; " +
                "-fx-border-color: " + ThemeManager.accentBorder() + "; -fx-border-radius: 20; -fx-font-size: 12;");
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        Button sendBtn = new Button("\u27A4");
        sendBtn.setStyle("-fx-background-color: " + ThemeManager.chatBtnGradient() + "; -fx-text-fill: white; " +
                "-fx-font-size: 14; -fx-padding: 8 12; -fx-background-radius: 20; -fx-cursor: hand;");
        sendBtn.setOnAction(e -> {
            String msg = chatInput.getText().trim();
            if (!msg.isEmpty()) { sendChatMessage(msg); chatInput.clear(); }
        });
        chatInput.setOnAction(e -> sendBtn.fire());
        inputArea.getChildren().addAll(chatInput, sendBtn);

        chatPanel.getChildren().addAll(header, msgScroll, quickActions, inputArea);
        chatPanel.setOpacity(0);
        chatPanel.setTranslateY(20);
        rootPane.getChildren().add(chatPanel);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), chatPanel);
        fadeIn.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), chatPanel);
        slideIn.setToY(0);
        new ParallelTransition(fadeIn, slideIn).play();
    }

    private void addBotMessage(String text) {
        HBox msgRow = new HBox(8);
        msgRow.setAlignment(Pos.TOP_LEFT);
        msgRow.setPadding(new Insets(2, 30, 2, 0));
        StackPane avatar = new StackPane();
        avatar.setMinSize(26, 26); avatar.setMaxSize(26, 26);
        avatar.setStyle("-fx-background-color: " + ThemeManager.chatBtnGradient() + "; -fx-background-radius: 13;");
        Label avIcon = new Label("\uD83E\uDD16");
        avIcon.setStyle("-fx-font-size: 10;");
        avatar.getChildren().add(avIcon);
        Label msgLbl = new Label(text);
        msgLbl.setWrapText(true); msgLbl.setMaxWidth(260);
        msgLbl.setStyle("-fx-background-color: " + ThemeManager.accentHover() + "; -fx-text-fill: " + ThemeManager.textPrimary() + "; " +
                "-fx-font-size: 11; -fx-padding: 10 14; -fx-background-radius: 4 14 14 14;");
        msgRow.getChildren().addAll(avatar, msgLbl);
        if (chatMessages != null) chatMessages.getChildren().add(msgRow);
    }

    private void addUserMessage(String text) {
        HBox msgRow = new HBox(8);
        msgRow.setAlignment(Pos.TOP_RIGHT);
        msgRow.setPadding(new Insets(2, 0, 2, 30));
        Label msgLbl = new Label(text);
        msgLbl.setWrapText(true); msgLbl.setMaxWidth(260);
        msgLbl.setStyle("-fx-background-color: " + ThemeManager.accentGradient() + "; -fx-text-fill: " + (ThemeManager.isDark() ? "#000" : "#fff") + "; " +
                "-fx-font-size: 11; -fx-padding: 10 14; -fx-background-radius: 14 4 14 14; -fx-font-weight: bold;");
        msgRow.getChildren().add(msgLbl);
        if (chatMessages != null) chatMessages.getChildren().add(msgRow);
    }

    private void sendChatMessage(String message) {
        addUserMessage(message);
        HBox typingRow = new HBox(8);
        typingRow.setAlignment(Pos.TOP_LEFT);
        StackPane tAvatar = new StackPane();
        tAvatar.setMinSize(26, 26); tAvatar.setMaxSize(26, 26);
        tAvatar.setStyle("-fx-background-color: " + ThemeManager.chatBtnGradient() + "; -fx-background-radius: 13;");
        Label tIcon = new Label("\uD83E\uDD16");
        tIcon.setStyle("-fx-font-size: 10;");
        tAvatar.getChildren().add(tIcon);
        Label typingLbl = new Label("\u2022\u2022\u2022 En train de reflechir...");
        typingLbl.setStyle("-fx-background-color: " + ThemeManager.accentHover() + "; -fx-text-fill: " + ThemeManager.textSecondary() + "; " +
                "-fx-font-size: 10; -fx-padding: 10 14; -fx-background-radius: 4 14 14 14; -fx-font-style: italic;");
        typingRow.getChildren().addAll(tAvatar, typingLbl);
        if (chatMessages != null) chatMessages.getChildren().add(typingRow);

        String contextMsg = message;
        if (reservations != null && !reservations.isEmpty()) {
            StringBuilder ctx = new StringBuilder(message);
            ctx.append("\n\n[Contexte reservations: ");
            ctx.append(reservations.size()).append(" reservations, ");
            double total = reservations.stream().mapToDouble(Reservation::getMontantTotal).sum();
            ctx.append(String.format("total depense %.2f DT", total));
            ctx.append("]");
            contextMsg = ctx.toString();
        }

        String finalMsg = contextMsg;
        new Thread(() -> {
            String response = chatbotService.chat(finalMsg);
            javafx.application.Platform.runLater(() -> {
                if (chatMessages != null) {
                    chatMessages.getChildren().remove(typingRow);
                    addBotMessage(response);
                }
            });
        }).start();
    }

    // ================================================================
    // FEATURE #11: In-App Calendar View
    // ================================================================
    private void exportToCalendar(Reservation r) {
        showInAppCalendar(r);
    }

    private void showInAppCalendar(Reservation r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Calendrier - Reservation");
        dialog.setHeaderText(null);
        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(520);
        content.setMinHeight(520);

        // Title
        Label title = new Label("\uD83D\uDCC5 Calendrier de Reservation");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");

        // Reservation info card
        HBox infoCard = new HBox(12);
        infoCard.setAlignment(Pos.CENTER_LEFT);
        infoCard.setStyle("-fx-background-color: rgba(255,215,0,0.06); -fx-padding: 12; -fx-background-radius: 10;");
        Label infoIcon = new Label(getServiceIcon(r.getTypeService()));
        infoIcon.setStyle("-fx-font-size: 20;");
        VBox infoText = new VBox(2);
        Label infoName = new Label(r.getNomEtablissement() != null ? r.getNomEtablissement() : "Reservation");
        infoName.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");
        Label infoCode = new Label("\uD83D\uDD11 " + (r.getCodeConfirmation() != null ? r.getCodeConfirmation() : "N/A"));
        infoCode.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 10; -fx-font-family: 'Consolas';");
        infoText.getChildren().addAll(infoName, infoCode);
        infoCard.getChildren().addAll(infoIcon, infoText);

        // Calendar month navigation
        java.time.LocalDateTime eventDate = r.getDatePaiement() != null ? r.getDatePaiement() : java.time.LocalDateTime.now();
        final java.time.YearMonth[] currentMonth = { java.time.YearMonth.from(eventDate) };

        HBox monthNav = new HBox(12);
        monthNav.setAlignment(Pos.CENTER);
        Button prevMonth = new Button("\u25C0");
        prevMonth.setStyle("-fx-background-color: rgba(255,215,0,0.12); -fx-text-fill: #FFD700; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");
        Label monthLabel = new Label("");
        monthLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        Button nextMonth = new Button("\u25B6");
        nextMonth.setStyle("-fx-background-color: rgba(255,215,0,0.12); -fx-text-fill: #FFD700; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");
        monthNav.getChildren().addAll(prevMonth, monthLabel, nextMonth);

        // Calendar grid container
        VBox calendarContainer = new VBox(4);

        // Day headers
        HBox dayHeaders = new HBox(0);
        dayHeaders.setAlignment(Pos.CENTER);
        String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (String d : days) {
            Label dayLbl = new Label(d);
            dayLbl.setPrefWidth(60);
            dayLbl.setAlignment(Pos.CENTER);
            dayLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 4;");
            dayHeaders.getChildren().add(dayLbl);
        }
        calendarContainer.getChildren().add(dayHeaders);

        // Grid placeholder
        GridPane calendarGrid = new GridPane();
        calendarGrid.setAlignment(Pos.CENTER);
        calendarGrid.setHgap(2);
        calendarGrid.setVgap(2);
        calendarContainer.getChildren().add(calendarGrid);

        // Event details panel below calendar
        VBox eventDetails = new VBox(6);
        eventDetails.setStyle("-fx-background-color: rgba(81,207,102,0.06); -fx-padding: 12; -fx-background-radius: 8;");
        eventDetails.setVisible(false);

        // Collect all reservation dates for highlighting
        java.util.Map<java.time.LocalDate, List<Reservation>> reservationDates = new java.util.HashMap<>();
        if (reservations != null) {
            for (Reservation res : reservations) {
                if (res.getDatePaiement() != null) {
                    java.time.LocalDate date = res.getDatePaiement().toLocalDate();
                    reservationDates.computeIfAbsent(date, k -> new java.util.ArrayList<>()).add(res);
                }
            }
        }

        // Build calendar function
        Runnable buildCalendar = () -> {
            calendarGrid.getChildren().clear();
            java.time.format.DateTimeFormatter monthFmt = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRENCH);
            monthLabel.setText(currentMonth[0].format(monthFmt).substring(0, 1).toUpperCase() + currentMonth[0].format(monthFmt).substring(1));

            java.time.LocalDate firstDay = currentMonth[0].atDay(1);
            int startDow = firstDay.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
            int daysInMonth = currentMonth[0].lengthOfMonth();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate eventDay = eventDate.toLocalDate();

            int row = 0;
            int col = startDow - 1;
            for (int day = 1; day <= daysInMonth; day++) {
                java.time.LocalDate date = currentMonth[0].atDay(day);
                Button dayBtn = new Button(String.valueOf(day));
                dayBtn.setPrefSize(58, 38);
                dayBtn.setMinSize(58, 38);

                boolean isEvent = date.equals(eventDay);
                boolean hasReservation = reservationDates.containsKey(date);
                boolean isToday = date.equals(today);

                String style = "-fx-background-radius: 8; -fx-font-size: 11; -fx-font-weight: bold; -fx-cursor: hand; ";
                if (isEvent) {
                    style += "-fx-background-color: linear-gradient(to bottom, #FFD700, #FF8C00); -fx-text-fill: black;";
                } else if (hasReservation) {
                    style += "-fx-background-color: rgba(81,207,102,0.25); -fx-text-fill: #51CF66;";
                } else if (isToday) {
                    style += "-fx-background-color: rgba(100,181,246,0.2); -fx-text-fill: #64B5F6;";
                } else {
                    style += "-fx-background-color: rgba(255,255,255,0.04); -fx-text-fill: #888;";
                }
                dayBtn.setStyle(style);

                final java.time.LocalDate clickDate = date;
                dayBtn.setOnAction(ev -> {
                    eventDetails.getChildren().clear();
                    if (reservationDates.containsKey(clickDate)) {
                        eventDetails.setVisible(true);
                        Label detailTitle = new Label("\uD83D\uDCC5 Reservations du " + clickDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        detailTitle.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 12; -fx-font-weight: bold;");
                        eventDetails.getChildren().add(detailTitle);
                        for (Reservation res : reservationDates.get(clickDate)) {
                            HBox detailRow = new HBox(8);
                            detailRow.setAlignment(Pos.CENTER_LEFT);
                            Label dot = new Label("\u25CF");
                            dot.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 8;");
                            Label detailLbl = new Label((res.getNomEtablissement() != null ? res.getNomEtablissement() : "Reservation") +
                                " | " + (res.getCodeConfirmation() != null ? res.getCodeConfirmation() : "") +
                                " | " + String.format("%.2f DT", res.getMontantTotal()));
                            detailLbl.setStyle("-fx-text-fill: #ccc; -fx-font-size: 10;");
                            detailRow.getChildren().addAll(dot, detailLbl);
                            eventDetails.getChildren().add(detailRow);
                        }
                    } else if (clickDate.equals(today)) {
                        eventDetails.setVisible(true);
                        Label todayLbl = new Label("\uD83D\uDCC6 Aujourd'hui - Aucune reservation");
                        todayLbl.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 11;");
                        eventDetails.getChildren().add(todayLbl);
                    } else {
                        eventDetails.setVisible(false);
                    }
                });

                calendarGrid.add(dayBtn, col, row);
                col++;
                if (col > 6) {
                    col = 0;
                    row++;
                }
            }
        };

        buildCalendar.run();

        prevMonth.setOnAction(e -> { currentMonth[0] = currentMonth[0].minusMonths(1); buildCalendar.run(); eventDetails.setVisible(false); });
        nextMonth.setOnAction(e -> { currentMonth[0] = currentMonth[0].plusMonths(1); buildCalendar.run(); eventDetails.setVisible(false); });

        // Legend
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
            buildLegendItem("#FFD700", "Cette reservation"),
            buildLegendItem("#51CF66", "Autres reservations"),
            buildLegendItem("#64B5F6", "Aujourd'hui")
        );

        content.getChildren().addAll(title, infoCard, monthNav, calendarContainer, eventDetails, legend);
        dp.setContent(content);
        dp.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));

        dialog.showAndWait();
    }

    private HBox buildLegendItem(String color, String text) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);
        Region dot = new Region();
        dot.setPrefSize(10, 10);
        dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 9;");
        item.getChildren().addAll(dot, lbl);
        return item;
    }

    // ================================================================
    // FEATURE #6: AI-Enhanced Review Generator
    // ================================================================
    private void showAIReviewGenerator(Reservation r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("AI Review Generator");
        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(500);

        Label title = new Label("\uD83E\uDD16 Generateur d'Avis Intelligent");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");

        Label subtitle = new Label("Notre IA va generer un avis base sur votre experience");
        subtitle.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");

        // Tone selector
        HBox toneRow = new HBox(8);
        toneRow.setAlignment(Pos.CENTER);
        Label toneLbl = new Label("Ton:");
        toneLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");
        ComboBox<String> toneCombo = new ComboBox<>();
        toneCombo.getItems().addAll("Positif \uD83D\uDE0A", "Neutre \uD83D\uDE10", "Critique constructive \uD83E\uDD14", "Enthousiaste \uD83E\uDD29");
        toneCombo.setValue("Positif \uD83D\uDE0A");
        toneCombo.setStyle("-fx-background-color: rgba(255,255,255,0.08);");
        toneRow.getChildren().addAll(toneLbl, toneCombo);

        TextArea reviewArea = new TextArea();
        reviewArea.setPrefRowCount(6);
        reviewArea.setWrapText(true);
        reviewArea.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-font-size: 12; -fx-background-radius: 10;");
        reviewArea.setPromptText("L'avis genere apparaitra ici...");

        Button generateBtn = new Button("\u2728 Generer un Avis");
        generateBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8;");

        generateBtn.setOnAction(e -> {
            reviewArea.setText("\u23F3 Generation en cours...");
            String tone = toneCombo.getValue().split(" ")[0];
            String ctx = "Genere un avis " + tone + " en francais pour: " + (r.getNomEtablissement() != null ? r.getNomEtablissement() : "service")
                    + " (" + r.getTypeService() + "), " + r.getNbPersonnes() + " personnes, " + String.format("%.2f DT", r.getMontantTotal())
                    + ". L'avis doit etre realiste, 3-5 phrases, avec des details specifiques.";
            new Thread(() -> {
                String response = chatbotService.chat(ctx);
                javafx.application.Platform.runLater(() -> reviewArea.setText(response));
            }).start();
        });

        Button copyBtn = new Button("\uD83D\uDCCB Copier");
        copyBtn.setStyle("-fx-background-color: rgba(100,181,246,0.12); -fx-text-fill: #64B5F6; -fx-padding: 8 16; -fx-background-radius: 8;");
        copyBtn.setOnAction(e -> {
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(
                java.util.Collections.singletonMap(javafx.scene.input.DataFormat.PLAIN_TEXT, reviewArea.getText()));
            showMessage("\uD83D\uDCCB Avis copie!", true);
        });

        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER);
        actionRow.getChildren().addAll(generateBtn, copyBtn);

        content.getChildren().addAll(title, subtitle, toneRow, reviewArea, actionRow);
        dp.setContent(content);
        dp.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.showAndWait();
    }

    // ================================================================
    // FEATURE #18: Re-book / Book Again
    // ================================================================
    private void rebookReservation(Reservation r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Re-book");
        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(450);

        Label title = new Label("\uD83D\uDD01 Re-book: " + (r.getNomEtablissement() != null ? r.getNomEtablissement() : "Reservation"));
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");

        Label desc = new Label("Reservez a nouveau avec les memes parametres ou modifiez les dates.");
        desc.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        desc.setWrapText(true);

        // Info from original booking
        VBox infoBox = new VBox(6);
        infoBox.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-padding: 12; -fx-background-radius: 8;");
        Label svcLbl = new Label(getServiceIcon(r.getTypeService()) + " " + r.getTypeService());
        svcLbl.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
        Label pplLbl = new Label("\uD83D\uDC65 " + r.getNbPersonnes() + " personnes");
        pplLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");
        Label priceLbl = new Label("\uD83D\uDCB0 " + String.format("%.2f DT", r.getMontantTotal()));
        priceLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12;");
        infoBox.getChildren().addAll(svcLbl, pplLbl, priceLbl);

        content.getChildren().addAll(title, desc, infoBox);
        dp.setContent(content);

        ButtonType rebookType = new ButtonType("\uD83D\uDD01 Re-book maintenant", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(rebookType, cancelType);

        Button rebookBtn = (Button) dp.lookupButton(rebookType);
        rebookBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == rebookType) {
            // Navigate back to main interface where user can book again
            handleBackToMain();
            showMessage("\uD83D\uDD01 Redirige vers les reservations. Cherchez: " + (r.getNomEtablissement() != null ? r.getNomEtablissement() : r.getTypeService()), true);
        }
    }

    // ================================================================
    // FEATURE #4: AI Spending Insights
    // ================================================================
    private void showAISpendingInsights() {
        if (reservations == null || reservations.isEmpty()) {
            showMessage("\u2139 Aucune reservation pour analyser.", false);
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("AI Spending Insights");
        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(550);

        Label title = new Label("\uD83D\uDCCA AI Spending Insights");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");

        // Quick stats
        double totalSpent = reservations.stream().mapToDouble(Reservation::getMontantTotal).sum();
        double avgSpent = totalSpent / reservations.size();
        long cardPayments = reservations.stream().filter(rv -> "Konnect".equalsIgnoreCase(rv.getModePaiement())).count();

        HBox statsRow = new HBox(12);
        statsRow.setAlignment(Pos.CENTER);
        statsRow.getChildren().addAll(
            buildMiniStat("\uD83D\uDCB0", String.format("%.0f DT", totalSpent), "Total"),
            buildMiniStat("\uD83D\uDCCA", String.format("%.0f DT", avgSpent), "Moyenne"),
            buildMiniStat("\uD83D\uDCB3", cardPayments + "/" + reservations.size(), "Par carte"),
            buildMiniStat("\u2B50", String.valueOf(reservations.stream().filter(rv -> rv.getRating() > 0).count()), "Evalues")
        );

        Label resultLabel = new Label("\u23F3 Analyse IA en cours...");
        resultLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");
        resultLabel.setWrapText(true);

        ScrollPane scroll = new ScrollPane(resultLabel);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);

        content.getChildren().addAll(title, statsRow, scroll);
        dp.setContent(content);
        dp.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));

        StringBuilder ctx = new StringBuilder();
        ctx.append("Analyse mes depenses de voyage en Tunisie:\n");
        for (Reservation rv : reservations) {
            ctx.append("- ").append(rv.getNomEtablissement() != null ? rv.getNomEtablissement() : rv.getTypeService());
            ctx.append(": ").append(String.format("%.2f DT", rv.getMontantTotal()));
            ctx.append(" (").append(rv.getModePaiement()).append(")");
            if (rv.getRating() > 0) ctx.append(" Note: ").append(rv.getRating()).append("/5");
            ctx.append("\n");
        }
        ctx.append("Total: ").append(String.format("%.2f DT", totalSpent)).append("\n");
        ctx.append("Donne des insights detailles: tendances de depenses, recommandations d'economies, score de voyage, et suggestions pour le prochain voyage. Reponds en francais.");

        new Thread(() -> {
            String response = chatbotService.chat(ctx.toString());
            javafx.application.Platform.runLater(() -> {
                resultLabel.setText(response);
                resultLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
            });
        }).start();

        dialog.showAndWait();
    }

    private VBox buildMiniStat(String icon, String value, String label) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-padding: 12; -fx-background-radius: 10;");
        box.setMinWidth(90);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 16;");
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 9;");
        box.getChildren().addAll(iconLbl, valueLbl, labelLbl);
        return box;
    }

    // ================================================================
    // FEATURE #17: Reservation Analytics Dashboard (in-card mini display)
    // ================================================================
    private void showAnalyticsDashboard() {
        if (reservations == null || reservations.isEmpty()) {
            showMessage("\u2139 Aucune reservation pour les analytics.", false);
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Analytics Dashboard");
        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(550);

        Label title = new Label("\uD83D\uDCCA Analytics Dashboard");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");

        // Build simple bar chart using labels/regions
        double totalSpent = reservations.stream().mapToDouble(Reservation::getMontantTotal).sum();
        double maxAmount = reservations.stream().mapToDouble(Reservation::getMontantTotal).max().orElse(1);

        VBox chartBox = new VBox(4);
        chartBox.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 14; -fx-background-radius: 10;");
        Label chartTitle = new Label("\uD83D\uDCB0 Depenses par reservation");
        chartTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");
        chartBox.getChildren().add(chartTitle);

        for (Reservation rv : reservations) {
            HBox bar = new HBox(8);
            bar.setAlignment(Pos.CENTER_LEFT);
            String barName = rv.getNomEtablissement() != null ? rv.getNomEtablissement() : "N/A";
            if (barName.length() > 15) barName = barName.substring(0, 15) + "..";
            Label nameLbl = new Label(barName);
            nameLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 9;");
            nameLbl.setMinWidth(100);

            double pct = maxAmount > 0 ? rv.getMontantTotal() / maxAmount : 0;
            Region barFill = new Region();
            barFill.setPrefHeight(14);
            barFill.setMinWidth(10);
            barFill.setPrefWidth(pct * 250);
            barFill.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-background-radius: 4;");

            Label amtLbl = new Label(String.format("%.0f DT", rv.getMontantTotal()));
            amtLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 9; -fx-font-weight: bold;");

            bar.getChildren().addAll(nameLbl, barFill, amtLbl);
            chartBox.getChildren().add(bar);
        }

        // Payment method breakdown
        VBox methodBox = new VBox(6);
        methodBox.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 14; -fx-background-radius: 10;");
        Label methodTitle = new Label("\uD83D\uDCB3 Methodes de paiement");
        methodTitle.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 12; -fx-font-weight: bold;");
        methodBox.getChildren().add(methodTitle);

        java.util.Map<String, Long> methodCounts = reservations.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    rv -> rv.getModePaiement() != null ? rv.getModePaiement() : "Inconnu",
                    java.util.stream.Collectors.counting()));
        for (java.util.Map.Entry<String, Long> entry : methodCounts.entrySet()) {
            HBox methodRow = new HBox(8);
            methodRow.setAlignment(Pos.CENTER_LEFT);
            Label methodNameLbl = new Label((entry.getKey().contains("Carte") ? "\uD83D\uDCB3" : "\uD83D\uDCB5") + " " + entry.getKey());
            methodNameLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");
            Region methodSpacer = new Region();
            HBox.setHgrow(methodSpacer, Priority.ALWAYS);
            Label countLbl = new Label(entry.getValue() + " reservation(s)");
            countLbl.setStyle("-fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold;");
            methodRow.getChildren().addAll(methodNameLbl, methodSpacer, countLbl);
            methodBox.getChildren().add(methodRow);
        }

        // Total
        HBox totalRow = new HBox();
        totalRow.setStyle("-fx-background-color: rgba(255,215,0,0.06); -fx-padding: 12; -fx-background-radius: 8;");
        totalRow.setAlignment(Pos.CENTER);
        Label totalLbl = new Label("\uD83D\uDCB0 Total depense: " + String.format("%.2f DT", totalSpent) + " | " + reservations.size() + " reservation(s)");
        totalLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14; -fx-font-weight: bold;");
        totalRow.getChildren().add(totalLbl);

        content.getChildren().addAll(title, chartBox, methodBox, totalRow);
        dp.setContent(content);
        dp.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.showAndWait();
    }

    // ================================================================
    // AI & ANALYTICS BUTTONS IN HEADER
    // ================================================================
    private void addFeatureButtons() {
        if (titleBar == null) return;

        // AI Spending Insights
        Button insightsBtn = new Button("\uD83D\uDCCA");
        insightsBtn.setStyle("-fx-background-color: rgba(178,102,255,0.12); -fx-text-fill: #B266FF; -fx-font-size: 14; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;");
        insightsBtn.setOnAction(e -> showAISpendingInsights());
        insightsBtn.setTooltip(new Tooltip("AI Spending Insights"));

        // Analytics Dashboard
        Button analyticsBtn = new Button("\uD83D\uDCC8");
        analyticsBtn.setStyle("-fx-background-color: rgba(81,207,102,0.12); -fx-text-fill: #51CF66; -fx-font-size: 14; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;");
        analyticsBtn.setOnAction(e -> showAnalyticsDashboard());
        analyticsBtn.setTooltip(new Tooltip("Analytics Dashboard"));

        int insertIndex = Math.max(0, titleBar.getChildren().size() - 3);
        titleBar.getChildren().addAll(insertIndex, java.util.Arrays.asList(insightsBtn, analyticsBtn));
    }
}