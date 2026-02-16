package com.esprit.controllers;

import com.esprit.entities.categorie;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.LocalDate;

public class CategorieFormDialogController {

    @FXML private TextField tfNom, tfDescription;
    @FXML private Label lblErrNom, lblErrDesc;

    private String mode;

    @FXML
    public void initialize() {
        // Real-time validation listeners
        if (tfNom != null) tfNom.textProperty().addListener((obs, o, n) -> validateNom(n));
        if (tfDescription != null) tfDescription.textProperty().addListener((obs, o, n) -> validateDescription(n));
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setCategorie(categorie c) {
        if (c != null) {
            if (tfNom != null) tfNom.setText(c.getNomcategorie() != null ? c.getNomcategorie() : "");
            if (tfDescription != null) tfDescription.setText(c.getDescription() != null ? c.getDescription() : "");
        }
    }

    public categorie getCategorie() {
        if (tfNom == null || tfDescription == null) {
            System.err.println("❌ Form fields not initialized");
            return null;
        }

        boolean valid = true;

        // --- Nom ---
        String nom = tfNom.getText().trim();
        if (nom.isEmpty()) {
            showFieldError(tfNom, lblErrNom, "Le nom est obligatoire");
            valid = false;
        } else if (nom.length() < 2) {
            showFieldError(tfNom, lblErrNom, "Minimum 2 caractères");
            valid = false;
        } else if (nom.length() > 50) {
            showFieldError(tfNom, lblErrNom, "Maximum 50 caractères");
            valid = false;
        } else if (!nom.matches("[a-zA-ZÀ-ÿ0-9\\s\\-'&]+")) {
            showFieldError(tfNom, lblErrNom, "Caractères spéciaux non autorisés");
            valid = false;
        } else {
            showFieldValid(tfNom, lblErrNom);
        }

        // --- Description ---
        String description = tfDescription.getText().trim();
        if (description.isEmpty()) {
            showFieldError(tfDescription, lblErrDesc, "La description est obligatoire");
            valid = false;
        } else if (description.length() < 5) {
            showFieldError(tfDescription, lblErrDesc, "Minimum 5 caractères");
            valid = false;
        } else {
            showFieldValid(tfDescription, lblErrDesc);
        }

        if (!valid) return null;

        return new categorie(nom, description, LocalDate.now());
    }

    // ===== Real-time validators =====

    private void validateNom(String val) {
        if (val == null) return;
        String v = val.trim();
        if (v.isEmpty()) {
            clearFieldState(tfNom, lblErrNom);
        } else if (v.length() < 2) {
            showFieldError(tfNom, lblErrNom, "Minimum 2 caractères");
        } else if (v.length() > 50) {
            showFieldError(tfNom, lblErrNom, "Maximum 50 caractères");
        } else if (!v.matches("[a-zA-ZÀ-ÿ0-9\\s\\-'&]+")) {
            showFieldError(tfNom, lblErrNom, "Caractères spéciaux non autorisés");
        } else {
            showFieldValid(tfNom, lblErrNom);
        }
    }

    private void validateDescription(String val) {
        if (val == null) return;
        String v = val.trim();
        if (v.isEmpty()) {
            clearFieldState(tfDescription, lblErrDesc);
        } else if (v.length() < 5) {
            showFieldError(tfDescription, lblErrDesc, "Minimum 5 caractères");
        } else {
            showFieldValid(tfDescription, lblErrDesc);
        }
    }

    // ===== Styling helpers =====

    private void showFieldError(TextField field, Label errLabel, String msg) {
        if (field != null) {
            field.getStyleClass().removeAll("input-error", "input-valid");
            field.getStyleClass().add("input-error");
        }
        if (errLabel != null) { errLabel.setText(msg); errLabel.setVisible(true); errLabel.setManaged(true); }
    }

    private void showFieldValid(TextField field, Label errLabel) {
        if (field != null) {
            field.getStyleClass().removeAll("input-error", "input-valid");
            field.getStyleClass().add("input-valid");
        }
        if (errLabel != null) { errLabel.setVisible(false); errLabel.setManaged(false); }
    }

    private void clearFieldState(TextField field, Label errLabel) {
        if (field != null) field.getStyleClass().removeAll("input-error", "input-valid");
        if (errLabel != null) { errLabel.setVisible(false); errLabel.setManaged(false); }
    }
}
