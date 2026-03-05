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
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.concurrent.Worker;

import com.esprit.entities.Panier;
import com.esprit.entities.Reservation;
import com.esprit.entities.Etablissement;
import com.esprit.entities.utilisateur;
import com.esprit.services.PanierService;
import com.esprit.services.ReservationService;
import com.esprit.services.EtablissementService;
import com.esprit.services.EmailService;
import com.esprit.services.utilisateurServices;
import com.esprit.services.PaymeePaymentService;
import com.esprit.services.SmsService;
import com.esprit.services.ChatbotService;
import com.esprit.utils.ThemeManager;
import com.esprit.services.TranslationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class PanierController {

    @FXML private HBox titleBar;

    // Drawer
    @FXML private Button menuToggleBtn;
    @FXML private Region drawerOverlay;
    @FXML private VBox sidebarDrawer;
    @FXML private Label drawerInitials;
    @FXML private Label drawerUserName;
    @FXML private Label drawerUserRole;
    private boolean drawerOpen = false;

    // Translation support
    private final java.util.LinkedHashMap<Labeled, String> staticOriginals = new java.util.LinkedHashMap<>();
    private boolean staticLabelsCollected = false;
    @FXML private Button fullscreenBtn;
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
    private final EmailService emailService = new EmailService();
    private final utilisateurServices userService = new utilisateurServices();
    private final PaymeePaymentService paymeeService = new PaymeePaymentService();
    private final SmsService smsService = new SmsService();
    private final ChatbotService chatbotService = new ChatbotService();

    private List<Panier> panierItems;
    private int currentPage = 0;
    private static final int CARDS_PER_PAGE = 3;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Promo code system
    private static final String[] PROMO_CODES = {"SMART10", "TRAVEL20", "VIP15", "GOLD25", "WELCOME5"};
    private static final double[] PROMO_DISCOUNTS = {10, 20, 15, 25, 5};
    // Loyalty-based promo codes: 100pts=5%, 200pts=10%, 500pts=20%
    private static final int[] LOYALTY_THRESHOLDS = {100, 200, 500};
    private static final double[] LOYALTY_DISCOUNTS = {5, 10, 20};
    private String appliedPromo = null;
    private double discountPercent = 0;

    /**
     * Apply premium Konnect-style theming to any Dialog:
     *  - Undecorated (no system chrome)
     *  - Gradient top accent bar with title, icon and draggable close button
     *  - Rounded corners, drop shadow, consistent button styling
     */
    private void styleDialog(Dialog<?> dialog, String icon, String title, String accentColor) {
        DialogPane dp = dialog.getDialogPane();

        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        dp.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 16; -fx-border-radius: 16; "
                + "-fx-border-color: #333; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 24, 0, 0, 8);");

        dp.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                final double[] dragDelta = {0, 0};
                dp.setOnMousePressed(e -> {
                    if (e.getY() < 52) { dragDelta[0] = e.getSceneX(); dragDelta[1] = e.getSceneY(); }
                });
                dp.setOnMouseDragged(e -> {
                    if (e.getY() < 52 || dragDelta[0] != 0) {
                        Stage s = (Stage) dp.getScene().getWindow();
                        s.setX(e.getScreenX() - dragDelta[0]);
                        s.setY(e.getScreenY() - dragDelta[1]);
                    }
                });
                dp.setOnMouseReleased(e -> { dragDelta[0] = 0; dragDelta[1] = 0; });
            }
        });

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle("-fx-background-color: linear-gradient(to right, #111, #1a1a1a); "
                + "-fx-background-radius: 16 16 0 0; -fx-border-color: " + accentColor + "; -fx-border-width: 0 0 1 0;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18;");
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 14; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("\u2715");
        closeBtn.setStyle("-fx-background-color: rgba(255,107,107,0.12); -fx-text-fill: #FF6B6B; -fx-font-size: 13; "
                + "-fx-background-radius: 20; -fx-padding: 4 10; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: rgba(255,107,107,0.3); -fx-text-fill: #FF6B6B; -fx-font-size: 13; -fx-background-radius: 20; -fx-padding: 4 10; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: rgba(255,107,107,0.12); -fx-text-fill: #FF6B6B; -fx-font-size: 13; -fx-background-radius: 20; -fx-padding: 4 10; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> {
            if (dp.getButtonTypes().isEmpty()) {
                dp.getButtonTypes().add(ButtonType.CANCEL);
            }
            ((Stage) dp.getScene().getWindow()).close();
        });
        topBar.getChildren().addAll(iconLbl, titleLbl, spacer, closeBtn);
        dp.setHeader(topBar);

        dp.getButtonTypes().addListener((javafx.collections.ListChangeListener<ButtonType>) c -> styleDialogButtons(dp, accentColor));
        javafx.application.Platform.runLater(() -> styleDialogButtons(dp, accentColor));
    }

    private void styleDialogButtons(DialogPane dp, String accentColor) {
        for (ButtonType bt : dp.getButtonTypes()) {
            Button b = (Button) dp.lookupButton(bt);
            if (b == null) continue;
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE || bt.getButtonData() == ButtonBar.ButtonData.APPLY) {
                b.setStyle("-fx-background-color: linear-gradient(to right, " + accentColor + ", #FF8C00); "
                        + "-fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 28; -fx-background-radius: 10; -fx-font-size: 13; -fx-cursor: hand;");
            } else {
                b.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #aaa; "
                        + "-fx-padding: 10 22; -fx-background-radius: 10; -fx-font-size: 12; -fx-cursor: hand;");
            }
        }
        dp.lookupAll(".button-bar").forEach(node -> node.setStyle("-fx-background-color: transparent; -fx-padding: 8 16 16 16;"));
    }

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
            addAIFeatureButtons();

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

        // Language support: translate on init + listen for changes
        TranslationService.addLanguageChangeListener(this::applyTranslation);
        javafx.application.Platform.runLater(() -> {
            collectStaticLabels();
            String lang = TranslationService.getCurrentLang();
            if (!"fr".equals(lang)) applyTranslation();
        });
    }

    private void collectStaticLabels() {
        if (staticLabelsCollected) return;
        javafx.scene.Parent root = null;
        if (titleBar != null && titleBar.getScene() != null) root = titleBar.getScene().getRoot();
        if (root != null) {
            root.lookupAll(".button").forEach(n -> {
                if (n instanceof Labeled l && l.getText() != null && !l.getText().isBlank() && l.getText().length() > 1)
                    staticOriginals.putIfAbsent(l, l.getText());
            });
            root.lookupAll("Label").forEach(n -> {
                if (n instanceof Label l && l.getText() != null && !l.getText().isBlank() && l.getText().length() > 1)
                    staticOriginals.putIfAbsent(l, l.getText());
            });
        }
        staticLabelsCollected = true;
    }

    private void applyTranslation() {
        String lang = TranslationService.getCurrentLang();
        if (!staticLabelsCollected) collectStaticLabels();
        if ("fr".equals(lang)) {
            staticOriginals.forEach((lbl, txt) -> lbl.setText(txt));
        } else {
            staticOriginals.forEach((lbl, txt) ->
                TranslationService.translateAsync(txt, lang, lbl::setText));
        }
    }

    public void setUser(utilisateur user) {
        this.currentUser = user;
        if (user != null) {
            if (userNameLabel != null) userNameLabel.setText(user.getNom() + " " + user.getPrenom());
            // Populate drawer user info
            String fullName = user.getNom() + " " + user.getPrenom();
            if (drawerUserName != null) drawerUserName.setText(fullName);
            if (drawerInitials != null) {
                String initials = ("" + user.getNom().charAt(0) + user.getPrenom().charAt(0)).toUpperCase();
                drawerInitials.setText(initials);
            }
            if (drawerUserRole != null) drawerUserRole.setText("Membre");
            // Refresh loyalty points from DB
            user.setLoyaltyPoints(userService.getLoyaltyPoints(user.getId()));
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
                .filter(p -> "en_attente".equals(p.getStatutItem()) ||
                    ("confirme".equals(p.getStatutItem()) && isRestoOrCafeType(p.getTypeService())))
                .collect(Collectors.toList());
        updateStats();
        updateBadge();
        renderCarousel();
    }

    private void updateStats() {
        int total = panierItems.size();
        long enAttente = panierItems.stream().filter(p -> "en_attente".equals(p.getStatutItem())).count();
        // Only sum prices for non-restaurant items (restaurants are free/pay on spot)
        double totalPrix = panierItems.stream()
                .filter(p -> "en_attente".equals(p.getStatutItem()) && !isRestoOrCafeType(p.getTypeService()))
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
    // BUILD PANIER CARD - ENHANCED CAROUSEL DESIGN
    // ================================================================
    private VBox buildPanierCard(Panier p) {
        VBox card = new VBox(0);
        card.setMinWidth(340);
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color: rgba(15,15,15,0.95); -fx-background-radius: 16; " +
                "-fx-border-color: rgba(255,215,0,0.18); -fx-border-radius: 16; -fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.08), 12, 0, 0, 4);");

        // ── GRADIENT HEADER BAR ──
        HBox headerBar = new HBox(12);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(16, 18, 12, 18));
        headerBar.setStyle("-fx-background-color: linear-gradient(to right, rgba(255,215,0,0.10), rgba(255,140,0,0.06)); -fx-background-radius: 16 16 0 0;");

        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(52, 52);
        iconPane.setStyle("-fx-background-color: rgba(255,215,0,0.12); -fx-background-radius: 14;");
        String icon = getServiceIcon(p.getTypeService());
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24;");
        iconPane.getChildren().add(iconLabel);

        VBox nameBox = new VBox(3);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        String displayName = p.getNomEtablissement() != null && !p.getNomEtablissement().isEmpty()
                ? p.getNomEtablissement() : p.getTypeService();
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold;");
        nameLabel.setMaxWidth(220);
        nameLabel.setWrapText(true);
        Label typeChip = new Label(p.getTypeService());
        typeChip.setStyle("-fx-text-fill: #999; -fx-font-size: 9; -fx-background-color: rgba(255,255,255,0.05); -fx-padding: 2 8; -fx-background-radius: 6;");
        nameBox.getChildren().addAll(nameLabel, typeChip);

        boolean isRestoItem = isRestoOrCafeType(p.getTypeService());
        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        if (isRestoItem) {
            Label gratuitLabel = new Label("Gratuit");
            gratuitLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 16; -fx-font-weight: bold;");
            Label payOnSpot = new Label("Sur place");
            payOnSpot.setStyle("-fx-text-fill: #888; -fx-font-size: 9;");
            priceBox.getChildren().addAll(gratuitLabel, payOnSpot);
        } else {
            Label prixLabel = new Label(String.format("%.2f DT", p.getPrixEstime()));
            prixLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 20; -fx-font-weight: bold;");
            // Per-person price
            if (p.getNbPersonnes() > 1) {
                Label perPerson = new Label(String.format("%.2f DT/pers", p.getPrixEstime() / p.getNbPersonnes()));
                perPerson.setStyle("-fx-text-fill: #888; -fx-font-size: 9;");
                priceBox.getChildren().addAll(prixLabel, perPerson);
            } else {
                priceBox.getChildren().add(prixLabel);
            }
        }
        headerBar.getChildren().addAll(iconPane, nameBox, priceBox);

        // ── DETAIL SECTIONS (scrollable carousel of info tiles) ──
        VBox detailSections = new VBox(8);
        detailSections.setPadding(new Insets(10, 14, 6, 14));

        // Section 1: Date range tile
        HBox dateSection = new HBox(10);
        dateSection.setAlignment(Pos.CENTER_LEFT);
        dateSection.setStyle("-fx-background-color: rgba(100,181,246,0.06); -fx-padding: 10 14; -fx-background-radius: 10;");
        Label calIcon = new Label("\uD83D\uDCC5");
        calIcon.setStyle("-fx-font-size: 16;");
        VBox dateInfo = new VBox(2);
        HBox.setHgrow(dateInfo, Priority.ALWAYS);
        Label dateTitle = new Label("Periode de sejour");
        dateTitle.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 9; -fx-font-weight: bold;");
        Label dateRange = new Label((p.getDateDebut() != null ? p.getDateDebut().format(DTF) : "N/A")
                + "  \u2192  " + (p.getDateFin() != null ? p.getDateFin().format(DTF) : "N/A"));
        dateRange.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        String durationStr = "";
        if (p.getDateDebut() != null && p.getDateFin() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(p.getDateDebut(), p.getDateFin());
            durationStr = days + " jour" + (days > 1 ? "s" : "");
        }
        if (!durationStr.isEmpty()) {
            Label durLabel = new Label("\u23F1 " + durationStr);
            durLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 9;");
            dateInfo.getChildren().addAll(dateTitle, dateRange, durLabel);
        } else {
            dateInfo.getChildren().addAll(dateTitle, dateRange);
        }
        dateSection.getChildren().addAll(calIcon, dateInfo);

        // Section 2: People tiles row
        HBox peopleTiles = new HBox(6);
        peopleTiles.setAlignment(Pos.CENTER);
        peopleTiles.getChildren().addAll(
            buildInfoTile("\uD83D\uDC65", String.valueOf(p.getNbPersonnes()), "Total"),
            buildInfoTile("\uD83E\uDDD1", String.valueOf(p.getNbAdultes()), "Adultes"),
            buildInfoTile("\uD83D\uDC76", String.valueOf(p.getNbEnfants()), "Enfants")
        );

        detailSections.getChildren().addAll(dateSection, peopleTiles);

        // Section 3: Hotel-specific room info
        boolean isHotelItem = p.getTypeService() != null && (p.getTypeService().toLowerCase().contains("hotel") || p.getTypeService().toLowerCase().contains("voyage") || p.getTypeService().toLowerCase().contains("hebergement"));
        if (isHotelItem && p.getNbChambres() > 0) {
            HBox hotelTiles = new HBox(6);
            hotelTiles.setAlignment(Pos.CENTER);
            hotelTiles.getChildren().addAll(
                buildInfoTile("\uD83D\uDECF", String.valueOf(p.getNbChambres()), "Chambres"),
                buildInfoTile("\uD83C\uDF19", String.valueOf(getDurationNights(p)), "Nuits")
            );
            detailSections.getChildren().add(hotelTiles);
        }

        // Section 4: Dynamic Pricing
        if (!isRestoItem) {
            String pricingLevel = getDynamicPricingLevel(p);
            HBox pricingRow = new HBox(8);
            pricingRow.setAlignment(Pos.CENTER_LEFT);
            pricingRow.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 6 12; -fx-background-radius: 8;");
            Label pricingBadge = new Label(pricingLevel);
            pricingBadge.setStyle(getPricingBadgeStyle(pricingLevel));
            pricingRow.getChildren().add(pricingBadge);
            detailSections.getChildren().add(pricingRow);
        }

        // Section 5: Weather widget
        try {
            String city = guessCity(p);
            if (city != null && !city.isEmpty()) {
                com.esprit.services.WeatherService weatherSvc = new com.esprit.services.WeatherService();
                com.esprit.services.WeatherService.WeatherInfo weather = weatherSvc.getWeather(city);
                if (weather != null) {
                    HBox weatherTile = new HBox(10);
                    weatherTile.setAlignment(Pos.CENTER_LEFT);
                    weatherTile.setStyle("-fx-background-color: rgba(100,181,246,0.06); -fx-padding: 10 14; -fx-background-radius: 10;");
                    Label wIcon = new Label(weather.getEmoji());
                    wIcon.setStyle("-fx-font-size: 22;");
                    VBox wInfo = new VBox(2);
                    Label wCity = new Label("\uD83C\uDF0D " + city);
                    wCity.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 10; -fx-font-weight: bold;");
                    Label wTemp = new Label(String.format("%.0f\u00B0C - %s", weather.temp, weather.description));
                    wTemp.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
                    wInfo.getChildren().addAll(wCity, wTemp);
                    weatherTile.getChildren().addAll(wIcon, wInfo);
                    detailSections.getChildren().add(weatherTile);
                }
            }
        } catch (Exception ignored) { /* weather is optional */ }

        // Section 6: Currency converter
        if (!isRestoItem && p.getPrixEstime() > 0) {
            Button currencyBtn = new Button("\uD83D\uDCB1 Convertir devise");
            currencyBtn.setStyle("-fx-background-color: rgba(255,215,0,0.08); -fx-text-fill: #FFD700; -fx-padding: 6 14; -fx-background-radius: 8; -fx-font-size: 10; -fx-cursor: hand;");
            currencyBtn.setMaxWidth(Double.MAX_VALUE);
            currencyBtn.setOnAction(e -> showCurrencyConverterDialog(p.getPrixEstime()));
            detailSections.getChildren().add(currencyBtn);
        }

        // Section 7: Promo discount display
        if (!isRestoItem && appliedPromo != null && discountPercent > 0) {
            double discounted = p.getPrixEstime() * (1 - discountPercent / 100.0);
            HBox promoTile = new HBox(10);
            promoTile.setAlignment(Pos.CENTER);
            promoTile.setStyle("-fx-background-color: rgba(81,207,102,0.06); -fx-padding: 10; -fx-background-radius: 10;");
            Label oldPrice = new Label(String.format("%.2f DT", p.getPrixEstime()));
            oldPrice.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11; -fx-strikethrough: true;");
            Label arrow = new Label("\u2192");
            arrow.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
            Label newPrice = new Label(String.format("%.2f DT", discounted));
            newPrice.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 14; -fx-font-weight: bold;");
            Label discLabel = new Label("-" + (int)discountPercent + "%");
            discLabel.setStyle("-fx-background-color: rgba(81,207,102,0.2); -fx-text-fill: #51CF66; -fx-padding: 2 10; -fx-background-radius: 6; -fx-font-size: 10; -fx-font-weight: bold;");
            promoTile.getChildren().addAll(oldPrice, arrow, newPrice, discLabel);
            detailSections.getChildren().add(promoTile);
        }

        // ── STATUS BADGE ──
        HBox statutRow = new HBox();
        statutRow.setAlignment(Pos.CENTER);
        statutRow.setPadding(new Insets(0, 14, 0, 14));
        Label statutBadge;
        if (isRestoItem && "confirme".equalsIgnoreCase(p.getStatutItem())) {
            statutBadge = new Label("\u2705 Reservation confirmee - Paiement sur place");
            statutBadge.setStyle("-fx-background-color: rgba(81,207,102,0.08); -fx-text-fill: #51CF66; -fx-padding: 6 16; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold;");
        } else {
            statutBadge = new Label("\u23F3 En attente de confirmation");
            statutBadge.setStyle("-fx-background-color: rgba(255,215,0,0.08); -fx-text-fill: #FFD700; -fx-padding: 6 16; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold;");
        }
        statutRow.getChildren().add(statutBadge);

        // ── ACTION BUTTONS ──
        VBox actionsArea = new VBox(6);
        actionsArea.setPadding(new Insets(8, 14, 14, 14));

        HBox mainButtons = new HBox(6);
        mainButtons.setAlignment(Pos.CENTER);

        Button modifyBtn = new Button("\u270F Modifier");
        modifyBtn.setStyle("-fx-background-color: rgba(255,167,38,0.12); -fx-text-fill: #FFA726; -fx-padding: 8 14; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        modifyBtn.setOnAction(e -> showModifyPanierDialog(p));

        if (!isRestoItem) {
            Button confirmBtn = new Button("\u2705 Confirmer");
            confirmBtn.setStyle("-fx-background-color: rgba(81,207,102,0.12); -fx-text-fill: #51CF66; -fx-padding: 8 14; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: hand;");
            confirmBtn.setOnAction(e -> showPaymentMethodDialog(p));
            mainButtons.getChildren().addAll(modifyBtn, confirmBtn);
        } else {
            mainButtons.getChildren().add(modifyBtn);
        }

        Button deleteBtn = new Button("\uD83D\uDDD1");
        deleteBtn.setStyle("-fx-background-color: rgba(255,107,107,0.12); -fx-text-fill: #FF6B6B; -fx-padding: 8 14; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deletePanierItem(p));
        mainButtons.getChildren().add(deleteBtn);

        HBox extraButtons = new HBox(6);
        extraButtons.setAlignment(Pos.CENTER);

        Button duplicateBtn = new Button("\uD83D\uDCCB Dupliquer");
        duplicateBtn.setStyle("-fx-background-color: rgba(41,182,246,0.12); -fx-text-fill: #29B6F6; -fx-padding: 6 10; -fx-background-radius: 8; -fx-font-size: 9; -fx-font-weight: bold; -fx-cursor: hand;");
        duplicateBtn.setOnAction(e -> duplicatePanierItem(p));

        Button compareBtn = new Button("\uD83D\uDCCA Comparer");
        compareBtn.setStyle("-fx-background-color: rgba(255,215,0,0.12); -fx-text-fill: #FFD700; -fx-padding: 6 10; -fx-background-radius: 8; -fx-font-size: 9; -fx-font-weight: bold; -fx-cursor: hand;");
        compareBtn.setOnAction(e -> showPriceComparisonDialog(p));

        extraButtons.getChildren().addAll(duplicateBtn, compareBtn);

        // Split payment info
        if (!isRestoItem && p.getNbPersonnes() > 1 && p.getPrixEstime() > 0) {
            HBox splitRow = new HBox(8);
            splitRow.setAlignment(Pos.CENTER);
            splitRow.setStyle("-fx-background-color: rgba(178,102,255,0.04); -fx-padding: 6; -fx-background-radius: 6;");
            double perPerson = p.getPrixEstime() / p.getNbPersonnes();
            Label splitLbl = new Label("\uD83D\uDC65 Split: " + String.format("%.2f DT", perPerson) + "/personne");
            splitLbl.setStyle("-fx-text-fill: #B266FF; -fx-font-size: 9; -fx-font-weight: bold;");
            splitRow.getChildren().add(splitLbl);
            actionsArea.getChildren().addAll(mainButtons, extraButtons, splitRow);
        } else {
            actionsArea.getChildren().addAll(mainButtons, extraButtons);
        }

        // ── ASSEMBLE CARD ──
        card.getChildren().addAll(headerBar, detailSections, statutRow, actionsArea);
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
        vLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
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
        if (type == null) return "\uD83C\uDFAF";
        if (type.toLowerCase().contains("voyage")) return "\uD83C\uDFDD";
        if (type.toLowerCase().contains("hotel")) return "\uD83C\uDFE8";
        if (type.toLowerCase().contains("caf")) return "\u2615";
        if (type.toLowerCase().contains("rest")) return "\uD83C\uDF55";
        return "\uD83C\uDFAF";
    }

    private boolean isRestoOrCafeType(String type) {
        if (type == null) return false;
        String lower = type.toLowerCase();
        return lower.contains("restaurant") || lower.contains("caf") || lower.contains("resto");
    }

    // ================================================================
    // MODIFY PANIER ITEM DIALOG
    // ================================================================
    private void showModifyPanierDialog(Panier p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'article");
        dialog.setHeaderText(null);
        styleDialog(dialog, "\u270F", "Modifier l'Article du Panier", "#FFA726");

        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(500);

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
        Label titleLbl = new Label("Modifier l'Article du Panier");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");
        String dispName = p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService();
        Label subLbl = new Label(dispName);
        subLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        headerText.getChildren().addAll(titleLbl, subLbl);
        headerRow.getChildren().addAll(headerIcon, headerText);

        boolean isHotelOrVoyage = p.getTypeService() != null &&
            (p.getTypeService().toLowerCase().contains("voyage") || p.getTypeService().toLowerCase().contains("hotel"));

        // Date pickers
        VBox dateSection = new VBox(10);
        dateSection.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 14; -fx-background-radius: 10;");
        Label dateSectionTitle = new Label("\uD83D\uDCC5 Dates");
        dateSectionTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");

        HBox dateRow = new HBox(12);
        VBox debutBox = new VBox(4);
        HBox.setHgrow(debutBox, Priority.ALWAYS);
        Label debutLbl = new Label("Date de debut");
        debutLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        DatePicker debutPicker = new DatePicker(p.getDateDebut() != null ? p.getDateDebut().toLocalDate() : LocalDate.now().plusDays(3));
        debutPicker.setStyle("-fx-background-color: #222;");
        // Prevent selecting past dates (like Booking.com, Expedia, all real travel sites)
        debutPicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: rgba(255,107,107,0.15); -fx-text-fill: #555;");
                }
            }
        });
        debutBox.getChildren().addAll(debutLbl, debutPicker);

        VBox finBox = new VBox(4);
        HBox.setHgrow(finBox, Priority.ALWAYS);
        Label finLbl = new Label("Date de fin");
        finLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        DatePicker finPicker = new DatePicker(p.getDateFin() != null ? p.getDateFin().toLocalDate() : LocalDate.now().plusDays(4));
        finPicker.setStyle("-fx-background-color: #222;");
        // End date must be at least 1 day after start date (minimum 1 night, like all hotel sites)
        finPicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate minEnd = debutPicker.getValue() != null ? debutPicker.getValue().plusDays(1) : LocalDate.now().plusDays(1);
                if (date.isBefore(minEnd)) {
                    setDisable(true);
                    setStyle("-fx-background-color: rgba(255,107,107,0.15); -fx-text-fill: #555;");
                }
            }
        });
        finBox.getChildren().addAll(finLbl, finPicker);
        // Auto-adjust end date when start date changes (like Booking.com, Airbnb)
        debutPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && finPicker.getValue() != null && !finPicker.getValue().isAfter(newVal)) {
                finPicker.setValue(newVal.plusDays(1));
            }
        });
        dateRow.getChildren().addAll(debutBox, finBox);
        dateSection.getChildren().addAll(dateSectionTitle, dateRow);

        // People section
        VBox peopleSection = new VBox(10);
        peopleSection.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 14; -fx-background-radius: 10;");
        Label peopleSectionTitle = new Label("\uD83D\uDC65 Voyageurs");
        peopleSectionTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");

        HBox peopleRow = new HBox(12);
        VBox adultesBox = new VBox(4);
        HBox.setHgrow(adultesBox, Priority.ALWAYS);
        Label adultesLbl = new Label("\uD83E\uDDD1 Adultes");
        adultesLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        Spinner<Integer> adultesSpinner = new Spinner<>(1, 20, Math.max(1, p.getNbAdultes()));
        adultesSpinner.setEditable(true);
        adultesSpinner.setStyle("-fx-background-color: #222;");
        adultesBox.getChildren().addAll(adultesLbl, adultesSpinner);

        VBox enfantsBox = new VBox(4);
        HBox.setHgrow(enfantsBox, Priority.ALWAYS);
        Label enfantsLbl = new Label("\uD83D\uDC76 Enfants");
        enfantsLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        Spinner<Integer> enfantsSpinner = new Spinner<>(0, 10, p.getNbEnfants());
        enfantsSpinner.setEditable(true);
        enfantsSpinner.setStyle("-fx-background-color: #222;");
        enfantsBox.getChildren().addAll(enfantsLbl, enfantsSpinner);

        peopleRow.getChildren().addAll(adultesBox, enfantsBox);
        peopleSection.getChildren().addAll(peopleSectionTitle, peopleRow);

        // Days section (only for hotel/voyage)
        VBox daysSection = new VBox(4);
        daysSection.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 14; -fx-background-radius: 10;");
        Label daysLbl = new Label("\uD83C\uDFE8 Nombre de jours");
        daysLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
        long currentDays = 1;
        if (p.getDateDebut() != null && p.getDateFin() != null) {
            currentDays = java.time.Duration.between(p.getDateDebut(), p.getDateFin()).toDays();
            if (currentDays < 1) currentDays = 1;
        }
        Spinner<Integer> daysSpinner = new Spinner<>(1, 30, (int) currentDays);
        daysSpinner.setEditable(true);
        daysSpinner.setStyle("-fx-background-color: #222;");
        daysSection.getChildren().addAll(daysLbl, daysSpinner);
        daysSection.setVisible(isHotelOrVoyage);
        daysSection.setManaged(isHotelOrVoyage);

        // Price display - calculate base price per unit
        double basePrice = p.getPrixEstime() / Math.max(1, p.getNbPersonnes());
        if (isHotelOrVoyage && currentDays > 0) {
            basePrice = basePrice / currentDays;
        }
        double bp = basePrice;

        VBox priceDisplay = new VBox(6);
        priceDisplay.setStyle("-fx-background-color: rgba(255,215,0,0.08); -fx-padding: 14; -fx-background-radius: 10;");
        priceDisplay.setAlignment(Pos.CENTER);
        Label priceInfoLbl = new Label("Prix unitaire: " + String.format("%.2f DT", bp));
        priceInfoLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");
        Label totalLabel = new Label();
        totalLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 22; -fx-font-weight: bold;");
        priceDisplay.getChildren().addAll(priceInfoLbl, totalLabel);

        Runnable updateTotal = () -> {
            int totalPeople = adultesSpinner.getValue() + enfantsSpinner.getValue();
            int days = isHotelOrVoyage ? daysSpinner.getValue() : 1;
            double total = bp * totalPeople * days;
            totalLabel.setText(String.format("Nouveau Total: %.2f DT", total));
        };
        updateTotal.run();
        adultesSpinner.valueProperty().addListener((obs, o, n) -> updateTotal.run());
        enfantsSpinner.valueProperty().addListener((obs, o, n) -> updateTotal.run());
        if (isHotelOrVoyage) {
            daysSpinner.valueProperty().addListener((obs, o, n) -> {
                updateTotal.run();
                // Auto-update end date
                LocalDate start = debutPicker.getValue();
                if (start != null) finPicker.setValue(start.plusDays(n));
            });
        }

        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");

        content.getChildren().addAll(headerRow, dateSection, peopleSection, daysSection, priceDisplay, errorLbl);
        dp.setContent(content);

        ButtonType saveType = new ButtonType("\uD83D\uDCBE Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(saveType, cancelType);

        Button saveBtn = (Button) dp.lookupButton(saveType);
        saveBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 28; -fx-background-radius: 8; -fx-font-size: 13;");

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            LocalDate startDate = debutPicker.getValue();
            LocalDate endDate = finPicker.getValue();
            if (startDate == null || endDate == null) {
                errorLbl.setText("\u274C Veuillez selectionner les dates");
                event.consume(); return;
            }
            // Prevent past dates (standard on Booking.com, Expedia, all real travel sites)
            if (startDate.isBefore(LocalDate.now())) {
                errorLbl.setText("\u274C La date de debut ne peut pas etre dans le passe");
                event.consume(); return;
            }
            if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
                errorLbl.setText("\u274C La date de fin doit etre au moins 1 jour apres le debut (minimum 1 nuit)");
                event.consume(); return;
            }

            int totalPeople = adultesSpinner.getValue() + enfantsSpinner.getValue();
            int days = isHotelOrVoyage ? daysSpinner.getValue() : 1;
            double newTotal = bp * totalPeople * days;

            p.setDateDebut(startDate.atStartOfDay());
            p.setDateFin(endDate.atStartOfDay());
            p.setNbPersonnes(totalPeople);
            p.setNbAdultes(adultesSpinner.getValue());
            p.setNbEnfants(enfantsSpinner.getValue());
            p.setPrixEstime(newTotal);

            if (panierService.modifier(p)) {
                showMessage("\u2705 Article modifie avec succes !", true);
                loadData();
            } else {
                errorLbl.setText("\u274C Echec de la modification.");
                event.consume();
            }
        });

        dialog.setResultConverter(bt -> null);
        dialog.showAndWait();
    }

    // ================================================================
    // PROMO CODE DIALOG
    // ================================================================
    @FXML public void handleApplyPromo() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Code Promo");
        dialog.setHeaderText(null);
        styleDialog(dialog, "\uD83C\uDF89", "Code Promotionnel", "#B266FF");

        DialogPane dp = dialog.getDialogPane();
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

        // Loyalty Points Section
        int userPoints = currentUser != null ? userService.getLoyaltyPoints(currentUser.getId()) : 0;
        VBox loyaltyBox = new VBox(8);
        loyaltyBox.setStyle("-fx-background-color: rgba(178,102,255,0.06); -fx-padding: 14; -fx-background-radius: 10; -fx-border-color: rgba(178,102,255,0.15); -fx-border-radius: 10; -fx-border-width: 1;");
        Label loyaltyTitle = new Label("\u2B50 Points de Fidelite: " + userPoints + " pts");
        loyaltyTitle.setStyle("-fx-text-fill: #B266FF; -fx-font-size: 13; -fx-font-weight: bold;");
        Label loyaltyDesc = new Label("Echangez vos points contre des reductions !");
        loyaltyDesc.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        HBox loyaltyBtns = new HBox(8);
        loyaltyBtns.setAlignment(Pos.CENTER);
        for (int i = 0; i < LOYALTY_THRESHOLDS.length; i++) {
            int pts = LOYALTY_THRESHOLDS[i];
            double disc = LOYALTY_DISCOUNTS[i];
            Button loyBtn = new Button(pts + " pts = -" + (int)disc + "%");
            boolean canAfford = userPoints >= pts;
            loyBtn.setStyle("-fx-background-color: " + (canAfford ? "rgba(178,102,255,0.15)" : "rgba(255,255,255,0.04)") +
                "; -fx-text-fill: " + (canAfford ? "#B266FF" : "#444") +
                "; -fx-padding: 6 12; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-cursor: " + (canAfford ? "hand" : "default") + ";");
            if (canAfford) {
                final double discount = disc;
                final int points = pts;
                loyBtn.setOnAction(e -> {
                    if (userService.deductLoyaltyPoints(currentUser.getId(), points)) {
                        appliedPromo = "LOYALTY-" + points;
                        discountPercent = discount;
                        currentUser.setLoyaltyPoints(currentUser.getLoyaltyPoints() - points);
                        showMessage("\u2B50 " + points + " points utilises ! -" + (int)discount + "% de reduction", true);
                        loadData();
                        dialog.close();
                    }
                });
            }
            loyaltyBtns.getChildren().add(loyBtn);
        }
        loyaltyBox.getChildren().addAll(loyaltyTitle, loyaltyDesc, loyaltyBtns);

        content.getChildren().addAll(titleLbl, descLbl, inputRow, resultLbl, hintBox, loyaltyBox);
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
        styleDialog(dialog, "\uD83D\uDCB3", "Choisir le mode de paiement", "#FFD700");

        DialogPane dp = dialog.getDialogPane();
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
        priceSection.setStyle("-fx-background-color: rgba(255,215,0,0.04); -fx-padding: 14; -fx-background-radius: 10;");

        // Price breakdown (like Booking.com, Expedia — show per-person and per-night)
        VBox breakdownBox = new VBox(4);
        breakdownBox.setStyle("-fx-padding: 0 0 6 0;");
        long nights = 1;
        if (p.getDateDebut() != null && p.getDateFin() != null) {
            nights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(p.getDateDebut().toLocalDate(), p.getDateFin().toLocalDate()));
        }
        if (p.getNbPersonnes() > 1) {
            Label perPersonLbl = new Label(String.format("\uD83D\uDC65 %d personnes x %.2f DT", p.getNbPersonnes(), p.getPrixEstime() / p.getNbPersonnes()));
            perPersonLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
            breakdownBox.getChildren().add(perPersonLbl);
        }
        boolean isHotelType = p.getTypeService() != null && (p.getTypeService().toLowerCase().contains("hotel") || p.getTypeService().toLowerCase().contains("voyage"));
        if (isHotelType && nights > 1) {
            Label perNightLbl = new Label(String.format("\uD83C\uDF19 %d nuit%s x %.2f DT/nuit", nights, nights > 1 ? "s" : "", p.getPrixEstime() / nights));
            perNightLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
            breakdownBox.getChildren().add(perNightLbl);
        }
        if (!breakdownBox.getChildren().isEmpty()) {
            priceSection.getChildren().add(breakdownBox);
        }

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

        // Loyalty points info
        int pointsToEarn = (int)(finalPrice * 10); // 10 points per DT with card
        VBox loyaltyInfo = new VBox(4);
        loyaltyInfo.setAlignment(Pos.CENTER);
        loyaltyInfo.setStyle("-fx-background-color: rgba(178,102,255,0.06); -fx-padding: 10; -fx-background-radius: 8;");
        Label loyaltyLbl = new Label("\u2B50 Payez par Paymee et gagnez " + pointsToEarn + " points de fidelite !");
        loyaltyLbl.setStyle("-fx-text-fill: #B266FF; -fx-font-size: 11; -fx-font-weight: bold;");
        Label currentPtsLbl = new Label("Solde actuel: " + (currentUser != null ? userService.getLoyaltyPoints(currentUser.getId()) : 0) + " pts");
        currentPtsLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        loyaltyInfo.getChildren().addAll(loyaltyLbl, currentPtsLbl);

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
        Label cashDesc = new Label("Reserve en attente\nApprobation admin requise");
        cashDesc.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        cashDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        cashBox.getChildren().addAll(cashIcon, cashLbl, cashDesc);

        VBox cardBox = new VBox(10);
        cardBox.setAlignment(Pos.CENTER);
        cardBox.setPadding(new Insets(20));
        cardBox.setStyle("-fx-background-color: rgba(100,181,246,0.08); -fx-background-radius: 14; -fx-border-color: rgba(100,181,246,0.2); -fx-border-radius: 14; -fx-border-width: 1; -fx-cursor: hand; -fx-pref-width: 180;");
        Label cardIcon = new Label("\uD83D\uDCF1");
        cardIcon.setStyle("-fx-font-size: 36;");
        Label cardLbl = new Label("Paymee");
        cardLbl.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 14; -fx-font-weight: bold;");
        Label cardDesc = new Label("Paiement en ligne\n\u2B50 +" + pointsToEarn + " points !");
        cardDesc.setStyle("-fx-text-fill: #B266FF; -fx-font-size: 10; -fx-font-weight: bold;");
        cardDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        cardBox.getChildren().addAll(cardIcon, cardLbl, cardDesc);

        paymentButtons.getChildren().addAll(cashBox, cardBox);
        content.getChildren().addAll(titleLbl, itemLbl, priceSection, loyaltyInfo, paymentButtons);
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
        // Cash payment = booking reserved, awaiting admin approval before it's confirmed
        r.setStatutPaiement("En cours de paiement");

        if (reservationService.ajouter(r)) {
            panierService.modifier(updatePanierStatut(p, "confirme"));
            // Send "reserved, awaiting payment" email (no loyalty points for cash)
            sendCashReservationEmail(p, r, finalPrice);
            showCashReservationSuccessOverlay(p, r, finalPrice);
            loadData();
        } else {
            showMessage("Echec du paiement.", false);
        }
    }

    // ================================================================
    // PAYMEE PAYMENT DIALOG — IN-APP WEBVIEW (no external browser!)
    // ================================================================
    private void showCardPaymentDialog(Panier p, double finalPrice) {
        // Use a Stage instead of Dialog for the in-app WebView payment
        Stage paymentStage = new Stage();
        paymentStage.setTitle("Paiement Paymee - TABAANI SmartTravel");
        paymentStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        paymentStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        int pointsToEarn = (int)(finalPrice * 10);
        final String[] paymentId = {null};
        final java.util.concurrent.atomic.AtomicBoolean paymentDone = new java.util.concurrent.atomic.AtomicBoolean(false);

        // ── Root container
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #333; -fx-border-width: 1;");

        // ── Custom title bar (draggable)
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle("-fx-background-color: #111; -fx-background-radius: 16 16 0 0;");

        Label paymeeIcon = new Label("\uD83D\uDCB3");
        paymeeIcon.setStyle("-fx-font-size: 18;");
        Label topTitle = new Label("Paiement Paymee");
        topTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14; -fx-font-weight: bold;");
        Label amountTag = new Label(String.format("%.2f DT", finalPrice));
        amountTag.setStyle("-fx-background-color: rgba(255,215,0,0.15); -fx-text-fill: #FFD700; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 12; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("\u2715");
        closeBtn.setStyle("-fx-background-color: rgba(255,107,107,0.15); -fx-text-fill: #FF6B6B; -fx-font-size: 14; -fx-background-radius: 20; -fx-padding: 4 10; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> paymentStage.close());

        topBar.getChildren().addAll(paymeeIcon, topTitle, amountTag, spacer, closeBtn);

        // Make title bar draggable
        final double[] dragOffset = {0, 0};
        topBar.setOnMousePressed(e -> { dragOffset[0] = e.getSceneX(); dragOffset[1] = e.getSceneY(); });
        topBar.setOnMouseDragged(e -> {
            paymentStage.setX(e.getScreenX() - dragOffset[0]);
            paymentStage.setY(e.getScreenY() - dragOffset[1]);
        });

        root.setTop(topBar);

        // ── Initial view: info + pay button (before generating link)
        VBox initialView = new VBox(14);
        initialView.setPadding(new Insets(20, 24, 20, 24));
        initialView.setAlignment(Pos.CENTER);
        initialView.setStyle("-fx-background-color: #1a1a1a;");

        Label mainIcon = new Label("\uD83D\uDCB3");
        mainIcon.setStyle("-fx-font-size: 48;");

        Label payTitle = new Label("Paiement securise via Paymee");
        payTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        Label paySubtitle = new Label("Le paiement s'effectue directement dans l'application.\nAucune redirection externe.");
        paySubtitle.setStyle("-fx-text-fill: #888; -fx-font-size: 11; -fx-text-alignment: center;");
        paySubtitle.setWrapText(true);

        // Amount card
        HBox amountCard = new HBox();
        amountCard.setAlignment(Pos.CENTER);
        amountCard.setStyle("-fx-background-color: linear-gradient(to right, rgba(255,215,0,0.1), rgba(255,140,0,0.1)); -fx-padding: 16 20; -fx-background-radius: 12;");
        VBox amountInfo = new VBox(4);
        amountInfo.setAlignment(Pos.CENTER);
        Label amountLabel = new Label(String.format("%.2f DT", finalPrice));
        amountLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 28; -fx-font-weight: bold;");
        Label amountSub = new Label("Montant total a payer");
        amountSub.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        amountInfo.getChildren().addAll(amountLabel, amountSub);
        amountCard.getChildren().add(amountInfo);

        // Points bonus
        HBox bonusCard = new HBox(8);
        bonusCard.setAlignment(Pos.CENTER);
        bonusCard.setStyle("-fx-background-color: rgba(178,102,255,0.08); -fx-padding: 10 16; -fx-background-radius: 8;");
        Label bonusLbl = new Label("\u2B50 +" + pointsToEarn + " points de fidelite");
        bonusLbl.setStyle("-fx-text-fill: #B266FF; -fx-font-size: 11; -fx-font-weight: bold;");
        bonusCard.getChildren().add(bonusLbl);

        // Pay button
        Button payBtn = new Button("\uD83D\uDCB3 Payer avec Paymee");
        payBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 14 40; -fx-background-radius: 12; -fx-font-size: 14; -fx-cursor: hand;");
        payBtn.setMaxWidth(Double.MAX_VALUE);

        // Security badges
        HBox securityRow = new HBox(16);
        securityRow.setAlignment(Pos.CENTER);
        Label secBadge1 = new Label("\uD83D\uDD12 SSL Securise");
        secBadge1.setStyle("-fx-text-fill: #666; -fx-font-size: 9;");
        Label secBadge2 = new Label("\u2705 BCT Approuve");
        secBadge2.setStyle("-fx-text-fill: #666; -fx-font-size: 9;");
        Label secBadge3 = new Label("\uD83C\uDDF9\uD83C\uDDF3 Tunisie");
        secBadge3.setStyle("-fx-text-fill: #666; -fx-font-size: 9;");
        securityRow.getChildren().addAll(secBadge1, secBadge2, secBadge3);

        Label statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.setMaxWidth(400);

        initialView.getChildren().addAll(mainIcon, payTitle, paySubtitle, amountCard, bonusCard, payBtn, securityRow, statusLbl);

        // ── WebView for in-app payment
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webView.setPrefSize(700, 580);
        webView.setStyle("-fx-background-color: white;");

        // Status bar below WebView
        HBox webStatusBar = new HBox(10);
        webStatusBar.setPadding(new Insets(8, 16, 8, 16));
        webStatusBar.setAlignment(Pos.CENTER_LEFT);
        webStatusBar.setStyle("-fx-background-color: #111; -fx-background-radius: 0 0 16 16;");
        Label webStatusIcon = new Label("\uD83D\uDD12");
        webStatusIcon.setStyle("-fx-font-size: 12;");
        Label webStatusLbl = new Label("Chargement de la page de paiement...");
        webStatusLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        Region webSpacer = new Region();
        HBox.setHgrow(webSpacer, Priority.ALWAYS);
        Button cancelPayBtn = new Button("Annuler");
        cancelPayBtn.setStyle("-fx-background-color: rgba(255,107,107,0.15); -fx-text-fill: #FF6B6B; -fx-padding: 5 16; -fx-background-radius: 6; -fx-font-size: 10; -fx-cursor: hand;");
        cancelPayBtn.setOnAction(e -> paymentStage.close());
        webStatusBar.getChildren().addAll(webStatusIcon, webStatusLbl, webSpacer, cancelPayBtn);

        VBox webViewContainer = new VBox();
        webViewContainer.getChildren().addAll(webView, webStatusBar);
        VBox.setVgrow(webView, Priority.ALWAYS);

        // ── Listen for URL changes to detect payment success/failure
        webEngine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl != null && !paymentDone.get()) {
                System.out.println("[PAYMEE WEBVIEW] Navigated to: " + newUrl);
                String lower = newUrl.toLowerCase();
                if (lower.contains(PaymeePaymentService.SUCCESS_URL.toLowerCase()) || lower.contains("/payment/success")) {
                    webStatusLbl.setText("\u2705 Paiement detecte ! Verification en cours...");
                    webStatusLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 10; -fx-font-weight: bold;");
                    // Auto-verify the payment
                    if (paymentId[0] != null && !paymentDone.get()) {
                        new Thread(() -> {
                            // Small delay to let Paymee process
                            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                            if (paymentDone.get()) return;
                            PaymeePaymentService.PaymentResult verifyResult = paymeeService.verifyPayment(paymentId[0]);
                            javafx.application.Platform.runLater(() -> {
                                if (paymentDone.get()) return;
                                if (verifyResult.isSuccess()) {
                                    paymentDone.set(true);
                                    processPaymeePaymentSuccess(p, finalPrice, pointsToEarn, paymentId[0]);
                                    paymentStage.close();
                                } else {
                                    // Payment might not be confirmed yet by Paymee, show manual verify
                                    showPostPaymentVerification(paymentStage, root, p, finalPrice, pointsToEarn, paymentId[0]);
                                }
                            });
                        }).start();
                    }
                } else if (lower.contains(PaymeePaymentService.FAIL_URL.toLowerCase()) || lower.contains("/payment/fail")) {
                    webStatusLbl.setText("\u274C Paiement refuse ou annule.");
                    webStatusLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 10; -fx-font-weight: bold;");
                    javafx.application.Platform.runLater(() -> {
                        root.setCenter(initialView);
                        statusLbl.setText("\u274C Paiement refuse. Vous pouvez reessayer.");
                        statusLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");
                        payBtn.setDisable(false);
                        payBtn.setText("\uD83D\uDCB3 Reessayer le paiement");
                    });
                }
            }
        });

        // Loading indicator on WebView
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                webStatusLbl.setText("\u23F3 Chargement...");
                webStatusLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
            } else if (newState == Worker.State.SUCCEEDED) {
                webStatusLbl.setText("\uD83D\uDD12 Page securisee chargee");
                webStatusLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 10;");
            } else if (newState == Worker.State.FAILED) {
                webStatusLbl.setText("\u26A0 Erreur de chargement. Verifiez votre connexion.");
                webStatusLbl.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 10;");
            }
        });

        // ── Pay button action: generate link → show WebView
        payBtn.setOnAction(e -> {
            payBtn.setDisable(true);
            payBtn.setText("\u23F3 Connexion a Paymee...");
            statusLbl.setText("");

            String description = "TABAANI_" + (p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService());

            // Pre-fill customer info for Paymee checkout form
            String custFirst = currentUser != null ? currentUser.getPrenom() : null;
            String custLast = currentUser != null ? currentUser.getNom() : null;
            String custEmail = currentUser != null ? currentUser.getEmail() : null;
            String custPhone = currentUser != null ? String.valueOf(currentUser.getNumTel()) : null;

            new Thread(() -> {
                PaymeePaymentService.PaymentResult result = paymeeService.initPayment(
                    finalPrice, description, custFirst, custLast, custEmail, custPhone);

                javafx.application.Platform.runLater(() -> {
                    if (result.isSuccess() && result.getPaymentLink() != null) {
                        paymentId[0] = result.getPaymentId();
                        // Load payment page in the in-app WebView
                        webEngine.load(result.getPaymentLink());
                        root.setCenter(webViewContainer);
                        // Resize the stage for the WebView
                        paymentStage.setWidth(750);
                        paymentStage.setHeight(680);
                        paymentStage.centerOnScreen();

                        // Start payment status polling every 5 seconds as fallback
                        // In case URL redirect detection fails (sandbox/bank card issues)
                        javafx.animation.Timeline poller = new javafx.animation.Timeline(
                            new javafx.animation.KeyFrame(Duration.seconds(5), ev -> {
                                if (paymentId[0] != null && !paymentDone.get()) {
                                    new Thread(() -> {
                                        if (paymentDone.get()) return;
                                        try {
                                            PaymeePaymentService.PaymentResult pollResult = paymeeService.verifyPayment(paymentId[0]);
                                            if (!paymentDone.get() && pollResult.isSuccess() && "completed".equalsIgnoreCase(pollResult.getStatus())) {
                                                javafx.application.Platform.runLater(() -> {
                                                    if (paymentDone.compareAndSet(false, true)) {
                                                        processPaymeePaymentSuccess(p, finalPrice, pointsToEarn, paymentId[0]);
                                                        paymentStage.close();
                                                    }
                                                });
                                            }
                                        } catch (Exception ex) {
                                            System.err.println("[PAYMEE POLL] Error: " + ex.getMessage());
                                        }
                                    }).start();
                                }
                            })
                        );
                        poller.setCycleCount(60); // Poll for up to 5 minutes
                        poller.play();
                        paymentStage.setOnHidden(ev -> {
                            paymentDone.set(true);
                            poller.stop();
                            // Clean up WebView to prevent native crashes
                            try { webEngine.load(null); } catch (Exception ignored) {}
                        });
                    } else {
                        statusLbl.setText("\u274C " + result.getMessage());
                        statusLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");
                        payBtn.setDisable(false);
                        payBtn.setText("\uD83D\uDCB3 Reessayer le paiement Paymee");
                    }
                });
            }).start();
        });

        // Start with the initial info view
        root.setCenter(initialView);

        Scene scene = new Scene(root, 520, 480);
        scene.setFill(Color.TRANSPARENT);
        paymentStage.setScene(scene);
        paymentStage.centerOnScreen();
        paymentStage.showAndWait();
    }

    /**
     * Show post-payment verification screen when auto-verify didn't confirm yet.
     */
    private void showPostPaymentVerification(Stage paymentStage, BorderPane root, Panier p,
                                              double finalPrice, int pointsToEarn, String paymeePaymentId) {
        VBox verifyView = new VBox(16);
        verifyView.setPadding(new Insets(30, 24, 30, 24));
        verifyView.setAlignment(Pos.CENTER);
        verifyView.setStyle("-fx-background-color: #1a1a1a;");

        Label checkIcon = new Label("\u23F3");
        checkIcon.setStyle("-fx-font-size: 48;");

        Label verifyTitle = new Label("Verification du paiement");
        verifyTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        Label verifyMsg = new Label("Le paiement a ete detecte. Veuillez patienter\nou cliquez le bouton ci-dessous pour verifier.");
        verifyMsg.setStyle("-fx-text-fill: #888; -fx-font-size: 11; -fx-text-alignment: center;");
        verifyMsg.setWrapText(true);

        Label verifyStatus = new Label("");
        verifyStatus.setWrapText(true);
        verifyStatus.setMaxWidth(400);

        Button verifyBtn = new Button("\u2705 Verifier le paiement");
        verifyBtn.setStyle("-fx-background-color: rgba(81,207,102,0.2); -fx-text-fill: #51CF66; -fx-font-weight: bold; -fx-padding: 12 32; -fx-background-radius: 10; -fx-font-size: 13; -fx-cursor: hand;");
        verifyBtn.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: rgba(255,107,107,0.1); -fx-text-fill: #FF6B6B; -fx-padding: 8 24; -fx-background-radius: 8; -fx-font-size: 11; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> paymentStage.close());

        verifyBtn.setOnAction(e -> {
            verifyBtn.setDisable(true);
            verifyBtn.setText("\u23F3 Verification en cours...");
            verifyStatus.setText("");

            new Thread(() -> {
                PaymeePaymentService.PaymentResult result = paymeeService.verifyPayment(paymeePaymentId);
                javafx.application.Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        verifyStatus.setText("\u2705 Paiement confirme !");
                        verifyStatus.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 13; -fx-font-weight: bold;");
                        processPaymeePaymentSuccess(p, finalPrice, pointsToEarn, paymeePaymentId);
                        // Close after small delay so user sees the success
                        new Thread(() -> {
                            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                            javafx.application.Platform.runLater(paymentStage::close);
                        }).start();
                    } else {
                        verifyStatus.setText("\u23F3 " + result.getMessage() + "\nReessayez dans quelques secondes.");
                        verifyStatus.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 11;");
                        verifyBtn.setDisable(false);
                        verifyBtn.setText("\u2705 Verifier le paiement");
                    }
                });
            }).start();
        });

        verifyView.getChildren().addAll(checkIcon, verifyTitle, verifyMsg, verifyBtn, verifyStatus, cancelBtn);
        root.setCenter(verifyView);
        paymentStage.setWidth(520);
        paymentStage.setHeight(420);
        paymentStage.centerOnScreen();
    }

    private void processPaymeePaymentSuccess(Panier p, double finalPrice, int pointsToEarn, String paymeePaymentId) {
        Reservation r = new Reservation();
        r.setIdPanier(p.getIdPanier());
        r.setDatePaiement(LocalDateTime.now());
        r.setMontantTotal(finalPrice);
        r.setModePaiement("Paymee");
        r.setStatutPaiement("Paye");

        if (reservationService.ajouter(r)) {
            panierService.modifier(updatePanierStatut(p, "confirme"));
            if (currentUser != null) {
                userService.addLoyaltyPoints(currentUser.getId(), pointsToEarn);
                currentUser.setLoyaltyPoints(currentUser.getLoyaltyPoints() + pointsToEarn);
            }
            sendConfirmationEmail(p, r, finalPrice, pointsToEarn);
            showPaymentSuccessOverlay(p, r, finalPrice, pointsToEarn);
            loadData();
        } else {
            showMessage("Echec de l'enregistrement de la reservation.", false);
        }
    }

    private void sendConfirmationEmail(Panier p, Reservation r, double finalPrice, int pointsEarned) {
        if (currentUser == null || currentUser.getEmail() == null) return;
        new Thread(() -> {
            String customerName = currentUser.getPrenom() + " " + currentUser.getNom();
            String serviceName = p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService();
            int totalPoints = userService.getLoyaltyPoints(currentUser.getId());
            emailService.sendPaymentConfirmationEmail(
                currentUser.getEmail(), customerName, serviceName,
                r.getCodeConfirmation(), finalPrice, r.getModePaiement(),
                pointsEarned, totalPoints
            );
            // Also send SMS notification
            try {
                int numTel = currentUser.getNumTel();
                if (numTel > 0) {
                    String phoneStr = String.valueOf(numTel);
                    smsService.sendPaymentConfirmation(
                        phoneStr, customerName, serviceName,
                        r.getCodeConfirmation(), finalPrice);
                }
            } catch (Exception e) {
                System.err.println("[SMS] Could not send SMS: " + e.getMessage());
            }
        }).start();
    }

    // ================================================================
    // CASH RESERVATION EMAIL (reserved, awaiting admin approval)
    // ================================================================
    private void sendCashReservationEmail(Panier p, Reservation r, double finalPrice) {
        if (currentUser == null || currentUser.getEmail() == null) return;
        new Thread(() -> {
            String customerName = currentUser.getPrenom() + " " + currentUser.getNom();
            String serviceName = p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService();
            // Send a "reserved, awaiting payment" email instead of a "payment confirmed" email
            emailService.sendCashReservationEmail(
                currentUser.getEmail(), customerName, serviceName,
                r.getCodeConfirmation(), finalPrice
            );
            // Also send SMS notification with "awaiting" message
            try {
                int numTel = currentUser.getNumTel();
                if (numTel > 0) {
                    String phoneStr = String.valueOf(numTel);
                    smsService.sendCashReservationSms(
                        phoneStr, customerName, serviceName,
                        r.getCodeConfirmation(), finalPrice);
                }
            } catch (Exception e) {
                System.err.println("[SMS] Could not send SMS: " + e.getMessage());
            }
        }).start();
    }

    // ================================================================
    // CASH RESERVATION SUCCESS OVERLAY (different from payment success)
    // ================================================================
    private void showCashReservationSuccessOverlay(Panier p, Reservation r, double finalPrice) {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        StackPane rootPane = findRootStackPane(stage);
        if (rootPane == null) {
            showMessage("\u23F3 Reservation en attente ! Code: " + r.getCodeConfirmation(), true);
            return;
        }

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.85);");
        overlay.setOpacity(0);

        VBox successCard = new VBox(14);
        successCard.setMaxWidth(440);
        successCard.setAlignment(Pos.CENTER);
        successCard.setPadding(new Insets(36, 32, 32, 32));
        successCard.setStyle("-fx-background-color: rgba(15,15,15,0.98); -fx-background-radius: 20; " +
                "-fx-border-color: rgba(100,181,246,0.3); -fx-border-radius: 20; -fx-border-width: 2;");

        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(80, 80);
        iconCircle.setMaxSize(80, 80);
        iconCircle.setStyle("-fx-background-color: rgba(100,181,246,0.15); -fx-background-radius: 40;");
        Label hourglassIcon = new Label("\u23F3");
        hourglassIcon.setStyle("-fx-font-size: 40;");
        iconCircle.getChildren().add(hourglassIcon);
        iconCircle.setScaleX(0);
        iconCircle.setScaleY(0);

        Label successTitle = new Label("Reservation Enregistree !");
        successTitle.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 22; -fx-font-weight: bold;");
        successTitle.setOpacity(0);

        Label successDesc = new Label("Votre reservation est en attente d'approbation par l'administrateur.\nVous recevrez un email de confirmation une fois approuvee.");
        successDesc.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");
        successDesc.setWrapText(true);
        successDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        successDesc.setOpacity(0);

        VBox receipt = new VBox(8);
        receipt.setStyle("-fx-background-color: rgba(100,181,246,0.04); -fx-padding: 16; -fx-background-radius: 12; -fx-border-color: rgba(100,181,246,0.1); -fx-border-radius: 12; -fx-border-width: 1;");
        receipt.setOpacity(0);
        String itemName = p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService();
        addReceiptRow(receipt, "\uD83C\uDFAF Service", itemName);
        addReceiptRow(receipt, "\uD83D\uDC65 Personnes", String.valueOf(p.getNbPersonnes()));
        addReceiptRow(receipt, "\uD83D\uDCB3 Mode", "Especes");
        addReceiptRow(receipt, "\uD83D\uDCCA Statut", "En attente d'approbation");
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: rgba(100,181,246,0.15);");
        receipt.getChildren().add(divider);
        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label totalLblLeft = new Label("Montant a Payer");
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

        Label emailNotif = new Label("\uD83D\uDCE7 Un email de confirmation vous sera envoye apres approbation");
        emailNotif.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        emailNotif.setOpacity(0);

        Label upgradeTip = new Label("\u2B50 Passez en Konnect depuis 'Modifier' pour un paiement immediat et gagner des points !");
        upgradeTip.setStyle("-fx-text-fill: #B266FF; -fx-font-size: 10; -fx-background-color: rgba(178,102,255,0.06); -fx-padding: 8 12; -fx-background-radius: 8;");
        upgradeTip.setWrapText(true);
        upgradeTip.setOpacity(0);

        Button closeBtn = new Button("\u2714 Fermer");
        closeBtn.setStyle("-fx-background-color: linear-gradient(to right, #64B5F6, #42A5F5); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 40; -fx-background-radius: 10; -fx-font-size: 14; -fx-cursor: hand;");
        closeBtn.setOpacity(0);

        successCard.getChildren().addAll(iconCircle, successTitle, successDesc, receipt, codeBadge, emailNotif, upgradeTip, closeBtn);
        overlay.getChildren().add(successCard);

        closeBtn.setOnAction(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(300), overlay);
            fade.setToValue(0);
            fade.setOnFinished(ev -> rootPane.getChildren().remove(overlay));
            fade.play();
        });

        rootPane.getChildren().add(overlay);

        // Animations
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), overlay);
        fadeIn.setToValue(1);
        fadeIn.play();

        ScaleTransition scale = new ScaleTransition(Duration.millis(500), iconCircle);
        scale.setFromX(0);
        scale.setFromY(0);
        scale.setToX(1);
        scale.setToY(1);
        scale.setDelay(Duration.millis(200));
        scale.play();

        FadeTransition[] fades = {
            new FadeTransition(Duration.millis(400), successTitle),
            new FadeTransition(Duration.millis(400), successDesc),
            new FadeTransition(Duration.millis(400), receipt),
            new FadeTransition(Duration.millis(400), codeBadge),
            new FadeTransition(Duration.millis(400), emailNotif),
            new FadeTransition(Duration.millis(400), upgradeTip),
            new FadeTransition(Duration.millis(400), closeBtn)
        };
        for (int i = 0; i < fades.length; i++) {
            fades[i].setToValue(1);
            fades[i].setDelay(Duration.millis(400 + i * 100));
            fades[i].play();
        }
    }

    // ================================================================
    // ANIMATED PAYMENT SUCCESS OVERLAY WITH CONFETTI
    // ================================================================
    private void showPaymentSuccessOverlay(Panier p, Reservation r, double finalPrice, int pointsEarned) {
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

        VBox successCard = new VBox(14);
        successCard.setMaxWidth(440);
        successCard.setAlignment(Pos.CENTER);
        successCard.setPadding(new Insets(36, 32, 32, 32));
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
        addReceiptRow(receipt, "\uD83D\uDCB3 Mode", r.getModePaiement());
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

        // Loyalty points badge (only for card payments)
        VBox loyaltyBadge = new VBox(4);
        loyaltyBadge.setAlignment(Pos.CENTER);
        loyaltyBadge.setOpacity(0);
        if (pointsEarned > 0) {
            loyaltyBadge.setStyle("-fx-background-color: rgba(178,102,255,0.08); -fx-padding: 12; -fx-background-radius: 10; -fx-border-color: rgba(178,102,255,0.2); -fx-border-radius: 10; -fx-border-width: 1;");
            Label loyPtsLbl = new Label("\u2B50 +" + pointsEarned + " points de fidelite gagnes !");
            loyPtsLbl.setStyle("-fx-text-fill: #B266FF; -fx-font-size: 13; -fx-font-weight: bold;");
            int newTotal = currentUser != null ? currentUser.getLoyaltyPoints() : pointsEarned;
            Label loyTotalLbl = new Label("Solde total: " + newTotal + " points");
            loyTotalLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
            loyaltyBadge.getChildren().addAll(loyPtsLbl, loyTotalLbl);
        }

        // Email notification
        Label emailNotif = new Label("\uD83D\uDCE7 Un email de confirmation a ete envoye");
        emailNotif.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        emailNotif.setOpacity(0);

        Button closeBtn = new Button("\u2714 Fermer");
        closeBtn.setStyle("-fx-background-color: linear-gradient(to right, #51CF66, #40C057); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 40; -fx-background-radius: 10; -fx-font-size: 14; -fx-cursor: hand;");
        closeBtn.setOpacity(0);

        successCard.getChildren().addAll(checkCircle, successTitle, successDesc, receipt, codeBadge);
        if (pointsEarned > 0) successCard.getChildren().add(loyaltyBadge);
        successCard.getChildren().addAll(emailNotif, closeBtn);
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
        FadeTransition fadeLoyalty = new FadeTransition(Duration.millis(300), loyaltyBadge);
        fadeLoyalty.setToValue(1); fadeLoyalty.setDelay(Duration.millis(700));
        FadeTransition fadeEmail = new FadeTransition(Duration.millis(300), emailNotif);
        fadeEmail.setToValue(1); fadeEmail.setDelay(Duration.millis(800));
        FadeTransition fadeClose = new FadeTransition(Duration.millis(300), closeBtn);
        fadeClose.setToValue(1); fadeClose.setDelay(Duration.millis(900));

        ParallelTransition allAnims = new ParallelTransition(fadeIn, scaleCheck, fadeTitle, fadeDesc, fadeReceipt, fadeCode, fadeLoyalty, fadeEmail, fadeClose);
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
            fadeOut.setOnFinished(ev -> {
                rootPane.getChildren().remove(overlay);
                // FEATURE #15: Gamified Loyalty Wheel after payment
                if (pointsEarned > 0) {
                    showLoyaltyWheelAnimation(pointsEarned);
                }
                // FEATURE #3: AI Personalized Recommendations after payment
                javafx.application.Platform.runLater(() -> showAIRecommendationsDialog(p));
            });
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
    // INNOVATIVE: DUPLICATE PANIER ITEM
    // ================================================================
    private void duplicatePanierItem(Panier p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Dupliquer l'article");
        dialog.setHeaderText(null);
        styleDialog(dialog, "\uD83D\uDCCB", "Dupliquer cet article", "#29B6F6");

        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setAlignment(Pos.CENTER);

        StackPane dupIcon = new StackPane();
        dupIcon.setPrefSize(60, 60);
        dupIcon.setStyle("-fx-background-color: rgba(41,182,246,0.1); -fx-background-radius: 30;");
        Label dIcon = new Label("\uD83D\uDCCB");
        dIcon.setStyle("-fx-font-size: 28;");
        dupIcon.getChildren().add(dIcon);

        Label titleLbl = new Label("Dupliquer cet article ?");
        titleLbl.setStyle("-fx-text-fill: #29B6F6; -fx-font-size: 16; -fx-font-weight: bold;");

        String itemName = p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService();
        Label descLbl = new Label("Un article identique a \"" + itemName + "\" sera ajoute a votre panier.");
        descLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        descLbl.setWrapText(true);

        Label tipLbl = new Label("\uD83D\uDCA1 Utile pour reserver le meme service pour des dates differentes !");
        tipLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 10; -fx-background-color: rgba(255,215,0,0.06); -fx-padding: 8 12; -fx-background-radius: 8;");
        tipLbl.setWrapText(true);

        content.getChildren().addAll(dupIcon, titleLbl, descLbl, tipLbl);
        dp.setContent(content);

        ButtonType dupType = new ButtonType("\uD83D\uDCCB Dupliquer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(dupType, cancelType);

        Button dupBtn = (Button) dp.lookupButton(dupType);
        dupBtn.setStyle("-fx-background-color: #29B6F6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == dupType) {
            Panier dup = new Panier();
            dup.setIdClient(p.getIdClient());
            dup.setIdEtablissement(p.getIdEtablissement());
            dup.setTypeService(p.getTypeService());
            dup.setDateDebut(p.getDateDebut());
            dup.setDateFin(p.getDateFin());
            dup.setNbPersonnes(p.getNbPersonnes());
            dup.setPrixEstime(p.getPrixEstime());
            dup.setStatutItem("en_attente");
            dup.setNbAdultes(p.getNbAdultes());
            dup.setNbEnfants(p.getNbEnfants());
            dup.setNomClient(p.getNomClient());
            dup.setNomEtablissement(p.getNomEtablissement());

            if (panierService.ajouter(dup)) {
                showMessage("\u2705 Article duplique avec succes !", true);
                loadData();
            } else {
                showMessage("\u274C Echec de la duplication.", false);
            }
        }
    }

    // ================================================================
    // INNOVATIVE: SHARE PANIER ITEM
    // ================================================================
    private void sharePanierItem(Panier p) {
        boolean isResto = isRestoOrCafeType(p.getTypeService());
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════\n");
        sb.append("   \uD83D\uDED2 SmartTravel - Panier\n");
        sb.append("═══════════════════════════════\n\n");
        sb.append(getServiceIcon(p.getTypeService())).append(" ").append(p.getNomEtablissement() != null ? p.getNomEtablissement() : "N/A").append("\n");
        sb.append("\uD83C\uDFAF Type: ").append(p.getTypeService() != null ? p.getTypeService() : "N/A").append("\n");
        sb.append("\uD83D\uDCC5 Du ").append(p.getDateDebut() != null ? p.getDateDebut().format(DTF) : "N/A");
        sb.append(" au ").append(p.getDateFin() != null ? p.getDateFin().format(DTF) : "N/A").append("\n");
        sb.append("\uD83D\uDC65 ").append(p.getNbPersonnes()).append(" personne(s) (").append(p.getNbAdultes()).append(" adultes, ").append(p.getNbEnfants()).append(" enfants)\n");
        if (isResto) {
            sb.append("\uD83D\uDCB0 Gratuit (paiement sur place)\n");
        } else {
            sb.append("\uD83D\uDCB0 Prix: ").append(String.format("%.2f DT", p.getPrixEstime())).append("\n");
        }
        sb.append("\n═══════════════════════════════\n");
        sb.append("Partage via SmartTravel \u2764\n");

        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(sb.toString());
        clipboard.setContent(cc);

        showMessage("\uD83D\uDCCB Details copies dans le presse-papiers !", true);
    }

    // ================================================================
    // INNOVATIVE: PRICE COMPARISON DIALOG
    // ================================================================
    private void showPriceComparisonDialog(Panier p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Comparateur de Prix");
        dialog.setHeaderText(null);
        styleDialog(dialog, "\uD83D\uDCCA", "Comparateur de Prix", "#FFD700");

        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(500);

        StackPane compIcon = new StackPane();
        compIcon.setPrefSize(60, 60);
        compIcon.setStyle("-fx-background-color: rgba(255,215,0,0.1); -fx-background-radius: 30;");
        Label cIcon = new Label("\uD83D\uDCCA");
        cIcon.setStyle("-fx-font-size: 28;");
        compIcon.getChildren().add(cIcon);
        HBox iconRow = new HBox();
        iconRow.setAlignment(Pos.CENTER);
        iconRow.getChildren().add(compIcon);

        Label titleLbl = new Label("Analyse de Votre Panier");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER);
        titleRow.getChildren().add(titleLbl);

        // Summary stats
        int totalItems = panierItems.size();
        double totalPrice = panierItems.stream().filter(item -> !isRestoOrCafeType(item.getTypeService())).mapToDouble(Panier::getPrixEstime).sum();
        int totalPeople = panierItems.stream().mapToInt(Panier::getNbPersonnes).sum();
        long restoCount = panierItems.stream().filter(item -> isRestoOrCafeType(item.getTypeService())).count();
        long paidCount = totalItems - restoCount;
        double avgPrice = paidCount > 0 ? totalPrice / paidCount : 0;

        VBox statsGrid = new VBox(10);
        statsGrid.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 16; -fx-background-radius: 12;");

        HBox stat1 = buildComparisonRow("\uD83D\uDED2 Articles total", String.valueOf(totalItems), "#FFD700");
        HBox stat2 = buildComparisonRow("\uD83D\uDCB0 Budget total", String.format("%.2f DT", totalPrice), "#51CF66");
        HBox stat3 = buildComparisonRow("\uD83D\uDCCA Prix moyen", String.format("%.2f DT", avgPrice), "#64B5F6");
        HBox stat4 = buildComparisonRow("\uD83D\uDC65 Total voyageurs", String.valueOf(totalPeople), "#FFA726");
        HBox stat5 = buildComparisonRow("\uD83C\uDF7D Restaurants/Cafes", String.valueOf(restoCount) + " (gratuit)", "#B266FF");

        statsGrid.getChildren().addAll(stat1, stat2, stat3, stat4, stat5);

        // Current item highlight
        VBox currentItem = new VBox(8);
        currentItem.setStyle("-fx-background-color: rgba(255,215,0,0.06); -fx-padding: 14; -fx-background-radius: 10; -fx-border-color: rgba(255,215,0,0.15); -fx-border-radius: 10; -fx-border-width: 1;");
        Label curTitle = new Label("\uD83D\uDC49 Article selectionne");
        curTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");
        String itemName = p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService();
        Label curName = new Label(getServiceIcon(p.getTypeService()) + " " + itemName);
        curName.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");

        boolean isResto = isRestoOrCafeType(p.getTypeService());
        String priceText = isResto ? "Gratuit" : String.format("%.2f DT", p.getPrixEstime());
        String priceColor = isResto ? "#51CF66" : "#FFD700";
        Label curPrice = new Label("Prix: " + priceText);
        curPrice.setStyle("-fx-text-fill: " + priceColor + "; -fx-font-size: 13;");

        if (!isResto && paidCount > 1) {
            double percent = (p.getPrixEstime() / totalPrice) * 100;
            // Visual bar
            HBox barBg = new HBox();
            barBg.setPrefHeight(8);
            barBg.setMaxWidth(Double.MAX_VALUE);
            barBg.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 4;");
            HBox barFill = new HBox();
            barFill.setPrefHeight(8);
            barFill.setMaxWidth(Double.MAX_VALUE);
            barFill.prefWidthProperty().bind(barBg.widthProperty().multiply(percent / 100.0));
            barFill.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-background-radius: 4;");
            StackPane bar = new StackPane();
            bar.getChildren().addAll(barBg, barFill);
            StackPane.setAlignment(barFill, Pos.CENTER_LEFT);

            Label percentLbl = new Label(String.format("%.1f%% du budget total", percent));
            percentLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
            currentItem.getChildren().addAll(curTitle, curName, curPrice, bar, percentLbl);
        } else {
            currentItem.getChildren().addAll(curTitle, curName, curPrice);
        }

        // Savings tip
        VBox tipBox = new VBox(4);
        tipBox.setAlignment(Pos.CENTER);
        tipBox.setStyle("-fx-background-color: rgba(81,207,102,0.06); -fx-padding: 12; -fx-background-radius: 8;");
        int loyaltyPts = currentUser != null ? userService.getLoyaltyPoints(currentUser.getId()) : 0;
        String tip;
        if (loyaltyPts >= 500) {
            tip = "\uD83C\uDF1F Vous avez " + loyaltyPts + " pts ! Utilisez-les pour -20% de reduction !";
        } else if (loyaltyPts >= 200) {
            tip = "\u2B50 " + loyaltyPts + " pts ! Encore " + (500 - loyaltyPts) + " pts pour -20% !";
        } else if (appliedPromo != null) {
            tip = "\uD83C\uDF89 Promo " + appliedPromo + " active ! -" + (int) discountPercent + "% sur vos reservations";
        } else {
            tip = "\uD83D\uDCA1 Astuce: Utilisez un code promo pour economiser !";
        }
        Label tipLbl = new Label(tip);
        tipLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11; -fx-font-weight: bold;");
        tipLbl.setWrapText(true);
        tipBox.getChildren().add(tipLbl);

        content.getChildren().addAll(iconRow, titleRow, statsGrid, currentItem, tipBox);
        dp.setContent(content);
        dp.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.showAndWait();
    }

    private HBox buildComparisonRow(String label, String value, String color) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label leftLbl = new Label(label);
        leftLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label rightLbl = new Label(value);
        rightLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13; -fx-font-weight: bold;");
        row.getChildren().addAll(leftLbl, spacer, rightLbl);
        return row;
    }

    // ================================================================
    // DELETE PANIER ITEM
    // ================================================================
    private void deletePanierItem(Panier p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Supprimer l'article");
        dialog.setHeaderText(null);
        styleDialog(dialog, "\u26A0", "Supprimer cet article", "#FF6B6B");

        DialogPane dp = dialog.getDialogPane();
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
        if (!isRestoOrCafeType(p.getTypeService())) {
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
        } else {
            Label freeInfo = new Label("Reservation gratuite (paiement sur place)");
            freeInfo.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11;");
            infoBox.getChildren().add(freeInfo);
        }

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

    // ================================================================
    // NAVIGATION DRAWER
    // ================================================================
    @FXML
    public void handleToggleDrawer() {
        if (drawerOpen) closeDrawer(); else openDrawer();
    }

    @FXML
    public void handleCloseDrawer() {
        closeDrawer();
    }

    private void openDrawer() {
        drawerOpen = true;
        if (drawerOverlay != null) {
            drawerOverlay.setVisible(true);
            FadeTransition fade = new FadeTransition(Duration.millis(250), drawerOverlay);
            fade.setFromValue(0); fade.setToValue(1); fade.play();
        }
        if (sidebarDrawer != null) {
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), sidebarDrawer);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
            slide.play();
        }
        if (menuToggleBtn != null) menuToggleBtn.setText("\u2715");
    }

    private void closeDrawer() {
        if (!drawerOpen) return;
        drawerOpen = false;
        if (sidebarDrawer != null) {
            TranslateTransition slide = new TranslateTransition(Duration.millis(250), sidebarDrawer);
            slide.setToX(-280);
            slide.setInterpolator(Interpolator.EASE_IN);
            slide.play();
        }
        if (drawerOverlay != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(250), drawerOverlay);
            fade.setToValue(0);
            fade.setOnFinished(e -> drawerOverlay.setVisible(false));
            fade.play();
        }
        if (menuToggleBtn != null) menuToggleBtn.setText("\u2630");
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

        // Insert before window buttons (last HBox)
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
            // Close chat
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

        // Header
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

        // Messages area
        chatMessages = new VBox(8);
        chatMessages.setPadding(new Insets(12));
        ScrollPane msgScroll = new ScrollPane(chatMessages);
        msgScroll.setFitToWidth(true);
        msgScroll.setPrefHeight(340);
        msgScroll.setStyle("-fx-background: " + ThemeManager.bg() + "; -fx-background-color: " + ThemeManager.bg() + "; -fx-border-width: 0;");
        msgScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(msgScroll, Priority.ALWAYS);

        // Welcome message
        addBotMessage("Bonjour! \uD83D\uDC4B Je suis votre assistant Tabaani AI. Je peux vous aider avec:\n\n" +
                "\u2022 Votre panier et reservations\n" +
                "\u2022 Suggestions de destinations\n" +
                "\u2022 Codes promo disponibles\n" +
                "\u2022 Estimation de budget\n\n" +
                "Comment puis-je vous aider ?");

        // Quick actions
        HBox quickActions = new HBox(6);
        quickActions.setPadding(new Insets(8, 12, 4, 12));
        quickActions.setAlignment(Pos.CENTER);
        String[][] actions = {
                {"\uD83D\uDED2 Mon Panier", "Analyse mon panier actuel et donne-moi des suggestions"},
                {"\uD83C\uDF89 Promos", "Quels sont les codes promo disponibles ?"},
                {"\uD83D\uDCB0 Budget", "Aide-moi a estimer mon budget de voyage"}
        };
        for (String[] action : actions) {
            Button qBtn = new Button(action[0]);
            qBtn.setStyle("-fx-background-color: " + ThemeManager.accentHover() + "; -fx-text-fill: " + ThemeManager.accent() + "; " +
                    "-fx-font-size: 9; -fx-padding: 5 10; -fx-background-radius: 12; -fx-cursor: hand; " +
                    "-fx-border-color: " + ThemeManager.accentBorder() + "; -fx-border-radius: 12;");
            qBtn.setOnAction(e -> sendChatMessage(action[1]));
            quickActions.getChildren().add(qBtn);
        }

        // Input area
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
            if (!msg.isEmpty()) {
                sendChatMessage(msg);
                chatInput.clear();
            }
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
        avatar.setMinSize(26, 26);
        avatar.setMaxSize(26, 26);
        avatar.setStyle("-fx-background-color: " + ThemeManager.chatBtnGradient() + "; -fx-background-radius: 13;");
        Label avIcon = new Label("\uD83E\uDD16");
        avIcon.setStyle("-fx-font-size: 10;");
        avatar.getChildren().add(avIcon);

        Label msgLbl = new Label(text);
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(260);
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
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(260);
        msgLbl.setStyle("-fx-background-color: " + ThemeManager.accentGradient() + "; -fx-text-fill: " + (ThemeManager.isDark() ? "#000" : "#fff") + "; " +
                "-fx-font-size: 11; -fx-padding: 10 14; -fx-background-radius: 14 4 14 14; -fx-font-weight: bold;");

        msgRow.getChildren().add(msgLbl);
        if (chatMessages != null) chatMessages.getChildren().add(msgRow);
    }

    private void sendChatMessage(String message) {
        addUserMessage(message);

        // Add typing indicator
        HBox typingRow = new HBox(8);
        typingRow.setAlignment(Pos.TOP_LEFT);
        StackPane tAvatar = new StackPane();
        tAvatar.setMinSize(26, 26);
        tAvatar.setMaxSize(26, 26);
        tAvatar.setStyle("-fx-background-color: " + ThemeManager.chatBtnGradient() + "; -fx-background-radius: 13;");
        Label tIcon = new Label("\uD83E\uDD16");
        tIcon.setStyle("-fx-font-size: 10;");
        tAvatar.getChildren().add(tIcon);
        Label typingLbl = new Label("\u2022\u2022\u2022 En train de reflechir...");
        typingLbl.setStyle("-fx-background-color: " + ThemeManager.accentHover() + "; -fx-text-fill: " + ThemeManager.textSecondary() + "; " +
                "-fx-font-size: 10; -fx-padding: 10 14; -fx-background-radius: 4 14 14 14; -fx-font-style: italic;");
        typingRow.getChildren().addAll(tAvatar, typingLbl);
        if (chatMessages != null) chatMessages.getChildren().add(typingRow);

        // Enrich message with context
        String contextMsg = message;
        if (panierItems != null && !panierItems.isEmpty()) {
            StringBuilder ctx = new StringBuilder(message);
            ctx.append("\n\n[Contexte panier: ");
            ctx.append(panierItems.size()).append(" articles, ");
            double total = panierItems.stream().mapToDouble(Panier::getPrixEstime).sum();
            ctx.append(String.format("total %.2f DT", total));
            if (appliedPromo != null) ctx.append(", promo active: ").append(appliedPromo);
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
    // FEATURE #7: Weather helper
    // ================================================================
    private String guessCity(Panier p) {
        String name = p.getNomEtablissement();
        if (name == null) name = p.getTypeService();
        if (name == null) return "Tunis";
        String lower = name.toLowerCase();
        if (lower.contains("tunis")) return "Tunis";
        if (lower.contains("sousse")) return "Sousse";
        if (lower.contains("sfax")) return "Sfax";
        if (lower.contains("djerba") || lower.contains("jerba")) return "Djerba";
        if (lower.contains("hammamet")) return "Hammamet";
        if (lower.contains("monastir")) return "Monastir";
        if (lower.contains("bizerte")) return "Bizerte";
        if (lower.contains("tozeur")) return "Tozeur";
        if (lower.contains("kairouan")) return "Kairouan";
        if (lower.contains("tabarka")) return "Tabarka";
        // Try to get from etablissement
        try {
            Etablissement etab = etabService.getById(p.getIdEtablissement());
            if (etab != null && etab.getVille() != null) return etab.getVille();
        } catch (Exception ignored) { /* fallback */ }
        return "Tunis";
    }

    // ================================================================
    // FEATURE #8: Currency Converter Dialog
    // ================================================================
    private void showCurrencyConverterDialog(double amountDT) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Convertisseur de devises");
        styleDialog(dialog, "\uD83D\uDCB1", "Convertisseur de Devises", "#51CF66");

        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(400);

        Label title = new Label("\uD83D\uDCB1 Conversion de " + String.format("%.2f DT", amountDT));
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");
        content.getChildren().add(title);

        java.util.Map<String, Double> rates = com.esprit.services.CurrencyService.getRates();
        java.util.Map<String, String> names = com.esprit.services.CurrencyService.getCurrencyNames();
        for (java.util.Map.Entry<String, Double> entry : rates.entrySet()) {
            String code = entry.getKey();
            double converted = com.esprit.services.CurrencyService.convert(amountDT, code);
            String symbol = com.esprit.services.CurrencyService.getCurrencySymbol(code);
            String cname = names.getOrDefault(code, code);

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-padding: 10; -fx-background-radius: 8;");
            Label flagLbl = new Label(getCurrencyFlag(code));
            flagLbl.setStyle("-fx-font-size: 16;");
            VBox infoBox = new VBox(2);
            HBox.setHgrow(infoBox, Priority.ALWAYS);
            Label codeLbl = new Label(code + " - " + cname);
            codeLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10;");
            infoBox.getChildren().add(codeLbl);
            Label valueLbl = new Label(symbol + " " + String.format("%.2f", converted));
            valueLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
            row.getChildren().addAll(flagLbl, infoBox, valueLbl);
            content.getChildren().add(row);
        }

        dp.setContent(content);
        dp.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.showAndWait();
    }

    private String getCurrencyFlag(String code) {
        switch (code) {
            case "EUR": return "\uD83C\uDDEA\uD83C\uDDFA";
            case "USD": return "\uD83C\uDDFA\uD83C\uDDF8";
            case "GBP": return "\uD83C\uDDEC\uD83C\uDDE7";
            case "MAD": return "\uD83C\uDDF2\uD83C\uDDE6";
            case "SAR": return "\uD83C\uDDF8\uD83C\uDDE6";
            case "AED": return "\uD83C\uDDE6\uD83C\uDDEA";
            case "CAD": return "\uD83C\uDDE8\uD83C\uDDE6";
            case "CHF": return "\uD83C\uDDE8\uD83C\uDDED";
            case "TRY": return "\uD83C\uDDF9\uD83C\uDDF7";
            case "DZD": return "\uD83C\uDDE9\uD83C\uDDFF";
            default: return "\uD83D\uDCB0";
        }
    }

    // ================================================================
    // FEATURE #14: Dynamic Pricing Level
    // ================================================================
    private String getDynamicPricingLevel(Panier p) {
        if (p.getDateDebut() == null) return "\uD83D\uDFE2 Prix Normal";
        java.time.DayOfWeek day = p.getDateDebut().getDayOfWeek();
        int month = p.getDateDebut().getMonthValue();
        // High season: June-August, December
        boolean highSeason = month >= 6 && month <= 8 || month == 12;
        // Weekend premium
        boolean weekend = day == java.time.DayOfWeek.FRIDAY || day == java.time.DayOfWeek.SATURDAY;
        if (highSeason && weekend) return "\uD83D\uDD34 Haute Saison + Weekend";
        if (highSeason) return "\uD83D\uDFE0 Haute Saison";
        if (weekend) return "\uD83D\uDFE1 Weekend";
        return "\uD83D\uDFE2 Prix Normal";
    }

    private String getPricingBadgeStyle(String level) {
        if (level.contains("Haute Saison + Weekend"))
            return "-fx-background-color: rgba(255,82,82,0.1); -fx-text-fill: #FF5252; -fx-padding: 3 10; -fx-background-radius: 6; -fx-font-size: 9; -fx-font-weight: bold;";
        if (level.contains("Haute Saison"))
            return "-fx-background-color: rgba(255,167,38,0.1); -fx-text-fill: #FFA726; -fx-padding: 3 10; -fx-background-radius: 6; -fx-font-size: 9; -fx-font-weight: bold;";
        if (level.contains("Weekend"))
            return "-fx-background-color: rgba(255,215,0,0.1); -fx-text-fill: #FFD700; -fx-padding: 3 10; -fx-background-radius: 6; -fx-font-size: 9; -fx-font-weight: bold;";
        return "-fx-background-color: rgba(81,207,102,0.1); -fx-text-fill: #51CF66; -fx-padding: 3 10; -fx-background-radius: 6; -fx-font-size: 9; -fx-font-weight: bold;";
    }

    // Hotel nights helper
    private long getDurationNights(Panier p) {
        if (p.getDateDebut() != null && p.getDateFin() != null) {
            return Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(p.getDateDebut().toLocalDate(), p.getDateFin().toLocalDate()));
        }
        return 1;
    }

    // ================================================================
    // FEATURE #1: AI Smart Trip Planner
    // ================================================================
    private void showAITripPlannerDialog() {
        if (panierItems == null || panierItems.isEmpty()) {
            showMessage("\u2139 Ajoutez des articles au panier d'abord.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("AI Smart Trip Planner");
        styleDialog(dialog, "\uD83E\uDDE0", "AI Smart Trip Planner", "#B266FF");

        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(550);

        Label title = new Label("\uD83E\uDDE0 AI Smart Trip Planner");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");

        Label subtitle = new Label("Notre IA va analyser votre panier et creer un itineraire optimise!");
        subtitle.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        subtitle.setWrapText(true);

        Label resultLabel = new Label("\u23F3 Generation en cours...");
        resultLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");
        resultLabel.setWrapText(true);

        ScrollPane scroll = new ScrollPane(resultLabel);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(350);

        content.getChildren().addAll(title, subtitle, scroll);
        dp.setContent(content);
        dp.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));

        // Build cart context
        StringBuilder cartCtx = new StringBuilder();
        cartCtx.append("Voici les articles de mon panier de voyage en Tunisie:\n");
        for (Panier p : panierItems) {
            cartCtx.append("- ").append(p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService());
            cartCtx.append(" (").append(p.getTypeService()).append(")");
            if (p.getDateDebut() != null) cartCtx.append(" du ").append(p.getDateDebut().format(DTF));
            if (p.getDateFin() != null) cartCtx.append(" au ").append(p.getDateFin().format(DTF));
            cartCtx.append(", ").append(p.getNbPersonnes()).append(" personnes");
            if (p.getNbChambres() > 1) cartCtx.append(", ").append(p.getNbChambres()).append(" chambres");
            cartCtx.append(", ").append(String.format("%.2f DT", p.getPrixEstime()));
            cartCtx.append("\n");
        }
        cartCtx.append("\nCree un itineraire jour par jour optimise avec les meilleures activites, restaurants et conseils pratiques. Reponds en francais.");

        new Thread(() -> {
            String response = chatbotService.chat(cartCtx.toString());
            javafx.application.Platform.runLater(() -> {
                resultLabel.setText(response);
                resultLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
            });
        }).start();

        dialog.showAndWait();
    }

    // ================================================================
    // FEATURE #2: AI Budget Optimizer
    // ================================================================
    private void showAIBudgetOptimizerDialog() {
        if (panierItems == null || panierItems.isEmpty()) {
            showMessage("\u2139 Ajoutez des articles au panier d'abord.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("AI Budget Optimizer");
        styleDialog(dialog, "\uD83D\uDCB0", "AI Budget Optimizer", "#51CF66");

        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(550);

        Label title = new Label("\uD83D\uDCB0 AI Budget Optimizer");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");

        Label resultLabel = new Label("\u23F3 Analyse budgetaire en cours...");
        resultLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");
        resultLabel.setWrapText(true);

        ScrollPane scroll = new ScrollPane(resultLabel);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(350);

        content.getChildren().addAll(title, scroll);
        dp.setContent(content);
        dp.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));

        double totalBudget = panierItems.stream().mapToDouble(Panier::getPrixEstime).sum();
        StringBuilder ctx = new StringBuilder();
        ctx.append("Analyse mon budget de voyage en Tunisie. Budget total actuel: ").append(String.format("%.2f DT", totalBudget)).append("\nArticles:\n");
        for (Panier p : panierItems) {
            ctx.append("- ").append(p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService());
            ctx.append(": ").append(String.format("%.2f DT", p.getPrixEstime()));
            ctx.append(" (").append(p.getNbPersonnes()).append(" pers.)");
            if (p.getNbChambres() > 1) ctx.append(" ").append(p.getNbChambres()).append(" chambres");
            ctx.append("\n");
        }
        ctx.append("\nSuggere des economies, alternatives moins cheres, et des astuces pour optimiser ce budget. Donne un score qualite/prix. Reponds en francais.");

        new Thread(() -> {
            String response = chatbotService.chat(ctx.toString());
            javafx.application.Platform.runLater(() -> {
                resultLabel.setText(response);
                resultLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
            });
        }).start();

        dialog.showAndWait();
    }

    // ================================================================
    // FEATURE #3: AI Personalized Recommendations (called after payment)
    // ================================================================
    private void showAIRecommendationsDialog(Panier paidItem) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Recommendations pour vous");
        styleDialog(dialog, "\u2728", "Recommendations personnalisees", "#FFD700");

        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(500);

        Label title = new Label("\u2728 Recommendations personnalisees");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");

        Label resultLabel = new Label("\u23F3 Notre IA cherche les meilleures recommandations...");
        resultLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");
        resultLabel.setWrapText(true);

        ScrollPane scroll = new ScrollPane(resultLabel);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);

        content.getChildren().addAll(title, scroll);
        dp.setContent(content);
        dp.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));

        String ctx = "Je viens de reserver: " + (paidItem.getNomEtablissement() != null ? paidItem.getNomEtablissement() : paidItem.getTypeService())
                + " (" + paidItem.getTypeService() + ") pour " + paidItem.getNbPersonnes() + " personnes. "
                + "Recommande-moi 5 activites, restaurants ou lieux a visiter a proximite en Tunisie. Reponds en francais avec emojis.";

        new Thread(() -> {
            String response = chatbotService.chat(ctx);
            javafx.application.Platform.runLater(() -> {
                resultLabel.setText(response);
                resultLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
            });
        }).start();

        dialog.showAndWait();
    }

    // ================================================================
    // FEATURE #13: Smart Notification Center
    // ================================================================
    private void showNotificationCenter() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Centre de Notifications");
        styleDialog(dialog, "\uD83D\uDD14", "Centre de Notifications", "#FFA726");

        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1a1a1a;");
        content.setMinWidth(450);

        Label title = new Label("\uD83D\uDD14 Notifications");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");
        content.getChildren().add(title);

        // Generate smart notifications based on cart state
        if (panierItems != null) {
            long enAttente = panierItems.stream().filter(p -> "en_attente".equals(p.getStatutItem())).count();
            if (enAttente > 0) {
                addNotification(content, "\u23F3", "Vous avez " + enAttente + " article(s) en attente de paiement", "#FFA726", "Maintenant");
            }

            // Check for items expiring soon (within 3 days)
            for (Panier p : panierItems) {
                if (p.getDateDebut() != null && p.getDateDebut().toLocalDate().isBefore(LocalDate.now().plusDays(3))) {
                    addNotification(content, "\u26A0", "Reservation proche: " + (p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService()), "#FF6B6B", "Urgent");
                }
            }

            // Promo hint
            if (appliedPromo == null) {
                addNotification(content, "\uD83C\uDF81", "Codes promo disponibles! Essayez SMART10 ou TRAVEL20", "#51CF66", "Astuce");
            }

            // Loyalty points
            if (currentUser != null) {
                int points = currentUser.getLoyaltyPoints();
                if (points >= 100) {
                    addNotification(content, "\u2B50", "Vous avez " + points + " points de fidelite! Echangez-les contre des reductions", "#B266FF", "Points");
                }
            }

            // Weather alert for upcoming trips
            for (Panier p : panierItems) {
                if (p.getDateDebut() != null && p.getDateDebut().toLocalDate().isBefore(LocalDate.now().plusDays(7))) {
                    String city = guessCity(p);
                    com.esprit.services.WeatherService weatherSvc = new com.esprit.services.WeatherService();
                    String alert = weatherSvc.getWeatherAlert(city);
                    if (alert != null && !alert.isEmpty()) {
                        addNotification(content, "\u26C8", alert, "#64B5F6", "Meteo");
                    }
                }
            }
        }

        if (content.getChildren().size() == 1) {
            addNotification(content, "\u2705", "Aucune notification pour le moment", "#51CF66", "Info");
        }

        dp.setContent(content);
        dp.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.showAndWait();
    }

    private void addNotification(VBox container, String icon, String message, String color, String badge) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-padding: 12; -fx-background-radius: 10;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18;");

        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-text-fill: white; -fx-font-size: 11;");
        msgLbl.setWrapText(true);
        textBox.getChildren().add(msgLbl);

        Label badgeLbl = new Label(badge);
        badgeLbl.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + "; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 9; -fx-font-weight: bold;");

        row.getChildren().addAll(iconLbl, textBox, badgeLbl);
        container.getChildren().add(row);
    }

    // ================================================================
    // FEATURE #15: Gamified Loyalty Wheel (called after payment)
    // ================================================================
    private void showLoyaltyWheelAnimation(int pointsEarned) {
        if (titleBar == null || titleBar.getScene() == null) return;
        StackPane rootPane = (StackPane) titleBar.getScene().getRoot();

        VBox wheelPanel = new VBox(16);
        wheelPanel.setAlignment(Pos.CENTER);
        wheelPanel.setMaxWidth(400);
        wheelPanel.setMaxHeight(400);
        wheelPanel.setStyle("-fx-background-color: rgba(20,20,20,0.97); -fx-padding: 30; -fx-background-radius: 20; " +
                "-fx-border-color: rgba(255,215,0,0.3); -fx-border-radius: 20; -fx-border-width: 2;");
        StackPane.setAlignment(wheelPanel, Pos.CENTER);

        Label trophyIcon = new Label("\uD83C\uDFC6");
        trophyIcon.setStyle("-fx-font-size: 50;");

        Label congratsLbl = new Label("Points de Fidelite Gagnes!");
        congratsLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");

        Label pointsLbl = new Label("+" + pointsEarned);
        pointsLbl.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 48; -fx-font-weight: bold;");

        int totalPoints = currentUser != null ? currentUser.getLoyaltyPoints() : 0;
        Label totalLbl = new Label("Total: " + totalPoints + " points");
        totalLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 14;");

        // Next reward info
        String nextReward = "";
        if (totalPoints < 100) nextReward = (100 - totalPoints) + " pts pour debloquer -5% !";
        else if (totalPoints < 200) nextReward = (200 - totalPoints) + " pts pour debloquer -10% !";
        else if (totalPoints < 500) nextReward = (500 - totalPoints) + " pts pour debloquer -20% !";
        else nextReward = "\uD83C\uDF1F Niveau VIP atteint! -20% disponible!";

        Label nextLbl = new Label(nextReward);
        nextLbl.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 11;");

        Button closeBtn = new Button("Super!");
        closeBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8;");
        closeBtn.setOnAction(e -> rootPane.getChildren().remove(wheelPanel));

        wheelPanel.getChildren().addAll(trophyIcon, congratsLbl, pointsLbl, totalLbl, nextLbl, closeBtn);

        // Entrance animation
        wheelPanel.setOpacity(0);
        wheelPanel.setScaleX(0.5);
        wheelPanel.setScaleY(0.5);
        rootPane.getChildren().add(wheelPanel);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), wheelPanel);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), wheelPanel);
        fadeIn.setToValue(1);
        new ParallelTransition(scaleIn, fadeIn).play();

        // Auto-close after 5 seconds
        PauseTransition autoClose = new PauseTransition(Duration.seconds(5));
        autoClose.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), wheelPanel);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> rootPane.getChildren().remove(wheelPanel));
            fadeOut.play();
        });
        autoClose.play();
    }

    // ================================================================
    // AI FEATURE BUTTONS IN HEADER (added to initialize)
    // ================================================================
    private void addAIFeatureButtons() {
        if (titleBar == null) return;

        // Notification bell
        Button notifBtn = new Button("\uD83D\uDD14");
        notifBtn.setStyle("-fx-background-color: rgba(255,167,38,0.12); -fx-text-fill: #FFA726; -fx-font-size: 14; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;");
        notifBtn.setOnAction(e -> showNotificationCenter());
        notifBtn.setTooltip(new Tooltip("Centre de notifications"));

        // AI Trip Planner
        Button tripBtn = new Button("\uD83E\uDDE0");
        tripBtn.setStyle("-fx-background-color: rgba(81,207,102,0.12); -fx-text-fill: #51CF66; -fx-font-size: 14; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;");
        tripBtn.setOnAction(e -> showAITripPlannerDialog());
        tripBtn.setTooltip(new Tooltip("AI Trip Planner"));

        // AI Budget Optimizer
        Button budgetBtn = new Button("\uD83D\uDCB0");
        budgetBtn.setStyle("-fx-background-color: rgba(178,102,255,0.12); -fx-text-fill: #B266FF; -fx-font-size: 14; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;");
        budgetBtn.setOnAction(e -> showAIBudgetOptimizerDialog());
        budgetBtn.setTooltip(new Tooltip("AI Budget Optimizer"));

        // Add before the minimize/close buttons
        int insertIndex = Math.max(0, titleBar.getChildren().size() - 3);
        titleBar.getChildren().addAll(insertIndex, java.util.Arrays.asList(notifBtn, tripBtn, budgetBtn));
    }

    private void showMessage(String msg) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> messageLabel.setText(""));
            pause.play();
        }
    }
}