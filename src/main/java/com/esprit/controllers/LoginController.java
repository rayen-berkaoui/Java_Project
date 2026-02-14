package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.Node;

import com.esprit.entities.utilisateur;
import com.esprit.services.utilisateurServices;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button togglePasswordButton;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.layout.HBox titleBar;

    private utilisateurServices utilisateurService;
    private boolean passwordVisible = false;
    private double xOffset = 0;
    private double yOffset = 0;

    public LoginController() {
        utilisateurService = new utilisateurServices();
    }

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
    }

    @FXML
    public void handleMinimize() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }

    // =========================
    // LOGIN
    // =========================
    @FXML
    public void handleLogin() {

        String email = emailField.getText();
        String password = passwordVisible ? visiblePasswordField.getText() : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("❌ Champs obligatoires");
            glowError(emailField, passwordField);
            return;
        }

        // Check if user is blocked FIRST
        if (utilisateurService.isUserBlocked(email, password)) {
            showError("🚫 Votre compte est bloqué. Contactez l'administrateur.");
            glowError(emailField);
            return;
        }

        utilisateur user = utilisateurService.loginUser(email, password);

        if (user != null) {

            errorLabel.setText("✅ Bienvenue " + user.getNom());
            errorLabel.setStyle("-fx-text-fill: #51CF66;");

            System.out.println("Utilisateur connecté : " + user);

            // ✅ Redirect based on role
            redirectToDashboard(user);

        } else {
            showError("❌ Identifiants incorrects");
            glowError(emailField, passwordField);
        }
    }

    // =========================
    // PASSWORD TOGGLE
    // =========================
    @FXML
    public void handleTogglePassword() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordButton.setText("🔒");
        } else {
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            togglePasswordButton.setText("👁");
        }
    }

    // =========================
    // FORGOT PASSWORD
    // =========================
    @FXML
    public void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forgotpassword.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Mot de passe oublié");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // SIGNUP PAGE
    // =========================
    @FXML
    public void handleSignupLink() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/signup.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            fadeTransition(stage, root, "Tabaani - Signup");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // DASHBOARD REDIRECT (ROLE-BASED)
    // =========================
    private void redirectToDashboard(utilisateur user) {

        try {
            boolean isAdmin = utilisateurService.isAdmin(user.getRoleId());

            if (isAdmin) {
                // ✅ Admin → Admin Dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
                Parent root = loader.load();

                DashboardController controller = loader.getController();
                controller.setUserName(user.getNom());

                Stage stage = (Stage) emailField.getScene().getWindow();
                fadeTransition(stage, root, "Tabaani - Dashboard");
            } else {
                // ✅ Normal user → User Profile
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/userprofile.fxml"));
                Parent root = loader.load();

                UserProfileController controller = loader.getController();
                controller.setUser(user);

                Stage stage = (Stage) emailField.getScene().getWindow();
                fadeTransition(stage, root, "Tabaani - Mon Profil");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("❌ Impossible d'ouvrir le dashboard");
        }
    }

    // =========================
    // ERROR DISPLAY
    // =========================
    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill: #FF6B6B;");
        shakeNode(errorLabel);
    }

    // =========================
    // SHAKE ANIMATION
    // =========================
    private void shakeNode(Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    // =========================
    // RED GLOW EFFECT
    // =========================
    private void glowError(Control... fields) {
        for (Control field : fields) {

            field.setStyle("-fx-border-color: red; -fx-effect: dropshadow(gaussian, red, 10, 0.5, 0, 0);");

            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> field.setStyle(null));
            pause.play();
        }
    }

    // =========================
    // FADE TRANSITION
    // =========================
    private void fadeTransition(Stage stage, Parent newRoot, String title) {

        Scene oldScene = stage.getScene();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), oldScene.getRoot());
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        fadeOut.setOnFinished(e -> {

            Scene newScene = new Scene(newRoot);

            newScene.getStylesheets().add(
                    getClass().getResource("/style.css").toExternalForm()
            );

            stage.setScene(newScene);
            stage.setTitle(title);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), newRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });

        fadeOut.play();
    }
}