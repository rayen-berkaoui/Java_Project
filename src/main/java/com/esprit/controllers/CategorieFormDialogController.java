package com.esprit.controllers;

import com.esprit.entities.categorie;
import com.esprit.services.SpeechRecognitionService;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.time.LocalDate;

public class CategorieFormDialogController {

    @FXML private TextField tfNom, tfDescription;
    @FXML private Label lblErrNom, lblErrDesc, lblVoiceStatus;
    @FXML private Button btnMic;

    private String mode;
    private SpeechRecognitionService speechService;
    private Timeline pulseAnimation;
    private String textBeforeRecording = "";

    @FXML
    public void initialize() {
        // Real-time validation listeners
        if (tfNom != null) tfNom.textProperty().addListener((obs, o, n) -> validateNom(n));
        if (tfDescription != null) tfDescription.textProperty().addListener((obs, o, n) -> validateDescription(n));

        // Initialize speech recognition service
        speechService = new SpeechRecognitionService();

        // Cleanup when dialog closes
        if (btnMic != null) {
            btnMic.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null && speechService != null) {
                    speechService.stopRecording();
                    speechService.dispose();
                }
            });
        }
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setCategorie(categorie c) {
        if (c != null) {
            if (tfNom != null) tfNom.setText(c.getNomcategorie() != null ? c.getNomcategorie() : "");
            if (tfDescription != null) tfDescription.setText(c.getDescription() != null ? c.getDescription() : "");
        }
    }

    public categorie getCategorie() {
        if (tfNom == null || tfDescription == null) {
            System.err.println("❌ Form fields not initialized");
            return null;
        }

        boolean valid = true;

        // --- Nom ---
        String nom = tfNom.getText().trim();
        if (nom.isEmpty()) {
            showFieldError(tfNom, lblErrNom, "Le nom est obligatoire");
            valid = false;
        } else if (nom.length() < 2) {
            showFieldError(tfNom, lblErrNom, "Minimum 2 caractères");
            valid = false;
        } else if (nom.length() > 50) {
            showFieldError(tfNom, lblErrNom, "Maximum 50 caractères");
            valid = false;
        } else if (!nom.matches("[a-zA-ZÀ-ÿ0-9\\s\\-'&]+")) {
            showFieldError(tfNom, lblErrNom, "Caractères spéciaux non autorisés");
            valid = false;
        } else {
            showFieldValid(tfNom, lblErrNom);
        }

        // --- Description ---
        String description = tfDescription.getText().trim();
        if (description.isEmpty()) {
            showFieldError(tfDescription, lblErrDesc, "La description est obligatoire");
            valid = false;
        } else if (description.length() < 5) {
            showFieldError(tfDescription, lblErrDesc, "Minimum 5 caractères");
            valid = false;
        } else {
            showFieldValid(tfDescription, lblErrDesc);
        }

        if (!valid) return null;

        return new categorie(nom, description, LocalDate.now());
    }

    // ===== Real-time validators =====

    private void validateNom(String val) {
        if (val == null) return;
        String v = val.trim();
        if (v.isEmpty()) {
            clearFieldState(tfNom, lblErrNom);
        } else if (v.length() < 2) {
            showFieldError(tfNom, lblErrNom, "Minimum 2 caractères");
        } else if (v.length() > 50) {
            showFieldError(tfNom, lblErrNom, "Maximum 50 caractères");
        } else if (!v.matches("[a-zA-ZÀ-ÿ0-9\\s\\-'&]+")) {
            showFieldError(tfNom, lblErrNom, "Caractères spéciaux non autorisés");
        } else {
            showFieldValid(tfNom, lblErrNom);
        }
    }

    private void validateDescription(String val) {
        if (val == null) return;
        String v = val.trim();
        if (v.isEmpty()) {
            clearFieldState(tfDescription, lblErrDesc);
        } else if (v.length() < 5) {
            showFieldError(tfDescription, lblErrDesc, "Minimum 5 caractères");
        } else {
            showFieldValid(tfDescription, lblErrDesc);
        }
    }

    // ===== Styling helpers =====

    private void showFieldError(TextField field, Label errLabel, String msg) {
        if (field != null) {
            field.getStyleClass().removeAll("input-error", "input-valid");
            field.getStyleClass().add("input-error");
        }
        if (errLabel != null) { errLabel.setText(msg); errLabel.setVisible(true); errLabel.setManaged(true); }
    }

    private void showFieldValid(TextField field, Label errLabel) {
        if (field != null) {
            field.getStyleClass().removeAll("input-error", "input-valid");
            field.getStyleClass().add("input-valid");
        }
        if (errLabel != null) { errLabel.setVisible(false); errLabel.setManaged(false); }
    }

    private void clearFieldState(TextField field, Label errLabel) {
        if (field != null) field.getStyleClass().removeAll("input-error", "input-valid");
        if (errLabel != null) { errLabel.setVisible(false); errLabel.setManaged(false); }
    }

    // ===== Voice Recognition =====

    /** Toggle voice recording on/off when mic button is clicked. */
    @FXML
    private void toggleVoiceRecording() {
        if (speechService.isRecording()) {
            stopVoiceRecording();
        } else {
            startVoiceRecording();
        }
    }

    private void startVoiceRecording() {
        // Step 1: Download model if not present
        if (!speechService.isModelDownloaded()) {
            downloadModelThenRecord();
            return;
        }

        // Step 2: Initialize model if needed
        if (!speechService.initializeModel()) {
            showVoiceStatus("❌ Erreur d'initialisation du modèle vocal", true);
            return;
        }

        // Step 3: Capture existing text to preserve it
        textBeforeRecording = tfDescription.getText() != null ? tfDescription.getText().trim() : "";

        // Step 4: Start recording
        btnMic.setText("⏹");
        btnMic.getStyleClass().add("btn-mic-recording");
        showVoiceStatus("🔴 Parlez maintenant... (cliquez ⏹ pour arrêter)", false);
        startPulseAnimation();

        speechService.startRecording(new SpeechRecognitionService.TranscriptionListener() {
            @Override
            public void onPartialResult(String partialText) {
                Platform.runLater(() -> {
                    String combined = textBeforeRecording.isEmpty()
                            ? partialText
                            : textBeforeRecording + " " + partialText;
                    tfDescription.setText(combined);
                    tfDescription.positionCaret(combined.length());
                });
            }

            @Override
            public void onFinalResult(String finalText) {
                Platform.runLater(() -> {
                    String combined = textBeforeRecording.isEmpty()
                            ? finalText
                            : textBeforeRecording + " " + finalText;
                    if (!combined.trim().isEmpty()) {
                        tfDescription.setText(combined.trim());
                    }
                    stopVoiceRecordingUI();
                });
            }

            @Override
            public void onError(String errorMessage) {
                Platform.runLater(() -> {
                    showVoiceStatus("❌ " + errorMessage, true);
                    stopVoiceRecordingUI();
                });
            }
        });
    }

    private void stopVoiceRecording() {
        speechService.stopRecording();
        stopVoiceRecordingUI();
    }

    private void stopVoiceRecordingUI() {
        btnMic.setText("🎤");
        btnMic.getStyleClass().remove("btn-mic-recording");
        stopPulseAnimation();
        hideVoiceStatus();
    }

    /** Download the Vosk French model with progress indicator, then start recording. */
    private void downloadModelThenRecord() {
        btnMic.setDisable(true);
        showVoiceStatus("📥 Téléchargement du modèle vocal français (première utilisation)...", false);

        Thread downloadThread = new Thread(() -> {
            try {
                speechService.downloadModel(progress -> {
                    Platform.runLater(() -> {
                        if (progress < 0) {
                            showVoiceStatus("📦 Extraction du modèle...", false);
                        } else {
                            showVoiceStatus(
                                String.format("📥 Téléchargement du modèle vocal: %.0f%%", progress * 100),
                                false
                            );
                        }
                    });
                });
                Platform.runLater(() -> {
                    btnMic.setDisable(false);
                    hideVoiceStatus();
                    // Model ready — now start recording
                    startVoiceRecording();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnMic.setDisable(false);
                    showVoiceStatus("❌ Erreur de téléchargement: " + e.getMessage(), true);
                });
            }
        });
        downloadThread.setDaemon(true);
        downloadThread.setName("vosk-model-download");
        downloadThread.start();
    }

    // ===== Voice UI helpers =====

    private void showVoiceStatus(String text, boolean isError) {
        if (lblVoiceStatus != null) {
            lblVoiceStatus.setText(text);
            lblVoiceStatus.setStyle(isError
                    ? "-fx-text-fill: #e74c3c; -fx-font-size: 10px;"
                    : "-fx-text-fill: rgba(191,162,0,0.85); -fx-font-size: 10px;");
            lblVoiceStatus.setVisible(true);
            lblVoiceStatus.setManaged(true);
        }
    }

    private void hideVoiceStatus() {
        if (lblVoiceStatus != null) {
            lblVoiceStatus.setVisible(false);
            lblVoiceStatus.setManaged(false);
        }
    }

    private void startPulseAnimation() {
        if (btnMic == null) return;
        pulseAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(btnMic.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(600), new KeyValue(btnMic.opacityProperty(), 0.35)),
                new KeyFrame(Duration.millis(1200), new KeyValue(btnMic.opacityProperty(), 1.0))
        );
        pulseAnimation.setCycleCount(Timeline.INDEFINITE);
        pulseAnimation.play();
    }

    private void stopPulseAnimation() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            if (btnMic != null) btnMic.setOpacity(1.0);
        }
    }
}
