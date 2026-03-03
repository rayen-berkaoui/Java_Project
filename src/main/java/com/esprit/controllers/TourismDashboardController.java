package com.esprit.controllers;

import com.esprit.entities.Adresse;
import com.esprit.entities.LieuTouristique;
import com.esprit.entities.categorie;
import com.esprit.services.AdresseServices;
import com.esprit.services.LieuTouristiqueServices;
import com.esprit.services.categorieServices;
import com.esprit.services.PdfExportService;
import com.esprit.services.ExcelExportService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TourismDashboardController {

    // ===== Date Filters =====
    @FXML private DatePicker dateFrom, dateTo;
    @FXML private Label lblFilterInfo;

    // ===== KPI Cards =====
    @FXML private Label lblTotalLieux, lblTotalCategories, lblAvgPrice, lblActiveRate, lblTopCity, lblTotalVilles;

    // ===== Chart Containers =====
    @FXML private VBox chartCategorieBox, chartVilleBox, chartPriceBox;
    @FXML private VBox chartAvgPriceCat, chartStatutBox, chartCatTimeline;
    @FXML private VBox topPlacesBox;

    // ===== Heatmap =====
    @FXML private WebView heatmapWebView;
    @FXML private VBox dashboardRoot;

    private LieuTouristiqueServices lieuServices;
    private categorieServices catServices;
    private AdresseServices adresseServices;

    // Full data (no filter)
    private List<LieuTouristique> allLieux = new ArrayList<>();
    private List<categorie> allCategories = new ArrayList<>();
    private List<Adresse> allAdresses = new ArrayList<>();

    // Filtered data (currently displayed)
    private List<LieuTouristique> lieux = new ArrayList<>();
    private List<categorie> categories = new ArrayList<>();

    @FXML
    public void initialize() {
        try {
            lieuServices = new LieuTouristiqueServices();
            catServices = new categorieServices();
            adresseServices = new AdresseServices();
        } catch (Exception e) {
            System.err.println("ÔØî Dashboard: Error initializing services: " + e.getMessage());
        }

        Platform.runLater(() -> {
            loadData();
            applyFilter(null, null); // show all
            buildDashboard();
            playEntranceAnimation();
        });
    }

    private void loadData() {
        try {
            allLieux = lieuServices.afficher();
            allCategories = catServices.afficher();
            allAdresses = adresseServices.afficher();
        } catch (SQLException e) {
            System.err.println("ÔØî Dashboard: Error loading data: " + e.getMessage());
        }
    }

    // ==================== DATE FILTERING ====================

    private void applyFilter(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            // No filter ÔÇö show all
            categories = new ArrayList<>(allCategories);
            lieux = new ArrayList<>(allLieux);
            if (lblFilterInfo != null) lblFilterInfo.setText("");
        } else {
            // Filter categories by date_creation
            categories = allCategories.stream()
                    .filter(c -> {
                        LocalDate d = c.getDateCreation();
                        if (d == null) return false;
                        if (from != null && d.isBefore(from)) return false;
                        if (to != null && d.isAfter(to)) return false;
                        return true;
                    })
                    .collect(Collectors.toList());

            // Get IDs of filtered categories
            Set<Integer> catIds = categories.stream()
                    .map(categorie::getIdcategorie)
                    .collect(Collectors.toSet());

            // Filter lieux to only those in filtered categories
            lieux = allLieux.stream()
                    .filter(l -> catIds.contains(l.getId_categorie()))
                    .collect(Collectors.toList());

            // Update info label
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String info = "­ƒöì Filtre actif : ";
            if (from != null && to != null) info += from.format(fmt) + " ÔåÆ " + to.format(fmt);
            else if (from != null) info += "depuis " + from.format(fmt);
            else info += "jusqu'au " + to.format(fmt);
            info += " | " + categories.size() + " cat├®g. | " + lieux.size() + " lieux";
            if (lblFilterInfo != null) lblFilterInfo.setText(info);
        }
    }

    @FXML
    public void applyDateFilter() {
        LocalDate from = dateFrom != null ? dateFrom.getValue() : null;
        LocalDate to = dateTo != null ? dateTo.getValue() : null;
        applyFilter(from, to);
        rebuildAll();
    }

    @FXML
    public void resetFilter() {
        if (dateFrom != null) dateFrom.setValue(null);
        if (dateTo != null) dateTo.setValue(null);
        applyFilter(null, null);
        rebuildAll();
    }

    @FXML
    public void filterLast7Days() {
        setFilterPeriod(7);
    }

    @FXML
    public void filterLast30Days() {
        setFilterPeriod(30);
    }

    @FXML
    public void filterLast90Days() {
        setFilterPeriod(90);
    }

    @FXML
    public void filterLastYear() {
        setFilterPeriod(365);
    }

    private void setFilterPeriod(int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days);
        if (dateFrom != null) dateFrom.setValue(from);
        if (dateTo != null) dateTo.setValue(to);
        applyFilter(from, to);
        rebuildAll();
    }

    private void rebuildAll() {
        buildDashboard();
    }

    // ==================== BUILD ALL ====================

    private void buildDashboard() {
        updateKPIs();
        buildCategoryPieChart();
        buildVilleBarChart();
        buildPriceDistributionChart();
        buildAvgPriceByCategoryChart();
        buildStatutPieChart();
        buildCategoryTimelineChart();
        buildTopPlacesTable();
        buildHeatmap();
    }

    // ==================== KPI CARDS ====================

    private void updateKPIs() {
        if (lblTotalLieux != null) lblTotalLieux.setText(String.valueOf(lieux.size()));
        if (lblTotalCategories != null) lblTotalCategories.setText(String.valueOf(categories.size()));

        if (lieux.isEmpty()) {
            if (lblAvgPrice != null) lblAvgPrice.setText("N/A");
            if (lblActiveRate != null) lblActiveRate.setText("N/A");
            if (lblTopCity != null) lblTopCity.setText("-");
            if (lblTotalVilles != null) lblTotalVilles.setText("0");
            return;
        }

        double avg = lieux.stream().mapToDouble(LieuTouristique::getPrix).average().orElse(0);
        if (lblAvgPrice != null) {
            lblAvgPrice.setText(String.format("%.0f TND", avg));
            animateLabel(lblAvgPrice);
        }

        long active = lieux.stream().filter(l -> l.getStatut() == 1).count();
        double rate = (double) active / lieux.size() * 100;
        if (lblActiveRate != null) {
            lblActiveRate.setText(String.format("%.0f%%", rate));
            animateLabel(lblActiveRate);
        }

        // Top city
        Map<String, Long> cityCount = lieux.stream()
                .filter(l -> l.getVille() != null && !l.getVille().isEmpty())
                .collect(Collectors.groupingBy(l -> l.getVille().trim(), Collectors.counting()));
        if (lblTopCity != null) {
            cityCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .ifPresentOrElse(
                            e -> lblTopCity.setText(e.getKey()),
                            () -> lblTopCity.setText("-")
                    );
            animateLabel(lblTopCity);
        }

        // Total distinct cities
        if (lblTotalVilles != null) {
            long distinctVilles = lieux.stream()
                    .map(LieuTouristique::getVille)
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .distinct()
                    .count();
            lblTotalVilles.setText(String.valueOf(distinctVilles));
        }
    }

    private void animateLabel(Label label) {
        if (label == null) return;
        label.setOpacity(0);
        label.setTranslateY(10);
        FadeTransition fade = new FadeTransition(Duration.millis(500), label);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(500), label);
        slide.setFromY(10);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    // ==================== CHART: Category Pie ====================

    private void buildCategoryPieChart() {
        if (chartCategorieBox == null) return;
        chartCategorieBox.getChildren().clear();

        Map<Integer, String> catNames = new HashMap<>();
        for (categorie c : categories) catNames.put(c.getIdcategorie(), c.getNomcategorie());

        Map<String, Integer> catCounts = new LinkedHashMap<>();
        for (LieuTouristique l : lieux) {
            String name = catNames.getOrDefault(l.getId_categorie(), "Inconnue");
            catCounts.merge(name, 1, Integer::sum);
        }

        if (catCounts.isEmpty()) {
            chartCategorieBox.getChildren().add(createEmptyLabel("Aucune donn├®e disponible"));
            return;
        }

        PieChart pieChart = new PieChart();
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(true);
        pieChart.setStartAngle(90);
        pieChart.setMinHeight(300);
        pieChart.setPrefHeight(320);
        pieChart.setMaxHeight(350);

        for (Map.Entry<String, Integer> entry : catCounts.entrySet()) {
            pieChart.getData().add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }

        pieChart.getStyleClass().add("dashboard-pie-chart");
        chartCategorieBox.getChildren().add(pieChart);

        Platform.runLater(() -> {
            String[] colors = {"#BFA200", "#D4B530", "#8a7000", "#e8dfa0", "#6b5800", "#c9a800", "#a89000", "#f0e6a0"};
            int i = 0;
            int total = catCounts.values().stream().mapToInt(Integer::intValue).sum();
            for (PieChart.Data d : pieChart.getData()) {
                if (d.getNode() != null) {
                    d.getNode().setStyle("-fx-pie-color: " + colors[i % colors.length] + ";");
                    double pct = (d.getPieValue() / total) * 100.0;
                    Tooltip tooltip = new Tooltip(d.getName() + "\n" + String.format("%.1f%%", pct));
                    tooltip.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
                    Tooltip.install(d.getNode(), tooltip);
                    d.getNode().setOnMouseEntered(e -> d.getNode().setStyle("-fx-opacity: 0.8; -fx-cursor: hand;"));
                    d.getNode().setOnMouseExited(e -> d.getNode().setStyle("-fx-opacity: 1;"));
                }
                i++;
            }
        });
    }

    // ==================== CHART: Ville Bar ====================

    private void buildVilleBarChart() {
        if (chartVilleBox == null) return;
        chartVilleBox.getChildren().clear();

        Map<String, Integer> villeCounts = new LinkedHashMap<>();
        for (LieuTouristique l : lieux) {
            String v = l.getVille() != null ? l.getVille() : "Inconnue";
            villeCounts.merge(v, 1, Integer::sum);
        }

        if (villeCounts.isEmpty()) {
            chartVilleBox.getChildren().add(createEmptyLabel("Aucune donn├®e disponible"));
            return;
        }

        List<Map.Entry<String, Integer>> sorted = villeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Ville");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Nombre de lieux");
        styleAxis(xAxis, yAxis);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(15);
        barChart.setBarGap(3);
        barChart.setMinHeight(300);
        barChart.setPrefHeight(320);
        barChart.setMaxHeight(350);
        barChart.setAnimated(true);
        barChart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        barChart.getData().add(series);
        barChart.getStyleClass().add("dashboard-bar-chart");
        chartVilleBox.getChildren().add(barChart);

        Platform.runLater(() -> colorBarsGold(barChart));
    }

    // ==================== CHART: Price Distribution ====================

    private void buildPriceDistributionChart() {
        if (chartPriceBox == null) return;
        chartPriceBox.getChildren().clear();

        if (lieux.isEmpty()) {
            chartPriceBox.getChildren().add(createEmptyLabel("Aucune donn├®e disponible"));
            return;
        }

        String[] rangeLabels = {"Gratuit", "1-50", "50-100", "100-200", "200-500", "500+"};
        int[] rangeCounts = new int[6];

        for (LieuTouristique l : lieux) {
            double p = l.getPrix();
            if (p == 0) rangeCounts[0]++;
            else if (p <= 50) rangeCounts[1]++;
            else if (p <= 100) rangeCounts[2]++;
            else if (p <= 200) rangeCounts[3]++;
            else if (p <= 500) rangeCounts[4]++;
            else rangeCounts[5]++;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Tranche de prix (TND)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Nombre de lieux");
        styleAxis(xAxis, yAxis);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(12);
        barChart.setBarGap(2);
        barChart.setMinHeight(300);
        barChart.setPrefHeight(320);
        barChart.setMaxHeight(350);
        barChart.setAnimated(true);
        barChart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < rangeLabels.length; i++) {
            series.getData().add(new XYChart.Data<>(rangeLabels[i], rangeCounts[i]));
        }
        barChart.getData().add(series);
        barChart.getStyleClass().add("dashboard-bar-chart");
        chartPriceBox.getChildren().add(barChart);

        Platform.runLater(() -> colorBarsGold(barChart));
    }

    // ==================== CHART: Avg Price by Category ====================

    private void buildAvgPriceByCategoryChart() {
        if (chartAvgPriceCat == null) return;
        chartAvgPriceCat.getChildren().clear();

        if (categories.isEmpty() || lieux.isEmpty()) {
            chartAvgPriceCat.getChildren().add(createEmptyLabel("Aucune donn├®e"));
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Cat├®gorie");
        yAxis.setLabel("Prix moyen (TND)");
        styleAxis(xAxis, yAxis);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(280);
        chart.setStyle("-fx-background-color: transparent;");
        chart.setMinHeight(300);
        chart.setMaxHeight(350);

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        Map<Integer, String> catNames = categories.stream()
                .collect(Collectors.toMap(categorie::getIdcategorie, categorie::getNomcategorie));

        Map<Integer, Double> avgPrices = lieux.stream()
                .collect(Collectors.groupingBy(LieuTouristique::getId_categorie,
                        Collectors.averagingDouble(LieuTouristique::getPrix)));

        avgPrices.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .forEach(e -> {
                    String name = catNames.getOrDefault(e.getKey(), "ID:" + e.getKey());
                    if (name.length() > 15) name = name.substring(0, 12) + "...";
                    series.getData().add(new XYChart.Data<>(name, e.getValue()));
                });

        chart.getData().add(series);
        chartAvgPriceCat.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);

        Platform.runLater(() -> colorBarsGold(chart));
    }

    // ==================== CHART: Status Donut ====================

    private void buildStatutPieChart() {
        if (chartStatutBox == null) return;
        chartStatutBox.getChildren().clear();

        if (lieux.isEmpty()) {
            chartStatutBox.getChildren().add(createEmptyLabel("Aucune donn├®e disponible"));
            return;
        }

        long active = lieux.stream().filter(l -> l.getStatut() == 1).count();
        long inactive = lieux.size() - active;

        PieChart pieChart = new PieChart();
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(true);
        pieChart.setStartAngle(90);
        pieChart.setMinHeight(300);
        pieChart.setPrefHeight(320);
        pieChart.setMaxHeight(350);

        pieChart.getData().add(new PieChart.Data("Actif (" + active + ")", active));
        pieChart.getData().add(new PieChart.Data("Inactif (" + inactive + ")", inactive));
        pieChart.getStyleClass().add("dashboard-pie-chart");
        chartStatutBox.getChildren().add(pieChart);

        Platform.runLater(() -> {
            if (pieChart.getData().size() >= 2) {
                Node activeNode = pieChart.getData().get(0).getNode();
                Node inactiveNode = pieChart.getData().get(1).getNode();
                if (activeNode != null) activeNode.setStyle("-fx-pie-color: #00b36b;");
                if (inactiveNode != null) inactiveNode.setStyle("-fx-pie-color: #e63946;");
                Tooltip.install(activeNode, new Tooltip("Actif: " + active + " lieux"));
                Tooltip.install(inactiveNode, new Tooltip("Inactif: " + inactive + " lieux"));
            }
        });
    }

    // ==================== CHART: Category Timeline ====================

    private void buildCategoryTimelineChart() {
        if (chartCatTimeline == null) return;
        chartCatTimeline.getChildren().clear();

        List<categorie> dated = categories.stream()
                .filter(c -> c.getDateCreation() != null)
                .sorted(Comparator.comparing(categorie::getDateCreation))
                .collect(Collectors.toList());

        if (dated.isEmpty()) {
            chartCatTimeline.getChildren().add(createEmptyLabel("Aucune cat├®gorie avec date"));
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Date");
        yAxis.setLabel("Cat├®gories cumul├®es");
        styleAxis(xAxis, yAxis);

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(280);
        chart.setMinHeight(300);
        chart.setMaxHeight(350);
        chart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");

        int cumulative = 0;
        for (categorie c : dated) {
            cumulative++;
            series.getData().add(new XYChart.Data<>(c.getDateCreation().format(fmt), cumulative));
        }

        chart.getData().add(series);
        chartCatTimeline.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);

        Platform.runLater(() -> {
            Node line = chart.lookup(".chart-series-line");
            if (line != null) {
                line.setStyle("-fx-stroke: #BFA200; -fx-stroke-width: 3px;");
            }
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node symbol = data.getNode();
                if (symbol != null) {
                    symbol.setStyle("-fx-background-color: #BFA200, #0a0a12; -fx-background-radius: 6; -fx-padding: 4;");
                }
            }
        });
    }

    // ==================== TOP PLACES TABLE ====================

    private void buildTopPlacesTable() {
        if (topPlacesBox == null) return;
        topPlacesBox.getChildren().clear();

        if (lieux.isEmpty()) {
            topPlacesBox.getChildren().add(createEmptyLabel("Aucun lieu"));
            return;
        }

        Map<Integer, String> catNames = categories.stream()
                .collect(Collectors.toMap(categorie::getIdcategorie, categorie::getNomcategorie, (a, b) -> a));

        // Header
        HBox header = createTableRow("­ƒÅà", "Nom", "Ville", "Cat├®gorie", "Prix", true);
        topPlacesBox.getChildren().add(header);

        List<LieuTouristique> top5 = lieux.stream()
                .sorted(Comparator.comparingDouble(LieuTouristique::getPrix).reversed())
                .limit(5)
                .collect(Collectors.toList());

        int rank = 1;
        for (LieuTouristique l : top5) {
            String medal;
            switch (rank) {
                case 1: medal = "­ƒÑç"; break;
                case 2: medal = "­ƒÑê"; break;
                case 3: medal = "­ƒÑë"; break;
                default: medal = "#" + rank;
            }
            String catName = catNames.getOrDefault(l.getId_categorie(), "-");
            HBox row = createTableRow(medal, l.getNom(), l.getVille() != null ? l.getVille() : "-",
                    catName, String.format("%.0f TND", l.getPrix()), false);

            row.setOpacity(0);
            row.setTranslateX(-20);
            FadeTransition fade = new FadeTransition(Duration.millis(400), row);
            fade.setDelay(Duration.millis(rank * 100));
            fade.setFromValue(0);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(400), row);
            slide.setDelay(Duration.millis(rank * 100));
            slide.setFromX(-20);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fade, slide).play();

            topPlacesBox.getChildren().add(row);
            rank++;
        }
    }

    private HBox createTableRow(String rank, String name, String city, String category, String price, boolean isHeader) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));

        if (isHeader) {
            row.setStyle("-fx-background-color: rgba(191,162,0,0.1); -fx-background-radius: 8;");
        } else {
            row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8;");
            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(191,162,0,0.08); -fx-background-radius: 8;"));
            row.setOnMouseExited(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8;"));
        }

        String textStyle = isHeader
                ? "-fx-text-fill: #BFA200; -fx-font-weight: bold; -fx-font-size: 12px;"
                : "-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 12px;";

        Label rankLbl = new Label(rank);
        rankLbl.setStyle(isHeader ? textStyle : "-fx-font-size: 16px;");
        rankLbl.setMinWidth(40);

        Label nameLbl = new Label(name);
        nameLbl.setStyle(textStyle + (isHeader ? "" : " -fx-font-weight: bold;"));
        nameLbl.setMinWidth(200);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        Label cityLbl = new Label(city);
        cityLbl.setStyle(textStyle);
        cityLbl.setMinWidth(120);

        Label catLbl = new Label(category);
        catLbl.setStyle(textStyle);
        catLbl.setMinWidth(140);

        Label priceLbl = new Label(price);
        priceLbl.setStyle(isHeader ? textStyle : "-fx-text-fill: #BFA200; -fx-font-weight: bold; -fx-font-size: 13px;");
        priceLbl.setMinWidth(80);

        row.getChildren().addAll(rankLbl, nameLbl, cityLbl, catLbl, priceLbl);
        return row;
    }

    // ==================== HEATMAP ====================

    private void buildHeatmap() {
        if (heatmapWebView == null) return;

        Map<Integer, Adresse> adresseMap = new HashMap<>();
        for (Adresse a : allAdresses) adresseMap.put(a.getId_adresse(), a);

        StringBuilder markers = new StringBuilder();
        double sumLat = 0, sumLon = 0;
        int count = 0;

        for (LieuTouristique l : lieux) {
            Adresse a = adresseMap.get(l.getId_adresse());
            if (a != null && a.getLatitude() != 0 && a.getLongitude() != 0) {
                String tooltip = l.getNom() != null ? l.getNom().replace("'", "\\'").replace("\"", "&quot;") : "";
                String city = l.getVille() != null ? l.getVille().replace("'", "\\'") : "";
                String statusColor = l.getStatut() == 1 ? "#27ae60" : "#c0392b";
                markers.append(String.format(
                        "addMarker(%f, %f, '%s', '%s', '%s', %s);\n",
                        a.getLatitude(), a.getLongitude(), tooltip, city,
                        statusColor, String.format("%.2f", l.getPrix())
                ));
                sumLat += a.getLatitude();
                sumLon += a.getLongitude();
                count++;
            }
        }

        // Also add adresses not linked to any lieu
        for (Adresse a : allAdresses) {
            if (a.getLatitude() != 0 && a.getLongitude() != 0) {
                boolean linked = allLieux.stream().anyMatch(l -> l.getId_adresse() == a.getId_adresse());
                if (!linked) {
                    markers.append(String.format(
                            "addMarker(%f, %f, '%s', '%s', '#BFA200', 'ÔÇö');\n",
                            a.getLatitude(), a.getLongitude(),
                            (a.getRue() != null ? a.getRue().replace("'", "\\'") : "Adresse"),
                            (a.getVille() != null ? a.getVille().replace("'", "\\'") : "")
                    ));
                    sumLat += a.getLatitude();
                    sumLon += a.getLongitude();
                    count++;
                }
            }
        }

        double centerLat = count > 0 ? sumLat / count : 36.8065;
        double centerLon = count > 0 ? sumLon / count : 10.1815;
        int zoom = count <= 1 ? 10 : (count <= 5 ? 8 : 6);

        String html = getHeatmapHtml(centerLat, centerLon, zoom, markers.toString(), count);

        final int markerCount = count;
        WebEngine engine = heatmapWebView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                System.out.println("Ô£à Dashboard heatmap loaded with " + markerCount + " markers");
            }
        });
        engine.loadContent(html);
    }

    private String getHeatmapHtml(double lat, double lon, int zoom, String markers, int totalMarkers) {
        return "<!DOCTYPE html>\n" +
                "<html><head><meta charset='utf-8'/>\n" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>\n" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>\n" +
                "<style>\n" +
                "  html,body{margin:0;padding:0;height:100%;background:#0d1a2a;overflow:hidden;font-family:'Segoe UI',sans-serif;}\n" +
                "  #map{width:100%;height:100%;}\n" +
                "  .leaflet-container{background:#0d1a2a;}\n" +
                "  .custom-popup .leaflet-popup-content-wrapper{background:rgba(10,10,20,0.95);color:white;border:1px solid rgba(191,162,0,0.4);border-radius:10px;box-shadow:0 4px 20px rgba(0,0,0,0.5);}\n" +
                "  .custom-popup .leaflet-popup-tip{background:rgba(10,10,20,0.95);border:1px solid rgba(191,162,0,0.4);}\n" +
                "  .popup-title{font-weight:bold;font-size:14px;color:#BFA200;margin-bottom:4px;}\n" +
                "  .popup-city{font-size:12px;color:rgba(200,200,200,0.7);}\n" +
                "  .popup-price{font-size:13px;color:#27ae60;margin-top:4px;font-weight:600;}\n" +
                "  .map-legend{position:absolute;bottom:20px;right:20px;z-index:1000;background:rgba(10,10,20,0.92);padding:12px 16px;border-radius:10px;border:1px solid rgba(191,162,0,0.3);color:white;font-size:12px;}\n" +
                "  .legend-item{display:flex;align-items:center;margin:4px 0;}\n" +
                "  .legend-dot{width:10px;height:10px;border-radius:50%;margin-right:8px;}\n" +
                "</style>\n" +
                "</head><body>\n" +
                "<div id='map'></div>\n" +
                "<div class='map-legend'>\n" +
                "  <div style='font-weight:bold;color:#BFA200;margin-bottom:6px;'>­ƒôì " + totalMarkers + " emplacements</div>\n" +
                "  <div class='legend-item'><div class='legend-dot' style='background:#27ae60;'></div>Disponible</div>\n" +
                "  <div class='legend-item'><div class='legend-dot' style='background:#c0392b;'></div>Indisponible</div>\n" +
                "  <div class='legend-item'><div class='legend-dot' style='background:#BFA200;'></div>Adresse seule</div>\n" +
                "</div>\n" +
                "<script>\n" +
                "var map = L.map('map',{zoomControl:true}).setView([" + lat + "," + lon + "]," + zoom + ");\n" +
                "L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',{\n" +
                "  maxZoom:19,attribution:'&copy; OpenStreetMap &copy; CARTO'\n" +
                "}).addTo(map);\n" +
                "\n" +
                "function addMarker(lat,lng,name,city,color,price){\n" +
                "  var icon = L.divIcon({\n" +
                "    className:'',\n" +
                "    html:'<div style=\"width:14px;height:14px;border-radius:50%;background:'+color+';border:2px solid rgba(255,255,255,0.7);box-shadow:0 0 10px '+color+',0 0 20px '+color+'44;\"></div>',\n" +
                "    iconSize:[14,14],iconAnchor:[7,7]\n" +
                "  });\n" +
                "  var m = L.marker([lat,lng],{icon:icon}).addTo(map);\n" +
                "  var popupContent = '<div class=\"popup-title\">'+name+'</div>';\n" +
                "  if(city) popupContent += '<div class=\"popup-city\">­ƒôì '+city+'</div>';\n" +
                "  if(price!=='ÔÇö') popupContent += '<div class=\"popup-price\">­ƒÆ░ '+price+' TND</div>';\n" +
                "  m.bindPopup(popupContent,{className:'custom-popup',maxWidth:220});\n" +
                "  L.circle([lat,lng],{radius:800,color:color,fillColor:color,fillOpacity:0.08,weight:1,opacity:0.3}).addTo(map);\n" +
                "}\n" +
                markers +
                "\n" +
                "setTimeout(function(){map.invalidateSize();},300);\n" +
                "setTimeout(function(){map.invalidateSize();},1000);\n" +
                "window.addEventListener('resize',function(){map.invalidateSize();});\n" +
                "</script></body></html>";
    }

    // ==================== HELPERS ====================

    private void styleAxis(Axis<?> x, Axis<?> y) {
        x.setStyle("-fx-tick-label-fill: rgba(255,255,255,0.6); -fx-tick-label-font-size: 10;");
        y.setStyle("-fx-tick-label-fill: rgba(255,255,255,0.6); -fx-tick-label-font-size: 10;");
    }

    private void colorBarsGold(BarChart<?, ?> chart) {
        String[] goldShades = {"#BFA200", "#D4B530", "#8a7000", "#e8dfa0", "#c9a800", "#6b5800"};
        int i = 0;
        for (XYChart.Series<?, ?> s : chart.getData()) {
            for (XYChart.Data<?, ?> data : s.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-bar-fill: " + goldShades[i % goldShades.length] + ";");
                    Tooltip.install(node, new Tooltip(data.getXValue() + ": " + data.getYValue()));
                }
                i++;
            }
        }
    }

    private Label createEmptyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: rgba(200,200,200,0.4); -fx-font-size: 14px; -fx-font-style: italic; -fx-padding: 40 0;");
        lbl.setAlignment(Pos.CENTER);
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private void playEntranceAnimation() {
        if (dashboardRoot == null) return;
        dashboardRoot.setOpacity(0);
        dashboardRoot.setTranslateY(15);
        FadeTransition fade = new FadeTransition(Duration.millis(500), dashboardRoot);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(500), dashboardRoot);
        slide.setFromY(15);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    // ==================== PDF / EXCEL EXPORT ====================

    @FXML
    public void exportDashboardPdf() {
        if (lieux.isEmpty()) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter le rapport PDF");
        fc.setInitialFileName("Rapport_Tableau_de_Bord.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        Stage stage = (Stage) dashboardRoot.getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        new Thread(() -> {
            try {
                PdfExportService pdfService = new PdfExportService();
                pdfService.generateReport(file, lieux, categories, new java.util.ArrayList<>(allAdresses));
                Platform.runLater(() -> {
                    if (lblFilterInfo != null) lblFilterInfo.setText("\u2705 PDF export├®: " + file.getName());
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (lblFilterInfo != null) lblFilterInfo.setText("\u274c Erreur PDF: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void exportDashboardExcel() {
        if (lieux.isEmpty()) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Excel");
        fc.setInitialFileName("Tableau_de_Bord.xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        Stage stage = (Stage) dashboardRoot.getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        new Thread(() -> {
            try {
                ExcelExportService excelService = new ExcelExportService();
                excelService.exportToExcel(file, lieux, categories, new java.util.ArrayList<>(allAdresses));
                Platform.runLater(() -> {
                    if (lblFilterInfo != null) lblFilterInfo.setText("\u2705 Excel export├®: " + file.getName());
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (lblFilterInfo != null) lblFilterInfo.setText("\u274c Erreur Excel: " + e.getMessage());
                });
            }
        }).start();
    }
}

