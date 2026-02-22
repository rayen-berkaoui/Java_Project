package com.esprit.controllers;

import com.esprit.entities.Activite;
import com.esprit.entities.ActiviteImage;
import com.esprit.services.ActiviteImageServices;
import com.esprit.services.ActiviteServices;
// ✅ A AJOUTER : ton service établissement
// import com.esprit.services.EtablissementServices;
// import com.esprit.entities.Etablissement;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.*;
import java.util.*;

public class AjouterActiviteController {

    // ====== FXML ======
    @FXML private ComboBox<EtablissementItem> etablissementBox;
    @FXML private Label etablissementError;

    @FXML private TextField nomField;
    @FXML private ComboBox<String> categorieBox;
    @FXML private ComboBox<String> niveauBox;
    @FXML private TextField dureeField;
    @FXML private TextArea descArea;

    @FXML private Label messageLabel;
    @FXML private Button saveBtn;

    @FXML private HBox imagesStrip;

    // ====== Errors ======
    @FXML private Label nomError;
    @FXML private Label categorieError;
    @FXML private Label niveauError;
    @FXML private Label dureeError;
    @FXML private Label descError;

    // ====== Services ======
    private final ActiviteServices service = new ActiviteServices();
    private final ActiviteImageServices imageService = new ActiviteImageServices();

    // private final EtablissementServices etabService = new EtablissementServices();

    private final List<File> selectedImageFiles = new ArrayList<>();

    @FXML
    public void initialize() {
        // ✅ combos
        categorieBox.setItems(FXCollections.observableArrayList("Sport", "Culture", "Loisir", "Nature", "Autre"));
        niveauBox.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé"));

        // ✅ charger établissements (FK)
        loadEtablissements();

        // ✅ mode edit
        if (ActiviteController.activiteToEdit != null) {
            Activite a = ActiviteController.activiteToEdit;
            nomField.setText(safe(a.getNomActivite()));
            categorieBox.setValue(blankToNull(a.getCategorie()));
            niveauBox.setValue(blankToNull(a.getNiveau()));
            dureeField.setText(a.getDuree() == null ? "" : String.valueOf(a.getDuree()));
            descArea.setText(safe(a.getDescription()));

            // ✅ IMPORTANT : sélectionner établissement si tu as idEtablissement dans Activite
            // Exemple :
            // selectEtablissementById(a.getIdEtablissement());
        }

        // ✅ validation live
        nomField.textProperty().addListener((o, a, b) -> validateAll());
        categorieBox.valueProperty().addListener((o, a, b) -> validateAll());
        niveauBox.valueProperty().addListener((o, a, b) -> validateAll());
        dureeField.textProperty().addListener((o, a, b) -> validateAll());
        descArea.textProperty().addListener((o, a, b) -> validateAll());
        etablissementBox.valueProperty().addListener((o, a, b) -> validateAll());

        validateAll();
        rebuildImagesStrip();
    }

    // ================== ETABLISSEMENTS (FK) ==================

    private void loadEtablissements() {
        // ✅ Affichage dans ComboBox
        etablissementBox.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(EtablissementItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNom());
            }
        });
        etablissementBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(EtablissementItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Choisir un établissement" : item.getNom());
            }
        });

        // ✅ TODO : remplace ça par ta vraie lecture DB
        // Exemple attendu :
        // List<Etablissement> list = etabService.getAll();
        // List<EtablissementItem> items = list.stream()
        //     .map(e -> new EtablissementItem(e.getIdEtablissement(), e.getNom()))
        //     .toList();
        // etablissementBox.setItems(FXCollections.observableArrayList(items));

        // --- DEMO TEMPORAIRE (à supprimer quand tu branches DB) ---
        etablissementBox.setItems(FXCollections.observableArrayList(
                new EtablissementItem(1, "Etablissement A"),
                new EtablissementItem(2, "Etablissement B")
        ));
    }

    // private void selectEtablissementById(Integer id) {
    //     if (id == null) return;
    //     for (EtablissementItem it : etablissementBox.getItems()) {
    //         if (it.getId() == id) {
    //             etablissementBox.getSelectionModel().select(it);
    //             return;
    //         }
    //     }
    // }

    // ================= IMAGES =================

    @FXML
    private void onChooseImages(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir des images");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        List<File> files = fc.showOpenMultipleDialog(nomField.getScene().getWindow());
        if (files == null || files.isEmpty()) return;

        // ✅ sans doublons
        for (File f : files) {
            if (f == null || !f.exists()) continue;
            boolean exists = selectedImageFiles.stream()
                    .anyMatch(x -> x.getAbsolutePath().equalsIgnoreCase(f.getAbsolutePath()));
            if (!exists) selectedImageFiles.add(f);
        }

        rebuildImagesStrip();
    }

    private void rebuildImagesStrip() {
        if (imagesStrip == null) return;

        imagesStrip.getChildren().clear();

        for (File f : selectedImageFiles) {
            ImageView iv = new ImageView(new Image(f.toURI().toString(), 110, 75, true, true));
            iv.setFitWidth(110);
            iv.setFitHeight(75);
            iv.setPreserveRatio(true);

            StackPane thumb = new StackPane(iv);
            thumb.getStyleClass().add("gallery-thumb-card");

            Label del = new Label("✕");
            del.getStyleClass().add("gallery-thumb-delete");
            del.setOnMouseClicked(e -> {
                selectedImageFiles.remove(f);   // ✅ suppression PAR FICHIER (pas index)
                rebuildImagesStrip();
            });

            StackPane wrap = new StackPane(thumb, del);
            StackPane.setAlignment(del, Pos.TOP_RIGHT);

            imagesStrip.getChildren().add(wrap);
        }
    }

    /**
     * ⚠️ CONSEIL : ne copie pas dans src/main/resources en runtime (ça marche mal après packaging JAR).
     * Ici je copie dans un dossier "uploads/activites" dans le dossier du projet.
     */
    private String copyImageToUploads(File src) throws Exception {
        String original = src.getName();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot);

        String newName = "act_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8) + ext;

        Path targetDir = Paths.get("uploads", "activites");
        if (!Files.exists(targetDir)) Files.createDirectories(targetDir);

        Path target = targetDir.resolve(newName);
        Files.copy(src.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

        return newName; // on sauvegarde ce nom dans DB
    }

    private void saveImagesToDB(int idActivite) throws Exception {
        if (idActivite <= 0 || selectedImageFiles.isEmpty()) return;

        int ordre = 1;
        for (File imgFile : selectedImageFiles) {
            String storedName = copyImageToUploads(imgFile);

            ActiviteImage img = new ActiviteImage();
            img.setIdActivite(idActivite);
            img.setImagePath(storedName);
            img.setOrdreAffichage(ordre++);

            imageService.ajouterImage(img);
        }
    }

    // ================= SAVE =================

    @FXML
    private void onSave(ActionEvent event) {
        if (!validateAll()) return;

        try {
            // ✅ FK : idEtablissement
            EtablissementItem etab = etablissementBox.getValue();
            int idEtablissement = etab.getId();

            String nom = safe(nomField.getText()).trim();
            String cat = categorieBox.getValue();
            String niv = niveauBox.getValue();
            String desc = safe(descArea.getText()).trim();
            Integer duree = parseIntOrNull(dureeField.getText());

            Activite a = (ActiviteController.activiteToEdit == null)
                    ? new Activite()
                    : ActiviteController.activiteToEdit;

            a.setNomActivite(nom);
            a.setCategorie(cat);
            a.setNiveau(niv);
            a.setDuree(duree);
            a.setDescription(desc);

            // ✅ IMPORTANT : tu dois avoir ce champ dans Activite
            // et dans ta table activite : idEtablissement (FK)
            a.setIdEtablissement(idEtablissement);

            if (a.getDevise() == null || a.getDevise().isBlank()) a.setDevise("TND");
            if (a.getStatut() == null || a.getStatut().isBlank()) a.setStatut("disponible");

            if (ActiviteController.activiteToEdit == null) {
                int newId = service.ajouterEtRetournerId(a);
                saveImagesToDB(newId);
                setMessage("✅ Activité ajoutée.", true);
            } else {
                service.modifier(a);
                if (!selectedImageFiles.isEmpty()) saveImagesToDB(a.getIdActivite());
                ActiviteController.activiteToEdit = null;
                setMessage("✅ Activité modifiée.", true);
            }

            NavigationUtils.goTo("/activite_view.fxml", event);

        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel(ActionEvent event) {
        ActiviteController.activiteToEdit = null;
        NavigationUtils.goTo("/activite_view.fxml", event);
    }

    // ================= VALIDATION =================

    private boolean validateAll() {
        clearErrors();
        StringBuilder errors = new StringBuilder();

        // ✅ etablissement obligatoire
        if (etablissementBox.getValue() == null) {
            errors.append("• Établissement obligatoire.\n");
            markError(etablissementBox);
            if (etablissementError != null) etablissementError.setText("Choisis un établissement.");
        }

        String nom = safe(nomField.getText()).trim();
        if (nom.isBlank()) {
            errors.append("• Nom obligatoire.\n");
            markError(nomField);
            if (nomError != null) nomError.setText("Nom obligatoire.");
        } else if (nom.length() > 120) {
            errors.append("• Nom : max 120 caractères.\n");
            markError(nomField);
            if (nomError != null) nomError.setText("Max 120 caractères.");
        }

        String cat = categorieBox.getValue();
        if (cat == null || cat.isBlank()) {
            errors.append("• Catégorie obligatoire.\n");
            markError(categorieBox);
            if (categorieError != null) categorieError.setText("Catégorie obligatoire.");
        }

        String niv = niveauBox.getValue();
        if (niv == null || niv.isBlank()) {
            errors.append("• Niveau obligatoire.\n");
            markError(niveauBox);
            if (niveauError != null) niveauError.setText("Niveau obligatoire.");
        }

        String dTxt = safe(dureeField.getText()).trim();
        if (!dTxt.isEmpty()) {
            Integer d = parseIntOrNull(dTxt);
            if (d == null) {
                errors.append("• Durée invalide (ex: 60).\n");
                markError(dureeField);
                if (dureeError != null) dureeError.setText("Durée invalide.");
            } else if (d <= 0) {
                errors.append("• Durée : doit être > 0.\n");
                markError(dureeField);
                if (dureeError != null) dureeError.setText("Doit être > 0.");
            }
        }

        String desc = safe(descArea.getText()).trim();
        if (desc.length() > 5000) {
            errors.append("• Description trop longue.\n");
            markError(descArea);
            if (descError != null) descError.setText("Trop long.");
        }

        boolean ok = errors.length() == 0;

        if (saveBtn != null) saveBtn.setDisable(!ok);

        if (messageLabel != null) {
            if (ok) setMessage("", true);
            else setMessage(errors.toString().trim(), false);
        }

        return ok;
    }

    private void clearErrors() {
        removeError(etablissementBox);
        removeError(nomField);
        removeError(categorieBox);
        removeError(niveauBox);
        removeError(dureeField);
        removeError(descArea);

        if (etablissementError != null) etablissementError.setText("");
        if (nomError != null) nomError.setText("");
        if (categorieError != null) categorieError.setText("");
        if (niveauError != null) niveauError.setText("");
        if (dureeError != null) dureeError.setText("");
        if (descError != null) descError.setText("");
    }

    private void markError(Control c) {
        if (c != null && !c.getStyleClass().contains("input-error")) c.getStyleClass().add("input-error");
    }

    private void removeError(Control c) {
        if (c != null) c.getStyleClass().remove("input-error");
    }

    private void setMessage(String msg, boolean ok) {
        if (messageLabel == null) return;
        messageLabel.setText(msg == null ? "" : msg);
        messageLabel.setStyle(ok
                ? "-fx-text-fill:#22c55e; -fx-font-weight:bold;"
                : "-fx-text-fill:#ef4444; -fx-font-weight:bold;");
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private Integer parseIntOrNull(String s) {
        try {
            String v = safe(s).trim();
            if (v.isBlank()) return null;
            return Integer.parseInt(v);
        } catch (Exception e) {
            return null;
        }
    }

    private String blankToNull(String s) {
        String v = safe(s).trim();
        return v.isBlank() ? null : v;
    }

    private String safe(String s) { return s == null ? "" : s; }

    // ================== MODEL FOR COMBO ==================
    public static class EtablissementItem {
        private final int id;
        private final String nom;

        public EtablissementItem(int id, String nom) {
            this.id = id;
            this.nom = nom;
        }

        public int getId() { return id; }
        public String getNom() { return nom; }

        @Override
        public String toString() { return nom; }
    }
}