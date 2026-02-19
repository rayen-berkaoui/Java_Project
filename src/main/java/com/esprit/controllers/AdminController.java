package com.esprit.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.Node;

import com.esprit.entities.role;
import com.esprit.entities.utilisateur;
import com.esprit.services.roleServices;
import com.esprit.services.utilisateurServices;

import java.util.List;
import java.util.Optional;

public class AdminController {

    // ================= UI =================
    @FXML private HBox titleBar;
    @FXML private StackPane contentArea;
    @FXML private Label statsLabel;

    // Panels
    @FXML private VBox usersPane;
    @FXML private VBox rolesPane;
    @FXML private VBox logsPane;
    @FXML private VBox reportsPane;
    @FXML private VBox securityPane;

    // Nav buttons
    @FXML private Button navUsers;
    @FXML private Button navRoles;
    @FXML private Button navLogs;
    @FXML private Button navReports;
    @FXML private Button navSecurity;

    // Users panel
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label blockedUsersLabel;
    @FXML private TextField userSearchField;
    @FXML private Label userCountLabel;
    @FXML private TableView<utilisateur> usersTable;
    @FXML private TableColumn<utilisateur, Integer> colId;
    @FXML private TableColumn<utilisateur, String> colName;
    @FXML private TableColumn<utilisateur, String> colEmail;
    @FXML private TableColumn<utilisateur, String> colPhone;
    @FXML private TableColumn<utilisateur, String> colRole;
    @FXML private TableColumn<utilisateur, String> colStatus;
    @FXML private TableColumn<utilisateur, String> colDate;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label pageInfoLabel;

    // Roles panel
    @FXML private FlowPane rolesContainer;
    @FXML private VBox roleStatsContainer;

    // Logs panel
    @FXML private VBox logsContainer;

    // Reports panel
    @FXML private Label reportTotalLabel;
    @FXML private Label reportActiveRateLabel;
    @FXML private Label reportRolesLabel;
    @FXML private Label reportBlockRateLabel;
    @FXML private VBox reportDistContainer;

    // Security panel
    @FXML private VBox securityContainer;

    // Services
    private final utilisateurServices userService = new utilisateurServices();
    private final roleServices roleService = new roleServices();

    // Pagination state
    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private int totalPages = 1;
    private String currentSearchQuery = "";

    private double xOffset = 0;
    private double yOffset = 0;

    // ================= INIT =================
    @FXML
    public void initialize() {
        setupTableColumns();
        showPane(usersPane);
        setActiveNav(navUsers);
        loadUsersPage();
        loadStats();

        if (titleBar != null) {
            titleBar.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
            titleBar.setOnMouseDragged(e -> {
                Stage s = (Stage) titleBar.getScene().getWindow();
                s.setX(e.getScreenX() - xOffset);
                s.setY(e.getScreenY() - yOffset);
            });
        }

        if (userSearchField != null) {
            userSearchField.setOnAction(e -> handleSearchUsers());
        }
    }

    // ================= TABLE SETUP =================
    private void setupTableColumns() {
        colId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        colName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPrenom() + " " + data.getValue().getNom()));

        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));

        colPhone.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getNumTel())));

        colRole.setCellValueFactory(data ->
                new SimpleStringProperty(userService.getRoleName(data.getValue().getRoleId())));

        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatut()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ACTIF".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #51CF66; -fx-font-weight: bold;");
                    } else if ("BLOQUE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #FF6B6B; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #cccccc;");
                    }
                }
            }
        });

        colDate.setCellValueFactory(data -> {
            if (data.getValue().getDateCreation() != null) {
                return new SimpleStringProperty(data.getValue().getDateCreation().toString());
            }
            return new SimpleStringProperty("-");
        });
    }

    // ================= USERS PANEL =================
    private void loadUsersPage() {
        List<utilisateur> users;
        int totalCount;

        if (currentSearchQuery.isEmpty()) {
            users = userService.getPage(currentPage, PAGE_SIZE);
            totalCount = userService.countAll();
        } else {
            users = userService.searchUsers(currentSearchQuery, currentPage, PAGE_SIZE);
            totalCount = userService.countSearch(currentSearchQuery);
        }

        totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        ObservableList<utilisateur> data = FXCollections.observableArrayList(users);
        usersTable.setItems(data);

        if (pageInfoLabel != null) pageInfoLabel.setText("Page " + currentPage + " / " + totalPages);
        if (btnPrevPage != null) btnPrevPage.setDisable(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisable(currentPage >= totalPages);
        if (userCountLabel != null) userCountLabel.setText(totalCount + " utilisateur(s)");
    }

    private void loadStats() {
        int total = userService.countAll();
        int active = userService.countByStatus("ACTIF");
        int blocked = userService.countByStatus("BLOQUE");

        if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(total));
        if (activeUsersLabel != null) activeUsersLabel.setText(String.valueOf(active));
        if (blockedUsersLabel != null) blockedUsersLabel.setText(String.valueOf(blocked));
        if (statsLabel != null) statsLabel.setText("Total: " + total + " | Actifs: " + active + " | Bloques: " + blocked);
    }

    @FXML
    public void handleSearchUsers() {
        String query = userSearchField != null ? userSearchField.getText().trim() : "";
        currentSearchQuery = query;
        currentPage = 1;
        loadUsersPage();
    }

    @FXML
    public void handleClearSearch() {
        if (userSearchField != null) userSearchField.clear();
        currentSearchQuery = "";
        currentPage = 1;
        loadUsersPage();
    }

    @FXML
    public void handlePrevPage() {
        if (currentPage > 1) { currentPage--; loadUsersPage(); }
    }

    @FXML
    public void handleNextPage() {
        if (currentPage < totalPages) { currentPage++; loadUsersPage(); }
    }

    @FXML
    public void handleRefreshUsers() {
        loadUsersPage();
        loadStats();
    }

    @FXML
    public void handleBlockUser() {
        utilisateur selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucune selection", "Veuillez selectionner un utilisateur a bloquer.");
            return;
        }
        if ("BLOQUE".equalsIgnoreCase(selected.getStatut())) {
            showAlert("Deja bloque", "Cet utilisateur est deja bloque.");
            return;
        }

        Optional<ButtonType> confirm = showConfirm("Bloquer l'utilisateur",
                "Voulez-vous vraiment bloquer " + selected.getPrenom() + " " + selected.getNom() + " ?");
        if (confirm.isPresent() && confirm.get() == ButtonType.OK) {
            if (userService.blockUser(selected.getId())) {
                loadUsersPage();
                loadStats();
            }
        }
    }

    @FXML
    public void handleUnblockUser() {
        utilisateur selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucune selection", "Veuillez selectionner un utilisateur a debloquer.");
            return;
        }
        if ("ACTIF".equalsIgnoreCase(selected.getStatut())) {
            showAlert("Deja actif", "Cet utilisateur est deja actif.");
            return;
        }
        if (userService.unblockUser(selected.getId())) {
            loadUsersPage();
            loadStats();
        }
    }

    @FXML
    public void handleDeleteUser() {
        utilisateur selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucune selection", "Veuillez selectionner un utilisateur a supprimer.");
            return;
        }

        Optional<ButtonType> confirm = showConfirm("Supprimer l'utilisateur",
                "Cette action est irreversible.\nSupprimer " + selected.getPrenom() + " " + selected.getNom() + " (ID: " + selected.getId() + ") ?");
        if (confirm.isPresent() && confirm.get() == ButtonType.OK) {
            if (userService.deleteAccount(selected.getId())) {
                loadUsersPage();
                loadStats();
            }
        }
    }

    // ================= ROLES PANEL =================
    private void loadRoles() {
        if (rolesContainer == null) return;
        rolesContainer.getChildren().clear();

        try {
            List<role> roles = roleService.afficher();
            String[] colors = {"#FFD700", "#51CF66", "#64B5F6", "#CE93D8", "#FF6B6B", "#FFB74D"};

            for (int i = 0; i < roles.size(); i++) {
                role r = roles.get(i);
                int memberCount = userService.countByRole(r.getId());
                String color = colors[i % colors.length];

                VBox card = new VBox(10);
                card.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 20; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: " + color + "; -fx-border-width: 0 0 0 3;");
                card.setPrefWidth(220);

                Label nameLabel = new Label(r.getNom());
                nameLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 15;");

                Label descLabel = new Label(r.getDescription() != null ? r.getDescription() : "Aucune description");
                descLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11;");
                descLabel.setWrapText(true);

                Label countLabel = new Label(memberCount + " membre(s)");
                countLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-font-weight: bold;");

                Button deleteBtn = new Button("Supprimer");
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #FF6B6B; -fx-font-size: 11; -fx-cursor: hand; -fx-border-color: #FF6B6B; -fx-border-radius: 5; -fx-padding: 3 10;");
                final int roleId = r.getId();
                final String roleName = r.getNom();
                deleteBtn.setOnAction(e -> handleDeleteRole(roleId, roleName));

                card.getChildren().addAll(nameLabel, descLabel, countLabel, deleteBtn);
                rolesContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadRoleStats();
    }

    private void loadRoleStats() {
        if (roleStatsContainer == null) return;
        roleStatsContainer.getChildren().clear();

        try {
            List<role> roles = roleService.afficher();
            int totalUsers = userService.countAll();
            String[] colors = {"#FFD700", "#51CF66", "#64B5F6", "#CE93D8", "#FF6B6B", "#FFB74D"};

            for (int i = 0; i < roles.size(); i++) {
                role r = roles.get(i);
                int count = userService.countByRole(r.getId());
                double percentage = totalUsers > 0 ? (count * 100.0 / totalUsers) : 0;
                String color = colors[i % colors.length];

                HBox row = new HBox(12);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label nameL = new Label(r.getNom());
                nameL.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12; -fx-min-width: 100;");

                ProgressBar bar = new ProgressBar(percentage / 100.0);
                bar.setPrefWidth(300);
                bar.setPrefHeight(12);
                bar.setStyle("-fx-accent: " + color + ";");
                HBox.setHgrow(bar, Priority.ALWAYS);

                Label pctLabel = new Label(String.format("%.0f%% (%d)", percentage, count));
                pctLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11; -fx-font-weight: bold; -fx-min-width: 80;");

                row.getChildren().addAll(nameL, bar, pctLabel);
                roleStatsContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleNewRole() {
        Alert dialog = new Alert(Alert.AlertType.NONE);
        dialog.initStyle(StageStyle.UNDECORATED);

        Label titleLabel = new Label("Nouveau Role");
        titleLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");

        Label namePrompt = new Label("Nom du role :");
        namePrompt.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13;");
        TextField nameField = new TextField();
        nameField.setPromptText("Ex: MODERATEUR");
        nameField.setStyle("-fx-background-color: #0d0d0d; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #666666; -fx-padding: 8; -fx-background-radius: 5; -fx-border-color: #333333; -fx-border-radius: 5;");

        Label descPrompt = new Label("Description :");
        descPrompt.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13;");
        TextField descField = new TextField();
        descField.setPromptText("Description du role");
        descField.setStyle("-fx-background-color: #0d0d0d; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #666666; -fx-padding: 8; -fx-background-radius: 5; -fx-border-color: #333333; -fx-border-radius: 5;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #333333;");

        VBox content = new VBox(12, titleLabel, sep, namePrompt, nameField, descPrompt, descField);
        content.setStyle("-fx-padding: 20;");

        DialogPane dp = dialog.getDialogPane();
        dp.setContent(content);
        dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dp.getStyleClass().add("dialog-pane");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                try {
                    role newRole = new role();
                    newRole.setNom(name.toUpperCase());
                    newRole.setDescription(descField.getText().trim().isEmpty() ? "Role cree depuis l'admin" : descField.getText().trim());
                    roleService.ajouter(newRole);
                    loadRoles();
                    loadStats();
                } catch (Exception e) {
                    showAlert("Erreur", "Impossible de creer le role : " + e.getMessage());
                }
            }
        }
    }

    private void handleDeleteRole(int roleId, String roleName) {
        int memberCount = userService.countByRole(roleId);
        if (memberCount > 0) {
            showAlert("Suppression impossible", "Le role '" + roleName + "' a " + memberCount + " membre(s).\nReassignez les utilisateurs avant de supprimer ce role.");
            return;
        }

        Optional<ButtonType> confirm = showConfirm("Supprimer le role",
                "Voulez-vous vraiment supprimer le role '" + roleName + "' ?");
        if (confirm.isPresent() && confirm.get() == ButtonType.OK) {
            try {
                roleService.supprimer(roleId);
                loadRoles();
                loadStats();
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de supprimer le role : " + e.getMessage());
            }
        }
    }

    // ================= LOGS PANEL =================
    private void loadLogs() {
        if (logsContainer == null) return;
        logsContainer.getChildren().clear();

        List<utilisateur> recentUsers = userService.getRecentUsers(20);

        if (recentUsers.isEmpty()) {
            Label empty = new Label("Aucune activite recente");
            empty.setStyle("-fx-text-fill: #666666; -fx-font-size: 13;");
            logsContainer.getChildren().add(empty);
            return;
        }

        for (utilisateur u : recentUsers) {
            String roleName = userService.getRoleName(u.getRoleId());
            String status = u.getStatut();
            String statusColor = "ACTIF".equalsIgnoreCase(status) ? "#51CF66" : "#FF6B6B";
            String dateStr = u.getDateCreation() != null ? u.getDateCreation().toString() : "-";

            HBox row = new HBox(15);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 12 18; -fx-background-radius: 8;");

            Label timeLabel = new Label(dateStr);
            timeLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11; -fx-min-width: 90;");

            Label typeLabel = new Label("INSCRIPTION");
            typeLabel.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 11; -fx-font-weight: bold; -fx-min-width: 100;");

            Label userLabel = new Label(u.getPrenom() + " " + u.getNom());
            userLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12;");
            HBox.setHgrow(userLabel, Priority.ALWAYS);

            Label roleL = new Label(roleName);
            roleL.setStyle("-fx-text-fill: #CE93D8; -fx-font-size: 11;");

            Label statusLabel = new Label(status);
            statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 11; -fx-font-weight: bold;");

            row.getChildren().addAll(timeLabel, typeLabel, userLabel, roleL, statusLabel);
            logsContainer.getChildren().add(row);
        }
    }

    @FXML
    public void handleRefreshLogs() {
        loadLogs();
    }

    // ================= REPORTS PANEL =================
    private void loadReports() {
        int total = userService.countAll();
        int active = userService.countByStatus("ACTIF");
        int blocked = userService.countByStatus("BLOQUE");
        int rolesCount = 0;
        try { rolesCount = roleService.afficher().size(); } catch (Exception e) { e.printStackTrace(); }

        double activeRate = total > 0 ? (active * 100.0 / total) : 0;
        double blockRate = total > 0 ? (blocked * 100.0 / total) : 0;

        if (reportTotalLabel != null) reportTotalLabel.setText(String.valueOf(total));
        if (reportActiveRateLabel != null) reportActiveRateLabel.setText(String.format("%.1f%%", activeRate));
        if (reportRolesLabel != null) reportRolesLabel.setText(String.valueOf(rolesCount));
        if (reportBlockRateLabel != null) reportBlockRateLabel.setText(String.format("%.1f%%", blockRate));

        // Role distribution
        if (reportDistContainer == null) return;
        reportDistContainer.getChildren().clear();

        try {
            List<role> roles = roleService.afficher();
            String[] colors = {"#FFD700", "#51CF66", "#64B5F6", "#CE93D8", "#FF6B6B", "#FFB74D"};

            for (int i = 0; i < roles.size(); i++) {
                role r = roles.get(i);
                int count = userService.countByRole(r.getId());
                double pct = total > 0 ? (count * 100.0 / total) : 0;
                String color = colors[i % colors.length];

                HBox row = new HBox(12);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label nameL = new Label(r.getNom());
                nameL.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13; -fx-min-width: 120; -fx-font-weight: bold;");

                ProgressBar bar = new ProgressBar(pct / 100.0);
                bar.setPrefWidth(400);
                bar.setPrefHeight(14);
                bar.setStyle("-fx-accent: " + color + ";");
                HBox.setHgrow(bar, Priority.ALWAYS);

                Label pctLabel = new Label(String.format("%.1f%% (%d utilisateurs)", pct, count));
                pctLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12; -fx-font-weight: bold; -fx-min-width: 160;");

                row.getChildren().addAll(nameL, bar, pctLabel);
                reportDistContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= SECURITY PANEL =================
    private void loadSecurity() {
        if (securityContainer == null) return;
        securityContainer.getChildren().clear();

        int total = userService.countAll();
        int blocked = userService.countByStatus("BLOQUE");

        String[][] items = {
                {"Politique de mot de passe", "Minimum 8 caracteres requis", "#51CF66"},
                {"Base de donnees", "MySQL - Connectee", "#51CF66"},
                {"Utilisateurs bloques", blocked + " sur " + total + " utilisateurs", blocked > 0 ? "#FFB74D" : "#51CF66"},
                {"Chiffrement", "BCrypt pour les mots de passe", "#51CF66"},
                {"Session", "Gestion locale JavaFX", "#64B5F6"},
                {"Email SMTP", "Service OTP actif", "#51CF66"}
        };

        for (String[] item : items) {
            HBox row = new HBox(15);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 18 22; -fx-background-radius: 10;");

            Label dot = new Label();
            dot.setMinSize(10, 10);
            dot.setMaxSize(10, 10);
            dot.setStyle("-fx-background-color: " + item[2] + "; -fx-background-radius: 5;");

            Label title = new Label(item[0]);
            title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14; -fx-font-weight: bold; -fx-min-width: 220;");

            Label desc = new Label(item[1]);
            desc.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12;");
            HBox.setHgrow(desc, Priority.ALWAYS);

            Label badge = new Label("OK");
            badge.setStyle("-fx-text-fill: " + item[2] + "; -fx-font-size: 11; -fx-font-weight: bold; -fx-border-color: " + item[2] + "; -fx-border-radius: 5; -fx-padding: 2 8; -fx-border-width: 1;");

            row.getChildren().addAll(dot, title, desc, badge);
            securityContainer.getChildren().add(row);
        }
    }

    // ================= NAVIGATION =================
    private void setActiveNav(Button active) {
        Button[] navButtons = {navUsers, navRoles, navLogs, navReports, navSecurity};
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

    @FXML
    public void handleShowUsers() {
        setActiveNav(navUsers);
        currentPage = 1;
        currentSearchQuery = "";
        if (userSearchField != null) userSearchField.clear();
        loadUsersPage();
        loadStats();
        showPane(usersPane);
    }

    @FXML
    public void handleShowRoles() {
        setActiveNav(navRoles);
        loadRoles();
        showPane(rolesPane);
    }

    @FXML
    public void handleShowLogs() {
        setActiveNav(navLogs);
        loadLogs();
        showPane(logsPane);
    }

    @FXML
    public void handleShowReports() {
        setActiveNav(navReports);
        loadReports();
        showPane(reportsPane);
    }

    @FXML
    public void handleShowSecurity() {
        setActiveNav(navSecurity);
        loadSecurity();
        showPane(securityPane);
    }

    private void showPane(VBox paneToShow) {
        VBox[] allPanes = {usersPane, rolesPane, logsPane, reportsPane, securityPane};
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
    @FXML
    public void handleMinimize() {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fadeTransition(Stage stage, Parent newRoot, String title) {
        Scene oldScene = stage.getScene();
        Node oldRoot = oldScene.getRoot();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), oldRoot);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(300), oldRoot);
        scaleOut.setToX(0.97);
        scaleOut.setToY(0.97);
        scaleOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition exitAnim = new ParallelTransition(fadeOut, scaleOut);
        exitAnim.setOnFinished(e -> {
            Scene newScene = new Scene(newRoot);
            newScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            newRoot.setOpacity(0);
            newRoot.setScaleX(1.03);
            newRoot.setScaleY(1.03);
            newRoot.setTranslateY(8);
            stage.setScene(newScene);
            stage.setTitle(title);
            stage.setMaximized(true);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), newRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), newRoot);
            scaleIn.setToX(1);
            scaleIn.setToY(1);
            scaleIn.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), newRoot);
            slideIn.setToY(0);
            slideIn.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
            new ParallelTransition(fadeIn, scaleIn, slideIn).play();
        });
        exitAnim.play();
    }

    // ================= HELPERS =================
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

    private Optional<ButtonType> showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.initStyle(StageStyle.UNDECORATED);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 16; -fx-font-weight: bold;");

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
        dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dp.getStyleClass().add("dialog-pane");

        return alert.showAndWait();
    }
}
