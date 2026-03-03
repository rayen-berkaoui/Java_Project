package com.esprit.controllers;

import com.esprit.entities.Adresse;
import com.esprit.entities.LieuTouristique;
import com.esprit.entities.categorie;
import com.esprit.services.AdresseServices;
import com.esprit.services.LieuTouristiqueServices;
import com.esprit.services.categorieServices;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced Statistics & Analytics controller.
 * Provides detailed data visualizations beyond the basic dashboard.
 */
public class StatisticsController {

    @FXML private VBox statsRoot;
    @FXML private Label lblAvgPrice, lblMaxPrice, lblMinPrice, lblActiveRate, lblTopCity;
    @FXML private VBox chartPriceHistogram, chartCategoryBar, chartCityPie;
    @FXML private VBox chartAvgPriceCat, chartStatusDonut, chartCatTimeline;
    @FXML private VBox topPlacesBox;

    private LieuTouristiqueServices lieuServices;
    private categorieServices catServices;
    private AdresseServices adresseServices;

    private List<LieuTouristique> lieux = new ArrayList<>();
    private List<categorie> categories = new ArrayList<>();
    private List<Adresse> adresses = new ArrayList<>();

    @FXML
    public void initialize() {
        try {
            lieuServices = new LieuTouristiqueServices();
            catServices = new categorieServices();
            adresseServices = new AdresseServices();
        } catch (Exception e) {
            System.err.println("❌ Statistics: Error initializing services: " + e.getMessage());
        }

        Platform.runLater(() -> {
            loadData();
            buildAllStats();
            playEntranceAnimation();
        });
    }

    @FXML
    public void refreshStats() {
        loadData();
        buildAllStats();
    }

    private void loadData() {
        try {
            lieux = lieuServices.afficher();
            categories = catServices.afficher();
            adresses = adresseServices.afficher();
        } catch (SQLException e) {
            System.err.println("❌ Statistics: Error loading data: " + e.getMessage());
        }
    }

    // ========== BUILD ALL ==========

    private void buildAllStats() {
        updateKPIs();
        buildPriceHistogram();
        buildCategoryBarChart();
        buildCityPieChart();
        buildAvgPriceByCategoryChart();
        buildStatusDonutChart();
        buildCategoryTimelineChart();
        buildTopPlacesTable();
    }

    // ========== KPI CARDS ==========

    private void updateKPIs() {
        if (lieux.isEmpty()) {
            lblAvgPrice.setText("N/A");
            lblMaxPrice.setText("N/A");
            lblMinPrice.setText("N/A");
            lblActiveRate.setText("N/A");
            lblTopCity.setText("-");
            return;
        }

        double avg = lieux.stream().mapToDouble(LieuTouristique::getPrix).average().orElse(0);
        double max = lieux.stream().mapToDouble(LieuTouristique::getPrix).max().orElse(0);
        double min = lieux.stream().mapToDouble(LieuTouristique::getPrix).min().orElse(0);
        long active = lieux.stream().filter(l -> l.getStatut() == 1).count();
        double rate = (double) active / lieux.size() * 100;

        lblAvgPrice.setText(String.format("%.0f TND", avg));
        lblMaxPrice.setText(String.format("%.0f TND", max));
        lblMinPrice.setText(String.format("%.0f TND", min));
        lblActiveRate.setText(String.format("%.0f%%", rate));

        // Top city by count
        Map<String, Long> cityCount = lieux.stream()
                .filter(l -> l.getVille() != null && !l.getVille().isEmpty())
                .collect(Collectors.groupingBy(l -> l.getVille().trim(), Collectors.counting()));
        cityCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(e -> lblTopCity.setText(e.getKey()));

        // Animate KPI values
        animateLabel(lblAvgPrice);
        animateLabel(lblMaxPrice);
        animateLabel(lblMinPrice);
        animateLabel(lblActiveRate);
        animateLabel(lblTopCity);
    }

    private void animateLabel(Label label) {
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

    // ========== PRICE HISTOGRAM ==========

    private void buildPriceHistogram() {
        chartPriceHistogram.getChildren().clear();
        if (lieux.isEmpty()) {
            chartPriceHistogram.getChildren().add(emptyLabel("Aucune donnée disponible"));
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Fourchette de prix (TND)");
        yAxis.setLabel("Nombre de lieux");
        styleAxis(xAxis, yAxis);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(280);
        chart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // Dynamic ranges
        double maxPrice = lieux.stream().mapToDouble(LieuTouristique::getPrix).max().orElse(100);
        int bucketSize = Math.max(10, (int) Math.ceil(maxPrice / 6));
        Map<String, Long> buckets = new LinkedHashMap<>();

        for (int i = 0; i < maxPrice; i += bucketSize) {
            int lo = i;
            int hi = i + bucketSize;
            String range = lo + "-" + hi;
            long count = lieux.stream()
                    .filter(l -> l.getPrix() >= lo && l.getPrix() < hi)
                    .count();
            buckets.put(range, count);
        }

        buckets.forEach((range, count) -> series.getData().add(new XYChart.Data<>(range, count)));
        chart.getData().add(series);

        chartPriceHistogram.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);

        // Color bars gold after render
        Platform.runLater(() -> colorBarsGold(chart));
    }

    // ========== CATEGORY BAR CHART ==========

    private void buildCategoryBarChart() {
        chartCategoryBar.getChildren().clear();
        if (categories.isEmpty()) {
            chartCategoryBar.getChildren().add(emptyLabel("Aucune catégorie"));
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Catégorie");
        yAxis.setLabel("Nombre de lieux");
        styleAxis(xAxis, yAxis);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(280);
        chart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        Map<Integer, String> catNames = categories.stream()
                .collect(Collectors.toMap(categorie::getIdcategorie, categorie::getNomcategorie));

        Map<Integer, Long> catCount = lieux.stream()
                .collect(Collectors.groupingBy(LieuTouristique::getId_categorie, Collectors.counting()));

        catCount.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .forEach(e -> {
                    String name = catNames.getOrDefault(e.getKey(), "ID:" + e.getKey());
                    if (name.length() > 15) name = name.substring(0, 12) + "...";
                    series.getData().add(new XYChart.Data<>(name, e.getValue()));
                });

        chart.getData().add(series);
        chartCategoryBar.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);

        Platform.runLater(() -> colorBarsGold(chart));
    }

    // ========== CITY PIE CHART ==========

    private void buildCityPieChart() {
        chartCityPie.getChildren().clear();
        if (lieux.isEmpty()) {
            chartCityPie.getChildren().add(emptyLabel("Aucune donnée"));
            return;
        }

        Map<String, Long> cityCount = lieux.stream()
                .filter(l -> l.getVille() != null && !l.getVille().isEmpty())
                .collect(Collectors.groupingBy(l -> l.getVille().trim(), Collectors.counting()));

        PieChart chart = new PieChart();
        chart.setAnimated(true);
        chart.setPrefHeight(280);
        chart.setStyle("-fx-background-color: transparent;");

        cityCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> chart.getData().add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue())));

        chartCityPie.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);

        // Color pie slices with gold palette
        Platform.runLater(() -> {
            String[] colors = {"#BFA200", "#D4B530", "#8a7000", "#e8dfa0", "#6b5800",
                    "#c9a800", "#a89000", "#f0e6a0", "#7a6500", "#dfc830"};
            int i = 0;
            for (PieChart.Data data : chart.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-pie-color: " + colors[i % colors.length] + ";");
                    Tooltip.install(node, new Tooltip(data.getName() + ": " + (int) data.getPieValue() + " lieux"));
                }
                i++;
            }
        });
    }

    // ========== AVG PRICE BY CATEGORY ==========

    private void buildAvgPriceByCategoryChart() {
        chartAvgPriceCat.getChildren().clear();
        if (categories.isEmpty() || lieux.isEmpty()) {
            chartAvgPriceCat.getChildren().add(emptyLabel("Aucune donnée"));
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Catégorie");
        yAxis.setLabel("Prix moyen (TND)");
        styleAxis(xAxis, yAxis);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(280);
        chart.setStyle("-fx-background-color: transparent;");

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

    // ========== STATUS DONUT CHART ==========

    private void buildStatusDonutChart() {
        chartStatusDonut.getChildren().clear();
        if (lieux.isEmpty()) {
            chartStatusDonut.getChildren().add(emptyLabel("Aucune donnée"));
            return;
        }

        long active = lieux.stream().filter(l -> l.getStatut() == 1).count();
        long inactive = lieux.size() - active;

        PieChart chart = new PieChart();
        chart.setAnimated(true);
        chart.setPrefHeight(280);
        chart.setStyle("-fx-background-color: transparent;");
        chart.getData().add(new PieChart.Data("Actif (" + active + ")", active));
        chart.getData().add(new PieChart.Data("Inactif (" + inactive + ")", inactive));

        chartStatusDonut.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);

        Platform.runLater(() -> {
            if (chart.getData().size() >= 2) {
                Node activeNode = chart.getData().get(0).getNode();
                Node inactiveNode = chart.getData().get(1).getNode();
                if (activeNode != null) activeNode.setStyle("-fx-pie-color: #00b36b;");
                if (inactiveNode != null) inactiveNode.setStyle("-fx-pie-color: #e63946;");

                Tooltip.install(activeNode, new Tooltip("Actif: " + active + " lieux"));
                Tooltip.install(inactiveNode, new Tooltip("Inactif: " + inactive + " lieux"));
            }
        });
    }

    // ========== CATEGORY TIMELINE ==========

    private void buildCategoryTimelineChart() {
        chartCatTimeline.getChildren().clear();

        List<categorie> dated = categories.stream()
                .filter(c -> c.getDateCreation() != null)
                .sorted(Comparator.comparing(categorie::getDateCreation))
                .collect(Collectors.toList());

        if (dated.isEmpty()) {
            chartCatTimeline.getChildren().add(emptyLabel("Aucune catégorie avec date"));
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Date");
        yAxis.setLabel("Catégories cumulées");
        styleAxis(xAxis, yAxis);

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(280);
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

        // Style line gold
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

    // ========== TOP PLACES TABLE ==========

    private void buildTopPlacesTable() {
        topPlacesBox.getChildren().clear();
        if (lieux.isEmpty()) {
            topPlacesBox.getChildren().add(emptyLabel("Aucun lieu"));
            return;
        }

        Map<Integer, String> catNames = categories.stream()
                .collect(Collectors.toMap(categorie::getIdcategorie, categorie::getNomcategorie));

        // Header
        HBox header = createTableRow("🏅", "Nom", "Ville", "Catégorie", "Prix", true);
        topPlacesBox.getChildren().add(header);

        List<LieuTouristique> top5 = lieux.stream()
                .sorted(Comparator.comparingDouble(LieuTouristique::getPrix).reversed())
                .limit(5)
                .collect(Collectors.toList());

        int rank = 1;
        for (LieuTouristique l : top5) {
            String medal;
            switch (rank) {
                case 1: medal = "🥇"; break;
                case 2: medal = "🥈"; break;
                case 3: medal = "🥉"; break;
                default: medal = "#" + rank;
            }
            String catName = catNames.getOrDefault(l.getId_categorie(), "-");
            HBox row = createTableRow(medal, l.getNom(), l.getVille() != null ? l.getVille() : "-",
                    catName, String.format("%.0f TND", l.getPrix()), false);

            // Entrance animation
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

    // ========== HELPERS ==========

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

    private Label emptyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 14px; -fx-font-style: italic;");
        return lbl;
    }

    private void playEntranceAnimation() {
        if (statsRoot == null) return;
        for (int i = 0; i < statsRoot.getChildren().size(); i++) {
            Node child = statsRoot.getChildren().get(i);
            child.setOpacity(0);
            child.setTranslateY(30);

            FadeTransition fade = new FadeTransition(Duration.millis(500), child);
            fade.setDelay(Duration.millis(i * 80));
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(500), child);
            slide.setDelay(Duration.millis(i * 80));
            slide.setFromY(30);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fade, slide).play();
        }
    }
}
