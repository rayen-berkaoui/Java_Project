package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.application.Platform;

import com.esprit.services.utilisateurServices;
import com.esprit.services.EmailService;
import com.esprit.services.OTPService;
import com.esprit.services.PasswordStrengthService;
import com.esprit.services.SmsOTPService;
import com.esprit.utils.ThemeManager;

public class signupController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    @FXML private RadioButton touristeRadio;
    @FXML private RadioButton partenaireRadio;

    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button togglePasswordButton;

    @FXML private PasswordField confirmPasswordField;
    @FXML private CheckBox termsCheckBox;
    @FXML private Label errorLabel;
    @FXML private Label passwordHintLabel;
    @FXML private Button signupButton;
    @FXML private ProgressBar strengthBar;
    @FXML private javafx.scene.layout.HBox titleBar;

    private utilisateurServices utilisateurService;
    private EmailService emailService;
    private OTPService otpService;
    private PasswordStrengthService passwordStrengthService;
    private SmsOTPService smsOTPService;

    private boolean passwordVisible = false;
    private double xOffset = 0;
    private double yOffset = 0;

    // ═══ Pending registration state ═══
    private String pendingEmail;
    private String pendingFirstName;
    private String pendingLastName;
    private String pendingPassword;
    private int pendingNumTel;
    private int pendingRoleId;
    private String pendingOtpMethod = "EMAIL"; // "EMAIL" or "SMS"
    private Timeline countdownTimeline;

    // ═══ Dialog references ═══
    private Stage verificationDialog;
    private TextField[] dialogCodeFields;
    private Label dialogTimerLabel;
    private Label dialogErrorLabel;
    private Button dialogVerifyButton;
    private Button dialogResendButton;

    public signupController() {
        utilisateurService = new utilisateurServices();
        emailService = new EmailService();
        otpService = new OTPService();
        passwordStrengthService = new PasswordStrengthService();
        smsOTPService = new SmsOTPService();
    }

    @FXML
    public void initialize() {

        signupButton.setDisable(true);
        strengthBar.setProgress(0);

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

        // ✅ Prevent letters in phone
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                phoneField.setText(newVal.replaceAll("[^\\d]", ""));
                shake(phoneField);
            }
        });

        // ✅ Sync password fields
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        passwordField.textProperty().addListener((obs, o, n) -> {
            updateStrength(n);
            validatePasswords();
            validateForm();
        });

        confirmPasswordField.textProperty().addListener((obs, o, n) -> {
            validatePasswords();
            validateForm();
        });

        firstNameField.textProperty().addListener((obs, o, n) -> validateForm());
        lastNameField.textProperty().addListener((obs, o, n) -> validateForm());
        emailField.textProperty().addListener((obs, o, n) -> validateForm());
        phoneField.textProperty().addListener((obs, o, n) -> validateForm());

        touristeRadio.selectedProperty().addListener((obs, o, n) -> validateForm());
        partenaireRadio.selectedProperty().addListener((obs, o, n) -> validateForm());

        termsCheckBox.selectedProperty().addListener((obs, o, n) -> validateForm());

        // Entrance animation
        Platform.runLater(() -> {
            try {
                Parent root = signupButton.getScene().getRoot();
                root.setOpacity(0);
                root.setTranslateY(12);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                TranslateTransition slideUp = new TranslateTransition(Duration.millis(500), root);
                slideUp.setFromY(12);
                slideUp.setToY(0);
                slideUp.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
                new ParallelTransition(fadeIn, slideUp).play();
            } catch (Exception ignored) {}
        });
    }

    // ✅ Toggle visibility
    @FXML
    public void handleTogglePassword() {

        passwordVisible = !passwordVisible;

        visiblePasswordField.setVisible(passwordVisible);
        visiblePasswordField.setManaged(passwordVisible);

        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);
    }

    @FXML
    public void handleShowTerms() {
        Stage termsStage = new Stage();
        termsStage.initModality(Modality.APPLICATION_MODAL);
        termsStage.initStyle(StageStyle.UNDECORATED);
        termsStage.setTitle("Conditions d'utilisation");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #FFD700; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        root.setPrefSize(650, 520);

        // Title bar
        HBox titleBarTerms = new HBox();
        titleBarTerms.setAlignment(Pos.CENTER_LEFT);
        titleBarTerms.setPadding(new Insets(14, 18, 14, 18));
        titleBarTerms.setStyle("-fx-background-color: #111; -fx-background-radius: 12 12 0 0;");

        Label titleLabel = new Label("\uD83D\uDCC4  Conditions d'utilisation - SmartTravel");
        titleLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 15; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-font-size: 16; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> termsStage.close());
        titleBarTerms.getChildren().addAll(titleLabel, spacer, closeBtn);

        // Terms content
        String termsText = """
        CONDITIONS GÉNÉRALES D'UTILISATION DE SMARTTRAVEL

        Dernière mise à jour : 16 Février 2026

        ══════════════════════════════════════════

        1. ACCEPTATION DES CONDITIONS
        En accédant à SmartTravel et en créant un compte, vous acceptez d'être lié par les présentes conditions d'utilisation. Si vous n'acceptez pas ces conditions, veuillez ne pas utiliser notre plateforme.

        2. DESCRIPTION DU SERVICE
        SmartTravel est une plateforme de voyage et loisirs permettant aux utilisateurs de :
        • Découvrir et explorer des destinations en Tunisie
        • Réserver des restaurants et activités de loisirs
        • Partager des avis et recommandations
        • Gérer leurs réservations et favoris

        3. INSCRIPTION ET COMPTE UTILISATEUR
        • Vous devez fournir des informations exactes et à jour lors de l'inscription.
        • Vous êtes responsable de la confidentialité de votre mot de passe.
        • Tout usage non autorisé de votre compte doit être signalé immédiatement.
        • SmartTravel se réserve le droit de suspendre ou supprimer tout compte en cas de violation.

        4. PROTECTION DES DONNÉES PERSONNELLES
        • Vos données personnelles sont collectées et traitées conformément à la législation tunisienne.
        • Les données de reconnaissance faciale (Face ID) sont stockées de manière sécurisée et chiffrée.
        • Vous pouvez demander la suppression de vos données à tout moment.
        • Nous ne partageons jamais vos données avec des tiers sans votre consentement.

        5. UTILISATION ACCEPTABLE
        Vous vous engagez à :
        • Ne pas publier de contenu offensant, frauduleux ou illégal.
        • Ne pas tenter d'accéder aux comptes d'autres utilisateurs.
        • Ne pas utiliser la plateforme à des fins commerciales non autorisées.
        • Respecter les autres utilisateurs et partenaires.

        6. PROPRIÉTÉ INTELLECTUELLE
        Tout le contenu de SmartTravel (logos, textes, images, design) est protégé par le droit d'auteur. Toute reproduction non autorisée est interdite.

        7. LIMITATION DE RESPONSABILITÉ
        SmartTravel ne peut être tenu responsable :
        • Des interruptions temporaires du service.
        • De l'exactitude des informations fournies par les partenaires.
        • Des dommages indirects liés à l'utilisation de la plateforme.

        8. MODIFICATION DES CONDITIONS
        SmartTravel se réserve le droit de modifier ces conditions à tout moment. Les utilisateurs seront notifiés de tout changement significatif.

        9. CONTACT
        Pour toute question relative aux présentes conditions :
        📧 support@smarttravel.tn
        📞 +216 71 000 000

        ══════════════════════════════════════════
        © 2025-2026 SmartTravel - Tous droits réservés.
        """;

        TextArea termsArea = new TextArea(termsText);
        termsArea.setEditable(false);
        termsArea.setWrapText(true);
        termsArea.setStyle(
            "-fx-control-inner-background: #1a1a1a; -fx-text-fill: #cccccc; " +
            "-fx-font-size: 13; -fx-font-family: 'Segoe UI'; " +
            "-fx-border-width: 0; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;"
        );
        VBox.setVgrow(termsArea, Priority.ALWAYS);

        // Bottom bar with accept button
        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(12, 18, 14, 18));
        bottomBar.setSpacing(12);
        bottomBar.setStyle("-fx-background-color: #111; -fx-background-radius: 0 0 12 12;");

        Button acceptBtn = new Button("✔  J'ai lu et j'accepte");
        acceptBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #FFD700, #FFA500); " +
            "-fx-text-fill: #111; -fx-font-weight: bold; -fx-font-size: 13; " +
            "-fx-padding: 10 28; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        acceptBtn.setOnAction(e -> {
            termsCheckBox.setSelected(true);
            termsStage.close();
        });

        Button declineBtn = new Button("Refuser");
        declineBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #999; " +
            "-fx-font-size: 12; -fx-border-color: #555; -fx-border-radius: 8; " +
            "-fx-padding: 10 20; -fx-cursor: hand;"
        );
        declineBtn.setOnAction(e -> {
            termsCheckBox.setSelected(false);
            termsStage.close();
        });

        bottomBar.getChildren().addAll(declineBtn, acceptBtn);

        root.getChildren().addAll(titleBarTerms, termsArea, bottomBar);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        termsStage.initStyle(StageStyle.TRANSPARENT);
        termsStage.setScene(scene);
        termsStage.showAndWait();
    }

    @FXML
    public void handleSignup() {

        if (signupButton.isDisable()) {
            showError("❌ Formulaire invalide");
            return;
        }

        try {
            int numTel = Integer.parseInt(phoneField.getText());
            int roleId = touristeRadio.isSelected() ? 1 : 2;
            String email = emailField.getText().trim();

            // Check if email already used
            if (utilisateurService.emailExists(email)) {
                showError("❌ Email déjà utilisé");
                return;
            }

            // Store pending registration data
            pendingEmail = email;
            pendingFirstName = firstNameField.getText().trim();
            pendingLastName = lastNameField.getText().trim();
            pendingPassword = passwordField.getText();
            pendingNumTel = numTel;
            pendingRoleId = roleId;

            // Disable button while sending
            signupButton.setDisable(true);
            signupButton.setText("Envoi en cours...");
            errorLabel.setText("");

            // Show OTP method choice dialog
            showOtpMethodChoiceDialog();

        } catch (NumberFormatException e) {
            showError("❌ Numéro invalide");
        }
    }

    /**
     * Show a choice dialog: send OTP via Email or SMS
     */
    private void showOtpMethodChoiceDialog() {
        Stage choiceDialog = new Stage();
        choiceDialog.initStyle(StageStyle.TRANSPARENT);
        choiceDialog.initModality(Modality.APPLICATION_MODAL);
        choiceDialog.initOwner((Stage) signupButton.getScene().getWindow());

        VBox container = new VBox(18);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(30, 40, 30, 40));
        container.setMaxWidth(400);
        container.setStyle(
            "-fx-background-color: #111111;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255,215,0,0.25);" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 1.5;"
        );
        container.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.7)));

        Label icon = new Label("\uD83D\uDD10");
        icon.setStyle("-fx-font-size: 40;");

        Label title = new Label("VÉRIFICATION DU COMPTE");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 17; -fx-font-weight: bold;");

        Label subtitle = new Label("Choisissez comment recevoir votre code de vérification");
        subtitle.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12; -fx-text-alignment: center;");
        subtitle.setWrapText(true);

        // ── Email button ──
        Button emailBtn = new Button("✉  Envoyer par Email");
        emailBtn.setPrefWidth(280);
        emailBtn.setPrefHeight(50);
        emailBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #FFD700, #FFA500);" +
            "-fx-text-fill: #0a0a0a;" +
            "-fx-font-size: 14;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;"
        );
        Label emailHint = new Label(maskEmail(pendingEmail));
        emailHint.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");

        // ── SMS button ──
        Button smsBtn = new Button("\uD83D\uDCF1  Envoyer par SMS");
        smsBtn.setPrefWidth(280);
        smsBtn.setPrefHeight(50);
        String phoneDisplay = String.valueOf(pendingNumTel);
        boolean smsAvailable = smsOTPService.isConfigured() && phoneDisplay.length() >= 8;
        if (smsAvailable) {
            smsBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-text-fill: #51CF66;" +
                "-fx-font-size: 14;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: rgba(81,207,102,0.3);" +
                "-fx-border-radius: 12;"
            );
        } else {
            smsBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04);" +
                "-fx-text-fill: #555555;" +
                "-fx-font-size: 14;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(100,100,100,0.2);" +
                "-fx-border-radius: 12;"
            );
            smsBtn.setDisable(true);
        }
        String maskedPhone = phoneDisplay.length() >= 8
            ? "****" + phoneDisplay.substring(phoneDisplay.length() - 4)
            : phoneDisplay;
        Label smsHint = new Label(smsAvailable ? maskedPhone : "Service SMS non disponible");
        smsHint.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");

        // ── Cancel ──
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #888888; -fx-font-size: 12; -fx-cursor: hand; -fx-underline: true;"
        );

        container.getChildren().addAll(icon, title, subtitle, emailBtn, emailHint, smsBtn, smsHint, cancelBtn);

        // ── Actions ──
        emailBtn.setOnAction(e -> {
            pendingOtpMethod = "EMAIL";
            choiceDialog.close();
            sendOtpViaChosenMethod();
        });

        smsBtn.setOnAction(e -> {
            pendingOtpMethod = "SMS";
            choiceDialog.close();
            sendOtpViaChosenMethod();
        });

        cancelBtn.setOnAction(e -> {
            choiceDialog.close();
            signupButton.setDisable(false);
            signupButton.setText("CRÉER MON COMPTE  ›");
        });

        StackPane overlay = new StackPane(container);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        overlay.setAlignment(Pos.CENTER);

        Scene scene = new Scene(overlay, 480, 440);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                choiceDialog.close();
                signupButton.setDisable(false);
                signupButton.setText("CRÉER MON COMPTE  ›");
            }
        });

        choiceDialog.setScene(scene);

        // Entrance animation
        container.setOpacity(0);
        container.setScaleX(0.85);
        container.setScaleY(0.85);
        choiceDialog.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), container);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), container);
        scaleIn.setFromX(0.85); scaleIn.setFromY(0.85); scaleIn.setToX(1); scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        new ParallelTransition(fadeIn, scaleIn).play();
    }

    /**
     * Send OTP using the method chosen by the user (EMAIL or SMS)
     */
    private void sendOtpViaChosenMethod() {
        new Thread(() -> {
            String otp = otpService.generateOTP();
            boolean stored = otpService.storeOTP(pendingEmail, otp);
            boolean sent = false;

            if ("SMS".equals(pendingOtpMethod)) {
                String phone = String.valueOf(pendingNumTel);
                sent = stored && smsOTPService.sendSmsOTP(phone, otp);
            } else {
                sent = stored && emailService.sendVerificationEmail(pendingEmail, otp);
            }

            final boolean success = sent;
            Platform.runLater(() -> {
                if (success) {
                    showVerificationDialog();
                } else {
                    signupButton.setDisable(false);
                    signupButton.setText("CRÉER MON COMPTE  ›");
                    showError("SMS".equals(pendingOtpMethod)
                        ? "❌ Erreur d'envoi du SMS. Réessayez."
                        : "❌ Erreur d'envoi de l'email. Réessayez.");
                }
            });
        }).start();
    }

    /**
     * Show verification dialog as a styled popup
     */
    private void showVerificationDialog() {
        Stage owner = (Stage) signupButton.getScene().getWindow();

        verificationDialog = new Stage();
        verificationDialog.initStyle(StageStyle.TRANSPARENT);
        verificationDialog.initModality(Modality.APPLICATION_MODAL);
        verificationDialog.initOwner(owner);

        // ═══ Build dialog content programmatically ═══
        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(32, 36, 28, 36));
        root.setStyle(
            "-fx-background-color: #111111;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255,215,0,0.25);" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 1.5;"
        );
        root.setEffect(new DropShadow(35, Color.rgb(0, 0, 0, 0.7)));
        root.setMaxWidth(420);
        root.setMaxHeight(Region.USE_PREF_SIZE);

        // Close button (top-right)
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #666; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 0;"
        );
        closeBtn.setOnAction(e -> closeVerificationDialog());
        HBox topBar = new HBox(closeBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);

        // Dynamic icon + title based on OTP method
        boolean viaSms = "SMS".equals(pendingOtpMethod);
        Label icon = new Label(viaSms ? "📱" : "📧");
        icon.setStyle("-fx-font-size: 36;");

        Label title = new Label(viaSms ? "VÉRIFICATION SMS" : "VÉRIFICATION EMAIL");
        title.setStyle(
            "-fx-text-fill: #FFD700; -fx-font-size: 17; -fx-font-weight: bold; -fx-letter-spacing: 1;"
        );

        // Gold accent line
        Region accentLine = new Region();
        accentLine.setPrefHeight(2);
        accentLine.setMaxWidth(180);
        accentLine.setStyle("-fx-background-color: linear-gradient(to right, transparent, #FFD700, transparent);");

        // Info label — show email or phone depending on method
        String destination = viaSms
            ? "****" + String.valueOf(pendingNumTel).substring(Math.max(0, String.valueOf(pendingNumTel).length() - 4))
            : maskEmail(pendingEmail);
        Label infoLabel = new Label("Un code de vérification a été envoyé" + (viaSms ? " au\n" : " à\n") + destination);
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12.5; -fx-text-alignment: center;");
        infoLabel.setAlignment(Pos.CENTER);

        // ═══ 6 code input fields ═══
        dialogCodeFields = new TextField[6];
        HBox codeBox = new HBox(8);
        codeBox.setAlignment(Pos.CENTER);
        codeBox.setPadding(new Insets(6, 0, 6, 0));

        for (int i = 0; i < 6; i++) {
            if (i == 3) {
                Label dash = new Label("—");
                dash.setStyle("-fx-text-fill: rgba(255,215,0,0.3); -fx-font-size: 18;");
                codeBox.getChildren().add(dash);
            }
            TextField tf = new TextField();
            tf.setPrefWidth(48);
            tf.setPrefHeight(52);
            tf.setAlignment(Pos.CENTER);
            tf.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04);" +
                "-fx-border-color: rgba(255,215,0,0.2);" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-text-fill: #FFD700;" +
                "-fx-font-size: 22;" +
                "-fx-font-weight: bold;" +
                "-fx-alignment: center;" +
                "-fx-padding: 0;"
            );
            dialogCodeFields[i] = tf;
            codeBox.getChildren().add(tf);
        }
        setupDialogCodeFields();

        // Timer
        dialogTimerLabel = new Label("⏱ 60s restantes");
        dialogTimerLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12; -fx-font-weight: 600;");

        // Verify button
        dialogVerifyButton = new Button("VÉRIFIER  ✓");
        dialogVerifyButton.setPrefHeight(44);
        dialogVerifyButton.setPrefWidth(260);
        dialogVerifyButton.setStyle(
            "-fx-background-color: linear-gradient(to right, #FFD700, #FFA500);" +
            "-fx-text-fill: #0a0a0a;" +
            "-fx-font-size: 14;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.3), 12, 0, 0, 3);"
        );
        dialogVerifyButton.setOnAction(e -> handleVerifyCode());

        // Resend button
        dialogResendButton = new Button("Renvoyer le code");
        dialogResendButton.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: rgba(255,215,0,0.6); -fx-font-size: 12; -fx-cursor: hand; -fx-underline: true;"
        );
        dialogResendButton.setOnAction(e -> handleResendCode());

        // Error label
        dialogErrorLabel = new Label();
        dialogErrorLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12; -fx-font-weight: 600;");
        dialogErrorLabel.setWrapText(true);
        dialogErrorLabel.setAlignment(Pos.CENTER);

        root.getChildren().addAll(
            topBar, icon, title, accentLine, infoLabel,
            codeBox, dialogTimerLabel, dialogVerifyButton, dialogResendButton, dialogErrorLabel
        );

        // Wrap in a StackPane for transparent background overlay
        StackPane overlay = new StackPane(root);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        overlay.setAlignment(Pos.CENTER);

        Scene scene = new Scene(overlay, 500, 480);
        scene.setFill(Color.TRANSPARENT);

        // Allow dragging the dialog
        final double[] dragOffset = new double[2];
        root.setOnMousePressed(e -> {
            dragOffset[0] = e.getSceneX();
            dragOffset[1] = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            verificationDialog.setX(e.getScreenX() - dragOffset[0]);
            verificationDialog.setY(e.getScreenY() - dragOffset[1]);
        });

        verificationDialog.setScene(scene);

        // Center on owner
        verificationDialog.setOnShown(e -> {
            verificationDialog.setX(owner.getX() + (owner.getWidth() - verificationDialog.getWidth()) / 2);
            verificationDialog.setY(owner.getY() + (owner.getHeight() - verificationDialog.getHeight()) / 2);
        });

        // Entrance animation
        root.setOpacity(0);
        root.setScaleX(0.85);
        root.setScaleY(0.85);

        verificationDialog.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), root);
        scaleIn.setFromX(0.85);
        scaleIn.setFromY(0.85);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        new ParallelTransition(fadeIn, scaleIn).play();

        // Start countdown & focus
        startCountdown();
        Platform.runLater(() -> dialogCodeFields[0].requestFocus());
    }

    /**
     * Close verification dialog and re-enable signup form
     */
    private void closeVerificationDialog() {
        if (countdownTimeline != null) countdownTimeline.stop();
        if (verificationDialog != null) verificationDialog.close();
        signupButton.setDisable(false);
        signupButton.setText("CRÉER MON COMPTE  ›");
    }

    /**
     * Setup single-character code fields with auto-advance and backspace navigation
     */
    private void setupDialogCodeFields() {
        for (int i = 0; i < dialogCodeFields.length; i++) {
            final int index = i;
            final TextField field = dialogCodeFields[i];

            field.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    field.setText(newVal.replaceAll("[^\\d]", ""));
                    return;
                }
                if (newVal.length() > 1) {
                    field.setText(newVal.substring(newVal.length() - 1));
                }
                if (newVal.length() == 1 && index < dialogCodeFields.length - 1) {
                    dialogCodeFields[index + 1].requestFocus();
                }
                // Highlight border on filled
                if (!newVal.isEmpty()) {
                    field.setStyle(field.getStyle().replace("rgba(255,215,0,0.2)", "#FFD700"));
                } else {
                    field.setStyle(field.getStyle().replace("#FFD700", "rgba(255,215,0,0.2)"));
                }
            });

            field.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.BACK_SPACE && field.getText().isEmpty() && index > 0) {
                    dialogCodeFields[index - 1].requestFocus();
                    dialogCodeFields[index - 1].clear();
                }
                if (event.getCode() == KeyCode.ENTER) {
                    handleVerifyCode();
                }
            });

            // Focus glow effect
            field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    field.setEffect(new DropShadow(10, Color.rgb(255, 215, 0, 0.4)));
                } else {
                    field.setEffect(null);
                }
            });
        }
    }

    /**
     * Get the 6-digit code from dialog fields
     */
    private String getEnteredCode() {
        StringBuilder sb = new StringBuilder();
        for (TextField f : dialogCodeFields) sb.append(f.getText());
        return sb.toString();
    }

    /**
     * Start 60-second countdown timer
     */
    private void startCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();

        final int[] secondsLeft = {60};
        dialogResendButton.setDisable(true);
        dialogResendButton.setOpacity(0.4);
        dialogTimerLabel.setText("⏱ 60s restantes");

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft[0]--;
            dialogTimerLabel.setText("⏱ " + secondsLeft[0] + "s restantes");
            if (secondsLeft[0] <= 0) {
                countdownTimeline.stop();
                dialogTimerLabel.setText("⏱ Code expiré");
                dialogResendButton.setDisable(false);
                dialogResendButton.setOpacity(1.0);
            }
        }));
        countdownTimeline.setCycleCount(60);
        countdownTimeline.play();
    }

    /**
     * Verify entered OTP code
     */
    private void handleVerifyCode() {
        String code = getEnteredCode();
        if (code.length() != 6) {
            dialogErrorLabel.setText("❌ Entrez les 6 chiffres du code");
            dialogErrorLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12; -fx-font-weight: 600;");
            shake(dialogVerifyButton);
            return;
        }

        dialogVerifyButton.setDisable(true);
        dialogVerifyButton.setText("Vérification...");
        dialogErrorLabel.setText("");

        new Thread(() -> {
            boolean valid = otpService.verifyOTP(pendingEmail, code);

            Platform.runLater(() -> {
                if (valid) {
                    completeRegistration();
                } else {
                    dialogVerifyButton.setDisable(false);
                    dialogVerifyButton.setText("VÉRIFIER  ✓");
                    dialogErrorLabel.setText("❌ Code invalide ou expiré");
                    dialogErrorLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12; -fx-font-weight: 600;");
                    shake(dialogVerifyButton);
                    clearCodeFields();
                }
            });
        }).start();
    }

    /**
     * Complete the user registration after OTP verification
     */
    private void completeRegistration() {
        boolean success = utilisateurService.registerUser(
                pendingFirstName,
                pendingLastName,
                pendingEmail,
                pendingPassword,
                pendingNumTel,
                null,
                pendingRoleId
        );

        if (success) {
            if (countdownTimeline != null) countdownTimeline.stop();
            if (verificationDialog != null) verificationDialog.close();
            showSuccess();
        } else {
            dialogVerifyButton.setDisable(false);
            dialogVerifyButton.setText("VÉRIFIER  ✓");
            dialogErrorLabel.setText("❌ Erreur lors de l'inscription");
        }
    }

    /**
     * Resend OTP code
     */
    private void handleResendCode() {
        dialogResendButton.setDisable(true);
        dialogResendButton.setText("Envoi...");
        dialogErrorLabel.setText("");
        clearCodeFields();

        new Thread(() -> {
            String otp = otpService.generateOTP();
            boolean stored = otpService.storeOTP(pendingEmail, otp);
            boolean sent;
            if ("SMS".equals(pendingOtpMethod)) {
                sent = stored && smsOTPService.sendSmsOTP(String.valueOf(pendingNumTel), otp);
            } else {
                sent = stored && emailService.sendVerificationEmail(pendingEmail, otp);
            }

            Platform.runLater(() -> {
                dialogResendButton.setText("Renvoyer le code");
                if (sent) {
                    dialogErrorLabel.setText("✅ Nouveau code envoyé !");
                    dialogErrorLabel.setStyle("-fx-text-fill: #2ECC71; -fx-font-size: 12; -fx-font-weight: 600;");
                    startCountdown();
                    dialogCodeFields[0].requestFocus();
                } else {
                    dialogErrorLabel.setText("❌ Erreur d'envoi. Réessayez.");
                    dialogErrorLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12; -fx-font-weight: 600;");
                    dialogResendButton.setDisable(false);
                }
            });
        }).start();
    }

    /**
     * Clear all code input fields
     */
    private void clearCodeFields() {
        for (TextField f : dialogCodeFields) f.clear();
        dialogCodeFields[0].requestFocus();
    }

    /**
     * Mask email for display: d***@gmail.com
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private void updateStrength(String password) {
        if (password.isEmpty()) {
            passwordHintLabel.setText("");
            strengthBar.setProgress(0);
            strengthBar.setStyle("");
            return;
        }

        PasswordStrengthService.PasswordAnalysis analysis = passwordStrengthService.analyze(password);

        // Update progress bar
        strengthBar.setProgress(analysis.getProgress());

        // Dynamic color on the progress bar via accent color
        String barColor = analysis.getColor();
        strengthBar.setStyle("-fx-accent: " + barColor + ";");

        // Build hint text: label + crack time + feedback
        StringBuilder hint = new StringBuilder();
        hint.append(analysis.getLabel());
        if (analysis.getCrackTime() != null && !analysis.getCrackTime().isEmpty()) {
            hint.append("  \u23F1 ").append(analysis.getCrackTime());
        }
        if (analysis.getFeedback() != null && !analysis.getFeedback().isEmpty()) {
            hint.append("\n\uD83D\uDCA1 ").append(analysis.getFeedback());
        }

        passwordHintLabel.setText(hint.toString());
        passwordHintLabel.setStyle("-fx-text-fill: " + barColor + "; -fx-font-size: 11;");
    }

    private void validatePasswords() {

        if (confirmPasswordField.getText().isEmpty()) return;

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            confirmPasswordField.setStyle("-fx-border-color: red;");
        } else {
            confirmPasswordField.setStyle("-fx-border-color: #2ECC71;");
        }
    }

    private void validateForm() {

        boolean valid =
                !firstNameField.getText().isEmpty() &&
                        !lastNameField.getText().isEmpty() &&
                        emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$") &&
                        !phoneField.getText().isEmpty() &&
                        (touristeRadio.isSelected() || partenaireRadio.isSelected()) &&
                        passwordField.getText().length() >= 8 &&
                        passwordStrengthService.analyze(passwordField.getText()).getScore() >= 2 &&
                        passwordField.getText().equals(confirmPasswordField.getText()) &&
                        termsCheckBox.isSelected();

        signupButton.setDisable(!valid);
        fadeButton(signupButton, valid);
    }

    private void fadeButton(Button button, boolean enable) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), button);
        ft.setToValue(enable ? 1.0 : 0.6);
        ft.play();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        shake(errorLabel);
    }

    private void showSuccess() {

        errorLabel.setText("🎉 Inscription réussie !");
        ScaleTransition st = new ScaleTransition(Duration.millis(300), signupButton);
        st.setToX(1.1);
        st.setToY(1.1);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();

        new Thread(() -> {
            try {
                Thread.sleep(1200);
                Platform.runLater(this::handleLoginLink);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleMinimize() {
        Stage stage = (Stage) signupButton.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) signupButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void handleLoginLink() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) signupButton.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shake(Node node) {
        Timeline shake = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(node.translateXProperty(), 0)),
            new KeyFrame(Duration.millis(80), new KeyValue(node.translateXProperty(), -10)),
            new KeyFrame(Duration.millis(160), new KeyValue(node.translateXProperty(), 10)),
            new KeyFrame(Duration.millis(240), new KeyValue(node.translateXProperty(), -8)),
            new KeyFrame(Duration.millis(320), new KeyValue(node.translateXProperty(), 8)),
            new KeyFrame(Duration.millis(400), new KeyValue(node.translateXProperty(), -4)),
            new KeyFrame(Duration.millis(480), new KeyValue(node.translateXProperty(), 0))
        );
        shake.play();
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
            newScene.getStylesheets().add(ThemeManager.getInstance().getCssPath());
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
            new ParallelTransition(fadeIn, scaleIn, slideIn).play();
        });
        exitAnim.play();
    }
}