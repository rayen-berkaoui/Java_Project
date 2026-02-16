package com.esprit.controllers;

import com.esprit.entities.Adresse;
import com.esprit.services.AdresseServices;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdresseController {

    @FXML private Button btnAjouter, btnModifier, btnSupprimer;
    @FXML private Button btnPrev, btnNext;
    @FXML private Label lblCount;
    @FXML private HBox cardsContainer;
    @FXML private VBox emptyState;
    @FXML private HBox pageIndicator;
    @FXML private Label lblPageInfo;
    @FXML private TextField searchField;

    private AdresseServices adresseServices;
    private List<Adresse> adresseList = new ArrayList<>();
    private List<Adresse> filteredList = new ArrayList<>();
    private int currentPage = 0;
    private int selectedIndex = -1;
    private static final int CARDS_PER_PAGE = 4;
    private static final double CARD_WIDTH = 280;
    private static final double CARD_HEIGHT = 300;

    private static final String[] CARD_GRADIENTS = {
            "linear-gradient(to bottom right, #0d1a2a, #061020)",
            "linear-gradient(to bottom right, #1a1a2e, #10102a)",
            "linear-gradient(to bottom right, #1a2a1e, #0d200d)",
            "linear-gradient(to bottom right, #2a1e1a, #20100d)"
    };
    private static final String[] CARD_ICONS = {"📍", "🏠", "🗺️", "🌍", "📌", "🏢"};

    public void initialize() {
        try {
            adresseServices = new AdresseServices();

            if (btnAjouter != null) btnAjouter.setOnAction(e -> { animateButton(btnAjouter); openAddDialog(); });
            if (btnModifier != null) btnModifier.setOnAction(e -> { animateButton(btnModifier); openEditDialog(); });
            if (btnSupprimer != null) btnSupprimer.setOnAction(e -> { animateButton(btnSupprimer); deleteAdresse(); });
            if (btnPrev != null) btnPrev.setOnAction(e -> { animateButton(btnPrev); navigate(-1); });
            if (btnNext != null) btnNext.setOnAction(e -> { animateButton(btnNext); navigate(1); });

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
            System.out.println("✅ AdresseController initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Error initializing AdresseController: " + e.getMessage());
            e.printStackTrace();
            showToast("Erreur d'initialisation: " + e.getMessage(), false);
        }
    }

    private void applyFilter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(adresseList);
        } else {
            String lower = query.toLowerCase().trim();
            filteredList.addAll(adresseList.stream().filter(a -> {
                if (a.getRue() != null && a.getRue().toLowerCase().contains(lower)) return true;
                if (a.getVille() != null && a.getVille().toLowerCase().contains(lower)) return true;
                return false;
            }).collect(Collectors.toList()));
        }
        if (lblCount != null) lblCount.setText(String.valueOf(filteredList.size()));
        buildCards();
    }

    private void loadData() {
        try {
            if (adresseServices == null) { showToast("Service non disponible", false); return; }
            adresseList.clear();
            adresseList.addAll(adresseServices.afficher());
            filteredList.clear();
            filteredList.addAll(adresseList);
            if (lblCount != null) animateCounter(lblCount, adresseList.size());
            int maxPage = getMaxPage();
            if (currentPage > maxPage) currentPage = maxPage;
            if (selectedIndex >= filteredList.size()) selectedIndex = filteredList.isEmpty() ? -1 : 0;
            buildCards();
            System.out.println("✅ Loaded " + adresseList.size() + " addresses");
        } catch (SQLException e) {
            System.err.println("❌ SQL Error: " + e.getMessage());
            showToast("Erreur de chargement: " + e.getMessage(), false);
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
            StackPane card = createCard(filteredList.get(i), i);
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

    private StackPane createCard(Adresse adr, int index) {
        StackPane card = new StackPane();
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        card.getStyleClass().add("adresse-card");

        Rectangle clip = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
        clip.setArcWidth(24); clip.setArcHeight(24);
        card.setClip(clip);

        // Gradient background
        Region bg = new Region();
        bg.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        bg.setStyle("-fx-background-color: " + CARD_GRADIENTS[index % CARD_GRADIENTS.length] + ";");

        // Decorative large icon (watermark style)
        Label watermark = new Label(CARD_ICONS[index % CARD_ICONS.length]);
        watermark.setStyle("-fx-font-size: 80px; -fx-opacity: 0.07;");
        StackPane.setAlignment(watermark, Pos.TOP_RIGHT);
        StackPane.setMargin(watermark, new Insets(-10, -10, 0, 0));

        // Content overlay
        VBox content = new VBox(8);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(22, 20, 20, 20));
        content.setPickOnBounds(false);

        // Top icon
        Label iconLabel = new Label(CARD_ICONS[index % CARD_ICONS.length]);
        iconLabel.setStyle("-fx-font-size: 30px;");

        // Rue (street) as title
        Label rueLabel = new Label(adr.getRue() != null ? adr.getRue() : "—");
        rueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        rueLabel.setWrapText(true);
        rueLabel.setMaxWidth(CARD_WIDTH - 40);

        // Ville (city) as subtitle
        Label villeLabel = new Label("🏙 " + (adr.getVille() != null ? adr.getVille() : "—"));
        villeLabel.setStyle("-fx-text-fill: rgba(191,162,0,0.85); -fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Coordinates section
        VBox coordBox = new VBox(4);
        coordBox.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8; -fx-padding: 10;");
        Label latLabel = new Label(String.format("📐 Lat: %.5f", adr.getLatitude()));
        latLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11px;");
        Label lonLabel = new Label(String.format("📐 Lon: %.5f", adr.getLongitude()));
        lonLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11px;");
        Label altLabel = new Label(String.format("⛰ Alt: %.1f m", adr.getAltitude()));
        altLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11px;");
        coordBox.getChildren().addAll(latLabel, lonLabel, altLabel);

        content.getChildren().addAll(iconLabel, rueLabel, villeLabel, spacer, coordBox);

        card.getChildren().addAll(bg, watermark, content);

        // Selection highlight
        if (index == selectedIndex) {
            card.setStyle("-fx-border-color: #BFA200; -fx-border-width: 2.5; -fx-border-radius: 12; "
                    + "-fx-effect: dropshadow(gaussian, rgba(191,162,0,0.6), 20, 0.7, 0, 0);");
            Rectangle selClip = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
            selClip.setArcWidth(24); selClip.setArcHeight(24);
            card.setClip(selClip);
        }

        card.setOnMouseClicked(e -> {
            selectedIndex = index;
            ScaleTransition pop = new ScaleTransition(Duration.millis(150), card);
            pop.setToX(0.95); pop.setToY(0.95);
            ScaleTransition back = new ScaleTransition(Duration.millis(200), card);
            back.setToX(1.0); back.setToY(1.0); back.setInterpolator(Interpolator.EASE_OUT);
            pop.setOnFinished(ev -> { back.play(); back.setOnFinished(ev2 -> buildCards()); });
            pop.play();
        });

        card.setOnMouseEntered(e -> {
            if (index != selectedIndex) {
                ScaleTransition hover = new ScaleTransition(Duration.millis(200), card);
                hover.setToX(1.04); hover.setToY(1.04); hover.setInterpolator(Interpolator.EASE_OUT); hover.play();
                card.setEffect(new DropShadow(25, Color.rgb(191, 162, 0, 0.4)));
            }
        });
        card.setOnMouseExited(e -> {
            if (index != selectedIndex) {
                ScaleTransition unhover = new ScaleTransition(Duration.millis(200), card);
                unhover.setToX(1.0); unhover.setToY(1.0); unhover.setInterpolator(Interpolator.EASE_OUT); unhover.play();
                card.setEffect(null);
            }
        });

        card.setCursor(Cursor.HAND);
        return card;
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

    private Adresse getSelectedAdresse() {
        if (selectedIndex < 0 || selectedIndex >= filteredList.size()) return null;
        return filteredList.get(selectedIndex);
    }

    // ========== ANIMATIONS ==========

    private void playEntranceAnimation() {
        Node root = cardsContainer.getParent();
        if (root == null) return;
        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(600), root);
        fade.setFromValue(0); fade.setToValue(1); fade.setInterpolator(Interpolator.EASE_OUT); fade.play();
        if (lblCount != null && lblCount.getParent() != null) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(500), lblCount.getParent());
            pulse.setFromX(0.85); pulse.setFromY(0.85); pulse.setToX(1.0); pulse.setToY(1.0);
            pulse.setInterpolator(Interpolator.EASE_OUT); pulse.setDelay(Duration.millis(300)); pulse.play();
        }
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

    // ========== CRUD DIALOGS ==========

    @FXML
    private void openAddDialog() {
        if (adresseServices == null) { showToast("Service non disponible", false); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdresseFormDialog.fxml"));
            DialogPane dialogPane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Ajouter une Adresse");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);
            dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            ((Stage) dialog.getDialogPane().getScene().getWindow()).setMaximized(true);
            AdresseFormDialogController dc = loader.getController();
            dc.setMode("ADD");
            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Adresse newAdresse = dc.getAdresse();
                if (newAdresse != null) { adresseServices.ajouter(newAdresse); loadData(); showToast("Adresse ajoutée avec succès !", true); }
            }
        } catch (IOException e) { showToast("Erreur d'ouverture du formulaire", false);
        } catch (SQLException e) { showToast("Erreur d'ajout: " + e.getMessage(), false); }
    }

    @FXML
    private void openEditDialog() {
        if (adresseServices == null) { showToast("Service non disponible", false); return; }
        Adresse selected = getSelectedAdresse();
        if (selected == null) { showToast("Cliquez sur une carte pour la sélectionner d'abord", false); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdresseFormDialog.fxml"));
            DialogPane dialogPane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Modifier l'Adresse");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);
            dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            ((Stage) dialog.getDialogPane().getScene().getWindow()).setMaximized(true);
            AdresseFormDialogController dc = loader.getController();
            dc.setMode("EDIT"); dc.setAdresse(selected);
            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Adresse updatedAdresse = dc.getAdresse();
                if (updatedAdresse != null) {
                    updatedAdresse.setId_adresse(selected.getId_adresse());
                    adresseServices.modifier(updatedAdresse); loadData(); showToast("Adresse modifiée avec succès !", true);
                }
            }
        } catch (IOException e) { showToast("Erreur d'ouverture du formulaire", false);
        } catch (SQLException e) { showToast("Erreur de modification: " + e.getMessage(), false); }
    }

    @FXML
    private void deleteAdresse() {
        if (adresseServices == null) { showToast("Service non disponible", false); return; }
        Adresse selected = getSelectedAdresse();
        if (selected == null) { showToast("Cliquez sur une carte pour la sélectionner d'abord", false); return; }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation"); confirmAlert.setHeaderText("Supprimer cette adresse ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer l'adresse \"" + selected.getRue() + ", " + selected.getVille() + "\" ?");
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                adresseServices.supprimer(selected.getId_adresse());
                selectedIndex = -1; loadData(); showToast("Adresse supprimée !", true);
            } catch (SQLException e) { showToast("Erreur de suppression: " + e.getMessage(), false); }
        }
    }
}
