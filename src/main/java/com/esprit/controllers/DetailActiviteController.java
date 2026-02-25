package com.esprit.controllers;

import com.esprit.entities.Activite;
import com.esprit.entities.ActiviteImage;
import com.esprit.services.ActiviteImageServices;
import com.esprit.services.ActiviteServices;
import com.esprit.services.PdfExportService;
import com.esprit.services.TranslationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DetailActiviteController {

    // ===== Passed from gallery =====
    public static Activite activiteToShow = null;

    // ===== FXML =====
    @FXML private StackPane heroPane;
    @FXML private Label heroTitle;
    @FXML private Label heroSubtitle;

    @FXML private Label lblCategorie;
    @FXML private Label lblNiveau;
    @FXML private Label lblDuree;
    @FXML private Label lblStatut;
    @FXML private Label lblAdresse;
    @FXML private Label lblDescription;

    @FXML private Label lblEquipement;
    @FXML private Label lblConditions;
    @FXML private Label lblAgeMin;

    @FXML private Label lblPrix;
    @FXML private Label lblNbPlaces;
    @FXML private Label lblPlacesDispo;
    @FXML private Label lblDateDebut;
    @FXML private Label lblDateFin;

    @FXML private HBox galleryBox;
    @FXML private ComboBox<String> langSelector;
    @FXML private BorderPane rootPane;

    // ===== Translation state =====
    private final Map<Labeled, String> staticOriginals = new LinkedHashMap<>();
    private final Map<Label, String> dynamicOriginals = new LinkedHashMap<>();
    private boolean staticLabelsCollected = false;

    // ===== Services =====
    private final ActiviteServices service = new ActiviteServices();
    private final ActiviteImageServices imageService = new ActiviteImageServices();

    private static final String[] GRADIENTS = {
        "linear-gradient(to bottom right, #667eea, #764ba2)",
        "linear-gradient(to bottom right, #f093fb, #f5576c)",
        "linear-gradient(to bottom right, #4facfe, #00f2fe)",
        "linear-gradient(to bottom right, #43e97b, #38f9d7)",
        "linear-gradient(to bottom right, #fa709a, #fee140)",
        "linear-gradient(to bottom right, #a18cd1, #fbc2eb)"
    };

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        if (activiteToShow == null) return;

        // ===== LANGUAGE SELECTOR =====
        langSelector.setItems(FXCollections.observableArrayList(
            TranslationService.langDisplayName("fr"),
            TranslationService.langDisplayName("en"),
            TranslationService.langDisplayName("es"),
            TranslationService.langDisplayName("de"),
            TranslationService.langDisplayName("it"),
            TranslationService.langDisplayName("ar")
        ));
        langSelector.setValue(TranslationService.langDisplayName(TranslationService.getCurrentLang()));
        langSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String code = TranslationService.langCode(newVal);
                TranslationService.setCurrentLang(code);
                if (!staticLabelsCollected) collectStaticLabels();
                applyTranslation(code);
            }
        });

        Activite a = activiteToShow;

        // ===== HERO =====
        heroTitle.setText(safe(a.getNomActivite()));
        String sub = safe(a.getCategorie());
        if (!safe(a.getNiveau()).isBlank()) sub += " \u2022 " + safe(a.getNiveau());
        if (a.getDuree() != null) sub += " \u2022 " + a.getDuree() + " min";
        heroSubtitle.setText(sub);

        // Hero background
        Image heroImg = loadFirstImage(a.getIdActivite());
        if (heroImg != null) {
            ImageView iv = new ImageView(heroImg);
            iv.setFitWidth(1200);
            iv.setFitHeight(260);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);

            Region overlay = new Region();
            overlay.setStyle("-fx-background-color: linear-gradient(to top, rgba(0,0,0,0.85), rgba(0,0,0,0.2)); -fx-background-radius: 20;");

            heroPane.getChildren().add(0, iv);
            heroPane.getChildren().add(1, overlay);
        } else {
            int idx = Math.abs(a.getIdActivite()) % GRADIENTS.length;
            heroPane.setStyle("-fx-background-color: " + GRADIENTS[idx] + ";");
        }

        // ===== Informations Générales =====
        lblCategorie.setText(emptyAsDash(a.getCategorie()));
        lblNiveau.setText(emptyAsDash(a.getNiveau()));
        lblDuree.setText(a.getDuree() != null ? a.getDuree() + " min" : "\u2014");
        lblStatut.setText(emptyAsDash(a.getStatut()));
        lblAdresse.setText(emptyAsDash(a.getAdresseDepart()));

        // ===== Description =====
        lblDescription.setText(emptyAsDash(a.getDescription()));

        // ===== Équipement & Conditions =====
        lblEquipement.setText(emptyAsDash(a.getEquipementInclus()));
        lblConditions.setText(emptyAsDash(a.getConditionsAnnulation()));
        lblAgeMin.setText(a.getAgeMin() != null ? a.getAgeMin() + " ans" : "\u2014");

        // ===== Tarification & Places =====
        String prixTxt = "\u2014";
        if (a.getPrix() != null) {
            prixTxt = a.getPrix().toPlainString() + " " + safe(a.getDevise());
        }
        lblPrix.setText(prixTxt);
        lblNbPlaces.setText(a.getNbPlaces() != null ? String.valueOf(a.getNbPlaces()) : "\u2014");
        lblPlacesDispo.setText(a.getPlacesDispo() != null ? String.valueOf(a.getPlacesDispo()) : "\u2014");

        // ===== Dates =====
        lblDateDebut.setText(a.getDateDebut() != null ? DATE_FMT.format(a.getDateDebut()) : "\u2014");
        lblDateFin.setText(a.getDateFin() != null ? DATE_FMT.format(a.getDateFin()) : "\u2014");

        // ===== Gallery =====
        loadGallery(a.getIdActivite());

        // ===== Store dynamic originals for translation =====
        dynamicOriginals.put(heroTitle, heroTitle.getText());
        dynamicOriginals.put(heroSubtitle, heroSubtitle.getText());
        dynamicOriginals.put(lblCategorie, lblCategorie.getText());
        dynamicOriginals.put(lblNiveau, lblNiveau.getText());
        dynamicOriginals.put(lblDuree, lblDuree.getText());
        dynamicOriginals.put(lblStatut, lblStatut.getText());
        dynamicOriginals.put(lblAdresse, lblAdresse.getText());
        dynamicOriginals.put(lblDescription, lblDescription.getText());
        dynamicOriginals.put(lblEquipement, lblEquipement.getText());
        dynamicOriginals.put(lblConditions, lblConditions.getText());
        dynamicOriginals.put(lblAgeMin, lblAgeMin.getText());
        dynamicOriginals.put(lblPrix, lblPrix.getText());
        dynamicOriginals.put(lblNbPlaces, lblNbPlaces.getText());
        dynamicOriginals.put(lblPlacesDispo, lblPlacesDispo.getText());
        dynamicOriginals.put(lblDateDebut, lblDateDebut.getText());
        dynamicOriginals.put(lblDateFin, lblDateFin.getText());

        // ===== Collect static labels & translate (deferred until scene is rendered) =====
        Platform.runLater(() -> {
            collectStaticLabels();
            String lang = TranslationService.getCurrentLang();
            if (!"fr".equals(lang)) {
                applyTranslation(lang);
            }
        });
    }

    /**
     * Traduit TOUS les labels de la page (statiques + dynamiques).
     */
    private void applyTranslation(String lang) {
        if ("fr".equals(lang)) {
            restoreOriginals();
            return;
        }

        // Translate static labels (section headers, field names, buttons)
        for (Map.Entry<Labeled, String> entry : staticOriginals.entrySet()) {
            TranslationService.translateAsync(entry.getValue(), lang, entry.getKey()::setText);
        }

        // Translate dynamic values from French originals
        for (Map.Entry<Label, String> entry : dynamicOriginals.entrySet()) {
            String fr = entry.getValue();
            if (fr == null || fr.isBlank() || fr.equals("\u2014")) continue;
            TranslationService.translateAsync(fr, lang, entry.getKey()::setText);
        }
    }

    private void restoreOriginals() {
        for (Map.Entry<Labeled, String> entry : staticOriginals.entrySet()) {
            entry.getKey().setText(entry.getValue());
        }
        for (Map.Entry<Label, String> entry : dynamicOriginals.entrySet()) {
            entry.getKey().setText(entry.getValue());
        }
    }

    private void collectStaticLabels() {
        if (staticLabelsCollected || rootPane == null) return;
        // Section headers
        rootPane.lookupAll(".detail-card-title").forEach(n -> {
            if (n instanceof Label l) staticOriginals.put(l, l.getText());
        });
        // Field labels (CATÉGORIE, NIVEAU, etc.)
        rootPane.lookupAll(".detail-info-label").forEach(n -> {
            if (n instanceof Label l) staticOriginals.put(l, l.getText());
        });
        // Google Maps button
        rootPane.lookupAll(".btn-google-maps").forEach(n -> {
            if (n instanceof Labeled l) staticOriginals.put(l, l.getText());
        });
        // Nav bar tabs (Accueil, Tableau, etc.)
        rootPane.lookupAll(".nav-tab").forEach(n -> {
            if (n instanceof Labeled l) staticOriginals.put(l, l.getText());
        });
        // Nav logo
        rootPane.lookupAll(".nav-logo").forEach(n -> {
            if (n instanceof Labeled l) staticOriginals.put(l, l.getText());
        });
        // Retour button
        rootPane.lookupAll(".pill").forEach(n -> {
            if (n instanceof Labeled l) staticOriginals.put(l, l.getText());
        });
        staticLabelsCollected = true;
    }

    // ===== Gallery thumbnails =====
    private void loadGallery(int idActivite) {
        try {
            List<ActiviteImage> imgs = imageService.getImagesByActivite(idActivite);
            if (imgs == null || imgs.isEmpty()) {
                galleryBox.getChildren().add(new Label("Aucune image"));
                return;
            }
            for (ActiviteImage ai : imgs) {
                Image img = loadImageSafe("/images/" + ai.getImagePath());
                if (img == null) continue;

                ImageView thumb = new ImageView(img);
                thumb.setFitWidth(100);
                thumb.setFitHeight(80);
                thumb.setPreserveRatio(true);
                thumb.setSmooth(true);

                StackPane wrapper = new StackPane(thumb);
                wrapper.getStyleClass().add("detail-gallery-thumb");
                wrapper.setPrefSize(110, 90);
                galleryBox.getChildren().add(wrapper);
            }
        } catch (Exception ignored) {
            galleryBox.getChildren().add(new Label("Erreur chargement images"));
        }
    }

    // ===== Image loading =====
    private Image loadFirstImage(int idActivite) {
        try {
            List<ActiviteImage> imgs = imageService.getImagesByActivite(idActivite);
            if (imgs != null && !imgs.isEmpty()) {
                return loadImageSafe("/images/" + imgs.get(0).getImagePath());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Image loadImageSafe(String resourcePathOrFilePath) {
        try {
            if (resourcePathOrFilePath == null || resourcePathOrFilePath.isBlank()) return null;

            if (resourcePathOrFilePath.startsWith("/")) {
                var is = getClass().getResourceAsStream(resourcePathOrFilePath);
                if (is != null) return new Image(is);
            }

            File f = new File(resourcePathOrFilePath);
            if (f.exists()) return new Image(f.toURI().toString(), true);
        } catch (Exception ignored) {}
        return null;
    }

    // ===== Google Maps =====
    @FXML
    private void onOpenGoogleMaps(ActionEvent event) {
        if (activiteToShow == null) return;

        String query = safe(activiteToShow.getAdresseDepart()).trim();

        if (query.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Localisation manquante");
            alert.setHeaderText(null);
            alert.setContentText("Aucune adresse de départ disponible pour cette activité.");
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

    // ===== Actions =====
    @FXML
    private void onExportPdf(ActionEvent event) {
        if (activiteToShow == null) return;

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter en PDF");
        fc.setInitialFileName(safe(activiteToShow.getNomActivite()).replaceAll("[^a-zA-Z0-9\\-_ ]", "") + "_fiche.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));

        javafx.stage.Window window = rootPane.getScene().getWindow();
        java.io.File file = fc.showSaveDialog(window);
        if (file == null) return;

        try {
            PdfExportService.exportActivite(activiteToShow, file);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export r\u00e9ussi");
            ok.setHeaderText(null);
            ok.setContentText("PDF g\u00e9n\u00e9r\u00e9 avec succ\u00e8s !\n" + file.getAbsolutePath());
            ok.showAndWait();

            if (java.awt.Desktop.isDesktopSupported()) {
                new Thread(() -> {
                    try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {}
                }).start();
            }
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur export");
            err.setHeaderText(null);
            err.setContentText("Erreur lors de la g\u00e9n\u00e9ration du PDF : " + ex.getMessage());
            err.showAndWait();
            ex.printStackTrace();
        }
    }

    @FXML
    private void onEdit(ActionEvent event) {
        AffichageActiviteController.activiteToEdit = activiteToShow;
        NavigationUtils.goTo("/ajouter_activite.fxml", event);
    }

    @FXML
    private void onDelete(ActionEvent event) {
        if (activiteToShow == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer activité");
        alert.setHeaderText(null);
        alert.setContentText("Voulez-vous supprimer : " + safe(activiteToShow.getNomActivite()) + " ?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            imageService.supprimerImagesByActivite(activiteToShow.getIdActivite());
            service.supprimer(activiteToShow.getIdActivite());
            NavigationUtils.goTo("/activite_affichage.fxml", event);
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur");
            err.setContentText("Erreur suppression: " + ex.getMessage());
            err.showAndWait();
        }
    }

    // ===== Navigation =====
    @FXML
    private void goBack(ActionEvent event) {
        NavigationUtils.goTo("/activite_affichage.fxml", event);
    }

    @FXML
    private void goHome(ActionEvent event) {
        NavigationUtils.goTo("/home.fxml", event);
    }

    @FXML
    private void goTableau(ActionEvent event) {
        NavigationUtils.goTo("/etablissement_tableau.fxml", event);
    }

    @FXML
    private void goEtablissements(ActionEvent event) {
        NavigationUtils.goTo("/etablissement_affichage.fxml", event);
    }

    @FXML
    private void goGalerie(ActionEvent event) {
        NavigationUtils.goTo("/activite_affichage.fxml", event);
    }

    // ===== Helpers =====
    private String safe(String s) { return s == null ? "" : s; }
    private String emptyAsDash(String s) {
        String v = safe(s).trim();
        return v.isEmpty() ? "\u2014" : v;
    }
}
