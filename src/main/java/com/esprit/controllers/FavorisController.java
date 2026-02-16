package com.esprit.controllers;

import com.esprit.entities.LieuImage;
import com.esprit.entities.LieuTouristique;
import com.esprit.services.FavorisServices;
import com.esprit.services.LieuImageServices;
import com.esprit.services.LieuTouristiqueServices;
import com.esprit.utils.LanguageManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Favorites view - shows all favorited LieuTouristique.
 */
public class FavorisController {

    @FXML private Button btnPrev, btnNext;
    @FXML private Label lblCount;
    @FXML private Label lblHeaderTag, lblHeaderTitle, lblHeaderDesc, lblCountLabel, lblEmptyTitle, lblEmptySubtitle;
    @FXML private HBox cardsContainer;
    @FXML private VBox emptyState;
    @FXML private HBox pageIndicator;
    @FXML private Label lblPageInfo;
    @FXML private TextField searchField;

    private LieuTouristiqueServices lieuServices;
    private LieuImageServices lieuImageServices;
    private FavorisServices favorisServices;
    private List<LieuTouristique> favorisList = new ArrayList<>();
    private List<LieuTouristique> filteredList = new ArrayList<>();
    private final Map<Integer, List<String>> galleryCache = new HashMap<>();
    private final Map<Integer, Integer> carouselIndex = new HashMap<>();
    private int currentPage = 0;
    private static final int CARDS_PER_PAGE = 4;
    private static final double CARD_WIDTH = 280;
    private static final double CARD_HEIGHT = 340;

    public void initialize() {
        try {
            lieuServices = new LieuTouristiqueServices();
            lieuImageServices = new LieuImageServices();
            favorisServices = new FavorisServices();

            if (btnPrev != null) btnPrev.setOnAction(e -> { animateButton(btnPrev); navigate(-1); });
            if (btnNext != null) btnNext.setOnAction(e -> { animateButton(btnNext); navigate(1); });

            if (searchField != null) {
                searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                    currentPage = 0;
                    applyFilter(newVal);
                });
            }

            Platform.runLater(() -> {
                loadData();
                playEntranceAnimation();
                updateLanguageUI();
            });
            LanguageManager.getInstance().addListener(this::updateLanguageUI);
            System.out.println("✅ FavorisController initialized");
        } catch (Exception e) {
            System.err.println("❌ Error initializing FavorisController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateLanguageUI() {
        LanguageManager lm = LanguageManager.getInstance();
        if (lblHeaderTag != null) lblHeaderTag.setText(lm.get("favorites.tag"));
        if (lblHeaderTitle != null) lblHeaderTitle.setText(lm.get("favorites.title"));
        if (lblHeaderDesc != null) lblHeaderDesc.setText(lm.get("favorites.subtitle"));
        if (lblCountLabel != null) lblCountLabel.setText(lm.get("favorites.total"));
        if (lblEmptyTitle != null) lblEmptyTitle.setText(lm.get("favorites.empty"));
        if (lblEmptySubtitle != null) lblEmptySubtitle.setText(lm.get("favorites.emptyHint"));
        if (searchField != null) searchField.setPromptText(lm.get("favorites.search"));
    }

    private void applyFilter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(favorisList);
        } else {
            String lower = query.toLowerCase().trim();
            filteredList.addAll(favorisList.stream().filter(l -> {
                if (l.getNom() != null && l.getNom().toLowerCase().contains(lower)) return true;
                if (l.getVille() != null && l.getVille().toLowerCase().contains(lower)) return true;
                if (l.getDescription() != null && l.getDescription().toLowerCase().contains(lower)) return true;
                return false;
            }).collect(Collectors.toList()));
        }
        if (lblCount != null) lblCount.setText(String.valueOf(filteredList.size()));
        buildCards();
    }

    public void loadData() {
        try {
            if (favorisServices == null || lieuServices == null) return;
            favorisList.clear();
            List<Integer> favIds = favorisServices.getAllFavoriIdsList();
            List<LieuTouristique> allLieux = lieuServices.afficher();
            // Map for quick lookup
            Map<Integer, LieuTouristique> lieuMap = new HashMap<>();
            for (LieuTouristique l : allLieux) lieuMap.put(l.getId_lieu(), l);
            // Preserve favorites order
            for (int id : favIds) {
                LieuTouristique l = lieuMap.get(id);
                if (l != null) favorisList.add(l);
            }
            filteredList.clear();
            filteredList.addAll(favorisList);
            if (lblCount != null) animateCounter(lblCount, favorisList.size());

            // Preload gallery
            galleryCache.clear();
            carouselIndex.clear();
            if (lieuImageServices != null) {
                for (LieuTouristique l : favorisList) {
                    try {
                        List<LieuImage> imgs = lieuImageServices.getByLieuId(l.getId_lieu());
                        List<String> paths = new ArrayList<>();
                        for (LieuImage li : imgs) paths.add(li.getImagePath());
                        if (paths.isEmpty() && l.getImage() != null && !l.getImage().trim().isEmpty()) {
                            paths.add(l.getImage());
                        }
                        galleryCache.put(l.getId_lieu(), paths);
                        carouselIndex.put(l.getId_lieu(), 0);
                    } catch (SQLException ex) {
                        System.err.println("⚠️ Error loading gallery for favori " + l.getId_lieu());
                    }
                }
            }

            int maxPage = getMaxPage();
            if (currentPage > maxPage) currentPage = maxPage;
            buildCards();
        } catch (SQLException e) {
            System.err.println("❌ SQL Error loading favorites: " + e.getMessage());
        }
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
            cardsContainer.setVisible(false); cardsContainer.setManaged(false);
            emptyState.setVisible(true); emptyState.setManaged(true);
            lblPageInfo.setText("");
            if (btnPrev != null) btnPrev.setDisable(true);
            if (btnNext != null) btnNext.setDisable(true);
            updateDots();
            return;
        }

        cardsContainer.setVisible(true); cardsContainer.setManaged(true);
        emptyState.setVisible(false); emptyState.setManaged(false);
        if (btnPrev != null) btnPrev.setDisable(false);
        if (btnNext != null) btnNext.setDisable(false);

        int startIdx = currentPage * CARDS_PER_PAGE;
        int endIdx = Math.min(startIdx + CARDS_PER_PAGE, filteredList.size());

        for (int i = startIdx; i < endIdx; i++) {
            StackPane card = createCard(filteredList.get(i));
            cardsContainer.getChildren().add(card);

            int delay = (i - startIdx) * 120;
            card.setOpacity(0); card.setTranslateY(40); card.setScaleX(0.9); card.setScaleY(0.9);
            FadeTransition fade = new FadeTransition(Duration.millis(400), card);
            fade.setToValue(1.0); fade.setDelay(Duration.millis(delay)); fade.setInterpolator(Interpolator.EASE_OUT);
            TranslateTransition slide = new TranslateTransition(Duration.millis(450), card);
            slide.setToY(0); slide.setDelay(Duration.millis(delay)); slide.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition scale = new ScaleTransition(Duration.millis(400), card);
            scale.setToX(1.0); scale.setToY(1.0); scale.setDelay(Duration.millis(delay)); scale.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fade, slide, scale).play();
        }

        lblPageInfo.setText((currentPage + 1) + " / " + (getMaxPage() + 1));
        updateDots();
    }

    private StackPane createCard(LieuTouristique lieu) {
        StackPane card = new StackPane();
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        card.getStyleClass().add("lieu-card");

        Rectangle clip = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
        clip.setArcWidth(24); clip.setArcHeight(24);
        card.setClip(clip);

        // Background image
        ImageView bgImage = new ImageView();
        bgImage.setFitWidth(CARD_WIDTH);
        bgImage.setFitHeight(CARD_HEIGHT);
        bgImage.setPreserveRatio(false);

        List<String> galleryPaths = galleryCache.getOrDefault(lieu.getId_lieu(), Collections.emptyList());
        if (!galleryPaths.isEmpty()) {
            int curIdx = carouselIndex.getOrDefault(lieu.getId_lieu(), 0);
            if (curIdx >= galleryPaths.size()) curIdx = 0;
            loadImage(bgImage, galleryPaths.get(curIdx));
        } else {
            loadImage(bgImage, lieu.getImage());
        }
        StackPane.setAlignment(bgImage, Pos.CENTER);

        Region darkBg = new Region();
        darkBg.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        darkBg.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a1a2e, #0d0d1a);");

        Region gradientOverlay = new Region();
        gradientOverlay.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        gradientOverlay.setStyle("-fx-background-color: linear-gradient(to bottom, "
                + "transparent 0%, transparent 35%, rgba(0,0,0,0.3) 55%, rgba(0,0,0,0.85) 80%, rgba(0,0,0,0.95) 100%);");

        // Status badge
        Label badge = new Label(lieu.getStatut() == 1 ? "ACTIF" : "INACTIF");
        if (lieu.getStatut() == 1) {
            badge.setStyle("-fx-background-color: #00b36b; -fx-text-fill: white; -fx-font-size: 9px; "
                    + "-fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 4;");
        } else {
            badge.setStyle("-fx-background-color: #e63946; -fx-text-fill: white; -fx-font-size: 9px; "
                    + "-fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 4;");
        }
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(14, 0, 0, 14));

        // Price badge
        Label priceBadge = new Label(String.format("%.0f TND", lieu.getPrix()));
        priceBadge.setStyle("-fx-background-color: rgba(191,162,0,0.9); -fx-text-fill: #0a0a0a; -fx-font-size: 10px; "
                + "-fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 4;");
        StackPane.setAlignment(priceBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priceBadge, new Insets(14, 14, 0, 0));

        // Unfavorite heart button (always red since it's in favorites)
        Label heartBtn = new Label("❤️");
        heartBtn.setStyle("-fx-font-size: 22px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 4, 0, 0, 1);");
        heartBtn.setOnMouseClicked(ev -> {
            ev.consume();
            try {
                favorisServices.removeFavori(lieu.getId_lieu());
                showToast("Retiré des favoris", true);
                // Animate card out then reload
                FadeTransition out = new FadeTransition(Duration.millis(300), card);
                out.setToValue(0);
                ScaleTransition shrink = new ScaleTransition(Duration.millis(300), card);
                shrink.setToX(0.8); shrink.setToY(0.8);
                new ParallelTransition(out, shrink).setOnFinished(e -> loadData());
                new ParallelTransition(out, shrink).play();
                // Small delay then reload
                Timeline reload = new Timeline(new KeyFrame(Duration.millis(350), e -> loadData()));
                reload.play();
            } catch (SQLException ex) {
                showToast("Erreur: " + ex.getMessage(), false);
            }
        });
        heartBtn.setOnMouseEntered(ev -> {
            ScaleTransition h = new ScaleTransition(Duration.millis(150), heartBtn);
            h.setToX(1.2); h.setToY(1.2); h.play();
        });
        heartBtn.setOnMouseExited(ev -> {
            ScaleTransition h = new ScaleTransition(Duration.millis(150), heartBtn);
            h.setToX(1.0); h.setToY(1.0); h.play();
        });
        StackPane.setAlignment(heartBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(heartBtn, new Insets(12, 50, 0, 0));

        // Text overlay
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
        StackPane.setAlignment(textOverlay, Pos.BOTTOM_LEFT);

        card.getChildren().addAll(darkBg, bgImage, gradientOverlay, badge, priceBadge, heartBtn, textOverlay);

        // Horizontal scroll for carousel
        if (galleryPaths.size() > 1) {
            HBox imgDots = new HBox(4);
            imgDots.setAlignment(Pos.CENTER);
            imgDots.setMouseTransparent(true);
            int curImgIdx = carouselIndex.getOrDefault(lieu.getId_lieu(), 0);
            for (int di = 0; di < Math.min(galleryPaths.size(), 8); di++) {
                Circle dot = new Circle(3);
                dot.setFill(di == curImgIdx ? Color.rgb(191, 162, 0) : Color.rgb(255, 255, 255, 0.5));
                imgDots.getChildren().add(dot);
            }
            StackPane.setAlignment(imgDots, Pos.TOP_CENTER);
            StackPane.setMargin(imgDots, new Insets(14, 0, 0, 0));
            card.getChildren().add(imgDots);

            card.setOnScroll(scrollEvent -> {
                double deltaX = scrollEvent.getDeltaX();
                double deltaY = scrollEvent.getDeltaY();
                double delta = Math.abs(deltaX) >= Math.abs(deltaY) ? deltaX : -deltaY;
                if (Math.abs(delta) < 5) return;
                int ci = carouselIndex.getOrDefault(lieu.getId_lieu(), 0);
                ci = delta < 0 ? (ci + 1) % galleryPaths.size() : (ci - 1 + galleryPaths.size()) % galleryPaths.size();
                carouselIndex.put(lieu.getId_lieu(), ci);
                loadImage(bgImage, galleryPaths.get(ci));
                bgImage.setOpacity(0.5);
                FadeTransition imgFade = new FadeTransition(Duration.millis(200), bgImage);
                imgFade.setToValue(1.0); imgFade.setInterpolator(Interpolator.EASE_OUT); imgFade.play();
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

        // Hover effects
        card.setOnMouseEntered(e -> {
            ScaleTransition hover = new ScaleTransition(Duration.millis(200), card);
            hover.setToX(1.04); hover.setToY(1.04); hover.setInterpolator(Interpolator.EASE_OUT); hover.play();
            card.setEffect(new DropShadow(25, Color.rgb(191, 162, 0, 0.4)));
        });
        card.setOnMouseExited(e -> {
            ScaleTransition unhover = new ScaleTransition(Duration.millis(200), card);
            unhover.setToX(1.0); unhover.setToY(1.0); unhover.setInterpolator(Interpolator.EASE_OUT); unhover.play();
            card.setEffect(null);
        });

        card.setCursor(Cursor.HAND);
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
            dot.setCursor(Cursor.HAND);
            dot.setOnMouseClicked(e -> { currentPage = page; buildCards(); });
            pageIndicator.getChildren().add(pageIndicator.getChildren().size() - 1, dot);
        }
    }

    // ========== ANIMATIONS ==========

    private void playEntranceAnimation() {
        Node root = cardsContainer.getParent();
        if (root == null) return;
        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(600), root);
        fade.setFromValue(0); fade.setToValue(1); fade.setInterpolator(Interpolator.EASE_OUT); fade.play();
    }

    private void animateButton(Button btn) {
        ScaleTransition press = new ScaleTransition(Duration.millis(100), btn);
        press.setToX(0.9); press.setToY(0.9);
        ScaleTransition release = new ScaleTransition(Duration.millis(150), btn);
        release.setToX(1.0); release.setToY(1.0); release.setInterpolator(Interpolator.EASE_OUT);
        press.setOnFinished(e -> release.play()); press.play();
    }

    private void animateCounter(Label label, int targetValue) {
        Timeline timeline = new Timeline();
        for (int i = 0; i <= 20; i++) {
            int val = (int) Math.round((double) targetValue * i / 20);
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(i * 30), e -> label.setText(String.valueOf(val))));
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
                toast.setMaxWidth(450); toast.setMaxHeight(50);
                Label icon = new Label(isSuccess ? "✓" : "✕"); icon.getStyleClass().add("toast-icon");
                Label msg = new Label(message); msg.getStyleClass().add("toast-label"); msg.setWrapText(true);
                toast.getChildren().addAll(icon, msg);
                StackPane.setAlignment(toast, Pos.TOP_CENTER);
                StackPane.setMargin(toast, new Insets(20, 0, 0, 0));
                toast.setOpacity(0); toast.setTranslateY(-30);
                overlay.getChildren().add(toast);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(350), toast); fadeIn.setToValue(1.0);
                TranslateTransition slideIn = new TranslateTransition(Duration.millis(350), toast);
                slideIn.setToY(0); slideIn.setInterpolator(Interpolator.EASE_OUT);
                ParallelTransition in = new ParallelTransition(fadeIn, slideIn);
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
                fadeOut.setToValue(0); fadeOut.setDelay(Duration.millis(2500));
                fadeOut.setOnFinished(e -> overlay.getChildren().remove(toast));
                in.setOnFinished(e -> fadeOut.play()); in.play();
                return;
            }
        } catch (Exception ignored) {}
        Alert alert = new Alert(isSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle(isSuccess ? "Succès" : "Erreur"); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}
