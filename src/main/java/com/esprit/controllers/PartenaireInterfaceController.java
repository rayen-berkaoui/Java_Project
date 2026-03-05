package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import com.esprit.entities.Etablissement;
import com.esprit.entities.Reservation;
import com.esprit.entities.utilisateur;
import com.esprit.services.EtablissementService;
import com.esprit.services.LieuTouristiqueServices;
import com.esprit.services.ReservationService;
import com.esprit.services.utilisateurServices;
import com.esprit.utils.ThemeManager;

import java.util.List;

public class PartenaireInterfaceController {

    // Header
    @FXML private HBox titleBar;
    @FXML private Button fullscreenBtn;
    @FXML private Label userNameLabel;

    // Accueil stats
    @FXML private Label statEtabLabel;
    @FXML private Label statLieuxLabel;
    @FXML private Label statReservLabel;
    @FXML private VBox recentActivityBox;

    // Panels
    @FXML private ScrollPane panelAccueil;
    @FXML private Node panelGalerieEtab;
    @FXML private Node panelGalerieActivite;
    @FXML private Node panelTableauEtab;
    @FXML private Node panelTableauActivite;
    @FXML private Node panelLieuxTouristiques;
    @FXML private Node panelAdresses;
    @FXML private Node panelCategories;
    @FXML private ScrollPane panelReservationsCash;
    @FXML private VBox cashReservationsBox;
    @FXML private Node panelForum;

    // Drawer
    @FXML private Button menuToggleBtn;
    @FXML private Region drawerOverlay;
    @FXML private VBox sidebarDrawer;
    @FXML private Label drawerInitials;
    @FXML private Label drawerUserName;
    @FXML private Label drawerUserRole;

    // Chatbot
    @FXML private VBox chatBotPanel;
    @FXML private Button chatBotFab;

    // State
    private utilisateur currentUser;
    private boolean drawerOpen = false;
    private double xOffset, yOffset;

    // Services
    private final EtablissementService etabService = new EtablissementService();
    private final LieuTouristiqueServices lieuService = new LieuTouristiqueServices();
    private final ReservationService reservationService = new ReservationService();

    @FXML
    public void initialize() {
        if (titleBar != null) {
            titleBar.setOnMousePressed(event -> { xOffset = event.getSceneX(); yOffset = event.getSceneY(); });
            titleBar.setOnMouseDragged(event -> {
                Stage stage = (Stage) titleBar.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
            javafx.application.Platform.runLater(() -> {
                if (titleBar.getScene() != null && titleBar.getScene().getRoot() != null) {
                    ThemeManager.applyThemeToFXML((Parent) titleBar.getScene().getRoot());
                }
            });
            ThemeManager.addThemeChangeListener(() -> javafx.application.Platform.runLater(() -> {
                if (titleBar.getScene() != null && titleBar.getScene().getRoot() != null) {
                    ThemeManager.applyThemeToFXML((Parent) titleBar.getScene().getRoot());
                }
            }));
        }
    }

    public void setUser(utilisateur user) {
        this.currentUser = user;
        if (currentUser != null) {
            if (userNameLabel != null) userNameLabel.setText(currentUser.getNom() + " " + currentUser.getPrenom());
            String init = "";
            if (currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty()) init += currentUser.getPrenom().charAt(0);
            if (currentUser.getNom() != null && !currentUser.getNom().isEmpty()) init += currentUser.getNom().charAt(0);
            if (drawerInitials != null) drawerInitials.setText(init.toUpperCase());
            if (drawerUserName != null) drawerUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            if (drawerUserRole != null) drawerUserRole.setText("Partenaire");
            loadData();
        }
    }

    private void loadData() {
        updateStats();
        handleShowAccueil();
    }

    private void updateStats() {
        if (currentUser == null) return;
        try {
            int etabCount = etabService.getByPartenaire(currentUser.getId()).size();
            int lieuxCount = lieuService.getByPartenaire(currentUser.getId()).size();
            int cashCount = reservationService.countCashReservationsForPartenaire(currentUser.getId());
            if (statEtabLabel != null) statEtabLabel.setText(String.valueOf(etabCount));
            if (statLieuxLabel != null) statLieuxLabel.setText(String.valueOf(lieuxCount));
            if (statReservLabel != null) statReservLabel.setText(cashCount + " en attente");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =====================================================
    // PANEL SWITCHING
    // =====================================================
    private void showPanel(Node target) {
        Node[] allPanels = { panelAccueil, panelGalerieEtab, panelGalerieActivite, panelTableauEtab,
                panelTableauActivite, panelLieuxTouristiques, panelAdresses, panelCategories,
                panelReservationsCash, panelForum };
        for (Node p : allPanels) {
            if (p != null) p.setVisible(false);
        }
        if (target != null) target.setVisible(true);
    }

    // =====================================================
    // NAVIGATION HANDLERS
    // =====================================================
    @FXML public void handleShowAccueil() { closeDrawer(); showPanel(panelAccueil); updateStats(); }
    @FXML public void handleShowGalerieEtab() { closeDrawer(); showPanel(panelGalerieEtab); }
    @FXML public void handleShowGalerieActivite() { closeDrawer(); showPanel(panelGalerieActivite); }
    @FXML public void handleShowLieuxTouristiques() { closeDrawer(); showPanel(panelLieuxTouristiques); }
    @FXML public void handleShowCategories() { closeDrawer(); showPanel(panelCategories); }
    @FXML public void handleShowAdresses() { closeDrawer(); showPanel(panelAdresses); }
    @FXML public void handleShowForum() { closeDrawer(); showPanel(panelForum); }

    @FXML public void handleShowReservationsCash() {
        closeDrawer();
        showPanel(panelReservationsCash);
        loadCashReservations();
    }

    @FXML public void handleShowProfil() {
        closeDrawer();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/userprofile.fxml"));
            Parent root = loader.load();
            UserProfileController profileCtrl = loader.getController();
            profileCtrl.setUser(currentUser);
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Mon Profil");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // =====================================================
    // CASH RESERVATIONS MANAGEMENT
    // =====================================================
    private void loadCashReservations() {
        if (cashReservationsBox == null || currentUser == null) return;
        cashReservationsBox.getChildren().clear();
        List<Reservation> cashReservations = reservationService.getCashReservationsForPartenaire(currentUser.getId());

        if (cashReservations.isEmpty()) {
            Label empty = new Label("\u2705 Aucune reservation cash en attente");
            empty.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 14;");
            cashReservationsBox.getChildren().add(empty);
            return;
        }

        for (Reservation r : cashReservations) {
            HBox card = new HBox(16);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-background-color: rgba(15,15,15,0.95); -fx-padding: 16; -fx-background-radius: 12; " +
                    "-fx-border-color: rgba(255,215,0,0.08); -fx-border-radius: 12; -fx-border-width: 1;");

            VBox info = new VBox(4);
            info.setStyle("-fx-padding: 0;");
            HBox.setHgrow(info, Priority.ALWAYS);

            Label title = new Label("\uD83D\uDCB0 Reservation #" + r.getIdReservation());
            title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14; -fx-font-weight: bold;");

            String etabName = r.getNomEtablissement() != null ? r.getNomEtablissement() : "N/A";
            Label details = new Label("Etablissement: " + etabName + "  |  Montant: " +
                    String.format("%.2f", r.getMontantTotal()) + " TND  |  Personnes: " + r.getNbPersonnes());
            details.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");

            String dateStr = r.getDatePaiement() != null ? r.getDatePaiement().toLocalDate().toString() : "N/A";
            Label date = new Label("Date: " + dateStr + "  |  Code: " + r.getCodeConfirmation());
            date.setStyle("-fx-text-fill: #666; -fx-font-size: 10;");

            info.getChildren().addAll(title, details, date);

            Button approveBtn = new Button("\u2705 Approuver");
            approveBtn.setStyle("-fx-background-color: #2D8A4E; -fx-text-fill: white; -fx-font-size: 11; " +
                    "-fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
            approveBtn.setOnAction(e -> {
                reservationService.updateStatut(r.getIdReservation(), "Paye");
                loadCashReservations();
                updateStats();
            });

            Button rejectBtn = new Button("\u274C Refuser");
            rejectBtn.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white; -fx-font-size: 11; " +
                    "-fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
            rejectBtn.setOnAction(e -> {
                reservationService.updateStatut(r.getIdReservation(), "Refuse");
                loadCashReservations();
                updateStats();
            });

            VBox buttons = new VBox(8);
            buttons.setAlignment(Pos.CENTER);
            buttons.getChildren().addAll(approveBtn, rejectBtn);

            card.getChildren().addAll(info, buttons);
            cashReservationsBox.getChildren().add(card);
        }
    }

    // =====================================================
    // DRAWER
    // =====================================================
    @FXML public void handleToggleDrawer() {
        if (drawerOpen) closeDrawer(); else openDrawer();
    }

    @FXML public void handleCloseDrawer() { closeDrawer(); }

    private void openDrawer() {
        drawerOpen = true;
        if (drawerOverlay != null) {
            drawerOverlay.setVisible(true);
            FadeTransition fade = new FadeTransition(Duration.millis(250), drawerOverlay);
            fade.setFromValue(0); fade.setToValue(1); fade.play();
        }
        if (sidebarDrawer != null) {
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), sidebarDrawer);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
            slide.play();
        }
        if (menuToggleBtn != null) menuToggleBtn.setText("\u2715");
    }

    private void closeDrawer() {
        if (!drawerOpen) return;
        drawerOpen = false;
        if (sidebarDrawer != null) {
            TranslateTransition slide = new TranslateTransition(Duration.millis(250), sidebarDrawer);
            slide.setToX(-280);
            slide.setInterpolator(Interpolator.EASE_IN);
            slide.play();
        }
        if (drawerOverlay != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(250), drawerOverlay);
            fade.setToValue(0);
            fade.setOnFinished(e -> drawerOverlay.setVisible(false));
            fade.play();
        }
        if (menuToggleBtn != null) menuToggleBtn.setText("\u2630");
    }

    // =====================================================
    // CHATBOT
    // =====================================================
    @FXML public void handleToggleChatBot() {
        if (chatBotPanel != null) {
            boolean show = !chatBotPanel.isVisible();
            chatBotPanel.setVisible(show);
            chatBotPanel.setManaged(show);
            if (chatBotFab != null) chatBotFab.setText(show ? "\u2715" : "\uD83E\uDD16");
        }
    }

    // =====================================================
    // WINDOW CONTROLS
    // =====================================================
    @FXML public void handleMinimize() { ((Stage) titleBar.getScene().getWindow()).setIconified(true); }
    @FXML public void handleClose() { ((Stage) titleBar.getScene().getWindow()).close(); }
    @FXML public void handleToggleFullscreen() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
        if (fullscreenBtn != null) fullscreenBtn.setText(stage.isFullScreen() ? "\u29C9" : "\u26F6");
    }

    // =====================================================
    // LOGOUT
    // =====================================================
    @FXML public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Connexion");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // =====================================================
    // SCENE TRANSITION
    // =====================================================
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
            stage.setScene(newScene); stage.setTitle(title);
            stage.setMaximized(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), newRoot);
            fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), newRoot);
            scaleIn.setToX(1); scaleIn.setToY(1);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), newRoot);
            slideIn.setToY(0);
            new ParallelTransition(fadeIn, scaleIn, slideIn).play();
        });
        exitAnim.play();
    }
}
