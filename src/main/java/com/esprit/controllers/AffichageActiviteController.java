package com.esprit.controllers;

import com.esprit.entities.Activite;
import com.esprit.entities.ActiviteImage;
import com.esprit.services.ActiviteImageServices;
import com.esprit.utils.ThemeManager;
import com.esprit.services.ActiviteServices;
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

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AffichageActiviteController {

    // ===== UI =====
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCategorie;
    @FXML private ComboBox<String> filterNiveau;
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
    private final ActiviteServices service = new ActiviteServices();
    private final ActiviteImageServices imageService = new ActiviteImageServices();

    private final ObservableList<Activite> masterData = FXCollections.observableArrayList();
    private FilteredList<Activite> filteredData;
    private SortedList<Activite> sortedData;

    // Carousel state
    private static final int CARDS_PER_PAGE = 3;
    private int currentPage = 0;

    // pour modifier
    public static Activite activiteToEdit = null;

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
        if (langSelector != null) {
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
        }

        filterCategorie.setItems(FXCollections.observableArrayList("Tous", "Sport", "Culture", "Loisir", "Nature", "Autre"));
        filterCategorie.getSelectionModel().selectFirst();

        filterNiveau.setItems(FXCollections.observableArrayList("Tous", "Débutant", "Intermédiaire", "Avancé"));
        filterNiveau.getSelectionModel().selectFirst();

        sortBox.setItems(FXCollections.observableArrayList("Aucun", "Nom A→Z", "Catégorie A→Z", "Durée ↑", "Durée ↓"));
        sortBox.getSelectionModel().selectFirst();

        loadData();

        filteredData = new FilteredList<>(masterData, a -> true);
        sortedData = new SortedList<>(filteredData);

        // listeners
        searchField.textProperty().addListener((obs, o, n) -> { applyFilters(); rebuildGrid(); });
        filterCategorie.valueProperty().addListener((obs, o, n) -> { applyFilters(); rebuildGrid(); });
        filterNiveau.valueProperty().addListener((obs, o, n) -> { applyFilters(); rebuildGrid(); });
        sortBox.valueProperty().addListener((obs, o, n) -> { applySort(); rebuildGrid(); });

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

    // ===== DATA =====
    private void loadData() {
        masterData.clear();
        try {
            masterData.addAll(service.afficher());
        } catch (SQLException e) {
            showError("Erreur chargement: " + e.getMessage());
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

    // ===== UI CAROUSEL =====
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

        // If fewer than 3 cards on page, add spacers to keep layout balanced
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

        // Page counter label
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

    private Pane buildCard(Activite a) {
        final double CARD_HEIGHT = 340;
        final double CARD_WIDTH = 380;

        // ===== CARD CONTAINER (StackPane overlay style) =====
        StackPane card = new StackPane();
        card.getStyleClass().add("gallery-premium-card");
        card.setMinHeight(CARD_HEIGHT);
        card.setPrefHeight(CARD_HEIGHT);
        card.setMaxHeight(CARD_HEIGHT);

        // ===== IMAGE / GRADIENT BACKGROUND =====
        Image fxImg = loadCoverImage(a.getIdActivite());
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
            int idx = Math.abs(a.getIdActivite()) % GRADIENTS.length;
            Region bg = new Region();
            bg.setStyle("-fx-background-color: " + GRADIENTS[idx] + "; -fx-background-radius: 16;");
            card.getChildren().add(bg);
        }

        // ===== GRADIENT OVERLAY (dark at bottom for text readability) =====
        Region overlay = new Region();
        overlay.setStyle("-fx-background-color: linear-gradient(to top, rgba(0,0,0,0.88) 0%, rgba(0,0,0,0.45) 50%, rgba(0,0,0,0.08) 100%); -fx-background-radius: 16;");
        card.getChildren().add(overlay);

        // ===== CATEGORY BADGE (top-left) =====
        String catText = safe(a.getCategorie());
        if (!catText.isBlank()) {
            Label badge = new Label(catText.toUpperCase());
            badge.getStyleClass().add("card-category-badge");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(14, 0, 0, 14));
            card.getChildren().add(badge);

            // Translate badge async
            String lang = TranslationService.getCurrentLang();
            if (!"fr".equals(lang)) {
                TranslationService.translateAsync(catText, lang, t -> badge.setText(t.toUpperCase()));
            }
        }

        // ===== INFO OVERLAY (bottom) =====
        Label title = new Label(safe(a.getNomActivite()));
        title.getStyleClass().add("gallery-card-title");

        StringBuilder meta = new StringBuilder();
        if (!safe(a.getNiveau()).isBlank()) meta.append(safe(a.getNiveau()));
        if (a.getDuree() != null) {
            if (meta.length() > 0) meta.append(" \u2022 ");
            meta.append(a.getDuree()).append(" min");
        }
        if (a.getPrix() != null && a.getPrix().compareTo(BigDecimal.ZERO) > 0) {
            if (meta.length() > 0) meta.append(" \u2022 ");
            meta.append(a.getPrix().stripTrailingZeros().toPlainString()).append(" DT");
        }
        Label desc = new Label(meta.toString());
        desc.getStyleClass().add("gallery-card-desc");

        // Translate title and meta async
        String lang = TranslationService.getCurrentLang();
        if (!"fr".equals(lang)) {
            TranslationService.translateAsync(safe(a.getNomActivite()), lang, title::setText);
            if (!safe(a.getNiveau()).isBlank()) {
                TranslationService.translateAsync(meta.toString(), lang, desc::setText);
            }
        }

        Button voirPlus = new Button("Voir plus  \u2192");
        voirPlus.getStyleClass().add("btn-voir-plus");
        if (!"fr".equals(lang)) {
            TranslationService.translateAsync("Voir plus", lang, t -> voirPlus.setText(t + "  \u2192"));
        }
        voirPlus.setOnAction(ev -> {
            DetailActiviteController.activiteToShow = a;
            NavigationUtils.goTo("/detail_activite.fxml",
                    new ActionEvent(ev.getSource(), ev.getTarget()));
        });

        Button pdfBtn = new Button("\uD83D\uDCC4");
        pdfBtn.getStyleClass().add("btn-export-pdf-small");
        pdfBtn.setOnAction(ev -> exportActivitePdf(a));

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
    private void exportActivitePdf(Activite a) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter en PDF");
        fc.setInitialFileName(safe(a.getNomActivite()).replaceAll("[^a-zA-Z0-9\\-_ ]", "") + "_fiche.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));

        javafx.stage.Window window = rootPane.getScene().getWindow();
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
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur export");
            err.setHeaderText(null);
            err.setContentText("Erreur lors de la g\u00e9n\u00e9ration du PDF : " + ex.getMessage());
            err.showAndWait();
            ex.printStackTrace();
        }
    }

    // ===== COVER IMAGE =====
    private Image loadCoverImage(int idActivite) {
        try {
            List<ActiviteImage> imgs = imageService.getImagesByActivite(idActivite);
            if (imgs == null || imgs.isEmpty()) return tryLoadPlaceholder();

            String fileName = imgs.get(0).getImagePath(); // 1ère image par ordre

            // 1) Chercher dans le dossier uploads/activites/
            File uploadFile = new File("uploads/activites/" + fileName);
            if (uploadFile.exists()) {
                return new Image(uploadFile.toURI().toString(), true);
            }

            // 2) Fallback : ressource /images/
            return loadImageSafe("/images/" + fileName);

        } catch (Exception e) {
            return tryLoadPlaceholder();
        }
    }

    private Image loadImageSafe(String resourcePathOrFilePath) {
        try {
            if (resourcePathOrFilePath == null || resourcePathOrFilePath.isBlank()) {
                return tryLoadPlaceholder();
            }

            // ressources
            if (resourcePathOrFilePath.startsWith("/")) {
                var is = getClass().getResourceAsStream(resourcePathOrFilePath);
                if (is != null) return new Image(is);
            }

            // fichier local
            File f = new File(resourcePathOrFilePath);
            if (f.exists()) return new Image(f.toURI().toString(), true);

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

    // ===== NAV =====
    @FXML
    private void onShowAjouter(ActionEvent event) {
        activiteToEdit = null;
        NavigationUtils.goTo("/ajouter_activite.fxml", event);
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
        if (sidebarContent == null) return;
        sidebarCollapsed = !sidebarCollapsed;
        sidebarContent.setVisible(!sidebarCollapsed);
        sidebarContent.setManaged(!sidebarCollapsed);
        if (sidebarToggle != null) sidebarToggle.setText(sidebarCollapsed ? "\u276F" : "\u276E");
    }

    @FXML
    private void goHome(ActionEvent event) {
        NavigationUtils.goTo("/maininterface.fxml", event);
    }

    @FXML
    private void goTableau(ActionEvent event) {
        NavigationUtils.goTo("/etablissement_tableau.fxml", event);
    }

    @FXML
    private void goEtablissements(ActionEvent event) {
        NavigationUtils.goTo("/etablissement_affichage.fxml", event);
    }

    @FXML
    private void goTableauActivites(ActionEvent event) {
        NavigationUtils.goTo("/activite_tableau.fxml", event);
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
            // Restore originals
            for (Map.Entry<Labeled, String> entry : staticOriginals.entrySet()) {
                entry.getKey().setText(entry.getValue());
            }
            return;
        }
        for (Map.Entry<Labeled, String> entry : staticOriginals.entrySet()) {
            TranslationService.translateAsync(entry.getValue(), lang, entry.getKey()::setText);
        }
    }

    // ===== helpers =====
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

    @FXML
    private void toggleTheme(ActionEvent event) {
        ThemeManager.handleToggleTheme(event);
    }
}