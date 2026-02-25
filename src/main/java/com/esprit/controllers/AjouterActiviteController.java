package com.esprit.controllers;

import com.esprit.entities.Activite;
import com.esprit.entities.ActiviteImage;
import com.esprit.services.ActiviteImageServices;
import com.esprit.services.ActiviteServices;
import com.esprit.services.EtablissementServices;
import com.esprit.entities.Etablissement;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

public class AjouterActiviteController {

    // ====== Regex patterns ======
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-zÀ-ÖØ-öø-ÿ0-9\\s'\\-&.,()]+$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");
    private static final Pattern PRIX_PATTERN = Pattern.compile("^\\d+([.,]\\d{1,2})?$");
    private static final Pattern ALPHA_ONLY =
            Pattern.compile("^[A-Za-zÀ-ÖØ-öø-ÿ\\s'\\-]+$");

    // ====== FXML ======
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private ComboBox<EtablissementItem> etablissementBox;
    @FXML private Label etablissementError;

    @FXML private TextField nomField;
    @FXML private ComboBox<String> categorieBox;
    @FXML private ComboBox<String> niveauBox;
    @FXML private TextField dureeField;
    @FXML private TextArea descArea;

    // Nouveaux champs
    @FXML private ComboBox<String> statutBox;
    @FXML private TextField ageMinField;
    @FXML private Label ageMinError;
    @FXML private TextField prixField;
    @FXML private Label prixError;
    @FXML private ComboBox<String> deviseBox;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField nbPlacesField;
    @FXML private Label nbPlacesError;
    @FXML private TextField placesDispoField;
    @FXML private Label placesDispoError;
    @FXML private TextField adresseDepartField;
    @FXML private Label adresseDepartError;
    @FXML private TextArea equipementArea;
    @FXML private Label equipementError;
    @FXML private TextArea conditionsArea;
    @FXML private Label conditionsError;
    @FXML private Label dateDebutError;
    @FXML private Label dateFinError;

    @FXML private Label messageLabel;
    @FXML private Button saveBtn;

    @FXML private HBox imagesStrip;

    // ====== Errors ======
    @FXML private Label nomError;
    @FXML private Label categorieError;
    @FXML private Label niveauError;
    @FXML private Label dureeError;
    @FXML private Label descError;

    // ====== Services ======
    private final ActiviteServices service = new ActiviteServices();
    private final ActiviteImageServices imageService = new ActiviteImageServices();
    private final EtablissementServices etabService = new EtablissementServices();

    private final List<File> selectedImageFiles = new ArrayList<>();

    @FXML
    public void initialize() {
        // combos
        categorieBox.setItems(FXCollections.observableArrayList("Sport", "Culture", "Loisir", "Nature", "Autre"));
        niveauBox.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé"));
        if (statutBox != null) statutBox.setItems(FXCollections.observableArrayList("disponible", "complete", "annulee"));
        if (deviseBox != null) {
            deviseBox.setItems(FXCollections.observableArrayList("TND", "EUR", "USD"));
            deviseBox.setValue("TND");
        }

        // charger établissements (FK)
        loadEtablissements();

        // mode edit
        if (AffichageActiviteController.activiteToEdit != null) {
            Activite a = AffichageActiviteController.activiteToEdit;
            if (titleLabel != null) titleLabel.setText("Modifier l'activité");
            if (subtitleLabel != null) subtitleLabel.setText("Modifiez les informations de l'activité");
            if (saveBtn != null) saveBtn.setText("Enregistrer les modifications");
            nomField.setText(safe(a.getNomActivite()));
            categorieBox.setValue(blankToNull(a.getCategorie()));
            niveauBox.setValue(blankToNull(a.getNiveau()));
            dureeField.setText(a.getDuree() == null ? "" : String.valueOf(a.getDuree()));
            descArea.setText(safe(a.getDescription()));

            // Nouveaux champs en mode edit
            if (statutBox != null && a.getStatut() != null) statutBox.setValue(a.getStatut());
            if (ageMinField != null) ageMinField.setText(a.getAgeMin() == null ? "" : String.valueOf(a.getAgeMin()));
            if (prixField != null) prixField.setText(a.getPrix() == null ? "" : a.getPrix().toPlainString());
            if (deviseBox != null && a.getDevise() != null && !a.getDevise().isBlank()) deviseBox.setValue(a.getDevise());
            if (dateDebutPicker != null && a.getDateDebut() != null) dateDebutPicker.setValue(a.getDateDebut().toLocalDateTime().toLocalDate());
            if (dateFinPicker != null && a.getDateFin() != null) dateFinPicker.setValue(a.getDateFin().toLocalDateTime().toLocalDate());
            if (nbPlacesField != null) nbPlacesField.setText(a.getNbPlaces() == null ? "" : String.valueOf(a.getNbPlaces()));
            if (placesDispoField != null) placesDispoField.setText(a.getPlacesDispo() == null ? "" : String.valueOf(a.getPlacesDispo()));
            if (adresseDepartField != null) adresseDepartField.setText(safe(a.getAdresseDepart()));
            if (equipementArea != null) equipementArea.setText(safe(a.getEquipementInclus()));
            if (conditionsArea != null) conditionsArea.setText(safe(a.getConditionsAnnulation()));

            // Sélectionner l'établissement correspondant
            if (a.getIdEtablissement() != null) {
                for (EtablissementItem item : etablissementBox.getItems()) {
                    if (item.getId() == a.getIdEtablissement()) {
                        etablissementBox.setValue(item);
                        break;
                    }
                }
            }
        }

        // validation live
        nomField.textProperty().addListener((o, a, b) -> validateAll());
        categorieBox.valueProperty().addListener((o, a, b) -> validateAll());
        niveauBox.valueProperty().addListener((o, a, b) -> validateAll());
        dureeField.textProperty().addListener((o, a, b) -> validateAll());
        descArea.textProperty().addListener((o, a, b) -> validateAll());
        etablissementBox.valueProperty().addListener((o, a, b) -> validateAll());
        if (prixField != null) prixField.textProperty().addListener((o, a, b) -> validateAll());
        if (nbPlacesField != null) nbPlacesField.textProperty().addListener((o, a, b) -> validateAll());
        if (placesDispoField != null) placesDispoField.textProperty().addListener((o, a, b) -> validateAll());
        if (ageMinField != null) ageMinField.textProperty().addListener((o, a, b) -> validateAll());

        validateAll();
        rebuildImagesStrip();
    }

    // ================== ETABLISSEMENTS (FK) ==================

    private void loadEtablissements() {
        // ✅ Affichage dans ComboBox
        etablissementBox.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(EtablissementItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNom());
            }
        });
        etablissementBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(EtablissementItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Choisir un établissement" : item.getNom());
            }
        });

        // Charger les établissements depuis la base de données
        try {
            List<Etablissement> list = etabService.afficher();
            List<EtablissementItem> items = list.stream()
                    .map(e -> new EtablissementItem(e.getIdEtablissement(), e.getNom()))
                    .toList();
            etablissementBox.setItems(FXCollections.observableArrayList(items));
        } catch (Exception ex) {
            System.err.println("Erreur chargement établissements: " + ex.getMessage());
            etablissementBox.setItems(FXCollections.observableArrayList());
        }
    }

    // private void selectEtablissementById(Integer id) {
    //     if (id == null) return;
    //     for (EtablissementItem it : etablissementBox.getItems()) {
    //         if (it.getId() == id) {
    //             etablissementBox.getSelectionModel().select(it);
    //             return;
    //         }
    //     }
    // }

    // ================= IMAGES =================

    @FXML
    private void onChooseImages(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir des images");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        List<File> files = fc.showOpenMultipleDialog(nomField.getScene().getWindow());
        if (files == null || files.isEmpty()) return;

        // ✅ sans doublons
        for (File f : files) {
            if (f == null || !f.exists()) continue;
            boolean exists = selectedImageFiles.stream()
                    .anyMatch(x -> x.getAbsolutePath().equalsIgnoreCase(f.getAbsolutePath()));
            if (!exists) selectedImageFiles.add(f);
        }

        rebuildImagesStrip();
    }

    private void rebuildImagesStrip() {
        if (imagesStrip == null) return;

        imagesStrip.getChildren().clear();

        for (File f : selectedImageFiles) {
            ImageView iv = new ImageView(new Image(f.toURI().toString(), 110, 75, true, true));
            iv.setFitWidth(110);
            iv.setFitHeight(75);
            iv.setPreserveRatio(true);

            StackPane thumb = new StackPane(iv);
            thumb.getStyleClass().add("gallery-thumb-card");

            Label del = new Label("✕");
            del.getStyleClass().add("gallery-thumb-delete");
            del.setOnMouseClicked(e -> {
                selectedImageFiles.remove(f);   // ✅ suppression PAR FICHIER (pas index)
                rebuildImagesStrip();
            });

            StackPane wrap = new StackPane(thumb, del);
            StackPane.setAlignment(del, Pos.TOP_RIGHT);

            imagesStrip.getChildren().add(wrap);
        }
    }

    /**
     * ⚠️ CONSEIL : ne copie pas dans src/main/resources en runtime (ça marche mal après packaging JAR).
     * Ici je copie dans un dossier "uploads/activites" dans le dossier du projet.
     */
    private String copyImageToUploads(File src) throws Exception {
        String original = src.getName();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot);

        String newName = "act_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8) + ext;

        Path targetDir = Paths.get("uploads", "activites");
        if (!Files.exists(targetDir)) Files.createDirectories(targetDir);

        Path target = targetDir.resolve(newName);
        Files.copy(src.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

        return newName; // on sauvegarde ce nom dans DB
    }

    private void saveImagesToDB(int idActivite) throws Exception {
        if (idActivite <= 0 || selectedImageFiles.isEmpty()) return;

        int ordre = 1;
        for (File imgFile : selectedImageFiles) {
            String storedName = copyImageToUploads(imgFile);

            ActiviteImage img = new ActiviteImage();
            img.setIdActivite(idActivite);
            img.setImagePath(storedName);
            img.setOrdreAffichage(ordre++);

            imageService.ajouterImage(img);
        }
    }

    // ================= SAVE =================

    @FXML
    private void onSave(ActionEvent event) {
        if (!validateAll()) return;

        try {
            // FK : idEtablissement
            EtablissementItem etab = etablissementBox.getValue();
            int idEtablissement = etab.getId();

            String nom = safe(nomField.getText()).trim();
            String cat = categorieBox.getValue();
            String niv = niveauBox.getValue();
            String desc = safe(descArea.getText()).trim();
            Integer duree = parseIntOrNull(dureeField.getText());

            // Nouveaux champs
            BigDecimal prix = parseBigDecimalOrNull(prixField);
            String devise = (deviseBox != null && deviseBox.getValue() != null) ? deviseBox.getValue() : "TND";
            String statut = (statutBox != null && statutBox.getValue() != null) ? statutBox.getValue() : "disponible";
            Integer ageMin = parseIntOrNull(ageMinField != null ? ageMinField.getText() : null);
            Integer nbPlaces = parseIntOrNull(nbPlacesField != null ? nbPlacesField.getText() : null);
            Integer placesDispo = parseIntOrNull(placesDispoField != null ? placesDispoField.getText() : null);
            String adresseDepart = adresseDepartField != null ? safe(adresseDepartField.getText()).trim() : "";
            String equipement = equipementArea != null ? safe(equipementArea.getText()).trim() : "";
            String conditions = conditionsArea != null ? safe(conditionsArea.getText()).trim() : "";

            Timestamp dateDebut = null;
            if (dateDebutPicker != null && dateDebutPicker.getValue() != null) {
                dateDebut = Timestamp.valueOf(dateDebutPicker.getValue().atStartOfDay());
            }
            Timestamp dateFin = null;
            if (dateFinPicker != null && dateFinPicker.getValue() != null) {
                dateFin = Timestamp.valueOf(dateFinPicker.getValue().atStartOfDay());
            }

            Activite a = (AffichageActiviteController.activiteToEdit == null)
                    ? new Activite()
                    : AffichageActiviteController.activiteToEdit;

            a.setNomActivite(nom);
            a.setCategorie(cat);
            a.setNiveau(niv);
            a.setDuree(duree);
            a.setDescription(desc);
            a.setIdEtablissement(idEtablissement);
            a.setPrix(prix);
            a.setDevise(devise);
            a.setStatut(statut);
            a.setAgeMin(ageMin);
            a.setNbPlaces(nbPlaces);
            a.setPlacesDispo(placesDispo);
            a.setAdresseDepart(adresseDepart.isBlank() ? null : adresseDepart);
            a.setEquipementInclus(equipement.isBlank() ? null : equipement);
            a.setConditionsAnnulation(conditions.isBlank() ? null : conditions);
            a.setDateDebut(dateDebut);
            a.setDateFin(dateFin);

            if (AffichageActiviteController.activiteToEdit == null) {
                int newId = service.ajouterEtRetournerId(a);
                saveImagesToDB(newId);

                try {
                    SuccessNotification.show(saveBtn, "Activité ajoutée avec succès !", () -> {
                        NavigationUtils.goTo("/activite_affichage.fxml", event);
                    });
                } catch (Exception notifEx) {
                    notifEx.printStackTrace();
                    NavigationUtils.goTo("/activite_affichage.fxml", event);
                }
            } else {
                service.modifier(a);
                if (!selectedImageFiles.isEmpty()) saveImagesToDB(a.getIdActivite());
                AffichageActiviteController.activiteToEdit = null;

                try {
                    SuccessNotification.show(saveBtn, "Activité modifiée avec succès !", () -> {
                        NavigationUtils.goTo("/activite_affichage.fxml", event);
                    });
                } catch (Exception notifEx) {
                    notifEx.printStackTrace();
                    NavigationUtils.goTo("/activite_affichage.fxml", event);
                }
            }

        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel(ActionEvent event) {
        AffichageActiviteController.activiteToEdit = null;
        NavigationUtils.goTo("/activite_affichage.fxml", event);
    }

    // ================= VALIDATION =================

    private boolean validateAll() {
        clearErrors();
        boolean valid = true;

        // ──────── Établissement (obligatoire) ────────
        if (etablissementBox.getValue() == null) {
            setError(etablissementError, "Choisis un établissement.");
            markError(etablissementBox);
            valid = false;
        }

        // ──────── Nom (obligatoire, 3-120 chars, pas de chiffres seuls) ────────
        String nom = safe(nomField.getText()).trim();
        if (nom.isBlank()) {
            setError(nomError, "Le nom est obligatoire.");
            markError(nomField);
            valid = false;
        } else if (nom.length() < 3) {
            setError(nomError, "Le nom doit contenir au moins 3 caractères.");
            markError(nomField);
            valid = false;
        } else if (nom.length() > 120) {
            setError(nomError, "Le nom ne doit pas dépasser 120 caractères.");
            markError(nomField);
            valid = false;
        } else if (!NAME_PATTERN.matcher(nom).matches()) {
            setError(nomError, "Le nom contient des caractères non autorisés.");
            markError(nomField);
            valid = false;
        } else if (nom.matches("^\\d+$")) {
            setError(nomError, "Le nom ne peut pas être uniquement des chiffres.");
            markError(nomField);
            valid = false;
        }

        // ──────── Catégorie (obligatoire) ────────
        String cat = categorieBox.getValue();
        if (cat == null || cat.isBlank()) {
            setError(categorieError, "La catégorie est obligatoire.");
            markError(categorieBox);
            valid = false;
        }

        // ──────── Niveau (obligatoire) ────────
        String niv = niveauBox.getValue();
        if (niv == null || niv.isBlank()) {
            setError(niveauError, "Le niveau est obligatoire.");
            markError(niveauBox);
            valid = false;
        }

        // ──────── Durée (optionnel, mais si rempli: entier > 0, max 9999) ────────
        String dTxt = safe(dureeField.getText()).trim();
        if (!dTxt.isEmpty()) {
            if (!DIGITS_ONLY.matcher(dTxt).matches()) {
                setError(dureeError, "La durée doit contenir uniquement des chiffres.");
                markError(dureeField);
                valid = false;
            } else {
                Integer d = parseIntOrNull(dTxt);
                if (d == null || d <= 0) {
                    setError(dureeError, "La durée doit être supérieure à 0.");
                    markError(dureeField);
                    valid = false;
                } else if (d > 9999) {
                    setError(dureeError, "La durée ne peut pas dépasser 9999 minutes.");
                    markError(dureeField);
                    valid = false;
                }
            }
        }

        // ──────── Âge minimum (optionnel, 0-120) ────────
        if (ageMinField != null) {
            String ageTxt = safe(ageMinField.getText()).trim();
            if (!ageTxt.isEmpty()) {
                if (!DIGITS_ONLY.matcher(ageTxt).matches()) {
                    setError(ageMinError, "L'âge doit contenir uniquement des chiffres.");
                    markError(ageMinField);
                    valid = false;
                } else {
                    Integer age = parseIntOrNull(ageTxt);
                    if (age == null || age < 0) {
                        setError(ageMinError, "L'âge minimum doit être >= 0.");
                        markError(ageMinField);
                        valid = false;
                    } else if (age > 120) {
                        setError(ageMinError, "L'âge minimum ne peut pas dépasser 120 ans.");
                        markError(ageMinField);
                        valid = false;
                    }
                }
            }
        }

        // ──────── Description (optionnel, max 5000 chars, min 10 si rempli) ────────
        String desc = safe(descArea.getText()).trim();
        if (!desc.isEmpty()) {
            if (desc.length() < 10) {
                setError(descError, "La description doit contenir au moins 10 caractères.");
                markError(descArea);
                valid = false;
            } else if (desc.length() > 5000) {
                setError(descError, "La description ne doit pas dépasser 5000 caractères.");
                markError(descArea);
                valid = false;
            }
        }

        // ──────── Prix (optionnel, >= 0, format decimal max 2 decimals) ────────
        if (prixField != null) {
            String pTxt = safe(prixField.getText()).trim();
            if (!pTxt.isEmpty()) {
                if (!PRIX_PATTERN.matcher(pTxt).matches()) {
                    setError(prixError, "Format invalide (ex: 45 ou 45.50).");
                    markError(prixField);
                    valid = false;
                } else {
                    BigDecimal p = parseBigDecimalOrNull(prixField);
                    if (p == null) {
                        setError(prixError, "Prix invalide.");
                        markError(prixField);
                        valid = false;
                    } else if (p.compareTo(BigDecimal.ZERO) < 0) {
                        setError(prixError, "Le prix doit être >= 0.");
                        markError(prixField);
                        valid = false;
                    } else if (p.compareTo(new BigDecimal("99999999.99")) > 0) {
                        setError(prixError, "Le prix est trop élevé.");
                        markError(prixField);
                        valid = false;
                    }
                }
            }
        }

        // ──────── Date début / Date fin (cohérence) ────────
        LocalDate dateDebut = (dateDebutPicker != null) ? dateDebutPicker.getValue() : null;
        LocalDate dateFin = (dateFinPicker != null) ? dateFinPicker.getValue() : null;

        if (dateDebut != null && dateFin != null) {
            if (dateFin.isBefore(dateDebut)) {
                setError(dateFinError, "La date de fin doit être après la date de début.");
                markError(dateFinPicker);
                valid = false;
            }
            if (dateDebut.isEqual(dateFin)) {
                setError(dateFinError, "La date de fin doit être différente de la date de début.");
                markError(dateFinPicker);
                valid = false;
            }
        }

        // ──────── Nombre de places (optionnel, > 0, max 100000) ────────
        Integer nbPlacesVal = null;
        if (nbPlacesField != null) {
            String nbTxt = safe(nbPlacesField.getText()).trim();
            if (!nbTxt.isEmpty()) {
                if (!DIGITS_ONLY.matcher(nbTxt).matches()) {
                    setError(nbPlacesError, "Doit contenir uniquement des chiffres.");
                    markError(nbPlacesField);
                    valid = false;
                } else {
                    nbPlacesVal = parseIntOrNull(nbTxt);
                    if (nbPlacesVal == null || nbPlacesVal <= 0) {
                        setError(nbPlacesError, "Le nombre de places doit être > 0.");
                        markError(nbPlacesField);
                        valid = false;
                    } else if (nbPlacesVal > 100000) {
                        setError(nbPlacesError, "Le nombre de places ne peut pas dépasser 100 000.");
                        markError(nbPlacesField);
                        valid = false;
                    }
                }
            }
        }

        // ──────── Places disponibles (optionnel, >= 0, <= nb_places) ────────
        if (placesDispoField != null) {
            String dpTxt = safe(placesDispoField.getText()).trim();
            if (!dpTxt.isEmpty()) {
                if (!DIGITS_ONLY.matcher(dpTxt).matches()) {
                    setError(placesDispoError, "Doit contenir uniquement des chiffres.");
                    markError(placesDispoField);
                    valid = false;
                } else {
                    Integer dp = parseIntOrNull(dpTxt);
                    if (dp == null || dp < 0) {
                        setError(placesDispoError, "Les places disponibles doivent être >= 0.");
                        markError(placesDispoField);
                        valid = false;
                    } else if (nbPlacesVal != null && dp > nbPlacesVal) {
                        setError(placesDispoError, "Ne peut pas dépasser le nombre total de places (" + nbPlacesVal + ").");
                        markError(placesDispoField);
                        valid = false;
                    }
                }
            }
        }

        // ──────── Adresse départ (optionnel, min 5 chars si rempli) ────────
        if (adresseDepartField != null) {
            String adr = safe(adresseDepartField.getText()).trim();
            if (!adr.isEmpty() && adr.length() < 5) {
                setError(adresseDepartError, "L'adresse doit contenir au moins 5 caractères.");
                markError(adresseDepartField);
                valid = false;
            }
        }

        // ──────── Équipement inclus (optionnel, max 255 chars) ────────
        if (equipementArea != null) {
            String eq = safe(equipementArea.getText()).trim();
            if (!eq.isEmpty() && eq.length() > 255) {
                setError(equipementError, "L'équipement ne doit pas dépasser 255 caractères.");
                markError(equipementArea);
                valid = false;
            }
        }

        // ──────── Conditions annulation (optionnel, max 255 chars) ────────
        if (conditionsArea != null) {
            String cond = safe(conditionsArea.getText()).trim();
            if (!cond.isEmpty() && cond.length() > 255) {
                setError(conditionsError, "Les conditions ne doivent pas dépasser 255 caractères.");
                markError(conditionsArea);
                valid = false;
            }
        }

        // ──────── Résultat ────────
        if (saveBtn != null) saveBtn.setDisable(!valid);

        if (messageLabel != null) {
            if (valid) setMessage("", true);
            // else: errors are already shown per-field
        }

        return valid;
    }

    private void clearErrors() {
        removeError(etablissementBox);
        removeError(nomField);
        removeError(categorieBox);
        removeError(niveauBox);
        removeError(dureeField);
        removeError(descArea);
        if (prixField != null) removeError(prixField);
        if (nbPlacesField != null) removeError(nbPlacesField);
        if (placesDispoField != null) removeError(placesDispoField);
        if (ageMinField != null) removeError(ageMinField);
        if (adresseDepartField != null) removeError(adresseDepartField);
        if (equipementArea != null) removeError(equipementArea);
        if (conditionsArea != null) removeError(conditionsArea);
        if (dateDebutPicker != null) removeError(dateDebutPicker);
        if (dateFinPicker != null) removeError(dateFinPicker);

        if (etablissementError != null) etablissementError.setText("");
        if (nomError != null) nomError.setText("");
        if (categorieError != null) categorieError.setText("");
        if (niveauError != null) niveauError.setText("");
        if (dureeError != null) dureeError.setText("");
        if (descError != null) descError.setText("");
        if (prixError != null) prixError.setText("");
        if (nbPlacesError != null) nbPlacesError.setText("");
        if (placesDispoError != null) placesDispoError.setText("");
        if (ageMinError != null) ageMinError.setText("");
        if (adresseDepartError != null) adresseDepartError.setText("");
        if (equipementError != null) equipementError.setText("");
        if (conditionsError != null) conditionsError.setText("");
        if (dateDebutError != null) dateDebutError.setText("");
        if (dateFinError != null) dateFinError.setText("");
    }

    private void setError(Label label, String msg) {
        if (label != null) label.setText(msg);
    }

    private void markError(Control c) {
        if (c != null && !c.getStyleClass().contains("input-error")) c.getStyleClass().add("input-error");
    }

    private void removeError(Control c) {
        if (c != null) c.getStyleClass().remove("input-error");
    }

    private void setMessage(String msg, boolean ok) {
        if (messageLabel == null) return;
        messageLabel.setText(msg == null ? "" : msg);
        messageLabel.setStyle(ok
                ? "-fx-text-fill:#22c55e; -fx-font-weight:bold;"
                : "-fx-text-fill:#ef4444; -fx-font-weight:bold;");
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private Integer parseIntOrNull(String s) {
        try {
            String v = safe(s).trim();
            if (v.isBlank()) return null;
            return Integer.parseInt(v);
        } catch (Exception e) {
            return null;
        }
    }

    private String blankToNull(String s) {
        String v = safe(s).trim();
        return v.isBlank() ? null : v;
    }

    private BigDecimal parseBigDecimalOrNull(TextField field) {
        if (field == null) return null;
        String txt = safe(field.getText()).trim().replace(",", ".");
        if (txt.isBlank()) return null;
        try {
            return new BigDecimal(txt);
        } catch (Exception e) {
            return null;
        }
    }

    private String safe(String s) { return s == null ? "" : s; }

    // ================== MODEL FOR COMBO ==================
    public static class EtablissementItem {
        private final int id;
        private final String nom;

        public EtablissementItem(int id, String nom) {
            this.id = id;
            this.nom = nom;
        }

        public int getId() { return id; }
        public String getNom() { return nom; }

        @Override
        public String toString() { return nom; }
    }
}