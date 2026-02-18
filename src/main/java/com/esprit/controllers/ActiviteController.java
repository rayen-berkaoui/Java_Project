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
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ActiviteController {

    // LIST + FILTERS
    @FXML private ListView<Activite> activiteList;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCategorie;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> sortBox;

    private final ActiviteServices service = new ActiviteServices();

    private final ObservableList<Activite> masterData = FXCollections.observableArrayList();
    private FilteredList<Activite> filteredData;
    private SortedList<Activite> sortedData;

    // pour modifier : on stocke l’activité sélectionnée (comme EtablissementController)
    public static Activite activiteToEdit = null;

    @FXML
    public void initialize() {
        // filtres
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

        // CARTES + boutons ronds jaunes à droite (comme ta capture)
        activiteList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Activite a, boolean empty) {
                super.updateItem(a, empty);

                if (empty || a == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label nom = new Label(safe(a.getNomActivite()));
                nom.getStyleClass().add("card-title");

                String dureeTxt = (a.getDuree() == null) ? "" : (a.getDuree() + " min");
                String metaTxt = safe(a.getCategorie())
                        + (safe(a.getNiveau()).isBlank() ? "" : "  •  " + safe(a.getNiveau()))
                        + (dureeTxt.isBlank() ? "" : "  •  " + dureeTxt);

                Label meta = new Label(metaTxt);
                meta.getStyleClass().add("card-meta");

                String desc = safe(a.getDescription());
                if (desc.length() > 120) desc = desc.substring(0, 120) + "...";
                Label description = new Label(desc);
                description.setWrapText(true);
                description.getStyleClass().add("card-desc");

                VBox info = new VBox(6, nom, meta, description);
                HBox.setHgrow(info, Priority.ALWAYS);

                // bouton MODIFIER (rond jaune)
                Button btnEdit = new Button("✎");
                btnEdit.getStyleClass().add("icon-yellow-btn");
                btnEdit.setOnAction(ev -> {
                    activiteToEdit = a;
                    NavigationUtils.goTo("/ajouter_activite.fxml",
                            new ActionEvent(ev.getSource(), ev.getTarget()));
                });

                // bouton SUPPRIMER (rond jaune)
                Button btnDelete = new Button("🗑");
                btnDelete.getStyleClass().add("icon-yellow-btn");
                btnDelete.setOnAction(ev -> {
                    boolean ok = confirm("Supprimer activité",
                            "Voulez-vous supprimer : " + safe(a.getNomActivite()) + " ?");
                    if (!ok) return;

                    try {
                        service.supprimer(a.getIdActivite());
                        loadData();
                        applyFilters();
                    } catch (Exception ex) {
                        showError("Erreur suppression: " + ex.getMessage());
                    }
                });

                VBox actions = new VBox(12, btnEdit, btnDelete);
                actions.setMinWidth(90);
                actions.setMaxWidth(90);
                actions.setStyle("-fx-alignment: center;");

                HBox root = new HBox(15, info, actions);
                root.getStyleClass().add("card-root");

                setGraphic(root);
                setText(null);
            }
        });

        // listeners
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        filterCategorie.valueProperty().addListener((obs, o, n) -> applyFilters());
        filterNiveau.valueProperty().addListener((obs, o, n) -> applyFilters());
        sortBox.valueProperty().addListener((obs, o, n) -> applySort());

        applyFilters();
        applySort();
    }

    // ========== NAVIGATION ==========
    @FXML
    private void onShowAjouter(ActionEvent event) {
        activiteToEdit = null;
        NavigationUtils.goTo("/ajouter_activite.fxml", event);
    }

    // ========== DATA ==========
    @FXML
    private void onRefresh() {
        loadData();
        applyFilters();
        applySort();
    }

    private void loadData() {
        masterData.clear();
        try {
            masterData.addAll(service.afficher());
            refreshCategorieFilterFromDB();
        } catch (SQLException e) {
            showError("Erreur chargement: " + e.getMessage());
        }
    }

    private void refreshCategorieFilterFromDB() {
        Set<String> cats = masterData.stream()
                .map(Activite::getCategorie)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (!cats.isEmpty()) {
            List<String> list = new ArrayList<>(cats);
            Collections.sort(list);

            ObservableList<String> items = FXCollections.observableArrayList();
            items.add("Tous");
            items.addAll(list);

            String current = filterCategorie.getValue();
            filterCategorie.setItems(items);

            if (current != null && items.contains(current)) filterCategorie.setValue(current);
            else filterCategorie.getSelectionModel().selectFirst();
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

    // ========== HELPERS ==========
    private boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String safe(String s) { return s == null ? "" : s; }
}
