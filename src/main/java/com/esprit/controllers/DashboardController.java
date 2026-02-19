package com.esprit.controllers;

import com.esprit.entities.Adresse;
import com.esprit.entities.LieuTouristique;
import com.esprit.entities.categorie;
import com.esprit.services.AdresseServices;
import com.esprit.services.LieuTouristiqueServices;
import com.esprit.services.categorieServices;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Worker;
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
import javafx.scene.shape.Circle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label lblTotalLieux, lblTotalCategories, lblTotalAdresses, lblTotalVilles;
    @FXML private VBox chartCategorieBox, chartVilleBox, chartPriceBox, chartStatutBox;
    @FXML private WebView heatmapWebView;
    @FXML private VBox dashboardRoot;

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
            System.err.println("❌ Dashboard: Error initializing services: " + e.getMessage());
        }

        Platform.runLater(() -> {
            loadData();
            buildDashboard();
            playEntranceAnimation();
        });
    }

    private void loadData() {
        try {
            lieux = lieuServices.afficher();
            categories = catServices.afficher();
            adresses = adresseServices.afficher();
        } catch (SQLException e) {
            System.err.println("❌ Dashboard: Error loading data: " + e.getMessage());
        }
    }

    private void buildDashboard() {
        updateStatCards();
        buildCategoryPieChart();
        buildVilleBarChart();
        buildPriceDistributionChart();
        buildStatutPieChart();
        buildHeatmap();
    }

    // ==================== STAT CARDS ====================

    private void updateStatCards() {
        if (lblTotalLieux != null) lblTotalLieux.setText(String.valueOf(lieux.size()));
        if (lblTotalCategories != null) lblTotalCategories.setText(String.valueOf(categories.size()));
        if (lblTotalAdresses != null) lblTotalAdresses.setText(String.valueOf(adresses.size()));
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

    // ==================== CHART: Lieux per Catégorie (PieChart) ====================

    private void buildCategoryPieChart() {
        if (chartCategorieBox == null) return;
        chartCategorieBox.getChildren().clear();

        // Map category id -> name
        Map<Integer, String> catNames = new HashMap<>();
        for (categorie c : categories) catNames.put(c.getIdcategorie(), c.getNomcategorie());

        // Count lieux per category
        Map<String, Integer> catCounts = new LinkedHashMap<>();
        for (LieuTouristique l : lieux) {
            String name = catNames.getOrDefault(l.getId_categorie(), "Inconnue");
            catCounts.merge(name, 1, Integer::sum);
        }

        if (catCounts.isEmpty()) {
            chartCategorieBox.getChildren().add(createEmptyLabel("Aucune donnée disponible"));
            return;
        }

        PieChart pieChart = new PieChart();
        pieChart.setTitle(null);
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(true);
        pieChart.setStartAngle(90);
        pieChart.setMinHeight(300);
        pieChart.setPrefHeight(320);
        pieChart.setMaxHeight(350);

        for (Map.Entry<String, Integer> entry : catCounts.entrySet()) {
            PieChart.Data data = new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            pieChart.getData().add(data);
        }

        pieChart.getStyleClass().add("dashboard-pie-chart");
        chartCategorieBox.getChildren().add(pieChart);

        // Tooltips
        Platform.runLater(() -> {
            int total = catCounts.values().stream().mapToInt(Integer::intValue).sum();
            for (PieChart.Data d : pieChart.getData()) {
                double pct = (d.getPieValue() / total) * 100.0;
                Tooltip tooltip = new Tooltip(d.getName() + "\n" + String.format("%.1f%%", pct));
                tooltip.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
                Tooltip.install(d.getNode(), tooltip);
                d.getNode().setOnMouseEntered(e -> d.getNode().setStyle("-fx-opacity: 0.8; -fx-cursor: hand;"));
                d.getNode().setOnMouseExited(e -> d.getNode().setStyle("-fx-opacity: 1;"));
            }
        });
    }

    // ==================== CHART: Lieux per Ville (BarChart) ====================

    private void buildVilleBarChart() {
        if (chartVilleBox == null) return;
        chartVilleBox.getChildren().clear();

        Map<String, Integer> villeCounts = new LinkedHashMap<>();
        for (LieuTouristique l : lieux) {
            String v = l.getVille() != null ? l.getVille() : "Inconnue";
            villeCounts.merge(v, 1, Integer::sum);
        }

        if (villeCounts.isEmpty()) {
            chartVilleBox.getChildren().add(createEmptyLabel("Aucune donnée disponible"));
            return;
        }

        // Sort by count descending, take top 10
        List<Map.Entry<String, Integer>> sorted = villeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Ville");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Nombre de lieux");
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(null);
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(15);
        barChart.setBarGap(3);
        barChart.setMinHeight(300);
        barChart.setPrefHeight(320);
        barChart.setMaxHeight(350);
        barChart.setAnimated(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lieux");
        for (Map.Entry<String, Integer> entry : sorted) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        barChart.getData().add(series);
        barChart.getStyleClass().add("dashboard-bar-chart");
        chartVilleBox.getChildren().add(barChart);

        // Tooltips for bars
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    Tooltip tooltip = new Tooltip(d.getXValue() + ": " + d.getYValue() + " lieux");
                    tooltip.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
                    Tooltip.install(d.getNode(), tooltip);
                    d.getNode().setOnMouseEntered(e -> d.getNode().setStyle("-fx-opacity: 0.75;"));
                    d.getNode().setOnMouseExited(e -> d.getNode().setStyle("-fx-opacity: 1;"));
                }
            }
        });
    }

    // ==================== CHART: Price Distribution (BarChart) ====================

    private void buildPriceDistributionChart() {
        if (chartPriceBox == null) return;
        chartPriceBox.getChildren().clear();

        if (lieux.isEmpty()) {
            chartPriceBox.getChildren().add(createEmptyLabel("Aucune donnée disponible"));
            return;
        }

        // Define price ranges
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
        xAxis.setLabel("Tranche de prix");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Nombre de lieux");
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(null);
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(12);
        barChart.setBarGap(2);
        barChart.setMinHeight(300);
        barChart.setPrefHeight(320);
        barChart.setMaxHeight(350);
        barChart.setAnimated(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Prix");
        for (int i = 0; i < rangeLabels.length; i++) {
            series.getData().add(new XYChart.Data<>(rangeLabels[i], rangeCounts[i]));
        }
        barChart.getData().add(series);
        barChart.getStyleClass().add("dashboard-bar-chart");
        chartPriceBox.getChildren().add(barChart);

        // Tooltips
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    Tooltip tooltip = new Tooltip(d.getXValue() + ": " + d.getYValue() + " lieux");
                    tooltip.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
                    Tooltip.install(d.getNode(), tooltip);
                }
            }
        });
    }

    // ==================== CHART: Disponible vs Indisponible (PieChart) ====================

    private void buildStatutPieChart() {
        if (chartStatutBox == null) return;
        chartStatutBox.getChildren().clear();

        final int available, unavailable;
        int av = 0, unav = 0;
        for (LieuTouristique l : lieux) {
            if (l.getStatut() == 1) av++;
            else unav++;
        }
        available = av;
        unavailable = unav;

        if (lieux.isEmpty()) {
            chartStatutBox.getChildren().add(createEmptyLabel("Aucune donnée disponible"));
            return;
        }

        PieChart pieChart = new PieChart();
        pieChart.setTitle(null);
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(true);
        pieChart.setStartAngle(90);
        pieChart.setMinHeight(300);
        pieChart.setPrefHeight(320);
        pieChart.setMaxHeight(350);

        PieChart.Data avail = new PieChart.Data("Disponible (" + available + ")", available);
        PieChart.Data unavail = new PieChart.Data("Indisponible (" + unavailable + ")", unavailable);
        pieChart.getData().addAll(avail, unavail);
        pieChart.getStyleClass().add("dashboard-pie-chart");
        chartStatutBox.getChildren().add(pieChart);

        // Style and tooltips
        Platform.runLater(() -> {
            if (avail.getNode() != null) avail.getNode().setStyle("-fx-pie-color: #27ae60;");
            if (unavail.getNode() != null) unavail.getNode().setStyle("-fx-pie-color: #c0392b;");

            int total = available + unavailable;
            for (PieChart.Data d : pieChart.getData()) {
                double pct = total > 0 ? (d.getPieValue() / total) * 100.0 : 0;
                Tooltip tooltip = new Tooltip(d.getName() + "\n" + String.format("%.1f%%", pct));
                tooltip.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
                Tooltip.install(d.getNode(), tooltip);
            }
        });
    }

    // ==================== HEATMAP: All locations on map ====================

    private void buildHeatmap() {
        if (heatmapWebView == null) return;

        // Gather locations with coordinates
        Map<Integer, Adresse> adresseMap = new HashMap<>();
        for (Adresse a : adresses) adresseMap.put(a.getId_adresse(), a);

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

        // Also add adresses that are not linked to any lieu
        for (Adresse a : adresses) {
            if (a.getLatitude() != 0 && a.getLongitude() != 0) {
                boolean linked = lieux.stream().anyMatch(l -> l.getId_adresse() == a.getId_adresse());
                if (!linked) {
                    markers.append(String.format(
                            "addMarker(%f, %f, '%s', '%s', '#BFA200', '—');\n",
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
                System.out.println("✅ Dashboard heatmap loaded with " + markerCount + " markers");
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
                "  <div style='font-weight:bold;color:#BFA200;margin-bottom:6px;'>📍 " + totalMarkers + " emplacements</div>\n" +
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
                "  if(city) popupContent += '<div class=\"popup-city\">📍 '+city+'</div>';\n" +
                "  if(price!=='—') popupContent += '<div class=\"popup-price\">💰 '+price+' TND</div>';\n" +
                "  m.bindPopup(popupContent,{className:'custom-popup',maxWidth:220});\n" +
                "  // Pulse animation circle\n" +
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
}
