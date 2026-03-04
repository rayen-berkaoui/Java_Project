package com.esprit.controllers;

import com.esprit.entities.Etablissement;
import com.esprit.services.EtablissementServices;
import com.esprit.services.GooglePlacesService;
import com.esprit.utils.ThemeManager;
import com.esprit.services.GooglePlacesService.PlaceResult;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the "Recommandation à Proximité" page.
 * Features a Leaflet map for location picking + a TableView for results.
 * Queries the Overpass API (real OSM data) + local DB, merges and deduplicates.
 */
public class RecommandationPlacesController {

    // ── FXML bindings ──
    @FXML private StackPane rootStack;
    @FXML private Label statusLabel;
    @FXML private WebView mapWebView;
    @FXML private Label latLabel;
    @FXML private Label lngLabel;
    @FXML private ComboBox<String> etabCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Slider radiusSlider;
    @FXML private Label radiusLabel;
    @FXML private Button searchBtn;
    @FXML private HBox selectedInfoBox;
    @FXML private Label selectedInfoLabel;
    @FXML private Label resultsTitle;
    @FXML private Label countLabel;
    @FXML private TableView<NearbyRow> resultsTable;
    @FXML private TableColumn<NearbyRow, String> colRank;
    @FXML private TableColumn<NearbyRow, String> colName;
    @FXML private TableColumn<NearbyRow, String> colType;
    @FXML private TableColumn<NearbyRow, String> colDistance;
    @FXML private TableColumn<NearbyRow, String> colAddress;
    @FXML private TableColumn<NearbyRow, String> colPhone;
    @FXML private TableColumn<NearbyRow, String> colHours;
    @FXML private TableColumn<NearbyRow, String> colSource;

    // ── Services ──
    private final GooglePlacesService placesService = new GooglePlacesService();
    private final EtablissementServices etabService = new EtablissementServices();

    // ── State ──
    private Double selectedLat = null;
    private Double selectedLng = null;
    private Etablissement selectedEtab = null;
    private List<Etablissement> allEtablissements = new ArrayList<>();
    private final Map<String, Etablissement> etabByName = new LinkedHashMap<>();
    private final ObservableList<NearbyRow> tableData = FXCollections.observableArrayList();

    /** Strong reference to prevent GC of the JS bridge */
    private MapBridge mapBridge;

    // ═══════════════════════════════════════════
    //  TABLE ROW MODEL
    // ═══════════════════════════════════════════

    public static class NearbyRow {
        private final String rank;
        private final String name;
        private final String type;
        private final double distanceKm;
        private final String distanceText;
        private final String address;
        private final String phone;
        private final String hours;
        private final String source;
        private final double lat;
        private final double lng;

        public NearbyRow(int rank, String name, String type, double distanceKm,
                         String address, String phone, String hours, String source,
                         double lat, double lng) {
            this.rank = String.valueOf(rank);
            this.name = name;
            this.type = type;
            this.distanceKm = distanceKm;
            this.distanceText = String.format("%.2f km", distanceKm);
            this.address = address;
            this.phone = phone;
            this.hours = hours;
            this.source = source;
            this.lat = lat;
            this.lng = lng;
        }

        public String getRank()         { return rank; }
        public String getName()         { return name; }
        public String getType()         { return type; }
        public double getDistanceKm()   { return distanceKm; }
        public String getDistanceText() { return distanceText; }
        public String getAddress()      { return address; }
        public String getPhone()        { return phone; }
        public String getHours()        { return hours; }
        public String getSource()       { return source; }
        public double getLat()          { return lat; }
        public double getLng()          { return lng; }
    }

    // ═══════════════════════════════════════════
    //  INITIALIZATION
    // ═══════════════════════════════════════════

    @FXML
    public void initialize() {
        setupTableColumns();
        setupTypeCombo();
        setupRadiusSlider();
        loadEtablissements();
        initMap();

        Platform.runLater(() -> ChatbotPanel.install(rootStack));
    }

    private void setupTableColumns() {
        colRank.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRank()));
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));
        colType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType()));
        colDistance.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDistanceText()));
        colAddress.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getAddress()));
        colPhone.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPhone()));
        colHours.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getHours()));
        colSource.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSource()));

        colDistance.setComparator((a, b) -> {
            try {
                double da = Double.parseDouble(a.replace(" km", "").replace(",", "."));
                double db = Double.parseDouble(b.replace(" km", "").replace(",", "."));
                return Double.compare(da, db);
            } catch (Exception e) { return a.compareTo(b); }
        });

        resultsTable.setItems(tableData);
    }

    private void setupTypeCombo() {
        typeCombo.setItems(FXCollections.observableArrayList(
                "Tous les types",
                "🍽️ Restaurant", "🏨 Hôtel", "☕ Café", "🏛️ Musée",
                "🍺 Bar", "🌳 Parc", "🧖 Spa / Hammam",
                "💊 Pharmacie", "🛍️ Shopping", "📸 Attraction touristique"
        ));
        typeCombo.setValue("Tous les types");
    }

    private void setupRadiusSlider() {
        radiusSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double km = newVal.doubleValue() / 1000.0;
            radiusLabel.setText(String.format("%.1f km", km));
        });
    }

    private void loadEtablissements() {
        try {
            allEtablissements = etabService.afficher();
        } catch (SQLException e) {
            allEtablissements = new ArrayList<>();
        }

        etabByName.clear();
        List<String> names = new ArrayList<>();
        names.add("— Choisir —");
        for (Etablissement etab : allEtablissements) {
            if (etab.getLatitude() != null && etab.getLongitude() != null) {
                String label = etab.getNom() + " (" + safe(etab.getVille()) + ")";
                etabByName.put(label, etab);
                names.add(label);
            }
        }
        etabCombo.setItems(FXCollections.observableArrayList(names));
        etabCombo.setValue("— Choisir —");

        etabCombo.setOnAction(e -> {
            String selected = etabCombo.getValue();
            if (selected != null && etabByName.containsKey(selected)) {
                Etablissement etab = etabByName.get(selected);
                selectedLat = etab.getLatitude();
                selectedLng = etab.getLongitude();
                selectedEtab = etab;
                updateCoordsDisplay();
                showSelectedInfo(etab);
                centerMapOn(selectedLat, selectedLng);
                doSearch();
            } else {
                selectedLat = null;
                selectedLng = null;
                selectedEtab = null;
                selectedInfoBox.setVisible(false);
                selectedInfoBox.setManaged(false);
            }
        });
    }

    // ═══════════════════════════════════════════
    //  MAP
    // ═══════════════════════════════════════════

    private void initMap() {
        if (mapWebView == null) return;
        WebEngine engine = mapWebView.getEngine();
        engine.setJavaScriptEnabled(true);

        String mapUrl = getClass().getResource("/map_picker.html").toExternalForm();
        engine.load(mapUrl);

        mapBridge = new MapBridge();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", mapBridge);
                engine.executeScript("setTimeout(function(){ map.invalidateSize(); }, 300);");
            }
        });
    }

    /** JS → Java bridge: called when user clicks the map */
    public class MapBridge {
        public void onLocationSelected(double lat, double lng) {
            Platform.runLater(() -> {
                selectedLat = lat;
                selectedLng = lng;
                selectedEtab = null; // free click, not a DB establishment
                updateCoordsDisplay();
                // Clear the combo selection since user clicked on map
                etabCombo.setValue("— Choisir —");
                selectedInfoBox.setVisible(false);
                selectedInfoBox.setManaged(false);
                doSearch();
            });
        }
    }

    private void updateCoordsDisplay() {
        if (selectedLat != null && selectedLng != null) {
            latLabel.setText(String.format("%.6f", selectedLat));
            lngLabel.setText(String.format("%.6f", selectedLng));
        }
    }

    private void centerMapOn(double lat, double lng) {
        if (mapWebView != null) {
            WebEngine engine = mapWebView.getEngine();
            try {
                engine.executeScript(String.format("setPosition(%f, %f)", lat, lng));
            } catch (Exception ignored) {}
        }
    }

    private void showRadiusOnMap(double lat, double lng, int radiusMeters) {
        if (mapWebView == null) return;
        try {
            mapWebView.getEngine().executeScript(String.format(
                    "clearResults(); showSearchRadius(%f, %f, %d);", lat, lng, radiusMeters));
        } catch (Exception ignored) {}
    }

    private void pushMarkersToMap(List<NearbyRow> results) {
        if (mapWebView == null) return;
        WebEngine engine = mapWebView.getEngine();
        try {
            engine.executeScript(
                    "for(var i=0;i<resultMarkers.length;i++){map.removeLayer(resultMarkers[i]);}resultMarkers=[];");
            for (NearbyRow row : results) {
                if (row.getLat() == 0 && row.getLng() == 0) continue;
                boolean isDb = row.getSource().contains("Base");
                engine.executeScript(String.format(
                        "addResultMarker(%f, %f, %s, %s, %f, %s, 3.5, %s, %s);",
                        row.getLat(), row.getLng(),
                        jsString(row.getName()),
                        jsString(row.getType()),
                        row.getDistanceKm(),
                        jsString(row.getAddress()),
                        jsString(safe(row.getPhone()) + (row.getHours().isEmpty() ? "" : " · " + row.getHours())),
                        isDb ? "true" : "false"));
            }
            engine.executeScript("fitResultBounds();");
        } catch (Exception e) {
            System.err.println("Error pushing markers: " + e.getMessage());
        }
    }

    private void showSelectedInfo(Etablissement etab) {
        StringBuilder sb = new StringBuilder();
        sb.append("📍 ").append(safe(etab.getNom()));
        if (etab.getType() != null) sb.append("  •  ").append(etab.getType());
        if (etab.getVille() != null) sb.append("  •  🏙️ ").append(etab.getVille());
        if (etab.getAdresse() != null) sb.append("  •  📫 ").append(etab.getAdresse());
        selectedInfoLabel.setText(sb.toString());
        selectedInfoBox.setVisible(true);
        selectedInfoBox.setManaged(true);
    }

    // ═══════════════════════════════════════════
    //  SEARCH
    // ═══════════════════════════════════════════

    @FXML
    private void onSearch(ActionEvent event) {
        doSearch();
    }

    private void doSearch() {
        if (selectedLat == null || selectedLng == null) {
            statusLabel.setText("⚠️ Cliquez sur la carte ou sélectionnez un établissement.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        String selectedType = extractType(typeCombo.getValue());
        int radius = (int) radiusSlider.getValue();

        statusLabel.setText("🔍 Recherche en cours…");
        statusLabel.setStyle("-fx-text-fill: #FFC107;");
        searchBtn.setDisable(true);
        tableData.clear();
        resultsTitle.setText("");
        countLabel.setText("");

        // Show radius circle on map
        showRadiusOnMap(selectedLat, selectedLng, radius);

        final double lat = selectedLat;
        final double lng = selectedLng;

        new Thread(() -> {
            System.out.println("[Nearby] Searching at (" + lat + ", " + lng
                    + ") radius=" + radius + "m type=" + selectedType);

            // 1) Query Overpass API (REAL places from OpenStreetMap)
            String[] apiError = {null};
            List<NearbyRow> apiRows = getOverpassNearbyRows(lat, lng, radius, selectedType, apiError);
            System.out.println("[Nearby] Overpass API results: " + apiRows.size());

            // 1b) Fallback: if filtered query returns 0, retry with all types
            boolean usedFallback = false;
            if (apiRows.isEmpty() && apiError[0] == null && selectedType != null) {
                System.out.println("[Nearby] 0 results with type filter → retrying ALL types...");
                apiRows = getOverpassNearbyRows(lat, lng, radius, null, apiError);
                usedFallback = !apiRows.isEmpty();
                System.out.println("[Nearby] Fallback results: " + apiRows.size());
            }

            // 2) Query local DB
            List<NearbyRow> dbRows = getDbNearbyRows(lat, lng, radius, selectedType);
            System.out.println("[Nearby] DB results: " + dbRows.size());

            // 3) Merge & deduplicate
            List<NearbyRow> merged = mergeAndDeduplicate(dbRows, apiRows);
            merged.sort(Comparator.comparingDouble(NearbyRow::getDistanceKm));

            double radiusKm = radius / 1000.0;
            int total = merged.size();
            final boolean fallback = usedFallback;
            final List<NearbyRow> finalMerged = merged;

            Platform.runLater(() -> {
                searchBtn.setDisable(false);
                tableData.setAll(finalMerged);

                resultsTitle.setText("📍 Résultats dans un rayon de " + String.format("%.1f", radiusKm) + " km");
                countLabel.setText(total + " lieu(x) trouvé(s)");

                // Push markers to map
                pushMarkersToMap(finalMerged);

                if (total > 0) {
                    long dbCount = finalMerged.stream().filter(r -> r.getSource().contains("Base")).count();
                    long apiCount = finalMerged.stream().filter(r -> r.getSource().contains("OpenStreetMap")).count();
                    String src = " (" + dbCount + " base locale + " + apiCount + " OpenStreetMap)";
                    if (fallback) {
                        src += " — filtre ignoré (aucun « " + typeCombo.getValue() + " »)";
                    }
                    if (apiError[0] != null) {
                        src += " ⚠️ API: " + apiError[0];
                    }
                    statusLabel.setText("✅ " + total + " lieu(x) trouvé(s)" + src);
                    statusLabel.setStyle("-fx-text-fill: #2ecc71;");
                } else {
                    String msg = "⚠️ Aucun lieu trouvé à ces coordonnées ("
                            + String.format("%.4f, %.4f", lat, lng)
                            + "). Essayez de cliquer ailleurs sur la carte.";
                    if (apiError[0] != null) {
                        msg = "❌ Erreur API : " + apiError[0];
                    }
                    statusLabel.setText(msg);
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                }
            });
        }).start();
    }

    // ═══════════════════════════════════════════
    //  DATA FETCHERS
    // ═══════════════════════════════════════════

    private List<NearbyRow> getDbNearbyRows(double lat, double lng, int radiusMeters, String type) {
        double radiusKm = radiusMeters / 1000.0;
        List<NearbyRow> rows = new ArrayList<>();
        for (Etablissement etab : allEtablissements) {
            if (etab.getLatitude() == null || etab.getLongitude() == null) continue;
            if (selectedEtab != null && etab.getIdEtablissement() == selectedEtab.getIdEtablissement()) continue;
            if (type != null && !safe(etab.getType()).toLowerCase().contains(type.toLowerCase())) continue;
            double dist = GooglePlacesService.haversineKm(lat, lng, etab.getLatitude(), etab.getLongitude());
            if (dist > radiusKm) continue;
            rows.add(new NearbyRow(0, safe(etab.getNom()), safe(etab.getType()), dist,
                    safe(etab.getAdresse()), safe(etab.getTelephone()), safe(etab.getHoraires()),
                    "🗄️ Base locale", etab.getLatitude(), etab.getLongitude()));
        }
        return rows;
    }

    private List<NearbyRow> getOverpassNearbyRows(double lat, double lng, int radiusMeters, String type, String[] apiError) {
        List<NearbyRow> rows = new ArrayList<>();
        try {
            List<PlaceResult> results;
            if (type == null) {
                results = placesService.searchAllNearby(lat, lng, radiusMeters);
            } else {
                results = placesService.searchNearby(lat, lng, radiusMeters, type);
            }
            for (PlaceResult pr : results) {
                String typeName = (pr.types != null && !pr.types.isEmpty())
                        ? GooglePlacesService.typeEmoji(pr.types.get(0)) + " " + pr.types.get(0)
                        : "—";
                rows.add(new NearbyRow(0, safe(pr.name), typeName, pr.distanceKm,
                        safe(pr.address), safe(pr.phone), safe(pr.openingHours),
                        "🌐 OpenStreetMap", pr.lat, pr.lng));
            }
        } catch (IOException e) {
            System.err.println("Overpass API error: " + e.getMessage());
            apiError[0] = e.getMessage();
        }
        return rows;
    }

    private List<NearbyRow> mergeAndDeduplicate(List<NearbyRow> dbRows, List<NearbyRow> apiRows) {
        List<NearbyRow> merged = new ArrayList<>(dbRows);
        for (NearbyRow api : apiRows) {
            boolean isDuplicate = false;
            for (NearbyRow db : dbRows) {
                if (namesMatch(db.getName(), api.getName())
                        && Math.abs(db.getDistanceKm() - api.getDistanceKm()) < 0.1) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) merged.add(api);
        }

        merged.sort(Comparator.comparingDouble(NearbyRow::getDistanceKm));
        List<NearbyRow> ranked = new ArrayList<>();
        for (int i = 0; i < merged.size(); i++) {
            NearbyRow r = merged.get(i);
            ranked.add(new NearbyRow(i + 1, r.getName(), r.getType(), r.getDistanceKm(),
                    r.getAddress(), r.getPhone(), r.getHours(), r.getSource(),
                    r.getLat(), r.getLng()));
        }
        return ranked;
    }

    private boolean namesMatch(String a, String b) {
        if (a == null || b == null) return false;
        String la = a.toLowerCase().trim();
        String lb = b.toLowerCase().trim();
        return la.equals(lb) || la.contains(lb) || lb.contains(la);
    }

    // ═══════════════════════════════════════════
    //  TYPE EXTRACTION
    // ═══════════════════════════════════════════

    private String extractType(String comboValue) {
        if (comboValue == null || comboValue.equals("Tous les types")) return null;
        String clean = comboValue.replaceAll("^[^\\p{L}]+", "").trim().toLowerCase();
        return switch (clean) {
            case "restaurant"             -> "restaurant";
            case "hôtel"                  -> "hotel";
            case "café"                   -> "cafe";
            case "musée"                  -> "museum";
            case "bar"                    -> "bar";
            case "parc"                   -> "park";
            case "spa / hammam"           -> "spa";
            case "pharmacie"              -> "pharmacy";
            case "shopping"               -> "shopping";
            case "attraction touristique" -> "tourist";
            default                       -> null;
        };
    }

    // ═══════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════

    @FXML private void goHome(ActionEvent event)             { NavigationUtils.goTo("/home.fxml", event); }
    @FXML private void goTableau(ActionEvent event)          { NavigationUtils.goTo("/etablissement_tableau.fxml", event); }
    @FXML private void goEtablissements(ActionEvent event)   { NavigationUtils.goTo("/etablissement_affichage.fxml", event); }
    @FXML private void goActivites(ActionEvent event)        { NavigationUtils.goTo("/activite_affichage.fxml", event); }
    @FXML private void goTableauActivites(ActionEvent event) { NavigationUtils.goTo("/activite_tableau.fxml", event); }

    // ═══════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════

    private String safe(String s) { return s == null ? "" : s; }

    private String jsString(String s) {
        if (s == null || s.isEmpty()) return "''";
        String escaped = s.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\n", " ").replace("\r", "");
        return "'" + escaped + "'";
    }

    @FXML
    private void toggleTheme(ActionEvent event) {
        ThemeManager.handleToggleTheme(event);
    }
}
