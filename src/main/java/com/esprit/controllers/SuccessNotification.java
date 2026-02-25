package com.esprit.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Notification de succès moderne et animée (overlay non-bloquant).
 * <p>
 * Design : fond sombre semi-transparent + carte centrée noir & jaune
 * avec icône ✓ animée (scale bounce + glow) et disparition automatique.
 * <p>
 * Usage :
 * <pre>
 *   SuccessNotification.show(anyNode, "Établissement ajouté avec succès !");
 *   SuccessNotification.show(anyNode, "Message", () -> doAfterDismiss());
 * </pre>
 */
public class SuccessNotification {

    /* ── Timing ─────────────────────────────── */
    private static final Duration SHOW_DUR  = Duration.millis(350);
    private static final Duration CARD_DUR  = Duration.millis(500);
    private static final Duration ICON_DUR  = Duration.millis(600);
    private static final Duration PAUSE_DUR = Duration.millis(2500);
    private static final Duration HIDE_DUR  = Duration.millis(400);

    // ─────────────────────────────────────────────────── //
    //  Public API
    // ─────────────────────────────────────────────────── //

    public static void show(Node ownerNode, String message) {
        show(ownerNode, message, null);
    }

    public static void show(Node ownerNode, String message, Runnable onDismiss) {
        try {
            if (ownerNode == null || ownerNode.getScene() == null) {
                // Fallback : exécuter le callback directement
                if (onDismiss != null) Platform.runLater(onDismiss);
                return;
            }

            Scene scene = ownerNode.getScene();
            StackPane rootStack = ensureStackPaneRoot(scene);
            StackPane overlay = buildOverlay(message);
            rootStack.getChildren().add(overlay);
            animateAndSchedule(overlay, rootStack, onDismiss);

        } catch (Exception ex) {
            ex.printStackTrace();
            // Sécurité : si quelque chose échoue, exécuter le callback quand même
            if (onDismiss != null) Platform.runLater(onDismiss);
        }
    }

    // ─────────────────────────────────────────────────── //
    //  Root wrapper — empile l'overlay au-dessus du contenu
    // ─────────────────────────────────────────────────── //

    private static StackPane ensureStackPaneRoot(Scene scene) {
        Parent currentRoot = scene.getRoot();

        // Déjà wrappé ?
        if (currentRoot instanceof StackPane sp
                && "notif-root".equals(sp.getUserData())) {
            return sp;
        }

        StackPane wrapper = new StackPane(currentRoot);
        wrapper.setUserData("notif-root");
        wrapper.setStyle("-fx-background-color: #121212;");
        scene.setRoot(wrapper);
        return wrapper;
    }

    // ─────────────────────────────────────────────────── //
    //  Construction de l'UI (styles 100% inline)
    // ─────────────────────────────────────────────────── //

    private static StackPane buildOverlay(String message) {

        // ── Cercle de fond ──
        Circle circleBg = new Circle(42);
        circleBg.setFill(Color.web("#66BB6A", 0.10));
        circleBg.setStroke(Color.web("#66BB6A", 0.30));
        circleBg.setStrokeWidth(3);

        // ── Icône ✓ (simple Label) ──
        Label checkLabel = new Label("\u2714");
        checkLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        checkLabel.setTextFill(Color.web("#66BB6A"));
        checkLabel.setAlignment(Pos.CENTER);
        checkLabel.setMinSize(84, 84);
        checkLabel.setMaxSize(84, 84);

        DropShadow checkGlow = new DropShadow();
        checkGlow.setColor(Color.web("#66BB6A", 0.60));
        checkGlow.setRadius(18);
        checkGlow.setSpread(0.45);
        checkLabel.setEffect(checkGlow);

        StackPane iconRing = new StackPane(circleBg, checkLabel);
        iconRing.setMaxSize(90, 90);
        iconRing.setMinSize(90, 90);
        iconRing.setAlignment(Pos.CENTER);

        // ── Titre "Succès !" ──
        Label titleLabel = new Label("Succès !");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#FFC107"));
        DropShadow titleGlow = new DropShadow();
        titleGlow.setColor(Color.web("#FFC107", 0.45));
        titleGlow.setRadius(14);
        titleGlow.setSpread(0.35);
        titleLabel.setEffect(titleGlow);

        // ── Ligne décorative dorée ──
        Region accentLine = new Region();
        accentLine.setMaxWidth(140);
        accentLine.setPrefHeight(2);
        accentLine.setMinHeight(2);
        accentLine.setStyle(
                "-fx-background-color: linear-gradient(to right, transparent, #FFC107, transparent);"
                        + "-fx-background-radius: 2;");

        // ── Message ──
        Label msgLabel = new Label(message);
        msgLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        msgLabel.setTextFill(Color.web("#E0E0E0"));
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(360);
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setTextAlignment(TextAlignment.CENTER);

        // ── Hint ──
        Label hint = new Label("Cliquer pour fermer");
        hint.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        hint.setTextFill(Color.web("#555555"));
        hint.setPadding(new Insets(10, 0, 0, 0));

        // ── Carte ──
        VBox card = new VBox(16, iconRing, titleLabel, accentLine, msgLabel, hint);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(460);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(50, 60, 42, 60));
        card.setStyle(
                "-fx-background-color: #1A1A1A;"
                        + "-fx-background-radius: 24;"
                        + "-fx-border-color: rgba(255,193,7,0.35);"
                        + "-fx-border-width: 1.5;"
                        + "-fx-border-radius: 24;");

        DropShadow cardShadow = new DropShadow();
        cardShadow.setColor(Color.web("#FFC107", 0.18));
        cardShadow.setRadius(45);
        cardShadow.setSpread(0.12);
        cardShadow.setOffsetY(10);
        card.setEffect(cardShadow);

        // ── Overlay plein écran ──
        StackPane overlay = new StackPane(card);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.60);");
        overlay.setPickOnBounds(true); // capturer les clics partout

        return overlay;
    }

    // ─────────────────────────────────────────────────── //
    //  Animations
    // ─────────────────────────────────────────────────── //

    private static void animateAndSchedule(StackPane overlay, StackPane rootStack, Runnable onDismiss) {

        VBox card       = (VBox) overlay.getChildren().get(0);
        StackPane ring  = (StackPane) card.getChildren().get(0); // iconRing

        // Référence pour le cleanup
        final boolean[] dismissed = {false};
        Runnable safeCleanup = () -> {
            if (dismissed[0]) return;
            dismissed[0] = true;
            rootStack.getChildren().remove(overlay);
            if (onDismiss != null) {
                Platform.runLater(onDismiss);
            }
        };

        // ── État initial ──
        overlay.setOpacity(0);
        card.setScaleX(0.5);
        card.setScaleY(0.5);
        card.setTranslateY(40);
        ring.setScaleX(0.0);
        ring.setScaleY(0.0);

        // === SHOW ===

        // 1) Fond fade-in
        FadeTransition bgIn = new FadeTransition(SHOW_DUR, overlay);
        bgIn.setFromValue(0);
        bgIn.setToValue(1);

        // 2) Carte scale + slide
        ScaleTransition cardScale = new ScaleTransition(CARD_DUR, card);
        cardScale.setFromX(0.5);
        cardScale.setFromY(0.5);
        cardScale.setToX(1.0);
        cardScale.setToY(1.0);

        TranslateTransition cardSlide = new TranslateTransition(CARD_DUR, card);
        cardSlide.setFromY(40);
        cardSlide.setToY(0);

        // 3) Icône bounce
        ScaleTransition iconBounce = new ScaleTransition(ICON_DUR, ring);
        iconBounce.setFromX(0.0);
        iconBounce.setFromY(0.0);
        iconBounce.setToX(1.0);
        iconBounce.setToY(1.0);
        iconBounce.setDelay(Duration.millis(200));

        // 4) Pulse léger sur l'icône (zoom in / out)
        ScaleTransition iconPulse = new ScaleTransition(Duration.millis(400), ring);
        iconPulse.setFromX(1.0);
        iconPulse.setFromY(1.0);
        iconPulse.setToX(1.12);
        iconPulse.setToY(1.12);
        iconPulse.setCycleCount(2);
        iconPulse.setAutoReverse(true);
        iconPulse.setDelay(Duration.millis(700));

        ParallelTransition showAll = new ParallelTransition(bgIn, cardScale, cardSlide, iconBounce, iconPulse);

        // === PAUSE ===
        PauseTransition pause = new PauseTransition(PAUSE_DUR);

        // === HIDE ===
        FadeTransition bgOut = new FadeTransition(HIDE_DUR, overlay);
        bgOut.setToValue(0);

        ScaleTransition cardShrink = new ScaleTransition(HIDE_DUR, card);
        cardShrink.setToX(0.88);
        cardShrink.setToY(0.88);

        TranslateTransition cardDrop = new TranslateTransition(HIDE_DUR, card);
        cardDrop.setToY(25);

        ParallelTransition hideAll = new ParallelTransition(bgOut, cardShrink, cardDrop);
        hideAll.setOnFinished(e -> safeCleanup.run());

        // === Séquence complète ===
        SequentialTransition seq = new SequentialTransition(showAll, pause, hideAll);
        seq.setOnFinished(e -> safeCleanup.run()); // double sécurité
        seq.play();

        // === Timer de sécurité : si l'animation bug, cleanup après 5s ===
        PauseTransition safety = new PauseTransition(Duration.millis(5000));
        safety.setOnFinished(e -> safeCleanup.run());
        safety.play();

        // === Clic = fermeture immédiate ===
        overlay.setOnMouseClicked(e -> {
            seq.stop();
            safety.stop();

            FadeTransition quickFade = new FadeTransition(Duration.millis(200), overlay);
            quickFade.setToValue(0);
            quickFade.setOnFinished(ev -> safeCleanup.run());
            quickFade.play();
        });
    }
}
