package com.esprit.controllers;

import com.esprit.entities.Adresse;
import com.esprit.entities.LieuTouristique;
import com.esprit.entities.categorie;
import com.esprit.services.AdresseServices;
import com.esprit.services.GeminiService;
import com.esprit.services.categorieServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for LieuTouristique form dialog.
 * Single image picker that stores the image path in lieu_touristique.image.
 */
public class LieuTouristiqueFormDialogController {

    @FXML private TextField tfNom;
    @FXML private TextField tfDescription;
    @FXML private TextField tfVille;
    @FXML private TextField tfPrix;
    @FXML private TextField tfStatut;
    @FXML private ComboBox<categorie> cbCategorie;
    @FXML private ComboBox<Adresse> cbAdresse;
    @FXML private Label lblErrNom, lblErrDesc, lblErrVille, lblErrPrix, lblErrStatut, lblErrCat, lblErrAdr;
    @FXML private Button btnAiDesc;
    @FXML private Label lblAiStatus;

    // Single image fields
    @FXML private Button btnBrowseImage;
    @FXML private StackPane imagePreviewPane;
    @FXML private Label lblImageHint;
    @FXML private ImageView ivPreview;
    @FXML private Label lblImagePath;

    private String mode = "ADD";
    private categorieServices catService;
    private AdresseServices adrService;
    private GeminiService geminiService;

    /** The selected image file path */
    private String selectedImagePath = "";

    @FXML
    public void initialize() {
        try {
            catService = new categorieServices();
            adrService = new AdresseServices();
            geminiService = new GeminiService();
            loadComboBoxData();
        } catch (Exception e) {
            System.err.println("❌ Error loading combo data: " + e.getMessage());
        }

        // --- Browse image button ---
        if (btnBrowseImage != null) {
            btnBrowseImage.setOnAction(e -> chooseSingleImage());
        }

        // --- Drag & Drop on preview pane ---
        setupDragAndDrop();

        // --- Validation listeners ---
        if (tfNom != null) tfNom.textProperty().addListener((obs, o, n) -> validateNom(n));
        if (tfDescription != null) tfDescription.textProperty().addListener((obs, o, n) -> validateDescription(n));
        if (tfVille != null) tfVille.textProperty().addListener((obs, o, n) -> validateVille(n));
        if (tfPrix != null) tfPrix.textProperty().addListener((obs, o, n) -> validatePrix(n));
        if (tfStatut != null) tfStatut.textProperty().addListener((obs, o, n) -> validateStatut(n));
        if (cbCategorie != null) cbCategorie.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) showComboValid(cbCategorie, lblErrCat);
        });
        if (cbAdresse != null) cbAdresse.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) showComboValid(cbAdresse, lblErrAdr);
        });
    }

    // ==================== DRAG & DROP ====================

    private void setupDragAndDrop() {
        if (imagePreviewPane == null) return;

        imagePreviewPane.setOnDragOver(event -> {
            if (event.getGestureSource() != imagePreviewPane && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

        imagePreviewPane.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                imagePreviewPane.getStyleClass().add("gallery-drop-zone-active");
                if (lblImageHint != null) lblImageHint.setText("📥 Relâchez pour sélectionner");
            }
            event.consume();
        });

        imagePreviewPane.setOnDragExited(event -> {
            imagePreviewPane.getStyleClass().remove("gallery-drop-zone-active");
            if (lblImageHint != null && selectedImagePath.isEmpty()) {
                lblImageHint.setText("📸 Aucune image sélectionnée");
            }
            event.consume();
        });

        imagePreviewPane.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File f : db.getFiles()) {
                    if (isImageFile(f)) {
                        setImagePreview(f.getAbsolutePath());
                        success = true;
                        break; // Only take the first image
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
            imagePreviewPane.getStyleClass().remove("gallery-drop-zone-active");
        });
    }

    // ==================== IMAGE HANDLING ====================

    private void chooseSingleImage() {
        if (imagePreviewPane == null || imagePreviewPane.getScene() == null) return;
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner une image");
            String userHome = System.getProperty("user.home");
            File initialDir = new File(userHome + File.separator + "Pictures");
            if (initialDir.exists()) fileChooser.setInitialDirectory(initialDir);
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"),
                    new FileChooser.ExtensionFilter("All Files", "*.*"));
            Stage stage = (Stage) imagePreviewPane.getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                setImagePreview(selectedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("❌ Error selecting image: " + e.getMessage());
        }
    }

    /**
     * Set the selected image path and update the preview.
     */
    private void setImagePreview(String path) {
        selectedImagePath = path;
        if (ivPreview != null) {
            try {
                File f = new File(path);
                if (f.exists()) {
                    ivPreview.setImage(new Image(f.toURI().toString(), 200, 130, true, true));
                    ivPreview.setVisible(true);
                }
            } catch (Exception e) {
                ivPreview.setVisible(false);
            }
        }
        if (lblImageHint != null) {
            lblImageHint.setVisible(false);
            lblImageHint.setManaged(false);
        }
        if (lblImagePath != null) {
            String fileName = new File(path).getName();
            lblImagePath.setText("📎 " + fileName);
        }
    }

    private boolean isImageFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp");
    }

    // ==================== COMBOBOX SETUP ====================

    private void loadComboBoxData() {
        try {
            List<categorie> categories = catService.afficher();
            cbCategorie.getItems().addAll(categories);
            cbCategorie.setConverter(new StringConverter<categorie>() {
                @Override
                public String toString(categorie c) { return c == null ? "" : c.getNomcategorie(); }
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

            List<Adresse> adresses = adrService.afficher();
            cbAdresse.getItems().addAll(adresses);
            cbAdresse.setConverter(new StringConverter<Adresse>() {
                @Override
                public String toString(Adresse a) { return a == null ? "" : a.getRue() + ", " + a.getVille(); }
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

    // ==================== MODE & DATA ====================

    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Populates form fields (for editing). Also loads the legacy single image.
     */
    public void setLieuTouristique(LieuTouristique l) {
        if (l == null) return;

        if (tfNom != null) tfNom.setText(l.getNom() != null ? l.getNom() : "");
        if (tfDescription != null) tfDescription.setText(l.getDescription() != null ? l.getDescription() : "");
        if (tfVille != null) tfVille.setText(l.getVille() != null ? l.getVille() : "");
        if (tfPrix != null) tfPrix.setText(String.valueOf(l.getPrix()));
        if (tfStatut != null) tfStatut.setText(String.valueOf(l.getStatut()));

        if (cbCategorie != null) {
            for (categorie c : cbCategorie.getItems()) {
                if (c.getIdcategorie() == l.getId_categorie()) {
                    cbCategorie.getSelectionModel().select(c);
                    break;
                }
            }
        }
        if (cbAdresse != null) {
            for (Adresse a : cbAdresse.getItems()) {
                if (a.getId_adresse() == l.getId_adresse()) {
                    cbAdresse.getSelectionModel().select(a);
                    break;
                }
            }
        }

        // Load existing image into preview if present
        if (l.getImage() != null && !l.getImage().trim().isEmpty()) {
            File imgFile = new File(l.getImage());
            if (imgFile.exists()) {
                setImagePreview(l.getImage());
            }
        }
    }

    /**
     * Validates all fields and builds a LieuTouristique object.
     */
    public LieuTouristique getLieuTouristique() {
        if (tfNom == null || tfDescription == null || tfVille == null || tfPrix == null ||
                tfStatut == null || cbCategorie == null || cbAdresse == null) {
            System.err.println("❌ One or more form fields are not initialized");
            return null;
        }

        boolean valid = true;

        String nom = tfNom.getText().trim();
        if (nom.isEmpty()) { showFieldError(tfNom, lblErrNom, "Le nom est obligatoire"); valid = false; }
        else if (nom.length() < 2) { showFieldError(tfNom, lblErrNom, "Minimum 2 caractères"); valid = false; }
        else if (nom.length() > 100) { showFieldError(tfNom, lblErrNom, "Maximum 100 caractères"); valid = false; }
        else showFieldValid(tfNom, lblErrNom);

        String description = tfDescription.getText().trim();
        if (description.isEmpty()) { showFieldError(tfDescription, lblErrDesc, "La description est obligatoire"); valid = false; }
        else if (description.length() < 5) { showFieldError(tfDescription, lblErrDesc, "Minimum 5 caractères"); valid = false; }
        else showFieldValid(tfDescription, lblErrDesc);

        String ville = tfVille.getText().trim();
        if (ville.isEmpty()) { showFieldError(tfVille, lblErrVille, "La ville est obligatoire"); valid = false; }
        else if (ville.length() < 2) { showFieldError(tfVille, lblErrVille, "Minimum 2 caractères"); valid = false; }
        else if (!ville.matches("[a-zA-ZÀ-ÿ\\s\\-'.]+")) { showFieldError(tfVille, lblErrVille, "La ville ne doit contenir que des lettres"); valid = false; }
        else showFieldValid(tfVille, lblErrVille);

        double prix = 0;
        String priceStr = tfPrix.getText().trim();
        if (priceStr.isEmpty()) { showFieldError(tfPrix, lblErrPrix, "Le prix est obligatoire"); valid = false; }
        else {
            try {
                prix = Double.parseDouble(priceStr);
                if (prix < 0) { showFieldError(tfPrix, lblErrPrix, "Le prix ne peut pas être négatif"); valid = false; }
                else showFieldValid(tfPrix, lblErrPrix);
            } catch (NumberFormatException e) {
                showFieldError(tfPrix, lblErrPrix, "Nombre invalide"); valid = false;
            }
        }

        int statut = 0;
        String statutStr = tfStatut.getText().trim();
        if (statutStr.isEmpty()) { showFieldError(tfStatut, lblErrStatut, "Le statut est obligatoire"); valid = false; }
        else {
            try {
                statut = Integer.parseInt(statutStr);
                if (statut != 0 && statut != 1) { showFieldError(tfStatut, lblErrStatut, "Seulement 0 (Inactif) ou 1 (Actif)"); valid = false; }
                else showFieldValid(tfStatut, lblErrStatut);
            } catch (NumberFormatException e) {
                showFieldError(tfStatut, lblErrStatut, "Entier invalide (0 ou 1)"); valid = false;
            }
        }

        categorie selectedCat = cbCategorie.getSelectionModel().getSelectedItem();
        if (selectedCat == null) { showComboError(cbCategorie, lblErrCat, "Veuillez sélectionner une catégorie"); valid = false; }
        else showComboValid(cbCategorie, lblErrCat);

        Adresse selectedAdr = cbAdresse.getSelectionModel().getSelectedItem();
        if (selectedAdr == null) { showComboError(cbAdresse, lblErrAdr, "Veuillez sélectionner une adresse"); valid = false; }
        else showComboValid(cbAdresse, lblErrAdr);

        if (!valid) return null;

        // Use the selected image path
        String image = selectedImagePath;
        int idCategorie = selectedCat.getIdcategorie();
        int idAdresse = selectedAdr.getId_adresse();

        return new LieuTouristique(nom, description, ville, idAdresse, prix, image, statut, idCategorie);
    }

    // ==================== AI DESCRIPTION GENERATOR ====================

    @FXML
    private void generateAiDescription() {
        String nom = tfNom.getText() != null ? tfNom.getText().trim() : "";
        String ville = tfVille.getText() != null ? tfVille.getText().trim() : "";
        categorie selectedCat = cbCategorie.getSelectionModel().getSelectedItem();

        if (nom.isEmpty()) {
            showAiStatus("⚠️ Entrez le nom du lieu d'abord", true);
            return;
        }
        if (ville.isEmpty()) {
            showAiStatus("⚠️ Entrez la ville d'abord", true);
            return;
        }

        String catName = selectedCat != null ? selectedCat.getNomcategorie() : "Général";
        double prix = 0;
        try { prix = Double.parseDouble(tfPrix.getText().trim()); } catch (Exception ignored) {}

        btnAiDesc.setDisable(true);
        showAiStatus("✨ Génération IA en cours...", false);

        double finalPrix = prix;
        Thread aiThread = new Thread(() -> {
            try {
                String description = geminiService.generateDescription(nom, ville, catName, finalPrix);
                Platform.runLater(() -> {
                    tfDescription.setText(description);
                    btnAiDesc.setDisable(false);
                    showAiStatus("✅ Description générée par IA", false);
                    // Auto-hide status after 3 seconds
                    new Thread(() -> {
                        try { Thread.sleep(3000); } catch (Exception ignored) {}
                        Platform.runLater(this::hideAiStatus);
                    }).start();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnAiDesc.setDisable(false);
                    showAiStatus("❌ " + e.getMessage(), true);
                });
            }
        });
        aiThread.setDaemon(true);
        aiThread.setName("gemini-description");
        aiThread.start();
    }

    private void showAiStatus(String text, boolean isError) {
        if (lblAiStatus != null) {
            lblAiStatus.setText(text);
            lblAiStatus.setStyle(isError
                    ? "-fx-text-fill: #e74c3c; -fx-font-size: 10px;"
                    : "-fx-text-fill: rgba(191,162,0,0.85); -fx-font-size: 10px;");
            lblAiStatus.setVisible(true);
            lblAiStatus.setManaged(true);
        }
    }

    private void hideAiStatus() {
        if (lblAiStatus != null) {
            lblAiStatus.setVisible(false);
            lblAiStatus.setManaged(false);
        }
    }

    // ==================== REAL-TIME VALIDATORS ====================

    private void validateNom(String val) {
        if (val == null) return;
        String v = val.trim();
        if (v.isEmpty()) clearFieldState(tfNom, lblErrNom);
        else if (v.length() < 2) showFieldError(tfNom, lblErrNom, "Minimum 2 caractères");
        else if (v.length() > 100) showFieldError(tfNom, lblErrNom, "Maximum 100 caractères");
        else showFieldValid(tfNom, lblErrNom);
    }

    private void validateDescription(String val) {
        if (val == null) return;
        String v = val.trim();
        if (v.isEmpty()) clearFieldState(tfDescription, lblErrDesc);
        else if (v.length() < 5) showFieldError(tfDescription, lblErrDesc, "Minimum 5 caractères");
        else showFieldValid(tfDescription, lblErrDesc);
    }

    private void validateVille(String val) {
        if (val == null) return;
        String v = val.trim();
        if (v.isEmpty()) clearFieldState(tfVille, lblErrVille);
        else if (v.length() < 2) showFieldError(tfVille, lblErrVille, "Minimum 2 caractères");
        else if (!v.matches("[a-zA-ZÀ-ÿ\\s\\-'.]+")) showFieldError(tfVille, lblErrVille, "Lettres uniquement");
        else showFieldValid(tfVille, lblErrVille);
    }

    private void validatePrix(String val) {
        if (val == null || val.trim().isEmpty()) { clearFieldState(tfPrix, lblErrPrix); return; }
        try {
            double d = Double.parseDouble(val.trim());
            if (d < 0) showFieldError(tfPrix, lblErrPrix, "Le prix ne peut pas être négatif");
            else showFieldValid(tfPrix, lblErrPrix);
        } catch (NumberFormatException e) {
            showFieldError(tfPrix, lblErrPrix, "Nombre invalide");
        }
    }

    private void validateStatut(String val) {
        if (val == null || val.trim().isEmpty()) { clearFieldState(tfStatut, lblErrStatut); return; }
        try {
            int s = Integer.parseInt(val.trim());
            if (s != 0 && s != 1) showFieldError(tfStatut, lblErrStatut, "Seulement 0 ou 1");
            else showFieldValid(tfStatut, lblErrStatut);
        } catch (NumberFormatException e) {
            showFieldError(tfStatut, lblErrStatut, "Entier invalide");
        }
    }

    // ==================== STYLING HELPERS ====================

    private void showFieldError(TextField field, Label errLabel, String msg) {
        if (field != null) { field.getStyleClass().removeAll("input-error", "input-valid"); field.getStyleClass().add("input-error"); }
        if (errLabel != null) { errLabel.setText(msg); errLabel.setVisible(true); errLabel.setManaged(true); }
    }

    private void showFieldValid(TextField field, Label errLabel) {
        if (field != null) { field.getStyleClass().removeAll("input-error", "input-valid"); field.getStyleClass().add("input-valid"); }
        if (errLabel != null) { errLabel.setVisible(false); errLabel.setManaged(false); }
    }

    private void clearFieldState(TextField field, Label errLabel) {
        if (field != null) field.getStyleClass().removeAll("input-error", "input-valid");
        if (errLabel != null) { errLabel.setVisible(false); errLabel.setManaged(false); }
    }

    private void showComboError(ComboBox<?> combo, Label errLabel, String msg) {
        if (combo != null) { combo.getStyleClass().removeAll("input-error", "input-valid"); combo.getStyleClass().add("input-error"); }
        if (errLabel != null) { errLabel.setText(msg); errLabel.setVisible(true); errLabel.setManaged(true); }
    }

    private void showComboValid(ComboBox<?> combo, Label errLabel) {
        if (combo != null) { combo.getStyleClass().removeAll("input-error", "input-valid"); combo.getStyleClass().add("input-valid"); }
        if (errLabel != null) { errLabel.setVisible(false); errLabel.setManaged(false); }
    }
}
