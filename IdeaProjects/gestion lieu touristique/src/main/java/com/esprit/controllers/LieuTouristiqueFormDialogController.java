package com.esprit.controllers;

import com.esprit.entities.LieuTouristique;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

/**
 * Controller for LieuTouristique (Tourist Location) form dialog.
 * Handles bidirectional data mapping between form fields and LieuTouristique entity.
 * Includes image file chooser and preview functionality.
 */
public class LieuTouristiqueFormDialogController {

    @FXML
    private TextField tfNom;
    @FXML
    private TextField tfDescription;
    @FXML
    private TextField tfVille;
    @FXML
    private TextField tfPrix;
    @FXML
    private TextField tfImage;
    @FXML
    private TextField tfStatut;
    @FXML
    private TextField tfIdCategorie;
    @FXML
    private TextField tfIdAdresse;
    @FXML
    private Button btnChooseImage;
    @FXML
    private ImageView imagePreview;

    private String mode = "ADD";

    @FXML
    public void initialize() {
        if (btnChooseImage != null) {
            btnChooseImage.setOnAction(e -> chooseImageFile());
        }
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Populates form fields with existing location data for editing.
     * Loads image preview if path exists and is valid.
     * @param l The location to edit
     */
    public void setLieuTouristique(LieuTouristique l) {
        if (l == null) {
            System.err.println("❌ LieuTouristique is null");
            return;
        }
        
        if (tfNom != null) tfNom.setText(l.getNom() != null ? l.getNom() : "");
        if (tfDescription != null) tfDescription.setText(l.getDescription() != null ? l.getDescription() : "");
        if (tfVille != null) tfVille.setText(l.getVille() != null ? l.getVille() : "");
        if (tfPrix != null) tfPrix.setText(String.valueOf(l.getPrix()));
        if (tfImage != null) tfImage.setText(l.getImage() != null ? l.getImage() : "");
        if (tfStatut != null) tfStatut.setText(String.valueOf(l.getStatut()));
        if (tfIdCategorie != null) tfIdCategorie.setText(String.valueOf(l.getId_categorie()));
        if (tfIdAdresse != null) tfIdAdresse.setText(String.valueOf(l.getId_adresse()));
        
        // Load image preview if path exists
        if (l.getImage() != null && !l.getImage().trim().isEmpty()) {
            loadImagePreview(l.getImage());
        }
    }

    /**
     * Extracts form data and creates a LieuTouristique object.
     * Validates that required fields are not empty and numeric fields are valid.
     * @return A new LieuTouristique object, or null if validation fails
     */
    public LieuTouristique getLieuTouristique() {
        if (tfNom == null || tfDescription == null || tfVille == null || tfPrix == null || 
            tfImage == null || tfStatut == null || tfIdCategorie == null || tfIdAdresse == null) {
            System.err.println("❌ One or more form fields are not initialized");
            return null;
        }
        
        try {
            String nom = tfNom.getText().trim();
            String description = tfDescription.getText().trim();
            String ville = tfVille.getText().trim();
            String priceStr = tfPrix.getText().trim();
            String statutStr = tfStatut.getText().trim();
            String catStr = tfIdCategorie.getText().trim();
            String adrStr = tfIdAdresse.getText().trim();
            
            if (nom.isEmpty() || ville.isEmpty()) {
                System.err.println("⚠️ Name and city cannot be empty");
                return null;
            }
            
            double prix = Double.parseDouble(priceStr);
            String image = tfImage.getText();
            int statut = Integer.parseInt(statutStr);
            int idCategorie = Integer.parseInt(catStr);
            int idAdresse = Integer.parseInt(adrStr);

            return new LieuTouristique(nom, description, ville, idAdresse, prix, image, statut, idCategorie);
        } catch (NumberFormatException e) {
            System.err.println("❌ Error parsing numeric fields: " + e.getMessage());
            return null;
        }
    }

    /**
     * Opens a file chooser dialog for selecting an image file.
     * Updates the image path field and loads preview if file exists.
     */
    private void chooseImageFile() {
        if (btnChooseImage == null || btnChooseImage.getScene() == null) {
            System.err.println("❌ Button or scene not yet initialized");
            return;
        }
        
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Image");
            
            // Set initial directory to Pictures folder
            String userHome = System.getProperty("user.home");
            File initialDir = new File(userHome + File.separator + "Pictures");
            if (initialDir.exists()) {
                fileChooser.setInitialDirectory(initialDir);
            }
            
            // Add image file filters
            FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                    "Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp");
            FileChooser.ExtensionFilter allFilesFilter = new FileChooser.ExtensionFilter(
                    "All Files", "*.*");
            fileChooser.getExtensionFilters().addAll(imageFilter, allFilesFilter);
            fileChooser.setSelectedExtensionFilter(imageFilter);
            
            // Get the stage from the button's scene
            Stage stage = (Stage) btnChooseImage.getScene().getWindow();
            
            // Show file chooser dialog
            File selectedFile = fileChooser.showOpenDialog(stage);
            
            // If a file is selected, set the path in TextField and load preview
            if (selectedFile != null) {
                String filePath = selectedFile.getAbsolutePath();
                if (tfImage != null) tfImage.setText(filePath);
                loadImagePreview(filePath);
            }
        } catch (Exception e) {
            System.err.println("❌ Error selecting image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads and displays an image preview from a file path.
     * Handles missing files gracefully by clearing the preview.
     * @param imagePath The absolute path to the image file
     */
    private void loadImagePreview(String imagePath) {
        if (imagePreview == null) {
            System.err.println("⚠️ ImageView not initialized");
            return;
        }
        
        try {
            if (imagePath == null || imagePath.trim().isEmpty()) {
                imagePreview.setImage(null);
                return;
            }
            
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                // Load image from file path
                String imageUrl = imageFile.toURI().toString();
                Image image = new Image(imageUrl, 180, 180, true, true);
                imagePreview.setImage(image);
            } else {
                // File doesn't exist, clear preview
                imagePreview.setImage(null);
                System.err.println("⚠️ Image file not found: " + imagePath);
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading image preview: " + e.getMessage());
            imagePreview.setImage(null);
        }
    }
}
