package com.esprit.controllers;

import com.esprit.entities.Activite;
import com.esprit.services.ActiviteImageServices;
import com.esprit.services.ActiviteServices;
import com.esprit.utils.ThemeManager;
import com.esprit.services.PdfExportService;
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
import java.util.*;
import java.util.stream.Collectors;

public class ActiviteController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCategorie;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> sortBox;
    @FXML private TilePane cardsPane;

    private final ActiviteServices service = new ActiviteServices();
    private final ActiviteImageServices imageService = new ActiviteImageServices();

    private final ObservableList<Activite> masterData = FXCollections.observableArrayList();
    private FilteredList<Activite> filteredData;
    private SortedList<Activite> sortedData;

    // ✅ cover images cache : idActivite -> image_path
    private Map<Integer, String> coverMap = new HashMap<>();

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

        searchField.textProperty().addListener((obs, o, n) -> { applyFilters(); rebuildGrid(); });
        filterCategorie.valueProperty().addListener((obs, o, n) -> { applyFilters(); rebuildGrid(); });
        filterNiveau.valueProperty().addListener((obs, o, n) -> { applyFilters(); rebuildGrid(); });
        sortBox.valueProperty().addListener((obs, o, n) -> { applySort(); rebuildGrid(); });

        applyFilters();
        applySort();
        rebuildGrid();
    }

    @FXML
    private void onShowAjouter(ActionEvent event) {
        activiteToEdit = null;
        NavigationUtils.goTo("/ajouter_activite.fxml", event);
    }

    @FXML
    private void onRefresh() {
        loadData();
        applyFilters();
        applySort();
        rebuildGrid();
    }

    private void loadData() {
        masterData.clear();
        try {
            masterData.addAll(service.afficher());
            refreshCategorieFilterFromDB();

            // ✅ charge covers en 1 fois (mieux que N requêtes)
            List<Integer> ids = masterData.stream().map(Activite::getIdActivite).collect(Collectors.toList());
            coverMap = imageService.getCoverMap(ids);

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

    private void rebuildGrid() {
        cardsPane.getChildren().clear();
        for (Activite a : sortedData) {
            cardsPane.getChildren().add(buildCard(a));
        }
    }

    private Pane buildCard(Activite a) {
        ImageView img = new ImageView();
        img.setFitWidth(360);
        img.setFitHeight(190);
        img.setPreserveRatio(false);
        img.getStyleClass().add("card-image");

        // ✅ image cover depuis DB activite_image
        String imagePath = coverMap.get(a.getIdActivite());
        img.setImage(loadImageFromDBPath(imagePath));

        StackPane imageWrap = new StackPane(img);
        imageWrap.getStyleClass().add("card-image-wrap");

        Label title = new Label(safe(a.getNomActivite()));
        title.getStyleClass().add("card-title");

        String dureeTxt = (a.getDuree() == null) ? "" : (a.getDuree() + " min");
        Label meta = new Label(
                safe(a.getCategorie())
                        + (safe(a.getNiveau()).isBlank() ? "" : " • " + safe(a.getNiveau()))
                        + (dureeTxt.isBlank() ? "" : " • " + dureeTxt)
        );
        meta.getStyleClass().add("card-meta");

        String desc = safe(a.getDescription());
        if (desc.length() > 110) desc = desc.substring(0, 110) + "...";
        Label description = new Label(desc);
        description.setWrapText(true);
        description.getStyleClass().add("card-desc");

        VBox content = new VBox(8, title, meta, description);
        content.getStyleClass().add("card-content");

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
                service.supprimer(a.getIdActivite());
                onRefresh();
            } catch (Exception ex) {
                showError("Erreur suppression: " + ex.getMessage());
            }
        });

        Button pdf = new Button("\uD83D\uDCC4");
        pdf.getStyleClass().addAll("icon-btn", "icon-pdf");
        pdf.setOnAction(ev -> exportActivitePdf(a));

        HBox actions = new HBox(10, edit, del, pdf);
        actions.getStyleClass().add("card-actions");

        VBox card = new VBox(imageWrap, content, actions);
        card.getStyleClass().add("premium-card");
        card.setCursor(Cursor.HAND);

        return card;
    }

    /**
     * image_path dans DB = ex: "act_1700_xxx.jpg"
     * => on charge depuis resources/images/ si tu copies là-bas (comme établissement)
     * sinon fallback placeholder.
     */
    private Image loadImageFromDBPath(String imagePath) {
        try {
            if (imagePath == null || imagePath.isBlank()) return loadPlaceholderSafe();

            // 1) si c’est une URL
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                return new Image(imagePath, true);
            }

            // 2) si c’est un chemin fichier absolu (optionnel)
            File f = new File(imagePath);
            if (f.exists()) return new Image(f.toURI().toString(), true);

            // 3) sinon on suppose resources/images/<imagePath>
            var is = getClass().getResourceAsStream("/images/" + imagePath);
            if (is != null) return new Image(is);

        } catch (Exception ignored) { }

        return loadPlaceholderSafe();
    }

    private Image loadPlaceholderSafe() {
        try {
            var is = getClass().getResourceAsStream("/telecharger (6).png");
            if (is != null) return new Image(is);
        } catch (Exception ignored) { }

        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9NwAAAABJRU5ErkJggg==");
    }

    // ===== PDF Export =====
    private void exportActivitePdf(Activite a) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter en PDF");
        fc.setInitialFileName(safe(a.getNomActivite()).replaceAll("[^a-zA-Z0-9\\-_ ]", "") + "_fiche.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));

        javafx.stage.Window window = cardsPane.getScene().getWindow();
        java.io.File file = fc.showSaveDialog(window);
        if (file == null) return;

        try {
            PdfExportService.exportActivite(a, file);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export r\u00e9ussi");
            ok.setHeaderText(null);
            ok.setContentText("PDF g\u00e9n\u00e9r\u00e9 avec succ\u00e8s !\n" + file.getAbsolutePath());
            ok.showAndWait();
            if (java.awt.Desktop.isDesktopSupported()) {
                new Thread(() -> { try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {} }).start();
            }
        } catch (Exception ex) {
            showError("Erreur lors de la g\u00e9n\u00e9ration du PDF : " + ex.getMessage());
            ex.printStackTrace();
        }
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

    @FXML
    private void goHome(ActionEvent event) {
        NavigationUtils.goTo("/home.fxml", event);
    }

    @FXML
    private void onExportAllPdf(ActionEvent event) {
        if (sortedData == null || sortedData.isEmpty()) {
            showError("Aucune activit\u00e9 \u00e0 exporter.");
            return;
        }

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter toutes les activit\u00e9s en PDF");
        fc.setInitialFileName("catalogue_activites.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));

        javafx.stage.Window window = cardsPane.getScene().getWindow();
        java.io.File file = fc.showSaveDialog(window);
        if (file == null) return;

        try {
            PdfExportService.exportAllActivites(new java.util.ArrayList<>(sortedData), file);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export r\u00e9ussi");
            ok.setHeaderText(null);
            ok.setContentText("Catalogue PDF g\u00e9n\u00e9r\u00e9 avec succ\u00e8s !\n" + sortedData.size() + " activit\u00e9(s) export\u00e9es.\n" + file.getAbsolutePath());
            ok.showAndWait();
            if (java.awt.Desktop.isDesktopSupported()) {
                new Thread(() -> { try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {} }).start();
            }
        } catch (Exception ex) {
            showError("Erreur lors de la g\u00e9n\u00e9ration du PDF : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String safe(String s) { return s == null ? "" : s; }

    @FXML
    private void toggleTheme(ActionEvent event) {
        ThemeManager.handleToggleTheme(event);
    }
}