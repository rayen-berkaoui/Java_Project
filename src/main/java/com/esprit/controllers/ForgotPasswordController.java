package com.esprit.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.scene.Node;

import com.esprit.entities.utilisateur;
import com.esprit.services.EmailService;
import com.esprit.services.OTPService;
import com.esprit.services.SmsOTPService;
import com.esprit.services.utilisateurServices;
import com.esprit.utils.ThemeManager;

public class ForgotPasswordController {

    // Title bar
    @FXML private HBox titleBar;

    // Step 1 - Email
    @FXML private VBox step1Pane;
    @FXML private TextField emailField;
    @FXML private Label step1StatusLabel;

    // Step 2 - OTP
    @FXML private VBox step2Pane;
    @FXML private TextField otpField;
    @FXML private Label otpInfoLabel;
    @FXML private Label timerLabel;
    @FXML private Button verifyButton;
    @FXML private Hyperlink resendLink;
    @FXML private Label step2StatusLabel;

    // Step 3 - New Password
    @FXML private VBox step3Pane;
    @FXML private PasswordField newPasswordField;
    @FXML private TextField visibleNewPasswordField;
    @FXML private Button toggleNewPwdButton;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField visibleConfirmPasswordField;
    @FXML private Button toggleConfirmPwdButton;
    @FXML private Label step3StatusLabel;

    // Services
    private final utilisateurServices userService = new utilisateurServices();
    private final OTPService otpService = new OTPService();
    private final EmailService emailService = new EmailService();
    private final SmsOTPService smsOTPService = new SmsOTPService();

    // State
    private String currentEmail;
    private String currentPhone;
    private String otpMethod = "EMAIL";
    private boolean newPwdVisible = false;
    private boolean confirmPwdVisible = false;
    private Timeline countdownTimeline;
    private int secondsRemaining;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        // Enable window dragging
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
    }

    // ================================================
    // WINDOW CONTROLS
    // ================================================
    @FXML
    public void handleMinimize() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    public void handleClose() {
        stopCountdown();
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }

    // ================================================
    // STEP 1: SEND OTP
    // ================================================
    @FXML
    public void handleSendOTP() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showStatus(step1StatusLabel, "❌ Veuillez entrer votre adresse email", false);
            shakeNode(emailField);
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showStatus(step1StatusLabel, "❌ Format d'email invalide", false);
            shakeNode(emailField);
            return;
        }

        if (!userService.emailExists(email)) {
            showStatus(step1StatusLabel, "❌ Aucun compte associé à cet email", false);
            shakeNode(emailField);
            return;
        }

        currentEmail = email;

        // Look up user to get phone number for SMS option
        utilisateur user = userService.getUserByEmail(email);
        currentPhone = (user != null && user.getNumTel() > 0) ? String.valueOf(user.getNumTel()) : null;

        showOtpMethodChoiceDialog();
    }

    /**
     * Show SMS / Email choice dialog
     */
    private void showOtpMethodChoiceDialog() {
        Stage choiceDialog = new Stage();
        choiceDialog.initStyle(StageStyle.TRANSPARENT);
        choiceDialog.initModality(Modality.APPLICATION_MODAL);
        choiceDialog.initOwner((Stage) emailField.getScene().getWindow());

        VBox container = new VBox(16);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(28, 36, 28, 36));
        container.setMaxWidth(380);
        container.setStyle(
            "-fx-background-color: #111111;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255,215,0,0.25);" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 1.5;"
        );
        container.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.7)));

        Label iconLbl = new Label("\uD83D\uDD10");
        iconLbl.setStyle("-fx-font-size: 38;");

        Label titleLbl = new Label("RÉCUPÉRATION DU COMPTE");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16; -fx-font-weight: bold;");

        Label subtitleLbl = new Label("Comment souhaitez-vous recevoir le code ?");
        subtitleLbl.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12;");

        // Email button
        Button emailBtn = new Button("✉  Envoyer par Email");
        emailBtn.setPrefWidth(260); emailBtn.setPrefHeight(46);
        emailBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #FFD700, #FFA500);" +
            "-fx-text-fill: #0a0a0a; -fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-background-radius: 12; -fx-cursor: hand;"
        );
        Label emailHint = new Label(maskEmail(currentEmail));
        emailHint.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");

        // SMS button
        Button smsBtn = new Button("\uD83D\uDCF1  Envoyer par SMS");
        smsBtn.setPrefWidth(260); smsBtn.setPrefHeight(46);
        boolean smsAvail = smsOTPService.isConfigured() && currentPhone != null && currentPhone.length() >= 8;
        if (smsAvail) {
            smsBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-text-fill: #51CF66; -fx-font-size: 13; -fx-font-weight: bold;" +
                "-fx-background-radius: 12; -fx-cursor: hand;" +
                "-fx-border-color: rgba(81,207,102,0.3); -fx-border-radius: 12;"
            );
        } else {
            smsBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04);" +
                "-fx-text-fill: #555555; -fx-font-size: 13;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(100,100,100,0.2); -fx-border-radius: 12;"
            );
            smsBtn.setDisable(true);
        }
        String maskedPhone = (currentPhone != null && currentPhone.length() >= 4)
            ? "****" + currentPhone.substring(currentPhone.length() - 4)
            : "N/A";
        Label smsHint = new Label(smsAvail ? maskedPhone : "Service SMS non disponible");
        smsHint.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");

        // Cancel
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888888; -fx-font-size: 12; -fx-cursor: hand; -fx-underline: true;");

        container.getChildren().addAll(iconLbl, titleLbl, subtitleLbl, emailBtn, emailHint, smsBtn, smsHint, cancelBtn);

        emailBtn.setOnAction(e -> { otpMethod = "EMAIL"; choiceDialog.close(); sendOtpAndGoToStep2(); });
        smsBtn.setOnAction(e -> { otpMethod = "SMS"; choiceDialog.close(); sendOtpAndGoToStep2(); });
        cancelBtn.setOnAction(e -> choiceDialog.close());

        StackPane overlay = new StackPane(container);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        overlay.setAlignment(Pos.CENTER);

        Scene scene = new Scene(overlay, 450, 420);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) choiceDialog.close(); });
        choiceDialog.setScene(scene);

        container.setOpacity(0); container.setScaleX(0.85); container.setScaleY(0.85);
        choiceDialog.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), container);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), container);
        scaleIn.setFromX(0.85); scaleIn.setFromY(0.85); scaleIn.setToX(1); scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        new ParallelTransition(fadeIn, scaleIn).play();
    }

    /**
     * Send OTP via chosen method and transition to step 2
     */
    private void sendOtpAndGoToStep2() {
        showStatus(step1StatusLabel, "⏳ Envoi du code en cours...", true);

        new Thread(() -> {
            String otp = otpService.generateOTP();
            boolean stored = otpService.storeOTP(currentEmail, otp);
            boolean sent;
            if ("SMS".equals(otpMethod)) {
                sent = stored && smsOTPService.sendSmsOTP(currentPhone, otp);
            } else {
                sent = stored && emailService.sendOtpEmail(currentEmail, otp);
            }

            Platform.runLater(() -> {
                if (sent) {
                    goToStep2();
                } else {
                    showStatus(step1StatusLabel, "❌ Erreur lors de l'envoi du code. Réessayez.", false);
                }
            });
        }).start();
    }

    // ================================================
    // STEP 2: VERIFY OTP
    // ================================================
    @FXML
    public void handleVerifyOTP() {
        String code = otpField.getText().trim();

        if (code.isEmpty()) {
            showStatus(step2StatusLabel, "❌ Veuillez entrer le code", false);
            shakeNode(otpField);
            return;
        }

        if (!code.matches("\\d{6}")) {
            showStatus(step2StatusLabel, "❌ Le code doit contenir 6 chiffres", false);
            shakeNode(otpField);
            return;
        }

        if (otpService.verifyOTP(currentEmail, code)) {
            stopCountdown();
            goToStep3();
        } else {
            showStatus(step2StatusLabel, "❌ Code invalide ou expiré", false);
            shakeNode(otpField);
        }
    }

    @FXML
    public void handleResendOTP() {
        showStatus(step2StatusLabel, "⏳ Renvoi du code...", true);
        resendLink.setDisable(true);

        new Thread(() -> {
            String otp = otpService.generateOTP();
            boolean stored = otpService.storeOTP(currentEmail, otp);
            boolean sent;
            if ("SMS".equals(otpMethod)) {
                sent = stored && smsOTPService.sendSmsOTP(currentPhone, otp);
            } else {
                sent = stored && emailService.sendOtpEmail(currentEmail, otp);
            }

            Platform.runLater(() -> {
                if (sent) {
                    showStatus(step2StatusLabel, "✅ Nouveau code envoyé !", true);
                    otpField.clear();
                    startCountdown();
                    resendLink.setDisable(false);
                } else {
                    showStatus(step2StatusLabel, "❌ Erreur lors du renvoi. Réessayez.", false);
                    resendLink.setDisable(false);
                }
            });
        }).start();
    }

    @FXML
    public void handleBackToStep1() {
        stopCountdown();
        showStep(step1Pane);
    }

    // ================================================
    // STEP 3: RESET PASSWORD
    // ================================================
    @FXML
    public void handleResetPassword() {
        String newPwd = newPwdVisible ? visibleNewPasswordField.getText() : newPasswordField.getText();
        String confirmPwd = confirmPwdVisible ? visibleConfirmPasswordField.getText() : confirmPasswordField.getText();

        if (newPwd.isEmpty() || confirmPwd.isEmpty()) {
            showStatus(step3StatusLabel, "❌ Veuillez remplir tous les champs", false);
            return;
        }

        if (newPwd.length() < 6) {
            showStatus(step3StatusLabel, "❌ Le mot de passe doit contenir au moins 6 caractères", false);
            shakeNode(newPasswordField);
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            showStatus(step3StatusLabel, "❌ Les mots de passe ne correspondent pas", false);
            shakeNode(confirmPasswordField);
            return;
        }

        boolean success = userService.resetPassword(currentEmail, newPwd);

        if (success) {
            showStatus(step3StatusLabel, "✅ Mot de passe réinitialisé avec succès !", true);

            // Redirect to login after 2 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> handleBackToLogin());
            pause.play();
        } else {
            showStatus(step3StatusLabel, "❌ Erreur lors de la réinitialisation", false);
        }
    }

    // ================================================
    // PASSWORD TOGGLES
    // ================================================
    @FXML
    public void handleToggleNewPassword() {
        newPwdVisible = !newPwdVisible;
        if (newPwdVisible) {
            visibleNewPasswordField.setText(newPasswordField.getText());
            visibleNewPasswordField.setVisible(true);
            visibleNewPasswordField.setManaged(true);
            newPasswordField.setVisible(false);
            newPasswordField.setManaged(false);
            toggleNewPwdButton.setText("🔒");
        } else {
            newPasswordField.setText(visibleNewPasswordField.getText());
            newPasswordField.setVisible(true);
            newPasswordField.setManaged(true);
            visibleNewPasswordField.setVisible(false);
            visibleNewPasswordField.setManaged(false);
            toggleNewPwdButton.setText("👁");
        }
    }

    @FXML
    public void handleToggleConfirmPassword() {
        confirmPwdVisible = !confirmPwdVisible;
        if (confirmPwdVisible) {
            visibleConfirmPasswordField.setText(confirmPasswordField.getText());
            visibleConfirmPasswordField.setVisible(true);
            visibleConfirmPasswordField.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            toggleConfirmPwdButton.setText("🔒");
        } else {
            confirmPasswordField.setText(visibleConfirmPasswordField.getText());
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            visibleConfirmPasswordField.setVisible(false);
            visibleConfirmPasswordField.setManaged(false);
            toggleConfirmPwdButton.setText("👁");
        }
    }

    // ================================================
    // NAVIGATION
    // ================================================
    @FXML
    public void handleBackToLogin() {
        stopCountdown();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Connexion");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================================================
    // STEP MANAGEMENT
    // ================================================
    private void goToStep2() {
        // Show appropriate message based on OTP method
        if ("SMS".equals(otpMethod) && currentPhone != null && currentPhone.length() >= 4) {
            String maskedPhone = "****" + currentPhone.substring(currentPhone.length() - 4);
            otpInfoLabel.setText("Un code a été envoyé au " + maskedPhone);
        } else {
            String masked = maskEmail(currentEmail);
            otpInfoLabel.setText("Un code a été envoyé à " + masked);
        }
        otpField.clear();
        step2StatusLabel.setText("");
        showStep(step2Pane);
        startCountdown();
    }

    private void goToStep3() {
        step3StatusLabel.setText("");
        newPasswordField.clear();
        confirmPasswordField.clear();
        visibleNewPasswordField.clear();
        visibleConfirmPasswordField.clear();
        showStep(step3Pane);
    }

    private void showStep(VBox activeStep) {
        VBox[] steps = {step1Pane, step2Pane, step3Pane};
        for (VBox step : steps) {
            boolean isActive = step == activeStep;
            step.setVisible(isActive);
            step.setManaged(isActive);
        }

        // Slide + fade in the active step
        activeStep.setOpacity(0);
        activeStep.setTranslateX(30);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), activeStep);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(350), activeStep);
        slideIn.setFromX(30);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

        new ParallelTransition(fadeIn, slideIn).play();
    }

    // ================================================
    // COUNTDOWN TIMER (60 seconds)
    // ================================================
    private void startCountdown() {
        stopCountdown();
        secondsRemaining = 60;
        verifyButton.setDisable(false);
        updateTimerLabel();

        countdownTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                secondsRemaining--;
                updateTimerLabel();

                if (secondsRemaining <= 0) {
                    stopCountdown();
                    timerLabel.setText("Code expiré !");
                    timerLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 14px; -fx-font-weight: bold;");
                    verifyButton.setDisable(true);
                }
            })
        );
        countdownTimeline.setCycleCount(60);
        countdownTimeline.play();
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void updateTimerLabel() {
        if (timerLabel != null) {
            timerLabel.setText(secondsRemaining + "s restantes");
            if (secondsRemaining <= 10) {
                timerLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 14px; -fx-font-weight: bold;");
            } else {
                timerLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14px; -fx-font-weight: bold;");
            }
        }
    }

    // ================================================
    // UTILITIES
    // ================================================
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        String prefix = email.substring(0, 2);
        String domain = email.substring(atIndex);
        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 2; i < atIndex; i++) masked.append('*');
        masked.append(domain);
        return masked.toString();
    }

    private void showStatus(Label label, String msg, boolean isSuccess) {
        label.setText(msg);
        label.setStyle(isSuccess
                ? "-fx-text-fill: #51CF66; -fx-font-size: 13px;"
                : "-fx-text-fill: #FF6B6B; -fx-font-size: 13px;");
    }

    private void shakeNode(Node node) {
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
