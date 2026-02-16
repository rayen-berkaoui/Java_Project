package com.esprit.controllers;

import com.esprit.entities.Adresse;
import com.esprit.entities.LieuTouristique;
import com.esprit.entities.categorie;
import com.esprit.services.AdresseServices;
import com.esprit.services.categorieServices;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Controller for LieuTouristique form dialog.
 * Supports multi-image drag & drop with compression and thumbnail gallery.
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

    // Gallery fields
    @FXML private Button btnAddPhotos;
    @FXML private VBox dropZone;
    @FXML private Label lblDropHint;
    @FXML private ScrollPane galleryScroll;
    @FXML private HBox galleryStrip;
    @FXML private Label lblPhotoCount;

    private String mode = "ADD";
    private categorieServices catService;
    private AdresseServices adrService;

    /** List of image file paths currently in the gallery */
    private final List<String> galleryImages = new ArrayList<>();

    private static final int MAX_IMAGES = 20;
    private static final int COMPRESS_MAX_WIDTH = 1200;
    private static final int COMPRESS_MAX_HEIGHT = 900;
    private static final float COMPRESS_QUALITY = 0.75f;

    @FXML
    public void initialize() {
        try {
            catService = new categorieServices();
            adrService = new AdresseServices();
            loadComboBoxData();
        } catch (Exception e) {
            System.err.println("❌ Error loading combo data: " + e.getMessage());
        }

        // --- Gallery: Add button ---
        if (btnAddPhotos != null) {
            btnAddPhotos.setOnAction(e -> chooseMultipleImages());
        }

        // --- Gallery: Drag & Drop ---
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
        if (dropZone == null) return;

        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                dropZone.getStyleClass().add("gallery-drop-zone-active");
                if (lblDropHint != null) lblDropHint.setText("📥 Relâchez pour ajouter");
            }
            event.consume();
        });

        dropZone.setOnDragExited(event -> {
            dropZone.getStyleClass().remove("gallery-drop-zone-active");
            if (lblDropHint != null) lblDropHint.setText("📸 Glissez-déposez vos images ici");
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> imageFiles = new ArrayList<>();
                for (File f : db.getFiles()) {
                    if (isImageFile(f)) imageFiles.add(f);
                }
                if (!imageFiles.isEmpty()) {
                    addImagesToGallery(imageFiles);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
            dropZone.getStyleClass().remove("gallery-drop-zone-active");
            if (lblDropHint != null) lblDropHint.setText("📸 Glissez-déposez vos images ici");
        });
    }

    // ==================== IMAGE HANDLING ====================

    private void chooseMultipleImages() {
        if (dropZone == null || dropZone.getScene() == null) return;
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner des images");
            String userHome = System.getProperty("user.home");
            File initialDir = new File(userHome + File.separator + "Pictures");
            if (initialDir.exists()) fileChooser.setInitialDirectory(initialDir);
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"),
                    new FileChooser.ExtensionFilter("All Files", "*.*"));
            Stage stage = (Stage) dropZone.getScene().getWindow();
            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                addImagesToGallery(selectedFiles);
            }
        } catch (Exception e) {
            System.err.println("❌ Error selecting images: " + e.getMessage());
        }
    }

    /**
     * Compress, add to list, and refresh the gallery strip.
     */
    private void addImagesToGallery(List<File> files) {
        for (File file : files) {
            if (galleryImages.size() >= MAX_IMAGES) {
                System.out.println("⚠️ Maximum " + MAX_IMAGES + " images allowed");
                break;
            }
            if (!isImageFile(file)) continue;

            // Compress the image
            String compressed = compressImage(file);
            if (compressed != null) {
                galleryImages.add(compressed);
            } else {
                galleryImages.add(file.getAbsolutePath());
            }
        }
        refreshGalleryStrip();
    }

    /**
     * Compress image: resize if too large, JPEG quality reduction.
     * Saves to a temp file and returns the path.
     */
    private String compressImage(File source) {
        try {
            BufferedImage original = ImageIO.read(source);
            if (original == null) return null;

            int origW = original.getWidth();
            int origH = original.getHeight();

            // Calculate new dimensions
            int newW = origW;
            int newH = origH;
            if (origW > COMPRESS_MAX_WIDTH || origH > COMPRESS_MAX_HEIGHT) {
                double ratioW = (double) COMPRESS_MAX_WIDTH / origW;
                double ratioH = (double) COMPRESS_MAX_HEIGHT / origH;
                double ratio = Math.min(ratioW, ratioH);
                newW = (int) (origW * ratio);
                newH = (int) (origH * ratio);
            }

            // If already small enough and JPEG, just use original
            if (newW == origW && newH == origH && source.getName().toLowerCase().matches(".*\\.(jpg|jpeg)$") && source.length() < 500_000) {
                return source.getAbsolutePath();
            }

            // Resize
            BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(original, 0, 0, newW, newH, null);
            g.dispose();

            // Write compressed JPEG
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "lieu_images");
            if (!tempDir.exists()) tempDir.mkdirs();
            File output = new File(tempDir, "img_" + System.currentTimeMillis() + "_" + source.getName().replaceAll("[^a-zA-Z0-9.]", "_"));
            if (!output.getName().toLowerCase().endsWith(".jpg") && !output.getName().toLowerCase().endsWith(".jpeg")) {
                output = new File(output.getAbsolutePath() + ".jpg");
            }

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(COMPRESS_QUALITY);
                ImageOutputStream ios = ImageIO.createImageOutputStream(output);
                writer.setOutput(ios);
                writer.write(null, new IIOImage(resized, null, null), param);
                ios.close();
                writer.dispose();
            }

            long savedKB = (source.length() - output.length()) / 1024;
            if (savedKB > 0) {
                System.out.println("✅ Compressed: " + source.getName() + " saved " + savedKB + "KB");
            }
            return output.getAbsolutePath();

        } catch (IOException e) {
            System.err.println("⚠️ Compression failed for " + source.getName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Rebuild the thumbnail gallery strip from the galleryImages list.
     */
    private void refreshGalleryStrip() {
        if (galleryStrip == null) return;
        galleryStrip.getChildren().clear();

        boolean hasImages = !galleryImages.isEmpty();

        if (galleryScroll != null) {
            galleryScroll.setVisible(hasImages);
            galleryScroll.setManaged(hasImages);
        }

        // Update drop zone hint
        if (hasImages && dropZone != null) {
            dropZone.setMinHeight(60);
            dropZone.setPrefHeight(60);
        } else if (dropZone != null) {
            dropZone.setMinHeight(120);
            dropZone.setPrefHeight(140);
        }

        // Build thumbnails
        for (int i = 0; i < galleryImages.size(); i++) {
            final int index = i;
            String path = galleryImages.get(i);
            StackPane thumb = createThumbnail(path, index);
            galleryStrip.getChildren().add(thumb);
        }

        // Update counter
        if (lblPhotoCount != null) {
            if (hasImages) {
                lblPhotoCount.setText(galleryImages.size() + " photo" + (galleryImages.size() > 1 ? "s" : "") + " ajoutée" + (galleryImages.size() > 1 ? "s" : ""));
            } else {
                lblPhotoCount.setText("");
            }
        }
    }

    /**
     * Create a single thumbnail card with delete button overlay.
     */
    private StackPane createThumbnail(String imagePath, int index) {
        StackPane card = new StackPane();
        card.setPrefSize(72, 72);
        card.setMinSize(72, 72);
        card.setMaxSize(72, 72);
        card.getStyleClass().add("gallery-thumb-card");

        // Clip
        Rectangle clip = new Rectangle(72, 72);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        card.setClip(clip);

        // Image
        ImageView iv = new ImageView();
        iv.setFitWidth(72);
        iv.setFitHeight(72);
        iv.setPreserveRatio(false);
        try {
            File f = new File(imagePath);
            if (f.exists()) {
                iv.setImage(new Image(f.toURI().toString(), 72, 72, false, true));
            }
        } catch (Exception ignored) {}

        // Delete button overlay
        Label deleteBtn = new Label("✕");
        deleteBtn.getStyleClass().add("gallery-thumb-delete");
        deleteBtn.setOnMouseClicked(e -> {
            galleryImages.remove(index);
            refreshGalleryStrip();
            e.consume();
        });
        StackPane.setAlignment(deleteBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(deleteBtn, new Insets(2, 2, 0, 0));
        deleteBtn.setVisible(false);

        card.setOnMouseEntered(e -> {
            deleteBtn.setVisible(true);
            card.setEffect(new DropShadow(8, Color.rgb(191, 162, 0, 0.5)));
        });
        card.setOnMouseExited(e -> {
            deleteBtn.setVisible(false);
            card.setEffect(null);
        });

        card.getChildren().addAll(iv, deleteBtn);
        card.setCursor(javafx.scene.Cursor.HAND);

        // Click to show full preview
        card.setOnMouseClicked(e -> {
            if (e.getPickResult().getIntersectedNode() == deleteBtn) return;
            showImagePreviewPopup(imagePath);
        });

        return card;
    }

    /**
     * Show a full-size image preview in a dialog.
     */
    private void showImagePreviewPopup(String imagePath) {
        try {
            File f = new File(imagePath);
            if (!f.exists()) return;
            Image img = new Image(f.toURI().toString(), 800, 600, true, true);
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(true);
            iv.setFitWidth(700);
            iv.setFitHeight(500);

            Dialog<Void> preview = new Dialog<>();
            preview.setTitle("Aperçu de l'image");
            DialogPane dp = new DialogPane();
            dp.setContent(new StackPane(iv));
            dp.getButtonTypes().add(ButtonType.CLOSE);
            dp.setStyle("-fx-background-color: #0a0a14;");
            preview.setDialogPane(dp);
            preview.setResizable(true);
            preview.showAndWait();
        } catch (Exception e) {
            System.err.println("❌ Error showing preview: " + e.getMessage());
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

        // Load the legacy single image into gallery if present
        if (l.getImage() != null && !l.getImage().trim().isEmpty()) {
            File imgFile = new File(l.getImage());
            if (imgFile.exists()) {
                galleryImages.add(l.getImage());
            }
        }
    }

    /**
     * Load existing gallery images from DB for an existing lieu.
     */
    public void loadExistingGalleryImages(List<String> existingPaths) {
        if (existingPaths != null) {
            for (String p : existingPaths) {
                if (p != null && !p.trim().isEmpty()) {
                    if (!galleryImages.contains(p)) {
                        galleryImages.add(p);
                    }
                }
            }
        }
        refreshGalleryStrip();
    }

    /**
     * Get the list of gallery image paths.
     */
    public List<String> getGalleryImagePaths() {
        return new ArrayList<>(galleryImages);
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

        // Use first gallery image as the main image (backward compatible)
        String image = galleryImages.isEmpty() ? "" : galleryImages.get(0);
        int idCategorie = selectedCat.getIdcategorie();
        int idAdresse = selectedAdr.getId_adresse();

        return new LieuTouristique(nom, description, ville, idAdresse, prix, image, statut, idCategorie);
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
