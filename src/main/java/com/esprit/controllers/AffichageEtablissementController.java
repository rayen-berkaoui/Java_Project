package com.esprit.controllers;

import com.esprit.entities.Etablissement;
import com.esprit.entities.EtablissementImage;
import com.esprit.services.EtablissementImageServices;
import com.esprit.services.EtablissementServices;
import com.esprit.services.PdfExportService;
import com.esprit.services.TranslationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Insets;

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

    // Carousel UI
    @FXML private HBox cardsRow;
    @FXML private HBox dotsBox;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    // Sidebar toggle
    @FXML private VBox sidebarContent;
    @FXML private Button sidebarToggle;
    private boolean sidebarCollapsed = false;

    // Language selector
    @FXML private ComboBox<String> langSelector;
    @FXML private BorderPane rootPane;
    @FXML private StackPane rootStack;

    // ===== Translation state =====
    private final Map<Labeled, String> staticOriginals = new LinkedHashMap<>();
    private boolean staticLabelsCollected = false;

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

    // Carousel state
    private static final int CARDS_PER_PAGE = 3;
    private int currentPage = 0;

    // Gradients for cards without images (matching image 1 design)
    private static final String[] GRADIENTS = {
        "linear-gradient(to bottom right, #667eea, #764ba2)",
        "linear-gradient(to bottom right, #f093fb, #f5576c)",
        "linear-gradient(to bottom right, #4facfe, #00f2fe)",
        "linear-gradient(to bottom right, #43e97b, #38f9d7)",
        "linear-gradient(to bottom right, #fa709a, #fee140)",
        "linear-gradient(to bottom right, #a18cd1, #fbc2eb)"
    };

    @FXML
    public void initialize() {

        // ===== LANGUAGE SELECTOR =====
        langSelector.setItems(FXCollections.observableArrayList(
            TranslationService.langDisplayName("fr"),
            TranslationService.langDisplayName("en"),
            TranslationService.langDisplayName("es"),
            TranslationService.langDisplayName("de"),
            TranslationService.langDisplayName("it"),
            TranslationService.langDisplayName("ar")
        ));
        langSelector.setValue(TranslationService.langDisplayName(TranslationService.getCurrentLang()));
        langSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String code = TranslationService.langCode(newVal);
                TranslationService.setCurrentLang(code);
                translateStaticLabels(code);
                rebuildGrid();
            }
        });

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

        // Translate static labels after scene is rendered
        Platform.runLater(() -> {
            collectStaticLabels();
            String lang = TranslationService.getCurrentLang();
            if (!"fr".equals(lang)) {
                translateStaticLabels(lang);
            }
            ChatbotPanel.install(rootStack);
        });
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
    private void onToggleSidebar(ActionEvent event) {
        sidebarCollapsed = !sidebarCollapsed;
        sidebarContent.setVisible(!sidebarCollapsed);
        sidebarContent.setManaged(!sidebarCollapsed);
        sidebarToggle.setText(sidebarCollapsed ? "\u276F" : "\u276E");
    }

    @FXML
    private void goHome(ActionEvent event) {
        NavigationUtils.goTo("/home.fxml", event);
    }

    @FXML
    private void goTableau(ActionEvent event) {
        NavigationUtils.goTo("/etablissement_tableau.fxml", event);
    }

    @FXML
    private void goActivites(ActionEvent event) {
        NavigationUtils.goTo("/activite_affichage.fxml", event);
    }

    @FXML
    private void goTableauActivites(ActionEvent event) {
        NavigationUtils.goTo("/activite_tableau.fxml", event);
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

    // ====== UI CAROUSEL ======
    private void rebuildGrid() {
        currentPage = 0;
        showPage();
    }

    private int totalPages() {
        int total = sortedData.size();
        return Math.max(1, (int) Math.ceil((double) total / CARDS_PER_PAGE));
    }

    private void showPage() {
        cardsRow.getChildren().clear();

        int total = sortedData.size();
        int start = currentPage * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, total);

        for (int i = start; i < end; i++) {
            Pane card = buildCard(sortedData.get(i));
            HBox.setHgrow(card, Priority.NEVER);
            card.setPrefWidth(380);
            card.setMinWidth(380);
            card.setMaxWidth(380);
            cardsRow.getChildren().add(card);
        }

        // If fewer than 3 cards on page, add spacers for balanced layout
        for (int i = cardsRow.getChildren().size(); i < CARDS_PER_PAGE; i++) {
            Region spacer = new Region();
            spacer.setPrefWidth(380);
            spacer.setMinWidth(380);
            spacer.setMaxWidth(380);
            HBox.setHgrow(spacer, Priority.NEVER);
            cardsRow.getChildren().add(spacer);
        }

        // Update arrows
        prevBtn.setDisable(currentPage <= 0);
        nextBtn.setDisable(currentPage >= totalPages() - 1);

        // Update dots
        buildDots();
    }

    private void buildDots() {
        dotsBox.getChildren().clear();
        int pages = totalPages();
        if (pages <= 1) return;

        for (int i = 0; i < pages; i++) {
            Circle dot = new Circle(i == currentPage ? 6 : 5);
            dot.getStyleClass().add(i == currentPage ? "carousel-dot-active" : "carousel-dot");
            final int page = i;
            dot.setCursor(Cursor.HAND);
            dot.setOnMouseClicked(e -> { currentPage = page; showPage(); });
            dotsBox.getChildren().add(dot);
        }

        Label counter = new Label((currentPage + 1) + " / " + pages);
        counter.getStyleClass().add("carousel-counter");
        dotsBox.getChildren().add(counter);
    }

    @FXML
    private void onPrev(ActionEvent event) {
        if (currentPage > 0) {
            currentPage--;
            showPage();
        }
    }

    @FXML
    private void onNext(ActionEvent event) {
        if (currentPage < totalPages() - 1) {
            currentPage++;
            showPage();
        }
    }

    private Pane buildCard(Etablissement e) {
        final double CARD_HEIGHT = 340;
        final double CARD_WIDTH = 380;

        // ===== CARD CONTAINER (StackPane overlay style) =====
        StackPane card = new StackPane();
        card.getStyleClass().add("gallery-premium-card");
        card.setMinHeight(CARD_HEIGHT);
        card.setPrefHeight(CARD_HEIGHT);
        card.setMaxHeight(CARD_HEIGHT);

        // ===== IMAGE / GRADIENT BACKGROUND =====
        String imgName = mainImageCache.get(e.getIdEtablissement());
        Image fxImg = loadImageFromResources(imgName);

        if (fxImg != null) {
            ImageView iv = new ImageView(fxImg);
            iv.setFitWidth(CARD_WIDTH);
            iv.setFitHeight(CARD_HEIGHT);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            Rectangle clip = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
            clip.setArcWidth(32);
            clip.setArcHeight(32);
            iv.setClip(clip);
            card.getChildren().add(iv);
        } else {
            int idx = Math.abs(e.getIdEtablissement()) % GRADIENTS.length;
            Region bg = new Region();
            bg.setStyle("-fx-background-color: " + GRADIENTS[idx] + "; -fx-background-radius: 16;");
            card.getChildren().add(bg);
        }

        // ===== GRADIENT OVERLAY (dark at bottom for text readability) =====
        Region overlay = new Region();
        overlay.setStyle("-fx-background-color: linear-gradient(to top, rgba(0,0,0,0.88) 0%, rgba(0,0,0,0.45) 50%, rgba(0,0,0,0.08) 100%); -fx-background-radius: 16;");
        card.getChildren().add(overlay);

        // ===== TYPE BADGE (top-left) =====
        String typeText = safe(e.getType());
        if (!typeText.isBlank()) {
            Label badge = new Label(typeText.toUpperCase());
            badge.getStyleClass().add("card-category-badge");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(14, 0, 0, 14));
            card.getChildren().add(badge);

            // Translate badge async
            String lang = TranslationService.getCurrentLang();
            if (!"fr".equals(lang)) {
                TranslationService.translateAsync(typeText, lang, t -> badge.setText(t.toUpperCase()));
            }
        }

        // ===== INFO OVERLAY (bottom) =====
        Label title = new Label(safe(e.getNom()));
        title.getStyleClass().add("gallery-card-title");

        StringBuilder meta = new StringBuilder();
        if (!safe(e.getVille()).isBlank()) meta.append(safe(e.getVille()));
        if (!safe(e.getGammePrix()).isBlank()) {
            if (meta.length() > 0) meta.append(" \u2022 ");
            meta.append(safe(e.getGammePrix()));
        }
        Label desc = new Label(meta.toString());
        desc.getStyleClass().add("gallery-card-desc");

        // Translate title and meta async
        String lang = TranslationService.getCurrentLang();
        if (!"fr".equals(lang)) {
            TranslationService.translateAsync(safe(e.getNom()), lang, title::setText);
            if (meta.length() > 0) {
                TranslationService.translateAsync(meta.toString(), lang, desc::setText);
            }
        }

        Button voirPlus = new Button("Voir plus  \u2192");
        voirPlus.getStyleClass().add("btn-voir-plus");
        if (!"fr".equals(lang)) {
            TranslationService.translateAsync("Voir plus", lang, t -> voirPlus.setText(t + "  \u2192"));
        }
        voirPlus.setOnAction(ev -> {
            DetailEtablissementController.etablissementToShow = e;
            NavigationUtils.goTo("/detail_etablissement.fxml",
                    new ActionEvent(ev.getSource(), ev.getTarget()));
        });

        Button pdfBtn = new Button("\uD83D\uDCC4");
        pdfBtn.getStyleClass().add("btn-export-pdf-small");
        pdfBtn.setOnAction(ev -> exportEtablissementPdf(e));

        HBox buttonsRow = new HBox(8, voirPlus, pdfBtn);
        buttonsRow.setAlignment(Pos.CENTER_LEFT);

        VBox infoBox = new VBox(6, title, desc, buttonsRow);
        infoBox.getStyleClass().add("card-info-overlay");
        StackPane.setAlignment(infoBox, Pos.BOTTOM_LEFT);
        card.getChildren().add(infoBox);

        card.setCursor(Cursor.HAND);
        return card;
    }
    // ===== PDF Export from card =====
    private void exportEtablissementPdf(Etablissement e) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter en PDF");
        fc.setInitialFileName(safe(e.getNom()).replaceAll("[^a-zA-Z0-9\\-_ ]", "") + "_fiche.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));

        javafx.stage.Window window = rootPane.getScene().getWindow();
        java.io.File file = fc.showSaveDialog(window);
        if (file == null) return;

        try {
            PdfExportService.exportEtablissement(e, file);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export r\u00e9ussi");
            ok.setHeaderText(null);
            ok.setContentText("PDF g\u00e9n\u00e9r\u00e9 avec succ\u00e8s !\n" + file.getAbsolutePath());
            ok.showAndWait();
            if (java.awt.Desktop.isDesktopSupported()) {
                new Thread(() -> { try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {} }).start();
            }
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur export");
            err.setHeaderText(null);
            err.setContentText("Erreur lors de la g\u00e9n\u00e9ration du PDF : " + ex.getMessage());
            err.showAndWait();
            ex.printStackTrace();
        }
    }
    // Charge l'image depuis le système de fichiers OU depuis /images/<nom> dans les resources
    private Image loadImageFromResources(String imgName) {
        try {
            if (imgName == null || imgName.isBlank()) {
                return tryLoadPlaceholder();
            }

            // 1) Si on a un chemin absolu ou contenant un séparateur, on tente le système de fichiers
            if (imgName.contains("\\") || imgName.contains("/") || imgName.matches("^[A-Za-z]:.*")) {
                try {
                    java.io.File f = new java.io.File(imgName);
                    if (f.exists()) {
                        return new Image(f.toURI().toString());
                    }
                } catch (Exception ignored) {
                    // on tombera sur le placeholder plus bas
                }
            }

            // 2) Chercher dans le dossier uploads/etablissements/
            java.io.File uploadFile = new java.io.File("uploads/etablissements/" + imgName);
            if (uploadFile.exists()) {
                return new Image(uploadFile.toURI().toString(), true);
            }

            // 3) Sinon, on considère que c'est juste un nom de fichier présent dans /images des resources
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

    // ===== STATIC LABEL TRANSLATION =====
    private void collectStaticLabels() {
        if (staticLabelsCollected || rootPane == null) return;
        // Nav bar
        rootPane.lookupAll(".nav-logo").forEach(n -> { if (n instanceof Labeled l) staticOriginals.put(l, l.getText()); });
        rootPane.lookupAll(".nav-tab").forEach(n -> { if (n instanceof Labeled l) staticOriginals.put(l, l.getText()); });
        rootPane.lookupAll(".nav-tab-active").forEach(n -> { if (n instanceof Labeled l) staticOriginals.put(l, l.getText()); });
        rootPane.lookupAll(".pill").forEach(n -> { if (n instanceof Labeled l) staticOriginals.put(l, l.getText()); });
        // Sidebar
        rootPane.lookupAll(".sidebar-title").forEach(n -> { if (n instanceof Labeled l) staticOriginals.put(l, l.getText()); });
        rootPane.lookupAll(".sidebar-section-label").forEach(n -> { if (n instanceof Labeled l) staticOriginals.put(l, l.getText()); });
        rootPane.lookupAll(".sidebar-btn").forEach(n -> { if (n instanceof Labeled l) staticOriginals.put(l, l.getText()); });
        // Page title
        rootPane.lookupAll(".h1").forEach(n -> { if (n instanceof Labeled l) staticOriginals.put(l, l.getText()); });
        staticLabelsCollected = true;
    }

    private void translateStaticLabels(String lang) {
        if (!staticLabelsCollected) collectStaticLabels();
        if ("fr".equals(lang)) {
            for (Map.Entry<Labeled, String> entry : staticOriginals.entrySet()) {
                entry.getKey().setText(entry.getValue());
            }
            return;
        }
        for (Map.Entry<Labeled, String> entry : staticOriginals.entrySet()) {
            TranslationService.translateAsync(entry.getValue(), lang, entry.getKey()::setText);
        }
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