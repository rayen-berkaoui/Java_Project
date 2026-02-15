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

import com.esprit.entities.Panier;
import com.esprit.entities.Reservation;
import com.esprit.entities.Etablissement;
import com.esprit.entities.utilisateur;
import com.esprit.services.PanierService;
import com.esprit.services.ReservationService;
import com.esprit.services.EtablissementService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class PanierController {

    @FXML private HBox titleBar;
    @FXML private Label userNameLabel;
    @FXML private Label totalItemsLabel;
    @FXML private Label enAttenteLabel;
    @FXML private Label totalPrixLabel;
    @FXML private Label messageLabel;
    @FXML private HBox panierCarousel;
    @FXML private Label pageLabel;

    private double xOffset = 0;
    private double yOffset = 0;
    private utilisateur currentUser;
    private final PanierService panierService = new PanierService();
    private final ReservationService reservationService = new ReservationService();
    private final EtablissementService etabService = new EtablissementService();

    private List<Panier> panierItems;
    private int currentPage = 0;
    private static final int CARDS_PER_PAGE = 3;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        if (titleBar != null) {
            titleBar.setOnMousePressed(event -> { xOffset = event.getSceneX(); yOffset = event.getSceneY(); });
            titleBar.setOnMouseDragged(event -> {
                Stage stage = (Stage) titleBar.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }
    }

    public void setUser(utilisateur user) {
        this.currentUser = user;
        if (user != null) {
            if (userNameLabel != null) userNameLabel.setText(user.getNom() + " " + user.getPrenom());
            loadData();
        }
    }

    private void loadData() {
        if (currentUser == null) return;
        panierItems = panierService.getPanierByClient(currentUser.getId());

        // Resolve etablissement names
        for (Panier p : panierItems) {
            if (p.getNomEtablissement() == null || p.getNomEtablissement().isEmpty()) {
                Etablissement etab = etabService.getById(p.getIdEtablissement());
                if (etab != null) p.setNomEtablissement(etab.getNom());
            }
        }

        updateStats();
        renderCarousel();
    }

    private void updateStats() {
        int total = panierItems.size();
        long enAttente = panierItems.stream().filter(p -> "en_attente".equals(p.getStatutItem())).count();
        double totalPrix = panierItems.stream()
                .filter(p -> "en_attente".equals(p.getStatutItem()))
                .mapToDouble(Panier::getPrixEstime).sum();
        if (totalItemsLabel != null) totalItemsLabel.setText(String.valueOf(total));
        if (enAttenteLabel != null) enAttenteLabel.setText(String.valueOf(enAttente));
        if (totalPrixLabel != null) totalPrixLabel.setText(String.format("%.2f DT", totalPrix));
    }

    private void renderCarousel() {
        if (panierCarousel == null) return;
        panierCarousel.getChildren().clear();

        int start = currentPage * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, panierItems.size());

        for (int i = start; i < end; i++) {
            VBox card = buildPanierCard(panierItems.get(i));
            HBox.setHgrow(card, Priority.ALWAYS);
            panierCarousel.getChildren().add(card);
        }

        if (panierCarousel.getChildren().isEmpty()) {
            Label empty = new Label("Votre panier est vide. Explorez les destinations pour ajouter des articles !");
            empty.setStyle("-fx-text-fill: #666; -fx-font-size: 14; -fx-padding: 40;");
            empty.setWrapText(true);
            panierCarousel.getChildren().add(empty);
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) panierItems.size() / CARDS_PER_PAGE));
        if (pageLabel != null) pageLabel.setText((currentPage + 1) + " / " + totalPages);
    }

    private VBox buildPanierCard(Panier p) {
        VBox card = new VBox(12);
        card.setMinWidth(300);
        card.setMaxWidth(380);
        boolean isActive = "en_attente".equals(p.getStatutItem());
        String borderColor = isActive ? "rgba(255,215,0,0.15)" : "rgba(255,107,107,0.15)";
        card.setStyle("-fx-background-color: rgba(15,15,15,0.95); -fx-padding: 20; -fx-background-radius: 14; " +
                "-fx-border-color: " + borderColor + "; -fx-border-radius: 14; -fx-border-width: 1;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(50, 50);
        iconPane.setStyle("-fx-background-color: " + (isActive ? "rgba(255,215,0,0.1)" : "rgba(255,107,107,0.1)") + "; -fx-background-radius: 12;");
        String icon = "Voyage".equals(p.getTypeService()) ? "\uD83C\uDFDD" : ("Caf\u00e9".equals(p.getTypeService()) ? "\u2615" : "\uD83C\uDF55");
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 22;");
        iconPane.getChildren().add(iconLabel);

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        String displayName = p.getNomEtablissement() != null && !p.getNomEtablissement().isEmpty()
                ? p.getNomEtablissement() : p.getTypeService();
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        Label typeLabel = new Label(p.getTypeService());
        typeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
        nameBox.getChildren().addAll(nameLabel, typeLabel);

        VBox priceBox = new VBox(2);
        priceBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label prixLabel = new Label(String.format("%.2f DT", p.getPrixEstime()));
        prixLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");
        priceBox.getChildren().add(prixLabel);

        header.getChildren().addAll(iconPane, nameBox, priceBox);

        // Details
        VBox details = new VBox(4);
        details.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 10; -fx-background-radius: 8;");
        Label dateLabel = new Label("\uD83D\uDCC5 " + (p.getDateDebut() != null ? p.getDateDebut().format(DTF) : "N/A")
                + " → " + (p.getDateFin() != null ? p.getDateFin().format(DTF) : "N/A"));
        dateLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 10;");
        Label persLabel = new Label("\uD83D\uDC65 " + p.getNbPersonnes() + " personnes");
        persLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 10;");
        details.getChildren().addAll(dateLabel, persLabel);

        // Statut badge
        Label statutBadge = new Label(isActive ? "En attente" : "Annule");
        statutBadge.setStyle("-fx-background-color: " + (isActive ? "rgba(255,215,0,0.12)" : "rgba(255,107,107,0.12)") +
                "; -fx-text-fill: " + (isActive ? "#FFD700" : "#FF6B6B") +
                "; -fx-padding: 3 10; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold;");

        // Buttons
        HBox buttons = new HBox(8);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);

        if (isActive) {
            Button confirmBtn = new Button("\u2705 Confirmer Reservation");
            confirmBtn.getStyleClass().add("dashboard-button");
            confirmBtn.setStyle("-fx-padding: 8 16; -fx-font-size: 11;");
            confirmBtn.setOnAction(e -> confirmReservation(p));

            Button deleteBtn = new Button("\uD83D\uDDD1 Supprimer");
            deleteBtn.getStyleClass().add("dashboard-button-danger");
            deleteBtn.setStyle("-fx-padding: 8 16; -fx-font-size: 11;");
            deleteBtn.setOnAction(e -> deletePanierItem(p));

            buttons.getChildren().addAll(confirmBtn, deleteBtn);
        } else {
            Button deleteBtn = new Button("\uD83D\uDDD1 Supprimer");
            deleteBtn.getStyleClass().add("dashboard-button-danger");
            deleteBtn.setStyle("-fx-padding: 8 16; -fx-font-size: 11;");
            deleteBtn.setOnAction(e -> deletePanierItem(p));
            buttons.getChildren().add(deleteBtn);
        }

        card.getChildren().addAll(header, details, statutBadge, buttons);
        return card;
    }

    // ═══════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════

    private void confirmReservation(Panier p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la reservation");
        confirm.setHeaderText("Reserver: " + (p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService()));
        confirm.setContentText("Montant: " + String.format("%.2f DT", p.getPrixEstime()) + "\nVoulez-vous confirmer ?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Reservation r = new Reservation();
            r.setIdPanier(p.getIdPanier());
            r.setDatePaiement(LocalDateTime.now());
            r.setMontantTotal(p.getPrixEstime());
            r.setModePaiement("Carte Bancaire");
            r.setStatutPaiement("En cours de paiement");

            if (reservationService.ajouter(r)) {
                showMessage("Reservation confirmee ! Code: " + r.getCodeConfirmation(), true);
                loadData();
            } else {
                showMessage("Echec de la reservation.", false);
            }
        }
    }

    private void deletePanierItem(Panier p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer cet article du panier ?");
        confirm.setContentText(p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService());
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (panierService.supprimer(p.getIdPanier())) {
                showMessage("Article supprime du panier.", true);
                loadData();
            } else {
                showMessage("Echec de la suppression.", false);
            }
        }
    }

    private void showMessage(String msg, boolean success) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + (success ? "#51CF66" : "#FF6B6B") + ";");
            messageLabel.setText(msg);
        }
    }

    // ═══════════════════════════════════════════════════
    // CAROUSEL NAVIGATION
    // ═══════════════════════════════════════════════════

    @FXML public void handlePrev() {
        if (currentPage > 0) { currentPage--; renderCarousel(); }
    }
    @FXML public void handleNext() {
        if ((currentPage + 1) * CARDS_PER_PAGE < panierItems.size()) { currentPage++; renderCarousel(); }
    }

    // ═══════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════

    @FXML
    public void handleBackToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/maininterface.fxml"));
            Parent root = loader.load();
            if (currentUser != null) { MainInterfaceController c = loader.getController(); c.setUser(currentUser); }
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Accueil");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleGoToReservation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reservation.fxml"));
            Parent root = loader.load();
            if (currentUser != null) { ReservationController c = loader.getController(); c.setUser(currentUser); }
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Mes Paiements");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleMinimize() { ((Stage) titleBar.getScene().getWindow()).setIconified(true); }
    @FXML public void handleClose() { ((Stage) titleBar.getScene().getWindow()).close(); }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Connexion");
        } catch (Exception e) { e.printStackTrace(); }
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
            newScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            newRoot.setOpacity(0); newRoot.setScaleX(1.03); newRoot.setScaleY(1.03); newRoot.setTranslateY(8);
            stage.setScene(newScene); stage.setTitle(title);
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
