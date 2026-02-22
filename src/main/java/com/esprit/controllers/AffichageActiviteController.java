package com.esprit.controllers;

import com.esprit.entities.Activite;
import com.esprit.entities.ActiviteImage;
import com.esprit.services.ActiviteImageServices;
import com.esprit.services.ActiviteServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class AffichageActiviteController {

    // ===== UI =====
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCategorie;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> sortBox;
    @FXML private TilePane cardsPane;

    // ===== DATA =====
    private final ActiviteServices service = new ActiviteServices();
    private final ActiviteImageServices imageService = new ActiviteImageServices();

    private final ObservableList<Activite> masterData = FXCollections.observableArrayList();
    private FilteredList<Activite> filteredData;
    private SortedList<Activite> sortedData;

    // pour modifier
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

        // listeners
        searchField.textProperty().addListener((obs, o, n) -> { applyFilters(); rebuildGrid(); });
        filterCategorie.valueProperty().addListener((obs, o, n) -> { applyFilters(); rebuildGrid(); });
        filterNiveau.valueProperty().addListener((obs, o, n) -> { applyFilters(); rebuildGrid(); });
        sortBox.valueProperty().addListener((obs, o, n) -> { applySort(); rebuildGrid(); });

        applyFilters();
        applySort();
        rebuildGrid();
    }

    // ===== DATA =====
    private void loadData() {
        masterData.clear();
        try {
            masterData.addAll(service.afficher());
        } catch (SQLException e) {
            showError("Erreur chargement: " + e.getMessage());
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

    // ===== UI GRID =====
    private void rebuildGrid() {
        cardsPane.getChildren().clear();
        for (Activite a : sortedData) {
            cardsPane.getChildren().add(buildCard(a));
        }
    }

    private Pane buildCard(Activite a) {

        // ===== IMAGE COVER (1ère image de activite_image) =====
        ImageView img = new ImageView();
        img.setFitWidth(330);
        img.setFitHeight(170);
        img.setPreserveRatio(false);
        img.getStyleClass().add("card-image");

        Image fxImg = loadCoverImage(a.getIdActivite());
        if (fxImg != null) img.setImage(fxImg);

        StackPane imageWrap = new StackPane(img);
        imageWrap.getStyleClass().add("card-image-wrap");

        // ===== TITRE =====
        Label title = new Label(safe(a.getNomActivite()));
        title.getStyleClass().add("card-title");

        // ===== META =====
        String metaTxt = safe(a.getCategorie())
                + (safe(a.getNiveau()).isBlank() ? "" : " • " + safe(a.getNiveau()));
        Label meta = new Label(metaTxt);
        meta.getStyleClass().add("card-meta");

        // ===== INFOS =====
        String dureeTxt = (a.getDuree() == null) ? "—" : (a.getDuree() + " min");
        Label duree = new Label("⏱ " + dureeTxt);
        duree.getStyleClass().add("card-info");

        String desc = safe(a.getDescription());
        if (desc.length() > 90) desc = desc.substring(0, 90) + "...";
        Label description = new Label(desc.isBlank() ? "—" : desc);
        description.setWrapText(true);
        description.getStyleClass().add("card-desc");

        VBox content = new VBox(8, title, meta, duree, description);
        content.getStyleClass().add("card-content");

        // ===== ACTIONS =====
        Button edit = new Button("✎");
        edit.getStyleClass().addAll("icon-btn", "icon-edit");
        edit.setOnAction(ev -> {
            activiteToEdit = a;
            NavigationUtils.goTo("/ajouter_activite.fxml",
                    new ActionEvent(ev.getSource(), ev.getTarget()));
        });

        Button del = new Button("🗑");
        del.getStyleClass().addAll("icon-btn", "icon-delete");
        del.setOnAction(ev -> {
            boolean ok = confirm("Supprimer activité",
                    "Voulez-vous supprimer : " + safe(a.getNomActivite()) + " ?");
            if (!ok) return;

            try {
                // optionnel: supprimer images d'abord
                imageService.supprimerImagesByActivite(a.getIdActivite());

                service.supprimer(a.getIdActivite());
                loadData();
                applyFilters();
                applySort();
                rebuildGrid();
            } catch (Exception ex) {
                showError("Erreur suppression: " + ex.getMessage());
            }
        });

        HBox actions = new HBox(10, edit, del);
        actions.getStyleClass().add("card-actions");

        VBox card = new VBox(imageWrap, content, actions);
        card.getStyleClass().add("premium-card");
        card.setCursor(Cursor.HAND);

        return card;
    }

    // ===== COVER IMAGE =====
    private Image loadCoverImage(int idActivite) {
        try {
            List<ActiviteImage> imgs = imageService.getImagesByActivite(idActivite);
            if (imgs == null || imgs.isEmpty()) return tryLoadPlaceholder();

            String fileName = imgs.get(0).getImagePath(); // 1ère image par ordre
            return loadImageSafe("/images/" + fileName);

        } catch (Exception e) {
            return tryLoadPlaceholder();
        }
    }

    private Image loadImageSafe(String resourcePathOrFilePath) {
        try {
            if (resourcePathOrFilePath == null || resourcePathOrFilePath.isBlank()) {
                return tryLoadPlaceholder();
            }

            // ressources
            if (resourcePathOrFilePath.startsWith("/")) {
                var is = getClass().getResourceAsStream(resourcePathOrFilePath);
                if (is != null) return new Image(is);
            }

            // fichier local
            File f = new File(resourcePathOrFilePath);
            if (f.exists()) return new Image(f.toURI().toString(), true);

            return tryLoadPlaceholder();
        } catch (Exception ignored) {
            return tryLoadPlaceholder();
        }
    }

    private Image tryLoadPlaceholder() {
        try {
            var is = getClass().getResourceAsStream("/images/placeholder.png");
            if (is != null) return new Image(is);
        } catch (Exception ignored) {}
        return null;
    }

    // ===== NAV =====
    @FXML
    private void onShowAjouter(ActionEvent event) {
        activiteToEdit = null;
        NavigationUtils.goTo("/ajouter_activite.fxml", event);
    }

    @FXML
    private void onRefresh(ActionEvent event) {
        loadData();
        applyFilters();
        applySort();
        rebuildGrid();
    }

    @FXML
    private void goHome(ActionEvent event) {
        NavigationUtils.goTo("/home.fxml", event);
    }

    // ===== helpers =====
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