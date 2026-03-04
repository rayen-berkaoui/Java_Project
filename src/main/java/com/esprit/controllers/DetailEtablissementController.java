package com.esprit.controllers;

import com.esprit.entities.Etablissement;
import com.esprit.entities.EtablissementImage;
import com.esprit.services.EtablissementImageServices;
import com.esprit.utils.ThemeManager;
import com.esprit.services.EtablissementServices;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DetailEtablissementController {

    // ===== Passed from gallery =====
    public static Etablissement etablissementToShow = null;

    // ===== FXML =====
    @FXML private StackPane heroPane;
    @FXML private Label heroTitle;
    @FXML private Label heroSubtitle;

    @FXML private Label lblAdresse;
    @FXML private Label lblVille;
    @FXML private Label lblTelephone;
    @FXML private Label lblEmail;
    @FXML private Label lblDescription;

    @FXML private Label lblType;
    @FXML private Label lblGamme;
    @FXML private Label lblHoraires;

    @FXML private HBox galleryBox;
    @FXML private ComboBox<String> langSelector;
    @FXML private BorderPane rootPane;

    // ===== Translation state =====
    private final Map<Labeled, String> staticOriginals = new LinkedHashMap<>();
    private final Map<Label, String> dynamicOriginals = new LinkedHashMap<>();
    private boolean staticLabelsCollected = false;

    // ===== Services =====
    private final EtablissementServices service = new EtablissementServices();
    private final EtablissementImageServices imageService = new EtablissementImageServices();

    private static final String[] GRADIENTS = {
        "linear-gradient(to bottom right, #667eea, #764ba2)",
        "linear-gradient(to bottom right, #f093fb, #f5576c)",
        "linear-gradient(to bottom right, #4facfe, #00f2fe)",
        "linear-gradient(to bottom right, #43e97b, #38f9d7)",
        "linear-gradient(to bottom right, #fa709a, #fee140)",
        "linear-gradient(to bottom right, #a18cd1, #fbc2eb)"
    };

    @FXML
    public void initialize() {
        if (etablissementToShow == null) return;

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

        Etablissement e = etablissementToShow;

        // ===== HERO =====
        heroTitle.setText(safe(e.getNom()));
        String sub = safe(e.getVille());
        if (!safe(e.getType()).isBlank()) sub += " \u2022 " + safe(e.getType());
        if (!safe(e.getGammePrix()).isBlank()) sub += " \u2022 " + safe(e.getGammePrix());
        heroSubtitle.setText(sub);

        // Hero background: try first image, else gradient
        Image heroImg = loadFirstImage(e.getIdEtablissement());
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
            int idx = Math.abs(e.getIdEtablissement()) % GRADIENTS.length;
            heroPane.setStyle("-fx-background-color: " + GRADIENTS[idx] + ";");
        }

        // ===== Coordonnées =====
        lblAdresse.setText(emptyAsDash(e.getAdresse()));
        lblVille.setText(emptyAsDash(e.getVille()));
        lblTelephone.setText(emptyAsDash(e.getTelephone()));
        lblEmail.setText(emptyAsDash(e.getEmail()));

        // ===== Description =====
        lblDescription.setText(emptyAsDash(e.getDescription()));

        // ===== Détails =====
        lblType.setText(emptyAsDash(e.getType()));
        lblGamme.setText(emptyAsDash(e.getGammePrix()));
        lblHoraires.setText(emptyAsDash(e.getHoraires()));

        // ===== Gallery =====
        loadGallery(e.getIdEtablissement());

        // ===== Store dynamic originals for translation =====
        dynamicOriginals.put(heroTitle, heroTitle.getText());
        dynamicOriginals.put(heroSubtitle, heroSubtitle.getText());
        dynamicOriginals.put(lblAdresse, lblAdresse.getText());
        dynamicOriginals.put(lblVille, lblVille.getText());
        dynamicOriginals.put(lblTelephone, lblTelephone.getText());
        dynamicOriginals.put(lblEmail, lblEmail.getText());
        dynamicOriginals.put(lblDescription, lblDescription.getText());
        dynamicOriginals.put(lblType, lblType.getText());
        dynamicOriginals.put(lblGamme, lblGamme.getText());
        dynamicOriginals.put(lblHoraires, lblHoraires.getText());

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
        // Field labels (ADRESSE, VILLE, etc.)
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
    private void loadGallery(int idEtab) {
        try {
            List<EtablissementImage> imgs = imageService.getImagesByEtablissement(idEtab);
            if (imgs == null || imgs.isEmpty()) {
                galleryBox.getChildren().add(new Label("Aucune image"));
                return;
            }
            for (EtablissementImage ei : imgs) {
                Image img = loadImageFromResources(ei.getImagePath());
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
    private Image loadFirstImage(int idEtab) {
        try {
            List<EtablissementImage> imgs = imageService.getImagesByEtablissement(idEtab);
            if (imgs != null && !imgs.isEmpty()) {
                return loadImageFromResources(imgs.get(0).getImagePath());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Image loadImageFromResources(String imgName) {
        try {
            if (imgName == null || imgName.isBlank()) return null;

            if (imgName.contains("\\") || imgName.contains("/") || imgName.matches("^[A-Za-z]:.*")) {
                java.io.File f = new java.io.File(imgName);
                if (f.exists()) return new Image(f.toURI().toString());
            }

            var is = getClass().getResourceAsStream("/images/" + imgName);
            if (is != null) return new Image(is);
        } catch (Exception ignored) {}
        return null;
    }

    // ===== Google Maps =====
    @FXML
    private void onOpenGoogleMaps(ActionEvent event) {
        if (etablissementToShow == null) return;

        String query = "";

        // Prefer address text so Google Maps resolves the actual place name
        StringBuilder sb = new StringBuilder();
        if (!safe(etablissementToShow.getNom()).isBlank()) sb.append(etablissementToShow.getNom());
        if (!safe(etablissementToShow.getAdresse()).isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(etablissementToShow.getAdresse());
        }
        if (!safe(etablissementToShow.getVille()).isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(etablissementToShow.getVille());
        }
        query = sb.toString();

        if (query.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Localisation manquante");
            alert.setHeaderText(null);
            alert.setContentText("Aucune adresse disponible.");
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
        if (etablissementToShow == null) return;

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter en PDF");
        fc.setInitialFileName(safe(etablissementToShow.getNom()).replaceAll("[^a-zA-Z0-9\\-_ ]", "") + "_fiche.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));

        javafx.stage.Window window = rootPane.getScene().getWindow();
        java.io.File file = fc.showSaveDialog(window);
        if (file == null) return;

        try {
            PdfExportService.exportEtablissement(etablissementToShow, file);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export r\u00e9ussi");
            ok.setHeaderText(null);
            ok.setContentText("PDF g\u00e9n\u00e9r\u00e9 avec succ\u00e8s !\n" + file.getAbsolutePath());
            ok.showAndWait();

            // Ouvrir le fichier
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
        AffichageEtablissementController.etablissementToEdit = etablissementToShow;
        NavigationUtils.goTo("/ajouter_etablissement.fxml", event);
    }

    @FXML
    private void onDelete(ActionEvent event) {
        if (etablissementToShow == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer établissement");
        alert.setHeaderText(null);
        alert.setContentText("Voulez-vous supprimer : " + safe(etablissementToShow.getNom()) + " ?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            service.supprimer(etablissementToShow.getIdEtablissement());
            NavigationUtils.goTo("/etablissement_affichage.fxml", event);
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
        NavigationUtils.goTo("/etablissement_affichage.fxml", event);
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
    private void goGalerie(ActionEvent event) {
        NavigationUtils.goTo("/etablissement_affichage.fxml", event);
    }

    @FXML
    private void goActivites(ActionEvent event) {
        NavigationUtils.goTo("/activite_affichage.fxml", event);
    }



    // ===== Helpers =====
    private String safe(String s) { return s == null ? "" : s; }
    private String emptyAsDash(String s) {
        String v = safe(s).trim();
        return v.isEmpty() ? "\u2014" : v;
    }

    @FXML
    private void toggleTheme(ActionEvent event) {
        ThemeManager.handleToggleTheme(event);
    }
}
