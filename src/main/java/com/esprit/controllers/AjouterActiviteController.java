package com.esprit.controllers;

import com.esprit.entities.Activite;
import com.esprit.services.ActiviteServices;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.Set;

public class AjouterActiviteController {

    @FXML private Label titleLabel;
    @FXML private Label messageLabel;

    @FXML private TextField nomField;
    @FXML private ComboBox<String> categorieField;
    @FXML private TextField dureeField;
    @FXML private ComboBox<String> niveauField;
    @FXML private TextArea descriptionArea;

    @FXML private Button btnEnregistrer;

    private final ActiviteServices service = new ActiviteServices();
    private Activite editing = null;

    // règles
    private static final int NOM_MAX = 20;
    private static final int DESC_MAX = 200;

    private static final Set<String> CATEGORIES = Set.of("Sport", "Culture", "Loisir", "Nature", "Autre");
    private static final Set<String> NIVEAUX = Set.of("Débutant", "Intermédiaire", "Avancé");

    @FXML
    public void initialize() {

        categorieField.getItems().setAll(CATEGORIES);
        niveauField.getItems().setAll(NIVEAUX);

        // Durée: chiffres seulement (live)
        dureeField.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            return newText.matches("\\d*") ? change : null;
        }));

        // mode modification / ajout
        editing = ActiviteController.activiteToEdit;
        if (editing != null) {
            titleLabel.setText("Modifier une activité");
            nomField.setText(editing.getNomActivite());
            categorieField.setValue(editing.getCategorie());
            dureeField.setText(editing.getDuree() == null ? "" : String.valueOf(editing.getDuree()));
            niveauField.setValue(editing.getNiveau());
            descriptionArea.setText(editing.getDescription());
        } else {
            titleLabel.setText("Ajouter une activité");
        }

        // validation live: chaque changement revalide et (dés)active Enregistrer
        ChangeListener<Object> live = (obs, o, n) -> validateFormAndUpdateUI();
        nomField.textProperty().addListener(live);
        categorieField.valueProperty().addListener(live);
        dureeField.textProperty().addListener(live);
        niveauField.valueProperty().addListener(live);
        descriptionArea.textProperty().addListener(live);

        validateFormAndUpdateUI(); // état initial
    }

    @FXML
    private void onEnregistrer(ActionEvent event) {
        // sécurité: si invalide, on ne fait rien
        if (!validateFormAndUpdateUI()) return;

        Activite a = buildActiviteFromForm();

        try {
            if (editing == null) {
                service.ajouter(a);
                setMessage("✅ Activité ajoutée.", true);
            } else {
                a.setIdActivite(editing.getIdActivite());
                service.modifier(a);
                setMessage("✅ Activité modifiée.", true);
            }
            NavigationUtils.goTo("/activite_view.fxml", event);

        } catch (SQLException ex) {
            setMessage("❌ " + ex.getMessage(), false);
        }
    }

    @FXML
    private void onAnnuler(ActionEvent event) {
        NavigationUtils.goTo("/activite_view.fxml", event);
    }

    // ================= VALIDATION =================
    private boolean validateFormAndUpdateUI() {
        clearAllErrors();
        setMessage("", true);

        boolean ok = true;

        String nom = safe(nomField.getText()).trim();
        String cat = categorieField.getValue() == null ? "" : categorieField.getValue().trim();
        String dureeStr = safe(dureeField.getText()).trim();
        String niv = niveauField.getValue() == null ? "" : niveauField.getValue().trim();
        String desc = safe(descriptionArea.getText()).trim();

        // Nom obligatoire + max 20
        if (nom.isBlank()) {
            setError(nomField, "❌ Nom obligatoire.");
            ok = false;
        } else if (nom.length() > NOM_MAX) {
            setError(nomField, "❌ Nom trop long (max " + NOM_MAX + " caractères).");
            ok = false;
        }

        // Catégorie obligatoire
        if (cat.isBlank()) {
            setError(categorieField, "❌ Catégorie obligatoire.");
            ok = false;
        } else if (!CATEGORIES.contains(cat)) {
            setError(categorieField, "❌ Catégorie invalide.");
            ok = false;
        }

        // Durée: chiffres, >0 et <120 (optionnelle ? tu peux la rendre obligatoire si tu veux)
        if (!dureeStr.isBlank()) {
            try {
                int d = Integer.parseInt(dureeStr);
                if (d <= 0 || d >= 120) {
                    setError(dureeField, "❌ Durée doit être entre 1 et 119 minutes.");
                    ok = false;
                }
            } catch (NumberFormatException e) {
                setError(dureeField, "❌ Durée invalide (chiffres uniquement).");
                ok = false;
            }
        }

        // Niveau optionnel mais contrôlé
        if (!niv.isBlank() && !NIVEAUX.contains(niv)) {
            setError(niveauField, "❌ Niveau invalide.");
            ok = false;
        }

        // Description max 200
        if (desc.length() > DESC_MAX) {
            setError(descriptionArea, "❌ Description trop longue (max " + DESC_MAX + " caractères).");
            ok = false;
        }

        // Désactiver Enregistrer si invalide
        if (btnEnregistrer != null) btnEnregistrer.setDisable(!ok);

        return ok;
    }

    private Activite buildActiviteFromForm() {
        String nom = safe(nomField.getText()).trim();
        String cat = categorieField.getValue().trim();
        String dureeStr = safe(dureeField.getText()).trim();
        String niv = niveauField.getValue() == null ? null : niveauField.getValue().trim();
        String desc = safe(descriptionArea.getText()).trim();

        Integer duree = dureeStr.isBlank() ? null : Integer.parseInt(dureeStr);

        Activite a = new Activite();
        a.setNomActivite(nom);
        a.setCategorie(cat);
        a.setDuree(duree);
        a.setNiveau(niv == null || niv.isBlank() ? null : niv);
        a.setDescription(desc.isBlank() ? null : desc);
        return a;
    }

    // ================= UI HELPERS =================
    private void setError(Control c, String msg) {
        c.getStyleClass().add("field-error");
        setMessage(msg, false);
    }

    private void clearAllErrors() {
        nomField.getStyleClass().remove("field-error");
        categorieField.getStyleClass().remove("field-error");
        dureeField.getStyleClass().remove("field-error");
        niveauField.getStyleClass().remove("field-error");
        descriptionArea.getStyleClass().remove("field-error");
    }

    private void setMessage(String msg, boolean ok) {
        messageLabel.setText(msg == null ? "" : msg);
        messageLabel.setStyle(ok
                ? "-fx-text-fill:#22c55e; -fx-font-weight:bold;"
                : "-fx-text-fill:#ef4444; -fx-font-weight:bold;");
    }

    private String safe(String s) { return s == null ? "" : s; }
}
