package com.esprit.controllers;

import com.esprit.entities.Adresse;
import com.esprit.entities.LieuTouristique;
import com.esprit.entities.categorie;
import com.esprit.services.AdresseServices;
import com.esprit.services.categorieServices;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for LieuTouristique form dialog.
 * Uses ComboBoxes for selecting existing categories and addresses.
 */
public class LieuTouristiqueFormDialogController {

    @FXML private TextField tfNom;
    @FXML private TextField tfDescription;
    @FXML private TextField tfVille;
    @FXML private TextField tfPrix;
    @FXML private TextField tfImage;
    @FXML private TextField tfStatut;
    @FXML private ComboBox<categorie> cbCategorie;
    @FXML private ComboBox<Adresse> cbAdresse;
    @FXML private Button btnChooseImage;
    @FXML private ImageView imagePreview;

    private String mode = "ADD";
    private categorieServices catService;
    private AdresseServices adrService;

    @FXML
    public void initialize() {
        if (btnChooseImage != null) {
            btnChooseImage.setOnAction(e -> chooseImageFile());
        }

        try {
            catService = new categorieServices();
            adrService = new AdresseServices();
            loadComboBoxData();
        } catch (Exception e) {
            System.err.println("❌ Error loading combo data: " + e.getMessage());
        }
    }

    private void loadComboBoxData() {
        try {
            // --- Catégorie ComboBox ---
            List<categorie> categories = catService.afficher();
            cbCategorie.getItems().addAll(categories);
            cbCategorie.setConverter(new StringConverter<categorie>() {
                @Override
                public String toString(categorie c) {
                    return c == null ? "" : c.getNomcategorie();
                }
                @Override
                public categorie fromString(String s) { return null; }
            });
            cbCategorie.setCellFactory(lv -> new ListCell<categorie>() {
                @Override
                protected void updateItem(categorie c, boolean empty) {
                    super.updateItem(c, empty);
                    setText(empty || c == null ? null : c.getNomcategorie());
                }
            });

            // --- Adresse ComboBox ---
            List<Adresse> adresses = adrService.afficher();
            cbAdresse.getItems().addAll(adresses);
            cbAdresse.setConverter(new StringConverter<Adresse>() {
                @Override
                public String toString(Adresse a) {
                    return a == null ? "" : a.getRue() + ", " + a.getVille();
                }
                @Override
                public Adresse fromString(String s) { return null; }
            });
            cbAdresse.setCellFactory(lv -> new ListCell<Adresse>() {
                @Override
                protected void updateItem(Adresse a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? null : a.getRue() + ", " + a.getVille());
                }
            });

        } catch (SQLException e) {
            System.err.println("❌ SQL error loading combobox data: " + e.getMessage());
        }
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Populates form fields with existing location data for editing.
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

        // Pre-select matching category
        if (cbCategorie != null) {
            for (categorie c : cbCategorie.getItems()) {
                if (c.getIdcategorie() == l.getId_categorie()) {
                    cbCategorie.getSelectionModel().select(c);
                    break;
                }
            }
        }
        // Pre-select matching address
        if (cbAdresse != null) {
            for (Adresse a : cbAdresse.getItems()) {
                if (a.getId_adresse() == l.getId_adresse()) {
                    cbAdresse.getSelectionModel().select(a);
                    break;
                }
            }
        }

        if (l.getImage() != null && !l.getImage().trim().isEmpty()) {
            loadImagePreview(l.getImage());
        }
    }

    /**
     * Extracts form data and creates a LieuTouristique object.
     */
    public LieuTouristique getLieuTouristique() {
        if (tfNom == null || tfDescription == null || tfVille == null || tfPrix == null ||
                tfImage == null || tfStatut == null || cbCategorie == null || cbAdresse == null) {
            System.err.println("❌ One or more form fields are not initialized");
            return null;
        }

        try {
            String nom = tfNom.getText().trim();
            String description = tfDescription.getText().trim();
            String ville = tfVille.getText().trim();
            String priceStr = tfPrix.getText().trim();
            String statutStr = tfStatut.getText().trim();

            if (nom.isEmpty() || ville.isEmpty()) {
                System.err.println("⚠️ Name and city cannot be empty");
                return null;
            }

            categorie selectedCat = cbCategorie.getSelectionModel().getSelectedItem();
            Adresse selectedAdr = cbAdresse.getSelectionModel().getSelectedItem();

            if (selectedCat == null) {
                System.err.println("⚠️ Please select a category");
                return null;
            }
            if (selectedAdr == null) {
                System.err.println("⚠️ Please select an address");
                return null;
            }

            double prix = Double.parseDouble(priceStr);
            String image = tfImage.getText();
            int statut = Integer.parseInt(statutStr);
            int idCategorie = selectedCat.getIdcategorie();
            int idAdresse = selectedAdr.getId_adresse();

            return new LieuTouristique(nom, description, ville, idAdresse, prix, image, statut, idCategorie);
        } catch (NumberFormatException e) {
            System.err.println("❌ Error parsing numeric fields: " + e.getMessage());
            return null;
        }
    }

    private void chooseImageFile() {
        if (btnChooseImage == null || btnChooseImage.getScene() == null) return;
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner une image");
            String userHome = System.getProperty("user.home");
            File initialDir = new File(userHome + File.separator + "Pictures");
            if (initialDir.exists()) fileChooser.setInitialDirectory(initialDir);
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                    new FileChooser.ExtensionFilter("All Files", "*.*"));
            Stage stage = (Stage) btnChooseImage.getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                if (tfImage != null) tfImage.setText(selectedFile.getAbsolutePath());
                loadImagePreview(selectedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("❌ Error selecting image: " + e.getMessage());
        }
    }

    private void loadImagePreview(String imagePath) {
        if (imagePreview == null) return;
        try {
            if (imagePath == null || imagePath.trim().isEmpty()) { imagePreview.setImage(null); return; }
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                imagePreview.setImage(new Image(imageFile.toURI().toString(), 180, 180, true, true));
            } else {
                imagePreview.setImage(null);
            }
        } catch (Exception e) {
            imagePreview.setImage(null);
        }
    }
}
