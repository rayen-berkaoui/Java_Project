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
import com.esprit.services.TOTPService;
import com.esprit.services.SmsOTPService;
import com.esprit.services.OTPService;
import com.esprit.services.EmailService;
import com.esprit.utils.ThemeManager;
import com.esprit.services.TranslationService;

import org.bytedeco.opencv.opencv_core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

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

    // ═══════ BIOMETRIC INFO PANEL ═══════
    @FXML private VBox biometricInfoPanel;
    @FXML private ProgressBar faceQualityBar;
    @FXML private Label faceQualityLabel;
    @FXML private ProgressBar confidenceBar;
    @FXML private Label confidenceLabel;
    @FXML private Label faceSizeLabel;
    @FXML private HBox captureProgressBox;
    @FXML private ProgressBar captureProgressBar;
    @FXML private Label captureProgressLabel;

    private utilisateurServices utilisateurService;
    private FaceRecognitionService faceService;
    private TOTPService totpService;
    private SmsOTPService smsOTPService;
    private OTPService otpService;
    private EmailService emailService;
    private ScheduledExecutorService cameraTimer;
    private boolean passwordVisible = false;
    private boolean faceCurrentlyDetected = false;
    private int frameCounter = 0;
    private double cachedQuality = 0.0;
    private double xOffset = 0;
    private double yOffset = 0;

    private static final String PREF_REMEMBER = "remember_me";
    private static final String PREF_EMAIL = "saved_email";
    private static final String PREF_PASSWORD = "saved_password";
    private final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    public LoginController() {
        utilisateurService = new utilisateurServices();
        faceService = new FaceRecognitionService();
        totpService = new TOTPService();
        smsOTPService = new SmsOTPService();
        otpService = new OTPService();
        emailService = new EmailService();
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
                if (emailField.getScene().getWindow() != null) {
                    emailField.getScene().getWindow().setOnHiding(e -> stopCameraCleanup());
                } else {
                    emailField.getScene().windowProperty().addListener((obs, oldW, newW) -> {
                        if (newW != null) {
                            newW.setOnHiding(e -> stopCameraCleanup());
                        }
                    });
                }
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
                    frameCounter++;
                    // Only assess quality every 15 frames (~1 second) to avoid lag
                    final boolean doQualityCheck = result.isFaceDetected() && (frameCounter % 15 == 0);
                    final double quality;
                    if (doQualityCheck && result.getOriginalMat() != null) {
                        quality = faceService.assessFaceQuality(result.getOriginalMat());
                        cachedQuality = quality;
                    } else {
                        quality = cachedQuality;
                    }
                    final int fw = result.getFaceWidth();
                    final int fh = result.getFaceHeight();
                    final boolean faceDetected = result.isFaceDetected();

                    Platform.runLater(() -> {
                        webcamView.setImage(result.getImage());

                        if (faceDetected != faceCurrentlyDetected) {
                            faceCurrentlyDetected = faceDetected;
                            updateFaceIndicator(faceCurrentlyDetected);
                        }

                        // Update biometric info panel
                        if (faceDetected) {
                            if (faceQualityBar != null) faceQualityBar.setProgress(quality);
                            if (faceQualityLabel != null) {
                                faceQualityLabel.setText(String.format("%.0f%%", quality * 100));
                                if (quality >= 0.7) {
                                    faceQualityLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11; -fx-font-weight: bold;");
                                } else if (quality >= 0.4) {
                                    faceQualityLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11; -fx-font-weight: bold;");
                                } else {
                                    faceQualityLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11; -fx-font-weight: bold;");
                                }
                            }
                            if (faceSizeLabel != null) {
                                String sizeInfo = fw + "x" + fh + "px";
                                if (fw < 80) {
                                    faceSizeLabel.setText(sizeInfo + " — Rapprochez-vous");
                                    faceSizeLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 10;");
                                } else if (fw < 150) {
                                    faceSizeLabel.setText(sizeInfo + " — Bonne distance");
                                    faceSizeLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 10;");
                                } else {
                                    faceSizeLabel.setText(sizeInfo + " — Distance ideale");
                                    faceSizeLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 10;");
                                }
                            }
                        } else {
                            if (faceQualityBar != null) faceQualityBar.setProgress(0);
                            if (faceQualityLabel != null) {
                                faceQualityLabel.setText("--");
                                faceQualityLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11; -fx-font-weight: bold;");
                            }
                            if (faceSizeLabel != null) {
                                faceSizeLabel.setText("");
                            }
                        }
                    });
                }
            }, 0, 66, TimeUnit.MILLISECONDS); // ~15 FPS for smooth performance

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
                for (int attempt = 0; attempt < 15 && capturedEncodings.size() < 5; attempt++) {
                    Mat currentFrame = faceService.grabMat();
                    if (currentFrame == null) continue;

                    // Only use good quality captures for matching
                    double quality = faceService.assessFaceQuality(currentFrame);
                    if (quality < 0.4) {
                        System.out.println("Skipping low quality capture for login (quality=" + String.format("%.2f", quality) + ")");
                        continue;
                    }

                    String enc = faceService.encodeFace(currentFrame);
                    if (enc != null) {
                        capturedEncodings.add(enc);
                        System.out.println("Face captured on attempt " + (attempt + 1)
                            + " (" + capturedEncodings.size() + "/5, quality=" + String.format("%.2f", quality) + ")");

                        // Update confidence bar during capture
                        final int count = capturedEncodings.size();
                        Platform.runLater(() -> {
                            if (confidenceBar != null) confidenceBar.setProgress(count / 5.0 * 0.3);
                            if (confidenceLabel != null) confidenceLabel.setText("Capture " + count + "/5...");
                        });
                    }
                    // Delay between attempts for variation
                    Thread.sleep(150);
                }

                if (capturedEncodings.isEmpty()) {
                    Platform.runLater(() -> {
                        faceStatusLabel.setText("Visage non detecte dans la capture. Reessayez.");
                        faceStatusLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12;");
                        if (confidenceBar != null) confidenceBar.setProgress(0);
                        if (confidenceLabel != null) { confidenceLabel.setText("--"); confidenceLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11; -fx-font-weight: bold;"); }
                    });
                    return;
                }

                Platform.runLater(() -> {
                    faceStatusLabel.setText("Comparaison biometrique en cours...");
                    faceStatusLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12;");
                });

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
                final double finalScore = overallBestScore;

                Platform.runLater(() -> {
                    // Update confidence display
                    if (confidenceBar != null) confidenceBar.setProgress(finalScore);
                    if (confidenceLabel != null) {
                        confidenceLabel.setText(String.format("%.1f%%", finalScore * 100));
                        if (finalScore >= 0.75) {
                            confidenceLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11; -fx-font-weight: bold;");
                        } else {
                            confidenceLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11; -fx-font-weight: bold;");
                        }
                    }

                    if (matchedUser != null) {
                        // SUCCESS — face matched
                        faceStatusLabel.setText("Visage reconnu: " + matchedUser.getNom() + " " + matchedUser.getPrenom()
                            + " (confiance: " + String.format("%.1f%%", finalScore * 100) + ")");
                        faceStatusLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 12; -fx-font-weight: bold;");

                        errorLabel.setText("Bienvenue " + matchedUser.getNom() + " !");
                        errorLabel.setStyle("-fx-text-fill: #51CF66;");

                        // Pulse the face indicator green
                        if (faceIndicator != null) {
                            faceIndicator.setStyle("-fx-fill: transparent; -fx-stroke: rgba(81,207,102,0.9); -fx-stroke-width: 3;");
                        }

                        // Update last face login in database
                        utilisateurService.updateLastFaceLogin(matchedUser.getId(), finalScore);

                        // Stop camera and check 2FA before redirect
                        PauseTransition delay = new PauseTransition(Duration.millis(1200));
                        delay.setOnFinished(e -> {
                            stopCameraCleanup();
                            if (totpService.is2FAEnabled(matchedUser.getId())) {
                                show2FAVerificationDialog(matchedUser);
                            } else {
                                redirectToDashboard(matchedUser);
                            }
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
                // Use multi-capture for more robust registration (8 samples) with progress
                Platform.runLater(() -> {
                    faceStatusLabel.setText("Capture en cours (8 echantillons)... Restez immobile et regardez la camera.");
                    faceStatusLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12;");
                    if (captureProgressBox != null) {
                        captureProgressBox.setVisible(true);
                        captureProgressBox.setManaged(true);
                    }
                    if (captureProgressBar != null) captureProgressBar.setProgress(0);
                    if (captureProgressLabel != null) captureProgressLabel.setText("0/8");
                });

                String faceEncoding = faceService.encodeMultipleFaces((captured, total, quality, qualityMsg) -> {
                    Platform.runLater(() -> {
                        if (captureProgressBar != null) captureProgressBar.setProgress((double) captured / total);
                        if (captureProgressLabel != null) captureProgressLabel.setText(captured + "/" + total);
                        if (faceQualityLabel != null) faceQualityLabel.setText(String.format("%.0f%%", quality * 100));
                        if (faceQualityBar != null) faceQualityBar.setProgress(quality);
                        faceStatusLabel.setText("Echantillon " + captured + "/" + total + " — " + qualityMsg);
                    });
                });

                if (faceEncoding == null) {
                    Platform.runLater(() -> {
                        faceStatusLabel.setText("Impossible d'encoder le visage. Ameliorez l'eclairage et reessayez.");
                        faceStatusLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12;");
                        if (captureProgressBox != null) { captureProgressBox.setVisible(false); captureProgressBox.setManaged(false); }
                    });
                    return;
                }

                // Save face encoding with confidence to database
                boolean saved = utilisateurService.saveFaceEncodingWithConfidence(user.getId(), faceEncoding, 1.0);

                Platform.runLater(() -> {
                    if (captureProgressBox != null) { captureProgressBox.setVisible(false); captureProgressBox.setManaged(false); }

                    if (saved) {
                        int samples = faceEncoding.split("\\|\\|\\|").length;
                        faceStatusLabel.setText("Visage enregistre avec succes ! " + samples + " echantillons biometriques stockes.\n"
                            + "Vous pouvez maintenant vous connecter par reconnaissance faciale.");
                        faceStatusLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 12; -fx-font-weight: bold;");

                        errorLabel.setText("Visage enregistre !");
                        errorLabel.setStyle("-fx-text-fill: #51CF66;");

                        if (confidenceBar != null) confidenceBar.setProgress(1.0);
                        if (confidenceLabel != null) { confidenceLabel.setText("100%"); confidenceLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11; -fx-font-weight: bold;"); }
                    } else {
                        faceStatusLabel.setText("Erreur lors de l'enregistrement du visage en base de donnees.");
                        faceStatusLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12;");
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
            faceIndicator.setStyle("-fx-fill: transparent; -fx-stroke: rgba(81,207,102,0.6); -fx-stroke-width: 2.5;");
            if (faceStatusLabel != null) {
                faceStatusLabel.setText("Visage detecte — Pret pour la connexion biometrique");
                faceStatusLabel.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 12;");
            }
        } else {
            faceIndicator.setStyle("-fx-fill: transparent; -fx-stroke: rgba(255,215,0,0.3); -fx-stroke-width: 1.5;");
            if (faceStatusLabel != null) {
                faceStatusLabel.setText("Positionnez votre visage dans le cercle");
                faceStatusLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12;");
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

        Platform.runLater(() -> {
            setCameraUIState(false);
            // Reset biometric displays
            if (faceQualityBar != null) faceQualityBar.setProgress(0);
            if (faceQualityLabel != null) { faceQualityLabel.setText("--"); faceQualityLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11; -fx-font-weight: bold;"); }
            if (confidenceBar != null) confidenceBar.setProgress(0);
            if (confidenceLabel != null) { confidenceLabel.setText("--"); confidenceLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11; -fx-font-weight: bold;"); }
            if (faceSizeLabel != null) faceSizeLabel.setText("");
            if (captureProgressBox != null) { captureProgressBox.setVisible(false); captureProgressBox.setManaged(false); }
        });
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

        // Check if partenaire is pending approval
        if (utilisateurService.isUserEnAttente(email, password)) {
            showError("⏳ Votre demande partenaire est en cours d'examen. Vous recevrez un email une fois approuvé.");
            glowError(emailField);
            return;
        }

        utilisateur user = utilisateurService.loginUser(email, password);

        if (user != null) {

            // Save or clear "Remember Me" credentials
            saveCredentials(email, password);

            // ✅ Check if 2FA (TOTP) is enabled for this user
            if (totpService.is2FAEnabled(user.getId())) {
                // Show 2FA verification dialog before proceeding
                show2FAVerificationDialog(user);
                return;
            }

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
    // 2FA VERIFICATION DIALOG
    // =========================

    /**
     * Show a stylish 2FA TOTP verification dialog.
     * The user must enter the 6-digit code from Google Authenticator.
     * Also offers SMS OTP and Email OTP as alternatives.
     */
    private void show2FAVerificationDialog(utilisateur user) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initOwner(emailField.getScene().getWindow());

        // ── Main container ──
        VBox container = new VBox(18);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(35, 40, 35, 40));
        container.setMaxWidth(420);
        container.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255,215,0,0.3);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 20;"
        );
        container.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.7)));

        // ── Shield icon ──
        Label shieldIcon = new Label("\uD83D\uDD10");
        shieldIcon.setStyle("-fx-font-size: 48;");

        // ── Title ──
        Label title = new Label("Vérification 2FA");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 22; -fx-font-weight: bold;");

        Label subtitle = new Label("Entrez le code à 6 chiffres de votre\napplication d'authentification");
        subtitle.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 13; -fx-text-alignment: center;");
        subtitle.setWrapText(true);

        // ── Code input (6-digit) ──
        TextField codeField = new TextField();
        codeField.setPromptText("000 000");
        codeField.setMaxWidth(220);
        codeField.setAlignment(Pos.CENTER);
        codeField.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 28;" +
            "-fx-font-weight: bold;" +
            "-fx-prompt-text-fill: #555555;" +
            "-fx-padding: 12 20;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255,215,0,0.2);" +
            "-fx-border-radius: 12;" +
            "-fx-alignment: center;"
        );
        // Restrict to 6 digits only
        codeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                codeField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (newVal.length() > 6) {
                codeField.setText(newVal.substring(0, 6));
            }
        });

        // ── Error label ──
        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12;");
        errorLbl.setVisible(false);

        // ── Verify button ──
        Button verifyBtn = new Button("✓  Vérifier");
        verifyBtn.setMaxWidth(220);
        verifyBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #FFD700, #FFA500);" +
            "-fx-text-fill: #1a1a2e;" +
            "-fx-font-size: 15;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 30;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;"
        );

        // ── Alternative methods ──
        Label altLabel = new Label("— ou utilisez une autre méthode —");
        altLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11;");

        HBox altButtons = new HBox(10);
        altButtons.setAlignment(Pos.CENTER);

        Button smsBtn = new Button("\uD83D\uDCF1 SMS");
        smsBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: #51CF66;" +
            "-fx-font-size: 12;" +
            "-fx-padding: 8 18;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: rgba(81,207,102,0.3);" +
            "-fx-border-radius: 10;"
        );

        Button emailBtn = new Button("✉ Email");
        emailBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: #74C0FC;" +
            "-fx-font-size: 12;" +
            "-fx-padding: 8 18;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: rgba(116,192,252,0.3);" +
            "-fx-border-radius: 10;"
        );

        altButtons.getChildren().addAll(smsBtn, emailBtn);

        // ── Cancel button ──
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #888888;" +
            "-fx-font-size: 12;" +
            "-fx-cursor: hand;" +
            "-fx-underline: true;"
        );

        container.getChildren().addAll(shieldIcon, title, subtitle, codeField, errorLbl,
                                       verifyBtn, altLabel, altButtons, cancelBtn);

        // ── Verify TOTP code action ──
        Runnable verifyAction = () -> {
            String code = codeField.getText().trim();
            if (code.length() != 6) {
                errorLbl.setText("❌ Le code doit contenir 6 chiffres");
                errorLbl.setVisible(true);
                shakeNode(codeField);
                return;
            }
            try {
                String secret = totpService.getSecret(user.getId());
                if (secret != null && totpService.verifyCode(secret, Integer.parseInt(code))) {
                    // ✅ 2FA verified!
                    dialog.close();
                    errorLabel.setText("✅ Bienvenue " + user.getNom());
                    errorLabel.setStyle("-fx-text-fill: #51CF66;");
                    redirectToDashboard(user);
                } else {
                    errorLbl.setText("❌ Code invalide. Réessayez.");
                    errorLbl.setVisible(true);
                    shakeNode(codeField);
                    codeField.clear();
                    codeField.requestFocus();
                }
            } catch (Exception ex) {
                errorLbl.setText("❌ Erreur de vérification: " + ex.getMessage());
                errorLbl.setVisible(true);
            }
        };

        verifyBtn.setOnAction(e -> verifyAction.run());
        codeField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) verifyAction.run();
        });

        // ── SMS OTP alternative ──
        smsBtn.setOnAction(e -> {
            String phone = String.valueOf(user.getNumTel());
            if (phone == null || phone.isEmpty()) {
                errorLbl.setText("❌ Aucun numéro de téléphone enregistré");
                errorLbl.setVisible(true);
                return;
            }
            if (!smsOTPService.isConfigured()) {
                errorLbl.setText("⚠ Service SMS non configuré");
                errorLbl.setVisible(true);
                return;
            }
            String otp = otpService.generateOTP();
            otpService.storeOTP(user.getEmail(), otp);
            smsOTPService.sendSmsOTP(phone, otp);
            subtitle.setText("Un code SMS a été envoyé au " + phone.replaceAll(".(?=.{3})", "*"));
            codeField.clear();
            codeField.setPromptText("Code SMS");
            codeField.requestFocus();
            // Override verify action for SMS OTP
            verifyBtn.setOnAction(ev -> {
                String enteredCode = codeField.getText().trim();
                if (otpService.verifyOTP(user.getEmail(), enteredCode)) {
                    dialog.close();
                    errorLabel.setText("✅ Bienvenue " + user.getNom());
                    errorLabel.setStyle("-fx-text-fill: #51CF66;");
                    redirectToDashboard(user);
                } else {
                    errorLbl.setText("❌ Code SMS invalide ou expiré");
                    errorLbl.setVisible(true);
                    shakeNode(codeField);
                    codeField.clear();
                }
            });
        });

        // ── Email OTP alternative ──
        emailBtn.setOnAction(e -> {
            String otp = otpService.generateOTP();
            otpService.storeOTP(user.getEmail(), otp);
            emailService.sendOtpEmail(user.getEmail(), otp);
            subtitle.setText("Un code a été envoyé à " + user.getEmail().replaceAll("(?<=.).(?=.*@)", "*"));
            codeField.clear();
            codeField.setPromptText("Code Email");
            codeField.requestFocus();
            // Override verify action for Email OTP
            verifyBtn.setOnAction(ev -> {
                String enteredCode = codeField.getText().trim();
                if (otpService.verifyOTP(user.getEmail(), enteredCode)) {
                    dialog.close();
                    errorLabel.setText("✅ Bienvenue " + user.getNom());
                    errorLabel.setStyle("-fx-text-fill: #51CF66;");
                    redirectToDashboard(user);
                } else {
                    errorLbl.setText("❌ Code email invalide ou expiré");
                    errorLbl.setVisible(true);
                    shakeNode(codeField);
                    codeField.clear();
                }
            });
        });

        cancelBtn.setOnAction(e -> dialog.close());

        // ── Build scene ──
        StackPane root = new StackPane(container);
        root.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 500, 520);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) dialog.close();
        });

        dialog.setScene(scene);

        // ── Entrance animation ──
        container.setOpacity(0);
        container.setScaleX(0.85);
        container.setScaleY(0.85);

        dialog.show();
        codeField.requestFocus();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), container);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(250), container);
        scaleIn.setFromX(0.85);
        scaleIn.setFromY(0.85);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

        new ParallelTransition(fadeIn, scaleIn).play();
    }

    // =========================
    // DASHBOARD REDIRECT (ROLE-BASED)
    // =========================
    private void redirectToDashboard(utilisateur user) {
        stopCameraCleanup();
        // Set user's language preference for TranslationService
        String lang = user.getLanguage() != null ? user.getLanguage() : "fr";
        TranslationService.setCurrentLang(lang);
        try {
            boolean isAdmin = utilisateurService.isAdmin(user.getRoleId());
            boolean isPartenaire = utilisateurService.isPartenaire(user.getRoleId());

            if (isAdmin) {
                // ✅ Admin → Admin Dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
                Parent root = loader.load();

                DashboardController controller = loader.getController();
                controller.setUserName(user.getNom());
                controller.setAdminId(user.getId());

                Stage stage = (Stage) emailField.getScene().getWindow();
                fadeTransition(stage, root, "Tabaani - Dashboard");
            } else if (isPartenaire) {
                // ✅ Partenaire → Partenaire Interface
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/partenaireinterface.fxml"));
                Parent root = loader.load();

                PartenaireInterfaceController controller = loader.getController();
                controller.setUser(user);

                Stage stage = (Stage) emailField.getScene().getWindow();
                fadeTransition(stage, root, "Tabaani - Espace Partenaire");
            } else {
                // ✅ Tourist → Main Interface (SmartTravel Hub)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/maininterface.fxml"));
                Parent root = loader.load();

                MainInterfaceController controller = loader.getController();
                controller.setUser(user);

                Stage stage = (Stage) emailField.getScene().getWindow();
                fadeTransition(stage, root, "Tabaani - Accueil");
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
            ThemeManager.applyTheme(newScene);
            newScene.setFill(javafx.scene.paint.Color.BLACK);

            // Prepare entrance state
            newRoot.setOpacity(0);
            newRoot.setScaleX(1.03);
            newRoot.setScaleY(1.03);
            newRoot.setTranslateY(8);

            stage.setScene(newScene);
            stage.setMaximized(true);
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