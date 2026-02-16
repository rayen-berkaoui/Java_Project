package com.esprit.controllers;

import com.esprit.entities.Adresse;
import com.esprit.services.AdresseServices;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;

public class AdresseController {

    @FXML private TableView<Adresse> tableAdresse;
    @FXML private TableColumn<Adresse, Integer> colId;
    @FXML private TableColumn<Adresse, String> colRue, colVilleCol;
    @FXML private TableColumn<Adresse, Double> colLatitude, colLongitude, colAltitude;
    @FXML private Button btnAjouter, btnModifier, btnSupprimer;
    @FXML private Label lblCount;
    @FXML private TextField searchField;

    private AdresseServices adresseServices;
    private ObservableList<Adresse> adresseList;
    private FilteredList<Adresse> filteredList;

    public void initialize() {
        try {
            adresseServices = new AdresseServices();
            adresseList = FXCollections.observableArrayList();
            tableAdresse.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Bind columns
            colId.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getId_adresse()));
            colRue.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getRue()));
            colVilleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getVille()));
            colLatitude.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getLatitude()));
            colLongitude.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getLongitude()));
            colAltitude.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getAltitude()));

            // Filtered list for search
            filteredList = new FilteredList<>(adresseList, p -> true);
            tableAdresse.setItems(filteredList);

            // Search listener
            if (searchField != null) {
                searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                    filteredList.setPredicate(adr -> {
                        if (newVal == null || newVal.trim().isEmpty()) return true;
                        String lower = newVal.toLowerCase().trim();
                        if (adr.getRue() != null && adr.getRue().toLowerCase().contains(lower)) return true;
                        if (adr.getVille() != null && adr.getVille().toLowerCase().contains(lower)) return true;
                        return false;
                    });
                    if (lblCount != null) lblCount.setText(String.valueOf(filteredList.size()));
                });
            }

            // Button handlers
            if (btnAjouter != null) btnAjouter.setOnAction(e -> { animateButton(btnAjouter); openAddDialog(); });
            if (btnModifier != null) btnModifier.setOnAction(e -> { animateButton(btnModifier); openEditDialog(); });
            if (btnSupprimer != null) btnSupprimer.setOnAction(e -> { animateButton(btnSupprimer); deleteAdresse(); });

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

    private void loadData() {
        try {
            if (adresseServices == null) {
                showToast("Service de base de données non disponible", false);
                return;
            }
            adresseList.clear();
            adresseList.addAll(adresseServices.afficher());
            if (lblCount != null) animateCounter(lblCount, adresseList.size());
            System.out.println("✅ Loaded " + adresseList.size() + " addresses");
        } catch (SQLException e) {
            System.err.println("❌ SQL Error: " + e.getMessage());
            showToast("Erreur de chargement: " + e.getMessage(), false);
        }
    }

    // ========== ANIMATIONS ==========

    private void playEntranceAnimation() {
        Node root = tableAdresse.getScene() != null ? tableAdresse.getParent() : null;
        if (root == null) return;

        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(600), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();

        tableAdresse.setTranslateY(30);
        TranslateTransition slide = new TranslateTransition(Duration.millis(700), tableAdresse);
        slide.setFromY(30);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        slide.setDelay(Duration.millis(200));
        slide.play();

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
            if (tableAdresse.getScene() == null) return;
            Node sceneRoot = tableAdresse.getScene().getRoot();
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
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMaximized(true);

            AdresseFormDialogController dc = loader.getController();
            dc.setMode("ADD");

            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Adresse newAdresse = dc.getAdresse();
                if (newAdresse != null) {
                    adresseServices.ajouter(newAdresse);
                    loadData();
                    showToast("Adresse ajoutée avec succès !", true);
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
        if (adresseServices == null) { showToast("Service non disponible", false); return; }
        Adresse selected = tableAdresse.getSelectionModel().getSelectedItem();
        if (selected == null) { showToast("Veuillez sélectionner une adresse", false); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdresseFormDialog.fxml"));
            DialogPane dialogPane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Modifier l'Adresse");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMaximized(true);

            AdresseFormDialogController dc = loader.getController();
            dc.setMode("EDIT");
            dc.setAdresse(selected);

            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Adresse updatedAdresse = dc.getAdresse();
                if (updatedAdresse != null) {
                    updatedAdresse.setId_adresse(selected.getId_adresse());
                    adresseServices.modifier(updatedAdresse);
                    loadData();
                    showToast("Adresse modifiée avec succès !", true);
                }
            }
        } catch (IOException e) {
            showToast("Erreur d'ouverture du formulaire", false);
        } catch (SQLException e) {
            showToast("Erreur de modification: " + e.getMessage(), false);
        }
    }

    @FXML
    private void deleteAdresse() {
        if (adresseServices == null) { showToast("Service non disponible", false); return; }
        Adresse selected = tableAdresse.getSelectionModel().getSelectedItem();
        if (selected == null) { showToast("Veuillez sélectionner une adresse", false); return; }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer cette adresse ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer cette adresse ?");
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                adresseServices.supprimer(selected.getId_adresse());
                loadData();
                showToast("Adresse supprimée !", true);
            } catch (SQLException e) {
                showToast("Erreur de suppression: " + e.getMessage(), false);
            }
        }
    }
}
