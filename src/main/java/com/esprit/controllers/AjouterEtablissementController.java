package com.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class AjouterEtablissementController {

    // ====== FXML fields ======
    @FXML private Label titleLabel;

    @FXML private TextField nomField;
    @FXML private ComboBox<String> typeField;
    @FXML private ComboBox<String> gouvernoratBox;
    @FXML private ComboBox<String> villeBox;

    @FXML private TextField adresseField;

    @FXML private ComboBox<String> phoneCodeBox;
    @FXML private TextField telephoneField;

    @FXML private TextField emailField;
    @FXML private ComboBox<String> gammePrixStarsBox;

    @FXML private ComboBox<String> horairesTemplateBox;
    @FXML private TextField horairesField;

    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;

    @FXML private TextArea descriptionArea;

    @FXML private HBox imagesStrip;
    @FXML private Button saveBtn;

    // ====== Error labels ======
    @FXML private Label nomError;
    @FXML private Label typeError;
    @FXML private Label gouvernoratError;
    @FXML private Label villeError;
    @FXML private Label adresseError;
    @FXML private Label telephoneError;
    @FXML private Label emailError;
    @FXML private Label gammeError;
    @FXML private Label horairesError;
    @FXML private Label latitudeError;
    @FXML private Label longitudeError;
    @FXML private Label descriptionError;

    // ====== Data ======
    private final List<File> selectedImages = new ArrayList<>();
    private final Map<String, List<String>> villesParGouvernorat = new LinkedHashMap<>();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML
    public void initialize() {
        setupData();
        setupCombos();
        setupListeners();
        clearErrors();
        refreshGallery(); // pour être sûr que c’est propre au démarrage
    }

    // ----------------- Setup -----------------
    private void setupData() {
        typeField.setItems(FXCollections.observableArrayList(
                "Hôtel", "Restaurant", "Café", "Musée", "Loisir", "Autre"
        ));

        phoneCodeBox.setItems(FXCollections.observableArrayList(
                "+216", "+33", "+39", "+49", "+34", "+44", "+1"
        ));

        gammePrixStarsBox.setItems(FXCollections.observableArrayList(
                "⭐", "⭐⭐", "⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐⭐⭐"
        ));

        horairesTemplateBox.setItems(FXCollections.observableArrayList(
                "Lun-Ven 08:00-18:00",
                "Tous les jours 09:00-22:00",
                "Mar-Dim 10:00-19:00 (Lundi fermé)",
                "24h/24"
        ));

        villesParGouvernorat.put("Tunis", Arrays.asList("Tunis", "La Marsa", "Carthage", "Le Bardo"));
        villesParGouvernorat.put("Ariana", Arrays.asList("Ariana", "Raoued", "Sidi Thabet"));
        villesParGouvernorat.put("Ben Arous", Arrays.asList("Ben Arous", "Ezzahra", "Rades", "Mourouj"));
        villesParGouvernorat.put("Manouba", Arrays.asList("Manouba", "Denden", "Oued Ellil"));
        villesParGouvernorat.put("Nabeul", Arrays.asList("Nabeul", "Hammamet", "Korba", "Menzel Temime"));
        villesParGouvernorat.put("Sousse", Arrays.asList("Sousse", "Hammam Sousse", "Kalaa Kebira"));
        villesParGouvernorat.put("Sfax", Arrays.asList("Sfax", "Sakiet Ezzit", "Sakiet Eddaier"));
    }

    private void setupCombos() {
        gouvernoratBox.setItems(FXCollections.observableArrayList(villesParGouvernorat.keySet()));
        villeBox.setItems(FXCollections.observableArrayList());

        // Placeholder “propre”
        applyPlaceholder(gouvernoratBox, "Choisir gouvernorat");
        applyPlaceholder(villeBox, "Choisir ville");
        applyPlaceholder(typeField, "hotel / restaurant / cafe / museum ...");
        applyPlaceholder(phoneCodeBox, "+216");

        // Valeur par défaut indicatif
        phoneCodeBox.getSelectionModel().select("+216");
    }

    private void setupListeners() {
        // Gouvernorat -> villes
        gouvernoratBox.valueProperty().addListener((obs, oldV, newV) -> {
            villeBox.getItems().clear();
            clearComboSelection(villeBox);

            if (newV != null && villesParGouvernorat.containsKey(newV)) {
                villeBox.setItems(FXCollections.observableArrayList(villesParGouvernorat.get(newV)));
            }
        });

        // Template horaires -> remplir le champ
        horairesTemplateBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.isBlank()) {
                horairesField.setText(newV);
            }
        });

        // Limite description 200
        descriptionArea.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.length() > 200) {
                descriptionArea.setText(newV.substring(0, 200));
            }
        });
    }

    // Placeholder pour ComboBox (même editable)
    private void applyPlaceholder(ComboBox<String> combo, String placeholder) {
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(placeholder);
                    getStyleClass().add("combo-placeholder");
                } else {
                    setText(item);
                    getStyleClass().remove("combo-placeholder");
                }
            }
        });
    }

    private void clearComboSelection(ComboBox<String> combo) {
        combo.getSelectionModel().clearSelection();
        if (combo.isEditable() && combo.getEditor() != null) combo.getEditor().clear();
    }

    // ----------------- Actions -----------------
    @FXML
    private void onChooseImages(ActionEvent event) {
        Stage stage = getStageFromEvent(event);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir des images");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files == null || files.isEmpty()) return;

        for (File f : files) {
            if (f == null || !f.exists()) continue;
            boolean exists = selectedImages.stream()
                    .anyMatch(x -> x.getAbsolutePath().equalsIgnoreCase(f.getAbsolutePath()));
            if (!exists) selectedImages.add(f);
        }

        refreshGallery();
    }

    @FXML
    private void onEnregistrer(ActionEvent event) {
        clearErrors();

        if (!validateForm()) return;

        String nom = text(nomField);
        String type = safeComboValue(typeField);
        String gouvernorat = safeComboValue(gouvernoratBox);
        String ville = safeComboValue(villeBox);
        String adresse = text(adresseField);

        String phoneCode = safeComboValue(phoneCodeBox);
        String tel = text(telephoneField);

        String email = text(emailField);
        String gamme = gammePrixStarsBox.getValue() == null ? "" : gammePrixStarsBox.getValue();

        String horaires = text(horairesField);

        String lat = text(latitudeField);
        String lon = text(longitudeField);

        String desc = text(descriptionArea);

        // TODO : ici tu appelles ton service DAO
        // etablissementService.insert(new Etablissement(...), selectedImages);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText("Établissement enregistré");
        alert.setContentText(
                "Nom: " + nom + "\n" +
                        "Type: " + type + "\n" +
                        "Lieu: " + gouvernorat + " / " + ville + "\n" +
                        "Téléphone: " + (phoneCode.isBlank() ? "" : phoneCode + " ") + tel + "\n" +
                        "Images: " + selectedImages.size()
        );
        alert.showAndWait();

        resetForm();
    }

    @FXML
    private void onAnnuler(ActionEvent event) {
        Stage stage = getStageFromEvent(event);
        if (stage != null) stage.close();
    }

    // ----------------- Validation -----------------
    private boolean validateForm() {
        boolean valid = true;

        String nom = text(nomField);
        if (nom.isBlank()) {
            setError(nomError, "Le nom est obligatoire.");
            valid = false;
        } else if (nom.length() < 2) {
            setError(nomError, "Le nom doit contenir au moins 2 caractères.");
            valid = false;
        }

        // type optionnel (si tu veux obligatoire, décommente)
        // String type = safeComboValue(typeField);
        // if (type.isBlank()) {
        //     setError(typeError, "Choisis un type.");
        //     valid = false;
        // }

        String gouv = safeComboValue(gouvernoratBox);
        if (gouv.isBlank()) {
            setError(gouvernoratError, "Choisis un gouvernorat.");
            valid = false;
        }

        String ville = safeComboValue(villeBox);
        if (ville.isBlank()) {
            setError(villeError, "Choisis une ville.");
            valid = false;
        }

        // (Optionnel mais conseillé) : vérifier ville appartient au gouvernorat choisi
        if (!gouv.isBlank() && !ville.isBlank() && villesParGouvernorat.containsKey(gouv)) {
            if (!villesParGouvernorat.get(gouv).contains(ville)) {
                setError(villeError, "Cette ville n'appartient pas à " + gouv + ".");
                valid = false;
            }
        }

        String email = text(emailField);
        if (!email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            setError(emailError, "Email invalide. Exemple: nom@gmail.com");
            valid = false;
        }

        String tel = text(telephoneField);
        if (!tel.isBlank()) {
            if (!tel.matches("\\d+")) {
                setError(telephoneError, "Le téléphone doit contenir uniquement des chiffres.");
                valid = false;
            } else if (tel.length() < 6 || tel.length() > 15) {
                setError(telephoneError, "Longueur téléphone invalide (6 à 15 chiffres).");
                valid = false;
            }
        }

        String desc = text(descriptionArea);
        if (desc.length() > 200) {
            setError(descriptionError, "Maximum 200 caractères.");
            valid = false;
        }

        String lat = text(latitudeField);
        if (!lat.isBlank() && !isDouble(lat)) {
            setError(latitudeError, "Latitude invalide (ex: 36.8).");
            valid = false;
        }

        String lon = text(longitudeField);
        if (!lon.isBlank() && !isDouble(lon)) {
            setError(longitudeError, "Longitude invalide (ex: 10.2).");
            valid = false;
        }

        return valid;
    }

    private boolean isDouble(String s) {
        try {
            Double.parseDouble(s.replace(",", "."));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ----------------- Gallery -----------------
    private void refreshGallery() {
        imagesStrip.getChildren().clear();

        for (File f : selectedImages) {
            ImageView iv = new ImageView();
            iv.setImage(new Image(f.toURI().toString(), 120, 90, true, true));
            iv.setFitWidth(120);
            iv.setFitHeight(90);
            iv.setPreserveRatio(true);

            // ✅ CSS de ta copine : gallery-thumb-card / gallery-thumb-delete
            Label delete = new Label("✕");
            delete.getStyleClass().add("gallery-thumb-delete");
            delete.setOnMouseClicked(e -> {
                selectedImages.remove(f);
                refreshGallery();
            });

            HBox card = new HBox();
            card.setPadding(new Insets(6));
            card.setSpacing(8);
            card.getStyleClass().add("gallery-thumb-card");
            card.getChildren().addAll(iv, delete);

            imagesStrip.getChildren().add(card);
        }
    }

    // ----------------- Helpers -----------------
    private void clearErrors() {
        List<Label> labels = Arrays.asList(
                nomError, typeError, gouvernoratError, villeError, adresseError,
                telephoneError, emailError, gammeError, horairesError,
                latitudeError, longitudeError, descriptionError
        );
        for (Label l : labels) if (l != null) l.setText("");
    }

    private void setError(Label label, String msg) {
        if (label != null) label.setText(msg);
    }

    private String text(TextInputControl t) {
        return t == null || t.getText() == null ? "" : t.getText().trim();
    }

    private String safeComboValue(ComboBox<String> combo) {
        if (combo == null) return "";
        String v = combo.getValue();
        if (v != null && !v.isBlank()) return v.trim();

        if (combo.isEditable() && combo.getEditor() != null) {
            String ed = combo.getEditor().getText();
            return ed == null ? "" : ed.trim();
        }
        return "";
    }

    private Stage getStageFromEvent(ActionEvent event) {
        if (event == null) return null;
        Object src = event.getSource();
        if (!(src instanceof Node)) return null;
        return (Stage) ((Node) src).getScene().getWindow();
    }

    private void resetForm() {
        nomField.clear();
        clearComboSelection(typeField);

        clearComboSelection(gouvernoratBox);
        villeBox.getItems().clear();
        clearComboSelection(villeBox);

        adresseField.clear();

        phoneCodeBox.getSelectionModel().select("+216");
        if (phoneCodeBox.isEditable() && phoneCodeBox.getEditor() != null) phoneCodeBox.getEditor().setText("+216");
        telephoneField.clear();

        emailField.clear();
        gammePrixStarsBox.getSelectionModel().clearSelection();

        horairesTemplateBox.getSelectionModel().clearSelection();
        horairesField.clear();

        latitudeField.clear();
        longitudeField.clear();

        descriptionArea.clear();

        selectedImages.clear();
        refreshGallery();

        clearErrors();
    }
}