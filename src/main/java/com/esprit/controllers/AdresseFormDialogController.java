package com.esprit.controllers;

import com.esprit.entities.Adresse;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * Controller for Adresse form dialog.
 * Two input modes:
 *   - MANUAL: user fills in all fields by hand
 *   - MAP: user clicks/searches on an interactive map; all fields are auto-filled
 */
public class AdresseFormDialogController {

    // --- Manual mode fields ---
    @FXML private TextField tfRue, tfVille, tfLatitude, tfLongitude, tfAltitude;
    @FXML private Label lblErrRue, lblErrVille, lblErrLat, lblErrLon, lblErrAlt;


    // --- Map mode fields ---
    @FXML private WebView mapWebView;
    @FXML private TextField tfSearch;
    @FXML private Button btnSearchMap;
    @FXML private Label lblSelectedPos;
    @FXML private Label lblMapRue, lblMapVille, lblMapLat, lblMapLon, lblMapAlt;

    // --- Panels & toggle ---
    @FXML private VBox manualPanel;
    @FXML private VBox mapPanel;
    @FXML private Button btnModeManual, btnModeMap;
    @FXML private Label lblSubtitle;

    private WebEngine mapEngine;          // full interactive map (map mode)
    private String mode = "ADD";          // ADD or EDIT
    private boolean mapReady = false;
    private boolean isMapInputMode = false;

    // Data collected from map mode clicks
    private String mapRue = "", mapVille = "";
    private double mapLat = 0, mapLon = 0, mapAlt = 0;
    private boolean mapPositionSelected = false;

    @FXML
    public void initialize() {
        // --- Mode toggle buttons ---
        if (btnModeManual != null) btnModeManual.setOnAction(e -> switchToManualMode());
        if (btnModeMap != null) btnModeMap.setOnAction(e -> switchToMapMode());

        // --- Interactive map (map mode) ---
        if (mapWebView != null) {
            mapEngine = mapWebView.getEngine();
            mapEngine.setJavaScriptEnabled(true);
            mapEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    JSObject window = (JSObject) mapEngine.executeScript("window");
                    window.setMember("javaBridge", new MapBridge());
                    mapEngine.executeScript("console.log = function(msg) { javaBridge.log(msg); };");
                    mapReady = true;
                }
            });
            mapEngine.loadContent(getMapHtml(36.8065, 10.1815, 13));
        }

        // --- Search button ---
        if (btnSearchMap != null) btnSearchMap.setOnAction(e -> searchLocation());
        if (tfSearch != null) tfSearch.setOnAction(e -> searchLocation());

        // --- Validation listeners ---
        setupValidationListeners();
    }

    // ========== MODE SWITCHING ==========

    private void switchToManualMode() {
        isMapInputMode = false;
        manualPanel.setVisible(true); manualPanel.setManaged(true);
        mapPanel.setVisible(false); mapPanel.setManaged(false);

        btnModeManual.getStyleClass().add("mode-toggle-active");
        btnModeMap.getStyleClass().remove("mode-toggle-active");

        if (lblSubtitle != null) lblSubtitle.setText("Remplissez les champs manuellement");
    }

    private void switchToMapMode() {
        isMapInputMode = true;
        manualPanel.setVisible(false); manualPanel.setManaged(false);
        mapPanel.setVisible(true); mapPanel.setManaged(true);

        btnModeMap.getStyleClass().add("mode-toggle-active");
        btnModeManual.getStyleClass().remove("mode-toggle-active");

        if (lblSubtitle != null) lblSubtitle.setText("Cliquez sur la carte pour choisir votre position");

        // If editing, center map on existing position
        Platform.runLater(() -> {
            if (mapReady && mapEngine != null && mapPositionSelected) {
                mapEngine.executeScript(String.format(
                    "updateMarker(%f, %f); map.setView([%f, %f], 15);",
                    mapLat, mapLon, mapLat, mapLon
                ));
            }
        });
    }

    // ========== PUBLIC API ==========

    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Populates fields with existing address for editing.
     */
    public void setAdresse(Adresse a) {
        if (a == null) return;

        // Fill manual fields
        if (tfRue != null) tfRue.setText(a.getRue() != null ? a.getRue() : "");
        if (tfVille != null) tfVille.setText(a.getVille() != null ? a.getVille() : "");
        if (tfLatitude != null) tfLatitude.setText(String.valueOf(a.getLatitude()));
        if (tfLongitude != null) tfLongitude.setText(String.valueOf(a.getLongitude()));
        if (tfAltitude != null) tfAltitude.setText(String.valueOf(a.getAltitude()));

        // Store for map mode too
        mapRue = a.getRue() != null ? a.getRue() : "";
        mapVille = a.getVille() != null ? a.getVille() : "";
        mapLat = a.getLatitude();
        mapLon = a.getLongitude();
        mapAlt = a.getAltitude();
        mapPositionSelected = true;
        updateMapSummaryLabels();

        // Center map
        Platform.runLater(() -> {
            if (mapReady && mapEngine != null) {
                mapEngine.executeScript(String.format(
                    "updateMarker(%f, %f); map.setView([%f, %f], 15);",
                    a.getLatitude(), a.getLongitude(), a.getLatitude(), a.getLongitude()
                ));
            }
        });
    }

    /**
     * Extracts and validates the address from whichever mode is active.
     */
    public Adresse getAdresse() {
        if (isMapInputMode) {
            return getAdresseFromMap();
        } else {
            return getAdresseFromManual();
        }
    }

    // ========== MAP MODE: getAdresse ==========

    private Adresse getAdresseFromMap() {
        if (!mapPositionSelected) {
            if (lblSelectedPos != null)
                lblSelectedPos.setText("⚠ Cliquez sur la carte pour choisir une position !");
            lblSelectedPos.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");
            return null;
        }

        // Validate that we have minimum data
        if (mapRue.isEmpty() && mapVille.isEmpty()) {
            if (lblSelectedPos != null) {
                lblSelectedPos.setText("⚠ Position trouvée mais adresse non résolue — réessayez");
                lblSelectedPos.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
            // Still allow — coordinates are valid even if reverse geocode returned nothing
        }

        String rue = mapRue.isEmpty() ? "Position sur carte" : mapRue;
        String ville = mapVille.isEmpty() ? "Inconnu" : mapVille;

        return new Adresse(rue, ville, mapLat, mapLon, mapAlt);
    }

    // ========== MANUAL MODE: getAdresse with validation ==========

    private Adresse getAdresseFromManual() {
        if (tfRue == null || tfVille == null || tfLatitude == null || tfLongitude == null || tfAltitude == null) {
            return null;
        }

        boolean valid = true;

        // --- Rue ---
        String rue = tfRue.getText().trim();
        if (rue.isEmpty()) {
            showFieldError(tfRue, lblErrRue, "La rue est obligatoire"); valid = false;
        } else if (rue.length() < 2) {
            showFieldError(tfRue, lblErrRue, "Minimum 2 caractères"); valid = false;
        } else if (!rue.matches("[a-zA-ZÀ-ÿ0-9\\s\\-'.]+")) {
            showFieldError(tfRue, lblErrRue, "Caractères spéciaux non autorisés"); valid = false;
        } else {
            showFieldValid(tfRue, lblErrRue);
        }

        // --- Ville ---
        String ville = tfVille.getText().trim();
        if (ville.isEmpty()) {
            showFieldError(tfVille, lblErrVille, "La ville est obligatoire"); valid = false;
        } else if (ville.length() < 2) {
            showFieldError(tfVille, lblErrVille, "Minimum 2 caractères"); valid = false;
        } else if (!ville.matches("[a-zA-ZÀ-ÿ\\s\\-'.]+")) {
            showFieldError(tfVille, lblErrVille, "Lettres uniquement"); valid = false;
        } else {
            showFieldValid(tfVille, lblErrVille);
        }

        // --- Latitude ---
        double latitude = 0;
        String latStr = tfLatitude.getText().trim();
        if (latStr.isEmpty()) {
            showFieldError(tfLatitude, lblErrLat, "La latitude est obligatoire"); valid = false;
        } else {
            try {
                latitude = Double.parseDouble(latStr);
                if (latitude < -90 || latitude > 90) {
                    showFieldError(tfLatitude, lblErrLat, "Doit être entre -90 et 90"); valid = false;
                } else { showFieldValid(tfLatitude, lblErrLat); }
            } catch (NumberFormatException e) {
                showFieldError(tfLatitude, lblErrLat, "Nombre invalide"); valid = false;
            }
        }

        // --- Longitude ---
        double longitude = 0;
        String lonStr = tfLongitude.getText().trim();
        if (lonStr.isEmpty()) {
            showFieldError(tfLongitude, lblErrLon, "La longitude est obligatoire"); valid = false;
        } else {
            try {
                longitude = Double.parseDouble(lonStr);
                if (longitude < -180 || longitude > 180) {
                    showFieldError(tfLongitude, lblErrLon, "Doit être entre -180 et 180"); valid = false;
                } else { showFieldValid(tfLongitude, lblErrLon); }
            } catch (NumberFormatException e) {
                showFieldError(tfLongitude, lblErrLon, "Nombre invalide"); valid = false;
            }
        }

        // --- Altitude ---
        double altitude = 0;
        String altStr = tfAltitude.getText().trim();
        if (altStr.isEmpty()) {
            showFieldError(tfAltitude, lblErrAlt, "L'altitude est obligatoire"); valid = false;
        } else {
            try {
                altitude = Double.parseDouble(altStr);
                showFieldValid(tfAltitude, lblErrAlt);
            } catch (NumberFormatException e) {
                showFieldError(tfAltitude, lblErrAlt, "Nombre invalide"); valid = false;
            }
        }

        if (!valid) return null;

        return new Adresse(rue, ville, latitude, longitude, altitude);
    }

    // ========== VALIDATION HELPERS ==========

    private void setupValidationListeners() {
        if (tfRue != null) tfRue.textProperty().addListener((obs, o, n) -> validateRue(n));
        if (tfVille != null) tfVille.textProperty().addListener((obs, o, n) -> validateVille(n));
        if (tfLatitude != null) tfLatitude.textProperty().addListener((obs, o, n) -> validateLatitude(n));
        if (tfLongitude != null) tfLongitude.textProperty().addListener((obs, o, n) -> validateLongitude(n));
        if (tfAltitude != null) tfAltitude.textProperty().addListener((obs, o, n) -> validateAltitude(n));
    }

    private void validateRue(String val) {
        if (val == null) return;
        String v = val.trim();
        if (v.isEmpty()) clearFieldState(tfRue, lblErrRue);
        else if (v.length() < 2) showFieldError(tfRue, lblErrRue, "Minimum 2 caractères");
        else if (!v.matches("[a-zA-ZÀ-ÿ0-9\\s\\-'.]+")) showFieldError(tfRue, lblErrRue, "Caractères spéciaux non autorisés");
        else showFieldValid(tfRue, lblErrRue);
    }

    private void validateVille(String val) {
        if (val == null) return;
        String v = val.trim();
        if (v.isEmpty()) clearFieldState(tfVille, lblErrVille);
        else if (v.length() < 2) showFieldError(tfVille, lblErrVille, "Minimum 2 caractères");
        else if (!v.matches("[a-zA-ZÀ-ÿ\\s\\-'.]+")) showFieldError(tfVille, lblErrVille, "Lettres uniquement");
        else showFieldValid(tfVille, lblErrVille);
    }

    private void validateLatitude(String val) {
        if (val == null || val.trim().isEmpty()) { clearFieldState(tfLatitude, lblErrLat); return; }
        try {
            double d = Double.parseDouble(val.trim());
            if (d < -90 || d > 90) showFieldError(tfLatitude, lblErrLat, "Doit être entre -90 et 90");
            else showFieldValid(tfLatitude, lblErrLat);
        } catch (NumberFormatException e) { showFieldError(tfLatitude, lblErrLat, "Nombre invalide"); }
    }

    private void validateLongitude(String val) {
        if (val == null || val.trim().isEmpty()) { clearFieldState(tfLongitude, lblErrLon); return; }
        try {
            double d = Double.parseDouble(val.trim());
            if (d < -180 || d > 180) showFieldError(tfLongitude, lblErrLon, "Doit être entre -180 et 180");
            else showFieldValid(tfLongitude, lblErrLon);
        } catch (NumberFormatException e) { showFieldError(tfLongitude, lblErrLon, "Nombre invalide"); }
    }

    private void validateAltitude(String val) {
        if (val == null || val.trim().isEmpty()) { clearFieldState(tfAltitude, lblErrAlt); return; }
        try { Double.parseDouble(val.trim()); showFieldValid(tfAltitude, lblErrAlt); }
        catch (NumberFormatException e) { showFieldError(tfAltitude, lblErrAlt, "Nombre invalide"); }
    }

    private void showFieldError(TextField field, Label errLabel, String msg) {
        if (field != null) { field.getStyleClass().removeAll("input-error", "input-valid"); field.getStyleClass().add("input-error"); }
        if (errLabel != null) { errLabel.setText(msg); errLabel.setVisible(true); errLabel.setManaged(true); }
    }

    private void showFieldValid(TextField field, Label errLabel) {
        if (field != null) { field.getStyleClass().removeAll("input-error", "input-valid"); field.getStyleClass().add("input-valid"); }
        if (errLabel != null) { errLabel.setVisible(false); errLabel.setManaged(false); }
    }

    private void clearFieldState(TextField field, Label errLabel) {
        if (field != null) field.getStyleClass().removeAll("input-error", "input-valid");
        if (errLabel != null) { errLabel.setVisible(false); errLabel.setManaged(false); }
    }

    // ========== MAP HELPERS ==========

    private void searchLocation() {
        if (tfSearch == null || tfSearch.getText().trim().isEmpty()) return;
        String query = tfSearch.getText().trim();
        if (mapReady && mapEngine != null) {
            mapEngine.executeScript("searchLocation('" + query.replace("'", "\\'") + "');");
        }
    }

    private void updateMapSummaryLabels() {
        if (lblMapRue != null) lblMapRue.setText(mapRue.isEmpty() ? "—" : mapRue);
        if (lblMapVille != null) lblMapVille.setText(mapVille.isEmpty() ? "—" : mapVille);
        if (lblMapLat != null) lblMapLat.setText(String.format("%.6f", mapLat));
        if (lblMapLon != null) lblMapLon.setText(String.format("%.6f", mapLon));
        if (lblMapAlt != null) lblMapAlt.setText(String.format("%.0f m", mapAlt));
        if (lblSelectedPos != null) {
            lblSelectedPos.setText(String.format("✓ %.5f, %.5f", mapLat, mapLon));
            lblSelectedPos.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 12px; -fx-font-weight: bold;");
        }
    }

    /**
     * JavaScript bridge: map calls back into Java.
     */
    public class MapBridge {
        public void onMapClick(double lat, double lng) {
            Platform.runLater(() -> {
                mapLat = lat;
                mapLon = lng;
                mapAlt = 0;
                mapPositionSelected = true;
                updateMapSummaryLabels();
            });
        }

        public void onReverseGeocode(String rue, String ville) {
            Platform.runLater(() -> {
                if (rue != null && !rue.isEmpty()) mapRue = rue;
                if (ville != null && !ville.isEmpty()) mapVille = ville;
                updateMapSummaryLabels();
            });
        }

        public void onSearchResult(double lat, double lng, String displayName) {
            Platform.runLater(() -> {
                mapLat = lat;
                mapLon = lng;
                mapAlt = 0;
                mapPositionSelected = true;
                updateMapSummaryLabels();
            });
        }

        public void log(String msg) {
            System.out.println("🗺️ Map: " + msg);
        }
    }

    // ========== HTML GENERATORS ==========

    private String getMapHtml(double initLat, double initLng, int zoom) {
        return "<!DOCTYPE html>\n" +
            "<html><head>\n" +
            "<meta charset='utf-8'/>\n" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'/>\n" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>\n" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>\n" +
            "<style>\n" +
            "  html, body { margin:0; padding:0; height:100%; background:#e8e8e8; overflow:hidden; }\n" +
            "  #map { width:100%; height:100%; }\n" +
            "  .leaflet-container { background:#e8e8e8; }\n" +
            "  .search-info {\n" +
            "    position:absolute; bottom:10px; left:10px; z-index:1000;\n" +
            "    background:rgba(13,26,42,0.9); color:#BFA200; padding:8px 14px;\n" +
            "    border-radius:8px; font-size:12px; border:1px solid rgba(191,162,0,0.3);\n" +
            "    display:none; max-width:300px;\n" +
            "  }\n" +
            "</style>\n" +
            "</head><body>\n" +
            "<div id='map'></div>\n" +
            "<div id='searchInfo' class='search-info'></div>\n" +
            "<script>\n" +
            "var map = L.map('map').setView([" + initLat + ", " + initLng + "], " + zoom + ");\n" +
            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
            "  attribution: '&copy; OpenStreetMap contributors', maxZoom: 19\n" +
            "}).addTo(map);\n" +
            "var marker = L.marker([" + initLat + ", " + initLng + "], {draggable: true}).addTo(map);\n" +
            "marker.bindPopup('<b>Cliquez ou glissez pour choisir</b>').openPopup();\n" +
            "\n" +
            "setTimeout(function(){ map.invalidateSize(); }, 200);\n" +
            "setTimeout(function(){ map.invalidateSize(); }, 800);\n" +
            "window.addEventListener('resize', function(){ map.invalidateSize(); });\n" +
            "\n" +
            "map.on('click', function(e) {\n" +
            "  var lat = e.latlng.lat, lng = e.latlng.lng;\n" +
            "  marker.setLatLng([lat, lng]);\n" +
            "  marker.bindPopup('<b>Lat:</b> ' + lat.toFixed(6) + '<br><b>Lng:</b> ' + lng.toFixed(6)).openPopup();\n" +
            "  if (window.javaBridge) javaBridge.onMapClick(lat, lng);\n" +
            "  reverseGeocode(lat, lng);\n" +
            "});\n" +
            "\n" +
            "marker.on('dragend', function(e) {\n" +
            "  var pos = marker.getLatLng();\n" +
            "  marker.bindPopup('<b>Lat:</b> ' + pos.lat.toFixed(6) + '<br><b>Lng:</b> ' + pos.lng.toFixed(6)).openPopup();\n" +
            "  if (window.javaBridge) javaBridge.onMapClick(pos.lat, pos.lng);\n" +
            "  reverseGeocode(pos.lat, pos.lng);\n" +
            "});\n" +
            "\n" +
            "function updateMarker(lat, lng) {\n" +
            "  marker.setLatLng([lat, lng]); map.setView([lat, lng], map.getZoom());\n" +
            "  marker.bindPopup('<b>Lat:</b> ' + lat.toFixed(6) + '<br><b>Lng:</b> ' + lng.toFixed(6)).openPopup();\n" +
            "}\n" +
            "\n" +
            "function reverseGeocode(lat, lng) {\n" +
            "  fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng + '&zoom=18&addressdetails=1')\n" +
            "    .then(function(r) { return r.json(); })\n" +
            "    .then(function(data) {\n" +
            "      if (data && data.address) {\n" +
            "        var rue = data.address.road || data.address.pedestrian || data.address.neighbourhood || '';\n" +
            "        var ville = data.address.city || data.address.town || data.address.village || data.address.municipality || '';\n" +
            "        if (window.javaBridge) javaBridge.onReverseGeocode(rue, ville);\n" +
            "      }\n" +
            "    }).catch(function(err) { console.log('Reverse geocode error: ' + err); });\n" +
            "}\n" +
            "\n" +
            "function searchLocation(query) {\n" +
            "  var info = document.getElementById('searchInfo');\n" +
            "  info.style.display = 'block'; info.innerText = 'Recherche...';\n" +
            "  fetch('https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(query) + '&limit=1')\n" +
            "    .then(function(r) { return r.json(); })\n" +
            "    .then(function(data) {\n" +
            "      if (data && data.length > 0) {\n" +
            "        var lat = parseFloat(data[0].lat), lng = parseFloat(data[0].lon);\n" +
            "        var name = data[0].display_name;\n" +
            "        marker.setLatLng([lat, lng]); map.setView([lat, lng], 16);\n" +
            "        marker.bindPopup('<b>' + name + '</b>').openPopup();\n" +
            "        info.innerText = name;\n" +
            "        if (window.javaBridge) javaBridge.onSearchResult(lat, lng, name);\n" +
            "        reverseGeocode(lat, lng);\n" +
            "      } else { info.innerText = 'Aucun résultat trouvé'; }\n" +
            "      setTimeout(function() { info.style.display = 'none'; }, 4000);\n" +
            "    }).catch(function(err) {\n" +
            "      info.innerText = 'Erreur de recherche';\n" +
            "      setTimeout(function() { info.style.display = 'none'; }, 3000);\n" +
            "    });\n" +
            "}\n" +
            "</script></body></html>";
    }

}
