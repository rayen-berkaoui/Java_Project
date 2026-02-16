package com.esprit.controllers;

import com.esprit.entities.categorie;
import com.esprit.services.categorieServices;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public class CategorieController {

    @FXML private TableView<categorie> tableCategorie;
    @FXML private TableColumn<categorie, Integer> colId;
    @FXML private TableColumn<categorie, String> colNom, colDescription;
    @FXML private TableColumn<categorie, LocalDate> colDateCreation;
    @FXML private Button btnAjouter, btnModifier, btnSupprimer;
    @FXML private Label lblCount;
    @FXML private TextField searchField;

    private categorieServices catServices;
    private ObservableList<categorie> categorieList;
    private FilteredList<categorie> filteredList;

    public void initialize() {
        try {
            catServices = new categorieServices();
            categorieList = FXCollections.observableArrayList();
            tableCategorie.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Bind columns
            colId.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getIdcategorie()));
            colNom.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNomcategorie()));
            colDescription.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
            colDateCreation.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getDateCreation()));

            // Filtered list for search
            filteredList = new FilteredList<>(categorieList, p -> true);
            tableCategorie.setItems(filteredList);

            // Search listener
            if (searchField != null) {
                searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                    filteredList.setPredicate(cat -> {
                        if (newVal == null || newVal.trim().isEmpty()) return true;
                        String lower = newVal.toLowerCase().trim();
                        if (cat.getNomcategorie() != null && cat.getNomcategorie().toLowerCase().contains(lower)) return true;
                        if (cat.getDescription() != null && cat.getDescription().toLowerCase().contains(lower)) return true;
                        return false;
                    });
                    if (lblCount != null) lblCount.setText(String.valueOf(filteredList.size()));
                });
            }

            // Button handlers
            if (btnAjouter != null) btnAjouter.setOnAction(e -> { animateButton(btnAjouter); openAddDialog(); });
            if (btnModifier != null) btnModifier.setOnAction(e -> { animateButton(btnModifier); openEditDialog(); });
            if (btnSupprimer != null) btnSupprimer.setOnAction(e -> { animateButton(btnSupprimer); deleteCategorie(); });

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

    private void loadData() {
        try {
            if (catServices == null) {
                showToast("Service de base de données non disponible", false);
                return;
            }
            categorieList.clear();
            categorieList.addAll(catServices.afficher());
            if (lblCount != null) animateCounter(lblCount, categorieList.size());
            System.out.println("✅ Loaded " + categorieList.size() + " categories");
        } catch (SQLException e) {
            System.err.println("❌ SQL Error: " + e.getMessage());
            showToast("Erreur de chargement: " + e.getMessage(), false);
        }
    }

    // ========== ANIMATIONS ==========

    private void playEntranceAnimation() {
        Node root = tableCategorie.getScene() != null ? tableCategorie.getParent() : null;
        if (root == null) return;

        // Fade in the entire view
        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(600), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();

        // Slide in the table from bottom
        tableCategorie.setTranslateY(30);
        TranslateTransition slide = new TranslateTransition(Duration.millis(700), tableCategorie);
        slide.setFromY(30);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        slide.setDelay(Duration.millis(200));
        slide.play();

        // Pulse the stat card
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
            if (tableCategorie.getScene() == null) return;
            Node sceneRoot = tableCategorie.getScene().getRoot();
            StackPane overlay = null;
            if (sceneRoot instanceof StackPane) {
                overlay = (StackPane) sceneRoot;
            } else if (sceneRoot instanceof javafx.scene.layout.BorderPane) {
                // Wrap in StackPane approach — just use Alert as fallback
                Alert alert = new Alert(isSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                alert.setTitle(isSuccess ? "Succès" : "Erreur");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
                return;
            } else {
                Alert alert = new Alert(isSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                alert.setTitle(isSuccess ? "Succès" : "Erreur");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
                return;
            }

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

            // Animate in
            FadeTransition fadeIn = new FadeTransition(Duration.millis(350), toast);
            fadeIn.setToValue(1.0);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(350), toast);
            slideIn.setToY(0);
            slideIn.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition in = new ParallelTransition(fadeIn, slideIn);

            // Animate out after delay
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
            fadeOut.setToValue(0);
            fadeOut.setDelay(Duration.millis(2500));
            StackPane finalOverlay = overlay;
            fadeOut.setOnFinished(e -> finalOverlay.getChildren().remove(toast));

            in.setOnFinished(e -> fadeOut.play());
            in.play();
        } catch (Exception e) {
            // Fallback to plain alert
            Alert alert = new Alert(isSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            alert.setTitle(isSuccess ? "Succès" : "Erreur");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
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
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMaximized(true);

            CategorieFormDialogController dc = loader.getController();
            dc.setMode("ADD");

            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                categorie newCat = dc.getCategorie();
                if (newCat != null) {
                    catServices.ajouter(newCat);
                    loadData();
                    showToast("Catégorie ajoutée avec succès !", true);
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
        if (catServices == null) { showToast("Service non disponible", false); return; }
        categorie selected = tableCategorie.getSelectionModel().getSelectedItem();
        if (selected == null) { showToast("Veuillez sélectionner une catégorie", false); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CategorieFormDialog.fxml"));
            DialogPane dialogPane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Modifier la Catégorie");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMaximized(true);

            CategorieFormDialogController dc = loader.getController();
            dc.setMode("EDIT");
            dc.setCategorie(selected);

            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                categorie updatedCat = dc.getCategorie();
                if (updatedCat != null) {
                    updatedCat.setIdcategorie(selected.getIdcategorie());
                    catServices.modifier(updatedCat);
                    loadData();
                    showToast("Catégorie modifiée avec succès !", true);
                }
            }
        } catch (IOException e) {
            showToast("Erreur d'ouverture du formulaire", false);
        } catch (SQLException e) {
            showToast("Erreur de modification: " + e.getMessage(), false);
        }
    }

    @FXML
    private void deleteCategorie() {
        if (catServices == null) { showToast("Service non disponible", false); return; }
        categorie selected = tableCategorie.getSelectionModel().getSelectedItem();
        if (selected == null) { showToast("Veuillez sélectionner une catégorie", false); return; }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer cette catégorie ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + selected.getNomcategorie() + "\" ?");
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                catServices.supprimer(selected.getIdcategorie());

                // Animate row removal
                int idx = tableCategorie.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && idx < tableCategorie.getItems().size()) {
                    TableRow<?> row = null;
                    for (Node node : tableCategorie.lookupAll(".table-row-cell")) {
                        if (node instanceof TableRow && ((TableRow<?>) node).getIndex() == idx) {
                            row = (TableRow<?>) node;
                            break;
                        }
                    }
                    if (row != null) {
                        FadeTransition fadeRow = new FadeTransition(Duration.millis(300), row);
                        fadeRow.setToValue(0);
                        fadeRow.setOnFinished(e -> loadData());
                        fadeRow.play();
                    } else {
                        loadData();
                    }
                } else {
                    loadData();
                }
                showToast("Catégorie supprimée !", true);
            } catch (SQLException e) {
                showToast("Erreur de suppression: " + e.getMessage(), false);
            }
        }
    }
}
