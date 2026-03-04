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
import com.esprit.services.ChatbotService;
import com.esprit.utils.ThemeManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class MainInterfaceController {

    @FXML private HBox titleBar;
    @FXML private Button fullscreenBtn;
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
    @FXML private Node panelAdresses;
    @FXML private Node panelCategories;
    @FXML private Node panelLieuxTouristiques;

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
    @FXML private Button navTourisme;
    @FXML private Button navAdresses;
    @FXML private Button navCategories;
    @FXML private Button navLieuxTouristiques;

    // Chatbot
    @FXML private VBox chatBotPanel;
    @FXML private Button chatBotFab;

    private double xOffset = 0;
    private double yOffset = 0;
    private utilisateur currentUser;

    private final LieuService lieuService = new LieuService();
    private final EtablissementService etabService = new EtablissementService();
    private final PanierService panierService = new PanierService();
    private final ReservationService reservationService = new ReservationService();
    private final utilisateurServices userService = new utilisateurServices();
    private final ChatbotService chatbotService = new ChatbotService();

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
            addThemeToggleButton();
            addChatbotButton();

            // Apply theme to FXML nodes (fix inline dark styles)
            javafx.application.Platform.runLater(() -> {
                if (titleBar.getScene() != null && titleBar.getScene().getRoot() != null) {
                    ThemeManager.applyThemeToFXML((Parent) titleBar.getScene().getRoot());
                }
            });
            // Re-apply when theme changes
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
    private void showPanel(Node target) {
        Node[] allPanels = { panelAccueil, panelLieux, panelRestaurants, panelProfil, panelAdresses, panelCategories, panelLieuxTouristiques };
        for (Node p : allPanels) {
            if (p != null) p.setVisible(false);
        }
        if (target != null) target.setVisible(true);
        highlightNav(null);
    }

    private void highlightNav(Button active) {
        Button[] allNav = { navAccueil, navLieux, navRestaurants, navPaiements, navProfil, navTourisme, navAdresses, navCategories, navLieuxTouristiques };
        for (Button btn : allNav) {
            if (btn != null) btn.setStyle(btn == active
                ? "-fx-background-color: " + ThemeManager.accentHover() + "; -fx-text-fill: " + ThemeManager.accent() + "; -fx-font-size: 12; -fx-padding: 10 14; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;"
                : "");
        }
    }

    @FXML public void handleShowAccueil() { showPanel(panelAccueil); highlightNav(navAccueil); }
    @FXML public void handleShowLieux() { showPanel(panelLieux); highlightNav(navLieux); }
    @FXML public void handleShowRestaurants() { showPanel(panelRestaurants); highlightNav(navRestaurants); }
    @FXML public void handleShowProfil() { showPanel(panelProfil); highlightNav(navProfil); loadProfileData(); }
    @FXML public void handleShowAdresses() { showPanel(panelAdresses); highlightNav(navAdresses); }
    @FXML public void handleShowCategories() { showPanel(panelCategories); highlightNav(navCategories); }
    @FXML public void handleShowLieuxTouristiques() { showPanel(panelLieuxTouristiques); highlightNav(navLieuxTouristiques); }

    @FXML
    public void handleToggleChatBot() {
        if (chatBotPanel != null) {
            boolean show = !chatBotPanel.isVisible();
            chatBotPanel.setVisible(show);
            chatBotPanel.setManaged(show);
            if (chatBotFab != null) {
                chatBotFab.setText(show ? "\u2715" : "\uD83E\uDD16");
            }
        }
    }

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

        // Only show price for bookable/payable services (not restaurants/cafes - lieux are always voyage/hotel type)
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
        // Only show price for hotels/voyages - restaurants/cafes don't have advance pricing
        String etabType = guessEtabType(etab);
        boolean isPayableEtab = "Hotel".equalsIgnoreCase(etabType) || "Spa".equalsIgnoreCase(etabType);
        if (isPayableEtab && etab.getGammePrix() != null && !etab.getGammePrix().isEmpty()) {
            Label gammeLbl = new Label(etab.getGammePrix());
            gammeLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 13; -fx-font-weight: bold;");
            priceBox.getChildren().add(gammeLbl);
        } else if (!isPayableEtab) {
            Label freeLbl = new Label("Reservation gratuite");
            freeLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 10; -fx-font-weight: bold;");
            priceBox.getChildren().add(freeLbl);
        }
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
        boolean isHotelType = serviceType.toLowerCase().contains("hotel") || serviceType.toLowerCase().contains("voyage") || serviceType.toLowerCase().contains("hebergement");
        boolean isRestaurantType = serviceType.toLowerCase().contains("restaurant") || serviceType.toLowerCase().contains("caf") || serviceType.toLowerCase().contains("resto");
        Label subLbl;
        if (isRestaurantType) {
            subLbl = new Label(serviceType + " | Reservation gratuite - paiement sur place");
            subLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11;");
        } else {
            subLbl = new Label(serviceType + " | " + String.format("%.2f DT / unite", basePrice));
            subLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        }
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
        Label dateDebutLbl = new Label(isHotel ? "\uD83D\uDCC5 Date d'arrivee (Check-in 14h00)" : (isRestaurant ? "\uD83D\uDCC5 Date de reservation" : "\uD83D\uDCC5 Date de debut"));
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

        // ROOMS + Duration for hotels (REAL-WORLD BOOKING LOGIC)
        Spinner<Integer> roomsSpinner = new Spinner<>(1, 10, 1);
        Spinner<Integer> daysSpinner = new Spinner<>(1, 30, 3);
        DatePicker dateFin = new DatePicker(LocalDate.now().plusDays(4));
        
        if (isHotel) {
            // Number of rooms (like Booking.com)
            VBox roomsBox = new VBox(4);
            Label roomsLbl = new Label("\uD83D\uDECF Nombre de chambres");
            roomsLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
            roomsSpinner.setMaxWidth(Double.MAX_VALUE);
            roomsSpinner.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
            Label roomTip = new Label("\uD83D\uDCA1 Max 3 adultes ou 2 adultes + 2 enfants par chambre");
            roomTip.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 9;");
            roomsBox.getChildren().addAll(roomsLbl, roomsSpinner, roomTip);
            formSection.getChildren().add(roomsBox);

            // Number of nights
            VBox daysBox = new VBox(4);
            Label daysLbl = new Label("\uD83C\uDFE8 Nombre de nuits");
            daysLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
            daysSpinner.setMaxWidth(Double.MAX_VALUE);
            daysSpinner.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
            Label checkInfo = new Label("\u2139 Check-in: 14h00 | Check-out: 11h00");
            checkInfo.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 9;");
            daysBox.getChildren().addAll(daysLbl, daysSpinner, checkInfo);
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

        // Guests section with room-based validation
        VBox guestsSection = new VBox(10);
        guestsSection.setStyle("-fx-background-color: rgba(255,215,0,0.03); -fx-padding: 14; -fx-background-radius: 10; -fx-border-color: rgba(255,215,0,0.08); -fx-border-radius: 10; -fx-border-width: 1;");
        Label guestsTitle = new Label("\uD83D\uDC65 Voyageurs");
        guestsTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");
        guestsSection.getChildren().add(guestsTitle);

        Spinner<Integer> adultesSpinner = new Spinner<>(1, 20, 2);
        Spinner<Integer> enfantsSpinner = new Spinner<>(0, 15, 0);
        Label capacityWarning = new Label("");
        capacityWarning.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 9;");

        if (isHotel || isLieu) {
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
            guestsSection.getChildren().addAll(guestsRow, capacityWarning);

            // Real-world room capacity validation (like Booking.com)
            if (isHotel) {
                Runnable validateCapacity = () -> {
                    int rooms = roomsSpinner.getValue();
                    int adults = adultesSpinner.getValue();
                    int children = enfantsSpinner.getValue();
                    int maxAdults = rooms * 3;  // Max 3 adults per room
                    int maxTotal = rooms * 4;   // Max 4 people per room (2 adults + 2 children typical)
                    
                    if (adults > maxAdults) {
                        capacityWarning.setText("\u26A0 Maximum " + maxAdults + " adultes pour " + rooms + " chambre(s). Ajoutez des chambres!");
                        capacityWarning.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 9;");
                    } else if (adults + children > maxTotal) {
                        capacityWarning.setText("\u26A0 Maximum " + maxTotal + " personnes pour " + rooms + " chambre(s). Ajoutez des chambres!");
                        capacityWarning.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 9;");
                    } else {
                        int suggested = (int) Math.ceil((double) adults / 2);
                        if (children > 0) suggested = Math.max(suggested, (int) Math.ceil((double) (adults + children) / 4.0));
                        if (rooms < suggested) {
                            capacityWarning.setText("\uD83D\uDCA1 Nous recommandons " + suggested + " chambre(s) pour votre groupe");
                            capacityWarning.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 9;");
                        } else {
                            capacityWarning.setText("\u2705 " + rooms + " chambre(s) pour " + adults + " adulte(s)" + (children > 0 ? " + " + children + " enfant(s)" : ""));
                            capacityWarning.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 9;");
                        }
                    }
                };
                roomsSpinner.valueProperty().addListener((o, ov, nv) -> validateCapacity.run());
                adultesSpinner.valueProperty().addListener((o, ov, nv) -> validateCapacity.run());
                enfantsSpinner.valueProperty().addListener((o, ov, nv) -> validateCapacity.run());
                validateCapacity.run();
            }
        } else {
            // Restaurant/cafe: party size with table logic
            VBox partySizeBox = new VBox(4);
            Label partySizeLbl = new Label("\uD83D\uDC65 Nombre de personnes (max 12 par table)");
            partySizeLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
            adultesSpinner.setMaxWidth(Double.MAX_VALUE);
            Spinner<Integer> restoSpinner = new Spinner<>(1, 12, 2);
            restoSpinner.setMaxWidth(Double.MAX_VALUE);
            restoSpinner.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-font-size: 12;");
            // Copy value to adultesSpinner for later use
            restoSpinner.valueProperty().addListener((o, ov, nv) -> adultesSpinner.getValueFactory().setValue(nv));
            partySizeBox.getChildren().addAll(partySizeLbl, restoSpinner);
            guestsSection.getChildren().add(partySizeBox);
        }

        formSection.getChildren().add(guestsSection);

        // Live price calculation - ONLY for payable services (hotels, voyages, lieux)
        // Restaurants/cafes have no advance price - you pay on the spot
        VBox priceSection = new VBox(6);
        priceSection.setStyle("-fx-background-color: rgba(255,215,0,0.04); -fx-padding: 14; -fx-background-radius: 10;");
        Label priceTitleLbl;
        Label priceBreakdown = new Label("");
        priceBreakdown.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        priceBreakdown.setWrapText(true);
        Label totalPriceLbl = new Label("0.00 DT");
        totalPriceLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 22; -fx-font-weight: bold;");

        if (isRestaurant) {
            priceTitleLbl = new Label("\u2139 Information");
            priceTitleLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 12; -fx-font-weight: bold;");
            Label freeNote = new Label("Pas de paiement a l'avance pour les restaurants et cafes.\nVotre reservation sera confirmee automatiquement.\nVous payerez directement sur place.");
            freeNote.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
            freeNote.setWrapText(true);
            totalPriceLbl.setText("Gratuit");
            totalPriceLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 22; -fx-font-weight: bold;");
            priceSection.getChildren().addAll(priceTitleLbl, freeNote, totalPriceLbl);
        } else {
            priceTitleLbl = new Label("\uD83D\uDCB0 Estimation du prix");
            priceTitleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");
            priceSection.getChildren().addAll(priceTitleLbl, priceBreakdown, totalPriceLbl);
        }

        // Price update runnable - Real-world pricing like Booking.com
        Runnable updatePrice = () -> {
            if (isRestaurant) return; // No price calculation for restaurants/cafes
            int adults = adultesSpinner.getValue();
            int children = enfantsSpinner.getValue();
            double price;
            StringBuilder breakdown = new StringBuilder();

            if (isHotel) {
                int nights = daysSpinner.getValue();
                int rooms = roomsSpinner.getValue();
                // Real-world hotel pricing: price per room per night (like Booking.com)
                double roomCost = rooms * basePrice * nights;
                // Children under 12 stay free in most hotels (real-world logic)
                // But we add a small supplement for extra beds
                double childSupplement = children > 0 ? children * (basePrice * 0.15) * nights : 0;
                price = roomCost + childSupplement;
                breakdown.append("\uD83C\uDFE8 " + rooms + " chambre(s) x " + nights + " nuit(s) x " + String.format("%.2f", basePrice) + " DT = " + String.format("%.2f", roomCost) + " DT");
                breakdown.append("\n\uD83D\uDC64 " + adults + " adulte(s) inclus");
                if (children > 0) {
                    breakdown.append("\n\uD83D\uDC76 " + children + " enfant(s) - supplement lit: " + String.format("%.2f", childSupplement) + " DT");
                }
                breakdown.append("\n\u2139 Check-in: 14h00 | Check-out: 11h00");
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
        roomsSpinner.valueProperty().addListener((o, ov, nv) -> updatePrice.run());
        dateDebut.valueProperty().addListener((o, ov, nv) -> updatePrice.run());
        dateFin.valueProperty().addListener((o, ov, nv) -> updatePrice.run());
        updatePrice.run();

        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");

        content.getChildren().addAll(headerRow, formSection, priceSection, errorLbl);
        dp.setContent(content);

        ButtonType addType = new ButtonType(isRestaurant ? "\u2705 Confirmer la Reservation" : "\uD83D\uDED2 Ajouter au Panier", ButtonBar.ButtonData.OK_DONE);
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

            // Room capacity validation for hotels (real-world Booking.com logic)
            if (isHotel) {
                int rooms = roomsSpinner.getValue();
                int maxAdults = rooms * 3;
                int maxTotal = rooms * 4;
                if (adults > maxAdults) {
                    errorLbl.setText("\u274C Maximum " + maxAdults + " adultes pour " + rooms + " chambre(s). Ajoutez des chambres!");
                    event.consume(); return;
                }
                if (totalPeople > maxTotal) {
                    errorLbl.setText("\u274C Maximum " + maxTotal + " personnes pour " + rooms + " chambre(s). Ajoutez des chambres!");
                    event.consume(); return;
                }
            }

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
            if (isRestaurant) {
                finalPrice = 0.0; // Restaurants/cafes - no advance payment
            } else {
                try {
                    finalPrice = Double.parseDouble(totalPriceLbl.getText().replace(" DT", "").replace(",", "."));
                } catch (NumberFormatException ex) {
                    finalPrice = basePrice * totalPeople;
                }
            }

            Panier p = new Panier();
            p.setIdClient(currentUser.getId());
            p.setIdEtablissement(etablissementId);
            p.setTypeService(serviceType);
            p.setDateDebut(start);
            p.setDateFin(end);
            p.setNbPersonnes(totalPeople);
            p.setPrixEstime(finalPrice);
            p.setNbAdultes(adults);
            p.setNbEnfants(children);
            p.setNbChambres(isHotel ? roomsSpinner.getValue() : 1);

            if (isRestaurant) {
                // Restaurants/cafes: auto-confirm with "Especes" - no advance payment needed
                p.setStatutItem("confirme");
                if (panierService.ajouter(p)) {
                    // Also auto-create a reservation
                    com.esprit.entities.Reservation r = new com.esprit.entities.Reservation();
                    r.setIdPanier(p.getIdPanier());
                    r.setDatePaiement(LocalDateTime.now());
                    r.setMontantTotal(0.0);
                    r.setModePaiement("Especes");
                    r.setStatutPaiement("Confirme");
                    r.setTypeService(serviceType);
                    r.setNomEtablissement(name);
                    r.setNbPersonnes(totalPeople);
                    r.setIdClient(currentUser.getId());
                    new com.esprit.services.ReservationService().ajouter(r);
                    updatePanierBadge();
                    updateStats();
                    showSuccessOverlay(name, 0, totalPeople, serviceType);
                } else {
                    errorLbl.setText("\u274C Echec de la reservation.");
                    event.consume();
                }
            } else {
                // Hotels/Voyages/Lieux: add to panier as en_attente for payment
                p.setStatutItem("en_attente");
                if (panierService.ajouter(p)) {
                    updatePanierBadge();
                    updateStats();
                    showSuccessOverlay(name, finalPrice, totalPeople, serviceType);
                } else {
                    errorLbl.setText("\u274C Echec de l'ajout au panier.");
                    event.consume();
                }
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

        boolean isRestoType = type != null && (type.toLowerCase().contains("restaurant") || type.toLowerCase().contains("caf") || type.toLowerCase().contains("resto"));

        Label successTitle = new Label(isRestoType ? "Reservation Confirmee !" : "Ajoute au Panier !");
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

        card.getChildren().addAll(checkCircle, successTitle, successName, detailsRow);

        if (isRestoType) {
            // Restaurant: show "free reservation" message
            Label freeLbl = new Label("Reservation gratuite");
            freeLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 22; -fx-font-weight: bold;");
            Label infoLbl = new Label("Paiement sur place le jour de votre visite");
            infoLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
            card.getChildren().addAll(freeLbl, infoLbl);
        } else {
            // Hotel/Voyage/Lieu: show price
            Label priceLbl = new Label(String.format("%.2f DT", price));
            priceLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 28; -fx-font-weight: bold;");
            card.getChildren().add(priceLbl);
        }

        Button goToPanierBtn = new Button(isRestoType ? "\uD83D\uDCB3 Voir mes Reservations" : "\uD83D\uDED2 Voir le Panier");
        goToPanierBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 28; -fx-background-radius: 8; -fx-font-size: 13; -fx-cursor: hand;");
        goToPanierBtn.setOnAction(e -> {
            root.getChildren().remove(overlay);
            if (isRestoType) { handleGoToMesPaiements(); } else { handleGoToPanier(); }
        });

        Button continueBtn = new Button("Continuer les achats");
        continueBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 6 16;");
        continueBtn.setOnAction(e -> root.getChildren().remove(overlay));

        card.getChildren().addAll(goToPanierBtn, continueBtn);
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

    @FXML public void handleGoToTourisme() {
        System.out.println("\n[TOURISM] handleGoToTourisme() called (user)");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
            System.out.println("[TOURISM] Loading MainView.fxml...");
            Parent root = loader.load();
            System.out.println("[TOURISM] MainView.fxml loaded OK!");
            MainController mainCtrl = loader.getController();
            mainCtrl.setReturnTarget("user", currentUser);
            mainCtrl.setDashboardOnly(true);
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Dashboard Tourisme");
            System.out.println("[TOURISM] Scene transition started");
        } catch (Exception e) {
            System.err.println("[TOURISM] ❌ FAILED to load tourism module:");
            e.printStackTrace();
        }
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
            // Save theme preference to DB for current user
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

        addBotMessage("Bonjour! \uD83D\uDC4B Je suis Tabaani AI, votre assistant de voyage.\n\n" +
                "\u2022 Decouvrir des destinations\n" +
                "\u2022 Reserver des hotels et restaurants\n" +
                "\u2022 Obtenir des conseils de voyage\n" +
                "\u2022 Codes promo disponibles\n\n" +
                "Que souhaitez-vous explorer ?");

        HBox quickActions = new HBox(6);
        quickActions.setPadding(new Insets(8, 12, 4, 12));
        quickActions.setAlignment(Pos.CENTER);
        String[][] actions = {
                {"\uD83C\uDF0D Destinations", "Quelles sont les meilleures destinations en Tunisie ?"},
                {"\uD83C\uDFE8 Hotels", "Recommande-moi des hotels pour un sejour de luxe"},
                {"\uD83D\uDCA1 Conseils", "Donne-moi des astuces pour voyager en Tunisie"}
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

        new Thread(() -> {
            String response = chatbotService.chat(message);
            javafx.application.Platform.runLater(() -> {
                if (chatMessages != null) {
                    chatMessages.getChildren().remove(typingRow);
                    addBotMessage(response);
                }
            });
        }).start();
    }
}