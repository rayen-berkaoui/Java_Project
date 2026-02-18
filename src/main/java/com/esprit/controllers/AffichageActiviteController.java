package com.esprit.controllers;

import com.esprit.entities.Activite;
import com.esprit.services.ActiviteServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.Comparator;

public class AffichageActiviteController {

    @FXML private ListView<Activite> activiteList;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCategorie;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> sortBox;

    private final ActiviteServices service = new ActiviteServices();

    private final ObservableList<Activite> masterData = FXCollections.observableArrayList();
    private FilteredList<Activite> filteredData;
    private SortedList<Activite> sortedData;

    // on stocke l’activité à modifier
    public static Activite activiteToEdit = null;

    @FXML
    public void initialize() {
        filterCategorie.setItems(FXCollections.observableArrayList("Tous", "Sport", "Culture", "Loisir", "Nature", "Autre"));
        filterCategorie.getSelectionModel().selectFirst();

        filterNiveau.setItems(FXCollections.observableArrayList("Tous", "Débutant", "Intermédiaire", "Avancé"));
        filterNiveau.getSelectionModel().selectFirst();

        sortBox.setItems(FXCollections.observableArrayList("Aucun", "Nom A→Z", "Catégorie A→Z", "Durée ↑", "Durée ↓"));
        sortBox.getSelectionModel().selectFirst();

        loadData();

        filteredData = new FilteredList<>(masterData, a -> true);
        sortedData = new SortedList<>(filteredData);
        activiteList.setItems(sortedData);

        activiteList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Activite a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setText(null); setGraphic(null); return; }
                setText(a.getNomActivite() + " (" + safe(a.getCategorie()) + ")");
            }
        });

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        filterCategorie.valueProperty().addListener((obs, o, n) -> applyFilters());
        filterNiveau.valueProperty().addListener((obs, o, n) -> applyFilters());
        sortBox.valueProperty().addListener((obs, o, n) -> applySort());

        applyFilters();
        applySort();
    }

    private void loadData() {
        masterData.clear();
        try {
            masterData.addAll(service.afficher());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        String q = safe(searchField.getText()).toLowerCase().trim();
        String cat = safe(filterCategorie.getValue());
        String niv = safe(filterNiveau.getValue());

        filteredData.setPredicate(a -> {
            if (a == null) return false;

            if (!"Tous".equalsIgnoreCase(cat) && !safe(a.getCategorie()).equalsIgnoreCase(cat)) return false;
            if (!"Tous".equalsIgnoreCase(niv) && !safe(a.getNiveau()).equalsIgnoreCase(niv)) return false;

            if (q.isEmpty()) return true;

            return safe(a.getNomActivite()).toLowerCase().contains(q)
                    || safe(a.getCategorie()).toLowerCase().contains(q)
                    || safe(a.getNiveau()).toLowerCase().contains(q)
                    || safe(a.getDescription()).toLowerCase().contains(q);
        });
    }

    private void applySort() {
        String sort = safe(sortBox.getValue());
        Comparator<Activite> c;

        switch (sort) {
            case "Nom A→Z":
                c = Comparator.comparing(a -> safe(a.getNomActivite()).toLowerCase());
                break;
            case "Catégorie A→Z":
                c = Comparator.comparing(a -> safe(a.getCategorie()).toLowerCase());
                break;
            case "Durée ↑":
                c = Comparator.comparingInt(a -> a.getDuree() == null ? Integer.MAX_VALUE : a.getDuree());
                break;
            case "Durée ↓":
                c = Comparator.<Activite>comparingInt(a -> a.getDuree() == null ? Integer.MIN_VALUE : a.getDuree()).reversed();
                break;
            default:
                c = null;
        }
        sortedData.setComparator(c);
    }

    @FXML
    private void onShowAjouter(ActionEvent event) {
        activiteToEdit = null;
        NavigationUtils.goTo("/ajouter_activite.fxml", event);
    }

    @FXML
    private void onShowModifier(ActionEvent event) {
        Activite selected = activiteList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        activiteToEdit = selected;
        NavigationUtils.goTo("/ajouter_activite.fxml", event);
    }

    @FXML
    private void onSupprimer(ActionEvent event) {
        Activite selected = activiteList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette activité ?", ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            service.supprimer(selected.getIdActivite());
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRefresh(ActionEvent event) {
        loadData();
        applyFilters();
        applySort();
    }

    private String safe(String s) { return s == null ? "" : s; }
}
