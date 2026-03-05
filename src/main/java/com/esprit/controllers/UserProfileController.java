package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.Node;

import com.esprit.entities.utilisateur;
import com.esprit.services.utilisateurServices;
import com.esprit.services.AuditLogService;
import com.esprit.services.TOTPService;
import com.esprit.utils.ThemeManager;
import com.esprit.utils.SessionManager;
import com.esprit.services.TranslationService;

import java.io.*;
import java.util.Base64;
import java.util.Optional;

public class UserProfileController {

    // ================= TITLE BAR =================
    @FXML private HBox titleBar;
    @FXML private Label userNameLabel;
    @FXML private StackPane contentArea;

    // ================= PANELS =================
    @FXML private VBox profilePane;
    @FXML private VBox securityPane;
    @FXML private VBox activityPane;
    @FXML private VBox settingsPane;
    @FXML private VBox supportPane;

    // ================= NAV BUTTONS =================
    @FXML private Button navProfile;
    @FXML private Button navSecurity;
    @FXML private Button navActivity;
    @FXML private Button navSettings;
    @FXML private Button navSupport;

    // ================= SIDEBAR AVATAR =================
    @FXML private Label avatarInitials;
    @FXML private ImageView profileImageView;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;

    // ================= PROFILE AVATAR (large) =================
    @FXML private Label avatarInitialsLarge;
    @FXML private ImageView profileImageViewLarge;
    @FXML private Label photoMessageLabel;

    // ================= PROFILE FIELDS =================
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField roleField;
    @FXML private TextField statusField;
    @FXML private TextField dateField;
    @FXML private Circle statusDot;
    @FXML private Label profileMessageLabel;

    // ================= SECURITY FIELDS =================
    @FXML private PasswordField currentPasswordField;
    @FXML private TextField currentPasswordVisible;
    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordVisible;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisible;
    @FXML private Label securityMessageLabel;
    @FXML private Label deleteMessageLabel;

    // ================= 2FA FIELDS =================
    @FXML private Label twoFAStatusLabel;
    @FXML private VBox qrCodeContainer;
    @FXML private ImageView qrCodeImageView;
    @FXML private Label totpSecretLabel;
    @FXML private TextField totpVerifyField;
    @FXML private Button toggle2FAButton;
    @FXML private Label twoFAMessageLabel;
    private String pendingTotpSecret;  // temp secret during setup

    // ================= ACTIVITY LABELS =================
    @FXML private Label memberSinceLabel;
    @FXML private Label accountStatusLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label userIdLabel;
    @FXML private Label nfcIdLabel;
    @FXML private Label activityEmailLabel;

    // ================= SETTINGS =================
    @FXML private CheckBox emailNotifCheck;
    @FXML private CheckBox smsNotifCheck;
    @FXML private ComboBox<String> languageCombo;
    @FXML private CheckBox profileVisibleCheck;
    @FXML private CheckBox activityVisibleCheck;
    @FXML private CheckBox darkModeToggle;

    // ================= SUPPORT / TICKET =================
    @FXML private VBox ticketFormContainer;
    @FXML private TextField ticketSubjectField;
    @FXML private ComboBox<String> ticketPriorityCombo;
    @FXML private TextArea ticketDescriptionArea;
    @FXML private Label ticketMessageLabel;

    // ================= STATE =================
    private utilisateurServices userService;
    private AuditLogService auditService;
    private TOTPService totpService;
    private utilisateur currentUser;
    private boolean isAdmin = false;
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean currentPwVisible = false;
    private boolean newPwVisible = false;
    private boolean confirmPwVisible = false;

    // Translation support
    private final java.util.LinkedHashMap<Labeled, String> staticOriginals = new java.util.LinkedHashMap<>();
    private boolean staticLabelsCollected = false;

    @FXML
    public void initialize() {
        userService = new utilisateurServices();
        auditService = new AuditLogService();
        totpService = new TOTPService();
        showPane(profilePane);

        // Window drag
        if (titleBar != null) {
            titleBar.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
            titleBar.setOnMouseDragged(e -> {
                Stage s = (Stage) titleBar.getScene().getWindow();
                s.setX(e.getScreenX() - xOffset);
                s.setY(e.getScreenY() - yOffset);
            });
        }

        // Phone numeric filter
        if (phoneField != null) {
            phoneField.textProperty().addListener((obs, o, n) -> {
                if (!n.matches("\\d*")) phoneField.setText(n.replaceAll("[^\\d]", ""));
            });
        }

        // Language combo - wired to TranslationService + DB persistence
        if (languageCombo != null) {
            languageCombo.getItems().addAll("FR - Français", "EN - English", "ES - Español", "DE - Deutsch", "IT - Italiano", "AR - العربية");
            languageCombo.setValue("FR - Français");
            languageCombo.setOnAction(e -> {
                String selected = languageCombo.getValue();
                if (selected != null && currentUser != null) {
                    String langCode = selected.substring(0, 2).toLowerCase();
                    TranslationService.setCurrentLang(langCode);
                    currentUser.setLanguage(langCode);
                    new Thread(() -> userService.saveLanguagePreference(currentUser.getId(), langCode)).start();
                }
            });
        }

        // Theme toggle — sync with current theme
        if (darkModeToggle != null) {
            darkModeToggle.setSelected(ThemeManager.isDark());
        }

        // Session timeout — start monitoring once scene is available
        javafx.application.Platform.runLater(() -> {
            if (contentArea != null && contentArea.getScene() != null) {
                SessionManager.getInstance().startMonitoring(contentArea.getScene());
            }
            collectStaticLabels();
            String lang = TranslationService.getCurrentLang();
            if (!"fr".equals(lang)) applyTranslation();
        });
        TranslationService.addLanguageChangeListener(this::applyTranslation);
    }

    // ================= THEME TOGGLE =================

    private void collectStaticLabels() {
        if (staticLabelsCollected) return;
        javafx.scene.Parent root = null;
        if (contentArea != null && contentArea.getScene() != null) root = contentArea.getScene().getRoot();
        if (root != null) {
            root.lookupAll(".button").forEach(n -> {
                if (n instanceof Labeled l && l.getText() != null && !l.getText().isBlank() && l.getText().length() > 1)
                    staticOriginals.putIfAbsent(l, l.getText());
            });
            root.lookupAll("Label").forEach(n -> {
                if (n instanceof Label l && l.getText() != null && !l.getText().isBlank() && l.getText().length() > 1)
                    staticOriginals.putIfAbsent(l, l.getText());
            });
        }
        staticLabelsCollected = true;
    }

    private void applyTranslation() {
        String lang = TranslationService.getCurrentLang();
        if (!staticLabelsCollected) collectStaticLabels();
        if ("fr".equals(lang)) {
            staticOriginals.forEach((lbl, txt) -> lbl.setText(txt));
        } else {
            staticOriginals.forEach((lbl, txt) ->
                TranslationService.translateAsync(txt, lang, lbl::setText));
        }
    }

    @FXML
    private void handleThemeToggle() {
        ThemeManager.toggleTheme();
        if (contentArea != null && contentArea.getScene() != null) {
            ThemeManager.applyTheme(contentArea.getScene());
        }
    }

    // ================= SET USER =================

    public void setUser(utilisateur user) {
        this.currentUser = user;
        this.isAdmin = userService.isAdmin(user.getRoleId());
        populateAll();
        refresh2FAStatus();

        // Sync language combo with user's saved language
        if (languageCombo != null && user.getLanguage() != null) {
            String lang = user.getLanguage().toLowerCase();
            for (String item : languageCombo.getItems()) {
                if (item.toLowerCase().startsWith(lang)) {
                    languageCombo.setValue(item);
                    break;
                }
            }
            TranslationService.setCurrentLang(lang);
        }

        // Hide ticket submission for admins - only users can submit tickets
        if (isAdmin) {
            if (ticketFormContainer != null) {
                ticketFormContainer.setVisible(false);
                ticketFormContainer.setManaged(false);
            }
        }
    }

    private void populateAll() {
        if (currentUser == null) return;

        String fullName = currentUser.getPrenom() + " " + currentUser.getNom();
        String initials = "";
        if (currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty())
            initials += currentUser.getPrenom().charAt(0);
        if (currentUser.getNom() != null && !currentUser.getNom().isEmpty())
            initials += currentUser.getNom().charAt(0);
        initials = initials.toUpperCase();

        // Title bar
        if (userNameLabel != null) userNameLabel.setText(fullName);

        // Sidebar
        if (sidebarUserName != null) sidebarUserName.setText(fullName);
        if (sidebarUserRole != null) sidebarUserRole.setText(userService.getRoleName(currentUser.getRoleId()));
        if (avatarInitials != null) avatarInitials.setText(initials);
        if (avatarInitialsLarge != null) avatarInitialsLarge.setText(initials);

        // Profile fields
        if (firstNameField != null) firstNameField.setText(currentUser.getPrenom());
        if (lastNameField != null) lastNameField.setText(currentUser.getNom());
        if (emailField != null) emailField.setText(currentUser.getEmail());
        if (phoneField != null) phoneField.setText(String.valueOf(currentUser.getNumTel()));
        if (roleField != null) roleField.setText(userService.getRoleName(currentUser.getRoleId()));
        if (statusField != null) statusField.setText(currentUser.getStatut());
        if (statusDot != null) {
            statusDot.setStyle("ACTIF".equalsIgnoreCase(currentUser.getStatut())
                    ? "-fx-fill: #51CF66;" : "-fx-fill: #FF6B6B;");
        }
        if (dateField != null) {
            dateField.setText(currentUser.getDateCreation() != null
                    ? currentUser.getDateCreation().toString() : "-");
        }

        // Activity panel
        if (memberSinceLabel != null) {
            memberSinceLabel.setText(currentUser.getDateCreation() != null
                    ? currentUser.getDateCreation().toString() : "-");
        }
        if (accountStatusLabel != null) {
            accountStatusLabel.setText(currentUser.getStatut());
            accountStatusLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: "
                    + ("ACTIF".equalsIgnoreCase(currentUser.getStatut()) ? "#51CF66" : "#FF6B6B") + ";");
        }
        if (userRoleLabel != null) userRoleLabel.setText(userService.getRoleName(currentUser.getRoleId()));
        if (userIdLabel != null) userIdLabel.setText("#" + currentUser.getId());
        if (nfcIdLabel != null) nfcIdLabel.setText(currentUser.getNfcId() != null ? currentUser.getNfcId() : "Non défini");
        if (activityEmailLabel != null) activityEmailLabel.setText(currentUser.getEmail());

        // Load profile picture
        loadProfilePicture();
    }

    // ================= PROFILE PICTURE =================

    private void loadProfilePicture() {
        String base64 = currentUser.getProfilePicture();
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] imageData = Base64.getDecoder().decode(base64);
                Image img = new Image(new ByteArrayInputStream(imageData));

                if (profileImageView != null) {
                    profileImageView.setImage(img);
                    profileImageView.setVisible(true);
                    if (avatarInitials != null) avatarInitials.setVisible(false);
                }
                if (profileImageViewLarge != null) {
                    profileImageViewLarge.setImage(img);
                    profileImageViewLarge.setVisible(true);
                    if (avatarInitialsLarge != null) avatarInitialsLarge.setVisible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleUploadPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fc.showOpenDialog(contentArea.getScene().getWindow());
        if (file == null) return;

        // Check size (max 2MB)
        if (file.length() > 2 * 1024 * 1024) {
            showMessage(photoMessageLabel, "❌ Image trop grande (max 2 MB)", false);
            return;
        }

        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);

            boolean success = userService.updateProfilePicture(currentUser.getId(), base64);
            if (success) {
                currentUser.setProfilePicture(base64);
                loadProfilePicture();
                showMessage(photoMessageLabel, "✅ Photo mise à jour !", true);
            } else {
                showMessage(photoMessageLabel, "❌ Erreur lors de l'upload", false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showMessage(photoMessageLabel, "❌ Erreur de lecture du fichier", false);
        }
    }

    @FXML
    private void handleRemovePhoto() {
        if (currentUser == null) return;

        boolean success = userService.updateProfilePicture(currentUser.getId(), null);
        if (success) {
            currentUser.setProfilePicture(null);

            // Reset to initials
            if (profileImageView != null) { profileImageView.setVisible(false); profileImageView.setImage(null); }
            if (profileImageViewLarge != null) { profileImageViewLarge.setVisible(false); profileImageViewLarge.setImage(null); }
            if (avatarInitials != null) avatarInitials.setVisible(true);
            if (avatarInitialsLarge != null) avatarInitialsLarge.setVisible(true);

            showMessage(photoMessageLabel, "✅ Photo supprimée", true);
        }
    }

    // ================= NAVIGATION =================

    @FXML private void handleShowProfile() { setActiveNav(navProfile); showPane(profilePane); }
    @FXML private void handleShowSecurity() { setActiveNav(navSecurity); showPane(securityPane); }
    @FXML private void handleShowActivity() { setActiveNav(navActivity); showPane(activityPane); }
    @FXML private void handleShowSettings() { setActiveNav(navSettings); showPane(settingsPane); }
    @FXML private void handleShowSupport() { setActiveNav(navSupport); showPane(supportPane); }

    private void setActiveNav(Button active) {
        Button[] navButtons = { navProfile, navSecurity, navActivity, navSettings, navSupport };
        for (Button b : navButtons) {
            if (b != null) {
                b.getStyleClass().removeAll("sidebar-button-active");
                if (!b.getStyleClass().contains("sidebar-button")) b.getStyleClass().add("sidebar-button");
            }
        }
        if (active != null) {
            active.getStyleClass().removeAll("sidebar-button");
            if (!active.getStyleClass().contains("sidebar-button-active")) active.getStyleClass().add("sidebar-button-active");
        }
    }

    private void showPane(VBox paneToShow) {
        VBox[] allPanes = { profilePane, securityPane, activityPane, settingsPane, supportPane };
        for (VBox p : allPanes) {
            if (p != null) p.setVisible(false);
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

    // ================= SAVE PROFILE =================

    @FXML
    private void handleSaveProfile() {
        if (currentUser == null) return;

        String prenom = firstNameField.getText().trim();
        String nom = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showMessage(profileMessageLabel, "❌ Tous les champs sont obligatoires", false);
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showMessage(profileMessageLabel, "❌ Email invalide", false);
            return;
        }

        try {
            int numTel = Integer.parseInt(phone);
            boolean success = userService.updateProfile(currentUser.getId(), prenom, nom, email, numTel);

            if (success) {
                currentUser.setPrenom(prenom);
                currentUser.setNom(nom);
                currentUser.setEmail(email);
                currentUser.setNumTel(numTel);

                String fullName = prenom + " " + nom;
                if (userNameLabel != null) userNameLabel.setText(fullName);
                if (sidebarUserName != null) sidebarUserName.setText(fullName);

                String initials = ("" + prenom.charAt(0) + nom.charAt(0)).toUpperCase();
                if (avatarInitials != null) avatarInitials.setText(initials);
                if (avatarInitialsLarge != null) avatarInitialsLarge.setText(initials);

                // Update activity panel
                if (activityEmailLabel != null) activityEmailLabel.setText(email);

                showMessage(profileMessageLabel, "✅ Profil mis à jour avec succès !", true);
            } else {
                showMessage(profileMessageLabel, "❌ Erreur lors de la mise à jour", false);
            }
        } catch (NumberFormatException e) {
            showMessage(profileMessageLabel, "❌ Numéro de téléphone invalide", false);
        }
    }

    // ================= PASSWORD TOGGLES =================

    @FXML
    private void handleToggleCurrentPw() {
        currentPwVisible = !currentPwVisible;
        togglePasswordFields(currentPasswordField, currentPasswordVisible, currentPwVisible);
    }

    @FXML
    private void handleToggleNewPw() {
        newPwVisible = !newPwVisible;
        togglePasswordFields(newPasswordField, newPasswordVisible, newPwVisible);
    }

    @FXML
    private void handleToggleConfirmPw() {
        confirmPwVisible = !confirmPwVisible;
        togglePasswordFields(confirmPasswordField, confirmPasswordVisible, confirmPwVisible);
    }

    private void togglePasswordFields(PasswordField hidden, TextField visible, boolean show) {
        if (show) {
            visible.setText(hidden.getText());
            visible.setVisible(true);
            visible.setManaged(true);
            hidden.setVisible(false);
            hidden.setManaged(false);
        } else {
            hidden.setText(visible.getText());
            hidden.setVisible(true);
            hidden.setManaged(true);
            visible.setVisible(false);
            visible.setManaged(false);
        }
    }

    // ================= CHANGE PASSWORD =================

    @FXML
    private void handleChangePassword() {
        if (currentUser == null) return;

        String current = currentPwVisible ? currentPasswordVisible.getText() : currentPasswordField.getText();
        String newPass = newPwVisible ? newPasswordVisible.getText() : newPasswordField.getText();
        String confirm = confirmPwVisible ? confirmPasswordVisible.getText() : confirmPasswordField.getText();

        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showMessage(securityMessageLabel, "❌ Tous les champs sont obligatoires", false);
            return;
        }

        if (newPass.length() < 8) {
            showMessage(securityMessageLabel, "❌ Le mot de passe doit avoir au moins 8 caractères", false);
            return;
        }

        if (!newPass.equals(confirm)) {
            showMessage(securityMessageLabel, "❌ Les mots de passe ne correspondent pas", false);
            return;
        }

        boolean success = userService.changePassword(currentUser.getId(), current, newPass);
        if (success) {
            showMessage(securityMessageLabel, "✅ Mot de passe modifié avec succès !", true);
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            if (currentPasswordVisible != null) currentPasswordVisible.clear();
            if (newPasswordVisible != null) newPasswordVisible.clear();
            if (confirmPasswordVisible != null) confirmPasswordVisible.clear();
        } else {
            showMessage(securityMessageLabel, "❌ Mot de passe actuel incorrect", false);
        }
    }

    // ================= TWO-FACTOR AUTHENTICATION =================

    /**
     * Refresh the 2FA status labels and buttons based on current DB state.
     */
    private void refresh2FAStatus() {
        if (currentUser == null) return;
        boolean enabled = totpService.is2FAEnabled(currentUser.getId());
        currentUser.setTotpEnabled(enabled);

        if (twoFAStatusLabel != null) {
            twoFAStatusLabel.setText(enabled ? "Activé ✓" : "Désactivé");
            twoFAStatusLabel.setStyle(enabled
                    ? "-fx-text-fill: #51CF66; -fx-font-size: 12; -fx-font-weight: bold;"
                    : "-fx-text-fill: #FF6B6B; -fx-font-size: 12; -fx-font-weight: bold;");
        }

        if (toggle2FAButton != null) {
            toggle2FAButton.setText(enabled ? "\uD83D\uDEE1  Désactiver la 2FA" : "\uD83D\uDEE1  Activer la 2FA");
            toggle2FAButton.setStyle(enabled
                    ? "-fx-padding: 10 30; -fx-background-color: rgba(255,75,75,0.15); -fx-text-fill: #FF6B6B; -fx-background-radius: 12; -fx-border-color: rgba(255,75,75,0.3); -fx-border-radius: 12; -fx-font-weight: bold; -fx-cursor: hand;"
                    : "-fx-padding: 10 30;");
        }

        // Hide QR container if already enabled
        if (qrCodeContainer != null) {
            qrCodeContainer.setVisible(false);
            qrCodeContainer.setManaged(false);
        }
    }

    /**
     * Toggle 2FA: if disabled → show QR setup flow; if enabled → disable after confirmation.
     */
    @FXML
    private void handleToggle2FA() {
        if (currentUser == null) return;

        boolean enabled = totpService.is2FAEnabled(currentUser.getId());

        if (enabled) {
            // ── DISABLE 2FA ──
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.initStyle(javafx.stage.StageStyle.UNDECORATED);
            confirm.setHeaderText(null);
            DialogPane dp = confirm.getDialogPane();
            dp.getStylesheets().add(ThemeManager.class.getResource("/style.css").toExternalForm());
            if (!ThemeManager.isDark()) dp.getStylesheets().add(ThemeManager.class.getResource("/style-light.css").toExternalForm());
            dp.setMinWidth(420);

            VBox content = new VBox(12);
            content.setStyle("-fx-padding: 10 5;");
            Label titleLbl = new Label("⚠ Désactiver la 2FA ?");
            titleLbl.setStyle("-fx-font-size: 16; -fx-text-fill: #FF6B6B; -fx-font-weight: bold;");
            Label msgLbl = new Label("Votre compte sera moins sécurisé sans la vérification\nen deux étapes. Vous pourrez la réactiver à tout moment.");
            msgLbl.setStyle("-fx-text-fill: #dddddd; -fx-font-size: 13;");
            msgLbl.setWrapText(true);
            content.getChildren().addAll(titleLbl, new Separator(), msgLbl);
            dp.setContent(content);

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean success = totpService.disable2FA(currentUser.getId());
                if (success) {
                    currentUser.setTotpEnabled(false);
                    currentUser.setTotpSecret(null);
                    refresh2FAStatus();
                    showMessage(twoFAMessageLabel, "✅ 2FA désactivée", true);
                } else {
                    showMessage(twoFAMessageLabel, "❌ Erreur lors de la désactivation", false);
                }
            }
        } else {
            // ── ENABLE 2FA: generate secret + show QR ──
            pendingTotpSecret = totpService.generateSecretKey();

            try {
                javafx.scene.image.Image qrImage = totpService.generateQRCodeImage(
                        pendingTotpSecret, currentUser.getEmail(), 200, 200);
                if (qrCodeImageView != null) qrCodeImageView.setImage(qrImage);
            } catch (Exception ex) {
                showMessage(twoFAMessageLabel, "❌ Erreur de génération du QR code", false);
                ex.printStackTrace();
                return;
            }

            if (totpSecretLabel != null) {
                totpSecretLabel.setText("Clé secrète : " + pendingTotpSecret);
            }

            if (qrCodeContainer != null) {
                qrCodeContainer.setVisible(true);
                qrCodeContainer.setManaged(true);
                // Animate in
                qrCodeContainer.setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(300), qrCodeContainer);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            }

            if (totpVerifyField != null) {
                totpVerifyField.clear();
                totpVerifyField.requestFocus();
                // Digits-only filter
                totpVerifyField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal.matches("\\d*")) totpVerifyField.setText(newVal.replaceAll("[^\\d]", ""));
                    if (newVal.length() > 6) totpVerifyField.setText(newVal.substring(0, 6));
                });
            }

            showMessage(twoFAMessageLabel, "", true);
        }
    }

    /**
     * Confirm 2FA activation by verifying the 6-digit code from the authenticator app.
     */
    @FXML
    private void handleConfirm2FA() {
        if (currentUser == null || pendingTotpSecret == null) return;

        String code = totpVerifyField != null ? totpVerifyField.getText().trim() : "";
        if (code.length() != 6) {
            showMessage(twoFAMessageLabel, "❌ Entrez un code à 6 chiffres", false);
            return;
        }

        try {
            boolean valid = totpService.verifyCode(pendingTotpSecret, Integer.parseInt(code));
            if (valid) {
                boolean saved = totpService.saveSecret(currentUser.getId(), pendingTotpSecret);
                if (saved) {
                    currentUser.setTotpSecret(pendingTotpSecret);
                    currentUser.setTotpEnabled(true);
                    pendingTotpSecret = null;
                    refresh2FAStatus();
                    showMessage(twoFAMessageLabel, "✅ 2FA activée avec succès ! Votre compte est maintenant protégé.", true);
                } else {
                    showMessage(twoFAMessageLabel, "❌ Erreur de sauvegarde en base de données", false);
                }
            } else {
                showMessage(twoFAMessageLabel, "❌ Code invalide. Vérifiez votre application et réessayez.", false);
                if (totpVerifyField != null) {
                    totpVerifyField.clear();
                    totpVerifyField.requestFocus();
                }
            }
        } catch (NumberFormatException ex) {
            showMessage(twoFAMessageLabel, "❌ Code invalide", false);
        }
    }

    // ================= DELETE ACCOUNT =================

    @FXML
    private void handleDeleteAccount() {
        if (currentUser == null) return;

        Alert alert = new Alert(Alert.AlertType.NONE, "", ButtonType.OK, ButtonType.CANCEL);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        alert.setHeaderText(null);

        DialogPane dp = alert.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dp.setMinWidth(450);

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 10 5;");
        Label titleLbl = new Label("\u26a0 Supprimer votre compte ?");
        titleLbl.setStyle("-fx-font-size: 16; -fx-text-fill: #FF6B6B; -fx-font-weight: bold;");
        Separator sep = new Separator();
        Label msgLbl = new Label("Cette action est irr\u00e9versible. Toutes vos donn\u00e9es seront\nd\u00e9finitivement perdues et ne pourront pas \u00eatre r\u00e9cup\u00e9r\u00e9es.");
        msgLbl.setStyle("-fx-text-fill: #dddddd; -fx-font-size: 13;");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(400);
        content.getChildren().addAll(titleLbl, sep, msgLbl);
        dp.setContent(content);

        Button cancelBtn = (Button) dp.lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setStyle("-fx-background-color: #333333; -fx-text-fill: #cccccc; -fx-font-weight: bold; -fx-padding: 10 28; -fx-background-radius: 8; -fx-cursor: hand;");
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = userService.deleteAccount(currentUser.getId());
            if (success) {
                // Redirect to login
                handleLogout();
            } else {
                showMessage(deleteMessageLabel, "❌ Erreur lors de la suppression", false);
            }
        }
    }

    // ================= HELPERS =================

    private void showMessage(Label label, String msg, boolean success) {
        if (label != null) {
            label.setText(msg);
            label.setStyle("-fx-text-fill: " + (success ? "#51CF66" : "#FF6B6B") + "; -fx-font-weight: bold;");
        }
    }

    // ================= TICKET SUBMISSION =================

    @FXML
    private void handleSubmitTicket() {
        String subject = ticketSubjectField != null ? ticketSubjectField.getText().trim() : "";
        String priority = ticketPriorityCombo != null ? ticketPriorityCombo.getValue() : null;
        String desc = ticketDescriptionArea != null ? ticketDescriptionArea.getText().trim() : "";

        if (subject.isEmpty() || desc.isEmpty()) {
            showMessage(ticketMessageLabel, "❌ Veuillez remplir le sujet et la description.", false);
            return;
        }

        if (currentUser != null) {
            auditService.log(currentUser.getId(), currentUser.getPrenom() + " " + currentUser.getNom(),
                    "SUBMIT_TICKET", "SUPPORT", 0, subject, null, priority);
        }

        showMessage(ticketMessageLabel, "✅ Ticket soumis avec succès !", true);
        if (ticketSubjectField != null) ticketSubjectField.clear();
        if (ticketDescriptionArea != null) ticketDescriptionArea.clear();
        if (ticketPriorityCombo != null) ticketPriorityCombo.setValue(null);
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
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToMain() {
        try {
            Stage stage = (Stage) contentArea.getScene().getWindow();

            if (isAdmin) {
                // Admin goes back to dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
                Parent root = loader.load();

                DashboardController controller = loader.getController();
                if (currentUser != null) {
                    controller.setUserName(currentUser.getNom());
                    controller.setAdminId(currentUser.getId());
                }

                fadeTransition(stage, root, "Tabaani - Dashboard");
            } else if (currentUser != null && userService.isPartenaire(currentUser.getRoleId())) {
                // Partenaire goes back to partenaire interface
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/partenaireinterface.fxml"));
                Parent root = loader.load();

                PartenaireInterfaceController controller = loader.getController();
                controller.setUser(currentUser);

                fadeTransition(stage, root, "Tabaani - Espace Partenaire");
            } else {
                // Tourist goes back to main interface
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/maininterface.fxml"));
                Parent root = loader.load();

                if (currentUser != null) {
                    MainInterfaceController controller = loader.getController();
                    controller.setUser(currentUser);
                }

                fadeTransition(stage, root, "Tabaani - Accueil");
            }
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
            ThemeManager.applyTheme(newScene);
            newScene.setFill(javafx.scene.paint.Color.BLACK);
            newRoot.setOpacity(0);
            newRoot.setScaleX(1.03);
            newRoot.setScaleY(1.03);
            newRoot.setTranslateY(8);
            stage.setScene(newScene);
            stage.setMaximized(true);
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
            new ParallelTransition(fadeIn, scaleIn, slideIn).play();
        });
        exitAnim.play();
    }
}
