package com.esprit.utils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.paint.Color;

/**
 * Singleton that tracks user activity and auto-logs out after inactivity.
 *
 * <p>Usage — call {@code SessionManager.getInstance().startMonitoring(scene)}
 * from any controller's {@code initialize()} after the scene is available.</p>
 */
public class SessionManager {

    private static SessionManager instance;

    /** Total idle time (minutes) before auto-logout. */
    private static final int TIMEOUT_MINUTES = 10;
    /** Warning popup appears this many seconds before logout. */
    private static final int WARNING_SECONDS = 60;

    private Timeline idleTimeline;
    private int remainingSeconds;
    private Alert warningAlert;
    private Scene monitoredScene;
    private boolean monitoring = false;

    private SessionManager() { }

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    // ────────────── public API ──────────────

    /** Attach mouse / key listeners to the scene and start the idle timer. */
    public void startMonitoring(Scene scene) {
        if (scene == null) return;
        stopMonitoring(); // clean up previous
        this.monitoredScene = scene;
        this.monitoring = true;

        // Reset timer on ANY user input
        EventHandler<javafx.scene.input.InputEvent> resetHandler = e -> resetTimer();
        scene.addEventFilter(javafx.scene.input.MouseEvent.ANY, resetHandler);
        scene.addEventFilter(javafx.scene.input.KeyEvent.ANY, resetHandler);

        resetTimer();
    }

    /** Stop monitoring (call on manual logout to avoid double fire). */
    public void stopMonitoring() {
        monitoring = false;
        if (idleTimeline != null) {
            idleTimeline.stop();
            idleTimeline = null;
        }
        dismissWarning();
        monitoredScene = null;
    }

    /** Reset the idle clock back to full. */
    public void resetTimer() {
        if (!monitoring) return;
        dismissWarning();

        if (idleTimeline != null) idleTimeline.stop();

        remainingSeconds = TIMEOUT_MINUTES * 60;

        idleTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        idleTimeline.setCycleCount(Timeline.INDEFINITE);
        idleTimeline.play();
    }

    // ────────────── internals ──────────────

    private void tick() {
        remainingSeconds--;

        if (remainingSeconds == WARNING_SECONDS) {
            Platform.runLater(this::showWarning);
        }

        if (remainingSeconds <= WARNING_SECONDS && warningAlert != null) {
            Platform.runLater(() -> updateWarningCountdown(remainingSeconds));
        }

        if (remainingSeconds <= 0) {
            Platform.runLater(this::performLogout);
        }
    }

    // ────────────── warning dialog ──────────────

    private Label countdownLabel;

    private void showWarning() {
        if (warningAlert != null) return;

        warningAlert = new Alert(Alert.AlertType.NONE);
        warningAlert.initStyle(StageStyle.UNDECORATED);

        Label title = new Label("Session Expiring");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #333333;");

        Label msg = new Label("You will be logged out due to inactivity.");
        msg.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13;");
        msg.setWrapText(true);

        countdownLabel = new Label(formatCountdown(WARNING_SECONDS));
        countdownLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 28; -fx-font-weight: bold;");

        Label hint = new Label("Move your mouse or press a key to stay connected.");
        hint.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");
        hint.setWrapText(true);

        VBox content = new VBox(14, title, sep, msg, countdownLabel, hint);
        content.setStyle("-fx-padding: 24;");

        DialogPane dp = warningAlert.getDialogPane();
        dp.setContent(content);
        dp.getButtonTypes().add(ButtonType.OK);
        ((Button) dp.lookupButton(ButtonType.OK)).setText("Stay Connected");
        if (monitoredScene != null) {
            dp.getStylesheets().add(ThemeManager.getInstance().getCssPath());
        }
        dp.getStyleClass().add("dialog-pane");

        warningAlert.setOnHidden(e -> {
            warningAlert = null;
            countdownLabel = null;
            resetTimer();
        });
        warningAlert.show();
    }

    private void updateWarningCountdown(int seconds) {
        if (countdownLabel != null) {
            countdownLabel.setText(formatCountdown(Math.max(0, seconds)));
        }
    }

    private String formatCountdown(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%d:%02d", m, s);
    }

    private void dismissWarning() {
        if (warningAlert != null) {
            warningAlert.close();
            warningAlert = null;
            countdownLabel = null;
        }
    }

    // ────────────── logout ──────────────

    private void performLogout() {
        stopMonitoring();
        if (monitoredScene == null) return;

        try {
            Stage stage = (Stage) monitoredScene.getWindow();
            if (stage == null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Scene newScene = new Scene(root);
            newScene.getStylesheets().add(ThemeManager.getInstance().getCssPath());
            newScene.setFill(Color.BLACK);
            stage.setScene(newScene);
            stage.setTitle("Tabaani - Login");
            stage.sizeToScene();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
