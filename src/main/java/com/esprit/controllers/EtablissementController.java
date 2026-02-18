package com.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

import com.esprit.entities.Etablissement;
import com.esprit.services.EtablissementServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EtablissementController {
    @FXML
    private void onShowAjouter(ActionEvent event) {
        NavigationUtils.goTo("/ajouter_etablissement.fxml", event);
    }

    @FXML
    private void onShowModifier(ActionEvent event) {
        // Optionnel : Pré-remplir le formulaire si un établissement est sélectionné
        NavigationUtils.goTo("/ajouter_etablissement.fxml", event);
    }

    // ===== Form fields =====
    @FXML private TextField nomField;
    @FXML private TextField adresseField;
    @FXML private TextField villeField;
    @FXML private TextField telephoneField;
    @FXML private TextField emailField;
    @FXML private TextField horairesField;
    @FXML private ComboBox<String> gammePrixField;
    @FXML private TextField imageUrlField;
    @FXML private TextArea descriptionArea;

    @FXML private Label messageLabel;
    @FXML private Label selectionInfoLabel;

    // ===== List + filters =====
    @FXML private ListView<Etablissement> etablissementList;
    @FXML private TextField searchField;

    // TOP filters
    @FXML private ComboBox<String> filterVille;
    @FXML private ComboBox<String> filterGamme;
    @FXML private ComboBox<String> sortBox;

    // SIDE filters (panel gauche)
    @FXML private ComboBox<String> filterVilleSide;
    @FXML private ComboBox<String> filterGammeSide;

    private final EtablissementServices service = new EtablissementServices();

    private final ObservableList<Etablissement> masterData = FXCollections.observableArrayList();
    private FilteredList<Etablissement> filteredData;
    private SortedList<Etablissement> sortedData;

    private Etablissement selected = null;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+ ]*$");

    @FXML
    public void initialize() {

        // ==== form combo ====
        gammePrixField.setItems(FXCollections.observableArrayList("€", "€€", "€€€"));

        // ==== filters combos ====
        filterGamme.setItems(FXCollections.observableArrayList("Tous", "€", "€€", "€€€"));
        filterGamme.getSelectionModel().selectFirst();

        filterVille.setItems(FXCollections.observableArrayList("Tous"));
        filterVille.getSelectionModel().selectFirst();

        sortBox.setItems(FXCollections.observableArrayList("Aucun", "Nom A→Z", "Ville A→Z"));
        sortBox.getSelectionModel().selectFirst();

        // ==== load data ====
        loadData();

        filteredData = new FilteredList<>(masterData, e -> true);
        sortedData = new SortedList<>(filteredData);
        etablissementList.setItems(sortedData);

        // ==== cell factory (cards) ====
        etablissementList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Etablissement e, boolean empty) {
                super.updateItem(e, empty);

                if (empty || e == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // Image
                ImageView iv = new ImageView();
                iv.setFitWidth(90);
                iv.setFitHeight(70);
                iv.setPreserveRatio(true);

                String img = e.getImageUrl();
                // load default image URL if present on classpath
                java.net.URL defaultUrl = getClass().getResource("/img/default.png");
                javafx.scene.image.Image defaultImage = null;
                if (defaultUrl != null) {
                    try {
                        defaultImage = new Image(defaultUrl.toExternalForm());
                    } catch (Exception ignored) {
                        defaultImage = null;
                    }
                }

                if (img != null && !img.isBlank()) {
                    try {
                        iv.setImage(new Image(img, true));
                    } catch (Exception ex) {
                        // fallback to bundled default image if available
                        iv.setImage(defaultImage);
                    }
                } else {
                    iv.setImage(defaultImage);
                }

                // Infos
                Label nom = new Label(safe(e.getNom()));
                nom.setStyle("-fx-font-size:16; -fx-text-fill:#FFD700; -fx-font-weight:bold;");

                Label meta = new Label(safe(e.getVille()) + "   •   " + safe(e.getGammePrix()));
                meta.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12;");

                String desc = safe(e.getDescription());
                if (desc.length() > 100) desc = desc.substring(0, 100) + "...";
                Label description = new Label(desc);
                description.setWrapText(true);
                description.setStyle("-fx-text-fill:#cccccc; -fx-font-size:12;");

                Label contact = new Label(
                        (!safe(e.getTelephone()).isBlank() ? "📞 " + safe(e.getTelephone()) + "   " : "") +
                                (!safe(e.getEmail()).isBlank() ? "✉ " + safe(e.getEmail()) : "")
                );
                contact.setStyle("-fx-text-fill:#aaaaaa; -fx-font-size:12;");

                VBox infoBox = new VBox(4, nom, meta, contact, description);
                HBox.setHgrow(infoBox, Priority.ALWAYS);

                // Buttons inside card
                Button btnEdit = new Button("Modifier");
                btnEdit.setStyle("-fx-padding:8 14; -fx-font-size:12; -fx-background-radius:10; -fx-background-color: rgba(255,255,255,0.12); -fx-text-fill:white;");
                btnEdit.setOnAction(ev -> {
                    etablissementList.getSelectionModel().select(e);
                    fillForm(e);
                });

                Button btnDelete = new Button("Supprimer");
                btnDelete.setStyle("-fx-padding:8 14; -fx-font-size:12; -fx-background-radius:10; -fx-background-color:#ff4d4d; -fx-text-fill:white;");
                btnDelete.setOnAction(ev -> {
                    boolean ok = AlertUtils.confirm("Confirmation", "Supprimer établissement",
                            "Voulez-vous supprimer : " + safe(e.getNom()) + " ?");
                    if (!ok) return;

                    try {
                        service.supprimer(e.getIdEtablissement());
                        loadData();
                        applyFilters();
                        clearForm();
                        setMessage("✅ Supprimé.", true);
                    } catch (Exception ex) {
                        setMessage("❌ " + ex.getMessage(), false);
                    }
                });

                VBox actions = new VBox(8, btnEdit, btnDelete);
                actions.setMinWidth(120);

                HBox root = new HBox(15, iv, infoBox, actions);
                root.setStyle("-fx-background-color: rgba(10,10,10,0.98); -fx-background-radius:15; -fx-padding:15;");

                setGraphic(root);
                setText(null);
            }
        });

        // ==== sync TOP <-> SIDE filters (optional panel gauche) ====
        syncTopAndSideFilters();

        // ==== listeners ====
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        filterVille.valueProperty().addListener((obs, o, n) -> applyFilters());
        filterGamme.valueProperty().addListener((obs, o, n) -> applyFilters());

        if (filterVilleSide != null) filterVilleSide.valueProperty().addListener((obs, o, n) -> applyFilters());
        if (filterGammeSide != null) filterGammeSide.valueProperty().addListener((obs, o, n) -> applyFilters());

        sortBox.valueProperty().addListener((obs, o, n) -> applySort());
        applySort();

        // Selection -> fill form
        etablissementList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selected = n;
            if (n != null) {
                fillForm(n);
                selectionInfoLabel.setText("Sélection: #" + n.getIdEtablissement() + " — " + safe(n.getNom()));
            } else {
                selectionInfoLabel.setText("Aucune sélection");
            }
        });
    }

    private void syncTopAndSideFilters() {
        // Copier items top -> side
        if (filterVilleSide != null) {
            filterVilleSide.setItems(filterVille.getItems());
            filterVilleSide.setValue(filterVille.getValue());
        }
        if (filterGammeSide != null) {
            filterGammeSide.setItems(filterGamme.getItems());
            filterGammeSide.setValue(filterGamme.getValue());
        }

        // side -> top
        if (filterVilleSide != null) {
            filterVilleSide.valueProperty().addListener((obs, o, n) -> {
                if (n != null) filterVille.setValue(n);
            });
        }
        if (filterGammeSide != null) {
            filterGammeSide.valueProperty().addListener((obs, o, n) -> {
                if (n != null) filterGamme.setValue(n);
            });
        }

        // top -> side
        filterVille.valueProperty().addListener((obs, o, n) -> {
            if (filterVilleSide != null && n != null) filterVilleSide.setValue(n);
        });
        filterGamme.valueProperty().addListener((obs, o, n) -> {
            if (filterGammeSide != null && n != null) filterGammeSide.setValue(n);
        });
    }

    private void loadData() {
        masterData.clear();
        try {
            masterData.addAll(service.afficher());
            refreshVilleFilter();
        } catch (SQLException e) {
            setMessage("Erreur chargement données: " + e.getMessage(), false);
        }
    }

    private void refreshVilleFilter() {
        Set<String> villes = masterData.stream()
                .map(Etablissement::getVille)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        String current = filterVille.getValue();

        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("Tous");
        items.addAll(villes.stream().sorted().collect(Collectors.toList()));

        filterVille.setItems(items);

        if (current != null && items.contains(current)) filterVille.setValue(current);
        else filterVille.getSelectionModel().selectFirst();

        // resync side
        if (filterVilleSide != null) {
            filterVilleSide.setItems(filterVille.getItems());
            filterVilleSide.setValue(filterVille.getValue());
        }
    }

    private void applyFilters() {
        String q = safe(searchField.getText()).toLowerCase().trim();

        // On prend la valeur TOP (car side est synchronisé)
        String ville = safe(filterVille.getValue());
        String gamme = safe(filterGamme.getValue());

        filteredData.setPredicate(e -> {
            if (e == null) return false;

            if (!"Tous".equalsIgnoreCase(ville) && !safe(e.getVille()).equalsIgnoreCase(ville)) return false;
            if (!"Tous".equalsIgnoreCase(gamme) && !safe(e.getGammePrix()).equalsIgnoreCase(gamme)) return false;

            if (q.isEmpty()) return true;

            return safe(e.getNom()).toLowerCase().contains(q)
                    || safe(e.getVille()).toLowerCase().contains(q)
                    || safe(e.getEmail()).toLowerCase().contains(q)
                    || safe(e.getTelephone()).toLowerCase().contains(q);
        });
    }

    private void applySort() {
        String sort = safe(sortBox.getValue());

        Comparator<Etablissement> c;
        switch (sort) {
            case "Nom A→Z":
                c = Comparator.comparing(e -> safe(e.getNom()).toLowerCase());
                break;
            case "Ville A→Z":
                c = Comparator.comparing(e -> safe(e.getVille()).toLowerCase());
                break;
            default:
                c = null;
        }
        sortedData.setComparator(c);
    }

    // ===== CRUD =====
    @FXML private void onAjouter() {
        setMessage("", true);

        Etablissement e = readForm();
        if (e == null) return;

        try {
            service.ajouter(e);
            setMessage("✅ Ajouté.", true);
            onRefresh();
            clearForm();
        } catch (SQLException ex) {
            setMessage("❌ Erreur ajout: " + ex.getMessage(), false);
        }
    }

    @FXML private void onModifier() {
        setMessage("", true);

        if (selected == null) {
            setMessage("⚠️ Sélectionne un établissement avant de modifier.", false);
            return;
        }

        Etablissement e = readForm();
        if (e == null) return;

        e.setIdEtablissement(selected.getIdEtablissement());

        try {
            service.modifier(e);
            setMessage("✅ Modifié.", true);
            onRefresh();
        } catch (SQLException ex) {
            setMessage("❌ Erreur modification: " + ex.getMessage(), false);
        }
    }

    @FXML private void onSupprimer() {
        setMessage("", true);

        if (selected == null) {
            setMessage("⚠️ Sélectionne un établissement avant de supprimer.", false);
            return;
        }

        boolean ok = AlertUtils.confirm("Confirmation", "Supprimer établissement",
                "Voulez-vous supprimer : " + safe(selected.getNom()) + " ?");
        if (!ok) return;

        try {
            service.supprimer(selected.getIdEtablissement());
            setMessage("✅ Supprimé.", true);
            onRefresh();
            clearForm();
        } catch (SQLException ex) {
            setMessage("❌ Erreur suppression: " + ex.getMessage(), false);
        }
    }

    @FXML private void onVider() {
        clearForm();
        setMessage("", true);
    }

    @FXML private void onRefresh() {
        loadData();
        applyFilters();
    }

    private void clearForm() {
        nomField.clear();
        adresseField.clear();
        villeField.clear();
        telephoneField.clear();
        emailField.clear();
        horairesField.clear();
        gammePrixField.getSelectionModel().clearSelection();
        imageUrlField.clear();
        descriptionArea.clear();

        selected = null;
        etablissementList.getSelectionModel().clearSelection();
        selectionInfoLabel.setText("Aucune sélection");
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

    private Etablissement readForm() {
        String nom = safe(nomField.getText()).trim();
        String ville = safe(villeField.getText()).trim();
        String tel = safe(telephoneField.getText()).trim();
        String email = safe(emailField.getText()).trim();
        String gamme = (gammePrixField.getValue() == null) ? "" : gammePrixField.getValue().trim();

        if (nom.isBlank()) { setMessage("❌ Nom obligatoire.", false); return null; }
        if (ville.isBlank()) { setMessage("❌ Ville obligatoire.", false); return null; }
        if (!email.isBlank() && !email.contains("@")) { setMessage("❌ Email invalide.", false); return null; }
        if (!tel.isBlank() && !PHONE_PATTERN.matcher(tel).matches()) { setMessage("❌ Téléphone invalide.", false); return null; }
        if (!gamme.isBlank() && !(gamme.equals("€") || gamme.equals("€€") || gamme.equals("€€€"))) {
            setMessage("❌ Gamme prix invalide.", false);
            return null;
        }

        Etablissement e = new Etablissement();
        e.setNom(nom);
        e.setAdresse(safe(adresseField.getText()).trim());
        e.setVille(ville);
        e.setTelephone(tel);
        e.setEmail(email);
        e.setHoraires(safe(horairesField.getText()).trim());
        e.setGammePrix(gamme);
        e.setImageUrl(safe(imageUrlField.getText()).trim());
        e.setDescription(safe(descriptionArea.getText()).trim());

        return e;
    }

    private void setMessage(String msg, boolean ok) {
        messageLabel.setText(msg == null ? "" : msg);
        messageLabel.setStyle(ok
                ? "-fx-text-fill:#22c55e; -fx-font-weight:bold;"
                : "-fx-text-fill:#ef4444; -fx-font-weight:bold;");
    }

    private String safe(String s) { return s == null ? "" : s; }
}
