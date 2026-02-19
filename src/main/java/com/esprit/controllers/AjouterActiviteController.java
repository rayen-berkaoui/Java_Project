package com.esprit.controllers;

import com.esprit.entities.Activite;
import com.esprit.services.ActiviteServices;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AjouterActiviteController {

    @FXML private TextField nomField;
    @FXML private ComboBox<String> categorieBox;
    @FXML private ComboBox<String> niveauBox;
    @FXML private TextField dureeField;
    @FXML private TextArea descArea;
    @FXML private TextField imageUrlField;

    // ✅ optionnel (si tu l’ajoutes dans FXML)
    @FXML private Label messageLabel;

    // ✅ important (si tu veux disable)
    @FXML private Button saveBtn;

    private final ActiviteServices service = new ActiviteServices();

    @FXML
    public void initialize() {
        categorieBox.setItems(FXCollections.observableArrayList("Sport", "Culture", "Loisir", "Nature", "Autre"));
        niveauBox.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé"));

        // Mode edit
        if (ActiviteController.activiteToEdit != null) {
            Activite a = ActiviteController.activiteToEdit;
            nomField.setText(safe(a.getNomActivite()));
            categorieBox.setValue(safe(a.getCategorie()).isBlank() ? null : a.getCategorie());
            niveauBox.setValue(safe(a.getNiveau()).isBlank() ? null : a.getNiveau());
            dureeField.setText(a.getDuree() == null ? "" : String.valueOf(a.getDuree()));
            descArea.setText(safe(a.getDescription()));
            imageUrlField.setText(safe(a.getImageUrl()));
        }

        // listeners validation live
        nomField.textProperty().addListener((o,a,b)-> validateAll());
        categorieBox.valueProperty().addListener((o,a,b)-> validateAll());
        niveauBox.valueProperty().addListener((o,a,b)-> validateAll());
        dureeField.textProperty().addListener((o,a,b)-> validateAll());
        descArea.textProperty().addListener((o,a,b)-> validateAll());
        imageUrlField.textProperty().addListener((o,a,b)-> validateAll());

        validateAll();
    }

    @FXML
    private void onSave(ActionEvent event) {
        if (!validateAll()) return;

        try {
            String nom = nomField.getText().trim();
            String cat = categorieBox.getValue();
            String niv = niveauBox.getValue();
            String desc = safe(descArea.getText()).trim();
            String imageUrl = safe(imageUrlField.getText()).trim(); // accepte tout

            Integer duree = null;
            String dTxt = safe(dureeField.getText()).trim();
            if (!dTxt.isEmpty()) {
                duree = Integer.parseInt(dTxt);
            }

            if (ActiviteController.activiteToEdit == null) {
                Activite a = new Activite(nom, desc, cat, duree, niv, imageUrl);
                service.ajouter(a);
            } else {
                Activite a = ActiviteController.activiteToEdit;
                a.setNomActivite(nom);
                a.setCategorie(cat);
                a.setNiveau(niv);
                a.setDescription(desc);
                a.setDuree(duree);
                a.setImageUrl(imageUrl);

                service.modifier(a);
                ActiviteController.activiteToEdit = null;
            }

            // ✅ après sauvegarde -> retourne à la liste
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

        // Nom obligatoire 1..20
        String nom = safe(nomField.getText()).trim();
        if (nom.isBlank()) {
            errors.append("• Nom obligatoire.\n");
            markError(nomField);
        } else if (nom.length() > 20) {
            errors.append("• Nom : max 20 caractères.\n");
            markError(nomField);
        }

        // Catégorie obligatoire
        String cat = categorieBox.getValue();
        if (cat == null || cat.isBlank()) {
            errors.append("• Catégorie obligatoire.\n");
            markError(categorieBox);
        }

        // Niveau obligatoire
        String niv = niveauBox.getValue();
        if (niv == null || niv.isBlank()) {
            errors.append("• Niveau obligatoire.\n");
            markError(niveauBox);
        }

        // Durée : optionnelle mais si remplie -> int > 0
        String dTxt = safe(dureeField.getText()).trim();
        if (!dTxt.isEmpty()) {
            try {
                int d = Integer.parseInt(dTxt);
                if (d <= 0) {
                    errors.append("• Durée : doit être > 0.\n");
                    markError(dureeField);
                }
            } catch (NumberFormatException ex) {
                errors.append("• Durée invalide (ex: 60).\n");
                markError(dureeField);
            }
        }

        // Description max 200
        String desc = safe(descArea.getText()).trim();
        if (desc.length() > 200) {
            errors.append("• Description : max 200 caractères.\n");
            markError(descArea);
        }

        // Image : accepte tout -> pas de validation

        boolean ok = errors.length() == 0;

        if (saveBtn != null) saveBtn.setDisable(!ok);

        if (messageLabel != null) {
            if (ok) {
                messageLabel.setText("");
                messageLabel.setStyle("-fx-text-fill:#22c55e; -fx-font-weight:bold;");
            } else {
                messageLabel.setText(errors.toString().trim());
                messageLabel.setStyle("-fx-text-fill:#ef4444; -fx-font-weight:bold;");
            }
        }

        return ok;
    }

    private void clearErrors() {
        removeError(nomField);
        removeError(categorieBox);
        removeError(niveauBox);
        removeError(dureeField);
        removeError(descArea);
        removeError(imageUrlField);
    }

    private void markError(Control c) {
        if (!c.getStyleClass().contains("field-error")) c.getStyleClass().add("field-error");
    }

    private void removeError(Control c) {
        c.getStyleClass().remove("field-error");
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String safe(String s) { return s == null ? "" : s; }
}
