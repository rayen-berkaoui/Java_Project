package com.esprit.controllers;

import com.esprit.entities.Etablissement;
import com.esprit.services.EtablissementServices;
import com.esprit.services.PdfExportService;
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

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Premium Table view with Statistics.
 * Displays establishments in a professional data-table layout
 * with avatar circles, status badges, action buttons,
 * and dynamic PieChart / BarChart statistics by type.
 */
public class TableauEtablissementController {

    // ===== ROOT =====
    @FXML private StackPane rootStack;

    // ===== UI =====
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterVille;
    @FXML private ComboBox<String> filterType;
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

    private boolean showingPie = true;
    private boolean statsVisible = true;

    // ===== DATA =====
    private final EtablissementServices service = new EtablissementServices();
    private final ObservableList<Etablissement> masterData = FXCollections.observableArrayList();
    private FilteredList<Etablissement> filteredData;

    // Type → display config (emoji, color)
    private static final Map<String, String[]> TYPE_CONFIG = new LinkedHashMap<>();
    static {
        TYPE_CONFIG.put("hotel",      new String[]{"\uD83C\uDFE8", "#667eea"});
        TYPE_CONFIG.put("restaurant", new String[]{"\uD83C\uDF7D", "#f5576c"});
        TYPE_CONFIG.put("cafe",       new String[]{"☕",            "#FFB300"});
        TYPE_CONFIG.put("museum",     new String[]{"\uD83C\uDFDB", "#43e97b"});
        TYPE_CONFIG.put("bar",        new String[]{"\uD83C\uDF7A", "#a18cd1"});
        TYPE_CONFIG.put("autre",      new String[]{"📍",           "#30cfd0"});
    }

    // Avatar background colors (modern palette)
    private static final String[] AVATAR_COLORS = {
        "#667eea", "#f5576c", "#4facfe", "#43e97b",
        "#fa709a", "#a18cd1", "#30cfd0", "#FFB300"
    };

    @FXML
    public void initialize() {
        loadData();

        filteredData = new FilteredList<>(masterData, e -> true);

        searchField.textProperty().addListener((obs, o, n) -> { applyFilters(); rebuildTable(); updateCharts(); });
        filterVille.valueProperty().addListener((obs, o, n) -> { applyFilters(); rebuildTable(); updateCharts(); });
        filterType.valueProperty().addListener((obs, o, n) -> { applyFilters(); rebuildTable(); updateCharts(); });

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
        // Ville filter
        Set<String> villes = new TreeSet<>();
        masterData.forEach(e -> {
            if (e.getVille() != null && !e.getVille().isBlank())
                villes.add(e.getVille().trim());
        });

        List<String> villeItems = new ArrayList<>();
        villeItems.add("Tous");
        villeItems.addAll(villes);

        String currentVille = filterVille.getValue();
        filterVille.setItems(FXCollections.observableArrayList(villeItems));
        if (currentVille != null && villeItems.contains(currentVille))
            filterVille.setValue(currentVille);
        else
            filterVille.getSelectionModel().selectFirst();

        // Type filter
        filterType.setItems(FXCollections.observableArrayList(
                "Tous", "hotel", "restaurant", "cafe", "museum", "bar", "autre"));
        if (filterType.getValue() == null)
            filterType.getSelectionModel().selectFirst();
    }

    private void applyFilters() {
        String q = safe(searchField.getText()).toLowerCase().trim();
        String ville = safe(filterVille.getValue());
        String type = safe(filterType.getValue());

        filteredData.setPredicate(e -> {
            if (e == null) return false;
            if (!"Tous".equalsIgnoreCase(ville) && !safe(e.getVille()).equalsIgnoreCase(ville)) return false;
            if (!"Tous".equalsIgnoreCase(type) && !safe(e.getType()).equalsIgnoreCase(type)) return false;
            if (q.isEmpty()) return true;
            return safe(e.getNom()).toLowerCase().contains(q)
                    || safe(e.getVille()).toLowerCase().contains(q)
                    || safe(e.getEmail()).toLowerCase().contains(q)
                    || safe(e.getTelephone()).toLowerCase().contains(q);
        });
    }

    // ====== TABLE BUILDING ======
    private void rebuildTable() {
        tableBody.getChildren().clear();

        for (Etablissement e : filteredData) {
            tableBody.getChildren().add(buildRow(e));
        }

        if (filteredData.isEmpty()) {
            Label empty = new Label("Aucun établissement trouvé");
            empty.getStyleClass().add("muted");
            empty.setStyle("-fx-padding: 40 0;");
            HBox emptyRow = new HBox(empty);
            emptyRow.setAlignment(Pos.CENTER);
            tableBody.getChildren().add(emptyRow);
        }
    }

    private HBox buildRow(Etablissement e) {

        // ── Avatar circle with initials ──
        String initials = getInitials(e.getNom());
        Label avatarText = new Label(initials);
        avatarText.getStyleClass().add("avatar-text");

        StackPane avatar = new StackPane(avatarText);
        avatar.getStyleClass().add("avatar-circle");
        int colorIdx = Math.abs(e.getIdEtablissement()) % AVATAR_COLORS.length;
        avatar.setStyle("-fx-background-color: " + AVATAR_COLORS[colorIdx] + ";");

        // Name
        Label name = new Label(safe(e.getNom()));
        name.getStyleClass().add("table-cell-bold");

        HBox nameCell = new HBox(12, avatar, name);
        nameCell.setAlignment(Pos.CENTER_LEFT);
        nameCell.setPrefWidth(240);
        nameCell.setMinWidth(240);

        // ── Ville ──
        Label ville = new Label(safe(e.getVille()));
        ville.getStyleClass().add("table-cell-text");
        ville.setPrefWidth(130);
        ville.setMinWidth(130);

        // ── Type badge (colored like status in image 2) ──
        Label typeBadge = new Label(safe(e.getType()).toUpperCase());
        typeBadge.getStyleClass().addAll("status-badge", getBadgeClass(e.getType()));
        typeBadge.setPrefWidth(100);
        typeBadge.setMinWidth(100);

        // ── Gamme prix (bold like montant in image 2) ──
        Label gamme = new Label(safe(e.getGammePrix()));
        gamme.getStyleClass().add("table-cell-bold");
        gamme.setPrefWidth(90);
        gamme.setMinWidth(90);

        // ── Telephone ──
        Label tel = new Label(safe(e.getTelephone()));
        tel.getStyleClass().add("table-cell-text");
        tel.setPrefWidth(140);
        tel.setMinWidth(140);

        // ── Action buttons (edit / delete icons like image 2) ──
        Button editBtn = new Button("\u270E");
        editBtn.getStyleClass().addAll("table-action-btn", "table-action-edit");
        editBtn.setCursor(Cursor.HAND);
        editBtn.setOnAction(ev -> {
            AffichageEtablissementController.etablissementToEdit = e;
            NavigationUtils.goTo("/ajouter_etablissement.fxml",
                    new ActionEvent(ev.getSource(), ev.getTarget()));
        });

        Button deleteBtn = new Button("\uD83D\uDDD1");
        deleteBtn.getStyleClass().addAll("table-action-btn", "table-action-delete");
        deleteBtn.setCursor(Cursor.HAND);
        deleteBtn.setOnAction(ev -> {
            boolean ok = confirm("Supprimer",
                    "Voulez-vous supprimer : " + safe(e.getNom()) + " ?");
            if (!ok) return;
            try {
                service.supprimer(e.getIdEtablissement());
                loadData();
                applyFilters();
                rebuildTable();
                SuccessNotification.show(tableBody, "Établissement supprimé !");
            } catch (Exception ex) {
                showError("Erreur: " + ex.getMessage());
            }
        });

        Button pdfBtn = new Button("\uD83D\uDCC4");
        pdfBtn.getStyleClass().addAll("table-action-btn", "table-action-pdf");
        pdfBtn.setCursor(Cursor.HAND);
        pdfBtn.setOnAction(ev -> exportSinglePdf(e));

        HBox actionsBox = new HBox(8, editBtn, deleteBtn, pdfBtn);
        actionsBox.setAlignment(Pos.CENTER);
        actionsBox.setPrefWidth(140);
        actionsBox.setMinWidth(140);

        // ── Row ──
        HBox row = new HBox(nameCell, ville, typeBadge, gamme, tel, actionsBox);
        row.getStyleClass().add("table-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(Cursor.HAND);

        return row;
    }

    // ====== CHARTS & STATISTICS ======
    private void updateCharts() {
        // Count by type
        Map<String, Long> typeCounts = filteredData.stream()
                .collect(Collectors.groupingBy(
                        e -> safe(e.getType()).toLowerCase().isBlank() ? "autre" : safe(e.getType()).toLowerCase(),
                        Collectors.counting()));

        int total = filteredData.size();
        countLabel.setText(total + " établissement" + (total > 1 ? "s" : ""));

        // ── Summary cards ──
        buildStatsCards(typeCounts, total);

        // ── Pie Chart ──
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        typeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String label = capitalize(entry.getKey()) + " (" + entry.getValue() + ")";
                    pieData.add(new PieChart.Data(label, entry.getValue()));
                });
        pieChart.setData(pieData);
        pieChart.setTitle("Répartition par Type (" + total + ")");

        // Style pie slices with type colors
        int idx = 0;
        for (PieChart.Data d : pieChart.getData()) {
            String typeName = d.getName().split(" \\(")[0].toLowerCase();
            String color = getTypeColor(typeName);
            d.getNode().setStyle("-fx-pie-color: " + color + ";");

            // Tooltip
            double pct = total > 0 ? (d.getPieValue() / total * 100) : 0;
            Tooltip tip = new Tooltip(d.getName() + "\n" + String.format("%.1f%%", pct));
            tip.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
            Tooltip.install(d.getNode(), tip);
            idx++;
        }

        // ── Bar Chart ──
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Établissements");
        typeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> series.getData().add(
                        new XYChart.Data<>(capitalize(entry.getKey()), entry.getValue())));

        barChart.getData().clear();
        barChart.getData().add(series);
        barChart.setTitle("Nombre par Type (" + total + ")");

        // Style bar colors
        javafx.application.Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    String color = getTypeColor(d.getXValue().toLowerCase());
                    d.getNode().setStyle("-fx-bar-fill: " + color + ";");
                    Tooltip tip = new Tooltip(d.getXValue() + ": " + d.getYValue());
                    tip.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
                    Tooltip.install(d.getNode(), tip);
                }
            }
        });
    }

    private void buildStatsCards(Map<String, Long> typeCounts, int total) {
        statsCardsRow.getChildren().clear();

        // Total card
        statsCardsRow.getChildren().add(buildStatCard("📊", "Total", String.valueOf(total), "#FFC107"));

        // Per-type cards (sorted by count desc)
        typeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String type = entry.getKey();
                    long count = entry.getValue();
                    double pct = total > 0 ? (count * 100.0 / total) : 0;
                    String[] cfg = TYPE_CONFIG.getOrDefault(type, TYPE_CONFIG.get("autre"));
                    statsCardsRow.getChildren().add(
                            buildStatCard(cfg[0], capitalize(type),
                                    count + " (" + String.format("%.0f%%", pct) + ")", cfg[1]));
                });
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

    private String getTypeColor(String type) {
        String[] cfg = TYPE_CONFIG.get(type);
        if (cfg != null) return cfg[1];
        // try partial match
        for (Map.Entry<String, String[]> e : TYPE_CONFIG.entrySet()) {
            if (type.contains(e.getKey()) || e.getKey().contains(type)) return e.getValue()[1];
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
        btnToggleStats.setText(statsVisible ? "▲ Masquer" : "▼ Afficher");
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

    private String getBadgeClass(String type) {
        if (type == null || type.isBlank()) return "badge-default";
        return switch (type.toLowerCase().trim()) {
            case "hotel" -> "badge-hotel";
            case "restaurant" -> "badge-restaurant";
            case "cafe" -> "badge-cafe";
            case "museum" -> "badge-museum";
            case "bar" -> "badge-bar";
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
    private void exportSinglePdf(Etablissement e) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter en PDF");
        fc.setInitialFileName(safe(e.getNom()).replaceAll("[^a-zA-Z0-9\\-_ ]", "") + "_fiche.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        javafx.stage.Window window = tableBody.getScene().getWindow();
        java.io.File file = fc.showSaveDialog(window);
        if (file == null) return;
        try {
            PdfExportService.exportEtablissement(e, file);
            showInfo("PDF g\u00e9n\u00e9r\u00e9 avec succ\u00e8s !\n" + file.getAbsolutePath());
            if (java.awt.Desktop.isDesktopSupported())
                new Thread(() -> { try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {} }).start();
        } catch (Exception ex) { showError("Erreur PDF : " + ex.getMessage()); ex.printStackTrace(); }
    }

    @FXML
    private void onExportAllPdf(ActionEvent event) {
        if (filteredData == null || filteredData.isEmpty()) { showError("Aucun \u00e9tablissement \u00e0 exporter."); return; }
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter tous les \u00e9tablissements en PDF");
        fc.setInitialFileName("catalogue_etablissements.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        javafx.stage.Window window = tableBody.getScene().getWindow();
        java.io.File file = fc.showSaveDialog(window);
        if (file == null) return;
        try {
            PdfExportService.exportAllEtablissements(new java.util.ArrayList<>(filteredData), file);
            showInfo("Catalogue PDF g\u00e9n\u00e9r\u00e9 !\n" + filteredData.size() + " \u00e9tablissement(s) export\u00e9(s).\n" + file.getAbsolutePath());
            if (java.awt.Desktop.isDesktopSupported())
                new Thread(() -> { try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {} }).start();
        } catch (Exception ex) { showError("Erreur PDF : " + ex.getMessage()); ex.printStackTrace(); }
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Export r\u00e9ussi"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // ====== NAVIGATION ======
    @FXML
    private void goHome(ActionEvent event) {
        NavigationUtils.goTo("/home.fxml", event);
    }

    @FXML
    private void goGalerie(ActionEvent event) {
        NavigationUtils.goTo("/etablissement_affichage.fxml", event);
    }

    @FXML
    private void goActivites(ActionEvent event) {
        NavigationUtils.goTo("/activite_affichage.fxml", event);
    }

    @FXML
    private void goTableauActivites(ActionEvent event) {
        NavigationUtils.goTo("/activite_tableau.fxml", event);
    }

    @FXML
    private void onShowAjouter(ActionEvent event) {
        AffichageEtablissementController.etablissementToEdit = null;
        NavigationUtils.goTo("/ajouter_etablissement.fxml", event);
    }

    @FXML
    private void onRefresh(ActionEvent event) {
        loadData();
        applyFilters();
        rebuildTable();
    }
}
