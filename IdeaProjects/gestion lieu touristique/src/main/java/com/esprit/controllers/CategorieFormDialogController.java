package com.esprit.controllers;

import com.esprit.entities.categorie;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import java.time.LocalDate;

public class CategorieFormDialogController {

    @FXML
    private TextField tfNom, tfDescription;

    private String mode;

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
        
        String nom = tfNom.getText().trim();
        String description = tfDescription.getText().trim();

        if (nom.isEmpty()) {
            System.err.println("❌ Category name cannot be empty");
            return null;
        }

        return new categorie(nom, description, LocalDate.now());
    }
}
