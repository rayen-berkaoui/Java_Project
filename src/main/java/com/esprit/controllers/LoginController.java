package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.application.Platform;

import com.esprit.entities.utilisateur;
import com.esprit.services.utilisateurServices;
import com.esprit.services.FaceRecognitionService;

import org.bytedeco.opencv.opencv_core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button togglePasswordButton;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.layout.HBox titleBar;
    @FXML private CheckBox rememberMeCheckBox;

    // ═══════ FACE RECOGNITION FXML ═══════
    @FXML private ImageView webcamView;
    @FXML private Circle outerRing;
    @FXML private Circle middleRing;
    @FXML private Circle innerCircle;
    @FXML private Circle webcamBorder;
    @FXML private Circle faceIndicator;
    @FXML private Label lockIcon;
    @FXML private Label faceStatusLabel;
    @FXML private Button btnStartCamera;
    @FXML private Button btnFaceLogin;
    @FXML private Button btnRegisterFace;

    private utilisateurServices utilisateurService;
    private FaceRecognitionService faceService;
    private ScheduledExecutorService cameraTimer;
    private boolean passwordVisible = false;
    private boolean faceCurrentlyDetected = false;
    private double xOffset = 0;
    private double yOffset = 0;

    private static final String PREF_REMEMBER = "remember_me";
    private static final String PREF_EMAIL = "saved_email";
    private static final String PREF_PASSWORD = "saved_password";
    private final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    public LoginController() {
        utilisateurService = new utilisateurServices();
        faceService = new FaceRecognitionService();
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

        // Load saved credentials if "Remember Me" was checked
        loadSavedCredentials();

        // Entrance animation for the form
        playEntranceAnimation();

        // Make sure camera stops when window closes
        Platform.runLater(() -> {
            if (emailField != null && emailField.getScene() != null) {
                emailField.getScene().getWindow().setOnHiding(e -> stopCameraCleanup());
            }
        });
    }

    /**
     * Animate the UI elements on scene load with a staggered fade+slide effect
     */
    private void playEntranceAnimation() {
        if (emailField == null) return;

        // Get the root and animate children with staggered delays
        javafx.application.Platform.runLater(() -> {
            try {
                Parent root = emailField.getScene().getRoot();
                root.setOpacity(0);
                root.setTranslateY(12);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                TranslateTransition slideUp = new TranslateTransition(Duration.millis(500), root);
                slideUp.setFromY(12);
                slideUp.setToY(0);
                slideUp.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

                ParallelTransition entrance = new ParallelTransition(fadeIn, slideUp);
                entrance.play();
            } catch (Exception ignored) {}
        });
    }

    @FXML
    public void handleMinimize() {
        stopCameraCleanup();
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    public void handleClose() {
        stopCameraCleanup();
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }

    // =========================
    // FACE RECOGNITION
    // =========================

    /**
     * Start the webcam and show live feed in the circle area
     */
    @FXML
    public void handleStartCamera() {
        try {
            if (faceService.isCameraRunning()) {
                // Stop camera
                stopCameraCleanup();
                setCameraUIState(false);
                if (faceStatusLabel != null) faceStatusLabel.setText("");
                return;
            }

            faceService.startCamera();
            setCameraUIState(true);

            if (faceStatusLabel != null) {
                faceStatusLabel.setText("Camera active - Positionnez votre visage");
                faceStatusLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11;");
            }

            // Start frame grabbing loop
            cameraTimer = Executors.newSingleThreadScheduledExecutor();
            cameraTimer.scheduleAtFixedRate(() -> {
                if (!faceService.isCameraRunning()) return;

                FaceRecognitionService.CameraResult result = faceService.grabFrameWithDetection();
                if (result != null) {
                    Platform.runLater(() -> {
                        webcamView.setImage(result.getImage());

                        if (result.isFaceDetected() != faceCurrentlyDetected) {
                            faceCurrentlyDetected = result.isFaceDetected();
                            updateFaceIndicator(faceCurrentlyDetected);
                        }
                    });
                }
            }, 0, 33, TimeUnit.MILLISECONDS); // ~30 FPS

        } catch (Exception e) {
            e.printStackTrace();
            if (faceStatusLabel != null) {
                faceStatusLabel.setText("Erreur: Camera non disponible");
                faceStatusLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");
            }
        }
    }

    /**
     * Attempt face login: capture face, compare with all stored face encodings
     */
    @FXML
    public void handleFaceLogin() {
        if (!faceService.isCameraRunning()) {
            showError("Activez la camera d'abord");
            return;
        }

        if (!faceCurrentlyDetected) {
            if (faceStatusLabel != null) {
                faceStatusLabel.setText("Aucun visage detecte. Positionnez-vous face a la camera.");
                faceStatusLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");
            }
            return;
        }

        if (faceStatusLabel != null) {
            faceStatusLabel.setText("Analyse du visage en cours...");
            faceStatusLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11;");
        }

        // Run face matching in background
        new Thread(() -> {
            try {
                // Take MULTIPLE captures for reliable cross-session matching
                // Each capture gets its own independent preprocessing
                List<String> capturedEncodings = new ArrayList<>();
                for (int attempt = 0; attempt < 10 && capturedEncodings.size() < 3; attempt++) {
                    Mat currentFrame = faceService.grabMat();
                    if (currentFrame == null) continue;

                    String enc = faceService.encodeFace(currentFrame);
                    if (enc != null) {
                        capturedEncodings.add(enc);
                        System.out.println("Face captured on attempt " + (attempt + 1)
                            + " (" + capturedEncodings.size() + "/3)");
                    }
                    // Delay between attempts for variation
                    Thread.sleep(200);
                }

                if (capturedEncodings.isEmpty()) {
                    Platform.runLater(() -> {
                        faceStatusLabel.setText("Visage non detecte dans la capture. Reessayez.");
                        faceStatusLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");
                    });
                    return;
                }

                // Try each captured encoding against all users, take best match
                utilisateur bestMatchUser = null;
                double overallBestScore = 0.0;
                for (String capturedEncoding : capturedEncodings) {
                    utilisateur candidate = utilisateurService.findUserByFace(capturedEncoding, faceService);
                    if (candidate != null) {
                        double score = faceService.compareFaces(candidate.getFaceEncoding(), capturedEncoding);
                        if (score > overallBestScore) {
                            overallBestScore = score;
                            bestMatchUser = candidate;
                        }
                    }
                }
                System.out.println("Face login: best overall score = " + overallBestScore
                    + " from " + capturedEncodings.size() + " captures");

                // Copy to effectively final variable for use inside lambda
                final utilisateur matchedUser = bestMatchUser;

                Platform.runLater(() -> {
                    if (matchedUser != null) {
                        // SUCCESS — face matched
                        faceStatusLabel.setText("Visage reconnu: " + matchedUser.getNom() + " " + matchedUser.getPrenom());
                        faceStatusLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11; -fx-font-weight: bold;");

                        errorLabel.setText("Bienvenue " + matchedUser.getNom() + " !");
                        errorLabel.setStyle("-fx-text-fill: #51CF66;");

                        // Pulse the face indicator green
                        if (faceIndicator != null) {
                            faceIndicator.setStyle("-fx-fill: transparent; -fx-stroke: rgba(81,207,102,0.9); -fx-stroke-width: 3;");
                        }

                        // Stop camera and redirect
                        PauseTransition delay = new PauseTransition(Duration.millis(1200));
                        delay.setOnFinished(e -> {
                            stopCameraCleanup();
                            redirectToDashboard(matchedUser);
                        });
                        delay.play();

                    } else {
                        // FAIL — no match
                        faceStatusLabel.setText("Visage non reconnu. Essayez a nouveau ou connectez-vous par email.");
                        faceStatusLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");

                        if (faceIndicator != null) {
                            faceIndicator.setStyle("-fx-fill: transparent; -fx-stroke: rgba(255,107,107,0.6); -fx-stroke-width: 2;");
                            PauseTransition reset = new PauseTransition(Duration.seconds(2));
                            reset.setOnFinished(e ->
                                faceIndicator.setStyle("-fx-fill: transparent; -fx-stroke: rgba(81,207,102,0.6); -fx-stroke-width: 2;")
                            );
                            reset.play();
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Erreur reconnaissance faciale: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Register the current user's face (must be logged in via email first)
     * This captures the face and stores the encoding in the database
     */
    @FXML
    public void handleRegisterFace() {
        if (!faceService.isCameraRunning()) {
            showError("Activez la camera d'abord");
            return;
        }

        if (!faceCurrentlyDetected) {
            if (faceStatusLabel != null) {
                faceStatusLabel.setText("Aucun visage detecte. Regardez la camera.");
                faceStatusLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");
            }
            return;
        }

        // Need email to know which user to register face for
        String email = emailField.getText().trim();
        String password = passwordVisible ? visiblePasswordField.getText() : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Entrez vos identifiants avant d'enregistrer votre visage");
            return;
        }

        // Verify credentials first
        utilisateur user = utilisateurService.loginUser(email, password);
        if (user == null) {
            showError("Identifiants incorrects. Impossible d'enregistrer le visage.");
            return;
        }

        if (faceStatusLabel != null) {
            faceStatusLabel.setText("Capture du visage en cours... Regardez la camera et ne bougez pas.");
            faceStatusLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11;");
        }

        new Thread(() -> {
            try {
                // Use multi-capture for more robust registration (5 samples)
                Platform.runLater(() -> {
                    faceStatusLabel.setText("Capture en cours (5 echantillons)... Restez immobile.");
                    faceStatusLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11;");
                });

                String faceEncoding = faceService.encodeMultipleFaces();
                if (faceEncoding == null) {
                    Platform.runLater(() -> {
                        faceStatusLabel.setText("Impossible d'encoder le visage. Reessayez en regardant la camera.");
                        faceStatusLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");
                    });
                    return;
                }

                // Save face encoding to database
                boolean saved = utilisateurService.saveFaceEncoding(user.getId(), faceEncoding);

                Platform.runLater(() -> {
                    if (saved) {
                        faceStatusLabel.setText("Visage enregistre avec succes ! Vous pouvez maintenant vous connecter par reconnaissance faciale.");
                        faceStatusLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11; -fx-font-weight: bold;");

                        errorLabel.setText("Visage enregistre !");
                        errorLabel.setStyle("-fx-text-fill: #51CF66;");
                    } else {
                        faceStatusLabel.setText("Erreur lors de l'enregistrement du visage.");
                        faceStatusLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11;");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Erreur: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Update camera UI visibility state
     */
    private void setCameraUIState(boolean cameraOn) {
        if (webcamView != null) webcamView.setVisible(cameraOn);
        if (webcamBorder != null) webcamBorder.setVisible(cameraOn);
        if (lockIcon != null) lockIcon.setVisible(!cameraOn);
        if (outerRing != null) outerRing.setVisible(!cameraOn);
        if (middleRing != null) middleRing.setVisible(!cameraOn);
        if (innerCircle != null) innerCircle.setVisible(!cameraOn);

        if (btnStartCamera != null) {
            btnStartCamera.setText(cameraOn ? "\uD83D\uDEB7 Desactiver Camera" : "\uD83D\uDCF7 Activer Camera");
        }

        if (btnFaceLogin != null) {
            btnFaceLogin.setVisible(cameraOn);
            btnFaceLogin.setManaged(cameraOn);
        }

        if (btnRegisterFace != null) {
            btnRegisterFace.setVisible(cameraOn);
            btnRegisterFace.setManaged(cameraOn);
        }

        if (faceIndicator != null) faceIndicator.setVisible(cameraOn);
    }

    /**
     * Update the face detection indicator ring
     */
    private void updateFaceIndicator(boolean detected) {
        if (faceIndicator == null) return;

        if (detected) {
            faceIndicator.setStyle("-fx-fill: transparent; -fx-stroke: rgba(81,207,102,0.6); -fx-stroke-width: 2;");
            if (faceStatusLabel != null) {
                faceStatusLabel.setText("Visage detecte - Pret pour la connexion");
                faceStatusLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11;");
            }
        } else {
            faceIndicator.setStyle("-fx-fill: transparent; -fx-stroke: rgba(255,215,0,0.3); -fx-stroke-width: 1;");
            if (faceStatusLabel != null) {
                faceStatusLabel.setText("Positionnez votre visage dans le cercle");
                faceStatusLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");
            }
        }
    }

    /**
     * Clean up camera resources
     */
    private void stopCameraCleanup() {
        if (cameraTimer != null && !cameraTimer.isShutdown()) {
            cameraTimer.shutdown();
            try {
                cameraTimer.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {}
        }
        faceService.stopCamera();
        faceCurrentlyDetected = false;

        Platform.runLater(() -> setCameraUIState(false));
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

            // Save or clear "Remember Me" credentials
            saveCredentials(email, password);

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
        stopCameraCleanup();
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
        stopCameraCleanup();
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
        stopCameraCleanup();
        try {
            boolean isAdmin = utilisateurService.isAdmin(user.getRoleId());

            if (isAdmin) {
                // ✅ Admin → Admin Dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
                Parent root = loader.load();

                DashboardController controller = loader.getController();
                controller.setUserName(user.getNom());
                controller.setAdminId(user.getId());

                Stage stage = (Stage) emailField.getScene().getWindow();
                fadeTransition(stage, root, "Tabaani - Dashboard");
            } else {
                // ✅ Normal user → Main Interface (SmartTravel Hub)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/maininterface.fxml"));
                Parent root = loader.load();

                MainInterfaceController controller = loader.getController();
                controller.setUser(user);

                Stage stage = (Stage) emailField.getScene().getWindow();
                fadeTransition(stage, root, "SmartTravel - Accueil");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("=== REDIRECT ERROR ===");
            System.err.println("Message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
                e.getCause().printStackTrace();
            }
            showError("❌ Impossible d'ouvrir le dashboard: " + e.getMessage());
        }
    }

    // =========================
    // REMEMBER ME
    // =========================

    /**
     * Load saved credentials from preferences if "Remember Me" was previously checked.
     */
    private void loadSavedCredentials() {
        boolean remembered = prefs.getBoolean(PREF_REMEMBER, false);
        if (remembered) {
            String savedEmail = prefs.get(PREF_EMAIL, "");
            String savedPassword = prefs.get(PREF_PASSWORD, "");

            if (!savedEmail.isEmpty()) {
                emailField.setText(savedEmail);
            }
            if (!savedPassword.isEmpty()) {
                passwordField.setText(savedPassword);
            }
            if (rememberMeCheckBox != null) {
                rememberMeCheckBox.setSelected(true);
            }
        }
    }

    /**
     * Save or clear credentials based on "Remember Me" checkbox state.
     */
    private void saveCredentials(String email, String password) {
        if (rememberMeCheckBox != null && rememberMeCheckBox.isSelected()) {
            prefs.putBoolean(PREF_REMEMBER, true);
            prefs.put(PREF_EMAIL, email);
            prefs.put(PREF_PASSWORD, password);
        } else {
            prefs.putBoolean(PREF_REMEMBER, false);
            prefs.remove(PREF_EMAIL);
            prefs.remove(PREF_PASSWORD);
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
    // SHAKE ANIMATION (enhanced)
    // =========================
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

    // =========================
    // RED GLOW EFFECT (enhanced)
    // =========================
    private void glowError(Control... fields) {
        for (Control field : fields) {
            field.setStyle("-fx-border-color: #FF6B6B; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, #FF6B6B, 15, 0.4, 0, 0);");

            // Subtle scale pulse
            ScaleTransition pulse = new ScaleTransition(Duration.millis(150), field);
            pulse.setToX(1.02);
            pulse.setToY(1.02);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(2);
            pulse.play();

            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> {
                field.setStyle(null);
                field.setScaleX(1);
                field.setScaleY(1);
            });
            pause.play();
        }
    }

    // =========================
    // SCENE TRANSITION (slide + fade + scale)
    // =========================
    private void fadeTransition(Stage stage, Parent newRoot, String title) {
        Scene oldScene = stage.getScene();
        Node oldRoot = oldScene.getRoot();

        // Fade out + slight scale down + blur
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
                getClass().getResource("/style.css").toExternalForm()
            );
            newScene.setFill(javafx.scene.paint.Color.BLACK);

            // Prepare entrance state
            newRoot.setOpacity(0);
            newRoot.setScaleX(1.03);
            newRoot.setScaleY(1.03);
            newRoot.setTranslateY(8);

            stage.setScene(newScene);
            stage.sizeToScene();
            stage.setTitle(title);

            // Fade in + scale to normal + slide up
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