package com.esprit.controllers;

import com.esprit.entities.Adresse;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * Controller for Adresse (Address) form dialog.
 * Handles bidirectional data mapping between form fields and Adresse entity.
 */
public class AdresseFormDialogController {

    @FXML
    private TextField tfRue, tfVille, tfLatitude, tfLongitude, tfAltitude;

    private String mode; // "ADD" or "EDIT"

    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Populates form fields with existing address data for editing.
     * @param a The address to edit
     */
    public void setAdresse(Adresse a) {
        if (a == null) {
            System.err.println("❌ Adresse is null");
            return;
        }
        
        if (tfRue != null) tfRue.setText(a.getRue() != null ? a.getRue() : "");
        if (tfVille != null) tfVille.setText(a.getVille() != null ? a.getVille() : "");
        if (tfLatitude != null) tfLatitude.setText(String.valueOf(a.getLatitude()));
        if (tfLongitude != null) tfLongitude.setText(String.valueOf(a.getLongitude()));
        if (tfAltitude != null) tfAltitude.setText(String.valueOf(a.getAltitude()));
    }

    /**
     * Extracts form data and creates an Adresse object.
     * Validates that required fields are not empty and coordinates are valid numbers.
     * @return A new Adresse object, or null if validation fails
     */
    public Adresse getAdresse() {
        if (tfRue == null || tfVille == null || tfLatitude == null || tfLongitude == null || tfAltitude == null) {
            System.err.println("❌ One or more form fields are not initialized");
            return null;
        }
        
        try {
            String rue = tfRue.getText().trim();
            String ville = tfVille.getText().trim();
            double latitude = Double.parseDouble(tfLatitude.getText().trim());
            double longitude = Double.parseDouble(tfLongitude.getText().trim());
            double altitude = Double.parseDouble(tfAltitude.getText().trim());

            if (rue.isEmpty() || ville.isEmpty()) {
                System.err.println("⚠️ Street or city cannot be empty");
                return null;
            }

            return new Adresse(rue, ville, latitude, longitude, altitude);
        } catch (NumberFormatException e) {
            System.err.println("❌ Error parsing coordinates: " + e.getMessage());
            return null;
        }
    }
}
