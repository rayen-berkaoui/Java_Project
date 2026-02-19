package com.esprit.controllers;

import com.esprit.entities.Etablissement;
import com.esprit.services.EtablissementServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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

    // Email obligatoire + valide
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Téléphone tunisien : 8 chiffres, commence par 2/3/4/5/7/9
    private static final Pattern TN_PHONE_PATTERN =
            Pattern.compile("^[234579][0-9]{7}$");

    @FXML
    public void initialize() {

        gammePrixField.getItems().setAll("€", "€€", "€€€");

        editing = AffichageEtablissementController.etablissementToEdit;

        if (editing != null) {
            titleLabel.setText("Modifier un établissement");
            fillForm(editing);
        } else {
            titleLabel.setText("Ajouter un établissement");
        }

        // validation live
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

        // téléphone تونس : on garde brut (sans espaces)
        String tel = safe(telephoneField.getText()).trim().replaceAll("\\s+", "");
        e.setTelephone(tel);

        e.setEmail(emailField.getText().trim());
        e.setHoraires(safe(horairesField.getText()).trim());
        e.setGammePrix(gammePrixField.getValue() == null ? "" : gammePrixField.getValue().trim());

        // ✅ image accepte tout
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

        // NOM obligatoire 1..20
        String nom = safe(nomField.getText()).trim();
        if (nom.isBlank()) {
            errors.append("• Nom obligatoire.\n");
            markError(nomField);
        } else if (nom.length() < 1 || nom.length() > 20) {
            errors.append("• Nom : 1 à 20 caractères.\n");
            markError(nomField);
        }

        // VILLE obligatoire 1..20
        String ville = safe(villeField.getText()).trim();
        if (ville.isBlank()) {
            errors.append("• Ville obligatoire.\n");
            markError(villeField);
        } else if (ville.length() < 1 || ville.length() > 20) {
            errors.append("• Ville : 1 à 20 caractères.\n");
            markError(villeField);
        }

        // EMAIL obligatoire + valide
        String email = safe(emailField.getText()).trim();
        if (email.isBlank()) {
            errors.append("• Email obligatoire.\n");
            markError(emailField);
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.append("• Email invalide (ex: nom@gmail.com).\n");
            markError(emailField);
        }

        // TELEPHONE tunisien optionnel
        String tel = safe(telephoneField.getText()).trim().replaceAll("\\s+", "");
        if (!tel.isBlank()) {
            if (!TN_PHONE_PATTERN.matcher(tel).matches()) {
                errors.append("• Téléphone tunisian chiffres et commence par 2/3/4/5/7/9.\n");
                markError(telephoneField);
            }
        }

        // DESCRIPTION max 200 (tu peux garder)
        String desc = safe(descriptionArea.getText()).trim();
        if (desc.length() > 200) {
            errors.append("• Description : max 200 caractères.\n");
            markError(descriptionArea);
        }

        // ✅ IMAGE : accepte tout -> aucune validation

        boolean ok = errors.length() == 0;
        if (saveBtn != null) saveBtn.setDisable(!ok);

        if (ok) setMessage("", true);
        else setMessage(errors.toString().trim(), false);

        return ok;
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
        if (!c.getStyleClass().contains("field-error")) c.getStyleClass().add("field-error");
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
