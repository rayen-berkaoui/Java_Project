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
import javafx.application.Platform;

import com.esprit.services.utilisateurServices;
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
    private boolean passwordVisible = false;
    private double xOffset = 0;
    private double yOffset = 0;

    public signupController() {
        utilisateurService = new utilisateurServices();
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
                // Apply theme to FXML nodes
                ThemeManager.applyThemeToFXML(root);
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
    public void handleSignup() {

        if (signupButton.isDisable()) {
            showError("❌ Formulaire invalide");
            return;
        }

        try {
            int numTel = Integer.parseInt(phoneField.getText());

            int roleId = touristeRadio.isSelected() ? 1 : 2;

            boolean success = utilisateurService.registerUser(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    numTel,
                    null,
                    roleId
            );

            if (success) {
                showSuccess();
            } else {
                showError("❌ Email déjà utilisé");
            }

        } catch (NumberFormatException e) {
            showError("❌ Numéro invalide");
        }
    }

    private void updateStrength(String password) {

        if (password.length() < 8) {
            passwordHintLabel.setText("⚠ Minimum 8 caractères");
            strengthBar.setProgress(0.3);
        } else {
            passwordHintLabel.setText("✅ Mot de passe fort");
            strengthBar.setProgress(1);
        }
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