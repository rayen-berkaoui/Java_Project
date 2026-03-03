package com.esprit.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.Node;

import com.esprit.services.EmailService;
import com.esprit.services.OTPService;
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

    // State
    private String currentEmail;
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

        // Apply theme to FXML nodes
        javafx.application.Platform.runLater(() -> {
            if (emailField != null && emailField.getScene() != null && emailField.getScene().getRoot() != null) {
                ThemeManager.applyThemeToFXML((javafx.scene.Parent) emailField.getScene().getRoot());
            }
        });
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
        showStatus(step1StatusLabel, "⏳ Envoi du code en cours...", true);

        // Send OTP in background thread to prevent UI freeze
        new Thread(() -> {
            String otp = otpService.generateOTP();
            boolean stored = otpService.storeOTP(email, otp);
            boolean sent = emailService.sendOtpEmail(email, otp);

            Platform.runLater(() -> {
                if (stored && sent) {
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
            boolean sent = emailService.sendOtpEmail(currentEmail, otp);

            Platform.runLater(() -> {
                if (stored && sent) {
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
        // Mask the email for display
        String masked = maskEmail(currentEmail);
        otpInfoLabel.setText("Un code a été envoyé à " + masked);
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
            ThemeManager.applyTheme(newScene);
            ThemeManager.trackScene(newScene);
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
}
