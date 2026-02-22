package com.esprit.controllers;

import com.esprit.entities.Etablissement;
import com.esprit.entities.EtablissementImage;
import com.esprit.services.EtablissementImageServices;
import com.esprit.services.EtablissementServices;
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

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class AffichageEtablissementController {

    // ===== UI =====
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterVille;
    @FXML private ComboBox<String> filterGamme;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> sortBox;
    @FXML private TilePane cardsPane;

    // ===== DATA =====
    private final EtablissementServices service = new EtablissementServices();
    private final EtablissementImageServices imageService = new EtablissementImageServices();

    private final ObservableList<Etablissement> masterData = FXCollections.observableArrayList();
    private FilteredList<Etablissement> filteredData;
    private SortedList<Etablissement> sortedData;

    // pour modifier
    public static Etablissement etablissementToEdit = null;

    // cache image principale
    private final Map<Integer, String> mainImageCache = new HashMap<>();

    @FXML
    public void initialize() {

        // filtres init
        filterVille.setItems(FXCollections.observableArrayList("Tous"));
        filterVille.getSelectionModel().selectFirst();

        filterGamme.setItems(FXCollections.observableArrayList("Tous", "€", "€€", "€€€"));
        filterGamme.getSelectionModel().selectFirst();

        filterType.setItems(FXCollections.observableArrayList(
                "Tous", "hotel", "restaurant", "cafe", "museum", "bar", "autre"
        ));
        filterType.getSelectionModel().selectFirst();

        sortBox.setItems(FXCollections.observableArrayList(
                "Aucun", "Nom A→Z", "Ville A→Z", "Gamme", "Type"
        ));
        sortBox.getSelectionModel().selectFirst();

        loadData();

        filteredData = new FilteredList<>(masterData, e -> true);
        sortedData = new SortedList<>(filteredData);

        // listeners
        searchField.textProperty().addListener((obs,o,n) -> { applyFilters(); rebuildGrid(); });
        filterVille.valueProperty().addListener((obs,o,n) -> { applyFilters(); rebuildGrid(); });
        filterGamme.valueProperty().addListener((obs,o,n) -> { applyFilters(); rebuildGrid(); });
        filterType.valueProperty().addListener((obs,o,n) -> { applyFilters(); rebuildGrid(); });
        sortBox.valueProperty().addListener((obs,o,n) -> { applySort(); rebuildGrid(); });

        applyFilters();
        applySort();
        rebuildGrid();
    }

    // ====== NAV ======
    @FXML
    private void onShowAjouter(ActionEvent event) {
        etablissementToEdit = null;
        NavigationUtils.goTo("/ajouter_etablissement.fxml", event);
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

    // ====== DATA ======
    private void loadData() {
        masterData.clear();
        mainImageCache.clear();

        try {
            masterData.addAll(service.afficher());
            refreshVilleFilterFromDB();
            preloadMainImages();
        } catch (SQLException e) {
            showError("Erreur chargement: " + e.getMessage());
        }
    }

    private void preloadMainImages() {
        for (Etablissement e : masterData) {
            try {
                List<EtablissementImage> imgs = imageService.getImagesByEtablissement(e.getIdEtablissement());
                if (imgs != null && !imgs.isEmpty()) {
                    // première image selon ordre_affichage ASC
                    mainImageCache.put(e.getIdEtablissement(), imgs.get(0).getImagePath());
                }
            } catch (SQLException ignored) {}
        }
    }

    private void refreshVilleFilterFromDB() {
        Set<String> villes = masterData.stream()
                .map(Etablissement::getVille)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        List<String> items = new ArrayList<>();
        items.add("Tous");
        if (!villes.isEmpty()) {
            List<String> v = new ArrayList<>(villes);
            Collections.sort(v);
            items.addAll(v);
        }

        String current = filterVille.getValue();
        filterVille.setItems(FXCollections.observableArrayList(items));
        if (current != null && items.contains(current)) filterVille.setValue(current);
        else filterVille.getSelectionModel().selectFirst();
    }

    private void applyFilters() {
        String q = safe(searchField.getText()).toLowerCase().trim();
        String ville = safe(filterVille.getValue());
        String gamme = safe(filterGamme.getValue());
        String type  = safe(filterType.getValue());

        filteredData.setPredicate(e -> {
            if (e == null) return false;

            if (!"Tous".equalsIgnoreCase(ville) && !safe(e.getVille()).equalsIgnoreCase(ville)) return false;
            if (!"Tous".equalsIgnoreCase(gamme) && !safe(e.getGammePrix()).equalsIgnoreCase(gamme)) return false;
            if (!"Tous".equalsIgnoreCase(type)  && !safe(e.getType()).equalsIgnoreCase(type)) return false;

            if (q.isEmpty()) return true;

            return safe(e.getNom()).toLowerCase().contains(q)
                    || safe(e.getVille()).toLowerCase().contains(q)
                    || safe(e.getEmail()).toLowerCase().contains(q)
                    || safe(e.getTelephone()).toLowerCase().contains(q)
                    || safe(e.getDescription()).toLowerCase().contains(q)
                    || safe(e.getType()).toLowerCase().contains(q);
        });
    }

    private void applySort() {
        String sort = safe(sortBox.getValue());
        Comparator<Etablissement> c;

        switch (sort) {
            case "Nom A→Z" -> c = Comparator.comparing(e -> safe(e.getNom()).toLowerCase());
            case "Ville A→Z" -> c = Comparator.comparing(e -> safe(e.getVille()).toLowerCase());
            case "Gamme" -> c = Comparator.comparing(e -> safe(e.getGammePrix()).toLowerCase());
            case "Type" -> c = Comparator.comparing(e -> safe(e.getType()).toLowerCase());
            default -> c = null;
        }

        sortedData.setComparator(c);
    }

    // ====== UI GRID ======
    private void rebuildGrid() {
        cardsPane.getChildren().clear();
        for (Etablissement e : sortedData) {
            cardsPane.getChildren().add(buildCard(e));
        }
    }

    private Pane buildCard(Etablissement e) {

        // ===== image principale depuis etablissement_image =====
        ImageView img = new ImageView();
        img.setFitWidth(330);
        img.setFitHeight(190);
        img.setPreserveRatio(false);
        img.getStyleClass().add("card-image");

        String imgName = mainImageCache.get(e.getIdEtablissement());
        Image fxImg = loadImageFromResources(imgName);
        if (fxImg != null) img.setImage(fxImg);

        StackPane imageWrap = new StackPane(img);
        imageWrap.getStyleClass().add("card-image-wrap");

        // ===== title =====
        Label title = new Label(safe(e.getNom()));
        title.getStyleClass().add("card-title");

        // ===== meta line (ville + gamme + type) =====
        String metaTxt = safe(e.getVille());

        if (!safe(e.getGammePrix()).isBlank()) metaTxt += " • " + safe(e.getGammePrix());
        if (!safe(e.getType()).isBlank()) metaTxt += " • " + safe(e.getType());

        Label meta = new Label(metaTxt);
        meta.getStyleClass().add("card-meta");

        // ===== infos =====
        Label adresse = new Label("📍 " + emptyAsDash(e.getAdresse()));
        Label tel     = new Label("📞 " + emptyAsDash(e.getTelephone()));
        Label email   = new Label("✉ " + emptyAsDash(e.getEmail()));
        Label horaires= new Label("⏰ " + emptyAsDash(e.getHoraires()));

        adresse.getStyleClass().add("card-info");
        tel.getStyleClass().add("card-info");
        email.getStyleClass().add("card-info");
        horaires.getStyleClass().add("card-info");

        VBox infosBox = new VBox(4, adresse, tel, email, horaires);
        infosBox.getStyleClass().add("card-infos");

        // ===== description (limit) =====
        String desc = safe(e.getDescription());
        if (desc.length() > 110) desc = desc.substring(0, 110) + "...";
        Label description = new Label(desc.isBlank() ? "—" : desc);
        description.setWrapText(true);
        description.getStyleClass().add("card-desc");

        VBox content = new VBox(8, title, meta, infosBox, description);
        content.getStyleClass().add("card-content");

        // ===== actions =====
        Button edit = new Button("✎");
        edit.getStyleClass().addAll("icon-btn", "icon-edit");
        edit.setOnAction(ev -> {
            etablissementToEdit = e;
            NavigationUtils.goTo("/ajouter_etablissement.fxml",
                    new ActionEvent(ev.getSource(), ev.getTarget()));
        });

        Button del = new Button("🗑");
        del.getStyleClass().addAll("icon-btn", "icon-delete");
        del.setOnAction(ev -> {
            boolean ok = confirm("Supprimer établissement",
                    "Voulez-vous supprimer : " + safe(e.getNom()) + " ?");
            if (!ok) return;

            try {
                service.supprimer(e.getIdEtablissement());
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

    // Charge /images/<nom> depuis resources
    private Image loadImageFromResources(String imgName) {
        try {
            if (imgName == null || imgName.isBlank()) {
                return tryLoadPlaceholder();
            }
            var is = getClass().getResourceAsStream("/images/" + imgName);
            if (is != null) return new Image(is);
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

    // ====== helpers ======
    private String emptyAsDash(String s) {
        String v = safe(s).trim();
        return v.isEmpty() ? "—" : v;
    }

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