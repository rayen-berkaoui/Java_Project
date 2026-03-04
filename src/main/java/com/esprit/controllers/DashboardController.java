package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;

import com.esprit.entities.AuditLog;
import com.esprit.entities.Panier;
import com.esprit.entities.Reservation;
import com.esprit.entities.RolePermission;
import com.esprit.entities.role;
import com.esprit.entities.utilisateur;
import com.esprit.services.*;
import com.esprit.utils.ThemeManager;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DashboardController {

    // ================= UI =================
    @FXML private Label welcomeLabel;
    @FXML private Label userNameLabel;

    // Panels
    @FXML private VBox dashboardPane;
    @FXML private VBox chartsPane;
    @FXML private VBox usersPane;
    @FXML private VBox auditPane;
    @FXML private VBox permissionsPane;
    @FXML private VBox settingsPane;
    @FXML private VBox supportPane;
    @FXML private VBox panierAdminPane;
    @FXML private VBox reservationAdminPane;

    @FXML private StackPane contentArea;
    @FXML private HBox titleBar;

    // Nav buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnCharts;
    @FXML private Button btnUsers;
    @FXML private Button btnAudit;
    @FXML private Button btnPermissions;
    @FXML private Button btnSettings;
    @FXML private Button btnSupport;
    @FXML private Button btnPanierAdmin;
    @FXML private Button btnReservationAdmin;

    // Dashboard stats
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label blockedUsersLabel;
    @FXML private Label totalRolesLabel;
    @FXML private VBox recentUsersContainer;

    // Mini charts on dashboard
    @FXML private PieChart miniPieChart;
    @FXML private BarChart<String, Number> miniBarChart;

    // Charts panel
    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> roleBarChart;
    @FXML private LineChart<String, Number> activityLineChart;

    // Users panel
    @FXML private TextField userSearchField;
    @FXML private ComboBox<String> bulkRoleCombo;
    @FXML private CheckBox selectAllCheckbox;
    @FXML private Label selectionCountLabel;
    @FXML private Label userStatusLabel;
    @FXML private VBox userTableContainer;
    @FXML private Label pageLabel;

    // Audit panel
    @FXML private ComboBox<String> auditActionFilter;
    @FXML private DatePicker auditDateFrom;
    @FXML private DatePicker auditDateTo;
    @FXML private TextField auditSearchField;
    @FXML private Label auditCountLabel;
    @FXML private VBox auditTableContainer;

    // Permissions panel
    @FXML private VBox permissionsGrid;
    @FXML private Label permissionsMessageLabel;

    // Settings
    @FXML private CheckBox emailNotifCheckbox;
    @FXML private CheckBox twoFactorCheckbox;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private Label settingsMessageLabel;

    // Support
    @FXML private TextField ticketSubjectField;
    @FXML private ComboBox<String> ticketPriorityCombo;
    @FXML private TextArea ticketDescriptionArea;
    @FXML private Label ticketMessageLabel;

    // Panier Admin
    @FXML private ComboBox<String> panierUserFilter;
    @FXML private Label panierAdminCountLabel;
    @FXML private VBox panierAdminContainer;

    // Reservation Admin
    @FXML private ComboBox<String> resUserFilter;
    @FXML private ComboBox<String> resStatusFilter;
    @FXML private Label resAdminCountLabel;
    @FXML private VBox reservationAdminContainer;
    @FXML private Label adminTotalResLabel;
    @FXML private Label adminPendingResLabel;
    @FXML private Label adminPaidResLabel;
    @FXML private Label adminRevenueLabel;

    // Services
    private final utilisateurServices userService = new utilisateurServices();
    private final roleServices roleService = new roleServices();
    private final AuditLogService auditService = new AuditLogService();
    private final RolePermissionService permService = new RolePermissionService();
    private final ExportService exportService = new ExportService();
    private final ImportService importService = new ImportService();
    private final PanierService panierService = new PanierService();
    private final ReservationService reservationService = new ReservationService();

    // State
    private String currentUserName;
    private int currentAdminId = 0;
    private double xOffset = 0;
    private double yOffset = 0;
    private int currentPage = 1;
    private final int PAGE_SIZE = 15;
    private String currentSearchQuery = "";
    private final Map<Integer, CheckBox> userCheckboxes = new LinkedHashMap<>();
    private final Map<Integer, CheckBox> permCheckboxMap = new LinkedHashMap<>();
    private Map<String, Integer> userNameToIdMap = new LinkedHashMap<>();

    // Carousel states
    private int panierCarouselIndex = 0;
    private static final int PANIER_CAROUSEL_SIZE = 4;
    private List<Panier> panierCarouselList = new ArrayList<>();
    private Integer currentPanierFilterUserId = null;

    private int resCarouselIndex = 0;
    private static final int RES_CAROUSEL_SIZE = 4;
    private List<Reservation> resCarouselList = new ArrayList<>();
    private Integer currentResFilterUserId = null;
    private String currentResFilterStatus = null;

    private int auditCarouselIndex = 0;
    private static final int AUDIT_CAROUSEL_SIZE = 6;
    private List<AuditLog> auditCarouselList = new ArrayList<>();

    // ================= INIT =================
    @FXML
    public void initialize() {
        showPane(dashboardPane);
        setActiveNav(btnDashboard);
        loadDashboardData();

        if (titleBar != null) {
            titleBar.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
            titleBar.setOnMouseDragged(e -> {
                Stage s = (Stage) titleBar.getScene().getWindow();
                s.setX(e.getScreenX() - xOffset);
                s.setY(e.getScreenY() - yOffset);
            });
        }

        loadRolesCombo();
        loadUserFilters();

        // Apply theme to FXML nodes
        javafx.application.Platform.runLater(() -> {
            if (titleBar != null && titleBar.getScene() != null && titleBar.getScene().getRoot() != null) {
                ThemeManager.applyThemeToFXML((javafx.scene.Parent) titleBar.getScene().getRoot());
            }
        });
        ThemeManager.addThemeChangeListener(() -> javafx.application.Platform.runLater(() -> {
            if (titleBar != null && titleBar.getScene() != null && titleBar.getScene().getRoot() != null) {
                ThemeManager.applyThemeToFXML((javafx.scene.Parent) titleBar.getScene().getRoot());
            }
        }));
    }

    public void setUserName(String userName) {
        this.currentUserName = userName;
        if (userNameLabel != null) userNameLabel.setText(userName);
        if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + userName);
    }

    public void setAdminId(int adminId) {
        this.currentAdminId = adminId;
    }

    private void loadRolesCombo() {
        try {
            List<role> roles = roleService.afficher();
            List<String> items = new ArrayList<>();
            for (role r : roles) {
                items.add(r.getId() + " - " + r.getNom());
            }
            if (bulkRoleCombo != null) bulkRoleCombo.setItems(FXCollections.observableArrayList(items));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserFilters() {
        try {
            List<utilisateur> allUsers = userService.afficher();
            userNameToIdMap.clear();
            List<String> userNames = new ArrayList<>();
            for (utilisateur u : allUsers) {
                String display = u.getPrenom() + " " + u.getNom() + " (#" + u.getId() + ")";
                userNames.add(display);
                userNameToIdMap.put(display, u.getId());
            }
            if (panierUserFilter != null) panierUserFilter.setItems(FXCollections.observableArrayList(userNames));
            if (resUserFilter != null) resUserFilter.setItems(FXCollections.observableArrayList(userNames));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= DASHBOARD DATA =================
    private void loadDashboardData() {
        int total = userService.countAll();
        int active = userService.countByStatus("ACTIF");
        int blocked = userService.countByStatus("BLOQUE");
        int roles = 0;
        try { roles = roleService.afficher().size(); } catch (Exception e) { e.printStackTrace(); }

        if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(total));
        if (activeUsersLabel != null) activeUsersLabel.setText(String.valueOf(active));
        if (blockedUsersLabel != null) blockedUsersLabel.setText(String.valueOf(blocked));
        if (totalRolesLabel != null) totalRolesLabel.setText(String.valueOf(roles));

        loadRecentUsers();
        loadMiniCharts(active, blocked);
    }

    private void loadMiniCharts(int active, int blocked) {
        if (miniPieChart != null) {
            miniPieChart.getData().clear();
            miniPieChart.getData().add(new PieChart.Data("Actifs (" + active + ")", active));
            miniPieChart.getData().add(new PieChart.Data("Bloques (" + blocked + ")", blocked));
            miniPieChart.setStyle("-fx-padding: 0;");
        }
        if (miniBarChart != null) {
            miniBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            try {
                List<role> roles = roleService.afficher();
                for (role r : roles) {
                    int count = userService.countByRole(r.getId());
                    series.getData().add(new XYChart.Data<>(r.getNom(), count));
                }
            } catch (Exception e) { e.printStackTrace(); }
            miniBarChart.getData().add(series);
        }
    }

    // Carousel state for recent users
    private List<utilisateur> recentUsersList = new ArrayList<>();
    private int carouselIndex = 0;
    private static final int CAROUSEL_PAGE_SIZE = 3;

    private void loadRecentUsers() {
        if (recentUsersContainer == null) return;
        recentUsersContainer.getChildren().clear();

        recentUsersList = userService.getRecentUsers(15);
        carouselIndex = 0;

        if (recentUsersList.isEmpty()) {
            Label empty = new Label("Aucun utilisateur recent");
            empty.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");
            recentUsersContainer.getChildren().add(empty);
            return;
        }

        buildCarousel();
    }

    private void buildCarousel() {
        if (recentUsersContainer == null) return;
        recentUsersContainer.getChildren().clear();

        int totalPages = (int) Math.ceil((double) recentUsersList.size() / CAROUSEL_PAGE_SIZE);
        int currentCarouselPage = carouselIndex / CAROUSEL_PAGE_SIZE;

        // Navigation bar
        HBox navBar = new HBox(12);
        navBar.setAlignment(Pos.CENTER);
        navBar.setStyle("-fx-padding: 0 0 8 0;");

        Button prevBtn = new Button("\u25C0  Precedent");
        prevBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFAA00); -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        prevBtn.setDisable(carouselIndex == 0);
        prevBtn.setOnAction(e -> {
            carouselIndex = Math.max(0, carouselIndex - CAROUSEL_PAGE_SIZE);
            buildCarousel();
        });

        Label pageIndicator = new Label((currentCarouselPage + 1) + " / " + totalPages);
        pageIndicator.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");

        Button nextBtn = new Button("Suivant  \u25B6");
        nextBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFAA00); -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        nextBtn.setDisable(carouselIndex + CAROUSEL_PAGE_SIZE >= recentUsersList.size());
        nextBtn.setOnAction(e -> {
            carouselIndex = Math.min(recentUsersList.size() - 1, carouselIndex + CAROUSEL_PAGE_SIZE);
            buildCarousel();
        });

        // Dot indicators
        HBox dots = new HBox(6);
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < totalPages; i++) {
            Label dot = new Label("\u25CF");
            if (i == currentCarouselPage) {
                dot.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 10;");
            } else {
                dot.setStyle("-fx-text-fill: #444444; -fx-font-size: 10;");
            }
            dots.getChildren().add(dot);
        }

        navBar.getChildren().addAll(prevBtn, dots, pageIndicator, nextBtn);
        recentUsersContainer.getChildren().add(navBar);

        // Cards grid
        HBox cardsRow = new HBox(14);
        cardsRow.setAlignment(Pos.CENTER);

        int end = Math.min(carouselIndex + CAROUSEL_PAGE_SIZE, recentUsersList.size());
        for (int i = carouselIndex; i < end; i++) {
            utilisateur u = recentUsersList.get(i);
            VBox card = buildUserCard(u);
            HBox.setHgrow(card, Priority.ALWAYS);
            cardsRow.getChildren().add(card);
        }

        // Animate cards in
        cardsRow.setOpacity(0);
        cardsRow.setTranslateX(15);
        recentUsersContainer.getChildren().add(cardsRow);

        FadeTransition fade = new FadeTransition(Duration.millis(300), cardsRow);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(300), cardsRow);
        slide.setFromX(15);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();
    }

    private VBox buildUserCard(utilisateur u) {
        String roleName = userService.getRoleName(u.getRoleId());
        String status = u.getStatut();
        String statusColor = "ACTIF".equalsIgnoreCase(status) ? "#51CF66" : "#FF6B6B";
        String statusBg = "ACTIF".equalsIgnoreCase(status) ? "rgba(81,207,102,0.1)" : "rgba(255,107,107,0.1)";
        String dateStr = u.getDateCreation() != null ? u.getDateCreation().toString() : "-";

        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: rgba(18,18,18,0.95); -fx-padding: 20; -fx-background-radius: 14; "
                + "-fx-border-color: rgba(255,215,0,0.08); -fx-border-radius: 14; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0.15, 0, 3);");
        card.setMinWidth(200);
        card.setAlignment(Pos.CENTER);

        // Avatar circle with initials
        String initials = "";
        if (u.getPrenom() != null && !u.getPrenom().isEmpty()) initials += u.getPrenom().charAt(0);
        if (u.getNom() != null && !u.getNom().isEmpty()) initials += u.getNom().charAt(0);
        Label avatar = new Label(initials.toUpperCase());
        avatar.setStyle("-fx-background-color: rgba(255,215,0,0.12); -fx-text-fill: #FFD700; -fx-font-size: 16; "
                + "-fx-font-weight: bold; -fx-min-width: 48; -fx-min-height: 48; -fx-max-width: 48; -fx-max-height: 48; "
                + "-fx-alignment: center; -fx-background-radius: 24;");

        Label idLabel = new Label("#" + u.getId());
        idLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 10;");

        Label nameLabel = new Label(u.getPrenom() + " " + u.getNom());
        nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);

        Label emailLabel = new Label(u.getEmail() != null ? u.getEmail() : "");
        emailLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 10;");
        emailLabel.setWrapText(true);

        HBox roleBox = new HBox(6);
        roleBox.setAlignment(Pos.CENTER);
        Label roleLabel = new Label("\uD83C\uDFF7 " + roleName);
        roleLabel.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 10; -fx-font-weight: bold;");
        roleBox.getChildren().add(roleLabel);

        Label statusLabel = new Label(status);
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 10; -fx-font-weight: bold; "
                + "-fx-background-color: " + statusBg + "; -fx-padding: 3 10; -fx-background-radius: 12;");

        Label dateLabel = new Label("\uD83D\uDCC5 " + dateStr);
        dateLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 9;");

        card.getChildren().addAll(avatar, idLabel, nameLabel, emailLabel, roleBox, statusLabel, dateLabel);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace("rgba(255,215,0,0.08)", "rgba(255,215,0,0.2)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("rgba(255,215,0,0.2)", "rgba(255,215,0,0.08)")));

        return card;
    }

    // ================= CHARTS =================
    private void loadCharts() {
        int active = userService.countByStatus("ACTIF");
        int blocked = userService.countByStatus("BLOQUE");

        if (statusPieChart != null) {
            statusPieChart.getData().clear();
            statusPieChart.getData().add(new PieChart.Data("Actifs (" + active + ")", active));
            statusPieChart.getData().add(new PieChart.Data("Bloques (" + blocked + ")", blocked));
        }
        if (roleBarChart != null) {
            roleBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Utilisateurs");
            try {
                List<role> roles = roleService.afficher();
                for (role r : roles) {
                    int count = userService.countByRole(r.getId());
                    series.getData().add(new XYChart.Data<>(r.getNom(), count));
                }
            } catch (Exception e) { e.printStackTrace(); }
            roleBarChart.getData().add(series);
        }
        if (activityLineChart != null) {
            activityLineChart.getData().clear();
            XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
            lineSeries.setName("Actions");
            List<Object[]> activity = auditService.getActivityPerDay(30);
            for (Object[] row : activity) {
                lineSeries.getData().add(new XYChart.Data<>((String) row[0], (Number) row[1]));
            }
            if (lineSeries.getData().isEmpty()) {
                lineSeries.getData().add(new XYChart.Data<>("Aujourd'hui", 0));
            }
            activityLineChart.getData().add(lineSeries);
        }
    }

    @FXML private void handleRefreshCharts() { loadCharts(); }

    // ================= USERS PANEL (CAROUSEL) =================
    private List<utilisateur> userCarouselList = new ArrayList<>();
    private int userCarouselIndex = 0;
    private static final int USER_CAROUSEL_SIZE = 3;

    private void loadUsersTable() {
        if (userTableContainer == null) return;
        userTableContainer.getChildren().clear();
        userCheckboxes.clear();

        List<utilisateur> users;
        int totalCount;
        if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
            users = userService.searchUsers(currentSearchQuery, currentPage, PAGE_SIZE);
            totalCount = userService.countSearch(currentSearchQuery);
        } else {
            users = userService.getPage(currentPage, PAGE_SIZE);
            totalCount = userService.countAll();
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        if (pageLabel != null) pageLabel.setText("Page " + currentPage + " / " + totalPages);

        if (users.isEmpty()) {
            Label empty = new Label("Aucun utilisateur trouve");
            empty.setStyle("-fx-text-fill: #666666; -fx-font-size: 13; -fx-padding: 20;");
            userTableContainer.getChildren().add(empty);
            return;
        }

        userCarouselList = users;
        userCarouselIndex = 0;

        // Register all checkboxes for bulk actions
        for (utilisateur u : users) {
            CheckBox cb = new CheckBox();
            cb.getStyleClass().add("custom-checkbox");
            cb.setOnAction(e -> updateSelectionCount());
            userCheckboxes.put(u.getId(), cb);
        }

        buildUserCarousel();
        updateSelectionCount();
    }

    private void buildUserCarousel() {
        if (userTableContainer == null) return;
        userTableContainer.getChildren().clear();

        int totalPages = (int) Math.ceil((double) userCarouselList.size() / USER_CAROUSEL_SIZE);
        int currentCarouselPage = userCarouselIndex / USER_CAROUSEL_SIZE;

        // Navigation bar
        HBox navBar = new HBox(12);
        navBar.setAlignment(Pos.CENTER);
        navBar.setStyle("-fx-padding: 0 0 10 0;");

        Button prevBtn = new Button("\u25C0  Precedent");
        prevBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFAA00); -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        prevBtn.setDisable(userCarouselIndex == 0);
        prevBtn.setOnAction(e -> {
            userCarouselIndex = Math.max(0, userCarouselIndex - USER_CAROUSEL_SIZE);
            buildUserCarousel();
        });

        Label pageIndicator = new Label((currentCarouselPage + 1) + " / " + totalPages);
        pageIndicator.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");

        Button nextBtn = new Button("Suivant  \u25B6");
        nextBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFAA00); -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        nextBtn.setDisable(userCarouselIndex + USER_CAROUSEL_SIZE >= userCarouselList.size());
        nextBtn.setOnAction(e -> {
            userCarouselIndex = Math.min(userCarouselList.size() - 1, userCarouselIndex + USER_CAROUSEL_SIZE);
            buildUserCarousel();
        });

        HBox dots = new HBox(6);
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < totalPages; i++) {
            Label dot = new Label("\u25CF");
            dot.setStyle(i == currentCarouselPage ? "-fx-text-fill: #FFD700; -fx-font-size: 10;" : "-fx-text-fill: #444444; -fx-font-size: 10;");
            dots.getChildren().add(dot);
        }

        navBar.getChildren().addAll(prevBtn, dots, pageIndicator, nextBtn);
        userTableContainer.getChildren().add(navBar);

        // Cards grid
        HBox cardsRow = new HBox(14);
        cardsRow.setAlignment(Pos.CENTER);

        int end = Math.min(userCarouselIndex + USER_CAROUSEL_SIZE, userCarouselList.size());
        for (int i = userCarouselIndex; i < end; i++) {
            utilisateur u = userCarouselList.get(i);
            VBox card = buildUserManageCard(u);
            HBox.setHgrow(card, Priority.ALWAYS);
            cardsRow.getChildren().add(card);
        }

        cardsRow.setOpacity(0);
        cardsRow.setTranslateX(15);
        userTableContainer.getChildren().add(cardsRow);

        FadeTransition fade = new FadeTransition(Duration.millis(300), cardsRow);
        fade.setFromValue(0); fade.setToValue(1); fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), cardsRow);
        slide.setFromX(15); slide.setToX(0); slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    private VBox buildUserManageCard(utilisateur u) {
        String roleName = userService.getRoleName(u.getRoleId());
        String status = u.getStatut() != null ? u.getStatut() : "";
        String statusColor = "ACTIF".equalsIgnoreCase(status) ? "#51CF66" : "#FF6B6B";
        String statusBg = "ACTIF".equalsIgnoreCase(status) ? "rgba(81,207,102,0.1)" : "rgba(255,107,107,0.1)";
        String dateStr = u.getDateCreation() != null ? u.getDateCreation().toString() : "-";

        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(18,18,18,0.95); -fx-padding: 18; -fx-background-radius: 14; "
                + "-fx-border-color: rgba(255,215,0,0.08); -fx-border-radius: 14; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0.15, 0, 3);");
        card.setMinWidth(220);
        card.setAlignment(Pos.CENTER);

        // Checkbox at top-right
        CheckBox cb = userCheckboxes.get(u.getId());
        HBox checkRow = new HBox();
        checkRow.setAlignment(Pos.CENTER_RIGHT);
        if (cb != null) checkRow.getChildren().add(cb);

        // Avatar
        String initials = "";
        if (u.getPrenom() != null && !u.getPrenom().isEmpty()) initials += u.getPrenom().charAt(0);
        if (u.getNom() != null && !u.getNom().isEmpty()) initials += u.getNom().charAt(0);
        Label avatar = new Label(initials.toUpperCase());
        avatar.setStyle("-fx-background-color: rgba(255,215,0,0.12); -fx-text-fill: #FFD700; -fx-font-size: 16; "
                + "-fx-font-weight: bold; -fx-min-width: 48; -fx-min-height: 48; -fx-max-width: 48; -fx-max-height: 48; "
                + "-fx-alignment: center; -fx-background-radius: 24;");

        Label idLabel = new Label("#" + u.getId());
        idLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 10;");

        Label nameLabel = new Label(u.getPrenom() + " " + u.getNom());
        nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);

        Label emailLabel = new Label(u.getEmail() != null ? u.getEmail() : "");
        emailLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 10;");
        emailLabel.setWrapText(true);

        HBox infoRow = new HBox(8);
        infoRow.setAlignment(Pos.CENTER);
        Label roleLabel = new Label("\uD83C\uDFF7 " + roleName);
        roleLabel.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 10; -fx-font-weight: bold;");
        Label telLabel = new Label("\uD83D\uDCDE " + u.getNumTel());
        telLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 9;");
        infoRow.getChildren().addAll(roleLabel, telLabel);

        Label statusLabel = new Label(status);
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 10; -fx-font-weight: bold; "
                + "-fx-background-color: " + statusBg + "; -fx-padding: 3 10; -fx-background-radius: 12;");

        Label dateLabel = new Label("\uD83D\uDCC5 " + dateStr);
        dateLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 9;");

        card.getChildren().addAll(checkRow, avatar, idLabel, nameLabel, emailLabel, infoRow, statusLabel, dateLabel);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace("rgba(255,215,0,0.08)", "rgba(255,215,0,0.2)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("rgba(255,215,0,0.2)", "rgba(255,215,0,0.08)")));

        return card;
    }

    private Label makeHeaderLabel(String text, double width) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 10; -fx-font-weight: bold;");
        l.setMinWidth(width);
        l.setMaxWidth(width);
        return l;
    }

    private Label makeDataLabel(String text, double width, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11;");
        l.setMinWidth(width);
        l.setMaxWidth(width);
        return l;
    }

    private boolean isRestoOrCafeAdmin(String type) {
        if (type == null) return false;
        String t = type.toLowerCase();
        return t.contains("restaurant") || t.contains("caf") || t.contains("resto");
    }

    @FXML private void handleUserSearch() {
        currentSearchQuery = userSearchField != null ? userSearchField.getText().trim() : "";
        currentPage = 1;
        loadUsersTable();
    }

    @FXML private void handleSelectAll() {
        boolean selected = selectAllCheckbox != null && selectAllCheckbox.isSelected();
        for (CheckBox cb : userCheckboxes.values()) { cb.setSelected(selected); }
        updateSelectionCount();
    }

    private void updateSelectionCount() {
        long count = userCheckboxes.values().stream().filter(CheckBox::isSelected).count();
        if (selectionCountLabel != null) {
            selectionCountLabel.setText(count > 0 ? count + " selectionne(s)" : "");
        }
    }

    private List<Integer> getSelectedUserIds() {
        List<Integer> ids = new ArrayList<>();
        for (Map.Entry<Integer, CheckBox> entry : userCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) ids.add(entry.getKey());
        }
        return ids;
    }

    // ================= BULK ACTIONS =================
    @FXML private void handleBulkActivate() {
        List<Integer> ids = getSelectedUserIds();
        if (ids.isEmpty()) { showStatus(userStatusLabel, "Selectionnez des utilisateurs", false); return; }
        int count = 0;
        for (int id : ids) {
            if (userService.unblockUser(id)) {
                auditService.log(currentAdminId, currentUserName, "UNBLOCK_USER", "USER", id, "User #" + id, "BLOQUE", "ACTIF");
                count++;
            }
        }
        showStatus(userStatusLabel, count + " utilisateur(s) active(s)", true);
        loadUsersTable();
    }

    @FXML private void handleBulkBlock() {
        List<Integer> ids = getSelectedUserIds();
        if (ids.isEmpty()) { showStatus(userStatusLabel, "Selectionnez des utilisateurs", false); return; }
        int count = 0;
        for (int id : ids) {
            if (userService.blockUser(id)) {
                auditService.log(currentAdminId, currentUserName, "BLOCK_USER", "USER", id, "User #" + id, "ACTIF", "BLOQUE");
                count++;
            }
        }
        showStatus(userStatusLabel, count + " utilisateur(s) bloque(s)", true);
        loadUsersTable();
    }

    @FXML private void handleBulkDelete() {
        List<Integer> ids = getSelectedUserIds();
        if (ids.isEmpty()) { showStatus(userStatusLabel, "Selectionnez des utilisateurs", false); return; }

        Alert confirm = createConfirmDialog("Supprimer " + ids.size() + " utilisateur(s) ?",
                "Cette action est irreversible. Les utilisateurs selectionnes seront definitivement supprimes.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int count = 0;
            for (int id : ids) {
                if (userService.deleteAccount(id)) {
                    auditService.log(currentAdminId, currentUserName, "DELETE_USER", "USER", id, "User #" + id, null, null);
                    count++;
                }
            }
            showStatus(userStatusLabel, count + " utilisateur(s) supprime(s)", true);
            loadUsersTable();
        }
    }

    @FXML private void handleBulkRoleChange() {
        List<Integer> ids = getSelectedUserIds();
        if (ids.isEmpty()) { showStatus(userStatusLabel, "Selectionnez des utilisateurs", false); return; }
        if (bulkRoleCombo == null || bulkRoleCombo.getValue() == null) {
            showStatus(userStatusLabel, "Selectionnez un role", false); return;
        }

        String roleStr = bulkRoleCombo.getValue();
        int newRoleId;
        try { newRoleId = Integer.parseInt(roleStr.split(" - ")[0].trim()); }
        catch (Exception e) { showStatus(userStatusLabel, "Role invalide", false); return; }

        int count = 0;
        for (int id : ids) {
            try {
                utilisateur u = userService.getUserById(id);
                if (u != null) {
                    String oldRole = userService.getRoleName(u.getRoleId());
                    u.setRoleId(newRoleId);
                    userService.modifier(u);
                    String newRole = userService.getRoleName(newRoleId);
                    auditService.log(currentAdminId, currentUserName, "ROLE_CHANGE", "USER", id,
                            u.getPrenom() + " " + u.getNom(), oldRole, newRole);
                    count++;
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        showStatus(userStatusLabel, count + " role(s) modifie(s)", true);
        loadUsersTable();
    }

    @FXML private void handlePrevPage() {
        if (currentPage > 1) { currentPage--; loadUsersTable(); }
    }

    @FXML private void handleNextPage() {
        int totalCount = currentSearchQuery.isEmpty() ? userService.countAll() : userService.countSearch(currentSearchQuery);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        if (currentPage < totalPages) { currentPage++; loadUsersTable(); }
    }

    // ================= EXPORT =================
    @FXML private void handleExportPDF() { exportUsers("PDF"); }
    @FXML private void handleExportExcel() { exportUsers("Excel"); }
    @FXML private void handleExportCSV() { exportUsers("CSV"); }

    private void exportUsers(String format) {
        try {
            List<utilisateur> users = userService.afficher();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exporter en " + format);
            switch (format) {
                case "PDF": fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf")); fc.setInitialFileName("utilisateurs.pdf"); break;
                case "Excel": fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")); fc.setInitialFileName("utilisateurs.xlsx"); break;
                case "CSV": fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv")); fc.setInitialFileName("utilisateurs.csv"); break;
            }
            Stage stage = (Stage) contentArea.getScene().getWindow();
            File file = fc.showSaveDialog(stage);
            if (file == null) return;

            boolean success = false;
            switch (format) {
                case "PDF": success = exportService.exportToPDF(users, file); break;
                case "Excel": success = exportService.exportToExcel(users, file); break;
                case "CSV": success = exportService.exportToCSV(users, file); break;
            }

            if (success) {
                auditService.log(currentAdminId, currentUserName, "EXPORT_" + format.toUpperCase(), "DATA", 0, file.getName(), null, users.size() + " utilisateurs");
                showStatus(userStatusLabel, "Export " + format + " reussi ! (" + users.size() + " utilisateurs)", true);
            } else {
                showStatus(userStatusLabel, "Erreur lors de l'export", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showStatus(userStatusLabel, "Erreur: " + e.getMessage(), false);
        }
    }

    // ================= IMPORT =================
    @FXML private void handleImportUsers() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importer des utilisateurs");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV / Excel", "*.csv", "*.xlsx"),
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        Stage stage = (Stage) contentArea.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        ImportService.ImportResult result;
        if (file.getName().toLowerCase().endsWith(".xlsx")) {
            result = importService.importFromExcel(file);
        } else {
            result = importService.importFromCSV(file);
        }
        auditService.log(currentAdminId, currentUserName, "IMPORT_USERS", "DATA", 0, file.getName(), null, result.getSummary());

        StringBuilder msg = new StringBuilder(result.getSummary());
        if (!result.errors.isEmpty()) {
            msg.append("\n\nErreurs:\n");
            for (int i = 0; i < Math.min(result.errors.size(), 10); i++) {
                msg.append("- ").append(result.errors.get(i)).append("\n");
            }
            if (result.errors.size() > 10) {
                msg.append("... et ").append(result.errors.size() - 10).append(" autres erreurs");
            }
        }
        showAlert("Resultat de l'import", msg.toString());
        loadUsersTable();
    }

    // ================= AUDIT TRAIL =================
    private void loadAuditTrail() {
        if (auditTableContainer == null) return;
        auditTableContainer.getChildren().clear();

        if (auditActionFilter != null && auditActionFilter.getItems().isEmpty()) {
            List<String> actions = auditService.getDistinctActions();
            actions.add(0, "Toutes");
            auditActionFilter.setItems(FXCollections.observableArrayList(actions));
        }
        List<AuditLog> logs = auditService.getAll();
        displayAuditLogs(logs);
    }

    @FXML private void handleFilterAudit() {
        String actionFilter = auditActionFilter != null ? auditActionFilter.getValue() : null;
        String dateFrom = auditDateFrom != null && auditDateFrom.getValue() != null ? auditDateFrom.getValue().toString() : null;
        String dateTo = auditDateTo != null && auditDateTo.getValue() != null ? auditDateTo.getValue().toString() : null;
        String search = auditSearchField != null ? auditSearchField.getText().trim() : "";
        List<AuditLog> logs = auditService.filter(actionFilter, dateFrom, dateTo, search.isEmpty() ? null : search);
        auditTableContainer.getChildren().clear();
        displayAuditLogs(logs);
    }

    @FXML private void handleResetAuditFilter() {
        if (auditActionFilter != null) auditActionFilter.setValue(null);
        if (auditDateFrom != null) auditDateFrom.setValue(null);
        if (auditDateTo != null) auditDateTo.setValue(null);
        if (auditSearchField != null) auditSearchField.clear();
        loadAuditTrail();
    }

    private void displayAuditLogs(List<AuditLog> logs) {
        if (auditCountLabel != null) auditCountLabel.setText(logs.size() + " entree(s) trouvee(s)");

        if (logs.isEmpty()) {
            Label empty = new Label("Aucune entree d'audit trouvee");
            empty.setStyle("-fx-text-fill: #666666; -fx-font-size: 13; -fx-padding: 20;");
            auditTableContainer.getChildren().add(empty);
            return;
        }

        auditCarouselList = logs;
        auditCarouselIndex = 0;
        buildAuditCarousel();
    }

    private void buildAuditCarousel() {
        if (auditTableContainer == null) return;
        auditTableContainer.getChildren().clear();

        int totalPages = (int) Math.ceil((double) auditCarouselList.size() / AUDIT_CAROUSEL_SIZE);
        int currentCarouselPage = auditCarouselIndex / AUDIT_CAROUSEL_SIZE;

        // Navigation bar
        HBox navBar = new HBox(12);
        navBar.setAlignment(Pos.CENTER);
        navBar.setStyle("-fx-padding: 0 0 10 0;");

        Button prevBtn = new Button("\u25C0  Precedent");
        prevBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFAA00); -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        prevBtn.setDisable(auditCarouselIndex == 0);
        prevBtn.setOnAction(e -> {
            auditCarouselIndex = Math.max(0, auditCarouselIndex - AUDIT_CAROUSEL_SIZE);
            buildAuditCarousel();
        });

        Label pageIndicator = new Label((currentCarouselPage + 1) + " / " + totalPages);
        pageIndicator.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");

        Button nextBtn = new Button("Suivant  \u25B6");
        nextBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFAA00); -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        nextBtn.setDisable(auditCarouselIndex + AUDIT_CAROUSEL_SIZE >= auditCarouselList.size());
        nextBtn.setOnAction(e -> {
            auditCarouselIndex = Math.min(auditCarouselList.size() - 1, auditCarouselIndex + AUDIT_CAROUSEL_SIZE);
            buildAuditCarousel();
        });

        HBox dots = new HBox(6);
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < totalPages; i++) {
            Label dot = new Label("\u25CF");
            dot.setStyle(i == currentCarouselPage ? "-fx-text-fill: #FFD700; -fx-font-size: 10;" : "-fx-text-fill: #444444; -fx-font-size: 10;");
            dots.getChildren().add(dot);
        }

        navBar.getChildren().addAll(prevBtn, dots, pageIndicator, nextBtn);
        auditTableContainer.getChildren().add(navBar);

        // Cards grid (2 columns x 3 rows = 6 per page)
        VBox cardsColumn = new VBox(10);
        cardsColumn.setAlignment(Pos.CENTER);

        int end = Math.min(auditCarouselIndex + AUDIT_CAROUSEL_SIZE, auditCarouselList.size());
        HBox currentRow = null;
        for (int i = auditCarouselIndex; i < end; i++) {
            if ((i - auditCarouselIndex) % 2 == 0) {
                currentRow = new HBox(14);
                currentRow.setAlignment(Pos.CENTER);
                cardsColumn.getChildren().add(currentRow);
            }
            AuditLog log = auditCarouselList.get(i);
            VBox card = buildAuditCard(log);
            HBox.setHgrow(card, Priority.ALWAYS);
            if (currentRow != null) currentRow.getChildren().add(card);
        }

        cardsColumn.setOpacity(0);
        cardsColumn.setTranslateX(15);
        auditTableContainer.getChildren().add(cardsColumn);

        FadeTransition fade = new FadeTransition(Duration.millis(300), cardsColumn);
        fade.setFromValue(0); fade.setToValue(1); fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), cardsColumn);
        slide.setFromX(15); slide.setToX(0); slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    private VBox buildAuditCard(AuditLog log) {
        String dateStr = log.getCreatedAt() != null ? log.getCreatedAt().toString().substring(0, 19) : "-";
        String actionColor = getActionColor(log.getAction());
        String actionBg = "rgba(100,181,246,0.08)";
        if (log.getAction() != null) {
            if (log.getAction().contains("DELETE") || log.getAction().contains("BLOCK")) actionBg = "rgba(255,107,107,0.08)";
            else if (log.getAction().contains("APPROVE") || log.getAction().contains("UNBLOCK")) actionBg = "rgba(81,207,102,0.08)";
        }

        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: rgba(18,18,18,0.95); -fx-padding: 14; -fx-background-radius: 12; "
                + "-fx-border-color: rgba(255,215,0,0.08); -fx-border-radius: 12; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0.1, 0, 2);");
        card.setMinWidth(300);
        card.setMaxWidth(400);

        // Action badge
        Label actionLabel = new Label(log.getAction() != null ? log.getAction() : "-");
        actionLabel.setStyle("-fx-text-fill: " + actionColor + "; -fx-font-size: 11; -fx-font-weight: bold; "
                + "-fx-background-color: " + actionBg + "; -fx-padding: 4 12; -fx-background-radius: 10;");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label adminLabel = new Label("\uD83D\uDC64 " + (log.getAdminName() != null ? log.getAdminName() : "-"));
        adminLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11; -fx-font-weight: bold;");
        Label dateLabel = new Label("\uD83D\uDD52 " + dateStr);
        dateLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 9;");
        topRow.getChildren().addAll(adminLabel, dateLabel);

        Label targetLabel = new Label("\uD83C\uDFAF Cible: " + (log.getTargetName() != null ? log.getTargetName() : "-"));
        targetLabel.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 10;");
        targetLabel.setWrapText(true);

        HBox valuesRow = new HBox(12);
        valuesRow.setAlignment(Pos.CENTER_LEFT);
        Label oldVal = new Label("\u274C " + (log.getOldValue() != null ? log.getOldValue() : "-"));
        oldVal.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 9;");
        oldVal.setWrapText(true);
        Label arrow = new Label("\u27A1");
        arrow.setStyle("-fx-text-fill: #888888; -fx-font-size: 10;");
        Label newVal = new Label("\u2705 " + (log.getNewValue() != null ? log.getNewValue() : "-"));
        newVal.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 9;");
        newVal.setWrapText(true);
        valuesRow.getChildren().addAll(oldVal, arrow, newVal);

        card.getChildren().addAll(actionLabel, topRow, targetLabel, valuesRow);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace("rgba(255,215,0,0.08)", "rgba(255,215,0,0.2)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("rgba(255,215,0,0.2)", "rgba(255,215,0,0.08)")));

        return card;
    }

    private String getActionColor(String action) {
        if (action == null) return "#cccccc";
        if (action.contains("DELETE")) return "#FF6B6B";
        if (action.contains("BLOCK")) return "#FF6B6B";
        if (action.contains("UNBLOCK")) return "#51CF66";
        if (action.contains("EXPORT")) return "#64B5F6";
        if (action.contains("IMPORT")) return "#FFB74D";
        if (action.contains("ROLE")) return "#CE93D8";
        if (action.contains("APPROVE")) return "#51CF66";
        return "#cccccc";
    }

    // ================= ROLE PERMISSIONS MATRIX =================
    private void loadPermissionsMatrix() {
        if (permissionsGrid == null) return;
        permissionsGrid.getChildren().clear();
        permCheckboxMap.clear();

        List<role> roles;
        try { roles = roleService.afficher(); } catch (Exception e) { e.printStackTrace(); return; }

        List<Integer> roleIds = new ArrayList<>();
        for (role r : roles) roleIds.add(r.getId());
        permService.ensureAllPermissions(roleIds);

        Map<Integer, List<RolePermission>> allPerms = permService.getAllPermissions();

        HBox headerRow = new HBox(0);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setStyle("-fx-background-color: rgba(255,215,0,0.08); -fx-padding: 12 16; -fx-border-color: transparent transparent rgba(255,215,0,0.08) transparent; -fx-border-width: 0 0 1 0;");
        Label permHeader = new Label("Permission");
        permHeader.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");
        permHeader.setMinWidth(220); permHeader.setMaxWidth(220);
        headerRow.getChildren().add(permHeader);

        for (role r : roles) {
            Label roleHeader = new Label(r.getNom());
            roleHeader.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");
            roleHeader.setMinWidth(120); roleHeader.setMaxWidth(120); roleHeader.setAlignment(Pos.CENTER);
            headerRow.getChildren().add(roleHeader);
        }
        permissionsGrid.getChildren().add(headerRow);

        for (String perm : RolePermissionService.ALL_PERMISSIONS) {
            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            String bgColor = permissionsGrid.getChildren().size() % 2 == 0 ? "#0d0d0d" : "#111111";
            row.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 10 16;");

            String label = RolePermissionService.PERMISSION_LABELS.getOrDefault(perm, perm);
            Label permLabel = new Label(label);
            permLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12;");
            permLabel.setMinWidth(220); permLabel.setMaxWidth(220);
            row.getChildren().add(permLabel);

            for (role r : roles) {
                boolean enabled = false;
                List<RolePermission> rolePerms = allPerms.getOrDefault(r.getId(), new ArrayList<>());
                for (RolePermission rp : rolePerms) {
                    if (rp.getPermissionName().equals(perm)) { enabled = rp.isEnabled(); break; }
                }
                CheckBox cb = new CheckBox();
                cb.setSelected(enabled);
                cb.getStyleClass().add("custom-checkbox");
                int key = r.getId() * 10000 + perm.hashCode();
                permCheckboxMap.put(key, cb);
                cb.setUserData(r.getId() + "|" + perm);

                StackPane wrapper = new StackPane(cb);
                wrapper.setMinWidth(120); wrapper.setMaxWidth(120); wrapper.setAlignment(Pos.CENTER);
                row.getChildren().add(wrapper);
            }
            permissionsGrid.getChildren().add(row);
        }
    }

    @FXML private void handleSavePermissions() {
        int updated = 0;
        for (CheckBox cb : permCheckboxMap.values()) {
            String data = (String) cb.getUserData();
            if (data == null) continue;
            String[] parts = data.split("\\|");
            int roleId = Integer.parseInt(parts[0]);
            String permName = parts[1];
            boolean enabled = cb.isSelected();
            if (permService.updatePermission(roleId, permName, enabled)) updated++;
        }
        auditService.log(currentAdminId, currentUserName, "UPDATE_PERMISSIONS", "ROLE", 0, "All Roles", null, updated + " permissions updated");
        showStatus(permissionsMessageLabel, "Permissions sauvegardees ! (" + updated + " mises a jour)", true);
    }

    // =============================================================
    //                    PANIER ADMIN PANEL
    // =============================================================
    private void loadAdminPaniers(Integer filterUserId) {
        if (panierAdminContainer == null) return;
        panierAdminContainer.getChildren().clear();
        currentPanierFilterUserId = filterUserId;

        List<Panier> allPaniers;
        if (filterUserId != null && filterUserId > 0) {
            allPaniers = panierService.getPanierByClient(filterUserId);
        } else {
            allPaniers = panierService.getAll();
        }

        if (panierAdminCountLabel != null) panierAdminCountLabel.setText(allPaniers.size() + " panier(s)");

        if (allPaniers.isEmpty()) {
            Label empty = new Label("Aucun panier trouve");
            empty.setStyle("-fx-text-fill: #666666; -fx-font-size: 13; -fx-padding: 20;");
            panierAdminContainer.getChildren().add(empty);
            return;
        }

        panierCarouselList = allPaniers;
        panierCarouselIndex = 0;
        buildPanierCarousel();
    }

    private void buildPanierCarousel() {
        if (panierAdminContainer == null) return;
        panierAdminContainer.getChildren().clear();

        int totalPages = (int) Math.ceil((double) panierCarouselList.size() / PANIER_CAROUSEL_SIZE);
        int currentCarouselPage = panierCarouselIndex / PANIER_CAROUSEL_SIZE;

        // Navigation bar
        HBox navBar = new HBox(12);
        navBar.setAlignment(Pos.CENTER);
        navBar.setStyle("-fx-padding: 0 0 10 0;");

        Button prevBtn = new Button("\u25C0  Precedent");
        prevBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFAA00); -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        prevBtn.setDisable(panierCarouselIndex == 0);
        prevBtn.setOnAction(e -> {
            panierCarouselIndex = Math.max(0, panierCarouselIndex - PANIER_CAROUSEL_SIZE);
            buildPanierCarousel();
        });

        Label pageIndicator = new Label((currentCarouselPage + 1) + " / " + totalPages);
        pageIndicator.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");

        Button nextBtn = new Button("Suivant  \u25B6");
        nextBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFAA00); -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        nextBtn.setDisable(panierCarouselIndex + PANIER_CAROUSEL_SIZE >= panierCarouselList.size());
        nextBtn.setOnAction(e -> {
            panierCarouselIndex = Math.min(panierCarouselList.size() - 1, panierCarouselIndex + PANIER_CAROUSEL_SIZE);
            buildPanierCarousel();
        });

        HBox dots = new HBox(6);
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < totalPages; i++) {
            Label dot = new Label("\u25CF");
            dot.setStyle(i == currentCarouselPage ? "-fx-text-fill: #FFD700; -fx-font-size: 10;" : "-fx-text-fill: #444444; -fx-font-size: 10;");
            dots.getChildren().add(dot);
        }

        navBar.getChildren().addAll(prevBtn, dots, pageIndicator, nextBtn);
        panierAdminContainer.getChildren().add(navBar);

        // Cards grid (2 columns)
        VBox cardsColumn = new VBox(12);
        cardsColumn.setAlignment(Pos.CENTER);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        int end = Math.min(panierCarouselIndex + PANIER_CAROUSEL_SIZE, panierCarouselList.size());
        HBox currentRow = null;
        for (int i = panierCarouselIndex; i < end; i++) {
            if ((i - panierCarouselIndex) % 2 == 0) {
                currentRow = new HBox(14);
                currentRow.setAlignment(Pos.CENTER);
                cardsColumn.getChildren().add(currentRow);
            }
            Panier p = panierCarouselList.get(i);
            VBox card = buildPanierCard(p, dtf, currentPanierFilterUserId);
            HBox.setHgrow(card, Priority.ALWAYS);
            if (currentRow != null) currentRow.getChildren().add(card);
        }

        cardsColumn.setOpacity(0);
        cardsColumn.setTranslateX(15);
        panierAdminContainer.getChildren().add(cardsColumn);

        FadeTransition fade = new FadeTransition(Duration.millis(300), cardsColumn);
        fade.setFromValue(0); fade.setToValue(1); fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), cardsColumn);
        slide.setFromX(15); slide.setToX(0); slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    private VBox buildPanierCard(Panier p, DateTimeFormatter dtf, Integer filterUserId) {
        String clientName = p.getNomClient() != null ? p.getNomClient().trim() : "Client #" + p.getIdClient();
        String dateDebut = p.getDateDebut() != null ? p.getDateDebut().format(dtf) : "-";
        String dateFin = p.getDateFin() != null ? p.getDateFin().format(dtf) : "-";
        String statut = p.getStatutItem() != null ? p.getStatutItem() : "-";
        boolean isRestoAdmin = isRestoOrCafeAdmin(p.getTypeService());

        String statutColor = "#888888";
        String statutBg = "rgba(136,136,136,0.1)";
        if ("en_attente".equalsIgnoreCase(statut)) { statutColor = "#FFA726"; statutBg = "rgba(255,167,38,0.1)"; }
        else if ("confirme".equalsIgnoreCase(statut)) { statutColor = "#51CF66"; statutBg = "rgba(81,207,102,0.1)"; }
        else if ("annul\u00e9".equalsIgnoreCase(statut)) { statutColor = "#FF6B6B"; statutBg = "rgba(255,107,107,0.1)"; }

        String prixDisplay = isRestoAdmin ? "\uD83C\uDF7D Sur place" : String.format("%.2f DT", p.getPrixEstime());

        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(18,18,18,0.95); -fx-padding: 16; -fx-background-radius: 14; "
                + "-fx-border-color: rgba(255,215,0,0.08); -fx-border-radius: 14; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0.15, 0, 3);");
        card.setMinWidth(280);
        card.setMaxWidth(380);

        // Top row: ID + Status badge
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label idLabel = new Label("#" + p.getIdPanier());
        idLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 12;");
        Label statutLabel = new Label(statut);
        statutLabel.setStyle("-fx-text-fill: " + statutColor + "; -fx-font-size: 10; -fx-font-weight: bold; "
                + "-fx-background-color: " + statutBg + "; -fx-padding: 3 10; -fx-background-radius: 10;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(idLabel, spacer, statutLabel);

        // Client
        Label clientLabel = new Label("\uD83D\uDC64 " + clientName);
        clientLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12; -fx-font-weight: bold;");
        clientLabel.setWrapText(true);

        // Service badge
        Label serviceLabel = new Label("\uD83C\uDFF7 " + (p.getTypeService() != null ? p.getTypeService() : "-"));
        serviceLabel.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 10; -fx-font-weight: bold;");

        // Dates
        HBox datesRow = new HBox(8);
        datesRow.setAlignment(Pos.CENTER_LEFT);
        Label fromLabel = new Label("\uD83D\uDCC5 " + dateDebut);
        fromLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 9;");
        Label toLabel = new Label("\u27A1 " + dateFin);
        toLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 9;");
        datesRow.getChildren().addAll(fromLabel, toLabel);

        // Persons + Price
        HBox detailsRow = new HBox(12);
        detailsRow.setAlignment(Pos.CENTER_LEFT);
        Label persLabel = new Label("\uD83D\uDC65 " + p.getNbPersonnes() + " pers.");
        persLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10;");
        Label prixLabel = new Label("\uD83D\uDCB0 " + prixDisplay);
        prixLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11; -fx-font-weight: bold;");
        detailsRow.getChildren().addAll(persLabel, prixLabel);

        card.getChildren().addAll(topRow, clientLabel, serviceLabel, datesRow, detailsRow);

        // Action buttons
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER);
        actions.setStyle("-fx-padding: 6 0 0 0;");

        if (!isRestoAdmin && "en_attente".equalsIgnoreCase(statut)) {
            Button confirmBtn = new Button("\u2705 Confirmer");
            confirmBtn.setStyle("-fx-background-color: #2d7a3a; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 5 14; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;");
            confirmBtn.setOnAction(e -> {
                p.setStatutItem("confirme");
                panierService.modifier(p);
                auditService.log(currentAdminId, currentUserName, "CONFIRM_PANIER", "PANIER", p.getIdPanier(), "Panier #" + p.getIdPanier(), "en_attente", "confirme");
                loadAdminPaniers(filterUserId);
            });
            actions.getChildren().add(confirmBtn);

            Button cancelBtn = new Button("\u274C Annuler");
            cancelBtn.setStyle("-fx-background-color: #8B0000; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 5 12; -fx-background-radius: 8; -fx-cursor: hand;");
            cancelBtn.setOnAction(e -> {
                p.setStatutItem("annul\u00e9");
                panierService.modifier(p);
                auditService.log(currentAdminId, currentUserName, "CANCEL_PANIER", "PANIER", p.getIdPanier(), "Panier #" + p.getIdPanier(), "en_attente", "annul\u00e9");
                loadAdminPaniers(filterUserId);
            });
            actions.getChildren().add(cancelBtn);
        }

        Button deleteBtn = new Button("\uD83D\uDDD1 Supprimer");
        deleteBtn.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; -fx-font-size: 9; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            Alert confirm = createConfirmDialog("Supprimer le panier #" + p.getIdPanier() + " ?", "Cette action est irreversible.");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                panierService.supprimer(p.getIdPanier());
                auditService.log(currentAdminId, currentUserName, "DELETE_PANIER", "PANIER", p.getIdPanier(), "Panier #" + p.getIdPanier(), null, null);
                loadAdminPaniers(filterUserId);
            }
        });
        actions.getChildren().add(deleteBtn);

        card.getChildren().add(actions);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace("rgba(255,215,0,0.08)", "rgba(255,215,0,0.2)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("rgba(255,215,0,0.2)", "rgba(255,215,0,0.08)")));

        return card;
    }

    @FXML private void handleShowPanierAdmin() {
        setActiveNav(btnPanierAdmin);
        loadAdminPaniers(null);
        showPane(panierAdminPane);
    }

    @FXML private void handleFilterPanierByUser() {
        if (panierUserFilter == null || panierUserFilter.getValue() == null) return;
        Integer userId = userNameToIdMap.get(panierUserFilter.getValue());
        loadAdminPaniers(userId);
    }

    @FXML private void handleShowAllPaniers() {
        if (panierUserFilter != null) panierUserFilter.setValue(null);
        loadAdminPaniers(null);
    }

    // =============================================================
    //                    RESERVATION ADMIN PANEL
    // =============================================================
    private void loadAdminReservations(Integer filterUserId, String filterStatus) {
        if (reservationAdminContainer == null) return;
        reservationAdminContainer.getChildren().clear();
        currentResFilterUserId = filterUserId;
        currentResFilterStatus = filterStatus;

        List<Reservation> allRes;
        if (filterUserId != null && filterUserId > 0) {
            allRes = reservationService.getReservationsByClient(filterUserId);
        } else {
            allRes = reservationService.getAll();
        }

        // Apply status filter
        if (filterStatus != null && !filterStatus.isEmpty() && !"Tous".equals(filterStatus)) {
            List<Reservation> filtered = new ArrayList<>();
            for (Reservation r : allRes) {
                if (filterStatus.equalsIgnoreCase(r.getStatutPaiement())) filtered.add(r);
            }
            allRes = filtered;
        }

        // Stats
        int totalRes = allRes.size();
        int pendingCount = 0;
        int paidCount = 0;
        double totalRevenue = 0;
        for (Reservation r : allRes) {
            String sp = r.getStatutPaiement() != null ? r.getStatutPaiement() : "";
            if ("En cours de paiement".equalsIgnoreCase(sp)) pendingCount++;
            else if ("Paye".equalsIgnoreCase(sp) || "Pay\u00e9".equalsIgnoreCase(sp)) {
                paidCount++;
                if (!isRestoOrCafeAdmin(r.getTypeService())) {
                    totalRevenue += r.getMontantTotal();
                }
            }
        }
        if (adminTotalResLabel != null) adminTotalResLabel.setText(String.valueOf(totalRes));
        if (adminPendingResLabel != null) adminPendingResLabel.setText(String.valueOf(pendingCount));
        if (adminPaidResLabel != null) adminPaidResLabel.setText(String.valueOf(paidCount));
        if (adminRevenueLabel != null) adminRevenueLabel.setText(String.format("%.2f DT", totalRevenue));
        if (resAdminCountLabel != null) resAdminCountLabel.setText(totalRes + " reservation(s)");

        if (allRes.isEmpty()) {
            Label empty = new Label("Aucune reservation trouvee");
            empty.setStyle("-fx-text-fill: #666666; -fx-font-size: 13; -fx-padding: 20;");
            reservationAdminContainer.getChildren().add(empty);
            return;
        }

        resCarouselList = allRes;
        resCarouselIndex = 0;
        buildResCarousel();
    }

    private void buildResCarousel() {
        if (reservationAdminContainer == null) return;
        reservationAdminContainer.getChildren().clear();

        int totalPages = (int) Math.ceil((double) resCarouselList.size() / RES_CAROUSEL_SIZE);
        int currentCarouselPage = resCarouselIndex / RES_CAROUSEL_SIZE;

        // Navigation bar
        HBox navBar = new HBox(12);
        navBar.setAlignment(Pos.CENTER);
        navBar.setStyle("-fx-padding: 0 0 10 0;");

        Button prevBtn = new Button("\u25C0  Precedent");
        prevBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFAA00); -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        prevBtn.setDisable(resCarouselIndex == 0);
        prevBtn.setOnAction(e -> {
            resCarouselIndex = Math.max(0, resCarouselIndex - RES_CAROUSEL_SIZE);
            buildResCarousel();
        });

        Label pageIndicator = new Label((currentCarouselPage + 1) + " / " + totalPages);
        pageIndicator.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");

        Button nextBtn = new Button("Suivant  \u25B6");
        nextBtn.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFAA00); -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        nextBtn.setDisable(resCarouselIndex + RES_CAROUSEL_SIZE >= resCarouselList.size());
        nextBtn.setOnAction(e -> {
            resCarouselIndex = Math.min(resCarouselList.size() - 1, resCarouselIndex + RES_CAROUSEL_SIZE);
            buildResCarousel();
        });

        HBox dots = new HBox(6);
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < totalPages; i++) {
            Label dot = new Label("\u25CF");
            dot.setStyle(i == currentCarouselPage ? "-fx-text-fill: #FFD700; -fx-font-size: 10;" : "-fx-text-fill: #444444; -fx-font-size: 10;");
            dots.getChildren().add(dot);
        }

        navBar.getChildren().addAll(prevBtn, dots, pageIndicator, nextBtn);
        reservationAdminContainer.getChildren().add(navBar);

        // Cards grid (2 columns)
        VBox cardsColumn = new VBox(12);
        cardsColumn.setAlignment(Pos.CENTER);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        int end = Math.min(resCarouselIndex + RES_CAROUSEL_SIZE, resCarouselList.size());
        HBox currentRow = null;
        for (int i = resCarouselIndex; i < end; i++) {
            if ((i - resCarouselIndex) % 2 == 0) {
                currentRow = new HBox(14);
                currentRow.setAlignment(Pos.CENTER);
                cardsColumn.getChildren().add(currentRow);
            }
            Reservation r = resCarouselList.get(i);
            VBox card = buildResCard(r, dtf, currentResFilterUserId, currentResFilterStatus);
            HBox.setHgrow(card, Priority.ALWAYS);
            if (currentRow != null) currentRow.getChildren().add(card);
        }

        cardsColumn.setOpacity(0);
        cardsColumn.setTranslateX(15);
        reservationAdminContainer.getChildren().add(cardsColumn);

        FadeTransition fade = new FadeTransition(Duration.millis(300), cardsColumn);
        fade.setFromValue(0); fade.setToValue(1); fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), cardsColumn);
        slide.setFromX(15); slide.setToX(0); slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    private VBox buildResCard(Reservation r, DateTimeFormatter dtf, Integer filterUserId, String filterStatus) {
        // Get client name
        String tempName = "Client #" + r.getIdClient();
        try {
            utilisateur u = userService.getUserById(r.getIdClient());
            if (u != null) tempName = u.getPrenom() + " " + u.getNom();
        } catch (Exception ignored) {}
        final String clientName = tempName;

        String dateStr = r.getDatePaiement() != null ? r.getDatePaiement().format(dtf) : "-";
        String statut = r.getStatutPaiement() != null ? r.getStatutPaiement() : "-";
        String mode = r.getModePaiement() != null ? r.getModePaiement() : "-";
        String code = r.getCodeConfirmation() != null ? r.getCodeConfirmation() : "-";

        String statutColor = "#888888";
        String statutBg = "rgba(136,136,136,0.1)";
        if ("En cours de paiement".equalsIgnoreCase(statut)) { statutColor = "#FFA726"; statutBg = "rgba(255,167,38,0.1)"; }
        else if ("Paye".equalsIgnoreCase(statut) || "Pay\u00e9".equalsIgnoreCase(statut)) { statutColor = "#51CF66"; statutBg = "rgba(81,207,102,0.1)"; }
        else if ("Rembours\u00e9".equalsIgnoreCase(statut)) { statutColor = "#FF6B6B"; statutBg = "rgba(255,107,107,0.1)"; }

        String modeColor = "#cccccc";
        if ("Especes".equalsIgnoreCase(mode) || "Cash".equalsIgnoreCase(mode)) modeColor = "#FFA726";
        else if ("Carte".equalsIgnoreCase(mode) || "Card".equalsIgnoreCase(mode)) modeColor = "#64B5F6";

        boolean isRestoRes = isRestoOrCafeAdmin(r.getTypeService());
        String montantDisplay = isRestoRes ? "\uD83C\uDF7D Sur place" : String.format("%.2f DT", r.getMontantTotal());

        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(18,18,18,0.95); -fx-padding: 16; -fx-background-radius: 14; "
                + "-fx-border-color: rgba(255,215,0,0.08); -fx-border-radius: 14; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0.15, 0, 3);");
        card.setMinWidth(280);
        card.setMaxWidth(380);

        // Top row: ID + Status badge
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label idLabel = new Label("#" + r.getIdReservation());
        idLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 12;");
        Label statutLabel = new Label(statut);
        statutLabel.setStyle("-fx-text-fill: " + statutColor + "; -fx-font-size: 10; -fx-font-weight: bold; "
                + "-fx-background-color: " + statutBg + "; -fx-padding: 3 10; -fx-background-radius: 10;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(idLabel, spacer, statutLabel);

        // Client
        Label clientLabel = new Label("\uD83D\uDC64 " + clientName);
        clientLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12; -fx-font-weight: bold;");
        clientLabel.setWrapText(true);

        // Service + Date
        HBox serviceRow = new HBox(10);
        serviceRow.setAlignment(Pos.CENTER_LEFT);
        Label serviceLabel = new Label("\uD83C\uDFF7 " + (r.getTypeService() != null ? r.getTypeService() : "-"));
        serviceLabel.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 10; -fx-font-weight: bold;");
        Label dateLabel = new Label("\uD83D\uDCC5 " + dateStr);
        dateLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 9;");
        serviceRow.getChildren().addAll(serviceLabel, dateLabel);

        // Amount + Mode + Code
        HBox detailsRow = new HBox(10);
        detailsRow.setAlignment(Pos.CENTER_LEFT);
        Label montantLabel = new Label("\uD83D\uDCB0 " + montantDisplay);
        montantLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11; -fx-font-weight: bold;");
        Label modeLabel = new Label("\uD83D\uDCB3 " + mode);
        modeLabel.setStyle("-fx-text-fill: " + modeColor + "; -fx-font-size: 10;");
        detailsRow.getChildren().addAll(montantLabel, modeLabel);

        Label codeLabel = new Label("\uD83D\uDD11 " + code);
        codeLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 9;");

        card.getChildren().addAll(topRow, clientLabel, serviceRow, detailsRow, codeLabel);

        // Action buttons
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER);
        actions.setStyle("-fx-padding: 6 0 0 0;");

        if (!isRestoRes && "En cours de paiement".equalsIgnoreCase(statut)) {
            Button approveBtn = new Button("\u2705 Approuver");
            approveBtn.setStyle("-fx-background-color: linear-gradient(to right, #2d7a3a, #1a5e28); -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 5 14; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;");
            approveBtn.setOnAction(e -> {
                Alert confirm = createConfirmDialog("Approuver le paiement ?",
                    "Reservation #" + r.getIdReservation() + " - " + clientName + "\nMontant: " + String.format("%.2f DT", r.getMontantTotal()) + "\nMode: " + mode + "\n\nLe statut sera change en 'Paye'.");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    reservationService.updateStatut(r.getIdReservation(), "Paye");
                    auditService.log(currentAdminId, currentUserName, "APPROVE_PAYMENT", "RESERVATION", r.getIdReservation(),
                        "Reservation #" + r.getIdReservation() + " (" + clientName + ")", "En cours de paiement", "Paye");
                    loadAdminReservations(filterUserId, filterStatus);
                }
            });
            actions.getChildren().add(approveBtn);

            Button rejectBtn = new Button("\u274C Rejeter");
            rejectBtn.setStyle("-fx-background-color: #8B0000; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 5 12; -fx-background-radius: 8; -fx-cursor: hand;");
            rejectBtn.setOnAction(e -> {
                Alert confirm = createConfirmDialog("Rejeter le paiement ?",
                    "La reservation #" + r.getIdReservation() + " sera marquee comme remboursee.");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    reservationService.updateStatut(r.getIdReservation(), "Rembours\u00e9");
                    auditService.log(currentAdminId, currentUserName, "REJECT_PAYMENT", "RESERVATION", r.getIdReservation(),
                        "Reservation #" + r.getIdReservation() + " (" + clientName + ")", "En cours de paiement", "Rembours\u00e9");
                    loadAdminReservations(filterUserId, filterStatus);
                }
            });
            actions.getChildren().add(rejectBtn);
        }

        Button deleteBtn = new Button("\uD83D\uDDD1 Supprimer");
        deleteBtn.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; -fx-font-size: 9; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            Alert confirm = createConfirmDialog("Supprimer la reservation #" + r.getIdReservation() + " ?", "Cette action est irreversible.");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                reservationService.supprimer(r.getIdReservation());
                auditService.log(currentAdminId, currentUserName, "DELETE_RESERVATION", "RESERVATION", r.getIdReservation(),
                    "Reservation #" + r.getIdReservation(), null, null);
                loadAdminReservations(filterUserId, filterStatus);
            }
        });
        actions.getChildren().add(deleteBtn);

        card.getChildren().add(actions);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace("rgba(255,215,0,0.08)", "rgba(255,215,0,0.2)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("rgba(255,215,0,0.2)", "rgba(255,215,0,0.08)")));

        return card;
    }

    @FXML private void handleShowReservationAdmin() {
        setActiveNav(btnReservationAdmin);
        loadAdminReservations(null, null);
        showPane(reservationAdminPane);
    }

    @FXML private void handleFilterReservations() {
        Integer userId = null;
        if (resUserFilter != null && resUserFilter.getValue() != null) {
            userId = userNameToIdMap.get(resUserFilter.getValue());
        }
        String status = null;
        if (resStatusFilter != null && resStatusFilter.getValue() != null) {
            status = resStatusFilter.getValue();
        }
        loadAdminReservations(userId, status);
    }

    @FXML private void handleShowAllReservations() {
        if (resUserFilter != null) resUserFilter.setValue(null);
        if (resStatusFilter != null) resStatusFilter.setValue(null);
        loadAdminReservations(null, null);
    }

    // ================= NAVIGATION =================
    private void setActiveNav(Button active) {
        Button[] navButtons = {btnDashboard, btnCharts, btnUsers, btnAudit, btnPermissions, btnSettings, btnSupport, btnPanierAdmin, btnReservationAdmin};
        for (Button b : navButtons) {
            if (b != null) {
                b.getStyleClass().remove("sidebar-button-active");
                if (!b.getStyleClass().contains("sidebar-button")) b.getStyleClass().add("sidebar-button");
            }
        }
        if (active != null) {
            active.getStyleClass().remove("sidebar-button");
            if (!active.getStyleClass().contains("sidebar-button-active")) active.getStyleClass().add("sidebar-button-active");
        }
    }

    @FXML private void handleShowDashboard() { setActiveNav(btnDashboard); loadDashboardData(); showPane(dashboardPane); }
    @FXML private void handleShowCharts() { setActiveNav(btnCharts); loadCharts(); showPane(chartsPane); }
    @FXML private void handleShowUsers() { setActiveNav(btnUsers); loadUsersTable(); showPane(usersPane); }
    @FXML private void handleShowAudit() { setActiveNav(btnAudit); loadAuditTrail(); showPane(auditPane); }
    @FXML private void handleShowPermissions() { setActiveNav(btnPermissions); loadPermissionsMatrix(); showPane(permissionsPane); }
    @FXML private void handleShowSettings() { setActiveNav(btnSettings); showPane(settingsPane); }
    @FXML private void handleShowSupport() { setActiveNav(btnSupport); showPane(supportPane); }

    @FXML public void handleRefreshDashboard() { loadDashboardData(); }

    private void showPane(VBox paneToShow) {
        VBox[] allPanes = {dashboardPane, chartsPane, usersPane, auditPane, permissionsPane, settingsPane, supportPane, panierAdminPane, reservationAdminPane};
        for (VBox pane : allPanes) {
            if (pane != null) pane.setVisible(false);
        }

        if (paneToShow != null) {
            paneToShow.setOpacity(0);
            paneToShow.setTranslateX(20);
            paneToShow.setVisible(true);

            FadeTransition fade = new FadeTransition(Duration.millis(350), paneToShow);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slide = new TranslateTransition(Duration.millis(350), paneToShow);
            slide.setFromX(20);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

            new ParallelTransition(fade, slide).play();
        }
    }

    // ================= WINDOW CONTROLS =================
    @FXML public void handleMinimize() {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML public void handleClose() {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.close();
    }

    // ================= ACTIONS =================
    @FXML private void handleLogout() {
        try {
            auditService.log(currentAdminId, currentUserName, "LOGOUT");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Login");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleViewProfile() {
        showAlert("Profil", "Fonctionnalite de profil administrateur - en cours de developpement.");
    }

    @FXML private void handleGoToAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Admin Panel");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleGoToTourisme() {
        System.out.println("\n[TOURISM] handleGoToTourisme() called (admin)");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
            System.out.println("[TOURISM] Loading MainView.fxml...");
            Parent root = loader.load();
            System.out.println("[TOURISM] MainView.fxml loaded OK!");
            MainController mainCtrl = loader.getController();
            mainCtrl.setReturnTarget("admin", null);
            root.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            Stage stage = (Stage) contentArea.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Gestion Touristique");
            System.out.println("[TOURISM] Scene transition started");
        } catch (Exception e) {
            System.err.println("[TOURISM] ❌ FAILED to load tourism module:");
            e.printStackTrace();
        }
    }

    // ================= SUPPORT =================
    @FXML private void handleViewFAQ() {
        showAlert("FAQ - Questions frequentes",
                "Comment reinitialiser mon mot de passe ?\n-> Cliquez 'Mot de passe oublie' sur la page de connexion.\n\nComment contacter le support ?\n-> Soumettez un ticket ci-dessous ou envoyez un email.\n\nComment modifier mes informations ?\n-> Accedez a votre profil via le menu utilisateur.");
    }

    @FXML private void handleSendEmail() {
        showAlert("Support par email", "Adresse : support@tabaani.com\n\nNotre equipe vous repondra sous 24 heures ouvrees.\nMerci de preciser votre identifiant et une description detaillee du probleme.");
    }

    @FXML private void handleStartChat() {
        showAlert("Chat en direct", "Le chat en direct est actuellement indisponible.\n\nVeuillez soumettre un ticket via le formulaire ci-dessous\nou contactez-nous par email a support@tabaani.com.");
    }

    @FXML private void handleSubmitTicket() {
        String subject = ticketSubjectField != null ? ticketSubjectField.getText().trim() : "";
        String priority = ticketPriorityCombo != null ? ticketPriorityCombo.getValue() : null;
        String desc = ticketDescriptionArea != null ? ticketDescriptionArea.getText().trim() : "";

        if (subject.isEmpty() || desc.isEmpty()) {
            if (ticketMessageLabel != null) {
                ticketMessageLabel.setText("Veuillez remplir le sujet et la description.");
                ticketMessageLabel.setStyle("-fx-text-fill: #FF6B6B;");
            }
            return;
        }
        auditService.log(currentAdminId, currentUserName, "SUBMIT_TICKET", "SUPPORT", 0, subject, null, priority);
        if (ticketMessageLabel != null) {
            ticketMessageLabel.setText("Ticket soumis avec succes !");
            ticketMessageLabel.setStyle("-fx-text-fill: #51CF66;");
        }
        if (ticketSubjectField != null) ticketSubjectField.clear();
        if (ticketDescriptionArea != null) ticketDescriptionArea.clear();
        if (ticketPriorityCombo != null) ticketPriorityCombo.setValue(null);
    }

    // ================= SETTINGS =================
    @FXML private void handleSaveSettings() {
        boolean emailNotif = emailNotifCheckbox != null && emailNotifCheckbox.isSelected();
        boolean twoFactor = twoFactorCheckbox != null && twoFactorCheckbox.isSelected();
        String lang = languageComboBox != null ? languageComboBox.getValue() : "Francais";
        auditService.log(currentAdminId, currentUserName, "SAVE_SETTINGS", "SETTING", 0, "Dashboard Settings",
                null, "Email: " + emailNotif + ", 2FA: " + twoFactor + ", Lang: " + lang);
        if (settingsMessageLabel != null) {
            settingsMessageLabel.setText("Parametres sauvegardes !");
            settingsMessageLabel.setStyle("-fx-text-fill: #51CF66;");
        }
    }

    // ================= HELPERS =================
    private void showStatus(Label label, String text, boolean success) {
        if (label != null) {
            label.setText(text);
            label.setStyle("-fx-text-fill: " + (success ? "#51CF66" : "#FF6B6B") + "; -fx-font-size: 11;");
        }
    }

    private void fadeTransition(Stage stage, Parent newRoot, String title) {
        Scene oldScene = stage.getScene();
        Node oldRoot = oldScene.getRoot();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), oldRoot);
        fadeOut.setFromValue(1); fadeOut.setToValue(0); fadeOut.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(300), oldRoot);
        scaleOut.setToX(0.97); scaleOut.setToY(0.97); scaleOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition exitAnim = new ParallelTransition(fadeOut, scaleOut);
        exitAnim.setOnFinished(e -> {
            Scene newScene = new Scene(newRoot);
            ThemeManager.applyTheme(newScene);
            ThemeManager.trackScene(newScene);
            newRoot.setOpacity(0); newRoot.setScaleX(1.03); newRoot.setScaleY(1.03); newRoot.setTranslateY(8);
            stage.setScene(newScene);
            stage.setTitle(title);
            stage.setMaximized(true);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), newRoot);
            fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), newRoot);
            scaleIn.setToX(1); scaleIn.setToY(1); scaleIn.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), newRoot);
            slideIn.setToY(0); slideIn.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
            new ParallelTransition(fadeIn, scaleIn, slideIn).play();
        });
        exitAnim.play();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.initStyle(StageStyle.UNDECORATED);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #333333;");
        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(420);

        VBox content = new VBox(12, titleLabel, sep, msgLabel);
        content.setStyle("-fx-padding: 20;");

        DialogPane dp = alert.getDialogPane();
        dp.setContent(content);
        dp.getButtonTypes().add(ButtonType.OK);
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dp.getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    private Alert createConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initStyle(StageStyle.UNDECORATED);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 16; -fx-font-weight: bold;");
        Separator sep = new Separator();
        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(420);

        VBox content = new VBox(12, titleLabel, sep, msgLabel);
        content.setStyle("-fx-padding: 20;");

        DialogPane dp = alert.getDialogPane();
        dp.setContent(content);
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dp.getStyleClass().add("dialog-pane");
        return alert;
    }
}