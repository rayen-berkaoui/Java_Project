package com.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.Node;

import com.esprit.entities.utilisateur;
import com.esprit.services.utilisateurServices;
import com.esprit.utils.ThemeManager;
import com.esprit.utils.SessionManager;
import javafx.application.Platform;

import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * Controller for the main Tabaani user interface.
 * User-focused hub: browse destinations, restaurants, reviews, reservations and view profile.
 * Profile editing is handled in UserProfileController (no duplication).
 */
public class MainInterfaceController {

    // ═══════ TOP BAR ═══════
    @FXML private HBox titleBar;
    @FXML private Label userNameLabel;
    @FXML private Label welcomeLabel;

    // ═══════ SIDEBAR BUTTONS ═══════
    @FXML private Button navAccueil;
    @FXML private Button navLieux;
    @FXML private Button navRestaurants;
    @FXML private Button navAvis;
    @FXML private Button navReservations;
    @FXML private Button navProfil;

    // ═══════ PANELS ═══════
    @FXML private ScrollPane panelAccueil;
    @FXML private ScrollPane panelLieux;
    @FXML private ScrollPane panelRestaurants;
    @FXML private ScrollPane panelAvis;
    @FXML private ScrollPane panelReservations;
    @FXML private ScrollPane panelProfil;

    // ═══════ DESTINATIONS FLOW ═══════
    @FXML private FlowPane lieuxFlow;

    // ═══════ RESTAURANTS FLOW ═══════
    @FXML private FlowPane restaurantsFlow;

    // ═══════ RESERVATIONS TABLE ═══════
    @FXML private TableView<String[]> reservationsTable;
    @FXML private TableColumn<String[], String> resIdCol;
    @FXML private TableColumn<String[], String> resLieuCol;
    @FXML private TableColumn<String[], String> resDateCol;
    @FXML private TableColumn<String[], String> resHeureCol;
    @FXML private TableColumn<String[], String> resNbCol;
    @FXML private TableColumn<String[], String> resStatutCol;

    // ═══════ RESERVATIONS TABS ═══════
    @FXML private Button tabReservations;
    @FXML private Button tabFavoris;
    @FXML private VBox subPanelReservations;
    @FXML private VBox subPanelFavoris;

    // ═══════ PROFILE VIEW (read-only) ═══════
    @FXML private Label profilInitials;
    @FXML private ImageView profilImageView;
    @FXML private Label profilFullName;
    @FXML private Label profilEmail;
    @FXML private Label profilStatut;
    @FXML private Label profilDate;
    @FXML private Label profilMessage;

    private double xOffset = 0;
    private double yOffset = 0;
    private utilisateur currentUser;
    private final utilisateurServices userService = new utilisateurServices();

    // ═══════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // Enable window dragging from title bar
        if (titleBar != null) {
            titleBar.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            titleBar.setOnMouseDragged(event -> {
                Stage stage = (Stage) titleBar.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }

        // Load fake reservations data
        loadReservationsData();

        // Show accueil by default
        showPanel("accueil");

        // Start session monitoring (auto-logout on inactivity)
        Platform.runLater(() -> {
            if (titleBar != null && titleBar.getScene() != null) {
                SessionManager.getInstance().startMonitoring(titleBar.getScene());
            }
        });
    }

    /**
     * Set the current user (called from LoginController after login)
     */
    public void setUser(utilisateur user) {
        this.currentUser = user;
        if (user != null) {
            String fullName = user.getNom() + " " + user.getPrenom();

            if (userNameLabel != null) {
                userNameLabel.setText(fullName);
            }

            if (welcomeLabel != null) {
                welcomeLabel.setText("\uD83C\uDF0D Bonjour, " + user.getPrenom() + " !");
            }

            // Populate profile panel
            populateProfile(user);
        }
    }

    public void setUserName(String name) {
        if (userNameLabel != null) {
            userNameLabel.setText(name);
        }
    }

    /**
     * Populate the profile panel with user data (read-only view)
     */
    private void populateProfile(utilisateur user) {
        if (profilInitials != null) {
            String initials = "";
            if (user.getNom() != null && !user.getNom().isEmpty()) initials += user.getNom().charAt(0);
            if (user.getPrenom() != null && !user.getPrenom().isEmpty()) initials += user.getPrenom().charAt(0);
            profilInitials.setText(initials.toUpperCase());
        }

        if (profilFullName != null) {
            profilFullName.setText(user.getNom() + " " + user.getPrenom());
        }

        if (profilEmail != null) {
            profilEmail.setText(user.getEmail());
        }

        if (profilStatut != null) {
            profilStatut.setText("Statut: " + (user.getStatut() != null ? user.getStatut() : "ACTIF"));
        }

        if (profilDate != null) {
            profilDate.setText("Membre depuis: " + (user.getDateCreation() != null ? user.getDateCreation().toString() : "--"));
        }

        if (profilMessage != null) {
            profilMessage.setText("");
        }

        // Load profile picture
        loadProfilePicture(user);
    }

    private void loadProfilePicture(utilisateur user) {
        String base64 = user.getProfilePicture();
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] imageData = Base64.getDecoder().decode(base64);
                Image img = new Image(new ByteArrayInputStream(imageData));
                if (profilImageView != null) {
                    profilImageView.setImage(img);
                    profilImageView.setVisible(true);
                    if (profilInitials != null) profilInitials.setVisible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (profilImageView != null) profilImageView.setVisible(false);
            if (profilInitials != null) profilInitials.setVisible(true);
        }
    }

    // ═══════════════════════════════════════════════════
    // FAKE DATA LOADER (Reservations only — rest is in FXML cards)
    // ═══════════════════════════════════════════════════

    private void loadReservationsData() {
        if (reservationsTable == null) return;

        setupColumns(resIdCol, resLieuCol, resDateCol, resHeureCol, resNbCol, resStatutCol);

        ObservableList<String[]> data = FXCollections.observableArrayList(
            new String[]{"1", "Le Baroque", "18/02/2026", "20:00", "4", "Confirmee"},
            new String[]{"2", "Visite Guidee - Carthage", "22/02/2026", "09:00", "2", "En attente"},
            new String[]{"3", "Dar El Jeld", "20/02/2026", "19:30", "6", "Confirmee"},
            new String[]{"4", "Aqua Palace", "25/02/2026", "10:00", "3", "Confirmee"},
            new String[]{"5", "Excursion Desert Douz", "01/03/2026", "06:00", "2", "En attente"},
            new String[]{"6", "Cafe des Delices", "17/02/2026", "15:00", "2", "Confirmee"},
            new String[]{"7", "Thermes de Jebel", "28/02/2026", "11:00", "1", "Annulee"}
        );

        reservationsTable.setItems(data);
    }

    /**
     * Helper to set up table columns using String[] array indices
     */
    @SafeVarargs
    private void setupColumns(TableColumn<String[], String>... cols) {
        for (int i = 0; i < cols.length; i++) {
            final int index = i;
            if (cols[i] != null) {
                cols[i].setCellValueFactory(cellData -> {
                    String[] row = cellData.getValue();
                    return new SimpleStringProperty(index < row.length ? row[index] : "");
                });
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // PANEL SWITCHING
    // ═══════════════════════════════════════════════════

    private void showPanel(String panel) {
        // Hide all panels
        panelAccueil.setVisible(false);
        panelLieux.setVisible(false);
        panelRestaurants.setVisible(false);
        panelAvis.setVisible(false);
        panelReservations.setVisible(false);
        panelProfil.setVisible(false);

        // Reset all sidebar buttons
        Button[] navButtons = {navAccueil, navLieux, navRestaurants, navAvis, navReservations, navProfil};
        for (Button btn : navButtons) {
            btn.getStyleClass().removeAll("sidebar-button-active");
            if (!btn.getStyleClass().contains("sidebar-button")) btn.getStyleClass().add("sidebar-button");
        }

        // Show selected panel with fade animation
        ScrollPane target = null;
        Button activeBtn = null;

        switch (panel) {
            case "accueil":
                target = panelAccueil;
                activeBtn = navAccueil;
                break;
            case "lieux":
                target = panelLieux;
                activeBtn = navLieux;
                break;
            case "restaurants":
                target = panelRestaurants;
                activeBtn = navRestaurants;
                break;
            case "avis":
                target = panelAvis;
                activeBtn = navAvis;
                break;
            case "reservations":
                target = panelReservations;
                activeBtn = navReservations;
                break;
            case "profil":
                target = panelProfil;
                activeBtn = navProfil;
                break;
        }

        if (target != null) {
            target.setVisible(true);
            target.setOpacity(0);

            FadeTransition fade = new FadeTransition(Duration.millis(250), target);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_OUT);
            fade.play();
        }

        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("sidebar-button");
            activeBtn.getStyleClass().add("sidebar-button-active");
        }
    }

    // ═══════════════════════════════════════════════════
    // SIDEBAR NAVIGATION HANDLERS
    // ═══════════════════════════════════════════════════

    @FXML public void handleShowAccueil() { showPanel("accueil"); }
    @FXML public void handleShowLieux() { showPanel("lieux"); }
    @FXML public void handleShowRestaurants() { showPanel("restaurants"); }
    @FXML public void handleShowAvis() { showPanel("avis"); }
    @FXML public void handleShowReservations() { showPanel("reservations"); }
    @FXML public void handleShowProfil() { showPanel("profil"); }

    // ═══════════════════════════════════════════════════
    // RESERVATIONS TAB SWITCHING
    // ═══════════════════════════════════════════════════

    @FXML
    public void handleTabReservations() {
        subPanelReservations.setVisible(true);
        subPanelReservations.setManaged(true);
        subPanelFavoris.setVisible(false);
        subPanelFavoris.setManaged(false);

        tabReservations.getStyleClass().removeAll("sidebar-button");
        tabReservations.getStyleClass().add("sidebar-button-active");
        tabFavoris.getStyleClass().removeAll("sidebar-button-active");
        if (!tabFavoris.getStyleClass().contains("sidebar-button")) tabFavoris.getStyleClass().add("sidebar-button");
    }

    @FXML
    public void handleTabFavoris() {
        subPanelReservations.setVisible(false);
        subPanelReservations.setManaged(false);
        subPanelFavoris.setVisible(true);
        subPanelFavoris.setManaged(true);

        tabFavoris.getStyleClass().removeAll("sidebar-button");
        tabFavoris.getStyleClass().add("sidebar-button-active");
        tabReservations.getStyleClass().removeAll("sidebar-button-active");
        if (!tabReservations.getStyleClass().contains("sidebar-button")) tabReservations.getStyleClass().add("sidebar-button");
    }

    // ═══════════════════════════════════════════════════
    // NAVIGATION TO FULL PROFILE PAGE
    // ═══════════════════════════════════════════════════

    @FXML
    public void handleGoToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/userprofile.fxml"));
            Parent root = loader.load();

            if (currentUser != null) {
                UserProfileController controller = loader.getController();
                controller.setUser(currentUser);
            }

            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Mon Profil");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════
    // WINDOW CONTROLS
    // ═══════════════════════════════════════════════════

    @FXML
    public void handleMinimize() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Connexion");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════
    // SCENE TRANSITION (same style as LoginController)
    // ═══════════════════════════════════════════════════

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
            newScene.getStylesheets().add(
                ThemeManager.getInstance().getCssPath()
            );
            newScene.setFill(javafx.scene.paint.Color.BLACK);

            newRoot.setOpacity(0);
            newRoot.setScaleX(1.03);
            newRoot.setScaleY(1.03);
            newRoot.setTranslateY(8);

            stage.setScene(newScene);
            stage.sizeToScene();
            stage.setTitle(title);

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

            ParallelTransition enterAnim = new ParallelTransition(fadeIn, scaleIn, slideIn);
            enterAnim.play();
        });

        exitAnim.play();
    }
}
