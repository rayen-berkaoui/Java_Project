package com.esprit.controllers;

import com.esprit.entities.Etablissement;
import com.esprit.services.EtablissementServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EtablissementController {

    // ===== Form fields =====
    @FXML private TextField nomField;
    @FXML private TextField adresseField;
    @FXML private TextField villeField;
    @FXML private TextField telephoneField;
    @FXML private TextField emailField;
    @FXML private TextField horairesField;
    @FXML private ComboBox<String> gammePrixField;

    @FXML private ComboBox<String> typeField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;

    @FXML private TextArea descriptionArea;

    @FXML private Label messageLabel;
    @FXML private Label selectionInfoLabel;

    // ===== List + filters =====
    @FXML private ListView<Etablissement> etablissementList;
    @FXML private TextField searchField;

    // TOP filters
    @FXML private ComboBox<String> filterVille;
    @FXML private ComboBox<String> filterGamme;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> sortBox;

    // SIDE filters
    @FXML private ComboBox<String> filterVilleSide;
    @FXML private ComboBox<String> filterGammeSide;
    @FXML private ComboBox<String> filterTypeSide;

    private final EtablissementServices service = new EtablissementServices();

    private final ObservableList<Etablissement> masterData = FXCollections.observableArrayList();
    private FilteredList<Etablissement> filteredData;
    private SortedList<Etablissement> sortedData;

    private Etablissement selected = null;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // تونس: 8 chiffres يبدأ بـ 2/3/4/5/7/9
    private static final Pattern TN_PHONE_PATTERN =
            Pattern.compile("^[234579][0-9]{7}$");

    @FXML
    public void initialize() {

        // ===== combos form =====
        gammePrixField.setItems(FXCollections.observableArrayList("€", "€€", "€€€"));
        typeField.setItems(FXCollections.observableArrayList("hotel", "restaurant", "cafe", "museum", "bar", "autre"));

        // ===== combos filters =====
        filterGamme.setItems(FXCollections.observableArrayList("Tous", "€", "€€", "€€€"));
        filterGamme.getSelectionModel().selectFirst();

        filterType.setItems(FXCollections.observableArrayList("Tous", "hotel", "restaurant", "cafe", "museum", "bar", "autre"));
        filterType.getSelectionModel().selectFirst();

        filterVille.setItems(FXCollections.observableArrayList("Tous"));
        filterVille.getSelectionModel().selectFirst();

        sortBox.setItems(FXCollections.observableArrayList("Aucun", "Nom A→Z", "Ville A→Z", "Type A→Z"));
        sortBox.getSelectionModel().selectFirst();

        // ===== load =====
        loadData();

        filteredData = new FilteredList<>(masterData, e -> true);
        sortedData = new SortedList<>(filteredData);
        etablissementList.setItems(sortedData);

        // ✅ pour l’instant texte simple (tu peux remettre tes cards après)
        etablissementList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Etablissement e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setText(null); setGraphic(null); return; }
                setText(safe(e.getNom()) + " — " + safe(e.getVille()) + " — " + safe(e.getType()));
            }
        });

        // sync top/side
        syncTopAndSideFilters();

        // listeners
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        filterVille.valueProperty().addListener((obs, o, n) -> applyFilters());
        filterGamme.valueProperty().addListener((obs, o, n) -> applyFilters());
        filterType.valueProperty().addListener((obs, o, n) -> applyFilters());
        sortBox.valueProperty().addListener((obs, o, n) -> applySort());

        applySort();
        applyFilters();

        // selection
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

    // ===== NAV =====
    @FXML
    private void onShowAjouter(ActionEvent event) {
        NavigationUtils.goTo("/ajouter_etablissement.fxml", event);
    }

    // ===== DATA =====
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

        if (filterVilleSide != null) {
            filterVilleSide.setItems(filterVille.getItems());
            filterVilleSide.setValue(filterVille.getValue());
        }
    }

    private void applyFilters() {
        String q = safe(searchField.getText()).toLowerCase().trim();

        String ville = safe(filterVille.getValue());
        String gamme = safe(filterGamme.getValue());
        String type = safe(filterType.getValue());

        filteredData.setPredicate(e -> {
            if (e == null) return false;

            if (!"Tous".equalsIgnoreCase(ville) && !safe(e.getVille()).equalsIgnoreCase(ville)) return false;
            if (!"Tous".equalsIgnoreCase(gamme) && !safe(e.getGammePrix()).equalsIgnoreCase(gamme)) return false;
            if (!"Tous".equalsIgnoreCase(type) && !safe(e.getType()).equalsIgnoreCase(type)) return false;

            if (q.isEmpty()) return true;

            return safe(e.getNom()).toLowerCase().contains(q)
                    || safe(e.getVille()).toLowerCase().contains(q)
                    || safe(e.getEmail()).toLowerCase().contains(q)
                    || safe(e.getTelephone()).toLowerCase().contains(q)
                    || safe(e.getType()).toLowerCase().contains(q);
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
            case "Type A→Z":
                c = Comparator.comparing(e -> safe(e.getType()).toLowerCase());
                break;
            default:
                c = null;
        }
        sortedData.setComparator(c);
    }

    private void syncTopAndSideFilters() {
        // copy items
        if (filterVilleSide != null) { filterVilleSide.setItems(filterVille.getItems()); filterVilleSide.setValue(filterVille.getValue()); }
        if (filterGammeSide != null) { filterGammeSide.setItems(filterGamme.getItems()); filterGammeSide.setValue(filterGamme.getValue()); }
        if (filterTypeSide != null)  { filterTypeSide.setItems(filterType.getItems());  filterTypeSide.setValue(filterType.getValue());  }

        // side -> top
        if (filterVilleSide != null) filterVilleSide.valueProperty().addListener((obs,o,n)-> { if (n != null) filterVille.setValue(n); });
        if (filterGammeSide != null) filterGammeSide.valueProperty().addListener((obs,o,n)-> { if (n != null) filterGamme.setValue(n); });
        if (filterTypeSide != null)  filterTypeSide.valueProperty().addListener((obs,o,n)-> { if (n != null) filterType.setValue(n); });

        // top -> side
        filterVille.valueProperty().addListener((obs,o,n)-> { if (filterVilleSide != null && n != null) filterVilleSide.setValue(n); });
        filterGamme.valueProperty().addListener((obs,o,n)-> { if (filterGammeSide != null && n != null) filterGammeSide.setValue(n); });
        filterType.valueProperty().addListener((obs,o,n)-> { if (filterTypeSide != null && n != null) filterTypeSide.setValue(n); });
    }

    // ===== CRUD =====
    @FXML
    private void onAjouter() {
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

    @FXML
    private void onModifier() {
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

    @FXML
    private void onSupprimer() {
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

    @FXML
    private void onVider() {
        clearForm();
        setMessage("", true);
    }

    @FXML
    private void onRefresh() {
        loadData();
        applyFilters();
        applySort();
    }

    // ===== form helpers =====
    private void clearForm() {
        nomField.clear();
        adresseField.clear();
        villeField.clear();
        telephoneField.clear();
        emailField.clear();
        horairesField.clear();
        gammePrixField.getSelectionModel().clearSelection();

        typeField.getSelectionModel().clearSelection();
        latitudeField.clear();
        longitudeField.clear();

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
        gammePrixField.setValue(blankToNull(e.getGammePrix()));

        typeField.setValue(blankToNull(e.getType()));
        latitudeField.setText(e.getLatitude() == null ? "" : String.valueOf(e.getLatitude()));
        longitudeField.setText(e.getLongitude() == null ? "" : String.valueOf(e.getLongitude()));

        descriptionArea.setText(safe(e.getDescription()));
    }

    private Etablissement readForm() {
        String nom = safe(nomField.getText()).trim();
        String ville = safe(villeField.getText()).trim();

        String tel = safe(telephoneField.getText()).trim().replaceAll("\\s+", "");
        String email = safe(emailField.getText()).trim();

        String gamme = (gammePrixField.getValue() == null) ? "" : gammePrixField.getValue().trim();
        String type = (typeField.getValue() == null) ? "" : typeField.getValue().trim();

        if (nom.isBlank()) { setMessage("❌ Nom obligatoire.", false); markError(nomField); return null; }
        if (ville.isBlank()) { setMessage("❌ Ville obligatoire.", false); markError(villeField); return null; }

        if (!email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            setMessage("❌ Email invalide.", false);
            markError(emailField);
            return null;
        }

        if (!tel.isBlank() && !TN_PHONE_PATTERN.matcher(tel).matches()) {
            setMessage("❌ Téléphone invalide (8 chiffres, 2/3/4/5/7/9).", false);
            markError(telephoneField);
            return null;
        }

        if (!gamme.isBlank() && !(gamme.equals("€") || gamme.equals("€€") || gamme.equals("€€€"))) {
            setMessage("❌ Gamme prix invalide.", false);
            markError(gammePrixField);
            return null;
        }

        Double lat = parseDoubleOrNull(latitudeField.getText());
        Double lon = parseDoubleOrNull(longitudeField.getText());

        if (!safe(latitudeField.getText()).trim().isBlank() && lat == null) {
            setMessage("❌ Latitude invalide.", false);
            markError(latitudeField);
            return null;
        }
        if (!safe(longitudeField.getText()).trim().isBlank() && lon == null) {
            setMessage("❌ Longitude invalide.", false);
            markError(longitudeField);
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
        e.setType(type);
        e.setLatitude(lat);
        e.setLongitude(lon);
        e.setDescription(safe(descriptionArea.getText()).trim());

        return e;
    }

    // ===== small UI helpers =====
    private void markError(Control c) {
        if (!c.getStyleClass().contains("field-error")) c.getStyleClass().add("field-error");
    }

    private void setMessage(String msg, boolean ok) {
        messageLabel.setText(msg == null ? "" : msg);
        messageLabel.setStyle(ok
                ? "-fx-text-fill:#22c55e; -fx-font-weight:bold;"
                : "-fx-text-fill:#ef4444; -fx-font-weight:bold;");
    }

    private Double parseDoubleOrNull(String s) {
        try {
            String v = safe(s).trim().replace(",", ".");
            if (v.isBlank()) return null;
            return Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
    }

    private String blankToNull(String s) {
        String v = safe(s).trim();
        return v.isBlank() ? null : v;
    }

    private String safe(String s) { return s == null ? "" : s; }
}