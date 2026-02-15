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

import com.esprit.entities.Reservation;
import com.esprit.entities.Etablissement;
import com.esprit.entities.utilisateur;
import com.esprit.services.ReservationService;
import com.esprit.services.EtablissementService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservationController {

    @FXML private HBox titleBar;
    @FXML private Label userNameLabel;
    @FXML private Label totalReservationsLabel;
    @FXML private Label payeLabel;
    @FXML private Label enCoursLabel;
    @FXML private Label totalDepenseLabel;
    @FXML private Label messageLabel;
    @FXML private HBox reservationCarousel;
    @FXML private Label pageLabel;

    private double xOffset = 0;
    private double yOffset = 0;
    private utilisateur currentUser;
    private final ReservationService reservationService = new ReservationService();
    private final EtablissementService etabService = new EtablissementService();

    private List<Reservation> reservations;
    private int currentPage = 0;
    private static final int CARDS_PER_PAGE = 3;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Card colors by index
    private static final String[] CARD_COLORS = {
        "rgba(255,215,0,0.08)", "rgba(81,207,102,0.08)", "rgba(100,181,246,0.08)",
        "rgba(255,107,107,0.08)", "rgba(178,102,255,0.08)", "rgba(255,167,38,0.08)"
    };
    private static final String[] STATUT_ICONS = { "\u2705", "\u23F3", "\u274C", "\uD83D\uDCB3" };

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
        reservations = reservationService.getReservationsByClient(currentUser.getId());

        // Resolve etablissement names where needed
        for (Reservation r : reservations) {
            if (r.getNomEtablissement() == null || r.getNomEtablissement().isEmpty()) {
                // Try to get from type_service
                r.setNomEtablissement(r.getTypeService() != null ? r.getTypeService() : "Service");
            }
        }

        updateStats();
        renderCarousel();
    }

    private void updateStats() {
        int total = reservations.size();
        long paye = reservations.stream().filter(r -> "Paye".equalsIgnoreCase(r.getStatutPaiement()) || "Payé".equalsIgnoreCase(r.getStatutPaiement())).count();
        long enCours = reservations.stream().filter(r -> "En cours de paiement".equalsIgnoreCase(r.getStatutPaiement())).count();
        double totalDepense = reservations.stream().mapToDouble(Reservation::getMontantTotal).sum();

        if (totalReservationsLabel != null) totalReservationsLabel.setText(String.valueOf(total));
        if (payeLabel != null) payeLabel.setText(String.valueOf(paye));
        if (enCoursLabel != null) enCoursLabel.setText(String.valueOf(enCours));
        if (totalDepenseLabel != null) totalDepenseLabel.setText(String.format("%.2f DT", totalDepense));
    }

    private void renderCarousel() {
        if (reservationCarousel == null) return;
        reservationCarousel.getChildren().clear();

        int start = currentPage * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, reservations.size());

        for (int i = start; i < end; i++) {
            VBox card = buildReservationCard(reservations.get(i), i);
            HBox.setHgrow(card, Priority.ALWAYS);
            reservationCarousel.getChildren().add(card);
        }

        if (reservationCarousel.getChildren().isEmpty()) {
            Label empty = new Label("Aucun paiement pour le moment. Confirmez des articles de votre panier pour voir vos reservations ici.");
            empty.setStyle("-fx-text-fill: #666; -fx-font-size: 14; -fx-padding: 40;");
            empty.setWrapText(true);
            reservationCarousel.getChildren().add(empty);
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) reservations.size() / CARDS_PER_PAGE));
        if (pageLabel != null) pageLabel.setText((currentPage + 1) + " / " + totalPages);
    }

    private VBox buildReservationCard(Reservation r, int index) {
        VBox card = new VBox(14);
        card.setMinWidth(300);
        card.setMaxWidth(400);

        String bgColor = CARD_COLORS[index % CARD_COLORS.length];
        String statutColor = getStatutColor(r.getStatutPaiement());
        String statutIcon = getStatutIcon(r.getStatutPaiement());

        card.setStyle("-fx-background-color: rgba(15,15,15,0.95); -fx-padding: 20; -fx-background-radius: 14; " +
                "-fx-border-color: " + bgColor.replace("0.08", "0.15") + "; -fx-border-radius: 14; -fx-border-width: 1;");

        // ═══ Header: icon + name + code ═══
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(50, 50);
        iconPane.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12;");
        Label iconLabel = new Label(statutIcon);
        iconLabel.setStyle("-fx-font-size: 22;");
        iconPane.getChildren().add(iconLabel);

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        String displayName = r.getNomEtablissement() != null && !r.getNomEtablissement().isEmpty()
                ? r.getNomEtablissement() : (r.getTypeService() != null ? r.getTypeService() : "Reservation");
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");

        Label codeLabel = new Label("\uD83D\uDD11 " + (r.getCodeConfirmation() != null ? r.getCodeConfirmation() : "N/A"));
        codeLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 10; -fx-font-weight: bold;");

        nameBox.getChildren().addAll(nameLabel, codeLabel);
        header.getChildren().addAll(iconPane, nameBox);

        // ═══ Montant ═══
        HBox montantRow = new HBox(8);
        montantRow.setAlignment(javafx.geometry.Pos.CENTER);
        montantRow.setStyle("-fx-background-color: rgba(255,215,0,0.05); -fx-padding: 12; -fx-background-radius: 10;");
        Label montantLabel = new Label(String.format("%.2f DT", r.getMontantTotal()));
        montantLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 20; -fx-font-weight: bold;");
        montantRow.getChildren().add(montantLabel);

        // ═══ Details ═══
        VBox details = new VBox(6);
        details.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 10; -fx-background-radius: 8;");

        Label dateLabel = new Label("\uD83D\uDCC5 " + (r.getDatePaiement() != null ? r.getDatePaiement().format(DTF) : "N/A"));
        dateLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 10;");

        Label modeLabel = new Label("\uD83D\uDCB3 " + (r.getModePaiement() != null ? r.getModePaiement() : "N/A"));
        modeLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 10;");

        Label persLabel = new Label("\uD83D\uDC65 " + r.getNbPersonnes() + " personnes");
        persLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 10;");

        Label serviceLabel = new Label("\uD83C\uDFAF " + (r.getTypeService() != null ? r.getTypeService() : "N/A"));
        serviceLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 10;");

        details.getChildren().addAll(dateLabel, modeLabel, persLabel, serviceLabel);

        // ═══ Statut badge ═══
        HBox statutRow = new HBox();
        statutRow.setAlignment(javafx.geometry.Pos.CENTER);
        Label statutBadge = new Label(statutIcon + " " + (r.getStatutPaiement() != null ? r.getStatutPaiement() : "Inconnu"));
        statutBadge.setStyle("-fx-background-color: " + statutColor.replace(")", ",0.12)").replace("rgb(", "rgba(") +
                "; -fx-text-fill: " + statutColor +
                "; -fx-padding: 5 14; -fx-background-radius: 10; -fx-font-size: 11; -fx-font-weight: bold;");
        statutRow.getChildren().add(statutBadge);

        // ═══ Action buttons ═══
        HBox buttons = new HBox(8);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);

        boolean isEnCours = "En cours de paiement".equalsIgnoreCase(r.getStatutPaiement());
        if (isEnCours) {
            Button payerBtn = new Button("\u2705 Marquer Paye");
            payerBtn.getStyleClass().add("dashboard-button");
            payerBtn.setStyle("-fx-padding: 8 16; -fx-font-size: 11;");
            payerBtn.setOnAction(e -> markAsPaid(r));
            buttons.getChildren().add(payerBtn);
        }

        Button deleteBtn = new Button("\uD83D\uDDD1 Supprimer");
        deleteBtn.getStyleClass().add("dashboard-button-danger");
        deleteBtn.setStyle("-fx-padding: 8 16; -fx-font-size: 11;");
        deleteBtn.setOnAction(e -> deleteReservation(r));
        buttons.getChildren().add(deleteBtn);

        card.getChildren().addAll(header, montantRow, details, statutRow, buttons);
        return card;
    }

    // ═══════════════════════════════════════════════════
    // STATUT HELPERS
    // ═══════════════════════════════════════════════════

    private String getStatutColor(String statut) {
        if (statut == null) return "rgb(136,136,136)";
        String lower = statut.toLowerCase();
        if (lower.contains("pay")) return "rgb(81,207,102)";
        if (lower.contains("cours")) return "rgb(100,181,246)";
        if (lower.contains("rembours")) return "rgb(255,107,107)";
        return "rgb(255,215,0)";
    }

    private String getStatutIcon(String statut) {
        if (statut == null) return "\uD83D\uDCB3";
        String lower = statut.toLowerCase();
        if (lower.contains("pay")) return "\u2705";
        if (lower.contains("cours")) return "\u23F3";
        if (lower.contains("rembours")) return "\u274C";
        return "\uD83D\uDCB3";
    }

    // ═══════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════

    private void markAsPaid(Reservation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer le paiement");
        confirm.setHeaderText("Marquer comme paye ?");
        confirm.setContentText("Code: " + r.getCodeConfirmation() + "\nMontant: " + String.format("%.2f DT", r.getMontantTotal()));
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                if (reservationService.updateStatut(r.getIdReservation(), "Payé")) {
                    showMessage("Paiement confirme ! Code: " + r.getCodeConfirmation(), true);
                    loadData();
                } else {
                    showMessage("Echec de la mise a jour.", false);
                }
            }
        });
    }

    private void deleteReservation(Reservation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la reservation");
        confirm.setHeaderText("Supprimer cette reservation ?");
        confirm.setContentText("Code: " + r.getCodeConfirmation());
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                if (reservationService.supprimer(r.getIdReservation())) {
                    showMessage("Reservation supprimee.", true);
                    loadData();
                } else {
                    showMessage("Echec de la suppression.", false);
                }
            }
        });
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
        if ((currentPage + 1) * CARDS_PER_PAGE < reservations.size()) { currentPage++; renderCarousel(); }
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
    public void handleGoToPanier() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/panier.fxml"));
            Parent root = loader.load();
            if (currentUser != null) { PanierController c = loader.getController(); c.setUser(currentUser); }
            Stage stage = (Stage) titleBar.getScene().getWindow();
            fadeTransition(stage, root, "SmartTravel - Mon Panier");
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
