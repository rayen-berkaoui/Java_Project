package com.esprit.controllers;

import com.esprit.entities.Etablissement;
import com.esprit.services.EtablissementServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class AjouterEtablissementController {

    private Etablissement editing = null;

    @FXML private Label titleLabel;
    @FXML private Label messageLabel;

    @FXML private TextField nomField;
    @FXML private TextField adresseField;
    @FXML private TextField villeField;
    @FXML private TextField telephoneField;
    @FXML private TextField emailField;
    @FXML private TextField horairesField;
    @FXML private ComboBox<String> gammePrixField;
    @FXML private TextField imageUrlField;
    @FXML private TextArea descriptionArea;

    @FXML private Button saveBtn;

    private final EtablissementServices service = new EtablissementServices();

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+ ]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML
    public void initialize() {

        // options
        gammePrixField.getItems().setAll("€", "€€", "€€€");

        // récupérer l’établissement à modifier (comme activité)
        editing = AffichageEtablissementController.etablissementToEdit;

        if (editing != null) {
            titleLabel.setText("Modifier un établissement");
            fillForm(editing);
        } else {
            titleLabel.setText("Ajouter un établissement");
        }

        // listeners validation live
        nomField.textProperty().addListener((o,a,b)-> validateAll());
        villeField.textProperty().addListener((o,a,b)-> validateAll());
        telephoneField.textProperty().addListener((o,a,b)-> validateAll());
        emailField.textProperty().addListener((o,a,b)-> validateAll());
        horairesField.textProperty().addListener((o,a,b)-> validateAll());
        gammePrixField.valueProperty().addListener((o,a,b)-> validateAll());
        imageUrlField.textProperty().addListener((o,a,b)-> validateAll());
        descriptionArea.textProperty().addListener((o,a,b)-> validateAll());
        adresseField.textProperty().addListener((o,a,b)-> validateAll());

        validateAll();
    }

    private void fillForm(Etablissement e) {
        nomField.setText(safe(e.getNom()));
        adresseField.setText(safe(e.getAdresse()));
        villeField.setText(safe(e.getVille()));
        telephoneField.setText(safe(e.getTelephone()));
        emailField.setText(safe(e.getEmail()));
        horairesField.setText(safe(e.getHoraires()));
        gammePrixField.setValue(safe(e.getGammePrix()).isBlank() ? null : e.getGammePrix());
        imageUrlField.setText(safe(e.getImageUrl()));
        descriptionArea.setText(safe(e.getDescription()));
    }

    @FXML
    private void onEnregistrer(ActionEvent event) {
        if (!validateAll()) return;

        Etablissement e = (editing == null) ? new Etablissement() : editing;

        e.setNom(nomField.getText().trim());
        e.setAdresse(safe(adresseField.getText()).trim());
        e.setVille(villeField.getText().trim());
        e.setTelephone(safe(telephoneField.getText()).trim());
        e.setEmail(safe(emailField.getText()).trim());
        e.setHoraires(safe(horairesField.getText()).trim());
        e.setGammePrix(gammePrixField.getValue() == null ? "" : gammePrixField.getValue().trim());
        e.setImageUrl(safe(imageUrlField.getText()).trim());
        e.setDescription(safe(descriptionArea.getText()).trim());

        try {
            if (editing == null) {
                service.ajouter(e);
                setMessage("✅ Établissement ajouté.", true);
            } else {
                service.modifier(e);
                setMessage("✅ Établissement modifié.", true);
            }

            // reset variable
            AffichageEtablissementController.etablissementToEdit = null;

            NavigationUtils.goTo("/etablissement_affichage.fxml", event);

        } catch (Exception ex) {
            setMessage("❌ Erreur: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void onAnnuler(ActionEvent event) {
        AffichageEtablissementController.etablissementToEdit = null;
        NavigationUtils.goTo("/etablissement_affichage.fxml", event);
    }

    // ================= VALIDATION =================

    private boolean validateAll() {
        clearErrors();
        StringBuilder errors = new StringBuilder();

        // NOM obligatoire + max 30
        String nom = safe(nomField.getText()).trim();
        if (nom.isBlank()) {
            errors.append("• Nom obligatoire.\n");
            markError(nomField);
        } else if (nom.length() > 30) {
            errors.append("• Nom : max 30 caractères.\n");
            markError(nomField);
        }

        // VILLE obligatoire + max 30
        String ville = safe(villeField.getText()).trim();
        if (ville.isBlank()) {
            errors.append("• Ville obligatoire.\n");
            markError(villeField);
        } else if (ville.length() > 30) {
            errors.append("• Ville : max 30 caractères.\n");
            markError(villeField);
        }

        // TELEPHONE optionnel : chiffres + + espace, 8..15 chiffres
        String tel = safe(telephoneField.getText()).trim();
        if (!tel.isBlank()) {
            if (!PHONE_PATTERN.matcher(tel).matches()) {
                errors.append("• Téléphone invalide (chiffres, espace, +).\n");
                markError(telephoneField);
            } else {
                String digits = tel.replaceAll("[^0-9]", "");
                if (digits.length() < 8 || digits.length() > 15) {
                    errors.append("• Téléphone : 8 à 15 chiffres.\n");
                    markError(telephoneField);
                }
            }
        }

        // EMAIL optionnel : format valide
        String email = safe(emailField.getText()).trim();
        if (!email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            errors.append("• Email invalide.\n");
            markError(emailField);
        }

        // GAMME PRIX optionnel mais contrôlé
        String gamme = (gammePrixField.getValue() == null) ? "" : gammePrixField.getValue().trim();
        if (!gamme.isBlank() && !(gamme.equals("€") || gamme.equals("€€") || gamme.equals("€€€"))) {
            errors.append("• Gamme prix invalide.\n");
            markError(gammePrixField);
        }

        // DESCRIPTION max 200
        String desc = safe(descriptionArea.getText()).trim();
        if (desc.length() > 200) {
            errors.append("• Description : max 200 caractères.\n");
            markError(descriptionArea);
        }

        // IMAGE URL optionnel : url valide
        String img = safe(imageUrlField.getText()).trim();
        if (!img.isBlank() && !isValidUrl(img)) {
            errors.append("• URL image invalide.\n");
            markError(imageUrlField);
        }

        boolean ok = errors.length() == 0;
        if (saveBtn != null) saveBtn.setDisable(!ok);

        if (ok) setMessage("", true);
        else setMessage(errors.toString().trim(), false);

        return ok;
    }

    private boolean isValidUrl(String s) {
        try {
            new URL(s);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private void clearErrors() {
        removeError(nomField);
        removeError(adresseField);
        removeError(villeField);
        removeError(telephoneField);
        removeError(emailField);
        removeError(horairesField);
        removeError(gammePrixField);
        removeError(imageUrlField);
        removeError(descriptionArea);
    }

    private void markError(Control c) {
        if (!c.getStyleClass().contains("field-error")) {
            c.getStyleClass().add("field-error");
        }
    }

    private void removeError(Control c) {
        c.getStyleClass().remove("field-error");
    }

    private void setMessage(String msg, boolean ok) {
        messageLabel.setText(msg == null ? "" : msg);
        messageLabel.setStyle(ok
                ? "-fx-text-fill:#22c55e; -fx-font-weight:bold;"
                : "-fx-text-fill:#ef4444; -fx-font-weight:bold;");
    }

    private String safe(String s) { return s == null ? "" : s; }
}
