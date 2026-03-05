package com.esprit.controllers;

import com.esprit.entities.Activite;
import com.esprit.services.ActiviteServices;
import com.esprit.services.PdfExportService;
import com.esprit.services.ExcelExportService;
import com.esprit.utils.ThemeManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Premium Table view of Activités.
 * Displays activities in a data-table layout with avatar circles,
 * status badges, action buttons, and dynamic PieChart / BarChart statistics by category.
 */
public class TableauActiviteController {

    // ===== ROOT =====
    @FXML private StackPane rootStack;

    // ===== UI =====
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCategorie;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> filterStatut;
    @FXML private VBox tableBody;

    // ===== CHARTS =====
    @FXML private PieChart pieChart;
    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis barXAxis;
    @FXML private NumberAxis barYAxis;
    @FXML private StackPane chartsContainer;
    @FXML private HBox statsCardsRow;
    @FXML private Button btnToggleChart;
    @FXML private Button btnToggleStats;
    @FXML private Label countLabel;
    @FXML private ComboBox<String> chartFieldSelector;

    private boolean showingPie = true;
    private boolean statsVisible = true;

    // ===== DATA =====
    private final ActiviteServices service = new ActiviteServices();
    private final ObservableList<Activite> masterData = FXCollections.observableArrayList();
    private FilteredList<Activite> filteredData;

    // Category → display config (emoji, color)
    private static final Map<String, String[]> CATEGORY_CONFIG = new LinkedHashMap<>();
    static {
        CATEGORY_CONFIG.put("sport",        new String[]{"\u26BD", "#667eea"});
        CATEGORY_CONFIG.put("culture",      new String[]{"\uD83C\uDFAD", "#a18cd1"});
        CATEGORY_CONFIG.put("gastronomie",  new String[]{"\uD83C\uDF7D", "#f5576c"});
        CATEGORY_CONFIG.put("nature",       new String[]{"\uD83C\uDF3F", "#43e97b"});
        CATEGORY_CONFIG.put("loisir",       new String[]{"\uD83C\uDFAE", "#FFB300"});
        CATEGORY_CONFIG.put("aventure",     new String[]{"\uD83C\uDFD4", "#fa709a"});
        CATEGORY_CONFIG.put("bien-etre",    new String[]{"\uD83E\uDDD8", "#4facfe"});
        CATEGORY_CONFIG.put("autre",        new String[]{"\uD83D\uDCCC", "#30cfd0"});
    }

    // Avatar background colors (modern palette)
    private static final String[] AVATAR_COLORS = {
        "#667eea", "#f5576c", "#4facfe", "#43e97b",
        "#fa709a", "#a18cd1", "#30cfd0", "#FFB300"
    };

    @FXML
    public void initialize() {
        loadData();

        filteredData = new FilteredList<>(masterData, a -> true);

        // Field selector for statistics
        chartFieldSelector.setItems(FXCollections.observableArrayList(
                "Catégorie", "Niveau", "Statut", "Durée", "Prix", "Devise", "Places", "Données remplies"));
        chartFieldSelector.setValue("Catégorie");
        chartFieldSelector.valueProperty().addListener((obs, o, n) -> updateCharts());

        searchField.textProperty().addListener((obs, o, n) -> { applyFilters(); rebuildTable(); updateCharts(); });
        filterCategorie.valueProperty().addListener((obs, o, n) -> { applyFilters(); rebuildTable(); updateCharts(); });
        filterNiveau.valueProperty().addListener((obs, o, n) -> { applyFilters(); rebuildTable(); updateCharts(); });
        filterStatut.valueProperty().addListener((obs, o, n) -> { applyFilters(); rebuildTable(); updateCharts(); });

        applyFilters();
        rebuildTable();
        updateCharts();

        Platform.runLater(() -> ChatbotPanel.install(rootStack));
    }

    // ====== DATA ======
    private void loadData() {
        masterData.clear();
        try {
            masterData.addAll(service.afficher());
            refreshFilters();
        } catch (SQLException e) {
            showError("Erreur chargement: " + e.getMessage());
        }
    }

    private void refreshFilters() {
        // Catégorie filter
        Set<String> categories = new TreeSet<>();
        masterData.forEach(a -> {
            if (a.getCategorie() != null && !a.getCategorie().isBlank())
                categories.add(a.getCategorie().trim());
        });

        List<String> catItems = new ArrayList<>();
        catItems.add("Tous");
        catItems.addAll(categories);

        String currentCat = filterCategorie.getValue();
        filterCategorie.setItems(FXCollections.observableArrayList(catItems));
        if (currentCat != null && catItems.contains(currentCat))
            filterCategorie.setValue(currentCat);
        else
            filterCategorie.getSelectionModel().selectFirst();

        // Niveau filter
        filterNiveau.setItems(FXCollections.observableArrayList(
                "Tous", "Débutant", "Intermédiaire", "Avancé"));
        if (filterNiveau.getValue() == null)
            filterNiveau.getSelectionModel().selectFirst();

        // Statut filter
        filterStatut.setItems(FXCollections.observableArrayList(
                "Tous", "disponible", "complete", "annulee"));
        if (filterStatut.getValue() == null)
            filterStatut.getSelectionModel().selectFirst();
    }

    private void applyFilters() {
        String q = safe(searchField.getText()).toLowerCase().trim();
        String cat = safe(filterCategorie.getValue());
        String niv = safe(filterNiveau.getValue());
        String statut = safe(filterStatut.getValue());

        filteredData.setPredicate(a -> {
            if (a == null) return false;
            if (!"Tous".equalsIgnoreCase(cat) && !safe(a.getCategorie()).equalsIgnoreCase(cat)) return false;
            if (!"Tous".equalsIgnoreCase(niv) && !safe(a.getNiveau()).equalsIgnoreCase(niv)) return false;
            if (!"Tous".equalsIgnoreCase(statut) && !safe(a.getStatut()).equalsIgnoreCase(statut)) return false;
            if (q.isEmpty()) return true;
            return safe(a.getNomActivite()).toLowerCase().contains(q)
                    || safe(a.getCategorie()).toLowerCase().contains(q)
                    || safe(a.getNiveau()).toLowerCase().contains(q)
                    || safe(a.getDescription()).toLowerCase().contains(q);
        });
    }

    // ====== TABLE BUILDING ======
    private void rebuildTable() {
        tableBody.getChildren().clear();

        for (Activite a : filteredData) {
            tableBody.getChildren().add(buildRow(a));
        }

        if (filteredData.isEmpty()) {
            Label empty = new Label("Aucune activité trouvée");
            empty.getStyleClass().add("muted");
            empty.setStyle("-fx-padding: 40 0;");
            HBox emptyRow = new HBox(empty);
            emptyRow.setAlignment(Pos.CENTER);
            tableBody.getChildren().add(emptyRow);
        }
    }

    private HBox buildRow(Activite a) {

        // ── Avatar circle with initials ──
        String initials = getInitials(a.getNomActivite());
        Label avatarText = new Label(initials);
        avatarText.getStyleClass().add("avatar-text");

        StackPane avatar = new StackPane(avatarText);
        avatar.getStyleClass().add("avatar-circle");
        int colorIdx = Math.abs(a.getIdActivite()) % AVATAR_COLORS.length;
        avatar.setStyle("-fx-background-color: " + AVATAR_COLORS[colorIdx] + ";");

        // Name
        Label name = new Label(safe(a.getNomActivite()));
        name.getStyleClass().add("table-cell-bold");

        HBox nameCell = new HBox(12, avatar, name);
        nameCell.setAlignment(Pos.CENTER_LEFT);
        nameCell.setPrefWidth(220);
        nameCell.setMinWidth(220);

        // ── Catégorie badge ──
        Label catBadge = new Label(safe(a.getCategorie()).toUpperCase());
        catBadge.getStyleClass().addAll("status-badge", getCategoryBadgeClass(a.getCategorie()));
        catBadge.setPrefWidth(120);
        catBadge.setMinWidth(120);

        // ── Niveau ──
        Label niveau = new Label(safe(a.getNiveau()));
        niveau.getStyleClass().add("table-cell-text");
        niveau.setPrefWidth(120);
        niveau.setMinWidth(120);

        // ── Durée ──
        String dureeTxt = (a.getDuree() == null) ? "—" : (a.getDuree() + " min");
        Label duree = new Label(dureeTxt);
        duree.getStyleClass().add("table-cell-text");
        duree.setPrefWidth(80);
        duree.setMinWidth(80);

        // ── Prix ──
        String prixTxt = "—";
        if (a.getPrix() != null && a.getPrix().compareTo(BigDecimal.ZERO) > 0) {
            prixTxt = a.getPrix().toPlainString() + " " + safe(a.getDevise());
        }
        Label prix = new Label(prixTxt);
        prix.getStyleClass().add("table-cell-bold");
        prix.setPrefWidth(100);
        prix.setMinWidth(100);

        // ── Statut badge ──
        Label statutBadge = new Label(safe(a.getStatut()).toUpperCase());
        statutBadge.getStyleClass().addAll("status-badge", getStatutBadgeClass(a.getStatut()));
        statutBadge.setPrefWidth(110);
        statutBadge.setMinWidth(110);

        // ── Places ──
        String placesTxt = "—";
        if (a.getPlacesDispo() != null && a.getNbPlaces() != null) {
            placesTxt = a.getPlacesDispo() + "/" + a.getNbPlaces();
        } else if (a.getNbPlaces() != null) {
            placesTxt = String.valueOf(a.getNbPlaces());
        }
        Label places = new Label(placesTxt);
        places.getStyleClass().add("table-cell-text");
        places.setPrefWidth(80);
        places.setMinWidth(80);

        // ── Action buttons ──
        Button editBtn = new Button("\u270E");
        editBtn.getStyleClass().addAll("table-action-btn", "table-action-edit");
        editBtn.setCursor(Cursor.HAND);
        editBtn.setOnAction(ev -> {
            AffichageActiviteController.activiteToEdit = a;
            NavigationUtils.goTo("/ajouter_activite.fxml",
                    new ActionEvent(ev.getSource(), ev.getTarget()));
        });

        Button deleteBtn = new Button("\uD83D\uDDD1");
        deleteBtn.getStyleClass().addAll("table-action-btn", "table-action-delete");
        deleteBtn.setCursor(Cursor.HAND);
        deleteBtn.setOnAction(ev -> {
            boolean ok = confirm("Supprimer",
                    "Voulez-vous supprimer : " + safe(a.getNomActivite()) + " ?");
            if (!ok) return;
            try {
                service.supprimer(a.getIdActivite());
                loadData();
                applyFilters();
                rebuildTable();
                SuccessNotification.show(tableBody, "Activité supprimée !");
            } catch (Exception ex) {
                showError("Erreur: " + ex.getMessage());
            }
        });

        Button pdfBtn = new Button("\uD83D\uDCC4");
        pdfBtn.getStyleClass().addAll("table-action-btn", "table-action-pdf");
        pdfBtn.setCursor(Cursor.HAND);
        pdfBtn.setOnAction(ev -> exportSinglePdf(a));

        HBox actionsBox = new HBox(8, editBtn, deleteBtn, pdfBtn);
        actionsBox.setAlignment(Pos.CENTER);
        actionsBox.setPrefWidth(140);
        actionsBox.setMinWidth(140);

        // ── Row ──
        HBox row = new HBox(nameCell, catBadge, niveau, duree, prix, statutBadge, places, actionsBox);
        row.getStyleClass().add("table-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(Cursor.HAND);

        return row;
    }

    // ====== CHARTS & STATISTICS ======
    private void updateCharts() {
        String field = chartFieldSelector.getValue();
        if (field == null) field = "Catégorie";

        int total = filteredData.size();
        countLabel.setText(total + " activité" + (total > 1 ? "s" : ""));

        Map<String, Long> counts;
        String chartTitle;

        switch (field) {
            case "Niveau" -> {
                counts = filteredData.stream().collect(Collectors.groupingBy(
                        a -> safe(a.getNiveau()).isBlank() ? "(vide)" : safe(a.getNiveau()).trim(),
                        Collectors.counting()));
                chartTitle = "Niveau";
            }
            case "Statut" -> {
                counts = filteredData.stream().collect(Collectors.groupingBy(
                        a -> safe(a.getStatut()).isBlank() ? "(vide)" : safe(a.getStatut()).trim(),
                        Collectors.counting()));
                chartTitle = "Statut";
            }
            case "Durée" -> {
                counts = filteredData.stream().collect(Collectors.groupingBy(
                        a -> {
                            if (a.getDuree() == null) return "Non défini";
                            int d = a.getDuree();
                            if (d <= 30) return "0-30 min";
                            if (d <= 60) return "31-60 min";
                            if (d <= 120) return "1h-2h";
                            return "> 2h";
                        },
                        Collectors.counting()));
                chartTitle = "Durée";
            }
            case "Prix" -> {
                counts = filteredData.stream().collect(Collectors.groupingBy(
                        a -> {
                            if (a.getPrix() == null || a.getPrix().compareTo(BigDecimal.ZERO) == 0) return "Gratuit";
                            double p = a.getPrix().doubleValue();
                            if (p <= 20) return "1-20 TND";
                            if (p <= 50) return "21-50 TND";
                            if (p <= 100) return "51-100 TND";
                            return "> 100 TND";
                        },
                        Collectors.counting()));
                chartTitle = "Prix";
            }
            case "Devise" -> {
                counts = filteredData.stream().collect(Collectors.groupingBy(
                        a -> safe(a.getDevise()).isBlank() ? "(vide)" : safe(a.getDevise()).trim().toUpperCase(),
                        Collectors.counting()));
                chartTitle = "Devise";
            }
            case "Places" -> {
                counts = filteredData.stream().collect(Collectors.groupingBy(
                        a -> {
                            if (a.getNbPlaces() == null) return "Non défini";
                            if (a.getPlacesDispo() == null || a.getPlacesDispo() == 0) return "Complet";
                            double ratio = (double) a.getPlacesDispo() / a.getNbPlaces();
                            if (ratio <= 0.25) return "Presque complet";
                            if (ratio <= 0.75) return "Partiellement dispo";
                            return "Largement dispo";
                        },
                        Collectors.counting()));
                chartTitle = "Occupation Places";
            }
            case "Données remplies" -> {
                counts = buildFieldCompletion();
                chartTitle = "Données remplies";
            }
            default -> { // Catégorie
                counts = filteredData.stream().collect(Collectors.groupingBy(
                        a -> safe(a.getCategorie()).toLowerCase().isBlank() ? "autre" : safe(a.getCategorie()).toLowerCase(),
                        Collectors.counting()));
                chartTitle = "Catégorie";
            }
        }

        buildStatsCards(counts, total, field);
        buildPieChart(counts, total, chartTitle);
        buildBarChart(counts, total, chartTitle);
    }

    private Map<String, Long> buildFieldCompletion() {
        long nom = filteredData.stream().filter(a -> !safe(a.getNomActivite()).isBlank()).count();
        long desc = filteredData.stream().filter(a -> !safe(a.getDescription()).isBlank()).count();
        long cat = filteredData.stream().filter(a -> !safe(a.getCategorie()).isBlank()).count();
        long duree = filteredData.stream().filter(a -> a.getDuree() != null).count();
        long niveau = filteredData.stream().filter(a -> !safe(a.getNiveau()).isBlank()).count();
        long prix = filteredData.stream().filter(a -> a.getPrix() != null && a.getPrix().compareTo(BigDecimal.ZERO) > 0).count();
        long dates = filteredData.stream().filter(a -> a.getDateDebut() != null).count();
        long places = filteredData.stream().filter(a -> a.getNbPlaces() != null).count();
        long adresse = filteredData.stream().filter(a -> !safe(a.getAdresseDepart()).isBlank()).count();
        long equip = filteredData.stream().filter(a -> !safe(a.getEquipementInclus()).isBlank()).count();

        Map<String, Long> m = new LinkedHashMap<>();
        m.put("Nom", nom);
        m.put("Description", desc);
        m.put("Catégorie", cat);
        m.put("Durée", duree);
        m.put("Niveau", niveau);
        m.put("Prix", prix);
        m.put("Dates", dates);
        m.put("Places", places);
        m.put("Adresse", adresse);
        m.put("Équipement", equip);
        return m;
    }

    private void buildPieChart(Map<String, Long> counts, int total, String chartTitle) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String label = capitalize(entry.getKey()) + " (" + entry.getValue() + ")";
                    pieData.add(new PieChart.Data(label, entry.getValue()));
                });
        pieChart.setData(pieData);
        pieChart.setTitle("Répartition par " + chartTitle + " (" + total + ")");

        String[] palette = {"#667eea", "#f5576c", "#43e97b", "#FFB300", "#a18cd1",
                "#30cfd0", "#fa709a", "#4facfe", "#ff9a9e", "#fbc2eb"};
        int idx = 0;
        for (PieChart.Data d : pieChart.getData()) {
            String color;
            if ("Catégorie".equals(chartTitle)) {
                String catName = d.getName().split(" \\(")[0].toLowerCase();
                color = getCategoryColor(catName);
            } else {
                color = idx < palette.length ? palette[idx] : palette[idx % palette.length];
            }
            d.getNode().setStyle("-fx-pie-color: " + color + ";");
            double pct = total > 0 ? (d.getPieValue() / total * 100) : 0;
            Tooltip tip = new Tooltip(d.getName() + "\n" + String.format("%.1f%%", pct));
            tip.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
            Tooltip.install(d.getNode(), tip);
            idx++;
        }
    }

    private void buildBarChart(Map<String, Long> counts, int total, String chartTitle) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Activités");
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> series.getData().add(
                        new XYChart.Data<>(capitalize(entry.getKey()), entry.getValue())));

        barChart.getData().clear();
        barChart.getData().add(series);
        barChart.setTitle("Nombre par " + chartTitle + " (" + total + ")");

        String[] palette = {"#667eea", "#f5576c", "#43e97b", "#FFB300", "#a18cd1",
                "#30cfd0", "#fa709a", "#4facfe", "#ff9a9e", "#fbc2eb"};
        javafx.application.Platform.runLater(() -> {
            int i = 0;
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    String color;
                    if ("Catégorie".equals(chartTitle)) {
                        color = getCategoryColor(d.getXValue().toLowerCase());
                    } else {
                        color = i < palette.length ? palette[i] : palette[i % palette.length];
                    }
                    d.getNode().setStyle("-fx-bar-fill: " + color + ";");
                    Tooltip tip = new Tooltip(d.getXValue() + ": " + d.getYValue());
                    tip.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
                    Tooltip.install(d.getNode(), tip);
                }
                i++;
            }
        });
    }

    private void buildStatsCards(Map<String, Long> counts, int total, String field) {
        statsCardsRow.getChildren().clear();

        // Total card
        statsCardsRow.getChildren().add(buildStatCard("\uD83D\uDCCA", "Total", String.valueOf(total), "#FFC107"));

        String[] palette = {"#667eea", "#f5576c", "#43e97b", "#FFB300", "#a18cd1",
                "#30cfd0", "#fa709a", "#4facfe", "#ff9a9e", "#fbc2eb"};
        int idx = 0;
        for (var entry : counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList())) {
            String key = entry.getKey();
            long count = entry.getValue();
            double pct = total > 0 ? (count * 100.0 / total) : 0;
            String color;
            String emoji;
            if ("Catégorie".equals(field)) {
                String[] cfg = CATEGORY_CONFIG.getOrDefault(key.toLowerCase(), CATEGORY_CONFIG.get("autre"));
                emoji = cfg[0];
                color = cfg[1];
            } else if ("Données remplies".equals(field)) {
                emoji = "✅";
                color = idx < palette.length ? palette[idx] : palette[idx % palette.length];
            } else {
                emoji = "📌";
                color = idx < palette.length ? palette[idx] : palette[idx % palette.length];
            }
            statsCardsRow.getChildren().add(
                    buildStatCard(emoji, capitalize(key),
                            count + " (" + String.format("%.0f%%", pct) + ")", color));
            idx++;
        }
    }

    private VBox buildStatCard(String emoji, String title, String value, String accentColor) {
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 22px;");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("stats-card-title");

        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add("stats-card-value");
        valueLbl.setStyle("-fx-text-fill: " + accentColor + ";");

        VBox card = new VBox(4, emojiLbl, titleLbl, valueLbl);
        card.getStyleClass().add("stats-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12, 18, 12, 18));
        card.setStyle("-fx-border-color: " + accentColor + "33; -fx-border-width: 0 0 3 0;");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private String getCategoryColor(String category) {
        String[] cfg = CATEGORY_CONFIG.get(category);
        if (cfg != null) return cfg[1];
        for (Map.Entry<String, String[]> e : CATEGORY_CONFIG.entrySet()) {
            if (category.contains(e.getKey()) || e.getKey().contains(category)) return e.getValue()[1];
        }
        return "#30cfd0";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    @FXML
    private void onToggleChart(ActionEvent event) {
        showingPie = !showingPie;
        pieChart.setVisible(showingPie);
        pieChart.setManaged(showingPie);
        barChart.setVisible(!showingPie);
        barChart.setManaged(!showingPie);
        btnToggleChart.setText(showingPie ? "\uD83D\uDD04 Voir BarChart" : "\uD83D\uDD04 Voir PieChart");
    }

    @FXML
    private void onToggleStats(ActionEvent event) {
        statsVisible = !statsVisible;
        chartsContainer.setVisible(statsVisible);
        chartsContainer.setManaged(statsVisible);
        statsCardsRow.setVisible(statsVisible);
        statsCardsRow.setManaged(statsVisible);
        btnToggleStats.setText(statsVisible ? "\u25B2 Masquer" : "\u25BC Afficher");
    }

    // ====== HELPERS ======
    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private String getCategoryBadgeClass(String category) {
        if (category == null || category.isBlank()) return "badge-default";
        return switch (category.toLowerCase().trim()) {
            case "sport" -> "badge-hotel";       // blue tones
            case "culture" -> "badge-museum";     // purple tones
            case "loisir" -> "badge-cafe";        // warm tones
            case "nature" -> "badge-restaurant";  // green tones
            default -> "badge-default";
        };
    }

    private String getStatutBadgeClass(String statut) {
        if (statut == null || statut.isBlank()) return "badge-default";
        return switch (statut.toLowerCase().trim()) {
            case "disponible" -> "badge-restaurant"; // green
            case "complete" -> "badge-hotel";        // blue
            case "annulee" -> "badge-bar";           // red / dark
            default -> "badge-default";
        };
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

    // ====== PDF EXPORT ======
    private void exportSinglePdf(Activite a) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter en PDF");
        fc.setInitialFileName(safe(a.getNomActivite()).replaceAll("[^a-zA-Z0-9\\-_ ]", "") + "_fiche.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        javafx.stage.Window window = tableBody.getScene().getWindow();
        java.io.File file = fc.showSaveDialog(window);
        if (file == null) return;
        try {
            PdfExportService.exportActivite(a, file);
            showInfo("PDF g\u00e9n\u00e9r\u00e9 avec succ\u00e8s !\n" + file.getAbsolutePath());
            if (java.awt.Desktop.isDesktopSupported())
                new Thread(() -> { try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {} }).start();
        } catch (Exception ex) { showError("Erreur PDF : " + ex.getMessage()); ex.printStackTrace(); }
    }

    @FXML
    private void onExportAllPdf(ActionEvent event) {
        if (filteredData == null || filteredData.isEmpty()) { showError("Aucune activit\u00e9 \u00e0 exporter."); return; }
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter toutes les activit\u00e9s en PDF");
        fc.setInitialFileName("catalogue_activites.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        javafx.stage.Window window = tableBody.getScene().getWindow();
        java.io.File file = fc.showSaveDialog(window);
        if (file == null) return;
        try {
            PdfExportService.exportAllActivites(new java.util.ArrayList<>(filteredData), file);
            showInfo("Catalogue PDF g\u00e9n\u00e9r\u00e9 !\n" + filteredData.size() + " activit\u00e9(s) export\u00e9es.\n" + file.getAbsolutePath());
            if (java.awt.Desktop.isDesktopSupported())
                new Thread(() -> { try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {} }).start();
        } catch (Exception ex) { showError("Erreur PDF : " + ex.getMessage()); ex.printStackTrace(); }
    }

    @FXML
    private void onExportExcel(ActionEvent event) {
        if (filteredData == null || filteredData.isEmpty()) { showError("Aucune activit\u00e9 \u00e0 exporter."); return; }
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter en Excel");
        fc.setInitialFileName("activites.xlsx");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        javafx.stage.Window window = tableBody.getScene().getWindow();
        java.io.File file = fc.showSaveDialog(window);
        if (file == null) return;
        try {
            ExcelExportService.exportAllActivites(new java.util.ArrayList<>(filteredData), file);
            showInfo("Excel g\u00e9n\u00e9r\u00e9 !\n" + filteredData.size() + " activit\u00e9(s) export\u00e9es.\n" + file.getAbsolutePath());
            if (java.awt.Desktop.isDesktopSupported())
                new Thread(() -> { try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {} }).start();
        } catch (Exception ex) { showError("Erreur Excel : " + ex.getMessage()); ex.printStackTrace(); }
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Export r\u00e9ussi"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // ====== NAVIGATION ======
    @FXML
    private void goHome(ActionEvent event) {
        NavigationUtils.goTo("/maininterface.fxml", event);
    }

    @FXML
    private void goTableauEtab(ActionEvent event) {
        NavigationUtils.goTo("/etablissement_tableau.fxml", event);
    }

    @FXML
    private void goGalerie(ActionEvent event) {
        NavigationUtils.goTo("/activite_affichage.fxml", event);
    }



    @FXML
    private void onShowAjouter(ActionEvent event) {
        AffichageActiviteController.activiteToEdit = null;
        NavigationUtils.goTo("/ajouter_activite.fxml", event);
    }

    @FXML
    private void onRefresh(ActionEvent event) {
        loadData();
        applyFilters();
        rebuildTable();
    }

    @FXML
    private void toggleTheme(ActionEvent event) {
        ThemeManager.handleToggleTheme(event);
    }
}
