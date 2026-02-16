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
import com.esprit.entities.Lieu;
import com.esprit.entities.Etablissement;
import com.esprit.entities.Panier;
import com.esprit.entities.utilisateur;
import com.esprit.services.LieuService;
import com.esprit.services.EtablissementService;
import com.esprit.services.PanierService;
import com.esprit.services.ReservationService;
import com.esprit.services.utilisateurServices;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class MainInterfaceController {
    @FXML private HBox titleBar;
    @FXML private Label userNameLabel;
    @FXML private Label welcomeLabel;
    @FXML private StackPane panierBadge;
    @FXML private Label panierCountLabel;
    @FXML private Button navAccueil;
    @FXML private Button navLieux;
    @FXML private Button navRestaurants;
    @FXML private Button navPaiements;
    @FXML private Button navProfil;
    @FXML private ScrollPane panelAccueil;
    @FXML private ScrollPane panelLieux;
    @FXML private ScrollPane panelRestaurants;
    @FXML private ScrollPane panelProfil;
    @FXML private Label statLieuxLabel;
    @FXML private Label statEtabLabel;
    @FXML private Label statPanierLabel;
    @FXML private Label statPaiementsLabel;
    @FXML private HBox lieuxAccueilCarousel;
    @FXML private HBox etabAccueilCarousel;
    @FXML private HBox lieuxCarousel;
    @FXML private HBox etabCarousel;
    @FXML private Label lieuxPageLabel;
    @FXML private Label etabPageLabel;
    @FXML private Label profilInitials;
    @FXML private Label profilFullName;
    @FXML private Label profilEmail;
    @FXML private Label profilStatut;
    @FXML private Label profilDate;
    @FXML private Label profilMessage;
    @FXML private TextField editNomField;
    @FXML private TextField editPrenomField;
    @FXML private TextField editEmailField;
    @FXML private TextField editTelField;
    private double xOffset = 0;
    private double yOffset = 0;
    private utilisateur currentUser;
    private final utilisateurServices userService = new utilisateurServices();
    private final LieuService lieuService = new LieuService();
    private final EtablissementService etabService = new EtablissementService();
    private final PanierService panierService = new PanierService();
    private final ReservationService reservationService = new ReservationService();
    private List<Lieu> allLieux;
    private List<Etablissement> allEtabs;
    private int lieuxAccueilPage = 0;
    private int etabAccueilPage = 0;
    private int lieuxPage = 0;
    private int etabPage = 0;
    private static final int CARDS_PER_PAGE = 3;
    private static final String[] LIEU_ICONS = {"\uD83C\uDFDB", "\uD83C\uDFD6", "\uD83C\uDFDC", "\uD83C\uDFDB", "\uD83C\uDFDB", "\uD83C\uDFDD"};
    private static final String[] LIEU_COLORS = {"rgba(100,181,246,0.1)", "rgba(81,207,102,0.1)", "rgba(255,167,38,0.1)", "rgba(255,215,0,0.1)", "rgba(100,181,246,0.1)", "rgba(81,207,102,0.1)"};
    private static final String[] ETAB_ICONS = {"\uD83C\uDF55", "\u2615", "\uD83C\uDFCA", "\uD83D\uDC86", "\u26BD", "\uD83C\uDF56"};
    private static final String[] ETAB_COLORS = {"rgba(255,167,38,0.1)", "rgba(100,181,246,0.1)", "rgba(81,207,102,0.1)", "rgba(255,107,107,0.1)", "rgba(255,215,0,0.1)", "rgba(100,181,246,0.1)"};

    @FXML
    public void initialize() {
        if (titleBar != null) {
            titleBar.setOnMousePressed(event -> { xOffset = event.getSceneX(); yOffset = event.getSceneY(); });
            titleBar.setOnMouseDragged(event -> { Stage stage = (Stage) titleBar.getScene().getWindow(); stage.setX(event.getScreenX() - xOffset); stage.setY(event.getScreenY() - yOffset); });
        }
        allLieux = lieuService.getAll();
        allEtabs = etabService.getAll();
        renderCarousel(lieuxAccueilCarousel, allLieux, lieuxAccueilPage, true);
        renderCarousel(etabAccueilCarousel, allEtabs, etabAccueilPage, false);
        renderCarousel(lieuxCarousel, allLieux, lieuxPage, true);
        renderCarousel(etabCarousel, allEtabs, etabPage, false);
        updatePageLabels();
        showPanel("accueil");
    }

    public void setUser(utilisateur user) {
        this.currentUser = user;
        if (user != null) {
            String fullName = user.getNom() + " " + user.getPrenom();
            if (userNameLabel != null) userNameLabel.setText(fullName);
            if (welcomeLabel != null) welcomeLabel.setText("\uD83C\uDF0D Bonjour, " + user.getPrenom() + " !");
            populateProfile(user);
            updateStats();
            updatePanierBadge();
        }
    }

    public void setUserName(String name) { if (userNameLabel != null) userNameLabel.setText(name); }

    private void updateStats() {
        if (statLieuxLabel != null) statLieuxLabel.setText(allLieux.size() + " a explorer");
        if (statEtabLabel != null) statEtabLabel.setText(allEtabs.size() + " disponibles");
        if (currentUser != null) {
            int pc = panierService.countByClient(currentUser.getId());
            int rc = reservationService.countByClient(currentUser.getId());
            if (statPanierLabel != null) statPanierLabel.setText(pc + " articles");
            if (statPaiementsLabel != null) statPaiementsLabel.setText(rc + " reservations");
        }
    }

    private void updatePanierBadge() {
        if (currentUser != null && panierCountLabel != null) {
            int count = panierService.countByClient(currentUser.getId());
            panierCountLabel.setText(String.valueOf(count));
            if (panierBadge != null) panierBadge.setVisible(count > 0);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void renderCarousel(HBox container, List<T> items, int page, boolean isLieu) {
        if (container == null) return;
        container.getChildren().clear();
        int start = page * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, items.size());
        for (int i = start; i < end; i++) {
            VBox card;
            if (isLieu) { card = buildLieuCard((Lieu) items.get(i), i); }
            else { card = buildEtabCard((Etablissement) items.get(i), i); }
            HBox.setHgrow(card, Priority.ALWAYS);
            container.getChildren().add(card);
        }
        if (container.getChildren().isEmpty()) {
            Label empty = new Label("Aucun element a afficher");
            empty.setStyle("-fx-text-fill: #666; -fx-font-size: 13;");
            container.getChildren().add(empty);
        }
    }

    private VBox buildLieuCard(Lieu lieu, int index) {
        VBox card = new VBox(10);
        card.setMinWidth(300); card.setMaxWidth(380);
        card.setStyle("-fx-background-color: rgba(15,15,15,0.95); -fx-padding: 20; -fx-background-radius: 14; -fx-border-color: rgba(255,215,0,0.06); -fx-border-radius: 14; -fx-border-width: 1; -fx-cursor: hand;");
        String icon = index < LIEU_ICONS.length ? LIEU_ICONS[index] : "\uD83C\uDFDB";
        String color = index < LIEU_COLORS.length ? LIEU_COLORS[index] : "rgba(100,181,246,0.1)";
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane iconPane = new StackPane(); iconPane.setPrefSize(50, 50);
        iconPane.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 12;");
        Label iconLbl = new Label(icon); iconLbl.setStyle("-fx-font-size: 24;");
        iconPane.getChildren().add(iconLbl);
        VBox nameBox = new VBox(2); HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label nameLbl = new Label(lieu.getNom()); nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold;");
        Label villeLbl = new Label(lieu.getVille()); villeLbl.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");
        nameBox.getChildren().addAll(nameLbl, villeLbl);
        VBox priceBox = new VBox(2); priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label prixLbl = new Label(lieu.getPrix() > 0 ? String.format("%.0f DT", lieu.getPrix()) : "Gratuit");
        prixLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");
        Label catLbl = new Label(lieu.getNomCategorie() != null ? lieu.getNomCategorie() : "");
        catLbl.setStyle("-fx-text-fill: #666666; -fx-font-size: 9;");
        priceBox.getChildren().addAll(prixLbl, catLbl);
        header.getChildren().addAll(iconPane, nameBox, priceBox);
        Label descLbl = new Label(lieu.getDescription() != null ? lieu.getDescription() : "");
        descLbl.setStyle("-fx-text-fill: #777777; -fx-font-size: 11; -fx-wrap-text: true;"); descLbl.setWrapText(true);
        HBox footer = new HBox(8); footer.setAlignment(Pos.CENTER_LEFT);
        Label catBadge = new Label(lieu.getNomCategorie() != null ? lieu.getNomCategorie() : "Lieu");
        catBadge.setStyle("-fx-background-color: rgba(255,215,0,0.12); -fx-text-fill: #FFD700; -fx-padding: 3 10; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addBtn = new Button("\uD83D\uDED2 Ajouter au panier");
        addBtn.getStyleClass().add("dashboard-button"); addBtn.setStyle("-fx-padding: 6 14; -fx-font-size: 10;");
        addBtn.setOnAction(e -> showBookingDialog(lieu.getNom(), lieu.getPrix() > 0 ? lieu.getPrix() : 50.0, "Voyage", 0, true));
        footer.getChildren().addAll(catBadge, spacer, addBtn);
        card.getChildren().addAll(header, descLbl, footer);
        return card;
    }

    private VBox buildEtabCard(Etablissement etab, int index) {
        VBox card = new VBox(10);
        card.setMinWidth(300); card.setMaxWidth(380);
        card.setStyle("-fx-background-color: rgba(15,15,15,0.95); -fx-padding: 20; -fx-background-radius: 14; -fx-border-color: rgba(255,215,0,0.06); -fx-border-radius: 14; -fx-border-width: 1; -fx-cursor: hand;");
        String icon = index < ETAB_ICONS.length ? ETAB_ICONS[index] : "\uD83C\uDF55";
        String color = index < ETAB_COLORS.length ? ETAB_COLORS[index] : "rgba(255,167,38,0.1)";
        HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
        StackPane iconPane = new StackPane(); iconPane.setPrefSize(50, 50);
        iconPane.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 12;");
        Label iconLbl = new Label(icon); iconLbl.setStyle("-fx-font-size: 24;");
        iconPane.getChildren().add(iconLbl);
        VBox nameBox = new VBox(2); HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label nameLbl = new Label(etab.getNom()); nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold;");
        Label villeLbl = new Label(etab.getVille() + " - " + etab.getHoraires()); villeLbl.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");
        nameBox.getChildren().addAll(nameLbl, villeLbl);
        VBox priceBox = new VBox(2); priceBox.setAlignment(Pos.CENTER_RIGHT);
        String prixColor = "Eleve".equals(etab.getGammePrix()) ? "#FF6B6B" : "#51CF66";
        Label prixLbl = new Label("Prix: " + (etab.getGammePrix() != null ? etab.getGammePrix() : "N/A"));
        prixLbl.setStyle("-fx-text-fill: " + prixColor + "; -fx-font-size: 10;");
        priceBox.getChildren().add(prixLbl);
        header.getChildren().addAll(iconPane, nameBox, priceBox);
        Label descLbl = new Label(etab.getDescription() != null ? etab.getDescription() : "");
        descLbl.setStyle("-fx-text-fill: #777777; -fx-font-size: 11; -fx-wrap-text: true;"); descLbl.setWrapText(true);
        HBox footer = new HBox(8); footer.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addBtn = new Button("\uD83D\uDED2 Ajouter au panier");
        addBtn.getStyleClass().add("dashboard-button"); addBtn.setStyle("-fx-padding: 6 14; -fx-font-size: 10;");
        double basePrice = "Eleve".equals(etab.getGammePrix()) ? 120.0 : ("Moyen".equals(etab.getGammePrix()) ? 80.0 : 50.0);
        addBtn.setOnAction(e -> showBookingDialog(etab.getNom(), basePrice, "Restaurant", etab.getIdEtablissement(), false));
        footer.getChildren().addAll(spacer, addBtn);
        card.getChildren().addAll(header, descLbl, footer);
        return card;
    }

    // ================================================================
    // BOOKING DIALOG - date, people, days (if hotel/voyage), total price
    // ================================================================
    private void showBookingDialog(String itemName, double pricePerUnit, String typeService, int etabId, boolean isLieu) {
        if (currentUser == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reservation");
        dialog.setHeaderText("Reserver: " + itemName);

        // Style the dialog
        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1a1a;");
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1a1a1a;");

        // Title
        Label titleLbl = new Label("\uD83D\uDCC5 Details de la reservation");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");

        // Date picker
        VBox dateBox = new VBox(4);
        Label dateLbl = new Label("Date de debut");
        dateLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");
        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(3));
        datePicker.setStyle("-fx-background-color: #222; -fx-text-fill: white;");
        dateBox.getChildren().addAll(dateLbl, datePicker);

        // Number of people
        VBox peopleBox = new VBox(4);
        Label peopleLbl = new Label("\uD83D\uDC65 Nombre de personnes");
        peopleLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");
        Spinner<Integer> peopleSpinner = new Spinner<>(1, 20, 2);
        peopleSpinner.setEditable(true);
        peopleSpinner.setStyle("-fx-background-color: #222;");
        peopleBox.getChildren().addAll(peopleLbl, peopleSpinner);

        // Number of days (only for Voyage / hotel)
        VBox daysBox = new VBox(4);
        Label daysLbl = new Label("\uD83C\uDFE8 Nombre de jours");
        daysLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");
        Spinner<Integer> daysSpinner = new Spinner<>(1, 30, 1);
        daysSpinner.setEditable(true);
        daysSpinner.setStyle("-fx-background-color: #222;");
        daysBox.getChildren().addAll(daysLbl, daysSpinner);
        daysBox.setVisible(isLieu);
        daysBox.setManaged(isLieu);

        // Price display
        VBox priceDisplayBox = new VBox(6);
        priceDisplayBox.setStyle("-fx-background-color: rgba(255,215,0,0.08); -fx-padding: 14; -fx-background-radius: 10;");
        priceDisplayBox.setAlignment(Pos.CENTER);
        Label priceInfoLbl = new Label("Prix unitaire: " + String.format("%.2f DT", pricePerUnit));
        priceInfoLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");
        Label totalLabel = new Label();
        totalLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 22; -fx-font-weight: bold;");
        priceDisplayBox.getChildren().addAll(priceInfoLbl, totalLabel);

        // Calculate total price
        Runnable updateTotal = () -> {
            int people = peopleSpinner.getValue();
            int days = isLieu ? daysSpinner.getValue() : 1;
            double total = pricePerUnit * people * days;
            totalLabel.setText(String.format("Total: %.2f DT", total));
        };
        updateTotal.run();
        peopleSpinner.valueProperty().addListener((obs, o, n) -> updateTotal.run());
        if (isLieu) daysSpinner.valueProperty().addListener((obs, o, n) -> updateTotal.run());

        content.getChildren().addAll(titleLbl, dateBox, peopleBox, daysBox, priceDisplayBox);
        dp.setContent(content);
        dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Style buttons
        Button okButton = (Button) dp.lookupButton(ButtonType.OK);
        okButton.setText("\uD83D\uDED2 Ajouter au Panier");
        okButton.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FF8C00); -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 8;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            LocalDate selectedDate = datePicker.getValue();
            if (selectedDate == null) selectedDate = LocalDate.now().plusDays(3);
            int people = peopleSpinner.getValue();
            int days = isLieu ? daysSpinner.getValue() : 1;
            double totalPrice = pricePerUnit * people * days;

            Panier p = new Panier();
            p.setIdClient(currentUser.getId());
            p.setIdEtablissement(etabId > 0 ? etabId : 1);
            p.setTypeService(typeService);
            p.setDateDebut(selectedDate.atStartOfDay());
            p.setDateFin(selectedDate.plusDays(days).atStartOfDay());
            p.setNbPersonnes(people);
            p.setPrixEstime(totalPrice);
            p.setStatutItem("en_attente");
            p.setNomEtablissement(itemName);

            if (panierService.ajouter(p)) {
                updatePanierBadge();
                updateStats();
                showAlert("Ajoute au panier !", itemName + " a ete ajoute.\nTotal: " + String.format("%.2f DT", totalPrice));
            }
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg); alert.showAndWait();
    }

    @FXML public void handleLieuxAccueilPrev() { if (lieuxAccueilPage > 0) { lieuxAccueilPage--; renderCarousel(lieuxAccueilCarousel, allLieux, lieuxAccueilPage, true); } }
    @FXML public void handleLieuxAccueilNext() { if ((lieuxAccueilPage + 1) * CARDS_PER_PAGE < allLieux.size()) { lieuxAccueilPage++; renderCarousel(lieuxAccueilCarousel, allLieux, lieuxAccueilPage, true); } }
    @FXML public void handleEtabAccueilPrev() { if (etabAccueilPage > 0) { etabAccueilPage--; renderCarousel(etabAccueilCarousel, allEtabs, etabAccueilPage, false); } }
    @FXML public void handleEtabAccueilNext() { if ((etabAccueilPage + 1) * CARDS_PER_PAGE < allEtabs.size()) { etabAccueilPage++; renderCarousel(etabAccueilCarousel, allEtabs, etabAccueilPage, false); } }
    @FXML public void handleLieuxPrev() { if (lieuxPage > 0) { lieuxPage--; renderCarousel(lieuxCarousel, allLieux, lieuxPage, true); updatePageLabels(); } }
    @FXML public void handleLieuxNext() { if ((lieuxPage + 1) * CARDS_PER_PAGE < allLieux.size()) { lieuxPage++; renderCarousel(lieuxCarousel, allLieux, lieuxPage, true); updatePageLabels(); } }
    @FXML public void handleEtabPrev() { if (etabPage > 0) { etabPage--; renderCarousel(etabCarousel, allEtabs, etabPage, false); updatePageLabels(); } }
    @FXML public void handleEtabNext() { if ((etabPage + 1) * CARDS_PER_PAGE < allEtabs.size()) { etabPage++; renderCarousel(etabCarousel, allEtabs, etabPage, false); updatePageLabels(); } }

    private void updatePageLabels() {
        int lt = (int) Math.ceil((double) allLieux.size() / CARDS_PER_PAGE);
        int et2 = (int) Math.ceil((double) allEtabs.size() / CARDS_PER_PAGE);
        if (lieuxPageLabel != null) lieuxPageLabel.setText((lieuxPage + 1) + " / " + Math.max(1, lt));
        if (etabPageLabel != null) etabPageLabel.setText((etabPage + 1) + " / " + Math.max(1, et2));
    }

    private void showPanel(String panel) {
        ScrollPane[] panels = {panelAccueil, panelLieux, panelRestaurants, panelProfil};
        for (ScrollPane sp : panels) { if (sp != null) sp.setVisible(false); }
        Button[] navButtons = {navAccueil, navLieux, navRestaurants, navPaiements, navProfil};
        for (Button btn : navButtons) { if (btn != null) { btn.getStyleClass().removeAll("sidebar-button-active"); if (!btn.getStyleClass().contains("sidebar-button")) btn.getStyleClass().add("sidebar-button"); } }
        ScrollPane target = null; Button activeBtn = null;
        switch (panel) {
            case "accueil": target = panelAccueil; activeBtn = navAccueil; break;
            case "lieux": target = panelLieux; activeBtn = navLieux; break;
            case "restaurants": target = panelRestaurants; activeBtn = navRestaurants; break;
            case "profil": target = panelProfil; activeBtn = navProfil; break;
        }
        if (target != null) { target.setVisible(true); target.setOpacity(0); FadeTransition fade = new FadeTransition(Duration.millis(250), target); fade.setFromValue(0); fade.setToValue(1); fade.setInterpolator(Interpolator.EASE_OUT); fade.play(); }
        if (activeBtn != null) { activeBtn.getStyleClass().remove("sidebar-button"); activeBtn.getStyleClass().add("sidebar-button-active"); }
    }

    @FXML public void handleShowAccueil() { showPanel("accueil"); }
    @FXML public void handleShowLieux() { showPanel("lieux"); }
    @FXML public void handleShowRestaurants() { showPanel("restaurants"); }
    @FXML public void handleShowProfil() { showPanel("profil"); }

    @FXML public void handleGoToPanier() {
        try { FXMLLoader loader = new FXMLLoader(getClass().getResource("/panier.fxml")); Parent root = loader.load(); if (currentUser != null) { PanierController ctrl = loader.getController(); ctrl.setUser(currentUser); } Stage stage = (Stage) titleBar.getScene().getWindow(); doFadeTransition(stage, root, "SmartTravel - Mon Panier"); } catch (Exception e) { e.printStackTrace(); }
    }
    @FXML public void handleGoToMesPaiements() {
        try { FXMLLoader loader = new FXMLLoader(getClass().getResource("/reservation.fxml")); Parent root = loader.load(); if (currentUser != null) { ReservationController ctrl = loader.getController(); ctrl.setUser(currentUser); } Stage stage = (Stage) titleBar.getScene().getWindow(); doFadeTransition(stage, root, "SmartTravel - Mes Paiements"); } catch (Exception e) { e.printStackTrace(); }
    }
    @FXML public void handleGoToProfile() {
        try { FXMLLoader loader = new FXMLLoader(getClass().getResource("/userprofile.fxml")); Parent root = loader.load(); if (currentUser != null) { UserProfileController ctrl = loader.getController(); ctrl.setUser(currentUser); } Stage stage = (Stage) titleBar.getScene().getWindow(); doFadeTransition(stage, root, "SmartTravel - Mon Profil"); } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateProfile(utilisateur user) {
        if (profilInitials != null) { String ini = ""; if (user.getNom() != null && !user.getNom().isEmpty()) ini += user.getNom().charAt(0); if (user.getPrenom() != null && !user.getPrenom().isEmpty()) ini += user.getPrenom().charAt(0); profilInitials.setText(ini.toUpperCase()); }
        if (profilFullName != null) profilFullName.setText(user.getNom() + " " + user.getPrenom());
        if (profilEmail != null) profilEmail.setText(user.getEmail());
        if (editNomField != null) editNomField.setText(user.getNom());
        if (editPrenomField != null) editPrenomField.setText(user.getPrenom());
        if (editEmailField != null) editEmailField.setText(user.getEmail());
        if (editTelField != null) editTelField.setText(user.getNumTel() != 0 ? String.valueOf(user.getNumTel()) : "");
        if (profilStatut != null) profilStatut.setText("Statut: " + (user.getStatut() != null ? user.getStatut() : "ACTIF"));
        if (profilDate != null) profilDate.setText("Membre depuis: " + (user.getDateCreation() != null ? user.getDateCreation().toString() : "--"));
        if (profilMessage != null) profilMessage.setText("");
    }

    @FXML public void handleSaveProfile() {
        if (currentUser == null) { showProfileMsg("Erreur: utilisateur non connecte.", false); return; }
        String nom = editNomField != null ? editNomField.getText().trim() : "";
        String prenom = editPrenomField != null ? editPrenomField.getText().trim() : "";
        String email = editEmailField != null ? editEmailField.getText().trim() : "";
        String tel = editTelField != null ? editTelField.getText().trim() : "";
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) { showProfileMsg("Veuillez remplir tous les champs obligatoires.", false); return; }
        try {
            int telNum = tel.isEmpty() ? 0 : Integer.parseInt(tel);
            boolean success = userService.updateProfile(currentUser.getId(), prenom, nom, email, telNum);
            if (!success) { showProfileMsg("Echec de la mise a jour du profil.", false); return; }
            currentUser.setNom(nom); currentUser.setPrenom(prenom); currentUser.setEmail(email); currentUser.setNumTel(telNum);
            String fullName = nom + " " + prenom;
            if (userNameLabel != null) userNameLabel.setText(fullName);
            if (profilFullName != null) profilFullName.setText(fullName);
            if (profilEmail != null) profilEmail.setText(email);
            if (welcomeLabel != null) welcomeLabel.setText("\uD83C\uDF0D Bonjour, " + prenom + " !");
            if (profilInitials != null) { String ini = ""; if (!nom.isEmpty()) ini += nom.charAt(0); if (!prenom.isEmpty()) ini += prenom.charAt(0); profilInitials.setText(ini.toUpperCase()); }
            showProfileMsg("Profil mis a jour avec succes !", true);
        } catch (NumberFormatException ex) { showProfileMsg("Numero de telephone invalide.", false); }
        catch (Exception ex) { showProfileMsg("Erreur: " + ex.getMessage(), false); ex.printStackTrace(); }
    }

    private void showProfileMsg(String msg, boolean success) {
        if (profilMessage != null) { profilMessage.setStyle("-fx-font-size: 12; -fx-text-fill: " + (success ? "#51CF66" : "#FF6B6B") + ";"); profilMessage.setText(msg); }
    }

    @FXML public void handleMinimize() { Stage stage = (Stage) titleBar.getScene().getWindow(); stage.setIconified(true); }
    @FXML public void handleClose() { Stage stage = (Stage) titleBar.getScene().getWindow(); stage.close(); }

    @FXML public void handleLogout() {
        try { FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml")); Parent root = loader.load(); Stage stage = (Stage) titleBar.getScene().getWindow(); doFadeTransition(stage, root, "SmartTravel - Connexion"); } catch (Exception e) { e.printStackTrace(); }
    }

    private void doFadeTransition(Stage stage, Parent newRoot, String title) {
        Scene oldScene = stage.getScene(); Node oldRoot = oldScene.getRoot();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), oldRoot); fadeOut.setFromValue(1); fadeOut.setToValue(0); fadeOut.setInterpolator(Interpolator.EASE_IN);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(300), oldRoot); scaleOut.setToX(0.97); scaleOut.setToY(0.97); scaleOut.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition exitAnim = new ParallelTransition(fadeOut, scaleOut);
        exitAnim.setOnFinished(e -> {
            Scene newScene = new Scene(newRoot); newScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            newRoot.setOpacity(0); newRoot.setScaleX(1.03); newRoot.setScaleY(1.03); newRoot.setTranslateY(8);
            stage.setScene(newScene); stage.setTitle(title);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), newRoot); fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), newRoot); scaleIn.setToX(1); scaleIn.setToY(1);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), newRoot); slideIn.setToY(0);
            new ParallelTransition(fadeIn, scaleIn, slideIn).play();
        });
        exitAnim.play();
    }
}