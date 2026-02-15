package com.esprit.controllers;

import com.esprit.entities.categorie;
import com.esprit.services.categorieServices;
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
import java.time.LocalDate;

/**
 * Controller for managing Category table view and CRUD operations.
 */
public class CategorieController {

    @FXML
    private TableView<categorie> tableCategorie;
    @FXML
    private TableColumn<categorie, Integer> colId;
    @FXML
    private TableColumn<categorie, String> colNom, colDescription;
    @FXML
    private TableColumn<categorie, LocalDate> colDateCreation;
    @FXML
    private Button btnAjouter, btnModifier, btnSupprimer;
    @FXML
    private Label lblCount;

    private categorieServices catServices;
    private ObservableList<categorie> categorieList;

    public void initialize() {
        try {
            catServices = new categorieServices();
            categorieList = FXCollections.observableArrayList();
            tableCategorie.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tableCategorie.setItems(categorieList);

            // Bind columns
            colId.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getIdcategorie()));
            colNom.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNomcategorie()));
            colDescription.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
            colDateCreation.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getDateCreation()));

            // Setup button handlers
            if (btnAjouter != null) btnAjouter.setOnAction(e -> openAddDialog());
            if (btnModifier != null) btnModifier.setOnAction(e -> openEditDialog());
            if (btnSupprimer != null) btnSupprimer.setOnAction(e -> deleteCategorie());
            
            // Load data after UI is set up
            Platform.runLater(this::loadData);
            System.out.println("✅ CategorieController initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Error initializing CategorieController: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Initialization Error", "Failed to initialize Categorie view: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            if (catServices == null) {
                showErrorAlert("Error", "Database service not available. Check MySQL connection.");
                return;
            }
            
            categorieList.clear();
            categorieList.addAll(catServices.afficher());
            tableCategorie.setItems(categorieList);            if (lblCount != null) lblCount.setText(String.valueOf(categorieList.size()));            System.out.println("✅ Loaded " + categorieList.size() + " categories");
        } catch (SQLException e) {
            System.err.println("❌ SQL Error loading data: " + e.getMessage());
            showErrorAlert("Database Error", "Error loading categories: " + e.getMessage());
        }
    }

    @FXML
    private void openAddDialog() {
        if (catServices == null) {
            showErrorAlert("Error", "Database service not available");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CategorieFormDialog.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Add Category");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);

            // Apply stylesheet and maximize
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMaximized(true);

            CategorieFormDialogController dialogController = loader.getController();
            dialogController.setMode("ADD");

            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                categorie newCat = dialogController.getCategorie();
                if (newCat != null) {
                    catServices.ajouter(newCat);
                    loadData();
                    showSuccessAlert("Success", "Category added successfully");
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error opening dialog: " + e.getMessage());
            showErrorAlert("Error", "Error opening form: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            showErrorAlert("Error", "Error adding category: " + e.getMessage());
        }
    }

    @FXML
    private void openEditDialog() {
        if (catServices == null) {
            showErrorAlert("Error", "Database service not available");
            return;
        }
        
        categorie selected = tableCategorie.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorAlert("Error", "Please select a category to edit");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CategorieFormDialog.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Edit Category");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(true);

            // Apply stylesheet and maximize
            String css = getClass().getResource("/style.css").toExternalForm();
            dialogPane.getStylesheets().add(css);
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMaximized(true);

            CategorieFormDialogController dialogController = loader.getController();
            dialogController.setMode("EDIT");
            dialogController.setCategorie(selected);

            java.util.Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                categorie updatedCat = dialogController.getCategorie();
                if (updatedCat != null) {
                    updatedCat.setIdcategorie(selected.getIdcategorie());
                    catServices.modifier(updatedCat);
                    loadData();
                    showSuccessAlert("Success", "Category updated successfully");
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error opening dialog: " + e.getMessage());
            showErrorAlert("Error", "Error opening form: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            showErrorAlert("Error", "Error updating category: " + e.getMessage());
        }
    }

    @FXML
    private void deleteCategorie() {
        if (catServices == null) {
            showErrorAlert("Error", "Database service not available");
            return;
        }
        
        categorie selected = tableCategorie.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorAlert("Error", "Please select a category to delete");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Category?");
        confirmAlert.setContentText("Are you sure you want to delete this category?");
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                catServices.supprimer(selected.getIdcategorie());
                loadData();
                showSuccessAlert("Success", "Category deleted successfully");
            } catch (SQLException e) {
                System.err.println("❌ Database error: " + e.getMessage());
                showErrorAlert("Error", "Error deleting category: " + e.getMessage());
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
