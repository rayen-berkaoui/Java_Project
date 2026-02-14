package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import com.esprit.entities.utilisateur;
import com.esprit.services.roleServices;
import com.esprit.services.utilisateurServices;

import java.util.List;

public class DashboardController {

    // ================= UI =================
    @FXML private Label welcomeLabel;
    @FXML private Label userNameLabel;

    // Panels
    @FXML private VBox dashboardPane;
    @FXML private VBox settingsPane;
    @FXML private VBox supportPane;

    @FXML private StackPane contentArea;
    @FXML private HBox titleBar;

    // Nav buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnSettings;
    @FXML private Button btnSupport;

    // Dashboard stats
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label blockedUsersLabel;
    @FXML private Label totalRolesLabel;
    @FXML private VBox recentUsersContainer;

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

    // Services
    private final utilisateurServices userService = new utilisateurServices();
    private final roleServices roleService = new roleServices();

    private String currentUserName;
    private double xOffset = 0;
    private double yOffset = 0;

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
    }

    public void setUserName(String userName) {
        this.currentUserName = userName;
        if (userNameLabel != null) userNameLabel.setText(userName);
        if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + userName);
    }

    // ================= DASHBOARD DATA (Real DB) =================
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
    }

    private void loadRecentUsers() {
        if (recentUsersContainer == null) return;
        recentUsersContainer.getChildren().clear();

        List<utilisateur> recent = userService.getRecentUsers(5);
        if (recent.isEmpty()) {
            Label empty = new Label("Aucun utilisateur recent");
            empty.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");
            recentUsersContainer.getChildren().add(empty);
            return;
        }

        for (utilisateur u : recent) {
            String roleName = userService.getRoleName(u.getRoleId());
            String status = u.getStatut();
            String statusColor = "ACTIF".equalsIgnoreCase(status) ? "#51CF66" : "#FF6B6B";
            String dateStr = u.getDateCreation() != null ? u.getDateCreation().toString() : "-";

            HBox row = new HBox(15);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #0d0d0d; -fx-padding: 10 15; -fx-background-radius: 8;");

            Label idLabel = new Label("#" + u.getId());
            idLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-min-width: 40;");

            Label nameLabel = new Label(u.getPrenom() + " " + u.getNom());
            nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12;");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Label emailLabel = new Label(u.getEmail());
            emailLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11;");

            Label roleLabel = new Label(roleName);
            roleLabel.setStyle("-fx-text-fill: #64B5F6; -fx-font-size: 11; -fx-font-weight: bold;");

            Label statusLabel = new Label(status);
            statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 11; -fx-font-weight: bold; -fx-min-width: 55;");

            Label dateLabel = new Label(dateStr);
            dateLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");

            row.getChildren().addAll(idLabel, nameLabel, emailLabel, roleLabel, statusLabel, dateLabel);
            recentUsersContainer.getChildren().add(row);
        }
    }

    // ================= NAVIGATION =================
    private void setActiveNav(Button active) {
        Button[] navButtons = {btnDashboard, btnSettings, btnSupport};
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
    private void handleShowDashboard() {
        setActiveNav(btnDashboard);
        loadDashboardData();
        showPane(dashboardPane);
    }

    @FXML
    private void handleShowSettings() {
        setActiveNav(btnSettings);
        showPane(settingsPane);
    }

    @FXML
    private void handleShowSupport() {
        setActiveNav(btnSupport);
        showPane(supportPane);
    }

    @FXML
    public void handleRefreshDashboard() {
        loadDashboardData();
    }

    private void showPane(VBox paneToShow) {
        VBox[] allPanes = {dashboardPane, settingsPane, supportPane};
        for (VBox pane : allPanes) {
            if (pane != null) pane.setVisible(false);
        }

        if (paneToShow != null) {
            paneToShow.setOpacity(0);
            paneToShow.setVisible(true);
            FadeTransition fade = new FadeTransition(Duration.millis(250), paneToShow);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
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

    // ================= ACTIONS =================
    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Tabaani - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewProfile() {
        showAlert("Profil", "Fonctionnalite de profil administrateur - en cours de developpement.");
    }

    @FXML
    private void handleGoToAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Tabaani - Admin Panel");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= SUPPORT =================
    @FXML
    private void handleViewFAQ() {
        showAlert("FAQ - Questions frequentes",
                "Comment reinitialiser mon mot de passe ?\n" +
                "-> Cliquez 'Mot de passe oublie' sur la page de connexion.\n\n" +
                "Comment contacter le support ?\n" +
                "-> Soumettez un ticket ci-dessous ou envoyez un email.\n\n" +
                "Comment modifier mes informations ?\n" +
                "-> Accedez a votre profil via le menu utilisateur.");
    }

    @FXML
    private void handleSendEmail() {
        showAlert("Support par email",
                "Adresse : support@tabaani.com\n\n" +
                "Notre equipe vous repondra sous 24 heures ouvrees.\n" +
                "Merci de preciser votre identifiant et une description detaillee du probleme.");
    }

    @FXML
    private void handleStartChat() {
        showAlert("Chat en direct",
                "Le chat en direct est actuellement indisponible.\n\n" +
                "Veuillez soumettre un ticket via le formulaire ci-dessous\n" +
                "ou contactez-nous par email a support@tabaani.com.");
    }

    @FXML
    private void handleSubmitTicket() {
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

        System.out.println("Ticket soumis - Sujet: " + subject + " | Priorite: " + priority);

        if (ticketMessageLabel != null) {
            ticketMessageLabel.setText("Ticket soumis avec succes !");
            ticketMessageLabel.setStyle("-fx-text-fill: #51CF66;");
        }
        if (ticketSubjectField != null) ticketSubjectField.clear();
        if (ticketDescriptionArea != null) ticketDescriptionArea.clear();
        if (ticketPriorityCombo != null) ticketPriorityCombo.setValue(null);
    }

    // ================= SETTINGS =================
    @FXML
    private void handleSaveSettings() {
        boolean emailNotif = emailNotifCheckbox != null && emailNotifCheckbox.isSelected();
        boolean twoFactor = twoFactorCheckbox != null && twoFactorCheckbox.isSelected();
        String lang = languageComboBox != null ? languageComboBox.getValue() : "Francais";

        System.out.println("Parametres sauvegardes - Email: " + emailNotif + " | 2FA: " + twoFactor + " | Langue: " + lang);

        if (settingsMessageLabel != null) {
            settingsMessageLabel.setText("Parametres sauvegardes !");
            settingsMessageLabel.setStyle("-fx-text-fill: #51CF66;");
        }
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
}
