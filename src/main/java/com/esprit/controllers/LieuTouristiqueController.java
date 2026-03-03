package com.esprit.controllers;

import com.esprit.entities.Adresse;
import com.esprit.entities.LieuTouristique;
import com.esprit.services.AdresseServices;
import com.esprit.services.LieuTouristiqueServices;
import com.esprit.services.QRCodeService;
import com.esprit.services.WeatherService;
import com.esprit.services.PdfExportService;
import com.esprit.services.ExcelExportService;
import com.esprit.entities.categorie;
import com.esprit.services.categorieServices;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Rectangle2D;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.util.Duration;

import javafx.scene.Scene;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class LieuTouristiqueController {

    @FXML private Button btnAjouter, btnModifier, btnSupprimer;
    @FXML private Button btnPrev, btnNext;
    @FXML private Label lblCount;
    @FXML private HBox cardsContainer;
    @FXML private VBox emptyState;
    @FXML private HBox pageIndicator;
    @FXML private Label lblPageInfo;
    @FXML private TextField searchField;

    private LieuTouristiqueServices lieuServices;
    private AdresseServices adresseServices;
    private WeatherService weatherService;
    private List<LieuTouristique> lieuList = new ArrayList<>();
    private List<LieuTouristique> filteredList = new ArrayList<>();
    /** Cache of gallery images per lieu id for carousel */
    private final Map<Integer, List<String>> galleryCache = new HashMap<>();
    /** Tracks current carousel image index per lieu id */
    private final Map<Integer, Integer> carouselIndex = new HashMap<>();
    /** Cache of weather data per city name */
    private final Map<String, WeatherService.WeatherData> weatherCache = new HashMap<>();
    /** Cache of Adresse per id_adresse */
    private final Map<Integer, Adresse> adresseCache = new HashMap<>();
    private int currentPage = 0;
    private int selectedIndex = -1;
    private static final int CARDS_PER_PAGE = 4;
    private static final double CARD_WIDTH = 280;
    private static final double CARD_HEIGHT = 340;

    public void initialize() {
        try {
            lieuServices = new LieuTouristiqueServices();
            adresseServices = new AdresseServices();
            weatherService = new WeatherService();

            if (btnAjouter != null) btnAjouter.setOnAction(e -> { animateButton(btnAjouter); openAddDialog(); });
            if (btnModifier != null) btnModifier.setOnAction(e -> { animateButton(btnModifier); openEditDialog(); });
            if (btnSupprimer != null) btnSupprimer.setOnAction(e -> { animateButton(btnSupprimer); deleteLieu(); });
            if (btnPrev != null) btnPrev.setOnAction(e -> { animateButton(btnPrev); navigate(-1); });
            if (btnNext != null) btnNext.setOnAction(e -> { animateButton(btnNext); navigate(1); });

            // Search listener — filters cards in real time
            if (searchField != null) {
                searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                    currentPage = 0;
                    selectedIndex = -1;
                    applyFilter(newVal);
                });
            }

            Platform.runLater(() -> {
                loadData();
                playEntranceAnimation();
            });
            System.out.println("✅ LieuTouristiqueController initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Error initializing LieuTouristiqueController: " + e.getMessage());
            e.printStackTrace();
            showToast("Erreur d'initialisation: " + e.getMessage(), false);
        }
    }

    private void applyFilter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(lieuList);
        } else {
            String lower = query.toLowerCase().trim();
            filteredList.addAll(lieuList.stream().filter(l -> {
                if (l.getNom() != null && l.getNom().toLowerCase().contains(lower)) return true;
                if (l.getVille() != null && l.getVille().toLowerCase().contains(lower)) return true;
                if (l.getDescription() != null && l.getDescription().toLowerCase().contains(lower)) return true;
                return false;
            }).collect(Collectors.toList()));
        }
        if (lblCount != null) lblCount.setText(String.valueOf(filteredList.size()));
        buildCards();
    }

    private void loadData() {
        try {
            if (lieuServices == null) {
                showToast("Service de base de données non disponible", false);
                return;
            }
            lieuList.clear();
            lieuList.addAll(lieuServices.afficher());
            filteredList.clear();
            filteredList.addAll(lieuList);
            if (lblCount != null) animateCounter(lblCount, lieuList.size());

            // Preload gallery images for carousel (using lieu's image field)
            galleryCache.clear();
            carouselIndex.clear();
            for (LieuTouristique l : lieuList) {
                List<String> paths = new ArrayList<>();
                if (l.getImage() != null && !l.getImage().trim().isEmpty()) {
                    paths.add(l.getImage());
                }
                galleryCache.put(l.getId_lieu(), paths);
                carouselIndex.put(l.getId_lieu(), 0);
            }

            // Preload adresse cache for weather lookup
            adresseCache.clear();
            for (LieuTouristique l : lieuList) {
                if (l.getId_adresse() > 0 && !adresseCache.containsKey(l.getId_adresse())) {
                    try {
                        Adresse adr = adresseServices.getById(l.getId_adresse());
                        if (adr != null) adresseCache.put(l.getId_adresse(), adr);
                    } catch (Exception ignored) {}
                }
            }

            int maxPage = getMaxPage();
            if (currentPage > maxPage) currentPage = maxPage;
            if (selectedIndex >= filteredList.size()) selectedIndex = filteredList.isEmpty() ? -1 : 0;

            buildCards();

            // Fetch weather asynchronously for each unique city from adresse table
            fetchWeatherAsync();

            System.out.println("✅ Loaded " + lieuList.size() + " tourist locations");
        } catch (SQLException e) {
            System.err.println("❌ SQL Error: " + e.getMessage());
            showToast("Erreur de chargement: " + e.getMessage(), false);
        }
    }

    /**
     * Fetches weather data asynchronously for all unique cities from the adresse table.
     * Once fetched, rebuilds cards to show weather info.
     */
    private void fetchWeatherAsync() {
        // Collect unique addresses with coordinates
        Map<String, Adresse> cityAdresses = new HashMap<>();
        for (Adresse adr : adresseCache.values()) {
            if (adr.getVille() != null && !adr.getVille().trim().isEmpty()) {
                cityAdresses.putIfAbsent(adr.getVille().trim().toLowerCase(), adr);
            }
        }

        if (cityAdresses.isEmpty()) return;

        // Run weather fetching in a background thread
        Thread weatherThread = new Thread(() -> {
            for (Map.Entry<String, Adresse> entry : cityAdresses.entrySet()) {
                String cityKey = entry.getKey();
                Adresse adr = entry.getValue();
                if (!weatherCache.containsKey(cityKey)) {
                    WeatherService.WeatherData data = weatherService.getWeather(
                            adr.getLatitude(), adr.getLongitude(), adr.getVille().trim());
                    if (data != null) {
                        weatherCache.put(cityKey, data);
                    }
                }
            }
            // Update UI on JavaFX thread
            Platform.runLater(this::buildCards);
        });
        weatherThread.setDaemon(true);
        weatherThread.setName("weather-fetch");
        weatherThread.start();
    }

    private int getMaxPage() {
        if (filteredList.isEmpty()) return 0;
        return (filteredList.size() - 1) / CARDS_PER_PAGE;
    }

    private void navigate(int direction) {
        if (filteredList.isEmpty()) return;
        currentPage += direction;
        int maxPage = getMaxPage();
        if (currentPage < 0) currentPage = maxPage;
        if (currentPage > maxPage) currentPage = 0;
        buildCards();
    }

    private void buildCards() {
        cardsContainer.getChildren().clear();

        if (filteredList.isEmpty()) {
            cardsContainer.setVisible(false);
            cardsContainer.setManaged(false);
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            lblPageInfo.setText("");
            if (btnPrev != null) btnPrev.setDisable(true);
            if (btnNext != null) btnNext.setDisable(true);
            updateDots();
            return;
        }

        cardsContainer.setVisible(true);
        cardsContainer.setManaged(true);
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        if (btnPrev != null) btnPrev.setDisable(false);
        if (btnNext != null) btnNext.setDisable(false);

        int startIdx = currentPage * CARDS_PER_PAGE;
        int endIdx = Math.min(startIdx + CARDS_PER_PAGE, filteredList.size());

        for (int i = startIdx; i < endIdx; i++) {
            StackPane card = createCard(filteredList.get(i), i);
            cardsContainer.getChildren().add(card);

            // Staggered card entrance animation
            int delay = (i - startIdx) * 120;
            card.setOpacity(0);
            card.setTranslateY(40);
            card.setScaleX(0.9);
            card.setScaleY(0.9);

            FadeTransition fade = new FadeTransition(Duration.millis(400), card);
            fade.setToValue(1.0);
            fade.setDelay(Duration.millis(delay));
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slide = new TranslateTransition(Duration.millis(450), card);
            slide.setToY(0);
            slide.setDelay(Duration.millis(delay));
            slide.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(Duration.millis(400), card);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setDelay(Duration.millis(delay));
            scale.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fade, slide, scale).play();
        }

        int totalPages = getMaxPage() + 1;
        lblPageInfo.setText((currentPage + 1) + " / " + totalPages);
        updateDots();
    }

    private StackPane createCard(LieuTouristique lieu, int index) {
        StackPane card = new StackPane();
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        card.getStyleClass().add("lieu-card");

        // Clip to rounded rectangle
        Rectangle clip = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        card.setClip(clip);

        // Background image (with carousel support)
        ImageView bgImage = new ImageView();
        bgImage.setFitWidth(CARD_WIDTH);
        bgImage.setFitHeight(CARD_HEIGHT);
        bgImage.setPreserveRatio(false);

        // Load from gallery cache or fallback to legacy image
        List<String> galleryPaths = galleryCache.getOrDefault(lieu.getId_lieu(), Collections.emptyList());
        if (!galleryPaths.isEmpty()) {
            int curIdx = carouselIndex.getOrDefault(lieu.getId_lieu(), 0);
            if (curIdx >= galleryPaths.size()) curIdx = 0;
            loadImage(bgImage, galleryPaths.get(curIdx));
        } else {
            loadImage(bgImage, lieu.getImage());
        }
        StackPane.setAlignment(bgImage, Pos.CENTER);

        // Dark fallback background (visible when no image)
        Region darkBg = new Region();
        darkBg.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        darkBg.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a1a2e, #0d0d1a);");

        // Gradient overlay at the bottom for text readability
        Region gradientOverlay = new Region();
        gradientOverlay.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        gradientOverlay.setStyle("-fx-background-color: linear-gradient(to bottom, "
                + "transparent 0%, transparent 35%, rgba(0,0,0,0.3) 55%, rgba(0,0,0,0.85) 80%, rgba(0,0,0,0.95) 100%);");

        // Category badge (top-left)
        Label badge = new Label(lieu.getStatut() == 1 ? "ACTIF" : "INACTIF");
        badge.getStyleClass().add("lieu-badge");
        if (lieu.getStatut() == 1) {
            badge.setStyle("-fx-background-color: #00b36b; -fx-text-fill: white; -fx-font-size: 9px; "
                    + "-fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 4;");
        } else {
            badge.setStyle("-fx-background-color: #e63946; -fx-text-fill: white; -fx-font-size: 9px; "
                    + "-fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 4;");
        }
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(14, 0, 0, 14));

        // Price badge (top-right)
        Label priceBadge = new Label(String.format("%.0f TND", lieu.getPrix()));
        priceBadge.setStyle("-fx-background-color: rgba(191,162,0,0.9); -fx-text-fill: #0a0a0a; -fx-font-size: 10px; "
                + "-fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 4;");
        StackPane.setAlignment(priceBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priceBadge, new Insets(14, 14, 0, 0));

        // Bottom text overlay
        VBox textOverlay = new VBox(4);
        textOverlay.setAlignment(Pos.BOTTOM_LEFT);
        textOverlay.setPadding(new Insets(0, 18, 18, 18));
        textOverlay.setPickOnBounds(false);

        Label nameLabel = new Label(lieu.getNom() != null ? lieu.getNom() : "Sans nom");
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(CARD_WIDTH - 36);

        String desc = lieu.getDescription();
        if (desc != null && desc.length() > 60) desc = desc.substring(0, 57) + "...";
        Label descLabel = new Label(desc != null ? desc : "");
        descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 11px;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(CARD_WIDTH - 36);

        Label locationLabel = new Label("📍 " + (lieu.getVille() != null ? lieu.getVille() : "N/A"));
        locationLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 11px;");

        textOverlay.getChildren().addAll(nameLabel, descLabel, locationLabel);

        // Weather info from adresse city
        Adresse lieuAdresse = adresseCache.get(lieu.getId_adresse());
        String weatherCity = lieuAdresse != null ? lieuAdresse.getVille() : null;
        WeatherService.WeatherData weather = weatherCity != null ? weatherCache.get(weatherCity.trim().toLowerCase()) : null;

        if (weather != null) {
            HBox weatherRow = new HBox(6);
            weatherRow.setAlignment(Pos.CENTER_LEFT);
            weatherRow.setPadding(new Insets(4, 8, 4, 8));
            weatherRow.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 6;");

            Label weatherEmoji = new Label(weather.getWeatherEmoji());
            weatherEmoji.setStyle("-fx-font-size: 14px;");

            Label weatherTemp = new Label(String.format("%.0f°C", weather.getTemperature()));
            weatherTemp.setStyle("-fx-text-fill: #BFA200; -fx-font-size: 12px; -fx-font-weight: bold;");

            Label weatherDesc = new Label(capitalize(weather.getDescription()));
            weatherDesc.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 10px;");
            weatherDesc.setMaxWidth(120);

            Label weatherHumidity = new Label("💧" + weather.getHumidity() + "%");
            weatherHumidity.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 9px;");

            weatherRow.getChildren().addAll(weatherEmoji, weatherTemp, weatherDesc, weatherHumidity);
            textOverlay.getChildren().add(weatherRow);
        }

        StackPane.setAlignment(textOverlay, Pos.BOTTOM_LEFT);

        // QR Code button (bottom-right)
        Button qrBtn = new Button("📱");
        qrBtn.getStyleClass().add("qr-btn");
        qrBtn.setStyle("-fx-background-color: rgba(191,162,0,0.85); -fx-text-fill: #0a0a0a; "
                + "-fx-font-size: 14px; -fx-padding: 4 8; -fx-background-radius: 8; -fx-cursor: hand;");
        qrBtn.setOnMouseEntered(ev -> qrBtn.setStyle("-fx-background-color: rgba(212,181,48,1); -fx-text-fill: #0a0a0a; "
                + "-fx-font-size: 14px; -fx-padding: 4 8; -fx-background-radius: 8; -fx-cursor: hand;"));
        qrBtn.setOnMouseExited(ev -> qrBtn.setStyle("-fx-background-color: rgba(191,162,0,0.85); -fx-text-fill: #0a0a0a; "
                + "-fx-font-size: 14px; -fx-padding: 4 8; -fx-background-radius: 8; -fx-cursor: hand;"));
        qrBtn.setTooltip(new Tooltip("Générer QR Code"));
        qrBtn.setOnAction(ev -> {
            ev.consume();
            showQRCode(lieu);
        });
        StackPane.setAlignment(qrBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(qrBtn, new Insets(0, 14, 18, 0));

        card.getChildren().addAll(darkBg, bgImage, gradientOverlay, badge, priceBadge, textOverlay, qrBtn);

        // Horizontal scroll to cycle images (only when multiple images)
        if (galleryPaths.size() > 1) {
            // Image counter dots at the top
            HBox imgDots = new HBox(4);
            imgDots.setAlignment(Pos.CENTER);
            imgDots.setMouseTransparent(true);
            int curImgIdx = carouselIndex.getOrDefault(lieu.getId_lieu(), 0);
            for (int di = 0; di < Math.min(galleryPaths.size(), 8); di++) {
                Circle dot = new Circle(3);
                dot.setFill(di == curImgIdx ? Color.rgb(191, 162, 0) : Color.rgb(255, 255, 255, 0.5));
                imgDots.getChildren().add(dot);
            }
            if (galleryPaths.size() > 8) {
                Label more = new Label("+" + (galleryPaths.size() - 8));
                more.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 8px;");
                more.setMouseTransparent(true);
                imgDots.getChildren().add(more);
            }
            StackPane.setAlignment(imgDots, Pos.TOP_CENTER);
            StackPane.setMargin(imgDots, new Insets(14, 0, 0, 0));
            card.getChildren().add(imgDots);

            // Horizontal scroll handler — scroll left/right cycles images in-place
            card.setOnScroll(scrollEvent -> {
                double deltaX = scrollEvent.getDeltaX();
                double deltaY = scrollEvent.getDeltaY();
                double delta = Math.abs(deltaX) >= Math.abs(deltaY) ? deltaX : -deltaY;
                if (Math.abs(delta) < 5) return;

                int ci = carouselIndex.getOrDefault(lieu.getId_lieu(), 0);
                if (delta < 0) {
                    ci = (ci + 1) % galleryPaths.size();
                } else {
                    ci = (ci - 1 + galleryPaths.size()) % galleryPaths.size();
                }
                carouselIndex.put(lieu.getId_lieu(), ci);

                // Update only this card's image (no full rebuild)
                loadImage(bgImage, galleryPaths.get(ci));

                // Smooth crossfade
                bgImage.setOpacity(0.5);
                FadeTransition imgFade = new FadeTransition(Duration.millis(200), bgImage);
                imgFade.setToValue(1.0);
                imgFade.setInterpolator(Interpolator.EASE_OUT);
                imgFade.play();

                // Update dots
                int dotCount = Math.min(galleryPaths.size(), 8);
                for (int d = 0; d < dotCount; d++) {
                    Node dotNode = imgDots.getChildren().get(d);
                    if (dotNode instanceof Circle) {
                        ((Circle) dotNode).setFill(d == ci ? Color.rgb(191, 162, 0) : Color.rgb(255, 255, 255, 0.5));
                    }
                }

                scrollEvent.consume();
            });
        }

        // Selection highlight
        if (index == selectedIndex) {
            card.setStyle("-fx-border-color: #BFA200; -fx-border-width: 2.5; -fx-border-radius: 12; "
                    + "-fx-effect: dropshadow(gaussian, rgba(191,162,0,0.6), 20, 0.7, 0, 0);");
            Rectangle selClip = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
            selClip.setArcWidth(24);
            selClip.setArcHeight(24);
            card.setClip(selClip);
        }

        // Click handler — select this card with scale pulse
        card.setOnMouseClicked(e -> {
            selectedIndex = index;
            ScaleTransition pop = new ScaleTransition(Duration.millis(150), card);
            pop.setToX(0.95);
            pop.setToY(0.95);
            ScaleTransition back = new ScaleTransition(Duration.millis(200), card);
            back.setToX(1.0);
            back.setToY(1.0);
            back.setInterpolator(Interpolator.EASE_OUT);
            pop.setOnFinished(ev -> { back.play(); back.setOnFinished(ev2 -> buildCards()); });
            pop.play();
        });

        // Hover effect for all cards
        card.setOnMouseEntered(e -> {
            if (index != selectedIndex) {
                ScaleTransition hover = new ScaleTransition(Duration.millis(200), card);
                hover.setToX(1.04);
                hover.setToY(1.04);
                hover.setInterpolator(Interpolator.EASE_OUT);
                hover.play();
                card.setEffect(new DropShadow(25, Color.rgb(191, 162, 0, 0.4)));
            }
        });
        card.setOnMouseExited(e -> {
            if (index != selectedIndex) {
                ScaleTransition unhover = new ScaleTransition(Duration.millis(200), card);
                unhover.setToX(1.0);
                unhover.setToY(1.0);
                unhover.setInterpolator(Interpolator.EASE_OUT);
                unhover.play();
                card.setEffect(null);
            }
        });

        card.setCursor(javafx.scene.Cursor.HAND);
        return card;
    }

    private void loadImage(ImageView iv, String imagePath) {
        try {
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                File file = new File(imagePath);
                if (file.exists()) {
                    iv.setImage(new Image(file.toURI().toString(), CARD_WIDTH, CARD_HEIGHT, false, true));
                    return;
                }
                if (imagePath.startsWith("http")) {
                    iv.setImage(new Image(imagePath, CARD_WIDTH, CARD_HEIGHT, false, true, true));
                    return;
                }
            }
            iv.setImage(null);
        } catch (Exception e) {
            iv.setImage(null);
        }
    }

    private void updateDots() {
        pageIndicator.getChildren().removeIf(node -> node instanceof Circle);
        int totalPages = getMaxPage() + 1;
        if (totalPages <= 1) return;

        int maxDots = Math.min(totalPages, 10);
        for (int i = 0; i < maxDots; i++) {
            Circle dot = new Circle(5);
            dot.setStyle(i == currentPage ? "-fx-fill: #BFA200;" : "-fx-fill: rgba(191,162,0,0.25);");
            final int page = i;
            dot.setCursor(javafx.scene.Cursor.HAND);
            dot.setOnMouseClicked(e -> {
                currentPage = page;
                buildCards();
            });
            pageIndicator.getChildren().add(pageIndicator.getChildren().size() - 1, dot);
        }
    }

    private LieuTouristique getSelectedLieu() {
        if (selectedIndex < 0 || selectedIndex >= filteredList.size()) return null;
        return filteredList.get(selectedIndex);
    }

    // ========== ANIMATIONS ==========

    private void playEntranceAnimation() {
        Node root = cardsContainer.getParent();
        if (root == null) return;

        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(600), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();

        if (lblCount != null && lblCount.getParent() != null) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(500), lblCount.getParent());
            pulse.setFromX(0.85);
            pulse.setFromY(0.85);
            pulse.setToX(1.0);
            pulse.setToY(1.0);
            pulse.setInterpolator(Interpolator.EASE_OUT);
            pulse.setDelay(Duration.millis(300));
            pulse.play();
        }
    }

    private void animateButton(Button btn) {
        ScaleTransition press = new ScaleTransition(Duration.millis(100), btn);
        press.setToX(0.9);
        press.setToY(0.9);
        ScaleTransition release = new ScaleTransition(Duration.millis(150), btn);
        release.setToX(1.0);
        release.setToY(1.0);
        release.setInterpolator(Interpolator.EASE_OUT);
        press.setOnFinished(e -> release.play());
        press.play();
    }

    private void animateCounter(Label label, int targetValue) {
        Timeline timeline = new Timeline();
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            int val = (int) Math.round((double) targetValue * i / steps);
            KeyFrame kf = new KeyFrame(Duration.millis(i * 30), e -> label.setText(String.valueOf(val)));
            timeline.getKeyFrames().add(kf);
        }
        timeline.play();
    }

    private void showToast(String message, boolean isSuccess) {
        try {
            if (cardsContainer.getScene() == null) return;
            Node sceneRoot = cardsContainer.getScene().getRoot();
            if (sceneRoot instanceof StackPane) {
                StackPane overlay = (StackPane) sceneRoot;
                HBox toast = new HBox(10);
                toast.setAlignment(Pos.CENTER);
                toast.getStyleClass().add(isSuccess ? "toast-success" : "toast-error");
                toast.setMaxWidth(450);
                toast.setMaxHeight(50);

                Label icon = new Label(isSuccess ? "✓" : "✕");
                icon.getStyleClass().add("toast-icon");
                Label msg = new Label(message);
                msg.getStyleClass().add("toast-label");
                msg.setWrapText(true);
                toast.getChildren().addAll(icon, msg);
                StackPane.setAlignment(toast, Pos.TOP_CENTER);
                StackPane.setMargin(toast, new Insets(20, 0, 0, 0));
                toast.setOpacity(0);
                toast.setTranslateY(-30);
                overlay.getChildren().add(toast);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(350), toast);
                fadeIn.setToValue(1.0);
                TranslateTransition slideIn = new TranslateTransition(Duration.millis(350), toast);
                slideIn.setToY(0);
                slideIn.setInterpolator(Interpolator.EASE_OUT);
                ParallelTransition in = new ParallelTransition(fadeIn, slideIn);
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
                fadeOut.setToValue(0);
                fadeOut.setDelay(Duration.millis(2500));
                fadeOut.setOnFinished(e -> overlay.getChildren().remove(toast));
                in.setOnFinished(e -> fadeOut.play());
                in.play();
                return;
            }
        } catch (Exception ignored) {}

        Alert alert = new Alert(isSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle(isSuccess ? "Succès" : "Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupFullscreenButton(DialogPane dialogPane, Stage dialogStage) {
        Button btnFs = (Button) dialogPane.lookup("#btnFullscreen");
        if (btnFs != null) {
            btnFs.setOnAction(e -> {
                boolean maximized = !dialogStage.isMaximized();
                dialogStage.setMaximized(maximized);
                btnFs.setText(maximized ? "\u2750" : "\u26F6");
            });
            dialogStage.maximizedProperty().addListener((obs, o, n) ->
                btnFs.setText(n ? "\u2750" : "\u26F6")
            );
        }
    }

    // ========== CRUD DIALOGS ==========

    @FXML
    private void openAddDialog() {
        if (lieuServices == null) { showToast("Service non disponible", false); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LieuTouristiqueFormDialog.fxml"));
            DialogPane dialogPane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Ajouter un Lieu Touristique");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            Rectangle2D sb = Screen.getPrimary().getVisualBounds();
            dialogStage.setWidth(sb.getWidth() * 0.75);
            dialogStage.setHeight(sb.getHeight() * 0.82);
            dialogStage.setX(sb.getMinX() + (sb.getWidth() - dialogStage.getWidth()) / 2);
            dialogStage.setY(sb.getMinY() + (sb.getHeight() - dialogStage.getHeight()) / 2);

            LieuTouristiqueFormDialogController dc = loader.getController();
            dc.setMode("ADD");
            setupFullscreenButton(dialogPane, dialogStage);
            // Prevent dialog from closing when validation fails
            dialogPane.lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
                if (dc.getLieuTouristique() == null) event.consume();
            });

            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                LieuTouristique newLieu = dc.getLieuTouristique();
                if (newLieu != null) {
                    lieuServices.ajouter(newLieu);
                    loadData();
                    showToast("Lieu touristique ajouté avec succès !", true);
                }
            }
        } catch (IOException e) {
            showToast("Erreur d'ouverture du formulaire", false);
        } catch (SQLException e) {
            showToast("Erreur d'ajout: " + e.getMessage(), false);
        }
    }

    @FXML
    private void openEditDialog() {
        if (lieuServices == null) { showToast("Service non disponible", false); return; }
        LieuTouristique selected = getSelectedLieu();
        if (selected == null) { showToast("Cliquez sur une carte pour la sélectionner d'abord", false); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LieuTouristiqueFormDialog.fxml"));
            DialogPane dialogPane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Modifier le Lieu Touristique");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            Rectangle2D sb = Screen.getPrimary().getVisualBounds();
            dialogStage.setWidth(sb.getWidth() * 0.75);
            dialogStage.setHeight(sb.getHeight() * 0.82);
            dialogStage.setX(sb.getMinX() + (sb.getWidth() - dialogStage.getWidth()) / 2);
            dialogStage.setY(sb.getMinY() + (sb.getHeight() - dialogStage.getHeight()) / 2);

            LieuTouristiqueFormDialogController dc = loader.getController();
            dc.setMode("EDIT");
            setupFullscreenButton(dialogPane, dialogStage);
            dc.setLieuTouristique(selected);

            // Prevent dialog from closing when validation fails
            dialogPane.lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
                if (dc.getLieuTouristique() == null) event.consume();
            });
            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                LieuTouristique updatedLieu = dc.getLieuTouristique();
                if (updatedLieu != null) {
                    updatedLieu.setId_lieu(selected.getId_lieu());
                    lieuServices.modifier(updatedLieu);
                    loadData();
                    showToast("Lieu touristique modifié avec succès !", true);
                }
            }
        } catch (IOException e) {
            showToast("Erreur d'ouverture du formulaire", false);
        } catch (SQLException e) {
            showToast("Erreur de modification: " + e.getMessage(), false);
        }
    }

    @FXML
    private void deleteLieu() {
        if (lieuServices == null) { showToast("Service non disponible", false); return; }
        LieuTouristique selected = getSelectedLieu();
        if (selected == null) { showToast("Cliquez sur une carte pour la sélectionner d'abord", false); return; }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer ce lieu ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + selected.getNom() + "\" ?");
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                lieuServices.supprimer(selected.getId_lieu());
                selectedIndex = -1;
                loadData();
                showToast("Lieu touristique supprimé !", true);
            } catch (SQLException e) {
                showToast("Erreur de suppression: " + e.getMessage(), false);
            }
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    // ========== QR CODE ==========

    private void showQRCode(LieuTouristique lieu) {
        try {
            Adresse adresse = adresseCache.get(lieu.getId_adresse());
            double lat = adresse != null ? adresse.getLatitude() : 0;
            double lon = adresse != null ? adresse.getLongitude() : 0;

            Image qrImage = QRCodeService.generateQRCode(lieu, lat, lon);
            if (qrImage == null) {
                showToast("Erreur de génération du QR Code", false);
                return;
            }

            // Create popup dialog
            Stage qrStage = new Stage();
            qrStage.initModality(Modality.APPLICATION_MODAL);
            qrStage.setTitle("QR Code — " + lieu.getNom());

            ImageView qrView = new ImageView(qrImage);
            qrView.setFitWidth(280);
            qrView.setFitHeight(280);
            qrView.setPreserveRatio(true);

            Label title = new Label("📱 " + lieu.getNom());
            title.setStyle("-fx-text-fill: #BFA200; -fx-font-size: 18px; -fx-font-weight: bold;");

            Label subtitle = new Label("Scannez pour ouvrir dans Google Maps");
            subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");

            Button saveBtn = new Button("💾  Sauvegarder PNG");
            saveBtn.setStyle("-fx-background-color: #BFA200; -fx-text-fill: #0a0a0a; -fx-font-weight: bold; "
                    + "-fx-padding: 8 20; -fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand;");
            saveBtn.setOnAction(ev -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Sauvegarder le QR Code");
                fc.setInitialFileName("QR_" + lieu.getNom().replaceAll("[^a-zA-Z0-9]", "_") + ".png");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
                File file = fc.showSaveDialog(qrStage);
                if (file != null) {
                    QRCodeService.saveQRCode(lieu, lat, lon, file);
                    showToast("QR Code sauvegardé !", true);
                }
            });

            Button closeBtn = new Button("Fermer");
            closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; "
                    + "-fx-padding: 8 20; -fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand;");
            closeBtn.setOnAction(ev -> qrStage.close());

            HBox buttons = new HBox(12, saveBtn, closeBtn);
            buttons.setAlignment(Pos.CENTER);

            VBox layout = new VBox(16, title, subtitle, qrView, buttons);
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(30));
            layout.setStyle("-fx-background-color: linear-gradient(to bottom, #0a0a12, #12121e);");

            Scene scene = new Scene(layout, 380, 460);
            qrStage.setScene(scene);
            qrStage.setResizable(false);
            qrStage.show();

        } catch (Exception e) {
            showToast("Erreur QR Code: " + e.getMessage(), false);
        }
    }

    // ========== PDF / EXCEL EXPORT ==========

    @FXML
    public void exportPdf() {
        if (filteredList.isEmpty()) {
            showToast("Aucun lieu à exporter", false);
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en PDF");
        fc.setInitialFileName("Rapport_Lieux_Touristiques.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        Stage stage = (Stage) cardsContainer.getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        // Show progress toast
        showToast("📄 Génération du PDF en cours...", true);

        new Thread(() -> {
            try {
                categorieServices catServices = new categorieServices();
                java.util.List<categorie> cats = catServices.afficher();
                java.util.List<Adresse> addrs = new java.util.ArrayList<>(adresseCache.values());

                PdfExportService pdfService = new PdfExportService();
                pdfService.generateReport(file, filteredList, cats, addrs);

                Platform.runLater(() ->
                        showToast("✅ PDF exporté: " + file.getName(), true));
            } catch (Exception e) {
                Platform.runLater(() ->
                        showToast("❌ Erreur export PDF: " + e.getMessage(), false));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void exportExcel() {
        if (filteredList.isEmpty()) {
            showToast("Aucun lieu à exporter", false);
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Excel");
        fc.setInitialFileName("Lieux_Touristiques.xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        Stage stage = (Stage) cardsContainer.getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        showToast("📊 Génération de l'Excel en cours...", true);

        new Thread(() -> {
            try {
                categorieServices catServices = new categorieServices();
                java.util.List<categorie> cats = catServices.afficher();
                AdresseServices addrServices = new AdresseServices();
                java.util.List<Adresse> addrs = addrServices.afficher();

                ExcelExportService excelService = new ExcelExportService();
                excelService.exportToExcel(file, filteredList, cats, addrs);

                Platform.runLater(() ->
                        showToast("✅ Excel exporté: " + file.getName(), true));
            } catch (Exception e) {
                Platform.runLater(() ->
                        showToast("❌ Erreur export Excel: " + e.getMessage(), false));
                e.printStackTrace();
            }
        }).start();
    }
}
