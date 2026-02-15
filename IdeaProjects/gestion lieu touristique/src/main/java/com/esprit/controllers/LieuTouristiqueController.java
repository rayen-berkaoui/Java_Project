package com.esprit.controllers;

import com.esprit.entities.LieuTouristique;
import com.esprit.services.LieuTouristiqueServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LieuTouristiqueController {

    @FXML private Button btnAjouter, btnModifier, btnSupprimer;
    @FXML private Button btnPrev, btnNext;
    @FXML private Label lblCount;
    @FXML private HBox cardsContainer;
    @FXML private VBox emptyState;
    @FXML private HBox pageIndicator;
    @FXML private Label lblPageInfo;

    private LieuTouristiqueServices lieuServices;
    private List<LieuTouristique> lieuList = new ArrayList<>();
    private int currentPage = 0;
    private int selectedIndex = -1;
    private static final int CARDS_PER_PAGE = 4;
    private static final double CARD_WIDTH = 280;
    private static final double CARD_HEIGHT = 340;

    public void initialize() {
        try {
            lieuServices = new LieuTouristiqueServices();

            if (btnAjouter != null) btnAjouter.setOnAction(e -> openAddDialog());
            if (btnModifier != null) btnModifier.setOnAction(e -> openEditDialog());
            if (btnSupprimer != null) btnSupprimer.setOnAction(e -> deleteLieu());
            if (btnPrev != null) btnPrev.setOnAction(e -> navigate(-1));
            if (btnNext != null) btnNext.setOnAction(e -> navigate(1));

            Platform.runLater(this::loadData);
            System.out.println("\u2705 LieuTouristiqueController initialized successfully");
        } catch (Exception e) {
            System.err.println("\u274c Error initializing LieuTouristiqueController: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Initialization Error", "Failed to initialize: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            if (lieuServices == null) {
                showErrorAlert("Error", "Database service not available.");
                return;
            }
            lieuList.clear();
            lieuList.addAll(lieuServices.afficher());
            if (lblCount != null) lblCount.setText(String.valueOf(lieuList.size()));

            int maxPage = getMaxPage();
            if (currentPage > maxPage) currentPage = maxPage;
            if (selectedIndex >= lieuList.size()) selectedIndex = lieuList.isEmpty() ? -1 : 0;

            buildCards();
            System.out.println("\u2705 Loaded " + lieuList.size() + " tourist locations");
        } catch (SQLException e) {
            System.err.println("\u274c SQL Error: " + e.getMessage());
            showErrorAlert("Database Error", "Error loading locations: " + e.getMessage());
        }
    }

    private int getMaxPage() {
        if (lieuList.isEmpty()) return 0;
        return (lieuList.size() - 1) / CARDS_PER_PAGE;
    }

    private void navigate(int direction) {
        if (lieuList.isEmpty()) return;
        currentPage += direction;
        int maxPage = getMaxPage();
        if (currentPage < 0) currentPage = maxPage;
        if (currentPage > maxPage) currentPage = 0;
        buildCards();
    }

    private void buildCards() {
        cardsContainer.getChildren().clear();

        if (lieuList.isEmpty()) {
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
        int endIdx = Math.min(startIdx + CARDS_PER_PAGE, lieuList.size());

        for (int i = startIdx; i < endIdx; i++) {
            StackPane card = createCard(lieuList.get(i), i);
            cardsContainer.getChildren().add(card);
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

        // Background image
        ImageView bgImage = new ImageView();
        bgImage.setFitWidth(CARD_WIDTH);
        bgImage.setFitHeight(CARD_HEIGHT);
        bgImage.setPreserveRatio(false);
        loadImage(bgImage, lieu.getImage());
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
        priceBadge.setStyle("-fx-background-color: rgba(255,215,0,0.9); -fx-text-fill: #0a0a0a; -fx-font-size: 10px; "
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
        StackPane.setAlignment(textOverlay, Pos.BOTTOM_LEFT);

        card.getChildren().addAll(darkBg, bgImage, gradientOverlay, badge, priceBadge, textOverlay);

        // Selection highlight
        if (index == selectedIndex) {
            card.setStyle("-fx-border-color: #FFD700; -fx-border-width: 2.5; -fx-border-radius: 12; "
                    + "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.6), 20, 0.7, 0, 0);");
            // Re-clip because border changes rendering
            Rectangle selClip = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
            selClip.setArcWidth(24);
            selClip.setArcHeight(24);
            card.setClip(selClip);
        }

        // Click handler — select this card
        card.setOnMouseClicked(e -> {
            selectedIndex = index;
            buildCards(); // Rebuild to show selection
        });

        // Hover effect
        card.setOnMouseEntered(e -> {
            if (index != selectedIndex) {
                card.setScaleX(1.04);
                card.setScaleY(1.04);
                card.setEffect(new DropShadow(25, Color.rgb(255, 215, 0, 0.4)));
            }
        });
        card.setOnMouseExited(e -> {
            if (index != selectedIndex) {
                card.setScaleX(1.0);
                card.setScaleY(1.0);
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
            dot.setStyle(i == currentPage ? "-fx-fill: #FFD700;" : "-fx-fill: rgba(255,215,0,0.25);");
            pageIndicator.getChildren().add(pageIndicator.getChildren().size() - 1, dot);
        }
    }

    private LieuTouristique getSelectedLieu() {
        if (selectedIndex < 0 || selectedIndex >= lieuList.size()) return null;
        return lieuList.get(selectedIndex);
    }

    @FXML
    private void openAddDialog() {
        if (lieuServices == null) { showErrorAlert("Error", "Database service not available"); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LieuTouristiqueFormDialog.fxml"));
            DialogPane dialogPane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Add Tourist Location");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMaximized(true);

            LieuTouristiqueFormDialogController dc = loader.getController();
            dc.setMode("ADD");

            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                LieuTouristique newLieu = dc.getLieuTouristique();
                if (newLieu != null) {
                    lieuServices.ajouter(newLieu);
                    loadData();
                    showSuccessAlert("Success", "Location added successfully");
                }
            }
        } catch (IOException e) {
            showErrorAlert("Error", "Error opening form: " + e.getMessage());
        } catch (SQLException e) {
            showErrorAlert("Error", "Error adding location: " + e.getMessage());
        }
    }

    @FXML
    private void openEditDialog() {
        if (lieuServices == null) { showErrorAlert("Error", "Database service not available"); return; }
        LieuTouristique selected = getSelectedLieu();
        if (selected == null) { showErrorAlert("Error", "Cliquez sur une carte pour la sélectionner d'abord"); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LieuTouristiqueFormDialog.fxml"));
            DialogPane dialogPane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Edit Tourist Location");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMaximized(true);

            LieuTouristiqueFormDialogController dc = loader.getController();
            dc.setMode("EDIT");
            dc.setLieuTouristique(selected);

            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                LieuTouristique updatedLieu = dc.getLieuTouristique();
                if (updatedLieu != null) {
                    updatedLieu.setId_lieu(selected.getId_lieu());
                    lieuServices.modifier(updatedLieu);
                    loadData();
                    showSuccessAlert("Success", "Location updated successfully");
                }
            }
        } catch (IOException e) {
            showErrorAlert("Error", "Error opening form: " + e.getMessage());
        } catch (SQLException e) {
            showErrorAlert("Error", "Error updating location: " + e.getMessage());
        }
    }

    @FXML
    private void deleteLieu() {
        if (lieuServices == null) { showErrorAlert("Error", "Database service not available"); return; }
        LieuTouristique selected = getSelectedLieu();
        if (selected == null) { showErrorAlert("Error", "Cliquez sur une carte pour la sélectionner d'abord"); return; }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Supprimer ce lieu ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + selected.getNom() + "\" ?");
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                lieuServices.supprimer(selected.getId_lieu());
                selectedIndex = -1;
                loadData();
                showSuccessAlert("Success", "Location deleted successfully");
            } catch (SQLException e) {
                showErrorAlert("Error", "Error deleting location: " + e.getMessage());
            }
        }
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}
