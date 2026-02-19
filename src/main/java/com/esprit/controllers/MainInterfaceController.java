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

import com.esprit.entities.Lieu;
import com.esprit.entities.Etablissement;
import com.esprit.entities.Panier;
import com.esprit.entities.utilisateur;
import com.esprit.services.LieuService;
import com.esprit.services.EtablissementService;
import com.esprit.services.PanierService;
import com.esprit.services.ReservationService;
import com.esprit.services.utilisateurServices;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class MainInterfaceController {

    @FXML private HBox titleBar;
    @FXML private Label userNameLabel;
    @FXML private Label welcomeLabel;
    @FXML private StackPane panierBadge;
    @FXML private Label panierCountLabel;

    // Accueil stats
    @FXML private Label statLieuxLabel;
    @FXML private Label statEtabLabel;
    @FXML private Label statPanierLabel;
    @FXML private Label statPaiementsLabel;

    // Panels
    @FXML private ScrollPane panelAccueil;
    @FXML private ScrollPane panelLieux;
    @FXML private ScrollPane panelRestaurants;
    @FXML private ScrollPane panelProfil;

    // Carousels
    @FXML private HBox lieuxAccueilCarousel;
    @FXML private HBox etabAccueilCarousel;
    @FXML private HBox lieuxCarousel;
    @FXML private HBox etabCarousel;

    @FXML private Label lieuxPageLabel;
    @FXML private Label etabPageLabel;

    // Profile fields
    @FXML private Label profilInitials;
    @FXML private Label profilFullName;
    @FXML private Label profilEmail;
    @FXML private Label profilStatut;
    @FXML private Label profilDate;
    @FXML private TextField editNomField;
    @FXML private TextField editPrenomField;
    @FXML private TextField editEmailField;
    @FXML private TextField editTelField;
    @FXML private Label profilMessage;

    // Sidebar buttons
    @FXML private Button navAccueil;
    @FXML private Button navLieux;
    @FXML private Button navRestaurants;
    @FXML private Button navPaiements;
    @FXML private Button navProfil;

    private double xOffset = 0;
    private double yOffset = 0;
    private utilisateur currentUser;

    private final LieuService lieuService = new LieuService();
    private final EtablissementService etabService = new EtablissementService();
    private final PanierService panierService = new PanierService();
    private final ReservationService reservationService = new ReservationService();
    private final utilisateurServices userService = new utilisateurServices();

    private List<Lieu> lieux;
    private List<Etablissement> etablissements;

    private int lieuxPage = 0;
    private int etabPage = 0;
    private int lieuxAccueilPage = 0;
    private int etabAccueilPage = 0;
    private static final int CARDS_PER_PAGE = 3;

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
            if (welcomeLabel != null) welcomeLabel.setText("Bienvenue " + user.getPrenom() + " !");
            loadData();
            loadProfileData();
        }
    }

    private void loadData() {
        lieux = lieuService.getAll();
        etablissements = etabService.getAll();
        updateStats();
        updatePanierBadge();
        renderLieuxAccueil();
        renderEtabAccueil();
        renderLieux();
        renderEtab();
        handleShowAccueil();
    }

    private void updateStats() {
        if (statLieuxLabel != null) statLieuxLabel.setText(lieux.size() + " a explorer");
        if (statEtabLabel != null) statEtabLabel.setText(etablissements.size() + " disponibles");
        if (currentUser != null) {
            int panierCount = panierService.countByClient(currentUser.getId());
            int resCount = reservationService.countByClient(currentUser.getId());
            if (statPanierLabel != null) statPanierLabel.setText(panierCount + " articles");
            if (statPaiementsLabel != null) statPaiementsLabel.setText(resCount + " reservations");
        }
    }

    private void updatePanierBadge() {
        if (currentUser != null && panierBadge != null && panierCountLabel != null) {
            int count = panierService.countByClient(currentUser.getId());
            panierBadge.setVisible(count > 0);
            panierCountLabel.setText(String.valueOf(count));
        }
    }

    // ================================================================
    // PANEL SWITCHING
    // ================================================================
    private void showPanel(ScrollPane target) {
        if (panelAccueil != null) panelAccueil.setVisible(false);
        if (panelLieux != null) panelLieux.setVisible(false);
        if (panelRestaurants != null) panelRestaurants.setVisible(false);
        if (panelProfil != null) panelProfil.setVisible(false);
        if (target != null) target.setVisible(true);
        highlightNav(null);
    }

    private void highlightNav(Button active) {
        Button[] allNav = { navAccueil, navLieux, navRestaurants, navPaiements, navProfil };
        for (Button btn : allNav) {
            if (btn != null) btn.setStyle(btn == active
                ? "-fx-background-color: rgba(255,215,0,0.1); -fx-text-fill: #FFD700; -fx-font-size: 12; -fx-padding: 10 14; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;"
                : "");
        }
    }

    @FXML public void handleShowAccueil() { showPanel(panelAccueil); highlightNav(navAccueil); }
    @FXML public void handleShowLieux() { showPanel(panelLieux); highlightNav(navLieux); }
    @FXML public void handleShowRestaurants() { showPanel(panelRestaurants); highlightNav(navRestaurants); }
    @FXML public void handleShowProfil() { showPanel(panelProfil); highlightNav(navProfil); loadProfileData(); }

    // ================================================================
    // RENDER LIEU CARDS (Destinations)
    // ================================================================
    private void renderLieuxAccueil() {
        if (lieuxAccueilCarousel == null) return;
        lieuxAccueilCarousel.getChildren().clear();
        int start = lieuxAccueilPage * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, lieux.size());
        for (int i = start; i < end; i++) {
            VBox card = buildLieuCard(lieux.get(i), i);
            HBox.setHgrow(card, Priority.ALWAYS);
            lieuxAccueilCarousel.getChildren().add(card);
        }
    }

    private void renderLieux() {
        if (lieuxCarousel == null) return;
        lieuxCarousel.getChildren().clear();
        int start = lieuxPage * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, lieux.size());
        for (int i = start; i < end; i++) {
            VBox card = buildLieuCard(lieux.get(i), i);
            HBox.setHgrow(card, Priority.ALWAYS);
            lieuxCarousel.getChildren().add(card);
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) lieux.size() / CARDS_PER_PAGE));
        if (lieuxPageLabel != null) lieuxPageLabel.setText((lieuxPage + 1) + " / " + totalPages);
    }

    private VBox buildLieuCard(Lieu lieu, int index) {
        VBox card = new VBox(10);
        card.setMinWidth(300);
        card.setMaxWidth(420);
        String bg = CARD_COLORS[index % CARD_COLORS.length];
        card.setStyle("-fx-background-color: rgba(15,15,15,0.95); -fx-padding: 18; -fx-background-radius: 14; " +
            "-fx-border-color: " + bg.replace("0.08", "0.12") + "; -fx-border-radius: 14; -fx-border-width: 1;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(48, 48);
        iconPane.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12;");
        String catIcon = getCategoryIcon(lieu.getNomCategorie());
        Label iconLbl = new Label(catIcon);
        iconLbl.setStyle("-fx-font-size: 20;");
        iconPane.getChildren().add(iconLbl);

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label nameLabel = new Label(lieu.getNom());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        nameLabel.setMaxWidth(200);
        Label villeLabel = new Label("\uD83D\uDCCD " + (lieu.getVille() != null ? lieu.getVille() : ""));
        villeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        nameBox.getChildren().addAll(nameLabel, villeLabel);

        Label prixLabel = new Label(String.format("%.2f DT", lieu.getPrix()));
        prixLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");
        header.getChildren().addAll(iconPane, nameBox, prixLabel);

        // Category badge
        HBox catBadge = new HBox(6);
        catBadge.setAlignment(Pos.CENTER_LEFT);
        if (lieu.getNomCategorie() != null && !lieu.getNomCategorie().isEmpty()) {
            Label catLabel = new Label(catIcon + " " + lieu.getNomCategorie());
            catLabel.setStyle("-fx-background-color: rgba(100,181,246,0.08); -fx-text-fill: #64B5F6; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold;");
            catBadge.getChildren().add(catLabel);
        }

        // Description
        Label descLabel = new Label(lieu.getDescription() != null ? truncate(lieu.getDescription(), 80) : "");
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");
        descLabel.setWrapText(true);

        // Book button
        Button bookBtn = new Button("\uD83D\uDED2 Reserver");
        bookBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; -fx-font-size: 12; -fx-cursor: hand;");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setOnAction(e -> showBookingDialog(lieu.getNom(), lieu.getPrix(), lieu.getIdLieu(), getLieuServiceType(lieu), true));

        card.getChildren().addAll(header, catBadge, descLabel, bookBtn);
        return card;
    }

    // ================================================================
    // RENDER ETABLISSEMENT CARDS
    // ================================================================
    private void renderEtabAccueil() {
        if (etabAccueilCarousel == null) return;
        etabAccueilCarousel.getChildren().clear();
        int start = etabAccueilPage * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, etablissements.size());
        for (int i = start; i < end; i++) {
            VBox card = buildEtabCard(etablissements.get(i), i);
            HBox.setHgrow(card, Priority.ALWAYS);
            etabAccueilCarousel.getChildren().add(card);
        }
    }

    private void renderEtab() {
        if (etabCarousel == null) return;
        etabCarousel.getChildren().clear();
        int start = etabPage * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, etablissements.size());
        for (int i = start; i < end; i++) {
            VBox card = buildEtabCard(etablissements.get(i), i);
            HBox.setHgrow(card, Priority.ALWAYS);
            etabCarousel.getChildren().add(card);
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) etablissements.size() / CARDS_PER_PAGE));
        if (etabPageLabel != null) etabPageLabel.setText((etabPage + 1) + " / " + totalPages);
    }

    private VBox buildEtabCard(Etablissement etab, int index) {
        VBox card = new VBox(10);
        card.setMinWidth(300);
        card.setMaxWidth(420);
        String bg = CARD_COLORS[(index + 2) % CARD_COLORS.length];
        card.setStyle("-fx-background-color: rgba(15,15,15,0.95); -fx-padding: 18; -fx-background-radius: 14; " +
            "-fx-border-color: " + bg.replace("0.08", "0.12") + "; -fx-border-radius: 14; -fx-border-width: 1;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(48, 48);
        iconPane.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12;");
        String svcIcon = getEtabTypeIcon(etab);
        Label iconLbl = new Label(svcIcon);
        iconLbl.setStyle("-fx-font-size: 20;");
        iconPane.getChildren().add(iconLbl);

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label nameLabel = new Label(etab.getNom());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        nameLabel.setMaxWidth(200);
        Label villeLabel = new Label("\uD83D\uDCCD " + (etab.getVille() != null ? etab.getVille() : ""));
        villeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        nameBox.getChildren().addAll(nameLabel, villeLabel);

        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label gammeLbl = new Label(etab.getGammePrix() != null ? etab.getGammePrix() : "");
        gammeLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 13; -fx-font-weight: bold;");
        priceBox.getChildren().add(gammeLbl);
        header.getChildren().addAll(iconPane, nameBox, priceBox);

        // Info chips row
        HBox infoRow = new HBox(8);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        if (etab.getHoraires() != null && !etab.getHoraires().isEmpty()) {
            Label horaireChip = new Label("\u23F0 " + etab.getHoraires());
            horaireChip.setStyle("-fx-background-color: rgba(81,207,102,0.08); -fx-text-fill: #51CF66; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 9;");
            infoRow.getChildren().add(horaireChip);
        }
        if (etab.getTelephone() != null && !etab.getTelephone().isEmpty()) {
            Label telChip = new Label("\uD83D\uDCDE " + etab.getTelephone());
            telChip.setStyle("-fx-background-color: rgba(100,181,246,0.08); -fx-text-fill: #64B5F6; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 9;");
            infoRow.getChildren().add(telChip);
        }

        Label descLabel = new Label(etab.getDescription() != null ? truncate(etab.getDescription(), 80) : "");
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");
        descLabel.setWrapText(true);

        Button bookBtn = new Button("\uD83D\uDED2 Reserver");
        bookBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; -fx-font-size: 12; -fx-cursor: hand;");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setOnAction(e -> showBookingDialog(etab.getNom(), extractPrice(etab.getGammePrix()), etab.getIdEtablissement(), guessEtabType(etab), false));

        card.getChildren().addAll(header, infoRow, descLabel, bookBtn);
        return card;
    }

    // ================================================================
    // PROFESSIONAL BOOKING DIALOG
    // ================================================================
    private void showBookingDialog(String name, double basePrice, int etablissementId, String serviceType, boolean isLieu) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reserver - " + name);
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(18);
        content.setPadding(new Insets(28));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(520);

        // Header
        HBox headerRow = new HBox(14);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        StackPane headerIcon = new StackPane();
        headerIcon.setPrefSize(56, 56);
        headerIcon.setStyle("-fx-background-color: rgba(255,215,0,0.1); -fx-background-radius: 16;");
        String typeIcon = getTypeIcon(serviceType);
        Label hIcon = new Label(typeIcon);
        hIcon.setStyle("-fx-font-size: 24;");
        headerIcon.getChildren().add(hIcon);
        VBox headerText = new VBox(2);
        Label titleLbl = new Label("Reserver: " + name);
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");
        Label subLbl = new Label(serviceType + " | " + String.format("%.2f DT / unite", basePrice));
        subLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        headerText.getChildren().addAll(titleLbl, subLbl);
        headerRow.getChildren().addAll(headerIcon, headerText);

        // Dynamic form based on service type
        VBox formSection = new VBox(14);
        formSection.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 18; -fx-background-radius: 12;");
        Label formTitle = new Label("\uD83D\uDCCB Details de la reservation");
        formTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        formSection.getChildren().add(formTitle);

        boolean isHotel = serviceType.toLowerCase().contains("hotel") || serviceType.toLowerCase().contains("voyage") || serviceType.toLowerCase().contains("hebergement");
        boolean isRestaurant = serviceType.toLowerCase().contains("restaurant") || serviceType.toLowerCase().contains("caf") || serviceType.toLowerCase().contains("resto");

        // Date debut
        VBox dateDebutBox = new VBox(4);
        Label dateDebutLbl = new Label(isHotel ? "\uD83D\uDCC5 Date d'arrivee" : (isRestaurant ? "\uD83D\uDCC5 Date de reservation" : "\uD83D\uDCC5 Date de debut"));
        dateDebutLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        DatePicker dateDebut = new DatePicker(LocalDate.now().plusDays(1));
        dateDebut.setMaxWidth(Double.MAX_VALUE);
        dateDebut.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
        dateDebutBox.getChildren().addAll(dateDebutLbl, dateDebut);
        formSection.getChildren().add(dateDebutBox);

        // Time picker for restaurant/cafe
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 12);
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0, 15);
        if (isRestaurant) {
            VBox timeBox = new VBox(4);
            Label timeLbl = new Label("\u23F0 Heure d'arrivee");
            timeLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
            HBox timeRow = new HBox(8);
            timeRow.setAlignment(Pos.CENTER_LEFT);
            hourSpinner.setPrefWidth(80);
            hourSpinner.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
            Label sep = new Label(":");
            sep.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
            minuteSpinner.setPrefWidth(80);
            minuteSpinner.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
            timeRow.getChildren().addAll(hourSpinner, sep, minuteSpinner);
            timeBox.getChildren().addAll(timeLbl, timeRow);
            formSection.getChildren().add(timeBox);
        }

        // Duration / Date fin for hotels
        Spinner<Integer> daysSpinner = new Spinner<>(1, 30, 3);
        DatePicker dateFin = new DatePicker(LocalDate.now().plusDays(4));
        if (isHotel) {
            VBox daysBox = new VBox(4);
            Label daysLbl = new Label("\uD83C\uDFE8 Nombre de nuits");
            daysLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
            daysSpinner.setMaxWidth(Double.MAX_VALUE);
            daysSpinner.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
            daysBox.getChildren().addAll(daysLbl, daysSpinner);
            formSection.getChildren().add(daysBox);
        } else if (!isRestaurant) {
            // Generic: Date fin
            VBox dateFinBox = new VBox(4);
            Label dateFinLbl = new Label("\uD83D\uDCC5 Date de fin");
            dateFinLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
            dateFin.setMaxWidth(Double.MAX_VALUE);
            dateFin.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
            dateFinBox.getChildren().addAll(dateFinLbl, dateFin);
            formSection.getChildren().add(dateFinBox);
        }

        // Guests section
        VBox guestsSection = new VBox(10);
        guestsSection.setStyle("-fx-background-color: rgba(255,215,0,0.03); -fx-padding: 14; -fx-background-radius: 10; -fx-border-color: rgba(255,215,0,0.08); -fx-border-radius: 10; -fx-border-width: 1;");
        Label guestsTitle = new Label("\uD83D\uDC65 Voyageurs");
        guestsTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");
        guestsSection.getChildren().add(guestsTitle);

        Spinner<Integer> adultesSpinner = new Spinner<>(1, 20, 2);
        Spinner<Integer> enfantsSpinner = new Spinner<>(0, 15, 0);

        if (isHotel || isLieu) {
            // Hotel/Voyage: show adults + children separately
            HBox guestsRow = new HBox(16);
            guestsRow.setAlignment(Pos.CENTER_LEFT);
            VBox adultBox = new VBox(4);
            HBox.setHgrow(adultBox, Priority.ALWAYS);
            Label adultLbl = new Label("\uD83D\uDC64 Adultes");
            adultLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
            adultesSpinner.setMaxWidth(Double.MAX_VALUE);
            adultesSpinner.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
            adultBox.getChildren().addAll(adultLbl, adultesSpinner);

            VBox childBox = new VBox(4);
            HBox.setHgrow(childBox, Priority.ALWAYS);
            Label childLbl = new Label("\uD83D\uDC76 Enfants (demi-tarif)");
            childLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
            enfantsSpinner.setMaxWidth(Double.MAX_VALUE);
            enfantsSpinner.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
            childBox.getChildren().addAll(childLbl, enfantsSpinner);

            guestsRow.getChildren().addAll(adultBox, childBox);
            guestsSection.getChildren().add(guestsRow);
        } else {
            // Restaurant/cafe: just party size
            VBox partySizeBox = new VBox(4);
            Label partySizeLbl = new Label("\uD83D\uDC65 Nombre de personnes");
            partySizeLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
            adultesSpinner.setMaxWidth(Double.MAX_VALUE);
            adultesSpinner.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
            partySizeBox.getChildren().addAll(partySizeLbl, adultesSpinner);
            guestsSection.getChildren().add(partySizeBox);
        }

        formSection.getChildren().add(guestsSection);

        // Live price calculation
        VBox priceSection = new VBox(6);
        priceSection.setStyle("-fx-background-color: rgba(255,215,0,0.04); -fx-padding: 14; -fx-background-radius: 10;");
        Label priceTitleLbl = new Label("\uD83D\uDCB0 Estimation du prix");
        priceTitleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");
        Label priceBreakdown = new Label("");
        priceBreakdown.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        priceBreakdown.setWrapText(true);
        Label totalPriceLbl = new Label("0.00 DT");
        totalPriceLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 22; -fx-font-weight: bold;");
        priceSection.getChildren().addAll(priceTitleLbl, priceBreakdown, totalPriceLbl);
        
        // Hide price section for restaurants/cafes (no price input in booking form)
        if (isRestaurant) {
            priceSection.setManaged(false);
            priceSection.setVisible(false);
        }

        // Price update runnable
        Runnable updatePrice = () -> {
            int adults = adultesSpinner.getValue();
            int children = enfantsSpinner.getValue();
            double price;
            StringBuilder breakdown = new StringBuilder();

            if (isHotel) {
                int nights = daysSpinner.getValue();
                double adultCost = adults * basePrice * nights;
                double childCost = children * (basePrice * 0.5) * nights;
                price = adultCost + childCost;
                breakdown.append(adults + " adulte(s) x " + nights + " nuit(s) x " + String.format("%.2f", basePrice) + " = " + String.format("%.2f", adultCost));
                if (children > 0) {
                    breakdown.append("\n" + children + " enfant(s) x " + nights + " nuit(s) x " + String.format("%.2f", basePrice * 0.5) + " = " + String.format("%.2f", childCost));
                }
            } else if (isRestaurant) {
                price = adults * basePrice;
                breakdown.append(adults + " personne(s) x " + String.format("%.2f", basePrice) + " DT");
            } else if (isLieu) {
                double adultCost = adults * basePrice;
                double childCost = children * (basePrice * 0.5);
                price = adultCost + childCost;
                long dateDays = 1;
                if (dateDebut.getValue() != null && dateFin.getValue() != null) {
                    dateDays = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(dateDebut.getValue(), dateFin.getValue()));
                }
                price = price * dateDays;
                breakdown.append(adults + " adulte(s) x " + String.format("%.2f", basePrice) + " = " + String.format("%.2f", adultCost));
                if (children > 0) breakdown.append("\n" + children + " enfant(s) x " + String.format("%.2f", basePrice * 0.5) + " = " + String.format("%.2f", childCost));
                if (dateDays > 1) breakdown.append("\nx " + dateDays + " jour(s)");
            } else {
                price = (adults + children) * basePrice;
                breakdown.append((adults + children) + " personne(s) x " + String.format("%.2f", basePrice));
            }

            priceBreakdown.setText(breakdown.toString());
            totalPriceLbl.setText(String.format("%.2f DT", price));
        };

        // Listeners
        adultesSpinner.valueProperty().addListener((o, ov, nv) -> updatePrice.run());
        enfantsSpinner.valueProperty().addListener((o, ov, nv) -> updatePrice.run());
        daysSpinner.valueProperty().addListener((o, ov, nv) -> updatePrice.run());
        dateDebut.valueProperty().addListener((o, ov, nv) -> updatePrice.run());
        dateFin.valueProperty().addListener((o, ov, nv) -> updatePrice.run());
        updatePrice.run();

        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");

        content.getChildren().addAll(headerRow, formSection, priceSection, errorLbl);
        dp.setContent(content);

        ButtonType addType = new ButtonType("\uD83D\uDED2 Ajouter au Panier", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(addType, cancelType);

        Button addBtn = (Button) dp.lookupButton(addType);
        addBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 28; -fx-background-radius: 8; -fx-font-size: 13;");

        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (currentUser == null) {
                errorLbl.setText("\u274C Vous devez etre connecte pour reserver.");
                event.consume(); return;
            }
            if (dateDebut.getValue() == null) {
                errorLbl.setText("\u274C Veuillez choisir une date.");
                event.consume(); return;
            }

            int adults = adultesSpinner.getValue();
            int children = enfantsSpinner.getValue();
            int totalPeople = adults + children;

            LocalDateTime start;
            LocalDateTime end;

            if (isRestaurant) {
                int hour = hourSpinner.getValue();
                int minute = minuteSpinner.getValue();
                start = dateDebut.getValue().atTime(LocalTime.of(hour, minute));
                end = start.plusHours(2); // 2 hours default dining
            } else if (isHotel) {
                start = dateDebut.getValue().atTime(14, 0); // check-in 14h
                end = dateDebut.getValue().plusDays(daysSpinner.getValue()).atTime(11, 0); // check-out 11h
            } else if (isLieu) {
                start = dateDebut.getValue().atStartOfDay();
                end = dateFin.getValue() != null ? dateFin.getValue().atTime(23, 59) : start.plusDays(1);
            } else {
                start = dateDebut.getValue().atStartOfDay();
                end = dateFin.getValue() != null ? dateFin.getValue().atTime(23, 59) : start.plusDays(1);
            }

            if (!end.isAfter(start)) {
                errorLbl.setText("\u274C La date de fin doit etre apres la date de debut.");
                event.consume(); return;
            }

            double finalPrice;
            try {
                finalPrice = Double.parseDouble(totalPriceLbl.getText().replace(" DT", "").replace(",", "."));
            } catch (NumberFormatException ex) {
                finalPrice = basePrice * totalPeople;
            }

            Panier p = new Panier();
            p.setIdClient(currentUser.getId());
            p.setIdEtablissement(etablissementId);
            p.setTypeService(serviceType);
            p.setDateDebut(start);
            p.setDateFin(end);
            p.setNbPersonnes(totalPeople);
            p.setPrixEstime(finalPrice);
            p.setStatutItem("en_attente");
            p.setNbAdultes(adults);
            p.setNbEnfants(children);

            if (panierService.ajouter(p)) {
                updatePanierBadge();
                updateStats();
                showSuccessOverlay(name, finalPrice, totalPeople, serviceType);
            } else {
                errorLbl.setText("\u274C Echec de l'ajout au panier.");
                event.consume();
            }
        });

        dialog.setResultConverter(bt -> null);
        dialog.showAndWait();
    }

    // ================================================================
    // SUCCESS OVERLAY
    // ================================================================
    private void showSuccessOverlay(String name, double price, int people, String type) {
        StackPane root = (StackPane) panelAccueil.getParent();

        VBox overlay = new VBox(16);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.85);");
        overlay.setMaxWidth(Double.MAX_VALUE);
        overlay.setMaxHeight(Double.MAX_VALUE);

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(400);
        card.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 20; -fx-padding: 36; -fx-border-color: rgba(81,207,102,0.3); -fx-border-radius: 20; -fx-border-width: 2;");

        StackPane checkCircle = new StackPane();
        checkCircle.setPrefSize(70, 70);
        checkCircle.setStyle("-fx-background-color: rgba(81,207,102,0.15); -fx-background-radius: 35;");
        Label checkIcon = new Label("\u2705");
        checkIcon.setStyle("-fx-font-size: 32;");
        checkCircle.getChildren().add(checkIcon);

        Label successTitle = new Label("Ajoute au Panier !");
        successTitle.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 20; -fx-font-weight: bold;");

        Label successName = new Label(name);
        successName.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        HBox detailsRow = new HBox(20);
        detailsRow.setAlignment(Pos.CENTER);
        Label peopleLbl = new Label("\uD83D\uDC65 " + people + " pers.");
        peopleLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        Label typeLbl = new Label(getTypeIcon(type) + " " + type);
        typeLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        detailsRow.getChildren().addAll(peopleLbl, typeLbl);

        Label priceLbl = new Label(String.format("%.2f DT", price));
        priceLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 28; -fx-font-weight: bold;");

        Button goToPanierBtn = new Button("\uD83D\uDED2 Voir le Panier");
        goToPanierBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 28; -fx-background-radius: 8; -fx-font-size: 13; -fx-cursor: hand;");
        goToPanierBtn.setOnAction(e -> { root.getChildren().remove(overlay); handleGoToPanier(); });

        Button continueBtn = new Button("Continuer les achats");
        continueBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 6 16;");
        continueBtn.setOnAction(e -> root.getChildren().remove(overlay));

        card.getChildren().addAll(checkCircle, successTitle, successName, detailsRow, priceLbl, goToPanierBtn, continueBtn);
        overlay.getChildren().add(card);

        root.getChildren().add(overlay);

        // Auto-dismiss after 5s
        Timeline autoClose = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(400), overlay);
            ft.setToValue(0);
            ft.setOnFinished(ev -> root.getChildren().remove(overlay));
            ft.play();
        }));
        autoClose.play();

        // Entrance animation
        card.setOpacity(0); card.setScaleX(0.8); card.setScaleY(0.8);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), card);
        fadeIn.setToValue(1);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), card);
        scaleIn.setToX(1); scaleIn.setToY(1);
        new ParallelTransition(fadeIn, scaleIn).play();
    }

    // ================================================================
    // PROFILE
    // ================================================================
    private void loadProfileData() {
        if (currentUser == null) return;
        if (profilInitials != null) {
            String init = "";
            if (currentUser.getNom() != null && !currentUser.getNom().isEmpty()) init += currentUser.getNom().charAt(0);
            if (currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty()) init += currentUser.getPrenom().charAt(0);
            profilInitials.setText(init.toUpperCase());
        }
        if (profilFullName != null) profilFullName.setText(currentUser.getNom() + " " + currentUser.getPrenom());
        if (profilEmail != null) profilEmail.setText(currentUser.getEmail());
        if (profilStatut != null) profilStatut.setText("Statut: " + (currentUser.getStatut() != null ? currentUser.getStatut() : "ACTIF"));
        if (profilDate != null) profilDate.setText("Membre depuis: " + (currentUser.getDateCreation() != null ? currentUser.getDateCreation().toString() : "--"));
        if (editNomField != null) editNomField.setText(currentUser.getNom());
        if (editPrenomField != null) editPrenomField.setText(currentUser.getPrenom());
        if (editEmailField != null) editEmailField.setText(currentUser.getEmail());
        if (editTelField != null) editTelField.setText(String.valueOf(currentUser.getNumTel()));
    }

    @FXML
    public void handleSaveProfile() {
        if (currentUser == null) return;
        String nom = editNomField.getText().trim();
        String prenom = editPrenomField.getText().trim();
        String email = editEmailField.getText().trim();
        String tel = editTelField.getText().trim();
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
            if (profilMessage != null) { profilMessage.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;"); profilMessage.setText("Veuillez remplir tous les champs obligatoires."); }
            return;
        }
        int telNum = 0;
        try { telNum = Integer.parseInt(tel.replaceAll("\\D", "")); } catch (NumberFormatException ignored) {}
        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        currentUser.setEmail(email);
        currentUser.setNumTel(telNum);
        if (userService.updateProfile(currentUser.getId(), prenom, nom, email, telNum)) {
            if (profilMessage != null) { profilMessage.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11;"); profilMessage.setText("\u2705 Profil mis a jour avec succes !"); }
            if (userNameLabel != null) userNameLabel.setText(nom + " " + prenom);
            loadProfileData();
        } else {
            if (profilMessage != null) { profilMessage.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;"); profilMessage.setText("\u274C Echec de la mise a jour."); }
        }
    }

    // ================================================================
    // CAROUSEL NAVIGATION
    // ================================================================
    @FXML public void handleLieuxPrev() { if (lieuxPage > 0) { lieuxPage--; renderLieux(); } }
    @FXML public void handleLieuxNext() { if ((lieuxPage + 1) * CARDS_PER_PAGE < lieux.size()) { lieuxPage++; renderLieux(); } }
    @FXML public void handleEtabPrev() { if (etabPage > 0) { etabPage--; renderEtab(); } }
    @FXML public void handleEtabNext() { if ((etabPage + 1) * CARDS_PER_PAGE < etablissements.size()) { etabPage++; renderEtab(); } }
    @FXML public void handleLieuxAccueilPrev() { if (lieuxAccueilPage > 0) { lieuxAccueilPage--; renderLieuxAccueil(); } }
    @FXML public void handleLieuxAccueilNext() { if ((lieuxAccueilPage + 1) * CARDS_PER_PAGE < lieux.size()) { lieuxAccueilPage++; renderLieuxAccueil(); } }
    @FXML public void handleEtabAccueilPrev() { if (etabAccueilPage > 0) { etabAccueilPage--; renderEtabAccueil(); } }
    @FXML public void handleEtabAccueilNext() { if ((etabAccueilPage + 1) * CARDS_PER_PAGE < etablissements.size()) { etabAccueilPage++; renderEtabAccueil(); } }

    // ================================================================
    // NAVIGATION
    // ================================================================
    @FXML public void handleGoToPanier() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/panier.fxml"));
            Parent root = loader.load();
            if (currentUser != null) { PanierController c = loader.getController(); c.setUser(currentUser); }
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Mon Panier");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleGoToMesPaiements() {
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

    // ================================================================
    // HELPERS
    // ================================================================
    private String truncate(String text, int max) {
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private String getCategoryIcon(String category) {
        if (category == null) return "\uD83C\uDFDD";
        String lower = category.toLowerCase();
        if (lower.contains("plage") || lower.contains("mer")) return "\uD83C\uDFD6";
        if (lower.contains("montagne") || lower.contains("randonnee")) return "\u26F0";
        if (lower.contains("ville") || lower.contains("historique")) return "\uD83C\uDFDB";
        if (lower.contains("desert") || lower.contains("sahara")) return "\uD83C\uDFDC";
        if (lower.contains("parc") || lower.contains("nature")) return "\uD83C\uDF33";
        if (lower.contains("hotel") || lower.contains("hebergement")) return "\uD83C\uDFE8";
        return "\uD83C\uDFDD";
    }

    private String getTypeIcon(String type) {
        if (type == null) return "\uD83C\uDFAF";
        String lower = type.toLowerCase();
        if (lower.contains("hotel")) return "\uD83C\uDFE8";
        if (lower.contains("voyage") || lower.contains("destination")) return "\uD83C\uDFDD";
        if (lower.contains("rest")) return "\uD83C\uDF55";
        if (lower.contains("caf")) return "\u2615";
        return "\uD83C\uDFAF";
    }

    private String getEtabTypeIcon(Etablissement etab) {
        String nom = (etab.getNom() != null ? etab.getNom() : "").toLowerCase();
        String desc = (etab.getDescription() != null ? etab.getDescription() : "").toLowerCase();
        String combined = nom + " " + desc;
        if (combined.contains("hotel")) return "\uD83C\uDFE8";
        if (combined.contains("rest")) return "\uD83C\uDF55";
        if (combined.contains("caf")) return "\u2615";
        if (combined.contains("spa") || combined.contains("bien")) return "\uD83D\uDC86";
        if (combined.contains("sport") || combined.contains("gym")) return "\uD83C\uDFCB";
        return "\uD83C\uDFAF";
    }

    private String getLieuServiceType(Lieu lieu) {
        String cat = lieu.getNomCategorie() != null ? lieu.getNomCategorie().toLowerCase() : "";
        if (cat.contains("hotel") || cat.contains("hebergement")) return "Hotel";
        if (cat.contains("voyage") || cat.contains("destination")) return "Voyage";
        if (cat.contains("plage") || cat.contains("mer")) return "Voyage";
        return "Voyage";
    }

    private String guessEtabType(Etablissement etab) {
        String nom = (etab.getNom() != null ? etab.getNom() : "").toLowerCase();
        String desc = (etab.getDescription() != null ? etab.getDescription() : "").toLowerCase();
        String combined = nom + " " + desc;
        if (combined.contains("hotel")) return "Hotel";
        if (combined.contains("rest")) return "Restaurant";
        if (combined.contains("caf")) return "Cafe";
        if (combined.contains("spa")) return "Spa";
        return "Restaurant";
    }

    private double extractPrice(String gammePrix) {
        if (gammePrix == null || gammePrix.isEmpty()) return 50.0;
        try {
            String cleaned = gammePrix.replaceAll("[^0-9.,]", "").replace(",", ".");
            if (!cleaned.isEmpty()) {
                String[] parts = cleaned.split("\\.");
                if (parts.length > 2) cleaned = parts[0] + "." + parts[1];
                return Double.parseDouble(cleaned);
            }
        } catch (NumberFormatException ignored) {}
        return 50.0;
    }
}