package com.esprit.controllers;

import com.esprit.entities.Etablissement;
import com.esprit.entities.EtablissementImage;
import com.esprit.services.DescriptionGeneratorService;
import com.esprit.services.EtablissementImageServices;
import com.esprit.utils.ThemeManager;
import com.esprit.utils.AlertUtils;
import com.esprit.services.EtablissementService;
import javafx.application.Platform;
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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.File;
import java.sql.SQLException;
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

    @FXML private TextArea descriptionArea;
    @FXML private Button generateDescBtn;

    @FXML private HBox imagesStrip;
    @FXML private Button saveBtn;

    // ====== Map picker ======
    @FXML private WebView mapWebView;
    @FXML private Label latLabel;
    @FXML private Label lngLabel;
    private Double selectedLat = null;
    private Double selectedLng = null;

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
    @FXML private Label descriptionError;

    // ====== Data ======
    private final List<File> selectedImages = new ArrayList<>();
    private final Map<String, List<String>> villesParGouvernorat = new LinkedHashMap<>();
    private final DescriptionGeneratorService descriptionGenerator = new DescriptionGeneratorService();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-zÀ-ÖØ-öø-ÿ0-9\\s'\\-&.,()]+$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{8}$");

    @FXML
    public void initialize() {
        setupData();
        setupCombos();
        setupListeners();
        clearErrors();
        refreshGallery();
        initMapPicker();

        // ===== MODE EDIT =====
        if (AffichageEtablissementController.etablissementToEdit != null) {
            Etablissement e = AffichageEtablissementController.etablissementToEdit;
            titleLabel.setText("Modifier l'établissement");
            saveBtn.setText("Enregistrer les modifications");

            nomField.setText(safe(e.getNom()));
            typeField.setValue(mapDbTypeToUi(e.getType()));
            adresseField.setText(safe(e.getAdresse()));

            // Trouver le gouvernorat correspondant à la ville
            String villeValue = safe(e.getVille());
            for (var entry : villesParGouvernorat.entrySet()) {
                if (entry.getValue().stream().anyMatch(v -> v.equalsIgnoreCase(villeValue))) {
                    gouvernoratBox.setValue(entry.getKey());
                    villeBox.setValue(villeValue);
                    break;
                }
            }
            if (gouvernoratBox.getValue() == null && !villeValue.isBlank()) {
                villeBox.setValue(villeValue);
            }

            // Téléphone : séparer indicatif si présent
            String tel = safe(e.getTelephone());
            if (tel.startsWith("+")) {
                int space = tel.indexOf(' ');
                if (space > 0) {
                    phoneCodeBox.setValue(tel.substring(0, space));
                    telephoneField.setText(tel.substring(space + 1));
                } else {
                    telephoneField.setText(tel);
                }
            } else {
                telephoneField.setText(tel);
            }

            emailField.setText(safe(e.getEmail()));
            gammePrixStarsBox.setValue(safe(e.getGammePrix()));
            horairesField.setText(safe(e.getHoraires()));
            descriptionArea.setText(safe(e.getDescription()));

            // Restore map position if available
            if (e.getLatitude() != null && e.getLongitude() != null) {
                selectedLat = e.getLatitude();
                selectedLng = e.getLongitude();
                latLabel.setText(String.valueOf(selectedLat));
                lngLabel.setText(String.valueOf(selectedLng));
                // Will set map position once loaded (see initMapPicker)
            }
        }
    }

    // ----------------- Map Picker -----------------
    /**
     * JavaScript-to-Java bridge: receives location clicks from the Leaflet map.
     */
    public class MapBridge {
        public void onLocationSelected(double lat, double lng) {
            Platform.runLater(() -> {
                selectedLat = lat;
                selectedLng = lng;
                latLabel.setText(String.format("%.6f", lat));
                lngLabel.setText(String.format("%.6f", lng));
            });
        }
    }

    private void initMapPicker() {
        if (mapWebView == null) return;
        WebEngine engine = mapWebView.getEngine();
        engine.setJavaScriptEnabled(true);

        // Load the map HTML
        String mapUrl = getClass().getResource("/map_picker.html").toExternalForm();
        engine.load(mapUrl);

        // When page loaded, inject the Java bridge + set initial position if editing
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", new MapBridge());

                // Force Leaflet to recalculate its size (JavaFX WebView fix)
                engine.executeScript("setTimeout(function(){ map.invalidateSize(); }, 300);");

                // If editing, move map to the saved position
                if (selectedLat != null && selectedLng != null) {
                    engine.executeScript(
                        String.format("setPosition(%f, %f)", selectedLat, selectedLng)
                    );
                }
            }
        });
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

        // ===== Gouvernorats Tunisie (24) + villes principales =====
        villesParGouvernorat.put("Tunis", Arrays.asList(
                "Tunis", "La Marsa", "Carthage", "Le Bardo", "Le Kram", "Berges du Lac"
        ));
        villesParGouvernorat.put("Ariana", Arrays.asList(
                "Ariana", "Raoued", "Sidi Thabet", "Soukra"
        ));
        villesParGouvernorat.put("Ben Arous", Arrays.asList(
                "Ben Arous", "Ezzahra", "Rades", "Mourouj", "Hammam Lif", "Hammam Chatt", "Mégrine"
        ));
        villesParGouvernorat.put("Manouba", Arrays.asList(
                "Manouba", "Denden", "Oued Ellil", "Douar Hicher", "Tebourba"
        ));

        villesParGouvernorat.put("Nabeul", Arrays.asList(
                "Nabeul", "Hammamet", "Korba", "Menzel Temime", "Kelibia", "Dar Chaabane", "Takelsa", "Beni Khiar"
        ));
        villesParGouvernorat.put("Zaghouan", Arrays.asList(
                "Zaghouan", "Zriba", "El Fahs", "Nadhour", "Bir Mcherga"
        ));

        villesParGouvernorat.put("Bizerte", Arrays.asList(
                "Bizerte", "Menzel Bourguiba", "Mateur", "Ras Jebel", "Menzel Jemil"
        ));
        villesParGouvernorat.put("Béja", Arrays.asList(
                "Béja", "Testour", "Medjez El Bab", "Teboursouk"
        ));
        villesParGouvernorat.put("Jendouba", Arrays.asList(
                "Jendouba", "Tabarka", "Ain Draham", "Bou Salem"
        ));
        villesParGouvernorat.put("Le Kef", Arrays.asList(
                "Le Kef", "Tajerouine", "Dahmani", "Sakiet Sidi Youssef"
        ));
        villesParGouvernorat.put("Siliana", Arrays.asList(
                "Siliana", "Gaafour", "Makthar", "Rouhia"
        ));

        villesParGouvernorat.put("Sousse", Arrays.asList(
                "Sousse", "Hammam Sousse", "Kalaa Kebira", "Kalaa Sghira", "Enfidha"
        ));
        villesParGouvernorat.put("Monastir", Arrays.asList(
                "Monastir", "Sahline", "Ksar Hellal", "Moknine", "Jammel"
        ));
        villesParGouvernorat.put("Mahdia", Arrays.asList(
                "Mahdia", "Chebba", "El Jem", "Ksour Essef"
        ));
        villesParGouvernorat.put("Sfax", Arrays.asList(
                "Sfax", "Sakiet Ezzit", "Sakiet Eddaier", "Agareb", "Thyna", "Mahres"
        ));

        villesParGouvernorat.put("Kairouan", Arrays.asList(
                "Kairouan", "Oueslatia", "Sbikha", "Haffouz"
        ));
        villesParGouvernorat.put("Kasserine", Arrays.asList(
                "Kasserine", "Sbeitla", "Foussana", "Feriana"
        ));
        villesParGouvernorat.put("Sidi Bouzid", Arrays.asList(
                "Sidi Bouzid", "Regueb", "Menzel Bouzaiane", "Meknassy"
        ));

        villesParGouvernorat.put("Gabès", Arrays.asList(
                "Gabès", "Ghannouch", "Mareth", "Metouia"
        ));
        villesParGouvernorat.put("Médenine", Arrays.asList(
                "Médenine", "Djerba Houmt Souk", "Djerba Midoun", "Ben Gardane", "Zarzis"
        ));
        villesParGouvernorat.put("Tataouine", Arrays.asList(
                "Tataouine", "Remada", "Ghomrassen"
        ));

        villesParGouvernorat.put("Gafsa", Arrays.asList(
                "Gafsa", "Metlaoui", "Redeyef", "Mdhilla", "El Ksar"
        ));
        villesParGouvernorat.put("Tozeur", Arrays.asList(
                "Tozeur", "Nefta", "Degueche"
        ));
        villesParGouvernorat.put("Kebili", Arrays.asList(
                "Kebili", "Douz", "Souk Lahad"
        ));
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
            liveValidate();
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

        // Live validation listeners
        nomField.textProperty().addListener((o, a, b) -> liveValidate());
        typeField.valueProperty().addListener((o, a, b) -> liveValidate());
        villeBox.valueProperty().addListener((o, a, b) -> liveValidate());
        adresseField.textProperty().addListener((o, a, b) -> liveValidate());
        telephoneField.textProperty().addListener((o, a, b) -> liveValidate());
        emailField.textProperty().addListener((o, a, b) -> liveValidate());
        gammePrixStarsBox.valueProperty().addListener((o, a, b) -> liveValidate());
        horairesField.textProperty().addListener((o, a, b) -> liveValidate());
        descriptionArea.textProperty().addListener((o, a, b) -> liveValidate());
    }

    private void liveValidate() {
        // Run validation without blocking the save — just shows errors in real time
        validateForm();
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
        String uiType = safeComboValue(typeField);
        String type = mapUiTypeToDb(uiType);
        String ville = safeComboValue(villeBox);
        String adresse = text(adresseField);

        String phoneCode = safeComboValue(phoneCodeBox);
        String tel = text(telephoneField);
        String fullTel = tel;
        if (!phoneCode.isBlank()) {
            fullTel = phoneCode.trim() + (tel.isBlank() ? "" : " " + tel.trim());
        }

        String email = text(emailField);
        String gamme = gammePrixStarsBox.getValue() == null ? "" : gammePrixStarsBox.getValue();

        String horaires = text(horairesField);

        String desc = text(descriptionArea);

        Etablissement e;
        boolean isEdit = AffichageEtablissementController.etablissementToEdit != null;

        if (isEdit) {
            e = AffichageEtablissementController.etablissementToEdit;
        } else {
            e = new Etablissement();
        }
        e.setNom(nom);
        e.setType(type);
        e.setAdresse(adresse);
        e.setVille(ville);
        e.setTelephone(fullTel);
        e.setEmail(email);
        e.setHoraires(horaires);
        e.setGammePrix(gamme);
        e.setLatitude(selectedLat);
        e.setLongitude(selectedLng);
        e.setDescription(desc);

        EtablissementService etabService = new EtablissementService();
        EtablissementImageServices imageService = new EtablissementImageServices();

        try {
            if (isEdit) {
                // UPDATE
                etabService.modifier(e);
                if (!selectedImages.isEmpty()) {
                    int ordre = 1;
                    for (File f : selectedImages) {
                        if (f == null) continue;
                        String path = f.getAbsolutePath();
                        if (path == null || path.isBlank()) continue;
                        EtablissementImage img = new EtablissementImage(e.getIdEtablissement(), path, ordre++);
                        imageService.ajouterImage(img);
                    }
                }
                AffichageEtablissementController.etablissementToEdit = null;

                try {
                    SuccessNotification.show(saveBtn, "Établissement modifié avec succès !", () -> {
                        NavigationUtils.goTo("/etablissement_affichage.fxml", event);
                    });
                } catch (Exception notifEx) {
                    notifEx.printStackTrace();
                    NavigationUtils.goTo("/etablissement_affichage.fxml", event);
                }
            } else {
                // INSERT
                int id = etabService.ajouterEtRetournerId(e);

                if (id > 0 && !selectedImages.isEmpty()) {
                    int ordre = 1;
                    for (File f : selectedImages) {
                        if (f == null) continue;
                        String path = f.getAbsolutePath();
                        if (path == null || path.isBlank()) continue;
                        EtablissementImage img = new EtablissementImage(id, path, ordre++);
                        imageService.ajouterImage(img);
                    }
                }

                // Notification animée puis navigation vers la liste
                try {
                    SuccessNotification.show(saveBtn, "Établissement ajouté avec succès !", () -> {
                        NavigationUtils.goTo("/etablissement_affichage.fxml", event);
                    });
                } catch (Exception notifEx) {
                    notifEx.printStackTrace();
                    NavigationUtils.goTo("/etablissement_affichage.fxml", event);
                }
            }

        } catch (SQLException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors de l'enregistrement");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onOpenGoogleMaps(ActionEvent event) {
        // Build query from address + ville
        String adresse = text(adresseField);
        String ville = safeComboValue(villeBox);

        String query = "";

        // Prefer address text so Google Maps resolves the actual place name
        StringBuilder sb = new StringBuilder();
        if (!adresse.isBlank()) sb.append(adresse);
        if (!ville.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ville);
        }
        query = sb.toString();

        // Fall back to map-selected coordinates
        if (query.isBlank() && selectedLat != null && selectedLng != null) {
            query = selectedLat + "," + selectedLng;
        }

        if (query.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Localisation manquante");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez saisir une adresse ou sélectionner un emplacement sur la carte.");
            alert.showAndWait();
            return;
        }

        try {
            String encoded = java.net.URLEncoder.encode(query, "UTF-8");
            String url = "https://www.google.com/maps/search/?api=1&query=" + encoded;
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir Google Maps : " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onAnnuler(ActionEvent event) {
        AffichageEtablissementController.etablissementToEdit = null;
        NavigationUtils.goTo("/etablissement_affichage.fxml", event);
    }

    // ----------------- Validation -----------------
    private boolean validateForm() {
        clearErrors();
        boolean valid = true;

        // ──────── Nom (obligatoire, 3-100 chars, pas uniquement des chiffres) ────────
        String nom = text(nomField);
        if (nom.isBlank()) {
            setError(nomError, "Le nom est obligatoire.");
            markError(nomField);
            valid = false;
        } else if (nom.length() < 3) {
            setError(nomError, "Le nom doit contenir au moins 3 caractères.");
            markError(nomField);
            valid = false;
        } else if (nom.length() > 100) {
            setError(nomError, "Le nom ne doit pas dépasser 100 caractères.");
            markError(nomField);
            valid = false;
        } else if (!NAME_PATTERN.matcher(nom).matches()) {
            setError(nomError, "Le nom contient des caractères non autorisés.");
            markError(nomField);
            valid = false;
        } else if (DIGITS_ONLY.matcher(nom).matches()) {
            setError(nomError, "Le nom ne peut pas être uniquement des chiffres.");
            markError(nomField);
            valid = false;
        }

        // ──────── Type (obligatoire) ────────
        String uiType = safeComboValue(typeField);
        if (uiType.isBlank()) {
            setError(typeError, "Choisis un type.");
            markError(typeField);
            valid = false;
        }

        // ──────── Gouvernorat (obligatoire) ────────
        String gouv = safeComboValue(gouvernoratBox);
        if (gouv.isBlank()) {
            setError(gouvernoratError, "Choisis un gouvernorat.");
            markError(gouvernoratBox);
            valid = false;
        }

        // ──────── Ville (obligatoire, cohérence avec gouvernorat) ────────
        String ville = safeComboValue(villeBox);
        if (ville.isBlank()) {
            setError(villeError, "Choisis une ville.");
            markError(villeBox);
            valid = false;
        } else if (!gouv.isBlank() && villesParGouvernorat.containsKey(gouv)) {
            if (!villesParGouvernorat.get(gouv).contains(ville)) {
                setError(villeError, "Cette ville n'appartient pas à " + gouv + ".");
                markError(villeBox);
                valid = false;
            }
        }

        // ──────── Adresse (obligatoire, 5-255 chars) ────────
        String adresse = text(adresseField);
        if (adresse.isBlank()) {
            setError(adresseError, "L'adresse est obligatoire.");
            markError(adresseField);
            valid = false;
        } else if (adresse.length() < 5) {
            setError(adresseError, "L'adresse doit contenir au moins 5 caractères.");
            markError(adresseField);
            valid = false;
        } else if (adresse.length() > 255) {
            setError(adresseError, "L'adresse ne doit pas dépasser 255 caractères.");
            markError(adresseField);
            valid = false;
        }

        // ──────── Téléphone (obligatoire, 8 chiffres) ────────
        String tel = text(telephoneField);
        if (tel.isBlank()) {
            setError(telephoneError, "Le téléphone est obligatoire.");
            markError(telephoneField);
            valid = false;
        } else if (!DIGITS_ONLY.matcher(tel).matches()) {
            setError(telephoneError, "Le téléphone doit contenir uniquement des chiffres.");
            markError(telephoneField);
            valid = false;
        } else if (!PHONE_PATTERN.matcher(tel).matches()) {
            setError(telephoneError, "Le téléphone doit contenir exactement 8 chiffres.");
            markError(telephoneField);
            valid = false;
        }

        // ──────── Email (obligatoire, format valide, max 100 chars) ────────
        String email = text(emailField);
        if (email.isBlank()) {
            setError(emailError, "L'email est obligatoire.");
            markError(emailField);
            valid = false;
        } else if (email.length() > 100) {
            setError(emailError, "L'email ne doit pas dépasser 100 caractères.");
            markError(emailField);
            valid = false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            setError(emailError, "Email invalide. Exemple: nom@gmail.com");
            markError(emailField);
            valid = false;
        }

        // ──────── Gamme de prix (obligatoire) ────────
        String gamme = gammePrixStarsBox.getValue() == null ? "" : gammePrixStarsBox.getValue().trim();
        if (gamme.isBlank()) {
            setError(gammeError, "Choisis une gamme de prix.");
            markError(gammePrixStarsBox);
            valid = false;
        }

        // ──────── Horaires (obligatoire, max 100 chars) ────────
        String horaires = text(horairesField);
        if (horaires.isBlank()) {
            setError(horairesError, "Les horaires sont obligatoires.");
            markError(horairesField);
            valid = false;
        } else if (horaires.length() > 100) {
            setError(horairesError, "Les horaires ne doivent pas dépasser 100 caractères.");
            markError(horairesField);
            valid = false;
        }

        // ──────── Description (obligatoire, 10-200 chars) ────────
        String desc = text(descriptionArea);
        if (desc.isBlank()) {
            setError(descriptionError, "La description est obligatoire.");
            markError(descriptionArea);
            valid = false;
        } else if (desc.length() < 10) {
            setError(descriptionError, "La description doit contenir au moins 10 caractères.");
            markError(descriptionArea);
            valid = false;
        } else if (desc.length() > 200) {
            setError(descriptionError, "Maximum 200 caractères.");
            markError(descriptionArea);
            valid = false;
        }

        // Disable save button if invalid
        if (saveBtn != null) saveBtn.setDisable(!valid);

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
        // Clear error labels
        List<Label> labels = Arrays.asList(
                nomError, typeError, gouvernoratError, villeError, adresseError,
                telephoneError, emailError, gammeError, horairesError,
                descriptionError
        );
        for (Label l : labels) if (l != null) l.setText("");

        // Remove CSS error highlight from all inputs
        removeError(nomField);
        removeError(typeField);
        removeError(gouvernoratBox);
        removeError(villeBox);
        removeError(adresseField);
        removeError(telephoneField);
        removeError(emailField);
        removeError(gammePrixStarsBox);
        removeError(horairesField);
        removeError(descriptionArea);
    }

    private void markError(Control c) {
        if (c != null && !c.getStyleClass().contains("input-error")) c.getStyleClass().add("input-error");
    }

    private void removeError(Control c) {
        if (c != null) c.getStyleClass().remove("input-error");
    }

    private void setError(Label label, String msg) {
        if (label != null) label.setText(msg);
    }

    private String text(TextInputControl t) {
        return t == null || t.getText() == null ? "" : t.getText().trim();
    }

    private String safe(String s) { return s == null ? "" : s; }

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

    private String mapDbTypeToUi(String dbType) {
        if (dbType == null) return null;
        return switch (dbType.trim().toLowerCase(Locale.ROOT)) {
            case "hotel" -> "Hôtel";
            case "restaurant" -> "Restaurant";
            case "cafe" -> "Café";
            case "museum" -> "Musée";
            case "bar" -> "Bar";
            case "loisir" -> "Loisir";
            default -> "Autre";
        };
    }

    /**
     * Convertit le type affiché dans le formulaire (avec accents / mots français)
     * vers la valeur attendue en base (ENUM: hotel, restaurant, cafe, museum, bar, autre).
     */
    private String mapUiTypeToDb(String uiType) {
        if (uiType == null) return null;
        String t = uiType.trim().toLowerCase(Locale.ROOT);

        return switch (t) {
            case "hôtel", "hotel" -> "hotel";
            case "restaurant" -> "restaurant";
            case "café", "cafe" -> "cafe";
            case "musée", "musee" -> "museum";
            case "bar" -> "bar";
            case "loisir", "autre" -> "autre";
            default -> "autre";
        };
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

        descriptionArea.clear();

        selectedImages.clear();
        refreshGallery();

        clearErrors();
    }

    @FXML
    private void handleGenerateDescription(ActionEvent event) {
        if (!descriptionGenerator.isConfigured()) {
            AlertUtils.error("API key not configured",
                    "Groq API key missing",
                    "Please set your Groq API key in config.properties (groq.api.key).\nGet a free key at: https://console.groq.com/keys");
            return;
        }

        String name = nomField.getText();
        if (name == null || name.isBlank()) {
            AlertUtils.error("Missing information",
                    "Name required",
                    "Please fill in the establishment name before generating a description.");
            return;
        }

        generateDescBtn.setDisable(true);
        generateDescBtn.setText("⏳ Generating...");

        String type = typeField.getValue() != null ? typeField.getValue() : "";
        String city = villeBox.getValue() != null ? villeBox.getValue() : "";
        String address = adresseField.getText() != null ? adresseField.getText() : "";
        String price = gammePrixStarsBox.getValue() != null ? gammePrixStarsBox.getValue() : "";
        String hours = horairesField.getText() != null ? horairesField.getText() : "";

        descriptionGenerator.generateEstablishmentDescription(
                name, type, city, address, price, hours
        ).thenAccept(description -> javafx.application.Platform.runLater(() -> {
            descriptionArea.setText(description);
            generateDescBtn.setDisable(false);
            generateDescBtn.setText("✨ Generate with AI");
        })).exceptionally(ex -> {
            javafx.application.Platform.runLater(() -> {
                generateDescBtn.setDisable(false);
                generateDescBtn.setText("✨ Generate with AI");
                AlertUtils.error("Generation failed",
                        "AI generation error",
                        ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            });
            return null;
        });
    }

    @FXML
    private void toggleTheme(ActionEvent event) {
        ThemeManager.handleToggleTheme(event);
    }
}