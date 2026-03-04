package com.esprit.controllers;

import com.esprit.entities.categorie;
import com.esprit.services.categorieServices;
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
import javafx.geometry.Rectangle2D;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CategorieController {

    @FXML private Button btnAjouter, btnModifier, btnSupprimer;
    @FXML private Button btnPrev, btnNext;
    @FXML private Label lblCount;
    @FXML private HBox cardsContainer;
    @FXML private VBox emptyState;
    @FXML private HBox pageIndicator;
    @FXML private Label lblPageInfo;
    @FXML private TextField searchField;

    private categorieServices catServices;
    private List<categorie> catList = new ArrayList<>();
    private List<categorie> filteredList = new ArrayList<>();
    private int currentPage = 0;
    private int selectedIndex = -1;
    private static final int CARDS_PER_PAGE = 4;
    private static final double CARD_WIDTH = 280;
    private static final double CARD_HEIGHT = 300;

    private static final String[] CARD_GRADIENTS = {
            "rgba(15,15,15,0.95)",
            "rgba(15,15,15,0.95)",
            "rgba(15,15,15,0.95)",
            "rgba(15,15,15,0.95)"
    };
    private static final String[] CARD_ICONS = {"📁", "📋", "🏷️", "📦", "🗂️", "📑"};

    public void initialize() {
        try {
            catServices = new categorieServices();

            if (btnAjouter != null) btnAjouter.setOnAction(e -> { animateButton(btnAjouter); openAddDialog(); });
            if (btnModifier != null) btnModifier.setOnAction(e -> { animateButton(btnModifier); openEditDialog(); });
            if (btnSupprimer != null) btnSupprimer.setOnAction(e -> { animateButton(btnSupprimer); deleteCategorie(); });
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
            System.out.println("✅ CategorieController initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Error initializing CategorieController: " + e.getMessage());
            e.printStackTrace();
            showToast("Erreur d'initialisation: " + e.getMessage(), false);
        }
    }

    private void applyFilter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(catList);
        } else {
            String lower = query.toLowerCase().trim();
            filteredList.addAll(catList.stream().filter(c -> {
                if (c.getNomcategorie() != null && c.getNomcategorie().toLowerCase().contains(lower)) return true;
                if (c.getDescription() != null && c.getDescription().toLowerCase().contains(lower)) return true;
                return false;
            }).collect(Collectors.toList()));
        }
        if (lblCount != null) lblCount.setText(String.valueOf(filteredList.size()));
        buildCards();
    }

    private void loadData() {
        try {
            if (catServices == null) { showToast("Service non disponible", false); return; }
            catList.clear();
            catList.addAll(catServices.afficher());
            filteredList.clear();
            filteredList.addAll(catList);
            if (lblCount != null) animateCounter(lblCount, catList.size());
            int maxPage = getMaxPage();
            if (currentPage > maxPage) currentPage = maxPage;
            if (selectedIndex >= filteredList.size()) selectedIndex = filteredList.isEmpty() ? -1 : 0;
            buildCards();
            System.out.println("✅ Loaded " + catList.size() + " categories");
        } catch (Exception e) {
            System.err.println("❌ CategorieController.loadData() Error: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
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

        // Ensure cards container has enough height for cards
        cardsContainer.setMinHeight(CARD_HEIGHT + 20);
        cardsContainer.setPrefHeight(CARD_HEIGHT + 20);

        int startIdx = currentPage * CARDS_PER_PAGE;
        int endIdx = Math.min(startIdx + CARDS_PER_PAGE, filteredList.size());

        for (int i = startIdx; i < endIdx; i++) {
            StackPane card = createCard(filteredList.get(i), i);
            cardsContainer.getChildren().add(card);

            // Cards start fully visible — no opacity animation on initial build
            card.setOpacity(1.0);
            card.setTranslateY(0);
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        }

        lblPageInfo.setText((currentPage + 1) + " / " + (getMaxPage() + 1));
        updateDots();
    }

    private StackPane createCard(categorie cat, int index) {
        StackPane card = new StackPane();
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        card.getStyleClass().add("categorie-card");

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
        VBox content = new VBox(10);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(24, 22, 22, 22));
        content.setPickOnBounds(false);

        // Top icon
        Label iconLabel = new Label(CARD_ICONS[index % CARD_ICONS.length]);
        iconLabel.setStyle("-fx-font-size: 32px;");

        // Name
        Label nameLabel = new Label(cat.getNomcategorie() != null ? cat.getNomcategorie() : "Sans nom");
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(CARD_WIDTH - 44);

        // Description
        String desc = cat.getDescription();
        if (desc != null && desc.length() > 80) desc = desc.substring(0, 77) + "...";
        Label descLabel = new Label(desc != null ? desc : "Aucune description");
        descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 12px;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(CARD_WIDTH - 44);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Date at bottom
        String dateStr = cat.getDateCreation() != null
                ? cat.getDateCreation().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "—";
        Label dateLabel = new Label("📅 " + dateStr);
        dateLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11px;");

        content.getChildren().addAll(iconLabel, nameLabel, descLabel, spacer, dateLabel);

        card.getChildren().addAll(bg, watermark, content);

        // Selection highlight
        if (index == selectedIndex) {
            card.setStyle("-fx-border-color: #FFD700; -fx-border-width: 2.5; -fx-border-radius: 12; "
                    + "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.6), 20, 0.7, 0, 0);");
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
                card.setEffect(new DropShadow(25, Color.rgb(255, 215, 0, 0.4)));
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
            dot.setStyle(i == currentPage ? "-fx-fill: #FFD700;" : "-fx-fill: rgba(255,215,0,0.25);");
            final int page = i;
            dot.setCursor(Cursor.HAND);
            dot.setOnMouseClicked(e -> { currentPage = page; buildCards(); });
            pageIndicator.getChildren().add(pageIndicator.getChildren().size() - 1, dot);
        }
    }

    private categorie getSelectedCategorie() {
        if (selectedIndex < 0 || selectedIndex >= filteredList.size()) return null;
        return filteredList.get(selectedIndex);
    }

    // ========== ANIMATIONS ==========

    private void playEntranceAnimation() {
        // Safe entrance: don't set parent opacity to 0 (causes invisible cards in non-active tabs)
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
        if (catServices == null) { showToast("Service non disponible", false); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CategorieFormDialog.fxml"));
            DialogPane dialogPane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Ajouter une Catégorie");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);
            dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            Rectangle2D sb = Screen.getPrimary().getVisualBounds();
            dialogStage.setWidth(Math.min(620, sb.getWidth() * 0.42));
            dialogStage.setHeight(Math.min(560, sb.getHeight() * 0.55));
            dialogStage.setX(sb.getMinX() + (sb.getWidth() - dialogStage.getWidth()) / 2);
            dialogStage.setY(sb.getMinY() + (sb.getHeight() - dialogStage.getHeight()) / 2);
            CategorieFormDialogController dc = loader.getController();
            dc.setMode("ADD");
            setupFullscreenButton(dialogPane, dialogStage);
            // Prevent dialog from closing when validation fails
            dialogPane.lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
                if (dc.getCategorie() == null) event.consume();
            });
            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                categorie newCat = dc.getCategorie();
                if (newCat != null) { catServices.ajouter(newCat); loadData(); showToast("Catégorie ajoutée avec succès !", true); }
            }
        } catch (IOException e) { showToast("Erreur d'ouverture du formulaire", false);
        } catch (SQLException e) { showToast("Erreur d'ajout: " + e.getMessage(), false); }
    }

    @FXML
    private void openEditDialog() {
        if (catServices == null) { showToast("Service non disponible", false); return; }
        categorie selected = getSelectedCategorie();
        if (selected == null) { showToast("Cliquez sur une carte pour la sélectionner d'abord", false); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CategorieFormDialog.fxml"));
            DialogPane dialogPane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Modifier la Catégorie");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);
            dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            Rectangle2D sb = Screen.getPrimary().getVisualBounds();
            dialogStage.setWidth(Math.min(620, sb.getWidth() * 0.42));
            dialogStage.setHeight(Math.min(560, sb.getHeight() * 0.55));
            dialogStage.setX(sb.getMinX() + (sb.getWidth() - dialogStage.getWidth()) / 2);
            dialogStage.setY(sb.getMinY() + (sb.getHeight() - dialogStage.getHeight()) / 2);
            CategorieFormDialogController dc = loader.getController();
            dc.setMode("EDIT"); dc.setCategorie(selected);
            setupFullscreenButton(dialogPane, dialogStage);
            // Prevent dialog from closing when validation fails
            dialogPane.lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
                if (dc.getCategorie() == null) event.consume();
            });
            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                categorie updatedCat = dc.getCategorie();
                if (updatedCat != null) {
                    updatedCat.setIdcategorie(selected.getIdcategorie());
                    catServices.modifier(updatedCat); loadData(); showToast("Catégorie modifiée avec succès !", true);
                }
            }
        } catch (IOException e) { showToast("Erreur d'ouverture du formulaire", false);
        } catch (SQLException e) { showToast("Erreur de modification: " + e.getMessage(), false); }
    }

    @FXML
    private void deleteCategorie() {
        if (catServices == null) { showToast("Service non disponible", false); return; }
        categorie selected = getSelectedCategorie();
        if (selected == null) { showToast("Cliquez sur une carte pour la sélectionner d'abord", false); return; }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation"); confirmAlert.setHeaderText("Supprimer cette catégorie ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + selected.getNomcategorie() + "\" ?");
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                catServices.supprimer(selected.getIdcategorie());
                selectedIndex = -1; loadData(); showToast("Catégorie supprimée !", true);
            } catch (SQLException e) { showToast("Erreur de suppression: " + e.getMessage(), false); }
        }
    }
}
