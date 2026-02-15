package com.esprit.controllers;

import com.esprit.entities.Adresse;
import com.esprit.services.AdresseServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class AdresseController {

    @FXML
    private TableView<Adresse> tableAdresse;
    @FXML
    private TableColumn<Adresse, Integer> colId;
    @FXML
    private TableColumn<Adresse, String> colRue, colVilleCol;
    @FXML
    private TableColumn<Adresse, Double> colLatitude, colLongitude, colAltitude;
    @FXML
    private Button btnAjouter, btnModifier, btnSupprimer;
    @FXML
    private Label lblCount;

    private AdresseServices adresseServices;
    private ObservableList<Adresse> adresseList;

    public void initialize() {
        try {
            // Initialize services and list
            adresseServices = new AdresseServices();
            adresseList = FXCollections.observableArrayList();
            tableAdresse.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tableAdresse.setItems(adresseList);

            // Bind columns to entity properties
            colId.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getId_adresse()));
            colRue.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getRue()));
            colVilleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getVille()));
            colLatitude.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getLatitude()));
            colLongitude.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getLongitude()));
            colAltitude.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getAltitude()));

            // Setup button handlers
            if (btnAjouter != null) btnAjouter.setOnAction(e -> openAddDialog());
            if (btnModifier != null) btnModifier.setOnAction(e -> openEditDialog());
            if (btnSupprimer != null) btnSupprimer.setOnAction(e -> deleteAdresse());
            
            // Load data after UI is fully initialized
            Platform.runLater(this::loadData);
            
            System.out.println("✅ AdresseController initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Error initializing AdresseController: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Initialization Error", "Failed to initialize Adresse view: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            if (adresseServices == null) {
                showErrorAlert("Error", "Database service not available. Check MySQL connection.");
                return;
            }
            
            adresseList.clear();
            adresseList.addAll(adresseServices.afficher());
            tableAdresse.setItems(adresseList);            if (lblCount != null) lblCount.setText(String.valueOf(adresseList.size()));            System.out.println("✅ Loaded " + adresseList.size() + " addresses");
        } catch (SQLException e) {
            System.err.println("❌ SQL Error loading data: " + e.getMessage());
            showErrorAlert("Database Error", "Error loading addresses: " + e.getMessage());
        }
    }

    @FXML
    private void openAddDialog() {
        if (adresseServices == null) {
            showErrorAlert("Error", "Database service not available");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdresseFormDialog.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Add Address");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);

            // Apply stylesheet and maximize
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMaximized(true);

            AdresseFormDialogController dialogController = loader.getController();
            dialogController.setMode("ADD");

            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Adresse newAdresse = dialogController.getAdresse();
                if (newAdresse != null) {
                    adresseServices.ajouter(newAdresse);
                    loadData();
                    showSuccessAlert("Success", "Address added successfully");
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error opening dialog: " + e.getMessage());
            showErrorAlert("Error", "Error opening form: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            showErrorAlert("Error", "Error adding address: " + e.getMessage());
        }
    }

    @FXML
    private void openEditDialog() {
        if (adresseServices == null) {
            showErrorAlert("Error", "Database service not available");
            return;
        }
        
        Adresse selected = tableAdresse.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorAlert("Error", "Please select an address to edit");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdresseFormDialog.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Edit Address");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);

            // Apply stylesheet and maximize
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMaximized(true);

            AdresseFormDialogController dialogController = loader.getController();
            dialogController.setMode("EDIT");
            dialogController.setAdresse(selected);

            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Adresse updatedAdresse = dialogController.getAdresse();
                if (updatedAdresse != null) {
                    updatedAdresse.setId_adresse(selected.getId_adresse());
                    adresseServices.modifier(updatedAdresse);
                    loadData();
                    showSuccessAlert("Success", "Address updated successfully");
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error opening dialog: " + e.getMessage());
            showErrorAlert("Error", "Error opening form: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            showErrorAlert("Error", "Error updating address: " + e.getMessage());
        }
    }

    @FXML
    private void deleteAdresse() {
        if (adresseServices == null) {
            showErrorAlert("Error", "Database service not available");
            return;
        }
        
        Adresse selected = tableAdresse.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorAlert("Error", "Please select an address to delete");
            return;
        }

        // Show confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Address?");
        confirmAlert.setContentText("Are you sure you want to delete this address?");
        
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                adresseServices.supprimer(selected.getId_adresse());
                loadData();
                showSuccessAlert("Success", "Address deleted successfully");
            } catch (SQLException e) {
                System.err.println("❌ Database error: " + e.getMessage());
                showErrorAlert("Error", "Error deleting address: " + e.getMessage());
            }
        }
    }

    /**
     * Show success alert with INFORMATION type
     */
    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error alert with ERROR type
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
