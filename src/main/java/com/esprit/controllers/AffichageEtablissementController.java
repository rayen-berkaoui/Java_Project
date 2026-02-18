package com.esprit.controllers;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import com.esprit.entities.Etablissement;
import com.esprit.services.EtablissementServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.event.ActionEvent;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class AffichageEtablissementController {
    @FXML private ListView<Etablissement> etablissementList;
    private final ObservableList<Etablissement> masterData = FXCollections.observableArrayList();
    private final EtablissementServices service = new EtablissementServices();
    @FXML private javafx.scene.control.ComboBox<String> filterVille;
    @FXML private javafx.scene.control.ComboBox<String> filterGamme;
    @FXML private javafx.scene.control.TextField searchField;

    private FilteredList<Etablissement> filteredData;
    private SortedList<Etablissement> sortedData;

    @FXML
    public void initialize() {
        loadData();
        filteredData = new FilteredList<>(masterData, e -> true);
        sortedData = new SortedList<>(filteredData);
        etablissementList.setItems(sortedData);

        // ComboBox setup
        filterGamme.setItems(FXCollections.observableArrayList("Tous", "€", "€€", "€€€"));
        filterGamme.getSelectionModel().selectFirst();
        filterVille.setItems(FXCollections.observableArrayList("Tous"));
        filterVille.getSelectionModel().selectFirst();

        // Listeners
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        filterVille.valueProperty().addListener((obs, o, n) -> applyFilters());
        filterGamme.valueProperty().addListener((obs, o, n) -> applyFilters());

        etablissementList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Etablissement e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                // Image
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
                iv.setFitWidth(90);
                iv.setFitHeight(70);
                iv.setPreserveRatio(true);
                String img = e.getImageUrl();
                if (img != null && !img.isBlank()) {
                    try {
                        iv.setImage(new javafx.scene.image.Image(img, true));
                    } catch (Exception ex) {
                        iv.setImage(null);
                    }
                }
                // Infos
                javafx.scene.control.Label nom = new javafx.scene.control.Label(e.getNom());
                nom.setStyle("-fx-font-size:16; -fx-text-fill:#FFD700; -fx-font-weight:bold;");
                javafx.scene.control.Label meta = new javafx.scene.control.Label(e.getVille() + "   •   " + e.getGammePrix());
                meta.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12;");
                String desc = e.getDescription();
                if (desc != null && desc.length() > 100) desc = desc.substring(0, 100) + "...";
                javafx.scene.control.Label description = new javafx.scene.control.Label(desc);
                description.setWrapText(true);
                description.setStyle("-fx-text-fill:#cccccc; -fx-font-size:12;");
                javafx.scene.control.Label contact = new javafx.scene.control.Label(
                        (e.getTelephone() != null && !e.getTelephone().isBlank() ? "📞 " + e.getTelephone() + "   " : "") +
                        (e.getEmail() != null && !e.getEmail().isBlank() ? "✉ " + e.getEmail() : "")
                );
                contact.setStyle("-fx-text-fill:#aaaaaa; -fx-font-size:12;");
                javafx.scene.layout.VBox infoBox = new javafx.scene.layout.VBox(4, nom, meta, contact, description);
                javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                // Buttons
                // Icon buttons
                javafx.scene.control.Button btnMod;
                javafx.scene.image.Image editImg = null;
                try {
                    editImg = new javafx.scene.image.Image(getClass().getResourceAsStream("/icons/edit.png"));
                } catch (Exception ex) {}
                if (editImg != null && !editImg.isError()) {
                    javafx.scene.image.ImageView editIcon = new javafx.scene.image.ImageView(editImg);
                    editIcon.setFitWidth(24);
                    editIcon.setFitHeight(24);
                    btnMod = new javafx.scene.control.Button();
                    btnMod.setGraphic(editIcon);
                } else {
                    btnMod = new javafx.scene.control.Button("✏️");
                }
                btnMod.getStyleClass().add("btn-warning");
                btnMod.setStyle("-fx-background-radius: 50; -fx-padding: 8; -fx-background-color: #FFBF00;");
                btnMod.setOnAction(ev -> {
                    etablissementToEdit = e;
                    NavigationUtils.goTo("/ajouter_etablissement.fxml", new javafx.event.ActionEvent(ev.getSource(), ev.getTarget()));
                });

                javafx.scene.control.Button btnSup;
                javafx.scene.image.Image deleteImg = null;
                try {
                    deleteImg = new javafx.scene.image.Image(getClass().getResourceAsStream("/icons/delete.png"));
                } catch (Exception ex) {}
                if (deleteImg != null && !deleteImg.isError()) {
                    javafx.scene.image.ImageView deleteIcon = new javafx.scene.image.ImageView(deleteImg);
                    deleteIcon.setFitWidth(24);
                    deleteIcon.setFitHeight(24);
                    btnSup = new javafx.scene.control.Button();
                    btnSup.setGraphic(deleteIcon);
                } else {
                    btnSup = new javafx.scene.control.Button("🗑️");
                }
                btnSup.getStyleClass().add("btn-danger");
                btnSup.setStyle("-fx-background-radius: 50; -fx-padding: 8; -fx-background-color: #FFBF00;");
                btnSup.setOnAction(ev -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet établissement ?", ButtonType.YES, ButtonType.NO);
                    alert.setHeaderText(null);
                    alert.showAndWait().ifPresent(type -> {
                        if (type == ButtonType.YES) {
                            try {
                                service.supprimer(e.getIdEtablissement());
                                loadData();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                });
                javafx.scene.layout.VBox btnBox = new javafx.scene.layout.VBox(8, btnMod, btnSup);
                btnBox.setStyle("-fx-alignment: center-right;");

                javafx.scene.layout.HBox root = new javafx.scene.layout.HBox(15, iv, infoBox, btnBox);
                root.setStyle("-fx-background-color: #000000; -fx-background-radius:15; -fx-padding:15;");
                javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);
                setGraphic(root);
                setText(null);
            }
        });
    }

    // --- Filtrage et recherche ---
    private void refreshVilleFilter() {
        java.util.Set<String> villes = masterData.stream()
                .map(Etablissement::getVille)
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        String current = filterVille.getValue();
        javafx.collections.ObservableList<String> items = FXCollections.observableArrayList();
        items.add("Tous");
        items.addAll(villes.stream().sorted().toList());
        filterVille.setItems(items);
        if (current != null && items.contains(current)) filterVille.setValue(current);
        else filterVille.getSelectionModel().selectFirst();
    }

    private void applyFilters() {
        String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String ville = filterVille.getValue() == null ? "Tous" : filterVille.getValue();
        String gamme = filterGamme.getValue() == null ? "Tous" : filterGamme.getValue();
        filteredData.setPredicate(e -> {
            if (e == null) return false;
            if (!"Tous".equalsIgnoreCase(ville) && !e.getVille().equalsIgnoreCase(ville)) return false;
            if (!"Tous".equalsIgnoreCase(gamme) && !e.getGammePrix().equalsIgnoreCase(gamme)) return false;
            if (q.isEmpty()) return true;
            return (e.getNom() != null && e.getNom().toLowerCase().contains(q))
                    || (e.getVille() != null && e.getVille().toLowerCase().contains(q))
                    || (e.getEmail() != null && e.getEmail().toLowerCase().contains(q))
                    || (e.getTelephone() != null && e.getTelephone().toLowerCase().contains(q));
        });
    }




    private void loadData() {
        masterData.clear();
        try {
            masterData.addAll(service.afficher());
            refreshVilleFilter();
        } catch (Exception e) {
            // Optionally show error
        }
    }


    // Static variable to hold the Etablissement to edit
    public static Etablissement etablissementToEdit = null;

    @FXML
    private void onShowAjouter(ActionEvent event) {
        etablissementToEdit = null;
        NavigationUtils.goTo("/ajouter_etablissement.fxml", event);
    }

    @FXML
    private void onShowModifier(ActionEvent event) {
        Etablissement selected = etablissementList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        etablissementToEdit = selected;
        NavigationUtils.goTo("/ajouter_etablissement.fxml", event);
    }

    @FXML
    private void onSupprimer(ActionEvent event) {
        Etablissement selected = etablissementList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet établissement ?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                try {
                    service.supprimer(selected.getIdEtablissement());
                    loadData();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
